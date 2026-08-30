# FZ-007 — El test de extremo a extremo corría un `run-headless` que `cargo test` no reconstruye

| campo | valor |
|---|---|
| **estado** | arreglado (2026-08-29) |
| **severidad** | media |
| **encontrado por** | el sabotaje del arreglo de FZ-001 — que no falló cuando tenía que fallar |
| **afecta** | todo test que ejecute un binario en vez de llamar a la biblioteca |

## Qué pasaba

Al cerrar FZ-001 corrí la disciplina de sabotaje: romper el arreglo a propósito y comprobar que el
test se da cuenta. Dos sabotajes, los dos sobre `src/bin/run-headless.rs` — quitar el volcado de la
consola, y quitar el `exit(1)`. **Los dos pasaron en verde.**

No porque el test estuviera mal escrito. Porque `cargo test --lib` compila la biblioteca y sus tests,
y **no reconstruye los binarios**. El test lanza `target/release/run-headless`, o sea lo que hubiera
quedado ahí de antes. Poniendo un `cargo build --release` entre el sabotaje y el test, los dos
fallaron, cada uno por su propio aserto.

## Por qué es un hallazgo y no una anécdota

Es el mismo género que FZ-003 y FZ-004: **una medición que parece estar probando algo y no lo está**.
El modo de falla es peor que un test rojo, porque un test rojo se ve. Acá el resultado verde es
indistinguible entre "el binario está bien" y "el binario que corrí no tiene nada que ver con el
árbol". Y el sesgo va para el lado malo: cuanto más viejo el artefacto, más probable que el verde
esté de más.

El comentario del test ya decía *«necesita un `target/release/run-headless` construido»*. La trampa
no era que faltara la instrucción: era que **incumplirla no se notaba**.

## El arreglo

`assert_headless_is_current()` al principio del test: si el binario no existe, o es más viejo que
`src/bin/run-headless.rs`, el test se planta con el mensaje que dice qué correr. Una guarda, no una
nota, porque una nota es exactamente lo que ya había.

## Lo que la guarda destapó de paso

`headless_path()` devolvía `…/release/run-headless`, **sin la extensión del ejecutable**. En Windows
eso funciona para lanzarlo — `Command::new` prueba con `.exe` — pero no para *inspeccionarlo*:
`fs::metadata` no encuentra nada y la guarda reportaba "falta construirlo" sobre un binario que
estaba ahí. Corregido con `std::env::consts::EXE_SUFFIX`, así que la ruta nombra el archivo que
realmente existe.

Vale la pena anotar la forma: el bug estuvo escondido justamente porque el único uso de la ruta era
lanzarla, y lanzar es la operación que perdona. El primer código que la *miró* lo encontró de
inmediato.
