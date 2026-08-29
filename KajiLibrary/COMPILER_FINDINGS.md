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
**El rango #2xx** arranca en la barrida de diff de API contra OpenJDK 25 (2026-08-22, secciones al
final): se abrio en #200 para dejar aire sobre el 1xx, que iba por #128. Ojo: chequear el numero mas
alto **no alcanza** — hay que mirar tambien el *contenido* de los findings previos. En esa barrida se
duplicaron dos (#236 era #115, #237 era #114) justamente por saltear ese paso.

---

## Indice por componente (#200–#249, barrida 2026-08-22)

El cuerpo de este archivo esta ordenado **cronologicamente por sesion**, no por componente, y un
mismo `#NNN` reaparece en varias tandas entre definicion, confirmaciones y correcciones. Este indice
existe para que cada sesion encuentre lo suyo sin leer el archivo entero. **La entrada canonica de un
finding es la primera aparicion**; las posteriores son confirmaciones o correcciones y lo dicen.

### VM / interprete — 0 (familia vacia)

### javac — codegen silencioso — 0 (familia vacia)

Era la familia mas peligrosa y la mas poblada. La guarda de #261 —que el emisor **falle fuerte**
cuando una llamada no resolvio— la desarmo de raiz: lo que antes era un `.class` mudo y roto hoy es
un error de compilacion. Lo que queda **no** es silencioso; esta aca porque el reporte original lo
era. Los dos ultimos —**#114** y **#219**— resultaron **ya cerrados** por arrastre: ver el cierre
del lote de abajo.

> Ya cerrados de esta familia: **#247** (emite los `invokeinterface`; ver la nota de residuo),
> **#251**, **#252**, **#253**-cara-muda, **#258**, **#261**.

### javac — bytecode estructuralmente invalido (el `.class` no se puede ni cargar) — 0 ✅

Familia **nueva** y **vacia el mismo dia**. Se separo de "codegen silencioso" porque el modo de
falla es otro: aca el descriptor y la firma estan bien y el archivo esta mal formado, asi que el
gate de forma no la puede ver **por construccion** — mide la API, y la API es correcta.

Cerrados: **#217** (`ireturn` en un `()J`) junto con **#217b** (el agujero del verificador que lo
dejaba pasar), **#238** (campo de interfaz con `flags: (0x0000)`), **#124** (`<init>` sintetizado
sobre una interfaz), **#257** (el `catch` caia dentro del `finally` excepcional) y **#259** (el pool
en UTF-8 estandar en vez de modificado).

> Vale la pena conservar la categoria vacia: es la unica que el gate **no puede** vigilar, asi que
> si vuelve a poblarse hay que enterarse por otro lado — corriendo, o pasandole el `javap` del JDK.

### javac — perdida de modificadores en la emision — 0 ✅

Segunda familia que se vacia. Los siete tenian la misma forma —**un flag que no se emite**— y tres
causas distintas, que vale distinguir porque se repiten:

| Causa | Cuales |
|---|---|
| dos predicados para el mismo modificador | **#110**, **#238** — hoy uno solo, `implicit_field_mods` |
| una linea que faltaba en la tabla | **#255** (`ACC_SYNCHRONIZED`), **#200/#118** (`ACC_VARARGS`) |
| **colision de bits** entre tablas distintas | **#115**, **#236** — `ACC_VOLATILE`/`ACC_TRANSIENT` comparten valor con `ACC_BRIDGE`/`ACC_VARARGS`, asi que hubo que separar campos de metodos |
| modificador **implicito** de la spec, no escrito en el fuente | **#116**, **#242** (§9.4, §9.5) |

> **#110** y **#238** eran el mismo error de fondo —**dos predicados distintos** para decidir el
> mismo modificador, hoy uno solo (`implicit_field_mods`)—; **#255** era una linea que faltaba en
> `modifier_flag`. Los tres ultimos (**#115**, **#236**, y `ACC_TRANSIENT`) pidieron separar los
> flags de campo de los de metodo, porque `ACC_VOLATILE`/`ACC_TRANSIENT` comparten bit con
> `ACC_BRIDGE`/`ACC_VARARGS`: no es un mapeo directo, es **una tabla por clase de miembro**.

### javac — resolucion de nombres — 0 (familia vacia)

### javac — genericos e inferencia — 0 (familia vacia)

### javac — chequeos que faltan — 0 (familia vacia)

### javac — tipos y conversiones — 0 (familia vacia)

> Ya cerrados: **#217** (faltaba la ampliacion implicita `int`→`long`/`double` en cinco
> posiciones) y **#261** (un array no era un tipo referencia: §4.3 y §4.10.3).

### javac — parser y literales — 0 (familia vacia)

### javac — atributos, CLI y otros — 0 (familia vacia)

### Biblioteca (KajiLibrary, no el compilador) — 5

| # | Que |
|---|---|
| **#201** | 🟡 falta `synchronized` — `Vector`/`Stack`/`ByteArray*`/`Throwable` hechos; queda `Hashtable` |
| **#202** | ~~falta `abstract`~~ — **RESUELTO**, 0 divergencias |
| **#203** | 🟡 falta `final` — `System.out` y otros cuatro hechos; el resto pide cotejo por **descriptor**, no por nombre |
| **#205** | 🟡 `ClassLoader`, `Collection.stream` y `Map.putAll` **hechos**; queda el resto del hueco de `Map` |
| **#246** | **hay dos bibliotecas divergentes y la que se desarrolla no es la que corre** |

### Retirados

**#206** (tipos izados a top-level) y **#207** (hooks de test en la API): las clases y los miembros
señalados son **package-private**, o sea internos, y por la regla del contrato son libres. Ver
"La regla" mas abajo.

### Herramientas propias

- **`bin/jvm.exe --javap`** — *nota corregida (2026-08-24)*. Decia que no imprime `transient` ni
  `volatile`; **se verifico y ya no es cierto**: contra el `javap` del JDK sobre las **120** clases de
  `boot/`, **coincide exacto en las 120, con cero diferencias**, incluidos los cinco campos
  `volatile` que hay ahi (`Thread.interrupted`, los tres `Atomic*.value`, `WaiterNode.thread`). Sigue
  valiendo que **oculta los campos privados sin `-p`**.
  **Y tiene una ventaja estructural sobre el `javap` del JDK para auditar lo nuestro:** solo acepta
  una **ruta de archivo**, asi que es *inmune por construccion* a la trampa de resolucion de `java.*`
  descrita en #246 — no existe el modo de invocacion que se puede usar mal. Para medir `boot/` o
  `KajiLibrary/` conviene el nuestro; el del JDK queda para el lado **de referencia** (ahi si se le
  pasa el FQN a proposito, porque el oraculo debe salir de `java.base`).
- **`tools/apidiff/apidiff.py`** compara el **texto** de `javap`, que incluye el `Signature`: un
  miembro con descriptor exacto cuenta como faltante. Subestima. Conviene un comparador por
  descriptor.

> **El rango #1–#128** es de sesiones anteriores y es casi todo `javac` (mas unos pocos de
> biblioteca); esta ordenado por numero mas arriba, con su propio estado ✅/⬜.

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

<!-- Movidos desde '## Open' (2026-08-24): estaban marcados como arreglados pero
     seguian bajo Open, donde se leen como pendientes. Texto sin cambios. -->

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

- **#101 — ✅ FIXED (verificado 2026-08-24; ver la revision con el snapshot congelado).** `Outer.Nested`
  resuelve y el descriptor sale bien (`Map.Entry<K,V> entry()` → `()Ljava/util/Map$Entry;`), y
  `Normalizer.Form` se puede nombrar desde otro archivo. Reporte original abajo.
  A qualified reference to a nested type, `Outer.Nested`, is not resolved. Only the simple
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

- **#116 — ✅ FIXED (verificado 2026-08-24; ver la revision con el snapshot congelado).**
  `java.util.stream.Stream.of` ya se emite `public static`, y eso destrabo
  `java.util.random.RandomGeneratorFactory.all()`. Reporte original abajo.
  The implicit `public` of an interface member isn't applied to `static` methods.
  In an interface every member is implicitly `public` (JLS §9.4). We apply that to *abstract*
  methods but not to *static* ones: `static ClassDesc of(String)` came out package-private
  (`flags: (0x0008) ACC_STATIC`), while `String descriptorString();` in the same interface
  correctly got `ACC_PUBLIC | ACC_ABSTRACT`. A package-private static on an interface isn't even
  expressible in Java, so the emitted class file is one no `javac` could produce.
  - Repro: `public interface I { static I of(String s) { return null; } String d(); }`
    → `javap -p` shows `static I of(java.lang.String)` where the reference shows `public static`.
  - Workaround in KajiLibrary: `java.lang.constant.ClassDesc.of` spells out `public static`.

- **#217 / #217b — ✅ la ampliacion primitiva implicita, y el verificador que la dejaba pasar.**
  El emisor elegia la variante de `Xreturn` —y el ancho del valor— por el tipo de la **expresion**
  en vez de por el **descriptor declarado**, asi que `return unInt;` en un `()J` salia `ireturn`:
  un class file **estructuralmente invalido**, no un resultado equivocado. Faltaba en las cinco
  posiciones que reportaba la tanda 4 (retorno, init de local, asignacion a local, asignacion a
  campo, paso de argumento y `array store`) y tampoco emitia `i2d`.
  Arreglado en `codegen.rs` con **una** funcion, `widen_cat`, que toma el origen de la **pila** (lo
  unico que describe con certeza que hay ahi) y el destino del contexto; el `ret_cat` del emisor
  sale del **mismo** `type_desc` que declara el metodo, para que opcode y descriptor no puedan
  discrepar. Los argumentos van por `emit_args`, que amplia cada uno al tipo de su parametro.
  **#217b** (el corolario del runtime) se cierra en `verifier.rs`: `expect_declared_return` cruza
  cada `Xreturn` contra el descriptor (JVMS §6.5), que es la unica forma de detectarlo — la pila
  queda consistente, asi que verificar solo el valor no alcanza. Test:
  `jvm::verifier::tests::return_opcode_must_match_the_declared_return_type`, mas dos de codegen.
  Repro: `repros/finding_217.java` (12 casos, corre a 0).

- **#226 — ✅ el nativo `String.valueOf([CII)` estaba implementado; el finding quedo desactualizado.**
  Verificado sobre los tres consumidores que daba por rotos: `StringBuilder.toString()`,
  `String.substring(int[,int])` y la concatenacion `"a" + n`. Los cuatro corren.
  **Lo que si era real y estaba tapado:** hay que compilar con `-cp KajiLibrary`. El
  `boot/java/lang/String.class` declara **solo** `valueOf(Object)`, asi que sin classpath la
  llamada no resuelve — y hasta #261 eso no daba error sino un `.class` mudo con la pila corrida,
  que es probablemente lo que se vio como "el nativo falta". Repro: `repros/finding_226.java`.

- **#220 y #244 — ✅ no eran de la VM: eran #110, el compilador.**
  Los dos panicaban en `objects_operations.rs:410` con `field_offset: field not found in the class
  or its superclasses`, y los dos se leyeron como fallas del interprete —#220 como "el
  comportamiento depende del constant pool de otros metodos", #244 como "un enum anidado en un
  paquete que `boot/` provee a medias no se puede inicializar"—.
  La causa unica: el lector de `.class` del classpath **descartaba el `access_flags` de los
  campos** (#110), asi que un campo `static` de una clase externa se modelaba como de instancia y
  el emisor sacaba `getfield` donde iba `getstatic`. **La VM tenia razon**: ese campo, como campo
  de instancia, no existe. El "parpadeo" de #220 (agregar un metodo *muerto* cambiaba si `t2()`
  andaba) era el otro sintoma de la misma familia: que simbolo externo ganaba la clave por nombre
  simple dependia del orden de carga.
  Medido: el mismo fuente, con el javac congelado da `getfield P220.estatico:I` y con el actual
  `getstatic`. Y los dos enums que disparaban #244 —`VarHandle$AccessMode` y
  `DirectMethodHandleDesc$Kind`— ahora inicializan y sus `values()` traen los constantes.
  Repro: `repros/finding_244.java`.
  **Leccion:** un panic de la VM no prueba que el bug sea de la VM. Los dos findings costaron una
  matriz de aislamiento cada uno sobre el componente equivocado.

- **#261 — ✅ un array no era un tipo referencia, y una llamada sin resolver se emitia como NADA.**
  Tres agujeros encadenados, encontrados tirando del hilo de #226:
  1. `types::is_subtype` no le daba supertipos a un array. §4.10.3: los directos de `T[]` son
     `Object`, `Cloneable` y `java.io.Serializable` (mas `S[]` por cada supertipo del elemento).
  2. `attribute::is_reference` no incluia `RType::Array`, asi que `assignable` y `convertible` ni
     llegaban a consultar el subtipado: caian al `_ => false`.
  3. Y cuando la resolucion fallaba sobre un tipo **externo**, la pasada 2 callaba **a proposito**
     (no modelamos toda firma del JDK) y el codegen, sin binding, hacia `return` en silencio — con
     los argumentos ya empujados. `s.length(1, 2)` compilaba a `iconst_1; iconst_2; ireturn`.
  El sintoma real: **`System.arraycopy(Object, int, Object, int, int)` no resolvia**, y once
  fuentes de la biblioteca (`StringBuilder`, `ArrayList`, los `Buffered*`, `CharArray*`,
  `Pushback*`, `StringBuffer`) se compilaban mudas y rotas. Tambien se agrego `types::member_class`
  (los miembros de un array son los de `Object`, §10.7) y el caso propio de `unArray.clone()`, cuyo
  retorno es el tipo array — sin el, el `values()` de **todo** enum dejaba de tipar.
  La indulgencia de la pasada 2 se mantiene (es correcta); lo que se quito es que pueda terminar en
  un class file roto y mudo: ahora el emisor **falla fuerte**. Es la regla que #114 ya habia
  escrito ("resolve must be a hard error, not a dropped statement") y que no estaba aplicada.
  Medicion sobre las 978 fuentes de KajiLibrary: **5 fallas antes → 7 despues**, pero las 11
  silenciosas dejaron de serlo. Repro: `repros/finding_261.java`.

- **#258 — ✅ era el mismo bug que #261, encontrado en paralelo.** "Un array no se acepta donde el
  parametro esta declarado `Object`, y `System.arraycopy` es exactamente esa forma": las dos caras
  que describe —error si el metodo esta en el archivo, **descarte mudo** si viene del classpath—
  son las capas 2 y 3 de #261. Verificado con su propio repro, los seis metodos dan lo que el
  encabezado promete: `copiaNativa` 9 (era 0), `objetoComoObject` 7, `propioMismaAridad` 9,
  `nativoConRetorno` 1, `nativoVoidSinArgs` 1, `ajenoNoNativo` 5.
  Las 31 llamadas a `System.arraycopy` en 12 archivos de la biblioteca ya no son adornos.

