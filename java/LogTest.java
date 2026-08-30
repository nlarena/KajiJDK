// Verifica `Math.log`/`StrictMath.log` **bit por bit** contra la implementacion de referencia.
//
// El valor tiene que coincidir exacto, no "parecerse": `Random.nextGaussian()` compone `log` con
// `sqrt`, y un ulp de diferencia cambia todos los gaussianos de la secuencia. Por eso el resumen
// mezcla los **bits crudos** del resultado y no una comparacion con tolerancia -- una tolerancia
// haria pasar justamente el error que se quiere detectar.
//
// Se compara contra `StrictMath` y no contra `Math` porque `Math.log` tiene permiso de usar un
// intrinseco de la maquina; `StrictMath.log` es fdlibm por contrato, que es lo que aca se escribio.
public class LogTest {

    // Un revoltijo barato que hace que cualquier bit que cambie mueva el resumen.
    static int mezclar(int h, long bits) {
        h = h * 31 + (int) (bits >>> 32);
        h = h * 31 + (int) bits;
        return h;
    }

    static int resumen() {
        int h = 17;

        // Tramo 1: los enteros chicos, donde k va creciendo de a uno.
        int i = 1;
        while (i <= 200) {
            h = mezclar(h, Double.doubleToRawLongBits(StrictMath.log((double) i)));
            i = i + 1;
        }

        // Tramo 2: alrededor de 1, que es donde el algoritmo cambia de rama. Incluye el caso
        // |f| < 2^-20 y el borde de la mantisa centrada en [sqrt(2)/2, sqrt(2)).
        double[] cerca = new double[] {
            1.0, 0.9999999, 1.0000001, 0.999999999999, 1.000000000001,
            0.7071067811865476, 0.70710678118654746, 1.4142135623730951, 1.4142135623730949,
            0.5, 2.0, 0.75, 1.5, 0.9, 1.1, 0.99, 1.01,
        };
        int j = 0;
        while (j < cerca.length) {
            h = mezclar(h, Double.doubleToRawLongBits(StrictMath.log(cerca[j])));
            j = j + 1;
        }

        // Tramo 3: los extremos, incluidos los subnormales (la rama que escala por 2^54).
        double[] bordes = new double[] {
            Double.MIN_VALUE, 4.9E-324, 1.0E-320, 2.2250738585072014E-308, 1.0E-300,
            Double.MAX_VALUE, 1.7976931348623157E308, 1.0E300, 1.0E-10, 1.0E10,
            0.0, -0.0, -1.0, -1.0E10, Double.POSITIVE_INFINITY,
        };
        j = 0;
        while (j < bordes.length) {
            h = mezclar(h, Double.doubleToRawLongBits(StrictMath.log(bordes[j])));
            j = j + 1;
        }

        // Tramo 4: un barrido pseudoaleatorio por todo el rango de exponentes. La semilla es fija
        // para que las dos corridas vean exactamente los mismos bits de entrada.
        long semilla = 0x5DEECE66DL;
        int n = 0;
        while (n < 500) {
            semilla = (semilla * 0x5DEECE66DL + 0xB) & ((1L << 48) - 1);
            // Se arma un double con exponente en [1, 2046] y mantisa arbitraria: siempre normal y
            // siempre positivo, para caer en el camino principal.
            long mant = semilla & 0xFFFFFFFFFFFFFL;
            long exp = (long) (n % 2046) + 1L;
            double x = Double.longBitsToDouble((exp << 52) | mant);
            h = mezclar(h, Double.doubleToRawLongBits(StrictMath.log(x)));
            n = n + 1;
        }

        // Tramo 5: NaN aparte, porque su bit patron no sobrevive a una comparacion normal.
        double nan = StrictMath.log(Double.NaN);
        h = mezclar(h, nan != nan ? 1L : 0L);

        return h;
    }

    // `nextGaussian` completo: la unica prueba que importa de verdad, porque compone log con sqrt.
    static int gaussianos() {
        java.util.Random r = new java.util.Random(42L);
        int h = 17;
        int i = 0;
        while (i < 100) {
            h = mezclar(h, Double.doubleToRawLongBits(r.nextGaussian()));
            i = i + 1;
        }
        // Con otra semilla, y alternando con otros metodos para mover la paridad del par guardado.
        java.util.Random s = new java.util.Random(-7L);
        i = 0;
        while (i < 50) {
            h = mezclar(h, Double.doubleToRawLongBits(s.nextGaussian()));
            h = h * 31 + s.nextInt(1000);
            i = i + 1;
        }
        return h;
    }

    public static int run() {
        return resumen() * 31 + gaussianos();
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) {
        System.out.println(run());
    }
}
