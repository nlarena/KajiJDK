package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * The padding of the decrypted block is not what it should be.
 *
 * <p>It nearly always means the key is wrong. A block cipher decrypts anything --the operation
 * cannot fail-- and what comes out with the wrong key is noise; the only place it shows is the
 * padding at the end, which does not close.
 *
 * <p>That is why this exception has to be handled with care: answering whoever sends the data
 * differently depending on whether the padding closed is the leak through which a whole message is
 * decrypted without the key. Whoever catches it should always give the same error outwards.
 *
 * @since 1.4
 */
public class BadPaddingException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** One with no message. */
    public BadPaddingException() {
        super();
    }

    /**
     * One with a message.
     *
     * @param msg what happened
     */
    public BadPaddingException(String msg) {
        super(msg);
    }
}
