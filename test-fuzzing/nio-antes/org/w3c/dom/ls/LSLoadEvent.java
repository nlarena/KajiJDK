package org.w3c.dom.ls;

import org.w3c.dom.Document;
import org.w3c.dom.events.Event;

/**
 * KajiLibrary's org.w3c.dom.ls.LSLoadEvent -- a document finished loading.
 *
 * <p>It is how an asynchronous {@link LSParser} returns its result: the call to {@code parse}
 * returns at once with no document, and the document arrives here later.
 *
 * <p>That it is a DOM {@code Event} and not a callback interface of its own has a practical
 * consequence: it is listened to with {@code addEventListener} on the parser, and therefore there
 * may be <b>several</b> parties interested in the same load without any of them knowing about the
 * others.
 *
 * <p>{@link #getInput} comes together with the document on purpose: with several loads in flight,
 * the event alone would not be enough to know which one finished.
 */
public interface LSLoadEvent extends Event {

    /** The document that finished loading. */
    Document getNewDocument();

    /** Where it was loaded from; see the note of the class on why it is needed. */
    LSInput getInput();
}
