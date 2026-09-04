package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que un ancestro del componente cambió de tamaño o de lugar.
 */
public interface HierarchyBoundsListener extends EventListener {

    /** Un ancestro cambió de lugar. */
    void ancestorMoved(HierarchyEvent e);

    /** Un ancestro cambió de tamaño. */
    void ancestorResized(HierarchyEvent e);
}
