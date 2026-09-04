package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que un ancestro del componente cambio; ver {@link AncestorEvent}.
 */
public interface AncestorListener extends EventListener {

    /** Un ancestro se agrego a la jerarquia o se hizo visible. */
    void ancestorAdded(AncestorEvent event);

    /** Un ancestro se saco o se escondio. */
    void ancestorRemoved(AncestorEvent event);

    /** Un ancestro se movio. */
    void ancestorMoved(AncestorEvent event);
}
