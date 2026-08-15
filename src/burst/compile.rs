//! The **bytecode → native compiler**: milestone F3, step 2.
//!
//! Step 1 built the machine that writes x86-64 ([`x64`][crate::burst::x64]) and the W^X pages to
//! run it from ([`exec_mem`][crate::burst::exec_mem]). This module is the part that decides *what*
//! to write: it takes a method's `code[]` and produces either machine code or a reason the method
//! is not eligible.
//!
//! # The subset, and why it is drawn there
//!
//! Only methods whose **entire** body lies in this whitelist are compiled:
//!
//! | group | opcodes |
//! |---|---|
//! | constants | `iconst_m1`…`iconst_5`, `bipush`, `sipush`, `ldc`/`ldc_w` **of an `Integer`**, `aconst_null` |
//! | locals | `iload`/`aload`, `iload_0..3`/`aload_0..3`, `istore`/`astore`, `istore_0..3`/`astore_0..3`, `iinc`, and all five under `wide` |
//! | arithmetic | `iadd`, `isub`, `imul`, `idiv`, `irem`, `ineg` |
//! | bits & shifts | `iand`, `ior`, `ixor`, `ishl`, `ishr`, `iushr` |
//! | control flow | `if_icmp<cond>`, `if<cond>`, `if_acmpeq`, `if_acmpne`, `ifnull`, `ifnonnull`, `goto`, `tableswitch`, `lookupswitch` |
//! | stack | `nop`, `pop`, `pop2`, `dup`, `dup_x1`, `dup_x2`, `dup2`, `dup2_x1`, `dup2_x2`, `swap` |
//! | heap (read) | `getstatic` **of an `int`, in an already-initialised class**; `getfield` **of a non-`volatile` `int`**; `arraylength`; `iaload` |
//! | heap (write) | `putstatic` and `putfield` under the same conditions as their reading twins; `iastore` |
//! | allocation | `new` **of an already-initialised class**, on Eden's fast path only |
//! | exits | `ireturn`, `areturn`, `return` |
//!
//! Anything else — one single byte — makes the method permanently ineligible. Until step 6 the
//! safety argument was one sentence: such a method **writes nothing observable**, so an abandoned
//! attempt could simply be re-run from the start. The three write opcodes are what that sentence
//! cost, and step 6 pays for them with a **deopt that resumes at a pc instead of restarting** — see
//! below. What is still true, and still load-bearing, is that compiled code **calls nothing**: that
//! is what keeps a garbage collection from ever observing a compiled frame — and step 7's `new`,
//! below, is what makes "allocates nothing" stop being the *other* half of that sentence without
//! weakening it.
//!
//! # Step 7: `return`, and an allocation that cannot collect
//!
//! ## `return` (0xb1)
//!
//! A `void` method was not worth compiling while the subset had no observable effects — a pure
//! `void` method is a no-op, and the only honest thing to do with one is nothing. Step 6's writes
//! changed that: a `void` method can now *do* something, and `void` is what `<init>` and every
//! setter return. So the exit is added, and with it the ceiling moves by the largest single step in
//! this milestone — `java.lang.Object.<init>` is literally the one instruction `return`.
//!
//! What it costs the boundary is one bit: an [`Outcome::Returned`] from a `void` method carries no
//! value, and the caller must push nothing rather than push a meaningless one. See
//! [`CompiledCode::returns_void`].
//!
//! ## `new`, and why the fast path is the *whole* design rather than an optimisation
//!
//! Allocation is the one thing that can trigger a garbage collection, and this tier's entire
//! safety argument (below, "Why this needs no stack maps") is that **no collection can happen while
//! a compiled frame exists**. Emitting a general allocation would destroy it. Emitting only the
//! **fast path** preserves it *by construction*:
//!
//! > Compiled code bumps Eden and nothing else. A bump that succeeds cannot have collected. A bump
//! > that fails leaves through [`Status::ALLOC`] — and it is then the **interpreter** that
//! > allocates, and that may collect, with no native frame anywhere on the stack.
//!
//! So the invariant is not weakened, argued around or conditionally suspended. There is simply no
//! emitted instruction that can collect.
//!
//! ### What an allocation is, besides a bump
//!
//! This is the part that had to be established before a single instruction was emitted, because
//! getting it wrong is silent: an object the collector does not know about is not a crash, it is a
//! heap that is quietly wrong. An interpreted Eden allocation
//! (`HeapService::try_malloc` + `objects_operations::try_allocate`) is **four** things:
//!
//!  1. an atomic bump of the arena cursor by the 8-rounded stride, with a bounds check;
//!  2. **zeroing** the block — the fields' JVMS default values;
//!  3. the header `[class_id | mark]`, `class_id` being the class's pinned mirror offset;
//!  4. a push of `Allocation { offset, size, gen: Young, age: 0 }` into the heap's **pending log**,
//!     which is what makes the object visible to `gc::minor` — and an unlogged young object *does*
//!     panic the collector, in the `young_info[&obj]` index of `Minor::evacuate`.
//!
//! The first three are emitted inline. The fourth is a `Mutex<Vec<Allocation>>` push, which no
//! instruction stream can do — so it is **deferred**, not skipped: the compiled `new` writes
//! `(offset, size)` into a fixed array in the caller's buffer ([`CompiledCode::alloc_base`]) and the
//! trampoline replays it through `HeapService::log_jit_allocation` the instant native code returns,
//! on **every** outcome.
//!
//! Deferring it is exactly as sound as doing it inline, and for the same one reason everything else
//! here is: the pending log is drained only by `HeapService::commit_pending`, which is called only
//! from the interpreter's GC entry (`Exec::parked`), and no collection can run while native code is
//! on this thread's stack. The window between the bump and the replay contains no GC *by
//! construction*, so there is no moment at which a collector could see a heap it does not fully
//! know about.
//!
//! The log is finite, so an excursion that fills it leaves the same way a full Eden does — see
//! [`ALLOC_LOG_RECORDS`] and [`Status::ALLOC`].
//!
//! ### What the class has to be
//!
//! **Resolved, laid out, mirrored and `Done`** — the same requirement `getstatic` has, and refused
//! the same way. `new` is a first active use, so an uninitialised class would have to run its
//! `<clinit>`, and compiled code cannot run anything. The size and the header word are resolved at
//! compile time and baked in as immediates; both are properties of the class that never change once
//! it is loaded (a mirror is `malloc_old`ed and pinned against `gc::compact`).
//!
//! ### What it does *not* buy on its own
//!
//! Essentially nothing, and that is worth writing down. `javac` never emits a bare `new`: it emits
//! `new; dup; …; invokespecial <init>`, so a method containing an allocation contains a call, and
//! calls are the next step's. The census says so numerically — adding `new` moved the compiled
//! count by zero and merely folded the `new` refusals into the `invokespecial` ones. This step is
//! the half of `new X(…)` that had a correctness argument to make; the other half is a call.
//!
//! # Step 8: calls, by **inlining** — and the virtual frames that pay for them
//!
//! `invokespecial` alone kept 379 methods of the corpus out of the subset, every other refusal
//! having been consolidated behind it, and step 7 ended by saying why: `javac` never emits a bare
//! `new`, so the inline allocation it built was worth nothing until a call could be expressed.
//!
//! ## Why inlining rather than a native call
//!
//! A real call would need the Microsoft x64 ABI honoured across a boundary the interpreter owns,
//! unwind data registered (`RtlAddFunctionTable` — see [`x64::Frame`][crate::burst::x64::Frame]'s
//! "known gap"), and a way to unwind *out* of native frames when an exception crosses them. Inlining
//! needs none of the three: there is no call instruction, no second machine frame and no return
//! address. What it costs instead is the **deopt**, which now has to give back state for a call
//! chain that never physically existed — and that cost is paid once, here, rather than by every
//! future opcode.
//!
//! ## A compilation is a tree of bodies
//!
//! What used to be "the method" is now the **root** of an inline tree, and each expanded call is a
//! node of it ([`Body`]). The three things a body used to take for granted became explicit, and
//! that refactor is most of the step:
//!
//! | | |
//! |---|---|
//! | its **constant pool** | every resolver in [`Environment`] is now a function of `(`[`Unit`]`, index)` — a callee's `ldc #7` means something else than the caller's |
//! | its **locals** | body `b`'s local `i` is buffer slot `b.locals_base + i`, so `istore` still writes straight through, just at this body's own offset |
//! | its **operand stack** | body `b`'s operand `k` is native slot `b.frame_base + k`, and spills to buffer slot `b.spill_base + k` |
//!
//! Regions are **disjoint** rather than overlaid. Two sibling call sites are never live at once, so
//! they could share, but an overlay is a liveness argument every future opcode would have to be
//! re-checked against while disjointness is a property of the arithmetic and nothing else.
//!
//! A call is then two things and no more: the arguments are copied into the callee's locals and the
//! remaining slots zeroed (the interpreter's `Frame::reset_for_call`, restated in machine code —
//! [`entry_locals`] claims those slots are `Int(0)` and this is what makes the claim true), and the
//! callee's body is emitted in place. Its `return` is not an exit: it writes the value where the
//! invoke's result belongs on the caller's operand stack and jumps past the invoke.
//!
//! ## The bounds, and which one is load-bearing
//!
//! [`MAX_INLINE_DEPTH`] (3), [`MAX_INLINE_BYTES`] (1024) and [`MAX_INLINE_BODIES`] (16) bound the
//! expansion, and a callee containing a **backward branch** is refused outright ([`scan_body`]):
//! only the root's loop headers carry a safepoint poll and only the root can be entered on-stack, so
//! an inlined loop would be one native code could not be pulled out of, and step 3's poll invariant
//! is not something this step may quietly weaken.
//!
//! But the bound that makes **recursion** terminate is none of those. It is the [`Unit`] identity: a
//! callee already on the path from the root is refused ([`Ineligible::InlineCycle`]), which stops
//! `f`→`f` at the first expansion and `f`→`g`→`f` at the second — a mutual recursion no depth bound
//! would recognise and no per-method check would see.
//!
//! ## Virtual frames: the deopt that rebuilds a call chain
//!
//! A guard inside an expanded callee has to hand back the frames inlining removed. The observation
//! that makes it tractable is that **the interpreter already has the convention**: at an invoke the
//! caller's pc is left pointing *at* the invoke and the matching `return` is what advances it past
//! the call (`invokespecial`/`invokestatic` do this so an exception unwinds to the right pc). So a
//! deopt is reconstructed as
//!
//! ```text
//!   caller frame   at the pc of the invoke, its arguments already popped (they are locals below)
//!   callee frame   at the pc of the guard, with its own locals and operand stack
//! ```
//!
//! and when the callee returns, `advance_past_call` steps the caller over the invoke and pushes the
//! result — with the machinery that already existed, and no special case anywhere. Nesting is the
//! same sentence applied at each level ([`ResumeSite::inlined`]).
//!
//! Three details carry it. Each frame's operands stop **short of the arguments** of the call it is
//! in the middle of, or every argument would be pushed twice. A [`VirtualFrame`]'s locals are
//! **complete**, not differential: unlike the root, that frame does not exist yet, so there is no
//! "leave this slot alone" to fall back on. And the low 32 bits of a [`Status::DEOPT`] stop being a
//! pc and become a [`key`][ResumeSite::key] — two guards in one expanded callee share the root's
//! single invoke pc, so an inlined site is numbered past [`MAX_CODE_LEN`], where no bytecode pc can
//! reach and the poll exits and OSR dispatch go on speaking pcs.
//!
//! ## What does *not* change
//!
//! - **The order rule.** Neither half of a call is observable — both write this compilation's own
//!   buffer regions — so the first observable effect inside an expansion is still the callee's own,
//!   still preceded by its own guards. A deopt inside a callee resumes *inside* it, so the invoke is
//!   never re-executed and no effect is applied twice.
//! - **No GC can observe a compiled frame.** Inlining opens no new path: it emits no call, and the
//!   only allocation is still the `new` fast path, whose bump either succeeds or leaves.
//! - **Exception handlers.** A method with a table still gets no on-stack entry points, and that is
//!   still enough when the inlined body comes from a method with a table of its own: native code
//!   throws nothing, so no handler can fire while it runs. One that fires later fires in the frame a
//!   deopt rebuilt — an ordinary interpreter frame for that method, with its own table.
//! - **Frame depth.** Inlining hides the invokes it expanded, and with them the `MAX_FRAMES` check
//!   the interpreter makes at each one. [`CompiledCode::frame_depth`] is what lets the caller refuse
//!   to *enter* when the chain would not fit, which is the last moment refusing is free.
//!
//! # Step 4: what widened the subset, and what each addition had to prove
//!
//! ## `wide` (`iload`/`istore`/`iinc` with a 16-bit index, `iinc` with a 16-bit constant)
//!
//! Purely frame-local — the same three instructions with a wider operand field, and `javac` emits
//! the wide `iinc` for something as ordinary as `x += 256`. Nothing about the purity argument
//! changes; the only new thing to get right is the 16-bit *signed* `iinc` constant, which still
//! goes through the same `add`/`movsxd` pair as the narrow form (trap 1).
//!
//! ## The rest of the stack shuffles
//!
//! `nop`, `pop2` and the five remaining `dup` forms. The operand stack is a **compile-time**
//! notion here (position `k` is frame slot `k`), so these are permutations of slots: `nop`,
//! `pop2` and `pop` emit nothing at all, and the `dup` family emits only `mov`s.
//!
//! Their JVMS definitions are *shape-dependent* — `dup2` duplicates either two category-1 values
//! or one category-2 value, and there is no opcode-level way to tell which. **In this subset the
//! question never arises**: every opcode that pushes pushes an `int`, so every operand-stack slot
//! holds a category-1 value, always. The category-1 reading is therefore not a guess but the only
//! possible one. The moment a category-2 value can reach the operand stack — the `long` step —
//! every one of these six opcodes has to be revisited, which is exactly why the guarantee is
//! written down here rather than assumed.
//!
//! ## `tableswitch` / `lookupswitch`
//!
//! Still perfectly pure — a multi-way `goto` on an `int`. Three details do the work: the 0–3 bytes
//! of **padding** that align the operand table to a 4-byte boundary *of the code array*, the
//! branch offsets being 4 bytes and relative to the **opcode** (not to the table), and the
//! `default` arm, which is a branch target like any other and must be walked by the scan.
//!
//! Both compile to a **compare chain** (`cmp`/`je` per case, then `jmp default`) rather than to a
//! jump table. A table would need a rip-relative data island inside the code page, and this
//! assembler deliberately has no encoding for one — the same reason the OSR entry dispatch is a
//! compare chain. So the chain is O(cases) where a table is O(1); the answer for now is
//! correctness plus a cap ([`MAX_SWITCH_CASES`]) that keeps the emitted chain bounded, and a real
//! jump table is a later step with an assembler change in front of it.
//!
//! ## `getstatic` of an `int` — a heap read that does not cost the purity argument
//!
//! A `getstatic` is a **read**. Re-executing the method re-reads the same location and gets the
//! same value, because in `green`/`os-gil` — the only substrates where the JIT is on — no other
//! thread runs a single opcode while a native frame is on this thread's stack. So deopt-by-restart
//! stays valid, and so does the OSR contract.
//!
//! What it does need is an **address that cannot move**, since the compiler bakes one in as an
//! immediate. Two facts make that true, and both are properties of code elsewhere rather than of
//! this module:
//!
//! 1. A class's statics live in its `Class<…>` mirror, which is allocated with `malloc_old` and is
//!    in the **pinned set** that `gc::compact` refuses to relocate. So the mirror's *heap offset*
//!    is fixed for the life of the VM.
//! 2. The heap's byte region is a `Vec` **pre-reserved to the maximum heap size at startup**, so
//!    it never reallocates while growing and every offset's *address* is stable too.
//!
//! And it needs the class to be **initialised**: the resolver is only allowed to answer with an
//! address once the declaring class's `<clinit>` has run, because compiled code has no way to
//! trigger one. A method reading a static of a class that is not yet initialised simply does not
//! compile — and since ineligibility is cached forever, that is a deliberate loss (the method may
//! well be compilable later) traded for not putting a class-init barrier in native code.
//!
//! The load is `movsxd r, dword [addr]`: a static `int` occupies 4 bytes in the mirror (the
//! interpreter's `putstatic` writes it with `write_u32`), and sign-extending it on the way in is
//! what re-establishes the normalisation invariant below.
//!
//! ## `putstatic` — left out *here*, and let in by step 6
//!
//! A `putstatic` is the first **observable side effect** the subset would have contained, and it
//! breaks deopt-by-restart outright: a method that stores and *then* meets a zero divisor would,
//! on re-execution, apply the store twice. Two ways out were weighed at step 4.
//!
//! *Compile it only in methods with no restart-style deopt site* (then: no `idiv`/`irem`) is sound
//! and easy to check, but it buys very little and costs the one-line invariant that makes this tier
//! reviewable — "compiled code writes nothing observable" would have become "…*unless* it cannot
//! deopt", and every future guard would have had to re-derive its interaction with that exception.
//!
//! *Turn those deopts into resume-at-pc* was the other, and it was not available then: the resume
//! mechanism the safepoint poll uses requires an **empty operand stack**, and a zero divisor happens
//! mid-expression with operands live. That is exactly what step 6 built, and it is why the writes
//! arrive as a consequence of the deopt work rather than as a widening of their own.
//!
//! ## `long` arithmetic — out, and blocked on the calling convention rather than on effort
//!
//! `lload`/`ladd`/`lshl`/… map beautifully onto x86-64: they are native 64-bit operations, they
//! need no normalisation (the whole register *is* the value), and x86 masks a 64-bit shift count
//! to 6 bits exactly as JLS §15.19 does for `long`. The work is in the slot mapping — a `long` is
//! **category 2**, so it occupies two local slots and two operand-stack slots, which every index
//! calculation in both passes would have to account for, and it is also what makes the six `dup`
//! forms above ambiguous again.
//!
//! But none of that is what stops it. **`lreturn` does not fit the return protocol.** The packed
//! `RAX = (status << 32) | value` below spends the high half on the status, leaving 32 bits for
//! the result — enough for every `int`, and not enough for any `long`. Adding `long` therefore
//! means changing the boundary itself (a second return register, or an out-pointer, and with it
//! the marshalling contract, the OSR write-back and every `unsafe` block that crosses it), which
//! is a step of its own rather than a widening of this one.
//!
//! ## Constant-folded shift counts — out, for now
//!
//! `x >> 7` currently materialises the 7 into a stack slot, loads it into `CL` and masks it, where
//! `sar r, 7` would do. The peephole itself is easy and safe (the count must be pushed by the
//! immediately preceding instruction, and that pc must not be a branch target — and a shift can
//! never be an OSR entry, since those have an empty operand stack). What it needs is a
//! **shift-by-immediate encoding in the assembler**, which does not exist yet; and the win is two
//! instructions against an operand stack that lives entirely in memory, which is the thing
//! register allocation is about to change anyway. Better done on top of that than under it.
//!
//! `ldc`/`ldc_w` restricted to `CONSTANT_Integer` is the one addition to the brief's list, and it
//! is not a widening of the safety argument: the constant is read from the pool **at compile time**
//! and baked into the instruction stream as an immediate, so the generated code never touches the
//! constant pool at all. Without it the subset cannot express an `int` outside `[-32768, 32767]`,
//! which excludes essentially every real loop bound — `BmLoop`'s own `i < 900000` compiles to
//! `ldc #7`. A `ldc` naming anything else (a `String`, a `float`, a class literal) is rejected like
//! any other unsupported opcode.
//!
//! # Step 5: references
//!
//! Until here every value this tier could touch was an `int`, and that is what made the boundary
//! trivial: `Value::Int(v)` marshals as `v as i64` and anything else abandons the call. The cost
//! was `aload_0` — **every instance method and every constructor** begins with one, which is 354 of
//! the corpus's 710 methods refused at their first byte.
//!
//! What the subset gains: `aconst_null`; `aload`/`aload_0..3`, `astore`/`astore_0..3` and their
//! `wide` forms; the stack shuffles, now over references as well; `ifnull`/`ifnonnull` and
//! `if_acmpeq`/`if_acmpne`; `getfield` of an `int`; `arraylength` and `iaload`; and `areturn`,
//! which is the first exit besides `ireturn` and therefore the first widening of the *ceiling*
//! rather than of the subset.
//!
//! What it deliberately did not gain, and step 6 did: **`putfield` and `iastore`**. They are
//! observable side effects, and they broke deopt-by-restart for exactly the reason `putstatic` does
//! — a method that stores and then meets a null receiver would, on re-execution, apply the store
//! twice. Reads were different in kind, not in degree: re-executing a read re-reads the same bytes,
//! because no other thread runs a single opcode while native code is on this stack (below).
//!
//! ## Why this needs no stack maps for the collector
//!
//! A compiled frame holds references in native stack slots and in registers, and no GC on earth can
//! find them there. The reason that is safe is not a map — it is that **no collection can happen
//! while a compiled frame exists**:
//!
//!  - compiled code **cannot collect**. Until step 7 that was "allocates nothing"; now it is the
//!    stronger and more precise statement that the only allocation it emits is a bump of Eden,
//!    which either succeeds — in which case no collection happened — or leaves. Every other
//!    allocating opcode (`newarray`, `anewarray`, `invoke*`) is still outside the subset;
//!  - the JIT runs only on `green` (one OS thread, and a context switch happens *between* opcodes)
//!    and `os-gil` (where the one global lock is held for the whole opcode, the native call
//!    included), and is switched off on `os` (parallel) at both dispatch points;
//!  - the GC is reached only from `Exec::safepoint`, i.e. between opcodes, on the same thread and
//!    under the same lock;
//!  - and at a **safepoint poll** compiled code *leaves* rather than waits, so it is never on the
//!    stack while a handshake runs.
//!
//! So while there is native code on the stack, the set of objects and their addresses are frozen. A
//! reference in a register cannot go stale, and one in the locals buffer cannot be missed: there is
//! no collector running to miss it.
//!
//! ## The type map
//!
//! What *is* needed is knowing, at each boundary, which slots hold references. The scan already
//! recomputed the operand-stack depth at every pc; step 5 widens that same fixed point to carry a
//! [`Kind`] for every local and every operand — the abstract interpretation, with types. Its
//! starting point is the **descriptor** ([`entry_locals`]), which is a fact rather than an
//! inference: a frame's non-argument slots are `Value::Int(0)` and its argument slots are what the
//! descriptor says.
//!
//! Two paths that reach a pc with different kinds are **joined** (step 9). For an *operand* that is
//! still fatal, as the depth check has always been. For a **local** the answer is
//! [`Kind::Conflict`], the top of the lattice: a slot nobody can read, which nobody has to, because
//! a disagreement of kinds at a merge is exactly `javac`'s way of saying the slot is dead there.
//! Reading one is refused ([`State::load`]) and *that refusal is the proof of the deadness*; a
//! `store` re-types it and the code after the store is ordinary again. Without this, no method that
//! allocates an object inside a loop ever compiled — which is one of the commonest shapes there is,
//! and is why `BmField` was out of reach until step 9.
//!
//! It is read at exactly two places. **[`CompiledCode::resume_sites`]** tells every exit that is
//! not a return whether to write `Value::Int` or `Value::Reference` back into the interpreter's
//! frame — the one mistake in this milestone that would corrupt the collector's world-view rather
//! than compute a wrong number. (Step 5 needed it only for the locals at a loop header; step 6
//! needs the same answer for the operand stack at every deopt site.) And
//! **[`CompiledCode::returns_reference`]** tells the caller what the 32 bits it got back mean.
//!
//! ## References across the boundary, and the 4 GiB limit
//!
//! A reference here is a **heap offset**, not an address — `0` is `null`, and the first real object
//! starts past a reserved null page. It crosses the boundary in the same 32 bits an `int` does
//! (`RAX = (status << 32) | value`), which caps the heap this tier can return a reference out of at
//! `u32::MAX`. That is not a new limit — the VM already stores every reference in a field, an array
//! slot and an object header as a `u32` — but it is now *load-bearing* for the JIT, so it is
//! checked rather than assumed: [`Heap::max_offset`] is compared against `u32::MAX` and a VM
//! configured past it simply compiles no method that returns or dereferences a reference
//! ([`Ineligible::HeapOutOfReach`]).
//!
//! Turning an offset into an address is [`heap_address`], and it is two-armed because the heap is
//! two buffers. Getting that backwards is the other way to be silently wrong, which is why the
//! offset/address distinction is named in every doc comment that touches it.
//!
//! ## Null and out-of-bounds: deopt, not exceptions
//!
//! `getfield` on `null`, `arraylength` on `null`, `iaload` on `null` or past the end — and, since
//! step 6, `putfield` on `null` and `iastore` out of range: every one of them **deopts**. Native
//! code raises no exception and decides no exception; the interpreter picks up at that pc, executes
//! that same instruction itself, and throws the right one by its ordinary path. Which exception it
//! is was never this tier's business, and now it does not even have to re-derive *where* it was.
//!
//! # The three semantics traps
//!
//! The emitter works in 64-bit registers; a Java `int` is 32-bit with wraparound. Three places
//! where the difference is not academic, and what this module does about each:
//!
//! ## 1. The normalisation invariant
//!
//! **Every `int` this code manipulates — in a register, in a stack slot, in a local slot — is the
//! sign-extension of its 32-bit value.** Equivalently: `v == (v as i32) as i64`, always.
//!
//! It is established on the way in (the interpreter marshals `Value::Int(v)` as `v as i64`, which
//! sign-extends) and re-established after every operation that can break it. Exactly three kinds
//! can: `iadd`/`isub`/`imul`/`ineg`/`iinc` (the 64-bit result may exceed 32 bits) and `ishl` (bits
//! shift up past bit 31) and `iushr` (which deliberately breaks it, then repairs it). Each is
//! followed by `movsxd r, r32` — "take the low 32 bits, sign-extend" — which *is* the JLS's
//! wraparound: `Integer.MAX_VALUE + 1` computes `0x8000_0000` in 64 bits and `movsxd` turns that
//! into `Integer.MIN_VALUE`.
//!
//! `iand`/`ior`/`ixor` need no repair, and that is a proof rather than an assumption: if both
//! operands are sign-extended, all 33 top bits of each are copies of bit 31, so the bitwise result
//! also has all 33 top bits equal — i.e. it is sign-extended too. `ishr` (arithmetic) likewise maps
//! `[-2^31, 2^31)` into itself. `movsxd` is emitted after `ishr` anyway: three bytes to make the
//! invariant hold *unconditionally* after every arithmetic opcode, rather than by case analysis.
//!
//! ## 2. Shift counts
//!
//! Java masks an `int` shift count to **5 bits** (JLS §15.19): `x << 33 == x << 1`. x86 masks to
//! 5 bits for 32-bit operands but to **6** for 64-bit ones — so a 64-bit `shl` by 33 shifts by 33.
//! Every shift here therefore emits an explicit `and rcx, 31` before the shift. The masking is in
//! the instruction stream, not implied by an operand size.
//!
//! `iushr` has a second half: it is a **logical** shift of the *32-bit* value. Applied to a
//! sign-extended `-1` (`0xFFFF_FFFF_FFFF_FFFF`), a 64-bit `shr` by 1 yields `0x7FFF_FFFF_FFFF_FFFF`,
//! whose low 32 bits are `0xFFFF_FFFF` = `-1` — not `Integer.MAX_VALUE`. So `iushr` first
//! *zero*-extends the low 32 bits (`mov eax, dword [slot]`), shifts, and then re-canonicalises with
//! `movsxd`.
//!
//! ## 3. Division
//!
//! `INT_MIN / -1` overflows a 32-bit `idiv` and raises `#DE`. Doing the division in **64 bits**
//! removes the problem instead of branching around it: the quotient `2^31` is representable, and
//! the mandatory `movsxd` truncates it to `INT_MIN` — exactly the wraparound JLS §15.17.2
//! prescribes. (`INT_MIN % -1` likewise yields `0`.)
//!
//! A **zero divisor** is the other `#DE`, and on Windows it arrives as a structured exception, not
//! a `java.lang.ArithmeticException`. Rather than emit exception machinery in native code, the
//! compiler emits an explicit `cmp rcx, 0; je deopt` — see below.
//!
//! # Deopt: the protocol, and why it is sound
//!
//! The compiled function is
//!
//! ```text
//!   extern "system" fn(buffer: *mut i64, entry_pc: i64) -> i64
//! ```
//!
//! and packs **status and value into the one return register**:
//!
//! ```text
//!   RAX = (status << 32) | (value as u32)
//!   status 0 = returned normally, value is the `ireturn`/`areturn` operand
//!   status 1 = deopt, value is the **bytecode pc** to resume interpreting at
//!   status 2 = safepoint, value is the **bytecode pc** to resume interpreting at
//! ```
//!
//! One register, no out-pointer, and the common case costs one instruction: `ireturn` is
//! `mov eax, dword [slot]`, whose zero-extension simultaneously loads the value and writes status
//! 0 into the high half. See [`Status`].
//!
//! # Step 6: a deopt that resumes instead of restarting, and the writes that unlocked
//!
//! Until this step a deopt carried **no state at all**: native code abandoned the attempt and the
//! interpreter ran the method **from its first byte**. That was sound for exactly one reason — the
//! subset was side-effect free, so a re-execution is indistinguishable from a first execution. It
//! is also the reason `putfield`, `iastore` and `putstatic` were outside the subset: a method that
//! stores and *then* meets a null receiver would, on re-execution, apply the store twice.
//!
//! What replaces it is a deopt that hands back **the interpreter's whole state at a pc**: the
//! locals, the operand stack, and the pc itself, so the interpreter *continues* rather than
//! repeats. The pieces were already here. `istore`/`iinc` write straight through to the caller's
//! buffer, so the locals are current the moment native code stops; and the abstract interpretation
//! of step 5 knows, at every pc, how many operands are live and what kind each one is. All that was
//! missing was somewhere to put the operands — the native frame slots die with the frame — so the
//! caller's buffer grew a second region:
//!
//! ```text
//!   buffer:  [ local 0 .. local (max_locals-1) | operand 0 .. operand (max_stack-1) ]
//!            └──── written through, always ────┘└──── written by a deopt stub only ────┘
//! ```
//!
//! [`CompiledCode::stack_base`] is where the second region starts and
//! [`CompiledCode::buffer_slots`] is how long the whole thing must be — **part of the marshalling
//! contract**, since a buffer sized the old way would be written past its end by the first guard
//! that fires. Each guarded pc gets its own **deopt stub**: copy the live operands into that region
//! bottom-first, `mov rax, (1 << 32) | pc`, and leave through the shared epilogue. The safepoint
//! exits are the same thing with an empty stack, which is why the two are now one mechanism and one
//! table — [`CompiledCode::resume_sites`].
//!
//! ## The rule: a deopt names an instruction that has **not** run
//!
//! This is the whole of what makes a write safe, and it is a rule about *emission order* rather
//! than about any one opcode:
//!
//! > **Every deopt guard of an instruction is emitted before that instruction's first observable
//! > effect, and nothing after that effect may deopt.** So the pc a deopt reports always names an
//! > instruction whose effect has not been applied, the interpreter re-executes exactly that
//! > instruction, and the pair "native attempt + interpreted resume" applies each write once.
//!
//! Equivalently: each instruction has a **guard phase** (no effects) followed by an **effect
//! phase** (no deopts). Every opcode in the subset obeys it, and the two halves are checked
//! separately in `burst::compile_tests` — a guard that fires leaves the heap untouched, and a
//! deopt *after* a write reports a pc past it.
//!
//! The alternative — deopt after the effect, reporting the pc of the *next* instruction — is also
//! sound and is what a mature JIT does for opcodes whose guard cannot precede the effect. Nothing
//! in this subset needs it, and having only one rule is worth more here than having the general one.
//!
//! A note on the locals: they are written through as the body runs, so they are not "applied twice"
//! in any sense — restoring them is a state transfer, not an effect. What makes them safe is the
//! **kind** they are restored with, which is the same argument as for the operand stack below.
//!
//! ## What this does *not* change
//!
//! - **No GC can observe a compiled frame.** Compiled code still allocates nothing, still runs only
//!   on `green`/`os-gil`, and still *leaves* at a poll rather than waiting. A write is not an
//!   allocation, and an `int` write is not a reference write, so no barrier and no card marking is
//!   involved (which is exactly why the three new opcodes are restricted to `int`).
//! - **Exception handlers.** A method with a non-empty exception table still compiles and still
//!   gets no on-stack entry points — see [`Method::has_handlers`]. The restriction is about an
//!   *entry* on an edge the forward analysis never followed; a deopt is an **exit**, taken at a pc
//!   native code reached along edges the analysis did follow, so the state there is exactly what the
//!   map says and a handler-bearing method may deopt like any other. (Its handler then catches, in
//!   the very frame that was rebuilt, and the frame goes on being interpreted.)
//! - **The 32-bit reference limit**, the normalisation invariant, and the resolvers' requirement
//!   that a `putstatic`'s class already be initialised, which is inherited from `getstatic`
//!   unchanged: compiled code has no way to run a `<clinit>`.
//!
//! # OSR and the safepoint poll: entering and leaving in the middle
//!
//! Step 3 adds the two halves of one mechanism — crossing the boundary at a **loop header**
//! instead of only at a method's entry and exit.
//!
//! ## The simplification that makes it tractable
//!
//! Both directions happen **only at back-edge targets whose operand-stack depth is 0**. The scan
//! already recomputes that depth at every pc, so the set is known at compile time (it is
//! [`CompiledCode::osr_entries`]). A `while`/`for` loop's back-edge always lands on an empty
//! stack, so this covers essentially every real loop — and it means the state to transfer, in
//! *both* directions, is only **the locals buffer plus a bytecode pc**. No operand stack is ever
//! reconstructed. A back-edge whose target has a non-empty stack is simply not an entry and not a
//! poll site; a method with no eligible back-edge behaves exactly as it did in step 2.
//!
//! Nothing has to be marshalled *out* on either exit, either: `istore`/`iinc` write straight
//! through to `[rbx + 8i]`, so by the time native code leaves, the caller's buffer already holds
//! the current locals. The interpreter copies them back into its `Frame` — see
//! [`code_cache`][super::code_cache].
//!
//! ## Entering (OSR)
//!
//! The second ABI argument (`RDX`) is the **bytecode pc to start at**; `0` means "the beginning",
//! which is what an ordinary invocation passes. When the method has entry points the prologue
//! emits one `cmp rdx, <pc>; je <that pc>` per entry and then falls through to pc 0. A compare
//! chain rather than a jump table because the count is one or two in practice (one per loop), the
//! whole chain runs once per *entry to native code* rather than per iteration, and a table would
//! need a rip-relative data island in the code page that this assembler deliberately has no
//! encoding for.
//!
//! ## Leaving (the safepoint poll)
//!
//! At each of those same pcs — i.e. at the top of every eligible loop header, which is executed
//! exactly once per iteration — the code checks a **poll word** and, if it is non-zero, returns
//! `status = 2` with that pc. The interpreter then resumes the method *interpreted* from there and
//! reaches its safepoint by the ordinary path; nothing about the GC has to be reachable from
//! native code. When the method next gets hot again it re-enters by OSR.
//!
//! Putting the poll at the loop **header** rather than at the back-edge itself is what keeps it to
//! one site per loop and needs no branch inversion: every back-edge to that header passes through
//! it, and the state there is exactly the state the entry contract already describes.
//!
//! The poll word's address is baked into the instruction stream as an immediate, so it **must not
//! move**. See [`code_cache::JitCache::poll_word`][super::code_cache::JitCache::poll_word] for
//! where it lives and why that address is stable.
//!
//! # Frame and stack mapping
//!
//! - **Locals** stay in the caller's buffer: `RBX` holds the incoming pointer and local `i` is
//!   `[rbx + 8i]`. No copy in or out — a compiled method returns its value, never its locals.
//! - **The operand stack** is *not* dynamic. Its depth is statically known at every pc (that is
//!   what a `StackMapTable` records, and [`scan_body`] recomputes it from the control-flow graph rather
//!   than trusting the attribute), so stack position `k` is simply the native frame slot `k`:
//!   `[rsp + 32 + 8k]`. Pushes and pops become nothing at all — only the compiler's idea of the
//!   current depth changes.
//!
//! A first tier deliberately keeps operands in memory rather than in registers: every slot is in
//! L1 and store-to-load forwarding covers the round trip, while a register-allocated operand stack
//! would have to negotiate with `idiv`'s fixed `RDX:RAX` and the shifts' fixed `CL`. Register
//! allocation is the next step, not this one.

