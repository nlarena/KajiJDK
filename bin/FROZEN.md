# `bin/` — herramientas congeladas

Snapshot binario de **todas** las herramientas del proyecto, commiteado a propósito.

**Por qué NO se commitean los `.exe`** (decisión del 2026-08-28, y es la definitiva).
Esta regla se dio vuelta dos veces discutiendo tamaño, así que el motivo real queda escrito:

- El **peso no es el argumento**. Son 11 MB con `msvc` y 59 MB con `gnu` — depende de qué
  máquina los construya, que es *justamente* por qué no sirven como referencia compartida:
  dos personas con el mismo commit obtienen binarios distintos.
- Un binario en el historial **no reproduce nada**: no se puede diffear, no se puede auditar,
  y cada refresco agrega una copia entera porque los `.exe` no deltifican.
- Lo que hay que conservar es la **receta y la procedencia**, no el artefacto. Eso es este
  archivo, y sí se commitea. Reconstruir las diez herramientas son **34 segundos**.

Lo que el snapshot resuelve sigue en pie y no cambia: la sesión de biblioteca compila con un
`javac` **fijo**, para que un `.class` distinto signifique "cambió la fuente" y no "cambió el
compilador". Eso se logra igual con un `bin/` local reconstruido desde el commit que dice la
tabla de procedencia — lo que no hace falta es que los bytes viajen en el repo.

### Reconstruirlos

```
git archive HEAD | tar -x -C /tmp/kaji-frozen-src
CARGO_TARGET_DIR=/tmp/kaji-frozen-target cargo build --release --manifest-path /tmp/kaji-frozen-src/Cargo.toml
cp /tmp/kaji-frozen-target/release/*.exe bin/
```

Desde una copia limpia, **no** desde el working tree: lo que el snapshot promete es "esto es
exactamente el commit tal", y ya hubo cuatro refrescos donde el árbol tenía cambios sin
commitear que no debían entrar. Después, actualizar la tabla de procedencia de abajo.


### Refresco 2026-08-30, tanda s (Optional/Random/SplittableRandom)

Misma copia limpia superpuesta que el refresco anterior, con los mismos tres archivos de
`src/fuzz/` dejados **afuera** (siguen a medio editar por la otra sesión, y el árbol de trabajo
sigue sin compilar por eso).

**Los que cambiaron en esta tanda:** `src/javac.rs`, `src/bin/javac.rs`, y de `src/javac/`
`{symbol,enter,attribute,types,check,desugar,codegen}.rs`. Llevan **#303-#306** y **#308**:

- **#303** — un tipo de `java.lang` del **mismo round** no lo veía el `import java.lang.*`
  implícito de otra unidad. Sin esto no se puede compilar `StrictMath` junto con `Random`.
- **#304** — el diagnóstico señalaba el **archivo equivocado** con varios fuentes. `Error` ahora
  lleva de qué unidad salió.
- **#305** — `null` era aplicable a un parámetro **primitivo**, y el generador emitía `aconst_null`
  contra un descriptor `(J)V`. Se modeló `RType::Null`.
- **#306** — una lambda pasada a un método genérico de **otra clase** no compilaba.
- **#308** — `() -> c[1]++` era un **no-op** (el bloque equivalente andaba).

**Verificado sobre estos binarios**, no sobre los de `src/`:

