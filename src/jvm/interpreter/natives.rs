//! The **native bridge** — the escape hatch from bytecode to real native code.
//!
//! A `native` method has no `Code`; the interpreter, instead of pushing a frame,
//! calls in here. This is where the JVM reaches the outside world (I/O, the OS) —
//! the things Java can't do itself. In a real JVM these are C/C++ via JNI; ours are
//! Rust functions matched by `(class, name, descriptor)`.
//!
//! Right now there's one: `PrintStream.println(int)`, so `System.out.println(n)`
//! prints for real — the wall the whole interpreter has been building toward.

use std::cell::RefCell;
use std::fmt::Write;

use crate::jvm::class_file::ClassFile;

use super::apt::AptContext;
use super::bytecode_interpreter::annotation_factory::{self, Element};
use crate::jvm::parser::attributes::annotations::{self, ResolvedAnnotation};
use super::bytecode_interpreter::objects_operations::{
    allocate_old, field_offset, HEADER_SIZE, SLOT_SIZE,
};
use super::bytecode_interpreter::{array_operations, class_operations, objects_operations};
use super::frame::Value;
use super::heap::HeapService;
use super::metaspace::MetaspaceService;
use super::strings;

// --- APT fase 4: el canal lateral del Filer -------------------------------------------------------
//
// El `Filer` de un procesador de anotaciones fabrica archivos fuente en tiempo de ejecución. Cada
// `createSourceFile(name)` que hace el procesador crea un `StringWriter` que recibe el texto
// generado, y necesita quedar **registrado** para que el round loop lo drene una vez que el
// procesador retorna. La bytecode no puede escribir directamente en estado del compilador, así que
// `KajiFiler.nativeRegisterSourceFile` empuja acá `(nombre, offset del StringWriter en el heap)`.
//
// Es un `thread_local` a propósito: el intérprete verde corre en un solo hilo, y cada test de
// `cargo test` (que corren en paralelo, un hilo por test) ve su propio Filer sin pisarse con otro.

/// Lo que un procesador registró vía su `Filer`: los archivos fuente pendientes, en orden de
/// creación, cada uno como `(nombre, writer_ref)` donde `writer_ref` es el offset en el heap del
/// `StringWriter` que acumula su texto.
#[derive(Default)]
pub struct FilerState {
    pub pending: Vec<(String, u32)>,
}

thread_local! {
    /// El Filer **armado** en este hilo, o `None` si no hay ninguno corriendo. Los nativos del
    /// Filer solo registran cuando está armado, así que una llamada suelta a `createSourceFile`
    /// fuera de una ronda de procesamiento no rompe nada (se descarta).
    static FILER: RefCell<Option<FilerState>> = const { RefCell::new(None) };
}

/// **Arma** el canal del Filer en este hilo — llamar antes de correr un procesador. Reemplaza
/// cualquier estado previo por uno vacío.
pub fn install_filer() {
    FILER.with(|f| *f.borrow_mut() = Some(FilerState::default()));
}

/// Los `StringWriter` que el Filer registró, como offsets del heap — **raíces del GC que sostiene
/// la VM**. El canal sobrevive a los frames que los crearon (el compilador lo drena *después* de
/// que el procesador retorna), así que ninguna pila los mantiene vivos: sin esto, una colección
/// que mueve objetos dejaría cada offset apuntando a donde el writer *solía* estar, y el texto
/// generado se recuperaría vacío.
pub fn filer_roots() -> Vec<usize> {
    FILER.with(|f| {
        f.borrow()
            .as_ref()
            .map(|state| state.pending.iter().map(|&(_, r)| r as usize).collect())
            .unwrap_or_default()
    })
}

/// Reescribe los offsets que el GC remapeó, en el mismo orden en que [`filer_roots`] los entregó.
pub fn remap_filer_roots(refs: &[usize]) {
    FILER.with(|f| {
        if let Some(state) = f.borrow_mut().as_mut() {
            for (entry, &offset) in state.pending.iter_mut().zip(refs) {
                entry.1 = offset as u32;
            }
        }
    });
}

/// **Desarma** el canal y devuelve todo lo que el procesador registró, en orden de creación. Si no
/// había Filer armado, devuelve un vector vacío.
pub fn drain_filer() -> Vec<(String, u32)> {
    FILER.with(|f| f.borrow_mut().take().map(|state| state.pending).unwrap_or_default())
}

