package javax.swing.text;

/**
 * A position the document does not have was asked for.
 *
 * <p>Checked, unlike {@link IndexOutOfBoundsException}, and the difference is one of intent: in
 * a document the length <strong>changes by itself</strong> while somebody types, so an offset
 * that was valid when it was computed may not be when it is used. It is not necessarily a bug in
 * the program, and that is why the compiler forces foreseeing it.
 */
public class BadLocationException extends Exception {

    private static final long serialVersionUID = 8934174085564342750L;

    private int offset;

    /**
     * @param s the message
     * @param offset the position that had been asked for
     */
    public BadLocationException(String s, int offset) {
        super(s);
        this.offset = offset;
    }

    /** The position that had been asked for. */
    public int offsetRequested() {
        return this.offset;
    }
}
