package javax.swing.event;

import java.util.EventObject;

import javax.swing.table.TableColumnModel;

/**
 * A table's columns changed: one was added, removed or moved.
 *
 * <p>The two indices are read differently according to what happened, and it is the class's trap:
 * when moving, they are from where and to where; when adding or removing, they are the same
 * number repeated. Which of the three it was is said by the
 * {@link TableColumnModelListener} method it reaches, not by the event.
 */
public class TableColumnModelEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** From where. */
    protected int fromIndex;

    /** To where. */
    protected int toIndex;

    public TableColumnModelEvent(TableColumnModel source, int from, int to) {
        super(source);
        this.fromIndex = from;
        this.toIndex = to;
    }

    /** Where the column came from. */
    public int getFromIndex() {
        return this.fromIndex;
    }

    /** Where it went. */
    public int getToIndex() {
        return this.toIndex;
    }
}
