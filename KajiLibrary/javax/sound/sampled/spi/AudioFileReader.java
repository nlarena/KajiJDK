package javax.sound.sampled.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * KajiLibrary's javax.sound.sampled.spi.AudioFileReader -- lee archivos de audio de un formato.
 *
 * <p>Lo que implementa quien agrega soporte para leer un formato nuevo. Se registra como servicio y
 * {@code AudioSystem} lo encuentra solo.
 *
 * <h2>La regla de las marcas</h2>
 *
 * <p>{@code AudioSystem} prueba los lectores <b>de a uno</b> sobre el mismo flujo. Por eso un lector
 * tiene que marcar antes de mirar los bytes y <b>rebobinar</b> si no reconoce el formato: si no, el
 * siguiente lector recibe un flujo ya consumido y ninguno funciona.
 *
 * <p>No reconocer un formato se avisa con {@link UnsupportedAudioFileException}, que
 * {@code AudioSystem} ataja para seguir con el proximo. Devolver null no esta permitido.
 *
 * <p>Las seis operaciones son tres por dos: formato o flujo, desde archivo, direccion o flujo.
 */
public abstract class AudioFileReader {

    /** Para las subclases. */
    protected AudioFileReader() {
    }

    /**
     * Que hay en ese flujo.
     *
     * <p>Tiene que dejarlo como lo encontro; ver la nota de la clase.
     *
     * @throws UnsupportedAudioFileException si este lector no reconoce el formato
     * @throws IOException si no se pudo leer
     */
    public abstract AudioFileFormat getAudioFileFormat(InputStream stream)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Idem, desde una direccion.
     *
     * @throws UnsupportedAudioFileException si este lector no reconoce el formato
     * @throws IOException si no se pudo leer
     */
    public abstract AudioFileFormat getAudioFileFormat(URL url)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Idem, desde un archivo.
     *
     * @throws UnsupportedAudioFileException si este lector no reconoce el formato
     * @throws IOException si no se pudo leer
     */
    public abstract AudioFileFormat getAudioFileFormat(File file)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Un flujo de audio desde ese flujo de bytes.
     *
     * @throws UnsupportedAudioFileException si este lector no reconoce el formato
     * @throws IOException si no se pudo leer
     */
    public abstract AudioInputStream getAudioInputStream(InputStream stream)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Idem, desde una direccion.
     *
     * @throws UnsupportedAudioFileException si este lector no reconoce el formato
     * @throws IOException si no se pudo leer
     */
    public abstract AudioInputStream getAudioInputStream(URL url)
        throws UnsupportedAudioFileException, IOException;

    /**
     * Idem, desde un archivo.
     *
     * @throws UnsupportedAudioFileException si este lector no reconoce el formato
     * @throws IOException si no se pudo leer
     */
    public abstract AudioInputStream getAudioInputStream(File file)
        throws UnsupportedAudioFileException, IOException;
}
