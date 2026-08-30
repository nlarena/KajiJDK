# FZ-009 — La sonda de identidad de `String` la borraba el compilador antes de llegar a la VM

| campo | valor |
|---|---|
| **estado** | arreglado (2026-08-29) |
| **severidad** | alta — tapó [FZ-008](FZ-008-literales-sin-internar.md), un bug de conformidad vivo |
| **encontrado por** | comprobar a mano una afirmación del roadmap que resultó falsa |
| **afecta** | la etapa de strings del generador |

## Qué pasaba

`StrProbe::Same` es, según su propia documentación, «la sonda que justifica la etapa»: pregunta si
esta VM internea los literales, que es la propiedad que estuvo en la lista de divergencias conocidas
del oráculo. Se emitía en línea:

```java
v5 = (("" == "") ? 1 : 0);
```

`javac` **pliega eso en tiempo de compilación**. El desensamblado de un caso equivalente no deja
lugar a dudas — no hay ni un `ldc`:

```
static int run();
     0: iconst_0
     1: istore_0
     2: iinc          0, 1
     5: iinc          0, 2
     ...
```

O sea que la sonda medía **el plegador de constantes de `javac`**, no la VM. Y como los dos lados del
pareo compilan con el mismo `javac`, los dos recibían la misma constante y coincidían siempre.

## El costo, que es lo que le da la severidad

La campaña `strings_agree_with_the_reference_jdk` reportó **80 semillas, 100% usables, 0
divergencias** mientras `"a" == "a"` daba `false` en esta VM y `true` en un JDK real. Peor: la
entrada correspondiente de la lista de divergencias conocidas del oráculo se había **quitado**
afirmando que el interning ya estaba implementado, y el silencio de la campaña era la evidencia que
respaldaba esa afirmación. **La herramienta confirmó una creencia falsa en vez de contradecirla.**

Y los literales iguales sí se generaban: 11 comparaciones `"x" == "x"` con ambos lados idénticos
entre 80 programas. La forma estaba; lo que no llegaba era a la VM.

## Por qué es de la familia FZ-003/004/005/007

La misma figura, otra vez: **una medición que parece estar probando algo y no lo está**, con
resultado verde. La variante nueva es de dónde viene la pérdida — acá no la borra el fuzzer ni el
entorno, la borra **el compilador que está en el medio**, que es una pieza que el diseño trataba como
transparente.

De ahí sale la regla generalizable: **cuando los dos lados de un pareo comparten el compilador, todo
lo que el compilador resuelva es invisible para el oráculo.** Vale para el plegado de constantes,
para el DCE y para cualquier cosa que `javac` decida antes del class file.

## El arreglo

La comparación pasa por parámetros de un método, que es lo que el plegador no puede atravesar:

```java
static int ssame(String p, String q) { return (p == q) ? 1 : 0; }
```

Se emite a demanda —como los clasificadores `fcls`/`dcls`—, y no se ofrece dentro de un cuerpo de
worker, porque ahí se llamaría sin calificar desde otra clase (`Scope::foreign`).

**Verificado en las dos direcciones**, que es lo único que separa esto de volver a creer:

| | divergencias en 40 semillas |
|---|---|
| sonda en línea (como estaba) | **0** |
| sonda por `ssame` | **2** |

Y la segunda son [FZ-008](FZ-008-literales-sin-internar.md), reducido por la herramienta a
`ssame("", "")`.

## Lo que queda anotado

`StrProbe::Identity` (el `equals`) y `StrProbe::Length` **no** corren este riesgo: una llamada a
método no es una expresión constante para la JLS §15.28, así que `javac` no las pliega. La que
estaba expuesta era exactamente la única de las tres que usa un operador.
