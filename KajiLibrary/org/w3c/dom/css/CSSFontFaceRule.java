package org.w3c.dom.css;

/** Un `@font-face`: las declaraciones que describen una fuente. */
public interface CSSFontFaceRule extends CSSRule {

    /** Las declaraciones de la fuente. */
    CSSStyleDeclaration getStyle();
}