use std::collections::{BTreeMap, BTreeSet};

use super::x64::{Asm, AsmError, Cond, Label, Mem, Reg};

/// What a value **is**, as far as the boundary is concerned — the lattice the abstract
/// interpretation carries for every local slot and every operand-stack position.
///
/// **A flat lattice with a top** (step 9). Three ordinary points that name what a slot holds, and
/// one that names *disagreement*:
///
/// ```text
///                Conflict          ⊤ — two paths, two answers
///              /    |    \
///           Int  Reference  Opaque
/// ```
///
/// Until step 8 there was no join at all: two paths reaching a pc with different kinds in the same
/// place made the method ineligible ([`Ineligible::TypeMismatch`]). That is a fine trade for the
/// *operand stack*, where it still holds — but for **locals** it threw away the single most common
/// shape `javac` emits. A slot that is **dead** at a merge is simply whatever the last writer on
/// each path left in it, and the two writers rarely agree: `BmField`'s outer loop header is reached
/// with an `int` in slot 2 on the way in (every non-argument slot starts as `Value::Int(0)`) and a
/// reference on the back-edge, and the slot is dead there because the loop body's first act is to
/// `astore` a fresh object into it.
///
/// So the join exists, and [`Conflict`][Kind::Conflict] is what it produces. The rule that keeps it
/// honest is not in this type but in [`State::load`]: a conflicted slot cannot be **read**. A method
/// that reads one is still refused, exactly as before; a method that merely carries one across a
/// merge and stores over it now compiles. See [`ResumeSite::locals`] for the write-back half of the
/// argument, which is the part that has to convince the collector rather than the verifier.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Kind {
    /// A Java `int` (or the `boolean`/`byte`/`short`/`char` that `javac` keeps in an `int`),
    /// carried as the sign-extension of its 32-bit value — see the normalisation invariant.
    Int,
    /// A **reference**: a heap *offset*, `0` for `null`, carried zero-extended (an offset is never
    /// negative). Not a machine address — see [`Heap`] for what turns one into the other.
    Reference,
    /// Something this tier cannot hold in a slot at all: a `long`, a `double` or a `float`
    /// argument. No opcode in the subset reads or writes one, so an `Opaque` slot is only ever
    /// *passed over* — which is why it needs no representation beyond "not ours".
    ///
    /// **Not the same claim as [`Conflict`][Kind::Conflict]**, though both are written back the same
    /// way (they are not written back at all). `Opaque` says *compiled code never wrote this slot*,
    /// so the interpreter's own value is the current one. That is a statement about the past.
    Opaque,
    /// **Two paths disagree** about what this slot holds: the top of the lattice, and the one point
    /// no value has a representation for.
    ///
    /// It says nothing about what is *in* the slot — that is exactly the point — and everything
    /// about what may be done with it. A `Conflict` slot may be carried, and it may be overwritten
    /// by a `store` (which re-types it, all the way back down to `Int` or `Reference`); it may not
    /// be **read**, which is what [`State::load`] and the `iinc` check refuse. That refusal is what
    /// turns "conflicted" into "dead", and *dead* is the whole of the write-back's licence to leave
    /// the interpreter's stale value alone.
    ///
    /// It can only ever appear in **locals**. An operand-stack position that would conflict is an
    /// [`Ineligible::TypeMismatch`] as it always was — see [`State::join_from`] for why the two
    /// halves of a frame do not get the same treatment.
    Conflict,
}

impl Kind {
    /// Whether this kind **names a `Value`** the interpreter can be handed — `Int` or `Reference`,
    /// and neither of the two that mean "no answer".
    ///
    /// The two negative cases are different claims with the same consequence: `Opaque` says
    /// compiled code never wrote the slot, `Conflict` says two paths wrote it differently. Where a
    /// value may be *skipped* (the root frame's locals) both are skipped; where it may not (an
    /// operand, an inlined frame's slot) both make the site unrebuildable and the method ineligible.
    pub fn names_a_value(self) -> bool {
        matches!(self, Kind::Int | Kind::Reference)
    }
}

/// The heap, as the addresses and layout constants compiled code needs to bake in.
///
/// A Java reference in this VM is a **byte offset into the heap**, and the heap is *two* buffers:
/// Eden is a separate lock-free arena, everything else (the survivor spaces and Old) lives in one
/// pre-reserved `Vec`. So turning an offset into a machine address is a two-armed function —
/// `offset < eden_end` picks Eden — and both arms are `base + offset` for a base that is fixed for
/// the VM's life. Both `base`s here are already **biased**, so that addition is the whole
/// computation and neither arm needs a subtraction.
///
/// Every field is a property of the VM rather than of the method, so this is filled in once by the
/// caller and read by every heap-touching opcode. Nothing here may move while compiled code exists
/// — see [`compile`] for the two facts (a pre-reserved `Vec` that never reallocates, a fixed-size
/// arena that never reallocates) that make that true.
#[derive(Clone, Copy, PartialEq, Eq, Debug, Default)]
pub struct Heap {
    /// Machine address of heap offset `0` **as seen from Eden** — the arena's base minus the null
    /// page. Valid for offsets below [`eden_end`][Heap::eden_end].
    pub eden_base: usize,
    /// Machine address of heap offset `0` for everything else: the byte region's base.
    pub other_base: usize,
    /// The first offset **not** served by Eden.
    pub eden_end: u32,
    /// One past the largest offset the heap can ever hand out. A reference is returned across the
    /// boundary in 32 bits (see [`Status`]), so a heap that could exceed `u32::MAX` puts every
    /// reference-returning and heap-reading method out of reach — [`Ineligible::HeapOutOfReach`].
    ///
    /// **Zero means "no heap was supplied"**, which is what [`Heap::default`] is: every opcode that
    /// would dereference a reference, and every `areturn`, is then out of reach. That is the
    /// configuration the assembler-level tests compile against, and it is what keeps a forgotten
    /// field from silently becoming a base address of 0.
    pub max_offset: usize,
    /// Machine address of **Eden's bump cursor**, the word a compiled `new` reserves through with a
    /// `lock xadd`. Zero means "no allocation may be compiled" — the same "no heap was supplied"
    /// posture [`max_offset`][Heap::max_offset] takes for the reads.
    ///
    /// The address is baked in as an immediate, so it must not move; the arena boxes the cursor for
    /// exactly that reason (see `EdenArena::cursor_address`), which is a stronger promise than the
    /// two bases have — those point into buffers that never reallocate, this one points at a word
    /// that also survives the *heap* being moved.
    pub eden_cursor: usize,
    /// Eden's capacity in bytes: what the arena checks a reservation against, and therefore what
    /// compiled code must check it against, to the byte.
    pub eden_capacity: usize,
    /// The **null page** — the heap offset of arena-local byte 0. A reservation at arena-local `l`
    /// is the heap offset `l + null_page`, and that offset is the reference the program sees.
    pub null_page: u32,
    /// Byte offset of an array's `length` word inside the array object.
    pub array_length: u32,
    /// Byte offset of element `0` of an `int[]`.
    pub int_array_data: u32,
    /// Bytes per element of an `int[]`.
    pub int_element: u32,
}

/// What a compiled `new` needs to know about the class it is allocating: how many bytes, and what
/// goes in the header's first word.
///
/// Both are resolved at **compile time** and baked in as immediates, so neither may depend on
/// anything that changes afterwards. Neither does: an instance layout is fixed once the class is
/// loaded, and a `Class<…>` mirror is allocated in Old and pinned against `gc::compact`, so its
/// offset is fixed for the VM's life — the same fact that lets `getstatic` bake in an address.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct Instance {
    /// The object's **logical** size in bytes: the header plus every instance field, its own and
    /// every superclass's. Not the 8-byte-rounded stride the arena bumps by — that rounding is the
    /// allocator's business, while *this* is what the collector copies when it evacuates, so the two
    /// must not be confused.
    pub size: u32,
    /// The header's `class_id` word: the class's `Class<…>` mirror offset, written exactly as the
    /// interpreter's `allocate` writes it. A byte of difference here is an object the collector
    /// cannot type, which is why it is resolved by the VM rather than derived here.
    pub class_id: u32,
}

/// Everything about the method being compiled that is not decided by reading its bytecode.
///
/// The **descriptor** is here because it is what makes the type map's starting point a fact rather
/// than an inference: on entry, local slot `i` holds either an argument, whose kind the descriptor
/// states, or `Value::Int(0)`, which is what the interpreter fills every other slot with when it
/// builds a frame. Without it the entry state would have to be "unknown", and an unknown that meets
/// a known at a merge is precisely the case where a write-back cannot decide between an `int` and a
/// reference — the one mistake in this milestone that would corrupt the collector's view of the
/// world rather than merely compute a wrong number.
#[derive(Clone, Copy, Debug)]
pub struct Method<'a> {
    /// **Which method body this is**, in the VM's own terms — its `MethodId`, passed back to every
    /// resolver in [`Environment`] and never interpreted here.
    ///
    /// Step 8 is what makes it necessary. A compilation is no longer one body: an inlined callee
    /// brings its **own constant pool**, so `ldc #7` means one thing in the caller and another in
    /// the callee, and a resolver that was handed only an index could not tell them apart. It is
    /// also the **identity** the cycle check is built on — a callee whose unit is already on the
    /// inline path would inline into itself forever (see [`MAX_INLINE_DEPTH`]).
    ///
    /// Opaque to `burst`: it is compared for equality and passed through, never dereferenced. `0`
    /// is a perfectly good value for a compilation of one body.
    pub unit: Unit,
    /// The method body — its `code[]`.
    pub code: &'a [u8],
    /// The frame's local-slot count.
    pub max_locals: usize,
    /// The method descriptor, e.g. `"(I[I)I"`. Read for the argument kinds and the return kind.
    pub descriptor: &'a str,
    /// Whether the method is `static` — i.e. whether slot 0 is an argument or `this`.
    pub is_static: bool,
    /// Whether the method has a **non-empty exception table**.
    ///
    /// Such a method still compiles, but it is offered **no on-stack entry points**. The reason is
    /// the type map: it is a forward analysis from pc 0, and a handler is reachable by an edge the
    /// analysis cannot see, so the interpreter could arrive at a loop header having run code the
    /// map never looked at — with, say, a reference in a slot the map calls an `int`. (It is not
    /// hypothetical: the subset's one deopt, a zero divisor, sends the interpreter back to run the
    /// method itself, and *that* execution can throw and be caught right here.) An **ordinary**
    /// entry is untouched by this: native code throws nothing, so no handler can fire while it
    /// runs, and its entry state is the descriptor's.
    pub has_handlers: bool,
}

/// What the compiler must ask the VM, and the addresses it bakes in.
///
/// Every one of these is a `&dyn Fn` rather than a trait so that `burst` needs to know nothing
/// about constant pools, class initialisation, field layouts or mirrors: an index it cannot have
/// answered is simply a `None` and the method is refused.
/// **Which method body a constant-pool index belongs to.** The VM's own `MethodId`, opaque here.
///
/// Before step 8 there was exactly one body in a compilation, so an index needed no qualifier. An
/// inlined callee has its own pool, its own class and its own resolved-site cache, so every
/// resolver below is a function of the **pair** `(unit, index)` — and reading a callee's `ldc #7`
/// out of the caller's pool is precisely the silent mistake this parameter exists to make
/// impossible.
pub type Unit = usize;

/// A callee this tier may **inline**, as [`Environment::invoke`] answers it: its whole body, in the
/// same shape the root method arrives in.
///
/// It is a [`Method`], not a lesser summary, because inlining compiles it with the *same* compiler:
/// its own `max_locals`, its own descriptor (which is where its argument kinds come from), its own
/// `unit` (which is what its constant-pool indices resolve against and what the cycle check
/// compares), and its own exception table.
#[derive(Clone, Copy, Debug)]
pub struct Callee<'a> {
    /// The callee's body and shape.
    pub method: Method<'a>,
    /// How many **operand-stack values** the call consumes — the descriptor's arguments plus, for
    /// an instance call, the receiver. It is the VM's answer rather than a re-parse of the
    /// descriptor here, because it is the same number the interpreter's own `invokespecial` pops
    /// and the two must not be able to disagree.
    pub arg_slots: usize,
}

pub struct Environment<'a> {
    /// Resolves a constant-pool index **of `unit`** to an `int` constant — `None` for every other
    /// constant kind, which is how a `String` or class-literal `ldc` is rejected.
    pub int_const: &'a dyn Fn(Unit, u16) -> Option<i32>,
    /// Resolves a `getstatic`'s index **in `unit`** to the **absolute address** of an `int` static,
    /// and answers `None` unless the field is a static `int` of an **already-initialised** class.
    pub static_int: &'a dyn Fn(Unit, u16) -> Option<usize>,
    /// Resolves a `getfield` at `(unit, pc, index)` to the field's **byte offset inside the
    /// object**, and answers `None` unless it is a **non-`volatile` instance field of type `int`**.
    /// The `pc` is passed because the VM keys its own resolved-site cache by it; the answer depends
    /// only on the unit and the index.
    pub int_field: &'a dyn Fn(Unit, usize, u16) -> Option<u32>,
    /// Resolves a `new`'s class-constant index to the two facts an inline allocation needs — see
    /// [`Instance`] — and answers `None` unless every precondition of a *bare* allocation holds:
    /// the class is loaded, its layout is known, its `Class<…>` mirror exists, and it is
    /// **initialised** (`Done`).
    ///
    /// That last one is the same requirement `getstatic` has and for the same reason: `new` is a
    /// first active use, so an uninitialised class would have to run its `<clinit>` — and compiled
    /// code has no way to run anything. A method allocating an instance of a class that is not yet
    /// initialised simply does not compile.
    pub instance: &'a dyn Fn(Unit, u16) -> Option<Instance>,
    /// Resolves the **invoke** at `(unit, pc, index)` to the callee this tier may inline there, or
    /// `None` for every call it may not.
    ///
    /// `None` is the answer for the overwhelming majority and it is never an error: a virtual call
    /// whose receiver type is not known, a native or abstract method, a class that is not
    /// initialised, an `invokedynamic`. The compiler treats a `None` exactly as it treats an opcode
    /// outside the whitelist — the method is refused — so the VM decides *what may be inlined*
    /// without `burst` knowing anything about virtual dispatch or class initialisation.
    ///
    /// What the VM must guarantee for a `Some`, and what this tier cannot check: the call is
    /// **statically bound** (so the body handed back is the body that would run), the callee's
    /// class is initialised (`Done`) — the same requirement `getstatic` and `new` have, and for the
    /// same reason: compiled code cannot run a `<clinit>` — and the callee is neither native nor
    /// synchronized (there is no monitor to take in an instruction stream).
    pub invoke: &'a dyn Fn(Unit, usize, u16) -> Option<Callee<'a>>,
    /// Where the heap is and how it is laid out — see [`Heap`].
    pub heap: Heap,
    /// The address of the 8-byte safepoint poll word, baked in as an immediate.
    pub poll_word: usize,
}

/// The register holding the caller's locals buffer for the whole body. Callee-saved, so the
/// prologue/epilogue pair saves and restores it; `RCX` (where the ABI delivers it) is needed as
/// the shift count register.
const LOCALS: Reg = Reg::Rbx;

/// The register holding the **safepoint poll word's address** for the whole body, loaded once in
/// the prologue. Callee-saved, and only saved (and only loaded) when the method has at least one
/// poll site — keeping the address in a register turns each poll into `mov` + `cmp` + `jcc`
/// instead of re-materialising a 10-byte `movabs` immediate on every loop iteration.
const POLL: Reg = Reg::Rsi;

/// Scratch. Never live across an instruction boundary — every bytecode opcode loads what it needs,
/// computes, and stores back to a slot.
const T0: Reg = Reg::Rax;
/// Second scratch. Also the *mandatory* shift-count register (`shl/shr/sar` read `CL`) and the
/// divisor register for `idiv`.
const T1: Reg = Reg::Rcx;
/// Third scratch. Clobbered by `cqo`/`idiv` (which write the remainder there), so nothing may be
/// live in it across a division.
const T2: Reg = Reg::Rdx;

/// **The operand-stack cache** (step 10): the registers native operand slots `0..CACHE.len()` live
/// in, in slot order. Everything past the end of this array stays in its frame slot, exactly as
/// every operand did before this step.
///
/// # Which registers are left, and why these eight
///
/// Of the sixteen: `RSP`/`RBP` are the frame, `RBX` is [`LOCALS`], `RSI` is [`POLL`], and
/// `RAX`/`RCX`/`RDX` are the three scratch registers every opcode computes in. That leaves `RDI`
/// and `R8`–`R15` — nine — and the eight `R8`–`R15` are taken. `RDI` is deliberately left out: it
/// buys one more cached position at the cost of a ninth save/restore, and no shape in the subset
/// reaches a ninth operand without a deep inline tree, where the memory fallback is the honest
/// answer anyway.
///
/// # The x86-64 traps this dodges rather than negotiates
///
/// `idiv` consumes and clobbers **RDX:RAX** implicitly, and every shift reads its count from
/// **CL**. A cache that could place an operand in one of those would have to move or spill it at
/// each division and each shift, per site, with the deopt state to match. **None of them is in
/// this array**, so the conflict does not exist: `idiv` clobbers only scratch, `shl r9, cl` names
/// a cached operand and a fixed count register that can never be the same register, and the
/// emitter needs no case analysis for either. That is why the set is chosen against the
/// instruction encoding rather than for its size.
///
/// The first four are **volatile** under the Microsoft x64 ABI and cost nothing; `R12`–`R15` are
/// callee-saved and are pushed only when the compilation's native slot count actually reaches them
/// — see [`compile`].
const CACHE: [Reg; 8] = [Reg::R8, Reg::R9, Reg::R10, Reg::R11, Reg::R12, Reg::R13, Reg::R14, Reg::R15];

/// How many operand-stack positions are register-resident by default: all of [`CACHE`].
///
/// [`compile_with_regs`] takes this as a parameter so that **both arms are the same binary** — the
/// measurement in `burst::jit_tests` flips it at run time, and the environment variable
/// `JVM_JIT_REGS=0` (see `JitCache::from_env`) is the user-facing switch. With `0` the emitter
/// produces byte-for-byte what step 9 produced, which is what makes the comparison honest: there is
/// no relink and therefore no code-layout difference between the two arms.
pub const CACHE_REGS: u32 = CACHE.len() as u32;

/// **Where one operand-stack position lives.** The whole of the register allocator's state, and it
/// is deliberately not a state at all: [`operand_home`] is a function of the position alone.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Home {
    /// In a [`CACHE`] register, for the whole life of the compilation.
    Reg(Reg),
    /// In its native frame slot `[rsp + 32 + 8s]`, exactly as before step 10.
    Slot(Mem),
}

