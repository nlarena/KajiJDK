package java.util;

import java.util.random.RandomGenerator;

// A pseudo-random generator: a 48-bit **linear congruential** sequence, seed = seed * 0x5DEECE66D
// + 0xB, exactly as specified by the JDK — which means a given seed produces the same numbers
// here as it does there. That reproducibility is the point of specifying an algorithm in the
// API instead of leaving it to the implementation.
//
// It is not cryptographically secure: 48 bits of state, and the next value follows from the
// previous one by arithmetic anyone can invert.
//
// Implements RandomGenerator, as the JDK's does — so anything written against the interface can
// take this generator, and the derived methods it does not override come from there. It keeps its
// own nextInt/nextInt(int)/nextDouble/nextBoolean because the JDK specifies THOSE exact bodies for
// this class: the values a given seed produces are part of Random's contract, not free to inherit.
//
// Los flujos (ints/longs/doubles) los aporta `RandomGenerator` como defaults; aca solo estan los
// metodos cuyos valores son parte del contrato de ESTA clase.
public class Random implements RandomGenerator {

    private long seed;

    // El segundo gaussiano, guardado. El metodo de abajo produce **dos** valores por vuelta y seria
    // un desperdicio tirar uno: la llamada impar calcula el par y devuelve el primero, la par
    // devuelve el que quedo. De paso, esto es lo que hace que la secuencia dependa de la paridad de
    // las llamadas, y por eso los dos campos son parte del estado y no una optimizacion local.
    private double nextNextGaussian;

    private boolean haveNextNextGaussian;


    /**
     * Un `Random` que delega en el generador dado.
     *
     * <p>El puente entre la API vieja y la nueva, y va en esta direccion porque es la que hace
     * falta: hay mucho codigo que **pide un `Random`** como parametro --`Collections.shuffle`, sin
     * ir mas lejos-- y no se puede cambiar. Con esto se le puede pasar un generador moderno
     * (`SplittableRandom`, `Xoshiro256PlusPlus`) sin tocar esa firma.
     *
     * <p>El que sale **no tiene semilla propia**: `setSeed` se niega, porque la semilla vive en el
     * generador de atras y este objeto no tiene como cambiarla.
     */
    public static Random from(java.util.random.RandomGenerator generator) {
        if (generator == null) {
            throw new NullPointerException();
        }
        return new RandomAdapter(generator);
    }

    public Random() {
        this(System.currentTimeMillis());
    }

    public Random(long seed) {
        setSeed(seed);
    }

    /**
     * El constructor **sin semilla**, para una subclase que no tiene ninguna.
     *
     * <p>Hace falta por algo que no se ve hasta que explota: el `Random(long)` de arriba llama a
     * `setSeed`, que es **virtual**. Una subclase que se niega a aceptar semilla --y `RandomAdapter`
     * se niega, porque la suya vive en el generador de atras-- reventaria durante su propia
     * construccion, antes de que nadie llegue a usarla. `Random.from` devolvia un objeto que tiraba
     * `UnsupportedOperationException` al crearse.
     *
     * <p>`Void` no tiene instancias, asi que el unico argumento posible es `null`: no hay forma de
     * confundir esta sobrecarga con la de la semilla, ni de llamarla por accidente.
     */
    Random(Void sinSemilla) {
    }

    // The seed is scrambled and masked to 48 bits on the way in, as the JDK does.
    public synchronized void setSeed(long seed) {
        this.seed = (seed ^ 0x5DEECE66DL) & 281474976710655L;
    }

    // The generator itself: advance the state and hand back the top `bits` of it. Every public
    // method below is a thin wrapper over this one.
    protected int next(int bits) {
        seed = (seed * 0x5DEECE66DL + 0xBL) & 281474976710655L;
        return (int) (seed >>> (48 - bits));
    }

    public int nextInt() {
        return next(32);
    }

