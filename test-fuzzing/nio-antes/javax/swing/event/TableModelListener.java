package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a table's data changed.
 */
public interface TableModelListener extends EventListener {

    /** The model changed; the event says which part. */
    void tableChanged(TableModelEvent e);
}
