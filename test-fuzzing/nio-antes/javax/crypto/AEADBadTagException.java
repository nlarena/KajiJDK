package javax.crypto;

/**
 * The authentication tag does not match.
 *
 * <p>In an authenticated cipher --GCM and its kin-- every message travels with a tag proving nobody
 * touched it. If it does not match, what was decrypted is discarded whole: not even a part is handed
 * over, because a part of an altered message is an altered message.
 *
 * <p>It is a {@link BadPaddingException} by historical inheritance, not because it has anything to
 * do with padding: authenticated ciphers arrived later and hung off the type everybody was already
 * catching.
 *
 * @since 1.7
 */
public class AEADBadTagException extends BadPaddingException {

    private static final long serialVersionUID = 1L;

    /** One with no message. */
    public AEADBadTagException() {
        super();
    }

    /**
     * One with a message.
     *
     * @param msg what happened
     */
    public AEADBadTagException(String msg) {
        super(msg);
    }
}
