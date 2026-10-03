# Compiler findings from dogfooding KajiLibrary

Bugs and gaps in the own `javac` (`src/javac/`) surfaced by compiling KajiLibrary with it.
The library is compiled with the frozen snapshot `bin/javac.exe` (`--emit X.java` -> `X.class`);
each compile failure --- or each `.class` that is structurally wrong yet passes `javap` --- is a
real compiler defect. These two files are the handoff to the **compiler session** (the library
session does not touch `src/javac/`).

**This file is the index: the status, how to read, and one row per finding.** The full record ---
every entry with its reproducer, its measurement and its closing note --- lives in
**[`findings-details.md`](findings-details.md)**. They were split on 2026-10-03, when the single
file reached 17 thousand lines. Nothing is in both.

> The index rows and the section list below are **in Spanish, deliberately**: each row carries its
> entry's own title and the list carries the record's own headings. Translating them would unhook
> them from what they point at.

---

## Status (2026-10-03)

**278 findings, #1-#559. Open: #307, #505, #559, plus #288 with no working reproducer.**

| # | where | what | how it is checked |
|---|---|---|---|
| **#307** | javac | inference does not look at the lambda body (JLS §18.2.1) | `Optional.of("perro").map(x -> x.toUpperCase()).orElseThrow().length()` does not compile: *no se encuentra el metodo: length / ubicacion: clase Object* |
| **#505** | VM/library | `Field.getAnnotations()` always returns empty | with a `@Retention(RUNTIME)` annotation on a field: ours gives 0 / `null` / `false`, JDK 25 gives 1 / `@Mk505("campo")` / `true` |
| **#559** | javac | a handler's frame carries the type of a **dead** local | `SmProbe` runs and answers 2 on our VM; HotSpot refuses the class with `VerifyError: Stack map does not match the one at exception handler 55` |
| **#288** | VM/GC | a stale reference reaching the GC in green mode | no reproducer fires today (see the `os-parallel` note); stays open until somebody looks at it with ThreadSanitizer |

The first three were **reproduced on 2026-10-03** with the frozen javac --- #505 against JDK 25 as
the oracle, #559 against HotSpot. The six the entries still called open and that were not ---
#113, #127, #474, #500, #502, #509 --- were measured the same day and are closed.

> **Do not trust the title.** Three of those six carried their own *Cerrado el...* note **inside**
> the `Open` section, and **#113 had no marker at all**, so no marker-based scan can see it. And the
> other way round: #307 says `**No arreglado**`, which contains the word *arreglado* (*fixed*), so a
> keyword scan counts it as closed. Both traps were paid for on 2026-10-03, one in each direction.

## How to read these two files

**The record is ordered chronologically by session, not by component**, and the same `#NNN` comes
back across several batches --- definition, confirmations, corrections. **A finding's canonical
entry is its first appearance**; the later ones say so. In a chronological log it is the **last**
word on a number that counts, not the first.

**To reach an entry:** find the `#NNN` in the index below --- it names the section --- and search
for `#NNN` in [`findings-details.md`](findings-details.md).

**Legend --- the six forms the file actually uses:**

| form | means |
|---|---|
| `✅` | closed (in `src/javac` + regression test, folded into the snapshot) |
| `⬜` | open |
| `⬜→✅` / `🔴→✅` | open and later closed; the entry carries the closing note |
| `🟡` | partial |
| `🔴` | **severity**, not status: set when it was raised and **not removed on closing** |
| *no marker* | **means nothing at all**: there are open and closed entries with none. Read the entry |

