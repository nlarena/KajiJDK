package java.lang;

// KajiLibrary's java.lang.Short — the boxed-short wrapper (extends Number, implements
// Comparable). The constants exercise the §5.2 narrowing of a constant int to short.
public final class Short extends Number implements Comparable<Short> {

    public static final short MIN_VALUE = -32768;

    public static final short MAX_VALUE = 32767;

    private final short value;

    public Short(short value) {
        this.value = value;
    }

    public static Short valueOf(short s) {
        return new Short(s);
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

    public short shortValue() {
        return value;
    }

    public int compareTo(Short o) {
        return this.value < o.value ? -1 : (this.value == o.value ? 0 : 1);
    }
}
