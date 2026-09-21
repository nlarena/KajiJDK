package jdk.jfr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The recorder: the point of entry to JFR.
 *
 * <h2>Why {@link #getFlightRecorder} may fail</h2>
 *
 * <p>Because JFR may not be there. It is not a theoretical supposition: the VM can be started with
 * {@code -XX:-FlightRecorder}, and there are implementations of Java with no JFR. That is why the
 * method declares {@link IllegalStateException} and that is why {@link #isAvailable} exists, which
 * is what one has to ask beforehand.
 *
 * <p>That is the part of the contract that makes this class usable here: the absence of JFR
 * <strong>was already foreseen by the API</strong>, and answering it is fulfilling the contract,
 * not breaking it.
 *
 * <h2>Lazy initialisation</h2>
 *
 * <p>{@link #isAvailable} says whether JFR <strong>can</strong> be used; {@link #isInitialized}
 * says whether it has already started. They are different because starting costs --buffers have to
 * be reserved and the configuration read-- and it is not done until somebody asks for it. A monitor
 * that wants to find out without forcing it asks the second one and registers itself with {@link
 * #addListener}.
 *
 * <h2>State in this VM</h2>
 *
 * <p>{@link #isAvailable} returns {@code false} and {@link #getFlightRecorder} throws
 * {@link IllegalStateException}, which is exactly what the API defines for a VM with no JFR.
 *
 * <p>What does work is the registration of listeners: {@link #addListener} and
 * {@link #removeListener} really keep and take away. It is the right thing -- a listener registered
 * before JFR appears is precisely the use case of that interface, and discarding it silently would
 * be worse than not having it.
 *
 * @since 9
 */
public final class FlightRecorder {

    private static final String NOT_AVAILABLE =
            "Flight Recorder is not available in this VM";

    /**
     * The registered listeners.
     *
     * <p>Copy on write: they are walked on each notice and modified almost never, which is exactly
     * the case that structure exists for.
     */
    private static final List<FlightRecorderListener> LISTENERS =
            new CopyOnWriteArrayList<FlightRecorderListener>();

    private FlightRecorder() {
    }

    /**
     * The recordings there are now.
     *
     * @return the recordings
     */
    public List<Recording> getRecordings() {
        return Collections.emptyList();
    }

    /**
     * A recording with whatever is in the buffers at this moment.
     *
     * <p>It is the operation that makes it useful to leave JFR switched on with no destination: it
     * records into a circular buffer and, when something goes wrong, this takes away what was left
     * of the last few minutes.
     *
     * @return the snapshot
     * @throws IllegalStateException in this VM, because there are no buffers to take it out of
     */
    public Recording takeSnapshot() {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It registers a type of event.
     *
     * <p>It is only needed for the classes marked {@code @Registered(false)}: the others register
     * themselves when they are loaded.
     *
     * @param eventClass the class of the event
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalStateException in this VM
     */
    public static void register(final Class<? extends Event> eventClass) {
        Objects.requireNonNull(eventClass, "eventClass");
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It takes a type of event out of the register.
     *
     * @param eventClass the class of the event
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalStateException in this VM
     */
    public static void unregister(final Class<? extends Event> eventClass) {
        Objects.requireNonNull(eventClass, "eventClass");
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * The recorder.
     *
     * @return the recorder
     * @throws IllegalStateException if JFR is not available, which is the case in this VM
     */
    public static FlightRecorder getFlightRecorder() throws IllegalStateException {
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It registers an action that is going to be run periodically in order to emit an event.
     *
     * <p>It is how a periodic event is implemented: JFR calls the action every so often, and the
     * action puts the event together and emits it. The frequency comes from the {@code period}
     * setting of the type.
     *
     * @param eventClass the class of the event
     * @param hook the action
     * @throws NullPointerException if either is {@code null}
     * @throws IllegalStateException in this VM
     */
    public static void addPeriodicEvent(final Class<? extends Event> eventClass,
            final Runnable hook) {
        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(hook, "hook");
        throw new IllegalStateException(NOT_AVAILABLE);
    }

    /**
     * It takes a periodic action away.
     *
     * @param hook the action
     * @return {@code false}, because in this VM none could ever be registered
     * @throws NullPointerException if it is {@code null}
     */
    public static boolean removePeriodicEvent(final Runnable hook) {
        Objects.requireNonNull(hook, "hook");
        return false;
    }

    /**
     * The registered types of event.
     *
     * @return the types
     */
    public List<EventType> getEventTypes() {
        return Collections.emptyList();
    }

    /**
     * It registers a listener.
     *
     * <p>If the recorder were already initialised, the {@code recorderInitialized} notice would
     * arrive at once. In this VM it never arrives, because the recorder is not initialised.
     *
     * @param changeListener the listener
     * @throws NullPointerException if it is {@code null}
     */
    public static void addListener(final FlightRecorderListener changeListener) {
        LISTENERS.add(Objects.requireNonNull(changeListener, "changeListener"));
    }

    /**
     * It takes a listener away.
     *
     * @param changeListener the listener
     * @return whether it was registered
     * @throws NullPointerException if it is {@code null}
     */
    public static boolean removeListener(final FlightRecorderListener changeListener) {
        return LISTENERS.remove(Objects.requireNonNull(changeListener, "changeListener"));
    }

    /**
     * Whether JFR can be used in this VM.
     *
     * @return {@code false} in this library
     */
    public static boolean isAvailable() {
        return false;
    }

    /**
     * Whether JFR has already started.
     *
     * @return {@code false} in this library
     */
    public static boolean isInitialized() {
        return false;
    }

    /** The registered listeners; for the classes of the package that have to notify them. */
    static List<FlightRecorderListener> listeners() {
        return new ArrayList<FlightRecorderListener>(LISTENERS);
    }
}
