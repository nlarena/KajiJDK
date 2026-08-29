package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.io.Serializable;
import java.util.stream.IntStream;

/**
 * KajiLibrary's java.lang.StringBuffer -- {@link StringBuilder} with a lock around it.
 *
 * <p>That is the entire difference between the two classes, and it is worth being blunt about
 * it, because the name suggests a different data structure and there is none. Every method here
 * takes this object as its monitor and forwards to a {@code StringBuilder} that holds the
 * characters. One implementation, so the two classes cannot drift apart in behaviour; the lock
 * is the only thing this one adds.
 *
 * <p><strong>And the lock buys less than it looks like.</strong> Each individual call is atomic,
 * so the buffer can never be left half-written. A SEQUENCE of calls is not: two threads doing
 * {@code if (sb.length() > 0) sb.deleteCharAt(0)} can both pass the test and the second one can
 * fail. That is why {@code StringBuilder} exists at all and why it is what the compiler emits
 * for string concatenation -- code that needs the buffer to be consistent across more than one
 * call has to hold a lock of its own anyway, and code that does not needs no lock.
 *
 * <p>A handful of methods are deliberately NOT synchronized: the inserts that only convert their
 * argument, and the one-argument {@code indexOf} and {@code lastIndexOf}. Each of them does
 * nothing but call a sibling that IS synchronized, so taking the monitor first would buy
 * nothing and cost a second acquisition. The JDK draws the line in the same places.
 */
