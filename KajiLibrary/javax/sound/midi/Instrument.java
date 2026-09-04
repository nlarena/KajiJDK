package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Instrument -- un sonido tocable de un banco.
 *
 * <p>Un {@link SoundbankResource} con una direccion: su {@link Patch}. Esa es toda la diferencia, y es
 * la que importa -- un instrumento se puede seleccionar con un cambio de programa, una muestra suelta
 * no.
 *
 * <p>Se carga en un sintetizador con {@code Synthesizer.loadInstrument}. Cargar todo un banco grande
 * puede llevar bastante memoria; por eso existen los metodos que cargan de a uno.
 */
public abstract class Instrument extends SoundbankResource {

    /** Donde esta en el banco. */
    private final Patch patch;

    /** Para las subclases. */
    protected Instrument(Soundbank soundbank, Patch patch, String name, Class<?> dataClass) {
        super(soundbank, name, dataClass);
        this.patch = patch;
    }

    /** Donde esta en el banco. */
    public Patch getPatch() {
        return this.patch;
    }
}
