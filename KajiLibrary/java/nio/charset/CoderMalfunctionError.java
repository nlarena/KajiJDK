package java.nio.charset;

/**
 * A coder's own {@code encodeLoop} or {@code decodeLoop} threw an unexpected exception.
 *
 * <p>An {@link Error} and not an exception because it says the charset implementation is
 * broken, not the data: the coding loops are specified to report every problem they can
 * anticipate through a {@link CoderResult}, so anything thrown out of one is a defect in the
 * charset itself and there is nothing sensible for the caller to do about it.
 */
public class CoderMalfunctionError extends Error {

    /**
     * An instance wrapping the exception the coding loop threw.
     *
     * @param cause the exception thrown out of the coding loop
     */
    public CoderMalfunctionError(Exception cause) {
        super(cause);
    }
}
