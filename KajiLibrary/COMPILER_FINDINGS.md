# Compiler findings from dogfooding KajiLibrary

Bugs and gaps in the own `javac` (`src/javac/`) surfaced by compiling KajiLibrary with it.
The library is compiled with the frozen snapshot `bin/javac-frozen.exe` (`--emit X.java` →
`X.class`); each compile failure — or each `.class` that is structurally wrong yet passes
`javap` — is a real compiler defect. This file is the handoff to the **compiler session**
(the library session does not touch `src/javac/`).

Legend: ✅ fixed (in `src/javac` + regression test, folded into the snapshot) · ⬜ open.

**Numbering:** findings raised from the **library session are numbered from #100 up** (#100, #101, …),
originally to avoid colliding with the compiler session running in parallel (which was still in the
low numbers); their repros are `finding_1NN.java`. **The two ranges have since converged** — the
compiler session now also numbers in the 1xx range (#113–#117). No duplicates so far, but **check the
highest number in this file before claiming a new one**, whichever session you are.

**Versioned repros:** `KajiLibrary/repros/finding_NN.java` — self-contained, one per finding.
Run from the repo root: `cargo run -- --emit KajiLibrary/repros/finding_09.java` (or
`target/debug/javac.exe --emit …`). The binary prints diagnostics but **exits 0** — judge by the
message, not the exit code. For emission-only bugs (#6) inspect with `bin/javap-clon.exe -v`.

**Status (2026-08-03): all of #1–#12 are fixed in `src/javac` and folded into `bin/javac-frozen.exe`**
(regression tests: `cargo test --lib`, 658 green; snapshot refreshed and verified). #10 and the
interface-nested-enum half of #12 were already in the live build; the rest were fixed across this and
earlier sessions. **Open now: #13–#20** (found 2026-08-04 dogfooding H3 — collections retrofits and
streams — H4 — java.time — H5 — java.util.regex — and H6 — java.util.Formatter; repros in
`KajiLibrary/repros/`, workarounds in the library — see Priority at the bottom). #19 is a
**performance** item, not a correctness bug; **#20 is silent wrong codegen** (a `new` with a qualified
name) that the shape gate can't catch. **#21 (was CRITICAL — enum machinery dropped for a
named-package enum) is ✅ FIXED** in `src/javac` (regression test added); the library still needs to
recompile java.time (its enums were degenerate on disk) to unblock H6-T5 `%t`.
(See the "Build gotcha" note before rebuilding — build to a local-disk `CARGO_TARGET_DIR`, not the USB
`target/debug`.)

---

## Fixed

- **#1 — ✅ `native` emitted as `abstract`.** `modifier_flag` didn't map `Modifier::Native`
  and `gen_method` forced `ACC_ABSTRACT` on bodyless methods, so `native` methods lost
  `ACC_NATIVE` and came out abstract. Found on `Object`/`String`. Fixed in `codegen.rs`.

- **#2 — ✅ `long` hex literal with the high bit rejected.** `0x8000000000000000L` overflowed
  because `parse_int_literal` used `i64::from_str_radix`; hex/bin/octal are unsigned bit
  patterns (JLS §3.10.1). Found on `Long`. Fixed to parse as `u64` then cast (`parser.rs`).

- **#3 — ✅ constant int→byte/short/char narrowing (JLS §5.2).** `static final byte MIN = -128;`
  was rejected; the narrowing assignment conversion for constant expressions was missing in
  field init, local init, and `ExprKind::Assign`. Found on `Byte`/`Short`. Fixed in `attribute.rs`.

- **#10 — ✅ capturing anonymous classes are miscompiled.** *Fixed in the live compiler* (confirmed
  2026-08-03 on `target/debug`): `finding_10.java` now emits `Enclosing$1.class` implementing
  `Runnable`, and its ctor takes the enclosing instance (the pool carries `LEnclosing;` for the
  `this$0` capture). Kept as a regression repro. (Was: no `this$0`, nested `.class` not emitted.)

- **#6 — ✅ `java.lang.Object` emitted with `super_class` pointing at itself.** Fixed in `codegen.rs`
  (`gen_type` + `gen_method`): when `this_class == java/lang/Object`, `super_class = 0` **and** its
  `<init>` emits no `super()` call (which would be a self-call → infinite recursion), marking `this`
  initialized instead (JVMS §4.10.2.4). Matches real javac (Object.<init> is just `return`).
  Regression tests in `codegen.rs` (`object_gets_super_class_zero_…`, `an_ordinary_class_still_…`).

- **#9 — ✅ generic override covariant-return doesn't substitute type variables.** Fixed in
  `check.rs::as_seen_from`: it was searching for the declaring supertype among the supertypes of the
  **return type** (a type variable, whose supertypes are its bounds) instead of among the supertypes
  of the **class**. Now it builds the class self-type (`types::self_type`), finds the parent as a
  supertype (`subst_for`), and substitutes the inherited return (so `List`'s `E` → the subclass's
  `E`). Regression tests in `check.rs` (`a_generic_covariant_override_substitutes_the_type_variable`,
  a concrete-arg case, and a negative case). Unblocks generic `implements` where a method returns the
  element/value type.

