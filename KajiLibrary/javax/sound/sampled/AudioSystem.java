package javax.sound.sampled;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.AudioFileWriter;
import javax.sound.sampled.spi.FormatConversionProvider;
import javax.sound.sampled.spi.MixerProvider;

/**
 * KajiLibrary's javax.sound.sampled.AudioSystem -- el punto de entrada del audio muestreado.
 *
 * <p>Solo metodos estaticos. Todo lo que hace es <b>preguntarles a los proveedores</b> registrados
 * --mezcladores, lectores de archivo, escritores, conversores-- y quedarse con el primero que sepa
 * hacer lo que se pide. La clase en si no sabe nada de audio.
 *
 * <h2>Los cuatro tipos de proveedor</h2>
 *
 * <ul>
 *   <li>{@link MixerProvider} trae dispositivos;
 *   <li>{@link AudioFileReader} lee archivos;
 *   <li>{@link AudioFileWriter} los escribe;
 *   <li>{@link FormatConversionProvider} convierte de un formato a otro.
 * </ul>
 *
 * <p>Se encuentran con {@link ServiceLoader}. Es lo que permite agregar soporte para un formato nuevo
 * poniendo un jar en la ruta de clases, sin tocar codigo.
 *
 * <h2>Un proveedor roto no tumba la busqueda</h2>
 *
 * <p>Si uno falla al cargarse o al responder, se lo saltea y se sigue con los demas. Es la decision
 * correcta: un formato exotico mal implementado no puede impedir que se reproduzca un WAV.
 *
 * <h2>{@link #NOT_SPECIFIED}</h2>
 *
 * <p>Vale -1 y significa "no se sabe" o "cualquiera", segun donde aparezca. Es el comodin de todo el
 * paquete y conviene reconocerlo: un {@code getFrameLength()} de -1 no es un error, es un flujo sin
 * final conocido.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae ningun proveedor: hablar con una placa de sonido pide codigo nativo, y
 * decodificar WAV o AIFF pide los decodificadores. La busqueda esta implementada de verdad y todo
 * funciona sobre el conjunto vacio -- arreglos vacios donde corresponde,
 * {@link IllegalArgumentException} cuando se pide una linea que nadie provee, y
 * {@link UnsupportedAudioFileException} cuando nadie sabe leer un archivo. Es exactamente lo que hace
 * el JDK en una maquina sin dispositivos de audio.
 *
 * <p>Registrando proveedores como servicios, esto anda sin cambios.
 */
public class AudioSystem {

    /** El comodin del paquete. Ver la nota de la clase. */
    public static final int NOT_SPECIFIED = -1;

    /** No tiene estado; el constructor publico es el que el JDK dejo. */
    public AudioSystem() {
    }

    /** Los mezcladores que hay. */
    public static Mixer.Info[] getMixerInfo() {
        List<Mixer.Info> found = new ArrayList<Mixer.Info>();
        Iterator<MixerProvider> it = providers(MixerProvider.class);
        while (it.hasNext()) {
            Mixer.Info[] some = quietMixerInfo(it.next());
            int i = 0;
            while (some != null && i < some.length) {
                found.add(some[i]);
                i = i + 1;
            }
        }
        return found.toArray(new Mixer.Info[found.size()]);
    }

    /**
     * El mezclador de ese nombre.
     *
     * @param info cual, o null para el que el sistema prefiera
     * @throws IllegalArgumentException si no hay ninguno asi
     */
    public static Mixer getMixer(Mixer.Info info) {
        Iterator<MixerProvider> it = providers(MixerProvider.class);
        while (it.hasNext()) {
            MixerProvider p = it.next();
            try {
                if (p.isMixerSupported(info)) {
                    return p.getMixer(info);
                }
            } catch (Throwable e) {
                // Un proveedor roto no tumba la busqueda; ver la nota de la clase.
            }
        }
        throw new IllegalArgumentException("Mixer not supported: "
            + (info == null ? "null" : info.toString()));
    }

    /** Los descriptores de lineas de entrada al mezclador que coinciden con ese. */
    public static Line.Info[] getSourceLineInfo(Line.Info info) {
        List<Line.Info> found = new ArrayList<Line.Info>();
        Mixer.Info[] mixers = getMixerInfo();
        int i = 0;
        while (i < mixers.length) {
            collect(found, sourceInfoOf(mixers[i], info));
            i = i + 1;
        }
        return found.toArray(new Line.Info[found.size()]);
    }

    /** Idem, de salida. */
    public static Line.Info[] getTargetLineInfo(Line.Info info) {
        List<Line.Info> found = new ArrayList<Line.Info>();
        Mixer.Info[] mixers = getMixerInfo();
        int i = 0;
        while (i < mixers.length) {
            collect(found, targetInfoOf(mixers[i], info));
            i = i + 1;
        }
        return found.toArray(new Line.Info[found.size()]);
    }

