# FZ-012 — El colector mueve el objeto y el `publish` de un constructor de `String` se pierde

**Estado:** arreglado · **Encontrado:** 2026-09-02 · **Lo encontró:** la corrida larga de K7, semilla 725

## Cómo se veía

```
divergence on seed 725: Returned(-1856459008) vs Returned(-309080448)
  interpreter -> Returned(-1856459008)
  jit         -> Returned(-309080448)
```

Un programa determinista por construcción —los workers escriben en slots disjuntos y todos los
`join` van antes de cualquier lectura— contestando dos cosas distintas según el motor. Se anotó como
«divergencia intérprete contra JIT», que es lo que el pareo podía decir, y **no era eso**.

## Quién tenía razón

El pareo dice que difieren, no quién miente. El desempate es un tercero que no es nuestro:

| | |
|---|---|
| JDK de referencia | `-1856459008` |
| intérprete | `-1856459008` ✔ 10/10 |
| **JIT** | **`-309080448`** ✘ 10/10 |
| os-gil | variaba entre **cuatro** valores en 10 corridas |
| os-parallel | correcto, o **panic** en `heap.rs:932`, 4 de 10 |

O sea: tres síntomas distintos —una divergencia determinista, un no-determinismo donde el programa no
tiene ninguno, y un panic intermitente— sobre el mismo programa.

## El camino hasta la causa

El reductor lo dejó en un programa que sigue teniendo tres hilos y una jerarquía de objetos, y ahí
empezó lo raro: **sacarle cualquier cosa lo hacía desaparecer**. Incluso un local muerto
(`byte b13 = (byte) 0;`). Eso ya no es un bug de traducción de opcodes —esos no dependen de un local
que nadie lee—, es algo sensible al *momento*.

El umbral lo confirmó. Con `JVM_JIT_THRESHOLD`:

| umbral | 1 | 4 | 8 | 16 | 32 | 64 | 1000 |
|---|---|---|---|---|---|---|---|
| respuesta | ✔ | ✔ | ✔ | ✘ | ✘ | ✔ | ✔ |

Mal en una ventana angosta y bien a los dos lados, incluso compilando **más**. Y `JVM_JIT_REGS=0`
—sin asignación de registros— seguía dando mal, así que tampoco era el emisor.

Para poder preguntar *qué* se compila hicieron falta dos perillas que no existían: `JVM_JIT_LOG`,
que lista lo compilado y lo rechazado en orden, y `JVM_JIT_DENY=<subcadena>`, que deja un método
interpretado a la fuerza. Sin la segunda no hay forma de aislar un método sospechoso: tocar el
programa Java cambia el conjunto entero y no prueba nada.

El log dijo que compilaban **dos** métodos: `Fz725.ssame` y `java/lang/Object.<init>()V`. Y:

```
JVM_JIT_DENY=Fz725.ssame        -> sigue mal
JVM_JIT_DENY=java/lang/Object.  -> bien
```

`java.lang.Object.<init>()V` es el método vacío que llama el `super()` de **todo** constructor. Que
compilarlo cambie el resultado de un programa no puede ser un bug de generación de código: no hay
código que generar. Lo único que cambia es *cuándo* pasan las cosas.

Bisecando el valor equivocado se llegó a que el worker con `k = 1` escribía `1` en vez de `0`, y su
cuerpo es `new String("ab").equals("")`. Tres sondas fijaron qué le pasaba a ese String:

- `new String("ab").length()` → **0** (debería ser 2),
- `new String("ab").charAt(0)` → lanza,
- `new String("ab") == ""` → **false**.

O sea: no es que aliasee al literal, es que **es un String vacío distinto**. Que es exactamente lo
que se obtiene si el constructor no llega a entregar lo que construyó: el objeto que `new` alocó
tiene lugar para cero caracteres.

## La causa

