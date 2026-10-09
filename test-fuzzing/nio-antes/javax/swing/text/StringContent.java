package javax.swing.text;

import java.io.Serializable;
import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * The simplest content: an array of characters that is copied whole on every edit.
 *
 * <p>It serves for small documents and for understanding what an
 * {@link AbstractDocument.Content} does without {@link GapContent}'s gap arithmetic. Every
 * insertion or removal moves everything that comes afterwards, so typing one character at a
 * time in a long document is quadratic: that is why real documents use {@code GapContent}.
 *
 * <p>The marks --what is behind each {@link Position}-- are kept in a list of integers that is
 * walked whole on every edit. Those nobody uses any more are taken out when the list grows, by
 * looking at their reference count: without that, a document edited a lot would pile up dead
 * marks for ever.
 */
public final class StringContent implements AbstractDocument.Content, Serializable {

    private static final char[] empty = new char[0];

    private char[] data;
    private int count;

    /** The live marks; see the class note. */
    transient Vector<PosRec> marks;

    /** A content with room for ten characters. */
    public StringContent() {
        this(10);
    }

    /** A content with initial room for that many; it always starts with a line ending. */
    public StringContent(int initialLength) {
        if (initialLength < 2) {
            initialLength = 2;
        }
        data = new char[initialLength];
        data[0] = '\n';
        count = 1;
    }

    public int length() {
        return count;
    }

    public UndoableEdit insertString(int where, String str) throws BadLocationException {
        if (where > count || where < 0) {
            throw new BadLocationException("Invalid location", count);
        }
        char[] chars = str.toCharArray();
        replace(where, 0, chars, 0, chars.length);
        if (marks != null) {
            updateMarksForInsert(where, str.length());
        }
        return new InsertUndo(where, str.length());
    }

    /** It returns an edit that knows how to put back what was removed; hence it keeps the text. */
    public UndoableEdit remove(int where, int nitems) throws BadLocationException {
        if (where + nitems >= count) {
            throw new BadLocationException("Invalid range", count);
        }
        String removedString = getString(where, nitems);
        UndoableEdit edit = new RemoveUndo(where, removedString);
        replace(where, nitems, empty, 0, 0);
        if (marks != null) {
            updateMarksForRemove(where, nitems);
        }
        return edit;
    }

    public String getString(int where, int len) throws BadLocationException {
        if (where + len > count) {
            throw new BadLocationException("Invalid range", count);
        }
        return new String(data, where, len);
    }

    /**
     * The text in the segment.
     *
     * <p>It points at its own array, without copying: whoever receives it must not write to it. It
     * is {@link Segment}'s deal, and what keeps drawing text from allocating memory.
     */
    public void getChars(int where, int len, Segment chars) throws BadLocationException {
        if (where + len > count) {
            throw new BadLocationException("Invalid location", count);
        }
        chars.array = data;
        chars.offset = where;
        chars.count = len;
    }

    /** A mark at that position; two equal positions share the mark. */
    public Position createPosition(int offset) throws BadLocationException {
        if (marks == null) {
            marks = new Vector<PosRec>();
        }
        return new StickyPosition(offset);
    }

    /** It replaces a stretch with another, growing the array if needed. */
    void replace(int offset, int length, char[] replArray, int replOffset, int replLength) {
        int delta = replLength - length;
        int src = offset + length;
        int nmove = count - src;
        int dest = src + delta;
        if ((count + delta) > data.length) {
            resize(count + delta);
        }
        System.arraycopy(data, src, data, dest, nmove);
        System.arraycopy(replArray, replOffset, data, offset, replLength);
        count = count + delta;
    }

    /** It grows the array to double or to what was asked for, whichever is greater. */
    void resize(int ncount) {
        char[] ndata = new char[ncount * 2 + 1];
        System.arraycopy(data, 0, ndata, 0, count);
        data = ndata;
    }

