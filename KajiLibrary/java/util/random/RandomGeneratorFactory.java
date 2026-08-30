package java.util.random;

import java.math.BigInteger;
import java.util.stream.Stream;
import jdk.internal.random.L128X1024MixRandom;
import jdk.internal.random.L128X128MixRandom;
import jdk.internal.random.L128X256MixRandom;
import jdk.internal.random.L32X64MixRandom;
import jdk.internal.random.L64X1024MixRandom;
import jdk.internal.random.L64X128MixRandom;
import jdk.internal.random.L64X128StarStarRandom;
import jdk.internal.random.L64X256MixRandom;
import jdk.internal.random.Xoroshiro128PlusPlus;
import jdk.internal.random.Xoshiro256PlusPlus;

/**
 * A factory for a named {@link RandomGenerator} algorithm, and the metadata that lets a caller
 * choose one without knowing its name.
 *
 * <p>The problem it solves: {@code new SplittableRandom()} hard-codes a decision that belongs to
 * the caller. An application that needs a generator it can split across tasks, or one with 64-bit
 * equidistribution, should be able to say <em>that</em> — and let the library pick. So each
 * algorithm is registered under a name together with its properties, and both lookups are possible:
 *
 * <pre>
 *     RandomGeneratorFactory.of("Xoshiro256PlusPlus").create(seed);   // by name
 *     ... isSplittable() / equidistribution() / period() ...          // by property
 * </pre>
 *
 * <p>The properties are not decoration. {@link #equidistribution()} says how many consecutive
 * values are guaranteed uniform <em>as a tuple</em>, which is what matters when the output is
 * consumed in groups — shuffling, sampling coordinates, generating vectors. {@link #period()} is
 * exact, and is returned as a {@link BigInteger} because for most of these algorithms it does not
 * fit in any primitive.
 *
 * @implNote The registry is a fixed table. The JDK builds its equivalent by discovering
 *           implementations at runtime, which needs both a service lookup and
 *           {@code Constructor.newInstance} to build the chosen class — reflection KajiLibrary does
 *           not have. A table gives working lookup and working metadata; what it does not give is
 *           extensibility, which is the actual point of the service pattern.
 *
 * @implNote NOT generic, unlike the JDK's {@code RandomGeneratorFactory<T extends RandomGenerator>}.
 *           A bounded type variable erases to {@code Object} in our compiler instead of to its
 *           bound (finding #100), so a generic {@code create()} would emit
 *           {@code ()Ljava/lang/Object;} where the JDK erases to
 *           {@code ()Ljava/util/random/RandomGenerator;}. Declaring the erased form directly gives
 *           the right descriptor and needs no allowlist entry.
 *
 * @implNote A KajiLibrary subset:
 *           <ul>
 *           <li>The no-argument {@code create()} is omitted. It would need an entropy source to
 *               vary the seed between runs, and {@code System.currentTimeMillis} is not yet a
 *               native — a {@code create()} that silently returned the same stream on every run
 *               would be worse than none.</li>
 *           <li>{@code create(byte[])} is omitted.</li>
 *           </ul>
 */
public final class RandomGeneratorFactory {

    // El contador que hace distintos a dos generadores creados en el mismo milisegundo.
    private static final java.util.concurrent.atomic.AtomicLong SIGUIENTE_SEMILLA =
            new java.util.concurrent.atomic.AtomicLong(1L);


    private final int index;

    private RandomGeneratorFactory(int index) {
        this.index = index;
    }

    // The registry, as parallel rows. Arrays rather than `static final` scalars because a
    // static-final primitive reads back as 0 at runtime (finding #112); an array is an object and
    // its `<clinit>` runs normally.
    private static String[] names() {
        return new String[] {
            "L32X64MixRandom", "L64X128MixRandom", "L64X128StarStarRandom", "L64X256MixRandom",
            "L64X1024MixRandom", "L128X128MixRandom", "L128X256MixRandom", "L128X1024MixRandom",
            "Xoroshiro128PlusPlus", "Xoshiro256PlusPlus", "Random", "SplittableRandom",
        };
    }

    private static String[] groups() {
        return new String[] {
            "LXM", "LXM", "LXM", "LXM", "LXM", "LXM", "LXM", "LXM",
            "Xoroshiro", "Xoshiro", "Legacy", "Legacy",
        };
    }

