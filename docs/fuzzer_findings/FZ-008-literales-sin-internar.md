# FZ-008 — Los literales de `String` no se internan (`"a" == "a"` daba `false`)

| campo | valor |
|---|---|
| **estado** | arreglado (2026-08-29) |
| **severidad** | alta — era una no-conformidad con la JLS, no una diferencia de implementación |
| **encontrado por** | la campaña de strings contra el JDK de referencia |
| **afectaba** | los tres substratos, con y sin JIT |

## El caso mínimo

Tal como lo dejó el reductor, sin retocar:

```java
static int ssame(String p, String q) { return (p == q) ? 1 : 0; }
static int m0() { return ssame("", ""); }
```

Daba **0** acá y **1** en un `java` real.

## Qué dice la especificación

**JLS §3.10.5.** Un literal de `String` es una referencia a una instancia **interneada**: dos
literales con el mismo contenido, en la misma clase o en clases distintas, son *la misma*
referencia. Y la otra mitad, que importa igual: lo que el programa **calcula** —una concatenación en
runtime, un `new String(...)`, `String.valueOf`— es un objeto **distinto**.

Las dos mitades son la misma regla. Un pool que se tragara los strings calculados haría que
`new String("a") == "a"` diera `true`, que está tan mal como lo que estábamos arreglando.

## El arreglo

**El pool vive en el metaspace**, no en el heap: el heap es una arena de bytes sin noción de qué es
un String, y el colector ya llega al metaspace para recorrer los mirrors — así que el pool es raíz y
bloque pinneado **por la misma vía** que ya existía, en vez de inventar una tercera.

Tres propiedades, y ninguna es opcional:

1. **Una instancia por literal**, consultada por unidades UTF-16 en cada `ldc`.
2. **Raíz de GC.** Entre dos `ldc` del mismo literal no lo referencia nada más — eso *es* un pool —,
   así que sin la raíz la primera colección lo libera y el siguiente `ldc` devuelve un offset muerto.
3. **Pinneado en `gc::compact`**, y por una razón que los mirrors no comparten. El paso 3(b) de
   `compact` reescribe todas las referencias, así que un objeto movido cualquiera queda bien; pero
   el contrato de un literal es su **identidad**, y el pool está indexado por **contenido**. Movelo y
   el mapa sigue nombrando la dirección vieja: el siguiente `ldc` devuelve un puntero a lo que ahora
   ocupe ese lugar.

**Alocado en Old**, además: un literal sobrevive a toda colección por definición, así que en la
generación joven se copiaría en cada minor para siempre — y hay que pinnearlo igual, cosa que la
generación joven no contempla.

Y la separación que hace cumplir la otra mitad: `strings::intern` (pooleado, para literales) frente a
`strings::allocate` (fresco, para todo lo demás). Hay exactamente **tres** llamadores del primero
—`ldc` de un `CONSTANT_String`, el `ConstantValue` de un campo estático `String` (JVMS §5.4.2) y un
argumento estático `String` de un bootstrap— porque esos son los tres lugares donde una entrada del
constant pool se vuelve un objeto.

Una pieza más, en la biblioteca: `String(String original)` publicaba **el original**, así que
`new String(s)` heredaba la identidad de `s`. Ahora publica una copia, que es lo que hace un `new`.

## Verificación

Sonda de las cinco propiedades, contra el JDK real:

| camino | resultado |
|---|---|
| `java` de referencia | **31** |
| green / os-gil / os-parallel | **31** |
| con `JVM_JIT=0` | **31** |
| en `os-parallel` con GC automático, promoción agresiva y `JVM_GC_VERIFY=1` | **31** |

Campañas: strings contra el JDK real **80 semillas, 0 divergencias** (antes 2 en 40), y el barrido
completo **200 semillas × 4 pareos, 100% usables, 0 divergencias**. Suites: 286 jvm, 98 fuzz, 256
burst.

## Los sabotajes, y el que no falló

Tres roturas deliberadas, para comprobar que los tests detectan cada propiedad por separado:

| sabotaje | resultado |
|---|---|
| sacar el pool de las raíces | **2 tests fallan** |
| que `intern` no consulte el pool | **1 test falla** |
| sacar el pool del conjunto `pinned` | **pasó — el test no lo detectaba** |

El tercero es el que valió la pena. El test de pinneado alocaba el hueco *antes* del primer literal,
y como `intern` carga `java/lang/String` —cuyo mirror también está pinneado— el mirror quedaba
delante del hueco y lo tapaba: nada se movía, con pin o sin pin, y el test pasaba por la razón
equivocada. Reordenado para que el literal quede **detrás** del hueco, el sabotaje lo falla.

## Cómo estuvo escondido tanto tiempo

Esta diferencia estuvo **listada como divergencia legítima** del oráculo; después se la quitó
*afirmando que el interning había aterrizado*, y no había aterrizado. La campaña que existía para
vigilarlo reportaba 0 divergencias — porque la sonda emitía `("a" == "a")` en línea y `javac` lo
pliega antes de que la VM vea un solo `ldc`. Eso es
[FZ-009](FZ-009-la-sonda-que-el-compilador-borraba.md), y es lo que hace que este hallazgo tenga dos
fichas en vez de una.

## Lo que destraba

Los **46 métodos** que `burst::compile` rechazaba por `ldc` de `String`. La razón que daba el
rechazo —«no hay tabla de interning, así que no hay offset permanente que hornear»— dejó de ser
cierta: un literal es ahora un objeto permanente en una dirección estable. Falta el lado del
compilador, que es otra frase.
