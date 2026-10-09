package jdk.internal.random;

import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
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
public final class L128X1024MixRandom implements RandomGenerator.SplittableGenerator {

    private static long multiplier() {
        return -3024805186288043011L;
    }

    private long sh;
    private long sl;
    private final long ah;
    private final long al;
    private final long[] x;
    // Initialised **in the declaration** and not in each constructor, which is how the JDK does it:
    // it is the index of the ring, and a constructor that forgets it starts one word displaced and
    // emits another sequence. That is exactly what happened with the byte[] one.
    private int p = 15;

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
    /**
     * The constructor of **explicit state**: each word of the generator is given by hand.
     *
     * <p>It is the one splitting uses, and that is why it exists: the other two derive the whole
     * state from a seed, and splitting needs precisely the opposite -- fixing the addend of the LCG
     * and drawing the rest.
     *
     * @implSpec The addend is forced odd (it is what makes the LCG walk the complete period), and
     *           an **all-zero** xor-shift state is replaced, because zero is a fixed point of its
     *           transition: the generator would stay there forever.
     */
    public L128X1024MixRandom(long ah, long al, long sh, long sl,
            long x0, long x1, long x2, long x3,
            long x4, long x5, long x6, long x7,
            long x8, long x9, long x10, long x11,
            long x12, long x13, long x14, long x15) {
        this.ah = ah;
        this.al = al | 1L;
        this.sh = sh;
        this.sl = sl;
        this.x = new long[16];
        this.x[0] = x0;
        this.x[1] = x1;
        this.x[2] = x2;
        this.x[3] = x3;
        this.x[4] = x4;
        this.x[5] = x5;
        this.x[6] = x6;
        this.x[7] = x7;
        this.x[8] = x8;
        this.x[9] = x9;
        this.x[10] = x10;
        this.x[11] = x11;
        this.x[12] = x12;
        this.x[13] = x13;
        this.x[14] = x14;
        this.x[15] = x15;
        if ((x0 | x1 | x2 | x3 | x4 | x5 | x6 | x7 | x8 | x9 | x10 | x11 | x12 | x13 | x14 | x15) == 0L) {
            // The sixteen at zero: they are filled in with the mixer, which guarantees
            // that at least fifteen of the sixteen come out different from zero.
            long v = this.sembradorInicial();
            int j = 0;
            while (j < 16) {
                v = v + RandomSupport.GOLDEN_RATIO_64;
                this.x[j] = RandomSupport.mixStafford13(v);
                j = j + 1;
            }
        }
        this.p = 15;
    }

    // Where the filling comes from when the xor-shift state arrives all at zero. It is the
    // low word of the LCG, which is the only thing different from zero that is at hand.
    private long sembradorInicial() {
        return this.sl;
    }

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

    // ---- the three entry points that were missing -----------------------------------------------

    // The seed of the generators with no arguments. It is a shared counter that advances by
    // GOLDEN_RATIO_64: two generators created one after the other do not start at neighbouring
    // states, which is the only thing asked of it.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** A generator with a seed chosen by itself, different on each call. */
    public L128X1024MixRandom() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * A generator seeded from bytes.
     *
     * <p>The bytes are spread over the words of the state and, if they are not enough, the rest is
     * filled in with an auxiliary generator -- a short seed would leave the state almost at zero,
     * which for a xor-shift is a fixed point. {@link RandomSupport#convertSeedBytesToLongs} does
     * it, with the same 20 and 16 the JDK uses for this algorithm, so the same seed gives the same
     * generator.
     */
    public L128X1024MixRandom(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 20, 16);
        this.ah = data[0];
        this.al = data[1] | 1L;
        this.sh = data[2];
        this.sl = data[3];
        this.x = new long[16];
        int j = 0;
        while (j < 16) {
            this.x[j] = data[4 + j];
            j = j + 1;
        }
    }

    // ---- splitting ------------------------------------------------------------------------------
    //
    // Splitting is not "seeding another one at random": two nearby seeds may give sequences that
    // overlap. The guarantee comes from the **addend** of the LCG --the `a`-- of the child being
    // taken from the brine and not from chance, with which each child walks a different orbit of
    // the same space.
    //
    // The shift `brine << 1` leaves the low bit free, which is where the constructor forces it odd.
    // Without that, half the brines would give the same addend.

    /** A generator independent of this one, with the entropy of this one. */
    public SplittableGenerator split() {
        return this.split(this);
    }

    /** A generator independent of this one, with the entropy of `source`. */
    public SplittableGenerator split(SplittableGenerator source) {
        return this.split(source, source.nextLong());
    }

    /** The one above with the brine made explicit: it is the one that does the work. */
    public SplittableGenerator split(SplittableGenerator source, long brine) {
        long[] w = new long[16];
        int j = 0;
        while (j < 16) {
            w[j] = source.nextLong();
            j = j + 1;
        }
        return new L128X1024MixRandom(source.nextLong(), brine << 1,
                source.nextLong(), source.nextLong(),
                w[0], w[1], w[2], w[3], w[4], w[5], w[6], w[7],
                w[8], w[9], w[10], w[11], w[12], w[13], w[14], w[15]);
    }

    /** `streamSize` independent generators, with the entropy of this one. */
    public java.util.stream.Stream<SplittableGenerator> splits(long streamSize) {
        return this.splits(streamSize, this);
    }

    /** `streamSize` independent generators, with the entropy of `source`. */
    public java.util.stream.Stream<SplittableGenerator> splits(long streamSize,
            SplittableGenerator source) {
        return Splits.de(this, streamSize, source);
    }
}
