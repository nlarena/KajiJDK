package java.awt.event;

import java.awt.AWTEvent;
import java.util.EventListenerProxy;

/**
 * An {@link AWTEventListener} with the mask of which families interest it stuck on.
 *
 * <p>The `Toolkit` keeps every global listener in a single list, and without this there would be no
 * way of asking it **with which mask** each one registered: the listener alone does not say. Wrapping
 * it preserves that datum so that {@code getAWTEventListeners} can return it.
 */
public class AWTEventListenerProxy extends EventListenerProxy<AWTEventListener>
        implements AWTEventListener {

    private final long eventMask;

    /**
     * With the mask and the listener.
     *
     * @throws NullPointerException if the listener is `null`
     */
    public AWTEventListenerProxy(long eventMask, AWTEventListener listener) {
        super(listener);
        this.eventMask = eventMask;
    }

    /** Hands the event to the wrapped listener. */
    public void eventDispatched(AWTEvent event) {
        this.getListener().eventDispatched(event);
    }

    /** With which mask it registered. */
    public long getEventMask() {
        return this.eventMask;
    }
}
