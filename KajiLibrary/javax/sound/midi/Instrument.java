package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Instrument -- a playable sound of a bank.
 *
 * <p>A {@link SoundbankResource} with an address: its {@link Patch}. That is the whole difference,
 * and it is the one that matters -- an instrument can be selected with a program change, a loose
 * sample cannot.
 *
 * <p>It is loaded into a synthesizer with {@code Synthesizer.loadInstrument}. Loading a whole large
 * bank can take a lot of memory; that is why the methods that load one at a time exist.
 */
public abstract class Instrument extends SoundbankResource {

    /** Where it is in the bank. */
    private final Patch patch;

    /** For the subclasses. */
    protected Instrument(Soundbank soundbank, Patch patch, String name, Class<?> dataClass) {
        super(soundbank, name, dataClass);
        this.patch = patch;
    }

    /** Where it is in the bank. */
    public Patch getPatch() {
        return this.patch;
    }
}
