package javax.imageio.spi;

import java.io.File;
import java.io.IOException;
import javax.imageio.stream.ImageInputStream;

/**
 * KajiLibrary's javax.imageio.spi.ImageInputStreamSpi -- el proveedor de un flujo de entrada de
 * imagenes.
 *
 * <p>Sabe envolver objetos de <b>una</b> clase --{@link #getInputClass}: un {@code File}, un
 * {@code InputStream}, una {@code URL}-- en un {@link ImageInputStream}. Es lo que
 * {@code ImageIO.createImageInputStream} consulta.
 *
 * <h2>La cache</h2>
 *
 * <p>{@link #canUseCacheFile} y {@link #needsCacheFile} son distintos y la diferencia decide si el
 * argumento {@code cacheDir} sirve de algo:
 *
 * <ul>
 *   <li><b>puede</b>: sabe funcionar con archivo de cache y tambien sin el. Un {@code InputStream}
 *       cae aca -- se puede cachear en disco o en memoria;
 *   <li><b>necesita</b>: no funciona sin archivo. Implica que puede.
 * </ul>
 *
 * <p>Un proveedor sobre {@code File} no precisa ninguna de las dos: el archivo ya se posiciona solo.
 */
public abstract class ImageInputStreamSpi extends IIOServiceProvider {

    /** Que clase de objeto sabe envolver. */
    protected Class<?> inputClass;

    /** El que exige el cargador de servicios. */
    protected ImageInputStreamSpi() {
    }

    /**
     * @param inputClass que clase sabe envolver
     * @throws IllegalArgumentException si es null
     */
    public ImageInputStreamSpi(String vendorName, String version, Class<?> inputClass) {
        super(vendorName, version);
        if (inputClass == null) {
            throw new IllegalArgumentException("inputClass == null!");
        }
        this.inputClass = inputClass;
    }

    /** Que clase sabe envolver. */
    public Class<?> getInputClass() {
        return this.inputClass;
    }

    /** Si sabe usar un archivo de cache. Ver la nota de la clase. */
    public boolean canUseCacheFile() {
        return false;
    }

    /** Si lo necesita. Ver la nota de la clase: implica que puede. */
    public boolean needsCacheFile() {
        return false;
    }

    /**
     * Envuelve ese objeto.
     *
     * @param useCache si usar un archivo de cache; se ignora si no lo soporta
     * @param cacheDir donde ponerlo, o null para el del sistema
     * @throws IllegalArgumentException si el objeto no es de la clase esperada, o si el directorio no
     *     lo es
     * @throws IOException si no se pudo crear
     */
    public abstract ImageInputStream createInputStreamInstance(Object input, boolean useCache,
                                                               File cacheDir) throws IOException;

    /**
     * Idem, con cache si el proveedor la necesita y sin ella si no.
     *
     * @throws IOException si no se pudo crear
     */
    public ImageInputStream createInputStreamInstance(Object input) throws IOException {
        return createInputStreamInstance(input, true, null);
    }
}
