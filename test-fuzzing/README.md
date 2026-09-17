# test-fuzzing

Casos reales encontrados por fuzzing diferencial: **KajiLibrary** contra **OpenJDK 25**. Hay dos
pruebas, una sobre `java.math.BigInteger` y otra sobre `java.lang.String`.

## Qué se compara

| Lado | Qué es |
|---|---|
| Referencia | OpenJDK 25.0.4.1, distribución Eclipse Temurin |
| Bajo prueba | KajiJDK (`target/release/run-headless.exe`) con `KajiLibrary` |

Los dos lados corren **el mismo `.class`**, compilado una sola vez con el `javac` de OpenJDK.

OpenJDK no se instaló: se descargó y descomprimió fuera del repo. Los comandos de abajo usan la
variable `OPENJDK` para esa carpeta.

## Qué hay en esta carpeta

| Ruta | Contenido |
|---|---|
| `caso/ShiftRightNegativo.java` | El primer caso encontrado, con su `.class` compilado por OpenJDK |
| `salidas/openjdk-25.txt` | Lo que imprime OpenJDK para ese caso |
| `salidas/kajijdk-kajilibrary.txt` | Lo que imprime KajiJDK con KajiLibrary |
| `salidas/diferencias.txt` | El `diff` entre las dos salidas |
| `referencia/BigInteger.java` | La implementación de OpenJDK, sacada de su `lib/src.zip` |
| `kajilibrary/BigInteger.java` | Copia de `KajiLibrary/java/math/BigInteger.java` |
| `fuzzer/bigfuzz.py` | La campaña de fuzzing sobre `BigInteger` |
| `fuzzer/strfuzz.py` | La campaña de fuzzing sobre `String` |
| `datos/` | Los datos de cada campaña, en JSON, y su registro |
| `presentar.py` | Genera los informes a partir de `datos/` |
| `informe.html` | Los resultados de la prueba de `BigInteger` |
| `informe-strings.html` | Los resultados de la prueba de `String` |

`referencia/BigInteger.java` es código de OpenJDK, con licencia **GPL versión 2 y la excepción
Classpath**. Se puede commitear siempre que conserve intacto su encabezado de copyright y licencia.

## El primer caso, a mano

| Operación | OpenJDK 25 | KajiJDK + KajiLibrary |
|---|---|---|
| `-2 >> 7` | -1 | **0** |
| `-129 >> 7` | -2 | **-1** |
| `-1 >> 1` | -1 | **0** |
| `-5 << -1` | -3 | **-2** |
| control: `129 >> 7` | 1 | 1 |
| control: `-128 >> 7` | -1 | -1 |

Los dos controles coinciden: un positivo, y un negativo cuya división es exacta. Los cuatro negativos
que pierden bits al desplazarse dan distinto.

La especificación de `shiftRight` dice que el resultado es `floor(this * 2^-n)`: redondea hacia menos
infinito, igual que el operador `>>` sobre un `int`.

- **OpenJDK**, `shiftRightImpl` en `referencia/BigInteger.java`: desplaza la magnitud y, si el número
  es negativo y se perdió algún bit en 1, le suma uno a la magnitud. Así redondea hacia menos infinito.
- **KajiLibrary**, `shiftRight` en `kajilibrary/BigInteger.java` (líneas 522 a 527): desplaza la
  magnitud y conserva el signo, sin esa corrección. Para los negativos trunca hacia cero.

`shiftLeft` con una distancia negativa delega en `shiftRight`, por eso `-5 << -1` falla igual.

## Cómo reproducirlo

Desde la raíz del repo, con `OPENJDK` apuntando a la carpeta del JDK:

```bash
"$OPENJDK/bin/javac" -d test-fuzzing/caso test-fuzzing/caso/ShiftRightNegativo.java
"$OPENJDK/bin/java" -cp test-fuzzing/caso ShiftRightNegativo
./target/release/run-headless.exe test-fuzzing/caso/ShiftRightNegativo.class run
```

KajiJDK escribe la salida del programa por **stderr** e intercala líneas `[gc]`, que hay que filtrar
para comparar.

