package javax.swing;

import javax.swing.event.ListSelectionListener;

/**
 * Which rows of a list are selected.
 *
 * <h2>Separate from the data model, on purpose</h2>
 *
 * <p>What is in the list and what is selected are two independent questions, and separating them
 * allows two views of the same data to have different selections -- or to share one.
 *
 * <h2>{@code valueIsAdjusting}, which is what avoids a thousand repaints</h2>
 *
 * <p>Dragging the mouse over twenty rows produces twenty changes of selection. With the flag
 * switched on, whoever listens knows that <em>more are coming</em> and may wait: recomputing
 * everything at each intermediate step is work thrown away. It is switched off on releasing, and
 * that last event is the one that counts.
 */
public interface ListSelectionModel {

    /** Only one row at a time. */
    int SINGLE_SELECTION = 0;

    /** A contiguous range. */
    int SINGLE_INTERVAL_SELECTION = 1;

    /** Any combination of rows. */
    int MULTIPLE_INTERVAL_SELECTION = 2;

    /** It selects the range, discarding what was there. */
    void setSelectionInterval(int index0, int index1);

    /** It adds the range to the selection. */
    void addSelectionInterval(int index0, int index1);

    /** It removes the range from the selection. */
    void removeSelectionInterval(int index0, int index1);

    /** The smallest selected row, or {@code -1}. */
    int getMinSelectionIndex();

    /** The largest selected row, or {@code -1}. */
    int getMaxSelectionIndex();

    /** Whether that row is selected. */
    boolean isSelectedIndex(int index);

    /** The fixed end of the range that is being built. */
    int getAnchorSelectionIndex();

    /** It fixes the fixed end. */
    void setAnchorSelectionIndex(int index);

    /** The moving end of the range that is being built. */
    int getLeadSelectionIndex();

    /** It fixes the moving end. */
    void setLeadSelectionIndex(int index);

    /** It deselects everything. */
    void clearSelection();

    /** Whether there is nothing selected. */
    boolean isSelectionEmpty();

    /** It gives notice that rows were inserted, so as to shift the selection. */
    void insertIndexInterval(int index, int length, boolean before);

    /** It gives notice that rows were deleted. */
    void removeIndexInterval(int index0, int index1);

    /** It marks that more changes are coming; see the interface note. */
    void setValueIsAdjusting(boolean valueIsAdjusting);

    /** Whether more changes are coming. */
    boolean getValueIsAdjusting();

    /** It changes the selection mode; one of the three constants. */
    void setSelectionMode(int selectionMode);

    /** The selection mode. */
    int getSelectionMode();

    /** It adds a listener. */
    void addListSelectionListener(ListSelectionListener x);

    /** It removes a listener. */
    void removeListSelectionListener(ListSelectionListener x);

    /**
     * The chosen indices, in order.
     *
     * <p>It is a method with a body in the interface -- Java 19 added it -- so that the old
     * implementations inherit it without being touched. It walks from the minimum to the maximum
     * asking one by one, which is the only thing that can be done with the interface above: a model
     * that knows better where its ranges are may override it.
     */
    default int[] getSelectedIndices() {
        int iMin = getMinSelectionIndex();
        int iMax = getMaxSelectionIndex();
        if (iMin < 0 || iMax < 0) {
            return new int[0];
        }
        int[] rvTmp = new int[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (isSelectedIndex(i)) {
                rvTmp[n] = i;
                n = n + 1;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /** How many are chosen; see {@link #getSelectedIndices}. */
    default int getSelectedItemsCount() {
        int iMin = getMinSelectionIndex();
        int iMax = getMaxSelectionIndex();
        int count = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (isSelectedIndex(i)) {
                count = count + 1;
            }
        }
        return count;
    }
}
