package javax.imageio.spi;

import java.awt.image.RenderedImage;
import java.io.IOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * KajiLibrary's javax.imageio.spi.ImageWriterSpi -- el proveedor de un escritor de imagenes.
 *
 * <p>El espejo de {@link ImageReaderSpi}. La diferencia esta en la pregunta que responde: aquel
 * pregunta si <b>reconoce</b> lo que hay en un flujo, este si <b>puede escribir</b> cierto tipo de
 * imagen.
 *
 * <p>Y esa pregunta no necesita tocar el flujo, asi que no tiene la regla de rebobinar.
 *
 * <h2>{@link #canEncodeImage} es la parte importante</h2>
 *
 * <p>Un formato acepta unos tipos de pixel y no otros: un GIF no guarda color verdadero, un JPEG
 * clasico no guarda transparencia. Preguntar antes es lo que evita escribir un archivo que pierde la
 * mitad de la imagen en silencio.
 *
 * <p>{@link #isFormatLossless} dice si el formato conserva todo. Por omision <b>true</b>, que es el
 * valor conservador: un escritor con perdida tiene que decirlo.
 */
public abstract class ImageWriterSpi extends ImageReaderWriterSpi {

    /**
     * El tipo de salida que casi todos aceptan.
     *
     * <p>Un arreglo de un elemento con {@code ImageOutputStream.class}; ver
     * {@link ImageReaderSpi#STANDARD_INPUT_TYPE}.
     */
    public static final Class<?>[] STANDARD_OUTPUT_TYPE = { ImageOutputStream.class };

    /** Que tipos de salida acepta. */
    protected Class<?>[] outputTypes = null;

    /** Los lectores del mismo formato. */
    protected String[] readerSpiNames = null;

    /** El que exige el cargador de servicios. */
    protected ImageWriterSpi() {
    }

    /**
     * El constructor completo.
     *
     * @throws IllegalArgumentException si los tipos de salida faltan o estan vacios
     */
    public ImageWriterSpi(String vendorName, String version, String[] names, String[] suffixes,
                          String[] MIMETypes, String writerClassName, Class<?>[] outputTypes,
                          String[] readerSpiNames,
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
        super(vendorName, version, names, suffixes, MIMETypes, writerClassName,
              supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName,
              nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames,
              extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat,
              nativeImageMetadataFormatName, nativeImageMetadataFormatClassName,
              extraImageMetadataFormatNames, extraImageMetadataFormatClassNames);
        if (outputTypes == null) {
            throw new IllegalArgumentException("outputTypes == null!");
        }
        if (outputTypes.length == 0) {
            throw new IllegalArgumentException("outputTypes.length == 0!");
        }
        this.outputTypes = ImageReaderSpi.copyClasses(outputTypes);
        if (readerSpiNames != null && readerSpiNames.length > 0) {
            this.readerSpiNames = copy(readerSpiNames);
        }
    }

    /** Si el formato conserva todo. Ver la nota de la clase: por omision true. */
    public boolean isFormatLossless() {
        return true;
    }

    /** Que tipos de salida acepta. Una copia. */
    public Class<?>[] getOutputTypes() {
        return ImageReaderSpi.copyClasses(this.outputTypes);
    }

    /** Si puede escribir imagenes de ese tipo. Ver la nota de la clase. */
    public abstract boolean canEncodeImage(ImageTypeSpecifier type);

    /**
     * Idem, preguntando por una imagen concreta.
     *
     * @throws IllegalArgumentException si es null
     */
    public boolean canEncodeImage(RenderedImage im) {
        if (im == null) {
            throw new IllegalArgumentException("im == null!");
        }
        return canEncodeImage(new ImageTypeSpecifier(im));
    }

    /**
     * Un escritor nuevo.
     *
     * @throws IOException si no se pudo crear
     */
    public ImageWriter createWriterInstance() throws IOException {
        return createWriterInstance(null);
    }

    /**
     * Idem, con configuracion propia del complemento.
     *
     * @throws IOException si no se pudo crear
     */
    public abstract ImageWriter createWriterInstance(Object extension) throws IOException;

    /** Si ese escritor lo creo este proveedor. Ver {@link ImageReaderSpi#isOwnReader}. */
    public boolean isOwnWriter(ImageWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer == null!");
        }
        String name = writer.getClass().getName();
        return name.equals(this.pluginClassName);
    }

    /** Los lectores del mismo formato, o null. */
    public String[] getImageReaderSpiNames() {
        return copyOrNull(this.readerSpiNames);
    }
}
