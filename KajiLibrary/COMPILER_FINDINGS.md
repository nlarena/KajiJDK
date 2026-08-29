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

### VM / interprete — 9

| # | Que |
|---|---|
| **#216** | `ConstantValue` no se aplica en la preparacion (§5.4.2) → todo `static final` primitivo lee 0 |
| **#217b** | el verificador **no rechaza** un `ireturn` en un metodo `()J` |
| **#220** | el comportamiento de un metodo depende del constant pool de **otros** metodos de la clase |
| **#225** | `invokeinterface` con receptor `String` revienta el interprete |
| **#226** | falta el nativo `String.valueOf([CII)` → no hay concatenacion en runtime |
| **#227** | `Throwable` de KajiLibrary sin `backtrace` → **toda** excepcion no capturada panica |
| **#229** | constantes `String` no-ASCII leidas como bytes UTF-8 crudos, un `char` por byte |
| **#230** | `invokevirtual` a un metodo resuelto `abstract` **no despacha** — bloqueante nº 1 |
| **#244** | enum **anidado** en paquete que `boot/` provee a medias → panic; y `unwind_with` panickea en vez de propagar |

### javac — codegen silencioso (lo mas peligroso: compila, no avisa, hace otra cosa) — 5

| # | Que |
|---|---|
| **#114** | concat sin sobrecarga de `append` se descarta; reaparece con `append(Object)` (ver #237) |
| **#217** | falta la ampliacion implicita `int`→`long`/`double` en 5 posiciones → bytecode invalido |
| **#219** | encadenar una llamada sobre el resultado de otra a un tipo del classpath → bytecode roto |
| **#247** | llamada a un metodo de una interfaz **anidada** del classpath: se descarta (`iconst_1; pop`) |
| **#248** | llamada generica estatica descartada; el llamador recibe **el argumento equivocado** |

### javac — perdida de modificadores en la emision — 5

| # | Que |
|---|---|
| **#110** | `ACC_STATIC` de un campo del classpath se lee y se tira → `getfield` sobre un static |
| **#115** | `ACC_VOLATILE` nunca se emite (**#236** lo amplia a `ACC_TRANSIENT`) |
| **#200** | `ACC_VARARGS` nunca se emite |
| **#238** | campo de interfaz sin `public static final` explicito → class file invalido |
| **#242** | tipo **miembro** de interfaz sin el `public` implicito (**#116**, lo mismo para metodos `static`) |

### javac — resolucion de nombres — 7

| # | Que |
|---|---|
| **#101** | nombre calificado de un tipo anidado no resuelve |
| **#208** | el `Signature` lleva el nombre anidado pelado — *corregido: no es el descriptor* |
| **#210** | calificado fuera de `java.lang` = error; **dentro de `java.lang` degrada a `Object` en silencio** |
| **#214** | `-cp` de varias entradas degrada los tipos a `Object`, sin diagnostico |
| **#239** | tipo anidado de otra unidad: 3 formas, 2 silenciosas — una **borra la clausula `implements`** |
| **#245** | `import X.*` + nombre simple: degrada a `Object` y ni siquiera deja `Signature` |
| **#249** | tipo anidado innombrable desde una clase hermana **del mismo archivo**, si hay `package` |

> Rodeo conocido: escribir el **nombre binario** (`Outer$Inner`) emite el descriptor exacto. El
> `javac` real hace lo mismo cuando la clase viene del classpath. Ver la novena tanda.

### javac — genericos e inferencia — 5

| # | Que |
|---|---|
| **#204 / #215** | la inferencia falla cuando el objetivo es una variable de tipo **del metodo** |
| **#211** | no se puede sobrescribir un metodo que devuelve **array** de un tipo fuera de `java.lang` |
| **#212** | el bound de un parametro de tipo va al `Signature` con el nombre simple (4 confirmaciones) |
| **#223** | `? super T` no sobrevive la captura |
| **#241** | la **borradura** de una variable acotada es `Object`, no su bound → `AbstractMethodError` |

### javac — chequeos que faltan — 3

| # | Que |
|---|---|
| **#104** | ignora el atributo `Exceptions` del classpath **y rechaza el override legal** con `throws` |
| **#213** | no verifica que una clase concreta implemente los abstractos de una **superclase** |
| **#222** | resolucion de sobrecarga: con un array elige `f(T)` en vez de `f(T[])` |

### javac — parser y literales — 4

| # | Que |
|---|---|
| **#209** | `int.class` / `void.class` no parsean |
| **#224** | (menor) `import IntStream;` sin paquete se acepta en silencio |
| **#228** | literal `char`/`String` con escape de sustituto se rechaza |
| **#232** | `-9223372036854775808L` → "literal long invalido" |

