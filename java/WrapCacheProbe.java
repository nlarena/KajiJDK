// Probe de regresion de #275: las caches de wrapper que JLS 5.1.7 EXIGE.
//
//   Integer a = 100, b = 100;   a == b   tiene que ser TRUE
//   Integer c = 200, d = 200;   c == d   tiene que ser FALSE
//
// No es una optimizacion perdida: es una regla del lenguaje. Boxear un valor entre -128 y 127
// tiene que devolver la MISMA referencia, y comparar enteros chicos con `==` -- comun, aunque sea
// mala practica -- anda en cualquier JVM.
//
// Lo que hace que este probe exista y no solo el arreglo: la cache YA HABIA ESTADO BIEN. Vivia en
// `boot/java/lang/Integer.class`, y cuando el `Integer` de KajiLibrary tomo su lugar -- KajiLibrary
// gana en el bootclasspath -- se perdio, y NADA lo noto, porque ninguna prueba comparaba dos
// boxeos con `==`. Un arreglo sin prueba se vuelve a perder por el mismo camino.
//
// Las dos direcciones importan igual: una cache que cubriera TODOS los valores haria que el codigo
// que compara con `==` pareciera andar, y se romperia contra un JDK real. Por eso los bits que
// piden `!=` valen tanto como los que piden `==`.
//
// Un bit por propiedad, para que una falla parcial se nombre sola. Las dieciocho -> 262143.
// Verificado como oraculo: el JDK 21 corriendo esta misma fuente imprime 262143.
public class WrapCacheProbe {
    public static int run() {
        int score = 0;

        // --- Integer: por valueOf y por autoboxing, dentro y fuera del rango ---
        if (Integer.valueOf(100) == Integer.valueOf(100)) { score += 1; }
        if (Integer.valueOf(200) != Integer.valueOf(200)) { score += 2; }
        Integer ia = 100, ib = 100;
        if (ia == ib) { score += 4; }
        Integer ic = 200, id = 200;
        if (ic != id) { score += 8; }

        // --- los bordes exactos: -128 y 127 cacheados, -129 y 128 no ---
        if (Integer.valueOf(-128) == Integer.valueOf(-128)) { score += 16; }
        if (Integer.valueOf(127) == Integer.valueOf(127)) { score += 32; }
        if (Integer.valueOf(-129) != Integer.valueOf(-129)) { score += 64; }
        if (Integer.valueOf(128) != Integer.valueOf(128)) { score += 128; }

        // --- Long ---
        if (Long.valueOf(100L) == Long.valueOf(100L)) { score += 256; }
        if (Long.valueOf(200L) != Long.valueOf(200L)) { score += 512; }

        // --- Short ---
        if (Short.valueOf((short) 100) == Short.valueOf((short) 100)) { score += 1024; }
        if (Short.valueOf((short) 200) != Short.valueOf((short) 200)) { score += 2048; }

        // --- Byte: el rango ENTERO entra en la cache, asi que nunca aloca ---
        if (Byte.valueOf((byte) 100) == Byte.valueOf((byte) 100)) { score += 4096; }
        if (Byte.valueOf((byte) -128) == Byte.valueOf((byte) -128)) { score += 8192; }

        // --- Character: cachea 0..127 ---
        if (Character.valueOf('A') == Character.valueOf('A')) { score += 16384; }
        if (Character.valueOf((char) 200) != Character.valueOf((char) 200)) { score += 32768; }

        // --- Boolean: sus dos instancias son constantes ---
        if (Boolean.valueOf(true) == Boolean.TRUE) { score += 65536; }
        if (Boolean.valueOf(false) == Boolean.FALSE) { score += 131072; }

        return score;   // 262143
    }
}
