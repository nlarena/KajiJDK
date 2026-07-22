//! `invokedynamic` (0xba) — the call site whose target isn't in the class file.
//!
//! Every other `invoke*` names its method in the constant pool: the VM looks it up and
//! calls it. `invokedynamic` names **nothing callable**. It points at a *bootstrap
//! method* which, the first time the instruction runs, is asked to **produce** the
//! target — a `CallSite` — which is then linked in place and reused. That indirection
//! is what let Java add lambdas, string concatenation and records without inventing a
//! new opcode for each: the language ships the policy in the bootstrap method, and the
//! VM stays fixed.
//!
//! ## How we model it
//!
//! A faithful implementation would need the whole `java.lang.invoke` machinery —
//! `MethodHandle`, `MethodType`, `MethodHandles.Lookup` — plus factories like
//! `LambdaMetafactory` that **spin a new class at runtime**. That is a large subsystem,
//! and most of it is Java code we do not have.
//!
//! So we take the same route as the rest of our `java.lang`: **the bootstrap methods are
//! intrinsics**. We resolve the handle to its `(class, name)`, recognise the factory,
//! and synthesise the call site's behaviour in Rust — exactly the "a method the VM
//! resolves by itself" definition from `docs/intrinsecos.md`. The call site is rebuilt
//! per execution rather than cached, which is semantically identical for the pure
//! factories below (with no JIT, the linkage cost buys us nothing yet).
//!
//! ## What is supported
//!
//! `StringConcatFactory` — what every `"a" + b` compiles to since Java 9:
//!
//! - `makeConcatWithConstants`: the first bootstrap argument is a **recipe** string, in
//!   which the marker U+0001 splices the next *dynamic* argument, U+0002 splices the
//!   next *constant* bootstrap argument, and every other character is literal text.
//! - `makeConcat`: no recipe — just the arguments, in order.
//!
//! `LambdaMetafactory` is deliberately absent: it needs a runtime-generated class
//! implementing the functional interface, which is its own milestone.

use super::{class_operations, objects_operations, LambdaShape, JVM};
use crate::jvm::class_file::MethodHandleKind;
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::heap::HeapService;
use crate::jvm::interpreter::metaspace::MetaspaceService;
use crate::jvm::interpreter::strings;
use crate::jvm::parser::float_to_decimal;

/// The factory behind `"a" + b` since Java 9.
const STRING_CONCAT_FACTORY: &str = "java/lang/invoke/StringConcatFactory";
/// The factory behind a `switch` over patterns (Java 21).
const SWITCH_BOOTSTRAPS: &str = "java/lang/runtime/SwitchBootstraps";
/// The factory behind a `record`'s `equals`/`hashCode`/`toString`.
const OBJECT_METHODS: &str = "java/lang/runtime/ObjectMethods";
/// The factory behind lambdas and method references.
const LAMBDA_METAFACTORY: &str = "java/lang/invoke/LambdaMetafactory";
/// The factory behind **dynamic constants** (condy).
const CONSTANT_BOOTSTRAPS: &str = "java/lang/invoke/ConstantBootstraps";
/// Recipe marker: splice the next **dynamic** argument here.
const TAG_ARG: char = '\u{1}';
/// Recipe marker: splice the next **constant** bootstrap argument here.
const TAG_CONST: char = '\u{2}';

