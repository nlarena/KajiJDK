package jdk.internal.random;

import java.util.random.RandomGenerator;
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
public final class L64X128MixRandom implements RandomGenerator {

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
}
