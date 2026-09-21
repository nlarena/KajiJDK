package java.util;

import java.io.Serializable;
import java.util.random.RandomGenerator;

// A pseudo-random generator: a 48-bit **linear congruential** sequence, seed = seed * 0x5DEECE66D
// + 0xB, exactly as specified by the JDK — which means a given seed produces the same numbers
// here as it does there. That reproducibility is the point of specifying an algorithm in the
// API instead of leaving it to the implementation.
//
// It is not cryptographically secure: 48 bits of state, and the next value follows from the
// previous one by arithmetic anyone can invert.
//
// Implements RandomGenerator, as the JDK's does — so anything written against the interface can
// take this generator, and the derived methods it does not override come from there. It keeps its
// own nextInt/nextInt(int)/nextDouble/nextBoolean because the JDK specifies THOSE exact bodies for
// this class: the values a given seed produces are part of Random's contract, not free to inherit.
//
// The streams (ints/longs/doubles) come from `RandomGenerator` as defaults; here there are only the
// methods whose values are part of THIS class's contract.
public class Random implements RandomGenerator, Serializable {

    private long seed;

    // The second gaussian, kept. The method below produces **two** values per turn and it would be a
    // waste to throw one away: the odd call computes the pair and returns the first, the even one
    // returns what was left. Incidentally, this is what makes the sequence depend on the parity of
    // the calls, and that is why the two fields are part of the state and not a local optimisation.
    private double nextNextGaussian;

    private boolean haveNextNextGaussian;


    /**
     * A `Random` that delegates to the given generator.
     *
     * <p>The bridge between the old API and the new, and it goes in this direction because that is
     * the one needed: there is a lot of code that **asks for a `Random`** as a parameter
     * --`Collections.shuffle`, to go no further-- and cannot be changed. With this a modern generator
     * (`SplittableRandom`, `Xoshiro256PlusPlus`) can be handed to it without touching that signature.
     *
     * <p>What comes out has **no seed of its own**: `setSeed` refuses, because the seed lives in the
     * generator behind and this object has no way of changing it.
     */
    public static Random from(java.util.random.RandomGenerator generator) {
        if (generator == null) {
            throw new NullPointerException();
        }
        return new RandomAdapter(generator);
    }

    public Random() {
        this(System.currentTimeMillis());
    }

    public Random(long seed) {
        setSeed(seed);
    }

    /**
     * The **seedless** constructor, for a subclass that has no seed.
     *
     * <p>It is needed for something that does not show until it explodes: the `Random(long)` above
     * calls `setSeed`, which is **virtual**. A subclass that refuses to accept a seed --and
     * `RandomAdapter` refuses, because its own lives in the generator behind-- would blow up during
     * its own construction, before anybody got to use it. `Random.from` used to return an object that
     * threw `UnsupportedOperationException` on being created.
     *
     * <p>`Void` has no instances, so the only possible argument is `null`: there is no way of
     * confusing this overload with the seed one, nor of calling it by accident.
     */
    Random(Void seedless) {
    }

    // The seed is scrambled and masked to 48 bits on the way in, as the JDK does.
    public synchronized void setSeed(long seed) {
        this.seed = (seed ^ 0x5DEECE66DL) & 281474976710655L;
    }

    // The generator itself: advance the state and hand back the top `bits` of it. Every public
    // method below is a thin wrapper over this one.
    protected int next(int bits) {
        seed = (seed * 0x5DEECE66DL + 0xBL) & 281474976710655L;
        return (int) (seed >>> (48 - bits));
    }

    public int nextInt() {
        return next(32);
    }

    // Uniform in [0, bound). The retry loop is what keeps it uniform: simply taking a modulus
    // would favour the low values whenever `bound` does not divide the range evenly.
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int r;
        if ((bound & -bound) == bound) {
            // A power of two divides the range exactly, so a shift suffices.
            r = (int) ((bound * (long) next(31)) >> 31);
        } else {
            int bits = next(31);
            int val = bits % bound;
            while (bits - val + (bound - 1) < 0) {
                bits = next(31);
                val = bits % bound;
            }
            r = val;
        }
        return r;
    }

    public long nextLong() {
        return ((long) next(32) << 32) + next(32);
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public float nextFloat() {
        return next(24) / ((float) (1 << 24));
    }

    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) * 1.1102230246251565E-16;
    }


    /**
     * A value from a normal of mean 0 and standard deviation 1.
     *
     * <p>It is **Marsaglia's polar method**, and it is written in that exact shape because the result
     * is part of the contract: two `Random`s with the same seed have to give the same gaussians, bit
     * for bit. The algorithm:
     *
     * <ol>
     * <li>Uniform points are thrown in the square {@code [-1,1]x[-1,1]} until one falls inside the
     *     unit circle. The ones outside are discarded --hence the loop-- and the ones inside are left
     *     with a uniform angle, which is what is needed.</li>
     * <li>With {@code s} the radius squared, the factor {@code sqrt(-2*log(s)/s)} turns that point
     *     into a pair of independent normals.</li>
     * </ol>
     *
     * <p>`StrictMath.sqrt` and `StrictMath.log` are used --not `Math`-- precisely because the value
     * is in the contract: `Math` is allowed to use a machine intrinsic and give a different ulp, and
     * here that would change the sequence.
     *
     * <p>Careful with `s == 0`: not only would it be a division by zero, `log(0)` is
     * {@code -infinity}. The loop discards it along with the ones outside the circle.
     */
    public synchronized double nextGaussian() {
        if (this.haveNextNextGaussian) {
            this.haveNextNextGaussian = false;
            return this.nextNextGaussian;
        }
        double v1 = 0.0d;
        double v2 = 0.0d;
        double s = 0.0d;
        boolean usable = false;
        while (!usable) {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
            usable = s < 1 && s != 0;
        }
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        this.nextNextGaussian = v2 * multiplier;
        this.haveNextNextGaussian = true;
        return v1 * multiplier;
    }

    public void nextBytes(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            int rnd = nextInt();
            int n = bytes.length - i;
            if (n > 4) {
                n = 4;
            }
            for (int j = 0; j < n; j++) {
                bytes[i] = (byte) rnd;
                rnd = rnd >> 8;
                i++;
            }
        }
    }
}

// The `Random` `Random.from` returns: it forwards everything to the generator behind.
//
// It extends `Random` --and does not wrap it-- because the point is handing it to code that asks for
// a `Random` by type. The inherited seed is left unused: every method that would read it is
// overridden.
final class RandomAdapter extends Random {

    private final java.util.random.RandomGenerator behind;

    RandomAdapter(java.util.random.RandomGenerator behind) {
        // The seedless constructor: the one above would call the `setSeed` below, which refuses.
        super(null);
        this.behind = behind;
    }

    // The seed lives in the generator behind, and this object has no way of changing it. Refusing is
    // the only honest thing: accepting and doing nothing would be worse.
    public synchronized void setSeed(long seed) {
        throw new UnsupportedOperationException();
    }

    public long nextLong() {
        return this.behind.nextLong();
    }

    public int nextInt() {
        return this.behind.nextInt();
    }

    public int nextInt(int bound) {
        return this.behind.nextInt(bound);
    }

    public boolean nextBoolean() {
        return this.behind.nextBoolean();
    }

    public double nextDouble() {
        return this.behind.nextDouble();
    }

    public float nextFloat() {
        return this.behind.nextFloat();
    }

    public void nextBytes(byte[] bytes) {
        this.behind.nextBytes(bytes);
    }
}
