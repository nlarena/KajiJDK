package java.util.random;

/**
 * An object that generates a stream of pseudorandom values.
 *
 * <p>A generator has exactly one primitive operation — produce the next batch of random bits — and
 * every other value a caller asks for (a bounded {@code int}, a {@code double} in
 * <code>[0,1)</code>, a {@code boolean}, a fill of bytes) is derived from it by arithmetic that has
 * nothing to do with the underlying algorithm. So {@link #nextLong()} is the single abstract method
 * and the rest are {@code default}s: a new engine implements one method and inherits an API.
 *
 * <p>Two values obtained from the same generator seeded the same way are <em>reproducible</em>.
 * That is the contract, and it is why an implementation may not substitute a different algorithm
 * for one whose sequence is specified.
 *
 * @implNote This interface replaces the old arrangement in which the derived methods were baked
 *           into {@link java.util.Random}, a concrete class. Every new engine then had to either
 *           re-derive them — and risk getting the bounded case subtly non-uniform — or extend a
 *           class whose 48-bit LCG state it did not want.
 *
 * @implNote A KajiLibrary subset. The stream methods ({@code ints}/{@code longs}/{@code doubles})
 *           are omitted: the unbounded forms are infinite, which the eager
 *           {@code java.util.stream} cannot express. {@code nextGaussian}/{@code nextExponential}
 *           are omitted because the JDK computes them with the modified ziggurat method, a pair of
 *           generated lookup tables; a polar-method version would have the right distribution but
 *           a different sequence, so it could not be validated against the JDK. {@code of(String)}
 *           and {@code getDefault()} need a registry of implementations.
 */
public interface RandomGenerator {

    /**
     * Returns the next pseudorandomly generated {@code long} value.
     *
     * <p>This is the generator's single primitive: every other method of this interface is defined
     * in terms of it.
     *
     * @return the next pseudorandom {@code long}
     */
    long nextLong();

    /**
     * Returns the next pseudorandomly generated {@code int} value.
     *
     * @return the next pseudorandom {@code int}
     * @implSpec Returns the HIGH 32 bits of {@link #nextLong()}. The top bits are used rather than
     *           the bottom ones because in several classic generators the low bits have a much
     *           shorter period than the high ones, and taking the top is the habit that survives a
     *           change of engine.
     */
    default int nextInt() {
        return (int) (this.nextLong() >>> 32);
    }

