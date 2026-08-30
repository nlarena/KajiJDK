//! The metaspace — our take on the JVM's **Method Area** (JVMS §2.5.4): the
//! thread-shared home for everything that belongs to a *class* rather than an
//! *instance*. Per loaded class it holds the bytecode of its methods, its runtime
//! constant pool, its static fields and its method/field metadata.
//!
//! Classes are filled by **class loading** (`add` / `get` / `get_or_load`). On
//! top of that, methods are resolved to a [`MethodId`] handle whose body — the
//! bytecode — is owned here **once**; frames hold the handle, not the bytes.
//!
//! Calls resolve straight from the bytecode's constant-pool index (the `00 07` of
//! an `invokestatic #7`): [`MetaspaceService::resolve_call`] reads the already-parsed
//! `Methodref` and caches the resulting handle **under that index** — the JVM's
//! "resolved constant pool", where a symbolic reference is resolved once and the
//! code then reuses the resolved handle.
//!
//! Named after HotSpot's MetaspaceService (the off-heap Method Area since Java 8).

use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicU32, AtomicU64, Ordering};

use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::code::ExceptionTableEntry;
use crate::jvm::uuid::UuidGenerator;

/// A handle to a method's body in the metaspace — an index into its method table.
/// Frames carry one of these instead of owning the bytecode.
pub type MethodId = usize;

/// Which class loader defined a class. The JVM has a delegation hierarchy
/// (bootstrap → application); a class's runtime identity is `(name, defining loader)`.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum ClassLoader {
    /// The bootstrap loader — core `java.lang.*` classes.
    Bootstrap,
    /// The application loader — the user classpath.
    Application,
}

/// A class's initialization state (JVMS §5.5). `<clinit>` runs **lazily** (on first
/// active use) and **exactly once**; the `InProgress` state makes re-entrant uses
/// (a class touching itself during its own `<clinit>`) not retrigger it.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum InitState {
    /// Not yet initialized — the next active use triggers `<clinit>`.
    NotStarted,
    /// `<clinit>` is running right now.
    InProgress,
    /// `<clinit>` has completed.
    Done,
    /// `<clinit>` completed abruptly (threw). The class is unusable: every subsequent active use
    /// throws `NoClassDefFoundError` (JVMS §5.5, the erroneous state).
    Erroneous,
}

/// A method the VM **intercepts** instead of (or before) running its body — `System.gc()`,
/// `Thread.start()`, `Object.wait()` and friends. Every one of them used to be recognised by a
/// chain of `class == "…" && name == "…" && descriptor == "…"` string compares run *on every
/// call*; a user call failed all ~7 of them before reaching the normal path.
///
/// The decision is a pure function of the method's own `(class, name, descriptor)`, so it is
/// **a property of the callee**, not of the call site: it is computed once in
/// [`MetaspaceService::resolve_method`] and read back as a `Copy` tag. That also means a
/// *subclass override* of, say, `Thread.start()` is naturally [`Intrinsic::None`] — its
/// `MethodBody` is a different one — which is exactly the old `class_of(callee) == …` test.
///
/// Storing it on the callee rather than per call site is what keeps `invokestatic` and
/// `invokevirtual` from each needing their own copy of the table: the same `Thread.join`
/// body is recognised identically wherever it is reached from (including through a vtable,
/// where the call site can't know the answer).
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Intrinsic {
    /// Not intercepted — run the method normally.
    None,
    /// `System.gc()` — request a collection at the next safepoint.
    SystemGc,
    /// `System.exit(status)` — end the VM.
    SystemExit,
    /// `String.valueOf(Object)` — calls back into the object's own `toString()`.
    StringValueOfObject,
    /// `String.publish(String)` — how a String constructor hands back what it built.
    ///
    /// Every other constructor returns its result by writing fields of `this`. A String cannot:
    /// its characters sit inline and are sized when the object is allocated, and `new` sizes an
    /// instance from its declared fields, of which `String` has none. So the object the
    /// constructor is handed has room for nothing and cannot be grown.
    ///
    /// The constructor builds a separate String and publishes it here; the `return` that ends
    /// the constructor then rewrites the caller's references from the object it was handed to
    /// the one it built. See `Exec::string_publish` and `Frame::published`.
    StringPublish,
    /// `LockSupport.park()` / `park(Object)` — block the current thread.
    LockSupportPark,
    /// `LockSupport.unpark(Thread)` — hand a thread a permit.
    LockSupportUnpark,
    /// `Thread.sleep(ms)`.
    ThreadSleep,
    /// `Thread.yield()`.
    ThreadYield,
    /// `Thread.holdsLock(o)`.
    ThreadHoldsLock,
    /// `Thread.currentThread()`.
    ThreadCurrentThread,
    /// `Thread.nextThreadNum()`.
    ThreadNextThreadNum,
    /// `Thread.start()`.
    ThreadStart,
    /// `Thread.join()`.
    ThreadJoin,
    /// `Thread.join(long)` — join with a millisecond deadline.
    ThreadJoinTimed,
    /// `Thread.interrupt()`.
    ThreadInterrupt,
    /// `Thread.getState()`.
    ThreadGetState,
    /// `Object.wait()`.
    ObjectWait,
    /// `Object.wait(long)`.
    ObjectWaitTimed,
    /// `Object.notify()`.
    ObjectNotify,
    /// `Object.notifyAll()`.
    ObjectNotifyAll,
    /// `Object.clone()` — reached by `invokevirtual` (no override) and by an override's
    /// `super.clone()` (`invokespecial`).
    ObjectClone,
    /// `jdk/internal/apt/SymElement.getKind()` (APT fase 3, capa 4): devuelve una constante del
    /// enum `ElementKind`. Va por intrínseco (no native del bridge) porque debe correr el
    /// `<clinit>` de `ElementKind` para que sus constantes existan — sólo el intérprete puede — y
    /// luego leer el campo estático, el mismo patrón que `Thread.getState()`.
    SymElementGetKind,
    /// `jdk/internal/apt/SymElement.getEnclosedElements()` (APT fase 3, capa 5): devuelve una
    /// `List`. Va por intrínseco porque construye un `ArrayList` y **re-entra** al intérprete por
    /// cada miembro (`ArrayList.add(element_for(child))`) — un native del bridge no tiene la vista
    /// `Exec` para invocar bytecode.
    SymElementGetEnclosedElements,
    /// `java.lang.reflect.Method.invoke(Object, Object[])` — la llamada reflexiva.
    ///
    /// Va acá y no en el puente de nativas porque tiene que **correr bytecode**: el puente sólo
    /// puede computar y devolver, y una llamada reflexiva es, entera, empujar un frame.
    MethodInvoke,
    /// `java.lang.reflect.Constructor.newInstance(Object[])` — alocar y correr `<init>`.
    ///
    /// Acá por lo mismo que `MethodInvoke`, y con una razón de más: tiene que **alocar**, y
    /// el objeto a medio construir queda vivo mientras corre bytecode arbitrario.
    ConstructorNewInstance,
}

/// Which [`Intrinsic`] a `(class, name, descriptor)` names — the ~7 string-compare chain the
/// invoke opcodes used to run per call, hoisted to method-resolution time. The tests mirror the
/// old ones exactly, including which of them ignored the descriptor.
fn classify_intrinsic(class: &str, name: &str, descriptor: &str) -> Intrinsic {
    match class {
        "java/lang/System" => match name {
            "gc" => Intrinsic::SystemGc,
            "exit" => Intrinsic::SystemExit,
            _ => Intrinsic::None,
        },
        "java/lang/String"
            if (name == "rawValueOfObject" || name == "valueOf")
                && descriptor == "(Ljava/lang/Object;)Ljava/lang/String;" =>
        {
            Intrinsic::StringValueOfObject
        }
        "java/lang/String" if name == "publish" && descriptor == "(Ljava/lang/String;)V" => {
            Intrinsic::StringPublish
        }
        "java/util/concurrent/locks/LockSupport" => match name {
            "park" => Intrinsic::LockSupportPark,
            "unpark" => Intrinsic::LockSupportUnpark,
            _ => Intrinsic::None,
        },
        "java/lang/Thread" => match (name, descriptor) {
            // Only the plain `sleep(long)` / `yield()` are scheduler primitives; the
            // `sleep(long,int)` and `sleep(Duration)` overloads are ordinary Java that convert
            // to millis and call `sleep(long)`, so they must NOT be intercepted here.
            ("sleep", "(J)V") => Intrinsic::ThreadSleep,
            ("yield", "()V") => Intrinsic::ThreadYield,
            ("holdsLock", _) => Intrinsic::ThreadHoldsLock,
            ("currentThread", _) => Intrinsic::ThreadCurrentThread,
            ("nextThreadNum", _) => Intrinsic::ThreadNextThreadNum,
            ("start", "()V") => Intrinsic::ThreadStart,
            ("join", "()V") => Intrinsic::ThreadJoin,
            ("join", "(J)V") => Intrinsic::ThreadJoinTimed,
            ("interrupt", "()V") => Intrinsic::ThreadInterrupt,
            ("getState", "()Ljava/lang/Thread$State;") => Intrinsic::ThreadGetState,
            _ => Intrinsic::None,
        },
        "java/lang/Object" => match (name, descriptor) {
            ("wait", "()V") => Intrinsic::ObjectWait,
            // `wait0` es el nombre que le da KajiLibrary -- y el JDK -- a la costura privada;
            // `wait` es el que declara la copia compilada de `boot/`. Los dos, mientras convivan.
            ("wait0" | "wait", "(J)V") => Intrinsic::ObjectWaitTimed,
            ("notify", "()V") => Intrinsic::ObjectNotify,
            ("notifyAll", "()V") => Intrinsic::ObjectNotifyAll,
            ("clone", "()Ljava/lang/Object;") => Intrinsic::ObjectClone,
            _ => Intrinsic::None,
        },
        // APT fase 3 (capas 4-5): los dos accesores de `SymElement` que no pueden ser native del
        // bridge — `getKind` corre un `<clinit>` (enum) y `getEnclosedElements` re-entra al
        // intérprete. Los demás (`getSimpleName`/`getQualifiedName`/`getEnclosingElement`) sí son
        // native, así que caen a `Intrinsic::None` y los toma el bridge.
        "jdk/internal/apt/SymElement" => match (name, descriptor) {
            ("getKind", "()Ljavax/lang/model/element/ElementKind;") => Intrinsic::SymElementGetKind,
            ("getEnclosedElements", "()Ljava/util/List;") => Intrinsic::SymElementGetEnclosedElements,
            _ => Intrinsic::None,
        },
        "java/lang/reflect/Method"
            if name == "invoke"
                && descriptor == "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;" =>
        {
            Intrinsic::MethodInvoke
        }
        "java/lang/reflect/Constructor"
            if name == "newInstance" && descriptor == "([Ljava/lang/Object;)Ljava/lang/Object;" =>
        {
            Intrinsic::ConstructorNewInstance
        }
        _ => Intrinsic::None,
    }
}

