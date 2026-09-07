package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that the component moved place in the tree, or that whether it shows changed.
 */
public interface HierarchyListener extends EventListener {

    /** The tree changed. */
    void hierarchyChanged(HierarchyEvent e);
}
