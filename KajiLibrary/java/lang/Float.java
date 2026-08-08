package java.lang;

// KajiLibrary's java.lang.Float — the boxed-float wrapper (extends Number, implements
// Comparable). The narrowing views (intValue/longValue) need an explicit cast.
public final class Float extends Number implements Comparable<Float> {

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
}
