package java.lang;

// java.lang.Integer — the int box (JLS §5.1.7 autoboxing target) plus the
// bit-twiddling statics, native (they map to CPU instructions: popcnt, lzcnt).
public final class Integer {
    // The `int` primitive's Class mirror — `int.class` compiles to `getstatic Integer.TYPE`.
    // Initialised from the VM's primitive-class factory (there's no Java expression for it).
    public static final Class<Integer> TYPE = (Class<Integer>) Class.getPrimitiveClass("int");

    public static native int bitCount(int i);

    public static native int numberOfLeadingZeros(int i);

    private final int value;

    public Integer(int value) {
        this.value = value;
    }

    // Cache for -128..127 — the JLS mandates boxed identity (==) for small values,
    // so valueOf must return the SAME object for repeats in that range.
    private static class IntegerCache {
        static final Integer[] cache = new Integer[256];

        static {
            for (int i = 0; i < 256; i++) {
                cache[i] = new Integer(i - 128);
            }
        }
    }

    public static Integer valueOf(int i) {
        if (i >= -128 && i <= 127) {
            return IntegerCache.cache[i + 128];
        }
        return new Integer(i);
    }

    public int intValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (o instanceof Integer) {
            return value == ((Integer) o).value;
        }
        return false;
    }

    public int hashCode() {
        return value;
    }
}
