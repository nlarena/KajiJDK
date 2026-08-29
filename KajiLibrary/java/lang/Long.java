package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde
// java.lang (finding #210).
import java.lang.constant.ConstantDesc;
import java.util.Optional;

// KajiLibrary's java.lang.Long — the boxed-long wrapper. Same shape as Integer: extends
// Number, implements Comparable, provides valueOf/longValue for boxing/unboxing.
public final class Long extends Number implements Comparable<Long>, ConstantDesc {

    public static final long MIN_VALUE = 0x8000000000000000L;

    public static final long MAX_VALUE = 0x7fffffffffffffffL;

    private final long value;

    public Long(long value) {
        this.value = value;
    }

    // Las instancias compartidas de -128..127. Viven en una clase anidada para que se construyan
    // en el primer `valueOf` y no en la primera mencion de la clase.
    //
    // No es una optimizacion: **JLS 5.1.7 exige que boxear un valor de ese rango devuelva la
    // MISMA referencia**, asi que `Long.valueOf(100L) == Long.valueOf(100L)` es una promesa del lenguaje. Sin la cache
    // la promesa se rompe en silencio -- el codigo sigue andando hasta que alguien compara con
    // `==`, que es justo lo que la cache existe para permitir.
    private static final class LongCache {

        static final Long[] CACHE = LongCache.fill();

        private static Long[] fill() {
            Long[] out = new Long[256];
            int i = 0;
            while (i < 256) {
                out[i] = new Long((long) (i + (-128)));
                i = i + 1;
            }
            return out;
        }
    }

    public static Long valueOf(long l) {
        if (l >= -128L && l <= 127L) {
            return LongCache.CACHE[(int) l + 128];
        }
        return new Long(l);
    }

    public int intValue() {
        return (int) value;
    }

    public long longValue() {
        return value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return (double) value;
    }

    public int compareTo(Long o) {
        return this.value < o.value ? -1 : (this.value == o.value ? 0 : 1);
    }

    // The signed decimal string for `i`. Magnitude is accumulated in negative space so
    // Long.MIN_VALUE (which can't be negated) is handled.
    public static String toString(long i) {
        if (i == 0) {
            return "0";
        }
        boolean neg = i < 0;
        long x = i;
        if (x > 0) {
            x = -x;
        }
        char[] buf = new char[20];
        int pos = 20;
        while (x != 0) {
            int d = (int) (-(x % 10));
            pos = pos - 1;
            buf[pos] = (char) ('0' + d);
            x = x / 10;
        }
        if (neg) {
            pos = pos - 1;
            buf[pos] = '-';
        }
        return String.valueOf(buf, pos, 20 - pos);
    }

    public String toString() {
        return Long.toString(this.value);
    }

    /** Bits in a long. */
    public static final int SIZE = 64;

    /** Bytes in a long. */
    public static final int BYTES = 8;

    // ---- text out ----

    /**
     * {@code i} written in {@code radix}, signed.
     *
     * @param i the value
     * @param radix the base; anything outside 2..36 is taken as 10
     */
    public static String toString(long i, int radix) {
        int base = radix;
        if (base < Character.MIN_RADIX || base > Character.MAX_RADIX) {
            base = 10;
        }
        if (base == 10) {
            return Long.toString(i);
        }
        if (i == 0L) {
            return "0";
        }
        // Negative accumulation, for the same reason as in Integer: MIN_VALUE has no positive
        // counterpart, so the magnitude cannot be built.
        boolean negative = i < 0L;
        long rest = i;
        if (!negative) {
            rest = -i;
        }
        String digits = "";
        while (rest != 0L) {
            int digit = (int) (-(rest % (long) base));
            rest = rest / (long) base;
            digits = Character.forDigit(digit, base) + digits;
        }
        if (negative) {
            return "-" + digits;
        }
        return digits;
    }

