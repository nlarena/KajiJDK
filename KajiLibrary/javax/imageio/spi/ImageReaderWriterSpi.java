package javax.imageio.spi;

import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataFormatImpl;

/**
 * KajiLibrary's javax.imageio.spi.ImageReaderWriterSpi -- lo que comparten el proveedor de lectores y
 * el de escritores.
 *
 * <p>Dos cosas: como se llama el formato --nombres, extensiones, tipos MIME-- y que formatos de
 * metadatos entiende.
 *
 * <h2>Los tres nombres del mismo formato</h2>
 *
 * <p>{@link #getFormatNames} son los nombres informales con los que un programa lo pide
 * --{@code "jpeg"}, {@code "JPG"}--; {@link #getFileSuffixes} las extensiones sin punto;
 * {@link #getMIMETypes} los tipos MIME. Los tres son arreglos porque un formato tiene varios de cada
 * uno, y {@code ImageIO} busca por cualquiera.
 *
 * <h2>Los formatos de metadatos vienen de a dos juegos</h2>
 *
 * <p>Uno para los metadatos del <b>flujo</b> --lo que vale para el archivo entero-- y otro para los de
 * cada <b>imagen</b>. Cada juego declara si soporta el formato estandar, cual es su formato nativo, y
 * que otros entiende.
 *
 * <p>El nombre de clase de cada formato se resuelve por reflexion, buscando su {@code getInstance}
 * estatico; ver {@link #getStreamMetadataFormat}. Es lo que permite declarar un formato sin cargar su
 * clase hasta que alguien lo pida.
 */
public abstract class ImageReaderWriterSpi extends IIOServiceProvider {

    /** Los nombres informales del formato. */
    protected String[] names = null;

    /** Las extensiones, sin punto. */
    protected String[] suffixes = null;

    /** Los tipos MIME. */
    protected String[] MIMETypes = null;

    /** La clase del lector o escritor que este proveedor crea. */
    protected String pluginClassName = null;

    /** Si entiende el formato estandar para los metadatos de flujo. */
    protected boolean supportsStandardStreamMetadataFormat = false;

    /** Como se llama su formato nativo de flujo, o null. */
    protected String nativeStreamMetadataFormatName = null;

    /** La clase que lo describe. */
    protected String nativeStreamMetadataFormatClassName = null;

    /** Otros formatos de flujo que entiende. */
    protected String[] extraStreamMetadataFormatNames = null;

    /** Las clases que los describen. */
    protected String[] extraStreamMetadataFormatClassNames = null;

    /** Si entiende el formato estandar para los metadatos de imagen. */
    protected boolean supportsStandardImageMetadataFormat = false;

    /** Como se llama su formato nativo de imagen, o null. */
    protected String nativeImageMetadataFormatName = null;

    /** La clase que lo describe. */
    protected String nativeImageMetadataFormatClassName = null;

    /** Otros formatos de imagen que entiende. */
    protected String[] extraImageMetadataFormatNames = null;

    /** Las clases que los describen. */
    protected String[] extraImageMetadataFormatClassNames = null;

    /**
     * El constructor completo.
     *
     * @throws IllegalArgumentException si los nombres del formato faltan o estan vacios, o si el
     *     nombre de la clase del complemento es null
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

    /** El que exige el cargador de servicios; ver {@link IIOServiceProvider}. */
    public ImageReaderWriterSpi() {
    }

    /** Los nombres informales del formato. Una copia. */
    public String[] getFormatNames() {
        return copy(this.names);
    }

    /** Las extensiones, sin punto; null si no declaro ninguna. */
    public String[] getFileSuffixes() {
        return copyOrNull(this.suffixes);
    }

    /** Los tipos MIME, o null. */
    public String[] getMIMETypes() {
        return copyOrNull(this.MIMETypes);
    }

    /** La clase del complemento que este proveedor crea. */
    public String getPluginClassName() {
        return this.pluginClassName;
    }

    /** Si entiende el formato estandar de metadatos de flujo. */
    public boolean isStandardStreamMetadataFormatSupported() {
        return this.supportsStandardStreamMetadataFormat;
    }

    /** Su formato nativo de flujo, o null. */
    public String getNativeStreamMetadataFormatName() {
        return this.nativeStreamMetadataFormatName;
    }

    /** Los otros que entiende, o null. */
    public String[] getExtraStreamMetadataFormatNames() {
        return copyOrNull(this.extraStreamMetadataFormatNames);
    }

    /** Si entiende el formato estandar de metadatos de imagen. */
    public boolean isStandardImageMetadataFormatSupported() {
        return this.supportsStandardImageMetadataFormat;
    }

    /** Su formato nativo de imagen, o null. */
    public String getNativeImageMetadataFormatName() {
        return this.nativeImageMetadataFormatName;
    }

    /** Los otros que entiende, o null. */
    public String[] getExtraImageMetadataFormatNames() {
        return copyOrNull(this.extraImageMetadataFormatNames);
    }

    /**
     * El esquema de ese formato de metadatos de flujo.
     *
     * <p>Ver la nota de la clase: la clase se carga por reflexion recien aca.
     *
     * @return null si este proveedor no entiende ese formato
     * @throws IllegalStateException si la clase esta declarada y no se pudo cargar
     */
    public IIOMetadataFormat getStreamMetadataFormat(String formatName) {
        return format(formatName, this.supportsStandardStreamMetadataFormat,
                      this.nativeStreamMetadataFormatName,
                      this.nativeStreamMetadataFormatClassName,
                      this.extraStreamMetadataFormatNames,
                      this.extraStreamMetadataFormatClassNames);
    }

    /**
     * Idem, para los de imagen.
     *
     * @return null si no entiende ese formato
     * @throws IllegalStateException si la clase esta declarada y no se pudo cargar
     */
    public IIOMetadataFormat getImageMetadataFormat(String formatName) {
        return format(formatName, this.supportsStandardImageMetadataFormat,
                      this.nativeImageMetadataFormatName,
                      this.nativeImageMetadataFormatClassName,
                      this.extraImageMetadataFormatNames,
                      this.extraImageMetadataFormatClassNames);
    }

    /** La busqueda de esquema que comparten los dos metodos de arriba. */
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

    /** Una copia; falla si es null. */
    static String[] copy(String[] source) {
        String[] result = new String[source.length];
        System.arraycopy(source, 0, result, 0, source.length);
        return result;
    }

    /** Una copia, o null. */
    static String[] copyOrNull(String[] source) {
        if (source == null) {
            return null;
        }
        return copy(source);
    }
}
