package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * The data is not a multiple of the block size.
 *
 * <p>A block cipher with no padding can only work with exact multiples of the block size. With
 * padding this never happens while encrypting --padding is precisely what completes the last
 * block-- but it does while decrypting, when what arrives is cut short.
 *
 * @since 1.4
 */
public class IllegalBlockSizeException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** One with no message. */
    public IllegalBlockSizeException() {
        super();
    }

    /**
     * One with a message.
     *
     * @param msg what happened
     */
    public IllegalBlockSizeException(String msg) {
        super(msg);
    }
}