    // Bits of LCG state and of xor-shift state, per entry. The pair is enough to derive both the
    // total state size and the exact period, so neither is stored separately.
    private static int[] lcgBits() {
        return new int[] {32, 64, 64, 64, 64, 128, 128, 128, 0, 0, 48, 64};
    }

    private static int[] xbgBits() {
        return new int[] {64, 128, 128, 256, 1024, 128, 256, 1024, 128, 256, 0, 0};
    }

    private static int[] equidistributions() {
        return new int[] {1, 2, 2, 4, 16, 1, 1, 1, 1, 3, 0, 1};
    }

    private static boolean[] splittables() {
        return new boolean[] {true, true, true, true, true, true, true, true,
            false, false, false, true};
    }

    private static boolean[] jumpables() {
        return new boolean[] {false, false, false, false, false, false, false, false,
            true, true, false, false};
    }

    /**
     * Returns the factory for the algorithm of the given name.
     *
     * @param name the algorithm name, as reported by {@link #name()}
     * @return the factory for that algorithm
     * @throws IllegalArgumentException if no algorithm of that name is registered
     */
    /**
     * Returns a stream of every algorithm this implementation provides.
     *
     * @return a stream of one factory per available algorithm
     * @implNote The JDK discovers its providers through {@code ServiceLoader} and can therefore
     *           report a different set per run. Ours is the fixed registry above, so the stream is
     *           just that table, in declaration order. Returning it is honest either way: the
     *           contract is "what is available", and here that is knowable up front.
     */
    public static Stream<RandomGeneratorFactory> all() {
        String[] names = RandomGeneratorFactory.names();
        RandomGeneratorFactory[] out = new RandomGeneratorFactory[names.length];
        int i = 0;
        while (i < names.length) {
            out[i] = new RandomGeneratorFactory(i);
            i = i + 1;
        }
        return Stream.of(out);
    }

    public static RandomGeneratorFactory of(String name) {
        String[] all = RandomGeneratorFactory.names();
        int i = 0;
        int found = -1;
        while (i < all.length) {
            if (all[i].equals(name)) {
                found = i;
                i = all.length;
            } else {
                i = i + 1;
            }
        }
        if (found < 0) {
            throw new IllegalArgumentException("No implementation of the random number generator algorithm \""
                    + name + "\" is available");
        }
        return new RandomGeneratorFactory(found);
    }

    /**
     * Returns the factory for the default algorithm.
     *
     * @return the default factory
     * @implNote The default is {@code L32X64MixRandom}, matching the JDK — deliberately NOT
     *           {@code Random}, whose 48-bit LCG is kept only for compatibility.
     */
    public static RandomGeneratorFactory getDefault() {
        return RandomGeneratorFactory.of("L32X64MixRandom");
    }

    /**
     * Returns the name of the algorithm.
     *
     * @return the algorithm name
     */
    public String name() {
        return RandomGeneratorFactory.names()[this.index];
    }

    /**
     * Returns the family the algorithm belongs to.
     *
     * @return the group name, such as {@code "LXM"} or {@code "Legacy"}
     */
    public String group() {
        return RandomGeneratorFactory.groups()[this.index];
    }

    /**
     * Returns the number of bits of state the algorithm keeps.
     *
     * @return the state size in bits
     */
    public int stateBits() {
        return RandomGeneratorFactory.lcgBits()[this.index]
                + RandomGeneratorFactory.xbgBits()[this.index];
    }

    /**
     * Returns how many consecutive values the algorithm produces uniformly <em>as a tuple</em>.
     *
     * @return the equidistribution
     */
    public int equidistribution() {
        return RandomGeneratorFactory.equidistributions()[this.index];
    }

    /**
     * Returns the exact period of the algorithm — how many values it produces before repeating.
     *
     * @return the period
     * @implSpec Computed rather than tabulated: a combined generator's period is the product of its
     *           subgenerators', so it is 2<sup>lcg</sup> &times; (2<sup>xbg</sup> &minus; 1), with
     *           either factor dropped when that half is absent. A pure LCG has period
     *           2<sup>bits</sup>; a pure xor-shift generator has 2<sup>bits</sup> &minus; 1, because
     *           the all-zero state is excluded.
     */
    public BigInteger period() {
        BigInteger two = BigInteger.valueOf(2L);
        int lcg = RandomGeneratorFactory.lcgBits()[this.index];
        int xbg = RandomGeneratorFactory.xbgBits()[this.index];
        BigInteger result = BigInteger.valueOf(1L);
        if (lcg > 0) {
            result = two.pow(lcg);
        }
        if (xbg > 0) {
            result = result.multiply(two.pow(xbg).subtract(BigInteger.valueOf(1L)));
        }
        return result;
    }

