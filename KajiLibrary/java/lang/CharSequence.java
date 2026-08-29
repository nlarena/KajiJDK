package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.util.stream.IntStream;

/**
 * KajiLibrary's java.lang.CharSequence -- a readable sequence of {@code char} values.
 *
 * <p>Four abstract methods and five defaults built on them, which is the whole shape: a length,
 * indexed access, and slicing are enough to derive everything else, so an implementation owes
 * almost nothing. That is what lets {@code String}, {@code StringBuilder}, {@code CharBuffer} and
 * a segment of somebody's own buffer all be one type.
 *
 * <p>Note what it does NOT promise: nothing about mutability, and nothing about {@code equals}.
 * A {@code StringBuilder} changes under the reader, and two sequences holding the same characters
 * are usually not equal -- which is why {@link #compare(CharSequence, CharSequence)} is a static
 * method here rather than a {@code Comparable} implementation.
 */
public interface CharSequence {

    /** How many {@code char} values the sequence holds. */
    int length();

    /**
     * The value at {@code index}.
     *
     * @param index the position
     * @throws IndexOutOfBoundsException if it is not in range
     */
    char charAt(int index);

    /**
     * The subsequence from {@code start} to {@code end}, exclusive.
     *
     * @param start where to start
     * @param end where to stop, exclusive
     * @throws IndexOutOfBoundsException if the range is not valid
     */
    CharSequence subSequence(int start, int end);

    /** The sequence as a string. */
    String toString();

    /** Whether the sequence holds no characters at all. */
    default boolean isEmpty() {
        return this.length() == 0;
    }

    /**
     * Copies {@code [srcBegin, srcEnd)} into {@code dst} starting at {@code dstBegin}.
     *
     * <p>The default reads one character at a time, which is correct for every implementation and
     * fast for none -- an implementation over a {@code char[]} overrides it with a bulk copy.
     *
     * @param srcBegin where to start reading
     * @param srcEnd where to stop reading, exclusive
     * @param dst where to write
     * @param dstBegin where to start writing
     * @throws IndexOutOfBoundsException if any index is out of range
     */
    default void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > this.length() || dstBegin < 0
                || dstBegin + (srcEnd - srcBegin) > dst.length) {
            throw new IndexOutOfBoundsException(
                    "srcBegin " + srcBegin + ", srcEnd " + srcEnd + ", dstBegin " + dstBegin);
        }
        int from = srcBegin;
        int to = dstBegin;
        while (from < srcEnd) {
            dst[to] = this.charAt(from);
            from = from + 1;
            to = to + 1;
        }
    }

    /**
     * The characters as ints, zero-extended.
     *
     * <p>A snapshot here, where the JDK reads lazily. The difference shows only if the sequence
     * is mutated while the stream is open, which the JDK's own documentation leaves undefined --
     * so this trades an unspecified behaviour for a predictable one.
     */
    default IntStream chars() {
        int n = this.length();
        int[] out = new int[n];
        int i = 0;
        while (i < n) {
            out[i] = this.charAt(i);
            i = i + 1;
        }
        return IntStream.of(out);
    }

    /**
     * The code points as ints.
     *
     * <p>Fewer than {@link #chars()} whenever a surrogate pair is present: that pair is two
     * {@code char} values and one code point, and the whole reason both methods exist is that
     * neither answer is the right one for every caller.
     */
    default IntStream codePoints() {
        int n = this.length();
        int[] out = new int[n];
        int found = 0;
        int i = 0;
        while (i < n) {
            char high = this.charAt(i);
            int point = high;
            i = i + 1;
            if (Character.isHighSurrogate(high) && i < n) {
                char low = this.charAt(i);
                if (Character.isLowSurrogate(low)) {
                    point = Character.toCodePoint(high, low);
                    i = i + 1;
                }
            }
            out[found] = point;
            found = found + 1;
        }
        int[] exact = new int[found];
        System.arraycopy(out, 0, exact, 0, found);
        return IntStream.of(exact);
    }

    /**
     * Compares two sequences lexicographically, by their characters.
     *
     * <p>Static rather than an instance method, and that is not an accident of history: an
     * instance method would have to live on every implementation, and the comparison is between
     * two sequences of possibly DIFFERENT implementations -- a {@code String} against a
     * {@code StringBuilder} -- so it belongs to neither of them.
     *
     * @param cs1 the first
     * @param cs2 the second
     */
    static int compare(CharSequence cs1, CharSequence cs2) {
        if (cs1 == cs2) {
            return 0;
        }
        int shorter = cs1.length();
        if (cs2.length() < shorter) {
            shorter = cs2.length();
        }
        int i = 0;
        while (i < shorter) {
            char a = cs1.charAt(i);
            char b = cs2.charAt(i);
            if (a != b) {
                return a - b;
            }
            i = i + 1;
        }
        return cs1.length() - cs2.length();
    }
}
