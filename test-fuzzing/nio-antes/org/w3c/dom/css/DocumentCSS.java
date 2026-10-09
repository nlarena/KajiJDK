package org.w3c.dom.css;

import org.w3c.dom.Element;
import org.w3c.dom.stylesheets.DocumentStyle;

/**
 * A document that admits override styles.
 *
 * <p>The override style is a layer that wins over all the sheets and over the `style` of the
 * element: the user's cascade, in CSS 2 terms. It is asked for empty and written, and from then on
 * what it says has the last word.
 */
public interface DocumentCSS extends DocumentStyle {

    /**
     * The override block of that element, to read it or write it.
     *
     * @param pseudoElt the pseudo-element --`:first-line`--, or the empty string
     */
    CSSStyleDeclaration getOverrideStyle(Element elt, String pseudoElt);
}
