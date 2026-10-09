package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiChannel -- one of the sixteen channels of a synthesizer.
 *
 * <p>It is the convenience API: instead of building a {@link ShortMessage} and sending it through a
 * {@link Receiver}, a method is called. It does exactly the same.
 *
 * <h2>Notes are turned off in two ways</h2>
 *
 * <p>{@link #noteOff(int, int)} carries the <b>release</b> velocity --how fast the finger is
 * lifted--, which some instruments use to change how the sound dies away. {@link #noteOff(int)} is
 * the shortcut when that does not matter.
 *
 * <h2>{@link #allNotesOff} and {@link #allSoundOff} are not the same</h2>
 *
 * <p>The first releases the notes: whatever is sounding dies away as it naturally would, with its
 * resonance. The second <b>cuts</b> the sound immediately.
 *
 * <p>For a panic button the second is wanted. The first leaves a note held by the pedal sounding.
 *
 * <h2>Mute and solo belong to the sequencer, not to the sound</h2>
 *
 * <p>{@link #setMute} and {@link #setSolo} do not touch the volume: they tell the sequencer not to
 * send --or to send only-- this channel's events. And they are optional: a synthesizer that does
 * not support them ignores them and {@link #getMute} keeps returning false.
 *
 * <h2>{@link #localControl}</h2>
 *
 * <p>Off, the keyboard stops playing its own synthesizer. It is what is done when sequencing:
 * otherwise each note sounds twice --once from the keyboard and once from the sequencer's echo--
 * with an audible delay.
 */
public interface MidiChannel {

    /** Plays a note. Velocity 0 turns it off; see {@link ShortMessage}. */
    void noteOn(int noteNumber, int velocity);

    /** Releases it, with release velocity. See the class note. */
    void noteOff(int noteNumber, int velocity);

    /** Releases it. */
    void noteOff(int noteNumber);

    /** Pressure on a key already pressed. */
    void setPolyPressure(int noteNumber, int pressure);

    /** How much pressure that key has. */
    int getPolyPressure(int noteNumber);

    /** Pressure on the whole channel. */
    void setChannelPressure(int pressure);

    /** How much pressure the channel has. */
    int getChannelPressure();

    /** Moves a controller. */
    void controlChange(int controller, int value);

    /** Where that controller is. */
    int getController(int controller);

    /** Changes the sound within the current bank. */
    void programChange(int program);

    /** Changes bank and sound. */
    void programChange(int bank, int program);

    /** Which sound is set. */
    int getProgram();

    /** Moves the pitch wheel; 8192 is the centre. */
    void setPitchBend(int bend);

    /** Where the pitch wheel is. */
    int getPitchBend();

    /** Sets all the controllers back to their default value. */
    void resetAllControllers();

    /** Releases all the notes. See the class note. */
    void allNotesOff();

    /** Cuts all the sound immediately. See the class note. */
    void allSoundOff();

    /**
     * Connects or disconnects the keyboard from its own synthesizer. See the class note.
     *
     * @return how it ended up; it may not be what was asked
     */
    boolean localControl(boolean on);

    /** Monophonic mode: a single note at a time. */
    void setMono(boolean on);

    /** Whether it is monophonic. */
    boolean getMono();

    /** Omni mode: respond to all channels. */
    void setOmni(boolean on);

    /** Whether it is in omni. */
    boolean getOmni();

    /** Mutes this channel in the sequencer. See the class note. */
    void setMute(boolean mute);

    /** Whether it is muted. */
    boolean getMute();

    /** Lets only this channel sound. See the class note. */
    void setSolo(boolean soloState);

    /** Whether it is soloed. */
    boolean getSolo();
}