Un constructor de `java.lang.String` no puede contestar llenando `this`: los caracteres van inline y
el tamaño se fija al alocar, y `String` no declara campos. Así que construye un String **aparte** y
lo publica; el `return` reescribe las referencias del llamador del objeto entregado al construido.
El par vive en el frame:

```rust
published: Option<(usize, usize)>,   // (el que le entregaron, el que construyó)
```

Instrumentando `publish` y el `return`, en la corrida con JIT aparece esto:

```
[str] publish handed=112 built=208 | llamador Fz725W0.run pila=[112]
[str] PERDIDO handed=112 -> built=208 en Fz725W0.run: pila=[3866] locales=[3566]
```

Entre las dos líneas el objeto **se movió de 112 a 3866** — una promoción del colector. El colector
arregla los frames con `Frame::remap_references`, que visita la pila de operandos, los locales y el
monitor… **y no `published`**. Así que el par queda con el offset viejo, y el `remap_references` que
hace `return_void` busca en el llamador un objeto que ya no tiene nadie.

No falla, no lanza: **no encuentra nada**. El llamador se queda con el objeto sin inicializar y el
programa lee `""` donde escribió `new String("ab")`.

El JIT no tiene nada que ver con el defecto. Sólo corre la película a otra velocidad: compilar
`Object.<init>` —el callee de todo constructor— corre la línea de tiempo de alocación de todo el
programa, y la colección cae adentro de esa ventana. Por eso cualquier perturbación lo escondía, por
eso el umbral lo prendía y lo apagaba, y por eso pasa lo mismo con el intérprete solo si la alocación
cae donde tiene que caer.

## El arreglo

`Frame::remap_references` mueve también el par publicado. Va ahí y no sólo en el camino del colector
porque ese método es el que promete visitar *todas* las referencias del frame: **dejar un campo
afuera es la forma exacta en que el bug entró**.

```rust
if let Some((handed, built)) = self.published {
    self.published = Some((remap(handed), remap(built)));
}
```

## Lo que se arregló con eso

| | antes | después |
|---|---|---|
| semilla 725, jit | `-309080448` | `-1856459008` ✔ |
| semilla 725, os-gil | 4 valores distintos en 10 | 12/12 igual ✔ |
| semilla 725, os-parallel | panic 4 de 10 | **2 de 24 todavía mal** (una respuesta distinta y un panic) |

Los dos primeros síntomas eran el mismo defecto y están cerrados. **El tercero no**: `os-parallel`
mejoró mucho —de 4 panics en 10 a 2 corridas malas en 24, una con otra respuesta y otra con
panic— pero sigue fallando. O sea que había *dos* cosas encima de este programa, y sacar el
`publish` perdido dejó a la vista la que ya conocíamos.

Esto importa más allá de acá: el ítem del **write barrier** del JIT está parado porque el
`DANGLING` intermitente se le atribuye al heisenbug de `os-parallel` *precisamente porque* el
código compilado no puede escribir un puntero. Por un rato pareció que este arreglo podía
destrabarlo. **No lo destraba**: medido sobre 24 corridas, el sustrato sigue contestando mal.

## Cómo se prueba que el arreglo hace falta

Sacándolo. `Frame::remap_references` sin las tres líneas devuelve el bug exacto —`q_len` da 62 en vez
de 64, el reducido da `1875315199` en vez de `1875315168`— y el test permanente
(`el_par_publicado_se_mueve_con_el_colector`, en `frame.rs`) falla diciendo por qué.

El test es de frames y no de código generado a propósito: la propiedad que faltaba es de acá, y un
test end-to-end dependería de que la colección caiga en la ventana, que es justamente lo frágil.

## Lo que dejó de regalo

- `JVM_JIT_LOG=1` — qué compila y qué rechaza el JIT, en orden, con la razón.
- `JVM_JIT_DENY=<subcadena>` — dejar un método interpretado a la fuerza, sin tocar el programa.

Las dos nacieron acá porque sin ellas la pregunta «¿qué método, al no compilarse, hace desaparecer el
bug?» no se puede hacer.
