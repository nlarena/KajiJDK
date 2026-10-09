package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidAttributeValueException -- the value is not valid for
 * that attribute.
 *
 * <p>The flip side of {@link InvalidAttributeIdentifierException}: the name is fine and the value
 * is not. Typically because the schema declares the attribute single-valued and it was given two,
 * or because the type does not match.
 */
public class InvalidAttributeValueException extends NamingException {

    private static final long serialVersionUID = 8720050295499275011L;

    /** With no detail. */
    public InvalidAttributeValueException() {
        super();
    }

    /** With a message saying what the problem was. */
    public InvalidAttributeValueException(String explanation) {
        super(explanation);
    }
}
