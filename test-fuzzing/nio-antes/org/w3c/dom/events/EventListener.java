package org.w3c.dom.events;

/**
 * KajiLibrary's org.w3c.dom.events.EventListener -- the one who receives an event.
 *
 * <p>One single method, and with no return value on purpose: a listener <b>does not decide</b>
 * whether the event goes on. {@code Event.stopPropagation()} and {@code Event.preventDefault()} are
 * there for that, which say two different things and get confused -- see {@link Event}.
 */
public interface EventListener {

    /** It receives the event. */
    void handleEvent(Event evt);
}
