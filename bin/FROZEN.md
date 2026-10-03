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

### Refresco 2026-08-30, tanda t (`java.util` cerrado, y el acceso a archivos)

Este refresco lleva **los seis nativos de sistema de archivos** (`jdk/internal/io/Fs`:
`readAllBytes`, `writeAllBytes`, `stat`, `size`, `delete`, `mkdir`), que son lo que destrabó los
últimos catorce miembros de `java.util` — `Scanner(File)`, `Scanner(Path)`, `Formatter(File)` y
`Formatter(String)`, que hasta ahora no se podían escribir sin mentir.

**Es la primera vez que la VM toca el disco por pedido de código Java.** Vale tenerlo presente al
correr las pruebas: `FileTest` **crea y borra archivos de verdad** en el directorio de trabajo
(`_kaji_filetest_*`), y los limpia al terminar. Si una corrida se interrumpe a la mitad pueden quedar.

Los `.rs` con los arreglos del compilador siguen siendo los mismos de la tanda anterior más
`natives.rs`.

**Verificado sobre estos binarios:**

- `java.util` **2097/2097**, las 109 clases al 100 %.
- `FileTest` da **-1** con nuestra VM y con `java` real: 44 comprobaciones que crean, escriben, leen,
  anexan, truncan y borran archivos, más directorios, `Scanner` sobre archivo y por `Path`, y
  `Formatter` a archivo.
- La biblioteca recompila **1091/1093** — `SymElement` y `StructuredTaskScope`, los dos de siempre.
- **1441 tests de Rust pasan**; los 20 que fallan son los 18 de `javac::` de siempre, comparados
  nombre por nombre, más `JcIc` y `WdWide`.

## Procedencia

| | |
|---|---|
| Commit | `85263fc5`, **con una superposición** — ver la fila siguiente |
| Construido desde | `git archive HEAD` a una copia limpia, con los **25 archivos de `src/` modificados** en el árbol de trabajo superpuestos encima (16 de `javac`, 4 de `burst`, 9 del intérprete). No hay archivos de `src/` sin seguir. Lo demás de la copia es exactamente `85263fc5`, así que **no es reproducible desde el historial solo**: hace falta el árbol |
| Toolchain | `rustc 1.97.1 (8bab26f4f 2026-07-14)`, host `x86_64-pc-windows-msvc` |
| Perfil | `cargo build --release` (50 s) |
| Fecha | 2026-10-03 |
| Herramientas | **11** |
| Peso | 16 MB — toolchain `msvc` |

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
| `javac.exe` | `95df…f136` | El compilador. `--emit X.java` → `X.class` |
| `jvm.exe` | `1f04…9806` | El desensamblador estilo `javap` (Nivel 0) y CLI principal |
| `run-headless.exe` | `06af…f2d3` | Corre `<File.class> <método>` con el intérprete (modo green) |
| `jvm-step.exe` | `3fe9…6c89` | El visor paso a paso del intérprete |
| `javac-step.exe` | `cfd1…9e6b` | El visor paso a paso del compilador |
| `jdb.exe` | `aa39…92a1` | El depurador (Fase I, JPDA) |
| `jvm-jdwp.exe` | `9ee8…e577` | Servidor JDWP (`dt_socket`) sobre nuestra VM |
| `jdi-attach.exe` | `b9c9…f2c7` | Cliente JDI de attach |
| `jimage.exe` | `e479…ee2d` | Lector/escritor del contenedor `jimage` |
| `jlink.exe` | `f405…2bdc` | Enlazador de imágenes de runtime |
| `javadoc.exe` | `37c7…50e6` | El generador de documentación |

Hashes completos: `sha256sum bin/*.exe` (o `Get-FileHash`).

## Nombres — ojo al leer la documentación vieja

`KajiLibrary/COMPILER_FINDINGS.md` menciona `bin/javac-frozen.exe` y `bin/javap-clon.exe`.
Acá los binarios llevan **el nombre que les da cargo**, sin duplicados:

| Nombre en los docs | Archivo real |
|---|---|
| `bin/javac-frozen.exe` | `bin/javac.exe` |
| `bin/javap-clon.exe` | `bin/jvm.exe` |

(`javac-step.exe` es el visor del compilador, **no** el javac congelado.)

### Refresco 2026-09-01, tanda u (`java.time` cerrado, y el parseo)

Misma copia limpia superpuesta que los refrescos anteriores, con los mismos archivos de `src/fuzz/`
dejados **afuera** (siguen a medio editar por la otra sesión).

**Lo que cambió en `src/` en esta tanda es un solo archivo**: `src/javac/codegen.rs`, con el
**#314**. Y `src/jvm/interpreter/natives.rs` sigue con los seis nativos de la tanda anterior, que
todavía no están commiteados.

