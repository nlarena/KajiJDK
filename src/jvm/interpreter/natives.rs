//! The **native bridge** — the escape hatch from bytecode to real native code.
//!
//! A `native` method has no `Code`; the interpreter, instead of pushing a frame,
//! calls in here. This is where the JVM reaches the outside world (I/O, the OS) —
//! the things Java can't do itself. In a real JVM these are C/C++ via JNI; ours are
//! Rust functions matched by `(class, name, descriptor)`.
//!
//! Right now there's one: `PrintStream.println(int)`, so `System.out.println(n)`
//! prints for real — the wall the whole interpreter has been building toward.

use std::fmt::Write;

use super::bytecode_interpreter::class_operations;
use super::bytecode_interpreter::objects_operations::{field_offset, HEADER_SIZE};
use super::frame::Value;
use super::heap::HeapService;
use super::metaspace::MetaspaceService;
use super::strings;

/// Runs the native method `class.name descriptor` with `args` (slot 0 is the
/// receiver for an instance method), returning its result (`None` for `void`).
/// `heap` lets a native read object memory (e.g. an object's header); anything the
/// method "prints" is appended to `out` — the program's stdout, which the caller
/// surfaces (the visualizer shows it; a headless run would flush it).
pub fn dispatch(
    class: &str,
    name: &str,
    descriptor: &str,
    args: &[Value],
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    out: &mut String,
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
        // Bulk copy between arrays — the memcpy the VM does for you. Assumes 4-byte
        // elements (int/reference arrays); byte/char arrays would need their width.
        ("java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V") => {
            const ARRAY_HEADER: usize = 12; // object header (8) + length word (4)
            const ELEM: usize = 4;
            let (src, src_pos) = (reference(&args[0]), int(&args[1]) as usize);
            let (dst, dst_pos) = (reference(&args[2]), int(&args[3]) as usize);
            let length = int(&args[4]) as usize;
            for i in 0..length {
                let value = heap.read_u32(src + ARRAY_HEADER + (src_pos + i) * ELEM);
                heap.write_u32(dst + ARRAY_HEADER + (dst_pos + i) * ELEM, value);
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

        // --- String -------------------------------------------------------------
        ("java/lang/String", "length", "()I") => {
            // The receiver is a heap String; its length word holds the UTF-8 byte count.
            Some(Value::Int(strings::length(heap, reference(&args[0])) as i32))
        }
        // charAt(int): the i-th byte (ASCII; our String is UTF-8, fine for ASCII).
        ("java/lang/String", "charAt", "(I)C") => {
            Some(Value::Int(strings::char_at(heap, reference(&args[0]), int(&args[1]) as usize) as i32))
        }
        // equals(Object): true if the other is a String with the same text.
        // (Simplified: assumes the argument is a String reference.)
        ("java/lang/String", "equals", "(Ljava/lang/Object;)Z") => {
            let other = reference(&args[1]);
            let equal = other != 0 && strings::read(heap, reference(&args[0])) == strings::read(heap, other);
            Some(Value::Int(equal as i32))
        }
        // hashCode(): Java's `s[0]*31^(n-1) + … + s[n-1]` over the bytes (ASCII).
        ("java/lang/String", "hashCode", "()I") => {
            let text = strings::read(heap, reference(&args[0]));
            let hash = text.bytes().fold(0i32, |h, b| h.wrapping_mul(31).wrapping_add(b as i32));
            Some(Value::Int(hash))
        }
        // startsWith(prefix): whether the receiver begins with the argument String.
        ("java/lang/String", "startsWith", "(Ljava/lang/String;)Z") => {
            let text = strings::read(heap, reference(&args[0]));
            let prefix = strings::read(heap, reference(&args[1]));
            Some(Value::Int(text.starts_with(&prefix) as i32))
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

        _ => panic!("no native implementation for {class}.{name}{descriptor}"),
    }
}

/// The `int` payload of an argument (a verifier-guaranteed `Int`).
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
fn reference(value: &Value) -> usize {
    match value {
        Value::Reference(offset) => *offset,
        other => panic!("native: expected a reference argument, found {other:?}"),
    }
}
