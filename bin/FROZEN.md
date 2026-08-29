# `bin/` — herramientas congeladas

Snapshot binario de **todas** las herramientas del proyecto, commiteado a propósito.

**Por qué se commitea.** La sesión de biblioteca compila KajiLibrary con un `javac` *fijo*:
si cada máquina usa el que le salga de su `src/javac` del momento, un `.class` distinto deja
de significar "la fuente cambió" y pasa a significar "el compilador cambió", que es
exactamente la señal que el dogfooding necesita leer. Congelarlas también desacopla el
trabajo de biblioteca de que el árbol de `src/` compile en ese instante — que no es
hipotético: hay sesiones editando `src/jvm/` en paralelo.

> **Costo, para tenerlo presente:** son ~11 MB y los `.exe` no deltifican, así que **cada
> refresco suma otros ~11 MB al historial**. Refrescar sólo cuando haya una razón (un fix de
> compilador que la biblioteca necesita), no por rutina.
>
> El `.gitignore` los excluyó por un tiempo, con el argumento de que eran ~48 MB. Ese número
> era el de la build `windows-gnu`; la `msvc` da 11 MB para las diez herramientas, y a ese
> precio el snapshot exacto en el historial vale más que el ahorro. Quedan commiteados.

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
| Commit | `8f0de93` |
| Construido desde | una copia **limpia de HEAD** (`git archive HEAD`), no del árbol de trabajo |
| Toolchain | `rustc 1.96.0 (ac68faa20 2026-05-25)`, host `x86_64-pc-windows-msvc`, LLVM 22.1.2 |
| Perfil | `cargo build --release` |
| Fecha | 2026-08-25 |

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
| `javac.exe` | `9dc6…a634` | El compilador. `--emit X.java` → `X.class` |
| `jvm.exe` | `5d71…0bb2` | El desensamblador estilo `javap` (Nivel 0) y CLI principal |
| `run-headless.exe` | `587d…cc20` | Corre `<File.class> <método>` con el intérprete (modo green) |
| `jvm-step.exe` | `cc56…1b5f` | El visor paso a paso del intérprete |
| `javac-step.exe` | `2ae3…0c24` | El visor paso a paso del compilador |
| `jdb.exe` | `61f3…af3d` | El depurador (Fase I, JPDA) |
| `jvm-jdwp.exe` | `edf9…f580` | Servidor JDWP (`dt_socket`) sobre nuestra VM |
| `jdi-attach.exe` | `b2ac…9478` | Cliente JDI de attach |
| `jimage.exe` | `26a1…417e` | Lector/escritor del contenedor `jimage` |
| `jlink.exe` | `a933…5be7` | Enlazador de imágenes de runtime |

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
