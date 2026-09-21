package jdk.jfr;

/**
 * At what point of its life a recording is.
 *
 * <h2>The order goes one way and there is no coming back</h2>
 *
 * <p>{@link #NEW} to {@link #RUNNING} to {@link #STOPPED} to {@link #CLOSED}, with {@link #DELAYED}
 * as a detour when the start was scheduled for later. There is no way back: a stopped recording
 * cannot be resumed.
 *
 * <p>The reason is the format of the file. A recording is a continuous sequence of blocks with
 * their time marks; resuming it would leave a hole in the middle, and a tool that read it would
 * have no way of telling that hole apart from a period with no activity. In order to go on
 * recording, a new recording is started.
 *
 * <h2>Stopped and closed are not the same</h2>
 *
 * <p>{@link #STOPPED} no longer records and <strong>still has the data</strong>: they can be dumped
 * to a file or read as a stream. {@link #CLOSED} released them. Closing without having dumped loses
 * the recording, and it is the most common mistake with this API.
 *
 * @since 9
 */
public enum RecordingState {

    /** Created and not started. */
    NEW,

    /** With the start scheduled for a future moment. */
    DELAYED,

    /** Grabando. */
    RUNNING,

    /** Stopped, with the data still available. */
    STOPPED,

    /** Closed; the data have already been released. */
    CLOSED
}
