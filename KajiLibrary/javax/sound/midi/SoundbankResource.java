package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.SoundbankResource -- something inside a sound bank.
 *
 * <p>An instrument, or an audio sample the instruments use. It is abstract and is only implemented
 * by whoever reads a concrete bank format.
 *
 * <h2>{@link #getDataClass} and {@link #getData}</h2>
 *
 * <p>The datum is an {@link Object} because it depends on the resource: it can be a {@code byte[]}
 * of samples, an {@code AudioInputStream}, or something of the provider's own. {@code getDataClass}
 * says which class it is, so that one can decide before loading it.
 *
 * <p>Both can return null, and they mean different things: {@code getDataClass} null is "I am not
 * saying"; {@code getData} null is "it is not loaded". The second is normal in a large bank that
 * loads the samples on demand.
 */
public abstract class SoundbankResource {

    /** Which bank it came from. */
    private final Soundbank soundBank;

    /** What it is called. */
    private final String name;

    /** Which class its datum is. */
    private final Class<?> dataClass;

    /** For the subclasses. */
    protected SoundbankResource(Soundbank soundBank, String name, Class<?> dataClass) {
        this.soundBank = soundBank;
        this.name = name;
        this.dataClass = dataClass;
    }

    /** Which bank it came from. */
    public Soundbank getSoundbank() {
        return this.soundBank;
    }

    /** What it is called. */
    public String getName() {
        return this.name;
    }

    /** Which class its datum is, or null. See the class note. */
    public Class<?> getDataClass() {
        return this.dataClass;
    }

    /** The datum, or null if it is not loaded. See the class note. */
    public abstract Object getData();
}
