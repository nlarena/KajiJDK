package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ClosedSelectorException — a closed selector was used.
 */
public class ClosedSelectorException extends IllegalStateException {

    private static final long serialVersionUID = 1000000007L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public ClosedSelectorException() {
        super();
    }
}