/// Runs the native method `class.name descriptor` with `args` (slot 0 is the
/// receiver for an instance method), returning its result (`None` for `void`).
/// `heap` lets a native read object memory (e.g. an object's header); anything the
/// method "prints" is appended to `out` — the program's stdout, which the caller
/// surfaces (the visualizer shows it; a headless run would flush it). `apt` is the
/// reified compiler model (APT fase 3), or `None` outside a processor run — the
/// `jdk/internal/apt/SymElement` natives read the symbol table through it.
pub fn dispatch(
    class: &str,
    name: &str,
    descriptor: &str,
    args: &[Value],
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    out: &mut String,
    apt: &mut Option<AptContext>,
) -> Option<Value> {
    match (class, name, descriptor) {
        // --- I/O: PrintStream.println --------------------------------------------
        // The receiver is args[0]; the value follows. The native `write` the real
        // java.io chain bottoms out at.
        ("java/io/PrintStream", "println", "(I)V") => {
            if let Value::Int(n) = args[1] {
                let _ = writeln!(out, "{n}");
            }
            None
        }
        // println(String): the arg is a heap String reference; read its bytes back.
        ("java/io/PrintStream", "println", "(Ljava/lang/String;)V") => {
            let _ = writeln!(out, "{}", strings::read(heap, reference(&args[1])));
            None
        }
        // APT (fase 2): salida de un processor durante el round loop, vía este native estático — el
        // mínimo `Messager` delegando a Rust. `args[0]` es la referencia al String (método estático:
        // sin receptor). (Nota histórica: se agregó cuando `System.out.println` no compilaba en este
        // javac por un bug de carga transitiva —el tipo de `System.out` quedaba sin resolver—; ese
        // bug ya está arreglado y `System.out.println` compila, pero el native se conserva como el
        // canal directo `Messager`→stdout de un processor.)
        ("javax/annotation/processing/AptTrace", "trace", "(Ljava/lang/String;)V") => {
            let _ = writeln!(out, "{}", strings::read(heap, reference(&args[0])));
            None
        }

        // --- Introspection / identity (things Java can't read of itself) ---------
        // getClass(): the receiver's header `class_id` *is* its Class<…> mirror.
        ("java/lang/Object", "getClass", "()Ljava/lang/Class;") => {
            Some(Value::Reference(heap.read_u32(reference(&args[0])) as usize))
        }
        // hashCode() (identity): the object's heap offset is its identity.
        ("java/lang/Object", "hashCode", "()I") => Some(Value::Int(reference(&args[0]) as i32)),
        // Throwable.toString(): "pkg.Class" or "pkg.Class: message". Reads the receiver's runtime
        // class name (Java has no Class.getName() yet) and the `message` field, then interns the
        // text. Called virtually, so a subclass instance (e.g. NullPointerException) reports its
        // own class name.
        ("java/lang/Throwable", "toString", "()Ljava/lang/String;") => {
            let this_ref = reference(&args[0]);
            let class_id = heap.read_u32(this_ref) as usize;
            let internal = metaspace
                .class_name_at_mirror(class_id)
                .unwrap_or("java/lang/Throwable")
                .to_string();
            let dotted = internal.replace('/', ".");
            let msg_off = field_offset(metaspace, &internal, "message");
            let msg_ref = heap.read_u32(this_ref + msg_off) as usize;
            let text = if msg_ref == 0 {
                dotted
            } else {
                format!("{dotted}: {}", strings::read(heap, msg_ref))
            };
            Some(Value::Reference(strings::intern(metaspace, heap, &text)))
        }
        // System.identityHashCode(Object): the same, as a static.
        ("java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I") => {
            Some(Value::Int(reference(&args[0]) as i32))
        }
        // System.nanoTime(): a monotonic timer with an arbitrary origin (elapsed since the first
        // call in this process). Backs scheduling delays; never goes backwards.
        ("java/lang/System", "nanoTime", "()J") => {
            use std::sync::OnceLock;
            use std::time::Instant;
            static START: OnceLock<Instant> = OnceLock::new();
            let start = START.get_or_init(Instant::now);
            Some(Value::Long(start.elapsed().as_nanos() as i64))
        }
        // System.mapLibraryName(name): the platform's file name for a native library, e.g.
        // `foo` -> `foo.dll` on Windows, `libfoo.dylib` on macOS, `libfoo.so` elsewhere. Native
        // because the mapping is a property of the host, not of bytecode. (KajiJDK loads no native
        // libraries -- see load/loadLibrary -- but the name it *would* look for is still well-defined.)
        ("java/lang/System", "mapLibraryName", "(Ljava/lang/String;)Ljava/lang/String;") => {
            let name = strings::read(heap, reference(&args[0]));
            let mapped = if cfg!(windows) {
                format!("{name}.dll")
            } else if cfg!(target_os = "macos") {
                format!("lib{name}.dylib")
            } else {
                format!("lib{name}.so")
            };
            Some(Value::Reference(strings::intern(metaspace, heap, &mapped)))
        }
        // System.setIn0/setOut0/setErr0(stream): the native seams behind setIn/setOut/setErr. They
        // exist because `in`/`out`/`err` are `public static final`: bytecode cannot reassign a final
        // field, but native code writes the slot directly (exactly how the JDK does it). A reference
        // static is a single slot holding a heap offset, written like `putstatic` (no barrier: the
        // mirror's statics are scanned as GC roots).
        ("java/lang/System", "setIn0", "(Ljava/io/InputStream;)V")
        | ("java/lang/System", "setOut0", "(Ljava/io/PrintStream;)V")
        | ("java/lang/System", "setErr0", "(Ljava/io/PrintStream;)V") => {
            let field = match name {
                "setIn0" => "in",
                "setOut0" => "out",
                _ => "err",
            };
            let at = class_operations::static_slot(metaspace, heap, "java/lang/System", field);
            heap.write_u32(at, reference(&args[0]) as u32);
            None
        }

        // Runtime.availableProcessors(): how many CPUs the VM can actually use — the host's
        // parallelism, which only the OS can answer. Falls back to 1 (the value the JVMS
        // permits when the count is unavailable) rather than failing.
        ("java/lang/Runtime", "availableProcessors", "()I") => Some(Value::Int(
            std::thread::available_parallelism().map_or(1, |n| n.get() as i32),
        )),
        // Runtime.gc(): a hint. KajiJDK's GC runs on its own schedule; an explicit request is a
        // no-op (a correct answer -- gc() has never been a guarantee) rather than a forced cycle.
        ("java/lang/Runtime", "gc", "()V") => None,
        // Memory accounting: KajiJDK does not expose the heap's real figures, so it reports a fixed,
        // plausible budget -- enough for callers that only compare or log these.
        ("java/lang/Runtime", "maxMemory", "()J") => Some(Value::Long(256 * 1024 * 1024)),
        ("java/lang/Runtime", "totalMemory", "()J") => Some(Value::Long(64 * 1024 * 1024)),
        ("java/lang/Runtime", "freeMemory", "()J") => Some(Value::Long(32 * 1024 * 1024)),

        // --- Math (would map to CPU instructions under a JIT) --------------------
        ("java/lang/Math", "abs", "(I)I") => Some(Value::Int(int(&args[0]).abs())),
        ("java/lang/Math", "max", "(II)I") => Some(Value::Int(int(&args[0]).max(int(&args[1])))),
        ("java/lang/Math", "min", "(II)I") => Some(Value::Int(int(&args[0]).min(int(&args[1])))),

        // --- Integer bit ops (popcnt / lzcnt) -----------------------------------
        ("java/lang/Integer", "bitCount", "(I)I") => {
            Some(Value::Int(int(&args[0]).count_ones() as i32))
        }
        ("java/lang/Integer", "numberOfLeadingZeros", "(I)I") => {
            Some(Value::Int(int(&args[0]).leading_zeros() as i32))
        }

        // --- Arrays: System.arraycopy -------------------------------------------
        // La copia a granel entre arrays. Dos cosas que parecen detalle y no lo son:
        //
        // 1. **El ancho del elemento sale de la CLASE del array**, no de una suposición. Un
        //    `char[]` mide dos bytes por elemento y un `long[]` ocho; copiar todo con paso de
        //    cuatro no solo escribe basura, además se **sale del array por el final** — que es
        //    de dónde salían los pánicos "range end index N out of range for slice of length
        //    N-4" en cuanto algo copiaba texto.
        //
        // 2. **El solapamiento**. El contrato dice que se comporta como si el origen se copiara
        //    primero a un buffer temporal, así que con el mismo array y rangos que se pisan la
        //    dirección del recorrido decide el resultado. Un `delete` de un `StringBuilder` es
        //    exactamente ese caso, y hacia abajo funcionaba por casualidad.
        ("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V") => {
            use super::bytecode_interpreter::array_operations::{
                array_element_width, ARRAY_HEADER_SIZE,
            };
            let (src, src_pos) = (reference(&args[0]), int(&args[1]) as usize);
            let (dst, dst_pos) = (reference(&args[2]), int(&args[3]) as usize);
            let length = int(&args[4]) as usize;
            let class = metaspace
                .class_name_at_mirror(heap.read_u32(src) as usize)
                .map(str::to_string)
                .expect("System.arraycopy: el origen no es un array conocido");
            let width = array_element_width(&class);
            let from = src + ARRAY_HEADER_SIZE + src_pos * width;
            let to = dst + ARRAY_HEADER_SIZE + dst_pos * width;
            // Un array de referencias se copia slot a slot y **por la puerta del barrier**: un
            // `write_u8` dejaría al GC sin enterarse de la referencia nueva.
            let of_references = matches!(class.as_bytes().get(1), Some(b'L' | b'['));
            if of_references {
                let slots: Vec<usize> =
                    (0..length).map(|i| heap.read_u32(from + i * width) as usize).collect();
                for (i, value) in slots.into_iter().enumerate() {
                    heap.store_reference(dst, to + i * width, value);
                }
            } else if to > from {
                for i in (0..length * width).rev() {
                    let byte = heap.read_u8(from + i);
                    heap.write_u8(to + i, byte);
                }
            } else {
                for i in 0..length * width {
                    let byte = heap.read_u8(from + i);
                    heap.write_u8(to + i, byte);
                }
            }
            None
        }

        // --- Class.isInstance: the subtype check, reusing is_subtype -------------
        // The receiver is a Class mirror; args[1] is the object to test. `null` is
        // never an instance.
        ("java/lang/Class", "isInstance", "(Ljava/lang/Object;)Z") => {
            let object = reference(&args[1]);
            if object == 0 {
                return Some(Value::Int(0));
            }
            let target = metaspace.class_name_at_mirror(reference(&args[0])).map(str::to_string);
            let runtime =
                metaspace.class_name_at_mirror(heap.read_u32(object) as usize).map(str::to_string);
            let is = match (target, runtime) {
                (Some(t), Some(r)) => class_operations::is_subtype(metaspace, &r, &t),
                _ => false,
            };
            Some(Value::Int(is as i32))
        }
        // --- Class.isAnnotationPresent: JSR 175 reflection over §4.7.16 ----------
        // Both the receiver and the argument are Class mirrors, so both heap offsets key
        // `class_name_at_mirror`. The receiver's class file holds its `RuntimeVisibleAnnotations`
        // (only RUNTIME-retention annotations are written there — that's javac's job, so the
        // retention rule falls out for free); each entry names its annotation type by *descriptor*,
        // which is what the argument's name becomes as `L…;`. A mirror with no class file behind
        // it (a primitive, an array) has no attributes → false.
        //
        // Only *directly present* class-level annotations count: no @Inherited walk up the
        // superclass chain, and no field/method/parameter annotations (we have no Field/Method
        // model to hang them on). `getAnnotation` is deliberately absent — returning an annotation
        // *object* would mean synthesising a proxy class implementing the @interface, as the JDK
        // does; presence is the part of the API that needs no such object.
        // `annotationPresent0` es el nombre de la costura privada de KajiLibrary;
        // `isAnnotationPresent` el de la copia compilada de `boot/`. Los dos, mientras convivan.
        ("java/lang/Class", "annotationPresent0" | "isAnnotationPresent", "(Ljava/lang/Class;)Z") => {
            let this = metaspace
                .class_name_at_mirror(reference(&args[0]))
                .expect("Class.isAnnotationPresent: no class at this mirror")
                .to_string();
            let wanted = metaspace
                .class_name_at_mirror(reference(&args[1]))
                .map(|name| format!("L{name};"))
                .expect("Class.isAnnotationPresent: no class at the argument mirror");
            let present = metaspace
                .get_or_load(&this)
                .map(ClassFile::runtime_visible_annotation_types)
                .is_some_and(|types| types.contains(&wanted));
            Some(Value::Int(present as i32))
        }
        // --- Class.getAnnotation & friends: reflection that hands back the annotation as an OBJECT.
        // Where `annotationPresent0` answers *presence* by a descriptor compare, this materialises a
        // real instance of each @interface. For every entry in `RuntimeVisibleAnnotations`, the VM
        // spins a class implementing the @interface whose element methods return the values written
        // at the use site (falling back to the @interface's defaults), then allocates one — an
        // annotation object carries no instance fields, so allocation alone is a complete object and
        // no `<init>` needs to run. The Java side filters this array by type
        // (getAnnotation/getAnnotationsByType/…). No @Inherited walk: only *directly present*
        // class-level annotations, matching `isAnnotationPresent`.
        ("java/lang/Class", "declaredAnnotations0", "()[Ljava/lang/annotation/Annotation;") => {
            let this = mirror_name(metaspace, reference(&args[0]));
            let objects = annotation_objects(metaspace, heap, &this);
            Some(Value::Reference(reference_array(
                metaspace,
                heap,
                "[Ljava/lang/annotation/Annotation;",
                &objects,
            )))
        }
        ("java/lang/Class", "descriptorString", "()Ljava/lang/String;") => {
            // The field descriptor of the class this mirror names. A **primitive** mirror is named
            // by its type name (`int`, …) → a one-letter descriptor; an array's internal name *is*
            // already a descriptor (`[I`, `[Ljava/lang/String;`); a class/interface name
            // (`java/lang/String`) becomes `L…;`.
            let name = metaspace
                .class_name_at_mirror(reference(&args[0]))
                .map(str::to_string)
                .expect("Class.descriptorString: no class at this mirror");
            let descriptor = match name.as_str() {
                "int" => "I".to_string(),
                "long" => "J".to_string(),
                "double" => "D".to_string(),
                "float" => "F".to_string(),
                "short" => "S".to_string(),
                "byte" => "B".to_string(),
                "char" => "C".to_string(),
                "boolean" => "Z".to_string(),
                "void" => "V".to_string(),
                n if n.starts_with('[') => n.to_string(),
                n => format!("L{n};"),
            };
            let offset = strings::intern(metaspace, heap, &descriptor);
            Some(Value::Reference(offset))
        }
        ("java/lang/Class", "getName", "()Ljava/lang/String;") => {
            // The JDK-format name of the class this mirror names. The receiver *is* the mirror,
            // so its heap offset keys `class_name_at_mirror` directly. Classes/interfaces get the
            // dotted binary name ("java.lang.String"); an array's internal name is already
            // descriptor-shaped, so it comes out in descriptor form with dots ("[I",
            // "[Ljava.lang.String;"). Both are just '/' → '.' on the internal name.
            let internal = metaspace
                .class_name_at_mirror(reference(&args[0]))
                .expect("Class.getName: no class at this mirror")
                .to_string();
            let dotted = internal.replace('/', ".");
            Some(Value::Reference(strings::intern(metaspace, heap, &dotted)))
        }
        ("java/lang/Class", "getSimpleName", "()Ljava/lang/String;") => {
            // The source-level simple name: the segment after the last '/' (package) and last
            // '$' (nesting). Arrays report the component's simple name plus "[]" per dimension
            // ("[I" → "int[]", "[Ljava/lang/String;" → "String[]"); a primitive mirror's
            // internal name is already its simple name ("int").
            let internal = metaspace
                .class_name_at_mirror(reference(&args[0]))
                .expect("Class.getSimpleName: no class at this mirror")
                .to_string();
            let dims = internal.bytes().take_while(|&b| b == b'[').count();
            let element = &internal[dims..];
            let element =
                element.strip_prefix('L').and_then(|e| e.strip_suffix(';')).unwrap_or(element);
            let base = match (dims, element) {
                (1.., "I") => "int",
                (1.., "J") => "long",
                (1.., "D") => "double",
                (1.., "F") => "float",
                (1.., "S") => "short",
                (1.., "B") => "byte",
                (1.., "C") => "char",
                (1.., "Z") => "boolean",
                _ => element.rsplit(|c| c == '/' || c == '$').next().unwrap_or(element),
            };
            let simple = format!("{base}{}", "[]".repeat(dims));
            Some(Value::Reference(strings::intern(metaspace, heap, &simple)))
        }
        // --- Class: la capa de METADATOS -----------------------------------------
        //
        // Todo lo que sigue sale del archivo de clase y de nada mas: flags, superclase,
        // interfaces, tipo componente. Es la mitad de `java.lang.Class` que no necesita el
        // modelo de objetos de `java.lang.reflect`.
        //
        // Los que el JDK declara `native` -- `isInstance`, `getSuperclass`, `isAssignableFrom`,
        // `isHidden` -- se llaman igual, para que la superficie publica coincida hasta el
        // modificador. El resto son costuras **privadas** con sufijo `0`: la logica que se puede
        // escribir en Java se escribe en Java, que es la misma regla que dejo a `String` con
        // cuatro costuras privadas y toda su API en el lenguaje.

        ("java/lang/Class", "name0", "()Ljava/lang/String;") => {
            // El nombre INTERNO, crudo, con barras: `java/lang/String`, `[I`, `int`. El lado
            // Java le pone los puntos para `getName` y lo corta por la barra para el paquete,
            // asi que la costura entrega la forma sin decidir nada.
            let internal = mirror_name(metaspace, reference(&args[0]));
            Some(Value::Reference(strings::intern(metaspace, heap, &internal)))
        }

        ("java/lang/Class", "getSuperclass", "()Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            // Un array hereda de Object; un primitivo no hereda de nada, y una interfaz tampoco
            // -- `Runnable.class.getSuperclass()` es null, no Object.
            let parent = if name.starts_with('[') {
                Some("java/lang/Object".to_string())
            } else if is_primitive_name(&name) {
                None
            } else {
                metaspace.get_or_load(&name).and_then(|cf| {
                    if cf.is_interface() || cf.super_class == 0 {
                        None
                    } else {
                        cf.class_name(cf.super_class).map(str::to_string)
                    }
                })
            };
            let mirror = match parent {
                Some(p) => mirror_for(metaspace, heap, &p),
                None => 0,
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "isAssignableFrom", "(Ljava/lang/Class;)Z") => {
            let target = mirror_name(metaspace, reference(&args[0]));
            let candidate = mirror_name(metaspace, reference(&args[1]));
            // La identidad primero, y es la unica respuesta posible entre primitivos: `int` no
            // participa de ninguna jerarquia, asi que solo es asignable a si mismo.
            let assignable = candidate == target
                || (!is_primitive_name(&target)
                    && !is_primitive_name(&candidate)
                    && class_operations::is_subtype(metaspace, &candidate, &target));
            Some(Value::Int(assignable as i32))
        }

        // No fabricamos clases ocultas (las de `Lookup.defineHiddenClass`), asi que ninguna lo es.
        ("java/lang/Class", "isHidden", "()Z") => Some(Value::Int(0)),

        ("java/lang/Class", "modifiers0", "()I") => {
            const PUBLIC: u16 = 0x0001;
            const FINAL: u16 = 0x0010;
            const SUPER: u16 = 0x0020;
            const ABSTRACT: u16 = 0x0400;
            let name = mirror_name(metaspace, reference(&args[0]));
            let flags = if is_primitive_name(&name) {
                PUBLIC | FINAL | ABSTRACT
            } else if name.starts_with('[') {
                // Un array toma la VISIBILIDAD de su componente y es siempre final y abstracto:
                // `String[]` es publico porque `String` lo es, y un componente package-private
                // hace package-private al array.
                let component = component_name(&name);
                let visibility = match component {
                    Some(ref c) if !is_primitive_name(c) && !c.starts_with('[') => {
                        metaspace.get_or_load(c).map(|cf| cf.access_flags & 0x0007).unwrap_or(PUBLIC)
                    }
                    _ => PUBLIC,
                };
                visibility | FINAL | ABSTRACT
            } else {
                // ACC_SUPER se saca: no es un modificador del lenguaje (la especificacion dice
                // que se ignore) y comparte bit con `synchronized`, asi que dejarlo haria que
                // `Modifier.toString` imprimiera una clase "synchronized".
                metaspace.get_or_load(&name).map(|cf| cf.access_flags).unwrap_or(0) & !SUPER
            };
            Some(Value::Int(flags as i32))
        }

        ("java/lang/Class", "isPrimitive0", "()Z") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            Some(Value::Int(is_primitive_name(&name) as i32))
        }

        ("java/lang/Class", "interfaces0", "()[Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            // Todo array implementa Cloneable y Serializable (JLS §10.7), y en ese orden; un
            // primitivo no implementa nada.
            let names: Vec<String> = if name.starts_with('[') {
                vec!["java/lang/Cloneable".to_string(), "java/io/Serializable".to_string()]
            } else if is_primitive_name(&name) {
                Vec::new()
            } else {
                metaspace
                    .get_or_load(&name)
                    .map(|cf| {
                        cf.interfaces
                            .iter()
                            .filter_map(|&i| cf.class_name(i).map(str::to_string))
                            .collect()
                    })
                    .unwrap_or_default()
            };
            let mut mirrors = Vec::with_capacity(names.len());
            for n in &names {
                mirrors.push(mirror_for(metaspace, heap, n));
            }
            Some(Value::Reference(reference_array(
                metaspace,
                heap,
                "[Ljava/lang/Class;",
                &mirrors,
            )))
        }

        ("java/lang/Class", "componentType0", "()Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let mirror = match component_name(&name) {
                Some(component) => mirror_for(metaspace, heap, &component),
                None => 0, // no es un array
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "arrayType0", "()Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let array_class = format!("[{}", descriptor_of(&name));
            let mirror = mirror_for(metaspace, heap, &array_class);
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "forName0", "(Ljava/lang/String;)Ljava/lang/Class;") => {
            // El nombre llega en forma binaria con puntos ("java.lang.String") o ya como
            // descriptor de array ("[I", "[Ljava.lang.String;"). Devolver 0 y no panicar es
            // deliberado: el que no exista es una respuesta normal, y el lado Java la convierte
            // en ClassNotFoundException.
            let dotted = strings::read(heap, reference(&args[0]));
            let internal = dotted.replace('.', "/");
            if internal.starts_with('[') {
                return Some(Value::Reference(mirror_for(metaspace, heap, &internal)));
            }
            if metaspace.get_or_load(&internal).is_none() {
                return Some(Value::Reference(0));
            }
            Some(Value::Reference(mirror_for(metaspace, heap, &internal)))
        }

        // --- Class: los ATRIBUTOS del archivo de clase ---------------------------
        //
        // Todo lo que sigue sale de atributos que `ClassFile` guarda en crudo y nadie leia:
        // `InnerClasses`, `EnclosingMethod`, `NestHost`, `NestMembers`, `PermittedSubclasses` y
        // `Record`. Son la unica fuente de la estructura que el LENGUAJE tiene y el archivo de
        // clase pierde: un `.class` no sabe que estaba adentro de otro -- los dos son archivos
        // sueltos con un `$` en el nombre --, y estos atributos son lo que reconstruye eso.

        // --- ClassLoader: definir y encontrar --------------------------------------

        ("java/lang/ClassLoader", "defineClass0",
            "(Ljava/lang/String;[BII)Ljava/lang/Class;") => {
            // Un `.class` que llega como bytes en vez de como archivo. Es la unica forma de
            // meter una clase en la VM sin pasar por el classpath, y es de lo que viven los
            // proxies dinamicos y los generadores de bytecode.
            // Estatica: los argumentos arrancan en 0, sin receptor delante.
            let bytes: Vec<u8> = {
                let array = reference(&args[1]);
                if array == 0 {
                    return Some(Value::Reference(0));
                }
                let offset = int(&args[2]) as usize;
                let length = int(&args[3]) as usize;
                (0..length)
                    .map(|i| {
                        heap.read_u8(array + array_operations::ARRAY_HEADER_SIZE + offset + i)
                    })
                    .collect()
            };
            let Ok(class) = ClassFile::from_bytes(&bytes) else {
                return Some(Value::Reference(0));
            };
            let Some(internal) = class.class_name(class.this_class).map(str::to_string) else {
                return Some(Value::Reference(0));
            };
            // El nombre que el llamador dijo tiene que coincidir con el que el archivo dice; si
            // no, es un `NoClassDefFoundError` en el JDK y acá un cero que el lado Java traduce.
            let asked = reference(&args[0]);
            if asked != 0 {
                let dotted = strings::read(heap, asked);
                if dotted.replace('.', "/") != internal {
                    return Some(Value::Reference(0));
                }
            }
            metaspace.add(internal.clone(), class);
            class_operations::load_class(metaspace, heap, &internal);
            Some(Value::Reference(mirror_for(metaspace, heap, &internal)))
        }

        ("java/lang/Class", "nestHost0", "()Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let host = attribute_class(metaspace, &name, "NestHost", 0);
            let mirror = match host {
                Some(h) => mirror_for(metaspace, heap, &h),
                // Sin atributo, una clase es su propio nido. No es un valor por defecto
                // arbitrario: una clase de nivel superior sin anidados ES un nido de uno.
                None => reference(&args[0]),
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "nestMembers0", "()[Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let members = attribute_class_list(metaspace, &name, "NestMembers");
            let mut mirrors = vec![reference(&args[0])];
            for m in &members {
                if *m != name {
                    mirrors.push(mirror_for(metaspace, heap, m));
                }
            }
            Some(Value::Reference(reference_array(metaspace, heap, "[Ljava/lang/Class;", &mirrors)))
        }

        ("java/lang/Class", "permittedSubclasses0", "()[Ljava/lang/Class;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let permitted = attribute_class_list(metaspace, &name, "PermittedSubclasses");
            if !has_attribute(metaspace, &name, "PermittedSubclasses") {
                // Null y no un array vacio: "no es sellada" y "es sellada y no permite a nadie"
                // son cosas distintas, y `isSealed` se apoya en esa diferencia.
                return Some(Value::Reference(0));
            }
            let mut mirrors = Vec::with_capacity(permitted.len());
            for p in &permitted {
                mirrors.push(mirror_for(metaspace, heap, p));
            }
            Some(Value::Reference(reference_array(metaspace, heap, "[Ljava/lang/Class;", &mirrors)))
        }

        ("java/lang/Class", "declaringClass0", "()Ljava/lang/Class;") => {
            // La clase que DECLARA a esta, o 0. Una local o anonima no tiene: su entrada de
            // `InnerClasses` deja el `outer_class_info` en cero, que es exactamente como el
            // archivo de clase distingue "anidada" de "declarada adentro de un metodo".
            let name = mirror_name(metaspace, reference(&args[0]));
            let outer = inner_class_entry(metaspace, &name).and_then(|e| e.1);
            let mirror = match outer {
                Some(o) => mirror_for(metaspace, heap, &o),
                None => 0,
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "enclosingClass0", "()Ljava/lang/Class;") => {
            // La que la ENCIERRA, que para una local o anonima es la del `EnclosingMethod` y
            // para una anidada es la que la declara.
            let name = mirror_name(metaspace, reference(&args[0]));
            let enclosing = attribute_class(metaspace, &name, "EnclosingMethod", 0)
                .or_else(|| inner_class_entry(metaspace, &name).and_then(|e| e.1));
            let mirror = match enclosing {
                Some(e) => mirror_for(metaspace, heap, &e),
                None => 0,
            };
            Some(Value::Reference(mirror))
        }

        ("java/lang/Class", "innerName0", "()Ljava/lang/String;") => {
            // El nombre simple que el FUENTE le dio, o 0 si es anonima. Es la unica forma exacta
            // de contestar `getSimpleName`, `isAnonymousClass` y `isMemberClass`: derivarlo del
            // `$` del nombre binario acierta casi siempre y falla con una clase de nivel superior
            // que de verdad se llame `A$B`.
            let name = mirror_name(metaspace, reference(&args[0]));
            match inner_class_entry(metaspace, &name).and_then(|e| e.2) {
                Some(simple) => {
                    Some(Value::Reference(strings::intern(metaspace, heap, &simple)))
                }
                None => Some(Value::Reference(0)),
            }
        }

        ("java/lang/Class", "isInnerClass0", "()Z") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            Some(Value::Int(inner_class_entry(metaspace, &name).is_some() as i32))
        }

        ("java/lang/Class", "hasEnclosingMethod0", "()Z") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let present = attribute_body(metaspace, &name, "EnclosingMethod")
                .map(|b| b.len() >= 4 && (b[2] != 0 || b[3] != 0))
                .unwrap_or(false);
            Some(Value::Int(present as i32))
        }

        ("java/lang/Class", "enclosingMethodInfo0", "()[Ljava/lang/String;") => {
            // `{clase, nombre, descriptor}` del metodo que encierra a una clase local o anonima,
            // o 0. El atributo `EnclosingMethod` guarda la clase siempre y el metodo solo cuando
            // la clase nacio adentro de uno -- una anonima declarada en un inicializador de campo
            // tiene clase y no tiene metodo, y ese cero es informacion, no un hueco.
            let name = mirror_name(metaspace, reference(&args[0]));
            let indices = {
                let Some(body) = attribute_body(metaspace, &name, "EnclosingMethod") else {
                    return Some(Value::Reference(0));
                };
                if body.len() < 4 {
                    return Some(Value::Reference(0));
                }
                (
                    u16::from_be_bytes([body[0], body[1]]),
                    u16::from_be_bytes([body[2], body[3]]),
                )
            };
            if indices.1 == 0 {
                return Some(Value::Reference(0));
            }
            let (owner, method, descriptor) = {
                let Some(class) = metaspace.get(&name) else {
                    return Some(Value::Reference(0));
                };
                let Some(owner) = class.class_name(indices.0).map(str::to_string) else {
                    return Some(Value::Reference(0));
                };
                let Some((m, d)) = class.name_and_type(indices.1) else {
                    return Some(Value::Reference(0));
                };
                (owner, m.to_string(), d.to_string())
            };
            let parts = vec![
                strings::intern(metaspace, heap, &owner),
                strings::intern(metaspace, heap, &method),
                strings::intern(metaspace, heap, &descriptor),
            ];
            Some(Value::Reference(reference_array(
                metaspace,
                heap,
                "[Ljava/lang/String;",
                &parts,
            )))
        }

        ("java/lang/Class", "declaredClasses0", "()[Ljava/lang/Class;") => {
            // Las que ESTA declara: las entradas de su propio `InnerClasses` cuyo
            // `outer_class_info` es ella. La tabla trae tambien las clases anidadas que la clase
            // solo MENCIONA -- por eso hay que filtrar por el outer y no tomarla entera.
            let name = mirror_name(metaspace, reference(&args[0]));
            let declared = declared_inner_classes(metaspace, &name);
            let mut mirrors = Vec::with_capacity(declared.len());
            for d in &declared {
                mirrors.push(mirror_for(metaspace, heap, d));
            }
            Some(Value::Reference(reference_array(metaspace, heap, "[Ljava/lang/Class;", &mirrors)))
        }

        ("java/lang/Class", "recordComponents0",
            "()[Ljava/lang/reflect/RecordComponent;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let Some(components) = record_components(metaspace, &name) else {
                return Some(Value::Reference(0)); // no es un record
            };
            class_operations::load_class(metaspace, heap, "java/lang/reflect/RecordComponent");
            let empty: Vec<usize> = vec![0; components.len()];
            let array =
                reference_array(metaspace, heap, "[Ljava/lang/reflect/RecordComponent;", &empty);
            let owner = reference(&args[0]);
            for (slot, (component, descriptor)) in components.into_iter().enumerate() {
                let Some(object) = objects_operations::try_allocate(
                    metaspace,
                    heap,
                    "java/lang/reflect/RecordComponent",
                ) else {
                    break;
                };
                let at = array_operations::ARRAY_HEADER_SIZE + slot * SLOT_SIZE;
                heap.store_reference(array, array + at, object);
                let interned = strings::intern(metaspace, heap, &component);
                let type_mirror = mirror_for(metaspace, heap, &internal_name_of(&descriptor));
                const RC: &str = "java/lang/reflect/RecordComponent";
                let clazz_at = field_offset(metaspace, RC, "clazz");
                let name_at = field_offset(metaspace, RC, "name");
                let type_at = field_offset(metaspace, RC, "type");
                heap.store_reference(object, object + clazz_at, owner);
                heap.store_reference(object, object + name_at, interned);
                heap.store_reference(object, object + type_at, type_mirror);
            }
            Some(Value::Reference(array))
        }

        ("java/lang/Class", "declaredConstructors0",
            "()[Ljava/lang/reflect/Constructor;") => {
            // Un constructor es un metodo llamado `<init>` y nada mas: la reflexion lo presenta
            // como otra cosa porque se invoca de otra forma -- aloca antes de correr --, pero en
            // el archivo de clase vive en la misma tabla que los demas.
            let name = mirror_name(metaspace, reference(&args[0]));
            let declared: Vec<(String, u16, Vec<String>)> =
                if name.starts_with('[') || is_primitive_name(&name) {
                    Vec::new()
                } else {
                    metaspace
                        .get_or_load(&name)
                        .map(|cf| {
                            cf.methods
                                .iter()
                                .filter_map(|m| {
                                    if cf.utf8(m.name_index)? != "<init>" {
                                        return None;
                                    }
                                    let d = cf.utf8(m.descriptor_index)?.to_string();
                                    Some((d, m.access_flags, declared_exceptions(cf, m)))
                                })
                                .collect()
                        })
                        .unwrap_or_default()
                };
            class_operations::load_class(metaspace, heap, "java/lang/reflect/Constructor");
            let empty: Vec<usize> = vec![0; declared.len()];
            let array =
                reference_array(metaspace, heap, "[Ljava/lang/reflect/Constructor;", &empty);
            let owner = reference(&args[0]);
            for (slot, (descriptor, flags, throws)) in declared.into_iter().enumerate() {
                let Some(object) = objects_operations::try_allocate(
                    metaspace,
                    heap,
                    "java/lang/reflect/Constructor",
                ) else {
                    break;
                };
                let at = array_operations::ARRAY_HEADER_SIZE + slot * SLOT_SIZE;
                heap.store_reference(array, array + at, object);

                let (parameters, _) = split_descriptor(&descriptor);
                let mut parameter_mirrors = Vec::with_capacity(parameters.len());
                for p in &parameters {
                    parameter_mirrors.push(mirror_for(metaspace, heap, p));
                }
                let parameter_array =
                    reference_array(metaspace, heap, "[Ljava/lang/Class;", &parameter_mirrors);
                let mut throws_mirrors = Vec::with_capacity(throws.len());
                for t in &throws {
                    throws_mirrors.push(mirror_for(metaspace, heap, t));
                }
                let throws_array =
                    reference_array(metaspace, heap, "[Ljava/lang/Class;", &throws_mirrors);

                const CTOR: &str = "java/lang/reflect/Constructor";
                let clazz_at = field_offset(metaspace, CTOR, "clazz");
                let params_at = field_offset(metaspace, CTOR, "parameterTypes");
                let throws_at = field_offset(metaspace, CTOR, "exceptionTypes");
                let mods_at = field_offset(metaspace, CTOR, "modifiers");
                let slot_at = field_offset(metaspace, CTOR, "slot");
                heap.store_reference(object, object + clazz_at, owner);
                heap.store_reference(object, object + params_at, parameter_array);
                heap.store_reference(object, object + throws_at, throws_array);
                heap.write_u32(object + mods_at, flags as u32);
                heap.write_u32(object + slot_at, slot as u32);
            }
            Some(Value::Reference(array))
        }

        ("java/lang/Class", "declaredMethods0", "()[Ljava/lang/reflect/Method;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            // `<init>` y `<clinit>` no son metodos para la reflexion: el primero sale por
            // `getDeclaredConstructors` y el segundo no sale por ningun lado, porque nadie
            // puede llamarlo.
            let declared: Vec<(String, String, u16, Vec<String>)> =
                if name.starts_with('[') || is_primitive_name(&name) {
                    Vec::new()
                } else {
                    metaspace
                        .get_or_load(&name)
                        .map(|cf| {
                            cf.methods
                                .iter()
                                .filter_map(|m| {
                                    let n = cf.utf8(m.name_index)?.to_string();
                                    if n == "<init>" || n == "<clinit>" {
                                        return None;
                                    }
                                    let d = cf.utf8(m.descriptor_index)?.to_string();
                                    Some((n, d, m.access_flags, declared_exceptions(cf, m)))
                                })
                                .collect()
                        })
                        .unwrap_or_default()
                };
            class_operations::load_class(metaspace, heap, "java/lang/reflect/Method");
            // El array primero y en Old, cada `Method` adentro apenas se crea: ver
            // `declaredFields0` para por que el orden inverso los dejaria sin raiz.
            let empty: Vec<usize> = vec![0; declared.len()];
            let array = reference_array(metaspace, heap, "[Ljava/lang/reflect/Method;", &empty);
            let owner = reference(&args[0]);
            for (slot, (method_name, descriptor, flags, throws)) in
                declared.into_iter().enumerate()
            {
                let Some(object) =
                    objects_operations::try_allocate(metaspace, heap, "java/lang/reflect/Method")
                else {
                    break;
                };
                let at = array_operations::ARRAY_HEADER_SIZE + slot * SLOT_SIZE;
                heap.store_reference(array, array + at, object);

                let (parameters, returns) = split_descriptor(&descriptor);
                let interned = strings::intern(metaspace, heap, &method_name);
                let return_mirror = mirror_for(metaspace, heap, &returns);
                let mut parameter_mirrors = Vec::with_capacity(parameters.len());
                for p in &parameters {
                    parameter_mirrors.push(mirror_for(metaspace, heap, p));
                }
                let parameter_array =
                    reference_array(metaspace, heap, "[Ljava/lang/Class;", &parameter_mirrors);
                let mut throws_mirrors = Vec::with_capacity(throws.len());
                for t in &throws {
                    throws_mirrors.push(mirror_for(metaspace, heap, t));
                }
                let throws_array =
                    reference_array(metaspace, heap, "[Ljava/lang/Class;", &throws_mirrors);

                let clazz_at = field_offset(metaspace, "java/lang/reflect/Method", "clazz");
                let name_at = field_offset(metaspace, "java/lang/reflect/Method", "name");
                let ret_at = field_offset(metaspace, "java/lang/reflect/Method", "returnType");
                let params_at =
                    field_offset(metaspace, "java/lang/reflect/Method", "parameterTypes");
                let throws_at =
                    field_offset(metaspace, "java/lang/reflect/Method", "exceptionTypes");
                let mods_at = field_offset(metaspace, "java/lang/reflect/Method", "modifiers");
                let slot_at = field_offset(metaspace, "java/lang/reflect/Method", "slot");
                heap.store_reference(object, object + clazz_at, owner);
                heap.store_reference(object, object + name_at, interned);
                heap.store_reference(object, object + ret_at, return_mirror);
                heap.store_reference(object, object + params_at, parameter_array);
                heap.store_reference(object, object + throws_at, throws_array);
                heap.write_u32(object + mods_at, flags as u32);
                heap.write_u32(object + slot_at, slot as u32);
            }
            Some(Value::Reference(array))
        }

        ("java/lang/Class", "declaredFields0", "()[Ljava/lang/reflect/Field;") => {
            let name = mirror_name(metaspace, reference(&args[0]));
            let declared: Vec<(String, String, u16)> = if name.starts_with('[')
                || is_primitive_name(&name)
            {
                Vec::new()
            } else {
                metaspace
                    .get_or_load(&name)
                    .map(|cf| {
                        cf.fields
                            .iter()
                            .filter_map(|f| {
                                let n = cf.utf8(f.name_index)?.to_string();
                                let d = cf.utf8(f.descriptor_index)?.to_string();
                                Some((n, d, f.access_flags))
                            })
                            .collect()
                    })
                    .unwrap_or_default()
            };
            class_operations::load_class(metaspace, heap, "java/lang/reflect/Field");
            // El array se aloca PRIMERO y en Old, y cada `Field` entra por el barrier apenas se
            // crea. Al reves -- juntar los `Field` en un `Vec` de Rust y armar el array despues --
            // los dejaria sin raiz: son objetos jovenes que solo un `Vec` de Rust conoce, y el
            // primer minor GC de la vuelta siguiente los moveria debajo de nuestros pies.
            let empty: Vec<usize> = vec![0; declared.len()];
            let array =
                reference_array(metaspace, heap, "[Ljava/lang/reflect/Field;", &empty);
            let owner = reference(&args[0]);
            for (slot, (field_name, descriptor, flags)) in declared.into_iter().enumerate() {
                let Some(object) =
                    objects_operations::try_allocate(metaspace, heap, "java/lang/reflect/Field")
                else {
                    break;
                };
                let at = array_operations::ARRAY_HEADER_SIZE + slot * SLOT_SIZE;
                heap.store_reference(array, array + at, object);
                let interned = strings::intern(metaspace, heap, &field_name);
                let type_mirror = mirror_for(metaspace, heap, &internal_name_of(&descriptor));
                let clazz_at = field_offset(metaspace, "java/lang/reflect/Field", "clazz");
                let name_at = field_offset(metaspace, "java/lang/reflect/Field", "name");
                let type_at = field_offset(metaspace, "java/lang/reflect/Field", "type");
                let mods_at = field_offset(metaspace, "java/lang/reflect/Field", "modifiers");
                let slot_at = field_offset(metaspace, "java/lang/reflect/Field", "slot");
                heap.store_reference(object, object + clazz_at, owner);
                heap.store_reference(object, object + name_at, interned);
                heap.store_reference(object, object + type_at, type_mirror);
                heap.write_u32(object + mods_at, flags as u32);
                heap.write_u32(object + slot_at, slot as u32);
            }
            Some(Value::Reference(array))
        }

        // --- reflective field access (java.lang.reflect.Field) -------------------------------
        // The typed raw seams the Field's Java layer calls after its own type-check/widening/boxing.
        // Each locates the field's slot from the Field object (its clazz/name/modifiers) and the
        // target: a static field lives on the owner's Class mirror, an instance field at the target's
        // offset. `getInt0` covers every 4-byte field (boolean/byte/short/char/int, and float via its
        // bit pattern); `getLong0` the 8-byte ones; `getReference0` the object fields. The width the
        // native reads is fixed per method — the Java side already picked the right one from the type.
        ("java/lang/reflect/Field", "getInt0", "(Ljava/lang/Object;)I") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            Some(Value::Int(heap.read_u32(at) as i32))
        }
        ("java/lang/reflect/Field", "getLong0", "(Ljava/lang/Object;)J") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            Some(Value::Long(heap.read_u64(at) as i64))
        }
        ("java/lang/reflect/Field", "getReference0", "(Ljava/lang/Object;)Ljava/lang/Object;") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            Some(Value::Reference(heap.read_u32(at) as usize))
        }
        ("java/lang/reflect/Field", "setInt0", "(Ljava/lang/Object;I)V") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            heap.write_u32(at, int(&args[2]) as u32);
            None
        }
        ("java/lang/reflect/Field", "setLong0", "(Ljava/lang/Object;J)V") => {
            let (at, _) = field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            heap.write_u64(at, long(&args[2]) as u64);
            None
        }
        ("java/lang/reflect/Field", "setReference0", "(Ljava/lang/Object;Ljava/lang/Object;)V") => {
            let (at, holder) =
                field_slot_addr(metaspace, heap, reference(&args[0]), reference(&args[1]));
            // A reference store goes through the write barrier so the GC learns of the new edge.
            heap.store_reference(holder, at, reference(&args[2]));
            None
        }

        ("java/lang/Class", "getPrimitiveClass", "(Ljava/lang/String;)Ljava/lang/Class;") => {
            // The primitive type's `Class` mirror — `int.class` compiles to `getstatic
            // Integer.TYPE`, whose `<clinit>` calls this. A header-only mirror (like an array
            // class's), keyed and cached by the type name so `int.class == int.class`. Old-pinned:
            // a primitive mirror is permanent, like any `Class`.
            let type_name = strings::read(heap, reference(&args[0]));
            let uuid = metaspace.class_id(&type_name).to_string();
            let mirror = match metaspace.class_object(&uuid) {
                Some(offset) => offset,
                None => {
                    let offset = heap.malloc_old(HEADER_SIZE);
                    metaspace.set_class_object(&uuid, offset);
                    // The mirror is itself an instance of `java.lang.Class`, so its header's
                    // `class_id` points at `Class`'s mirror — that's what makes `invokevirtual`
                    // on it (`descriptorString`, `getClass`, …) dispatch correctly.
                    class_operations::load_class(metaspace, heap, "java/lang/Class");
                    let class_uuid = metaspace.class_id("java/lang/Class").to_string();
                    let class_mirror = metaspace.class_object(&class_uuid).unwrap_or(0);
                    heap.write_u32(offset, class_mirror as u32);
                    offset
                }
            };
            Some(Value::Reference(mirror))
        }

        // --- APT fase 3: el modelo de elementos reificado (ver `super::apt`) -----
        // El receptor es un `jdk/internal/apt/SymElement`: su campo `int sym` es el `SymbolId`
        // en la tabla de `javac` (viva en este mismo proceso, atada con `JVM::set_apt`). Leemos
        // ese id, indexamos la tabla y respondemos contra la tabla del compilador. Estos tres son
        // native (no intrínsecos): no corren `<clinit>` ni re-entran al intérprete —construyen un
        // objeto a mano, el mismo patrón que `materialize_method_type`—. Los que sí lo necesitan
        // (`getKind`/`getEnclosedElements`) los intercepta el intérprete antes de llegar acá.
        //
        // `getSimpleName`: el `Symbol.name` (`"Foo"`), envuelto en un `Name` (`SymName`) —el
        // contrato de `Element` devuelve `Name`, no `String`—.
        ("jdk/internal/apt/SymElement", "getSimpleName", "()Ljavax/lang/model/element/Name;") => {
            let sym = sym_of(&args[0], metaspace, heap);
            let text = apt_ref(apt).table().symbol(sym).name.clone();
            Some(Value::Reference(make_name(metaspace, heap, &text)))
        }
        // `getQualifiedName`: el *binary name* de un tipo con el `$` del anidamiento vuelto `.`
        // (`"a.b.Outer$Inner"` → `"a.b.Outer.Inner"`), también como un `Name`. Para un símbolo que
        // no es una clase (no debería pasar: sólo `TypeElement` lo declara) cae al nombre simple.
        ("jdk/internal/apt/SymElement", "getQualifiedName", "()Ljavax/lang/model/element/Name;") => {
            let sym = sym_of(&args[0], metaspace, heap);
            let symbol = apt_ref(apt).table().symbol(sym);
            let text = match &symbol.kind {
                crate::javac::symbol::SymbolKind::Class { binary, .. } => binary.replace('$', "."),
                _ => symbol.name.clone(),
            };
            Some(Value::Reference(make_name(metaspace, heap, &text)))
        }
        // `getEnclosingElement`: el `Symbol.owner` reificado —la clase de un miembro, el paquete de
        // un tipo top-level— vía `element_for`, así que hereda su **caché de identidad**: dos hijos
        // del mismo dueño devuelven el **mismo** objeto (`a.getEnclosingElement() ==
        // b.getEnclosingElement()`). `null` (referencia 0) si el símbolo no tiene dueño.
        ("jdk/internal/apt/SymElement", "getEnclosingElement", "()Ljavax/lang/model/element/Element;") => {
            let sym = sym_of(&args[0], metaspace, heap);
            let owner = apt_ref(apt).table().symbol(sym).owner;
            let element = match owner {
                Some(owner) => apt.as_mut().expect("AptContext atado").element_for(owner, metaspace, heap),
                None => 0,
            };
            Some(Value::Reference(element))
        }

        // El reloj de pared, en milisegundos desde la epoca. Declarado hace rato del lado
        // Java y sin implementar de este; lo destapo `java.util.Random`, cuyo constructor sin
        // argumentos se siembra de aca.
        ("java/lang/System", "currentTimeMillis", "()J") => {
            let since = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .map(|d| d.as_millis() as i64)
                .unwrap_or(0);
            Some(Value::Long(since))
        }

        // --- las propiedades del sistema ---------------------------------------
        //
        // Que propiedades existen es cosa de la implementacion, no de la especificacion: el JDK
        // documenta un conjunto minimo y cada plataforma agrega lo suyo. Estas son las que esta
        // VM puede responder de verdad; para cualquier otra clave devuelve `null`, que es
        // exactamente lo que el contrato dice para una clave ausente.
        ("java/lang/System", "getProperty0", "(Ljava/lang/String;)Ljava/lang/String;") => {
            let key = strings::read(heap, reference(&args[0]));
            let value = match key.as_str() {
                "java.version" | "java.specification.version" => Some("25"),
                "java.vm.specification.version" => Some("25"),
                "java.vm.name" | "java.vendor" => Some("KajiJDK"),
                "line.separator" => Some(if cfg!(windows) { "\r\n" } else { "\n" }),
                "file.separator" => Some(if cfg!(windows) { "\\" } else { "/" }),
                "path.separator" => Some(if cfg!(windows) { ";" } else { ":" }),
                "os.name" => Some(std::env::consts::OS),
                "os.arch" => Some(std::env::consts::ARCH),
                "native.encoding" | "file.encoding" => Some("UTF-8"),
                _ => None,
            };
            match value {
                Some(text) => Some(Value::Reference(strings::intern(metaspace, heap, text))),
                None => Some(Value::Reference(0)),
            }
        }

        // --- las costuras de bits de Double/Float -------------------------------
        //
        // Reinterpretar los bits de un flotante es lo unico que el bytecode NO puede hacer: no
        // hay opcode que lea un `double` como un `long` sin convertir el VALOR. De ahi que sean
        // nativas — y que lo sean en el JDK tambien, con estos mismos nombres.
        //
        // `raw` significa que un NaN se devuelve tal cual, con su carga util; la canonicalizacion
        // a 0x7ff8000000000000 la hace `doubleToLongBits`, que es Java y esta en la biblioteca.
        ("java/lang/Double", "doubleToRawLongBits" | "doubleToLongBits", "(D)J") => {
            Some(Value::Long(double(&args[0]).to_bits() as i64))
        }
        ("java/lang/Double", "longBitsToDouble", "(J)D") => {
            Some(Value::Double(f64::from_bits(long(&args[0]) as u64)))
        }
        ("java/lang/Float", "floatToRawIntBits" | "floatToIntBits", "(F)I") => {
            Some(Value::Int(float(&args[0]).to_bits() as i32))
        }
        ("java/lang/Float", "intBitsToFloat", "(I)F") => {
            Some(Value::Float(f32::from_bits(int(&args[0]) as u32)))
        }

        // --- String -------------------------------------------------------------
        // Dos nombres para la misma costura. `rawLength` es la de KajiLibrary, privada, con el
        // String como parametro explicito; `length` es la que declara la copia compilada de
        // `boot/`, publica y de instancia. El cuerpo es el mismo porque el receptor de una y el
        // primer argumento de la otra ocupan la MISMA posicion, `args[0]`. Se aceptan las dos
        // mientras los dos arboles convivan (ver COMPILER_FINDINGS sobre boot/ vs KajiLibrary).
        ("java/lang/String", "rawLength" | "length", "(Ljava/lang/String;)I" | "()I") => {
            // The receiver is a heap String; its length word holds the UTF-8 byte count.
            Some(Value::Int(strings::length(heap, reference(&args[0])) as i32))
        }
        // charAt(int): the i-th byte (ASCII; our String is UTF-8, fine for ASCII).
        ("java/lang/String", "rawCharAt" | "charAt", "(Ljava/lang/String;I)C" | "(I)C") => {
            Some(Value::Int(strings::char_at(heap, reference(&args[0]), int(&args[1]) as usize) as i32))
        }
        // equals(Object): true if the other is a String with the same text.
        // valueOf(char[], offset, count): the seam KajiLibrary builds every String through —
        // `StringBuilder.toString`, `substring`, `Writer.write(String)`'s inverse, etc. A `char[]`
        // stores UTF-16 code units two bytes wide (after the 12-byte array header); we slice
        // `[offset, offset+count)`, decode them, and intern the text as a fresh heap String.
        // Los tres que KajiLibrary ya NO declara nativos -- los implementa en Java sobre
        // `charAt`, que ademas es lo que arregla el hash de las cadenas no-ASCII. Siguen aca
        // porque la copia de `boot/` los declara nativos y la VM los carga a ella en los tests.
        ("java/lang/String", "equals", "(Ljava/lang/Object;)Z") => {
            let other = reference(&args[1]);
            let equal = other != 0 && strings::read(heap, reference(&args[0])) == strings::read(heap, other);
            Some(Value::Int(equal as i32))
        }
        ("java/lang/String", "hashCode", "()I") => {
            let text = strings::read(heap, reference(&args[0]));
            let hash = text.chars().flat_map(|c| { let mut b = [0u16; 2]; c.encode_utf16(&mut b).to_vec() })
                .fold(0i32, |h, u| h.wrapping_mul(31).wrapping_add(u as i32));
            Some(Value::Int(hash))
        }
        ("java/lang/String", "startsWith", "(Ljava/lang/String;)Z") => {
            let text = strings::read(heap, reference(&args[0]));
            let prefix = strings::read(heap, reference(&args[1]));
            Some(Value::Int(text.starts_with(&prefix) as i32))
        }
        ("java/lang/String", "rawValueOf" | "valueOf", "([CII)Ljava/lang/String;") => {
            const ARRAY_HEADER: usize = 12; // object header (8) + length word (4)
            let array = reference(&args[0]);
            let start = int(&args[1]) as usize;
            let count = int(&args[2]) as usize;
            let units: Vec<u16> =
                (0..count).map(|i| heap.read_u16(array + ARRAY_HEADER + (start + i) * 2)).collect();
            // **Allocated, not pooled**: this is a String the program *computes* out of a
            // `char[]`, so it is a distinct object even when its contents equal a literal — which
            // is what makes `new String("a") == "a"` false, as JLS 3.10.5 requires.
            //
            // By UNITS, not through a Rust `String`. Going through one would be lossy in a way
            // Java can observe: `from_utf16_lossy` turns an unpaired surrogate into U+FFFD, and a
            // `char[]` is allowed to hold one. `new String(chars).charAt(0)` must answer 0xD800
            // when that is what was put in.
            Some(Value::Reference(strings::allocate_units(metaspace, heap, &units)))
        }

        // The CAS primitive (H5) — the atomic root of every lock-free counter. Compare the
        // `value` field to `expectedValue` (args[1]); if equal, set it to `newValue` (args[2]) and
        // return `true`. In `os` mode an `invokevirtual` escalates to the write path, so this
        // read-compare-write is exclusive (atomic) and correct; the retry loops in the Java
        // `AtomicInteger` build every other operation (`incrementAndGet`, …) on top of it.
        ("java/util/concurrent/atomic/AtomicInteger", "compareAndSet", "(II)Z") => {
            let at = reference(&args[0])
                + field_offset(metaspace, "java/util/concurrent/atomic/AtomicInteger", "value");
            let matched = heap.read_u32(at) as i32 == int(&args[1]);
            if matched {
                heap.write_u32(at, int(&args[2]) as u32);
            }
            Some(Value::Int(matched as i32))
        }
        ("java/util/concurrent/atomic/AtomicLong", "compareAndSet", "(JJ)Z") => {
            let at = reference(&args[0])
                + field_offset(metaspace, "java/util/concurrent/atomic/AtomicLong", "value");
            let matched = heap.read_u64(at) as i64 == long(&args[1]);
            if matched {
                heap.write_u64(at, long(&args[2]) as u64);
            }
            Some(Value::Int(matched as i32))
        }
        ("java/util/concurrent/atomic/AtomicReference", "compareAndSet", "(Ljava/lang/Object;Ljava/lang/Object;)Z") => {
            // Reference CAS: compares by **identity** (heap offset). On success the store goes
            // through the write barrier (an Old holder pointing at a young value must be remembered).
            let object = reference(&args[0]);
            let at = object + field_offset(metaspace, "java/util/concurrent/atomic/AtomicReference", "value");
            let matched = heap.read_u32(at) as usize == reference(&args[1]);
            if matched {
                heap.store_reference(object, at, reference(&args[2]));
            }
            Some(Value::Int(matched as i32))
        }

        // --- APT fase 4: registro de un archivo fuente del Filer ----------------
        // `KajiFiler.createSourceFile(name)` acaba acá: args[0] es el `KajiFiler` receptor, args[1]
        // el nombre (una `String` del heap) y args[2] el `StringWriter` recién creado que recibirá
        // el texto. Guardamos `(nombre, offset del writer)` en el canal lateral del hilo; el round
        // loop lo drena con `drain_filer` y recupera el texto con `read_generated_text`. Si no hay
        // Filer armado (`install_filer` no corrió), el registro se descarta en silencio.
        ("javax/annotation/processing/KajiFiler", "nativeRegisterSourceFile", "(Ljava/lang/String;Ljava/io/StringWriter;)V") => {
            let name = strings::read(heap, reference(&args[1]));
            let writer_ref = reference(&args[2]) as u32;
            FILER.with(|f| {
                if let Some(state) = f.borrow_mut().as_mut() {
                    state.pending.push((name, writer_ref));
                }
            });
            None
        }

        _ => panic!("no native implementation for {class}.{name}{descriptor}"),
    }
}

