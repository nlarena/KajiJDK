package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que algo cambio; ver {@link ChangeEvent}.
 */
public interface ChangeListener extends EventListener {

    /** Algo cambio en {@code e.getSource()}. */
    void stateChanged(ChangeEvent e);
}
