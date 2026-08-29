package java.lang;

// KajiLibrary's java.lang.Float — the boxed-float wrapper (extends Number, implements
// Comparable). The narrowing views (intValue/longValue) need an explicit cast.
public final class Float extends Number implements Comparable<Float>, ConstantDesc {

    private final float value;

    public Float(float value) {
        this.value = value;
    }

    public static Float valueOf(float f) {
        return new Float(f);
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return (long) value;
    }

    public float floatValue() {
        return value;
    }

    public double doubleValue() {
        return value;
    }

    public int compareTo(Float o) {
        return this.value < o.value ? -1 : (this.value > o.value ? 1 : 0);
    }

    // The shortest decimal that round-trips to this float; delegates to Double's shared
    // shortest-decimal machinery in float mode (round-trip against float precision).
    public static String toString(float f) {
        return Double.shortestDecimal((double) f, true);
    }

    public String toString() {
        return Float.toString(this.value);
    }

    // ---- the extremes, as bit patterns rather than literals ----
    //
    // See `Double` for why these are built from bits: a decimal literal for MIN_VALUE is
    // 1.4E-45, which is a rounding of the value meant rather than the value itself.

    /** The largest finite value. */
    public static final float MAX_VALUE = Float.intBitsToFloat(0x7f7fffff);

    /** The smallest positive value, which is subnormal. */
    public static final float MIN_VALUE = Float.intBitsToFloat(0x1);

    /** The smallest positive value with a full significand. */
    public static final float MIN_NORMAL = Float.intBitsToFloat(0x00800000);

    /** Positive infinity. */
    public static final float POSITIVE_INFINITY = Float.intBitsToFloat(0x7f800000);

    /** Negative infinity. */
    public static final float NEGATIVE_INFINITY = Float.intBitsToFloat(0xff800000);

    /** Not-a-number. Note that {@code Float.NaN == Float.NaN} is false. */
    public static final float NaN = Float.intBitsToFloat(0x7fc00000);

    /** The largest exponent a finite value may have. */
    public static final int MAX_EXPONENT = 127;

    /** The smallest exponent a normal value may have. */
    public static final int MIN_EXPONENT = -126;

    /** Bits in a float. */
    public static final int SIZE = 32;

    /** Bytes in a float. */
    public static final int BYTES = 4;

    /** Bits of precision in the significand, counting the implicit leading one. */
    public static final int PRECISION = 24;

    // ---- the bits ----

    /**
     * The IEEE-754 bits of {@code value}, with every NaN collapsed to one pattern.
     *
     * @param value the value to read
     * @see Double#doubleToLongBits(double) for why the collapsing matters
     */
    public static int floatToIntBits(float value) {
        if (value != value) {
            return 0x7fc00000;
        }
        return Float.floatToRawIntBits(value);
    }

    /**
     * The IEEE-754 bits of {@code value}, exactly as they are.
     *
     * @param value the value to read
     */
    public static native int floatToRawIntBits(float value);

    /**
     * The float those IEEE-754 bits describe.
     *
     * @param bits the bit pattern
     */
    public static native float intBitsToFloat(int bits);

    // ---- classification ----

    /**
     * Whether {@code v} is not-a-number.
     *
     * @param v the value to test
     */
    public static boolean isNaN(float v) {
        return v != v;
    }

    /** Whether this value is not-a-number. */
    public boolean isNaN() {
        return Float.isNaN(this.value);
    }

    /**
     * Whether {@code v} is an infinity.
     *
     * @param v the value to test
     */
    public static boolean isInfinite(float v) {
        return v == Float.POSITIVE_INFINITY || v == Float.NEGATIVE_INFINITY;
    }

    /** Whether this value is an infinity. */
    public boolean isInfinite() {
        return Float.isInfinite(this.value);
    }

    /**
     * Whether {@code f} is neither infinite nor NaN.
     *
     * @param f the value to test
     */
    public static boolean isFinite(float f) {
        return !Float.isNaN(f) && !Float.isInfinite(f);
    }

    // ---- ordering and identity ----

    /**
     * A total order over floats, which {@code <} is not.
     *
     * @param f1 one value
     * @param f2 the other
     * @see Double#compare(double, double) for the two places it differs from {@code <}
     */
    public static int compare(float f1, float f2) {
        if (f1 < f2) {
            return -1;
        }
        if (f1 > f2) {
            return 1;
        }
        int b1 = Float.floatToIntBits(f1);
        int b2 = Float.floatToIntBits(f2);
        if (b1 == b2) {
            return 0;
        }
        if (b1 < b2) {
            return -1;
        }
        return 1;
    }

