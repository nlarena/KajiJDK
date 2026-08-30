package java.lang;

import java.io.Serializable;

/**
 * KajiLibrary's java.lang.StringBuffer -- {@link StringBuilder} with a lock around it.
 *
 * <p>Same implementation as {@code StringBuilder}: both extend the shared, package-private
 * {@link AbstractStringBuilder}. What this class adds is the monitor. It re-declares <strong>every
 * public method</strong> as {@code synchronized}, so the lock is taken on this object before any
 * read or write of the buffer -- the chainable ones as covariant overrides typed {@code
 * StringBuffer} (the bridge the JDK emits), the rest as synchronized forwarders to the base. It
 * cannot simply inherit them: an inherited method (or a #268 accessor forwarder) would run with no
 * lock, which is the one thing this class exists to prevent.
 *
 * <p>This is the legacy, synchronized one; {@code StringBuilder} is what the compiler emits into
 * every string concatenation.
 */
public final class StringBuffer extends AbstractStringBuilder
        implements Serializable, Comparable<StringBuffer> {

    /** An empty buffer with room for sixteen characters. */
    public StringBuffer() {
        super(16);
    }

    /**
     * An empty buffer with room for {@code capacity} characters.
     *
     * @param capacity how much room to reserve
     * @throws NegativeArraySizeException if {@code capacity} is negative
     */
    public StringBuffer(int capacity) {
        super(capacity);
    }

    /**
     * A buffer holding {@code str}, with sixteen characters of room to spare.
     *
     * @param str the initial contents
     */
    public StringBuffer(String str) {
        super(str.length() + 16);
        this.append(str);
    }

    /**
     * A buffer holding {@code seq}, with sixteen characters of room to spare.
     *
     * @param seq the initial contents
     */
    public StringBuffer(CharSequence seq) {
        super(seq.length() + 16);
        this.append(seq);
    }

    // ---- room ----

    public synchronized int capacity() { return super.capacity(); }
    public synchronized void ensureCapacity(int minimumCapacity) { super.ensureCapacity(minimumCapacity); }
    public synchronized void trimToSize() { super.trimToSize(); }
    public synchronized void setLength(int newLength) { super.setLength(newLength); }

    // ---- reading ----

    public synchronized int length() { return super.length(); }
    public synchronized char charAt(int index) { return super.charAt(index); }
    public synchronized void setCharAt(int index, char ch) { super.setCharAt(index, ch); }
    public synchronized void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) { super.getChars(srcBegin, srcEnd, dst, dstBegin); }
    public synchronized int codePointAt(int index) { return super.codePointAt(index); }
    public synchronized int codePointBefore(int index) { return super.codePointBefore(index); }
    public synchronized int codePointCount(int beginIndex, int endIndex) { return super.codePointCount(beginIndex, endIndex); }
    public synchronized int offsetByCodePoints(int index, int codePointOffset) { return super.offsetByCodePoints(index, codePointOffset); }

    // ---- chainable methods: synchronized covariant overrides typed StringBuffer ----

    public synchronized StringBuffer append(char c) { super.append(c); return this; }
    public synchronized StringBuffer append(String str) { super.append(str); return this; }
    public synchronized StringBuffer append(char[] str) { super.append(str); return this; }
    public synchronized StringBuffer append(char[] str, int offset, int len) { super.append(str, offset, len); return this; }
    public synchronized StringBuffer append(Object obj) { super.append(obj); return this; }
    public synchronized StringBuffer append(boolean b) { super.append(b); return this; }
    public synchronized StringBuffer append(CharSequence s) { super.append(s); return this; }
    public synchronized StringBuffer append(CharSequence s, int start, int end) { super.append(s, start, end); return this; }
    public synchronized StringBuffer append(StringBuffer sb) { super.append(sb); return this; }
    public synchronized StringBuffer append(int i) { super.append(i); return this; }
    public synchronized StringBuffer append(long l) { super.append(l); return this; }
    public synchronized StringBuffer append(float f) { super.append(f); return this; }
    public synchronized StringBuffer append(double d) { super.append(d); return this; }
    public synchronized StringBuffer appendCodePoint(int codePoint) { super.appendCodePoint(codePoint); return this; }

    public synchronized StringBuffer delete(int start, int end) { super.delete(start, end); return this; }
    public synchronized StringBuffer deleteCharAt(int index) { super.deleteCharAt(index); return this; }
    public synchronized StringBuffer replace(int start, int end, String str) { super.replace(start, end, str); return this; }

    // Los inserts que **tocan el buffer** van `synchronized`; los que sólo convierten su argumento y
    // delegan en uno de ésos (boolean/int/long/float/double/CharSequence) van como overrides
    // covariantes **sin** `synchronized` —el candado lo toma el sibling—, igual que el JDK. Todos
    // devuelven `StringBuffer` (covariante) para no romper el encadenado.
    public synchronized StringBuffer insert(int offset, String str) { super.insert(offset, str); return this; }
    public synchronized StringBuffer insert(int offset, char[] str) { super.insert(offset, str); return this; }
    public synchronized StringBuffer insert(int index, char[] str, int strOffset, int len) { super.insert(index, str, strOffset, len); return this; }
    public synchronized StringBuffer insert(int offset, Object obj) { super.insert(offset, obj); return this; }
    public synchronized StringBuffer insert(int dstOffset, CharSequence s, int start, int end) { super.insert(dstOffset, s, start, end); return this; }
    public synchronized StringBuffer insert(int offset, char c) { super.insert(offset, c); return this; }
    public StringBuffer insert(int offset, boolean b) { super.insert(offset, b); return this; }
    public StringBuffer insert(int offset, int i) { super.insert(offset, i); return this; }
    public StringBuffer insert(int offset, long l) { super.insert(offset, l); return this; }
    public StringBuffer insert(int offset, float f) { super.insert(offset, f); return this; }
    public StringBuffer insert(int offset, double d) { super.insert(offset, d); return this; }
    public StringBuffer insert(int dstOffset, CharSequence s) { super.insert(dstOffset, s); return this; }

    public synchronized StringBuffer repeat(int codePoint, int times) { super.repeat(codePoint, times); return this; }
    public synchronized StringBuffer repeat(CharSequence cs, int times) { super.repeat(cs, times); return this; }

    public synchronized StringBuffer reverse() { super.reverse(); return this; }

    // ---- searching / reading out ----

    // `indexOf(String)`/`lastIndexOf(String)` sin sincronizar: delegan en el `(String, int)` que sí
    // lo está. `chars()`/`codePoints()` tampoco (el JDK las hereda). Todo eso lo cubren los
    // forwarders de #268.
    public synchronized int indexOf(String str, int fromIndex) { return super.indexOf(str, fromIndex); }
    public synchronized int lastIndexOf(String str, int fromIndex) { return super.lastIndexOf(str, fromIndex); }
    public synchronized String substring(int start) { return super.substring(start); }
    public synchronized String substring(int start, int end) { return super.substring(start, end); }
    public synchronized CharSequence subSequence(int start, int end) { return super.subSequence(start, end); }
    public synchronized String toString() { return String.valueOf(this.value, 0, this.count); }

    /**
     * Compare two buffers lexicographically, by contents. Like {@link StringBuilder#compareTo}, this
     * is not {@code equals}: a mutable object has no stable hash code.
     *
     * @param another what to compare against
     */
    public synchronized int compareTo(StringBuffer another) {
        int mine = this.length();
        int theirs = another.length();
        int shorter = mine;
        if (theirs < shorter) {
            shorter = theirs;
        }
        int i = 0;
        while (i < shorter) {
            char a = this.charAt(i);
            char b = another.charAt(i);
            if (a != b) {
                return a - b;
            }
            i = i + 1;
        }
        return mine - theirs;
    }
}
