package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * Wraps a {@link File} in an image output stream.
 *
 * <p>Package-private: it is not API. See {@link FileImageInputStreamSpi}.
 */
final class FileImageOutputStreamSpi extends ImageOutputStreamSpi {

    FileImageOutputStreamSpi() {
        super("KajiJDK", "1.0", File.class);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Service provider that instantiates a FileImageOutputStream from a File";
    }

    @Override
    public ImageOutputStream createOutputStreamInstance(Object output, boolean useCache,
                                                        File cacheDir) throws IOException {
        if (!(output instanceof File)) {
            throw new IllegalArgumentException("output not a File!");
        }
        return new FileImageOutputStream((File) output);
    }
}