/// The `int` payload of an argument (a verifier-guaranteed `Int`).
// --- El vecindario de `java.lang.Class`: nombres, mirrors y descriptores -------------------------
//
// Un mirror es una identidad, no un objeto con campos: la VM guarda uno por clase cargada, y
// tambien por cada clase array sintetica y por cada primitivo (que no tienen archivo de clase
// ninguno). Estas cuatro funciones son la traduccion entre las tres formas en que un tipo se
// nombra -- nombre interno (`java/lang/String`, `[I`, `int`), descriptor (`Ljava/lang/String;`) y
// mirror -- y existen para que las nativas de arriba no la repitan cada una a su manera.

// --- Los atributos de clase que `ClassFile` guarda en crudo -------------------------------------
//
// `ClassFile` parsea el cuerpo de unos pocos atributos y deja los demas como bytes. Estos
// lectores son la otra mitad: cada uno conoce el formato de §4.7 del que le toca y nada mas.

/// El cuerpo crudo del atributo `wanted` de la clase `name`, si lo tiene.
fn attribute_body<'a>(
    metaspace: &'a mut MetaspaceService,
    name: &str,
    wanted: &str,
) -> Option<&'a [u8]> {
    let class = metaspace.get_or_load(name)?;
    class
        .attributes
        .iter()
        .find(|a| class.utf8(a.name_index) == Some(wanted))
        .map(|a| a.info.as_slice())
}

