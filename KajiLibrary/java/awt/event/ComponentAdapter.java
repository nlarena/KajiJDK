package java.awt.event;

/**
 * Un {@link ComponentListener} que no hace nada, para redefinir sólo lo que interese.
 */
public abstract class ComponentAdapter implements ComponentListener {

    /** Para las subclases. */
    protected ComponentAdapter() {
    }

    /** No hace nada. */
    public void componentResized(ComponentEvent e) {
    }

    /** No hace nada. */
    public void componentMoved(ComponentEvent e) {
    }

    /** No hace nada. */
    public void componentShown(ComponentEvent e) {
    }

    /** No hace nada. */
    public void componentHidden(ComponentEvent e) {
    }
}
