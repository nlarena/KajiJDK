package javax.imageio.spi;

import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * KajiLibrary's javax.imageio.spi.ImageReaderSpi -- the provider of an image reader.
 *
 * <p>What gets registered so that {@code ImageIO} knows a reader for some format exists. The reader
 * itself is not created until it is needed.
 *
 * <h2>{@link #canDecodeInput} has to leave the stream as it found it</h2>
 *
 * <p>It is the rule that makes the whole mechanism possible, and the one broken most often.
 * {@code ImageIO} asks <b>every</b> registered provider, with the same stream: if one looks at the
 * first bytes and does not rewind, the next provider gets a consumed stream and none recognizes
 * anything.
 *
 * <p>The right way is to mark, look, and go back -- {@code ImageInputStream} has stacked marks
 * precisely for this.
 *
 * <h2>The names of sibling providers</h2>
 *
 * <p>{@link #getImageWriterSpiNames} returns the class names of the <b>writers</b> of the same
 * format. It is how {@code ImageIO.getImageWriter(reader)} finds what to write back what was just
 * read with, keeping the format.
 *
 * <p>They are names and not objects on purpose: that way declaring the relation does not force
 * loading the writer.
 */
public abstract class ImageReaderSpi extends ImageReaderWriterSpi {

    /**
     * The input type almost all of them accept.
     *
     * <p>A one-element array with {@code ImageInputStream.class}. It is public and mutable --it is
     * an array--, which is an old JDK defect; better not to touch it. The JDK marks it {@code
     * @Deprecated} (build the array yourself instead); here it is not marked.
     */
    public static final Class<?>[] STANDARD_INPUT_TYPE = { ImageInputStream.class };

    /** Which input types it accepts. */
    protected Class<?>[] inputTypes = null;

    /** The writers of the same format. See the class note. */
    protected String[] writerSpiNames = null;

    /** The one the service loader requires. */
    protected ImageReaderSpi() {
    }

    /**
     * The full constructor.
     *
     * @param inputTypes what it accepts; typically {@link #STANDARD_INPUT_TYPE}
     * @throws IllegalArgumentException if the input types are missing or empty
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
        // An empty array of sibling writers is stored as null: both mean "none", and having a
        // single representation spares whoever reads it from handling both.
        if (writerSpiNames != null && writerSpiNames.length > 0) {
            this.writerSpiNames = copy(writerSpiNames);
        }
    }

    /** Which input types it accepts. A copy. */
    public Class<?>[] getInputTypes() {
        return copyClasses(this.inputTypes);
    }

    /**
     * Whether this reader recognizes what is in that input.
     *
     * <p>See the class note: <b>it has to leave the stream as it found it</b>.
     *
     * @throws IOException if reading failed
     */
    public abstract boolean canDecodeInput(Object source) throws IOException;

    /**
     * A new reader.
     *
     * @throws IOException if it could not be created
     */
    public ImageReader createReaderInstance() throws IOException {
        return createReaderInstance(null);
    }

    /**
     * Same, with a configuration object of the plug-in's own.
     *
     * @param extension whatever the plug-in understands, or null
     * @throws IllegalArgumentException if that extension does not work
     * @throws IOException if it could not be created
     */
    public abstract ImageReader createReaderInstance(Object extension) throws IOException;

    /**
     * Whether this provider created that reader.
     *
     * <p>It is decided by the <b>class</b>, not by who created it: two instances of the same reader
     * class are interchangeable for what this is for.
     */
    public boolean isOwnReader(ImageReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader == null!");
        }
        String name = reader.getClass().getName();
        return name.equals(this.pluginClassName);
    }

    /** The writers of the same format, or null. See the class note. */
    public String[] getImageWriterSpiNames() {
        return copyOrNull(this.writerSpiNames);
    }

    /** A copy of an array of classes, or null. */
    static Class<?>[] copyClasses(Class<?>[] source) {
        if (source == null) {
            return null;
        }
        Class<?>[] result = new Class<?>[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }
}
