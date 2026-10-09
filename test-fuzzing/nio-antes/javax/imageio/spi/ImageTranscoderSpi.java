package javax.imageio.spi;

import javax.imageio.ImageTranscoder;

/**
 * KajiLibrary's javax.imageio.spi.ImageTranscoderSpi -- the provider of a metadata transcoder.
 *
 * <p>It declares that it knows how to translate metadata from <b>one</b> specific reader to
 * <b>one</b> specific writer, named by the class of their providers.
 *
 * <p>That specificity is the point. Every writer already knows how to translate from the standard
 * format --it is {@link ImageTranscoder}, which {@code ImageWriter} implements--, but that
 * translation goes through the common one and loses what is specific. A dedicated transcoder
 * between two similar formats can keep much more.
 */
public abstract class ImageTranscoderSpi extends IIOServiceProvider {

    /** The one the service loader requires. */
    protected ImageTranscoderSpi() {
    }

    /** With a name and a version. */
    public ImageTranscoderSpi(String vendorName, String version) {
        super(vendorName, version);
    }

    /** The class of the reader provider it knows how to translate from. */
    public abstract String getReaderServiceProviderName();

    /** The one of the writer provider it knows how to translate to. */
    public abstract String getWriterServiceProviderName();

    /** A new transcoder. */
    public abstract ImageTranscoder createTranscoderInstance();
}
