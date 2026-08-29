# FZ-002 — `DANGLING` intermitente en `verify_heap` (1 de 10 corridas)

| campo | valor |
|---|---|
| **estado** | abierto |
| **severidad** | alta |
| **encontrado por** | el agente del Grupo 1 del JIT (tipos anchos), corriendo `JVM_GC_VERIFY=1` |
| **afecta** | `os-parallel` |

## Qué pasa

Con el verificador de heap activo, una de cada ~10 corridas de la suite de GC falla con:

```
verify_heap: DANGLING field 5072 (region=Old, header->java/lang/RuntimeException)
          -> 8 (region=Eden, header->java/lang/String) @slot 5084
```

dentro de `completable_future_exceptionally_recovers_pool_failure`, panicando en un hilo worker del
pool. Ese test corre green x1, os-gil x1 y **os-parallel x10**.

## Frecuencia medida

| quién | corridas | apariciones |
|---|---|---|
| Grupo 1 (tipos anchos) | 4 | 1 |
| Grupo 4 (arrays) | 2 | 0 |
| Grupo 2b (auditoría) | 3 | 0 |
| Grupo 5 (athrow) | 1 | 0 |
| **total** | **10** | **1** |

## Por qué no es del JIT

El slot corrupto es `RuntimeException.detailMessage`: un **campo de referencia de un objeto Old**.
El codigo compilado **no puede escribir un campo de referencia** — `putfield`/`putstatic` de
referencia se rechazan con `Ineligible::ReferenceWrite`, `iastore` solo acepta `Int`, y `aastore`
esta fuera del subconjunto. Eso se confirmo ademas por sabotaje: el agente 2b borro esa restriccion
a proposito (S7) y un test la atrapo, lo que prueba que el rechazo esta realmente activo.

Ademas el JIT esta **apagado en `os-parallel`** en los dos puntos de despacho.

Esa es la razon por la que el limite de solo-lectura sobre referencias se mantiene deliberadamente:
mientras el codigo compilado no pueda escribir un puntero, este hallazgo se puede atribuir con
seguridad al heisenbug conocido y no a nosotros. **Habilitar el write barrier borra ese argumento**,
asi que conviene cerrar este hallazgo antes.

## Relacion con el heisenbug de `os-parallel`

Coincide con el bug abierto hace ~17 sesiones (referencia stale bajo GC concurrente). Ver la memoria
`os-parallel-gc-stale-ref-heisenbug`. El guard `had_local_frames` cerro un mecanismo; queda un
segundo abierto. Reproductor single-thread ~50%: `java/BxDbgT.java` / `java/BxDbgY.java`.

## Por que el fuzzer puede ayudar

Un generador de programas concurrentes corriendo el **mismo** programa muchas veces en `os-parallel`
y comparandolo contra si mismo detecta no-determinismo sin necesidad de saber el resultado correcto.
Hoy dependemos de `gc_race_stress`, un caso escrito a mano; el fuzzer explora el espacio que nadie
penso.
