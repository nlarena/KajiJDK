package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse del teclado sobre un menu abierto; ver {@link MenuKeyEvent}.
 */
public interface MenuKeyListener extends EventListener {

    /** Se tipeo una tecla. */
    void menuKeyTyped(MenuKeyEvent e);

    /** Se apreto una tecla. */
    void menuKeyPressed(MenuKeyEvent e);

    /** Se solto una tecla. */
    void menuKeyReleased(MenuKeyEvent e);
}
