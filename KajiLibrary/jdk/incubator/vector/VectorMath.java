package jdk.incubator.vector;

/**
 * Aritmetica que satura y aritmetica sin signo, en su version escalar.
 *
 * <h2>Por que hace falta que exista para escalares</h2>
 *
 * <p>Estas operaciones son las que las instrucciones vectoriales de la maquina hacen de verdad --
 * saturar en vez de dar la vuelta es lo normal en el hardware de senales y de imagen. Java no las
 * tiene: su aritmetica siempre da la vuelta.
 *
 * <p>Tener la version escalar aca sirve para dos cosas. Para escribir la version de referencia de un
 * algoritmo vectorial y comparar; y para el remanente, las pocas posiciones que sobran al final de
 * un arreglo cuando no completan un vector entero.
 *
 * <h2>Saturar y dar la vuelta</h2>
 *
 * <p>Con {@code byte}, {@code 100 + 100} da {@code -56} en Java: se paso de 127 y volvio por abajo.
 * {@link #addSaturating(byte, byte)} da {@code 127}.
 *
 * <p>La diferencia importa donde el numero representa una magnitud fisica. En una imagen, un pixel
 * muy iluminado tiene que quedar blanco; con la aritmetica de Java queda <strong>negro</strong>, que
 * es el peor error posible.
 *
 * <h2>Sin signo</h2>
 *
 * <p>Java no tiene enteros sin signo, asi que un {@code byte} de valor 200 se guarda como
 * {@code -56}. Comparar dos de esos con {@code <} da el resultado equivocado. Los
 * {@code minUnsigned} y {@code maxUnsigned} comparan como si no tuvieran signo, que es lo que hace
 * falta para datos que vienen de afuera de Java.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Todo esto es aritmetica entera y no necesita nada de la maquina: esta implementado de verdad y
 * comprobado contra el JDK 25 en todos los valores de borde.
 *
 * @since 19
 */
public final class VectorMath {

    private VectorMath() {
    }

    // ---- long ----

    /**
     * El menor de los dos, comparados sin signo.
     *
     * @param a el primero
     * @param b el segundo
     * @return el menor
     */
    public static long minUnsigned(long a, long b) {
        return Long.compareUnsigned(a, b) < 0 ? a : b;
    }

    /**
     * El mayor de los dos, comparados sin signo.
     *
     * @param a el primero
     * @param b el segundo
     * @return el mayor
     */
    public static long maxUnsigned(long a, long b) {
        return Long.compareUnsigned(a, b) > 0 ? a : b;
    }

    /**
     * Suma con signo que satura en los extremos en vez de dar la vuelta.
     *
     * <p>El desbordamiento se detecta por el signo: solo puede haber si los dos sumandos tienen el
     * mismo signo y el resultado tiene el otro. Con signos distintos la suma nunca se pasa.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma, o el extremo del rango
     */
    public static long addSaturating(long a, long b) {
        final long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return r;
    }

