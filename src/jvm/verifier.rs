//! The bytecode **verifier** (JVMS §4.10) — type-checks a method's `Code` *before*
//! it runs, so the interpreter can execute without runtime type checks (and so
//! malformed/malicious bytecode can't corrupt the VM).
//!
//! We support **both** schemes (JVMS §4.10). The modern, **StackMapTable-based** one
//! (Java 6+): the compiler-emitted stack-map frames give the expected type state at
//! every branch target, so verification is a single linear pass that *checks*
//! consistency at those points. And the legacy **inference** one, used when no
//! StackMapTable is present: a work-list data-flow that *infers* the type state at
//! each point by merging (joining) all the paths that reach it, to a fixpoint. Both
//! share one per-opcode transition function ([`transfer`]).
//!
//! The verifier works over **verification types** ([`VType`]) — abstract stand-ins
//! for what's on the operand stack and in the locals — instead of concrete values.
//! This module is the foundation: the type lattice and the per-point [`TypeState`].
//! Assignability, StackMapTable decoding and the instruction pass come on top.

use std::collections::{HashMap, HashSet};

use crate::jvm::class_file::ClassFile;
use crate::jvm::interpreter::bytecode_interpreter::class_operations;
use crate::jvm::interpreter::metaspace::MetaspaceService;
use crate::jvm::opcode::disassemble;
use crate::jvm::parser::constant_pool::ConstantPoolEntry;
use crate::jvm::parser::code::Code;
use crate::jvm::parser::member::MemberInfo;
use crate::jvm::parser::stack_map_table::{self, FrameDelta, StackMapTable, VerificationTypeInfo};

/// A **verification type** (JVMS §4.10.1.2): what the verifier tracks on the operand
/// stack and in each local slot, in place of a value. Simplified to what our
/// interpreter models — `int`-category and references — plus the bookkeeping types.
#[derive(Clone, Debug, PartialEq, Eq)]
pub enum VType {
    /// `⊤` — an unusable slot: an uninitialised local, or the high half of a
    /// `long`/`double`. Assignable from nothing useful; the top of the lattice.
    Top,
    /// The `int` category — covers `boolean`, `byte`, `char`, `short` and `int`,
    /// which all verify as `int`.
    Int,
    /// The `long` type — a **category-2** value (logically two slots; its high half
    /// shows up as `Top` in the slot after it).
    Long,
    /// The `double` type — category-2, like `long`.
    Double,
    /// The `float` type — **category-1** (one slot), like `int` but its own type.
    Float,
    /// The `null` reference — assignable to *any* reference type.
    Null,
    /// A reference of a known class/interface, by binary name (e.g. "Dog").
    Reference(String),
    /// `this` inside a constructor *before* the `super`/`this` `<init>` has run —
    /// can't be used as a normal reference until initialised.
    UninitializedThis,
    /// An object freshly made by a `new` at this bytecode offset, *before* its
    /// `<init>` runs. Tracked by the `new`'s pc so `invokespecial <init>` can find it.
    Uninitialized(usize),
}

impl VType {
    /// Whether a value of `self`'s type can be used where a `to` is expected — the
    /// lattice's **assignability** (= subtyping). References defer to `is_subtype`,
    /// which walks the class hierarchy (hence the `metaspace`).
    ///
    /// Rules: anything fits `Top` (an unconstrained slot); identical types match;
    /// `Null` fits any reference; `Reference(a)` fits `Reference(b)` iff `a` is a
    /// subtype of `b`. Everything else (e.g. `Int` vs a reference, or the
    /// uninitialised kinds vs a normal reference) is *not* assignable.
    pub fn is_assignable_to(&self, to: &VType, metaspace: &mut MetaspaceService) -> bool {
        use VType::*;
        match (self, to) {
            (_, Top) => true,
            (a, b) if a == b => true,
            (Null, Reference(_)) => true,
            (Reference(from), Reference(target)) => {
                class_operations::is_subtype(metaspace, from, target)
            }
            _ => false,
        }
    }

    /// The **join** (least upper bound) of two verification types — the lattice
    /// operation that merges two control-flow paths. Our verifier doesn't *call* it
    /// (the StackMapTable carries the compiler's already-computed merges, which we
    /// only check), but it completes the lattice and is what a *type-inference*
    /// verifier would use:
    ///  - identical types join to themselves;
    ///  - `Null` is the bottom of the references → `join(Null, R) = R`;
    ///  - two references join to their first **common superclass** (the JVM ignores
    ///    interfaces here, deferring those checks to run time → falls back to
    ///    `Object` with array covariance handled by `is_subtype`);
    ///  - anything else (e.g. `Int` vs a reference) has no common type → `Top` (⊤).
    pub fn join(&self, other: &VType, metaspace: &mut MetaspaceService) -> VType {
        use VType::*;
        match (self, other) {
            (a, b) if a == b => a.clone(),
            (Null, r @ Reference(_)) | (r @ Reference(_), Null) => r.clone(),
            (Reference(a), Reference(b)) => Reference(common_supertype(metaspace, a, b)),
            _ => Top,
        }
    }
}

/// The first common supertype of two reference types: walk `a`'s superclass chain
/// (with array covariance via `is_subtype`) until one is a supertype of `b`; fall
/// back to `Object` if none is closer.
fn common_supertype(metaspace: &mut MetaspaceService, a: &str, b: &str) -> String {
    let mut current = Some(a.to_string());
    while let Some(class) = current {
        if class_operations::is_subtype(metaspace, b, &class) {
            return class;
        }
        current = superclass_of(metaspace, &class);
    }
    "java/lang/Object".to_string()
}

/// The direct superclass binary name of `class` (arrays → `Object`; `Object` or an
/// unloadable class → `None`, ending the walk).
fn superclass_of(metaspace: &mut MetaspaceService, class: &str) -> Option<String> {
    if class.starts_with('[') {
        return Some("java/lang/Object".to_string());
    }
    metaspace.get_or_load(class).and_then(|cf| cf.class_name(cf.super_class).map(str::to_string))
}

/// The verifier's abstract state at one program point: the types on the operand
/// stack (bottom → top) and in the local-variable slots. The pass threads one of
/// these through the instructions; stack-map frames pin it down at branch targets.
#[derive(Clone, Debug)]
pub struct TypeState {
    /// Operand-stack slot types, bottom → top (mirrors the interpreter's stack).
    pub stack: Vec<VType>,
    /// Local-variable slot types, by index.
    pub locals: Vec<VType>,
}

impl TypeState {
    /// An empty stack with `max_locals` slots, all `Top` (uninitialised). The pass
    /// fills the leading locals with the method's parameter types before walking.
    pub fn new(max_locals: usize) -> Self {
        TypeState { stack: Vec::new(), locals: vec![VType::Top; max_locals] }
    }
}

/// Expands a method's delta-encoded [`StackMapTable`] into **absolute** type states,
/// keyed by bytecode offset — the verifier's oracle at every branch target.
///
/// The frames are deltas over a *running* state that begins at `initial_locals`
/// (the method's parameter types) with an empty stack. Each frame adjusts the
/// locals (same/append/chop/full) and sets the stack, and its absolute pc is
/// `offset_delta` for the first frame, then `previous_pc + offset_delta + 1`.
pub fn decode(
    table: &StackMapTable,
    initial_locals: Vec<VType>,
    class: &ClassFile,
) -> HashMap<usize, TypeState> {
    let mut states = HashMap::new();
    let mut locals = initial_locals;
    let mut pc: Option<usize> = None;

    for frame in &table.frames {
        let delta = frame.delta();
        let offset_delta = delta.offset_delta() as usize;

        // Evolve the running locals and compute this frame's operand stack. Locals
        // are **slot-indexed** (a `long`/`double` occupies two slots), so appending
        // or replacing them expands category-2 types with a `Top` high half. The
        // operand stack keeps one entry per value (matching our opcode transitions).
        let stack = match delta {
            FrameDelta::Same { .. } => Vec::new(),
            FrameDelta::SameLocals1 { stack, .. } => vec![to_vtype(stack, class)],
            FrameDelta::Chop { chopped, .. } => {
                for _ in 0..chopped {
                    chop_one_local(&mut locals);
                }
                Vec::new()
            }
            FrameDelta::Append { locals: appended, .. } => {
                for info in appended {
                    push_local(&mut locals, to_vtype(info, class));
                }
                Vec::new()
            }
            FrameDelta::Full { locals: full, stack, .. } => {
                locals = Vec::new();
                for info in full {
                    push_local(&mut locals, to_vtype(info, class));
                }
                stack.iter().map(|info| to_vtype(info, class)).collect()
            }
        };

        // First frame's pc is the raw delta; later frames add the previous pc + 1.
        let this_pc = match pc {
            None => offset_delta,
            Some(prev) => prev + offset_delta + 1,
        };
        states.insert(this_pc, TypeState { stack, locals: locals.clone() });
        pc = Some(this_pc);
    }

    states
}

/// Pushes a verification type onto the slot-indexed locals, adding a `Top` high-half
/// slot after a **category-2** value (`long`/`double`) so it occupies two slots.
fn push_local(locals: &mut Vec<VType>, vt: VType) {
    let category2 = matches!(vt, VType::Long | VType::Double);
    locals.push(vt);
    if category2 {
        locals.push(VType::Top);
    }
}

/// Removes the last *logical* local. A category-2 value occupies two slots — the
/// value plus its trailing `Top` high half — so those are dropped together.
fn chop_one_local(locals: &mut Vec<VType>) {
    let cat2_high_half = matches!(locals.last(), Some(VType::Top))
        && locals.len() >= 2
        && matches!(locals[locals.len() - 2], VType::Long | VType::Double);
    if cat2_high_half {
        locals.pop();
    }
    locals.pop();
}

/// Converts a parser `VerificationTypeInfo` into our [`VType`]. `Object` resolves
/// its class through `class`'s constant pool. `long`/`double`/`float` aren't in our
/// value model yet, so they collapse to `Top` (unusable) for now.
fn to_vtype(info: &VerificationTypeInfo, class: &ClassFile) -> VType {
    match info {
        VerificationTypeInfo::Top => VType::Top,
        VerificationTypeInfo::Integer => VType::Int,
        VerificationTypeInfo::Null => VType::Null,
        VerificationTypeInfo::UninitializedThis => VType::UninitializedThis,
        VerificationTypeInfo::Object { cpool_index } => {
            VType::Reference(class.class_name(*cpool_index).unwrap_or("?").to_string())
        }
        VerificationTypeInfo::Uninitialized { offset } => VType::Uninitialized(*offset as usize),
        VerificationTypeInfo::Long => VType::Long,
        VerificationTypeInfo::Double => VType::Double,
        VerificationTypeInfo::Float => VType::Float,
    }
}

/// A problem found while verifying a method. `unsupported` marks the cases the
/// verifier can't judge yet (an opcode it doesn't model) — distinct from a real
/// type-safety violation — so callers can warn-and-proceed instead of rejecting.
#[derive(Debug)]
pub struct VerifyError {
    pub method: String,
    pub pc: usize,
    pub message: String,
    pub unsupported: bool,
}

fn err(method: &str, pc: usize, message: impl Into<String>) -> VerifyError {
    VerifyError { method: method.to_string(), pc, message: message.into(), unsupported: false }
}

fn unsupported(method: &str, pc: usize, opcode: u8) -> VerifyError {
    VerifyError {
        method: method.to_string(),
        pc,
        message: format!("verifier: unsupported opcode 0x{opcode:02x}"),
        unsupported: true,
    }
}

/// Type-checks one method's `Code` (JVMS §4.10). Runs the structural well-formedness
/// gate ([`structural_check`], §4.9), builds the initial type state from the method's
/// signature, validates the exception table, then dispatches to the StackMapTable
/// checker ([`verify_with_stackmap`]) or — when no frames are present — the inference
/// fixpoint ([`verify_by_inference`]); both apply the same per-opcode [`transfer`].
/// With a StackMapTable it also **cross-checks** the compiler's frames against the
/// inference result ([`cross_check_stackmap`]). Returns the first violation found.
///
/// Covers the whole opcode set the interpreter executes: the int/control-flow core
/// plus objects (`new`/`dup`/`invokespecial` with uninitialised tracking, the
/// `invokevirtual`/`invokeinterface`/`invokestatic` calls, `getfield`/`putfield`,
/// `getstatic`/`putstatic`, `instanceof`/`checkcast`), arrays (`newarray`/
/// `anewarray`, the loads/stores, `arraylength`), the typed returns (`ireturn`…
/// `areturn`), `athrow`, `ldc`, and the reference branches. The method's exception
/// table is validated separately (see [`verify_exception_table`]). Any opcode still
/// outside this set is reported as unsupported.
pub fn verify_method(
    metaspace: &mut MetaspaceService,
    class: &ClassFile,
    member: &MemberInfo,
) -> Result<(), VerifyError> {
    verify_method_impl(metaspace, class, member, false)
}

