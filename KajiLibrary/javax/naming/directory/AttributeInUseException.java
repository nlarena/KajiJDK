package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.AttributeInUseException -- the attribute is already there.
 *
 * <p>The JDK's definition: an operation tried to add an attribute that already exists -- for
 * example {@code modifyAttributes} with {@link DirContext#ADD_ATTRIBUTE}, or creating an entry,
 * with an attribute (or value) the entry already has. An earlier note put here the case of adding a
 * second value to a single-valued attribute; that one is a schema conflict, and the JDK documents
 * it under {@link InvalidAttributeValueException}.
 */
public class AttributeInUseException extends NamingException {

    private static final long serialVersionUID = 4437710305529322564L;

    /** With no detail. */
    public AttributeInUseException() {
        super();
    }

    /** With a message saying what the problem was. */
    public AttributeInUseException(String explanation) {
        super(explanation);
    }
}