    /**
     * Equal when {@code other} is a Float with the same BITS.
     *
     * @param other the object to compare against
     * @see Double#equals(Object) for why bits rather than {@code ==}
     */
    public boolean equals(Object other) {
        if (!(other instanceof Float)) {
            return false;
        }
        Float that = (Float) other;
        return Float.floatToIntBits(this.value) == Float.floatToIntBits(that.floatValue());
    }

    /** A hash consistent with {@link #equals}, which is simply the bits. */
    public int hashCode() {
        return Float.hashCode(this.value);
    }

    /**
     * The hash of a float, without boxing it.
     *
     * @param value the value to hash
     */
    public static int hashCode(float value) {
        return Float.floatToIntBits(value);
    }

    // ---- the arithmetic a reduction needs ----

    /**
     * The sum, as a method so it can be passed as a function.
     *
     * @param a one addend
     * @param b the other
     */
    public static float sum(float a, float b) {
        return a + b;
    }

    /**
     * The greater of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    /**
     * The smaller of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    /**
     * {@code f} in the hexadecimal form that round-trips exactly.
     *
     * <p>Routed through the double form, which is exact: widening a float to a double changes no
     * bit of the value, so the hex rendering is the same string.
     *
     * @param f the value to render
     */
    public static String toHexString(float f) {
        if (Float.isNaN(f)) {
            return "NaN";
        }
        if (f == Float.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (f == Float.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        int bits = Float.floatToRawIntBits(f);
        int exponent = (bits >> 23) & 0xff;
        if (exponent != 0) {
            // Normal: the widened double has the same significand and an equivalent exponent.
            return Double.toHexString((double) f);
        }
        int significand = bits & 0x007fffff;
        String sign = "";
        if (bits < 0) {
            sign = "-";
        }
        if (significand == 0) {
            return sign + "0x0.0p0";
        }
        // Subnormal floats must show the FLOAT minimum exponent, which widening would lose --
        // as a double the same value is a perfectly ordinary normal number.
        return sign + "0x0." + Float.hexSignificand(significand) + "p-126";
    }

    // The 23 significand bits as 6 hex digits, left-aligned in the nibble grid and trimmed.
    private static String hexSignificand(int significand) {
        String digits = "";
        int shift = 20;
        while (shift >= 0) {
            int nibble = (significand >> shift) & 0xf;
            digits = digits + Float.hexDigit(nibble);
            shift = shift - 4;
        }
        int end = digits.length();
        while (end > 1 && digits.charAt(end - 1) == '0') {
            end = end - 1;
        }
        return digits.substring(0, end);
    }

    private static char hexDigit(int nibble) {
        if (nibble < 10) {
            return (char) ('0' + nibble);
        }
        return (char) ('a' + (nibble - 10));
    }


    /**
     * A Float holding the given value, narrowed.
     *
     * @param value the value to hold
     * @deprecated the JDK deprecates every wrapper constructor; use {@link #valueOf(float)}.
     */
    @Deprecated(since = "9")
    public Float(double value) {
        this((float) value);
    }

    /**
     * The float nearest to what {@code s} says, rounding half to even.
     *
     * <p>Read directly at float precision rather than by narrowing a double, which would round
     * TWICE -- and two roundings can land somewhere one would not. The shared reader in
     * {@link Double} takes the format as parameters for exactly this reason.
     *
     * @param s the text to read
     * @throws NumberFormatException if the text does not denote a number
     */
    public static float parseFloat(String s) {
        long bits = Double.parseBits(s, 24, -149, 254, 23);
        return Float.intBitsToFloat((int) bits);
    }

    /**
     * The Float nearest to what {@code s} says.
     *
     * @param s the text to read
     * @throws NumberFormatException if the text does not denote a number
     */
    public static Float valueOf(String s) {
        return Float.valueOf(Float.parseFloat(s));
    }

    /**
     * A Float holding what {@code s} says.
     *
     * @param s the text to read
     * @throws NumberFormatException if the text does not denote a number
     * @deprecated the JDK deprecates every wrapper constructor; use {@link #valueOf(String)}.
     */
    @Deprecated(since = "9")
    public Float(String s) {
        this(Float.parseFloat(s));
    }

    /**
     * Resolves this descriptor, which for a boxed number is itself.
     *
     * <p>Cannot fail, which is what separates the self-describing values from the descriptors
     * that name something else -- see {@link ConstantDesc}. The {@link Optional} that
     * {@code describeConstable} returns looks pointless for the same reason, and is not: both
     * come from interfaces whose other implementations really can fail.
     *
     * @param lookup accepted and unused; there is nothing to look up
     */
    public Float resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup lookup) {
        return Float.valueOf(this.floatValue());
    }


    /** This value as a byte, truncated. */
    public byte byteValue() {
        return (byte) this.floatValue();
    }

    /** This value as a short, truncated. */
    public short shortValue() {
        return (short) this.floatValue();
    }

    /** This value as a nominal descriptor, which is always present. */
    public Optional<Float> describeConstable() {
        return Optional.of(Float.valueOf(this.floatValue()));
    }

    // ---- half precision ----
    //
    // IEEE-754 binary16: one sign bit, five of exponent, ten of significand. Java has no `half`
    // type, so the format lives in a `short` and these two methods are the only way in and out.
    // It exists for storage and for the arithmetic of machine learning, where the range matters
    // more than the precision.
    //
    // The conversion in is where the work is: sixteen bits cannot hold what thirty-two can, so
    // it has to round -- half to even -- and it has to handle the two edges where the formats
    // disagree about what is representable at all. A float that is an ordinary normal number can
    // land in the half format as a subnormal, or as an infinity, and both are correct answers.

    /**
     * The half-precision value nearest to {@code f}, as the bits of a binary16 in a short.
     *
     * @param f the value to narrow
     */
    public static short floatToFloat16(float f) {
        int bits = Float.floatToRawIntBits(f);
        int sign = (bits >>> 16) & 0x8000;
        int exponent = (bits >>> 23) & 0xff;
        int significand = bits & 0x7fffff;
        if (exponent == 0xff) {
            if (significand == 0) {
                return (short) (sign | 0x7c00);
            }
            // A NaN must stay a NaN: keep whatever payload fits, and never let it become zero,
            // which would turn it into an infinity.
            int payload = significand >> 13;
            if (payload == 0) {
                payload = 1;
            }
            return (short) (sign | 0x7c00 | payload);
        }
        int e = exponent - 127;
        if (exponent == 0 && significand == 0) {
            return (short) sign;
        }
        if (e > 15) {
            return (short) (sign | 0x7c00);
        }
        if (e >= -14) {
            // Normal on both sides: drop thirteen bits and round on what was dropped. A carry
            // out of the significand runs into the exponent field, which is exactly right --
            // and at the top it becomes an infinity, which is also exactly right.
            int half = sign | ((e + 15) << 10) | (significand >> 13);
            int dropped = significand & 0x1fff;
            if (dropped > 0x1000 || (dropped == 0x1000 && ((significand >> 13) & 1) != 0)) {
                half = half + 1;
            }
            return (short) half;
        }
        // Subnormal in the half format: the exponent cannot go lower, so precision is given up
        // instead of range.
        if (e < -25) {
            return (short) sign;
        }
        int full = significand | 0x800000;
        int shift = 13 - e - 14;
        int kept = full >>> shift;
        int dropped = full & ((1 << shift) - 1);
        int halfway = 1 << (shift - 1);
        int half = sign | kept;
        if (dropped > halfway || (dropped == halfway && (kept & 1) != 0)) {
            half = half + 1;
        }
        return (short) half;
    }

    /**
     * The float equal to the binary16 whose bits are in {@code bits}.
     *
     * <p>Exact, and it cannot fail: every half-precision value is a float, including the
     * subnormal ones -- which stop being subnormal on the way, since a float has room to
     * normalise them.
     *
     * @param bits the binary16 bit pattern
     */
    public static float float16ToFloat(short bits) {
        int b = bits & 0xffff;
        int sign = (b & 0x8000) << 16;
        int exponent = (b >>> 10) & 0x1f;
        int significand = b & 0x3ff;
        if (exponent == 0x1f) {
            if (significand == 0) {
                return Float.intBitsToFloat(sign | 0x7f800000);
            }
            return Float.intBitsToFloat(sign | 0x7f800000 | (significand << 13));
        }
        if (exponent == 0) {
            if (significand == 0) {
                return Float.intBitsToFloat(sign);
            }
            // Subnormal half, normal float: shift the highest set bit up into the implicit
            // position and pay for it with the exponent.
            int highest = 31 - Integer.numberOfLeadingZeros(significand);
            int e = highest - 24;
            int mantissa = (significand << (23 - highest)) & 0x7fffff;
            return Float.intBitsToFloat(sign | ((e + 127) << 23) | mantissa);
        }
        return Float.intBitsToFloat(sign | ((exponent - 15 + 127) << 23) | (significand << 13));
    }

    /**
     * The {@link Class} of the primitive {@code float}.
     *
     * <p>Not the same thing as {@code Float.class}, and the difference trips people: that one is
     * the mirror of the WRAPPER, this one of the primitive. They compare unequal, and a
     * reflective lookup that wants one and gets the other simply finds nothing.
     */
    public static final Class<Float> TYPE = Class.getPrimitiveClass("float");

}
