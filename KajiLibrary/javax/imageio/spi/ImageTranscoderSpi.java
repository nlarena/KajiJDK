package javax.imageio.spi;

import javax.imageio.ImageTranscoder;

/**
 * KajiLibrary's javax.imageio.spi.ImageTranscoderSpi -- el proveedor de un traductor de metadatos.
 *
 * <p>Declara que sabe traducir metadatos de <b>un</b> lector a <b>un</b> escritor concretos, nombrados
 * por la clase de sus proveedores.
 *
 * <p>Esa especificidad es el punto. Todo escritor ya sabe traducir desde el formato estandar --es
 * {@link ImageTranscoder}, que {@code ImageWriter} implementa--, pero esa traduccion pasa por el
 * comun y pierde lo especifico. Un traductor dedicado entre dos formatos parecidos puede conservar
 * mucho mas.
 */
public abstract class ImageTranscoderSpi extends IIOServiceProvider {

    /** El que exige el cargador de servicios. */
    protected ImageTranscoderSpi() {
    }

    /** Con nombre y version. */
    public ImageTranscoderSpi(String vendorName, String version) {
        super(vendorName, version);
    }

    /** La clase del proveedor de lectores del que sabe traducir. */
    public abstract String getReaderServiceProviderName();

    /** La del proveedor de escritores al que sabe traducir. */
    public abstract String getWriterServiceProviderName();

    /** Un traductor nuevo. */
    public abstract ImageTranscoder createTranscoderInstance();
}
