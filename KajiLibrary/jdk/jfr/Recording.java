package jdk.jfr;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A recording: which events to capture, with what limits and where the result goes.
 *
 * <h2>The three limits, and why there are three</h2>
 *
 * <p>{@link #setDuration} cuts by time from when it starts. {@link #setMaxAge} discards the oldest
 * of a circular buffer. {@link #setMaxSize} discards the oldest on reaching a size.
 *
 * <p>The last two are not the same thing even though they look it, and the difference matters: with
 * {@code maxAge} one knows how far back one has and not how much it takes up; with {@code maxSize}
 * one knows how much it takes up and not how much time it covers. In a peak of activity, the same
 * size covers many fewer minutes.
 *
 * <p>{@code duration} is of another kind: the other two leave the recording running forever and
 * discard the old, this one ends it.
 *
 * <h2>The two uses</h2>
 *
 * <p><strong>Record and dump</strong>: start, wait, stop, {@link #dump}. It is what one does in
 * order to investigate something that can be reproduced.
 *
 * <p><strong>Leave it on and take a snapshot</strong>: start with {@code maxAge} and with no
 * destination, and when something goes wrong, {@link FlightRecorder#takeSnapshot}. It is what one
 * does in production in order to have the last few minutes of what has already happened.
 *
 * <h2>Closing without dumping loses the data</h2>
 *
 * <p>{@link #close} releases everything. A stopped recording still has its data and a closed one
 * does not, so the {@code try}-with-resources, which closes on leaving the block, erases the
 * recording if the {@link #dump} did not happen inside. It is the most common mistake with this
 * API.
 *
 * <h2>State in this VM</h2>
 *
 * <p>All the <strong>configuration</strong> is real: name, settings, limits, destination, state. A
 * recording can be built, configured and read, and {@link #copy} and {@link #getSettings} do what
 * they say.
 *
 * <p>What cannot work is what needs the recorder: {@link #start}, {@link #stop}, {@link #dump},
 * {@link #getStream} and {@link #getSize} fail with {@link IllegalStateException}, which is the
 * same as what the API defines for a VM with no JFR. The object does not lie about its state -- it
 * stays at {@link RecordingState#NEW} because it never started.
 *
 * @since 9
 */
public final class Recording implements Closeable {

    private static final String NOT_AVAILABLE = "Flight Recorder is not available in this VM";

    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    private final long id = NEXT_ID.getAndIncrement();
    private final Map<String, String> settings = new LinkedHashMap<String, String>();

    private String name;
    private RecordingState state = RecordingState.NEW;
    private long maxSize;
    private Duration maxAge;
    private Duration duration;
    private Path destination;
    private boolean dumpOnExit;
    private boolean toDisk = true;

    /**
     * A recording with no settings.
     *
     * <p>The name starts out being the identifier, as in the JDK: a recording with no name still
     * has to be able to be told apart from another in a list.
     */
    public Recording() {
        this.name = String.valueOf(id);
    }

    /**
     * A recording with those settings.
     *
     * @param settings the settings, with the key {@code "event#setting"}
     * @throws NullPointerException if it is {@code null}
     */
    public Recording(final Map<String, String> settings) {
        this();
        setSettings(settings);
    }

    /**
     * A recording with the settings of a configuration.
     *
     * @param configuration the configuration
     * @throws NullPointerException if it is {@code null}
     */
    public Recording(final Configuration configuration) {
        this();
        setSettings(Objects.requireNonNull(configuration, "configuration").getSettings());
    }

    /**
     * It starts the recording.
     *
     * @throws IllegalStateException in this VM, or if the recording already started or was closed
     */
    public void start() {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It schedules the start for that much time from now.
     *
     * @param delay how long to wait
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalStateException in this VM
     */
    public void scheduleStart(final Duration delay) {
        Objects.requireNonNull(delay, "delay");
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It stops the recording, keeping the data.
     *
     * @return whether it was recording
     * @throws IllegalStateException in this VM
     */
    public boolean stop() {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * The settings.
     *
     * @return a copy of the settings
     */
    public Map<String, String> getSettings() {
        return new LinkedHashMap<String, String>(settings);
    }

    /**
     * How much the recording takes up.
     *
     * @return the bytes
     * @throws IllegalStateException in this VM, because there are no data to measure
     */
    public long getSize() {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * When it stopped.
     *
     * @return the moment, or {@code null} if it did not stop
     */
    public Instant getStopTime() {
        return null;
    }

    /**
     * When it started.
     *
     * @return the moment, or {@code null} if it did not start
     */
    public Instant getStartTime() {
        return null;
    }

    /**
     * The maximum size.
     *
     * @return the bytes; zero is no limit
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * The maximum age of the data.
     *
     * @return the duration, or {@code null} if there is no limit
     */
    public Duration getMaxAge() {
        return maxAge;
    }

    /**
     * The name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * It replaces the settings.
     *
     * <p>It replaces and does not merge: the ones that were there and do not come in the new map
     * are lost. It is what the JDK does, and it is coherent with a recording being configured from
     * a complete {@link Configuration} and not by dint of touch-ups.
     *
     * @param settings the settings
     * @throws NullPointerException if it is {@code null}
     */
    public void setSettings(final Map<String, String> settings) {
        Objects.requireNonNull(settings, "settings");
        this.settings.clear();
        this.settings.putAll(settings);
    }

    /**
     * At what point of its life it is.
     *
     * @return the state
     */
    public RecordingState getState() {
        return state;
    }

    /**
     * It releases the data and the resources.
     *
     * <p>It does not fail even if JFR is not there: closing something that never started is
     * legitimate, and making it fail would break any {@code try}-with-resources.
     */
    public void close() {
        state = RecordingState.CLOSED;
    }

    /**
     * A copy of this recording.
     *
     * @param stop whether the copy has to be left stopped
     * @return the copy, with an identifier of its own
     */
    public Recording copy(final boolean stop) {
        final Recording r = new Recording(getSettings());
        r.name = name;
        r.maxSize = maxSize;
        r.maxAge = maxAge;
        r.duration = duration;
        r.destination = destination;
        r.dumpOnExit = dumpOnExit;
        r.toDisk = toDisk;
        r.state = stop ? RecordingState.STOPPED : state;
        return r;
    }

    /**
     * It writes the data into a file.
     *
     * @param destination the file
     * @throws IOException if it could not be written
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalStateException in this VM, because there are no data to write
     */
    public void dump(final Path destination) throws IOException {
        Objects.requireNonNull(destination, "destination");
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * Whether the data are written to disk while recording.
     *
     * @return whether they go to disk
     */
    public boolean isToDisk() {
        return toDisk;
    }

    /**
     * It sets the maximum size.
     *
     * @param maxSize the bytes; zero for no limit
     * @throws IllegalArgumentException if it is negative
     */
    public void setMaxSize(final long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("the maximum size cannot be negative");
        }
        this.maxSize = maxSize;
    }

    /**
     * It sets the maximum age of the data.
     *
     * @param maxAge the duration, or {@code null} for no limit
     * @throws IllegalArgumentException if it is negative
     */
    public void setMaxAge(final Duration maxAge) {
        if (maxAge != null && maxAge.isNegative()) {
            throw new IllegalArgumentException("the maximum age cannot be negative");
        }
        this.maxAge = maxAge;
    }

    /**
     * It sets where to dump the data on finishing.
     *
     * @param destination the file, or {@code null} for not dumping
     * @throws IOException if the destination does not serve
     */
    public void setDestination(final Path destination) throws IOException {
        this.destination = destination;
    }

    /**
     * The configured destination.
     *
     * @return the file, or {@code null}
     */
    public Path getDestination() {
        return destination;
    }

    /**
     * The identifier of the recording.
     *
     * @return the identifier
     */
    public long getId() {
        return id;
    }

    /**
     * It sets the name.
     *
     * @param name the name
     * @throws NullPointerException if it is {@code null}
     */
    public void setName(final String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    /**
     * Whether to dump the data when the VM ends.
     *
     * <p>It only has an effect with a destination set: without it there is nowhere to dump to.
     *
     * @param dumpOnExit whether to dump
     */
    public void setDumpOnExit(final boolean dumpOnExit) {
        this.dumpOnExit = dumpOnExit;
    }

    /**
     * Whether it is going to dump when the VM ends.
     *
     * @return whether it dumps
     */
    public boolean getDumpOnExit() {
        return dumpOnExit;
    }

    /**
     * Whether to write to disk while recording.
     *
     * <p>In memory is faster and limits how much can be kept; to disk it stands long recordings and
     * costs input and output.
     *
     * @param toDisk whether to write to disk
     */
    public void setToDisk(final boolean toDisk) {
        this.toDisk = toDisk;
    }

    /**
     * A stream with the events of that interval.
     *
     * @param start the beginning of the interval, or {@code null} for from the start
     * @param end the end, or {@code null} for up to the end
     * @return the stream
     * @throws IOException if it could not be read
     * @throws IllegalStateException in this VM, because there are no data to read
     */
    public InputStream getStream(final Instant start, final Instant end) throws IOException {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * How long the recording is going to last.
     *
     * @return the duration, or {@code null} if it has no limit
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * It sets how long it is going to last.
     *
     * @param duration the duration, or {@code null} for no limit
     */
    public void setDuration(final Duration duration) {
        this.duration = duration;
    }

    /**
     * It enables an event by name and returns its settings in order to go on configuring it.
     *
     * @param name the name of the event
     * @return the settings of that event
     * @throws NullPointerException if it is {@code null}
     */
    public EventSettings enable(final String name) {
        return settingsOf(Objects.requireNonNull(name, "name"), "true");
    }

    /**
     * It disables an event by name.
     *
     * @param name the name of the event
     * @return the settings of that event
     * @throws NullPointerException if it is {@code null}
     */
    public EventSettings disable(final String name) {
        return settingsOf(Objects.requireNonNull(name, "name"), "false");
    }

    /**
     * It enables an event by its class.
     *
     * @param eventClass the class of the event
     * @return the settings of that event
     * @throws NullPointerException if it is {@code null}
     */
    public EventSettings enable(final Class<? extends Event> eventClass) {
        return enable(nameOf(eventClass));
    }

    /**
     * It disables an event by its class.
     *
     * @param eventClass the class of the event
     * @return the settings of that event
     * @throws NullPointerException if it is {@code null}
     */
    public EventSettings disable(final Class<? extends Event> eventClass) {
        return disable(nameOf(eventClass));
    }

    /**
     * The name under which the event appears in the recording.
     *
     * <p>It respects the {@link Name} of the class: enabling by class and enabling by name have to
     * arrive at the same key, or one of the two ways would not work.
     */
    private static String nameOf(final Class<? extends Event> eventClass) {
        Objects.requireNonNull(eventClass, "eventClass");
        final Name n = eventClass.getAnnotation(Name.class);
        return n != null ? n.value() : eventClass.getName();
    }

    private EventSettings settingsOf(final String event, final String enabled) {
        settings.put(event + "#" + Enabled.NAME, enabled);
        return new EventSettingsImpl(event);
    }

    /**
     * The settings of an event, writing straight into the map of the recording.
     *
     * <p>With a name and not anonymous because of #482: the bytecode generator does not emit an
     * anonymous class that is in the initialiser of a field, and it is as well not to depend on the
     * context it is instantiated in.
     */
    private final class EventSettingsImpl extends EventSettings {

        private final String event;

        EventSettingsImpl(final String event) {
            this.event = event;
        }

        public EventSettings with(final String name, final String value) {
            Objects.requireNonNull(name, "name");
            settings.put(event + "#" + name, value);
            return this;
        }
    }
}
