package java.util.random;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

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
 * @implNote This note used to list as omitted the {@code ints}/{@code longs}/{@code doubles}
 *           stream methods, {@code nextGaussian}/{@code nextExponential}, {@code of(String)} and
 *           {@code getDefault()}. All of them are declared below. What survives of it is narrower
 *           and is stated where it applies: the four stream forms that take no size refuse, because
 *           an unbounded stream needs laziness and this library's are eager; and the two
 *           distributions are computed by the polar and inverse-transform methods rather than the
 *           JDK's ziggurat, so they have the right distribution and a different sequence -- which
 *           these defaults are allowed, because unlike {@link java.util.Random} they name no
 *           algorithm.
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

    // ---- the streams of values --------------------------------------------------------------------
    //
    // Twelve factories that are the same idea four times per type: with or without a count, with or
    // without a range. They are here --and not in each generator-- because they depend on nothing
    // more than `nextInt`, `nextLong` and `nextDouble`, which is exactly what each implementation
    // brings.
    //
    // **A deliberate divergence, and it is the only one**: the forms **without a count** (`ints()`,
    // `longs()`, `doubles()`) refuse instead of returning an infinite stream.
    //
    // The JDK defines them as "effectively unlimited", and that asks for a **lazy** stream: the
    // values are generated as somebody asks for them, and `limit(n)` cuts before generating the
    // rest. This library's streams are backed by an array and are **eager** --they materialise whole
    // on creation-- so an infinite stream cannot be represented.
    //
    // Of the two ways out the noisy one is chosen. Returning a long prefix and pretending it is
    // infinite would work for `ints().limit(10)` and would give **fewer** values than asked for on
    // `ints().limit(a_million)`, in silence. A method that refuses and says what to replace it with
    // is worse to use and better to trust.
    private static UnsupportedOperationException noSize(String which) {
        return new UnsupportedOperationException(
                "this library's streams are eager: use " + which + "(streamSize)");
    }

    /**
     * `streamSize` pseudorandom ints.
     *
     * @throws IllegalArgumentException if `streamSize` is negative
     */
    default IntStream ints(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        int[] a = new int[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextInt();
            i = i + 1;
        }
        return IntStream.of(a);
    }

    // `streamSize` ints in `[origin, bound)`.
    default IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        int[] a = new int[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextInt(randomNumberOrigin, randomNumberBound);
            i = i + 1;
        }
        return IntStream.of(a);
    }

    default IntStream ints() {
        throw noSize("ints");
    }

    default IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        throw noSize("ints");
    }

    default LongStream longs(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        long[] a = new long[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextLong();
            i = i + 1;
        }
        return LongStream.of(a);
    }

    default LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        long[] a = new long[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextLong(randomNumberOrigin, randomNumberBound);
            i = i + 1;
        }
        return LongStream.of(a);
    }

    default LongStream longs() {
        throw noSize("longs");
    }

    // Mind this one: it is **not** the count form. `longs(long, long)` is origin and bound; the
    // single-count one is `longs(long)`. The signature clash is the JDK's and is replicated as it
    // is.
    default LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        throw noSize("longs");
    }

    default DoubleStream doubles(long streamSize) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        double[] a = new double[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextDouble();
            i = i + 1;
        }
        return DoubleStream.of(a);
    }

    default DoubleStream doubles(long streamSize, double randomNumberOrigin,
            double randomNumberBound) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        double[] a = new double[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.nextDouble(randomNumberOrigin, randomNumberBound);
            i = i + 1;
        }
        return DoubleStream.of(a);
    }

    default DoubleStream doubles() {
        throw noSize("doubles");
    }

    default DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        throw noSize("doubles");
    }

    /**
     * A generator that knows how to **split**: to give another generator independent of the first.
     *
     * <p>It exists for a very concrete problem of parallelism. Sharing a generator between threads
     * requires synchronising it, and that makes it the bottleneck; giving each thread its own seed
     * "at random" guarantees nothing -- two nearby seeds can give overlapping sequences. Splitting
     * settles both: each thread takes away a generator of its own, with no lock, and with the
     * guarantee that the sequences do not step on each other.
     *
     * <p>The forms with `source` take their entropy from **another** generator instead of their own,
     * which is what allows a whole split to be reproduced from a single seed.
     */
    interface SplittableGenerator extends RandomGenerator {

        SplittableGenerator split();

        SplittableGenerator split(SplittableGenerator source);

        Stream<SplittableGenerator> splits(long streamSize);

        Stream<SplittableGenerator> splits(long streamSize, SplittableGenerator source);

        // The two without a count refuse, for the same reason as `ints()`/`longs()`/`doubles()`:
        // this library's streams are eager and cannot be infinite.
        default Stream<SplittableGenerator> splits() {
            throw new UnsupportedOperationException(
                    "this library's streams are eager: use splits(streamSize)");
        }

        default Stream<SplittableGenerator> splits(SplittableGenerator source) {
            throw new UnsupportedOperationException(
                    "this library's streams are eager: use splits(streamSize, source)");
        }
    }

    /**
     * A generator that knows how to **jump**: to advance a huge distance of its sequence in one go.
     *
     * <p>It settles the same problem as splitting, by the other road. A generator with a gigantic
     * period can be shared out among threads by giving each one a **disjoint** stretch: thread N
     * starts at position N times the jump distance. The guarantee is not statistical but arithmetic
     * -- the stretches do not overlap because the distance is known.
     *
     * <p>The difference from splitting: jumping needs the algorithm to have a closed form for
     * advancing (a transition matrix raised to a power), and not all of them do. The LXM split; the
     * xoshiro jump.
     */
    interface JumpableGenerator extends RandomGenerator {

        /** A copy of this generator, in the same state. */
        JumpableGenerator copy();

        /** It advances this generator by one jump distance. */
        void jump();

        /** How many values {@link #jump()} advances by. */
        double jumpDistance();

        /**
         * A copy in the current state, and **this one** is left advanced by a jump.
         *
         * <p>The order matters and it is the one the name says: the copy comes first. What is
         * returned is the stretch that starts where it was, and the caller keeps the next one.
         */
        default RandomGenerator copyAndJump() {
            RandomGenerator snapshot = this.copy();
            this.jump();
            return snapshot;
        }

        /** `streamSize` generators, each one a jump further along than the previous. */
        default Stream<RandomGenerator> jumps(long streamSize) {
            if (streamSize < 0L) {
                throw new IllegalArgumentException("size must be non-negative");
            }
            java.util.List<RandomGenerator> out = new java.util.ArrayList<RandomGenerator>();
            long i = 0L;
            while (i < streamSize) {
                out.add(this.copyAndJump());
                i = i + 1L;
            }
            return out.stream();
        }

        /** As with {@link #jumps(long)}: this library's streams are eager. */
        default Stream<RandomGenerator> rngs(long streamSize) {
            return this.jumps(streamSize);
        }

        // The two without a count refuse, for the same reason as `ints()`/`longs()`/`doubles()`.
        default Stream<RandomGenerator> jumps() {
            throw new UnsupportedOperationException(
                    "this library's streams are eager: use jumps(streamSize)");
        }

        default Stream<RandomGenerator> rngs() {
            throw new UnsupportedOperationException(
                    "this library's streams are eager: use rngs(streamSize)");
        }

        /**
         * A jumpable generator of the algorithm named.
         *
         * @throws IllegalArgumentException if that algorithm does not exist or cannot jump
         */
        static JumpableGenerator of(String name) {
            RandomGenerator g = RandomGeneratorFactory.of(name).create();
            if (!(g instanceof JumpableGenerator)) {
                throw new IllegalArgumentException("the algorithm " + name + " cannot jump");
            }
            return (JumpableGenerator) g;
        }
    }

    /**
     * A generator that on top of that knows how to take a **leap**.
     *
     * <p>The two levels are not a whim: the jump shares stretches out among threads, and the leap
     * shares **sets of stretches** out among machines. With a single size one has to choose between
     * fine granularity and reach, and with two one does not.
     */
    interface LeapableGenerator extends JumpableGenerator {

        /** A copy of this generator, in the same state. */
        LeapableGenerator copy();

        /** It advances this generator by one leap distance. */
        void leap();

        /** How many values {@link #leap()} advances by. */
        double leapDistance();

        /** A copy in the current state, and **this one** is left advanced by a leap. */
        default JumpableGenerator copyAndLeap() {
            JumpableGenerator snapshot = this.copy();
            this.leap();
            return snapshot;
        }

        /** `streamSize` generators, each one a leap further along than the previous. */
        default Stream<JumpableGenerator> leaps(long streamSize) {
            if (streamSize < 0L) {
                throw new IllegalArgumentException("size must be non-negative");
            }
            java.util.List<JumpableGenerator> out = new java.util.ArrayList<JumpableGenerator>();
            long i = 0L;
            while (i < streamSize) {
                out.add(this.copyAndLeap());
                i = i + 1L;
            }
            return out.stream();
        }

        default Stream<JumpableGenerator> leaps() {
            throw new UnsupportedOperationException(
                    "this library's streams are eager: use leaps(streamSize)");
        }

        /**
         * A leapable generator of the algorithm named.
         *
         * @throws IllegalArgumentException if that algorithm does not exist or cannot leap
         */
        static LeapableGenerator of(String name) {
            RandomGenerator g = RandomGeneratorFactory.of(name).create();
            if (!(g instanceof LeapableGenerator)) {
                throw new IllegalArgumentException("the algorithm " + name + " cannot leap");
            }
            return (LeapableGenerator) g;
        }
    }

    // ---- the static factories     ---------------------------------------------------------------

    /**
     * A generator of the algorithm named.
     *
     * <p>The names are `RandomGeneratorFactory.names()`'s twelve. One that is not there is an
     * `IllegalArgumentException`, not a silent default generator: asking for `"Xoshiro256"` and
     * getting something else would be the worst possible outcome, because the code would carry on
     * running with statistical properties that are not the ones it asked for.
     *
     * @throws IllegalArgumentException if there is no implementation of that algorithm
     */
    static RandomGenerator of(String name) {
        return RandomGeneratorFactory.of(name).create();
    }

    /**
     * The default generator.
     *
     * <p>It delegates to `RandomGeneratorFactory.getDefault()`, and that delegation is the point: if
     * the two chose on their own they could stop agreeing, and "the default algorithm" would come to
     * depend on which of the two doors one came in by.
     */
    static RandomGenerator getDefault() {
        return RandomGeneratorFactory.getDefault().create();
    }

    // ---- the distributions  ----------------------------------------------------------------------

    /**
     * Whether the algorithm is **deprecated**.
     *
     * <p>`false` here, and whichever one is deprecated overrides it. Today **none** of the twelve is,
     * and that was verified against JDK 25 rather than taken for granted: `java.util.Random` is the
     * obvious candidate --its 48-bit LCG survives only for compatibility, because its sequence is
     * part of the contract and improving it would break everyone who depends on it-- and even so the
     * real `java` also returns `false` for it. It agrees with what `RandomGeneratorFactory` says.
     */
    default boolean isDeprecated() {
        return false;
    }

    /**
     * A value from a normal of mean 0 and standard deviation 1.
     *
     * <p>It is Marsaglia's polar method: uniform points are thrown in the square
     * {@code [-1,1]x[-1,1]} until one falls inside the unit circle, and the factor
     * {@code sqrt(-2*log(s)/s)} turns it into a normal.
     *
     * <p><b>The value is not part of the contract, and the distinction matters.</b>
     * `java.util.Random` **overrides** this method and there the value **is** part of it --its
     * javadoc names the algorithm, so two `Random`s with the same seed have to give the same
     * gaussians. This default names none: the only thing it promises is the distribution. That is
     * why the JDK's version was not copied (a ziggurat with tables of 256 entries): it would give
     * other numbers and neither of the two would be wrong.
     *
     * <p>Unlike `Random`'s, this one does **not** keep the pair's second value: one of every two is
     * discarded. Keeping it would ask for state, and an interface has nowhere to put it.
     */
    default double nextGaussian() {
        double v1 = 0.0d;
        double s = 0.0d;
        boolean usable = false;
        while (!usable) {
            v1 = 2 * this.nextDouble() - 1;
            double v2 = 2 * this.nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
            // `s == 0` is discarded along with the ones outside the circle: not only would it
            // divide by zero, `log(0)` is -infinity.
            usable = s < 1 && s != 0;
        }
        return v1 * StrictMath.sqrt(-2 * StrictMath.log(s) / s);
    }

    /**
     * A value from a normal with the given mean and standard deviation.
     *
     * @throws IllegalArgumentException if `stddev` is negative
     */
    default double nextGaussian(double mean, double stddev) {
        // `stddev < 0`, the direct form, and **not** a negated one that would catch `NaN` too.
        //
        // The negated version (`!(stddev >= 0)`) looks better and is wrong: the contract says "if
        // stddev is negative", and `NaN` is not negative. It was verified against the real `java`,
        // which returns `NaN` instead of throwing. A `-0.0` does not throw either, and that agrees
        // as well: `-0.0 < 0` is false.
        if (stddev < 0.0d) {
            throw new IllegalArgumentException("stddev must be non-negative");
        }
        return mean + stddev * this.nextGaussian();
    }

    /**
     * A value from an exponential of mean 1.
     *
     * <p>By inverse transform: if {@code u} is uniform in {@code (0,1]}, then {@code -log(u)} is
     * exponential of mean 1. {@code 1 - nextDouble()} is used and not plain {@code nextDouble()}
     * precisely so that zero stays out --`nextDouble()` is {@code [0,1)}, and `log(0)` would give
     * infinity.
     *
     * <p>As with the one above, the value is not part of the contract: only the distribution. The
     * JDK uses a ziggurat, which is faster and gives other numbers.
     */
    default double nextExponential() {
        return -StrictMath.log(1.0d - this.nextDouble());
    }

    /**
     * A stream of doubles equidistributed over the given range.
     *
     * <p>**It refuses**, for the same reason as `ints()`/`longs()`/`doubles()`: the JDK defines it
     * with no count limit, and this library's streams are eager. See `noSize`'s long note.
     *
     * <p>And here there is not even a sized replacement to offer --`equiDoubles` has no overload
     * with `streamSize`-- so the message points at `doubles(streamSize, origin, bound)`, which is as
     * close as one can be.
     */
    default DoubleStream equiDoubles(double origin, double bound, boolean isOriginInclusive,
            boolean isBoundInclusive) {
        throw new UnsupportedOperationException(
                "this library's streams are eager and `equiDoubles` has no overload"
                        + " with a size: use doubles(streamSize, origin, bound)");
    }
}