/// An interned `(name, descriptor)` pair — a **call-site signature**, handed out by
/// [`MetaspaceService::intern_signature`]. `invokeinterface` has no stable interface slot to
/// cache (each implementor numbers its own table), so what its site caches instead is this
/// id: the signature it must look up in whatever the receiver's runtime class turns out to be.
/// One `u32` replaces the two `String` allocations the lookup used to need per call.
pub type SignatureId = u32;

/// One slot of a class's **virtual method table**: the method to run for a given
/// `(name, descriptor)` signature. A subclass inherits its super's slots in the
/// *same order* and overrides them in place — so a signature has the same slot
/// index across the whole hierarchy, which is what makes dynamic dispatch O(1).
#[derive(Clone)]
struct VtableEntry {
    name: String,
    descriptor: String,
    method: MethodId,
}

/// One resolved method body, owned a single time by the metaspace (the
/// "load-once" home every frame of that method points at).
struct MethodBody {
    /// Owning class's binary name — used to resolve this method's own constant
    /// pool (e.g. the `#7` of an `invokestatic` it runs).
    class: String,
    /// The method's own name (for tooling/labels).
    name: String,
    /// The method's descriptor — kept so an already-resolved method can still be told
    /// from its overloads, which the name alone can't do.
    descriptor: String,
    max_locals: usize,
    /// Declared argument count, parsed from the descriptor once at resolution.
    arg_count: usize,
    /// The bytecode, owned here exactly once. Empty for a native method.
    code: Vec<u8>,
    /// The method's exception table (the `try`/`catch` ranges) — consulted by
    /// `athrow` to find a handler. `catch_type` indices are into this method's
    /// class's constant pool.
    exceptions: Vec<ExceptionTableEntry>,
    /// `true` for a `native` method (no bytecode): the interpreter dispatches it to
    /// the native bridge instead of pushing a frame.
    native_: bool,
    /// `true` for an `abstract` method — also bodiless, but for the opposite reason: there is
    /// nothing to run, and selecting one at dispatch time is an `AbstractMethodError` (§6.5).
    /// It is resolved anyway because it must **hold a vtable slot**: the call site reads the slot
    /// off the *static* type, so if the abstract declaration has no slot, every override of it is
    /// unreachable through the type that declares it (COMPILER_FINDINGS #230).
    abstract_: bool,
    /// `true` for a `synchronized` method (`ACC_SYNCHRONIZED`). There is no opcode for
    /// it — the VM takes the receiver's (or `Class`'s) monitor when it pushes the frame
    /// and releases it when the frame is popped. See `JVM::push_frame_locked`.
    synchronized_: bool,
    /// `true` for a `static` method — recorded because the JIT needs to know whether slot 0 is
    /// `this` (a reference) or the first argument before it can type the frame it is compiling.
    static_: bool,
    /// The **field-site cache** (F0 quickening): one cell per code byte, so the
    /// `getfield`/`putfield` at `pc` reads its already-resolved field with a single
    /// indexed load — no `String`, no hash, no layout rebuild. The payload is opaque
    /// here (`objects_operations` packs offset + descriptor kind + volatility into the
    /// `u64`); `0` means "not resolved yet". Empty for a method whose code contains no
    /// `0xb4`/`0xb5` byte at all, which makes every lookup miss harmlessly.
    ///
    /// `AtomicU64` (not plain `u64`) because the H3 W3 lock-free read path holds only
    /// `&MetaspaceService` and must be able to fill a cell too — see
    /// `objects_operations::resolve_field_site_read` for why that race is benign.
    field_sites: Vec<AtomicU64>,
    /// The **call-site cache** (F0 quickening, part 2): the twin of [`Self::field_sites`] for the
    /// four `invoke*` opcodes — one cell per code byte, so the call at `pc` reads its resolved
    /// target with a single indexed load. The payload is opaque here (`call_site::CallSite` packs
    /// the target handle, the site kind and the argument count into the `u64`); `0` means "not
    /// resolved yet". Empty for a method whose code contains no `0xb6`–`0xb9` byte.
    ///
    /// Separate from `field_sites` on purpose: a `pc` is one opcode, so the two tables can never
    /// contend for the same cell, and keeping them apart means the field cache's payload layout
    /// and this one's stay independent.
    call_sites: Vec<AtomicU64>,
    /// **The last receiver class seen at each dispatched call site** (milestone F2, the
    /// monomorphic inline cache): the heap offset of the `Class<…>` mirror the `invokevirtual` or
    /// `invokeinterface` at `pc` last dispatched on, or `0` for "never executed". One cell per code
    /// byte, alongside [`Self::call_sites`] and allocated on the same condition.
    ///
    /// **Why a *last* rather than a count, a majority or a history.** The JIT compiles a method
    /// only once its counter has run out, which is thirty-two invocations or a loop's worth of
    /// back-edges — so by the time this word is read it has been written thirty-two times, and at a
    /// site that really is monomorphic every one of those writes was the same value. Nothing
    /// cleverer buys anything a guard does not already provide: a wrong guess is not a wrong answer,
    /// it is a deopt, and the interpreter dispatches the call by its full path. What a wrong guess
    /// costs is speed at that site, which is the thing this whole tier is allowed to be wrong about.
    ///
    /// It is *not* part of the packed [`Self::call_sites`] word, which has no room for another 32
    /// bits, and it is deliberately not a `Vec<AtomicU64>`: a mirror offset is a `u32` by the same
    /// boundary argument every heap offset here crosses on.
    ///
    /// `AtomicU32` and `Relaxed`, for exactly the reason the two site caches are: the word is
    /// self-contained, publishes no other memory, and two threads writing different receivers race
    /// to a value that is a real receiver either way.
    receiver_classes: Vec<AtomicU32>,
    /// The slot width of the callee's own arguments, **receiver-first**: `[1, param widths…]`.
    /// Parsed once here instead of by a fresh `param_slot_widths` `Vec` on every call — laying a
    /// call's operands into the callee's locals is the last thing every invoke does, and it used
    /// to re-parse the descriptor and allocate for it each time. An instance call reads the whole
    /// slice ([`MetaspaceService::receiver_slot_widths`]); a static call reads `[1..]`
    /// ([`MetaspaceService::param_slot_widths_of`]).
    slot_widths: Vec<usize>,
    /// Whether the VM intercepts this method instead of running it — see [`Intrinsic`].
    intrinsic: Intrinsic,
}