/// The verification core (see [`verify_method`]). `force_inference` runs the legacy
/// inference fixpoint even when a StackMapTable is present — used by tests to exercise
/// that path over modern (framed) branching methods, since this toolchain won't emit
/// a frame-less branching class.
fn verify_method_impl(
    metaspace: &mut MetaspaceService,
    class: &ClassFile,
    member: &MemberInfo,
    force_inference: bool,
) -> Result<(), VerifyError> {
    let name = class.utf8(member.name_index).unwrap_or("?").to_string();
    let descriptor = class.utf8(member.descriptor_index).unwrap_or("?").to_string();
    // This method's own class — the type `UninitializedThis` becomes once its
    // `super`/`this` `<init>` runs.
    let this_class = class.class_name(class.this_class).unwrap_or("?").to_string();
    let Some(code) = class.member_code(member) else {
        return Ok(()); // abstract/native: no bytecode to verify
    };
    let max_locals = code.max_locals as usize;
    let max_stack = code.max_stack as usize;

    // Structural well-formedness (JVMS §4.9) — branch/handler targets, no fall-off,
    // no subroutines — *before* any type checking, so the data-flow can trust the CFG.
    structural_check(&code, &name)?;

    // Initial locals: `this` (for instance methods) then the parameters; the rest
    // start as `Top`. This is also the base the stack-map deltas build on.
    //
    // In a **constructor**, `this` starts as `UninitializedThis` until the body's
    // `super(…)`/`this(…)` call runs — so the verifier can forbid using a half-built
    // object (JVMS §4.10.2.4). The sole exception is `Object.<init>` itself: as the
    // root it has no superclass to chain to, so its `this` is initialized on entry.
    let is_constructor = name == "<init>";
    let mut initial = Vec::new();
    if !member.is_static() {
        if is_constructor && this_class != "java/lang/Object" {
            initial.push(VType::UninitializedThis);
        } else {
            initial.push(VType::Reference(this_class.clone()));
        }
    }
    // Category-2 parameters (`long`/`double`) occupy two local slots: push the type
    // then a `Top` high half, so a later `lload_2` etc. indexes the right slot.
    for param in parse_params(&descriptor) {
        let category2 = matches!(param, VType::Long | VType::Double);
        initial.push(param);
        if category2 {
            initial.push(VType::Top);
        }
    }

    let stackmap = stack_map_of(class, &code);
    let frames = match &stackmap {
        Some(table) => decode(table, initial.clone(), class),
        None => HashMap::new(),
    };

    // Validate the exception table up front (JVMS §4.10.1.6): every caught type must
    // be a `Throwable`, and each handler must enter with just the exception on its
    // stack. (In inference mode the handler stacks are seeded directly, so the
    // frame-based half of this check is simply skipped — `frames` is empty.)
    verify_exception_table(&code, class, &frames, metaspace, &name)?;

    // The entry state: the parameter locals (built above), an empty operand stack.
    let mut entry = TypeState::new(max_locals);
    for (slot, ty) in initial.into_iter().enumerate() {
        if slot < entry.locals.len() {
            entry.locals[slot] = ty;
        }
    }

    let ctx = MethodCtx {
        name: &name,
        this_class: &this_class,
        descriptor: &descriptor,
        is_constructor,
        max_stack,
        max_locals,
    };

    // Two verification strategies (JVMS §4.10). With a StackMapTable (Java 6+), check
    // the compiler's frames in a single linear pass, then **cross-check** those frames
    // against the inference fixpoint so a forged table can't slip a lie past the linear
    // pass. Without a table — or when a test forces it — verify by inference alone.
    if stackmap.is_some() && !force_inference {
        verify_with_stackmap(&code.code, &frames, entry.clone(), metaspace, class, &ctx)?;
        let inferred = infer_states(&code, entry, metaspace, class, &ctx)?;
        cross_check_stackmap(&frames, &inferred, metaspace, &name)
    } else {
        verify_by_inference(&code, entry, metaspace, class, &ctx)
    }
}

/// An instruction's control-flow effect, returned by [`transfer`] alongside the
/// mutated type state: any explicit branch `targets`, and whether it `fallthrough`s
/// to the next instruction in sequence.
struct Transfer {
    targets: Vec<usize>,
    fallthrough: bool,
}

/// The method-level context threaded through verification: identity (`name`,
/// `this_class`, `descriptor`), whether it is a constructor, and the declared frame
/// sizes. Bundled so the per-opcode [`transfer`] and both drivers share one handle.
struct MethodCtx<'a> {
    name: &'a str,
    this_class: &'a str,
    descriptor: &'a str,
    is_constructor: bool,
    max_stack: usize,
    max_locals: usize,
}

