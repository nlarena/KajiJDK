package jdk.internal.util.random;

/**
 * The bit-mixing functions every modern generator in {@code java.util.random} seeds itself with.
 *
 * <p>Mixing is needed because a generator's quality is a property of its STATE TRANSITION, not of
 * its starting point. Seed a shift-register generator with {@code 1} and its first outputs carry
 * almost no entropy — the state is nearly all zeros and it takes many steps to fill up. So the seed
 * is first run through a bijective avalanche function that spreads one changed input bit over all
 * 64 output bits, and the result becomes the state.
 *
 * <p>Every mixer here has the same shape: xor the value with a shifted copy of itself, multiply by
 * an odd constant, repeat. The xor-shift spreads bits downward, the multiply spreads them upward,
 * and alternating the two is what makes every input bit reach every output bit. Built only from
 * xor, shift and odd multiplication, each one is a BIJECTION — no two seeds can collide.
 *
 * <p>The constants and shift distances are not tunable. They are the published, search-tuned
 * values, and they were extracted from the JDK's own bytecode rather than transcribed.
 *
 * @implNote An internal package, not part of the public API. It exists here because the generators
 *           in {@code jdk.internal.random} need it, exactly as in the JDK.
 */
public final class RandomSupport {

    private RandomSupport() {
    }

    /**
     * Stafford's variant 13 of the MurmurHash3 finalizer.
     *
     * <p>This is the mixer used to expand a seed into generator state throughout
     * {@code java.util.random}.
     *
     * @param z the value to mix
     * @return the mixed value
     */
    public static long mixStafford13(long z) {
        z = (z ^ (z >>> 30)) * -4658895280553007687L;
        z = (z ^ (z >>> 27)) * -7723592293110705685L;
        return z ^ (z >>> 31);
    }

    /**
     * Doug Lea's 64-bit mixer: the same constant twice, with 32-bit shifts.
     *
     * <p>Unlike the others this one is on the HOT PATH rather than only in seeding — the LXM
     * generators run every output through it.
     *
     * @param z the value to mix
     * @return the mixed value
     */
    public static long mixLea64(long z) {
        z = (z ^ (z >>> 32)) * -2685821657736338717L;
        z = (z ^ (z >>> 32)) * -2685821657736338717L;
        return z ^ (z >>> 32);
    }

    /**
     * The original MurmurHash3 64-bit finalizer.
     *
     * @param z the value to mix
     * @return the mixed value
     */
    public static long mixMurmur64(long z) {
        z = (z ^ (z >>> 33)) * -49064778989728563L;
        z = (z ^ (z >>> 33)) * -4265267296055464877L;
        return z ^ (z >>> 33);
    }

    /**
     * The 32-bit counterpart of {@link #mixLea64}, for generators whose state is int-sized.
     *
     * @param z the value to mix
     * @return the mixed value
     * @implSpec Same shape as the 64-bit version, with the shifts and constant retuned for half
     *           the width.
     */
    public static int mixLea32(int z) {
        z = (z ^ (z >>> 16)) * -747796405;
        z = (z ^ (z >>> 16)) * -747796405;
        return z ^ (z >>> 16);
    }

    /**
     * The 32-bit counterpart of {@link #mixMurmur64}.
     *
     * @param z the value to mix
     * @return the mixed value
     */
    public static int mixMurmur32(int z) {
        z = (z ^ (z >>> 16)) * -2048144789;
        z = (z ^ (z >>> 13)) * -1028477387;
        return z ^ (z >>> 16);
    }
}
