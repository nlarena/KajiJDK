package java.lang;

// Calificar el tipo en el uso (`java.math.BigInteger`) no resuelve desde java.lang
// (finding #210); tiene que entrar por import y usarse por su nombre simple.
import java.math.BigInteger;

// KajiLibrary's java.lang.Double — the boxed-double wrapper (extends Number, implements
// Comparable). All numeric views except doubleValue are narrowing, so they need a cast.
public final class Double extends Number implements Comparable<Double>, ConstantDesc {

    private final double value;

    public Double(double value) {
        this.value = value;
    }

    public static Double valueOf(double d) {
        return new Double(d);
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return (long) value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return value;
    }

    public int compareTo(Double o) {
        return this.value < o.value ? -1 : (this.value > o.value ? 1 : 0);
    }


    public static String toString(double d) {
        return shortestDecimal(d, false);
    }

    public String toString() {
        return Double.toString(this.value);
    }

    /**
     * The shortest decimal that reads back as exactly this value.
     *
     * <p>Shortest and exact, both. The obvious way to write this -- scale the value by a power
     * of ten and round -- does the scaling in floating point, which is itself a rounding, so the
     * digits it produces are a guess about a value that was already a guess. This does the
     * scaling in arbitrary precision instead, and then VERIFIES each candidate by reading it
     * back: the first digit count whose decimal parses to the original bits wins. That makes the
     * property the method claims -- that printing and reading are inverses -- something it
     * checks rather than something it hopes.
     *
     * <p>{@code asFloat} does the whole thing at float width, for {@link Float#toString}.
     */
    static String shortestDecimal(double value, boolean asFloat) {
        if (value != value) {
            return "NaN";
        }
        if (value == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (value == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        boolean neg = value < 0.0d || (value == 0.0d && 1.0d / value < 0.0d);
        double d = value;
        if (neg) {
            d = -value;
        }
        if (d == 0.0d) {
            if (neg) {
                return "-0.0";
            }
            return "0.0";
        }
        // The exact value, as an integer significand times a power of two. Nothing is lost here
        // and nothing is lost afterwards, which is the whole point.
        long bits = Double.doubleToRawLongBits(d);
        long biasedExp = (bits >> 52) & 0x7ffL;
        long rawSignificand = bits & 0x000fffffffffffffL;
        BigInteger m;
        int e;
        if (biasedExp == 0L) {
            m = BigInteger.valueOf(rawSignificand);
            e = -1074;
        } else {
            m = BigInteger.valueOf(rawSignificand | 0x0010000000000000L);
            e = (int) biasedExp - 1075;
        }
        int maxDigits = 17;
        if (asFloat) {
            maxDigits = 9;
        }
        // The decimal place, computed EXACTLY once rather than estimated per candidate: the
        // number of digits before the point does not depend on how many we choose to print.
        int place = Double.exactPlace(m, e);
        int maxDigits = 17;
        if (asFloat) {
            maxDigits = 9;
        }
        // Binary search on the digit count. Valid because the property is monotone: if the
        // correctly-rounded k-digit decimal reads back as this value, the (k+1)-digit one is
        // closer still and reads back too. Five verifications instead of up to seventeen, and
        // each verification is a full parse, which is not cheap.
        int lo = 1;
        int hi = maxDigits;
        while (lo < hi) {
            int mid = (lo + hi) / 2;
            if (Double.roundTrips(m, e, place, mid, d, asFloat)) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        BigInteger chosen = Double.digitsAt(m, e, place, lo);
        String digits = chosen.toString();
        int leading = place;
        if (digits.length() > lo) {
            // Rounding carried: 999 became 1000, so the value gained a digit before the point.
            leading = place + 1;
            digits = Double.digitsAt(m, e, leading, lo).toString();
        }
        String body = assembleDecimal(digits, leading - 1);
        if (neg) {
            return "-" + body;
        }
        return body;
    }

    // How many digits `m * 2^e` has before the decimal point, as a first approximation. Taken
    // from the bit length because log10(2) is about 0.30103 and the estimate is only ever off by
    // one -- which the caller corrects by looking at how many digits actually came out.
    private static int decimalPlace(BigInteger m, int e) {
        int bits = m.bitLength() + e;
        int approx = (int) ((double) bits * 0.30103d);
        return approx + 1;
    }

    // Whether the correctly-rounded `k`-digit decimal of this value reads back as it.
    private static boolean roundTrips(BigInteger m, int e, int place, int k, double d,
            boolean asFloat) {
        BigInteger n = Double.digitsAt(m, e, place, k);
        String text = n.toString();
        int at = place;
        if (text.length() > k) {
            at = place + 1;
            text = Double.digitsAt(m, e, at, k).toString();
        }
        return Double.readsBackAs(assembleDecimal(text, at - 1), d, asFloat);
    }

    // How many digits `m * 2^e` has before the point: the `d` with 10^(d-1) <= value < 10^d.
    // Exact, because the estimate from the bit length is off by one often enough to matter --
    // the cast toward zero is not a floor once the value drops below 1.
    private static int exactPlace(BigInteger m, int e) {
        int d = Double.decimalPlace(m, e);
        while (Double.atLeast(m, e, d)) {
            d = d + 1;
        }
        while (!Double.atLeast(m, e, d - 1)) {
            d = d - 1;
        }
        return d;
    }

    // Whether `m * 2^e >= 10^p`, cross-multiplied so nothing is divided and nothing rounds.
    private static boolean atLeast(BigInteger m, int e, int p) {
        BigInteger lhs = m;
        BigInteger rhs = BigInteger.ONE;
        if (e > 0) {
            lhs = lhs.shiftLeft(e);
        } else if (e < 0) {
            rhs = rhs.shiftLeft(-e);
        }
        BigInteger ten = BigInteger.valueOf(10L);
        if (p > 0) {
            rhs = rhs.multiply(ten.pow(p));
        } else if (p < 0) {
            lhs = lhs.multiply(ten.pow(-p));
        }
        return lhs.compareTo(rhs) >= 0;
    }

    // The `k` most significant decimal digits of `m * 2^e`, as an integer, assuming the value
    // has `place` digits before the point. Exact: the scaling is a big-integer division and the
    // rounding is half-even on its remainder.
    private static BigInteger digitsAt(BigInteger m, int e, int place, int k) {
        BigInteger num = m;
        BigInteger den = BigInteger.ONE;
        if (e > 0) {
            num = num.shiftLeft(e);
        } else if (e < 0) {
            den = den.shiftLeft(-e);
        }
        BigInteger ten = BigInteger.valueOf(10L);
        int p = place - k;
        if (p > 0) {
            den = den.multiply(ten.pow(p));
        } else if (p < 0) {
            num = num.multiply(ten.pow(-p));
        }
        BigInteger q = num.divide(den);
        BigInteger product = q.multiply(den);
        BigInteger r = num.subtract(product);
        BigInteger twice = r.shiftLeft(1);
        int cmp = twice.compareTo(den);
        boolean up = cmp > 0;
        if (cmp == 0) {
            up = q.testBit(0);
        }
        if (up) {
            return q.add(BigInteger.ONE);
        }
        return q;
    }

    // Whether `candidate` reads back as exactly `d`, at the width being printed.
    private static boolean readsBackAs(String candidate, double d, boolean asFloat) {
        if (asFloat) {
            long got = Double.parseBits(candidate, 24, -149, 254, 23);
            long want = (long) Float.floatToRawIntBits((float) d) & 0xFFFFFFFFL;
            return got == want;
        }
        long got = Double.parseBits(candidate, 53, -1074, 2046, 52);
        return got == Double.doubleToRawLongBits(d);
    }

    private static long roundHalfEven(double x) {
        long fl = (long) x;
        double frac = x - (double) fl;
        if (frac > 0.5) {
            return fl + 1;
        }
        if (frac < 0.5) {
            return fl;
        }
        if ((fl & 1L) == 0) {
            return fl;
        }
        return fl + 1;
    }

    private static double pow10e(int n) {
        double r = 1.0;
        for (int i = 0; i < n; i = i + 1) {
            r = r * 10.0;
        }
        return r;
    }

    private static double tenPow(int n) {
        double r = 1.0;
        if (n >= 0) {
            for (int i = 0; i < n; i = i + 1) {
                r = r * 10.0;
            }
        } else {
            for (int i = 0; i < -n; i = i + 1) {
                r = r / 10.0;
            }
        }
        return r;
    }

    // r * 10^p, correctly rounded via a single multiply/divide by an exact power of ten when
    // |p| <= 22, else the (double-rounded) fallback.
    private static double reconstruct(long r, int p) {
        if (p >= 0) {
            if (p <= 22) {
                return (double) r * pow10e(p);
            }
            return (double) r * tenPow(p);
        }
        if (-p <= 22) {
            return (double) r / pow10e(-p);
        }
        return (double) r * tenPow(p);
    }

    // Lays out significant `digits` with the first digit at place 10^e in the JDK style:
    // plain decimal for e in [-3, 6], scientific (d.dddEexp) otherwise; always one fractional
    // digit.
    private static String assembleDecimal(String digits, int e) {
        int len = digits.length();
        if (e >= -3 && e < 7) {
            if (e >= 0) {
                if (len <= e + 1) {
                    String ip = digits;
                    for (int i = len; i < e + 1; i = i + 1) {
                        ip = ip + "0";
                    }
                    return ip + ".0";
                }
                return digits.substring(0, e + 1) + "." + digits.substring(e + 1, len);
            }
            String zeros = "";
            for (int i = 0; i < -e - 1; i = i + 1) {
                zeros = zeros + "0";
            }
            return "0." + zeros + digits;
        }
        String mant;
        if (len > 1) {
            mant = digits.charAt(0) + "." + digits.substring(1, len);
        } else {
            mant = digits.charAt(0) + ".0";
        }
        return mant + "E" + Integer.toString(e);
    }

    // ---- the extremes, as bit patterns rather than literals ----
    //
    // Written through `longBitsToDouble` and not as decimal literals, which is not pedantry:
    // MAX_VALUE has 17 significant digits and MIN_VALUE is 10^-324, and a literal for either one
    // is a decimal string that the compiler has to parse back into exactly the bits meant. The
    // bit pattern says what it is with no rounding in between.

    /** The largest finite value. */
    public static final double MAX_VALUE = Double.longBitsToDouble(0x7fefffffffffffffL);

    /** The smallest positive value, which is subnormal. */
    public static final double MIN_VALUE = Double.longBitsToDouble(0x1L);

    /** The smallest positive value with a full significand. */
    public static final double MIN_NORMAL = Double.longBitsToDouble(0x0010000000000000L);

    /** Positive infinity. */
    public static final double POSITIVE_INFINITY = Double.longBitsToDouble(0x7ff0000000000000L);

    /** Negative infinity. */
    public static final double NEGATIVE_INFINITY = Double.longBitsToDouble(0xfff0000000000000L);

    /** Not-a-number. Note that {@code Double.NaN == Double.NaN} is false. */
    public static final double NaN = Double.longBitsToDouble(0x7ff8000000000000L);

    /** The largest exponent a finite value may have. */
    public static final int MAX_EXPONENT = 1023;

    /** The smallest exponent a normal value may have. */
    public static final int MIN_EXPONENT = -1022;

    /** Bits in a double. */
    public static final int SIZE = 64;

    /** Bytes in a double. */
    public static final int BYTES = 8;

    /** Bits of precision in the significand, counting the implicit leading one. */
    public static final int PRECISION = 53;

    // ---- the bits ----

    /**
     * The IEEE-754 bits of {@code value}, with every NaN collapsed to one pattern.
     *
     * <p>The collapsing is the whole difference from {@link #doubleToRawLongBits}: IEEE-754 has
     * millions of NaN patterns, and two of them mean the same thing, so a method meant for
     * comparing values must not tell them apart.
     *
     * @param value the value to read
     */
    public static long doubleToLongBits(double value) {
        if (value != value) {
            return 0x7ff8000000000000L;
        }
        return Double.doubleToRawLongBits(value);
    }

    /**
     * The IEEE-754 bits of {@code value}, exactly as they are.
     *
     * <p>Native because no bytecode can do it: every conversion opcode converts the VALUE, and
     * this reinterprets the storage.
     *
     * @param value the value to read
     */
    public static native long doubleToRawLongBits(double value);

    /**
     * The double those IEEE-754 bits describe.
     *
     * @param bits the bit pattern
     */
    public static native double longBitsToDouble(long bits);

    // ---- classification ----

    /**
     * Whether {@code v} is not-a-number.
     *
     * @param v the value to test
     */
    public static boolean isNaN(double v) {
        // The definition, not a trick: NaN is the only value that is not equal to itself.
        return v != v;
    }

    /** Whether this value is not-a-number. */
    public boolean isNaN() {
        return Double.isNaN(this.value);
    }

    /**
     * Whether {@code v} is an infinity.
     *
     * @param v the value to test
     */
    public static boolean isInfinite(double v) {
        return v == Double.POSITIVE_INFINITY || v == Double.NEGATIVE_INFINITY;
    }

    /** Whether this value is an infinity. */
    public boolean isInfinite() {
        return Double.isInfinite(this.value);
    }

    /**
     * Whether {@code d} is neither infinite nor NaN.
     *
     * @param d the value to test
     */
    public static boolean isFinite(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d);
    }

    // ---- ordering and identity ----

    /**
     * A total order over doubles, which {@code <} is not.
     *
     * <p>Two places where it differs, and both are why this method exists: {@code -0.0} sorts
     * below {@code +0.0} though they compare equal, and NaN sorts above everything though it
     * compares false against everything. Without that, a sorted collection of doubles has no
     * defined shape.
     *
     * @param d1 one value
     * @param d2 the other
     */
    public static int compare(double d1, double d2) {
        if (d1 < d2) {
            return -1;
        }
        if (d1 > d2) {
            return 1;
        }
        // Equal, unordered, or differing only in the sign of a zero: fall through to the bits,
        // which order correctly because the sign bit is the top one.
        long b1 = Double.doubleToLongBits(d1);
        long b2 = Double.doubleToLongBits(d2);
        if (b1 == b2) {
            return 0;
        }
        if (b1 < b2) {
            return -1;
        }
        return 1;
    }

    /**
     * Equal when {@code other} is a Double with the same BITS.
     *
     * <p>Which makes {@code new Double(Double.NaN).equals(new Double(Double.NaN))} true and
     * {@code new Double(0.0).equals(new Double(-0.0))} false -- both the opposite of what
     * {@code ==} says about the values inside. That is deliberate in the JDK: a collection needs
     * equality to be reflexive, and {@code ==} on NaN is not.
     *
     * @param other the object to compare against
     */
    public boolean equals(Object other) {
        if (!(other instanceof Double)) {
            return false;
        }
        Double that = (Double) other;
        return Double.doubleToLongBits(this.value) == Double.doubleToLongBits(that.doubleValue());
    }

    /** A hash consistent with {@link #equals}, folded from the bits. */
    public int hashCode() {
        return Double.hashCode(this.value);
    }

    /**
     * The hash of a double, without boxing it.
     *
     * @param value the value to hash
     */
    public static int hashCode(double value) {
        long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));
    }