/// The Method Area: loaded classes, resolved method bodies, the per-index call
/// resolution cache, and where to find classes that aren't loaded yet.
pub struct MetaspaceService {
    /// The **bootstrap** loader's directories — searched *first* (delegation). Home
    /// of the core classes (`java.lang.*`).
    bootstrap: Vec<PathBuf>,
    /// The module graph of the image, when the VM booted from one. `None` means the VM
    /// booted from plain directories — there are no modules, so the JPMS access rule has
    /// nothing to say and every access is allowed.
    configuration: Option<crate::jvm::modules::Configuration>,
    /// A **runtime image** the bootstrap loader reads from, when the VM was booted with
    /// one instead of plain directories — the `lib/modules` of a jlink image. Consulted
    /// after the bootstrap directories, so a directory can still shadow it.
    boot_image: Option<crate::jvm::jimage::BootImage>,
    /// The **application** loader's directories — the user classpath, searched only
    /// if the bootstrap loader didn't find the class.
    application: Vec<PathBuf>,
    /// Which loader defined each loaded class (by binary name). A class's identity
    /// is really `(name, loader)`; with a single app loader there are no cross-loader
    /// name clashes yet, so we track the loader as metadata rather than re-key by it.
    loaders: HashMap<String, ClassLoader>,
    /// Loaded classes, keyed by binary name (e.g. "Add", "java/lang/Object").
    classes: HashMap<String, ClassFile>,
    /// Resolved method bodies, indexed by `MethodId`.
    methods: Vec<MethodBody>,
    /// Method resolution cache, keyed by `(class, name, descriptor)`.
    resolved: HashMap<(String, String, String), MethodId>,
    /// Call resolution cache, keyed by `(class, constant-pool index)` — the
    /// "resolved constant pool": once `#7` of a class is resolved, that code
    /// maps straight to its handle.
    resolved_calls: HashMap<(String, u16), MethodId>,
    /// Memory index of each class's `Class<…>` object: **Class ID (UUID) → heap
    /// offset**. Keyed by the stable identity, not the name — the mirror's location
    /// belongs to the class's id. HotSpot keeps a class's statics in its mirror on
    /// the heap; this map locates that mirror so `getstatic`/`putstatic` (and the
    /// GC) can reach the statics. Filled during Preparation when the class loads.
    /// **The string pool** (JLS §3.10.5): one `java.lang.String` instance per distinct literal,
    /// keyed by its UTF-16 code units.
    ///
    /// It lives here rather than in the heap because the heap is a byte arena with no notion of
    /// what a String is, and because the collector already reaches the metaspace to walk the class
    /// mirrors — so the pool gets to be a GC root and a pinned block by the same route the mirrors
    /// use, instead of inventing a third one.
    ///
    /// **Only literals go in.** A String the program *computes* — a runtime concatenation, a
    /// `new String(…)`, `String.valueOf` — must stay a distinct object, which is the other half of
    /// §3.10.5 and the reason `strings::allocate` exists beside `strings::intern`.
    interned: HashMap<Vec<u16>, usize>,
    class_objects: HashMap<String, usize>,
    /// The reverse of [`Self::class_objects`], composed with [`Self::names_by_id`]: **mirror
    /// heap offset → binary name**. Every object's header carries its class's *mirror offset*
    /// as the `class_id` word, so recovering the class from an object (`invokevirtual`'s
    /// dispatch, the GC's slot walk) is a lookup by offset. Without this index that is a
    /// linear scan of `class_objects` on *every* virtual call; with it, [`Self::class_name_at_mirror`]
    /// is a single hash lookup — and the name is stored resolved, so the hot path never
    /// hashes a UUID string. Kept in sync inside [`Self::set_class_object`], the single point
    /// that mutates `class_objects`.
    ///
    /// Sound because **mirrors never move**: they are `malloc_old`-allocated (see
    /// `class_operations::load_class`, `array_operations::array_class_mirror` and the
    /// primitive mirror in `natives.rs`), the major collector pins them out of compaction
    /// (`gc::compact`'s `pinned` set), and they are permanent GC roots, so a sweep never
    /// frees one. An entry can therefore never go stale behind our back.
    mirror_names: HashMap<usize, String>,
    /// Each class's identity **UUID** (its "Class ID"): binary name → UUID string.
    /// Minted once on first sight and cached here — the dedup point, so a class
    /// always resolves to the same id no matter where it's referenced from.
    class_ids: HashMap<String, String>,
    /// The reverse index of [`Self::class_ids`]: UUID → binary name. Lets us go
    /// from a Class ID found in the wild (an object header, say) back to the class,
    /// without scanning. Kept in sync inside [`Self::class_id`].
    names_by_id: HashMap<String, String>,
    /// The source of those UUIDs, seeded once for the whole metaspace (so ids never
    /// collide from reseeding — see [`UuidGenerator`]).
    uuid_gen: UuidGenerator,
    /// Each class's virtual method table, keyed by binary name. Built lazily from
    /// the superclass's table plus the class's own (overriding) methods.
    vtables: HashMap<String, Vec<VtableEntry>>,
    /// Each class's initialization state, keyed by binary name. Absent = `NotStarted`.
    init_states: HashMap<String, InitState>,
    /// The interned call-site signatures — see [`SignatureId`]. Indexed by the id.
    signatures: Vec<(String, String)>,
    /// The reverse of [`Self::signatures`], so interning the same pair twice yields one id.
    signature_ids: HashMap<(String, String), SignatureId>,
}

impl MetaspaceService {
    /// Boots the bootstrap loader from a **runtime image** (`lib/modules`) instead of only
    /// directories. Returns whether the image could be opened.
    pub fn boot_from_image(&mut self, path: &str) -> bool {
        self.boot_image = crate::jvm::jimage::BootImage::open(path);
        // Los módulos de la imagen ya están resueltos por construcción: lo que hace falta
        // es la relación de legibilidad sobre ellos, que es lo que la regla de acceso mira.
        self.configuration = self
            .boot_image
            .as_ref()
            .map(|i| crate::jvm::modules::Configuration::of(i.module_descriptors()));
        self.boot_image.is_some()
    }

    /// The module that defines a class, or `None` when the VM booted from directories.
    pub fn module_of(&self, class: &str) -> Option<&str> {
        self.boot_image.as_ref()?.module_of(class)
    }

    /// Whether code in `from` may use the type `to`, per JPMS (JLS §7.7, JVMS §5.4.4).
    ///
    /// Two conditions, and **both** are needed — this is the pair people conflate:
    /// `from`'s module must **read** `to`'s, *and* `to`'s module must **export** the
    /// package to it. Readability alone is not access: a module can read another and
    /// still be barred from its internal packages.
    ///
    /// Everything is allowed when there is no module graph (a VM booted from
    /// directories), when either class has no owning module, or within one module.
    pub fn can_access(&self, from: &str, to: &str) -> bool {
        let Some(configuration) = &self.configuration else { return true };
        let (Some(from_module), Some(to_module)) = (self.module_of(from), self.module_of(to))
        else {
            return true;
        };
        if from_module == to_module {
            return true;
        }
        let package = to.rsplit_once('/').map_or(String::new(), |(p, _)| p.replace('/', "."));
        configuration.reads(from_module, to_module)
            && configuration.exports_to(to_module, &package, from_module)
    }

    /// How many classes the boot image offers, or `0` when the VM booted from directories.
    pub fn boot_image_classes(&self) -> usize {
        self.boot_image.as_ref().map_or(0, |i| i.len())
    }

    /// Los nombres binarios de todas las clases cargadas, en orden determinístico (para el snapshot de
    /// depuración: `VirtualMachine.AllClasses`/`ClassesBySignature`; ordenar mantiene estable el
    /// `referenceTypeID` de una clase dentro de una sesión).
    pub fn loaded_class_names(&self) -> Vec<String> {
        let mut names: Vec<String> = self.classes.keys().cloned().collect();
        names.sort();
        names
    }

    /// A metaspace whose loaders search `bootstrap` first, then `application`
    /// (the JVM's parent-first delegation).
    pub fn new(bootstrap: Vec<PathBuf>, application: Vec<PathBuf>) -> Self {
        MetaspaceService {
            bootstrap,
            boot_image: None,
            configuration: None,
            application,
            loaders: HashMap::new(),
            classes: HashMap::new(),
            methods: Vec::new(),
            resolved: HashMap::new(),
            resolved_calls: HashMap::new(),
            interned: HashMap::new(),
            class_objects: HashMap::new(),
            mirror_names: HashMap::new(),
            class_ids: HashMap::new(),
            names_by_id: HashMap::new(),
            uuid_gen: UuidGenerator::new(),
            vtables: HashMap::new(),
            init_states: HashMap::new(),
            signatures: Vec::new(),
            signature_ids: HashMap::new(),
        }
    }

    /// A class's initialization state (`NotStarted` if never touched).
    pub fn init_state(&self, class: &str) -> InitState {
        self.init_states.get(class).copied().unwrap_or(InitState::NotStarted)
    }

    /// Records a class's initialization state — the interpreter drives the
    /// `NotStarted → InProgress → Done` transitions as it runs `<clinit>`.
    pub fn set_init_state(&mut self, class: &str, state: InitState) {
        self.init_states.insert(class.to_string(), state);
    }

    /// The binary name of `class`'s direct superclass (loading `class` if needed),
    /// or `None` for `Object` / an unloadable super. Used to initialize the
    /// superclass before the subclass.
    pub fn superclass_name(&mut self, class: &str) -> Option<String> {
        self.get_or_load(class)
            .and_then(|cf| cf.class_name(cf.super_class).map(str::to_string))
    }

