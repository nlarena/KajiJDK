package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * The encapsulated secret could not be recovered.
 *
 * <p>Nearly always because the encapsulation arrived altered or because the private key is not the
 * matching one. As with an authenticated cipher's tag, it is best not to say outwards which of the
 * two it was.
 *
 * @since 21
 */
public class DecapsulateException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /**
     * One with a message.
     *
     * @param message what happened
     */
    public DecapsulateException(String message) {
        super(message);
    }

    /**
     * One with a message and a cause.
     *
     * @param message what happened
     * @param cause why
     */
    public DecapsulateException(String message, Throwable cause) {
        super(message, cause);
    }
}