    /**
     * Returns whether the algorithm is designed to pass statistical quality tests.
     *
     * @return {@code true} for every algorithm in this registry
     */
    public boolean isStatistical() {
        return true;
    }

    /**
     * Returns whether the algorithm draws from a source of true randomness.
     *
     * @return {@code false} — every algorithm here is deterministic given its seed
     */
    public boolean isStochastic() {
        return false;
    }

    /**
     * Returns whether the algorithm is implemented in hardware.
     *
     * @return {@code false}
     */
    public boolean isHardware() {
        return false;
    }

    /**
     * Returns whether an instance can produce an independent instance of itself.
     *
     * @return {@code true} if the algorithm is splittable
     */
    public boolean isSplittable() {
        return RandomGeneratorFactory.splittables()[this.index];
    }

    /**
     * Returns whether an instance can skip forward a fixed distance in its stream.
     *
     * @return {@code true} if the algorithm is jumpable
     */
    public boolean isJumpable() {
        return RandomGeneratorFactory.jumpables()[this.index];
    }

    /**
     * Returns whether an instance can skip forward a larger fixed distance in its stream.
     *
     * @return {@code true} if the algorithm is leapable
     * @implSpec Every leapable algorithm here is also jumpable; leaping is the coarser move.
     */
    public boolean isLeapable() {
        return RandomGeneratorFactory.jumpables()[this.index];
    }

    /**
     * Returns whether an instance can skip forward an arbitrary distance in its stream.
     *
     * @return {@code false} — no algorithm in this registry supports it
     */
    public boolean isArbitrarilyJumpable() {
        return false;
    }

    /**
     * Returns whether an instance can produce streams of values.
     *
     * @return {@code true} if the algorithm is streamable
     */
    public boolean isStreamable() {
        return RandomGeneratorFactory.splittables()[this.index]
                || RandomGeneratorFactory.jumpables()[this.index];
    }

    /**
     * Returns whether the algorithm is deprecated.
     *
     * @return {@code false} for every algorithm in this registry
     */
    public boolean isDeprecated() {
        return false;
    }

    /**
     * Creates a generator of this algorithm, seeded with the given value.
     *
     * @param seed the seed
     * @return a new generator
     * @implSpec The chosen class is constructed directly rather than reflectively, which is what
     *           lets this registry work without {@code Constructor.newInstance}.
     */
    /**
     * Creates a generator of this algorithm, seeded from a value chosen at random.
     *
     * @return a new generator
     * @implNote The seed mixes the clock with a per-call counter. The clock alone is not enough:
     *           two generators created in the same millisecond would run the same sequence, which
     *           is exactly the trap this method exists to avoid.
     */
    public RandomGenerator create() {
        long n = SIGUIENTE_SEMILLA.getAndIncrement();
        long seed = System.currentTimeMillis() ^ (n * -7046029254386353131L);
        // El mismo mezclador de 64 bits que usa `SplittableRandom`: sin el, semillas consecutivas
        // quedan consecutivas, y varios generadores arrancarian en estados vecinos.
        seed = (seed ^ (seed >>> 30)) * -4658895280553007687L;
        seed = (seed ^ (seed >>> 27)) * -7723592293110705685L;
        seed = seed ^ (seed >>> 31);
        return this.create(seed);
    }

    public RandomGenerator create(long seed) {
        int i = this.index;
        if (i == 0) {
            return new L32X64MixRandom(seed);
        }
        if (i == 1) {
            return new L64X128MixRandom(seed);
        }
        if (i == 2) {
            return new L64X128StarStarRandom(seed);
        }
        if (i == 3) {
            return new L64X256MixRandom(seed);
        }
        if (i == 4) {
            return new L64X1024MixRandom(seed);
        }
        if (i == 5) {
            return new L128X128MixRandom(seed);
        }
        if (i == 6) {
            return new L128X256MixRandom(seed);
        }
        if (i == 7) {
            return new L128X1024MixRandom(seed);
        }
        if (i == 8) {
            return new Xoroshiro128PlusPlus(seed);
        }
        if (i == 9) {
            return new Xoshiro256PlusPlus(seed);
        }
        if (i == 10) {
            return new java.util.Random(seed);
        }
        return new java.util.SplittableRandom(seed);
    }
}
