package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import java.util.random.RandomGenerator;

// A generator built for splitting: `split()` hands back a new instance whose stream is
// statistically independent of this one, which is what a divide-and-conquer computation needs —
// sharing one Random across tasks serializes them, and seeding each by hand risks correlation.
//
// The algorithm is SplitMix64: advance the seed by a fixed odd increment (the golden gamma) and
// run the result through a mixing function. There is no loop and no dependence on the previous
// output, only on the counter — which is what makes splitting sound, and what makes it possible
// to jump to the n-th output without generating the first n-1.
//
// Contrast with {@link Random}: there the state IS the output (a 48-bit LCG), so two instances
// seeded close together produce visibly related streams. Here the counter is deliberately dull
// and all the quality comes from the mixer, so neighbouring seeds decorrelate immediately.
//
// Implements RandomGenerator, which is where nextInt(int), nextBoolean() and nextDouble() now come
// from — as interface defaults, exactly as in the JDK. Declaring them here instead was costing
// three api_shape allowlist entries: the JDK does NOT declare them on this class, so ours read as
// extras. `nextInt()` and `nextLong()` stay, because the JDK overrides those too (mix32/mix64 are
// cheaper and better than deriving them from the primitive).
//
// Subset: the stream methods (ints/longs/doubles/splits) are omitted, as is the nested
// Implementa `RandomGenerator.SplittableGenerator`, que es donde vive el contrato de partirse.
// La nota vieja decia que un tipo anidado no resolvia (#101); eso quedo arreglado.
public final class SplittableRandom
        implements RandomGenerator, RandomGenerator.SplittableGenerator {

    private long seed;
    // The per-instance increment. Odd by construction, so the counter visits every 64-bit value
    // before repeating — a period of 2^64 with no short cycles to fall into.
    private final long gamma;

    // The golden gamma, 0x9e3779b97f4a7c15 — 2^64 divided by the golden ratio, the same constant
    // Knuth's multiplicative hashing uses. It is spelled out at each use site rather than kept in
    // a `static final long`, which our javac does not initialize (#112).
    public SplittableRandom(long seed) {
        this.seed = seed;
        this.gamma = -7046029254386353131L;
    }

    public SplittableRandom() {
        this(System.currentTimeMillis());
    }

    private SplittableRandom(long seed, long gamma) {
        this.seed = seed;
        this.gamma = gamma;
    }

    // Stafford variant 13 of the SplitMix64 finalizer: xor-shift, multiply, twice, then a final
    // xor-shift. Nothing here is reversible-looking by accident — the shift/multiplier pairs were
    // searched for avalanche, so a one-bit change in `z` flips about half the output bits.
    private static long mix64(long z) {
        long x = z;
        x = (x ^ (x >>> 30)) * -4658895280553007687L;  // 0xbf58476d1ce4e5b9
        x = (x ^ (x >>> 27)) * -7723592293110705685L;  // 0x94d049bb133111eb
        return x ^ (x >>> 31);
    }

    // Stafford variant 4, keeping only the top 32 bits. Taking the HIGH half matters: in a
    // multiply the high bits are the ones every input bit has had a chance to reach.
    private static int mix32(long z) {
        long x = z;
        x = (x ^ (x >>> 33)) * 7109453100751455733L;   // 0x62a9d9ed799705f5
        return (int) (((x ^ (x >>> 28)) * -3808689974395783757L) >>> 32);
    }

    // The gamma for a split-off generator gets a different mixer (MurmurHash3's) plus two
    // corrections: force it odd, so the new counter still has full period; and if its bit pattern
    // is too regular — fewer than 24 transitions between adjacent bits — flip alternate bits.
    // A gamma like 0x0000000000000003 would step the counter in a way the mixer handles poorly,
    // and splitting is exactly where such a value would go unnoticed.
    private static long mixGamma(long z) {
        long x = z;
        x = (x ^ (x >>> 33)) * -49064778989728563L;    // 0xff51afd7ed558ccd
        x = (x ^ (x >>> 33)) * -4265267296055464877L;  // 0xc4ceb9fe1a85ec53
        x = (x ^ (x >>> 33)) | 1L;
        int n = bitCount(x ^ (x >>> 1));
        long g;
        if (n < 24) {
            g = x ^ -6148914691236517206L;             // 0xaaaaaaaaaaaaaaaa
        } else {
            g = x;
        }
        return g;
    }

    // Population count, by hand: java.lang.Long has no bitCount here, and only mixGamma needs it.
    private static int bitCount(long v) {
        long x = v;
        int n = 0;
        for (int i = 0; i < 64; i++) {
            n = n + ((int) (x & 1L));
            x = x >>> 1;
        }
        return n;
    }

    // Advance the counter and return the raw state. Deliberately unmixed: the seed the caller
    // sees is never the seed the generator holds.
    private long nextSeed() {
        seed = seed + gamma;
        return seed;
    }

    // A new generator, independent of this one: both the seed and the gamma are derived from
    // fresh state, so the two streams do not shadow each other.
    public SplittableRandom split() {
        long newSeed = nextLong();
        long newGamma = mixGamma(nextSeed());
        return new SplittableRandom(newSeed, newGamma);
    }

    public long nextLong() {
        return mix64(nextSeed());
    }

    public int nextInt() {
        return mix32(nextSeed());
    }


    /**
     * Un generador nuevo, con la entropia sacada de `source` en vez de la propia.
     *
     * <p>Es lo que permite **reproducir** una particion entera: dos corridas que partan del mismo
     * `source` obtienen exactamente los mismos hijos, sin importar cuanto haya consumido este
     * generador por su cuenta.
     */
    public SplittableRandom split(RandomGenerator.SplittableGenerator source) {
        long newSeed = source.nextLong();
        long newGamma = mixGamma(source.nextLong());
        return new SplittableRandom(newSeed, newGamma);
    }

    public Stream<RandomGenerator.SplittableGenerator> splits(long streamSize) {
        return this.splits(streamSize, this);
    }

    public Stream<RandomGenerator.SplittableGenerator> splits(long streamSize,
            RandomGenerator.SplittableGenerator source) {
        if (streamSize < 0) {
            throw new IllegalArgumentException("streamSize must be non-negative");
        }
        Object[] a = new Object[(int) streamSize];
        int i = 0;
        while (i < a.length) {
            a[i] = this.split(source);
            i = i + 1;
        }
        return (Stream<RandomGenerator.SplittableGenerator>) Stream.of(a);
    }
}
