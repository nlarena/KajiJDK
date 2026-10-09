package java.awt.datatransfer;

/**
 * A malformed MIME type.
 *
 * <p>A MIME type has a shape —`type/subtype` plus parameters— and this exception is what is raised
 * when the string does not respect it.
 */
public class MimeTypeParseException extends Exception {

    private static final long serialVersionUID = -5604407764691570741L;

    /** With no explanation. */
    public MimeTypeParseException() {
        super();
    }

    /** With the given explanation. */
    public MimeTypeParseException(String s) {
        super(s);
    }
}
