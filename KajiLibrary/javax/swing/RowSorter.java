package javax.swing;

import java.util.List;

import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;

/**
 * The translation between the model's order and the one that is seen.
 *
 * <h2>Sorting without touching the data</h2>
 *
 * <p>A user who clicks on a column's header expects to see the rows sorted. The naive way would
 * be to reorder the model, and it would be wrong: the model belongs to the application, and the
 * order is a preference of <em>this</em> view. Two tables over the same data would have to fight
 * over it.
 *
 * <p>This class resolves that with a mapping. The model is not touched; what changes is that the
 * view's row 0 may be the model's 37. Hence the two conversion methods, and hence they are the
 * source of almost every bug of a sortable table: using a view index where a model one belonged
 * returns the wrong datum without failing.
 *
 * <p>It also filters: {@link #getViewRowCount} may be smaller than {@link #getModelRowCount}.
 *
 * @param <M> the model's type
 * @since 1.6
 */
public abstract class RowSorter<M> {

    private List<RowSorterListener> listeners;

    /** For the subclasses. */
    public RowSorter() {
        this.listeners = new java.util.ArrayList<RowSorterListener>();
    }

    /** The model whose rows are sorted. */
    public abstract M getModel();

    /** It cycles that model column's order: ascending, descending, unsorted. */
    public abstract void toggleSortOrder(int column);

    /** The model index of the row that is seen at {@code index}. */
    public abstract int convertRowIndexToModel(int index);

    /** Where the model's row {@code index} is seen, or {@code -1} if it is filtered out. */
    public abstract int convertRowIndexToView(int index);

    /** It fixes which columns it sorts by and in which direction. */
    public abstract void setSortKeys(List<? extends SortKey> keys);

    /** Which columns it sorts by. */
    public abstract List<? extends SortKey> getSortKeys();

    /** How many rows are seen, already filtered. */
    public abstract int getViewRowCount();

    /** How many rows the model has. */
    public abstract int getModelRowCount();

    /** Notice that the model changed shape completely. */
    public abstract void modelStructureChanged();

    /** Notice that the content of every row changed. */
    public abstract void allRowsChanged();

    /** Notice that rows were inserted into the model. */
    public abstract void rowsInserted(int firstRow, int endRow);

    /** Notice that rows were deleted from the model. */
    public abstract void rowsDeleted(int firstRow, int endRow);

    /** Notice that rows of the model changed. */
    public abstract void rowsUpdated(int firstRow, int endRow);

    /** Notice that a column of a range of rows changed. */
    public abstract void rowsUpdated(int firstRow, int endRow, int column);

    /** It adds a listener. */
    public void addRowSorterListener(RowSorterListener l) {
        this.listeners.add(l);
    }

    /** It removes a listener. */
    public void removeRowSorterListener(RowSorterListener l) {
        this.listeners.remove(l);
    }

    /**
     * It gives notice that which columns it sorts by changed, without the rows' order being
     * rebuilt.
     */
    protected void fireSortOrderChanged() {
        distribute(new RowSorterEvent(this));
    }

    /**
     * It gives notice that the rows were reordered.
     *
     * @param lastRowIndexToModel where each row was before, or {@code null} if it is not known. It
     *     is what allows a view to keep the selection through the reordering
     */
    protected void fireRowSorterChanged(int[] lastRowIndexToModel) {
        distribute(new RowSorterEvent(this, RowSorterEvent.Type.SORTED, lastRowIndexToModel));
    }

    private void distribute(RowSorterEvent e) {
        for (int i = this.listeners.size() - 1; i >= 0; i--) {
            this.listeners.get(i).sorterChanged(e);
        }
    }

    /**
     * Which column it sorts by and in which direction.
     *
     * <p>Immutable, and it is a list and not a single one: sorting by surname and then by first
     * name needs two keys, and the list's order is the tie-breaking one.
     */
    public static class SortKey {

        private final int column;
        private final SortOrder sortOrder;

        /**
         * @throws IllegalArgumentException if {@code sortOrder} is {@code null}
         */
        public SortKey(int column, SortOrder sortOrder) {
            if (sortOrder == null) {
                throw new IllegalArgumentException("The sort order cannot be null");
            }
            this.column = column;
            this.sortOrder = sortOrder;
        }

        /** The model's column. */
        public final int getColumn() {
            return this.column;
        }

        /** The direction. */
        public final SortOrder getSortOrder() {
            return this.sortOrder;
        }

        public int hashCode() {
            return 31 * this.column + this.sortOrder.hashCode();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof SortKey) {
                SortKey other = (SortKey) o;
                return other.column == this.column && other.sortOrder == this.sortOrder;
            }
            return false;
        }
    }
}