**Numbering.** Findings raised by the **library session are numbered from #100 up**; their
reproducers are `KajiLibrary/repros/finding_NNN/`. The **#2xx range** starts with the API diff sweep
against OpenJDK 25 (2026-08-22). Before claiming a new number, check the highest one in the index
below **and the content of the previous ones**: that sweep duplicated two (#236 was #115, #237 was
#114) precisely by skipping that step. The fourteen remaining collisions were resolved on 2026-09-06
by moving the second finding of each number into the 523+ series --- see *La renumeracion*.

**Versioned reproducers:** `KajiLibrary/repros/finding_NNN/`, one per finding, self-contained. Run
them from the repo root: `bin/javac.exe --emit -cp KajiLibrary <repro>`. The binary prints
diagnostics but **exits 0**, so judge by the message, not by the exit code.

## Index by number

Generated from the record, not written by hand: **a finding's canonical entry is its first
appearance**, and that is the one listed here. The later ones are confirmations, delimitations or
the closing note, and they say so. The *section* column is the `##` of
[`findings-details.md`](findings-details.md) that contains it. `·medido 2026-10-03` marks the nine
that were reproduced that day --- about the rest, all that is known is what the entry claims.

| # | status | what | section |
|---|---|---|---|
| #1 | ✅ | `native` emitted as `abstract` | Fixed |
| #2 | ✅ | `long` hex literal with the high bit rejected | Fixed |
| #3 | ✅ | constant int→byte/short/char narrowing (JLS §5.2) | Fixed |
| #4 | ✅ | finder doesn't auto-load an unqualified same-package classpath type | Fixed |
| #5 | ✅ | a source core type's identity vs the external one in the override check | Fixed |
| #6 | ✅ | `java.lang.Object` emitted with `super_class` pointing at itself | Fixed |
| #7 | ✅ | no classpath, so KajiLibrary can't build against its own types | Fixed |
| #8 | ✅ | no abstract-method-completeness check | Fixed |
| #9 | ✅ | generic override covariant-return doesn't substitute type variables | Fixed |
| #10 | ✅ | capturing anonymous classes are miscompiled | Fixed |
| #11 | ✅ | static-method calls on `java.util` utility classes don't resolve | Fixed |
| #12 | ✅ | an enum nested in an interface is miscompiled + enum class/field flags | Fixed |
| #13 | ✅ | FIXED. enclosing-instance capture is broken for a class declared inside a *generic* class. | Fixed |
| #14 | ✅ | FIXED (by the #15 fix; same root cause) | Fixed |
| #15 | ✅ | FIXED. a method inherited through a *generic* superinterface doesn't resolve. | Fixed |
| #16 | ✅ | FIXED. a lambda argument to a generic constructor isn't supported by codegen | Fixed |
| #17 | ✅ | FIXED. generic override doesn't unify a *method*-level type variable in the return. | Fixed |
| #18 | ✅ | FIXED (by the #21 fix; same root cause) | Fixed |
| #19 | ✅ | FIXED (performance). the `-cp` finder is pathologically slow when a compiled type is also | Fixed |
| #20 | ✅ | FIXED. a `new` with a fully-qualified class name is miscompiled | Fixed |
| #21 | ✅ | FIXED: the compiler dropped ALL enum machinery for an enum in a *named package* | Fixed |
| #22 | ✅ | FIXED. method resolution on `this` ignores ALL inherited methods (superclass *and* interfaces). | Fixed |
| #100 | ✅ | / #241 — ✅ la borradura de una variable de tipo del METODO era `Object`, no su cota. | Fixed |
| #101 | ✅ | FIXED (verificado 2026-08-24; ver la revision con el snapshot congelado) | Fixed |
| #102 | ✅ | a call to a method returning an array of a *cross-package* reference type is generated | Open *(historico)* --- vaciada el 2026-10-0… |
| #103 | ✅ | missing `int`→`long` widening (`i2l`) on assignment and method arguments | Open *(historico)* --- vaciada el 2026-10-0… |
| #104 | ✅ | / #256 - OK: el atributo `Exceptions` ya se LEE | Fixed |
| #105 | ✅ | `monitorexit` is not emitted on an early `return` inside a `synchronized` block | Open *(historico)* --- vaciada el 2026-10-0… |
| #108 | ✅ | a chained call through an INTERFACE-typed intermediate is silently dropped (and | Priority *(historico, 2026-08-24)* --- habl… |
| #109 | ✅ | FIXED (2026-08-24). `attribute.rs` clasificaba el ternario en dos formas y §15.25 | Priority *(historico, 2026-08-24)* --- habl… |
| #110 | ✅ | FIXED (library session, 2026-08-18) — a STATIC field of a *classpath* class was read | Priority *(historico, 2026-08-24)* --- habl… |
| #111 | ✅ | ARREGLADO (2026-08-24). En `attribute.rs`, la resolucion de una llamada busca los | Priority *(historico, 2026-08-24)* --- habl… |
| #112 | ✅ | FIXED (library session, 2026-08-18) — a `static final` primitive constant was neither | Priority *(historico, 2026-08-24)* --- habl… |
| #113 | ✅ | the enhanced-`for` desugaring erases a generic return type to `Object` instead of to its eras… | Open *(historico)* --- vaciada el 2026-10-0… ·medido 2026-10-03 |
| #114 | ✅ | la concatenacion ya no depende de las sobrecargas de `append` | Fixed |
| #115 | ✅ | / #236 — ✅ `ACC_VOLATILE` y `ACC_TRANSIENT` ya se emiten | Fixed |
| #116 | ✅ | FIXED (verificado 2026-08-24; ver la revision con el snapshot congelado). | Fixed |
| #118 | ✅ | the varargs flag is never emitted, and a spread call to a *classpath* varargs method is | Priority *(historico, 2026-08-24)* --- habl… |
| #119 | ✅ | ARREGLADO (2026-08-24), y con el se cerraron #120 y #126. La causa era un atajo de | Priority *(historico, 2026-08-24)* --- habl… |
| #120 | ✅ | cae con el mismo fix: `viaSubclass` emite | Priority *(historico, 2026-08-24)* --- habl… |
| #121 | ✅ | FIXED (2026-08-24). No era un hueco del emisor, aunque el mensaje dijera "el generador | Priority *(historico, 2026-08-24)* --- habl… |
| #122 | ✅ | FIXED (2026-08-24). `attribute::candidates` ya deduplicaba, pero comparando los | Priority *(historico, 2026-08-24)* --- habl… |
| #123 | ✅ | un override covariante se rechaza cuando el retorno lleva un COMODIN | La regla: el contrato es publico + protecte… |
| #124 | ✅ | un campo inicializado en una interfaz sintetizaba un constructor y perdia el `public static f… | La regla: el contrato es publico + protecte… |
| #126 | ✅ | tambien: desaparece el puente espurio `public Object getTarget()` que se emitia de | Priority *(historico, 2026-08-24)* --- habl… |
| #127 | ✅ | un `default` NEGATIVO de una anotacion se descarta en silencio | jakarta.persistence — cerrado en 203/203, c… ·medido 2026-10-03 |
| #128 | ✅ | no hay forma de escribir un caracter ASTRAL en una constante | jakarta.persistence — cerrado en 203/203, c… |
| #200 | ✅ | / #118 — ✅ `ACC_VARARGS` se emite | Fixed |
| #201 | ✅ | parcial: 43 metodos, y el reporte tenia numeros de otro JDK | Fixed |
| #202 | ✅ | RESUELTO. Faltaba `abstract` en 30 metodos, 18 de ellos en `java.nio.ByteBuffer`. | Barrida de diff de API contra OpenJDK 25 —… |
| #203 | ✅ | parcial: `System.out`, y por que el resto no se puede barrer por nombre | Fixed |
| #204 | ✅ | / #215 — ✅ un parametro de tipo DEL METODO no resolvia dentro de su propio cuerpo. | Fixed |
| #205 | ✅ | RESUELTO: los 3 huecos cerrados, los 4 archivos compilan. | Barrida de diff de API contra OpenJDK 25 —… |
| #206 | retirado | RETIRADO. Ver "La regla" al final: las 11 clases que listaba como "izadas a top-level" | Barrida de diff de API contra OpenJDK 25 —… |
| #207 | retirado | RETIRADO. Ver "La regla": `TreeMap.checkInvariants()` y `Vector.countUnlocked()` son | Barrida de diff de API contra OpenJDK 25 —… |
| #208 | ✅ | cerrado (2026-08-25): el generador ya no inventa un tipo que no resolvio | Fixed |
| #209 | ✅ | OK: `int.class` / `void.class` / `int[].class` | Fixed |
| #216 | ✅ | `ConstantValue` se aplica | Fixed |
| #217 | ✅ | / #217b — ✅ la ampliacion primitiva implicita, y el verificador que la dejaba pasar. | Fixed |
| #218 | ✅ | una lambda pasada como ARGUMENTO a un metodo cuyo tipo declarante vino de un `.class` | Barrida de diff de API contra OpenJDK 25 —… |
| #219 | ✅ | encadenar sobre una llamada a un tipo del classpath ya emite el bytecode correcto | Fixed |
| #220 | ✅ | y #244 — ✅ no eran de la VM: eran #110, el compilador. | Fixed |
| #221 | ✅ | OK: un retorno `A[]` ya llama al generador | Fixed |
| #224 | ✅ | OK: `import IntStream;` es error | Fixed |
| #225 | ✅ | `invokeinterface` con receptor `String` anda | Fixed |
| #226 | ✅ | el nativo `String.valueOf([CII)` estaba implementado; el finding quedo desactualizado. | Fixed |
| #227 | ✅ | una excepcion no capturada ya no panica | Fixed |
| #228 | ✅ | OK: el escape de sustituto | Fixed |
| #229 | ✅ | las constantes `String` no-ASCII se leen bien | Fixed |
| #230 | ✅ | CERRADO Y VERIFICADO EN LA VM (2026-08-24) | Barrida de diff de API contra OpenJDK 25 —… |
| #231 | ✅ | / #125 — ✅ la mitad del COMPILADOR: `super.metodo()` ya emite bytecode, y el correcto. | Fixed |
| #232 | ✅ | `-9223372036854775808L` ya compila | Fixed |
| #233 | ✅ | los overrides covariantes ABSTRACTOS ya reciben su puente | Fixed |
| #234 | ✅ | una invocacion con varios archivos resuelve cruzado | Fixed |
| #235 | ✅ | OK: el `SourceFile` es el de la unidad | Fixed |
| #237 | ✅ | ES #114, que reaparece con `append(Object)` | Barrida de diff de API contra OpenJDK 25 —… |
| #238 | ✅ | y #124 — ✅ los dos venian de mirar los modificadores DECLARADOS de un campo de interfaz. | Fixed |
| #239 | ✅ | / #245 - OK: un tipo ANIDADO de otra unidad ya es nombrable en las cuatro formas | Fixed |
| #240 | ✅ | ARREGLADO (2026-08-24). `src/bin/javac.rs` tomaba un solo archivo (`args.get(1)`) y | Barrida de diff de API contra OpenJDK 25 —… |
| #242 | ✅ | un tipo miembro de una interfaz es implicitamente `public` (§9.5) | Fixed |
| #246 | ✅ | RESUELTO: la que se desarrolla es la que corre. | La regla: el contrato es publico + protecte… |
| #247 | ✅ | toda llamada a un metodo de una interfaz ANIDADA cargada del classpath se descarta en | La regla: el contrato es publico + protecte… |
| #248 | ✅ | una llamada generica estatica se descarta en silencio cuando la inferencia tiene que | La regla: el contrato es publico + protecte… |
| #250 | ✅ | ARREGLADO Y MEDIDO (2026-08-24) | La regla: el contrato es publico + protecte… |
| #251 | ✅ | ARREGLADO (2026-08-24), y se llevo puestos a #247 y #108. Causa raiz en | La regla: el contrato es publico + protecte… |
| #252 | ✅ | una llamada cuyo receptor tiene tipo de VARIABLE DE TIPO ya no se descarta. | Fixed |
| #253 | ✅ | / #264 - OK: un argumento con COMODIN ya se puede pasar a un parametro de ese mismo tipo. | Fixed |
| #254 | ✅ | llamar a un metodo que la clase sobreescribe de una interfaz parametrizada ya no es | Fixed |
| #255 | ✅ | `ACC_SYNCHRONIZED` no se emitia | Fixed |
| #257 | ✅ | un `catch` que se dispara ya corre su `finally` y saltea el handler | Fixed |
| #258 | ✅ | era el mismo bug que #261, encontrado en paralelo | Fixed |
| #259 | ✅ | el pool se escribia en UTF-8 estandar y no en UTF-8 MODIFICADO (§4.4.7). | Fixed |
| #260 | ✅ | OK: el condicional ya no colapsa `char`/`byte`/`short` a `int` | Fixed |
| #261 | ✅ | un array no era un tipo referencia, y una llamada sin resolver se emitia como NADA. | Fixed |
| #262 | ✅ | OK: los miembros que un array hereda de `Object` | Fixed |
| #263 | ✅ | OK: un receptor de tipo variable con cota externa resuelve sus miembros | Fixed |
| #265 | ✅ | OK: la resolucion sube por la jerarquia | Fixed |
| #266 | ✅ | OK: los dos bugs de fuente | Fixed |
| #267 | ✅ | OK: los cuatro `import`, y la cadena que destaparon | Fixed |
| #268 | ✅ | el cast sintetico que faltaba, y por que el gate propio no podia verlo | Fixed |
| #269 | ✅ | VM: `System.arraycopy` supone que todo elemento mide cuatro bytes | Tanda 2026-08-24 (g) -- `StringBuilder` y `… |
| #270 | ✅ | `Void.TYPE` | Tanda 2026-08-24 (h) -- `java.lang.Class`:… |
| #271 | ✅ | `String[].class` no parseaba | Tanda 2026-08-24 (h) -- `java.lang.Class`:… |
| #272 | ✅ | VM: `invokespecial` no preguntaba si el metodo tiene cuerpo | Tanda 2026-08-24 (h) -- `java.lang.Class`:… |
| #273 | ✅ | el corpus de probes tenia SEIS nombres de clase en colision | Tanda 2026-08-24 (h) -- `java.lang.Class`:… |
| #274 | ✅ | un nombre CALIFICADO no se reconocia como tipo dentro de una expresion | Tanda 2026-08-24 (i) -- `java.lang.reflect.… |
| #275 | ✅ | las caches de wrapper habian desaparecido, y JLS 5.1.7 las EXIGE | Tanda 2026-08-24 (j) -- los wrappers, y dos… |
| #276 | ✅ | `ArrayIndexOutOfBoundsException` no descendia de `IndexOutOfBoundsException` | Tanda 2026-08-24 (j) -- los wrappers, y dos… |
| #277 | ✅ | el Filer registraba un offset que la colecta invalidaba | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #278 | ✅ | el `inner_name` de una clase LOCAL llevaba el prefijo numerico | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #279 | ✅ | una llamada con un argumento `T[]` se declara ambigua | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #280 | no existio | no existio: era el script que escribia el metodo en la clase equivocada | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #281 | ✅ | javac no puede *crear* arreglos multidimensionales (el tipo si existe, y la VM ya emite `mult… | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #282 | ✅ | la concatenacion de una referencia no-String produce basura en silencio (y con dos, lee fuera… | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #283 | ✅ | un local declarado dentro de un `synchronized` pisaba el slot del monitor | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #284 | ✅ | no se chequea un metodo abstracto heredado de una superclase del classpath | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #285 | ✅ | una llamada generica anidada como argumento no resuelve si el destino es un generico parametr… | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #286 | ✅ | un lambda en linea no liga su parametro si el destino es `X<? super E>` con E del receptor | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #287 | ✅ | una llamada estatica calificada, hecha desde una interfaz con un homonimo, ignora el calificador | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #288 | ⬜? | referencia stale que llega al GC en modo verde: `young_info` sin la clave | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #289 | ✅ | falta el autoboxing en un inicializador de arreglo de tipo referencia | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #290 | ✅ | un arreglo de primitivos se consideraba convertible a `T[]` | Tanda 2026-08-24 (l) -- `Class` y `ClassLoa… |
| #291 | ✅ | (biblioteca) `java.lang.Iterable` no tenia `forEach` | Tanda 2026-08-29 (m) -- `Collections` compl… |
| #292 | ✅ | (biblioteca) `AbstractList` y `AbstractMap` no tenian `equals` ni `hashCode` | Tanda 2026-08-29 (m) -- `Collections` compl… |
| #293 | ✅ | un `new` con argumentos que no matchean ningún constructor compilaba igual | Tanda 2026-08-29 (n) -- `TreeMap`/`TreeSet`… |
| #294 | ✅ | una firma del classpath que nombra una clase que se está compilando no unificaba | Tanda 2026-08-29 (n) -- `TreeMap`/`TreeSet`… |
| #295 | ✅ | una clase anidada en una interfaz se trataba como interna de instancia | Tanda 2026-08-29 (n) -- `TreeMap`/`TreeSet`… |
| #296 | ✅ | `Thread.sleep(ms)` cuenta pasos de bytecode, no milisegundos | Tanda 2026-08-29 (p) -- las tres que faltab… |
| #297 | ✅ | un escape ilegal en un literal se aceptaba, tragándose la barra | Tanda 2026-08-29 (p) -- las tres que faltab… |
| #298 | ✅ | un arreglo pasado a un varargs genérico se envolvía en otro arreglo | Tanda 2026-08-29 (p) -- las tres que faltab… |
| #299 | ✅ | un tipo del mismo paquete perdía el nombre simple contra un homónimo | Tanda 2026-08-29 (p) -- las tres que faltab… |
| #300 | ✅ | un tipo calificado en un `catch` no resolvía, y el handler se descartaba | Tanda 2026-08-30 (q) -- cerrando findings,… |
| #301 | ✅ | `super.m()` sobre una superclase genérica no sustituía sus argumentos de tipo | Tanda 2026-08-30 (q) -- cerrando findings,… |
| #302 | ✅ | el hash de identidad cambiaba cuando el GC movía el objeto | Tanda 2026-08-30 (r) -- el cierre chico, y… |
| #303 | ✅ | un tipo de `java.lang` del mismo round no lo veía el import implícito | Tanda: Optional, Random, SplittableRandom —… |
| #304 | ✅ | el diagnóstico señalaba el archivo equivocado | Tanda: Optional, Random, SplittableRandom —… |
| #305 | ✅ | `null` era aplicable a un parámetro primitivo | Tanda: Optional, Random, SplittableRandom —… |
| #306 | ✅ | una lambda hacia un genérico de otra clase no compilaba | Tanda: Optional, Random, SplittableRandom —… |
| #307 | ⬜ | la inferencia no mira el cuerpo de la lambda | Tanda: Optional, Random, SplittableRandom —… ·medido 2026-10-03 |
| #308 | ✅ | `() -> c[1]++` era un no-op | Tanda: Optional, Random, SplittableRandom —… |
| #309 | ✅ | un `++` sobre un campo, en posición de valor, no emitía nada | `RandomGenerator` al 100 %, y el finding qu… |
| #310 | ✅ | un primitivo pasado a un `Object...` no se boxeaba | Tanda: el merge de `java.lang`, `java.util`… |
| #311 | ✅ | la referencia de una lambda quedaba stale si el GC corría durante su constructor | Tanda: el merge de `java.lang`, `java.util`… |
| #312 | ✅ | un `import` explícito del mismo round no resolvía en posición de expresión | Tanda: el merge de `java.lang`, `java.util`… |
| #313 | ✅ | `this.toString()` dentro de un `default` de interfaz no resolvía | Tanda: el merge de `java.lang`, `java.util`… |
| #314 | ✅ | el generador no encontraba un tipo del fuente nombrado entero | Tanda: `java.time` al 100 %, `chrono` casi,… |
| #315 | ✅ | el literal hexadecimal de coma flotante | Tanda: de los más completos hacia abajo, y… |
| #316 | ✅ | el chequeo de excepciones dependía del orden de carga | Los dos findings nuevos, arreglados |
| #317 | ✅ | el `<clinit>` de un `enum` corría al revés | Los dos findings nuevos, arreglados |
| #318 | ✅ | un `super.m()` heredado corría contra el pool de constantes equivocado | Los dos findings nuevos, arreglados |
| #319 | ✅ | la resolución de sobrecargas elegía la última declarada, no la más específica | Tanda: de más completo a menos, segunda parte |
| #320 | ✅ | una llamada no resuelve si un parámetro mete una variable de tipo dentro de un argumento de t… | Tanda: de más completo a menos, segunda parte |
| #321 | ✅ | `null` en posición de una variable de tipo no infiere | Tanda: de más completo a menos, segunda parte |
| #322 | ✅ | una referencia a método bajo un cast explícito no se emite | Tanda: de más completo a menos, segunda parte |
| #323 | ✅ | la instrucción de invocación se elegía por la clase declarante, no por el owner escrito | Tanda: de más completo a menos, segunda parte |
| #324 | ✅ | una variable local declarada dos veces se acepta | Tanda: de más completo a menos, segunda parte |
| #325 | ✅ | un externo nombrado entero resolvía por nombre simple, y ganaba el homónimo cargado antes | Tanda: `javax.tools`, `javax.sql` y el `jav… |
| #326 | ✅ | los argumentos de una constante de `enum` no cargaban sus tipos | Tanda: `javax.tools`, `javax.sql` y el `jav… |
| #327 | ✅ | se pierde `@Retention(RUNTIME)`, y con eso todas las anotaciones en una JVM real | Tanda: `javax.tools`, `javax.sql` y el `jav… |
| #328 | ✅ | un constructor varargs no empaquetaba sus argumentos | Tanda: `javax.tools`, `javax.sql` y el `jav… |
| #329 | ✅ | no se aplica el control de acceso `protected` entre paquetes (JLS 6.6.2) | Tanda: `java.net` — dos bugs que sólo se ve… |
| #330 | ✅ | los tipos anidados se resuelven por nombre simple entre unidades de compilación | Tanda: `java.net` — dos bugs que sólo se ve… |
| #331 | ✅ | el emisor toma la cláusula `throws` del classpath, no de la fuente | Tanda: `java.net` — dos bugs que sólo se ve… |
| #340 | ✅ | el espejo de `Class` de la clase de entrada nunca se prepara, y consultarlo la rompe para sie… | Tanda: `java.net` — dos bugs que sólo se ve… |
| #341 | ✅ | un argumento de tipo variable elegía la sobrecarga más angosta, y compilaba a la equivocada | Tanda: `java.net` — dos bugs que sólo se ve… |
| #342 | ✅ | un constructor con `super(...)` explícito y campos con inicializador construía la superclase… | Tanda: `java.net` — dos bugs que sólo se ve… |
| #343 | ✅ | la resolución de constructores no filtra por accesibilidad | Tanda: `java.net` — dos bugs que sólo se ve… |
| #350 | ✅ | llamar a un método sobre el resultado de un genérico con comodín no emite | Tanda: `javax.tools`, `javax.sql` y el `jav… |
| #351 | ✅ | dos tipos anidados con el mismo nombre simple se pisan dentro de la misma unidad | Tanda: `java.net` — dos bugs que sólo se ve… |
| #352 | ✅ | un tipo homónimo del propio paquete le gana al import de un solo tipo cuando los dos están en… | Tanda: `java.net` — dos bugs que sólo se ve… |
| #353 | ✅ | un `native` sin puente mataba el proceso en vez de tirar | Tanda: `java.lang.foreign`, y los cuatro bl… |
| #354 | ✅ | javac no implementaba el polimorfismo de firma (JLS 15.12.3) | Tanda: `java.lang.foreign`, y los cuatro bl… |
| #355 | ✅ | el #341 rompio la conversion hacia una captura de `? super T` | Tanda: `java.lang.foreign`, y los cuatro bl… |
| #390 | ✅ | un literal de clase usado como receptor pierde su argumento generico | Tanda: `jdk.internal.reflect` |
| #400 | ✅ | el parser no acepta `Interface.super.metodo()` | Tanda: `javax.lang.model.util` |
| #401 | ✅ | el generador de bytecode no baja un `switch` con selector `enum` | Tanda: `javax.lang.model.util` |
| #402 | ✅ | una clase anonima no puede pasarle al `super` un argumento tipado con una variable de tipo de… | Tanda: `javax.lang.model.util` |
| #403 | ✅ | el parser no acepta el constructor canonico compacto de un `record` | Tanda: `javax.lang.model.util` |
| #440 | ✅ | la instancia externa implicita solo se agrega desde la clase externa | Tanda: `javax.print.attribute.standard` |
| #460 | ✅ | la asignacion encadenada sobre dos campos no deja el valor en la pila | Tanda: `javax.print.attribute.standard` |
| #461 | ✅ | una constante `case` que viene de otra unidad de compilacion se rechaza | Tanda: `javax.print.attribute.standard` |
| #462 | ✅ | una constante heredada de una interfaz implementada no entra al alcance | Tanda: `javax.print.attribute.standard` |
| #463 | ✅ | una comparacion constante usada como valor emitia codigo muerto sin frame | Tanda: `javax.print.attribute.standard` |
| #464 | ✅ | el `String.valueOf` de una concatenacion no actualizaba la pila simulada | Tanda: `javax.print.attribute.standard` |
| #465 | ✅ | una variable local capturada no se ve desde el inicializador de campo de una clase anidada en… | Tanda: `javax.management` |
| #466 | ✅ | `Method.invoke` no redespachaba: ejecutaba el cuerpo del metodo declarado, no el del receptor | Tanda: `javax.management` |
| #467 | ✅ | la clausula `implements` no comprueba que el tipo sea una interfaz | Tanda: `org.xml.sax.ext`, y los dos bugs qu… |
| #468 | ✅ | las vistas de `LinkedHashMap` no conservan el orden, y eso rompe `ObjectName.toString()` | Tanda: `org.xml.sax.ext`, y los dos bugs qu… |
| #469 | ✅ | los campos de una interfaz no se heredan: ni al que la implementa ni a una subinterfaz | Tanda: `java.awt` (parte de datos: color, g… |
| #470 | ✅ | un cast a un tipo arreglo no emite `checkcast`, y la JVM real rechaza la clase | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #471 | ✅ | los miembros de un supertipo crudo no se erasan, y por eso no se lo puede implementar como ma… | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #472 | ✅ | un plazo vencido resucitaba un hilo ya terminado, que no tiene marcos | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #473 | ✅ | un local puede redeclarar un parametro del metodo, y gana el local | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #474 | ✅ | un `import` de un solo tipo pierde segun QUE OTROS archivos entren en la invocacion | Tanda: `java.beans` -- XMLDecoder, y el bug… ·medido 2026-10-03 |
| #475 | ✅ | los miembros de un supertipo crudo no vienen borrados | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #476 | ✅ | un cast a un tipo arreglo no emitia `checkcast` | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #477 | ✅ | el literal `null` se declaraba `Object` en el `StackMapTable` | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #478 | ✅ | un `goto` muerto despues de un cuerpo que no termina | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #479 | ✅ | `java.lang.Double d = new Rectangle2D.Double(...)` compila | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #480 | ✅ | un `super(...)` de una interna al constructor de la interna de su superclase | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #481 | ✅ | un miembro de interfaz sin modificador se trataba como de paquete | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #482 | ✅ | una clase anonima en el inicializador de un campo de interfaz | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #483 | ✅ | un metodo declarado en una interfaz ANIDADA que redefine al de la que la encierra | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #484 | ✅ | un ternario cuyas ramas dan tipos distintos no calcula el supertipo comun | Tanda: `java.beans` -- XMLDecoder, y el bug… |
| #485 | ✅ | la resolucion de sobrecargas no descarta lo que no se puede ver | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #486 | ✅ | las constantes de una interfaz implementada no resuelven sin calificar | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #487 | ✅ | `Class.forName` no corria el inicializador estatico (VM, no compilador) | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #488 | ✅ | la entrada `InnerClasses` de un tipo anidado perdia sus modificadores implicitos | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #489 | ✅ | una constante `static final` no se estrecha en contexto de asignación | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #490 | ✅ | un inicializador de arreglo no ampliaba sus elementos al tipo del arreglo | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #491 | ✅ | un tipo del classpath se vuelve visible sin importar, con solo haberse cargado | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #492 | ✅ | el `super()` implicito no pasaba la instancia envolvente | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #493 | ✅ | un `import` de un solo tipo pierde contra el homonimo del paquete, y se filtra entre unidades | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #494 | ✅ | `java.lang.reflect.Array` medía completo y no servía para nada (VM) | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #496 | ✅ | una clase anidada `protected` salia con acceso de paquete en sus `access_flags` | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #497 | ✅ | el constructor implicito de una clase `public` se reporta como de paquete | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #498 | ✅ | el tipo objetivo con comodin no llega a la inferencia de un metodo generico | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #499 | ✅ | una clase anonima en un inicializador de campo no llega al generador de bytecode | Sobre el JDK 25.0.2 (no es un hallazgo de n… |
| #500 | ✅ | `Number`, `String` y `ArrayList` no declaraban `Serializable` (biblioteca) | Tanda: `jdk.dynalink.linker` y `jdk.dynalin… ·medido 2026-10-03 |
| #501 | ✅ | `TypeUtilities`: la tabla de conversiones no se puede derivar de la JLS (mio, cerrado) | Tanda: `jdk.dynalink.linker` y `jdk.dynalin… |
| #502 | ✅ | `--emit` rechaza una inferencia que el chequeo acepta (javac) | Tanda: `jdk.javadoc.doclet` (sesion de bibl… ·medido 2026-10-03 |
| #503 | ✅ | un `case` con una constante leida de un `.class` no se pliega (javac) -- CERRADO | Tanda: `javax.sql.rowset` y `javax.sql.rows… |
| #504 | ✅ | `--emit` no le pone a un `@interface` su supertipo implicito `Annotation` (javac) -- CERRADO | Tanda: `jdk.dynalink.support`, `jdk.dynalin… |
| #505 | ⬜ | `Field.getAnnotations()` devuelve vacio siempre (VM/biblioteca) | Tanda: `jdk.dynalink.support`, `jdk.dynalin… ·medido 2026-10-03 |
| #506 | ✅ | los modificadores de acceso de un miembro leido de un `.class` no se chequean | Tanda: `javax.swing` visual -- cajas y desp… |
| #507 | ✅ | `--emit` no soporta un `new` calificado por una instancia (`e.new Interna()`) | Tanda: `javax.swing` visual -- cajas y desp… |
| #508 | ✅ | una clase interna de una clase de nivel superior no puede crear a una hermana | Tanda: `javax.swing` visual -- cajas y desp… |
| #509 | ✅ | compilando en LOTE, un `import` explicito pierde y el tipo equivocado va al DESCRIPTOR (javac) | Tanda: `com.sun.jdi`, `com.sun.jdi.event`,… ·medido 2026-10-03 |
| #510 | ✅ | un `for` que declara dos variables no compila | Tanda: `javax.swing` visual -- cajas y desp… |
| #511 | ✅ | en una clase interna, `this(...)` cuenta la instancia externa y puede llamarse a si mismo | Tanda: `javax.swing` visual -- cajas y desp… |
| #512 | ✅ | una clase anidada no hereda el permiso de su envolvente sobre un `protected` de otro paquete | Tanda: `javax.swing` visual -- cajas y desp… |
| #513 | ✅ | en un `case`, una constante de la clase envolvente no se pliega desde una clase anidada | Tanda: `javax.swing` visual -- cajas y desp… |
| #514 | ✅ | el nombre binario de un anidado no resuelve si el tipo esta en el mismo lote | Tanda: cierre de `javax.swing.text` (sesion… |
| #515 | ✅ | el constructor por omision se genera sin comprobar que exista `super()` | Tanda: cierre de `javax.swing.text` (sesion… |
| #516 | ✅ | un campo heredado con comodin `? super T` pierde la cota inferior al capturarse | Tanda: deslizante, progreso, items con esta… |
| #517 | ✅ | llamar un metodo sobre el resultado de un comodin, sin variable intermedia, no compila | Tanda: deslizante, progreso, items con esta… |
| #518 | ✅ | la invocacion calificada del constructor de la superclase (`externa.super(...)`) no se analiza | Tanda: tamanos, filtros y orden de filas (s… |
| #519 | ✅ | un argumento con comodin no entra por el constructor de una clase generica | Tanda: los resortes (sesion de biblioteca,… |
| #520 | ✅ | un literal de clase de un tipo INEXISTENTE compila y da `Object.class`, miscompilacion silenc… | Tanda: `javax.swing.tree` cerrado y casi to… |
| #521 | ✅ | `"texto" + unEnvoltorio` desempaqueta en vez de usar `String.valueOf(Object)` | Tanda: `javax.swing.tree` cerrado y casi to… |
| #522 | ✅ | una clase interna que llama a un metodo heredado de la externa compila y revienta, miscompila… | Tanda: `javax.swing.tree` cerrado y casi to… |
| #523 | ✅ | el decimal `9223372036854775808L` se rechaza (y su hermano `2147483648`) | Tanda 2026-08-24 (f) -- `StrictMath` comple… ·era el 2º #267 |
| #524 | ✅ | javac no sintetiza los accesores de una superclase package-private | Tanda 2026-08-24 (g) -- `StringBuilder` y `… ·era el 2º #268 |
| #525 | ✅ | un `long...` que recibe `int` no ampliaba | Los dos findings nuevos, arreglados ·era el 2º #316 |
| #526 | ✅ | un cast a primitivo desde una referencia no emitía nada | Tanda: `javax.tools`, `javax.sql` y el `jav… ·era el 2º #325 |
| #527 | ✅ | el compilador se colgaba con una variable llamada `con` | Tanda: `javax.tools`, `javax.sql` y el `jav… ·era el 2º #326 |
| #528 | ✅ | un `goto` muerto después de un `throw` hacía que la JVM real rechazara la clase | Tanda: `javax.tools`, `javax.sql` y el `jav… ·era el 2º #328 |
| #529 | ✅ | `java.lang` le gana a un tipo anidado en un nombre calificado `Externa.Anidada` | Tanda: `javax.management` ·era el 2º #465 |
| #530 | ✅ | un `import` de un solo tipo pierde contra una clase del classpath cuando el fuente del tipo i… | Tanda: `org.xml.sax.ext`, y los dos bugs qu… ·era el 2º #466 |
| #531 | ✅ | una declaracion `@interface` se emitia sin sus meta-anotaciones, y eso borraba `@Retention` | Tanda: `org.xml.sax.ext`, y los dos bugs qu… ·era el 2º #467 |
| #532 | ✅ | con `--emit`, un tipo del classpath nombrado con nombre completo se resuelve distinto segun l… | Tanda: `org.xml.sax.ext`, y los dos bugs qu… ·era el 2º #468 |
| #533 | ✅ | una clase anonima declarada adentro de otra clase anonima no se puede emitir | Tanda: `java.beans` -- XMLDecoder, y el bug… ·era el 2º #470 |
| #534 | ✅ | un `wait(ms)` vencido dejaba al hilo adentro del conjunto de espera del monitor | Tanda: `java.beans` -- XMLDecoder, y el bug… ·era el 2º #471 |
| #535 | ✅ | el `lub` de dos arreglos daba `Unresolved`, y el ternario lo emitia como `Top` | Tanda: `java.beans` -- XMLDecoder, y el bug… ·era el 2º #477 |
| #536 | ✅ | el ternario descontaba uno de mas de la pila, y `max_stack` salia corto | Tanda: `java.beans` -- XMLDecoder, y el bug… ·era el 2º #478 |
| #537 | ✅ | un metodo `static` de una interfaz se heredaba, y no se hereda nunca | Una regresion, y por que la recompilacion c… |
| #538 | ✅ | un `switch` sobre un `enum` solo se baja si el enum es de nivel superior y del fuente | Dos huecos de biblioteca que el censo no ve… |
| #539 | ✅ | un plazo no positivo en `Thread.sleep` no es un punto de interrupcion | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #540 | ✅ | un `import static X.*` no importa los tipos miembro de `X` | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #541 | ✅ | un `@interface` de la misma invocacion no cuenta como `Annotation` | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #542 | ✅ | un nombre calificado a medias (`reflect.Field`, `util.List`) se acepta como el completo | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #543 | ✅ | un `import static` de un campo, un metodo o un tipo suelto no se resuelve nunca | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #544 | ✅ | el `StackMapTable` nombra a una variable de tipo como si fuera una clase | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #545 | ✅ | una llamada generica con otra llamada generica de argumento no infiere nada si es argumento d… | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #546 | ✅ | una llamada generica como argumento no recibe el parametro como objetivo, y con una sobrecarg… | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #547 | ✅ | una referencia a metodo con objetivo con comodines lleva un `instantiatedMethodType` borrado… | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #548 | ✅ | el lub de `String` e `Integer`, y el de una captura, no dan un tipo usable | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #549 | ✅ | una referencia a un metodo sobrecargado elige la sobrecarga sin mirar el parametro de la inte… | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #550 | ✅ | leer un campo de tipo variable a traves de un tipo parametrizado no lleva `checkcast` | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #551 | ✅ | un cast de un primitivo a un tipo referencia no boxea | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #552 | ✅ | el `LambdaMetafactory` de la VM solo arma `REF_invokeStatic`, y sin adaptar el boxing | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #553 | ✅ | `Externa.super.m()` desde una clase interna llama al metodo de la interna | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #554 | ✅ | adentro de una clase local o anonima no se inserta ningun boxing ni unboxing | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #555 | ✅ | una clase anonima adentro de un metodo de una clase local no se puede emitir | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #556 | ✅ | una clase local adentro de un metodo de una anonima no resuelve su propia llamada | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #557 | ✅ | una creacion calificada de una interna de segundo nivel no resuelve | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #558 | ✅ | `collect(Collector<? super T, A, R>)` no infiere su `R` del argumento en linea | Tandas 2026-09-18 a 2026-10-03 (#539-#559) |
| #559 | ⬜ | el frame de un *handler* arrastra el tipo de un local MUERTO, y una JVM real rechaza la clase | Tandas 2026-09-18 a 2026-10-03 (#539-#559) ·medido 2026-10-03 |

## Index by section

The sections of [`findings-details.md`](findings-details.md), in order. The first four are listings;
from there down they are the batches, chronological. The batch is also the unit the record is read
in: the method notes (*lo que esta tanda deja*) belong to the batch, not to the finding.

- Indice por componente (#200-#249, barrida 2026-08-22) *(historico)*
- Fixed
- Open *(historico)* --- vaciada el 2026-10-03: los cuatro estan cerrados
- Priority *(historico, 2026-08-24)* --- habla de #13-#126, todos cerrados
- Los dos fixes de esta sesion (#110 y #112) — nota para el merge
- jakarta.persistence — cerrado en 203/203, con 9 metodos omitidos (2026-08-24)
- Barrida de diff de API contra OpenJDK 25 — #200 en adelante (2026-08-22)
- La regla: el contrato es publico + protected. Lo demas es libre
- Tanda 2026-08-24 (b) — cerrando `java.util.concurrent`: por que el mapa no corria
- Tanda 2026-08-24 (c) — `SubmissionPublisher`: tres findings nuevos, uno de ellos grave
- Tanda 2026-08-24 (d) — cerrando clases de `java.lang`
- Tanda 2026-08-24 (e) -- #118 cerrado, y por que #110 "volvia"
- Tanda 2026-08-24 (f) -- `StrictMath` completo, y un literal que no se podia escribir
- Tanda 2026-08-24 (g) -- `StringBuilder` y `StringBuffer` completos
- Tanda 2026-08-24 (h) -- `java.lang.Class`: la capa de metadatos
- Tanda 2026-08-24 (i) -- `java.lang.reflect.Method`, y la llamada que decide su destino leyendo un objeto
- Tanda 2026-08-24 (j) -- los wrappers, y dos cosas que el gate no puede ver
- Tanda 2026-08-24 (k) -- `Character` cerrado: 164 de 166
- Tanda 2026-08-24 (l) -- `Class` y `ClassLoader`: hasta donde llega el techo
- Tanda 2026-08-29 (m) -- `Collections` completa, y la igualdad que no existia
- Tanda 2026-08-29 (n) -- `TreeMap`/`TreeSet` completos, y el `new` que nunca se chequeaba
- Tanda 2026-08-29 (o) -- `SimpleTimeZone` y `Objects`, y el snapshot al día
- Tanda 2026-08-29 (p) -- las tres que faltaban, y el varargs que envolvia de mas
- Tanda 2026-08-30 (q) -- cerrando findings, y `Locale`/`Properties` completas
- Tanda 2026-08-30 (r) -- el cierre chico, y el hash de identidad que se movía
- Tanda: Optional, Random, SplittableRandom — y `Math.log`
- `RandomGenerator` al 100 %, y el finding que destapó
- Tanda: el merge de `java.lang`, `java.util` al 99,3 %, y `java.time`
- Tanda: `java.util` al 100 %, el acceso a archivos, y `java.time`
- Tanda: `java.time` al 100 %, `chrono` casi, y el parseo
- Tanda: `java.time.chrono` cerrado, y el medidor que contaba mal
- Tanda: de los más completos hacia abajo, y el medidor que engañaba
- Los dos findings nuevos, arreglados
- Tanda: de más completo a menos, segunda parte
- Tanda: `javax.tools`, `javax.sql` y el `java.sql` que hacía falta debajo
- Tanda: `java.net` — dos bugs que sólo se ven corriendo la prueba de comportamiento
- Tanda: `java.lang.foreign`, y los cuatro bloqueos que lo tenian parado
- Tanda: `jdk.internal.reflect`
- Tanda: `javax.lang.model.util`
- Tanda: el literal de clase perdia su argumento (#390, arreglado)
- Tanda: `javax.print.attribute.standard`
- Tanda: `javax.management`
- Tanda: `org.xml.sax.ext`, y los dos bugs que estaban tapados por un `javap` mentiroso
- Tanda: `java.awt` (parte de datos: color, geometria entera, constantes de disposicion)
- Tanda: `java.beans` -- XMLDecoder, y el bug que solo se ve en la JVM real
- Hallazgo: la referencia adelantada a un campo estatico no se rechaza
- Hallazgo (VM): `System.arraycopy` entraba en panico con un origen nulo
- Sobre el JDK 25.0.2 (no es un hallazgo de nuestro compilador)
- Tanda: los 11 paquetes vacios (sesion de biblioteca, 2026-09-04)
- Tanda: `jdk.dynalink.linker` y `jdk.dynalink.linker.support` (sesion de biblioteca, 2026-09-04)
- Tanda: `jdk.javadoc.doclet` (sesion de biblioteca, 2026-09-04)
- Tanda: `com.sun.management` y `com.sun.security.auth.module` (sesion de biblioteca, 2026-09-04)
- Tanda: `javax.sql.rowset` y `javax.sql.rowset.spi` (sesion de biblioteca, 2026-09-04)
- Tanda: `javax.swing` visual -- cajas y desplazamiento (sesion de biblioteca, 2026-09-05)
- Tanda: `jdk.dynalink.support`, `jdk.dynalink.beans` y `jdk.jfr` (sesion de biblioteca, 2026-09-04)
- Tanda: `sun.misc` (sesion de biblioteca, 2026-09-05)
- Tanda: `com.sun.jdi`, `com.sun.jdi.event`, `com.sun.jdi.request` (sesion de biblioteca, 2026-09-05)
- Tanda: cierre de `javax.swing.text` (sesion de biblioteca, 2026-09-05)
- Tanda: `javax.swing.text.html`, su analizador y `rtf` (sesion de biblioteca, 2026-09-05)
- Tanda: desbloquear `javax.swing` (sesion de biblioteca, 2026-09-05)
- Tanda: `javax.swing.plaf` casi cerrado (sesion de biblioteca, 2026-09-05)
- Tanda: `javax.swing.plaf` cerrado — el escritorio y los dialogos (sesion de biblioteca, 2026-09-05)
- Tanda: los spinners, y el formato localizado de `java.time` (sesion de biblioteca, 2026-09-05)
- Tanda: deslizante, progreso, items con estado y barra de herramientas (sesion de biblioteca, 2026-09-05)
- Tanda: tamanos, filtros y orden de filas (sesion de biblioteca, 2026-09-05)
- Tanda: bordes, editores de celda y seis clases chicas (sesion de biblioteca, 2026-09-05)
- Tanda: los resortes (sesion de biblioteca, 2026-09-05)
- Tanda: `javax.swing.tree` cerrado y casi todo `javax.swing.table` (sesion de biblioteca, 2026-09-05)
- Tanda: `JTable` (sesion de biblioteca, 2026-09-05)
- Tanda: `javax.swing` CERRADO (sesion de biblioteca, 2026-09-06)
- Tanda: `javax.swing.plaf.basic`, primera mitad (sesion de biblioteca, 2026-09-06)
- Tanda: `javax.swing.plaf.basic`, segunda mitad (sesion de biblioteca, 2026-09-06)
- Tanda: `javax.swing.plaf.basic`, los dialogos y la barra (sesion de biblioteca, 2026-09-06)
- Tanda: `javax.swing.plaf.basic` CERRADO (sesion de biblioteca, 2026-09-06)
- Tanda: `plaf.metal` y `plaf.synth` CERRADOS (sesion de biblioteca, 2026-09-06)
- Verificacion de repros (2026-09-06) *(historico)*
- La renumeracion (2026-09-06)
- Una regresion, y por que la recompilacion completa vale lo que cuesta (2026-09-07)
- Tanda: siete hallazgos arreglados, y dos cosas que no eran del compilador (2026-09-07)
- Dos huecos de biblioteca que el censo no ve (2026-09-07)
- Los que quedan abiertos, agrupados por causa (2026-09-09) *(historico)* --- los once estan cerrados
- Tandas 2026-09-18 a 2026-10-03 (#539-#559)

## The method rules the record leaves behind

The most reusable part of the record is not the findings but how they were found. This is the
distillation; each one is argued in its own batch, which is named so it can be read there.

**Reproduce before writing code.** In the 2026-09-09 sweep, of the findings that got closed,
**sixteen were already fixed** and still marked open: the fix had come in through another door. That
same sweep also uncovered #538, so it cuts both ways. Checking costs minutes.
*(Los que quedan abiertos, 2026-09-09.)*

**And reproduce with both javacs**, the working one and the frozen one: that is usually where the
difference between what the entry says and what happens comes from.

**A report without the neighbouring case that WORKS is not enough.** The findings that turned out
small had the right diagnosis; the ones that turned out large --- #503, #537 --- described the
symptom with the cause two layers below. The difference was not the size of the fix but whether the
report carried the form that does *not* fail: in #503 the clue only appeared once both forms were put
**in the same file**. *(Tanda: siete hallazgos arreglados, 2026-09-07.)*

**A half fix can be worse than the bug.** #465 was missing two of its three passes, and like that it
compiled and returned `null`: it traded a compile error for silently wrong output. It was reverted ---
and the diagnosis of that failed attempt is what led to the third pass. It is the house rule: **a
missing member is a legal subset; one that lies compiles and blows up later.**

**Refuting an attempt is not losing it.** #402 took three. The second one, refuted with concrete
programs on three fronts --- miscompilation, specification, regression --- produced the constraint
that made the third work, and the third is smaller and passes the checks that killed the second. A
diagnosis of why something does **not** work, with the case that proves it, is worth more than a half
patch. *(#402, `javax.lang.model.util` batch.)*

**When two functions answer the same question, touch both.** Four such pairs: the two name resolvers
in #514, rewriting versus materialising `this$0` (#522 and #508), and the bare name versus the
qualified one (#352 and #516). Every time, one was up to date and the other was not, and the symptom
showed up far from both.

**A comment that justifies an absence stops being true without warning.** Three of them in a single
batch (#506, #284, and the one on `DateTimeFormatter.toFormat`). They come out the same way: by
turning the check on and measuring, instead of believing the comment.

**Several fixes were just reading something the `.class` already carried and the reader threw away**:
the `ConstantValue` (#503), the access flags (#506), the `ACC_STATIC` of `InnerClasses` (#480), where
each entry is four `u2` and only the first was used.

**Gates can fail GREEN, and that is worse than not having them.** Three shapes seen so far:

| gate | how it lied |
|---|---|
| the API census | it counts **what** is there, not **how**: it reported `java.util` COMPLETE with `TreeSet(Comparator<E>)` where the JDK declares `Comparator<? super E>`. Three blind spots in one day |
| `tools/emitdiff` | it disassembles with `javap -p -c`, which **does not print stack maps**: the one divergence that invalidates a class is out of its reach by construction (#559) |
| `tools/suite.py` | a fixture that **threw** was reported as `(sin valor)` and stayed out of the broken count. Fixed 2026-10-03; fixing it exposed a second dead fixture |

**A shape gate cannot see a body.** The old #101 entry measured the damage with the gate --- which
only sees signatures --- and classified it as a cosmetic, allowlistable divergence. The real damage
was in the **bodies**. A finding whose scope was estimated with the gate has to be re-measured by
**running** it.

**HotSpot is the fourth oracle, and the only one that verifies.** Our VM does not verify, so it
accepts bytecode a real JVM refuses. #463 (`boolean b = (1 + 1) == 3;` --- dead code with no frame),
#528 (a dead `goto` after a `throw`) and #559 all came out of handing one of our `.class` files to
`java`, and all three ran fine here.

**Comparing two `recompile.py` output trees made at different moments does not work** while another
session is editing the library: the first comparison reported 125 differing classes, and the first
ones inspected were line numbers shifted by one. A valid comparison compiles each source **twice in a
row**, once with each binary, from the same copy.

**A number that does not identify one finding makes it impossible to verify anything.** Twenty
numbers had more than one entry, and in fourteen of them they were different findings. Resolved on
2026-09-06 by moving the second of each into the 523+ series. *(La renumeracion.)*

---

## Where the detail is

The full record --- every entry with its reproducer, its measurement and its closing note --- is in
**[`findings-details.md`](findings-details.md)**, 16937 lines in chronological order. To reach an
entry: locate the `#NNN` in the index above, open the detail and search for `#NNN`; the **first**
appearance is the canonical one.
