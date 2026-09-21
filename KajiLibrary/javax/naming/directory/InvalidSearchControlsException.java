package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.InvalidSearchControlsException -- the search controls are
 * not valid.
 *
 * <p>A {@link SearchControls} with a scope that does not exist, or with inconsistent limits. It is
 * known before touching the directory, so it shows up in the call and not halfway through the
 * enumeration.
 */
public class InvalidSearchControlsException extends NamingException {

    private static final long serialVersionUID = -5124108943352665777L;

    /** With no detail. */
    public InvalidSearchControlsException() {
        super();
    }

    /** With a message saying what the problem was. */
    public InvalidSearchControlsException(String explanation) {
        super(explanation);
    }
}
