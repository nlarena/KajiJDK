package java.awt.datatransfer;

/**
 * The data was asked for in a format that cannot be handed over.
 *
 * <p>It is a **checked** exception on purpose: asking for a format the source does not offer is not
 * a programming error but a normal possibility, and whoever asks has to be prepared.
 */
public class UnsupportedFlavorException extends Exception {

    private static final long serialVersionUID = 5383814944251665601L;

    /**
     * With the format that could not be handed over.
     *
     * <p>The message is the format's human-readable name and not its MIME type: the one who is going
     * to read it is a person.
     */
    public UnsupportedFlavorException(DataFlavor flavor) {
        super(flavor != null ? flavor.getHumanPresentableName() : null);
    }
}