impl JVM {
/// Runs the `invokedynamic` at `cp_index`: resolve the bootstrap method, produce the
/// call site's value, and leave it on the operand stack.
///
/// An `impl JVM` method like its `invoke*` siblings, rather than a free function over
/// loose fields. The shape matters: resolving a bootstrap's static arguments can require
/// **evaluating another constant**, and evaluating one can require *running Java* — so
/// the whole VM, not a handful of borrowed pieces, is the honest receiver.
pub(super) fn invokedynamic(&mut self, cp_index: u16) {
    // Everything is read out of the caller's class file into *owned* values first: the
    // borrow of the `ClassFile` has to end before the VM can be used mutably — to intern
    // a String, load a class, or run a bootstrap.
    let caller = self.metaspace.class_of(self.frame().method()).to_string();
    let site = {
        let class = self.metaspace.get(&caller).expect("invokedynamic: caller class not loaded");
        // The call site's *name* is not decoration: `ObjectMethods` bootstraps all three
        // of a record's methods from one entry, and only the name says which.
        let (bsm_index, site_name, descriptor) = class
            .invokedynamic_site(cp_index)
            .expect("invokedynamic: cp_index is not an InvokeDynamic constant");
        let bootstrap_methods = class.bootstrap_methods();
        let bootstrap = bootstrap_methods
            .get(bsm_index as usize)
            .expect("invokedynamic: bootstrap method index out of range");
        let handle = class.method_handle(bootstrap.method_ref).unwrap_or_else(|| {
            panic!(
                "invokedynamic: constant pool #{} is not a resolvable MethodHandle",
                bootstrap.method_ref
            )
        });
        // Every bootstrap method is a static method, so a handle of any other kind here
        // means the class file is malformed rather than merely unsupported.
        assert!(
            !handle.kind.names_a_field(),
            "invokedynamic: the bootstrap handle names a field ({:?}), but a bootstrap \
             method must be invokable",
            handle.kind
        );
        // The static arguments mean something different per factory, so they are read
        // *here* — inside the borrow — and handed on as owned data. Reading them eagerly
        // for every factory would report a confusing "not a String" where the honest
        // answer is "no intrinsic for this factory".
        let bootstrap = match (handle.class, handle.name) {
            (STRING_CONCAT_FACTORY, name @ ("makeConcatWithConstants" | "makeConcat")) => {
                // The recipe is the first static argument; whatever follows are the
                // constants its U+0002 markers splice in. `makeConcat` has neither.
                //
                // Each is read *positionally*: skipping one we can't model would shift
                // the rest up and splice the wrong text at each marker — a silently
                // wrong answer, the failure mode worth going out of the way to avoid.
                let mut texts = bootstrap.arguments.iter().enumerate().map(|(position, &i)| {
                    class.string_constant(i).map(str::to_string).unwrap_or_else(|| {
                        panic!(
                            "invokedynamic: bootstrap static argument #{position} (constant \
                             pool #{i}) is not a String constant. StringConcatFactory also \
                             allows int/long/float/double/Class/MethodHandle/MethodType \
                             there, which needs the wider `ldc` support (see \
                             docs/invokedynamic-ruta.md, D1)"
                        )
                    })
                });
                let recipe = if name == "makeConcat" { None } else { texts.next() };
                Bootstrap::StringConcat { recipe, constants: texts.collect() }
            }
            (SWITCH_BOOTSTRAPS, "typeSwitch") => {
                // The static arguments are the case labels in source order, and they are
                // not all the same kind: a **type** pattern gives a `Class` constant,
                // while an **enum** pattern gives a *dynamic* constant that has to be
                // computed. Only the indices are captured here; resolving them needs the
                // whole VM, which the class-file borrow is still holding.
                let labels = bootstrap
                    .arguments
                    .iter()
                    .map(|&i| match class.class_name(i) {
                        Some(name) => Label::Type(name.to_string()),
                        None => Label::Dynamic(i),
                    })
                    .collect();
                Bootstrap::TypeSwitch { labels }
            }
            (OBJECT_METHODS, "bootstrap") => {
                // Static arguments: the record's `Class`, a `;`-separated list of the
                // component names, then one getter handle per component. All three call
                // sites (`equals`/`hashCode`/`toString`) share this one entry.
                let mut arguments = bootstrap.arguments.iter();
                let record_class = arguments
                    .next()
                    .and_then(|&i| class.class_name(i))
                    .map(str::to_string)
                    .expect("ObjectMethods: the 1st static argument must be the record Class");
                let names = arguments
                    .next()
                    .and_then(|&i| class.string_constant(i))
                    .map(str::to_string)
                    .expect("ObjectMethods: the 2nd static argument must be the name list");
                let components: Vec<Component> = arguments
                    .map(|&i| {
                        let getter = class.method_handle(i).unwrap_or_else(|| {
                            panic!("ObjectMethods: constant pool #{i} is not a MethodHandle")
                        });
                        // A component getter always reads a field; anything else means we
                        // misread the argument list rather than hit an unsupported shape.
                        assert_eq!(
                            getter.kind,
                            MethodHandleKind::GetField,
                            "ObjectMethods: a component getter must be a getField handle"
                        );
                        Component {
                            name: getter.name.to_string(),
                            descriptor: getter.descriptor.to_string(),
                        }
                    })
                    .collect();
                // The name list and the getters describe the same components from two
                // directions; if they disagree we parsed the argument list wrong.
                let listed = if names.is_empty() { 0 } else { names.split(';').count() };
                assert_eq!(
                    listed,
                    components.len(),
                    "ObjectMethods: the name list '{names}' and the getters disagree"
                );
                Bootstrap::ObjectMethods {
                    method: site_name.to_string(),
                    record_class,
                    components,
                }
            }
            (LAMBDA_METAFACTORY, "metafactory") => {
                // Static arguments: the interface method's erased type, the handle to the
                // implementation, and the instantiated type. Only the middle one matters
                // to us — the two MethodTypes exist for the generic bridging a real
                // metafactory performs when spinning a class.
                let implementation = bootstrap
                    .arguments
                    .get(1)
                    .and_then(|&i| class.method_handle(i))
                    .expect("metafactory: the 2nd static argument must be the implementation");
                assert!(
                    !implementation.kind.names_a_field(),
                    "metafactory: the implementation handle must name a method, not a field"
                );
                Bootstrap::Lambda {
                    implementation_class: implementation.class.to_string(),
                    implementation_name: implementation.name.to_string(),
                    implementation_descriptor: implementation.descriptor.to_string(),
                    // The call site's *return* type is the functional interface; its
                    // parameters are exactly what the lambda captured.
                    interface: return_class(descriptor).unwrap_or_else(|| {
                        panic!("metafactory: call site '{descriptor}' must return an interface")
                    }),
                }
            }
            (class, name) => panic!(
                "invokedynamic: no intrinsic for the bootstrap method {class}.{name} \
                 (StringConcatFactory, SwitchBootstraps.typeSwitch, \
                 ObjectMethods.bootstrap and LambdaMetafactory.metafactory are modelled \
                 — see docs/invokedynamic-ruta.md)"
            ),
        };

        CallSite { descriptor: descriptor.to_string(), bootstrap }
    };

    let params = param_descriptors(&site.descriptor);
    // The arguments were pushed left to right, so popping yields them backwards.
    let mut args: Vec<Value> = (0..params.len()).map(|_| self.top().pop()).collect();
    args.reverse();

    match &site.bootstrap {
        Bootstrap::StringConcat { recipe, constants } => {
            let text = match recipe {
                Some(recipe) => concat_with_recipe(&self.heap, recipe, &args, &params, constants),
                // No recipe: the arguments, in order, and nothing else.
                None => args
                    .iter()
                    .zip(&params)
                    .map(|(value, descriptor)| render(&self.heap, value, descriptor))
                    .collect(),
            };
            let offset = strings::intern(&mut self.metaspace, &mut self.heap, &text);
            self.top().push(Value::Reference(offset));
        }
        Bootstrap::TypeSwitch { labels } => {
            // Dynamic labels are resolved *now*, outside the class-file borrow — this is
            // where an enum pattern's condy tree actually runs.
            let resolved: Vec<ResolvedLabel> = labels
                .iter()
                .map(|label| match label {
                    Label::Type(name) => ResolvedLabel::Type(name.clone()),
                    Label::Dynamic(index) => {
                        ResolvedLabel::Constant(self.static_argument(&caller, *index))
                    }
                })
                .collect();
            let selected = self.type_switch(&resolved, &args);
            self.top().push(Value::Int(selected));
        }
        Bootstrap::Lambda {
            implementation_class,
            implementation_name,
            implementation_descriptor,
            interface,
        } => {
            let implementation = self
                .metaspace
                .resolve_method(implementation_class, implementation_name, implementation_descriptor)
                .unwrap_or_else(|| {
                    panic!(
                        "metafactory: cannot resolve the implementation \
                         {implementation_class}.{implementation_name}{implementation_descriptor}"
                    )
                });

            // One synthetic class per **call site** — stable, so a lambda created in a
            // loop doesn't mint a class per iteration. The captured values live in each
            // object, which is what keeps two closures over different values apart.
            let synthetic = format!("{caller}$${interface}$${cp_index}");
            self.lambdas.entry(synthetic.clone()).or_insert_with(|| LambdaShape {
                implementation,
                captures: params.clone(),
            });

            let offset =
                allocate_lambda(&mut self.metaspace, &mut self.heap, &synthetic, &args, &params);
            self.top().push(Value::Reference(offset));
        }
        Bootstrap::ObjectMethods { method, record_class, components } => {
            match method.as_str() {
                "toString" => {
                    let text = record_to_string(self, record_class, components, &args);
                    let offset = strings::intern(&mut self.metaspace, &mut self.heap, &text);
                    self.top().push(Value::Reference(offset));
                }
                "hashCode" => {
                    let hash = record_hash_code(self, record_class, components, &args);
                    self.top().push(Value::Int(hash));
                }
                "equals" => {
                    let equal = record_equals(self, record_class, components, &args);
                    self.top().push(Value::Int(equal as i32));
                }
                other => panic!(
                    "ObjectMethods: unknown call site name '{other}' \
                     (only equals/hashCode/toString exist)"
                ),
            }
        }
    }
}
/// Resolves one **static bootstrap argument** to a runtime value.
///
/// This is where the constant pool stops being flat: an argument can be an ordinary
/// constant, or it can be another *dynamic* constant whose value has to be computed —
/// possibly by running Java. An enum `switch` builds exactly that tree, one condy per
/// case label, each one referring to a shared condy describing the enum's class.
pub(super) fn static_argument(&mut self, owner: &str, index: u16) -> Value {
    // Read the constant's shape, then drop the class-file borrow: resolving it may need
    // the metaspace mutably (interning a String, loading a class, running a method).
    enum Shape {
        Text(String),
        Class(String),
        Int(i32),
        Long(i64),
        Float(f32),
        Double(f64),
        Dynamic,
    }
    let shape = {
        let class = self.metaspace.get(owner).expect("static argument: owner not loaded");
        if let Some(text) = class.string_constant(index) {
            Shape::Text(text.to_string())
        } else if let Some(value) = class.integer_constant(index) {
            Shape::Int(value)
        } else if let Some(value) = class.long_constant(index) {
            Shape::Long(value)
        } else if let Some(value) = class.float_constant(index) {
            Shape::Float(value)
        } else if let Some(value) = class.double_constant(index) {
            Shape::Double(value)
        } else if class.dynamic_constant(index).is_some() {
            Shape::Dynamic
        } else if let Some(name) = class.class_name(index) {
            Shape::Class(name.to_string())
        } else {
            panic!("static argument: constant pool #{index} of {owner} is not a value")
        }
    };

    match shape {
        Shape::Text(text) => {
            Value::Reference(strings::intern(&mut self.metaspace, &mut self.heap, &text))
        }
        Shape::Class(name) => {
            class_operations::load_class(&mut self.metaspace, &mut self.heap, &name);
            let mirror = self.metaspace.class_mirror(&name).unwrap_or_else(|| {
                panic!("static argument: no Class mirror for '{name}'")
            });
            Value::Reference(mirror)
        }
        Shape::Int(value) => Value::Int(value),
        Shape::Long(value) => Value::Long(value),
        Shape::Float(value) => Value::Float(value),
        Shape::Double(value) => Value::Double(value),
        Shape::Dynamic => self.dynamic_constant(owner, index),
    }
}

/// Computes a **dynamic constant** and remembers it.
///
/// A condy names a bootstrap method that produces a value on first use. The only one
/// `javac` emits is `ConstantBootstraps.invoke`, which means *call this `MethodHandle`
/// with these arguments* — so resolving one is: resolve the target handle, resolve its
/// arguments (recursively, since they may be condys themselves), and run it with
/// [`JVM::call_java`]. That last step is what was impossible before the VM could invoke
/// Java from inside an intrinsic.
fn dynamic_constant(&mut self, owner: &str, cp_index: u16) -> Value {
    let key = (owner.to_string(), cp_index);
    if let Some(&cached) = self.condy.get(&key) {
        return cached;
    }
    // A condy's arguments can be condys, so resolution walks a graph. A constant that
    // reaches itself would recurse until the Rust stack died; stop it with a diagnosis.
    assert!(
        self.condy_in_progress.insert(key.clone()),
        "condy: constant pool #{cp_index} of {owner} depends on itself"
    );

    // Lift the whole shape out of the pool before resolving anything.
    let (target_class, target_name, target_descriptor, argument_indices) = {
        let class = self.metaspace.get(owner).expect("condy: owner not loaded");
        let (bsm_index, _, declared) = class
            .dynamic_constant(cp_index)
            .expect("condy: cp_index is not a Dynamic constant");
        let bootstraps = class.bootstrap_methods();
        let bootstrap = bootstraps
            .get(bsm_index as usize)
            .expect("condy: bootstrap method index out of range");

        let factory = class
            .method_handle(bootstrap.method_ref)
            .expect("condy: the bootstrap handle does not resolve");
        assert_eq!(
            (factory.class, factory.name),
            (CONSTANT_BOOTSTRAPS, "invoke"),
            "condy: only ConstantBootstraps.invoke is modelled, got {}.{}",
            factory.class,
            factory.name
        );

        // Argument 0 is the handle to call; the rest are what to call it with.
        let target = bootstrap
            .arguments
            .first()
            .and_then(|&i| class.method_handle(i))
            .expect("condy: the 1st static argument must be the target MethodHandle");

        // The kind is not decoration. A `REF_newInvokeSpecial` handle *constructs*, an
        // `REF_invokeVirtual` one takes a receiver as its first argument — calling either
        // as if it were static would shift every argument by one and answer nonsense.
        // Only the static form is modelled, so anything else stops here rather than
        // quietly producing the wrong constant.
        assert_eq!(
            target.kind,
            MethodHandleKind::InvokeStatic,
            "condy: the target handle is {:?}; only REF_invokeStatic is modelled \
             (see docs/invokedynamic-ruta.md)",
            target.kind
        );

        // The constant's own descriptor says what it is *supposed* to be. The real
        // `ConstantBootstraps.invoke` adapts the handle's result to that type; we only
        // check they already agree, which is what `javac` emits.
        let returns = return_descriptor(target.descriptor);
        assert_eq!(
            returns, declared,
            "condy: the target returns '{returns}' but the constant is declared \
             '{declared}'"
        );

        (
            target.class.to_string(),
            target.name.to_string(),
            target.descriptor.to_string(),
            bootstrap.arguments[1..].to_vec(),
        )
    };

    // Resolving an argument can recurse right back here for a nested condy.
    let args: Vec<Value> =
        argument_indices.iter().map(|&i| self.static_argument(owner, i)).collect();
    let widths = MetaspaceService::param_slot_widths(&target_descriptor);

    let target = self
        .metaspace
        .resolve_method(&target_class, &target_name, &target_descriptor)
        .unwrap_or_else(|| {
            panic!("condy: cannot resolve {target_class}.{target_name}{target_descriptor}")
        });
    let value = self
        .call_java(target, args, &widths)
        .expect("condy: the bootstrap target must return a value");

    self.condy_in_progress.remove(&key);
    self.condy.insert(key, value);
    value
}

/// `SwitchBootstraps.typeSwitch` — the case selector behind a pattern `switch`.
///
/// The call site has shape `(Object, int) -> int`: it takes the value being switched on
/// and a **restart index**, and answers *which case to run* as an index that `javac`
/// feeds straight into a `tableswitch`. Three outcomes:
///
/// - the selector is `null` → **-1** (a `case null` arm, or an error if there is none);
/// - the first label at or after `restart` that the selector matches → **its index**;
/// - nothing matches → **`labels.len()`**, which lands on `default`.
///
/// The restart index exists for guarded patterns (`case Foo f when …`): a guard that
/// fails re-enters the same call site asking for the *next* match rather than starting
/// over, which is why matching cannot simply scan from zero.
fn type_switch(&mut self, labels: &[ResolvedLabel], args: &[Value]) -> i32 {
    let selector = match args.first() {
        Some(Value::Reference(offset)) => *offset,
        other => panic!("typeSwitch: expected a reference selector, found {other:?}"),
    };
    let restart = match args.get(1) {
        Some(Value::Int(n)) => (*n).max(0) as usize,
        other => panic!("typeSwitch: expected an int restart index, found {other:?}"),
    };

    // Null never matches a label; it is its own outcome.
    if selector == 0 {
        return -1;
    }

    for (offset, label) in labels.iter().enumerate().skip(restart) {
        let matched = match label {
            ResolvedLabel::Type(name) => {
                let runtime = self.runtime_class_of(selector);
                class_operations::is_subtype(&mut self.metaspace, &runtime, name)
            }
            ResolvedLabel::Constant(Value::Reference(descriptor)) => {
                self.matches_enum_constant(*descriptor, selector)
            }
            ResolvedLabel::Constant(other) => {
                panic!("typeSwitch: a case label resolved to {other:?}, not an object")
            }
        };
        if matched {
            return offset as i32;
        }
    }
    labels.len() as i32
}

/// Whether `selector` *is* the enum constant an `Enum$EnumDesc` names.
///
/// The descriptor is nominal — it holds a class name and a constant name, never a
/// reference to the constant itself. Matching therefore compares names on both sides:
/// the selector's runtime class against the descriptor's, and the selector's own `name`
/// (the field `java.lang.Enum` stores) against the descriptor's constant name. No
/// `getstatic` is needed, because an enum constant already knows what it is called.
fn matches_enum_constant(&mut self, descriptor: usize, selector: usize) -> bool {
    let class_desc = self.reference_field(descriptor, "constantType");
    let wanted_class = {
        let name = self.reference_field(class_desc, "name");
        // A ClassDesc carries the binary name with dots; runtime classes use slashes.
        strings::read(&self.heap, name).replace('.', "/")
    };
    if self.runtime_class_of(selector) != wanted_class {
        return false;
    }
    let wanted_constant = {
        let name = self.reference_field(descriptor, "constantName");
        strings::read(&self.heap, name)
    };
    let actual = self.reference_field(selector, "name");
    strings::read(&self.heap, actual) == wanted_constant
}

/// The name of an object's runtime class, read from the `class_id` in its header.
fn runtime_class_of(&mut self, object: usize) -> String {
    let mirror = self.heap.read_u32(object) as usize;
    self.metaspace
        .class_name_at_mirror(mirror)
        .expect("the object's header does not point at a known class")
        .to_string()
}

/// The text of an object — our `String.valueOf(Object)`.
///
/// `null` is the string `"null"`; a `String` is itself; anything else is whatever its own
/// `toString()` returns, which is a **virtual call back into user code**. Reading an
/// arbitrary object's bytes as if it were a String — which is what this used to do — gave
/// garbage for anything that wasn't one.
pub(super) fn text_of(&mut self, object: usize) -> String {
    if object == 0 {
        return "null".to_string();
    }
    let class = self.runtime_class_of(object);
    if class == "java/lang/String" {
        return strings::read(&self.heap, object);
    }
    match self.call_virtual(object, "toString", "()Ljava/lang/String;", Vec::new()) {
        Some(Value::Reference(0)) => "null".to_string(),
        Some(Value::Reference(text)) => strings::read(&self.heap, text),
        // Our `java.lang.Object` declares no `toString`, so a class that defines none has
        // no text to give. Java would answer `Class@hash`; inventing that here would be
        // guessing at a format nothing has asked us to match yet.
        None => panic!(
            "toString: class '{class}' defines no toString() and java.lang.Object \
             provides none (see docs/invokedynamic-ruta.md)"
        ),
        other => panic!("toString: expected a String, got {other:?}"),
    }
}

/// Reads a reference-typed field of an object by name, resolving the offset against the
/// object's **runtime** class so inherited fields (like `Enum.name`) are found too.
fn reference_field(&mut self, object: usize, field: &str) -> usize {
    let class = self.runtime_class_of(object);
    let at = object + objects_operations::field_offset(&mut self.metaspace, &class, field);
    self.heap.read_u32(at) as usize
}
} // impl JVM