Para correr las campañas, también desde la raíz del repo:

```bash
python test-fuzzing/fuzzer/bigfuzz.py --jdk "$OPENJDK" --seed 1 --lotes 6 --por-lote 120 --reducir 8
python test-fuzzing/fuzzer/strfuzz.py --jdk "$OPENJDK" --seed 1 --lotes 6 --por-lote 120 --reducir 8
```

Los datos quedan en `test-fuzzing/datos/`, y se guardan después de cada lote y de cada reducción, así
que una campaña cortada no pierde lo ya medido. Los programas generados quedan en
`test-fuzzing/fuzzer/runs/`.

La prueba de `String` tiene dos cuidados propios. Cada cadena se escribe en el programa generado como
la lista de sus códigos, `s(65, 55296)`, y cada resultado se imprime igual, `[65,55296]`, así que ni el
código fuente ni la consola pueden cambiar un carácter. Y las mayúsculas van con `Locale.ROOT`, para
que no dependan del idioma de la máquina.

## Los informes

Cada informe presenta una campaña: cuántas expresiones se compararon, qué errores aparecieron, sus
casos mínimos con cada paso de la reducción, y todas las divergencias. Son páginas autónomas, con los
datos adentro, que se abren sin servidor y **no ejecutan nada**: los scripts corren las pruebas y
guardan los datos, y las páginas solo los muestran.

Se generan a partir de dos archivos de `datos/`:

| Archivo | Quién lo escribe |
|---|---|
| `campaña-<semilla>.json` y `strings-<semilla>.json` | El fuzzer. Son las mediciones, sin interpretar |
| `diagnostico.json` y `diagnostico-strings.json` | Una persona, después de leer el código. Agrupan los casos mínimos por causa y explican cada una |

Un caso mínimo que no figure en el diagnóstico aparece igual en el informe, marcado como pendiente.

```bash
python test-fuzzing/presentar.py --prueba bigint  --semilla 1
python test-fuzzing/presentar.py --prueba strings --semilla 1
```

Para publicar un informe como página hay que generarlo con `--fragmento`, que omite el `<head>`. El
informe de `BigInteger` incluye una introducción al fuzzing; el de `String` va directo a los
resultados.

### Lo que encontró la prueba de `BigInteger`, semilla 1

720 expresiones, 16 divergencias, reducidas a **dos errores distintos** de `KajiLibrary`:

- **`shiftRight` redondea hacia cero con los negativos**, cuando la especificación pide redondear
  hacia menos infinito. Mínimo: `new BigInteger("-1").shiftRight(1)` da -1 en OpenJDK y 0 en KajiJDK.
- **`testBit` ignora el signo**, cuando la especificación pide responder sobre el complemento a dos.
  Mínimo: `new BigInteger("-1").testBit(31)` da `true` en OpenJDK y `false` en KajiJDK.

Un tercer caso, un `modPow` que lanza en KajiJDK, no es un error propio: recibe el 0 que produce el
error de `shiftRight`, y OpenJDK también lanza con ese 0.

### Lo que encontró la prueba de `String`, semilla 1

720 expresiones, 5 divergencias, reducidas a **dos errores distintos** de `KajiLibrary`. Las cadenas
se escriben como la lista de sus códigos, igual que en el programa generado:

- **Comparar ignorando mayúsculas solo funciona con la A a la Z.** El plegado suma 32 a las letras
  entre `A` y `Z` y deja el resto igual, mientras OpenJDK pliega por punto de código con las reglas de
  Unicode. Mínimo: `s(8486).compareToIgnoreCase(s(55297))` da -54328 en OpenJDK y -46811 en KajiJDK,
  donde 8486 es el signo ohm. El mismo plegado lo usan `equalsIgnoreCase` y `CASE_INSENSITIVE_ORDER`.
- **Buscar una cadena vacía más allá del final devuelve −1.** Mínimo: `s().indexOf(s(), 1)` da 0 en
  OpenJDK, porque la posición de arranque se acota al largo de la cadena, y -1 en KajiJDK.