    synchronized void updateMarksForInsert(int offset, int length) {
        if (offset == 0) {
            // A mark at zero does not shift: it is the document's beginning.
            offset = 1;
        }
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                marks.removeElementAt(i);
                i = i - 1;
                n = n - 1;
            } else if (mark.offset >= offset) {
                mark.offset = mark.offset + length;
            }
        }
    }

    synchronized void updateMarksForRemove(int offset, int length) {
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                marks.removeElementAt(i);
                i = i - 1;
                n = n - 1;
            } else if (mark.offset >= (offset + length)) {
                mark.offset = mark.offset - length;
            } else if (mark.offset >= offset) {
                // A mark inside what was removed sticks to the beginning of the gap.
                mark.offset = offset;
            }
        }
    }

    /** The marks that fall in that range, so that an edit can put them back when undoing. */
    protected Vector<UndoPosRef> getPositionsInRange(Vector<UndoPosRef> v, int offset, int length) {
        int n = marks.size();
        int end = offset + length;
        Vector<UndoPosRef> placeIn = (v == null) ? new Vector<UndoPosRef>() : v;
        for (int i = 0; i < n; i++) {
            PosRec mark = marks.elementAt(i);
            if (mark.unused) {
                marks.removeElementAt(i);
                i = i - 1;
                n = n - 1;
            } else if (mark.offset >= offset && mark.offset <= end) {
                placeIn.addElement(new UndoPosRef(mark));
            }
        }
        return placeIn;
    }

    /** It puts each mark back where it was; an edit calls it when undoing itself. */
    protected void updateUndoPositions(Vector<UndoPosRef> positions) {
        for (int counter = positions.size() - 1; counter >= 0; counter--) {
            UndoPosRef ref = positions.elementAt(counter);
            ref.resetLocation();
        }
    }

    /**
     * A shared mark: the integer one or more {@link Position}s point at.
     *
     * <p>Static and not inner: it does not need the content, and our javac does not yet pass the
     * implicit outer instance when an inner class creates a sibling (#508).
     */
    static final class PosRec {

        PosRec(int offset) {
            this.offset = offset;
        }

        int offset;
        boolean unused;
    }

    /**
     * A position that sticks to the text.
     *
     * <p>When it is collected it marks its record as unused, and the next edit takes it out of the
     * list. It is the only way of not growing without limit without forcing the user to release
     * positions by hand.
     */
    final class StickyPosition implements Position {

        StickyPosition(int offset) {
            rec = new PosRec(offset);
            marks.addElement(rec);
        }

        public int getOffset() {
            return rec.offset;
        }

        protected void finalize() throws Throwable {
            rec.unused = true;
        }

        public String toString() {
            return Integer.toString(getOffset());
        }

        PosRec rec;
    }

    /** Where a mark was before an edit, to put it back when undoing; static because of #508. */
    static final class UndoPosRef {

        UndoPosRef(PosRec rec) {
            this.rec = rec;
            this.undoLocation = rec.offset;
        }

        protected void resetLocation() {
            rec.offset = undoLocation;
        }

        protected PosRec rec;
        protected int undoLocation;
    }

    /** Undoing an insertion is removing what was inserted; redoing it, putting it back. */
    class InsertUndo extends AbstractUndoableEdit {

        protected InsertUndo(int offset, int length) {
            super();
            this.offset = offset;
            this.length = length;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                synchronized (StringContent.this) {
                    if (marks != null) {
                        posRefs = getPositionsInRange(null, offset, length);
                    }
                    string = getString(offset, length);
                    remove(offset, length);
                }
            } catch (BadLocationException bl) {
                throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                synchronized (StringContent.this) {
                    insertString(offset, string);
                    string = null;
                    if (posRefs != null) {
                        updateUndoPositions(posRefs);
                        posRefs = null;
                    }
                }
            } catch (BadLocationException bl) {
                throw new CannotRedoException();
            }
        }

        protected int offset;
        protected int length;
        protected String string;
        protected Vector<UndoPosRef> posRefs;
    }

    /** Undoing a removal is putting back the text, and with it the marks that were left inside. */
    class RemoveUndo extends AbstractUndoableEdit {

        protected RemoveUndo(int offset, String string) {
            super();
            this.offset = offset;
            this.string = string;
            this.length = string.length();
            if (marks != null) {
                posRefs = getPositionsInRange(null, offset, length);
            }
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                synchronized (StringContent.this) {
                    insertString(offset, string);
                    if (posRefs != null) {
                        updateUndoPositions(posRefs);
                        posRefs = null;
                    }
                    string = null;
                }
            } catch (BadLocationException bl) {
                throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                synchronized (StringContent.this) {
                    string = getString(offset, length);
                    if (marks != null) {
                        posRefs = getPositionsInRange(null, offset, length);
                    }
                    remove(offset, length);
                }
            } catch (BadLocationException bl) {
                throw new CannotRedoException();
            }
        }

        protected int offset;
        protected int length;
        protected String string;
        protected Vector<UndoPosRef> posRefs;
    }
}