/// Allocates the object a lambda call site produces: a header naming its synthetic class,
/// followed by the captured values laid out at their own widths.
///
/// The synthetic class gets a header-only mirror, the same shape `anewarray` gives array
/// classes — it exists to give the object an identity the dispatch can recognise, not to
/// hold statics.
///
/// A captured **reference** is only safe because the synthetic class registers its
/// reference-slot layout with the metaspace, which is where the collector looks. Without
/// that the capture would be invisible to the GC: never marked, and never rewritten when
/// the object it points at moves.
fn allocate_lambda(
    metaspace: &mut MetaspaceService,
    heap: &mut HeapService,
    synthetic: &str,
    captured: &[Value],
    descriptors: &[String],
) -> usize {
    let uuid = metaspace.class_id(synthetic).to_string();
    let mirror = match metaspace.class_object(&uuid) {
        Some(offset) => offset,
        None => {
            let offset = heap.malloc_old(objects_operations::HEADER_SIZE);
            metaspace.set_class_object(&uuid, offset);
            offset
        }
    };

    // The layout depends only on the descriptors, so it is the same for every instance
    // this call site ever produces — declare it once, alongside the mirror.
    let mut within = objects_operations::HEADER_SIZE;
    let mut reference_slots = Vec::new();
    for descriptor in descriptors {
        if matches!(descriptor.as_bytes().first(), Some(b'L' | b'[')) {
            reference_slots.push(within);
        }
        within += capture_width(descriptor);
    }
    metaspace.set_synthetic_reference_slots(synthetic, reference_slots);

    let offset = heap.malloc(within);
    heap.write_u32(offset, mirror as u32);

    let mut at = offset + objects_operations::HEADER_SIZE;
    for (value, descriptor) in captured.iter().zip(descriptors) {
        match value {
            Value::Int(n) => heap.write_u32(at, *n as u32),
            Value::Float(f) => heap.write_u32(at, f.to_bits()),
            Value::Long(n) => heap.write_u64(at, *n as u64),
            Value::Double(d) => heap.write_u64(at, d.to_bits()),
            // Through the barrier gateway, like every other reference store: a lambda
            // living in Old that captures a young object is exactly the `old→young`
            // pointer the remembered set exists to catch.
            Value::Reference(target) => heap.store_reference(offset, at, *target),
        }
        at += capture_width(descriptor);
    }
    offset
}

