# fuzzer_findings.md — registro de hallazgos

Índice de todo lo que el fuzzer diferencial (`src/fuzz/`) encuentra, más los hallazgos previos que
comparten su naturaleza: **una diferencia observable entre dos caminos que deberían coincidir**.

Cada hallazgo tiene un ID `FZ-nnn`, una línea acá, y un archivo con el detalle en
`docs/fuzzer_findings/`. La regla de oro del registro: **un hallazgo sin caso mínimo reproducible no
está terminado** — el programa de 200 líneas que salió del generador no sirve, hace falta lo que
queda después del reductor.

## Estados

| estado | significa |
|---|---|
| `abierto` | reproducido, sin arreglar |
| `arreglado` | arreglado y con test de regresión en la suite |
| `por diseño` | la diferencia es real y aceptada; queda en la lista de divergencias conocidas del oráculo |
| `infra` | no es un bug del sistema bajo prueba sino de la herramienta o del entorno |

## Índice

| ID | título | severidad | estado | detalle |
|---|---|---|---|---|
| FZ-001 | La VM no expone la clase de la excepción a través de `run-headless` | media | arreglado | [detalle](fuzzer_findings/FZ-001-excepcion-sin-clase.md) |
| FZ-002 | `DANGLING` intermitente en `verify_heap` | alta | abierto — **no reproducible acá** (0 en 4000 corridas) | [detalle](fuzzer_findings/FZ-002-dangling-intermitente.md) |
| FZ-003 | El `javac` del `PATH` puede ser el nuestro bajo `cargo test` | media | arreglado | [detalle](fuzzer_findings/FZ-003-javac-del-path.md) |
| FZ-004 | El brazo "JIT" de la campaña corría el intérprete el 88% de las veces | alta | arreglado | [detalle](fuzzer_findings/FZ-004-jit-nunca-compila.md) |
| FZ-005 | Los programas con arrays morían antes de que el JIT los mirara | alta | arreglado | [detalle](fuzzer_findings/FZ-005-arrays-mueren-antes-del-jit.md) |
| FZ-006 | El reductor no podía reemplazar por una constante una hoja que **nombra** algo | media | arreglado | [detalle](fuzzer_findings/FZ-006-el-reductor-no-podia-soltar-una-referencia.md) |
| FZ-007 | El test de extremo a extremo corría un `run-headless` que `cargo test` no reconstruye | media | arreglado | [detalle](fuzzer_findings/FZ-007-el-test-corre-un-binario-viejo.md) |
| FZ-008 | Los literales de `String` no se internan (`"a" == "a"` daba `false`) | alta | arreglado | [detalle](fuzzer_findings/FZ-008-literales-sin-internar.md) |
| FZ-009 | La sonda de identidad de `String` la borraba el compilador antes de llegar a la VM | alta | arreglado | [detalle](fuzzer_findings/FZ-009-la-sonda-que-el-compilador-borraba.md) |

## Lo que el registro deja ver

Seis de los nueve hallazgos —FZ-003, FZ-004, FZ-005, FZ-006, FZ-007 y FZ-009— **no son bugs de la
VM**: son bugs de la herramienta que la mide. Una campaña que compila con el compilador bajo prueba,
un brazo "JIT" que corría el intérprete, programas que morían antes de que el JIT los mirara, un
reductor que no podía soltar una referencia, un test que ejecutaba un binario que ya no existía en el
árbol, y una sonda que el compilador borraba antes de que la VM la viera. Todos comparten la misma
forma: **parecen estar probando algo y no lo están**, y el resultado es verde en los seis casos.

**FZ-009 es el que muestra por qué esto importa y no es higiene.** Tapó a FZ-008, una no-conformidad
con la JLS que estuvo viva mientras la campaña que existía para vigilarla reportaba 0 divergencias —
y ese silencio se usó como evidencia para *quitar* la supresión del oráculo afirmando que el bug
estaba arreglado. La herramienta no falló en encontrar el bug: **confirmó la creencia falsa**.
Arreglada la sonda, la campaña lo encontró en minutos y el reductor lo dejó en dos líneas; FZ-008
está cerrado desde el mismo día.

De ahí las dos reglas de trabajo del hito. Los tres primeros aparecieron **midiendo, no razonando**;
FZ-007 y FZ-009 aparecieron **comprobando una afirmación** — el sabotaje que no falló cuando tenía
que fallar, y una frase del roadmap que resultó no ser cierta. Un reporte limpio de una herramienta
que no se auditó a sí misma no es evidencia de nada. Y el corolario nuevo, de FZ-009: **cuando los
dos lados de un pareo comparten el compilador, todo lo que el compilador resuelva es invisible para
el oráculo.**

## Divergencias conocidas y legítimas

Lo que el oráculo debe **ignorar** para no reportar el mismo no-bug para siempre. Cada entrada
necesita su justificación: una lista de excepciones sin argumentos es una forma elegante de esconder
bugs.

| diferencia | por qué es legítima |
|---|---|
| ~~`"a" == "a"` da `false` acá y `true` en el JDK real~~ — **RESUELTO (2026-08-22)** | Era una **no-conformidad con la JLS §3.10.5**, no una diferencia legítima de implementación: los literales de String deben internarse. La VM ahora tiene tabla de interning (en Old, raíz de GC y en el conjunto `pinned` del compactador), con los literales separados de los strings calculados — `new String("a") == "a"` sigue dando `false`, como corresponde. **Esta entrada llegó a ser peligrosa**: mientras estuvo listada como divergencia conocida, el fuzzer habría *suprimido* la regresión si el interning se hubiera roto. Un supresor sobrevive a la razón que lo justificaba |
| Los mensajes de las excepciones difieren | dos implementaciones pueden redactar distinto y las dos estar bien; el oráculo compara **clases**, no textos |
