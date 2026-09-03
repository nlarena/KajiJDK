package javax.management.openmbean;

/**
 * Una clave que no es un nombre de item válido para el tipo compuesto o tabular que se consultó.
 *
 * <p>De ejecución, como {@link InvalidOpenTypeException}.
 */
public class InvalidKeyException extends IllegalArgumentException {

    private static final long serialVersionUID = 4224269443946322062L;

    /** Sin mensaje. */
    public InvalidKeyException() {
        super();
    }

    /** Con ese mensaje. */
    public InvalidKeyException(String msg) {
        super(msg);
    }
}
