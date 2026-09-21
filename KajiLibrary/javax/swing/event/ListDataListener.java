package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a list's contents changed.
 */
public interface ListDataListener extends EventListener {

    /** Elements were added. */
    void intervalAdded(ListDataEvent e);

    /** Elements were removed. */
    void intervalRemoved(ListDataEvent e);

    /** Elements changed, without changing how many there are. */
    void contentsChanged(ListDataEvent e);
}
