package java.awt.event;

/**
 * A {@link ContainerListener} that does nothing, to override only what matters.
 */
public abstract class ContainerAdapter implements ContainerListener {

    /** For the subclasses. */
    protected ContainerAdapter() {
    }

    /** Does nothing. */
    public void componentAdded(ContainerEvent e) {
    }

    /** Does nothing. */
    public void componentRemoved(ContainerEvent e) {
    }
}
