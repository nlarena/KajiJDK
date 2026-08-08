package java.lang;

// KajiLibrary's java.lang.Long — the boxed-long wrapper. Same shape as Integer: extends
// Number, implements Comparable, provides valueOf/longValue for boxing/unboxing.
public final class Long extends Number implements Comparable<Long> {

    public static final long MIN_VALUE = 0x8000000000000000L;

    public static final long MAX_VALUE = 0x7fffffffffffffffL;

    private final long value;

    public Long(long value) {
        this.value = value;
    }

    public static Long valueOf(long l) {
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
}
