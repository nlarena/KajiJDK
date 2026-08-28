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
| FZ-001 | La VM no expone la clase de la excepción a través de `run-headless` | media | abierto | [detalle](fuzzer_findings/FZ-001-excepcion-sin-clase.md) |
| FZ-002 | `DANGLING` intermitente en `verify_heap` (1 de 10 corridas) | alta | abierto | [detalle](fuzzer_findings/FZ-002-dangling-intermitente.md) |
| FZ-003 | El `javac` del `PATH` puede ser el nuestro bajo `cargo test` | media | arreglado | [detalle](fuzzer_findings/FZ-003-javac-del-path.md) |
| FZ-004 | El brazo "JIT" de la campaña corría el intérprete el 88% de las veces | alta | arreglado | [detalle](fuzzer_findings/FZ-004-jit-nunca-compila.md) |
| FZ-005 | Los programas con arrays morían antes de que el JIT los mirara | alta | arreglado | [detalle](fuzzer_findings/FZ-005-arrays-mueren-antes-del-jit.md) |

## Divergencias conocidas y legítimas

Lo que el oráculo debe **ignorar** para no reportar el mismo no-bug para siempre. Cada entrada
necesita su justificación: una lista de excepciones sin argumentos es una forma elegante de esconder
bugs.

| diferencia | por qué es legítima |
|---|---|
| ~~`"a" == "a"` da `false` acá y `true` en el JDK real~~ — **RESUELTO (2026-08-22)** | Era una **no-conformidad con la JLS §3.10.5**, no una diferencia legítima de implementación: los literales de String deben internarse. La VM ahora tiene tabla de interning (en Old, raíz de GC y en el conjunto `pinned` del compactador), con los literales separados de los strings calculados — `new String("a") == "a"` sigue dando `false`, como corresponde. **Esta entrada llegó a ser peligrosa**: mientras estuvo listada como divergencia conocida, el fuzzer habría *suprimido* la regresión si el interning se hubiera roto. Un supresor sobrevive a la razón que lo justificaba |
| Los mensajes de las excepciones difieren | dos implementaciones pueden redactar distinto y las dos estar bien; el oráculo compara **clases**, no textos |
