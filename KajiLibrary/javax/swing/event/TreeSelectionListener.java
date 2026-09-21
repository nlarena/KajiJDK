package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that the tree's selection changed.
 */
public interface TreeSelectionListener extends EventListener {

    /** The selection changed. */
    void valueChanged(TreeSelectionEvent e);
}