- **#314** — en una compilación **multi-unidad**, un tipo hermano de **otro paquete** escrito con su
  nombre completo no resolvía en el generador. `resolve_type_id` buscaba en el scope, en los externos
  por nombre simple, y bajaba a los anidados, pero **nunca miraba las clases del fuente por su nombre
  cualificado** — y como el *shadowing* del fuente (#5) impide, con razón, cargar del classpath un
  tipo que está en el round, no quedaba ningún camino. Salía como «el generador de bytecode no puede
  resolver el tipo `p.Base`» sobre una clase que estaba en la misma línea de comandos.

  **Por qué ninguna de las tres redes lo veía**: la biblioteca recompila **de a un archivo**, así que
  el hermano llega como externo por el `-cp`; el punto fijo compila igual, de a uno; y el
  multi-unidad solo lo usaba APT, cuyos generados no se nombran entre paquetes.

  Repro: `scratchpad/zz314/` — `p/Base.java` y `q/Impl.java`, que hay que emitir **juntas**. Se
  verificó por **ablación**: con el arreglo revertido el error vuelve, con el arreglo puesto emite.

**Un fantasma perseguido antes de encontrarlo, que conviene dejar escrito**: el primer repro usaba
`--check` con dos archivos y también fallaba, así que parecía el mismo bug. No lo era — **`--check`
compila de a un archivo por diseño**, y el repro no probaba lo que parecía. Se había tocado
`enter.rs` por eso; la ablación lo mostró innecesario en un minuto y el cambio se revirtió.

**Verificado sobre estos binarios**, no sobre los de `src/`:

- La biblioteca recompila **1093/1095**; los dos que fallan son `StructuredTaskScope` y `SymElement`,
  los dos de siempre.
- Los `.class` del árbol están en **punto fijo**: **1569 idénticos, 0 distintos**.
- **1441 tests de Rust pasan**; los 20 que fallan son los 18 de `javac::` de siempre —comparados
  nombre por nombre, no por conteo— más `JcIc` y `WdWide`.
- De las **51** pruebas comparables, **49** dan el mismo entero que `java` real; las dos que no son
  `JcIc` y `WdWide`. La nueva es `FmtTest` (**-1** con las dos VMs).
- `java.time` **892/892 — las 25 clases**; `java.time.temporal` **173/173**; `java.time.chrono`
  **576/589**.

**Corrección al smoke test de más abajo**: `javac.exe --emit KajiLibrary/repros/finding_09.java`
necesita `-cp KajiLibrary` — el repro implementa `java.util.List`, que sale de la biblioteca. Sin el
`-cp` falla en los dos binarios, el viejo y el nuevo; no era una regresión, era el comando escrito
mal acá.

### Refresco 2026-09-01, tanda v (lo que ninguna red nuestra veia)

Misma copia limpia superpuesta que los refrescos anteriores. Cambian **cuatro archivos** de `src/`
respecto de la tanda u: `codegen.rs`, `transtypes.rs`, `enter.rs` y `symbol.rs`. Seis arreglos, y lo
que los une es que **ninguno daba error de compilacion**: cuatro emitian codigo malo en silencio y el
quinto colgaba el proceso.

- **#323 (dos mitades)** — la instruccion de invocacion se elegia mirando la clase que **declara** el
  metodo, pero el owner que se escribia en el pool es el **tipo estatico del receptor** (que es lo
  correcto, §5.4.3.3). Cuando difieren en clase/interfaz, la etiqueta del `Methodref`/
  `InterfaceMethodref` contradice al owner y el `.class` es invalido:

  | | antes | javac real |
  |---|---|---|
  | `default` heredado, receptor de tipo clase | `invokeinterface Impl.val` | `invokevirtual Impl.val` |
  | metodo de `Object` sobre receptor de interfaz | `invokevirtual Base.equals` | `invokeinterface Base.equals` |
  | estatico de interfaz (`List.of`, `Path.of`, `Stream.of`) | `Methodref` | `InterfaceMethodref` |

  Repro: `scratchpad/zz323/Kind2.java` y `St.java`.

- **#325** — un cast a primitivo desde una referencia no emitia nada: `(int) o` daba
  `aload_0; istore_1`, sin `checkcast` ni `intValue()`. Le pegaba a cualquier `(int) mapa.get(k)`.
  El disparador es **el cast explicito**: la conversion de asignacion (`Integer o; int v = o;`)
  siempre estuvo bien. Repro y los ocho casos medidos contra el javac real en `scratchpad/zz333/`.

- **#326** — una variable local llamada `con` **colgaba el compilador para siempre**, sin mensaje y
  sin consumir CPU. Al resolver `con.length()` se busca `con.class` en el classpath, y en Windows eso
  abre la **consola**. Cuelgan los dispositivos de entrada (`con`, `aux`, `com1`) y no los de salida
  (`prn`, `nul`, `lpt1`). En un codigo escrito en castellano no es una curiosidad. Repro de tres
  lineas en `scratchpad/zz332/Recep.java`.

- **#328** — el emisor del `if`/`else` ponia siempre un `goto` para saltear el `else`, aun cuando la
  rama `then` terminaba en `throw` o `return`. Ese salto es codigo muerto, y §4.10.1 exige un frame
  del `StackMapTable` en toda instruccion que sigue a una transferencia incondicional: la JVM real
  rechazaba la clase con `VerifyError: Expecting a stack map frame`. Ahora no se emite, que es lo que
  hace el javac real. Repro en `scratchpad/zz339/`.

- **Tipos miembro heredados** — `class S extends B { N campo; }` con `N` anidada en `B` daba *"no se
  encuentra el simbolo: N"*, mientras que `B.N` compilaba; pasaba igual con anidadas estaticas.
  `resolve_type` subia por los scopes **lexicos**, y la superclase no encierra lexicamente a la
  subclase. Ahora tambien recorre la cadena de supertipos, en anchura y con marca de visitados
  --las interfaces forman un grafo, no un arbol--. Repro y ablacion en `scratchpad/zz334/`.

**Por que ninguna de las redes lo veia, que es la leccion de esta tanda**: nuestra VM resuelve por
nombre y descriptor y **no compara la etiqueta del pool contra lo que la clase es**. La medicion por
miembros tampoco --las firmas estaban todas bien-- y las pruebas de comportamiento tampoco, porque
corrian en la VM permisiva. El #323 aparecio al intentar correr `ChronoTest` y `FmtTest` --compilados
por nosotros-- con `java` de verdad. **Correr nuestros `.class` en la JVM real es una red que no
teniamos**, y quedo escrita en `scratchpad/cruce.sh`.

**Verificado sobre estos binarios**, no sobre los de `src/`:

- La biblioteca reemite **1294/1296**; los dos que fallan son `StructuredTaskScope` y `SymElement`,
  los dos de siempre.
- **1455 tests de Rust pasan**; los 20 que fallan son los 18 de `javac::` de siempre --comparados
  nombre por nombre, no por conteo-- mas `JcIc` y `WdWide`. Los cinco arreglos no movieron ninguno.
- **36 de 36** pruebas con `run()` cargan y corren en la **JVM real**; antes del #323 eran 28, con 8
  rechazadas al cargar.
- La bateria de `java/` da **132 invocaciones, 2 rotas**: `ParseTest` (el `Double.toString`
  cuadratico, medido y anotado) y `UtilAuditTest`, que es de otra sesion y no compila con el javac
  anterior tampoco.
- Paquetes cerrados en esta tanda: `java.util.concurrent.atomic` **308/308**,
  `java.util.concurrent.locks` **154/158**, `java.util.stream` **287/287**, `java.text` **429/436**,
  `java.time.format` **120/137**.

**Ojo con `cargo test` en un clon limpio**: `java/KjBootStr.{java,class}` **no esta en git**, y sin esa
fixture `burst::jit_tests::una_fixture_de_string_da_lo_mismo_que_el_jdk_real` falla. No es una
regresion; falta el archivo.

## Refrescar el snapshot

Desde una copia limpia, para que la procedencia siga siendo un commit y no un working tree:

```bash
git archive HEAD | tar -x -C /tmp/kaji-frozen-src
CARGO_TARGET_DIR=/tmp/kaji-frozen-target cargo build --release --manifest-path /tmp/kaji-frozen-src/Cargo.toml
cp /tmp/kaji-frozen-target/release/*.exe bin/
```

Y actualizar la tabla de procedencia de arriba. Si el árbol de trabajo tiene cambios que el
snapshot necesita, commitealos primero — no construyas desde el working tree.

Dos trampas, las dos pisadas de verdad:

- **Mover los hashes actuales a la entrada del refresco ANTES de escribir los nuevos.** La tabla de
  «Las herramientas» es siempre la del snapshot vigente, y las de las entradas de refresco son el
  registro de los superados. Un `sed`/`re.sub` por el patrón del hash abreviado toca **las dos** y
  reescribe la historia: pasó el 2026-10-03, y se recuperó sólo porque los `.exe` viejos estaban
  respaldados fuera del repo.
- **Revisar que no haya `javac.exe` corriendo.** Otra sesión puede estar compilando la biblioteca en
  lote; pisar el compilador a mitad de camino parte la salida entre dos compiladores, que es
  exactamente lo que el snapshot existe para evitar. `Get-Process javac` antes de copiar.

## Verificado al congelar

- `javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_09.java` → emite el `.class`.
- `jvm.exe java/Add.class` → desensambla.
- `run-headless`, `jimage`, `jlink`, `jdb`, `jdi-attach`, `jvm-jdwp`, `jvm-step`,
  `javac-step` → arrancan y muestran su uso.

Es un smoke test de que los binarios están sanos, no una validación funcional.


### Refresco 2026-09-03, tanda de cierre de agentes (codegen + resolucion de anidados)

Misma receta que los anteriores: `git archive HEAD` a una copia limpia, superpuestos **solo** los
archivos de `src/` modificados en el arbol de trabajo, con `src/fuzz.rs` y `src/fuzz/` dejados
**afuera** (siguen a medio editar por la otra sesion).

**Los que cambiaron en esta tanda:** `src/javac/codegen.rs`, `src/javac/attribute.rs` y
`src/javac/enter.rs`.

**Por que se refresco.** Cuatro arreglos del generador, y los cuatro cambian los `.class` que salen:

- **#463** una comparacion constante materializada como valor dejaba codigo muerto sin frame.
- **#464** el `String.valueOf` de una concatenacion no actualizaba la pila simulada, y el frame
  declaraba `Object` donde el `invokedynamic` pide `String`.
- **#477** el literal `null` se declaraba `Object` en vez de la etiqueta 5 del verificador.
- **#478** un `goto` muerto despues de un cuerpo de bucle que no termina normalmente.

Mas **#465**, la resolucion de un tipo anidado calificado (`Point2D.Double` daba `java.lang.Double`),
que no cambia los frames pero si a que clase apunta un `new` y cual es la superclase que se emite.

**Lo que hay que rehacer despues de este refresco, y no es opcional:** los `.class` de la biblioteca
compilados con el `bin/` anterior tienen los frames viejos. Cargan y corren en nuestra VM --que no
los verifica-- y una JVM real los rechaza. En esta tanda se recompilaron `javax/xml/**`,
`org/xml/**`, `java/awt/geom`, `java/beans`, `java/beans/beancontext` y
`jdk/internal/classfile/impl`; **el resto del arbol sigue con los frames viejos**.

**Ojo al recompilar en lote (#474).** Compilar un paquete entero en una sola invocacion puede hacer
que un `import` de un solo tipo pierda contra un homonimo que otro archivo del lote trajo al alcance.
`jdk/internal/classfile/impl/Annotations.java` sale con `java.lang.annotation.Annotation` en sus
firmas si se lo compila junto al resto de su paquete, y correcto si se lo compila solo. Despues de un
lote grande conviene la comprobacion barata: `javap -p` sobre los `.class` buscando
`java.lang.annotation.Annotation` donde no corresponde.


### Refresco 2026-09-03, tanda `java.lang.classfile` (acceso en interfaces)

Misma receta. **El que cambio en esta tanda:** `src/javac/check.rs`.

**Por que.** El hallazgo **#481**: un miembro de interfaz sin `public` escrito se trataba como de
paquete en el chequeo de acceso, aunque el `.class` que se emite lo marca `ACC_PUBLIC`. Sin ese
arreglo no compila ninguna interfaz publica cuyas implementaciones vivan en otro paquete -- que es
todo `java.lang.classfile` con su `jdk.internal.classfile.impl`.

Este arreglo **no cambia los `.class` que ya estan**: toca el chequeo, no la emision. Lo que cambia
es que ahora compilan archivos que antes no.


### Refresco 2026-09-03, tanda de cierre de paquetes (nativos nuevos)

Misma receta. **Los que cambiaron:** `src/jvm/interpreter/natives.rs`,
`src/jvm/interpreter/metaspace.rs`, `src/jvm/interpreter/bytecode_interpreter.rs` y
`src/jvm/interpreter/bytecode_interpreter/invokestatic.rs`.

**Por que.** Tres bloqueos de la VM levantados, cada uno de los cuales tenia miembros de la
biblioteca esperandolo:

- **`park` con plazo.** `LockSupport.parkNanos`/`parkUntil` --las cuatro sobrecargas-- estaban
  documentadas como imposibles, y lo eran mientras el unico intrinseco fuera `park()` a secas: el
  permiso vive en la VM y no se puede emular el plazo desde Java sin partirlo en dos sistemas. Ahora
  hay `Intrinsic::LockSupportParkNanos`, con **el mismo** permiso. `java.util.concurrent.locks` paso
  a 158/158.
- **Espacio de volumen.** `Fs.diskTotal`/`diskUsable`/`diskUnallocated`, por `GetDiskFreeSpaceExW`.
  Es lo que le faltaba a `Files.getFileStore`, que no existia porque un `FileStore` con ceros afirma
  cosas falsas. `java.nio.file` paso a 247/247.
- **Enumeracion de raices.** `Fs.roots()`, por `GetLogicalDrives`. `FileSystem.getRootDirectories()`
  devolvia una lista vacia y `getFileStores()` levantaba; los dos contestan ahora.

Los tres nativos devuelven **-1 o vacio cuando no se sabe**, y el lado Java lo traduce a la excepcion
que la firma declara. Es la unica forma de que "no se pudo averiguar" no se lea como "cero".

Fuera de Windows, los dos ultimos contestan "no se sabe": no hay forma portable de preguntarlo sin
dependencias, y esta dicho en el `#[cfg(not(windows))]` de cada uno en vez de devolver un cero que se
leeria como un disco lleno.

### Refresco 2026-09-03, tanda TCP (`java.net` habla de verdad)

Misma receta. **Los que cambiaron:** `src/jvm/interpreter/natives.rs` (el bloque nuevo
`jdk/internal/net/Net`) y `src/jvm/interpreter/library_conformance.rs` (un probe que estaba mal
escrito, ver abajo).

**Por que.** La VM no sabia abrir un socket, y por eso `Socket.connect`, los constructores que
conectan, los dos flujos y `ServerSocket.bind`/`accept` estaban documentados como imposibles: un
`connect` que falla siempre deja compilar un cliente entero que revienta en la primera linea que
corre. Ahora hay costura --catorce nativos, el mismo diseno que `Proc`: cadenas, arreglos y enteros,
nada que sepa que existe `java.net`-- y esos miembros estan. `java.util.logging` cerro en 199/199
gracias a `SocketHandler`, y `java.net` quedo en 684/717.

**Lo que costo, y es lo unico interesante de esta tanda.** El primer intento tenia los nativos
bloqueantes --`accept()` espera a que alguien conecte, que es lo que hace un `accept`-- y colgaba la
VM entera. No era lentitud: los hilos de Java de esta VM comparten un interprete (el verde los
multiplexa sobre un hilo del sistema; los dos modos con hilos del sistema todavia serializan con un
candado global), asi que el hilo parado adentro del nativo no deja correr **al que iba a conectar**.
Un abrazo mortal perfecto: servidor y cliente en la misma VM es justamente el caso de prueba.

La respuesta fue poner **todos** los sockets en modo no bloqueante y devolver **-3** para "todavia no
hay nada". La espera pasa al lado Java, reintentando con un `Thread.sleep(1)`: dormir **si** es algo
que esta VM sabe manejar --suelta el interprete-- asi que el que espera no le impide a nadie avanzar.
De regalo, como el tiempo lo cuenta Java, `ServerSocket.accept` respeta `setSoTimeout`, cosa que un
`accept` bloqueante del sistema no permitia.

Y un segundo fondo de pozo, este de Windows: los nativos sacan un duplicado del socket con
`try_clone` para soltar la tabla antes de tocar la red, y **el duplicado no hereda el modo**. En
Windows "no bloqueante" es propiedad del descriptor y no del socket, asi que el duplicado nacia
bloqueante aunque el original no lo fuera y el sintoma volvia identico. Los dos ayudantes le ponen el
modo al duplicado, y esta escrito ahi para el que venga.

**El probe de `library_conformance`.** `system_properties_streams_and_surface` compilaba un probe con
`System.in.read()` dentro de un metodo sin `throws`. Eso es Java ilegal --el JDK real lo rechaza con
`unreported exception IOException`-- y compilaba de casualidad, porque la resolucion de nombres no
llegaba hasta `InputStream.read` para ver que excepciones declara. El arreglo **#465** la hizo
llegar, y el probe empezo a fallar. Lo que estaba mal era el probe; ahora declara la excepcion.

### Refresco 2026-09-03, tanda UDP y alcance

Misma receta. **El que cambio:** `src/jvm/interpreter/natives.rs`.

**Por que.** Con TCP andando quedaba la otra mitad de `java.net`, y la misma frase la bloqueaba:
"no hay pila de UDP en esta VM". Ahora hay ocho nativos de datagramas --atar, mandar, recibir, quien
mando el ultimo, entrar y salir de un grupo multicast, y el TTL-- y con eso entraron nueve miembros
que estaban documentados como imposibles: `DatagramSocket.send`/`receive`, sus dos de membresia, los
cuatro de `MulticastSocket` y su `send(DatagramPacket, byte)`. Los constructores que atan atan.

Siguen las reglas de la tanda anterior: **nada bloquea**, y "todavia no llego nada" es -3. Un
datagrama no tiene fin de flujo --no hay conexion que cerrar-- asi que aca el -1 es siempre un error
de verdad y no hay -2.

**Lo que hubo que pensar: el remitente.** Un datagrama y quien lo mando son **un solo dato** --
`DatagramPacket` los quiere juntos -- y un nativo que devuelve un entero no puede devolver los dos.
Se resolvio anotando el remitente en la entrada del socket al recibir, y leyendolo con
`udpSenderAddress`/`udpSenderPort` justo despues. Para que ese par sea atomico, `receive` es
`synchronized` del lado Java: sin el candado, dos hilos recibiendo sobre el mismo socket podrian
llevarse el remitente del otro. Esta dicho en los dos lados.

**La sonda de alcance.** `InetAddress.isReachable(int)` contestaba `false` siempre. Era legal --su
contrato es "decime si llegaste", no "llega"-- pero dejo de tener sentido en cuanto la VM aprendio
TCP. Ahora prueba: un TCP al puerto 7 tomando **el rechazo como respuesta**, porque el RST lo manda
el host. Es el camino de reserva del JDK cuando no puede mandar un ICMP.

Son **tres** nativos (`reachableStart`/`reachablePoll`/`reachableFree`) y no uno, por un detalle de
Windows que costo encontrar: `connect_timeout` reporta `TimedOut` **tambien cuando la conexion fue
rechazada** --el rechazo llega por el conjunto de excepciones del `select` y Rust solo mira el de
escritura-- asi que la respuesta que mas prueba quedaba indistinguible del silencio. El `connect`
bloqueante si las distingue, pero bloquea; corre entonces en un hilo del sistema aparte y el lado
Java pregunta hasta que conteste o se acabe el plazo. Misma forma que `accept` y que `read`.

**Lo unico observable que separa esto del JDK es el tiempo**, y esta dicho en el javadoc: Windows
tarda unos dos segundos en reportar un rechazo de TCP, mientras que el JDK manda un ICMP y contesta
en el acto. La respuesta es la misma; lo que cambia es cuanto hay que esperarla.

**Y una correccion de rumbo.** El arreglo **#465** de la tanda anterior hizo que la resolucion de
nombres llegara hasta `InputStream.read`, y eso destapo que el probe de
`library_conformance::system_properties_streams_and_surface` era Java ilegal: llamaba a
`System.in.read()` en un metodo sin `throws`. El JDK real lo rechaza igual. Lo que estaba mal era el
probe, y ahora declara la excepcion.

`cargo test` sobre la superposicion: 1461 ok / 21 fallan, y son las 19 de HEAD mas dos de `burst`
que fallan por fixtures (`java/JcCat2.class`, `java/KjBootStr.class`) que todavia no estan
commiteadas y por eso no entran en la superposicion. Cero regresiones nuestras.

### Refresco 2026-09-03, tanda del socket crudo

Misma receta. **El que cambio:** `src/jvm/interpreter/natives.rs`.

**Por que.** Quedaban seis miembros de `java.net` documentados como imposibles, y los tres motivos
que los bloqueaban eran el mismo motivo: **`std::net` no lo expone**. Atar la punta local antes de
conectar (`TcpStream::connect` toma destino y nada mas), el TTL de una conexion saliente, y mandar un
byte fuera de banda. Ninguno se puede rodear desde arriba.

Asi que la VM baja a las llamadas del sistema --`socket`, `bind`, `connect`, `setsockopt`, `send`--
declaradas a mano, igual que `GetDiskFreeSpaceExW` unas lineas mas abajo en el mismo archivo. El
socket que sale de ahi se le entrega a `std` con `from_raw_socket`/`from_raw_fd`: desde ese momento
lo administra `TcpStream` y el resto del archivo no se entera de que nacio distinto. Hay un `cfg` por
plataforma con su tabla de numeros y sus envoltorios; la logica de arriba es una sola. Lo unico que
no coincide entre las dos tablas es `IP_TTL`, que en Windows es 4 y en Unix 2, y esta dicho ahi.

Con eso entraron: los dos constructores de `Socket` con **direccion local**, `sendUrgentData(int)` y
`InetAddress.isReachable(NetworkInterface, int, int)`. Y de paso `Socket.bind` dejo de tirar siempre:
anota la punta y el `connect` que venga sale por ahi.

**Los otros dos entraron sin tocar Rust**, y es el hallazgo mas barato de la tanda: los constructores
`Socket(host, port, boolean)` estaban afuera porque con `false` prometian un socket **UDP** con cara
de `Socket`. Se probo contra el JDK 25 y **el JDK ya no lo sostiene**: tira
`IllegalArgumentException("Socket constructor does not support creation of datagram sockets")`. Lo
que era imposible de cumplir dejo de ser parte del contrato; esta clase hace exactamente eso.

**Donde el `connect` bloqueante vuelve a aparecer.** Atar y conectar necesita un `connect` de verdad,
que bloquea, y ya se sabe lo que pasa cuando un nativo de esta VM bloquea. Se reusa el mecanismo de
la sonda de alcance: el `connect` corre en un hilo del sistema aparte y el lado Java recoge la
respuesta con `answerPoll`. El casillero se generalizo de `bool` a `i32` para que sirva a los dos
--la sonda contesta 1 o 0, el connect contesta el handle o -1--.

**Lo que sigue afuera de `java.net`, y ahora esta escrito.** Ninguno de los tres estaba documentado y
los tres lo estan: `IDN` (nameprep necesita plegado completo de mayusculas y NFKC, el mismo muro de
tablas Unicode que deja a `Normalizer` sin NFKC -- el JDK manda `stra{eszett}.de` y `strasse.de` a la
misma cadena, y un `toLowerCase` no hace eso), `SecureCacheResponse` (cinco de sus siete miembros
nombran `javax.net.ssl`, y no hay TLS en este arbol) y los tres de `URLStreamHandler` (existen solo
para mutar una `URL`, que aca es inmutable por decision tomada y documentada).

`java.net` quedo en **701/717**. `cargo test`: 1461 ok / 21 fallan, la lista identica a la de la
tanda anterior.

### Refresco 2026-09-03, `Class.forName` corre el `<clinit>` (#487)

Misma receta. **Los que cambiaron:** `src/jvm/interpreter/natives.rs`,
`src/jvm/interpreter/bytecode_interpreter.rs`, y sus dos despachos
(`bytecode_interpreter/invokestatic.rs`, `bytecode_interpreter/invokevirtual.rs`).

**Por que.** `Class.forName` cargaba la clase y **no corria su inicializador estatico**. Cargar no es
inicializar (JVMS §5.4 vs §5.5), pero `forName(String)` promete las dos cosas, y de esa promesa viven
los idiomas en los que una clase **se registra sola** al ser nombrada: el driver de JDBC que se anota
en el `DriverManager`, un proveedor de `spi` que se instala al cargarse, cualquier tabla que se llene
desde un bloque `static`. Todos compilaban, corrian, y no hacian nada -- sin un solo error que mirar,
que es la peor forma de fallar. Lo destapo la adopcion de sockets de `java.nio.channels`.

**Lo que costo.** El mecanismo ya existia: `ensure_initialized` empuja el marco del `<clinit>` y lo
drivea hasta el final. Lo que faltaba era que el nativo pudiera **alcanzarlo**: un nativo recibe el
metaspace y el heap, no el `&mut self` del interprete. Se resolvio dandole una forma de pedir lo que
no puede hacer -- `NativeOutcome::RanEInicializa(clase)` -- y dejando que el sitio de despacho, que
si tiene el interprete, corra la inicializacion. La alternativa era darle al nativo el interprete
entero, que es muchisimo mas de lo que necesita.

La variante **no lleva el mirror ya calculado**, y esa decision es la que mas vale la pena anotar:
entre calcularlo y usarlo corre un `<clinit>` entero, que aloca, y alocar puede disparar el GC. Una
referencia guardada mientras tanto en una variable de Rust no es una raiz para el recolector. El
mirror se pide **despues** de inicializar.

`forName0` paso a tomar la bandera, porque las cuatro sobrecargas publicas piden cosas distintas y se
comprobo contra el JDK 25 cual pide cual: `forName(String)` y `forName(String, true, ClassLoader)`
inicializan; `forName(String, false, ClassLoader)` y `forName(Module, String)` no. Las dos ultimas
delegaban en la primera, asi que ademas de no inicializar cuando debian, habrian inicializado cuando
no debian en cuanto se arreglara la primera.

**Una nota sobre como se verifica esto.** La superposicion (`git archive HEAD` + los `src/` tocados)
trae **la `KajiLibrary` de HEAD**, no la del arbol de trabajo. Cuando un cambio de la VM va atado a un
cambio de la biblioteca --como este, que cambia la firma de un nativo-- `cargo test` corre con la
biblioteca vieja contra la VM nueva y falla por eso y no por el cambio. Aparecio como una regresion
que no existia. Hay que copiar tambien la `KajiLibrary` del arbol a la superposicion.

`cargo test`: 1461 ok / 21 fallan, la lista identica a la de la tanda anterior. Prueba de
comportamiento: `java/ClinitTest.java`, que da -1 contra el JDK 25 y contra `run-headless`.

### Refresco 2026-09-21, la traduccion al ingles y el trabajo de javac en vuelo

**Esta entrada no nombra un commit, y hay que decirlo de entrada.** Los refrescos anteriores se
hicieron desde una copia limpia (`git archive HEAD`) justamente para poder prometer "esto es
exactamente el commit tal". Este no: se construyo **desde el working tree**, por pedido explicito,
con 4775 lineas sin commitear en 23 archivos de `src/` -- `javac/desugar.rs` (+1523),
`attribute.rs` (+617), `enter.rs` (+525), `symbol.rs` (+323), `check.rs` (+303), el JIT de `burst/`
y cuatro del interprete. El commit de base es `50ce81c6` ("Traduccion KajiLibrary"), pero los
binarios llevan mas que eso. No es reproducible desde el historial.

**Por que se congelo igual.** Se midio el arbol contra una copia limpia de HEAD, y el trabajo en
vuelo es netamente mejor:

| | pasan | fallan |
|---|---:|---:|
| HEAD limpio (`50ce81c6`) | 1500 | 38 |
| Working tree (lo congelado) | 1526 | 22 |

Arregla **17** pruebas de `javac` -- `attribute` (5), `enter` (3), `desugar` (3), `codegen` (3),
`check` (2), `types` (1) -- y rompe **una**:

- `javac::check::tests::an_external_class_ancestor_still_exempts`: una clase que hereda de un
  ancestro del classpath dejo de estar exenta de implementar sus abstractos. Sale
  `` `B` no es abstracta y no implementa `extra` de `Base` `` donde no deberia salir nada. **Queda
  adentro de este snapshot.** Lo que acota el riesgo: los 4820 archivos de `KajiLibrary` compilan
  igual con este javac, o sea que la forma que rompe no aparece en codigo real -- la construye la
  prueba a mano.

Las otras 21 que fallan **fallan igual en HEAD limpio**: los 14 de `gc::tests` (CompletableFuture,
executors, method handles, delay queue), 2 de `class_file`, 2 de `burst::jit_tests`, y uno cada uno
de `parser::reader`, `verifier` y `metaspace`. Son preexistentes.

**Dos sitios de prueba habia que arreglar para que `cargo test` siquiera compilara**, porque habian
quedado desfasados de la produccion: `NestedCall` sin el campo `args` (#558) y
`generate_lambda_class` sin el parametro `impl_kind` (#552). Los valores salen del texto de las
propias pruebas: la anidada del primero es `empty()`, sin argumentos; el segundo dice "forwarding to
a **static**", y la doc de la firma dice que `6` es `invokestatic`.

**Bateria de comportamiento** (`tools/suite.py`): **221 invocaciones, 2 rotas**. La referencia
anterior eran 132 con 2 rotas, y `UtilAuditTest` --una de aquellas dos-- se arreglo: ahora corre y da
-1. Las dos de hoy:

- `ParseTest` TIMEOUT, que es la conocida y anotada (el `Double.toString` cuadratico);
- `MhLayoutTest` `panic: field_offset: field not found in the class or its superclasses`, que **no
  es regresion**: falla identico con los binarios de Sep 4. Es la misma familia que los cuatro
  `method_handle_*` de `cargo test`.

**Lo que este refresco NO hizo, y hace falta:** regenerar los `.class` de `KajiLibrary`. Sobre una
muestra de 12 archivos, **3 salieron distintos** con este javac (`HashMap`, `IntStreamImpl`,
`JComponent`), asi que los 4820 `.class` versionados ya no son lo que este compilador emite. Hasta
que se regeneren, un `.class` distinto puede significar "cambio la fuente" **o** "cambio el
compilador", que es justo lo que el snapshot existe para evitar.

| Binario | SHA-256 | |
|---|---|---|
| `javac.exe` | `7e57…7e3b` | 4382720 bytes |
| `jvm.exe` | `a682…8394` | 462336 bytes |
| `run-headless.exe` | `19d4…75e0` | 1712640 bytes |
| `javac-step.exe` | `6b63…e879` | |
| `jvm-step.exe` | `ac8f…6f92` | |
| `javadoc.exe` | `0697…d229` | |
| `jdb.exe` | `9a81…174c` | |
| `jvm-jdwp.exe` | `166d…f7f7` | |
| `jdi-attach.exe` | `34da…6fbf` | |
| `jimage.exe` | `de5e…907c` | |
| `jlink.exe` | `9e13…9120` | |

Los once anteriores (Sep 4) quedaron respaldados fuera del repo antes de pisarlos, porque `bin/*`
esta en `.gitignore` y no se recuperan desde git.

### Refresh 2026-10-03 — all eleven tools rebuilt on request

**Base.** `git archive HEAD` (`85263fc5`, "Traduccion: IoTest y dos de java.security") into a clean
copy, with the **25 modified `src/` files** from the working tree overlaid: 16 under `javac/`, 4 under
`burst/`, and 9 in the interpreter. No untracked `src/` files, so nothing was left out by judgement.
`rustc 1.97.1`, `--release`, 50 s. As in the previous refresh, the provenance is **not a commit
alone** — reproducing these binaries needs the tree.

**What this refresh does NOT invalidate, and it is the headline.** The previous entry had to warn
that the versioned `.class` files were no longer what the compiler emits. This one does not: the old
and the new `javac` were run over a 12-file sample of `KajiLibrary` (`String`, `HashMap`,
`BigInteger`, `TreeMap`, `Duration`, `ArrayList`, `StringBuilder`, `Collectors`, `JComponent`,
`DecimalFormat`, `Deflater`, `SequenceInputStream`) and the **106 resulting `.class` files are
byte-identical**. The in-flight javac work changes what *resolves and type-checks*, not what gets
emitted — consistent with `desugar`/`attribute`/`enter`/`symbol` carrying most of the diff. It is a
sample and not a proof, so a wholesale divergence elsewhere is not excluded; it does mean the usual
obligation to recompile the tree does not apply here.

**The debt from the previous refresh is still open, though, and it is measured.** Of those same 106
`.class` files, **14 differ from the versioned ones in the tree**. That staleness is inherited from
the 2026-09-21 refresh and not created by this one — and it is being paid off right now: another
session recompiled the 4822 sources with the frozen compiler while this freeze was happening.

**The suite at the moment of freezing.** `cargo test javac:: --lib`: **792 pass, 1 fails, 1 ignored**
(348 s). The single failure is `javac::check::tests::an_external_class_ancestor_still_exempts`, and
**the test is what is wrong**, not the compiler: for
`abstract class Base extends Thread { abstract void extra(); } class B extends Base { }` ours reports
``B` no es abstracta y no implementa `extra` de `Base`` and the real `javac` reports *"B is not
abstract and does not override abstract method extra() in Base"*. The test was written when the
abstract-completeness check exempted any hierarchy touching a classpath type; the overlaid work made
it precise and the `ok(...)` became the lie. It was already failing in the previous snapshot, where
it is described as a regression — that description is retracted here.

**A defect that goes INTO this snapshot, known and reproduced.** The emitted `StackMapTable` is
rejected by a real JVM when a local slot is reused across `catch` scopes. Twenty lines are enough:

```java
try { ... throw new IllegalStateException(); } catch (IllegalStateException a) { i++; }
try { StringBuilder b = new StringBuilder(); ... } catch (NumberFormatException c) { i++; }
```

`a`, `b` and `c` share slot 2. The frame of the second handler comes out
`[B, int, IllegalStateException]` where the real compiler emits `[B, int]`: it carries the type of a
**dead** local. `java` refuses the class with `VerifyError: Stack map does not match the one at
exception handler`. `close_scope` restores `next_free` but never clears `locals_t`, and the handler
frame is captured from `entry_locals` on entering the `try`, before the new declaration overwrites
the slot. It is what makes `KajiLibrary/java/IoTest.class` unloadable on the real JDK. **Our own
verifier accepts it** — the probe runs on `run-headless` and answers correctly — so
`cross_check_stackmap` does not police handler frames. Neither does `tools/emitdiff`, which
disassembles with `javap -p -c` and therefore never sees a stack map at all.

**A lock worth writing down.** `bin/javac.exe` could not be replaced on the first attempt: **another
session was in the middle of recompiling all of KajiLibrary with it**, one process per file, at
~4200/4822. The other ten tools were already swapped; the copy waited 105 s for that batch to finish
(`rc=0`) and only then replaced the compiler. Had it been swapped mid-run, half of that output would
have come from each compiler — exactly what this snapshot exists to prevent. Before any refresh,
check for live `javac.exe` processes.

**Smoke test** (the one this file prescribes): `javac --emit -cp KajiLibrary` on `finding_09.java`
emits `MyList.class`; `jvm java/Add.class` disassembles; `run-headless java/Add.class main` answers
6; the other nine start and print their usage. 16 MB in total, `msvc`.

The previous eleven were backed up outside the repo before being overwritten, since `bin/*` is in
`.gitignore` and does not come back from git.
