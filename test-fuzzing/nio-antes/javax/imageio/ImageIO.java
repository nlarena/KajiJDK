package javax.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ImageOutputStreamSpi;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageTranscoderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * KajiLibrary's javax.imageio.ImageIO -- reading and writing images in one line.
 *
 * <p>The facade of the whole package. Underneath it consults the {@link IIORegistry} and builds
 * whatever is needed; for the normal case --{@code ImageIO.read(new File("photo.png"))}-- that
 * stays invisible.
 *
 * <h2>{@link #read} returns null when nobody recognizes the format</h2>
 *
 * <p>It is what surprises most about this class: it does not throw. A file that is not an image,
 * or of a format with no registered reader, gives <b>null</b>; the exception is kept for real
 * input/output errors.
 *
 * <p>A program that does not check for null ends up with a {@code NullPointerException} far from
 * the place that caused it.
 *
 * <h2>{@link #write} returns false for the same reason</h2>
 *
 * <p>With no writer for that format name it returns false and <b>writes nothing</b>. Writing to a
 * {@code File}, this library first deletes the file and creates it again, so it is left there
 * empty -- worth deleting. The JDK looks for the writer first and, without one, does not touch
 * the file system. (An earlier note described the empty file as the normal behaviour.)
 *
 * <h2>The cache</h2>
 *
 * <p>{@link #setUseCache} decides whether the streams created over an {@code InputStream} may use a
 * temporary file instead of memory. By default <b>yes</b>, which is right for large images;
 * turning it off is what you do in an environment without a writable disk.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>The whole mechanism is implemented: the registry, lookup by name, suffix and MIME type,
 * stream creation, and the pairing between readers and writers. What this library does not ship
 * is <b>plug-ins</b>: decoding PNG or JPEG takes the codecs.
 *
 * <p>With that, {@link #read} returns null and {@link #write} returns false -- which is exactly
 * what the JDK does when nobody registered a reader for that format (apart from the empty file
 * above). The streams do really work: {@link #createImageInputStream} over a {@code File} or an
 * {@code InputStream} returns a usable stream, because those providers are written.
 */
public final class ImageIO {

    /** Whether streams may use a temporary file. See the class note. */
    private static boolean useCache = true;

    /** Where to put it, or null for the system's. */
    private static File cacheDirectory = null;

    /** Not instantiated. */
    private ImageIO() {
    }

    /**
     * Looks for plug-ins on the class path again.
     *
     * <p>Needed when they appear after startup; see
     * {@link IIORegistry#registerApplicationClasspathSpis}.
     */
    public static void scanForPlugins() {
        IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
    }

    /** Whether streams may use a temporary file. */
    public static void setUseCache(boolean useCache) {
        ImageIO.useCache = useCache;
    }

    /** Whether they may. */
    public static boolean getUseCache() {
        return useCache;
    }

    /**
     * Where to put the temporary files; null uses the system's.
     *
     * @throws IllegalArgumentException if it is not a directory
     */
    public static void setCacheDirectory(File cacheDirectory) {
        if (cacheDirectory != null && !cacheDirectory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory!");
        }
        ImageIO.cacheDirectory = cacheDirectory;
    }

    /** Where they go, or null. */
    public static File getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Wraps that in an image input stream.
     *
     * @return the stream, or null if nobody knows how to wrap that kind of object
     * @throws IllegalArgumentException if it is null
     * @throws IOException if it could not be created
     */
    public static ImageInputStream createImageInputStream(Object input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        Iterator<ImageInputStreamSpi> it =
            registry().getServiceProviders(ImageInputStreamSpi.class, true);
        while (it.hasNext()) {
            ImageInputStreamSpi spi = it.next();
            if (spi.getInputClass().isInstance(input)) {
                return spi.createInputStreamInstance(input, getUseCache(), getCacheDirectory());
            }
        }
        return null;
    }

    /**
     * Same, for output.
     *
     * @return the stream, or null if nobody knows how
     * @throws IllegalArgumentException if it is null
     * @throws IOException if it could not be created
     */
    public static ImageOutputStream createImageOutputStream(Object output) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        Iterator<ImageOutputStreamSpi> it =
            registry().getServiceProviders(ImageOutputStreamSpi.class, true);
        while (it.hasNext()) {
            ImageOutputStreamSpi spi = it.next();
            if (spi.getOutputClass().isInstance(output)) {
                return spi.createOutputStreamInstance(output, getUseCache(), getCacheDirectory());
            }
        }
        return null;
    }

    /** Which formats can be read, without repeats. */
    public static String[] getReaderFormatNames() {
        return collect(ImageReaderSpi.class, NAMES);
    }

    /** Which MIME types. */
    public static String[] getReaderMIMETypes() {
        return collect(ImageReaderSpi.class, MIME);
    }

    /** Which suffixes. */
    public static String[] getReaderFileSuffixes() {
        return collect(ImageReaderSpi.class, SUFFIXES);
    }

    /**
     * The readers that recognize what is in that input.
     *
     * <p>It asks each provider with {@code canDecodeInput}; see the rewind rule in
     * {@link ImageReaderSpi}.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static Iterator<ImageReader> getImageReaders(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        List<ImageReader> readers = new ArrayList<ImageReader>();
        Iterator<ImageReaderSpi> it =
            registry().getServiceProviders(ImageReaderSpi.class, true);
        while (it.hasNext()) {
            ImageReaderSpi spi = it.next();
            try {
                if (spi.canDecodeInput(input)) {
                    readers.add(spi.createReaderInstance());
                }
            } catch (IOException e) {
                // That provider could not look at the input; the others still can.
            }
        }
        return readers.iterator();
    }

    /**
     * The readers for that format.
     *
     * @throws IllegalArgumentException if the name is null
     */
    public static Iterator<ImageReader> getImageReadersByFormatName(String formatName) {
        return readersMatching(formatName, NAMES);
    }

    /**
     * The ones for that suffix.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static Iterator<ImageReader> getImageReadersBySuffix(String fileSuffix) {
        return readersMatching(fileSuffix, SUFFIXES);
    }

    /**
     * The ones for that MIME type.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static Iterator<ImageReader> getImageReadersByMIMEType(String MIMEType) {
        return readersMatching(MIMEType, MIME);
    }

    /** Which formats can be written. */
    public static String[] getWriterFormatNames() {
        return collect(ImageWriterSpi.class, NAMES);
    }

    /** Which MIME types. */
    public static String[] getWriterMIMETypes() {
        return collect(ImageWriterSpi.class, MIME);
    }

    /** Which suffixes. */
    public static String[] getWriterFileSuffixes() {
        return collect(ImageWriterSpi.class, SUFFIXES);
    }

    /**
     * The writers for that format.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static Iterator<ImageWriter> getImageWritersByFormatName(String formatName) {
        return writersMatching(formatName, NAMES);
    }

    /**
     * The ones for that suffix.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static Iterator<ImageWriter> getImageWritersBySuffix(String fileSuffix) {
        return writersMatching(fileSuffix, SUFFIXES);
    }

    /**
     * The ones for that MIME type.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static Iterator<ImageWriter> getImageWritersByMIMEType(String MIMEType) {
        return writersMatching(MIMEType, MIME);
    }

    /**
     * The writer for the same format as that reader, or null.
     *
     * <p>It is how you rewrite what you just read without changing format; see
     * {@link ImageReaderSpi#getImageWriterSpiNames}.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static ImageWriter getImageWriter(ImageReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader == null!");
        }
        ImageReaderSpi readerSpi = reader.getOriginatingProvider();
        if (readerSpi == null) {
            return null;
        }
        String[] writerNames = readerSpi.getImageWriterSpiNames();
        if (writerNames == null || writerNames.length == 0) {
            return null;
        }
        int i = 0;
        while (i < writerNames.length) {
            ImageWriterSpi spi = spiByName(writerNames[i], ImageWriterSpi.class);
            if (spi != null) {
                try {
                    return spi.createWriterInstance();
                } catch (IOException e) {
                    // That one could not be created; try the next sibling.
                }
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * The reader for the same format as that writer, or null.
     *
     * @throws IllegalArgumentException if it is null
     */
    public static ImageReader getImageReader(ImageWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer == null!");
        }
        ImageWriterSpi writerSpi = writer.getOriginatingProvider();
        if (writerSpi == null) {
            return null;
        }
        String[] readerNames = writerSpi.getImageReaderSpiNames();
        if (readerNames == null || readerNames.length == 0) {
            return null;
        }
        int i = 0;
        while (i < readerNames.length) {
            ImageReaderSpi spi = spiByName(readerNames[i], ImageReaderSpi.class);
            if (spi != null) {
                try {
                    return spi.createReaderInstance();
                } catch (IOException e) {
                    // See above.
                }
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * The writers that can write that image type in that format.
     *
     * <p>It crosses both conditions, which is what is needed before writing: that the format exists
     * and that it also supports that pixel type.
     *
     * @throws IllegalArgumentException if the name is null
     */
    public static Iterator<ImageWriter> getImageWriters(ImageTypeSpecifier type,
                                                        String formatName) {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        List<ImageWriter> writers = new ArrayList<ImageWriter>();
        Iterator<ImageWriterSpi> it =
            registry().getServiceProviders(ImageWriterSpi.class, true);
        while (it.hasNext()) {
            ImageWriterSpi spi = it.next();
            if (matches(spi, formatName, NAMES) && (type == null || spi.canEncodeImage(type))) {
                try {
                    writers.add(spi.createWriterInstance());
                } catch (IOException e) {
                    // See above.
                }
            }
        }
        return writers.iterator();
    }

    /**
     * The metadata transcoders between that reader and that writer.
     *
     * @throws IllegalArgumentException if either is null
     */
    public static Iterator<ImageTranscoder> getImageTranscoders(ImageReader reader,
                                                                ImageWriter writer) {
        if (reader == null) {
            throw new IllegalArgumentException("reader == null!");
        }
        if (writer == null) {
            throw new IllegalArgumentException("writer == null!");
        }
        List<ImageTranscoder> transcoders = new ArrayList<ImageTranscoder>();
        ImageReaderSpi readerSpi = reader.getOriginatingProvider();
        ImageWriterSpi writerSpi = writer.getOriginatingProvider();
        if (readerSpi == null || writerSpi == null) {
            return transcoders.iterator();
        }
        String readerName = readerSpi.getClass().getName();
        String writerName = writerSpi.getClass().getName();
        Iterator<ImageTranscoderSpi> it =
            registry().getServiceProviders(ImageTranscoderSpi.class, true);
        while (it.hasNext()) {
            ImageTranscoderSpi spi = it.next();
            if (readerName.equals(spi.getReaderServiceProviderName())
                && writerName.equals(spi.getWriterServiceProviderName())) {
                transcoders.add(spi.createTranscoderInstance());
            }
        }
        return transcoders.iterator();
    }

    /**
     * Reads the first image of that file.
     *
     * @return the image, or null if nobody recognizes the format. See the class note
     * @throws IllegalArgumentException if it is null
     * @throws IOException if reading failed
     */
    public static BufferedImage read(File input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        if (!input.canRead()) {
            throw new IIOException("Can't read input file!");
        }
        ImageInputStream stream = createImageInputStream(input);
        if (stream == null) {
            throw new IIOException("Can't create an ImageInputStream!");
        }
        BufferedImage bi = read(stream);
        if (bi == null) {
            stream.close();
        }
        return bi;
    }

    /**
     * Same, from a stream. The stream is <b>not</b> closed.
     *
     * @return the image, or null
     * @throws IOException if reading failed
     */
    public static BufferedImage read(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        ImageInputStream stream = createImageInputStream(input);
        BufferedImage bi = read(stream);
        if (bi == null && stream != null) {
            stream.close();
        }
        return bi;
    }

    /**
     * Same, from an address.
     *
     * @return the image, or null
     * @throws IOException if reading failed
     */
    public static BufferedImage read(URL input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        InputStream istream = input.openStream();
        ImageInputStream stream = createImageInputStream(istream);
        BufferedImage bi;
        try {
            bi = read(stream);
            if (bi == null && stream != null) {
                stream.close();
            }
        } finally {
            // This method opened the connection, so this method closes it -- unlike the stream the
            // InputStream version receives.
            istream.close();
        }
        return bi;
    }

    /**
     * Same, from an already built image stream.
     *
     * @return the image, or null if nobody recognizes the format
     * @throws IOException if reading failed
     */
    public static BufferedImage read(ImageInputStream stream) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        Iterator<ImageReader> iter = getImageReaders(stream);
        if (!iter.hasNext()) {
            return null;
        }
        ImageReader reader = iter.next();
        ImageReadParam param = reader.getDefaultReadParam();
        reader.setInput(stream, true, true);
        BufferedImage bi;
        try {
            bi = reader.read(0, param);
        } finally {
            reader.dispose();
            stream.close();
        }
        return bi;
    }

    /**
     * Writes that image in that format.
     *
     * @return whether it was written; false if there is no writer for that format. See the class
     *     note
     * @throws IllegalArgumentException if any argument is null
     * @throws IOException if writing failed
     */
    public static boolean write(RenderedImage im, String formatName, ImageOutputStream output)
        throws IOException {
        if (im == null) {
            throw new IllegalArgumentException("im == null!");
        }
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        ImageWriter writer = firstWriter(im, formatName);
        if (writer == null) {
            return false;
        }
        writer.setOutput(output);
        try {
            writer.write(new IIOImage(im, null, null));
        } finally {
            writer.dispose();
            output.flush();
        }
        return true;
    }

    /**
     * Same, to a file.
     *
     * <p>If there is no writer it returns false and the file is left created and <b>empty</b>,
     * unlike the JDK; see the class note.
     *
     * @throws IOException if writing failed
     */
    public static boolean write(RenderedImage im, String formatName, File output)
        throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        output.delete();
        ImageOutputStream stream = createImageOutputStream(output);
        if (stream == null) {
            throw new IIOException("Can't create an ImageOutputStream!");
        }
        try {
            return write(im, formatName, stream);
        } finally {
            stream.close();
        }
    }

    /**
     * Same, to a stream. The stream is <b>not</b> closed.
     *
     * @throws IOException if writing failed
     */
    public static boolean write(RenderedImage im, String formatName, OutputStream output)
        throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        ImageOutputStream stream = createImageOutputStream(output);
        if (stream == null) {
            throw new IIOException("Can't create an ImageOutputStream!");
        }
        try {
            return write(im, formatName, stream);
        } finally {
            stream.close();
        }
    }

    /** The format names. */
    private static final int NAMES = 0;

    /** The suffixes. */
    private static final int SUFFIXES = 1;

    /** The MIME types. */
    private static final int MIME = 2;

    /** The registry consulted for everything. */
    private static IIORegistry registry() {
        return IIORegistry.getDefaultInstance();
    }

    /** The names, suffixes or MIME types of all the providers of that category. */
    private static <T extends ImageReaderWriterSpi> String[] collect(Class<T> category, int which) {
        List<String> found = new ArrayList<String>();
        Iterator<T> it = registry().getServiceProviders(category, true);
        while (it.hasNext()) {
            String[] some = valuesOf(it.next(), which);
            int i = 0;
            while (some != null && i < some.length) {
                if (!found.contains(some[i])) {
                    found.add(some[i]);
                }
                i = i + 1;
            }
        }
        return found.toArray(new String[found.size()]);
    }

    /** The readers whose provider has that name, suffix or MIME type. */
    private static Iterator<ImageReader> readersMatching(String value, int which) {
        if (value == null) {
            throw new IllegalArgumentException("argument == null!");
        }
        List<ImageReader> readers = new ArrayList<ImageReader>();
        Iterator<ImageReaderSpi> it =
            registry().getServiceProviders(ImageReaderSpi.class, true);
        while (it.hasNext()) {
            ImageReaderSpi spi = it.next();
            if (matches(spi, value, which)) {
                try {
                    readers.add(spi.createReaderInstance());
                } catch (IOException e) {
                    // That one could not be created; the others are still good.
                }
            }
        }
        return readers.iterator();
    }

    /** Same, for writers. */
    private static Iterator<ImageWriter> writersMatching(String value, int which) {
        if (value == null) {
            throw new IllegalArgumentException("argument == null!");
        }
        List<ImageWriter> writers = new ArrayList<ImageWriter>();
        Iterator<ImageWriterSpi> it =
            registry().getServiceProviders(ImageWriterSpi.class, true);
        while (it.hasNext()) {
            ImageWriterSpi spi = it.next();
            if (matches(spi, value, which)) {
                try {
                    writers.add(spi.createWriterInstance());
                } catch (IOException e) {
                    // See above.
                }
            }
        }
        return writers.iterator();
    }

    /** The first writer that works for that image and that format, or null. */
    private static ImageWriter firstWriter(RenderedImage im, String formatName) {
        Iterator<ImageWriter> iter = getImageWriters(new ImageTypeSpecifier(im), formatName);
        if (!iter.hasNext()) {
            return null;
        }
        return iter.next();
    }

    /** Whether that provider declares that name, suffix or MIME type. */
    private static boolean matches(ImageReaderWriterSpi spi, String value, int which) {
        String[] values = valuesOf(spi, which);
        int i = 0;
        while (values != null && i < values.length) {
            if (value.equalsIgnoreCase(values[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** The array that corresponds to that kind of name. */
    private static String[] valuesOf(ImageReaderWriterSpi spi, int which) {
        if (which == SUFFIXES) {
            return spi.getFileSuffixes();
        }
        if (which == MIME) {
            return spi.getMIMETypes();
        }
        return spi.getFormatNames();
    }

    /** The provider of that class, or null if it is not registered or could not be loaded. */
    private static <T> T spiByName(String className, Class<T> category) {
        try {
            Class<?> cls = Class.forName(className, true,
                                         ImageIO.class.getClassLoader());
            Object spi = registry().getServiceProviderByClass(cls);
            if (category.isInstance(spi)) {
                return category.cast(spi);
            }
        } catch (Throwable e) {
            // That class is not there; the declared sibling is not installed.
        }
        return null;
    }
}
