package jdk.internal.random;

import java.util.random.RandomGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * Identical to L64X128MixRandom in state,
 * seeding and advance; the ONLY difference is the output scrambler.
 *
 * <p>Mix       mixLea64(s + x0)                  — two xor-shift/multiply rounds
 * StarStar  rotl((s + x0) * 5, 7) * 9         — multiply, rotate, multiply
 *
 * <p>The pair is worth keeping side by side because it isolates what a scrambler does. Both take the
 * same combined state and both produce a well-distributed 64 bits; "**" is cheaper (three
 * arithmetic ops against six) while the Lea mixer avalanches harder. The generator's PERIOD and its
 * equidistribution come from the state transition, which is the same here — the scrambler only
 * decides how well the output hides that structure.
 *
 * <p>The constants 5, 7 and 9 are not arbitrary either: multiplying by small odd numbers before and
 * after a rotation is the standard "**" construction from the xoshiro family.
 *
 * @implNote An INTERNAL class, same subset caveat as its sibling (nested SplittableGenerator, finding #101).
 */
public final class L64X128StarStarRandom implements RandomGenerator {

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
    public L64X128StarStarRandom(long a, long s, long x0, long x1) {
        this.a = a | 1L;
        this.s = s;
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
    public L64X128StarStarRandom(long seed) {
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
        long result = Bits.rotateLeft((this.s + this.x0) * 5L, 7) * 9L;

        this.s = L64X128StarStarRandom.multiplier() * this.s + this.a;

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