/// **The allocation, in one line**: native operand slot `s` lives in `CACHE[s]` when `s < regs`, and
/// in its frame slot otherwise.
///
/// # Why this needs no liveness analysis, and no spill at a block edge
///
/// The mapping is from an operand's **position**, not from a value, and the operand-stack depth is
/// already known statically at every pc (that is what [`scan_body`] recomputes and what
/// [`Ineligible::StackMismatch`] refuses to guess at). So the set of live cache registers at a pc is
/// a function of the depth there — and two paths that reach a pc *agree* about that depth, or the
/// method was never compiled. A branch therefore needs no spill, a merge needs no reconciliation,
/// and there is no "register state" for two edges to disagree about: **the state is the depth**,
/// which the scan has already made single-valued.
///
/// That is a stronger property than the usual basic-block stack cache, and it is what makes the
/// deopt side tractable too: a stub knows its site's depth, so it knows exactly which registers hold
/// which operands, with no per-site bookkeeping beyond the depth the resume map already carries.
///
/// The positions are cached from the **bottom** up, which is the right end for this subset: an
/// expression like `iload a; iload b; iadd` occupies positions 0 and 1, and a body whose stack goes
/// deeper than [`CACHE`] simply keeps its deepest positions in memory. Inlined bodies get the slots
/// *above* the root's ([`Body::frame_base`]), so the root — the only body that may contain a loop —
/// is the one that gets the registers.
fn operand_home(frame: &super::x64::Frame, regs: u32, s: u32) -> Home {
    match s < regs {
        true => Home::Reg(CACHE[s as usize]),
        false => Home::Slot(frame.local(s)),
    }
}

/// Materialises an operand into `dst`, all 64 bits. A no-op when it is already there.
fn read_home(a: &mut Asm, dst: Reg, h: Home) {
    match h {
        Home::Reg(r) if r == dst => {}
        Home::Reg(r) => a.mov_rr(dst, r),
        Home::Slot(m) => a.mov_rm(dst, m),
    }
}

/// Materialises an operand into `dst` **zero-extended from 32 bits** — what `iushr` needs before a
/// logical shift, and what `ireturn` needs to put the value in the low half of `RAX`.
///
/// Unlike [`read_home`] the register case is *not* elided when `dst` is already the home: on
/// x86-64 every write to a 32-bit register zeroes the upper half, so `mov r9d, r9d` is exactly the
/// zero-extension being asked for and dropping it would leave a sign-extended `-1` intact.
fn read_home32(a: &mut Asm, dst: Reg, h: Home) {
    match h {
        Home::Reg(r) => a.mov_rr32(dst, r),
        Home::Slot(m) => a.mov_rm32(dst, m),
    }
}

/// Stores `src` into an operand's home. A no-op when `src` already *is* that home.
fn write_home(a: &mut Asm, h: Home, src: Reg) {
    match h {
        Home::Reg(r) if r == src => {}
        Home::Reg(r) => a.mov_rr(r, src),
        Home::Slot(m) => a.mov_mr(m, src),
    }
}

/// The register an operand can be **read from**: its own, or `scratch` after a load.
///
/// The returned register must be treated as read-only by the caller when it is a home — which is
/// what every use below does, since a home register is the operand and clobbering it would lose it.
fn in_reg(a: &mut Asm, h: Home, scratch: Reg) -> Reg {
    match h {
        Home::Reg(r) => r,
        Home::Slot(m) => {
            a.mov_rm(scratch, m);
            scratch
        }
    }
}

/// The register an operand's **result** should be computed in: its own home when it has one, so the
/// store afterwards is free, and [`T0`] otherwise, which is what the pre-step-10 emitter always did.
fn work_reg(h: Home) -> Reg {
    match h {
        Home::Reg(r) => r,
        Home::Slot(_) => T0,
    }
}

/// The ALU operations that read one operand in place — the register/memory pair, chosen by where
/// the operand lives. Written as an enum rather than as seven near-identical helpers so the
/// `Home::Reg`/`Home::Slot` split is made exactly once.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Alu {
    Add,
    Sub,
    Imul,
    And,
    Or,
    Xor,
    Cmp,
}

/// `op dst, <operand>` — the `r64, r64` form when the operand is cached, the `r64, r/m64` form when
/// it is in a frame slot. The two are the same instruction with a different ModRM, which is the
/// whole reason this step costs the emitter so little.
fn alu_home(a: &mut Asm, op: Alu, dst: Reg, h: Home) {
    match h {
        Home::Reg(r) => match op {
            Alu::Add => a.add_rr(dst, r),
            Alu::Sub => a.sub_rr(dst, r),
            Alu::Imul => a.imul_rr(dst, r),
            Alu::And => a.and_rr(dst, r),
            Alu::Or => a.or_rr(dst, r),
            Alu::Xor => a.xor_rr(dst, r),
            Alu::Cmp => a.cmp_rr(dst, r),
        },
        Home::Slot(m) => match op {
            Alu::Add => a.add_rm(dst, m),
            Alu::Sub => a.sub_rm(dst, m),
            Alu::Imul => a.imul_rm(dst, m),
            Alu::And => a.and_rm(dst, m),
            Alu::Or => a.or_rm(dst, m),
            Alu::Xor => a.xor_rm(dst, m),
            Alu::Cmp => a.cmp_rm(dst, m),
        },
    }
}

/// How a native call ended — the decoded form of the packed return value (see the module docs).
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Outcome {
    /// Ran to an `ireturn`/`areturn`; this is the method's result.
    Returned(i32),
    /// Gave up at this **bytecode pc** — a zero divisor, a null receiver, an index out of range.
    /// The instruction at that pc has *not* been executed (see the write/pc rule in the module
    /// docs), the caller's buffer holds the locals **and** the operand stack, and the interpreter
    /// resumes there: it re-executes that very instruction and raises the proper exception.
    ///
    /// [`Status::NO_PC`] means the status word was not one this compiler emits — the state cannot
    /// be reconstructed, so the caller must fall back rather than resume.
    Deopt(u32),
    /// The safepoint poll fired. Same contract as [`Outcome::Deopt`]: the buffer holds the state
    /// and this is the bytecode pc to resume interpreting at. The only difference is *why*, which
    /// matters to the counters and to whether on-stack entry stays open.
    Safepoint(u32),
    /// A `new`'s fast path was not available — Eden is full, or this excursion has already logged
    /// as many allocations as the buffer holds. Same state contract again; see [`Status::ALLOC`]
    /// for why it is not folded into [`Outcome::Deopt`].
    AllocFailed(u32),
}

/// The status half of the packed return value (see the module docs).
pub struct Status;

impl Status {
    /// Bits the status occupies: the high half of `RAX`.
    pub const SHIFT: u32 = 32;
    /// The method ran to an `ireturn`; the low 32 bits are the result.
    pub const OK: i64 = 0;
    /// The method gave up; the low 32 bits are the bytecode pc to resume at.
    pub const DEOPT: i64 = 1;
    /// The poll fired; the low 32 bits are the bytecode pc to resume at.
    pub const SAFEPOINT: i64 = 2;
    /// A `new` could not take its **fast path**; the low 32 bits are the bytecode pc to resume at.
    ///
    /// The state contract is a deopt's exactly — the buffer holds the locals and the operand stack,
    /// and the instruction at that pc has not run — but the *reason* is a capacity condition rather
    /// than a guard failure: Eden had no room, or the excursion's allocation log is full. Two things
    /// follow from that difference, which is why it is a status of its own. It must not close the
    /// method's on-stack entry (the condition clears at the next collection, and closing OSR on the
    /// first Eden fill would retire every allocating loop after one lap); and it must not be counted
    /// as a deopt, because "this method keeps failing a guard" and "this loop keeps filling Eden"
    /// are different facts about a run.
    pub const ALLOC: i64 = 3;

    /// The pc [`unpack`][Status::unpack] reports for a status word no emitted code produces. Not a
    /// valid bytecode pc ([`MAX_CODE_LEN`] is four kilobytes), so a caller that looks it up in the
    /// resume table simply finds nothing.
    pub const NO_PC: u32 = u32::MAX;

    /// The exact `i64` a deopt stub at bytecode `pc` returns — `mov rax, <this>`.
    pub const fn deopt_value(pc: u32) -> i64 {
        (Status::DEOPT << Status::SHIFT) | pc as i64
    }

    /// The exact `i64` a safepoint exit at bytecode `pc` returns.
    pub const fn safepoint_value(pc: u32) -> i64 {
        (Status::SAFEPOINT << Status::SHIFT) | pc as i64
    }

    /// The exact `i64` an **allocation** exit at bytecode `pc` returns — see [`Status::ALLOC`].
    pub const fn alloc_value(pc: u32) -> i64 {
        (Status::ALLOC << Status::SHIFT) | pc as i64
    }

    /// Decodes a returned `i64`.
    ///
    /// This is the *only* place the packing is interpreted, so the encoding above and the
    /// decoding here cannot drift apart. An unknown status decodes as a deopt at
    /// [`Status::NO_PC`]: no emitted code produces one, and "the caller cannot reconstruct
    /// anything, so it must not try" is the answer that is safe for every possible state.
    pub fn unpack(raw: i64) -> Outcome {
        match raw >> Status::SHIFT {
            Status::OK => Outcome::Returned(raw as i32),
            Status::SAFEPOINT => Outcome::Safepoint(raw as u32),
            Status::DEOPT => Outcome::Deopt(raw as u32),
            Status::ALLOC => Outcome::AllocFailed(raw as u32),
            _ => Outcome::Deopt(Status::NO_PC),
        }
    }
}

/// Why a method will never be compiled. Every variant is a *permanent* property of the bytecode,
/// so the answer is cached once and the scan never repeated.
#[derive(Clone, PartialEq, Eq, Debug)]
pub enum Ineligible {
    /// An opcode outside the whitelist (the overwhelmingly common answer).
    Opcode { pc: usize, opcode: u8 },
    /// `ldc`/`ldc_w` naming something that is not a `CONSTANT_Integer`.
    NonIntegerConstant { pc: usize, index: u16 },
    /// A `getstatic` the resolver would not give an address for: the field is not a static `int`,
    /// or its declaring class is not initialised yet. Unlike every other variant this one is a
    /// property of the *VM's state* as well as of the bytecode, so caching it means a method that
    /// would compile after the class initialises never gets a second look. That is the deliberate
    /// price of keeping a class-init barrier out of native code — see the module docs.
    UnresolvedStatic { pc: usize, index: u16 },
    /// A `getfield` the resolver would not give an offset for: the field is not an instance `int`,
    /// it is `volatile`, or its class's layout could not be folded. Like
    /// [`UnresolvedStatic`][Ineligible::UnresolvedStatic] this is a property of the VM's state as
    /// well as of the bytecode, and caching it is the same deliberate price.
    UnresolvedField { pc: usize, index: u16 },
    /// A `new` the resolver would not answer for: the class is not loaded, has no mirror, or — the
    /// common case — has not been initialised yet. Like the two above it is a property of the VM's
    /// state as well as of the bytecode, and caching it is the same deliberate price.
    UnresolvedClass { pc: usize, index: u16 },
    /// A `new` this tier cannot allocate inline even though the class resolved: no Eden cursor was
    /// supplied, the object is bigger than Eden, or Eden's capacity does not fit the immediate the
    /// bounds check needs. A property of the VM's configuration and of the class, not of the method.
    AllocOutOfReach { pc: usize },
    /// A branch target outside the code array, or an instruction whose operand bytes run off
    /// the end.
    OutOfRange { pc: usize },
    /// Two paths reach the same pc with different operand-stack depths. Legal bytecode never does
    /// this (JVMS §4.10.1), so it means the walk lost track — bail rather than guess.
    StackMismatch { pc: usize, seen: u16, found: u16 },
    /// Two paths reach the same pc with different **kinds** in the same operand-stack position — an
    /// `int` on one and a reference on the other. Verified bytecode never does this (the JVMS
    /// requires the operand stack types to agree at every merge), and an operand is the one thing a
    /// resume site cannot skip, so it stays fatal.
    ///
    /// Since step 9 it is *not* what a disagreeing **local** produces: those join to
    /// [`Kind::Conflict`] and the method compiles. The exception is a body with exception handlers,
    /// where a conflict is still this error — see [`State::join_from`] for why the walk cannot prove
    /// the slot dead there.
    TypeMismatch { pc: usize },
    /// A [`ResumeSite`] carries a value the interpreter could not put back: an operand-stack
    /// position or an **inlined frame's** slot whose kind is [`Kind::Opaque`] or
    /// [`Kind::Conflict`], neither of which names a `Value`.
    ///
    /// Unlike the root frame's locals, these have no skip: an operand's position is its identity,
    /// and an inlined frame does not exist yet, so every one of its slots has to be written. A
    /// compilation with such a site is refused **here**, at compile time, which is what makes the
    /// reconstruction on the other side of the boundary total — see [`CompiledCode::resume_sites`].
    Unrebuildable { key: u32 },
    /// An opcode was handed the wrong kind: an `iload` of a slot holding a reference, an `iadd` on
    /// one, an `areturn` in a method whose descriptor returns an `int`. Verified bytecode never
    /// does this, so it means either a hostile class file or a hole in the map — refuse either way.
    WrongType { pc: usize },
    /// A heap-reading opcode this tier cannot address: the VM's heap could grow past the 32 bits a
    /// reference has to cross the boundary in, or its Eden boundary does not fit an immediate.
    /// A property of the VM's configuration, not of the method.
    HeapOutOfReach { pc: usize },
    /// An opcode wanted more operands than the statically-known depth provides.
    StackUnderflow { pc: usize },
    /// Instructions overlap — a branch landed *inside* another instruction. Impossible from
    /// `javac`, possible from a hand-written or hostile class file.
    OverlappingInstructions { pc: usize },
    /// A local index at or beyond `max_locals`.
    LocalOutOfRange { pc: usize, slot: usize },
    /// A callee that would be inlined contains a **backward branch**. Only the root's loop headers
    /// carry a safepoint poll and only the root can be entered on-stack, so an inlined loop would be
    /// one native code could not be pulled out of — see [`scan_body`].
    InlineLoop { pc: usize },
    /// The invoke at this pc is past [`MAX_INLINE_DEPTH`].
    InlineDepth { pc: usize },
    /// The callee at this pc is **already on the inline path** — direct or mutual recursion, which
    /// would expand without end. Caught by [`Unit`] identity rather than by the depth bound.
    InlineCycle { pc: usize },
    /// Expanding the callee at this pc would take the compilation past [`MAX_INLINE_BYTES`].
    InlineBudget { pc: usize },
    /// Past the size this tier is willing to compile (code length or operand-stack depth).
    TooBig,
    /// The assembler refused to encode something — a compiler bug, surfaced rather than panicked.
    Assembler(AsmError),
}

impl std::fmt::Display for Ineligible {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Ineligible::Opcode { pc, opcode } => write!(f, "opcode 0x{opcode:02x} at {pc} is outside the compiled subset"),
            Ineligible::NonIntegerConstant { pc, index } => write!(f, "ldc #{index} at {pc} is not an integer constant"),
            Ineligible::UnresolvedStatic { pc, index } => {
                write!(f, "getstatic #{index} at {pc} is not an int static of an initialised class")
            }
            Ineligible::UnresolvedField { pc, index } => {
                write!(f, "getfield #{index} at {pc} is not a non-volatile int instance field")
            }
            Ineligible::UnresolvedClass { pc, index } => {
                write!(f, "new #{index} at {pc} is not an instance of a loaded, initialised class")
            }
            Ineligible::AllocOutOfReach { pc } => {
                write!(f, "the allocation at {pc} is outside what this tier can do inline")
            }
            Ineligible::OutOfRange { pc } => write!(f, "instruction or branch at {pc} leaves the code array"),
            Ineligible::StackMismatch { pc, seen, found } => {
                write!(f, "pc {pc} is reached with stack depth {seen} and {found}")
            }
            Ineligible::TypeMismatch { pc } => write!(f, "pc {pc} is reached with disagreeing value kinds"),
            Ineligible::Unrebuildable { key } => write!(f, "resume site {key} holds a value with no interpreter representation"),
            Ineligible::WrongType { pc } => write!(f, "the opcode at {pc} was handed a value of the wrong kind"),
            Ineligible::HeapOutOfReach { pc } => {
                write!(f, "the heap read at {pc} is outside what this tier can address")
            }
            Ineligible::StackUnderflow { pc } => write!(f, "operand stack underflow at {pc}"),
            Ineligible::OverlappingInstructions { pc } => write!(f, "an instruction boundary falls inside another at {pc}"),
            Ineligible::LocalOutOfRange { pc, slot } => write!(f, "local {slot} at {pc} is past max_locals"),
            Ineligible::InlineLoop { pc } => write!(f, "the callee to inline loops (back-edge at {pc})"),
            Ineligible::InlineDepth { pc } => write!(f, "the call at {pc} is past the inlining depth limit"),
            Ineligible::InlineCycle { pc } => write!(f, "the callee at {pc} is already on the inline path"),
            Ineligible::InlineBudget { pc } => write!(f, "inlining the call at {pc} exceeds the code budget"),
            Ineligible::TooBig => write!(f, "method is larger than this tier compiles"),
            Ineligible::Assembler(e) => write!(f, "assembler: {e}"),
        }
    }
}

impl std::error::Error for Ineligible {}

/// **One point at which native code can hand a half-finished method back**, and everything the
/// interpreter needs to rebuild its own frame there.
///
/// Two things produce one: a **loop header** (where the safepoint poll may fire) and a **deopt
/// guard** (a zero divisor, a null receiver, an index out of range). They differ only in why
/// control left; what has to be reconstructed is the same, so it is described once.
///
/// The kinds are read straight out of the fixed point the scan already computed for this pc — there
/// is exactly one type map — and they are what turns the bare `i64`s in the caller's buffer back
/// into `Value`s. Getting one wrong is the mistake in this milestone that does not fail where the
/// bug is: an offset put back as an `int` is a live object the collector can no longer see or
/// relocate, and an `int` put back as a reference is a pointer made of arithmetic.
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct ResumeSite {
    /// **What native code reports to name this site** — the low 32 bits of a [`Status::DEOPT`],
    /// [`Status::ALLOC`] or [`Status::SAFEPOINT`] return.
    ///
    /// For a site in the root's own body it *is* [`pc`][ResumeSite::pc], which is what keeps the
    /// safepoint exits and the OSR entry dispatch talking about bytecode pcs. A site inside an
    /// **inlined** callee has no pc in the root's body that names it — two deopts in one expanded
    /// callee share the root's single invoke pc — so it is given a key past [`MAX_CODE_LEN`], where
    /// no bytecode pc can reach. See [`inlined`][ResumeSite::inlined].
    pub key: u32,
    /// The bytecode pc **the root frame** resumes at. The instruction there has **not** run.
    ///
    /// For a site inside an inlined callee this is the pc of the *invoke*, and that is the whole
    /// trick: the interpreter leaves a caller's pc pointing at the invoke and lets the matching
    /// `return` step over it, so a rebuilt caller frame at the invoke — with its arguments already
    /// popped, since they are the callee's locals now — is exactly the state an interpreted call
    /// would have left, and the callee's return finishes the call with no special case at all.
    pub pc: u32,
    /// The kind of each of [`CompiledCode::touched_locals`], in that order, *at this pc*.
    ///
    /// **Two kinds mean "do not write this slot back", for two different reasons, and the second
    /// one is what step 9 had to justify to the collector.**
    ///
    /// [`Kind::Opaque`] is the easy one: the map proves compiled code cannot have written the slot
    /// on any path reaching here, so the interpreter's own value *is* the current one and copying
    /// anything over it would be the mistake.
    ///
    /// [`Kind::Conflict`] is the interesting one. Compiled code may well have written this slot —
    /// as an `int` down one path and as a reference down another — and which one it did is not a
    /// static fact. There is no right answer to write, so nothing is written, and the interpreter
    /// keeps whatever `Value` its frame held before the call. The three things that makes true:
    ///
    ///  - **It is safe for the collector, unconditionally.** The stale `Value` is a real one the
    ///    frame already held — a valid reference or an `int` — so the frame stays a well-typed GC
    ///    root. Nothing here can produce the one unrecoverable error, a heap offset handed back as
    ///    `Value::Int` (invisible to the collector, never relocated) or an `int` handed back as
    ///    `Value::Reference` (a pointer made of arithmetic). Writing *nothing* cannot make either.
    ///  - **It is correct, because a conflicted slot is dead.** Not by assumption: by the refusal in
    ///    [`State::load`]. Every read of a slot is checked against its kind at that pc, and
    ///    `Conflict` matches neither `Int` nor `Reference`, so a method that reads one never
    ///    compiles. Since `Conflict` is the top of the lattice and only a `store` can bring a slot
    ///    back down, any read reachable from here is preceded, on every path, by a store that
    ///    re-types it — and the interpreter takes one of exactly those paths, because this walk
    ///    covered every edge out of this pc that it can take. (Every edge *except* into an exception
    ///    handler, which is why a body with handlers is not allowed to have conflicts at all.)
    ///  - **The cost is a leak, and it is bounded.** If compiled code overwrote a reference slot
    ///    with an `int`, the frame still names the old object, so it stays reachable until the frame
    ///    dies or the slot is stored to — which is the *first* thing that happens on any path that
    ///    reads it. One object, until the next store. That is the whole price of the feature.
    pub locals: Vec<Kind>,
    /// The kind of each live operand-stack position **of the root frame**, bottom-first.
    /// `stack.len()` is the depth, and position `k` was spilled to buffer slot
    /// [`CompiledCode::stack_base`]` + k`. Never contains [`Kind::Opaque`]: nothing in this subset
    /// can push one.
    ///
    /// At a site inside an inlined callee this is the stack **with the call's arguments already
    /// removed** — they became the callee's locals, so leaving them here would push them twice.
    pub stack: Vec<Kind>,
    /// **The frames above the root**, outermost first and innermost last — empty for a site in the
    /// root's own body, which is every site there was before step 8.
    ///
    /// Each is a whole interpreter frame the compilation flattened away and now has to give back.
    /// The interpreter pushes them in this order on top of the root frame it has just rebuilt, and
    /// from that moment the call chain is an ordinary one: the innermost frame runs the instruction
    /// native code could not, and each `return` unwinds into the caller waiting at its invoke.
    pub inlined: Vec<VirtualFrame>,
}

impl ResumeSite {
    /// Whether the interpreter can rebuild **all** of this site: every operand and every slot of
    /// every inlined frame names a `Value`.
    ///
    /// The root frame's [`locals`][ResumeSite::locals] are deliberately not checked — they are the
    /// one part with a legitimate skip. Checked at the end of [`compile`], so a compilation that
    /// would fail here is never handed out; see [`Ineligible::Unrebuildable`].
    pub fn is_rebuildable(&self) -> bool {
        self.stack.iter().all(|k| k.names_a_value())
            && self.inlined.iter().all(|frame| {
                frame.locals.iter().all(|&(_, k)| k.names_a_value())
                    && frame.stack.iter().all(|&(_, k)| k.names_a_value())
            })
    }
}

/// **One interpreter frame that inlining removed**, and everything needed to build it again.
///
/// A compiled method with an expanded callee has no second frame at run time — that is the point of
/// inlining — so a deopt from inside one has to *materialise* what a call would have created. Unlike
/// the root frame, which the interpreter is already holding, none of this exists yet: every local
/// has to be written, not just the ones compiled code may have touched.
///
/// That is why the locals here are complete and carry no `Opaque` escape hatch. The emitted call
/// writes the arguments into the leading slots and **zeroes the rest** — the interpreter's own
/// `Frame::reset_for_call`, restated in machine code — so every slot in this list holds a value
/// this compilation put there.
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct VirtualFrame {
    /// Which method this frame runs: the [`Unit`] the VM handed back for the inlined callee, i.e.
    /// its `MethodId`. The interpreter builds the frame for exactly this method.
    pub unit: Unit,
    /// The bytecode pc **in that method**. For the innermost frame it is where the guard fired; for
    /// any frame in between it is the invoke that expanded the frame above it, and the same
    /// leave-the-pc-at-the-invoke convention applies as for the root.
    pub pc: u32,
    /// Every local slot of this frame, slot `0` first: the buffer slot it lives in, and its kind.
    /// `locals.len()` is the method's `max_locals`.
    pub locals: Vec<(u32, Kind)>,
    /// This frame's operand stack, **bottom-first**: the buffer slot each position was spilled to,
    /// and its kind. Already trimmed of any arguments belonging to a call this frame made.
    pub stack: Vec<(u32, Kind)>,
}

/// Finished machine code plus what the caller needs to invoke it. Deliberately *not* an
/// [`ExecMem`][crate::burst::exec_mem::ExecMem]: mapping pages is Windows-only, while everything
/// here is plain byte emission, so the compiler and its tests build and run anywhere.
#[derive(Clone, PartialEq, Eq, Debug)]
pub struct CompiledCode {
    /// The machine code, labels resolved — ready for `ExecMem::from_code`.
    pub code: Vec<u8>,
    /// The local slots the body actually reads or writes, ascending. **The marshalling contract**:
    /// the caller must fill exactly these before the call, and may leave every other slot alone —
    /// the generated code provably never reads them.
    ///
    /// This is what lets an instance method with an unused `this` compile: slot 0 holds a
    /// `Value::Reference` the caller could not marshal, but if it is not in this list nothing ever
    /// looks at it.
    pub touched_locals: Vec<u16>,
    /// Number of 8-byte operand-stack slots the native frame reserves: each body's true maximum
    /// depth, recomputed here rather than read from `max_stack`, **summed over every body** — an
    /// inlined callee gets its own disjoint slice ([`Body::frame_base`]). With nothing inlined it
    /// is the root method's own depth, which is all it ever was before step 8.
    pub stack_slots: u32,
    /// Where operand-stack position 0 **of the root method** lands in the caller's buffer when a
    /// resume spills it — i.e. the root's `max_locals`. Each inlined body's spill area follows, and
    /// a [`ResumeSite`] names those slots outright rather than deriving them, so this stays the one
    /// number the interpreter needs for the frame it is already holding.
    pub stack_base: u32,
    /// Where the **allocation log** starts in the caller's buffer: past every body's locals and
    /// spill area. Only meaningful when [`alloc_records`][CompiledCode::alloc_records] is non-zero.
    ///
    /// The log is `[count, offset₀, size₀, offset₁, size₁, …]` — one `i64` holding how many objects
    /// this excursion allocated, then that many `(heap offset, logical size)` pairs, in allocation
    /// order. **The caller must zero the count before every call and replay the records into the
    /// heap's pending log the instant the call returns**, on *every* outcome: a returned method, a
    /// deopt and a safepoint exit have all equally already allocated. That is the third clause of
    /// the marshalling contract, and the one whose omission would be silent — an unlogged object is
    /// one the collector cannot see.
    pub alloc_base: u32,
    /// How many `(offset, size)` records the log has room for — [`ALLOC_LOG_RECORDS`] when the
    /// method contains a `new`, and **0** when it does not, in which case the method carries no log
    /// at all and the caller has nothing to replay.
    pub alloc_records: u32,
    /// Total 8-byte slots the caller's buffer must have: the locals, the operand spill area, and
    /// the allocation log. **The marshalling contract's second half** — a shorter buffer would be
    /// written past the end by a deopt spill or by the first inline allocation.
    pub buffer_slots: u32,
    /// **Loop headers**: the bytecode pcs this code may be entered at on-stack, and the same pcs
    /// at which it polls the safepoint word. Ascending, and always the target of some backward
    /// branch with an operand-stack depth of 0 (see the module docs). Empty for a method with no
    /// loop, or whose only loops carry operands across their back-edge — such a method compiles
    /// and runs exactly as it did before OSR existed.
    pub osr_entries: Vec<u32>,
    /// **The resume map**: every pc at which native code can hand a half-finished method back, with
    /// the state to rebuild there. Ascending by pc, and it is the union of two sets — the loop
    /// headers in [`osr_entries`][CompiledCode::osr_entries] (where the poll fires) and the pcs of
    /// the deopt guards. See [`ResumeSite`].
    ///
    /// This is the half of the type map that leaves the compiler, and it is what replaced
    /// deopt-by-restart: with it the interpreter *continues* a method rather than re-running it,
    /// which is what makes an observable write inside compiled code safe.
    pub resume_sites: Vec<ResumeSite>,
    /// Whether the method's descriptor says it returns a **reference**, i.e. whether the 32 bits
    /// [`Outcome::Returned`] carries are a heap offset rather than an `int`.
    ///
    /// The descriptor is the authority, not the opcode: `ireturn` and `areturn` are already checked
    /// against it at compile time ([`Ineligible::WrongType`]), so the caller never has to inspect
    /// the code to know what it was handed.
    pub returns_reference: bool,
    /// **How many interpreter frames one deopt out of this code can produce**: 1 for a method with
    /// nothing inlined, and one more per level of the deepest inline chain.
    ///
    /// It is part of the entry contract rather than a statistic. The interpreter checks its frame
    /// stack against `MAX_FRAMES` at every invoke, and inlining hides the invokes it expanded — so
    /// without this a compilation entered near the limit could deopt into a stack deeper than the
    /// interpreter would ever have allowed itself to build. The caller refuses to *enter* when the
    /// headroom is short, which is the only point at which refusing is still free.
    pub frame_depth: u32,
    /// Whether the method's descriptor says it returns **`void`**, i.e. whether an
    /// [`Outcome::Returned`] carries *no* value at all and its 32 bits are meaningless.
    ///
    /// Mutually exclusive with [`returns_reference`][CompiledCode::returns_reference], and the two
    /// together are the whole of what the caller needs to interpret a normal exit. Kept as a second
    /// flag rather than folded into an enum because that is the shape the boundary already had, and
    /// a `void` method is the only new case step 7 adds to it.
    pub returns_void: bool,
}

