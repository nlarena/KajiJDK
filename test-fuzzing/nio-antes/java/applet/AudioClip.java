package java.applet;

/**
 * A sound that can be played, looped and stopped.
 *
 * <p>It is Java's oldest sound interface —older than `javax.sound`— and that is why it says nothing
 * about the format or the volume: only three verbs. {@link #loop} is not "play several times" but
 * "play until somebody stops it", which is the difference between background music and a chime.
 *
 * @deprecated the applet model has been deprecated since Java 9 and marked for removal since 17;
 *     sound is handled with `javax.sound.sampled`.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AudioClip {

    /** Plays it once from the start; if it was already sounding, it starts again. */
    void play();

    /** Plays it in a loop until somebody calls {@link #stop}. */
    void loop();

    /** Stops it, whether it is sounding once or in a loop. */
    void stop();
}