    /**
     * Resta con signo que satura en los extremos.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta, o el extremo del rango
     */
    public static long subSaturating(long a, long b) {
        final long r = a - b;
        if (((a ^ b) & (a ^ r)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return r;
    }

    /**
     * Suma sin signo que satura arriba.
     *
     * <p>Se paso si el resultado es menor que cualquiera de los dos sumandos, comparando sin signo.
     * El tope es todo unos, que como {@code long} con signo se escribe {@code -1}.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma, o el maximo sin signo
     */
    public static long addSaturatingUnsigned(long a, long b) {
        final long r = a + b;
        return Long.compareUnsigned(r, a) < 0 ? -1L : r;
    }

    /**
     * Resta sin signo que satura en cero.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta, o cero
     */
    public static long subSaturatingUnsigned(long a, long b) {
        return Long.compareUnsigned(a, b) < 0 ? 0L : a - b;
    }

    // ---- int ----

    /**
     * El menor de los dos, comparados sin signo.
     *
     * @param a el primero
     * @param b el segundo
     * @return el menor
     */
    public static int minUnsigned(int a, int b) {
        return Integer.compareUnsigned(a, b) < 0 ? a : b;
    }

    /**
     * El mayor de los dos, comparados sin signo.
     *
     * @param a el primero
     * @param b el segundo
     * @return el mayor
     */
    public static int maxUnsigned(int a, int b) {
        return Integer.compareUnsigned(a, b) > 0 ? a : b;
    }

    /**
     * Suma con signo que satura en los extremos.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma, o el extremo del rango
     */
    public static int addSaturating(int a, int b) {
        final int r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            return a < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return r;
    }

    /**
     * Resta con signo que satura en los extremos.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta, o el extremo del rango
     */
    public static int subSaturating(int a, int b) {
        final int r = a - b;
        if (((a ^ b) & (a ^ r)) < 0) {
            return a < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return r;
    }

    /**
     * Suma sin signo que satura arriba.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma, o el maximo sin signo
     */
    public static int addSaturatingUnsigned(int a, int b) {
        final int r = a + b;
        return Integer.compareUnsigned(r, a) < 0 ? -1 : r;
    }

    /**
     * Resta sin signo que satura en cero.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta, o cero
     */
    public static int subSaturatingUnsigned(int a, int b) {
        return Integer.compareUnsigned(a, b) < 0 ? 0 : a - b;
    }

    // ---- short ----
    //
    // Con short y byte se calcula en int y se recorta al final. Es lo que hay que hacer: Java
    // promueve los dos a int antes de operar, asi que el desbordamiento del tipo chico no se puede
    // detectar mirando el resultado de la suma -- nunca se desborda un int.

    /**
     * El menor de los dos, comparados sin signo.
     *
     * @param a el primero
     * @param b el segundo
     * @return el menor
     */
    public static short minUnsigned(short a, short b) {
        return (a & 0xFFFF) < (b & 0xFFFF) ? a : b;
    }

    /**
     * El mayor de los dos, comparados sin signo.
     *
     * @param a el primero
     * @param b el segundo
     * @return el mayor
     */
    public static short maxUnsigned(short a, short b) {
        return (a & 0xFFFF) > (b & 0xFFFF) ? a : b;
    }

    /**
     * Suma con signo que satura en los extremos del {@code short}.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma, o el extremo del rango
     */
    public static short addSaturating(short a, short b) {
        return recortar(a + b);
    }

    /**
     * Resta con signo que satura en los extremos del {@code short}.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta, o el extremo del rango
     */
    public static short subSaturating(short a, short b) {
        return recortar(a - b);
    }

    /**
     * Suma sin signo que satura en 65535.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma, o el maximo sin signo
     */
    public static short addSaturatingUnsigned(short a, short b) {
        final int r = (a & 0xFFFF) + (b & 0xFFFF);
        return (short) (r > 0xFFFF ? 0xFFFF : r);
    }

    /**
     * Resta sin signo que satura en cero.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta, o cero
     */
    public static short subSaturatingUnsigned(short a, short b) {
        final int r = (a & 0xFFFF) - (b & 0xFFFF);
        return (short) (r < 0 ? 0 : r);
    }

    private static short recortar(final int r) {
        if (r > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (r < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) r;
    }

    // ---- byte ----

    /**
     * El menor de los dos, comparados sin signo.
     *
     * @param a el primero
     * @param b el segundo
     * @return el menor
     */
    public static byte minUnsigned(byte a, byte b) {
        return (a & 0xFF) < (b & 0xFF) ? a : b;
    }

    /**
     * El mayor de los dos, comparados sin signo.
     *
     * @param a el primero
     * @param b el segundo
     * @return el mayor
     */
    public static byte maxUnsigned(byte a, byte b) {
        return (a & 0xFF) > (b & 0xFF) ? a : b;
    }

    /**
     * Suma con signo que satura en los extremos del {@code byte}.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma, o el extremo del rango
     */
    public static byte addSaturating(byte a, byte b) {
        return recortarByte(a + b);
    }

    /**
     * Resta con signo que satura en los extremos del {@code byte}.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta, o el extremo del rango
     */
    public static byte subSaturating(byte a, byte b) {
        return recortarByte(a - b);
    }

    /**
     * Suma sin signo que satura en 255.
     *
     * @param a el primero
     * @param b el segundo
     * @return la suma, o el maximo sin signo
     */
    public static byte addSaturatingUnsigned(byte a, byte b) {
        final int r = (a & 0xFF) + (b & 0xFF);
        return (byte) (r > 0xFF ? 0xFF : r);
    }

    /**
     * Resta sin signo que satura en cero.
     *
     * @param a el minuendo
     * @param b el sustraendo
     * @return la resta, o cero
     */
    public static byte subSaturatingUnsigned(byte a, byte b) {
        final int r = (a & 0xFF) - (b & 0xFF);
        return (byte) (r < 0 ? 0 : r);
    }

    private static byte recortarByte(final int r) {
        if (r > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE;
        }
        if (r < Byte.MIN_VALUE) {
            return Byte.MIN_VALUE;
        }
        return (byte) r;
    }
}