/// The heap width of a captured value: category-2 primitives take 8 bytes, the rest 4.
fn capture_width(descriptor: &str) -> usize {
    match descriptor.as_bytes().first() {
        Some(b'J' | b'D') => 8,
        _ => 4,
    }
}

/// A record's `toString`: `Name[a=1, b=2]`, using the class's **simple** name — the part
/// after the last `/` (package) and `$` (nesting).
fn record_to_string(
    jvm: &mut JVM,
    record_class: &str,
    components: &[Component],
    args: &[Value],
) -> String {
    let object = receiver(args, "toString");
    let simple = record_class.rsplit(['/', '$']).next().unwrap_or(record_class);
    let body: Vec<String> = components
        .iter()
        .map(|component| {
            let value = read_component(&mut jvm.metaspace, &jvm.heap, record_class, component, object);
            let text = match value {
                // A reference component is whatever *its own* `toString` says — the one
                // place a record's text depends on user code.
                Value::Reference(target) => jvm.text_of(target),
                other => render(&jvm.heap, &other, &component.descriptor),
            };
            format!("{}={}", component.name, text)
        })
        .collect();
    format!("{simple}[{}]", body.join(", "))
}

/// A record's `hashCode`: `31 * accumulator + hash(component)` over the components in
/// declaration order, starting from zero — so `P(1, 2)` hashes to `1 * 31 + 2 = 33`.
fn record_hash_code(
    jvm: &mut JVM,
    record_class: &str,
    components: &[Component],
    args: &[Value],
) -> i32 {
    let object = receiver(args, "hashCode");
    let mut accumulator = 0i32;
    for component in components {
        let value = read_component(&mut jvm.metaspace, &jvm.heap, record_class, component, object);
        let hash = match value {
            // `Objects.hashCode`: null is 0, anything else answers for itself.
            Value::Reference(0) => 0,
            Value::Reference(target) => match jvm.call_virtual(target, "hashCode", "()I", Vec::new())
            {
                Some(Value::Int(hash)) => hash,
                other => panic!("record hashCode: a component's hashCode gave {other:?}"),
            },
            other => component_hash(&component.descriptor, &other),
        };
        accumulator = accumulator.wrapping_mul(31).wrapping_add(hash);
    }
    accumulator
}