    /// The superinterfaces of `class` — direct **and** indirect — that declare at least one
    /// **default** method, in breadth-first order. These are exactly the interfaces JVMS §5.5
    /// makes part of a class's initialization: implementing an interface does *not* initialize
    /// it, unless it contributes a default method the instance can actually run.
    ///
    /// A default is a non-`static`, non-`private` interface method with a body — the same test
    /// [`Self::build_vtable`] uses to decide what lands in the table, since [`Self::resolve_method`]
    /// only succeeds for a *declared* method that has `Code` (an abstract one yields `None`).
    ///
    /// **Empty when `class` is itself an interface**: initializing an interface never initializes
    /// its superinterfaces, not even ones declaring defaults (JVMS §5.5). The superclass's own
    /// interfaces are absent too — its initialization brings them in.
    pub fn default_method_superinterfaces(&mut self, class: &str) -> Vec<String> {
        let mut queue: Vec<String> = match self.get_or_load(class) {
            Some(cf) if !cf.is_interface() => {
                cf.interfaces.iter().filter_map(|&i| cf.class_name(i).map(str::to_string)).collect()
            }
            _ => return Vec::new(),
        };
        let mut seen: std::collections::HashSet<String> = queue.iter().cloned().collect();
        let (mut at, mut with_defaults) = (0, Vec::new());
        while at < queue.len() {
            let iface = queue[at].clone();
            at += 1;
            let (declared, supers) = match self.get_or_load(&iface) {
                Some(cf) => (
                    cf.methods
                        .iter()
                        .filter(|m| !m.is_static() && !m.is_private())
                        .filter_map(|m| {
                            let name = cf.utf8(m.name_index)?;
                            let descriptor = cf.utf8(m.descriptor_index)?;
                            (!name.starts_with('<'))
                                .then(|| (name.to_string(), descriptor.to_string()))
                        })
                        .collect::<Vec<_>>(),
                    cf.interfaces
                        .iter()
                        .filter_map(|&i| cf.class_name(i).map(str::to_string))
                        .collect::<Vec<_>>(),
                ),
                None => continue,
            };
            // "Declares a default method" means declaring one **with a body**. This used to read
            // `resolve_method(...).is_some()`, which worked only because an abstract method failed
            // to resolve at all; now that it resolves (so it can hold a vtable slot, #230) the
            // question has to be asked directly, which is what it always meant.
            let declares_default = declared.iter().any(|(n, d)| {
                match self.resolve_method(&iface, n, d) {
                    Some(m) => !self.is_abstract(m),
                    None => false,
                }
            });
            if declares_default {
                with_defaults.push(iface);
            }
            for s in supers {
                if seen.insert(s.clone()) {
                    queue.push(s);
                }
            }
        }
        with_defaults
    }

    /// The slot index of `(name, descriptor)` in `class`'s virtual table, or `None`
    /// if it has no such virtual method. The slot is computed from the **static**
    /// type at a call site; it indexes the *receiver's* table at dispatch time.
    pub fn vtable_slot(&mut self, class: &str, name: &str, descriptor: &str) -> Option<usize> {
        self.vtable(class)
            .iter()
            .position(|e| e.name == name && e.descriptor == descriptor)
    }

    /// The method handle in `class`'s virtual table at `slot` — the heart of dynamic
    /// dispatch: pass the receiver's runtime class and the slot from the static type.
    pub fn vtable_method(&mut self, class: &str, slot: usize) -> Option<MethodId> {
        self.vtable(class).get(slot).map(|e| e.method)
    }

    /// Whether `(name, descriptor)` declared in `class` is `private`. `javac` (since
    /// nestmates, Java 11) emits `invokevirtual` for a same-class private instance call,
    /// but a private method has **no vtable slot** and isn't overridable — per JVMS §6.5
    /// selection, a `private` resolved method *is* the selected method, so `invokevirtual`
    /// must invoke it directly on the declaring class rather than through the receiver's
    /// table. `false` if the class or member can't be found (fall back to virtual dispatch).
    pub fn method_is_private(&mut self, class: &str, name: &str, descriptor: &str) -> bool {
        self.get_or_load(class)
            .and_then(|cf| {
                cf.methods
                    .iter()
                    .find(|m| {
                        cf.utf8(m.name_index) == Some(name)
                            && cf.utf8(m.descriptor_index) == Some(descriptor)
                    })
                    .map(|m| m.is_private())
            })
            .unwrap_or(false)
    }

    /// `class`'s virtual method table, building (and caching) it on first use.
    fn vtable(&mut self, class: &str) -> &[VtableEntry] {
        if !self.vtables.contains_key(class) {
            let table = self.build_vtable(class);
            self.vtables.insert(class.to_string(), table);
        }
        &self.vtables[class]
    }

    /// Builds `class`'s vtable: start from the superclass's table (so inherited
    /// methods keep their slots), then fold in the class's own virtual methods —
    /// overriding a slot when the signature already exists, appending otherwise.
    fn build_vtable(&mut self, class: &str) -> Vec<VtableEntry> {
        // An **array class** has no class file to read methods from — it is synthetic — but it is
        // not method-less: JLS §10.7 says its members are exactly `Object`'s (plus the `length`
        // field and a covariant `clone`, both handled before dispatch). Its table therefore *is*
        // `Object`'s, and building it from an absent class file yielded an empty one: every
        // `array.hashCode()` / `array.getClass()` missed its slot and died as a `NoSuchMethodError`
        // — a call javac emits with owner `java/lang/Object`, so the slot the site cached was right
        // and only the receiver's table was missing (#262).
        if class.starts_with('[') {
            return match self.get_or_load("java/lang/Object") {
                Some(_) => self.vtable("java/lang/Object").to_vec(),
                None => Vec::new(),
            };
        }
        // Inherit the superclass's table first (recursing to build it if needed).
        let super_name = self
            .get_or_load(class)
            .and_then(|cf| cf.class_name(cf.super_class).map(|s| s.to_string()));
        let mut entries: Vec<VtableEntry> = match &super_name {
            Some(s) if self.get_or_load(s).is_some() => self.vtable(s).to_vec(),
            _ => Vec::new(),
        };

        // This class's own virtual methods (skip static, private, and <init>/<clinit>).
        let own: Vec<(String, String)> = match self.get_or_load(class) {
            Some(cf) => cf
                .methods
                .iter()
                .filter(|m| !m.is_static() && !m.is_private())
                .filter_map(|m| {
                    let name = cf.utf8(m.name_index)?;
                    let descriptor = cf.utf8(m.descriptor_index)?;
                    (!name.starts_with('<')).then(|| (name.to_string(), descriptor.to_string()))
                })
                .collect(),
            None => Vec::new(),
        };

        // Override an inherited slot, or append a new one.
        for (name, descriptor) in own {
            let Some(method) = self.resolve_method(class, &name, &descriptor) else {
                continue;
            };
            match entries.iter().position(|e| e.name == name && e.descriptor == descriptor) {
                Some(slot) => entries[slot].method = method,
                None => entries.push(VtableEntry { name, descriptor, method }),
            }
        }

        // JSR 335: fold in the **default methods** of the superinterfaces. A signature the class
        // hierarchy already provides keeps its slot (a class method always beats a default); for
        // the rest, walk breadth-first from the direct interfaces so a more-specific interface is
        // visited before the ones it extends — its default shadows theirs (the maximally-specific
        // rule for every shape javac emits; the unrelated-diamond conflict, ICCE per JVMS §5.4.6,
        // can't reach us because javac refuses to compile the implementor without an override).
        // The superclass's interfaces need no walk here: they're already in its inherited table.
        // An *abstract* interface method has no Code, so `resolve_method` returns `None` and it
        // never lands a slot — only real defaults do.
        let mut queue: Vec<String> = match self.get_or_load(class) {
            Some(cf) => {
                cf.interfaces.iter().filter_map(|&i| cf.class_name(i).map(str::to_string)).collect()
            }
            None => Vec::new(),
        };
        let mut seen: std::collections::HashSet<String> = queue.iter().cloned().collect();
        let mut at = 0;
        while at < queue.len() {
            let iface = queue[at].clone();
            at += 1;
            let (candidates, supers) = match self.get_or_load(&iface) {
                Some(cf) => (
                    cf.methods
                        .iter()
                        .filter(|m| !m.is_static() && !m.is_private())
                        .filter_map(|m| {
                            let name = cf.utf8(m.name_index)?;
                            let descriptor = cf.utf8(m.descriptor_index)?;
                            (!name.starts_with('<'))
                                .then(|| (name.to_string(), descriptor.to_string()))
                        })
                        .collect::<Vec<_>>(),
                    cf.interfaces
                        .iter()
                        .filter_map(|&i| cf.class_name(i).map(str::to_string))
                        .collect::<Vec<_>>(),
                ),
                None => continue,
            };
            for (name, descriptor) in candidates {
                if entries.iter().any(|e| e.name == name && e.descriptor == descriptor) {
                    continue; // already provided by the class hierarchy or a more-specific default
                }
                let Some(method) = self
                    .resolve_method(&iface, &name, &descriptor)
                    .filter(|&m| !self.is_abstract(m))
                else {
                    continue; // abstract — declares the signature, provides nothing to inherit
                };
                entries.push(VtableEntry { name, descriptor, method });
            }
            for s in supers {
                if seen.insert(s.clone()) {
                    queue.push(s);
                }
            }
        }
        entries
    }

    /// The class name whose `Class<…>` mirror sits at heap `offset` — the reverse of
    /// the mirror index, used to recover an object's class from the `class_id` in its
    /// header (e.g. for `invokevirtual`'s dynamic dispatch).
    /// O(1) via [`Self::mirror_names`] — this sits on the `invokevirtual` /
    /// `invokeinterface` hot path, where the scan it replaces cost one comparison per
    /// loaded class *per call*.
    pub fn class_name_at_mirror(&self, offset: usize) -> Option<&str> {
        self.mirror_names.get(&offset).map(String::as_str)
    }

