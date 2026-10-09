package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.VoiceStatus -- what a synthesizer's voice is doing.
 *
 * <p>A <b>voice</b> is a note sounding. A synthesizer has a fixed number --its polyphony-- and when
 * they run out, the new note steals the voice of the oldest. This object is the snapshot of one of
 * them.
 *
 * <p>The six fields are <b>public and mutable</b>, which is unusual for the JDK. The reason is
 * performance: {@code Synthesizer.getVoiceStatus()} returns a whole array and is called many times
 * per second to draw a meter; with six accessors per voice and sixty-four voices, the cost would
 * show.
 *
 * <p>{@link #active} decides everything: if it is false, the other five mean nothing and should not
 * be read.
 */
public class VoiceStatus {

    /** Whether it is sounding. See the class note: if it is false, the rest does not hold. */
    public boolean active = false;

    /** On which MIDI channel. */
    public int channel = 0;

    /** Which bank the sound came from. */
    public int bank = 0;

    /** Which program. */
    public int program = 0;

    /** Which note, from 0 to 127. */
    public int note = 0;

    /** How hard. */
    public int volume = 0;

    /** All zero and not sounding. */
    public VoiceStatus() {
    }
}
