package java.awt.event;

/**
 * A {@link HierarchyBoundsListener} that does nothing, to override only what matters.
 */
public abstract class HierarchyBoundsAdapter implements HierarchyBoundsListener {

    /** For the subclasses. */
    protected HierarchyBoundsAdapter() {
    }

    /** Does nothing. */
    public void ancestorMoved(HierarchyEvent e) {
    }

    /** Does nothing. */
    public void ancestorResized(HierarchyEvent e) {
    }
}
