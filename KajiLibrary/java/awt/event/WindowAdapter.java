package java.awt.event;

/**
 * Un oyente de ventana que no hace nada, para redefinir sólo lo que interese.

 <p>Implementa las tres interfaces de ventana. El uso clásico es redefinir un solo método,
 {@code windowClosing}, para preguntar si hay que guardar antes de cerrar.
 */
public abstract class WindowAdapter implements WindowListener, WindowStateListener, WindowFocusListener {

    /** Para las subclases. */
    protected WindowAdapter() {
    }

    /** No hace nada. */
    public void windowOpened(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowClosing(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowClosed(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowIconified(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowDeiconified(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowActivated(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowDeactivated(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowStateChanged(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowGainedFocus(WindowEvent e) {
    }

    /** No hace nada. */
    public void windowLostFocus(WindowEvent e) {
    }
}