/// The largest method this tier compiles, in bytes of bytecode. A first-tier JIT exists for hot
/// *loops*, which are small; the bound keeps compile time and code size bounded by construction
/// and makes the `Vec`s the scan allocates (one entry per code byte) trivially cheap.
const MAX_CODE_LEN: usize = 4096;

/// The deepest operand stack this tier maps to frame slots. `javac` emits single digits for the
/// arithmetic this subset covers; the bound just stops a pathological class file from asking for
/// a megabyte-deep frame.
const MAX_STACK_SLOTS: u16 = 64;

/// How many objects **one excursion into native code** may allocate before it has to hand the
/// method back (`Status::ALLOC`).
///
/// A compiled `new` cannot push to the heap's pending log itself — that is a `Mutex<Vec<_>>` — so it
/// writes `(offset, size)` into a fixed array in the caller's buffer and the trampoline replays it
/// on the way out. Fixed, because the buffer is allocated once and a compiled call must not
/// allocate; and therefore bounded, because a loop can allocate without limit.
///
/// The number is a trade between two costs, and both are small. Too low and a loop pays a boundary
/// crossing every few iterations; too high and every method with a `new` reserves the space. At 256
/// records the log is 4 KiB — the scratch buffer is grown once, to the largest any compiled method
/// needs, and shared by all of them — and an allocating loop crosses the boundary once per 256
/// objects, which is far less often than it fills Eden.
pub const ALLOC_LOG_RECORDS: u32 = 256;

/// The most cases this tier will turn into a compare chain. A `tableswitch`/`lookupswitch` with
/// more is [`Ineligible::TooBig`] rather than a kilobyte of `cmp`/`je` — the honest answer while
/// the switch has no jump table (see the module docs). A method of [`MAX_CODE_LEN`] bytes cannot
/// hold much more than a thousand cases anyway, so this bites only on the pathological end.
pub(super) const MAX_SWITCH_CASES: usize = 256;

// ---------------------------------------------------------------------------------------------
// The descriptor: where the type map starts, and how it ends.
// ---------------------------------------------------------------------------------------------

/// The kind of one field/argument/return descriptor, and how many local **slots** it occupies.
///
/// `float` is `Opaque` rather than `Int` even though it is category-1 and four bytes wide: the
/// subset has no float opcode, so nothing may read one, and calling it an `Int` would let a
/// hypothetical future `iload` of that slot through. Category-2 (`J`, `D`) is `Opaque` and two
/// slots — the second of which the interpreter leaves as `Value::Int(0)`, and which is therefore
/// `Int` by the same reading as any other slot the frame never filled.
fn descriptor_kind(bytes: &[u8]) -> Option<(Kind, usize, usize)> {
    Some(match bytes.first()? {
        b'B' | b'C' | b'I' | b'S' | b'Z' => (Kind::Int, 1, 1),
        b'F' => (Kind::Opaque, 1, 1),
        b'J' | b'D' => (Kind::Opaque, 2, 1),
        b'L' => (Kind::Reference, 1, bytes.iter().position(|&b| b == b';')? + 1),
        b'[' => {
            // An array descriptor is `[`s followed by one element descriptor; only its *length*
            // matters here, since every array is a reference whatever it holds.
            let dims = bytes.iter().take_while(|&&b| b == b'[').count();
            let (_, _, len) = descriptor_kind(&bytes[dims..])?;
            (Kind::Reference, 1, dims + len)
        }
        _ => return None,
    })
}

/// The **entry state's locals**: what every slot provably holds when the method is entered.
///
/// Two facts, and they are facts about the interpreter rather than assumptions about the caller:
/// a frame is built by `Frame::reset_for_call`, which fills **every** slot with `Value::Int(0)`
/// and then writes the arguments over the leading ones at their category-aware widths; and slot 0
/// of an instance method is the receiver, a reference. So a slot is either an argument, whose kind
/// the descriptor states, or an `Int` — never anything else, and never unknown.
///
/// A descriptor this cannot parse yields all-`Opaque`, which makes the method compile only if it
/// touches no local at all. That is the safe direction for a case that cannot arise from a real
/// class file.
fn entry_locals(method: &Method) -> Vec<Kind> {
    let mut kinds = vec![Kind::Int; method.max_locals];
    let mut place = |slot: usize, kind: Kind| {
        if let Some(cell) = kinds.get_mut(slot) {
            *cell = kind;
        }
    };
    let mut slot = 0;
    if !method.is_static {
        place(0, Kind::Reference); // `this`
        slot = 1;
    }
    let bytes = method.descriptor.as_bytes();
    let Some(open) = bytes.iter().position(|&b| b == b'(') else { return vec![Kind::Opaque; method.max_locals] };
    let mut at = open + 1;
    while at < bytes.len() && bytes[at] != b')' {
        let Some((kind, width, len)) = descriptor_kind(&bytes[at..]) else {
            return vec![Kind::Opaque; method.max_locals];
        };
        place(slot, kind);
        slot += width;
        at += len;
    }
    kinds
}

/// The kind a `return`-family opcode must hand back: what the descriptor says after the `)`.
/// `Opaque` covers `void`, `long`, `double` and `float` — i.e. "no exit in this subset can be
/// taken", which is why such a method compiles only if it never returns at all (it cannot).
fn return_kind(descriptor: &str) -> Kind {
    let bytes = descriptor.as_bytes();
    match bytes.iter().position(|&b| b == b')') {
        Some(close) => descriptor_kind(&bytes[close + 1..]).map_or(Kind::Opaque, |(kind, _, _)| kind),
        None => Kind::Opaque,
    }
}

/// Whether the descriptor returns **`void`** — the one `Opaque` return kind that *does* have an
/// exit in this subset (step 7's `return`, 0xb1).
///
/// [`return_kind`] cannot answer this: it maps `void`, `long`, `double` and `float` all to
/// `Kind::Opaque`, because none of them is a value this tier can carry. But `void` is not a value
/// this tier cannot carry — it is **no value at all**, which is a different thing entirely, and the
/// only reason `return` was out of the subset before is that a `void` method could not have any
/// observable effect until step 6 let the writes in.
fn returns_void(descriptor: &str) -> bool {
    let bytes = descriptor.as_bytes();
    match bytes.iter().position(|&b| b == b')') {
        Some(close) => bytes.get(close + 1..) == Some(b"V"),
        None => false,
    }
}

// ---------------------------------------------------------------------------------------------
// Pass 1: the scan. Decide eligibility, and recover the operand-stack depth at every pc.
// ---------------------------------------------------------------------------------------------

/// The abstract state at one pc: the kind of every local slot and of every live operand-stack
/// position (bottom-first). Its `stack.len()` **is** the operand-stack depth — the number step 2
/// tracked on its own, now carried as part of a richer state rather than beside it.
///
/// Two paths that reach the same pc are **joined** ([`State::join_from`]) rather than compared for
/// equality: the locals meet in the lattice [`Kind`] describes, and the stack must still agree
/// exactly. Until step 9 the whole state was one equality check; what replaced it is one
/// three-outcome join, and the outcome "the state rose" is what makes the walk a real fixed point
/// rather than a single visit per pc.
#[derive(Clone, PartialEq, Eq, Debug)]
struct State {
    locals: Vec<Kind>,
    stack: Vec<Kind>,
}

/// The join of two kinds: equal is itself, anything else is [`Kind::Conflict`], which absorbs.
///
/// A flat lattice, so the height is 2 and a slot can rise at most twice — which is what bounds the
/// fixed point in [`scan_body`].
fn join(a: Kind, b: Kind) -> Kind {
    match a == b {
        true => a,
        false => Kind::Conflict,
    }
}

/// What joining an arriving state into an already-seen one did.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Join {
    /// The seen state already absorbs the arriving one: nothing changed and this edge is done.
    Settled,
    /// A local rose in the lattice. The pc must be re-processed and its successors re-pushed.
    Rose,
}

impl State {
    /// **The merge.** Joins `found` (an arriving state) into `self` (the state already recorded at
    /// `pc`), in place, and says whether anything moved.
    ///
    /// The two halves of a frame get deliberately different treatment, and the reason is the
    /// **write-back** rather than the type system:
    ///
    ///  - A **local** may conflict. It is written back by name, so a conflicted one can simply be
    ///    *skipped* — the interpreter keeps the `Value` it already had. See [`ResumeSite::locals`].
    ///  - An **operand** may not. Its position *is* its identity, so there is no such thing as
    ///    skipping one: a resume site hands back a stack of exactly `depth` values, and a position
    ///    with no kind is a stack that cannot be rebuilt at all. It is also a case that cannot arise
    ///    from bytecode a verifier accepted — the JVMS requires the operand stack types to match at
    ///    every merge, `Top` included — so refusing it costs nothing real. `javac`'s dead-slot shape
    ///    is a *locals* shape.
    ///
    /// `conflicts_allowed` is `false` for a body with **exception handlers**, and that is the one
    /// place this analysis declines to trust the verifier. The licence to skip a conflicted slot is
    /// "nobody reads it before storing to it", and this walk proves that by covering every edge the
    /// interpreter can take out of a resume site — every edge *except* the ones into a handler,
    /// which it does not follow (the same gap that costs a handler method its OSR entries, see
    /// [`Method::has_handlers`]). Rather than lean on the verifier's handler-frame rule to close it,
    /// a body with handlers keeps step 8's behaviour exactly: a conflict is a
    /// [`Ineligible::TypeMismatch`] and the method is lost.
    fn join_from(&mut self, found: &State, pc: usize, conflicts_allowed: bool) -> Result<Join, Ineligible> {
        if self.stack.len() != found.stack.len() {
            return Err(Ineligible::StackMismatch {
                pc,
                seen: self.stack.len() as u16,
                found: found.stack.len() as u16,
            });
        }
        if self.stack != found.stack {
            return Err(Ineligible::TypeMismatch { pc });
        }
        // `locals` has the same length in both by construction (every state descends from
        // `entry_locals`, and nothing resizes it), so `zip` visits every slot.
        let mut moved = Join::Settled;
        for (seen, &arriving) in self.locals.iter_mut().zip(&found.locals) {
            let merged = join(*seen, arriving);
            if merged != *seen {
                if merged == Kind::Conflict && !conflicts_allowed {
                    return Err(Ineligible::TypeMismatch { pc });
                }
                *seen = merged;
                moved = Join::Rose;
            }
        }
        Ok(moved)
    }

    /// Pops one operand, checking it is of `want`ed kind.
    fn pop(&mut self, pc: usize, want: Kind) -> Result<(), Ineligible> {
        match self.stack.pop() {
            Some(kind) if kind == want => Ok(()),
            Some(_) => Err(Ineligible::WrongType { pc }),
            None => Err(Ineligible::StackUnderflow { pc }),
        }
    }

    /// Pops one operand of **either** kind — for the stack shuffles, which move slots without
    /// caring what is in them.
    fn pop_any(&mut self, pc: usize) -> Result<Kind, Ineligible> {
        self.stack.pop().ok_or(Ineligible::StackUnderflow { pc })
    }

    /// The kind of local `slot`, or `Opaque` for one past `max_locals` (which [`decode`] has
    /// already refused, so this is only ever the belt to that brace).
    fn local(&self, slot: u16) -> Kind {
        self.locals.get(slot as usize).copied().unwrap_or(Kind::Opaque)
    }

    /// Reads local `slot`, requiring it to be of `want`ed kind, and pushes it.
    ///
    /// **This is where [`Kind::Conflict`] earns its keep**, by failing this comparison like any
    /// other wrong kind. Nothing special is written for it and nothing needs to be: `want` is only
    /// ever `Int` or `Reference`, so a conflicted slot is refused, and *that refusal is the proof*
    /// that every conflicted slot in a method this compiler accepts is dead where it conflicts.
    fn load(&mut self, pc: usize, slot: u16, want: Kind) -> Result<(), Ineligible> {
        match self.local(slot) == want {
            true => {
                self.stack.push(want);
                Ok(())
            }
            false => Err(Ineligible::WrongType { pc }),
        }
    }

    /// Pops a value of `kind` and stores it into local `slot`, which becomes that kind.
    fn store(&mut self, pc: usize, slot: u16, kind: Kind) -> Result<(), Ineligible> {
        self.pop(pc, kind)?;
        if let Some(cell) = self.locals.get_mut(slot as usize) {
            *cell = kind;
        }
        Ok(())
    }
}

/// **The type map, one instruction at a time**: applies the instruction at `pc` to `state`.
///
/// This is the abstract interpretation the module docs describe — the same walk that recovers the
/// operand-stack depth, carrying the *kind* of every value as well as the count. It is the single
/// source of truth for what each opcode consumes and produces; [`Insn::pops`]/[`Insn::pushes`] are
/// kept alongside as an independent statement of the same shape, and the scan asserts they agree
/// (a `debug_assert` rather than a check, because a disagreement is a compiler bug, not an input).
///
/// Only ever called on an instruction [`decode`] has already accepted, so every operand byte it
/// indexes is known to be present and every local index is known to be inside `max_locals`.
fn transfer(
    state: &mut State,
    method: &Method,
    env: &Environment,
    pc: usize,
    returns: Kind,
    void: bool,
) -> Result<(), Ineligible> {
    use Kind::{Int, Reference};

    let code = method.code;
    let op = code[pc];
    let wrong = Ineligible::WrongType { pc };
    // `iinc` (narrow and wide) is the one local access with no stack traffic at all: it reads and
    // writes the slot in place, so all it can do is *insist* the slot is already an `int`.
    let increment = |state: &State, slot: u16| match state.local(slot) == Int {
        true => Ok(()),
        false => Err(Ineligible::WrongType { pc }),
    };

    match op {
        // --- constants --------------------------------------------------------------------
        0x01 => state.stack.push(Reference), // aconst_null: `null` is the reference 0
        0x02..=0x08 | 0x10 | 0x11 | 0x12 | 0x13 => state.stack.push(Int),

        // --- locals: the `i`/`a` opcode pairs differ *only* in the kind they move -----------
        0x15 => state.load(pc, code[pc + 1] as u16, Int)?,
        0x1a..=0x1d => state.load(pc, (op - 0x1a) as u16, Int)?,
        0x19 => state.load(pc, code[pc + 1] as u16, Reference)?,
        0x2a..=0x2d => state.load(pc, (op - 0x2a) as u16, Reference)?,
        0x36 => state.store(pc, code[pc + 1] as u16, Int)?,
        0x3b..=0x3e => state.store(pc, (op - 0x3b) as u16, Int)?,
        0x3a => state.store(pc, code[pc + 1] as u16, Reference)?,
        0x4b..=0x4e => state.store(pc, (op - 0x4b) as u16, Reference)?,
        0x84 => increment(state, code[pc + 1] as u16)?,
        0xc4 => {
            let slot = u16::from_be_bytes([code[pc + 2], code[pc + 3]]);
            match code[pc + 1] {
                0x15 => state.load(pc, slot, Int)?,
                0x19 => state.load(pc, slot, Reference)?,
                0x36 => state.store(pc, slot, Int)?,
                0x3a => state.store(pc, slot, Reference)?,
                _ => increment(state, slot)?, // wide iinc
            }
        }

        // --- the heap, read-only ------------------------------------------------------------
        // Every one of these produces an `int`; what differs is what it consumes. `getfield` and
        // `arraylength` take the object, `iaload` takes the array **under** the index (JVMS's
        // `arrayref, index` order, so the index pops first).
        // `new` pushes the reference to a freshly allocated, all-zero instance.
        0xbb => state.stack.push(Reference),
        0xb2 => state.stack.push(Int), // getstatic
        0xb4 => {
            state.pop(pc, Reference)?; // getfield
            state.stack.push(Int);
        }
        0xbe => {
            state.pop(pc, Reference)?; // arraylength
            state.stack.push(Int);
        }
        0x2e => {
            state.pop(pc, Int)?; // iaload: index...
            state.pop(pc, Reference)?; // ...over the array
            state.stack.push(Int);
        }

        // --- the heap, written (step 6) -----------------------------------------------------
        // The mirror images of the three reads above, and the pops come off in JVMS's order: the
        // value is always on top, whatever is being written into is underneath it.
        0xb3 => state.pop(pc, Int)?, // putstatic
        0xb5 => {
            state.pop(pc, Int)?; // putfield: the value...
            state.pop(pc, Reference)?; // ...over the receiver
        }
        0x4f => {
            state.pop(pc, Int)?; // iastore: the value...
            state.pop(pc, Int)?; // ...over the index...
            state.pop(pc, Reference)?; // ...over the array
        }

        // --- arithmetic, bits and shifts ----------------------------------------------------
        0x60 | 0x64 | 0x68 | 0x6c | 0x70 | 0x78 | 0x7a | 0x7c | 0x7e | 0x80 | 0x82 => {
            state.pop(pc, Int)?;
            state.pop(pc, Int)?;
            state.stack.push(Int);
        }
        0x74 => {
            state.pop(pc, Int)?; // ineg
            state.stack.push(Int);
        }

        // --- the stack shuffles: permutations, and blind to the kinds they move --------------
        // Each is written as the JVMS spec reads — pop the affected region top-first, push the
        // result bottom-first — so the type permutation and the `mov` permutation in pass 2 are
        // two statements of the same sentence and can be read against each other.
        0x00 => {}
        0x57 => {
            state.pop_any(pc)?;
        }
        0x58 => {
            state.pop_any(pc)?;
            state.pop_any(pc)?;
        }
        0x59 => {
            let s0 = state.pop_any(pc)?;
            state.stack.extend([s0, s0]);
        }
        0x5a => {
            let (s1, s0) = (state.pop_any(pc)?, state.pop_any(pc)?);
            state.stack.extend([s1, s0, s1]);
        }
        0x5b => {
            let (s2, s1, s0) = (state.pop_any(pc)?, state.pop_any(pc)?, state.pop_any(pc)?);
            state.stack.extend([s2, s0, s1, s2]);
        }
        0x5c => {
            let (s1, s0) = (state.pop_any(pc)?, state.pop_any(pc)?);
            state.stack.extend([s0, s1, s0, s1]);
        }
        0x5d => {
            let (s2, s1, s0) = (state.pop_any(pc)?, state.pop_any(pc)?, state.pop_any(pc)?);
            state.stack.extend([s1, s2, s0, s1, s2]);
        }
        0x5e => {
            let (s3, s2) = (state.pop_any(pc)?, state.pop_any(pc)?);
            let (s1, s0) = (state.pop_any(pc)?, state.pop_any(pc)?);
            state.stack.extend([s2, s3, s0, s1, s2, s3]);
        }
        0x5f => {
            let (s1, s0) = (state.pop_any(pc)?, state.pop_any(pc)?);
            state.stack.extend([s1, s0]);
        }

        // --- control flow --------------------------------------------------------------------
        0x99..=0x9e => state.pop(pc, Int)?, // if<cond> against zero
        0x9f..=0xa4 => {
            state.pop(pc, Int)?; // if_icmp<cond>
            state.pop(pc, Int)?;
        }
        0xa5 | 0xa6 => {
            state.pop(pc, Reference)?; // if_acmpeq / if_acmpne
            state.pop(pc, Reference)?;
        }
        0xc6 | 0xc7 => state.pop(pc, Reference)?, // ifnull / ifnonnull
        0xa7 => {}                                // goto
        0xaa | 0xab => state.pop(pc, Int)?,       // tableswitch / lookupswitch

        // --- the exits: checked against the **descriptor**, not against each other ------------
        // `ireturn` in a method that returns a reference (or the other way round) is unverifiable
        // bytecode, and it is also the one mistake that would send the interpreter a heap offset
        // labelled `int`. The descriptor is the authority on both sides of the boundary, so this is
        // where the two are made to agree.
        0xac => {
            if returns != Int {
                return Err(wrong);
            }
            state.pop(pc, Int)?;
        }
        0xb0 => {
            if returns != Reference {
                return Err(wrong);
            }
            state.pop(pc, Reference)?;
        }
        // `return` (step 7): no operand at all, and legal in exactly one kind of method. The
        // descriptor is the authority here as it is for the other two, and `returns_void` is what
        // separates `void` from the other three `Opaque` return kinds.
        0xb1 => {
            if !void {
                return Err(wrong);
            }
        }

        // --- the call (step 8): the arguments become the callee's locals ----------------------
        //
        // This is the one opcode whose type effect is stated by a *different* method's descriptor,
        // and stating it here is what makes the caller and the callee agree by construction. The
        // operands `[depth - arg_slots, depth)` become the callee's locals `[0, arg_slots)`
        // bottom-first, so operand `depth - arg_slots + k` is local `k` — and [`entry_locals`] is
        // the authority on what kind local `k` is, exactly as it is for the root's own entry.
        //
        // Popping top-first with `state.pop` therefore walks the slots **downwards**, and a
        // mismatch is an [`Ineligible::WrongType`] like any other. It is also what refuses a
        // `long`, a `double` or a `float` argument without a special case: such a slot is
        // [`Kind::Opaque`], nothing on the operand stack can ever be `Opaque`, so the pop fails.
        0xb7 | 0xb8 => {
            let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
            let callee = (env.invoke)(method.unit, pc, index).ok_or(Ineligible::Opcode { pc, opcode: op })?;
            let want = entry_locals(&callee.method);
            for k in (0..callee.arg_slots).rev() {
                state.pop(pc, want.get(k).copied().unwrap_or(Kind::Opaque))?;
            }
            if !returns_void(callee.method.descriptor) {
                // A callee returning a `long`, a `double` or a `float` has no exit this tier can
                // express, so it could never have compiled; refusing it *here* is what keeps an
                // `Opaque` off the operand stack, where nothing could ever put it back.
                match return_kind(callee.method.descriptor) {
                    Kind::Opaque => return Err(wrong),
                    kind => state.stack.push(kind),
                }
            }
        }

        _ => return Err(Ineligible::Opcode { pc, opcode: op }),
    }
    Ok(())
}

/// What one whitelisted instruction does, as far as the scan is concerned.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum Flow {
    /// Falls through to the next instruction and nothing else.
    Next,
    /// Falls through *or* branches to the target.
    Branch(usize),
    /// Jumps unconditionally; does not fall through.
    Goto(usize),
    /// A `tableswitch`/`lookupswitch`: jumps to one of many targets and does not fall through.
    /// The targets are **not** carried here — there can be hundreds of them and `Insn` is `Copy`,
    /// so both the scan and the emitter re-read them with [`switch_layout`], which is a pure
    /// function of `(code, pc)` and therefore cannot disagree with itself between the two passes.
    Switch,
    /// Leaves the method.
    Return,
}

/// One decoded instruction.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
struct Insn {
    /// Length in bytes. `u16` rather than `u8` because a `tableswitch` is as long as its table —
    /// bounded only by [`MAX_CODE_LEN`].
    len: u16,
    /// Operands consumed from the stack.
    pops: u16,
    /// Values left on the stack.
    pushes: u16,
    flow: Flow,
}

/// Whether control can reach the byte after this instruction. Written once because both passes ask
/// — the scan to decide whether running off the end is possible, the emitter to decide whether it
/// owes the next instruction a bridging `jmp` — and a disagreement between them would be code that
/// runs into the wrong place.
fn falls_through(flow: Flow) -> bool {
    matches!(flow, Flow::Next | Flow::Branch(_))
}

/// The result of scanning **one** method body: everything pass 2 needs for it, and proof that pass
/// 2 can run on it at all.
///
/// Before step 8 a compilation was exactly one of these and the type was called `Scan`. It is now a
/// node of the **inline tree** — the root method, or a callee expanded into it — and what turned it
/// from a bag of vectors into a frame is the three `_base` fields: a body no longer owns the whole
/// locals buffer and the whole native operand area, it owns a *slice* of each.
struct Body<'a> {
    /// Whose body this is: its code, its `max_locals`, its descriptor, and the [`Unit`] its
    /// constant-pool indices resolve against.
    method: Method<'a>,
    /// **The inline sites of this body**: the pc of each invoke that was expanded, and the index in
    /// the body table of the body it expanded to. An invoke *not* in this map is not in the subset
    /// and made the whole method ineligible, so an empty map means a body with no calls at all.
    children: BTreeMap<usize, usize>,
    /// The body this one was inlined into, and the pc of the invoke that did it. `None` for the
    /// root. This is the **caller chain**, and it is what a deopt walks to rebuild the interpreter
    /// frames above the one it stopped in.
    parent: Option<(usize, usize)>,
    /// How many operand-stack values the call that expanded this body consumed — its arguments and,
    /// for an instance call, the receiver. `0` for the root, which no call expanded.
    ///
    /// It is both halves of the call convention in one number: the caller's operands
    /// `[depth - arg_slots, depth)` become this body's locals `[0, arg_slots)`, and what is left
    /// underneath them — `depth - arg_slots` — is where this body's **result** goes.
    arg_slots: usize,
    /// Where this body's **locals** start in the caller's buffer. Local `i` of this body is buffer
    /// slot `locals_base + i` — so `istore` writes through to the interpreter exactly as it always
    /// has, just at this body's own offset.
    locals_base: u32,
    /// Where this body's **operand stack** starts among the native frame's slots. Operand `k` is
    /// native slot `frame_base + k`.
    frame_base: u32,
    /// Where this body's operand stack is **spilled** into the caller's buffer when a deopt has to
    /// hand this frame back. Operand `k` lands in buffer slot `spill_base + k`.
    spill_base: u32,
    /// The abstract state on entry to each reachable instruction — the kinds of the locals and of
    /// the operand stack, whose depth is `stack.len()`. `None` for a byte that is not a reachable
    /// instruction start.
    state: Vec<Option<State>>,
    /// Reachable instruction starts, ascending.
    order: Vec<usize>,
    /// Decoded form of each reachable instruction, parallel to `order`.
    insns: Vec<Insn>,
    /// Deepest this body's operand stack ever gets.
    max_depth: u16,
    /// Local slots read or written anywhere in this body.
    touched: BTreeSet<u16>,
    /// Targets of **backward** branches whose operand-stack depth is 0 — the loop headers that
    /// become OSR entry points and safepoint poll sites. A `BTreeSet` so the order is the pc
    /// order, which is what the entry dispatch and the interpreter's lookup both want.
    ///
    /// Only ever non-empty for the **root**: an inlined body is refused if it contains a backward
    /// branch at all (see [`scan_body`]), so there is no loop inside one to enter or to poll.
    osr: BTreeSet<usize>,
}

/// What [`switch_layout`] recovers from a `tableswitch`/`lookupswitch`: its **total length** in
/// bytes, the pc of its `default` arm, and its `(match value, target pc)` pairs in table order.
type SwitchLayout = (u16, usize, Vec<(i32, usize)>);

