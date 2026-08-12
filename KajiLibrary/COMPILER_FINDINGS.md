# Compiler findings from dogfooding KajiLibrary

Bugs and gaps in the own `javac` (`src/javac/`) surfaced by compiling KajiLibrary with it.
The library is compiled with the frozen snapshot `bin/javac-frozen.exe` (`--emit X.java` →
`X.class`); each compile failure — or each `.class` that is structurally wrong yet passes
`javap` — is a real compiler defect. This file is the handoff to the **compiler session**
(the library session does not touch `src/javac/`).

Legend: ✅ fixed (in `src/javac` + regression test, folded into the snapshot) · ⬜ open.

**Versioned repros:** `KajiLibrary/repros/finding_NN.java` — self-contained, one per finding.
Run from the repo root: `cargo run -- --emit KajiLibrary/repros/finding_09.java` (or
`target/debug/javac.exe --emit …`). The binary prints diagnostics but **exits 0** — judge by the
message, not the exit code. For emission-only bugs (#6) inspect with `bin/javap-clon.exe -v`.

**Status (2026-08-07): ALL findings #1–#22 are ✅ FIXED in `src/javac`**, each with a regression test.
Verified on the unified build (this javac + the refactored JVM with OS-threads/GIL): `cargo test --lib`
is **768 green, 0 failing**. #1–#12 are additionally folded into `bin/javac-frozen.exe`. #19 was a
**performance** item (slow `-cp` finder), not a correctness bug; #20 / #21 / #22 were the dangerous
ones — silent wrong codegen (a `new` with a qualified name → dropped body), dropped enum machinery for
a named-package enum (java.time went degenerate on disk), and a dangling superclass reference — none of
which the API-shape gate can catch; all three are closed. **Nothing open.**
(Historical note: #1–#12 were closed by 2026-08-03; #13–#22 surfaced 2026-08-04..06 dogfooding H3
collections/streams, H4 java.time, H5 java.util.regex and H6 java.util.Formatter, and were closed since.
The per-finding entries below keep their original reports for the record.)
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

## Fixed (#13–#22 — logged as open, now all closed)

_Every finding below was open when first found; all are now ✅ FIXED with a passing regression test
(`cargo test --lib`, 768 green). The original reports are kept for the record._

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

---

## Priority

**All closed — #1–#22 fixed** (verified by `cargo test --lib`, 768 green). Nothing left to prioritize;
the list below is kept as the historical impact order the findings were worked in (highest-impact
first). Each still has a **versioned repro** in `KajiLibrary/repros/` and had a KajiLibrary workaround
while it was open.

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

Repros live in `KajiLibrary/repros/finding_NN.java`. **Run them with their setup** — some need `-cp`
or the bootstrap classes; a naive `--emit` can hit an unrelated classpath error (e.g. `finding_09`
reports a missing `size` when `List` binds to the JDK's, not the real #9 error). The "still failing
(✗)" comments inside some repros predate their fix — the authoritative check is the regression suite.

Status (2026-08-07): **#1–#22 all fixed** — verified by `cargo test --lib` (**768 green, 0 failing**)
on the unified build (this javac + the refactored JVM with OS-threads/GIL). #1–#12 are also folded into
`bin/javac-frozen.exe`. **Nothing open.**
