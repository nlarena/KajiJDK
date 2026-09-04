package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.CardException -- algo salio mal con la tarjeta o el lector.
 *
 * <p>Es comprobada a proposito: una tarjeta se saca en cualquier momento, y el codigo que la usa
 * tiene que decir que hace cuando eso pasa.
 */
public class CardException extends Exception {

    private static final long serialVersionUID = 7787607144922050628L;

    /** Con ese mensaje. */
    public CardException(String message) {
        super(message);
    }

    /** Envolviendo esa causa. */
    public CardException(Throwable cause) {
        super(cause);
    }

    /** Con mensaje y causa. */
    public CardException(String message, Throwable cause) {
        super(message, cause);
    }
}