    /**
     * {@code i} written in {@code radix}, read as unsigned.
     *
     * <p>Cannot borrow a wider type the way {@code Integer} does -- there is nothing wider -- so
     * the first division is done by halving first: {@code (i >>> 1) / (radix / 2)} is exact for
     * an even radix, and the remainder it leaves is small enough to finish in signed arithmetic.
     *
     * @param i the bits to read
     * @param radix the base; anything outside 2..36 is taken as 10
     */
    public static String toUnsignedString(long i, int radix) {
        int base = radix;
        if (base < Character.MIN_RADIX || base > Character.MAX_RADIX) {
            base = 10;
        }
        if (i >= 0L) {
            return Long.toString(i, base);
        }
        // The top bit is set, so the signed reading is negative. One unsigned division by hand
        // brings it back into the range where signed arithmetic agrees with unsigned.
        long quotient = ((i >>> 1) / (long) base) << 1;
        long remainder = i - quotient * (long) base;
        if (remainder >= (long) base) {
            quotient = quotient + remainder / (long) base;
            remainder = remainder % (long) base;
        }
        return Long.toString(quotient, base) + Character.forDigit((int) remainder, base);
    }

    /**
     * {@code i} written in base ten, read as unsigned.
     *
     * @param i the bits to read
     */
    public static String toUnsignedString(long i) {
        return Long.toUnsignedString(i, 10);
    }

    /**
     * {@code i} in hexadecimal, read as unsigned and without leading zeros.
     *
     * @param i the bits to read
     */
    public static String toHexString(long i) {
        return Long.toUnsignedString(i, 16);
    }

    /**
     * {@code i} in binary, read as unsigned and without leading zeros.
     *
     * @param i the bits to read
     */
    public static String toBinaryString(long i) {
        return Long.toUnsignedString(i, 2);
    }

    /**
     * {@code i} in octal, read as unsigned and without leading zeros.
     *
     * @param i the bits to read
     */
    public static String toOctalString(long i) {
        return Long.toUnsignedString(i, 8);
    }

    // ---- text in ----

    /**
     * The long {@code s} denotes in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable long
     */
    public static long parseLong(String s) {
        return Long.parseLong(s, 10);
    }

