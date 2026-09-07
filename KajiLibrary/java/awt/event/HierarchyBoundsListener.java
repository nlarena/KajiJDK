package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that an ancestor of the component changed size or place.
 */
public interface HierarchyBoundsListener extends EventListener {

    /** An ancestor changed place. */
    void ancestorMoved(HierarchyEvent e);

    /** An ancestor changed size. */
    void ancestorResized(HierarchyEvent e);
}