/// A record's `equals`: the other object must be **the same record class** and every
/// component must match. Anything else — a different class, or `null` — is `false`
/// rather than an error.
fn record_equals(
    jvm: &mut JVM,
    record_class: &str,
    components: &[Component],
    args: &[Value],
) -> bool {
    let object = receiver(args, "equals");
    let other = match args.get(1) {
        Some(Value::Reference(offset)) => *offset,
        other => panic!("record equals: expected a reference argument, found {other:?}"),
    };
    if other == 0 {
        return false; // null is never equal to a record
    }
    // A record only equals another instance of the very same class — no subclassing to
    // worry about, since records are implicitly final.
    let other_class = jvm.metaspace.class_name_at_mirror(jvm.heap.read_u32(other) as usize);
    if other_class != Some(record_class) {
        return false;
    }
    components.iter().all(|component| {
        let mine = read_component(&mut jvm.metaspace, &jvm.heap, record_class, component, object);
        let theirs = read_component(&mut jvm.metaspace, &jvm.heap, record_class, component, other);
        match (mine, theirs) {
            // `Objects.equals`: identical references are equal without asking, a lone
            // null never is, and otherwise the component decides for itself. Comparing
            // references directly — which is what this did before — is `==`, not
            // `equals`, and answers *false* for two equal Strings built separately.
            (Value::Reference(a), Value::Reference(b)) if a == b => true,
            (Value::Reference(0), _) | (_, Value::Reference(0)) => false,
            (Value::Reference(a), Value::Reference(b)) => {
                match jvm.call_virtual(a, "equals", "(Ljava/lang/Object;)Z", vec![Value::Reference(b)])
                {
                    Some(Value::Int(result)) => result != 0,
                    // No `equals` in the hierarchy means the component inherits identity
                    // comparison — and identity already failed above.
                    None => false,
                    other => panic!("record equals: a component's equals gave {other:?}"),
                }
            }
            (mine, theirs) => mine == theirs,
        }
    })
}

