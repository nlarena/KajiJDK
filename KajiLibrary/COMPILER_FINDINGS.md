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

### #274 -- ✅ un nombre CALIFICADO no se reconocia como tipo dentro de una expresion

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

Repro: `repros/finding_274.java`.

**Arreglado** (2026-08-30, tanda q): ver esa tanda al pie. Es la reclasificacion del §6.5.2, y va
en `attribute.rs` como `reclassify_as_type`, llamada antes de atribuir el receptor de un acceso a
campo o de una llamada. El rodeo que se usaba --`import` y nombre simple-- ya no hace falta.

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


---

### #281 -- ✅ javac no puede *crear* arreglos multidimensionales (el tipo si existe, y la VM ya emite `multianewarray`)

Salio escribiendo `java.util.ListResourceBundle`, cuyo `getContents()` devuelve `Object[][]` por
contrato del JDK.

Tres formas fallan, y fallan distinto:

| forma | error |
|---|---|
| `new Object[][] { { "k", "v" } }` | `se esperaba una expresion, se encontro LBrace` (parser) |
| `new Object[2][3]` | `tipo incompatible en \`a\`` |
| `new int[2][]` | `tipo incompatible en \`a\`` |

Lo que **si** anda, que es lo que acota el hallazgo:

- El **tipo** `Object[][]` como campo, parametro o retorno. El descriptor emitido es
  `[[Ljava/lang/Object;`, **identico** al del JDK — comprobado sobre nuestra
  `ListResourceBundle.getContents`.
- `.length` sobre un parametro bidimensional.
- Los arreglos de **una** dimension, en las dos sintaxis: `new T[] { ... }` y `T[] a = { ... }`.

O sea: el sistema de tipos representa el arreglo multidimensional; lo que no existe es la
**expresion de creacion**. El parser no acepta el inicializador anidado, y la creacion con dos
dimensiones no produce el tipo que la declaracion espera.

**La VM no es el problema.** `multianewarray` (0xc5) esta implementado en
`src/jvm/interpreter/bytecode_interpreter/array_operations.rs:101` y despachado en
`bytecode_interpreter.rs:3473`. Un `grep` por `multianewarray` sobre `src/javac/` no devuelve
nada: el emisor nunca lo produce. La mitad cara ya esta pagada — falta el frente.

Repro: `KajiLibrary/repros/finding_281.java` (el JDK 25 lo compila sin una sola queja).

**Donde muerde hoy:** `ListResourceBundle` compila y su superficie coincide 5/5 con el JDK, pero
**ninguna subclase se puede escribir con nuestro propio javac**, porque toda subclase tiene que
construir el `Object[][]` que devuelve `getContents()`. La clase quedo verificada contra `java`
real en vez de contra la nuestra. Es el primer caso en el que una clase de la biblioteca esta
completa y correcta y aun asi es **inusable desde fuentes propias** por un hueco del compilador.


---

### #282 -- ✅ la concatenacion de una referencia no-String produce basura en silencio (y con dos, lee fuera del heap)

Salio escribiendo `java.util.ResourceBundle`, cuyo mensaje de error concatena un `Locale`.

```java
String s = "" + new java.util.Locale("es");                        // java real: "es" (2)
                                                                   // el nuestro: 168 chars de basura
String t = "" + new java.util.Locale("es") + new Locale("es");     // el nuestro: panic
//   heap.rs:878: range end index 4672 out of range for slice of length 4670
```

**Compila sin una sola queja.** Los caracteres devueltos son NUL y valores arbitrarios: es memoria
del objeto leida como UTF-16.

La causa se ve comparando los dos emisores para la misma linea:

| | javac del JDK 25 | el nuestro |
|---|---|---|
| antes del call site | `invokestatic String.valueOf(Object)` | **nada** |
| descriptor del indy | `(Ljava/lang/String;)Ljava/lang/String;` | `(Ljava/util/Locale;)Ljava/lang/String;` |

El JDK convierte a `String` **antes** del `invokedynamic`. Nosotros mandamos el objeto crudo. Del
otro lado, `render` en `src/jvm/interpreter/bytecode_interpreter/invokedynamic.rs` hace
`strings::read(heap, offset)` sobre **cualquier** referencia no nula — o sea lee los campos del
`Locale` como si fueran el `char[]` de un `String`. Con un operando eso da basura; con dos, el
offset cae afuera del old-gen y revienta en `read_u16`.

Lo notable: **el comentario de `render` ya habia previsto esto textualmente** — *"javac never sends
anything else — it inserts `String.valueOf(Object)` before the call site — but another compiler
could, ours included"*. Ours does. La suposicion estaba escrita y nadie la verifico contra el
propio emisor.

**No es duplicado de #114, es su sucesor.** #114 era el error **duro** por falta de
`StringBuilder.append(Object)`, y se dio por cerrado con el argumento de que *"la familia entera
desaparecio con el cambio de estrategia"* a `invokedynamic` (ver el cierre de lote del 2026-08-25).
El cambio de estrategia **saco el sintoma viejo y metio este**, que es peor: antes no compilaba,
ahora compila y corrompe. Es el modo de falla que #114 llamaba "la variante silenciosa", pero la
causa real no era el *stack underflow* que se anoto entonces.

Alcance, acotado por los controles del repro: **solo** las referencias no-`String`. Los operandos
`String` andan (`"" + s` → correcto) y los primitivos tambien (`"a" + 5 + true` → 6). `String.valueOf(Object)`
por si solo tambien anda (→ 2), que es justamente lo que falta insertar.

Dos lados que arreglar, y conviene los dos:

1. **javac** (la causa): insertar `String.valueOf(Object)` para todo operando de referencia que no
   sea ya `String`, como hace el JDK.
2. **VM** (la defensa): `render` no deberia leer una referencia arbitraria como `String`. Con el
   descriptor a mano puede detectar que no es `Ljava/lang/String;` y fallar fuerte — la misma regla
   que #114 y #20 ya pedian: mejor "no compila" o "revienta aca" que "gatea y revienta lejos".

Repro: `KajiLibrary/repros/finding_282.java` (cinco metodos: los dos que fallan y tres controles).


---

### Cierre de #281 y #282, y lo que destaparon (2026-08-29)

**#281 — arreglado**, en tres puntos:

- `attribute.rs`: el tipo de un `new` de array se envuelve **una vez por cada `[]`**, no una sola.
  Era lo que tipaba `new int[2][3]` como `int[]` y hacia que `int[][] a = …` lo rechazara.
- `codegen.rs`: mas de una dimension **con tamano** emite `multianewarray` (0xc5). El opcode ya
  estaba en la VM desde siempre; solo faltaba producirlo.
- `parser.rs`: los elementos de un `{ … }` de creacion se leen con `var_init` y no con `expr`,
  que es quien sabe abrir una llave anidada y quitarle una dimension al tipo.

**Correccion al alcance que se anoto:** el finding decia que el inicializador anidado no parseaba,
a secas. Falso a medias — `int[][] a = { {1,2}, {3,4} }` **ya funcionaba**, porque la forma
declarativa pasa por `var_init`. Lo que no parseaba era solo `new int[][] { {1,2} }`. Se comprobo
antes de tocar nada, y por eso el arreglo del parser fue de una linea y no de un parser nuevo.

Verificado: las ocho formas (declarativa, `new` con inicializador, `[2][3]`, `[2][]`, tres
dimensiones, y sus variantes con referencias) dan el mismo valor que `java` real, y
`new int[2][3]` emite `multianewarray #N, 2 // class "[[I"`, identico al JDK.

**#282 — arreglado**, en los dos lados que el propio finding pedia:

- `desugar.rs` + `codegen.rs`: un operando de referencia que no sea ya `String` viaja al call site
  **convertido**, con `invokestatic String.valueOf(Object)` delante y `Ljava/lang/String;` en el
  descriptor. El bytecode de `"" + locale` sale ahora instruccion por instruccion igual al del
  JDK 25. El predicado (`concat_needs_value_of`) es **uno solo**, usado por el emisor y por el
  descriptor, para que los dos lados no puedan discrepar.
- `invokedynamic.rs`: `render` ya no lee una referencia arbitraria como si fuera un `String`.
  Ahora exige `Ljava/lang/String;` y falla con un mensaje que nombra el descriptor que llego y el
  finding. La suposicion que estaba escrita en el comentario pasa a estar **comprobada en runtime**.

Esa guarda se gano el sueldo en el acto: al correr las pruebas contra la biblioteca todavia
compilada con el javac viejo, salto y dijo exactamente que `.class` estaba atrasado.

---

### #283 -- ✅ un local declarado dentro de un `synchronized` pisaba el slot del monitor

Salio de recompilar la biblioteca entera con el `javac` de `src/` despues de arreglar #281 y #282:
`TimerTask.cancel()` — que es `synchronized (lock) { boolean result = …; return result; }` —
reventaba con `monitorexit: expected an object reference on the stack`.

El bytecode lo decia entero:

```
  5: astore_1      <-- la referencia bloqueada, al slot 1
  6: monitorenter
 11: istore_1      <-- el local `result`, AL MISMO SLOT: pisa la referencia
 13: aload_1       <-- carga un int
 14: monitorexit   <-- y explota
```

**Causa:** el codegen reserva dos slots por nivel de `synchronized` (el objeto del monitor y el
aparcadero de excepcion del handler) justo encima de los parametros, y asume que los locales del
cuerpo empiezan por encima de esa region — el comentario de `note_local` incluso dice "ya
reubicado". Pero quien reparte los slots es la **atribucion**, que numeraba desde el primer libre
tras los parametros sin saber de la reserva. Las dos mitades venian de acuerdo por casualidad
hasta que se cambio la emision de `synchronized`.

Arreglado en `attribute.rs` con `attrib_body`, que hace **la misma** reserva antes de recorrer el
cuerpo, en los cuatro puntos de entrada (metodo, constructor, inicializador static y de instancia,
y metodo de clase local). Un helper unico, para que no se vuelvan a desincronizar por separado.

**Por que no lo vio nadie antes:** hace falta un local **declarado adentro** del bloque. Un
`synchronized` que solo toca campos — la forma de casi todos los casos de prueba — no pide
ningun slot nuevo y sale correcto. `TimerTask.cancel()` fue el primer cuerpo de la biblioteca que
declaro uno.

**Es una regresion posterior al snapshot congelado**, asi que `bin/javac.exe` **no** la reproduce:
el repro `repros/finding_283.java` da 7/7/14 con el congelado y con el arreglado, y solo fallaba
con el `javac` intermedio. Queda como regresion, no como demostracion.

**De paso, un defecto viejo que el cambio de emision ya habia arreglado sin anotarse:** el javac
congelado **no libera el monitor en el retorno normal** — emite `ireturn` desde adentro del
bloque y solo hace `monitorexit` por el handler de excepcion. Un `return` dentro de un
`synchronized` se llevaba el monitor puesto. La emision actual hace `aload; monitorexit; ireturn`,
que es lo correcto (JVMS §3.14).

---

### Nota de estado tras estos tres arreglos

`bin/*.exe` **quedo atras**: la biblioteca esta compilada con el `javac` de `src/`, no con el
congelado. Recompila 1000/1001 en el sitio, con el unico fallo siendo el `SymElement` de siempre
(que no es del compilador: la clase no implementa `asType`, y `javac` tiene razon). Regenerar el
snapshot segun `bin/FROZEN.md` es el paso pendiente, y hay que hacerlo **despues** de commitear,
porque la receta parte de un `git archive HEAD` limpio.


---

### #284 -- ⬜ no se chequea un metodo abstracto heredado de una superclase del **classpath**

Una clase concreta que no implementa un metodo abstracto de su superclase compila igual, en
silencio, **si la superclase viene del classpath**. Con las dos clases en el mismo archivo el
chequeo si dispara — que es exactamente lo que lo hacia invisible.

```java
// finding_284_base.java, compilado APARTE
public abstract class finding_284_base { public abstract int f(); }

// finding_284.java
public class finding_284 extends finding_284_base { }   // el nuestro: lo acepta
```

El JDK 25 sobre el mismo par:

```
error: finding_284 is not abstract and does not override abstract method f() in finding_284_base
```

Lo que **si** se detecta, y acota el hallazgo a un solo camino:

- La misma situacion con las dos clases en el **mismo archivo** → error correcto, con buen
  mensaje.
- Un metodo abstracto de una **interfaz** del classpath → error correcto. Es el diagnostico que
  da hoy `jdk/internal/apt/SymElement.java` (*"no es abstracta y no implementa `asType` de
  `TypeElement`"*), el unico fallo de compilacion de toda la biblioteca.

O sea: el chequeo existe y funciona. Lo que no mira son los metodos abstractos que llegan por
**herencia de clase** desde el classpath.

**Como salio.** Colgando `HashMap` de `AbstractMap` para que heredara `values()`. Nuestro
`AbstractMap` declara `entrySet()` abstracto y `HashMap` no lo implementa: compilaron las dos, y
tambien compilo un `new HashMap<>().entrySet()`. En Java real ese programa **no compila**; aca
compila y queda para reventar en runtime.

Es el modo de falla mas caro de todos, y el mismo patron que #282: el compilador **acepta** algo
que no deberia, y el sintoma aparece lejos del error. Un `javac` que rechaza es util; uno que
acepta y deja el problema para el runtime es peor que uno que no compila nada.

Repro: `KajiLibrary/repros/finding_284.java` + `finding_284_base.java` (hay que compilarlos por
separado, porque el defecto es justamente ese).


---

### #285 -- ⬜ una llamada generica anidada como argumento no resuelve si el destino es un generico parametrizado por variables de tipo

```java
final class C<K, V> {
    void f(K k, V v) {
        HashSet<Map.Entry<K, V>> out = new HashSet<Map.Entry<K, V>>();
        out.add(Map.entry(k, v));      // error: una llamada que no resolvio a ningun metodo
    }
}
```

El caret apunta al **`add`**, no al `entry`: el argumento no se pudo tipar, asi que ningun
candidato de `add` resulto aplicable. `HashSet.add` tiene **un solo** candidato, o sea que no hay
nada que desambiguar.

Lo que decide si falla es el **tipo del parametro destino**, no la llamada anidada:

| destino de `add` | con llamada generica anidada |
|---|---|
| `String` (concreto) | compila |
| `V` (variable de tipo suelta) | compila |
| `Map.Entry<K, V>` (generico parametrizado por variables) | **falla** |

Hace falta que el destino sea un tipo **generico parametrizado por variables de tipo**. Con el
mismo destino y un local tipado en medio anda:

```java
Map.Entry<K, V> e = Map.entry(k, v);
out.add(e);                            // compila
```

Nombrar el tipo es exactamente lo que la inferencia no dedujo.

**No es #279**, aunque el rodeo sea el mismo. Aquel es una **ambiguedad** entre cinco sobrecargas
con un argumento `T[]`; aca hay un solo candidato y el mensaje es "no resolvio a **ningun**
metodo". Alli sobra informacion y no se elige; aca falta y no se llega a tipar.

**Tampoco es #274**: con el nombre completamente calificado (`java.util.Map.entry(...)`) lo que
salta primero es #274, que es otro defecto. El repro importa `Map` justamente para aislar este.

Salio agregandole `entrySet()` a las implementaciones de `Map` de **fuera** de `java.util`
(`ConcurrentHashMap`, `FrozenMap`, `StableMap`), que tienen que construir sus pares con
`Map.entry` porque `FixedEntry` es package-private. Las de `java.util` no lo tocan: usan
`new FixedEntry<K, V>(...)`, que es un `new` y no una llamada generica.

Repro: `KajiLibrary/repros/finding_285.java` (dos metodos que fallan y dos controles que
compilan; el JDK 25 compila los cuatro).


---

### #286 -- ⬜ un lambda en linea no liga su parametro si el destino es `X<? super E>` con E del receptor

```java
class C<E> {
    E v;
    boolean f(Predicate<? super E> p) { return p.test(this.v); }
}

C<String> b = new C<String>();
b.f(s -> s.length() == 2);      // error: una llamada que no resolvio a ningun metodo
//       ^ `s` quedo sin tipo
```

Hacen falta **dos** cosas a la vez; con una sola no pasa nada:

| destino | resultado |
|---|---|
| `Predicate<String>` (concreto) | compila |
| `Predicate<E>`, E del receptor | compila |
| `Predicate<? super T>`, T de **otro argumento** | compila |
| `Predicate<? super E>`, E **del receptor** | **falla** |

O sea: el comodin `? super` **y** que la variable venga del argumento de tipo del receptor.

Poner el tipo del parametro explicito (`(String s) -> …`) **no alcanza** — sigue fallando. Eso
descarta que sea la inferencia del parametro del lambda: lo que no llega a tiparse es la llamada
entera.

**Rodeo:** sacar el lambda de la posicion de argumento.

```java
Predicate<String> p = s -> s.length() == 2;
b.f(p);                                        // compila
```

Es el mismo rodeo que #285 y #279 —nombrar el tipo en un local— pero el mecanismo es distinto:
alli una llamada generica anidada y una ambiguedad de sobrecargas; aca un lambda. Los tres
comparten sintoma y cura, y valdria la pena mirarlos juntos: huelen a la misma fase de
inferencia con el tipo destino.

**Donde muerde, que es lo que lo hace caro:** `Collection.removeIf(Predicate<? super E>)`,
`Collection.forEach(Consumer<? super E>)`, `Map.forEach(BiConsumer<? super K, ? super V>)`,
`Map.merge`, `Map.compute*`, `List.sort(Comparator<? super E>)`. Los `default` que el JDK 8+ le
agrego a las colecciones usan `? super` casi sin excepcion, asi que

```java
lista.removeIf(s -> s.length() == 2);
```

—codigo Java de todos los dias— no compila. La biblioteca **si** los declara y los implementa
correctamente; lo que no se puede es *llamarlos con un lambda en linea*, que es como se llaman
siempre.

Repro: `KajiLibrary/repros/finding_286.java` (uno que falla, el rodeo, y tres controles que
compilan; el JDK 25 compila los cinco).


---

### #287 -- ⬜ una llamada estatica calificada, hecha desde una interfaz con un homonimo, ignora el calificador

```java
interface I<E> {
    static <E> I<E> of(E a, E b) { return null; }
    static <E> I<E> of() {
        return Ayudante.of(new Object[0], 0);    // deberia llamar a Ayudante.of(Object[], int)
    }
}
final class Ayudante<E> implements I<E> {
    static <E> Ayudante<E> of(Object[] a, int n) { return new Ayudante<E>(); }
}
```

Lo que emite cada uno para el cuerpo de `of()`:

| javac del JDK 25 | el nuestro |
|---|---|
| `anewarray Object` | `anewarray Object` |
| `iconst_0` | `iconst_0` |
| `invokestatic Ayudante.of([Object;I)` | `invokestatic Integer.valueOf(I)` ← **boxea** |
| `areturn` | `invokestatic of:(Object;Object;)` ← **la propia interfaz** |

El calificador `Ayudante.` **se ignora**: se elige `of(E, E)` de la interfaz misma, boxeando el
`int` para que encaje en el segundo parametro. Y como `of(E, E)` vuelve a llamar a
`Ayudante.of(...)`, que vuelve a resolver a `of(E, E)`, el programa recursa hasta
`StackOverflowError`.

**El caso analogo entre dos clases resuelve bien.** Una clase con `f(Object, Object)` que llama a
`Otra.f(Object[], int)` emite la llamada correcta. Lo que lo dispara es que el llamador sea una
**interfaz** con estaticos homonimos.

Es el modo de falla mas caro que hay: no hay error de compilacion, el bytecode es valido y pasa el
verificador, y el sintoma —un `StackOverflowError` sin nada del metodo que se quiso llamar— no
apunta ni de lejos a la causa. Costo un rato largo llegar desde "`Set.of()` devuelve `None`" hasta
esto.

**Como salio:** `Set.of()` y `Map.of()`. Sus cuerpos llamaban a `FixedSet.of(...)` y
`FixedMap.of(...)`, ayudantes package-private con firma propia, y los dos terminaban llamandose a
si mismos. `List.of` **no** se vio afectado, y la razon es iluminadora: su cuerpo usa
`new FixedList<E>(a)` — un constructor, no una llamada estatica.

**Rodeo aplicado:** renombrar los ayudantes para que no sean homonimos (`FixedSet.fromArray`,
`FixedSet.dedup`, `FixedMap.fromPairs`). Son nombres internos, asi que no toca el contrato — pero
es un rodeo, no un arreglo: cualquier interfaz futura con un estatico homonimo de un ayudante
vuelve a caer.

Repro: `KajiLibrary/repros/finding_287.java` (el que falla y un control con el ayudante sin
homonimo, que emite bien).

---

### #288 -- ⬜ referencia stale que llega al GC en modo verde: `young_info` sin la clave

```
thread 'main' panicked at src/jvm/interpreter/gc.rs:189:42:
no entry found for key
```

`gc.rs:189` es `evacuate`, en `let (size, age) = self.young_info[&obj];`. El colector recibio un
offset que **no corresponde a ningun objeto joven conocido**: una referencia que sobrevivio a una
coleccion anterior sin actualizarse, o que nunca fue un objeto.

**Determinista** — falla 3 de 3, siempre en la misma linea — y en **modo verde**, un solo hilo,
asi que no es la carrera de os-parallel.

**No es del codigo nuevo de colecciones.** El mismo programa revienta igual con el
`bin/run-headless.exe` congelado, que es **anterior** a toda esta tanda de cambios. Es un defecto
preexistente que aparecio recien ahora porque hasta ahora no habia suficiente API implementada
como para escribir un programa que alocara lo bastante.

**El lugar del crash no es el lugar del bug.** Cada pedazo del repro corre bien por separado:

| | |
|---|---|
| las factorias `List.of`/`Set.of`/`Map.of` solas | anda |
| el bloque de `default` de `Map` solo | anda, y da lo mismo que `java` real |
| `HashMap` put/remove/values por separado | anda |
| 400 `HashMap` con `entrySet()`/`values()` en bucle | anda |
| alocacion pura de 400 strings | anda |

Lo que hace falta es el **volumen acumulado**. La reduccion automatica —el prefijo mas corto que
falla— corta justo en `m.values()`, despues de dos `remove(k, v)`. Esa linea no es la culpable: es
donde el GC toca por primera vez con la referencia ya podrida.

Para el que lo agarre: el sospechoso es algo que guarda un offset del heap a traves de una
coleccion sin estar en las raices. Misma familia que el hueco de `pending_exception` —cerrado con
el parking en la pila de operandos— y que el bug de os-parallel. La diferencia, y es una buena
noticia, es que **este se reproduce en verde y a la primera**: no hace falta modo tortura.

Repro: `KajiLibrary/repros/finding_288.java`.


---

### #289 -- ✅ falta el autoboxing en un inicializador de arreglo de tipo referencia

```java
Object[] a = { "x", 7 };      // compila; emite `aastore` con un `int` crudo
```

Compila sin una queja y emite **bytecode invalido**. Nuestra VM lo caza al ejecutar
(`array_operations.rs:777: expected a reference, found Int(7)`); un JVM de verdad lo rechazaria en
la verificacion.

| javac del JDK 25 | el nuestro |
|---|---|
| `bipush 7` | `bipush 7` |
| `invokestatic Integer.valueOf(I)` | *(nada)* |
| `aastore` | `aastore` ← un `int` donde va una referencia |

El autoboxing **si** funciona en los demas contextos, y eso acota el hallazgo a un solo camino:

| contexto | |
|---|---|
| `Object o = 7;` (asignacion) | boxea bien |
| `toma(7)` con parametro `Object` (argumento) | boxea bien |
| `Object[] a = { 7 };` (inicializador) | **no boxea** |

O sea que la conversion existe y esta implementada; lo que no la aplica es el camino del
inicializador de arreglo, que emite el `aastore` sin mirar si el elemento necesita convertirse al
tipo del componente.

Es el mismo modo de falla de #282 y #287: **compila, el sintoma aparece lejos**. La diferencia a
favor es que aca revienta en el primer uso y no corrompe memoria.

Como salio: escribiendo `java.util.Currency`, cuya tabla arranco siendo un `Object[]` con los
codigos ISO y los numeros mezclados. Se reescribio con arreglos paralelos —que ademas es mejor
diseno: sin boxeo, sin casts y sin la aritmetica de indices en tripletes— pero el defecto queda.

Repro: `KajiLibrary/repros/finding_289.java` (el que falla y tres controles que pasan; el JDK 25
compila los cuatro).


**Arreglado** (2026-08-29), en `transtypes.rs`, y el arreglo es de una linea util.

La pasada ya recorria el `NewArray` y ya convertia sus **dimensiones** a `int`
(`self.coerce(d, &RType::Prim(PrimType::Int))`). A los elementos del inicializador, en cambio,
solo los recorria con `self.expr(...)` — sin `coerce`. Faltaba llamar a la misma conversion que
ya usaban la asignacion y los argumentos, que es exactamente por lo que esos dos contextos si
boxeaban.

El tipo del componente sale de `e.ty`, leido **antes** de prestar `e.kind`; es el mismo rodeo que
el caso de la lambda ya tenia unas lineas mas arriba, con su comentario explicando por que.

El bytecode que emite ahora es identico al del JDK 25, offset por offset:

```
11: bipush 7
13: invokestatic Integer.valueOf(I)
16: aastore
```

Repro verificado en los cuatro metodos (7 / 7 / 7 / 2). La biblioteca sigue en 1024/1026 y las
trece pruebas de comportamiento contra `java` real siguen dando el mismo entero.

**Nota sobre `Currency`:** su tabla se habia reescrito con arreglos paralelos por este defecto.
Se **deja asi**: sin boxeo, sin casts y sin aritmetica de indices en tripletes es mejor diseno de
todos modos. Lo que se corrigio es el comentario, que decia que la forma con `Object[]` "no
compila bien hoy" — ya no es cierto.


---

### #290 -- ✅ un arreglo de primitivos se consideraba convertible a `T[]`

```java
int[] x = { 1, 2 };
int[] y = { 1, 2, 3 };
Arrays.compare(x, y);     // error: la referencia a `compare` es ambigua
```

Es Java corriente y el JDK 25 lo compila. Los dos candidatos de dos argumentos son
`compare(int[], int[])` y `<T extends Comparable<? super T>> compare(T[], T[])`, y no hay nada que
desambiguar: un arreglo de primitivos no convierte a `T[]` para ninguna variable de tipo de
referencia, asi que el primero es el unico aplicable.

**Alcance, y es angosto.** De las diez familias de `Arrays` que toman `int[]`, **solo `compare`**
falla:

| familia | |
|---|---|
| `compare(int[], int[])` | **ambigua** |
| `compareUnsigned`, `mismatch`, `equals`, `sort`, `fill`, `binarySearch`, `copyOf`, `toString`, `hashCode` | resuelven bien |

Lo que `compare` tiene de particular es ser la unica con una sobrecarga **generica de dos
argumentos** al lado de las primitivas de dos; en `equals` y `mismatch` la generica lleva un
`Comparator` y es de tres.

**Cuando aparecio:** al completar `Arrays` de 34 a 214 miembros. Con las dos sobrecargas de
`compare` que habia antes — **las mismas dos** — la llamada compilaba. No es el par lo que
confunde al resolutor, es el conjunto ampliado alrededor.

**Lo que NO reproduce**, y conviene que este anotado porque acota donde buscar: una clase sintetica
con la misma forma de sobrecargas —las ocho primitivas de dos argumentos, las ocho de seis, la
generica de dos con cota `Comparable`, la generica de seis, y las dos con `Comparator`— compila
sin problema, en el mismo archivo y desde el classpath. La forma del conjunto no alcanza para
disparar el defecto; algo del `java.util.Arrays` real si. Ahi esta la pista.

Familia de **#279** (una llamada con un argumento `T[]` se declara ambigua) vista desde el otro
lado: alli el argumento era `T[]`, aca es un `int[]` concreto al que se le ofrece un candidato
`T[]` que no deberia ser aplicable.

**Rodeo:** la forma de rango, que no es ambigua —
`Arrays.compare(x, 0, x.length, y, 0, y.length)`.

**No se saco la sobrecarga generica para esquivarlo.** El JDK la tiene y sacarla seria un hueco de
contrato para tapar un defecto del compilador; el hueco duraria mas que el defecto.

Repro: `KajiLibrary/repros/finding_290.java` (el que falla, el rodeo y los nueve controles; el JDK
compila los tres metodos).


**Arreglado** (2026-08-29). Y la ambiguedad de `compare` era la cara **benigna**: la otra es que
`Arrays.copyOf(int[], int)` **compilaba** eligiendo `<T> T[] copyOf(T[], int)`, emitia la llamada a
la sobrecarga de `Object[]` y le ponia encima un `checkcast [Ljava/lang/Integer;`. Reventaba en el
primer `aastore` — `array_operations.rs:558: value header does not point at a known class`.

**Causa**, en `attribute.rs`, y el mismo error en dos funciones (`convertible`, que usa la
resolucion de sobrecargas, y `assignable`, que usa el chequeo de asignaciones): el brazo
`(Array(a), Array(b))` recursaba sobre los elementos sin mirar si uno era primitivo, y la
recursion caia en el brazo de **boxing** (`int` → `Integer` → `T`) o en la **indulgencia** con las
variables de metodo, que existe a proposito para que la inferencia las resuelva despues.

Adentro de un arreglo ninguna de las dos corresponde: la covarianza del §4.10.3 vale **solo entre
tipos referencia**, y una variable de tipo **solo liga tipos referencia** (§4.5.1). El arreglo es
la regla exacta — si alguno de los dos elementos es primitivo, tienen que ser el mismo primitivo.

Tambien se cerro el mismo agujero en `infer.rs` (`‹S[] → α[]›` con `S` primitivo ahora reduce a
false en vez de boxear), aunque por si solo no alcanzaba: el filtro de inferencia **no corre sobre
clases del classpath**, que es justo el caso de `java.util.Arrays`.

---

### Lo que destapo completar `Arrays`: cinco stubs que el conteo por firma daba como presentes

Al ejercitar `Arrays` por primera vez aparecieron **cinco metodos que existian y no hacian nada**:

| metodo | cuerpo que tenia |
|---|---|
| `binarySearch(int[], int, int, int)` | `return 0;` |
| `mismatch(int[], int[])` | `return 0;` |
| `compare(int[], int[])` | `return 0;` |
| `<T> mismatch(T[], T[], Comparator)` | `return 0;` |
| `<T extends Comparable> compare(T[], T[])` | `return 0;` |

Y un sexto que era peor que un stub: `binarySearch(int[], int)` devolvia **el valor** en vez del
indice cuando encontraba, y recursaba con las ramas **invertidas**.

Vale la pena que quede anotado como limite del metodo de medicion: el diff de API compara
**firmas**, asi que un stub cuenta como presente. Los seis figuraban en el 34/214 con el que
`Arrays` arrancaba. La unica forma de encontrarlos era correrlos — que es lo que hace la prueba de
comportamiento contra `java` real, y por eso vale mas que el conteo.


---

## Tanda 2026-08-29 (m) -- `Collections` completa, y la igualdad que no existia

`Collections` pasa de **5/74 a 74/74**. Los 69 miembros nuevos se agrupan en cuatro familias, y de
las cuatro salio un solo hallazgo -- pero de los grandes, porque no estaba en `Collections` sino
debajo de toda la biblioteca.

| familia | cuantos | donde vive la maquinaria |
|---|---|---|
| vacias y de un elemento | 17 | `FixedList`/`FixedSet`/`FixedMap` ya existentes, + `EmptySortedSet.java` |
| envoltorios `unmodifiable*` / `synchronized*` / `checked*` | 28 | `GuardedCollection.java`, `GuardedMap.java` |
| algoritmos | 19 | en `Collections` mismo |
| puentes | 5 | `SetFromMap.java` |

### Una sola familia de envoltorios en vez de tres

El JDK tiene una clase por familia **y** por interfaz: `UnmodifiableList`, `SynchronizedList`,
`CheckedList`, y lo mismo por cada una de las ocho interfaces de coleccion. Son mas de veinte
clases que delegan igual.

Aca hay **una** familia con tres interruptores, porque las tres hacen exactamente lo mismo
--delegar-- y solo se diferencian en que hacen ANTES:

| interruptor | efecto |
|---|---|
| `readOnly` | los mutadores tiran `UnsupportedOperationException` en vez de delegar |
| `type` | `add`/`set` validan la clase del elemento y tiran `ClassCastException` en el acto |
| *(cerrojo)* | todo pasa por el monitor de `this` |

El cerrojo merece la aclaracion que esta escrita en el archivo: **todos** los envoltorios toman el
monitor, no solo los de `synchronized*`. En los de solo lectura es un monitor que nadie mas mira,
asi que cuesta un `monitorenter` sin contienda, y a cambio evita escribir cada metodo dos veces.
Para el que si sincroniza, el monitor es el envoltorio mismo -- que es lo que el contrato exige,
porque `synchronized (lista) { for (X x : lista) ... }` es la unica forma de proteger una recorrida
y solo funciona si la coleccion sincroniza sobre si misma.

Lo que estos envoltorios tapan y es facil pasar por alto son las **vias laterales**: un
`unmodifiable*` que solo niegue `add` y `remove` no sirve de nada si deja pasar

```java
unmodifiableList(l).iterator().remove();
unmodifiableList(l).subList(0, 1).add(x);
unmodifiableMap(m).keySet().remove(k);
unmodifiableMap(m).entrySet().iterator().next().setValue(v);   // la mas fina
```

Las cuatro estan cubiertas y las cuatro estan en la prueba de comportamiento. La ultima obliga a
envolver tambien las entradas, porque `Map.Entry.setValue` escribe en el mapa sin pasar por ninguna
de sus vistas.

### #291 -- ✅ (biblioteca) `java.lang.Iterable` no tenia `forEach`

```java
lista.forEach(x -> System.out.println(x));   // error: no se encuentra el metodo: forEach
```

Era el unico miembro publico de `Iterable` que faltaba, y como `Collection extends Iterable`, el
efecto no era local: **ninguna coleccion de la biblioteca tenia `forEach`**. Ni `ArrayList`, ni
`HashSet`, ni `ArrayDeque` -- ninguna de las quince.

Se noto escribiendo `GuardedCollection`, que necesita interceptarlo para que un envoltorio
sincronizado lo tome bajo el monitor.

Dos detalles del estado en que estaba:

- `LinkedBlockingDeque` y `LinkedTransferQueue` **ya lo declaraban** (`public void
  forEach(Consumer<? super E>)`) creyendo que sobreescribian algo. No sobreescribian nada; ahora
  si.
- `Map.forEach(BiConsumer)` es otro metodo, no relacionado, y ese si estaba.

**Arreglado**: `default void forEach(Consumer<? super T>)` en `Iterable`, con el cuerpo de siempre
--recorrer el iterador y llamar a `accept`--. Va como `default` por la razon de siempre: declararlo
abstracto obligaria a escribirlo en cada implementor, y el cuerpo seria este mismo.

Repro: `KajiLibrary/repros/finding_291.java`.

### #292 -- ✅ (biblioteca) `AbstractList` y `AbstractMap` no tenian `equals` ni `hashCode`

Este es el que importa, y estuvo escondido todo este tiempo por una razon que vale la pena dejar
anotada.

```java
new ArrayList<String>(List.of("x")).equals(new ArrayList<String>(List.of("x")))   // era false
new HashMap<String,String>(Map.of("k","v")).equals(new LinkedHashMap<...>(...))   // era false
List.of("x").equals(List.of("x"))                                                 // era false
```

Lo que se heredaba de `Object` es la igualdad por **identidad**. Consecuencias, todas reales:

- ninguna `List` ni ningun `Map` servia de clave de otro mapa;
- `assertEquals` sobre listas no podia funcionar;
- `List.of("x").equals(List.of("x"))` daba false, que es de las que hacen dudar de la propia cabeza;
- y la simetria que el contrato exige entre implementaciones distintas --un `ArrayList` tiene que
  ser igual a un `LinkedList` con los mismos elementos-- no existia.

**Por que no se vio antes.** El diff de API compara firmas, y `equals`/`hashCode` estan en la lista
de miembros de `Object` que el medidor **excluye a proposito** (si no, las 100 clases de `java.util`
figurarian con dos faltantes cada una). O sea: el conteo no podia verlo, ni cuando `Collections`
estaba en 5/74 ni cuando llego a 74/74. Es la misma leccion que dejaron los cinco stubs de `Arrays`
en la tanda anterior, por otro camino: **el conteo por firma tiene dos puntos ciegos, el cuerpo
vacio y el miembro heredado de `Object`. Los dos los encuentra la prueba de comportamiento y
ninguno el conteo.**

Estado previo, medido con una prueba de 16 bits contra `java` real (`P2`, 62496 de 65535):

| clase | tenia `equals`/`hashCode` |
|---|---|
| `AbstractSet` (y `HashSet`, `TreeSet`, `LinkedHashSet`) | si |
| `Vector` | si, propio y `synchronized` |
| `FixedMap` / `FixedEntry` (los de `Map.of`) | si |
| `EnumMap` | si |
| `AbstractList` -> `ArrayList`, `LinkedList`, `Stack`, `SubList`, `FixedList`, `ReverseOrderListView` | **no** |
| `AbstractMap` -> `HashMap`, `LinkedHashMap`, `TreeMap`, `IdentityHashMap`, `WeakHashMap` | **no** |
| `Hashtable` (desciende de `Dictionary`, no de `AbstractMap`) | **no** |

**Arreglado** en los tres lugares, con las formulas que la especificacion fija al detalle -- y son
fijas justamente porque el contrato exige que dos colecciones iguales de clases distintas den el
mismo numero:

| | |
|---|---|
| `List.hashCode` | `h = 31*h + hash(e)`, arrancando en 1, en orden |
| `Map.hashCode` | la **suma** de `hash(clave) ^ hash(valor)` -- suma, y no combinacion posicional, porque un mapa no tiene orden |

`AbstractList.equals` compara los dos iteradores en vez de los dos `size()`: asi no depende de que
`size()` sea barato, y corta en el primer elemento distinto. `AbstractMap.equals` recorre por
`keySet()`+`get()` en vez de comparar los dos `entrySet` como hace el JDK, porque asi no depende de
que las entradas de cada implementacion tengan su propio `equals` bien puesto. El caso del valor
`null` pide el paso extra de `containsKey`: "no esta la clave" y "esta, y vale null" se ven igual
desde `get`, y no son lo mismo.

Despues del arreglo, `P2` da **65535**, identico a `java` real.

**Lo que queda pendiente y se deja dicho:** `IdentityHashMap` ahora hereda la igualdad por
contenido de `AbstractMap`. El JDK la sobreescribe para comparar por identidad cuando el otro
tambien es un `IdentityHashMap` -- es de las pocas clases que rompe el contrato de `Map` a
proposito. Heredar la de contenido es **mejor que lo que habia** (identidad del mapa entero), pero
no es lo que el JDK hace.

Repro: `KajiLibrary/repros/finding_292.java`.

### Divergencias que quedan anotadas de esta tanda

| | |
|---|---|
| `nCopies(n, o)` | materializa el arreglo de `n` referencias. La del JDK guarda el elemento **una** vez y finge el largo, asi que `nCopies(1000000, x)` no ocupa nada. Es correcta y mas cara |
| vistas de un `synchronizedMap` | en el JDK comparten el mutex del mapa, asi que `synchronized (mapa)` tambien excluye a quien recorre el `keySet()`. Aca cada vista toma su propio monitor |
| `emptySortedSet().subSet(a, b)` | no valida que `a <= b`; el JDK tira `IllegalArgumentException` |
| `Collections.sort` | sigue siendo la insercion que ya estaba, O(n^2). `Arrays.mergeSortObj` existe desde la tanda anterior y seria el reemplazo natural |

### Verificacion

- `Collections` **74/74**, `Arrays` **214/214** (contrato con clausura de herencia).
- `CollTest` da **123156332** con nuestra VM y con `java` real. Cubre las cuatro vias laterales de
  los envoltorios, la `ClassCastException` adelantada de `checkedList`/`checkedMap`, y un barajado
  con semilla fija -- que es la prueba mas exigente del archivo, porque cualquier diferencia en el
  orden de las llamadas a `nextInt` cambia la permutacion.
- La biblioteca recompila **1028/1030** (los dos de siempre: la circularidad de `Map.java` con
  `FixedEntry`, y `SymElement`).
- De las 34 pruebas de `java/*.java` con `run():I` que el `javac` real tambien compila, **32 dan el
  mismo entero**. Las dos que no --`JcIc` y `WdWide`-- no mencionan `List`, `Map`, `Set`, `Iterable`
  ni `equals` en ninguna linea: son divergencias previas del JIT y de los locales anchos.

### El snapshot de `bin/` volvio a quedar atras, y el sintoma no se parecio a la causa

Otra vez, y se anota porque es la tercera:

```
panicked at src\jvm\interpreter\natives.rs:1182:14:
no native implementation for java/lang/reflect/Array
```

El nativo `Array.newArray` **existe** en `src/`, en la linea 1194 -- despues del 1182 donde el
binario congelado se cae. O sea que el panic senala el hueco que dejo el nativo que todavia no
estaba cuando se construyo `bin/`. Con el `run-headless` recien construido, seis pruebas que
"fallaban" pasan sin tocar una linea.

Y su reciproca en la misma corrida: `ArrTest.class` compilado con el `bin/javac.exe` congelado
revienta con el sintoma de #290 (`aastore: value header does not point at a known class`), porque
ese `javac` es anterior al arreglo. Compilado con el de `src/`, da 11246595.

La regla practica, ya escrita arriba en este documento y confirmada una vez mas: **antes de
perseguir un panic con el congelado, correr lo mismo con el de `src/`.**


---

## Tanda 2026-08-29 (n) -- `TreeMap`/`TreeSet` completos, y el `new` que nunca se chequeaba

`TreeMap` pasa de **26/51 a 51/51** y `TreeSet` de **16/33 a 33/33**. Las dos pasan a implementar la
interfaz que les corresponde -- `NavigableMap` y `NavigableSet` --, que hasta ahora declaraban solo
`Map` y `Set`.

Lo importante de esta tanda, sin embargo, no es eso: es **#293**, que la prueba de comportamiento
de `TreeMap` destapó de casualidad y que estaba corrompiendo bytecode en cuatro sitios de la propia
biblioteca.

### Las vistas, que es donde está el diseño

Los cortes de un `TreeMap` son **vistas**: `mapa.subMap(a, b).clear()` borra ese rango del mapa, y
un `put` dentro del rango se ve por la vista. Lo que una vista no hace es dejar escribir afuera de
sus límites -- `put` de una clave fuera de rango tira `IllegalArgumentException`, porque si no "la
vista de [a, b)" sería mentira.

El JDK tiene tres clases para esto (`NavigableSubMap` y sus dos subclases `Ascending`/`Descending`).
Acá hay **una**, `TmView`, con un piso, un techo y un booleano de sentido; las cinco fábricas son
cinco combinaciones de esos campos. Lo que ahorra la mitad del código es calcular **todo** en orden
absoluto -- el del mapa de atrás -- con seis métodos `abs*`, y traducir recién al final: para una
vista al revés, "el primero" es el mayor y `lower(k)` es el `absHigher(k)`. Escribir las dos
direcciones por separado sería duplicar catorce métodos para cambiarles el signo.

Las cuatro búsquedas primitivas del árbol (`getCeilingNode`/`getFloorNode`/`getHigherNode`/
`getLowerNode`) son la base de todo, y las cuatro son la misma bajada con el signo cambiado: O(log n),
recordando el mejor candidato visto. Cuando la bajada se pasa de largo, ese candidato **es** la
respuesta.

`TreeSet` se apoya en un `NavigableMap` cualquiera y no en un `TreeMap`, y ahí las vistas del
conjunto salen gratis: `headSet(x)` es el mismo `TreeSet` sobre `mapa.headMap(x)`. Los quince
métodos de navegación se escriben una vez y andan igual sobre el conjunto entero o sobre un corte de
un corte al revés. De paso, `TreeMap.navigableKeySet()` es un `TreeSet` sobre el mapa con `noAdd`
puesto: la única diferencia entre un conjunto y la vista de claves de un mapa es que la vista no
puede agregar, porque no sabría qué valor poner.

Y una corrección que no estaba en la lista de faltantes porque no es un miembro: `TreeMap.keySet()`
devolvía un **`HashSet`**. O sea que recorrer las claves de un mapa *ordenado* salía en orden de
hash -- y como `values()` y `entrySet()` se construían sobre él, las tres vistas estaban
desordenadas. Ahora `keySet()` es `navigableKeySet()`, y las otras dos recorren el árbol.

### #293 -- ✅ un `new` con argumentos que no matchean ningún constructor compilaba igual

```java
new HashSet<String>(unaLista())      // `HashSet(Collection)` no existía; compilaba
new Propia(7)                        // `Propia` solo tiene `Propia()`; compilaba
new StringBuilder("x", "y")          // ninguna de dos; compilaba
```

Lo que emitía:

```
0: new           java/util/HashSet
3: dup
4: invokestatic  unaLista:()Ljava/util/List;      <- el argumento queda empujado
7: invokespecial java/util/HashSet."<init>":()V   <- descriptor SIN parámetros
```

Seguí la pila. Después del `dup` hay `[HashSet, HashSet]`; el `invokestatic` deja
`[HashSet, HashSet, List]`; y el `invokespecial ()V` saca **uno** -- la `List`. O sea que corrió
`HashSet.<init>` sobre un objeto que no es un HashSet, dejó el objeto nuevo sin inicializar, y la
pila desbalanceada para todo lo que siguiera. Con un argumento primitivo es peor: `new Propia(7)`
corría `<init>` sobre el entero 7. La JVM real rechaza ese bytecode en la verificación.

**No se chequeaba nunca**: ni para una clase del classpath, ni para una del propio archivo, ni
cuando la clase no tenía ningún constructor sin argumentos. El único caso que sí se cazaba era el de
un **método** con la aridad equivocada.

Cómo apareció: `TreeTest` moría con

```
arithmetic_operations.rs:315: expected an int on the operand stack, found Reference(10146)
```

cincuenta líneas más abajo del `new HashSet<>(lista)` que la había corrido. Es el modo de falla de
#282 y #287 otra vez: **compila, el síntoma aparece lejos**.

**Arreglado** en dos mitades, y las dos hacían falta:

| | |
|---|---|
| `attribute.rs` | reporta "no se encontró un constructor aplicable" cuando la clase es del **fuente**, con las mismas notas de candidatos que ya daba una llamada a método |
| `codegen.rs` | se planta si llega un `new` **con argumentos** sin constructor resuelto. Es la red para el caso del **classpath**, donde la resolución es indulgente a propósito |

La segunda mitad es literalmente el guard que `super(...)`/`this(...)` ya tenía unas líneas más
arriba, con su comentario explicando por qué: *"Con argumentos no hay a qué llamar — y dejarlos
empujados corrompería la pila."* El `new` no lo tenía.

Repros: `finding_293.java` (los controles, que compilan y corren) y `finding_293b.java` (los tres
que ahora **tienen que ser rechazados**; el `javac` del JDK 25 los rechaza con las mismas razones).

### Lo que el arreglo destapó: cuatro sitios de la biblioteca

Ese es el valor real del arreglo. Los cuatro estaban emitiendo el bytecode corrupto:

| archivo | qué era |
|---|---|
| `java/lang/String.java:84` | #294 |
| `javax/lang/model/element/ModuleElement.java:89` | #294 |
| `java/util/concurrent/StructuredTaskScope.java:453` | #295 |
| `jakarta/validation/ConstraintViolationException.java:17` | `HashSet(Collection)` no existía -- se agregó, junto con `HashMap(Map)` |

`String.charAt` fuera de rango, para dar el ejemplo concreto, lanzaba un
`StringIndexOutOfBoundsException` **sin inicializar** y con la pila corrida.

### #294 -- ✅ una firma del classpath que nombra una clase que se está compilando no unificaba

```
error: no se encontró un constructor `StringIndexOutOfBoundsException(String)` aplicable
  método StringIndexOutOfBoundsException.<init>(String) no es aplicable
    (los argumentos no coinciden: String no se convierte a String)
```

"String no se convierte a String". Los dos se imprimen igual porque son **dos símbolos distintos**:
`Class(3800)` contra `Class(1)`.

Compilando `java.lang.String` en sí, el tipo de una concatenación salía del `java.lang.String`
**sintético** que el modelo de externos fabrica (`add_external`), no de la clase que ese mismo
archivo declara. Y compilando `ModuleElement.java`, el parámetro `ModuleElement$Directive` de un
constructor leído del classpath resolvía al `Directive` **externo**, distinto del `Directive` que
ese archivo declara -- el mensaje ahí era "Directive no se convierte a ModuleElement$Directive",
que suena a otra cosa entera.

El shadowing fuente-sobre-classpath ya existía (#5/#7/#19) y funcionaba para los nombres que el
**fuente** escribe. Lo que faltaba era el camino inverso: los nombres que vienen **de un `.class`**,
escritos como los escribe un class file.

**Arreglado** en tres lugares, uno por cada forma de nombre:

| | |
|---|---|
| `attribute::class_rtype` | el tipo de un literal/concatenación busca primero `java.lang.X` en el fuente |
| `attribute::resolve_type_name` | una clase fuente por FQN gana sobre el externo homónimo |
| `enter::resolve_name_to_sym` | un anidado del fuente nombrado `Outer$Inner` se repuntea a `Outer.Inner` antes de mirar los externos |

Y de yapa cerró la **circularidad de `Map.java` con `FixedEntry`**, que figuraba desde hacía tandas
como uno de los dos fallos conocidos de la recompilación ("tipo de retorno incompatible" en
`Map.java:290`). Era el mismo defecto: `FixedEntry` leído del classpath declaraba
`Map$Entry`, que no unificaba con el `Map.Entry` que `Map.java` estaba compilando.

Repro: se reproduce compilando `KajiLibrary/java/lang/String.java` o
`KajiLibrary/javax/lang/model/element/ModuleElement.java` **solos** -- el mismo `new` desde otro
archivo compila bien, y eso es lo que acota el defecto.

### #295 -- ✅ una clase anidada en una interfaz se trataba como interna de instancia

```
error: no se encontró un constructor `FailedException(Throwable)` aplicable
  método FailedException.FailedException(StructuredTaskScope, Throwable) no es aplicable
    (las listas de argumentos difieren en longitud)
```

El constructor que el fuente escribió toma uno; el que el compilador buscaba tomaba dos, porque le
había antepuesto la instancia envolvente. Un tipo **miembro** de una interfaz es implícitamente
`static` (§9.5) aunque no lo diga: no hay instancia que capturar, porque una interfaz no tiene
instancias.

**Arreglado** en `desugar.rs`, en los dos lados que tienen que coincidir -- el que decide si la
anidada lleva el campo/parámetro `this$0`, y el que decide si el `new Inner(...)` antepone el
argumento.

El matiz que costó dos intentos, y que quedó escrito en el código porque es fácil volver a pisarlo:
la regla vale para los tipos **miembro**, no para una clase **anónima** creada en un método
`default`. Esa tiene dueña interfaz igual, pero se crea en contexto de instancia, donde `this`
existe, y **sí** captura. Ponerle el filtro dejaba a los `$1` de `Spliterator` y `PrimitiveIterator`
sin su parámetro de cabecera. Se distinguen por el nombre sintético: una anónima es `$1`, una local
es `1L`, y ningún identificador del fuente empieza con `$` ni con un dígito.

Repro: `finding_295.java` (7 / 9 / 5, los mismos tres enteros que da `java` real).

### Divergencias que quedan anotadas de esta tanda

| | |
|---|---|
| `TmView.size()` | **cuenta**, O(n). El JDK hace lo mismo con sus submapas: el árbol no lleva cuenta por rango, y mantenerla saldría más caro que la cuenta ocasional |
| `TreeMap.values()` / `entrySet()` | siguen siendo **copias**, no vistas. `keySet()` sí pasó a ser vista en esta tanda |
| `a.new Inner()` calificado | no compila: el tipo no resuelve en esa posición. Es anterior a esta tanda -- el javac congelado falla igual -- y queda pendiente |

### Verificación

- `TreeMap` **51/51**, `TreeSet` **33/33**; `Collections` 74/74 y `Arrays` 214/214 siguen.
- `TreeTest` da **5341435** con nuestra VM y con `java` real. Son 62 comprobaciones, e incluyen lo
  que más fácil se rompe: los cortes de un mapa **descendente**, donde los límites se piden en el
  orden de la vista y hay que guardarlos en el absoluto.
- Los repros dan los enteros documentados en los dos lados: `finding_293` 2/4/3/0, `finding_295`
  7/9/5. `finding_293b` es rechazado por nuestro javac y por el del JDK 25, con las mismas razones.
- La biblioteca recompila **1030/1031** — el mejor número hasta ahora. El único fallo es
  `SymElement`, que no es del compilador: la clase no implementa `asType` y javac tiene razón. La
  circularidad de `Map.java` **dejó de fallar** con #294.
- De las 35 pruebas de `java/*.java` con `run():I` que el `javac` real también compila, **33 dan el
  mismo entero**. Las dos que no --`JcIc` y `WdWide`-- son las mismas de la tanda anterior y no
  mencionan colecciones en ninguna línea.

**`cargo test javac::` tiene 18 fallos, y son de entorno, no de estas tandas.** Se midió: con los
cambios de #294 revertidos dan exactamente los mismos 18. Lo que fallan es que el *class finder* les
está sirviendo el `Integer`/`Throwable`/`String` recortados de `boot/` en vez de los del JDK 25 del
classpath -- el propio mensaje de `loads_wrappers_with_their_real_hierarchy` lo dice. Queda anotado
como cosa aparte.

### `bin/` está atrás de nuevo, y esta vez importa más

El snapshot congelado **no puede compilar la biblioteca actual**: le faltan #290, #293, #294 y #295.
Compilar `java/util/TreeMap.java` con él anda, pero `ConstraintViolationException.java` sale con el
bytecode corrupto de #293 y `Map.java` no compila. Todo lo verificado arriba se hizo con el javac y
el `run-headless` construidos desde `src/`.


---

## Tanda 2026-08-29 (o) -- `SimpleTimeZone` y `Objects`, y el snapshot al día

`SimpleTimeZone` pasa de **no existir a 28/28** y `Objects` de **8/21 a 21/21**. Sin hallazgos
nuevos: las dos entraron sin destapar nada, que después de las tres tandas anteriores es en sí un
dato -- el compilador ya no se cae con código ordinario.

### `SimpleTimeZone`: la zona que se escribe a mano

Era una de las cuatro clases de `java.util` que directamente no existían, y la única de las cuatro
que ya era viable: lo que le faltaba era aritmética de calendario, y eso llegó con
`GregorianCalendar`.

Es la única `TimeZone` concreta y pública del JDK, o sea la única forma de escribir una zona sin la
base de datos IANA. Lo que la hace complicada no es el offset sino que las reglas se dan como
**patrones**, porque las transiciones reales caen en días de la semana -- "el segundo domingo de
marzo", "el último domingo de octubre". Hay cuatro formas de decirlo, y el JDK las codifica en los
**signos de dos enteros** en vez de tener cuatro campos:

| día | dow | modo | ejemplo |
|---|---|---|---|
| >0 | 0 | DOM | el día 15 del mes |
| >0 | >0 | DOW_IN_MONTH | el 2.º domingo (día=2) |
| <0 | >0 | DOW_IN_MONTH | el **último** domingo (día=-1) |
| >0 | <0 | DOW_GE_DOM | el primer domingo en o **después** del 8 |
| <0 | <0 | DOW_LE_DOM | el último domingo en o **antes** del 21 |

Esa codificación es historia y no diseño, pero es contrato: los constructores públicos reciben esos
enteros. Los `setStartRule`/`setEndRule` son la cara legible de lo mismo.

El detalle que se pasa por alto es el **modo de la hora**. `startTime` puede venir en hora de pared,
en hora estándar o en UTC, y los tres significan instantes distintos -- justamente porque el reloj
salta en ese momento. Acá se normaliza todo a **hora estándar local**, que es el reloj en el que
llegan los argumentos de `getOffset(era, year, month, day, dayOfWeek, millis)`, y ahí el modo deja
de importar:

| regla | ajuste sobre la hora dada |
|---|---|
| arranque, pared | ninguno -- en ese momento el reloj **todavía** marca hora estándar |
| cierre, pared | `- dstSavings` -- en ese momento el reloj **viene adelantado** |
| cualquiera, estándar | ninguno |
| cualquiera, UTC | `+ rawOffset` |

Y la comparación se hace sobre `milisegundos desde la época`, no sobre `(mes, día, hora)`: el ajuste
puede empujar el instante fuera del día, y con una tripleta habría que arrastrar acarreos a mano.

El hemisferio sur sale de una línea: si el instante de arranque cae **después** del de cierre dentro
del año, el verano cruza el fin de año y la condición es el complemento (`>= inicio || < fin`).

**Divergencia deliberada**, la misma que el JDK: una `SimpleTimeZone` no modela transiciones
históricas. `startYear` corta la regla hacia atrás, pero de ahí en adelante dice lo mismo para 1970
que para 2030. Para historia de verdad hace falta la tzdb, y no la hay.

### `Objects`: los seis `check*`, que no son lo que parecen

Los trece miembros que faltaban son `compare`, `deepEquals`, `hash`, `toIdentityString`, los tres
`requireNonNull*` diferidos, y los **seis** `checkIndex`/`checkFromToIndex`/`checkFromIndexSize`.

Los seis últimos parecen triviales hasta que se ve por qué el JDK los agregó en el 9: la
comprobación obvia está **mal**.

```java
if (from + size > length) throw ...;   // falso negativo: `from + size` desborda y da negativa
if (size > length - from) throw ...;   // la de acá
```

Estaban en el JDK porque cada quien las escribía distinto y algunas mal. La prueba de comportamiento
incluye el caso: `checkFromIndexSize(1, 2147483647, 5)` tiene que lanzar, y con la forma obvia pasa.

De `hash(Object...)` queda anotada la trampa conocida y se replica tal cual, porque el número es
parte del contrato: **`Objects.hash(x)` no es `Objects.hashCode(x)`**. El varargs arma un arreglo de
uno y le aplica igual el `31 * 1 + h`.

### Verificación

- `SimpleTimeZone` **28/28**, `Objects` **21/21**.
- `StzTest` da **16859132** con nuestra VM y con `java` real, y a la primera. Son 62
  comprobaciones, y lo que de verdad prueban son las **transiciones**: no alcanza con "en julio hay
  verano", hay que mirar el milisegundo justo antes y justo después de cada salto. Están las de
  EE.UU. (2.º domingo de marzo / 1.º de noviembre, hora de pared), las de Europa (último domingo,
  hora en UTC), un caso del hemisferio sur, las cuatro formas de escribir una regla, `startYear`, y
  una zona sin verano.
- La biblioteca recompila **1031/1032**; de las 36 pruebas comparables, **34** dan el mismo entero.
  Las dos que no son `JcIc` y `WdWide`, las mismas de las tandas anteriores.

### El snapshot de `bin/` se refrescó

Todo lo de arriba corre sobre el `bin/` nuevo. El anterior no podía compilar la biblioteca: le
faltaban #290, #293, #294, #295 y el nativo `Array.newArray`. La procedencia, los doce archivos que
se superpusieron sobre `git archive HEAD` y los quince que **no** entraron están en `bin/FROZEN.md`.

Lo que cierra el círculo: los `.class` que produce el `javac.exe` congelado son **byte a byte
idénticos** a los del construido desde `src/`.

### Estado de `java.util`

**1756/2097 (83,7 %)**, 69 clases al 100 %, y quedan **tres** sin escribir en vez de cuatro:
`Scanner` (la más grande del paquete, con su propia máquina de tokens), `Timer` (necesita un hilo de
fondo y el ejecutor de pruebas es monohilo) y `ServiceLoader` (pide `java.lang.Module`, que no
existe).


---

## Tanda 2026-08-29 (p) -- las tres que faltaban, y el varargs que envolvia de mas

Las tres clases que `java.util` no tenia escritas ya existen. **109 de 109**: no queda ninguna clase
pública del paquete sin cuerpo.

| | antes | ahora | qué falta y por qué |
|---|---|---|---|
| `Timer` | no existía | **12/12** | nada |
| `ServiceLoader` | no existía | **8/9** | `load(ModuleLayer, Class)` pide `java.lang.ModuleLayer` |
| `Scanner` | no existía | **64/73** | los nueve constructores que toman `File`, `Path` o `ReadableByteChannel` |

Y de escribirlas salieron **tres hallazgos**, uno de ellos de los que corrompen en silencio.

### `Timer`: tres piezas y un solo hilo

Una cola de prioridad ordenada por hora (un montículo binario: lo que se hace todo el tiempo es
"mirar el próximo" y "reinsertar el que corrió", O(1) y O(log n)), un hilo que duerme hasta la hora
del primero, y el monitor de la cola sincronizando a los dos. Un solo hilo para todas las tareas, y
eso es contrato y no atajo -- una tarea lenta demora a las de atrás. Es la razón por la que
`ScheduledThreadPoolExecutor` existe.

Del bucle del hilo, tres detalles que están cada uno por algo: espera sobre el monitor de la
**cola** (así `schedule` puede despertarlo con una tarea que va antes); corre la tarea **fuera** del
`synchronized` (si no, programar otra quedaría bloqueado lo que dure); y reprograma una tarea
repetida **antes** de correrla (si fuera después, una tarea que lanza excepción no volvería a la
cola).

Las dos formas de repetir son la confusión clásica y quedan escritas: `schedule` cuenta desde que
**terminó** la anterior (retraso fijo), `scheduleAtFixedRate` desde su hora **teórica** (frecuencia
fija, y por eso se recupera en ráfaga después de una demora). El signo de `TimerTask.period` es la
codificación, y viene del JDK.

### `Scanner`: un partidor de tokens con dos caras

La idea entera es una línea: la entrada se parte por un **patrón de delimitadores**, y `next`,
`nextInt`, `hasNextDouble` son variaciones sobre eso. Por eso la misma clase sirve para leer un
entero de la consola y para partir un CSV.

Los pares `hasNextX`/`nextX` van juntos y no por casualidad: el primero mira **sin consumir**. Esa
asimetría es la que hace que `while (sc.hasNextInt()) suma += sc.nextInt();` corte solo cuando
aparece algo que no es número, sin perderlo.

La trampa clásica quedó anotada porque no es un detalle de implementación sino del diseño:
`nextInt()` consume el número y **deja el fin de línea**, así que el `nextLine()` que sigue devuelve
el resto vacío de esa línea. La prueba de comportamiento la fija.

Divergencias, todas anotadas en el archivo: una fuente `InputStream` se lee **entera** al construir
(no hay `InputStreamReader`, y decodificar por trozos parte los multibyte del borde); el buffer no
se compacta; el separador de miles es `,` fijo porque nuestro `Locale` no lleva símbolos numéricos;
y `tokens()`/`findAll()` son ansiosos.

Un detalle del camino: `match()` sobre un token no pasa por ningún `Matcher`. Lo natural sería
matchear `[\s\S]*` sobre la región, pero nuestro motor **rechaza** una clase predefinida negada
adentro de otra clase (`\S` dentro de `[...]`) y lo dice con todas las letras en
`Node.addClassEscape`. Un token ya localizado no necesita motor: sus límites ya se conocen.

De paso se agregó `BigInteger(String, int radix)`, que faltaba y sin el cual
`Scanner.nextBigInteger(radix)` no tenía con qué parsear. Es el mismo Horner de siempre con la base
como parámetro; la versión decimal ahora delega ahí. Y `CharBuffer` pasó a declarar `Readable`, que
ya implementaba de hecho.

### `ServiceLoader`: todo escrito menos el primer paso

El mecanismo completo está -- parsear el archivo de configuración, cargar la clase por nombre,
verificar el subtipo, instanciar, cachear, la carga perezosa, `reload()`. Lo que no está es
**enumerar el recurso**: `META-INF/services/<servicio>` a lo largo del classpath pide
`ClassLoader.getResources`, y nuestro `ClassLoader` sabe cargar clases y nada más.

Está aislado en **un** método, `nombresDeProveedores`, justamente para que el día que existan los
recursos sea eso lo único que haya que escribir. Mientras tanto un `ServiceLoader` es una colección
vacía bien formada: se itera, se le pide `findFirst`, se recarga, y no rompe nada.

### #298 -- ✅ un arreglo pasado a un varargs **genérico** se envolvía en otro arreglo

```java
String[] a = { "x", "y", "z" };
Arrays.asList(a).size()   // daba 1, no 3
Stream.of(a).count()      // daba 1, no 3
```

La lista tenía **un** elemento, y ese elemento era el arreglo. Compilaba, corría, y devolvía algo
del tipo correcto: por eso podía estar ahí mucho tiempo sin que nada reventara.

La regla (§15.12.4.2) es que si el único argumento **ya es** el arreglo del varargs, se pasa tal
cual. El chequeo existía en `desugar::lower_varargs`, pero preguntaba por **subtipado**: `String[]`
contra `Object...` da true, y contra `T...` da **false**, porque `T` todavía no está instanciada.
Así que los varargs no genéricos andaban y los genéricos no -- que son justo los más usados:
`Arrays.asList`, `List.of`, `Set.of`, `Stream.of`, `Collections.addAll`.

**Arreglado** agregando el caso de la variable de tipo, con la misma regla del §4.5.1 que cerró
#290:

| | |
|---|---|
| `String[]` contra `T...` | `T` liga `String`, el arreglo **pasa tal cual** |
| `int[]` contra `T...` | `T` no puede ligar `int`, así que liga `int[]` y el arreglo **se envuelve** |

El segundo no es un descuido: es lo que hace el javac real, y es la razón por la que
`Arrays.asList(new int[]{1,2})` da una lista de un solo elemento. El repro lo fija.

**Lo que destapó**: 22 clases de la biblioteca emitían bytecode distinto -- entre ellas `String`,
`Arrays`, `Collection`, `Pattern`, `Matcher` y el paquete `stream` entero. Se recompilaron y se
llegó a punto fijo.

Cómo apareció: `ScanTest` daba 1156107 contra 1156108, y el bit que faltaba era
`scanner.tokens().count() == 4`.

Repro: `KajiLibrary/repros/finding_298.java`.

### #297 -- ✅ un escape ilegal en un literal se aceptaba, tragándose la barra

```java
"\d"     // compilaba, y valía "d"
"\x27"   // compilaba, y valía "x27"
```

El javac real los rechaza con *illegal escape character*, y por buenas razones: el único motivo para
escribir `\d` en un literal es querer el patrón `\d` de una expresión regular, o sea haber escrito
una barra de menos. Con la barra tragada, `Pattern.compile("\d")` compila un patrón que matchea la
**letra** `d` en vez de un dígito, y nadie se entera.

**Lo que destapó, y es el mejor argumento a favor:** dos archivos de la propia biblioteca usaban
`\x27` -- sintaxis de C y de Python, que en Java no existe -- para escribir una comilla simple.
`MissingFormatArgumentException.getMessage()` devolvía

```
Format specifier x27%sx27
```

en vez de `Format specifier '%s'`. Igual `UnknownFormatConversionException`.

**Arreglado** en `parser.rs`, en las dos funciones de desescapado (`unescape_utf16` y
`unescape_impl`), que las dos terminaban en `other => out.push(other)`. De paso se completó el
escape **octal** (§3.10.7), que estaba a medias: solo `\0` se decodificaba, y `\101` daba la letra
`1` seguida de `01` en vez de la `A`.

Repros: `finding_297.java` (los controles) y `finding_297b.java` (los tres que ahora **tienen que
ser rechazados**; el javac del JDK 25 los rechaza también).

### #296 -- ⬜ `Thread.sleep(ms)` cuenta **pasos** de bytecode, no milisegundos

```rust
// bytecode_interpreter.rs::thread_sleep
sleep_until = Some(self.shared.steps + ms.max(0) as usize);
```

`Thread.sleep(120)` significa "120 pasos de intérprete". Sobre un planificador cooperativo es un
reloj virtual coherente y determinista, y para eso funciona bien. El problema es que **no es el
mismo reloj** que lee `System.currentTimeMillis()`, que sí devuelve la hora real: los dos relojes
existen y no están relacionados.

Se rompe todo lo que mezcle los dos, y el caso concreto que lo destapó fue `Timer`: guarda la hora
de cada tarea con `currentTimeMillis` y espera con `wait(millis)`. Su hilo despierta a los pocos
microsegundos de reloj real, ve que todavía no es la hora, y vuelve a dormir -- mientras el hilo
principal, que "durmió" 200 ms, ya terminó. La tarea nunca corre. Medido: un `Thread.sleep(120)`
tarda **0 ms** de reloj de pared.

La vuelta que sí funciona es **esperar activamente** sobre `currentTimeMillis`, que mide el mismo
reloj que el Timer. Es lo que hacen `TmSvTest` y el repro, y con eso las dos VM dan el mismo entero.

**Queda abierto a propósito.** Arreglarlo es una decisión de diseño y no un parche: o el reloj
virtual pasa a ser también el que devuelve `currentTimeMillis` -- y entonces el tiempo del programa
deja de ser el del mundo --, o el planificador espera de verdad cuando todos los hilos están
dormidos. No se elige acá.

De paso quedó visto que **`System.nanoTime()` no existe** en la biblioteca (hay un `NANO_ORIGIN`
sin usar en `natives.rs`, así que estuvo planeado). Es aparte y no se tocó.

Repro: `finding_296.java`, que **da distinto de los dos lados a propósito** -- `duermeDeVerdad()`
vale 1 en `java` real y 0 acá. Los otros tres métodos coinciden y son los que acotan el hallazgo.


### #299 -- ✅ un tipo del **mismo paquete** perdía el nombre simple contra un homónimo

Apareció refrescando `bin/`, por el peor camino posible: **dos recompilaciones seguidas de la
biblioteca daban `.class` distintos**. No un `.class` mal, no un error: dos corridas idénticas con
resultados distintos.

Lo que cambiaba era el tipo declarado en una firma:

```
public interface BasicType<X> extends jakarta.persistence.metamodel.Type<X>   // a veces
public interface BasicType<X> extends java.lang.reflect.Type<X>               // otras
```

El tipo declarado es **otra clase**. Compila, y el error viaja adentro del `.class`.

**El mecanismo.** El espacio de tipos externos es plano: se clavea por **nombre simple**, y gana el
que se cargó primero — está escrito en `SymbolTable::register_external`, y hasta ahora el caso que
importaba era el de dos homónimos de paquetes distintos que la unidad nombra. Este es peor, porque
el segundo no lo nombra nadie:

1. la unidad nombra `Class` y `Type`;
2. cargar `java.lang.Class` arrastra sus supertipos, y **`Class` implementa `java.lang.reflect.Type`**;
3. ese `Type` se queda con la clave simple;
4. cuando le toca el turno al `Type` del propio paquete, la guardia de `try_load` ve la clave
   ocupada y no hace nada. El del paquete **no se carga nunca**.

Y la no-determinación venía de que los nombres que la unidad escribe y los tipos *core* de
`java.lang` iban al **mismo `HashSet`**, recorrido en su orden — o sea, en ninguno. Que ganara
`Class` o `Type` dependía del hash.

**Arreglado en dos partes**, porque el problema también eran dos:

| | |
|---|---|
| la **no-determinación** | `load_externals` recorre la fase 1 en dos pasos y **ordenada**: primero los nombres que la unidad escribe (alfabético, que no significa nada salvo ser siempre el mismo), después los core de `java.lang`. Dos recompilaciones seguidas dan `.class` **byte a byte idénticos** |
| la **clave robada** | `try_load` deja de salir temprano cuando la clave simple está ocupada. Si el candidato que corresponde es de los que el JLS hace **prevalecer** —el del paquete propio, o el de un `import` de un solo tipo (§6.5.5.1)— le **quita** la clave al ocupante, con `SymbolTable::override_external` |

El orden solo no alcanzaba, y el caso que lo probaba es el cuarto de la lista: `MapAttribute` nombra
`Map` y `PluralAttribute`, y cargar cualquiera de los dos llega a `java.lang.Class` —y con él al
`Type` de `java.lang.reflect`— antes de que le toque el turno a `Type`. Ningún orden de los nombres
que la unidad escribe arregla eso; hay que poder reclamar la clave después.

**Solo esos dos candidatos la reclaman.** `java.lang`, los `import` on-demand y el nombre pelado no:
ahí el desempate por "el primero que llegó" es tan bueno como cualquier otro, y pisar sería más
riesgo que beneficio. Es la misma prudencia con la que `register_external` ya trataba el caso de dos
homónimos que la unidad **sí** nombra.

**Lo que arregló en la biblioteca:** las **cuatro** clases de `jakarta.persistence.metamodel` que
declaraban `java.lang.reflect.Type` en su firma —`BasicType`, `MapAttribute`, `PluralAttribute` y
`SingularAttribute`— ya declaran el `Type` de su paquete.

Repro: `KajiLibrary/repros/zz299/` (`Type.java` + `Uso.java`). Determinista y con la causa aislada:
el método `getJavaType()` está ahí **solo** para que la unidad nombre `Class`, y sin él no dispara.
El `-cp` lleva **dos** rutas, y también es parte del repro: una trae `java.lang.Class` y la otra el
`zz299.Type`; con una sola no hay colisión que reproducir.

### Verificación

- `Timer` **12/12**, `ServiceLoader` **8/9**, `Scanner` **64/73**.
- `ScanTest` da **1156108** y `TmSvTest` **16894**, los dos idénticos a `java` real. `ScanTest` son
  62 comprobaciones e incluye la trampa de `nextInt()`+`nextLine()`, el token vacío entre dos comas
  de un CSV, y las tres fuentes (String, `InputStream`, un `Readable` que entrega de a tres
  caracteres para forzar la lectura incremental).
- Los repros dan lo documentado en los dos lados: `finding_297` 3/65/1/39, `finding_298` 3/3/3/1/3.
  `finding_297b` es rechazado por los dos compiladores. `finding_296` diverge a propósito.
- La biblioteca recompila **1034/1035**; el único fallo sigue siendo `SymElement`, donde javac tiene
  razón. Los `.class` del árbol están en **punto fijo** contra el compilador arreglado, y dos
  recompilaciones seguidas dan bytes idénticos — que antes de #299 no pasaba.
- De las 39 pruebas de `java/*.java` comparables, **37** dan el mismo entero. Las dos que no son
  `JcIc` y `WdWide`, las mismas de las tres tandas anteriores.
- `cargo test javac::`: 763 pasan, 18 fallan -- los mismos 18 de entorno de la tanda (n), sin cambio.

### Estado de `java.util`

**1840/2097 (87,7 %)**, 70 clases al 100 %, y **109 de 109 clases públicas existen**: no queda
ninguna sin escribir.


---

## Tanda 2026-08-30 (q) -- cerrando findings, y `Locale`/`Properties` completas

Cuatro findings cerrados —**#274**, **#299**, **#300** y **#301**— y las dos clases más grandes que
quedaban incompletas de `java.util`.

| | antes | ahora |
|---|---|---|
| `Locale` | 17/75 | **75/75** |
| `Properties` | 30/51 | **51/51** |

Los tres findings nuevos de la tanda (#300 y #301 aparecieron escribiéndolas, #299 se había quedado
a medias) son **todos de resolución de nombres**, y los tres tienen la misma forma: un nombre que se
escribe de dos maneras distintas termina en dos símbolos distintos, y algo no unifica. Es la misma
familia de #294.

### #274 -- ✅ un nombre calificado no se reconocía como tipo dentro de una expresión

```java
java.lang.reflect.Modifier.PUBLIC        // error: no se encuentra el símbolo: java
java.util.Arrays.asList("a", "b")        // idem
```

En una **declaración** el mismo nombre resolvía; en una **expresión** no, y el error lo decía todo:
*"símbolo: variable java"*. El atribuidor leía `java.util.Arrays.asList` como una cadena de accesos
a campo que arranca en una variable llamada `java`.

Es la **reclasificación de nombres ambiguos** del §6.5.2, y no estaba. La regla es probar prefijos
de izquierda a derecha hasta que uno resuelva como tipo, y recién lo que sigue es acceso a miembro.

**Arreglado** con una función, `reclassify_as_type`, llamada en los dos sitios donde un receptor
puede ser un tipo: el acceso a campo y el receptor de una llamada. Va **antes** de atribuir el
receptor como expresión, que es lo que fallaba.

El orden del JLS se respeta y está probado: **gana el prefijo más corto**. Si `java` nombra un
local, un campo o un tipo en scope, esto *es* una cadena de accesos y no hay nada que reclasificar
-- el repro tiene ese control (`Paquete java = new Paquete(); java.lang` lee el campo, no el
paquete).

El cargador ya traía el tipo: `collect_from_expr` arma el nombre punteado y lo mete en la lista de
externos a cargar. O sea que el tipo estaba, y lo único que faltaba era mirarlo.

Repro: `finding_274.java`, más siete formas verificadas contra `java` real (campo estático, método
estático, `java.util.List`/`Arrays.asList`, encadenado, tipo anidado `java.util.Map.Entry`, y los
dos controles).

### #300 -- ✅ un tipo calificado en un `catch` no resolvía, y el handler se descartaba

```java
static void tira() throws IOException { ... }

try { tira(); } catch (java.io.IOException e) { }
// error: excepción chequeada `IOException` sin capturar ni declarar en `throws`
```

El `try` **sí** la atrapaba. Lo que pasaba es que el tipo del `catch`, escrito calificado, no
resolvía a nada, así que el handler no entraba en el conjunto de excepciones manejadas.

**Causa**: `check.rs::resolve_exc` tenía su propia resolución de nombres, de dos líneas:

```rust
table.resolve_type(scope, name).or_else(|| table.external(name))
```

Las dos claves son por nombre **simple**. Un `java.io.IOException` no está en ninguna, así que
devolvía `None` — y `None` ahí no es un error, es *"este catch no atrapa nada"*.

Lo peculiar del síntoma: solo aparece cuando el `throws` y el `catch` se escriben con **formas
distintas** del mismo nombre. Con los dos calificados no resolvía ninguno, pero entonces el `throws`
de arriba tampoco cubría y el error salía igual; con los dos simples andaba todo. El error dependía
del estilo de escritura y no de la semántica.

**Arreglado** haciendo que `resolve_exc` vaya por la resolución **general** (`attribute::resolve_rtype`),
que ya sabe de nombres calificados, anidados y del paquete propio. Una resolución menos que
mantener aparte.

El error era **espurio**: el bytecode del handler lo emite el codegen, que resuelve por otro camino,
así que lo que se rechazaba era fuente válido. No había riesgo de una excepción tragada en tiempo de
ejecución.

Repro: `finding_300.java` (1/1/1/2, los mismos enteros que `java` real).

### #301 -- ✅ `super.m()` sobre una superclase genérica no sustituía sus argumentos de tipo

```java
class C extends Hashtable<Object, Object> {
    Enumeration<Object> e = super.keys();   // error: tipo incompatible
    Enumeration<Object> f = this.keys();    // anda
}
```

La misma llamada por `this` andaba. La diferencia estaba en el tipo del receptor:

| | |
|---|---|
| `this` | `C`, con sus argumentos |
| `super` | `Hashtable` **crudo**, sin ellos |

Con el crudo, el retorno `Enumeration<K>` sale con la `K` sin sustituir.

**Causa**: el tipo de `super` salía de `table.super_class(clase)`, que devuelve el **símbolo** de la
superclase y nada más. El supertipo ya instanciado está en otro lado: en el `Resolved::Class` de la
propia clase, donde `enter` lo dejó resuelto. **Arreglado** sacándolo de ahí, con el crudo como
respaldo.

Apareció escribiendo `Properties`, que extiende `Hashtable<Object,Object>`. El rodeo era `this.m()`
en vez de `super.m()` -- que ahí daba lo mismo porque `keys` no está sobreescrito, pero **no es un
rodeo válido en general**: si la subclase sí lo sobreescribe, las dos llamadas van a métodos
distintos. El repro tiene ese control.

Repro: `finding_301.java` (11/7/12).

### #299 -- cerrado del todo

Quedaba la mitad difícil, y está en su propia sección más arriba: `try_load` ahora le **quita** la
clave simple al ocupante cuando el candidato que corresponde es de los que el JLS hace prevalecer
(el del paquete propio, o el de un `import` de un solo tipo). Las cuatro clases de
`jakarta.persistence.metamodel` que declaraban `java.lang.reflect.Type` ya declaran el de su
paquete, y dos recompilaciones seguidas dan bytes idénticos.

### `Locale`: mecanismo completo, datos ninguno

De 17/75 a **75/75**. La clase pasa a tener las cuatro partes de una etiqueta (idioma, escritura,
región, variante), las extensiones, las etiquetas BCP 47 en las dos direcciones, el filtrado RFC
4647 con `LanguageRange`, los defaults por categoría y las cuatro enums anidadas.

Lo que hay que tener claro es qué **no** hay, y es una sola cosa: **datos**. Un `Locale` no traduce
ni sabe nada -- es la clave con la que otras clases buscan. Eso deja tres divergencias, todas
escritas en el archivo:

| | |
|---|---|
| `getDisplayLanguage()` y compañía | devuelven el **código** (`"es"`, no `"español"`). Es lo que hace el JDK cuando no tiene datos para un locale: la respuesta de respaldo, no una inventada |
| `getISO3Language()`/`getISO3Country()` | tabla para lo que la clase nombra; para el resto, `MissingResourceException` — que es lo que el JDK hace con un código que no conoce |
| `getISOLanguages()`/`getISOCountries()` | lo que hay en esa tabla, no las listas ISO completas (184 idiomas, 249 países) |

Dos cosas que se verificaron contra el JDK 25 y salieron distintas de lo esperado, así que quedan
anotadas:

- **Los tres códigos que ISO renombró van al revés de como fueron veinte años.** El JDK guardaba el
  viejo (`he` → `iw`) y solo `toLanguageTag()` devolvía el nuevo. En el 25, `new Locale("iw")` da
  `he`: se canonicaliza al **nuevo**. Se replica eso.
- **`x-lvariant-` no es una extensión de uso privado**: el JDK lo convierte en la **variante**. El
  repro de la prueba lo evita a propósito y lo dice.

Y un bug propio que la prueba cazó y vale anotar porque es de los que solo se ven corriendo:
`SIN_EXT` estaba declarado **después** de las constantes, y los inicializadores estáticos corren en
orden de aparición. `ROOT` se construía con el arreglo de extensiones en `null`, y `toLanguageTag()`
moría con NPE en el primer uso.

### `Properties`: los seis métodos de formato, y el XML

De 30/51 a **51/51**. Entran `store` (Writer y OutputStream), `save`, `list` (PrintStream y
PrintWriter), el par `storeToXML`/`loadFromXML`, el constructor por capacidad, y las once
operaciones de `Map` sobre `Object` que el JDK redeclara.

La nota vieja del archivo decía que escribir el formato sería *"inventar un parser que nadie puede
probar acá"*. Lo que cambió no es el criterio sino que **ahora sí se puede probar**: el round-trip
`store` → `load` se compara contra `java` real, y el formato está especificado al detalle.

Donde vive la única dificultad real es en los escapes al **escribir**, y la asimetría es del formato:
en una **clave** hay que escapar los tres separadores (`=`, `:` y el blanco), porque si no partirían
el par al releer; en un **valor** solo el blanco **inicial**, que es el único que el lector se
comería. Escapar de más no rompe el round-trip, pero produce archivos distintos de los del JDK.

De `loadFromXML` queda dicho el subconjunto: se lee la forma del DTD sin validar contra él ni
resolverlo por la red. Un XML bien formado con otra estructura se rechaza con
`InvalidPropertiesFormatException` en vez de aceptarse a medias.

### Verificación

- `Locale` **75/75**, `Properties` **51/51**.
- `LocPropTest` da **1204219** con nuestra VM y con `java` real. Son 84 comprobaciones, y las que
  más valen son las del filtrado RFC 4647 (el comodín, el peso cero que **excluye**, `lookup`
  acortando el rango) y los tres round-trips de `Properties`: texto, bytes y XML, con claves que
  llevan espacios, `=`, `:`, tabuladores y `<`/`&`/`"`.
- Los repros de los tres findings dan los mismos enteros que `java` real: `finding_274` (siete
  formas), `finding_300` 1/1/1/2, `finding_301` 11/7/12.
- La biblioteca recompila **1034/1035**, en punto fijo, y dos corridas seguidas dan bytes idénticos.
- De las 40 pruebas de `java/*.java` comparables, **38** dan el mismo entero. Las dos que no son
  `JcIc` y `WdWide`, las mismas de las cuatro tandas anteriores.
- `cargo test javac::`: 763 pasan, 18 fallan — los mismos 18 de entorno, sin cambio.

### Los findings que quedan abiertos

| | |
|---|---|
| **#268** | javac no sintetiza los accesores de una superclase package-private |
| **#279** | una llamada con un argumento `T[]` se declara ambigua |
| **#284** | no se chequea un método abstracto heredado de una superclase del **classpath** |
| **#285** | una llamada genérica anidada como argumento no resuelve si el destino es un genérico parametrizado por variables de tipo |
| **#286** | un lambda en línea no liga su parámetro si el destino es `X<? super E>` con `E` del receptor |
| **#287** | una llamada estática calificada, hecha desde una interfaz con un homónimo, ignora el calificador |
| **#288** | referencia stale que llega al GC en modo verde |
| **#296** | `Thread.sleep(ms)` cuenta pasos de bytecode, no milisegundos |

De los ocho, **#287 es el más cercano a lo cerrado en esta tanda** — es resolución de nombres con un
homónimo, la misma familia que #294/#299/#300. Los tres de inferencia (#279, #285, #286) son una
tanda propia. #288 y #296 no son del compilador.


---

## Tanda 2026-08-30 (r) -- el cierre chico, y el hash de identidad que se movía

Doce clases de `java.util` cerradas al 100 % con trabajo mecánico, y **#302**, que salió de ahí y es
el hallazgo más serio de la sesión: `Object.hashCode()` no era estable.

**84 clases al 100 %** (eran 72), 1944/2097 miembros.

### El lote: constructores de capacidad y copia, y los sueltos

| clase | qué entró |
|---|---|
| `ArrayList` | `(int)`, `(Collection)`, `trimToSize()` |
| `HashSet` | `(int)`, `(int, float)`, `newHashSet(int)` |
| `Hashtable`, `IdentityHashMap`, `WeakHashMap` | `(Map)`; y `newWeakHashMap(int)` |
| `ArrayDeque` | `(Collection)`, `clone()` |
| `EnumMap` | `(Map)`, `clone()` |
| `PriorityQueue` | `(PriorityQueue)`, `(SortedSet)` |
| `LinkedList` | `(Collection)` |
| `NoSuchElementException` | las dos formas con causa |
| `Enumeration` | `asIterator()` |
| `Queue` | `element()`, `remove()` |
| `UUID` | `nameUUIDFromBytes(byte[])` |
| `NavigableSet`, `NavigableMap` | `reversed()` |

Tres cosas que valen más que el conteo:

**`Queue.element()`/`remove()`** son el par que lanza de `peek`/`poll`, y la distinción es la razón
de que existan los cuatro: `poll` devuelve null porque el vacío es un resultado esperado —se lo usa
en un bucle que consume hasta agotar—, y `remove` lanza porque ahí el vacío es un error. Elegir el
equivocado convierte un bug en un `null` que viaja.

**`HashSet(int)` dimensiona al doble de lo pedido**, y no es un margen arbitrario: esta
implementación es de direccionamiento abierto con sondeo lineal, y pasada la mitad de ocupación los
grupos de colisiones se funden entre sí. De paso, `newHashSet(int)` existe porque `new HashSet<>(n)`
**no** quiere decir "para n elementos" —ese `n` es la capacidad de la tabla— y es una de las trampas
más viejas de la API.

**`reversed()` en un navegable ES `descending*()`**, y no es una simplificación: son el mismo objeto
en el JDK también. Cerrar `NavigableSet`/`NavigableMap` obligó a estrechar el retorno de `reversed()`
en cinco implementaciones (`TreeSet`, `EmptySortedSet`, `EmptySortedMap`, `ConcurrentSkipListSet`,
`ConcurrentSkipListMap`), que lo declaraban `SequencedSet`/`SequencedMap`.

**`UUID.nameUUIDFromBytes`** trajo un MD5 escrito a mano, con su tabla de 64 constantes literal
—`Math.sin` no está en la biblioteca, así que no se puede calcular—. Una tabla escrita a mano es
justo donde se cuela un dígito cambiado, así que la prueba compara el UUID resultante contra el de
`java` real: un solo bit distinto en cualquiera de las 64 constantes cambia los 128 de la salida.

### #302 -- ✅ el hash de identidad cambiaba cuando el GC movía el objeto

```rust
("java/lang/Object", "hashCode", "()I") => Some(Value::Int(reference(&args[0]) as i32))
```

El offset del objeto en el heap **es** su identidad, y por eso servía de respuesta. Lo que no es, es
**estable**: el recolector joven es copiador, y mueve lo que sobrevive. Después de una colecta menor
el mismo objeto vive en otro offset y su hash cambió.

Eso rompe el contrato de `Object.hashCode()` —el valor tiene que ser el mismo mientras el objeto
viva— y de ese contrato cuelgan `HashMap` y `HashSet`. Una clave puesta antes de una colecta puede
no encontrarse después.

**Cómo apareció, que es lo que lo hace valioso.** Escribiendo `EnumMap(Map)`, la prueba daba un
resultado **no monótono**: una versión con *menos* código antes fallaba y una con *más* andaba.

```
a()  sin nada antes                          -> 1  ✔
b()  con un EnumMap construido antes         -> 0  ✘
c()  el mismo EnumMap, y además un put       -> 1  ✔
d()  con un clone()                          -> 0  ✘
```

Ningún bug de lógica hace eso. El síntoma concreto era que `HashMap.get(Color.AZUL)` devolvía null
adentro del constructor, con la misma clave con la que se había hecho el `put` tres líneas arriba —
y lo único que había cambiado en el medio eran unas asignaciones. De ahí a "el hash se movió" hay un
paso, y de ahí a la línea de arriba, otro.

**Arreglado** como lo hace HotSpot, y por las mismas dos razones: el hash se materializa **a
demanda** (la mayoría de los objetos no lo piden nunca) y se guarda en el **encabezado**, que es lo
único que se mueve junto con el objeto — `evacuate_block` lo copia con el resto.

El encabezado son ocho bytes, `[class_id | mark]`, y la palabra de marca usaba un booleano entero
para un solo bit. Ahora el bit 0 es la marca y los bits 1..31 el hash. El precio es que `set_mark` y
`clear_all_marks` ya no pueden escribir la palabra completa: escribían `1` y `0`, y ahora tocan solo
el bit 0. Sin eso, cada marcado borraría el hash de todo objeto vivo — que es el mismo bug con otro
disparador.

**El valor sale de un contador global mezclado, no del offset**, y esa fue la segunda pasada. Con el
offset como semilla el hash ya era estable —que era lo que el finding pedía— pero seguía siendo malo:
los objetos mueren, el asignador reusa sus direcciones, y cien `new Object()` en un bucle que solo
guarda sus hashes daban varios repetidos. Estable e inútil para repartir en cubetas. El repro tiene
ese control aparte, y fue el que lo cazó.

Repro: `finding_302.java` (1/1/1/1, los mismos cuatro que `java` real).

### Verificación

- **84 clases al 100 %** (eran 72), **1944/2097** miembros (92,7 %).
- `LoteTest` da **311293** con nuestra VM y con `java` real. Son 52 comprobaciones, e incluyen los
  dos valores exactos del MD5 y el control de que una copia es independiente del original.
- La biblioteca recompila **1034/1035**, y de las 41 pruebas comparables **39** dan el mismo entero
  (las dos de siempre, `JcIc` y `WdWide`).
- Los **286 tests de la VM pasan**, incluidos los del GC. Es la verificación que #302 necesitaba:
  el cambio toca la palabra de marca, que es del recolector.

**Nota sobre cómo se construyó**: el árbol de trabajo tiene tres archivos de `src/fuzz/` de otra
sesión a medio editar, y **no compila**. Todo lo de esta tanda se construyó y se probó sobre la copia
limpia de `git archive HEAD` más los archivos propios superpuestos — la misma que usa el refresco del
snapshot. El trabajo ajeno no se tocó ni entró.

## Tanda: Optional, Random, SplittableRandom — y `Math.log`

Seis clases pedidas, seis cerradas al 100 %. Lo interesante no fue ninguna de las seis: fue que
para cerrar la última hubo que escribir un logaritmo, y que compilarlo destapó dos defectos del
compilador — uno de ellos, un mensaje de error que mandaba a leer el archivo equivocado.

| clase | antes | ahora |
|---|---|---|
| `Optional` | 15/20 | **20/20** |
| `OptionalInt` | 10/15 | **15/15** |
| `OptionalLong` | 10/15 | **15/15** |
| `OptionalDouble` | 9/15 | **15/15** |
| `Random` | 23/25 | **25/25** |
| `SplittableRandom` | 21/25 | **25/25** |

De arrastre: `RandomGenerator` 14/33 → 26/33, y `Math`/`StrictMath` ganaron `log`.

### La divergencia deliberada, y por qué se documenta como tal

`RandomGenerator` define doce fábricas de flujos. Las que llevan `streamSize` se implementaron; las
**sin cantidad** (`ints()`, `longs()`, `doubles()`, y las dos `splits()` sin tamaño) **se niegan**:

```java
private static UnsupportedOperationException sinTamano(String cual) {
    return new UnsupportedOperationException(
            "los flujos de esta biblioteca son ansiosos: use " + cual + "(streamSize)");
}
```

El JDK las define como "efectivamente ilimitadas", y eso pide un flujo **perezoso**: los valores se
generan a medida que alguien los pide y `limit(n)` corta antes de generar el resto. Los flujos de
esta biblioteca están respaldados por un arreglo y son **ansiosos** — se materializan enteros al
crearse —, así que un flujo infinito no se puede representar.

De las dos salidas se eligió la ruidosa. Devolver un prefijo largo y llamarlo infinito andaría para
`ints().limit(10)` y daría **menos** valores de los pedidos para `ints().limit(un_millón)`, en
silencio. Un método que se niega y dice con qué reemplazarlo es peor de usar y mejor de confiar:
*un miembro que falta es un subconjunto legal; uno que miente, no.*

### `Math.log`: fdlibm entero, porque una aproximación no servía

`Random.nextGaussian()` estaba bloqueado: su especificación es el método polar de Marsaglia sobre
`StrictMath.sqrt` y `StrictMath.log`, y `log` no existía en la biblioteca.

Y no alcanzaba con una `log` *buena*. El valor que devuelve `nextGaussian` es parte del contrato —
dos `Random` con la misma semilla tienen que dar los mismos gaussianos —, y ahí `log` se compone con
`sqrt` y con la división: **un ulp de diferencia cambia toda la secuencia**. Así que se escribió el
algoritmo de fdlibm, el mismo que usa el JDK, en tres pasos:

1. **Descomponer.** Todo double es `m * 2^k` con `m` en `[1,2)`, así que `log(x) = k*log(2) + log(m)`.
2. **Centrar.** A `m` se lo lleva a `[sqrt(2)/2, sqrt(2))` ajustando `k`, porque el polinomio solo es
   preciso cerca de 1.
3. **Aproximar.** Con `s = f/(2+f)` y `f = m-1`, la serie `log(1+f) = 2s + 2s³/3 + ...` converge
   rapidísimo y siete coeficientes alcanzan para el último bit.

`log(2)` va partido en dos mitades (`ln2_hi`, `ln2_lo`) para que `k*ln2_hi` sea exacto y la parte
baja aporte lo que falta sin cancelarse.

**Verificación**: `LogTest` da **-1250194933** con nuestra VM y con `java` real. Son ~700
evaluaciones de `log` cuyos **bits crudos** se mezclan en el resumen — enteros chicos, el entorno de
1 donde el algoritmo cambia de rama, subnormales, los dos ceros, negativos, infinito, NaN y un
barrido por todos los exponentes — más **150 valores de `nextGaussian()`** de dos semillas,
alternando con `nextInt` para mover la paridad del par guardado. Sin tolerancia: se comparan bits.

Se compara contra `StrictMath.log` y no contra `Math.log` porque `Math.log` tiene permiso de usar un
intrínseco de la máquina; `StrictMath.log` es fdlibm por contrato, que es lo que se escribió.

### Finding #303 — un tipo de `java.lang` del mismo round no lo veía el import implícito

Compilar `java.lang.StrictMath` y `java.util.Random` en la **misma** invocación fallaba: el
`StrictMath` que `Random` nombra no resolvía. Por separado andaba.

```
javac --emit java/lang/Zzz.java pp/Uso.java     ->  "no se encuentra el símbolo: Zzz"
javac --emit java/lang/Zzz.java                     por separado: los dos compilan
javac --emit pp/Uso.java
```

Esa asimetría es la firma exacta del bug. El `import java.lang.*` implícito (§7.3) solo consultaba
el *class finder*, así que veía el `.class` en disco y **no** el hermano que se estaba compilando al
lado. Compilado antes, el `.class` quedaba en el classpath y la segunda invocación lo encontraba.

La causa está en la guardia *source-shadows-classpath* de `try_load`: si algún candidato corresponde
a un tipo del fuente, no se carga nada del classpath —correcto, el tipo ya existe— pero se sale
**sin registrar la clave simple**, que es por donde otra unidad lo buscaría.

**Lo que no se hizo, y es la parte que importa.** El arreglo obvio era aliasar el tipo del fuente en
la tabla de externos, que ya tiene `alias_external` para algo parecido. Habría andado, y habría sido
un error: `check.rs:1428` define "externo" como *estar en esa tabla*, y a los externos les afloja los
chequeos de miembros por la indulgencia de jerarquía incompleta. Meter ahí los tipos del fuente
habría desactivado esos chequeos justo en `java.lang`, que es lo que más se apoya — un arreglo que
apaga un control en silencio es peor que el bug.

Se arregló donde correspondía, en la **resolución**: `SymbolTable::java_lang_source` como fallback,
después del scope (locales, paquete propio e imports explícitos ganan) y **antes** del externo (por
el shadowing del #5: si esta compilación declara `java.lang.X`, ese gana sobre el `.class` homónimo).
Tres sitios: `enter::resolve_name_to_sym`, `attribute::resolve_type_name` y `attribute::resolve_name`
— el último para la posición de expresión, que es la que fallaba en `StrictMath.log(x)`.

Repro: `repros/zz303/`.

### Finding #304 — el diagnóstico señalaba el archivo equivocado

Salió del mismo mensaje de error del #303, y es el peor de los dos:

```
zzpos/Aaa.java:3: error: no se encuentra el símbolo: noExiste
        return 1;                      <- la línea 3 de Aaa, que no tiene nada que ver
               ^
  ubicación: clase Bbb                 <- y acá dice Bbb, contradiciéndose solo
```

El error está en `Bbb.java`. `Aaa.java` no tiene errores. Con varios archivos, **todos** los
diagnósticos se renderizaban contra el primero: la línea y la columna eran correctas y el archivo
no, que es la peor combinación posible porque manda a leer código sano. Yo mismo perdí un rato con
él cinco minutos antes de diagnosticarlo.

El driver lo hacía a propósito, y el comentario lo admitía:

```rust
// El error trae línea y columna pero no de qué archivo: se renderiza contra el primero que
// lo contenga. Con un solo archivo —el caso normal— es exacto.
```

Con un archivo es exacto; con varios es una adivinanza — "el primero con suficientes líneas" — que
sale mal casi siempre.

**Arreglado** dándole al `Error` un campo `unit: Option<usize>`. El sitio que construye el error no
sabe de qué unidad es, y hacérselo llegar querría enhebrar el índice por cientos de llamadas; pero
**cada pase por unidad sí sabe**, y ahí se marca lo que se agregó. `compile_units_cp` marca en el
lexer/parser, en `register_local_classes`/`hoist_anonymous`, en `attribute`, en `check`/`flow` y en
la bajada final; `enter_cp_multi` es una pasada global pero sus bucles son por unidad, así que
también marca. Solo se marca lo que está sin marcar, para que un pase de más adentro que ya supo cuál
era no lo pierda.

La salida ahora es idéntica a la de `javac` real:

```
zzpos/Bbb.java:3: error: no se encuentra el símbolo: noExiste     (nuestro)
zzpos\Bbb.java:3: error: cannot find symbol                       (javac 25)
        return noExiste;
               ^
```

Repro: `repros/zz304/`.

### Finding #305 — `null` era aplicable a un parámetro **primitivo**

El más grave de la tanda, y salió de escribir `Random.from`. Su adaptador llama `super(null)` para
saltear el constructor que llamaría al `setSeed` que el adaptador se niega a atender. El compilador
aceptó, y emitió esto:

```
1: aconst_null
2: invokespecial  // Method java/util/Random."<init>":(J)V
```

Una referencia empujada donde va un `long`, con un slot de pila en vez de dos. **Bytecode que ningún
verificador acepta, salido de un compilador que no dijo nada.**

La causa: `null` se tipaba `RType::Unresolved`, el comodín **indulgente** del compilador —el que
existe para no ahogarse con los tipos del JDK que no modelamos—. Indulgente quiere decir convertible
a cualquier cosa, primitivos incluidos. Las dos mitades del defecto:

```java
static int f(long x) { return 1; }
static int f(Void v) { return 2; }
f(null)   // javac real: 2 (la única aplicable). El nuestro: 1.

static int g(long x) { return 1; }
g(null)   // javac real: "incompatible types: <null> cannot be converted to long".
          // El nuestro: compilaba.
```

**Arreglado modelando el tipo que faltaba**: `RType::Null` (§4.1), con su regla de una línea — es
subtipo de **todo** tipo referencia y de **ningún** primitivo (§4.10.2, §5.3) — puesta en las dos
funciones que deciden esto, `assignable` (asignaciones) y `convertible` (aplicabilidad).

El costo de agregar una variante a un enum de tipos suena alto y no lo fue: el compilador de Rust
señaló **los siete** lugares que faltaba cubrir, uno por uno. Cuatro son texto para diagnósticos
(`<null>`, que es como lo escribe `javac`), dos son "no hay nada que borrar ni sustituir", y el
último es el descriptor, donde el tipo nulo sale `Ljava/lang/Object;`.

**La medición que da confianza**: recompilando la biblioteca entera con el arreglo, cambió el
bytecode de **exactamente dos** clases de 1464 — `Random` y `RandomAdapter`, las dos que se estaban
arreglando. Las otras 1462 salieron byte por byte iguales. Es un arreglo puro, no un cambio de
comportamiento.

Repros: `finding_305.java` (da 2, como `java` real) y `finding_305b.java` (no tiene que compilar).

### Finding #306 — una lambda hacia un genérico de otra clase no compilaba

```
error: el generador de bytecode no puede resolver el tipo `X`
```

Con `<X extends Throwable> T orElseThrow(Supplier<? extends X>) throws X` —que es un método que
acababa de escribir— pasarle una lambda no compilaba. Y andaba si el método genérico estaba en la
**misma clase** que la lambda, una asimetría que no tiene ninguna justificación.

El método sintético de la lambda salía declarando un retorno `X`: la sustitución del SAM resuelve las
variables de la **interfaz**, no las que entran por el **tipo esperado**, y `X` es de un método de
otra clase. Acá no hay declaración a la que apunte.

**Arreglado** borrando a su *erasure* las variables de tipo que ese sintético no puede nombrar
—cualquiera que no sea del método ni de la clase envolvente—. No es una salida por la tangente: el
descriptor usa la erasure igual (§4.6), así que el bytecode sale idéntico; lo único que se pierde es
precisión en el `Signature` de un método privado sintético, que no lee nadie. Va en los dos caminos,
el de la lambda y el de la referencia a método.

Repro: `finding_306.java` (da 1, como `java` real).

### Finding #308 — `() -> c[1]++` era un no-op

El peor de todos, aunque el arreglo sea de una línea: sin error, sin aviso, y **solo** en una de dos
formas que el JLS declara equivalentes.

```java
Runnable r = () -> c[1]++;        // daba 0   ✘
Runnable r = () -> { c[1]++; };   // daba 1   ✔
Runnable r = () -> { ++c[1]; };   // daba 1   ✔
Runnable r = () -> { c[1] += 1; };// daba 1   ✔
```

El cuerpo-expresión de una lambda con SAM `void` es una **posición de descarte**: su valor no se usa,
exactamente igual que en una sentencia-expresión, y ahí `x++` *es* `x += 1`. El desugar tiene esa
función (`discard_expr`, con el comentario que lo explica), y la lambda no la usaba: bajaba su cuerpo
por el camino de expresión. El `++` llegaba crudo al generador, y para un **elemento de arreglo** se
emitía algo que no incrementaba nada. Con una variable local no se notaba: `iinc` anda igual.

Lo encontró la prueba de la tanda, que usa `ifPresentOrElse(v -> c[0]++, () -> c[1]++)` para contar
cuál de las dos ramas corrió. Devolvía 7 —el índice de la primera comprobación fallada— y la clase
que se estaba probando no tenía nada que ver.

Repro: `finding_308.java` (da 1111, como `java` real).

### Finding #307 — la inferencia no mira el cuerpo de la lambda (ABIERTO)

```java
Optional.of("perro").map(x -> x.toUpperCase()).orElseThrow().length()
// nuestro: "no se encuentra el método: length / ubicación: clase Object"
// javac real: compila
```

El `U` sale `Object`: falta la restricción que aporta el **cuerpo** de la lambda (§18.2.1). En
`flatMap` es peor —tampoco le da tipo al parámetro—, porque su firma anida un comodín adentro de otro
(`Function<? super T, ? extends Optional<? extends U>>`).

**No arreglado**: es trabajo de §18, de otro tamaño que el resto de esta tanda. Queda anotado con su
repro (`finding_307.java`, que no compila a propósito), y la prueba de la tanda escribe esos dos
casos con una variable de tipo explícito, con el motivo en un comentario al lado.

### La equivocación que vale la pena contar

`Random.from` **no funcionaba** cuando la escribí. El constructor público `Random(long)` llama a
`setSeed`, que es virtual, así que `new RandomAdapter(g)` invocaba el `setSeed` del adaptador —el que
se niega— **durante su propia construcción**. El objeto explotaba antes de que nadie llegara a usarlo.

No lo dedujo nadie: lo encontró correrlo. Y el arreglo (un constructor sin semilla, `Random(Void)`,
que el adaptador llama con `super(null)`) es lo que destapó el #305, que es el hallazgo más grande de
la tanda. La prueba tiene ahora una comprobación explícita de que `setSeed` falla **al llamarlo** y
no al construir, que es la distinción que se me había pasado.

### Verificación

- **90 clases de `java.util` al 100 %** (eran 84), **1999/2097** miembros (95,3 %; eran 92,7 %).
- `OptRndTest` da **-1** con nuestra VM y con `java` real: 39 comprobaciones, todas verdes. El valor
  es el índice de la primera que falla, así que un fallo dice *cuál*.
  (El primer intento usaba una máscara de bits y estaba mal: 39 comprobaciones no entran en un `int`,
  y `1 << 32` volvía al bit 0, así que las últimas siete se solapaban con las primeras —que ya
  estaban en 1— y no se observaban. La prueba daba -1 igual. Lo dice el comentario del archivo.)
- `LogTest` da **-1250194933** con nuestra VM y con `java` real: ~700 evaluaciones de `log`
  comparadas **por bits**, más 150 `nextGaussian()`.
- Los tres repros nuevos dan lo mismo que `java` real: 305=2, 306=1, 308=1111.
- La biblioteca recompila **1034/1035** (el `SymElement` de siempre, donde javac tiene razón).
- **Punto fijo**: los 1464 `.class` del árbol salen byte por byte iguales al recompilar.
- **Determinismo**: dos recompilaciones seguidas, 1464/1464 idénticas.
- De las **43** pruebas comparables, **41** dan el mismo entero — las dos nuevas entraron a la
  batería permanente. Las dos que no coinciden son las de siempre, `JcIc` y `WdWide`, sin relación
  con esta tanda.
- **1407 tests de Rust pasan**; los 18 que fallan son los mismos 18 de antes de esta tanda, nombre
  por nombre. Se comparó la lista completa, no el conteo: dieciocho fallos podrían ser otros
  dieciocho.

## `RandomGenerator` al 100 %, y el finding que destapó

Siete miembros, y el último de ellos hizo caer un defecto del generador de código que tenía **mal
compilada una familia entera** de la biblioteca sin que ninguna de las dos redes de seguridad lo
viera.

`java.util.random.RandomGenerator`: **26/33 → 33/33**.

### Los siete

| miembro | qué se hizo |
|---|---|
| `of(String)` · `getDefault()` | delegan en `RandomGeneratorFactory`, que ya tenía los doce algoritmos |
| `isDeprecated()` | `false` |
| `nextGaussian()` | método polar de Marsaglia, sobre la `log` de la tanda anterior |
| `nextGaussian(mean, stddev)` | desplazamiento y escala |
| `nextExponential()` | transformada inversa |
| `equiDoubles(...)` | **se niega** — la misma divergencia deliberada de `ints()`/`longs()`/`doubles()` |

Para `of`/`getDefault` hizo falta un `RandomGeneratorFactory.create()` sin argumentos, que tampoco
existía. La delegación es a propósito: si la interfaz y la fábrica eligieran cada una por su cuenta
podrían dejar de coincidir, y "el algoritmo por defecto" pasaría a depender de por cuál de las dos
puertas se entró.

**Qué es contractual y qué no, que acá se separa fino.** El `nextGaussian()` de *esta interfaz* no
promete valores: su javadoc describe la **distribución** y no nombra un algoritmo. El JDK usa un
ziggurat con tablas de 256 entradas; acá está el polar, y los números difieren sin que ninguno de los
dos esté mal. Distinto es `java.util.Random.nextGaussian()`, que **sobreescribe** este default y cuyo
javadoc **sí** nombra el algoritmo: ahí el valor es parte del contrato, y se comprueba por bits en
`LogTest`. La prueba nueva (`RgTest`) compara **propiedades** —los bordes exactos, las excepciones, y
que la distribución sea la que dice ser—, no valores.

### Dos cosas que la prueba corrigió, no yo

**La guarda de `nextGaussian(mean, stddev)`.** La había escrito negada —`!(stddev >= 0)`— para que un
`NaN` también cayera en el `IllegalArgumentException`. Parece mejor y es un apartamiento del
contrato: dice "si `stddev` es negativo", y `NaN` no es negativo. `java` real devuelve `NaN` en vez de
tirar, y `-0.0` tampoco tira. La prueba lo dijo antes de que se fuera al árbol; ahora la guarda es
`stddev < 0.0` y el caso está fijado con una comprobación explícita.

**El javadoc de `isDeprecated`.** Había escrito que el único algoritmo desaconsejado es
`java.util.Random`, por su LCG de 48 bits. Es el candidato obvio y es falso: `java` real devuelve
`false` también para él. Se verificó en vez de darlo por sentado.

### Finding #309 — un `++` sobre un campo, en posición de valor, no emitía **nada**

```java
long getAndIncrement() { return value++; }
```

```
  0: lstore_1     <- guarda desde una pila VACÍA
  1: lload_1
  2: l2i
  3: ireturn
```

El `value++` desapareció. `incdec` empezaba así:

```rust
let Some(Binding::Local { slot }) = target.binding else { return };
```

Ese `return` mudo era todo: si el destino no era un local, **ni código ni diagnóstico**. En posición
de **descarte** no se veía —ahí el desugar reescribe `x++` a `x += 1` y lo resuelve el camino de
asignación compuesta—, así que el defecto solo aparecía cuando alguien usaba el valor. Y no cubría
una esquina: fallaban las cuatro clases de destino (campo de instancia, campo estático, elemento de
arreglo) y los dos tipos de posición (prefijo y postfijo).

**Cómo apareció.** Escribiendo el `create()` de la fábrica usé un `AtomicLong` como contador. Cada
llamada moría con `operand stack underflow`. `AtomicLong.getAndIncrement()` es, literalmente, la línea
de arriba.

**Lo que hace grave a este finding no es el bug: es que ninguna de las dos redes lo vio.**

La biblioteca tiene el punto fijo (los `.class` del árbol no cambian al recompilar) y las pruebas de
comportamiento. El punto fijo **no podía** verlo: los dos lados de la comparación los emite el mismo
compilador, así que coincidían perfectamente **en estar mal**. Recompilar con el arreglo cambió
cuatro clases que llevaban meses versionadas con bytecode roto:

```
java/util/concurrent/atomic/AtomicInteger.class
java/util/concurrent/atomic/AtomicLong.class
java/util/concurrent/atomic/AtomicIntegerArray.class
java/util/concurrent/atomic/AtomicLongArray.class
```

Las cuatro de `java.util.concurrent.atomic`, que es exactamente donde vive `return value++`. Ocho
métodos (`getAndIncrement`/`getAndDecrement` de cada una) que no se podían llamar.

Es la **tercera** vez que el proyecto se lleva la misma lección, y conviene anotarla junto a las
otras dos: un cuerpo vacío (los cinco stubs de `Arrays`) y un miembro heredado de `Object` (#292) ya
habían pasado por debajo de la medición de firmas. Ahora se suma **bytecode mal emitido de forma
consistente**, que pasa por debajo del punto fijo. Las tres veces el único que lo encontró fue
correr el código.

**Arreglado** con el juego de pila que corresponde, que es el mismo que emite `javac` real:

```text
campo de instancia, postfijo:  recv, dup, getfield, dup_x1, const, add, putfield   -> [viejo]
campo de instancia, prefijo:   recv, dup, getfield, const, add, dup_x1, putfield   -> [nuevo]
estático, postfijo:            getstatic, dup, const, add, putstatic               -> [viejo]
elemento, postfijo:            arr, idx, dup2, xaload, dup_x2, const, add, xastore -> [viejo]
```

El destino se evalúa **una sola vez** —re-evaluarlo repetiría sus efectos, y la prueba tiene un caso
con un índice que cuenta sus llamadas— y con un valor de categoría 2 cada `dup` pasa a su forma ancha
(`dup2`, `dup2_x1`, `dup2_x2`). Ahí estaba el *underflow*: una copia de un slot donde hacen falta dos.

**Lo que deliberadamente no cubre.** Un destino `byte`/`short`/`char` necesita además el truncado de
§15.26.2 (`i2b`/`i2s`/`i2c`), que acá habría que emitir a mano. En vez de emitir un incremento que no
trunca —un resultado *equivocado*, no uno faltante— se reporta el caso. Es la misma decisión que ya
tomaba `compound_effectful` unas líneas más abajo, y la diferencia con el estado anterior es toda la
que importa: antes se callaba, ahora avisa.

Repro: `finding_309.java` (once formas, da **2068615891** como `java` real).

### Verificación

- `java.util.random.RandomGenerator` **33/33**; `RandomGeneratorFactory` ganó `create()`.
- `RgTest` da **-1** con nuestra VM y con `java` real.
- `AtomTest` da **-1** con las dos: los ocho métodos de la familia atómica, más los vecinos de cada
  arreglo (un índice mal duplicado escribiría en otra celda).
- `finding_309` da **2068615891** con las dos.
- La biblioteca recompila **1034/1035**, el `SymElement` de siempre.
- Punto fijo restaurado: **1464/1464** idénticos, después de regenerar los `.class` de las cuatro
  clases atómicas, que estaban versionados con bytecode mal emitido.
- Determinismo: dos recompilaciones seguidas, **1464/1464** byte a byte.
- De las **45** pruebas comparables, **43** dan el mismo entero. `RgTest` y `AtomTest` entraron a la
  batería permanente; las dos que no coinciden son las de siempre, `JcIc` y `WdWide`.
- **1407 tests de Rust pasan**, y los 18 que fallan son los mismos 18 de antes, comparados **nombre
  por nombre** y no por conteo.
- El único `++` en posición de valor sobre un no-local que queda en la biblioteca es el de la familia
  atómica, ya arreglado: se barrió la fuente entera buscando la forma.

## Tanda: el merge de `java.lang`, `java.util` al 99,3 %, y `java.time`

Cuatro findings, todos salidos de escribir biblioteca. Dos de ellos —#311 y #313— son de la clase
que solo aparece corriendo el código, y uno de esos es el peor de la sesión.

### Lo que se cerró

| paquete | antes | ahora |
|---|---|---|
| `java.util` | 1944/2097 (92,7 %) | **2082/2097 (99,3 %)**, 107 de 109 clases al 100 % |
| `java.time.temporal` | 131/173 (75,7 %) | **173/173**, las 16 clases al 100 % |
| `java.time` | 458/892 | 545/892, con `Duration` 62/62 y `Period` 43/43 |

De `java.util` quedan **15 miembros**, todos de `Scanner` y `Formatter`, y todos esperando lo mismo:
que la VM sepa leer y escribir archivos. El `java.io.File` que llegó con el merge es manipulación de
rutas nada más —`exists()` devuelve `false` siempre y no hay nativos de filesystem—, así que escribir
`new Scanner(archivo)` daría un miembro que **miente sobre el mundo**: reporta que un archivo que
existe no existe. Se dejan afuera y se dice por qué.

### El merge, y las dos cosas que git no podía ver

Nueve conflictos de fuente sobre 277 archivos. `Math`/`StrictMath` los auto-mergeó dejando **dos
`log`** —la de ellos vía `FdLibm` y la que yo había escrito la tanda anterior—; quedó la suya, que
viene con las dieciocho trascendentes. En `Iterable` quedó su cuerpo (tiene el chequeo de null que el
JDK hace) con mi javadoc. Las otras cinco se resolvieron con mis versiones, verificando firma por
firma que nada existiera solo del lado suyo.

Lo interesante es lo que el auto-merge dejó **roto y limpio a la vez**:

- `NoSuchElementException` quedó con **constructores duplicados**: los dos lados habían agregado el
  mismo par, y un merge textual los concatena sin protestar.
- Su `Thread.sleep` ahora declara `throws InterruptedException` —correctamente; el nuestro era
  `native` sin `throws`— y eso destapó **tres llamadas nuestras** que no la manejaban, en
  `CompletableFuture` y `ScheduledThreadPoolExecutor`. Ahora restauran la marca de interrumpido y
  cancelan, que es lo que significa una interrupción ahí.

Ninguna de las dos las ve git. Las vio el compilador, en la recompilación de la biblioteca entera.

### Finding #310 — un primitivo pasado a un `Object...` no se boxeaba

```java
String.format("%d", 42)   // "expected a reference, found Int(42)"
```

El `42` entraba **crudo** al `Object[]` del varargs. Una de las líneas más comunes que tiene Java.

La causa estaba escrita en un comentario, y decía lo contrario de lo que hacía falta:

```rust
// Con varargs los argumentos de cola aún no se empaquetaron en un array (eso lo hace el
// desugar, después): se convierten solo los del prefijo fijo.
```

De "el array se arma después" se concluía "no hay nada que convertir", y no: **el elemento** ya es un
target válido. Los de cola se convierten ahora contra el tipo elemento del array, y eso cubre las
cuatro formas de una sola vez — un `int` contra `Object` boxea, un `Integer` contra `int...`
desboxea, un `String` contra `Object` no hace nada, y el paso directo del array tampoco, porque
`coerce` no tiene arm para `(Array, _)`. No hay que distinguirlos a mano.

Repro: `finding_310.java` (da 212112, como `java` real).

### Finding #311 — la referencia de una lambda quedaba stale si el GC corría durante su constructor

El más serio de la sesión.

```rust
let object = allocate(...);                    // un offset crudo dentro del heap
self.call_java(ctor, ...);                     // código real: puede asignar, puede colectar
self.top().push(Value::Reference(object));     // <- la dirección VIEJA
```

El recolector mueve los objetos vivos y actualiza las raíces —la pila de operandos entre ellas— pero
no puede saber nada de una variable local de Rust. Si la colecta caía durante el `<init>`, lo que se
empujaba era una referencia a memoria ya reciclada, y quien llamaba veía un `NullPointerException`
que no tenía nada que ver con su código.

**Cómo se acorraló, que es la parte que vale.** El síntoma dependía de la presión de asignación, y esa
dependencia es lo que lo volvió localizable:

- con **ocho** call sites en el método andaba, con **nueve** fallaba, y con **diez** volvía a andar —
  con diez, el noveno es un `Consumer` que un `Optional` vacío nunca invoca, así que el caso roto no
  se ejercitaba;
- con `JVM_GC_EDEN_SIZE` agrandado —o sea, sin colectas— andaba siempre;
- y el `.class` que emite **nuestro** javac corre bien en `java` real.

Lo último es lo que lo ubica: el class file está bien, el defecto es de la VM. Sin esa comprobación
el sospechoso natural habría sido el emisor, y la búsqueda habría empezado en el lugar equivocado.

**Arreglado** empujando la referencia **antes** de correr el constructor, con lo que pasa a ser una
raíz y el recolector la reubica junto con el objeto. Es, exactamente, para lo que el
`new`/`dup`/`invokespecial` de javac tiene el `dup`: la copia en la pila no es una comodidad, es lo
que mantiene viva y al día la referencia mientras corre el constructor.

Repro: `finding_311.java` (da 11, como `java` real).

### Finding #312 — un `import` explícito del mismo round no resolvía en posición de expresión

```
javac --emit pp/Tipo.java qq/Uso.java   ->  "no se encuentra el símbolo: variable Tipo"
```

Hermano del #303: aquél era el `import java.lang.*` implícito, éste es un `import` escrito. Y el
diagnóstico decía **variable**, no tipo, porque en posición de **tipo** sí resolvía — faltaba solo en
posición de expresión, que es donde `Tipo.uno()` pone el nombre.

La causa es la misma guardia *source-shadows-classpath*: `try_load` ve que el tipo ya existe en el
fuente, no carga nada del classpath —correcto— y sale **sin anotar que ese nombre corto lo designa**.

El arreglo generaliza el del #303: un mapa `source_aliases` —nombre simple → tipo del fuente— que se
llena solo para los nombres que la unidad puede escribir cortos (su paquete, un `import` de un solo
tipo, `java.lang`). Un homónimo que nadie nombró no se vuelve visible por estar en el round.

Va **aparte** de `externals`, y esa separación es la misma decisión del #303: `check.rs` define
"externo" como *estar en aquel mapa*, y a los externos les afloja los chequeos de miembros.

Repro: `repros/zz312/`.

### Finding #313 — `this.toString()` dentro de un `default` de interfaz no resolvía

```java
interface Base {
    default String nombre() { return this.toString(); }
}
// error: no se encuentra el símbolo: toString / ubicación: clase Base
```

Y es código legal: una interfaz **declara implícitamente** un método abstracto por cada método
público de `Object` (§9.2), justamente para que esto se pueda escribir.

La búsqueda de miembros sube por `super_class` y por las superinterfaces, y una interfaz **no tiene
superclase** — así que a `Object` no se llegaba nunca. El arreglo agrega `Object` al cierre cuando el
tipo es una interfaz sin superclase, que es exactamente lo que dice la regla.

Es la misma familia que el **#292**, del otro lado: aquél era la **medición** ignorando los miembros
de `Object`; éste es la **resolución** no llegando a ellos. Dos formas distintas del mismo punto
ciego, encontradas con meses de diferencia.

Lo destapó escribir `TemporalField.getDisplayName`, que devuelve `this.toString()`.

Repro: `finding_313.java` (da 4102, como `java` real).

### Lo que completar una interfaz destapó

`TemporalField` estaba en 5/12. Al completarlo —`getBaseUnit`, `getRangeUnit`, `range`,
`rangeRefinedBy`, `adjustInto`— **tres implementadores dejaron de compilar**: `IsoFields.IsoField`,
`JulianFields.JulianField` y `WeekFields.ComputedField`. Los tres estaban incompletos y nada lo
notaba, porque la interfaz que debían cumplir también lo estaba.

Es un efecto que conviene esperar y no lamentar: completar un tipo base es lo que vuelve visibles a
los que se apoyaban en que estuviera incompleto. Los tres se cerraron, y `ChronoUnit` ganó de paso el
`getDuration`/`addTo` que le faltaba.

### Dos expectativas mías que la prueba corrigió, no yo

**`Duration.truncatedTo` trunca hacia cero, no hacia abajo.** `-1.5s` a segundos es `-1s`, y `-90s` a
minutos es `-60s`. Yo lo escribí con la corrección del resto negativo —o sea, `floor`— porque
"truncar" suena a eso, y para los negativos es la dirección opuesta. Lo dijo `DurTest` corrido contra
`java` real, antes de que se fuera al árbol.

**`nextGaussian(mean, NaN)` no tira.** Había escrito la guarda negada (`!(stddev >= 0)`) para que el
`NaN` cayera en el `IllegalArgumentException`. Parece mejor y es un apartamiento del contrato, que
dice "si `stddev` es negativo" — y `NaN` no es negativo. `java` real devuelve `NaN`.

Las dos son del mismo tipo: una regla que parece más prolija que la del contrato, y que por eso
mismo está mal. Las dos quedaron fijadas con una comprobación explícita y un comentario que dice qué
pasó.

### Verificación

- La biblioteca recompila **1083/1085**. Los dos que fallan son `SymElement` y `StructuredTaskScope`,
  los dos de siempre.
- **1439 tests de Rust pasan**; los 20 que fallan son los 18 de `javac::` de siempre más `JcIc` y
  `WdWide`, que son las dos discrepancias conocidas del arnés ahora también encodadas como tests.
  Se comparó la lista completa, no el conteo.
- De las **47** pruebas comparables, **45** dan el mismo entero que `java` real. Las nuevas de esta
  tanda —`SeqTest`, `IoBridgeTest`, `RgTest`, `AtomTest`, `DurTest`, `PerTest`— dan todas -1.
- Los cuatro repros nuevos dan lo mismo que `java` real: `finding_310` → 212112, `finding_311` → 11,
  `finding_313` → 4102, y `zz312/` compila junto.
- **Nota sobre el árbol overlay**: los 27 tests de `library_conformance` que trajo el merge fallaban
  por una razón que vale anotar — el `KajiLibrary` de la copia limpia era el de **antes** del merge,
  así que probaban una biblioteca vieja contra un compilador nuevo. No era un bug: era una fecha. Es
  la misma lección que `bin/FROZEN.md` documenta para los `.exe`, un nivel más adentro.

## Tanda: `java.util` al 100 %, el acceso a archivos, y `java.time`

`java.util` **2097/2097 — las 109 clases**, desde 1944/2097 (92,7 %) al empezar la sesión. Los
últimos catorce miembros no eran una pieza de `java.util`: eran el nivel de abajo.

### El acceso a archivos

`Scanner(File)`, `Scanner(Path)`, `Formatter(File)` y `Formatter(String)` estaban bloqueados desde
hacía dos tandas, y siempre por lo mismo: **la VM no sabía abrir un archivo**. El `java.io` que llegó
por merge trajo los streams (`PrintStream` real, `BufferedReader`, `InputStreamReader`,
`OutputStreamWriter`) pero no `FileInputStream` ni nativos de filesystem — `File.exists()` devolvía
`false` siempre.

Escribirlos igual habría dado un `new Scanner(archivoQueExiste)` que tira `FileNotFoundException`:
un miembro que **miente sobre el mundo**, no uno que falta. Por eso quedaron afuera hasta ahora.

**Seis nativos, y nada más.** `jdk.internal.io.Fs`: `readAllBytes`, `writeAllBytes`, `stat`, `size`,
`delete`, `mkdir`. Todo lo demás —`File.getParent`, `FileInputStream`, `Scanner`, `Formatter`— se
construye arriba en Java, donde se lee, se prueba y se corrige sin recompilar la VM. Bajar a Rust es
la excepción, y esta lista es exactamente donde no hay alternativa.

**Tres decisiones que son diferencias reales con el JDK**, y las tres están escritas en el código
donde alguien las va a leer:

- **El archivo se lee o escribe entero de una.** No hay descriptor abierto, ni posición, ni `close`
  que pueda faltar. Un archivo de un giga entra en memoria dos veces; a cambio no hay ningún estado
  que se pueda quedar colgado, que es la clase de error más difícil de encontrar en una VM. Cuando
  haga falta streaming de verdad, la puerta es agregar un handle en `Fs` — `Scanner` y `Formatter`
  hablan con `InputStream`, no con el nativo, así que no se enteran.
- **`FileOutputStream` hay que cerrarlo.** Lo escrito se acumula y va al disco en `flush`/`close`. En
  el JDK conviene cerrar porque el sistema operativo termina escribiendo lo ya entregado; acá no hay
  nada entregado hasta el `flush`, así que un stream abandonado **pierde** lo que quedaba. Lo dice el
  javadoc de `Formatter(File)`, que es donde el usuario se lo va a encontrar.
- **`createNewFile()` no es atómico.** Entre el `exists()` y la escritura otro proceso puede crear el
  archivo. El javadoc del JDK promete atomicidad y éste no la tiene, así que se dice de frente en vez
  de dejar que alguien lo descubra.

**Ninguno de los nativos tira.** Devuelven `null`, `false` o cero, y el lado Java decide qué
excepción corresponde. Es a propósito: el nativo no tiene con qué distinguir "no existe" de "no tengo
permiso", y adivinar mal sería peor que no decir nada. `FileInputStream` sí distingue el directorio,
que falla de una forma distinta y con su propio mensaje — confundirlo con "no existe" manda a buscar
al lugar equivocado.

**Y `stat` devuelve las cinco banderas juntas**, no cinco nativos. Salen de una sola consulta al
sistema: preguntarlas por separado tocaría el disco cinco veces y —peor— podría dar respuestas de
momentos distintos si algo cambia en el medio.

Se siguió la convención que la otra sesión dejó escrita en `IOException`: **todo `java.io` de esta
biblioteca es sin `throws`**, porque las bases se escribieron así y un override no puede ensanchar
las excepciones chequeadas (§8.4.8.3). `FileInputStream`/`FileOutputStream` señalan con
`UncheckedIOException` envolviendo la `IOException`, con la nota que explica por qué — el motivo no se
pierde y el código compila contra esas bases.

`FileTest` da **-1** con nuestra VM y con `java` real: 44 comprobaciones que crean, escriben, leen,
anexan, truncan y borran archivos de verdad, más directorios, `Scanner` sobre archivo y por `Path`, y
`Formatter` a archivo. La prueba crea sus propios archivos en vez de leer alguno del repo, para que
los dos lados vean exactamente el mismo contenido.

### `java.time`

| paquete | al empezar | ahora |
|---|---|---|
| `java.time.temporal` | 131/173 (75,7 %) | **173/173 — 100 %**, las 16 clases |
| `java.time` | 458/892 (51,3 %) | ~700/892 (78 %) |
| `java.time.chrono` | 287/538 (53,3 %) | 360/553 (65 %) |

Cerradas: `Duration` 62/62, `Period` 43/43, `Instant` 50/50, `Clock` 16/16, `ZoneId` 14/14,
`ZoneOffset` 24/24, `InstantSource` 7/7, `DayOfWeek`, `Month`, `ChronoField`, `ChronoUnit`,
`TemporalField`, `TemporalUnit`, `TemporalAccessor`, `TemporalAdjusters`, `IsoFields`,
`JulianFields`, `WeekFields`. Y `LocalDate` 87/88, `LocalTime` 65/66, `LocalDateTime` 85/93,
`Year` 47/48, `YearMonth` 50/51, `MonthDay` 29/30.

**Completar una interfaz base rompe a los que se apoyaban en que estuviera incompleta, y eso es lo
que uno quiere.** Pasó tres veces seguidas y conviene anotarlo como patrón:

- completar `TemporalField` dejó sin compilar a `IsoFields`, `JulianFields` y `WeekFields`;
- completar `ChronoLocalDate` dejó sin compilar a `MinguoDate`, `ThaiBuddhistDate`, `JapaneseDate` y
  `HijrahDate`.

Los siete estaban incompletos y **nada lo notaba**, porque el contrato que debían cumplir también lo
estaba. El compilador los delató en la recompilación, uno por uno. Los siete se cerraron.

De paso, los métodos puente que le faltaban a `LocalDate` no eran un hueco del generador —sí los
emite— sino que `ChronoLocalDate` no declaraba los retornos estrechados. Completándolo, los puentes
salieron solos.

### Lo que queda afuera, y por qué

Siete `parse(CharSequence, DateTimeFormatter)` —en `LocalDate`, `LocalTime`, `LocalDateTime`, `Year`,
`YearMonth` y `MonthDay`— siguen sin escribirse: `DateTimeFormatter` está en 3/48 y **no sabe
parsear**. Escribirlos sería la misma clase de mentira que `Scanner(File)` antes de los nativos.

Y una limitación del compilador anotada sin arreglar: `Temporal.super.plus(...)` —la forma
cualificada de llamar al `default` de una superinterfaz, §15.12.1— no la parsea. En `ChronoLocalDate`
se repitió el cuerpo, que es una línea y hace lo mismo, con el comentario que lo dice.

### Verificación

- **`java.util` 2097/2097**, las 109 clases al 100 %.
- La biblioteca recompila **1091/1093**; los dos que fallan son `SymElement` y `StructuredTaskScope`,
  los dos de siempre.
- **1441 tests de Rust pasan**; los 20 que fallan son los 18 de `javac::` de siempre —comparados
  nombre por nombre, no por conteo— más `JcIc` y `WdWide`.
- `FileTest` da **-1** con las dos VMs.

## Tanda: `java.time` al 100 %, `chrono` casi, y el parseo

`java.time` **892/892 — las 25 clases**, desde 458/892 (51,3 %) al empezar la sesión y 876/892 al
empezar esta tanda. `java.time.temporal` sigue en **173/173**. `java.time.chrono` pasó de 376/553 a
**576/589**.

### Lo que destrabó el paquete entero

Los últimos nueve miembros de `java.time` eran el mismo: `parse(CharSequence, DateTimeFormatter)`, uno
por clase, y los nueve estaban bloqueados por lo mismo — **el formateador no sabía parsear**. Era el
caso `Scanner(File)` otra vez: escribirlos sin el nivel de abajo habría dado nueve métodos que tiran.

Se le enseñó a parsear **exactamente el lenguaje que ya sabía formatear**, y esa simetría es el
diseño, no una casualidad: lo que este formateador escribe, lo vuelve a leer. Un patrón con una letra
que no soporta falla igual en los dos sentidos y con el mismo mensaje, en vez de escribir algo que
después no se puede releer. De paso se agregaron las letras que faltaban en los dos lados —`X`, `x`,
`Z` para el desplazamiento y `V` para la zona—, que son las que `OffsetTime`, `OffsetDateTime` y
`ZonedDateTime` necesitan para que su `parse` sirva de algo.

Lo que devuelve el parseo es una bolsa de campos (`Parsed`), no una fecha: quien parsea no sabe —ni
tiene por qué— en qué clase van a entrar. Los campos **crudos se conservan además de los resueltos**,
y eso importa: `Year.from` lee `YEAR` directo, así que si la resolución los consumiera, un patrón de
`"yyyy"` no daría un `Year`.

### `TemporalQueries` devolvía un objeto nuevo cada vez

Un error que no rompía nada visiblemente y que estaba en el centro de todo. Un temporal reconoce qué
le preguntaron comparando `query == TemporalQueries.zone()` **por identidad** —así está en el JDK y
así estaba escrito en toda esta biblioteca— porque una `TemporalQuery` no tiene ningún otro dato con
el que identificarse. `zone()` devolvía `new ZoneIdQuery()`, así que **ninguna de esas comparaciones
acertó nunca**: todos los `query()` caían al `queryFrom` genérico, que para las consultas marcadoras
devuelve `null`.

Todo compilaba. `zonedDateTime.query(zone())` daba `null`, y el síntoma salía tres capas más arriba
como un "Unable to obtain ZonedDateTime" al parsear. Lo encontró `FmtTest`.

### Dos huecos más que la prueba destapó

- **`ZonedDateTime` delegaba `isSupported`/`getLong`/`get` enteros en su `LocalDateTime`**, que no
  tiene `OFFSET_SECONDS` ni `INSTANT_SECONDS`. Formatear una fecha con zona con un patrón que llevara
  `X` fallaba, y desde afuera parecía un problema del formateador.
- **Veinticuatro `throw new IllegalArgumentException()` pelados** en `java.time`, sobre un campo o una
  unidad no soportada. El contrato pide `UnsupportedTemporalTypeException`, y la diferencia no es de
  nombre: esa **extiende `DateTimeException`**, que es lo que el resto de `java.time` atrapa. Un IAE
  sin mensaje se escapaba de todos esos `catch` y salía sin decir qué campo era.

### Finding #314 — el generador no encontraba un tipo del fuente nombrado entero

En una compilación **multi-unidad**, un tipo hermano de **otro paquete** escrito con su nombre
completo no resolvía en el generador: `resolve_type_id` buscaba en el scope, en los externos por
nombre simple, y bajaba a los anidados, pero **nunca miraba las clases del fuente por su nombre
cualificado**. Como los externos se indexan por nombre simple y el *shadowing* del fuente (#5) impide
—con toda la razón— cargar del classpath un tipo que está en el round, no quedaba ningún camino.

Lo audita `audit_declared_types`, así que salía como «el generador de bytecode no puede resolver el
tipo `java.time.chrono.ChronoZonedDateTime`» sobre una clase que estaba en la misma línea de comandos.

**Por qué ninguna de las tres redes lo veía**, que es la parte que vale la pena anotar: la biblioteca
recompila **de a un archivo**, así que el hermano llega como externo por el `-cp` y ese camino no se
pisa; el punto fijo compila igual, de a uno; y el multi-unidad solo lo usaba APT, cuyos generados no
se nombran entre paquetes. Un hueco exactamente en la intersección de lo que ninguna prueba hacía.

La consecuencia práctica era que **dos clases que se referencian mutuamente no se podían escribir**:
ninguna compila primero, y juntas tampoco. Salió al escribir `ChronoLocalDateTimeImpl` y
`ChronoZonedDateTimeImpl`, que se nombran una a la otra.

Antes de esto se persiguió un fantasma: `--check` con dos archivos también fallaba, y parecía el mismo
bug. No lo era — **`--check` compila de a un archivo por diseño**, así que el repro no probaba lo que
parecía. La ablación (revertir el arreglo y ver si el síntoma vuelve) lo mostró en un minuto, y el
cambio que se había hecho en `enter.rs` se revirtió por innecesario.

### `atTime` perdía el calendario

`minguoDate.atTime(hora)` devolvía un `LocalDateTime`, y un `LocalDateTime` **es del calendario ISO**:
su `getChronology()` contestaba `ISO` sobre una fecha Minguo. El día epoch era correcto y todo lo
demás mentía. Se escribieron `ChronoLocalDateTimeImpl` y `ChronoZonedDateTimeImpl` —las dos que el JDK
tiene de paquete— y ahora el ISO sigue dando `LocalDateTime`, que sabe más, y los otros calendarios
dan la implementación que los conserva.

### `Chronology`, otra vez el patrón de la interfaz base

`Chronology` estaba en 11/34 y **las cinco cronologías heredaban su hueco**. Completarla —seis métodos
abstractos, doce `default`, cuatro estáticos— las cerró a las cinco de una: las cuatro no-ISO pasaron
de 11/33 a **33/33** y `IsoChronology` de 10/39 a **39/39**. Lo mismo con `Era` (1/8 → 7/8), que cerró
las cinco eras, y con `ChronoLocalDateTime`/`ChronoZonedDateTime`, cuyas redeclaraciones covariantes
son las que hacen que el compilador emita los **métodos puente** en `LocalDateTime` y `ZonedDateTime`.

### Lo que queda afuera, y por qué

- **`getDisplayName(TextStyle, Locale)`** en `Chronology` y en las cinco `Era`: los nombres traducidos
  de calendarios y eras son datos del CLDR, no código. Devolver `"CE"` para cualquier locale sería un
  miembro que **miente sobre lo que le pidieron**. Misma pared que la tzdb.
- **`Chronology.ofLocale`** solo mira la extensión Unicode `ca` explícita. El JDK además tiene el mapa
  de "qué calendario usa cada región", que es la misma pared; sin él, `th-TH` da ISO acá y budista en
  el JDK. Se prefiere esa diferencia, visible y escrita, a un mapa parcial que acierte a veces.
- **`ChronoPeriodImpl.writeReplace()`**: es el gancho de la serialización de Java, que esta biblioteca
  no implementa.
- El resto de `java.time.format` (65/145): los `ISO_*` predefinidos, los localizados, `DecimalStyle`,
  `parseBest`, `toFormat`.

### Verificación

- **`java.time` 892/892**, las 25 clases al 100 %. `java.time.temporal` **173/173**.
  `java.time.chrono` 576/589.
- `FmtTest` da **-1** con nuestra VM y con `java` real: 60 comprobaciones de parseo, ida y vuelta, y
  casos que **no** tienen que andar. Las que dependen del locale —nombres de mes, AM/PM— se
  comprueban por ida y vuelta y no contra texto escrito a mano: el `java` real en español escribe
  `jul` y `a. m.`, así que comparar la cadena compararía el locale y no el parser.

## Tanda: `java.time.chrono` cerrado, y el medidor que contaba mal

`java.time.chrono` **588/589**, desde 576/589. Lo que queda es un solo miembro, y es el que no se
puede escribir: `ChronoPeriodImpl.writeReplace()`, el gancho de la serialización de Java, que esta
biblioteca no implementa.

### Un bug que sólo la prueba de comportamiento podía ver

`ChronoLocalDate` no tenía `query(...)`, así que `Chronology.from(minguoDate)` contestaba **ISO**.
Es exactamente el mismo mecanismo del bug de `TemporalQueries` de la tanda anterior: las consultas
marcadoras se reconocen **por identidad**, y si nadie las reconoce el `queryFrom` genérico devuelve
`null` — que acá terminaba cayendo en el ISO por defecto.

Y es exactamente el tipo de error que la medición por forma **no puede ver**: `ChronoLocalDate`
figuraba en 36/36 porque el JDK declara `query` como `default` heredado de `TemporalAccessor`, así
que el cierre de herencia lo daba por presente. Lo encontró `ChronoTest`, en la comprobación 46.

**`ChronoTest` da -1 con nuestra VM y con `java` real**: 90 comprobaciones sobre los cinco
calendarios, sus eras, los períodos y los dos compuestos. Es la primera vez que este paquete se
ejercita — hasta ahora estaba medido y nada más.

### `atTime` y los retornos estrechados

`ChronoLocalDateTimeImpl` ahora estrecha `with`/`plus` a sí misma, como el JDK. La sutileza que hubo
que escribir: **no puede pasar por `of`**, porque `of` divierte una fecha ISO a `LocalDateTime`. No
hay contradicción — un `Impl` nunca lleva una fecha ISO, porque `of` la habría divertido antes de
construirlo — y lo que garantiza que siga siendo así es el `ensureValid` del JDK, acá `asegurar`:
mezclar calendarios tira `ClassCastException` en vez de producir un híbrido.

### `getDisplayName`, y por qué la nota anterior estaba mal

La tanda pasada dejó afuera los seis `getDisplayName(TextStyle, Locale)` con este argumento:
*"devolver `CE` para cualquier locale sería un miembro que miente sobre lo que le pidieron"*. El
argumento es correcto y no aplica, porque **considera una sola opción**: inventar un nombre.

El contrato tiene una tercera rama, y la nombra él mismo. El javadoc de `Era.getDisplayName` dice
*"If no textual mapping is found then the numeric value is returned"*, y la implementación de
`Chronology.getDisplayName` en el JDK termina en
`Objects.requireNonNullElseGet(name, () -> chrono.getId())`. Esta biblioteca no trae datos del CLDR,
así que esa rama se toma **siempre** — que es una diferencia con el JDK, no una mentira.

Lo que decide la cuestión es que **el número se anuncia solo**: nadie confunde `"1"` con un nombre
traducido, mientras que un `"CE"` devuelto para un locale francés pasaría por bueno. La opción
descartada sigue descartada; la que se implementó es otra.

### El medidor contaba mal las variables de tipo

`apidiff` borraba los genéricos (`<...>` fuera) pero **no sustituía las variables de tipo por su
cota**. El JDK imprime `public default D toLocalDate()`, y `D extends ChronoLocalDate` borra
exactamente a `ChronoLocalDate` (§4.6) — que es lo que la implementación declara. El miembro estaba
escrito bien y se contaba como faltante.

Es el peor tipo de error de medición: **empuja a "arreglar" código que no está roto**. Ahora `parse`
lee los parámetros de tipo de la declaración de la clase (y los del propio miembro, para un
`static <R extends X> Foo<R> of(R)`) y los sustituye por su borrado.

**El impacto es chico y verificado**, que es como hay que reportar un cambio de regla: 11 miembros en
toda la biblioteca, en cuatro paquetes — `java.lang.invoke` 239→241, `java.util.concurrent`
1028→1032, `java.util.random` 50→52, y 3 de los 12 que ganó `chrono`. Ningún paquete cambió de
denominador.

### Verificación

- `java.time.chrono` **588/589**; `java.time` **892/892**; `java.time.temporal` **173/173**.
- La biblioteca recompila **1093/1095**; los dos de siempre.
- Punto fijo: **1569 idénticos, 0 distintos**.
- De las **52** pruebas comparables, **50** dan el mismo entero que `java` real; las dos que no son
  `JcIc` y `WdWide`. La nueva es `ChronoTest`.
- La biblioteca entera: **10.897/11.611 miembros (93,9 %)** en 39 paquetes, 14 de ellos al 100 %.
  El detalle por paquete está en `docs/completitud.html`.

## Tanda: de los más completos hacia abajo, y el medidor que engañaba

Se pidió cerrar todos los paquetes en orden de completitud. **Cerrados en esta tanda** (por miembro):
`java.util` 2152/2152, `java.lang.reflect` 323/323, `java.lang.constant` 163/163,
`java.lang.invoke` 241/241, `java.util.random` 53/53, `jdk.internal.random` 73/73,
`jdk.internal.util.random` 38/38, `java.util.concurrent.locks` 59/59. Y muy cerca:
`java.lang` 1718/1719, `java.io` 643/645, `java.time.zone` 59/60, `java.util.zip` 228/240.

### El punto ciego del medidor, que es lo más importante de la tanda

`medir_todo.py` recorre **nuestras** clases. Una clase que no existe en el árbol es **invisible**
para esa medida, así que un paquete al que le falta la mitad de sus tipos puede figurar al 100 %.

No es hipotético:

| paquete | miembros | clases públicas |
|---|---|---|
| `java.util.concurrent.locks` | **59/59** | **5/10** — falta `AbstractQueuedSynchronizer`, sobre la que se apoya el paquete entero |
| `java.nio.channels` | **3/3** | **2/53** |
| `java.security` | 50/58 | **8/81** |
| `java.net` | 80/102 | **7/64** |
| `java.nio.file` | 62/65 | **10/44** |

Medido en las dos dimensiones: **674 de 952 clases públicas** en los paquetes empezados. El gráfico
(`docs/completitud.html`) ahora muestra las dos barras, con la de clases arriba porque es la que
**acota** a la otra: un 100 % de miembros sobre un 20 % de clases es un 20 %.

### Un chequeo de excepciones que dependía del orden de carga

`ReentrantLock.lock()` llamaba a `sync.wait()` sin manejar `InterruptedException`, y **compilaba**.
Agregar un `import` distinto en el mismo archivo lo hacía dejar de compilar. La causa es de la misma
familia que #251: si `java.lang.Object` se cargó por un camino que no leyó su atributo `Exceptions`,
sus métodos se ven como no-lanzadores; cargar otros tipos cambia ese camino.

Salió al agregar una `ArrayList` a `ReentrantLock`. Lo que destapó vale más que el síntoma: **cinco
esperas de los dos locks reentrantes ignoraban la interrupción**. Ahora hacen lo del JDK — atrapan,
siguen esperando, y **remarcan** el hilo al final, que es la diferencia entre "no me interrumpe" y
"me trago la interrupción".

De paso cayeron tres notas que ya no eran ciertas:

- `lockInterruptibly()` decía *"la VM no tiene interrupción de hilos — idéntico a `lock()`"*.
  `Thread.interrupt()` existe y despierta las esperas, así que ahora aborta de verdad.
- Los `tryLock` con plazo decían *"la biblioteca no tiene reloj para recalcular"*.
  `System.nanoTime()` existe: ahora esperan el **plazo restante** en un bucle, en vez de un solo
  intento que devolvía `false` con el plazo entero por delante.
- `readLock()`/`writeLock()` devolvían `Lock` por el finding #108. Ese finding se cerró: ahora
  devuelven el tipo anidado, y `rwl.writeLock().getHoldCount()` compila.

### Finding #314, segunda mitad

La primera mitad (tanda anterior) era un tipo del fuente nombrado **entero**. Esta es el mismo tipo
nombrado **corto**, por un `import`: `resolve_type_id` tampoco consultaba el mapa de alias que
`try_load` registra al detectar el shadowing (#312). Las dos mitades tienen la misma forma y la
misma causa — la pasada 1 sabe resolver las dos y el generador no consultaba ninguno.

Repro: `scratchpad/zz314/`, con `Base.java` + `Impl.java` (entero) y `Usuario.java` (corto).

### Finding #315 — el literal hexadecimal de coma flotante

`0x1.0p64` (§3.10.2) no lo parsea el compilador: *"se esperaba Semi, se encontró DoubleLiteral"*.
Sale en los generadores xoshiro, donde el JDK escribe así las distancias de salto. Se usó
`Math.scalb(1.0, 64)`, que es **el mismo double** —una potencia de dos entra sin redondeo— en vez del
decimal, que para 2^192 son 58 dígitos. Queda anotado, no arreglado.

### Lo que se escribió, y lo que se corrigió de paso

- **`RandomSupport` 38/38**: los mensajes compartidos, las cuatro constantes irracionales, los ocho
  `check*`, los ocho `boundedNext*`, y los dos `convertSeedBytes*`. Los tres `compute*` **delegan** en
  el generador: el JDK usa el ziggurat de McFarland (dos tablas generadas), esta biblioteca el método
  polar — misma distribución, otra secuencia, y ya estaba así en `RandomGenerator`.
- **Los diez generadores**: constructor sin argumentos, constructor de `byte[]`, y particiónn real
  (`SplittableGenerator`) para los ocho LXM; salto y salto largo para los dos xoshiro, con
  `JumpableGenerator`/`LeapableGenerator` nuevas en `RandomGenerator`.
- **`RndSeedTest` da el mismo entero que `java` real**: 1222898991. El mapeo de bytes a estado, las
  tablas de salto y la partición coinciden **bit por bit**. Encontró un error real: el campo `p` de
  los generadores de 1024 bits se inicializa en la **declaración** en el JDK (`= N - 1`), y el
  constructor de `byte[]` que escribí no lo ponía — el anillo arrancaba desplazado una palabra y
  emitía otra secuencia. Ahora se inicializa en la declaración, como allá, para que ningún
  constructor pueda olvidarlo.
- **`ClassLoader`**: los tres `find*` no eran sólo miembros faltantes. `getResource` devolvía `null`
  directo, así que **una subclase que sobrescribiera `findResource` era ignorada** — un método
  público que no cumple el protocolo que documenta. Ahora delega. Y `definePackage` era la mitad que
  faltaba de un par cortado: estaban `getDefinedPackage(s)` y no había nada que poblara su registro.
- **`URL.openStream()`** (sólo `file:`, vía los nativos de `Fs`), con `UnknownServiceException` para
  el resto de los esquemas — que es lo que distingue "la URL no se entiende" de "se entendió y no hay
  cómo atenderla".
- **`ZipInputStream`/`ZipOutputStream` con `Charset`**: no era sólo un constructor. Los nombres de
  entrada se leían y escribían **byte a byte como Latin-1**, y el largo declarado en la cabecera era
  el de los caracteres y no el de los bytes — un nombre con acentos producía un archivo **corrupto**.
- **`ZoneRulesProvider` es un SPI de verdad**: antes era una clase abstracta con dos estáticos que
  leían la tabla directo, o sea la forma de un SPI sin el mecanismo — un `provideRules` sobrescrito no
  lo llamaba nadie. Ahora hay lista de proveedores, `registerProvider`, y el de la tabla embebida es
  simplemente el primero.

### Verificación

- La biblioteca recompila **1095/1097**; los dos que fallan son los de siempre.
- Punto fijo: **1575 idénticos, 0 distintos**.
- **1441 tests de Rust pasan**; los 20 que fallan son los mismos, comparados nombre por nombre.
- De las **53** pruebas comparables, **51** dan el mismo entero que `java` real; las dos que no son
  `JcIc` y `WdWide`. La nueva es `RndSeedTest`.

## Los dos findings nuevos, arreglados

### #315 — el literal hexadecimal de coma flotante (§3.10.2)

`0x1.0p64` no se parseaba: *"se esperaba Semi, se encontró DoubleLiteral"*. Dos mitades: el **lexer**
no aceptaba la forma, y el **parser** no sabía convertirla (el `parse::<f64>()` de Rust no entiende
hexadecimal, así que el valor se arma a mano).

Lo que explica toda la forma de la gramática es que el exponente en `p` es **obligatorio**: sin él,
`0x1.8` no se podría distinguir de un acceso a miembro (`0x1 . 8`), y el sufijo `d`/`f` chocaría con
un dígito hexadecimal. Por eso no existe el literal hexadecimal sin exponente — y por eso el arreglo
sólo consume el `.` cuando después hay una `p`: un `0x1.foo()` sigue siendo una llamada.

La `p` cuenta potencias de **dos**, no de dieciséis. Es la única notación que escribe un `double` sin
redondeo, y por eso el JDK la usa para las constantes que tienen que ser exactas — las distancias de
salto de los generadores xoshiro, sin ir más lejos.

`HexFloat` da **-1** con las dos VMs: 20 comprobaciones, incluidas `Double.MAX_VALUE`
(`0x1.fffffffffffffP1023`), el subnormal más chico (`0x1.0p-1074`), separadores, y que un hexadecimal
entero **siga siendo** entero.

### #316 — el chequeo de excepciones dependía del orden de carga

Y este es el importante, porque el modo de fallar era un **falso negativo**: un programa con una
excepción comprobada sin manejar **compilaba**, y la excepción aparecía en tiempo de ejecución.

`is_checked` es indulgente a propósito cuando no puede resolver la jerarquía de una excepción —mejor
no inventar un error que forzar uno dudoso—. El problema es que la excepción declarada en el `throws`
de un método **del classpath** no se cargaba nunca: la fase 2 de `load_externals` (finding #251)
junta los tipos que aparecen en parámetros y retornos, y se olvidaba de los `throws`. Así que la
indulgencia se aplicaba siempre y `sync.wait()` sin manejar `InterruptedException` compilaba.

Lo que lo volvía desconcertante es que **dependía del resto del round**: si otra unidad nombraba
`InterruptedException` en un `throws` suyo, el tipo se cargaba por ese camino y el error aparecía.
`V1.java` sola compilaba; `V1.java` junto a un archivo que mencionara la excepción, no. Repro en
`scratchpad/zz316/`.

**Y otra vez el patrón: completar el chequeo rompió a los que se apoyaban en que estuviera
incompleto.** Catorce archivos, todos de espera:

- **`java.util.concurrent`**: `ArrayBlockingQueue`, `LinkedBlockingQueue`, `PriorityBlockingQueue`,
  `SynchronousQueue`, `DelayQueue`, `CountDownLatch`, `CyclicBarrier`, `Exchanger`, `FutureTask`,
  `Semaphore`, `ThreadPoolExecutor`, `Phaser`, `ExecutorCompletionService`.
- **`javax.tools`**: `ForwardingFileObject`, que reenviaba cinco métodos sin declarar la `IOException`
  que su propia interfaz declara.

El arreglo no fue taparlas sino **declararlas donde el JDK las declara**, lo que de paso hace más
fieles a tres interfaces que las omitían: `BlockingQueue.take()/put()/offer()/poll()`,
`Future.get()`, `ExecutorService.awaitTermination()` y `CompletionService.take()/poll()`. La nota de
esta última decía literalmente *"the JDK's `throws InterruptedException` on take/poll is omitted
package-wide"* — esa omisión era el error.

Las que el JDK **no** declara —`Phaser.arriveAndAwaitAdvance()`, `awaitAdvance(int)`,
`Semaphore.acquireUninterruptibly()`, el bucle del pool, `ExecutorService.close()`, `Future.state()` y
`resultNow()`— atrapan y **remarcan** el hilo al salir, que es lo que distingue "no me interrumpe" de
"me trago la interrupción".

De paso cayó otra nota falsa: `Semaphore.acquireUninterruptibly` decía *"KajiJDK has no interruption,
so this is exactly acquire()"*. La tiene, y la diferencia entre los dos métodos es real.

### Finding #316 — un `long...` que recibe `int` no ampliaba

`suma(3, 4, 5)` contra un `long...` guardaba tres `int` **crudos** en un `long[]`, y la VM cortaba con
*"expected a long, found Int(3)"*. Es el primo del #310 un escalón más abajo: aquél arregló
primitivo→referencia (el boxing que faltaba dentro de un `Object[]`), éste es primitivo→primitivo más
ancho.

El arreglo va **sólo en el camino de varargs**, y esa precisión importa: con un parámetro **fijo** la
conversión ya la emite el generador contra el descriptor y funciona. Lo que no tiene descriptor contra
el cual convertir es el `astore` del arreglo de varargs — ahí el valor entra tal como está.

Sólo se convierte cuando la **representación** cambia (`long`/`float`/`double`). Dentro de la familia
del `int` el valor ya está en la pila como `int` y un cast sería ruido.

Repro: `scratchpad/zz316/VarargsLong.java`. Salió escribiendo `SequenceLayout.reshape(3, -1)`.

### Finding #317 — el `<clinit>` de un `enum` corría al revés

Las constantes de un `enum` se construían **después** del inicializador estático del usuario. JLS
§12.4.2 pide lo contrario, y la consecuencia es de lo peor que puede pasar: **nada falla**. La clase
carga, y un `static final X ALIAS = A;` queda en `null` porque `A` todavía no existía cuando se leyó.

La causa es de orden de pasadas: `hoist_initializers` corre al entrar a la clase y deja el
`StaticInit` del usuario en `members`; la síntesis del `enum` corre después y **agregaba** el suyo
detrás. El emisor concatena en orden de miembro.

Ahora el bloque sintetizado se fusiona **por delante** del que haya. Lo encontró un agente escribiendo
`AclEntryPermission`, donde tres constantes son alias de otras tres.

Repro: `scratchpad/zz317/EnumAlias.java`.

### Finding #318 — un `super.m()` heredado corría contra el pool de constantes equivocado

`resolve_method` busca el cuerpo en la clase **declarante** (eso ya estaba bien, JVMS §5.4.3.3) pero
guardaba el `MethodBody` con el nombre de la clase **del sitio de llamada**. Ese campo es de donde sale
el pool de constantes con el que el cuerpo se ejecuta.

Concretamente: una `class B extends Exception` con `super.getMessage()` emite
`invokespecial java/lang/Exception.getMessage` —que es lo que emite el `javac` real, verificado con
`javap`—, el cuerpo se lee de `Throwable`, y después se corría contra el pool de `Exception`, donde el
`getfield detailMessage` no existe. Reventaba con *"getfield: bad FieldRef"*, tres capas lejos de la
causa.

Sólo aparece cuando la superclase **nombrada hereda** el método en vez de declararlo, que es
justamente lo común en una jerarquía de excepciones de dos niveles — y por eso no había salido antes:
`A extends Throwable` anda, `B extends Exception` no.

De paso se corrigió la clave del caché: ahora se guarda también por la clase declarante, para que un
mismo método alcanzado por dos nombres dé **el mismo** `MethodId`. Dos ids para un solo método se
separan en la vtable, y un override reemplazaría una de las dos ranuras.

Repro: `scratchpad/zz318/SuperHeredado.java`. Lo encontró un agente escribiendo
`InvalidPathException.getReason()`.

### Lo que el #316 rompió dentro de los tests de Rust

Tres pruebas empezaron a fallar, y las tres por lo mismo: **fixtures de Java escritas dentro del
código Rust** que se tragaban una `IOException` de `Filer.createSourceFile`/`FileObject.openWriter`.
Compilaban por el mismo motivo que la biblioteca.

- `javac::codegen::tests::the_filer_hands_generated_source_back_to_rust` — el driver es un método
  estático suelto, así que declara el `throws`.
- `jvm::interpreter::apt::tests::a_processor_generates_a_source_that_is_recompiled_and_runs` — acá
  **no** se puede declarar: la firma de `Processor.process` no lo tiene y un override no puede
  ensanchar las chequeadas. Se atrapa, que es lo que hace cualquier procesador real.

La tercera, `gc::tests::method_handle_constructor`, no era del #316: se rompió porque re-emití
`java/MHCtor.class`, y esa prueba arranca desde `boot/` —un snapshot viejo de la biblioteca donde
`MethodHandles.lookup()` funciona— y no desde `KajiLibrary`, donde `lookup()` tira. Restaurar el
`.class` commiteado la devolvió a verde. **Vale como aviso**: los `.class` de `java/` que arrancan
desde `boot/` no se pueden recompilar con el javac actual sin romper su prueba.

## Tanda: de más completo a menos, segunda parte

**Cerrados**: `java.util.zip` 240/240, `java.net`→`URI` 35/35, `javax.lang.model` 41/41,
`java.util.concurrent.locks` 59/59, `jdk.internal.vm`→`ContinuationScope` 6/6.

### `ZipFile` dejó de tirar

Su comentario decía: *"cuando lleguen los nativos de archivo, esto se vuelve un salto más la misma
decodificación de campos"*. Llegaron, y eso es lo que hace ahora — lee el EOCD desde el final, decodifica
el directorio central, y salta a la cabecera **local** de cada entrada (los largos de nombre y extra
pueden diferir entre las dos, y el que vale para encontrar los datos es el de la local).

Se lee el archivo entero de una: no hay descriptor abierto ni posición. Es la misma decisión que
`FileInputStream`, con la misma contrapartida — un ZIP de un giga entra en memoria, y a cambio no hay
estado que se pueda quedar colgado.

**Y la prueba destapó dos cosas que valen más que el número:**

- **`ZipInputStream` sólo leía la primera entrada.** El escritor usa descriptor de datos (bit 3) y el
  lector no lo salteaba: al buscar la cabecera siguiente encontraba la firma del descriptor y se
  rendía. Peor: para una entrada comprimida el inflater **lee de más**, así que los bytes sobrantes
  ya habían salido del flujo. `Inflater.getRemaining()` dice cuántos son — la nota de la clase decía
  *"el inflater puede reportarlo pero la plomería de acá todavía no lo usa"*, y ésta es esa plomería.
- **Nuestro `Deflater` no comprime**: emite sólo bloques `STORED`, lo cual está documentado con su
  motivo. La primera versión de `ZipTest` afirmaba que el tamaño comprimido era menor que el
  original y fallaba — la aserción estaba mal, no la biblioteca. Se cambió por lo que sí es contrato:
  los tamaños que el directorio guarda son los que hay, y el contenido vuelve exacto.

`ZipTest` da **-1** con las dos VMs: 27 comprobaciones que escriben un ZIP real, lo leen por acceso
aleatorio **pidiendo la última entrada primero**, y lo vuelven a leer secuencialmente.

### El resto

- **`java.net.URI` 35/35**: los cuatro constructores por partes —que arman la cadena y la
  **reparsean**, para que un URI armado y uno parseado con el mismo texto sean indistinguibles— y el
  álgebra de caminos. `normalize` es la única con trampa: sacar un `..` **no** es borrar el segmento
  anterior si ese segmento era a su vez un `..`; `a/../../b` es `../b`.
- **`URL`**: `openStream()` (sólo `file:`, vía los nativos), `URL(URL, String)` que delega en
  `URI.resolve` para que las dos clases no discrepen, `getDefaultPort` y `sameFile` —que ignora el
  fragmento, y eso es todo su punto: `pagina#a` y `pagina#b` son un solo documento—.
- **`SourceVersion` 37/37**: las constantes hasta `RELEASE_25` y las tres preguntas sobre una cadena,
  que son tres y distintas: `isIdentifier` mira la forma léxica (`class` **es** un identificador
  bien formado), `isKeyword` si está reservada, `isName` si se puede usar como nombre — y ésta acepta
  puntos, porque `java.util.List` es un nombre válido.

### Lo que sigue bloqueado, y por qué

- **`javax.sql.DataSource` 0/7**: necesita `java.sql.Connection` y `ConnectionBuilder`, o sea empezar
  `java.sql` entero.
- **`jdk.internal.vm.Continuation` 3/20**: continuaciones de Loom, que son soporte de VM.
- **`URL`: `openConnection`, `getContent`, los constructores con handler** — necesitan
  `URLConnection`/`URLStreamHandler`, que no existen.
- **`java.io`: los dos `getChannel()`** — necesitan `FileChannel`, y nuestra E/S es de archivo
  entero, sin posición: un `FileChannel` con `position()` sería un miembro que miente.

### Finding #319 — la resolución de sobrecargas elegía la última declarada, no la más específica

El repro es concluyente porque es **el mismo código dos veces**, con la única diferencia del orden en
que se declaran las dos sobrecargas:

```
java/OvLib3.java + java/OvUse3.java    of(T...) antes que of(T)   ->  error: tipo incompatible en `l`
java/OvLib5.java + java/OvUse5.java    of(T)    antes que of(T...) ->  compila
```

Con `of(T...)` primero, la llamada `of(unArregloDeString)` liga a `of(T)` con `T = String[]` en vez
de a la variarg. El JLS §15.12.2 fija lo contrario —gana la más específica, y el orden de declaración
no participa— así que esto no es una preferencia distinta sino una regla que falta.

**Qué rompe hoy**: es lo que había bloqueado `Stream.of(T)` en una pasada anterior. El rodeo es
escribir la no-variarg primero, que es lo que `Stream.java` hace ahora, con la razón anotada en la
declaración y fijada por la prueba. Un rodeo que depende del orden del archivo es frágil por
definición: cualquiera que reordene por prolijidad lo rompe sin enterarse.

### Finding #320 — una llamada no resuelve si un parámetro mete una variable de tipo dentro de un argumento de tipo invariante

`java/WcLib3.java` + `java/WcUse3.java` prueban las cinco variantes, y lo que separa las que andan de
las que no es **el comodín en esa posición**:

```
Supplier<Down3<T>>                 FALLA
Supplier<? extends Down3<T>>       ok
BiConsumer<Object, Down3<R>>       FALLA    <- una sola variable, igual falla
BiConsumer<A, Down3<R>>            FALLA
BiConsumer<A, ? extends Down3<R>>  ok
```

No es la cantidad de variables, ni que el tipo anidado sea de primer nivel o miembro, ni la falta de
un witness explícito: es que el argumento de tipo sea invariante. El error es duro
(`no resolvió a ningún método`), no una inferencia silenciosamente mala.

**A quién le pega**: las cuatro `Gatherer.of`/`ofSequential` con finalizador, que reciben
`BiConsumer<A, Downstream<? super R>>` —el `? super` está *adentro* de `Downstream`, no en la
posición anidada, así que no salva—. Están declaradas y sus cuerpos son correctos; hoy no se pueden
llamar. `Gatherers` arma sus implementaciones con el constructor para no depender de ellas.

### Finding #321 — `null` en posición de una variable de tipo no infiere

```java
static <T> Caja<T> f(Runnable r, T t) { ... }
Caja<String> c = f(r, null);
```

da `no se pueden inferir los argumentos de tipo de f: restricciones de tipo incompatibles`. El javac
real lo acepta: `null` tiene el tipo nulo, que es subtipo de todo, así que no restringe nada y `T`
sale del destino. Acá parece tratarse como una restricción imposible en vez de como ninguna
restricción. Repro en `scratchpad/zz321/Nl.java`, con el javac real al lado dando cero errores.

### Lo que NO resultó ser un bug

Al arreglar `PriorityBlockingQueue` vi un `.class` sin el atributo `Exceptions` de un método cuya
fuente sí declaraba `throws`, y estuve por anotarlo como hallazgo. **Dos repros mínimos —uno suelto,
otro redefiniendo un método de interfaz genérica— emiten el atributo correctamente con nuestro javac
y con el real.** Era un `.class` viejo en el árbol, no el compilador. Queda escrito acá por la misma
razón que el #314: el repro que no se construye antes de escribir el arreglo hace perder más tiempo
que el que ahorra.

### Finding #322 — una referencia a método bajo un cast explícito no se emite

`scratchpad/zz322/Cast.java` lo aísla en dos líneas seguidas, con la misma referencia y la misma
interfaz funcional:

```java
Function<Integer, String> sinCast = Ayuda2::dos;                              // compila
Function<Integer, String> conCast = (Function<Integer, String>) Ayuda2::dos;  // error
```

La segunda da `el generador de bytecode todavía no soporta una referencia a método (necesita
invokedynamic)`. Por asignación la referencia se resuelve; con el cast escrito delante, no. El javac
real acepta las dos.

**No es lo que se había reportado.** El informe del agente de `java.time.format` lo describía como
"una referencia a método en posición de variarg", y la posición de variarg no tiene nada que ver: la
llamada variarg fallaba porque el argumento llevaba un cast, y el mismo cast falla igual en una
asignación común. La otra mitad del informe —que `Tipo::metodo` no resuelve cuando `Tipo` no aparece
como tipo en ningún otro lado del archivo— **no se reproduce**: `scratchpad/zz322/Mr2.java` tiene
exactamente esa forma y compila.

**A quién le pega**: el cast sobre una referencia a método es la manera de desambiguar cuando el
destino es `Object` —que es justo el caso de una llamada variarg—, así que el rodeo es atar la
referencia a una variable local con el tipo escrito y pasar la variable.

### Finding #323 — la instrucción de invocación se elegía por la clase declarante, no por el owner escrito

**Éste es el más grave de la tanda, porque no lo detectaba nada nuestro.** Los `.class` que emitíamos
en dos formas muy comunes eran inválidos, y nuestra VM los corría igual.

El repro son dos líneas, en `scratchpad/zz323/Kind2.java`:

```java
interface Base2 { default int val() { return 1; } }
final class Impl2 implements Base2 { }

Impl2 i = new Impl2();
Base2 b = i;
int x = i.val();          // declarado en una INTERFAZ, receptor de tipo CLASE
boolean y = b.equals(i);  // declarado en Object (CLASE), receptor de tipo INTERFAZ
```

| | nuestro javac (antes) | javac real |
|---|---|---|
| `i.val()` | `invokeinterface Impl2.val` | `invokevirtual Impl2.val` |
| `b.equals(i)` | `invokevirtual Base2.equals` | `invokeinterface Base2.equals` |

En los dos casos la **etiqueta** de la entrada del pool (`Methodref` vs `InterfaceMethodref`) no
concordaba con lo que la clase nombrada realmente es. §5.4.3.3 y §5.4.3.4 exigen que concuerden. La
JVM real se niega a resolverlas:

```
Exception in thread "main" java.lang.IncompatibleClassChangeError: Found class Impl2, but interface was expected
```

**Cómo apareció.** No lo encontró ninguna prueba nuestra: apareció al intentar correr `ChronoTest` y
`FmtTest` —compilados por nosotros— con `java` de verdad, para comparar resultados entre las dos VMs.
Los dos fallaron al cargar, uno por cada dirección del error (`MinguoDate` es una clase con métodos
`default` heredados; `Chronology` es una interfaz a la que se le llama `equals`).

**La causa.** `codegen.rs` decidía la instrucción con `owner_is_interface`, que pregunta por la clase
que **declara** el método, pero escribía como owner el **tipo estático del receptor** (que es lo
correcto, §5.4.3.3). Cuando los dos difieren en clase/interfaz —un `default` heredado por una clase,
o un método de `Object` sobre una interfaz— la etiqueta y el owner se contradicen. Ahora la pregunta
se hace sobre el owner que se va a escribir (`ref_owner_is_interface`), que es la única que importa.

**Por qué no se notó antes.** Nuestra VM resuelve por nombre y descriptor y no compara la etiqueta
del pool contra lo que la clase es. Es una permisividad, no una decisión, y su costo es exactamente
éste: un `.class` que corre acá y ninguna otra JVM acepta. La medición por miembros tampoco lo veía
—las firmas estaban todas bien— y las pruebas de comportamiento tampoco, porque corrían en la VM
permisiva. **La prueba que lo encontró es correr nuestros `.class` en la JVM de verdad**, y conviene
que eso sea parte de la batería y no algo que se hace de casualidad.

### Finding #324 — una variable local declarada dos veces se acepta

```java
int x = 1;
int x = 2;   // el javac real: "variable x is already defined in method f()"
```

Nuestro javac emite el `.class` sin decir nada. Repro en `scratchpad/zz324/Dup.java`.

**No es teórico: ya había una en el árbol.** `Double.java` declaraba `int maxDigits = 17` dos veces
seguidas en `decimalString`, con el mismo `if (asFloat) maxDigits = 9` repetido debajo. Inofensivo
—la segunda pisa a la primera con el mismo valor— pero es exactamente el residuo de una edición a
medio terminar que este chequeo existe para atrapar. Queda una sola.

### Lo que ParseTest destapó: `Double.toString` crece de forma cuadrática

`ParseTest` no termina: pasa de 900 s sin llegar al primer `println`. **No está colgado, está a paso
de tortuga**, y la causa está acotada:

| llamadas a `Double.toString(3.0)` | tiempo |
|---|---|
| 50 | 1 s |
| 100 | 5 s |
| 150 | 8 s |
| 200 | 19 s |
| 250 | 28 s |

Siempre **el mismo valor**, así que no es el dato: es la cantidad. El costo por llamada crece con
cuántas van, que es lo que hace el total cuadrático.

Lo que el repro (`scratchpad/zz324/`) ya descarta:

- **No es `parseDouble`**: mil llamadas terminan al instante (`Vol2.p1000`).
- **No es la presión de asignación en general**: mil `StringBuilder`+`String` y mil `char[24]`
  terminan al instante (`Aloc`). Si fuera el GC ahogándose con basura joven, éstos también irían mal.
- **Es específico de `Double.toString`**, que hace una búsqueda binaria sobre la cantidad de dígitos
  y en cada paso **parsea de nuevo** con `BigInteger`. Cinco parseos por llamada es caro pero
  constante; que además *crezca* apunta a que algo se acumula entre llamadas.

Queda como hallazgo con repro y medición, no arreglado: pinchar dónde se acumula es entrar en la
política de promoción del GC y en `BigInteger`, y es otra tarea. Mientras tanto `ParseTest` conviene
correrlo por método (`basicos`, `dificiles`, `hexadecimal`, `flotantes`, `malos` dan **0** en
segundos); los dos que barren de a miles —`ida_y_vuelta` y `dobleRedondeo`— son los que no terminan.

### Sobre el bloqueo que `java/text/FieldPosition.java` documenta: la causa que dice NO es la causa

El encabezado de `FieldPosition.java` explica que sus dos constructores con `Format.Field` y
`getFieldAttribute()` quedan afuera porque declararlos **pone `java/text/Format$Field` en el pool de
constantes de `FieldPosition.class`**, y que con eso ninguna unidad que declare una subclase de
`Format.Field` vuelve a compilar. (Cita un "#319" que hoy es otro hallazgo: la numeración se cruzó.)

**Reproducido y ablacionado en `scratchpad/zz325/`**, con la forma exacta —`Fmt` abstracta con una
anidada `Field`, `Pos` que la nombra, `Num extends Fmt` con `Num.Field extends Fmt.Field`—. El
síntoma es real: `super(nombre)` da *«el generador de bytecode todavía no soporta un `super(...)` que
no resolvió a ningún constructor»*.

**Pero la causa no es la que dice.** Con `Pos` reescrita **sin nombrar `Fmt.Field`** —única
diferencia— el error es idéntico. El pool de constantes de `Pos` no participa.

Y la forma sola tampoco alcanza: `scratchpad/zz326/` prueba las cuatro combinaciones (anidada que
extiende una anidada de otra clase, de primer nivel que extiende una anidada, con constructor
`protected` y con `public`) y **las cuatro compilan**. Falta algo más del caso real para cerrarlo —
lo más probable es la circularidad (`Fmt` nombra `Pos` y `Pos` nombra `Fmt`) sumada al anidamiento.

Queda escrito con el repro porque un bloqueo documentado con la causa equivocada es peor que uno sin
explicar: manda a arreglar lo que no está roto. Los tres miembros siguen afuera, pero por una razón
que todavía no está identificada.

## Tanda: `javax.tools`, `javax.sql` y el `java.sql` que hacía falta debajo

Cerrados `javax.tools` (149/149), `javax.transaction.xa` (55/55) y `javax.sql` (19/19 clases,
198/198 miembros), con el núcleo de `java.sql` (55/56 clases públicas, 1243/1245) y el de
`java.util.logging` que `getParentLogger()` necesitaba.

Los dos hallazgos salieron de **medir** lo emitido y no de que algo fallara: en los dos casos el
compilador aceptaba el archivo y escribía un `.class` distinto de lo escrito.

### Finding #325 — un externo nombrado entero resolvía por nombre simple, y ganaba el homónimo cargado antes

`resolve_type_id` (`codegen.rs`) indexaba los externos **solo por nombre simple**. Un
`java.sql.Date` escrito entero no lo encontraban ni `external(name)` —la clave es `Date`— ni
`class(name)`, que solo mira el fuente; caía entonces en el `external(simple)` del final, pensado
para el anidado escrito corto (`Map.Entry` → `external("Entry")`), que **tira el calificador**.

Con lo cual, si otro `Date` cualquiera ya estaba cargado, ganaba ese.

En compilación de a un archivo no pasa nunca. En **multi-unidad** pasa solo: alcanza con que una
unidad anterior del lote mencione un `java.util.Calendar`, cuyos miembros arrastran `java.util.Date`.
Emitir `DatabaseMetaData.java` y `CallableStatement.java` juntos hacía que el
`java.sql.Date getDate(int)` del segundo saliera con `java.util.Date` en el descriptor.

Lo que lo escondía: **la pasada 1 sí consulta el índice por nombre completo** (`enter.rs`,
`resolve_name_to_sym`, que hace `class(dotted) → external_fqn(dotted) → external(simple)`), así que el
chequeo daba bien y el desacuerdo aparecía recién en el `.class`. Y la biblioteca recompila de a un
archivo, donde no hay unidad anterior que cargue al homónimo. Salió al medir `java.sql` y ver seis
miembros faltando en `CallableStatement` que estaban escritos ahí mismo.

**Arreglo**: consultar `external_fqn(name)` antes del `external(simple)`, en el mismo orden que la
pasada 1 —exacto primero, simple como red—. Lo simple sigue haciendo falta para `Map.Entry`, donde el
nombre completo del externo es `java.util.Map$Entry` y no coincide con lo escrito.

Repro en `scratchpad/zz325_fqn/`: `p.Date` (una clase), `p.Uno` (nombra `java.util.Calendar`) y
`p.Dos` (devuelve `p.Date`). `javac --emit p/Uno.java p/Dos.java` emitía `java.util.Date cual()`;
`javac --emit p/Dos.java` solo, `p.Date cual()`. Un artefacto que compila, carga, y miente.

Y va **en las dos direcciones**, cosa que apareció sola al comparar el punto fijo. `java/sql/Timestamp.java`
declara `extends java.util.Date` y `compareTo(java.util.Date)`, las dos escritas enteras. El javac
congelado emitía `public int compareTo(java.sql.Date)`: al compilar un archivo **del paquete
`java.sql`**, el `Date` que estaba indexado por nombre simple era el del propio paquete, y ganaba ese.
El mismo defecto que arruinaba `getDate` en `CallableStatement`, con el calificador tirado hacia el
otro lado.

### Finding #326 — los argumentos de una constante de `enum` no cargaban sus tipos

`collect_type_names` (`enter.rs`) recorre supertipos, cotas, componentes y miembros — y **no**
`class.enum_constants`. Los argumentos de una constante de `enum` son las únicas expresiones de una
declaración que no viven ni en un campo ni en un método, así que un tipo que solo aparece ahí no se
cargaba del classpath.

El delator es el mismo que en #325 y al revés: **el chequeo pasaba y la emisión fallaba**. La pasada 1
llega a ese nombre por otro camino, así que el desacuerdo salía como
*«no se encuentra el símbolo: variable Types»* sobre un tipo escrito ahí mismo — el "variable" es el
resolvedor de expresiones tomando el calificador como receptor después de que el tipo no resolvió.

`java.sql.JDBCType` es el caso natural: nombra a `java.sql.Types` **solo** dentro de los paréntesis de
sus treinta y nueve constantes, y en ningún cuerpo ni firma.

**Arreglo**: recorrer `enum_constants` y pasarle cada argumento a `collect_from_expr`.
`collect_annotation_names` ya recorría `enum_constants` para las anotaciones, lo cual deja ver que la
omisión fue del recorrido de tipos y no de la representación.

Repro en `scratchpad/zz326_enum/`: `q.K` con una constante `int`, y un `enum` que la nombra **solo**
en el argumento de sus constantes. Basta con que el mismo `enum` use `K` una vez en un cuerpo para
que el defecto desaparezca, que es lo que lo hacía parecer caprichoso.

### Cómo quedaron

- **`javax.sql`** 19/19 clases, **198/198** miembros.
- **`javax.tools`** 17/17, **149/149**. **`javax.transaction.xa`** 3/3, **55/55**.
- **`java.sql`** 55/55 clases públicas, **1245/1245**. `Connection`, `ResultSet`, `Statement`,
  `PreparedStatement`, `CallableStatement` y `DatabaseMetaData` completos; `SQLXML` cerró al traer
  `javax.xml.transform.Source` y `Result`, que son dos interfaces puras.
- **`java.util.logging`**: se trajo el núcleo —`Level`, `LogRecord`, `Filter`, `Formatter`,
  `SimpleFormatter`, `ErrorManager`, `Handler`, `StreamHandler`, `ConsoleHandler`, `LogManager`,
  `Logger`— porque era el único bloqueo de `CommonDataSource.getParentLogger()` y
  `Driver.getParentLogger()`. Funciona de verdad: los niveles filtran, los mensajes suben por el
  árbol de nombres y `LogManager` recuelga a los descendientes cuando aparece un intermedio. Faltan
  `FileHandler`, `SocketHandler`, `MemoryHandler` y `XMLFormatter`, que no los pide nadie todavía.
- **`java.sql.DriverInfo`** no es pública, así que no cuenta. Su papel —el par (driver, acción de
  baja)— lo hace una clase privada dentro de `DriverManager`.

### Y un arreglo en la medición

El censo contaba **todas** las clases que el jimage lista, incluidos los internos del JDK
(`StringLatin1`, `CharacterData00`, `TimSort`, `ImmutableCollections`). Eso hacía que `java.lang`
leyera 110/145 cuando lo que de verdad le falta son tres clases, y que `java.util` pareciera 113/131
estando cerrado en 109/109.

Además, el denominador de miembros salía **solo de las clases que tenemos**, que es el mismo punto
ciego un nivel más abajo: `java.net` leía 93/102 teniendo 7 de sus 64 clases. Ahora una clase ausente
entra con cero en el numerador y con todos sus miembros en el denominador, y `java.net` lee 93/757 —
que es lo que es.

`tools/apidiff/censo.py` y `tools/apidiff/grafico.py` quedaron en el repo. De paso el censo pasó de
correr un `javap` por clase a uno por paquete: de unos doce minutos a treinta segundos.

`java/SqlTest.java` (26 comprobaciones: fecha y hora SQL, el registro de drivers, la herencia de
niveles por el árbol de nombres) da `-1` en las dos VMs.

### Finding #325 — un cast a primitivo desde una referencia no emitía nada

```java
Object o = Integer.valueOf(42);
int v = (int) o;
```

emitía `aload_0; istore_1`: la referencia guardada tal cual en un local `int`, **sin `checkcast` y sin
`intValue()`**, y sin un solo diagnóstico. Nuestra VM entraba en panic
(`expected an int on the operand stack, found Reference`) y la JVM real rechazaba la clase al
verificarla (`VerifyError: Type 'java/lang/Integer' is not assignable to integer`).

**No es un caso raro**: `(int) mapa.get(k)` es la forma normal de sacar un primitivo de una colección
heterogénea, y hoy salía mal en todo el árbol.

Lo reportó el agente de `java.lang.foreign` con la ablación ya hecha, y la ablación es la parte que
importa: el disparador es **el cast explícito**, no el desempaquetado. `Integer o; int v = o;` --que
es conversión de asignación, §5.2-- siempre estuvo bien, y `((Integer) o).intValue()` también. Sólo
fallaba el cast.

**La causa**: `transtypes.rs` visitaba el operando del `Cast` y no coercionaba nada. El desempaquetado
lo inserta esa pasada (el codegen sólo baja lo que le llega), así que un cast a primitivo desde una
referencia se quedaba sin conversión. Ahora `cast()` la aplica, con las **dos formas** que el javac
real distingue y que se midieron en `scratchpad/zz333/Casos.java`:

| operando | destino | qué emite |
|---|---|---|
| `Integer` | `int` | `intValue()` — sin `checkcast` |
| `Integer` | `long` | `intValue(); i2l` |
| `Object` / `Number` | `int` | `checkcast Integer; intValue()` |
| `Object` | `long` | `checkcast **Long**; longValue()` |

La cuarta fila es la que no se adivina: el `checkcast` va al wrapper **del destino**, no al del
operando, que no se conoce. `(long) unObject` que en realidad tiene un `Integer` **tiene** que tirar
`ClassCastException`, y ahora la tira en las dos VMs.

Prueba: `java/CastUnboxTest.java`, **-1** con las dos, y el bytecode de los ocho casos es idéntico
instrucción por instrucción al del `javac` real.

### Finding #326 — el compilador se colgaba con una variable llamada `con`

Tres líneas:

```java
String con = "x";
return con.length();
```

`javac` se quedaba **colgado para siempre**, sin mensaje y sin consumir CPU.

Al resolver `con.length()` el compilador considera que `con` podría ser un tipo y lo busca en el
classpath: `std::fs::read("KajiLibrary/con.class")`. En Windows eso **no falla**: `CON` es un nombre
de dispositivo reservado, se resuelve con cualquier extensión y sin distinguir mayúsculas, y abrirlo
abre la **consola**. La lectura se queda esperando entrada que nunca llega.

Confirmado con la familia entera, y el patrón cierra: cuelgan los de **entrada** y no los de salida.

| `con` | `aux` | `com1` | `prn` | `nul` | `lpt1` |
|---|---|---|---|---|---|
| cuelga | cuelga | cuelga | ok | ok | ok |

`CON` es la consola y `AUX`/`COM1` son puertos serie; `NUL` da EOF al instante y `PRN`/`LPT*` son de
salida. Se filtran los veintidós igual: ninguno puede contener un `.class`.

**Para este proyecto en particular no es una curiosidad**: `con` es de las palabras más comunes al
escribir código en castellano, y bastaba una variable local con ese nombre --en posición de
receptor-- para colgar la compilación entera. Apareció así, en una prueba recién escrita.

Arreglado en `enter.rs`, en `ClassFinder::find`, y sólo bajo `cfg(windows)`: en cualquier otro sistema
`Con.class` es un archivo común y saltearlo escondería una clase que existe. Repro de tres líneas en
`scratchpad/zz332/Recep.java`.

### Finding #327 — se pierde `@Retention(RUNTIME)`, y con eso todas las anotaciones en una JVM real

> **Corrección (misma sesión).** La primera versión de este hallazgo decía que hacían falta **dos**
> cosas: que el tipo anotación viniera del classpath **y** que tuviera un miembro de tipo enum. La
> segunda mitad es falsa. Mi ablación movió dos variables a la vez —comparé *mismo archivo + enum*
> (anda) contra *classpath + enum* (falla)— y le atribuí la causa a la que no era. El caso que
> faltaba, **classpath + miembro `String`**, también falla: `scratchpad/zz340/`. Lo encontró el
> agente de `javax.annotation.processing`, con la ablación completa.
>
> Queda escrito acá y no borrado porque es el mismo error que este archivo le reprocha al #314 y al
> comentario de `FieldPosition`, cometido por quien escribió esos reproches.

Una anotación cuyo **tipo se resuelve desde el classpath** (un `.class`, no la misma unidad de
compilación) no emite su atributo `RuntimeVisibleAnnotations`, **sea cual sea el tipo de sus
miembros**. La misma anotación declarada en el mismo archivo fuente sí lo
emite. Ablación en `scratchpad/zz336/` y `scratchpad/zz337/`:

| forma | atributo emitido |
|---|---|
| `@Deprecated` sobre una clase o sobre un `@interface` | sí |
| anotación **del mismo archivo**, miembro `String`, `Class` o enum | sí |
| anotación **del classpath**, miembro `String` | **no** |
| anotación **del classpath**, miembro enum | **no** |
| `@Retention(RetentionPolicy.RUNTIME)` | **no** |

Lo que separa las filas es de dónde sale el tipo de la anotación, y nada más. `@Deprecated` se salva
porque no tiene miembros que resolver.

La última fila es la que hace grave al resto: `java.lang.annotation.Retention` **es** exactamente ese
caso —del classpath, con miembros—, así que **ningún tipo anotación que compilemos conserva su
retención**. Y el efecto se propaga: al compilar un `@interface` propio se lee `@Retention` del
classpath y se pierde, con lo cual esa anotación tampoco es visible en tiempo de ejecución.

Prueba directa y barata de que el atributo ni se intenta: agregarle `@Documented` y `@Target` al
fuente de un `@interface` de la biblioteca deja el `.class` **byte-idéntico**.

**La consecuencia se ve sólo cruzando a la JVM real**, que es la red nueva de esta tanda:

```
scratchpad/zz335/M2.java   ->  nuestra VM: 1 anotacion    java real: 0 anotaciones
```

Nuestra VM devuelve 1 porque no mira la retención; la JVM real trata la anotación como
`RetentionPolicy.CLASS` --el valor por omisión, que es lo que queda cuando el atributo no está-- y no
la expone en tiempo de ejecución. O sea que **nuestros `.class` con anotaciones no son portables**:
todo lo que dependa de leerlas por reflexión ve un programa sin anotaciones.

Es la misma familia que el #323: no hay error de compilación, nuestra VM lo acepta, y el problema
aparece recién cuando el resultado sale de casa.

### Y algo que topa con lo mismo desde el otro lado: la reflexión sobre una anotación voltea la VM

`scratchpad/zz335/Mi.java` invoca por reflexión los métodos del tipo anotación sobre una instancia
--que es como se leen los valores de una anotación-- y nuestra VM entra en panic:

```
panicked at src\jvm\interpreter\bytecode_interpreter.rs:2705: index out of bounds: the len is 0 but the index is 0
```

Ablado: `Method.invoke` con argumentos anda (`M1`, da 42), sin argumentos anda (`M3`, da 7), y
`getAnnotations()` responde (`M2`). Lo que rompe es invocar un método **sobre la instancia de la
anotación**. Los dos juntos son los que dejan afuera `VMSupport.encodeAnnotations`/`decodeAnnotations`,
documentado en el encabezado de esa clase.

### Finding #328 — un constructor varargs no empaquetaba sus argumentos

Ojo con el numero: el `#327` **ya estaba usado** (la perdida de `@Retention(RUNTIME)`, arriba en esta
misma tanda). Este es el `#328`.

`f(1, 2)` contra `void f(int... xs)` emite el `anewarray` y pasa el array. `new C(1, 2)` contra
`C(int... xs)` **no**: emitia los argumentos sueltos contra un descriptor `([I)V`.

Repro en `scratchpad/zz339/Var.java`, con el metodo al lado del constructor para que se vea que es lo
mismo escrito dos veces:

```java
public Var(String... xs)        { this.n = xs.length; }
public static int m(String... xs) { return xs.length; }
new Var("a").n + m("b", "c")
```

Lo que salia (`javap -c`), con el metodo bien y el constructor mal:

```
 0: new           #23   // class Var
 3: dup
 4: ldc           #46   // String a          <-- el String suelto...
 6: invokespecial #48   // Var."<init>":([Ljava/lang/String;)V   <-- ...contra un [String
 ...
19: iconst_2
20: anewarray     #47   // class java/lang/String                <-- el metodo si empaqueta
```

**Pasa `javap` y pasa el verificador nuestro**; revienta en tiempo de ejecucion, cuando el
`invokespecial` toma el `String` como si fuera el array. Es de la familia de #323/#327: el `.class`
se ve sano y el problema aparece al usarlo.

La causa es de una linea: `Desugar::lower_varargs` solo miraba `ExprKind::Call`. Un `new` es
`ExprKind::NewObject`, que nunca entraba — aunque su `binding` ya apunte al constructor resuelto y
lleve el `varargs: true`. Arreglado en `desugar.rs` haciendo que `lower_varargs` acepte las dos
formas, y llamandola desde la rama de `NewObject`.

**Donde se llama importa.** Va *despues* de `add_local_captures_arg`/`add_outer_arg`, no antes: esos
dos anteponen argumentos sinteticos (`this$0`, los `val$`) y el simbolo del constructor de una
interna ya lleva esos parametros en su firma resuelta. Empaquetar primero contaria los fijos contra
una lista de argumentos a la que todavia le faltan los sinteticos, y partiria en el lugar equivocado.

#### La mitad de la anonima, que fallaba de otra forma

`new Anon("x") { }` contra `Anon(String... xs)` no llegaba ni a emitir: era un **error de
compilacion**.

```
error: no se encontro un constructor `$1(String)` aplicable
  metodo $1.$1(String[]) no es aplicable
    (los argumentos no coinciden: String no se convierte a String[])
```

`hoist_anonymous` sintetiza en `$N` un constructor de reenvio con los parametros del ctor del
supertipo — pero los copiaba con `varargs: false` fijo, en el `Param` del AST y en el
`Resolved::Method`. Asi el reenvio dejaba de ser variadico y el `new $N("x")` que reemplaza al
`new Anon("x"){…}` no resolvia. Arreglado en `enter.rs`, heredando la marca del ctor del supertipo en
los dos lugares. Repro en `scratchpad/zz339/Anon.java`.

Las dos mitades se cruzaron contra la JVM real: `Var.run()` da 5 y `Anon.run()` da 1 en las dos VMs.

Regresiones en `desugar.rs`: `varargs_constructor_wraps_trailing_args_too`,
`varargs_constructor_with_fixed_params_wraps_only_the_tail` y
`passing_an_array_directly_to_a_varargs_constructor_is_not_rewrapped` — los espejos de los tres que
ya existian para `Call`.

**Como aparecio:** escribiendo `java.lang.ProcessBuilder`, cuyo constructor `ProcessBuilder(String...)`
es de los mas usados de la biblioteca estandar. Un `new ProcessBuilder("cmd")` volteaba la VM con
`range start index N out of range for slice of length 1024`.

### Finding #328 — un `goto` muerto después de un `throw` hacía que la JVM real rechazara la clase

```java
try {
    if (c) { throw new IllegalStateException(); } else { throw new RuntimeException(); }
} catch (RuntimeException e) { ... }
```

Nuestra VM lo corría; la JVM real se negaba a cargar la clase:

```
java.lang.VerifyError: Expecting a stack map frame
  Location: V3.run(I)I @14: goto
```

El emisor del `if`/`else` ponía siempre un `goto` para saltear el `else` **después** de la rama
`then`. Cuando el `then` termina en `throw` o en `return`, ese `goto` es código muerto — y §4.10.1
exige un frame del `StackMapTable` en toda instrucción que sigue a una transferencia incondicional.
No lo emitíamos, y no había forma sensata de emitirlo: el código es inalcanzable, no tiene tipos.

El arreglo es el que hace el javac real: **no emitir el salto** cuando la rama no puede llegar hasta
ahí. El compilador ya llevaba la bandera `reachable`; sólo faltaba consultarla.

Repro y ablación en `scratchpad/zz339/`: `V3` (if/else con dos `throw`) y `V5` (con `else if`)
fallaban; `V2` (un `throw` suelto) y `V4` (bucle con un `throw`) andaban. Ahora los cinco dan lo mismo
en las dos VMs.

**Cómo apareció, que es lo que vale la pena anotar**: escribiendo `java/ChanTest.java` para
`java.nio.channels`. La prueba tenía un `catch` que atrapaba tres excepciones lanzadas desde un
`if`/`else if`/`else` — código perfectamente normal— y no cargaba en la JVM real. Es el tercer
hallazgo de esta tanda (con el #323 y el #327) que **sólo se ve cruzando a la JVM de verdad**, y el
tercero que ninguna de nuestras redes internas podía ver.

### Finding #350 ⬜ — llamar a un método sobre el resultado de un genérico con comodín no emite

```java
import java.util.List;

public class Repro350 {
    static String directo(List<? extends CharSequence> l) {
        return l.get(0).toString();          // error
    }

    static String porLocal(List<? extends CharSequence> l) {
        CharSequence c = l.get(0);           // anda
        return c.toString();
    }
}
```

```
Repro350.java:5: error: el generador de bytecode todavía no soporta una llamada que no resolvió a ningún método
        return l.get(0).toString();
               ^
```

`List<? extends CharSequence>.get(int)` devuelve el **capture** de `? extends CharSequence`. La
resolución de métodos no le encuentra supertipo a ese tipo capturado, así que `.toString()` sobre él
no resuelve y el emisor se queda sin método. Asignarlo primero a una local del límite superior
(`CharSequence c = l.get(0)`) lo arregla, lo que apunta a que lo que falta es la **conversión de
captura al límite** en el receptor de la llamada, no la resolución en sí.

No es un caso raro: `List<? extends T>` es el tipo de retorno de
`java.security.cert.CertPath.getCertificates()` — la firma la fija el JDK — y de ahí sale cualquier
recorrido de una cadena de certificados. Apareció escribiendo `java.security.CodeSigner`,
`Timestamp` y `CertPath`, que la recorren para su `toString`.

Rodeo aplicado en esos tres archivos: la local intermedia. Repro mínimo en
`scratchpad/r350/Repro350.java`.

## Tanda: `java.net` — dos bugs que sólo se ven corriendo la prueba de comportamiento

### Finding #340 — el espejo de `Class` de la clase de entrada nunca se prepara, y consultarlo la rompe para siempre

Repro mínimo: cualquier clase que corra un `static synchronized` **propio** como punto de entrada.

```java
public class Repro340 {
    private static synchronized int uno() { return 1; }
    public static int run() { return uno(); }
}
```

```
thread 'main' panicked at src\jvm\interpreter\bytecode_interpreter\invokestatic.rs:307:
static synchronized: the Class mirror exists after initialization
```

Son **dos** bugs encadenados, y el segundo es el que dejaba daño permanente:

1. `invokestatic` daba por hecho que el espejo ya existía porque `ensure_initialized` había corrido
   más arriba. No alcanza: `ensure_initialized` corre `<clinit>` y nada más. Una clase **sin**
   `<clinit>` —o una que ya está en estado `Done`, que es exactamente el caso de la clase de
   entrada, porque la ejecución **empieza adentro** de ella— nunca pasa por `load_class`, que es
   quien asigna el espejo.

2. `class_mirror(name)` resolvía el nombre con `class_id(name)`, que **acuña un Class ID si no
   existe**. Y `load_class` se rinde apenas ve que el nombre ya tiene id — su invariante es ser el
   único que los acuña. Es decir: la consulta fallida dejaba la clase marcada como "ya vista" y
   `load_class` no la preparaba nunca más. El segundo intento fallaba igual, y cualquier otro uso
   de esa clase también.

Arreglo, en dos lugares:

- `MetaspaceService::class_mirror` chequea `has_class_id` primero y sale por `None` sin acuñar
  nada. Una consulta ya no tiene efectos de borde.
- El sitio de `invokestatic` llama a `class_operations::load_class` **antes** de pedir el espejo,
  que es el mismo orden que ya usaba `static_slot`.

Con eso, `java/NetTest.class` pasó de voltear la VM a correr entero.

### Finding #341 — un argumento de tipo variable elegía la sobrecarga más angosta, y compilaba a la equivocada

```java
class Box<V> {
    V v;
    String s() { return String.valueOf(v); }   // emite valueOf([C), no valueOf(Object)
}
```

```
  java.lang.String s();
       0: aload_0
       1: getfield      #39   // Field v:Ljava/lang/Object;
       4: invokestatic  #45   // Method java/lang/String.valueOf:([C)Ljava/lang/String;
```

`convertible` —la relación que decide qué candidatos son aplicables— arrancaba con
`if from == to || lenient(from) || lenient(to) { return true }`, y `lenient` da `true` para toda
`RType::TypeVar`. Con el argumento indulgente, **las siete sobrecargas** de `String.valueOf`
resultaban aplicables en la fase 1, y el desempate por §15.12.2.5 elegía la más específica: `char[]`
es subtipo de `Object`, así que ganaba `valueOf(char[])`.

La indulgencia tiene sentido en el **destino** (una variable de tipo del propio método genérico, que
la inferencia resuelve después) pero no en el **origen**: de una variable de tipo sí sabemos algo, su
cota (§4.4), y la cota es todo lo que el argumento puede ser. `bounds_of` ya devuelve `Object`
cuando no se declaró ninguna, así que el caso común cae justo en `valueOf(Object)`.

Arreglo: sacar `TypeVar` de la indulgencia del origen en `convertible` y agregar el arm
`(RType::TypeVar(v), _) => bounds_of(v).any(|b| convertible(b, to, boxing))`. `assignable` —la
relación de asignación, no la de invocación— se dejó como estaba: el bug era de resolución de
sobrecargas, y ensancharlo era arriesgar errores nuevos donde no había ninguno.

**Cómo apareció, que es lo que hace que valga la pena anotarlo**: no compilando, sino **corriendo**.
`java.util.AbstractMap` no tenía `toString`, así que `CookieManager.get(...)` se imprimía como
`java.util.HashMap@affa061f`. Al escribirle el `toString` —con el `String.valueOf(v)` que escribiría
cualquiera— la salida pasó a ser `{Cookie=}`: la clave bien, el valor vacío. Es la peor forma de este
bug: **compila, verifica, y devuelve la cadena vacía**. Ninguna red interna lo veía porque el
bytecode emitido es formalmente válido — sólo llama a otro método.

Ablación: con el arm puesto, `cargo test --release` da los **mismos 21 fallos** de la base (los dos
extra que aparecieron en una corrida son los dos de APT, que pasan solos y son intermitentes por el
directorio temporal que comparten).

### Finding #329 — no se aplica el control de acceso `protected` entre paquetes (JLS 6.6.2)

```java
package pk;
public class Base { protected int secreto() { return 7; } protected int campo = 3; }
```

```java
import pk.Base;
public class Prot {                       // NO es subclase de Base, y esta en otro paquete
    public static int run() {
        Base b = new Base();
        return b.secreto() + b.campo;     // los dos accesos son ilegales
    }
}
```

| | |
|---|---|
| javac real | `secreto() has protected access in Base` (y lo mismo para el campo) |
| nuestro javac | **compila y emite el `.class`** |
| JVM real, con nuestro `.class` | `IllegalAccessError: class Prot tried to access protected method 'int pk.Base.secreto()'` |

Es un *accepts-invalid*: no rompe programas correctos, sólo deja pasar los incorrectos. Pero cae en la
misma familia de esta tanda, porque el resultado es un `.class` que corre acá y ninguna otra JVM
acepta — y el error aparece lejos del lugar donde se puede arreglar.

Falta la regla entera, no un caso borde: ni el chequeo de paquete, ni la parte de §6.6.2 que exige que
el acceso sea a través de una referencia del tipo de la clase que accede (o de un subtipo suyo), que
es lo que impide que una subclase toque el `protected` de un *hermano*.

Lo encontró el agente de `javax.annotation.processing`, y **lo encontró corriendo su prueba en la JVM
real**: pasaba en nuestra VM y fallaba allá. Repro en `scratchpad/zz341/`.

### Finding #330 — los tipos anidados se resuelven por nombre simple entre unidades de compilación

En un `--emit` con varios archivos, un tipo anidado mencionado en **un** archivo cambia cómo resuelve
el mismo nombre simple en **otro**:

```java
// Sink.java, ya compilado al classpath
public interface Sink { void take(javax.tools.Diagnostic.Kind k); }
// Leak1.java
import javax.tools.JavaFileObject.Kind;
public class Leak1 { public static Kind k() { return Kind.SOURCE; } }
// Leak2.java
public class Leak2 implements Sink { public void take(javax.tools.Diagnostic.Kind k) { } }
```

```
javac --emit -cp "KajiLibrary;dir" Leak2.java              ->  compila
javac --emit -cp "KajiLibrary;dir" Leak1.java Leak2.java   ->  error: `Leak2` no es abstracta
                                                                y no implementa `take` de `Sink`
```

`Leak2` no cambió. Lo que cambió es que en el mismo round hay otro archivo que menciona
`JavaFileObject.Kind`, y a partir de ahí `Kind` --el nombre simple-- resuelve a ése en todos lados. El
`take` de `Leak2` termina tomando un parámetro de otro tipo que el de la interfaz, y por eso "no la
implementa".

Ablaciones del agente que lo encontró: sacar el `import` y escribir el nombre completo **no** alcanza
--es la mención, no el import--; con dos nombres simples distintos (`JavaFileManager.Location`) no
pasa; si los dos archivos usan `Diagnostic.Kind` tampoco; y el orden en la línea de comandos no
importa.

**Qué obliga a hacer hoy**: compilar de a un archivo los paquetes que mezclan tipos anidados
homónimos, que es justamente lo que `javax.tools` y `javax.lang.model` hacen todo el tiempo.
Repro en `scratchpad/zz342/`.

### Una divergencia de biblioteca que hay que decidir: `java.io` no declara `throws IOException`

No es un bug del compilador sino de la biblioteca, y se anota acá porque **aparece una y otra vez** y
cada vez cuesta el mismo rato de averiguación.

`java.io.InputStream` y `java.io.Reader` declaran `read`, `available`, `mark`, `reset` y `close`
**sin** `throws IOException`; el JDK las declara con ella. Lo mismo `java.io.Closeable.close()`, que
arrastra a `java.nio.channels.Channel` y a todo lo que lo extienda.

Las dos consecuencias, las dos reales y ya encontradas por separado:

1. **Un `catch (IOException)` alrededor de esas llamadas no compila** contra esta biblioteca —el
   compilador, con razón, dice que esa excepción no se puede lanzar ahí—. El agente de
   `javax.xml.transform` tuvo que escribir `catch (Throwable)` en `StreamSource.isEmpty()` por eso.
   O sea que el código escrito para el JDK **no compila acá**, que es la dirección de portabilidad que
   más duele.
2. **Ninguna subclase puede señalar un fallo de E/S por el canal previsto**, porque redefinir no puede
   ensanchar la cláusula `throws` (§8.4.8.3). Un `InputStream` propio que lea de un archivo no tiene
   cómo reportar que el archivo se rompió.

**Por qué no lo cambié**: agregar `throws IOException` en `InputStream`/`Reader`/`Closeable` obliga a
todo **llamador** dentro de la biblioteca a atrapar o declarar, y eso es una pasada por buena parte de
`java.io`, `java.nio`, `java.util.zip` y `javax.xml`. Es la decisión correcta y es grande; conviene
hacerla de una vez, con la batería entera detrás, y no de a pedazos mientras hay agentes escribiendo.

Queda medido y escrito para que se decida a propósito y no por omisión.

### Arreglado de paso: `Throwable.printStackTrace()` escribía en `System.out`

El JDK escribe en `System.err`. El comentario del archivo justificaba el desvío diciendo que "no hay
`System.err` en esta biblioteca todavía" — y `System.err` existe desde hace varias tandas, así que la
nota había quedado vieja y el destino equivocado.

No es cosmético: una traza en la salida estándar se mezcla con lo que el programa imprime, y un
`programa > archivo` se lleva el error adentro del resultado en vez de dejarlo en la consola. Que los
dos flujos estén separados es exactamente para que eso no pase. Verificado con
`scratchpad/zz343/Ps.java`: antes la marca salía por stdout y no por stderr; ahora al revés.

### Finding #342 — un constructor con `super(...)` explícito **y** campos con inicializador construía la superclase dos veces

```java
class Base {
    int marca;
    Base()          { this.marca = 1; }
    Base(String s)  { this.marca = 2; }
}
class ConCampo extends Base {
    int x = 7;
    ConCampo() { super("explicito"); }
}
```

```
  Repro342$ConCampo();
    Code:
         0: aload_0
         1: invokespecial #37   // Method Repro342$Base."<init>":()V     <-- de más
         4: aload_0
         5: bipush        7
         7: putfield      #39   // Field x:I
        10: aload_0
        11: ldc           #41   // String explicito
        13: invokespecial #44   // Method Repro342$Base."<init>":(Ljava/lang/String;)V
        16: return
```

`desugar` copia los inicializadores de instancia **al frente** del cuerpo de cada constructor
(§8.6/§12.5). Al frente de todo, incluido delante de un `super(...)` escrito. Después de eso el
emisor ya no ve un `super` como primera sentencia, así que le antepone el implícito — y quedan
**dos** `<init>` sobre el mismo `this`.

Lo que eso rompe, en orden de gravedad:

1. **La superclase se construye con el constructor equivocado primero.** Si su no-args tira, el
   objeto no se puede construir aunque el `super(...)` escrito fuera perfectamente válido.
2. Si la superclase **no tiene** constructor sin argumentos, el `.class` referencia uno que no
   existe.
3. Una JVM real **rechaza la clase**: §4.10.2.4 prohíbe dos `<init>` sobre el mismo
   `uninitializedThis`.

Cómo apareció: `java.net.MulticastSocket(SocketAddress)` hace `super(bindaddr)` y tiene cuatro
campos con inicializador. `new MulticastSocket((SocketAddress) null)` —que el JDK documenta como
"socket sin atar" y que tiene que andar— reventaba con la `SocketException` de
`DatagramSocket()`, el constructor que **no** se había escrito. Lo agarró `java/NetTest.java`
corriendo en las dos VMs; compilar no lo veía.

Arreglo, en `desugar.rs`: los inicializadores se insertan **detrás** del `super(...)` explícito
cuando lo hay (`delegates_to_super`), que es el orden que manda §12.5 —superclase, inicializadores,
cuerpo— y el que emite el javac real. El caso de `this(...)` ya estaba contemplado.

**Ojo con los `.class` versionados**: todo constructor de KajiLibrary con `super(...)` explícito y
campos con inicializador quedó mal emitido hasta este arreglo. Los `.class` de `java.net` se
regeneraron; el resto del árbol conviene refrescarlo.

### Finding #343 ⬜ — la resolución de constructores no filtra por accesibilidad

```java
new DatagramSocket(null)
```

```
error: la referencia a `DatagramSocket` es ambigua
```

`java.net.DatagramSocket` tiene tres constructores de un argumento: `(SocketAddress)` y `(int)`,
públicos, y `(DatagramSocketImpl)`, **protected**. El javac real descarta el `protected` desde el
paso 1 de §15.12.2 —los candidatos tienen que ser accesibles desde el sitio de la llamada— y le
queda uno solo aplicable a `null`. El nuestro los mantiene a los dos y declara ambigüedad.

No es exclusivo de los constructores: la misma falta de filtro haría ambigua cualquier llamada donde
la sobrecarga que desempata sea inaccesible. No apareció antes porque hace falta una sobrecarga
`protected` o package-private con la misma aridad que una pública, que es un diseño poco común fuera
del JDK.

Rodeo aplicado en `java/NetTest.java`: el cast explícito, `new DatagramSocket((SocketAddress) null)`,
que además es más claro. Repro mínimo: la línea de arriba contra `KajiLibrary`.

### Finding #351 ⬜ — dos tipos anidados con el mismo nombre simple se pisan dentro de la misma unidad

```java
public class GF {
    static java.util.Map.Entry<String, Integer> a(java.util.Map<String, Integer> m) {
        return m.entrySet().iterator().next();
    }

    static java.security.KeyStore.Entry b() {
        return null;
    }
}
```

```
error: tipo de retorno incompatible
        return m.entrySet().iterator().next();
               ^
```

Los dos tipos anidados se nombran **completamente calificados** y viven en clases distintas
(`java.util.Map` y `java.security.KeyStore`); lo unico que comparten es el nombre simple `Entry`.
Con las dos referencias en la misma unidad de compilacion, la de `Map.Entry` se resuelve al `Entry`
equivocado y cualquier uso parametrizado deja de tipar.

Es **independiente del orden**: da igual cual de los dos metodos venga primero. Y desaparece
borrando cualquiera de los dos: `a()` sola compila, `b()` sola compila.

Solo pasa con tipos que se cargan desde `.class` del classpath. La version equivalente con dos
clases anidadas definidas en el mismo archivo fuente compila bien, asi que el problema esta en la
cache/tabla que se llena al leer tipos anidados de un `.class`, no en el resolutor de nombres del
parser.

Como aparecio: `java/SecTest.java` prueba `java.security.KeyStore.getEntry` —que devuelve
`KeyStore.Entry`— y en el mismo archivo un `KeyStoreSpi` de prueba recorre un
`Map<String, Certificate>` con `Map.Entry`. El error salia en la clase del `Map`, a 2000 lineas de
la referencia que lo causaba, lo que lo hace especialmente confuso de diagnosticar.

Rodeo aplicado en `java/SecTest.java`: recorrer el mapa por `keySet()` en vez de por `entrySet()`.
Repro minimo: el bloque de arriba contra `KajiLibrary`.

### Finding #352 ⬜ — un tipo homónimo del propio paquete le gana al import de un solo tipo cuando los dos están en la misma invocación

```java
// p/sub/Cosa.java
package p.sub;
public class Cosa {
    public int valor() { return 1; }
}

// p/Cosa.java  — homónima, en el paquete de al lado, sin relación con la anterior
package p;
public interface Cosa {
}

// p/Usa.java
package p;
import p.sub.Cosa;
public class Usa {
    public static int leer(Cosa c) {
        return c.valor();
    }
}
```

```
p/Usa.java:9: error: no se encuentra el método: valor
        return c.valor();
               ^
  símbolo:   método valor()
```

Un import de un solo tipo **tapa** al tipo homónimo del propio paquete (JLS §6.4.1: el import está
en un scope más interno que la declaración de paquete). En `Usa`, `Cosa` tiene que significar
`p.sub.Cosa`, y `valor()` existe.

Lo llamativo es **cuándo** falla: compilar `p/Usa.java` solo, con `p/Cosa.class` ya en disco, anda
perfecto. Falla únicamente cuando `p/Cosa.java` entra en la **misma invocación** de `javac`, y es
independiente del orden en la línea de comandos. O sea que los tipos que se están compilando en esta
tanda se meten en la resolución de nombres con más prioridad que un import explícito, mientras que
los que vienen del classpath no. El javac real acepta las tres juntas sin chistar.

Cómo apareció: `java/security/CodeSource.java` importa `java.security.cert.Certificate` y en
`java.security` existe la interfaz obsoleta `java.security.Certificate`. Cada archivo compila bien
por separado —así se emitió toda la biblioteca— pero **una reconstrucción de `java/security` entero
en una sola invocación falla**, con el error a 90 líneas del import que lo causa:

```
javac --emit -cp . java/security/Certificate.java java/security/CodeSource.java
java/security/CodeSource.java:104: error: tipo incompatible en `cs`
```

No hay rodeo aplicado: los `.class` del árbol están bien porque se emitieron en tandas chicas. Lo
que hay que saber es que **`java/security` no se puede reconstruir de un saque** hasta que esto se
arregle. Repro mínimo: el bloque de arriba.

**Corolario de #352 — la variante silenciosa.** El caso de arriba al menos **falla**. Hay una
segunda forma del mismo problema que no falla: emite mal y sigue.

`java/security/Identity.java` usa el nombre simple `Certificate` **sin import**, en el paquete
`java.security`, o sea `java.security.Certificate`. Compilando el directorio en una tanda que
**excluia** `Certificate.java`, ese nombre se resolvio a `java.security.cert.Certificate` —el tipo
del otro paquete, que llega por el classpath— y los tres metodos salieron con la firma equivocada:

```
public java.security.cert.Certificate[] certificates()      // deberia ser java.security.Certificate[]
public void addCertificate(java.security.cert.Certificate)
public void removeCertificate(java.security.cert.Certificate)
```

Sin ningun error. Se detecto midiendo contra el JDK (`Identity` cayo de 17/17 a 14/17), no
compilando. Recompilando `Identity.java` sola vuelve a salir bien.

Los dos sintomas son la misma causa —que composicion tenga la tanda cambia como se resuelven los
nombres— y juntos dan la regla practica de hoy: **`java/security` se reconstruye archivo por
archivo**, no por lotes.

### Finding #331 — el emisor toma la cláusula `throws` del classpath, no de la fuente

La prueba es de dos líneas y concluyente. Se le puso a `java.io.Flushable.flush()` un
`throws InterruptedException` en la **fuente**; el `.class` emitido salió con
`throws IOException` — el del JDK.

```
fuente:  void flush() throws InterruptedException;
.class:  public abstract void flush() throws java.io.IOException;
```

Cuando una clase que compilamos **también existe en el classpath**, el atributo `Exceptions` que se
emite es el de la copia del classpath y la cláusula de la fuente se descarta. Como `-cp KajiLibrary`
va delante del JDK explotado, para la biblioteca la copia del classpath es **nuestro propio `.class`
anterior**, así que el valor se autoperpetúa de build en build.

**El chequeador no hace lo mismo**, y ahí está el daño: dentro de un round mira la fuente. Así que
emisor y chequeador discrepan, y eso produce dos síntomas que parecen no tener relación:

- Un `throws` escrito en la fuente **no aparece** en el `.class` si el JDK no lo tiene. Fue el caso de
  `PriorityBlockingQueue.put`, que investigué como "un `.class` viejo en el árbol" y **anoté como
  «lo que NO resultó ser un bug»**. Esa nota estaba equivocada: era este bug.
- Un `throws` que la fuente **no** declara sí aparece si el JDK lo tiene. Por eso
  `catch (IOException)` alrededor de `InputStream.read()` compilaba desde afuera aunque la fuente no
  declarara nada, mientras que dentro de `java.io` el chequeador lo rechazaba.

**Lo que se hizo con esto, y el orden importa.** Se arreglaron primero las **fuentes**: 215
declaraciones de `java.io` --y `java.lang.Appendable`, y `Closeable`, que arrastraba a toda la cadena
de canales-- recuperaron el `throws` que el JDK declara, emparejadas contra `javap` por tipos de
parámetro exactos y tipo de retorno (por nombre y aridad hay falsos positivos, y sin el retorno se le
atribuye a un método el `throws` de su **puente**). Después de eso `java.io` compila entero y sus
firmas emitidas siguen coincidiendo con el JDK en las 74 clases.

**Arreglar el emisor tiene que venir después, no antes.** Si se lo hiciera honrar la fuente mientras
las fuentes estaban incompletas, todas esas firmas habrían perdido su `throws` de golpe: hoy están
bien **por accidente**, y el accidente es lo único que las sostiene hasta que las fuentes estén
completas. Falta auditar el resto de los paquetes con el mismo método antes de tocar `src/`.


## Tanda: `java.lang.foreign`, y los cuatro bloqueos que lo tenian parado

`java.lang.foreign` cierra en **246/246**. Los seis miembros que faltaban --`MemoryLayout.varHandle`,
`arrayElementVarHandle`, `byteOffsetHandle`, `sliceHandle`, `scaleHandle` y `ValueLayout.varHandle`--
no faltaban por alcance: la nota de `MemoryLayout` decia, con razon, que **no habia manera de escribir
el sitio de llamada** que los usa, y listaba cuatro hechos comprobados. Esta tanda levanta los cuatro.

### Finding #353 - un `native` sin puente mataba el proceso en vez de tirar

`natives::dispatch` terminaba en `_ => panic!("no native implementation for ...")`. O sea que "esta
biblioteca declara un metodo que la VM todavia no implementa" --una ausencia perfectamente normal
mientras se construye un JDK-- se convertia en la muerte del interprete: sin traza de Java, sin `catch`
posible, y sin manera de que una biblioteca degradara con elegancia.

**Arreglo**: `UnsatisfiedLinkError`, que es lo que tira la JVM real. `dispatch` no tenia como
senalarlo --devolvia `Option<Value>`, con las dos respuestas ya ocupadas por "valor" y "void"-- asi que
se le agrego una tercera, `NativeOutcome::Unimplemented`. Los tres sitios del opcode la convierten en
la excepcion; el cuarto --`call_virtual`, el camino de las llamadas **iniciadas por la VM**-- la
convierte en `None`, porque ahi no hay pila de Java donde ponerla ni `catch` que la pudiera atrapar.

Repro en `scratchpad/unsat/`: un `native` que la VM no implementa, atrapado. Da `-1` en las dos VMs.

### Finding #354 - javac no implementaba el polimorfismo de firma (JLS 15.12.3)

El bloqueo grande, y el que hacia imposible cualquier prueba cruzada. `MethodHandle.invoke`/
`invokeExact` y los 31 accesores de `VarHandle` se declaran **una sola vez** como `(Object...)`, pero
no se llaman como un varargs: el descriptor que va al sitio es el de los **argumentos reales**, sin
boxear, y el de retorno lo dicta el **contexto**. Nuestro javac los trataba como un varargs comun:
armaba un `Object[]`, boxeaba, y emitia `([Ljava/lang/Object;)Ljava/lang/Object;` -- un sitio que la
JVM real rechaza con `WrongMethodTypeException`.

**Arreglo**, en cinco lugares y ninguno de mas:

- `codegen.rs`: el predicado `signature_polymorphic` (los cuatro requisitos del JLS: declarado en
  `MethodHandle` o `VarHandle`, `native`, varargs, un solo `Object[]`), el descriptor armado desde los
  argumentos reales, y el retorno tomado del contexto por un canal de un solo uso (`sigpoly_ret`) que
  llenan `cast` y `discard`.
- `desugar.rs`: no empaquetar el varargs.
- `transtypes.rs`: no convertir los argumentos, y **no materializar el cast**.
- `classfile.rs`/`enter.rs`: leer `ACC_NATIVE` de los metodos **externos**. Sin esto el predicado no
  disparaba nunca, porque `MethodHandle` y `VarHandle` siempre llegan del classpath y jamas del
  fuente que se compila.

Contra el javac del JDK 25, los cinco casos salen **identicos**:

```
vh.set(9)                  ->  VarHandle.set:(I)V
return (int) vh.get(o, i)  ->  VarHandle.get:(Ljava/lang/Object;J)I
return (String) vh.get()   ->  VarHandle.get:()Ljava/lang/String;
vh.get();  // sentencia    ->  VarHandle.get:()V
mh.invokeExact(a)          ->  MethodHandle.invokeExact:(I)V
```

**Y disolvio el otro defecto que estaba anotado aparte.** La nota de `scratchpad/cast-unbox/` decia
que `(int) vh.get(...)` "tampoco emite el unboxing", y lo daba por un segundo bug independiente. No lo
era: el cast se realiza **en el descriptor**, asi que nunca hubo un unboxing que emitir. El javac real
tampoco emite uno.

Repro en `scratchpad/sigpoly/SpCast.java`, con los cinco casos.

### La VM ahora conoce `VarHandle`

El tercer hecho de la nota --`grep -rn VarHandle src/` daba cero-- ya no es cierto. La intercepcion es
la misma idea que la de `MethodHandle.invoke`: se ataja **antes** de resolver, porque resolver
buscaria el `(Object[])Object` declarado.

Lo que se atiende y lo que no, dicho de frente:

- **Si**: las lecturas y escrituras en los cinco modos (plain, `Opaque`, `Acquire`, `Release`,
  `Volatile`). Los cinco hacen lo mismo, y en un interprete de un solo hilo por acceso eso es
  **correcto** y no una simplificacion: no hay reordenamiento que impedir, asi que la garantia mas
  fuerte se cumple sin hacer nada.
- **No**: los modos de lectura-modificacion-escritura (`compareAndSet`, `getAndAdd`, ...). Esos si
  necesitan atomicidad de verdad, y fingirla seria exactamente el tipo de mentira que este proyecto no
  admite. Caen en el `native` sin puente, o sea en el `UnsatisfiedLinkError` del #353 -- que es
  justamente la razon por la que ese finding va primero.

El despacho va a helpers **tipados** de `VarHandleDeSegmento` (`get` con retorno `I` -> `leerInt`), con
los indices de los pasos abiertos empaquetados en un `long[]` que la VM arma. Un arreglo y no una
aridad por cada cantidad de indices: son 18 helpers en vez de setenta y pico. `MethodHandleDeLayout`
usa el mismo mecanismo con dos helpers, uno por forma de retorno.

Un caso que casi se pasa por alto y que vale anotar: una lectura **como sentencia** (`vh.get(seg, 0);`)
tiene retorno `V` por la regla del contexto, y no es un no-op -- los chequeos de limites y de
alineacion son **efectos** de acceder. Tiene su propio helper, `leerYDescartar`, que hace el acceso y
tira el valor.

### Una divergencia, y es la unica

`java.lang.invoke.VarHandles` queda **publica**, y en el JDK es package-private. El constructor de
`VarHandle` es package-private, asi que su subclase tiene que vivir en `java.lang.invoke`, y
`java.lang.foreign` --que es quien la fabrica-- no puede nombrarla. El JDK resuelve esa costura con
**modulos**; sin modulos, la unica forma de que un paquete llegue a otro es que el miembro sea publico.

No es un miembro que miente --hace exactamente lo que dice-- pero es un miembro **de mas**, que es la
otra forma de no coincidir: codigo escrito contra `VarHandles.deSegmento` compila aca y no compila
contra un JDK de verdad. Esta escrito en la cabecera del archivo, con las dos alternativas que se
descartaron y por que.

### Finding #355 - el #341 rompio la conversion hacia una captura de `? super T`

No es de esta tanda: lo introdujo el #341, y lo destapo el recompile de esta. El #341 hizo que un
argumento de tipo **variable** solo fuera aplicable a lo que su **cota** es aplicable, que es correcto.
Lo que le falto es que una **captura** no es un `RType::TypeVar` sino un `RType::Capture`, asi que
`lenient` no la atajaba y el arm nuevo terminaba preguntando si la cota *superior* del argumento
--`Object`-- entra en la captura. No entra.

Resultado: `Comparator.thenComparing` dejo de compilar (`otro.compare(a, b)` con
`otro : Comparator<? super T>` y `a, b : T`), y con el `List`, `Map`, `Collection`, `Iterable`,
`Collections` y todo lo que los nombra -- **veinticinco archivos en el recompile, en cascada desde uno**.

**Arreglo**: un arm que va **antes** del de `TypeVar`. Un destino que es la captura de un `? super L`
acepta todo lo que `L` acepta, porque la captura designa *algun* supertipo de `L` (4.10.2, 5.2).

Repro en `scratchpad/zz341/Cmp.java`: el javac congelado y el del JDK 25 lo aceptan; el de la tanda
anterior lo rechazaba.

### Verificacion

`java/VhTest.java` (21 comprobaciones: los `ValueLayout` sueltos de cuatro carriers, caminos cerrados
sobre un struct, pasos abiertos, `arrayElementVarHandle`, la suma del desplazamiento del llamador, los
cinco modos de memoria, y el error por aridad equivocada) y `java/MhLayoutTest.java` (13: `scaleHandle`,
`byteOffsetHandle` cerrado y abierto, `sliceHandle`) dan **`-1` en las dos VMs**. Las expectativas se
escribieron y se corrieron **primero contra el JDK real**.

Una divergencia previa que aparecio al escribirlas y que **no** es de VarHandles: `Arena.allocate(n)`
sola no sirve para probar esto, porque el JDK devuelve memoria nativa --que queda alineada a 8 aunque
se haya pedido 1-- y nuestro segmento de heap se respalda en un `byte[]`, que solo puede garantizar 1.
Los dos son correctos para lo que representan. Las pruebas piden el alineamiento explicito.

### Y el arrastre de `java.io` que rompio `java.util.zip`

Otra sesion completo `java.io.InputStream.read()` con su `throws IOException`, y eso volteo la familia
entera de flujos de `java.util.zip`: once archivos que no compilaban. Es el mismo patron del #316 --
completar una base rompe a quien se apoyaba en que estuviera incompleta.

La correccion no es opinable y por eso se automatizo en dos pasos: lo **publico** se toma del JDK real
(que metodos declaran `throws IOException`), y los **helpers privados** --`fill`, `drain`,
`writeHeader`-- del error del compilador, iterando hasta punto fijo porque agregar un `throws` destapa
el siguiente. `java.util.zip` vuelve a 240/240 y su prueba sigue en `-1`.

---

## Tanda: `jdk.internal.reflect`

### Finding #390 ⬜ — un literal de clase usado **como receptor** pierde su argumento generico

`X.class` vale `Class<X>`, y ese argumento es lo unico que hace utiles a los metodos de `Class` que
lo devuelven adentro de otro tipo: `getDeclaredConstructor` devuelve `Constructor<T>`, no
`Constructor`. Nuestro javac lo tipa como `Class` pelada, y en `attribute.rs` esta dicho a proposito:

```rust
// `C.class` vale un `java.lang.Class` (se ignora el argumento genérico `<C>`, que la
// *erasure* borra igual).
ExprKind::ClassLit(_) => {
    (resolve_rtype(env.table, env.class_scope, &Type::Class("Class".into())), None)
}
```

El comentario es cierto para el **codegen** —la erasure lo borra— y falso para la **atribucion**: el
tipo del literal es lo que alimenta la sustitucion de `T` cuando el literal es el receptor.

Repro minimo (`scratchpad/G3.java`), que el JDK 25 acepta:

```java
Constructor<String> k = String.class.getDeclaredConstructor();   // error: tipo incompatible en `k`
```

Lo que delimita el bug es que **las tres variantes vecinas si compilan**:

```java
Class<String> c = String.class;  Constructor<String> k = c.getDeclaredConstructor();   // ok
Constructor<String> k = ((Class<String>) String.class).getDeclaredConstructor();       // ok
String s = String.class.cast("x");                                                     // ok
```

O sea: el literal **si** lleva su argumento cuando se lo asigna o se lo castea, y lo pierde solo en
posicion de receptor. Que `cast` ande y `getDeclaredConstructor` no sugiere que lo que falta es la
sustitucion de `T` **anidada** dentro del tipo de retorno, no la de `T` a secas.

Arreglo probable: tipar `ClassLit(ty)` como `Parameterized { base: Class, args: [ty] }` (con
`Integer`/`Void`/etc. para los primitivos, como manda la JLS 15.8.2). **No se hizo en esta sesion**:
cambia el tipo de *todas* las apariciones de `X.class` en la biblioteca entera, y verificarlo pide un
recompile completo que no entra en el alcance de esta tanda.

Rodeo mientras tanto: pasar el literal por una variable `Class<X>`. Esta hecho asi, con el numero de
finding al lado, en `java/ReflTest.java`.

## Tanda: `javax.lang.model.util`

Las 55 clases que faltaban son cinco familias de visitantes por seis versiones del lenguaje, mas
`ElementFilter`. El paquete quedo en 57/57 y 276/276, y la prueba `java/ModelUtilTest.java` (68
comprobaciones de despacho) da lo mismo en las dos VMs. Los tres findings de abajo salieron de
traducir el fuente del JDK: los tres tienen rodeo y ninguno bloqueo el cierre del paquete.

### Finding #400 ⬜ — el parser no acepta `Interface.super.metodo()`

Una clase que implementa una interfaz con metodos `default` puede llamar al cuerpo heredado con
`Interface.super.m()` (JLS 15.12.1). Nuestro parser corta ahi:

```java
interface I { default String f() { return "I"; } }
public class Probe implements I {
    public String f() { return I.super.f() + "-C"; }   // error: se esperaba un identificador, se encontró Super
}
```

Lo delimita que **`Outer.this` si compila**, asi que no es que el parser no sepa calificar una
expresion primaria con un nombre de tipo — es solo la forma con `super`:

```java
public class S3 { class In { S3 g() { return S3.this; } } }   // ok
```

Donde aparecio: `AbstractElementVisitor6.visitModule` en el JDK es
`return ElementVisitor.super.visitModule(e, p);`. Rodeo: escribir el cuerpo del `default` en vez de
delegarle. En este caso el `default` de nuestro `ElementVisitor` es `return visitUnknown(e, p);` y esa
es la linea que quedo, con el numero de finding al lado — las dos formas terminan en el mismo
`visitUnknown` de la subclase, asi que no hay diferencia observable.

### Finding #401 ⬜ — el generador de bytecode no baja un `switch` con selector `enum`

```java
switch (k) {                 // k es un javax.lang.model.element.ElementKind
case CLASS: return "c";
default: throw new AssertionError();
}
// error: el generador de bytecode todavía no soporta un `switch` cuyo selector no es `int`
```

El mensaje dice "no es `int`" y sugiere que falta todo el desugar, pero **el de `String` si anda**:

```java
switch (s) { case "a": return 1; default: return 0; }   // ok
switch (i) { case 1:   return 1; default: return 0; }   // ok
```

O sea que el desugar existe y lo que le falta es el caso del enum, que se baja distinto: la constante
de `case` es un nombre **no calificado** que hay que resolver contra el tipo del selector, y el
selector se convierte con `ordinal()` (o con la tabla `$SwitchMap`).

Donde aparecio: `ElementKindVisitor6` y `TypeKindVisitor6` reparten por kind con tres y dos `switch`
respectivamente. Rodeo: `if` encadenados. Es la misma semantica porque en el original ninguna rama cae
en la siguiente — todas devuelven — y esta dicho asi en un comentario en las dos clases.

### Finding #402 ⬜ — una clase anonima no puede pasarle al `super` un argumento tipado con una variable de tipo de la superclase

Repro minimo:

```java
public class F {
    static class G<R> { G(R v) {} }
    public static Object f() { return new G<String>("D") {}; }   // error: el generador de bytecode
}                                                                //        no puede resolver el tipo `R`
```

El error no trae linea (sale como `:0:`), asi que es de la fase de emision y no de la atribucion.

Lo que lo delimita son **cuatro variantes vecinas que si compilan**, y entre las cuatro dejan el bug
en un solo punto:

```java
new G<String>("D");                                    // ok — instanciacion sin cuerpo
new G<String>() {};                                    // ok — anonima, pero al constructor sin argumentos
class E extends G<String> { E() { super("D"); } }      // ok — subclase con nombre
static class G2<R> { G2(String v) {} }
new G2<String>("D") {};                                // ok — anonima, ctor cuyo parametro NO es `R`
```

O sea: falla solo cuando se juntan las tres cosas — clase **anonima**, con **argumento**, y el
parametro formal de ese constructor **declarado con una variable de tipo de la superclase**. Lo que
parece faltar es sustituir `R` por el argumento de tipo (`String`) al armar el descriptor de la llamada
a `<init>` de la superclase; con el parametro ya concreto no hay nada que sustituir y por eso `G2`
anda.

Donde aparecio: los `SimpleXxxVisitorN` y los `ElementKindVisitorN` toman `(R defaultValue)`, y la
manera natural de probarlos es `new SimpleElementVisitor6<String, Void>("D") {}`. Rodeo, en
`java/ModelUtilTest.java`: una subclase **con nombre** por cada visitante, con constructor sin
argumentos que hace `super("D")`, y la anonima con cuerpo colgando de esa. Prueba exactamente lo mismo
—`DEFAULT_VALUE` sigue llegando por el constructor de un argumento— y esta explicado ahi con el numero
de finding al lado.

### Finding #403 ⬜ — el parser no acepta el constructor canonico compacto de un `record`

Un `record` puede escribir su constructor canonico en la forma **compacta**: se declaran los
parametros implicitamente y las asignaciones a los campos las agrega el compilador al final
(JLS 8.10.4.2). Nuestro parser corta en la llave:

```java
public record Compacto(int n) {
    public Compacto {                       // error: se esperaba un identificador, se encontró LBrace
        if (n < 0) throw new IllegalArgumentException();
    }
}
```

Lo delimita que **la forma canonica completa si compila**, asi que los `record` con validacion no
quedan afuera; lo unico que falta es la abreviatura:

```java
public record Explicito(int n) {
    public Explicito(int n) {               // ok
        if (n < 0) throw new IllegalArgumentException();
        this.n = n;
    }
}
```

Los dos fragmentos de arriba son el repro completo: se compilan solos, sin classpath ni nada al
lado. El JDK 25 real acepta las dos formas.

Donde aparecio: `java.security.PEMRecord`, que en el JDK usa la forma compacta para rechazar un
`type` nulo o con sintaxis de PEM ya armada. Rodeo: el constructor canonico completo, con las tres
asignaciones escritas a mano y el numero de finding al lado. Las dos formas son equivalentes para el
lenguaje —la compacta solo ahorra escribir las asignaciones— asi que no hay diferencia observable en
el `.class`; se comprobo con `java/PemTest.java`, que da lo mismo en las dos VMs.


## Tanda: el literal de clase perdia su argumento (#390, arreglado)

### Finding #390 - `X.class` tipaba como `Class` pelada, y por eso la inferencia elegia la cota

Lo habia anotado un agente al cerrar `jdk.internal.reflect`, sin arreglar. Se arreglo, y vale la pena
contar como se delimito porque el sintoma no apunta a la causa.

`ExprKind::ClassLit` resolvia a `java.lang.Class` **sin argumento**, con un comentario que decia que
el argumento se podia ignorar *"porque la erasure lo borra igual"*. Eso es cierto para el **descriptor**
que emite el generador y falso para la **atribucion**, que es donde se infiere.

Lo que producia:

```java
static <T extends Base> T leer(Class<T> c) { ... }
static void usa(Sub s) { ... }

Sub v = leer(Sub.class); usa(v);   // andaba
usa(leer(Sub.class));              // "no se encontro un metodo `usa(Base)` aplicable"
```

La primera forma anda porque ahi la inferencia la dirige el **destino de la asignacion**; la segunda
la dirige el argumento, y con un `Class` pelado no hay de donde sacar `T`, asi que cae en la cota.

**La delimitacion** esta en `scratchpad/zz405/Loc.java`, y es lo que convierte una sospecha en un
diagnostico: se probaron cuatro formas de inferir el mismo `T`.

| forma | resultado |
|---|---|
| `porValor(s)` con `s : Sub` | anda |
| `porLista(l)` con `l : List<Sub>` | anda |
| `porClase(claseDeSub())` con retorno `Class<Sub>` | anda |
| `porClase(Sub.class)` | **falla** |

O sea que ni la inferencia ni la resolucion de sobrecargas estaban rotas: lo unico roto era el tipo
del **literal**. Tres de cuatro andando es lo que descarta las otras hipotesis.

**Arreglo**: `C.class` vale `Class<C>` (§15.8.2). Con dos cuidados que el JLS obliga y que es facil
pasar por alto: un primitivo va con su **envoltorio** --`int.class` es `Class<Integer>`, porque un
argumento de tipo no puede ser primitivo (§4.5.1)-- y `void.class` es `Class<Void>`. Si el argumento
no resuelve se cae al `Class` pelado de antes: peor para inferir, pero no rompe nada que ya funcione.

### Y un desmentido: los metodos `default` **no** pierden su `public`

En el mismo informe venia un segundo bug: *"KajiJDK le da acceso de paquete a los metodos `default` de
una interfaz"*, con un repro de dos archivos en paquetes distintos. **No reproduce**, ni con el javac
del arbol ni con el congelado de `bin/`:

- El `.class` que emitimos para `default int dos(int)` sale con `flags: (0x0001) ACC_PUBLIC`.
- La llamada cruzando paquetes compila por los dos caminos --con la interfaz como **fuente** en la
  misma invocacion, y como **externa** desde el classpath--.

Queda escrito porque un bug fantasma cuesta caro: manda a arreglar lo que no esta roto, y peor, deja
sin explicar el sintoma real que si estaba. Lo que fallaba en aquel paquete era otra cosa, y sigue sin
identificarse. Repro del desmentido en `scratchpad/zz404/`.


## Tanda: `javax.print.attribute.standard`

### Finding #440 - la instancia externa implicita solo se agrega desde la clase externa

Una clase interna --no `static`-- lleva un primer parametro oculto con la instancia de la clase que
la encierra. El compilador lo agrega bien cuando el `new` esta escrito **en la clase externa**, y no
lo agrega cuando esta escrito **adentro de otra interna**, aunque ahi `Externa.this` este igual de
disponible (JLS §15.9.2: si la clase es interna y el `new` no tiene calificador, la instancia
externa es la del contexto que encierra al `new`).

Repro minimo, dos formas del mismo programa:

```java
public class Outer5 {
    B desdeElExterior() { return new B(1); }   // anda
    class B { B(int x) {} }
}

public class Outer6 {
    class A {
        A(int y) {}
        A hacer() { return new A(2); }         // "no se encontro un constructor `A(int)` aplicable"
    }                                          // "metodo A.A(Outer6, int) no es aplicable"
}
```

El mensaje delata la causa: el compilador ya sabe que el constructor real es `A(Outer6, int)` --lo
imprime-- y le pasa un solo argumento. O sea que la firma se computa bien y lo que falta es el paso
que sintetiza el argumento del `this` externo, que aparentemente esta condicionado a que el contexto
del `new` sea la clase externa misma.

**Delimitacion.** Tres cortes, y los tres importan porque descartan lo obvio:

| forma | resultado |
|---|---|
| `new B(1)` desde la clase externa | anda |
| `new B()` sin argumentos, desde otra interna | **anda** |
| `new B(1)` con argumentos, desde otra interna | falla |
| `new A(2)` con argumentos, desde la propia `A` | falla |

Que el caso sin argumentos ande es lo que hace ruido: sugiere que hay dos caminos distintos para
resolver el constructor y que solo uno hace la sintesis. Y que falle tambien dentro de la *misma*
clase interna descarta que sea un problema de alcance entre hermanas.

No es un problema de generics: se vio primero con
`Iterator<Map.Entry<PrinterStateReason, Severity>>` y reduce sin generics ninguno.

Donde aparecio: `PrinterStateReasons`, cuya vista por gravedad es un `AbstractSet` interno que
fabrica un `Iterator` interno. Rodeo: las dos clases auxiliares se declararon `static`. Es
equivalente aca --ninguna de las dos usa el `this` externo, solo lo que le pasan por constructor-- y
las dos son `private`, asi que no hay diferencia en la API. En el JDK son internas.

---

### #460 -- la asignacion encadenada sobre dos **campos** no deja el valor en la pila

```java
class X {
    Object p, q;
    X() { p = q = null; }      // compila; el constructor revienta al ejecutarse
}
```

Compila sin una queja y emite **bytecode invalido**. La VM lo caza en el primer uso
(`frame.rs:236: operand stack underflow`); un JVM de verdad lo rechazaria en la verificacion,
porque el marco declara `stack=3` y el codigo intenta sacar de una pila vacia.

| JDK 25 | el nuestro |
|---|---|
| `aload_0` / `aload_0` / `aconst_null` | igual |
| `dup_x1` | *(nada)* |
| `putfield q` | `putfield q` |
| `putfield p` | `putfield p` ← ya no queda el `objectref` |

La causa se lee sola en la tabla. `a = b = v` es una **expresion** de asignacion anidada: la
interna tiene que *dejar su valor* para que la externa lo consuma. Cuando el destino es un local
eso es gratis --`istore`/`astore` no tocan lo que ya estaba y el valor se duplica con un `dup`
comun, o directamente se reemite--, pero cuando es un campo el `putfield` consume **dos** slots
(el `objectref` y el valor), asi que el valor hay que meterlo *debajo* del `objectref` de la
asignacion externa: para eso esta el `dup_x1`. Nuestro codegen emite el `putfield` como si fuera
una sentencia y no una expresion, y el segundo `putfield` se queda sin receptor.

**Delimitacion.** Cuatro cortes, y el contraste entre los dos primeros es el que senala el camino
roto:

| forma | resultado |
|---|---|
| `p = q = v` sobre dos **locales** | anda |
| `x.p = x.q = v` sobre dos **campos** de referencia | **falla** |
| `x.a = x.b = 7` sobre dos campos **primitivos** | **falla** |
| `x.p = h = v` con el **interno** en un local | anda |
| `x.q = v; x.p = v;` en sentencias separadas | anda |

Que falle igual con `int` que con referencias descarta que sea cosa del tipo, y el cuarto corte
--el interno en un local, el externo en un campo-- lo termina de acotar: lo que importa no es el
destino de la asignacion externa sino **el de la interna**, que es la que tiene que dejar el valor.
Ahi el destino local usa `dup` comun y anda; el campo necesita `dup_x1` y no lo emite. Con un campo
de categoria 2
(`long`/`double`) haria falta `dup2_x1`, asi que el arreglo tiene que mirar el ancho y no emitir
`dup_x1` a ciegas.

Es el mismo modo de falla de #289: **compila, el sintoma aparece lejos** --y aca especialmente
lejos, porque el constructor que lo lleva puede correr mil veces antes de que a alguien se le
ocurra mirarlo--. A favor: revienta en el primer uso y no corrompe memoria.

Como salio: escribiendo `javax.naming`. `NamingException` tiene, en sus dos constructores,
`resolvedName = remainingName = null` --es literalmente el codigo del JDK--, y eso hacia que
**toda** la jerarquia de veinticinco excepciones del paquete fuera imposible de instanciar. El
sintoma aparecio a tres saltos de distancia: una prueba de `CompositeName.addAll` que revienta al
construir el `InvalidNameException` que tiene que lanzar.

Rodeo aplicado en `NamingException`: las dos asignaciones en sentencias separadas, con un
comentario que apunta aca. Es equivalente --y los campos ya arrancan en `null` de todos modos--,
pero el defecto queda.

Repro: `KajiLibrary/repros/finding_460.java` (los dos que fallan y los dos controles; el JDK 25
devuelve `-1 -1 -1 -1` en los cuatro).

### #461 -- una constante `case` que viene de otra unidad de compilacion se rechaza

```java
switch (x) {
    case Integer.MAX_VALUE: ...   // error: no soporta un `case` que no es una constante entera
}
```

No compila. El mensaje --*"el generador de bytecode todavia no soporta un `case` que no es una
constante entera"*-- describe mal lo que pasa: `Integer.MAX_VALUE` **es** una constante entera, y
el propio compilador la pliega sin problemas en cualquier otra posicion.

**Delimitacion.** Cuatro cortes, y el que parte el mundo en dos es el tercero contra el cuarto:

| forma | resultado |
|---|---|
| `case P:` -- constante de la misma clase, nombre simple | anda |
| `case Anidada.N:` -- constante de una clase **anidada**, calificada | anda |
| `case Vecina.V:` -- otra clase de **primer nivel del mismo archivo**, calificada | anda |
| `case Integer.MAX_VALUE:` -- constante de **otro archivo** | **falla** |
| `int lim = Integer.MAX_VALUE;` -- la misma constante, fuera de un `case` | anda |

Las dos primeras filas descartan que moleste el nombre calificado. La tercera y la cuarta usan las
dos un nombre calificado de una clase de primer nivel y solo difieren en si esa clase esta en el
mismo archivo. Y la quinta descarta que sea la resolucion del simbolo: la constante ajena se lee
bien, se pliega bien y se emite bien mientras no este en una etiqueta.

Lo que queda es esto: el plegado de constantes que corre sobre las etiquetas de `case` mira solo
los tipos de la **unidad de compilacion actual**. Para un tipo de otro archivo --este ya compilado
en el classpath o pasado en la misma invocacion, da igual: los dos fallan-- no llega al
`ConstantValue` del campo y trata la etiqueta como si no fuera constante. El arreglo es hacer que
ese plegado consulte la misma tabla de simbolos que ya usa el resto del compilador, que
evidentemente si lo resuelve.

A favor: **falla al compilar**, no en ejecucion. Es lo contrario de #460 -- ruidoso, inmediato y sin
riesgo de llegar a produccion.

Como salio: escribiendo `javax.management`. `BinaryRelQueryExp` y `BinaryOpValueExp` despachan los
nueve operadores de `Query` con `switch (relOp) { case Query.GT: ... }`, que es literalmente el
codigo del JDK, y `Query` es otro archivo.

Rodeo aplicado en las dos clases: cadenas de `if`/`else if` sobre los mismos valores, con un
comentario que apunta aca. Es equivalente y no cuesta nada en un `switch` de cinco ramas, pero
obliga a desnaturalizar el codigo cada vez que una constante publica vive en su propia clase --que
es justo el caso normal de una constante publica--.

Repro: `KajiLibrary/repros/finding_461.java` (el que falla mas los cuatro controles; el JDK 25
compila el archivo entero y `run()` devuelve `-1`).

### #462 -- una constante heredada de una **interfaz implementada** no entra al alcance

```java
interface Marcas { int DOS = 2; }

class C implements Marcas {
    int x = DOS;                 // error: no se encuentra el simbolo: DOS
    int y() { return DOS; }      // error: idem
    int z() { return Marcas.DOS; }  // anda, calificada
}
```

No compila. `DOS` es un miembro heredado de `C` --JLS 8.1.5: una clase hereda de sus
superinterfaces todos sus campos accesibles-- y por lo tanto esta en el alcance por su nombre
simple, sin calificar. Nuestro compilador solo lo encuentra calificado.

**Delimitacion.** Cuatro cortes; el que importa es el tercero contra el cuarto:

| forma | resultado |
|---|---|
| `Marcas.DOS` -- la misma constante, calificada | anda |
| `TRES` -- heredada de una **superclase**, nombre simple | anda |
| `DOS` -- heredada de una **interfaz**, nombre simple, en un cuerpo de metodo | **falla** |
| `DOS` -- heredada de una **interfaz**, nombre simple, en un inicializador de campo | **falla** |

Las dos primeras filas descartan que el simbolo no exista o no se pueda leer: la constante se
resuelve, se pliega y se emite bien. Y descartan que sea la herencia de estaticos en general, que
por `extends` anda. Las dos ultimas descartan que sea una particularidad del inicializador: falla
igual en un metodo. Da lo mismo que la interfaz este en el mismo archivo o en otro, y da lo mismo
que se implemente directa o indirectamente.

Lo que queda es esto: la construccion del alcance de una clase sube por la cadena de `extends`
pero no por la de `implements`. El arreglo es que el mismo recorrido que ya trae los estaticos de
las superclases recorra tambien las superinterfaces, transitivamente y en el orden de JLS 8.1.5.

A favor: **falla al compilar**. Igual que #461, es ruidoso e inmediato.

Como salio: escribiendo `java.io.ObjectOutputStream`. Toda la familia de la serializacion
implementa `ObjectStreamConstants` y usa `TC_NULL`, `TC_OBJECT`, `SC_WRITE_METHOD` y
`PROTOCOL_VERSION_2` por su nombre simple -- que es *el motivo por el que esa interfaz existe*, y
la forma en que esta escrito el JDK. La interfaz no aporta ningun metodo: su unico proposito es
meterle esos nombres al alcance de quien la implementa, que es exactamente lo que aca no pasa.

Rodeo aplicado en `ObjectOutputStream` y `ObjectInputStream`: cada constante calificada como
`ObjectStreamConstants.TC_NULL`. Es equivalente y verboso, y deja la clausula `implements`
cumpliendo solo con la firma publica.

Repro: `KajiLibrary/repros/finding_462.java` (los dos que fallan mas los dos controles; el JDK 25
compila el archivo entero y `run()` devuelve `-1`).

### #463 -- ✅ una comparacion **constante** usada como valor emitia codigo muerto sin frame

```java
boolean b = (1 + 1) == 3;   // VerifyError en la JVM real; anda en la nuestra
```

`java.lang.VerifyError: Expecting a stack map frame`, en el `iconst_0` que sigue al `goto`.

**Delimitacion.** Seis cortes, y el que parte el mundo en dos es el ultimo:

| forma | resultado |
|---|---|
| `boolean b = 2==2;` | **falla** |
| `boolean b = (1+1)==2;` (constante verdadera) | **falla** |
| `boolean b = (1+1)==3;` (constante falsa) | **falla** |
| `int n=(1+1); boolean b = n==2;` (no constante) | anda |
| `if (x==y) {...} else {...}` con `x`,`y` variables | anda |
| `boolean c = x&&y` con `x`,`y` variables | anda |

Las tres primeras filas descartan que importe el **valor** de la constante: falla igual verdadera
que falsa. La cuarta es la misma comparacion con un operando que el compilador no puede plegar, y
anda -- o sea que el disparador no es la comparacion sino que sea **constante**. La quinta descarta
que sea la comparacion en general: en posicion de condicion, plegada o no, siempre anduvo. Lo que
queda es la interseccion de las dos cosas: una comparacion constante **materializada como valor**.

Por que fallaba: `bool_value` emitia siempre la misma forma --`branch_if f; iconst_1; goto end;
f: iconst_0; end:`-- sin mirar si la condicion era constante. Cuando lo es, `branch_if` emite un
salto **incondicional**, y entonces una de las dos ramas no la alcanza nadie. La instruccion que
sigue a una transferencia incondicional necesita su propio frame en el `StackMapTable` (JVMS
§4.10.1), y para codigo muerto no hay frame que se pueda armar.

Por que solo se ve afuera: **nuestra VM no verifica los frames**. El `.class` cargaba y corria acá
sin quejarse; el mismo archivo en `java` del JDK 25 no pasa el verificador. Es la quinta vez en
esta sesion que un bug del codegen solo aparece corriendo el `.class` en una JVM real, y por eso
esa comprobacion ya no es opcional.

Arreglo: `const_bool(cond)` pliega la condicion antes de emitir nada --literales, `!`, `&&`/`||`
con los dos lados constantes, y las seis comparaciones-- y en ese caso se emite un solo
`iconst_0`/`iconst_1`. Sin ramas, no hay codigo muerto.

Un intento anterior, descartado: consultar `self.reachable` **despues** de `branch_if`. Arreglaba
el caso constantemente verdadero y no el falso, porque con una condicion constante falsa
`branch_if` salta incondicional y deja `self.reachable` desactualizado. Queda anotado porque
parecia funcionar: pasaba tres de los cinco repros.

Repro: `scratchpad/zz360/{D1..D5,B1..B5,C1..C6}.java`.

### #464 -- ✅ el `String.valueOf` de una concatenacion no actualizaba la pila simulada

```java
Object o = "x";
boolean b = (1 + 1) == 2;
System.out.println("a=" + o + " b=" + (b == true));   // VerifyError en la JVM real
```

```
java.lang.VerifyError: Bad type on operand stack
  Reason: Type 'java/lang/Object' (current frame, stack[1]) is not assignable to 'java/lang/String'
  Stackmap Table:
    full_frame(@21,{...},{Object[#62],Object[#25]})     // #25 = java/lang/Object
```

El descriptor del call site dice `(Ljava/lang/String;Z)Ljava/lang/String;` y el frame que nosotros
declaramos dice `Object`. El verificador usa **el frame declarado**, no el que el mismo calcula, y
por eso rechaza el `invokedynamic` aunque en la pila real haya un `String`.

**Delimitacion.** Cinco cortes:

| forma | resultado |
|---|---|
| `"a=" + o` -- un objeto solo | anda |
| `"a=" + o + " b=" + o` -- dos objetos | anda |
| `"a=" + o + " b=" + b` -- objeto **mas** un booleano de variable | anda |
| `"a=" + o + " b=" + (b==true)` -- objeto mas un booleano de una comparacion | **falla** |
| `"b=" + (b==true)` -- el booleano solo, sin el objeto | anda |

Las tres primeras descartan la concatenacion en general y descartan que moleste tener dos
operandos. La quinta descarta que el problema sea el booleano. Falla solo la cuarta, la unica que
tiene las dos cosas juntas: un operando que pasa por `String.valueOf(Object)` **y** un salto
posterior que obliga a escribir un frame con ese valor todavia en la pila.

Por que fallaba: en `ExprKind::Indy`, para un operando no-`String` se emite un
`invokestatic String.valueOf(Object)` --correcto-- pero **no se tocaba la pila simulada**. El
operando entro como `Object` y sale como `String`, y sin decirlo, todo frame que se escriba de ahi
en adelante lo declara `Object`. Mientras no haya un salto despues, no se escribe ningun frame y no
se nota: por eso hacen falta las dos condiciones a la vez.

Arreglo: `self.pop(1); self.push(VType::Object("java/lang/String"))` justo despues del
`invokestatic`.

Sobre la causa que se dio primero y estaba mal: se reporto como *"nuestro javac tipa los operandos
de la concatenacion como `Object` donde el descriptor del `invokedynamic` pide `String`"*, lo cual
describe bien el sintoma pero se atribuyo al descriptor del call site, que estaba bien. El
descriptor nunca fue el problema; el problema era el frame. Y por debajo habia **dos** bugs
distintos --este y #463-- que daban `VerifyError` en el mismo archivo con mensajes distintos, lo
que hizo que el primer repro apuntara al lugar equivocado.

Repro: `scratchpad/zz360/{B4,B5,Vt}.java`.

## Tanda: `javax.management`

### #465 ⬜ -- una variable local capturada no se ve desde el **inicializador de campo** de una clase anidada en el metodo

```java
static S mk() {
    final int x = 7;
    return new S() {
        private int y = x;               // error: no se encuentra el simbolo: x
        public int get() { return y; }
    };
}
```

```
Cap1.java:6: error: no se encuentra el símbolo: x
            private int y = x;
                            ^
  símbolo:   variable x
  ubicación: clase $1
  ¿quisiste decir 'y'?
```

El JDK real acepta las cinco formas de abajo. Nosotros rechazamos dos, y las dos tienen en comun
**donde** aparece la referencia, no que sea una captura:

| forma | resultado |
|---|---|
| clase anonima, referencia en el **cuerpo de un metodo** (`return x;`) | anda |
| clase anonima, referencia en el cuerpo **mezclada con un campo propio** (`return y + x;`) | anda |
| clase anonima, referencia en un **inicializador de campo** (`int y = x;`) | **falla** |
| clase anonima, referencia en un **bloque de inicializacion de instancia** (`{ y = x; }`) | **falla** |
| **clase local** (`class L implements S`), inicializador de campo | **falla** |

Las dos primeras descartan que el problema sea la captura: el mecanismo funciona, y funciona
incluso conviviendo con campos propios en la misma expresion. Falla exactamente cuando el sitio de
uso es un inicializador --de campo o de instancia--, y da igual que la clase sea anonima o local.

La pista fuerte es el mensaje: *"¿quisiste decir 'y'?"*. El resolvedor esta buscando `x` **solo
entre los miembros de la clase anidada** y ni siquiera intenta el alcance del metodo que la
contiene. O sea: al armar el alcance en el que se resuelven los inicializadores no se le encadena
el alcance lexico del metodo, mientras que al armar el de los cuerpos de metodo si. Es coherente
con que los inicializadores se procesen aparte --se mueven al constructor-- y con que ese camino
aparte se construya el alcance desde cero.

Es distinto de #440: alla faltaba la instancia externa implicita, aca falta el alcance lexico de
las **locales**, y la clase que falla ni siquiera necesita instancia externa (el metodo es
`static`).

**Impacto real.** Apareció en `javax/management/MBeanServerPermission.java`, en el `Enumeration`
anonimo que devuelve `elements()`: el idioma natural es `private boolean pendiente = (p != null);`.
Rodeo: mover la condicion a los cuerpos de `hasMoreElements`/`nextElement`, que es lo que quedó en
el arbol.

Repro: `/tmp/rep/{Cap1..Cap5}.java`.

### #466 -- ✅ `Method.invoke` no redespachaba: ejecutaba el cuerpo del metodo **declarado**, no el del receptor

No es del compilador sino de la maquina virtual, y no es un error de compilacion sino un panico:

```java
public interface I { int f(); }
public static class C implements I { public int f() { return 3; } }

Method m = I.class.getMethod("f");
m.invoke(new C());                      // panic en el interprete
```

```
thread 'main' panicked at src\jvm\interpreter\bytecode_interpreter.rs:2740:22:
index out of bounds: the len is 0 but the index is 0
```

La linea 2740 es `let opcode = self.current_code()[self.pc()];`. O sea: se armo un frame para el
metodo **abstracto de la interfaz**, cuyo `Code` esta vacio, y se intento leer su primer opcode.

**Delimitacion.** Cuatro cortes, y el cuarto es el que da la causa:

| `Method` obtenido de | cuerpo | resultado |
|---|---|---|
| la clase concreta (`C.class.getMethod("f")`) | hay | **3**, correcto |
| una **interfaz**, metodo abstracto | no hay | **panico** |
| una **clase abstracta**, metodo abstracto | no hay | **panico** |
| una interfaz, metodo `default`, con override en `C` | hay (el del `default`) | **1**, y tendria que ser **3** |

El cuarto es la clave: no hay panico, hay una respuesta **incorrecta y silenciosa**. Se ejecuto el
cuerpo del `default` de la interfaz teniendo delante un receptor que lo redefine. Los dos primeros
casos abstractos no son entonces un bug propio: son el mismo bug --no se redespacha-- donde ademas
resulta que no hay cuerpo que ejecutar, y el sintoma pasa de resultado equivocado a panico.

La causa esta en como se ejecuta la reflexion: `Method.invoke` invoca el `Method` que le dieron como
si fuera `invokespecial`, ligado estaticamente al metodo que lo declara, cuando la especificacion
dice que para un metodo de instancia no privado ni de constructor tiene que comportarse como
`invokevirtual`/`invokeinterface`, o sea resolver el override sobre `receiver.getClass()`.

**Impacto real.** Es un bloqueante para todo `DynamicMBean` que introspeccione una interfaz, que es
justo lo que hace `javax.management.StandardMBean`: la especificacion dice mirar **la interfaz de
administracion**, y entonces cada `getAttribute`/`invoke` termina invocando un `Method` abstracto.

Rodeo que quedo en el arbol: `StandardMBean.resolver(Method)` baja el metodo de la interfaz a
`implementacion.getClass().getMethod(nombre, tipos)` antes de invocarlo, y solo se queda con el de
la interfaz si la clase concreta no es publica. Es un rodeo aceptable porque el JDK ya lo necesita
por accesibilidad, pero **tapa el bug**: si se arregla la VM, el rodeo sigue siendo correcto y deja
de ser necesario.

Repro: `java/JmxP6.java` (clase, anda), `java/JmxP7.java` (interfaz abstracta, panico),
`java/JmxP8.java` (clase abstracta, panico), `java/JmxP9.java` (`default`, responde 1 en vez de 3).

### #465 -- ✅ `java.lang` le gana a un tipo anidado en un nombre calificado `Externa.Anidada`

**Arreglado.** La bajada al miembro-tipo del `outer` ahora va **antes** que la busqueda del ultimo
segmento entre los externos, en los **tres** lugares donde se resolvia un nombre calificado y cada
uno tenia su propia copia del orden equivocado:

| archivo | funcion | que rompia ahi |
|---|---|---|
| `src/javac/enter.rs` | `resolve_name_to_sym` | la pasada 1: el tipo declarado |
| `src/javac/attribute.rs` | `resolve_type_name` | el atributado: el tipo de un `new` |
| `src/javac/codegen.rs` | `resolve_type_id` | la emision: el `extends`, el descriptor |

Que hicieran falta los tres se vio de a uno: arreglado `enter`, `Rectangle2D.Double` seguia mal;
arreglado `attribute`, el `new` salia bien pero `GeneralPath extends Path2D.Float` seguia emitiendo
`java.lang.Float` como superclase. La prueba que lo cerro: `javap -c` sobre el `.class`, mirando a
que clase apunta el `new` -- `java/awt/geom/Rectangle2D$Double` y no `java/lang/Double`.

El fallback al nombre simple sigue existiendo en los tres, ultimo, y sigue haciendo falta: un
anidado escrito corto (`Map.Entry`) tiene nombre completo `java.util.Map$Entry`, que no coincide con
lo escrito.

Lo que destapo: `java.awt.geom` entero. Sus `Double` y `Float` anidados son la API de ese paquete, y
ninguno resolvia desde afuera. `CgeomAwtTest` no compilaba; ahora da -1 en las dos VMs y contra el
JDK. El rodeo que habia en `GeneralPath` --escribir el `extends` con el nombre completo-- ya no hace
falta, aunque se deja: nombrar entero nunca esta de mas.

**Lo que este arreglo NO cubre, y quedo sin ablar:** `java.lang.Double d = new Rectangle2D.Double(...)`
**compila**, cuando el JDK lo rechaza. El `new` ya emite la clase correcta --se verifico con
`javap -c`-- asi que no es esto: es una indulgencia aparte del chequeo de asignacion. No la persegui.

---

### El diagnostico original (queda porque la delimitacion es la que sirvio)

```java
import java.awt.geom.Point2D;
...
Point2D p = new Point2D.Double();   // error: tipo incompatible en `p`
Object  o = new Point2D.Double();   // COMPILA, y emite `new java/lang/Double`
```

Al resolver un nombre calificado `Externa.Anidada`, el compilador busca el ultimo segmento por
nombre simple en `java.lang` **antes** de buscarlo entre los miembros de `Externa`. Si `java.lang`
tiene una clase con ese nombre, gana ella. `Point2D.Double` no denota
`java.awt.geom.Point2D$Double` sino `java.lang.Double`.

Lo grave es el segundo caso: **compila limpio y construye la clase equivocada**. El pool de
constantes del `.class` de `Object o = new Point2D.Double();` no menciona a `Point2D$Double` en
ningun lado -- solo `java/lang/Double`. Si el tipo destino no acepta un `java.lang.Double` el error
aparece al compilar; si lo acepta (`Object`, `Number`, `Comparable`, un parametro generico), no
aparece nunca.

**Delimitacion.** Siete cortes. Ninguno usa `java.awt.geom` como parte esencial: el paquete de
prueba `probe` es mio y reproduce lo mismo.

| forma | resultado |
|---|---|
| `Padre p = new Padre.Hija();` (`import probe.Padre`, sin choque) | anda |
| `Caja c = new Caja.Zed();` con una clase `Zed` de primer nivel en el classpath | anda |
| `Caja2 c = new Caja2.Deriv();` con `import probe.Deriv;` en el mismo archivo | **anda** |
| `Caja c = new Caja.Double();` (choque con `java.lang.Double`) | **falla** |
| `Caja2 c = new Caja2.Thread();` (choque con `java.lang.Thread`) | **falla** |
| `probe.Caja.Double` / `java.awt.geom.Point2D.Double` -- calificado completo | anda |
| `Object o = new Point2D.Double();` | **compila mal en silencio** |

Las dos primeras filas descartan que moleste el nombre calificado o los tipos anidados en general.
La tercera es la que sorprende y la que delimita: un `import` de tipo unico que trae `Deriv` al
alcance del archivo **no** le gana al miembro `Caja2.Deriv` -- o sea que no es "cualquier nombre
simple en alcance gana", como decia la nota que hasta ahora estaba en `Point2D.java`. La cuarta y
la quinta muestran que el que gana es especificamente el import implicito de `java.lang`, y con dos
nombres distintos, asi que no es una particularidad de `Double`. La sexta descarta la resolucion
del tipo anidado en si: por su nombre completo se encuentra y se instancia bien.

Lo que queda es esto: el paso que resuelve `A.B` como tipo consulta `java.lang` en la resolucion
del segmento `B` antes de mirar los miembros de `A`. El orden correcto (JLS 6.5.5.2) es al reves:
`B` en `A.B` es **solo** un miembro de `A`; `java.lang` no participa. El arreglo es que la
resolucion de un nombre calificado de tipo no caiga al alcance global para el ultimo segmento.

Cuanto duele: `java.awt.geom` es el caso extremo. Sus veinte clases anidadas se llaman `Double` y
`Float` --`Point2D.Double`, `Rectangle2D.Float`, `Line2D.Double`, `Arc2D.Float`, `Path2D.Double`...--
que son justo los dos nombres de `java.lang` con los que se choca. Desde afuera del paquete, la
forma normal de usar el paquete entero esta rota.

Rodeo en `java.awt.geom`: las fabricas internas `Point2D.newDouble`, `Rectangle2D.newDouble`,
`Path2D.newDouble`, que construyen la anidada desde el cuerpo de la clase externa (ahi `Double` a
secas ya es la anidada y no hay choque). Para el codigo de afuera el rodeo es el nombre completo
`java.awt.geom.Point2D.Double`, que anda.

Nota: la nota que estaba en el encabezado de `Point2D.java` daba otra causa --"el javac mantiene un
solo mapa nombre-simple -> tipo por archivo, y un archivo que nombre a la vez `Point2D.Double` y
`java.lang.Double` resuelve mal uno de los dos"--. Es falsa: los repros de arriba no nombran
`java.lang.Double` en ningun lado y fallan igual, y el caso del `import probe.Deriv` demuestra que
tener dos nombres iguales vivos en el archivo no alcanza para romperlo. Esa nota quedo corregida.

Repro: `scratchpad/zz319b/{S1..S6,U1..U6,T1}.java` mas `scratchpad/zz319b/probe/`, y
`scratchpad/zz319/{R1..R11}.java`. `T1.java` es el silencioso: compila y `strings` sobre el
`.class` muestra `java/lang/Double` y ni rastro de `Point2D`.

## Tanda: `org.xml.sax.ext`, y los dos bugs que estaban tapados por un `javap` mentiroso

El paquete `org.xml.sax.ext` entro entero sin tropezar con nada: los ocho tipos compilaron a la
primera. Los dos hallazgos de abajo salieron de **verificar** lo que ya estaba, no de escribirlo.

### Antes que nada: `javap -cp` miente sobre cualquier paquete que este en un modulo del JDK

No es un bug nuestro, es una trampa del metodo de verificacion, y conviene que quede escrita porque
casi me hace reportar un desmentido falso.

```
javap -cp scratchpad/repro org.xml.sax.helpers.DefaultHandler   # lee el java.xml del JDK
javap        scratchpad/repro/org/xml/sax/helpers/DefaultHandler.class   # lee el nuestro
```

`org.xml.sax`, `org.w3c.dom`, `javax.xml.*` y todo lo que vive en un modulo de la plataforma le
ganan al classpath. Pedir la clase **por nombre** con `-cp` devuelve la del JDK, con sus supertipos
correctos, sin ningun aviso. Con eso yo "comprobe" cuatro veces seguidas que un bug no reproducia,
cuando reproducia siempre. **Para verificar un `.class` nuestro de uno de esos paquetes hay que
pasarle a `javap` la ruta del archivo**, o cargarlo con `--patch-module` (ver mas abajo).

### #466 -- un `import` de un solo tipo pierde contra una clase del classpath cuando el fuente del tipo importado esta en la misma invocacion

Esto es el bug que el comentario de `KajiLibrary/org/xml/sax/helpers/DefaultHandler.java` ya
describia y que **no estaba en este informe**. Queda confirmado y ablado.

```java
// org/xml/sax/helpers/DefaultHandler.java
import org.xml.sax.ContentHandler;          // import explicito, de un solo tipo
public class DefaultHandler
        implements EntityResolver, DTDHandler, ContentHandler, ErrorHandler { ... }
```

```
bin/javac.exe --emit -cp KajiLibrary org/xml/sax/ContentHandler.java \
                                     org/xml/sax/helpers/DefaultHandler.java
javap org/xml/sax/helpers/DefaultHandler.class
  -> implements ...,java.net.ContentHandler,...      # deberia ser org.xml.sax.ContentHandler
```

`java.net.ContentHandler` existe en el arbol (`KajiLibrary/java/net/ContentHandler.class`) y es una
**clase abstracta**, no la interfaz. El `.class` sale diciendo que implementa otra cosa: compila,
mide bien, y despues ningun parser acepta la clase como manejador.

**Ablacion del disparador.** Lo unico que se movio entre las dos corridas es si el *fuente* de
`ContentHandler.java` entra o no en la invocacion:

| invocacion | resultado |
|---|---|
| `DefaultHandler.java` **solo** (con `org/xml/sax/ContentHandler.class` en el classpath) | `org.xml.sax.ContentHandler` -- bien |
| `ContentHandler.java` + `DefaultHandler.java` | `java.net.ContentHandler` -- **mal** |
| las dos, orden invertido | `java.net.ContentHandler` -- **mal** |
| las ~27 fuentes de `sax`+`helpers`+`ext` juntas | `java.net.ContentHandler` -- **mal** |

Sacado el fuente de la invocacion, el sintoma desaparece; puesto, vuelve. El orden de los argumentos
no cambia nada. Es decir: tener el tipo **como fuente en la tanda** es lo que lo saca de la mesa
como candidato del `import`, y gana el homonimo del classpath.

Emparentado con #351 y #352, pero no es ninguno de los dos: aca el homonimo que gana **no** esta en
el paquete propio (`java.net` contra `org.xml.sax.helpers`) y el import es de un solo tipo, no
on-demand.

**Rodeo, y por eso esta asi hoy:** el nombre calificado en la clausula `implements`. Es lo que tiene
`DefaultHandler.java` en el arbol, y por eso mide y anda bien.

Repro: `scratchpad/abl1/` (el caso que anda) y `scratchpad/saxrepro/` (el que falla).

### #467 -- la clausula `implements` no comprueba que el tipo sea una interfaz

Este es el que hace que #466 sea **silencioso** en vez de un error de compilacion, y es mas general:
no necesita ninguna ambiguedad de nombres.

```java
// pkgc/Foo.java
package pkgc;
public class Foo { }        // una CLASE

// pkgd/Baz.java
package pkgd;
public class Baz implements pkgc.Foo { }    // nombre totalmente calificado, sin ambiguedad
```

Nuestro javac lo acepta y emite `Baz` con `pkgc.Foo` en la lista de interfaces:

```
javap pkgd/Baz.class  ->  public class pkgd.Baz implements pkgc.Foo {
```

El javac real lo rechaza:

```
Baz.java:2: error: interface expected here
public class Baz implements pkgc.Foo { }
```

**Y corre en las dos VMs de forma distinta**, que es la parte que importa:

```
bin/run-headless.exe pkgd/Uso.class run          -> Some(Int(-1))          # anda
java -cp ... Corre pkgd.Uso
  -> java.lang.IncompatibleClassChangeError: class pkgd.Baz can not implement
     pkgc.Foo, because it is not an interface
```

Nuestra VM no mira el flag `ACC_INTERFACE` de los supertipos al cargar, asi que se lo traga; la JVM
real lo rechaza en `defineClass`. Es exactamente la familia de bugs que solo se ve corriendo en las
dos.

No lo arregle: no me bloqueaba --el rodeo del nombre calificado alcanza-- y tocar la resolucion de
nombres o el chequeo de supertipos en `src/` no es un cambio chico. Queda con el repro completo.

Repro: `scratchpad/abl2/` (`pkgc/Foo.java`, `pkgd/Baz.java`, `pkgd/Uso.java`).

### #467 -- ✅ una declaracion `@interface` se emitia **sin** sus meta-anotaciones, y eso borraba `@Retention`

```java
// Ann.java, compilado solo
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Ann { boolean value() default true; }
```

Nuestro `Ann.class` **no tiene atributo `RuntimeVisibleAnnotations`**; el del JDK real si:

```
RuntimeVisibleAnnotations:
  0: #14()    java.lang.annotation.Documented
  1: #15(...) java.lang.annotation.Retention(RUNTIME)
  2: #18(...) java.lang.annotation.Target(TYPE)
```

El `AnnotationDefault` del metodo `value()` si se emite. O sea que no es que las anotaciones no se
escriban en general: es que las que van **sobre la declaracion del tipo de anotacion** se pierden.

**El daño no esta ahi, esta en la unidad siguiente.** Un `@interface` sin `@Retention` legible vale
como `CLASS` --es el valor por omision de la especificacion--. Entonces, cuando se compila **otra**
unidad que usa esa anotacion y la resuelve leyendo el `.class`, javac concluye que la retencion es
`CLASS` y **no** emite la anotacion en la clase del usuario. El resultado en ejecucion es
`getAnnotation(...) == null`, sin ningun error en ningun paso.

**Delimitacion.** El corte deja ver por que el bug se esconde tan bien:

| la anotacion se declara | resultado de `getAnnotation` |
|---|---|
| en **la misma** unidad de compilacion que la usa | **anda** (`value()` correcto, y el `default` tambien) |
| en **otra** unidad, resuelta desde el `.class` | **null** |

En el primer caso javac tiene la retencion en el AST y nunca consulta el classfile, asi que todo
funciona; en el segundo tiene que leerla del `.class`, donde no esta. Cualquier prueba de
anotaciones escrita en un solo archivo pasa, y por eso esto no se detecto antes.

**Impacto real.** `javax.management.JMX.isMXBeanInterface` mira `@MXBean`, que es una anotacion de
biblioteca: sobre esta VM devuelve siempre lo que dice la convencion del sufijo, porque la anotacion
del usuario nunca llega al classfile. Es un caso perdido desde `javax.management`: el descarte pasa
al compilar el codigo del usuario, no adentro de la biblioteca, asi que no hay rodeo posible del
lado de la biblioteca. La implementacion quedo escrita como corresponde --lee la anotacion primero--
y la prueba `java/JmxTest.java` **retiro** el caso 92, que era exactamente ese, con la nota
correspondiente.

Repro: `scratchpad/anno/{Ann,UsaAnn}.java` (dos unidades), y `java/JmxP13.java` (falla, con
`@MXBean` de biblioteca) contra `java/JmxP14.java` (anda, con la anotacion en el mismo archivo).

### #468 ⬜ -- las vistas de `LinkedHashMap` no conservan el orden, y eso rompe `ObjectName.toString()`

No es del compilador ni de la VM: es una **divergencia declarada** en
`KajiLibrary/java/util/LinkedHashMap.java`, y se anota aca porque el costo aparece lejos de donde se
tomo la decision.

```java
Map<String,String> m = new LinkedHashMap<String,String>();
m.put("z","1"); m.put("a","2"); m.put("m","3");
for (String v : m.values()) ...     // da 2,1,3 -- deberia dar 1,2,3
m.keySet()                          // da a,z,m -- deberia dar z,a,m
```

La causa esta a la vista en el propio archivo: `keySet()` recorre la lista de orden como
corresponde, pero **vuelca el resultado en un `HashSet`**, que no lo conserva; y `values()` y
`entrySet()` se construyen a partir de `keySet()`, asi que heredan la perdida. El comentario del
archivo lo declara ("Divergencia: el JDK devuelve una vista ordenada por insercion; este devuelve un
HashSet, que no conserva ese orden").

**Por que importa mas de lo que parece.** Un `LinkedHashMap` no se elige por lo que hace sino por el
orden: si el orden no se conserva, la clase entera es un `HashMap` con mas costo. Y el que la usa no
tiene forma de enterarse, porque el orden **casi** se conserva --con pocas claves la tabla suele
devolverlas en un orden parecido-- y falla de a poco.

**Impacto real.** `javax.management.ObjectName` guardaba las claves en un `LinkedHashMap` para poder
cumplir su contrato: `getCanonicalName()` devuelve las claves ordenadas y `toString()` **el nombre
tal como se escribio**. Con la divergencia, `new ObjectName("d:z=1,a=2,m=3").toString()` devolvia
`d:a=2,z=1,m=3`. Arreglado del lado de `ObjectName`, que ahora guarda una `List` y detecta claves
repetidas con un recorrido --las listas de claves de un MBean son de unos pocos elementos--.

Repro: `java/JmxP12.java`.

### #468 -- con `--emit`, un tipo del classpath nombrado con nombre completo se resuelve distinto segun la POSICION del archivo en la linea de comandos

Salio al reconstruir `org.xml.sax.helpers` entero. El paquete compila archivo por archivo y falla en
lote, con un error que no tiene sentido leido solo:

```
bin/javac.exe --emit -cp KajiLibrary KajiLibrary/org/xml/sax/helpers/*.java
  ParserAdapter.java:216: error: asignación de tipo incompatible
          contentHandler = handler;
```

El campo y el parametro estan declarados con **el mismo nombre completo**:

```java
org.xml.sax.ContentHandler contentHandler = null;                 // linea 87
public void setContentHandler(org.xml.sax.ContentHandler handler) {
    contentHandler = handler;                                     // linea 216 -- "incompatible"
}
```

Para que el compilador los considere incompatibles tiene que estar resolviendo las dos apariciones
del mismo nombre calificado a dos tipos distintos.

**Tres ablaciones, una variable por vez.**

| lo unico que se mueve | resultado |
|---|---|
| `... ParserAdapter.java LocatorImpl.java` (ParserAdapter **primero**) | compila |
| `... LocatorImpl.java ParserAdapter.java` (ParserAdapter **segundo**) | **falla** |
| el mismo lote segundo, pero con `--check` en vez de `--emit` | compila |
| el mismo lote segundo, agregando `org/xml/sax/ContentHandler.java` al lote | compila |

O sea que hacen falta las tres cosas a la vez: `--emit`, el archivo **no** en primera posicion, y el
fuente de `ContentHandler.java` **fuera** del lote. Sacando cualquiera de las tres el sintoma
desaparece. Con `--check` no pasa nunca, que es la razon por la que se puede "validar" el paquete
entero y creer que esta bien: **`--check` no es un sustituto de `--emit` ni para detectar esto**.

No lo achique mas alla de estos archivos ni mire `src/`, asi que **no tengo la causa**; lo de arriba
es el disparador, no la explicacion. La sospecha razonable, sin comprobar, es que con `--emit` el
tipo se resuelve una vez al armar el esqueleto de la clase y otra al generar el cuerpo, y que entre
las dos cambia lo que hay en la tabla de tipos --el `.class` de un archivo anterior del lote ya
fue escrito-- pero no la ablacie y puede ser falso.

**Rodeo, y es el que uso el build:** listar el archivo afectado primero, o compilar el paquete de a
un archivo. Con eso `ParserAdapter.class` sale correcto (verificado contra el JDK: mismos
supertipos, mismo descriptor de `setContentHandler`).

## Tanda: `java.awt` (parte de datos: color, geometria entera, constantes de disposicion)

### #469 ⬜ -- los campos de una interfaz **no se heredan**: ni al que la implementa ni a una subinterfaz

Salio escribiendo `java.awt.Polygon`, cuyo `PathIterator` interno devuelve `WIND_EVEN_ODD` y
`SEG_MOVETO` sin calificar, que es como estan escritos los otros iteradores del JDK.

```java
interface ConIface {
    int VALOR = 7;
}

class DirectaIface implements ConIface {
    public int f() { return VALOR; }   // error: no se encuentra el simbolo: VALOR
}

interface SubIface extends ConIface { }
// SubIface.VALOR   ->   error: no se encuentra el campo: VALOR
```

```
javac: java/IfaceConst.java:7: error: no se encuentra el simbolo: VALOR
    public int f() { return VALOR; }
                            ^
  simbolo:   variable VALOR
  ubicacion: clase DirectaIface
```

**Que si anda, para acotarlo.** No es "no se heredan campos": la herencia por `extends` de una
**clase** funciona bien, tanto para los estaticos como para los de instancia.

| forma | resultado |
|---|---|
| `class Sub extends Base` y usar `Base.CAMPO_ESTATICO` sin calificar | compila |
| `class Sub extends Base` y usar un campo de instancia heredado | compila |
| `class C implements I` y usar `I.VALOR` **calificado** | compila |
| `class C implements I` y usar `VALOR` **sin calificar** | **falla** |
| `interface S extends I` y usar `S.VALOR` | **falla** |

O sea que lo que falta es meter los campos de las interfaces implementadas --y de las
superinterfaces-- en el conjunto de miembros heredados. Los de las superclases si se meten. Que
`S.VALOR` tambien falle dice que el problema no esta en la resolucion de nombres simples sino en la
construccion de la tabla de miembros del tipo: por eso falla igual con el nombre calificado.

**Por que importa mas de lo que parece.** Es el patron de todas las implementaciones de
`PathIterator`, `Transparency`, `Paint` y compania: la interfaz declara las constantes y las clases
que la implementan las usan a secas. Cualquier reimplementacion fiel del JDK lo va a pisar.

**Rodeo:** calificar siempre con el nombre de la interfaz que **declara** la constante --
`PathIterator.WIND_EVEN_ODD`, no `SubInterfaz.WIND_EVEN_ODD`, que tampoco anda--. Es lo que hace
`java/awt/Polygon.java`.

### Nota sobre un sintoma que resulto ser cascada

Antes de achicarlo, el mismo `Polygon.java` daba en un lote multi-archivo:

```
error: no se encuentra el metodo: moveTo
    camino.moveTo(xpoints[0], ypoints[0]);
  simbolo: metodo moveTo(int, int)
  ubicacion: clase Double
```

que parece un problema de ensanchamiento `int` -> `double`. **No lo es**: compilando el archivo
solo, ese error no aparece y sale el de arriba. El ensanchamiento anda en todos los casos que
probe (metodo estatico, metodo de instancia, clase anidada, elemento de arreglo, constructor de dos
argumentos de `Path2D.Double`). Queda anotado porque el mensaje manda a mirar el lugar equivocado:
**ante un error raro en un lote, recompilar el archivo solo antes de creerle al mensaje.**

## Tanda: `java.beans` -- XMLDecoder, y el bug que solo se ve en la JVM real

### Finding #470 ⬜ -- un cast a un tipo **arreglo** no emite `checkcast`, y la JVM real rechaza la clase

Nuestra VM no verifica los stack maps, asi que el .class corre igual aca y explota alla. Salio
escribiendo `java/BeansTest.java`: un `(String[]) arr[0]` seguido de `.length`.

```
java.lang.VerifyError: Bad type on operand stack in arraylength
  Reason: Invalid type: 'java/lang/Object' (current frame, stack[0])
```

**Repro minimo** (`scratchpad/zzbeans/CastArr.java`):

```java
public class CastArr {
  public static int run() {
    Object[] caja = new Object[] { new String[] { "a", "b" }, "x" };
    String[] s = (String[]) caja[0];
    return s.length;
  }
}
```

`bin/javac.exe --emit -cp KajiLibrary scratchpad/zzbeans/CastArr.java` compila y escribe el
`.class`; `bin/run-headless.exe scratchpad/zzbeans/CastArr.class run` da 2; y
`java.exe -cp "KajiLibrary;scratchpad/zzbeans;scratchpad/zz319" Corre CastArr` da VerifyError.

**Causa, ablada.** Se saco el unico disparador sospechado --que el tipo destino del cast sea un
arreglo-- y el sintoma desaparece. El par de archivos es identico salvo en eso:

| fuente | cast | `checkcast` emitido | JVM real |
|---|---|---|---|
| `CastObj.java` | `(String) caja[0]` | si (`checkcast #34`) | anda, da 4 |
| `CastArr.java` | `(String[]) caja[0]` | **no, cero** | VerifyError |

Y no depende de la forma en que llega el valor: `CastArr2.java` castea desde un `Object` que
devuelve un metodo, y `CastArr3.java` castea la llamada en el lugar (`((int[]) dame()).length`).
Los tres emiten **cero** `checkcast` y los tres fallan igual. Lo unico comun es el tipo destino.

El desensamblado del repro es literalmente `aload_0 / iconst_0 / aaload / astore_1 / aload_1 /
arraylength`: entre el `aaload` y el `astore_1` deberia ir el `checkcast [Ljava/lang/String;` y no
va ninguno. La `LocalVariableTable` **si** dice `[Ljava/lang/String;`, asi que el frontend tipa
bien y lo que falta es la emision: el emisor de casts debe estar mirando solo el caso "tipo destino
es una clase" y saliendo sin hacer nada cuando es un arreglo.

**Alcance.** Pega en todo cast a arreglo, que es el patron de cualquier API que devuelve `Object`
--`XMLDecoder.readObject()`, `Array.newInstance`, `List.toArray(new T[0])` guardado en `Object`,
`Map.get`--. Lo que se emite compila, mide bien y **corre bien en nuestra VM**; solo la JVM real lo
rechaza, que es exactamente la clase de bug que la verificacion cruzada esta para encontrar.

**Rodeo mientras tanto:** no escribir el cast. Si el valor viene de un `Object`, sacarle lo que se
necesite por un metodo que ya devuelva el tipo arreglo (`bean.getNicknames().length` anda: ahi el
tipo lo da el descriptor del metodo y no hay cast), o por `getClass()`. Es lo que hace
`java/BeansTest.java` en la prueba de `<array>`. `java.lang.reflect.Array.getLength/get` **no**
sirven de rodeo en este arbol: son `native` sin implementacion registrada.

### Finding #471 ⬜ -- los miembros de un supertipo **crudo** no se erasan, y por eso no se lo puede implementar como manda el JLS

JLS 4.8: los miembros de un tipo crudo son las **erasuras** de los miembros de la declaracion
generica. Nuestro javac los deja parametrizados, asi que una clase que implementa un supertipo
crudo no puede escribir sus metodos ni de la forma erasada ni de la parametrizada.

**Repro y ablacion.** Los dos archivos son identicos salvo en la clausula `implements`, que es la
unica variable:

```java
// scratchpad/zzbeans/RawA.java  -- crudo
public class RawA implements Collection {
  public Iterator<Object> iterator(){ ... }
  ...
}
// scratchpad/zzbeans/RawB.java  -- parametrizado
public class RawB implements Collection<Object> {
  public Iterator<Object> iterator(){ ... }
  ...
}
```

| clausula | resultado |
|---|---|
| `implements Collection<Object>` | compila |
| `implements Collection` | `error: el retorno de 'iterator' no es compatible con el de 'Iterable': Iterator no es un subtipo de Iterator` |

El mensaje se delata solo: dice "Iterator no es un subtipo de Iterator". Lo que compara son
`Iterator` (crudo, lo que declara la clase) contra `Iterator<E>` sin erasar (lo que saco del
supertipo crudo), y como no unifica, falla.

El mismo tipo crudo rompe la otra mitad: con `implements Collection` crudo,
`public Object[] toArray(Object[] a)` --que es como el JDK escribe ese metodo en
`BeanContextSupport`-- da `error: Object[] no es un subtipo de T[]`.

**Por que importa.** `java.beans.beancontext.BeanContext extends ... java.util.Collection` es
crudo en el JDK, asi que toda clase que implemente BeanContext cae aca. Es tambien el patron de
cualquier API vieja anterior a los genericos.

**Rodeo, y no cuesta nada porque las erasuras coinciden:** declarar el metodo crudo
(`public Iterator iterator()`) y el otro generico (`public <T> T[] toArray(T[] a)`). Los dos
erasan exactamente a lo que el JDK declara --`Iterator iterator()` y
`Object[] toArray(Object[])`-- asi que el `.class` que sale es el correcto y `medir.py` lo cuenta
bien. Es lo que hace `KajiLibrary/java/beans/beancontext/BeanContextSupport.java`.

### #470 ⬜ -- una clase anonima declarada **adentro** de otra clase anonima no se puede emitir

**Sintoma.** `error: el generador de bytecode todavia no soporta una clase anonima (necesita una
clase sintetica anidada)`.

Lo que confunde es que las clases anonimas **si** andan: la de un nivel se emite bien y se usa por
todo el arbol. Lo que no anda es la del segundo nivel, y el mensaje no dice que el problema es el
anidamiento.

**Repro** (`scratchpad/rep469/R469.java`, tres lineas de sustancia):

```java
public class R469 {
    interface F { String f(); }

    static String correr(F f) { return f.f(); }

    public static void main(String[] a) {
        String r = correr(new F() {              // esta se emite bien
            public String f() {
                return correr(new F() {          // esta rompe
                    public String f() { return "adentro"; }
                });
            }
        });
        System.out.println(r);
    }
}
```

| forma | nuestro javac | JDK 25 |
|---|---|---|
| una anonima sola | compila | compila |
| una anonima adentro del **metodo** de otra anonima | **error** | compila, imprime `adentro` |

**Donde aparece.** Cualquier prueba o codigo que anide dos callbacks. Lo encontre escribiendo
`java/AuthTest.java`, donde el caso natural para `Subject.callAs` anidado --que es justamente el
comportamiento que hay que probar: el de adentro tapa al de afuera y al salir vuelve el de afuera--
pide un `Callable` adentro de otro `Callable`.

**Rodeo.** Sacar la de adentro a una clase anidada con nombre (`static class X implements F`), que
es lo que hace `java/AuthTest.java`. No cuesta nada de semantica; solo deja de leerse en linea.

**Nota de numeracion.** Este archivo tiene los numeros #465 a #468 **repetidos**: dos sesiones
distintas escribieron findings en paralelo y ninguna vio la del otro. Los textos son todos validos
y distintos entre si; lo unico que colisiona es el numero. Al citarlos conviene nombrar el titulo y
no solo el numero.

### #471 -- ✅ un `wait(ms)` vencido dejaba al hilo **adentro** del conjunto de espera del monitor

**Sintoma, y por que costo encontrarlo.** Dos sintomas distintos, los dos silenciosos:

1. `run()` devolvia `None` sin excepcion, sin panico y en 70 ms. Es el
   `Step::Return(None)` de `bytecode_interpreter.rs:1302` --"nothing left to run"--: un abrazo
   mortal de verdad, no un cuelgue.
2. En otras corridas, `panicked at bytecode_interpreter.rs:1167: no frame on the call stack`.

Ninguno de los dos nombra a `wait`, y el segundo apunta a `frame()`, que no tiene nada que ver.

**La causa.** `expire_timed_block` saca al hilo del conjunto de espera del monitor **solo si su
estado es `Waiting`**:

```rust
if self.shared.threads[idx].status == ThreadStatus::Waiting {   // <-- nunca es este
    if let Some((obj, _)) = self.shared.threads[idx].wait_reacquire {
        if let Some(mon) = self.shared.monitors.get_mut(&obj) { mon.waiting.retain(...); }
    }
}
self.make_runnable(idx);
```

Pero un `wait(ms)` se estaciona como **`TimedWaiting`**, que es justamente el unico estado en el
que esta funcion se llama. O sea que la rama no corria nunca, y el comentario de `monitor_wait`
--"`expire_timed_block` then pulls the thread out of the wait-set"-- describia algo que el codigo
no hacia.

La consecuencia se acumula: el hilo se despierta por vencimiento **sin salir del conjunto de
espera**, vuelve a llamar a `wait` y `mon.waiting.push(current)` lo mete **de nuevo**. Cada
vencimiento agrega un duplicado. Un `notifyAll()` despues mueve esos duplicados al conjunto de
bloqueados, y el que de verdad esperaba el monitor se queda afuera: el escritor no podia ni entrar
al metodo `synchronized`.

**Repro.** `java/Tubo2.java`: una `PipedOutputStream.write(byte[], 0, n)` contra una
`PipedInputStream.read(byte[], int, int)` en dos hilos.

| n | antes | despues |
|---|---|---|
| 10 | anda | anda |
| 1000, 1024, 1025, 3000 | `run()I -> None` (abrazo mortal) | anda |

El 10 andaba porque el escritor alcanzaba a entrar antes del primer vencimiento. Y ojo con el
plazo: en el motor verde `wait(1000)` son **mil opcodes**, no un segundo, asi que vence enseguida.

**El arreglo** es la condicion correcta: `matches!(status, Waiting | TimedWaiting)`.

### #472 -- ✅ un plazo vencido resucitaba un hilo **ya terminado**, que no tiene marcos

Sale del mismo lugar y es el que producia el panico `no frame on the call stack`.

Un `wait(ms)` que termina **por aviso** y no por vencimiento dejaba su `sleep_until` armado. El
hilo seguia, terminaba, y mas tarde `wake_sleepers` veia ese plazo viejo y llamaba a
`expire_timed_block`, que hacia `make_runnable` sobre un hilo `Terminated`. El planificador
cambiaba a el y `frame()` --`self.running.frames.last().expect("no frame on the call stack")`--
reventaba.

Que reventara o no dependia de cuantos opcodes corriera el programa despues: por eso dos programas
casi identicos (`java/Mon4.java` y `java/Mon7.java`) daban resultados distintos, y por eso
`wait(100)` no fallaba y `wait(300)` si --con 100 el plazo vencia **antes** del aviso, que es el
camino que si limpiaba--.

**Dos arreglos, los dos necesarios:**

1. Al retomar el monitor despues de un `wait` (`wait_reacquire = None`), limpiar tambien
   `sleep_until`: la espera se acabo, su plazo esta gastado.
2. En `expire_timed_block`, no reprogramar un hilo `Terminated`. Limpiar el plazo es todo el
   trabajo que queda ahi.

**Repro.** `java/Mon4.java` --un hilo secundario con `notifyAll(); wait(1000);` en un bucle y el
principal avisando a los 200 ms-- panicaba en las tres corridas de tres; despues de los dos
arreglos da `-1` en las tres.

**Lo que destapo.** Con los dos arreglados, `java/IoKajiTest.java` --las cuatro clases `Piped*` de
`java.io`-- pasa en las dos VMs. Antes moria sin decir nada en la seccion de tuberias.

### #473 ⬜ -- un local puede redeclarar un **parametro** del metodo, y gana el local

**Sintoma.** No hay sintoma en la compilacion: compila. El programa hace otra cosa, y en silencio.

**La regla.** JLS 6.4: el alcance de un parametro es **todo el cuerpo** del metodo, asi que un
local con el mismo nombre no lo tapa -- lo redeclara, y eso es un error de compilacion. El JDK dice
`variable n is already defined in method f(int)`.

**Repro** (`scratchpad/rep473/R473.java`):

```java
public class R473 {
    static int f(int n) {
        int n = 5;      // el JDK lo rechaza; nosotros lo aceptamos
        return n;
    }
    public static void main(String[] a) { System.out.println(f(1)); }
}
```

| | resultado |
|---|---|
| nuestro javac | `javac: escrito R473.class` |
| JDK 25 | `error: variable n is already defined in method f(int)` |

**Por que importa, con un caso real.** Lo encontre renombrando identificadores al ingles en
`X509CRLSelector.setIssuerNames(Collection<?> names)`: el renombre dejo un local `names` adentro de
un metodo cuyo parametro ya se llamaba `names`. Compilo sin una palabra, y el `for` recorrio el
conjunto **vacio** que acababa de declarar en vez del argumento. El selector quedaba sin criterio de
emisor -- o sea aceptando CRLs de cualquier CA -- y solo lo delato una prueba de comportamiento
(`java/CertX500Test.java`, caso 64).

Ese es exactamente el error que el chequeo del JLS existe para evitar, y es el peor de los dos
mundos: el codigo dice una cosa y hace otra.

**Donde mirar.** El alcance se arma al entrar al metodo; el parametro tendria que estar ya en la
tabla de locales antes de procesar el cuerpo, y la declaracion tendria que chocar con el como choca
con otro local del mismo bloque.

### #474 -- un `import` de un solo tipo pierde segun QUE OTROS archivos entren en la invocacion

```java
// jdk/internal/classfile/impl/Annotations.java
import java.lang.classfile.Annotation;      // el import explicito

public static Annotation annotation(Utf8Entry className, List<AnnotationElement> elements) { ... }
```

Compilado **solo**, el `.class` dice lo correcto:

```
public static java.lang.classfile.Annotation annotation(...)
```

Compilado **en el mismo lote que los demas archivos de su paquete**, el mismo archivo, con el mismo
classpath, emite:

```
public static java.lang.annotation.Annotation annotation(...)
```

**Delimitacion.** Una sola variable, y es el lote:

| forma | resultado |
|---|---|
| `javac --emit Annotations.java` | `java.lang.classfile.Annotation` -- correcto |
| `javac --emit impl/*.java` (mismo archivo adentro) | `java.lang.annotation.Annotation` -- **mal** |

Nada mas cambia: mismo fuente, mismo `-cp`, mismo import explicito. `java/lang/classfile/Annotation.java`
**no** estaba en ninguno de los dos lotes --se lee del `.class` del classpath en los dos casos-- asi
que esto no es #466, donde el disparador era que el fuente del tipo importado entrara en la
invocacion. Aca el disparador es la presencia de *otros* archivos.

Lo que se puede decir con lo ablado: el alcance de nombres simples se comparte entre las unidades de
compilacion de una misma invocacion, y un tipo que otra unidad trajo puede ganarle al `import`
explicito de esta. **La causa exacta no la able**: no probe cual de los otros archivos es el que
introduce `java.lang.annotation.Annotation` en el alcance, ni si hace falta que alguno lo importe o
alcanza con que lo mencione. Lo digo asi en vez de completar la explicacion.

Es la version peligrosa de #330 y de #468, y peligrosa por una razon concreta: **no falla al
compilar**. El `.class` sale con un descriptor equivocado y el error aparece mucho despues, en otro
archivo que consume ese metodo, con un mensaje que no apunta ni de lejos al lugar del problema.

Como salio: escribiendo `AttributeReader`. Un `readAnnotation` que devolvia `Annotation` no tipaba
contra `Annotations.annotation(...)`, con el mensaje "tipo de retorno incompatible" y las dos partes
diciendo `Annotation` en el fuente.

Rodeo aplicado: `Annotations.java` se compila **solo**, y el resto del paquete en lote aparte. Vale
la pena revisar el arbol despues de un lote grande: `javap -p` sobre los `.class` y buscar
`java.lang.annotation.Annotation` donde no corresponde encuentra el sintoma en un segundo.

### #475 -- los miembros de un supertipo **crudo** no vienen borrados

```java
public interface R extends java.util.Collection { }   // cruda, sin argumento de tipo

public class RImpl implements R {
    public Object[] toArray(Object[] a) { return a; }  // error acá; el JDK 25 lo acepta
    ...
}
```

```
error: el retorno de `toArray` no es compatible con el de `Collection`:
       Object[] no es un subtipo de T[]
```

Con un supertipo crudo, JLS 4.8 dice que los miembros heredados son los **borrados**: `toArray(T[])`
se ve como `Object[] toArray(Object[])`, y eso es exactamente lo que la clase escribe. Nuestro javac
sigue viendo la firma generica y exige `T[]`.

**Delimitacion.** Tres cortes:

| forma | resultado |
|---|---|
| `implements R` (R extiende `Collection` cruda) + `Object[] toArray(Object[])` | **falla** |
| `implements R` + `<T> T[] toArray(T[])` | anda |
| `implements R2` (R2 extiende `Collection<Object>`) + `Object[] toArray(Object[])` | falla |

La segunda fila descarta que el problema sea la clase o el metodo: el mismo archivo, cambiando solo
la forma de esa firma, compila. La primera contra la tercera muestra que el borrado por crudeza no
se esta aplicando --si se aplicara, la primera andaria--.

Repro: `KajiLibrary/repros/finding_475/` (`R.java` + `RImpl.java`; el JDK 25 compila los dos sin un
aviso).

Como salio: escribiendo `java.beans.beancontext.BeanContextSupport`. El JDK declara
`BeanContext extends Collection` **cruda** --el paquete es de 1998 y nunca se genero-- y por eso su
`BeanContextSupport` tiene `public Object[] toArray(Object[])` a secas.

Rodeo aplicado: se declaro `public <T> T[] toArray(T[])`. Tiene el **mismo borrado**
--`Object[] toArray(Object[])`-- asi que la firma que queda en el `.class` es la del JDK y el medidor
no ve diferencia; lo que cambia es la firma generica, que acepta un poco mas que la de alla. Queda
dicho en el javadoc del metodo.

### #476 -- ✅ un cast a un tipo **arreglo** no emitia `checkcast`

```java
static int run(Object o) {
    Object[] k = (Object[]) o;   // sin checkcast
    return k.length;
}
```

Dos sintomas, y el segundo es el grave:

```
java.lang.VerifyError: Bad type on operand stack in arraylength
  Reason: Invalid type: 'java/lang/Object' (current frame, stack[0])
```

y, cuando el verificador no llega a quejarse, **el cast que tenia que fallar no falla**: pasarle un
`String` a ese metodo devolvia 17 --el largo de la cadena leido como largo de arreglo-- en vez de
tirar `ClassCastException`. El mismo fuente con el javac del JDK 25 tira, que es lo correcto.

**Delimitacion.** Ocho cortes. El primer intento culpo al `instanceof` que habia antes del cast en
el codigo donde aparecio, y estaba **mal**:

| forma | resultado |
|---|---|
| `(String) o` sobre un parametro | anda |
| `(String) o` con `if (o instanceof String)` antes | anda |
| `(Object[]) o` sobre un **parametro** | **falla** |
| `(int[]) o` sobre un parametro | **falla** |
| `(Object[]) o` con `instanceof` antes | **falla** |
| `(Object[]) o` sobre un **local** al que se le asigno un arreglo en el mismo metodo | anda |
| `(String[]) o` sobre ese mismo local | anda |
| `((Object[]) o).length` sin local intermedio, sobre ese local | anda |

Las dos primeras filas descartan el `instanceof`: con un tipo de clase, la guarda no cambia nada.
Las filas 3 y 5 son el mismo cast con y sin guarda y fallan las dos, o sea que la guarda tampoco es
el disparador **con arreglos**. Lo que parte el mundo en dos es la fila 3 contra la 6: el mismo cast
sobre un parametro falla y sobre un local recien asignado anda.

Y esa ultima fila no es que ahi si se emita el `checkcast`: **tampoco se emite**. Lo que pasa es que
el verificador infiere el tipo del local por si mismo --vio el `anewarray`-- y no necesita el
`checkcast` para creerse el `arraylength`. Por eso el bug se escondio tanto tiempo: se ve solo
cuando el valor entra por un parametro, un campo o un retorno declarado `Object`, que es justo donde
un cast hace falta de verdad.

Causa: en `cast()` de `codegen.rs`, la rama de las referencias era

```rust
if let RType::Class(id) | RType::Parameterized { base: id, .. } = &to { ... }
```

Un `RType::Array(_)` no entra en ese `if let` y el metodo salia sin emitir nada -- ni el opcode ni
el cambio de tipo en la pila simulada. `checkcast_name` ya sabia dar el descriptor de un arreglo
(`[Ljava/lang/Object;`); lo que faltaba era llamarla.

Como salio: escribiendo `javax.management.openmbean`. `TabularDataSupport.remove(Object)` hace
`(Object[]) key` sobre su parametro, y el `.class` cargado en la JVM real con `-Xverify:all` no
paso. En **nuestra** VM el test daba -1 igual, porque no verifica frames: es la sexta vez en esta
sesion que un bug del codegen aparece solo corriendo el `.class` afuera.

Repro: `scratchpad/zzcast/{K1..K9,KA,KB,KC,KD}.java`. `KD` es el que importa: comprueba que un cast
invalido **tire**, y es el que separa este bug de un mero problema de verificacion.

### #477 -- ✅ el literal `null` se declaraba `Object` en el `StackMapTable`

```java
// javax/xml/datatype/DatatypeFactory.java
return newDuration(isPositive, null, null, day, hour, minute,
        second != null ? new BigDecimal(second) : null);
```

```
java.lang.VerifyError: Bad type on operand stack
  Location: newDurationDayTime(...)  @27: invokevirtual
  Reason: Type 'java/lang/Object' (current frame, stack[3]) is not assignable to 'java/math/BigInteger'
```

`null` tiene **tipo propio** en el verificador --`ITEM_NULL`, la etiqueta 5-- y es asignable a
cualquier referencia. `Object` no es asignable a ninguna otra. Nuestro `VType` no tenia esa variante:
`aconst_null` empujaba `Object("java/lang/Object")`, y el comentario que estaba al lado lo decia sin
saber lo que estaba diciendo -- *"declararlo `Object` alcanza mientras no se mezcle en un merge"*.

**Delimitacion.** El disparador son las dos cosas juntas:

| forma | resultado |
|---|---|
| `f(null, x)` -- sin salto en el medio | anda |
| `f(a ? null : b, x)` -- el `null` sobrevive a un salto | **falla** |

Sin salto no se escribe ningun frame y el tipo del `null` nunca se anota; con salto, el frame dice
`Object` y el llamado pide otra cosa. Por eso paso desapercibido tanto tiempo: la forma comun de
pasar `null` no lo toca.

Arreglo: `VType::Null` (etiqueta 5), empujada por `ExprKind::Null`. Y en `merge_states`, `null`
mezclado con una referencia da **esa referencia** en vez de `Top` -- es asignable a todas, asi que
el merge no pierde nada al quedarse con la mas especifica, mientras que degradar a `Top` dejaba el
valor inutilizable.

Como salio: corriendo `StaxTest` con `--patch-module` y `-Xverify:all` en el JDK 25.

### #478 -- ✅ un `goto` muerto despues de un cuerpo que no termina

```java
while (true) {
    if (a) { ...; continue; }
    if (b) { ...; continue; }
    break;
}
```

```
java.lang.VerifyError: Expecting a stack map frame
  Location: nextTag()I @66: goto
```

El cuerpo del bucle **no termina normalmente** --toda salida es un `continue` o el `break`-- y aun
asi el generador emitia la arista de vuelta al encabezado. Esa instruccion queda despues de una
transferencia incondicional y nadie llega a ella; §4.10.1 exige un frame ahi, y para codigo muerto
no hay frame que armar.

En el mismo repro habia **dos** instrucciones muertas y no una: el `goto` del `break` --escrito
despues del segundo `continue`, que ya corto el flujo-- y el de vuelta del `while`.

Arreglo: `if self.reachable` antes de emitir, en los cuatro lugares que emiten un salto que puede
quedar muerto -- el `while`, el `for`, el `break` y el `continue`. Es la misma idea del #463, que
arreglaba el caso del ternario constante; este es el caso a nivel de sentencia.

Nuestra VM no verifica frames, asi que estos `.class` cargaban y corrian bien aca. Es el sexto bug
del codegen que solo se ve corriendo el `.class` en una JVM real.

### #479 ⬜ -- `java.lang.Double d = new Rectangle2D.Double(...)` compila

El `new` emite la clase correcta --`javap -c` muestra `new java/awt/geom/Rectangle2D$Double`-- y sin
embargo el resultado se deja asignar a un `java.lang.Double`. El JDK lo rechaza.

Salio como control del #465: la asignacion se escribio para **comprobar** que el `new` resolvia mal,
y siguio compilando despues de que el `new` se arreglara. O sea que son dos cosas distintas y esta
segunda sigue abierta.

**No able la causa** y lo digo asi: no se si el chequeo de asignacion no consulta la jerarquia, si la
consulta y encuentra un supertipo que no corresponde, o si hay una indulgencia deliberada para los
tipos de `java.lang`. Lo unico comprobado es lo de arriba.

Repro: `java.lang.Double a = new java.awt.geom.Rectangle2D.Double(0.0, 0.0, 2.0, 2.0);`

### #477 -- ✅ el `lub` de dos arreglos daba `Unresolved`, y el ternario lo emitia como `Top`

```java
static char[] f(char[] p) {
    return p == null ? new char[0] : copy(p);   // VerifyError en la JVM real
}
```

```
java.lang.VerifyError: Bad type on operand stack
  Reason: Type top (current frame, stack[0]) is not assignable to reference type
  Stackmap Table:
    same_locals_1_stack_item_frame(@14,Top)
```

**Delimitacion.** Siete cortes:

| forma | resultado |
|---|---|
| ternario de `char[]`, una rama `new` y otra una llamada | **falla** |
| ternario de `char[]`, las **dos** ramas `new` | **falla** |
| ternario de `int[]` | **falla** |
| ternario de `String[]` | **falla** |
| ternario de `char[]` asignado a un campo | **falla** |
| ternario de `String` | anda |
| el mismo tipo y las mismas ramas con `if/return` en vez de ternario | anda |

Las cuatro primeras filas descartan que dependa del tipo del elemento o de como se construya el
arreglo. La sexta descarta el ternario en general. La septima es la que senala el mecanismo: con
`if/return` no hay merge --cada rama retorna-- y por eso anda.

Causa: `types::lub` filtra sus entradas por `erased_id(t).is_some()`, y `erased_id` de un
`RType::Array(_)` es `None` --un arreglo no tiene simbolo--. Con dos arreglos, el filtro se los
lleva a los dos, la lista queda vacia y `lub` devuelve `RType::Unresolved`. Eso no se ve en el tipo
de la expresion, pero el codegen del ternario declara sus dos ramas con el tipo del ternario
**entero** --que es lo que las hace coincidir en el destino-- y `vtype_of(Unresolved)` es `Top`.

Arreglo: `lub` trata los arreglos antes del filtro, siguiendo §4.10.3 -- mismo elemento da ese
arreglo; elementos de referencia distintos, el arreglo del lub de los elementos; elementos
primitivos distintos, `Object`. El `null` y lo no resuelto **no cuentan para decidir**: sin esa
salvedad, `cond ? null : bytes` pasaba a valer `Object` y rompia una asignacion que antes andaba
(lo detecto el compilado de `HKDFParameterSpec` inmediatamente despues del primer intento).

### #478 -- ✅ el ternario descontaba uno de mas de la pila, y `max_stack` salia corto

```java
class M { byte[] f;
    M(byte[] p) { this.f = p == null ? null : g(p, 0, p.length); }   // VerifyError
}
```

```
java.lang.VerifyError: Operand stack overflow
  Reason: Exceeded max stack size.
  stack: { 'M', '[B', integer }      // y todavia falta un `aload` mas
```

El metodo declaraba `stack=3` y necesitaba 4.

**Delimitacion.** Tres cortes, y el segundo es el que parte el mundo en dos:

| forma | resultado |
|---|---|
| `this.f = cond ? null : g(p, 0, p.length)` --ternario **con algo debajo** en la pila | **falla** |
| `byte[] r = cond ? null : g(p, 0, p.length)` --el mismo ternario en un local | anda |
| lo mismo con `String` en vez de `byte[]` | **falla** |

La segunda fila descarta el ternario y la llamada: lo unico que cambia es si hay algo en la pila
cuando el ternario empieza. La tercera descarta que tenga que ver con #477 --con `String` el `lub`
anda y el `max_stack` sale corto igual--, o sea que son dos bugs distintos que se manifestaban en el
mismo archivo.

Causa: `ExprKind::Ternary` hacia `self.pop(1)` **despues** de `self.bind(otherwise)`. `bind` ya
restaura el estado anotado en el salto --el de antes de la rama verdadera--, asi que ese `pop`
descuenta uno de mas y la rama falsa se simula una posicion por debajo de la real. Con el ternario
solo, la altura previa es cero y el `pop` satura sin dano; con algo debajo --el `this` de una
asignacion a campo-- el `max_stack` sale exactamente uno corto.

Se veia solo si la rama falsa era **la mas alta del metodo**: en cualquier otro caso el maximo lo
fijaba otra instruccion y el numero corto no molestaba. Y nuestra VM no comprueba `max_stack`, asi
que el `.class` corria bien aca.

Repro: `scratchpad/zzter/{T1..T7}.java` (#477) y `scratchpad/zzmax/{M1,M2,M3}.java` (#478).

### #480 ⬜ -- un `super(...)` de una interna al constructor de la interna de su superclase

```java
public class Outer {
    protected class Inner { Inner(Object a) { } }
}
public class Sub extends Outer {
    protected class SubInner extends Inner {
        SubInner(Object a) { super(a); }   // <-- aca
    }
}
```

Dos sintomas distintos segun de donde venga `Inner`, y **no able por que difieren**:

| forma | resultado |
|---|---|
| `Outer.java` y `Sub.java` en la misma invocacion | `error: el generador de bytecode todavia no soporta un super(...) que no resolvio a ningun constructor` |
| `Inner` leida de un `.class` del classpath | **compila**, y emite un descriptor que no existe |

El segundo es el peligroso. El constructor de `Inner` se emite bien --con la instancia envolvente de
cabecera, `(Outer, Object)V`-- y la llamada desde `SubInner` se emite sin ella:

```
Sub$SubInner(Sub, java.lang.Object);
   0: aload_0
   1: aload_2
   2: invokespecial // Method Outer$Inner."<init>":(Ljava/lang/Object;)V   <-- no existe
```

Falta el `aload_1` de la envolvente, y el descriptor del `Methodref` se arma con los argumentos
escritos en vez de con la firma del constructor que se resolvio. El `.class` carga en nuestra VM
--que no comprueba que el metodo exista hasta usarlo-- y muere en una JVM real con
`NoSuchMethodError` al construir el objeto.

Como salio: escribiendo `java.beans.beancontext.BeanContextServicesSupport`, cuyo `BCSSChild`
extiende al `BCSChild` de `BeanContextSupport`. `BeanCtxTest` daba -1 en nuestra VM y
`NoSuchMethodError` en el JDK 25 con `--patch-module`.

Rodeo aplicado: `BCSSChild` no se escribio. `createBCSChild` delega en el de la superclase, y el
javadoc dice por que. No es parte de la superficie medida, asi que no cuesta nada medido -- pero es
una clase del JDK que falta por un bug nuestro, no por una decision de diseno.

Repro: `KajiLibrary/repros/finding_480/` (`Outer.java`, `Sub.java`; el JDK 25 compila los dos y emite
el `aload_1` que falta).

### #466, el arreglo

`reflective_call.rs` resolvia el metodo **solo** sobre la clase que lo declara, con este comentario:

> El método se resuelve sobre la clase que lo DECLARA. Un `invoke` no despacha virtualmente por su
> cuenta: lo hace `call_java` si el callee tiene una entrada de vtable, y si no, la declarante es la
> única respuesta posible.

La segunda mitad no era cierta. `invokeinterface` ya tenia la respuesta: buscar la firma en la tabla
**fusionada** del receptor con `vtable_method_at_mirror_by_signature`, que encuentra tanto un
override de clase como un `default` heredado de una superinterfaz. `Method.invoke` ahora hace lo
mismo, salvo para lo que la especificacion excluye --estaticos, privados y `<init>`--, que siguen
ligados a lo declarado.

| `Method` obtenido de | antes | ahora | JDK 25 |
|---|---|---|---|
| clase concreta | 3 | 3 | 3 |
| interfaz, metodo abstracto | panico | **3** | 3 |
| clase abstracta, metodo abstracto | panico | **5** | 5 |
| interfaz, `default` con override | **1** (mal) | **3** | 3 |

Repro: `java/Rf466.java`, los cuatro casos en un solo archivo. `cargo test` sigue en 1463/22, con la
lista de fallos identica.

### #481 -- ✅ un miembro de interfaz sin modificador se trataba como de paquete

```java
// java/lang/classfile/ClassFileTransform.java
public interface ClassFileTransform<...> {
    default void atStart(B builder) { }
}

// jdk/internal/classfile/impl/Transforms.java  -- otro paquete
this.first.atStart(inner);
```

```
error: el metodo `atStart` es `de paquete` en `ClassFileTransform`
       y no es accesible desde `ChainedClass`
```

JLS §9.4: **todo miembro de una interfaz es implicitamente `public`**, escrito o no. La unica
excepcion es un metodo de interfaz declarado `private` (Java 9+), que si es privado.

Lo que hace interesante a este bug es que **el chequeo y la emision no estaban de acuerdo**: el
`.class` que sale marca el metodo `ACC_PUBLIC` --se comprobo con `javap` sobre
`AttributedElement`-- y el que se plantaba era el chequeo de acceso. O sea que el artefacto estaba
bien y el error era imaginario.

La funcion con la regla puesta ya existia: `access_level_in` en `check.rs`, escrita para otra cosa y
con el comentario que dice exactamente esto. Lo que faltaba era que `check_access` la usara en vez
de mirar `modifiers.contains(Public)` a mano.

Por que no habia salido antes: hace falta que un miembro `default` sin `public` escrito se use
**desde otro paquete**. Dentro del mismo paquete, "de paquete" y "publico" se comportan igual, y casi
toda la biblioteca consume sus interfaces desde su propio paquete.

Arreglo: `check_access` calcula el nivel con `access_level_in(&mods, owner)` y compara contra eso.

Como salio: escribiendo `java.lang.classfile.ClassFileTransform`, cuyas implementaciones viven en
`jdk.internal.classfile.impl`.

### #482 ⬜ -- una clase anonima en el inicializador de un campo de interfaz

```java
public interface ClassTransform ... {
    ClassTransform ACCEPT_ALL = new ClassTransform() {
        public void accept(ClassBuilder b, ClassElement e) { b.with(e); }
    };
}
```

```
error: el generador de bytecode todavia no soporta una clase anonima
       (necesita una clase sintetica anidada)
```

Una anonima en el cuerpo de un metodo si anda; en el inicializador de un campo de una **interfaz**,
no. **No able cual de las dos cosas es el disparador** --si el inicializador de campo o que el
contenedor sea una interfaz-- y lo digo asi en vez de completar la explicacion.

Rodeo aplicado: una clase de paquete con nombre al final del archivo. Ademas de compilar, se lee
mejor en un volcado de pila que un `ClassTransform$1`.

### #483 ⬜ -- un metodo declarado en una interfaz ANIDADA que redefine al de la que la encierra

```java
public interface ConstantInstruction extends Instruction {
    ConstantDesc constantValue();

    public interface ArgumentConstantInstruction extends ConstantInstruction {
        Integer constantValue();          // estrecha el retorno
    }
}
```

```java
Integer v = ((ArgumentConstantInstruction) ins).constantValue();
// error: el generador de bytecode todavia no soporta una llamada
//        que no resolvio a ningun metodo
```

Preguntando por el supertipo --`((ConstantInstruction) ins).constantValue()`-- resuelve. O sea que el
metodo existe y se encuentra por la interfaz de afuera, pero no por la anidada que lo redefine.

**Lo que no able:** si el disparador es la anidacion, el retorno covariante, o los dos juntos. Hay
otros casos de retorno covariante en el arbol que si compilan, asi que la anidacion es sospechosa,
pero no lo comprobe.

Rodeo aplicado: preguntar por el supertipo y castear el resultado. Donde eso no alcanzaba
--`LoadConstantInstruction.constantEntry()`, que no existe en el supertipo-- se puso un ayudante en
`jdk.internal.classfile.impl.Instructions`, que esta en el mismo paquete que la implementacion y
puede preguntarle directo.

### #484 ⬜ -- un ternario cuyas ramas dan tipos distintos no calcula el supertipo comun

```java
MemberRefEntry ref = isInterface
        ? pool.interfaceMethodRefEntry(owner, name, type)   // InterfaceMethodRefEntry
        : pool.methodRefEntry(owner, name, type);           // MethodRefEntry
// error: el generador de bytecode todavia no soporta una llamada
//        que no resolvio a ningun metodo
```

Las dos ramas dan tipos distintos que comparten un supertipo (`MemberRefEntry`), y §15.25 dice que el
tipo del ternario es ese supertipo. Nuestro javac no lo calcula.

**No able** si el problema es el `lub` de dos tipos de referencia en general o algo particular de
estas interfaces. `javac::types::tests::lub_of_types_sharing_several_supertypes_is_an_intersection`
--que estaba fallando en HEAD antes de que yo tocara nada-- apunta a que el `lub` tiene sus propios
problemas, pero no probe que sea el mismo.

Rodeo aplicado: un `if`/`else` con el local declarado del tipo del supertipo. Ahi no hay nada que
calcular.

---

## Hallazgo: la referencia adelantada a un campo estatico no se rechaza

**Que pasa.** Un inicializador de campo estatico que usa, por su **nombre simple**, otro campo
estatico declarado mas abajo en la misma clase es un error de compilacion (JLS 8.3.3). Nuestro javac
lo acepta y produce una clase que lee el campo antes de que su inicializador haya corrido -- o sea,
en null o en cero.

```java
public class FR {
  static final int A = B;        // JDK 25: error: illegal forward reference
  static final int B = 7;
  static final int[] T = U;      // idem
  static final int[] U = {1,2,3};
}
```

| fuente | nuestro javac | JDK 25 |
|---|---|---|
| el de arriba | **compila** ✗ | dos errores |

**Como aparecio.** Escribiendo `javax.imageio.plugins.jpeg.JPEGQTable`, cuyas cuatro constantes
publicas se arman a partir de cuatro tablas privadas. El generador emitio primero las constantes:

```java
public static final JPEGQTable K1Luminance = new JPEGQTable(K1LUMINANCE_TABLE, false);
// ...
private static final int[] K1LUMINANCE_TABLE = { 16, 11, ... };
```

Con el JDK eso no compila. Con el nuestro compilo, y `K1Luminance` quedo envolviendo un `null` que
recien exploto en tiempo de ejecucion, adentro de un `System.arraycopy` -- treinta lineas y una capa
mas lejos del error real.

**Por que importa mas de lo que parece.** No es permisividad como el hallazgo anterior: aca el
programa que sale **hace otra cosa** que lo que el fuente aparenta decir. Y el sintoma llega tarde y
disfrazado, que es exactamente lo que la regla del JLS existe para evitar.

**Donde estaria.** En el chequeo de inicializadores de campo, comparando la posicion de la
declaracion que se referencia contra la del inicializador que la usa, con las tres excepciones de
la regla: el nombre cualificado (`FR.B`), el lado izquierdo de una asignacion, y el uso adentro de
una clase anidada.

**Efecto colateral bueno.** Buscando el sintoma aparecio un bug de la maquina virtual: ver abajo.

## Hallazgo (VM): `System.arraycopy` entraba en panico con un origen nulo

**Que pasaba.** `System.arraycopy(null, 0, dst, 0, n)` no lanzaba `NullPointerException`: hacia
`expect` sobre el nombre de clase del origen y se llevaba puesta la maquina virtual entera.

```
thread 'main' panicked at src\jvm\interpreter
atives.rs:
System.arraycopy: el origen no es un array conocido
```

Un panico es la respuesta correcta para un invariante roto de la VM. No lo es para un programa Java
que paso un null: JLS 11.5 define eso como `NullPointerException`, y como tal se puede atrapar.

**El arreglo.** `NativeOutcome` no tenia forma de pedir que se lanzara una excepcion: sus tres
variantes eran "corrio", "corrio y hay que inicializar", y "no hay puente". Se agrego `Lanza(String)`
y los cuatro sitios de despacho la traducen a `throw_exception`. Con eso, `arraycopy` lanza
`NullPointerException` si el origen o el destino son nulos, y `ArrayStoreException` si lo que llega
no es un array.

La variante sirve para cualquier otro nativo con la misma necesidad; hasta ahora la unica salida era
el panico.

## Sobre el JDK 25.0.2 (no es un hallazgo de nuestro compilador)

`java.lang.constant.ClassDesc.ofDescriptor("I")` **como primer uso del paquete** tira
`ExceptionInInitializerError`: `PrimitiveClassDescImpl.<clinit>` llama por dentro a
`MethodTypeDescImpl` antes de que ese termine de inicializarse, y a partir de ahi
`java.lang.constant` queda inutilizable en ese proceso
(`NoClassDefFoundError: Could not initialize class ConstantDescs`).

Ablado con dos programas de cuatro lineas corridos con `java` de verdad:

| primer uso | resultado |
|---|---|
| `ClassDesc.ofDescriptor("I")` | **falla** |
| `MethodTypeDesc.of(ClassDesc.of("java.lang.Object"))` y despues el primitivo, dentro de `main` | anda |
| `ConstantDescs.CD_int` | anda |

Queda anotado porque nos afecta de una forma concreta: `java/CfBuildTest.java` usa el JDK como
oraculo, asi que **tiene que poder correr alla**. Por eso toma los descriptores de primitivos de
`ConstantDescs` y no de `ClassDesc.ofDescriptor`, con la explicacion escrita en el propio archivo
para que nadie lo "simplifique" de vuelta.

### #467, el arreglo -- y las dos mitades que faltaban

El bug tenia **tres** partes, no una, y solo la primera estaba en el titulo.

**1. La lista de anotaciones conocidas.** `runtime_retained_annotations` sembraba el conjunto con dos
nombres --`Deprecated` y `FunctionalInterface`-- y le agregaba los `@interface` **de la unidad**.
`Retention`, `Target` y `Documented` no estaban en ninguno de los dos grupos, asi que las
meta-anotaciones de un `@interface` se filtraban y no llegaban al `.class`. El arreglo es la lista
completa de las del JDK que estan retenidas en runtime, con una salvaguarda: si el fuente declara
una anotacion **con el mismo nombre simple** y retencion menor, gana la del fuente.

**2. Nunca se leia la retencion de una anotacion del classpath.** Esta era la mitad que hacia el
daño de verdad. Aun con `Ann.class` bien emitido, al compilar la unidad que **usa** `@Ann` el emisor
seguia sin saber su retencion --solo miraba la unidad que tenia delante-- y la descartaba en
silencio. Ahora `read_attributes` mira el `RuntimeVisibleAnnotations` de la clase externa,
`ExternalClass` lleva el dato, la tabla de simbolos lo indexa por nombre binario, y
`build_annotations` le pregunta al tipo que resolvio.

Se lee con un recorrido de verdad del `element_value` (§4.7.16.1) y no buscando la cadena `RUNTIME`
en el pool: esa cadena aparece tambien en una anotacion que la mencione por otro motivo.

**3. `Method.getAnnotation` devolvia null siempre.** Con las dos mitades de arriba, el `.class` ya
llevaba las anotaciones del metodo -- y la biblioteca seguia sin poder leerlas. El stub decia, con
razon en su momento, "un metodo sin anotaciones en runtime, que es el caso comun, recibe la
respuesta vacia correcta"; dejo de ser cierto en el momento en que el compilador empezo a emitirlas.
Se agrego el native `Method.declaredAnnotations0`, que hace lo mismo que el de `Class` pero busca el
atributo en el `method_info`.

**Repro de punta a punta** (`scratchpad/a467/`): `Ann.java` con `@Retention(RUNTIME)` compilado
solo, y `Usa.java` compilado **despues** contra el `.class` del primero.

| | antes | ahora |
|---|---|---|
| `Ann.class` trae `RuntimeVisibleAnnotations` | no | **si** |
| `Usa.class` trae la anotacion de clase | no | **si** |
| `Usa.class` trae la anotacion de metodo | no | **si** |
| `Usa.class.getAnnotation(Ann.class)` | null | **`hola`** |
| `Usa.class.getMethod("m").getAnnotation(...)` | null | **`metodo`** |

`cargo test`: 1462 ok / 22 fallan, la lista identica a la de antes.

### #485 ⬜ -- la resolucion de sobrecargas no descarta lo que no se puede ver

`new DatagramSocket(null)` desde otro paquete no compila: *"la referencia a `DatagramSocket` es
ambigua"*. `java.net.DatagramSocket` tiene dos constructores de un argumento --el `public` que toma
`SocketAddress` y el `protected` que toma `DatagramSocketImpl`-- y `null` le entra a los dos, asi que
sin mas informacion son igual de aplicables y ninguno es mas especifico que el otro.

Pero desde otro paquete el `protected` **no se puede ver**, y la JLS resuelve eso antes de comparar:
§15.12.2.1 arma el conjunto de metodos aplicables solo con los **accesibles** desde el sitio de la
llamada. Con uno solo adentro no hay nada que comparar y no hay ambiguedad. El JDK 25 compila la
misma linea sin una queja.

Nuestro compilador junta todos los candidatos por aridad y compatibilidad, y recien despues --si
queda uno-- chequea el acceso. Lo que hay que mover es el filtro de accesibilidad **antes** del
descarte por especificidad, no despues.

Se nota en cualquier clase con un miembro `protected` sobrecargado contra uno `public`, que es un
patron comun en `java.net` y en `java.io`: la implementacion que una subclase puede traer, al lado
de la que usa todo el mundo.

Rodeo: escribir el tipo, `new DatagramSocket((SocketAddress) null)`. Es lo que hace `UdpTest`.


### #489 ⬜ -- una constante `static final` no se estrecha en contexto de asignación

`byte getBaselineFor(char)` con `return ROMAN_BASELINE;`, donde `ROMAN_BASELINE` es un
`static final int = 0`, da **error: tipo de retorno incompatible**. El javac real lo acepta: §5.2
permite el estrechamiento primitivo cuando la expresión es una **constante** de tipo `byte`, `short`,
`char` o `int` y el valor entra en el destino, y el contexto de `return` es contexto de asignación.

Ablación (`/tmp/narrow/N.java`):

| código | javac real | el nuestro |
|---|---|---|
| `static byte f() { return 0; }` | compila | compila |
| `static byte f() { return K; }` con `static final int K = 0` | compila | **error** |
| `byte b = K;` | compila | **error** |

La causa está anotada en el propio código: `constant_narrowing_ok` llama a `const_eval_int` con un
`ConstFieldMap::new()` **vacío**, porque el atributado corre antes de plegar las constantes de la
unidad. Pliega literales y aritmética entre literales; no pliega referencias a otra `final`.

O sea que no alcanza con agregar `constant_narrowing_ok` al camino de `return` --que también falta--:
hay que poder resolver las constantes de la propia clase durante el atributado.

Rodeo: conversión explícita, `return (byte) ROMAN_BASELINE;`. Es lo que hace `java/awt/Font.java`.

### #490 -- ✅ un inicializador de arreglo no ampliaba sus elementos al tipo del arreglo

`new float[] { unInt }` emitía `iload` + `fastore` **sin** el `i2f`. Nuestra VM no verifica los tipos
de la pila y lo dejaba pasar leyendo el `int` como si fuera un `float`; la JVM real lo rechaza:

```
java/awt/image/ComponentColorModel.getRGB(I)I @12: fastore
Reason: Type integer (current frame, stack[5]) is not assignable to float
```

Es el noveno bug que sólo se ve corriendo nuestras clases en la JVM real con `-Xverify:all`.

Ablación (`/tmp/abl/A.java`), mirando el bytecode emitido:

| código | antes | después | javac real |
|---|---|---|---|
| `new float[] { p }` con `int p` | `iload_0; fastore` | `iload_0; i2f; fastore` | `iload_0; i2f; fastore` |
| `new float[] { 1 }` | `iconst_1; fastore` | `iconst_1; i2f; fastore` | `fconst_1; fastore` |
| `new long[] { p }` con `int p` | `iload_0; lastore` | `iload_0; i2l; lastore` | `iload_0; i2l; lastore` |
| `float[] a = new float[1]; a[0] = p;` | correcto | correcto | correcto |

La última fila es la que señala dónde estaba el agujero: el camino de `a[i] = v` en `assign` ya
llamaba a `widen_cat`, y el del **inicializador** no. Eran dos caminos distintos al mismo `IASTORE`.

Con el literal seguimos emitiendo una instrucción de más (`iconst_1; i2f` donde el javac real pliega a
`fconst_1`). Es correcto y no lo arreglamos acá: plegar la constante al tipo del arreglo es otra cosa
y no cambia lo que hace el programa.

Arreglo: en `codegen.rs`, `self.widen_cat(elem_cat)` después de emitir cada elemento del
inicializador.

### #486 ⬜ -- las constantes de una interfaz implementada no resuelven sin calificar

```java
public interface If1 { short ELEMENT_NODE = 1; short tipo(); }
public class Uso implements If1 { public short tipo() { return ELEMENT_NODE; } }
```

*"no se encuentra el simbolo: ELEMENT_NODE"*. El JDK compila las dos lineas sin una queja: una clase
hereda los campos de las interfaces que implementa (JLS 8.1.5, y 6.5.6.1 para el nombre simple).

**Lo interesante es que el arreglo #475 ya esta hecho y no alcanza.** `lookup_field`
(`src/javac/attribute.rs`) recorre las interfaces en anchura y hasta lo documenta; lo que falla son
los otros dos caminos, que no pasan por ahi. Las tres formas dan tres resultados distintos, y esa es
toda la pista:

| se escribe | resultado | mensaje |
|---|---|---|
| `If1.ELEMENT_NODE` | **compila** | -- |
| `this.ELEMENT_NODE` | falla | `no se encuentra el campo` |
| `ELEMENT_NODE` | falla | `no se encuentra el simbolo` |

Dos mensajes distintos son dos caminos distintos: el del campo sobre un receptor y el del nombre
simple en el ambito. Que la forma calificada ande descarta que el simbolo de la interfaz no
resuelva --resuelve-- asi que lo que sospechar es que la lista de interfaces de la **clase** este
vacia cuando esos dos caminos preguntan, o que directamente no la miren.

Se descarto que sea un problema de herencia en general: el mismo programa con una **superclase**
--`class Uso2 extends Base` usando `OTRO_NODE` de `Base`-- compila, tanto con `Base` del classpath
como compilada en la misma invocacion. Es especifico de las interfaces.

Tampoco es del classpath: falla igual compilando `If1.java` y `Uso.java` en una sola invocacion.

Repro: `scratchpad/zz486/{If1,Uso,Uso2,Uso3,Uso4,Base}.java`.

Se ve en cualquier interfaz de constantes, que es un patron viejo pero muy usado en las APIs que este
arbol copia: `org.w3c.dom.Node` y sus doce tipos de nodo son el caso que lo destapo.

Rodeo: calificar con el nombre de la interfaz.

### #487 -- ✅ `Class.forName` no corria el inicializador estatico (VM, no compilador)

```java
class Marca { static boolean visto = false; }
class Cargada { static { Marca.visto = true; } }
...
Class.forName("Cargada");                                   // Marca.visto sigue en false
Class.forName("Cargada2", true, unCargador());              // tambien false
```

Las dos formas cargan la clase y **ninguna corre su `<clinit>`**. El JLS 12.4.1 es explicito: cargar
no es inicializar, pero `Class.forName(String)` inicializa siempre, y la de tres argumentos
inicializa cuando el segundo es `true`. Es la unica diferencia entre las dos, y es toda la razon de
que la de tres argumentos exista.

**Por que importa mas de lo que parece.** Es el mecanismo con el que una clase se **registra sola**
al ser nombrada, y sobre eso se apoyan varios idiomas centrales de la plataforma: el driver de JDBC
que se anota en el `DriverManager`, un proveedor de `spi` que se instala al cargarse, cualquier
tabla que se llene desde bloques `static`. Todos compilan, corren, y **no hacen nada**, sin un solo
error que mirar -- que es la peor forma de fallar.

Lo destapo la adopcion de sockets de `java.nio.channels`: `jdk.internal.net.Adopcion` cargaba
`java.net.Socket` por nombre para que su bloque `static` registrara la fabrica, y la fabrica nunca
aparecia. El rodeo fue forzar la inicializacion desde el unico lado que puede nombrar la clase sin
volver circular la dependencia --los canales, que ya la nombran en la firma de `socket()`-- con un
`new java.net.Socket()` que no abre nada. Esta escrito ahi y aca porque es un rodeo, no un arreglo.

**El arreglo.** El problema no era que faltara el mecanismo --`ensure_initialized` existe y hace
exactamente lo que hay que hacer: empuja el marco del `<clinit>` y lo drivea hasta el final-- sino
que **un nativo no lo alcanza**: recibe el metaspace y el heap, no el `&mut self` del interprete. El
nativo cargaba y ahi se le acababan las manos.

Se resolvio dandole al nativo una forma de **pedir** lo que no puede hacer:
`NativeOutcome::RanEInicializa(valor, clase)`. El nativo devuelve eso, y el sitio de despacho --que
si tiene el interprete-- corre la inicializacion antes de empujar el valor a la pila. La alternativa
era darle al nativo acceso al interprete entero, que es muchisimo mas de lo que necesita para esto.

`forName0` paso a tomar la bandera --`forName0(String, boolean)`-- porque las tres sobrecargas
publicas piden cosas distintas, y se comprobo contra el JDK 25 cual pide cual:

| forma | inicializa |
|---|---|
| `forName(String)` | **si** |
| `forName(String, true, ClassLoader)` | **si** |
| `forName(String, false, ClassLoader)` | no |
| `forName(Module, String)` | no |

Las dos ultimas antes delegaban en la primera, asi que ademas de no inicializar cuando debian,
habrian inicializado cuando no debian en cuanto se arreglara la primera. Las cuatro estan ahora
sobre `forName0` con la bandera que les toca.

Repro y prueba: `java/ClinitProbe.java`, que da -1 contra el JDK 25 y contra `run-headless`.

### #488 -- ✅ la entrada `InnerClasses` de un tipo anidado perdia sus modificadores implicitos

```java
public class P {
    public enum Color { ROJO, VERDE }
}
```

Compilado por nosotros y corrido en la **JVM real**: `Color.class.isEnum()` daba `false` y
`getEnumConstants()` daba `null`. En nuestra VM daba `true`, y esa diferencia es toda la pista.

**Donde estaba.** Los `access_flags` de clase (§4.1) salian bien --`javap` mostraba
`ACC_PUBLIC, ACC_FINAL, ACC_SUPER, ACC_ENUM`-- pero para un tipo **anidado**
`Class.getModifiers()` no los lee de ahi: lee su entrada del atributo `InnerClasses` (§4.7.6). Y esa
entrada la armaba `inner_entry` con los modificadores **escritos en el fuente** mas
`ACC_INTERFACE|ACC_ABSTRACT` para interfaces, y nada mas:

| | nosotros | JDK 25 |
|---|---|---|
| `InnerClasses` de un enum anidado | `public` | `public static final` |

Faltaban los implicitos: `ACC_ENUM` y `ACC_STATIC` de un enum (§8.9), su `ACC_FINAL` cuando no
declara metodos abstractos (§8.9.1), el `ACC_STATIC` de una interfaz o `@interface` miembro (§8.1.3,
§9.5), el `ACC_ANNOTATION`, y el `ACC_FINAL|ACC_STATIC` de un record anidado (§8.10). El codigo que
los pone para los access_flags de clase estaba unas lineas mas arriba, correcto y completo; lo que
faltaba era el mismo criterio en la otra mitad.

**Lo que rompia.** Todo lo que introspecciona un tipo anidado corriendo en una JVM de verdad. Se
destapo con el introspector de MXBean del JDK, que rechazaba nuestra interfaz entera con *"has
parameter or return type that cannot be translated into an open type"* porque el `enum` que
mencionaba no era, para el, un enum. Nuestra propia VM no lo notaba porque no consulta
`InnerClasses` para `getModifiers`, asi que la unica forma de verlo era correr nuestro `.class` en
la JVM real -- que es exactamente para lo que sirve ese cruce.

Arreglado en `inner_entry` (`src/javac/codegen.rs`), con el mismo criterio que los access_flags de
clase.

Repro y prueba: `java/MXBeanProxyTest.java`, que da -1 contra el JDK 25, contra `run-headless`, y
--lo que importa aca-- compilado por nosotros y corrido en la JVM real con `-Xverify:all`.

**Queda una consecuencia por atender:** los `.class` que ya estan en `KajiLibrary` se emitieron con
la entrada vieja, asi que **cualquier tipo anidado de la biblioteca sigue mintiendo sus
modificadores en una JVM real** hasta que se recompile. Adentro de nuestra VM no cambia nada.

### #491 ⬜ -- un tipo del classpath se vuelve visible sin importar, con solo haberse cargado

```java
public class B {
    java.awt.Container c;                        // basta con nombrarlo una vez
    public int f() { return MouseEvent.MOUSE_WHEEL; }  // sin ningun import: compila
}
```

`MouseEvent` no esta importado, no esta en `java.lang` y no esta en el paquete de `B`. El javac real
lo rechaza; el nuestro lo acepta.

**La ablacion.** El disparador es que **algo** haya obligado a cargar la clase, no de donde se
hereda ni que se importe:

| fuente | nuestro javac | JDK 25 |
|---|---|---|
| `class A { int f(){ return MouseEvent.MOUSE_WHEEL; } }` | error ✓ | error |
| lo mismo + un campo `java.awt.Container c;` | **compila** ✗ | error |
| `class ZZP2 extends CheckboxGroup` (su unidad no importa nada de eventos) | **compila** ✗ | error |
| `class ZZP3 extends Container` usando `NumericShaper` (fuera de la clausura de `Container`) | error ✓ | error |

O sea: no fugan los `import` de la superclase --`CheckboxGroup` solo importa `Serializable`-- ni hay
un `java.awt.event.*` implicito --`NumericShaper`, que esta en `java.awt.font`, sigue fallando--.
Fuga **la clausura de carga**: nombrar `Container` carga `Component`, que menciona `MouseEvent` en
sus firmas, y con eso el nombre queda disponible. Pasa igual en el paquete por omision, asi que no es
cosa de estar dentro de `java.awt`.

**Donde esta.** `SymbolTable::externals` (`src/javac/symbol.rs`) es un mapa plano de **nombre
simple** a simbolo, que se llena con todo lo que la carga toca. `resolve_class_name`
(`src/javac/enter.rs`) consulta:

```rust
if table.external(name).is_some() {
    return true; // java.lang, auto-importado
}
```

El comentario dice `java.lang`, pero el mapa hace rato que tiene mucho mas que `java.lang`. La
disciplina correcta ya esta escrita al lado, para los tipos **del fuente**, en `alias_source`: *"un
nombre que nadie importo no se vuelve visible por estar en el round"*. Lo que falta es la misma regla
para los externos.

**Lo que puede romper.** Hoy es permisividad --acepta programas que el javac real rechaza, y eso solo
muerde cuando el fuente se lleva a otro compilador--. Pero el propio `external` avisa que *"con dos
homonimos de paquetes distintos, aca esta el que se cargo primero"*, asi que en cuanto dos paquetes
tengan el mismo nombre simple, un nombre sin importar puede resolver al **equivocado**, y eso ya no
es permisividad sino compilar otra cosa.

Repro: `repros/finding_491/`.

### #492 -- ✅ el `super()` implicito no pasaba la instancia envolvente

```java
class Base {
    int valor = 7;
    class Inner { int leer() { return Base.this.valor; } }
}
class Sub extends Base {
    class SubInner extends Inner { }        // sin constructor: el `super()` lo pone el compilador
    Inner armar() { return new SubInner(); }
}
```

`s.armar().leer()` da 7 con el javac real. Con el nuestro, corrido en la **JVM real**:

```
java.lang.NoSuchMethodError: Base$Inner: method 'void <init>()' not found
    at Sub$SubInner.<init>(Sub.java)
```

**Donde estaba.** Una clase interna de instancia no tiene un `<init>()V`: tiene
`<init>(LEnvolvente;)V`, porque su instancia envolvente es parte de su identidad (§8.1.3). El
`super()` implicito de `SubInner` es en realidad `super(this$0)`, y nosotros lo emitiamos siempre
con descriptor `()V`:

| | nosotros | JDK 25 |
|---|---|---|
| `Sub$SubInner.<init>` | `aload_0; invokespecial Base$Inner."<init>":()V` | `aload_0; aload_1; invokespecial Base$Inner."<init>":(LBase;)V` |

El desugar ya le ponia a cada constructor su parametro de cabecera `this$0`, y el `new SubInner(...)`
ya lo pasaba; lo unico que no lo reenviaba era el `super()` que el emisor sintetiza cuando el cuerpo
no arranca con `super(...)`/`this(...)` --que es justamente el caso de una interna sin constructor
escrito--.

**Lo que rompia.** Todo el patron `AccessibleAWTXxx` de AWT, que es la forma en que la accesibilidad
esta escrita en la biblioteca entera: `Panel.AccessibleAWTPanel extends Container.AccessibleAWTContainer
extends Component.AccessibleAWTComponent`. En la JVM real, `NoSuchMethodError` al construir. En la
nuestra --que resuelve mas suelto y deja pasar la llamada-- el objeto quedaba con el `this$0` de la
superclase en `null`, asi que el sintoma era un `NullPointerException` la primera vez que un metodo
**heredado** usaba `Component.this`: `getAccessibleStateSet()` sobre cualquier widget. Los metodos
propios de la subclase andaban, que es lo que hacia el defecto tan confuso.

**El detalle que costo, dos veces.** El primer arreglo miraba el `owner` y el modificador `static`
del simbolo de la superclase. Anda con un supertipo del **mismo round** y no con uno del classpath:
un tipo anidado externo se registra en el scope de su envolvente por nombre, con `owner: None` y sin
`static`. Y `Component$AccessibleAWTComponent` llega siempre del classpath, porque la biblioteca se
compila de a un archivo. La segunda version lo decidia por la **firma de los constructores** --si no
tiene ninguno sin argumentos y el primer parametro de alguno es su envolvente-- y con eso paso el
caso del classpath... y **rompio el del mismo round**, que la primera version si cubria y que no se
volvio a correr: la firma de un constructor del fuente puede no tener todavia el `this$0` cuando se
emite la subclase, porque lo agrega el desugar por unidad y el orden entre unidades no esta
garantizado. Se destapo al escribir el repro de #496, que pone `Outer` y `Sub` en la misma linea de
comandos: `NoSuchMethodError: Base$Inner: method 'void <init>()' not found`, el mismo sintoma de
origen. La version que quedo tiene **los dos caminos**: primero el simbolo (dueno, `static`,
miembro de interfaz), que es lo que sabe un tipo del fuente; y solo si no hay dueno --un externo--
la firma. `repros/finding_492/sep` se corre ahora de las dos formas, juntos y contra el `.class`.

Arreglado en `super_enclosing_internal` + el `super()` implicito de `gen_method`
(`src/javac/codegen.rs`).

Repro: `repros/finding_492/`. Prueba: `java/AwtWidgetTest.java`, que da -1 contra el JDK 25 y contra
`run-headless`.

**El barrido, ya hecho.** Los `.class` emitidos antes de esto tienen el `super()` sin argumento, asi
que toda interna que extiende a otra interna quedaba rota hasta recompilar su archivo. Se recorrio
`KajiLibrary` entera comparando cada `Methodref` a un `<init>` contra los constructores que la clase
apuntada **realmente tiene**: los 22 casos eran los `AccessibleAWTXxx` de `java.awt`, y ya estan
recompilados. Las otras internas-que-heredan-de-internas de la biblioteca --`Format.Field`,
`Control.Type`, `Line.Info`, las de `AttributeSetUtilities`-- son anidadas `static` o traen un
`super(...)` escrito, asi que nunca pasaron por este camino.

### #493 ⬜ -- un `import` de un solo tipo pierde contra el homonimo del paquete, y se filtra entre unidades

Son dos sintomas del mismo lugar: la resolucion de un nombre de tipo simple no distingue **de que
unidad de compilacion** viene cada candidato. El JLS ordena los candidatos en cuatro escalones
(§6.5.5, §7.5.1): tipos de **la propia unidad**, `import` de un solo tipo, tipos del **paquete**,
`import` on demand. Nosotros juntamos el primero y el tercero en un solo "scope" y lo consultamos
**antes** del import.

**Sintoma A -- el import pierde contra el paquete.**

```java
package p;
import java.util.ArrayList;
import java.util.List;          // tapa a p.List (JLS 7.5.1)

public class Uso {
    private final List<String> xs = new ArrayList<String>();
}
```

Con un `p/List.java` en el mismo round, el JDK 25 compila y nosotros damos
*"tipo incompatible en el inicializador de `xs`"*: `List` resolvio a `p.List`.

**Sintoma B -- el import de una unidad se filtra a la otra.** Dos archivos del paquete por omision,
cada uno con su `import` del mismo nombre simple:

| fuente | nuestro javac | JDK 25 |
|---|---|---|
| `UnaUtil.java` (`import java.util.List`) sola | compila ✓ | compila |
| `OtraAwt.java` (`import java.awt.List`) sola | compila ✓ | compila |
| **las dos juntas** | error en `UnaUtil` ✗ | compila |

El `import` de una gana la clave para las dos, y cual gana depende del orden en la linea de comandos.

**Donde esta.** `resolve_class_name` (`src/javac/enter.rs`) consulta `table.resolve_type(scope, name)`
--que ya trae los tipos del paquete-- antes que `imports.single`, y los alias del fuente
(`alias_source`, `src/javac/symbol.rs`) viven en un mapa **plano por nombre simple**, sin unidad. Es
la misma raiz que #491, que es la variante con los tipos del classpath.

**Lo que rompe.** Solo una compilacion **multi-unidad**; la biblioteca se compila de a un archivo y
ahi no se ve. Salio al agregar `java.awt.List`, que le puso un homonimo del paquete al
`import java.util.List` de siete archivos de `java.awt`. Mientras tanto: compilar de a un archivo, y
escribir `java.util.List` entero donde convivan los dos (`CardLayout`).

Repro: `repros/finding_493/`.

### #494 -- ✅ `java.lang.reflect.Array` medía completo y no servía para nada (VM)

Los diecisiete accesores --`getLength`, `get`, `set` y las catorce variantes tipadas-- estaban
declarados `native` y **ninguno tenia puente en la VM**: todos tiraban `UnsatisfiedLinkError`.
Tampoco lo tenia `multiNewArray`, asi que `newInstance(Class, int...)` moria igual. El unico que
funcionaba era `newArray`.

`java.lang.reflect` medía 318/318. Es el caso que la regla de la casa no cubre: un miembro que no
falta y no miente en su firma, pero que no se puede llamar.

**Como se destapo.** Escribiendo el mapeo de tipos abiertos de MXBean, que necesitaba recorrer un
arreglo de tipo desconocido. Lo primero fue esquivarlo --si la conversion no es la identidad el
componente es siempre de referencia, asi que alcanza con castear a `Object[]`-- y despues arreglarlo.

**El arreglo.** Cinco puertas internas en la VM (`getInt0`, `getLong0`, `getFloat0`, `getDouble0`,
`getRef0` y sus simetricas, mas `length0` y `multiNewArray`) y los diecisiete accesores publicos
reescritos **en Java** encima. Son cinco y no diecisiete porque el **ancho** de un elemento lo
decide el tipo del arreglo, que la VM ya conoce; lo que elige quien llama es la **forma** en que
quiere el valor.

Y los chequeos quedaron del lado Java: el nulo, el indice fuera de rango, el tipo que no
corresponde y las conversiones de ensanchamiento, que no son simetricas y son lo que se equivoca
facil --un `byte` entra en un `short[]`, un `char` no; un `byte[]` se lee como `int`, un `int[]` no
se lee como `byte`--. Es la decision que ya estaba tomada para `newArray`, cuyo comentario decia que
el largo negativo lo rechazaba el lado Java; **no lo rechazaba**, y un `newInstance(int.class, -1)`
devolvia un arreglo vacio en silencio. Ahora lo rechaza de verdad.

Prueba: `java/ArrayReflTest.java`, que da -1 contra el JDK 25, contra `run-headless`, y compilado
por nosotros corriendo en la JVM real con `-Xverify:all`.

### #496 -- ✅ una clase anidada `protected` salia con acceso de paquete en sus `access_flags`

```java
package p;
public class Outer {
    protected class Inner { }
}
package q;
public class Sub extends p.Outer {
    class SubInner extends Inner { }     // legal: Sub es subclase de Outer
}
```

Con el javac real anda. Con el nuestro, en la **JVM real**:

```
java.lang.IllegalAccessError: class q.Sub$SubInner cannot access its superclass p.Outer$Inner
```

**Donde estaba.** Los `access_flags` de clase (§4.1) no admiten `ACC_PROTECTED`: el acceso real de
un tipo anidado vive en su entrada de `InnerClasses` (§4.7.6). Eso ya se sabia, y por eso el emisor
le **sacaba** el bit a la anidada... y la dejaba con acceso de paquete:

| | nosotros | JDK 25 |
|---|---|---|
| `access_flags` de `Outer$Inner` | `ACC_SUPER` | `ACC_PUBLIC, ACC_SUPER` |
| `InnerClasses` de `Outer$Inner` | `protected` | `protected` |

Lo que faltaba es lo que hace javac: un anidado `protected` sale **`ACC_PUBLIC`** a nivel clase. No
es una aproximacion, es lo unico verificable: la JVM resuelve `protected` por la relacion de
subclase entre las **envolventes**, y esa relacion no esta en los `access_flags` de la anidada. Con
acceso de paquete, una subclase de la envolvente que viva en **otro paquete** no puede extender a
la anidada, que es exactamente para lo que existe un anidado `protected`.

**Lo que rompia.** Nada dentro de `java.awt`, y por eso no se vio antes: todos los
`AccessibleAWTXxx` que extienden a `Component.AccessibleAWTComponent` estan en el mismo paquete, y
ahi el acceso de paquete alcanza. Salio con el primer heredero **de afuera**: `java.applet.Applet.AccessibleApplet
extends java.awt.Panel.AccessibleAWTPanel`, `IllegalAccessError` en la JVM real a la primera
instancia. Nuestra VM no verifica acceso entre clases y lo dejaba pasar.

Arreglado en el calculo de `class_level` de `class_file` (`src/javac/codegen.rs`): si los
modificadores del fuente traen `protected`, se enciende `ACC_PUBLIC`. Se recompilaron los 27
fuentes de la biblioteca con anidadas `protected` (23 de `java.awt`, `java.awt.dnd`,
`java.beans.beancontext`, `java.applet`).

Repro: `repros/finding_496/`. Prueba: `java/BeansAwtTest.java`, que da -1 contra el JDK 25, contra
`run-headless`, y compilado por nosotros en la JVM real con `-Xverify:all`.

### #497 ⬜ -- el constructor implicito de una clase `public` se reporta como de paquete

```java
package q;
public class Sub { }               // sin constructor: el implicito es public (§8.8.9)

// en otro paquete
new q.Sub();                       // nuestro javac: "el metodo `Sub` es `de paquete` en `Sub`"
```

El JDK 25 compila. El nuestro lo rechaza en atribucion con *"el método `Sub` es `de paquete` en
`Sub` y no es accesible desde ..."*. Poner `public Sub() { }` a mano lo arregla, asi que el
sintoma es solo del constructor **implicito**.

**Donde esta, a medias.** El emisor ya lo sabe: `default_ctor` toma el acceso de la clase (§8.8.9),
y esta comentado como tal en `class_file`. Lo que no lo sabe es la **atribucion**: el simbolo del
constructor sintetizado se registra sin el acceso de la clase, y el chequeo de acceso lo lee como
de paquete. Es un desacuerdo entre dos pasadas sobre el mismo hecho.

**Lo que rompe.** Cualquier `new` desde otro paquete sobre una clase publica sin constructor
escrito. La biblioteca no lo sufre porque casi todas sus clases traen constructor explicito; salio
en el repro de #496, que no lo traia. Mientras tanto: escribir el constructor.

Repro: el `q/Sub.java` de `repros/finding_496/` sin su `public Sub() { }`.

