package javax.imageio.spi;

import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

/**
 * KajiLibrary's javax.imageio.spi.ImageReaderWriterSpi -- what the reader and the writer providers
 * share.
 *
 * <p>Two things: what the format is called --names, suffixes, MIME types-- and which metadata
 * formats it understands.
 *
 * <h2>The three names of the same format</h2>
 *
 * <p>{@link #getFormatNames} are the informal names a program asks for it by --{@code "jpeg"},
 * {@code "JPG"}--; {@link #getFileSuffixes} the suffixes without a dot; {@link #getMIMETypes} the
 * MIME types. All three are arrays because a format has several of each, and {@code ImageIO} looks
 * up by any of them.
 *
 * <h2>Metadata formats come in two sets</h2>
 *
 * <p>One for the <b>stream</b> metadata --what holds for the whole file-- and another for each
 * <b>image</b>'s. Each set declares whether it supports the standard format, which is its native
 * format, and which others it understands.
 *
 * <p>Each format's class name is resolved by reflection, looking for its static {@code
 * getInstance}; see {@link #getStreamMetadataFormat}. It is what allows declaring a format without
 * loading its class until someone asks for it.
 */
public abstract class ImageReaderWriterSpi extends IIOServiceProvider {

    /** The format's informal names. */
    protected String[] names = null;

    /** The suffixes, without a dot. */
    protected String[] suffixes = null;

    /** The MIME types. */
    protected String[] MIMETypes = null;

    /** The class of the reader or writer this provider creates. */
    protected String pluginClassName = null;

    /** Whether it understands the standard format for stream metadata. */
    protected boolean supportsStandardStreamMetadataFormat = false;

    /** What its native stream format is called, or null. */
    protected String nativeStreamMetadataFormatName = null;

    /** The class that describes it. */
    protected String nativeStreamMetadataFormatClassName = null;

    /** Other stream formats it understands. */
    protected String[] extraStreamMetadataFormatNames = null;

    /** The classes that describe them. */
    protected String[] extraStreamMetadataFormatClassNames = null;

    /** Whether it understands the standard format for image metadata. */
    protected boolean supportsStandardImageMetadataFormat = false;

    /** What its native image format is called, or null. */
    protected String nativeImageMetadataFormatName = null;

    /** The class that describes it. */
    protected String nativeImageMetadataFormatClassName = null;

    /** Other image formats it understands. */
    protected String[] extraImageMetadataFormatNames = null;

    /** The classes that describe them. */
    protected String[] extraImageMetadataFormatClassNames = null;

    /**
     * The full constructor.
     *
     * @throws IllegalArgumentException if the format names are missing or empty, or if the plug-in
     *     class name is null
     */
    public ImageReaderWriterSpi(String vendorName, String version, String[] names,
                                String[] suffixes, String[] MIMETypes, String pluginClassName,
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
        super(vendorName, version);
        if (names == null) {
            throw new IllegalArgumentException("names == null!");
        }
        if (names.length == 0) {
            throw new IllegalArgumentException("names.length == 0!");
        }
        if (pluginClassName == null) {
            throw new IllegalArgumentException("pluginClassName == null!");
        }
        this.names = copy(names);
        this.suffixes = copyOrNull(suffixes);
        this.MIMETypes = copyOrNull(MIMETypes);
        this.pluginClassName = pluginClassName;
        this.supportsStandardStreamMetadataFormat = supportsStandardStreamMetadataFormat;
        this.nativeStreamMetadataFormatName = nativeStreamMetadataFormatName;
        this.nativeStreamMetadataFormatClassName = nativeStreamMetadataFormatClassName;
        this.extraStreamMetadataFormatNames = copyOrNull(extraStreamMetadataFormatNames);
        this.extraStreamMetadataFormatClassNames = copyOrNull(extraStreamMetadataFormatClassNames);
        this.supportsStandardImageMetadataFormat = supportsStandardImageMetadataFormat;
        this.nativeImageMetadataFormatName = nativeImageMetadataFormatName;
        this.nativeImageMetadataFormatClassName = nativeImageMetadataFormatClassName;
        this.extraImageMetadataFormatNames = copyOrNull(extraImageMetadataFormatNames);
        this.extraImageMetadataFormatClassNames = copyOrNull(extraImageMetadataFormatClassNames);
    }

