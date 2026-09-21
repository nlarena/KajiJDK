package org.w3c.dom.css;

import org.w3c.dom.DOMException;

/**
 * A `@charset`: the encoding of the sheet.
 *
 * <p>It can only be at the start and there can only be one. That is why `setEncoding` is the only
 * thing that can be touched: moving the rule or adding a second one would not describe a valid
 * sheet.
 */
public interface CSSCharsetRule extends CSSRule {

    /** The declared encoding. */
    String getEncoding();

    /**
     * It changes the encoding.
     *
     * @throws DOMException `SYNTAX_ERR` if the value is not a valid encoding name;
     *     `NO_MODIFICATION_ALLOWED_ERR` if the rule is read-only
     */
    void setEncoding(String encoding) throws DOMException;
}
