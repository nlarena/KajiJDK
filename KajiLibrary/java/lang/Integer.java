package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde
// java.lang (finding #210).
import java.lang.constant.ConstantDesc;
import java.util.Optional;

// KajiLibrary's java.lang.Integer — the full boxed-int wrapper (Tier 2): extends Number,
// holds the wrapped `int value`, exposes `valueOf`/`intValue` (what the compiler's
// boxing/unboxing calls), and implements Comparable so ints sort by value. The bit
// operations stay `native` (CPU instructions).
public final class Integer extends Number implements Comparable<Integer>, ConstantDesc {

    public static final int MIN_VALUE = 0x80000000;

    public static final int MAX_VALUE = 0x7fffffff;

    private final int value;

    public Integer(int value) {
        this.value = value;
    }

    // Las instancias compartidas de -128..127. Viven en una clase anidada para que se construyan
    // en el primer `valueOf` y no en la primera mencion de la clase.
    //
    // No es una optimizacion: **JLS 5.1.7 exige que boxear un valor de ese rango devuelva la
    // MISMA referencia**, asi que `Integer.valueOf(100) == Integer.valueOf(100)` es una promesa del lenguaje. Sin la cache
    // la promesa se rompe en silencio -- el codigo sigue andando hasta que alguien compara con
    // `==`, que es justo lo que la cache existe para permitir.
    private static final class IntegerCache {

        static final Integer[] CACHE = IntegerCache.fill();

        private static Integer[] fill() {
            Integer[] out = new Integer[256];
            int i = 0;
            while (i < 256) {
                out[i] = new Integer((int) (i + (-128)));
                i = i + 1;
            }
            return out;
        }
    }

    public static Integer valueOf(int i) {
        if (i >= -128 && i <= 127) {
            return IntegerCache.CACHE[i + 128];
        }
        return new Integer(i);
    }

    public int intValue() {
        return value;
    }

    public long longValue() {
        return (long) value;
    }

    public float floatValue() {
        return (float) value;
    }

    public double doubleValue() {
        return (double) value;
    }

    // Natural ordering by value. Overriding Comparable.compareTo(T) makes the compiler
    // synthesize the compareTo(Object) bridge.
    public int compareTo(Integer o) {
        return this.value < o.value ? -1 : (this.value == o.value ? 0 : 1);
    }

    /**
     * How many bits of {@code i} are set.
     *
     * <p>Not native, and it does not need to be: the JDK does not declare it native either. The
     * trick is that it counts in PARALLEL -- first the 32 one-bit fields against their
     * neighbours, then the 16 two-bit sums, and so on -- so it takes five steps rather than
     * thirty-two, with no branch and no loop.
     *
     * @param i the value
     */
    public static int bitCount(int i) {
        int n = i;
        n = n - ((n >>> 1) & 0x55555555);
        n = (n & 0x33333333) + ((n >>> 2) & 0x33333333);
        n = (n + (n >>> 4)) & 0x0f0f0f0f;
        n = n + (n >>> 8);
        n = n + (n >>> 16);
        return n & 0x3f;
    }

    /**
     * How many zero bits precede the highest set bit.
     *
     * <p>A binary search over the width: each step asks whether anything is left in the top half
     * and, if so, throws away the bottom one. Five steps for thirty-two bits.
     *
     * @param i the value; zero answers 32
     */
    public static int numberOfLeadingZeros(int i) {
        if (i == 0) {
            return 32;
        }
        if (i < 0) {
            return 0; // the top bit is the sign bit, and it is set
        }
        int n = 31;
        int rest = i;
        if (rest >= (1 << 16)) {
            n = n - 16;
            rest = rest >>> 16;
        }
        if (rest >= (1 << 8)) {
            n = n - 8;
            rest = rest >>> 8;
        }
        if (rest >= (1 << 4)) {
            n = n - 4;
            rest = rest >>> 4;
        }
        if (rest >= (1 << 2)) {
            n = n - 2;
            rest = rest >>> 2;
        }
        return n - (rest >>> 1);
    }

