package jdk.internal.random;

import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The largest XBG in the LXM family: 1024 bits
 * of xor-shift state, held as SIXTEEN longs with a rotating index.
 *
 * <p>The structural change is the interesting part. The 128- and 256-bit versions keep their state
 * in named fields and touch every word on every step. At 1024 bits that would be wasteful, so
 * xoroshiro1024 keeps a RING: each step reads two words — the one at the cursor and the one just
 * behind it — updates only those two, and advances the cursor. A full sweep of the ring takes 16
 * steps, and that is what carries the state's memory forward.
 *
 * <p>{@code p} is the cursor, masked with 15 rather than reduced modulo 16 because the ring size is
 * a power of two — the same trick a circular buffer uses.
 *
 * <p>This is the highest-equidistribution member of the family. An INTERNAL class; the JDK's
 * version is splittable through a nested interface (finding #101), so {@code split} is omitted
 * here.
 */
public final class L64X1024MixRandom implements RandomGenerator.SplittableGenerator {

    private static long multiplier() {
        return -3372029247567499371L;
    }

    private long s;
    private final long a;
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
    public L64X1024MixRandom(long a, long s,
            long x0, long x1, long x2, long x3,
            long x4, long x5, long x6, long x7,
            long x8, long x9, long x10, long x11,
            long x12, long x13, long x14, long x15) {
        this.a = a | 1L;
        this.s = s;
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
        return this.s;
    }

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

    // ---- the three entry points that were missing -----------------------------------------------

    // The seed of the generators with no arguments. It is a shared counter that advances by
    // GOLDEN_RATIO_64: two generators created one after the other do not start at neighbouring
    // states, which is the only thing asked of it.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** A generator with a seed chosen by itself, different on each call. */
    public L64X1024MixRandom() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * A generator seeded from bytes.
     *
     * <p>The bytes are spread over the words of the state and, if they are not enough, the rest is
     * filled in with an auxiliary generator -- a short seed would leave the state almost at zero,
     * which for a xor-shift is a fixed point. {@link RandomSupport#convertSeedBytesToLongs} does
     * it, with the same 18 and 16 the JDK uses for this algorithm, so the same seed gives the same
     * generator.
     */
    public L64X1024MixRandom(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 18, 16);
        this.a = data[0] | 1L;
        this.s = data[1];
        this.x = new long[16];
        int j = 0;
        while (j < 16) {
            this.x[j] = data[2 + j];
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
        return new L64X1024MixRandom(brine << 1, source.nextLong(),
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
