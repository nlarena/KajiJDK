package java.util;

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
// Subset: the stream methods (ints/longs/doubles) and nextGaussian are omitted.
public class Random implements RandomGenerator {

    private long seed;

    public Random() {
        this(System.currentTimeMillis());
    }

    public Random(long seed) {
        setSeed(seed);
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