fn has_attribute(metaspace: &mut MetaspaceService, name: &str, wanted: &str) -> bool {
    attribute_body(metaspace, name, wanted).is_some()
}

/// The absolute heap offset of the slot a `Field` object names, and the object that HOLDS it (for
/// the write barrier). A static field's slot lives on the owner's `Class` mirror; an instance
/// field's at the target object's offset. Reads the Field's own `clazz`/`name`/`modifiers`.
fn field_slot_addr(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    field_obj: usize,
    target: usize,
) -> (usize, usize) {
    const F: &str = "java/lang/reflect/Field";
    let clazz_off = field_offset(metaspace, F, "clazz");
    let name_off = field_offset(metaspace, F, "name");
    let mods_off = field_offset(metaspace, F, "modifiers");
    let clazz_mirror = heap.read_u32(field_obj + clazz_off) as usize;
    let owner = metaspace
        .class_name_at_mirror(clazz_mirror)
        .expect("Field.clazz has no class")
        .to_string();
    let name_ref = heap.read_u32(field_obj + name_off) as usize;
    let name = strings::read(heap, name_ref);
    let mods = heap.read_u32(field_obj + mods_off);
    if mods & 0x0008 != 0 {
        // static: the slot is on the owner's Class mirror.
        let at = class_operations::static_slot(metaspace, heap, &owner, &name);
        let uuid = metaspace.class_id(&owner).to_string();
        let holder = metaspace.class_object(&uuid).unwrap_or(0);
        (at, holder)
    } else {
        // instance: the slot is at the target object's field offset.
        let at = target + field_offset(metaspace, &owner, &name);
        (at, target)
    }
}

