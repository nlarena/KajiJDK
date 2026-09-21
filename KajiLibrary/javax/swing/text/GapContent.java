package javax.swing.text;

import java.io.Serializable;
import java.util.Vector;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

/**
 * The usual content: an array with a moving gap.
 *
 * <h2>The gap</h2>
 *
 * <p>The array has a hole inside, and the text is the two pieces on either side. Inserting where
 * the gap is means writing a character and shrinking the gap: nothing moves. Inserting somewhere
 * else means moving the gap there first, and that does copy, but only once per place. As
 * whoever types does so in one place for a while, in practice it almost never copies.
 *
 * <p>Hence each field's name: {@code g0} and {@code g1} are the gap's edges. Every index this
 * class gives outwards is <em>logical</em> --it counts characters, not array positions-- and
 * {@link #shiftGap} is the only thing that translates.
 *
 * <h2>The marks</h2>
 *
 * <p>The {@link Position}s are kept in an ordered list of marks, each with its <em>array</em>
 * index. Keeping them like that is what keeps moving the gap from having to touch them: only
 * those that fall inside the moved stretch are fixed up. Those nobody uses any more are
 * discarded when the list grows.
 */
public class GapContent implements AbstractDocument.Content, Serializable {

    /** How much the array grows when it fills up. */
    static final int GROWTH_SIZE = 1024 * 512;

    private char[] array;
    private int g0;
    private int g1;

    private transient Vector<MarkData> marks;
    private transient MarkVector unusedMarks;

    /** A content with room for ten characters. */
    public GapContent() {
        this(10);
    }

    /** A content with that initial room; it always starts with a line ending. */
    public GapContent(int initialLength) {
        if (initialLength < 2) {
            initialLength = 2;
        }
        array = (char[]) allocateArray(initialLength);
        g0 = 1;
        g1 = initialLength;
        array[0] = '\n';
        marks = new Vector<MarkData>();
    }

    /** The array where the characters are kept; a subclass could use another type. */
    protected Object allocateArray(int len) {
        return new char[len];
    }

    protected int getArrayLength() {
        return array.length;
    }

    /** It grows the array to that size, leaving the gap where it was. */
    void resize(int nsize) {
        char[] narray = new char[nsize];
        int part1 = g0;
        int part2 = array.length - g1;
        System.arraycopy(array, 0, narray, 0, part1);
        System.arraycopy(array, g1, narray, nsize - part2, part2);
        g1 = nsize - part2;
        array = narray;
    }

    /** The logical length: the array minus the gap. */
    public int length() {
        return array.length - (g1 - g0);
    }

    public UndoableEdit insertString(int where, String str) throws BadLocationException {
        if (where > length() || where < 0) {
            throw new BadLocationException("Invalid insert", length());
        }
        char[] chars = str.toCharArray();
        replace(where, 0, chars, chars.length);
        return new InsertUndo(where, str.length());
    }

    public UndoableEdit remove(int where, int nitems) throws BadLocationException {
        if (where + nitems >= length()) {
            throw new BadLocationException("Invalid remove", length() + 1);
        }
        String removedString = getString(where, nitems);
        UndoableEdit edit = new RemoveUndo(where, removedString);
        replace(where, nitems, null, 0);
        return edit;
    }

    public String getString(int where, int len) throws BadLocationException {
        Segment s = new Segment();
        getChars(where, len, s);
        return new String(s.array, s.offset, s.count);
    }

    /**
     * The text in the segment, without copying if it can.
     *
     * <p>If the requested stretch falls entirely on one side of the gap, the segment points at the
     * array. If it crosses it, it has to copy: there is no way of seeing two separate pieces as a
     * single one.
     */
    public void getChars(int where, int len, Segment chars) throws BadLocationException {
        int end = where + len;
        if (where < 0 || end < 0) {
            throw new BadLocationException("Invalid location", -1);
        }
        if (end > length() || where > length()) {
            throw new BadLocationException("Invalid location", length() + 1);
        }
        if ((where + len) <= g0) {
            chars.array = array;
            chars.offset = where;
        } else if (where >= g0) {
            chars.array = array;
            chars.offset = g1 + where - g0;
        } else {
            int before = g0 - where;
            char[] copy = new char[len];
            System.arraycopy(array, where, copy, 0, before);
            System.arraycopy(array, g1, copy, before, len - before);
            chars.array = copy;
            chars.offset = 0;
        }
        chars.count = len;
    }

    public Position createPosition(int offset) throws BadLocationException {
        if (offset < 0 || offset > length()) {
            throw new BadLocationException("Invalid position offset", offset);
        }
        int g0 = this.g0;
        int g1 = this.g1;
        int index = (offset < g0) ? offset : (offset + (g1 - g0));
        removeUnusedMarks();
        int sortIndex = findSortIndex(index);
        MarkData m = new MarkData(index);
        marks.insertElementAt(m, sortIndex);
        return new StickyPosition(m);
    }

    /** It replaces a stretch with another; it is the only write, and the one that moves the gap. */
    void replace(int position, int rmSize, char[] addItems, int addSize) {
        if (rmSize > 0) {
            removeChars(position, rmSize);
        }
        if (addSize > 0) {
            insert(position, addItems, addSize);
        }
    }