    /// The class's identity **UUID** ("Class ID"), minting and caching one the
    /// first time `name` is seen. The dedup point: the same class name always maps
    /// to the same UUID, however many times it's referenced. Also records the
    /// reverse (UUID → name) so the id can be resolved back to its class.
    pub fn class_id(&mut self, name: &str) -> &str {
        if !self.class_ids.contains_key(name) {
            let id = self.uuid_gen.next();
            self.names_by_id.insert(id.clone(), name.to_string());
            self.class_ids.insert(name.to_string(), id);
        }
        self.class_ids[name].as_str()
    }

    /// Whether `name` already has a Class ID minted — a *non-minting* check (unlike
    /// [`Self::class_id`], which mints on first sight). Used as the "already
    /// prepared?" guard so asking the question doesn't itself create the id.
    pub fn has_class_id(&self, name: &str) -> bool {
        self.class_ids.contains_key(name)
    }

    /// The Class ID for `name` if it's already been minted, without minting one — the read-only
    /// twin of [`Self::class_id`] for the W2c lock-free (`&self`) allocation path.
    pub fn class_id_read(&self, name: &str) -> Option<&str> {
        self.class_ids.get(name).map(|id| id.as_str())
    }

    /// The loaded class whose Class ID is `uuid`, or `None` if the UUID is unknown
    /// or its class hasn't been loaded yet. The reverse of [`Self::class_id`]: used
    /// to resolve an object's class from the id carried in its header.
    pub fn class_by_id(&self, uuid: &str) -> Option<&ClassFile> {
        let name = self.names_by_id.get(uuid)?;
        self.classes.get(name)
    }

    /// The heap offset of the `Class<…>` object for Class ID `uuid`, if it's been
    /// prepared. `None` until Preparation `malloc`s and registers it. Keyed by the
    /// stable id — resolve a class name to its id with [`Self::class_id`] first.
    pub fn class_object(&self, uuid: &str) -> Option<usize> {
        self.class_objects.get(uuid).copied()
    }

    /// The heap offset of `name`'s `Class<…>` mirror — the lock object for a `static
    /// synchronized` method. Resolves the name to its Class ID (minting one if needed),
    /// then looks up the mirror; `None` until the class has been prepared.
    pub fn class_mirror(&mut self, name: &str) -> Option<usize> {
        let uuid = self.class_id(name).to_string();
        self.class_object(&uuid)
    }

    /// Records that the `Class<…>` object for Class ID `uuid` lives at `offset` —
    /// called by Preparation once the mirror has been allocated.
    /// Also maintains the reverse index [`Self::mirror_names`], which the hot
    /// [`Self::class_name_at_mirror`] reads — this is the **only** mutation point of
    /// `class_objects`, so the two cannot drift apart. Re-registering a Class ID at a new
    /// offset (never happens today — every caller dedups on [`Self::class_object`] first)
    /// retires the old offset's entry rather than leaving it stale.
    pub fn set_class_object(&mut self, uuid: &str, offset: usize) {
        if let Some(previous) = self.class_objects.insert(uuid.to_string(), offset) {
            if previous != offset {
                self.mirror_names.remove(&previous);
            }
        }
        if let Some(name) = self.names_by_id.get(uuid) {
            self.mirror_names.insert(offset, name.clone());
        }
    }

    /// The whole mirror index as `(Class ID, class name, offset)` rows, sorted by
    /// offset. The map is keyed by Class ID; each id is resolved back to its name
    /// (via the reverse index) for display. For tooling labelling the heap.
    /// The pooled `String` for these code units, if it has been created.
    pub fn interned_string(&self, units: &[u16]) -> Option<usize> {
        self.interned.get(units).copied()
    }

    /// Records the pooled `String` for these code units. Called once per distinct literal.
    pub fn set_interned_string(&mut self, units: Vec<u16>, offset: usize) {
        self.interned.insert(units, offset);
    }

    /// Every pooled `String`, for the collector.
    ///
    /// Two things depend on this and they are different: the pool must be a **root** (a literal is
    /// reachable from nowhere else between two `ldc`s of it, so without this the first collection
    /// frees it and the next `ldc` hands back a dead offset) and it must be **pinned** (a moved
    /// literal would leave every reference to it dangling, and unlike an ordinary object there is
    /// no second copy of the identity to restore).
    pub fn interned_offsets(&self) -> Vec<usize> {
        self.interned.values().copied().collect()
    }

    pub fn class_object_offsets(&self) -> Vec<(&str, &str, usize)> {
        let mut rows: Vec<(&str, &str, usize)> = self
            .class_objects
            .iter()
            .filter_map(|(uuid, &offset)| {
                self.names_by_id.get(uuid).map(|n| (uuid.as_str(), n.as_str(), offset))
            })
            .collect();
        rows.sort_by_key(|&(_, _, offset)| offset);
        rows
    }

    /// Stores an already-parsed class under `name` (replacing any prior one),
    /// attributed to the **application** loader (an explicitly-added class — the
    /// entry — is always a user class). The low-level insert that loading builds on.
    pub fn add(&mut self, name: String, class: ClassFile) {
        self.loaders.entry(name.clone()).or_insert(ClassLoader::Application);
        self.classes.insert(name, class);
    }

    /// Which loader defined `name`, if it's loaded.
    pub fn loader_of(&self, name: &str) -> Option<ClassLoader> {
        self.loaders.get(name).copied()
    }

    /// Reads a loaded class. `None` if it hasn't been loaded yet — this only looks
    /// up, it never loads.
    pub fn get(&self, name: &str) -> Option<&ClassFile> {
        self.classes.get(name)
    }

    /// Lazy class loading — the JVM's real behaviour. Returns the class if it's
    /// already loaded; otherwise finds `<name>.class` on the classpath, parses it,
    /// `add`s it and returns it. Just `add` + `get` fused behind one entry point.
    pub fn get_or_load(&mut self, name: &str) -> Option<&ClassFile> {
        if !self.classes.contains_key(name) {
            let (class, loader) = self.find_on_classpath(name)?;
            self.loaders.insert(name.to_string(), loader);
            self.classes.insert(name.to_string(), class);
        }
        self.get(name)
    }

    /// Resolves a **call** from its bytecode operand: the constant-pool `index`
    /// (the `00 07`) read against `caller_class`'s own pool. Reads the already
    /// parsed `Methodref`, resolves the target method, and caches the handle under
    /// `(caller_class, index)` so the next run of that same code is a direct
    /// lookup. This is the JVM's symbolic-reference *resolution*, done once.
    pub fn resolve_call(&mut self, caller_class: &str, index: u16) -> Option<MethodId> {
        let key = (caller_class.to_string(), index);
        if let Some(&id) = self.resolved_calls.get(&key) {
            return Some(id);
        }
        let (class, name, descriptor) = {
            let cf = self.get_or_load(caller_class)?;
            let (c, n, d) = cf.methodref_target(index)?;
            (c.to_string(), n.to_string(), d.to_string())
        };
        let id = self.resolve_method(&class, &name, &descriptor)?;
        self.resolved_calls.insert(key, id);
        Some(id)
    }

    /// **The read-only half of [`Self::resolve_call`]**, for the JIT: the target of `caller_class`'s
    /// methodref `index` **if it has already been resolved**, and `None` otherwise. Loads nothing,
    /// parses nothing, and inserts nothing.
    ///
    /// It exists for the *cold call site*. `burst` normally learns what an invoke binds to from the
    /// F0 quickened site, which the interpreter fills the first time it executes that pc — a better
    /// filter than resolution, because it offers inlining only for calls that have really happened.
    /// But a hot method can perfectly well contain a call it has never made: a branch that has not
    /// been taken yet, an error path, a `if (debug) log(…)`. Such a site leaves its cache cell at
    /// zero, and one of them used to refuse the whole method.
    ///
    /// It does not have to, and this is the narrow condition under which it does not: a statically
    /// bound call needs nothing from the receiver, so all the site was ever going to tell us is
    /// *which method* — and that answer is already sitting in the two resolution caches whenever
    /// **some** site in this class, or any site anywhere, has resolved the same triple. Reading it
    /// back is free of the two things compilation may not do: it cannot load a class (the `classes`
    /// lookup is a plain `get`) and it cannot fail with a linkage error (a resolution that is in the
    /// cache is one that already succeeded).
    ///
    /// What it deliberately does **not** do is resolve from scratch. A methodref whose class has
    /// never been loaded stays unanswered, because loading it here would be a compilation with a
    /// side effect, and a `<clinit>` is exactly the thing compiled code cannot run.
    pub fn resolved_call_readonly(&self, caller_class: &str, index: u16) -> Option<MethodId> {
        if let Some(&id) = self.resolved_calls.get(&(caller_class.to_string(), index)) {
            return Some(id);
        }
        let cf = self.classes.get(caller_class)?;
        let (class, name, descriptor) = cf.methodref_target(index)?;
        self.resolved.get(&(class.to_string(), name.to_string(), descriptor.to_string())).copied()
    }

