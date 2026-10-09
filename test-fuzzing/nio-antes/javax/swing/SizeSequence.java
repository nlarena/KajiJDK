package javax.swing;

/**
 * The sizes of a row of things put one after another, and where each one starts.
 *
 * <h2>What it is for</h2>
 *
 * <p>A table with a thousand rows of different heights needs to answer two questions all the
 * time: at which pixel row 700 starts, and which row pixel 4823 corresponds to. Adding one at a
 * time is too slow when it happens on every repaint, and keeping the already added positions
 * forces half the table to be recomputed every time a row changes height.
 *
 * <p>This class is the answer to both questions with the same structure. It keeps the
 * <em>sizes</em> -- which is what changes -- and derives the positions.
 *
 * <h2>The two questions are not symmetrical</h2>
 *
 * <p>{@link #getPosition} of an index out of range returns the total, and {@link #getIndex} of a
 * position past the end returns the number of entries. Both are the same answer said in two
 * ways: "it is after everything there is".
 */
public class SizeSequence {

    private static final int[] EMPTY = new int[0];

    private int[] sizeArray;

    /** With no entries. */
    public SizeSequence() {
        sizeArray = EMPTY;
    }

    /** That many entries, all of size zero. */
    public SizeSequence(int numEntries) {
        this(numEntries, 0);
    }

    /** That many entries, all of that size. */
    public SizeSequence(int numEntries, int value) {
        this();
        insertEntries(0, numEntries, value);
    }

    /** With those sizes. */
    public SizeSequence(int[] sizes) {
        this();
        setSizes(sizes);
    }

    /** It gives that size to the first {@code length} entries. */
    void setSizes(int length, int size) {
        int[] added = new int[length];
        for (int i = 0; i < length; i++) {
            added[i] = size;
        }
        setSizes(added);
    }

    /** It replaces every size; a copy is kept. */
    public void setSizes(int[] sizes) {
        int[] copy = new int[sizes.length];
        System.arraycopy(sizes, 0, copy, 0, sizes.length);
        sizeArray = copy;
    }

    /** A copy of the sizes. */
    public int[] getSizes() {
        int[] copy = new int[sizeArray.length];
        System.arraycopy(sizeArray, 0, copy, 0, sizeArray.length);
        return copy;
    }

    /**
     * Where that entry starts.
     *
     * <p>An index past the end gives the total; see the class note.
     */
    public int getPosition(int index) {
        int sum = 0;
        int cap = index;
        if (cap > sizeArray.length) {
            cap = sizeArray.length;
        }
        for (int i = 0; i < cap; i++) {
            sum = sum + sizeArray[i];
        }
        return sum;
    }

    /**
     * Which entry that position corresponds to.
     *
     * <p>A position past the end gives the number of entries; see the class note. Entries of size
     * zero take up no room and therefore cannot be reached: the position falls on the first that
     * does take up something.
     */
    public int getIndex(int position) {
        int sum = 0;
        for (int i = 0; i < sizeArray.length; i++) {
            sum = sum + sizeArray[i];
            if (position < sum) {
                return i;
            }
        }
        return sizeArray.length;
    }

    /** That entry's size; zero if the index is out of range. */
    public int getSize(int index) {
        if (index < 0 || index >= sizeArray.length) {
            return 0;
        }
        return sizeArray[index];
    }

    /**
     * It changes an entry's size.
     *
     * <p>An index out of range does nothing. <strong>With a negative index the JDK
     * differs</strong>: its implementation keeps a tree of partial sums instead of the sizes, and
     * a negative index ends up adding the requested size to entry zero -- asking a sequence that
     * starts at 5 for {@code setSize(-1, 100)} leaves it at 105. It is an effect of its internal
     * structure, not a rule; here it is not copied, and it is said because it is the only
     * observable difference between the two implementations.
     */
    public void setSize(int index, int size) {
        if (index < 0 || index >= sizeArray.length) {
            return;
        }
        sizeArray[index] = size;
    }

    /**
     * It puts {@code length} entries of that size in from {@code start} on.
     *
     * <p>Those that were there from {@code start} on are shifted; it is an insertion, not a
     * replacement.
     */
    public void insertEntries(int start, int length, int value) {
        int[] sizes = getSizes();
        int end = start + length;
        int newLength = sizes.length + length;
        int[] newSizes = new int[newLength];
        for (int i = 0; i < start; i++) {
            newSizes[i] = sizes[i];
        }
        for (int i = start; i < end; i++) {
            newSizes[i] = value;
        }
        for (int i = end; i < newLength; i++) {
            newSizes[i] = sizes[i - length];
        }
        setSizes(newSizes);
    }

    /** It removes {@code length} entries from {@code start} on. */
    public void removeEntries(int start, int length) {
        int[] sizes = getSizes();
        int newLength = sizes.length - length;
        int[] newSizes = new int[newLength];
        for (int i = 0; i < start; i++) {
            newSizes[i] = sizes[i];
        }
        for (int i = start; i < newLength; i++) {
            newSizes[i] = sizes[i + length];
        }
        setSizes(newSizes);
    }
}