/// Decodes a `tableswitch` (0xaa) or `lookupswitch` (0xab) at `pc` into its **total length**, its
/// `default` target, and its `(match, target)` pairs — everything both passes need.
///
/// Three things here are easy to get wrong and are the whole reason this is one shared function:
///
///  - **Padding.** The operand table starts at the next multiple of 4 *counted from the start of
///    the code array*, so the 0–3 pad bytes depend on `pc`, not on anything local.
///  - **Offsets are 4 bytes and relative to the opcode**, like every other Java branch (and unlike
///    x86's rel32, which is relative to the next instruction).
///  - **`default` is a target too.** Forgetting it in the scan would leave a reachable pc
///    undecoded and un-labelled; forgetting it in the emitter would fall off the end of the chain.
///
/// Being a pure function of `(code, pc)` is load-bearing: the scan and the emitter each call it
/// and must agree, and this is why they cannot fail to.
fn switch_layout(code: &[u8], pc: usize) -> Result<SwitchLayout, Ineligible> {
    let out_of_range = || Ineligible::OutOfRange { pc };
    // The pad is measured from the byte *after* the opcode, to the next 4-byte boundary.
    let base = pc + 1 + (4 - ((pc + 1) % 4)) % 4;
    let word = |at: usize| -> Result<i32, Ineligible> {
        let b = code.get(at..at + 4).ok_or_else(out_of_range)?;
        Ok(i32::from_be_bytes([b[0], b[1], b[2], b[3]]))
    };
    let target = |offset: i32| -> Result<usize, Ineligible> {
        let t = pc as isize + offset as isize;
        match t >= 0 && (t as usize) < code.len() {
            true => Ok(t as usize),
            false => Err(out_of_range()),
        }
    };

    let default = target(word(base)?)?;
    let mut pairs = Vec::new();
    let end = match code[pc] {
        0xaa => {
            let (low, high) = (word(base + 4)?, word(base + 8)?);
            // JVMS §6.5 requires `low <= high`; a class file that says otherwise is malformed, and
            // the subtraction below would underflow on it.
            if low > high {
                return Err(out_of_range());
            }
            // In `i64`, so `high - low + 1` cannot overflow for `low = i32::MIN, high = i32::MAX`.
            let count = (high as i64 - low as i64 + 1) as usize;
            if count > MAX_SWITCH_CASES {
                return Err(Ineligible::TooBig);
            }
            for k in 0..count {
                // `low + k` stays in range: `k < count` means `low + k <= high <= i32::MAX`.
                pairs.push((low + k as i32, target(word(base + 12 + 4 * k)?)?));
            }
            base + 12 + 4 * count
        }
        _ => {
            let npairs = word(base + 4)?;
            if npairs < 0 || npairs as usize > MAX_SWITCH_CASES {
                return Err(match npairs < 0 {
                    true => out_of_range(),
                    false => Ineligible::TooBig,
                });
            }
            for k in 0..npairs as usize {
                pairs.push((word(base + 8 + 8 * k)?, target(word(base + 12 + 8 * k)?)?));
            }
            base + 8 + 8 * npairs as usize
        }
    };
    // `MAX_CODE_LEN` keeps this inside `u16`, but say so rather than assume it.
    let len = u16::try_from(end - pc).map_err(|_| Ineligible::TooBig)?;
    Ok((len, default, pairs))
}

/// Decodes the instruction at `pc`, rejecting anything outside the whitelist.
///
/// Every resolver in [`Environment`] is consulted **here**, at decode time, so a `getstatic` whose
/// class is not initialised or a `getfield` of the wrong shape is an eligibility answer rather than
/// a surprise in pass 2 — and so the generated code carries the resolved answer as an immediate and
/// never touches a constant pool, a metaspace or a class-init state.
fn decode(
    code: &[u8],
    pc: usize,
    method: &Method,
    env: &Environment,
) -> Result<(Insn, Option<i32>), Ineligible> {
    let (op, max_locals) = (code[pc], method.max_locals);
    // Every arm below indexes `code[pc + k]`; check the whole instruction fits first, once.
    let need = |n: usize| -> Result<(), Ineligible> {
        match pc + n <= code.len() {
            true => Ok(()),
            false => Err(Ineligible::OutOfRange { pc }),
        }
    };
    let local = |slot: usize| -> Result<u16, Ineligible> {
        match slot < max_locals && slot <= u16::MAX as usize {
            true => Ok(slot as u16),
            false => Err(Ineligible::LocalOutOfRange { pc, slot }),
        }
    };
    // A branch's 2-byte operand is signed and relative to the *opcode*, not to the next
    // instruction (unlike x86's rel32 — the two conventions differ, which is exactly why the
    // translation happens through named labels rather than arithmetic).
    let target = |code: &[u8]| -> Result<usize, Ineligible> {
        let offset = i16::from_be_bytes([code[pc + 1], code[pc + 2]]) as isize;
        let t = pc as isize + offset;
        match t >= 0 && (t as usize) < code.len() {
            true => Ok(t as usize),
            false => Err(Ineligible::OutOfRange { pc }),
        }
    };
    let simple = |len: u16, pops: u16, pushes: u16| Insn { len, pops, pushes, flow: Flow::Next };
    // A `wide`'s local index is the two bytes after the wrapped opcode.
    let wide_index = |code: &[u8]| u16::from_be_bytes([code[pc + 2], code[pc + 3]]) as usize;

    // A heap-reading opcode needs the VM's heap to be *addressable* by this tier at all: an offset
    // must fit the 32 bits a reference crosses the boundary in, and Eden's boundary must fit the
    // immediate the address computation compares against. Both are properties of the VM's
    // configuration rather than of this method, so they are checked once per such opcode and the
    // whole method goes with them.
    let reachable_heap = || -> Result<(), Ineligible> {
        let heap = env.heap;
        match heap.max_offset != 0 && heap.max_offset <= u32::MAX as usize && heap.eden_end <= i32::MAX as u32 {
            true => Ok(()),
            false => Err(Ineligible::HeapOutOfReach { pc }),
        }
    };

    let insn = match op {
        // --- new: the fast path, or nothing ------------------------------------------------
        // Everything is resolved here and baked in as an immediate: the instance's size, its
        // header word, the Eden cursor's address and the two bounds. If any of them cannot be
        // answered — the class is not initialised, the object is too big to zero inline, this VM
        // has no Eden this tier can reach — the method is refused rather than the opcode escaped.
        0xbb => {
            need(3)?;
            let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
            let instance = (env.instance)(method.unit, index).ok_or(Ineligible::UnresolvedClass { pc, index })?;
            // The reference it pushes crosses the boundary in 32 bits like any other.
            reachable_heap()?;
            alloc_bounds(env.heap, instance.size).ok_or(Ineligible::AllocOutOfReach { pc })?;
            simple(3, 0, 1)
        }
        // --- constants ---------------------------------------------------------------------
        // `aconst_null` is a constant like any other here: `null` is the reference `0`, so it
        // materialises the immediate 0 and the type map is what remembers it is not an `int`.
        0x01 => return Ok((simple(1, 0, 1), Some(0))),
        // iconst_m1 (0x02) .. iconst_5 (0x08): the value is the opcode minus iconst_0.
        0x02..=0x08 => return Ok((simple(1, 0, 1), Some(op as i32 - 0x03))),
        0x10 => {
            need(2)?;
            return Ok((simple(2, 0, 1), Some(code[pc + 1] as i8 as i32)));
        }
        0x11 => {
            need(3)?;
            return Ok((simple(3, 0, 1), Some(i16::from_be_bytes([code[pc + 1], code[pc + 2]]) as i32)));
        }
        // ldc / ldc_w restricted to CONSTANT_Integer — resolved *now*, so the generated code
        // carries an immediate and never looks at a constant pool.
        0x12 | 0x13 => {
            let (len, index) = match op {
                0x12 => {
                    need(2)?;
                    (2u16, code[pc + 1] as u16)
                }
                _ => {
                    need(3)?;
                    (3u16, u16::from_be_bytes([code[pc + 1], code[pc + 2]]))
                }
            };
            let value = (env.int_const)(method.unit, index).ok_or(Ineligible::NonIntegerConstant { pc, index })?;
            return Ok((simple(len, 0, 1), Some(value)));
        }

        // --- locals ------------------------------------------------------------------------
        // The `i` and `a` forms decode identically — same length, same stack effect, and (pass 2)
        // the same 8-byte `mov`. What separates them is only which *kind* the type map moves, so
        // they are paired here rather than duplicated.
        0x1a..=0x1d => {
            local((op - 0x1a) as usize)?; // iload_0..3
            simple(1, 0, 1)
        }
        0x2a..=0x2d => {
            local((op - 0x2a) as usize)?; // aload_0..3
            simple(1, 0, 1)
        }
        0x15 | 0x19 => {
            need(2)?;
            local(code[pc + 1] as usize)?;
            simple(2, 0, 1)
        }
        0x3b..=0x3e => {
            local((op - 0x3b) as usize)?; // istore_0..3
            simple(1, 1, 0)
        }
        0x4b..=0x4e => {
            local((op - 0x4b) as usize)?; // astore_0..3
            simple(1, 1, 0)
        }
        0x36 | 0x3a => {
            need(2)?;
            local(code[pc + 1] as usize)?;
            simple(2, 1, 0)
        }
        0x84 => {
            need(3)?;
            local(code[pc + 1] as usize)?;
            simple(3, 0, 0)
        }

        // --- wide: the same local instructions with a 16-bit operand field -------------------
        // Only these five forms. A `wide lstore`/`wide ret`/… is refused like the narrow opcode it
        // wraps would be — reported as 0xc4 rather than as the inner byte, so the invariant
        // "`Ineligible::Opcode { pc, opcode }` names `code[pc]`" holds for every rejection.
        0xc4 => {
            need(2)?;
            match code[pc + 1] {
                0x15 | 0x19 => {
                    need(4)?;
                    local(wide_index(code))?;
                    simple(4, 0, 1)
                }
                0x36 | 0x3a => {
                    need(4)?;
                    local(wide_index(code))?;
                    simple(4, 1, 0)
                }
                0x84 => {
                    need(6)?;
                    local(wide_index(code))?;
                    simple(6, 0, 0)
                }
                _ => return Err(Ineligible::Opcode { pc, opcode: op }),
            }
        }

        // --- getstatic of an int ------------------------------------------------------------
        // Resolved **now**, to an address baked in as an immediate — so the generated code never
        // touches a constant pool, a metaspace or a class-init state. See the module docs for why
        // that address cannot move and why the class has to be initialised already.
        0xb2 => {
            need(3)?;
            let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
            (env.static_int)(method.unit, index).ok_or(Ineligible::UnresolvedStatic { pc, index })?;
            simple(3, 0, 1)
        }

        // --- putstatic of an int (step 6) -----------------------------------------------------
        // The same resolver, the same baked-in address, and the same requirement that the declaring
        // class be **initialised already** — which is what makes writing through that address sound
        // rather than merely possible: an uninitialised class's mirror may not exist yet, and
        // compiled code has no way to trigger a `<clinit>`. It is the one write in the subset with
        // no guard at all, because there is nothing about a fixed address that can fail.
        0xb3 => {
            need(3)?;
            let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
            (env.static_int)(method.unit, index).ok_or(Ineligible::UnresolvedStatic { pc, index })?;
            simple(3, 1, 0)
        }

        // --- getfield of an int ---------------------------------------------------------------
        // Resolved **now** as well, but to a *byte offset inside the object* rather than to an
        // address: the object's own position is only known at run time, and turning it into an
        // address is what the two-armed [`Heap`] computation in pass 2 does. The resolver refuses
        // anything but a non-`volatile` instance `int`.
        0xb4 => {
            need(3)?;
            reachable_heap()?;
            let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
            let offset = (env.int_field)(method.unit, pc, index).ok_or(Ineligible::UnresolvedField { pc, index })?;
            // The displacement goes into a ModRM byte, so it has to fit a signed 32-bit field.
            i32::try_from(offset).map_err(|_| Ineligible::HeapOutOfReach { pc })?;
            simple(3, 1, 1)
        }

        // --- arraylength / iaload -------------------------------------------------------------
        // Both are reads, and both carry a check the interpreter would have thrown on: a null
        // array, and (for `iaload`) an index outside the length. Neither throws here — they
        // **deopt**, and the interpreter re-runs the method and raises the proper exception.
        0xbe => {
            reachable_heap()?;
            simple(1, 1, 1)
        }
        0x2e => {
            reachable_heap()?;
            simple(1, 2, 1)
        }

        // --- putfield / iastore of an int (step 6) --------------------------------------------
        // Resolved exactly as their reading twins are, and guarded exactly as they are — a null
        // receiver, a null array, an index outside `[0, length)`. What is new is only the
        // **ordering**: every one of those guards is emitted before the store, so a deopt at this pc
        // names an instruction that has not been applied. See the module docs.
        0xb5 => {
            need(3)?;
            reachable_heap()?;
            let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
            let offset = (env.int_field)(method.unit, pc, index).ok_or(Ineligible::UnresolvedField { pc, index })?;
            i32::try_from(offset).map_err(|_| Ineligible::HeapOutOfReach { pc })?;
            simple(3, 2, 0)
        }
        0x4f => {
            reachable_heap()?;
            simple(1, 3, 0)
        }

        // --- arithmetic, bits and shifts ---------------------------------------------------
        // iadd isub imul idiv irem / ishl ishr iushr iand ior ixor: two operands, one result.
        0x60 | 0x64 | 0x68 | 0x6c | 0x70 | 0x78 | 0x7a | 0x7c | 0x7e | 0x80 | 0x82 => simple(1, 2, 1),
        0x74 => simple(1, 1, 1), // ineg

        // --- stack -------------------------------------------------------------------------
        // Every one of these is read as its **category-1** form, which in this subset is not a
        // choice: nothing pushes anything but an `int`. See the module docs — this is the exact
        // assumption the `long` step has to come back and re-examine.
        0x00 => simple(1, 0, 0), // nop
        0x57 => simple(1, 1, 0), // pop
        0x58 => simple(1, 2, 0), // pop2
        0x59 => simple(1, 1, 2), // dup
        0x5a => simple(1, 2, 3), // dup_x1
        0x5b => simple(1, 3, 4), // dup_x2
        0x5c => simple(1, 2, 4), // dup2
        0x5d => simple(1, 3, 5), // dup2_x1
        0x5e => simple(1, 4, 6), // dup2_x2
        0x5f => simple(1, 2, 2), // swap

        // --- control flow ------------------------------------------------------------------
        // `ifnull`/`ifnonnull` (0xc6/0xc7) join the one-operand group and `if_acmpeq`/`if_acmpne`
        // (0xa5/0xa6) the two-operand one: same length, same shape, same emitted comparison. The
        // only thing that distinguishes them from their `int` twins is the kind the map moves —
        // and, for the reference forms, that the comparison is of *identity*, which for an offset
        // is the same machine instruction.
        0x99..=0x9e | 0xc6 | 0xc7 => {
            need(3)?;
            Insn { len: 3, pops: 1, pushes: 0, flow: Flow::Branch(target(code)?) }
        }
        0x9f..=0xa6 => {
            need(3)?;
            Insn { len: 3, pops: 2, pushes: 0, flow: Flow::Branch(target(code)?) }
        }
        0xa7 => {
            need(3)?;
            Insn { len: 3, pops: 0, pushes: 0, flow: Flow::Goto(target(code)?) }
        }
        // tableswitch / lookupswitch: pop the key, jump to one of many places, never fall through.
        // `switch_layout` does all the decoding *and* all the validation, so a malformed table is
        // an `Ineligible` here rather than a surprise in pass 2.
        0xaa | 0xab => {
            let (len, _, _) = switch_layout(code, pc)?;
            Insn { len, pops: 1, pushes: 0, flow: Flow::Switch }
        }
        // ireturn / areturn. Which of the two is legal is decided by the **descriptor**, in
        // [`transfer`], not here — the shape of the instruction is identical.
        0xac | 0xb0 => {
            if op == 0xb0 {
                reachable_heap()?; // a returned reference has to fit the 32 bits of the protocol
            }
            Insn { len: 1, pops: 1, pushes: 0, flow: Flow::Return }
        }
        // `return` — the same exit with nothing to hand back. Whether the method is allowed to take
        // it is again the descriptor's business, in [`transfer`].
        0xb1 => Insn { len: 1, pops: 0, pushes: 0, flow: Flow::Return },

        // --- invokestatic / invokespecial: a call, which this tier **inlines** (step 8) --------
        // Resolved here like every other opcode with an operand, and refused the same way: a `None`
        // from the resolver is an opcode outside the subset, not an error. What the answer fixes is
        // the instruction's *shape* — how many operands the call consumes, which is the callee's
        // arguments plus its receiver, and whether it leaves one behind.
        //
        // It stays `Flow::Next`: from this body's point of view a call is one instruction that
        // falls through to the next, which is exactly what it will be once the callee's body is
        // emitted in between. The callee's own control flow is a graph of its own, walked by its
        // own [`scan_body`], and nothing about it is visible here.
        0xb7 | 0xb8 => {
            need(3)?;
            let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
            let callee = (env.invoke)(method.unit, pc, index).ok_or(Ineligible::Opcode { pc, opcode: op })?;
            let pops = u16::try_from(callee.arg_slots).map_err(|_| Ineligible::TooBig)?;
            let pushes = u16::from(!returns_void(callee.method.descriptor));
            simple(3, pops, pushes)
        }

        _ => return Err(Ineligible::Opcode { pc, opcode: op }),
    };
    Ok((insn, None))
}

/// The locals an instruction reads or writes, if any.
///
/// Only ever called on an instruction [`decode`] has already accepted, so the operand bytes it
/// indexes are known to be there — and the `wide` arm is known to wrap one of the five forms
/// with a local index.
fn touched_local(code: &[u8], pc: usize) -> Option<u16> {
    match code[pc] {
        0x1a..=0x1d => Some((code[pc] - 0x1a) as u16), // iload_0..3
        0x2a..=0x2d => Some((code[pc] - 0x2a) as u16), // aload_0..3
        0x3b..=0x3e => Some((code[pc] - 0x3b) as u16), // istore_0..3
        0x4b..=0x4e => Some((code[pc] - 0x4b) as u16), // astore_0..3
        0x15 | 0x19 | 0x36 | 0x3a | 0x84 => Some(code[pc + 1] as u16),
        0xc4 => Some(u16::from_be_bytes([code[pc + 2], code[pc + 3]])),
        _ => None,
    }
}

/// Walks the control-flow graph from pc 0, decoding as it goes.
///
/// The operand-stack depth is **recomputed**, not read from the `StackMapTable`: the attribute is
/// optional, can be stale, and is exactly the kind of input a JIT must not trust. Re-deriving it
/// costs one pass and turns "the class file said so" into "two paths agree" — a disagreement is an
/// [`Ineligible::StackMismatch`] and the method is simply never compiled.
///
/// Step 5 widens that walk from a *depth* to a whole [`State`]: the same fixed point over the same
/// graph, carrying the **kind** of every local and every operand as well as how many there are.
/// The starting state is not a guess — see [`entry_locals`].
///
/// **Step 9 makes it a fixed point in earnest.** Until then a re-visited pc was compared for
/// equality and the walk moved on, so every pc was processed exactly once; now the arriving state
/// is *joined* into the recorded one ([`State::join_from`]), and a join that moves a local up the
/// lattice sends the walk back through everything downstream. The lattice is flat, so a slot rises
/// at most twice and the whole thing still terminates in a couple of passes — `budget` below is the
/// belt to that brace, and a walk that exceeds it is a compiler bug rather than an input.
/// Step 8 splits this into "one body" (here) and "the tree of them" ([`plan`]). Nothing about the
/// walk changed: it is the same fixed point over the same graph, and `method.unit` is what its
/// resolutions are now qualified by. The one addition is `inlined` — when a body is *not* the root,
/// a backward branch refuses it outright, because an inlined loop would be a loop with no safepoint
/// poll in it (only the root's loop headers get one, and only the root can be entered on-stack).
fn scan_body<'a>(method: Method<'a>, env: &Environment<'a>, inlined: bool) -> Result<Body<'a>, Ineligible> {
    let method = &method;
    let code = method.code;
    if code.is_empty() || code.len() > MAX_CODE_LEN {
        return Err(Ineligible::TooBig);
    }
    let returns = return_kind(method.descriptor);
    let void = returns_void(method.descriptor);
    let mut state: Vec<Option<State>> = vec![None; code.len()];
    let mut decoded: Vec<Option<Insn>> = vec![None; code.len()];
    let mut touched = BTreeSet::new();
    let mut max_depth = 0u16;

    // A conflicted local is only safe where this walk covers every edge the interpreter can take
    // out of a resume site, which is every method *without* exception handlers. See
    // [`State::join_from`].
    let conflicts_allowed = !method.has_handlers;
    // **The fixed point's budget.** The lattice is flat, so each of the `max_locals` slots at each
    // pc can rise at most twice (to `Conflict`, and no further) — every re-visit past that would be
    // a join that is not monotone, i.e. a compiler bug rather than an input. The bound is generous
    // and its only job is to make a non-terminating walk a refused method instead of a hang.
    let mut budget = (code.len() + 1).saturating_mul(2 * method.max_locals + 4);

    let mut work = vec![(0usize, State { locals: entry_locals(method), stack: Vec::new() })];
    while let Some((pc, mut entry)) = work.pop() {
        // Reached before? Then the arriving state is **joined** into the one already recorded. Three
        // outcomes: the stack disagrees and the method is refused; the join changed nothing and this
        // edge is done; or a local rose in the lattice, and everything downstream of `pc` has to be
        // walked again with the wider state. That third outcome is what step 9 added — before it,
        // "reached before" was always `continue`, because the states could only be equal or fatal.
        if let Some(seen) = &mut state[pc] {
            match seen.join_from(&entry, pc, conflicts_allowed)? {
                Join::Settled => continue,
                Join::Rose => entry = seen.clone(),
            }
        }
        budget = match budget.checked_sub(1) {
            Some(left) => left,
            None => return Err(Ineligible::TooBig),
        };

        let (insn, _) = decode(code, pc, method, env)?;
        decoded[pc] = Some(insn);
        if let Some(slot) = touched_local(code, pc) {
            touched.insert(slot);
        }

        let before = entry.stack.len();
        if before < insn.pops as usize {
            return Err(Ineligible::StackUnderflow { pc });
        }
        // The type map is applied to a *copy*, so `state[pc]` keeps the state on **entry** to the
        // instruction — which is what pass 2 reads to know where its operands are and, at a loop
        // header, what the write-back has to say about every local.
        let mut next = entry.clone();
        transfer(&mut next, method, env, pc, returns, void)?;
        debug_assert_eq!(
            next.stack.len(),
            before - insn.pops as usize + insn.pushes as usize,
            "the decoder and the type map disagree about the stack effect at {pc}"
        );
        state[pc] = Some(entry);

        let after = next.stack.len() as u16;
        max_depth = max_depth.max(before as u16).max(after);
        if max_depth > MAX_STACK_SLOTS {
            return Err(Ineligible::TooBig);
        }

        let fallthrough = pc + insn.len as usize;
        match insn.flow {
            Flow::Next => work.push((fallthrough, next)),
            Flow::Branch(t) => {
                work.push((t, next.clone()));
                work.push((fallthrough, next));
            }
            Flow::Goto(t) => work.push((t, next)),
            // Every arm *and* the default — a `default` that never got walked would be a reachable
            // pc with no state, no label and no emitted code.
            Flow::Switch => {
                let (_, default, pairs) = switch_layout(code, pc)?;
                work.extend(pairs.into_iter().map(|(_, t)| (t, next.clone())));
                work.push((default, next));
            }
            Flow::Return => {}
        }
        // A fall-through (or the instruction after a branch) that leaves the code array means the
        // method can run off its own end — never true of verified bytecode, always a reject here.
        if falls_through(insn.flow) && fallthrough >= code.len() {
            return Err(Ineligible::OutOfRange { pc });
        }
    }

    // Collect the reachable starts in address order and check they *tile*: no instruction may
    // begin inside another. A branch into the middle of an instruction would otherwise be
    // decoded twice, at two different boundaries, and pass 2 would emit both.
    let order: Vec<usize> = (0..code.len()).filter(|&pc| state[pc].is_some()).collect();
    let insns: Vec<Insn> = order.iter().map(|&pc| decoded[pc].expect("reachable implies decoded")).collect();
    for (i, &pc) in order.iter().enumerate() {
        let end = pc + insns[i].len as usize;
        if let Some(&next) = order.get(i + 1) {
            if end > next {
                return Err(Ineligible::OverlappingInstructions { pc });
            }
        }
    }

    // The loop headers. A branch is a back-edge when its target precedes it; the target is
    // *eligible* when the operand stack is empty there, because that is the only state the
    // transfer contract can describe with a locals buffer and a pc alone. `state[t]` is `Some`
    // for every branch target (the walk reached it), and its depth is the depth the branch leaves
    // behind — the scan's own agreement check is what makes those two the same number.
    //
    // A method with **exception handlers** gets none, whatever its loops look like: the interpreter
    // can arrive at a header by an edge this walk never followed, so the state here would not be
    // the state there. See [`Method::has_handlers`].
    let mut osr = BTreeSet::new();
    for (i, &pc) in order.iter().enumerate() {
        // A `switch` back-edge is a loop header like any other — a `continue` inside a `switch`
        // inside a loop is exactly that shape — so its arms are considered too.
        let targets: Vec<usize> = match insns[i].flow {
            Flow::Branch(t) | Flow::Goto(t) => vec![t],
            Flow::Switch => {
                let (_, default, pairs) = switch_layout(code, pc)?;
                std::iter::once(default).chain(pairs.into_iter().map(|(_, t)| t)).collect()
            }
            Flow::Next | Flow::Return => continue,
        };
        for target in targets {
            if target >= pc {
                continue;
            }
            // **An inlined body may not loop.** Only the root's headers get a safepoint poll and
            // only the root can be entered on-stack, so a loop expanded inline would be a loop
            // native code cannot be pulled out of — and step 3's poll invariant is not something
            // step 8 is allowed to quietly weaken. Refusing the *callee* costs a call site; letting
            // it through would cost the handshake.
            if inlined {
                return Err(Ineligible::InlineLoop { pc });
            }
            if !method.has_handlers && state[target].as_ref().is_some_and(|s| s.stack.is_empty()) {
                osr.insert(target);
            }
        }
    }

    Ok(Body {
        method: *method,
        children: BTreeMap::new(),
        parent: None,
        arg_slots: 0,
        // The three bases are the **layout**, which is a property of the whole tree rather than of
        // one body: [`plan`] assigns them once every body is known. Zero is the root's answer and
        // the only one that is right by default.
        locals_base: 0,
        frame_base: 0,
        spill_base: 0,
        state,
        order,
        insns,
        max_depth,
        touched,
        osr,
    })
}

// ---------------------------------------------------------------------------------------------
// The inline tree: which callees are expanded, how deep, and where each one's frame lives.
// ---------------------------------------------------------------------------------------------

/// **How deep the inline tree may go.** A callee inlined into a callee inlined into the root is
/// depth 3, and that is the limit.
///
/// Two different things make a bound necessary and only one of them is recursion. A *cycle* —
/// `f` calling itself, or `f`→`g`→`f` — would expand forever, and is caught exactly by the
/// [`Unit`] identity: a callee whose unit is already on the path from the root is refused
/// ([`Ineligible::InlineCycle`]), which terminates even a mutual recursion no depth bound would
/// reach. The depth bound is for the acyclic case, where nothing is wrong except that a chain of
/// small methods multiplies: each level adds its own locals region, its own operand region and its
/// own copy of the callee's code, and three levels is where the ceiling of this tier's frame
/// (`MAX_STACK_SLOTS` operand slots in total) starts to bind anyway.
///
/// Three rather than one because the shape this step exists for is exactly three deep: `new X(a, b)`
/// is `X.<init>`, and `X.<init>` begins with `super()` — `java.lang.Object.<init>`. A smaller bound
/// would inline the constructor and then refuse the `super()` inside it, which is to say it would
/// refuse every allocation `javac` emits. `java/JiNew.class` is that shape, and it compiles.
///
/// **The frontier this draws**, stated because it is the first question anyone will ask of the
/// number: a `new` is inlinable in the root or one level below it, and no deeper. A callee that
/// itself allocates would be root → callee → `<init>` → `Object.<init>`, which is four, so
/// `int f() { return g(); }` inlines but `int f() { return new G().v; }` inlined *into* something
/// else does not ([`Ineligible::InlineDepth`]). Raising it costs only code size, and the honest
/// reason it is not raised here is that this step is already the largest in the milestone.
const MAX_INLINE_DEPTH: usize = 3;

