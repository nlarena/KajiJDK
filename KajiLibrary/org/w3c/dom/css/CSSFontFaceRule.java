package org.w3c.dom.css;

/** A `@font-face`: the declarations that describe a font. */
public interface CSSFontFaceRule extends CSSRule {

    /** The declarations of the font. */
    CSSStyleDeclaration getStyle();
}
