package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.NoSuchAttributeException -- that attribute is not in the
 * entry.
 *
 * <p>Different from {@link InvalidAttributeIdentifierException}: there the name was not valid, here
 * it is valid and the entry does not have it.
 */
public class NoSuchAttributeException extends NamingException {

    private static final long serialVersionUID = 4836415647935888137L;

    /** With no detail. */
    public NoSuchAttributeException() {
        super();
    }

    /** With a message saying what the problem was. */
    public NoSuchAttributeException(String explanation) {
        super(explanation);
    }
}