/// **The code-size budget**: the most bytecode, in bytes, that one compilation may expand *in
/// total* across every body in the tree.
///
/// Inlining trades code size for calls, and the trade is only good while the callee is small: a
/// getter is three bytes against a whole frame push, while a 300-byte method inlined at four sites
/// is 1200 bytes of instruction cache bought for four calls. The budget is what makes the trade
/// bounded rather than a judgement — a candidate that does not fit is simply not inlined, and its
/// caller is refused (this tier has no way to *emit* a call, so "do not inline" and "do not
/// compile" are the same answer).
///
/// It is a budget over the **total** rather than a per-callee size limit because that is the
/// quantity that actually matters — `MAX_CODE_LEN` bounds each body, and the sum is what bounds
/// the emitted function.
const MAX_INLINE_BYTES: usize = 1024;

/// The most **bodies** one compilation may contain, the root included. A second, blunter bound
/// than [`MAX_INLINE_BYTES`]: it caps the number of frame regions the buffer layout has to
/// allocate, and therefore the length of the caller's scratch buffer, independently of how small
/// the inlined bodies happen to be.
const MAX_INLINE_BODIES: usize = 16;

/// **Builds the inline tree** and lays out its frames: scan the root, expand every invoke it may
/// expand, and give each body a disjoint slice of the locals buffer and of the native operand area.
///
/// The traversal is breadth-first over a worklist so a body's `children` can be filled in after it
/// has been pushed, which the borrow checker asks for and which happens also to be the order that
/// makes the depth bound easy to state: `depth[b]` is fixed when `b` is pushed.
///
/// # Why the regions are disjoint rather than overlaid
///
/// Two sibling call sites are never live at the same moment — a call completes before the next
/// begins — so their frames *could* share slots. They deliberately do not. An overlay is an
/// argument about liveness that every future opcode would have to be checked against, while
/// disjointness is a property of the arithmetic here and of nothing else; and the cost is bounded
/// by the two budgets above, which is what makes the simple answer affordable.
fn plan<'a>(root: Method<'a>, env: &Environment<'a>) -> Result<Vec<Body<'a>>, Ineligible> {
    let mut bodies = vec![scan_body(root, env, false)?];
    let mut depth = vec![1usize];
    let mut bytes = bodies[0].method.code.len();

    let mut at = 0;
    while at < bodies.len() {
        // The invokes this body contains, in pc order. Each is either expanded or the method is
        // refused — `decode` has already established that nothing else in the body is outside the
        // subset, so an invoke reaching here is one it accepted *pending* a callee.
        let sites: Vec<(usize, u16)> = bodies[at]
            .order
            .iter()
            .filter(|&&pc| is_invoke(bodies[at].method.code[pc]))
            .map(|&pc| (pc, u16::from_be_bytes([bodies[at].method.code[pc + 1], bodies[at].method.code[pc + 2]])))
            .collect();
        for (pc, index) in sites {
            let unit = bodies[at].method.unit;
            let opcode = bodies[at].method.code[pc];
            if depth[at] >= MAX_INLINE_DEPTH {
                return Err(Ineligible::InlineDepth { pc });
            }
            if bodies.len() >= MAX_INLINE_BODIES {
                return Err(Ineligible::TooBig);
            }
            let callee = (env.invoke)(unit, pc, index).ok_or(Ineligible::Opcode { pc, opcode })?;
            // **The cycle check.** Walk the caller chain to the root; a unit that is already on it
            // would expand into itself, and no depth bound is the honest reason to stop.
            let mut up = Some(at);
            while let Some(b) = up {
                if bodies[b].method.unit == callee.method.unit {
                    return Err(Ineligible::InlineCycle { pc });
                }
                up = bodies[b].parent.map(|(caller, _)| caller);
            }
            bytes = bytes.saturating_add(callee.method.code.len());
            if bytes > MAX_INLINE_BYTES {
                return Err(Ineligible::InlineBudget { pc });
            }
            // The **shape** check the emitter depends on and the type map cannot state: the call
            // must consume exactly the operands the callee's locals are built from.
            let entry_depth = bodies[at].state[pc].as_ref().expect("a reachable invoke").stack.len();
            if callee.arg_slots > entry_depth || callee.arg_slots > callee.method.max_locals {
                return Err(Ineligible::StackUnderflow { pc });
            }
            // **One operand, one local slot.** The emitter copies operand `k` to local `k`, which
            // is only the callee's argument layout while every argument is category-1: a `long`
            // occupies one operand and *two* local slots, and the arguments after it would land
            // one slot low. Comparing the VM's operand count against the descriptor's slot width
            // is what catches that, and it catches it for the receiver too.
            if arg_slot_width(&callee.method) != Some(callee.arg_slots) {
                return Err(Ineligible::WrongType { pc });
            }
            let mut body = scan_body(callee.method, env, true)?;
            body.parent = Some((at, pc));
            body.arg_slots = callee.arg_slots;
            let child = bodies.len();
            bodies.push(body);
            depth.push(depth[at] + 1);
            bodies[at].children.insert(pc, child);
        }
        at += 1;
    }

    // The layout. Each body takes the next slice of the caller's buffer — its locals, then the
    // room its operand stack needs when a deopt spills it — and the next slice of the native
    // frame's operand area.
    let (mut buffer, mut frame) = (0u32, 0u32);
    for body in &mut bodies {
        body.locals_base = buffer;
        buffer += body.method.max_locals as u32;
        body.spill_base = buffer;
        buffer += body.max_depth as u32;
        body.frame_base = frame;
        frame += body.max_depth as u32;
    }
    if frame > MAX_STACK_SLOTS as u32 {
        return Err(Ineligible::TooBig);
    }
    Ok(bodies)
}

/// Whether the opcode is an **invoke this tier inlines**: `invokespecial` and `invokestatic`, the
/// two that are statically bound.
///
/// `invokevirtual` and `invokeinterface` bind on the receiver's runtime class, which would need a
/// type guard this tier has no way to emit, and `invokedynamic` needs a call site resolved through
/// a bootstrap method. All three are refused by [`decode`] like any other opcode outside the
/// whitelist. Written once because both [`decode`] and [`plan`] ask, and a disagreement between
/// them would be an invoke with a body that never got emitted.
fn is_invoke(op: u8) -> bool {
    matches!(op, 0xb7 | 0xb8)
}

/// The number of **local slots** a callee's receiver and arguments occupy, or `None` for a
/// descriptor this tier cannot parse.
///
/// It is compared against the operand count the VM reports for the same call, and the comparison is
/// the whole point: the two are equal exactly when every argument is category-1, which is exactly
/// when "operand `k` becomes local `k`" — the only argument-passing rule the emitter implements —
/// is the callee's real layout. A `long` or `double` parameter makes the width larger than the
/// count and the call is refused.
fn arg_slot_width(method: &Method) -> Option<usize> {
    let bytes = method.descriptor.as_bytes();
    let mut slots = usize::from(!method.is_static); // `this`
    let mut at = bytes.iter().position(|&b| b == b'(')? + 1;
    while at < bytes.len() && bytes[at] != b')' {
        let (_, width, len) = descriptor_kind(&bytes[at..])?;
        slots += width;
        at += len;
    }
    Some(slots)
}

// ---------------------------------------------------------------------------------------------
// Pass 2: emission.
// ---------------------------------------------------------------------------------------------

/// Compiles a method body to native code, or explains why it cannot be.
///
/// [`Method`] says what is being compiled — its code, its frame size, and the descriptor that fixes
/// the type map's starting point and its exits. [`Environment`] says what the VM will answer:
/// constants, static addresses, field offsets, where the heap is, and where the safepoint poll word
/// lives. Every address in `Environment` is **baked into the instruction stream as an immediate**,
/// so all of them must stay valid and unmoved for as long as this code can run — see the module
/// docs for the facts that make that true of each.
///
/// The result is a function of signature `extern "system" fn(*mut i64, i64) -> i64` following the
/// [`Status`] protocol; see the module docs for the marshalling contract on the pointer and for
/// what the second argument (the entry pc) means.
pub fn compile<'a>(method: &Method<'a>, env: &Environment<'a>) -> Result<CompiledCode, Ineligible> {
    compile_with_regs(method, env, CACHE_REGS)
}

/// [`compile`] with the size of the **operand-stack register cache** chosen by the caller — see
/// [`CACHE`] and [`operand_home`].
///
/// `regs` is clamped to [`CACHE_REGS`]; `0` turns the allocator off entirely and emits byte-for-byte
/// what step 9 emitted. The parameter exists so the two arms of the measurement are *the same
/// binary at the same addresses*, which is the only way to keep this machine's ±3–12% code-layout
/// noise out of the comparison — the same reason `JVM_JIT` is a runtime flag rather than a `cfg`.
pub fn compile_with_regs<'a>(
    method: &Method<'a>,
    env: &Environment<'a>,
    regs: u32,
) -> Result<CompiledCode, Ineligible> {
    let bodies = plan(*method, env)?;
    let root = &bodies[0];
    // The native frame holds **every** body's operand stack, each in its own slice — see
    // [`Body::frame_base`]. With one body that is the method's own depth, exactly as before.
    let native_slots: u32 = bodies.iter().map(|b| b.max_depth as u32).sum();
    // The cache never reaches past the operand stack this compilation actually has, so a shallow
    // method saves no register it does not use.
    let regs = regs.min(CACHE_REGS).min(native_slots);
    // `POLL` is saved only when something reads it, so a method without loop headers keeps
    // exactly the frame — and the prologue — it had before OSR existed. The **callee-saved** half of
    // the cache (`R12`–`R15`) joins it on the same terms: pushed only when the operand stack is deep
    // enough to reach them, so nothing shallower than five live operands pays a byte for step 10.
    let mut saved: Vec<Reg> = vec![LOCALS];
    if !root.osr.is_empty() {
        saved.push(POLL);
    }
    saved.extend(CACHE[..regs as usize].iter().copied().filter(|r| !r.is_volatile()));
    let frame = super::x64::Frame::new(native_slots, &saved);
    let mut a = Asm::new();

    // One label per reachable instruction start **of each body**, so *any* branch target can be
    // named without the emitter reasoning about x86 displacements at all — `Asm::finish` resolves
    // them. Per body because a pc means nothing on its own once there is more than one code array.
    let mut st = Frames {
        labels: bodies.iter().map(|b| vec![None; b.method.code.len()]).collect(),
        deopt: bodies.iter().map(|b| vec![None; b.method.code.len()]).collect(),
        sites: BTreeSet::new(),
        allocs: BTreeSet::new(),
        // A loop header gets two more names: `osr_labels[pc]` is the instruction itself (where an
        // on-stack entry lands, *past* the poll — so a re-entry always makes at least one iteration
        // of progress even against a poll word that is permanently set), and `exits[pc]` is the
        // stub that returns `Safepoint(pc)`. **Root only**: an inlined body may not loop.
        osr_labels: vec![None; root.method.code.len()],
        exits: vec![None; root.method.code.len()],
        epilogue: a.new_label(),
        regs,
    };
    for (b, body) in bodies.iter().enumerate() {
        for &pc in &body.order {
            st.labels[b][pc] = Some(a.new_label());
        }
    }
    for &pc in &root.osr {
        st.osr_labels[pc] = Some(a.new_label());
        st.exits[pc] = Some(a.new_label());
    }

    frame.prologue(&mut a);
    // The ABI delivers the locals pointer in RCX; park it in the callee-saved register so RCX is
    // free to be the shift count (`shl` and friends can read the count from nowhere else).
    a.mov_rr(LOCALS, frame.arg(0));
    if !root.osr.is_empty() {
        // The entry dispatch. `frame.arg(1)` is RDX, which is also `T2` (scratch, and `cqo`'s
        // output) — so it is consumed here, before a single body instruction can clobber it.
        a.mov_ri(POLL, env.poll_word as i64);
        for &pc in &root.osr {
            a.cmp_ri(frame.arg(1), pc as i32);
            a.jcc(Cond::E, st.osr_labels[pc].expect("every loop header has an OSR label"));
        }
        // Anything else — in practice only 0, the ordinary invocation — falls through to pc 0.
    }

    // Where the **allocation log** starts: just past every body's region. Slot `alloc_base` is the
    // record count, and record `r` is the pair at `alloc_base + 1 + 2r`.
    let alloc_base: i32 = bodies.iter().map(|b| b.method.max_locals as i32 + b.max_depth as i32).sum();

    emit_body(&mut a, &bodies, 0, env, &frame, alloc_base, &mut st)?;

    // The safepoint exit stubs, one per loop header, parked here at the end of the function so a
    // taken poll costs the loop body nothing but the `jcc` — the stub itself never shares a cache
    // line with the loop. Each is two instructions: pack this pc with `Status::SAFEPOINT` and
    // leave through the shared epilogue. Nothing is marshalled out: `istore`/`iinc` wrote straight
    // through to the caller's buffer, so it is already current.
    for &pc in &bodies[0].osr {
        a.bind(st.exits[pc].expect("every loop header has an exit stub"));
        a.mov_ri(T0, Status::safepoint_value(pc as u32));
        a.jmp(st.epilogue);
    }

    // **The deopt stubs**, one per guarded pc, parked here for the same reason the poll exits are:
    // the guard in the body costs one `jcc` and the stub never shares a cache line with it.
    //
    // Each stub does the one thing the old restart protocol never had to: it **materialises the
    // interpreter's state**. The locals are already there — `istore`/`iinc` write straight through
    // to the caller's buffer — so all that is left is the operand stack, which lives in native frame
    // slots that vanish with the frame. It is copied, bottom-first, into the buffer just past the
    // locals, where [`ResumeSite`] says the interpreter will look for it.
    //
    // The depth is the one **on entry to** the instruction, and that is the whole ordering rule:
    // the pc handed back names an instruction that has not run, so the state to rebuild is the state
    // before it. See the module docs.
    //
    // **Step 8 adds the frames above it.** A guard inside an inlined callee has to hand back not
    // one interpreter state but the whole chain, so the stub walks its caller chain to the root and
    // spills each frame's live operands into that frame's own slice of the buffer. Two details
    // carry the correctness:
    //
    //  - a caller's operands are spilled **minus the call's arguments**. Those operands became the
    //    callee's locals, they are already in the callee's region, and re-materialising them on the
    //    caller's stack would push each argument twice;
    //  - every frame's *locals* are already there and nothing copies them. `istore` writes straight
    //    through to the buffer whichever body it is in, and the call itself wrote the callee's.
    //
    // **Step 10 is what makes the spill per-site rather than uniform**, and this is the delicate
    // part of the register cache. An operand now lives wherever [`operand_home`] says, so a stub has
    // to know *which register* holds position `k` of the frame it is rebuilding. It does, and for
    // free: the home is a function of the native slot index, and the depth at this site is already
    // in the type map. So a cached operand costs the stub **one** store instead of a load and a
    // store, and no operand is ever spilled eagerly — the alternative (flush every register before
    // anything that might deopt) would have spilled before every division, field read, array access
    // and allocation, which is to say before most of what the subset does.
    //
    // What the emitter owes in exchange is stated once here and obeyed by every guarded opcode
    // below: **a guard phase may not write a home register.** Guards compute in `T0`/`T1`/`T2` only,
    // and an operand's home is written after the last `jcc` of its instruction. That is the register
    // form of the step 6 order rule, and it is what makes the values these stubs read the values the
    // instruction was entered with.
    for &(b, pc) in &st.sites {
        a.bind(st.deopt[b][pc].expect("a guarded pc has a stub"));
        let (mut cur, mut cur_pc, mut child_args) = (b, pc, 0usize);
        loop {
            let body = &bodies[cur];
            let depth = body.state[cur_pc].as_ref().expect("a guarded pc is reachable").stack.len();
            // Everything below the arguments of the call this frame is in the middle of — for the
            // innermost frame, which is in the middle of nothing, that is its whole stack.
            for k in 0..depth - child_args {
                let home = operand_home(&frame, st.regs, body.frame_base + k as u32);
                let src = in_reg(&mut a, home, T0);
                a.mov_mr(Mem::at(LOCALS, 8 * (body.spill_base as i32 + k as i32)), src);
            }
            let Some((caller, invoke)) = body.parent else { break };
            (child_args, cur, cur_pc) = (body.arg_slots, caller, invoke);
        }
        let key = site_key(&st.sites, b, pc);
        a.mov_ri(
            T0,
            match st.allocs.contains(&(b, pc)) {
                true => Status::alloc_value(key),
                false => Status::deopt_value(key),
            },
        );
        a.jmp(st.epilogue);
    }

    a.bind(st.epilogue);
    frame.epilogue(&mut a);

    let emitted = a.finish().map_err(Ineligible::Assembler)?;
    // **The marshalling contract is the root's**: the interpreter fills the frame it already has,
    // and an inlined body's locals are written by the emitted code itself (the arguments, at the
    // call) rather than by the caller. So this is the root's touched set and no other's.
    let touched_locals: Vec<u16> = bodies[0].touched.iter().copied().collect();
    // The resume map, read straight out of the states the scan already computed: at every pc native
    // code can leave from, the kind of every local the body touches and of every live operand.
    // Nothing is derived here that the fixed point did not already establish, which is the point —
    // there is exactly one type map.
    // The poll exits are root sites by construction (only the root may loop); the guards are
    // wherever they are, and the two are collected into one table keyed by what native code
    // reports. Sorted by key so the table is deterministic and a binary search stays possible.
    let mut resume_sites: Vec<ResumeSite> = bodies[0]
        .osr
        .iter()
        .map(|&pc| (0usize, pc))
        .chain(st.sites.iter().copied())
        .collect::<BTreeSet<_>>()
        .into_iter()
        .map(|(b, pc)| resume_site(&bodies, &st.sites, &touched_locals, b, pc))
        .collect();
    resume_sites.sort_by_key(|s| s.key);
    // **Every resume site must be rebuildable, and that is checked here rather than believed.**
    //
    // Two of the three things a site hands back have no skip: an operand-stack position (its index
    // is its identity) and an inlined frame (which does not exist yet, so all of it has to be
    // written). Only the root frame's locals may be skipped, because the interpreter is already
    // holding that frame and its existing `Value` is the fallback.
    //
    // Step 8 stated this as a `debug_assert` in the compiler and a second `debug_assert!(false)` in
    // `JitCache::resume_state`, whose release-build behaviour was to return `None` — i.e. "the JIT
    // declined", *after native code had already run and already written to the heap*. That is the
    // silent-restart shape step 6 exists to have eliminated. Turning the claim into a compile-time
    // refusal is what closes it: a compilation with an unrebuildable site is never installed, so the
    // reconstruction on the far side has no failure case left to degrade through.
    if let Some(bad) = resume_sites.iter().find(|s| !s.is_rebuildable()) {
        return Err(Ineligible::Unrebuildable { key: bad.key });
    }
    let stack_base = bodies[0].spill_base;
    // The deepest chain a deopt can hand back: the root plus its longest run of expansions.
    let frame_depth = (0..bodies.len()).map(|b| chain_len(&bodies, b)).max().unwrap_or(1) as u32;
    // A method with no `new` carries no log and the caller has nothing to replay — which is every
    // method compiled before this step, so none of them pays a slot for the feature.
    let alloc_records = match st.allocs.is_empty() {
        true => 0,
        false => ALLOC_LOG_RECORDS,
    };
    let log_slots = match alloc_records {
        0 => 0,
        n => 1 + 2 * n,
    };
    Ok(CompiledCode {
        code: emitted,
        touched_locals,
        stack_slots: native_slots,
        stack_base,
        alloc_base: alloc_base as u32,
        alloc_records,
        buffer_slots: alloc_base as u32 + log_slots,
        osr_entries: bodies[0].osr.iter().map(|&pc| pc as u32).collect(),
        resume_sites,
        frame_depth,
        returns_reference: return_kind(method.descriptor) == Kind::Reference,
        returns_void: returns_void(method.descriptor),
    })
}

/// The **labels and stubs** one compilation accumulates, kept apart from the bodies so the emitter
/// can recurse: a body is emitted by [`emit_body`], which calls itself for every callee inlined
/// into it, and everything below is shared across the whole tree.
struct Frames {
    /// One label per reachable instruction start, **per body**. A pc alone names nothing once a
    /// compilation holds more than one code array, which is exactly the mistake this shape prevents.
    labels: Vec<Vec<Option<Label>>>,
    /// The deopt stub of each guarded pc, per body. Created on demand and emitted after the code.
    deopt: Vec<Vec<Option<Label>>>,
    /// Every `(body, pc)` that can deopt, in a deterministic order — the stubs are emitted from it
    /// and it is what the resume map is built from.
    sites: BTreeSet<(usize, usize)>,
    /// The subset of [`sites`][Frames::sites] whose stub reports `Status::ALLOC` rather than
    /// `Status::DEOPT` — the `new`s. Same stub shape, same state, different reason; see
    /// [`Status::ALLOC`] for why the difference is worth a status of its own.
    allocs: BTreeSet<(usize, usize)>,
    /// The **root's** loop headers: where an on-stack entry lands (past the poll).
    /// Root-only, because an inlined body may not contain a loop at all.
    osr_labels: Vec<Option<Label>>,
    /// The stub each of those headers polls out through.
    exits: Vec<Option<Label>>,
    /// The one shared exit: every `return`, every poll and every deopt leaves through it.
    epilogue: Label,
    /// **How many operand-stack positions are register-resident** in this compilation — see
    /// [`operand_home`]. Carried here rather than passed down so [`emit_body`] keeps the argument
    /// count it had, and because it is a property of the whole tree: the mapping is from the
    /// *native* slot index, which is what makes two bodies' caches disjoint by construction.
    regs: u32,
}

/// How many interpreter frames body `b` stands for: 1 for the root, one more per expansion above
/// it. The [`MAX_INLINE_DEPTH`] bound is what keeps this walk short and its result small.
fn chain_len(bodies: &[Body], b: usize) -> usize {
    let mut n = 1;
    let mut at = b;
    while let Some((caller, _)) = bodies[at].parent {
        n += 1;
        at = caller;
    }
    n
}

/// **What native code reports to name the site** `(b, pc)` — see [`ResumeSite::key`].
///
/// A site in the root's body is named by its pc, which is what keeps the poll exits and the OSR
/// entry dispatch speaking bytecode. A site inside an inlined callee cannot be: two guards in one
/// expanded callee share the root's single invoke pc, and even one would collide with a real pc of
/// the root. So it is numbered past [`MAX_CODE_LEN`], which no bytecode pc can reach because
/// [`scan_body`] refuses a body longer than that.
fn site_key(sites: &BTreeSet<(usize, usize)>, b: usize, pc: usize) -> u32 {
    match b {
        0 => pc as u32,
        _ => {
            let n = sites
                .iter()
                .filter(|&&(body, _)| body != 0)
                .position(|&site| site == (b, pc))
                .expect("every inlined site is in the table it is being numbered against");
            MAX_CODE_LEN as u32 + n as u32
        }
    }
}

/// **The whole interpreter state at one resume site**: the root frame, and every frame above it
/// that inlining removed.
///
/// The walk goes from the site *upwards* to the root, because that is the direction the caller
/// chain is stored in, and the result is reversed at the end so the interpreter can push the frames
/// in the order it needs them. Two things are decided here and nowhere else:
///
///  - **which pc each frame resumes at.** The innermost is where the guard fired; every frame above
///    it sits at the invoke that expanded the frame below, which is exactly where the interpreter
///    leaves a caller during a real call.
///  - **how much of each stack is live.** A frame in the middle of a call has already given its
///    arguments away — they are the callee's locals — so its operands stop short of them. Getting
///    this wrong would push every argument twice, once as a local and once as an operand.
fn resume_site(
    bodies: &[Body],
    sites: &BTreeSet<(usize, usize)>,
    touched_locals: &[u16],
    b: usize,
    pc: usize,
) -> ResumeSite {
    let mut frames: Vec<VirtualFrame> = Vec::new();
    let (mut cur, mut cur_pc, mut child_args) = (b, pc, 0usize);
    let (root_pc, root_state) = loop {
        let body = &bodies[cur];
        let at = body.state[cur_pc].as_ref().expect("a resume site is reachable");
        let live = at.stack.len() - child_args;
        let Some((caller, invoke)) = body.parent else { break (cur_pc, at) };
        frames.push(VirtualFrame {
            unit: body.method.unit,
            pc: cur_pc as u32,
            // Every slot, because this frame does not exist yet: the emitted call wrote the
            // arguments and zeroed the rest, so all of them are real.
            locals: (0..body.method.max_locals)
                .map(|i| (body.locals_base + i as u32, at.locals.get(i).copied().unwrap_or(Kind::Int)))
                .collect(),
            stack: (0..live).map(|k| (body.spill_base + k as u32, at.stack[k])).collect(),
        });
        (child_args, cur, cur_pc) = (body.arg_slots, caller, invoke);
    };
    // Collected innermost-first; the interpreter pushes outermost-first.
    frames.reverse();
    ResumeSite {
        key: site_key(sites, b, pc),
        pc: root_pc as u32,
        locals: touched_locals.iter().map(|&slot| root_state.local(slot)).collect(),
        // The root's own stack, minus the arguments of the call it is in the middle of (nothing,
        // when the site is in its own body).
        stack: root_state.stack[..root_state.stack.len() - child_args].to_vec(),
        inlined: frames,
    }
}

/// **Where an inlined body's result goes**: the native frame slot holding the caller's operand
/// stack position the invoke's value belongs at.
///
/// The arithmetic is the calling convention in one line. The caller was `arg_slots` deep in
/// arguments when it reached the invoke, those arguments are now the callee's locals, and what the
/// call leaves behind sits exactly where the bottom-most of them was — `depth - arg_slots`. So a
/// `return` overwrites the first argument's slot, which is precisely what a real frame's teardown
/// would leave the caller looking at.
///
/// Panics for the root, which no call expanded and whose `return` is an exit rather than a value.
fn result_slot(bodies: &[Body], b: usize) -> u32 {
    let (caller, at) = bodies[b].parent.expect("only an inlined body returns into a caller");
    let depth = bodies[caller].state[at].as_ref().expect("a reachable invoke").stack.len();
    bodies[caller].frame_base + (depth - bodies[b].arg_slots) as u32
}