    /**
     * It puts characters at that position.
     *
     * <p>It takes the gap there and writes inside it. A mark that was right at the position stays
     * before or moves after according to which side of the gap its index is on, which is how this
     * class keeps "this mark sticks to the left or to the right".
     */
    private void insert(int where, char[] chars, int len) {
        if (len > (g1 - g0)) {
            shiftEnd(getNewArraySize(array.length + len - (g1 - g0)));
        }
        shiftGap(where);
        System.arraycopy(chars, 0, array, g0, len);
        g0 = g0 + len;
    }

    /** It takes characters out: it takes the gap there and grows it over them. */
    private void removeChars(int where, int nitems) {
        shiftGap(where);
        shiftGapEndUp(g1 + nitems);
    }

    /** How much to ask for when growing: double, or the big jump if it is already big. */
    int getNewArraySize(int reqSize) {
        if (reqSize < GROWTH_SIZE) {
            return Math.max(2 * reqSize, 4);
        }
        return reqSize + GROWTH_SIZE;
    }

    /** It grows the array; the marks after the gap shift with it. */
    protected void shiftEnd(int newSize) {
        int oldGapEnd = g1;
        resize(newSize);
        int dg = g1 - oldGapEnd;
        int adjustIndex = findMarkAdjustIndex(oldGapEnd);
        int n = marks.size();
        for (int i = adjustIndex; i < n; i++) {
            marks.elementAt(i).index = marks.elementAt(i).index + dg;
        }
    }

    /**
     * It moves the gap to that logical position.
     *
     * <p>It is the only copying of characters this class does, and the reason that inserting in the
     * same place many times in a row is cheap: the first one moves the gap and the rest do not.
     */
    protected void shiftGap(int newGapStart) {
        if (newGapStart == g0) {
            return;
        }
        int oldGapStart = g0;
        int dg = newGapStart - oldGapStart;
        int oldGapEnd = g1;
        int newGapEnd = oldGapEnd + dg;
        int gapSize = oldGapEnd - oldGapStart;

        if (dg > 0) {
            System.arraycopy(array, oldGapEnd, array, oldGapStart, dg);
        } else {
            System.arraycopy(array, newGapStart, array, newGapEnd, -dg);
        }
        g0 = newGapStart;
        g1 = newGapEnd;

        // Only the marks of the stretch the gap crossed are touched: the rest go on holding.
        if (dg > 0) {
            int adjustIndex = findMarkAdjustIndex(oldGapStart);
            int n = marks.size();
            for (int i = adjustIndex; i < n; i++) {
                MarkData mark = marks.elementAt(i);
                if (mark.index >= newGapEnd) {
                    break;
                }
                mark.index = mark.index - gapSize;
            }
        } else if (dg < 0) {
            int adjustIndex = findMarkAdjustIndex(newGapStart);
            int n = marks.size();
            for (int i = adjustIndex; i < n; i++) {
                MarkData mark = marks.elementAt(i);
                if (mark.index >= oldGapEnd) {
                    break;
                }
                mark.index = mark.index + gapSize;
            }
        }
        resetMarksAtZero();
    }

