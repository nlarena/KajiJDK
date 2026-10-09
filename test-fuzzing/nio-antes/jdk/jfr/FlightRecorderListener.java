package jdk.jfr;

/**
 * Notices about the recorder and about the changes of state of the recordings.
 *
 * <h2>What finding out that the recorder started is for</h2>
 *
 * <p>Because JFR is initialised lazily: it may not exist when the application starts and appear
 * later, if somebody switches it on from outside with {@code jcmd}. Code that wants to configure
 * something as soon as it exists cannot ask once and give up -- it has to register itself and wait.
 *
 * <p>{@link #recorderInitialized} is that notice. If the recorder was <strong>already</strong>
 * initialised when one registered, the notice arrives all the same and at once, so there is no race
 * to handle.
 *
 * <h2>The two methods are {@code default}</h2>
 *
 * <p>Almost nobody wants both. Leaving them with an empty body avoids the empty method of
 * compromise that would have to be written in each implementation.
 *
 * @since 9
 */
public interface FlightRecorderListener {

    /**
     * The recorder was initialised.
     *
     * @param recorder the recorder
     */
    default void recorderInitialized(FlightRecorder recorder) {
    }

    /**
     * A recording changed state.
     *
     * @param recording the recording, already with its new state
     */
    default void recordingStateChanged(Recording recording) {
    }
}
