package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import javax.imageio.stream.ImageInputStream;

/**
 * KajiLibrary's javax.imageio.spi.ImageInputStreamSpi -- the provider of an image input stream.
 *
 * <p>It knows how to wrap objects of <b>one</b> class --{@link #getInputClass}: a {@code File}, an
 * {@code InputStream}, a {@code URL}-- in an {@link ImageInputStream}. It is what
 * {@code ImageIO.createImageInputStream} consults.
 *
 * <h2>The cache</h2>
 *
 * <p>{@link #canUseCacheFile} and {@link #needsCacheFile} are different, and the difference decides
 * whether the {@code cacheDir} argument is of any use:
 *
 * <ul>
 *   <li><b>can</b>: it knows how to work with a cache file and also without one. An
 *       {@code InputStream} falls here -- it can be cached on disk or in memory;
 *   <li><b>needs</b>: it does not work without a file. It implies can.
 * </ul>
 *
 * <p>A provider over {@code File} needs neither: the file already seeks on its own.
 */
public abstract class ImageInputStreamSpi extends IIOServiceProvider {

    /** Which class of object it knows how to wrap. */
    protected Class<?> inputClass;

    /** The one the service loader requires. */
    protected ImageInputStreamSpi() {
    }

    /**
     * @param inputClass which class it knows how to wrap
     * @throws IllegalArgumentException if it is null
     */
    public ImageInputStreamSpi(String vendorName, String version, Class<?> inputClass) {
        super(vendorName, version);
        if (inputClass == null) {
            throw new IllegalArgumentException("inputClass == null!");
        }
        this.inputClass = inputClass;
    }

    /** Which class it knows how to wrap. */
    public Class<?> getInputClass() {
        return this.inputClass;
    }

    /** Whether it can use a cache file. See the class note. */
    public boolean canUseCacheFile() {
        return false;
    }

    /** Whether it needs one. See the class note: it implies it can. */
    public boolean needsCacheFile() {
        return false;
    }

    /**
     * Wraps that object.
     *
     * @param useCache whether to use a cache file; ignored if not supported
     * @param cacheDir where to put it, or null for the system's
     * @throws IllegalArgumentException if the object is not of the expected class, or if the
     *     directory is not one
     * @throws IOException if it could not be created
     */
    public abstract ImageInputStream createInputStreamInstance(Object input, boolean useCache,
                                                               File cacheDir) throws IOException;

    /**
     * Same, with a cache if the provider needs one and without it if not.
     *
     * @throws IOException if it could not be created
     */
    public ImageInputStream createInputStreamInstance(Object input) throws IOException {
        return createInputStreamInstance(input, true, null);
    }
}