    // ---- the arithmetic a reduction needs ----

    /**
     * The sum, as a method so it can be passed as a function.
     *
     * @param a one addend
     * @param b the other
     */
    public static double sum(double a, double b) {
        return a + b;
    }

    /**
     * The greater of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    /**
     * The smaller of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    // ---- as a hexadecimal float ----

    /**
     * {@code d} in the hexadecimal form that round-trips exactly.
     *
     * <p>The point of the format is that it is lossless: a decimal string for a double is a
     * rounding, and this is not -- the significand goes out in hex, four bits per digit, with
     * the binary exponent after a {@code p}.
     *
     * @param d the value to render
     */
    public static String toHexString(double d) {
        if (Double.isNaN(d)) {
            return "NaN";
        }
        if (d == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (d == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        long bits = Double.doubleToRawLongBits(d);
        String sign = "";
        if (bits < 0L) {
            sign = "-";
        }
        long exponent = (bits >> 52) & 0x7ffL;
        long significand = bits & 0x000fffffffffffffL;
        if (exponent == 0L && significand == 0L) {
            return sign + "0x0.0p0";
        }
        if (exponent == 0L) {
            // Subnormal: no implicit leading one, and the exponent is pinned at the minimum.
            return sign + "0x0." + Double.hexSignificand(significand) + "p-1022";
        }
        long unbiased = exponent - 1023L;
        return sign + "0x1." + Double.hexSignificand(significand) + "p" + Long.toString(unbiased);
    }

    // The 52 significand bits as 13 hex digits, with trailing zeros trimmed -- but never to
    // nothing, because the format always shows at least one fractional digit.
    private static String hexSignificand(long significand) {
        String digits = "";
        int shift = 48;
        while (shift >= 0) {
            int nibble = (int) ((significand >> shift) & 0xfL);
            digits = digits + Double.hexDigit(nibble);
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


    // ---- reading a double back out of text ----
    //
    // The hard direction. Printing a double is a choice among strings that all mean the same
    // value; reading one is a claim that a particular decimal has a particular nearest double,
    // and there is exactly one right answer. Getting it "close" means a value that round-trips
    // wrong, and a program whose numbers change every time they pass through a file.
    //
    // So this does not estimate. It builds the exact rational the text denotes -- an integer
    // times a power of ten -- and divides it out in arbitrary precision, keeping the remainder
    // to round half-even on. `BigInteger` is what makes that affordable to write.

    /**
     * The double nearest to what {@code s} says, rounding half to even.
     *
     * <p>Accepts what the JDK accepts: optional surrounding whitespace, an optional sign, a
     * decimal or hexadecimal significand, an optional exponent, an optional {@code f}/{@code d}
     * suffix, and the three names {@code NaN}, {@code Infinity} and {@code -Infinity}.
     *
     * @param s the text to read
     * @return the nearest double
     * @throws NumberFormatException if the text does not denote a number
     * @throws NullPointerException if {@code s} is null
     */
    public static double parseDouble(String s) {
        long bits = Double.parseBits(s, 53, -1074, 2046, 52);
        return Double.longBitsToDouble(bits);
    }

    // The shared reader, in terms of the target format rather than of `double`.
    //
    // `precision` is the significand width counting the implicit one, `minExp` the exponent of
    // the smallest subnormal, `maxBiased` the largest finite biased exponent and `shift` the
    // position of the exponent field. Parameterising it is not generality for its own sake: a
    // float CANNOT be read by rounding to a double first and narrowing, because that rounds
    // twice and the two roundings can disagree with one.
    static long parseBits(String s, int precision, int minExp, int maxBiased, int shift) {
        if (s == null) {
            throw new NullPointerException();
        }
        String t = s.trim();
        int n = t.length();
        if (n == 0) {
            throw new NumberFormatException("empty String");
        }
        int at = 0;
        boolean negative = false;
        char first = t.charAt(0);
        if (first == '+' || first == '-') {
            negative = first == '-';
            at = 1;
        }
        String body = t.substring(at, n);
        // The three names, which are not numerals at all.
        if (body.equals("NaN")) {
            return Double.namedBits(false, true, precision, maxBiased, shift);
        }
        if (body.equals("Infinity")) {
            return Double.namedBits(negative, false, precision, maxBiased, shift);
        }
        // A trailing type suffix is allowed and means nothing here.
        int end = body.length();
        if (end > 0) {
            char last = body.charAt(end - 1);
            if (last == 'f' || last == 'F' || last == 'd' || last == 'D') {
                end = end - 1;
            }
        }
        String digits = body.substring(0, end);
        if (digits.length() == 0) {
            throw new NumberFormatException("For input string: " + s);
        }
        if (Double.isHexPrefix(digits)) {
            return Double.parseHexBits(negative, digits, precision, minExp, maxBiased, shift);
        }
        return Double.parseDecBits(negative, digits, s, precision, minExp, maxBiased, shift);
    }

    private static boolean isHexPrefix(String d) {
        if (d.length() < 2) {
            return false;
        }
        char c1 = d.charAt(1);
        return d.charAt(0) == '0' && (c1 == 'x' || c1 == 'X');
    }

    // NaN or an infinity, as the bit pattern of the target format.
    private static long namedBits(boolean negative, boolean nan, int precision, int maxBiased,
            int shift) {
        long exponentField = (long) (maxBiased + 1) << shift;
        long payload = 0L;
        if (nan) {
            // The canonical quiet NaN: the top significand bit set, nothing else.
            payload = 1L << (shift - 1);
            return exponentField | payload;
        }
        long signBit = 0L;
        if (negative) {
            signBit = 1L << (shift + (63 - shift));
        }
        if (precision == 24) {
            signBit = 0L;
            if (negative) {
                signBit = 1L << 31;
            }
        }
        return signBit | exponentField;
    }

    // The decimal form. Splits the text into an integer of digits and a power of ten, and hands
    // both to the exact rounder.
    private static long parseDecBits(boolean negative, String d, String original, int precision,
            int minExp, int maxBiased, int shift) {
        String mantissa = "";
        int pointExp = 0;
        int expPart = 0;
        int i = 0;
        int len = d.length();
        boolean seenDigit = false;
        boolean seenPoint = false;
        while (i < len) {
            char c = d.charAt(i);
            if (c >= '0' && c <= '9') {
                mantissa = mantissa + c;
                if (seenPoint) {
                    pointExp = pointExp - 1;
                }
                seenDigit = true;
                i = i + 1;
                continue;
            }
            if (c == '.' && !seenPoint) {
                seenPoint = true;
                i = i + 1;
                continue;
            }
            if ((c == 'e' || c == 'E') && seenDigit) {
                expPart = Double.readExponent(d, i + 1, original);
                i = len;
                break;
            }
            throw new NumberFormatException("For input string: " + original);
        }
        if (!seenDigit) {
            throw new NumberFormatException("For input string: " + original);
        }
        return Double.roundExact(negative, mantissa, pointExp + expPart, precision, minExp,
                maxBiased, shift);
    }

    private static int readExponent(String d, int from, String original) {
        int len = d.length();
        if (from >= len) {
            throw new NumberFormatException("For input string: " + original);
        }
        int i = from;
        boolean negative = false;
        char c0 = d.charAt(i);
        if (c0 == '+' || c0 == '-') {
            negative = c0 == '-';
            i = i + 1;
        }
        if (i >= len) {
            throw new NumberFormatException("For input string: " + original);
        }
        long value = 0L;
        while (i < len) {
            char c = d.charAt(i);
            if (c < '0' || c > '9') {
                throw new NumberFormatException("For input string: " + original);
            }
            value = value * 10L + (long) (c - '0');
            // Clamped rather than overflowed: an exponent past this is decided by its sign
            // alone, and letting it wrap would turn an overflow into a small number.
            if (value > 1000000L) {
                value = 1000000L;
            }
            i = i + 1;
        }
        if (negative) {
            return (int) (-value);
        }
        return (int) value;
    }

    // The hexadecimal form, which is exact by construction: the significand is already binary,
    // so there is nothing to convert -- only to place.
    private static long parseHexBits(boolean negative, String d, int precision, int minExp,
            int maxBiased, int shift) {
        int len = d.length();
        int i = 2;
        BigInteger value = BigInteger.ZERO;
        BigInteger sixteen = BigInteger.valueOf(16L);
        int binExp = 0;
        boolean seenPoint = false;
        boolean seenDigit = false;
        while (i < len) {
            char c = d.charAt(i);
            int nibble = Double.hexValue(c);
            if (nibble >= 0) {
                BigInteger scaled = value.multiply(sixteen);
                value = scaled.add(BigInteger.valueOf((long) nibble));
                if (seenPoint) {
                    binExp = binExp - 4;
                }
                seenDigit = true;
                i = i + 1;
                continue;
            }
            if (c == '.' && !seenPoint) {
                seenPoint = true;
                i = i + 1;
                continue;
            }
            if (c == 'p' || c == 'P') {
                binExp = binExp + Double.readExponent(d, i + 1, d);
                i = len;
                break;
            }
            throw new NumberFormatException("For input string: " + d);
        }
        if (!seenDigit) {
            throw new NumberFormatException("For input string: " + d);
        }
        return Double.assemble(negative, value, binExp, precision, minExp, maxBiased, shift);
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    // digits * 10^k, rounded to the target format. The decimal power becomes an exact rational:
    // a positive k multiplies the numerator, a negative one the denominator. Nothing is
    // approximated before the single rounding at the end.
    private static long roundExact(boolean negative, String digits, int k, int precision,
            int minExp, int maxBiased, int shift) {
        BigInteger value = new BigInteger(digits);
        if (value.signum() == 0) {
            return Double.zeroBits(negative, precision, shift);
        }
        BigInteger num = value;
        BigInteger den = BigInteger.ONE;
        BigInteger ten = BigInteger.valueOf(10L);
        if (k > 0) {
            num = num.multiply(ten.pow(k));
        } else if (k < 0) {
            den = ten.pow(-k);
        }
        return Double.divideRound(negative, num, den, precision, minExp, maxBiased, shift);
    }

    // An exact binary significand times 2^binExp -- the hexadecimal path, which needs no
    // decimal power at all.
    private static long assemble(boolean negative, BigInteger value, int binExp,
            int precision, int minExp, int maxBiased, int shift) {
        if (value.signum() == 0) {
            return Double.zeroBits(negative, precision, shift);
        }
        BigInteger num = value;
        BigInteger den = BigInteger.ONE;
        if (binExp > 0) {
            num = num.shiftLeft(binExp);
        } else if (binExp < 0) {
            den = den.shiftLeft(-binExp);
        }
        return Double.divideRound(negative, num, den, precision, minExp, maxBiased, shift);
    }

    /**
     * The heart of it: the correctly rounded bit pattern of {@code num / den}.
     *
     * <p>Finds the binary exponent that puts the quotient in {@code [2^(p-1), 2^p)}, divides
     * there, and rounds on the remainder -- half to even, which is the only tie rule that does
     * not drift. Subnormals are handled by pinning the exponent at the format minimum instead of
     * normalising, which is exactly what makes them lose precision rather than range.
     */
    private static long divideRound(boolean negative, BigInteger num,
            BigInteger den, int precision, int minExp, int maxBiased, int shift) {
        // A first estimate of the exponent from the sizes; it is off by at most one, and the
        // loop below fixes that rather than trusting it.
        int e = num.bitLength() - den.bitLength() - precision;
        if (e < minExp) {
            e = minExp;
        }
        BigInteger q = BigInteger.ZERO;
        BigInteger r = BigInteger.ZERO;
        BigInteger scaledDen = den;
        while (true) {
            BigInteger topNum = num;
            scaledDen = den;
            if (e > 0) {
                scaledDen = den.shiftLeft(e);
            } else if (e < 0) {
                topNum = num.shiftLeft(-e);
            }
            q = topNum.divide(scaledDen);
            BigInteger product = q.multiply(scaledDen);
            r = topNum.subtract(product);
            if (q.bitLength() > precision && e < 1024) {
                e = e + 1;
                continue;
            }
            if (q.bitLength() < precision && e > minExp) {
                e = e - 1;
                continue;
            }
            break;
        }
        // Round half to even on the remainder: twice the remainder against the divisor.
        BigInteger twice = r.shiftLeft(1);
        int cmp = twice.compareTo(scaledDen);
        boolean roundUp = cmp > 0;
        if (cmp == 0) {
            roundUp = q.testBit(0);
        }
        if (roundUp) {
            q = q.add(BigInteger.ONE);
            if (q.bitLength() > precision) {
                // Carried out of the significand: shift it back and take the exponent up.
                q = q.shiftRight(1);
                e = e + 1;
            }
        }
        return Double.pack(negative, q, e, precision, minExp, maxBiased, shift);
    }

    // Lays the significand and exponent into the format's bits, turning the two out-of-range
    // cases into what IEEE-754 says they are: an infinity above, a subnormal or zero below.
    private static long pack(boolean negative, BigInteger q, int e, int precision,
            int minExp, int maxBiased, int shift) {
        long signBit = 0L;
        if (negative) {
            if (precision == 24) {
                signBit = 1L << 31;
            } else {
                signBit = 1L << 63;
            }
        }
        if (q.signum() == 0) {
            return signBit;
        }
        int biased = e + (precision - 1) + maxBiased / 2;
        if (biased > maxBiased) {
            return signBit | ((long) (maxBiased + 1) << shift);
        }
        long mantissa = q.longValue();
        // Subnormal exactly when the significand never reached full width -- NOT when the
        // computed exponent field looks small. The two are almost the same and differ in the one
        // case that matters: a subnormal that rounds up to full width becomes the smallest
        // NORMAL number, whose exponent field is 1, and pinning on the field would encode it as
        // a subnormal with the leading bit spilled into the exponent.
        if (q.bitLength() < precision) {
            return signBit | mantissa;
        }
        long implicitOff = mantissa & ((1L << shift) - 1L);
        return signBit | ((long) biased << shift) | implicitOff;
    }

    private static long zeroBits(boolean negative, int precision, int shift) {
        if (!negative) {
            return 0L;
        }
        if (precision == 24) {
            return 1L << 31;
        }
        return 1L << 63;
    }

    /**
     * The Double nearest to what {@code s} says.
     *
     * @param s the text to read
     * @throws NumberFormatException if the text does not denote a number
     */
    public static Double valueOf(String s) {
        return Double.valueOf(Double.parseDouble(s));
    }

    /**
     * A Double holding what {@code s} says.
     *
     * @param s the text to read
     * @throws NumberFormatException if the text does not denote a number
     * @deprecated the JDK deprecates every wrapper constructor; use {@link #valueOf(String)},
     *             which may share instances.
     */
    @Deprecated(since = "9")
    public Double(String s) {
        this(Double.parseDouble(s));
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
    public Double resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup lookup) {
        return Double.valueOf(this.doubleValue());
    }


    /** This value as a byte, truncated. */
    public byte byteValue() {
        return (byte) this.doubleValue();
    }

    /** This value as a short, truncated. */
    public short shortValue() {
        return (short) this.doubleValue();
    }

    /** This value as a nominal descriptor, which is always present. */
    public Optional<Double> describeConstable() {
        return Optional.of(Double.valueOf(this.doubleValue()));
    }

    /**
     * The {@link Class} of the primitive {@code double}.
     *
     * <p>Not the same thing as {@code Double.class}, and the difference trips people: that one is
     * the mirror of the WRAPPER, this one of the primitive. They compare unequal, and a
     * reflective lookup that wants one and gets the other simply finds nothing.
     */
    public static final Class<Double> TYPE = Class.getPrimitiveClass("double");

}
