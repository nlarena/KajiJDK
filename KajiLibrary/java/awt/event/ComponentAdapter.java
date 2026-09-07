package java.awt.event;

/**
 * A {@link ComponentListener} that does nothing, to override only what matters.
 */
public abstract class ComponentAdapter implements ComponentListener {

    /** For the subclasses. */
    protected ComponentAdapter() {
    }

    /** Does nothing. */
    public void componentResized(ComponentEvent e) {
    }

    /** Does nothing. */
    public void componentMoved(ComponentEvent e) {
    }

    /** Does nothing. */
    public void componentShown(ComponentEvent e) {
    }

    /** Does nothing. */
    public void componentHidden(ComponentEvent e) {
    }
}
