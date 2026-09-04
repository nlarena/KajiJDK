package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que un menu se abrio o se cerro.
 */
public interface MenuListener extends EventListener {

    /** El menu se selecciono. */
    void menuSelected(MenuEvent e);

    /** El menu se deselecciono. */
    void menuDeselected(MenuEvent e);

    /** El menu se cancelo. */
    void menuCanceled(MenuEvent e);
}
