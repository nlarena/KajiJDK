# FZ-001 — La VM no expone la clase de la excepción a través de `run-headless`

| campo | valor |
|---|---|
| **estado** | arreglado (2026-08-29) |
| **severidad** | media |
| **encontrado por** | construcción del ejecutor del fuzzer (nivel 2.1), midiendo la salida real de ambos lados |
| **afecta** | la comparación contra el JDK de referencia |

## Qué pasaba

Un programa cuya excepción escapa producía salidas **asimétricas** en los dos lados:

```
$ java -cp . Fz1
Exception in thread "main" java.lang.ArithmeticException: / by zero
        at Fz1.run(Fz1.java:2)

$ run-headless Fz1.class run
Fz1.class run()I -> None
```

El `java` real nombra la clase y sale con código ≠ 0. Nuestra VM imprimía `-> None` y salía con 0.

## Por qué

No era que la VM no supiera cuál fue la excepción — **sí lo sabía**. El reporte
`Exception in thread "main" ...` existe desde el hito A7 (`athrow.rs::report_uncaught`). El problema
es que iba al **buffer de consola** del VM (`shared.console`), y `src/bin/run-headless.rs` solo
imprimía el `Option<Value>` que devuelve `execute`; nunca volcaba ese buffer.

O sea: la información existía y se perdía en el último metro.

## Consecuencia para el fuzzer

Sin la clase, un `Outcome::Threw` de nuestro lado no se podía comparar contra uno del JDK real, y
todo programa que lanzara quedaba fuera del alcance del oráculo — que es justamente donde viven
muchos de los casos de esquina interesantes (división por cero, índices fuera de rango, casts).

## El rodeo (en el generador, y se queda)

Los programas generados son **totales**: se atrapan a sí mismos y codifican el resultado en el `int`
que devuelven.

```java
static int run() {
    try { return body(); }
    catch (ArithmeticException e)           { return MARK_ARITHMETIC; }
    catch (ArrayIndexOutOfBoundsException e) { return MARK_BOUNDS; }
    catch (Throwable t)                      { return MARK_OTHER; }
}
```

**Esto no se retira, y no debería.** Comparar enteros es una propiedad más fuerte que "sabemos leer
la clase": no la puede engañar un cambio de formato del reporte, y el oráculo nunca tiene que
comparar *mensajes* de excepción, que difieren entre implementaciones por razones perfectamente
legales. `Outcome::Threw` es el camino de reserva para los programas que escapan igual.

## El arreglo

Tres piezas, ninguna en el camino caliente:

1. **`SharedVm::uncaught_entry`** — `report_uncaught` anota la clase (con puntos) cuando el hilo que
   muere es el de entrada (slot 0, que es donde los dos drivers de OS dejan sus frames). Estructural
   a propósito: buscar `Exception in thread` en la consola lo engañaría un programa que imprima esa
   cadena por su cuenta. Se queda la **primera** clase — la que mató al hilo, no la que reporte una
   posterior.
2. **`execute_reporting`** — `execute` devuelve `Option<Value>` y suelta el resto al salir de
   alcance. La variante devuelve `RunReport { value, console, uncaught }`. Los dos drivers de OS
   ahora entregan también el estado compartido (`run_os_threaded_reporting`,
   `run_os_parallel_reporting`), que es de donde sale la consola cuando el hilo de entrada no es el
   dueño del `JVM`.
3. **`run-headless`** — vuelca la consola a **stderr** (no a stdout: la línea de resultado la parsea
   el runner del fuzzer, y un arreglo que la moviera habría cambiado un bug por otro) y sale con
   **1** cuando el hilo de entrada murió, que es el código que usa `java`.

Del lado del fuzzer, `interpret` lee la clase de nuestro stderr con **la misma** `uncaught_class`
que ya usaba para el JDK de referencia. Que sea la misma función no es economía: los dos lados se
comparan, y un formato que solo uno de ellos sabe leer no vale nada.

## Lo que queda afuera, y por qué

Nuestro reporte **no trae la traza** `\tat …`. No es un olvido del arreglo: `backtrace` es un campo
que la VM *ofrece* llenar (`capture_backtrace`, con el parking del throwable en la pila de operandos
para sobrevivir al GC que dispara el interning) y que `java.lang.Throwable` de KajiLibrary **no
declara** — el mismo patrón de cooperación de COMPILER_FINDINGS #227, y una decisión escrita en el
comentario de esa clase. Agregarle el campo cambia el layout de toda excepción del sistema, que es
un radio muchísimo mayor que este hallazgo; queda como decisión aparte.

Tampoco trae el mensaje cuando la excepción la levanta la **propia VM**: un `7 / 0` produce
`java.lang.ArithmeticException` sin detalle, mientras que un `throw new RuntimeException("x")`
explícito sí imprime `: x`. Medido, no supuesto. El oráculo compara clases, así que no lo afecta.

## Tests

| test | qué fija |
|---|---|
| `a_headless_throw_now_carries_the_class_like_the_reference_jdk_does` | los dos lados aterrizan en el **mismo** `Outcome`, leídos por la misma función |
| `a_non_zero_exit_with_a_report_is_a_throw_and_not_a_crash` | el código ≠ 0 sigue siendo un *throw*: leerlo como crash convertiría toda excepción en un hallazgo falso |
| `a_headless_none_with_no_report_is_a_throw_without_a_class_name` | `-> None` sin reporte es un método `void`, no una muerte |
| `a_program_whose_exception_escapes_names_the_class_and_exits_non_zero` (`--ignored`) | extremo a extremo contra el binario real: clase, código de salida **y** que la línea de resultado no se movió |

**Sabotaje**, con `cargo build` de por medio (ver FZ-007): quitar el volcado de la consola y quitar
el `exit(1)` fallan el test de extremo a extremo, cada uno por su propio aserto.
