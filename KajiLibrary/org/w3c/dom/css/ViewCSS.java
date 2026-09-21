package org.w3c.dom.css;

import org.w3c.dom.Element;
import org.w3c.dom.views.AbstractView;

/**
 * A view that knows how to calculate the **computed** style of an element.
 *
 * <p>The computed style is the result of applying the whole cascade --the author's sheets, the
 * user's, the browser's, inheritance and the `style` of the element-- and that is why it is
 * **read-only**: it is a conclusion, not a source. Writing to it would have nobody to affect.
 *
 * <p>It is a method of the **view** and not of the element because the result depends on the
 * medium: the same rule gives a different `font-size` on screen than on paper.
 */
public interface ViewCSS extends AbstractView {

    /**
     * The computed style of that element in this view.
     *
     * @param pseudoElt the pseudo-element --`:first-line`--, or the empty string
     */
    CSSStyleDeclaration getComputedStyle(Element elt, String pseudoElt);
}
