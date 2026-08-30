# FZ-002 — `DANGLING` intermitente en `verify_heap`

| campo | valor |
|---|---|
| **estado** | abierto — **no reproducible en esta máquina** (2026-08-29) |
| **severidad** | alta si es real; la evidencia es de 10 corridas |
| **encontrado por** | el agente del Grupo 1 del JIT (tipos anchos), corriendo `JVM_GC_VERIFY=1` |
| **afecta** | `os-parallel` |

## Qué se reportó

Con el verificador de heap activo, una de cada ~10 corridas de la suite de GC falla con:

```
verify_heap: DANGLING field 5072 (region=Old, header->java/lang/RuntimeException)
          -> 8 (region=Eden, header->java/lang/String) @slot 5084
```

dentro de `completable_future_exceptionally_recovers_pool_failure`, que corre green x1, os-gil x1 y
**os-parallel x10**.

## Lo que se midió el 2026-08-29

**El reproductor no dispara acá.** El test nombrado, con `JVM_GC_VERIFY=1`, corrido directo contra
el binario de test: **60 de 60 en verde**.

Y no es que falte presión. El nivel de hilos del fuzzer (K4) existe desde hoy, y se lo apuntó a este
hallazgo:

| medición | resultado |
|---|---|
| programas concurrentes, `os-parallel`, contra sí mismos | **200 semillas × 20 corridas = 4000 ejecuciones** |
| con `JVM_GC_AUTO=1`, `JVM_GC_OCCUPANCY=0.2`, `JVM_GC_TENURE=1`, `JVM_GC_VERIFY=1` | |
| colecciones por corrida (muestra de 40 semillas) | media **528**, máximo **2016**, mínimo 2 |
| divergencias | **0** |
| panics (`Crashed`) | **0** |

Esa medición de colecciones importa más que el cero. La primera versión de la campaña corría con la
recolección automática **apagada** (`DEFAULT_AUTO = false`) y coleccionaba 2 veces por corrida: un
reporte limpio ahí no habría dicho nada sobre el GC, y habría parecido idéntico a éste. Es la
lección de FZ-004 aplicada por adelantado — **primero se mide que el instrumento toque el sistema, y
recién después significa algo que se calle**.

Segunda cifra que califica el cero: **11 de 40 semillas mueren en un marcador de excepción** y por lo
tanto trabajan muy poco. La cobertura real es menor que el conteo de semillas.

## La auditoría de raíces, y lo que sí encontró

La clase del bug es «algo guarda un offset del heap y cruza un safepoint sin ser raíz» — que es
exactamente lo que era `pending_exception` (arreglado, `java/PeGcStale.java`). Se recorrieron los
sostenedores de offsets:

| sostenedor | estado |
|---|---|
| `RunningCtx.frames` | raíz (`gc::roots` los camina) |
| `parked_exception` | es una bandera; el throwable va en la pila de operandos, que ya es raíz |
| `frame_pool` | no sostiene referencias, y está documentado que nunca debe |
| `SharedVm.monitors` (clave = offset) | **remapeado** tras compactar |
| `condy` | raíz, y remapeado en `compact` |
| `threads[*].thread_obj`, `wait_reacquire` | remapeados |
| mirrors | **pinneados** y escaneados siempre como raíz Old→young |
| remembered set | reconstruido tras el major **y** tras `compact` |
| referencias entre objetos | reescritas por `compact` paso 3(b), sobre **todos** los objetos |

Dos cosas salieron de ahí, y ninguna es el bug:

1. **Una escritura de referencia que se salteaba la barrera.** `capture_backtrace` guardaba el
   `String` interneado con `heap.write_u32` en vez de `HeapService::store_reference`, que la
   documentación describe como *la* puerta única «para que la barrera no se pueda olvidar». Hoy no
   dispararía —`strings::intern` aloca en Old, así que no hay arista Old→young que recordar— pero eso
   es un hecho sobre dónde vive la tabla de interning, no sobre esa línea. Corregido.

2. **Un aviso viejo en `compact`, que era peor que no tener aviso.** Decía que las referencias entre
   objetos quedaban sin reescribir «hasta que exista el slot walk» y que por eso el paso era «seguro
   para grafos sin esas referencias (p. ej. las demos)». `reference_slots` existe hace mucho y el
   paso 3(b) la usa sobre todos los objetos. Un aviso que sobrevivió a su causa no solo deja de
   informar: **invita a quien persigue un puntero colgado a descartar este camino por conocido-roto y
   buscar en otro lado** — que es lo contrario de para lo que sirve un aviso. Reescrito por lo que el
   pase hace de verdad.

Queda anotado un punto de la auditoría que no es un bug pero sí una dirección incómoda:
`reference_slots` devuelve **la lista vacía** cuando el header de un objeto no resuelve a un mirror
conocido. Eso es inalcanzable si el invariante de `class_id` se cumple, pero falla en silencio y
hacia «este objeto no tiene referencias», que es el lado equivocado para que un colector se
equivoque.

## Qué significa el estado

**Abierto, no cerrado.** Cuatro mil corridas sin reproducir no prueban que no exista: prueban que no
es alcanzable por *esta* forma en *esta* máquina. Y la forma tiene un límite que conviene tener
escrito — [`Stmt::Fork`] es rígida a propósito para que el resultado del programa sea determinista,
así que **solo puede detectar carreras que rompan el determinismo**. Esa es la clase correcta (una
referencia stale bajo GC da un valor distinto), pero no cualquier entrelazado.

Lo que **no** se debe hacer es bajarle la severidad porque no aparece. La evidencia original son 10
corridas con 1 aparición; eso es demasiado poco para afirmar «1 de cada 10» y demasiado para
descartarlo.

## Lo que sigue

- **Presión donde el bug vive, no donde es fácil**: el síntoma es un campo de **referencia** de un
  objeto **Old** apuntando a Eden. La gramática todavía no genera campos de referencia (bloqueados
  por la barrera de escritura del JIT), así que la única forma de crear esa arista hoy es indirecta.
- **Correr en otra máquina.** Este hallazgo y el heisenbug de `os-parallel` comparten la propiedad de
  no observarse acá; el conteo de núcleos y el modelo de memoria son parte del experimento.
- **ThreadSanitizer** sigue fuera de alcance en Windows (`x86_64-pc-windows-*` solo soporta ASan).

## Relación con el heisenbug de `os-parallel`

Coincide con el bug abierto hace ~17 sesiones (referencia stale bajo GC concurrente). El guard
`had_local_frames` cerró un mecanismo. Los reproductores conservados, `java/BxDbgT.java` y
`java/BxDbgY.java`, tampoco disparan hoy: **20 de 20 y 12 de 12 idénticas** en `os-parallel`.
