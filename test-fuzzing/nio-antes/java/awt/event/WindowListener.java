package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear about a window's life cycle.
 *
 * <p>{@code windowClosing} is the notice that the user asked to close, and it arrives **before**: it
 * is where they are asked whether they want to save. {@code windowClosed} arrives after the window no
 * longer exists.
 */
public interface WindowListener extends EventListener {

    /** The window was opened for the first time. */
    void windowOpened(WindowEvent e);

    /** The user asked to close it. */
    void windowClosing(WindowEvent e);

    /** The window was closed. */
    void windowClosed(WindowEvent e);

    /** It was minimized. */
    void windowIconified(WindowEvent e);

    /** It was restored. */
    void windowDeiconified(WindowEvent e);

    /** It became the active window. */
    void windowActivated(WindowEvent e);

    /** It stopped being the active window. */
    void windowDeactivated(WindowEvent e);
}
