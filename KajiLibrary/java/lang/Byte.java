package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.Byte -- the boxed {@code byte}.
 *
 * <p>Built on {@link Integer} for the same reason {@link Short} is: the JVM has no byte
 * arithmetic, so parsing and formatting one is int work with a narrowing at the end, and writing
 * it twice would be writing two things that can disagree.
 *
 * <p>One thing here has no counterpart in the other wrappers: the cache covers the WHOLE type.
 * A byte has 256 values and the shared range is -128..127, so every {@code Byte} that
 * {@link #valueOf(byte)} ever returns is one of the 256 built once -- {@code new Byte(b)} is the
 * only way to get a distinct one, which is most of why the constructor is deprecated.
 */
public final class Byte extends Number implements Comparable<Byte>, java.lang.constant.Constable {

    /** -128, the most negative byte. */
    public static final byte MIN_VALUE = -128;

    /** 127, the largest byte. */
    public static final byte MAX_VALUE = 127;

    /** How many bits a byte occupies. */
    public static final int SIZE = 8;

    /** How many bytes a byte occupies, which is one. */
    public static final int BYTES = 1;

    /**
     * The mirror of the primitive type {@code byte}.
     *
     * <p>Not {@code Byte.class}: that one names this class.
     */
    public static final Class<Byte> TYPE = Class.getPrimitiveClass("byte");

    private final byte value;

    /**
     * A Byte holding {@code value}.
     *
     * @param value the value
     * @deprecated the JDK deprecates every wrapper constructor; use {@link #valueOf(byte)},
     *         which never allocates.
     */
    @Deprecated(since = "9")
    public Byte(byte value) {
        this.value = value;
    }

    /**
     * A Byte holding what {@code s} says.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable byte
     * @deprecated as with the other constructor; use {@link #valueOf(String)}.
     */
    @Deprecated(since = "9")
    public Byte(String s) {
        this.value = Byte.parseByte(s, 10);
    }

    // Every byte there is, built on the first `valueOf` and shared from then on. JLS 5.1.7
    // requires the sharing for -128..127, which for this type is all of them.
    private static final class ByteCache {

        static final Byte[] CACHE = ByteCache.fill();

        private static Byte[] fill() {
            Byte[] out = new Byte[256];
            int i = 0;
            while (i < 256) {
                out[i] = new Byte((byte) (i - 128));
                i = i + 1;
            }
            return out;
        }
    }

    /**
     * The shared Byte holding {@code b}. Never allocates.
     *
     * @param b the value
     */
    public static Byte valueOf(byte b) {
        return ByteCache.CACHE[b + 128];
    }

    /**
     * A Byte holding what {@code s} says, in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable byte
     */
    public static Byte valueOf(String s) {
        return Byte.valueOf(Byte.parseByte(s, 10));
    }

    /**
     * A Byte holding what {@code s} says, in base {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not a parsable byte
     */
    public static Byte valueOf(String s, int radix) {
        return Byte.valueOf(Byte.parseByte(s, radix));
    }

    /**
     * The byte {@code s} names, in base ten.
     *
     * @param s the text
     * @throws NumberFormatException if it is not a parsable byte
     */
    public static byte parseByte(String s) {
        return Byte.parseByte(s, 10);
    }

    /**
     * The byte {@code s} names, in base {@code radix}.
     *
     * @param s the text
     * @param radix the base
     * @throws NumberFormatException if it is not a parsable byte
     */
    public static byte parseByte(String s, int radix) {
        int wide = Integer.parseInt(s, radix);
        if (wide < Byte.MIN_VALUE || wide > Byte.MAX_VALUE) {
            throw new NumberFormatException("value out of range for a byte: " + s);
        }
        return (byte) wide;
    }

    /**
     * The byte {@code nm} names, accepting the {@code 0x}, {@code 0X}, {@code #} and leading-zero
     * spellings as well as decimal.
     *
     * @param nm the text
     * @throws NumberFormatException if it is not a parsable byte
     */
    public static Byte decode(String nm) {
        int wide = Integer.decode(nm).intValue();
        if (wide < Byte.MIN_VALUE || wide > Byte.MAX_VALUE) {
            throw new NumberFormatException("value out of range for a byte: " + nm);
        }
        return Byte.valueOf((byte) wide);
    }

    // ---- reading the value out ----

    public byte byteValue() {
        return this.value;
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
     * Whether {@code obj} is a Byte holding the same value.
     *
     * @param obj the object to compare against
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Byte)) {
            return false;
        }
        Byte other = (Byte) obj;
        return this.value == other.byteValue();
    }

    /** The value itself, widened. */
    public int hashCode() {
        return Byte.hashCode(this.value);
    }

    /**
     * The hash a Byte holding {@code value} would have.
     *
     * @param value the value
     */
    public static int hashCode(byte value) {
        return value;
    }

    /**
     * Compares two bytes as signed.
     *
     * @param x the first
     * @param y the second
     */
    public static int compare(byte x, byte y) {
        return x - y;
    }

    /**
     * Compares two bytes as UNSIGNED, so that -1 is the largest of them.
     *
     * @param x the first
     * @param y the second
     */
    public static int compareUnsigned(byte x, byte y) {
        return Byte.toUnsignedInt(x) - Byte.toUnsignedInt(y);
    }

    /**
     * Compares this Byte against another.
     *
     * @param anotherByte what to compare against
     */
    public int compareTo(Byte anotherByte) {
        return Byte.compare(this.value, anotherByte.byteValue());
    }

    // ---- reinterpreting ----

    /**
     * The value read as unsigned, in an int: {@code -1} becomes {@code 255}.
     *
     * <p>The one every byte-oriented format needs, because a byte read off a stream is data and
     * not a small negative number.
     *
     * @param x the value
     */
    public static int toUnsignedInt(byte x) {
        return x & 0xff;
    }

    /**
     * The value read as unsigned, in a long.
     *
     * @param x the value
     */
    public static long toUnsignedLong(byte x) {
        return x & 0xffL;
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
    public static String toString(byte value) {
        return Integer.toString(value, 10);
    }

    /**
     * This value as a constant that can be written into a class file.
     *
     * <p>A dynamic constant, for the same reason as {@link Short#describeConstable()}: the pool
     * holds an int and the descriptor says to cast it.
     */
    public Optional<DynamicConstantDesc<Byte>> describeConstable() {
        return Optional.of(DynamicConstantDesc.ofNamed(ConstantDescs.BSM_EXPLICIT_CAST,
                ConstantDescs.DEFAULT_NAME, ConstantDescs.CD_byte,
                Integer.valueOf(this.value)));
    }
}