/// The receiver of a record method — always the first call site argument.
fn receiver(args: &[Value], method: &str) -> usize {
    match args.first() {
        Some(Value::Reference(offset)) if *offset != 0 => *offset,
        other => panic!("record {method}: expected a non-null receiver, found {other:?}"),
    }
}

/// Reads one component out of a record instance, at the width its descriptor implies.
fn read_component(
    metaspace: &mut MetaspaceService,
    heap: &HeapService,
    record_class: &str,
    component: &Component,
    object: usize,
) -> Value {
    let at = object + objects_operations::field_offset(metaspace, record_class, &component.name);
    match component.descriptor.as_bytes().first() {
        Some(b'J') => Value::Long(heap.read_u64(at) as i64),
        Some(b'D') => Value::Double(f64::from_bits(heap.read_u64(at))),
        Some(b'F') => Value::Float(f32::from_bits(heap.read_u32(at))),
        Some(b'L' | b'[') => Value::Reference(heap.read_u32(at) as usize),
        _ => Value::Int(heap.read_u32(at) as i32),
    }
}

/// One component's contribution to a record's hash, following the wrapper types'
/// `hashCode` — which is **not** simply the value: `Boolean` hashes to 1231/1237, and a
/// `long` folds its two halves together.
fn component_hash(descriptor: &str, value: &Value) -> i32 {
    match (descriptor.as_bytes().first(), value) {
        (Some(b'Z'), Value::Int(n)) => {
            if *n != 0 {
                1231
            } else {
                1237
            }
        }
        (_, Value::Int(n)) => *n,
        (_, Value::Long(n)) => (*n ^ (*n as u64 >> 32) as i64) as i32,
        (_, Value::Float(f)) => f.to_bits() as i32,
        (_, Value::Double(d)) => {
            let bits = d.to_bits();
            (bits ^ (bits >> 32)) as i32
        }
        (_, Value::Reference(0)) => 0, // Objects.hashCode(null) == 0
        (_, Value::Reference(_)) => panic!(
            "record hashCode: a reference component needs the object's own hashCode(), \
             which requires calling Java from a native (see docs/invokedynamic-ruta.md)"
        ),
    }
}

