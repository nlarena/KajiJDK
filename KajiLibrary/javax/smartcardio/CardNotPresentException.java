package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.CardNotPresentException -- no habia tarjeta en el lector.
 *
 * <p>Tiene su propio tipo porque es el error que hay que distinguir: casi siempre se arregla solo
 * --alguien mete la tarjeta-- y merece una respuesta distinta a la de un lector roto.
 */
public class CardNotPresentException extends CardException {

    private static final long serialVersionUID = 1346879911706545215L;

    /** Con ese mensaje. */
    public CardNotPresentException(String message) {
        super(message);
    }

    /** Envolviendo esa causa. */
    public CardNotPresentException(Throwable cause) {
        super(cause);
    }

    /** Con mensaje y causa. */
    public CardNotPresentException(String message, Throwable cause) {
        super(message, cause);
    }
}
