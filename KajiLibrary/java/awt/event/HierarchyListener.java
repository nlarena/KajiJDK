package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que el componente cambió de lugar en el árbol, o de que cambió si se ve.
 */
public interface HierarchyListener extends EventListener {

    /** Cambió el árbol. */
    void hierarchyChanged(HierarchyEvent e);
}
