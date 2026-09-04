package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.SoundbankResource -- algo que hay adentro de un banco de sonidos.
 *
 * <p>Un instrumento, o una muestra de audio que los instrumentos usan. Es abstracta y solo la
 * implementa quien lee un formato de banco concreto.
 *
 * <h2>{@link #getDataClass} y {@link #getData}</h2>
 *
 * <p>El dato es {@link Object} porque depende del recurso: puede ser un {@code byte[]} de muestras, un
 * {@code AudioInputStream}, o algo propio del proveedor. {@code getDataClass} dice de que clase es,
 * para poder decidir antes de cargarlo.
 *
 * <p>Los dos pueden devolver null, y significan cosas distintas: {@code getDataClass} null es "no lo
 * digo"; {@code getData} null es "no esta cargado". El segundo es normal en un banco grande que carga
 * las muestras a demanda.
 */
public abstract class SoundbankResource {

    /** De que banco salio. */
    private final Soundbank soundBank;

    /** Como se llama. */
    private final String name;

    /** De que clase es su dato. */
    private final Class<?> dataClass;

    /** Para las subclases. */
    protected SoundbankResource(Soundbank soundBank, String name, Class<?> dataClass) {
        this.soundBank = soundBank;
        this.name = name;
        this.dataClass = dataClass;
    }

    /** De que banco salio. */
    public Soundbank getSoundbank() {
        return this.soundBank;
    }

    /** Como se llama. */
    public String getName() {
        return this.name;
    }

    /** De que clase es su dato, o null. Ver la nota de la clase. */
    public Class<?> getDataClass() {
        return this.dataClass;
    }

    /** El dato, o null si no esta cargado. Ver la nota de la clase. */
    public abstract Object getData();
}