/// One instruction's **type transition** (JVMS §4.10.1): applies the opcode at `pc`
/// to `state` — popping/pushing/typing as it dictates, erroring on any type-safety
/// violation — and returns the new state plus its control flow. The opcode semantics
/// live here once, shared by the StackMapTable checker and the inference fixpoint.
fn transfer(
    bytes: &[u8],
    pc: usize,
    mut state: TypeState,
    class: &ClassFile,
    metaspace: &mut MetaspaceService,
    ctx: &MethodCtx,
) -> Result<(TypeState, Transfer), VerifyError> {
    let name = ctx.name.to_string();
    let descriptor = ctx.descriptor.to_string();
    let this_class = ctx.this_class.to_string();
    let is_constructor = ctx.is_constructor;
    let mut flow = Transfer { targets: Vec::new(), fallthrough: true };

    match bytes[pc] {
            // iconst_*, bipush, sipush → push an int.
            0x02..=0x08 | 0x10 | 0x11 => state.stack.push(VType::Int),

            // iload(_n) → the local must actually hold an int; push it.
            0x1a..=0x1d | 0x15 => {
                let slot = slot_of(bytes, pc, 0x1a, 0x15);
                load_local(&mut state, slot, &VType::Int, metaspace, &name, pc)?;
            }

            // aload(_n) → the local must hold a *reference*; push its (precise) type.
            0x2a..=0x2d | 0x19 => {
                let slot = slot_of(bytes, pc, 0x2a, 0x19);
                let ty = state.locals.get(slot).cloned().unwrap_or(VType::Top);
                if !is_reference(&ty) {
                    return Err(err(&name, pc, format!("aload: local {slot} holds {ty:?}, not a reference")));
                }
                state.stack.push(ty);
            }

            // istore(_n) → pop an int into the local.
            0x3b..=0x3e | 0x36 => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Int, metaspace, &name, pc)?;
                let slot = slot_of(bytes, pc, 0x3b, 0x36);
                set_local(&mut state, slot, VType::Int);
            }

            // astore(_n) → pop a reference into the local.
            0x4b..=0x4e | 0x3a => {
                let v = pop(&mut state, &name, pc)?;
                let slot = slot_of(bytes, pc, 0x4b, 0x3a);
                set_local(&mut state, slot, v);
            }

            // int binary ops → pop two ints, push an int: add/sub/mul, div/rem,
            // bitwise (and/or/xor) and the shifts (`ishl`/`ishr`/`iushr` — the shift
            // amount is also an int, so they fit here too).
            0x60 | 0x64 | 0x68 | 0x6c | 0x70 | 0x78 | 0x7a | 0x7c | 0x7e | 0x80 | 0x82 => {
                let b = pop(&mut state, &name, pc)?;
                expect(&b, &VType::Int, metaspace, &name, pc)?;
                let a = pop(&mut state, &name, pc)?;
                expect(&a, &VType::Int, metaspace, &name, pc)?;
                state.stack.push(VType::Int);
            }

            // goto → unconditional: check the target frame, then no fall-through.
            0xa7 => {
                flow.targets.push(branch_target(bytes, pc));
                flow.fallthrough = false;
            }

            // if_icmp* (eq/ne/lt/ge/gt/le) → pop two ints; branch or fall through.
            0x9f..=0xa4 => {
                let b = pop(&mut state, &name, pc)?;
                expect(&b, &VType::Int, metaspace, &name, pc)?;
                let a = pop(&mut state, &name, pc)?;
                expect(&a, &VType::Int, metaspace, &name, pc)?;
                flow.targets.push(branch_target(bytes, pc));
            }

            // if* (compare a single int to zero) → pop one int; branch or fall through.
            0x99..=0x9e => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Int, metaspace, &name, pc)?;
                flow.targets.push(branch_target(bytes, pc));
            }

            // invokestatic → pop the parameters, push the return type.
            0xb8 => {
                let (mname, desc) = class
                    .methodref_target(u2(bytes, pc))
                    .map(|(_, n, d)| (n.to_string(), d.to_string()))
                    .ok_or_else(|| err(&name, pc, "invokestatic: bad Methodref"))?;
                reject_special_name(&mname, "invokestatic", &name, pc)?;
                for param in parse_params(&desc).iter().rev() {
                    let v = pop(&mut state, &name, pc)?;
                    expect(&v, param, metaspace, &name, pc)?;
                }
                if let Some(ret) = return_type(&desc) {
                    state.stack.push(ret);
                }
            }

            // ireturn → pop an int; method ends (no fall-through).
            0xac => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Int, metaspace, &name, pc)?;
                flow.fallthrough = false;
            }

            // return (void) → method ends. In a constructor, `this` must be fully
            // initialised on this path (its `super`/`this` `<init>` has run).
            0xb1 => {
                if is_constructor {
                    assert_no_uninitialized(&state, &name, pc, "constructor return")?;
                }
                flow.fallthrough = false;
            }

            // --- long / double (category-2) -------------------------------------
            // lconst_0/1 → long; dconst_0/1 → double; ldc2_w → whichever the pool says.
            0x09 | 0x0a => state.stack.push(VType::Long),
            0x0e | 0x0f => state.stack.push(VType::Double),
            0x14 => state.stack.push(ldc2_type(class, u2(bytes, pc))),
            // lload/dload(_n) → the local must hold a long/double; push it.
            0x16 | 0x1e..=0x21 => {
                let slot = slot_of(bytes, pc, 0x1e, 0x16);
                load_local(&mut state, slot, &VType::Long, metaspace, &name, pc)?;
            }
            0x18 | 0x26..=0x29 => {
                let slot = slot_of(bytes, pc, 0x26, 0x18);
                load_local(&mut state, slot, &VType::Double, metaspace, &name, pc)?;
            }
            // lstore/dstore(_n) → pop the value into the local; its high half is Top.
            0x37 | 0x3f..=0x42 => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Long, metaspace, &name, pc)?;
                let slot = slot_of(bytes, pc, 0x3f, 0x37);
                set_local(&mut state, slot, VType::Long);
                set_local(&mut state, slot + 1, VType::Top);
            }
            0x39 | 0x47..=0x4a => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Double, metaspace, &name, pc)?;
                let slot = slot_of(bytes, pc, 0x47, 0x39);
                set_local(&mut state, slot, VType::Double);
                set_local(&mut state, slot + 1, VType::Top);
            }
            // long binary ops → pop two longs, push a long: add/sub/mul, div/rem,
            // bitwise (and/or/xor). (Long *shifts* take an int amount — handled below.)
            0x61 | 0x65 | 0x69 | 0x6d | 0x71 | 0x7f | 0x81 | 0x83 => {
                let b = pop(&mut state, &name, pc)?;
                expect(&b, &VType::Long, metaspace, &name, pc)?;
                let a = pop(&mut state, &name, pc)?;
                expect(&a, &VType::Long, metaspace, &name, pc)?;
                state.stack.push(VType::Long);
            }
            // double binary ops → pop two doubles, push a double (add/sub/mul/div/rem).
            0x63 | 0x67 | 0x6b | 0x6f | 0x73 => {
                let b = pop(&mut state, &name, pc)?;
                expect(&b, &VType::Double, metaspace, &name, pc)?;
                let a = pop(&mut state, &name, pc)?;
                expect(&a, &VType::Double, metaspace, &name, pc)?;
                state.stack.push(VType::Double);
            }
            // lreturn / dreturn → pop the category-2 value; method ends.
            0xad => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Long, metaspace, &name, pc)?;
                flow.fallthrough = false;
            }
            0xaf => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Double, metaspace, &name, pc)?;
                flow.fallthrough = false;
            }
            // areturn → pop a reference assignable to the method's declared return
            // type; method ends (no fall-through).
            0xb0 => {
                let v = pop(&mut state, &name, pc)?;
                if !is_reference(&v) {
                    return Err(err(&name, pc, format!("areturn: {v:?} is not a reference")));
                }
                if let Some(ret) = return_type(&descriptor) {
                    expect(&v, &ret, metaspace, &name, pc)?;
                }
                flow.fallthrough = false;
            }

            // --- float (category-1) ---------------------------------------------
            // fconst_0/1/2 → push a float. (ldc of a float is handled by the ldc arm.)
            0x0b | 0x0c | 0x0d => state.stack.push(VType::Float),
            // fload(_n) → the local must hold a float; push it.
            0x17 | 0x22..=0x25 => {
                let slot = slot_of(bytes, pc, 0x22, 0x17);
                load_local(&mut state, slot, &VType::Float, metaspace, &name, pc)?;
            }
            // fstore(_n) → pop a float into the local (one slot — no high half).
            0x38 | 0x43..=0x46 => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Float, metaspace, &name, pc)?;
                let slot = slot_of(bytes, pc, 0x43, 0x38);
                set_local(&mut state, slot, VType::Float);
            }
            // float binary ops → pop two floats, push a float (add/sub/mul/div/rem).
            0x62 | 0x66 | 0x6a | 0x6e | 0x72 => {
                let b = pop(&mut state, &name, pc)?;
                expect(&b, &VType::Float, metaspace, &name, pc)?;
                let a = pop(&mut state, &name, pc)?;
                expect(&a, &VType::Float, metaspace, &name, pc)?;
                state.stack.push(VType::Float);
            }

            // negation (ineg/lneg/fneg/dneg) → pop one of the type, push the same.
            0x74 | 0x75 | 0x76 | 0x77 => {
                let ty = match bytes[pc] {
                    0x74 => VType::Int,
                    0x75 => VType::Long,
                    0x76 => VType::Float,
                    _ => VType::Double,
                };
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &ty, metaspace, &name, pc)?;
                state.stack.push(ty);
            }

            // long shifts (lshl/lshr/lushr) → pop the int amount, then the long, push a long.
            0x79 | 0x7b | 0x7d => {
                let amount = pop(&mut state, &name, pc)?;
                expect(&amount, &VType::Int, metaspace, &name, pc)?;
                let value = pop(&mut state, &name, pc)?;
                expect(&value, &VType::Long, metaspace, &name, pc)?;
                state.stack.push(VType::Long);
            }

            // iinc → increments an int local in place; no operand-stack effect.
            0x84 => {}
            // freturn → pop a float; method ends.
            0xae => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &VType::Float, metaspace, &name, pc)?;
                flow.fallthrough = false;
            }

            // lcmp / fcmpl / fcmpg / dcmpl / dcmpg → pop two of the type, push an int.
            0x94 | 0x95 | 0x96 | 0x97 | 0x98 => {
                let ty = match bytes[pc] {
                    0x94 => VType::Long,
                    0x95 | 0x96 => VType::Float,
                    _ => VType::Double,
                };
                let b = pop(&mut state, &name, pc)?;
                expect(&b, &ty, metaspace, &name, pc)?;
                let a = pop(&mut state, &name, pc)?;
                expect(&a, &ty, metaspace, &name, pc)?;
                state.stack.push(VType::Int);
            }

            // --- numeric conversions (i2l..i2s) → pop the source, push the target.
            0x85..=0x93 => {
                let (from, to) = match bytes[pc] {
                    0x85 => (VType::Int, VType::Long),
                    0x86 => (VType::Int, VType::Float),
                    0x87 => (VType::Int, VType::Double),
                    0x88 => (VType::Long, VType::Int),
                    0x89 => (VType::Long, VType::Float),
                    0x8a => (VType::Long, VType::Double),
                    0x8b => (VType::Float, VType::Int),
                    0x8c => (VType::Float, VType::Long),
                    0x8d => (VType::Float, VType::Double),
                    0x8e => (VType::Double, VType::Int),
                    0x8f => (VType::Double, VType::Long),
                    0x90 => (VType::Double, VType::Float),
                    _ => (VType::Int, VType::Int), // i2b/i2c/i2s (0x91..0x93)
                };
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &from, metaspace, &name, pc)?;
                state.stack.push(to);
            }

            // aconst_null → push the null reference.
            0x01 => state.stack.push(VType::Null),

            // ldc / ldc_w → push the constant's type (a String or an int, here).
            0x12 => state.stack.push(ldc_type(class, bytes[pc + 1] as u16)),
            0x13 => state.stack.push(ldc_type(class, u2(bytes, pc))),

            // Operand-stack manipulation (pop/dup/swap, 0x57..0x5f): category-aware
            // reshuffling of the verifier's type stack, mirroring the interpreter.
            0x57..=0x5f => {
                let stack = &mut state.stack;
                let len = stack.len();
                match bytes[pc] {
                    0x57 => drop_slots(stack, 1),                 // pop
                    0x58 => drop_slots(stack, 2),                 // pop2
                    0x5f => stack.swap(len - 1, len - 2),         // swap
                    op => {
                        let (dup_slots, skip_slots) = match op {
                            0x59 => (1, 0), // dup
                            0x5a => (1, 1), // dup_x1
                            0x5b => (1, 2), // dup_x2
                            0x5c => (2, 0), // dup2
                            0x5d => (2, 1), // dup2_x1
                            _ => (2, 2),    // dup2_x2 (0x5e)
                        };
                        dup_insert_types(stack, dup_slots, skip_slots);
                    }
                }
            }

            // if_acmpeq / if_acmpne → pop two (initialized) references; branch or fall.
            0xa5 | 0xa6 => {
                use_reference(&mut state, &name, pc)?;
                use_reference(&mut state, &name, pc)?;
                flow.targets.push(branch_target(bytes, pc));
            }

            // ifnull / ifnonnull → pop one reference, compare to null; branch or fall.
            0xc6 | 0xc7 => {
                use_reference(&mut state, &name, pc)?;
                flow.targets.push(branch_target(bytes, pc));
            }

            // monitorenter / monitorexit → pop the (initialized) lock object reference.
            0xc2 | 0xc3 => {
                use_reference(&mut state, &name, pc)?;
            }

            // new → push a freshly-allocated, *uninitialised* object tagged by this pc.
            0xbb => state.stack.push(VType::Uninitialized(pc)),

            // getstatic / putstatic → push / pop the field's type (no receiver).
            0xb2 => state.stack.push(field_type(class, u2(bytes, pc))),
            0xb3 => {
                let ft = field_type(class, u2(bytes, pc));
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &ft, metaspace, &name, pc)?;
            }

            // getfield → pop the (initialized) receiver, push the field's type.
            0xb4 => {
                let ft = field_type(class, u2(bytes, pc));
                let receiver = use_reference(&mut state, &name, pc)?;
                check_protected_field(class, metaspace, &this_class, u2(bytes, pc), &receiver, &name, pc)?;
                state.stack.push(ft);
            }
            // putfield → pop the value (of the field's type), then the receiver.
            0xb5 => {
                let ft = field_type(class, u2(bytes, pc));
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &ft, metaspace, &name, pc)?;
                let receiver = use_reference(&mut state, &name, pc)?;
                check_protected_field(class, metaspace, &this_class, u2(bytes, pc), &receiver, &name, pc)?;
            }

            // invokevirtual / invokeinterface → pop params + receiver, push the return.
            0xb6 | 0xb9 => {
                let (mclass, mname, desc) = class
                    .methodref_target(u2(bytes, pc))
                    .map(|(c, n, d)| (c.to_string(), n.to_string(), d.to_string()))
                    .ok_or_else(|| err(&name, pc, "invoke: bad Methodref"))?;
                reject_special_name(&mname, "invoke", &name, pc)?;
                // invokeinterface carries an arg-slot `count` and a zero byte (§4.9.1).
                if bytes[pc] == 0xb9 {
                    check_invokeinterface_count(bytes, pc, &desc, &name)?;
                }
                for param in parse_params(&desc).iter().rev() {
                    let v = pop(&mut state, &name, pc)?;
                    expect(&v, param, metaspace, &name, pc)?;
                }
                let receiver = use_reference(&mut state, &name, pc)?;
                // invokevirtual is subject to the protected-access rule (§4.10.1.8).
                if bytes[pc] == 0xb6 {
                    check_protected_access(metaspace, &this_class, &mclass, &mname, &desc, false, &receiver, &name, pc)?;
                }
                if let Some(ret) = return_type(&desc) {
                    state.stack.push(ret);
                }
            }

            // invokespecial → constructors (`<init>`) and private/super calls. For an
            // `<init>`, the receiver's *uninitialised* type becomes the initialised
            // reference everywhere it appears (the `new`/`dup` copy, the `this` slot).
            0xb7 => {
                let (mclass, mname, mdesc) = class
                    .methodref_target(u2(bytes, pc))
                    .map(|(c, n, d)| (c.to_string(), n.to_string(), d.to_string()))
                    .ok_or_else(|| err(&name, pc, "invokespecial: bad Methodref"))?;
                // `<init>` is invokespecial's job; `<clinit>` is only ever VM-run.
                if mname == "<clinit>" {
                    return Err(err(&name, pc, "invokespecial must not target <clinit>"));
                }
                for param in parse_params(&mdesc).iter().rev() {
                    let v = pop(&mut state, &name, pc)?;
                    expect(&v, param, metaspace, &name, pc)?;
                }
                let receiver = pop(&mut state, &name, pc)?;
                if mname == "<init>" {
                    // `<init>` may only target an *uninitialised* object, and exactly
                    // once: after this, every copy of that object becomes a normal
                    // reference, so a second `<init>` on it pops a `Reference` and is
                    // rejected here. A `new`'s object initialises to the constructed
                    // class; an `UninitializedThis` (a `super`/`this` call) to *this*
                    // method's class.
                    let initialized = match &receiver {
                        VType::UninitializedThis => VType::Reference(this_class.clone()),
                        VType::Uninitialized(_) => VType::Reference(mclass.clone()),
                        other => {
                            return Err(err(&name, pc, format!(
                                "<init> receiver {other:?} is not an uninitialized object"
                            )));
                        }
                    };
                    initialize(&mut state, &receiver, &initialized);
                } else {
                    // A non-`<init>` invokespecial (a `private`/`super` method call)
                    // needs a fully-initialised receiver.
                    if !matches!(receiver, VType::Reference(_) | VType::Null) {
                        return Err(err(&name, pc, format!(
                            "invokespecial receiver {receiver:?} is not an initialized reference"
                        )));
                    }
                    if let Some(ret) = return_type(&mdesc) {
                        state.stack.push(ret);
                    }
                }
            }

            // athrow → pop the exception; it must be a `Throwable` (or `null`, which
            // is assignable to any reference). The method doesn't fall through.
            0xbf => {
                let v = pop(&mut state, &name, pc)?;
                expect(&v, &throwable(), metaspace, &name, pc)?;
                flow.fallthrough = false;
            }

            // instanceof → pop an (initialized) reference, push an int.
            0xc1 => {
                use_reference(&mut state, &name, pc)?;
                state.stack.push(VType::Int);
            }
            // checkcast → pop a reference, push the (narrowed) target reference type.
            0xc0 => {
                let target = class.class_name(u2(bytes, pc)).unwrap_or("?").to_string();
                use_reference(&mut state, &name, pc)?;
                state.stack.push(VType::Reference(target));
            }

            // arraylength → pop the (initialized) array reference, push an int.
            0xbe => {
                use_reference(&mut state, &name, pc)?;
                state.stack.push(VType::Int);
            }
            // int-category array loads (iaload/baload/caload/saload) → pop index +
            // array, push an int.
            0x2e | 0x33 | 0x34 | 0x35 => {
                pop(&mut state, &name, pc)?; // index
                use_reference(&mut state, &name, pc)?; // array
                state.stack.push(VType::Int);
            }
            // aaload → pop index + array, push the array's component (reference) type.
            0x32 => {
                pop(&mut state, &name, pc)?; // index
                let array = use_reference(&mut state, &name, pc)?;
                state.stack.push(component_type(&array));
            }
            // laload / faload / daload → pop index + array, push the typed element.
            0x2f | 0x30 | 0x31 => {
                pop(&mut state, &name, pc)?; // index
                use_reference(&mut state, &name, pc)?; // array
                state.stack.push(match bytes[pc] {
                    0x2f => VType::Long,
                    0x30 => VType::Float,
                    _ => VType::Double,
                });
            }
            // array stores (iastore/bastore/castore/sastore/aastore, plus the typed
            // lastore/fastore/dastore) → pop value, index, array.
            0x4f | 0x50 | 0x51 | 0x52 | 0x53 | 0x54 | 0x55 | 0x56 => {
                pop(&mut state, &name, pc)?; // value
                pop(&mut state, &name, pc)?; // index
                use_reference(&mut state, &name, pc)?; // array
            }
            // newarray → pop the count, push the primitive array's reference type.
            0xbc => {
                pop(&mut state, &name, pc)?; // count
                state.stack.push(VType::Reference(primitive_array_type(bytes[pc + 1]).to_string()));
            }
            // anewarray → pop the count, push the reference array's type.
            0xbd => {
                let element = class.class_name(u2(bytes, pc)).unwrap_or("?");
                pop(&mut state, &name, pc)?; // count
                state.stack.push(VType::Reference(format!("[L{element};")));
            }

            // tableswitch / lookupswitch → pop the int key; control transfers to the
            // `default` or one of the cases (no fall-through). The targets come from
            // the same parser the structural check uses.
            0xaa | 0xab => {
                let key = pop(&mut state, &name, pc)?;
                expect(&key, &VType::Int, metaspace, &name, pc)?;
                flow.targets = branch_targets(bytes, pc);
                flow.fallthrough = false;
            }

            other => return Err(unsupported(&name, pc, other)),
        }

    Ok((state, flow))
}

