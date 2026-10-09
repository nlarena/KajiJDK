package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a table's columns changed.
 *
 * <p>The five methods do not receive the same kind of event, and that is deliberate: adding,
 * removing and moving columns are changes of <em>structure</em> and arrive as
 * {@link TableColumnModelEvent}, while the margin is a {@link ChangeEvent} --there are no indices
 * to report-- and the selection is a {@link ListSelectionEvent}, the same one any list uses.
 *
 * <p>Reusing those last two instead of inventing events of its own is what allows one same
 * selection listener to serve for rows and for columns.
 */
public interface TableColumnModelListener extends EventListener {

    /** A column was added. */
    void columnAdded(TableColumnModelEvent e);

    /** A column was removed. */
    void columnRemoved(TableColumnModelEvent e);

    /** A column was moved. */
    void columnMoved(TableColumnModelEvent e);

    /** The space between columns changed. */
    void columnMarginChanged(ChangeEvent e);

    /** Which columns are selected changed. */
    void columnSelectionChanged(ListSelectionEvent e);
}