    /** Si algun mezclador puede dar una linea asi. */
    public static boolean isLineSupported(Line.Info info) {
        Mixer.Info[] mixers = getMixerInfo();
        int i = 0;
        while (i < mixers.length) {
            try {
                if (getMixer(mixers[i]).isLineSupported(info)) {
                    return true;
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Una linea de ese tipo, sin abrir.
     *
     * @throws LineUnavailableException si el recurso esta ocupado
     * @throws IllegalArgumentException si ningun mezclador la provee
     */
    public static Line getLine(Line.Info info) throws LineUnavailableException {
        Mixer.Info[] mixers = getMixerInfo();
        int i = 0;
        while (i < mixers.length) {
            Mixer m = null;
            try {
                m = getMixer(mixers[i]);
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
            if (m != null && m.isLineSupported(info)) {
                return m.getLine(info);
            }
            i = i + 1;
        }
        throw new IllegalArgumentException("No line matching " + info + " is supported.");
    }

    /**
     * Un clip del mezclador por omision.
     *
     * @throws LineUnavailableException si no hay ninguno disponible
     */
    public static Clip getClip() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                             NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED,
                                             NOT_SPECIFIED, NOT_SPECIFIED, false);
        return (Clip) getLine(new DataLine.Info(Clip.class, format));
    }

    /**
     * Un clip de ese mezclador.
     *
     * @throws LineUnavailableException si no hay ninguno disponible
     */
    public static Clip getClip(Mixer.Info mixerInfo) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                             NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED,
                                             NOT_SPECIFIED, NOT_SPECIFIED, false);
        return (Clip) getMixer(mixerInfo).getLine(new DataLine.Info(Clip.class, format));
    }

    /**
     * Una linea de salida para ese formato.
     *
     * @throws LineUnavailableException si no hay ninguna disponible
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format)
        throws LineUnavailableException {
        return (SourceDataLine) getLine(new DataLine.Info(SourceDataLine.class, format));
    }

    /**
     * Idem, de ese mezclador.
     *
     * @throws LineUnavailableException si no hay ninguna disponible
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format, Mixer.Info mixerinfo)
        throws LineUnavailableException {
        return (SourceDataLine) getMixer(mixerinfo)
            .getLine(new DataLine.Info(SourceDataLine.class, format));
    }

    /**
     * Una linea de captura para ese formato.
     *
     * @throws LineUnavailableException si no hay ninguna disponible
     */
    public static TargetDataLine getTargetDataLine(AudioFormat format)
        throws LineUnavailableException {
        return (TargetDataLine) getLine(new DataLine.Info(TargetDataLine.class, format));
    }

    /**
     * Idem, de ese mezclador.
     *
     * @throws LineUnavailableException si no hay ninguna disponible
     */
    public static TargetDataLine getTargetDataLine(AudioFormat format, Mixer.Info mixerinfo)
        throws LineUnavailableException {
        return (TargetDataLine) getMixer(mixerinfo)
            .getLine(new DataLine.Info(TargetDataLine.class, format));
    }

    /** A que codificaciones se puede convertir desde esa. */
    public static AudioFormat.Encoding[] getTargetEncodings(AudioFormat.Encoding sourceEncoding) {
        List<AudioFormat.Encoding> found = new ArrayList<AudioFormat.Encoding>();
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            FormatConversionProvider p = it.next();
            try {
                if (p.isSourceEncodingSupported(sourceEncoding)) {
                    addAll(found, p.getTargetEncodings());
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return found.toArray(new AudioFormat.Encoding[found.size()]);
    }

    /** Idem, partiendo de un formato completo. */
    public static AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        List<AudioFormat.Encoding> found = new ArrayList<AudioFormat.Encoding>();
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            try {
                addAll(found, it.next().getTargetEncodings(sourceFormat));
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return found.toArray(new AudioFormat.Encoding[found.size()]);
    }

    /** Si alguien sabe convertir de ese formato a esa codificacion. */
    public static boolean isConversionSupported(AudioFormat.Encoding targetEncoding,
                                                AudioFormat sourceFormat) {
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            try {
                if (it.next().isConversionSupported(targetEncoding, sourceFormat)) {
                    return true;
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return false;
    }

    /**
     * Convierte ese flujo a esa codificacion.
     *
     * @throws IllegalArgumentException si nadie sabe hacer esa conversion
     */
    public static AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding,
                                                       AudioInputStream sourceStream) {
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            FormatConversionProvider p = it.next();
            try {
                if (p.isConversionSupported(targetEncoding, sourceStream.getFormat())) {
                    return p.getAudioInputStream(targetEncoding, sourceStream);
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        throw new IllegalArgumentException("Unsupported conversion: " + targetEncoding
            + " from " + sourceStream.getFormat());
    }

    /** Los formatos concretos a los que se puede convertir. */
    public static AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding,
                                                 AudioFormat sourceFormat) {
        List<AudioFormat> found = new ArrayList<AudioFormat>();
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            try {
                AudioFormat[] some = it.next().getTargetFormats(targetEncoding, sourceFormat);
                int i = 0;
                while (some != null && i < some.length) {
                    found.add(some[i]);
                    i = i + 1;
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return found.toArray(new AudioFormat[found.size()]);
    }

    /** Si alguien sabe convertir entre esos dos formatos. */
    public static boolean isConversionSupported(AudioFormat targetFormat,
                                                AudioFormat sourceFormat) {
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            try {
                if (it.next().isConversionSupported(targetFormat, sourceFormat)) {
                    return true;
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return false;
    }

    /**
     * Convierte ese flujo a ese formato.
     *
     * @throws IllegalArgumentException si nadie sabe hacer esa conversion
     */
    public static AudioInputStream getAudioInputStream(AudioFormat targetFormat,
                                                       AudioInputStream sourceStream) {
        if (sourceStream.getFormat().matches(targetFormat)) {
            return sourceStream;
        }
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            FormatConversionProvider p = it.next();
            try {
                if (p.isConversionSupported(targetFormat, sourceStream.getFormat())) {
                    return p.getAudioInputStream(targetFormat, sourceStream);
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        throw new IllegalArgumentException("Unsupported conversion: " + targetFormat
            + " from " + sourceStream.getFormat());
    }

    /**
     * Que hay en ese flujo.
     *
     * <p>El flujo tiene que soportar marcas: los lectores prueban de a uno y rebobinan.
     *
     * @throws UnsupportedAudioFileException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static AudioFileFormat getAudioFileFormat(InputStream stream)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioFileFormat(stream);
            } catch (UnsupportedAudioFileException e) {
                // Ese lector no lo reconoce; se prueba con el siguiente.
            }
        }
        throw new UnsupportedAudioFileException("file is not a supported file type");
    }

    /**
     * Idem, desde una direccion.
     *
     * @throws UnsupportedAudioFileException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static AudioFileFormat getAudioFileFormat(URL url)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioFileFormat(url);
            } catch (UnsupportedAudioFileException e) {
                // Ver arriba.
            }
        }
        throw new UnsupportedAudioFileException("file is not a supported file type");
    }

    /**
     * Idem, desde un archivo.
     *
     * @throws UnsupportedAudioFileException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static AudioFileFormat getAudioFileFormat(File file)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioFileFormat(file);
            } catch (UnsupportedAudioFileException e) {
                // Ver arriba.
            }
        }
        throw new UnsupportedAudioFileException("file is not a supported file type");
    }

    /**
     * Un flujo de audio desde ese flujo de bytes.
     *
     * @throws UnsupportedAudioFileException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static AudioInputStream getAudioInputStream(InputStream stream)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioInputStream(stream);
            } catch (UnsupportedAudioFileException e) {
                // Ver arriba.
            }
        }
        throw new UnsupportedAudioFileException("could not get audio input stream from input stream");
    }

    /**
     * Idem, desde una direccion.
     *
     * @throws UnsupportedAudioFileException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static AudioInputStream getAudioInputStream(URL url)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioInputStream(url);
            } catch (UnsupportedAudioFileException e) {
                // Ver arriba.
            }
        }
        throw new UnsupportedAudioFileException("could not get audio input stream from input URL");
    }

    /**
     * Idem, desde un archivo.
     *
     * @throws UnsupportedAudioFileException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static AudioInputStream getAudioInputStream(File file)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioInputStream(file);
            } catch (UnsupportedAudioFileException e) {
                // Ver arriba.
            }
        }
        throw new UnsupportedAudioFileException("could not get audio input stream from input file");
    }

    /** Que tipos de archivo se pueden escribir. */
    public static AudioFileFormat.Type[] getAudioFileTypes() {
        List<AudioFileFormat.Type> found = new ArrayList<AudioFileFormat.Type>();
        Iterator<AudioFileWriter> it = providers(AudioFileWriter.class);
        while (it.hasNext()) {
            try {
                AudioFileFormat.Type[] some = it.next().getAudioFileTypes();
                int i = 0;
                while (some != null && i < some.length) {
                    if (!found.contains(some[i])) {
                        found.add(some[i]);
                    }
                    i = i + 1;
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return found.toArray(new AudioFileFormat.Type[found.size()]);
    }

    /** Si ese tipo se puede escribir. */
    public static boolean isFileTypeSupported(AudioFileFormat.Type fileType) {
        AudioFileFormat.Type[] all = getAudioFileTypes();
        int i = 0;
        while (i < all.length) {
            if (all[i].equals(fileType)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** Que tipos se pueden escribir con ese contenido. */
    public static AudioFileFormat.Type[] getAudioFileTypes(AudioInputStream stream) {
        List<AudioFileFormat.Type> found = new ArrayList<AudioFileFormat.Type>();
        Iterator<AudioFileWriter> it = providers(AudioFileWriter.class);
        while (it.hasNext()) {
            try {
                AudioFileFormat.Type[] some = it.next().getAudioFileTypes(stream);
                int i = 0;
                while (some != null && i < some.length) {
                    if (!found.contains(some[i])) {
                        found.add(some[i]);
                    }
                    i = i + 1;
                }
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return found.toArray(new AudioFileFormat.Type[found.size()]);
    }

    /** Si ese tipo se puede escribir con ese contenido. */
    public static boolean isFileTypeSupported(AudioFileFormat.Type fileType,
                                              AudioInputStream stream) {
        AudioFileFormat.Type[] all = getAudioFileTypes(stream);
        int i = 0;
        while (i < all.length) {
            if (all[i].equals(fileType)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Escribe el flujo a ese destino con ese tipo de archivo.
     *
     * @return cuantos bytes se escribieron
     * @throws IOException si no se pudo escribir
     * @throws IllegalArgumentException si nadie sabe escribir ese tipo
     */
    public static int write(AudioInputStream stream, AudioFileFormat.Type fileType,
                            OutputStream out) throws IOException {
        Iterator<AudioFileWriter> it = providers(AudioFileWriter.class);
        while (it.hasNext()) {
            AudioFileWriter w = it.next();
            if (w.isFileTypeSupported(fileType, stream)) {
                return w.write(stream, fileType, out);
            }
        }
        throw new IllegalArgumentException("could not write audio file: file type not supported: "
            + fileType);
    }

    /**
     * Idem, a un archivo.
     *
     * @return cuantos bytes se escribieron
     * @throws IOException si no se pudo escribir
     * @throws IllegalArgumentException si nadie sabe escribir ese tipo
     */
    public static int write(AudioInputStream stream, AudioFileFormat.Type fileType, File out)
        throws IOException {
        Iterator<AudioFileWriter> it = providers(AudioFileWriter.class);
        while (it.hasNext()) {
            AudioFileWriter w = it.next();
            if (w.isFileTypeSupported(fileType, stream)) {
                return w.write(stream, fileType, out);
            }
        }
        throw new IllegalArgumentException("could not write audio file: file type not supported: "
            + fileType);
    }

    /**
     * Los proveedores de ese tipo, saltandose los que no cargan.
     *
     * <p>Se materializa la lista en lugar de devolver el iterador perezoso del {@link ServiceLoader}
     * para que un proveedor que falle al construirse no rompa el recorrido; ver la nota de la clase.
     */
    private static <T> Iterator<T> providers(Class<T> type) {
        List<T> all = new ArrayList<T>();
        try {
            Iterator<T> it = ServiceLoader.load(type).iterator();
            while (it.hasNext()) {
                try {
                    all.add(it.next());
                } catch (Throwable e) {
                    // Ese proveedor no carga; se sigue con los demas.
                }
            }
        } catch (Throwable e) {
            // Ni siquiera se pudo abrir el cargador de servicios.
        }
        return all.iterator();
    }

    /** Los descriptores de ese mezclador, o null si el mezclador falla. */
    private static Line.Info[] sourceInfoOf(Mixer.Info mixerInfo, Line.Info info) {
        try {
            return getMixer(mixerInfo).getSourceLineInfo(info);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Idem, de salida. */
    private static Line.Info[] targetInfoOf(Mixer.Info mixerInfo, Line.Info info) {
        try {
            return getMixer(mixerInfo).getTargetLineInfo(info);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Los descriptores de un mezclador, o nada si fallo. */
    private static void collect(List<Line.Info> into, Line.Info[] some) {
        int i = 0;
        while (some != null && i < some.length) {
            into.add(some[i]);
            i = i + 1;
        }
    }

    /** Los mezcladores de un proveedor, o null si falla. */
    private static Mixer.Info[] quietMixerInfo(MixerProvider p) {
        try {
            return p.getMixerInfo();
        } catch (Throwable e) {
            return null;
        }
    }

    /** Agrega los que no esten repetidos. */
    private static void addAll(List<AudioFormat.Encoding> into, AudioFormat.Encoding[] some) {
        int i = 0;
        while (some != null && i < some.length) {
            if (!into.contains(some[i])) {
                into.add(some[i]);
            }
            i = i + 1;
        }
    }
}