/// The **StackMapTable** driver (JVMS §4.10.1): one linear pass. At each frame point
/// the flowed-in state must fit the compiler's frame, which then becomes authoritative;
/// every opcode's [`transfer`] is applied and each branch target checked against its
/// frame. `reachable` skips dead code that no frame anchors.
fn verify_with_stackmap(
    bytes: &[u8],
    frames: &HashMap<usize, TypeState>,
    entry: TypeState,
    metaspace: &mut MetaspaceService,
    class: &ClassFile,
    ctx: &MethodCtx,
) -> Result<(), VerifyError> {
    let mut state = entry;
    let mut reachable = true;
    for ins in disassemble(bytes) {
        let pc = ins.pc;

        if let Some(frame) = frames.get(&pc) {
            // The `if reachable` reads the *incoming* reachability (did the previous
            // instruction fall through?); a frame then re-anchors the state, and the
            // point is reachable regardless — `reachable` is set from `flow` below.
            if reachable {
                assignable_state(&state, frame, metaspace, ctx.name, pc)?;
            }
            state = adopt(frame, ctx.max_locals);
        } else if !reachable {
            continue; // unreachable code with no frame to anchor it
        }

        let (next, flow) = transfer(bytes, pc, state, class, metaspace, ctx)?;
        state = next;
        for target in &flow.targets {
            check_branch(&state, *target, frames, metaspace, ctx.name, pc)?;
        }
        check_max_stack(&state, ctx.max_stack, ctx.name, pc)?;
        reachable = flow.fallthrough;
    }
    Ok(())
}

/// The **inference** driver (JVMS §4.10.2): the legacy verifier, used when there is no
/// StackMapTable. A work-list fixpoint — each instruction's inferred entry state flows
/// to its successors (and, for any instruction inside a `try` range, to the handler
/// with just the exception on the stack); successors **merge** by the type lattice's
/// join ([`join_states`]). It terminates because states only ever move up the
/// finite-height lattice. A type error in any [`transfer`], an inconsistent stack
/// height at a merge, or an uninitialised object across a back-edge fails verification.
fn verify_by_inference(
    code: &Code,
    entry: TypeState,
    metaspace: &mut MetaspaceService,
    class: &ClassFile,
    ctx: &MethodCtx,
) -> Result<(), VerifyError> {
    infer_states(code, entry, metaspace, class, ctx).map(|_| ())
}

/// The inference fixpoint itself — returns the inferred entry [`TypeState`] of every
/// reachable instruction. [`verify_by_inference`] discards the map; the StackMapTable
/// path keeps it to cross-check the compiler's frames ([`cross_check_stackmap`]).
fn infer_states(
    code: &Code,
    entry: TypeState,
    metaspace: &mut MetaspaceService,
    class: &ClassFile,
    ctx: &MethodCtx,
) -> Result<HashMap<usize, TypeState>, VerifyError> {
    let bytes = &code.code;
    let instrs = disassemble(bytes);
    let starts: HashSet<usize> = instrs.iter().map(|i| i.pc).collect();
    let next_pc: HashMap<usize, usize> = instrs.iter().map(|i| (i.pc, i.pc + i.length)).collect();

    // The inferred entry state of each instruction, refined as more paths reach it.
    let mut in_states: HashMap<usize, TypeState> = HashMap::new();
    in_states.insert(0, entry);
    let mut worklist: Vec<usize> = vec![0];

    while let Some(pc) = worklist.pop() {
        let state = in_states.get(&pc).cloned().expect("a queued pc has an in-state");

        // Any instruction in a `try` range can throw: flow its locals to the handler,
        // which is entered with just the caught exception on the operand stack.
        for handler in &code.exception_table {
            if (handler.start_pc as usize) <= pc && pc < handler.end_pc as usize {
                let exc = if handler.catch_type == 0 {
                    throwable()
                } else {
                    VType::Reference(class.class_name(handler.catch_type).unwrap_or("?").to_string())
                };
                let into = TypeState { stack: vec![exc], locals: state.locals.clone() };
                propagate(handler.handler_pc as usize, into, &starts, metaspace, ctx, &mut in_states, &mut worklist, pc)?;
            }
        }

        let (out, flow) = transfer(bytes, pc, state, class, metaspace, ctx)?;
        check_max_stack(&out, ctx.max_stack, ctx.name, pc)?;

        let mut successors = flow.targets;
        if flow.fallthrough {
            successors.push(*next_pc.get(&pc).unwrap_or(&bytes.len()));
        }
        for succ in successors {
            // An uninitialised object must not survive a loop's back-edge (JVMS
            // §4.10.2.4) — the same rule the StackMapTable driver enforces in `check_branch`.
            if succ <= pc {
                assert_no_uninitialized(&out, ctx.name, pc, "backward branch")?;
            }
            propagate(succ, out.clone(), &starts, metaspace, ctx, &mut in_states, &mut worklist, pc)?;
        }
    }
    Ok(in_states)
}

/// Cross-checks the compiler's StackMapTable against what inference derives (a
/// consistency guard on a possibly-malicious table). At every declared frame the
/// inference actually reached, the operand-stack **heights** must match and each stack
/// slot / local must be **compatible** — one assignable to the other. Equality isn't
/// required (our `join` is coarser than javac's at interface merges, folding to
/// `Object`), but a genuine contradiction — a wrong height, or `Int` where inference
/// found a reference — is rejected.
fn cross_check_stackmap(
    frames: &HashMap<usize, TypeState>,
    inferred: &HashMap<usize, TypeState>,
    metaspace: &mut MetaspaceService,
    method: &str,
) -> Result<(), VerifyError> {
    for (pc, declared) in frames {
        let Some(found) = inferred.get(pc) else { continue }; // unreachable by inference
        if found.stack.len() != declared.stack.len() {
            return Err(err(method, *pc, format!(
                "StackMapTable stack height {} contradicts inferred {}",
                declared.stack.len(), found.stack.len()
            )));
        }
        for (a, b) in found.stack.iter().zip(&declared.stack) {
            if !compatible(a, b, metaspace) {
                return Err(err(method, *pc, format!("StackMapTable stack {b:?} contradicts inferred {a:?}")));
            }
        }
        for (i, b) in declared.locals.iter().enumerate() {
            let a = found.locals.get(i).unwrap_or(&VType::Top);
            if !compatible(a, b, metaspace) {
                return Err(err(method, *pc, format!("StackMapTable local {i} {b:?} contradicts inferred {a:?}")));
            }
        }
    }
    Ok(())
}

/// Whether two verification types don't **contradict** — one is assignable to the other
/// (sub/supertype, either direction). Looser than equality, to tolerate `join`'s
/// coarseness, while still catching a real type clash.
fn compatible(a: &VType, b: &VType, metaspace: &mut MetaspaceService) -> bool {
    a.is_assignable_to(b, metaspace) || b.is_assignable_to(a, metaspace)
}

/// Merges `candidate` into the inferred in-state at `target` (by the lattice join) and
/// re-queues `target` if its state changed. Errors if `target` is not an instruction
/// boundary (a branch into the middle of an instruction) — caught here so inference
/// never reads a bogus pc.
#[allow(clippy::too_many_arguments)]
fn propagate(
    target: usize,
    candidate: TypeState,
    starts: &HashSet<usize>,
    metaspace: &mut MetaspaceService,
    ctx: &MethodCtx,
    in_states: &mut HashMap<usize, TypeState>,
    worklist: &mut Vec<usize>,
    pc: usize,
) -> Result<(), VerifyError> {
    if !starts.contains(&target) {
        return Err(err(ctx.name, pc, format!("control flows to {target}, not an instruction boundary")));
    }
    let merged = match in_states.get(&target) {
        None => candidate,
        Some(existing) => join_states(existing, &candidate, ctx.max_locals, metaspace, ctx.name, pc)?,
    };
    let changed = match in_states.get(&target) {
        Some(existing) => existing.stack != merged.stack || existing.locals != merged.locals,
        None => true,
    };
    if changed {
        in_states.insert(target, merged);
        if !worklist.contains(&target) {
            worklist.push(target);
        }
    }
    Ok(())
}

/// The **merge** of two inferred states at a control-flow join: the operand stacks must
/// have equal height (a depth mismatch is a structural type error), then each stack
/// slot and local is joined by [`VType::join`] (the lattice LUB). Locals are normalised
/// to `max_locals`, padding with `Top`, so all in-states share one shape.
fn join_states(
    a: &TypeState,
    b: &TypeState,
    max_locals: usize,
    metaspace: &mut MetaspaceService,
    method: &str,
    pc: usize,
) -> Result<TypeState, VerifyError> {
    if a.stack.len() != b.stack.len() {
        return Err(err(
            method,
            pc,
            format!("stack height mismatch at a merge: {} vs {}", a.stack.len(), b.stack.len()),
        ));
    }
    let stack = a.stack.iter().zip(&b.stack).map(|(x, y)| x.join(y, metaspace)).collect();
    let mut locals = Vec::with_capacity(max_locals);
    for i in 0..max_locals {
        let x = a.locals.get(i).unwrap_or(&VType::Top);
        let y = b.locals.get(i).unwrap_or(&VType::Top);
        locals.push(x.join(y, metaspace));
    }
    Ok(TypeState { stack, locals })
}

/// Enforces the declared `max_stack`: the operand-stack depth in *slots* (a
/// `long`/`double` counts as two) must never exceed it. Checked after every opcode.
fn check_max_stack(state: &TypeState, max_stack: usize, method: &str, pc: usize) -> Result<(), VerifyError> {
    let depth: usize = state.stack.iter().map(type_width).sum();
    if depth > max_stack {
        return Err(err(method, pc, format!("operand stack depth {depth} exceeds max_stack {max_stack}")));
    }
    Ok(())
}

