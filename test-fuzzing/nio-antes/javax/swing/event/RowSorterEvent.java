package javax.swing.event;

import java.util.EventObject;

import javax.swing.RowSorter;

/**
 * The rows' order changed.
 *
 * <p>{@link #convertPreviousRowIndexToModel} is what justifies this class: it says where each row
 * was <strong>before</strong> the reordering. Without that, a view that had row 5 selected could
 * not know which model row it was and would lose the selection on every click on the header.
 *
 * <p>It may return {@code -1}: whoever reorders does not always keep the previous mapping, and
 * saying so is better than inventing it.
 */
public class RowSorterEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private Type type;
    private int[] oldViewToModel;

    /** The sort keys changed, without the rows being redone. */
    public RowSorterEvent(RowSorter<?> source) {
        this(source, Type.SORT_ORDER_CHANGED, null);
    }

    /**
     * @param previousRowIndexToModel where each row was before, or {@code null}
     * @throws IllegalArgumentException if {@code type} is {@code null}
     */
    public RowSorterEvent(RowSorter<?> source, Type type, int[] previousRowIndexToModel) {
        super(source);
        if (type == null) {
            throw new IllegalArgumentException("The type cannot be null");
        }
        this.type = type;
        this.oldViewToModel = previousRowIndexToModel;
    }

    /** Who reordered. */
    public RowSorter<?> getSource() {
        return (RowSorter) super.getSource();
    }

    /** What kind of change it was. */
    public Type getType() {
        return this.type;
    }

    /** Which model row was seen at {@code index} before the change, or {@code -1}. */
    public int convertPreviousRowIndexToModel(int index) {
        if (this.oldViewToModel != null && index >= 0 && index < this.oldViewToModel.length) {
            return this.oldViewToModel[index];
        }
        return -1;
    }

    /** How many rows were seen before the change. */
    public int getPreviousRowCount() {
        return this.oldViewToModel == null ? 0 : this.oldViewToModel.length;
    }

    /** What kind of change it was. */
    public enum Type {

        /** The sort keys changed; the rows have not moved yet. */
        SORT_ORDER_CHANGED,
        /** The contents were reordered or filtered. */
        SORTED
    }
}
