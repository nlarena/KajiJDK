package javax.sound.sampled;

import java.security.BasicPermission;

/**
 * KajiLibrary's javax.sound.sampled.AudioPermission -- permission to use audio.
 *
 * <p>Two names: {@code "play"} to play and {@code "record"} to capture. The second was the one that
 * mattered -- an applet that could open the microphone without permission is an evident problem.
 *
 * <p>Marked for removal together with the whole {@code SecurityManager} mechanism, which no longer
 * controls anything. It is kept so that old code compiles.
 */
@Deprecated(since = "24", forRemoval = true)
public class AudioPermission extends BasicPermission {

    private static final long serialVersionUID = -5518053473477801126L;

    /**
     * @param name {@code "play"}, {@code "record"} or {@code "*"}
     * @throws NullPointerException if it is null
     * @throws IllegalArgumentException if it is empty
     */
    public AudioPermission(String name) {
        super(name);
    }

    /**
     * Likewise; the actions are not used.
     *
     * @param actions it is ignored
     */
    public AudioPermission(String name, String actions) {
        super(name, actions);
    }
}
