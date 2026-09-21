package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that something changed; see {@link ChangeEvent}.
 */
public interface ChangeListener extends EventListener {

    /** Something changed in {@code e.getSource()}. */
    void stateChanged(ChangeEvent e);
}
