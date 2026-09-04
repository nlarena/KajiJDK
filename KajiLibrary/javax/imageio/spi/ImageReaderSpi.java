package javax.imageio.spi;

import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * KajiLibrary's javax.imageio.spi.ImageReaderSpi -- el proveedor de un lector de imagenes.
 *
 * <p>Lo que se registra para que {@code ImageIO} sepa que existe un lector de cierto formato. El
 * lector en si no se crea hasta que hace falta.
 *
 * <h2>{@link #canDecodeInput} tiene que dejar el flujo como lo encontro</h2>
 *
 * <p>Es la regla que hace posible todo el mecanismo, y la que se rompe seguido. {@code ImageIO} le
 * pregunta a <b>todos</b> los proveedores registrados, con el mismo flujo: si uno mira los primeros
 * bytes y no rebobina, el proveedor siguiente recibe un flujo consumido y ninguno reconoce nada.
 *
 * <p>La forma correcta es marcar, mirar, y volver -- {@code ImageInputStream} tiene marcas apiladas
 * justamente para esto.
 *
 * <h2>Los nombres de proveedores hermanos</h2>
 *
 * <p>{@link #getImageWriterSpiNames} devuelve los nombres de clase de los <b>escritores</b> del mismo
 * formato. Es como {@code ImageIO.getImageWriter(reader)} encuentra con que volver a escribir lo que
 * se acaba de leer, conservando el formato.
 *
 * <p>Son nombres y no objetos a proposito: asi declarar la relacion no obliga a cargar el escritor.
 */
public abstract class ImageReaderSpi extends ImageReaderWriterSpi {

    /**
     * El tipo de entrada que casi todos aceptan.
     *
     * <p>Un arreglo de un elemento con {@code ImageInputStream.class}. Es publico y mutable --es un
     * arreglo-- lo que es un defecto viejo del JDK; conviene no tocarlo.
     */
    public static final Class<?>[] STANDARD_INPUT_TYPE = { ImageInputStream.class };

    /** Que tipos de entrada acepta. */
    protected Class<?>[] inputTypes = null;

    /** Los escritores del mismo formato. Ver la nota de la clase. */
    protected String[] writerSpiNames = null;

    /** El que exige el cargador de servicios. */
    protected ImageReaderSpi() {
    }

    /**
     * El constructor completo.
     *
     * @param inputTypes que acepta; tipicamente {@link #STANDARD_INPUT_TYPE}
     * @throws IllegalArgumentException si los tipos de entrada faltan o estan vacios
     */
    public ImageReaderSpi(String vendorName, String version, String[] names, String[] suffixes,
                          String[] MIMETypes, String readerClassName, Class<?>[] inputTypes,
                          String[] writerSpiNames,
                          boolean supportsStandardStreamMetadataFormat,
                          String nativeStreamMetadataFormatName,
                          String nativeStreamMetadataFormatClassName,
                          String[] extraStreamMetadataFormatNames,
                          String[] extraStreamMetadataFormatClassNames,
                          boolean supportsStandardImageMetadataFormat,
                          String nativeImageMetadataFormatName,
                          String nativeImageMetadataFormatClassName,
                          String[] extraImageMetadataFormatNames,
                          String[] extraImageMetadataFormatClassNames) {
        super(vendorName, version, names, suffixes, MIMETypes, readerClassName,
              supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName,
              nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames,
              extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat,
              nativeImageMetadataFormatName, nativeImageMetadataFormatClassName,
              extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
        if (inputTypes == null) {
            throw new IllegalArgumentException("inputTypes == null!");
        }
        if (inputTypes.length == 0) {
            throw new IllegalArgumentException("inputTypes.length == 0!");
        }
        this.inputTypes = copyClasses(inputTypes);
        // Un arreglo vacio de escritores hermanos se guarda como null: los dos significan "ninguno",
        // y tener una sola representacion evita que quien lea tenga que contemplar las dos.
        if (writerSpiNames != null && writerSpiNames.length > 0) {
            this.writerSpiNames = copy(writerSpiNames);
        }
    }

    /** Que tipos de entrada acepta. Una copia. */
    public Class<?>[] getInputTypes() {
        return copyClasses(this.inputTypes);
    }

    /**
     * Si este lector reconoce lo que hay en esa entrada.
     *
     * <p>Ver la nota de la clase: <b>tiene que dejar el flujo como lo encontro</b>.
     *
     * @throws IOException si fallo la lectura
     */
    public abstract boolean canDecodeInput(Object source) throws IOException;

    /**
     * Un lector nuevo.
     *
     * @throws IOException si no se pudo crear
     */
    public ImageReader createReaderInstance() throws IOException {
        return createReaderInstance(null);
    }

    /**
     * Idem, con un objeto de configuracion propio del complemento.
     *
     * @param extension lo que el complemento entienda, o null
     * @throws IllegalArgumentException si esa extension no sirve
     * @throws IOException si no se pudo crear
     */
    public abstract ImageReader createReaderInstance(Object extension) throws IOException;

    /**
     * Si ese lector lo creo este proveedor.
     *
     * <p>Se decide por la <b>clase</b>, no por quien lo creo: dos instancias de la misma clase de
     * lector son intercambiables para lo que esto sirve.
     */
    public boolean isOwnReader(ImageReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader == null!");
        }
        String name = reader.getClass().getName();
        return name.equals(this.pluginClassName);
    }

    /** Los escritores del mismo formato, o null. Ver la nota de la clase. */
    public String[] getImageWriterSpiNames() {
        return copyOrNull(this.writerSpiNames);
    }

    /** Una copia de un arreglo de clases, o null. */
    static Class<?>[] copyClasses(Class<?>[] source) {
        if (source == null) {
            return null;
        }
        Class<?>[] result = new Class<?>[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }
}
