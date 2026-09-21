package org.w3c.dom.xpath;

/**
 * KajiLibrary's org.w3c.dom.xpath.XPathException -- the expression could not be compiled or its
 * result could not be read as asked.
 *
 * <p>The two codes separate the two moments in which something can go wrong, and it is as well to
 * tell them apart because one belongs to whoever wrote the expression and the other to whoever
 * reads the result: {@link #INVALID_EXPRESSION_ERR} is "this is not XPath", {@link #TYPE_ERR} is
 * "this is XPath but it does not return what you asked for".
 *
 * <p>Unchecked and with the code in a public field, by the DOM convention.
 */
public class XPathException extends RuntimeException {

    private static final long serialVersionUID = 6156942920132862751L;

    /** The expression is not valid XPath. */
    public static final short INVALID_EXPRESSION_ERR = 51;

    /** The result cannot be converted to the type asked for. */
    public static final short TYPE_ERR = 52;

    /** Which of the two. Public by the DOM convention. */
    public short code;

    public XPathException(short code, String message) {
        super(message);
        this.code = code;
    }
}