/// A case label of a pattern `switch`, as it sits in the constant pool. A **type**
/// pattern names a class outright; an **enum** pattern names a dynamic constant that has
/// to be computed before it can be compared to anything.
enum Label {
    Type(String),
    Dynamic(u16),
}

/// A case label with its dynamic constants already computed.
enum ResolvedLabel {
    Type(String),
    /// An `Enum$EnumDesc` — which enum type, and which constant of it.
    Constant(Value),
}

/// Everything an indy call site needs, lifted out of the constant pool.
struct CallSite {
    /// The call site's shape: what it pops and what it pushes.
    descriptor: String,
    /// Which bootstrap produced it, with its static arguments already interpreted.
    bootstrap: Bootstrap,
}

/// A resolved bootstrap method. The same raw static arguments mean different things per
/// factory, so they are decoded once — while the constant pool is still borrowed — and
/// carried here as owned data.
enum Bootstrap {
    /// `StringConcatFactory`: an optional recipe plus the constants it splices.
    StringConcat { recipe: Option<String>, constants: Vec<String> },
    /// `SwitchBootstraps.typeSwitch`: the case labels, in source order.
    TypeSwitch { labels: Vec<Label> },
    /// `ObjectMethods.bootstrap`: one entry serves a record's three methods, so the call
    /// site's `method` name is what selects the behaviour.
    ObjectMethods { method: String, record_class: String, components: Vec<Component> },
    /// `LambdaMetafactory.metafactory`: the method the lambda body compiled to, and the
    /// functional interface the produced object must satisfy.
    Lambda {
        implementation_class: String,
        implementation_name: String,
        implementation_descriptor: String,
        interface: String,
    },
}

/// The class named by a descriptor's **return** type — `()LOp;` gives `Op`. A lambda
/// call site returns the functional interface it produces.
fn return_class(descriptor: &str) -> Option<String> {
    descriptor
        .rsplit(')')
        .next()?
        .strip_prefix('L')?
        .strip_suffix(';')
        .map(str::to_string)
}

/// A method descriptor's **return** part, verbatim — `(Ljava/lang/String;)LFoo;` gives
/// `LFoo;`. A dynamic constant declares its own type as a *field* descriptor, which is
/// exactly this shape, so the two can be compared directly.
fn return_descriptor(descriptor: &str) -> &str {
    descriptor.rsplit(')').next().unwrap_or("")
}

/// One component of a record, as named by its `REF_getField` getter handle.
struct Component {
    name: String,
    descriptor: String,
}

/// Walks a `makeConcatWithConstants` recipe, splicing dynamic arguments at U+0001 and
/// constants at U+0002, and copying every other character verbatim.
fn concat_with_recipe(
    heap: &HeapService,
    recipe: &str,
    args: &[Value],
    params: &[String],
    constants: &[String],
) -> String {
    let mut out = String::new();
    let mut next_arg = 0;
    let mut next_const = 0;
    for character in recipe.chars() {
        match character {
            TAG_ARG => {
                out.push_str(&render(heap, &args[next_arg], &params[next_arg]));
                next_arg += 1;
            }
            TAG_CONST => {
                out.push_str(&constants[next_const]);
                next_const += 1;
            }
            literal => out.push(literal),
        }
    }
    out
}

