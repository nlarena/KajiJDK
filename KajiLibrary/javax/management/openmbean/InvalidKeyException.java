package javax.management.openmbean;

/**
 * A key that is not a valid item name for the composite or tabular type queried.
 *
 * <p>Unchecked, like {@link InvalidOpenTypeException}.
 */
public class InvalidKeyException extends IllegalArgumentException {

    private static final long serialVersionUID = 4224269443946322062L;

    /** Without a message. */
    public InvalidKeyException() {
        super();
    }

    /** With that message. */
    public InvalidKeyException(String msg) {
        super(msg);
    }
}
