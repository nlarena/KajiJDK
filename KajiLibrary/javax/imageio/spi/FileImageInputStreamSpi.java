package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

/**
 * Envuelve un {@link File} en un flujo de entrada de imagenes.
 *
 * <p>De acceso de paquete: no es API. Es uno de los cuatro proveedores de flujo que
 * {@link IIORegistry} registra de fabrica, y el que hace que
 * {@code ImageIO.createImageInputStream(new File(...))} funcione sin que nadie instale nada.
 *
 * <p>No usa cache ni la necesita: un archivo ya se puede posicionar.
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
