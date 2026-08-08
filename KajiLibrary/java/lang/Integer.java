package java.lang;

// KajiLibrary's java.lang.Integer — the full boxed-int wrapper (Tier 2): extends Number,
// holds the wrapped `int value`, exposes `valueOf`/`intValue` (what the compiler's
// boxing/unboxing calls), and implements Comparable so ints sort by value. The bit
// operations stay `native` (CPU instructions).
public final class Integer extends Number implements Comparable<Integer> {

    public static final int MIN_VALUE = 0x80000000;

    public static final int MAX_VALUE = 0x7fffffff;

    private final int value;

    public Integer(int value) {
        this.value = value;
    }

    public static Integer valueOf(int i) {
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

    public static native int bitCount(int i);

    public static native int numberOfLeadingZeros(int i);

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
}
