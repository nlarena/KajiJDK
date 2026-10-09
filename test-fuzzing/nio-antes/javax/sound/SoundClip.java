package javax.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * KajiLibrary's javax.sound.SoundClip -- play a short sound, nothing more.
 *
 * <p>It arrived in Java 25 (the note said 22; the JDK marks it {@code @since 25}) and it is the
 * answer to an old complaint: playing a sound file with the {@code javax.sound.sampled} APIs
 * requires opening a line, choosing a format, writing bytes and closing everything. This is two
 * lines.
 *
 * <h2>Everything that can fail fails in the factory</h2>
 *
 * <p>{@link #createSoundClip} is the only thing that throws. The five instance methods declare
 * nothing and do not fail: if the file cannot be played, {@link #canPlay} returns false and
 * {@link #play} does nothing.
 *
 * <p>It is a deliberate design, and it is worth noting: <b>creating the clip does not guarantee
 * that it sounds</b>. A text file produces a perfectly valid {@code SoundClip} with {@code
 * canPlay()} false. The factory only complains if the file cannot be <b>read</b>.
 *
 * <h2>{@link #play} returns right away</h2>
 *
 * <p>Playback goes on in another thread. {@link #loop} repeats until somebody calls {@link #stop},
 * and calling {@code play} on a clip that is already sounding restarts it from the beginning.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library has no audio output: neither decoders nor access to the sound card. {@link
 * #createSoundClip} really works --it opens the file and throws {@link IOException} if it cannot be
 * read, just like the JDK-- and the clip it returns has {@code canPlay()} false.
 *
 * <p>That is not an excuse: it is <b>exactly</b> what the JDK does with a file it does not know how
 * to decode, and it is checked against JDK 25. A program written against this class behaves the
 * same on both sides; the only thing that changes is that here no file can be decoded.
 */
public final class SoundClip {

    /** Whether it could be decoded; here never. See the class note. */
    private final boolean playable;

    /** Reached through {@link #createSoundClip}. */
    private SoundClip(boolean playable) {
        this.playable = playable;
    }

    /**
     * A clip from that file.
     *
     * <p>It reads the file to check that it can. Whether the result sounds is asked afterwards with
     * {@link #canPlay}; see the class note.
     *
     * @throws NullPointerException if the file is null
     * @throws IOException if it cannot be read
     */
    public static SoundClip createSoundClip(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file must not be null");
        }
        // It is really opened: it is what turns "the file does not exist" into the IOException the
        // contract promises, and not into a mute clip that deceives.
        InputStream in = new FileInputStream(file);
        try {
            in.read();
        } finally {
            in.close();
        }
        return new SoundClip(false);
    }

    /** Whether this clip can be played. See the class note. */
    public boolean canPlay() {
        return this.playable;
    }

    /** Whether it is sounding now. */
    public boolean isPlaying() {
        return false;
    }

    /** Plays it from the beginning. It does nothing if {@link #canPlay} is false. */
    public void play() {
    }

    /** Repeats it until {@link #stop}. It does nothing if {@link #canPlay} is false. */
    public void loop() {
    }

    /** Stops it. It does nothing if it was not sounding. */
    public void stop() {
    }
}
