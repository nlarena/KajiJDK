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
 * KajiLibrary's javax.sound.sampled.AudioSystem -- the entry point to sampled audio.
 *
 * <p>Only static methods. All it does is <b>ask the registered providers</b> --mixers, file
 * readers, writers, converters-- and keep the first one that knows how to do what is asked. The
 * class itself knows nothing about audio.
 *
 * <h2>The four kinds of provider</h2>
 *
 * <ul>
 *   <li>{@link MixerProvider} provides devices;
 *   <li>{@link AudioFileReader} reads files;
 *   <li>{@link AudioFileWriter} writes them;
 *   <li>{@link FormatConversionProvider} converts from one format to another.
 * </ul>
 *
 * <p>They are found with {@link ServiceLoader}. It is what allows adding support for a new format
 * by putting a jar on the class path, without touching code.
 *
 * <h2>A broken provider does not bring the search down</h2>
 *
 * <p>If one fails to load or to answer, it is skipped and the others are tried. It is the right
 * decision: a badly implemented exotic format cannot stop a WAV from playing.
 *
 * <h2>{@link #NOT_SPECIFIED}</h2>
 *
 * <p>It is -1 and means "not known" or "any", depending on where it appears. It is the wildcard of
 * the whole package and it is worth recognizing: a {@code getFrameLength()} of -1 is not an error,
 * it is a stream with no known end.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library comes with no provider: talking to a sound card needs native code, and decoding
 * WAV or AIFF needs the decoders. The search is really implemented and everything works over the
 * empty set -- empty arrays where they belong, {@link IllegalArgumentException} when a line nobody
 * provides is asked for, and {@link UnsupportedAudioFileException} when nobody knows how to read a
 * file. It is exactly what the JDK does on a machine without audio devices.
 *
 * <p>Registering providers as services, this works unchanged.
 */
public class AudioSystem {

    /** The wildcard of the package. See the class note. */
    public static final int NOT_SPECIFIED = -1;

    /** It has no state; the public constructor is the one the JDK left. */
    public AudioSystem() {
    }

    /** The mixers there are. */
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
     * The mixer with that name.
     *
     * @param info which one, or null for the one the system prefers
     * @throws IllegalArgumentException if there is none like that
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
                // A broken provider does not bring the search down; see the class note.
            }
        }
        throw new IllegalArgumentException("Mixer not supported: "
            + (info == null ? "null" : info.toString()));
    }

    /** The descriptors of lines into the mixer that match that one. */
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

    /** Likewise, out of it. */
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

    /** Whether some mixer can give a line like that. */
    public static boolean isLineSupported(Line.Info info) {
        Mixer.Info[] mixers = getMixerInfo();
        int i = 0;
        while (i < mixers.length) {
            try {
                if (getMixer(mixers[i]).isLineSupported(info)) {
                    return true;
                }
            } catch (Throwable e) {
                // See the class note.
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * A line of that type, not opened.
     *
     * @throws LineUnavailableException if the resource is busy
     * @throws IllegalArgumentException if no mixer provides it
     */
    public static Line getLine(Line.Info info) throws LineUnavailableException {
        Mixer.Info[] mixers = getMixerInfo();
        int i = 0;
        while (i < mixers.length) {
            Mixer m = null;
            try {
                m = getMixer(mixers[i]);
            } catch (Throwable e) {
                // See the class note.
            }
            if (m != null && m.isLineSupported(info)) {
                return m.getLine(info);
            }
            i = i + 1;
        }
        throw new IllegalArgumentException("No line matching " + info + " is supported.");
    }

    /**
     * A clip of the default mixer.
     *
     * @throws LineUnavailableException if there is none available
     */
    public static Clip getClip() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                             NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED,
                                             NOT_SPECIFIED, NOT_SPECIFIED, false);
        return (Clip) getLine(new DataLine.Info(Clip.class, format));
    }

    /**
     * A clip of that mixer.
     *
     * @throws LineUnavailableException if there is none available
     */
    public static Clip getClip(Mixer.Info mixerInfo) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                             NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED,
                                             NOT_SPECIFIED, NOT_SPECIFIED, false);
        return (Clip) getMixer(mixerInfo).getLine(new DataLine.Info(Clip.class, format));
    }

    /**
     * An output line for that format.
     *
     * @throws LineUnavailableException if there is none available
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format)
        throws LineUnavailableException {
        return (SourceDataLine) getLine(new DataLine.Info(SourceDataLine.class, format));
    }

    /**
     * Likewise, of that mixer.
     *
     * @throws LineUnavailableException if there is none available
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format, Mixer.Info mixerinfo)
        throws LineUnavailableException {
        return (SourceDataLine) getMixer(mixerinfo)
            .getLine(new DataLine.Info(SourceDataLine.class, format));
    }

    /**
     * A capture line for that format.
     *
     * @throws LineUnavailableException if there is none available
     */
    public static TargetDataLine getTargetDataLine(AudioFormat format)
        throws LineUnavailableException {
        return (TargetDataLine) getLine(new DataLine.Info(TargetDataLine.class, format));
    }

    /**
     * Likewise, of that mixer.
     *
     * @throws LineUnavailableException if there is none available
     */
    public static TargetDataLine getTargetDataLine(AudioFormat format, Mixer.Info mixerinfo)
        throws LineUnavailableException {
        return (TargetDataLine) getMixer(mixerinfo)
            .getLine(new DataLine.Info(TargetDataLine.class, format));
    }

    /** Which encodings that one can be converted to. */
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
                // See the class note.
            }
        }
        return found.toArray(new AudioFormat.Encoding[found.size()]);
    }

    /** Likewise, starting from a complete format. */
    public static AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        List<AudioFormat.Encoding> found = new ArrayList<AudioFormat.Encoding>();
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            try {
                addAll(found, it.next().getTargetEncodings(sourceFormat));
            } catch (Throwable e) {
                // See the class note.
            }
        }
        return found.toArray(new AudioFormat.Encoding[found.size()]);
    }

    /** Whether somebody knows how to convert from that format to that encoding. */
    public static boolean isConversionSupported(AudioFormat.Encoding targetEncoding,
                                                AudioFormat sourceFormat) {
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            try {
                if (it.next().isConversionSupported(targetEncoding, sourceFormat)) {
                    return true;
                }
            } catch (Throwable e) {
                // See the class note.
            }
        }
        return false;
    }

    /**
     * Converts that stream to that encoding.
     *
     * @throws IllegalArgumentException if nobody knows how to do that conversion
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
                // See the class note.
            }
        }
        throw new IllegalArgumentException("Unsupported conversion: " + targetEncoding
            + " from " + sourceStream.getFormat());
    }

    /** The concrete formats it can be converted to. */
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
                // See the class note.
            }
        }
        return found.toArray(new AudioFormat[found.size()]);
    }

    /** Whether somebody knows how to convert between those two formats. */
    public static boolean isConversionSupported(AudioFormat targetFormat,
                                                AudioFormat sourceFormat) {
        Iterator<FormatConversionProvider> it = providers(FormatConversionProvider.class);
        while (it.hasNext()) {
            try {
                if (it.next().isConversionSupported(targetFormat, sourceFormat)) {
                    return true;
                }
            } catch (Throwable e) {
                // See the class note.
            }
        }
        return false;
    }

    /**
     * Converts that stream to that format.
     *
     * @throws IllegalArgumentException if nobody knows how to do that conversion
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
                // See the class note.
            }
        }
        throw new IllegalArgumentException("Unsupported conversion: " + targetFormat
            + " from " + sourceStream.getFormat());
    }

    /**
     * What there is in that stream.
     *
     * <p>The stream has to support marks: the readers try one at a time and rewind.
     *
     * @throws UnsupportedAudioFileException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static AudioFileFormat getAudioFileFormat(InputStream stream)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioFileFormat(stream);
            } catch (UnsupportedAudioFileException e) {
                // That reader does not recognize it; try the next one.
            }
        }
        throw new UnsupportedAudioFileException("file is not a supported file type");
    }

    /**
     * Likewise, from a URL.
     *
     * @throws UnsupportedAudioFileException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static AudioFileFormat getAudioFileFormat(URL url)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioFileFormat(url);
            } catch (UnsupportedAudioFileException e) {
                // See above.
            }
        }
        throw new UnsupportedAudioFileException("file is not a supported file type");
    }

    /**
     * Likewise, from a file.
     *
     * @throws UnsupportedAudioFileException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static AudioFileFormat getAudioFileFormat(File file)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioFileFormat(file);
            } catch (UnsupportedAudioFileException e) {
                // See above.
            }
        }
        throw new UnsupportedAudioFileException("file is not a supported file type");
    }

    /**
     * An audio stream from that byte stream.
     *
     * @throws UnsupportedAudioFileException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static AudioInputStream getAudioInputStream(InputStream stream)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioInputStream(stream);
            } catch (UnsupportedAudioFileException e) {
                // See above.
            }
        }
        throw new UnsupportedAudioFileException("could not get audio input stream from input stream");
    }

    /**
     * Likewise, from a URL.
     *
     * @throws UnsupportedAudioFileException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static AudioInputStream getAudioInputStream(URL url)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioInputStream(url);
            } catch (UnsupportedAudioFileException e) {
                // See above.
            }
        }
        throw new UnsupportedAudioFileException("could not get audio input stream from input URL");
    }

    /**
     * Likewise, from a file.
     *
     * @throws UnsupportedAudioFileException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static AudioInputStream getAudioInputStream(File file)
        throws UnsupportedAudioFileException, IOException {
        Iterator<AudioFileReader> it = providers(AudioFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getAudioInputStream(file);
            } catch (UnsupportedAudioFileException e) {
                // See above.
            }
        }
        throw new UnsupportedAudioFileException("could not get audio input stream from input file");
    }

    /** Which file types can be written. */
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
                // See the class note.
            }
        }
        return found.toArray(new AudioFileFormat.Type[found.size()]);
    }

    /** Whether that type can be written. */
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

    /** Which types can be written with that content. */
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
                // See the class note.
            }
        }
        return found.toArray(new AudioFileFormat.Type[found.size()]);
    }

    /** Whether that type can be written with that content. */
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
     * Writes the stream to that destination with that file type.
     *
     * @return how many bytes were written
     * @throws IOException if it could not be written
     * @throws IllegalArgumentException if nobody knows how to write that type
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
     * Likewise, to a file.
     *
     * @return how many bytes were written
     * @throws IOException if it could not be written
     * @throws IllegalArgumentException if nobody knows how to write that type
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
     * The providers of that kind, skipping the ones that do not load.
     *
     * <p>The list is materialized instead of returning the {@link ServiceLoader}'s lazy iterator so
     * that a provider that fails to construct does not break the walk; see the class note.
     */
    private static <T> Iterator<T> providers(Class<T> type) {
        List<T> all = new ArrayList<T>();
        try {
            Iterator<T> it = ServiceLoader.load(type).iterator();
            while (it.hasNext()) {
                try {
                    all.add(it.next());
                } catch (Throwable e) {
                    // That provider does not load; carry on with the others.
                }
            }
        } catch (Throwable e) {
            // Not even the service loader could be opened.
        }
        return all.iterator();
    }

    /** That mixer's descriptors, or null if the mixer fails. */
    private static Line.Info[] sourceInfoOf(Mixer.Info mixerInfo, Line.Info info) {
        try {
            return getMixer(mixerInfo).getSourceLineInfo(info);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Likewise, output. */
    private static Line.Info[] targetInfoOf(Mixer.Info mixerInfo, Line.Info info) {
        try {
            return getMixer(mixerInfo).getTargetLineInfo(info);
        } catch (Throwable e) {
            return null;
        }
    }

    /** A mixer's descriptors, or nothing if it failed. */
    private static void collect(List<Line.Info> into, Line.Info[] some) {
        int i = 0;
        while (some != null && i < some.length) {
            into.add(some[i]);
            i = i + 1;
        }
    }

    /** A provider's mixers, or null if it fails. */
    private static Mixer.Info[] quietMixerInfo(MixerProvider p) {
        try {
            return p.getMixerInfo();
        } catch (Throwable e) {
            return null;
        }
    }

    /** Adds the ones that are not repeated. */
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