- **#252 — ✅ una llamada cuyo receptor tiene tipo de VARIABLE DE TIPO ya no se descarta.**
  Lo arreglo `types::member_class` (parte de #261): los miembros de un receptor sin simbolo propio
  se buscan donde corresponde. Hoy `if (!a.equals(b))` con `a`/`b` de tipo `T` emite lo mismo que
  javac — `aload_1; aload_2; invokevirtual java/lang/Object.equals; ifne` — en vez del
  `aload_2; ifne` que aplicaba un salto de int a una referencia.
  Verificado en runtime con un driver sobre `finding_252<String>`: los cuatro casos dan 0.
  **Ojo con el repro:** sus metodos son de **instancia**, y `run-headless` los invoca sin receptor
  valido, asi que corrido directo panica por el harness y no por el bug. Hace falta un driver.

- **#254 — ✅ llamar a un metodo que la clase sobreescribe de una interfaz parametrizada ya no es
  "ambiguo".** `pub.subscribe(sub)` sobre un `SubmissionPublisher<String>` compila y emite
  `invokevirtual java/util/concurrent/SubmissionPublisher.subscribe:(Ljava/util/concurrent/Flow$Subscriber;)V`.
  Lo cerro la deduplicacion de candidatos por firma **borrada** (#122, §8.4.2 y §15.12.2.5): las
  dos declaraciones —la de `Flow.Publisher` y el override de la clase— eran el mismo metodo y
  dejaron de competir entre si. Igual que #252, su repro necesita argumentos referencia: corrido
  con `run-headless` a secas panica por el harness.

- **#238 y #124 — ✅ los dos venian de mirar los modificadores DECLARADOS de un campo de interfaz.**
  §9.3 los da por implicitos, asi que en una interfaz vienen vacios. Con eso, el codegen emitia el
  campo con `flags: (0x0000)` y sin `ConstantValue` (**#238**, class file que el `javap` real marca
  invalido) y el desugar lo tomaba por campo **de instancia**, bajando su inicializador a un
  constructor sintetizado **sobre la interfaz** (**#124**: un `<init>()V` ilegal que nadie llama,
  asi que el campo quedaba sin asignar).
  Los dos usan ahora la **misma** funcion que ya tenia `enter.rs`, `implicit_field_mods` — que es
  la leccion de #110/#112: si dos predicados divergen, el campo se emite con unos flags y se
  resuelve con otros. Mas una guarda explicita para que no se sintetice un constructor en una
  interfaz. Verificado con `javap -v` y corriendo: `NOPOS/DONE/NOMBRE/CALC` salen
  `(0x0019) ACC_PUBLIC, ACC_STATIC, ACC_FINAL`, sin `<init>`, y `CALC` (no constante) se asigna en
  el `<clinit>`. Repro: `repros/finding_238.java`.

- **#255 — ✅ `ACC_SYNCHRONIZED` no se emitia.** Una linea: `modifier_flag` no mapeaba
  `Modifier::Synchronized`. El flag no es decorativo — es lo **unico** que hace que la JVM tome el
  monitor del receptor al entrar y lo suelte en cualquier salida (§2.11.10), asi que sin el un
  `wait()`/`notifyAll()` en el cuerpo corria sin monitor y tiraba `IllegalMonitorStateException`:
  **todo diseño de espera/aviso quedaba inejecutable**, y sin rodeo posible porque el modificador es
  API. Hoy sale `flags: (0x0021) ACC_PUBLIC, ACC_SYNCHRONIZED`, identico al javac del JDK 25, y
  `finding_255.llamaAvisa()` devuelve 1 (devolvia `None`).
  Comparte el bit `0x0020` con `ACC_SUPER` de las clases y con `ACC_OPEN`/`ACC_TRANSITIVE` de los
  modulos, pero son tablas de flags **distintas** (§4.1 vs §4.6) y esta funcion solo ve miembros.
  Test: `javac::codegen::tests::a_synchronized_method_emits_acc_synchronized`.

- **#257 — ✅ un `catch` que se dispara ya corre su `finally` y saltea el handler.** La causa no era
  "falta duplicar el cuerpo del `finally`": el emisor **ya** lo duplicaba. Era que la copia en linea
  estaba detras de un `if self.reachable`, y el cuerpo del `try` del repro termina en `throw`, que
  deja `reachable` en `false`. Nadie lo reseteaba al entrar al handler — y un handler **siempre** es
  alcanzable: se llega por excepcion, no por caida. Con `reachable` en falso no se emitia ni la copia
  en linea ni el `goto`, asi que el `catch` caia derecho dentro del catch-all, que hace `astore` de
  un throwable que nadie apilo.
  El arreglo es una linea, y el bytecode resultante calza **offset por offset** con el del javac del
  JDK 25 que el propio finding transcribe (`23` finally en linea, `32: goto 47`, `35: astore_1`,
  `36` finally, `45: aload_1`, `46: athrow`, `47` sigue). La tabla de excepciones difiere en la
  **forma**, no en el efecto: javac usa un rango `4 23 35 any` que cubre `try` y `catch` juntos, y
  nosotros dos rangos disjuntos (`4 14 35` y `14 23 35`).
  Los seis casos del repro dan lo que promete su encabezado, `atrapaYLimpia` incluido (11, era panic).
  Test: `javac::codegen::tests::a_catch_that_fires_still_runs_its_finally_and_jumps_past_the_handler`.

- **#259 — ✅ el pool se escribia en UTF-8 estandar y no en UTF-8 MODIFICADO (§4.4.7).**
  `Const::Utf8` serializaba `s.as_bytes()` y `s.len()`. Son dos codificaciones distintas en dos
  puntos: `U+0000` va en **dos** bytes (`C0 80`), y un code point **suplementario** va como su **par
  sustituto** de UTF-16, cada sustituto en tres bytes —seis en total— en vez de los cuatro del
  estandar. El largo tambien salia mal, porque es el de los bytes modificados.
  La implementacion pasa por `encode_utf16`, y eso resuelve los dos casos de una: un suplementario
  ya sale como dos sustitutos y cada uno cae en la rama de tres bytes.
  **Comprobado por diferencial contra el `javac` del JDK 25**, no por lectura: para `U+1D160` los
  dos emiten exactamente `ed a0 b4 ed b5 a0`. Y el `.class` ahora lo leen los dos `javap` y corre:
  `suplementario()` devuelve **2** (dos unidades UTF-16, que es lo que dice Java) y el control
  `soloBMP()` 16. Test: `javac::class_writer::tests::the_constant_pool_uses_modified_utf8_not_plain_utf8`.

### Barrida del nivel 4 (2026-08-24): 12 de 28 ya estaban cerrados

Antes de arreglar nada se **verificó uno por uno** contra el build del dia, con repro propio donde no
lo habia. El resultado importa mas que los arreglos: **doce de los veintiocho ya no reproducian**,
arrastrados por los cierres de nivel 2 y 3 (la resolucion de nombres calificados, el indice por FQN,
`member_class`, la dedup por firma borrada). La lista de prioridades llevaba meses de atraso.

| # | Que decia | Que da hoy |
|---|---|---|
| **#106** | tipo del JDK **calificado** en una firma no resuelve; retorno generico con comodin se borra a `Object` | `()Ljava/util/List;`, `()Ljava/lang/reflect/Member;`, `()Ljava/util/Set;` |
| **#117** | nombre calificado en posicion de tipo no resuelve | `java.lang.constant.ClassDesc d;` compila con el descriptor exacto |
| **#210** | calificado dentro de `java.lang` degrada a `Object` **en silencio** | `(Ljava/lang/String;)Ljava/lang/String;` |
| **#211** | no se puede sobreescribir un retorno `T[]` de un tipo fuera de `java.lang` | compila; `ClassDesc[] dame()` en la hija |
| **#212** | el bound va al `Signature` con el **nombre simple** | `<T::Ljava/util/Map;>()TT;` — calificado **y** con el `::` de interfaz |
| **#213** | no verifica que una clase concreta implemente los abstractos de su **superclase** | es error, con el mensaje correcto |
| **#214** | `-cp` de varias entradas degrada los tipos a `Object` | una y dos entradas dan el **mismo** `()Ljava/util/List;` |
| **#222** | con un array elige `f(T)` en vez de `f(T[])` | elige `f(T[])` (devuelve 2) |
| **#223** | `? super T` no sobrevive la captura | pasa un `List<? super T>` a un parametro del mismo tipo |
| **#243** | concat de un operando de tipo anidado → "`append` es ambigua" | emite `invokedynamic makeConcatWithConstants`, como javac |
| **#249** | tipo anidado de una interfaz hermana, con `package` | `final class pk.PABuilder implements pk.PA$Builder` |
| **#123** | se pierde la relacion de supertipo cuando la clausula lleva argumentos de tipo | `class Impl implements Sup<String>` compila y despacha |

> **Aviso de metodo.** Varios de estos "compilan" pero **panican al ejecutarse**, y no por lo que
> decia el finding: `#243` y `#123` mueren en `no native implementation for
> java/lang/String.rawLength`, que es el `run-headless` **congelado** quedandose atras de las
> costuras privadas nuevas de `String` (ver `bin/FROZEN.md`), no el defecto reportado.
> Al verificar hay que separar **el sintoma del finding** del ruido de alrededor: si el finding
> decia "error de compilacion" y hoy compila con el bytecode correcto, esta cerrado, aunque el
> runtime falle por otra cosa. Y la regla practica que se desprende: **comparar siempre contra
> `target/release`, no contra `bin/`**, cuando lo que se mide es un arreglo de hoy.

- **#100 / #241 — ✅ la borradura de una variable de tipo del METODO era `Object`, no su cota.**
  El sintoma se conocia desde #100 y se re-reporto como #241; la causa estaba a medio arreglar y
  nadie lo habia visto, porque **para los parametros de tipo de la CLASE ya funcionaba**:

  ```
  class C<T extends Number> { void f(T t) }        ->  (Ljava/lang/Number;)V   bien, desde antes
  <N extends Number> N met(Class<N> c)             ->  (Ljava/lang/Object;)V   MAL
  ```

  `method_descriptor` resolvia los tipos de la firma en el scope de la **clase**, y los parametros de
  tipo del **metodo** no viven ahi: `resolve_type_id` fallaba y se caia al `Ljava/lang/Object;` por
  defecto. El `Signature`, en cambio, sale por otro camino —que si recibe la lista de `type_params`—
  y salia **bien**. Esa combinacion es la peor posible: el `javap` muestra la firma generica correcta
  y el desajuste solo se manifiesta al **sobreescribir**, con `AbstractMethodError` en runtime.
  Arreglado con `type_desc_m`, que consulta primero los `type_params` del metodo y toma el descriptor
  de su **primera cota** (§4.6), recursivo para cotas que nombran a un hermano (`<A, B extends A>`) y
  acotado en profundidad para que un ciclo declarado no cuelgue el compilador.
  **Comprobado por diferencial contra el javac del JDK 25**: para `<N extends Number> N f(Class<N>)`
  los dos emiten `(Ljava/lang/Class;)Ljava/lang/Number;`.
  Test: `javac::codegen::tests::a_method_type_parameter_erases_to_its_bound_not_to_object`.

- **#115 / #236 — ✅ `ACC_VOLATILE` y `ACC_TRANSIENT` ya se emiten.** La causa era una **colision de
  bits**, no un olvido: los dos comparten valor con flags de **metodo** —`ACC_VOLATILE` (0x0040) con
  `ACC_BRIDGE`, `ACC_TRANSIENT` (0x0080) con `ACC_VARARGS`— asi que no pueden salir de la misma tabla
  que el resto. Son tablas **distintas** (§4.5 para campos, §4.6 para metodos), no un espacio comun.
  Se separo `field_flags` de `class_flags` y listo. Valores cotejados contra el javac del JDK 25:
  `volatile` → `0x0041`, `transient` → `0x0081`, los dos juntos → `0x00c1`.
  **`strictfp` no emite nada, a proposito:** desde la v17 es implicito y el javac real tampoco emite
  `ACC_STRICT` — avisa que el modificador sobra. Test:
  `javac::codegen::tests::a_field_gets_acc_volatile_and_acc_transient`.

- **#200 / #118 — ✅ `ACC_VARARGS` se emite.** `public static String f(String s, Object... a)` sale
  `(0x0089) ACC_PUBLIC, ACC_STATIC, ACC_VARARGS`, y el `javap` del JDK ya imprime `Object...`.
  El `bin/` congelado de `7be628d` **no lo trae**: es anterior al arreglo.

- **#242 — ✅ un tipo miembro de una interfaz es implicitamente `public` (§9.5).** Igual que sus
  campos son `public static final` (§9.3) y sus metodos `public` (§9.4) — la misma familia que ya
  habian cerrado #116 y #238. Sin esto, un anidado de interfaz salia package-private e **inusable
  desde otro paquete**, que es lo que obligo a escribir el `public` a mano en los ocho anidados de
  `javax.lang.model.element.ModuleElement`.
  Va en dos lugares y los dos importan: los `access_flags` de la propia clase y su entrada de
  `InnerClasses`, donde ademas corresponde el `static` implicito. Cotejado con el javac del JDK 25
  para los tres casos: `interface In` → `0x0601`, `class C` → `0x0021`, `enum E` → `0x4031`.
  Test: `javac::codegen::tests::a_member_type_of_an_interface_is_implicitly_public`.

- **#231 / #125 — ✅ la mitad del COMPILADOR: `super.metodo()` ya emite bytecode, y el correcto.**
  Eran dos cosas: `super` **como receptor** caia en `unsupported` (es el mismo `this`; lo que cambia
  no es el objeto sino el despacho), y el despacho en si — `super.m()` **no es virtual** (§15.12.4.4):
  va por `invokespecial`, y el dueño del methodref es la **superclase directa**, no la clase que
  declara el metodo. Comprobado contra el javac del JDK 25: con `C extends B extends A` y `f`
  declarado en `A`, los dos emiten `invokespecial B.f`, no `A.f`.
  Test: `javac::codegen::tests::a_super_call_uses_invokespecial_on_the_direct_superclass`.
  **Quedaba una mitad, y era de la VM: #265, cerrada el 2026-08-25.**

- **#233 — ✅ los overrides covariantes ABSTRACTOS ya reciben su puente.** El emisor salteaba todo
  metodo `abstract` al armar los puentes, y ese salto no tiene fundamento: **el puente lo necesita el
  llamador que ve el supertipo, no la implementacion**. javac lo emite igual, y **concreto** —
  `aload_0; invokevirtual <el angosto>; areturn` —, y el despacho virtual lo lleva al override real
  de la subclase concreta. Bastaba con no saltearlos; el emisor de puentes ya hacia lo correcto.
  Cotejado con el javac del JDK 25 en dos formas: la clase abstracta intermedia (cuerpo del puente
  identico, `flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC`) y la interfaz generica
  (`interface I extends Comparator<String>` emite `public default int compare(Object, Object)` en
  los dos). Costo que tenia: ~25 miembros de `java.nio` (`Buffer slice()`, `duplicate()`, …).
  Repro: `repros/finding_233.java` (corre a 0). Test:
  `javac::codegen::tests::an_abstract_covariant_override_still_gets_its_bridge`.

- **#234 — ✅ una invocacion con varios archivos resuelve cruzado.** Cada archivo se compilaba como
  una unidad **independiente**, con su propia tabla de simbolos.
  El arreglo no es "compartir la tabla": es que el **orden de las fases sea global**. Enter de
  *todas* las clases de *todos* los archivos primero —asi una referencia adelantada tiene a quien
  resolver, sin importar el orden en la linea de comandos ni si dos clases se referencian
  mutuamente—, despues MemberEnter de todas, y recien entonces la resolucion. Los `import` siguen
  siendo **por unidad** (§7.5), que es lo que corresponde: cada `resolve_type_decl` corre con los
  suyos.
  `enter::enter_cp_multi` + `javac::compile_units_cp`, que devuelve las clases **agrupadas por
  unidad** para que cada `.class` se escriba al lado de *su* fuente. Un error en cualquiera de los
  archivos aborta la emision de **todos**: es una unidad de trabajo, y emitir la mitad dejaria un
  directorio a medias que parece completo.
  - **Limitacion anotada, no arreglada:** los `import static` van a un unico mapa global de la
    tabla, asi que con varios archivos uno podria ver el estatico importado por otro. Es el unico
    punto donde la separacion por unidad se pierde, y quedo escrito en el codigo.
  - Repro: `repros/finding_234.java` + `finding_234b.java` — recursion **mutua** entre archivos,
    que es el caso que antes pedia el bootstrap de tres pasos. Corre a 0 **en los dos ordenes**.

- **#204 / #215 — ✅ un parametro de tipo DEL METODO no resolvia dentro de su propio cuerpo.**
  Los dos se habian reportado como fallas de **inferencia** —"no se pueden inferir los argumentos de
  tipo de `id`: restricciones de tipo incompatibles"— y no tenian nada que ver con inferir. Lo dijo
  la instrumentacion, no la lectura: el bound que hacia insatisfacible el sistema era
  `lower=[(0, "?")]`, o sea el argumento llegaba **`Unresolved`**.

  La causa: los parametros de tipo de un metodo generico viven en un scope **propio**, colgado del
  de la clase (§8.4.4), y la pasada 2 resolvia la firma **y el cuerpo** en el de la **clase**. La
  `A` de `<A> A f(A x)` quedaba sin resolver, el parametro `x` con ella, y de ahi se caia todo lo
  que dependiera de su tipo. **La pasada 1 ya lo hacia bien** (`enter::owner_scope`): las dos
  discrepaban, y por eso el `Signature` salia correcto mientras el cuerpo no compilaba.

  Es mas ancho que la inferencia — `<A> int f(A x) { return x.hashCode(); }` tampoco compilaba,
  porque el receptor no tenia tipo. Se arreglo con `attribute::method_scope`, que espeja lo que ya
  hacia la pasada 1. Con sobrecargas homonimas el simbolo se identifica por nombre + aridad + la
  **grafia** de cada tipo de parametro: dos sobrecargas genericas tienen `A`s que son simbolos
  distintos, y agarrar la ajena daria una variable de otro metodo.

  **Efecto medido:** `java/util/Collections.java` y `java/util/Optional.java` vuelven a compilar —
  la recompilacion de la biblioteca pasa de **7 fuentes sin `.class` a 5**.
  Repro: `repros/finding_204.java` (corre a 0). Test:
  `javac::attribute::tests::a_methods_own_type_parameter_resolves_inside_its_body`.

### Cierre del nivel 4 (2026-08-24): los siete que quedaban

- **#239 / #245 - OK: un tipo ANIDADO de otra unidad ya es nombrable en las cuatro formas.** Eran
  **dos** cosas encadenadas, y por eso el sintoma parecia inconsistente:
  1. traducir el nombre punteado cambiando cada punto por una barra **nunca** encuentra un anidado:
     `p.Outer.Kind` se buscaba como `p/Outer/Kind` y el archivo se llama `p/Outer$Kind`. El fuente no
     distingue un paquete de un tipo envolvente, asi que hay que probar las dos particiones
     (`internal_candidates`, de la mas externa a la mas anidada);
  2. aun cargandolo, su clave en el espacio de externos -que es **plano**- es `Outer$Kind`, y el
     fuente escribe `Kind`.

  Lo segundo se arreglo con un **alias**, y solo para el nombre que la unidad escribio. Registrar
  cada nombre interior de oficio parece mas simple y **es peor**: se probo, y
  `java.lang.Thread$State` reclamo la clave `State` y tapo el `State` propio de
  `java.util.concurrent.StructuredTaskScope`, que dejo de compilar. La leccion vale mas que el
  arreglo: en un espacio de nombres plano, **solo entra lo que alguien pidio por ese nombre**.
  La forma peor -`import p.Outer.Marker;` + `implements Marker`, que **borraba la clausula del class
  file**- es la que se cierra aca. Repro: `repros/finding_239.java` + `finding_239_ext.java`.

- **#263 - OK: un receptor de tipo variable con cota externa resuelve sus miembros.** El sintoma
  parecia caprichoso -con la cota **calificada** andaba, con la misma cota por `import` + nombre
  simple no- y la causa no estaba en la resolucion sino **antes**: el recolector de nombres a cargar
  del classpath no miraba las **cotas** de los parametros de tipo, ni de clase ni de metodo. Un tipo
  que solo aparece ahi nunca se cargaba, la cota quedaba sin resolver y el receptor se quedaba sin
  miembros. Con el nombre calificado la resolucion lo traia por otro camino - de ahi la asimetria.
  Desbloqueo `javax/tools/ForwardingJavaFileManager.java`. Repro: `repros/finding_263.java`.

- **#253 / #264 - OK: un argumento con COMODIN ya se puede pasar a un parametro de ese mismo tipo.**
  Una linea, y es de **precedencia**: en `is_subtype`, el arm de variable de tipo venia **antes** que
  el de captura. Con `T <: CAP` -donde `CAP` es la captura de `? super T`, o sea con cota inferior
  `T`- entraba por el de variable de tipo y preguntaba `Object <: CAP`, que es falso. Poner el arm
  de captura primero lo resuelve por su cota inferior, que es la regla (JLS 4.10.2).
  No es un caso raro: es lo que hace **cualquier reenvio** de un parametro con comodin. **#264** -el
  `? super` anidado de `BiConsumer<? super Flow.Subscriber<? super T>, ? super Throwable>`- era el
  mismo bug con una capa mas, y cayo con el: `java/util/concurrent/SubmissionPublisher.java` vuelve
  a compilar. Repro: `repros/finding_253.java`.

- **#221 - OK: un retorno `A[]` ya llama al generador.** Tambien de aplicabilidad, no de emision: el
  argumento de tipo del parametro es `A[]`, o sea un array de variable de tipo y no una variable
  pelada. La indulgencia que deja pasar un `Box<A>` contra un `Box<String>` -porque `A` la fija la
  inferencia- no atravesaba el array, asi que se caia a la invariancia y comparaba `String[]` contra
  `A[]`. Ahora la atraviesa; **no** atraviesa los parametrizados a proposito, porque ahi la
  indulgencia empezaria a hacer aplicables sobrecargas que no lo son. Repro:
  `repros/finding_221.java` (devuelve un array de largo 3; antes daba 0 sin excepcion).

- **#260 - OK: el condicional ya no colapsa `char`/`byte`/`short` a `int`.** Se usaba la **promocion
  binaria** (JLS 5.6.2), cuyo piso es `int`. Para `a + b` es correcto; para el condicional no:
  JLS 15.25 tiene reglas propias -mismo tipo en las dos ramas da ese tipo; `byte` con `short` da
  `short`; una rama angosta con una constante `int` que entra da el tipo angosto- y solo lo que no
  cae en ninguna va a la promocion. Que el `if`/`else` equivalente anduviera era la pista de que el
  roto no era el condicional sino su **tipo**. Repro: `repros/finding_260.java` (los cinco
  descriptores correctos; el JDK 25 imprime `aAAa1`).

- **#104 / #256 - OK: el atributo `Exceptions` ya se LEE.** Se escribia al emitir y no se leia al
  cargar, asi que un metodo del classpath parecia no declarar ninguna excepcion comprobada:
  implementar `Callable.call() throws Exception` se rechazaba con "mas ancho que lo que permite
  `Callable`" (JLS 8.4.8.3), con el atributo ahi, identico al que imprime el `javap` del JDK.
  `ExtMethod` no tenia `throws` y el lector de `.class` salteaba el atributo entero.
  - **Y tiene reciproca, que aparecio al arreglarlo:** ahora una llamada a un metodo del classpath
    que declara `throws` **exige** manejarla. Eso destapo un bug real de biblioteca en
    `ForkJoinTask.exec()`, que llama a `body.call()` sin capturar ni declarar - codigo que el javac
    real tampoco acepta (el JDK envuelve esa llamada en un `try`/`catch` que relanza).

#### Consecuencia: se levanta el bloqueo de `SimpleJavaFileObject`

`javax/tools/JavaFileObject.java` documenta por que `SimpleJavaFileObject` se omitio, y una de las
tres razones era que **`Kind` es un tipo anidado de otra unidad que el javac no podia nombrar** -o
sea #239-. Ya se puede: `JavaFileObject.Kind` emite `Ljavax/tools/JavaFileObject$Kind;` tanto en un
campo como en un retorno. Queda del lado de la biblioteca decidir si se escribe (sigue faltando
`java.net.URI`), y de paso `KajiSourceFile` puede declarar su `getKind`, que es lo que hoy la
mantiene sin compilar.

### #208 cerrado (2026-08-25): el generador ya no inventa un tipo que no resolvio

**El sintoma**: un tipo que no resuelve salia igual al `.class`, y con **dos mentiras distintas**
para el mismo parametro:

```
void f(NoExiste, java.lang.Class<?>)
  descriptor: (Ljava/lang/Object;Ljava/lang/Class;)V     <- degradado a Object
  Signature:  (LNoExiste;Ljava/lang/Class<*>;)V          <- nombre crudo, no es ninguna clase
```

No coinciden porque los calculan **dos caminos separados**: el descriptor degrada a `Object` cuando
el nombre no resuelve, y la firma escribe el nombre tal como se lo escribio con los puntos vueltos
barras. Es el patron 2 de la lista de arriba, en su forma mas pura.

**El disparador que decia el reporte** -"otro parametro de la misma firma lleva argumentos de
tipo"- era el del **sintoma, no el de la causa**: el `Signature` solo se emite si algo de la firma
usa genericos, asi que sin el `Class<?>` el nombre roto no se llegaba a **ver**. La causa no depende
de eso, y el descriptor ya estaba mal en las tres filas de la matriz.

**La forma que reportaba el finding ya no reproduce.** `import p.Outer.Inner;` + `Inner` emite hoy
`Lp/Outer$Inner;` en el descriptor **y** en el `Signature`, igual que `import p.Outer.*;` y que el
nombre calificado: cayo con #239/#245. Lo que quedaba abierto era el **mecanismo**, que sigue vivo
para cualquier otro nombre que no resuelva.

**Por que un nombre sin resolver llega hasta el generador.** Un `import` de tipo unico se da por
bueno en la fase semantica (`resolve_class_name`: "importado explicitamente, lo damos por
existente"), la misma indulgencia que con `import *` — sin classpath no hay forma de descartarlo.
Esa indulgencia esta bien y **se deja**: se probo hacerla estricta y falla en cualquier arnes sin
classpath (`java.util.Iterator` no se puede cargar, y hay tests que dependen de eso). Lo que estaba
mal era la otra mitad: que el generador, al no resolverlo, **inventara** un artefacto plausible.

**El arreglo**: `audit_declared_types` recorre, antes de emitir, todo nombre de tipo escrito en las
declaraciones de la clase -cotas de parametros de tipo, `extends`, `implements`, `permits`,
componentes de record, y de cada miembro su tipo, retorno, parametros y `throws`, bajando por
arrays y argumentos de tipo- y reporta el que no resuelve. `generate` ya no devolvia `.class` a
medias ante un error, asi que alcanza con anotarlo.

Los tres fallbacks silenciosos siguen en el codigo -`type_desc` degradando a `Object`, `sig_type`
escribiendo el nombre crudo-, pero ahora son **inalcanzables** para un tipo declarado: la auditoria
corre antes. Se dejan porque el mismo `Object` es la **erasure correcta** de una variable de tipo,
que es el otro caso que pasa por ahi.

#### Lo que destapo

- **Un bug real de sustitucion en el desazucarado.** El SAM de una lambda puede estar declarado en
  una **superinterfaz**: el de `BinaryOperator<T>` es el `apply` de `BiFunction<T,T,T>`, y sus tipos
  hablan del `T`/`U`/`R` de *BiFunction*. Se sustituia con `subst_of(iface)` -que solo mapea los
  parametros de la interfaz **instanciada**-, asi que esas variables sobrevivian sin sustituir y el
  metodo sintetico quedaba declarando un retorno `R` que no es ningun tipo. **La erasure lo tapaba**
  borrandolo a `Object`, que para una variable sin cota es lo mismo; se ve recien cuando la variable
  tiene cota. Arreglado con `subst_for(iface, dueño_del_SAM)`, que compone la sustitucion por la
  cadena de herencia. Toca las lambdas **y** las referencias a metodo.
- **Cuatro `import` de KajiLibrary a tipos que no existen** (#267). `java.util.Calendar`,
  `java.security.ProtectionDomain`, `javax.sql.DataSource` no estan en la biblioteca, y
  `Attribute$PersistentAttributeType` -un enum anidado- nunca se emitio como `.class` aparte. Las
  cuatro firmas se venian emitiendo con `Object` donde va el tipo, o sea **metodos distintos** de
  los que la API declara: cualquiera compilado contra la API real se lleva un `NoSuchMethodError`.
  Son de biblioteca, no del compilador; quedan anotadas ahi.

**Medicion**: recompilar KajiLibrary da **970/978** con .class (antes 974, y las 4 nuevas son estas
cuatro firmas que **ya estaban mal** y ahora se ven). Suite en aislamiento: **1260 / 16**, las mismas
16 de la baseline. Repro: `repros/finding_208.java`.

### Cierre del lote de parser, literales y atributos (2026-08-25)

Con esto **todas** las familias de javac del indice quedan vacias. Lo que sigue abierto es de la VM
y de la biblioteca.

**Dos de los siete ya no reproducian**, y por causas que valen anotarse porque no son las que uno
esperaria:

- **#232 - `-9223372036854775808L` ya compila.** Emite el `ldc2_w` correcto. Lo que hoy falla en un
  repro que lo imprima **no es el literal**: es que `PrintStream.println(long)` no esta en la
  biblioteca. El sintoma tapaba al otro.
- **#114 - la concatenacion ya no depende de las sobrecargas de `append`.** El emisor pasa por
  `invokedynamic makeConcatWithConstants` (la estrategia del JDK 9+), asi que la familia entera del
  finding -"falta `append(long)`/`append(double)`/`append(Object)`"- desaparecio con el cambio de
  estrategia, no con un arreglo del lookup. La guarda de #261 cubre lo que quedaba.
- **#219 - encadenar sobre una llamada a un tipo del classpath ya emite el bytecode correcto.** Los
  tres casos que listaba (`mapper.apply(x).toArray()`, `s.mapToObj(f).count()`,
  `s.findAny().getAsInt()`) salen bien; lo arreglo #251 (cargar los tipos que aparecen en las firmas
  de los externos). **Pero verificarlo destapo #268**, abajo: el bytecode encadenaba, y le faltaba
  el `checkcast`.

#### Los cuatro que si estaban

- **#209 - OK: `int.class` / `void.class` / `int[].class`.** El nombre del tipo es una **keyword**,
  asi que no llegaba por el camino del identificador y `primary()` se quedaba sin caso. Es la unica
  forma en que un primitivo aparece donde va una expresion. El emitido es el del javac real
  (JLS 15.8.2), y no es un `ldc`: **no hay entrada `CONSTANT_Class` para `int`**, porque `int` no es
  una clase. Es el campo `TYPE` de su envoltorio.

  | Escrito | Emitido |
  |---|---|
  | `int.class` | `getstatic java/lang/Integer.TYPE:Ljava/lang/Class;` |
  | `void.class` | `getstatic java/lang/Void.TYPE:Ljava/lang/Class;` |
  | `int[].class` | `ldc class "[I"` - un array **si** es un `ldc`, pero de su **descriptor** |
  | `Integer.class` | `ldc class java/lang/Integer` |

  Deja de estar bloqueado `MethodType.unwrap()`. Quedaba del lado de la biblioteca `Void.TYPE`
  -anotado como #270-, y **se cerro el mismo dia**: el comentario que lo daba por diferido "hasta
  que la VM soporte la clase primitiva void" habia sobrevivido a su razon, porque la soportaba
  desde que existe `getPrimitiveClass`. Repro: `repros/finding_209.java`.

- **#228 - OK: el escape de sustituto.** La causa no estaba en el lexer sino en la
  **representacion**: el literal se decodificaba a un `char` de **Rust**, que es un *scalar value*
  de Unicode y por definicion **excluye** los sustitutos. Un `char` de **Java** es otra cosa: una
  unidad de codigo UTF-16 (JLS 3.1). Dos tipos con el mismo nombre y distinto dominio, y el literal
  caia justo en la diferencia. `CharLit` pasa a ser `u16`, que es lo que un `char` de Java es.
  - El **par** sustituto en un `String` tenia el mismo origen y otra cara: cada escape se
    decodificaba por separado, asi que el alto fallaba solo. Ahora la decodificacion pasa por UTF-16
    y `from_utf16` los junta en el caracter suplementario que son.
  - **Queda un caso sin soporte, y falla fuerte en vez de en silencio:** un sustituto **suelto**
    dentro de un `String`. Un `String` de Rust tampoco lo sostiene, y sostenerlo pide llevar todos
    los literales como `Vec<u16>` -que toca el plegado de concatenacion, el `switch` sobre String,
    el pool y el lint-. El mensaje lo dice explicitamente en vez de sustituir por U+FFFD.
  - Repro: `repros/finding_228.java`.

- **#235 - OK: el `SourceFile` es el de la unidad.** El atributo (JVMS 4.7.10) es de la **unidad de
  compilacion**, no de la clase: las secundarias y las anidadas comparten archivo con la principal.
  Cada una escribia **su propio** nombre, o sea archivos que no existen (`Kind.java`,
  `Secundaria.java`) - y es justo para abrir la fuente que existe el atributo.
  - El compilador no recibe la **ruta**, asi que el nombre se deduce del tipo que le da nombre a la
    unidad: el **publico** de nivel superior si lo hay -que segun JLS 7.6 obliga a que el archivo se
    llame como el- y si no el primero declarado. Es exacto para toda unidad que respete esa regla.
  - Repro: `repros/finding_235.java`.

- **#224 - OK: `import IntStream;` es error.** JLS 7.5: de un paquete sin nombre no se importa. Se
  aceptaba en silencio y el tipo resolvia igual por otro camino, que es lo que lo hacia invisible.
  El `javac` real lo reporta como error de **sintaxis** (`'.' expected`), y aca tambien: un `import`
  de tipo unico necesita un nombre cualificado. El on-demand de un paquete de un solo nombre
  (`import p.*;`) sigue siendo legal y sigue andando.

#### #268 - el cast sintetico que faltaba, y por que el gate propio no podia verlo

Verificar que #219 ya no reproducia dejo a la vista otra cosa en el mismo bytecode. `List<String>.get`
esta declarado `E`, asi que su **descriptor dice `Object`** (JLS 4.6): lo que queda en la pila es un
`Object`. Encadenar ahi emitia

```
invokeinterface java/util/List.get:(I)Ljava/lang/Object;
invokevirtual   java/lang/String.length:()I        <- sobre un Object
```

y eso **no verifica**: JVMS 4.10.1.9 pide que el `objectref` sea asignable al tipo del metodo. La JVM
real lo rechaza con `VerifyError` **antes de ejecutar una sola instruccion**. Lo mismo con
`return l.get(0);` en un metodo que devuelve `String`: el `areturn` pide `String` y hay un `Object`.

**Por que estuvo tanto tiempo invisible:** nuestro interprete despacha por el objeto **real**, no
por el tipo estatico, asi que corria igual. Es la familia "compila, corre aca, revienta en la JVM de
verdad" - la peor, porque el gate propio la deja pasar por construccion. Se encontro comparando
contra el `javap` de un `javac` real, que es lo unico que la ve.

El arreglo es un `checkcast` tras la llamada cuando el tipo del **sitio** es estrictamente mas
angosto que el retorno **borrado**. **Diferencia deliberada con el javac real:** el javac lo inserta
solo donde el contexto pide el tipo angosto y lo omite cuando el destino es mas ancho
(`Object o = l.get(0);`) o cuando el valor se descarta. Aca se inserta siempre: es correcto -el cast
no puede fallar en un programa bien tipado- y cuesta un `checkcast` de mas en esas dos formas.
Saber el contexto pide un pase aparte, y el `checkcast` de mas no rompe nada.
Repro: `repros/finding_268.java`.

**Medicion del lote**: biblioteca **970/978** (sin cambios). Suite en aislamiento: **1267 / 16**,
las mismas 16 de la baseline, con 7 tests nuevos.

### Cierre del lote de VM (2026-08-25): la ultima familia del indice

Con esto **todas** las familias del indice quedan vacias salvo la de biblioteca.

**Cuatro de los seis ya no reproducian.** Vale anotar por que, porque en dos de ellos el arreglo
esta escrito en el codigo con el numero del finding al lado y en los otros dos no:

- **#216 - `ConstantValue` se aplica.** `class_operations::prepare` lo hace, con el finding citado.
  `K = 7` leido por `getstatic` devuelve **7**. Y el `.class` no tiene `<clinit>`: el 7 sale del
  atributo, que es lo que habia que probar.
- **#227 - una excepcion no capturada ya no panica.** `report_uncaught` lee `message` y `backtrace`
  con `try_field_offset`, y degrada al nombre de la clase si faltan. El comentario lo dice:
  *"both are read by a name the VM chose, so both are optional"*.
- **#225 - `invokeinterface` con receptor `String` anda.** `CharSequence cs = "abc"; cs.length()`
  devuelve **3**, sin panico.
- **#229 - las constantes `String` no-ASCII se leen bien.** `"\u0301".length()` devuelve **1**.

#### Los dos que si estaban

- **#265 - OK: la resolucion sube por la jerarquia.** JVMS 5.4.3.3 manda buscar en la clase,
  **despues en sus superclases** y despues en sus superinterfaces. `resolve_method` hacia un `find`
  sobre la clase nombrada y devolvia `None`: el sitio quedaba `NoTarget`, no se empujaba nada, y el
  llamador moria con `operand stack underflow` **lejos del origen y sin decir que fue**.
  - No es un caso raro: `super.m()` emite `invokespecial <superclase directa>.m` -que es lo que
    emite el javac real- y la superclase directa **no tiene por que declarar el metodo**.
  - **Un detalle que hay que tener presente al tocar esto:** `<init>` y `<clinit>` **no se heredan**
    (JVMS 2.9). No son metodos que la resolucion busque, son metodos de inicializacion que la VM
    invoca sobre una clase concreta. La primera version subia tambien por ellos, y una clase sin
    `<clinit>` propio "encontraba" el de su superclase y lo corria por segunda vez en el contexto
    equivocado - lo caza `class_init_pulls_in_only_default_declaring_superinterfaces`.
  - Probe: `java/SuperProbe.java` (+ `_A`, `_B` vacia a proposito, `_I` con un `default`). Da **7**.

- **#262 - OK: los miembros que un array hereda de `Object`.** Eran **dos** cosas, y la primera
  tapaba a la segunda:
  1. JLS 10.7 dice que los miembros de un array son los de `Object`. Su clase es **sintetica** -sin
     class file- asi que `build_vtable` la construia **vacia**: todo `array.hashCode()` erraba su
     slot y moria como `NoSuchMethodError`. El sitio tenia el slot **bien** (javac emite owner
     `java/lang/Object`, y ese slot es el de `Object`); lo que faltaba era la **tabla del receptor**.
     La tabla de un array **es** la de `Object`.
  2. Y su mirror se alocaba **sin escribir el header**, o sea un objeto sin clase. Que `getClass()`
     devolviera algo no-nulo **tapaba** el hueco: devolvia un objeto a medio construir, y cualquier
     cosa que leyera su clase -un `instanceof`, un `checkcast`, `getName()`- terminaba en *"could
     not resolve the object's class from its header"*. El camino de una clase normal escribe ese
     header; el de un array se habia quedado a mitad.
  - Probe: `java/ArrayProbe.java`, da **15**. La asercion del mirror va por `instanceof` y **no** por
    `getClass().getName()` a proposito: el `instanceof` lo resuelve la VM leyendo el header, sin
    ejecutar una linea de `java.lang.Class`, asi que mide lo que este probe mide.

#### Una nota de metodo: medir contra un arbol que se mueve

A mitad de esta tanda, `getClass().getName()` empezo a fallar para **toda** clase -incluida
`"x".getClass()`, que minutos antes daba 16-. No fue ningun cambio de aca:
`KajiLibrary/java/lang/Class.class` se habia reescrito cinco minutos antes. Es el mismo genero de
confusion que ya costo una atribucion equivocada en `FROZEN.md`, y la conclusion practica es la
misma: **una asercion de VM no debe depender de codigo de biblioteca que este en vuelo**. De ahi que
el probe use `instanceof` y no `getName()`.

**Medicion**: suite en aislamiento **1269 / 16**, las mismas 16 de la baseline, con 2 probes nuevos.

### Lote de biblioteca (2026-08-25): 985/985, la primera recompilacion limpia

**Las 985 fuentes de KajiLibrary compilan.** Nunca habia pasado: el numero venia de 970/978, y las
ocho que faltaban se cerraron todas — cinco por arreglos de aca, dos por el `Map.putAll` que la
sesion de biblioteca tenia en vuelo, y una por arrastre.

#### #266 — OK: los dos bugs de fuente

- **`ZonedDateTime.of(int x7, ZoneId)` llamaba a un `LocalDateTime.of` de siete enteros que no
  existia.** `LocalTime` ya tenia su `of(h, m, s, nano)`; lo que faltaba era el **puente** en
  `LocalDateTime`, que paraba en seis. Un metodo de una linea.
- **`ForkJoinTask.AdaptedCallable.exec()` llamaba a `Callable.call()` sin capturar ni declarar.**
  `call` declara `throws Exception` y `exec` no puede: ensancharla romperia el contrato del que
  depende todo llamador (JLS 8.4.8.3). Se relanza envuelta, que es lo que hace el JDK y lo unico
  que se puede hacer — con la no-chequeada pasando **sin envolver**, para no enterrar el tipo sobre
  el que el llamador esta capturando.

#### #267 — OK: los cuatro `import`, y la cadena que destaparon

Los cuatro se cerraron, pero el cuarto salio distinto de como lo decia el reporte y los otros tres
resultaron ser **la punta de una cadena**:

| Reportado | Lo que hizo falta |
|---|---|
| `Attribute$PersistentAttributeType` | nada: el tipo estaba escrito, faltaba el `.class` del anidado. Re-emitir `Attribute.java` |
| `java.security.ProtectionDomain` | la clase, con `getClassLoader()` y nada mas |
| `javax.sql.DataSource` | la interfaz, **vacia** |
| `java.util.Calendar` | la clase abstracta… y despues `java.util.Date`, que `Query` tambien pedia |
| (no reportado) | `java.net.URL` + `MalformedURLException` + `java.util.Properties`, que pedia `PersistenceUnitInfo` |

Siete tipos nuevos, todos con el mismo criterio que fijo `ClassLoader` en #205: **subconjunto si,
miembro que miente no**. Vale escribir en que se traduce eso en cada uno, porque no es lo mismo:

- **`DataSource` esta vacia, y eso es la respuesta.** En el JDK es una fabrica de
  `java.sql.Connection`, y `java.sql` no existe **entero**. Declarar `getConnection()` contra algun
  reemplazo seria peor que no declararla: la firma **es** el contrato, y un `getConnection` que
  devolviera otra cosa no es el metodo contra el que el llamador compilo.
- **`ProtectionDomain` contesta lo unico que puede contestar.** De sus cuatro partes —CodeSource,
  ClassLoader, Principals, PermissionCollection— tres no tienen ni tipo que nombrar. Y su
  `implies(Permission)` queda afuera por algo mas fuerte que la falta de tipo: **conceder o negar
  un permiso sin evidencia es una decision, no un dato**.
- **`Calendar` conserva la FORMA y no los metodos.** Un `long time`, un `int[] fields` al lado, y
  los cuatro ganchos que una implementacion concreta rellena (`computeTime`, `computeFields`,
  `add`, `roll`). Mantener esa forma vale mas que mantener metodos: es lo que permite que una
  subclase escrita contra el `Calendar` del JDK compile aca sin tocarla. `getInstance()` queda
  afuera porque necesita `TimeZone`, `Locale` **y** un `GregorianCalendar` concreto.
- **`Date` no es un hueco de tipo: es un `long`.** Toda su superficie no deprecada se puede
  escribir con la verdad. Lo que falta son justo los accesores de anio/mes/dia, que el JDK deprecio
  **porque leen un reloj de pared de un instante**, y eso no se puede sin `TimeZone`. Su
  `toString()` imprime el instante y no una fecha: renderizarla seria imprimir UTC diciendo que es
  la zona local.
- **`URL` delega el parseo a `java.net.URI`**, que ya estaba y ya implementa RFC 3986. Dos parsers
  para la misma gramatica es una chance mas de que discrepen, y un `URL` que partiera un string
  distinto del `URI` construido con ese mismo string seria un bug que nadie iria a buscar. Su
  `equals` compara la forma **parseada** y **no resuelve nombres**: el `URL.equals` del JDK es
  famoso por bloquear, porque compara las IPs a las que resuelven los hosts, y heredar eso seria
  heredar lo peor de la clase.
- **`Properties` extiende `Hashtable<Object,Object>`**, que es lo que explica que `getProperty`
  devuelva `null` —y no lo guardado— cuando el valor no es un String. Sin `load`/`store`: son un
  formato de stream, con sintaxis propia, y escribirlos contra una capa de IO que no modelamos es
  inventar un parser que nadie puede probar aca.

#### Y de arrastre: `KajiSourceFile` compila

Las tres razones por las que se omitio estan escritas en `javax/tools/JavaFileObject.java`, y las
tres se cayeron: `java.net.URI` existe, `JavaFileObject.Kind` ya se puede nombrar (#239), y con eso
`getKind`/`isNameCompatible`/`getCharContent`/`openReader` se escriben solas. Los dos de **bytes**
tiran `UnsupportedOperationException`, igual que `SimpleJavaFileObject` del JDK: el objeto es texto
en memoria y no hay codificacion elegida con la que convertirlo sin inventarla.

**El test del Filer sigue rojo, y ahora falla por otra cosa** — que es el progreso. Antes no
compilaba `KajiSourceFile`; ahora compila, corre, y el texto vuelve **vacio**: se recupera el
nombre (`Foo`) pero no el contenido. `StringWriter` anda —probado suelto: escribir y leer da 12—,
asi que lo que queda es la plomeria de APT del lado de la VM, probablemente un offset que sobrevive
a una colecta. Es otro trabajo; queda anotado abajo como **#277**.

#### #201 — parcial: 43 metodos, y el reporte tenia numeros de otro JDK

`synchronized` agregado en **`Vector` (31), `Stack` (3), `ByteArrayOutputStream` (5),
`ByteArrayInputStream` (2), `Throwable` (2)**. No se adivino: se leyo del `javap` del JDK real que
metodos lo declaran y se toco **solo** los que nuestra fuente ya tenia con ese nombre.

Dos cosas que salieron de hacerlo asi:

- **El reporte cuenta metodos que el JDK de hoy ya no sincroniza.** Pedia 6 en
  `BufferedInputStream`, 3 en `BufferedOutputStream` y 1 en `PushbackInputStream`; en el JDK 21 los
  tres tienen **cero** — se reescribieron para usar cierres internos. Un finding de superficie
  envejece con su referencia.
- **`Stack` ya sincronizaba por dentro**, con un `synchronized (this)` en el cuerpo y un comentario
  que explica por que (#105: un `return` desde adentro de un bloque `synchronized` filtra el
  monitor con nuestro javac). Agregar el modificador **anida** el mismo monitor. Se verifico que la
  VM lo soporta —un metodo `synchronized` con un `synchronized (this)` adentro devuelve lo que
  debe— antes de dejarlo asi; los monitores son reentrantes y el modificador es superficie de API,
  que es lo que el finding mide.

Queda `Hashtable` (11), que la sesion de biblioteca tiene en vuelo, y `StringBuffer`, **que ya
esta hecho** (45 metodos sincronizados hoy).

#### #203 — parcial: `System.out`, y por que el resto no se puede barrer por nombre

**`System.out` es `final`**, que es el caso que el finding nombra. Va como *blank final* —declarado
arriba, asignado en el `static {}`— porque el `PrintStream` no se puede construir antes de que la
clase se inicialice; es legal (JLS 8.3.1.2) y es como lo declara el JDK.

Del resto, el barrido automatico encontro **100** miembros en 15 clases… y **95 de ellos no se
pueden cotejar por nombre**. `ByteBuffer.put` tiene diez sobrecargas y solo algunas son `final`;
marcarlas todas romperia los overrides covariantes de las subclases, que es peor que el finding.
Con esa guarda puesta sobreviven **cinco** (`FormattableFlags.ALTERNATE`/`LEFT_JUSTIFY`/`UPPERCASE`,
`Locale.toString`, `Phaser.getPhase`), y esos se aplicaron.

**Lo que queda pide cotejo por descriptor, no por nombre** — que es exactamente lo que hace
`apidiff.py`. La conclusion practica es la misma que dejo la novena tanda sobre ese tool: el
comparador por descriptor no es un lujo, es la unica forma de medir esto sin romper algo.

**Medicion del lote**: KajiLibrary **985/985** (era 970/978). Suite en aislamiento **1270 / 16**,
la baseline. Verificado corriendo: `Date`/`Properties`/`URL` dan las once propiedades de su probe,
y `Vector`/`Stack`/`ByteArrayOutputStream` las cinco del suyo.
















---

## Open

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

> ### Prioridad vigente (revisada 2026-08-24)
>
> Lo de mas abajo en esta seccion quedo **viejo** (habla de #13–#17, todos cerrados). Esta es la
> foto de hoy, despues de verificar estado real contra el codigo y los commits: ya estan cerrados
> #101, #110, #112, #116, #202, #216, #225, #227, #229, #230, y #109/#121/#122 estan arreglados en
> el arbol de trabajo. #206 y #207 retirados. #236 duplica #115 y #237 duplica #114.
>
> **Nivel 0 — rompe la construccion hoy.**
> **#250** (regresion del desugar de `enum`): **70 de 941 fuentes no compilan**. Va primero ademas
> por encadenamiento: **bloquea el camino de #246**, porque sin poder recompilar la biblioteca no
> hay forma de cerrar el circulo.
>
> **Nivel 1 — la medicion no es confiable.**
> **#246**: 93 miembros publicos que `boot/` tiene y `KajiLibrary/` no, en 40 clases, mas 34 clases
> que solo existen en `boot/`. Mientras siga asi, "pasa el gate" y "funciona" hablan de artefactos
> distintos.
>
> **Nivel 2 — silencio: compila, pasa el gate, hace otra cosa.** La familia mas peligrosa, y **sigue
> creciendo** (#251 aparecio esta semana): **#251, #247, #248, #219, #108, #111, #120, #118, #114,
> #240**. **Los dos ultimos que quedaban abiertos (#114 y #219) resultaron ya cerrados**; ver el
> cierre del lote de parser y literales.
> Recomendacion transversal: **una guarda que convierta "no resolvio" en error duro** cubre ~10 de
> estos de una sola vez, es independiente de arreglar cada resolucion, y cambia el modo de falla de
> "compila, gatea, revienta lejos" a "no compila". El propio documento ya la pedia en #20 y #114.
> **Hecha el 2026-08-24** (en `codegen.rs::invoke`, como parte de #261). El efecto medido sobre las
> 978 fuentes: 5 fallas → 7, pero once miscompilaciones mudas se volvieron visibles.
> **Segunda mitad, el 2026-08-25** (`codegen.rs::audit_declared_types`, cierre de #208): la misma
> guarda pero para los **tipos declarados**, no para las llamadas. Efecto: 4 fallas → 8, y las
> cuatro nuevas son firmas que se venian emitiendo con `Object` donde va un tipo que no existe.
> Queda una tercera mitad sin hacer: los tipos que aparecen **solo en los cuerpos**.
>
> **Nivel 3 — bytecode invalido o la VM revienta. ✅ CERRADO (2026-08-24).**
> Estaban **#238**, **#124**, **#217**, **#217b**, **#226**, **#244** y **#220**. Los siete se
> cerraron, y tres de ellos por una causa que no era la reportada: **#220 y #244 no eran de la VM,
> eran #110** (el lector de `.class` descartaba `ACC_STATIC`, asi que el emisor sacaba `getfield`
> donde iba `getstatic`), y **#226 ya estaba implementado** — lo que faltaba era compilar con
> `-cp KajiLibrary`. Tirando de ese hilo salio **#261** (un array no era un tipo referencia, y una
> llamada sin resolver se emitia como nada), que tapaba once fuentes rotas de la biblioteca.
>
> La recomendacion transversal del nivel 2 —**una guarda que convierta "no resolvio" en error
> duro**— quedo **aplicada** como parte de #261. Destapo tres findings nuevos (#262, #263, #264) y
> un bug de biblioteca; ver la undecima tanda.
>
> ---
>
> ### Los que llegaron despues (#252–#264), ya clasificados
>
> **Cerrados sin tocarlos, por los arreglos de nivel 3.** Cuatro de los trece. Vale la pena que
> quede escrito **por que**, porque no es casualidad: los cuatro eran sintomas de bugs que ya
> estaban en la lista con otro numero.
> - **#258 ≡ #261** — el mismo bug, encontrado en paralelo. Verificado con su propio repro.
> - **#252** — receptor de tipo variable de tipo: lo cerro `types::member_class`.
> - **#254** — el override de una interfaz parametrizada era "ambiguo": lo cerro la deduplicacion
>   de candidatos por firma **borrada** (#122).
> - **#253, cara muda** — la que el propio finding llamaba "la cara cara": hoy es error duro.
>
> **Nivel 3 (bytecode invalido o la VM revienta) — tres nuevos. ✅ CERRADOS (2026-08-24).**
> - **#255** — `ACC_SYNCHRONIZED` no se emitia: ningun metodo `synchronized` tomaba el monitor.
>   Una linea en `modifier_flag`.
> - **#257** — el `catch` caia dentro de la copia **excepcional** del `finally`. No era "falta
>   duplicar el `finally`" (ya se duplicaba): era que un *handler* siempre es alcanzable y nadie
>   reseteaba `reachable` al entrar. Una linea, y el bytecode calza offset por offset con javac.
> - **#259** — el pool iba en UTF-8 estandar y no en el **modificado** de §4.4.7. Comprobado por
>   diferencial contra el javac del JDK 25: los dos emiten `ed a0 b4 ed b5 a0` para `U+1D160`.
>
> Los tres eran arreglos chicos con radio de daño grande, y los tres tenian repro antes de empezar.
> Vale registrar que **ninguno estaba donde el reporte decia**: #257 se leia como "falta duplicar el
> `finally`" y era un flag de alcanzabilidad; los otros dos eran una linea que faltaba en una tabla.
>
> **Nivel 4 (contrato incompleto).** **#260** (el ternario colapsa `char`/`byte`/`short` a `int`,
> §15.25), **#253** cara ruidosa, **#263** (receptor con variable de tipo de **cota externa**),
> **#264** (`? super` anidado). **#256 duplica #104** — el atributo `Exceptions` se escribe pero no
> se lee — y se unifica ahi.
>
> **VM.** **#262** (**cerrado el 2026-08-25**): los miembros que un array hereda de `Object` no
> despachan. Lo destapo cerrar
> #261, que hizo que el compilador por fin los **emitiera** bien.
>
> **Residuo abierto de #247.** El finding se dio por cerrado con #251, y la mitad del compilador lo
> esta: hoy emite `invokeinterface IntStream$Builder.accept/build` donde antes ponia `iconst_1;
> pop`. Pero `acumula()` sigue devolviendo `None` en runtime, **tambien contra la biblioteca
> recompilada**, asi que queda algo del lado de la VM o de `IntStream.Builder`. No cerrarlo del
> todo hasta que de 3.
>
> ---
>
> **Nivel 4 — contrato incompleto, con rodeo. CERRADO (2026-08-24).**
> Estaban los 28 de la lista de abajo. **Doce ya no reproducian** cuando se los verifico uno por uno
> -arrastrados por los cierres de nivel 2 y 3-, y los dieciseis restantes se cerraron.
>
> Resolucion de nombres: **#210, #214, #239, #245, #249, #106, #117, #119**.
> Genericos: **#100/#241, #204/#215, #211, #212, #223, #123, #253, #264, #221, #263**.
> Modificadores: **#115/#236, #200, #242**. Chequeos: **#104/#256, #213, #222**.
> Otros: **#231/#125, #233, #234, #243, #260**.
>
> **Dos patrones se repitieron tanto que conviene tenerlos a mano al leer un finding nuevo:**
>
> 1. **El reporte nombra el sintoma, no la causa, y a veces ni el componente.** #220 y #244 decian
>    "VM" y eran el compilador (#110). #204/#215 decian "inferencia" y era un scope. #257 decia
>    "falta duplicar el `finally`" y era un flag de alcanzabilidad. #253 y #221 decian cosas
>    distintas y eran los dos aplicabilidad. Conviene **instrumentar antes que leer**: en #204 el
>    diagnostico salio de imprimir el bound insatisfacible, no de mirar el codigo.
> 2. **Dos caminos que deberian dar lo mismo y no lo dan.** El `Signature` correcto con el
>    descriptor equivocado (#100/#241), la pasada 1 resolviendo lo que la pasada 2 no (#204/#215),
>    dos predicados para el mismo modificador (#110/#238). Cuando un artefacto sale bien y su
>    gemelo mal, la causa esta en que se calculan por separado.
>
> **Nivel 5 — menores y de biblioteca. La parte de javac, CERRADA (2026-08-25).**
> **#209, #224, #228, #232, #235** — dos de ellos (**#232**, y con ellos **#114** y **#219** del
> nivel 2) ya no reproducian: los arrastraron #251 y el cambio de estrategia de concatenacion a
> `invokedynamic`. Verificar #219 destapo **#268**, el `checkcast` sintetico que faltaba, que es el
> unico defecto de esta tanda que la **JVM real** rechazaba y el gate propio no podia ver.
> Biblioteca: **#201, #203, #205, #267**.
>
> **Cuatro duplicados entre rangos, para unificar** (los del rango viejo ya traen repro y analisis):
> **#241≡#100**, **#217≡#103**, **#200≡#118**, **#231≡#125**.

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

- **#109 — ✅ FIXED (2026-08-24).** `attribute.rs` clasificaba el ternario en **dos** formas y §15.25
  define **tres**: faltaba la **booleana**. Como `boolean` es un `RType::Prim`, un ternario booleano
  entraba por la promocion numerica y moria en `is_numeric`. Se agrego la rama que faltaba, cubriendo
  tambien `Boolean` (§15.25 da `boolean` para las dos formas). Verificado en bytecode: `eq()` emite
  `if_acmpne` … `invokevirtual Object.equals:(…)Z` … `ireturn`, y el caso con envoltorio pasa por
  `Boolean.booleanValue()Z`. Reporte original abajo.
  A boolean-valued conditional expression is rejected as "operando no numérico". The
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

- **#111 — ✅ ARREGLADO (2026-08-24).** En `attribute.rs`, la resolucion de una llamada busca los
  miembros con `types::erased_id(&recv)`, que devuelve `None` para un `RType::TypeVar`. La resolucion
  caia al brazo `None` y **la llamada se descartaba en silencio**. La regla es §4.4: los miembros de
  una variable de tipo son los de su **cota** (`Object` si no declaro ninguna), que es justo lo que da
  `types::erasure`; ahora el receptor se erasa antes de buscar. Verificado: `viaTypeVar` emite
  `getfield value; aload_1; invokevirtual java/lang/Object.equals:(Ljava/lang/Object;)Z; ireturn`,
  donde antes salia `aload_1; areturn` —devolver el propio argumento desde un metodo `boolean`—.
  Reporte original abajo.
  A method call on a receiver whose static type is a TYPE VARIABLE is silently dropped.
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

- **#119 — ✅ ARREGLADO (2026-08-24), y con el se cerraron #120 y #126.** La causa era un **atajo de
  resolucion** en `enter.rs`: tanto `resolve_name_to_sym` como `resolve_class_name` hacian
  `strip_prefix("java.lang.")` y usaban lo que quedaba **como si fuera el nombre simple**. Para un
  tipo de un **subpaquete** eso deja `"ref.WeakReference"`, que no es clave de ningun externo —se
  registran por nombre simple— asi que el atajo devolvia `None`/`false` **cortando antes** del camino
  generico de nombres cualificados, que lo habria resuelto sin problema.
  - De ahi el sabor caprichoso de la familia: el tipo resolvia en unas posiciones y en otras no, el
    descriptor salia `Object`, o la llamada desaparecia entera.
  - **Aislado con un A/B sintetico**, no por inspeccion: la misma jerarquia (`Padre` con el metodo,
    `Hijo extends Padre`, un llamador que solo los ve por `-cp`) compila **bien en `zz.ref`** y
    **reproduce en `java.lang.zzref`**. Es el prefijo, no el paquete.
  - El atajo se conservo para lo que fue creado (el shadowing del fuente, #5) pero solo para un
    miembro **directo** de `java.lang`; un `java.lang.Thread.State` cae al camino de abajo, que sabe
    bajar a un tipo anidado.
  - Verificado: `WeakReference.<init>` emite `(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V`
    (era `(Object,Object)`) y `ReferenceQueue.poll()` emite `()Ljava/lang/ref/Reference;` (era
    `()Object`). **`java.util.WeakHashMap` deja de estar bloqueado.**
  - **#120 cae con el mismo fix**: `viaSubclass` emite
    `aload_1; invokevirtual java/lang/ref/Reference.get; areturn`, donde antes salia `0: areturn` con
    la pila vacia.
  - **#126 tambien**: desaparece el **puente espurio** `public Object getTarget()` que se emitia de
    mas. (Ver la nota de #126: el reporte decia que "el retorno del override se erasa a Object", y en
    realidad el metodo real siempre estuvo bien; lo que sobraba era el puente.)
  - Suite: **1244 pasan / 16 fallan**, identico a la linea base. Texto original abajo.

  A type from a SUBPACKAGE of `java.lang` erases to `java.lang.Object` in the descriptor of
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

- **#121 — ✅ FIXED (2026-08-24).** No era un hueco del emisor, aunque el mensaje dijera "el generador
  de bytecode todavia no soporta un `super(...)`": era **resolucion**. `attribute.rs` usaba
  `super_class`, que devuelve el supertipo ya **borrado** (`G`), asi que `ctor_binding` no tenia
  argumentos de tipo que sustituir y la `E` **de G** no matcheaba con la `E` de la subclase. Ahora se
  pasa el supertipo **como se declaro** (`G<E>`), igual que hace el receptor de una llamada a metodo.
  Verificado sobre `repros/finding_121.java`: el ctor de `G121Sub` emite
  `aload_0; aload_1; invokespecial G121."<init>":(LBox121;)V`. Reporte original abajo.
  `super(...)` fails to resolve when the target constructor takes a PARAMETERIZED type
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

- **#122 — ✅ FIXED (2026-08-24).** `attribute::candidates` **ya** deduplicaba, pero comparando los
  parametros **sin borrar**: la `V` de la clase y la `V` de su interfaz son simbolos **distintos**, asi
  que `Callable<V>` no era igual a `Callable<V>` y quedaban dos candidatos. Ahora compara la firma
  **borrada**, que es como §8.4.2 juzga la override-equivalencia y lo que §15.12.2.5 pide antes del
  test de especificidad. (El buscador de SAM del mismo archivo ya lo hacia bien; el fix los alinea.)
  Verificado: la llamada por el tipo de la clase emite `invokevirtual`, la del tipo de la interfaz
  `invokeinterface`, y un overload real de dos firmas distintas **no** se deduplica. Reporte original
  abajo.
  Overload resolution counts a class's declaration and its interface's re-declaration as
  two distinct candidates.** `ExecutorCompletionService<V>` declares `submit(Callable<V>)` and
  implements `CompletionService<V>`, which declares the same method. A call through the *class*-typed
  receiver is rejected as "la referencia a `submit` es ambigua"; typing the local as the interface
  compiles fine. This is not ordinary ambiguity — the two candidates are the same method, one being
  the implementation of the other, and JLS 15.12.2.5 removes such duplicates before the
  most-specific test. It bites any class that implements an interface and re-declares its methods,
  which is the normal shape for a concrete implementation.
  - Workaround in the library: fixtures declare the local with the interface type.

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

## jakarta.persistence — cerrado en 203/203, con 9 metodos omitidos (2026-08-24)

Las 6 clases que faltaban entraron. Ninguna necesito un fix de compilador: entraron **omitiendo el
miembro que no compila**, que el gate acepta porque compara la superficie declarada y exige
SUBCONJUNTO, no igualdad. Queda anotado para que nadie lo lea como "la API esta completa".

| Clase | Omitido | Por que | Vuelve cuando |
|---|---|---|---|
| `SetJoin` / `CollectionJoin` / `ListJoin` | `getModel()` | #123 | se arregle #123 |
| `MapJoin` | `getModel()` | #123 | se arregle #123 |
| `CriteriaBuilder` | `currentDate()`, `currentTime()`, `currentTimestamp()` | piden `java.sql`, otro modulo | exista `java.sql` (arrastra `java.util.Date`) |
| `CriteriaBuilder` | `toBigDecimal()`, `toBigInteger()` | piden `java.math` | exista `java.math` |

**Al 2026-08-24:** `MapJoin.entry()` VOLVIO — #101 esta arreglado y `Map.Entry` ya resuelve,
emitiendo `()Ljakarta/persistence/criteria/Expression;` como corresponde. Quedan 9: los cuatro
`getModel()` (#123, sigue abierto) y los cinco que piden `java.sql`/`java.math`. De esos,
`toBigDecimal()`/`toBigInteger()` ya tienen su dependencia (`java.math` esta 4/4) y estan
escritos en la fuente, pero **no se pudieron verificar**: `CriteriaBuilder.java` no compila hoy
por #250.

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

---

## Barrida de diff de API contra OpenJDK 25 — #200 en adelante (2026-08-22)

Se recompilaron **las 834 fuentes** de KajiLibrary con el javac congelado (`bin/javac.exe`, ver
`bin/FROZEN.md`) en un arbol aparte, y se comparo la superficie declarada contra `javap -p` del
JDK 25. Herramientas versionadas en `tools/apidiff/` (`recompile.py` + `apidiff.py`), reproducible:

```
python tools/apidiff/recompile.py /tmp/libfresh /tmp/recompile.json
JDK_HOME=H:/jdk-25.0.2 python tools/apidiff/apidiff.py /tmp/apidiff.json /tmp/libfresh
```

### Antes de leer nada: los `.class` versionados NO son lo que emite el javac de hoy

`RandomGenerator.class` en el repo declara `default int nextInt()`; recompilado ahora dice
`public default int nextInt()`. Al recompilar, **104 divergencias de `-public` desaparecieron**:
eran bytecode viejo, no defectos vigentes. **Comparar contra el bytecode versionado mide una mezcla
de "que dice la fuente" y "que javac la compilo, y cuando"** — por eso `apidiff.py` se corre sobre
la salida de `recompile.py` y no sobre `KajiLibrary/` directamente. Vale para cualquier auditoria
futura, no solo para esta.

| | Bytecode versionado | Recompilado con el congelado |
|---|---:|---:|
| Cobertura de miembros | 33.6% | **34.6%** (4516 de 13068) |
| Publicos/protected faltantes | 4660 | **4476** |
| Firmas con modificadores distintos | 395 | **291** |

522 clases comparables (nuestras que existen en `java.base`); 383 con la declaracion de clase
identica; **78 clases completas** (0 miembros faltantes), casi todas interfaces.

---

- **#201 — ⬜ falta `synchronized` donde la spec lo exige (biblioteca).** 78 metodos:
  `Vector` 28, `StringBuffer` 13, `Hashtable` 11, `BufferedInputStream` 6, `ByteArrayOutputStream` 5,
  `Stack` 3, `BufferedOutputStream` 3, `ByteArrayInputStream` 2, `Throwable` 2, y uno cada uno en
  `PushbackInputStream`, `Thread`, `Random`, `GZIPOutputStream`, `ZipOutputStream`.
  `Vector`/`Hashtable`/`StringBuffer` estan **especificados** como sincronizados; ese es el unico
  motivo por el que existen al lado de `ArrayList`/`HashMap`/`StringBuilder`. Dejo de ser cosmetico
  cuando el runtime gano paralelismo real (`JVM_THREADS=os`).

- **#202 — ✅ RESUELTO. Faltaba `abstract` en 30 metodos, 18 de ellos en `java.nio.ByteBuffer`.**
  Nuestro `ByteBuffer` declaraba concretos los `putLong(int,long)`, `putInt`, los `getX` y compania
  que el JDK declara abstractos, asi que no era la clase base abstracta que deberia: una subclase no
  estaba obligada a implementarlos y heredaba un cuerpo que no le corresponde. Mismo patron, mas
  chico, en `compact()`/`isDirect()` de `CharBuffer`, `DoubleBuffer`, `FloatBuffer`, `IntBuffer`,
  `LongBuffer` y `ShortBuffer` (2 cada uno).
  **Cerrado** moviendo las implementaciones a los `Heap*Buffer` que ya existian — el unico subtipo
  concreto —, sin introducir ninguna clase nueva. El gate paso de 30 `-abstract` + 21 `-final` a
  **cero divergencias de modificadores**.

- **#203 — ⬜ falta `final` en 48 miembros (biblioteca).** El caso visible es **`System.out`**, que
  en el JDK es `public static final PrintStream` y aca es asignable. Un `final` de menos en un campo
  publico tambien cambia lo que el compilador puede plegar (ver #112).

- **#205 — 🟡 PARCIAL (2026-08-24): 2 de los 3 huecos cerrados, 3 de los 4 archivos compilan.**
  - **`java.lang.ClassLoader` — creado.** Existe porque la API necesita el **tipo**
    (`ClassTransformer.transform(ClassLoader, …)`, `PersistenceUnitInfo`), y ese es su alcance
    honesto: **no carga clases**. Nuestra VM resuelve por metaspace y boot path, sin delegacion de
    loaders ni `defineClass` ni namespaces por loader, asi que `loadClass` tira
    `ClassNotFoundException` en vez de devolver algo plausible — un `loadClass` que devolviera `null`
    le mete un `Class` nulo a un llamador que tiene todo el derecho a desreferenciarlo. Mismo criterio
    que `PersistenceProviderResolverHolder`: subconjunto sí, miembro que miente no.
    Destraba `ClassTransformer.java` y `PersistenceUnitInfo.java`.
  - **`Collection.stream()` — agregado como `default`**, igual que el JDK (hay 15 implementadores y
    ninguno deberia tener que escribirlo). El JDK lo hace con
    `StreamSupport.stream(spliterator(), false)`; esa ruta esta cerrada porque `java.util.Spliterator`
    no existe —la misma razon por la que `StreamSupport` se declaro inviable—, asi que recorre el
    iterador a un arreglo y se lo pasa a `Stream.of`. La diferencia es la **pereza**, no el resultado:
    el del JDK tira del origen bajo demanda, este saca una foto primero.
    Verificado corriendo: `l.stream().count()` sobre un `ArrayList` de dos elementos devuelve **2**.
    Destraba `Query.java`.
  - **`Map.putAll` — NO hecho, y el motivo importa.** En el JDK es **abstracto** en `Map`, y para
    implementarlo hay que poder **recorrer el mapa origen** (el call site es
    `this.properties.putAll(properties)`, con el argumento tipado como la interfaz `Map`). Nuestro
    `Map` no declara `keySet`/`values`/`entrySet`, asi que hoy **no hay con que recorrerlo**: cerrar
    esto pide primero agregar una enumeracion a `Map` —`keySet()` es la mas barata y es API real del
    JDK— e implementarla en los **8** implementadores (`HashMap` y compania guardan `keys[]`/`values[]`
    sueltos; solo `AbstractMap` declara `entrySet()`). Son ~16 metodos chicos sobre la interfaz de la
    que cuelga media biblioteca. Queda anotado como el paso siguiente, deliberadamente separado para
    que una regresion ahi no se mezcle con lo demas.

  Reporte original abajo.

  **Huecos de biblioteca que voltean la compilacion de terceros (NO es el compilador).**
  Anotado explicitamente porque casi se atribuye a #14/#15 de resolucion:
  - `Map.putAll` y `Collection.stream` **no estan declarados** — las unicas apariciones de esos
    nombres en `Map.java`/`Collection.java` son comentarios que dicen "the JDK also has …". Voltean
    `jakarta/persistence/PersistenceConfiguration.java:413` y `jakarta/persistence/Query.java:96`.
  - **`java.lang.ClassLoader` no existe** en la biblioteca. Voltea
    `jakarta/persistence/spi/ClassTransformer.java` y `.../PersistenceUnitInfo.java`.
  El diagnostico del javac ("no se encuentra el metodo/simbolo") es **correcto** en los cuatro casos.

- **#206 — RETIRADO.** Ver "La regla" al final: las 11 clases que listaba como "izadas a top-level"
  son **package-private**, o sea internos, y por lo tanto libres.

- **#207 — RETIRADO.** Ver "La regla": `TreeMap.checkInvariants()` y `Vector.countUnlocked()` son
  **package-private**, no superficie.

### Segunda tanda: dogfooding de `java.lang.reflect` (2026-08-22)

Salieron de escribir el paquete `java.lang.reflect` con el javac congelado. **Los cinco primeros se
verificaron con repro propio antes de asentarlos.**

### Tercera tanda: dogfooding de `java.util.stream` (2026-08-22)

- **#218 — 🔴 una lambda pasada como ARGUMENTO a un metodo cuyo tipo declarante vino de un `.class`
  del classpath no genera bytecode.**

  ```java
  // IH.java, compilado antes a IH.class
  public interface IH { int f(Function<String,Integer> g); }
  // Caller.java, compilado con -cp <dir de IH.class>
  static int a(IH h) { return h.f(x -> x.length()); }
  // error: el generador de bytecode todavia no soporta una expresion lambda (necesita invokedynamic)
  ```

  Con la misma interfaz **en la misma unidad de compilacion**, compila sin chistar.
  **Precision del disparador** (la primera lectura era mas amplia y no reproducia): no alcanza con que
  el *tipo destino* venga del classpath — una lambda en posicion de **retorno** con tipo destino del
  classpath (`static Function<String,Integer> make() { return x -> x.length(); }`) **compila bien**.
  Lo que falla es la **posicion de argumento a traves de un metodo declarado en un `.class`**.
  **Consecuencia:** ningun codigo fuera de KajiLibrary puede usar lambdas contra la API de
  KajiLibrary. `stream.filter(x -> ...)` es hoy imposible, y por eso las suites de prueba del paquete
  hubo que escribirlas con clases nombradas.

#### Actualizaciones a findings existentes

- **#17 — ✅ ARREGLADO** (verificado en esa sesion). El retorno de variable de tipo de metodo
  "pelada" ya unifica en el chequeo de override, asi que `<R,A> R collect(Collector<? super T,A,R>)`
  compila. Era el que bloqueaba `Stream.collect(Collector)`, que ahora esta implementado.

### Cuarta tanda: dogfooding de `java.util.regex` (2026-08-22)

#### Correccion: el caso de la "constante compuesta" NO es del compilador

Se reporto que un `static final` inicializado con una expresion constante *compuesta*
(`0x40 | 0x80`) no recibe `ConstantValue` y se lee 0. **La segunda mitad es cierta; la primera no.**
Matriz verificada — las cuatro formas reciben el atributo, con el valor **ya plegado**:

```
public  static final int PUB_SIMPLE;   ConstantValue: int 7
public  static final int PUB_COMP;     ConstantValue: int 192
private static final int PRI_SIMPLE;   ConstantValue: int 7
private static final int PRI_COMP;     ConstantValue: int 192
```

El compilador esta bien: pliega `0x40 | 0x80` a 192 y lo emite. La lectura de 0 es **#216** (la VM no
aplica `ConstantValue`), y esta observacion lo **amplia**.
*Por que se vio distinto:* nuestro `javap` no lista campos privados sin `-p`, asi que un volcado sin
esa bandera parece no tener el atributo. **Al auditar un `.class`, usar siempre `-p`.**

#### Metodo que vale la pena robar

La sesion de regex no podia validar en runtime (la VM no ejecuta el paquete, ver #225/#226/#227), asi
que compilo **las mismas fuentes con el `javac` real** bajo el paquete `kaji.regex` y las corrio en
**diferencial contra `java.util.regex`**: 163 comprobaciones, 0 discrepancias semanticas. Eso le
encontro y arreglo dos bugs propios (`^` con `MULTILINE` al final del input, e interseccion de clases
aceptada en silencio). **Es la unica forma que tenemos hoy de validar comportamiento de la biblioteca**
mientras la VM no pueda ejecutarla, y no depende de arreglar nada primero.

### Quinta tanda: `java.text` y `java.nio` (2026-08-22)

#### Correccion a #110 — NO esta arreglado en este arbol

Se habia reportado que si, por ver `pub is_static: bool` en `classfile.rs`. **Esta la mitad que lee y
falta la que consume.** Verificado de punta a punta:

```java
public class K5 { public static final int CONST = 7; }
public class K6 { public static int other() { return K5.CONST; } }
```
```
public static int other();
  stack=1, locals=0
     0: getfield  #34   // Field K5.CONST:I      <-- getfield sobre un STATIC, con la pila vacia
     3: ireturn
```

Bytecode estructuralmente invalido; la VM muere. Donde falta el fix: `classfile.rs:128` si puebla
`ExtField.is_static`, pero `enter.rs:531` y `enter.rs:660` construyen los simbolos de campo con
`modifiers: Vec::new()`, y `.is_static` se consume para **metodos** (`enter.rs:575`) y **nunca para
campos**. `codegen::field_info` decide el opcode por `modifiers`, que para un campo del classpath
siempre esta vacio.
**Corolario:** lo que la sesion de `java.nio` reporto como "leer un static del bootclasspath desde
otra clase revienta" (`Boolean.TRUE`, `Integer.MAX_VALUE`, `ByteOrder.BIG_ENDIAN`) **no es un
defecto de VM aparte**: es este mismo `getfield` invalido.

#### Defectos nuevos verificados

- **#230 — ✅ CERRADO Y VERIFICADO EN LA VM (2026-08-24).** El fix entro en `71f0cde` (`build_vtable`
  ya no saltea un miembro sin `Code`: un `abstract` resuelve, toma slot, y el override aterriza donde
  el call site mira — mismo precedente que los `native`). Faltaba **comprobarlo corriendo**, que es lo
  que se hizo con `repros/finding_230.java`, nuevo, cinco formas, todas en verde sobre `run-headless`:

  | Caso | Esperado | Da |
  |---|---|---|
  | `run()` — por el **tipo abstracto** (EL caso) | 25 | 25 |
  | `plantilla()` — el abstracto llamado **desde la superclase** | 50 | 50 |
  | `dosNivelesPorRaiz()` — `Buffer` ← `BufferDeBytes` ← concreta, **saltando el intermedio** | 9 | 9 |
  | `dosNivelesPorMedio()` — por el abstracto intermedio que **re-declara** | 9 | 9 |
  | `abstractoConInterfaz()` — abstracta que hereda el metodo de una **interfaz** | 12 | 12 |

  Los dos de dos niveles son la forma de `java.nio` (`Buffer`/`ByteBuffer`/`HeapByteBuffer`), que era
  el bloqueante declarado. La *anomalia sin explicar* del reporte original (`DecimalFormat` escapaba
  mientras una gemela minima fallaba) queda **sin objeto**: con el slot bien asignado, andan las dos.
  Texto original abajo.

  Panic
  `field_offset: field not found…` en `objects_operations.rs:410`. Llamar al mismo objeto por el
  **tipo concreto** anda. **Dos sesiones independientes lo encontraron** (`java.text` y `java.nio`),
  con A/B minimo: mismo cuerpo, misma clase, mismo tipo estatico; lo unico que cambia es si el metodo
  sobreescribe un `abstract`. Hipotesis: `metaspace.rs::build_vtable` hace
  `let Some(method) = resolve_method(...) else { continue }`, y `resolve_method` devuelve `None` para
  un metodo sin `Code`, asi que **un `abstract` declarado en una clase nunca recibe slot**.
  **Es el bloqueante nº 1 de `java.nio`** y de cualquier API basada en clases abstractas.
  *Anomalia sin explicar:* `java.text.DecimalFormat` escapa a esto, mientras que una gemela minima
  puesta en el mismo paquete falla.

#### Reportado pero NO reproducido

- **Un metodo `private` de instancia en una interfaz contado como abstracto** (JLS §9.4). **No
  reproduce** con el caso minimo: compilando la interfaz a `.class` y luego la implementacion con
  `-cp`, **compila sin diagnostico**. El disparador debe ser mas estrecho que lo reportado.
- **El import on-demand (`import java.util.*;`) no resolveria tipos.** **No reproduce:**
  `import java.util.*; new ArrayList<String>().size()` compila igual que con import explicito.

#### Lo mas util de todo: que tan delgada es la base

| Clase | Lo que hay | Lo que NO hay |
|---|---|---|
| `String` | `length charAt equals hashCode startsWith isEmpty isBlank substring(int,int) subSequence compareTo valueOf format` | `indexOf`, `substring(int)`, `toCharArray`, `toLowerCase/UpperCase`, `trim`, `split`, `regionMatches`, `equalsIgnoreCase`, ctor `String(char[])` |
| `Character` | `MIN_VALUE MAX_VALUE valueOf charValue compareTo` | `isLetter`, `isDigit`, `isWhitespace`, `isUpperCase`, `getType` |
| `Math` | `abs max min` (solo `int`) | `abs(double)`, `round`, `pow`, `ulp`, `IEEEremainder` |
| `Double` | — | `parseDouble`, `longBitsToDouble`, `isNaN`, `NaN`, `POSITIVE_INFINITY`, `MAX_VALUE` |
| `StringBuilder` | | `setLength`, **`append(Object)`** (ver #114) |
| `Object` | | `clone()` |

Cada paquete que se agregue va a chocar con esto antes que con el compilador. `java.text` tuvo que
traer su propia clasificacion de caracteres, su propio parser decimal y una conversion IEEE-754 por
aritmetica pura.

### Sexta tanda: `javax.lang.model` + `javax.lang.model.type` (2026-08-22)

Resultado: **20 de 20 clases nuevas, todas compilando** (`javax.lang.model` 3/3,
`javax.lang.model.type` 18/18, el paquete no existia). 14 salen identicas al JDK 25.

- **#237 — ES #114, que reaparece con `append(Object)`.** `"texto" + <referencia no-String>` no
  compila: nuestro `java/lang/StringBuilder` declara `append` para `char`, `String`, `boolean` y
  `CharSequence` — **no para `Object`**. #114 documenta este defecto con `append(long)` y lo da por
  arreglado del lado de la biblioteca: **el arreglo fue parcial**.

  ```java
  static String a(int i)    { return "x" + i; }   // compila
  static String f(Object o) { return "x" + o; }   // error: no se encuentra el metodo: append
  ```

  Es hueco de biblioteca, no del compilador — el `javac` real pasa por `String.valueOf`. Pero el
  **diagnostico es malo**: sale en la **linea 0**, apunta al inicio del archivo y nombra `append`, no
  la expresion de concatenacion. Repro: `repros/finding_114b.java`.
  **La variante silenciosa de #114 es real** (se dudo en su momento): la sesion de `element` la vio
  produciendo bytecode invalido en los tres constructores `Unknown*Exception` —
  `super("... " + e + " ...")` emitia el `invokespecial` con la pila vacia → *operand stack
  underflow*. Depende de la forma de la expresion; en las formas simples da error duro.

> **Patron a tener en cuenta al leer informes de sesion.** Van tres defectos reportados que no
> reproducen en aislamiento (metodo `private` en interfaz, import on-demand, y la variante silenciosa
> del concat). La hipotesis mas probable es el estado del classpath: una sesion trabajando dentro de
> un paquete grande compila contra `.class` de compañeros a medio escribir, y ve comportamientos que
> un repro limpio no tiene. **Vale reproducir en un directorio vacio antes de asentar un finding.**

### Septima tanda: `javax.tools` (2026-08-22)

Resultado: **14 de las 15 clases publicas faltantes**, mas completar `Diagnostic` y `JavaFileObject`,
que eran cascaras de una linea. 16/16 fuentes compilan, 22 `.class` (16 top-level + 6 anidados).
Ocho salen identicas al JDK miembro por miembro.

- **#240 — ✅ ARREGLADO (2026-08-24).** `src/bin/javac.rs` tomaba **un solo** archivo (`args.get(1)`) y
  descartaba el resto sin decir nada. Ahora todos los modos (`--emit`, `--check`, `--tokens`, …)
  procesan la lista completa. Verificado: `javac --emit A.java B.java` escribe los dos `.class`.
  **Ojo con lo que esto NO arregla:** cada archivo se sigue compilando como unidad independiente, asi
  que uno no ve los tipos del otro — eso es **#234**, que sigue abierto. Lo unico que cambia aca es
  que ningun archivo se pierda sin aviso. Texto original abajo.
  `--emit` con varios archivos ignora los que siguen al primero, en silencio. Amplia
  #234, y es peor de lo que decia: no es solo que no resuelva cruzado.

  ```
  bin\javac.exe --emit p/E1.java p/E2.java
  javac: escrito ...p\E1.class (499 bytes)
  ```
  `E2.class` **no se emite y no se reporta nada**; salida 0. Cualquier script de build que pase una
  lista de archivos compila solo el primero y cree que anduvo todo.

#### Confirmaciones de findings ya abiertos

- **#104 — confirmado, con la consecuencia real.** El `throws` de un metodo leido de un `.class` del
  classpath se ignora, **y ademas el override legal se rechaza**:
  ```java
  public interface Thrower { void go() throws IOException; }        // compilado antes al cp
  public class Impl implements Thrower { public void go() throws IOException { } }
  // error: `go` declara lanzar `IOException`, mas ancho que lo que permite `Thrower` (§8.4.8.3)
  ```
  Las dos declaraciones son **identicas**. En la misma unidad de compilacion pasa sin chistar.
  Efecto: **ninguna implementacion de una interfaz de I/O puede declarar `throws`**.
- **#200 — vivo**, confirmado en `Tool.run(..., String...)` y dos mas.

#### Hueco de biblioteca, no del compilador

`java.io.Closeable.close()` y `java.io.Flushable.flush()` de KajiLibrary **no declaran
`throws IOException`**, a diferencia del JDK. Sumado a #104, es la razon real de que
`JavaFileManager.flush`/`close` hayan quedado sin `throws`. Para una sesion de `java.io`.

#### Nota de criterio, vale como precedente

`SimpleJavaFileObject` se omitio **entera**: sin `java.net.URI` y sin poder nombrar `Kind`, se caen
sus dos campos y su **unico** constructor, y javac sintetizaria un `public SimpleJavaFileObject()`
que la API real **no tiene**. Preferir la ausencia a la declaracion falsa: el gate acepta subconjunto,
pero da por buena una firma inventada.

### Octava tanda: `javax.lang.model.element` (2026-08-22)

Resultado: **21/21 fuentes, 21/21 compilan**, 29 tipos emitidos (21 top-level + 8 anidados de
`ModuleElement`). **24 de 29 con conjunto de miembros identico al JDK**. Las 4 clases preexistentes
eran esqueletos (`TypeElement` tenia 2 de 11 miembros; `ElementKind`, 13 de 21 constantes).

#### Dos notas de infraestructura

- **El desensamblador propio no imprime `transient` ni `volatile`**, asi que no puede detectar #115
  por si solo. Para auditar modificadores de campo hay que usar el `javap` del JDK apuntado al
  `.class` por ruta. **Y oculta los campos privados sin `-p`.**
- **`bin/FROZEN.md` tiene una invariante que hoy no se sostiene.** Recompilar `Name.java` **sin
  tocarla** da 570 bytes contra los 198 commiteados: el javac actual pre-siembra ~21 `Utf8` de
  nombres de atributo en el pool que el javac viejo no ponia. Semanticamente identico, pero
  "un `.class` distinto significa que la fuente cambio" **es falso** para todo lo compilado antes de
  este snapshot, y una recompilacion masiva va a mover todos esos archivos.

---

## La regla: el contrato es publico + protected. Lo demas es libre

Decision de proyecto, explicitada el 2026-08-22 (venia siendo la practica de hecho, pero no estaba
escrita y varias sesiones la redescubrieron por separado).

**Lo que hay que respetar** es lo que el codigo de un usuario puede observar:

- miembros `public` y `protected` (estos ultimos son contrato para quien herede),
- nombres y jerarquia de las clases **publicas**,
- descriptores y modificadores semanticos (`static`, `final`, `abstract`, `synchronized`, `volatile`,
  `varargs`) de todo lo anterior.

**Lo que es libre** es todo lo que un usuario no puede alcanzar: clases package-private, miembros
privados y package-private, la estructura interna, los nombres de los helpers, y los algoritmos.
Nuestra implementacion **no tiene por que parecerse** a la de HotSpot — y de hecho no puede, porque
no hay `Unsafe`, `MemorySegment` ni `jdk.internal.*`.

Bordes que **si** son observables aunque parezcan internos, para no pasarse de largo: la forma de
serializacion (`serialVersionUID`, que campos son `transient`), los nombres de clase que se filtran
por `getClass().getName()` y por las trazas, y **anidada vs top-level**, que cambia el nombre binario.

### Consecuencia 1 — dos findings de esta barrida quedan RETIRADOS

- **#206 — RETIRADO.** Listaba 11 clases "izadas a top-level" como defecto de biblioteca. **Las once
  son package-private** (`abstract class Node`, `final class TzData`, `final class ZoneMath`,
  `class DescNames`, `final class NormImpl`, `final class NormTables`, `final class HijrahTable`,
  `class ReentrantCondition`, `class WriteCondition`, `final class ConstantMethodHandleDesc`,
  `final class Bits`). Ninguna es alcanzable desde afuera del paquete, asi que son exactamente tan
  invisibles como la clase anidada equivalente del JDK: **son decisiones de implementacion legitimas,
  no un defecto.**
- **#207 — RETIRADO.** `TreeMap.checkInvariants()` y `Vector.countUnlocked()` se llamaron "hooks de
  test en la superficie de la biblioteca". **Los dos son package-private** (`int checkInvariants()`,
  `int countUnlocked()`), o sea que no son superficie: son internos, y por lo tanto libres.

En los dos casos se dedujo "es API" de la salida de una herramienta sin comprobar la visibilidad en
la fuente. **Es la tercera vez en esta barrida** (la primera fue `java.lang.invoke.Lookup`, que
resulto ser anidada). Regla para el proximo: **antes de asentar un finding de superficie, mirar el
modificador en el `.java`.**

### Consecuencia 2 — el gate mide contra el denominador equivocado

`apidiff.py` venia reportando cobertura sobre **todos** los miembros del JDK, privados de HotSpot
incluidos — miembros que por esta misma regla **nunca** deberiamos escribir:

| Denominador | Miembros | Faltan | Cobertura |
|---|---:|---:|---:|
| Todos los miembros del JDK (lo que se reportaba) | 13410 | 8216 | **38.7 %** |
| **Solo el contrato (public + protected)** | ~9274 | 4080 | **56.0 %** |

Los ~4136 de diferencia son privados y package-private de HotSpot: `Unsafe`, `MemorySegment`,
`$assertionsDisabled`, los `static {}` de `assert`, y la maquinaria de pipelines. `apidiff.py` pasa a
reportar el contrato como titular.

### Novena tanda: `java.lang.invoke` terminado (2026-08-22)

Resultado: **77,7 % → 97,6 %** de miembros pub/prot por descriptor (24 tipos, anidados incluidos),
**66 miembros cerrados**, y las divergencias `+native` de 5 a **0**. Todos los tipos al 100 % salvo
`MethodType` (33/41), bloqueado por cosas fuera del paquete.

#### El rodeo para tipos anidados: escribir el NOMBRE BINARIO

Verificado, y desbloquea los 14 miembros que #208 daba por perdidos:

| Escritura en la fuente | Descriptor emitido |
|---|---|
| `Outer.Inner` / `p.Outer.Inner` | `error: no se encuentra el simbolo` (#101) |
| `import p.Outer.Inner;` + `Inner` | `Ljava/lang/Object;` + `Signature: LInner;` (#208) |
| `import p.Outer.*;` + `Inner` | `Ljava/lang/Object;` **y sin `Signature`** — ver #245 |
| **`Outer$Inner`** (nombre binario) | **`Lp/Outer$Inner;`** — exacto, y `Signature` tambien |

> **Al dia de hoy (2026-08-25) esta tabla es historica: las cuatro formas emiten el descriptor
> exacto.** Las dos filas del medio cayeron con #239/#245 y la primera con #101. El rodeo del nombre
> binario ya no hace falta.

**No es una invencion nuestra:** el `javac` del JDK 25 hace lo mismo cuando la clase esta en el
classpath y no en la misma invocacion — y el build de KajiLibrary es exactamente ese caso
(`--emit -cp KajiLibrary`, un archivo por vez, que ademas es lo unico que permite #240).
Queda documentado en `MethodHandles.java` con la instruccion de sacarlo cuando se arregle #101.
**Prueba de que es un arreglo real y no un truco de metrica:** el `BootstrapMethods` de una lambda
emite `metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;…)`; antes la declaracion decia
`(Ljava/lang/Object;…)` y el call site apuntaba a un metodo que la clase no tenia. Ahora coinciden.

#### Defectos nuevos

#### Nota de tooling — `apidiff.py` subestima

Compara el **texto** de `javap`, que incluye el atributo `Signature`. Un miembro con el descriptor
**exacto** al del JDK cuenta como faltante si el JDK imprime `T`/`F`/`M` y nosotros el bound: 11 casos
solo en este paquete, todos consecuencia de la convencion "declarar raw para que el binario sea fiel"
que la propia biblioteca eligio por #100. Por eso las dos metricas del informe difieren (97,6 % por
descriptor vs 94,0 % por texto). **Vale versionar un comparador por descriptor**, que ademas valida
que el `javap` real pueda leer nuestros class files.

### Decima tanda: `java.util.stream` terminado (2026-08-22)

Contrato **51,2 % → 81,2 %** (+79 miembros), `Collectors` de 4 a **37 factories publicas**,
modificadores divergentes **9 → 0**, 39 asserts de runtime en verde. `StreamSupport` confirmado
**inviable**: `java.util.Spliterator` no existe y los cinco metodos publicos lo toman por parametro.

- **#246 — 🔴 HAY DOS BIBLIOTECAS ESTANDAR DIVERGENTES, Y LA QUE SE DESARROLLA NO ES LA QUE CORRE.**
  Es el hallazgo estructural de toda la barrida; venia apareciendo de a pedazos.

  > **Barrido sistematico (2026-08-24): la divergencia, medida entera.**
  >
  > Hasta aca #246 se apoyaba en tres divergencias halladas de casualidad. Se midio clase por clase
  > sobre las **122** que la VM carga: **93 miembros `public`/`protected` que `boot/` tiene y
  > `KajiLibrary/` no, en 40 clases**, mas **34 clases que solo existen en `boot/`** (`ThreadGroup`,
  > `Runtime`, `AbstractQueuedSynchronizer`, `LockSupport`, los cinco `*Cache` de los wrappers). De
  > las 88 compartidas, **27 tienen superficie identica y 59 divergen**.
  >
  > | Clase | le faltan a KajiLibrary |
  > |---|---|
  > | `java.lang.Thread` | **15** — `isDaemon()`, `getPriority()`, el `ThreadGroup`… |
  > | `CompletableFuture` | 5 — entre ellos `thenCombine` |
  > | `ScheduledThreadPoolExecutor` / `java.lang.Class` | 4 cada uno |
  > | `Object` / `System` | 3 cada uno — `clone()`, `nanoTime()`… |
  > | **los 6 wrappers** (`Boolean` `Byte` `Character` `Integer` `Long` `Short`) | **3 cada uno: `equals`, `hashCode`, `TYPE`** |
  > | `Float` / `Double` | 1 cada uno: `TYPE` |
  >
  > El patron de los wrappers confirma al pie lo que decia el reporte: el hito **A7 #5** se implemento
  > en `boot/` y **KajiLibrary nunca lo recibio** — los seis, con el mismo hueco. Y en
  > `java.util.concurrent` el grueso de lo que falta son los metodos **bloqueantes** (`take()`,
  > `put()`, `await()`, `acquire()`): la version de `boot/` los tiene, la de KajiLibrary no.
  >
  > El resto de las diferencias son casi todas **privadas** (los locks de `LinkedBlockingQueue`, el
  > heap de `PriorityBlockingQueue`): ahi no hay contrato incumplido, son dos implementaciones
  > distintas. Por eso la cifra que vale es la de **miembros publicos: 93**, no el total.
  >
  > **⚠ Trampa de medicion — leer antes de auditar `java.*` con `javap`.** El primer barrido de esta
  > sesion uso `javap -p -cp <dir> <FQN>` y dio un resultado **falso**: 87 de 88 clases "identicas".
  > Para un nombre `java.*`, `javap` resuelve desde el modulo `java.base` **del JDK que lo ejecuta** y
  > no mira el `-cp` — estaba comparando el `Integer` de Temurin contra si mismo. Lo delato que el
  > hash daba **0 clases byte-identicas** mientras la superficie daba "casi todas iguales". Hay que
  > pasarle **la ruta del archivo** (`javap -p boot/java/lang/Integer.class`). El modo de fallo es
  > silencioso y *favorable*: mide el JDK real y da todo bien.
  >
  > `tools/apidiff/apidiff.py` **esta bien** en este punto (se reviso): a nuestro lado le pasa la
  > **ruta** (`jvm.exe --javap -p <path>`) y al lado de referencia el FQN, que ahi es justamente lo
  > que corresponde porque el oraculo **debe** salir de `java.base`. Ojo igual con que responde otra
  > pregunta: apidiff compara *lo nuestro contra el JDK real*, no `boot/` contra `KajiLibrary/` — para
  > eso hace falta un comparador entre los dos arboles propios, que es el que se uso aca.

  La VM carga **`boot/`**. `KajiLibrary/` se compila, se mide y se audita, pero **nunca se ejecuta**
  — `run-headless` bootea contra `boot/` y cargar una clase de KajiLibrary falla. Las dos copias se
  fueron separando y ya hay tres divergencias verificadas:

  | Clase | `boot/` | `KajiLibrary/` | Sintoma |
  |---|---|---|---|
  | `Throwable` | declara `backtrace` | **no lo declara** | toda excepcion no capturada panica (#227) |
  | `Integer` | tiene `equals`/`hashCode` + cache | **cero `equals`/`hashCode`** | `HashMap` con claves `Integer` nunca encuentra nada |
  | `java.lang.invoke.*` | juego minimo que interpreta la VM | modelo completo | son formas distintas a proposito |

  Verificado: `grep -cE "public (boolean equals|int hashCode)" KajiLibrary/java/lang/Integer.java`
  → **0**; el `javap` sobre `boot/java/lang/Integer.class` → **3**. El hito A7 #5 del roadmap
  ("los 8 wrappers reales… `equals`/`hashCode`") se implemento en `boot/` y **KajiLibrary nunca lo
  recibio**.
  **Consecuencia medible hoy:** `Collectors.groupingBy`/`toMap` con clasificador boxeado a `Integer`
  compilan, corren y devuelven **un grupo por elemento**; con claves `String` andan perfecto.
  Mientras esto siga asi, "la biblioteca pasa el gate" y "la biblioteca funciona" son afirmaciones
  sobre **dos artefactos distintos**. Es la Fase E del roadmap: cerrar el circulo.

- **#247 — 🔴 toda llamada a un metodo de una interfaz ANIDADA cargada del classpath se descarta en
  silencio.**
  ```java
  IntStream.Builder b = IntStream.builder();
  b.accept(1);              // emite:  iconst_1; pop
  b.add(2);                 // emite:  iconst_2; pop
  IntStream s = b.build();  // no emite nada: s = b
  ```
  Despues revienta con *operand stack underflow*. El tipo anidado **si** resuelve como tipo; lo que
  falla es el **despacho**. Por eso `Stream.builder()`/`IntStream.builder()` quedan declarados e
  inertes. Misma familia que #239/#245, pero en posicion de llamada.

- **#248 — 🔴 una llamada generica estatica se descarta en silencio cuando la inferencia tiene que
  bajar a una variable de tipo anidada en un argumento de tipo.** Con el parametro declarado
  **invariante** (`Function<T, Stream<U>>`) el call site compila **sin `invokestatic`**:
  ```
  15: aload_1     // mapper — queda colgado en la pila
  16: aload_0     // downstream
  17: astore_2    // c = downstream   (!!)
  ```
  El llamador recibe el argumento equivocado, sin diagnostico. Declararlo como el JDK
  (`Function<T, ? extends Stream<? extends U>>`) lo hace resolver. **Sigue roto con comodines** en
  `Stream.mapMulti(BiConsumer<? super T, ? super Consumer<R>>)`.

#### Re-atribuciones (no son defectos nuevos)

- **`Boolean.TRUE`/`FALSE` panican → es #110.** Verificado: emite
  `getfield // Field java/lang/Boolean.TRUE:...` — `getfield` sobre un **static**, con la pila vacia.
  No es especifico de `Boolean` ni un defecto de VM aparte: es el `ACC_STATIC` del classpath que se
  pierde. Rompia `minBy`/`maxBy`/`reducing`/`partitioningBy`.
- **La inferencia que falla desde un contexto generico → #204/#215.** Confirmado el workaround:
  **argumentos de tipo explicitos** (`P1.<A>id(b)`) resuelven.
- **#12 ya NO reproduce.** Un `enum` anidado en una interfaz compila correcto hoy (constantes,
  `$VALUES`, `values()`, `valueOf`, `<clinit>`). Lo que bloquea `Collector.characteristics()` es otra
  cosa: la **identidad del tipo anidado no sobrevive al `import`** en otra unidad — el chequeo de
  override rechaza con "`Set` no es un subtipo de `Set`".
- **#226 confirmado sobre esta API:** los tres `joining()` compilan con logica correcta y panican en
  runtime por `String.valueOf([CII)`.
  **Corregido despues (2026-08-24):** el nativo esta y funciona; lo que faltaba era compilar con
  `-cp KajiLibrary` (en `boot/` solo existe `valueOf(Object)`). Ver #226 en *Fixed*.

#### Revision con el snapshot congelado de 2026-08-24

Todo lo de abajo se comprobo con `bin/` reconstruido desde `9642607` y, donde dice que algo corre,
ejecutandolo con `bin/run-headless.exe`. Los estados del documento venian atrasados respecto del
codigo: varios marcados abiertos ya estaban arreglados, y uno marcado arreglado no lo estaba.

- **#250 — ✅ ARREGLADO Y MEDIDO (2026-08-24).** `desugar.rs::enum_members`, item 6: el `valueOf(String)`
  sintetizado volvio a ser **autocontenido** —`if ($n.equals("ROJO")) return ROJO; … throw new
  IllegalArgumentException($n);`— en vez de delegar en `Enum.valueOf(Class, String)`.
  - **Efecto medido** con `tools/apidiff/recompile.py` sobre las 942 fuentes: **de 78 sin `.class` a
    9**, o sea **69 fuentes recuperadas**. Las 9 restantes son otros findings ya conocidos
    (`ClassLoader`/`putAll`/`stream` → #205; inferencia → #204/#215; un `new` sin resolver).
  - **Por que el desvio se queda:** `values()` si se pudo alinear con javac (`$VALUES.clone()`, que
    el emisor y la VM soportan); `valueOf` no, porque la version real va por
    `Class.enumConstantDirectory()` — reflexion sobre el `$VALUES` de otra clase. La ausencia de
    `Enum.valueOf(Class,String)` en KajiLibrary es **intencional** y esta documentada en su
    `Enum.java`; el arreglo respeta esa decision en vez de taparla con un metodo que no podria andar.
  - **El comentario de la propia funcion ya describia la version correcta** (decia "compara contra
    los nombres literales") mientras el codigo delegaba: la contradiccion entre codigo y doc es lo
    que delato la regresion. Ambos quedaron alineados.
  - **Dos detalles de la forma restaurada que NO conviene "mejorar"** (se probaron los dos y rompen):
    el receptor del `equals` es el **literal** (`"ROJO".equals($n)`, no al reves), y el
    `IllegalArgumentException` va **sin argumentos**. Pasarle `$n` al constructor hace que el
    **verificador estricto rechace el metodo** (`<init> receiver Reference("java/lang/String") is
    not an uninitialized object`) cuando el tipo no resuelve en el classpath de quien compila —el
    caso de los tests con classpath minimo—, y tira abajo
    `an_enum_passes_the_strict_verifier` y `an_enum_with_a_string_constant_argument_verifies`.
  - **El test `an_enum_valueof_delegates_to_enum_valueof` afirmaba la regresion** y se reemplazo por
    `an_enum_valueof_is_self_contained`, con el criterio invertido: que **no** haya `Enum.valueOf` en
    los method refs, que si haya `String.equals`, y que el pool mencione `IllegalArgumentException`.
    Suite: **1244 pasan / 16 fallan**, identico a la linea base previa al cambio.
  - Nota de proceso que el reporte original ya marcaba y conviene repetir: **el gate compara `.class`
    contra `.class`**, y los versionados los produjo un compilador anterior, asi que una regresion de
    este tipo no se ve hasta que alguien recompila. Vale correr `recompile.py` en cada refresco del
    snapshot congelado. Texto original abajo.

  **REGRESION: el desugar de un `enum` llama a `Enum.valueOf(Class, String)`, que la
  biblioteca no declara.** Cualquier archivo con un `enum` deja de compilar con *"no se encuentra
  el metodo: valueOf — simbolo: valueOf(Class, String)"*. No hace falta usar el enum ni llamar a
  `valueOf` en la fuente: la referencia la trae el metodo sintetizado.
  - **Alcance medido, no estimado:** recompilando las 941 fuentes de KajiLibrary con
    `tools/apidiff/recompile.py`, **78 no producen `.class`, y 70 de esas 78 son este bug**. Toca
    `jakarta.persistence` (20), `java.time.chrono` (5), `java.time.format` (4),
    `javax.lang.model.element` (4), `javax.tools` (4), `java.lang.invoke` (2),
    `java.lang.annotation` (2) y otros.
  - **`Enum.valueOf(Class, String)` esta ausente A PROPOSITO**, y `java/lang/Enum.java` lo dice en
    un comentario: el real reflexiona sobre el `$VALUES` de la clase, cosa que no tenemos, y por eso
    el compilador venia sintetizando un `valueOf(String)` **autocontenido** dentro de cada enum.
    Esa era la decision correcta; la regresion es haberla cambiado por una delegacion.
  - **El arreglo va del lado del compilador.** Agregar un `Enum.valueOf(Class, String)` de mentira
    taparia el sintoma con un metodo que no puede funcionar sin reflexion.
  - **Como se colo:** el gate compara `.class` contra `.class`, y los `.class` versionados los
    produjo un compilador anterior. Nada los revalida contra la fuente hasta que alguien recompila.
    Conviene correr `recompile.py` en cada refresco del snapshot congelado.
  - Repro: `repros/finding_250.java`.

- **#251 — ✅ ARREGLADO (2026-08-24), y se llevo puestos a #247 y #108.** Causa raiz en
  `enter.rs::build_external`: cargaba transitivamente la **jerarquia** y los **tipos anidados** de un
  externo, pero **no los tipos que aparecen en las firmas de sus miembros**. Por eso el defecto
  parecia caprichoso — dependia de si el archivo **escribia** el tipo en algun lado. Si la unidad no
  menciona `Stream`, nadie lo carga, el receptor de `.count()` queda sin resolver y la llamada se
  descarta. Ahora `build_external` tambien carga los tipos de los parametros y retornos de sus
  miembros, con el mismo patron que ya usaba para los supertipos (los misses los memoiza el finder,
  #19, asi que un nombre ausente se busca una sola vez).
  - Verificado: `repros/finding_251.java` emite la cadena completa
    (`invokestatic all; invokeinterface count; l2i; ireturn`) y **corre dando 12**, igual que su
    contraprueba `finding_251b`. Compilacion en 190 ms — sin el efecto #19.
  - **#247 cae con el mismo fix** (`repros/finding_247.java`, nuevo): `IntStream.builder()` devuelve
    `IntStream$Builder`, un tipo que la fuente nunca nombra. Ahora emite `invokestatic builder`, dos
    `invokeinterface accept` y `invokeinterface build`, donde antes salia `iconst_1; pop`. **Ojo:** el
    repro todavia no *corre* — la propia `IntStream.java` de KajiLibrary dice que dejo `builder()`
    "declarado e inerte" **por este bug**, asi que el fix desbloquea ese trabajo de biblioteca en vez
    de completarlo.
  - **#108 tambien** (`repros/finding_108.java`): `iface().act()` emite `invokevirtual iface()` +
    `invokeinterface Iface.act()`, sin el `pop` suelto.
  - Suite: **1244 pasan / 16 fallan**, identico a la linea base.

  > **Defecto estructural que este fix destapo — el espacio de externos era PLANO por nombre
  > simple.** (Sin numero propio a proposito: la sesion paralela esta usando el rango 2xx en
  > simultaneo y ya hubo dos numeros duplicados por reclamarlos sin mirar. Asignarle uno al integrar.)
  >
  > Los tipos del classpath se registraban **solo** por nombre simple, asi que dos clases homonimas de
  > paquetes distintos no podian coexistir: la primera se quedaba con la clave y la segunda **no se
  > podia cargar ni nombrar**. Estaba ahi desde siempre; cargar mas tipos (por las firmas) lo hizo
  > visible. Se manifesto con los dos `TemporalField` —`java.time.temporal` y
  > `jakarta.persistence.criteria`— y el chequeo de abstractos reclamandole a `LocalTimeField` el
  > `getFrom` de la interfaz que no era.
  >
  > **Arreglado**: `SymbolTable` ahora indexa los externos tambien por **nombre completo**
  > (`external_fqn`), y las tres decisiones de "¿lo cargo?" —supertipos, anidados y la fase 2 de
  > firmas— preguntan por FQN en vez de por nombre simple. Un nombre cualificado resuelve **al suyo**.
  > El indice por nombre simple se conserva con "gana el primero", que es lo correcto: desambiguar una
  > referencia **simple** necesita el paquete y los imports, y eso lo tiene `try_load`, no la tabla —
  > por eso ademas `load_externals` carga en dos fases, primero lo que la unidad nombra.
  >
  > Verificado con un A/B: dos `Campo` homonimos (en `uno` y en `dos`), y `Uso implements
  > dos.Campo<Integer>` compila sin que le reclamen el metodo del `uno.Campo`.

  > **Efecto acumulado del dia sobre la biblioteca**, medido con `tools/apidiff/recompile.py`:
  > de **78 fuentes sin `.class` a 9** (946 de 955 compilan; el total crecio porque la sesion paralela
  > sumo fuentes). Las 9 que quedan son findings ya conocidos: `putAll`/`stream`/`ClassLoader` (#205),
  > la inferencia de `swap`/`empty` (#204/#215), un `new` sin resolver (familia #20), y dos de
  > biblioteca.

  Texto original abajo.

  **Una llamada encadenada sobre un tipo que el archivo no NOMBRA se descarta EN
  SILENCIO.** `return (int) RandomGeneratorFactory.all().count();` — `all()` devuelve un `Stream`,
  y si el archivo no menciona a `Stream` en ningun lado, el compilador no resuelve el `count()` y
  **se come la expresion entera**: `invokestatic all; invokeinterface count; l2i` desaparece y el
  metodo sale con `stack=0` y un `ireturn` solo. Sin error, sin aviso.
  - **Contraprueba exacta:** el mismo cuerpo, mismo `-cp`, en un archivo que nombra a `Stream` en
    OTRO metodo, compila bien y devuelve 12. No hace falta llamar a ese otro metodo — alcanza con
    que el tipo aparezca escrito. Repros: `repros/finding_251.java` (roto) y
    `repros/finding_251b.java` (sano).
  - **En Java valido no se importa lo que no se escribe.** Encadenar sobre el retorno de un metodo
    es la forma normal de usar una API fluida, y jamas obliga a importar el tipo intermedio: la
    fuente correcta es la que falla.
  - **El gate NO puede verlo**: el descriptor queda perfecto (`()I`). El metodo miente sobre lo que
    hace y revienta recien al ejecutar, con un `operand stack underflow` que no apunta a nada.
  - Familia de #127 y #128(b) — silencio en vez de error. Este es el mas caro de los tres, porque
    el silencio produce codigo que ejecuta.

- **#101 — ✅ ARREGLADO (verificado 2026-08-24).** Un tipo anidado nombrado como `Outer.Nested`
  resuelve y el descriptor sale bien: `Map.Entry<K,V> entry()` emite `()Ljava/util/Map$Entry;`, y
  `Normalizer.Form` se puede nombrar desde otro archivo — calificado o importado — cosa que dejaba
  a `java.text.Normalizer` con su API publica inalcanzable. Consecuencia: `MapJoin.entry()` vuelve.

- **#116 — ✅ ARREGLADO (verificado 2026-08-24).** `java.util.stream.Stream.of` ya se emite
  `public static`, con el `public` implicito que le toca a un miembro de interfaz. Se destrabo
  `java.util.random.RandomGeneratorFactory.all()`, omitido justamente porque no se lo podia llamar:
  vuelve a la biblioteca, emite `()Ljava/util/stream/Stream;` igual que el JDK y, corriendo sobre
  nuestra VM, devuelve las 12 entradas del registro.

- **#123 — ⬜ SIGUE ABIERTO, y precisado: lo que rompe es el `extends` PARAMETRIZADO.** Con la
  misma forma pero jerarquia NO generica (`interface CB extends CA`, `CBase.m()` devuelve `CA`,
  `CSub.m()` devuelve `CB`, los tres en el classpath) el override **se acepta**. Con
  `GB<X,E> extends GA<X,Set<E>,E>` se rechaza. O sea que la cadena de supertipos SI se consulta; lo
  que se pierde es la relacion cuando la clausula `extends` lleva argumentos de tipo — que es la
  forma exacta de `SetAttribute<X,E> extends PluralAttribute<X,Set<E>,E>`. Los cuatro `getModel()`
  siguen omitidos. Repro minimo: `repros/finding_123_GA.java`, `_GB`, `_GBase`, `_GSub`.

- **#124 — ⬜ SIGUE ABIERTO (revisado 2026-08-24).** Un campo inicializado en una interfaz todavia
  sintetiza `public P124()` sobre la interfaz, con los inicializadores adentro, y los campos salen
  con `flags: (0x0000)` — sin el `public static final` implicito de JLS §9.3. Un `getstatic` sobre
  ellos revienta con `operand stack underflow`. `CharacterIterator.DONE` sigue omitido.
  Ojo al revisarlo: el listado breve de `jvm.exe --javap` **no muestra** el constructor
  sintetizado; hay que mirar con `-v`.

- **#228 — ⬜ SIGUE ABIERTO (revisado 2026-08-24).** El escape del rango sustituto se sigue
  rechazando (`'\ud800'`, `"\ud834\udd60"`). Los del BMP compilan en ambos tipos de
  literal. Sin cambios respecto de #128(a).

---

## Tanda 2026-08-24 (b) — cerrando `java.util.concurrent`: por que el mapa no corria

Las dos entradas de abajo salieron de una sola pregunta: `ConcurrentSkipListMap` daba **0 fallos**
contra el JDK con la version portada a Java real, pero sobre NUESTRA VM tres de siete grupos de
prueba panicaban. Como la logica estaba probada, el defecto tenia que estar en la cadena de
herramientas — y estaba, en dos lugares distintos. Con las dos rodeadas, el mapa **corre entero
sobre nuestra VM, 0 fallos en los siete grupos**, y `ConcurrentSkipListSet` en los cinco suyos.

- **#101(import) — ⬜ SIGUE ABIERTO, y es MUCHO peor de lo que decia la ficha.** La forma
  calificada `Outer.Nested` quedo arreglada (ver la tanda anterior), pero la del **import de un tipo
  anidado** (`import java.util.Map.Entry;` + usar `Entry`) sigue rota, y lo que estaba anotado como
  "diverge el descriptor, se allowlistea" resulta ser **codegen incorrecto que produce una clase
  imposible de ejecutar**. Con `import java.util.Map.Entry;`:

  ```
  public static int usaImport(Entry<String, String>);
    descriptor: (Ljava/lang/Object;)I      // deberia ser (Ljava/util/Map$Entry;)I
    stack=0
       0: areturn                           // el cuerpo era `return e.getKey().length();`
    Signature: (LEntry<...>;)I              // nombre sin calificar: invalido
  ```

  El control con `Map.Entry` escrito entero emite el descriptor bien Y el cuerpo bien. O sea que el
  tipo importado no se registra: queda sin resolver, se borra a `Object`, y **toda llamada a traves
  de un valor de ese tipo se descarta** (#251). Tres sintomas, una causa.
  - Repro: la primera version de `repros/finding_252.java` mostraba las cuatro combinaciones; la
    forma corta es declarar `static int f(Entry<String,String> e) { return e.getKey().length(); }`
    junto a la misma con `Map.Entry`.
  - **Rodeo en la biblioteca, ya aplicado:** escribir `Map.Entry` calificado y borrar el import.
    Seis archivos: `java/util/SequencedMap`, `NavigableMap`, `AbstractMap`, `LinkedHashMap`,
    `java/util/concurrent/ConcurrentSkipListMap` y `ConcurrentSkipListSet`. Los descriptores pasan de
    `()Ljava/lang/Object;` a `()Ljava/util/Map$Entry;` — que es lo que emite el JDK — asi que la
    correccion es una mejora del gate, no solo del runtime.
  - **Lo que esto dice del metodo:** la ficha vieja de #101 media el dano con el gate, que solo ve
    firmas, y por eso lo clasifico como divergencia cosmetica allowlisteable. El dano real estaba en
    los cuerpos, donde el gate no mira. Un finding cuyo alcance se estimo con el gate hay que
    re-medirlo ejecutando.
  - Quedan `DirectMethodHandleDesc.Kind` (x2), `ModuleElement.Directive`, `Path.Node` y
    `Attribute.PersistentAttributeType` con la forma rota; se arreglan igual, calificando.

---

## Tanda 2026-08-24 (c) — `SubmissionPublisher`: tres findings nuevos, uno de ellos grave

Escribir una clase que usa comodines en su API publica y `wait`/`notify` para la contrapresion
destapo tres defectos que las estructuras de datos anteriores no habian tocado. Con los tres
rodeados, `SubmissionPublisher` da **0 fallos en los seis grupos de prueba, tanto contra el JDK 25
como sobre nuestra VM** — la misma fuente corriendo en los dos lados.

- **#251 — confirmado de nuevo, y con una formulacion mas util.** `pub.consume(sink)` en un archivo
  que **no nombra a `CompletableFuture`** se compilo con descriptor de retorno `()Ljava/lang/Object;`
  y no resolvio en runtime. La regla practica que sale de las dos apariciones: **si el archivo no
  escribe el tipo, el compilador no lo resuelve** — y segun el caso descarta la expresion (#251
  original) o le pone `Object` en el descriptor del sitio de llamada (#102). Es un solo defecto con
  dos salidas.

- **#250 — ✅ CERRADO, y no era del compilador: faltaba `Enum.valueOf(Class, String)` en la
  BIBLIOTECA.** El compilador sintetiza el `valueOf(String)` de cada enum igual que el JDK, en una
  linea — `(E) Enum.valueOf(E.class, name)` — y nuestro `java.lang.Enum` no declaraba ese metodo.
  El comentario del archivo decia que estaba ausente "a proposito", porque el compilador generaba un
  `valueOf(String)` autocontenido; **eso dejo de ser cierto** y la ficha quedo atras. Sin ese metodo
  **ningun enum compilaba**, y el error apuntaba a la linea 0 del archivo, que es lo que lo hacia
  parecer un defecto de codegen.
  - **Medicion, antes y despues, con `tools/apidiff/recompile.py` sobre las 944 fuentes:**

    | | compilan | fallan |
    |---|---|---|
    | antes | 865 | **78** |
    | despues | 936 | **8** |

    **70 archivos destrabados con 25 lineas**, y ninguno roto. Los 8 que quedan son otra cosa
    (inferencia de tipos en `Collections`/`Optional`, y cuatro de `jakarta`).
  - **La implementacion es por reflexion**, como la del JDK: recorre los campos declarados y devuelve
    el que se llama como el nombre pedido. Lleva dos rodeos, cada uno comentado: `Object[]` en vez de
    `Field[]` para el retorno de `getDeclaredFields` (#102), y nada de cachear por `Class` porque
    `Enum` se carga demasiado temprano.
  - **Verificado corriendo**, no solo compilando: `TimeUnit.valueOf("SECONDS")` sobre nuestra VM
    devuelve la constante correcta con `ordinal() == 3`, `TimeUnit.values()` devuelve las 7, y un
    nombre inexistente tira `IllegalArgumentException`.
  - **Lo que sigue sin andar de un enum es #110**, no esto: leer `TimeUnit.MILLISECONDS` se compila
    como `getfield` sobre un campo estatico y revienta. `values()` y `valueOf` no tocan ese camino.
  - **Leccion, y es la misma de #101:** el error decia "no se encuentra el metodo: valueOf" con la
    ubicacion "clase Enum". Lo decia todo. Se archivo como defecto de compilador por la linea 0 del
    reporte, y costo 78 archivos hasta que alguien leyo el mensaje entero.

- **#253 — la forma que SI acepta.** Probadas cinco maneras de pasar un valor a un parametro
  `Handle<? extends A>`, una sola compila: pasarlo por un **local del supertipo CRUDO**.

  | forma | resultado |
  |---|---|
  | receptor crudo, argumento crudo | rechazado |
  | receptor crudo, argumento tipado | rechazado |
  | receptor tipado, argumento tipado | rechazado |
  | **receptor crudo, argumento por un local crudo** | **compila** |
  | receptor crudo, argumento casteado al comodin | rechazado |

  Volver crudo el **receptor** no alcanza: el compilador igual chequea contra la firma generica del
  metodo. Lo que hay que volver crudo es el **argumento**, y hay que hacerlo con una asignacion, no
  con un cast. `StructuredTaskScope` lo usa en las dos llamadas al joiner.

---

## Tanda 2026-08-24 (d) — cerrando clases de `java.lang`

- **#5 (cola) — ⬜ un LITERAL de String dentro de la fuente de `java.lang.String` tiene el tipo del
  String EXTERNO, no el de la clase que se esta compilando.** El sombreado de tipos-nucleo arreglo
  el caso del retorno heredado, pero no este:

  ```
  error: tipo de retorno incompatible          <- `return "";` en un metodo que devuelve String
  error: no se encuentra el metodo: repeat     <- `" ".repeat(n)`, porque el literal es el String
                                                  del classpath, con la API vieja
  error: no se encontro un metodo `concat(String)` aplicable
    (los argumentos no coinciden: String no se convierte a String)   <- los dos String
  ```

  El tercer mensaje es el que lo dice todo: "String no se convierte a String".
  - **Rodeo, ya aplicado:** un helper privado que lava el literal por `valueOf(Object)`, que si
    esta declarado en la clase y por lo tanto devuelve ESTE tipo:

    ```java
    private static String lit(Object text) {
        return String.valueOf(text);
    }
    ```

    Queda en la fuente de `java.lang.String` con su comentario, y se saca el dia que el sombreado
    cubra tambien los literales. Son 9 usos.
  - Aparecio escribiendo las ~50 incorporaciones de `String`: sin el helper, ninguna de las que
    devuelve `""` o encadena sobre un literal compila.

### Undecima tanda: lo que destapo hacer fallar fuerte al emisor (2026-08-24)

Al cerrar #261 el codegen dejo de emitir **nada** cuando una llamada no resolvia y paso a fallar con
un diagnostico. Eso convirtio once miscompilaciones mudas en errores visibles. De las once, ocho
eran `System.arraycopy` (la propia #261). Las otras tres son findings nuevos, y una **no** es del
compilador.

#### Confirmacion aguas abajo: #110 desbloquea `java.nio.charset`

El propio documento decia que `java.nio.charset` "corre el dia que #110 se arregle". Se arreglo, y
corre — pero **solo contra la biblioteca recompilada**:

```
run-headless Nio110.class run                          -> panic (objects_operations.rs:410)
run-headless Nio110.class run --boot <arbol recompilado> -> Some(Int(0))
```

`StandardCharsets.UTF_8` se lee, `Charset.name()` devuelve `"UTF-8"`. La diferencia entre las dos
lineas es **entera** de los `.class` versionados: los del repo los emitio el javac viejo, con
`getfield` donde va `getstatic`. Refuerza lo que ya dice la seccion "los `.class` versionados NO
son lo que emite el javac de hoy": **hace falta una regeneracion del arbol**, y hasta que pase, una
prueba en runtime contra `KajiLibrary/` mide el compilador de ayer.

## Tanda 2026-08-24 (e) -- #118 cerrado, y por que #110 "volvia"

- **#118 -- ✅ ARREGLADO. Faltaba solo la mitad de ESCRITURA: `ACC_VARARGS` no se emitia.**

  La mitad de lectura ya estaba entera y funcionando: `classfile.rs` leia el flag hacia
  `ExtMethod.varargs`, y `enter.rs` lo pasaba al ultimo `ParamSig`. Lo que no habia era nada que
  lo ESCRIBIERA, asi que ningun `.class` nuestro lo llevaba y la lectura no tenia que leer.

  El arreglo son tres lineas en `codegen.rs`: la constante `ACC_VARARGS` (0x0080) y un
  `if is_varargs(m)` en los **dos** caminos de emision de metodo (con cuerpo y sin cuerpo -- el
  segundo importa porque cubre los `abstract` y los `native`).

  Verificado de punta a punta contra el JDK 25, con el varargs en **otra unidad de compilacion**,
  que es el caso que fallaba:

  | forma | antes | ahora |
  |---|---|---|
  | `join("-", "a", "b", "c")` desplegada | se emitia **nada** | `anewarray` + call, da `a-b-c` |
  | `join("-")` sin variables | se emitia **nada** | arreglo vacio, da `""` |
  | `join("-", parts)` con el arreglo | ya andaba | sigue andando |

  **Efecto inmediato en la biblioteca:** `java.lang.String` pasa de 91/103 a 95/103 sin tocar una
  linea de su fuente -- sus cuatro varargs (`format` x2, `join`, `formatted`) ya estaban escritos
  con `...` y solo les faltaba el flag.

- **#265-b -- ✅ RESUELTO: una asignacion grande hace leer un `u32` pasando el fin del heap.**
  Se renumera a `-b` porque **#265** quedo tomado por otro defecto de VM (la resolucion que no
  sube por la jerarquia), y el propio documento ya lo llamaba asi mas abajo. **No era la
  asignacion ni el tamano: era el ancho del elemento en `System.arraycopy`** -- ver "la leccion
  del ancho del elemento" mas abajo, donde esta el analisis y la medicion contra el JDK 25.
  Queda el reporte original tal como se escribio:

  ```
  panicked at src\jvm\interpreter\heap.rs:752:51:
  range end index 55815 out of range for slice of length 55811
  ```

  Cuatro bytes exactos pasado el final: es la lectura de **una palabra de cabecera** sobre un
  objeto cuyo offset cae justo en el limite. No depende del contenido.

  - **Es PREEXISTENTE**, y se verifico en vez de suponerlo: guardando con `git stash` los cambios
    de VM de la sesion en curso y reconstruyendo, panica igual y con los **mismos dos numeros**.
  - **Por que aparece recien ahora:** hasta el fix de #261, `StringBuilder.ensureCapacity` llamaba
    a `System.arraycopy`, la llamada se emitia como nada y el buffer no crecia. Con la copia
    funcionando se asigna de verdad, y el heap llega al limite que destapa esto.
  - Repro: `KajiLibrary/repros/finding_265.java` (el JDK 25 da 0).

### La leccion que costo media sesion: los `.class` del arbol estaban RANCIOS

#110 parecia haber **resucitado**: leer `StandardCharsets.UTF_8` desde otra unidad emitia
`getfield` sobre un static y la VM moria con `field_offset: field not found`. No habia resucitado
nada. El compilador esta bien -- las dos mitades del fix de #110 estan en el arbol
(`classfile.rs:121` lee `ACC_STATIC`, `enter.rs:605` lo pasa a los modificadores del simbolo) y
tambien en el `bin/javac.exe` congelado. Lo que estaba viejo era el **`.class` commiteado**,
emitido por un javac que todavia tiraba el flag al escribir.

Se comprobo asi: `git show <commit>~1:KajiLibrary/java/lang/String.class` daba **2937 bytes**
contra 32300 de la fuente de hoy. No era el mismo archivo ni de lejos.

**Recompilar arregla, y el diagnostico se repitio identico tres veces en una sesion:**

| sintoma | se leia como | era |
|---|---|---|
| `getfield` sobre un static | #110 resucitado | `.class` anterior al fix de #110 |
| `System.arraycopy` sin efecto | #258 | `.class` anterior al fix de #261 |
| `StringBuilder` que pierde todo al crecer | corrupcion de heap | lo mismo, via `arraycopy` |

El tercero es el que mas asusta: **cualquier `StringBuilder` que pasara de 16 caracteres perdia
todo lo anterior**, y con el se caia `CharBuffer.toString()` y todo lo que arme texto largo.

> El documento ya avisaba de esto en su propia seccion "los `.class` versionados NO son lo que
> emite el javac de hoy". El aviso estaba; lo que faltaba era el reflejo de **recompilar antes de
> diagnosticar**. Ante cualquier sintoma que huela a un finding ya cerrado, el primer movimiento
> es recompilar la clase involucrada y recien despues creerle al sintoma.

### Dos defectos de fidelidad de la VM, encontrados y arreglados

- **Un surrogate SUELTO no sobrevivia a `String.valueOf(char[],int,int)`.** El nativo armaba un
  `String` de **Rust** con `from_utf16_lossy` y recien despues lo internaba; un `String` de Rust
  es UTF-8 bien formado por construccion, asi que un `0xD800` sin par se volvia U+FFFD. Java si
  permite uno suelto. Medido: `charAt(0)` daba 65533 donde el JDK da 55296.
  Arreglado con `strings::intern_units`, que escribe las unidades UTF-16 **derecho**, sin paso de
  codificacion donde perderlas.

- **`String.hashCode` hasheaba los BYTES UTF-8 en vez de los code units.** `"n con tilde"`
  (U+00F1) daba **6222** donde el JDK da **241**. El efecto no es un hash distinto: es que **todo
  `HashMap` con claves no-ASCII estaba roto**, y en silencio. Se arreglo solo al bajar la costura:
  `hashCode` dejo de ser nativo y pasa a ser el bucle en Java que fija la especificacion, que es
  lo unico que puede computar.

  Esto es un argumento sobre **donde poner la costura**, no una anecdota. `equals`, `hashCode` y
  `startsWith` nunca necesitaron la VM -- son bucles sobre `charAt`. Estaban nativos, y por estarlo
  leian el almacenamiento en los terminos de la VM, que es donde se colo el error. Hoy
  `java.lang.String` tiene **cuatro** costuras y las cuatro son privadas: `rawLength`, `rawCharAt`
  y los dos `rawValueOf`. Toda su superficie publica es Java, igual que la del JDK -- que era
  ademas la unica forma de cerrar los siete miembros que diferian solo en el modificador `native`.

---

## Tanda 2026-08-24 (f) -- `StrictMath` completo, y un literal que no se podia escribir

### #267 -- el decimal `9223372036854775808L` se rechaza (y su hermano `2147483648`) [CERRADO]

**Sintoma.** Una tabla de patrones de bits generada del JDK no compilaba:

```java
xs[1] = -9223372036854775808L;   // error: literal long invalido
```

Ese numero no es exotico: es `Double.doubleToLongBits(-0.0)`, o sea `Long.MIN_VALUE`, que aparece
en cualquier tabla de referencia que incluya el cero negativo. La misma forma con `int` --
`-2147483648` -- pasaba **por accidente**: el decimal entraba como `i64` y el emisor lo truncaba
a 32 bits, y truncar `2147483648` da justo `Integer.MIN_VALUE`. Con `long` no hay ancho mayor
donde apoyarse, asi que ahi el parseo fallaba de verdad.

**Causa.** `parse_int_literal` mandaba el decimal a `str::parse::<i64>`, que rechaza `2^63` por
overflow -- correctamente, si uno mira el literal solo.

**Lo que dice la especificacion (JLS §3.10.1) y donde estaba el error de encuadre.** Mirar el
literal solo es precisamente el error. `9223372036854775808L` **no es** un literal legal por si
mismo: la gramatica lo admite **unicamente** como operando de un menos unario, y lo mismo vale
para `2147483648`. Son las magnitudes de los dos `MIN_VALUE`, que no tienen contraparte positiva,
y la especificacion resuelve eso dandoles un unico contexto legal en vez de un tipo.

**Arreglo** (`src/javac/parser.rs`): el menos unario delante de un literal entero **pliega el
signo** en `unary()`, antes de armar el `Unary`. Eso hace que el literal grande solo pueda
parsearse donde la gramatica lo permite -- no se lo acepta suelto -- y de paso el `Integer.MIN_VALUE`
deja de depender del truncado del emisor para salir bien. `parse_negated_int_literal` niega
envolviendo lo que ya se sabia parsear y agrega el unico caso nuevo: el decimal `2^63`.

**Estado:** cerrado en `src/javac`. **No** esta en el snapshot congelado de `bin/`.

### Un defecto de fidelidad de la biblioteca, encontrado por la tabla

**`Math.toRadians` y `Math.toDegrees` erraban por un ulp.** Estaban escritos como dicen los
libros y como los escribia el propio JDK hace diez versiones:

```java
return angdeg / 180.0d * Math.PI;      // dos redondeos
```

El JDK 25 multiplica **una vez** por una constante ya redondeada (`DEGREES_TO_RADIANS`,
`RADIANS_TO_DEGREES`). No son equivalentes: dos redondeos y uno solo discrepan en **5 de 24**
entradas de la tabla, siempre por un ulp.

Lo que vale la pena registrar no es el arreglo -- es **por que no lo habiamos visto**. El grupo
`angulos` de `MathTest` estaba en verde y lo sigue estando: prueba `toRadians(180) == PI`,
`toDegrees(PI) == 180`, `toRadians(360) == TAU`. Las tres pasan con **las dos** formulas, porque
son justamente los puntos donde coinciden. Una prueba escrita a partir de las identidades que uno
recuerda mide las identidades, no la implementacion; hizo falta una tabla de bits generada del
oraculo sobre entradas arbitrarias para que el ulp apareciera. Es el mismo argumento que ya
habiamos hecho para `sqrt` y `fma`, cobrado esta vez en un metodo que parecia demasiado simple
para necesitarlo.


---

## Tanda 2026-08-24 (g) -- `StringBuilder` y `StringBuffer` completos

### #269 -- 🔴 VM: `System.arraycopy` supone que todo elemento mide cuatro bytes [CERRADO]

**El peor de la sesion, y estuvo a la vista todo el tiempo.** El nativo lo decia en su propio
comentario:

```rust
// Bulk copy between arrays -- the memcpy the VM does for you. Assumes 4-byte
// elements (int/reference arrays); byte/char arrays would need their width.
```

Un `char[]` mide **dos** bytes por elemento. Copiar con paso de cuatro no escribe "casi bien":
lee y escribe el doble de rango, mete basura en el destino y **se sale del array por el final**.
De ahi salian los panicos

```
range end index 59550 out of range for slice of length 59546
```

que teniamos anotados como #265-b ("una asignacion grande hace leer un `u32` pasando el fin del
heap"). No era la asignacion ni el tamano: era **el ancho del elemento**, y el "grande" del
sintoma solo describia cuando el desborde caia fuera del ultimo objeto del heap en vez de encima
del siguiente. Cuando caia encima del siguiente no habia panico -- habia corrupcion silenciosa.

Y un segundo defecto en el mismo nativo, independiente: **no contemplaba el solapamiento**. El
contrato dice que se comporta como si el origen se copiara primero a un buffer aparte, asi que
con el mismo array y rangos que se pisan la direccion del recorrido decide el resultado. El
`delete` de un `StringBuilder` es exactamente ese caso.

Medido contra el JDK 25 con la misma sonda, antes y despues:

| caso | antes | JDK 25 | despues |
|---|---|---|---|
| `arraycopy(a,4,a,2,3)` sobre un `char[]` (solapado hacia abajo) | 99 `'c'` | 101 `'e'` | 101 |
| `arraycopy(a,0,a,2,3)` (solapado hacia arriba) | 99 `'c'` | 97 `'a'` | 97 |
| `arraycopy(a,4,b,2,3)` entre `char[]` distintos | 0 | 101 `'e'` | 101 |

Notar el tercero: **sin solapamiento tampoco copiaba**. Cualquier `char[]` que pasara por
`arraycopy` quedaba mal, siempre.

**Arreglo** (`src/jvm/interpreter/natives.rs`): el ancho sale de la CLASE del array
(`array_element_width` sobre `class_name_at_mirror`), la copia va byte a byte eligiendo la
direccion segun se solapen los rangos, y los arrays de **referencias** se copian por
`store_reference` para que el write barrier se entere -- cosa que el codigo viejo tampoco hacia.

**Lo que destapo:** `CharsetTest` pasa de panico a **0**, y con el las dos mitades de `SbTest`
que reventaban. `ParseTest.ida_y_vuelta` deja de panicar (queda lento, no roto).

**La leccion, que no es sobre arrays.** El comentario del nativo declaraba la limitacion en voz
alta y aun asi el defecto sobrevivio meses: un `TODO` escrito en el lugar correcto no es una
salvaguarda, porque nadie lee el cuerpo de un nativo que "anda". Lo que lo encontro fue una
prueba que comparaba UN valor concreto contra el JDK.

### #268 -- ⬜ javac no sintetiza los accesores de una superclase package-private

Cuando una clase **publica** hereda metodos publicos de una superclase **package-private**,
`javac` sintetiza en la subclase un metodo puente por cada uno. No es una optimizacion: un
llamador de otro paquete no puede **nombrar** la superclase, asi que el metodo resuelto no pasa
el chequeo de acceso (JVMS 5.4.4) y la llamada seria un `IllegalAccessError`.

Repro:

```java
package probe;
abstract class Base3 { public int len() { return 1; } public Base3 self() { return this; } }
public final class Sub3 extends Base3 { public Sub3 self() { return this; } }
```

| | `javac` del JDK 25 | el nuestro |
|---|---|---|
| `public Sub3 self()` | si | si |
| `public Base3 self()` (puente covariante) | si | **si** |
| `public int len()` (accesor de la superclase) | si | **no** |

Los puentes covariantes ya salen (#233 los cerro); lo que falta es la otra familia.

**Costo real, medido:** es la razon por la que `StringBuilder` y `StringBuffer` **no** estan
partidos como en el JDK. Alli la implementacion vive en un `AbstractStringBuilder`
package-private que las dos extienden, y son esos accesores los que hacen que
`sb.length()` sea invocable desde fuera de `java.lang`. Con nuestro javac esa forma dejaria
diecisiete metodos (`length`, `charAt`, `capacity`, `setLength`, `getChars`, los de code point,
`substring`, `chars`, ...) resolviendo a una clase que el llamador no puede ver.

Asi que la implementacion vive entera en `StringBuilder` y `StringBuffer` **compone** una y
reenvia bajo el candado. Se conserva lo que importaba -- una sola implementacion, imposible que
las dos clases discrepen -- y el costo cae en `StringBuffer`, que es la vieja. Cuando #268 este
cerrado, la forma del JDK queda disponible.

### Una nota de medicion: el gate cuenta 53 puentes sinteticos de 96

`javap` no distingue, y `apidiff` compara la salida de `javap`. Para `java.lang.StringBuilder`
eso da 96 miembros publicos, de los cuales **53 son `ACC_BRIDGE, ACC_SYNTHETIC`** -- puentes que
`javac` fabrica por la forma interna del JDK (los treinta que devuelven `AbstractStringBuilder`,
mas los diecisiete accesores de #268). Ninguno lo escribio una persona y ninguno es API que
alguien pueda nombrar.

Contra el denominador que si es API -- los 42 no sinteticos -- nuestro `StringBuilder` **no
tiene ninguno faltante**, y declara ademas los diecisiete que el JDK hereda. Lo mismo
`StringBuffer`: **0 faltantes** sobre 57.

Es el mismo argumento de la seccion "el gate mide contra el denominador equivocado", cobrado en
otra clase: **un miembro sintetico mide al compilador, no a la biblioteca**, y compararlo mezcla
las dos cosas.

### Y ocho metodos que faltaban en `Character`

`StringBuilder` necesita leer code points sobre su `char[]` con un limite explicito -- su buffer
es mas largo que su contenido, y un surrogate leido pasando el `count` es un caracter que el
llamador no tiene. Esas formas no existian, asi que se implementaron en vez de rodearlas:
`codePointAt(char[],int)` y `(char[],int,int)`, `codePointBefore(char[],int)` y
`(char[],int,int)`, `codePointCount(CharSequence,int,int)` y `(char[],int,int)`,
`offsetByCodePoints(CharSequence,int,int)` y `(char[],int,int,int,int)`. `Character` pasa de 68
a 76 miembros.


---

## Tanda 2026-08-24 (h) -- `java.lang.Class`: la capa de metadatos

### El punto de partida: cinco de ocho metodos eran nativos INEXISTENTES

`java.lang.Class` declaraba ocho miembros y el gate los contaba los ocho. Cinco de ellos --
`getModifiers`, `getSuperclass`, `isInterface`, `isAssignableFrom`, `getDeclaredFields` -- eran
`native` sin ninguna implementacion del otro lado:

```
no native implementation for java/lang/Class.getModifiers()I
```

Es "gate PASS != anda" en su forma mas pura, y vale la pena tenerlo escrito: **la firma estaba
bien**. El `.class` era correcto, `javap` lo mostraba correcto, la comparacion contra OpenJDK
daba correcto, y llamar al metodo tiraba abajo la VM. Un `native` es un agujero que el gate no
puede ver por construccion -- mide lo declarado, y un `native` declara justamente que su cuerpo
esta en otra parte.

### #271 -- ⬜→✅ `String[].class` no parseaba

El literal de clase de un tipo **array de referencias**:

```java
Class<?> c = String[].class;   // error: se esperaba una expresion, se encontro RBracket
```

El camino de los primitivos ya lo hacia (`int[].class` funcionaba); el de los tipos de
referencia no, porque `String` entra como identificador y el `[` que sigue lo tomaba el sufijo de
**acceso a arreglo**, que despues se encuentra un `]` donde esperaba un indice.

**Arreglo** (`src/javac/parser.rs`): en `postfix`, un `[` seguido inmediatamente de `]` no puede
ser un acceso a arreglo -- el indice no es opcional --, asi que la unica lectura posible es un
tipo array, y detras solo puede venir `.class` (JLS §15.8.2). Se consumen los pares y se arma el
`ClassLit`. El mismo razonamiento que ya estaba escrito para los primitivos, aplicado al otro
camino.

**Verificado aparte** (2026-08-25): las dos formas emiten lo mismo que el `javac` del JDK 21 --
`ldc class "[Ljava/lang/String;"` y `ldc class "[[Ljava/lang/String;"`-- y el **acceso** a arreglo
(`xs[0]`), que es lo que este cambio podia romper, sigue emitiendo su `aaload`.


### #272 -- 🔴→✅ VM: `invokespecial` no preguntaba si el metodo tiene cuerpo

Un metodo `private native` se invoca con **`invokespecial`**, no con `invokevirtual`: un privado
no se despacha dinamicamente (JVMS §6.5). Y `invokespecial` era el unico de los cuatro `invoke`
que no consultaba el embudo de metodos sin cuerpo, asi que le empujaba un frame y el bucle de
despacho indexaba un `code` vacio:

```
index out of bounds: the len is 0 but the index is 0
```

Lo notable es que el embudo **ya existia y ya avisaba de esto**. El comentario de
`dispatch_bodiless` dice, palabra por palabra:

> Shared by `invokevirtual` and `invokeinterface` deliberately. A native has **no `Code`**, so
> pushing a frame for one leaves the dispatch loop indexing an empty code slice -- which is
> precisely how an `invokeinterface` with a `String` receiver took the whole interpreter down
> (#225). **One funnel, so the next opcode that dispatches dynamically cannot forget to ask.**

El embudo se construyo para que el proximo opcode no se olvidara, y el proximo opcode se olvido
igual -- porque `invokespecial` no es "el que despacha dinamicamente", es justamente el que NO
lo hace, y por eso quedo fuera de la frase. La regla real no es sobre despacho dinamico: es que
**todo sitio que empuja un frame tiene que preguntar antes si hay cuerpo**.

Por que no habia aparecido: las cuatro costuras de `String` son `private static native`, y una
estatica va por `invokestatic`, que si preguntaba. La primera costura **de instancia** privada
del proyecto fue la de `Class`, y salto en la primera llamada.

**Arreglo** (`src/jvm/interpreter/bytecode_interpreter/invokespecial.rs`): la misma consulta a
`dispatch_bodiless`, con los `[receptor, args...]` ya armados.

**Verificado aparte** (2026-08-25): con el arreglo, `getClass().getName()` vuelve a andar tanto
sobre una clase normal como sobre el mirror de un array, y `isArray()` tambien. Vale registrar el
rastro que dejo mientras estuvo roto, porque **costo una atribucion equivocada**: a mitad de la
tanda de VM, `"x".getClass().getName()` -que minutos antes daba 16- empezo a fallar. Se descarto
que fuera el cambio en curso comparandolo contra un build de `HEAD`, y se atribuyo a "trabajo de
biblioteca en vuelo" porque `java/lang/Class.class` se habia reescrito cinco minutos antes. La
mitad era cierta -- lo que faltaba era **este** defecto de VM, que la nueva `Class` destapo al ser
la primera costura `private native` **de instancia** del proyecto.


### #273 -- 🔴→✅ el corpus de probes tenia SEIS nombres de clase en colision

Se encontro verificando #271/#272: con el arbol de trabajo entero, la suite daba **17** fallas en
vez de 16, y la de mas era `clinit_failure_wraps_in_exception_in_initializer_error`:

```
getstatic/putstatic: static field not found in the class or its superclasses
```

**No era ningun cambio de codigo.** Se aisló cruzando las dos mitades: con el `src` de `HEAD` y el
`java/` del arbol de trabajo **ya fallaba**, asi que el defecto estaba en los probes, no en el
compilador ni en la VM. Y recompilar `ClinitProbe.java` lo devolvia a verde.

La causa: **`java/` es un paquete por defecto plano**, y tres fuentes distintas declaraban un
`Boom`:

| Fuente | Que es su `Boom` |
|---|---|
| `java/Boom.java` | `class Boom extends RuntimeException` |
| `java/ClinitProbe.java` | `class Boom { static final int VALUE = compute(); }` |
| `java/ScopeTest.java` | `final class Boom implements Callable<String>` |

Los tres escriben el **mismo** `java/Boom.class`, y gana **el ultimo que se compilo**. O sea que el
resultado de la suite depende de en que orden alguien corrio el compilador sobre `java/` por
ultima vez -- una falla que aparece y desaparece sin que nadie toque el codigo que ejercita.

Es el mismo genero que "los `.class` del arbol estaban rancios", y peor en un aspecto: ahi el
sintoma era una clase vieja, aca es **la clase de otro**.

#### Correccion del conteo: son SEIS, no doce

La primera barrida dijo doce y **estaba mal**: contaba las clases **anidadas**, que no colisionan
-- una anidada emite `Outer$Inner.class`, y el `Base` de `AbsProbe` es `AbsProbe$Base`. Solo
colisionan las de **nivel superior**. La medicion que vale no es leer las fuentes sino **compilar
cada una en un directorio aparte y ver que `.class` escribe**, que es la verdad de terreno; con eso
los falsos positivos (`Bad`, `Base`, `Impl`, `Nested`, `Sub`, `Worker`) se caen solos.

| Nombre | Fuentes que lo declaran | Resuelto como |
|---|---|---|
| **`Boom`** | `Boom.java`, `ClinitProbe.java`, `ScopeTest.java` | `ClinitBoom`, `ScopeBoom` |
| `Shape` / `Circle` / `Square` | `Sealed.java`, `Shape.java` | `SealedShape`, `SealedCircle`, `SealedSquare` |
| `Counter` | `PubTest.java`, `StableTest.java` | `PubCounter`, `StableCounter` |
| `Sizer` | `LambdaRef.java`, `StableTest.java` | `LambdaSizer`, `StableSizer` |
| `Box` | `Box.java`, `MHCtor.java` | `StatBox` (ver abajo) |

**El `Box` mostro que el defecto ya habia cobrado una victima.** El `Box.class` del arbol es el de
`MHCtor.java` (un ctor `(int)` y un `get()`), no el de `Box.java` (`static Animal shared`). Con lo
cual `Stat.java` -que hace `Box.shared = new Animal()`- **llevaba tiempo sin compilar**, en
silencio, sin que ningun test lo notara porque ningun test lo corre. Renombrado el par a
`StatBox`, `Stat` compila y **corre**: da 4, su valor documentado.

Se renombro el lado de `Box.java` y no el de `MHCtor.java` por una razon concreta: **`MHCtor.java`
no se puede recompilar hoy** -- `MethodHandle.invoke` es *signature-polymorphic* y el compilador
todavia no lo resuelve -- asi que su `.class` es intocable y quien tiene que moverse es el otro.

Tambien se borran `Counter.class` y `Sizer.class`, que despues de los renombres **ya no los
escribe ninguna fuente**: son exactamente el `.class` rancio contra el que este documento avisa en
otro lado.

`Sealed.java` y `Shape.java`, de paso, son **casi el mismo archivo** -- declaran los mismos tres
tipos y la unica diferencia es el `public` de la interfaz. Se prefijo el de `Sealed.java` en vez de
retirarlo, porque retirar el fixture de otra tanda no es una decision de esta.

**Verificacion**: cero colisiones de nivel superior, y suite en aislamiento **1270 / 16** -- la
baseline. La regla para no volver a caer: **el auxiliar de un probe lleva el prefijo del probe**.

### Lo que quedo de `Class`: 37 de 81

De 8. Cuatro nativas -- las mismas cuatro que el JDK declara `native` (`isInstance`,
`getSuperclass`, `isAssignableFrom`, `isHidden`) -- y ocho costuras **privadas** con sufijo `0`
sobre las que el resto se escribe en Java. Es la misma disciplina que dejo a `String` con cuatro
costuras privadas: lo que se puede escribir en el lenguaje se escribe en el lenguaje, porque un
metodo que vive en la VM es un metodo que ningun test de Java puede recorrer -- y porque cada
`native` de conveniencia era ademas una divergencia de modificadores contra la referencia.

Lo que entro es la mitad de **metadatos**: los cinco nombres (`getName`, `getTypeName`,
`getSimpleName`, `getCanonicalName`, `getPackageName`) mas `toString`, `toGenericString`,
`descriptorString` y `describeConstable`; el que-clase-de-cosa-es (`isInterface`, `isArray`,
`isPrimitive`, `isAnnotation`, `isEnum`, `isRecord`, `isSynthetic`, `getModifiers`); el grafo
(`getSuperclass`, `getInterfaces`, `getComponentType`, `componentType`, `arrayType`,
`isAssignableFrom`, `isInstance`); los casteos (`cast`, `asSubclass`); la busqueda por nombre
(los dos `forName`, `forPrimitiveName`); y los campos (`getDeclaredFields`, `getDeclaredField`,
`getFields`, `getField`).

Lo que falta se divide en tres, y ninguna de las tres es "escribir mas metodos":

1. **El modelo de objetos de `java.lang.reflect`** (~25 miembros): `getMethods`,
   `getConstructors`, las anotaciones -- que exigen clases proxy por `@interface` -- y el modelo
   de tipos genericos. `Field` ya existe y por eso los campos entraron.
2. **Atributos del class file que la VM todavia no lee** (~10): `InnerClasses` (de ahi salen
   `getDeclaringClass`, `getEnclosingClass`, `isMemberClass`, `isAnonymousClass`,
   `isLocalClass`, `getDeclaredClasses`), `NestHost`/`NestMembers`, `PermittedSubclasses`,
   `Record`. `ClassFile` guarda los atributos crudos, asi que es parseo, no infraestructura.
3. **Modulos, seguridad y recursos** (~8): `getModule`, `getPackage`, `getProtectionDomain`,
   `getSigners`, `getResource`. Nada de eso existe en KajiJDK.

Dos elecciones que conviene dejar dichas, porque parecen atajos y no lo son:

- **`getClassLoader()` devuelve `null` y eso es la respuesta correcta**, no un placeholder:
  KajiJDK carga todo por un unico loader, y `null` es exactamente lo que la especificacion dice
  que reporta el bootstrap loader.
- **`isNestmateOf` esta y `getNestHost` no.** Sin el atributo `NestHost` lo unico que se puede
  contestar es un tipo consigo mismo, que es lo que `isNestmateOf` necesita. Un `getNestHost`
  inventado concederia acceso `private` entre tipos que no son nestmates -- un permiso, no un
  dato.

### #270 -- ✅ `Void.TYPE`, cerrado de paso

`void.class` no compilaba porque `java.lang.Void` no declaraba `TYPE`. El comentario del archivo
lo daba por diferido "hasta que la VM soporte la clase primitiva void" -- y la soportaba desde
que existe `getPrimitiveClass`: la nota sobrevivio a la razon que la habia puesto. `Void`: 1/1.

Vale como recordatorio de la otra mitad de la leccion de los `.class` rancios: **un comentario
que explica por que algo falta tambien envejece**, y no hay nada que lo recompile.

**Verificado corriendo** (2026-08-25), que es lo que faltaba para bajarlo del indice: las cinco
propiedades del mirror dan las cinco -- `void.class` no es nulo, **es** `Void.TYPE`, **no** es
`Void.class` (son dos mirrors distintos, que es lo que el javadoc del archivo se toma el trabajo de
aclarar), su `getName()` es `"void"`, y `int.class == Integer.TYPE`.

Y la verificacion trae un dato sobre el estado del arbol: **con el `run-headless` congelado
panica**, porque `getName()` llama a `name0()`, que es `private native`, y eso es #272 -- que esta
documentado pero cuyo codigo no esta en `HEAD`. O sea que hoy `Void.TYPE` se puede *usar* pero no
se puede *inspeccionar* con las herramientas congeladas. Es el mismo acoplamiento que anota
`FROZEN.md`, visto desde el otro lado.


---

## Tanda 2026-08-24 (i) -- `java.lang.reflect.Method`, y la llamada que decide su destino leyendo un objeto

`Method` 15/26 → **22/26**, `Executable` → 17/22, `Class` 37 → **41/81**. `MethodTest` (cinco
grupos) da 0 en el JDK 25 y 0 en nuestra VM.

### Donde vive `invoke`, y por que no es una nativa

`Method.invoke` **no** puede ser una nativa del puente. La razon es estructural y vale mas que el
metodo: el puente de nativas recibe `(metaspace, heap, console, apt)` y devuelve un `Option<Value>`
-- puede computar, no puede **empujar un frame**. Y una llamada reflexiva es, entera, empujar un
frame.

Lo que si existe es `Exec::call_java`, que ya sostiene la inicializacion de clases, el
`String.valueOf(Object)` de la concatenacion y `ConstantBootstraps.invoke`. Asi que `invoke` se
suma a esa lista: se intercepta en `invokevirtual` con un `Intrinsic`, igual que `Object.clone` y
`Thread.start`, que estan ahi por la misma clase de razon (tienen que poder tirar, o tocar el
scheduler, cosas que una nativa tampoco puede).

Lo que hace, en orden: **rearma el descriptor** desde `parameterTypes` y `returnType` -- el
descriptor es la mitad de la identidad de un metodo y sin el no hay a quien resolver --,
**desempaqueta** los argumentos, corre el frame, y **empaqueta** el resultado. Los dos
empaquetados son la frontera entre una API que habla en `Object` y un interprete que habla en
valores de maquina; es el mismo cruce que hace el autoboxing del lenguaje, a mano porque aca el
tipo no se conoce hasta leer el `Method`.

Dos detalles que el probe fijo y no eran obvios:

- **El resultado se empaqueta llamando al `valueOf` del wrapper**, no alocando el objeto a mano.
  Es lo unico que respeta la cache de −128..127, y por lo tanto lo unico que hace que
  `invoke` de un entero chico devuelva el **mismo objeto** que el autoboxing.
- **Una excepcion del metodo invocado sale envuelta en `InvocationTargetException`.** No es
  cosmetica: sin el envoltorio quien llama no puede distinguir "el metodo que llame tiro" de
  "invoke no llego a llamarlo". El envoltorio pone esa frontera en el tipo, y es por lo que
  `getTargetException` existe.

### El bug que encontro el probe: `invoke` leia el objeto equivocado

La primera version desempaquetaba asi: si el parametro es `int`, buscar el campo `value` de
`java/lang/Integer` y leerlo del argumento. Sin preguntar **de que clase es el argumento**.

Pasarle un `String` donde va un `int` no fallaba: leia el offset del campo `value` sobre un objeto
que no lo tiene y devolvia lo que hubiera ahi -- un numero perfectamente plausible. El metodo
corria con basura y devolvia un resultado.

Es el modo de falla mas caro que hay: no hay excepcion, no hay panico, hay un **numero**. Y no lo
encontro leer el codigo -- lo encontro una linea del probe que decia que eso tiene que tirar
`IllegalArgumentException`, contrastada contra el JDK.

Ahora `unbox` pregunta la clase primero y acepta el tipo exacto mas las **ampliaciones** del
lenguaje (JLS §5.1.2): un `Integer` sirve para un parametro `long`, `float` o `double`, y no al
reves. Y lee con el ancho del **wrapper que llego**, no con el del parametro: un `Integer` para un
parametro `long` tiene cuatro bytes, y leerle ocho le pide prestado el campo de al lado.

### #274 -- ⬜ un nombre CALIFICADO no se reconoce como tipo dentro de una expresion

```java
public class QualProbe {
    static java.lang.reflect.Method campo;                    // resuelve
    public static int lectura() {
        return java.lang.reflect.Modifier.PUBLIC;             // error: no se encuentra el simbolo: java
    }
    public static int llamada() {
        return java.lang.reflect.Modifier.isPublic(1) ? 1 : 0;  // idem
    }
}
```

En una **declaracion** el nombre calificado resuelve; en una **expresion** no, y el error lo dice
todo: `simbolo: variable java`. El compilador esta leyendo `java.lang.reflect.Modifier.PUBLIC`
como una cadena de accesos a campo que arranca en una variable llamada `java`.

Es exactamente la reclasificacion de nombres ambiguos de JLS §6.5.2: frente a `a.b.c.d`, hay que
probar prefijos de izquierda a derecha hasta que uno resuelva como tipo, y recien lo que sigue es
acceso a miembro. No es lo mismo que **#210** -- ahi un tipo calificado *dentro de `java.lang`*
degrada a `Object` en silencio; aca el nombre no se reconoce en ningun paquete y el error es
ruidoso.

Repro: `repros/finding_274.java`. Rodeo: `import` y nombre simple, que es el mismo que ya usan
casi todos los archivos de la biblioteca.

### La familia de nativas fantasma, tercera entrega

`java.lang.Class` tenia cinco (tanda h). `java.lang.reflect` tenia tres mas, y por el mismo
motivo: se declararon `native` como marcador de "esto lo hara la VM algun dia" y el gate las
conto como implementadas.

| donde | que | como quedo |
|---|---|---|
| `Executable.getGenericParameterTypes()` | `native` sin cuerpo | Java: los tipos **erasados** |
| `Executable.getGenericExceptionTypes()` | `native` sin cuerpo | idem |
| `Method.getTypeParameters()` | `native` sin cuerpo | Java: array vacio |

Y devolver los tipos erasados no es un relleno: **es la respuesta** cuando el metodo no tiene
atributo `Signature`, que es lo que el JDK contesta en ese mismo caso. Ningun metodo que nuestra
VM sepa describir tiene uno, asi que hoy la respuesta coincide siempre. Lo que un `Signature`
agregaria es la diferencia entre `List` y `List<String>` -- una brecha real, angosta, y la misma
para las tres.

Quedan cuatro nativas fantasma en la familia (`getAnnotatedReceiverType`,
`getAnnotatedParameterTypes`, `getAnnotatedExceptionTypes` en `Executable`,
`getAnnotatedReturnType` en `Method`). Esas no se pueden contestar con lo erasado: hay que
**construir** un `AnnotatedType`, y no hay de donde. Se dejan anotadas en vez de arregladas a
medias.

### Lo que falta de `Method`: cuatro

`getAnnotation`, `getDeclaredAnnotations`, `getParameterAnnotations` y `getDefaultValue`. Los
tres primeros necesitan una clase proxy por `@interface`, que es el mismo bloqueo que tiene
`Class`. El cuarto necesita leer el atributo `AnnotationDefault` y decodificar un `element_value`
completo -- anidamiento incluido --, y contestar `null` mientras tanto haria que **toda anotacion
pareciera no tener defaults**, que es una respuesta y no una ausencia. Por eso no esta.


---

## Tanda 2026-08-24 (j) -- los wrappers, y dos cosas que el gate no puede ver

`Short` 10→31/31, `Byte` 10→30/30, `Boolean` 6→21/21, `CharSequence` 4→9/9,
`ConstantDescs` 50→66/66, `Character` 74→135/166. `WrapMoreTest` (siete grupos) da 0 en el JDK 25
y 0 en nuestra VM.

Casi todo `Short` y `Byte` pasa por `Integer`, y eso es el diseño y no pereza: la JVM no tiene
aritmetica de `short` ni de `byte` -- toda operacion sobre uno es una operacion de `int` con un
estrechamiento al final --, asi que un parser propio seria una segunda implementacion de lo mismo,
capaz de discrepar con la primera. Lo unico que estas clases agregan es **el chequeo de rango**,
que es donde de verdad se diferencian.

### #275 -- 🔴 las caches de wrapper habian desaparecido, y JLS 5.1.7 las EXIGE

```java
Integer a = Integer.valueOf(100);
Integer b = Integer.valueOf(100);
a == b   // false
```

Y peor, porque el autoboxing pasa por `valueOf`:

```java
Integer a = 100;
Integer b = 100;
a == b   // false
```

**No es una optimizacion perdida: es una regla del lenguaje rota.** JLS 5.1.7 dice que boxear un
valor entre −128 y 127 tiene que devolver la MISMA referencia, y el codigo que compara enteros
chicos con `==` -- que es comun, aunque sea mala practica -- funciona en cualquier JVM y no
funcionaba en la nuestra.

Lo que hace esto interesante es que **ya habia estado bien**. El roadmap lo registra verificado
("identidad `==` para valores chicos, como exige la JLS -- verificado: `100==100` true,
`200==200` false"), y era cierto: la cache estaba en `boot/java/lang/Integer.class`. Cuando el
`Integer` de KajiLibrary tomo el lugar del de `boot/` -- KajiLibrary gana en el bootclasspath --
la cache se perdio, y nada lo noto porque **ninguna prueba comparaba dos boxeos con `==`**.

Arreglado en `Integer`, `Long`, `Short`, `Byte` y `Character` (que cachea 0..127); `Boolean` ya
estaba bien porque sus dos instancias son constantes. `Byte` es el caso curioso: su rango entero
cabe en la cache, asi que `Byte.valueOf` **nunca aloca**.

La prueba que lo fija comprueba las dos direcciones, y la que tiene que dar **false** importa
tanto como las otras: una cache que cubriera todos los valores haria que el codigo que compara
con `==` pareciera andar, y se romperia contra un JDK real.

**Verificado aparte, y con oraculo** (2026-08-25). Se escribio un probe de **dieciocho**
propiedades -los dos caminos (`valueOf` y autoboxing), los cuatro bordes exactos (-128, 127, -129,
128) y los seis wrappers- y se corrio la **misma fuente** contra el JDK 21: imprime **262143**, el
mismo numero. Eso es lo que convierte al probe en oraculo y no en opinion: el valor esperado no es
el que da hoy, es el que **tiene que dar**.

**Y la prueba quedo cableada, que es la mitad que faltaba.** El propio finding dice por que se
perdio la cache: *"nada lo noto porque ninguna prueba comparaba dos boxeos con `==`"*. Habia un
probe nuevo (`java/WrapMoreTest.java`) pero **ningun test de Rust que lo corriera**, o sea el mismo
hueco un nivel mas arriba. Ahora hay uno: `java/WrapCacheProbe.java` mas
`jvm::interpreter::library_conformance`.

El modulo es nuevo y la categoria vale separarla: los tests de `gc.rs` preguntan si el interprete
hace lo que el bytecode dice; estos preguntan si **la biblioteca que la VM carga** se comporta como
la JLS obliga. Un defecto de esta clase no rompe ninguna instruccion, rompe una **garantia**, y por
eso pasa desapercibido hasta que alguien la usa. Su `run_probe` pone **KajiLibrary primero** en el
bootclasspath a proposito: medir la conformidad contra `boot/` seria medir la biblioteca
equivocada, que es literalmente #246.

> **El test esta `#[ignore]` y hay que sacarselo.** El arreglo de los cinco wrappers vive **sin
> commitear** en el arbol de trabajo, asi que sobre `HEAD` el test **falla** -se comprobo-. Esa es
> la prueba de que la guarda sirve; y es la tercera vez en el dia que la documentacion de un
> arreglo esta en `HEAD` y el arreglo no (ver #271/#272 y la nota de `FROZEN.md`).

### #276 -- 🔴 `ArrayIndexOutOfBoundsException` no descendia de `IndexOutOfBoundsException`

```java
try { a[5] = 'x'; }
catch (IndexOutOfBoundsException e) { ... }   // NO entraba
```

Nuestra `ArrayIndexOutOfBoundsException` extendia `RuntimeException` derecho. La jerarquia real
es `RuntimeException` → `IndexOutOfBoundsException` → `ArrayIndexOutOfBoundsException`, y
`catch (IndexOutOfBoundsException)` es la forma mas comun que hay de manejar un error de indice.

**El gate no puede verlo por construccion.** Los miembros de la clase estaban todos: dos
constructores, las firmas correctas, `javap` conforme. Lo que estaba mal era la **superclase**, y
un miembro correcto en una jerarquia equivocada pasa cualquier comparacion de firmas. Lo encontro
una prueba que atrapaba la excepcion **por su supertipo**, que es una pregunta que ninguna
comparacion de superficie hace.

`apidiff` sí lo mide -- "declaracion de clase coincide: 448/593" -- pero ese numero es un total, y
un total no nombra a nadie. Al desglosarlo aparecieron **63 clases con la superclase o la lista de
interfaces distinta**. La mayoria son libertad legitima (no tenemos `AbstractStringBuilder`,
`AbstractList`, `Striped64`: son package-private y el contrato publico no las nombra). Las que
**no** lo son, ordenadas por lo que cuestan:

| clase | nuestra | la real | que rompe |
|---|---|---|---|
| `ArrayIndexOutOfBoundsException` | `RuntimeException` | `IndexOutOfBoundsException` | ✅ arreglado |
| `IllegalThreadStateException` | `RuntimeException` | `IllegalArgumentException` | ✅ arreglado |
| `InvocationTargetException` | `Exception` | `ReflectiveOperationException` | ✅ arreglado |
| `GenericSignatureFormatError` | `Error` | `ClassFormatError` | ✅ arreglado |
| `ExecutionException` | `RuntimeException` | `Exception` | ⬜ **es unchecked donde deberia ser checked** |
| `TimeoutException` | `RuntimeException` | `Exception` | ⬜ idem |
| `ExecutorService` | `Executor` | `Executor, AutoCloseable` | ⬜ no se puede usar en try-with-resources |
| `List` | `Collection` | `SequencedCollection` | ⬜ falta la interfaz de Java 21 |
| `Deque` | `Queue` | `Queue, SequencedCollection` | ⬜ idem |
| `ChronoLocalDate` y dos mas | -- | falta `Comparable` | ✅ arreglado |
| `ExecutorService` | `Executor` | `Executor, AutoCloseable` | ✅ arreglado |
| las anotaciones | sin `ACC_ANNOTATION` | con | ✅ arreglado — **y era del compilador** |
| `PrintStream` | `(Object)` | `FilterOutputStream` | ⬜ no sirve donde se espera la base |
| `MessageFormat` | `(Object)` | `Format` | ⬜ idem |
| `List` / `Deque` | -- | falta `SequencedCollection` | ⬜ pide una vista inversa de verdad |
| `ExecutionException` / `TimeoutException` | `RuntimeException` | `Exception` | ⬜ unchecked donde va checked |

Los dos primeros pendientes son los mas caros y por eso no los toque en esta tanda: pasar
`ExecutionException` de unchecked a checked obliga a declarar `throws` en cada sitio que la
propaga, y eso es una barrida por `java.util.concurrent` -- territorio de la otra sesion.

#### Segunda vuelta (2026-08-25): tres mas, y la de las anotaciones no era de la biblioteca

**Las siete anotaciones eran un defecto del COMPILADOR**, no de la biblioteca — las fuentes ya
decian `@interface`. Lo que faltaba eran las **dos** cosas que JLS 9.6 y JVMS 4.1 ponen y el fuente
no escribe: el flag `ACC_ANNOTATION` y el `extends java.lang.annotation.Annotation` implicito.

```
nuestro    flags: (0x0601) ACC_PUBLIC, ACC_INTERFACE, ACC_ABSTRACT
el JDK     flags: (0x2601) ACC_PUBLIC, ACC_INTERFACE, ACC_ABSTRACT, ACC_ANNOTATION
           public interface java.lang.Deprecated extends java.lang.annotation.Annotation
```

Y las dos mitades importan por separado: **sin el flag**, `Class.isAnnotation()` niega que lo sea;
**sin la superinterfaz**, una anotacion no es asignable a `Annotation`, que es el tipo por el que la
reflexion las devuelve. O sea que ninguna de las dos mitades de la reflexion de anotaciones podia
funcionar — con la clase entera bien formada y todos sus miembros correctos, que es exactamente por
lo que ninguna comparacion de firmas lo veia. Es el mismo modo de falla que el finding ya describe
para la superclase, un piso mas abajo. Es tambien de la familia de #116/#242: un modificador
**implicito** de la spec que el emisor no ponia.

La superinterfaz se agrega al emitir y **no** en la lista de `implements` del AST, a proposito: no
esta escrita en el fuente, asi que meterla ahi la haria aparecer en el `Signature` y en los chequeos
de override como si el programador la hubiera puesto.

**`ExecutorService` es `AutoCloseable`**, con `close()` como `default` — que es lo que es en el JDK,
y por eso agregar la interfaz no rompe a ningun implementador. Espera en tandas y no para siempre:
la version del JDK espera indefinidamente y maneja la interrupcion; aca no hay interrupcion que
manejar, asi que la forma honesta es un bucle acotado que se rinde y no uno que finge esperar.

**Los tres `Chrono*` son `Comparable`**, con `compareTo` como `default`. El orden ya existia
—`isAfter`/`isBefore`/`isEqual` estan construidos sobre el—; lo que faltaba era **el nombre por el
que el lenguaje lo conoce**. Dos detalles que no son decoracion:

- `ChronoLocalDate.compareTo` desempata por **id de cronologia** cuando dos fechas de calendarios
  distintos nombran el mismo dia. Sin ese desempate, dos fechas que no son `equals` comparan 0 y un
  `TreeSet` se queda con una sola.
- `ChronoZonedDateTime.compareTo` **no** es `isBefore`/`isAfter`: esas comparan el instante y nada
  mas, y `compareTo` sigue por local date-time y por zona. Dos zonificados en el mismo instante en
  zonas distintas no son iguales, asi que un orden que parara en el instante los daria por
  equivalentes.

**Y destapo uno que estaba latente:** `LocalDate` implementaba `Comparable<LocalDate>` **y** ahora
heredaba `Comparable<ChronoLocalDate>`, y una clase **no puede** implementar dos parametrizaciones
de la misma interfaz. El compilador lo dijo bien —*"la referencia a `compareTo` es ambigua"*— y el
arreglo es el del JDK: `LocalDate` no declara su propio `Comparable` y su `compareTo` toma
`ChronoLocalDate`. La firma cambia y **se acerca** a la real: el JDK declara
`public int compareTo(ChronoLocalDate)`.

Queda pendiente `List`/`Deque` con `SequencedCollection`, y no por pereza: `reversed()` devuelve una
**vista**, no una copia, asi que hacerlo bien pide escribir la vista inversa. Un `reversed()` que
copiara cumpliria la firma y mentiria sobre el contrato, que es justo lo que la regla de esta
biblioteca prohibe.

### Un error propio que la prueba atrapo, y por que vale contarlo

`isTitleCase` la escribi derivada de las tablas de caso que ya existen: "es su propia titlecase y
no es su propia minuscula". Suena impecable. Comparada contra el JDK sobre el BMP entero:
**discrepa en 73 code points** -- encuentra 50 donde hay 31, y entre ellos mete a la `A`.

Titlecase es una **categoria** que el estandar asigna, no una consecuencia de los mapeos. Son 31
caracteres, los digrafos que se escriben con solo su primera letra en mayuscula (`Dz` frente a
`DZ` y `dz`) mas las formas griegas con iota suscrita, y caben en diez rangos. Ahora es una tabla,
generada del JDK como las de `sqrt` y `fma`.

La prueba que lo fija cuenta: hay exactamente 31 en el BMP. Un caso suelto (`'A'` tiene que dar
false) tambien lo hubiera atrapado -- pero el conteo es el que no depende de que se me ocurra el
contraejemplo correcto.

### Y `ConstantDescs` cerrado, porque #101 ya no lo bloquea

Los doce `BSM_*`, las dos fabricas `of*Bootstrap` y `NULL`/`TRUE`/`FALSE` estaban omitidos con una
nota que decia por que: necesitan `DirectMethodHandleDesc.Kind.STATIC`, y nombrar un miembro
estatico de un tipo anidado desde otro archivo era el finding #101. **#101 esta cerrado.** Es la
tercera vez en la sesion que una nota sobrevive a su motivo -- `Void.TYPE`, el pliegue de
`ofCanonical`, y esta.

Con los BSM disponibles, `describeConstable` deja de ser inalcanzable para `Short`, `Byte`,
`Boolean` y `Character`: los cuatro se describen como un **cast** de un int, porque un archivo de
clase no tiene constante de ninguno de esos tipos. Y `DynamicConstantDesc.ofCanonical` pliega el
constante nulo, que era lo que su propio comentario declaraba imposible.

### Lo que falta de `Character`: 31, y son una sola tarea

`getType`, `getDirectionality`, `getName`, `codePointOf`, los seis de emoji, `isDefined`,
`isMirrored`, `isIdeographic`, `isAlphabetic` y las cuatro familias de identificadores. **Todos
salen de la misma pieza que no existe: la tabla de categorias generales.** `Character` ya tiene
nueve tablas de rangos (mayusculas, minusculas, letras, digitos, espacios...) generadas del
oraculo; falta esa, y de ella cuelga todo lo demas -- las familias de identificadores son
combinaciones de categorias, no propiedades independientes.

Es trabajo de datos, no de codigo, y tiene el mismo metodo que ya usamos: generar la tabla
corriendo el JDK sobre los 0x110000 code points y comprimirla en rangos.

---

## Tanda 2026-08-24 (k) -- `Character` cerrado: 164 de 166

`Character` 135 → **164/166**. `CharUnicodeTest` (tres grupos) da 0 en el JDK 25 y 0 en nuestra
VM.

### Todo cuelga de una tabla

Unicode le asigna a cada code point exactamente una **categoria general** -- letra mayuscula,
digito decimal, separador de linea, sin asignar -- y casi toda pregunta sobre un caracter resulta
ser una pregunta sobre a que conjunto de categorias pertenece. Esa es la razon por la que las
veintinueve consultas que faltaban eran, en realidad, una sola tarea: escribir `getType`.

La tabla son **4099 rangos**, generados corriendo el JDK 25 sobre los 0x110000 code points y
comprimidos en pares `[desde, categoria]` -- el rango llega hasta el `desde` siguiente menos uno,
que es lo que permite guardar dos enteros por rango en vez de tres. Es el mismo metodo que usan
las tablas de referencia de `sqrt` y `fma`, y por la misma razon: **los datos del estandar no se
derivan, se consultan**, asi que la unica fuente honesta es una implementacion que los tiene.

Encima de ella, en una linea cada una: `isDefined`, las cuatro familias de identificadores
(Java y Unicode, inicio y continuacion) e `isIdentifierIgnorable`. Que sean derivables **no se
supuso: se comprobo**, barriendo los 0x110000 code points y contando discrepancias contra la
referencia. Las que dieron cero se escribieron derivadas; las que no, no.

### Las tres que NO se derivan, y como se supo

| propiedad | discrepancias con la derivacion "obvia" | que quedo |
|---|---|---|
| `isDefined` | 0 | derivada de la categoria |
| `isJavaIdentifierStart` / `Part` | 0 | derivadas |
| `isIdentifierIgnorable` | 0 | derivada |
| `isUnicodeIdentifierStart` | **6** | derivada + lista `OTHER_ID_START` |
| `isUnicodeIdentifierPart` | **18** | derivada + lista `OTHER_ID_CONTINUE` |
| `isAlphabetic` | **1495** | tabla propia de 757 rangos |

`isAlphabetic` es el caso que mejor explica por que esto no se puede razonar: parece "letra o
numero-letra" y el estandar marca ademas 1495 code points con la propiedad Other_Alphabetic --
las senales vocalicas de las escrituras indias, sobre todo, que son marcas combinantes y son
indiscutiblemente parte de una palabra. Ninguna cantidad de leer la especificacion produce ese
numero; barrer contra el oraculo lo produce en un segundo.

Los dos identificadores Unicode fallaban por seis y dieciocho code points. Son listas de
excepciones, no tablas, y se recorren linealmente a proposito: una busqueda binaria sobre
dieciocho entradas cuesta mas leerla que correrla.

### `getDirectionality`, y por que es una tabla aparte

2300 rangos propios. La clase bidireccional **no** es una funcion de la categoria -- es la
propiedad que decide como se acomoda una linea con hebreo e ingles mezclados -- y
`DIRECTIONALITY_UNDEFINED` vale −1 y no 0 justamente porque 0 ya significa izquierda-a-derecha y
hacia falta un valor para "no hay respuesta".

### Como se valida una tabla de 4099 rangos

No con ejemplos. Un rango copiado un code point corto cae **entre** dos ejemplos cualesquiera que
a uno se le ocurran, y por eso la prueba barre: el BMP entero mas una muestra fija de los planos
suplementarios, reducido a un numero, contra el numero que la referencia produce sobre exactamente
el mismo conjunto. Dos hashes (categoria y direccionalidad) y dieciocho conteos.

Los ejemplos nombrados siguen estando, y su papel es otro: un conteo que se corre en uno no dice
**cual** propiedad se rompio, y `getType('$') == CURRENCY_SYMBOL` si lo dice.

Vale anotar un error de expectativa que la prueba destapo, porque es la clase de cosa que uno
"sabe": escribi que un digito es `Emoji_Component` **sin ser** emoji. Es al reves -- `'7'` es las
dos cosas, porque una secuencia de keycap se arma con el. Lo que Emoji_Component separa son los
tonos de piel, que son componentes, de las caras a las que se pegan, que no lo son.

### Las dos que quedan afuera, con el numero que lo justifica

`getName(int)` y `codePointOf(String)`. No es que sean dificiles: es que la base de nombres de
Unicode son **294.579 code points con nombre y 10 MB de texto**. El JDK los carga perezosamente
de un recurso comprimido (`uniName.dat`); KajiJDK no tiene carga de recursos, y 10 MB de literales
de cadena no entran en un archivo fuente -- ni de lejos en el limite de 64 KB por metodo.

Se podria implementar el subconjunto **algoritmico** (`CJK UNIFIED IDEOGRAPH-XXXX`,
`HANGUL SYLLABLE ...`), que son unos 5876. Devolver el nombre correcto para esos y uno equivocado
o nulo para los otros 288.703 seria peor que no tener el metodo, por la misma regla que dejo
afuera a `getNestHost` y a `getDefaultValue`: **una respuesta equivocada no es media respuesta.**

Cuando exista carga de recursos, las dos entran juntas y sin decisiones nuevas.


---

## Tanda 2026-08-24 (l) -- `Class` y `ClassLoader`: hasta donde llega el techo

`Class` 41 → **66/81**, `ClassLoader` 7 → **27/43**, `Constructor` 12 → **18/22**.
`ClassAttrTest` y `LoaderTest` dan 0 en el JDK 25 y 0 en nuestra VM.

**No quedaron cerradas, y el motivo es concreto**: 15 miembros de `Class` y 16 de `ClassLoader`
necesitan clases que la biblioteca no tiene -- `java.lang.Module`, `java.lang.Package`,
`java.lang.reflect.AccessFlag` -- o maquinaria que la VM no tiene todavia (leer un recurso que no
es un `.class` del classpath, y clases proxy por `@interface`). Escribirlos devolviendo null seria
peor que no tenerlos: un `getResource` que nunca encuentra nada es indistinguible de un recurso que
no existe.

### Los constructores, que son la misma maquinaria que los metodos

`Constructor.newInstance` se intercepta en `invokevirtual` con un `Intrinsic`, igual que
`Method.invoke`, y por la misma razon estructural: tiene que **correr bytecode**, y el puente de
nativas solo puede computar y devolver. Un `new` de bytecode son dos instrucciones -- `new` aloca
y `invokespecial <init>` corre el cuerpo -- y esto es exactamente eso, con el tipo decidido en
tiempo de ejecucion en vez de escrito en el pool.

Con eso entraron cinco miembros de `Class` (`getConstructor`, `getDeclaredConstructor`, los dos
plurales y `newInstance`), y de arrastre `getEnumConstants`, que se implementa **llamando** al
`values()` que el compilador sintetiza: es el unico lugar donde la lista existe.

Un detalle de GC que vale escribir. El objeto recien alocado se estaciona en la **pila de
operandos** antes de correr el `<init>`, no en una variable de Rust. La pila es raiz del colector y
el colector la reescribe; una variable de Rust no lo es, y el `<init>` puede alocar y mover el
objeto debajo. Es el mismo patron que `capture_backtrace` usa para la excepcion.

`Class.newInstance()` reproduce a proposito su propia misfeature -- la excepcion del constructor
sale **sin envolver y sin estar declarada** -- con un `sneak` generico que el borrado de tipos hace
legal. Es lo que el JDK hace con `Unsafe.throwException`, y es exactamente por lo que el metodo
esta deprecado.

### La capa de ATRIBUTOS: lo que el archivo de clase pierde

Un `.class` no sabe que estaba adentro de otro. `Outer` y `Outer$Inner` son dos archivos sueltos
cuyos nombres comparten un prefijo, y `$` es un caracter legal en un identificador -- asi que
**ninguna** de estas preguntas se contesta mirando un nombre. Las contestan seis atributos que
`ClassFile` guardaba en crudo y nadie leia: `InnerClasses`, `EnclosingMethod`, `NestHost`,
`NestMembers`, `PermittedSubclasses` y `Record`.

De ahi salieron quince miembros: `getDeclaringClass` y `getEnclosingClass` (que se distinguen
justamente en la clase local, declarada adentro de un METODO: tiene la que la encierra y no tiene
la que la declara), `isMemberClass`/`isAnonymousClass`/`isLocalClass`, `getDeclaredClasses`,
`getClasses`, `getEnclosingMethod`, `getEnclosingConstructor`, `getNestHost`, `getNestMembers`,
`isNestmateOf`, `isSealed`, `getPermittedSubclasses` y `getRecordComponents`.

Tres cosas que el atributo dice y ninguna heuristica puede:

- **Una anonima no tiene nombre simple**, y eso es un dato: su entrada de `InnerClasses` deja el
  `inner_name` en cero. `getSimpleName()` estaba derivando el nombre del `$` del nombre binario,
  lo cual acierta casi siempre y falla con una clase de nivel superior que de verdad se llame
  `A$B`. Ahora lee el atributo.
- **Null y vacio son respuestas distintas** en `getPermittedSubclasses`: null es "cualquiera
  puede extenderla" y un array vacio es "nadie puede", que es una cosa legal y muy restrictiva
  para que una clase sellada diga.
- **`isNestmateOf` dejo de ser una comparacion de identidad.** Estaba asi porque sin el atributo
  lo unico contestable era un tipo consigo mismo; ahora compara anfitriones de nido, que es la
  pregunta real: quienes comparten acceso `private`.

### #278 -- ⬜→✅ el `inner_name` de una clase LOCAL llevaba el prefijo numerico

```java
static Class<?> laLocal() {
    class Local { }
    return new Local().getClass();
}
// getSimpleName() daba "1Local" donde el JDK da "Local"
```

El nombre binario de una clase local es `Outer$1Local`, y el numero esta ahi para desambiguar:
dos clases locales llamadas igual en dos metodos distintos compartirian archivo si no. Pero el
`inner_name` del atributo `InnerClasses` guarda el nombre del **fuente**, sin el, y `inner_entry`
lo copiaba del nombre binario entero.

Cotejado contra el JDK 25 sobre el mismo fuente:

```
el nuestro:  #27= #23;   // 1Local=class ClassAttrTest$1Local
el JDK:      #23= #7;    // Local=class ClassAttrTest$1Local
```

Arreglado en `src/javac/codegen.rs`. Es un defecto que **solo se ve reflexivamente**: el nombre
binario estaba bien, la clase cargaba bien, y lo unico que mentia era `getSimpleName()`.

### `ClassLoader`: una forma sin mecanismo, salvo donde si lo hay

KajiJDK tiene **un** cargador. La jerarquia de delegacion que da forma a esta clase esta como
forma y no como mecanismo: `getParent()` da null, y `getSystemClassLoader()` y
`getPlatformClassLoader()` dan el mismo objeto. No es un stub -- es lo que una VM con un cargador
reporta honestamente, igual que `Class.getClassLoader()` da null para lo que definio el bootstrap.

Lo que **si** es mecanismo de verdad es `defineClass`: bytes adentro, un tipo cargado afuera, sin
archivo en ningun lado. Es el unico camino por el que un tipo entra a la VM sin venir del
classpath, y es de lo que viven los generadores de bytecode. La prueba arma **un archivo de clase
byte por byte** y se lo pasa; una prueba que cargara uno de un archivo no ejercitaria nada de esto.

Y `findLoadedClass` mide algo mas fino de lo que parece: no es "esta cargada" sino "la defini
**yo**". Un cargador propio no ve `java.lang.String` aunque la VM la tenga hace rato. Por eso es
estado por instancia y no una consulta a la VM -- la primera version consultaba la VM, y con eso
un cargador propio contestaba `String.class`, que es la respuesta de otra pregunta.

Las cuatro de aserciones llevan estado real y `Class.desiredAssertionStatus` las consulta. Que
cambiar el estado no afecte a una clase ya inicializada no es una limitacion nuestra: `assert` se
desugariza a una guarda sobre un campo que el `<clinit>` lee una sola vez, y por eso la
especificacion dice que estos setters afectan a lo que se cargue **despues**.

### El techo, con nombres

| bloqueado por | miembros de `Class` | miembros de `ClassLoader` |
|---|---|---|
| clases proxy por `@interface` | 8 (las seis de anotaciones + dos `AnnotatedType`) | -- |
| `java.lang.Module` | 2 (`getModule`, `forName(Module,String)`) | 1 (`getUnnamedModule`) |
| `java.lang.Package` | 1 | 5 |
| leer un recurso del classpath | 2 | 10 |
| `java.lang.reflect.AccessFlag` | 1 (`accessFlags`) | -- |
| `java.security.ProtectionDomain` real | 1 | -- |

`URL`, `ProtectionDomain` y `RecordComponent` **si** existen, asi que el bloqueo de los recursos
es la lectura del classpath y no la clase de retorno. Ese es el siguiente escalon si se quiere
seguir: una costura que lea un archivo cualquiera del classpath desbloquea doce miembros de una.


### #277 -- ⬜→✅ el Filer registraba un offset que la colecta invalidaba

La sospecha anotada era la correcta, y se pudo confirmar en vez de creerle: instrumentando
`gc::minor` se ve **una** colecta durante `the_filer_hands_generated_source_back_to_rust`, y el
driver termina bien (`drive()` devuelve 0 a los 480 pasos, sin excepcion). O sea: el programa
escribio el texto, y lo que volvia vacio era la **lectura**.

`KajiFiler.nativeRegisterSourceFile` empuja `(nombre, offset del StringWriter)` a un canal
`thread_local`. El nombre es una `String` copiada del heap y por eso sobrevive a cualquier cosa. El
offset, no: es un numero crudo. Cuando el colector copiador evacua el writer, el objeto queda en
otra direccion y el canal sigue apuntando a la vieja. Ahi todavia estan los bytes del original
—un colector copiador no borra lo que deja atras—, asi que la lectura *funciona*: encuentra un
objeto con el class-id de `StringWriter`, le resuelve `toString()` por vtable y le contesta. Con la
copia **anterior a la escritura**. Por eso el sintoma era texto vacio y no un crash: la respuesta
era plausible, que es la peor forma de estar mal.

El arreglo no es un caso especial del Filer. La VM ya tenia el mecanismo para esto —el cache de
condy son referencias que la VM sostiene y ningun frame ve, y por eso viajan al colector como una
lista que cada fase que mueve objetos **reescribe en el lugar**. El canal del Filer es exactamente
lo mismo, asi que entra por la misma puerta: `condy_roots` paso a llamarse `vm_roots` (el nombre
viejo ya seria mentira), junta las dos fuentes en una sola lista de offsets, y `restore_vm_roots`
devuelve cada tramo a donde estaba. `gc_mark` y `gc_sweep` tambien la usan: no mueven nada, pero
sin el writer en la raiz lo **liberarian**.

Un detalle del test que importa mas que el arreglo: fallaba solo porque el programa alcanzaba a
llenar Eden por su cuenta. Eso no es una prueba, es una casualidad —y explica por que el finding
aparecio cuando aparecio, con una `StringBuilder` que asigna de verdad y un `arraycopy` que copia
de verdad detras. El test ahora pide la colecta **explicitamente** antes de drenar, y sabotear la
raiz (devolver una lista vacia de writers) lo pone en rojo con el mismo `("Foo", "")` de siempre.

### #279 -- ⬜ una llamada con un argumento `T[]` se declara ambigua

```java
public static <T> Spliterator<T> deArreglo(T[] array) {
    return Spliterators.spliterator(array, 0);   // error: la referencia a `spliterator` es ambigua
}
public static Spliterator<String> deStrings(String[] array) {
    return Spliterators.spliterator(array, 0);   // compila
}
```

`Spliterators.spliterator` tiene cinco sobrecargas de dos argumentos: `Object[]`, `int[]`,
`long[]`, `double[]` y `Collection<? extends T>`. Con `String[]` nuestro javac elige bien. Con
`T[]` —un arreglo de una variable de tipo— dice que no puede elegir.

No hay nada que elegir: `T[]` es un tipo arreglo, convierte a `Object[]` por conversion de
referencia por ampliacion, y no convierte a ninguna de las otras cuatro. El javac real lo compila
sin chistar (verificado con el JDK 25 sobre el mismo fuente). El defecto es de la fase 1 de
resolucion de sobrecargas: parece tratar `T[]` como si fuera aplicable a mas de un candidato, o
como si no fuera aplicable a ninguno y cayera a un desempate que no puede resolver.

Rodeo en `java/util/Arrays.java`, en las dos sobrecargas genericas de `spliterator`: un local
`Object[] widened = array;` antes de la llamada. Nombrar el tipo del parametro es exactamente lo
que el compilador no dedujo.

### #280 -- ❌ no existio: era el script que escribia el metodo en la clase equivocada

Se anoto en vivo que `this` no resolvia contra un parametro `Collection<? extends T>` —dieciseis
colecciones fallaban con "una llamada que no resolvio a ningun metodo", y el rodeo del local
tipado fallaba con "tipo incompatible en `self`"—. No habia tal cosa.

El script que cableo `spliterator()` insertaba el metodo antes del **ultimo** `\n}` del archivo, y
media docena de estas clases llevan una clase package-private de apoyo detras de la publica: el
iterador de `ArrayList`, el de `TreeSet`. El metodo caia adentro del iterador. Ahi `this` es un
`ArrayListItr`, que efectivamente no es una `Collection` y efectivamente no tiene `size()`. El
compilador tenia razon las dieciseis veces, y con un mensaje exacto.

Queda anotado porque el rodeo llego a escribirse en el arbol, con un comentario que explicaba un
defecto inexistente. Un comentario asi es peor que ninguno: el que lo lea despues le va a creer, y
va a arrastrar el rodeo a codigo nuevo. Se saco de las veinte clases. **El sintoma acusaba al
compilador y el error estaba en la herramienta que lo media** — que es lo mismo que dice #201
sobre un reporte que envejece con su referencia, visto desde el otro lado.
