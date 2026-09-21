package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Wraps an {@link InputStream} in an image input stream.
 *
 * <p>Package-private: it is not API. It chooses between caching on disk or in memory according to
 * what {@code ImageIO.getUseCache} says; see {@code ImageIO}.
 *
 * <p>It can use a cache file and does not need one: if not allowed to, it caches in memory.
 */
final class InputStreamImageInputStreamSpi extends ImageInputStreamSpi {

    InputStreamImageInputStreamSpi() {
        super("KajiJDK", "1.0", InputStream.class);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Service provider that instantiates an ImageInputStream from an InputStream";
    }

    /** Yes; with a file it caches on disk. */
    @Override
    public boolean canUseCacheFile() {
        return true;
    }

    /** No: without a file it caches in memory. */
    @Override
    public boolean needsCacheFile() {
        return false;
    }

    @Override
    public ImageInputStream createInputStreamInstance(Object input, boolean useCache,
                                                      File cacheDir) throws IOException {
        if (!(input instanceof InputStream)) {
            throw new IllegalArgumentException("input not an InputStream!");
        }
        if (useCache) {
            return new FileCacheImageInputStream((InputStream) input, cacheDir);
        }
        return new MemoryCacheImageInputStream((InputStream) input);
    }
}
