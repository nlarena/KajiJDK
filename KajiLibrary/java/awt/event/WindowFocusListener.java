package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a window gained or lost the focus.
 */
public interface WindowFocusListener extends EventListener {

    /** The window gained the focus. */
    void windowGainedFocus(WindowEvent e);

    /** The window lost the focus. */
    void windowLostFocus(WindowEvent e);
}
