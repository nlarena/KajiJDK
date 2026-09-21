package javax.swing.text;

import java.text.CharacterIterator;

/**
 * A <strong>borrowed</strong> piece of text: somebody else's array, an offset and a length.
 *
 * <h2>What it exists for</h2>
 *
 * <p>So as not to copy. Asking a document for a fragment as a {@link String} allocates and
 * copies; a {@code Segment} points at the array the document already has. In an editor that
 * happens on every repaint, so the difference shows.
 *
 * <p>The price is in the word <em>borrowed</em>: the array <strong>is not its own</strong> and
 * the document may change it. A {@code Segment} holds until the next edit, and keeping it beyond
 * that is reading memory that already means something else.
 */
public class Segment implements Cloneable, CharacterIterator, CharSequence {

    /** The array, which belongs to somebody else. */
    public char[] array;

    /** Where the piece starts. */
    public int offset;

    /** How many characters it has. */
    public int count;

    private boolean partialReturn;
    private int pos;

    /** An empty segment, with no array. */
    public Segment() {
        this(null, 0, 0);
    }

    /** A segment over {@code array}. */
    public Segment(char[] array, int offset, int count) {
        this.array = array;
        this.offset = offset;
        this.count = count;
        this.partialReturn = false;
    }

    /**
     * Whether it is accepted that the document return less than was asked for.
     *
     * <p>A document may have the text split into several arrays. With this at {@code true} it hands
     * over the first contiguous stretch instead of joining everything into a new one -- which is
     * exactly the copy this class came to avoid. Whoever turns it on has to be willing to call
     * again for what is missing.
     */
    public void setPartialReturn(boolean p) {
        this.partialReturn = p;
    }

    /** Whether a partial return is accepted. */
    public boolean isPartialReturn() {
        return this.partialReturn;
    }

    public String toString() {
        if (this.array != null) {
            return new String(this.array, this.offset, this.count);
        }
        return "";
    }

    public char first() {
        this.pos = this.offset;
        if (this.count != 0) {
            return this.array[this.pos];
        }
        return CharacterIterator.DONE;
    }

    public char last() {
        this.pos = this.offset + this.count;
        if (this.count != 0) {
            this.pos = this.pos - 1;
            return this.array[this.pos];
        }
        return CharacterIterator.DONE;
    }

    public char current() {
        if (this.count != 0 && this.pos < this.offset + this.count) {
            return this.array[this.pos];
        }
        return CharacterIterator.DONE;
    }

    public char next() {
        this.pos = this.pos + 1;
        int end = this.offset + this.count;
        if (this.pos >= end) {
            this.pos = end;
            return CharacterIterator.DONE;
        }
        return current();
    }

    public char previous() {
        if (this.pos == this.offset) {
            return CharacterIterator.DONE;
        }
        this.pos = this.pos - 1;
        return current();
    }

    public char setIndex(int position) {
        int end = this.offset + this.count;
        if (position < this.offset || position > end) {
            throw new IllegalArgumentException("position out of range");
        }
        this.pos = position;
        if (this.pos != end && this.count != 0) {
            return this.array[this.pos];
        }
        return CharacterIterator.DONE;
    }

    public int getBeginIndex() {
        return this.offset;
    }

    public int getEndIndex() {
        return this.offset + this.count;
    }

    public int getIndex() {
        return this.pos;
    }

    public char charAt(int index) {
        if (index < 0 || index >= this.count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return this.array[this.offset + index];
    }

    public int length() {
        return this.count;
    }

    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end > this.count || start > end) {
            throw new StringIndexOutOfBoundsException("subSequence out of range");
        }
        Segment s = new Segment();
        s.array = this.array;
        s.offset = this.offset + start;
        s.count = end - start;
        return s;
    }

    /**
     * A shallow copy: it shares the array.
     *
     * <p>And it has to share it. Copying the array would be exactly what this class avoids, and
     * besides it would break the relation with the document, which is where the array gets its
     * meaning from.
     */
    public Object clone() {
        Object copy = null;
        try {
            copy = super.clone();
        } catch (CloneNotSupportedException e) {
            // It cannot happen: this class implements Cloneable.
        }
        return copy;
    }
}