/// Emits **one body** of the inline tree, recursing into every callee inlined inside it.
///
/// This is the loop `compile` used to be, with the three addressing decisions it made implicitly
/// now read out of the body rather than out of "the method": operand `k` is native slot
/// `frame_base + k`, local `i` is buffer slot `locals_base + i`, and a deopt spills operand `k` to
/// buffer slot `spill_base + k`. With one body all three bases are zero, zero and `max_locals`, and
/// the emitted bytes are what they always were.
fn emit_body(
    a: &mut Asm,
    bodies: &[Body],
    b: usize,
    env: &Environment,
    frame: &super::x64::Frame,
    alloc_base: i32,
    st: &mut Frames,
) -> Result<(), Ineligible> {
    let body = &bodies[b];
    let code = body.method.code;
    let unit = body.method.unit;
    let alloc_count = Mem::at(LOCALS, 8 * alloc_base);
    let alloc_first = 8 * (alloc_base + 1);
    // Operand-stack position `k` of **this body** -> native slot `frame_base + k`, which since step
    // 10 is a [`CACHE`] register or a frame slot depending only on that index ([`operand_home`]).
    // The closures exist so each mapping is written down once; `frame.local` panics on an
    // out-of-range slot, which the layout in [`plan`] has already made impossible.
    let (frame_base, locals_base, regs) = (body.frame_base, body.locals_base, st.regs);
    let home = |k: u16| -> Home { operand_home(frame, regs, frame_base + k as u32) };
    let lcl = |i: u16| -> Mem { Mem::at(LOCALS, 8 * (locals_base as i32 + i as i32)) };

    for (i, &pc) in body.order.iter().enumerate() {
        let insn = body.insns[i];
        let d = body.state[pc].as_ref().expect("reachable implies a known state").stack.len() as u16;
        a.bind(st.labels[b][pc].expect("every reachable start has a label"));
        // A loop header polls before its own instruction. Every back-edge to this pc branches to
        // the label just bound, so the poll runs exactly once per iteration; an OSR entry lands on
        // `st.osr_labels[pc]` just below it and therefore skips it.
        if body.osr.contains(&pc) {
            a.mov_rm(T0, Mem::at(POLL, 0));
            a.cmp_ri(T0, 0);
            a.jcc(Cond::Ne, st.exits[pc].expect("every loop header has an exit stub"));
            a.bind(st.osr_labels[pc].expect("every loop header has an OSR label"));
        }
        let op = code[pc];
        // The label every guard of *this* instruction branches to. Created only for the opcodes
        // that have one, so no unbound label is ever left behind; the arms below that never deopt
        // are handed the epilogue, which they never name.
        let deopt = match guards(op) {
            true => {
                let label = a.new_label();
                st.deopt[b][pc] = Some(label);
                st.sites.insert((b, pc));
                // A `new`'s stub is the same stub, reporting a different reason — see
                // [`Status::ALLOC`].
                if op == 0xbb {
                    st.allocs.insert((b, pc));
                }
                label
            }
            false => st.epilogue,
        };

        match op {
            // --- new: bump Eden inline, or hand the method back ------------------------------
            //
            // This is the first opcode in the subset that **allocates**, and the design is the one
            // every real JIT uses: emit only the fast path — a bump of Eden — and leave the moment
            // it does not fit. If the bump succeeds no collection happened, so the invariant the
            // whole tier rests on ("no GC can observe a compiled frame") holds *by construction*
            // rather than by argument. If it fails, the interpreter does the allocation, and it is
            // the interpreter that may collect — with no native frame anywhere on the stack.
            //
            // An interpreted Eden allocation is four things, and three of them are here:
            //
            //   1. **reserve** — one atomic bump of the arena cursor by the 8-rounded stride, and a
            //      bounds check against the capacity. `lock xadd` *is* the arena's `fetch_add`, on
            //      the same word, so a reservation made here can never overlap one made there. When
            //      the check fails the cursor is left past the end, which is exactly what
            //      `EdenArena::alloc` does on failure — so the interpreter, re-executing this very
            //      instruction, fails Eden too and falls to Old by its ordinary path.
            //   2. **zero** the bytes — the fields' default values (JVMS §2.3, §2.4).
            //   3. **write the header** `[class_id | mark]`, `class_id` resolved at compile time and
            //      `mark` left at the zero step 2 just wrote. Byte-for-byte what
            //      `objects_operations::allocate` writes, because an object the collector cannot
            //      type is worse than one it never sees.
            //
            // The fourth is the **pending-log entry**, a `Mutex<Vec<Allocation>>` push that no
            // instruction stream can do. So it is *recorded* — `(offset, size)` into the caller's
            // buffer — and replayed by the trampoline the instant native code returns. See
            // [`CompiledCode::alloc_base`] and `HeapService::log_jit_allocation` for why deferring
            // it is not merely convenient but exactly as sound as doing it here.
            //
            // **Order.** The log-capacity check comes first, before anything has happened at all.
            // The Eden bounds check comes after the bump, because a bump is how you find out — and
            // that is still within the write/pc rule, because the cursor movement it leaves behind
            // is not observable to the program and is precisely the movement the interpreter's own
            // failing allocation would have left. Nothing after the bounds check can leave.
            0xbb => {
                let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
                let instance = (env.instance)(unit, index).ok_or(Ineligible::UnresolvedClass { pc, index })?;
                let (bump, limit) =
                    alloc_bounds(env.heap, instance.size).ok_or(Ineligible::AllocOutOfReach { pc })?;

                // (1) Room in this excursion's log? Nothing has happened yet if not.
                a.mov_rm(T0, alloc_count);
                a.cmp_ri(T0, ALLOC_LOG_RECORDS as i32);
                a.jcc(Cond::Ae, deopt);

                // (2) Reserve. `T1` comes back holding the cursor's previous value — the
                // arena-local offset of our block.
                a.mov_ri(T1, bump as i64);
                a.mov_ri(T2, env.heap.eden_cursor as i64);
                a.lock_xadd_mr(Mem::at(T2, 0), T1);
                a.cmp_ri(T1, limit);
                a.jcc(Cond::A, deopt); // unsigned: a cursor left past the end is a huge number

                // (3) Two derived values, and the distinction between them is the one this module
                // names everywhere: `T2` is the block's **machine address** and `T1` becomes its
                // **heap offset**, which is the reference the program gets. No two-armed
                // `heap_address` here — a reservation is in Eden by construction.
                a.mov_ri(T2, env.heap.eden_base.wrapping_add(env.heap.null_page as usize) as i64);
                a.add_rr(T2, T1);
                a.add_ri(T1, env.heap.null_page as i32);

                // (4) The fields' default values, then the header over the first word.
                a.xor_rr(T0, T0);
                for at in (0..bump).step_by(8) {
                    a.mov_mr(Mem::at(T2, at), T0);
                }
                a.mov_ri(T0, i64::from(instance.class_id));
                a.mov_mr32(Mem::at(T2, 0), T0);

                // (5) The reference is the instruction's result — and the first write to an operand
                // home, after the last guard, as the order rule requires.
                write_home(a, home(d), T1);

                // (6) Record it. `T2` is dead, so it carries the record's address:
                // `LOCALS + 8*(alloc_base + 1) + 16*count`.
                a.mov_rm(T0, alloc_count);
                a.imul_rri(T2, T0, 16);
                a.add_rr(T2, LOCALS);
                a.mov_mr(Mem::at(T2, alloc_first), T1);
                a.mov_ri(T1, i64::from(instance.size));
                a.mov_mr(Mem::at(T2, alloc_first + 8), T1);
                a.add_ri(T0, 1);
                a.mov_mr(alloc_count, T0);
            }

            // --- constants: materialise the immediate, store it at the new top ---------------
            // `aconst_null` (0x01) is here too: `null` is the reference `0`, and a reference is
            // carried in a slot exactly as an `int` is — eight bytes, no tag. What tells the two
            // apart is the type map, and nothing else needs to.
            0x01..=0x08 | 0x10 | 0x11 | 0x12 | 0x13 => {
                let (_, value) = decode(code, pc, &body.method, env)?;
                let value = value.expect("the constant opcodes always decode a value");
                // A sign-extended i32 immediate: the normalisation invariant holds on entry to
                // the stack, by construction. Straight into the operand's home when it has one, so
                // a cached push is *one* instruction rather than a materialise and a store.
                let w = work_reg(home(d));
                a.mov_ri(w, value as i64);
                write_home(a, home(d), w);
            }

            // --- iload / aload, in all their forms -------------------------------------------
            // One 8-byte `mov` either way. A reference is a heap *offset*, which is a small
            // non-negative number, so it needs no normalisation and no separate move; the pair of
            // opcodes exists in the bytecode for the verifier's benefit, not for the machine's.
            0x15 | 0x19 | 0x1a..=0x1d | 0x2a..=0x2d => {
                let i = match op {
                    0x15 | 0x19 => code[pc + 1] as u16,
                    0x1a..=0x1d => (op - 0x1a) as u16,
                    _ => (op - 0x2a) as u16,
                };
                let w = work_reg(home(d));
                a.mov_rm(w, lcl(i));
                write_home(a, home(d), w);
            }

            // --- istore / astore, in all their forms -----------------------------------------
            0x36 | 0x3a | 0x3b..=0x3e | 0x4b..=0x4e => {
                let i = match op {
                    0x36 | 0x3a => code[pc + 1] as u16,
                    0x3b..=0x3e => (op - 0x3b) as u16,
                    _ => (op - 0x4b) as u16,
                };
                let v = in_reg(a, home(d - 1), T0);
                a.mov_mr(lcl(i), v);
            }

            // --- iinc: local += constant, then re-normalise ----------------------------------
            // TRAP 1. `iinc` is the one local write that computes: `Integer.MAX_VALUE` plus one
            // must wrap, and in 64 bits it would not.
            0x84 => {
                let i = code[pc + 1] as u16;
                let delta = code[pc + 2] as i8 as i32;
                a.mov_rm(T0, lcl(i));
                a.add_ri(T0, delta);
                a.movsxd_rr(T0, T0);
                a.mov_mr(lcl(i), T0);
            }

            // --- wide iload / istore / iinc -------------------------------------------------
            // The same three instructions as above with a 16-bit local index (and, for `iinc`, a
            // 16-bit *signed* delta). `decode` has already established that the wrapped opcode is
            // one of these three and that the index is inside `max_locals`.
            0xc4 => {
                let i = u16::from_be_bytes([code[pc + 2], code[pc + 3]]);
                match code[pc + 1] {
                    0x15 | 0x19 => {
                        let w = work_reg(home(d));
                        a.mov_rm(w, lcl(i));
                        write_home(a, home(d), w);
                    }
                    0x36 | 0x3a => {
                        let v = in_reg(a, home(d - 1), T0);
                        a.mov_mr(lcl(i), v);
                    }
                    // wide iinc — TRAP 1 exactly as the narrow form: `add` then `movsxd`.
                    _ => {
                        let delta = i16::from_be_bytes([code[pc + 4], code[pc + 5]]) as i32;
                        a.mov_rm(T0, lcl(i));
                        a.add_ri(T0, delta);
                        a.movsxd_rr(T0, T0);
                        a.mov_mr(lcl(i), T0);
                    }
                }
            }

            // --- getstatic of an int ---------------------------------------------------------
            // A 32-bit **sign-extending** load from a baked-in address. Two things are load-bearing
            // and neither is obvious: the static occupies 4 bytes (the interpreter's `putstatic`
            // writes it with `write_u32`), so a 64-bit load would drag in the neighbouring slot;
            // and `movsxd` is what puts the value into the normalised form every other opcode here
            // assumes of its inputs. `T1` holds the address only for the one instruction that uses
            // it — nothing is live across an opcode boundary.
            0xb2 => {
                let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
                let address = (env.static_int)(unit, index).ok_or(Ineligible::UnresolvedStatic { pc, index })?;
                a.mov_ri(T1, address as i64);
                // `T1` holds the address and is never a [`CACHE`] register, so the load may land
                // straight in the operand's home.
                let w = work_reg(home(d));
                a.movsxd_rm(w, Mem::at(T1, 0));
                write_home(a, home(d), w);
            }

            // --- getfield of an int -----------------------------------------------------------
            // The first opcode that dereferences a value the *program* produced rather than one
            // the compiler resolved, so it is the first that can be handed `null`. It is checked
            // and **deopted**: the interpreter re-runs the method and throws a proper
            // `NullPointerException`, which is sound for the same reason every other deopt is —
            // nothing observable was written, and re-reading the heap reads the same bytes (no
            // other thread runs while native code is on this stack).
            //
            // Then the offset becomes an address ([`heap_address`]) and the field is a 32-bit
            // **sign-extending** load: an `int` field is four bytes (the interpreter writes it with
            // `write_u32`), and `movsxd` is what re-establishes the normalisation invariant.
            0xb4 => {
                let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
                let offset = (env.int_field)(unit, pc, index).ok_or(Ineligible::UnresolvedField { pc, index })?;
                // The guard reads the receiver into scratch and **leaves its home alone**, so the
                // deopt stub above still finds the operand where the resume map says it is.
                read_home(a, T0, home(d - 1));
                a.cmp_ri(T0, 0);
                a.jcc(Cond::E, deopt);
                heap_address(a, env.heap, T0, T1);
                let w = work_reg(home(d - 1));
                a.movsxd_rm(w, Mem::at(T0, offset as i32));
                write_home(a, home(d - 1), w);
            }

            // --- arraylength ------------------------------------------------------------------
            // The same null check, then the `length` word out of the array header. It is written
            // as a `u32` and is never negative, so the sign-extending load is the value *and* the
            // normalisation, exactly as for a field.
            0xbe => {
                read_home(a, T0, home(d - 1));
                a.cmp_ri(T0, 0);
                a.jcc(Cond::E, deopt);
                heap_address(a, env.heap, T0, T1);
                let w = work_reg(home(d - 1));
                a.movsxd_rm(w, Mem::at(T0, env.heap.array_length as i32));
                write_home(a, home(d - 1), w);
            }

            // --- iaload -----------------------------------------------------------------------
            // Two guards rather than one, and both **deopt**: a null array, and an index outside
            // `[0, length)`. The interpreter then re-runs and throws the right one of
            // `NullPointerException` / `ArrayIndexOutOfBoundsException` — this tier never decides
            // *which* exception, only that it cannot proceed.
            //
            // The bounds check is two signed comparisons rather than the usual single unsigned
            // one. The unsigned trick (`index >= length` as `u32` catches negatives too) needs the
            // index zero-extended, and here it arrives sign-extended into 64 bits — where a
            // negative is a huge positive and would pass. Two compares, and no re-normalisation.
            0x2e => {
                // Both operands are read into scratch and neither home is touched until the last
                // `jcc` is behind us — the array's home in particular, which the address
                // computation would otherwise overwrite with an *address* that the deopt stub of
                // the bounds check would then spill as if it were a reference.
                read_home(a, T0, home(d - 2)); // the array reference...
                a.cmp_ri(T0, 0);
                a.jcc(Cond::E, deopt);
                heap_address(a, env.heap, T0, T2);
                read_home(a, T1, home(d - 1)); // ...and the index, on top of it
                a.cmp_ri(T1, 0);
                a.jcc(Cond::L, deopt);
                a.mov_rm32(T2, Mem::at(T0, env.heap.array_length as i32));
                a.cmp_rr(T1, T2);
                a.jcc(Cond::Ge, deopt);
                a.imul_rri(T1, T1, env.heap.int_element as i32);
                a.add_rr(T0, T1);
                let w = work_reg(home(d - 2));
                a.movsxd_rm(w, Mem::at(T0, env.heap.int_array_data as i32));
                write_home(a, home(d - 2), w);
            }

            // --- putstatic of an int ----------------------------------------------------------
            // The subset's first **observable side effect** (step 6), and the simplest of the
            // three: a 4-byte store to an address resolved and baked in at compile time. Four
            // bytes and not eight because that is what a static `int` occupies — the interpreter's
            // own `putstatic` writes it with `write_u32`, and a 64-bit store would take the
            // neighbouring slot with it. `mov_mr32` truncates the 64-bit register to exactly the
            // 32 bits a Java `int` is, which the normalisation invariant makes lossless.
            //
            // No guard, so this instruction is not a deopt site: there is nothing about a fixed
            // address that can fail, and the class was already initialised when the address was
            // resolved (see `decode`).
            0xb3 => {
                let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
                let address = (env.static_int)(unit, index).ok_or(Ineligible::UnresolvedStatic { pc, index })?;
                a.mov_ri(T1, address as i64);
                let v = in_reg(a, home(d - 1), T0);
                a.mov_mr32(Mem::at(T1, 0), v);
            }

            // --- putfield of an int -----------------------------------------------------------
            // **The order is the correctness argument.** The null check comes first and deopts;
            // only then is the store emitted, and nothing after the store can deopt. So a deopt at
            // this pc means the field was not written, and the interpreter re-executing this
            // instruction writes it exactly once. Reverse the two and the interpreter would write
            // it twice — which is precisely why `putfield` was outside the subset until deopt could
            // resume rather than restart.
            0xb5 => {
                let index = u16::from_be_bytes([code[pc + 1], code[pc + 2]]);
                let offset = (env.int_field)(unit, pc, index).ok_or(Ineligible::UnresolvedField { pc, index })?;
                read_home(a, T0, home(d - 2)); // the receiver, under the value
                a.cmp_ri(T0, 0);
                a.jcc(Cond::E, deopt);
                heap_address(a, env.heap, T0, T1);
                let v = in_reg(a, home(d - 1), T1); // the value
                a.mov_mr32(Mem::at(T0, offset as i32), v);
            }

            // --- iastore ----------------------------------------------------------------------
            // `iaload`'s three guards — null array, negative index, index past the length — in the
            // same order and for the same reasons (two signed compares rather than one unsigned
            // one, because the index arrives sign-extended). All three precede the store, so the
            // write/pc rule holds here as it does for `putfield`.
            //
            // `T1` carries the index into the address computation and is dead immediately after the
            // `add`, which is what lets the value be loaded into it rather than into a fourth
            // register this tier does not have.
            0x4f => {
                read_home(a, T0, home(d - 3)); // the array...
                a.cmp_ri(T0, 0);
                a.jcc(Cond::E, deopt);
                heap_address(a, env.heap, T0, T2);
                read_home(a, T1, home(d - 2)); // ...the index...
                a.cmp_ri(T1, 0);
                a.jcc(Cond::L, deopt);
                a.mov_rm32(T2, Mem::at(T0, env.heap.array_length as i32));
                a.cmp_rr(T1, T2);
                a.jcc(Cond::Ge, deopt);
                a.imul_rri(T1, T1, env.heap.int_element as i32);
                a.add_rr(T0, T1);
                let v = in_reg(a, home(d - 1), T1); // ...and the value, on top of both
                a.mov_mr32(Mem::at(T0, env.heap.int_array_data as i32), v);
            }

            // --- binary arithmetic: lhs at d-2, rhs at d-1, result overwrites lhs -------------
            // TRAP 1 again: add/sub/mul are exactly the operations that can leave the 32-bit
            // range, so each is followed by `movsxd`.
            //
            // Since step 10 the result is computed **in the destination operand's own register**
            // when it has one, so `iload a; iload b; iadd` is `mov r8, [a]; mov r9, [b];
            // add r8, r9; movsxd r8, r8` — where before it was four memory accesses on top of the
            // same three operations. The lower position is the destination, and a cached position
            // is always below an uncached one, so `d - 1` is never a register while `d - 2` is not.
            0x60 | 0x64 | 0x68 => {
                let w = work_reg(home(d - 2));
                read_home(a, w, home(d - 2));
                alu_home(a, match op {
                    0x60 => Alu::Add,
                    0x64 => Alu::Sub,
                    _ => Alu::Imul,
                }, w, home(d - 1));
                a.movsxd_rr(w, w);
            }

            // --- bitwise: no normalisation needed (see the module docs for the proof) --------
            0x7e | 0x80 | 0x82 => {
                let w = work_reg(home(d - 2));
                read_home(a, w, home(d - 2));
                alu_home(a, match op {
                    0x7e => Alu::And,
                    0x80 => Alu::Or,
                    _ => Alu::Xor,
                }, w, home(d - 1));
                write_home(a, home(d - 2), w);
            }

            // --- ineg -----------------------------------------------------------------------
            // `-Integer.MIN_VALUE` is `Integer.MIN_VALUE`; in 64 bits `neg` would answer 2^31.
            0x74 => {
                let w = work_reg(home(d - 1));
                read_home(a, w, home(d - 1));
                a.neg(w);
                a.movsxd_rr(w, w);
                write_home(a, home(d - 1), w);
            }

            // --- shifts ---------------------------------------------------------------------
            // TRAP 2. The count is masked to 5 bits *explicitly* — a 64-bit shift would mask to
            // 6 and `x << 33` would not equal `x << 1`. `iushr` additionally zero-extends the
            // low 32 bits first, or `-1 >>> 1` answers -1 instead of Integer.MAX_VALUE.
            //
            // **The `CL` conflict does not arise**, and that is a property of [`CACHE`] rather than
            // of this arm: the count must be in `RCX` and no operand is ever *in* `RCX`, so
            // `shl r9, cl` shifts a cached operand by a count that cannot be the same register.
            // Nothing is moved out of the way and nothing is spilled.
            0x78 | 0x7a | 0x7c => {
                read_home(a, T1, home(d - 1));
                a.and_ri(T1, 31);
                let w = work_reg(home(d - 2));
                match op {
                    0x7c => read_home32(a, w, home(d - 2)), // iushr: zero-extend, then logical shift
                    _ => read_home(a, w, home(d - 2)),
                }
                match op {
                    0x78 => a.shl_cl(w),
                    0x7a => a.sar_cl(w),
                    _ => a.shr_cl(w),
                }
                a.movsxd_rr(w, w);
                write_home(a, home(d - 2), w);
            }

            // --- idiv / irem ----------------------------------------------------------------
            // TRAP 3, both halves. A zero divisor would raise #DE (a Windows structured
            // exception, not an ArithmeticException), so it is checked and **deopted** — the
            // interpreter then runs the method from the start and throws properly. The
            // `INT_MIN / -1` overflow needs no check at all: in 64 bits the quotient 2^31 is
            // representable, and the `movsxd` truncates it to INT_MIN as JLS 15.17.2 requires.
            //
            // **The `RDX:RAX` conflict does not arise either.** `cqo` and `idiv` clobber exactly
            // `RAX` and `RDX`, and neither is in [`CACHE`] — so no cached operand can be sitting in
            // one of them, nothing has to be moved before the division, and the *other* operands
            // live in `R8`–`R15` straight across it. The dividend is loaded into `RAX` and the
            // divisor into `RCX` here as it always was, and the result is moved into the
            // destination's home afterwards, which is the only new instruction.
            0x6c | 0x70 => {
                read_home(a, T1, home(d - 1));
                a.cmp_ri(T1, 0);
                a.jcc(Cond::E, deopt);
                read_home(a, T0, home(d - 2));
                a.cqo(); // sign-extend RAX into RDX:RAX -- idiv's dividend is the pair
                a.idiv(T1);
                let result = if op == 0x6c { T0 } else { T2 }; // quotient in RAX, remainder in RDX
                a.movsxd_rr(result, result);
                write_home(a, home(d - 2), result);
            }

            // --- stack shuffles -------------------------------------------------------------
            // `nop`, `pop` and `pop2` emit nothing at all: the operand stack is a compile-time
            // notion here, so dropping a value is just a smaller depth for the next instruction.
            //
            // The rest are permutations of frame slots. Writing them out one by one rather than
            // through a generic permutation engine is deliberate: the operand slots are both the
            // sources *and* the destinations, so the order of the `mov`s is the correctness
            // argument, and it is different for each. Below, `b` is the bottom of the affected
            // region and `s0..sn` are the values found there on entry, bottom-first.
            //
            // (All six are the **category-1** readings. In this subset that is forced, not chosen
            // — nothing can push a category-2 value. See the module docs.)
            //
            // Step 10 changes none of that reasoning: a home — register or slot — is still one
            // location per position, distinct positions still have distinct homes, and the order of
            // the moves is still what makes each permutation right. Only the addressing mode moved.
            0x00 | 0x57 | 0x58 => {}
            // dup: [s0] -> [s0, s0]
            0x59 => {
                let v = in_reg(a, home(d - 1), T0);
                write_home(a, home(d), v);
            }
            // dup_x1: [s0, s1] -> [s1, s0, s1]. Both sources must be in registers before the
            // first store, since every slot in the region is overwritten.
            0x5a => {
                let b = d - 2;
                read_home(a, T0, home(b));
                read_home(a, T1, home(b + 1));
                write_home(a, home(b), T1);
                write_home(a, home(b + 1), T0);
                write_home(a, home(b + 2), T1);
            }
            // dup_x2: [s0, s1, s2] -> [s2, s0, s1, s2]
            0x5b => {
                let b = d - 3;
                read_home(a, T0, home(b));
                read_home(a, T1, home(b + 1));
                read_home(a, T2, home(b + 2));
                write_home(a, home(b), T2);
                write_home(a, home(b + 1), T0);
                write_home(a, home(b + 2), T1);
                write_home(a, home(b + 3), T2);
            }
            // dup2: [s0, s1] -> [s0, s1, s0, s1]. The bottom two are already right where they
            // belong, so this is a pure copy upwards and clobbers no source.
            0x5c => {
                let b = d - 2;
                read_home(a, T0, home(b));
                read_home(a, T1, home(b + 1));
                write_home(a, home(b + 2), T0);
                write_home(a, home(b + 3), T1);
            }
            // dup2_x1: [s0, s1, s2] -> [s1, s2, s0, s1, s2]
            0x5d => {
                let b = d - 3;
                read_home(a, T0, home(b));
                read_home(a, T1, home(b + 1));
                read_home(a, T2, home(b + 2));
                write_home(a, home(b), T1);
                write_home(a, home(b + 1), T2);
                write_home(a, home(b + 2), T0);
                write_home(a, home(b + 3), T1);
                write_home(a, home(b + 4), T2);
            }
            // dup2_x2: [s0, s1, s2, s3] -> [s2, s3, s0, s1, s2, s3].
            //
            // Four live sources and three scratch registers, so this one spills — but it spills
            // *into its own result*: slots b+4 and b+5 are above the source region, they are
            // untouched on entry, and their final contents are s2 and s3. So writing them first
            // is simultaneously the backup and the answer, and nothing extra is needed.
            0x5e => {
                let b = d - 4;
                read_home(a, T0, home(b + 2)); // s2
                read_home(a, T1, home(b + 3)); // s3
                write_home(a, home(b + 4), T0); // final top pair, and the only copy of s2/s3 now
                write_home(a, home(b + 5), T1);
                read_home(a, T0, home(b)); // s0
                read_home(a, T1, home(b + 1)); // s1
                write_home(a, home(b + 2), T0);
                write_home(a, home(b + 3), T1);
                read_home(a, T0, home(b + 4)); // s2 back out of its own result home
                read_home(a, T1, home(b + 5)); // s3
                write_home(a, home(b), T0);
                write_home(a, home(b + 1), T1);
            }
            // swap: [s0, s1] -> [s1, s0]
            0x5f => {
                read_home(a, T0, home(d - 2));
                read_home(a, T2, home(d - 1));
                write_home(a, home(d - 2), T2);
                write_home(a, home(d - 1), T0);
            }

            // --- branches -------------------------------------------------------------------
            // Java's `if_icmp*` are *signed* comparisons, so the signed condition codes are the
            // only correct ones: `Cond::B` would rank -1 above 1.
            //
            // The four reference forms fold in here without a line of their own: `if_acmpeq`/
            // `if_acmpne` are the two-operand shape with `E`/`Ne`, and `ifnull`/`ifnonnull` are the
            // against-zero shape with `E`/`Ne`. Reference *identity* is equality of heap offsets,
            // and `null` is the offset 0, so both are the comparisons already emitted here — only
            // the type map knows the difference, which is precisely where the difference matters.
            0x99..=0xa6 | 0xc6 | 0xc7 => {
                let cond = match op {
                    0x9f | 0x99 | 0xa5 | 0xc6 => Cond::E,
                    0xa0 | 0x9a | 0xa6 | 0xc7 => Cond::Ne,
                    0xa1 | 0x9b => Cond::L,
                    0xa2 | 0x9c => Cond::Ge,
                    0xa3 | 0x9d => Cond::G,
                    _ => Cond::Le,
                };
                match op {
                    0x9f..=0xa6 => {
                        // Both cached, this is one `cmp r8, r9` and no memory traffic at all —
                        // which is what a loop condition costs from step 10 on.
                        let l = in_reg(a, home(d - 2), T0);
                        alu_home(a, Alu::Cmp, l, home(d - 1));
                    }
                    _ => {
                        let l = in_reg(a, home(d - 1), T0);
                        a.cmp_ri(l, 0);
                    }
                }
                let Flow::Branch(t) = insn.flow else { unreachable!("a conditional branch has a target") };
                a.jcc(cond, st.labels[b][t].expect("a reachable branch target has a label"));
            }
            0xa7 => {
                let Flow::Goto(t) = insn.flow else { unreachable!("goto has a target") };
                a.jmp(st.labels[b][t].expect("a reachable branch target has a label"));
            }

            // --- tableswitch / lookupswitch -------------------------------------------------
            // A compare chain: one `cmp`/`je` per case in table order, then an unconditional jump
            // to `default`. The key is loaded **once**, into T0, and `cmp reg, imm` leaves it
            // untouched, so the chain costs one load however long it is.
            //
            // `default` is emitted as a real `jmp` rather than left to fall through: a switch does
            // not fall through, and the next instruction in address order is usually the first arm
            // of the switch itself.
            //
            // A `tableswitch` whose arms are contiguous still gets the chain — see the module docs
            // for why there is no jump table yet. Correct and O(cases), rather than clever and
            // needing an assembler feature that does not exist.
            0xaa | 0xab => {
                let (_, default, pairs) = switch_layout(code, pc)?;
                // `cmp reg, imm` leaves the key untouched, so a cached key needs no load at all.
                let key = in_reg(a, home(d - 1), T0);
                for (value, t) in pairs {
                    a.cmp_ri(key, value);
                    a.jcc(Cond::E, st.labels[b][t].expect("a reachable switch arm has a label"));
                }
                a.jmp(st.labels[b][default].expect("the switch default is reachable and has a label"));
            }

            // --- ireturn / areturn ------------------------------------------------------------
            // The whole OK path in one instruction: a 32-bit load zero-extends, which puts the
            // value in the low half and status 0 (`Status::OK`) in the high half at once.
            //
            // **The same instruction returns a reference.** A reference is a heap offset, so the
            // zero-extension that carries an `int`'s bit pattern carries an offset's *value* — and
            // `decode` has already refused the method if this VM's heap could ever exceed the 32
            // bits available (`Ineligible::HeapOutOfReach`). Which of the two the caller has been
            // handed is [`CompiledCode::returns_reference`], read from the descriptor, so the
            // question is never asked of the bits themselves.
            //
            // **An inlined body's return is not an exit** (step 8). There is no boundary to cross
            // and no status to pack: the value is written where the invoke's result belongs on the
            // *caller's* operand stack — an ordinary 8-byte slot move, since it stays an operand —
            // and control jumps past the invoke. That is the whole of the return half of a call.
            0xac | 0xb0 => match body.parent {
                None => {
                    // The 32-bit read is the value *and* `Status::OK` in the high half; from a
                    // cached operand it is `mov eax, r9d`, whose zero-extension says the same thing.
                    read_home32(a, T0, home(d - 1));
                    a.jmp(st.epilogue);
                }
                Some((caller, at)) => {
                    // The result lands on the **caller's** operand stack, so its home is the
                    // caller's — one register-to-register move when both are cached.
                    let v = in_reg(a, home(d - 1), T0);
                    let into = operand_home(frame, regs, result_slot(bodies, b));
                    write_home(a, into, v);
                    a.jmp(st.labels[caller][at + 3].expect("the instruction after an invoke is reachable"));
                }
            },

            // --- return (void) ----------------------------------------------------------------
            // There is no value, so the whole of the exit is the status word: `Status::OK` is 0 and
            // the low half is ignored by the caller (a `void` method's `Outcome::Returned` carries
            // nothing — see [`CompiledCode::returns_void`]). Zeroing the register outright, rather
            // than leaving whatever the last opcode happened to put there, keeps the status half
            // provably `OK` without depending on any of them.
            //
            // Inlined, it is the same jump past the invoke with nothing written: a `void` call
            // leaves the caller's operand stack exactly as deep as the arguments left it.
            0xb1 => match body.parent {
                None => {
                    a.xor_rr(T0, T0);
                    a.jmp(st.epilogue);
                }
                Some((caller, at)) => {
                    a.jmp(st.labels[caller][at + 3].expect("the instruction after an invoke is reachable"))
                }
            },

            // --- invokestatic / invokespecial: the callee's body, expanded here ----------------
            //
            // A call in this tier is not a call. There is no `call` instruction, no second frame,
            // no ABI and no unwind data — the callee's body is emitted **in place**, and what is
            // left of the calling convention is the two things a frame would otherwise have done:
            //
            //  1. **The arguments become the callee's locals.** Operand `d - args + k` of this body
            //     is local `k` of the callee's, bottom-first — the same order (and, checked in
            //     [`plan`], the same widths) the interpreter's `push_frame_locked` uses.
            //  2. **Every other local is zero**, which is not tidiness but the interpreter's own
            //     `Frame::reset_for_call` restated: a fresh frame's non-argument slots hold
            //     `Value::Int(0)`, [`entry_locals`] is what says so, and the type map for the
            //     callee's body was computed against exactly that claim. Leaving whatever the last
            //     expansion wrote would make the claim false for the second call at the same site.
            //
            // The callee's own `return` is what jumps back — see the return arms above. Nothing is
            // emitted *after* the body here, so the caller's next instruction is the next thing in
            // the stream and every `return` lands on it.
            //
            // **The order rule holds across the boundary.** Neither of the two steps above is
            // observable to the program: they write this compilation's own buffer regions, never
            // the heap. So the first observable effect inside the expansion is still the callee's
            // own first effect, still preceded by its own guards, and a deopt anywhere in the
            // callee still names an instruction that has not run.
            0xb7 | 0xb8 => {
                let child = *body.children.get(&pc).expect("plan expanded every invoke it accepted");
                let (base, locals) = (bodies[child].locals_base as i32, bodies[child].method.max_locals);
                let args = bodies[child].arg_slots as u16;
                for k in 0..args {
                    let v = in_reg(a, home(d - args + k), T0);
                    a.mov_mr(Mem::at(LOCALS, 8 * (base + k as i32)), v);
                }
                if (args as usize) < locals {
                    a.xor_rr(T0, T0);
                    for i in args as usize..locals {
                        a.mov_mr(Mem::at(LOCALS, 8 * (base + i as i32)), T0);
                    }
                }
                emit_body(a, bodies, child, env, frame, alloc_base, st)?;
            }

            _ => return Err(Ineligible::Opcode { pc, opcode: op }),
        }

        // Fall-through must land on the instruction emitted next. It always does for `javac`
        // output (reachable code is contiguous), but a gap — created by unreachable bytes between
        // two reachable instructions — would silently run the wrong code, so bridge it explicitly.
        let next_emitted = body.order.get(i + 1).copied();
        if falls_through(insn.flow) && next_emitted != Some(pc + insn.len as usize) {
            let t = pc + insn.len as usize;
            a.jmp(st.labels[b][t].ok_or(Ineligible::OutOfRange { pc })?);
        }
    }
    Ok(())
}

