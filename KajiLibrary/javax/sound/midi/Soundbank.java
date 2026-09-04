package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Soundbank -- una coleccion de sonidos para un sintetizador.
 *
 * <p>MIDI manda numeros de nota, no sonido. Lo que suena lo pone el sintetizador, y de aca lo saca: un
 * banco de sonidos es el archivo --SoundFont, DLS-- que dice como suena cada instrumento.
 *
 * <p>Por eso el mismo archivo MIDI suena distinto en dos maquinas: cambia el banco, no la musica.
 *
 * <p>{@link #getInstruments} son los sonidos tocables; {@link #getResources} incluye ademas lo que los
 * instrumentos usan por dentro --las muestras de audio-- y que no se puede tocar directamente.
 */
public interface Soundbank {

    /** Como se llama. */
    String getName();

    /** Que version. */
    String getVersion();

    /** Quien lo hizo. */
    String getVendor();

    /** Que trae. */
    String getDescription();

    /** Todo lo que contiene, tocable o no. Ver la nota de la clase. */
    SoundbankResource[] getResources();

    /** Los sonidos tocables. */
    Instrument[] getInstruments();

    /** El sonido de esa direccion, o null. */
    Instrument getInstrument(Patch patch);
}
