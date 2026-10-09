package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.CardException -- something went wrong with the card or the
 * reader.
 *
 * <p>It is checked on purpose: a card can be pulled out at any moment, and the code that uses it
 * has to say what it does when that happens.
 */
public class CardException extends Exception {

    private static final long serialVersionUID = 7787607144922050628L;

    /** With that message. */
    public CardException(String message) {
        super(message);
    }

    /** Wrapping that cause. */
    public CardException(Throwable cause) {
        super(cause);
    }

    /** With a message and a cause. */
    public CardException(String message, Throwable cause) {
        super(message, cause);
    }
}
