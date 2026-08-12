package java.lang;

// KajiLibrary's java.lang.StringBuilder — the mutable char buffer the compiler lowers
// String concatenation to (`a + b` → `new StringBuilder().append(a).append(b).toString()`).
// It is both a CharSequence (readable) and an Appendable (writable).
//
// Almost entirely real Java: a growable `char[]`, the append overloads (including int → its
// decimal digits, done here without allocating an intermediate String), indexed access, and
// length. The one VM seam is producing a String from the buffer via
// `String.valueOf(char[], int, int)` — the single thing plain bytecode can't do.
public final class StringBuilder implements CharSequence, Appendable {

    private char[] value;

    private int count;

    public StringBuilder() {
        this.value = new char[16];
        this.count = 0;
    }

    // Grow the backing array (doubling) so it can hold at least `min` chars.
    private void ensureCapacity(int min) {
        if (min > value.length) {
            int newCap = value.length * 2;
            if (newCap < min) {
                newCap = min;
            }
            char[] bigger = new char[newCap];
            System.arraycopy(value, 0, bigger, 0, count);
            value = bigger;
        }
    }

    public StringBuilder append(char c) {
        ensureCapacity(count + 1);
        value[count] = c;
        count = count + 1;
        return this;
    }

    public StringBuilder append(String s) {
        if (s == null) {
            s = "null";
        }
        int n = s.length();
        ensureCapacity(count + n);
        for (int i = 0; i < n; i++) {
            value[count + i] = s.charAt(i);
        }
        count = count + n;
        return this;
    }

    public StringBuilder append(boolean b) {
        return append(b ? "true" : "false");
    }

    // Append all of a CharSequence (Appendable). Returns StringBuilder (covariant with
    // Appendable's return), so chaining keeps the concrete type.
    public StringBuilder append(CharSequence s) {
        if (s == null) {
            return append("null");
        }
        int n = s.length();
        ensureCapacity(count + n);
        for (int i = 0; i < n; i++) {
            value[count + i] = s.charAt(i);
        }
        count = count + n;
        return this;
    }

    // Append the sub-range [start, end) of a CharSequence. (null is rendered as "null" —
    // spelled out through a String since our String isn't a CharSequence yet, blocked by #5.)
    public StringBuilder append(CharSequence s, int start, int end) {
        if (s == null) {
            String nul = "null";
            for (int i = start; i < end; i++) {
                append(nul.charAt(i));
            }
            return this;
        }
        for (int i = start; i < end; i++) {
            append(s.charAt(i));
        }
        return this;
    }

    // int → its decimal representation, appended digit by digit. Works in negative space so
    // that Integer.MIN_VALUE (whose magnitude overflows a positive int) is handled correctly.
    public StringBuilder append(int i) {
        if (i == 0) {
            return append('0');
        }
        boolean neg = i < 0;
        if (i > 0) {
            i = -i;
        }
        char[] tmp = new char[11];
        int p = 11;
        while (i < 0) {
            int digit = -(i % 10);
            p = p - 1;
            tmp[p] = (char) ('0' + digit);
            i = i / 10;
        }
        if (neg) {
            append('-');
        }
        for (int k = p; k < 11; k++) {
            append(tmp[k]);
        }
        return this;
    }

    // --- CharSequence ---

    public int length() {
        return count;
    }

    public char charAt(int index) {
        return value[index];
    }

    public CharSequence subSequence(int start, int end) {
        return String.valueOf(value, start, end - start);
    }

    public String toString() {
        return String.valueOf(value, 0, count);
    }
}