    /**
     * Returns a pseudorandomly generated {@code int} value uniformly distributed between zero
     * (inclusive) and {@code bound} (exclusive).
     *
     * @param bound the upper bound (exclusive); must be positive
     * @return a pseudorandom {@code int} in <code>[0, bound)</code>
     * @throws IllegalArgumentException if {@code bound} is not positive
     * @implSpec Rejection sampling, not a modulus. {@code nextInt() % bound} is biased whenever
     *           {@code bound} does not divide 2<sup>32</sup>, because the leftover values at the
     *           top of the range map back onto the low results; rejecting that leftover band is
     *           what restores uniformity. A power-of-two bound divides the range exactly, so it is
     *           handled by masking instead.
     */
    default int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int m = bound - 1;
        int r = this.nextInt();
        if ((bound & m) == 0) {
            return r & m;
        }
        int u = r >>> 1;
        r = u % bound;
        while (u + m - r < 0) {
            u = this.nextInt() >>> 1;
            r = u % bound;
        }
        return r;
    }

    /**
     * Returns a pseudorandomly generated {@code int} value uniformly distributed between
     * {@code origin} (inclusive) and {@code bound} (exclusive).
     *
     * @param origin the least value that can be returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code int} in <code>[origin, bound)</code>
     * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code bound}
     */
    default int nextInt(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + this.nextInt(bound - origin);
    }

    /**
     * Returns a pseudorandomly generated {@code long} value uniformly distributed between zero
     * (inclusive) and {@code bound} (exclusive).
     *
     * @param bound the upper bound (exclusive); must be positive
     * @return a pseudorandom {@code long} in <code>[0, bound)</code>
     * @throws IllegalArgumentException if {@code bound} is not positive
     * @implSpec The same rejection loop as {@link #nextInt(int)}, widened to 64 bits.
     */
    default long nextLong(long bound) {
        if (bound <= 0L) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long m = bound - 1L;
        long r = this.nextLong();
        if ((bound & m) == 0L) {
            return r & m;
        }
        long u = r >>> 1;
        r = u % bound;
        while (u + m - r < 0L) {
            u = this.nextLong() >>> 1;
            r = u % bound;
        }
        return r;
    }

    /**
     * Returns a pseudorandomly generated {@code long} value uniformly distributed between
     * {@code origin} (inclusive) and {@code bound} (exclusive).
     *
     * @param origin the least value that can be returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code long} in <code>[origin, bound)</code>
     * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code bound}
     */
    default long nextLong(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + this.nextLong(bound - origin);
    }

    /**
     * Returns a pseudorandomly generated {@code boolean} value.
     *
     * @return a pseudorandom {@code boolean}
     * @implSpec The sign bit of {@link #nextInt()}, which is as random as any other and costs
     *           nothing to read.
     */
    default boolean nextBoolean() {
        return this.nextInt() < 0;
    }

    /**
     * Returns a pseudorandomly generated {@code double} value between zero (inclusive) and one
     * (exclusive).
     *
     * @return a pseudorandom {@code double} in <code>[0.0, 1.0)</code>
     * @implSpec Takes the top 53 bits of {@link #nextLong()} and scales by 2<sup>-53</sup>. 53 is
     *           exactly the precision of a {@code double}'s mantissa, so every representable value
     *           in the range gets the same probability: more bits would not add resolution, fewer
     *           would leave gaps.
     */
    default double nextDouble() {
        return (this.nextLong() >>> 11) * 1.1102230246251565E-16;
    }

    /**
     * Returns a pseudorandomly generated {@code double} value between zero (inclusive) and
     * {@code bound} (exclusive).
     *
     * @param bound the upper bound (exclusive); must be positive and finite
     * @return a pseudorandom {@code double} in <code>[0.0, bound)</code>
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    default double nextDouble(double bound) {
        if (!(bound > 0.0)) {
            throw new IllegalArgumentException("bound must be positive");
        }
        return this.nextDouble() * bound;
    }

    /**
     * Returns a pseudorandomly generated {@code double} value between {@code origin} (inclusive)
     * and {@code bound} (exclusive).
     *
     * @param origin the least value that can be returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code double} in <code>[origin, bound)</code>
     * @throws IllegalArgumentException if {@code origin} is not less than {@code bound}
     */
    default double nextDouble(double origin, double bound) {
        if (!(origin < bound)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + this.nextDouble() * (bound - origin);
    }

    /**
     * Returns a pseudorandomly generated {@code float} value between zero (inclusive) and one
     * (exclusive).
     *
     * @return a pseudorandom {@code float} in <code>[0.0f, 1.0f)</code>
     * @implSpec 24 bits, for the same reason {@link #nextDouble()} uses 53: a {@code float}'s
     *           mantissa.
     */
    default float nextFloat() {
        return (float) ((this.nextLong() >>> 40) * 5.9604644775390625E-8);
    }

    /**
     * Returns a pseudorandomly generated {@code float} value between zero (inclusive) and
     * {@code bound} (exclusive).
     *
     * @param bound the upper bound (exclusive); must be positive and finite
     * @return a pseudorandom {@code float} in <code>[0.0f, bound)</code>
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    default float nextFloat(float bound) {
        if (!(bound > 0.0f)) {
            throw new IllegalArgumentException("bound must be positive");
        }
        return this.nextFloat() * bound;
    }

    /**
     * Returns a pseudorandomly generated {@code float} value between {@code origin} (inclusive)
     * and {@code bound} (exclusive).
     *
     * @param origin the least value that can be returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code float} in <code>[origin, bound)</code>
     * @throws IllegalArgumentException if {@code origin} is not less than {@code bound}
     */
    default float nextFloat(float origin, float bound) {
        if (!(origin < bound)) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + this.nextFloat() * (bound - origin);
    }

    /**
     * Fills the given array with pseudorandomly generated bytes.
     *
     * @param bytes the array to fill
     * @implSpec Consumes one {@link #nextLong()} per eight bytes rather than one per byte.
     */
    default void nextBytes(byte[] bytes) {
        int i = 0;
        int len = bytes.length;
        while (i < len) {
            long rnd = this.nextLong();
            int n = len - i;
            if (n > 8) {
                n = 8;
            }
            int k = 0;
            while (k < n) {
                bytes[i] = (byte) rnd;
                rnd = rnd >> 8;
                i = i + 1;
                k = k + 1;
            }
        }
    }
}
