package java.util.concurrent;

import java.util.Random;

// A {@link Random} whose state is **per thread**. That is the whole design, and it fixes a
// problem that is easy to miss: a shared Random is not merely slow under concurrency, it is
// slow *because* it is correct. Its 48-bit seed is a single mutable word that every nextInt()
// advances, so the JDK guards it with a CAS retry loop — and under contention threads spend
// their time losing that race and retrying. Worse, giving each thread its own `new Random()`
// instead is a trap of its own: constructed from the clock, several of them created in the
// same instant get the *same seed* and produce identical streams, which silently correlates
// exactly the workers you wanted to be independent.
//
// ThreadLocalRandom answers both at once. Each thread gets its own generator, seeded from a
// per-instance mixer rather than the clock, so there is no shared word to contend on and no
// two threads share a stream. The cost is that you do not choose the seed and cannot line a
// stream up with a particular thread — a deliberate trade: this class is for Monte-Carlo
// work, jitter, backoff and random load spreading, not for anything that has to replay
// identically.
//
// The API says all of this out loud. The constructor is **private** and there is no public
// factory returning a new instance — the only way in is the static {@link #current}, which
// hands back *this thread's* generator. So a ThreadLocalRandom cannot be stored in a field
// and shared; the idiom is to call current() at the point of use, every time. Doing so is
// cheap, and it is what keeps the per-thread guarantee true even on a pooled thread.
//
// The JDK stores the seed in three fields on {@link Thread} itself and has current() read
// them directly, which is why its instances are stateless singletons. KajiLibrary has no such
// hook into Thread, so the per-thread instance is held in a {@link ThreadLocal} and the seed
// lives where {@link Random} already keeps it — one instance per thread instead of one shared
// instance reading per-thread fields. Observably identical: distinct, uncontended state per
// thread.
//
// Subset: the JDK's setSeed(long) override (which throws UnsupportedOperationException) is
// not declared, so Random's inherited setSeed still works here — a divergence in what the
// class *forbids*, not in what it does. The stream methods (ints/longs/doubles) and
// nextGaussian are absent, as they are on Random.
//
// Single-exit style throughout (finding #105).
public class ThreadLocalRandom extends Random {

    // This thread's generator. Reference-typed and initialised by a real <clinit>, unlike a
    // `static final` *primitive*, which our javac stores only in a ConstantValue attribute
    // and which then reads back as 0 (finding #112).
    private static ThreadLocal<ThreadLocalRandom> LOCAL = new ThreadLocal<ThreadLocalRandom>();

    // Guards the seed mixer below. A plain object rather than a class literal, which our
    // javac does not emit.
    private static Object SEED_LOCK = new Object();

    // The mixer that gives each new generator a distinct seed. It is multiplied by an odd
    // constant per instance, so successive threads get values far apart in the state space
    // instead of the near-identical clock readings `new Random()` would hand them. The
    // starting value is the golden-ratio constant the JDK uses for the same purpose.
    private static long seedMixer = -7046029254386353131L;

    // Private, and there is no other constructor: an instance may only be obtained from
    // current(), which is what makes "one per thread" enforceable rather than a convention.
    //
    // The seed goes through `super(...)` rather than a setSeed() call afterwards, because the
    // inherited no-arg Random() would otherwise run first and seed itself from the clock —
    // which is exactly the correlated-seeds trap this class exists to avoid (and which
    // KajiJDK's runtime has no native for in any case).
    private ThreadLocalRandom() {
        super(nextSeed());
    }

    // A fresh, well-separated seed. The JDK mixes in a clock reading as well; KajiJDK's
    // runtime has no clock native, so the mixer stands alone — which means the *sequence of
    // seeds* repeats from run to run. That costs nothing here: the guarantee this class owes
    // is that two threads do not correlate with each other, not that a run is unpredictable.
    private static long nextSeed() {
        long s;
        synchronized (SEED_LOCK) {
            seedMixer = seedMixer * 1181783497276652981L;
            s = seedMixer;
        }
        return s;
    }

    // The calling thread's generator. The entry point, and the only one: call it where you
    // need a number rather than caching the result in a shared field.
    public static ThreadLocalRandom current() {
        ThreadLocalRandom r = LOCAL.get();
        if (r == null) {
            r = new ThreadLocalRandom();
            LOCAL.set(r);
        }
        return r;
    }

    // Uniform in [origin, bound). The offset form is the one that saves callers from writing
    // `origin + nextInt(bound - origin)` and getting the overflow wrong.
    public int nextInt(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        int span = bound - origin;
        int r;
        if (span > 0) {
            r = origin + nextInt(span);
        } else {
            // The range is wider than an int: draw from the whole space and reject until the
            // value lands inside. Rejection rather than a modulus, which would skew it.
            int v = nextInt();
            while (v < origin || v >= bound) {
                v = nextInt();
            }
            r = v;
        }
        return r;
    }

    // Uniform in [0, bound). The retry loop is what keeps it uniform: a plain modulus favours
    // the low values whenever `bound` does not divide the range evenly.
    public long nextLong(long bound) {
        if (bound <= 0L) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long r = nextLong();
        if (r < 0L) {
            // Fold onto the non-negative half without using Math.abs, which maps
            // Long.MIN_VALUE to itself.
            r = r >>> 1;
        }
        long v = r % bound;
        while (r - v + (bound - 1L) < 0L) {
            r = nextLong() >>> 1;
            v = r % bound;
        }
        return v;
    }

    public long nextLong(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        long span = bound - origin;
        long r;
        if (span > 0L) {
            r = origin + nextLong(span);
        } else {
            long v = nextLong();
            while (v < origin || v >= bound) {
                v = nextLong();
            }
            r = v;
        }
        return r;
    }

    // Uniform in [0, bound). Clamped below `bound` because the multiplication can round up to
    // it at the top of the double's range.
    public double nextDouble(double bound) {
        if (bound <= 0.0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        double r = nextDouble() * bound;
        if (r >= bound) {
            r = 0.9999999999999999 * bound;
        }
        return r;
    }

    public double nextDouble(double origin, double bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        double r = origin + nextDouble() * (bound - origin);
        if (r >= bound) {
            r = 0.9999999999999999 * bound;
        }
        return r;
    }
}
