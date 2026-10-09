package jdk.internal.random;

import java.util.random.RandomGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * The 128-bit sibling of {@link Xoshiro256PlusPlus}: same family, half the state, period
 * 2<sup>128</sup>&nbsp;&minus;&nbsp;1.
 *
 * <p>The name states the difference. <em>xoro</em> is xor/<b>rotate</b>, <em>xosh</em> is
 * xor/<b>shift</b>. With only two state words there is no room for a shift-based transition that
 * still mixes every word, so this one rotates the words into each other instead:
 *
 * <pre>
 *     x1 ^= x0
 *     x0  = rotl(x0, 49) ^ x1 ^ (x1 &lt;&lt; 21)
 *     x1  = rotl(x1, 28)
 * </pre>
 *
 * <p>Every step touches both words. The three constants are search-tuned: changing 49, 21 or 28
 * does not give "a slightly different generator", it gives a worse one.
 *
 * <p>Note that the xoroshiro128 used <em>inside</em> the LXM generators is a different variant,
 * with rotations (24, 16, 37). Same family, different tuning; they are not interchangeable.
 *
 * @implNote An internal class. A KajiLibrary subset: the JDK's implements the nested interface
 *           {@code RandomGenerator.LeapableGenerator}, which does not resolve (finding #101), so
 *           {@code jump}/{@code leap} are omitted.
 */
public final class Xoroshiro128PlusPlus implements RandomGenerator.LeapableGenerator {

    private long x0;
    private long x1;

    /**
     * Creates a generator with the given state words.
     *
     * @param x0 the first state word
     * @param x1 the second state word
     * @implSpec An all-zero state is a fixed point, so it is replaced by a known-good state.
     */
    public Xoroshiro128PlusPlus(long x0, long x1) {
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
     */
    public Xoroshiro128PlusPlus(long seed) {
        this(RandomSupport.mixStafford13(seed ^ Bits.silverRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64()));
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec The {@code ++} scrambler is the same idea as its bigger sibling's, with its own
     *           rotation: {@code rotl(x0 + x1, 17) + x0}.
     */
    public long nextLong() {
        long s0 = this.x0;
        long s1 = this.x1;
        long result = Bits.rotateLeft(s0 + s1, 17) + s0;

        s1 = s1 ^ s0;
        this.x0 = Bits.rotateLeft(s0, 49) ^ s1 ^ (s1 << 21);
        this.x1 = Bits.rotateLeft(s1, 28);
        return result;
    }

    // ---- the entry points that were missing -----------------------------------------------------

    // The seed of the generators with no arguments: a shared counter that advances by
    // GOLDEN_RATIO_64, so that two generators in a row do not start at neighbouring states.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** A generator with a seed chosen by itself, different on each call. */
    public Xoroshiro128PlusPlus() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * A generator seeded from bytes.
     *
     * <p>The 2 values that come out of the seed **cannot be all zero**: for a xor-shift zero is a
     * fixed point, and the generator would stay there. `RandomSupport.convertSeedBytesToLongs`
     * guarantees it, with the same parameters the JDK uses.
     */
    public Xoroshiro128PlusPlus(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 2, 2);
        this.x0 = data[0];
        this.x1 = data[1];
    }

    // ---- jump and long jump ---------------------------------------------------------------------
    //
    // The two tables are the **jump polynomials** of the algorithm: each bit at one says that the
    // state has to be accumulated at that step. Walking them is equivalent to advancing
    // `jumpDistance()` values, and that equivalence is what makes two threads that start a jump
    // apart walk stretches that **do not overlap** -- it is not a statistical guarantee but an
    // arithmetic one.
    //
    // The numbers are not tunable: they are the published ones for this generator.

    private static final long[] JUMP_TABLE = { 0x2bd7a6a6e99c2ddcL, 0x0992ccaf6a6fca05L };

    private static final long[] LONG_JUMP_TABLE = { 0x360fd5f2cf8d5d99L, 0x9c6e6877736c46e3L };

    /** A copy of this generator, in the same state. */
    public Xoroshiro128PlusPlus copy() {
        return new Xoroshiro128PlusPlus(this.x0, this.x1);
    }

    /** It advances this generator two to the 64 values. */
    public void jump() {
        this.jumpUsing(JUMP_TABLE);
    }

    /** It advances this generator two to the 96 values. */
    public void leap() {
        this.jumpUsing(LONG_JUMP_TABLE);
    }

    /** How many values {@link #jump()} advances. */
    public double jumpDistance() {
        return Math.scalb(1.0d, 64);
    }

    /** How many values {@link #leap()} advances. */
    public double leapDistance() {
        return Math.scalb(1.0d, 96);
    }

    // The jump algorithm: the generator is advanced 64 times per word of the table, accumulating
    // the state at the steps the table marks. At the end the accumulator **is** the state the
    // generator would have had after the jump distance.
    private void jumpUsing(long[] table) {
        long s0 = 0L;
        long s1 = 0L;
        int i = 0;
        while (i < table.length) {
            int b = 0;
            while (b < 64) {
                if ((table[i] & (1L << b)) != 0L) {
                    s0 ^= this.x0;
                    s1 ^= this.x1;
                }
                this.nextLong();
                b = b + 1;
            }
            i = i + 1;
        }
        this.x0 = s0;
        this.x1 = s1;
    }
}
