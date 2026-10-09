package java.text;

// KajiLibrary's java.text.StringCharacterIterator — a CharacterIterator over a String.
//
// The three-index constructor is the interesting one: begin/end bound a WINDOW into the string and
// the cursor starts anywhere inside it, so a caller can iterate a substring without copying it.
// `getBeginIndex`/`getEndIndex` report that window, not the whole string.
public final class StringCharacterIterator implements CharacterIterator {

    private String text;
    private int begin;
    private int end;
    private int pos;

    public StringCharacterIterator(String text) {
        this(text, 0, text.length(), 0);
    }

    public StringCharacterIterator(String text, int pos) {
        this(text, 0, text.length(), pos);
    }

    public StringCharacterIterator(String text, int begin, int end, int pos) {
        if (text == null) {
            throw new NullPointerException();
        }
        if (begin < 0 || begin > end || end > text.length()) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        if (pos < begin || pos > end) {
            throw new IllegalArgumentException("Invalid position");
        }
        this.text = text;
        this.begin = begin;
        this.end = end;
        this.pos = pos;
    }

    // Re-points the iterator at new text, resetting the window to the whole string.
    public void setText(String text) {
        if (text == null) {
            throw new NullPointerException();
        }
        this.text = text;
        this.begin = 0;
        this.end = text.length();
        this.pos = 0;
    }

    public char first() {
        this.pos = this.begin;
        return this.current();
    }

    public char last() {
        if (this.end != this.begin) {
            this.pos = this.end - 1;
        } else {
            this.pos = this.end;
        }
        return this.current();
    }

    public char setIndex(int position) {
        if (position < this.begin || position > this.end) {
            throw new IllegalArgumentException("Invalid index");
        }
        this.pos = position;
        return this.current();
    }

    // The literal is used rather than `CharacterIterator.DONE`: reading that constant across
    // classes traps (finding #110) and its value reads back as 0 anyway (finding #112).
    public char current() {
        if (this.pos >= this.begin && this.pos < this.end) {
            return this.text.charAt(this.pos);
        }
        return '￿';
    }

    public char next() {
        if (this.pos < this.end - 1) {
            this.pos = this.pos + 1;
            return this.text.charAt(this.pos);
        }
        // Walking off the end parks the cursor AT end, so a following previous() still works.
        this.pos = this.end;
        return '￿';
    }

    public char previous() {
        if (this.pos > this.begin) {
            this.pos = this.pos - 1;
            return this.text.charAt(this.pos);
        }
        return '￿';
    }

    public int getBeginIndex() {
        return this.begin;
    }

    public int getEndIndex() {
        return this.end;
    }

    public int getIndex() {
        return this.pos;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof StringCharacterIterator) {
            StringCharacterIterator other = (StringCharacterIterator) obj;
            return this.hashCode() == other.hashCode()
                    && this.text.equals(other.text)
                    && this.pos == other.pos
                    && this.begin == other.begin
                    && this.end == other.end;
        }
        return false;
    }

    public int hashCode() {
        return this.text.hashCode() ^ this.pos ^ this.begin ^ this.end;
    }

    // Built by hand rather than through Object.clone(): a fresh instance with the same window and
    // cursor is the same thing, and it does not depend on the clone native.
    public Object clone() {
        return new StringCharacterIterator(this.text, this.begin, this.end, this.pos);
    }
}
