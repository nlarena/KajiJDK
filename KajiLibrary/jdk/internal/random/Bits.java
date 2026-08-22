package jdk.internal.random;

/**
 * Bit primitives the generators in this package need and that KajiLibrary's {@code java.lang} does
 * not provide: {@code Long} has no {@code rotateLeft}, {@code Integer} has no {@code rotateLeft},
 * and {@code Math} has no {@code unsignedMultiplyHigh}.
 *
 * <p>They live here rather than in {@link jdk.internal.util.random.RandomSupport} because that
 * class has a JDK counterpart, and adding members the JDK's version does not declare would make our
 * public surface a SUPERSET — the one thing the API-shape gate rejects outright. This class has no
 * counterpart, so its shape is ours to choose.
 */
final class Bits {

    private Bits() {
    }

    /**
     * Rotates a {@code long} left by {@code n} bits.
     *
     * @param x the value to rotate
     * @param n the rotation distance
     * @return the rotated value
     * @implSpec The right half must shift UNSIGNED. An arithmetic shift would smear the sign bit
     *           across the rotated result.
     */
    static long rotateLeft(long x, int n) {
        return (x << n) | (x >>> (64 - n));
    }

    /**
     * Rotates an {@code int} left by {@code n} bits.
     *
     * @param x the value to rotate
     * @param n the rotation distance
     * @return the rotated value
     */
    static int rotateLeft32(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    /**
     * Returns the high 64 bits of the UNSIGNED product of two {@code long} values.
     *
     * <p>This is the carry a 128-bit LCG needs and that Java's {@code *} throws away.
     *
     * @param x the first factor
     * @param y the second factor
     * @return the high half of the 128-bit product
     * @implSpec Both operands are split into 32-bit halves and the result reassembled by hand. The
     *           whole difficulty is that Java has no unsigned type, so every half must be masked
     *           and every partial product carried explicitly. ({@code Math.unsignedMultiplyHigh}
     *           in the JDK.)
     */
    static long unsignedMultiplyHigh(long x, long y) {
        long x0 = x & 4294967295L;
        long x1 = x >>> 32;
        long y0 = y & 4294967295L;
        long y1 = y >>> 32;
        long p00 = x0 * y0;
        long p01 = x0 * y1;
        long p10 = x1 * y0;
        long p11 = x1 * y1;
        long middle = p10 + (p00 >>> 32) + (p01 & 4294967295L);
        return p11 + (middle >>> 32) + (p01 >>> 32);
    }

    /**
     * Compares two {@code long} values as UNSIGNED.
     *
     * @param a the first value
     * @param b the second value
     * @return a negative value, zero or a positive value as {@code a} is less than, equal to or
     *         greater than {@code b}, treating both as unsigned
     * @implSpec This is how a carry is detected: a sum wrapped exactly when it came out SMALLER
     *           than one of its addends, and "smaller" there has to be unsigned. A signed
     *           comparison would miss every case that crossed 2<sup>63</sup>, which is half of them.
     */
    static int compareUnsigned(long a, long b) {
        long fa = a + -9223372036854775807L - 1L;
        long fb = b + -9223372036854775807L - 1L;
        if (fa < fb) {
            return -1;
        }
        if (fa > fb) {
            return 1;
        }
        return 0;
    }

    /**
     * Returns 2<sup>64</sup> divided by the golden ratio, {@code 0x9e3779b97f4a7c15}.
     *
     * @return the constant
     * @implSpec An odd constant with well-distributed bits, used as a seed increment so that
     *           consecutive seeds decorrelate. A method rather than a {@code static final long},
     *           which our compiler leaves uninitialized (finding #112).
     */
    static long goldenRatio64() {
        return -7046029254386353131L;
    }

    /**
     * Returns 2<sup>64</sup> divided by the silver ratio, {@code 0x6a09e667f3bcc909}.
     *
     * @return the constant
     */
    static long silverRatio64() {
        return 7640891576956012809L;
    }
}