/// Materialise every `RuntimeVisibleAnnotation` on class `this` as an object, returning their heap
/// offsets — backs `Class.declaredAnnotations0`. Each is an instance of a class the VM spins
/// implementing the @interface (see `annotation_factory`). Since annotation objects carry no
/// instance fields, `allocate` alone yields a complete object; the constructor (just `super()`)
/// need not run, which is what lets this stay a plain native without re-entering the interpreter.
fn annotation_objects(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    this: &str,
) -> Vec<usize> {
    // Resolve the attribute into an owned tree FIRST, so the class-file borrow ends before we begin
    // mutating the metaspace (spinning + loading the synthetic classes).
    let resolved: Vec<ResolvedAnnotation> = match metaspace.get_or_load(this) {
        Some(cf) => cf
            .attributes
            .iter()
            .find(|a| cf.utf8(a.name_index) == Some("RuntimeVisibleAnnotations"))
            .map(|a| annotations::resolve(cf, &a.info))
            .unwrap_or_default(),
        None => Vec::new(),
    };
    let mut objects = Vec::with_capacity(resolved.len());
    for (i, ann) in resolved.iter().enumerate() {
        let iface = ann
            .type_descriptor
            .strip_prefix('L')
            .and_then(|s| s.strip_suffix(';'))
            .unwrap_or(&ann.type_descriptor)
            .to_string();
        let elements = annotation_elements(metaspace, &iface, ann);
        // One spun class per (annotated class, annotation slot): stable, so repeated reflection
        // over the same class reuses it instead of minting a new class each call.
        let synthetic = format!("{this}$$Anno${i}");
        if metaspace.get(&synthetic).is_none() {
            let bytes = annotation_factory::generate_annotation_class(&synthetic, &iface, &elements);
            match ClassFile::from_bytes(&bytes) {
                Ok(class) => metaspace.add(synthetic.clone(), class),
                Err(_) => continue,
            }
        }
        class_operations::load_class(metaspace, heap, &synthetic);
        objects.push(objects_operations::allocate(metaspace, heap, &synthetic));
    }
    objects
}