    /** The one the service loader requires; see {@link IIOServiceProvider}. */
    public ImageReaderWriterSpi() {
    }

    /** The format's informal names. A copy. */
    public String[] getFormatNames() {
        return copy(this.names);
    }

    /** The suffixes, without a dot; null if it declared none. */
    public String[] getFileSuffixes() {
        return copyOrNull(this.suffixes);
    }

    /** The MIME types, or null. */
    public String[] getMIMETypes() {
        return copyOrNull(this.MIMETypes);
    }

    /** The class of the plug-in this provider creates. */
    public String getPluginClassName() {
        return this.pluginClassName;
    }

    /** Whether it understands the standard stream metadata format. */
    public boolean isStandardStreamMetadataFormatSupported() {
        return this.supportsStandardStreamMetadataFormat;
    }

    /** Its native stream format, or null. */
    public String getNativeStreamMetadataFormatName() {
        return this.nativeStreamMetadataFormatName;
    }

    /** The other ones it understands, or null. */
    public String[] getExtraStreamMetadataFormatNames() {
        return copyOrNull(this.extraStreamMetadataFormatNames);
    }

    /** Whether it understands the standard image metadata format. */
    public boolean isStandardImageMetadataFormatSupported() {
        return this.supportsStandardImageMetadataFormat;
    }

    /** Its native image format, or null. */
    public String getNativeImageMetadataFormatName() {
        return this.nativeImageMetadataFormatName;
    }

    /** The other ones it understands, or null. */
    public String[] getExtraImageMetadataFormatNames() {
        return copyOrNull(this.extraImageMetadataFormatNames);
    }

    /**
     * The schema of that stream metadata format.
     *
     * <p>See the class note: the class is loaded by reflection only here.
     *
     * @return null if this provider does not understand that format (the JDK throws
     *     {@code IllegalArgumentException} instead)
     * @throws IllegalStateException if the class is declared and could not be loaded
     */
    public IIOMetadataFormat getStreamMetadataFormat(String formatName) {
        return format(formatName, this.supportsStandardStreamMetadataFormat,
                      this.nativeStreamMetadataFormatName,
                      this.nativeStreamMetadataFormatClassName,
                      this.extraStreamMetadataFormatNames,
                      this.extraStreamMetadataFormatClassNames);
    }

    /**
     * Same, for the image ones.
     *
     * @return null if it does not understand that format (the JDK throws
     *     {@code IllegalArgumentException} instead)
     * @throws IllegalStateException if the class is declared and could not be loaded
     */
    public IIOMetadataFormat getImageMetadataFormat(String formatName) {
        return format(formatName, this.supportsStandardImageMetadataFormat,
                      this.nativeImageMetadataFormatName,
                      this.nativeImageMetadataFormatClassName,
                      this.extraImageMetadataFormatNames,
                      this.extraImageMetadataFormatClassNames);
    }

    /** The schema lookup both methods above share. */
    private IIOMetadataFormat format(String formatName, boolean standardSupported,
                                     String nativeName, String nativeClassName,
                                     String[] extraNames, String[] extraClassNames) {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (standardSupported
            && formatName.equals(IIOMetadataFormatImpl.standardMetadataFormatName)) {
            return IIOMetadataFormatImpl.getStandardFormatInstance();
        }
        String className = null;
        if (formatName.equals(nativeName)) {
            className = nativeClassName;
        } else if (extraNames != null) {
            int i = 0;
            while (i < extraNames.length) {
                if (formatName.equals(extraNames[i])) {
                    className = extraClassNames[i];
                }
                i = i + 1;
            }
        }
        if (className == null) {
            return null;
        }
        try {
            Class<?> cls = Class.forName(className, true, getClass().getClassLoader());
            java.lang.reflect.Method meth = cls.getMethod("getInstance");
            return (IIOMetadataFormat) meth.invoke(null);
        } catch (Exception e) {
            throw new IllegalStateException("Can't obtain format");
        }
    }

    /** A copy; fails if it is null. */
    static String[] copy(String[] source) {
        String[] result = new String[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /** A copy, or null. */
    static String[] copyOrNull(String[] source) {
        if (source == null) {
            return null;
        }
        return copy(source);
    }
}
