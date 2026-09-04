package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Synthesizer -- un dispositivo que convierte MIDI en sonido.
 *
 * <p>Un {@link MidiDevice} que ademas tiene canales, voces y banco de sonidos.
 *
 * <h2>Polifonia y voces</h2>
 *
 * <p>{@link #getMaxPolyphony} es cuantas notas pueden sonar a la vez. Al pasarse, el sintetizador le
 * roba la voz a la nota mas vieja: no falla, corta. Es por eso que un pasaje denso puede perder notas
 * sin que nada avise.
 *
 * <p>{@link #getVoiceStatus} devuelve <b>siempre</b> un arreglo del tamano de la polifonia; las voces
 * libres vienen con {@code active} en false. Ver {@link VoiceStatus}.
 *
 * <h2>{@link #getLatency}</h2>
 *
 * <p>Microsegundos entre que llega un mensaje y que se oye. Es lo que hay que compensar al sincronizar
 * con otra cosa, y lo que hace que tocar en vivo con un sintetizador por software se sienta lento.
 *
 * <h2>Cargar y descargar instrumentos</h2>
 *
 * <p>{@link #loadAllInstruments} carga un banco entero, que en un SoundFont grande son cientos de
 * megabytes. {@link #loadInstruments} carga solo los que hacen falta, y es lo correcto cuando se sabe
 * que instrumentos usa la obra.
 *
 * <p>{@link #remapInstrument} sustituye uno por otro sin tocar la musica: es como se reemplaza un
 * sonido que no gusta sin editar los cambios de programa del archivo.
 */
public interface Synthesizer extends MidiDevice {

    /** Cuantas notas pueden sonar a la vez. Ver la nota de la clase. */
    int getMaxPolyphony();

    /** Microsegundos de retardo. Ver la nota de la clase. */
    long getLatency();

    /** Los dieciseis canales. */
    MidiChannel[] getChannels();

    /** El estado de todas las voces, ocupadas y libres. Ver la nota de la clase. */
    VoiceStatus[] getVoiceStatus();

    /** Si entiende ese banco. */
    boolean isSoundbankSupported(Soundbank soundbank);

    /**
     * Carga un instrumento.
     *
     * @return si se pudo
     * @throws IllegalArgumentException si el instrumento no es de un banco que este soporte
     */
    boolean loadInstrument(Instrument instrument);

    /**
     * Lo descarga.
     *
     * @throws IllegalArgumentException si el instrumento no es de un banco que este soporte
     */
    void unloadInstrument(Instrument instrument);

    /**
     * Hace que uno suene en lugar de otro. Ver la nota de la clase.
     *
     * @param from el que la musica pide
     * @param to el que va a sonar
     * @return si se pudo
     * @throws IllegalArgumentException si alguno no es de un banco soportado
     */
    boolean remapInstrument(Instrument from, Instrument to);

    /** El banco que trae de fabrica, o null. */
    Soundbank getDefaultSoundbank();

    /** Todo lo que se podria cargar. */
    Instrument[] getAvailableInstruments();

    /** Lo que esta cargado ahora. */
    Instrument[] getLoadedInstruments();

    /**
     * Carga un banco entero. Ver la nota de la clase sobre la memoria.
     *
     * @return si se pudo
     */
    boolean loadAllInstruments(Soundbank soundbank);

    /** Lo descarga entero. */
    void unloadAllInstruments(Soundbank soundbank);

    /**
     * Carga solo esos sonidos del banco.
     *
     * @return si se pudieron cargar todos
     */
    boolean loadInstruments(Soundbank soundbank, Patch[] patchList);

    /** Los descarga. */
    void unloadInstruments(Soundbank soundbank, Patch[] patchList);
}
