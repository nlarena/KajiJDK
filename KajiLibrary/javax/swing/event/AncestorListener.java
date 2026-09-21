package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that an ancestor of the component changed; see {@link AncestorEvent}.
 */
public interface AncestorListener extends EventListener {

    /** An ancestor was added to the hierarchy or became visible. */
    void ancestorAdded(AncestorEvent event);

    /** An ancestor was removed or hidden. */
    void ancestorRemoved(AncestorEvent event);

    /** An ancestor moved. */
    void ancestorMoved(AncestorEvent event);
}
