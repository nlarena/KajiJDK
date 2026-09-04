package java.awt.event;

/**
 * Un {@link HierarchyBoundsListener} que no hace nada, para redefinir sólo lo que interese.
 */
public abstract class HierarchyBoundsAdapter implements HierarchyBoundsListener {

    /** Para las subclases. */
    protected HierarchyBoundsAdapter() {
    }

    /** No hace nada. */
    public void ancestorMoved(HierarchyEvent e) {
    }

    /** No hace nada. */
    public void ancestorResized(HierarchyEvent e) {
    }
}
