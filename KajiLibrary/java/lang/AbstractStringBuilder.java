package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.util.stream.IntStream;

/**
 * KajiLibrary's java.lang.AbstractStringBuilder -- the shared, mutable character buffer that both
 * {@link StringBuilder} and {@link StringBuffer} extend, exactly as the JDK factors it.
 *
 * <p>The whole class is a {@code char[]} and a count, and every method is arithmetic on those two.
 * The one thing plain bytecode cannot do is turn the array into a {@code String}, so
 * {@link #toString()} goes through {@code String.valueOf(char[], int, int)} and nothing else here
 * touches the VM.
 *
 * <p>It is <strong>package-private</strong>: a caller outside {@code java.lang} cannot name it, so
 * the public methods it declares reach the outside only through the two public subclasses. That is
 * exactly why the chainable methods here return {@code AbstractStringBuilder} and each subclass
 * re-declares them with a covariant return (a bridge), and why the non-chainable ones a subclass
 * does not re-declare are reached through the synthetic accessor forwarders {@code javac}
 * synthesizes in the public subclass (finding #268). One implementation, so the two classes cannot
 * drift apart.
 */
abstract class AbstractStringBuilder implements CharSequence, Appendable {


    // Package-private, como en el JDK: las subclases (`StringBuilder`/`StringBuffer`) viven en
    // `java.lang` y pueden verlos, y nadie fuera del paquete debería.
    char[] value;

    int count;

    /** An empty buffer with room for sixteen characters. */
    AbstractStringBuilder() {
        this.value = new char[16];
        this.count = 0;
    }

    /**
     * An empty buffer with room for {@code capacity} characters.
     *
     * @param capacity how much room to reserve
     * @throws NegativeArraySizeException if {@code capacity} is negative
     */
    AbstractStringBuilder(int capacity) {
        if (capacity < 0) {
            throw new NegativeArraySizeException("capacity is negative: " + capacity);
        }
        this.value = new char[capacity];
        this.count = 0;
    }

    // ---- room ----
    //
    // Growth doubles and adds two rather than doubling alone, which is not a detail: doubling a
    // capacity of zero gives zero, and a builder made with `new StringBuilder(0)` would never
    // grow at all. The `+ 2` is what makes the sequence escape from zero.

    /**
     * How many characters fit before the next reallocation.
     */
    public int capacity() {
        return this.value.length;
    }

