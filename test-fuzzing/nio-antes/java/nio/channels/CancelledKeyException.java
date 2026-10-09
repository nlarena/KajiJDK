package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.CancelledKeyException — a selection key that had been cancelled
 * already was used.
 *
 * <p>A cancelled key goes on being a valid object --it can be held in the hand-- but it no longer
 * represents a live registration. That throwing is right and not returning a neutral value: the key
 * was cancelled because somebody asked for it, and going on using it is the mistake.
 */
public class CancelledKeyException extends IllegalStateException {

    private static final long serialVersionUID = 1000000004L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public CancelledKeyException() {
        super();
    }
}
