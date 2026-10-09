package java.lang;

import java.io.Serializable;

/**
 * KajiLibrary's java.lang.StringBuilder -- the mutable character buffer the compiler lowers string
 * concatenation to ({@code a + b} becomes
 * {@code new StringBuilder().append(a).append(b).toString()}).
 *
 * <p>The implementation lives in the shared, package-private {@link AbstractStringBuilder}; this
 * class adds the public identity, the constructors, and a <strong>covariant override</strong> of
 * each chainable method so the result is typed {@code StringBuilder} (each of those is exactly the
 * bridge the JDK synthesizes, and its body just forwards to the base and returns {@code this}). The
 * non-chainable methods it inherits ({@code length}, {@code charAt}, ...) are reached from outside
 * {@code java.lang} through the accessor forwarders {@code javac} synthesizes in this public class
 * for a public method of a package-private superclass (finding #268).
 */
public final class StringBuilder extends AbstractStringBuilder
        implements Serializable, Comparable<StringBuilder> {

    /** An empty builder with room for sixteen characters. */
    public StringBuilder() {
        super(16);
    }

    /**
     * An empty builder with room for {@code capacity} characters.
     *
     * @param capacity how much room to reserve
     * @throws NegativeArraySizeException if {@code capacity} is negative
     */
    public StringBuilder(int capacity) {
        super(capacity);
    }

    /**
     * A builder holding {@code str}, with sixteen characters of room to spare.
     *
     * @param str the initial contents
     */
    public StringBuilder(String str) {
        super(str.length() + 16);
        this.append(str);
    }

    /**
     * A builder holding {@code seq}, with sixteen characters of room to spare.
     *
     * @param seq the initial contents
     */
    public StringBuilder(CharSequence seq) {
        super(seq.length() + 16);
        this.append(seq);
    }

    // ---- chainable methods ----
    //
    // A covariant override of each: the result is typed `StringBuilder` (the JDK synthesizes the
    // very same bridge), and the body forwards to `AbstractStringBuilder` and returns `this`.

    public StringBuilder append(char c) { super.append(c); return this; }
    public StringBuilder append(String str) { super.append(str); return this; }
    public StringBuilder append(char[] str) { super.append(str); return this; }
    public StringBuilder append(char[] str, int offset, int len) { super.append(str, offset, len); return this; }
    public StringBuilder append(Object obj) { super.append(obj); return this; }
    public StringBuilder append(boolean b) { super.append(b); return this; }
    public StringBuilder append(CharSequence s) { super.append(s); return this; }
    public StringBuilder append(CharSequence s, int start, int end) { super.append(s, start, end); return this; }
    public StringBuilder append(StringBuffer sb) { super.append(sb); return this; }
    public StringBuilder append(int i) { super.append(i); return this; }
    public StringBuilder append(long l) { super.append(l); return this; }
    public StringBuilder append(float f) { super.append(f); return this; }
    public StringBuilder append(double d) { super.append(d); return this; }
    public StringBuilder appendCodePoint(int codePoint) { super.appendCodePoint(codePoint); return this; }

    public StringBuilder delete(int start, int end) { super.delete(start, end); return this; }
    public StringBuilder deleteCharAt(int index) { super.deleteCharAt(index); return this; }
    public StringBuilder replace(int start, int end, String str) { super.replace(start, end, str); return this; }

    public StringBuilder insert(int offset, String str) { super.insert(offset, str); return this; }
    public StringBuilder insert(int offset, char[] str) { super.insert(offset, str); return this; }
    public StringBuilder insert(int index, char[] str, int strOffset, int len) { super.insert(index, str, strOffset, len); return this; }
    public StringBuilder insert(int offset, Object obj) { super.insert(offset, obj); return this; }
    public StringBuilder insert(int dstOffset, CharSequence s) { super.insert(dstOffset, s); return this; }
    public StringBuilder insert(int dstOffset, CharSequence s, int start, int end) { super.insert(dstOffset, s, start, end); return this; }
    public StringBuilder insert(int offset, boolean b) { super.insert(offset, b); return this; }
    public StringBuilder insert(int offset, char c) { super.insert(offset, c); return this; }
    public StringBuilder insert(int offset, int i) { super.insert(offset, i); return this; }
    public StringBuilder insert(int offset, long l) { super.insert(offset, l); return this; }
    public StringBuilder insert(int offset, float f) { super.insert(offset, f); return this; }
    public StringBuilder insert(int offset, double d) { super.insert(offset, d); return this; }

    public StringBuilder repeat(int codePoint, int times) { super.repeat(codePoint, times); return this; }
    public StringBuilder repeat(CharSequence cs, int times) { super.repeat(cs, times); return this; }

    public StringBuilder reverse() { super.reverse(); return this; }

    /**
     * Compare two builders lexicographically, by contents.
     *
     * <p>Note what this is not: {@code equals}. Two builders holding the same characters compare
     * equal here and are still different objects, because {@code StringBuilder} deliberately does
     * NOT override {@code equals} -- a mutable object cannot have a stable hash code, so it would be
     * a trap in every hash-based collection.
     *
     * @param another what to compare against
     */
    public int compareTo(StringBuilder another) {
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

    /** The contents as a string. */
    public String toString() {
        return String.valueOf(this.value, 0, this.count);
    }
}
