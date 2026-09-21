package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidAttributesException -- required attributes are
 * missing, or there are extra ones.
 *
 * <p>It is about the <b>set</b> and not about one: creating an entry without the attributes its
 * object class requires lands here. That is why it differs from the two above, which talk about a
 * particular attribute.
 */
public class InvalidAttributesException extends NamingException {

    private static final long serialVersionUID = 2607612850539889765L;

    /** With no detail. */
    public InvalidAttributesException() {
        super();
    }

    /** With a message saying what the problem was. */
    public InvalidAttributesException(String explanation) {
        super(explanation);
    }
}