/// Whether the opcode at a pc emits **deopt guards** — i.e. whether that pc needs a stub and a
/// [`ResumeSite`].
///
/// Written as one list rather than discovered from the emitter because it is half of an invariant
/// the emitter cannot state on its own: a pc that branches to a deopt stub must be in the resume
/// map, and a pc in the map must be one the emitter can actually leave from. Both passes read this.
fn guards(op: u8) -> bool {
    matches!(
        op,
        0x6c | 0x70 // idiv / irem: a zero divisor
        | 0xb4      // getfield: a null receiver
        | 0xbe      // arraylength: a null array
        | 0x2e      // iaload: a null array, or an index out of range
        | 0xb5      // putfield: a null receiver
        | 0x4f      // iastore: a null array, or an index out of range
        | 0xbb      // new: Eden full, or this excursion's allocation log full
    )
}

/// Turns the heap **offset** in `reg` into a machine address, in place, clobbering `scratch`.
///
/// This is the one place offsets become addresses, and it is six bytes of reasoning rather than
/// one: this VM's heap is two buffers — Eden is a separate lock-free arena, the survivor spaces and
/// Old share one pre-reserved `Vec` — so which base an offset belongs to depends on the offset.
/// Both bases in [`Heap`] are pre-biased, so each arm is a plain `base + offset` and the whole
/// thing is a compare, a pair of `mov`s and an `add`:
///
/// ```text
///     cmp  reg, eden_end        ; unsigned: an offset is never negative
///     mov  scratch, eden_base   ; `mov` does not touch the flags
///     jb   have_base
///     mov  scratch, other_base
///   have_base:
///     add  reg, scratch
/// ```
///
/// A `cmov` would save the branch and the label, and this assembler has no encoding for one yet —
/// the same reason the switches are compare chains. It is also the obvious next optimisation here,
/// together with parking the two bases in callee-saved registers for the whole body instead of
/// re-materialising a 10-byte `movabs` per access.
///
/// **Nothing checks that the offset names a live object**, and nothing can: that is the caller's
/// invariant, and it is the interpreter's type system plus the fact that no collection can run
/// while native code is on this stack.
///
/// Two things the emitted loads deliberately do *not* do, and both are worth stating because the
/// interpreter's equivalents do them: they are not atomic, and they do not assume alignment. Not
/// atomic is sound because nothing else is running — the interpreter's Eden accessors are atomic
/// only to be sound under the `os` parallel substrate, where the JIT is switched off. Not aligned
/// matters because the survivor/Old buffer is a `Vec<u8>` whose base carries no alignment promise;
/// x86-64 loads at any alignment, and the value is the same bytes the interpreter's
/// `u32::from_le_bytes` over a slice would read.
/// The largest object this tier allocates inline, in bytes.
///
/// A compiled `new` zeroes its object with a straight run of 8-byte stores — no loop, because a
/// loop would need a counter register nothing here has spare, and no `rep stosq`, because that
/// clobbers three fixed registers. So the code size of a `new` is linear in the object's size, and
/// this is what keeps it bounded: 512 bytes is 64 stores, which covers every object `javac` emits
/// for ordinary code (127 `int` fields, or 63 `long`s) and refuses the pathological tail.
const MAX_INLINE_ALLOC: u32 = 512;

/// The two immediates a compiled `new` of a `size`-byte object needs: the **stride** the arena bumps
/// its cursor by, and the largest cursor value a reservation of that stride may read back and still
/// be inside Eden. `None` when this tier must not allocate inline at all.
///
/// Both numbers are the arena's own, restated: [`EdenArena::alloc`] bumps by `(n + 7) & !7` and
/// fails when the value it read back is greater than `capacity - bump`. Emitting that comparison
/// with any other constant would let compiled code hand out a block the interpreter thinks is past
/// the end, or refuse one it thinks is fine.
///
/// The capacity used is the **smaller** of the arena's own and Eden's declared extent
/// (`eden_end - null_page`). Those two can differ — the arena rounds its size up to a multiple of
/// eight — and an object allocated in the gap would have a heap offset at or past `eden_end`, which
/// [`heap_address`] would then translate against the *wrong base*. Taking the minimum is what makes
/// "every reference this code produces is an Eden offset" true by arithmetic rather than by luck.
///
/// [`EdenArena::alloc`]: crate::jvm::interpreter::eden_arena::EdenArena::alloc
fn alloc_bounds(heap: Heap, size: u32) -> Option<(i32, i32)> {
    if heap.eden_cursor == 0 || size == 0 || size > MAX_INLINE_ALLOC {
        return None;
    }
    let bump = ((size as usize).checked_add(7)?) & !7;
    let capacity = heap.eden_capacity.min((heap.eden_end as usize).checked_sub(heap.null_page as usize)?);
    let limit = capacity.checked_sub(bump)?;
    // Both are `cmp r64, imm32` operands, and `limit` is compared **unsigned** — which is only the
    // right comparison while it is small enough to be a non-negative `i32`.
    Some((i32::try_from(bump).ok()?, i32::try_from(limit).ok()?))
}

fn heap_address(a: &mut Asm, heap: Heap, reg: Reg, scratch: Reg) {
    let have_base = a.new_label();
    a.cmp_ri(reg, heap.eden_end as i32);
    a.mov_ri(scratch, heap.eden_base as i64);
    a.jcc(Cond::B, have_base);
    a.mov_ri(scratch, heap.other_base as i64);
    a.bind(have_base);
    a.add_rr(reg, scratch);
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A constant pool with no integers in it — the default for tests that use no `ldc`.
    fn no_constants(_: u16) -> Option<i32> {
        None
    }

    /// A poll word for the tests below, which read the emitted bytes but never run them. A real
    /// `static` rather than a fabricated integer, so the immediate `compile` bakes in is a genuine
    /// address — and one whose lifetime is the process, which is the property the emitted code
    /// depends on.
    static TEST_POLL: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(0);

    /// [`super::compile`] for a `static` method returning an `int`, against [`TEST_POLL`] and a
    /// heap no program here reads. Shadows the glob-imported name so the tests below read exactly
    /// as they did before the signature grew.
    fn compile(
        code: &[u8],
        max_locals: usize,
        int_const: &dyn Fn(u16) -> Option<i32>,
    ) -> Result<CompiledCode, Ineligible> {
        compile_as(code, max_locals, "()I", true, int_const)
    }

    /// [`compile`] for a method of some other shape — the descriptor is what fixes the kinds of the
    /// entry locals and of the exit, so a test about references has to state one.
    fn compile_as(
        code: &[u8],
        max_locals: usize,
        descriptor: &str,
        is_static: bool,
        int_const: &dyn Fn(u16) -> Option<i32>,
    ) -> Result<CompiledCode, Ineligible> {
        super::compile(
            &Method { unit: 0, code, max_locals, descriptor, is_static, has_handlers: false },
            &Environment {
                int_const: &|_, index| int_const(index),
                static_int: &|_, _| None,
                int_field: &|_, _, _| None,
                instance: &|_, _| None,
                invoke: &|_, _, _| None,
                heap: Heap::default(),
                poll_word: &TEST_POLL as *const _ as usize,
            },
        )
    }

    #[test]
    fn a_trivial_method_compiles() {
        // iconst_1; ireturn
        let c = compile(&[0x04, 0xac], 0, &no_constants).unwrap();
        assert!(!c.code.is_empty());
        assert_eq!(c.stack_slots, 1);
        assert!(c.touched_locals.is_empty());
    }

    #[test]
    fn one_foreign_opcode_disqualifies_the_whole_method() {
        // iload_0; invokestatic #0; ireturn -- a call is outside the subset, so the method is
        // rejected outright rather than compiled up to that point.
        let err = compile(&[0x1a, 0xb8, 0x00, 0x00, 0xac], 1, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::Opcode { pc: 1, opcode: 0xb8 });
        // The same for a monitor, a `long` load, and a `wide` wrapping something unsupported.
        for op in [0xbfu8, 0x16, 0xc2] {
            let err = compile(&[0x03, op, 0x00, 0x00, 0xac], 1, &no_constants).unwrap_err();
            assert_eq!(err, Ineligible::Opcode { pc: 1, opcode: op }, "0x{op:02x} must be rejected");
        }
        // `return` (0xb1) is in the subset since step 7, but only in a `void` method — here the
        // descriptor says `()I`, so it is a type error rather than an unknown opcode.
        let err = compile(&[0x03, 0xb1, 0xac], 1, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::WrongType { pc: 1 }, "`return` in a non-void method");
        // `new` (0xbb) is also in the subset since step 7, and it is *conditionally* in: this
        // environment resolves no class, so it is refused for that rather than as a foreign opcode.
        let err = compile(&[0xbb, 0x00, 0x02, 0x03, 0xac], 1, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::UnresolvedClass { pc: 0, index: 2 });
        // The four heap opcodes are *conditionally* in the subset, so their rejections have their
        // own shapes. `getstatic`/`putstatic` need a resolver that answers with an address, and
        // this one answers nothing...
        for op in [0xb2u8, 0xb3] {
            let err = compile(&[0x1a, op, 0x00, 0x01, 0xac], 1, &no_constants).unwrap_err();
            assert_eq!(err, Ineligible::UnresolvedStatic { pc: 1, index: 1 }, "0x{op:02x}");
        }
        // ...and `putfield`/`iastore` additionally need a heap this tier can address, which
        // `Heap::default` deliberately is not.
        let err = compile(&[0x03, 0x03, 0xb5, 0x00, 0x01, 0xac], 1, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::HeapOutOfReach { pc: 2 });
        let err = compile(&[0x03, 0x03, 0x03, 0x4f, 0x03, 0xac], 1, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::HeapOutOfReach { pc: 3 });
    }

    /// Step 7's first widening: a `void` method is an exit at last.
    #[test]
    fn a_void_method_compiles_and_returns_no_value() {
        // `return` alone — `java.lang.Object.<init>` is literally this method.
        let c = compile_as(&[0xb1], 1, "()V", false, &no_constants).unwrap();
        assert!(!c.code.is_empty());
        assert!(c.returns_void);
        assert!(!c.returns_reference);
        // And the shape that made it worth having: a `void` method with an observable effect.
        // iload_0; putstatic #1; return — refused only because this environment resolves nothing.
        let err = compile_as(&[0x1a, 0xb3, 0x00, 0x01, 0xb1], 1, "(I)V", true, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::UnresolvedStatic { pc: 1, index: 1 });
        // `ireturn` in a `void` method is the mirror image of the check above.
        let err = compile_as(&[0x03, 0xac], 1, "()V", true, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::WrongType { pc: 1 });
    }

    #[test]
    fn ldc_is_accepted_only_for_integer_constants() {
        // ldc #1; ireturn, with #1 an Integer -> compiles.
        assert!(compile(&[0x12, 0x01, 0xac], 0, &|i| (i == 1).then_some(90_000)).is_ok());
        // The same bytes with #1 a String (the resolver answers None) -> rejected.
        let err = compile(&[0x12, 0x01, 0xac], 0, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::NonIntegerConstant { pc: 0, index: 1 });
    }

    #[test]
    fn only_the_locals_actually_used_are_in_the_marshalling_contract() {
        // iload_2; iload 5; iadd; istore_2; iinc 7, 1; iload_2; ireturn
        // Locals 0, 1, 3, 4, 6 are never named, so the caller need not (and must not have to)
        // marshal them -- that is what lets an unused `this` in slot 0 stay unmarshalled.
        let code = [0x1c, 0x15, 0x05, 0x60, 0x3d, 0x84, 0x07, 0x01, 0x1c, 0xac];
        let c = compile(&code, 8, &no_constants).unwrap();
        assert_eq!(c.touched_locals, vec![2, 5, 7]);
    }

    #[test]
    fn a_local_past_max_locals_is_refused() {
        let err = compile(&[0x15, 0x09, 0xac], 4, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::LocalOutOfRange { pc: 0, slot: 9 });
    }

    #[test]
    fn the_operand_stack_depth_is_recomputed_from_the_control_flow_graph() {
        // iconst_0; ifeq +5 (to the iconst_3); iconst_1; ireturn; iconst_3; ireturn
        //  0        1                          4          5        6         7
        // Both arms of the branch leave the stack at 0 before pushing their result, so the
        // deepest the stack ever gets is 1.
        let code = [0x03, 0x99, 0x00, 0x05, 0x04, 0xac, 0x06, 0xac];
        let c = compile(&code, 0, &no_constants).unwrap();
        assert_eq!(c.stack_slots, 1);
    }

    #[test]
    fn paths_that_disagree_about_the_stack_depth_are_refused() {
        // iconst_0; iconst_0; ifeq +4 (to pc 6); pop; iconst_1; ireturn
        //  0         1         2                  5    6         7
        // pc 6 is reached with depth 1 by the branch (one value left) and depth 0 by the
        // fall-through through `pop`. Verified bytecode never does this.
        let code = [0x03, 0x03, 0x99, 0x00, 0x04, 0x57, 0x04, 0xac];
        let err = compile(&code, 0, &no_constants).unwrap_err();
        assert!(matches!(err, Ineligible::StackMismatch { pc: 6, .. }), "{err:?}");
    }

    #[test]
    fn underflow_and_out_of_range_branches_are_refused() {
        // iadd with an empty stack.
        assert_eq!(compile(&[0x60, 0xac], 0, &no_constants).unwrap_err(), Ineligible::StackUnderflow { pc: 0 });
        // goto -100, off the front of the code array.
        let err = compile(&[0xa7, 0xff, 0x9c], 0, &no_constants).unwrap_err();
        assert_eq!(err, Ineligible::OutOfRange { pc: 0 });
        // A method whose last instruction falls through past the end.
        assert_eq!(compile(&[0x03], 0, &no_constants).unwrap_err(), Ineligible::OutOfRange { pc: 0 });
    }

    #[test]
    fn a_branch_into_the_middle_of_an_instruction_is_refused() {
        // A branch target that is not an instruction boundary of the linear decode. Both readings
        // happen to agree about the stack depth, so only the tiling check catches it — which is
        // exactly why that check exists. `javac` cannot produce this; a hand-written or hostile
        // class file can.
        //
        //  0: iconst_0            pushes 1
        //  1: ifeq  -> 6          pops 1; falls through to 4 and branches to 6
        //  4: sipush 0x0004       occupies 4, 5 and 6 -- so the branch lands *inside* it
        //  7: ireturn
        //
        // Decoded from 6, the byte 0x04 is `iconst_1`, which pushes 1 and reaches pc 7 at the
        // same depth the sipush leaves it. Everything is consistent; the instructions simply
        // overlap.
        let code = [0x03, 0x99, 0x00, 0x05, 0x11, 0x00, 0x04, 0xac];
        assert_eq!(
            compile(&code, 0, &no_constants).unwrap_err(),
            Ineligible::OverlappingInstructions { pc: 4 }
        );
    }

    #[test]
    fn an_oversized_method_is_refused() {
        let mut code = vec![0x00u8; MAX_CODE_LEN + 1];
        code[0] = 0x04;
        assert_eq!(compile(&code, 0, &no_constants).unwrap_err(), Ineligible::TooBig);
        assert_eq!(compile(&[], 0, &no_constants).unwrap_err(), Ineligible::TooBig);
    }

    #[test]
    fn the_status_packing_round_trips() {
        assert_eq!(Status::unpack(0), Outcome::Returned(0));
        assert_eq!(Status::unpack(42), Outcome::Returned(42));
        // A negative int is carried as its *zero-extended* 32-bit pattern, so the high half
        // stays free for the status -- that is why `ireturn` is a 32-bit load.
        assert_eq!(Status::unpack(0xFFFF_FFFF), Outcome::Returned(-1));
        assert_eq!(Status::unpack(0x8000_0000), Outcome::Returned(i32::MIN));
        // A deopt now carries the pc it gave up at, which is what the interpreter resumes from.
        assert_eq!(Status::unpack(Status::deopt_value(0)), Outcome::Deopt(0));
        assert_eq!(Status::unpack(Status::deopt_value(4095)), Outcome::Deopt(4095));
        assert_eq!(Status::unpack(Status::safepoint_value(0)), Outcome::Safepoint(0));
        assert_eq!(Status::unpack(Status::safepoint_value(4095)), Outcome::Safepoint(4095));
        // An unknown status is read as a deopt at a pc no method has, so a caller that looks it up
        // finds nothing and declines to resume: no emitted code produces one, and "do not
        // reconstruct anything out of this" is the answer that is safe whatever the state.
        assert_eq!(Status::unpack(99 << Status::SHIFT), Outcome::Deopt(Status::NO_PC));
    }

    #[test]
    fn a_loop_header_with_an_empty_stack_becomes_an_entry_point() {
        //  0: iconst_0        push 0
        //  1: istore_0        i = 0
        //  2: iload_0         <- the loop header, stack empty
        //  3: sipush 100
        //  6: if_icmpge +9    -> 15 (exit)
        //  9: iinc 0, 1
        // 12: goto -10        -> 2, the back-edge
        // 15: iload_0
        // 16: ireturn
        let code = [
            0x03, 0x3b, 0x1a, 0x11, 0x00, 0x64, 0xa2, 0x00, 0x09, 0x84, 0x00, 0x01, 0xa7, 0xff,
            0xf6, 0x1a, 0xac,
        ];
        let c = compile(&code, 1, &no_constants).unwrap();
        assert_eq!(c.osr_entries, vec![2], "the `goto`'s target is the one loop header");
    }

    #[test]
    fn a_back_edge_that_carries_operands_is_not_an_entry_point() {
        // The same loop, but with a value live on the operand stack across the back-edge:
        //  0: iconst_5        push 5          (the carried operand)
        //  1: iconst_0
        //  2: istore_0        i = 0
        //  3: iload_0         <- header, but the stack holds 1 value here
        //  4: sipush 100
        //  7: if_icmpge +9    -> 16
        // 10: iinc 0, 1
        // 13: goto -10        -> 3
        // 16: ireturn         returns the carried 5
        let code = [
            0x08, 0x03, 0x3b, 0x1a, 0x11, 0x00, 0x64, 0xa2, 0x00, 0x09, 0x84, 0x00, 0x01, 0xa7,
            0xff, 0xf6, 0xac,
        ];
        let c = compile(&code, 1, &no_constants).unwrap();
        assert!(c.osr_entries.is_empty(), "depth 1 at the header, so it is not a transfer point");
        // And it still compiles and runs as it always did — ineligibility for OSR is not
        // ineligibility for compilation.
        assert_eq!(c.stack_slots, 3);
    }

    #[test]
    fn nested_loops_give_one_entry_point_each() {
        //  0: iconst_0; istore_0                      i = 0
        //  2: iload_0; bipush 10; if_icmpge -> 28     <- outer header at 2
        //  8: iconst_0; istore_1                      j = 0
        // 10: iload_1; bipush 10; if_icmpge -> 22     <- inner header at 10
        // 16: iinc 1,1; goto -> 10                    the inner back-edge
        // 22: iinc 0,1; goto -> 2                     the outer back-edge
        // 28: iload_0; ireturn
        let code = [
            0x03, 0x3b, // 0: iconst_0; istore_0
            0x1a, 0x10, 0x0a, 0xa2, 0x00, 0x17, // 2: iload_0; bipush 10; if_icmpge +23 -> 28
            0x03, 0x3c, // 8: iconst_0; istore_1
            0x1b, 0x10, 0x0a, 0xa2, 0x00, 0x09, // 10: iload_1; bipush 10; if_icmpge +9 -> 22
            0x84, 0x01, 0x01, 0xa7, 0xff, 0xf7, // 16: iinc 1,1; goto -9 -> 10
            0x84, 0x00, 0x01, 0xa7, 0xff, 0xe9, // 22: iinc 0,1; goto -23 -> 2
            0x1a, 0xac, // 28: iload_0; ireturn
        ];
        let c = compile(&code, 2, &no_constants).unwrap();
        assert_eq!(c.osr_entries, vec![2, 10], "both headers, in pc order");
    }

    #[test]
    fn a_method_without_a_loop_pays_nothing_for_osr() {
        // No back-edge -> no entry points, no poll, and RSI is never saved: the emitted code is
        // byte-for-byte what step 2 produced.
        let c = compile(&[0x1a, 0x1b, 0x60, 0xac], 2, &no_constants).unwrap();
        assert!(c.osr_entries.is_empty());
        // `push rsi` is `56`; the prologue of a poll-free method must not contain it.
        assert!(!c.code.starts_with(&[0x55, 0x48, 0x89, 0xE5, 0x53, 0x56]), "RSI must not be saved");
    }

    #[test]
    fn the_deopt_stub_is_only_emitted_when_something_can_deopt() {
        // `mov rax, (DEOPT << 32) | pc` is a 10-byte movabs, and the pc is *in the bytes*: a stub
        // for the division at pc 2 ends `...02 00 00 00 01 00 00 00`.
        let stub = |pc: u8| [0x48u8, 0xB8, pc, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00];
        // No division -> no deopt site -> no stub, and no resume site either.
        let plain = compile(&[0x1a, 0xac], 1, &no_constants).unwrap();
        assert!(!plain.code.windows(10).any(|w| w == stub(0)));
        assert!(plain.resume_sites.is_empty());
        // iload_0; iload_1; idiv; ireturn -> the guard, the stub *naming pc 2*, and a resume site.
        let divides = compile(&[0x1a, 0x1b, 0x6c, 0xac], 2, &no_constants).unwrap();
        assert!(divides.code.windows(10).any(|w| w == stub(2)));
        assert_eq!(divides.resume_sites.len(), 1);
        assert_eq!(divides.resume_sites[0].pc, 2);
        // Two operands live at the division — the dividend and the zero divisor — and they are what
        // the interpreter has to be handed to re-execute it and throw.
        assert_eq!(divides.resume_sites[0].stack, vec![Kind::Int, Kind::Int]);
        assert_eq!(divides.stack_base, 2, "the operand spill starts past the two locals");
        assert_eq!(divides.buffer_slots, 4);
    }
}
