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
}

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
    /// `true` for a `synchronized` method (`ACC_SYNCHRONIZED`). There is no opcode for
    /// it — the VM takes the receiver's (or `Class`'s) monitor when it pushes the frame
    /// and releases it when the frame is popped. See `JVM::push_frame_locked`.
    synchronized_: bool,
}

/// The Method Area: loaded classes, resolved method bodies, the per-index call
/// resolution cache, and where to find classes that aren't loaded yet.
pub struct MetaspaceService {
    /// The **bootstrap** loader's directories — searched *first* (delegation). Home
    /// of the core classes (`java.lang.*`).
    bootstrap: Vec<PathBuf>,
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
    class_objects: HashMap<String, usize>,
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
}

impl MetaspaceService {
    /// A metaspace whose loaders search `bootstrap` first, then `application`
    /// (the JVM's parent-first delegation).
    pub fn new(bootstrap: Vec<PathBuf>, application: Vec<PathBuf>) -> Self {
        MetaspaceService {
            bootstrap,
            application,
            loaders: HashMap::new(),
            classes: HashMap::new(),
            methods: Vec::new(),
            resolved: HashMap::new(),
            resolved_calls: HashMap::new(),
            class_objects: HashMap::new(),
            class_ids: HashMap::new(),
            names_by_id: HashMap::new(),
            uuid_gen: UuidGenerator::new(),
            vtables: HashMap::new(),
            init_states: HashMap::new(),
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
        entries
    }

    /// The class name whose `Class<…>` mirror sits at heap `offset` — the reverse of
    /// the mirror index, used to recover an object's class from the `class_id` in its
    /// header (e.g. for `invokevirtual`'s dynamic dispatch).
    pub fn class_name_at_mirror(&self, offset: usize) -> Option<&str> {
        let uuid = self.class_objects.iter().find(|&(_, &o)| o == offset).map(|(u, _)| u)?;
        self.names_by_id.get(uuid).map(String::as_str)
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
    pub fn set_class_object(&mut self, uuid: &str, offset: usize) {
        self.class_objects.insert(uuid.to_string(), offset);
    }

    /// The whole mirror index as `(Class ID, class name, offset)` rows, sorted by
    /// offset. The map is keyed by Class ID; each id is resolved back to its name
    /// (via the reverse index) for display. For tooling labelling the heap.
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

    /// Resolves a method by name+descriptor to a [`MethodId`], loading its class
    /// and parsing its `Code` the first time, then caching the handle. `None` if
    /// the class can't be loaded or the method has no body.
    pub fn resolve_method(&mut self, class: &str, name: &str, descriptor: &str) -> Option<MethodId> {
        let key = (class.to_string(), name.to_string(), descriptor.to_string());
        if let Some(&id) = self.resolved.get(&key) {
            return Some(id);
        }
        self.get_or_load(class)?; // make sure the class is loaded
        let (max_locals, code, exceptions, native_, synchronized_) = {
            let cf = self.classes.get(class)?;
            let member = cf.methods.iter().find(|m| {
                cf.utf8(m.name_index) == Some(name)
                    && cf.utf8(m.descriptor_index) == Some(descriptor)
            })?;
            let synchronized_ = member.is_synchronized();
            if member.is_native() {
                // Native: no `Code`. We still record a body so it has a `MethodId`;
                // the invoke checks `is_native` and dispatches to the native bridge.
                (0, Vec::new(), Vec::new(), true, synchronized_)
            } else {
                let body = cf.member_code(member)?;
                (body.max_locals as usize, body.code, body.exception_table, false, synchronized_)
            }
        };
        let id = self.methods.len();
        self.methods.push(MethodBody {
            class: class.to_string(),
            name: name.to_string(),
            max_locals,
            arg_count: argument_count(descriptor),
            code,
            exceptions,
            native_,
            synchronized_,
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

    /// Whether a resolved method is `synchronized` (`ACC_SYNCHRONIZED`) — the VM takes
    /// the object monitor on entry and releases it on every exit (no opcode involved).
    pub fn is_synchronized(&self, method: MethodId) -> bool {
        self.methods[method].synchronized_
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
