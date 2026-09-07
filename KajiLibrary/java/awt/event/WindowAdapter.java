package java.awt.event;

/**
 * A window listener that does nothing, to override only what matters.
 *
 * <p>It implements the three window interfaces. The classic use is overriding a single method,
 * {@code windowClosing}, to ask whether anything has to be saved before closing.
 */
public abstract class WindowAdapter implements WindowListener, WindowStateListener, WindowFocusListener {

    /** For the subclasses. */
    protected WindowAdapter() {
    }

    /** Does nothing. */
    public void windowOpened(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowClosing(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowClosed(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowIconified(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowDeiconified(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowActivated(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowDeactivated(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowStateChanged(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowGainedFocus(WindowEvent e) {
    }

    /** Does nothing. */
    public void windowLostFocus(WindowEvent e) {
    }
}
