package java.awt.event;

/**
 * Un {@link ContainerListener} que no hace nada, para redefinir sólo lo que interese.
 */
public abstract class ContainerAdapter implements ContainerListener {

    /** Para las subclases. */
    protected ContainerAdapter() {
    }

    /** No hace nada. */
    public void componentAdded(ContainerEvent e) {
    }

    /** No hace nada. */
    public void componentRemoved(ContainerEvent e) {
    }
}
