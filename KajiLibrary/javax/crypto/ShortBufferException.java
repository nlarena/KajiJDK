package javax.crypto;

import java.security.GeneralSecurityException;

/**
 * The array given for the output is not big enough.
 *
 * <p>The versions that write into a given array exist so as not to allocate on every step, which
 * matters in a stream of data. The price is having to ask beforehand how much will come out, with
 * {@link Cipher#getOutputSize}; this exception is what happens when nobody asked.
 *
 * @since 1.4
 */
public class ShortBufferException extends GeneralSecurityException {

    private static final long serialVersionUID = 1L;

    /** One with no message. */
    public ShortBufferException() {
        super();
    }

    /**
     * One with a message.
     *
     * @param msg what happened
     */
    public ShortBufferException(String msg) {
        super(msg);
    }
}
