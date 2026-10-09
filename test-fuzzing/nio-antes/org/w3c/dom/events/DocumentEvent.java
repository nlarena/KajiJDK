package org.w3c.dom.events;

import org.w3c.dom.DOMException;

/**
 * KajiLibrary's org.w3c.dom.events.DocumentEvent -- the factory of events.
 *
 * <p>The {@code Document} implements it. It is the only way of creating an event: there are no
 * constructors.
 *
 * <p>The event comes out <b>empty</b> and has to be initialised with the {@code init*} that belongs
 * to its type before dispatching it. It is in two steps because the factory takes an interface name
 * and cannot know which arguments each one carries.
 */
public interface DocumentEvent {

    /**
     * An event of the type asked for, uninitialised.
     *
     * @param eventType the name of the <b>interface</b>, not of the event: {@code "MouseEvents"},
     *     {@code "MutationEvents"}, {@code "UIEvents"}, {@code "Events"}. In the plural, which is
     *     how the standard writes it and is easy to get wrong
     * @throws DOMException {@code NOT_SUPPORTED_ERR} if that interface is not implemented
     */
    Event createEvent(String eventType) throws DOMException;
}