    /// Resolves a method by name+descriptor to a [`MethodId`], loading its class
    /// and parsing its `Code` the first time, then caching the handle. `None` if
    /// the class can't be loaded or the method has no body.
    /// La clase que **declara** `name`+`descriptor` empezando por `class`, siguiendo el orden de
    /// JVMS 5.4.3.3: la clase, sus superclases, y despues sus superinterfaces (en anchura).
    ///
    /// Devuelve el **nombre** y no el `ClassFile` porque cargar un ancestro puede insertar en
    /// `self.classes`, y sostener un prestamo del mapa mientras tanto no compila.
    fn declaring_class(&mut self, class: &str, name: &str, descriptor: &str) -> Option<String> {
        // `<init>` y `<clinit>` **no se heredan** (JVMS §2.9): no son metodos que la resolucion
        // busque, son metodos de inicializacion que la VM invoca implicitamente sobre una clase
        // concreta. Subir por la jerarquia buscandolos hace que una clase sin `<clinit>` propio
        // "encuentre" el de su superclase y lo corra por segunda vez, en el contexto equivocado
        // -- lo que rompia la inicializacion de superinterfaces con `default`.
        if name.starts_with('<') {
            let has = self.get_or_load(class).is_some_and(|cf| {
                cf.methods.iter().any(|m| {
                    cf.utf8(m.name_index) == Some(name)
                        && cf.utf8(m.descriptor_index) == Some(descriptor)
                })
            });
            return has.then(|| class.to_string());
        }
        let declares = |cf: &ClassFile| {
            cf.methods.iter().any(|m| {
                cf.utf8(m.name_index) == Some(name)
                    && cf.utf8(m.descriptor_index) == Some(descriptor)
            })
        };
        // La clase y su cadena de superclases.
        let mut at = Some(class.to_string());
        let mut interfaces: Vec<String> = Vec::new();
        while let Some(cur) = at {
            let (found, supers, sup) = match self.get_or_load(&cur) {
                Some(cf) => (
                    declares(cf),
                    cf.interfaces.iter().filter_map(|&i| cf.class_name(i).map(str::to_string)).collect::<Vec<_>>(),
                    cf.class_name(cf.super_class).map(str::to_string),
                ),
                None => break,
            };
            if found {
                return Some(cur);
            }
            interfaces.extend(supers);
            at = sup;
        }
        // Y despues las superinterfaces, en anchura. Un metodo `default` vive aca.
        let mut seen: std::collections::HashSet<String> = interfaces.iter().cloned().collect();
        let mut i = 0;
        while i < interfaces.len() {
            let iface = interfaces[i].clone();
            i += 1;
            let (found, supers) = match self.get_or_load(&iface) {
                Some(cf) => (
                    declares(cf),
                    cf.interfaces.iter().filter_map(|&x| cf.class_name(x).map(str::to_string)).collect::<Vec<_>>(),
                ),
                None => continue,
            };
            if found {
                return Some(iface);
            }
            for s in supers {
                if seen.insert(s.clone()) {
                    interfaces.push(s);
                }
            }
        }
        None
    }

    pub fn resolve_method(&mut self, class: &str, name: &str, descriptor: &str) -> Option<MethodId> {
        let key = (class.to_string(), name.to_string(), descriptor.to_string());
        if let Some(&id) = self.resolved.get(&key) {
            return Some(id);
        }
        self.get_or_load(class)?; // make sure the class is loaded
        // JVMS 5.4.3.3: la resolucion busca en la clase, **despues en sus superclases** y despues
        // en sus superinterfaces. Buscar solo en la nombrada devolvia `None` para cualquier metodo
        // **heredado**, el sitio quedaba `NoTarget`, no se empujaba nada y el llamador moria con
        // `operand stack underflow` -lejos del origen y sin decir que fue-.
        //
        // No es un caso raro: `super.m()` emite `invokespecial <superclase directa>.m`, que es lo
        // que emite el javac real, y la superclase directa **no tiene por que declarar el metodo**.
        let declaring = self.declaring_class(class, name, descriptor)?;
        let declaring = declaring.as_str();
        let (max_locals, code, exceptions, native_, abstract_, synchronized_, static_) = {
            let cf = self.classes.get(declaring)?;
            let member = cf.methods.iter().find(|m| {
                cf.utf8(m.name_index) == Some(name)
                    && cf.utf8(m.descriptor_index) == Some(descriptor)
            })?;
            let (synchronized_, static_) = (member.is_synchronized(), member.is_static());
            if member.is_native() {
                // Native: no `Code`. We still record a body so it has a `MethodId`;
                // the invoke checks `is_native` and dispatches to the native bridge.
                (0, Vec::new(), Vec::new(), true, false, synchronized_, static_)
            } else if member.is_abstract() {
                // Abstract: no `Code` either, and for the same practical reason it must still
                // resolve — a `MethodId` is what lets it take a vtable slot, and the slot is what
                // an override replaces. Skipping it here is what made a call through an abstract
                // declaration miss the table entirely (COMPILER_FINDINGS #230).
                (0, Vec::new(), Vec::new(), false, true, synchronized_, static_)
            } else {
                let body = cf.member_code(member)?;
                (body.max_locals as usize, body.code, body.exception_table, false, false, synchronized_, static_)
            }
        };
        let id = self.methods.len();
        // Only methods that *might* contain a field access get a site table (a naive byte
        // scan: a `0xb4`/`0xb5` appearing as an operand only costs an unused table, while a
        // real one is never missed). Everything else keeps an empty `Vec` — no per-method
        // allocation for the majority of methods that never touch an instance field.
        let field_sites = match code.iter().any(|&b| b == 0xb4 || b == 0xb5) {
            true => (0..code.len()).map(|_| AtomicU64::new(0)).collect(),
            false => Vec::new(),
        };
        // Same deal for the call sites: only a method whose bytes contain an `invoke*`
        // (`0xb6`–`0xb9`) pays for a table. `invokedynamic` (`0xba`) is deliberately out — its
        // call sites are resolved through the condy/lambda machinery, not this cache.
        let call_sites = match code.iter().any(|&b| (0xb6..=0xb9).contains(&b)) {
            true => (0..code.len()).map(|_| AtomicU64::new(0)).collect(),
            false => Vec::new(),
        };
        // The inline cache's observations (F2). Only a method that can *dispatch* — one whose bytes
        // contain an `invokevirtual` or an `invokeinterface` — pays for the table; a body full of
        // `invokestatic`s is statically bound and has nothing to observe.
        let receiver_classes = match code.iter().any(|&b| b == 0xb6 || b == 0xb9) {
            true => (0..code.len()).map(|_| AtomicU32::new(0)).collect(),
            false => Vec::new(),
        };
        // `[1, param widths…]` — receiver-first, so an instance call takes the whole slice and a
        // static one takes `[1..]`. Parsed here, once, instead of per call.
        let mut slot_widths = vec![1];
        slot_widths.extend(Self::param_slot_widths(descriptor));
        self.methods.push(MethodBody {
            class: class.to_string(),
            name: name.to_string(),
            descriptor: descriptor.to_string(),
            max_locals,
            arg_count: argument_count(descriptor),
            code,
            exceptions,
            native_,
            abstract_,
            synchronized_,
            static_,
            field_sites,
            call_sites,
            receiver_classes,
            slot_widths,
            intrinsic: classify_intrinsic(class, name, descriptor),
        });
        self.resolved.insert(key, id);
        Some(id)
    }

    /// The bytecode of a resolved method. Every frame of that method shares it.
    pub fn code(&self, method: MethodId) -> &[u8] {
        &self.methods[method].code
    }

    /// A resolved method's exception table — the `try`/`catch` ranges `athrow`
    /// searches for a handler.
    pub fn exception_table(&self, method: MethodId) -> &[ExceptionTableEntry] {
        &self.methods[method].exceptions
    }

    /// A resolved method's local-slot count (for building its frame).
    pub fn max_locals(&self, method: MethodId) -> usize {
        self.methods[method].max_locals
    }

    /// A resolved method's declared argument count (how many values an
    /// `invokestatic` pops off the caller's stack).
    pub fn arg_count(&self, method: MethodId) -> usize {
        self.methods[method].arg_count
    }

    /// Whether a resolved method is `native` (no bytecode → dispatched to the bridge).
    pub fn is_native(&self, method: MethodId) -> bool {
        self.methods[method].native_
    }

    /// Whether a resolved method is `abstract` — bodiless, and an `AbstractMethodError` if a
    /// dispatch ever selects it (JVMS §6.5).
    pub fn is_abstract(&self, method: MethodId) -> bool {
        self.methods[method].abstract_
    }

    /// Whether a resolved method is `synchronized` (`ACC_SYNCHRONIZED`) — the VM takes
    /// the object monitor on entry and releases it on every exit (no opcode involved).
    pub fn is_synchronized(&self, method: MethodId) -> bool {
        self.methods[method].synchronized_
    }

    /// Whether a resolved method is `static` — i.e. whether local slot 0 is the first argument
    /// or `this`. Asked by the JIT, for which that is the difference between a frame whose slot 0
    /// is an `int` and one whose slot 0 is a reference (`burst::compile`'s entry type map).
    pub fn is_static(&self, method: MethodId) -> bool {
        self.methods[method].static_
    }

