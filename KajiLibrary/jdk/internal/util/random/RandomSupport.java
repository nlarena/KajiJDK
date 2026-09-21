package jdk.internal.util.random;

import java.util.random.RandomGenerator;

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

    // ---- the error messages, shared -------------------------------------------------------------
    //
    // They are `public` because the JDK exposes them: each generator uses them when validating its
    // arguments, and their being here is what makes the message **the same** wherever it comes
    // from.

    /** The one of an upper bound that is not positive. */
    public static final String BAD_BOUND = "bound must be positive";

    /** The one of an empty or inverted range. */
    public static final String BAD_RANGE = "bound must be greater than origin";

    /** The one of a negative stream size. */
    public static final String BAD_SIZE = "size must be non-negative";

    /** The one of a jump distance that is not finite and positive. */
    public static final String BAD_DISTANCE =
            "jump distance must be finite, positive, and an exact integer";

    /** The one of a floating point bound that is not finite and positive. */
    public static final String BAD_FLOATING_BOUND = "bound must be finite and positive";

    // ---- the two irrational constants -----------------------------------------------------------
    //
    // They are the first bits of the fractional part of two irrationals --the golden ratio and the
    // silver ratio-- rounded to odd. That they are irrational is the point: as the increment of a
    // counter, a number whose binary expansion has no period makes the successive states not fall
    // into a pattern; and their being odd makes them invertible modulo two to the n, with which no
    // pair of different counters collides.

    /** The golden ratio in 32 bits. */
    public static final int GOLDEN_RATIO_32 = 0x9e3779b9;

    /** The golden ratio in 64 bits. */
    public static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;

    /** The silver ratio in 32 bits. */
    public static final int SILVER_RATIO_32 = 0x6A09E667;

    /** The silver ratio in 64 bits. */
    public static final long SILVER_RATIO_64 = 0x6A09E667F3BCC909L;

    // `protected` and not `private`: it is what the JDK declares. A class of static utilities is
    // not instantiated, but leaving it protected allows a subclass to exist, which is the
    // difference between "it makes no sense" and "it is forbidden".
    protected RandomSupport() {
    }

    // ---- validation of arguments ----------------------------------------------------------------

    /** @throws IllegalArgumentException if the bound is not positive */
    public static void checkBound(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
    }

    /** @throws IllegalArgumentException if the bound is not positive */
    public static void checkBound(long bound) {
        if (bound <= 0L) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
    }

    /** @throws IllegalArgumentException if the bound is not finite and positive */
    public static void checkBound(float bound) {
        if (!(0.0f < bound && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_FLOATING_BOUND);
        }
    }

    /** @throws IllegalArgumentException if the bound is not finite and positive */
    public static void checkBound(double bound) {
        if (!(0.0d < bound && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_FLOATING_BOUND);
        }
    }

    /** @throws IllegalArgumentException if the range is empty or inverted */
    public static void checkRange(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /** @throws IllegalArgumentException if the range is empty or inverted */
    public static void checkRange(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    // The two floating point ones also ask for both ends to be **finite**. The negated form is not
    // an ornament: it also catches the NaN, which is neither greater nor smaller than anything and
    // would slip through with the direct comparison.

    /** @throws IllegalArgumentException if the range is empty, inverted, or not finite */
    public static void checkRange(float origin, float bound) {
        if (!(Float.NEGATIVE_INFINITY < origin && origin < bound
                && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /** @throws IllegalArgumentException if the range is empty, inverted, or not finite */
    public static void checkRange(double origin, double bound) {
        if (!(Double.NEGATIVE_INFINITY < origin && origin < bound
                && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /** @throws IllegalArgumentException if the size of the stream is negative */
    public static void checkStreamSize(long streamSize) {
        if (streamSize < 0L) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    }

    // ---- bounded values -------------------------------------------------------------------------
    //
    // The four integer ones share the same idea, which is the only interesting thing about this
    // block: taking the remainder of a uniform value **biases** the result when the range does not
    // divide the space, so the over-represented candidates have to be **rejected** and thrown
    // again. A range that is a power of two does not have that problem and is resolved with a mask.
    //
    // The loop of rejection has an uncomfortable shape on purpose --the work is in the condition--
    // and it is that of the JDK: as the first candidate is already available, one has to leave from
    // the middle.

    /** A uniform int in the range from zero to bound, not including it. */
    public static int boundedNextInt(RandomGenerator rng, int bound) {
        final int m = bound - 1;
        int r = rng.nextInt();
        if ((bound & m) == 0) {
            r &= m;
        } else {
            for (int u = r >>> 1; u + m - (r = u % bound) < 0; u = rng.nextInt() >>> 1) {
                continue;
            }
        }
        return r;
    }

    /** A uniform int in the range from origin to bound, not including it. */
    public static int boundedNextInt(RandomGenerator rng, int origin, int bound) {
        int r = rng.nextInt();
        if (origin < bound) {
            final int n = bound - origin;
            final int m = n - 1;
            if ((n & m) == 0) {
                r = (r & m) + origin;
            } else if (n > 0) {
                for (int u = r >>> 1; u + m - (r = u % n) < 0; u = rng.nextInt() >>> 1) {
                    continue;
                }
                r = r + origin;
            } else {
                // The width of the range does not fit in an int: there is no arithmetic that
                // serves, it is thrown until it hits.
                while (r < origin || r >= bound) {
                    r = rng.nextInt();
                }
            }
        }
        return r;
    }

    /** A uniform long in the range from zero to bound, not including it. */
    public static long boundedNextLong(RandomGenerator rng, long bound) {
        final long m = bound - 1L;
        long r = rng.nextLong();
        if ((bound & m) == 0L) {
            r &= m;
        } else {
            for (long u = r >>> 1; u + m - (r = u % bound) < 0L; u = rng.nextLong() >>> 1) {
                continue;
            }
        }
        return r;
    }

    /** A uniform long in the range from origin to bound, not including it. */
    public static long boundedNextLong(RandomGenerator rng, long origin, long bound) {
        long r = rng.nextLong();
        if (origin < bound) {
            final long n = bound - origin;
            final long m = n - 1L;
            if ((n & m) == 0L) {
                r = (r & m) + origin;
            } else if (n > 0L) {
                for (long u = r >>> 1; u + m - (r = u % n) < 0L; u = rng.nextLong() >>> 1) {
                    continue;
                }
                r = r + origin;
            } else {
                while (r < origin || r >= bound) {
                    r = rng.nextLong();
                }
            }
        }
        return r;
    }

    // The floating point ones scale and then **correct**: multiplying may round right up to the
    // bound, and the bound is exclusive. Without the correction, the bound comes out every so
    // often.

    /** A uniform double in the range from zero to bound, not including it. */
    public static double boundedNextDouble(RandomGenerator rng, double bound) {
        double r = rng.nextDouble();
        r = r * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    /** A uniform double in the range from origin to bound, not including it. */
    public static double boundedNextDouble(RandomGenerator rng, double origin, double bound) {
        double r = rng.nextDouble();
        if (origin < bound) {
            if (bound - origin < Double.POSITIVE_INFINITY) {
                r = r * (bound - origin) + origin;
            } else {
                // The width does not fit in a double: it is scaled to half and doubled at the end.
                double halfOrigin = 0.5d * origin;
                r = (r * (0.5d * bound - halfOrigin) + halfOrigin) * 2.0d;
            }
            if (r >= bound) {
                r = Math.nextDown(bound);
            }
        }
        return r;
    }

    /** A uniform float in the range from zero to bound, not including it. */
    public static float boundedNextFloat(RandomGenerator rng, float bound) {
        float r = rng.nextFloat();
        r = r * bound;
        if (r >= bound) {
            r = Math.nextDown(bound);
        }
        return r;
    }

    /** A uniform float in the range from origin to bound, not including it. */
    public static float boundedNextFloat(RandomGenerator rng, float origin, float bound) {
        float r = rng.nextFloat();
        if (origin < bound) {
            if (bound - origin < Float.POSITIVE_INFINITY) {
                r = r * (bound - origin) + origin;
            } else {
                float halfOrigin = 0.5f * origin;
                r = (r * (0.5f * bound - halfOrigin) + halfOrigin) * 2.0f;
            }
            if (r >= bound) {
                r = Math.nextDown(bound);
            }
        }
        return r;
    }

    // ---- seeds ----------------------------------------------------------------------------------

    /**
     * An initial seed, different on each call.
     *
     * <p>It mixes the wall clock with the high resolution one **separately** and then combines
     * them: the two on their own are predictable --the first one advances in milliseconds, the
     * second one starts at an arbitrary origin-- and what each one brings is different.
     */
    public static long initialSeed() {
        return mixStafford13(System.currentTimeMillis()) ^ mixStafford13(System.nanoTime());
    }

    /**
     * It turns a seed of bytes of any length into n long values, guaranteeing that the last z are
     * not all zero.
     *
     * <p>The three steps answer three different problems, and it is as well not to confuse them:
     * packing the bytes there are; **filling in** with a generator if they are not enough (a short
     * seed would leave the rest at zero, which is a poor state); and guaranteeing that the tail is
     * not all zero, because for a xor-shift generator zero is a **fixed point** -- it stays there
     * forever.
     *
     * <p>The and with the one's complement of the last part is not decorative: it covers the case
     * of z equal to one, where one has to make sure that the first generated value is not zero.
     */
    public static long[] convertSeedBytesToLongs(byte[] seed, int n, int z) {
        final long[] result = new long[n];
        final int m = Math.min(seed.length, n << 3);
        int j = 0;
        while (j < m) {
            result[j >> 3] = (result[j >> 3] << 8) | (long) (seed[j] & 0xFF);
            j = j + 1;
        }
        long v = result[0];
        j = (m + 7) >> 3;
        while (j < n) {
            v = v + SILVER_RATIO_64;
            result[j] = mixMurmur64(v);
            j = j + 1;
        }
        boolean someNonZero = false;
        j = n - z;
        while (j < n) {
            if (result[j] != 0L) {
                someNonZero = true;
            }
            j = j + 1;
        }
        if (!someNonZero) {
            long w = result[0] & ~1L;
            j = n - z;
            while (j < n) {
                w = w + SILVER_RATIO_64;
                result[j] = mixMurmur64(w);
                j = j + 1;
            }
        }
        return result;
    }

    /** The 32-bit twin of {@link #convertSeedBytesToLongs}. */
    public static int[] convertSeedBytesToInts(byte[] seed, int n, int z) {
        final int[] result = new int[n];
        final int m = Math.min(seed.length, n << 2);
        int j = 0;
        while (j < m) {
            result[j >> 2] = (result[j >> 2] << 8) | (seed[j] & 0xFF);
            j = j + 1;
        }
        int v = result[0];
        j = (m + 3) >> 2;
        while (j < n) {
            v = v + SILVER_RATIO_32;
            result[j] = mixMurmur32(v);
            j = j + 1;
        }
        boolean someNonZero = false;
        j = n - z;
        while (j < n) {
            if (result[j] != 0) {
                someNonZero = true;
            }
            j = j + 1;
        }
        if (!someNonZero) {
            int w = result[0] & ~1;
            j = n - z;
            while (j < n) {
                w = w + SILVER_RATIO_32;
                result[j] = mixMurmur32(w);
                j = j + 1;
            }
        }
        return result;
    }

    // ---- the two non-uniform distributions ------------------------------------------------------
    //
    // They **delegate to the generator**, and that is a difference with the JDK that is as well
    // said outright: the JDK calculates them with McFarland's modified ziggurat, which is two
    // generated tables of several hundred entries. This library uses the polar method for the
    // normal one and the inverse transform for the exponential one -- **the distribution is the
    // right one, the sequence is not the same**.
    //
    // It is the same difference `RandomGenerator.nextGaussian` and `nextExponential` already have,
    // and it is written there as well. Delegating is what keeps it in **one single place**: if the
    // ziggurat ever comes in, it comes in once.

    /** A value of a standard normal. See the note above about the sequence. */
    public static double computeNextGaussian(RandomGenerator rng) {
        return rng.nextGaussian();
    }

    /** A value of an exponential of mean 1. See the note above. */
    public static double computeNextExponential(RandomGenerator rng) {
        return rng.nextExponential();
    }

    /**
     * The same as {@link #computeNextExponential}, with a **soft** cap at maxValue.
     *
     * <p>The cap exists in the JDK in order to bound the worst case of the ziggurat: it guarantees
     * that the minimum between the value and the cap has the right distribution with a number of
     * calls linear in the cap, and in order to achieve that it **may return a value greater** than
     * the cap. With no ziggurat there is no worst case to bound, so the cap changes nothing -- and
     * returning more than the cap is still allowed, which is just what "soft" means.
     */
    public static double computeNextExponentialSoftCapped(RandomGenerator rng, double maxValue) {
        return rng.nextExponential();
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
