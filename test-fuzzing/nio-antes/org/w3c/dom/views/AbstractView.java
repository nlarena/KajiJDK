package org.w3c.dom.views;

/**
 * KajiLibrary's org.w3c.dom.views.AbstractView -- a view of a document.
 *
 * <p>One same document may be shown in several ways at a time: a window, a printout, a screen
 * reader. Each of those is a view, and the interface exists to be able to <b>name</b> them without
 * saying anything about them -- that is why it has only one method, and it is the one that returns
 * the document it is a view of.
 *
 * <p>The Views module of the DOM never grew beyond this pair of interfaces. It is still in the API
 * because {@code UIEvent} needs to say in <b>which</b> view an event happened, and without this
 * type there would be no way.
 */
public interface AbstractView {

    /** The document this is a view of. */
    DocumentView getDocument();
}
