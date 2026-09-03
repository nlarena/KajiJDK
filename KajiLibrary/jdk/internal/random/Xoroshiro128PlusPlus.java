package jdk.internal.random;

import java.util.random.RandomGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The 128-bit sibling of {@link Xoshiro256PlusPlus}: same family, half the state, period
 * 2<sup>128</sup>&nbsp;&minus;&nbsp;1.
 *
 * <p>The name states the difference. <em>xoro</em> is xor/<b>rotate</b>, <em>xosh</em> is
 * xor/<b>shift</b>. With only two state words there is no room for a shift-based transition that
 * still mixes every word, so this one rotates the words into each other instead:
 *
 * <pre>
 *     x1 ^= x0
 *     x0  = rotl(x0, 49) ^ x1 ^ (x1 &lt;&lt; 21)
 *     x1  = rotl(x1, 28)
 * </pre>
 *
 * <p>Every step touches both words. The three constants are search-tuned: changing 49, 21 or 28
 * does not give "a slightly different generator", it gives a worse one.
 *
 * <p>Note that the xoroshiro128 used <em>inside</em> the LXM generators is a different variant,
 * with rotations (24, 16, 37). Same family, different tuning; they are not interchangeable.
 *
 * @implNote An internal class. A KajiLibrary subset: the JDK's implements the nested interface
 *           {@code RandomGenerator.LeapableGenerator}, which does not resolve (finding #101), so
 *           {@code jump}/{@code leap} are omitted.
 */
public final class Xoroshiro128PlusPlus implements RandomGenerator.LeapableGenerator {

    private long x0;
    private long x1;

    /**
     * Creates a generator with the given state words.
     *
     * @param x0 the first state word
     * @param x1 the second state word
     * @implSpec An all-zero state is a fixed point, so it is replaced by a known-good state.
     */
    public Xoroshiro128PlusPlus(long x0, long x1) {
        this.x0 = x0;
        this.x1 = x1;
        if ((x0 | x1) == 0L) {
            this.x0 = Bits.silverRatio64();
            this.x1 = Bits.goldenRatio64();
        }
    }

    /**
     * Creates a generator seeded from a single {@code long}.
     *
     * @param seed the seed
     */
    public Xoroshiro128PlusPlus(long seed) {
        this(RandomSupport.mixStafford13(seed ^ Bits.silverRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64()));
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec The {@code ++} scrambler is the same idea as its bigger sibling's, with its own
     *           rotation: {@code rotl(x0 + x1, 17) + x0}.
     */
    public long nextLong() {
        long s0 = this.x0;
        long s1 = this.x1;
        long result = Bits.rotateLeft(s0 + s1, 17) + s0;

        s1 = s1 ^ s0;
        this.x0 = Bits.rotateLeft(s0, 49) ^ s1 ^ (s1 << 21);
        this.x1 = Bits.rotateLeft(s1, 28);
        return result;
    }

    // ---- las entradas que faltaban ---------------------------------------------------------------

    // La semilla de los generadores sin argumentos: un contador compartido que avanza de a
    // GOLDEN_RATIO_64, para que dos generadores seguidos no arranquen en estados vecinos.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** Un generador con una semilla elegida sola, distinta en cada llamada. */
    public Xoroshiro128PlusPlus() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Un generador sembrado desde bytes.
     *
     * <p>Los 2 valores que salen de la semilla **no pueden ser todos cero**: para un xor-shift el
     * cero es un punto fijo, y el generador se quedaria ahi. Lo garantiza
     * `RandomSupport.convertSeedBytesToLongs`, con los mismos parametros que usa el JDK.
     */
    public Xoroshiro128PlusPlus(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 2, 2);
        this.x0 = data[0];
        this.x1 = data[1];
    }

    // ---- salto y salto largo ----------------------------------------------------------------------
    //
    // Las dos tablas son los **polinomios de salto** del algoritmo: cada bit en uno dice que hay que
    // acumular el estado en ese paso. Recorrerlas equivale a avanzar `jumpDistance()` valores, y esa
    // equivalencia es la que hace que dos hilos que arrancan a un salto de distancia recorran tramos
    // que **no se solapan** -- no es una garantia estadistica sino aritmetica.
    //
    // Los numeros no son ajustables: son los publicados para este generador.

    private static final long[] TABLA_SALTO = { 0x2bd7a6a6e99c2ddcL, 0x0992ccaf6a6fca05L };

    private static final long[] TABLA_SALTO_LARGO = { 0x360fd5f2cf8d5d99L, 0x9c6e6877736c46e3L };

    /** Una copia de este generador, en el mismo estado. */
    public Xoroshiro128PlusPlus copy() {
        return new Xoroshiro128PlusPlus(this.x0, this.x1);
    }

    /** Avanza este generador dos a la 64 valores. */
    public void jump() {
        this.saltar(TABLA_SALTO);
    }

    /** Avanza este generador dos a la 96 valores. */
    public void leap() {
        this.saltar(TABLA_SALTO_LARGO);
    }

    /** Cuantos valores avanza {@link #jump()}. */
    public double jumpDistance() {
        return Math.scalb(1.0d, 64);
    }

    /** Cuantos valores avanza {@link #leap()}. */
    public double leapDistance() {
        return Math.scalb(1.0d, 96);
    }

    // El algoritmo de salto: se avanza el generador 64 veces por palabra de la tabla, acumulando el
    // estado en los pasos que la tabla marca. Al final el acumulador **es** el estado que el
    // generador habria tenido despues de la distancia de salto.
    private void saltar(long[] tabla) {
        long s0 = 0L;
        long s1 = 0L;
        int i = 0;
        while (i < tabla.length) {
            int b = 0;
            while (b < 64) {
                if ((tabla[i] & (1L << b)) != 0L) {
                    s0 ^= this.x0;
                    s1 ^= this.x1;
                }
                this.nextLong();
                b = b + 1;
            }
            i = i + 1;
        }
        this.x0 = s0;
        this.x1 = s1;
    }
}
