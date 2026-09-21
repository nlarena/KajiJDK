package java.awt;

/**
 * An event that knows how to dispatch itself.
 *
 * <p>Normally the event queue decides whom to deliver each event to. One that implements this
 * dispatches itself: the queue calls its {@link #dispatch} and that is it.
 *
 * <p>It is what makes {@link java.awt.event.InvocationEvent} possible, and with it putting work to
 * run on the event thread from another thread — which is the only legitimate way to touch the
 * interface from outside.
 */
public interface ActiveEvent {

    /** Dispatches the event. */
    void dispatch();
}
