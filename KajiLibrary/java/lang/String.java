package java.lang;

import java.io.UnsupportedEncodingException;
import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Optional;
import java.util.Formatter;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * KajiLibrary's java.lang.String. The characters are laid out inline in the object by
 * the VM (materialised by `ldc`, read back by native code), so the primitives that touch
 * that storage are `native`; everything expressible on top of them is real Java.
 *
 * There are only FOUR seams into the VM -- rawLength, rawCharAt and the two rawValueOfs -- and
 * the whole rest of this class is built on them. That is deliberate: every method below can be
 * read, and wrong answers have one place to hide instead of a hundred native ones. They are all
 * PRIVATE, so the public surface is Java throughout, exactly as the JDK's is.
 *
 * It orders lexicographically (Comparable) and is a CharSequence (charAt/length/subSequence),
 * with toString() returning itself. (Compiling java.lang.String's own CharSequence/toString
 * used to fail because the inherited String return bound to the external String, not the
 * source one -- that's finding #5, now fixed by source-core-type shadowing.)
 *
 * @implNote A KajiLibrary subset, and the omissions are deliberate rather than pending:
 * - the fifteen constructors are all here, and none of them assigns anything: a String
 * cannot be filled in after it is allocated, so each one builds a separate string and
 * `publish`es it -- see the note above them.
 * - the varargs forms (`format`, `join`, `formatted`) are declared over arrays instead,
 * because `ACC_VARARGS` is never emitted (finding #118).
 */
public final class String implements Comparable<String>, CharSequence, ConstantDesc {

    /**
     * Case-insensitive order, as a shared comparator. Declared here because the JDK does, but
     * reading it from another class is emitted as a `getfield` over a static and crashes our
     * own VM (finding #110); `compareToIgnoreCase` is the same order without that hazard.
     */
    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveOrder();

    // --- the VM seams, and the public API built on them ---
    //
    // The characters live in storage the VM lays out inline, so SOMETHING here has to be native.
    // What is native is the private half: `rawLength`, `rawCharAt` and the two `rawValueOf`s.
    // Everything public is ordinary Java, which is not a cosmetic choice.
    //
    // It matters for fidelity: `String.length()` is not native in the JDK either, so declaring
    // ours `native` was a visible difference -- `Method.getModifiers()` answers differently, and
    // the API gate counted seven of these.
    //
    // And it matters for correctness, which is the part worth dwelling on. `equals`, `hashCode`
    // and `startsWith` do not need the VM at all: they are loops over `charAt`. Having them
    // native meant they read the storage in the VM's own terms, and `hashCode` got it wrong --
    // it folded the UTF-8 BYTES instead of the UTF-16 code units, so every non-ASCII string
    // hashed to something the JDK never would (U+00F1 gave 6222 where the answer is 241), and
    // every HashMap keyed by such a string was quietly broken. Written in Java over `charAt`,
    // the specified formula is the only thing it can compute.

    /** The number of {@code char}s -- UTF-16 code units, so a supplementary character counts 2. */
    public int length() {
        return String.rawLength(this);
    }

    // The one irreducible read: the characters are not in any field this class can name.
    private static native int rawLength(String s);

    /**
     * The {@code char} at {@code index}.
     *
     * @param index a position in {@code [0, length())}
     * @throws StringIndexOutOfBoundsException if it is not
     */
    public char charAt(int index) {
        if (index < 0 || index >= this.length()) {
            throw new StringIndexOutOfBoundsException("index " + index
                    + ", length " + this.length());
        }
        return String.rawCharAt(this, index);
    }

    private static native char rawCharAt(String s, int index);

    /**
     * Equal when {@code other} is a String of the same characters.
     *
     * @param other the object to compare against
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof String)) {
            return false;
        }
        String that = (String) other;
        int n = this.length();
        if (that.length() != n) {
            return false;
        }
        int i = 0;
        while (i < n) {
            if (String.rawCharAt(this, i) != String.rawCharAt(that, i)) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * The hash the specification fixes: {@code s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]}.
     *
     * <p>Written out rather than cached, and fixed by the spec rather than chosen -- a String
     * hash that differs from the JDK's is not merely a different hash, it is a different answer
     * from every serialized structure that ever recorded one.
     */
    public int hashCode() {
        int h = 0;
        int n = this.length();
        int i = 0;
        while (i < n) {
            h = h * 31 + String.rawCharAt(this, i);
            i = i + 1;
        }
        return h;
    }

    /**
     * Whether this string begins with {@code prefix}.
     *
     * @param prefix the prefix to look for
     */
    public boolean startsWith(String prefix) {
        int n = prefix.length();
        if (n > this.length()) {
            return false;
        }
        int i = 0;
        while (i < n) {
            if (String.rawCharAt(this, i) != String.rawCharAt(prefix, i)) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /**
     * A string holding {@code count} characters of {@code data} from {@code offset}.
     *
     * @param data the characters
     * @param offset where to start
     * @param count how many to take
     */
    public static String valueOf(char[] data, int offset, int count) {
        return String.rawValueOf(data, offset, count);
    }

    // The other irreducible seam: making a String at run time. Until this existed, a String could
    // only come from `ldc` -- that is, only from a literal the compiler had already seen.
    private static native String rawValueOf(char[] data, int offset, int count);

    /**
     * The text of any object, or {@code "null"}.
     *
     * @param obj the object to describe; may be null
     */
    public static String valueOf(Object obj) {
        return String.rawValueOfObject(obj);
    }

    // Native because it has to work for `null` too (a real `toString()` call would throw), and
    // because it is what a string concatenation lowers its non-String operands to -- the VM
    // services it before any Java frame exists.
    private static native String rawValueOfObject(Object obj);

    // A string LITERAL written inside String's own source has the type of the EXTERNAL
    // java.lang.String on the classpath, not of the class being compiled, so it cannot be
    // returned or assigned where this String is expected (the tail of finding #5). Laundering
    // it through valueOf(Object) -- which is declared here and therefore returns THIS type --
    // is the whole workaround.
    private static String lit(Object text) {
        return String.rawValueOfObject(text);
    }

    // --- constructors ---
    //
    // All fifteen of them, and every one ends in `publish` instead of assigning to a field.
    //
    // A String keeps its characters in storage the VM lays out inline, sized at the moment the
    // object is allocated. The `new` opcode sizes an instance from its declared fields, and this
    // class declares none, so the object a constructor is handed has room for zero characters --
    // and a heap block cannot be grown in place. There is nothing to assign to and no way to
    // reassign `this`.
    //
    // So a constructor here builds a SEPARATE string and publishes it. The `return` that ends the
    // constructor rewrites the caller's references from the object it was handed to the one it
    // built, which is sound because the verifier only lets an uninitialised reference live on
    // that frame's operand stack or in its locals -- never in a field, an array, or another
    // frame. See `Frame::published` and `Exec::string_publish` on the VM side.
    //
    // What this buys is that the conversions stay HERE, readable, instead of being reimplemented
    // in the interpreter: `new String(bytes, charset)` decodes through java.nio.charset, the same
    // code any other caller would reach.

    /**
     * Hands the string this constructor built back to the caller.
     *
     * <p>Native because it is not a computation at all -- it is the VM instruction that makes a
     * constructor of this class mean anything. See the note above.
     *
     * @param built the string to hand back
     */
    private static native void publish(String built);

    /**
     * The empty string.
     *
     * <p>Equivalent to {@code ""} and worse than it in every way: the literal is one shared
     * object, this allocates. The JDK says the same in its own Javadoc.
     */
    public String() {
        String.publish(String.valueOf(new char[0], 0, 0));
    }

    /**
     * A string holding a copy of {@code value}.
     *
     * <p>A copy, not a view: mutating the array afterwards does not change the string. That is
     * what makes strings safe to share, and it is why this is not free.
     *
     * @param value the characters
     */
    public String(char[] value) {
        String.publish(String.copyOf(value, 0, value.length));
    }

    /**
     * A string holding a copy of {@code count} characters of {@code value} from {@code offset}.
     *
     * @param value the characters
     * @param offset where to start
     * @param count how many to take
     * @throws StringIndexOutOfBoundsException if the range falls outside the array
     */
    public String(char[] value, int offset, int count) {
        String.publish(String.copyOf(value, offset, count));
    }

    /**
     * A string of {@code count} code points of {@code codePoints} from {@code offset}.
     *
     * <p>Code points, not characters: anything above U+FFFF arrives as one {@code int} here and
     * becomes two {@code char}s in the string, so the result can be longer than {@code count}.
     *
     * @param codePoints the code points
     * @param offset where to start
     * @param count how many to take
     * @throws IllegalArgumentException if any of them is not a valid code point
     * @throws StringIndexOutOfBoundsException if the range falls outside the array
     */
    public String(int[] codePoints, int offset, int count) {
        String.publish(String.fromCodePoints(codePoints, offset, count));
    }

    /**
     * A copy of {@code original}.
     *
     * <p>Of no use, and that is not a criticism of this implementation -- a string is immutable,
     * so a copy of one can never differ from it. The JDK carries this constructor for the same
     * reason: it is API, and something out there calls it.
     *
     * @param original the string to copy
     */
    public String(String original) {
        if (original == null) {
            throw new NullPointerException();
        }
        // A **copy**, not the original. `publish` makes the constructor's result be the object
        // handed to it, so publishing `original` would give `new String(s)` the identity of `s` —
        // and with a literal that means `new String("a") == "a"` answers true, which JLS 3.10.5
        // requires to be false. It is the half of interning that is about *not* sharing.
        String.publish(String.valueOf(original.toCharArray(), 0, original.length()));
    }

    /**
     * A snapshot of {@code buffer} as it is right now.
     *
     * @param buffer the buffer to read
     */
    public String(StringBuffer buffer) {
        if (buffer == null) {
            throw new NullPointerException();
        }
        String.publish(String.lit(buffer));
    }

    /**
     * A snapshot of {@code builder} as it is right now.
     *
     * @param builder the builder to read
     */
    public String(StringBuilder builder) {
        if (builder == null) {
            throw new NullPointerException();
        }
        String.publish(String.lit(builder));
    }

    /**
     * A string built by putting {@code hibyte} on top of each byte of {@code ascii}.
     *
     * @param ascii the low bytes
     * @param hibyte the high byte of every resulting character
     * @deprecated it predates charsets and cannot express anything a charset would. Use a
     *             constructor that names one.
     */
    @Deprecated(since = "1.1")
    public String(byte[] ascii, int hibyte) {
        String.publish(String.fromHibyte(ascii, hibyte, 0, ascii.length));
    }

    /**
     * As {@link #String(byte[], int)}, over a slice.
     *
     * @param ascii the low bytes
     * @param hibyte the high byte of every resulting character
     * @param offset where to start
     * @param count how many to take
     * @deprecated see {@link #String(byte[], int)}
     */
    @Deprecated(since = "1.1")
    public String(byte[] ascii, int hibyte, int offset, int count) {
        String.publish(String.fromHibyte(ascii, hibyte, offset, count));
    }

    /**
     * The bytes decoded with the default charset, which is UTF-8.
     *
     * <p>Anything the charset cannot make sense of becomes U+FFFD rather than an error, so this
     * never throws for bad input. That is the documented behaviour and it is worth knowing: a
     * file read with the wrong charset produces a string, not a complaint.
     *
     * @param bytes the bytes to decode
     */
    public String(byte[] bytes) {
        String.publish(String.decode(bytes, 0, bytes.length, Charset.defaultCharset()));
    }

    /**
     * A slice of {@code bytes} decoded with the default charset, which is UTF-8.
     *
     * @param bytes the bytes to decode
     * @param offset where to start
     * @param length how many bytes to take
     * @throws StringIndexOutOfBoundsException if the range falls outside the array
     */
    public String(byte[] bytes, int offset, int length) {
        String.publish(String.decode(bytes, offset, length, Charset.defaultCharset()));
    }

    /**
     * The bytes decoded with {@code charset}, replacing anything malformed.
     *
     * @param bytes the bytes to decode
     * @param charset the charset to decode with
     */
    public String(byte[] bytes, Charset charset) {
        String.publish(String.decode(bytes, 0, bytes.length, charset));
    }

    /**
     * A slice of {@code bytes} decoded with {@code charset}, replacing anything malformed.
     *
     * @param bytes the bytes to decode
     * @param offset where to start
     * @param length how many bytes to take
     * @param charset the charset to decode with
     * @throws StringIndexOutOfBoundsException if the range falls outside the array
     */
    public String(byte[] bytes, int offset, int length, Charset charset) {
        String.publish(String.decode(bytes, offset, length, charset));
    }

    /**
     * The bytes decoded with the named charset.
     *
     * @param bytes the bytes to decode
     * @param charsetName a canonical name or an alias
     * @throws UnsupportedEncodingException if no charset answers to that name
     */
    public String(byte[] bytes, String charsetName) throws UnsupportedEncodingException {
        Charset charset = String.charsetFor(charsetName);
        String.publish(String.decode(bytes, 0, bytes.length, charset));
    }

    /**
     * A slice of {@code bytes} decoded with the named charset.
     *
     * @param bytes the bytes to decode
     * @param offset where to start
     * @param length how many bytes to take
     * @param charsetName a canonical name or an alias
     * @throws UnsupportedEncodingException if no charset answers to that name
     * @throws StringIndexOutOfBoundsException if the range falls outside the array
     */
    public String(byte[] bytes, int offset, int length, String charsetName)
            throws UnsupportedEncodingException {
        Charset charset = String.charsetFor(charsetName);
        String.publish(String.decode(bytes, offset, length, charset));
    }

    // The conversions the constructors above delegate to. Kept separate from them for a reason
    // that is not style: a constructor body has to end in `publish`, so anything it computes has
    // to be computable in one expression -- and these are not.

    private static String copyOf(char[] value, int offset, int count) {
        String.checkRange(offset, count, value.length);
        return String.valueOf(value, offset, count);
    }

    private static String fromCodePoints(int[] codePoints, int offset, int count) {
        String.checkRange(offset, count, codePoints.length);
        // Sized for the worst case -- every code point supplementary, so two chars each -- and
        // then trimmed by the `put` that valueOf is given. Counting first would mean walking the
        // array twice to save at most `count` chars that are about to be copied anyway.
        char[] out = new char[count * 2];
        int put = 0;
        int i = 0;
        while (i < count) {
            int cp = codePoints[offset + i];
            if (!Character.isValidCodePoint(cp)) {
                throw new IllegalArgumentException("Not a valid code point: " + cp);
            }
            if (cp > 0xffff) {
                out[put] = Character.highSurrogate(cp);
                out[put + 1] = Character.lowSurrogate(cp);
                put = put + 2;
            } else {
                out[put] = (char) cp;
                put = put + 1;
            }
            i = i + 1;
        }
        return String.valueOf(out, 0, put);
    }

    private static String fromHibyte(byte[] ascii, int hibyte, int offset, int count) {
        String.checkRange(offset, count, ascii.length);
        // Only the low eight bits of `hibyte` are used, which is the documented rule and not a
        // simplification: the argument is an int purely because the language had no byte literal
        // worth using when this was designed.
        int high = (hibyte & 0xff) << 8;
        char[] out = new char[count];
        int i = 0;
        while (i < count) {
            out[i] = (char) (high | (ascii[offset + i] & 0xff));
            i = i + 1;
        }
        return String.valueOf(out, 0, count);
    }

    private static String decode(byte[] bytes, int offset, int length, Charset charset) {
        if (charset == null) {
            throw new NullPointerException();
        }
        String.checkRange(offset, length, bytes.length);
        ByteBuffer in = ByteBuffer.wrap(bytes, offset, length);
        CharBuffer out = charset.decode(in);
        // Through `lit`, because `CharBuffer.toString()` is declared over the java.lang.String of
        // the classpath and not over the class being compiled (the tail of finding #5).
        return String.lit(out);
    }

    private static void checkRange(int offset, int count, int length) {
        if (offset < 0 || count < 0 || offset > length - count) {
            throw new StringIndexOutOfBoundsException("offset " + offset + ", count " + count
                    + ", length " + length);
        }
    }

    // --- emptiness ---

    public boolean isEmpty() {
        return this.length() == 0;
    }

    /** Empty, or nothing but whitespace. */
    public boolean isBlank() {
        int n = this.length();
        for (int i = 0; i < n; i++) {
            if (!String.isSpace(this.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // What counts as whitespace for strip/isBlank: the Unicode definition, not the ASCII one.
    // trim() deliberately uses a different and older rule -- see there.
    private static boolean isSpace(char c) {
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 0x0b) {
            return true;
        }
        if (c == 0x1c || c == 0x1d || c == 0x1e || c == 0x1f) {
            return true;
        }
        if (c == 0x1680 || (c >= 0x2000 && c <= 0x200a)) {
            return true;
        }
        return c == 0x2028 || c == 0x2029 || c == 0x205f || c == 0x3000;
    }

    // --- slicing ---

    /** From {@code beginIndex} to the end. */
    public String substring(int beginIndex) {
        return this.substring(beginIndex, this.length());
    }

    /**
     * The substring `[beginIndex, endIndex)`, built char by char through the native
     * valueOf(char[]) seam.
     */
    public String substring(int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > this.length() || beginIndex > endIndex) {
            throw new StringIndexOutOfBoundsException("begin " + beginIndex + ", end " + endIndex
                    + ", length " + this.length());
        }
        int len = endIndex - beginIndex;
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = this.charAt(beginIndex + i);
        }
        return String.valueOf(buf, 0, len);
    }

    /**
     * CharSequence view: a String IS its own char sequence.
     */
    public CharSequence subSequence(int start, int end) {
        return this.substring(start, end);
    }

    public String toString() {
        return this;
    }

    /** This string followed by {@code str}. */
    public String concat(String str) {
        int a = this.length();
        int b = str.length();
        if (b == 0) {
            return this;
        }
        char[] buf = new char[a + b];
        for (int i = 0; i < a; i++) {
            buf[i] = this.charAt(i);
        }
        for (int i = 0; i < b; i++) {
            buf[a + i] = str.charAt(i);
        }
        return String.valueOf(buf, 0, a + b);
    }

    /** This string {@code count} times over. */
    public String repeat(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        int n = this.length();
        if (count == 0 || n == 0) {
            return String.lit("");
        }
        char[] buf = new char[n * count];
        int at = 0;
        for (int c = 0; c < count; c++) {
            for (int i = 0; i < n; i++) {
                buf[at] = this.charAt(i);
                at = at + 1;
            }
        }
        return String.valueOf(buf, 0, buf.length);
    }

    // --- characters out ---

    public char[] toCharArray() {
        int n = this.length();
        char[] buf = new char[n];
        for (int i = 0; i < n; i++) {
            buf[i] = this.charAt(i);
        }
        return buf;
    }

    /** Copies {@code [srcBegin, srcEnd)} into {@code dst} starting at {@code dstBegin}. */
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcEnd > this.length() || srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException("begin " + srcBegin + ", end " + srcEnd
                    + ", length " + this.length());
        }
        int n = srcEnd - srcBegin;
        for (int i = 0; i < n; i++) {
            dst[dstBegin + i] = this.charAt(srcBegin + i);
        }
    }

    // --- code points ---
    //
    // A char is 16 bits and Unicode is not, so anything above U+FFFF is stored as a SURROGATE
    // PAIR: a high surrogate followed by a low one. The four methods below are the whole of
    // that story -- read a code point forwards, read one backwards, count them, and step by
    // them -- and everything else in this class works in chars, which is what the JDK does too.

    private static boolean isHigh(char c) {
        return c >= 0xd800 && c <= 0xdbff;
    }

    private static boolean isLow(char c) {
        return c >= 0xdc00 && c <= 0xdfff;
    }

    private static int combine(char high, char low) {
        return 0x10000 + ((high - 0xd800) << 10) + (low - 0xdc00);
    }

    /** The code point starting at {@code index}. */
    public int codePointAt(int index) {
        int n = this.length();
        if (index < 0 || index >= n) {
            throw new StringIndexOutOfBoundsException("index " + index + ", length " + n);
        }
        char c = this.charAt(index);
        if (String.isHigh(c) && index + 1 < n) {
            char next = this.charAt(index + 1);
            if (String.isLow(next)) {
                return String.combine(c, next);
            }
        }
        return c;
    }

    /** The code point ENDING at {@code index}, that is the one just before it. */
    public int codePointBefore(int index) {
        int n = this.length();
        if (index < 1 || index > n) {
            throw new StringIndexOutOfBoundsException("index " + index + ", length " + n);
        }
        char c = this.charAt(index - 1);
        if (String.isLow(c) && index - 2 >= 0) {
            char prev = this.charAt(index - 2);
            if (String.isHigh(prev)) {
                return String.combine(prev, c);
            }
        }
        return c;
    }

    /** How many code points {@code [beginIndex, endIndex)} holds — pairs count as one. */
    public int codePointCount(int beginIndex, int endIndex) {
        int n = this.length();
        if (beginIndex < 0 || endIndex > n || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException("begin " + beginIndex + ", end " + endIndex);
        }
        int count = 0;
        int i = beginIndex;
        while (i < endIndex) {
            char c = this.charAt(i);
            if (String.isHigh(c) && i + 1 < endIndex && String.isLow(this.charAt(i + 1))) {
                i = i + 2;
            } else {
                i = i + 1;
            }
            count = count + 1;
        }
        return count;
    }

    /** The char index {@code codePointOffset} code points away from {@code index}. */
    public int offsetByCodePoints(int index, int codePointOffset) {
        int n = this.length();
        if (index < 0 || index > n) {
            throw new IndexOutOfBoundsException("index " + index + ", length " + n);
        }
        int at = index;
        if (codePointOffset >= 0) {
            for (int k = 0; k < codePointOffset; k++) {
                if (at >= n) {
                    throw new IndexOutOfBoundsException("past the end");
                }
                char c = this.charAt(at);
                if (String.isHigh(c) && at + 1 < n && String.isLow(this.charAt(at + 1))) {
                    at = at + 2;
                } else {
                    at = at + 1;
                }
            }
        } else {
            for (int k = codePointOffset; k < 0; k++) {
                if (at <= 0) {
                    throw new IndexOutOfBoundsException("before the start");
                }
                char c = this.charAt(at - 1);
                if (String.isLow(c) && at - 2 >= 0 && String.isHigh(this.charAt(at - 2))) {
                    at = at - 2;
                } else {
                    at = at - 1;
                }
            }
        }
        return at;
    }

    // --- comparison ---

    /**
     * Lexicographic order by char value. Overriding Comparable.compareTo(T) synthesizes the
     * compareTo(Object) bridge.
     */
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

    /** The same order, ignoring case. */
    public int compareToIgnoreCase(String other) {
        int len1 = this.length();
        int len2 = other.length();
        int lim = len1 < len2 ? len1 : len2;
        for (int i = 0; i < lim; i++) {
            char c1 = String.fold(this.charAt(i));
            char c2 = String.fold(other.charAt(i));
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    // Case folding for COMPARISON only, over ASCII. It is not a case conversion and does not
    // pretend to be one: `toLowerCase` is absent from this class precisely because doing it
    // properly needs Unicode tables that java.lang.Character does not have yet. Comparing is
    // the weaker job, and ASCII covers what the library itself compares -- names, descriptors,
    // encodings.
    private static char fold(char c) {
        if (c >= 'A' && c <= 'Z') {
            return (char) (c + 32);
        }
        return c;
    }

    public boolean equalsIgnoreCase(String other) {
        if (other == null) {
            return false;
        }
        int n = this.length();
        if (other.length() != n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (String.fold(this.charAt(i)) != String.fold(other.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Whether the chars at {@code toffset} match {@code other}'s at {@code ooffset}. */
    public boolean regionMatches(int toffset, String other, int ooffset, int len) {
        return this.matchesRegion(toffset, other, ooffset, len, false);
    }

    /** The same, optionally ignoring case. */
    public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset,
            int len) {
        return this.matchesRegion(toffset, other, ooffset, len, ignoreCase);
    }

    // Out of range is FALSE and not an exception, unlike substring: the question "do these
    // regions match" has an answer when one of them does not exist, and the answer is no.
    private boolean matchesRegion(int toffset, String other, int ooffset, int len,
            boolean ignoreCase) {
        if (toffset < 0 || ooffset < 0 || len < 0) {
            return false;
        }
        if (toffset + len > this.length() || ooffset + len > other.length()) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            char a = this.charAt(toffset + i);
            char b = other.charAt(ooffset + i);
            if (a != b) {
                if (!ignoreCase) {
                    return false;
                }
                if (String.fold(a) != String.fold(b)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean startsWith(String prefix, int toffset) {
        return this.matchesRegion(toffset, prefix, 0, prefix.length(), false);
    }

    public boolean endsWith(String suffix) {
        return this.matchesRegion(this.length() - suffix.length(), suffix, 0, suffix.length(),
                false);
    }

    /** Whether the same characters appear, whatever the other sequence's class. */
    public boolean contentEquals(CharSequence cs) {
        if (cs == null) {
            return false;
        }
        int n = this.length();
        if (cs.length() != n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (this.charAt(i) != cs.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean contentEquals(StringBuffer sb) {
        CharSequence cs = sb;
        return this.contentEquals(cs);
    }

    // --- searching ---

    public int indexOf(int ch) {
        return this.indexOf(ch, 0);
    }

    public int indexOf(int ch, int fromIndex) {
        return this.indexOf(ch, fromIndex, this.length());
    }

    /** The first occurrence of the code point {@code ch} in {@code [fromIndex, endIndex)}. */
    public int indexOf(int ch, int fromIndex, int endIndex) {
        int n = this.length();
        int from = fromIndex < 0 ? 0 : fromIndex;
        int to = endIndex > n ? n : endIndex;
        if (ch < 0x10000) {
            for (int i = from; i < to; i++) {
                if (this.charAt(i) == (char) ch) {
                    return i;
                }
            }
            return -1;
        }
        // Above the BMP the needle is a surrogate pair, so it is two chars wide.
        char high = (char) (0xd800 + ((ch - 0x10000) >> 10));
        char low = (char) (0xdc00 + ((ch - 0x10000) & 0x3ff));
        for (int i = from; i + 1 < to; i++) {
            if (this.charAt(i) == high && this.charAt(i + 1) == low) {
                return i;
            }
        }
        return -1;
    }

    public int indexOf(String str) {
        return this.indexOf(str, 0);
    }

    public int indexOf(String str, int fromIndex) {
        return this.indexOf(str, fromIndex, this.length());
    }

    /**
     * The first occurrence of {@code str} that ends at or before {@code endIndex}.
     *
     * <p>Plain scanning, not Boyer-Moore: the patterns a library searches for are short, and a
     * clear loop that is right beats a clever one that has to be trusted.
     */
    public int indexOf(String str, int fromIndex, int endIndex) {
        int n = this.length();
        int m = str.length();
        int from = fromIndex < 0 ? 0 : fromIndex;
        int to = endIndex > n ? n : endIndex;
        if (m == 0) {
            return from > to ? -1 : from;
        }
        for (int i = from; i + m <= to; i++) {
            if (this.matchesRegion(i, str, 0, m, false)) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(int ch) {
        return this.lastIndexOf(ch, this.length() - 1);
    }

    /** The last occurrence of {@code ch} at or before {@code fromIndex}. */
    public int lastIndexOf(int ch, int fromIndex) {
        int n = this.length();
        int from = fromIndex >= n ? n - 1 : fromIndex;
        if (ch < 0x10000) {
            for (int i = from; i >= 0; i--) {
                if (this.charAt(i) == (char) ch) {
                    return i;
                }
            }
            return -1;
        }
        char high = (char) (0xd800 + ((ch - 0x10000) >> 10));
        char low = (char) (0xdc00 + ((ch - 0x10000) & 0x3ff));
        for (int i = from; i >= 0; i--) {
            if (i + 1 < n && this.charAt(i) == high && this.charAt(i + 1) == low) {
                return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(String str) {
        return this.lastIndexOf(str, this.length());
    }

    /** The last occurrence of {@code str} that STARTS at or before {@code fromIndex}. */
    public int lastIndexOf(String str, int fromIndex) {
        int n = this.length();
        int m = str.length();
        int from = fromIndex > n - m ? n - m : fromIndex;
        if (m == 0) {
            return from < 0 ? -1 : from;
        }
        for (int i = from; i >= 0; i--) {
            if (this.matchesRegion(i, str, 0, m, false)) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(CharSequence s) {
        return this.indexOf(s.toString()) >= 0;
    }

    // --- rewriting ---

    /** Every {@code oldChar} replaced by {@code newChar}. */
    public String replace(char oldChar, char newChar) {
        if (oldChar == newChar) {
            return this;
        }
        int n = this.length();
        char[] buf = new char[n];
        boolean touched = false;
        for (int i = 0; i < n; i++) {
            char c = this.charAt(i);
            if (c == oldChar) {
                buf[i] = newChar;
                touched = true;
            } else {
                buf[i] = c;
            }
        }
        if (!touched) {
            return this;
        }
        return String.valueOf(buf, 0, n);
    }

    /**
     * Every literal occurrence of {@code target} replaced by {@code replacement}.
     *
     * <p>Literal, not a pattern: this is the method to reach for when the text is data rather
     * than a regular expression, and it is why it exists next to {@code replaceAll}.
     */
    public String replace(CharSequence target, CharSequence replacement) {
        String from = target.toString();
        String to = replacement.toString();
        int m = from.length();
        if (m == 0) {
            // The empty target matches between every pair of chars, and before and after.
            String out = to;
            int n = this.length();
            for (int i = 0; i < n; i++) {
                out = out.concat(this.substring(i, i + 1)).concat(to);
            }
            return out;
        }
        String out = String.lit("");
        int at = 0;
        while (at <= this.length() - m) {
            int hit = this.indexOf(from, at);
            if (hit < 0) {
                break;
            }
            out = out.concat(this.substring(at, hit)).concat(to);
            at = hit + m;
        }
        return out.concat(this.substring(at));
    }

    // --- trimming ---

    /**
     * Both ends cut of everything at or below U+0020.
     *
     * <p>Older and blunter than {@link #strip}: it treats every control character as trimmable
     * and no non-ASCII space as such. Both are kept because changing what {@code trim} removes
     * would silently change what existing code parses.
     */
    public String trim() {
        int n = this.length();
        int start = 0;
        while (start < n && this.charAt(start) <= ' ') {
            start = start + 1;
        }
        int end = n;
        while (end > start && this.charAt(end - 1) <= ' ') {
            end = end - 1;
        }
        if (start == 0 && end == n) {
            return this;
        }
        return this.substring(start, end);
    }

    /** Both ends cut of Unicode whitespace. */
    public String strip() {
        return this.stripLeading().stripTrailing();
    }

    public String stripLeading() {
        int n = this.length();
        int start = 0;
        while (start < n && String.isSpace(this.charAt(start))) {
            start = start + 1;
        }
        if (start == 0) {
            return this;
        }
        return this.substring(start, n);
    }

    public String stripTrailing() {
        int n = this.length();
        int end = n;
        while (end > 0 && String.isSpace(this.charAt(end - 1))) {
            end = end - 1;
        }
        if (end == n) {
            return this;
        }
        return this.substring(0, end);
    }

    // --- line-oriented, for text blocks ---

    /**
     * Every line indented by {@code n} spaces, or outdented if {@code n} is negative, and
     * normalised to end in a newline.
     */
    public String indent(int n) {
        if (this.isEmpty()) {
            return String.lit("");
        }
        String pad = String.lit("");
        if (n > 0) {
            pad = String.lit(" ").repeat(n);
        }
        String[] parts = this.splitLines();
        String out = String.lit("");
        for (int i = 0; i < parts.length; i++) {
            String line = parts[i];
            if (n < 0) {
                int drop = 0;
                int limit = -n;
                while (drop < limit && drop < line.length()
                        && String.isSpace(line.charAt(drop))) {
                    drop = drop + 1;
                }
                line = line.substring(drop);
            } else {
                line = pad.concat(line);
            }
            out = out.concat(line).concat(String.lit("\n"));
        }
        return out;
    }

    /**
     * The common leading whitespace removed from every line — what the compiler does to a text
     * block, exposed so a string built at run time can be given the same treatment.
     */
    public String stripIndent() {
        if (this.isEmpty()) {
            return String.lit("");
        }
        int n = this.length();
        boolean endsTerm = this.charAt(n - 1) == 0x0a || this.charAt(n - 1) == 0x0d;
        String[] parts = this.splitLines();
        int common = 2147483647;
        for (int i = 0; i < parts.length; i++) {
            String line = parts[i];
            boolean last = i == parts.length - 1 && !endsTerm;
            int lead = 0;
            while (lead < line.length() && String.isSpace(line.charAt(lead))) {
                lead = lead + 1;
            }
            if (line.isBlank()) {
                // A blank line says nothing about the indentation -- unless it is the LAST one,
                // where the rule counts its length instead of its leading space.
                if (last && line.length() < common) {
                    common = line.length();
                }
                continue;
            }
            if (lead < common) {
                common = lead;
            }
        }
        if (endsTerm) {
            // A trailing terminator means there IS a last line, an empty one, and its length is
            // zero -- so nothing can be stripped. Surprising, and it is what the JDK does.
            common = 0;
        }
        if (common == 2147483647) {
            common = 0;
        }
        String out = String.lit("");
        for (int i = 0; i < parts.length; i++) {
            String line = parts[i];
            if (line.length() >= common) {
                line = line.substring(common);
            } else {
                line = String.lit("");
            }
            line = line.stripTrailing();
            out = out.concat(line);
            if (i < parts.length - 1 || endsTerm) {
                out = out.concat(String.lit("\n"));
            }
        }
        return out;
    }

    /**
     * The escape sequences a Java source file would understand, translated.
     *
     * @throws IllegalArgumentException on an escape that is not one
     */
    public String translateEscapes() {
        int n = this.length();
        char[] buf = new char[n];
        int put = 0;
        int i = 0;
        while (i < n) {
            char c = this.charAt(i);
            if (c != '\\') {
                buf[put] = c;
                put = put + 1;
                i = i + 1;
                continue;
            }
            i = i + 1;
            if (i >= n) {
                throw new IllegalArgumentException("dangling escape at the end");
            }
            char e = this.charAt(i);
            i = i + 1;
            if (e == 'b') {
                buf[put] = '\b';
            } else if (e == 't') {
                buf[put] = '\t';
            } else if (e == 'n') {
                buf[put] = '\n';
            } else if (e == 'f') {
                buf[put] = '\f';
            } else if (e == 'r') {
                buf[put] = '\r';
            } else if (e == 's') {
                buf[put] = ' ';
            } else if (e == '"' || e == '\'' || e == '\\') {
                buf[put] = e;
            } else if (e >= '0' && e <= '7') {
                // An octal escape is one to three digits, and never above 0377.
                int value = e - '0';
                int digits = 1;
                int cap = e <= '3' ? 3 : 2;
                while (digits < cap && i < n) {
                    char d = this.charAt(i);
                    if (d < '0' || d > '7') {
                        break;
                    }
                    value = value * 8 + (d - '0');
                    i = i + 1;
                    digits = digits + 1;
                }
                buf[put] = (char) value;
            } else if (e == '\n') {
                // A line continuation: the newline disappears and nothing is emitted.
                continue;
            } else {
                throw new IllegalArgumentException("invalid escape: \\" + e);
            }
            put = put + 1;
        }
        return String.valueOf(buf, 0, put);
    }

    // Splits on line terminators, keeping a trailing empty line only when the text ends in one.
    // Shared by indent/stripIndent; `lines()` is the public, stream-shaped form.
    private String[] splitLines() {
        int n = this.length();
        int count = 0;
        int i = 0;
        while (i < n) {
            char c = this.charAt(i);
            if (c == '\n') {
                count = count + 1;
                i = i + 1;
            } else if (c == '\r') {
                count = count + 1;
                i = i + 1;
                if (i < n && this.charAt(i) == '\n') {
                    i = i + 1;
                }
            } else {
                i = i + 1;
            }
        }
        boolean trailing = n > 0 && (this.charAt(n - 1) == '\n' || this.charAt(n - 1) == '\r');
        int total = trailing ? count : count + 1;
        String[] out = new String[total];
        int put = 0;
        int start = 0;
        i = 0;
        while (i < n && put < total) {
            char c = this.charAt(i);
            if (c == '\n' || c == '\r') {
                out[put] = this.substring(start, i);
                put = put + 1;
                i = i + 1;
                if (c == '\r' && i < n && this.charAt(i) == '\n') {
                    i = i + 1;
                }
                start = i;
            } else {
                i = i + 1;
            }
        }
        if (put < total) {
            out[put] = this.substring(start, n);
        }
        return out;
    }

    // --- building strings from other things ---

    public static String valueOf(boolean b) {
        return b ? String.lit("true") : String.lit("false");
    }

    public static String valueOf(char c) {
        char[] buf = new char[1];
        buf[0] = c;
        return String.valueOf(buf, 0, 1);
    }

    public static String valueOf(char[] data) {
        return String.valueOf(data, 0, data.length);
    }

    public static String valueOf(int i) {
        return Integer.toString(i);
    }

    public static String valueOf(long l) {
        return Long.toString(l);
    }

    public static String valueOf(float f) {
        return Float.toString(f);
    }

    public static String valueOf(double d) {
        return Double.toString(d);
    }

    /** A copy of {@code data}. Identical to {@link #valueOf(char[])}, and older. */
    public static String copyValueOf(char[] data) {
        return String.valueOf(data, 0, data.length);
    }

    public static String copyValueOf(char[] data, int offset, int count) {
        return String.valueOf(data, offset, count);
    }

    /** The elements with {@code delimiter} between them. */
    public static String join(CharSequence delimiter, CharSequence... elements) {
        String sep = delimiter.toString();
        String out = String.lit("");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) {
                out = out.concat(sep);
            }
            out = out.concat(String.valueOf(elements[i]));
        }
        return out;
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        String sep = delimiter.toString();
        String out = String.lit("");
        boolean first = true;
        java.util.Iterator<? extends CharSequence> it = elements.iterator();
        while (it.hasNext()) {
            CharSequence next = it.next();
            if (!first) {
                out = out.concat(sep);
            }
            out = out.concat(String.valueOf(next));
            first = false;
        }
        return out;
    }

    /**
     * Formats `args` per the printf-style `format` string (see java.util.Formatter). The
     * Locale-aware overload is H6-T5.
     */
    public static String format(String format, Object... args) {
        // NOTE: a simple name (via import) — not `new java.util.Formatter()`. A qualified
        // name in a `new` is miscompiled to an empty body (compiler finding #20).
        return new Formatter().format(format, args).toString();
    }

    public static String format(Locale l, String format, Object... args) {
        return new Formatter(l).format(format, args).toString();
    }

    /** This string used as the format, with {@code args} filled in. */
    public String formatted(Object... args) {
        return String.format(this, args);
    }

    /**
     * Applies {@code f} to this string.
     *
     * <p>It exists so that a call the reader follows left to right does not have to be turned
     * inside out just because the operation is not a method of String.
     */
    public <R> R transform(Function<? super String, ? extends R> f) {
        return f.apply(this);
    }

    // ---- case ----

    /**
     * This string in lower case.
     *
     * <p>Character by character through {@link Character#toLowerCase(int)}, with the handful of
     * mappings that are NOT one-to-one taken from a table. There is exactly one of those in
     * lower case and a hundred in upper case, and getting them wrong is not a rounding error:
     * {@code "ß".toUpperCase()} has to be {@code "SS"}, two characters where there was one.
     *
     * @implNote Locale-INSENSITIVE, always. The locale-taking overloads below delegate here, so
     *           the Turkish dotless-i rule is not applied; that needs per-locale tables this
     *           library does not have. Every other language agrees with the root locale.
     */
    public String toLowerCase() {
        return this.recase(false);
    }

    /** As {@link #toLowerCase()}. The locale is accepted and, for now, ignored — see there. */
    public String toLowerCase(Locale locale) {
        return this.recase(false);
    }

    /** This string in upper case. See {@link #toLowerCase()} for what "not one-to-one" means. */
    public String toUpperCase() {
        return this.recase(true);
    }

    /** As {@link #toUpperCase()}. The locale is accepted and, for now, ignored. */
    public String toUpperCase(Locale locale) {
        return this.recase(true);
    }

    // Both directions in one pass. The result can be LONGER than the input, which is why it is
    // built into a growing buffer instead of one sized like the source.
    private String recase(boolean upper) {
        int n = this.length();
        char[] buf = new char[n + 8];
        int put = 0;
        int i = 0;
        while (i < n) {
            int cp = this.codePointAt(i);
            i = i + (cp > 0xffff ? 2 : 1);
            int[] special = String.specialFor(cp, upper);
            if (special != null) {
                for (int k = 0; k < special.length; k++) {
                    if (put == buf.length) {
                        buf = String.grow(buf);
                    }
                    buf[put] = (char) special[k];
                    put = put + 1;
                }
                continue;
            }
            int mapped = upper ? Character.toUpperCase(cp) : Character.toLowerCase(cp);
            if (mapped > 0xffff) {
                if (put + 1 >= buf.length) {
                    buf = String.grow(buf);
                }
                buf[put] = Character.highSurrogate(mapped);
                buf[put + 1] = Character.lowSurrogate(mapped);
                put = put + 2;
                continue;
            }
            if (put == buf.length) {
                buf = String.grow(buf);
            }
            buf[put] = (char) mapped;
            put = put + 1;
        }
        return String.valueOf(buf, 0, put);
    }

    private static char[] grow(char[] buf) {
        char[] bigger = new char[buf.length * 2 + 8];
        for (int i = 0; i < buf.length; i++) {
            bigger[i] = buf[i];
        }
        return bigger;
    }

    // The multi-character mapping for `cp`, or null when the ordinary one-to-one rule applies.
    // The table is (codePoint, count, chars...) flattened, in code-point order, so a linear scan
    // over a hundred entries is a bisection away from being worth optimising and is not.
    private static int[] specialFor(int cp, boolean upper) {
        int[] table = upper ? String.SPECIAL_UPPER : String.SPECIAL_LOWER;
        int at = 0;
        while (at < table.length) {
            int key = table[at];
            int count = table[at + 1];
            if (key == cp) {
                int[] out = new int[count];
                for (int k = 0; k < count; k++) {
                    out[k] = table[at + 2 + k];
                }
                return out;
            }
            if (key > cp) {
                return null;
            }
            at = at + 2 + count;
        }
        return null;
    }

    // ---- regular expressions ----
    //
    // Every one of these is one line over java.util.regex, and that is the point: a regex method
    // on String is a convenience, not a second implementation. The behaviour a caller sees is
    // Pattern's, including which flavour of regex it speaks.

    /**
     * Whether the WHOLE string matches {@code regex}.
     *
     * <p>The whole string, not a part of it — which is the difference between this and
     * {@code Pattern.matcher(s).find()}, and the most common surprise in the class.
     */
    public boolean matches(String regex) {
        // Through a named local, and Matcher is imported for the same reason: a type the file
        // never writes is not resolved, and the whole chained call is dropped (finding #251).
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this);
        return matcher.matches();
    }

    /** Every match of {@code regex} replaced by {@code replacement}. */
    public String replaceAll(String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this);
        return matcher.replaceAll(replacement);
    }

    /** The first match of {@code regex} replaced by {@code replacement}. */
    public String replaceFirst(String regex, String replacement) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(this);
        return matcher.replaceFirst(replacement);
    }

    /**
     * Split around matches of {@code regex}, dropping trailing empty strings.
     *
     * @see #split(String, int) for what the limit changes
     */
    public String[] split(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return pattern.split(this);
    }

    /**
     * Split around matches of {@code regex}.
     *
     * @param limit how many parts at most; a NEGATIVE limit means no maximum and keeps the
     *              trailing empty strings, which zero and the one-argument form discard
     */
    public String[] split(String regex, int limit) {
        Pattern pattern = Pattern.compile(regex);
        return pattern.split(this, limit);
    }

    /**
     * Split around {@code regex}, keeping the SEPARATORS in the result.
     *
     * <p>The parts and the separators alternate, starting with a part, so the original string is
     * the concatenation of everything returned — which is what makes it usable for rewriting
     * rather than only for parsing.
     */
    public String[] splitWithDelimiters(String regex, int limit) {
        Pattern pattern = Pattern.compile(regex);
        return pattern.splitWithDelimiters(this, limit);
    }

    // ---- as a stream ----

    /**
     * The chars, as ints, each one on its own.
     *
     * <p>Chars and not code points: a surrogate pair arrives as its two halves. {@link
     * #codePoints} is the one that puts them back together.
     */
    public IntStream chars() {
        int n = this.length();
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = this.charAt(i);
        }
        return IntStream.of(out);
    }

    /** The code points, with surrogate pairs read as the single character they encode. */
    public IntStream codePoints() {
        int n = this.length();
        int[] out = new int[n];
        int put = 0;
        int i = 0;
        while (i < n) {
            int cp = this.codePointAt(i);
            out[put] = cp;
            put = put + 1;
            i = i + (cp > 0xffff ? 2 : 1);
        }
        int[] exact = new int[put];
        for (int k = 0; k < put; k++) {
            exact[k] = out[k];
        }
        return IntStream.of(exact);
    }

    /**
     * The lines, with the terminators removed.
     *
     * <p>All three terminators are recognised — {@code \n}, {@code \r} and {@code \r\n} — so
     * text written on one platform reads the same on another.
     */
    public Stream<String> lines() {
        String[] parts = this.splitLines();
        return Stream.of(parts);
    }

    // ---- odds and ends ----

    /**
     * The bytes of {@code [srcBegin, srcEnd)}, one per char, taking the LOW eight bits of each.
     *
     * @deprecated it throws away the high byte, so anything outside Latin-1 is silently
     *             corrupted. Use a charset-aware conversion.
     */
    @Deprecated(since = "1.1")
    public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
        if (srcBegin < 0 || srcEnd > this.length() || srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException("begin " + srcBegin + ", end " + srcEnd
                    + ", length " + this.length());
        }
        int n = srcEnd - srcBegin;
        for (int i = 0; i < n; i++) {
            dst[dstBegin + i] = (byte) this.charAt(srcBegin + i);
        }
    }

    /**
     * This string as a nominal descriptor, which for a String is itself.
     *
     * <p>Always present, never empty: every string is its own constant description, which is
     * why the {@link java.util.Optional} looks pointless here. It is not — the method comes from
     * an interface whose other implementations can fail to describe themselves.
     */
    public Optional<String> describeConstable() {
        return Optional.of(this);
    }

    /** Resolves this descriptor, which for a String is itself. */
    public String resolveConstantDesc(MethodHandles.Lookup lookup) {
        return this;
    }

    /**
     * The canonical instance of this string, so that equal strings become the SAME object.
     *
     * <p>The VM keeps one table of them, shared with the strings the constant pool materialises,
     * which is why it is native: the pool is not reachable from Java.
     */
    public native String intern();

    // Generated from the reference JDK: the code points whose case mapping is not
    // one-to-one, flattened as (codePoint, count, chars...). A hundred and two going up,
    // exactly one coming down.
    private static final int[] SPECIAL_UPPER = String.specialUpperTable();
    private static final int[] SPECIAL_LOWER = String.specialLowerTable();

    private static int[] specialUpperTable() {
        return new int[] {
           223, 2, 83, 83, 329, 2, 700, 78, 496, 2, 74, 780,
           912, 3, 921, 776, 769, 944, 3, 933, 776, 769, 1415, 2,
           1333, 1362, 7830, 2, 72, 817, 7831, 2, 84, 776, 7832, 2,
           87, 778, 7833, 2, 89, 778, 7834, 2, 65, 702, 8016, 2,
           933, 787, 8018, 3, 933, 787, 768, 8020, 3, 933, 787, 769,
           8022, 3, 933, 787, 834, 8064, 2, 7944, 921, 8065, 2, 7945,
           921, 8066, 2, 7946, 921, 8067, 2, 7947, 921, 8068, 2, 7948,
           921, 8069, 2, 7949, 921, 8070, 2, 7950, 921, 8071, 2, 7951,
           921, 8072, 2, 7944, 921, 8073, 2, 7945, 921, 8074, 2, 7946,
           921, 8075, 2, 7947, 921, 8076, 2, 7948, 921, 8077, 2, 7949,
           921, 8078, 2, 7950, 921, 8079, 2, 7951, 921, 8080, 2, 7976,
           921, 8081, 2, 7977, 921, 8082, 2, 7978, 921, 8083, 2, 7979,
           921, 8084, 2, 7980, 921, 8085, 2, 7981, 921, 8086, 2, 7982,
           921, 8087, 2, 7983, 921, 8088, 2, 7976, 921, 8089, 2, 7977,
           921, 8090, 2, 7978, 921, 8091, 2, 7979, 921, 8092, 2, 7980,
           921, 8093, 2, 7981, 921, 8094, 2, 7982, 921, 8095, 2, 7983,
           921, 8096, 2, 8040, 921, 8097, 2, 8041, 921, 8098, 2, 8042,
           921, 8099, 2, 8043, 921, 8100, 2, 8044, 921, 8101, 2, 8045,
           921, 8102, 2, 8046, 921, 8103, 2, 8047, 921, 8104, 2, 8040,
           921, 8105, 2, 8041, 921, 8106, 2, 8042, 921, 8107, 2, 8043,
           921, 8108, 2, 8044, 921, 8109, 2, 8045, 921, 8110, 2, 8046,
           921, 8111, 2, 8047, 921, 8114, 2, 8122, 921, 8115, 2, 913,
           921, 8116, 2, 902, 921, 8118, 2, 913, 834, 8119, 3, 913,
           834, 921, 8124, 2, 913, 921, 8130, 2, 8138, 921, 8131, 2,
           919, 921, 8132, 2, 905, 921, 8134, 2, 919, 834, 8135, 3,
           919, 834, 921, 8140, 2, 919, 921, 8146, 3, 921, 776, 768,
           8147, 3, 921, 776, 769, 8150, 2, 921, 834, 8151, 3, 921,
           776, 834, 8162, 3, 933, 776, 768, 8163, 3, 933, 776, 769,
           8164, 2, 929, 787, 8166, 2, 933, 834, 8167, 3, 933, 776,
           834, 8178, 2, 8186, 921, 8179, 2, 937, 921, 8180, 2, 911,
           921, 8182, 2, 937, 834, 8183, 3, 937, 834, 921, 8188, 2,
           937, 921, 64256, 2, 70, 70, 64257, 2, 70, 73, 64258, 2,
           70, 76, 64259, 3, 70, 70, 73, 64260, 3, 70, 70, 76,
           64261, 2, 83, 84, 64262, 2, 83, 84, 64275, 2, 1348, 1350,
           64276, 2, 1348, 1333, 64277, 2, 1348, 1339, 64278, 2, 1358, 1350,
           64279, 2, 1348, 1341,
        };
    }

    private static int[] specialLowerTable() {
        return new int[] {
           304, 2, 105, 775,
        };
    }

    // ---- as bytes ----
    //
    // Every one of these goes through java.nio.charset rather than doing the conversion here.
    // That is the point: "the bytes of a string" is not a property of the string, it is a
    // question that only has an answer once a charset is named, and the charset is the thing
    // that knows how to answer it.

    /**
     * These characters encoded with the default charset, which is UTF-8.
     *
     * <p>Unrepresentable input is replaced rather than reported, so this never throws -- with
     * UTF-8 nothing is unrepresentable anyway, but an unpaired surrogate still becomes a question
     * mark instead of an error.
     *
     * @return a fresh array of the encoded bytes
     */
    public byte[] getBytes() {
        Charset charset = Charset.defaultCharset();
        return this.getBytes(charset);
    }

    /**
     * These characters encoded with the named charset.
     *
     * <p>Prefer {@link #getBytes(Charset)} where the charset is known at compile time: the name
     * here can fail at run time, and this overload is declared to throw for exactly that reason.
     *
     * @param charsetName a canonical name or an alias, matched without regard to case
     * @return a fresh array of the encoded bytes
     * @throws UnsupportedEncodingException if no charset answers to that name
     */
    public byte[] getBytes(String charsetName) throws UnsupportedEncodingException {
        if (charsetName == null) {
            throw new NullPointerException();
        }
        Charset charset = String.charsetFor(charsetName);
        return this.getBytes(charset);
    }

    /**
     * These characters encoded with the given charset.
     *
     * <p>Unrepresentable input is replaced, not reported: the whole string always encodes to
     * something. Use {@link Charset#newEncoder} when it matters that a character was lost --
     * {@code "a-acute".getBytes(US_ASCII)} silently yields a question mark here.
     *
     * @param charset the charset to encode with
     * @return a fresh array of the encoded bytes
     */
    public byte[] getBytes(Charset charset) {
        if (charset == null) {
            throw new NullPointerException();
        }
        // Through the CharSequence overload, named in a local: `wrap` is also declared for
        // char[], and an unqualified argument picks the wrong one (finding #254).
        CharSequence self = this;
        CharBuffer chars = CharBuffer.wrap(self);
        ByteBuffer bytes = charset.encode(chars);
        int n = bytes.remaining();
        byte[] out = new byte[n];
        int i = 0;
        while (i < n) {
            out[i] = bytes.get();
            i = i + 1;
        }
        return out;
    }

    // Charset.forName reports an unknown name and a malformed one as the same unchecked
    // exception; this method has to report both as the CHECKED one. try/catch with no finally,
    // which is the form that compiles here (finding #257).
    private static Charset charsetFor(String charsetName) throws UnsupportedEncodingException {
        try {
            return Charset.forName(charsetName);
        } catch (IllegalArgumentException unusable) {
            throw new UnsupportedEncodingException(charsetName);
        }
    }

}


/** The comparator behind {@link String#CASE_INSENSITIVE_ORDER}. */
final class CaseInsensitiveOrder implements Comparator<String> {

    @Override
    public int compare(String a, String b) {
        return a.compareToIgnoreCase(b);
    }
}
