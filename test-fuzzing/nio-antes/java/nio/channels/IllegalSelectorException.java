package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.IllegalSelectorException — registering a channel in a selector of
 * another provider was attempted.
 */
public class IllegalSelectorException extends IllegalArgumentException {

    private static final long serialVersionUID = 1000000012L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public IllegalSelectorException() {
        super();
    }
}
