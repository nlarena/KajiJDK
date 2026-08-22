package java.lang;

import java.util.Formatter;
import java.util.Locale;

// KajiLibrary's java.lang.String. The characters are laid out inline in the object by
// the VM (materialised by `ldc`, read back by native code), so the primitives that touch
// that storage are `native`; everything expressible on top of them is real Java.
//
// It orders lexicographically (Comparable) and is a CharSequence (charAt/length/subSequence),
// with toString() returning itself. (Compiling java.lang.String's own CharSequence/toString
// used to fail because the inherited String return bound to the external String, not the
// source one — that's finding #5, now fixed by source-core-type shadowing.)
public final class String implements Comparable<String>, CharSequence {

    // --- primitives backed by the VM's inline character storage ---

    public native int length();

    public native char charAt(int index);

    public native boolean equals(Object other);

    public native int hashCode();

    // Build a String from a slice of a char[] at run time. This is the one seam the VM
    // must provide (until now Strings only came from `ldc` at compile time): it lets
    // StringBuilder.toString() and substring produce fresh Strings.
    public static native String valueOf(char[] data, int offset, int count);

    // The text of any object. Native because it has to work for `null` too (a real call to
    // `toString()` would NPE), and because it is what a string concatenation lowers its
    // non-String operands to — the VM services it before any Java frame exists.
    public static native String valueOf(Object obj);

    // Prefix test. Native rather than a `charAt` loop: comparing the two inline character
    // stores directly is a single pass in the VM, and this is hot in name/descriptor matching.
    public native boolean startsWith(String prefix);

    // --- real Java, layered on the primitives above ---

    public boolean isEmpty() {
        return length() == 0;
    }

    public boolean isBlank() {
        int n = length();
        for (int i = 0; i < n; i++) {
            char c = charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }

    // The substring `[beginIndex, endIndex)`, built char by char through the native
    // valueOf(char[]) seam.
    public String substring(int beginIndex, int endIndex) {
        int len = endIndex - beginIndex;
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = charAt(beginIndex + i);
        }
        return String.valueOf(buf, 0, len);
    }

    // CharSequence view: a String IS its own char sequence.
    public CharSequence subSequence(int start, int end) {
        return this.substring(start, end);
    }

    public String toString() {
        return this;
    }

    // Lexicographic order by char value. Overriding Comparable.compareTo(T) synthesizes the
    // compareTo(Object) bridge.
    public int compareTo(String other) {
        int len1 = this.length();
        int len2 = other.length();
        int lim = len1 < len2 ? len1 : len2;
        for (int i = 0; i < lim; i++) {
            char c1 = this.charAt(i);
            char c2 = other.charAt(i);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    // Formats `args` per the printf-style `format` string (see java.util.Formatter). The
    // Locale-aware overload is H6-T5.
    public static String format(String format, Object... args) {
        // NOTE: a simple name (via import) — not `new java.util.Formatter()`. A qualified
        // name in a `new` is miscompiled to an empty body (compiler finding #20).
        return new Formatter().format(format, args).toString();
    }

    public static String format(Locale l, String format, Object... args) {
        return new Formatter(l).format(format, args).toString();
    }
}
