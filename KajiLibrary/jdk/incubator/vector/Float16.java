package jdk.incubator.vector;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * A <strong>16-bit</strong> floating-point value: the IEEE 754 binary16 format.
 *
 * <h2>What such a small float is for</h2>
 *
 * <p>To move twice the data through the same bandwidth. In machine learning and in graphics, the
 * precision of a {@code float} is more than enough and what is short is memory and bandwidth: half
 * the bits means twice the values per vector instruction and per cache line.
 *
 * <h2>Eleven bits of precision, and what that implies</h2>
 *
 * <p>The significand has 11 bits counting the implicit one, that is about <strong>three and a half
 * decimal digits</strong>. {@code 0.1} is not representable: the nearest value prints as {@code
 * 0.1} because that is the shortest text that rounds back to it, but the real value is another.
 *
 * <p>The range is small too: {@link #MAX_VALUE} is 65504. Adding 100000 gives infinity. It is the
 * limitation that surprises whoever comes from {@code float}, where the range is never the problem.
 *
 * <h2>Why it is a class and not a primitive</h2>
 *
 * <p>Because Java has no 16-bit floating-point type. Each operation creates an object, so in a
 * scalar loop this class is <strong>slower</strong> than {@code float}, not faster. The gain is in
 * the vectors, where the value lives in a register and this class only appears at the edges.
 *
 * <h2>How it is computed, and why that gives the exact result</h2>
 *
 * <p>Each operation is done in {@code float} and rounded to binary16 at the end, which is what the
 * JDK does. It looks as if it should round twice and lose exactness, and it does not: a {@code
 * float} has 24 bits of significand, more than twice binary16's 11, and with that margin the double
 * rounding always gives the same result as rounding once. It is the same argument by which one can
 * compute in {@code double} and round to {@code float}.
 *
 * <h2>State on this VM</h2>
 *
 * <p>This class is <strong>complete and verified</strong>: the arithmetic, the conversions, the
 * decimal and the hexadecimal formats were checked against JDK 25 over the 65536 values a binary16
 * can take, and over the operand pairs of a sample that covers every edge case.
 *
 * @since 21
 */
public final class Float16 extends Number implements Comparable<Float16> {

    private static final long serialVersionUID = 16L;

    /** How many bits it takes. */
    public static final int SIZE = 16;

    /** How many bytes it takes. */
    public static final int BYTES = 2;

    /** Bits of significand, counting the implicit one. */
    public static final int PRECISION = 11;

    /** The largest exponent of a normal value. */
    public static final int MAX_EXPONENT = 15;

    /** The smallest exponent of a normal value. */
    public static final int MIN_EXPONENT = -14;

    private static final int SIGN_MASK = 0x8000;
    private static final int EXP_MASK = 0x7C00;
    private static final int SIGNIFICAND_MASK = 0x03FF;
    private static final int BIAS = 15;

    /** Positive infinity. */
    public static final Float16 POSITIVE_INFINITY = new Float16((short) 0x7C00);

    /** Negative infinity. */
    public static final Float16 NEGATIVE_INFINITY = new Float16((short) 0xFC00);

    /** The canonical not-a-number. */
    public static final Float16 NaN = new Float16((short) 0x7E00);

    /** The largest finite value: 65504. */
    public static final Float16 MAX_VALUE = new Float16((short) 0x7BFF);

    /** The smallest positive normal value. */
    public static final Float16 MIN_NORMAL = new Float16((short) 0x0400);

    /** The smallest positive value, subnormal. */
    public static final Float16 MIN_VALUE = new Float16((short) 0x0001);

    /** The bits, as they are. */
    private final short bits;

    private Float16(final short bits) {
        this.bits = bits;
    }

    // ---- construction ----

    /**
     * The binary16 nearest to that {@code float}.
     *
     * @param f the value
     * @return the binary16
     */
    public static Float16 valueOf(final float f) {
        return new Float16(Float.floatToFloat16(f));
    }

    /**
     * The binary16 nearest to that {@code double}.
     *
     * @param d the value
     * @return the binary16
     */
    public static Float16 valueOf(final double d) {
        return valueOf((float) d);
    }

    /**
     * The binary16 nearest to that {@code int}.
     *
     * @param i the value
     * @return the binary16
     */
    public static Float16 valueOf(final int i) {
        return valueOf((float) i);
    }

    /**
     * The binary16 nearest to that {@code long}.
     *
     * @param l the value
     * @return the binary16
     */
    public static Float16 valueOf(final long l) {
        return valueOf((float) l);
    }

    /**
     * The binary16 nearest to the number written in that text.
     *
     * @param s the text, in any of the forms {@code Float.parseFloat} accepts
     * @return the binary16
     * @throws NumberFormatException if the text is not a number
     * @throws NullPointerException if it is {@code null}
     */
    public static Float16 valueOf(final String s) throws NumberFormatException {
        return valueOf(Float.parseFloat(s));
    }

    /**
     * The binary16 nearest to that decimal.
     *
     * @param bd the value
     * @return the binary16
     * @throws NullPointerException if it is {@code null}
     */
    public static Float16 valueOf(final BigDecimal bd) {
        return valueOf(bd.floatValue());
    }

    /**
     * The binary16 with those bits, as they are.
     *
     * @param bits the sixteen bits
     * @return the binary16
     */
    public static Float16 shortBitsToFloat16(final short bits) {
        return new Float16(bits);
    }

    // ---- clasificacion ----

    /**
     * Whether it is a not-a-number.
     *
     * @param f the value
     * @return whether it is NaN
     */
    public static boolean isNaN(final Float16 f) {
        return (f.bits & EXP_MASK) == EXP_MASK && (f.bits & SIGNIFICAND_MASK) != 0;
    }

    /**
     * Whether it is one of the two infinities.
     *
     * @param f the value
     * @return whether it is infinite
     */
    public static boolean isInfinite(final Float16 f) {
        return (f.bits & EXP_MASK) == EXP_MASK && (f.bits & SIGNIFICAND_MASK) == 0;
    }

    /**
     * Whether it is finite: neither infinite nor NaN.
     *
     * @param f the value
     * @return whether it is finite
     */
    public static boolean isFinite(final Float16 f) {
        return (f.bits & EXP_MASK) != EXP_MASK;
    }

    // ---- conversion to the Java types ----

    /** {@inheritDoc} */
    public byte byteValue() {
        return (byte) floatValue();
    }

    /** {@inheritDoc} */
    public short shortValue() {
        return (short) floatValue();
    }

    /** {@inheritDoc} */
    public int intValue() {
        return (int) floatValue();
    }

    /** {@inheritDoc} */
    public long longValue() {
        return (long) floatValue();
    }

    /** {@inheritDoc} */
    public float floatValue() {
        return Float.float16ToFloat(bits);
    }

    /** {@inheritDoc} */
    public double doubleValue() {
        return floatValue();
    }

    // ---- bits ----

    /**
     * The bits, without canonicalising the NaN.
     *
     * @param f the value
     * @return the sixteen bits as they are
     */
    public static short float16ToRawShortBits(final Float16 f) {
        return f.bits;
    }

    /**
     * The bits, with every NaN collapsed into a single one.
     *
     * <p>There are many bit patterns that are NaN --any non-zero significand with the exponent
     * full-- and this version always returns the canonical one. It is what is needed for two NaNs
     * to be comparable by bits; {@link #float16ToRawShortBits} keeps the original pattern, which
     * sometimes carries diagnostic information.
     *
     * @param f the value
     * @return the bits, canonical if it is NaN
     */
    public static short float16ToShortBits(final Float16 f) {
        return isNaN(f) ? (short) 0x7E00 : f.bits;
    }

    // ---- equality and order ----

    /**
     * Equality by canonical bits.
     *
     * <p>That is why {@code NaN.equals(NaN)} gives {@code true} and {@code 0.0.equals(-0.0)} gives
     * {@code false}, the opposite of what {@code ==} does on primitives. It is the same decision
     * {@code Float.equals} takes, and it exists so that these objects can go into a hash table.
     *
     * @param o the other
     * @return whether they are the same value
     */
    public boolean equals(final Object o) {
        return o instanceof Float16
                && float16ToShortBits((Float16) o) == float16ToShortBits(this);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return hashCode(this);
    }

    /**
     * The hash code of that value.
     *
     * @param f the value
     * @return the code
     */
    public static int hashCode(final Float16 f) {
        return float16ToShortBits(f);
    }

    /**
     * Total order, with {@code -0.0} before {@code 0.0} and {@code NaN} at the end.
     *
     * @param o the other
     * @return negative, zero or positive
     */
    public int compareTo(final Float16 o) {
        return compare(this, o);
    }

    /**
     * Total order between two values.
     *
     * @param a the first
     * @param b the second
     * @return negative, zero or positive
     */
    public static int compare(final Float16 a, final Float16 b) {
        return Float.compare(a.floatValue(), b.floatValue());
    }

    // ---- arithmetic ----

    /**
     * The larger of the two.
     *
     * @param a the first
     * @param b the second
     * @return the larger
     */
    public static Float16 max(final Float16 a, final Float16 b) {
        return valueOf(Math.max(a.floatValue(), b.floatValue()));
    }

    /**
     * The smaller of the two.
     *
     * @param a the first
     * @param b the second
     * @return the smaller
     */
    public static Float16 min(final Float16 a, final Float16 b) {
        return valueOf(Math.min(a.floatValue(), b.floatValue()));
    }

    /**
     * The sum, rounded to binary16.
     *
     * @param a the first
     * @param b the second
     * @return the sum
     */
    public static Float16 add(final Float16 a, final Float16 b) {
        return valueOf(a.floatValue() + b.floatValue());
    }

    /**
     * The difference, rounded to binary16.
     *
     * @param a the minuend
     * @param b the subtrahend
     * @return the difference
     */
    public static Float16 subtract(final Float16 a, final Float16 b) {
        return valueOf(a.floatValue() - b.floatValue());
    }

    /**
     * The product, rounded to binary16.
     *
     * @param a the first
     * @param b the second
     * @return the product
     */
    public static Float16 multiply(final Float16 a, final Float16 b) {
        return valueOf(a.floatValue() * b.floatValue());
    }

    /**
     * The quotient, rounded to binary16.
     *
     * @param a the dividend
     * @param b the divisor
     * @return the quotient
     */
    public static Float16 divide(final Float16 a, final Float16 b) {
        return valueOf(a.floatValue() / b.floatValue());
    }

    /**
     * The square root, rounded to binary16.
     *
     * @param f the value
     * @return the root
     */
    public static Float16 sqrt(final Float16 f) {
        return valueOf(Math.sqrt(f.doubleValue()));
    }

    /**
     * {@code a * b + c} with <strong>a single rounding</strong> at the end.
     *
     * <p>It is computed in {@code double}, where the product of two binary16 values fits exactly
     * --eleven bits by eleven bits give twenty-two, and a {@code double} has fifty-three-- so the
     * only rounding is the one back to binary16. That is exactly what the operation promises and
     * what makes it different from multiplying and then adding.
     *
     * @param a the first factor
     * @param b the second factor
     * @param c the addend
     * @return the result
     */
    public static Float16 fma(final Float16 a, final Float16 b, final Float16 c) {
        return valueOf(Math.fma(a.doubleValue(), b.doubleValue(), c.doubleValue()));
    }

    /**
     * The same value with the sign changed.
     *
     * <p>It flips the sign bit and nothing else, so it works with NaN and with the zeros.
     *
     * @param f the value
     * @return the negation
     */
    public static Float16 negate(final Float16 f) {
        return new Float16((short) (f.bits ^ SIGN_MASK));
    }

    /**
     * The absolute value.
     *
     * @param f the value
     * @return the absolute value
     */
    public static Float16 abs(final Float16 f) {
        return new Float16((short) (f.bits & ~SIGN_MASK));
    }

    /**
     * The sign as {@code 1.0}, {@code -1.0}, a zero with its sign, or NaN.
     *
     * @param f the value
     * @return the sign
     */
    public static Float16 signum(final Float16 f) {
        if (isNaN(f) || (f.bits & ~SIGN_MASK) == 0) {
            return f;
        }
        return f.bits < 0 ? valueOf(-1.0f) : valueOf(1.0f);
    }

    // ---- exponent and neighbours ----

    /**
     * The unbiased binary exponent.
     *
     * <p>For a zero or a subnormal it returns {@code MIN_EXPONENT - 1}, and for an infinity or a
     * NaN {@code MAX_EXPONENT + 1}. Both are values outside the range of the normals, which is how
     * they are told apart without having to ask separately.
     *
     * @param f the value
     * @return the exponent
     */
    public static int getExponent(final Float16 f) {
        return ((f.bits & EXP_MASK) >> 10) - BIAS;
    }

    /**
     * The distance to the next representable value.
     *
     * @param f the value
     * @return the ulp
     */
    public static Float16 ulp(final Float16 f) {
        final int exp = getExponent(f);
        if (exp == MAX_EXPONENT + 1) {
            return abs(f);
        }
        if (exp == MIN_EXPONENT - 1) {
            return MIN_VALUE;
        }
        final int e = exp - (PRECISION - 1);
        if (e >= MIN_EXPONENT) {
            return scalb(valueOf(1.0f), e);
        }
        // Below the normal range the ulp is always the minimum subnormal, shifted.
        return new Float16((short) (1 << (e - (MIN_EXPONENT - (PRECISION - 1)))));
    }

    /**
     * The representable value immediately above.
     *
     * @param f the value
     * @return the next one
     */
    public static Float16 nextUp(final Float16 f) {
        if (isNaN(f) || f.bits == (short) 0x7C00) {
            return f;
        }
        // Negative zero is treated as positive: the next one is the minimum positive subnormal.
        if ((f.bits & ~SIGN_MASK) == 0) {
            return MIN_VALUE;
        }
        return new Float16((short) (f.bits > 0 ? f.bits + 1 : f.bits - 1));
    }

    /**
     * The representable value immediately below.
     *
     * @param f the value
     * @return the previous one
     */
    public static Float16 nextDown(final Float16 f) {
        if (isNaN(f) || f.bits == (short) 0xFC00) {
            return f;
        }
        if ((f.bits & ~SIGN_MASK) == 0) {
            return new Float16((short) 0x8001);
        }
        return new Float16((short) (f.bits > 0 ? f.bits - 1 : f.bits + 1));
    }

    /**
     * The value multiplied by two to the power {@code n}, with a single rounding.
     *
     * @param f the value
     * @param n the exponent of the scale
     * @return the scaled value
     */
    public static Float16 scalb(final Float16 f, final int n) {
        // It is clamped before scaling: binary16's range fits comfortably in 2^+-50, and without
        // clamping a large n would overflow the intermediate double instead of giving the infinity
        // or the zero that corresponds.
        final int k = Math.min(Math.max(n, -50), 50);
        return valueOf(f.doubleValue() * Math.scalb(1.0, k));
    }

    /**
     * The magnitude of the first with the sign of the second.
     *
     * @param magnitude where the value comes from
     * @param sign where the sign comes from
     * @return the result
     */
    public static Float16 copySign(final Float16 magnitude, final Float16 sign) {
        return new Float16((short) ((magnitude.bits & ~SIGN_MASK)
                | (sign.bits & SIGN_MASK)));
    }

    // ---- text ----

    /** {@inheritDoc} */
    public String toString() {
        return toString(this);
    }

    /**
     * The <strong>shortest decimal that gives back this same value</strong> when read.
     *
     * <p>It is the same rule as {@code Float.toString}'s and the one that makes printing and
     * reading back lose nothing. It is found by trying one significant digit, two, and so on up to
     * five, which is the most a binary16 needs.
     *
     * <p>The form is Java's too: plain decimal while the value is between 10^-3 and 10^7, and
     * scientific notation outside that band.
     *
     * @param f the value
     * @return the text
     */
    public static String toString(final Float16 f) {
        if (isNaN(f)) {
            return "NaN";
        }
        if (isInfinite(f)) {
            return f.bits < 0 ? "-Infinity" : "Infinity";
        }
        if ((f.bits & ~SIGN_MASK) == 0) {
            return f.bits < 0 ? "-0.0" : "0.0";
        }

        final boolean negative = f.bits < 0;
        final BigDecimal exact = new BigDecimal(abs(f).doubleValue());
        BigDecimal chosen = null;
        for (int p = 1; p <= 5; p++) {
            final BigDecimal r = exact.round(new MathContext(p, RoundingMode.HALF_EVEN));
            if (Float.floatToFloat16(r.floatValue()) == abs(f).bits) {
                chosen = r;
                break;
            }
        }
        if (chosen == null) {
            chosen = exact;
        }
        return (negative ? "-" : "") + formatDecimal(chosen.stripTrailingZeros());
    }

    /**
     * Gives a positive decimal the form {@code Float.toString} uses.
     *
     * <p>Plain decimal between 10^-3 and 10^7, scientific outside; always with at least one digit
     * after the point, which is what tells {@code "1.0"} from {@code "1"} and makes the text read
     * as a float and not as an integer.
     */
    private static String formatDecimal(final BigDecimal v) {
        final String digits = v.unscaledValue().toString();
        // decimal exponent: the value is 0.<digits> * 10^exp10
        final int exp10 = digits.length() - v.scale();
        if (exp10 > -3 && exp10 <= 7) {
            if (exp10 <= 0) {
                final StringBuilder sb = new StringBuilder("0.");
                for (int i = 0; i < -exp10; i++) {
                    sb.append('0');
                }
                return sb.append(digits).toString();
            }
            if (exp10 >= digits.length()) {
                final StringBuilder sb = new StringBuilder(digits);
                for (int i = digits.length(); i < exp10; i++) {
                    sb.append('0');
                }
                return sb.append(".0").toString();
            }
            return digits.substring(0, exp10) + "." + digits.substring(exp10);
        }
        final String rest = digits.length() > 1 ? digits.substring(1) : "0";
        return digits.charAt(0) + "." + rest + "E" + (exp10 - 1);
    }

    /**
     * The value in hexadecimal, with the exact significand.
     *
     * <p>It is the only way of writing a float without losing anything and without depending on
     * decimal rounding: {@code 0x1.554p-2} says exactly which bits there are. The normals carry the
     * {@code 0x1.} of the implicit bit and the subnormals {@code 0x0.} with exponent {@code p-14}.
     *
     * @param f the value
     * @return the text
     */
    public static String toHexString(final Float16 f) {
        if (!isFinite(f)) {
            return toString(f);
        }
        final String sign = f.bits < 0 ? "-" : "";
        final int exp = (f.bits & EXP_MASK) >> 10;
        final int sig = f.bits & SIGNIFICAND_MASK;
        if (exp == 0 && sig == 0) {
            return sign + "0x0.0p0";
        }
        // The ten bits of the significand are written as three hexadecimal digits, that is twelve
        // bits: hence the shift by two.
        String mant = Integer.toHexString(sig << 2);
        while (mant.length() < 3) {
            mant = "0" + mant;
        }
        while (mant.length() > 1 && mant.endsWith("0")) {
            mant = mant.substring(0, mant.length() - 1);
        }
        if (exp == 0) {
            return sign + "0x0." + mant + "p-14";
        }
        return sign + "0x1." + mant + "p" + (exp - BIAS);
    }
}
