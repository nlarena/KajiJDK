package org.w3c.dom.events;

/**
 * KajiLibrary's org.w3c.dom.events.EventTarget -- something that can receive events.
 *
 * <p>{@code Node} implements it, so any node of the document is one.
 *
 * <h2>useCapture is part of the identity of the registration</h2>
 *
 * <p>It is what is not obvious: registering the same listener for the same type with a different
 * {@code useCapture} is <b>two registrations</b>, and removing one does not remove the other.
 * {@link #removeEventListener} has to receive the same value it was registered with, or it finds
 * nothing and does not warn.
 *
 * <p>Registering twice with the three arguments equal, on the other hand, does <b>not</b>
 * duplicate: the second is discarded and the listener receives the event only once.
 */
public interface EventTarget {

    /**
     * It registers a listener.
     *
     * @param useCapture whether it listens in the capturing phase --going down-- instead of at
     *     target and bubbling. See the note of the class: it is part of the identity of the
     *     registration
     */
    void addEventListener(String type, EventListener listener, boolean useCapture);

    /**
     * It removes a listener. If there is none with those three values, it does nothing and does not
     * warn.
     */
    void removeEventListener(String type, EventListener listener, boolean useCapture);

    /**
     * It dispatches an event through this target, with the three phases complete.
     *
     * @return whether the default action was <b>not</b> cancelled. Careful with the sense: it
     *     returns true when nobody called {@code preventDefault()}
     * @throws EventException {@code UNSPECIFIED_EVENT_TYPE_ERR} if the event has no type
     */
    boolean dispatchEvent(Event evt) throws EventException;
}
