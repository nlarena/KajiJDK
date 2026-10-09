package javax.xml.xpath;

/**
 * KajiLibrary's javax.xml.xpath.XPathFunctionException -- an extension function failed.
 *
 * <p>It is the only exception in this package thrown by <b>the API's user</b> and not by the
 * implementation: it comes out of an {@link XPathFunction} you wrote. That is why it extends
 * {@link XPathExpressionException} -- from the outside, a failing function is a failing evaluation,
 * and whoever called {@code evaluate} has no reason to know there was a custom function inside.
 */
public class XPathFunctionException extends XPathExpressionException {

    private static final long serialVersionUID = -1837080260374986980L;

    /** With a message. */
    public XPathFunctionException(String message) {
        super(message);
    }

    /** With the underlying cause. */
    public XPathFunctionException(Throwable cause) {
        super(cause);
    }
}