public final class StringBuffer implements CharSequence, Appendable, Serializable,
        Comparable<StringBuffer> {

    // Composition rather than a shared superclass: the JDK factors the implementation into a
    // package-private `AbstractStringBuilder`, which relies on javac synthesizing accessor
    // forwarders for the public methods a public class inherits from a package-private one --
    // machinery our javac does not have (finding #268). See StringBuilder for the long version.
    private final StringBuilder buf;

    /** An empty buffer with room for sixteen characters. */
    public StringBuffer() {
        this.buf = new StringBuilder();
    }

    /**
     * An empty buffer with room for {@code capacity} characters.
     *
     * @param capacity how much room to reserve
     * @throws NegativeArraySizeException if {@code capacity} is negative
     */
    public StringBuffer(int capacity) {
        this.buf = new StringBuilder(capacity);
    }

    /**
     * A buffer holding {@code str}, with sixteen characters of room to spare.
     *
     * @param str the initial contents
     */
    public StringBuffer(String str) {
        this.buf = new StringBuilder(str);
    }

    /**
     * A buffer holding {@code seq}, with sixteen characters of room to spare.
     *
     * @param seq the initial contents
     */
    public StringBuffer(CharSequence seq) {
        this.buf = new StringBuilder(seq);
    }


    // ---- room ----

    /**
     * How many characters fit before the next reallocation.
     *
     * @see StringBuilder#capacity()
     */
    public synchronized int capacity() {
        return this.buf.capacity();
    }

    /**
     * Reserve room for at least {@code minimumCapacity} characters.
     *
     * @see StringBuilder#ensureCapacity(int)
     */
    public synchronized void ensureCapacity(int minimumCapacity) {
        this.buf.ensureCapacity(minimumCapacity);
    }

    /**
     * Give back the room that is not being used.
     *
     * @see StringBuilder#trimToSize()
     */
    public synchronized void trimToSize() {
        this.buf.trimToSize();
    }

    /**
     * Set the length, truncating or padding with NUL characters.
     *
     * @see StringBuilder#setLength(int)
     */
    public synchronized void setLength(int newLength) {
        this.buf.setLength(newLength);
    }


    // ---- reading ----

    /**
     * How many characters are in the buffer.
     *
     * @see StringBuilder#length()
     */
    public synchronized int length() {
        return this.buf.length();
    }

    /**
     * The character at {@code index}.
     *
     * @see StringBuilder#charAt(int)
     */
    public synchronized char charAt(int index) {
        return this.buf.charAt(index);
    }

    /**
     * Overwrite the character at {@code index}.
     *
     * @see StringBuilder#setCharAt(int, char)
     */
    public synchronized void setCharAt(int index, char ch) {
        this.buf.setCharAt(index, ch);
    }

    /**
     * Copy {@code [srcBegin, srcEnd)} into {@code dst} starting at {@code dstBegin}.
     *
     * @see StringBuilder#getChars(int, int, char[], int)
     */
    public synchronized void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        this.buf.getChars(srcBegin, srcEnd, dst, dstBegin);
    }

    /**
     * The code point beginning at {@code index}.
     *
     * @see StringBuilder#codePointAt(int)
     */
    public synchronized int codePointAt(int index) {
        return this.buf.codePointAt(index);
    }

    /**
     * The code point ending just before {@code index}.
     *
     * @see StringBuilder#codePointBefore(int)
     */
    public synchronized int codePointBefore(int index) {
        return this.buf.codePointBefore(index);
    }

    /**
     * How many code points {@code [beginIndex, endIndex)} holds.
     *
     * @see StringBuilder#codePointCount(int, int)
     */
    public synchronized int codePointCount(int beginIndex, int endIndex) {
        return this.buf.codePointCount(beginIndex, endIndex);
    }

    /**
     * The index {@code codePointOffset} code points away from {@code index}.
     *
     * @see StringBuilder#offsetByCodePoints(int, int)
     */
    public synchronized int offsetByCodePoints(int index, int codePointOffset) {
        return this.buf.offsetByCodePoints(index, codePointOffset);
    }


    // ---- appending ----

    /**
     * Append one character.
     *
     * @see StringBuilder#append(char)
     */
    public synchronized StringBuffer append(char c) {
        this.buf.append(c);
        return this;
    }

    /**
     * Append a string, or the four characters {@code null}.
     *
     * @see StringBuilder#append(String)
     */
    public synchronized StringBuffer append(String str) {
        this.buf.append(str);
        return this;
    }

    /**
     * Append every character of {@code str}.
     *
     * @see StringBuilder#append(char[])
     */
    public synchronized StringBuffer append(char[] str) {
        this.buf.append(str);
        return this;
    }

    /**
     * Append {@code len} characters of {@code str} starting at {@code offset}.
     *
     * @see StringBuilder#append(char[], int, int)
     */
    public synchronized StringBuffer append(char[] str, int offset, int len) {
        this.buf.append(str, offset, len);
        return this;
    }

    /**
     * Append the string form of {@code obj}, or {@code null}.
     *
     * @see StringBuilder#append(Object)
     */
    public synchronized StringBuffer append(Object obj) {
        this.buf.append(obj);
        return this;
    }

    /**
     * Append {@code true} or {@code false}.
     *
     * @see StringBuilder#append(boolean)
     */
    public synchronized StringBuffer append(boolean b) {
        this.buf.append(b);
        return this;
    }

    /**
     * Append a character sequence, or the four characters {@code null}.
     *
     * @see StringBuilder#append(CharSequence)
     */
    public synchronized StringBuffer append(CharSequence s) {
        this.buf.append(s);
        return this;
    }

    /**
     * Append {@code [start, end)} of a character sequence.
     *
     * @see StringBuilder#append(CharSequence, int, int)
     */
    public synchronized StringBuffer append(CharSequence s, int start, int end) {
        this.buf.append(s, start, end);
        return this;
    }

    /**
     * Append the contents of another buffer, or the four characters {@code null}.
     *
     * @see StringBuilder#append(StringBuffer)
     */
    public synchronized StringBuffer append(StringBuffer sb) {
        this.buf.append(sb);
        return this;
    }

    /**
     * Append the decimal form of an int.
     *
     * @see StringBuilder#append(int)
     */
    public synchronized StringBuffer append(int i) {
        this.buf.append(i);
        return this;
    }

    /**
     * Append the decimal form of a long.
     *
     * @see StringBuilder#append(long)
     */
    public synchronized StringBuffer append(long l) {
        this.buf.append(l);
        return this;
    }

    /**
     * Append the shortest decimal that reads back as {@code f}.
     *
     * @see StringBuilder#append(float)
     */
    public synchronized StringBuffer append(float f) {
        this.buf.append(f);
        return this;
    }

    /**
     * Append the shortest decimal that reads back as {@code d}.
     *
     * @see StringBuilder#append(double)
     */
    public synchronized StringBuffer append(double d) {
        this.buf.append(d);
        return this;
    }

    /**
     * Append a code point, as one character or as a surrogate pair.
     *
     * @see StringBuilder#appendCodePoint(int)
     */
    public synchronized StringBuffer appendCodePoint(int codePoint) {
        this.buf.appendCodePoint(codePoint);
        return this;
    }


    // ---- removing ----

    /**
     * Remove {@code [start, end)}.
     *
     * @see StringBuilder#delete(int, int)
     */
    public synchronized StringBuffer delete(int start, int end) {
        this.buf.delete(start, end);
        return this;
    }

    /**
     * Remove the character at {@code index}.
     *
     * @see StringBuilder#deleteCharAt(int)
     */
    public synchronized StringBuffer deleteCharAt(int index) {
        this.buf.deleteCharAt(index);
        return this;
    }

    /**
     * Replace {@code [start, end)} with {@code str}.
     *
     * @see StringBuilder#replace(int, int, String)
     */
    public synchronized StringBuffer replace(int start, int end, String str) {
        this.buf.replace(start, end, str);
        return this;
    }


    // ---- inserting ----

    // The six that are not synchronized do nothing but convert their argument and hand it to
    // a sibling that is. Taking the monitor here as well would be a second acquisition for no
    // additional guarantee.

    /**
     * Insert {@code str} at {@code offset}.
     *
     * @see StringBuilder#insert(int, String)
     */
    public synchronized StringBuffer insert(int offset, String str) {
        this.buf.insert(offset, str);
        return this;
    }

    /**
     * Insert every character of {@code str} at {@code offset}.
     *
     * @see StringBuilder#insert(int, char[])
     */
    public synchronized StringBuffer insert(int offset, char[] str) {
        this.buf.insert(offset, str);
        return this;
    }

    /**
     * Insert {@code len} characters of {@code str}, from {@code strOffset}, at {@code index}.
     *
     * @see StringBuilder#insert(int, char[], int, int)
     */
    public synchronized StringBuffer insert(int index, char[] str, int strOffset, int len) {
        this.buf.insert(index, str, strOffset, len);
        return this;
    }

    /**
     * Insert the string form of {@code obj} at {@code offset}.
     *
     * @see StringBuilder#insert(int, Object)
     */
    public synchronized StringBuffer insert(int offset, Object obj) {
        this.buf.insert(offset, obj);
        return this;
    }

    /**
     * Insert {@code [start, end)} of a character sequence at {@code dstOffset}.
     *
     * @see StringBuilder#insert(int, CharSequence, int, int)
     */
    public synchronized StringBuffer insert(int dstOffset, CharSequence s, int start, int end) {
        this.buf.insert(dstOffset, s, start, end);
        return this;
    }

    /**
     * Insert one character at {@code offset}.
     *
     * @see StringBuilder#insert(int, char)
     */
    public synchronized StringBuffer insert(int offset, char c) {
        this.buf.insert(offset, c);
        return this;
    }

    /**
     * Insert a character sequence at {@code dstOffset}.
     *
     * @see StringBuilder#insert(int, CharSequence)
     */
    public StringBuffer insert(int dstOffset, CharSequence s) {
        this.buf.insert(dstOffset, s);
        return this;
    }

    /**
     * Insert {@code true} or {@code false} at {@code offset}.
     *
     * @see StringBuilder#insert(int, boolean)
     */
    public StringBuffer insert(int offset, boolean b) {
        this.buf.insert(offset, b);
        return this;
    }

    /**
     * Insert the decimal form of an int at {@code offset}.
     *
     * @see StringBuilder#insert(int, int)
     */
    public StringBuffer insert(int offset, int i) {
        this.buf.insert(offset, i);
        return this;
    }

    /**
     * Insert the decimal form of a long at {@code offset}.
     *
     * @see StringBuilder#insert(int, long)
     */
    public StringBuffer insert(int offset, long l) {
        this.buf.insert(offset, l);
        return this;
    }

    /**
     * Insert the decimal form of a float at {@code offset}.
     *
     * @see StringBuilder#insert(int, float)
     */
    public StringBuffer insert(int offset, float f) {
        this.buf.insert(offset, f);
        return this;
    }

    /**
     * Insert the decimal form of a double at {@code offset}.
     *
     * @see StringBuilder#insert(int, double)
     */
    public StringBuffer insert(int offset, double d) {
        this.buf.insert(offset, d);
        return this;
    }


    // ---- repeating ----

    /**
     * Append {@code times} copies of a code point.
     *
     * @see StringBuilder#repeat(int, int)
     */
    public synchronized StringBuffer repeat(int codePoint, int times) {
        this.buf.repeat(codePoint, times);
        return this;
    }

    /**
     * Append {@code times} copies of a character sequence.
     *
     * @see StringBuilder#repeat(CharSequence, int)
     */
    public synchronized StringBuffer repeat(CharSequence cs, int times) {
        this.buf.repeat(cs, times);
        return this;
    }


    // ---- searching ----

    /**
     * The first index at which {@code str} occurs, or -1.
     *
     * @see StringBuilder#indexOf(String)
     */
    public int indexOf(String str) {
        return this.buf.indexOf(str);
    }

    /**
     * The first index at or after {@code fromIndex} at which {@code str} occurs, or -1.
     *
     * @see StringBuilder#indexOf(String, int)
     */
    public synchronized int indexOf(String str, int fromIndex) {
        return this.buf.indexOf(str, fromIndex);
    }

    /**
     * The last index at which {@code str} occurs, or -1.
     *
     * @see StringBuilder#lastIndexOf(String)
     */
    public int lastIndexOf(String str) {
        return this.buf.lastIndexOf(str);
    }

    /**
     * The last index at or before {@code fromIndex} at which {@code str} occurs, or -1.
     *
     * @see StringBuilder#lastIndexOf(String, int)
     */
    public synchronized int lastIndexOf(String str, int fromIndex) {
        return this.buf.lastIndexOf(str, fromIndex);
    }


    // ---- reversing ----

    /**
     * Reverse the buffer in place, keeping surrogate pairs unbroken.
     *
     * @see StringBuilder#reverse()
     */
    public synchronized StringBuffer reverse() {
        this.buf.reverse();
        return this;
    }


    // ---- reading out ----

    /**
     * The contents from {@code start} to the end, as a string.
     *
     * @see StringBuilder#substring(int)
     */
    public synchronized String substring(int start) {
        return this.buf.substring(start);
    }

    /**
     * {@code [start, end)} as a string.
     *
     * @see StringBuilder#substring(int, int)
     */
    public synchronized String substring(int start, int end) {
        return this.buf.substring(start, end);
    }

    /**
     * {@code [start, end)} as a character sequence.
     *
     * @see StringBuilder#subSequence(int, int)
     */
    public synchronized CharSequence subSequence(int start, int end) {
        return this.buf.subSequence(start, end);
    }

    /**
     * The contents as a string.
     *
     * @see StringBuilder#toString()
     */
    public synchronized String toString() {
        return this.buf.toString();
    }


    // ---- the streams, and the lock they still take ----
    //
    // Neither is synchronized, which matches the JDK -- and neither reads the buffer without the
    // lock either, because the work happens in a private method that IS synchronized. The public
    // flag and the actual guarantee are separate things here, and this is the shape that gets
    // both right.

    /** The characters as ints, zero-extended. */
    public IntStream chars() {
        return this.lockedChars();
    }

    private synchronized IntStream lockedChars() {
        return this.buf.chars();
    }

    /** The code points as ints. */
    public IntStream codePoints() {
        return this.lockedCodePoints();
    }

    private synchronized IntStream lockedCodePoints() {
        return this.buf.codePoints();
    }

    /**
     * Compare two buffers lexicographically, by contents.
     *
     * <p>Takes only THIS buffer's monitor, not the other one's -- taking both would be a lock
     * ordering problem and a deadlock waiting for two threads to compare the same pair in
     * opposite directions. So the other buffer can change underneath the comparison, and the
     * answer is about some state it had rather than the state it ends in. The JDK makes the same
     * trade.
     *
     * <p>And note this is not {@code equals}: {@code StringBuffer} does not override it, because
     * a mutable object cannot have a stable hash code.
     *
     * @param another what to compare against
     */
    public synchronized int compareTo(StringBuffer another) {
        return this.buf.compareTo(another.contents());
    }

    // The other buffer's characters, for compareTo. Package-private and not a getter anybody
    // else should reach for: it hands out the live builder.
    StringBuilder contents() {
        return this.buf;
    }
}
