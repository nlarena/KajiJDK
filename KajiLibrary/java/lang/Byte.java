package java.lang;

// KajiLibrary's java.lang.Byte — the boxed-byte wrapper (extends Number, implements
// Comparable). The constants exercise the §5.2 narrowing of a constant int to byte.
public final class Byte extends Number implements Comparable<Byte> {

    public static final byte MIN_VALUE = -128;

    public static final byte MAX_VALUE = 127;

    private final byte value;

    public Byte(byte value) {
        this.value = value;
    }

    public static Byte valueOf(byte b) {
        return new Byte(b);
    }

    public int intValue() {
        return value;
    }

    public long longValue() {
        return value;
    }

    public float floatValue() {
        return value;
    }

    public double doubleValue() {
        return value;
    }

    public byte byteValue() {
        return value;
    }

    public int compareTo(Byte o) {
        return this.value < o.value ? -1 : (this.value == o.value ? 0 : 1);
    }
}
