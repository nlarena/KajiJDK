# El "escalón": `java.lang.fushite.Throwable` (excepciones sin tocar el JDK)

> Nota de diseño (A4). Decisión sobre cómo modelar las excepciones **ahora**, sin
> arrastrar el `java.lang` real ni necesitar nuestro compilador, y cuál es el camino
> hacia un `java.lang.Throwable` "de verdad".

## El problema

Para tener excepciones necesitamos una clase `Throwable` (objetos que se lanzan y
atrapan). Pero hay tres tensiones:

1. **`javac` tiene hardcodeado** que los tipos de `throw`/`catch` deben ser-un
   `java.lang.Throwable` (esa clase, por su nombre binario exacto). No podemos
   inventar un `Throwable` en el default package: javac lo rechaza.
2. **Traer el `java.lang.Throwable` real del JDK** a nuestra JVM arrastraría un grafo
   enorme de dependencias (`Object`, `String`, `System`, `StackTraceElement`, …) y
   métodos **nativos** que no podemos ejecutar.
3. **Reemplazar `java.lang.*` por las nuestras** requiere tooling (`--patch-module`)
   y un bootstrap loader — trabajo que pertenece al último pilar de A4.

## La decisión (el escalón)

Definimos **nuestra propia raíz de excepciones en nuestro paquete**:

```java
package java.lang.fushite;

public class Throwable extends java.lang.Throwable {   // ← peaje de javac
    public int code;
    public Throwable() { this.code = 0; }
}
```

- El `extends java.lang.Throwable` es **solo el peaje de javac** (para que sea-un
  throwable y acepte `throw`/`catch`).
- En **nuestra** JVM, esa raíz real **nunca se carga**: su `<init>` **no-opea**,
  igual que `Object.<init>` hoy (ver el manejo de clases no resolubles en
  `invokespecial` y en `instance_field_count`).
- **Todo nuestro árbol de excepciones cuelga de `java.lang.fushite.Throwable`**, que
  es 100% cargable y caminable por `is_subtype` (en `class_operations`).

### Por qué funciona el `catch`

`is_subtype(sub, target)` matchea **por nombre** y solo *carga* las clases que tienen
super que recorrer. Para `catch (java.lang.fushite.Throwable e)` atrapando un `Boom`:
`is_subtype(Boom, fushite.Throwable)` sube `Boom → fushite.Throwable` (ambas
cargables) → match. No necesita cargar nada del JDK.

### Limitaciones honestas

- No podemos **instanciar** ni **llamar métodos** de la `java.lang.Throwable` real
  (no está cargada): sin `getMessage()`, sin stack trace, sin `fillInStackTrace`.
- Por eso el mensaje es un `int code` y no un `String` (un `String` necesitaría `ldc`
  + `java.lang.String`, que el intérprete aún no modela).
- Atrapar por tipos que requieran *cargar y recorrer* clases del JDK por encima de
  `fushite.Throwable` no está soportado (más allá del match por nombre).

## ¿Necesitamos nuestro compilador? **No.**

El compilador propio (**Fase B**) **no está en el camino crítico**:

- El compilador hace `.java → .class`. Nuestro `Throwable.class` lo genera **cualquier
  javac** — el del sistema alcanza.
- Nuestra JVM **consume** `.class`; no le importa quién los generó.
- **No hay circularidad**: javac del sistema compila *nuestro* `.java` → `.class` →
  nuestra JVM lo carga.
- Así arranca el **OpenJDK real**: su biblioteca (`java.lang.*`) se compila con un
  javac **preexistente**. Tener tu propia `java.lang` no exige tener tu propio javac;
  eso recién importa para ser **self-hosting** (Fase B+), que es aspiración, no
  requisito del runtime.

## El camino al `Throwable` "de verdad" (pilar de class loaders, A4)

Cuando ataquemos el **bootstrap class loader**, "promovemos" el escalón:

1. Escribir `java/lang/Object.java`, `java/lang/Throwable.java` (+ `Exception`,
   `RuntimeException`) **nuestros**, minimal, sin nativos.
2. Compilarlos con el **javac del sistema** a un dir de *bootstrap*.
3. Compilar el código de usuario con
   `javac --patch-module java.base=<bootdir> …` → linkea contra los **nuestros** en
   vez de los del JDK.
4. En la JVM: un *bootstrap loader* (en la práctica, ese dir primero en el classpath)
   provee `java.lang.*`. Ahí `new`/`invokespecial`/`is_subtype` cargan **nuestra**
   `java.lang.Throwable` en lugar de no-opear.
5. Se cae el hack del no-op para supers no resolubles (o queda como *fallback*), y de
   paso se arregla `Object.<init>`.

`java.lang.fushite.Throwable` es entonces el **mismo código** que la futura
`java.lang.Throwable` — cambia el nombre del paquete y el tooling de compilación.

## Estado

- ✅ `java.lang.fushite.Throwable` implementada y **cargada/ejecutada** por la JVM
  (compila a `java/java/lang/fushite/Throwable.class`; el classpath es `java/`).
- ⏭️ Pendiente: subclase concreta (`Boom`), `athrow` + tabla de excepciones +
  *unwinding*. Y, más adelante, el bootstrap loader (promoción a `java.lang.*`).
