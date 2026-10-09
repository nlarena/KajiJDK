package org.w3c.dom.events;

/**
 * KajiLibrary's org.w3c.dom.events.EventException -- an event that cannot be dispatched.
 *
 * <p>It has one single code, and that already says how the module is conceived: almost everything
 * that can go wrong in the dispatch is the responsibility of the listeners, not of the dispatcher.
 * The only thing the dispatcher can reject is an event <b>with no type</b> -- without it there is
 * nobody to deliver it to.
 *
 * <p>Unchecked and with the code in a public field, by the same DOM convention as
 * {@code DOMException}.
 */
public class EventException extends RuntimeException {

    private static final long serialVersionUID = 3728411136506952248L;

    /** The type of the event is null or empty: there is nobody to deliver it to. */
    public static final short UNSPECIFIED_EVENT_TYPE_ERR = 0;

    /** Which one. Public by the DOM convention. */
    public short code;

    public EventException(short code, String message) {
        super(message);
        this.code = code;
    }
}
