package jdk.management.jfr;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.management.MBeanServerConnection;

import jdk.jfr.EventSettings;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.MetadataEvent;
import jdk.jfr.consumer.RecordedEvent;

/**
 * A stream of events of <strong>another</strong> VM, through JMX.
 *
 * <h2>What it does underneath</h2>
 *
 * <p>It talks to the {@link FlightRecorderMXBean} of the remote process: it creates a recording
 * over there, starts it, and goes on bringing the data over with {@code readStream} while it
 * delivers them here as {@link RecordedEvent}.
 *
 * <p>It presents all of that as an ordinary {@link EventStream}, so that the code that consumes
 * events is the same for one's own and for those of another machine. That is the reason why it
 * exists instead of letting everybody put together the cycle of
 * {@code openStream}/{@code readStream} themselves.
 *
 * <h2>What changes with respect to a local stream</h2>
 *
 * <p>The events arrive <strong>in batches and with delay</strong>. There is no way for it to be
 * otherwise: the data are brought over when the block on the other side closes. A local stream can
 * deliver almost at the moment; this one cannot.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The two constructors fail with {@link IOException}, which is what they already declared: the
 * stream needs the reader of the binary format of JFR in order to turn the bytes it brings into
 * events, and this library does not have it. The JMX connection is not the problem -- what is
 * missing is the one that interprets the bytes of the other side.
 *
 * @since 16
 */
public final class RemoteRecordingStream implements EventStream {

    private static final String NOT_THERE =
            "a remote stream needs the reader of the binary format of JFR in order to turn the "
            + "bytes it brings into events, and this library does not implement it";

    /**
     * A stream over the VM on the other side of that connection, with a temporary working
     * directory.
     *
     * @param connection the JMX connection
     * @throws IOException in this VM always; see the note of the class
     * @throws NullPointerException if it is {@code null}
     */
    public RemoteRecordingStream(final MBeanServerConnection connection) throws IOException {
        Objects.requireNonNull(connection, "connection");
        throw new IOException(NOT_THERE);
    }

    /**
     * A stream over the VM on the other side of that connection, with that working directory.
     *
     * @param connection the JMX connection
     * @param directory the local directory where to leave the data brought over
     * @throws IOException in this VM always; see the note of the class
     * @throws NullPointerException if either is {@code null}
     */
    public RemoteRecordingStream(final MBeanServerConnection connection, final Path directory)
            throws IOException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(directory, "directory");
        throw new IOException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void onMetadata(final Consumer<MetadataEvent> action) {
        throw new IllegalStateException(NOT_THERE);
    }

    /**
     * It replaces the settings of the remote recording.
     *
     * @param settings the settings
     */
    public void setSettings(final Map<String, String> settings) {
        throw new IllegalStateException(NOT_THERE);
    }

    /**
     * It disables a remote event.
     *
     * @param name the name of the event
     * @return the settings of that event
     */
    public EventSettings disable(final String name) {
        throw new IllegalStateException(NOT_THERE);
    }

    /**
     * It enables a remote event.
     *
     * @param name the name of the event
     * @return the settings of that event
     */
    public EventSettings enable(final String name) {
        throw new IllegalStateException(NOT_THERE);
    }

    /**
     * The maximum age of the data of the remote recording.
     *
     * @param maxAge the duration
     */
    public void setMaxAge(final Duration maxAge) {
        throw new IllegalStateException(NOT_THERE);
    }

    /**
     * The maximum size of the remote recording.
     *
     * @param maxSize the bytes
     */
    public void setMaxSize(final long maxSize) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void onEvent(final Consumer<RecordedEvent> action) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void onEvent(final String eventName, final Consumer<RecordedEvent> action) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void onFlush(final Runnable action) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void onError(final Consumer<Throwable> action) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void onClose(final Runnable action) {
        throw new IllegalStateException(NOT_THERE);
    }

    /**
     * It closes the stream.
     *
     * <p>It does not fail: closing something that was never opened is legitimate.
     */
    public void close() {
    }

    /** {@inheritDoc} */
    public boolean remove(final Object action) {
        return false;
    }

    /** {@inheritDoc} */
    public void setReuse(final boolean reuse) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void setOrdered(final boolean ordered) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void setStartTime(final Instant startTime) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void setEndTime(final Instant endTime) {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void start() {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void startAsync() {
        throw new IllegalStateException(NOT_THERE);
    }

    /**
     * It stops the remote recording.
     *
     * @return whether it was recording
     */
    public boolean stop() {
        throw new IllegalStateException(NOT_THERE);
    }

    /**
     * It dumps the data brought over to a local file.
     *
     * @param destination the file
     * @throws IOException if it could not be written
     */
    public void dump(final Path destination) throws IOException {
        throw new IOException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void awaitTermination(final Duration timeout) throws InterruptedException {
        throw new IllegalStateException(NOT_THERE);
    }

    /** {@inheritDoc} */
    public void awaitTermination() throws InterruptedException {
        throw new IllegalStateException(NOT_THERE);
    }
}
