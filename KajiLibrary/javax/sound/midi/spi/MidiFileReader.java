package javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.Sequence;

/**
 * KajiLibrary's javax.sound.midi.spi.MidiFileReader -- lee archivos MIDI de un formato.
 *
 * <p>Se registra como servicio y {@code MidiSystem} lo encuentra solo.
 *
 * <p>Igual que con el audio muestreado, {@code MidiSystem} prueba los lectores de a uno sobre el mismo
 * flujo: hay que marcar antes de mirar y <b>rebobinar</b> si el formato no se reconoce. No reconocerlo
 * se avisa con {@link InvalidMidiDataException}, que {@code MidiSystem} ataja para seguir.
 *
 * <p>Los seis metodos son dos por tres: el encabezado o la obra entera, desde flujo, direccion o
 * archivo.
 */
public abstract class MidiFileReader {

    /** Para las subclases. */
    protected MidiFileReader() {
    }

    /**
     * Que hay en ese flujo, sin leer la obra.
     *
     * <p>Tiene que dejarlo como lo encontro; ver la nota de la clase.
     *
     * @throws InvalidMidiDataException si este lector no lo reconoce
     * @throws IOException si no se pudo leer
     */
    public abstract MidiFileFormat getMidiFileFormat(InputStream stream)
        throws InvalidMidiDataException, IOException;

    /**
     * Idem, desde una direccion.
     *
     * @throws InvalidMidiDataException si este lector no lo reconoce
     * @throws IOException si no se pudo leer
     */
    public abstract MidiFileFormat getMidiFileFormat(URL url)
        throws InvalidMidiDataException, IOException;

    /**
     * Idem, desde un archivo.
     *
     * @throws InvalidMidiDataException si este lector no lo reconoce
     * @throws IOException si no se pudo leer
     */
    public abstract MidiFileFormat getMidiFileFormat(File file)
        throws InvalidMidiDataException, IOException;

    /**
     * La obra entera.
     *
     * @throws InvalidMidiDataException si este lector no lo reconoce
     * @throws IOException si no se pudo leer
     */
    public abstract Sequence getSequence(InputStream stream)
        throws InvalidMidiDataException, IOException;

    /**
     * Idem, desde una direccion.
     *
     * @throws InvalidMidiDataException si este lector no lo reconoce
     * @throws IOException si no se pudo leer
     */
    public abstract Sequence getSequence(URL url) throws InvalidMidiDataException, IOException;

    /**
     * Idem, desde un archivo.
     *
     * @throws InvalidMidiDataException si este lector no lo reconoce
     * @throws IOException si no se pudo leer
     */
    public abstract Sequence getSequence(File file) throws InvalidMidiDataException, IOException;
}