/// **Structural** verification (JVMS §4.9) — the static well-formedness of the `Code`,
/// checked once up front so the type-checking data flow can trust the control-flow
/// graph. It rejects:
///  - empty code, or a final instruction truncated past `code_length`;
///  - execution **falling off the bottom** of the code (the last instruction must
///    transfer control: a return/throw/goto/switch);
///  - **subroutines** (`jsr`/`jsr_w`/`ret`) — illegal under the type-checking verifier
///    (JVMS §4.10.1);
///  - any branch/switch **target** that isn't an instruction boundary;
///  - an **exception table** range that is empty, out of order, or whose start/end/
///    handler isn't an instruction boundary.
fn structural_check(code: &Code, method: &str) -> Result<(), VerifyError> {
    let bytes = &code.code;
    if bytes.is_empty() {
        return Err(err(method, 0, "method Code is empty"));
    }
    let instrs = disassemble(bytes);
    let starts: HashSet<usize> = instrs.iter().map(|i| i.pc).collect();

    // The decoded stream must cover the code exactly — nothing truncated off the end.
    let last = instrs.last().expect("non-empty code yields ≥1 instruction");
    let end = last.pc + last.length;
    if end != bytes.len() {
        return Err(err(method, last.pc, format!(
            "instruction at {} runs to {end}, past code length {}", last.pc, bytes.len()
        )));
    }
    // Execution must not be able to run off the bottom of the code array.
    if can_fall_through(bytes[last.pc]) {
        return Err(err(method, last.pc, "control can fall off the end of the code"));
    }

    for ins in &instrs {
        let pc = ins.pc;
        // Subroutines are not allowed (jsr 0xa8, jsr_w 0xc9, ret 0xa9, wide ret).
        if matches!(bytes[pc], 0xa8 | 0xc9 | 0xa9) || ins.mnemonic == "ret_w" {
            return Err(err(method, pc, format!("subroutines (jsr/ret) are not allowed: {}", ins.mnemonic)));
        }
        // Every branch/switch target must land on an instruction boundary.
        for target in branch_targets(bytes, pc) {
            if !starts.contains(&target) {
                return Err(err(method, pc, format!("branch target {target} is not an instruction boundary")));
            }
        }
    }

    // Exception-table ranges: non-empty, in order, on instruction boundaries (the
    // exclusive `end_pc` may equal `code_length`).
    for h in &code.exception_table {
        let (s, e, hp) = (h.start_pc as usize, h.end_pc as usize, h.handler_pc as usize);
        if s >= e {
            return Err(err(method, s, format!("exception range start {s} is not before end {e}")));
        }
        if !starts.contains(&s) {
            return Err(err(method, s, format!("exception range start {s} is not an instruction boundary")));
        }
        if e != bytes.len() && !starts.contains(&e) {
            return Err(err(method, e, format!("exception range end {e} is not an instruction boundary")));
        }
        if !starts.contains(&hp) {
            return Err(err(method, hp, format!("exception handler {hp} is not an instruction boundary")));
        }
    }
    Ok(())
}

/// Whether control can continue past this opcode to the next instruction in sequence —
/// false for the unconditional transfers (`goto`/`goto_w`, the switches, the returns,
/// `athrow`, `ret`). Used to forbid execution falling off the end of the code.
fn can_fall_through(op: u8) -> bool {
    !matches!(op, 0xa7 | 0xc8 | 0xaa | 0xab | 0xac..=0xb1 | 0xbf | 0xa9)
}

/// The explicit branch targets of the instruction at `pc` (absolute offsets): the
/// single destination of a `goto`/`if*`/`goto_w`, or the `default` plus every case of
/// a `tableswitch`/`lookupswitch`. Non-branching opcodes have none.
fn branch_targets(bytes: &[u8], pc: usize) -> Vec<usize> {
    match bytes[pc] {
        // 2-byte relative branch: if*, if_icmp*, if_acmp*, goto, jsr, ifnull/ifnonnull.
        0x99..=0xa8 | 0xc6 | 0xc7 => vec![branch_target(bytes, pc)],
        // 4-byte relative branch: goto_w, jsr_w.
        0xc8 | 0xc9 => {
            let off = i32::from_be_bytes([bytes[pc + 1], bytes[pc + 2], bytes[pc + 3], bytes[pc + 4]]);
            vec![(pc as i64 + off as i64) as usize]
        }
        0xaa | 0xab => switch_targets(bytes, pc),
        _ => Vec::new(),
    }
}

/// The targets of a `tableswitch`/`lookupswitch` at `pc`: the `default`, then each case
/// target. Mirrors the disassembler's layout — 0–3 bytes of 4-byte alignment padding,
/// then 4-byte big-endian offsets relative to `pc`.
fn switch_targets(bytes: &[u8], pc: usize) -> Vec<usize> {
    let read = |i: usize| i32::from_be_bytes([bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3]]);
    let abs = |off: i32| (pc as i64 + off as i64) as usize;
    let mut i = pc + 1 + switch_padding(pc);
    let mut targets = vec![abs(read(i))]; // default
    i += 4;
    if bytes[pc] == 0xaa {
        let low = read(i);
        i += 4;
        let high = read(i);
        i += 4;
        for _ in 0..(high - low + 1).max(0) {
            targets.push(abs(read(i)));
            i += 4;
        }
    } else {
        let npairs = read(i).max(0);
        i += 4;
        for _ in 0..npairs {
            i += 4; // skip the match key
            targets.push(abs(read(i)));
            i += 4;
        }
    }
    targets
}

/// Bytes of padding after a switch opcode so its jump table is 4-byte aligned (from the
/// start of the code array) — matching the disassembler.
fn switch_padding(pc: usize) -> usize {
    (4 - ((pc + 1) % 4)) % 4
}

// --- access / linkage (JVMS §4.9.1, §4.10.1.8) ----------------------------------

/// The instance-initialisation methods may not be called by `invokevirtual`/
/// `invokestatic`/`invokeinterface` (`<init>` is only ever an `invokespecial` target,
/// `<clinit>` only the VM's to run). Rejects either special name for those opcodes.
fn reject_special_name(mname: &str, op: &str, method: &str, pc: usize) -> Result<(), VerifyError> {
    if mname == "<init>" || mname == "<clinit>" {
        return Err(err(method, pc, format!("{op} must not target {mname}")));
    }
    Ok(())
}

/// Validates an `invokeinterface`'s trailing operands (JVMS §4.9.1): the `count` byte
/// must equal the argument slots the call consumes — 1 (the receiver) plus each
/// parameter's width (a `long`/`double` counts as two) — and the 4th byte must be 0.
fn check_invokeinterface_count(bytes: &[u8], pc: usize, desc: &str, method: &str) -> Result<(), VerifyError> {
    let count = bytes[pc + 3] as usize;
    let expected = 1 + parse_params(desc).iter().map(type_width).sum::<usize>();
    if count != expected {
        return Err(err(method, pc, format!("invokeinterface count {count} should be {expected}")));
    }
    if bytes[pc + 4] != 0 {
        return Err(err(method, pc, format!("invokeinterface 4th byte must be 0, got {}", bytes[pc + 4])));
    }
    Ok(())
}

/// Looks up the `cp` Fieldref and applies the protected-access rule to it (see
/// [`check_protected_access`]). A no-op if the field reference can't be read.
fn check_protected_field(
    class: &ClassFile,
    metaspace: &mut MetaspaceService,
    this_class: &str,
    cp: u16,
    receiver: &VType,
    method: &str,
    pc: usize,
) -> Result<(), VerifyError> {
    if let Some((fc, fname, fdesc)) =
        class.fieldref_target(cp).map(|(c, n, d)| (c.to_string(), n.to_string(), d.to_string()))
    {
        check_protected_access(metaspace, this_class, &fc, &fname, &fdesc, true, receiver, method, pc)?;
    }
    Ok(())
}

/// The protected-member access rule (JVMS §4.10.1.8). When `getfield`/`putfield`/
/// `invokevirtual` touches a member that resolves to a **`protected`** declaration in
/// a **superclass of the current class** that lives in a **different runtime package**,
/// the receiver's type must be the current class or a subtype of it — you may reach an
/// inherited protected member only through your own kind, not a bare instance of the
/// declaring class. Every other case (member not protected, same package, current class
/// not a subclass, or unresolved) imposes no extra constraint.
fn check_protected_access(
    metaspace: &mut MetaspaceService,
    this_class: &str,
    ref_class: &str,
    member_name: &str,
    member_desc: &str,
    is_field: bool,
    receiver: &VType,
    method: &str,
    pc: usize,
) -> Result<(), VerifyError> {
    let Some((declaring, flags)) = resolve_member(metaspace, ref_class, member_name, member_desc, is_field) else {
        return Ok(()); // unresolved here — resolution/linking is checked elsewhere
    };
    let protected = flags & 0x0004 != 0;
    if !protected
        || !class_operations::is_subtype(metaspace, this_class, &declaring)
        || same_package(this_class, &declaring)
    {
        return Ok(());
    }
    // Cross-package, inherited, protected: the receiver must be of the current type.
    if receiver.is_assignable_to(&VType::Reference(this_class.to_string()), metaspace) {
        Ok(())
    } else {
        let kind = if is_field { "field" } else { "method" };
        Err(err(method, pc, format!(
            "protected {kind} {member_name} of {declaring} accessed via {receiver:?}, not a subtype of {this_class}"
        )))
    }
}

/// Resolves a member by walking up from `start_class` through its superclasses, looking
/// for a field/method of the given name and descriptor. Returns the **declaring** class
/// and its access flags, or `None` if it isn't found (e.g. the class isn't loadable).
fn resolve_member(
    metaspace: &mut MetaspaceService,
    start_class: &str,
    name: &str,
    descriptor: &str,
    is_field: bool,
) -> Option<(String, u16)> {
    let mut current = start_class.to_string();
    loop {
        let cf = metaspace.get_or_load(&current)?;
        let members = if is_field { &cf.fields } else { &cf.methods };
        let found = members.iter().find(|m| {
            cf.utf8(m.name_index) == Some(name) && cf.utf8(m.descriptor_index) == Some(descriptor)
        });
        if let Some(m) = found {
            return Some((current, m.access_flags));
        }
        match cf.class_name(cf.super_class).map(str::to_string) {
            Some(sup) if sup != current => current = sup,
            _ => return None,
        }
    }
}

/// Whether two binary class names share a runtime package — the substring before the
/// last `/` (the default package, with no `/`, is its own package).
fn same_package(a: &str, b: &str) -> bool {
    package_of(a) == package_of(b)
}

/// The package portion of a binary class name (`java/lang/Object` → `java/lang`;
/// `Sub` → `""`).
fn package_of(binary_name: &str) -> &str {
    match binary_name.rfind('/') {
        Some(i) => &binary_name[..i],
        None => "",
    }
}

/// The `java/lang/Throwable` reference type — the root every caught/thrown exception
/// must be assignable to.
fn throwable() -> VType {
    VType::Reference("java/lang/Throwable".to_string())
}

/// Validates a method's **exception table** (JVMS §4.10.1.6). For each handler:
///  - its `catch_type` must be a subtype of `Throwable` (a bare `finally`, encoded as
///    `catch_type == 0`, catches any `Throwable`);
///  - if the `handler_pc` carries a stack-map frame, the handler must be entered with
///    a stack of exactly **one** operand — the caught exception — and the caught type
///    must be assignable to that frame's slot (the handler can't claim a narrower or
///    unrelated type than what it actually catches).
fn verify_exception_table(
    code: &Code,
    class: &ClassFile,
    frames: &HashMap<usize, TypeState>,
    metaspace: &mut MetaspaceService,
    method: &str,
) -> Result<(), VerifyError> {
    for entry in &code.exception_table {
        let handler = entry.handler_pc as usize;
        // The type of the exception object the handler receives on its stack.
        let exc_type = if entry.catch_type == 0 {
            throwable()
        } else {
            let caught = VType::Reference(class.class_name(entry.catch_type).unwrap_or("?").to_string());
            if !caught.is_assignable_to(&throwable(), metaspace) {
                return Err(err(method, handler, format!("catch type {caught:?} is not a Throwable")));
            }
            caught
        };
        // With a stack-map frame present, the handler's entry stack must be just the
        // exception (a supertype of the caught type is allowed — e.g. a `finally`
        // declaring `Throwable`).
        if let Some(frame) = frames.get(&handler) {
            let ok = frame.stack.len() == 1 && exc_type.is_assignable_to(&frame.stack[0], metaspace);
            if !ok {
                return Err(err(
                    method,
                    handler,
                    format!("handler stack {:?} is not a single {exc_type:?}", frame.stack),
                ));
            }
        }
    }
    Ok(())
}

