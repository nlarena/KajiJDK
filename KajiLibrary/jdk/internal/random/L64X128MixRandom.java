package jdk.internal.random;

import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The LXM family, and the JDK's modern default
 * shape for a general-purpose generator.
 *
 * <p>LXM is three independent pieces bolted together, and the split is the whole idea:
 *
 * <p>L  an LCG            s = M*s + a. Long period, trivially fast, but its low bits are famously
 * weak and its state is "too smooth".
 * X  an XBG            a xor-shift/rotate generator (here xoroshiro128) run in PARALLEL, not in
 * series. Its weaknesses are of a completely different kind.
 * M  a mixer           the two states are added and run through mixLea64.
 *
 * <p>Combining two generators of DIFFERENT failure modes is what makes the result strong: a
 * statistical test that catches the LCG's lattice structure is not the one that catches the XBG's
 * linear structure, and the mixer destroys what is left. The period is the product of the two
 * subgenerators' periods, 2<sup>64</sup> x (2<sup>128</sup> - 1).
 *
 * <p>Note that the LCG's ADDEND {@code a} is per-instance and odd. That is what makes the family splittable:
 * two instances with different addends walk different LCG orbits, so their streams are independent
 * without needing to coordinate.
 *
 * @implNote CAREFUL: the xoroshiro128 used here is the "v1_0" variant, with rotations (24, 16, 37) — NOT the
 *           (49, 21, 28) of Xoroshiro128PlusPlus. Same family, different tuning; they are not interchangeable.
 *
 * @implNote An INTERNAL class. A KajiLibrary subset: the JDK's implements the nested SplittableGenerator
 *           interface (finding #101), so {@code split()} is omitted.
 */
public final class L64X128MixRandom implements RandomGenerator.SplittableGenerator {

    // The LCG multiplier, 0xd1342543de82ef95.
    private static long multiplier() {
        return -3372029247567499371L;
    }

    private long s;
    private final long a;
    private long x0;
    private long x1;

    /**
     * Creates a generator with the given subgenerator state.
     *
     * @param a the LCG addend; forced odd, which is what gives the LCG its full period
     * @param s the initial LCG state
     * @param x0 the first xor-shift state word
     * @param x1 the second xor-shift state word
     * @implSpec An all-zero xor-shift state is a fixed point of its transition, so it is
     *           replaced by a known-good state. The LCG half needs no such guard: it
     *           advances whatever its value.
     */
    public L64X128MixRandom(long a, long s, long x0, long x1) {
        // The addend must be odd for the LCG to reach full period.
        this.a = a | 1L;
        this.s = s;
        this.x0 = x0;
        this.x1 = x1;
        // An all-zero XBG state is a fixed point; only the XBG half needs the guard, because the
        // LCG half advances regardless of its value.
        if ((x0 | x1) == 0L) {
            this.x0 = Bits.silverRatio64();
            this.x1 = Bits.goldenRatio64();
        }
    }

    // Note the asymmetry, which is deliberate: the addend comes from mixMurmur64 while the XBG
    // words come from mixStafford13, so a seed cannot make the two subgenerators agree.
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
    public L64X128MixRandom(long seed) {
        this(RandomSupport.mixMurmur64(seed ^ Bits.silverRatio64()),
                1L,
                RandomSupport.mixStafford13(seed ^ Bits.silverRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
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
        // Output first, from the CURRENT state of both subgenerators.
        long result = RandomSupport.mixLea64(this.s + this.x0);

        // Advance the LCG.
        this.s = L64X128MixRandom.multiplier() * this.s + this.a;

        // Advance the XBG (xoroshiro128 v1_0).
        long q0 = this.x0;
        long q1 = this.x1;
        q1 = q1 ^ q0;
        q0 = Bits.rotateLeft(q0, 24);
        q0 = q0 ^ q1 ^ (q1 << 16);
        q1 = Bits.rotateLeft(q1, 37);
        this.x0 = q0;
        this.x1 = q1;
        return result;
    }

    // ---- las tres entradas que faltaban ----------------------------------------------------------

    // La semilla de los generadores sin argumentos. Es un contador compartido que avanza de a
    // GOLDEN_RATIO_64: dos generadores creados uno detras del otro no arrancan en estados vecinos,
    // que es lo unico que se le pide.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** Un generador con una semilla elegida sola, distinta en cada llamada. */
    public L64X128MixRandom() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Un generador sembrado desde bytes.
     *
     * <p>Los bytes se reparten en las palabras del estado y, si no alcanzan, el resto se rellena con
     * un generador auxiliar -- una semilla corta dejaria el estado casi en cero, que para un
     * xor-shift es un punto fijo. Lo hace {@link RandomSupport#convertSeedBytesToLongs}, con los
     * mismos 4 y 2 que usa el JDK para este algoritmo, asi que la misma semilla da el mismo
     * generador.
     */
    public L64X128MixRandom(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 4, 2);
        this.a = data[0] | 1L;
        this.s = data[1];
        this.x0 = data[2];
        this.x1 = data[3];
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
        return new L64X128MixRandom(brine << 1, source.nextLong(),
                source.nextLong(), source.nextLong());
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
