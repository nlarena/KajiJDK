package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AlreadyConnectedException — connecting a channel that was
 * connected already was attempted.
 */
public class AlreadyConnectedException extends IllegalStateException {

    private static final long serialVersionUID = 1000000002L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public AlreadyConnectedException() {
        super();
    }
}
