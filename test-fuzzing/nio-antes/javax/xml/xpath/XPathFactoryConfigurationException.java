package javax.xml.xpath;

/**
 * KajiLibrary's javax.xml.xpath.XPathFactoryConfigurationException -- there is no factory for that
 * object model.
 *
 * <p>Unlike {@code FactoryConfigurationError} in {@code javax.xml.parsers}, this is a <b>checked
 * exception</b> and not an error. The difference makes sense: there the failure is that the
 * platform has no XML, which is unrecoverable; here it is that there is no support for <b>one
 * particular object model</b>, and a reasonable program can try another.
 */
public class XPathFactoryConfigurationException extends XPathException {

    private static final long serialVersionUID = -1837080260374986980L;

    /** With a message. */
    public XPathFactoryConfigurationException(String message) {
        super(message);
    }

    /** With the underlying cause. */
    public XPathFactoryConfigurationException(Throwable cause) {
        super(cause);
    }
}
