package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ShutdownChannelGroupException — the channel group has been shut
 * down already, or the completion handler cannot be invoked because the channel's group was shut
 * down.
 *
 * <p>Both cases are the same problem seen from the two ends: there is nobody left to run the work.
 */
public class ShutdownChannelGroupException extends IllegalStateException {

    private static final long serialVersionUID = 1000000021L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public ShutdownChannelGroupException() {
        super();
    }
}