    /// Argument count parsed straight from a method `descriptor` — for callers that
    /// have the descriptor but no resolved [`MethodId`] (e.g. an `invokespecial`
    /// whose target class can't be loaded, like `java.lang.Object.<init>`, and so
    /// must still pop the right number of operands).
    pub fn descriptor_arg_count(descriptor: &str) -> usize {
        argument_count(descriptor)
    }

    /// The slot width of each parameter in `descriptor` — `2` for the category-2
    /// types (`long`/`double`), `1` otherwise. The caller lays a call's arguments
    /// into the callee's locals by these widths, so a `long` parameter leaves its
    /// high-half slot empty and the next parameter lands one slot further along.
    pub fn param_slot_widths(descriptor: &str) -> Vec<usize> {
        let bytes = descriptor.as_bytes();
        let mut i = 1; // skip '('
        let mut widths = Vec::new();
        while i < bytes.len() && bytes[i] != b')' {
            let is_array = bytes[i] == b'[';
            while i < bytes.len() && bytes[i] == b'[' {
                i += 1;
            }
            // A bare `long`/`double` is category-2 (2 slots); an *array* of them is a
            // reference (1 slot).
            let width = if !is_array && matches!(bytes.get(i), Some(b'J') | Some(b'D')) { 2 } else { 1 };
            i += match bytes.get(i) {
                Some(b'L') => bytes[i..].iter().position(|&c| c == b';').map_or(1, |p| p + 1),
                _ => 1,
            };
            widths.push(width);
        }
        widths
    }

    /// The binary name of the class a resolved method belongs to (for resolving
    /// that method's own constant-pool references).
    pub fn class_of(&self, method: MethodId) -> &str {
        &self.methods[method].class
    }

    /// A resolved method's own name (for tooling labels).
    pub fn name(&self, method: MethodId) -> &str {
        &self.methods[method].name
    }

    /// A resolved method's descriptor — needed to tell overloads apart once a call has
    /// already been resolved (`valueOf(Object)` from `valueOf(int)`, say).
    pub fn descriptor(&self, method: MethodId) -> &str {
        &self.methods[method].descriptor
    }

    /// The **field-site cache** entry for the field access at `pc` of `method`, or `0`
    /// ("not resolved yet") — also what an out-of-range `pc` or a method with no site
    /// table yields, so a miss is always just the slow path. `&self`: the lock-free read
    /// path reads it too. The payload is packed by `objects_operations`.
    pub fn field_site(&self, method: MethodId, pc: usize) -> u64 {
        match self.methods[method].field_sites.get(pc) {
            Some(cell) => cell.load(Ordering::Relaxed),
            None => 0,
        }
    }

    /// Fills the field-site cache entry for `(method, pc)`. Takes `&self` (the cells are
    /// atomic) so the H3 W3 lock-free **read** path can populate the cache as well as the
    /// write path — two threads resolving the same site compute the *same* `packed`, so
    /// the race is benign and `Relaxed` suffices (the word is self-contained; it publishes
    /// no other memory). A `pc` with no cell (unresolvable site table) is a silent no-op.
    pub fn set_field_site(&self, method: MethodId, pc: usize, packed: u64) {
        if let Some(cell) = self.methods[method].field_sites.get(pc) {
            cell.store(packed, Ordering::Relaxed);
        }
    }

    /// The **call-site cache** entry for the `invoke*` at `pc` of `method`, or `0` ("not resolved
    /// yet") — also what an out-of-range `pc` or a method with no site table yields, so a miss is
    /// always just the slow path. Twin of [`Self::field_site`]; the payload is packed by
    /// `bytecode_interpreter::call_site`.
    pub fn call_site(&self, method: MethodId, pc: usize) -> u64 {
        match self.methods[method].call_sites.get(pc) {
            Some(cell) => cell.load(Ordering::Relaxed),
            None => 0,
        }
    }

    /// Fills the call-site cache entry for `(method, pc)`. `&self` and `Relaxed` for the same
    /// reasons as [`Self::set_field_site`]: two threads resolving the same site compute the
    /// *same* word (resolution is a pure function of the site), so the race is benign and the
    /// word publishes no other memory. A `pc` with no cell is a silent no-op.
    pub fn set_call_site(&self, method: MethodId, pc: usize, packed: u64) {
        if let Some(cell) = self.methods[method].call_sites.get(pc) {
            cell.store(packed, Ordering::Relaxed);
        }
    }

    /// The **last receiver class** the dispatched call at `pc` of `method` was made on — the heap
    /// offset of its `Class<…>` mirror — or `0` for a site that has never run (and for a `pc` with
    /// no cell at all). See [`MethodBody::receiver_classes`]; this is the JIT's only source of
    /// profile, and `0` is what makes a never-executed site simply not inlinable.
    pub fn receiver_class(&self, method: MethodId, pc: usize) -> u32 {
        match self.methods[method].receiver_classes.get(pc) {
            Some(cell) => cell.load(Ordering::Relaxed),
            None => 0,
        }
    }

    /// Records the receiver class of the dispatched call at `pc`. One `Relaxed` store on a path
    /// that has already read the same word out of the object's header, and a `pc` with no cell is a
    /// silent no-op — exactly like the two site caches above.
    pub fn set_receiver_class(&self, method: MethodId, pc: usize, mirror: u32) {
        if let Some(cell) = self.methods[method].receiver_classes.get(pc) {
            cell.store(mirror, Ordering::Relaxed);
        }
    }

    /// A resolved method's argument slot widths **with the receiver's slot in front**
    /// (`[1, param widths…]`) — what an instance call (`invokevirtual`/`invokespecial`/
    /// `invokeinterface`) lays its `[receiver, args…]` out by. Precomputed at resolution.
    pub fn receiver_slot_widths(&self, method: MethodId) -> &[usize] {
        &self.methods[method].slot_widths
    }

    /// The same table without the receiver's slot — what a `static` call lays its arguments out
    /// by. It is literally the tail of [`Self::receiver_slot_widths`], so both share one `Vec`.
    pub fn param_slot_widths_of(&self, method: MethodId) -> &[usize] {
        &self.methods[method].slot_widths[1..]
    }

    /// Whether the VM intercepts this method rather than running its body — see [`Intrinsic`].
    pub fn intrinsic(&self, method: MethodId) -> Intrinsic {
        self.methods[method].intrinsic
    }

    /// Whether the class declaring `method` has **finished** initializing (`Done`). This is the
    /// only initialization state a call site may cache: `Done` is terminal (JVMS §5.5 has no
    /// transition out of it), whereas `NotStarted`/`InProgress` still have work to do and
    /// `Erroneous` must keep throwing `NoClassDefFoundError` on *every* use. Borrows the class
    /// name from the method body, so the check costs no allocation.
    pub fn declaring_class_initialized(&self, method: MethodId) -> bool {
        matches!(self.init_states.get(self.methods[method].class.as_str()), Some(InitState::Done))
    }

    /// Interns a `(name, descriptor)` call-site signature and hands back its [`SignatureId`].
    /// Called once per `invokeinterface` site, on its first execution.
    pub fn intern_signature(&mut self, name: &str, descriptor: &str) -> SignatureId {
        let key = (name.to_string(), descriptor.to_string());
        if let Some(&id) = self.signature_ids.get(&key) {
            return id;
        }
        let id = self.signatures.len() as SignatureId;
        self.signatures.push(key.clone());
        self.signature_ids.insert(key, id);
        id
    }

    /// Dynamic dispatch **without naming the receiver's class**: the method at `slot` of the
    /// vtable of whichever class owns the `Class` mirror at heap offset `mirror`. Folding the
    /// `class_name_at_mirror` → `vtable_method` pair into one call is what removes the
    /// `runtime_class.to_string()` every `invokevirtual` used to pay — the borrow of the name
    /// never has to escape the metaspace.
    pub fn vtable_method_at_mirror(&mut self, mirror: usize, slot: usize) -> Option<MethodId> {
        self.build_vtable_at_mirror(mirror)?;
        let name = self.class_name_at_mirror(mirror)?;
        self.vtables.get(name)?.get(slot).map(|e| e.method)
    }

    /// The `invokeinterface` twin of [`Self::vtable_method_at_mirror`]: an interface call has no
    /// stable slot, so it searches the receiver's own table for the interned `signature` instead.
    pub fn vtable_method_at_mirror_by_signature(
        &mut self,
        mirror: usize,
        signature: SignatureId,
    ) -> Option<MethodId> {
        self.build_vtable_at_mirror(mirror)?;
        let name = self.class_name_at_mirror(mirror)?;
        let (want_name, want_descriptor) = self.signatures.get(signature as usize)?;
        self.vtables
            .get(name)?
            .iter()
            .find(|e| e.name == *want_name && e.descriptor == *want_descriptor)
            .map(|e| e.method)
    }

    /// **The read-only twin of [`Self::vtable_method_at_mirror`]**, for the JIT (milestone F2).
    ///
    /// Compiling is deliberately unable to change the VM's state — it is triggered by a counter, at
    /// a moment the interpreter did not choose for its side effects — so it may not build a vtable
    /// the way the `&mut self` form does. It does not have to: this is asked only about a class the
    /// interpreter has **already dispatched on** at this very call site, and dispatching is what
    /// built the table. A `None` here therefore means "not yet", and the honest answer to "not yet"
    /// is to refuse the inline rather than to prepare for it.
    pub fn vtable_method_at_mirror_readonly(&self, mirror: usize, slot: usize) -> Option<MethodId> {
        let name = self.class_name_at_mirror(mirror)?;
        self.vtables.get(name)?.get(slot).map(|e| e.method)
    }