    // Uniform in [0, bound). The retry loop is what keeps it uniform: simply taking a modulus
    // would favour the low values whenever `bound` does not divide the range evenly.
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int r;
        if ((bound & -bound) == bound) {
            // A power of two divides the range exactly, so a shift suffices.
            r = (int) ((bound * (long) next(31)) >> 31);
        } else {
            int bits = next(31);
            int val = bits % bound;
            while (bits - val + (bound - 1) < 0) {
                bits = next(31);
                val = bits % bound;
            }
            r = val;
        }
        return r;
    }

    public long nextLong() {
        return ((long) next(32) << 32) + next(32);
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public float nextFloat() {
        return next(24) / ((float) (1 << 24));
    }

    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) * 1.1102230246251565E-16;
    }


    /**
     * Un valor de una normal de media 0 y desvio 1.
     *
     * <p>Es el **metodo polar de Marsaglia**, y esta escrito con esa forma exacta porque el
     * resultado es parte del contrato: dos `Random` con la misma semilla tienen que dar los mismos
     * gaussianos, bit por bit. El algoritmo:
     *
     * <ol>
     * <li>Se tiran puntos uniformes en el cuadrado {@code [-1,1]x[-1,1]} hasta que uno caiga dentro
     *     del circulo unitario. Los de afuera se descartan --de ahi el bucle-- y los de adentro
     *     quedan con angulo uniforme, que es lo que hace falta.</li>
     * <li>Con {@code s} el radio al cuadrado, el factor
     *     {@code sqrt(-2*log(s)/s)} convierte ese punto en un par de normales independientes.</li>
     * </ol>
     *
     * <p>Se usan `StrictMath.sqrt` y `StrictMath.log` --no `Math`-- justamente porque el valor esta
     * en el contrato: `Math` tiene permiso de usar un intrinseco de la maquina y dar un ulp
     * distinto, y aca eso cambiaria la secuencia.
     *
     * <p>Cuidado con `s == 0`: no solo haria una division por cero, sino que `log(0)` es
     * {@code -infinito}. El bucle lo descarta junto con los de afuera del circulo.
     */
    public synchronized double nextGaussian() {
        if (this.haveNextNextGaussian) {
            this.haveNextNextGaussian = false;
            return this.nextNextGaussian;
        }
        double v1 = 0.0d;
        double v2 = 0.0d;
        double s = 0.0d;
        boolean sirve = false;
        while (!sirve) {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
            sirve = s < 1 && s != 0;
        }
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        this.nextNextGaussian = v2 * multiplier;
        this.haveNextNextGaussian = true;
        return v1 * multiplier;
    }

    public void nextBytes(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            int rnd = nextInt();
            int n = bytes.length - i;
            if (n > 4) {
                n = 4;
            }
            for (int j = 0; j < n; j++) {
                bytes[i] = (byte) rnd;
                rnd = rnd >> 8;
                i++;
            }
        }
    }
}

// El `Random` que devuelve `Random.from`: reenvia todo al generador de atras.
//
// Extiende `Random` --y no lo envuelve-- porque el punto es pasarselo a codigo que pide un `Random`
// por tipo. La semilla heredada queda sin usar: todos los metodos que la leerian estan
// sobreescritos.
final class RandomAdapter extends Random {

    private final java.util.random.RandomGenerator atras;

    RandomAdapter(java.util.random.RandomGenerator atras) {
        // El constructor sin semilla: el de arriba llamaria al `setSeed` de aca abajo, que se niega.
        super(null);
        this.atras = atras;
    }

    // La semilla vive en el generador de atras, y este objeto no tiene como cambiarla. Negarse es
    // lo unico honesto: aceptar y no hacer nada seria peor.
    public synchronized void setSeed(long seed) {
        throw new UnsupportedOperationException();
    }

    public long nextLong() {
        return this.atras.nextLong();
    }

    public int nextInt() {
        return this.atras.nextInt();
    }

    public int nextInt(int bound) {
        return this.atras.nextInt(bound);
    }

    public boolean nextBoolean() {
        return this.atras.nextBoolean();
    }

    public double nextDouble() {
        return this.atras.nextDouble();
    }

    public float nextFloat() {
        return this.atras.nextFloat();
    }

    public void nextBytes(byte[] bytes) {
        this.atras.nextBytes(bytes);
    }
}
