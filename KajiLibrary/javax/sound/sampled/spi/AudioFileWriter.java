package javax.sound.sampled.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * KajiLibrary's javax.sound.sampled.spi.AudioFileWriter -- escribe archivos de audio de un formato.
 *
 * <p>El espejo de {@link AudioFileReader}. Se registra igual y {@code AudioSystem} lo encuentra igual.
 *
 * <h2>Los dos {@code getAudioFileTypes}</h2>
 *
 * <p>El sin argumentos dice que tipos sabe escribir <b>en general</b>; el que toma un flujo dice
 * cuales puede escribir <b>con ese contenido</b>. La diferencia importa: un escritor puede saber hacer
 * WAV y no poder guardar en WAV un flujo de largo desconocido, porque el encabezado WAV lleva el
 * tamano y hay que saberlo antes.
 *
 * <p>Los dos {@code isFileTypeSupported} vienen implementados sobre los anteriores; una subclase no
 * necesita tocarlos.
 */
public abstract class AudioFileWriter {

    /** Para las subclases. */
    protected AudioFileWriter() {
    }

    /** Que tipos sabe escribir en general. Ver la nota de la clase. */
    public abstract AudioFileFormat.Type[] getAudioFileTypes();

    /** Si sabe escribir ese tipo. */
    public boolean isFileTypeSupported(AudioFileFormat.Type fileType) {
        AudioFileFormat.Type[] types = getAudioFileTypes();
        int i = 0;
        while (types != null && i < types.length) {
            if (fileType.equals(types[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** Que tipos puede escribir con ese contenido. Ver la nota de la clase. */
    public abstract AudioFileFormat.Type[] getAudioFileTypes(AudioInputStream stream);

    /** Si puede escribir ese tipo con ese contenido. */
    public boolean isFileTypeSupported(AudioFileFormat.Type fileType, AudioInputStream stream) {
        AudioFileFormat.Type[] types = getAudioFileTypes(stream);
        int i = 0;
        while (types != null && i < types.length) {
            if (fileType.equals(types[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Escribe.
     *
     * @return cuantos bytes se escribieron
     * @throws IOException si no se pudo escribir
     * @throws IllegalArgumentException si no soporta ese tipo con ese contenido
     */
    public abstract int write(AudioInputStream stream, AudioFileFormat.Type fileType,
                              OutputStream out) throws IOException;

    /**
     * Idem, a un archivo.
     *
     * @return cuantos bytes se escribieron
     * @throws IOException si no se pudo escribir
     * @throws IllegalArgumentException si no soporta ese tipo con ese contenido
     */
    public abstract int write(AudioInputStream stream, AudioFileFormat.Type fileType, File out)
        throws IOException;
}
