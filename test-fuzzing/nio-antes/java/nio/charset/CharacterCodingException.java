package java.nio.charset;

import java.io.IOException;

/**
 * A character encoding or decoding error, thrown by the convenience methods that have nowhere
 * to put a {@link CoderResult}.
 *
 * <p>It is an {@link IOException} and not a runtime exception on purpose: text that fails to
 * decode is bad <em>data</em>, not a bug in the program reading it, so the compiler insists it
 * be handled.
 */
public class CharacterCodingException extends IOException {

    /** An instance with no detail message. */
    public CharacterCodingException() {
        super();
    }
}
