package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Soundbank -- a collection of sounds for a synthesizer.
 *
 * <p>MIDI sends note numbers, not sound. What sounds is supplied by the synthesizer, and it takes
 * it from here: a sound bank is the file --SoundFont, DLS-- that says how each instrument sounds.
 *
 * <p>That is why the same MIDI file sounds different on two machines: the bank changes, not the
 * music.
 *
 * <p>{@link #getInstruments} are the playable sounds; {@link #getResources} also includes what the
 * instruments use inside --the audio samples-- and that cannot be played directly.
 */
public interface Soundbank {

    /** What it is called. */
    String getName();

    /** Which version. */
    String getVersion();

    /** Who made it. */
    String getVendor();

    /** What it contains. */
    String getDescription();

    /** Everything it contains, playable or not. See the class note. */
    SoundbankResource[] getResources();

    /** The playable sounds. */
    Instrument[] getInstruments();

    /** The sound at that address, or null. */
    Instrument getInstrument(Patch patch);
}
