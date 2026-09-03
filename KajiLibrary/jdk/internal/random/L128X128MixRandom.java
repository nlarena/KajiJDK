package jdk.internal.random;

import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The LXM family with a 128-BIT LCG.
 *
 * <p>This is where the family stops being a variation on a theme. The other members hold the LCG state
 * in one long and advance it with {@code s = M*s + a}, which the hardware does in one instruction. Here
 * the state is a PAIR (sh, sl) and the multiply has to be done by hand:
 *
 * <pre>
 *     sl' = M*sl + al                            the low half, wrapping
 *     sh' = M*sh + high64(M x sl) + sl + ah      the high half, plus the carry out of the low
 *     if (sl' &lt;u sl_partial) sh'++               the addition's own carry
 * </pre>
 *
 * <p>{@code high64(M x sl)} is the top 64 bits of an unsigned 128-bit product — the part Java's {@code *} throws
 * away. Reconstructing it from 32-bit halves is what Bits.unsignedMultiplyHigh does, and
 * it is the single reason this class is more than a copy of L64X128MixRandom.
 *
 * <p>Both carry detections must be UNSIGNED. Using a signed comparison would miss exactly the cases
 * where the sum crossed 2<sup>63</sup>, which is half of them.
 *
 * <p>What it buys: an LCG period of 2<sup>128</sup> instead of 2<sup>64</sup>, so the two subgenerators' periods are
 * comparable rather than one dwarfing the other.
 *
 * @implNote An INTERNAL class; the JDK's is splittable through a nested interface (finding #101).
 */
public final class L128X128MixRandom implements RandomGenerator.SplittableGenerator {

    // The 128-bit LCG multiplier's low half, 0xd605bbb58c8abbfd. (The high half is 1, implicit.)
    private static long multiplier() {
        return -3024805186288043011L;
    }

    private long sh;
    private long sl;
    private final long ah;
    private final long al;
    private long x0;
    private long x1;

    /**
     * Creates a generator with the given subgenerator state.
     *
     * @param ah the high half of the 128-bit LCG addend
     * @param al the low half of the LCG addend; forced odd
     * @param sh the high half of the 128-bit LCG state
     * @param sl the low half of the 128-bit LCG state
     * @param x0 the first xor-shift state word
     * @param x1 the second xor-shift state word
     * @implSpec An all-zero xor-shift state is a fixed point of its transition, so it is
     *           replaced by a known-good state. The LCG half needs no such guard: it
     *           advances whatever its value.
     */
    public L128X128MixRandom(long ah, long al, long sh, long sl, long x0, long x1) {
        this.ah = ah;
        // Only the LOW half of the addend needs to be odd — that is what makes the 128-bit
        // increment odd as a whole.
        this.al = al | 1L;
        this.sh = sh;
        this.sl = sl;
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
     * @implSpec The seed is expanded through the mixing functions of
     *           {@link jdk.internal.util.random.RandomSupport}: a raw seed would leave a
     *           nearly-empty state that takes many steps to fill. The LCG addend and the
     *           xor-shift words come from DIFFERENT mixers, so no seed can make the two
     *           subgenerators agree.
     */
    public L128X128MixRandom(long seed) {
        this(RandomSupport.mixMurmur64(seed ^ Bits.silverRatio64()),
                RandomSupport.mixMurmur64((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64()),
                0L,
                1L,
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64() + Bits.goldenRatio64()));
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
        long result = RandomSupport.mixLea64(this.sh + this.x0);

        // The 128-bit LCG step, in two halves with the carry threaded between them.
        long u = L128X128MixRandom.multiplier() * this.sl;
        this.sh = (L128X128MixRandom.multiplier() * this.sh)
                + Bits.unsignedMultiplyHigh(L128X128MixRandom.multiplier(), this.sl)
                + this.sl + this.ah;
        this.sl = u + this.al;
        // The low half wrapped exactly when the sum came out unsigned-smaller than the partial.
        if (Bits.compareUnsigned(this.sl, u) < 0) {
            this.sh = this.sh + 1L;
        }

        // XBG: xoroshiro128 v1_0, the same one L64X128MixRandom uses.
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
    public L128X128MixRandom() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Un generador sembrado desde bytes.
     *
     * <p>Los bytes se reparten en las palabras del estado y, si no alcanzan, el resto se rellena con
     * un generador auxiliar -- una semilla corta dejaria el estado casi en cero, que para un
     * xor-shift es un punto fijo. Lo hace {@link RandomSupport#convertSeedBytesToLongs}, con los
     * mismos 6 y 2 que usa el JDK para este algoritmo, asi que la misma semilla da el mismo
     * generador.
     */
    public L128X128MixRandom(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 6, 2);
        this.ah = data[0];
        this.al = data[1] | 1L;
        this.sh = data[2];
        this.sl = data[3];
        this.x0 = data[4];
        this.x1 = data[5];
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
        return new L128X128MixRandom(source.nextLong(), brine << 1,
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
