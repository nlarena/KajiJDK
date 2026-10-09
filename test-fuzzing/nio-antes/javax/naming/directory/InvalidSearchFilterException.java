package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidSearchFilterException -- the search filter is
 * malformed.
 *
 * <p>The filter is a string with RFC 2254 syntax --parentheses, operators, escapes-- and building
 * it by concatenating text is like building SQL that way: a value with a parenthesis inside changes
 * what the filter selects. That is what the {@code search} version with numbered arguments is for.
 */
public class InvalidSearchFilterException extends NamingException {

    private static final long serialVersionUID = 2902700940682875441L;

    /** With no detail. */
    public InvalidSearchFilterException() {
        super();
    }

    /** With a message saying what the problem was. */
    public InvalidSearchFilterException(String explanation) {
        super(explanation);
    }
}