/// Pops the top type off the verifier's stack, or a `VerifyError` if it's empty.
fn pop(state: &mut TypeState, method: &str, pc: usize) -> Result<VType, VerifyError> {
    state.stack.pop().ok_or_else(|| err(method, pc, "operand stack underflow"))
}

/// Checks `actual` is assignable to `expected`, else a type-mismatch `VerifyError`.
fn expect(
    actual: &VType,
    expected: &VType,
    metaspace: &mut MetaspaceService,
    method: &str,
    pc: usize,
) -> Result<(), VerifyError> {
    if actual.is_assignable_to(expected, metaspace) {
        Ok(())
    } else {
        Err(err(method, pc, format!("expected {expected:?}, found {actual:?}")))
    }
}

/// Writes a local slot, growing the (max_locals-sized) vector defensively.
fn set_local(state: &mut TypeState, slot: usize, ty: VType) {
    if slot >= state.locals.len() {
        state.locals.resize(slot + 1, VType::Top);
    }
    state.locals[slot] = ty;
}

/// Reads the u2 operand (a constant-pool index) following the opcode at `pc`.
fn u2(bytes: &[u8], pc: usize) -> u16 {
    u16::from_be_bytes([bytes[pc + 1], bytes[pc + 2]])
}

/// A typed local **load**: checks `local[slot]` actually holds (is assignable to) the
/// `expected` type, then pushes it. The strict-verification rule for `iload`/`lload`/
/// `dload`/`fload` — reading a local of the wrong type is a `VerifyError`.
fn load_local(
    state: &mut TypeState,
    slot: usize,
    expected: &VType,
    metaspace: &mut MetaspaceService,
    method: &str,
    pc: usize,
) -> Result<(), VerifyError> {
    let actual = state.locals.get(slot).cloned().unwrap_or(VType::Top);
    if !actual.is_assignable_to(expected, metaspace) {
        return Err(err(method, pc, format!("load: local {slot} holds {actual:?}, expected {expected:?}")));
    }
    state.stack.push(expected.clone());
    Ok(())
}

/// Pops a reference that must be **fully initialized** — what every *use* of an
/// object needs (a field access, a method receiver, an array/`acmp`/cast operand),
/// as opposed to the move-only `aload`/`astore`/`dup`. Rejects an uninitialised
/// object (JVMS §4.10.2.4: a half-built object can only be the `invokespecial
/// <init>` receiver) and any non-reference. `null` is allowed (NPE is a run-time
/// concern, not a verification one).
fn use_reference(state: &mut TypeState, method: &str, pc: usize) -> Result<VType, VerifyError> {
    let v = pop(state, method, pc)?;
    match v {
        VType::Reference(_) | VType::Null => Ok(v),
        other => Err(err(method, pc, format!("expected an initialized reference, found {other:?}"))),
    }
}

/// Errors if any **uninitialised** object is still live (on the stack or in a local).
/// Used at two points required by JVMS §4.10.2.4:
///  - a **backward branch** — an uninitialised object must not survive a loop (else a
///    `new` in a loop could reach its `<init>` twice, or a partially-built object
///    could be smuggled around);
///  - a **constructor's `return`** — `this` must have been initialised on every path
///    (its `super(…)`/`this(…)` call must have run), so no `UninitializedThis` remains.
fn assert_no_uninitialized(
    state: &TypeState,
    method: &str,
    pc: usize,
    context: &str,
) -> Result<(), VerifyError> {
    for ty in state.stack.iter().chain(state.locals.iter()) {
        if matches!(ty, VType::Uninitialized(_) | VType::UninitializedThis) {
            return Err(err(method, pc, format!("{context}: uninitialized object {ty:?} still live")));
        }
    }
    Ok(())
}

/// Whether a verification type is a reference (what `aload`/`areturn`/`athrow` need):
/// a known class, `null`, or an as-yet-uninitialised object.
fn is_reference(ty: &VType) -> bool {
    matches!(
        ty,
        VType::Reference(_) | VType::Null | VType::UninitializedThis | VType::Uninitialized(_)
    )
}

/// The slot width of a verification type: category-2 (`long`/`double`) = 2, else 1.
/// Used by the stack-manipulation opcodes, which work in slots.
fn type_width(ty: &VType) -> usize {
    if matches!(ty, VType::Long | VType::Double) {
        2
    } else {
        1
    }
}

/// How many type entries from the top of `stack` add up to `slots` slots (0 → none).
fn type_entries_for(stack: &[VType], slots: usize) -> usize {
    if slots == 0 {
        return 0;
    }
    let mut acc = 0;
    let mut count = 0;
    for ty in stack.iter().rev() {
        acc += type_width(ty);
        count += 1;
        if acc >= slots {
            break;
        }
    }
    count
}

/// Drops the top `slots` slots' worth of type entries (`pop`/`pop2`).
fn drop_slots(stack: &mut Vec<VType>, slots: usize) {
    let n = type_entries_for(stack, slots);
    stack.truncate(stack.len() - n);
}

/// The verifier's mirror of the interpreter's `dup_insert` — over `VType`s.
fn dup_insert_types(stack: &mut Vec<VType>, dup_slots: usize, skip_slots: usize) {
    let len = stack.len();
    let dup_n = type_entries_for(stack, dup_slots);
    let skip_n = type_entries_for(&stack[..len - dup_n], skip_slots);
    let insert_at = len - dup_n - skip_n;
    let group: Vec<VType> = stack[len - dup_n..].to_vec();
    for (offset, ty) in group.into_iter().enumerate() {
        stack.insert(insert_at + offset, ty);
    }
}

/// The verification type of the field named by the `FieldRef` at `cp` — parsed from
/// its descriptor. Unknown ⇒ `Top` (harmless: assignable to/from anything).
fn field_type(class: &ClassFile, cp: u16) -> VType {
    match class.fieldref_target(cp) {
        Some((_, _, descriptor)) => parse_one(descriptor.as_bytes(), 0).0,
        None => VType::Top,
    }
}

/// The type an `ldc`/`ldc_w` of the constant at `cp` pushes: a `String` literal
/// becomes `Reference(java/lang/String)`, an `Integer` becomes `Int`; anything else
/// we don't model (float/Class/…) collapses to `Top`.
fn ldc_type(class: &ClassFile, cp: u16) -> VType {
    match class.constant_pool.get(cp.wrapping_sub(1) as usize) {
        Some(ConstantPoolEntry::String { .. }) => VType::Reference("java/lang/String".to_string()),
        Some(ConstantPoolEntry::Integer(_)) => VType::Int,
        Some(ConstantPoolEntry::Float(_)) => VType::Float,
        _ => VType::Top,
    }
}

/// The category-2 type an `ldc2_w` pushes — `Long` or `Double`, per the pool entry.
fn ldc2_type(class: &ClassFile, cp: u16) -> VType {
    match class.constant_pool.get(cp.wrapping_sub(1) as usize) {
        Some(ConstantPoolEntry::Long(_)) => VType::Long,
        Some(ConstantPoolEntry::Double(_)) => VType::Double,
        _ => VType::Top,
    }
}

/// The component type of an array reference type (`[LFoo;` → `Foo`, `[I` → `Int`,
/// `[[I` → `[I`) — what `aaload` pushes. Non-arrays fall back to `Object`.
fn component_type(array: &VType) -> VType {
    match array {
        VType::Reference(name) if name.starts_with('[') => parse_one(name[1..].as_bytes(), 0).0,
        _ => VType::Reference("java/lang/Object".to_string()),
    }
}

/// Replaces every occurrence of the type `from` (an uninitialised object) with `to`
/// (its initialised reference) across the operand stack and locals — what running an
/// object's `<init>` does to the verifier's view of it.
fn initialize(state: &mut TypeState, from: &VType, to: &VType) {
    for slot in state.stack.iter_mut().chain(state.locals.iter_mut()) {
        if slot == from {
            *slot = to.clone();
        }
    }
}

/// The array reference type for a `newarray` `atype` code (`10` → `"[I"`, …).
fn primitive_array_type(atype: u8) -> &'static str {
    match atype {
        4 => "[Z",  // boolean
        5 => "[C",  // char
        6 => "[F",  // float
        7 => "[D",  // double
        8 => "[B",  // byte
        9 => "[S",  // short
        10 => "[I", // int
        11 => "[J", // long
        _ => "[I",
    }
}

/// The local-variable slot an `iload`/`istore`/`aload`/`astore` touches: the compact
/// `_0..3` forms encode it in the opcode (`base..base+3`), the generic form (`wide`)
/// reads it from the following byte.
fn slot_of(bytes: &[u8], pc: usize, base: u8, wide: u8) -> usize {
    let op = bytes[pc];
    if op == wide {
        bytes[pc + 1] as usize
    } else {
        (op - base) as usize
    }
}

/// The absolute target of a 2-byte-offset branch at `pc` (offset is relative to the
/// branch's own pc).
fn branch_target(bytes: &[u8], pc: usize) -> usize {
    let offset = i16::from_be_bytes([bytes[pc + 1], bytes[pc + 2]]);
    (pc as i64 + offset as i64) as usize
}

/// Checks the current state is assignable to the stack-map frame at a branch target.
/// A **backward** branch (target at or before this instruction — a loop's back-edge)
/// additionally must not carry any uninitialised object across it (JVMS §4.10.2.4).
fn check_branch(
    state: &TypeState,
    target: usize,
    frames: &HashMap<usize, TypeState>,
    metaspace: &mut MetaspaceService,
    method: &str,
    pc: usize,
) -> Result<(), VerifyError> {
    if target <= pc {
        assert_no_uninitialized(state, method, pc, "backward branch")?;
    }
    match frames.get(&target) {
        Some(frame) => assignable_state(state, frame, metaspace, method, pc),
        None => Err(err(method, pc, format!("branch target {target} has no stack-map frame"))),
    }
}

/// Whether `from` (the flowed-in state) is assignable to `to` (a stack-map frame):
/// the stacks must have equal depth with each slot assignable, and each local the
/// frame constrains must be assignable from the current one.
fn assignable_state(
    from: &TypeState,
    to: &TypeState,
    metaspace: &mut MetaspaceService,
    method: &str,
    pc: usize,
) -> Result<(), VerifyError> {
    if from.stack.len() != to.stack.len() {
        return Err(err(method, pc, "stack height mismatch at a merge point"));
    }
    for (a, b) in from.stack.iter().zip(&to.stack) {
        if !a.is_assignable_to(b, metaspace) {
            return Err(err(method, pc, format!("stack slot {a:?} not assignable to {b:?}")));
        }
    }
    for (slot, expected) in to.locals.iter().enumerate() {
        let actual = from.locals.get(slot).unwrap_or(&VType::Top);
        if !actual.is_assignable_to(expected, metaspace) {
            return Err(err(method, pc, format!("local {slot}: {actual:?} not assignable to {expected:?}")));
        }
    }
    Ok(())
}

/// Adopts a stack-map frame as the current state: its stack, and its locals padded
/// to `max_locals` with `Top`.
fn adopt(frame: &TypeState, max_locals: usize) -> TypeState {
    let mut locals = frame.locals.clone();
    locals.resize(max_locals, VType::Top);
    TypeState { stack: frame.stack.clone(), locals }
}

/// Finds and parses a method's `StackMapTable` attribute, if present.
fn stack_map_of(class: &ClassFile, code: &Code) -> Option<StackMapTable> {
    code.attributes
        .iter()
        .find(|attr| class.utf8(attr.name_index) == Some("StackMapTable"))
        .and_then(|attr| stack_map_table::parse(&attr.info).ok())
}

/// The parameter types of a method `descriptor` (`(II)I` → `[Int, Int]`), as
/// verification types. `long`/`double`/`float` collapse to `Top` (not modelled).
fn parse_params(descriptor: &str) -> Vec<VType> {
    let bytes = descriptor.as_bytes();
    let mut i = 1; // skip '('
    let mut params = Vec::new();
    while i < bytes.len() && bytes[i] != b')' {
        let (ty, next) = parse_one(bytes, i);
        params.push(ty);
        i = next;
    }
    params
}

/// The return type of a method `descriptor`, or `None` for `void`.
fn return_type(descriptor: &str) -> Option<VType> {
    let close = descriptor.find(')')?;
    let ret = &descriptor[close + 1..];
    if ret == "V" {
        return None;
    }
    Some(parse_one(ret.as_bytes(), 0).0)
}

