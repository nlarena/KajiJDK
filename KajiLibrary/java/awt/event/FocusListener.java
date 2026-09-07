package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a component gained or lost the keyboard focus.
 */
public interface FocusListener extends EventListener {

    /** It gained the focus. */
    void focusGained(FocusEvent e);

    /** It lost the focus. */
    void focusLost(FocusEvent e);
}
