package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Wraps an {@link OutputStream} in an image output stream.
 *
 * <p>Package-private: it is not API. See {@link InputStreamImageInputStreamSpi}.
 */
final class OutputStreamImageOutputStreamSpi extends ImageOutputStreamSpi {

    OutputStreamImageOutputStreamSpi() {
        super("KajiJDK", "1.0", OutputStream.class);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Service provider that instantiates an ImageOutputStream from an OutputStream";
    }

    /** Yes. */
    @Override
    public boolean canUseCacheFile() {
        return true;
    }

    /** No. */
    @Override
    public boolean needsCacheFile() {
        return false;
    }

    @Override
    public ImageOutputStream createOutputStreamInstance(Object output, boolean useCache,
                                                        File cacheDir) throws IOException {
        if (!(output instanceof OutputStream)) {
            throw new IllegalArgumentException("output not an OutputStream!");
        }
        if (useCache) {
            return new FileCacheImageOutputStream((OutputStream) output, cacheDir);
        }
        return new MemoryCacheImageOutputStream((OutputStream) output);
    }
}