/// Renders one concatenation argument — our stand-in for `String.valueOf`.
///
/// The **descriptor decides**, not the `Value`: `boolean` and `char` both travel as
/// `Value::Int`, so without the declared type a `char` would print as its numeric code
/// and a `boolean` as `0`/`1`.
///
/// Floating point goes through [`float_to_decimal`], the same Java-faithful formatter
/// `javap` uses — **not** Rust's `Display`, which lays the digits out differently
/// (`1.0` → `1`, `1e7` → `10000000`). Reusing it is what keeps a concatenated `double`
/// printing identically under our VM and under `java`; rolling our own here would have
/// silently broken that, since Rust's shortest form also differs from Java's in the
/// last-digit tie-break and in the denormals.
///
/// Object arguments beyond `String` would need a real `toString()` call (a virtual
/// dispatch back into the interpreter); for now a non-null reference is read as a
/// `String`. `javac` never sends anything else — it inserts `String.valueOf(Object)`
/// *before* the call site — but another compiler could, ours included.
fn render(heap: &HeapService, value: &Value, descriptor: &str) -> String {
    match (descriptor.as_bytes().first(), value) {
        (Some(b'Z'), Value::Int(n)) => if *n != 0 { "true" } else { "false" }.to_string(),
        (Some(b'C'), Value::Int(n)) => {
            char::from_u32(*n as u32).map(String::from).unwrap_or_default()
        }
        (_, Value::Int(n)) => n.to_string(),
        (_, Value::Long(n)) => n.to_string(),
        (_, Value::Float(f)) => float_to_decimal::java_float(*f),
        (_, Value::Double(d)) => float_to_decimal::java_double(*d),
        (_, Value::Reference(0)) => "null".to_string(),
        (_, Value::Reference(offset)) => strings::read(heap, *offset),
    }
}

/// Splits a method descriptor's parameter list into raw descriptors —
/// `(ILjava/lang/String;)V` becomes `["I", "Ljava/lang/String;"]`. One entry per
/// *argument*, so it doubles as the count of values to pop.
fn param_descriptors(descriptor: &str) -> Vec<String> {
    let bytes = descriptor.as_bytes();
    let mut params = Vec::new();
    let mut i = 1; // skip the opening `(`
    while i < bytes.len() && bytes[i] != b')' {
        let start = i;
        while i < bytes.len() && bytes[i] == b'[' {
            i += 1; // array dimensions bind to the type that follows
        }
        if i < bytes.len() && bytes[i] == b'L' {
            while i < bytes.len() && bytes[i] != b';' {
                i += 1; // a class name runs to its terminating `;`
            }
        }
        i += 1;
        params.push(descriptor[start..i.min(descriptor.len())].to_string());
    }
    params
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn param_descriptors_splits_every_shape() {
        assert_eq!(param_descriptors("()V"), Vec::<String>::new());
        assert_eq!(param_descriptors("(I)Ljava/lang/String;"), ["I"]);
        assert_eq!(
            param_descriptors("(ILjava/lang/String;J)Ljava/lang/String;"),
            ["I", "Ljava/lang/String;", "J"]
        );
        // Array dimensions bind to the following type, objects included.
        assert_eq!(
            param_descriptors("([I[[Ljava/lang/String;C)V"),
            ["[I", "[[Ljava/lang/String;", "C"]
        );
    }

    /// A condy declares its own type as a field descriptor, and the handle that produces
    /// it declares a method descriptor; checking one against the other is what catches a
    /// bootstrap wired to the wrong target. The two shapes have to line up exactly.
    #[test]
    fn return_descriptor_matches_a_constants_declared_type() {
        // Exactly the pair an enum switch emits.
        assert_eq!(
            return_descriptor("(Ljava/lang/constant/ClassDesc;Ljava/lang/String;)Ljava/lang/Enum$EnumDesc;"),
            "Ljava/lang/Enum$EnumDesc;"
        );
        assert_eq!(
            return_descriptor("(Ljava/lang/String;)Ljava/lang/constant/ClassDesc;"),
            "Ljava/lang/constant/ClassDesc;"
        );
        // Primitives and void are single characters, not `L…;`.
        assert_eq!(return_descriptor("(I)I"), "I");
        assert_eq!(return_descriptor("()V"), "V");
        // A `)` inside a class name can't occur, but an argument-less form still parses.
        assert_eq!(return_descriptor("()[Ljava/lang/Object;"), "[Ljava/lang/Object;");
    }

    #[test]
    fn render_uses_the_descriptor_not_the_value() {
        // `boolean` and `char` both travel as `Value::Int`; only the declared type
        // tells `true` from `1`, or `A` from `65`.
        let heap = HeapService::new();
        assert_eq!(render(&heap, &Value::Int(1), "Z"), "true");
        assert_eq!(render(&heap, &Value::Int(0), "Z"), "false");
        assert_eq!(render(&heap, &Value::Int(65), "C"), "A");
        assert_eq!(render(&heap, &Value::Int(65), "I"), "65");
        assert_eq!(render(&heap, &Value::Reference(0), "Ljava/lang/String;"), "null");
    }
}
