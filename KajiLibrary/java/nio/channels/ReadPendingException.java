package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ReadPendingException — reading was asked for over an asynchronous
 * channel that has a read under way already.
 */
public class ReadPendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000020L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public ReadPendingException() {
        super();
    }
}