    // The signed decimal string for `i`. Magnitude is accumulated in negative space so
    // Integer.MIN_VALUE (which can't be negated) is handled.
    public static String toString(int i) {
        if (i == 0) {
            return "0";
        }
        boolean neg = i < 0;
        int x = i;
        if (x > 0) {
            x = -x;
        }
        char[] buf = new char[12];
        int pos = 12;
        while (x != 0) {
            int d = -(x % 10);
            pos = pos - 1;
            buf[pos] = (char) ('0' + d);
            x = x / 10;
        }
        if (neg) {
            pos = pos - 1;
            buf[pos] = '-';
        }
        return String.valueOf(buf, pos, 12 - pos);
    }

    public String toString() {
        return Integer.toString(this.value);
    }

    // The unsigned hexadecimal string for `i` (no leading zeros), treating the int as a
    // 32-bit bit pattern. Used by Object.toString and the %x conversion (H6-T2).
    public static String toHexString(int i) {
        if (i == 0) {
            return "0";
        }
        char[] buf = new char[8];
        int pos = 8;
        int v = i;
        while (v != 0) {
            int d = v & 0xF;
            pos = pos - 1;
            if (d < 10) {
                buf[pos] = (char) ('0' + d);
            } else {
                buf[pos] = (char) ('a' + d - 10);
            }
            v = v >>> 4;
        }
        return String.valueOf(buf, pos, 8 - pos);
    }

    /** Bits in an int. */
    public static final int SIZE = 32;

    /** Bytes in an int. */
    public static final int BYTES = 4;

    // ---- text out ----
    //
    // Two families that look alike and are not. `toString` reads the int as SIGNED, so half its
    // range comes out with a minus; `toUnsignedString` reads the same bits as a value in
    // [0, 2^32), so nothing ever does. Which one a program wants is not a detail: the same word
    // is -1 or 4294967295 depending only on the question asked.

