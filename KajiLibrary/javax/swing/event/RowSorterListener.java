package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that the rows' order changed.
 */
public interface RowSorterListener extends EventListener {

    /** The order or the sort keys changed. */
    void sorterChanged(RowSorterEvent e);
}
