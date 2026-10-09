package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a list's selection changed.
 */
public interface ListSelectionListener extends EventListener {

    /** The selection changed. */
    void valueChanged(ListSelectionEvent e);
}
