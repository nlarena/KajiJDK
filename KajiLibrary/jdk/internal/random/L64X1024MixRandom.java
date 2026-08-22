package jdk.internal.random;

import java.util.random.RandomGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The largest XBG in the LXM family: 1024 bits
 * of xor-shift state, held as SIXTEEN longs with a rotating index.
 *
 * <p>The structural change is the interesting part. The 128- and 256-bit versions keep their state in
 * named fields and touch every word on every step. At 1024 bits that would be wasteful, so
 * xoroshiro1024 keeps a RING: each step reads two words — the one at the cursor and the one just
 * behind it — updates only those two, and advances the cursor. A full sweep of the ring takes 16
 * steps, and that is what carries the state's memory forward.
 *
 * <p>{@code p} is the cursor, masked with 15 rather than reduced modulo 16 because the ring size is a power
 * of two — the same trick a circular buffer uses.
 *
 * <p>This is the highest-equidistribution member of the family. An INTERNAL class; the JDK's version
 * is splittable through a nested interface (finding #101), so {@code split} is omitted here.
 */
public final class L64X1024MixRandom implements RandomGenerator {

    private static long multiplier() {
        return -3372029247567499371L;
    }

    private long s;
    private final long a;
    private final long[] x;
    private int p;

    // Package-private: the JDK spells this constructor out as eighteen (or twenty) separate
    // longs rather than an array, and a differing PUBLIC signature is a gate mismatch. The
    // array form is the useful one internally, so it simply stops being public.
    /**
     * Creates a generator with the given subgenerator state.
     *
     * @param a the LCG addend; forced odd, which is what gives the LCG its full period
     * @param s the initial LCG state
     * @param x the sixteen-word xor-shift ring
     * @implSpec An all-zero xor-shift state is a fixed point of its transition, so it is
     *           replaced by a known-good state. The LCG half needs no such guard: it
     *           advances whatever its value.
     */
    L64X1024MixRandom(long a, long s, long[] x) {
        this.a = a | 1L;
        this.s = s;
        this.x = x;
        this.p = 15;
    }

    // The seed is expanded into sixteen words, each a further step of the golden-ratio counter.
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
    public L64X1024MixRandom(long seed) {
        long v = seed ^ Bits.silverRatio64();
        this.a = RandomSupport.mixMurmur64(v) | 1L;
        this.s = 1L;
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
        // Advance the cursor first: the word it now points at is this step's "s0", and the one it
        // pointed at before is "s15" — the far end of the ring.
        this.p = (q + 1) & 15;
        long s0 = this.x[this.p];
        long s15 = this.x[q];

        long result = RandomSupport.mixLea64(this.s + s0);

        this.s = L64X1024MixRandom.multiplier() * this.s + this.a;

        s15 = s15 ^ s0;
        this.x[q] = Bits.rotateLeft(s0, 25) ^ s15 ^ (s15 << 27);
        this.x[this.p] = Bits.rotateLeft(s15, 36);
        return result;
    }
}
