package jdk.internal.random;

import java.util.random.RandomGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * A 128-bit LCG paired with xoshiro256.
 *
 * <p>This is where the family stops being a variation on a theme. The other members hold the LCG state
 * in one long and advance it with {@code s = M*s + a}, which the hardware does in one instruction. Here
 * the state is a PAIR (sh, sl) and the multiply has to be done by hand:
 *
 * <p>sl' = M*sl + al                            the low half, wrapping
 * sh' = M*sh + high64(M x sl) + sl + ah      the high half, plus the carry out of the low
 * if (sl' &lt;u sl_partial) sh'++               the addition's own carry
 *
 * <p>{@code high64(M x sl)} is the top 64 bits of an unsigned 128-bit product — the part Java's {@code *} throws
 * away. Reconstructing it from 32-bit halves is what Bits.unsignedMultiplyHigh does, and
 * it is the single reason this class is more than a copy of L64X128MixRandom.
 *
 * <p>Both carry detections must be UNSIGNED. Using a signed comparison would miss exactly the cases
 * where the sum crossed 2<sup>63</sup>, which is half of them.
 *
 * <p>This is the balanced member of the family: 128 bits of LCG against 256 bits of XBG, giving both
 * a long period and high equidistribution. The 128-bit LCG arithmetic is identical to
 * L128X128MixRandom's — see that class for why the carry has to be handled by hand — and only the
 * xor-shift half differs, which is the point of the family being built from swappable parts.
 *
 * @implNote An INTERNAL class; the JDK's is splittable through a nested interface (finding #101).
 */
public final class L128X256MixRandom implements RandomGenerator {

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
    private long x2;
    private long x3;

    /**
     * Creates a generator with the given subgenerator state.
     *
     * @param ah the high half of the 128-bit LCG addend
     * @param al the low half of the LCG addend; forced odd
     * @param sh the high half of the 128-bit LCG state
     * @param sl the low half of the 128-bit LCG state
     * @param x0 the first xor-shift state word
     * @param x1 the second xor-shift state word
     * @param x2 the third xor-shift state word
     * @param x3 the fourth xor-shift state word
     * @implSpec An all-zero xor-shift state is a fixed point of its transition, so it is
     *           replaced by a known-good state. The LCG half needs no such guard: it
     *           advances whatever its value.
     */
    public L128X256MixRandom(long ah, long al, long sh, long sl, long x0, long x1, long x2, long x3) {
        this.ah = ah;
        // Only the LOW half of the addend needs to be odd — that is what makes the 128-bit
        // increment odd as a whole.
        this.al = al | 1L;
        this.sh = sh;
        this.sl = sl;
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
     * @implSpec The seed is expanded through the mixing functions of
     *           {@link jdk.internal.util.random.RandomSupport}: a raw seed would leave a
     *           nearly-empty state that takes many steps to fill. The LCG addend and the
     *           xor-shift words come from DIFFERENT mixers, so no seed can make the two
     *           subgenerators agree.
     */
    public L128X256MixRandom(long seed) {
        this(RandomSupport.mixMurmur64(seed ^ Bits.silverRatio64()),
                RandomSupport.mixMurmur64((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64()),
                0L,
                1L,
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64() + Bits.goldenRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64() + Bits.goldenRatio64()
                        + Bits.goldenRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64() + Bits.goldenRatio64()
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
        long u = L128X256MixRandom.multiplier() * this.sl;
        this.sh = (L128X256MixRandom.multiplier() * this.sh)
                + Bits.unsignedMultiplyHigh(L128X256MixRandom.multiplier(), this.sl)
                + this.sl + this.ah;
        this.sl = u + this.al;
        // The low half wrapped exactly when the sum came out unsigned-smaller than the partial.
        if (Bits.compareUnsigned(this.sl, u) < 0) {
            this.sh = this.sh + 1L;
        }

        // XBG: xoshiro256, the same transition Xoshiro256PlusPlus and L64X256MixRandom use.
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
}
