package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.IllegalChannelGroupException — a channel and a group that do not
 * belong to the same provider were combined.
 *
 * <p>It is an `IllegalArgumentException` and not one of state because the error is in **the
 * argument**: the group that was passed does not serve for this channel, and there is no moment when
 * it would.
 */
public class IllegalChannelGroupException extends IllegalArgumentException {

    private static final long serialVersionUID = 1000000011L;

    /** Builds one. With no message: the name of the class **is** the message. */
    public IllegalChannelGroupException() {
        super();
    }
}
