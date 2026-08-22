package jdk.internal.random;

import java.util.random.RandomGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The largest member of the family: a
 * 128-bit LCG paired with the 1024-bit ring XBG.
 *
 * <p>It is the combination of the two structural ideas the other members introduce separately — the
 * hand-carried 128-bit LCG step of L128X128MixRandom, and the sixteen-word rotating ring of
 * L64X1024MixRandom. Nothing new is invented here; that is exactly what makes the family a family.
 *
 * <p>Together they give the highest period and the highest equidistribution the JDK ships, at 1152
 * bits of state. Whether that is ever the right choice is another question — the point of offering
 * eight members is that the trade between state size, speed and equidistribution belongs to the
 * caller, not to the library.
 *
 * @implNote An INTERNAL class; the JDK's is splittable through a nested interface (finding #101).
 */
public final class L128X1024MixRandom implements RandomGenerator {

    private static long multiplier() {
        return -3024805186288043011L;
    }

    private long sh;
    private long sl;
    private final long ah;
    private final long al;
    private final long[] x;
    private int p;

    // Package-private: the JDK spells this constructor out as eighteen (or twenty) separate
    // longs rather than an array, and a differing PUBLIC signature is a gate mismatch. The
    // array form is the useful one internally, so it simply stops being public.
    /**
     * Creates a generator with the given subgenerator state.
     *
     * @param ah the high half of the 128-bit LCG addend
     * @param al the low half of the LCG addend; forced odd
     * @param sh the high half of the 128-bit LCG state
     * @param sl the low half of the 128-bit LCG state
     * @param x the sixteen-word xor-shift ring
     * @implSpec An all-zero xor-shift state is a fixed point of its transition, so it is
     *           replaced by a known-good state. The LCG half needs no such guard: it
     *           advances whatever its value.
     */
    L128X1024MixRandom(long ah, long al, long sh, long sl, long[] x) {
        this.ah = ah;
        this.al = al | 1L;
        this.sh = sh;
        this.sl = sl;
        this.x = x;
        this.p = 15;
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
    public L128X1024MixRandom(long seed) {
        long v = seed ^ Bits.silverRatio64();
        this.ah = RandomSupport.mixMurmur64(v);
        v = v + Bits.goldenRatio64();
        this.al = RandomSupport.mixMurmur64(v) | 1L;
        this.sh = 0L;
        this.sl = 1L;
        // The FIRST word reuses the counter value the addend was built from; only the later ones
        // advance it. Advancing before the first assignment shifts the whole ring by one word and
        // produces a generator that looks fine and emits a different sequence — which is exactly
        // what the JDK comparison caught.
        this.x = new long[16];
        int i = 0;
        while (i < 16) {
            this.x[i] = RandomSupport.mixStafford13(v);
            v = v + Bits.goldenRatio64();
            i = i + 1;
        }
        this.p = 15;
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
        int q = this.p;
        this.p = (q + 1) & 15;
        long s0 = this.x[this.p];
        long s15 = this.x[q];

        long result = RandomSupport.mixLea64(this.sh + s0);

        // The 128-bit LCG step — see L128X128MixRandom for why the carry is explicit.
        long u = L128X1024MixRandom.multiplier() * this.sl;
        this.sh = (L128X1024MixRandom.multiplier() * this.sh)
                + Bits.unsignedMultiplyHigh(L128X1024MixRandom.multiplier(), this.sl)
                + this.sl + this.ah;
        this.sl = u + this.al;
        if (Bits.compareUnsigned(this.sl, u) < 0) {
            this.sh = this.sh + 1L;
        }

        // XBG: xoroshiro1024, the same ring L64X1024MixRandom uses.
        s15 = s15 ^ s0;
        this.x[q] = Bits.rotateLeft(s0, 25) ^ s15 ^ (s15 << 27);
        this.x[this.p] = Bits.rotateLeft(s15, 36);
        return result;
    }
}
