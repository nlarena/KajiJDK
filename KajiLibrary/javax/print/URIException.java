package javax.print;

import java.net.URI;

/**
 * KajiLibrary's javax.print.URIException -- the failure was because of a URI.
 *
 * <p>An interface; see the note of {@link PrintException}. It shows up when an attribute carries a
 * URI --typically {@code Destination}, which says which file to write to-- and that URI does not
 * serve.
 *
 * <p>{@link #URIOtherProblem} is -1 and not 3; the constants come from the IPP standard and there
 * the "other" one is a separate value.
 */
public interface URIException {

    /** It cannot be reached. */
    int URIInaccessible = 1;

    /** The scheme is not supported. */
    int URISchemeNotSupported = 2;

    /** Something else. */
    int URIOtherProblem = -1;

    /** The URI that failed. */
    URI getUnsupportedURI();

    /** Which of the three reasons. */
    int getReason();
}
