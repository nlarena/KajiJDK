package javax.management.modelmbean;

/**
 * KajiLibrary's javax.management.modelmbean.XMLParseException -- a descriptor written in XML
 * could not be read.
 *
 * <p>{@link DescriptorSupport}'s constructor that takes an XML string throws it. It is the only
 * place in the package where there is XML, and it is there because of a 1999 idea that did not
 * prosper: keeping an MBean's descriptors in a file.
 *
 * <p>The constructor taking an {@link Exception} wraps what really failed. The message it builds
 * is neither the wrapper's nor the cause's but both concatenated, after a fixed prefix. The
 * prefix is not the JDK's, which writes {@code "XML Parse Exception: "} with a colon and
 * separates the cause with another one; this one writes {@code "XML Parse Exception. "} and
 * concatenates the cause with nothing in between. Nothing reads that text, so the difference
 * only shows in a log.
 */
public class XMLParseException extends Exception {

    private static final long serialVersionUID = 3176664577895105181L;

    /** Without detail. */
    public XMLParseException() {
        super("XML Parse Exception.");
    }

    /** With a message. */
    public XMLParseException(String s) {
        super("XML Parse Exception. " + s);
    }

    /** With the cause and a message. */
    public XMLParseException(Exception e, String s) {
        super("XML Parse Exception. " + s + ((e == null) ? "" : e.toString()));
    }
}
