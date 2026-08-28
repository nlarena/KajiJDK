# FZ-003 — El `javac` del `PATH` puede ser el nuestro bajo `cargo test`

| campo | valor |
|---|---|
| **estado** | arreglado |
| **severidad** | media (habria invalidado campañas enteras en silencio) |
| **encontrado por** | los tests de integracion del ejecutor, que fallaban con un error en español |
| **afecta** | la herramienta, no el sistema bajo prueba |

## Qué pasa

Los tests de integracion del ejecutor fallaban asi:

```
CompileError("javac: no se pudo leer -d: The system cannot find the file specified. (os error 2)")
```

El mensaje **en español**, quejandose de no poder leer un archivo llamado `-d`, delata al culpable:
el `javac` que se ejecuto no era el del JDK sino **el nuestro** (`src/bin/javac.rs`), que no entiende
la opcion `-d` de la misma forma y trata el argumento siguiente como un archivo.

## Por qué

`cargo test` agrega el directorio de build al `PATH` del proceso hijo, y **este repo compila un
binario llamado `javac`**. Desde una shell normal `which javac` da el del JDK; desde adentro de
`cargo test`, gana el nuestro.

## Por qué importaba más de lo que parece

Una campaña que confiara en el `PATH` estaria compilando sus programas **con el compilador bajo
prueba** — exactamente la herramienta cuyos bugs se supone que el fuzzer contrasta contra una
referencia. Y no habria fallado ruidosamente: habria producido class files distintos y reportado
divergencias falsas, o peor, habria ocultado divergencias reales.

Es el mismo genero de error que el resto del proyecto ya conoce: **confiar en una resolucion
implicita cuando hay dos cosas con el mismo nombre**.

## Arreglo

`Toolchain::detect()` nunca resuelve `javac` por `PATH`:

1. `JAVA_HOME` si esta seteada;
2. si no, busca `java` en el `PATH` — un nombre que este repo **no** compila — **salteando cualquier
   directorio dentro de un `target/`**, y toma el `javac` de al lado.

El razonamiento quedo escrito en el doc de `Toolchain::detect`, porque es de los que se vuelven a
perder si no se explican.
