package org.w3c.dom.css;

/**
 * An element with a `style` attribute.
 *
 * <p>The block it returns is **live and writable**: changing it changes the attribute of the
 * document. It is what tells this style apart from the computed one of {@link ViewCSS}, which is
 * read-only because it is a result and not a source.
 */
public interface ElementCSSInlineStyle {

    /** The block of the `style` attribute. */
    CSSStyleDeclaration getStyle();
}
