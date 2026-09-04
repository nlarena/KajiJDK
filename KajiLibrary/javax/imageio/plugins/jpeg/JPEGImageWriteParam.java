package javax.imageio.plugins.jpeg;

import java.util.Locale;
import javax.imageio.ImageWriteParam;

/**
 * KajiLibrary's javax.imageio.plugins.jpeg.JPEGImageWriteParam -- los parametros propios de JPEG al
 * escribir.
 *
 * <p>Trae tres cosas sobre {@link ImageWriteParam}: las tablas para escribir un JPEG abreviado --ver
 * {@link JPEGImageReadParam}--, la optimizacion de las tablas de Huffman, y las descripciones de
 * calidad.
 *
 * <h2>Los tramos de calidad</h2>
 *
 * <p>{@link #getCompressionQualityDescriptions} devuelve tres nombres y
 * {@link #getCompressionQualityValues} <b>cuatro</b> numeros: son los limites de los tres tramos. La
 * calidad por omision es 0.75, justo el limite entre "media" y "visualmente sin perdida".
 *
 * <p>Ese 0.75 es la razon de que un JPEG guardado con la configuracion de fabrica se vea bien y no
 * ocupe demasiado.
 *
 * <h2>{@link #setOptimizeHuffmanTables}</h2>
 *
 * <p>Apagado por omision, y prenderlo achica el archivo entre un cinco y un diez por ciento sin perder
 * <b>nada</b> de calidad. El costo es una pasada mas sobre la imagen para contar frecuencias y armar
 * las tablas a medida, en lugar de usar las del anexo K.
 *
 * <p>Es la mejora mas barata que tiene JPEG y casi nadie la usa.
 *
 * <h2>{@link #unsetCompression} no vuelve a cero</h2>
 *
 * <p>Deja la calidad en 0.75 y el tipo en {@code "JPEG"} -- no en null, como haria la clase base.
 * JPEG <b>siempre</b> comprime, asi que "sin compresion" no es un estado posible.
 */
public class JPEGImageWriteParam extends ImageWriteParam {

    /** Los tramos de calidad. Ver la nota de la clase. */
    private static final String[] QUALITY_DESCRIPTIONS = {
        "Low quality",
        "Medium quality",
        "Visually lossless",
    };

    /** Sus limites; uno mas que las descripciones. */
    private static final float[] QUALITY_VALUES = { 0.00F, 0.30F, 0.75F, 1.00F };

    /** Las de cuantizacion, o null. */
    private JPEGQTable[] qTables = null;

    /** Las de continua, o null. */
    private JPEGHuffmanTable[] DCHuffmanTables = null;

    /** Las de alterna, o null. */
    private JPEGHuffmanTable[] ACHuffmanTables = null;

    /** Si armar las tablas a medida de la imagen. Ver la nota de la clase. */
    private boolean optimizeHuffmanTables = false;

    /**
     * @param locale en que idioma dar los textos, o null
     */
    public JPEGImageWriteParam(Locale locale) {
        super(locale);
        this.canWriteProgressive = true;
        this.progressiveMode = MODE_DISABLED;
        this.canWriteCompressed = true;
        this.compressionTypes = new String[] { "JPEG" };
        this.compressionType = this.compressionTypes[0];
        this.compressionQuality = 0.75F;
    }

    /**
     * Vuelve a la calidad y el tipo de fabrica.
     *
     * <p>Ver la nota de la clase: no deja el tipo en null, porque JPEG siempre comprime.
     *
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     */
    @Override
    public void unsetCompression() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        this.compressionQuality = 0.75F;
        this.compressionType = this.compressionTypes[0];
    }

    /**
     * No: JPEG pierde siempre.
     *
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     */
    @Override
    public boolean isCompressionLossless() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return false;
    }

    /**
     * Los tres tramos de calidad. Ver la nota de la clase.
     *
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     */
    @Override
    public String[] getCompressionQualityDescriptions() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        String[] copy = new String[QUALITY_DESCRIPTIONS.length];
        System.arraycopy(QUALITY_DESCRIPTIONS, 0, copy, 0, QUALITY_DESCRIPTIONS.length);
        return copy;
    }

    /**
     * Sus cuatro limites.
     *
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     */
    @Override
    public float[] getCompressionQualityValues() {
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        float[] copy = new float[QUALITY_VALUES.length];
        System.arraycopy(QUALITY_VALUES, 0, copy, 0, QUALITY_VALUES.length);
        return copy;
    }

    /** Si hay tablas puestas. */
    public boolean areTablesSet() {
        return this.qTables != null;
    }

    /**
     * Pone los tres juegos; ver {@link JPEGImageReadParam#setDecodeTables}.
     *
     * @throws IllegalArgumentException si alguno es null, si estan vacios, o si los dos de Huffman no
     *     tienen el mismo largo
     */
    public void setEncodeTables(JPEGQTable[] qTables, JPEGHuffmanTable[] DCHuffmanTables,
                                JPEGHuffmanTable[] ACHuffmanTables) {
        if (qTables == null || DCHuffmanTables == null || ACHuffmanTables == null
            || qTables.length == 0 || DCHuffmanTables.length == 0 || ACHuffmanTables.length == 0
            || DCHuffmanTables.length != ACHuffmanTables.length) {
            throw new IllegalArgumentException("Invalid JPEG table arrays");
        }
        this.qTables = new JPEGQTable[qTables.length];
        System.arraycopy(qTables, 0, this.qTables, 0, qTables.length);
        this.DCHuffmanTables = new JPEGHuffmanTable[DCHuffmanTables.length];
        System.arraycopy(DCHuffmanTables, 0, this.DCHuffmanTables, 0, DCHuffmanTables.length);
        this.ACHuffmanTables = new JPEGHuffmanTable[ACHuffmanTables.length];
        System.arraycopy(ACHuffmanTables, 0, this.ACHuffmanTables, 0, ACHuffmanTables.length);
    }

    /** Los saca. */
    public void unsetEncodeTables() {
        this.qTables = null;
        this.DCHuffmanTables = null;
        this.ACHuffmanTables = null;
    }

    /** Las de cuantizacion, o null; una copia. */
    public JPEGQTable[] getQTables() {
        if (this.qTables == null) {
            return null;
        }
        JPEGQTable[] copy = new JPEGQTable[this.qTables.length];
        System.arraycopy(this.qTables, 0, copy, 0, this.qTables.length);
        return copy;
    }

    /** Las de continua, o null; una copia. */
    public JPEGHuffmanTable[] getDCHuffmanTables() {
        if (this.DCHuffmanTables == null) {
            return null;
        }
        JPEGHuffmanTable[] copy = new JPEGHuffmanTable[this.DCHuffmanTables.length];
        System.arraycopy(this.DCHuffmanTables, 0, copy, 0, this.DCHuffmanTables.length);
        return copy;
    }

    /** Las de alterna, o null; una copia. */
    public JPEGHuffmanTable[] getACHuffmanTables() {
        if (this.ACHuffmanTables == null) {
            return null;
        }
        JPEGHuffmanTable[] copy = new JPEGHuffmanTable[this.ACHuffmanTables.length];
        System.arraycopy(this.ACHuffmanTables, 0, copy, 0, this.ACHuffmanTables.length);
        return copy;
    }

    /** Si armar las tablas de Huffman a medida. Ver la nota de la clase: conviene. */
    public void setOptimizeHuffmanTables(boolean optimize) {
        this.optimizeHuffmanTables = optimize;
    }

    /** Si se van a armar a medida. */
    public boolean getOptimizeHuffmanTables() {
        return this.optimizeHuffmanTables;
    }
}
