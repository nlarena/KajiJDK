package javax.management.openmbean;

/**
 * A value whose open type is not the one expected in that place.
 *
 * <p>Unchecked: whoever causes it already had the value and its type at hand, so checking
 * beforehand was within reach. See the note in {@link OpenDataException}, which is the checked one
 * of the family.
 */
public class InvalidOpenTypeException extends IllegalArgumentException {

    private static final long serialVersionUID = -2837312755412327534L;

    /** Without a message. */
    public InvalidOpenTypeException() {
        super();
    }

    /** With that message. */
    public InvalidOpenTypeException(String msg) {
        super(msg);
    }
}