### javac — atributos, CLI y otros — 7

| # | Que |
|---|---|
| **#221** | retorno `A[]` no llama al generador: devuelve un array de longitud 0 |
| **#231** | `super.metodo()` no lo soporta el generador de bytecode |
| **#233** | no se emiten puentes para overrides covariantes **abstractos** (~25 miembros en `java.nio`) |
| **#234** | una invocacion con varios archivos no resuelve cruzado |
| **#235** | `SourceFile` incorrecto en clases secundarias y anidadas |
| **#240** | `--emit` con varios archivos **ignora todos menos el primero, en silencio** |
| **#243** | concat de un operando de tipo anidado → "la referencia a `append` es ambigua" |

### Biblioteca (KajiLibrary, no el compilador) — 5

| # | Que |
|---|---|
| **#201** | falta `synchronized` donde la spec lo exige (`Vector`, `Hashtable`, `StringBuffer`…) |
| **#202** | ~~falta `abstract`~~ — **RESUELTO**, 0 divergencias |
| **#203** | falta `final` en 48 miembros, incluido `System.out` |
| **#205** | `Map.putAll`, `Collection.stream` y `java.lang.ClassLoader` no existen |
| **#246** | **hay dos bibliotecas divergentes y la que se desarrolla no es la que corre** |

### Retirados

**#206** (tipos izados a top-level) y **#207** (hooks de test en la API): las clases y los miembros
señalados son **package-private**, o sea internos, y por la regla del contrato son libres. Ver
"La regla" mas abajo.

### Herramientas propias

- **`bin/jvm.exe --javap`** no imprime `transient` ni `volatile`, y **oculta los campos privados sin
  `-p`**. Para auditar modificadores hay que usar el `javap` del JDK.
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

- **#200 — ⬜ el javac no emite `ACC_VARARGS`.** 33 metodos en 15 clases. Aislado con una matriz
  cruzada, no inferido del agregado:

  | | `javap` real | `javap` nuestro |
  |---|---|---|
  | **javac real** | `f(String, Object...)` | `f(String, Object...)` |
  | **javac nuestro** | `f(String, Object[])` | `f(String, Object[])` |

  O sea: **el renderer esta bien, el emisor no marca el flag** (`ACC_VARARGS` = `0x0080` en
  `method_info.access_flags`, JVMS §4.6). Afectados: `String.format` (las dos sobrecargas),
  `PrintWriter.printf`/`format`, `ClassDesc.nested`, `MethodTypeDesc.of`/`insertParameterTypes`,
  `DynamicConstantDesc.of`/`ofNamed` + su constructor protegido, `DynamicCallSiteDesc.of`/`withArgs`,
  `MethodHandleDesc.of*`, y todo `ConstantBootstraps.*`.
  **Impacto:** nada compilado contra nuestra biblioteca puede llamarlos en forma varargs — hay que
  construir el array a mano. Repro: `repros/finding_200.java`.

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

- **#204 — ⬜ no infiere argumentos de tipo en dos llamadas a metodo generico estatico.** Rompe la
  compilacion de dos clases:
  - `java/util/Collections.java:29` — *"no se pueden inferir los argumentos de tipo de `swap`"*.
  - `java/util/Optional.java:86` — idem con `empty`.
  Misma familia que #15/#17 (genericos en interfaces funcionales), pero aca sobre metodos estaticos
  de la propia clase. **`Collections` y `Optional` hoy no compilan.**
  **Correccion de alcance (tanda 3):** `Optional.java` no recompila, pero el `Optional.class`
  commiteado **existe y funciona** — verificado en la VM que `findFirst`, `findAny`, `min`, `max` y
  `reduce` devuelven un `Optional` usable. El riesgo real no es "no se puede usar", es que **quien
  recompile `Optional.java` hoy se queda sin `.class`**.

- **#205 — ⬜ huecos de biblioteca que voltean la compilacion de terceros (NO es el compilador).**
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