/// The elements to give the factory for one annotation: every accessor the @interface declares,
/// paired with the value written at the use site or, absent that, the accessor's `AnnotationDefault`.
fn annotation_elements(
    metaspace: &mut MetaspaceService,
    iface: &str,
    ann: &ResolvedAnnotation,
) -> Vec<Element> {
    let cf = match metaspace.get_or_load(iface) {
        Some(cf) => cf,
        None => return Vec::new(),
    };
    let mut out = Vec::new();
    for m in &cf.methods {
        let (Some(name), Some(desc)) = (cf.utf8(m.name_index), cf.utf8(m.descriptor_index)) else {
            continue;
        };
        // Element accessors are non-static, parameterless, and not <init>/<clinit>.
        if m.is_static() || name.starts_with('<') || !desc.starts_with("()") {
            continue;
        }
        let value = if let Some((_, v)) = ann.elements.iter().find(|(n, _)| n.as_str() == name) {
            v.clone()
        } else if let Some(def) =
            m.attributes.iter().find(|a| cf.utf8(a.name_index) == Some("AnnotationDefault"))
        {
            match annotations::resolve_default(cf, &def.info) {
                Some(v) => v,
                None => continue,
            }
        } else {
            continue;
        };
        out.push(Element { name: name.to_string(), descriptor: desc.to_string(), value });
    }
    out
}

