package javax.xml.xpath;

/**
 * KajiLibrary's javax.xml.xpath.XPathExpressionException -- the expression could not be compiled
 * or evaluated.
 *
 * <p>It covers both stages and does not separate them, which is a debatable API decision: a syntax
 * error --known at compile time-- and an evaluation error --which depends on the document-- arrive
 * as the same type. Whoever wants to tell them apart has to look at which call threw.
 */
public class XPathExpressionException extends XPathException {

    private static final long serialVersionUID = -1837080260374986980L;

    /** With a message. */
    public XPathExpressionException(String message) {
        super(message);
    }

    /** With the underlying cause. */
    public XPathExpressionException(Throwable cause) {
        super(cause);
    }
}
