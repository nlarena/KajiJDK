package javax.management.openmbean;

/**
 * A row whose index is already in the table.
 *
 * <p>{@link TabularData#put} throws it, and it is the reason {@code put} is <b>not</b> a
 * replacement like a {@code Map}'s: an open table has keys derived from the row's content, so two
 * rows with the same index are an error of whoever built them, not an intention to overwrite the
 * first. To replace, you have to remove and put again.
 */
public class KeyAlreadyExistsException extends IllegalArgumentException {

    private static final long serialVersionUID = 1845183636745282866L;

    /** Without a message. */
    public KeyAlreadyExistsException() {
        super();
    }

    /** With that message. */
    public KeyAlreadyExistsException(String msg) {
        super(msg);
    }
}
