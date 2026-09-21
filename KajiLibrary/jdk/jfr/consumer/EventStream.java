package jdk.jfr.consumer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * A stream of events, in order to consume them <strong>while they happen</strong> instead of
 * reading a file at the end.
 *
 * <h2>What changes with respect to recording and dumping</h2>
 *
 * <p>That there is no need to know beforehand when something interesting is going to happen. A
 * program that reacts to its own events --that raises a metric, that logs a long pause-- cannot
 * wait for somebody to dump a file.
 *
 * <p>It is also what allows one to consume events of <strong>another</strong> process, with
 * {@link #openRepository()}: JFR writes its repository on disk and this stream follows it.
 *
 * <h2>The two settings one has to understand before using it</h2>
 *
 * <p>{@link #setReuse} decides whether the same {@link RecordedEvent} object is reused for each
 * event. With {@code true} --the default value-- the event cannot be kept for later: the object one
 * received is going to be overwritten on the next turn. It is extremely fast and it is the most
 * common source of error of this API.
 *
 * <p>{@link #setOrdered} decides whether the events arrive in order of time. Ordering them forces
 * one to wait and to accumulate, because different threads write into different buffers. With
 * {@code false} they arrive sooner and out of order.
 *
 * <h2>The two modes of starting</h2>
 *
 * <p>{@link #start} blocks the calling thread until the stream finishes; {@link #startAsync}
 * returns at once and the stream runs in another thread. With the second one {@link
 * #awaitTermination()} is needed in order to know when it finished.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The three static factories fail with {@link IOException} saying that there is neither a
 * repository nor a reader of the binary format. The interface is whole: an implementation that
 * reads the format fits in here without touching it.
 *
 * @since 14
 */
public interface EventStream extends AutoCloseable {

    /**
     * A stream over the repository of the current VM.
     *
     * @return the stream
     * @throws IOException if there is no repository, which is the case in this VM
     */
    static EventStream openRepository() throws IOException {
        throw new IOException(
                "there is no JFR repository in this VM: the recorder is not available");
    }

    /**
     * A stream over the repository there is in that directory.
     *
     * @param directory the directory of the repository
     * @return the stream
     * @throws IOException if it cannot be read, which is the case in this VM
     */
    static EventStream openRepository(final Path directory) throws IOException {
        throw new IOException(
                "reading a JFR repository needs the reader of the binary format, which this "
                + "library does not implement");
    }

    /**
     * A stream over a recording file.
     *
     * @param file the file
     * @return the stream
     * @throws IOException if it cannot be read, which is the case in this VM
     */
    static EventStream openFile(final Path file) throws IOException {
        throw new IOException(
                "reading a recording file needs the reader of the binary format, which this "
                + "library does not implement");
    }

    /**
     * What to do when the metadata change.
     *
     * <p>By default it does nothing: almost no consumer needs to find out, and forcing it to write
     * an empty method would be noise.
     *
     * @param action the action
     */
    default void onMetadata(Consumer<MetadataEvent> action) {
    }

    /**
     * What to do with each event.
     *
     * @param action the action
     */
    void onEvent(Consumer<RecordedEvent> action);

    /**
     * What to do with each event of that type.
     *
     * @param eventName the name of the type
     * @param action the action
     */
    void onEvent(String eventName, Consumer<RecordedEvent> action);

    /**
     * What to do when the stream empties its buffers.
     *
     * <p>It is the point where one knows that everything emitted up to that moment has already been
     * delivered, and therefore the only place where closing a window of aggregation makes sense.
     *
     * @param action the action
     */
    void onFlush(Runnable action);

    /**
     * What to do with an error of the stream.
     *
     * @param action the action
     */
    void onError(Consumer<Throwable> action);

    /**
     * What to do when the stream closes.
     *
     * @param action the action
     */
    void onClose(Runnable action);

    /** It closes the stream. */
    void close();

    /**
     * It takes a registered action away.
     *
     * @param action the action
     * @return whether it was registered
     */
    boolean remove(Object action);

    /**
     * Whether the same event object is reused for each delivery.
     *
     * @param reuse whether to reuse
     */
    void setReuse(boolean reuse);

    /**
     * Whether the events are delivered in order of time.
     *
     * @param ordered whether to order
     */
    void setOrdered(boolean ordered);

    /**
     * From when to deliver events.
     *
     * @param startTime the moment
     */
    void setStartTime(Instant startTime);

    /**
     * Up to when to deliver events.
     *
     * @param endTime the moment
     */
    void setEndTime(Instant endTime);

    /** It starts the stream in this thread and does not return until it finishes. */
    void start();

    /** It starts the stream in another thread and returns at once. */
    void startAsync();

    /**
     * It waits for the stream to finish, up to that time.
     *
     * @param timeout how long to wait
     * @throws InterruptedException if the thread is interrupted
     */
    void awaitTermination(Duration timeout) throws InterruptedException;

    /**
     * It waits for the stream to finish.
     *
     * @throws InterruptedException if the thread is interrupted
     */
    void awaitTermination() throws InterruptedException;
}
