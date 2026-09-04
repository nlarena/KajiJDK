# FZ-010 — `System.out.println` panica: `PrintStream.writeString` no tiene nativo

**Estado:** abierto · **Encontrado:** 2026-08-30 · **Lo encontró:** el canal de lanzamientos de K7

## El caso

Una línea, corrida **desde la raíz del repositorio**:

```java
public class A { static int run() { System.out.println("hola"); return 7; } }
```

```
run-headless A.class run
thread 'main' panicked at src\jvm\interpreter\natives.rs:1205:14:
no native implementation for java/io/PrintStream.writeString(Ljava/lang/String;)V
```

Lo que falta es concreto y está nombrado por el propio mensaje: `PrintStream.writeString` está
declarado `native` en `KajiLibrary` y no tiene implementación del lado de Rust.

## Las dos mitades, que son distintas

1. **El nativo que falta.** Trabajo de biblioteca/VM.
2. **Que sea un `panic` y no un `UnsatisfiedLinkError`.** Esta es la mitad que importa para
   cualquiera que lo cruce: un nativo sin implementar es una condición que la JVMS **nombra** y que
   un programa puede observar y atrapar. Un panic se lleva puesto el proceso entero, así que del
   lado de una campaña no se distingue de que la VM se haya roto de verdad, y el `try`/`catch` del
   envoltorio total —que existe justamente para que ninguna excepción escape— no puede hacer nada.

## Lo que este hallazgo **no** es, y cómo casi lo escribo mal

La primera versión de esta nota decía otras dos cosas, las dos falsas:

- que `getstatic java/lang/System.out` no resolvía;
- que `"x" + n` lanzaba algo cuya clase la VM no podía resolver.

Las dos salieron de correr las sondas desde `/tmp` en vez de la raíz del repositorio. `run-headless`
bootea con **rutas relativas** (`KajiLibrary`, después `boot`), así que desde otro directorio la
imagen de arranque queda vacía y **cualquier** cosa que toque la biblioteca falla — `length()`,
`charAt`, `equals`, la concatenación, todo. Corridas desde la raíz, las tres funcionan.

Vale anotarlo porque el error es reproducible por cualquiera: el síntoma (`throw: cannot resolve the
thrown object's class`) apunta al mecanismo de reporte y no a la causa, y no dice ni una palabra
sobre el bootclasspath. Un `run-headless` que verificara que la imagen de arranque no está vacía
antes de ejecutar convertiría media hora de arqueología en una línea.

## Por qué el fuzzer no lo había visto

Porque **ningún programa generado imprime**. El envoltorio total devuelve un `int` y `run-headless`
lo reporta él mismo; la gramática no tiene ninguna forma que toque `System.out`. El canal de
lanzamientos de K7 fue lo primero que necesitó publicar algo desde adentro del programa.

Es la misma familia que FZ-009: **lo que la gramática no puede escribir, la campaña no puede
encontrar**, y ahí no vale que el reporte diga cero.

## Consecuencia para el canal de K7

El canal no se publica por consola. El contador vive en un campo estático y una sonda `kjthrew()` lo
devuelve por valor de retorno, así que no toca `java.io` en absoluto.
