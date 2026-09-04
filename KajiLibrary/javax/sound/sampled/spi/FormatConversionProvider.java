package javax.sound.sampled.spi;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * KajiLibrary's javax.sound.sampled.spi.FormatConversionProvider -- convierte audio de un formato a
 * otro.
 *
 * <p>Decodificar mu-law a PCM, cambiar la frecuencia de muestreo, pasar de estereo a mono. Se registra
 * como servicio y {@code AudioSystem} lo encuentra solo.
 *
 * <h2>La conversion es perezosa</h2>
 *
 * <p>{@code getAudioInputStream} devuelve un flujo que convierte <b>a medida que se lee</b>, no un
 * bufer ya convertido. Eso es lo que permite convertir un archivo de una hora sin cargarlo en memoria,
 * y lo que permite encadenar conversores.
 *
 * <h2>Codificacion contra formato completo</h2>
 *
 * <p>Los metodos vienen de a pares: uno toma una {@link AudioFormat.Encoding} y otro un
 * {@link AudioFormat} entero. El primero dice "pasalo a PCM, elegi vos el resto"; el segundo dice
 * exactamente a que. El primero es el util cuando solo hace falta descomprimir.
 *
 * <p>Los cuatro metodos de consulta vienen implementados sobre los abstractos; una subclase solo
 * necesita los cinco abstractos.
 */
public abstract class FormatConversionProvider {

    /** Para las subclases. */
    protected FormatConversionProvider() {
    }

    /** De que codificaciones sabe partir. */
    public abstract AudioFormat.Encoding[] getSourceEncodings();

    /** A cuales sabe llegar. */
    public abstract AudioFormat.Encoding[] getTargetEncodings();

    /** Si sabe partir de esa. */
    public boolean isSourceEncodingSupported(AudioFormat.Encoding sourceEncoding) {
        return contains(getSourceEncodings(), sourceEncoding);
    }

    /** Si sabe llegar a esa. */
    public boolean isTargetEncodingSupported(AudioFormat.Encoding targetEncoding) {
        return contains(getTargetEncodings(), targetEncoding);
    }

    /** A que codificaciones puede llevar ese formato concreto. */
    public abstract AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat);

    /** Si puede llevar ese formato a esa codificacion. */
    public boolean isConversionSupported(AudioFormat.Encoding targetEncoding,
                                         AudioFormat sourceFormat) {
        return contains(getTargetEncodings(sourceFormat), targetEncoding);
    }

    /** Los formatos concretos a los que puede llevarlo. */
    public abstract AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding,
                                                   AudioFormat sourceFormat);

    /** Si puede convertir entre esos dos formatos. */
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        AudioFormat[] formats = getTargetFormats(targetFormat.getEncoding(), sourceFormat);
        int i = 0;
        while (formats != null && i < formats.length) {
            if (targetFormat.matches(formats[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Un flujo que convierte a esa codificacion mientras se lee.
     *
     * @throws IllegalArgumentException si no soporta esa conversion
     */
    public abstract AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding,
                                                         AudioInputStream sourceStream);

    /**
     * Idem, a un formato concreto.
     *
     * @throws IllegalArgumentException si no soporta esa conversion
     */
    public abstract AudioInputStream getAudioInputStream(AudioFormat targetFormat,
                                                         AudioInputStream sourceStream);

    /** Si esa codificacion esta en el arreglo. */
    private static boolean contains(AudioFormat.Encoding[] all, AudioFormat.Encoding one) {
        int i = 0;
        while (all != null && i < all.length) {
            if (all[i].equals(one)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
