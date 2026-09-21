package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

/**
 * Wraps a {@link File} in an image input stream.
 *
 * <p>Package-private: it is not API. It is one of the four stream providers {@link IIORegistry}
 * registers by default, and the one that makes {@code ImageIO.createImageInputStream(new
 * File(...))} work without anybody installing anything.
 *
 * <p>It uses no cache and needs none: a file can already seek.
 */
final class FileImageInputStreamSpi extends ImageInputStreamSpi {

    FileImageInputStreamSpi() {
        super("KajiJDK", "1.0", File.class);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Service provider that instantiates a FileImageInputStream from a File";
    }

    @Override
    public ImageInputStream createInputStreamInstance(Object input, boolean useCache,
                                                      File cacheDir) throws IOException {
        if (!(input instanceof File)) {
            throw new IllegalArgumentException("input not a File!");
        }
        return new FileImageInputStream((File) input);
    }
}
