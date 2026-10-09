package jdk.internal.random;

import java.util.random.RandomGenerator;
import jdk.internal.util.random.RandomSupport;

/**
 * A xor/shift/rotate generator with 256 bits of state and a period of
 * 2<sup>256</sup>&nbsp;&minus;&nbsp;1.
 *
 * <p>Where {@link java.util.Random} advances by multiplication modulo 2<sup>48</sup> — so its state
 * <em>is</em> a number, and neighbouring seeds produce visibly related streams — this generator
 * advances by xors, shifts and rotations. Pure bit stirring, no carry chain: much faster to step
 * and a far longer period, at the cost of needing a well-mixed seed to start.
 *
 * <p>The {@code ++} in the name is the <em>scrambler</em>. The raw state is never returned;
 * {@code rotl(x0 + x3, 23) + x0} takes two state words, adds, rotates and adds again, which fixes
 * the weak low bits the plain {@code +} variant is known for. The state transition and the output
 * function are independent designs, and only the second one changes between the {@code +},
 * {@code ++} and {@code **} members of the family.
 *
 * @implNote An internal class: {@code java.util.random} exposes only {@link RandomGenerator} and
 *           {@code RandomGeneratorFactory}.
 * @implNote A KajiLibrary subset. The JDK's version implements the nested interface
 *           {@code RandomGenerator.LeapableGenerator} — jump and leap to a distant point in the
 *           stream — which a nested type cannot resolve to (finding #101), so this one implements
 *           the top-level {@link RandomGenerator} and omits {@code jump}/{@code leap}.
 */
public final class Xoshiro256PlusPlus implements RandomGenerator.LeapableGenerator {

    private long x0;
    private long x1;
    private long x2;
    private long x3;

    /**
     * Creates a generator with the given state words.
     *
     * @param x0 the first state word
     * @param x1 the second state word
     * @param x2 the third state word
     * @param x3 the fourth state word
     * @implSpec An all-zero state is a fixed point of the transition — it would emit zeros forever
     *           — so it is replaced by a known-good state.
     */
    public Xoshiro256PlusPlus(long x0, long x1, long x2, long x3) {
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
     * @implSpec The seed is expanded into four words, each a further mix of a counter advanced by
     *           the golden ratio. The mixing matters: seeding a shift-register generator directly
     *           with a small number leaves a state that is nearly all zeros, which takes many steps
     *           to fill and produces poor early output.
     */
    public Xoshiro256PlusPlus(long seed) {
        this(RandomSupport.mixStafford13(seed ^ Bits.silverRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64() + Bits.goldenRatio64()),
                RandomSupport.mixStafford13((seed ^ Bits.silverRatio64())
                        + Bits.goldenRatio64() + Bits.goldenRatio64()
                        + Bits.goldenRatio64()));
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec The scrambler reads the state <em>before</em> it advances, so the returned value
     *           and the next state are computed from the same snapshot.
     */
    public long nextLong() {
        long result = Bits.rotateLeft(this.x0 + this.x3, 23) + this.x0;

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

    // ---- the entry points that were missing -----------------------------------------------------

    // The seed of the generators with no arguments: a shared counter that advances by
    // GOLDEN_RATIO_64, so that two generators in a row do not start at neighbouring states.
    private static final java.util.concurrent.atomic.AtomicLong SEMILLERO =
            new java.util.concurrent.atomic.AtomicLong(RandomSupport.initialSeed());

    /** A generator with a seed chosen by itself, different on each call. */
    public Xoshiro256PlusPlus() {
        this(SEMILLERO.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * A generator seeded from bytes.
     *
     * <p>The 4 values that come out of the seed **cannot be all zero**: for a xor-shift zero is a
     * fixed point, and the generator would stay there. `RandomSupport.convertSeedBytesToLongs`
     * guarantees it, with the same parameters the JDK uses.
     */
    public Xoshiro256PlusPlus(byte[] seed) {
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 4, 4);
        this.x0 = data[0];
        this.x1 = data[1];
        this.x2 = data[2];
        this.x3 = data[3];
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

    private static final long[] JUMP_TABLE = { 0x180ec6d33cfd0abaL, 0xd5a61266f0c9392cL, 0xa9582618e03fc9aaL, 0x39abdc4529b1661cL };

    private static final long[] LONG_JUMP_TABLE = { 0x76e15d3efefdcbbfL, 0xc5004e441c522fb3L, 0x77710069854ee241L, 0x39109bb02acbe635L };

    /** A copy of this generator, in the same state. */
    public Xoshiro256PlusPlus copy() {
        return new Xoshiro256PlusPlus(this.x0, this.x1, this.x2, this.x3);
    }

    /** It advances this generator two to the 128 values. */
    public void jump() {
        this.jumpUsing(JUMP_TABLE);
    }

    /** It advances this generator two to the 192 values. */
    public void leap() {
        this.jumpUsing(LONG_JUMP_TABLE);
    }

    /** How many values {@link #jump()} advances. */
    public double jumpDistance() {
        return Math.scalb(1.0d, 128);
    }

    /** How many values {@link #leap()} advances. */
    public double leapDistance() {
        return Math.scalb(1.0d, 192);
    }

    // The jump algorithm: the generator is advanced 64 times per word of the table, accumulating
    // the state at the steps the table marks. At the end the accumulator **is** the state the
    // generator would have had after the jump distance.
    private void jumpUsing(long[] table) {
        long s0 = 0L;
        long s1 = 0L;
        long s2 = 0L;
        long s3 = 0L;
        int i = 0;
        while (i < table.length) {
            int b = 0;
            while (b < 64) {
                if ((table[i] & (1L << b)) != 0L) {
                    s0 ^= this.x0;
                    s1 ^= this.x1;
                    s2 ^= this.x2;
                    s3 ^= this.x3;
                }
                this.nextLong();
                b = b + 1;
            }
            i = i + 1;
        }
        this.x0 = s0;
        this.x1 = s1;
        this.x2 = s2;
        this.x3 = s3;
    }
}
