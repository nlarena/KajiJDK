package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NonReadableChannelException — a channel that was not opened for
 * reading was read from.
 *
 * <p>The permission is fixed on opening and does not change. That it is an `IllegalStateException`
 * --and not one of argument-- is right: the argument of `read` is fine, what does not fit is asking
 * **this** channel for it.
 */
public class NonReadableChannelException extends IllegalStateException {

    private static final long serialVersionUID = 1000000015L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public NonReadableChannelException() {
        super();
    }
}
