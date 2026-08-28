# FZ-001 — La VM no expone la clase de la excepción a través de `run-headless`

| campo | valor |
|---|---|
| **estado** | abierto (con rodeo en el generador) |
| **severidad** | media |
| **encontrado por** | construcción del ejecutor del fuzzer (nivel 2.1), midiendo la salida real de ambos lados |
| **afecta** | la comparación contra el JDK de referencia |

## Qué pasa

Un programa cuya excepción escapa produce salidas **asimétricas** en los dos lados:

```
$ java -cp . Fz1
Exception in thread "main" java.lang.ArithmeticException: / by zero
        at Fz1.run(Fz1.java:2)

$ run-headless Fz1.class run
Fz1.class run()I -> None
```

El `java` real nombra la clase y sale con código ≠ 0. Nuestra VM imprime `-> None` y sale con 0.

## Por qué

No es que la VM no sepa cuál fue la excepción — **sí lo sabe**. El reporte
`Exception in thread "main" ...` existe y se construyó en el hito A7 (`athrow.rs::report_uncaught`),
con la traza capturada y todo. El problema es que va al **buffer de consola** del VM
(`shared.console`), y `src/bin/run-headless.rs` solo imprime el `Option<Value>` que devuelve
`execute`; nunca vuelca ese buffer.

O sea: la información existe y se pierde en el último metro.

## Consecuencia para el fuzzer

Sin la clase, un `Outcome::Threw` de nuestro lado no se puede comparar contra uno del JDK real, y
todo programa que lance quedaría fuera del alcance del oráculo — que es justamente donde viven
muchos de los casos de esquina interesantes (división por cero, índices fuera de rango, casts).

## El rodeo (implementado en el generador, no acá)

Los programas generados son **totales**: se atrapan a sí mismos y codifican el resultado en el `int`
que devuelven.

```java
static int run() {
    try { return body(); }
    catch (ArithmeticException e)          { return MARK_ARITHMETIC; }
    catch (ArrayIndexOutOfBoundsException e){ return MARK_BOUNDS; }
    catch (Throwable t)                     { return MARK_OTHER; }
}
```

Así los dos lados devuelven un entero y la comparación vuelve a ser exacta. **Beneficio lateral**:
el oráculo nunca tiene que comparar *mensajes* de excepción, que difieren entre implementaciones
por razones perfectamente legales.

## Arreglo de fondo (no hecho)

Que `run-headless` vuelque el buffer de consola al terminar, y que salga con código ≠ 0 cuando el
hilo principal murió por una excepción. Es chico, pero toca un binario existente y el rodeo del
generador es suficiente para lo que el fuzzer necesita — así que queda anotado y no hecho.
