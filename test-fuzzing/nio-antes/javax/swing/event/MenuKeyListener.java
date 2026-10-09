package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear about the keyboard over an open menu; see {@link MenuKeyEvent}.
 */
public interface MenuKeyListener extends EventListener {

    /** A key was typed. */
    void menuKeyTyped(MenuKeyEvent e);

    /** A key was pressed. */
    void menuKeyPressed(MenuKeyEvent e);

    /** A key was released. */
    void menuKeyReleased(MenuKeyEvent e);
}
