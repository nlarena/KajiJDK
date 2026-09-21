package org.w3c.dom.css;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;

/**
 * The factory of CSS style sheets.
 *
 * <p>It adds one single method to {@link DOMImplementation}, and it is the only road for creating a
 * sheet that does not come from a document: a newly created sheet is linked to nothing until
 * somebody puts it into a document.
 */
public interface DOMImplementationCSS extends DOMImplementation {

    /**
     * A new and empty sheet, with that title and those media.
     *
     * @throws DOMException `SYNTAX_ERR` if the list of media does not parse
     */
    CSSStyleSheet createCSSStyleSheet(String title, String media) throws DOMException;
}
