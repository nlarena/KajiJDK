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
public final class Xoroshiro128PlusPlus implements RandomGenerator {

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
}