    /**
     * The long {@code s} denotes in {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not parsable
     */
    public static long parseLong(String s, int radix) {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string");
        }
        return Long.parseLong(s, 0, s.length(), radix);
    }

    /**
     * The long denoted by {@code [beginIndex, endIndex)} of {@code s}, in {@code radix}.
     *
     * @param s the text
     * @param beginIndex where to start
     * @param endIndex where to stop
     * @param radix the base
     * @throws NumberFormatException if it is not parsable
     * @throws IndexOutOfBoundsException if the range is not within {@code s}
     */
    public static long parseLong(CharSequence s, int beginIndex, int endIndex, int radix) {
        if (s == null) {
            throw new NullPointerException();
        }
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix + " less than Character.MIN_RADIX");
        }
        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + " greater than Character.MAX_RADIX");
        }
        if (beginIndex == endIndex) {
            throw new NumberFormatException("Empty input");
        }
        int i = beginIndex;
        boolean negative = false;
        long limit = -Long.MAX_VALUE;
        char first = s.charAt(i);
        if (first == '-') {
            negative = true;
            limit = Long.MIN_VALUE;
            i = i + 1;
        } else if (first == '+') {
            i = i + 1;
        }
        if (i == endIndex) {
            throw new NumberFormatException("No digits");
        }
        long multmin = limit / (long) radix;
        long result = 0L;
        while (i < endIndex) {
            int digit = Character.digit(s.charAt(i), radix);
            if (digit < 0) {
                throw new NumberFormatException("Not a digit at " + i);
            }
            if (result < multmin) {
                throw new NumberFormatException("Value out of range");
            }
            result = result * (long) radix;
            if (result < limit + (long) digit) {
                throw new NumberFormatException("Value out of range");
            }
            result = result - (long) digit;
            i = i + 1;
        }
        if (negative) {
            return result;
        }
        return -result;
    }

    /**
     * The unsigned long {@code s} denotes in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not parsable
     */
    public static long parseUnsignedLong(String s) {
        return Long.parseUnsignedLong(s, 10);
    }

    /**
     * The unsigned long {@code s} denotes in {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not parsable
     */
    public static long parseUnsignedLong(String s, int radix) {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string");
        }
        return Long.parseUnsignedLong(s, 0, s.length(), radix);
    }

    /**
     * The unsigned long denoted by {@code [beginIndex, endIndex)} of {@code s}.
     *
     * <p>There is no wider type to lean on, so the check is done by hand: accumulate and refuse
     * the step that would carry past 2^64.
     *
     * @param s the text
     * @param beginIndex where to start
     * @param endIndex where to stop
     * @param radix the base
     * @throws NumberFormatException if it is not parsable
     */
    public static long parseUnsignedLong(CharSequence s, int beginIndex, int endIndex,
            int radix) {
        if (s == null) {
            throw new NullPointerException();
        }
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + " out of range");
        }
        if (beginIndex == endIndex) {
            throw new NumberFormatException("Empty input");
        }
        int i = beginIndex;
        char first = s.charAt(i);
        if (first == '-') {
            throw new NumberFormatException("Illegal leading minus sign");
        }
        if (first == '+') {
            i = i + 1;
        }
        if (i == endIndex) {
            throw new NumberFormatException("No digits");
        }
        long result = 0L;
        while (i < endIndex) {
            int digit = Character.digit(s.charAt(i), radix);
            if (digit < 0) {
                throw new NumberFormatException("Not a digit at " + i);
            }
            long next = result * (long) radix;
            // Overflow past 2^64 shows up as the unsigned quotient no longer matching.
            if (Long.compareUnsigned(result, Long.divideUnsigned(-1L, (long) radix)) > 0) {
                throw new NumberFormatException("Value out of range");
            }
            long sum = next + (long) digit;
            if (Long.compareUnsigned(sum, next) < 0) {
                throw new NumberFormatException("Value out of range");
            }
            result = sum;
            i = i + 1;
        }
        return result;
    }

    /**
     * The Long {@code s} denotes in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not parsable
     */
    public static Long valueOf(String s) {
        return Long.valueOf(Long.parseLong(s, 10));
    }

    /**
     * The Long {@code s} denotes in {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not parsable
     */
    public static Long valueOf(String s, int radix) {
        return Long.valueOf(Long.parseLong(s, radix));
    }

    /**
     * The Long {@code nm} denotes, with the base taken from its prefix.
     *
     * @param nm the text
     * @throws NumberFormatException if it is not parsable
     * @see Integer#decode(String) for the prefix rules
     */
    public static Long decode(String nm) {
        if (nm == null) {
            throw new NullPointerException();
        }
        int len = nm.length();
        if (len == 0) {
            throw new NumberFormatException("Zero length string");
        }
        int at = 0;
        String sign = "";
        char first = nm.charAt(0);
        if (first == '-' || first == '+') {
            if (first == '-') {
                sign = "-";
            }
            at = 1;
        }
        int radix = 10;
        if (nm.startsWith("0x", at) || nm.startsWith("0X", at)) {
            at = at + 2;
            radix = 16;
        } else if (nm.startsWith("#", at)) {
            at = at + 1;
            radix = 16;
        } else if (nm.startsWith("0", at) && len > at + 1) {
            at = at + 1;
            radix = 8;
        }
        if (at >= len) {
            throw new NumberFormatException("No digits");
        }
        String digits = sign + nm.substring(at, len);
        return Long.valueOf(Long.parseLong(digits, radix));
    }

    // ---- reading the same bits as unsigned ----

    /**
     * Compares two longs as unsigned.
     *
     * @param x one value
     * @param y the other
     */
    public static int compareUnsigned(long x, long y) {
        return Long.compare(x + Long.MIN_VALUE, y + Long.MIN_VALUE);
    }

    /**
     * {@code dividend / divisor} with both read as unsigned.
     *
     * @param dividend the dividend
     * @param divisor the divisor
     * @throws ArithmeticException if the divisor is zero
     */
    public static long divideUnsigned(long dividend, long divisor) {
        if (divisor < 0L) {
            // The divisor has its top bit set, so it is larger than half the range: the quotient
            // can only be 0 or 1.
            if (Long.compareUnsigned(dividend, divisor) < 0) {
                return 0L;
            }
            return 1L;
        }
        if (dividend >= 0L) {
            return dividend / divisor;
        }
        // Halve, divide, double -- then fix the one step that halving may have lost.
        long quotient = ((dividend >>> 1) / divisor) << 1;
        long remainder = dividend - quotient * divisor;
        if (Long.compareUnsigned(remainder, divisor) >= 0) {
            return quotient + 1L;
        }
        return quotient;
    }

    /**
     * The remainder of {@link #divideUnsigned}.
     *
     * @param dividend the dividend
     * @param divisor the divisor
     * @throws ArithmeticException if the divisor is zero
     */
    public static long remainderUnsigned(long dividend, long divisor) {
        long quotient = Long.divideUnsigned(dividend, divisor);
        return dividend - quotient * divisor;
    }

    // ---- the bit operations ----

    /**
     * How many bits of {@code i} are set.
     *
     * @param i the value
     */
    public static int bitCount(long i) {
        int low = Integer.bitCount((int) i);
        int high = Integer.bitCount((int) (i >>> 32));
        return low + high;
    }

    /**
     * How many zero bits precede the highest set bit.
     *
     * @param i the value; zero answers 64
     */
    public static int numberOfLeadingZeros(long i) {
        int high = (int) (i >>> 32);
        if (high == 0) {
            return 32 + Integer.numberOfLeadingZeros((int) i);
        }
        return Integer.numberOfLeadingZeros(high);
    }

    /**
     * How many zero bits follow the lowest set bit.
     *
     * @param i the value; zero answers 64
     */
    public static int numberOfTrailingZeros(long i) {
        if (i == 0L) {
            return 64;
        }
        return 63 - Long.numberOfLeadingZeros(Long.lowestOneBit(i));
    }

    /**
     * A long with only the highest set bit of {@code i} kept.
     *
     * @param i the value
     */
    public static long highestOneBit(long i) {
        if (i == 0L) {
            return 0L;
        }
        return 1L << (63 - Long.numberOfLeadingZeros(i));
    }

    /**
     * A long with only the lowest set bit of {@code i} kept.
     *
     * @param i the value
     */
    public static long lowestOneBit(long i) {
        return i & -i;
    }

    /**
     * {@code i} with its bits in the opposite order.
     *
     * @param i the value
     */
    public static long reverse(long i) {
        long result = 0L;
        int bit = 0;
        while (bit < 64) {
            result = result | (((i >>> bit) & 1L) << (63 - bit));
            bit = bit + 1;
        }
        return result;
    }

    /**
     * {@code i} with its eight bytes in the opposite order.
     *
     * @param i the value
     */
    public static long reverseBytes(long i) {
        long result = 0L;
        int b = 0;
        while (b < 8) {
            long octet = (i >>> (b * 8)) & 0xFFL;
            result = result | (octet << ((7 - b) * 8));
            b = b + 1;
        }
        return result;
    }

    /**
     * {@code i} rotated left.
     *
     * @param i the value
     * @param distance how far; only the low six bits matter
     */
    public static long rotateLeft(long i, int distance) {
        return (i << distance) | (i >>> -distance);
    }

    /**
     * {@code i} rotated right.
     *
     * @param i the value
     * @param distance how far; only the low six bits matter
     */
    public static long rotateRight(long i, int distance) {
        return (i >>> distance) | (i << -distance);
    }

    /**
     * The sign: -1, 0 or 1.
     *
     * @param i the value
     */
    public static int signum(long i) {
        if (i < 0L) {
            return -1;
        }
        if (i > 0L) {
            return 1;
        }
        return 0;
    }

    /**
     * The bits of {@code i} at the positions {@code mask} selects, packed into the low end.
     *
     * @param i the value to gather from
     * @param mask which positions to take
     */
    public static long compress(long i, long mask) {
        long result = 0L;
        int out = 0;
        int bit = 0;
        while (bit < 64) {
            if (((mask >>> bit) & 1L) != 0L) {
                result = result | (((i >>> bit) & 1L) << out);
                out = out + 1;
            }
            bit = bit + 1;
        }
        return result;
    }

    /**
     * The low bits of {@code i} spread out to the positions {@code mask} selects.
     *
     * @param i the value to scatter
     * @param mask which positions to fill
     */
    public static long expand(long i, long mask) {
        long result = 0L;
        int from = 0;
        int bit = 0;
        while (bit < 64) {
            if (((mask >>> bit) & 1L) != 0L) {
                result = result | (((i >>> from) & 1L) << bit);
                from = from + 1;
            }
            bit = bit + 1;
        }
        return result;
    }

    // ---- the small arithmetic a reduction needs ----

    /**
     * Compares two longs as signed.
     *
     * @param x one value
     * @param y the other
     */
    public static int compare(long x, long y) {
        if (x < y) {
            return -1;
        }
        if (x > y) {
            return 1;
        }
        return 0;
    }

    /**
     * The sum, as a method so it can be passed as a function.
     *
     * @param a one addend
     * @param b the other
     */
    public static long sum(long a, long b) {
        return a + b;
    }

    /**
     * The greater of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static long max(long a, long b) {
        return Math.max(a, b);
    }

    /**
     * The smaller of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static long min(long a, long b) {
        return Math.min(a, b);
    }

    // ---- as an object ----

    /**
     * Equal when {@code other} is a Long holding the same value.
     *
     * @param other the object to compare against
     */
    public boolean equals(Object other) {
        if (!(other instanceof Long)) {
            return false;
        }
        Long that = (Long) other;
        return this.longValue() == that.longValue();
    }

    /** The two halves folded together, which is what the specification fixes the hash to be. */
    public int hashCode() {
        return Long.hashCode(this.longValue());
    }

    /**
     * The hash of a long, without boxing it.
     *
     * @param value the value to hash
     */
    public static int hashCode(long value) {
        return (int) (value ^ (value >>> 32));
    }

    /** This value as a byte, truncated. */
    public byte byteValue() {
        return (byte) this.longValue();
    }

    /** This value as a short, truncated. */
    public short shortValue() {
        return (short) this.longValue();
    }

    /** This value as a nominal descriptor, which is always present. */
    public Optional<Long> describeConstable() {
        return Optional.of(Long.valueOf(this.longValue()));
    }

    /**
     * A Long holding what {@code s} says.
     *
     * @param s the text
     * @throws NumberFormatException if it is not parsable
     * @deprecated the JDK deprecates every wrapper constructor; use {@link #valueOf(String)}.
     */
    @Deprecated(since = "9")
    public Long(String s) {
        this(Long.parseLong(s, 10));
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
    public Long resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup lookup) {
        return Long.valueOf(this.longValue());
    }

    /**
     * The {@link Class} of the primitive {@code long}.
     *
     * <p>Not the same thing as {@code Long.class}, and the difference trips people: that one is
     * the mirror of the WRAPPER, this one of the primitive. They compare unequal, and a
     * reflective lookup that wants one and gets the other simply finds nothing.
     */
    public static final Class<Long> TYPE = Class.getPrimitiveClass("long");

    // ---- from a system property ----
    //
    // A small, odd corner of the API: it reads a system property and decodes it, so the same
    // prefixes a source literal uses (0x, #, leading 0) work there too. It answers `null` rather
    // than throwing when the property is absent OR unparsable, which is what makes the
    // two-argument forms the ones worth using.

    /**
     * The system property {@code nm}, decoded, or null.
     *
     * @param nm the property name
     */
    public static Long getLong(String nm) {
        // Cast, because there are two two-argument forms and a bare `null` picks the
        // primitive one -- which then tries to unbox it (finding #254).
        Long missing = null;
        return Long.getLong(nm, missing);
    }

    /**
     * The system property {@code nm}, decoded, or {@code val} boxed.
     *
     * @param nm the property name
     * @param val what to answer when the property is absent or unparsable
     */
    public static Long getLong(String nm, long val) {
        // Same disambiguation as above: a bare `null` binds to the primitive overload.
        Long missing = null;
        Long found = Long.getLong(nm, missing);
        if (found == null) {
            return Long.valueOf(val);
        }
        return found;
    }

    /**
     * The system property {@code nm}, decoded, or {@code val}.
     *
     * @param nm the property name
     * @param val what to answer when the property is absent or unparsable; may be null
     */
    public static Long getLong(String nm, Long val) {
        if (nm == null || nm.length() == 0) {
            return val;
        }
        String text = System.getProperty(nm);
        if (text == null) {
            return val;
        }
        // Unparsable is treated as absent, not as an error: the value came from outside the
        // program, and the caller already said what to do when it is not there.
        try {
            return Long.decode(text);
        } catch (NumberFormatException unusable) {
            return val;
        }
    }

}
