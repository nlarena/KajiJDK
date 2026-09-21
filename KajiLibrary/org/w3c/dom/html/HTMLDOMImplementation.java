package org.w3c.dom.html;

import org.w3c.dom.DOMImplementation;

/**
 * The factory of HTML documents.
 *
 * <p>It extends {@link org.w3c.dom.DOMImplementation} with one single method, and the method is the
 * reason for the interface existing: `createHTMLDocument` builds a document **with its skeleton in
 * place** --`html`, `head`, `title` and `body`--, which is what tells an HTML document apart from
 * an empty XML one.
 */
public interface HTMLDOMImplementation extends org.w3c.dom.DOMImplementation {

    /** A new document with its skeleton in place and that title. */
    HTMLDocument createHTMLDocument(String title);
}
