package javax.naming.event;

import java.util.EventObject;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.event.NamingExceptionEvent -- the subscription went down.
 *
 * <p>What reaches {@link NamingListener#namingExceptionThrown}. By the time it arrives, the
 * listener <b>has already been deregistered</b>: it is not a notice that something went wrong and
 * carries on, it is the last event.
 *
 * <p>{@link #dispatch} exists so that whoever hands out events does not have to know which method
 * to call for each type: the event dispatches itself, which is what allows a queue of events of
 * different types without an {@code instanceof} for each one. (An earlier note compared this to
 * {@code java.awt.AWTEvent}; that class has no such method -- AWT picks the listener method in
 * {@code Component.processEvent}.)
 */
public class NamingExceptionEvent extends EventObject {

    private static final long serialVersionUID = -4877678086134736336L;

    /** What failed. */
    private final NamingException exception;

    /**
     * @param source the context where the subscription was
     * @param exc what failed
     */
    public NamingExceptionEvent(EventContext source, NamingException exc) {
        super(source);
        this.exception = exc;
    }

    /** What failed. */
    public NamingException getException() {
        return this.exception;
    }

    /** The context where the subscription was. */
    public EventContext getEventContext() {
        return (EventContext) getSource();
    }

    /** Dispatched to the listener. See the class note. */
    public void dispatch(NamingListener listener) {
        listener.namingExceptionThrown(this);
    }
}
