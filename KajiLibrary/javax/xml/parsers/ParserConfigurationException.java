package javax.xml.parsers;

/**
 * KajiLibrary's javax.xml.parsers.ParserConfigurationException -- the parser could not be put
 * together.
 *
 * <p>It is about <b>configuration</b>, not content: something was asked that the implementation
 * cannot do --validate, or understand namespaces, or a property it does not know-- and that is why
 * there is no parser. The distinction from {@code SAXException} matters: here not a single byte of
 * the document has been read yet, so retrying with the same request cannot work.
 */
public class ParserConfigurationException extends Exception {

    private static final long serialVersionUID = -3688849216575373917L;

    /** Without detail. */
    public ParserConfigurationException() {
        super();
    }

    /** With a message that says what was asked and could not be provided. */
    public ParserConfigurationException(String msg) {
        super(msg);
    }
}
