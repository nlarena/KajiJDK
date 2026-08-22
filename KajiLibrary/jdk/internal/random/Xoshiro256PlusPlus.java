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
public final class Xoshiro256PlusPlus implements RandomGenerator {

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
}
