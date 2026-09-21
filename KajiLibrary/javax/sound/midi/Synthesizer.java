package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Synthesizer -- a device that turns MIDI into sound.
 *
 * <p>A {@link MidiDevice} that also has channels, voices and a sound bank.
 *
 * <h2>Polyphony and voices</h2>
 *
 * <p>{@link #getMaxPolyphony} is how many notes can sound at once. When it is exceeded, the
 * synthesizer steals the voice of the oldest note: it does not fail, it cuts. That is why a dense
 * passage can lose notes without anything warning.
 *
 * <p>{@link #getVoiceStatus} <b>always</b> returns an array the size of the polyphony; the free
 * voices come with {@code active} false. See {@link VoiceStatus}.
 *
 * <h2>{@link #getLatency}</h2>
 *
 * <p>Microseconds between a message arriving and it being heard. It is what has to be compensated
 * when synchronizing with something else, and what makes playing live with a software synthesizer
 * feel slow.
 *
 * <h2>Loading and unloading instruments</h2>
 *
 * <p>{@link #loadAllInstruments} loads a whole bank, which in a large SoundFont is hundreds of
 * megabytes. {@link #loadInstruments} loads only the ones needed, and it is the right thing when it
 * is known which instruments the piece uses.
 *
 * <p>{@link #remapInstrument} substitutes one for another without touching the music: it is how a
 * sound one does not like is replaced without editing the file's program changes.
 */
public interface Synthesizer extends MidiDevice {

    /** How many notes can sound at once. See the class note. */
    int getMaxPolyphony();

    /** Microseconds of delay. See the class note. */
    long getLatency();

    /** The sixteen channels. */
    MidiChannel[] getChannels();

    /** The state of all the voices, busy and free. See the class note. */
    VoiceStatus[] getVoiceStatus();

    /** Whether it understands that bank. */
    boolean isSoundbankSupported(Soundbank soundbank);

    /**
     * Loads an instrument.
     *
     * @return whether it could
     * @throws IllegalArgumentException if the instrument is not from a bank this one supports
     */
    boolean loadInstrument(Instrument instrument);

    /**
     * Unloads it.
     *
     * @throws IllegalArgumentException if the instrument is not from a bank this one supports
     */
    void unloadInstrument(Instrument instrument);

    /**
     * Makes one sound in place of another. See the class note.
     *
     * @param from the one the music asks for
     * @param to the one that is going to sound
     * @return whether it could
     * @throws IllegalArgumentException if either is not from a supported bank
     */
    boolean remapInstrument(Instrument from, Instrument to);

    /** The bank it comes with from the factory, or null. */
    Soundbank getDefaultSoundbank();

    /** Everything that could be loaded. */
    Instrument[] getAvailableInstruments();

    /** What is loaded now. */
    Instrument[] getLoadedInstruments();

    /**
     * Loads a whole bank. See the class note on memory.
     *
     * @return whether it could
     */
    boolean loadAllInstruments(Soundbank soundbank);

    /** Unloads it whole. */
    void unloadAllInstruments(Soundbank soundbank);

    /**
     * Loads only those sounds of the bank.
     *
     * @return whether all could be loaded
     */
    boolean loadInstruments(Soundbank soundbank, Patch[] patchList);

    /** Unloads them. */
    void unloadInstruments(Soundbank soundbank, Patch[] patchList);
}
