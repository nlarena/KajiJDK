package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.SchemaViolationException -- the operation contradicts the
 * directory schema.
 *
 * <p>The generic one of the schema exceptions, for what does not fall into the others: modifying an
 * entry's attributes against its object class, moving something where its object class cannot
 * live, changing an object class for an incompatible one. (An earlier note also listed deleting an
 * entry that has children; JNDI reports that as {@code javax.naming.ContextNotEmptyException}.)
 */
public class SchemaViolationException extends NamingException {

    private static final long serialVersionUID = -3041762429525049663L;

    /** With no detail. */
    public SchemaViolationException() {
        super();
    }

    /** With a message saying what the problem was. */
    public SchemaViolationException(String explanation) {
        super(explanation);
    }
}