    /**
     * Reserve room for at least {@code minimumCapacity} characters.
     *
     * <p>A hint and nothing more: it never shrinks, and a non-positive argument does nothing.
     * Actual capacity may end up larger, because growth follows the doubling rule rather than
     * the request.
     *
     * @param minimumCapacity how much room is wanted
     */
    public void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > 0) {
            this.grow(minimumCapacity);
        }
    }

    // Make room for at least `min` characters. Separate from the public `ensureCapacity` because
    // that one ignores non-positive requests, and the internal callers need it to not.
    private void grow(int min) {
        if (min <= this.value.length) {
            return;
        }
        int doubled = this.value.length * 2 + 2;
        int newCapacity = doubled;
        if (doubled < min || doubled < 0) {
            newCapacity = min;
        }
        char[] bigger = new char[newCapacity];
        System.arraycopy(this.value, 0, bigger, 0, this.count);
        this.value = bigger;
    }

    /**
     * Give back the room that is not being used.
     *
     * <p>A request, like {@link #ensureCapacity(int)}, and the mirror of it.
     */
    public void trimToSize() {
        if (this.count < this.value.length) {
            char[] exact = new char[this.count];
            System.arraycopy(this.value, 0, exact, 0, this.count);
            this.value = exact;
        }
    }

    /**
     * Set the length, truncating or padding with NUL characters.
     *
     * <p>Padding with NUL and not with a space is the part worth stating: the padded
     * region is indistinguishable from real content afterwards, so growing a builder this
     * way is a way to get characters you did not put there.
     *
     * @param newLength the length to set
     * @throws StringIndexOutOfBoundsException if {@code newLength} is negative
     */
    public void setLength(int newLength) {
        if (newLength < 0) {
            throw new StringIndexOutOfBoundsException("newLength is negative: " + newLength);
        }
        this.grow(newLength);
        int i = this.count;
        while (i < newLength) {
            this.value[i] = (char) 0;
            i = i + 1;
        }
        this.count = newLength;
    }

    // ---- reading ----

    /** How many characters are in the buffer. */
    public int length() {
        return this.count;
    }

    /**
     * The character at {@code index}.
     *
     * @param index the position
     * @throws StringIndexOutOfBoundsException if {@code index} is not in range
     */
    public char charAt(int index) {
        if (index < 0 || index >= this.count) {
            throw new StringIndexOutOfBoundsException("index " + index + ", length " + this.count);
        }
        return this.value[index];
    }

    /**
     * Overwrite the character at {@code index}.
     *
     * @param index the position
     * @param ch what to write there
     * @throws StringIndexOutOfBoundsException if {@code index} is not in range
     */
    public void setCharAt(int index, char ch) {
        if (index < 0 || index >= this.count) {
            throw new StringIndexOutOfBoundsException("index " + index + ", length " + this.count);
        }
        this.value[index] = ch;
    }

    /**
     * Copy {@code [srcBegin, srcEnd)} into {@code dst} starting at {@code dstBegin}.
     *
     * @param srcBegin where to start reading
     * @param srcEnd where to stop reading, exclusive
     * @param dst where to write
     * @param dstBegin where to start writing
     * @throws StringIndexOutOfBoundsException if any index is out of range
     */
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > this.count) {
            throw new StringIndexOutOfBoundsException(
                    "srcBegin " + srcBegin + ", srcEnd " + srcEnd + ", length " + this.count);
        }
        System.arraycopy(this.value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    /**
     * The code point beginning at {@code index}.
     *
     * @param index the position
     * @throws StringIndexOutOfBoundsException if {@code index} is not in range
     */
    public int codePointAt(int index) {
        if (index < 0 || index >= this.count) {
            throw new StringIndexOutOfBoundsException("index " + index + ", length " + this.count);
        }
        return Character.codePointAt(this.value, index, this.count);
    }

    /**
     * The code point ending just before {@code index}.
     *
     * @param index the position to look back from
     * @throws StringIndexOutOfBoundsException if {@code index} is not in range
     */
    public int codePointBefore(int index) {
        if (index < 1 || index > this.count) {
            throw new StringIndexOutOfBoundsException("index " + index + ", length " + this.count);
        }
        return Character.codePointBefore(this.value, index, 0);
    }

    /**
     * How many code points {@code [beginIndex, endIndex)} holds.
     *
     * <p>Not the same as the number of characters, and that is the point: a supplementary code
     * point occupies two of them.
     *
     * @param beginIndex where to start
     * @param endIndex where to stop, exclusive
     * @throws IndexOutOfBoundsException if the range is not valid
     */
    public int codePointCount(int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > this.count || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException(
                    "begin " + beginIndex + ", end " + endIndex + ", length " + this.count);
        }
        return Character.codePointCount(this.value, beginIndex, endIndex - beginIndex);
    }

    /**
     * The index {@code codePointOffset} code points away from {@code index}.
     *
     * @param index where to start
     * @param codePointOffset how many code points to move, either direction
     * @throws IndexOutOfBoundsException if the walk leaves the buffer
     */
    public int offsetByCodePoints(int index, int codePointOffset) {
        if (index < 0 || index > this.count) {
            throw new IndexOutOfBoundsException("index " + index + ", length " + this.count);
        }
        return Character.offsetByCodePoints(this.value, 0, this.count, index, codePointOffset);
    }

    // ---- appending ----
    //
    // Thirteen overloads, and only three of them do any work: char, char[] and String. The rest
    // convert and hand over, which is what keeps the bounds arithmetic in one place.

    /**
     * Append one character.
     *
     * @param c the character
     */
    public AbstractStringBuilder append(char c) {
        this.grow(this.count + 1);
        this.value[this.count] = c;
        this.count = this.count + 1;
        return this;
    }

    /**
     * Append a string, or the four characters {@code null}.
     *
     * @param str what to append
     */
    public AbstractStringBuilder append(String str) {
        if (str == null) {
            return this.appendNull();
        }
        int n = str.length();
        this.grow(this.count + n);
        str.getChars(0, n, this.value, this.count);
        this.count = this.count + n;
        return this;
    }

    /**
     * Append every character of {@code str}.
     *
     * @param str what to append
     */
    public AbstractStringBuilder append(char[] str) {
        return this.append(str, 0, str.length);
    }

    /**
     * Append {@code len} characters of {@code str} starting at {@code offset}.
     *
     * @param str where to read from
     * @param offset where to start reading
     * @param len how many characters
     * @throws IndexOutOfBoundsException if the range is not valid
     */
    public AbstractStringBuilder append(char[] str, int offset, int len) {
        if (offset < 0 || len < 0 || offset + len > str.length) {
            throw new IndexOutOfBoundsException(
                    "offset " + offset + ", len " + len + ", length " + str.length);
        }
        this.grow(this.count + len);
        System.arraycopy(str, offset, this.value, this.count, len);
        this.count = this.count + len;
        return this;
    }

    /**
     * Append the string form of {@code obj}, or {@code null}.
     *
     * @param obj what to append
     */
    public AbstractStringBuilder append(Object obj) {
        if (obj == null) {
            return this.appendNull();
        }
        return this.append(obj.toString());
    }

    /**
     * Append {@code true} or {@code false}.
     *
     * @param b what to append
     */
    public AbstractStringBuilder append(boolean b) {
        if (b) {
            return this.append("true");
        }
        return this.append("false");
    }

    /**
     * Append a character sequence, or the four characters {@code null}.
     *
     * @param s what to append
     */
    public AbstractStringBuilder append(CharSequence s) {
        if (s == null) {
            return this.appendNull();
        }
        return this.append(s, 0, s.length());
    }

    /**
     * Append {@code [start, end)} of a character sequence.
     *
     * <p>A null sequence is spelled out as {@code null} and then sliced, so
     * {@code append((CharSequence) null, 1, 3)} appends {@code "ul"} -- which looks odd and is
     * what the specification says.
     *
     * @param s what to append
     * @param start where to start
     * @param end where to stop, exclusive
     * @throws IndexOutOfBoundsException if the range is not valid
     */
    public AbstractStringBuilder append(CharSequence s, int start, int end) {
        CharSequence seq = s;
        if (seq == null) {
            seq = "null";
        }
        if (start < 0 || start > end || end > seq.length()) {
            throw new IndexOutOfBoundsException(
                    "start " + start + ", end " + end + ", length " + seq.length());
        }
        this.grow(this.count + (end - start));
        int i = start;
        while (i < end) {
            this.value[this.count] = seq.charAt(i);
            this.count = this.count + 1;
            i = i + 1;
        }
        return this;
    }

    /**
     * Append the contents of a {@link StringBuffer}, or the four characters {@code null}.
     *
     * @param sb what to append
     */
    public AbstractStringBuilder append(StringBuffer sb) {
        if (sb == null) {
            return this.appendNull();
        }
        return this.append(sb.toString());
    }

    /**
     * Append the decimal form of an int.
     *
     * <p>Written out digit by digit rather than through {@code Integer.toString}, because this is
     * the method every string concatenation lands in and an intermediate {@code String} would be
     * garbage on every one of them.
     *
     * @param i what to append
     */
    public AbstractStringBuilder append(int i) {
        if (i == 0) {
            return this.append('0');
        }
        // Accumulate in NEGATIVE space: the most negative int has no positive counterpart, so
        // negating first would be the one input this method could not print.
        int rest = i;
        boolean negative = rest < 0;
        if (rest > 0) {
            rest = -rest;
        }
        char[] digits = new char[11];
        int p = 11;
        while (rest < 0) {
            int digit = -(rest % 10);
            p = p - 1;
            digits[p] = (char) ('0' + digit);
            rest = rest / 10;
        }
        if (negative) {
            this.append('-');
        }
        return this.append(digits, p, 11 - p);
    }

    /**
     * Append the decimal form of a long.
     *
     * @param l what to append
     * @see #append(int) for why it counts downward
     */
    public AbstractStringBuilder append(long l) {
        if (l == 0L) {
            return this.append('0');
        }
        long rest = l;
        boolean negative = rest < 0L;
        if (rest > 0L) {
            rest = -rest;
        }
        char[] digits = new char[20];
        int p = 20;
        while (rest < 0L) {
            int digit = (int) -(rest % 10L);
            p = p - 1;
            digits[p] = (char) ('0' + digit);
            rest = rest / 10L;
        }
        if (negative) {
            this.append('-');
        }
        return this.append(digits, p, 20 - p);
    }

    /**
     * Append the shortest decimal that reads back as {@code f}.
     *
     * @param f what to append
     */
    public AbstractStringBuilder append(float f) {
        return this.append(String.valueOf(f));
    }

    /**
     * Append the shortest decimal that reads back as {@code d}.
     *
     * @param d what to append
     */
    public AbstractStringBuilder append(double d) {
        return this.append(String.valueOf(d));
    }

    /**
     * Append a code point, as one character or as a surrogate pair.
     *
     * @param codePoint the code point
     * @throws IllegalArgumentException if it is not a valid code point
     */
    public AbstractStringBuilder appendCodePoint(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("not a code point: " + codePoint);
        }
        if (!Character.isSupplementaryCodePoint(codePoint)) {
            return this.append((char) codePoint);
        }
        char[] pair = Character.toChars(codePoint);
        return this.append(pair, 0, pair.length);
    }

    // The four characters every null argument turns into. One method rather than a literal in
    // eight places, so there is one thing to be wrong.
    private AbstractStringBuilder appendNull() {
        return this.append("null");
    }

    // ---- removing ----

    /**
     * Remove {@code [start, end)}.
     *
     * <p>{@code end} past the end is not an error, it means "to the end" -- which is what makes
     * {@code delete(k, Integer.MAX_VALUE)} the idiomatic truncation.
     *
     * @param start where to start removing
     * @param end where to stop, exclusive
     * @throws StringIndexOutOfBoundsException if {@code start} is not in range or is past
     *         {@code end}
     */
    public AbstractStringBuilder delete(int start, int end) {
        int stop = end;
        if (stop > this.count) {
            stop = this.count;
        }
        if (start < 0 || start > this.count || start > stop) {
            throw new StringIndexOutOfBoundsException(
                    "start " + start + ", end " + end + ", length " + this.count);
        }
        int removed = stop - start;
        if (removed > 0) {
            System.arraycopy(this.value, stop, this.value, start, this.count - stop);
            this.count = this.count - removed;
        }
        return this;
    }

    /**
     * Remove the character at {@code index}.
     *
     * @param index which one
     * @throws StringIndexOutOfBoundsException if {@code index} is not in range
     */
    public AbstractStringBuilder deleteCharAt(int index) {
        if (index < 0 || index >= this.count) {
            throw new StringIndexOutOfBoundsException("index " + index + ", length " + this.count);
        }
        System.arraycopy(this.value, index + 1, this.value, index, this.count - index - 1);
        this.count = this.count - 1;
        return this;
    }

    /**
     * Replace {@code [start, end)} with {@code str}.
     *
     * <p>The replacement does not have to be the same length, which is the whole reason this is
     * not {@code delete} followed by {@code insert}: doing it in one move shifts the tail once.
     *
     * @param start where to start replacing
     * @param end where to stop, exclusive
     * @param str what to put there
     * @throws StringIndexOutOfBoundsException if the range is not valid
     */
    public AbstractStringBuilder replace(int start, int end, String str) {
        int stop = end;
        if (stop > this.count) {
            stop = this.count;
        }
        if (start < 0 || start > this.count || start > stop) {
            throw new StringIndexOutOfBoundsException(
                    "start " + start + ", end " + end + ", length " + this.count);
        }
        int n = str.length();
        int tail = this.count - stop;
        this.grow(start + n + tail);
        System.arraycopy(this.value, stop, this.value, start + n, tail);
        str.getChars(0, n, this.value, start);
        this.count = start + n + tail;
        return this;
    }

    // ---- inserting ----
    //
    // Twelve overloads over one primitive: open a hole and write into it. Everything else
    // converts to a String first, which costs an allocation the append family avoids -- and is
    // worth it here, because insert has to know the length BEFORE it moves the tail.

    /**
     * Insert {@code str} at {@code offset}.
     *
     * @param offset where to insert
     * @param str what to insert; null becomes the four characters {@code null}
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, String str) {
        String text = str;
        if (text == null) {
            text = "null";
        }
        this.openHole(offset, text.length());
        text.getChars(0, text.length(), this.value, offset);
        return this;
    }

    /**
     * Insert every character of {@code str} at {@code offset}.
     *
     * @param offset where to insert
     * @param str what to insert
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, char[] str) {
        this.openHole(offset, str.length);
        System.arraycopy(str, 0, this.value, offset, str.length);
        return this;
    }

    /**
     * Insert {@code len} characters of {@code str}, from {@code strOffset}, at {@code index}.
     *
     * @param index where to insert
     * @param str where to read from
     * @param strOffset where to start reading
     * @param len how many characters
     * @throws StringIndexOutOfBoundsException if any index is out of range
     */
    public AbstractStringBuilder insert(int index, char[] str, int strOffset, int len) {
        if (strOffset < 0 || len < 0 || strOffset + len > str.length) {
            throw new StringIndexOutOfBoundsException(
                    "offset " + strOffset + ", len " + len + ", length " + str.length);
        }
        this.openHole(index, len);
        System.arraycopy(str, strOffset, this.value, index, len);
        return this;
    }

    /**
     * Insert the string form of {@code obj} at {@code offset}.
     *
     * @param offset where to insert
     * @param obj what to insert
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, Object obj) {
        if (obj == null) {
            return this.insert(offset, "null");
        }
        return this.insert(offset, obj.toString());
    }

    /**
     * Insert a character sequence at {@code dstOffset}.
     *
     * @param dstOffset where to insert
     * @param s what to insert
     * @throws IndexOutOfBoundsException if {@code dstOffset} is not in range
     */
    public AbstractStringBuilder insert(int dstOffset, CharSequence s) {
        if (s == null) {
            return this.insert(dstOffset, "null");
        }
        return this.insert(dstOffset, s, 0, s.length());
    }

    /**
     * Insert {@code [start, end)} of a character sequence at {@code dstOffset}.
     *
     * @param dstOffset where to insert
     * @param s what to insert
     * @param start where to start reading
     * @param end where to stop reading, exclusive
     * @throws IndexOutOfBoundsException if any index is out of range
     */
    public AbstractStringBuilder insert(int dstOffset, CharSequence s, int start, int end) {
        CharSequence seq = s;
        if (seq == null) {
            seq = "null";
        }
        if (dstOffset < 0 || dstOffset > this.count) {
            throw new IndexOutOfBoundsException(
                    "dstOffset " + dstOffset + ", length " + this.count);
        }
        if (start < 0 || start > end || end > seq.length()) {
            throw new IndexOutOfBoundsException(
                    "start " + start + ", end " + end + ", length " + seq.length());
        }
        this.openHole(dstOffset, end - start);
        int i = start;
        int at = dstOffset;
        while (i < end) {
            this.value[at] = seq.charAt(i);
            at = at + 1;
            i = i + 1;
        }
        return this;
    }

    /**
     * Insert {@code true} or {@code false} at {@code offset}.
     *
     * @param offset where to insert
     * @param b what to insert
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, boolean b) {
        if (b) {
            return this.insert(offset, "true");
        }
        return this.insert(offset, "false");
    }

    /**
     * Insert one character at {@code offset}.
     *
     * @param offset where to insert
     * @param c what to insert
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, char c) {
        this.openHole(offset, 1);
        this.value[offset] = c;
        return this;
    }

    /**
     * Insert the decimal form of an int at {@code offset}.
     *
     * @param offset where to insert
     * @param i what to insert
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, int i) {
        return this.insert(offset, Integer.toString(i));
    }

    /**
     * Insert the decimal form of a long at {@code offset}.
     *
     * @param offset where to insert
     * @param l what to insert
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, long l) {
        return this.insert(offset, Long.toString(l));
    }

    /**
     * Insert the decimal form of a float at {@code offset}.
     *
     * @param offset where to insert
     * @param f what to insert
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, float f) {
        return this.insert(offset, String.valueOf(f));
    }

    /**
     * Insert the decimal form of a double at {@code offset}.
     *
     * @param offset where to insert
     * @param d what to insert
     * @throws StringIndexOutOfBoundsException if {@code offset} is not in range
     */
    public AbstractStringBuilder insert(int offset, double d) {
        return this.insert(offset, String.valueOf(d));
    }

    // Make a gap of `len` characters at `offset` and count it as already occupied. The caller
    // then writes into it. Splitting it out is what keeps the twelve inserts from each having
    // their own copy of the bounds check and the shift.
    private void openHole(int offset, int len) {
        if (offset < 0 || offset > this.count) {
            throw new StringIndexOutOfBoundsException(
                    "offset " + offset + ", length " + this.count);
        }
        this.grow(this.count + len);
        System.arraycopy(this.value, offset, this.value, offset + len, this.count - offset);
        this.count = this.count + len;
    }

    // ---- repeating ----

    /**
     * Append {@code times} copies of a code point.
     *
     * @param codePoint what to repeat
     * @param times how many copies; zero appends nothing
     * @throws IllegalArgumentException if {@code times} is negative or the code point is invalid
     */
    public AbstractStringBuilder repeat(int codePoint, int times) {
        if (times < 0) {
            throw new IllegalArgumentException("count is negative: " + times);
        }
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("not a code point: " + codePoint);
        }
        int width = Character.charCount(codePoint);
        this.grow(this.count + width * times);
        int i = 0;
        while (i < times) {
            this.appendCodePoint(codePoint);
            i = i + 1;
        }
        return this;
    }

    /**
     * Append {@code times} copies of a character sequence.
     *
     * @param cs what to repeat; null becomes the four characters {@code null}
     * @param times how many copies; zero appends nothing
     * @throws IllegalArgumentException if {@code times} is negative
     */
    public AbstractStringBuilder repeat(CharSequence cs, int times) {
        if (times < 0) {
            throw new IllegalArgumentException("count is negative: " + times);
        }
        CharSequence seq = cs;
        if (seq == null) {
            seq = "null";
        }
        int n = seq.length();
        this.grow(this.count + n * times);
        int i = 0;
        while (i < times) {
            this.append(seq, 0, n);
            i = i + 1;
        }
        return this;
    }

    // ---- searching ----

    /**
     * The first index at which {@code str} occurs, or -1.
     *
     * @param str what to look for
     */
    public int indexOf(String str) {
        return this.indexOf(str, 0);
    }

    /**
     * The first index at or after {@code fromIndex} at which {@code str} occurs, or -1.
     *
     * @param str what to look for
     * @param fromIndex where to start looking
     */
    public int indexOf(String str, int fromIndex) {
        int n = str.length();
        int from = fromIndex;
        if (from < 0) {
            from = 0;
        }
        if (n == 0) {
            if (from > this.count) {
                return this.count;
            }
            return from;
        }
        int last = this.count - n;
        int i = from;
        while (i <= last) {
            if (this.matchesAt(str, n, i)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * The last index at which {@code str} occurs, or -1.
     *
     * @param str what to look for
     */
    public int lastIndexOf(String str) {
        return this.lastIndexOf(str, this.count);
    }

    /**
     * The last index at or before {@code fromIndex} at which {@code str} occurs, or -1.
     *
     * @param str what to look for
     * @param fromIndex where to start looking, going backwards
     */
    public int lastIndexOf(String str, int fromIndex) {
        int n = str.length();
        int start = fromIndex;
        if (start > this.count - n) {
            start = this.count - n;
        }
        if (start < 0) {
            return -1;
        }
        if (n == 0) {
            return start;
        }
        int i = start;
        while (i >= 0) {
            if (this.matchesAt(str, n, i)) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    // Whether the first `n` characters of `str` sit at `at`. The caller has already checked that
    // the window fits.
    private boolean matchesAt(String str, int n, int at) {
        int k = 0;
        while (k < n) {
            if (this.value[at + k] != str.charAt(k)) {
                return false;
            }
            k = k + 1;
        }
        return true;
    }

    // ---- reversing ----

    /**
     * Reverse the buffer in place.
     *
     * <p>Surrogate pairs come out unbroken, which is the only difficult part of this method: a
     * plain reversal would leave the low surrogate before the high one, and that pair no longer
     * spells the character it used to. So any pair the reversal inverted is swapped back. Note
     * what this does NOT promise: an unpaired surrogate stays unpaired and moves like an ordinary
     * character.
     */
    public AbstractStringBuilder reverse() {
        boolean sawSurrogate = false;
        int last = this.count - 1;
        int j = (last - 1) / 2;
        while (j >= 0) {
            int k = last - j;
            char cj = this.value[j];
            char ck = this.value[k];
            if (Character.isSurrogate(cj) || Character.isSurrogate(ck)) {
                sawSurrogate = true;
            }
            this.value[j] = ck;
            this.value[k] = cj;
            j = j - 1;
        }
        if (sawSurrogate) {
            int i = 0;
            while (i < this.count - 1) {
                char low = this.value[i];
                if (Character.isLowSurrogate(low)) {
                    char high = this.value[i + 1];
                    if (Character.isHighSurrogate(high)) {
                        this.value[i] = high;
                        this.value[i + 1] = low;
                        i = i + 1;
                    }
                }
                i = i + 1;
            }
        }
        return this;
    }

    // ---- reading out ----

    /**
     * The contents from {@code start} to the end, as a string.
     *
     * @param start where to start
     * @throws StringIndexOutOfBoundsException if {@code start} is not in range
     */
    public String substring(int start) {
        return this.substring(start, this.count);
    }

    /**
     * {@code [start, end)} as a string.
     *
     * @param start where to start
     * @param end where to stop, exclusive
     * @throws StringIndexOutOfBoundsException if the range is not valid
     */
    public String substring(int start, int end) {
        if (start < 0 || end > this.count || start > end) {
            throw new StringIndexOutOfBoundsException(
                    "start " + start + ", end " + end + ", length " + this.count);
        }
        return String.valueOf(this.value, start, end - start);
    }

    /**
     * {@code [start, end)} as a character sequence.
     *
     * <p>Same thing as {@link #substring(int, int)}: the result is a {@code String}, so it does
     * NOT track later changes to this builder.
     *
     * @param start where to start
     * @param end where to stop, exclusive
     * @throws StringIndexOutOfBoundsException if the range is not valid
     */
    public CharSequence subSequence(int start, int end) {
        return this.substring(start, end);
    }

    /**
     * The contents as a string. Abstract here, as in the JDK: each concrete builder provides it
     * (both do the same {@code String.valueOf(value, 0, count)}), which is what lets this class stay
     * the shared implementation without being instantiable on its own.
     */
    public abstract String toString();

    /**
     * The characters as ints, zero-extended.
     *
     * <p>A snapshot, unlike the JDK's, which reads the buffer lazily as the stream is consumed.
     * The difference shows only if the builder is mutated while the stream is open, which the
     * JDK's own documentation calls undefined -- so this trades an unspecified behaviour for a
     * predictable one.
     */
    public IntStream chars() {
        int[] out = new int[this.count];
        int i = 0;
        while (i < this.count) {
            out[i] = this.value[i];
            i = i + 1;
        }
        return IntStream.of(out);
    }

    /**
     * The code points as ints.
     *
     * <p>A snapshot, for the same reason as {@link #chars()}.
     */
    public IntStream codePoints() {
        int[] out = new int[this.count];
        int found = 0;
        int i = 0;
        while (i < this.count) {
            int cp = Character.codePointAt(this.value, i, this.count);
            out[found] = cp;
            found = found + 1;
            i = i + Character.charCount(cp);
        }
        int[] exact = new int[found];
        System.arraycopy(out, 0, exact, 0, found);
        return IntStream.of(exact);
    }

}
