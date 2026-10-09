package javax.imageio.spi;

import java.awt.image.RenderedImage;
import java.io.IOException;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * KajiLibrary's javax.imageio.spi.ImageWriterSpi -- the provider of an image writer.
 *
 * <p>The mirror of {@link ImageReaderSpi}. The difference is in the question it answers: that one
 * asks whether it <b>recognizes</b> what is in a stream, this one whether it <b>can write</b> a
 * certain type of image.
 *
 * <p>And that question does not need to touch the stream, so it has no rewind rule.
 *
 * <h2>{@link #canEncodeImage} is the important part</h2>
 *
 * <p>A format accepts some pixel types and not others: a GIF does not store true colour, a classic
 * JPEG does not store transparency. Asking beforehand is what avoids writing a file that silently
 * loses half the image.
 *
 * <p>{@link #isFormatLossless} says whether the format keeps everything. By default <b>true</b>,
 * which is the conservative value: a lossy writer has to say so.
 */
public abstract class ImageWriterSpi extends ImageReaderWriterSpi {

    /**
     * The output type almost all of them accept.
     *
     * <p>A one-element array with {@code ImageOutputStream.class}; see
     * {@link ImageReaderSpi#STANDARD_INPUT_TYPE} (the JDK deprecates this one too).
     */
    public static final Class<?>[] STANDARD_OUTPUT_TYPE = { ImageOutputStream.class };

    /** Which output types it accepts. */
    protected Class<?>[] outputTypes = null;

    /** The readers of the same format. */
    protected String[] readerSpiNames = null;

    /** The one the service loader requires. */
    protected ImageWriterSpi() {
    }

    /**
     * The full constructor.
     *
     * @throws IllegalArgumentException if the output types are missing or empty
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

    /** Whether the format keeps everything. See the class note: true by default. */
    public boolean isFormatLossless() {
        return true;
    }

    /** Which output types it accepts. A copy. */
    public Class<?>[] getOutputTypes() {
        return ImageReaderSpi.copyClasses(this.outputTypes);
    }

    /** Whether it can write images of that type. See the class note. */
    public abstract boolean canEncodeImage(ImageTypeSpecifier type);

    /**
     * Same, asking about a concrete image.
     *
     * @throws IllegalArgumentException if it is null
     */
    public boolean canEncodeImage(RenderedImage im) {
        if (im == null) {
            throw new IllegalArgumentException("im == null!");
        }
        return canEncodeImage(new ImageTypeSpecifier(im));
    }

    /**
     * A new writer.
     *
     * @throws IOException if it could not be created
     */
    public ImageWriter createWriterInstance() throws IOException {
        return createWriterInstance(null);
    }

    /**
     * Same, with the plug-in's own configuration.
     *
     * @throws IOException if it could not be created
     */
    public abstract ImageWriter createWriterInstance(Object extension) throws IOException;

    /** Whether this provider created that writer. See {@link ImageReaderSpi#isOwnReader}. */
    public boolean isOwnWriter(ImageWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer == null!");
        }
        String name = writer.getClass().getName();
        return name.equals(this.pluginClassName);
    }

    /** The readers of the same format, or null. */
    public String[] getImageReaderSpiNames() {
        return copyOrNull(this.readerSpiNames);
    }
}
