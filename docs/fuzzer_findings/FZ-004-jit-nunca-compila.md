# FZ-004 — El brazo "JIT" de la campaña corría el intérprete el 88% de las veces

| campo | valor |
|---|---|
| **estado** | arreglado |
| **severidad** | alta (una campaña limpia no significaba nada) |
| **encontrado por** | el generador del fuzzer (nivel 2.3), midiendo `JitStats` sobre los programas generados en vez de suponer |
| **afecta** | la herramienta, no el sistema bajo prueba |

## Qué pasa

El emparejamiento `Path::Interpreter` vs `Path::Jit` es el que más importa: es el único donde una
discrepancia es inequívocamente un bug **de este proyecto**, sin juicios de valor sobre la
implementación de referencia. La primera versión del generador emitía programas cuyo `run()` llamaba
al cuerpo **una sola vez**.

`JitCache::THRESHOLD` es **32**. Un método se compila después de 32 invocaciones, o *on-stack* cuando
un bucle ya dio suficientes vueltas. Un programa que llama a su cuerpo una vez no cruza ninguno de
los dos umbrales — y entonces `JVM_JIT=0` y `JVM_JIT` sin setear **son el mismo motor**.

Medido, no supuesto (`fuzz::campaigns::jit_coverage`, 60 semillas):

```
de 60 programas generados: 7 compilaron algo, 7 entraron a código nativo, 6 entraron on-stack
```

**7 de 60.** El 88% de las semillas comparaba el intérprete contra sí mismo.

## Por qué importaba más de lo que parece

La campaña de 120 semillas reportaba `0 divergencias, 100% usable` y parecía una buena noticia. No lo
era: de esas 120 semillas, unas 14 tocaron el compilador. El resto era un test de que el intérprete
es determinista, que ya se sabía.

Es exactamente el mismo género que FZ-003: **una herramienta que parece estar probando algo y no lo
está**, y que no falla ruidosamente sino que devuelve verde. La diferencia con FZ-003 es que allá la
pista era un mensaje en español; acá no había ninguna pista, porque `run-headless` nunca imprime los
contadores del JIT. Sólo aparece si se va a buscar.

La suite propia del JIT ya tenía escrita la lección — `jit_tests.rs::differential` afirma
`on_stats.compiled > 0, "nothing was compiled, so nothing was tested"` — y el fuzzer la repitió por
no haberla mirado.

## Caso mínimo reproducible

Cualquier programa cuyo `run()` llame al cuerpo menos de 32 veces. El mínimo:

```java
public class FzMin {
    static int m0() { return 7 * 6; }
    static int run() { try { return m0(); } catch (Throwable t) { return 1526595599; } }
    public static void main(String[] a) { System.out.println(run()); }
}
```

Con `JitStats` a la vista: `compiled: 0, native_calls: 0`. Los dos caminos de la campaña devuelven
`42` y coinciden, pero ninguno de los dos ejecutó código nativo.

## Arreglo

El `run()` que emite el generador envuelve la llamada en un **bucle de calentamiento**
(`GenConfig::warmup`, 40 por defecto, con margen sobre el umbral de 32):

```java
static int run() {
    try {
        int acc = 0;
        for (int w = 0; w < 40; w++) { acc = ((acc * 31) + m3()); }
        return acc;
    }
    catch (ArithmeticException e) { return 1526595585; }
    ...
}
```

Dos decisiones dentro del arreglo:

- **Repetir la llamada es seguro** porque un método generado es **puro**: no hay campos, ni arrays,
  ni estáticos, ni nada que se acarree entre llamadas. Las 40 iteraciones calculan lo mismo y el
  significado del programa no cambia.
- **Se acumula (`acc * 31 + …`) en vez de quedarse con el último resultado** para que una respuesta
  equivocada en *cualquier* iteración llegue al valor de retorno: con deoptimización de por medio,
  la iteración que corre nativa y la que corre última no tienen por qué ser la misma.

El bucle no queda fijo en el caso reducido: el reductor tiene una pasada `ShrinkWarmup` que lo baja
hacia 1 como cualquier otra constante. Si el bug sólo aparece en código compilado, el predicado
rechaza cada recorte y el caso mínimo **conserva su bucle** — que es la forma en que un hallazgo dice
"esto necesita el JIT".

## Medición después del arreglo

```
de 60 programas generados: 45 compilaron algo, 45 entraron a código nativo, 17 entraron on-stack
```

De 7/60 (12%) a 45/60 (75%). Los 15 restantes son programas que el JIT **rechaza** por diseño
(`Ineligible`), que es información legítima y no una falla del generador.

Y la campaña que antes no probaba nada ahora sí prueba: 400 semillas en proceso, **316 entraron a
código nativo**, cero desacuerdos entre el intérprete y el JIT. Eso es un resultado; lo de antes era
un placebo.

El test que lo mide queda en la suite como guardia: falla si menos de una cuarta parte de los
programas generados entra a código nativo, de modo que la próxima vez que el generador cambie de
forma y deje de calentar, se entere alguien.

```
cargo test --release --lib fuzz::campaigns::jit_coverage -- --ignored --nocapture
```
