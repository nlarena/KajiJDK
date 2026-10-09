package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import javax.imageio.stream.ImageOutputStream;

/**
 * KajiLibrary's javax.imageio.spi.ImageOutputStreamSpi -- the provider of an image output stream.
 *
 * <p>The mirror of {@link ImageInputStreamSpi}, with the same rules about the cache -- and there
 * the cache weighs more: a format that needs to go back and fix its header cannot be written to an
 * {@code OutputStream} without collecting it somewhere.
 */
public abstract class ImageOutputStreamSpi extends IIOServiceProvider {

    /** Which class of object it knows how to wrap. */
    protected Class<?> outputClass;

    /** The one the service loader requires. */
    protected ImageOutputStreamSpi() {
    }

    /**
     * @throws IllegalArgumentException if the class is null
     */
    public ImageOutputStreamSpi(String vendorName, String version, Class<?> outputClass) {
        super(vendorName, version);
        if (outputClass == null) {
            throw new IllegalArgumentException("outputClass == null!");
        }
        this.outputClass = outputClass;
    }

    /** Which class it knows how to wrap. */
    public Class<?> getOutputClass() {
        return this.outputClass;
    }

    /** Whether it can use a cache file. See {@link ImageInputStreamSpi}. */
    public boolean canUseCacheFile() {
        return false;
    }

    /** Whether it needs one. */
    public boolean needsCacheFile() {
        return false;
    }

    /**
     * Wraps that object.
     *
     * @throws IllegalArgumentException if it is not of the expected class
     * @throws IOException if it could not be created
     */
    public abstract ImageOutputStream createOutputStreamInstance(Object output, boolean useCache,
                                                                 File cacheDir)
        throws IOException;

    /**
     * Same, with a cache if needed.
     *
     * @throws IOException if it could not be created
     */
    public ImageOutputStream createOutputStreamInstance(Object output) throws IOException {
        return createOutputStreamInstance(output, true, null);
    }
}
