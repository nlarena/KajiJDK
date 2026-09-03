package jdk.internal.random;

import java.util.random.RandomGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * A xor/shift/rotate generator with 256 bits of state and a period of
 * 2<sup>256</sup>&nbsp;&minus;&nbsp;1.
 *
 * <p>Where {@link java.util.Random} advances by multiplication modulo 2<sup>48</sup> — so its state
 * <em>is</em> a number, and neighbouring seeds produce visibly related streams — this generator
 * advances by xors, shifts and rotations. Pure bit stirring, no carry chain: much faster to step
 * and a far longer period, at the cost of needing a well-mixed seed to start.
 *
 * <p>The {@code ++} in the name is the <em>scrambler</em>. The raw state is never returned;
 * {@code rotl(x0 + x3, 23) + x0} takes two state words, adds, rotates and adds again, which fixes
 * the weak low bits the plain {@code +} variant is known for. The state transition and the output
 * function are independent designs, and only the second one changes between the {@code +},
 * {@code ++} and {@code **} members of the family.
 *
 * @implNote An internal class: {@code java.util.random} exposes only {@link RandomGenerator} and
 *           {@code RandomGeneratorFactory}.
 * @implNote A KajiLibrary subset. The JDK's version implements the nested interface
 *           {@code RandomGenerator.LeapableGenerator} — jump and leap to a distant point in the
 *           stream — which a nested type cannot resolve to (finding #101), so this one implements
 *           the top-level {@link RandomGenerator} and omits {@code jump}/{@code leap}.
 */
public final class Xoshiro256PlusPlus implements RandomGenerator.LeapableGenerator {

    private long x0;
    private long x1;
    private long x2;
    private long x3;

    /**
     * Creates a generator with the given state words.
     *
     * @param x0 the first state word
     * @param x1 the second state word
     * @param x2 the third state word
     * @param x3 the fourth state word
     * @implSpec An all-zero state is a fixed point of the transition — it would emit zeros forever
     *           — so it is replaced by a known-good state.
     */
    public Xoshiro256PlusPlus(long x0, long x1, long x2, long x3) {
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
        if ((x0 | x1 | x2 | x3) == 0L) {
            this.x0 = Bits.silverRatio64();
            this.x1 = Bits.goldenRatio64();
            this.x2 = this.x0;
            this.x3 = this.x1;
        }
    }

    /**
     * Creates a generator seeded from a single {@code long}.
     *
     * @param seed the seed
     * @implSpec The seed is expanded into four words, each a further mix of a counter advanced by
     *           the golden ratio. The mixing matters: seeding a shift-register generator directly
     *           with a small number leaves a state that is nearly all zeros, which takes many steps
     *           to fill and produces poor early output.
     */
    public Xoshiro256PlusPlus(long seed) {
        this(RandomSupport.mixStafford13(seed ^ Bits.silverRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64() + Bits.goldenRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64() + Bits.goldenRatio64()
                        + Bits.goldenRatio64()));
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec The scrambler reads the state <em>before</em> it advances, so the returned value
     *           and the next state are computed from the same snapshot.
     */
    public long nextLong() {
        long result = Bits.rotateLeft(this.x0 + this.x3, 23) + this.x0;

        long q0 = this.x0;
        long q1 = this.x1;
        long q2 = this.x2;
        long q3 = this.x3;
        long t = q1 << 17;
        q2 = q2 ^ q0;
        q3 = q3 ^ q1;
        q1 = q1 ^ q2;
        q0 = q0 ^ q3;
        q2 = q2 ^ t;
        q3 = Bits.rotateLeft(q3, 45);
        this.x0 = q0;
        this.x1 = q1;
        this.x2 = q2;
        this.x3 = q3;
        return result;
    }

    // ---- las entradas que faltaban ---------------------------------------------------------------

    // La semilla de los generadores sin argumentos: un contador compartido que avanza de a
    // GOLDEN_RATIO_64, para que dos generadores seguidos no arranquen en estados vecinos.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** Un generador con una semilla elegida sola, distinta en cada llamada. */
    public Xoshiro256PlusPlus() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Un generador sembrado desde bytes.
     *
     * <p>Los 4 valores que salen de la semilla **no pueden ser todos cero**: para un xor-shift el
     * cero es un punto fijo, y el generador se quedaria ahi. Lo garantiza
     * `RandomSupport.convertSeedBytesToLongs`, con los mismos parametros que usa el JDK.
     */
    public Xoshiro256PlusPlus(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 4, 4);
        this.x0 = data[0];
        this.x1 = data[1];
        this.x2 = data[2];
        this.x3 = data[3];
    }

    // ---- salto y salto largo ----------------------------------------------------------------------
    //
    // Las dos tablas son los **polinomios de salto** del algoritmo: cada bit en uno dice que hay que
    // acumular el estado en ese paso. Recorrerlas equivale a avanzar `jumpDistance()` valores, y esa
    // equivalencia es la que hace que dos hilos que arrancan a un salto de distancia recorran tramos
    // que **no se solapan** -- no es una garantia estadistica sino aritmetica.
    //
    // Los numeros no son ajustables: son los publicados para este generador.

    private static final long[] TABLA_SALTO = { 0x180ec6d33cfd0abaL, 0xd5a61266f0c9392cL, 0xa9582618e03fc9aaL, 0x39abdc4529b1661cL };

    private static final long[] TABLA_SALTO_LARGO = { 0x76e15d3efefdcbbfL, 0xc5004e441c522fb3L, 0x77710069854ee241L, 0x39109bb02acbe635L };

    /** Una copia de este generador, en el mismo estado. */
    public Xoshiro256PlusPlus copy() {
        return new Xoshiro256PlusPlus(this.x0, this.x1, this.x2, this.x3);
    }

    /** Avanza este generador dos a la 128 valores. */
    public void jump() {
        this.saltar(TABLA_SALTO);
    }

    /** Avanza este generador dos a la 192 valores. */
    public void leap() {
        this.saltar(TABLA_SALTO_LARGO);
    }

    /** Cuantos valores avanza {@link #jump()}. */
    public double jumpDistance() {
        return Math.scalb(1.0d, 128);
    }

    /** Cuantos valores avanza {@link #leap()}. */
    public double leapDistance() {
        return Math.scalb(1.0d, 192);
    }

    // El algoritmo de salto: se avanza el generador 64 veces por palabra de la tabla, acumulando el
    // estado en los pasos que la tabla marca. Al final el acumulador **es** el estado que el
    // generador habria tenido despues de la distancia de salto.
    private void saltar(long[] tabla) {
        long s0 = 0L;
        long s1 = 0L;
        long s2 = 0L;
        long s3 = 0L;
        int i = 0;
        while (i < tabla.length) {
            int b = 0;
            while (b < 64) {
                if ((tabla[i] & (1L << b)) != 0L) {
                    s0 ^= this.x0;
                    s1 ^= this.x1;
                    s2 ^= this.x2;
                    s3 ^= this.x3;
                }
                this.nextLong();
                b = b + 1;
            }
            i = i + 1;
        }
        this.x0 = s0;
        this.x1 = s1;
        this.x2 = s2;
        this.x3 = s3;
    }
}
