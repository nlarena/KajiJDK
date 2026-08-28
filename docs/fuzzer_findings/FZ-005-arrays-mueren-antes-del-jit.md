# FZ-005 — Los programas con arrays morían antes de que el JIT los mirara

| campo | valor |
|---|---|
| **estado** | arreglado |
| **severidad** | alta (la mitad de la campaña "Interpreter vs Jit" volvió a comparar el intérprete contra sí mismo) |
| **encontrado por** | la etapa de arrays del generador (nivel 2.3), midiendo `JitStats` por configuración en vez de suponer que la cobertura se mantenía |
| **afecta** | la herramienta, no el sistema bajo prueba |

## Qué pasa

Al entrar arrays en la gramática, la cobertura del JIT se **partió al medio**. Medido con
`fuzz::campaigns::jit_coverage::what_each_grammar_setting_costs_in_jit_coverage`, 80 semillas por
configuración:

```
scalars, integral only : 73/80 entraron a código nativo
scalars, with floats   : 66/80
arrays, no floats      : 44/80
everything (el default): 40/80
```

De 91% a 50%. La primera sospecha —que el JIT rechaza los arrays— es **falsa**, y el mismo
contador lo desmiente: con arrays hay *menos* métodos rechazados (57) que sin ellos (117). Si el
compilador estuviera rechazando arrays, ese número tendría que subir, no bajar.

Lo que había bajado era otra cosa: la cantidad de métodos que el JIT llega a **mirar**.

## Por qué

La columna que lo resuelve es la que hubo que agregar para verlo — cuántos programas devuelven un
marcador de excepción en vez de un valor:

| configuración | entraron a nativo | murieron en un marcador |
|---|---|---|
| scalars, integral only | 73/80 | 8 |
| scalars, with floats | 66/80 | 11 |
| arrays, no floats | 44/80 | 36 |
| everything (el default) | 40/80 | 37 |

`73 + 8 = 81`. `44 + 36 = 80`. `40 + 37 = 77`. **Casi exactamente todo programa que no entra a
código nativo es un programa que lanzó una excepción.**

La cadena es corta y es la misma de FZ-004 con otro disfraz:

1. `JitCache::THRESHOLD` es **32**: un método se compila recién después de 32 invocaciones;
2. el `run()` generado llama al cuerpo 40 veces dentro de un `try` (el bucle de calentamiento que
   FZ-004 puso ahí justamente para cruzar ese umbral);
3. una excepción **sale del bucle entero**, no de una iteración. Si el cuerpo lanza en la
   iteración 1, el método se invocó **una** vez;
4. una vez ≪ 32, así que el JIT nunca escanea el método. No lo rechaza: nunca lo ve.

Y la gramática de arrays lanzaba en la iteración 1 constantemente, por un error de diseño concreto:
**`Gen::index_expr` no sabía a qué array estaba indexando.** Generaba un índice en
`0..max_array_len` (0..6) contra un array cuya longitud era 1..6, elegida aparte. Un índice
"chico" contra un array de longitud 2 se sale de rango más o menos la mitad de las veces. Sumado a
que 1 de cada 10 longitudes era negativa o cero, el 46% de las semillas moría antes de calcular
nada.

## Consecuencia

La campaña `Interpreter` vs `Jit` es la única donde una discrepancia es inequívocamente un bug de
**este** proyecto. Con el 50% de las semillas sin tocar el compilador, la mitad de esa campaña
comparaba el intérprete contra el intérprete y devolvía verde.

Peor: el verde era *más* convincente que en FZ-004, porque los programas sí lanzaban excepciones y
los dos motores sí coincidían en cuál. La campaña reportaba acuerdo sobre `marks::BOUNDS` una y
otra vez — un acuerdo real, sobre el intérprete, dos veces.

Y hay una ironía que vale anotar: lo que se pierde es exactamente lo que los arrays venían a
probar. El `iaload` compilado **no lanza**, *deoptimiza* — emite una guarda de cota que sale del
código nativo y le devuelve el pc al intérprete, que recién ahí levanta la excepción. Ese cruce de
frontera es la mitad del valor de tener arrays, y sólo se puede probar **desde adentro de código
compilado**. Un índice fuera de rango en la iteración 1 no lo prueba nunca.

