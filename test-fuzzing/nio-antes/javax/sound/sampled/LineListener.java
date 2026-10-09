package javax.sound.sampled;

import java.util.EventListener;

/**
 * KajiLibrary's javax.sound.sampled.LineListener -- listens to a line's state changes.
 *
 * <p>A single method for the four events; the type is read from the {@link LineEvent}.
 *
 * <p>It is the only way to know that a clip finished sounding: {@code Clip.start()} returns right
 * away and playback goes on in another thread. Waiting with pauses is what almost everybody does
 * and it always comes out wrong.
 *
 * <p>The notice arrives on a thread of the audio system, not on the one that asked for the
 * operation. Blocking it delays the audio of the whole program.
 */
public interface LineListener extends EventListener {

    /** Something changed in a line. */
    void update(LineEvent event);
}
