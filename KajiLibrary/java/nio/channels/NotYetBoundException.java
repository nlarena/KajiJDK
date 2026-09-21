package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NotYetBoundException — a server channel that has not been tied to
 * any address yet was used.
 *
 * <p>Accepting connections without having said which port to listen on has no possible answer.
 */
public class NotYetBoundException extends IllegalStateException {

    private static final long serialVersionUID = 1000000017L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public NotYetBoundException() {
        super();
    }
}