/// El nombre de clase que el atributo `wanted` nombra en el offset `at` de su cuerpo.
fn attribute_class(
    metaspace: &mut MetaspaceService,
    name: &str,
    wanted: &str,
    at: usize,
) -> Option<String> {
    let index = {
        let body = attribute_body(metaspace, name, wanted)?;
        if body.len() < at + 2 {
            return None;
        }
        u16::from_be_bytes([body[at], body[at + 1]])
    };
    let class = metaspace.get(name)?;
    class.class_name(index).map(str::to_string)
}

/// Los nombres de clase de un atributo con forma `u2 count` + `u2[] classes` -- que es la de
/// `NestMembers` y la de `PermittedSubclasses`.
fn attribute_class_list(
    metaspace: &mut MetaspaceService,
    name: &str,
    wanted: &str,
) -> Vec<String> {
    let indices = {
        let Some(body) = attribute_body(metaspace, name, wanted) else {
            return Vec::new();
        };
        if body.len() < 2 {
            return Vec::new();
        }
        let count = u16::from_be_bytes([body[0], body[1]]) as usize;
        (0..count)
            .filter_map(|i| {
                let at = 2 + i * 2;
                (at + 1 < body.len()).then(|| u16::from_be_bytes([body[at], body[at + 1]]))
            })
            .collect::<Vec<u16>>()
    };
    let Some(class) = metaspace.get(name) else {
        return Vec::new();
    };
    indices.iter().filter_map(|&i| class.class_name(i).map(str::to_string)).collect()
}

/// La entrada de `InnerClasses` que habla de la clase `name` MISMA, como
/// `(interna, externa, nombre simple)`. La externa es `None` para una local o anonima, y el
/// nombre simple es `None` para una anonima -- que es como el archivo de clase distingue los tres
/// casos sin decirlo con un flag.
fn inner_class_entry(
    metaspace: &mut MetaspaceService,
    name: &str,
) -> Option<(String, Option<String>, Option<String>)> {
    let entries = inner_classes(metaspace, name);
    entries.into_iter().find(|e| e.0 == name)
}

/// Toda la tabla `InnerClasses` de la clase `name`, decodificada.
fn inner_classes(
    metaspace: &mut MetaspaceService,
    name: &str,
) -> Vec<(String, Option<String>, Option<String>)> {
    let rows = {
        let Some(body) = attribute_body(metaspace, name, "InnerClasses") else {
            return Vec::new();
        };
        if body.len() < 2 {
            return Vec::new();
        }
        let count = u16::from_be_bytes([body[0], body[1]]) as usize;
        (0..count)
            .filter_map(|i| {
                let at = 2 + i * 8;
                if at + 5 >= body.len() {
                    return None;
                }
                Some((
                    u16::from_be_bytes([body[at], body[at + 1]]),
                    u16::from_be_bytes([body[at + 2], body[at + 3]]),
                    u16::from_be_bytes([body[at + 4], body[at + 5]]),
                ))
            })
            .collect::<Vec<(u16, u16, u16)>>()
    };
    let Some(class) = metaspace.get(name) else {
        return Vec::new();
    };
    rows.iter()
        .filter_map(|&(inner, outer, simple)| {
            let inner_name = class.class_name(inner)?.to_string();
            let outer_name = class.class_name(outer).map(str::to_string);
            let simple_name = class.utf8(simple).map(str::to_string);
            Some((inner_name, outer_name, simple_name))
        })
        .collect()
}

/// Las clases que `name` declara adentro: las entradas cuyo `outer_class_info` es ella.
fn declared_inner_classes(metaspace: &mut MetaspaceService, name: &str) -> Vec<String> {
    inner_classes(metaspace, name)
        .into_iter()
        .filter(|(inner, outer, _)| {
            inner != name && outer.as_deref() == Some(name)
        })
        .map(|(inner, _, _)| inner)
        .collect()
}

/// Los componentes de un record, como `(nombre, descriptor)`, o `None` si la clase no lo es.
fn record_components(
    metaspace: &mut MetaspaceService,
    name: &str,
) -> Option<Vec<(String, String)>> {
    let pairs = {
        let body = attribute_body(metaspace, name, "Record")?;
        if body.len() < 2 {
            return Some(Vec::new());
        }
        let count = u16::from_be_bytes([body[0], body[1]]) as usize;
        let mut at = 2;
        let mut out = Vec::with_capacity(count);
        for _ in 0..count {
            if at + 5 >= body.len() {
                break;
            }
            let name_index = u16::from_be_bytes([body[at], body[at + 1]]);
            let descriptor_index = u16::from_be_bytes([body[at + 2], body[at + 3]]);
            let attributes = u16::from_be_bytes([body[at + 4], body[at + 5]]) as usize;
            at += 6;
            // Cada componente puede traer sus propios atributos (`Signature`, anotaciones); se
            // saltean leyendo su largo, que es un u4 detras del indice del nombre.
            for _ in 0..attributes {
                if at + 5 >= body.len() {
                    break;
                }
                let length = u32::from_be_bytes([
                    body[at + 2],
                    body[at + 3],
                    body[at + 4],
                    body[at + 5],
                ]) as usize;
                at += 6 + length;
            }
            out.push((name_index, descriptor_index));
        }
        out
    };
    let class = metaspace.get(name)?;
    Some(
        pairs
            .iter()
            .filter_map(|&(n, d)| Some((class.utf8(n)?.to_string(), class.utf8(d)?.to_string())))
            .collect(),
    )
}

