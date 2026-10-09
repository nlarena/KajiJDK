package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * A padding the provider does not have was asked for.
 *
 * <p>It is separate from {@link java.security.NoSuchAlgorithmException} because the name handed to
 * {@link Cipher#getInstance} carries three things --algorithm, mode and padding-- and knowing which
 * of the three was missing changes the error message.
 *
 * @since 1.4
 */
public class NoSuchPaddingException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** One with no message. */
    public NoSuchPaddingException() {
        super();
    }

    /**
     * One with a message.
     *
     * @param msg what happened
     */
    public NoSuchPaddingException(String msg) {
        super(msg);
    }
}
