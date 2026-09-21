package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidAttributeIdentifierException -- the attribute's name
 * is not valid.
 *
 * <p>The name, not the value. An LDAP directory has rules about what can be called an attribute,
 * and an identifier with characters that do not belong is rejected before looking at what is
 * inside.
 */
public class InvalidAttributeIdentifierException extends NamingException {

    private static final long serialVersionUID = -9036920266322999923L;

    /** With no detail. */
    public InvalidAttributeIdentifierException() {
        super();
    }

    /** With a message saying what the problem was. */
    public InvalidAttributeIdentifierException(String explanation) {
        super(explanation);
    }
}