/// Los tipos que un metodo declara en su `throws`, leidos del atributo `Exceptions` (§4.7.5).
/// Vacio cuando no lo tiene, que es lo mismo que no declarar ninguno.
///
/// El atributo se parsea acá y no en `ClassFile` porque su cuerpo se guarda crudo: son dos
/// bytes de cuenta y dos por indice al pool, y este es el unico lector que hay.
fn declared_exceptions(class: &ClassFile, member: &crate::jvm::parser::MemberInfo) -> Vec<String> {
    for attribute in &member.attributes {
        if class.utf8(attribute.name_index) != Some("Exceptions") {
            continue;
        }
        let body = &attribute.info;
        if body.len() < 2 {
            return Vec::new();
        }
        let count = u16::from_be_bytes([body[0], body[1]]) as usize;
        let mut out = Vec::with_capacity(count);
        for i in 0..count {
            let at = 2 + i * 2;
            if at + 1 >= body.len() {
                break;
            }
            let index = u16::from_be_bytes([body[at], body[at + 1]]);
            if let Some(name) = class.class_name(index) {
                out.push(name.to_string());
            }
        }
        return out;
    }
    Vec::new()
}

/// Un descriptor de metodo partido en `(nombres internos de los parametros, del retorno)`.
/// `(Ljava/lang/String;[IJ)V` → `(["java/lang/String", "[I", "long"], "void")`.
fn split_descriptor(descriptor: &str) -> (Vec<String>, String) {
    let bytes = descriptor.as_bytes();
    let mut i = 1; // saltar el '('
    let mut parameters = Vec::new();
    while i < bytes.len() && bytes[i] != b')' {
        let start = i;
        while i < bytes.len() && bytes[i] == b'[' {
            i += 1;
        }
        if i < bytes.len() && bytes[i] == b'L' {
            while i < bytes.len() && bytes[i] != b';' {
                i += 1;
            }
        }
        i += 1;
        parameters.push(internal_name_of(&descriptor[start..i]));
    }
    let returns = if i + 1 <= descriptor.len() {
        internal_name_of(&descriptor[i + 1..])
    } else {
        "void".to_string()
    };
    (parameters, returns)
}

/// El nombre interno de la clase que un mirror nombra.
pub(super) fn mirror_name(metaspace: &MetaspaceService, mirror: usize) -> String {
    metaspace
        .class_name_at_mirror(mirror)
        .expect("Class: no hay ninguna clase en este mirror")
        .to_string()
}

/// Las nueve palabras que nombran un tipo primitivo. No hay archivo de clase detras de ninguna,
/// asi que el nombre **es** la identidad.
fn is_primitive_name(name: &str) -> bool {
    matches!(
        name,
        "int" | "long" | "double" | "float" | "short" | "byte" | "char" | "boolean" | "void"
    )
}

/// El descriptor de campo de un nombre interno: `int` → `I`, `java/lang/String` →
/// `Ljava/lang/String;`, y un array ya viene en forma de descriptor.
pub(super) fn descriptor_of(name: &str) -> String {
    match name {
        "int" => "I".to_string(),
        "long" => "J".to_string(),
        "double" => "D".to_string(),
        "float" => "F".to_string(),
        "short" => "S".to_string(),
        "byte" => "B".to_string(),
        "char" => "C".to_string(),
        "boolean" => "Z".to_string(),
        "void" => "V".to_string(),
        n if n.starts_with('[') => n.to_string(),
        n => format!("L{n};"),
    }
}

/// La vuelta de [`descriptor_of`]: el nombre interno que un descriptor de campo nombra.
fn internal_name_of(descriptor: &str) -> String {
    match descriptor.as_bytes().first() {
        Some(b'I') => "int".to_string(),
        Some(b'J') => "long".to_string(),
        Some(b'D') => "double".to_string(),
        Some(b'F') => "float".to_string(),
        Some(b'S') => "short".to_string(),
        Some(b'B') => "byte".to_string(),
        Some(b'C') => "char".to_string(),
        Some(b'Z') => "boolean".to_string(),
        Some(b'V') => "void".to_string(),
        Some(b'L') => descriptor[1..descriptor.len() - 1].to_string(),
        _ => descriptor.to_string(), // ya es un array
    }
}

/// El nombre interno del tipo COMPONENTE de una clase array, o `None` si no es un array. Una
/// dimension menos: `[[I` → `[I`, y recien `[I` → `int`.
fn component_name(array_class: &str) -> Option<String> {
    let inner = array_class.strip_prefix('[')?;
    Some(internal_name_of(inner))
}

/// El mirror de un nombre interno cualquiera, creandolo si hace falta. Las tres formas de tipo
/// llegan por caminos distintos: un array por su mirror sintetico, un primitivo por el suyo
/// (idem, pero indexado por la palabra clave) y una clase de verdad cargandola.
fn mirror_for(metaspace: &mut MetaspaceService, heap: &mut HeapService, name: &str) -> usize {
    if name.starts_with('[') {
        return array_operations::array_class_mirror(metaspace, heap, name);
    }
    if is_primitive_name(name) {
        return primitive_mirror(metaspace, heap, name);
    }
    class_operations::load_class(metaspace, heap, name);
    let uuid = metaspace.class_id(name).to_string();
    metaspace.class_object(&uuid).unwrap_or(0)
}

/// El mirror de un primitivo por su palabra clave, creado la primera vez y cacheado despues --
/// que es lo que hace que `int.class == int.class`. Solo cabecera: un primitivo no tiene
/// estaticos, y como todo mirror va a **Old**, donde el GC no lo mueve.
fn primitive_mirror(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    keyword: &str,
) -> usize {
    let uuid = metaspace.class_id(keyword).to_string();
    if let Some(offset) = metaspace.class_object(&uuid) {
        return offset;
    }
    let offset = heap.malloc_old(HEADER_SIZE);
    metaspace.set_class_object(&uuid, offset);
    class_operations::load_class(metaspace, heap, "java/lang/Class");
    let class_uuid = metaspace.class_id("java/lang/Class").to_string();
    let class_mirror = metaspace.class_object(&class_uuid).unwrap_or(0);
    heap.write_u32(offset, class_mirror as u32);
    offset
}

/// Un array de referencias de la clase `array_class`, con los elementos ya adentro. En **Old** y
/// por el write barrier, por lo mismo que `build_object_array`: el llamador tiene las referencias
/// en un `Vec` de Rust que no es raiz de nada.
fn reference_array(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    array_class: &str,
    elements: &[usize],
) -> usize {
    let mirror = array_operations::array_class_mirror(metaspace, heap, array_class);
    let offset =
        heap.malloc_old(array_operations::ARRAY_HEADER_SIZE + elements.len() * SLOT_SIZE);
    heap.write_u32(offset, mirror as u32);
    heap.write_u32(offset + array_operations::LENGTH_OFFSET, elements.len() as u32);
    for (i, &value) in elements.iter().enumerate() {
        if value != 0 {
            heap.store_reference(
                offset,
                offset + array_operations::ARRAY_HEADER_SIZE + i * SLOT_SIZE,
                value,
            );
        }
    }
    offset
}

fn int(value: &Value) -> i32 {
    match value {
        Value::Int(n) => *n,
        other => panic!("native: expected an int argument, found {other:?}"),
    }
}

/// The `long` payload of an argument (a verifier-guaranteed `Long`).
fn long(value: &Value) -> i64 {
    match value {
        Value::Long(n) => *n,
        other => panic!("native: expected a long argument, found {other:?}"),
    }
}

/// The heap offset of a reference argument (a verifier-guaranteed `Reference`).
fn double(value: &Value) -> f64 {
    match value {
        Value::Double(d) => *d,
        other => panic!("se esperaba un double, llego {other:?}"),
    }
}

fn float(value: &Value) -> f32 {
    match value {
        Value::Float(f) => *f,
        other => panic!("se esperaba un float, llego {other:?}"),
    }
}

fn reference(value: &Value) -> usize {
    match value {
        Value::Reference(offset) => *offset,
        other => panic!("native: expected a reference argument, found {other:?}"),
    }
}

// --- APT fase 3: ayudantes de la reificación de `SymElement` -------------------------------------

/// El `SymbolId` que reifica un `SymElement` receptor: lee su campo `int sym`. Es el índice en la
/// tabla viva de `javac` que los native de arriba usan para releer el símbolo.
fn sym_of(receiver: &Value, metaspace: &mut MetaspaceService, heap: &HeapService) -> usize {
    let this = reference(receiver);
    let sym_offset = field_offset(metaspace, "jdk/internal/apt/SymElement", "sym");
    heap.read_u32(this + sym_offset) as usize
}

/// El [`AptContext`] atado (con `JVM::set_apt`). Panica si falta: un native de `SymElement` sólo se
/// alcanza durante una corrida de procesador, donde el contexto siempre está presente.
fn apt_ref(apt: &Option<AptContext>) -> &AptContext {
    apt.as_ref().expect("SymElement native sin un AptContext (usar JVM::set_apt)")
}

/// Construye un `jdk/internal/apt/SymName` (un `javax.lang.model.element.Name`) que envuelve `text`:
/// aloca el objeto y le escribe el `String` internado en su campo `value`. Alocado en **Old** (como
/// un `SymElement`): un `Name` es un handle que el procesador puede sostener a través de otras
/// alocaciones/GC. El `String` va por la barrera de escritura (`store_reference`), por si el Name
/// (Old) apunta a una `String` joven. Mismo patrón que `Exec::materialize_method_type`.
fn make_name(metaspace: &mut MetaspaceService, heap: &mut HeapService, text: &str) -> usize {
    const SYM_NAME: &str = "jdk/internal/apt/SymName";
    class_operations::load_class(metaspace, heap, SYM_NAME);
    let object = allocate_old(metaspace, heap, SYM_NAME);
    let value = strings::intern(metaspace, heap, text);
    let value_offset = field_offset(metaspace, SYM_NAME, "value");
    heap.store_reference(object, object + value_offset, value);
    object
}
