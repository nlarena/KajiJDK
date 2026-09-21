package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.CardNotPresentException -- there was no card in the reader.
 *
 * <p>It has its own type because it is the error that needs telling apart: it almost always fixes
 * itself --somebody inserts the card-- and deserves a different response from that to a broken
 * reader.
 */
public class CardNotPresentException extends CardException {

    private static final long serialVersionUID = 1346879911706545215L;

    /** With that message. */
    public CardNotPresentException(String message) {
        super(message);
    }

    /** Wrapping that cause. */
    public CardNotPresentException(Throwable cause) {
        super(cause);
    }

    /** With a message and a cause. */
    public CardNotPresentException(String message, Throwable cause) {
        super(message, cause);
    }
}
