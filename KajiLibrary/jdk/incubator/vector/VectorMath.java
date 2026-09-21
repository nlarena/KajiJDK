package jdk.incubator.vector;

/**
 * Saturating arithmetic and unsigned arithmetic, in their scalar version.
 *
 * <h2>Why it has to exist for scalars</h2>
 *
 * <p>These operations are what the machine's vector instructions really do -- saturating instead of
 * wrapping around is the norm in signal and image hardware. Java does not have them: its arithmetic
 * always wraps around.
 *
 * <p>Having the scalar version here serves two things. Writing the reference version of a vector
 * algorithm and comparing; and the remainder, the few lanes left over at the end of an array when
 * they do not fill a whole vector.
 *
 * <h2>Saturating and wrapping around</h2>
 *
 * <p>With {@code byte}, {@code 100 + 100} gives {@code -56} in Java: it went past 127 and came back
 * from below. {@link #addSaturating(byte, byte)} gives {@code 127}.
 *
 * <p>The difference matters where the number represents a physical magnitude. In an image, a very
 * bright pixel has to stay white; with Java's arithmetic it turns <strong>black</strong>, which is
 * the worst possible error.
 *
 * <h2>Unsigned</h2>
 *
 * <p>Java has no unsigned integers, so a {@code byte} of value 200 is stored as {@code -56}.
 * Comparing two of those with {@code <} gives the wrong result. {@code minUnsigned} and {@code
 * maxUnsigned} compare as if they had no sign, which is what data coming from outside Java needs.
 *
 * <h2>State on this VM</h2>
 *
 * <p>All of this is integer arithmetic and needs nothing from the machine: it is really implemented
 * and checked against JDK 25 on every edge value.
 *
 * @since 19
 */
public final class VectorMath {

    private VectorMath() {
    }

    // ---- long ----

    /**
     * The smaller of the two, compared unsigned.
     *
     * @param a the first
     * @param b the second
     * @return the smaller
     */
    public static long minUnsigned(long a, long b) {
        return Long.compareUnsigned(a, b) < 0 ? a : b;
    }

    /**
     * The larger of the two, compared unsigned.
     *
     * @param a the first
     * @param b the second
     * @return the larger
     */
    public static long maxUnsigned(long a, long b) {
        return Long.compareUnsigned(a, b) > 0 ? a : b;
    }

    /**
     * Signed addition that saturates at the ends instead of wrapping around.
     *
     * <p>The overflow is detected by the sign: there can only be one if both addends have the same
     * sign and the result has the other. With different signs the sum never goes past.
     *
     * @param a the first
     * @param b the second
     * @return the sum, or the end of the range
     */
    public static long addSaturating(long a, long b) {
        final long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return r;
    }