    /**
     * The marks at the beginning stick to the left.
     *
     * <p>With the gap at the beginning, any mark that falls inside is logically at zero, and has to
     * be pinned there: otherwise, inserting at the start of the document would push the mark of
     * position zero, which by definition does not move.
     */
    protected void resetMarksAtZero() {
        if (marks == null || g0 != 0) {
            return;
        }
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            MarkData mark = marks.elementAt(i);
            if (mark.index <= g1) {
                mark.index = 0;
            }
        }
    }

    protected void shiftGapStartDown(int newGapStart) {
        shiftGap(newGapStart);
    }

    /** It grows the gap forwards; the marks it eats are left at its edge. */
    protected void shiftGapEndUp(int newGapEnd) {
        int adjustIndex = findMarkAdjustIndex(g1);
        int n = marks.size();
        for (int i = adjustIndex; i < n; i++) {
            MarkData mark = marks.elementAt(i);
            if (mark.index >= newGapEnd) {
                break;
            }
            mark.index = newGapEnd;
        }
        g1 = newGapEnd;
    }

    /** It compares two marks by their array index. */
    final int compare(MarkData o1, MarkData o2) {
        if (o1.index < o2.index) {
            return -1;
        } else if (o1.index > o2.index) {
            return 1;
        }
        return 0;
    }

    /** Where the stretch of marks that has to be fixed up from that index starts. */
    final int findMarkAdjustIndex(int searchIndex) {
        int index = findSortIndex(searchIndex);
        // Every mark with the same index has to end up on the same side.
        for (int i = index - 1; i >= 0; i--) {
            if (marks.elementAt(i).index != searchIndex) {
                break;
            }
            index = i;
        }
        return index;
    }

    /** The place a mark with that index goes in, by binary search. */
    final int findSortIndex(int index) {
        int lower = 0;
        int upper = marks.size() - 1;
        int mid = 0;

        if (upper == -1) {
            return 0;
        }

        while (lower <= upper) {
            mid = lower + ((upper - lower) / 2);
            int i = marks.elementAt(mid).index;
            if (i == index) {
                return mid;
            } else if (i < index) {
                lower = mid + 1;
            } else {
                upper = mid - 1;
            }
        }
        if (marks.elementAt(mid).index < index) {
            mid = mid + 1;
        }
        return mid;
    }

    /** It takes out of the list the marks nobody uses any more. */
    final void removeUnusedMarks() {
        int n = marks.size();
        MarkVector cleaned = new MarkVector(n);
        for (int i = 0; i < n; i++) {
            MarkData mark = marks.elementAt(i);
            if (mark.refCount > 0) {
                cleaned.addElement(mark);
            }
        }
        if (cleaned.size() != n) {
            marks = cleaned;
        }
    }

    /** The marks that fall in that range, to put them back when undoing. */
    protected Vector<UndoPosRef> getPositionsInRange(Vector<UndoPosRef> v, int offset,
            int length) {
        int endOffset = offset + length;
        int startIndex;
        int endIndex;
        int g0 = this.g0;
        int g1 = this.g1;

        if (offset < g0) {
            if (offset == 0) {
                startIndex = 0;
            } else {
                startIndex = findMarkAdjustIndex(offset);
            }
            if (endOffset >= g0) {
                endIndex = findMarkAdjustIndex(endOffset + (g1 - g0));
            } else {
                endIndex = findMarkAdjustIndex(endOffset);
            }
        } else {
            startIndex = findMarkAdjustIndex(offset + (g1 - g0));
            endIndex = findMarkAdjustIndex(endOffset + (g1 - g0));
        }

        Vector<UndoPosRef> placeIn = (v == null) ? new Vector<UndoPosRef>(Math.max(1,
                endIndex - startIndex)) : v;
        for (int counter = startIndex; counter < endIndex; counter++) {
            placeIn.addElement(new UndoPosRef(marks.elementAt(counter)));
        }
        return placeIn;
    }

    /** It puts each mark back where it was. */
    protected void updateUndoPositions(Vector<UndoPosRef> positions, int offset, int length) {
        for (int counter = positions.size() - 1; counter >= 0; counter--) {
            UndoPosRef ref = positions.elementAt(counter);
            ref.resetLocation(offset, length);
        }
    }

    /** It translates an array index into a logical one. */
    private int logicalIndex(int arrayIndex) {
        if (arrayIndex < g0) {
            return arrayIndex;
        }
        return arrayIndex - (g1 - g0);
    }

    /** A list of marks; it exists so that it can be created with a capacity. */
    static class MarkVector extends Vector<MarkData> {

        MarkVector(int size) {
            super(size);
        }
    }

    /**
     * A mark: an array index with a reference count.
     *
     * <p>Static and not inner because of finding #508; the content goes as a parameter where it is
     * needed.
     */
    static final class MarkData {

        MarkData(int index) {
            this.index = index;
        }

        int index;
        int refCount;
    }

    /** A position that follows the text; it translates the array index into a logical one. */
    final class StickyPosition implements Position {

        StickyPosition(MarkData mark) {
            this.mark = mark;
            mark.refCount = mark.refCount + 1;
        }

        public int getOffset() {
            return logicalIndex(mark.index);
        }

        protected void finalize() throws Throwable {
            mark.refCount = mark.refCount - 1;
        }

        public String toString() {
            return Integer.toString(getOffset());
        }

        MarkData mark;
    }

    /** Where a mark was before an edit; static because of #508. */
    static final class UndoPosRef {

        UndoPosRef(MarkData rec) {
            this.rec = rec;
            this.undoLocation = rec.index;
        }

        protected void resetLocation(int startOffset, int length) {
            rec.index = undoLocation;
        }

        protected MarkData rec;
        protected int undoLocation;
    }

    /** Undoing an insertion is removing what was inserted. */
    class InsertUndo extends AbstractUndoableEdit {

        protected InsertUndo(int offset, int length) {
            super();
            this.offset = offset;
            this.length = length;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                string = getString(offset, length);
                posRefs = getPositionsInRange(null, offset, length);
                remove(offset, length);
            } catch (BadLocationException bl) {
                throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                insertString(offset, string);
                string = null;
                if (posRefs != null) {
                    updateUndoPositions(posRefs, offset, length);
                    posRefs = null;
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

    /** Undoing a removal is putting back the text and the marks that were left inside. */
    class RemoveUndo extends AbstractUndoableEdit {

        protected RemoveUndo(int offset, String string) {
            super();
            this.offset = offset;
            this.string = string;
            this.length = string.length();
            posRefs = getPositionsInRange(null, offset, length);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            try {
                insertString(offset, string);
                if (posRefs != null) {
                    updateUndoPositions(posRefs, offset, length);
                    posRefs = null;
                }
                string = null;
            } catch (BadLocationException bl) {
                throw new CannotUndoException();
            }
        }

        public void redo() throws CannotRedoException {
            super.redo();
            try {
                string = getString(offset, length);
                posRefs = getPositionsInRange(null, offset, length);
                remove(offset, length);
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
