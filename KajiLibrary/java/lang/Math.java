package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.math.BigInteger;

/**
 * KajiLibrary's java.lang.Math.
 *
 * <p>What is here is the half that has <em>exact</em> answers: integer arithmetic, the
 * overflow-checking operations, division that rounds the way you asked rather than the way C
 * does, and the handful of double operations whose result is fixed by the bit pattern rather
 * than by an approximation. Every one of them can be checked against the reference and be either
 * right or wrong, with nothing in between.
 *
 * <p>The transcendental half -- {@code sin}, {@code exp}, {@code pow}, {@code sqrt} and their
 * relatives -- is deliberately absent rather than pending. Those are not "more arithmetic": they
 * are approximations whose contract is stated in <em>ulps</em>, and the JDK meets it by shipping
 * a specific implementation (fdlibm) rather than by computing a defined value. Writing something
 * that merely looks close would produce a library that passes casual use and disagrees with the
 * reference in the last bit, which is the one place a math library must not disagree.
 *
 * <p>None of this needs the VM. The class had three {@code native} methods before -- int
 * {@code abs}, {@code max} and {@code min} -- on the grounds that HotSpot intrinsifies them;
 * that is an argument about speed, and it cost fidelity, since the JDK does not declare them
 * native and so the modifiers differed. They are Java here, which is what they always were in
 * the reference.
 */
public final class Math {

    // Non-instantiable: Math is a static-only utility (matches the JDK, which hides
    // the constructor). Without this, javac would synthesize a *public* default one.
    private Math() {
    }

    /** The base of the natural logarithms. */
    public static final double E = 2.718281828459045d;

    /** The ratio of a circle's circumference to its diameter. */
    public static final double PI = 3.141592653589793d;

    /** The ratio of a circle's circumference to its radius -- two PI. */
    public static final double TAU = 6.283185307179586d;

    // The extremes, computed rather than named. `1 << 31` IS Integer.MIN_VALUE and `1L << 63` is
    // Long.MIN_VALUE, exactly, and writing them this way keeps this class from depending on
    // constants of another class to define its own overflow checks.
    private static final int INT_MIN = 1 << 31;
    private static final int INT_MAX = ~INT_MIN;
    private static final long LONG_MIN = 1L << 63;
    private static final long LONG_MAX = ~LONG_MIN;

    // ---- magnitude ----

    /**
     * The absolute value.
     *
     * <p>Note the one input it cannot answer for: the most negative int has no positive
     * counterpart, so {@code abs(Integer.MIN_VALUE)} is itself. That is the specified behaviour
     * and it is a trap; {@link #absExact(int)} is the version that refuses instead.
     *
     * @param a the value
     */
    public static int abs(int a) {
        if (a < 0) {
            return -a;
        }
        return a;
    }

    /**
     * The absolute value.
     *
     * @param a the value
     * @see #abs(int) for what happens at the most negative value
     */
    public static long abs(long a) {
        if (a < 0L) {
            return -a;
        }
        return a;
    }

    /**
     * The absolute value.
     *
     * <p>Written as a subtraction rather than a sign test so that the two awkward inputs come out
     * right without being special cases: {@code -0.0} yields {@code +0.0}, and NaN -- which
     * compares false against everything, including zero -- falls through unchanged.
     *
     * @param a the value
     */
    public static double abs(double a) {
        if (a <= 0.0d) {
            return 0.0d - a;
        }
        return a;
    }

    /**
     * The absolute value.
     *
     * @param a the value
     */
    public static float abs(float a) {
        if (a <= 0.0f) {
            return 0.0f - a;
        }
        return a;
    }

    /**
     * The absolute value, refusing the one it cannot represent.
     *
     * @param a the value
     * @throws ArithmeticException if {@code a} is the most negative int
     */
    public static int absExact(int a) {
        if (a == Math.INT_MIN) {
            throw new ArithmeticException(
                    "Overflow to represent absolute value of Integer.MIN_VALUE");
        }
        return Math.abs(a);
    }

    /**
     * The absolute value, refusing the one it cannot represent.
     *
     * @param a the value
     * @throws ArithmeticException if {@code a} is the most negative long
     */
    public static long absExact(long a) {
        if (a == Math.LONG_MIN) {
            throw new ArithmeticException("Overflow to represent absolute value of Long.MIN_VALUE");
        }
        return Math.abs(a);
    }

    // ---- extremes ----

    /**
     * The greater of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static int max(int a, int b) {
        if (a >= b) {
            return a;
        }
        return b;
    }

    /**
     * The greater of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static long max(long a, long b) {
        if (a >= b) {
            return a;
        }
        return b;
    }

    /**
     * The greater of two values.
     *
     * <p>Two answers here are not what {@code >} would give, and both are deliberate. NaN wins
     * over everything, because a comparison against NaN is meaningless rather than false. And
     * {@code +0.0} wins over {@code -0.0}, which {@code >} cannot see at all -- the two compare
     * equal and differ only in a bit.
     *
     * @param a one value
     * @param b the other
     */
    public static double max(double a, double b) {
        if (a != a) {
            return a;
        }
        if (a == 0.0d && b == 0.0d) {
            // Both are zero and they may differ in sign. The positive one has all bits clear.
            if (Double.doubleToLongBits(a) == 0L) {
                return a;
            }
            return b;
        }
        if (a >= b) {
            return a;
        }
        return b;
    }

    /**
     * The greater of two values.
     *
     * @param a one value
     * @param b the other
     * @see #max(double, double) for the NaN and signed-zero rules
     */
    public static float max(float a, float b) {
        if (a != a) {
            return a;
        }
        if (a == 0.0f && b == 0.0f) {
            // Widening a float to a double is exact and keeps the sign of a zero, so the same
            // bit test serves; `java.lang.Float` has no bit accessor of its own here.
            if (Double.doubleToLongBits((double) a) == 0L) {
                return a;
            }
            return b;
        }
        if (a >= b) {
            return a;
        }
        return b;
    }

    /**
     * The smaller of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static int min(int a, int b) {
        if (a <= b) {
            return a;
        }
        return b;
    }

    /**
     * The smaller of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static long min(long a, long b) {
        if (a <= b) {
            return a;
        }
        return b;
    }

    /**
     * The smaller of two values.
     *
     * @param a one value
     * @param b the other
     * @see #max(double, double) for the NaN and signed-zero rules, mirrored
     */
    public static double min(double a, double b) {
        if (a != a) {
            return a;
        }
        if (a == 0.0d && b == 0.0d) {
            if (Double.doubleToLongBits(a) != 0L) {
                return a;
            }
            return b;
        }
        if (a <= b) {
            return a;
        }
        return b;
    }

    /**
     * The smaller of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static float min(float a, float b) {
        if (a != a) {
            return a;
        }
        if (a == 0.0f && b == 0.0f) {
            if (Double.doubleToLongBits((double) a) != 0L) {
                return a;
            }
            return b;
        }
        if (a <= b) {
            return a;
        }
        return b;
    }

    // ---- clamping ----

    /**
     * {@code value} confined to {@code [min, max]}.
     *
     * @param value the value to confine
     * @param min the lower bound
     * @param max the upper bound
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static int clamp(long value, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        long confined = Math.min(Math.max(value, (long) min), (long) max);
        return (int) confined;
    }

    /**
     * {@code value} confined to {@code [min, max]}.
     *
     * @param value the value to confine
     * @param min the lower bound
     * @param max the upper bound
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static long clamp(long value, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return Math.min(Math.max(value, min), max);
    }

    /**
     * {@code value} confined to {@code [min, max]}.
     *
     * <p>A NaN {@code value} stays NaN; a NaN bound is an error, because a bound that cannot be
     * compared is not a bound. The bounds may be {@code -0.0} and {@code +0.0} in that order,
     * which is a real interval of two values even though they compare equal.
     *
     * @param value the value to confine
     * @param min the lower bound
     * @param max the upper bound
     * @throws IllegalArgumentException if either bound is NaN, or if {@code min > max}
     */
    public static double clamp(double value, double min, double max) {
        if (!(min < max)) {
            if (min != min) {
                throw new IllegalArgumentException("min is NaN");
            }
            if (max != max) {
                throw new IllegalArgumentException("max is NaN");
            }
            if (Math.compare(min, max) > 0) {
                throw new IllegalArgumentException(min + " > " + max);
            }
        }
        double lifted = Math.max(value, min);
        return Math.min(max, lifted);
    }

    /**
     * {@code value} confined to {@code [min, max]}.
     *
     * @param value the value to confine
     * @param min the lower bound
     * @param max the upper bound
     * @throws IllegalArgumentException if either bound is NaN, or if {@code min > max}
     * @see #clamp(double, double, double)
     */
    public static float clamp(float value, float min, float max) {
        if (!(min < max)) {
            if (min != min) {
                throw new IllegalArgumentException("min is NaN");
            }
            if (max != max) {
                throw new IllegalArgumentException("max is NaN");
            }
            if (Math.compare((double) min, (double) max) > 0) {
                throw new IllegalArgumentException(min + " > " + max);
            }
        }
        float lifted = Math.max(value, min);
        return Math.min(max, lifted);
    }

    // Total order over doubles, the way `Double.compare` defines it: -0.0 sorts below +0.0, and
    // the two are distinguished by their bits because no comparison operator can see the
    // difference. NaN never reaches here -- every caller rejects it first.
    private static int compare(double a, double b) {
        if (a < b) {
            return -1;
        }
        if (a > b) {
            return 1;
        }
        long ab = Double.doubleToLongBits(a);
        long bb = Double.doubleToLongBits(b);
        if (ab == bb) {
            return 0;
        }
        if (ab < bb) {
            return -1;
        }
        return 1;
    }

    // ---- division that rounds where you asked ----
    //
    // Java's `/` truncates toward zero, which is the C rule and is almost never the one a
    // program wants for negative operands: `-7 / 2` is -3, so the remainder is -1 and the
    // sequence of remainders is not periodic. The two families below round toward negative and
    // positive infinity respectively, which restores that periodicity.

    /**
     * {@code x / y}, rounded toward negative infinity.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static int floorDiv(int x, int y) {
        int q = x / y;
        // The quotient was truncated toward zero. It needs correcting exactly when the signs
        // differ AND the division was not exact.
        if ((x ^ y) < 0 && q * y != x) {
            return q - 1;
        }
        return q;
    }

    /**
     * {@code x / y}, rounded toward negative infinity.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static long floorDiv(long x, long y) {
        long q = x / y;
        if ((x ^ y) < 0L && q * y != x) {
            return q - 1L;
        }
        return q;
    }

    /**
     * {@code x / y}, rounded toward negative infinity.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static long floorDiv(long x, int y) {
        return Math.floorDiv(x, (long) y);
    }

    /**
     * The remainder left by {@link #floorDiv(int, int)}, which takes the sign of the divisor.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static int floorMod(int x, int y) {
        int q = Math.floorDiv(x, y);
        return x - q * y;
    }

    /**
     * The remainder left by {@link #floorDiv(long, long)}, which takes the sign of the divisor.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static long floorMod(long x, long y) {
        long q = Math.floorDiv(x, y);
        return x - q * y;
    }

    /**
     * The remainder left by {@link #floorDiv(long, int)}.
     *
     * <p>An {@code int} result even though the dividend is a long, and correctly so: the
     * remainder is bounded by the divisor, which is an int.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static int floorMod(long x, int y) {
        long r = Math.floorMod(x, (long) y);
        return (int) r;
    }

    /**
     * {@code x / y}, rounded toward positive infinity.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static int ceilDiv(int x, int y) {
        int q = x / y;
        if ((x ^ y) >= 0 && q * y != x) {
            return q + 1;
        }
        return q;
    }

    /**
     * {@code x / y}, rounded toward positive infinity.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static long ceilDiv(long x, long y) {
        long q = x / y;
        if ((x ^ y) >= 0L && q * y != x) {
            return q + 1L;
        }
        return q;
    }

    /**
     * {@code x / y}, rounded toward positive infinity.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static long ceilDiv(long x, int y) {
        return Math.ceilDiv(x, (long) y);
    }

    /**
     * The remainder left by {@link #ceilDiv(int, int)}, which takes the sign opposite the divisor.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static int ceilMod(int x, int y) {
        int q = Math.ceilDiv(x, y);
        return x - q * y;
    }

    /**
     * The remainder left by {@link #ceilDiv(long, long)}.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static long ceilMod(long x, long y) {
        long q = Math.ceilDiv(x, y);
        return x - q * y;
    }

    /**
     * The remainder left by {@link #ceilDiv(long, int)}.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero
     */
    public static int ceilMod(long x, int y) {
        long r = Math.ceilMod(x, (long) y);
        return (int) r;
    }

