package jdk.internal.random;

import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The same LXM recipe as L64X128MixRandom with
 * a BIGGER xor-shift half: xoshiro256 instead of xoroshiro128.
 *
 * <p>What buying 128 more bits of state actually gets you is EQUIDISTRIBUTION, not a longer period in
 * any practical sense (2<sup>64</sup> x (2<sup>128</sup> - 1) is already unreachable). A generator is k-equidistributed
 * when every possible k-tuple of consecutive outputs appears equally often; more XBG state raises
 * k, which matters when the values are consumed in groups — shuffling, sampling coordinates,
 * generating vectors. That is the axis the LXM family scales along, and it is why there are four
 * XBG sizes rather than one.
 *
 * <p>The XBG here is exactly the transition of Xoshiro256PlusPlus (shift 17, rotate 45) — only the
 * scrambler differs, because in LXM the output comes from mixing the LCG and XBG states together
 * rather than from the XBG alone.
 *
 * @implNote An INTERNAL class. A KajiLibrary subset: the JDK's extends an abstract splittable base and
 *           implements the nested SplittableGenerator interface (finding #101), so {@code split} is omitted, as are
 *           the byte[] seed constructor and the no-arg one (which needs a shared AtomicLong counter).
 */
public final class L64X256MixRandom implements RandomGenerator.SplittableGenerator {

    private static long multiplier() {
        return -3372029247567499371L;
    }

    private long s;
    private final long a;
    private long x0;
    private long x1;
    private long x2;
    private long x3;

    /**
     * Creates a generator with the given subgenerator state.
     *
     * @param a the LCG addend; forced odd, which is what gives the LCG its full period
     * @param s the initial LCG state
     * @param x0 the first xor-shift state word
     * @param x1 the second xor-shift state word
     * @param x2 the third xor-shift state word
     * @param x3 the fourth xor-shift state word
     * @implSpec An all-zero xor-shift state is a fixed point of its transition, so it is
     *           replaced by a known-good state. The LCG half needs no such guard: it
     *           advances whatever its value.
     */
    public L64X256MixRandom(long a, long s, long x0, long x1, long x2, long x3) {
        this.a = a | 1L;
        this.s = s;
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
        if ((x0 | x1 | x2 | x3) == 0L) {
            long v = s;
            this.x0 = RandomSupport.mixStafford13(v);
            v = v + Bits.goldenRatio64();
            this.x1 = RandomSupport.mixStafford13(v);
            v = v + Bits.goldenRatio64();
            this.x2 = RandomSupport.mixStafford13(v);
            this.x3 = RandomSupport.mixStafford13(v + Bits.goldenRatio64());
        }
    }

    /**
     * Creates a generator seeded from a single {@code long}.
     *
     * @param seed the seed
     * @implSpec The seed is expanded through the mixing functions of
     *           {@link jdk.internal.util.random.RandomSupport}: a raw seed would leave a
     *           nearly-empty state that takes many steps to fill. The LCG addend and the
     *           xor-shift words come from DIFFERENT mixers, so no seed can make the two
     *           subgenerators agree.
     */
    public L64X256MixRandom(long seed) {
        this(RandomSupport.mixMurmur64(seed ^ Bits.silverRatio64()),
                1L,
                RandomSupport.mixStafford13(seed ^ Bits.silverRatio64()),
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
     * @implSpec The output is computed from the CURRENT state of both subgenerators, and
     *           only then are the two advanced independently — the LCG by its multiply-add,
     *           the xor-shift by its own transition. Mixing two generators whose failure
     *           modes differ is what makes the combination stronger than either half.
     */
    public long nextLong() {
        long result = RandomSupport.mixLea64(this.s + this.x0);

        this.s = L64X256MixRandom.multiplier() * this.s + this.a;

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

    // ---- las tres entradas que faltaban ----------------------------------------------------------

    // La semilla de los generadores sin argumentos. Es un contador compartido que avanza de a
    // GOLDEN_RATIO_64: dos generadores creados uno detras del otro no arrancan en estados vecinos,
    // que es lo unico que se le pide.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** Un generador con una semilla elegida sola, distinta en cada llamada. */
    public L64X256MixRandom() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Un generador sembrado desde bytes.
     *
     * <p>Los bytes se reparten en las palabras del estado y, si no alcanzan, el resto se rellena con
     * un generador auxiliar -- una semilla corta dejaria el estado casi en cero, que para un
     * xor-shift es un punto fijo. Lo hace {@link RandomSupport#convertSeedBytesToLongs}, con los
     * mismos 6 y 4 que usa el JDK para este algoritmo, asi que la misma semilla da el mismo
     * generador.
     */
    public L64X256MixRandom(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 6, 4);
        this.a = data[0] | 1L;
        this.s = data[1];
        this.x0 = data[2];
        this.x1 = data[3];
        this.x2 = data[4];
        this.x3 = data[5];
    }

    // ---- particion ---------------------------------------------------------------------------------
    //
    // Partir no es "sembrar otro al azar": dos semillas cercanas pueden dar secuencias que se pisan.
    // La garantia sale de que el **addend** del LCG --la `a`-- del hijo se toma de la salmuera
    // (`brine`) y no del azar, con lo cual cada hijo recorre una orbita distinta del mismo espacio.
    //
    // El corrimiento `brine << 1` deja el bit bajo libre, que es donde el constructor fuerza el
    // impar. Sin eso, la mitad de las salmueras darian el mismo addend.

    /** Un generador independiente de este, con la entropia de este. */
    public SplittableGenerator split() {
        return this.split(this);
    }

    /** Un generador independiente de este, con la entropia de `source`. */
    public SplittableGenerator split(SplittableGenerator source) {
        return this.split(source, source.nextLong());
    }

    /** El de arriba con la salmuera explicita: es el que hace el trabajo. */
    public SplittableGenerator split(SplittableGenerator source, long brine) {
        return new L64X256MixRandom(brine << 1, source.nextLong(),
                source.nextLong(), source.nextLong(), source.nextLong(), source.nextLong());
    }

    /** `streamSize` generadores independientes, con la entropia de este. */
    public java.util.stream.Stream<SplittableGenerator> splits(long streamSize) {
        return this.splits(streamSize, this);
    }

    /** `streamSize` generadores independientes, con la entropia de `source`. */
    public java.util.stream.Stream<SplittableGenerator> splits(long streamSize,
            SplittableGenerator source) {
        return Splits.de(this, streamSize, source);
    }
}