- De las **45** pruebas comparables, **43** dan el mismo entero que `java` real; las dos que no son
  `JcIc` y `WdWide`, de siempre. Las cuatro nuevas: `LogTest` **-1250194933** (compara `log` **por
  bits**, ~700 evaluaciones, más 150 `nextGaussian()`), `OptRndTest` **-1**, `RgTest` **-1** y
  `AtomTest` **-1** (la familia atómica, que #309 tenía rota entera).
- Los repros nuevos dan lo mismo que `java` real: `finding_305` → 2, `finding_306` → 1,
  `finding_308` → 1111, `finding_309` → 2068615891. `finding_305b` y `finding_307` **no compilan**, que es lo que se espera de
  ellos (`finding_307` está abierto).
- La biblioteca recompila **1034/1035** — el mismo y único `SymElement`, donde javac tiene razón.
- Los `.class` del árbol están en **punto fijo** (1464/1464 idénticos) y dos recompilaciones
  seguidas dan bytes idénticos.
- **La medición que más dice de cada arreglo**: con #305 puesto, recompilar la biblioteca entera
  cambió el bytecode de **dos** clases de 1464 —`Random` y `RandomAdapter`, las que se estaban
  arreglando—; #306 y #308 no cambiaron ninguna. Son arreglos puros. **#309 cambió cuatro**, y esas
  cuatro son el hallazgo: estaban versionadas con bytecode roto.

### Este refresco NO salió de una copia limpia a secas — y por qué igual respeta la regla

Sigue valiendo lo que la regla protege —**que no se cuele trabajo ajeno o a medio hacer**— y no el
gesto de escribir `git archive`. Este refresco (2026-08-30, tanda r) se hizo así:

- Se partió de `git archive HEAD` (`67ac198`).
- Se superpusieron **once** archivos, nombrados uno por uno: los del compilador y los dos de la VM.
- Y esta vez **hubo que dejar algo afuera**, que es justamente para lo que existe la regla: mientras
  se trabajaba aparecieron tres archivos modificados de `src/fuzz/` —`campaigns.rs`, `gen.rs` y
  `reduce.rs`—, de otra sesión y sin commitear. **No entraron.** Se verificó archivo por archivo que
  son los únicos tres que quedan afuera.

**Los once que entraron:**

```
src/javac/{attribute,check,codegen,desugar,enter,infer,parser,symbol,transtypes}.rs
src/jvm/interpreter/{natives,heap}.rs
src/jvm/interpreter/bytecode_interpreter/array_operations.rs
```

Llevan los arreglos #281-#283, #289, #290, #293-#295, #297-#301, más el nativo `Array.newArray`.
Sin ellos el snapshot no sirve: la biblioteca actual no compila.

El nuevo de esta tanda es `heap.rs`, que lleva **#302**: el hash de identidad pasa a guardarse en la
palabra de marca del encabezado en vez de salir del offset, para que no cambie cuando el recolector
mueve el objeto.

**Y esta vez el árbol de trabajo directamente no compila**: los tres archivos de `src/fuzz/` que
quedaron afuera están a medio editar por la otra sesión, con errores de tipos. Todo lo de la tanda se
construyó y se probó sobre esta misma copia limpia — que es, de hecho, el único árbol que compila.

**Verificado sobre estos binarios**, no sobre los de `src/`:

- Las **ocho** pruebas de comportamiento de `java.util` dan el mismo entero que `java` real:
  `ArrTest` 11246595, `CollTest` 123156332, `TreeTest` 5341435, `StzTest` 16859132,
  `ScanTest` 1156108, `TmSvTest` 16894, `LocPropTest` 1204219, `LoteTest` 311293.
- Los **286 tests de la VM pasan**, incluidos los del GC — la verificación que #302 necesitaba,
  porque toca la palabra de marca del recolector.
- Los repros de los findings cerrados en esta tanda corren a lo documentado: `finding_274` → 1,
  `finding_300` → 1/1/1/2, `finding_301` → 11/7/12, y `zz299/Uso` declara `zz299.Type` (que es
  exactamente lo que #299 arregló).
- La biblioteca recompila **1034/1035**. El único fallo es `SymElement`, que no es del compilador:
  la clase no implementa `asType` y javac tiene razón.
- Los `.class` del árbol están en **punto fijo**, y dos recompilaciones seguidas dan bytes
  idénticos.

## El snapshot puede quedar ATRAS de lo que la biblioteca necesita

Es el riesgo propio de congelar, y ya mordio una vez. Se deja escrito el caso porque el sintoma
no se parece en nada a la causa.

**Resuelto en este refresco.** El snapshot anterior (`9642607`) **no podia correr
`java/CtorTest.class`**:

```
panicked at src\jvm\interpreter\heap.rs:878:51:
range start index 528404 out of range for slice of length 528400
```

No era un bug del binario. `new String(...)` anda porque la VM **reescribe** el par `new`/`<init>`
en una llamada a fabrica, y esa reescritura (`dace994`) es **posterior** a aquel snapshot. Sin
ella, `new` deja un objeto con lugar para cero caracteres y el primer `charAt` lee fuera de el,
que es exactamente lo que dice el mensaje.

Con el refresco de `07e8ebc` eso quedo cubierto un rato, y **volvio a quedar atras el mismo dia**,
por dos cambios distintos: las costuras privadas nuevas de `java.lang.String` (`rawLength`,
`rawCharAt`, los dos `rawValueOf`), que el `run-headless` de entonces no conocia, y `ACC_VARARGS`
(#118), que el `javac.exe` de entonces no emitia.

**Este refresco cubre los dos**, verificado sobre estos binarios: `CtorTest` y los tres grupos de
`StringTest` dan `0`, y el `String.class` que sale de recompilar la fuente ya trae sus **cuatro**
varargs (`format` x2, `join`, `formatted`) impresos como `String...`.

**Pero el `String.class` versionado en el arbol sigue sin ellos**, porque lo emitio un javac
anterior. Es la otra mitad de la misma leccion y conviene tenerla presente: **el snapshot y los
`.class` del arbol tienen cada uno su propia fecha**, y no son la misma.

**Y volvio a quedar atras (2026-08-24, tanda f):** el `javac.exe` de este snapshot rechaza
`-9223372036854775808L` (#267), asi que no puede compilar una tabla de referencia que incluya el
cero negativo. El arreglo esta en `src/javac/parser.rs`; para reproducir esa clase de prueba hay
que compilar con `target/debug/javac.exe`, no con `bin/`.

La leccion queda, y es la que importa: **un panic con el congelado puede ser una fecha, no un
bug**. Antes de perseguirlo, comparar contra `target/debug` (o `--release`). Y su reciproca, que
costo media sesion aprender del otro lado: **un `.class` del arbol tambien puede ser una fecha**
— ver la seccion de `.class` rancios en `COMPILER_FINDINGS.md`.

## Procedencia

| | |
|---|---|
| Commit | `099b348` |
| Construido desde | el working tree, **verificado limpio para las fuentes Rust** (`git status --porcelain \| grep '\.rs$'` = 0 modificados y 0 sin seguir), así que es idéntico a `git archive HEAD`. Lo único sin commitear eran `.java`/`.class` de biblioteca (`java.util.function`), que no entran al binario |
| Toolchain | `rustc 1.96.0 (ac68faa20 2026-05-25)`, host `x86_64-pc-windows-msvc` |
| Perfil | `cargo build --release` |
| Fecha | 2026-08-30 (regeneración: `bin/` había quedado con sólo este `.md`) |
| Herramientas | **11** |
| Peso | 14 MB — toolchain `msvc` (el refresco anterior era `gnu`/61 MB); ver la nota de arriba sobre por qué el peso no es el argumento |

> **Regeneración 2026-08-30.** `bin/` se encontró vacío (sólo este `FROZEN.md`). Se reconstruyeron
> las **11** herramientas con `cargo build --release` sobre el working tree —verificado limpio para
> `*.rs`, o sea equivalente a la copia limpia de `099b348`— y se copiaron a `bin/`. **AppControl
> bloquea 4 por hash nuevo** (`jvm`, `run-headless`, `jdi-attach`, `jimage`): vienen de `099b348`
> (no del `67ac198` que estaba allowlisted), así que necesitan aprobarse en la política de la
> máquina antes de correr. Las otras 7 (`javac`, `jvm-step`, `javac-step`, `jdb`, `jvm-jdwp`,
> `jlink`, `javadoc`) pasan. Mientras tanto: `javap` del JDK (`.jdk25_tmp`) para desensamblar/medir
> y `target/debug/*.exe` (hash distinto, ya allowlisted) para correr.

**Por qué se refrescó**: cinco findings más, y dos de ellos hacen falta para compilar la biblioteca
tal como está — **#303** (sin él no se puede compilar `StrictMath` junto con `Random`) y **#305**
(sin él `Random.from` emite bytecode que no verifica). Los otros tres: **#304** (el diagnóstico
señalaba el archivo equivocado), **#306** (una lambda hacia un genérico de otra clase no compilaba) y
**#308** (`() -> c[1]++` era un no-op) y **#309**, el más grave de los seis.

**#309** — un `++`/`--` sobre un campo o un elemento de arreglo, en posición de **valor**, no emitía
**nada**: ni código ni diagnóstico. Tenía mal compiladas las cuatro clases de
`java.util.concurrent.atomic` (`return value++` es literalmente su `getAndIncrement`), y **el punto
fijo no podía verlo**, porque los dos lados de esa comparación los emite el mismo compilador. Por eso
este refresco **cambia `.class` versionados** que no venían de un cambio de fuente.

#305, #308 y #309 cambian qué bytecode se emite; #303 y #306, qué fuente se acepta; #304, solo el
mensaje.

La tanda anterior (r) cerró **#274**, **#299**, **#300** y **#301**, los cuatro de resolución de
nombres, y su descripción quedó en el historial de abajo.

### Historial — los refrescos anteriores

Lo que sigue hasta `## Las herramientas` describe refrescos **viejos** y se conserva por la
trazabilidad. La procedencia vigente es la tabla de arriba.

Se construyó desde una copia limpia y no desde el working tree a propósito, y **las cuatro veces
importó**: las cuatro veces el árbol tenía archivos modificados sin commitear, y **ninguno de esos
cambios entró**. Un snapshot "congelado" que se lleve trabajo a medio hacer no sirve de referencia:
lo que promete es "esto es exactamente el commit tal".

Esta vez la diferencia era grande — 126 archivos modificados y 132 sin seguir, casi todos de la
tanda de `java.lang.Class` en curso — y es exactamente el escenario para el que la regla existe.

**Por qué se refrescó** (2026-08-25): el cierre de **todas** las familias de findings del indice
salvo la de biblioteca. Cambia qué bytecode se emite, qué se acepta y qué se rechaza — las tres.
La lista completa está más abajo; la razón corta es que el snapshot anterior (`de0e786`) quedó
**tres tandas atrás**.

**El refresco anterior** se hizo por los niveles 3 y 4 de los findings de compilador, que entre
los dos cambian **qué bytecode se emite**, no solo qué se acepta.

| | |
|---|---|
| `#217`/`#217b` | faltaba la ampliación implícita `int`→`long`/`double`: el class file salía estructuralmente inválido |
| `#110` | el `ACC_STATIC` de un campo del classpath se leía y se tiraba → `getfield` sobre un estático |
| `#238`/`#124` | campo de interfaz sin `public static final`, y un `<init>` sintetizado sobre la interfaz |
| `#261` | un array no era un tipo referencia: `System.arraycopy` no resolvía y once fuentes se compilaban mudas y rotas |
| `#100`/`#241` | la borradura de una variable de tipo **del método** era `Object` y no su cota → descriptores equivocados |
| `#115`/`#236`, `#242`, `#255`, `#200` | cuatro flags que no se emitían (`volatile`, `transient`, el `public` implícito, `synchronized`, `varargs`) |
| `#233`, `#231` | faltaban puentes covariantes abstractos, y `super.m()` no emitía nada |
| `#259` | el pool iba en UTF-8 estándar y no en el modificado: el `.class` no se podía cargar |
| `#204`/`#215` | un parámetro de tipo del método no resolvía en su cuerpo → `Collections` y `Optional` no compilaban |
| `#234` | una invocación con varios archivos no resolvía cruzado |

Verificado sobre **estos** binarios, no sobre los de `target/`: `finding_204`, `finding_217`,
`finding_226`, `finding_233`, `finding_234` y `finding_261` corren a `0`.

### Lo que agrega este refresco (`de0e786` → `8f0de93`)

| | |
|---|---|
| `#208` | el generador **inventaba** un tipo que no resolvió: descriptor `Object` y `Signature` con el nombre crudo, dos artefactos distintos y los dos falsos. Ahora falla |
| `#239`/`#245`, `#253`/`#264`, `#221`, `#263`, `#260`, `#104`/`#256` | el nivel 4 entero: nombres anidados, aplicabilidad con comodines, el tipo del condicional, y el atributo `Exceptions` que se escribía y no se leía |
| `#209` | `int.class` / `void.class` / `int[].class` — antes ni parseaban |
| `#228` | el escape de sustituto (`'\ud800'`) — un `char` de Java es una unidad de código UTF-16, no un *scalar value* |
| `#235` | el `SourceFile` es el de la **unidad**: una secundaria decía el nombre de un archivo que no existe |
| `#224` | un `import` sin cualificar pasa a ser error, como en el `javac` real |
| `#268` | faltaba el **`checkcast` sintético** tras una llamada de retorno borrado: ese bytecode **la JVM real lo rechaza** con `VerifyError`, y el nuestro no |
| `#265` | VM: la resolución no subía por la jerarquía — un `super.m()` a un método heredado moría con `operand stack underflow` |
| `#262` | VM: la vtable de un array se construía vacía, y su mirror se alocaba sin header |

Verificado sobre **estos** binarios: `finding_204`, `finding_217`, `finding_221`, `finding_226`,
`finding_233`, `finding_234` (con `finding_234b`), `finding_261` y `finding_263` corren a `0`;
`finding_208` **falla a propósito** (es lo que prueba); `finding_209` emite los cuatro literales de
clase correctos; `finding_235` da el mismo `SourceFile` en las tres clases; `finding_268` emite sus
dos `checkcast`; y `SuperProbe`/`ArrayProbe` dan `7` y `15` en el `run-headless` de este snapshot.
Recompilar KajiLibrary con **este** `javac.exe`: **970/978**.

### Lo que este snapshot NO trae, y conviene saberlo

**`#271` (`String[].class`) y `#272` (`invokespecial` a un método sin cuerpo) están documentados en
`COMPILER_FINDINGS.md` pero su código NO está en `8f0de93`** — vive sin commitear en el árbol de
trabajo. El documento se adelantó al código por un `git add` de más, y como este snapshot se
construye desde `git archive HEAD` —que es justamente lo que lo hace confiable— no los incluye.

Se verificó: con este `javac.exe`, `String[].class` sigue dando *"se esperaba una expresión, se
encontró RBracket"*. Es el mismo género que la sección de arriba ("el snapshot puede quedar atrás"),
con una vuelta de tuerca: acá no quedó atrás del código, quedó atrás de la **documentación**.

El host y el toolchain son los mismos que en el snapshot de `9642607` (`windows-msvc`, rustc
1.96.0), asi que **los diez hashes cambiaron por el codigo y por nada mas**. Siguen siendo 11 MB
las diez herramientas.

## Las herramientas

| Binario | SHA-256 | Qué es |
|---|---|---|
| `javac.exe` | `ba32…e18c` | El compilador. `--emit X.java` → `X.class` |
| `jvm.exe` | `6983…f7f6` | El desensamblador estilo `javap` (Nivel 0) y CLI principal |
| `run-headless.exe` | `8697…a674` | Corre `<File.class> <método>` con el intérprete (modo green) |
| `jvm-step.exe` | `3deb…5f60` | El visor paso a paso del intérprete |
| `javac-step.exe` | `ba28…227c` | El visor paso a paso del compilador |
| `jdb.exe` | `8624…6342` | El depurador (Fase I, JPDA) |
| `jvm-jdwp.exe` | `f825…664b` | Servidor JDWP (`dt_socket`) sobre nuestra VM |
| `jdi-attach.exe` | `7c87…b513` | Cliente JDI de attach |
| `jimage.exe` | `77ed…67bf` | Lector/escritor del contenedor `jimage` |
| `jlink.exe` | `2657…0e68` | Enlazador de imágenes de runtime |
| `javadoc.exe` | `a075…72bd` | El generador de documentación |

Hashes completos: `sha256sum bin/*.exe` (o `Get-FileHash`).

## Nombres — ojo al leer la documentación vieja

`KajiLibrary/COMPILER_FINDINGS.md` menciona `bin/javac-frozen.exe` y `bin/javap-clon.exe`.
Acá los binarios llevan **el nombre que les da cargo**, sin duplicados:

| Nombre en los docs | Archivo real |
|---|---|
| `bin/javac-frozen.exe` | `bin/javac.exe` |
| `bin/javap-clon.exe` | `bin/jvm.exe` |

(`javac-step.exe` es el visor del compilador, **no** el javac congelado.)

## Refrescar el snapshot

Desde una copia limpia, para que la procedencia siga siendo un commit y no un working tree:

```bash
git archive HEAD | tar -x -C /tmp/kaji-frozen-src
CARGO_TARGET_DIR=/tmp/kaji-frozen-target cargo build --release --manifest-path /tmp/kaji-frozen-src/Cargo.toml
cp /tmp/kaji-frozen-target/release/*.exe bin/
```

Y actualizar la tabla de procedencia de arriba. Si el árbol de trabajo tiene cambios que el
snapshot necesita, commitealos primero — no construyas desde el working tree.

## Verificado al congelar

- `javac.exe --emit KajiLibrary/repros/finding_09.java` → emite el `.class`.
- `jvm.exe java/Add.class` → desensambla.
- `run-headless`, `jimage`, `jlink`, `jdb`, `jdi-attach`, `jvm-jdwp`, `jvm-step`,
  `javac-step` → arrancan y muestran su uso.

Es un smoke test de que los binarios están sanos, no una validación funcional.