- **#5 — ✅ a source core type's identity vs the external one in the override check.** Fixed in
  `enter.rs::resolve_name_to_sym`: the `java.lang.*` shortcut went **straight** to `table.external`,
  so when compiling `java.lang.String` itself, references to `java.lang.String` (e.g. the return of
  `Object.toString()` read from the `.class`) bound to the **external** `String`, distinct from the
  source one being compiled, and the covariant-return check saw two different `String`s. Now the
  shortcut prefers `table.class(&dotted)` (source shadows external, `--patch-module` semantics), so
  both sides of the override are the same `String`. Regression test in `check.rs`
  (`a_source_core_type_shadows_the_external_one_in_the_override_check`). Unblocks
  `String implements CharSequence` / `toString()`. **Note:** this shadows a source-declared type
  within the *same* compilation; making a KajiLibrary type in *another* file shadow the JDK still
  needs classpath support (that's the remaining half of #7).

- **#11 — ✅ static-method calls on `java.util` utility classes don't resolve.** **Root cause was
  NOT the classfile reader** (the reader reads `Objects.class` fine — the hypothesis in the old entry
  was wrong). It was that `enter.rs::collect_from_expr` never collected the **receiver of a static
  call/field access** as a candidate type, so `Objects` in `Objects.requireNonNull(x)` was never
  handed to the finder → never loaded → "symbol not found". `java.lang` utilities (`Math`, `System`)
  worked only because they're in the hard-coded `JAVA_LANG` force-load list. Fix: a `qualifier_name`
  helper collects the receiver of `Call`/`Field` (`Objects`, `a.b.C`) so it gets loaded. Regression
  test in `check.rs` (`a_static_call_on_a_java_util_utility_resolves`).

- **#7 — ✅ no classpath, so KajiLibrary can't build against its own types.** Fixed with three pieces:
  (1) a `-cp <dirs>` CLI flag (`javac.rs`) — dirs of `.class` **prepended** to the finder path so they
  win over the JDK; threaded via `analyze_cp`/`compile_cp`/`check_cp` → `enter_cp` → `ClassFinder::new`.
  (2) collecting static-call receivers (shared with #11) so a KajiLibrary-only type referenced only by
  `Sib.v()` is actually loaded. (3) `try_load` now also tries the **bare** name as an internal name,
  so a **default-package** type on the classpath resolves. Together with #5's shadowing, a KajiLibrary
  type on `-cp` wins over the JDK. Regression tests in `codegen.rs`
  (`a_classpath_dir_lets_a_file_reference_a_separately_compiled_type`, plus a negative control).

- **#4 — ✅ finder doesn't auto-load an unqualified same-package classpath type.** Fixed in
  `enter.rs::try_load`: for a simple name it only tried `java/lang/<name>` + single-imports. Now it
  also tries `<current-package>/<name>` (so `List` from `package java.util` finds `java/util/List`)
  and the **bare** name (default-package classpath types, shared with #7). The current package is
  threaded in via `enter_cp`. Regression test in `check.rs`
  (`a_same_package_classpath_type_resolves_by_simple_name`).

- **#8 — ✅ no abstract-method-completeness check.** The check **existed** (`check.rs::abstracts`)
  but **exempted any class with an external ancestor** — and every class extends the external
  `Object`, so `implements List` was skipped. Fixed by (1) narrowing the exemption to *external
  **class** ancestors other than `Object`* (external **interfaces**, whose methods the finder loads
  transitively, no longer exempt); (2) marking external interface **`default`** methods (`enter.rs`
  `build_external`) so they aren't demanded; (3) matching abstract-vs-impl by the signature **as seen
  from the subclass** — substituting the supertype's type args before erasing — so
  `Comparable<Foo>.compareTo(T)` matches `Foo.compareTo(Foo)` (the bridge is synthesized later). That
  last piece was essential: without it, every generic `implements` false-positived. Regression tests
  in `check.rs` (reports unimplemented external-interface methods; full-impl and `default`-method
  negative controls; external-class ancestor still exempt).

- **#12 — ✅ an enum nested in an interface is miscompiled + enum class/field flags.** The
  interface-nested **degeneration** (no constants/`$VALUES`/`values`/`valueOf`/`<clinit>`, bogus
  `()V` ctor) was **already fixed in the live build** — verified: `interface I { enum E { A, B } }`
  now emits `I$E` with the full machinery and a `Signature` of `Enum<E>` (not raw), identical to a
  class-nested enum. What remained (the finding's "isn't final" note, and a general gap for **all**
  enums) was the **access flags**: we emitted only `ACC_SUPER`. Fixed in `codegen.rs`: the enum class
  now gets `ACC_ENUM` (+ `ACC_FINAL` for a simple enum, or `ACC_ABSTRACT` if it declares an abstract
  method), enum **constant** fields get `ACC_ENUM`, and `$VALUES` gets `ACC_SYNTHETIC` — matching real
  javac (`0x4030` class, `0x4019` constants, `0x101a` `$VALUES`). Without `ACC_ENUM`, `Class.isEnum()`
  / `Field.isEnumConstant()` return false. Regression tests in `codegen.rs`
  (`an_enum_gets_enum_and_final_flags…`, `an_enum_nested_in_an_interface_gets_the_full_machinery`).

**Note:** #4, #5, #6, #7, #8, #9, #11, #12 are fixed in `src/javac` with passing regression tests
(`cargo test --lib`, 658 green), and **`bin/javac-frozen.exe` has been refreshed** with all of them
(the previous snapshot is preserved as `bin/javac-frozen.exe.bak`). Verified with the new frozen:
enum class flags `0x4030` and `Object` `super_class: #0`.

**Build gotcha (USB drive):** `cargo build` into the repo's `target/debug` keeps failing at *link*
with a locked-`.exe`/`.pdb` error (`link.exe returned an unexpected error` / `os error 5` / `1224`) —
antivirus/USB locking the output. Workaround that works reliably: build to a **local disk** target
dir, e.g. `CARGO_TARGET_DIR=$LOCALAPPDATA/Temp/jvm-target cargo build --bin javac` (~12s, clean
link), then `cp` the resulting `javac.exe` over `bin/javac-frozen.exe`. `cargo test --lib` is
unaffected (it doesn't link the executables).

---

## Open

- **#116 — the implicit `public` of an interface member isn't applied to `static` methods.**
  In an interface every member is implicitly `public` (JLS §9.4). We apply that to *abstract*
  methods but not to *static* ones: `static ClassDesc of(String)` came out package-private
  (`flags: (0x0008) ACC_STATIC`), while `String descriptorString();` in the same interface
  correctly got `ACC_PUBLIC | ACC_ABSTRACT`. A package-private static on an interface isn't even
  expressible in Java, so the emitted class file is one no `javac` could produce.
  - Repro: `public interface I { static I of(String s) { return null; } String d(); }`
    → `javap -p` shows `static I of(java.lang.String)` where the reference shows `public static`.
  - Workaround in KajiLibrary: `java.lang.constant.ClassDesc.of` spells out `public static`.

- **#117 — a fully-qualified type name isn't resolved in a type position.**
  Writing the package on the type instead of importing it fails to resolve, even though the class
  is on the classpath and the very same type resolves fine once imported.
  - Repro: `private final java.lang.constant.ClassDesc constantType;`
    → `error: no se encuentra el símbolo: java.lang.constant.ClassDesc`; adding
    `import java.lang.constant.ClassDesc;` and writing `ClassDesc` compiles.
  - Observed on a field declaration (the first occurrence in the file); ctor/method parameter and
    return positions in the same class used the same qualified name, so the fix is presumably one
    place in name resolution rather than per-position.
  - Workaround in KajiLibrary: `Enum.EnumDesc` imports `ClassDesc` instead of qualifying it.

- **#115 — `volatile` on a field is dropped: `ACC_VOLATILE` is never emitted.**
  A field declared `private volatile boolean interrupted;` comes out as `flags: (0x0002) ACC_PRIVATE`;
  the reference `.class` has `(0x0042) ACC_PRIVATE, ACC_VOLATILE`. This is not cosmetic — the
  interpreter keys the memory model off exactly that bit (`MemberInfo::is_volatile()` →
  `Ordering::Release`/`Acquire` on `putfield`/`getfield`, and a real `AtomicU64` for `long`), so a
  dropped flag silently downgrades the field to `Relaxed`. Under the green scheduler nothing shows
  (one carrier, no real parallelism); under the OS-parallel substrate it is a genuine data race.
  - Repro: any `volatile` field → `javap -v` shows no `ACC_VOLATILE`.
  - Blocks: KajiLibrary's `java.lang.Thread` can't take over from `boot/`'s until this is fixed —
    its `interrupted` flag is written by *another* thread (the interrupter) and read by this one,
    which is precisely the case `volatile` exists for. The source is written and compiles; it is
    a strict superset of `boot`'s Thread **except** for this flag.

- **#113 — the enhanced-`for` desugaring erases a generic return type to `Object` instead of to its erasure.**
  A for-each over a generic collection emits the `iterator()` call site with descriptor
  `()Ljava/lang/Object;`, but the declared method erases to `()Ljava/util/Iterator;`. The descriptors
  don't match, so resolution fails at run time with `NoSuchMethodError` (the VM looks the call up by
  exact descriptor). The erasure of `Iterator<E>` is `Iterator`, never `Object` — `Object` would only
  be right for an unbounded *type variable* return (`E`), not for a parameterized type.
  - Repro: `ArrayList<String> l = new ArrayList<>(); l.add("x"); for (String s : l) { ... }`
    → `invokevirtual java/util/ArrayList.iterator:()Ljava/lang/Object;` while
    `javap -s KajiLibrary/java/util/ArrayList.class` reports `descriptor: ()Ljava/util/Iterator;`.
  - Impact: **every** for-each over a KajiLibrary collection is unusable; the indexed form
    (`for (int i = 0; i < l.size(); i++)`) works fine, so it's specific to the desugaring.

- **#114 — a concat whose operand has no matching `append` overload is silently dropped (no error).**
  `"Thread-" + tid` (a `long`) emitted **nothing at all** for the right-hand side — the ctor came out
  as `aload_0; putfield name`, i.e. a `putfield` with an empty stack, which the verifier/interpreter
  then rejects as *operand stack underflow*. Root cause was a **library** gap (`StringBuilder` had no
  `append(long)`), now fixed on the KajiLibrary side; but the compiler behaviour is the real bug:
  a missing overload must be a **compile error**, never a silently empty expression that produces
  invalid bytecode. `String + int` and `String + String` were fine, so it's the overload-lookup
  failure path that's unguarded.
  - Repro (before the library fix): `long t = 7L; String c = "y" + t;` → `astore` with nothing pushed.
  - Note: `append(double)` / `append(float)` / `append(Object)` are still missing from
    `StringBuilder`, so concatenating those types will hit the same silent-drop today.

- **#13 — ✅ FIXED. enclosing-instance capture is broken for a class declared inside a *generic* class.**
  Turned out to be **two** independent things, both now resolved:
  **(1) the capture itself was already fixed** — an anonymous class inside a generic enclosing class now
  emits its nested `.class` with a `this$0` field, a ctor that takes the enclosing instance, and
  `getfield this$0 + invokevirtual` for enclosing calls (verified: `Gen$1` verifies clean). The generic
  enclosing class is no longer special-cased.
  **(2) a separate, newly-found bug the original repro was tripping** — a **bare** reference to an
  anonymous class's **own** field (`c`, not `this.c`) failed with *"no se encuentra: c"* (even with a
  *non*-generic enclosing class). Root cause: `hoist_anonymous` turns each anonymous class into a
  synthetic local class **after** the last `resolve_symbols` (the one in `enter` and the one in
  `register_local_classes`), so the synthetic class's fields never got a `Resolved::Field`; a bare name
  needs it (`this.c` tolerates `Unresolved`, which is why that path worked). Fix: `hoist_anonymous` now
  re-runs `resolve_symbols` at the end (idempotent, same pattern as `register_local_classes`). With both,
  the full repro compiles and verifies. Regression tests in `codegen.rs`
  (`an_anonymous_class_in_a_generic_enclosing_class_captures_this0`,
  `a_bare_reference_to_an_anonymous_class_own_field_resolves`). Original report below.
  An inner/anonymous class that references the enclosing instance (calls its methods / reads its
  fields) is miscompiled when the enclosing class has type parameters: no `this$0` field/param is
  generated. For an **anonymous** class the nested `.class` isn't emitted at all (and its ctor is
  `()V`); for a **named inner** class the `.class` IS emitted but its ctor is `()V` and calls to
  enclosing methods come out **receiver-less** (invalid bytecode). A non-generic enclosing class
  works (that's #10's fix), and a same-file **top-level** class that takes the instance explicitly
  works too. Surfaced writing `java.util.ArrayList<E>.iterator()`.
  - Repro: `public class Gen<E>{ int size(){return 0;} E get(int i){return null;}
    public java.util.Iterator<E> it(){ return new java.util.Iterator<E>(){ int c=0;
    public boolean hasNext(){return c<size();} public E next(){return get(c++);} }; } }`
    → `Gen$1.class` is not emitted; the named-inner variant emits `Gen$Itr.class` with a bogus
    `()V` ctor and receiver-less `size()`/`get()` calls.
  - Workaround in KajiLibrary: `ArrayListItr` / `HashSetItr` are **same-file top-level** classes
    holding the collection explicitly (no compiler-generated capture).

- **#14 — ✅ FIXED (by the #15 fix; same root cause). inherited method from a `-cp` superinterface
  doesn't resolve.** Verified: with `Collection { int size(); }` and `List extends Collection` both
  compiled to a `-cp` dir, `list.size()` on a `List<String>` now resolves (default-package flat layout
  and named-package subdir layout both checked). The #15 fix (`candidates` walks the full supertype
  graph — interfaces + super_class — not just the `super_class` chain) covers the `-cp`-loaded
  superinterfaces too, since `build_external` loads their members. Regression test in `codegen.rs`
  (`a_method_inherited_from_a_classpath_superinterface_resolves`, a two-file `-cp` scenario). Original
  report below.
  With `-cp`, a call to a
  method **inherited** from a superinterface loaded via `-cp` fails: `list.size()` where `list` is
  `List<T>`, `List extends Collection`, and `size()` is declared in `Collection` (List and Collection
  both on the `-cp`) → *"no se encuentra el método: size"*. Without `-cp` (both interfaces from the
  JDK) it resolves fine, so the finder handles external interface inheritance but not `-cp`
  interface inheritance — it loads the named type but doesn't walk its `-cp`-loaded superinterfaces
  for member lookup. Surfaced compiling `java.util.Collections.reverse` against `-cp KajiLibrary`.
  - Workaround in KajiLibrary: `Collections` is compiled **without** `-cp` (its `List` params bind
    to the JDK's, whose `size()` resolves); only the concretes that must bind to our own subset
    interfaces (`ArrayList`/`HashMap`/`HashSet`) use `-cp`.

- **#15 — ✅ FIXED. a method inherited through a *generic* superinterface doesn't resolve.**
  Fix (compiler session): `attribute::candidates` now walks the **full supertype graph** (interfaces +
  super_class) via a stack, instead of only the `super_class` chain, so a method inherited through a
  generic superinterface (`apply` via `BinaryOperator extends BiFunction<T,T,T>`) is a candidate; the
  superinterface's type args are substituted by `substitute_member` with the receiver. Regression test
  in `attribute.rs` (`a_method_inherited_through_a_generic_superinterface_resolves`). Original report below.
  Calling a
  method a functional interface inherits from a generic super-interface fails: `op.apply(a, b)` where
  `op` is `BinaryOperator<T>` — `apply` is declared in `BiFunction<T,U,R>` and inherited via
  `BinaryOperator extends BiFunction<T,T,T>` — errors *"no se encuentra el método: apply"* (no `-cp`;
  both from the JDK). A method inherited through a **non-generic** super-interface resolves (that's
  why `Collection.size()` on a `List` works — cf. #14, which is the `-cp` flavour). What's missing is
  substituting the super-interface's type args (`BiFunction<T,U,R>` → `<T,T,T>`) during member lookup.
  Surfaced writing `Stream.reduce(BinaryOperator)`.
  - Repro: `import java.util.function.BinaryOperator; class R{ Integer f(BinaryOperator<Integer> op,
    Integer a, Integer b){ return op.apply(a,b); } }` → apply not found.
  - Workaround in KajiLibrary: widen to the declaring super-interface —
    `BiFunction<T,T,T> op = binaryOperator; op.apply(...)` (BiFunction declares `apply` directly).

- **#16 — ✅ FIXED. a lambda argument to a generic constructor isn't supported by codegen.** A lambda
  passed directly to a generic constructor, with its target functional-interface type inferred
  *through* the constructor's type parameters, failed: *"el generador de bytecode todavía no soporta
  una expresión lambda (necesita invokedynamic)"*.
  Root cause (compiler session): `ExprKind::NewObject` resolved the constructor overload but **never ran
  phase 2** — the re-attribution of poly arguments with the parameter's (receiver-substituted) type as
  target — that a method call does via `reattribute_poly_args`. So the lambda argument kept no target
  (`Unresolved`) and LambdaToMethod couldn't lower it. Fix: after resolving the constructor binding,
  `NewObject` now calls `reattribute_poly_args(env, &instance_type, ctor, args)`, so `Supplier<A>` with
  `Box<long[]>` substitutes to `Supplier<long[]>` and the lambda gets its target. Verified: the repro
  emits `invokedynamic` and verifies. Regression test in `codegen.rs`
  (`a_lambda_argument_to_a_generic_constructor_lowers_to_invokedynamic`). Original report below.
  - Repro: `new CollectorImpl<T, long[], Long>(() -> new long[1], …)` fails; hoisting to
    `Supplier<long[]> s = () -> new long[1]; new CollectorImpl<>(s, …)` compiles.
  - Workaround in KajiLibrary: every `Collectors` lambda is bound to an explicitly-typed local first.

- **#17 — ✅ FIXED. generic override doesn't unify a *method*-level type variable in the return.**
  Fix (compiler session): `check::as_seen_from` now, besides the class-level substitution (#9), also
  maps the **parent method's** type variables to the **child's** positionally (§8.4.2) before comparing
  returns, so a bare `<R> R foo(R)` override unifies its `R` with the parent's. Regression test in
  `check.rs` (`a_generic_override_with_a_bare_method_type_variable_return_is_accepted`). Original report below.
  The covariant-return override check rejected a method whose return is a bare method-level type variable:
  `class C<T> implements I<T>{ public <R> R foo(R x){ return x; } }` against `interface I<T>{ <R> R
  foo(R x); }` → *"el retorno de `foo` no es compatible con el de `I`: R no es un subtipo de R"*. #9
  fixed **class** type variables; this is the **method** analogue in a *bare* return position. A
  method returning a parameterized type that merely *contains* R (e.g. `<R> Stream<R> map(...)`)
  works — only the bare-`R` return trips it. Blocks `Stream.collect(Collector)` (`<R,A> R collect`),
  so `Collectors` is written and gate-clean but not yet consumable.

- **#18 — ✅ FIXED (by the #21 fix; same root cause). an enum in a *named package* without an explicit
  constructor gets a degenerate ctor.** Verified on the current build: `finding_18.java` emits
  `private repro.Repro18(java.lang.String, int)` (the correct name+ordinal ctor, no longer a
  `new`-instantiable `public ()V`), plus `values()`/`valueOf()`. The desugar's member-synthesis FQN
  lookup that dropped the machinery for a packaged enum (the #21 root cause) also drove the degenerate
  ctor here; #21's fix (build the lookup FQN **with the package**) covers both. Original report below.
  A plain `enum` in a named package, with no explicit constructor, synthesised a `public ()V`
  constructor instead of the correct `private <Name>(String, int)` (name + ordinal). The **same
  enum in the default package compiles correctly**, and adding an explicit (even empty) constructor
  fixes it — so only the implicit-constructor synthesis path, and only for named-package enums, is
  wrong. The `.class` still has the constants/`values()`/`valueOf()`, but the enum becomes
  `new`-instantiable and lacks the `(String,int)` ctor those helpers call. Surfaced writing
  `java.time.Month`/`DayOfWeek`.
  - Repro: `package repro; public enum Repro18 { A, B; public int getValue(){return ordinal()+1;} }`
    → `javap -p` shows `public repro.Repro18();`; the same enum with no `package` line, or with an
    explicit `Repro18(){}`, shows `private Repro18(String,int)`.
  - Workaround in KajiLibrary: an explicit empty constructor on `Month`/`DayOfWeek` (ChronoField/
    ChronoUnit already had explicit ctors, so they were fine).

- **#19 — ✅ FIXED (performance). the `-cp` finder is pathologically slow when a compiled type is also
  on the classpath.** Both documented mechanisms are addressed in `enter.rs`:
  **(a) source-shadows-classpath in the load paths.** `try_load`'s guard only checked `table.class(name)`
  for the name *as given*, which misses when the source type is in a **package** but referenced by its
  simple name (`K1` vs the registered `p.K1`) — so the finder loaded the `-cp` copy of a type the
  compilation already declares, dragging in its whole hierarchy as redundant externals. Now `try_load`
  skips loading if **any** candidate internal path maps to a source type (`table.class(fqn)`), and the
  transitive supertype loop in `build_external` skips a supertype that is a source type too.
  **(b) miss-memoization in `ClassFinder`.** `find` now caches internal names already known absent
  (`RefCell<HashSet>`), so a transitively-referenced missing type (e.g. `RegexParser`, absent from the
  `-cp`) is looked up on disk **once** instead of re-stat'd per referencing class.
  Verified: full suite green (710), and a regression test confirms a packaged source type whose own
  `.class` is on the `-cp` is **not** loaded as a redundant external
  (`a_source_type_shadowed_on_the_classpath_is_not_loaded_as_a_redundant_external`). Note: the original
  multi-minute hang needs the dense `java.util.regex` class graph and wasn't reproduced with a small
  synthetic case, but both redundant-work paths it named (own-output-on-`-cp` and repeated absent
  lookups) are now eliminated. Original report below.
  Compiling `X.java` with `-cp <dir>` where `<dir>` contains the `.class` of a type
  that `X.java` *itself declares* (its own prior output, or a sibling package-private class in the
  same file) makes the compile take **>2 min / effectively hang**; removing those stale `.class`
  from the `-cp` dir makes it **instant**. A second trigger: a class on `-cp` whose transitive
  references are *absent* from the `-cp` (e.g. the real `Pattern.class`, whose ctor references
  `RegexParser`/`Node`, once those are removed from the dir) — the finder appears to re-walk /
  re-resolve the missing names repeatedly. **This likely explains the earlier "`-cp KajiLibrary` is
  impractically slow (>180s)" observation** (previously chalked up to dir size): it isn't the
  ~500-class dir size per se, it's the finder redoing transitive resolution for types that are
  simultaneously being compiled and present on `-cp`, with no memoization of (especially failed)
  lookups. Not a correctness bug — the compile succeeds (exit 0, correct `.class`) — but it makes
  iterating on a multi-class file (regex: 15 package-private classes in one `.java`) painful.
  - Reproduction (no single-file repro — needs a stale `-cp`): compile
    `KajiLibrary/java/util/regex/Node.java` with `-cp` containing the previous `Node.class` + sibling
    node `.class` → hangs; with those removed (only the `Matcher`/`Pattern`/`PatternSyntaxException`
    deps present) → ~instant. Reconfirmed: the same file with the *real* `Pattern.class` on `-cp`
    (drags in absent `RegexParser`/`Node`) → ~2 min, vs a minimal `Pattern` stub → instant.
  - Likely fix (compiler session): in `ClassFinder`/`try_load`, (a) skip a classpath `.class` for any
    type already present as a **source** type in the current compilation (source-shadows-classpath —
    extend #5/#7 to short-circuit *before* the load), and (b) **memoize** finder lookups — cache both
    resolved and *failed* name resolutions so a repeated/transitive lookup of the same internal name
    is O(1) instead of a fresh transitive walk.
  - Workaround (library session): keep `mincp` minimal — include only the *dependencies* of the file
    being compiled, never the file's own output or siblings it redefines; when a needed dep (e.g.
    `Matcher`) transitively references a heavy type (`Pattern`), put a **minimal stub** of that type
    on `mincp` (fields only, no deep references) rather than the real class. Surfaced compiling the
    `java.util.regex` engine (H5).

- **#20 — ✅ FIXED. a `new` with a fully-qualified class name is miscompiled.** `new java.lang.Object()`
  (qualified) is mishandled — the qualified type name in the instance-creation expression isn't
  resolved for codegen; `new Object()` / `new Formatter()` (simple name, via import) works. Two
  symptoms, same root: **(A)** self-contained (no `-cp`) → a **compile error** *"el generador de
  bytecode todavía no soporta un `new` de un tipo que no se pudo resolver"*; **(B)** with `-cp` and a
  cross-package type (`new java.util.Formatter()` inside `java.lang.String`) → **no error, the whole
  method body is silently dropped to a bare `0: areturn`** — passes `javap` and the API-shape gate but
  is broken bytecode. **(B) is the dangerous one** (silent wrong codegen that the shape gate can't
  catch). Surfaced writing `String.format` in H6-T1.
  - Repro: `KajiLibrary/repros/finding_20.java` — `qualified()` = `new java.lang.Object()` errors;
    `simple()` = `new Object()` compiles. (Symptom B needs the `-cp` cross-package setup, so the
    versioned repro shows symptom A.)
  - Likely fix (compiler session): resolve a qualified `Name.Name.Class` in `ExprKind::New` the same
    way a simple name / a qualified name in other positions is resolved, so codegen has the symbol.
    And — defensively — codegen should never silently emit an empty body: a `new` whose type didn't
    resolve must be a hard error, not a dropped statement (that's what let symptom B pass the gate).
  - Workaround in KajiLibrary: `import` the class and use the simple name (`String.format` uses
    `import java.util.Formatter; new Formatter()`).

- **#21 — ✅ FIXED: the compiler dropped ALL enum machinery for an enum in a *named package*.** A
  packaged `enum { A, B, C }` compiled to a `final class extends Enum` with **none** of the enum
  machinery: no `public static final <E> A/B/C` constant fields, no `values()`, no `valueOf()`, no
  synthetic `$VALUES`, no `<clinit>`, and a degenerate no-arg ctor instead of `(String,int)`.
  **Root cause:** the **desugar** built the FQN for its `table.class(fqn)` member-synthesis lookup with
  `qualify(enclosing, name)` — which prepends only the **enclosing classes**, never the **package** —
  while `enter` registers the class under `package.Name`. So for a packaged enum `table.class("Repro21")`
  was `None` (registered as `finding_21.Repro21`), the whole enum/record/assert synthesis was silently
  skipped, and the default no-arg ctor path ran instead. **Fix:** `desugar()` now seeds the initial
  `enclosing` with `unit.package` (like `enter`'s `base`), threaded through `collect_enums`,
  `collect_records`, `collect_constants`/`collect_const_fields`, and the top-level `d.class(ty, &base)`.
  Regression test `an_enum_in_a_named_package_keeps_its_machinery` (codegen). **Correction to the
  original note:** it does **not** affect a default-package enum (there `base == ""` and the FQN
  already matched) — the "even a default-package one" claim was a misdiagnosis (likely a wrong-CWD run
  where the relative bootstrap classpath didn't load and `String.equals` failed instead). **It silently broke H4 / java.time**:
  `ChronoField`, `ChronoUnit`, `Month`, `DayOfWeek` on disk are all degenerate (0 constants) — the
  value types (`LocalDate` etc.) were compiled earlier against a good enum build, so they carry
  `Fieldref`s to constants that no longer exist. **The API-shape gate cannot catch this** (missing
  members are still a subset of the JDK's), which is why H4 "273 match PASS" masked it. Blocks
  H6-T5 `%t` (needs `ChronoField.YEAR` etc.).
  - Repro: `KajiLibrary/repros/finding_21.java` — `javap -p` the emitted `Repro21` shows no A/B/C
    fields, no `values()`/`valueOf()`/`$VALUES`.
  - Narrowing: a **regular (non-enum) class** with static-initializer constants
    (`static final X = new X(); static final int[] A = {..}`) DOES get a correct `<clinit>`
    (anewarray/invokestatic/putstatic verified). So the defect is isolated to the **enum-specific
    machinery synthesis** (constant fields + `$VALUES` + `values()`/`valueOf()` + enum `<clinit>`),
    not general static-init / `<clinit>` generation.
  - Impact: once fixed, **all of java.time must be recompiled** (the enums, then re-verify the value
    types still link). Consider adding enum machinery (constants + `values()` presence) to a
    regression test AND to the gate/behaviour check so a subset-masked drop like this can't recur.

- **#22 — ✅ FIXED. method resolution on `this` ignores ALL inherited methods (superclass *and* interfaces).**
  Fix (compiler session): the interface half was already resolved by finding #15 (`candidates` now walks
  the full supertype graph). The remaining superclass half was root-caused to `enter::resolve_symbols`:
  a **class** with no explicit `extends` got `super_type = None` (only `enum`→`Enum` and `record`→`Record`
  had an implicit default), so the supertype walk never reached `java.lang.Object`. Now a plain class
  defaults its `super_type` to `Object` (§8.1.4) when `Object` resolves to a concrete class other than
  itself (no cycle; no-op when `Object` isn't on the classpath). Interfaces keep no superclass.
  Verified: the repro compiles clean, and `this.getClass()`/`this.hashCode()`/implicit `hashCode()`/the
  two-hop `class C extends B{}` all resolve. Regression tests in `enter` (plain class → Object, interface
  → none). Original report below.
  A call with `this` as the receiver (or an implicit receiver) resolves only against the methods
  **declared in the class itself**; it does not consult methods inherited from the superclass chain
  (`java.lang.Object`) or from implemented interfaces. The JDK resolves all of these. Verified with the
  full bootstrap `Object` (has `getClass`/`hashCode`/`toString`) on the `-cp`:
  - `this.getClass()` → `error: no se encuentra el método: getClass`
  - `this.hashCode()` (inherited) → `error: … hashCode`; but with an **own** `hashCode()` declared in
    the class, `this.hashCode()` resolves → OK. So the discriminator is *declared-here* vs *inherited*.
  - `this.toString()` (inherited) → `error: … toString`
  - `getClass()` (implicit receiver) → `error: … getClass`
  - `this.id()` where `id()` is from an implemented interface → `error: … id`; the same call on a
    variable of the interface type (`other.id()`) → OK.
  Likely cause: the `this`/implicit-receiver resolution walks only the class's own member table, not
  the resolved superclass + super-interface method sets. Surfaced writing `AbstractChronology` (JT5,
  java.time.chrono), whose `compareTo`/`hashCode`/`toString` call `this.getId()` (a `Chronology`
  method) and `this.getClass()` (an `Object` method).
  - Repro: `KajiLibrary/repros/finding_22.java` (the inherited cases fail; own-override + supertype-var
    rebinds compile).
  - Workaround in the library: rebind `this` to a supertype variable and call through it —
    `Chronology self = this; self.getId()` (used in `AbstractChronology`); for the `Object` methods,
    avoid them or use `super.m()`.
  - **Not caught by the shape gate**, and worse: our javac still **emitted `IsoChronology` (which
    `extends AbstractChronology`) even though `AbstractChronology.class` had failed to compile and was
    absent** — a dangling superclass reference that `javap` shape-checks clean. Treat "gate PASS" as
    necessary-not-sufficient here; verify every expected `.class` is actually on disk.
  - **Library-session note (2026-08-06) — RESOLVED by refreshing the snapshot.** A case that still
    reproduced on the *old* frozen `bin/javac-frozen.exe` (Aug 6 11:13) was a **super-interface method
    reached through an intermediate interface variable**: with `Temporal temp = …` (our `Temporal
    extends TemporalAccessor`), `temp.get(DAY_OF_WEEK)` → `no se encuentra el método: get`. **The
    library session refreshed the frozen snapshot** to the newer local-disk build
    (`$LOCALAPPDATA/Temp/jvm-target/debug/javac.exe`, Aug 6 13:38; old snapshot backed up to
    `bin/javac-frozen.exe.pre22`). On the refreshed snapshot this case, the `this.getClass()` /
    `this.getId()` cases, and a standalone `B extends A` super-interface repro all **compile clean**.
    The `#22` workarounds were **removed** from `AbstractChronology` (now `this.getId()` +
    `this.getClass()`, restoring the JDK-faithful hashCode) and `TemporalAdjusters` (now `temp.get(…)`),
    and both recompile + gate clean. So #15's supertype-graph walk does cover it — the old snapshot was
    simply stale.

- **#100 — ⬜ a bounded type variable erases to `java.lang.Object` instead of to its leftmost bound
  (JLS §4.6).** Our javac emits `Object` as the erasure of every type variable, ignoring any `extends`
  bound. Verified in isolation (`finding_100.java`): in `interface X<A extends Annotation, T>`,
  - `void single(A)` → we emit `(Ljava/lang/Object;)V`; JDK emits `(Ljava/lang/annotation/Annotation;)V`
  - `<U extends Comparable<U>> void cmp(U)` → we emit `(Ljava/lang/Object;)V`; JDK `(Ljava/lang/Comparable;)V`
  - `void obj(T)` (unbounded) → both emit `(Ljava/lang/Object;)V` (correct)
  So the bound is simply not consulted when computing a type variable's erasure. **Impact:** any
  method/field whose signature mentions a bounded type variable gets the wrong descriptor — a shape-gate
  MISMATCH, and (worse) it breaks overriding: an override written with the concrete bound type won't
  match the `Object`-erased inherited method. Surfaced building `jakarta.validation` (Bean Validation):
  `ConstraintValidator<A extends Annotation, T>.initialize(A)` erases to `(Object)V` vs the reference's
  `(Annotation)V`. Likely a general priority once the current work needs bounded generics widely.
  - Repro: `KajiLibrary/repros/finding_100.java` (`javap -s` the emitted interface).
  - Library handling: the source is kept faithful (`initialize(A)`); the erased-descriptor divergence is
    allowlisted (`tools/api_shape_allow.txt`) until the fix lands, then the snapshot is refreshed, the
    classes recompiled, and the allowlist entry removed. No clean library-only workaround exists that
    both keeps the generic signature and fixes the erasure.

- **#101 — ⬜ a qualified reference to a nested type, `Outer.Nested`, is not resolved.** Only the simple
  name `Nested` (in scope or via a single-type import) resolves; writing `Outer.Nested` fails with
  `no se encuentra el símbolo: Outer.Nested`. Verified in isolation (`finding_101.java`): both the
  same-file self-qualified form (`finding_101.Flag`) and the cross-file form (a sibling naming
  `Outer.Flag` with `Outer` on the classpath) fail, while `Flag[]` (simple name) compiles. So name
  resolution doesn't walk from an enclosing type to its member type through the `Outer.Nested` form.
  Same family as **#20** (a qualified `new` name is miscompiled) — the compiler struggles with
  qualified names generally. Surfaced building `jakarta.validation.constraints.Email`, whose
  `flags()` returns `Pattern.Flag[]` (`Pattern.Flag` being a *sibling file's* nested enum).
  - **Worse — the import form is silent wrong codegen, not a fix.** Importing the nested type and using
    the simple name (`import …Pattern.Flag; … Flag[] flags();`) makes it *compile*, but the emitted
    descriptor uses `Object` for the cross-file nested type — e.g. `Flag[]` → `[Ljava/lang/Object;`
    (not `[Ljakarta/validation/constraints/Pattern$Flag;`), and a **non-array** param `Path.Node`
    (imported) → `Ljava/lang/Object;` (not `Ljakarta/validation/Path$Node;`, seen in
    `TraversableResolver.isReachable`/`isCascadable`). So a **same-file** nested reference works and
    emits the right type (`Pattern.flags()` → `Pattern$Flag[]`), but any **cross-file** nested reference
    has *no* clean workaround: `Outer.Nested` won't compile, and the imported simple name silently emits
    `Object` (array or not). Where the divergence is only in the descriptor, the faithful source is kept
    and the entry allowlisted (`TraversableResolver`); where it corrupts an annotation element, the
    member is omitted (`Email.flags()`).
  - Repro: `KajiLibrary/repros/finding_101.java`.
  - Library handling: `Email.flags()` is **omitted** (subset) rather than shipping a wrong `Object[]`
    descriptor; it returns once cross-file nested resolution is fixed.
  - **Update (2026-08-13): it also bites SAME-PACKAGE, different-file.** `java.util.SequencedMap` and
    `java.util.NavigableMap` import `java.util.Map.Entry` and use the simple name `Entry`; every method
    returning it emits `()Ljava/lang/Object;` instead of `()Ljava/util/Map$Entry;` (8 allowlisted
    entries). So the trigger is the nested type living in another **file**, not another package — the
    same compilation unit is what makes `Map.Entry` work inside `Map.java` itself.

- **#102 — ⬜ a call to a method returning an array of a *cross-package* reference type is generated
  with an `Object[]` return descriptor.** Compiling a class that does `Field[] fs = c.getDeclaredFields()`
  (where `c` is a `java.lang.Class` and `getDeclaredFields()` is declared `()[Ljava/lang/reflect/Field;`)
  emits the `invokevirtual` with descriptor **`()[Ljava/lang/Object;`** instead of
  `()[Ljava/lang/reflect/Field;`. The **declared** method reads back correctly (`javap -s` on our
  `Class.class` shows `()[Ljava/lang/reflect/Field;`), and the JDK javac emits the right call — so it's
  the **call-site codegen** that erases the array element type to `Object` when the element is a
  reference type from another package (here `java.lang.reflect.Field`, imported into a default-package
  class). At the VM this call then fails `vtable_slot` (no `getDeclaredFields()[Ljava/lang/Object;`
  exists) → `NoSuchMethodError`. A same-package array-element return (`repro102.Elem[] make()`) compiles
  the call **correctly**, both same-file and cross-file — so the trigger is the cross-package element
  type (family of #100/#101: reference-type erasure/resolution in codegen).
  - Evidence: `java/AnnoRead.java` compiled with the frozen javac → `Object[]` call (VM throws
    `NoSuchMethodError`); the *same* source compiled with the JDK javac → `[Ljava/lang/reflect/Field;`
    call, and the reflection test passes (`reads_constraint_annotations_off_fields` = 102101).
  - **This blocks the Bean Validation *engine*:** a reference `Validator` compiled by the frozen javac
    can't call `Class.getDeclaredFields()`. The runtime side is done and verified (getDeclaredFields /
    Field.get / the annotation-reading natives all work when the caller is compiled by the JDK javac);
    the engine waits on this fix (or a `Reflect.declaredFields(Class)` helper typed `Object[]` as a
    library workaround).

- **#103 — ⬜ missing `int`→`long` widening (`i2l`) on assignment and method arguments.** An `int`
  value used where a `long` is required must be widened with `i2l` (JLS §5.1.2, widening primitive
  conversion). The frozen javac omits it: `long t = 5;` emits `iconst_5; lstore_0` (should be
  `iconst_5; i2l; lstore_0`), and `obj.wait(5)` (a call to `wait(J)V`) emits `iconst_5; invokevirtual`
  with **no `i2l`** — pushing an `int` where the callee reads a `long`. Only an explicit `long` literal
  is correct: `obj.wait(5L)` → `ldc2_w 5L; invokevirtual`. Our lenient, `Value`-tagged interpreter masks
  it for arithmetic (an `Int` in a long slot still adds), but any consumer that distinguishes the two —
  a native reading the argument, or the real JVM's bytecode verifier (which would reject `int` on the
  stack for a `long` parameter) — breaks. Surfaced building `Object.wait(long)` for the JSR 166 locks:
  `wait(5)` was read as `wait(0)` (= indefinite wait per the JLS) → deadlock. Worked around at the VM by
  reading the timeout leniently as `Int` or `Long`; the real fix is to emit `i2l` (and, generally, the
  right widening conversion) at the point an `int` expression is used in a `long`/`float`/`double`
  context. Repro: `repros/finding_103.java`.
  - **Update (2026-08-13, JSR 166 C3): #103 bites *library* code, not just fixtures.** Two live
    defects it caused in KajiLibrary: `CountDownLatch.getCount()` compiled its `long n = count;`
    (int field) to `getfield …:I; lstore` with no `i2l`, so the returned value was int-tagged and the
    caller's `lcmp` against `0L` **panicked the VM**; and `TimeUnit.convert(Duration)` passed an int
    `nano` to the long parameter of `cvt`, silently mis-converting. **An explicit `(long)` cast DOES
    emit `i2l` correctly** (`long n = (long) count;` → `getfield; i2l; lstore`), so that is the
    library-side workaround, applied in both places. Standing rule for KajiLibrary until this is
    fixed: never rely on implicit int→long widening — write the cast, and use `L` literals. Likely the same gap exists for `int`→`float`/`double` and
  `long`→`float`/`double` widenings in the same positions — worth checking together.

- **#104 — ⬜ the class reader ignores a classpath method's `Exceptions` attribute.** When the
  frozen javac loads a class from `-cp` and checks an override, it does not read the overridden
  method's `throws` clause (the `Exceptions` attribute) — so an override that declares the *same*
  checked exception reads as **wider** and is rejected ("declara lanzar `X`, más ancho que lo que
  permite `<iface>` §8.4.8.3"). The **write** side is correct: our compiled `Condition.class` carries
  `Exceptions: throws java.lang.InterruptedException` (JDK `javap` shows it) — it's the **read** side
  that drops it. Surfaced implementing `Condition.await() throws InterruptedException` in a class that
  `implements` our subset `Condition`. Workaround: since the KajiLibrary bodies raise no checked
  exception (our `Object.wait`/`wait(long)` are declared without `throws`), the overriding methods
  simply **omit** `throws` — a narrower throws is always a valid override. Fix: parse the `Exceptions`
  attribute in the class-file reader so classpath `throws` clauses are honoured.

- **#105 — ⬜ `monitorexit` is not emitted on an early `return` inside a `synchronized` block.** A
  `synchronized (obj) { … return …; … }` must release the monitor on **every** exit path. The frozen
  javac emits the `monitorexit` for the fall-through exit and installs the exception handler
  (`… monitorexit; athrow`, covering `throw`s and implicit exceptions) — but an **early `return`**
  inside the block jumps straight out with **no `monitorexit`**, leaking the monitor. Concretely, in
  `ReentrantLock.lock()` the reentrant fast path `if (owner == me) { holdCount++; return; }` compiled to
  `… putfield holdCount; return` with no `monitorexit` before it (the `monitorenter` was never undone).
  Every reentrant call leaked one level; after 100 iterations the internal `sync` monitor sat at
  `owner=<dead thread>, count=100`, so no other thread could ever acquire it → deadlock (the VM's
  `execute` returned `None`, nothing runnable). **Synchronized *methods* (`ACC_SYNCHRONIZED`) are
  unaffected** — the VM releases their monitor on frame exit, on any path — which is why the `atomic.*`
  classes (all synchronized methods) work. Only synchronized *blocks* with an early `return` leak.
  Workaround: write such methods **single-exit** — compute into a local inside the block and `return`
  it *after* the block; keep `throw` inside (the exception handler releases correctly). Fix: duplicate
  the `monitorexit` before every early `return` inside a synchronized block (what the real javac does
  via the synthesized finally). This is the most impactful finding of the JSR 166 work — any
  synchronized block with an early return is silently broken. Repro: `repros/finding_105.java`.

---

## Priority

**Open: #13–#17** (#1–#12 fixed and in `bin/javac-frozen.exe`). These five surfaced while retrofitting
the collections and writing the streams (H3). Each has a **versioned repro** and a KajiLibrary
workaround, so none blocks the library — they're quality items. Rough impact order:

- **#21 — ✅ FIXED (was TOP PRIORITY, CRITICAL regression).** Root cause: the desugar's FQN for the
  member-synthesis lookup didn't include the **package**, so a **named-package** enum lost all its
  machinery (a default-package enum was fine — the note's "every enum" was a misdiagnosis). Fixed in
  `src/javac` (regression test added). **Library side DONE (2026-08-06):** `bin/javac-frozen.exe`
  refreshed from the fixed build (`$LOCALAPPDATA/Temp/jvm-target/debug/javac.exe`, old snapshot kept as
  `bin/javac-frozen.exe.pre21`); java.time enums recompiled — `ChronoField` (10 consts), `ChronoUnit`
  (16), `Month` (12), `DayOfWeek` all have their machinery back, gate PASS (the gate now *counts* the
  constants: ChronoUnit 23 match vs the ~5 that masked the drop). Value types didn't need recompiling
  (their Fieldrefs to the constants resolve now). H6-T5 `%t` unblocked.

- **#17** — highest: it's what actually blocks a real feature (`Stream.collect(Collector)`); it's the
  method-level analogue of #9 (generic override) and likely a small extension of that fix.
- **#15** — same family (member lookup through a *generic* superinterface); with #17 these two round
  out generics on functional interfaces (`BinaryOperator`/`BinaryOperator`-shaped inheritance).
- **#16** — lambda target-type inference through a generic constructor (codegen bails to "no
  invokedynamic yet"); annoying but the explicit-local workaround is trivial.
- **#13** (anon/inner capture in a generic enclosing class) and **#14** (`-cp` superinterface member
  lookup) — localized; clean workarounds (same-file iterators; compile `Collections` without `-cp`).
- **#19** (performance) — not a correctness bug, but it makes iterating on multi-class files slow and
  probably explains the long-standing "`-cp KajiLibrary` is too slow" pain. Likely a small, high-value
  fix (memoize finder lookups + source-shadows-classpath short-circuit).
- **#20** — **highest among the correctness items after #17**: it's *silent wrong codegen* (a `new`
  with a qualified name drops the method body to `areturn`), which the API-shape gate cannot detect —
  so it can ship broken bytecode unnoticed. Two-part fix: resolve qualified names in `new`, and make a
  `new` of an unresolved type a hard error instead of a silently-empty body.

Repros for all open findings live in `KajiLibrary/repros/finding_NN.java` (13–17 added this session),
each confirmed to reproduce against the current `bin/javac-frozen.exe` — re-verify against the live
`target/debug` before/after fixing.

Status (2026-08-04): fixed and folded into `bin/javac-frozen.exe` — **#1–#12**. Open — **#13, #14,
#15, #16, #17, #18** (found dogfooding H3 collections/streams and H4 java.time; workarounds applied
in KajiLibrary; repros `finding_13`…`finding_18` in `KajiLibrary/repros/`) plus **#19** (performance;
`-cp` finder slowness, found dogfooding H5 java.util.regex; no single-file repro — needs a stale `-cp`)
and **#20** (silent wrong codegen — a `new` with a qualified name drops the body to `areturn`; found
dogfooding H6 java.util.Formatter; repro `finding_20.java`) and **#21** (CRITICAL regression — the
compiler drops ALL enum machinery; every enum unusable, H4/java.time silently broken; both frozen and
live javac; repro `finding_21.java`).

- **#106 — ⬜ resolution/erasure defects surfaced building the JPA metamodel & criteria packages.**
  Three related codegen/resolution issues in interfaces with rich generics (jakarta.persistence.metamodel):
  (a) a **qualified** reference to a JDK type in a signature does not resolve — `java.lang.reflect.Member`,
  `java.util.Collection/List/Set/Map`, `java.sql.Date` all fail as `no se encuentra el símbolo`; the fix is
  an `import` + simple name (same family as the `java.lang.reflect.Field` note and #101). (b) A simple-name
  type that exists in **both** `java.lang.reflect` and the current (same) package resolves to the JDK one:
  `Type<X> getType()` (meant as `jakarta.persistence.metamodel.Type`) emits `()Ljava/lang/reflect/Type;`.
  An explicit `import jakarta.persistence.metamodel.Type;` fixes it **inconsistently** (worked for
  IdentifiableType, not SingularAttribute/MapAttribute) — those two are allowlisted. (c) A generic return
  whose element involves a type variable + wildcard (`Set<Attribute<? super X, ?>>`) can erase the whole
  return to `Object` (`()Ljava/lang/Object;` instead of `()Ljava/util/Set;`) — the #100 family. Net: the
  metamodel compiles and gates with 2 allowlisted `Type` divergences; criteria (P5) hits the same class of
  issues at larger scale. Fix direction: honor qualified JDK-type references in the class-file reader/finder;
  prefer same-package over non-imported java.lang.reflect for simple names; erase parameterized returns to
  their raw type, not Object.

- **#108 — ⬜ a chained call through an INTERFACE-typed intermediate is silently dropped (and
  corrupts the stack).** `lock.writeLock().lock();` — where `writeLock()` returns the interface
  `java.util.concurrent.locks.Lock` — compiles to a single stray `pop`: **both** calls vanish, and the
  `pop` runs on an empty stack. Chaining through a **class**-typed intermediate is fine
  (`c.inner().act()`, `c.sb().append("x")` both emit correctly), so the trigger is the *interface*
  receiver of the second call. Binding the intermediate to a local first is a correct workaround
  (`Lock w = lock.writeLock(); w.lock();` → `invokevirtual` + `invokeinterface`, both emitted).
  This is the most dangerous class of defect we have seen — worse than #20: it is **silent wrong
  codegen that the shape gate cannot catch** (the enclosing class's API is unchanged), and it deletes
  the very operation the caller asked for. Found building `ReentrantReadWriteLock`: every
  `lock.writeLock().lock()` / `.unlock()` in the behaviour fixture compiled to nothing, so the test
  "passed the lock" while never locking. Repro: `repros/finding_108.java`.
  - **Related (extension of #102): a cross-package NESTED type as a return type erases to `Object`.**
    With `readLock()` declared to return the JDK-faithful covariant nested type
    `ReentrantReadWriteLock.ReadLock`, the call site emitted
    `readLock:()Ljava/lang/Object;` → `NoSuchMethodError` at run time. The class finder appears not to
    resolve `Outer$Inner` names from the classpath. KajiLibrary's `ReentrantReadWriteLock` therefore
    declares `readLock()`/`writeLock()` as returning the **`Lock` interface** — a descriptor the JDK
    class also has (it emits exactly that bridge), so the gate still matches — and the code runs.

- **#109 — ⬜ a boolean-valued conditional expression is rejected as "operando no numérico".** The
  ternary `o == null ? e == null : o.equals(e)` — both branches `boolean` — fails to compile; an
  int-valued (`c ? 1 : 0`) or reference-valued (`c ? null : "x"`) ternary compiles fine, so the
  conditional operator's type is being computed numerically instead of by JLS §15.25 (which folds a
  both-`boolean` conditional to `boolean`). Surfaced writing the null-safe equality used by the
  concurrent collections. Workaround: an explicit `if`/`else` helper —
  `private static boolean eq(Object a, Object b)` — used by `CopyOnWriteArrayList`,
  `ArrayBlockingQueue` and `LinkedBlockingQueue`.

- **#110 — ✅ FIXED (library session, 2026-08-18) — a STATIC field of a *classpath* class was read
  with `getfield` instead of `getstatic`.**
  `Integer.MAX_VALUE` compiles to `aload_0; getfield java/lang/Integer.MAX_VALUE:I` — the wrong opcode
  *and* a bogus receiver (`this`, or whatever happens to be on the stack in a static method). At the VM
  this is `field_offset: field not found in the class or its superclasses`. A static declared in the
  **same compilation unit** compiles correctly (`getstatic`), so the defect is in the class-file reader:
  it does not record `ACC_STATIC` for fields loaded from `-cp` (the same blind spot as #104's
  `Exceptions` attribute). **Scope is large:** every reference to an enum constant of a separately
  compiled class (`TimeUnit.SECONDS`, `ChronoField.YEAR`, `Month.JANUARY`, `Locale.US`…) is broken at
  run time, which is why it went unnoticed — java.time and the formatter were validated by self-tests
  in *real* Java plus the shape gate, never executed on our VM. Worked around in KajiLibrary by writing
  the literal (`2147483647` in LinkedBlockingQueue) and by taking the unit-free constructor path in
  Executors; fixture code uses `TimeUnit.valueOf("MILLISECONDS")`, since static *methods* resolve fine.
  Repro: `repros/finding_110.java`.

- **#111 — ⬜ a method call on a receiver whose static type is a TYPE VARIABLE is silently dropped.**
  `boolean viaTypeVar(Object o) { return value.equals(o); }` (where `value` is a `T` field) compiles to
  **`aload_1; areturn`** — the argument is returned, as a reference, from a `boolean` method; the call
  never happens. Binding the receiver to an `Object` local first compiles correctly
  (`aload; invokevirtual Object.equals; ireturn`). This is the same silent-drop family as #108 (there
  the receiver was interface-typed), and just as dangerous: the shape gate cannot see it, and the
  emitted method does something entirely different from its source. Found in
  `ConcurrentHashMap.remove(key, value)`, which ended up branching on its own argument instead of
  comparing values. Repro: `repros/finding_111.java`.

- **#112 — ✅ FIXED (library session, 2026-08-18) — a `static final` primitive constant was neither
  folded nor initialized.** For
  `private static final int NEW = 0;` the compiler writes the value **only** into the field's
  `ConstantValue` attribute — it emits no `<clinit>`, and it reads the field at use sites with
  `getstatic` rather than folding the constant in (JLS §13.1 requires constant expressions to be folded;
  real javac emits `iconst_0`, so a JDK class never depends on the VM applying ConstantValue for
  primitives). Our VM does not apply `ConstantValue` at class initialization, so **every such constant
  reads back as 0**. Found in `FutureTask`, whose four state constants (`NEW`/`COMPLETED`/`FAILED`/
  `CANCELLED`) were all 0: `state = COMPLETED` left the task indistinguishable from unfinished, so
  `get()` waited forever and the VM ran out of runnable threads. Either half is a fix — fold constants
  in the compiler (preferred, matches javac) or honour `ConstantValue` in the runtime. Worked around by
  dropping `final`, which forces a real `<clinit>`. Repro: `repros/finding_112.java`.

- **#118 — the varargs flag is never emitted, and a spread call to a *classpath* varargs method is
  silently DELETED.** Two halves, the second being silent wrong codegen the shape gate cannot see.
  - **Write side:** a `T...` parameter gets the right descriptor (`[Ljava/lang/Object;`) but the method
    never gets ACC_VARARGS — ours `flags: (0x0009) ACC_PUBLIC, ACC_STATIC` where real javac emits
    `(0x0089)` with the varargs bit. Pre-existing and library-wide: `java/lang/String.class`'s two
    `format` methods lack it too, so `Method.isVarArgs()` answers false for all of them.
  - **Read side (the dangerous half):** with the flag absent, a caller compiled against that `.class`
    cannot tell the method is varargs, finds no applicable overload for a spread call, and emits
    **nothing** — no diagnostic, no `invokestatic`, no `anewarray`. Verified independently:
    `Va.join("-", "a", "b")` compiles to `ldc "-"; ldc "a"; ldc "b"; areturn`, which returns `"b"` and
    strands two operands on the stack (bytecode the real JVM verifier would reject). The no-argument
    form `Va.join("-")` is deleted the same way — no empty array is built.
  - **Correct forms:** passing the array explicitly (`Va.join("-", parts)`) compiles right, and so does
    a **same-file** varargs call — there the compiler still has the source AST and never consults the
    flag, which is why this went unnoticed.
  - Same unguarded overload-lookup-failure path as **#114** (a call that resolves to nothing must be a
    compile error, never an empty expression); closest relatives are #108 and #111.
  - **Impact:** `PrintWriter.printf`/`format` as compiled are *correct* — their bodies pass the
    already-built array straight through — but any future KajiLibrary or user code writing
    `pw.printf("x=%d", n)`, or `String.format("%s", x)` against our own `java/lang` on the `-cp`, will
    silently compile to nothing. Worth fixing before anything downstream uses printf-style calls.
  - Repro: `repros/finding_118.java` (compile `Va118` first, then the caller with `-cp`).

- **#119 — a type from a SUBPACKAGE of `java.lang` erases to `java.lang.Object` in the descriptor of
  a call site in another compilation unit.** Our `java/lang/ref/WeakReference.class` declares
  `(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V`, but a caller compiled against it emits
  `invokespecial java/lang/ref/WeakReference."<init>":(Ljava/lang/Object;Ljava/lang/Object;)V` —
  verified independently. Affects parameters, returns and constructors alike
  (`ReferenceQueue poll()` becomes `()Ljava/lang/Object;`). Identical hierarchies placed in `zz.ref`,
  `a.b.c` or a single-segment package all compile correctly, so it is specific to subpackages of
  `java.lang` — plausibly the simple-name lookup falling back to the hard-coded `JAVA_LANG` path
  (`java.lang.ReferenceQueue`), missing, and stubbing the type as Object instead of reporting an
  error. Same "resolution failure becomes silence" root as #108/#111/#118.
  - **LIVE IMPACT:** `java.util.WeakHashMap` compiles and gates clean but **cannot run** — every
    WeakReference construction and every `ReferenceQueue.poll()` links to a descriptor that does not
    exist (NoSuchMethodError). There is **no source-level workaround**; the source is correct Java.
  - Repro: `repros/finding_119.java`.

- **#120 — a call to a method the receiver only INHERITS from an EXTERNAL superclass is silently
  deleted, leaving verifier-invalid bytecode.** `WeakReference.get()` is declared on `Reference`;
  calling it through a `WeakReference`-typed receiver emits no invoke at all — `return w.get();`
  compiles to a bare `0: areturn` **with an empty operand stack** (verified independently). That is
  worse than #108/#111, which at least left the arguments behind. Seen in the wild as `astore 5` with
  nothing pushed. Inheritance within the same compilation unit is fine, so this is another
  classpath-reader blind spot, alongside #110 (ACC_STATIC), #104 (Exceptions) and #118 (ACC_VARARGS):
  the reader does not carry the supertype's method table.
  - **Workaround:** cast the receiver to the class that *declares* the method —
    `Reference r = (Reference) w; r.get();` emits the `invokevirtual`. The plain widening assignment
    `Reference r = w;` is rejected outright ("tipo incompatible"), so the cast is required. Applied in
    `WeakHashMap`'s entry wrapper.
  - Repro: `repros/finding_120.java`.

- **#121 — `super(...)` fails to resolve when the target constructor takes a PARAMETERIZED type
  mentioning the superclass's type variable.** `abstract class G<E> { G(Class<E> t) {} }` with
  `class Sub<E> extends G<E> { Sub(Class<E> t) { super(t); } }` reports "el generador de bytecode
  todavia no soporta un super(...)/this(...) que no resolvio a ningun constructor". It is a
  *resolution* failure, not a codegen gap: the constructor exists and the argument type matches
  exactly. Works with `int`, `Object`, a bare `E`, or `Class<?>` on a non-generic superclass; fails
  identically with a user-defined `Box<E>`, within one file and across files.
  - This is exactly the shape of the JDK's `EnumSet(Class<E>, Enum[])`, so KajiLibrary's `EnumSet`
    was given a no-argument constructor with the subclass assigning the field directly.
  - Repro: `repros/finding_121.java`.

- **#101 — additional manifestation, worse than the descriptor erasure already documented.** A class
  implementing a nested interface via an import (`final class LhmEntry<K,V> implements Entry<K,V>`
  with `import java.util.Map.Entry;`) emits an **empty `interfaces[]` table**: javap shows the
  interface only from the `Signature` attribute, and the constant pool has no `Map$Entry` Class entry
  at all. So the implements clause is silently DROPPED, not merely erased. Harmless where the library
  only passes such an object around by its concrete type, but any call through a `Map.Entry`-typed
  receiver would fail at run time.

- **#122 — overload resolution counts a class's declaration and its interface's re-declaration as
  two distinct candidates.** `ExecutorCompletionService<V>` declares `submit(Callable<V>)` and
  implements `CompletionService<V>`, which declares the same method. A call through the *class*-typed
  receiver is rejected as "la referencia a `submit` es ambigua"; typing the local as the interface
  compiles fine. This is not ordinary ambiguity — the two candidates are the same method, one being
  the implementation of the other, and JLS 15.12.2.5 removes such duplicates before the
  most-specific test. It bites any class that implements an interface and re-declares its methods,
  which is the normal shape for a concrete implementation.
  - Workaround in the library: fixtures declare the local with the interface type.

- **#123 — a covariant override is rejected when the override's return type inherits its
  relationship to the overridden type from a CLASSPATH generic hierarchy.** `SetJoin<Z,E> extends
  PluralJoin<Z,Set<E>,E>` narrows `PluralAttribute<? super Z, Set<E>, E> getModel()` to
  `SetAttribute<? super Z, E> getModel()`, which is legal because the classpath declares
  `SetAttribute<X,E> extends PluralAttribute<X, Set<E>, E>`. The compiler rejects it with
  *"el retorno de `getModel` no es compatible con el de `PluralJoin`: SetAttribute no es un subtipo
  de PluralAttribute"* — note the message names the **erasures**, so the check is failing before any
  generic reasoning: the super-interface chain of an external type is not being consulted for the
  return-type subtype test. Same failure, same shape, in `CollectionJoin` and `ListJoin`.
  - **Live impact:** these are 3 of the 7 jakarta.persistence classes still missing; there is no
    source-level workaround (widening the return to `PluralAttribute` would diverge from the API).
  - Sibling of #120 (a supertype's *method table* isn't read from the classpath): here it's the
    supertype *chain* that isn't read. Both point at the same gap in the class-file reader.
  - Repro: pending — the minimal form needs the hierarchy split across a classpath, which tripped
    finding #4 (a same-package unqualified reference isn't auto-loaded) before it could be reduced.

- **#124 — a field initializer in an INTERFACE synthesizes a constructor ON THE INTERFACE.** Any
  initialized interface field (`interface I { int VALUE = 7; }`) is lowered as if it belonged to a
  class: the emitted interface gains
  `public default I(); Code: aload_0; invokespecial Object.<init>; bipush 7; putstatic VALUE:I; return`.
  Two defects in one method:
  1. **An interface must never declare `<init>`.** Its fields are implicitly `public static final`
     (JLS §9.3) and are initialized in `<clinit>` (JVMS §2.9.2). The method is also emitted as
     `default` — an interface method *with a body* — and begins `aload_0; invokespecial
     Object.<init>` against a `this` that cannot exist.
  2. **The field is therefore never assigned.** Nothing calls that constructor, so the value
     survives only in the field's `ConstantValue`, which our VM does not apply — compounding #112.
     With a non-constant initializer (`int C = "abc".length();`) there is no `ConstantValue` either,
     so the field is unconditionally zero.
  - Surfaced writing `java.text.CharacterIterator`, whose `char DONE` is part of the JDK contract.
  - **The API-shape gate DOES catch this one**, as an `EXTRA <init>()V` — a rare case where the
    gate sees a codegen defect, because the bogus member is *added* to the public surface rather
    than silently miscompiled. (Contrast #110/#112/#119, which the gate cannot see at all.)
  - Repro: `KajiLibrary/repros/finding_124.java` (both the constant and the computed shapes).
  - Library handling: `CharacterIterator.DONE` is **omitted** — a missing member is a legal subset,
    a spurious one is not — and the implementations use the literal `'￿'`. It returns once
    this is fixed.
  - Fix direction: the lowering that moves field initializers into constructors must check the
    enclosing type's kind. For an interface — and for a `static` field of a class — the initializer
    belongs in `<clinit>`, and no `<init>` may be synthesized for an interface at all.

- **#101 — the `import` sidestep for a nested type compiles but emits the WRONG descriptor.**
  Previously recorded as a clean workaround ("importing a nested type works"). It is not: with
  `import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;`,
  `void removeAttributeNodes(PersistentAttributeType t)` compiles, and emits
  `(Ljava/lang/Object;)V` instead of `(Ljakarta/persistence/metamodel/Attribute$PersistentAttributeType;)V`.
  So the class links against a method that does not exist — the same shape as the original #101
  erasure, just reached through the workaround. Found emitting `jakarta.persistence.Graph`;
  allowlisted there, and the entry comes out when #101 is fixed.

---

## Los dos fixes de esta sesion (#110 y #112) — nota para el merge

Ambos se arreglaron en `src/javac`, que es dominio de la sesion de compilador: **revisar antes de
mergear**. Cada cambio lleva su comentario en el codigo explicando el bug, no solo el que.

**#110 — `classfile.rs` tiraba los access flags del campo.** El loop de campos hacia
`r.u2()?; // access` y seguia de largo, asi que `ExtField` no sabia si el campo era estatico.
`enter.rs::build_external` creaba el simbolo con `modifiers: Vec::new()`, y `codegen::field_info`
decide el opcode justamente por ese `modifiers` — o sea que "no es estatico" era el default
silencioso para **todo** campo del classpath. Cambios: `ExtField.is_static`, poblado desde
`ACC_STATIC`, y propagado como `Modifier::Static` en `build_external`. El codegen no se toco: ya
elegia bien, le mentian los datos.

**#112 — no habia plegado de constantes.** Se agrego:
- `classfile.rs`: el pool retiene los valores (`Integer`/`Long`/`Float`/`Double`/`String`, antes
  salteados como `Other`) y `read_attributes` devuelve el `ConstantValue` ademas del `Signature`.
- `symbol.rs`: mapa lateral `field_consts: SymbolId -> FieldConst` (+ `set_field_const`/`field_const`),
  al lado de `resolved_map`.
- `enter.rs`: lo puebla para los campos del classpath (desde `ConstantValue`) y para los declarados
  en la fuente, usando **el mismo predicado** (`const_field_value`) que decide el atributo — si
  divergieran, el campo se emitiria con valor y se leeria sin el.
- `codegen.rs`: `read_field` pliega antes de emitir nada, con `push_const`. De paso se unifico
  `ConstVal` con `classfile::FieldConst`, que eran el mismo tipo duplicado.

Verificado con `javap` sobre bytecode emitido (fixtures en el scratchpad): `Integer.MAX_VALUE` sale
`ldc int 2147483647` y `Long.MIN_VALUE` sale `ldc2_w` (antes: `getfield`); un `static final int` de la
propia fuente sale `bipush 7`; y un estatico NO constante (`ChronoUnit.NANOS`, `IsoChronology.INSTANCE`)
sale `getstatic`, que es el caso de las 26 clases medidas. **La suite de Rust NO se pudo correr**: el
linker MSVC (`link.exe`) desaparecio del entorno a mitad de sesion — `cargo` falla con "linker not
found" incluso para binarios que habia linkeado 20 minutos antes. Correr `cargo test` antes de mergear.

**Impacto medido antes del fix** (scripts `scan110.py`/`scan112.py`, sobre los `.class` emitidos):
26 clases con al menos un `getfield` a un campo estatico (21 de ellas `java.time`, mas `Formatter`,
`TimeUnit` y 2 de jakarta), y 14 clases declarando 65 constantes que leian 0. Union: 34 clases que
gateaban limpio y no podian correr. Vale versionar esos scripts como chequeo de regresion.

---

## jakarta.persistence — cerrado en 203/203, con 10 metodos omitidos (2026-08-18)

Las 6 clases que faltaban entraron. Ninguna necesito un fix de compilador: entraron **omitiendo el
miembro que no compila**, que el gate acepta porque compara la superficie declarada y exige
SUBCONJUNTO, no igualdad. Queda anotado para que nadie lo lea como "la API esta completa".

| Clase | Omitido | Por que | Vuelve cuando |
|---|---|---|---|
| `SetJoin` / `CollectionJoin` / `ListJoin` | `getModel()` | #123 | se arregle #123 |
| `MapJoin` | `getModel()`, `entry()` | #123 / #101 | se arreglen #123 y #101 |
| `CriteriaBuilder` | `currentDate()`, `currentTime()`, `currentTimestamp()` | piden `java.sql`, otro modulo | exista `java.sql` (arrastra `java.util.Date`) |
| `CriteriaBuilder` | `toBigDecimal()`, `toBigInteger()` | piden `java.math` | exista `java.math` |

El `getModel()` omitido **se hereda** de `PluralJoin`, asi que la unica perdida real es el retorno
covariante (un llamador recibe `PluralAttribute` en vez de `SetAttribute` y tiene que castear).

`CriteriaBuilder` ademas lleva **14 entradas de allowlist, todas del #100**: una variable de tipo
acotada erasa a `Object` en vez de a su cota (`N extends Number` -> `Number`, `Y extends Comparable`
-> `Comparable`, `C extends Collection` -> `Collection`, `M extends Map` -> `Map`). La fuente declara
la cota correctamente; lo que sale mal es el descriptor. Son 14 metodos que **no linkearian** contra
un llamador real — hoy no importa porque JPA es gate-only y no ejecuta, pero importa el dia que si.

`PersistenceProviderResolverHolder` es la unica cuyo **cuerpo** es nuestro y no el de la spec: la
version de referencia descubre proveedores con `ServiceLoader`, cachea por class loader con
`WeakReference` y loguea con `java.util.logging`, nada de lo cual existe. Como tampoco hay ningun
proveedor que descubrir, el resolver por defecto devuelve lista vacia — honesto en vez de simulado —
y `setPersistenceProviderResolver` sigue funcionando, que es el punto de la indireccion.

- **#125 — el emisor no soporta `super.metodo(...)`.** `super.write(b, off, len)` en un override falla
  con "el generador de bytecode todavia no soporta `super`". La invocacion explicita de constructor
  (`super(...)`, §8.8.7.1) SI anda: lo que falta es el acceso calificado por `super` a un metodo
  (§15.11.2), que es un `invokespecial` sobre el receptor `this` con el metodo del supertipo.
  - Repro: cualquier clase que extienda otra y llame `super.m()` en el override de `m`.
  - **Es la forma canonica de "extender sin reemplazar"**, asi que aparece apenas se escribe una
    jerarquia de decoradores: lo encontraron `GZIPOutputStream.write` (que quiere sumar el CRC a lo
    que ya hace `DeflaterOutputStream.write`) y `GZIPInputStream.read`.
  - **Agrava un workaround ya documentado:** la nota de #14 dice "evitarlos o usar `super.m()`" — esa
    salida no existe hoy.
  - Workaround en la biblioteca: (a) si el cuerpo del padre son dos lineas, inlinearlas; (b) si no,
    mover el cuerpo del padre a un metodo package-private con otro nombre y que el hijo llame a ese
    (heredado, sin calificar); (c) si el override solo delegaba, borrarlo y heredar.

- **#126 — el RETORNO de un override contra un supertipo del CLASSPATH se erasa a `Object`.**
  `CallSite` (clase abstracta) declara `public abstract MethodHandle getTarget()`. Compilada sola,
  emite `()Ljava/lang/invoke/MethodHandle;` — correcto. Pero al compilar `MutableCallSite extends
  CallSite` con `CallSite.class` en el `-cp`, el override `public final MethodHandle getTarget()`
  emite `()Ljava/lang/Object;`.
  - **Lo revelador: el PARAMETRO del mismo tipo sale bien.** `setTarget(MethodHandle)` en la misma
    clase emite `(Ljava/lang/invoke/MethodHandle;)V`. O sea que el tipo resuelve perfecto; lo que
    se pierde es especificamente el retorno **cuando el metodo es un override**.
  - Se disparo en las tres subclases de `CallSite` (`getTarget` y `dynamicInvoker`), 6 metodos.
  - Familia de #123 (override covariante contra jerarquia del classpath), pero **mas amplio**: aca
    no hay covarianza — el tipo de retorno es EL MISMO que el del supertipo.
  - Sin workaround de fuente: declarar el tipo, calificarlo o importarlo dan lo mismo. Allowlist.

- **#127 — un `default` NEGATIVO de una anotacion se descarta en silencio.** Para
  `int secondPrecision() default -1;` el emisor no escribe el atributo `AnnotationDefault`: el
  metodo sale sin default. Con `default 255` (positivo) el atributo se emite bien, asi que el
  disparador es el **menos unario** en la posicion de valor por defecto.
  - Visto en `jakarta.persistence.Column.secondPrecision`.
  - **El gate NO puede verlo**: un `default` no esta en el descriptor, asi que la clase pasa con
    `0 mismatch` mientras el valor desaparece. Lo caza `tools/check_jpa_defaults.py`, que compara
    los `AnnotationDefault` uno por uno contra el jar de referencia.
  - Consecuencia real: una anotacion sin su default NO es la misma anotacion — un proveedor que
    lea `secondPrecision` recibe "sin especificar" en vez de -1.
  - Familia del patron del dia: cuando algo no se puede representar, se emite silencio en vez de
    un error.
  - **Ampliacion: tampoco se emite un default con valor de ANOTACION.** Para
    `ForeignKey foreignKey() default @ForeignKey(value = ConstraintMode.PROVIDER_DEFAULT);` la
    fuente compila sin error y el `.class` sale igual de mudo, sin `AnnotationDefault`. Afecta a
    `JoinColumn`, `JoinColumns` y `AssociationOverride` en jakarta.persistence.
    Los defaults simples (int positivo, String, boolean, constante de enum, array vacio) SI se
    emiten bien, asi que el emisor cubre las formas constantes y se calla en las otras dos:
    el menos unario y la anotacion anidada.

- **#128 — no hay forma de escribir un caracter ASTRAL en una constante.** Son dos fallas
  independientes que se tapan la salida entre si.
  - **(a) El escape del rango subrogado se rechaza.** `"\ud834\udd60"` falla con "literal string
    invalido", y `'\ud800'` con "literal char invalido". Un String de Java es UTF-16, asi que el par
    subrogado es la forma **portable y canonica** de escribir U+1D160 — es lo que emite el propio
    javac. Causa: el lexer decodifica cada `\uXXXX` a un `char` de Rust, y D800..DFFF no es un
    `char` de Rust valido. **Los escapes del BMP andan perfecto en ambos tipos de literal**; el
    rango subrogado es lo unico que falla, y es exactamente el que hace falta.
  - **(b) El emisor escribe UTF-8 estandar donde va UTF-8 MODIFICADO.** Escribiendo el caracter
    directo en la fuente UTF-8 la compilacion pasa, pero el `.class` sale mal formado: para U+1D160
    emite `f0 9d 85 a0` (4 bytes, UTF-8 estandar) cuando `CONSTANT_Utf8` exige el par subrogado con
    cada mitad en 3 bytes (`ed a0 b4 ed b5 a0`). **Nuestro propio cargador lo rechaza con
    `BadUtf8`**, asi que no es "no estandar pero anda": no carga. El `javap` del JDK tampoco lo
    imprime, muestra `???`.
  - **El gate NO puede verlo**: nada de esto esta en un descriptor. La clase pasa con `0 mismatch` y
    despues no carga.
  - Encontrado escribiendo el smoke test de `java.text.Normalizer`, cuyo caso astral hubo que sacar.
    El algoritmo si esta validado sobre todo el rango 0..0x10FFFF, pero contra el JDK, no sobre
    nuestra VM: hoy no se puede escribir el caso de prueba.
  - Sin workaround. Se puede construir el String en tiempo de ejecucion desde los `int` del par
    (`(char) 0xd834`), que es lo que hace `NormImpl`, pero una **constante** astral es inalcanzable.
  - Arreglo: (a) que el lexer guarde unidades de codigo UTF-16 (`u16`) en vez de `char` de Rust;
    (b) que el emisor de `CONSTANT_Utf8` haga el encoding modificado — subrogados en 3 bytes cada
    uno, y `NUL` como `c0 80`.
  - **`NUL` tiene el mismo defecto, comprobado.** `"A\u0000B"` emite el byte `00` crudo en vez de
    `c0 80`. Nuestro cargador lo acepta (el largo da 3, correcto) porque decodifica UTF-8 a secas,
    pero el formato prohibe el byte `00` dentro de `CONSTANT_Utf8`: una JVM real rechaza la clase.
    Es el mismo bug del emisor que (b), y por eso conviene arreglar los dos juntos.
  - Repro: `repros/finding_128.java`.