    /**
     * Signed subtraction that saturates at the ends.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference, or the end of the range
     */
    public static long subSaturating(long a, long b) {
        final long r = a - b;
        if (((a ^ b) & (a ^ r)) < 0) {
            return a < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
        return r;
    }

    /**
     * Unsigned addition that saturates at the top.
     *
     * <p>It went past if the result is less than either addend, comparing unsigned. The cap is all
     * ones, which as a signed {@code long} is written {@code -1}.
     *
     * @param a the first
     * @param b the second
     * @return the sum, or the unsigned maximum
     */
    public static long addSaturatingUnsigned(long a, long b) {
        final long r = a + b;
        return Long.compareUnsigned(r, a) < 0 ? -1L : r;
    }

    /**
     * Unsigned subtraction that saturates at zero.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference, or zero
     */
    public static long subSaturatingUnsigned(long a, long b) {
        return Long.compareUnsigned(a, b) < 0 ? 0L : a - b;
    }

    // ---- int ----

    /**
     * The smaller of the two, compared unsigned.
     *
     * @param a the first
     * @param b the second
     * @return the smaller
     */
    public static int minUnsigned(int a, int b) {
        return Integer.compareUnsigned(a, b) < 0 ? a : b;
    }

    /**
     * The larger of the two, compared unsigned.
     *
     * @param a the first
     * @param b the second
     * @return the larger
     */
    public static int maxUnsigned(int a, int b) {
        return Integer.compareUnsigned(a, b) > 0 ? a : b;
    }

    /**
     * Signed addition that saturates at the ends.
     *
     * @param a the first
     * @param b the second
     * @return the sum, or the end of the range
     */
    public static int addSaturating(int a, int b) {
        final int r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            return a < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return r;
    }

    /**
     * Signed subtraction that saturates at the ends.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference, or the end of the range
     */
    public static int subSaturating(int a, int b) {
        final int r = a - b;
        if (((a ^ b) & (a ^ r)) < 0) {
            return a < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return r;
    }

    /**
     * Unsigned addition that saturates at the top.
     *
     * @param a the first
     * @param b the second
     * @return the sum, or the unsigned maximum
     */
    public static int addSaturatingUnsigned(int a, int b) {
        final int r = a + b;
        return Integer.compareUnsigned(r, a) < 0 ? -1 : r;
    }

    /**
     * Unsigned subtraction that saturates at zero.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference, or zero
     */
    public static int subSaturatingUnsigned(int a, int b) {
        return Integer.compareUnsigned(a, b) < 0 ? 0 : a - b;
    }

    // ---- short ----
    //
    // With short and byte it is computed in int and clamped at the end. It is what has to be done:
    // Java promotes both to int before operating, so the overflow of the small type cannot be
    // detected by looking at the result of the sum -- an int never overflows.

    /**
     * The smaller of the two, compared unsigned.
     *
     * @param a the first
     * @param b the second
     * @return the smaller
     */
    public static short minUnsigned(short a, short b) {
        return (a & 0xFFFF) < (b & 0xFFFF) ? a : b;
    }

    /**
     * The larger of the two, compared unsigned.
     *
     * @param a the first
     * @param b the second
     * @return the larger
     */
    public static short maxUnsigned(short a, short b) {
        return (a & 0xFFFF) > (b & 0xFFFF) ? a : b;
    }

    /**
     * Signed addition that saturates at the ends of {@code short}.
     *
     * @param a the first
     * @param b the second
     * @return the sum, or the end of the range
     */
    public static short addSaturating(short a, short b) {
        return saturate(a + b);
    }

    /**
     * Signed subtraction that saturates at the ends of {@code short}.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference, or the end of the range
     */
    public static short subSaturating(short a, short b) {
        return saturate(a - b);
    }

    /**
     * Unsigned addition that saturates at 65535.
     *
     * @param a the first
     * @param b the second
     * @return the sum, or the unsigned maximum
     */
    public static short addSaturatingUnsigned(short a, short b) {
        final int r = (a & 0xFFFF) + (b & 0xFFFF);
        return (short) (r > 0xFFFF ? 0xFFFF : r);
    }

    /**
     * Unsigned subtraction that saturates at zero.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference, or zero
     */
    public static short subSaturatingUnsigned(short a, short b) {
        final int r = (a & 0xFFFF) - (b & 0xFFFF);
        return (short) (r < 0 ? 0 : r);
    }

    private static short saturate(final int r) {
        if (r > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (r < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) r;
    }

    // ---- byte ----

    /**
     * The smaller of the two, compared unsigned.
     *
     * @param a the first
     * @param b the second
     * @return the smaller
     */
    public static byte minUnsigned(byte a, byte b) {
        return (a & 0xFF) < (b & 0xFF) ? a : b;
    }

    /**
     * The larger of the two, compared unsigned.
     *
     * @param a the first
     * @param b the second
     * @return the larger
     */
    public static byte maxUnsigned(byte a, byte b) {
        return (a & 0xFF) > (b & 0xFF) ? a : b;
    }

    /**
     * Signed addition that saturates at the ends of {@code byte}.
     *
     * @param a the first
     * @param b the second
     * @return the sum, or the end of the range
     */
    public static byte addSaturating(byte a, byte b) {
        return saturateByte(a + b);
    }

    /**
     * Signed subtraction that saturates at the ends of {@code byte}.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference, or the end of the range
     */
    public static byte subSaturating(byte a, byte b) {
        return saturateByte(a - b);
    }

    /**
     * Unsigned addition that saturates at 255.
     *
     * @param a the first
     * @param b the second
     * @return the sum, or the unsigned maximum
     */
    public static byte addSaturatingUnsigned(byte a, byte b) {
        final int r = (a & 0xFF) + (b & 0xFF);
        return (byte) (r > 0xFF ? 0xFF : r);
    }

    /**
     * Unsigned subtraction that saturates at zero.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference, or zero
     */
    public static byte subSaturatingUnsigned(byte a, byte b) {
        final int r = (a & 0xFF) - (b & 0xFF);
        return (byte) (r < 0 ? 0 : r);
    }

    private static byte saturateByte(final int r) {
        if (r > Byte.MAX_VALUE) {
            return Byte.MAX_VALUE;
        }
        if (r < Byte.MIN_VALUE) {
            return Byte.MIN_VALUE;
        }
        return (byte) r;
    }
}
