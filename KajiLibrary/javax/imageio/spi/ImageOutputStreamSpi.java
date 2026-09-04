package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import javax.imageio.stream.ImageOutputStream;

/**
 * KajiLibrary's javax.imageio.spi.ImageOutputStreamSpi -- el proveedor de un flujo de salida de
 * imagenes.
 *
 * <p>El espejo de {@link ImageInputStreamSpi}, con las mismas reglas sobre la cache -- y ahi la cache
 * pesa mas: un formato que necesita volver a corregir su encabezado no se puede escribir a un
 * {@code OutputStream} sin juntar en algun lado.
 */
public abstract class ImageOutputStreamSpi extends IIOServiceProvider {

    /** Que clase de objeto sabe envolver. */
    protected Class<?> outputClass;

    /** El que exige el cargador de servicios. */
    protected ImageOutputStreamSpi() {
    }

    /**
     * @throws IllegalArgumentException si la clase es null
     */
    public ImageOutputStreamSpi(String vendorName, String version, Class<?> outputClass) {
        super(vendorName, version);
        if (outputClass == null) {
            throw new IllegalArgumentException("outputClass == null!");
        }
        this.outputClass = outputClass;
    }

    /** Que clase sabe envolver. */
    public Class<?> getOutputClass() {
        return this.outputClass;
    }

    /** Si sabe usar un archivo de cache. Ver {@link ImageInputStreamSpi}. */
    public boolean canUseCacheFile() {
        return false;
    }

    /** Si lo necesita. */
    public boolean needsCacheFile() {
        return false;
    }

    /**
     * Envuelve ese objeto.
     *
     * @throws IllegalArgumentException si no es de la clase esperada
     * @throws IOException si no se pudo crear
     */
    public abstract ImageOutputStream createOutputStreamInstance(Object output, boolean useCache,
                                                                 File cacheDir)
        throws IOException;

    /**
     * Idem, con cache si hace falta.
     *
     * @throws IOException si no se pudo crear
     */
    public ImageOutputStream createOutputStreamInstance(Object output) throws IOException {
        return createOutputStreamInstance(output, true, null);
    }
}