- **#208 — ⬜ un tipo no resuelto aparece con *nombre simple pelado* en el atributo `Signature`
  cuando otro parametro de la misma firma lleva argumentos de tipo.**

  Matriz que aisla el disparador — el mismo parametro `Lookup`, cambiando lo que lo acompania:

  | Firma | Emitido |
  |---|---|
  | `f(Lookup l)` | descriptor `Ljava/lang/Object;` (#101) |
  | `f(Lookup l, String s)` | descriptor `Ljava/lang/Object;` (#101) |
  | **`f(Lookup l, Class<?> t)`** | descriptor `Ljava/lang/Object;` + **`Signature: LLookup;`** |

  El disparador es la **presencia de un tipo parametrizado en la firma**, no la posicion.
  **Correccion importante (tanda 9):** la primera version de este finding decia que el **descriptor**
  emitia `LLookup;` y que el class file referenciaba una clase inexistente. **Es falso**: el
  descriptor degrada a `Object` (#101) y el nombre roto vive en el `Signature`, que es lo que `javap`
  imprime. No hay ningun `Methodref` colgado. Sigue siendo un defecto — cualquier lector de genericos
  intenta resolver `LLookup;`, y es lo que hizo fallar la primera version de #206 — pero es menos
  grave de como se escribio.
  **Corolario para quien audite:** un nombre sin paquete en la salida de `javap` es sintoma de este
  defecto, **no** evidencia de que exista una clase top-level con ese nombre.
  **Rodeo (tanda 9):** escribir el **nombre binario** (`Outer$Inner`) emite el descriptor exacto.

- **#209 — ⬜ el literal de clase de un primitivo no parsea.** `int.class` y `void.class` dan
  *"error: se esperaba una expresion, se encontro Int"*; `Integer.class` compila bien.
  **Consecuencia concreta:** no hay ninguna expresion Java cuyo valor sea el mirror de un primitivo,
  asi que `MethodType.unwrap()` no se puede implementar — y el escape clasico, `Integer.TYPE`, esta
  declarado en el JDK justamente como `= int.class`. Repro: `repros/finding_209.java`.

### Segunda tanda: dogfooding de `java.lang.reflect` (2026-08-22)

Salieron de escribir el paquete `java.lang.reflect` con el javac congelado. **Los cinco primeros se
verificaron con repro propio antes de asentarlos.**

- **#210 — ⬜ los nombres calificados: fuera de `java.lang` es ERROR, dentro degrada a `Object` EN
  SILENCIO.** La primera version decia "solo resuelven para `java.lang.*`", basandose en que
  `java.lang.Integer ok;` compilaba. **Compilar no es resolver.** Verificado:

  ```java
  public java.lang.String simple(java.lang.String s) { return s; }   // -> (Ljava/lang/Object;)Ljava/lang/Object;
  public String           control(String s)          { return s; }   // -> (Ljava/lang/String;)Ljava/lang/String;
  ```

  El `javap` real confirma que el `.class` esta mal. Fuera de `java.lang` (`java.util.List`) da
  "no se encuentra el simbolo" incluso con el import exacto presente, y falla igual con paquetes
  propios. **Generaliza #101**, que estaba acotado a *tipos anidados* calificados.
  La mitad silenciosa es la peligrosa: **un override escrito con nombre calificado deja de
  sobreescribir sin decir nada.** Repro: `repros/finding_210.java`.

- **#211 — ⬜ no se puede sobrescribir ni redeclarar un metodo cuyo retorno es un ARRAY de un tipo
  fuera de `java.lang`.**

  ```java
  public interface D3a { Annotation[] get(); }
  public interface D3b extends D3a { Annotation[] get(); }
  // error: el retorno de `get` no es compatible con el de `D3a`:
  //        Annotation[] no es un subtipo de ?[]
  ```

  El componente del array, leido de vuelta del `.class` del supertipo, resuelve a `?`. Alcance
  medido: retornos array **si**, escalares **no**, parametros **no**, `java.lang.*` **no**. Aplica
  incluso a tipos del mismo paquete y con classpath de una sola entrada; falla en toda forma
  (`native` o con cuerpo, nombre simple o calificado).
  **Consecuencia:** `java.lang.reflect.AnnotatedElement` es **imposible de implementar** — por eso
  `Parameter` y `RecordComponent` no lo declaran, aunque en el JDK si: omitir `getAnnotations()` da
  "metodo abstracto sin implementar" y declararlo da error de compatibilidad de retorno. No hay
  programa legal en el medio.
  **Corolario (#211b):** de la misma raiz sale un **bridge sintetico espurio** —
  `Method.class`/`Constructor.class` traen un `public java.lang.Object getAnnotatedReturnType()` con
  `ACC_BRIDGE|ACC_SYNTHETIC` sin covarianza que lo justifique. Cosmetico, pero delata la misma falla.

- **#212 — ⬜ el bound de un parametro de tipo se escribe en `Signature` con el NOMBRE SIMPLE**, salvo
  que el tipo aparezca ademas en una posicion ordinaria del mismo archivo.

  ```java
  public interface ZZProbe { <T extends Annotation> T get(Class<T> c); }
  // Signature: <T:LAnnotation;>(Ljava/lang/Class<TT;>;)TT;     <-- MAL
  ```

  Agregar una linea `Annotation plain();` al mismo archivo lo corrige a
  `<T::Ljava/lang/annotation/Annotation;>` — y de paso arregla el marcador de bound-interfaz
  (`::` vs `:`). **Ya esta en el arbol:** `TypeVariable.class` lleva `<D:LGenericDeclaration;>`.
  **Cuatro confirmaciones independientes.** Precisiones acumuladas: (a) solo los bounds de
  `java.lang` salen bien (`<N extends Number>` correcto, `<A extends Annotation>` y
  `<E extends Element>` del mismo paquete, mal); (b) tambien pasa en el `Signature` **de clase**
  (`class G<T extends Bnd>`); (c) si el tipo **no existe en absoluto**, tampoco hay diagnostico:
  emite el nombre pelado, mismo sintoma que #208. Repro: `repros/finding_212.java`.

- **#213 — ⬜ no se verifica que una clase concreta implemente los metodos abstractos heredados de una
  SUPERCLASE.**

  ```java
  public abstract class AbsA { public abstract int f(); }
  public final class AbsB extends AbsA { }   // nuestro javac: compila sin decir nada
  ```

  Verificado: emite `AbsB.class` sin diagnostico. El `javac` real dice *"AbsB is not abstract and does
  not override abstract method f() in AbsA"*. Deja un **`AbstractMethodError` latente** en el class
  file. El chequeo **si** existe para interfaces implementadas directamente, pero no para superclases
  abstractas ni para interfaces heredadas via superclase.

- **#214 — ⬜ con `-cp` de mas de una entrada, los tipos del classpath degradan en silencio a
  `java.lang.Object`.**

  ```
  javac --emit -cp KajiLibrary          P.java   ->  ()Ljava/util/List;      correcto
  javac --emit -cp "KajiLibrary;otro"   P.java   ->  ()Ljava/lang/Object;    sin diagnostico
  ```

  Con `extends` ademas **desaparece el supertipo** del class file. No afecta el build de la
  biblioteca (usa una sola entrada), pero envenena cualquier setup de prueba con dos entradas — y al
  ser silencioso, se lee como un defecto del codigo bajo prueba.

- **#215 — ⬜ la inferencia falla cuando el argumento inferido es un parametro de tipo DEL METODO que
  envuelve.**

  ```java
  static <T> T id(T x) { return x; }
  static <A> A caller(A x) { return id(x); }   // error: no se pueden inferir los argumentos de tipo de `id`
  ```

  Anda si `A` es parametro de tipo *de la clase*, o si el argumento es concreto. **Probablemente la
  misma raiz que #204** — conviene atacarlos juntos. Forma equivalente con el objetivo generico:

  ```java
  class Opt<T> {
      private static final Opt<?> EMPTY = new Opt<Object>();
      static <X> Opt<X> empty() { return (Opt<X>) EMPTY; }
      <U> Opt<U> map()  { return empty(); }   // error de inferencia
      Opt<T> self()     { return empty(); }   // OK
  }
  ```

  **Workaround confirmado:** argumentos de tipo explicitos (`C.<A>id(x)`) resuelven; es lo que usa
  `Collectors` hoy.

- **#216 — 🔴 VM: el atributo `ConstantValue` no se aplica en la preparacion de la clase
  (JVMS §5.4.2), asi que todo `static final` primitivo lee 0.** No es del compilador: es del runtime.

  ```java
  public class KProbe {
      public static final int K = 7;
      public static int run() { return K; }
  }
  ```

  Verificado de punta a punta: el `.class` emitido lleva `ConstantValue: int 7`, y
  `run-headless KProbe.class run` devuelve **`Some(Int(0))`**.

  **Por que importa mas de lo que parece.** `docs/roadmap.md:225` tiene este item pendiente desde A4
  con esta justificacion: *"**no testeable con javac**: javac inlinea toda constante compile-time en
  el sitio de uso, asi que nunca se emite un `getstatic` que lo observe; requeriria class files a
  mano"*. **Esa premisa es falsa para NUESTRO javac**, que no pliega constantes (#112, cuyo fix no
  esta en este arbol y emite `getstatic`). O sea: dos huecos conocidos y tolerados por separado
  **se componen en una respuesta incorrecta silenciosa**.

  **Impacto medido en codigo ya publicado de KajiLibrary:** `Modifier.isPublic(1)` devuelve
  **`false`** — verificado corriendo —, y con el toda la familia `Modifier.isXxx`, porque compilan a
  `getstatic Modifier.PUBLIC`. Arreglar #112 **o** #216 lo tapa; corresponden los dos, y el item del
  roadmap deberia dejar de estar marcado como no testeable.
  **Ampliacion (tanda 4):** alcanza tambien a expresiones constantes **compuestas** (`0x40 | 0x80`).
  Repro: `repros/finding_216.java`.

### Tercera tanda: dogfooding de `java.util.stream` (2026-08-22)

- **#217 — 🔴 falta el ensanchamiento implicito `int` → `long`/`double`, y se emite `ireturn` en un
  metodo `()J`.** Es peor que "falta un `i2l`": el class file es **estructuralmente invalido**.

  ```java
  static int size = 3;
  static long asLong() { return Widen.size; }
  ```
  ```
  static long asLong();
    descriptor: ()J
       0: getstatic  #32   // Field size:I
       3: ireturn                          <-- ilegal en un metodo que devuelve long
  ```

  Verificado; en runtime da `compare: expected a long, found Int(3)`.
  **Alcance completo (tanda 4):** falta en **cinco posiciones** — `return`, inicializacion/asignacion
  de local, asignacion a campo, paso de argumento y `array store` — y tampoco emite `i2d`. Si
  funcionan la promocion numerica binaria (`n + 1L`) y el cast explicito. Afectaba a `count()` en las
  cuatro clases de `java.util.stream`.
  **Corolario (#217b), del runtime:** nuestro **verificador no lo rechaza**. `ireturn` exige que el
  tipo de retorno sea int/short/byte/char/boolean (JVMS §6.5); este class file pasa el verificador y
  revienta al ejecutarse. Es un agujero del verificador, no solo del emisor.
  Repro: `repros/finding_217.java`.

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

- **#219 — 🔴 encadenar una llamada sobre el resultado de otra llamada a un tipo del classpath produce
  bytecode roto.** Se manifiesta como *operand stack underflow* o como resultados vacios. Ligar el
  intermedio a un local lo arregla siempre. Casos reales encontrados: `mapper.apply(x).toArray()`
  (era la causa de que **`flatMap` no funcionara en absoluto**), `s.mapToObj(f).count()`,
  `s.findAny().getAsInt()`.

- **#220 — 🔴 VM: el comportamiento de un metodo depende de entradas del constant pool de OTROS
  metodos de la misma clase.** Dos archivos que difieren solo en un metodo **que nunca se llama**: sin
  el, `t2()` muere con `field_offset: field not found in the class or its superclasses`; con el
  metodo muerto agregado, `t2()` devuelve 2. Explica el "parpadeo" de que agregar o quitar una clase
  auxiliar cambiara que metodos no relacionados funcionaban.

- **#221 — ⬜ un retorno `A[]` (array de variable de tipo del metodo) no llama al generador.**
  `<A> A[] toArray(IntFunction<A[]> g)` compila, y en runtime el llamador recibe un array de longitud
  **0**, sin excepcion, incluso pasando el generador literal. Invocar el mismo generador directamente
  funciona. Por esto `Stream.toArray(IntFunction)` se saco del paquete en vez de dejarlo
  silenciosamente incorrecto.

- **#222 — ⬜ resolucion de sobrecarga `f(T[])` vs `f(T)`:** con un argumento array elige `f(T)`
  (con `T` = el tipo array) en vez de la mas especifica. Por eso **no** se agrego la sobrecarga
  `Stream.of(T)`: habria roto en silencio todo `Stream.of(unArray)` existente.

- **#223 — ⬜ `? super T` no sobrevive la captura.** Un parametro no se puede pasar a otro parametro
  del mismo tipo declarado; `? extends` si funciona.

  ```java
  class P<T> {
      void a(Consumer<? super T> x) { }
      void b(Consumer<? super T> x) { this.a(x); }
      // error: Consumer<cap#0 of Object> no se convierte a Consumer<? super T>
  }
  ```

- **#224 — ⬜ (menor) `import IntStream;`** — un import de un solo tipo sin paquete, sintaxis invalida,
  se acepta en silencio y el tipo resuelve igual. Deberia ser error.

#### Actualizaciones a findings existentes

- **#17 — ✅ ARREGLADO** (verificado en esa sesion). El retorno de variable de tipo de metodo
  "pelada" ya unifica en el chequeo de override, asi que `<R,A> R collect(Collector<? super T,A,R>)`
  compila. Era el que bloqueaba `Stream.collect(Collector)`, que ahora esta implementado.

### Cuarta tanda: dogfooding de `java.util.regex` (2026-08-22)

- **#225 — 🔴 VM: un `invokeinterface` con receptor `String` revienta el interprete.**

  ```java
  CharSequence cs = "abc";  cs.length();
  ```
  → panico `index out of bounds: the len is 0 but the index is 0` en `bytecode_interpreter.rs:2451`.
  **Refinamiento (tanda 5):** la primera lectura lo acoto a "cuando el destino resuelto es `native`";
  la sesion de `java.nio` lo reprodujo tambien con un metodo de **Java puro**, asi que la nativez
  **no** es el disparador. Con receptor `StringBuilder` anda.
  **Esto solo hace inejecutable todo `java.util.regex`**, porque `Matcher` opera sobre `CharSequence`.

- **#226 — 🔴 VM: falta la implementacion nativa de `java/lang/String.valueOf([CII)Ljava/lang/String;`.**
  Panican `StringBuilder.toString()`, `String.substring()` y la concatenacion `"a" + x`. O sea: hoy
  no se puede producir un `String` desde codigo de biblioteca.

- **#227 — 🔴 VM: `report_uncaught` (`athrow.rs`) lee un campo `backtrace` en la clase de la
  excepcion, y `KajiLibrary/java/lang/Throwable` solo declara `message`** → **cualquier** excepcion no
  capturada panica con `field_offset: field not found in the class or its superclasses`. Es una
  divergencia entre `boot/` (que si lo declara) y `KajiLibrary/`: la VM asume la forma de `boot/`.
  Ver #246.

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

- **#228 — ⬜ un literal `char`/`String` con escape de sustituto se rechaza.** Es Java valido y el
  propio JDK lo usa (`Character.MIN_HIGH_SURROGATE`). El escape del BMP compila; el del rango
  sustituto da "literal char invalido". El `javac` real acepta los dos. Rodeo: comparar contra
  `0xd800` numerico. Repro: `repros/finding_228.java`.

- **#229 — 🔴 VM: las constantes `String` no-ASCII se leen como bytes UTF-8 crudos, un `char` por
  byte.** El `.class` esta **bien** (pool verificado: `01 00 02 CC 81`); es la lectura de la VM la que
  no decodifica UTF-8 modificado. `"́".length()` devuelve **2**, deberia ser 1; el mismo escape
  en un literal `char` si funciona (769). Invalida en runtime cualquier dato no-ASCII en un `String`.
  Repro: `repros/finding_229.java`.

- **#230 — 🔴 VM: `invokevirtual` a un metodo resuelto `abstract` no despacha.** Panic
  `field_offset: field not found…` en `objects_operations.rs:410`. Llamar al mismo objeto por el
  **tipo concreto** anda. **Dos sesiones independientes lo encontraron** (`java.text` y `java.nio`),
  con A/B minimo: mismo cuerpo, misma clase, mismo tipo estatico; lo unico que cambia es si el metodo
  sobreescribe un `abstract`. Hipotesis: `metaspace.rs::build_vtable` hace
  `let Some(method) = resolve_method(...) else { continue }`, y `resolve_method` devuelve `None` para
  un metodo sin `Code`, asi que **un `abstract` declarado en una clase nunca recibe slot**.
  **Es el bloqueante nº 1 de `java.nio`** y de cualquier API basada en clases abstractas.
  *Anomalia sin explicar:* `java.text.DecimalFormat` escapa a esto, mientras que una gemela minima
  puesta en el mismo paquete falla.

- **#231 — ⬜ `super.metodo()` no lo soporta el generador de bytecode** (error explicito).
  `super(...)` de constructor si anda.

- **#232 — ⬜ `-9223372036854775808L` → "literal long invalido".** JLS §3.10.1 permite esa magnitud
  justamente como operando de menos unario.

- **#233 — ⬜ no se emiten puentes para overrides covariantes abstractos.** El `javac` real si los
  emite. Costo medido: ~25 miembros de `java.nio` (`Buffer slice()`, `duplicate()`, …). Los puentes de
  un override **concreto** si se emiten.

- **#234 — ⬜ una sola invocacion con varios archivos no resuelve cruzado.**
  `javac --emit A.java B.java` no le muestra `B` a `A`. Para dos clases que se referencian mutuamente
  hace falta un bootstrap en dos fases (compilar una con el cuerpo talado, compilar la otra,
  recompilar la primera). Se necesito tres veces en `java.text` y una en `javax.lang.model.type`.

- **#235 — ⬜ (menor) el `SourceFile` es incorrecto en clases secundarias y anidadas:** dice su propio
  nombre en vez del de la unidad de compilacion (`HeapByteBuffer.class` → "HeapByteBuffer.java";
  `JavaFileObject$Kind.class` → "Kind.java").

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

- **#236 — AMPLIA #115 a `transient`.** `volatile` descartado **ya estaba documentado en #115**, con
  la misma consecuencia sobre el JMM. Lo genuinamente nuevo es que **`ACC_TRANSIENT` (0x0080) tampoco
  se emite**, que #115 no menciona. Verificado con el `javap` **real** sobre nuestro `.class`:

  ```java
  public class Flags { transient int t; volatile int v; int plain; }
  ```
  ```
  int t;      flags: (0x0000)      <-- falta ACC_TRANSIENT
  int v;      flags: (0x0000)      <-- falta ACC_VOLATILE (#115)
  ```

  `volatile` es el grave: la VM **tiene** el soporte implementado (`Acquire`/`Release`, ver
  `docs/H4_memory_model.md`) y **nunca se activa**, porque el flag no llega al class file. Es el
  tercer modificador que se pierde en la emision, junto con `ACC_VARARGS` (#200) y el `ACC_STATIC`
  del classpath (#110). (`strictfp` tampoco emite `ACC_STRICT`, pero eso **es correcto** desde
  Java 17.)

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

- **#238 — 🔴 un campo de interfaz sin `public static final` explicito produce un class file
  invalido.** JLS §9.3 dice que esos modificadores son **implicitos**; el javac no los aplica.

  ```java
  public interface IfaceField { long NOPOS = -1L; }
  ```

  Verificado con el `javap` **real** sobre nuestro `.class` — tres violaciones a la vez:
  ```
  long NOPOS;
    flags: (0x0000)                        <-- ni public, ni static, ni final; sin ConstantValue
  public default p.IfaceField();           <-- un <init>()V DENTRO de una interfaz (ilegal)
         7: putstatic  #36  // Field NOPOS:J   <-- putstatic sobre un campo no estatico
  ```
  Escribir `public static final long NOPOS = -1L;` sale correcto (`0x0019` + `ConstantValue`).
  Es el defecto mas serio de esa tanda: produce clases que un verificador debe rechazar, sin ruido.
  Repro: `repros/finding_238.java`.

- **#239 — 🔴 un tipo ANIDADO declarado en otra unidad de compilacion es innombrable, en las tres
  formas, y dos de ellas fallan en silencio.** Con `p/Outer.class` y `p/Outer$Kind.class` en el cp:

  | Forma | Resultado |
  |---|---|
  | `Outer.Kind pick();` | **error duro** — `no se encuentra el simbolo` (#101) |
  | `import p.Outer.Kind;` + `Kind pick();` | compila → emite `java.lang.Object pick()` |
  | `import p.Outer2.Marker;` + `class X implements Marker {}` | compila → **la clausula `implements` DESAPARECE del class file** |
  | `import java.util.Map.Entry;` + `Entry<K,V> pick();` | compila → `Signature` con `LEntry;` (#208) |

  Verificada la tercera fila, que es la peor: `public class UsesMarker implements Marker {}` emite
  `public class p.UsesMarker {` a secas. **Un tipo deja de implementar una interfaz sin que nadie
  avise.** Dentro de la misma unidad de compilacion resuelve bien. Costo medido en `javax.tools`:
  14 metodos de `ForwardingJavaFileManager`, 4 de `JavaFileManager`, 2 de `ForwardingJavaFileObject`
  y dos clausulas `implements`.
  **Rodeo (tanda 9):** el **nombre binario** (`Outer$Inner`) resuelve y emite el descriptor exacto.

- **#240 — 🔴 `--emit` con varios archivos ignora los que siguen al primero, en silencio.** Amplia
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

- **#241 — 🔴 la borradura de una variable de tipo acotada es `Object`, no su bound.**

  ```java
  public interface ZB { <N extends Number> N f(Class<N> c); }
  ```
  ```
  descriptor: (Ljava/lang/Class;)Ljava/lang/Object;      <-- deberia ser Ljava/lang/Number;
  Signature:  <N:Ljava/lang/Number;>(Ljava/lang/Class<TN;>;)TN;    <-- este SI esta bien
  ```

  Contraste con el JDK real: `AnnotatedConstruct.getAnnotation` emite
  `(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;`.
  **Notar que el `Signature` es correcto y el descriptor no**: son dos caminos distintos y solo uno
  aplica el bound. Es grave porque un override escrito con la borradura correcta tendria **otro
  descriptor** → no sobreescribe → `AbstractMethodError` en runtime.
  Distinto de #212, que es sobre el `Signature`; aca el `Signature` esta bien.
  Repro: `repros/finding_241.java`.

- **#242 — ⬜ un tipo MIEMBRO de una interfaz no recibe el `public` implicito (JLS §9.5).** Hermano
  de **#116**, que cubre solo metodos `static`.

  ```java
  public interface ZO { interface In { } }
  ```
  → `javap` real sobre nuestro `.class`: `interface p.ZO$In {` (package-private); el JDK da
  `public interface`. Escribirlo `public interface In { }` lo arregla — es lo que hubo que hacer en
  `ModuleElement`, o sus 8 tipos anidados quedaban inusables desde otro paquete.
  Repro: `repros/finding_242.java`.

- **#243 — ⬜ concatenar un operando cuyo tipo estatico es un tipo anidado de otra unidad de
  compilacion da error duro `la referencia a 'append' es ambigua`.** Contraparte ruidosa de #114 y
  pariente de #239: el tipo no resuelve, y la resolucion de sobrecarga de `append` queda ambigua en
  vez de reportar el tipo. `"x" + i.toString()` falla igual.

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

**No es una invencion nuestra:** el `javac` del JDK 25 hace lo mismo cuando la clase esta en el
classpath y no en la misma invocacion — y el build de KajiLibrary es exactamente ese caso
(`--emit -cp KajiLibrary`, un archivo por vez, que ademas es lo unico que permite #240).
Queda documentado en `MethodHandles.java` con la instruccion de sacarlo cuando se arregle #101.
**Prueba de que es un arreglo real y no un truco de metrica:** el `BootstrapMethods` de una lambda
emite `metafactory:(Ljava/lang/invoke/MethodHandles$Lookup;…)`; antes la declaracion decia
`(Ljava/lang/Object;…)` y el call site apuntaba a un metodo que la clase no tenia. Ahora coinciden.

#### Defectos nuevos

- **#244 — 🔴 VM: un enum ANIDADO en un paquete que `boot/` provee parcialmente no se puede
  inicializar; la VM panickea.** Preexistente, no documentado.
  `panicked at objects_operations.rs:410: field_offset: field not found`, con el stack pasando por
  **`unwind_with`** — o sea que **se rompe desenrollando una excepcion**.

  Matriz que aisla el disparador (⛔ = panic):

  | Caso | ¿el paquete lo provee `boot/`? | ¿la clase esta en `boot/`? | |
  |---|---|---|---|
  | enum anidado, paquete default | no | — | ✅ |
  | `java.text.Normalizer$Form` | no | — | ✅ |
  | `javax.tools.JavaFileObject$Kind` | no | — | ✅ |
  | `java.util.concurrent.TimeUnit` (top-level) | si | no | ✅ |
  | `java.lang.Thread$State` | si | si | ✅ |
  | `java.lang.invoke.ZzProbe2$N` (**clase**, no enum) | si | no | ✅ |
  | `java.lang.constant.DirectMethodHandleDesc$Kind` | si | no | ⛔ |
  | `java.lang.invoke.VarHandle$AccessMode` | si | no | ⛔ |

  Es especifico de **enum** + **anidado** + **paquete provisto a medias por `boot/`**. La clase
  hermana no-enum del mismo archivo corre. Consecuencia: los dos enums nuevos del paquete
  (`VarHandle$AccessMode`, `Lookup$ClassOption`) tienen el bytecode verificado (`<clinit>` completo,
  31 `putstatic`, `$VALUES`, `values()`/`valueOf()`) pero **no se pueden ejercitar en la VM hoy**.
  Segundo sintoma independiente: **`unwind_with` panickea en vez de propagar**.

- **#245 — ⬜ `import p.Outer.*;` + nombre simple degrada a `Object` SIN emitir `Signature`.** La otra
  forma (`import p.Outer.Inner;`) al menos deja el rastro en la firma generica (#208); esta no deja
  nada. Es la degradacion mas silenciosa de la familia #101/#208/#239.

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

- **#249 — ⬜ un tipo anidado de una interfaz no se puede nombrar desde una clase top-level hermana
  del MISMO archivo, si el archivo esta en un paquete con nombre.**
  ```java
  package pk;
  public interface PA { interface Builder { PA build(); } }
  final class PABuilder implements PA.Builder { public PA build() { return null; } }
  // error: no se encuentra el simbolo: PA.Builder
  ```
  Sin `package` (paquete por defecto) el mismo codigo compila. Rodeo: anidar la implementacion
  dentro de la interfaz.

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
