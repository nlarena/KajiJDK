package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * Envuelve un {@link InputStream} en un flujo de entrada de imagenes.
 *
 * <p>De acceso de paquete: no es API. Elige entre cachear en disco o en memoria segun lo que
 * {@code ImageIO.getUseCache} diga; ver {@code ImageIO}.
 *
 * <p>Puede usar archivo de cache y no lo necesita: si no se le permite, cachea en memoria.
 */
final class InputStreamImageInputStreamSpi extends ImageInputStreamSpi {

    InputStreamImageInputStreamSpi() {
        super("KajiJDK", "1.0", InputStream.class);
    }

    @Override
    public String getDescription(Locale locale) {
        return "Service provider that instantiates an ImageInputStream from an InputStream";
    }

    /** Si; con archivo cachea en disco. */
    @Override
    public boolean canUseCacheFile() {
        return true;
    }

    /** No: sin archivo cachea en memoria. */
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
