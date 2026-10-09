package org.w3c.dom.stylesheets;

/**
 * A document that exposes its style sheets.
 *
 * <p>It is a separate interface and not a method of `Document` because an XML document with no
 * style sheets has no reason to implement it: the DOM is built in layers and this is the one of
 * styles.
 */
public interface DocumentStyle {

    /** The sheets of the document, in a live list. */
    StyleSheetList getStyleSheets();
}