    /**
     * {@code i} written in {@code radix}, signed.
     *
     * @param i the value
     * @param radix the base; anything outside 2..36 is taken as 10, which is the JDK behaviour
     *        and is worth knowing because it silently ignores a mistake
     */
    public static String toString(int i, int radix) {
        int base = radix;
        if (base < Character.MIN_RADIX || base > Character.MAX_RADIX) {
            base = 10;
        }
        if (base == 10) {
            return Integer.toString(i);
        }
        if (i == 0) {
            return "0";
        }
        // Accumulated NEGATIVE, so that Integer.MIN_VALUE needs no special case: its magnitude
        // does not fit but its negation does, and every other value fits either way.
        boolean negative = i < 0;
        int rest = i;
        if (!negative) {
            rest = -i;
        }
        String digits = "";
        while (rest != 0) {
            int digit = -(rest % base);
            rest = rest / base;
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
     * @param i the bits to read
     * @param radix the base; anything outside 2..36 is taken as 10
     */
    public static String toUnsignedString(int i, int radix) {
        return Long.toString(Integer.toUnsignedLong(i), radix);
    }

    /**
     * {@code i} written in base ten, read as unsigned.
     *
     * @param i the bits to read
     */
    public static String toUnsignedString(int i) {
        return Long.toString(Integer.toUnsignedLong(i));
    }

    /**
     * {@code i} in binary, read as unsigned and without leading zeros.
     *
     * @param i the bits to read
     */
    public static String toBinaryString(int i) {
        return Integer.toUnsignedString(i, 2);
    }

    /**
     * {@code i} in octal, read as unsigned and without leading zeros.
     *
     * @param i the bits to read
     */
    public static String toOctalString(int i) {
        return Integer.toUnsignedString(i, 8);
    }

    // ---- text in ----

    /**
     * The int {@code s} denotes in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable int
     */
    public static int parseInt(String s) {
        return Integer.parseInt(s, 10);
    }

    /**
     * The int {@code s} denotes in {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not parsable, or the radix is out of range
     */
    public static int parseInt(String s, int radix) {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string");
        }
        return Integer.parseInt(s, 0, s.length(), radix);
    }

    /**
     * The int denoted by {@code [beginIndex, endIndex)} of {@code s}, in {@code radix}.
     *
     * <p>The accumulation runs NEGATIVE, which is not a trick for its own sake: the range of an
     * int is asymmetric, so building the magnitude and negating at the end cannot represent
     * {@link #MIN_VALUE}. Building the negative and negating only when the sign says so can.
     *
     * @param s the text
     * @param beginIndex where to start
     * @param endIndex where to stop
     * @param radix the base
     * @throws NumberFormatException if it is not parsable, or the radix is out of range
     * @throws IndexOutOfBoundsException if the range is not within {@code s}
     */
    public static int parseInt(CharSequence s, int beginIndex, int endIndex, int radix) {
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
        int limit = -Integer.MAX_VALUE;
        char first = s.charAt(i);
        if (first == '-') {
            negative = true;
            limit = Integer.MIN_VALUE;
            i = i + 1;
        } else if (first == '+') {
            i = i + 1;
        }
        if (i == endIndex) {
            throw new NumberFormatException("No digits");
        }
        int multmin = limit / radix;
        int result = 0;
        while (i < endIndex) {
            int digit = Character.digit(s.charAt(i), radix);
            if (digit < 0) {
                throw new NumberFormatException("Not a digit at " + i);
            }
            if (result < multmin) {
                throw new NumberFormatException("Value out of range");
            }
            result = result * radix;
            if (result < limit + digit) {
                throw new NumberFormatException("Value out of range");
            }
            result = result - digit;
            i = i + 1;
        }
        if (negative) {
            return result;
        }
        return -result;
    }

    /**
     * The unsigned int {@code s} denotes in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable unsigned int
     */
    public static int parseUnsignedInt(String s) {
        return Integer.parseUnsignedInt(s, 10);
    }

    /**
     * The unsigned int {@code s} denotes in {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not parsable
     */
    public static int parseUnsignedInt(String s, int radix) {
        if (s == null) {
            throw new NumberFormatException("Cannot parse null string");
        }
        return Integer.parseUnsignedInt(s, 0, s.length(), radix);
    }

    /**
     * The unsigned int denoted by {@code [beginIndex, endIndex)} of {@code s}.
     *
     * @param s the text
     * @param beginIndex where to start
     * @param endIndex where to stop
     * @param radix the base
     * @throws NumberFormatException if it is not parsable
     */
    public static int parseUnsignedInt(CharSequence s, int beginIndex, int endIndex, int radix) {
        if (s == null) {
            throw new NullPointerException();
        }
        if (beginIndex < 0 || beginIndex > endIndex || endIndex > s.length()) {
            throw new IndexOutOfBoundsException();
        }
        if (beginIndex < endIndex && s.charAt(beginIndex) == '-') {
            throw new NumberFormatException("Illegal leading minus sign");
        }
        // Parsed one size up and then range-checked: a long holds every unsigned int exactly, so
        // there is no overflow to detect by hand.
        long wide = Long.parseLong(s, beginIndex, endIndex, radix);
        if ((wide & 0xFFFFFFFF00000000L) != 0L) {
            throw new NumberFormatException("Value out of range");
        }
        return (int) wide;
    }

    /**
     * The Integer {@code s} denotes in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not parsable
     */
    public static Integer valueOf(String s) {
        return Integer.valueOf(Integer.parseInt(s, 10));
    }

    /**
     * The Integer {@code s} denotes in {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not parsable
     */
    public static Integer valueOf(String s, int radix) {
        return Integer.valueOf(Integer.parseInt(s, radix));
    }

    /**
     * The Integer {@code nm} denotes, with the base taken from its prefix.
     *
     * <p>{@code 0x}/{@code 0X}/{@code #} mean hexadecimal, a leading {@code 0} means octal, and
     * anything else is decimal -- the same convention a Java source literal uses, which is the
     * point: this is for reading values that were written for people.
     *
     * @param nm the text
     * @throws NumberFormatException if it is not parsable
     */
    public static Integer decode(String nm) {
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
        return Integer.valueOf(Integer.parseInt(digits, radix));
    }

    // ---- reading the same bits as unsigned ----

    /**
     * {@code x} widened without sign extension, so the result is in {@code [0, 2^32)}.
     *
     * @param x the bits to read
     */
    public static long toUnsignedLong(int x) {
        return (long) x & 0xFFFFFFFFL;
    }

    /**
     * Compares two ints as unsigned.
     *
     * @param x one value
     * @param y the other
     */
    public static int compareUnsigned(int x, int y) {
        return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
    }

    /**
     * {@code dividend / divisor} with both read as unsigned.
     *
     * @param dividend the dividend
     * @param divisor the divisor
     * @throws ArithmeticException if the divisor is zero
     */
    public static int divideUnsigned(int dividend, int divisor) {
        long q = Integer.toUnsignedLong(dividend) / Integer.toUnsignedLong(divisor);
        return (int) q;
    }

    /**
     * The remainder of {@link #divideUnsigned}.
     *
     * @param dividend the dividend
     * @param divisor the divisor
     * @throws ArithmeticException if the divisor is zero
     */
    public static int remainderUnsigned(int dividend, int divisor) {
        long r = Integer.toUnsignedLong(dividend) % Integer.toUnsignedLong(divisor);
        return (int) r;
    }

    // ---- the bit operations ----

    /**
     * An int with only the highest set bit of {@code i} kept.
     *
     * @param i the value
     */
    public static int highestOneBit(int i) {
        if (i == 0) {
            return 0;
        }
        return 1 << (31 - Integer.numberOfLeadingZeros(i));
    }

    /**
     * An int with only the lowest set bit of {@code i} kept.
     *
     * @param i the value
     */
    public static int lowestOneBit(int i) {
        // Two's complement in one line: negating flips every bit above the lowest one and leaves
        // that one alone, so the AND keeps exactly it.
        return i & -i;
    }

    /**
     * How many zero bits follow the lowest set bit.
     *
     * @param i the value; zero answers 32
     */
    public static int numberOfTrailingZeros(int i) {
        if (i == 0) {
            return 32;
        }
        return 31 - Integer.numberOfLeadingZeros(Integer.lowestOneBit(i));
    }

    /**
     * {@code i} with its bits in the opposite order.
     *
     * @param i the value
     */
    public static int reverse(int i) {
        int result = 0;
        int bit = 0;
        while (bit < 32) {
            result = result | (((i >>> bit) & 1) << (31 - bit));
            bit = bit + 1;
        }
        return result;
    }

    /**
     * {@code i} with its four bytes in the opposite order.
     *
     * @param i the value
     */
    public static int reverseBytes(int i) {
        return ((i >>> 24) & 0xFF) | ((i >>> 8) & 0xFF00) | ((i << 8) & 0xFF0000) | (i << 24);
    }

    /**
     * {@code i} rotated left, with the bits that fall off the top coming back at the bottom.
     *
     * @param i the value
     * @param distance how far; only the low five bits matter, and a negative value rotates right
     */
    public static int rotateLeft(int i, int distance) {
        return (i << distance) | (i >>> -distance);
    }

    /**
     * {@code i} rotated right.
     *
     * @param i the value
     * @param distance how far; only the low five bits matter
     */
    public static int rotateRight(int i, int distance) {
        return (i >>> distance) | (i << -distance);
    }

    /**
     * The sign: -1, 0 or 1.
     *
     * @param i the value
     */
    public static int signum(int i) {
        return (i >> 31) | (-i >>> 31);
    }

    /**
     * The bits of {@code i} at the positions {@code mask} selects, packed into the low end.
     *
     * @param i the value to gather from
     * @param mask which positions to take
     */
    public static int compress(int i, int mask) {
        int result = 0;
        int out = 0;
        int bit = 0;
        while (bit < 32) {
            if (((mask >>> bit) & 1) != 0) {
                result = result | (((i >>> bit) & 1) << out);
                out = out + 1;
            }
            bit = bit + 1;
        }
        return result;
    }

    /**
     * The low bits of {@code i} spread out to the positions {@code mask} selects.
     *
     * <p>The inverse of {@link #compress} against the same mask.
     *
     * @param i the value to scatter
     * @param mask which positions to fill
     */
    public static int expand(int i, int mask) {
        int result = 0;
        int from = 0;
        int bit = 0;
        while (bit < 32) {
            if (((mask >>> bit) & 1) != 0) {
                result = result | (((i >>> from) & 1) << bit);
                from = from + 1;
            }
            bit = bit + 1;
        }
        return result;
    }

    // ---- the small arithmetic a reduction needs ----

    /**
     * Compares two ints as signed.
     *
     * @param x one value
     * @param y the other
     */
    public static int compare(int x, int y) {
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
    public static int sum(int a, int b) {
        return a + b;
    }

    /**
     * The greater of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static int max(int a, int b) {
        return Math.max(a, b);
    }

    /**
     * The smaller of two values.
     *
     * @param a one value
     * @param b the other
     */
    public static int min(int a, int b) {
        return Math.min(a, b);
    }

    // ---- as an object ----

    /**
     * Equal when {@code other} is an Integer holding the same value.
     *
     * @param other the object to compare against
     */
    public boolean equals(Object other) {
        if (!(other instanceof Integer)) {
            return false;
        }
        Integer that = (Integer) other;
        return this.intValue() == that.intValue();
    }

    /** The value itself, which is what the specification fixes the hash to be. */
    public int hashCode() {
        return this.intValue();
    }

    /**
     * The hash of an int, without boxing it.
     *
     * @param value the value to hash
     */
    public static int hashCode(int value) {
        return value;
    }

    /** This value as a byte, truncated. */
    public byte byteValue() {
        return (byte) this.intValue();
    }

    /** This value as a short, truncated. */
    public short shortValue() {
        return (short) this.intValue();
    }

    /** This value as a nominal descriptor, which is always present. */
    public Optional<Integer> describeConstable() {
        return Optional.of(Integer.valueOf(this.intValue()));
    }

    /**
     * An Integer holding what {@code s} says.
     *
     * @param s the text
     * @throws NumberFormatException if it is not parsable
     * @deprecated the JDK deprecates every wrapper constructor; use {@link #valueOf(String)}.
     */
    @Deprecated(since = "9")
    public Integer(String s) {
        this(Integer.parseInt(s, 10));
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
    public Integer resolveConstantDesc(java.lang.invoke.MethodHandles.Lookup lookup) {
        return Integer.valueOf(this.intValue());
    }

    /**
     * The {@link Class} of the primitive {@code int}.
     *
     * <p>Not the same thing as {@code Integer.class}, and the difference trips people: that one is
     * the mirror of the WRAPPER, this one of the primitive. They compare unequal, and a
     * reflective lookup that wants one and gets the other simply finds nothing.
     */
    public static final Class<Integer> TYPE = Class.getPrimitiveClass("int");

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
    public static Integer getInteger(String nm) {
        // Cast, because there are two two-argument forms and a bare `null` picks the
        // primitive one -- which then tries to unbox it (finding #254).
        Integer missing = null;
        return Integer.getInteger(nm, missing);
    }

    /**
     * The system property {@code nm}, decoded, or {@code val} boxed.
     *
     * @param nm the property name
     * @param val what to answer when the property is absent or unparsable
     */
    public static Integer getInteger(String nm, int val) {
        // Same disambiguation as above: a bare `null` binds to the primitive overload.
        Integer missing = null;
        Integer found = Integer.getInteger(nm, missing);
        if (found == null) {
            return Integer.valueOf(val);
        }
        return found;
    }

    /**
     * The system property {@code nm}, decoded, or {@code val}.
     *
     * @param nm the property name
     * @param val what to answer when the property is absent or unparsable; may be null
     */
    public static Integer getInteger(String nm, Integer val) {
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
            return Integer.decode(text);
        } catch (NumberFormatException unusable) {
            return val;
        }
    }

}