    // ---- the exact family ----
    //
    // Every one of these computes what its plain operator computes and then refuses the answer
    // if it wrapped. Wrapping is not an error in Java -- it is the defined behaviour -- which is
    // exactly why these exist: silent wrap-around is the right default for a hash and the wrong
    // one for a length, and only the caller knows which it is writing.

    /**
     * {@code x + y}, refusing to wrap.
     *
     * @param x one addend
     * @param y the other
     * @throws ArithmeticException on overflow
     */
    public static int addExact(int x, int y) {
        int r = x + y;
        // It overflowed exactly when both addends have the sign opposite the result.
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    /**
     * {@code x + y}, refusing to wrap.
     *
     * @param x one addend
     * @param y the other
     * @throws ArithmeticException on overflow
     */
    public static long addExact(long x, long y) {
        long r = x + y;
        if (((x ^ r) & (y ^ r)) < 0L) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /**
     * {@code x - y}, refusing to wrap.
     *
     * @param x the minuend
     * @param y the subtrahend
     * @throws ArithmeticException on overflow
     */
    public static int subtractExact(int x, int y) {
        int r = x - y;
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    /**
     * {@code x - y}, refusing to wrap.
     *
     * @param x the minuend
     * @param y the subtrahend
     * @throws ArithmeticException on overflow
     */
    public static long subtractExact(long x, long y) {
        long r = x - y;
        if (((x ^ y) & (x ^ r)) < 0L) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /**
     * {@code x * y}, refusing to wrap.
     *
     * @param x one factor
     * @param y the other
     * @throws ArithmeticException on overflow
     */
    public static int multiplyExact(int x, int y) {
        // The product of two ints always fits in a long, so the check is a range test rather
        // than a bit trick.
        long r = (long) x * (long) y;
        int narrowed = (int) r;
        if ((long) narrowed != r) {
            throw new ArithmeticException("integer overflow");
        }
        return narrowed;
    }

    /**
     * {@code x * y}, refusing to wrap.
     *
     * @param x one factor
     * @param y the other
     * @throws ArithmeticException on overflow
     */
    public static long multiplyExact(long x, long y) {
        long r = x * y;
        long high = Math.multiplyHigh(x, y);
        // For the product to fit in 64 bits, the upper half must be nothing but a copy of the
        // sign of the lower half.
        if (high != (r >> 63)) {
            throw new ArithmeticException("long overflow");
        }
        // One case the check above cannot see: the most negative long times -1 is itself.
        if (x == Math.LONG_MIN && y == -1L) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /**
     * {@code x * y}, refusing to wrap.
     *
     * @param x one factor
     * @param y the other
     * @throws ArithmeticException on overflow
     */
    public static long multiplyExact(long x, int y) {
        return Math.multiplyExact(x, (long) y);
    }

    /**
     * {@code a + 1}, refusing to wrap.
     *
     * @param a the value
     * @throws ArithmeticException on overflow
     */
    public static int incrementExact(int a) {
        if (a == Math.INT_MAX) {
            throw new ArithmeticException("integer overflow");
        }
        return a + 1;
    }

    /**
     * {@code a + 1}, refusing to wrap.
     *
     * @param a the value
     * @throws ArithmeticException on overflow
     */
    public static long incrementExact(long a) {
        if (a == Math.LONG_MAX) {
            throw new ArithmeticException("long overflow");
        }
        return a + 1L;
    }

    /**
     * {@code a - 1}, refusing to wrap.
     *
     * @param a the value
     * @throws ArithmeticException on overflow
     */
    public static int decrementExact(int a) {
        if (a == Math.INT_MIN) {
            throw new ArithmeticException("integer overflow");
        }
        return a - 1;
    }

    /**
     * {@code a - 1}, refusing to wrap.
     *
     * @param a the value
     * @throws ArithmeticException on overflow
     */
    public static long decrementExact(long a) {
        if (a == Math.LONG_MIN) {
            throw new ArithmeticException("long overflow");
        }
        return a - 1L;
    }

    /**
     * {@code -a}, refusing to wrap.
     *
     * @param a the value
     * @throws ArithmeticException if {@code a} is the most negative int
     */
    public static int negateExact(int a) {
        if (a == Math.INT_MIN) {
            throw new ArithmeticException("integer overflow");
        }
        return -a;
    }

    /**
     * {@code -a}, refusing to wrap.
     *
     * @param a the value
     * @throws ArithmeticException if {@code a} is the most negative long
     */
    public static long negateExact(long a) {
        if (a == Math.LONG_MIN) {
            throw new ArithmeticException("long overflow");
        }
        return -a;
    }

    /**
     * {@code x / y}, refusing the one quotient that does not fit.
     *
     * <p>Division overflows exactly once, and it surprises people: the most negative value
     * divided by -1 has no representation, because the positive range is one short.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero, or on overflow
     */
    public static int divideExact(int x, int y) {
        int q = x / y;
        if (x == Math.INT_MIN && y == -1) {
            throw new ArithmeticException("integer overflow");
        }
        return q;
    }

    /**
     * {@code x / y}, refusing the one quotient that does not fit.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero, or on overflow
     */
    public static long divideExact(long x, long y) {
        long q = x / y;
        if (x == Math.LONG_MIN && y == -1L) {
            throw new ArithmeticException("long overflow");
        }
        return q;
    }

    /**
     * {@link #floorDiv(int, int)}, refusing the quotient that does not fit.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero, or on overflow
     */
    public static int floorDivExact(int x, int y) {
        if (x == Math.INT_MIN && y == -1) {
            throw new ArithmeticException("integer overflow");
        }
        return Math.floorDiv(x, y);
    }

    /**
     * {@link #floorDiv(long, long)}, refusing the quotient that does not fit.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero, or on overflow
     */
    public static long floorDivExact(long x, long y) {
        if (x == Math.LONG_MIN && y == -1L) {
            throw new ArithmeticException("long overflow");
        }
        return Math.floorDiv(x, y);
    }

    /**
     * {@link #ceilDiv(int, int)}, refusing the quotient that does not fit.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero, or on overflow
     */
    public static int ceilDivExact(int x, int y) {
        if (x == Math.INT_MIN && y == -1) {
            throw new ArithmeticException("integer overflow");
        }
        return Math.ceilDiv(x, y);
    }

    /**
     * {@link #ceilDiv(long, long)}, refusing the quotient that does not fit.
     *
     * @param x the dividend
     * @param y the divisor
     * @throws ArithmeticException if {@code y} is zero, or on overflow
     */
    public static long ceilDivExact(long x, long y) {
        if (x == Math.LONG_MIN && y == -1L) {
            throw new ArithmeticException("long overflow");
        }
        return Math.ceilDiv(x, y);
    }

    /**
     * {@code value} as an int, refusing to truncate.
     *
     * @param value the long to narrow
     * @throws ArithmeticException if it does not fit
     */
    public static int toIntExact(long value) {
        int narrowed = (int) value;
        if ((long) narrowed != value) {
            throw new ArithmeticException("integer overflow");
        }
        return narrowed;
    }

    /**
     * {@code b} raised to {@code e}, refusing to wrap.
     *
     * @param b the base
     * @param e the exponent; must not be negative
     * @throws ArithmeticException if the exponent is negative, or on overflow
     */
    public static int powExact(int b, int e) {
        if (e < 0) {
            throw new ArithmeticException("negative exponent");
        }
        int result = 1;
        int base = b;
        int exp = e;
        // Square-and-multiply, so the number of multiplications is logarithmic. Each one is
        // checked, including the squarings -- and the squaring is skipped on the last round,
        // because a base that would overflow when squared is fine if it is never used again.
        while (exp > 0) {
            if ((exp & 1) != 0) {
                result = Math.multiplyExact(result, base);
            }
            exp = exp >>> 1;
            if (exp > 0) {
                base = Math.multiplyExact(base, base);
            }
        }
        return result;
    }

    /**
     * {@code b} raised to {@code e}, refusing to wrap.
     *
     * @param b the base
     * @param e the exponent; must not be negative
     * @throws ArithmeticException if the exponent is negative, or on overflow
     */
    public static long powExact(long b, int e) {
        if (e < 0) {
            throw new ArithmeticException("negative exponent");
        }
        long result = 1L;
        long base = b;
        int exp = e;
        while (exp > 0) {
            if ((exp & 1) != 0) {
                result = Math.multiplyExact(result, base);
            }
            exp = exp >>> 1;
            if (exp > 0) {
                base = Math.multiplyExact(base, base);
            }
        }
        return result;
    }

    // ---- the unsigned family ----
    //
    // Java has no unsigned integer type, so these read the same bits as a value in
    // [0, 2^32) or [0, 2^64) instead. The result comes back in the same signed type, holding the
    // same bits: `unsignedPowExact(3, 20)` answers -808182895, which IS 3486784401 read the
    // other way.

    /**
     * {@code x * y} with both read as unsigned, refusing to wrap.
     *
     * @param x one factor
     * @param y the other
     * @throws ArithmeticException on overflow
     */
    public static int unsignedMultiplyExact(int x, int y) {
        long r = (x & 0xFFFFFFFFL) * (y & 0xFFFFFFFFL);
        if ((r >>> 32) != 0L) {
            throw new ArithmeticException("unsigned integer overflow");
        }
        return (int) r;
    }

    /**
     * {@code x * y} with both read as unsigned, refusing to wrap.
     *
     * @param x one factor
     * @param y the other
     * @throws ArithmeticException on overflow
     */
    public static long unsignedMultiplyExact(long x, long y) {
        long high = Math.unsignedMultiplyHigh(x, y);
        if (high != 0L) {
            throw new ArithmeticException("long overflow");
        }
        return x * y;
    }

    /**
     * {@code x * y} with both read as unsigned, refusing to wrap.
     *
     * @param x one factor
     * @param y the other
     * @throws ArithmeticException on overflow
     */
    public static long unsignedMultiplyExact(long x, int y) {
        return Math.unsignedMultiplyExact(x, y & 0xFFFFFFFFL);
    }

    /**
     * {@code b} raised to {@code e}, both read as unsigned, refusing to wrap.
     *
     * @param b the base
     * @param e the exponent; must not be negative
     * @throws ArithmeticException if the exponent is negative, or on overflow
     */
    public static int unsignedPowExact(int b, int e) {
        if (e < 0) {
            throw new ArithmeticException("negative exponent");
        }
        int result = 1;
        int base = b;
        int exp = e;
        while (exp > 0) {
            if ((exp & 1) != 0) {
                result = Math.unsignedMultiplyExact(result, base);
            }
            exp = exp >>> 1;
            if (exp > 0) {
                base = Math.unsignedMultiplyExact(base, base);
            }
        }
        return result;
    }

    /**
     * {@code b} raised to {@code e}, the base read as unsigned, refusing to wrap.
     *
     * @param b the base
     * @param e the exponent; must not be negative
     * @throws ArithmeticException if the exponent is negative, or on overflow
     */
    public static long unsignedPowExact(long b, int e) {
        if (e < 0) {
            throw new ArithmeticException("negative exponent");
        }
        long result = 1L;
        long base = b;
        int exp = e;
        while (exp > 0) {
            if ((exp & 1) != 0) {
                result = Math.unsignedMultiplyExact(result, base);
            }
            exp = exp >>> 1;
            if (exp > 0) {
                base = Math.unsignedMultiplyExact(base, base);
            }
        }
        return result;
    }

    // ---- the wide product ----

    /**
     * The exact product of two ints, which always fits in a long.
     *
     * @param x one factor
     * @param y the other
     */
    public static long multiplyFull(int x, int y) {
        return (long) x * (long) y;
    }

    /**
     * The high 64 bits of the 128-bit product of two longs.
     *
     * <p>The half that {@code x * y} throws away. Computed from four 32-bit partial products,
     * because there is no 128-bit type to hold the whole thing and carry it.
     *
     * @param x one factor
     * @param y the other
     */
    public static long multiplyHigh(long x, long y) {
        long x1 = x >> 32;
        long x2 = x & 0xFFFFFFFFL;
        long y1 = y >> 32;
        long y2 = y & 0xFFFFFFFFL;
        long z2 = x2 * y2;
        long t = x1 * y2 + (z2 >>> 32);
        long z1 = t & 0xFFFFFFFFL;
        long z0 = t >> 32;
        z1 = z1 + x2 * y1;
        return x1 * y1 + z0 + (z1 >> 32);
    }

    /**
     * The high 64 bits of the 128-bit product, with both factors read as unsigned.
     *
     * <p>The signed result plus a correction: a factor whose top bit is set was read as
     * {@code v - 2^64} by {@link #multiplyHigh}, so the other factor has to be added back once
     * for each of them.
     *
     * @param x one factor
     * @param y the other
     */
    public static long unsignedMultiplyHigh(long x, long y) {
        long signed = Math.multiplyHigh(x, y);
        long corrected = signed + (y & (x >> 63));
        return corrected + (x & (y >> 63));
    }

    // ---- rounding to an integer ----

    /**
     * {@code a} rounded to the nearest long, with ties going up.
     *
     * <p>Not {@code floor(a + 0.5)}, though it reads like it should be. Adding a half can itself
     * round up, which makes the largest double below one half answer 1 -- Java said so until 7
     * and it was a bug. This reads the exponent, shifts the significand into place and rounds
     * there, so the addition never happens.
     *
     * @param a the value; NaN gives 0
     */
    public static long round(double a) {
        if (a != a) {
            return 0L;
        }
        long bits = Double.doubleToLongBits(a);
        long biasedExp = (bits & 0x7FF0000000000000L) >> 52;
        // 1074 = (significand width - 2) + exponent bias, i.e. how far to shift so that one
        // fractional bit is left in the low position to round on.
        long shift = 1074L - biasedExp;
        if ((shift & -64L) == 0L) {
            long significand = (bits & 0x000FFFFFFFFFFFFFL) | 0x0010000000000000L;
            if (bits < 0L) {
                significand = -significand;
            }
            return ((significand >> (int) shift) + 1L) >> 1;
        }
        // Outside that range the value is either already integral, or so large that the
        // conversion saturates -- and a plain cast does both.
        return (long) a;
    }

    /**
     * {@code a} rounded to the nearest int, with ties going up.
     *
     * <p>Routed through the double form, which is exact: every float widens to a double without
     * loss, so the nearest integer cannot change. Only the saturation has to be redone, because
     * it must clamp to the int range rather than the long one.
     *
     * @param a the value; NaN gives 0
     */
    public static int round(float a) {
        long r = Math.round((double) a);
        if (r > (long) Math.INT_MAX) {
            return Math.INT_MAX;
        }
        if (r < (long) Math.INT_MIN) {
            return Math.INT_MIN;
        }
        return (int) r;
    }

    // ---- angles ----
    //
    // Both conversions are ONE multiplication by a constant, and the constant is written out
    // rather than computed. Writing `angdeg / 180.0 * PI` instead looks equivalent and is not:
    // it rounds twice, and the two roundings disagree with the single one on roughly a fifth of
    // inputs -- by one ulp, which is exactly the size of error that survives every plausible
    // test and shows up in a bit comparison. The constants below are the correctly rounded
    // values of pi/180 and 180/pi, which is what makes one multiplication enough.

    /** The factor that turns degrees into radians: pi/180, correctly rounded. */
    private static final double DEGREES_TO_RADIANS = 0.017453292519943295d;

    /** The factor that turns radians into degrees: 180/pi, correctly rounded. */
    private static final double RADIANS_TO_DEGREES = 57.29577951308232d;

    /**
     * Degrees as radians.
     *
     * @param angdeg an angle in degrees
     */
    public static double toRadians(double angdeg) {
        return angdeg * Math.DEGREES_TO_RADIANS;
    }

    /**
     * Radians as degrees.
     *
     * @param angrad an angle in radians
     */
    public static double toDegrees(double angrad) {
        return angrad * Math.RADIANS_TO_DEGREES;
    }

    // ---- looking at the bits ----
    //
    // These do not compute anything in the ordinary sense: they read the representation. What
    // the next representable value is, how far apart two neighbours are, what the exponent says
    // -- none of that is a property of the number, it is a property of the FORMAT holding it.
    // Which is why they were impossible until `Double` and `Float` grew their bit accessors,
    // and why every one of them is exact.

    /**
     * The unbiased exponent of {@code d}.
     *
     * @param d the value; a NaN or infinity answers one above the maximum, and a zero or
     *        subnormal one below the minimum, so the two extremes are distinguishable
     */
    public static int getExponent(double d) {
        long bits = Double.doubleToRawLongBits(d);
        int biased = (int) ((bits >> 52) & 0x7ffL);
        return biased - 1023;
    }

    /**
     * The unbiased exponent of {@code f}.
     *
     * @param f the value
     * @see #getExponent(double)
     */
    public static int getExponent(float f) {
        int bits = Float.floatToRawIntBits(f);
        int biased = (bits >> 23) & 0xff;
        return biased - 127;
    }

    /**
     * {@code magnitude} with the sign of {@code sign}.
     *
     * <p>Reads the sign BIT rather than comparing against zero, which is the whole point: it
     * works for {@code -0.0}, and it works for a NaN, neither of which any comparison can tell
     * apart from its positive twin.
     *
     * @param magnitude where the value comes from
     * @param sign where the sign comes from
     */
    public static double copySign(double magnitude, double sign) {
        long signBit = Double.doubleToRawLongBits(sign) & 0x8000000000000000L;
        long rest = Double.doubleToRawLongBits(magnitude) & 0x7fffffffffffffffL;
        return Double.longBitsToDouble(signBit | rest);
    }

    /**
     * {@code magnitude} with the sign of {@code sign}.
     *
     * @param magnitude where the value comes from
     * @param sign where the sign comes from
     * @see #copySign(double, double)
     */
    public static float copySign(float magnitude, float sign) {
        int signBit = Float.floatToRawIntBits(sign) & 0x80000000;
        int rest = Float.floatToRawIntBits(magnitude) & 0x7fffffff;
        return Float.intBitsToFloat(signBit | rest);
    }

    /**
     * The sign as a value: -1.0, 0.0 or 1.0.
     *
     * <p>A zero answers ITSELF, not 0.0 -- so {@code signum(-0.0)} is {@code -0.0}. That is not
     * a quirk: the sign of a negative zero is negative, and losing it here would lose it
     * everywhere downstream.
     *
     * @param d the value
     */
    public static double signum(double d) {
        if (d == 0.0d || d != d) {
            return d;
        }
        return Math.copySign(1.0d, d);
    }

    /**
     * The sign as a value: -1.0, 0.0 or 1.0.
     *
     * @param f the value
     * @see #signum(double)
     */
    public static float signum(float f) {
        if (f == 0.0f || f != f) {
            return f;
        }
        return Math.copySign(1.0f, f);
    }

    /**
     * The distance from {@code d} to the next representable value away from zero.
     *
     * <p>The size of one step of the format at that magnitude -- which grows with the exponent,
     * because a floating-point number has a fixed number of significant BITS and not a fixed
     * precision. Near 1.0 it is about 2.2e-16; near 1e300 it is about 1e284.
     *
     * @param d the value
     */
    public static double ulp(double d) {
        int exponent = Math.getExponent(d);
        if (exponent == 1024) {
            return Math.abs(d); // NaN or infinity
        }
        if (exponent == -1023) {
            return Double.MIN_VALUE; // zero or subnormal: the step is the smallest there is
        }
        int stepExponent = exponent - 52;
        if (stepExponent >= -1022) {
            return Math.twoTo(stepExponent);
        }
        // The step itself is subnormal, so it cannot be written as a normal power of two.
        return Double.longBitsToDouble(1L << (stepExponent + 1074));
    }

    /**
     * The distance from {@code f} to the next representable value away from zero.
     *
     * @param f the value
     * @see #ulp(double)
     */
    public static float ulp(float f) {
        int exponent = Math.getExponent(f);
        if (exponent == 128) {
            return Math.abs(f);
        }
        if (exponent == -127) {
            return Float.MIN_VALUE;
        }
        int stepExponent = exponent - 23;
        if (stepExponent >= -126) {
            return Math.twoToF(stepExponent);
        }
        return Float.intBitsToFloat(1 << (stepExponent + 149));
    }

    /**
     * The next representable value above {@code d}.
     *
     * <p>Adding one to the BITS, which works because IEEE-754 lays consecutive values out as
     * consecutive integers -- an ordering property the format was designed to have. The sign
     * decides the direction, and {@code -0.0} is nudged to {@code +0.0} first so that the step
     * from either zero goes the same way.
     *
     * @param d the value
     */
    public static double nextUp(double d) {
        if (d != d || d == Double.POSITIVE_INFINITY) {
            return d;
        }
        double from = d + 0.0d;
        long bits = Double.doubleToRawLongBits(from);
        if (from >= 0.0d) {
            return Double.longBitsToDouble(bits + 1L);
        }
        return Double.longBitsToDouble(bits - 1L);
    }

    /**
     * The next representable value above {@code f}.
     *
     * @param f the value
     * @see #nextUp(double)
     */
    public static float nextUp(float f) {
        if (f != f || f == Float.POSITIVE_INFINITY) {
            return f;
        }
        float from = f + 0.0f;
        int bits = Float.floatToRawIntBits(from);
        if (from >= 0.0f) {
            return Float.intBitsToFloat(bits + 1);
        }
        return Float.intBitsToFloat(bits - 1);
    }

    /**
     * The next representable value below {@code d}.
     *
     * @param d the value; a zero of either sign answers the largest negative subnormal
     */
    public static double nextDown(double d) {
        if (d != d || d == Double.NEGATIVE_INFINITY) {
            return d;
        }
        if (d == 0.0d) {
            return -Double.MIN_VALUE;
        }
        long bits = Double.doubleToRawLongBits(d);
        if (d > 0.0d) {
            return Double.longBitsToDouble(bits - 1L);
        }
        return Double.longBitsToDouble(bits + 1L);
    }

    /**
     * The next representable value below {@code f}.
     *
     * @param f the value
     * @see #nextDown(double)
     */
    public static float nextDown(float f) {
        if (f != f || f == Float.NEGATIVE_INFINITY) {
            return f;
        }
        if (f == 0.0f) {
            return -Float.MIN_VALUE;
        }
        int bits = Float.floatToRawIntBits(f);
        if (f > 0.0f) {
            return Float.intBitsToFloat(bits - 1);
        }
        return Float.intBitsToFloat(bits + 1);
    }

    /**
     * The representable value adjacent to {@code start}, on the side {@code direction} is.
     *
     * @param start where to step from
     * @param direction which way; equal to {@code start} means stay
     */
    public static double nextAfter(double start, double direction) {
        if (start != start || direction != direction) {
            return start + direction; // either is NaN, so the answer is NaN
        }
        if (start == direction) {
            return direction;
        }
        if (direction > start) {
            return Math.nextUp(start);
        }
        return Math.nextDown(start);
    }

    /**
     * The representable value adjacent to {@code start}, on the side {@code direction} is.
     *
     * <p>Note the asymmetric signature -- a float and a double -- which the JDK has so that a
     * direction can be named more precisely than the value being stepped.
     *
     * @param start where to step from
     * @param direction which way
     */
    public static float nextAfter(float start, double direction) {
        if (start != start || direction != direction) {
            return start + (float) direction;
        }
        if ((double) start == direction) {
            return (float) direction;
        }
        if (direction > (double) start) {
            return Math.nextUp(start);
        }
        return Math.nextDown(start);
    }

    /**
     * {@code d} multiplied by two to the {@code scaleFactor}.
     *
     * <p>Exact where the result is normal, and rounded exactly ONCE where it is not -- which is
     * the whole reason this exists instead of {@code d * Math.pow(2, n)}. Scaling in one
     * multiplication can overflow or underflow on the way to an answer that is perfectly
     * representable; scaling in bounded steps cannot, and only the step that lands in the
     * subnormal range ever rounds.
     *
     * @param d the value to scale
     * @param scaleFactor the power of two
     */
    public static double scalb(double d, int scaleFactor) {
        if (d != d || d == 0.0d || d == Double.POSITIVE_INFINITY
                || d == Double.NEGATIVE_INFINITY) {
            return d;
        }
        // Past these the answer is an infinity or a zero whatever the input was, and clamping
        // keeps the step loop below bounded.
        int n = scaleFactor;
        if (n > 2200) {
            n = 2200;
        }
        if (n < -2200) {
            n = -2200;
        }
        double result = d;
        while (n > 0) {
            int step = n;
            if (step > 1023) {
                step = 1023;
            }
            result = result * Math.twoTo(step);
            n = n - step;
            if (result == Double.POSITIVE_INFINITY || result == Double.NEGATIVE_INFINITY) {
                return result;
            }
        }
        while (n < 0) {
            int step = n;
            if (step < -1022) {
                step = -1022;
            }
            result = result * Math.twoTo(step);
            n = n - step;
            if (result == 0.0d) {
                return result;
            }
        }
        return result;
    }

    /**
     * {@code f} multiplied by two to the {@code scaleFactor}.
     *
     * <p>Done at float width rather than through a double, for the same reason
     * {@code Float.parseFloat} is: going wide and coming back rounds twice.
     *
     * @param f the value to scale
     * @param scaleFactor the power of two
     */
    public static float scalb(float f, int scaleFactor) {
        if (f != f || f == 0.0f || f == Float.POSITIVE_INFINITY
                || f == Float.NEGATIVE_INFINITY) {
            return f;
        }
        int n = scaleFactor;
        if (n > 300) {
            n = 300;
        }
        if (n < -300) {
            n = -300;
        }
        float result = f;
        while (n > 0) {
            int step = n;
            if (step > 127) {
                step = 127;
            }
            result = result * Math.twoToF(step);
            n = n - step;
            if (result == Float.POSITIVE_INFINITY || result == Float.NEGATIVE_INFINITY) {
                return result;
            }
        }
        while (n < 0) {
            int step = n;
            if (step < -126) {
                step = -126;
            }
            result = result * Math.twoToF(step);
            n = n - step;
            if (result == 0.0f) {
                return result;
            }
        }
        return result;
    }

    // An exact power of two, built from its exponent field. Only defined for a NORMAL exponent,
    // which is what every caller above guarantees by bounding its steps.
    private static double twoTo(int k) {
        return Double.longBitsToDouble(((long) (k + 1023)) << 52);
    }

    private static float twoToF(int k) {
        return Float.intBitsToFloat((k + 127) << 23);
    }


    // ---- rounding to an integral VALUE ----
    //
    // Distinct from `round`, which answers an int or a long: these answer a double that happens
    // to be integral, so they work past 2^63 where an integer type has nothing to say. All three
    // are decided by the bits -- there is no arithmetic in them at all -- which is why they
    // belong to the exact half of this class rather than to the approximating one.

    /**
     * The largest integral value not greater than {@code a}.
     *
     * @param a the value; a NaN, an infinity or a zero comes back unchanged, signs included
     */
    public static double floor(double a) {
        if (a != a || a == 0.0d || a == Double.POSITIVE_INFINITY
                || a == Double.NEGATIVE_INFINITY) {
            return a;
        }
        int exponent = Math.getExponent(a);
        if (exponent >= 52) {
            return a; // no fractional bits left to drop
        }
        if (exponent < 0) {
            // |a| < 1, so the answer is one of the two integers around zero. The negative side
            // keeps its sign through -1.0; the positive side must give +0.0 and not -0.0.
            if (a > 0.0d) {
                return 0.0d;
            }
            return -1.0d;
        }
        long bits = Double.doubleToRawLongBits(a);
        long fraction = 0x000fffffffffffffL >> exponent;
        if ((bits & fraction) == 0L) {
            return a; // already integral
        }
        double truncated = Double.longBitsToDouble(bits & ~fraction);
        if (a < 0.0d) {
            // Clearing the fraction moves a negative number UP, toward zero, so the floor is one
            // step further down.
            return truncated - 1.0d;
        }
        return truncated;
    }

    /**
     * The smallest integral value not less than {@code a}.
     *
     * <p>Note that {@code ceil(-0.5)} is {@code -0.0} and not {@code 0.0}: the sign survives even
     * when the magnitude does not.
     *
     * @param a the value
     */
    public static double ceil(double a) {
        return -Math.floor(-a);
    }

    /**
     * The integral value closest to {@code a}, with ties going to the EVEN one.
     *
     * <p>Ties to even, where {@link #round(double)} takes ties up. The difference matters over a
     * long summation: rounding halves consistently upward accumulates a bias, and rounding them
     * to even does not.
     *
     * @param a the value
     */
    public static double rint(double a) {
        if (a != a || a == 0.0d || a == Double.POSITIVE_INFINITY
                || a == Double.NEGATIVE_INFINITY) {
            return a;
        }
        double magnitude = Math.abs(a);
        // 2^52 is where doubles stop having fractional bits at all.
        double twoTo52 = 4.503599627370496e15d;
        if (magnitude >= twoTo52) {
            return a;
        }
        // The classic trick, and it is not a hack: adding 2^52 pushes every fractional bit off
        // the end of the significand, and IEEE-754 rounds that addition to nearest-even -- which
        // is exactly the rounding being asked for. Subtracting it back is exact.
        double shifted = (magnitude + twoTo52) - twoTo52;
        return Math.copySign(shifted, a);
    }

    // ---- the square root ----

    /**
     * El logaritmo natural.
     *
     * <p>Es el algoritmo de **fdlibm** --el mismo que usa el JDK--, y se escribe entero porque es la
     * unica forma de que dos implementaciones den el **mismo double**. Una aproximacion "buena" no
     * sirve: `Random.nextGaussian()` compone `log` con `sqrt`, y un ulp de diferencia se propaga a
     * todos los valores que devuelve.
     *
     * <p>El metodo, en tres pasos:
     *
     * <ol>
     * <li><b>Descomponer.</b> Todo double es {@code m * 2^k} con {@code m} en {@code [1, 2)}, asi
     *     que {@code log(x) = k*log(2) + log(m)}. La parte {@code k*log(2)} es exacta.</li>
     * <li><b>Centrar.</b> A {@code m} se lo lleva a {@code [sqrt(2)/2, sqrt(2))} ajustando {@code k},
     *     porque el polinomio de abajo solo es preciso cerca de 1.</li>
     * <li><b>Aproximar.</b> Con {@code s = f/(2+f)} y {@code f = m-1}, la serie
     *     {@code log(1+f) = 2s + 2s^3/3 + 2s^5/5 + ...} converge rapidisimo, y siete coeficientes
     *     alcanzan para el ultimo bit.</li>
     * </ol>
     *
     * <p>Los dos pedazos de {@code log(2)} --alto y bajo-- son el truco que evita perder precision
     * al sumar: {@code k*ln2_hi} es exacto porque {@code ln2_hi} tiene ceros en los bits bajos, y
     * {@code ln2_lo} aporta lo que falta sin cancelarse contra el resto.
     */
    public static double log(double a) {
        // Los siete coeficientes del polinomio, y log(2) partido en dos.
        double ln2Hi = 6.93147180369123816490e-01d;
        double ln2Lo = 1.90821492927058770002e-10d;
        double dosAla54 = 1.80143985094819840000e+16d;
        double lg1 = 6.666666666666735130e-01d;
        double lg2 = 3.999999999940941908e-01d;
        double lg3 = 2.857142874366239149e-01d;
        double lg4 = 2.222219843214978396e-01d;
        double lg5 = 1.818357216161805012e-01d;
        double lg6 = 1.531383769920937332e-01d;
        double lg7 = 1.479819860511658591e-01d;

        double x = a;
        long bits = Double.doubleToRawLongBits(x);
        int hx = (int) (bits >>> 32);
        int lx = (int) bits;
        int k = 0;

        if (hx < 0x00100000) {
            // Subnormal o cero.
            if (((hx & 0x7fffffff) | lx) == 0) {
                return Double.NEGATIVE_INFINITY;      // log(+-0)
            }
            if (hx < 0) {
                return Double.NaN;                    // log(negativo)
            }
            // Se escala a normal y se compensa en el exponente.
            k = k - 54;
            x = x * dosAla54;
            bits = Double.doubleToRawLongBits(x);
            hx = (int) (bits >>> 32);
        }
        if (hx >= 0x7ff00000) {
            return x + x;                             // NaN o +infinito
        }
        k = k + (hx >> 20) - 1023;
        hx = hx & 0x000fffff;
        // El ajuste que centra la mantisa en [sqrt(2)/2, sqrt(2)).
        int i = (hx + 0x95f64) & 0x100000;
        long nuevos = (((long) (hx | (i ^ 0x3ff00000))) << 32) | (bits & 0xffffffffL);
        x = Double.longBitsToDouble(nuevos);
        k = k + (i >> 20);
        double f = x - 1.0d;
        double dk = (double) k;

        if ((0x000fffff & (2 + hx)) < 3) {
            // |f| < 2^-20: la serie se corta en el tercer termino.
            if (f == 0.0d) {
                if (k == 0) {
                    return 0.0d;
                }
                return dk * ln2Hi + dk * ln2Lo;
            }
            double rr = f * f * (0.5d - 0.33333333333333333d * f);
            if (k == 0) {
                return f - rr;
            }
            return dk * ln2Hi - ((rr - dk * ln2Lo) - f);
        }

        double sx = f / (2.0d + f);
        double z = sx * sx;
        double w = z * z;
        int ii = hx - 0x6147a;
        int jj = 0x6b851 - hx;
        double t1 = w * (lg2 + w * (lg4 + w * lg6));
        double t2 = z * (lg1 + w * (lg3 + w * (lg5 + w * lg7)));
        ii = ii | jj;
        double rr = t2 + t1;
        if (ii > 0) {
            double hfsq = 0.5d * f * f;
            if (k == 0) {
                return f - (hfsq - sx * (hfsq + rr));
            }
            return dk * ln2Hi - ((hfsq - (sx * (hfsq + rr) + dk * ln2Lo)) - f);
        }
        if (k == 0) {
            return f - sx * (f - rr);
        }
        return dk * ln2Hi - ((sx * (f - rr) - dk * ln2Lo) - f);
    }


    /**
     * The square root of {@code a}, correctly rounded.
     *
     * <p>Correctly rounded is not a nicety here, it is the specification: IEEE-754 lists the
     * square root among the operations that must return the representable value nearest the
     * exact result, alongside the four arithmetic ones. So this does not iterate in floating
     * point and hope -- it computes an integer square root in arbitrary precision and rounds the
     * result once, half to even.
     *
     * @param a the value; negative gives NaN, and both zeros come back as themselves
     */
    public static double sqrt(double a) {
        if (a != a || a == 0.0d || a == Double.POSITIVE_INFINITY) {
            return a;
        }
        if (a < 0.0d) {
            return Double.NaN;
        }
        // The exact value, as an integer significand times a power of two.
        long bits = Double.doubleToRawLongBits(a);
        long biased = (bits >> 52) & 0x7ffL;
        long significand = bits & 0x000fffffffffffffL;
        int exponent;
        if (biased == 0L) {
            exponent = -1074;
        } else {
            significand = significand | 0x0010000000000000L;
            exponent = (int) biased - 1075;
        }
        BigInteger m = BigInteger.valueOf(significand);
        // Scale so the integer square root comes out with 54 bits: one more than a double
        // holds, which is the guard bit the rounding needs. The exponent of the scaled value
        // must be even, so that halving it is exact.
        int length = m.bitLength();
        int t = 107 - length;
        int e = exponent;
        if (((e - t) & 1) != 0) {
            t = t + 1;
        }
        BigInteger scaled = m.shiftLeft(t);
        int k = (e - t) / 2;
        BigInteger root = Math.integerSqrt(scaled);
        BigInteger square = root.multiply(root);
        boolean sticky = scaled.compareTo(square) != 0;
        // `root` has 54 bits; drop the lowest and round on it.
        boolean guard = root.testBit(0);
        BigInteger kept = root.shiftRight(1);
        if (guard && (sticky || kept.testBit(0))) {
            kept = kept.add(BigInteger.ONE);
        }
        int resultExponent = k + 1;
        if (kept.bitLength() > 53) {
            // Rounding carried out of the significand.
            kept = kept.shiftRight(1);
            resultExponent = resultExponent + 1;
        }
        long mantissa = kept.longValue();
        long resultBiased = (long) (resultExponent + 52 + 1023);
        return Double.longBitsToDouble((resultBiased << 52) | (mantissa & 0x000fffffffffffffL));
    }

    // The integer square root, by Newton's method. Each step roughly doubles the number of
    // correct digits, and the sequence decreases once it is above the answer -- which is what
    // makes "stop when it stops decreasing" the right termination test.
    private static BigInteger integerSqrt(BigInteger n) {
        if (n.signum() == 0) {
            return BigInteger.ZERO;
        }
        BigInteger guess = BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2);
        while (true) {
            BigInteger quotient = n.divide(guess);
            BigInteger sum = guess.add(quotient);
            BigInteger next = sum.shiftRight(1);
            if (next.compareTo(guess) >= 0) {
                return guess;
            }
            guess = next;
        }
    }

    // ---- a random number ----

    // One generator for the whole program, made on first use. Held in its own class so that
    // merely mentioning `Math` does not construct it: a `Random` seeds itself from the clock,
    // and doing that eagerly would cost every program that never asks for a random number.
    private static final class RandomHolder {
        static final java.util.Random SHARED = new java.util.Random();
    }

    /**
     * A double in {@code [0.0, 1.0)}, from a generator shared by the whole program.
     *
     * <p>Shared, which is the part worth knowing: two calls from two threads draw from the same
     * sequence, so this is convenient rather than fast. Code that generates a lot of numbers
     * should hold its own {@link java.util.Random}.
     */
    public static double random() {
        return RandomHolder.SHARED.nextDouble();
    }


    // ---- the remainder, and the fused multiply-add ----
    //
    // The last two operations in this class with exactly ONE right answer. Both are defined by
    // IEEE-754 as exact-then-round-once, and both are impossible to write with ordinary double
    // arithmetic, because the intermediate they need does not fit in a double. So both compute
    // the intermediate as an integer of whatever width it takes and round at the end -- the same
    // move `sqrt` makes, and for the same reason.

    /**
     * The IEEE-754 remainder of {@code f1} divided by {@code f2}.
     *
     * <p>Not {@code %}, and the difference is not small: {@code %} takes the quotient toward
     * zero, this one takes it to the NEAREST integer. So the result lands in
     * {@code [-|f2|/2, |f2|/2]} rather than in {@code [0, |f2|)}, and it can have the sign
     * opposite to {@code f1}. That is what makes it the right one for reducing an angle.
     *
     * <p>The result is always exact -- never rounded -- which is a property of the operation and
     * not of this implementation.
     *
     * @param f1 the dividend
     * @param f2 the divisor
     */
    public static double IEEEremainder(double f1, double f2) {
        if (f1 != f1 || f2 != f2) {
            return Double.NaN;
        }
        if (f1 == Double.POSITIVE_INFINITY || f1 == Double.NEGATIVE_INFINITY) {
            return Double.NaN;
        }
        if (f2 == 0.0d) {
            return Double.NaN;
        }
        if (f2 == Double.POSITIVE_INFINITY || f2 == Double.NEGATIVE_INFINITY) {
            return f1;
        }
        if (f1 == 0.0d) {
            return f1; // keeps the sign of the zero
        }
        // Both finite and non-zero: line the two up as exact integers over a common power of
        // two, so the quotient and the remainder can be taken without rounding anything.
        long[] left = Math.decompose(f1);
        long[] right = Math.decompose(f2);
        int scale = (int) Math.min(left[1], right[1]);
        BigInteger x = BigInteger.valueOf(left[0]).shiftLeft((int) left[1] - scale);
        BigInteger y = BigInteger.valueOf(right[0]).shiftLeft((int) right[1] - scale);
        BigInteger absY = y.abs();
        BigInteger quotient = x.divide(y);
        BigInteger remainder = x.subtract(quotient.multiply(y));
        // Round the quotient to NEAREST, ties to even, by looking at twice the remainder.
        BigInteger twice = remainder.abs().shiftLeft(1);
        int cmp = twice.compareTo(absY);
        boolean up = cmp > 0;
        if (cmp == 0) {
            up = quotient.testBit(0);
        }
        if (up) {
            // Step the quotient one further in the direction the remainder points.
            if (remainder.signum() == y.signum()) {
                quotient = quotient.add(BigInteger.ONE);
            } else {
                quotient = quotient.subtract(BigInteger.ONE);
            }
            remainder = x.subtract(quotient.multiply(y));
        }
        if (remainder.signum() == 0) {
            // An exact multiple: the answer is a zero with the sign of the dividend.
            return Math.copySign(0.0d, f1);
        }
        return Math.exactly(remainder, scale);
    }

    /**
     * {@code a * b + c}, rounded ONCE.
     *
     * <p>The point is the single rounding. Written as an expression, {@code a * b + c} rounds
     * twice -- once on the product and once on the sum -- and the two roundings can land
     * somewhere neither would alone. Measured against the reference, the two disagree on about a
     * quarter of ordinary inputs, so this is not a subtlety that stays theoretical.
     *
     * @param a one factor
     * @param b the other factor
     * @param c what to add
     */
    public static double fma(double a, double b, double c) {
        if (a != a || b != b || c != c) {
            return Double.NaN;
        }
        boolean aInf = a == Double.POSITIVE_INFINITY || a == Double.NEGATIVE_INFINITY;
        boolean bInf = b == Double.POSITIVE_INFINITY || b == Double.NEGATIVE_INFINITY;
        boolean cInf = c == Double.POSITIVE_INFINITY || c == Double.NEGATIVE_INFINITY;
        if (aInf || bInf || cInf) {
            // Infinity times zero is undefined, and so is an infinite product added to the
            // opposite infinity. Everything else the ordinary expression already gets right,
            // because with an infinity involved there is nothing to round.
            return a * b + c;
        }
        if (a == 0.0d || b == 0.0d) {
            return a * b + c; // the product is an exact zero; only its sign matters
        }
        if (c == 0.0d) {
            return a * b; // one rounding either way
        }
        // The exact product, as an integer times a power of two: two 53-bit significands make a
        // 106-bit one, which is exactly the width no double has.
        long[] fa = Math.decompose(a);
        long[] fb = Math.decompose(b);
        long[] fc = Math.decompose(c);
        BigInteger product = BigInteger.valueOf(fa[0]).multiply(BigInteger.valueOf(fb[0]));
        int productScale = (int) (fa[1] + fb[1]);
        BigInteger addend = BigInteger.valueOf(fc[0]);
        int addendScale = (int) fc[1];
        // Line the two up on the lower of the two scales, then add exactly.
        int scale = Math.min(productScale, addendScale);
        BigInteger sum = product.shiftLeft(productScale - scale)
                .add(addend.shiftLeft(addendScale - scale));
        if (sum.signum() == 0) {
            // The two cancelled exactly. Under round-to-nearest that is a positive zero, unless
            // both contributions were negative zeros -- which they cannot be here.
            return 0.0d;
        }
        return Math.exactly(sum, scale);
    }

    /**
     * {@code a * b + c}, rounded once, at float width.
     *
     * <p>Routed through doubles, which is correct rather than convenient. The product of two
     * floats has at most 48 significant bits and so is EXACT in a double; adding {@code c} then
     * rounds once to 53 bits, and narrowing rounds again to 24. Two roundings would normally be
     * a hazard, and here they are not: the intermediate carries at least {@code 2p + 2} bits for
     * the target width (53 against the 50 that 24-bit floats need), which is the condition under
     * which the double rounding provably agrees with rounding once.
     *
     * @param a one factor
     * @param b the other factor
     * @param c what to add
     */
    public static float fma(float a, float b, float c) {
        if (a != a || b != b || c != c) {
            return Float.NaN;
        }
        boolean finite = !Float.isInfinite(a) && !Float.isInfinite(b) && !Float.isInfinite(c);
        if (!finite) {
            return a * b + c;
        }
        if (a == 0.0f || b == 0.0f) {
            return a * b + c;
        }
        double exact = (double) a * (double) b;
        return (float) (exact + (double) c);
    }

    // A finite, non-zero double as an exact `significand * 2^scale`, with the sign on the
    // significand. Two longs rather than an object: this is called from the hot inside of the
    // three methods above and allocating a pair for it would be the most expensive thing they do.
    private static long[] decompose(double d) {
        long bits = Double.doubleToRawLongBits(d);
        long biased = (bits >> 52) & 0x7ffL;
        long significand = bits & 0x000fffffffffffffL;
        long scale;
        if (biased == 0L) {
            scale = -1074L;
        } else {
            significand = significand | 0x0010000000000000L;
            scale = biased - 1075L;
        }
        if (bits < 0L) {
            significand = -significand;
        }
        long[] out = new long[2];
        out[0] = significand;
        out[1] = scale;
        return out;
    }

    // `value * 2^scale` as the nearest double, rounding half to even. The one place the exact
    // arithmetic above meets the format again, and therefore the only place anything rounds.
    private static double exactly(BigInteger value, int scale) {
        int sign = value.signum();
        BigInteger magnitude = value.abs();
        int length = magnitude.bitLength();
        int shift = length - 53;
        BigInteger kept = magnitude;
        int exponent = scale;
        if (shift > 0) {
            BigInteger dropped = magnitude.subtract(magnitude.shiftRight(shift).shiftLeft(shift));
            kept = magnitude.shiftRight(shift);
            exponent = scale + shift;
            // Round on what was dropped: half to even, with the halfway point being the top bit
            // of the discarded part and nothing below it.
            BigInteger half = BigInteger.ONE.shiftLeft(shift - 1);
            int cmp = dropped.compareTo(half);
            boolean up = cmp > 0;
            if (cmp == 0) {
                up = kept.testBit(0);
            }
            if (up) {
                kept = kept.add(BigInteger.ONE);
                if (kept.bitLength() > 53) {
                    kept = kept.shiftRight(1);
                    exponent = exponent + 1;
                }
            }
        }
        double result = Math.scalb((double) kept.longValue(), exponent);
        if (sign < 0) {
            return -result;
        }
        return result;
    }

    // ======================================================================================
    // The transcendental half -- fdlibm, ported.
    //
    // These are the functions whose contract is stated in ULPs, which the JDK meets by shipping
    // fdlibm (today as `java.lang.FdLibm`, a Java translation of the C). They are ported here from
    // that same reference, constant for constant and step for step, so the result is bit-for-bit the
    // JDK's -- the only way a math library is allowed to be "close". `StrictMath` delegates to these.
    //
    // The bit games fdlibm plays on the two 32-bit halves of a double are expressed through these
    // helpers: `hi(x)`/`lo(x)` read the high/low word, `withHi`/`withLo` return `x` with that word
    // replaced. They stand in for the C macros `GET/SET_HIGH_WORD` and `__HI`/`__LO`.

    private static int hi(double x) {
        return (int) (Double.doubleToRawLongBits(x) >>> 32);
    }

    private static int lo(double x) {
        return (int) Double.doubleToRawLongBits(x);
    }

    private static double withHi(double x, int high) {
        long bits = Double.doubleToRawLongBits(x) & 0x00000000ffffffffL;
        return Double.longBitsToDouble((((long) high) << 32) | bits);
    }

    private static double withLo(double x, int low) {
        long bits = Double.doubleToRawLongBits(x) & 0xffffffff00000000L;
        return Double.longBitsToDouble(bits | (((long) low) & 0x00000000ffffffffL));
    }

    // ---- cube root (fdlibm s_cbrt.c) ----

    /**
     * The cube root of {@code a}. Ported from fdlibm: a 5-bit seed by exponent division, refined to
     * 23 bits by a rational approximation and to 53 by one Newton step, for an error under 0.667 ulp.
     */
    public static double cbrt(double a) {
        // (682-0.03306235651)*2**20 and (664-0.03306235651)*2**20: the exponent-division seeds.
        final int B1 = 715094163;
        final int B2 = 696219795;
        final double C = 5.42857142857142815906e-01; //  19/35
        final double D = -7.05306122448979611050e-01; // -864/1225
        final double E = 1.41428571428571436819e+00; //  99/70
        final double F = 1.60714285714285720630e+00; //  45/28
        final double G = 3.57142857142857150787e-01; //   5/14

        if (a == 0.0d || a != a || a == Double.POSITIVE_INFINITY
                || a == Double.NEGATIVE_INFINITY) {
            return a; // signed zeros, NaN and infinities are their own cube roots
        }
        double sign = (a < 0.0d) ? -1.0d : 1.0d;
        double x = Math.abs(a);

        // Rough cube root to 5 bits, from dividing the biased exponent by three.
        double t;
        if (x < Double.MIN_NORMAL) { // subnormal: scale up by 2**54 first
            t = Double.longBitsToDouble(0x4350000000000000L); // 2**54
            t = t * x;
            t = withHi(t, hi(t) / 3 + B2);
        } else {
            t = withHi(0.0d, hi(x) / 3 + B1);
        }

        // New cube root to 23 bits by a rational approximation.
        double r = t * t / x;
        double s = C + r * t;
        t = t * (G + F / (s + E + D / s));

        // Chop to 20 bits and nudge up, so t is a hair larger than cbrt(x).
        t = withLo(t, 0);
        t = withHi(t, hi(t) + 1);

        // One Newton step to 53 bits, error < 0.667 ulp. t*t and r-t are exact here.
        s = t * t;
        r = x / s;
        double w = t + t;
        r = (r - t) / (w + r);
        t = t + t * r;

        return sign * t;
    }

    // ---- e^x (fdlibm e_exp.c) ----

    /**
     * {@code e} raised to {@code a}. Ported from fdlibm: reduce to {@code x = r + k*ln2} with
     * {@code |r| <= 0.5 ln2}, evaluate {@code e^r} by a degree-5 rational, then scale by {@code 2^k}.
     * Error under 1 ulp.
     */
    public static double exp(double a) {
        final double one = 1.0d;
        final double[] halF = {0.5d, -0.5d};
        final double huge = 1.0e+300d;
        final double twom1000 = Double.longBitsToDouble(0x0170000000000000L); // 2**-1000
        final double o_threshold = 7.09782712893383973096e+02d;
        final double u_threshold = -7.45133219101941108420e+02d;
        final double[] ln2HI = {6.93147180369123816490e-01d, -6.93147180369123816490e-01d};
        final double[] ln2LO = {1.90821492927058770002e-10d, -1.90821492927058770002e-10d};
        final double invln2 = 1.44269504088896338700e+00d;
        final double P1 = 1.66666666666666019037e-01d;
        final double P2 = -2.77777777770155933842e-03d;
        final double P3 = 6.61375632143793436117e-05d;
        final double P4 = -1.65339022054652515390e-06d;
        final double P5 = 4.13813679705723846039e-08d;

        double x = a;
        double y;
        double hi = 0.0d;
        double lo = 0.0d;
        double c;
        double t;
        int k = 0;
        int hx = hi(x);
        int xsb = (hx >>> 31) & 1;    // sign bit of x
        hx = hx & 0x7fffffff;         // high word of |x|

        if (hx >= 0x40862E42) {       // |x| >= 709.78...
            if (hx >= 0x7ff00000) {
                if (((hx & 0xfffff) | lo(x)) != 0) {
                    return x + x;                     // NaN
                }
                return (xsb == 0) ? x : 0.0d;         // exp(+-inf) = {inf, 0}
            }
            if (x > o_threshold) {
                return huge * huge;                   // overflow
            }
            if (x < u_threshold) {
                return twom1000 * twom1000;           // underflow
            }
        }

        if (hx > 0x3fd62e42) {                // |x| > 0.5 ln2
            if (hx < 0x3FF0A2B2) {            // and |x| < 1.5 ln2
                hi = x - ln2HI[xsb];
                lo = ln2LO[xsb];
                k = 1 - xsb - xsb;
            } else {
                k = (int) (invln2 * x + halF[xsb]);
                t = k;
                hi = x - t * ln2HI[0];        // t*ln2HI is exact here
                lo = t * ln2LO[0];
            }
            x = hi - lo;
        } else if (hx < 0x3e300000) {         // |x| < 2**-28
            if (huge + x > one) {
                return one + x;               // trigger inexact
            }
        } else {
            k = 0;
        }

        t = x * x;
        c = x - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
        if (k == 0) {
            return one - ((x * c) / (c - 2.0d) - x);
        }
        y = one - ((lo - (x * c) / (2.0d - c)) - hi);
        if (k >= -1021) {
            y = withHi(y, hi(y) + (k << 20)); // add k to y's exponent
            return y;
        }
        y = withHi(y, hi(y) + ((k + 1000) << 20));
        return y * twom1000;
    }

    // ---- natural logarithm (fdlibm e_log.c) ----

    /**
     * The natural logarithm of {@code a}. Ported from fdlibm: split {@code a = 2^k * (1+f)}, then
     * {@code log(1+f)} from {@code s = f/(2+f)} through a polynomial in {@code s^2}. Error under 1 ulp.
     */
    public static double log(double a) {
        final double ln2_hi = 6.93147180369123816490e-01d;
        final double ln2_lo = 1.90821492927058770002e-10d;
        final double two54 = 1.80143985094819840000e+16d;
        final double Lg1 = 6.666666666666735130e-01d;
        final double Lg2 = 3.999999999940941908e-01d;
        final double Lg3 = 2.857142874366239149e-01d;
        final double Lg4 = 2.222219843214978396e-01d;
        final double Lg5 = 1.818357216161805012e-01d;
        final double Lg6 = 1.531383769920937332e-01d;
        final double Lg7 = 1.479819860511658591e-01d;

        double x = a;
        double hfsq;
        double f;
        double s;
        double z;
        double R;
        double w;
        double t1;
        double t2;
        double dk;
        int k = 0;
        int hx = hi(x);
        int lx = lo(x);

        if (hx < 0x00100000) {                     // x < 2**-1022
            if (((hx & 0x7fffffff) | lx) == 0) {
                return -two54 / 0.0d;              // log(+-0) = -inf
            }
            if (hx < 0) {
                return (x - x) / 0.0d;             // log(-#) = NaN
            }
            k = k - 54;
            x = x * two54;                         // subnormal: scale up
            hx = hi(x);
        }
        if (hx >= 0x7ff00000) {
            return x + x;
        }
        k = k + (hx >> 20) - 1023;
        hx = hx & 0x000fffff;
        int i = (hx + 0x95f64) & 0x100000;
        x = withHi(x, hx | (i ^ 0x3ff00000));      // normalize x or x/2
        k = k + (i >> 20);
        f = x - 1.0d;
        if ((0x000fffff & (2 + hx)) < 3) {         // |f| < 2**-20
            if (f == 0.0d) {
                if (k == 0) {
                    return 0.0d;
                }
                dk = (double) k;
                return dk * ln2_hi + dk * ln2_lo;
            }
            R = f * f * (0.5d - 0.33333333333333333d * f);
            if (k == 0) {
                return f - R;
            }
            dk = (double) k;
            return dk * ln2_hi - ((R - dk * ln2_lo) - f);
        }
        s = f / (2.0d + f);
        dk = (double) k;
        z = s * s;
        i = hx - 0x6147a;
        w = z * z;
        int j = 0x6b851 - hx;
        t1 = w * (Lg2 + w * (Lg4 + w * Lg6));
        t2 = z * (Lg1 + w * (Lg3 + w * (Lg5 + w * Lg7)));
        i = i | j;
        R = t2 + t1;
        if (i > 0) {
            hfsq = 0.5d * f * f;
            if (k == 0) {
                return f - (hfsq - s * (hfsq + R));
            }
            return dk * ln2_hi - ((hfsq - (s * (hfsq + R) + dk * ln2_lo)) - f);
        }
        if (k == 0) {
            return f - s * (f - R);
        }
        return dk * ln2_hi - ((s * (f - R) - dk * ln2_lo) - f);
    }

    // ---- trigonometry (fdlibm, via the shared reduction in FdLibm) ----

    /** The sine of {@code a} (radians). fdlibm: argument reduction (see {@link FdLibm}) + a kernel. */
    public static double sin(double a) {
        return FdLibm.sin(a);
    }

    /** The cosine of {@code a} (radians). fdlibm: argument reduction + a kernel. */
    public static double cos(double a) {
        return FdLibm.cos(a);
    }

    /** The tangent of {@code a} (radians). fdlibm: argument reduction + a kernel. */
    public static double tan(double a) {
        return FdLibm.tan(a);
    }

    // ---- e^x - 1 (fdlibm s_expm1.c) ----

    /** {@code e^a - 1}, accurate near zero where {@code exp(a) - 1} would lose all its bits. */
    public static double expm1(double a) {
        final double one = 1.0d;
        final double huge = 1.0e+300d;
        final double tiny = 1.0e-300d;
        final double o_threshold = 7.09782712893383973096e+02d;
        final double ln2_hi = 6.93147180369123816490e-01d;
        final double ln2_lo = 1.90821492927058770002e-10d;
        final double invln2 = 1.44269504088896338700e+00d;
        final double Q1 = -3.33333333333331316428e-02d;
        final double Q2 = 1.58730158725481460165e-03d;
        final double Q3 = -7.93650757867487942473e-05d;
        final double Q4 = 4.00821782732936239552e-06d;
        final double Q5 = -2.01099218183624371326e-07d;

        double x = a;
        double y;
        double hival;
        double loval;
        double c = 0.0d;
        double t;
        double e;
        double hxs;
        double hfx;
        double r1;
        double twopk;
        int k;
        int hx = hi(x);
        int xsb = hx & 0x80000000;
        hx = hx & 0x7fffffff;
        if (hx >= 0x4043687A) {
            if (hx >= 0x40862E42) {
                if (hx >= 0x7ff00000) {
                    int low = lo(x);
                    if (((hx & 0xfffff) | low) != 0) {
                        return x + x;
                    }
                    return (xsb == 0) ? x : -1.0d;
                }
                if (x > o_threshold) {
                    return huge * huge;
                }
            }
            if (xsb != 0) {
                if (x + tiny < 0.0d) {
                    return tiny - one;
                }
            }
        }
        if (hx > 0x3fd62e42) {
            if (hx < 0x3FF0A2B2) {
                if (xsb == 0) {
                    hival = x - ln2_hi;
                    loval = ln2_lo;
                    k = 1;
                } else {
                    hival = x + ln2_hi;
                    loval = -ln2_lo;
                    k = -1;
                }
            } else {
                k = (int) (invln2 * x + ((xsb == 0) ? 0.5d : -0.5d));
                t = k;
                hival = x - t * ln2_hi;
                loval = t * ln2_lo;
            }
            x = hival - loval;
            c = (hival - x) - loval;
        } else if (hx < 0x3c900000) {
            t = huge + x;
            return x - (t - (huge + x));
        } else {
            k = 0;
        }
        hfx = 0.5d * x;
        hxs = x * hfx;
        r1 = one + hxs * (Q1 + hxs * (Q2 + hxs * (Q3 + hxs * (Q4 + hxs * Q5))));
        t = 3.0d - r1 * hfx;
        e = hxs * ((r1 - t) / (6.0d - x * t));
        if (k == 0) {
            return x - (x * e - hxs);
        }
        twopk = Double.longBitsToDouble((((long) (0x3ff00000 + (k << 20))) & 0xffffffffL) << 32);
        e = (x * (e - c) - c);
        e -= hxs;
        if (k == -1) {
            return 0.5d * (x - e) - 0.5d;
        }
        if (k == 1) {
            if (x < -0.25d) {
                return -2.0d * (e - (x + 0.5d));
            }
            return one + 2.0d * (x - e);
        }
        if (k <= -2 || k > 56) {
            y = one - (e - x);
            if (k == 1024) {
                y = y * 2.0d * Double.longBitsToDouble(0x7fe0000000000000L);
            } else {
                y = y * twopk;
            }
            return y - one;
        }
        t = one;
        if (k < 20) {
            t = withHi(t, 0x3ff00000 - (0x200000 >> k));
            y = t - (e - x);
            y = y * twopk;
        } else {
            t = withHi(t, ((0x3ff - k) << 20));
            y = x - (e + t);
            y += one;
            y = y * twopk;
        }
        return y;
    }

    // ---- log(1 + x) (fdlibm s_log1p.c, with the JDK's grouping revision) ----

    /**
     * {@code log(1 + a)}, accurate near zero where {@code log(1 + a)} would lose bits. The final
     * combination groups {@code k*ln2_lo} with the correction term rather than with {@code f} — the
     * JDK's one revision to netlib fdlibm here, without which the result is 1 ulp off for {@code
     * 1+a >= sqrt(2)}.
     */
    public static double log1p(double a) {
        final double ln2_hi = 6.93147180369123816490e-01d;
        final double ln2_lo = 1.90821492927058770002e-10d;
        final double two54 = 1.80143985094819840000e+16d;
        final double zero = 0.0d;
        final double Lp1 = 6.666666666666735130e-01d;
        final double Lp2 = 3.999999999940941908e-01d;
        final double Lp3 = 2.857142874366239149e-01d;
        final double Lp4 = 2.222219843214978396e-01d;
        final double Lp5 = 1.818357216161805012e-01d;
        final double Lp6 = 1.531383769920937332e-01d;
        final double Lp7 = 1.479819860511658591e-01d;

        double x = a;
        double hfsq;
        double f = 0.0d;
        double c = 0.0d;
        double s;
        double z;
        double R;
        double u;
        int k = 1;
        int hx = hi(x);
        int ax = hx & 0x7fffffff;
        int hu = 0;
        if (hx < 0x3FDA827A) {
            if (ax >= 0x3ff00000) {
                if (x == -1.0d) {
                    return -two54 / zero;
                }
                return Double.NaN;
            }
            if (ax < 0x3e200000) {
                if (two54 + x > zero && ax < 0x3c900000) {
                    return x;
                }
                return x - x * x * 0.5d;
            }
            if (hx > 0 || hx <= 0xbfd2bec3) {
                k = 0;
                f = x;
                hu = 1;
            }
        }
        if (hx >= 0x7ff00000) {
            return x + x;
        }
        if (k != 0) {
            if (hx < 0x43400000) {
                u = 1.0d + x;
                hu = hi(u);
                k = (hu >> 20) - 1023;
                c = (k > 0) ? 1.0d - (u - x) : x - (u - 1.0d);
                c /= u;
            } else {
                u = x;
                hu = hi(u);
                k = (hu >> 20) - 1023;
                c = 0;
            }
            hu &= 0x000fffff;
            if (hu < 0x6a09e) {
                u = withHi(u, hu | 0x3ff00000);
            } else {
                k += 1;
                u = withHi(u, hu | 0x3fe00000);
                hu = (0x00100000 - hu) >> 2;
            }
            f = u - 1.0d;
        }
        hfsq = 0.5d * f * f;
        if (f == zero) {
            if (k == 0) {
                return zero;
            }
            c += k * ln2_lo;
            return k * ln2_hi + c;
        }
        s = f / (2.0d + f);
        z = s * s;
        R = z * (Lp1 + z * (Lp2 + z * (Lp3 + z * (Lp4 + z * (Lp5 + z * (Lp6 + z * Lp7))))));
        if (k == 0) {
            return f - (hfsq - s * (hfsq + R));
        }
        return k * ln2_hi - ((hfsq - (s * (hfsq + R) + (k * ln2_lo + c))) - f);
    }

    // ---- base-10 logarithm (fdlibm e_log10.c) ----

    /** The base-10 logarithm of {@code a}. Splits off the power of two, then reuses {@link #log}. */
    public static double log10(double a) {
        final double two54 = 1.80143985094819840000e+16d;
        final double ivln10 = 4.34294481903251816668e-01d;
        final double log10_2hi = 3.01029995663611771306e-01d;
        final double log10_2lo = 3.69423907715893078616e-13d;

        double x = a;
        double y;
        double z;
        int i;
        int k = 0;
        int hx = hi(x);
        int lx = lo(x);
        if (hx < 0x00100000) {
            if (((hx & 0x7fffffff) | lx) == 0) {
                return -two54 / 0.0d;
            }
            if (hx < 0) {
                return (x - x) / 0.0d;
            }
            k -= 54;
            x *= two54;
            hx = hi(x);
        }
        if (hx >= 0x7ff00000) {
            return x + x;
        }
        k += (hx >> 20) - 1023;
        i = (int) (((long) k & 0x80000000L) >>> 31);
        hx = (hx & 0x000fffff) | ((0x3ff - i) << 20);
        y = (double) (k + i);
        x = withHi(x, hx);
        z = y * log10_2lo + ivln10 * log(x);
        return z + y * log10_2hi;
    }

    // ---- hyperbolics (fdlibm), on exp/expm1 above ----

    /** The hyperbolic sine of {@code a}. */
    public static double sinh(double a) {
        final double one = 1.0d;
        final double shuge = 1.0e307d;
        double t;
        double w;
        double h;
        int jx = hi(a);
        int ix = jx & 0x7fffffff;
        if (ix >= 0x7ff00000) {
            return a + a;
        }
        h = 0.5d;
        if (jx < 0) {
            h = -h;
        }
        if (ix < 0x40360000) {
            if (ix < 0x3e300000) {
                if (shuge + a > one) {
                    return a;
                }
            }
            t = expm1(Math.abs(a));
            if (ix < 0x3ff00000) {
                return h * (2.0d * t - t * t / (t + one));
            }
            return h * (t + t / (t + one));
        }
        if (ix < 0x40862E42) {
            return h * exp(Math.abs(a));
        }
        if (ix <= 0x408633CE) {
            w = exp(0.5d * Math.abs(a));
            t = h * w;
            return t * w;
        }
        return a * shuge;
    }

    /** The hyperbolic cosine of {@code a}. */
    public static double cosh(double a) {
        final double one = 1.0d;
        final double half2 = 0.5d;
        final double huge = 1.0e300d;
        double t;
        double w;
        int ix = hi(a) & 0x7fffffff;
        if (ix >= 0x7ff00000) {
            return a * a;
        }
        if (ix < 0x3fd62e43) {
            t = expm1(Math.abs(a));
            w = one + t;
            if (ix < 0x3c800000) {
                return w;
            }
            return one + (t * t) / (w + w);
        }
        if (ix < 0x40360000) {
            t = exp(Math.abs(a));
            return half2 * t + half2 / t;
        }
        if (ix < 0x40862E42) {
            return half2 * exp(Math.abs(a));
        }
        if (ix <= 0x408633CE) {
            w = exp(half2 * Math.abs(a));
            t = half2 * w;
            return t * w;
        }
        return huge * huge;
    }

    /** The hyperbolic tangent of {@code a}. */
    public static double tanh(double a) {
        final double one = 1.0d;
        final double tiny = 1.0e-300d;
        double t;
        double z;
        int jx = hi(a);
        int ix = jx & 0x7fffffff;
        if (ix >= 0x7ff00000) {
            if (jx >= 0) {
                return one / a + one;
            }
            return one / a - one;
        }
        if (ix < 0x40360000) {
            if (ix < 0x3c800000) {
                return a * (one + a);
            }
            if (ix >= 0x3ff00000) {
                t = expm1(2.0d * Math.abs(a));
                z = one - 2.0d / (t + 2.0d);
            } else {
                t = expm1(-2.0d * Math.abs(a));
                z = -t / (t + 2.0d);
            }
        } else {
            z = one - tiny;
        }
        return (jx >= 0) ? z : -z;
    }

    // ---- inverse trigonometry (fdlibm) ----

    private static final double[] ATANHI = {
        4.63647609000806093515e-01, 7.85398163397448278999e-01,
        9.82793723247329054082e-01, 1.57079632679489655800e+00
    };
    private static final double[] ATANLO = {
        2.26987774529616870924e-17, 3.06161699786838301793e-17,
        1.39033110312309984516e-17, 6.12323399573676603587e-17
    };
    private static final double[] ATAN_T = {
        3.33333333333329318027e-01, -1.99999999998764832476e-01, 1.42857142725034663711e-01,
        -1.11111104054623557880e-01, 9.09088713343650656196e-02, -7.69187620504482999495e-02,
        6.66107313738753120669e-02, -5.83357013379057348645e-02, 4.97687799461593236017e-02,
        -3.65315727442169155270e-02, 1.62858201153657823623e-02
    };
    private static final double PIO2_HI = 1.57079632679489655800e+00;
    private static final double PIO2_LO = 6.12323399573676603587e-17;
    private static final double PIO4_HI = 7.85398163397448278999e-01;
    private static final double PS0 = 1.66666666666666657415e-01;
    private static final double PS1 = -3.25565818622400915405e-01;
    private static final double PS2 = 2.01212532134862925881e-01;
    private static final double PS3 = -4.00555345006794114027e-02;
    private static final double PS4 = 7.91534994289814532176e-04;
    private static final double PS5 = 3.47933107596021167570e-05;
    private static final double QS1 = -2.40339491173441421878e+00;
    private static final double QS2 = 2.02094576023350569471e+00;
    private static final double QS3 = -6.88283971605453293030e-01;
    private static final double QS4 = 7.70381505559019352791e-02;

    /** The arc tangent of {@code a}, in {@code [-pi/2, pi/2]}. */
    public static double atan(double a) {
        final double one = 1.0d;
        final double huge = 1.0e300d;
        double x = a;
        double w;
        double s1;
        double s2;
        double z;
        int id;
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        if (ix >= 0x44100000) {
            int low = lo(x);
            if (ix > 0x7ff00000 || (ix == 0x7ff00000 && low != 0)) {
                return x + x;
            }
            if (hx > 0) {
                return ATANHI[3] + ATANLO[3];
            }
            return -ATANHI[3] - ATANLO[3];
        }
        if (ix < 0x3fdc0000) {
            if (ix < 0x3e400000) {
                if (huge + x > one) {
                    return x;
                }
            }
            id = -1;
        } else {
            x = Math.abs(x);
            if (ix < 0x3ff30000) {
                if (ix < 0x3fe60000) {
                    id = 0;
                    x = (2.0d * x - one) / (2.0d + x);
                } else {
                    id = 1;
                    x = (x - one) / (x + one);
                }
            } else {
                if (ix < 0x40038000) {
                    id = 2;
                    x = (x - 1.5d) / (one + 1.5d * x);
                } else {
                    id = 3;
                    x = -1.0d / x;
                }
            }
        }
        z = x * x;
        w = z * z;
        s1 = z * (ATAN_T[0] + w * (ATAN_T[2] + w * (ATAN_T[4] + w * (ATAN_T[6] + w * (ATAN_T[8] + w * ATAN_T[10])))));
        s2 = w * (ATAN_T[1] + w * (ATAN_T[3] + w * (ATAN_T[5] + w * (ATAN_T[7] + w * ATAN_T[9]))));
        if (id < 0) {
            return x - x * (s1 + s2);
        }
        z = ATANHI[id] - ((x * (s1 + s2) - ATANLO[id]) - x);
        return (hx < 0) ? -z : z;
    }

    /** The arc sine of {@code a}, in {@code [-pi/2, pi/2]}; NaN outside {@code [-1, 1]}. */
    public static double asin(double a) {
        final double one = 1.0d;
        final double huge = 1.0e300d;
        double x = a;
        double t = 0.0d;
        double w;
        double p;
        double q;
        double c;
        double r;
        double s;
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        if (ix >= 0x3ff00000) {
            int lx = lo(x);
            if (((ix - 0x3ff00000) | lx) == 0) {
                return x * PIO2_HI + x * PIO2_LO;
            }
            return (x - x) / (x - x);
        } else if (ix < 0x3fe00000) {
            if (ix < 0x3e500000) {
                if (huge + x > one) {
                    return x;
                }
            } else {
                t = x * x;
                p = t * (PS0 + t * (PS1 + t * (PS2 + t * (PS3 + t * (PS4 + t * PS5)))));
                q = one + t * (QS1 + t * (QS2 + t * (QS3 + t * QS4)));
                w = p / q;
                return x + x * w;
            }
        }
        w = one - Math.abs(x);
        t = w * 0.5d;
        p = t * (PS0 + t * (PS1 + t * (PS2 + t * (PS3 + t * (PS4 + t * PS5)))));
        q = one + t * (QS1 + t * (QS2 + t * (QS3 + t * QS4)));
        s = sqrt(t);
        if (ix >= 0x3FEF3333) {
            w = p / q;
            t = PIO2_HI - (2.0d * (s + s * w) - PIO2_LO);
        } else {
            w = s;
            w = withLo(w, 0);
            c = (t - w * w) / (s + w);
            r = p / q;
            p = 2.0d * s * r - (PIO2_LO - 2.0d * c);
            q = PIO4_HI - 2.0d * w;
            t = PIO4_HI - (p - q);
        }
        return (hx > 0) ? t : -t;
    }

    /** The arc cosine of {@code a}, in {@code [0, pi]}; NaN outside {@code [-1, 1]}. */
    public static double acos(double a) {
        final double one = 1.0d;
        double x = a;
        double z;
        double p;
        double q;
        double r;
        double w;
        double s;
        double c;
        double df;
        int hx = hi(x);
        int ix = hx & 0x7fffffff;
        if (ix >= 0x3ff00000) {
            int lx = lo(x);
            if (((ix - 0x3ff00000) | lx) == 0) {
                if (hx > 0) {
                    return 0.0d;
                }
                return PI + 2.0d * PIO2_LO;
            }
            return (x - x) / (x - x);
        }
        if (ix < 0x3fe00000) {
            if (ix <= 0x3c600000) {
                return PIO2_HI + PIO2_LO;
            }
            z = x * x;
            p = z * (PS0 + z * (PS1 + z * (PS2 + z * (PS3 + z * (PS4 + z * PS5)))));
            q = one + z * (QS1 + z * (QS2 + z * (QS3 + z * QS4)));
            r = p / q;
            return PIO2_HI - (x - (PIO2_LO - x * r));
        } else if (hx < 0) {
            z = (one + x) * 0.5d;
            p = z * (PS0 + z * (PS1 + z * (PS2 + z * (PS3 + z * (PS4 + z * PS5)))));
            q = one + z * (QS1 + z * (QS2 + z * (QS3 + z * QS4)));
            s = sqrt(z);
            r = p / q;
            w = r * s - PIO2_LO;
            return PI - 2.0d * (s + w);
        } else {
            z = (one - x) * 0.5d;
            s = sqrt(z);
            df = s;
            df = withLo(df, 0);
            c = (z - df * df) / (s + df);
            p = z * (PS0 + z * (PS1 + z * (PS2 + z * (PS3 + z * (PS4 + z * PS5)))));
            q = one + z * (QS1 + z * (QS2 + z * (QS3 + z * QS4)));
            r = p / q;
            w = r * s + c;
            return 2.0d * (df + w);
        }
    }

    /**
     * The angle of the vector {@code (x, a)} — {@code atan2(a, x)} — in {@code [-pi, pi]}. Unlike the
     * classic fdlibm, huge {@code |a/x|} is not short-circuited: {@code atan(|a/x|)} handles it
     * (the ratio overflows to infinity and {@code atan(inf) = pi/2}), which is what the JDK does.
     */
    public static double atan2(double a, double x) {
        final double tiny = 1.0e-300d;
        final double zero = 0.0d;
        final double pi_o_4 = 7.8539816339744827900E-01d;
        final double pi_o_2 = 1.5707963267948965580E+00d;
        final double pi_lo = 1.2246467991473531772E-16d;
        double y = a;
        double z;
        int k;
        int m;
        int hx = hi(x);
        int lx = lo(x);
        int ix = hx & 0x7fffffff;
        int hy = hi(y);
        int ly = lo(y);
        int iy = hy & 0x7fffffff;
        if (((ix | ((lx | -lx) >>> 31)) > 0x7ff00000) || ((iy | ((ly | -ly) >>> 31)) > 0x7ff00000)) {
            return x + y;
        }
        if (((hx - 0x3ff00000) | lx) == 0) {
            return atan(y);
        }
        m = ((hy >>> 31) & 1) | ((hx >>> 30) & 2);
        if ((iy | ly) == 0) {
            switch (m) {
                case 0:
                case 1:
                    return y;
                case 2:
                    return PI + tiny;
                default:
                    return -PI - tiny;
            }
        }
        if ((ix | lx) == 0) {
            return (hy < 0) ? -pi_o_2 - tiny : pi_o_2 + tiny;
        }
        if (ix == 0x7ff00000) {
            if (iy == 0x7ff00000) {
                switch (m) {
                    case 0:
                        return pi_o_4 + tiny;
                    case 1:
                        return -pi_o_4 - tiny;
                    case 2:
                        return 3.0d * pi_o_4 + tiny;
                    default:
                        return -3.0d * pi_o_4 - tiny;
                }
            } else {
                switch (m) {
                    case 0:
                        return zero;
                    case 1:
                        return -zero;
                    case 2:
                        return PI + tiny;
                    default:
                        return -PI - tiny;
                }
            }
        }
        if (iy == 0x7ff00000) {
            return (hy < 0) ? -pi_o_2 - tiny : pi_o_2 + tiny;
        }
        k = (iy - ix) >> 20;
        if (hx < 0 && k < -60) {
            z = 0.0d;
        } else {
            z = atan(Math.abs(y / x));
        }
        switch (m) {
            case 0:
                return z;
            case 1:
                return -z;
            case 2:
                return PI - (z - pi_lo);
            default:
                return (z - pi_lo) - PI;
        }
    }

    // ---- Euclidean length (fdlibm e_hypot.c) ----

    /** {@code sqrt(x^2 + y^2)} without intermediate overflow or underflow. */
    public static double hypot(double x, double y) {
        double a;
        double b;
        double t1;
        double t2;
        double y1;
        double y2;
        double w;
        int j;
        int k;
        int ha = hi(x) & 0x7fffffff;
        int hb = hi(y) & 0x7fffffff;
        if (hb > ha) {
            a = y;
            b = x;
            j = ha;
            ha = hb;
            hb = j;
        } else {
            a = x;
            b = y;
        }
        a = withHi(a, ha);
        b = withHi(b, hb);
        if ((ha - hb) > 0x3c00000) {
            return a + b;
        }
        k = 0;
        if (ha > 0x5f300000) {
            if (ha >= 0x7ff00000) {
                int low;
                w = a + b;
                low = lo(a);
                if (((ha & 0xfffff) | low) == 0) {
                    w = a;
                }
                low = lo(b);
                if (((hb ^ 0x7ff00000) | low) == 0) {
                    w = b;
                }
                return w;
            }
            ha -= 0x25800000;
            hb -= 0x25800000;
            k += 600;
            a = withHi(a, ha);
            b = withHi(b, hb);
        }
        if (hb < 0x20b00000) {
            if (hb <= 0x000fffff) {
                int low = lo(b);
                if ((hb | low) == 0) {
                    return a;
                }
                t1 = withHi(0.0d, 0x7fd00000);
                b *= t1;
                a *= t1;
                k -= 1022;
            } else {
                ha += 0x25800000;
                hb += 0x25800000;
                k -= 600;
                a = withHi(a, ha);
                b = withHi(b, hb);
            }
        }
        w = a - b;
        if (w > b) {
            t1 = withHi(0.0d, ha);
            t2 = a - t1;
            w = sqrt(t1 * t1 - (b * (-b) - t2 * (a + t1)));
        } else {
            a = a + a;
            y1 = withHi(0.0d, hb);
            y2 = b - y1;
            t1 = withHi(0.0d, ha + 0x00100000);
            t2 = a - t1;
            w = sqrt(t1 * y1 - (w * (-w) - (t1 * y2 + t2 * b)));
        }
        if (k != 0) {
            int high = hi(1.0d);
            t1 = withHi(1.0d, high + (k << 20));
            return t1 * w;
        }
        return w;
    }

    // ---- x^y (fdlibm e_pow.c) ----

    /** {@code x} raised to {@code y}, with all the special cases the spec pins down. */
    public static double pow(double x, double y) {
        final double one = 1.0d;
        final double zero = 0.0d;
        final double two = 2.0d;
        final double huge = 1.0e300d;
        final double tiny = 1.0e-300d;
        final double two53 = 9007199254740992.0d;
        final double[] bp = {1.0d, 1.5d};
        final double[] dp_h = {0.0d, 5.84962487220764160156e-01d};
        final double[] dp_l = {0.0d, 1.35003920212974897128e-08d};
        final double L1 = 5.99999999999994648725e-01d;
        final double L2 = 4.28571428578550184252e-01d;
        final double L3 = 3.33333329818377432918e-01d;
        final double L4 = 2.72728123808534006489e-01d;
        final double L5 = 2.30660745775561366331e-01d;
        final double L6 = 2.06975017800338417784e-01d;
        final double P1 = 1.66666666666666019037e-01d;
        final double P2 = -2.77777777770155933842e-03d;
        final double P3 = 6.61375632143793436117e-05d;
        final double P4 = -1.65339022054652515390e-06d;
        final double P5 = 4.13813679705723846039e-08d;
        final double lg2 = 6.93147180559945286227e-01d;
        final double lg2_h = 6.93147182464599609375e-01d;
        final double lg2_l = -1.90465429995776804525e-09d;
        final double ovt = 8.0085662595372944372e-17d;
        final double cp = 9.61796693925975554329e-01d;
        final double cp_h = 9.61796700954437255859e-01d;
        final double cp_l = -7.02846165095275826516e-09d;
        final double ivln2 = 1.44269504088896338700e+00d;
        final double ivln2_h = 1.44269502162933349609e+00d;
        final double ivln2_l = 1.92596299112661746887e-08d;

        double z;
        double ax;
        double z_h;
        double z_l;
        double p_h;
        double p_l;
        double y1;
        double t1;
        double t2;
        double r;
        double s;
        double t;
        double u;
        double v;
        double w;
        int i;
        int j;
        int k;
        int yisint;
        int n;
        int hx = hi(x);
        int lx = lo(x);
        int hy = hi(y);
        int ly = lo(y);
        int ix = hx & 0x7fffffff;
        int iy = hy & 0x7fffffff;
        if ((iy | ly) == 0) {
            return one;
        }
        if (ix > 0x7ff00000 || (ix == 0x7ff00000 && lx != 0)
                || iy > 0x7ff00000 || (iy == 0x7ff00000 && ly != 0)) {
            return x + y;
        }
        yisint = 0;
        if (hx < 0) {
            if (iy >= 0x43400000) {
                yisint = 2;
            } else if (iy >= 0x3ff00000) {
                k = (iy >> 20) - 0x3ff;
                if (k > 20) {
                    j = ly >>> (52 - k);
                    if ((j << (52 - k)) == ly) {
                        yisint = 2 - (j & 1);
                    }
                } else if (ly == 0) {
                    j = iy >>> (20 - k);
                    if ((j << (20 - k)) == iy) {
                        yisint = 2 - (j & 1);
                    }
                }
            }
        }
        if (ly == 0) {
            if (iy == 0x7ff00000) {
                if (((ix - 0x3ff00000) | lx) == 0) {
                    return one;
                } else if (ix >= 0x3ff00000) {
                    return (hy >= 0) ? y : zero;
                } else {
                    return (hy < 0) ? -y : zero;
                }
            }
            if (iy == 0x3ff00000) {
                return (hy < 0) ? one / x : x;
            }
            if (hy == 0x40000000) {
                return x * x;
            }
            if (hy == 0x3fe00000) {
                if (hx >= 0) {
                    return sqrt(x);
                }
            }
        }
        ax = Math.abs(x);
        if (lx == 0) {
            if (ix == 0x7ff00000 || ix == 0 || ix == 0x3ff00000) {
                z = ax;
                if (hy < 0) {
                    z = one / z;
                }
                if (hx < 0) {
                    if (((ix - 0x3ff00000) | yisint) == 0) {
                        z = (z - z) / (z - z);
                    } else if (yisint == 1) {
                        z = -z;
                    }
                }
                return z;
            }
        }
        n = (hx >> 31) + 1;
        if ((n | yisint) == 0) {
            return (x - x) / (x - x);
        }
        s = one;
        if ((n | (yisint - 1)) == 0) {
            s = -one;
        }
        if (iy > 0x41e00000) {
            if (iy > 0x43f00000) {
                if (ix <= 0x3fefffff) {
                    return (hy < 0) ? huge * huge : tiny * tiny;
                }
                if (ix >= 0x3ff00000) {
                    return (hy > 0) ? huge * huge : tiny * tiny;
                }
            }
            if (ix < 0x3fefffff) {
                return (hy < 0) ? s * huge * huge : s * tiny * tiny;
            }
            if (ix > 0x3ff00000) {
                return (hy > 0) ? s * huge * huge : s * tiny * tiny;
            }
            t = ax - one;
            w = (t * t) * (0.5d - t * (0.3333333333333333333333d - t * 0.25d));
            u = ivln2_h * t;
            v = t * ivln2_l - w * ivln2;
            t1 = u + v;
            t1 = withLo(t1, 0);
            t2 = v - (t1 - u);
        } else {
            double s2;
            double s_h;
            double s_l;
            double t_h;
            double t_l;
            double ss;
            n = 0;
            if (ix < 0x00100000) {
                ax *= two53;
                n -= 53;
                ix = hi(ax);
            }
            n += ((ix) >> 20) - 0x3ff;
            j = ix & 0x000fffff;
            ix = j | 0x3ff00000;
            if (j <= 0x3988E) {
                k = 0;
            } else if (j < 0xBB67A) {
                k = 1;
            } else {
                k = 0;
                n += 1;
                ix -= 0x00100000;
            }
            ax = withHi(ax, ix);
            u = ax - bp[k];
            v = one / (ax + bp[k]);
            ss = u * v;
            s_h = ss;
            s_h = withLo(s_h, 0);
            t_h = withHi(0.0d, ((ix >> 1) | 0x20000000) + 0x00080000 + (k << 18));
            t_l = ax - (t_h - bp[k]);
            s_l = v * ((u - s_h * t_h) - s_h * t_l);
            s2 = ss * ss;
            r = s2 * s2 * (L1 + s2 * (L2 + s2 * (L3 + s2 * (L4 + s2 * (L5 + s2 * L6)))));
            r += s_l * (s_h + ss);
            s2 = s_h * s_h;
            t_h = 3.0d + s2 + r;
            t_h = withLo(t_h, 0);
            t_l = r - ((t_h - 3.0d) - s2);
            u = s_h * t_h;
            v = s_l * t_h + t_l * ss;
            p_h = u + v;
            p_h = withLo(p_h, 0);
            p_l = v - (p_h - u);
            z_h = cp_h * p_h;
            z_l = cp_l * p_h + p_l * cp + dp_l[k];
            t = (double) n;
            t1 = (((z_h + z_l) + dp_h[k]) + t);
            t1 = withLo(t1, 0);
            t2 = z_l - (((t1 - t) - dp_h[k]) - z_h);
        }
        y1 = y;
        y1 = withLo(y1, 0);
        p_l = (y - y1) * t1 + y * t2;
        p_h = y1 * t1;
        z = p_l + p_h;
        j = hi(z);
        i = lo(z);
        if (j >= 0x40900000) {
            if (((j - 0x40900000) | i) != 0) {
                return s * huge * huge;
            } else {
                if (p_l + ovt > z - p_h) {
                    return s * huge * huge;
                }
            }
        } else if ((j & 0x7fffffff) >= 0x4090cc00) {
            if (((j - 0xc090cc00) | i) != 0) {
                return s * tiny * tiny;
            } else {
                if (p_l <= z - p_h) {
                    return s * tiny * tiny;
                }
            }
        }
        i = j & 0x7fffffff;
        k = (i >> 20) - 0x3ff;
        n = 0;
        if (i > 0x3fe00000) {
            n = j + (0x00100000 >> (k + 1));
            k = ((n & 0x7fffffff) >> 20) - 0x3ff;
            t = withHi(0.0d, (n & ~(0x000fffff >> k)));
            n = ((n & 0x000fffff) | 0x00100000) >> (20 - k);
            if (j < 0) {
                n = -n;
            }
            p_h -= t;
        }
        t = p_l + p_h;
        t = withLo(t, 0);
        u = t * lg2_h;
        v = (p_l - (t - p_h)) * lg2 + t * lg2_l;
        z = u + v;
        w = v - (z - u);
        t = z * z;
        t1 = z - t * (P1 + t * (P2 + t * (P3 + t * (P4 + t * P5))));
        r = (z * t1) / (t1 - two) - (w + z * w);
        z = one - (r - z);
        j = hi(z);
        j += (n << 20);
        if ((j >> 20) <= 0) {
            z = scalb(z, n);
        } else {
            z = withHi(z, j);
        }
        return s * z;
    }

}