/// Parses one field-type descriptor starting at `i`, returning its `VType` and the
/// index just past it. Arrays keep their whole descriptor as the reference name.
fn parse_one(bytes: &[u8], start: usize) -> (VType, usize) {
    let mut i = start;
    while i < bytes.len() && bytes[i] == b'[' {
        i += 1;
    }
    let is_array = i > start;
    match bytes.get(i) {
        Some(b'L') => {
            let end = bytes[i..].iter().position(|&c| c == b';').map_or(bytes.len(), |p| i + p + 1);
            let name = if is_array {
                std::str::from_utf8(&bytes[start..end]).unwrap_or("?")
            } else {
                std::str::from_utf8(&bytes[i + 1..end - 1]).unwrap_or("?")
            };
            (VType::Reference(name.to_string()), end)
        }
        Some(_) if is_array => {
            let end = i + 1;
            (VType::Reference(std::str::from_utf8(&bytes[start..end]).unwrap_or("?").to_string()), end)
        }
        // Primitive scalar: int-category verifies as int; `long`/`double`/`float`
        // are their own types.
        Some(b'I' | b'B' | b'C' | b'S' | b'Z') => (VType::Int, i + 1),
        Some(b'J') => (VType::Long, i + 1),
        Some(b'D') => (VType::Double, i + 1),
        Some(b'F') => (VType::Float, i + 1),
        _ => (VType::Top, i + 1),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    fn reference(name: &str) -> VType {
        VType::Reference(name.to_string())
    }

    #[test]
    fn assignability_lattice() {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        // int ↔ int, and anything fits the unconstrained Top.
        assert!(VType::Int.is_assignable_to(&VType::Int, &mut ms));
        assert!(reference("Dog").is_assignable_to(&VType::Top, &mut ms));
        // null fits any reference type.
        assert!(VType::Null.is_assignable_to(&reference("Dog"), &mut ms));
        // a subclass is assignable to its superclass, not the other way round.
        assert!(reference("Dog").is_assignable_to(&reference("Animal"), &mut ms));
        assert!(!reference("Animal").is_assignable_to(&reference("Dog"), &mut ms));
        // an int is not a reference.
        assert!(!VType::Int.is_assignable_to(&reference("Dog"), &mut ms));
    }

    #[test]
    fn array_covariance() {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        // `Dog[] ⊑ Animal[]` (covariant components), and every array ⊑ Object…
        assert!(reference("[LDog;").is_assignable_to(&reference("[LAnimal;"), &mut ms));
        assert!(reference("[LDog;").is_assignable_to(&reference("java/lang/Object"), &mut ms));
        // …but not the other way, and not across unrelated/primitive element types.
        assert!(!reference("[LAnimal;").is_assignable_to(&reference("[LDog;"), &mut ms));
        assert!(!reference("[I").is_assignable_to(&reference("[J"), &mut ms));
        // nested arrays follow the same rule.
        assert!(reference("[[LDog;").is_assignable_to(&reference("[[LAnimal;"), &mut ms));
    }

    #[test]
    fn strict_local_load_checks_the_type() {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let mut state = TypeState::new(2);
        state.locals[0] = reference("Dog"); // a reference local
        state.locals[1] = VType::Int; // an int local

        // `iload 0` over a *reference* local is a type error…
        assert!(load_local(&mut state, 0, &VType::Int, &mut ms, "m", 0).is_err());
        // `lload 1` over an *int* local is a type error…
        assert!(load_local(&mut state, 1, &VType::Long, &mut ms, "m", 0).is_err());
        // …but `iload 1` over the int local is fine and pushes an int.
        assert!(load_local(&mut state, 1, &VType::Int, &mut ms, "m", 0).is_ok());
        assert_eq!(state.stack, vec![VType::Int]);
    }

    #[test]
    fn join_least_upper_bound() {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        // identical → itself; Null is the bottom of the references.
        assert_eq!(reference("Dog").join(&reference("Dog"), &mut ms), reference("Dog"));
        assert_eq!(VType::Null.join(&reference("Dog"), &mut ms), reference("Dog"));
        // a class and its superclass join to the superclass; with Object → Object.
        assert_eq!(reference("Dog").join(&reference("Animal"), &mut ms), reference("Animal"));
        assert_eq!(
            reference("Dog").join(&reference("java/lang/Object"), &mut ms),
            reference("java/lang/Object")
        );
        // an int and a reference have no common type → Top.
        assert_eq!(VType::Int.join(&reference("Dog"), &mut ms), VType::Top);
    }

    fn verify(class_file: &str, method: &str) -> Result<(), VerifyError> {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path(class_file).expect("load class");
        let member = class
            .methods
            .iter()
            .find(|m| class.utf8(m.name_index) == Some(method))
            .expect("method not found");
        verify_method(&mut ms, &class, member)
    }

    /// Verifies a method through the legacy **inference** fixpoint, ignoring any
    /// StackMapTable — the only way to exercise that path here, since the toolchain
    /// always emits frames for branching methods.
    fn verify_inferred(class_file: &str, method: &str) -> Result<(), VerifyError> {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path(class_file).expect("load class");
        let member = class
            .methods
            .iter()
            .find(|m| class.utf8(m.name_index) == Some(method))
            .expect("method not found");
        verify_method_impl(&mut ms, &class, member, true)
    }

    #[test]
    fn verifies_a_linear_method() {
        // Add.add: iload_0, iload_1, iadd, ireturn — no branches, no StackMapTable.
        verify("java/Add.class", "add").expect("Add.add should verify");
    }

    #[test]
    fn verifies_a_branching_method() {
        // Recursion.fact: recursion + a base-case branch ⇒ has a StackMapTable, so
        // this exercises decode + the merge/branch frame checks.
        verify("java/Recursion.class", "fact").expect("Recursion.fact should verify");
    }

    #[test]
    fn verifies_objects_and_fields() {
        // Garbage.run: new/dup/invokespecial <init>, astore/aload, getfield, ireturn —
        // the object core, with the uninitialised → initialised transition.
        verify("java/Garbage.class", "run").expect("Garbage.run should verify");
    }

    #[test]
    fn verifies_arrays() {
        // Arr.run: anewarray, aastore, aaload, getfield — the reference-array path.
        verify("java/Arr.class", "run").expect("Arr.run should verify");
    }

    #[test]
    fn verifies_virtual_calls_and_branches() {
        // Zoo.run: invokevirtual (getClass/isInstance/String.equals/charAt), ldc String
        // literals, and `if` branches with a StackMapTable — the full mix.
        verify("java/Zoo.class", "run").expect("Zoo.run should verify");
    }

    #[test]
    fn verifies_long_arithmetic() {
        // Lng.run: ldc2_w / lstore / lload / ladd / lreturn — the category-2 `long`
        // path (the first non-int primitive in the value model).
        verify("java/Lng.class", "run").expect("Lng.run should verify");
    }

    #[test]
    fn verifies_long_field() {
        // LongField.run: an 8-byte `long` field plus an `int` field after it — the
        // width-aware object layout (putfield/getfield of a category-2 field).
        verify("java/LongField.class", "run").expect("LongField.run should verify");
    }

    #[test]
    fn verifies_double_arithmetic() {
        // Doub.run: ldc2_w / dstore / dload / dadd / dreturn — the `double` path.
        verify("java/Doub.class", "run").expect("Doub.run should verify");
    }

    #[test]
    fn verifies_double_field() {
        // DoubField.run: an 8-byte `double` field (putfield/getfield, f64 bits).
        verify("java/DoubField.class", "run").expect("DoubField.run should verify");
    }

    #[test]
    fn verifies_long_parameters() {
        // LAdd.add(long, long): category-2 parameters — `a` at local 0, `b` at 2 —
        // so the initial locals must be laid out with the high-half slots.
        verify("java/LAdd.class", "add").expect("LAdd.add should verify");
        verify("java/LAdd.class", "run").expect("LAdd.run should verify");
        // Adder.plus: the same, on an instance method (`this` at 0, then the longs).
        verify("java/Adder.class", "plus").expect("Adder.plus should verify");
    }

    #[test]
    fn verifies_float() {
        // Category-1 `float`: arithmetic (fconst/fadd/freturn via ldc), a 4-byte
        // field, and parameters (no high-half gap — `a` at 0, `b` at 1).
        verify("java/Flt.class", "run").expect("Flt.run should verify");
        verify("java/FloatField.class", "run").expect("FloatField.run should verify");
        verify("java/FAdd.class", "add").expect("FAdd.add should verify");
    }

    #[test]
    fn verifies_numeric_conversions() {
        // Conv: i2l..i2s across the int/long/float/double types — each pops the
        // source type and pushes the target, so the stack typing must track them.
        for method in ["widen", "narrow", "mixed", "roundtrip", "chars", "rest"] {
            verify("java/Conv.class", method).unwrap_or_else(|e| panic!("Conv.{method}: {e:?}"));
        }
    }

    #[test]
    fn verifies_stack_manipulation() {
        // Stk: pop (discarded return), dup (chained assign), dup2 (compound assign on
        // an int array and on a long array — the category-2 form).
        for method in ["withPop", "chainAssign", "compound", "longCompound"] {
            verify("java/Stk.class", method).unwrap_or_else(|e| panic!("Stk.{method}: {e:?}"));
        }
    }

    #[test]
    fn verifies_array_covariance() {
        // ArrCov.run: passes a `Dog[]` where an `Animal[]` is expected — accepted only
        // because the assignability check now knows array covariance.
        verify("java/ArrCov.class", "run").expect("ArrCov.run should verify");
        verify("java/ArrCov.class", "firstSound").expect("ArrCov.firstSound should verify");
    }

    #[test]
    fn verifies_category2_statics_and_arrays() {
        // Statics: long/double/float static fields (width-aware mirror layout).
        verify("java/Statics.class", "run").expect("Statics.run should verify");
        // Arrays2: new long[]/double[]/float[] with the typed load/store opcodes.
        for method in ["longArr", "dblArr", "fltArr"] {
            verify("java/Arrays2.class", method)
                .unwrap_or_else(|e| panic!("Arrays2.{method}: {e:?}"));
        }
    }

    #[test]
    fn verifies_arithmetic() {
        // Arith: div/rem/neg/shift/bitwise across int/long/double, `iinc` + the
        // `if_icmp*` loop, and a try/catch around a divide-by-zero.
        for method in ["intMix", "longMix", "dblMix", "loop", "divZero"] {
            verify("java/Arith.class", method).unwrap_or_else(|e| panic!("Arith.{method}: {e:?}"));
        }
    }

    #[test]
    fn verifies_comparisons_with_category2_branches() {
        // Cmp: lcmp/fcmp/dcmp feeding iflt/ifge, with `long`/`double` locals across
        // branches — exercises the category-2 expansion in `decode` (incl. the
        // back-edge frame in the `sumWhile` loop).
        for method in ["longLess", "dmax", "nanLess", "sumWhile"] {
            verify("java/Cmp.class", method).unwrap_or_else(|e| panic!("Cmp.{method}: {e:?}"));
        }
    }

    #[test]
    fn verifies_exception_handlers() {
        // Exc: an explicit `throw` (athrow) caught by a *supertype* handler, a method
        // returning a reference (areturn), and one try with two typed handlers —
        // exercises the exception-table validation and the athrow/areturn type checks.
        for method in ["thrown", "pick", "classify", "run"] {
            verify("java/Exc.class", method).unwrap_or_else(|e| panic!("Exc.{method}: {e:?}"));
        }
    }

    #[test]
    fn verifies_switch() {
        // tableswitch (dense) and lookupswitch (sparse): pop the int key, branch to the
        // default or a case. Checked through both drivers (frames + forced inference).
        for m in ["dense", "sparse"] {
            verify("java/Switch.class", m).unwrap_or_else(|e| panic!("Switch.{m}: {e:?}"));
            verify_inferred("java/Switch.class", m).unwrap_or_else(|e| panic!("Switch.{m} (inferred): {e:?}"));
        }
    }

    #[test]
    fn cross_check_catches_a_contradictory_frame() {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let at = |pc: usize, stack: Vec<VType>| {
            let mut m = HashMap::new();
            m.insert(pc, TypeState { stack, locals: Vec::new() });
            m
        };
        // Inference found a single int on the stack at pc 4.
        let inferred = at(4, vec![VType::Int]);
        // A frame that agrees passes; one claiming a reference there contradicts it.
        assert!(cross_check_stackmap(&at(4, vec![VType::Int]), &inferred, &mut ms, "m").is_ok());
        assert!(cross_check_stackmap(&at(4, vec![reference("Dog")]), &inferred, &mut ms, "m").is_err());
        // A stack-height mismatch is a contradiction too.
        assert!(cross_check_stackmap(&at(4, vec![VType::Int, VType::Int]), &inferred, &mut ms, "m").is_err());
        // A frame at a pc inference never reached is simply skipped.
        assert!(cross_check_stackmap(&at(99, vec![reference("Dog")]), &inferred, &mut ms, "m").is_ok());
    }

    #[test]
    fn inference_verifies_branching_methods_without_frames() {
        // Drive the legacy inference fixpoint (JVMS §4.10.2) over real branching
        // bytecode, ignoring its StackMapTable: a base-case branch + recursion
        // (Recursion.fact), back-edge loops with an int and a category-2 accumulator
        // (Arith.loop, Cmp.sumWhile), reference/virtual-call merges (Zoo.run), and a
        // try/catch whose handler the fixpoint must seed (Arith.divZero).
        verify_inferred("java/Recursion.class", "fact").expect("Recursion.fact (inferred)");
        verify_inferred("java/Arith.class", "loop").expect("Arith.loop (inferred)");
        verify_inferred("java/Cmp.class", "sumWhile").expect("Cmp.sumWhile (inferred)");
        verify_inferred("java/Zoo.class", "run").expect("Zoo.run (inferred)");
        verify_inferred("java/Arith.class", "divZero").expect("Arith.divZero (inferred)");
        // The inference path agrees with the StackMapTable path on a linear method too.
        verify_inferred("java/Add.class", "add").expect("Add.add (inferred)");
    }

    #[test]
    fn access_rules_special_names_and_invokeinterface_count() {
        // A real invokeinterface call site (List.iterator/Iterator.hasNext/next, each
        // count 1) verifies through the count check.
        verify("java/LocalVars.class", "g").expect("LocalVars.g should verify");
        // `<init>`/`<clinit>` may not be invokevirtual/static/interface targets.
        assert!(reject_special_name("<init>", "invoke", "m", 0).is_err());
        assert!(reject_special_name("<clinit>", "invokestatic", "m", 0).is_err());
        assert!(reject_special_name("size", "invoke", "m", 0).is_ok());
        // The count must equal the argument slots (receiver + params) and be followed
        // by a zero byte. `b9 _ _ <count> <zero>`.
        assert!(check_invokeinterface_count(&[0xb9, 0, 9, 1, 0], 0, "()V", "m").is_ok());
        assert!(check_invokeinterface_count(&[0xb9, 0, 9, 2, 0], 0, "()V", "m").is_err()); // wrong count
        assert!(check_invokeinterface_count(&[0xb9, 0, 9, 1, 7], 0, "()V", "m").is_err()); // nonzero 4th byte
        // A `long` argument is two slots → count for `(J)V` is 1 + 2 = 3.
        assert!(check_invokeinterface_count(&[0xb9, 0, 9, 3, 0], 0, "(J)V", "m").is_ok());
    }

    #[test]
    fn protected_access_rule_across_packages() {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        // Sub (default package) extends pkg.Base and reads the inherited protected `x`
        // through `this` (a Sub) — the legal cross-package form.
        verify("java/Sub.class", "get").expect("Sub.get should verify");
        // The decision predicate directly: pkg.Base.x is reachable on a Sub receiver,
        // but not on a bare pkg.Base (not a subtype of the accessing class Sub).
        let on_sub = VType::Reference("Sub".to_string());
        let on_base = VType::Reference("pkg/Base".to_string());
        assert!(check_protected_access(&mut ms, "Sub", "pkg/Base", "x", "I", true, &on_sub, "m", 0).is_ok());
        assert!(check_protected_access(&mut ms, "Sub", "pkg/Base", "x", "I", true, &on_base, "m", 0).is_err());
        // Same-package access (Base to its own protected field) imposes no constraint.
        assert!(check_protected_access(&mut ms, "pkg/Base", "pkg/Base", "x", "I", true, &on_base, "m", 0).is_ok());
    }

    #[test]
    fn structural_check_accepts_real_methods() {
        // Real javac output is structurally sound: a back-edge loop (Cmp.sumWhile), a
        // try/catch with an exception table (Arith.divZero), and an object/branch mix.
        for (file, method) in [("java/Cmp.class", "sumWhile"), ("java/Arith.class", "divZero"), ("java/Zoo.class", "run")] {
            let class = ClassFile::from_path(file).expect("load class");
            let member = class.methods.iter().find(|m| class.utf8(m.name_index) == Some(method)).expect("method");
            let code = class.member_code(member).expect("has code");
            structural_check(&code, method).unwrap_or_else(|e| panic!("{file}::{method}: {e:?}"));
        }
    }

    #[test]
    fn structural_check_rejects_malformed_code() {
        use crate::jvm::parser::code::{Code, ExceptionTableEntry};
        let code = |bytes: Vec<u8>, table: Vec<ExceptionTableEntry>| Code {
            max_stack: 8,
            max_locals: 8,
            code: bytes,
            exception_table: table,
            attributes: Vec::new(),
        };
        // A minimal well-formed method (iconst_0, ireturn) passes.
        assert!(structural_check(&code(vec![0x03, 0xac], vec![]), "m").is_ok());
        // Empty code, and code that falls off the end (no return after iconst_0).
        assert!(structural_check(&code(vec![], vec![]), "m").is_err());
        assert!(structural_check(&code(vec![0x03], vec![]), "m").is_err());
        // A truncated final instruction (goto with one offset byte missing).
        assert!(structural_check(&code(vec![0xa7, 0x00], vec![]), "m").is_err());
        // goto into the middle of itself (offset 2) is rejected; to a boundary (the
        // return at 3) is fine.
        assert!(structural_check(&code(vec![0xa7, 0x00, 0x02, 0xb1], vec![]), "m").is_err());
        assert!(structural_check(&code(vec![0xa7, 0x00, 0x03, 0xb1], vec![]), "m").is_ok());
        // Subroutines (jsr) are rejected outright.
        assert!(structural_check(&code(vec![0xa8, 0x00, 0x03, 0xb1], vec![]), "m").is_err());
        // An exception range whose start is inside an instruction (offset 1 is the
        // middle of `bipush 5`, whose boundaries are {0, 2}).
        let bad = ExceptionTableEntry { start_pc: 1, end_pc: 2, handler_pc: 0, catch_type: 0 };
        assert!(structural_check(&code(vec![0x10, 0x05, 0xac], vec![bad]), "m").is_err());
    }

    #[test]
    fn switch_targets_decode_default_and_cases() {
        // A `tableswitch` at pc 0 (3 padding bytes), low=1 high=2 → default + 2 cases,
        // with offsets 12/13/14 → absolute targets 12/13/14.
        let mut bytes = vec![0xaa, 0x00, 0x00, 0x00]; // opcode + 3 padding
        for word in [12i32, 1, 2, 13, 14] {
            bytes.extend_from_slice(&word.to_be_bytes()); // default, low, high, t0, t1
        }
        let mut got = branch_targets(&bytes, 0);
        got.sort();
        assert_eq!(got, vec![12, 13, 14]);
    }

    #[test]
    fn join_states_merges_by_lattice_and_rejects_height_mismatch() {
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        // Two paths reaching a merge: one carries a Dog, the other an Animal. The
        // join is their common supertype (Animal), slot by slot.
        let a = TypeState { stack: vec![reference("Dog")], locals: vec![VType::Int, reference("Dog")] };
        let b = TypeState { stack: vec![reference("Animal")], locals: vec![VType::Int, reference("Animal")] };
        let merged = join_states(&a, &b, 2, &mut ms, "m", 0).expect("joinable");
        assert_eq!(merged.stack, vec![reference("Animal")]);
        assert_eq!(merged.locals, vec![VType::Int, reference("Animal")]);

        // Stacks of different heights describe incompatible paths → not mergeable.
        let tall = TypeState { stack: vec![VType::Int, VType::Int], locals: Vec::new() };
        let short = TypeState { stack: vec![VType::Int], locals: Vec::new() };
        assert!(join_states(&tall, &short, 0, &mut ms, "m", 0).is_err());
    }

    #[test]
    fn verifies_constructors_with_uninitialized_this() {
        // A constructor now starts with `this` as UninitializedThis; the implicit
        // `super()` (invokespecial Object.<init>) initialises it, and only then is it
        // used (putfield this.a/this.b). The `return` confirms `this` was initialised.
        verify("java/Init.class", "<init>").expect("Init.<init> should verify");
        verify("java/Garbage.class", "<init>").expect("Garbage.<init> should verify");
        verify("java/Init.class", "run").expect("Init.run should verify");
    }

    #[test]
    fn use_reference_rejects_uninitialized_objects() {
        let mut state = TypeState::new(0);
        // An uninitialised object (`new` not yet `<init>`-ed) can't be *used*…
        state.stack = vec![VType::Uninitialized(7)];
        assert!(use_reference(&mut state, "m", 0).is_err());
        state.stack = vec![VType::UninitializedThis];
        assert!(use_reference(&mut state, "m", 0).is_err());
        // …nor can a non-reference; but an initialised reference and `null` are fine.
        state.stack = vec![VType::Int];
        assert!(use_reference(&mut state, "m", 0).is_err());
        state.stack = vec![reference("Dog")];
        assert!(use_reference(&mut state, "m", 0).is_ok());
        state.stack = vec![VType::Null];
        assert!(use_reference(&mut state, "m", 0).is_ok());
    }

    #[test]
    fn assert_no_uninitialized_scans_stack_and_locals() {
        let mut state = TypeState::new(2);
        state.locals[0] = reference("Dog");
        state.locals[1] = VType::Int;
        // A clean state passes…
        assert!(assert_no_uninitialized(&state, "m", 0, "ctx").is_ok());
        // …an uninitialised object in a local fails (the constructor-return case)…
        state.locals[0] = VType::UninitializedThis;
        assert!(assert_no_uninitialized(&state, "m", 0, "ctx").is_err());
        // …and one left on the stack fails too (the backward-branch case).
        state.locals[0] = reference("Dog");
        state.stack = vec![VType::Uninitialized(3)];
        assert!(assert_no_uninitialized(&state, "m", 0, "ctx").is_err());
    }

    #[test]
    fn exception_table_rejects_non_throwable_and_bad_handler_stack() {
        use crate::jvm::parser::code::{Code, ExceptionTableEntry};
        let mut ms = MetaspaceService::new(vec![PathBuf::from("boot")], vec![PathBuf::from("java")]);
        let class = ClassFile::from_path("java/Exc.class").expect("load Exc");

        let entry = |handler: u16, catch_type: u16| ExceptionTableEntry {
            start_pc: 0,
            end_pc: 1,
            handler_pc: handler,
            catch_type,
        };
        let code = |table: Vec<ExceptionTableEntry>| Code {
            max_stack: 1,
            max_locals: 1,
            code: Vec::new(),
            exception_table: table,
            attributes: Vec::new(),
        };
        let no_frames = HashMap::new();

        // A `catch_type` pointing at `java/lang/Object` (the superclass) is not a
        // Throwable → rejected.
        let object_idx = class.super_class;
        assert_eq!(class.class_name(object_idx), Some("java/lang/Object"));
        assert!(verify_exception_table(&code(vec![entry(0, object_idx)]), &class, &no_frames, &mut ms, "m").is_err());

        // A bare `finally` (catch_type 0) with no frame to check is accepted.
        assert!(verify_exception_table(&code(vec![entry(0, 0)]), &class, &no_frames, &mut ms, "m").is_ok());

        // A handler whose frame enters with two operands (not just the exception) is
        // rejected — the JVM enters a handler with exactly the exception on the stack.
        let mut frames = HashMap::new();
        frames.insert(99, TypeState { stack: vec![throwable(), VType::Int], locals: Vec::new() });
        assert!(verify_exception_table(&code(vec![entry(99, 0)]), &class, &frames, &mut ms, "m").is_err());
    }
}