## Caso mínimo reproducible

Cualquier programa que lance en la primera iteración del calentamiento. El mínimo:

```java
public class FzMin {
    static int m0() {
        int[] a0 = new int[2];
        return a0[5];
    }
    static int run() {
        try {
            int acc = 0;
            for (int w = 0; w < 40; w++) { acc = ((acc * 31) + m0()); }
            return acc;
        }
        catch (ArrayIndexOutOfBoundsException e) { return 1526595586; }
        catch (Throwable t) { return 1526595599; }
    }
    public static void main(String[] a) { System.out.println(run()); }
}
```

Con `JitStats` a la vista: `compiled: 0, native_calls: 0, rejected: 0`. Los tres en cero es la
firma del hallazgo — `rejected: 0` es lo que distingue "el JIT lo rechazó" de "el JIT nunca lo
vio". Los dos caminos devuelven `1526595586` y coinciden, y ninguno de los dos ejecutó código
nativo.

Cambiar el `5` por un `1` alcanza para que el mismo programa dé `compiled > 0`.

## Arreglo

`Local` ahora lleva la longitud declarada del array (`array_len`), y `Gen::index_expr` la recibe:

- un índice en rango se genera contra la longitud **de ese array**, no contra una cota global;
- los índices fuera de rango siguen existiendo, pero salen de las dos ramas que no pueden estar en
  rango por construcción (una variable `int` del scope, o una expresión entera cualquiera), no de
  la rama que *creía* estar en rango y no lo estaba;
- las longitudes degeneradas —cero y negativa— pasaron de 1 en 10 a 1 en 25 cada una.

No se sacaron los casos de excepción, y sacarlos habría sido el arreglo equivocado: son la mitad
del valor de la gramática. Lo que cambió es la **tasa**. Una de cada veinticinco semillas sigue
siendo cuarenta semillas en una campaña de mil, de sobra para fijar que los dos motores lanzan lo
mismo — y una de cada diez alcanzaba para partir el brazo del JIT.

## Medición después del arreglo

```
                          entraron a nativo    murieron en un marcador
scalars, integral only :       73/80                    8
scalars, with floats   :       66/80                   11
arrays of int only     :       63/80   (antes 42)      14   (antes 35)
no narrowing conversion:       66/80   (antes 47)      13   (antes 33)
everything (el default):       56/80   (antes 40)      20   (antes 37)
```

Del 50% al 70% en el default, y del 53% al 79% con arrays de `int`. Los que quedan afuera son
mayoritariamente métodos que el JIT **sí** rechaza y por razones documentadas: `laload`/`daload` y
sus gemelos de escritura no están en el subconjunto, y `f2i`/`d2i`/`f2l`/`d2l` tampoco
(`GenConfig::wide_array_elements` y `GenConfig::fp_narrowing` existen para poder medir cada uno por
separado). Eso es información legítima, no una falla del generador.

El contador de `deopts` también volvió: 43 en la fila de arrays de `int`, contra 0 antes. Un deopt
es, por definición, código nativo que se ejecutó y salió — la prueba directa de que la guarda de
cota se está ejercitando desde adentro del JIT, que era lo que el hallazgo hacía imposible.

## La lección, que ya estaba escrita

Es el tercer hallazgo del mismo género (FZ-003, FZ-004, este): **una herramienta que parece estar
probando algo y no lo está, y que no falla ruidosamente sino que devuelve verde.** La única defensa
que ha funcionado las tres veces es la misma: medir la cobertura por construcción nueva en vez de
suponer que se mantiene.

Por eso el arreglo incluye la columna de "murieron en un marcador" en la medición permanente. La
cobertura sola dice *que* bajó; las dos juntas dicen *por qué*, y distinguen las dos causas que
tienen arreglos opuestos:

| síntoma | causa | qué hacer |
|---|---|---|
| `rejected` sube, `entered` baja | el JIT rechaza la construcción | es información: anotarla y seguir |
| `rejected` baja, `died on a marker` sube | los programas mueren antes del umbral | es un bug del generador: bajar la tasa |

```
cargo test --release --lib fuzz::campaigns::jit_coverage::what_each -- --ignored --nocapture
```
