package javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;

/**
 * KajiLibrary's javax.sound.midi.spi.SoundbankReader -- lee bancos de sonidos.
 *
 * <p>SoundFont, DLS, o el formato propio de un fabricante. Se registra como servicio y
 * {@code MidiSystem} lo encuentra solo.
 *
 * <p>A diferencia de {@link MidiFileReader}, aca "no lo reconozco" se avisa devolviendo <b>null</b>, no
 * lanzando. {@code MidiSystem} sigue con el proximo lector y solo lanza si ninguno pudo.
 *
 * <p>Es una inconsistencia del JDK entre dos interfaces hermanas, y hay que respetarla: un lector que
 * lance en lugar de devolver null corta la busqueda.
 */
public abstract class SoundbankReader {

    /** Para las subclases. */
    protected SoundbankReader() {
    }

    /**
     * El banco de esa direccion, o null si no lo reconoce. Ver la nota de la clase.
     *
     * @throws InvalidMidiDataException si lo reconoce y esta roto
     * @throws IOException si no se pudo leer
     */
    public abstract Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException;

    /**
     * Idem, desde un flujo.
     *
     * @throws InvalidMidiDataException si lo reconoce y esta roto
     * @throws IOException si no se pudo leer
     */
    public abstract Soundbank getSoundbank(InputStream stream)
        throws InvalidMidiDataException, IOException;

    /**
     * Idem, desde un archivo.
     *
     * @throws InvalidMidiDataException si lo reconoce y esta roto
     * @throws IOException si no se pudo leer
     */
    public abstract Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException;
}