    /// The `invokeinterface` twin of [`Self::vtable_method_at_mirror_readonly`]: the same read-only
    /// lookup, searching the receiver class's own table for an interned signature instead of
    /// indexing it by a slot no interface has.
    pub fn vtable_method_at_mirror_by_signature_readonly(
        &self,
        mirror: usize,
        signature: SignatureId,
    ) -> Option<MethodId> {
        let name = self.class_name_at_mirror(mirror)?;
        let (want_name, want_descriptor) = self.signatures.get(signature as usize)?;
        self.vtables
            .get(name)?
            .iter()
            .find(|e| e.name == *want_name && e.descriptor == *want_descriptor)
            .map(|e| e.method)
    }

    /// Makes sure the vtable of the class whose mirror sits at `mirror` is built, so the two
    /// lookups above can then read it through a shared borrow. `None` if the offset names no class.
    fn build_vtable_at_mirror(&mut self, mirror: usize) -> Option<()> {
        let built = {
            let name = self.class_name_at_mirror(mirror)?;
            self.vtables.contains_key(name)
        };
        if !built {
            let name = self.class_name_at_mirror(mirror)?.to_string();
            let table = self.build_vtable(&name);
            self.vtables.insert(name, table);
        }
        Some(())
    }


    /// Searches the classpath for `<name>.class` and parses the first hit. The
    /// binary name's `/`s map straight onto path separators
    /// (`java/lang/Object` → `<dir>/java/lang/Object.class`).
    /// Finds and parses `<name>.class`, **delegating bootstrap-first** then to the
    /// application loader (the JVM's parent-first model). Returns the class together
    /// with the loader that found it.
    fn find_on_classpath(&self, name: &str) -> Option<(ClassFile, ClassLoader)> {
        for dir in &self.bootstrap {
            if let Some(class) = Self::read_class(dir, name) {
                return Some((class, ClassLoader::Bootstrap));
            }
        }
        // The image is part of the *bootstrap* loader: a class that comes out of it is
        // defined by the same loader as one from `boot/`, which keeps runtime identity
        // (`(name, defining loader)`) the same whether the VM booted from dirs or an image.
        if let Some(image) = &self.boot_image {
            if let Some(class) = image.class_bytes(name).and_then(|b| ClassFile::from_bytes(&b).ok()) {
                return Some((class, ClassLoader::Bootstrap));
            }
        }
        for dir in &self.application {
            if let Some(class) = Self::read_class(dir, name) {
                return Some((class, ClassLoader::Application));
            }
        }
        None
    }

    /// Parses `<dir>/<name>.class`, or `None` if it isn't there / doesn't parse.
    fn read_class(dir: &Path, name: &str) -> Option<ClassFile> {
        let path = dir.join(format!("{name}.class"));
        ClassFile::from_path(path.to_str()?).ok()
    }
}

/// Number of arguments a method declares, parsed from its descriptor's `(...)`.
/// Scans the field-type descriptors between the parens — base types are one char,
/// `L…;` runs to the semicolon, `[` prefixes belong to the type that follows (so
/// an array is one argument, not two). Everything is one slot wide for now, so
/// this doubles as the slot count.
fn argument_count(descriptor: &str) -> usize {
    let bytes = descriptor.as_bytes();
    let mut i = 1; // skip '('
    let mut count = 0;
    while i < bytes.len() && bytes[i] != b')' {
        // Array dimension prefixes don't start a new argument.
        while i < bytes.len() && bytes[i] == b'[' {
            i += 1;
        }
        i += match bytes.get(i) {
            Some(b'L') => bytes[i..].iter().position(|&c| c == b';').map_or(1, |p| p + 1),
            _ => 1, // single-char base type
        };
        count += 1;
    }
    count
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Corre `run()I` de una clase de `java/` y devuelve el entero. Es el mismo arnes que usa
    /// `gc::tests::run_int`, replicado aca para que estos dos probes vivan al lado del codigo que
    /// ejercen.
    fn run_probe(class_file: &str) -> i32 {
        use crate::jvm::class_file::ClassFile;
        use crate::jvm::interpreter::bytecode_interpreter::execute;
        use crate::jvm::interpreter::frame::{Frame, Value};
        let mut metaspace = MetaspaceService::new(
            vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")],
            vec![PathBuf::from("java")],
        );
        let class = ClassFile::from_path(class_file).expect("load class");
        let name = class.class_name(class.this_class).unwrap().to_string();
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, "run", "()I").expect("run()");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        match execute(metaspace, frame) {
            Some(Value::Int(v)) => v,
            other => panic!("se esperaba un int, salio {other:?}"),
        }
    }

    /// #265 (JVMS §5.4.3.3): la resolucion busca en la clase, **despues en sus superclases** y
    /// despues en sus superinterfaces. Buscar solo en la nombrada devolvia `None` para todo metodo
    /// heredado, y el llamador moria con `operand stack underflow` lejos del origen.
    ///
    /// Un bit por propiedad: heredado por superclase (1), `default` de una superinterfaz (2),
    /// `static` heredado (4).
    #[test]
    fn method_resolution_walks_superclasses_and_superinterfaces() {
        assert_eq!(run_probe("java/SuperProbe.class"), 7);
    }

    /// #262 (JLS §10.7): los miembros de un array son los de `Object`. Su clase es sintetica, asi
    /// que la tabla se construia **vacia** y su mirror se alocaba **sin header**.
    ///
    /// Un bit por propiedad: `hashCode` despacha (1), `getClass` devuelve algo (2), ese algo tiene
    /// clase (4, por `instanceof` — que lo resuelve la VM leyendo el header, sin correr una linea
    /// de `java.lang.Class`), `equals` despacha (8).
    #[test]
    fn an_arrays_object_members_dispatch_and_its_mirror_has_a_class() {
        assert_eq!(run_probe("java/ArrayProbe.class"), 15);
    }

    #[test]
    fn loads_a_class_lazily() {
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        assert!(metaspace.get("Add").is_none()); // not loaded until asked for
        let class = metaspace.get_or_load("Add").expect("should load java/Add.class");
        assert_eq!(class.class_name(class.this_class), Some("Add"));
        assert!(metaspace.get("Add").is_some()); // now cached
    }

    #[test]
    fn loaders_delegate_bootstrap_first() {
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        // A core class is served by the bootstrap loader (found in boot/).
        metaspace.get_or_load("java/lang/Object").expect("Object should load from boot/");
        assert_eq!(metaspace.loader_of("java/lang/Object"), Some(ClassLoader::Bootstrap));
        // A user class comes from the application loader (java/).
        metaspace.get_or_load("Add").expect("Add should load from java/");
        assert_eq!(metaspace.loader_of("Add"), Some(ClassLoader::Application));
    }

    #[test]
    fn resolves_a_method_to_a_handle() {
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let id = metaspace.resolve_method("Add", "add", "(II)I").expect("resolve Add.add");
        // iload_0, iload_1, iadd, ireturn
        assert_eq!(metaspace.code(id), &[0x1a, 0x1b, 0x60, 0xac]);
        assert_eq!(metaspace.max_locals(id), 2);
        assert_eq!(metaspace.arg_count(id), 2);
        // resolving the same method again reuses the handle.
        assert_eq!(metaspace.resolve_method("Add", "add", "(II)I"), Some(id));
    }

    #[test]
    fn resolves_a_call_by_constant_pool_index() {
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        // #7 in Add's constant pool is Methodref Add.add:(II)I.
        let by_code = metaspace.resolve_call("Add", 7).expect("resolve Add #7");
        let by_name = metaspace.resolve_method("Add", "add", "(II)I").expect("resolve by name");
        assert_eq!(by_code, by_name); // same handle, two routes
    }

    #[test]
    fn counts_arguments() {
        assert_eq!(argument_count("(II)I"), 2);
        assert_eq!(argument_count("()V"), 0);
        assert_eq!(argument_count("(IJ)J"), 2);
        assert_eq!(argument_count("([Ljava/lang/String;)V"), 1);
        assert_eq!(argument_count("(Ljava/lang/Object;I)V"), 2);
    }

    #[test]
    fn class_id_is_stable_and_unique() {
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let first = metaspace.class_id("Counter").to_string();
        // Same class → same UUID, however many times it's asked for (dedup).
        assert_eq!(metaspace.class_id("Counter"), first);
        // A different class → a different UUID.
        assert_ne!(metaspace.class_id("Point"), first);
    }

    #[test]
    fn resolves_a_class_back_from_its_id() {
        let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let id = metaspace.class_id("Add").to_string();
        // The id exists, but the class isn't loaded yet → no class behind it.
        assert!(metaspace.class_by_id(&id).is_none());
        // Once loaded, the id resolves back to the class.
        metaspace.get_or_load("Add");
        assert_eq!(metaspace.class_by_id(&id).map(|c| c.this_class), Some(metaspace.get("Add").unwrap().this_class));
        // An unknown UUID resolves to nothing.
        assert!(metaspace.class_by_id("not-a-real-uuid").is_none());
    }
}
