package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.Short -- the boxed {@code short}.
 *
 * <p>Almost everything here goes through {@link Integer}, and that is the design rather than
 * laziness: a {@code short} has no arithmetic of its own in the JVM -- every operation on one is
 * an {@code int} operation with a narrowing at the end -- so a separate parser or formatter would
 * be a second implementation of the same thing, able to disagree with the first. What this class
 * adds is the RANGE CHECK, which is the only place a short differs from an int.
 */
public final class Short extends Number implements Comparable<Short>, java.lang.constant.Constable {

    /** -32768, the most negative short. */
    public static final short MIN_VALUE = -32768;

    /** 32767, the largest short. */
    public static final short MAX_VALUE = 32767;

    /** How many bits a short occupies. */
    public static final int SIZE = 16;

    /** How many bytes a short occupies. */
    public static final int BYTES = 2;

    /**
     * The mirror of the primitive type {@code short}.
     *
     * <p>Not {@code Short.class}: that one names this class, this one names the primitive. They
     * are different mirrors and comparing them is false.
     */
    public static final Class<Short> TYPE = Class.getPrimitiveClass("short");

    private final short value;

    /**
     * A Short holding {@code value}.
     *
     * @param value the value
     * @deprecated the JDK deprecates every wrapper constructor; use {@link #valueOf(short)},
     *         which can hand back a shared instance.
     */
    @Deprecated(since = "9")
    public Short(short value) {
        this.value = value;
    }

    /**
     * A Short holding what {@code s} says.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable short
     * @deprecated as with the other constructor; use {@link #valueOf(String)}.
     */
    @Deprecated(since = "9")
    public Short(String s) {
        this.value = Short.parseShort(s, 10);
    }

    // The shared instances for -128..127. Held in a nested class so that they are built on the
    // first `valueOf` and not on the first mention of `Short` -- and they must be shared, not
    // merely equal: JLS 5.1.7 requires boxing a value in this range to yield the SAME reference,
    // so `Short.valueOf((short) 100) == Short.valueOf((short) 100)` is a promise the language
    // makes and code does rely on it.
    private static final class ShortCache {

        static final Short[] CACHE = ShortCache.fill();

        private static Short[] fill() {
            Short[] out = new Short[256];
            int i = 0;
            while (i < 256) {
                out[i] = new Short((short) (i - 128));
                i = i + 1;
            }
            return out;
        }
    }

    /**
     * A Short holding {@code s}, shared if it is small.
     *
     * @param s the value
     */
    public static Short valueOf(short s) {
        if (s >= -128 && s <= 127) {
            return ShortCache.CACHE[s + 128];
        }
        return new Short(s);
    }

    /**
     * A Short holding what {@code s} says, in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable short
     */
    public static Short valueOf(String s) {
        return Short.valueOf(Short.parseShort(s, 10));
    }

    /**
     * A Short holding what {@code s} says, in base {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not a parsable short
     */
    public static Short valueOf(String s, int radix) {
        return Short.valueOf(Short.parseShort(s, radix));
    }

    /**
     * The short {@code s} names, in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable short
     */
    public static short parseShort(String s) {
        return Short.parseShort(s, 10);
    }

    /**
     * The short {@code s} names, in base {@code radix}.
     *
     * <p>Parsed as an int and then range-checked, which is where the two differ: {@code "40000"}
     * is a perfectly good int and not a short, and it has to fail here rather than silently
     * wrapping to -25536.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not a parsable short
     */
    public static short parseShort(String s, int radix) {
        int wide = Integer.parseInt(s, radix);
        if (wide < Short.MIN_VALUE || wide > Short.MAX_VALUE) {
            throw new NumberFormatException("value out of range for a short: " + s);
        }
        return (short) wide;
    }

    /**
     * The short {@code nm} names, accepting the {@code 0x}, {@code 0X}, {@code #} and leading-zero
     * spellings as well as decimal.
     *
     * @param nm the text
     * @throws NumberFormatException if it is not a parsable short
     */
    public static Short decode(String nm) {
        int wide = Integer.decode(nm).intValue();
        if (wide < Short.MIN_VALUE || wide > Short.MAX_VALUE) {
            throw new NumberFormatException("value out of range for a short: " + nm);
        }
        return Short.valueOf((short) wide);
    }

    // ---- reading the value out ----

    public byte byteValue() {
        return (byte) this.value;
    }

    public short shortValue() {
        return this.value;
    }

    public int intValue() {
        return this.value;
    }

    public long longValue() {
        return this.value;
    }

    public float floatValue() {
        return this.value;
    }

    public double doubleValue() {
        return this.value;
    }

    // ---- comparing ----

    /**
     * Whether {@code obj} is a Short holding the same value.
     *
     * @param obj the object to compare against
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Short)) {
            return false;
        }
        Short other = (Short) obj;
        return this.value == other.shortValue();
    }

    /** The value itself, widened -- which is what the JDK specifies. */
    public int hashCode() {
        return Short.hashCode(this.value);
    }

    /**
     * The hash a Short holding {@code value} would have.
     *
     * @param value the value
     */
    public static int hashCode(short value) {
        return value;
    }

    /**
     * Compares two shorts as signed.
     *
     * @param x the first
     * @param y the second
     */
    public static int compare(short x, short y) {
        return x - y;
    }

    /**
     * Compares two shorts as UNSIGNED, so that -1 is the largest of them.
     *
     * @param x the first
     * @param y the second
     */
    public static int compareUnsigned(short x, short y) {
        return Short.toUnsignedInt(x) - Short.toUnsignedInt(y);
    }

    /**
     * Compares this Short against another.
     *
     * @param anotherShort what to compare against
     */
    public int compareTo(Short anotherShort) {
        return Short.compare(this.value, anotherShort.shortValue());
    }

    // ---- reinterpreting ----

    /**
     * The value read as unsigned, in an int: {@code -1} becomes {@code 65535}.
     *
     * @param x the value
     */
    public static int toUnsignedInt(short x) {
        return x & 0xffff;
    }

    /**
     * The value read as unsigned, in a long.
     *
     * @param x the value
     */
    public static long toUnsignedLong(short x) {
        return x & 0xffffL;
    }

    /**
     * The value with its two bytes swapped.
     *
     * @param i the value
     */
    public static short reverseBytes(short i) {
        return (short) (((i & 0xff00) >> 8) | (i << 8));
    }

    // ---- printing ----

    /** The value in base ten. */
    public String toString() {
        return Integer.toString(this.value, 10);
    }

    /**
     * {@code value} in base ten.
     *
     * @param value the value
     */
    public static String toString(short value) {
        return Integer.toString(value, 10);
    }

    /**
     * This value as a constant that can be written into a class file.
     *
     * <p>A <em>dynamic</em> constant, unlike {@code Integer}'s, and the reason is that a class
     * file has no short constant: the pool holds an int, and the descriptor says "take this int
     * and cast it", which is exactly what {@code BSM_EXPLICIT_CAST} means.
     */
    public Optional<DynamicConstantDesc<Short>> describeConstable() {
        return Optional.of(DynamicConstantDesc.ofNamed(ConstantDescs.BSM_EXPLICIT_CAST,
                ConstantDescs.DEFAULT_NAME, ConstantDescs.CD_short,
                Integer.valueOf(this.value)));
    }
}
