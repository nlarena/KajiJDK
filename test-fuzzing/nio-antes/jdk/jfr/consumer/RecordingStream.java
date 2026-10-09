package jdk.jfr.consumer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.EventSettings;

/**
 * A recording and its consumption, in the same object and in the same process.
 *
 * <h2>What it joins</h2>
 *
 * <p>A {@link jdk.jfr.Recording} and an {@link EventStream}. What to record is configured as in the
 * first one --{@link #enable}, {@link #setMaxAge}-- and it is consumed as in the second one
 * --{@link #onEvent}--, with no file in between.
 *
 * <p>It is the way for a program to react to its own events. A server that wants to log every
 * collection pause longer than 100 ms writes it like this, in five lines and without writing
 * anything to disk.
 *
 * <h2>The trap of {@code setReuse}</h2>
 *
 * <p>Inherited from {@link EventStream}: by default the {@link RecordedEvent} object is reused on
 * each delivery, so keeping it in a list in order to look at it later does not work -- every
 * element ends up being the same object with the last value. With {@code setReuse(false)} it can be
 * kept, and it costs one allocation per event.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The constructor fails with {@link IllegalStateException}, which is exactly what the one of the
 * JDK does when JFR is not available: underneath it asks for the recorder, and asking for it with
 * no JFR is that exception. It is not an addition of this library -- it is the behaviour defined
 * for this case.
 *
 * @since 14
 */
public final class RecordingStream implements AutoCloseable, EventStream {

    private static final String NOT_AVAILABLE = "Flight Recorder is not available in this VM";

    /**
     * A stream with the default configuration.
     *
     * @throws IllegalStateException if JFR is not available, which is the case in this VM
     */
    public RecordingStream() {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * A stream with that configuration.
     *
     * @param configuration the configuration
     * @throws IllegalStateException if JFR is not available, which is the case in this VM
     */
    public RecordingStream(final Configuration configuration) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It enables an event by name.
     *
     * @param name the name
     * @return the settings of that event
     */
    public EventSettings enable(final String name) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It replaces the settings.
     *
     * @param settings the settings
     */
    public void setSettings(final Map<String, String> settings) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It enables an event by its class.
     *
     * @param eventClass the class
     * @return the settings of that event
     */
    public EventSettings enable(final Class<? extends Event> eventClass) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It disables an event by name.
     *
     * @param name the name
     * @return the settings of that event
     */
    public EventSettings disable(final String name) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It disables an event by its class.
     *
     * @param eventClass the class
     * @return the settings of that event
     */
    public EventSettings disable(final Class<? extends Event> eventClass) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * The maximum age of the data.
     *
     * @param maxAge the duration
     */
    public void setMaxAge(final Duration maxAge) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * The maximum size.
     *
     * @param maxSize the bytes
     */
    public void setMaxSize(final long maxSize) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void setReuse(final boolean reuse) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void setOrdered(final boolean ordered) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void setStartTime(final Instant startTime) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void setEndTime(final Instant endTime) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void onEvent(final String eventName, final Consumer<RecordedEvent> action) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void onEvent(final Consumer<RecordedEvent> action) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void onFlush(final Runnable action) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void onClose(final Runnable action) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void onError(final Consumer<Throwable> action) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It closes the stream.
     *
     * <p>It does not fail: closing something that never started is legitimate, and making it fail
     * would break any {@code try}-with-resources.
     */
    public void close() {
    }

    /** {@inheritDoc} */
    public boolean remove(final Object action) {
        return false;
    }

    /** {@inheritDoc} */
    public void start() {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void startAsync() {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It stops the recording.
     *
     * @return whether it was recording
     */
    public boolean stop() {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It dumps the data to a file.
     *
     * @param destination the file
     * @throws IOException if it could not be written
     */
    public void dump(final Path destination) throws IOException {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void awaitTermination(final Duration timeout) throws InterruptedException {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void awaitTermination() throws InterruptedException {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /** {@inheritDoc} */
    public void onMetadata(final Consumer<MetadataEvent> action) {
        throw new IllegalStateException(NOT_AVAILABLE);
    }
}
