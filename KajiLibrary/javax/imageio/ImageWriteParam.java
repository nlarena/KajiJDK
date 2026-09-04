package javax.imageio;

import java.awt.Dimension;
import java.util.Locale;

/**
 * KajiLibrary's javax.imageio.ImageWriteParam -- como escribir una imagen.
 *
 * <p>Agrega sobre {@link IIOParam} lo que solo tiene sentido al escribir: mosaico, progresividad y
 * compresion. Los tres funcionan igual y ese patron es la clase entera.
 *
 * <h2>Los cuatro modos, y por que hay cuatro</h2>
 *
 * <p>Cada una de las tres caracteristicas se controla con un <b>modo</b>:
 *
 * <ul>
 *   <li>{@link #MODE_DISABLED}: no hacerlo. Sin mosaico, sin progresividad, sin compresion;
 *   <li>{@link #MODE_DEFAULT}: hacerlo como el escritor prefiera. No se puede consultar con que
 *       parametros -- preguntar el ancho de tesela en este modo lanza {@link IllegalStateException};
 *   <li>{@link #MODE_EXPLICIT}: hacerlo con los parametros que se den. Es el unico donde los
 *       {@code setXxx} y los {@code getXxx} concretos valen;
 *   <li>{@link #MODE_COPY_FROM_METADATA}: tomarlos de los metadatos de la imagen. Es el modo <b>por
 *       omision</b>, y es el correcto al reescribir algo que se leyo.
 * </ul>
 *
 * <p>Que el modo por omision sea el ultimo sorprende. Tiene sentido: la operacion mas comun es leer y
 * volver a escribir, y ahi lo que se quiere es conservar lo que el archivo original decia.
 *
 * <h2>Dos excepciones distintas</h2>
 *
 * <p>Es lo que mas confunde de esta clase:
 *
 * <ul>
 *   <li>{@link UnsupportedOperationException} significa "<b>este escritor</b> no sabe hacer eso". Se
 *       pregunta antes con {@link #canWriteTiles} y companeros;
 *   <li>{@link IllegalStateException} significa "sabe, pero el modo no es
 *       {@link #MODE_EXPLICIT}", o "todavia no se fijaron los parametros".
 * </ul>
 *
 * <p>La primera es un problema de eleccion de escritor; la segunda, del orden de las llamadas.
 *
 * <h2>La calidad va al reves de lo que parece</h2>
 *
 * <p>{@link #setCompressionQuality} toma un valor de 0 a 1 donde <b>1 es la mejor calidad</b> --y el
 * archivo mas grande--. Es lo contrario de "nivel de compresion", que en otras APIs va de menos a
 * mas. El de omision es 1.
 */
public class ImageWriteParam extends IIOParam {

    /** No hacerlo. */
    public static final int MODE_DISABLED = 0;

    /** Hacerlo como el escritor prefiera. Ver la nota de la clase. */
    public static final int MODE_DEFAULT = 1;

    /** Hacerlo con los parametros dados. */
    public static final int MODE_EXPLICIT = 2;

    /** Tomarlos de los metadatos. Es el de omision. */
    public static final int MODE_COPY_FROM_METADATA = 3;

    /** Si este escritor sabe hacer mosaico. */
    protected boolean canWriteTiles = false;

    /** En que modo esta el mosaico. */
    protected int tilingMode = MODE_COPY_FROM_METADATA;

    /** Los tamanos de tesela que prefiere, o null. */
    protected Dimension[] preferredTileSizes = null;

    /** Si ya se fijaron los parametros de mosaico. */
    protected boolean tilingSet = false;

    /** Ancho de tesela. */
    protected int tileWidth = 0;

    /** Alto de tesela. */
    protected int tileHeight = 0;

    /** Si sabe desplazar la rejilla de teselas. */
    protected boolean canOffsetTiles = false;

    /** Desplazamiento de la rejilla en X. */
    protected int tileGridXOffset = 0;

    /** Idem en Y. */
    protected int tileGridYOffset = 0;

    /** Si sabe escribir progresivo. */
    protected boolean canWriteProgressive = false;

    /** En que modo esta la progresividad. */
    protected int progressiveMode = MODE_COPY_FROM_METADATA;

    /** Si sabe comprimir. */
    protected boolean canWriteCompressed = false;

    /** En que modo esta la compresion. */
    protected int compressionMode = MODE_COPY_FROM_METADATA;

    /** Los tipos de compresion que soporta, o null. */
    protected String[] compressionTypes = null;

    /** El elegido, o null. */
    protected String compressionType = null;

    /** La calidad, de 0 a 1. Ver la nota de la clase. */
    protected float compressionQuality = 1.0F;

    /** En que idioma dar los textos, o null. */
    protected Locale locale = null;

    /** Para las subclases, que fijan las capacidades. */
    protected ImageWriteParam() {
    }

    /** @param locale en que idioma dar los textos, o null */
    public ImageWriteParam(Locale locale) {
        this.locale = locale;
    }

    /** En que idioma, o null. */
    public Locale getLocale() {
        return this.locale;
    }

    /** Si este escritor sabe hacer mosaico. */
    public boolean canWriteTiles() {
        return this.canWriteTiles;
    }

    /** Si sabe desplazar la rejilla. */
    public boolean canOffsetTiles() {
        return this.canOffsetTiles;
    }

    /**
     * Fija el modo del mosaico.
     *
     * @throws UnsupportedOperationException si este escritor no sabe hacer mosaico
     * @throws IllegalArgumentException si el modo no es uno de los cuatro, o si se pide
     *     {@link #MODE_EXPLICIT} sin poder desplazar y con desplazamiento pedido
     */
    public void setTilingMode(int mode) {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (mode < MODE_DISABLED || mode > MODE_COPY_FROM_METADATA) {
            throw new IllegalArgumentException("Illegal value for mode!");
        }
        this.tilingMode = mode;
        if (mode == MODE_EXPLICIT) {
            unsetTiling();
        }
    }

    /**
     * En que modo esta.
     *
     * @throws UnsupportedOperationException si no sabe hacer mosaico
     */
    public int getTilingMode() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported");
        }
        return this.tilingMode;
    }

    /**
     * Los tamanos de tesela que prefiere, de a pares minimo y maximo; null si no opina.
     *
     * @throws UnsupportedOperationException si no sabe hacer mosaico
     */
    public Dimension[] getPreferredTileSizes() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported");
        }
        if (this.preferredTileSizes == null) {
            return null;
        }
        Dimension[] copy = new Dimension[this.preferredTileSizes.length];
        int i = 0;
        while (i < this.preferredTileSizes.length) {
            copy[i] = (Dimension) this.preferredTileSizes[i].clone();
            i = i + 1;
        }
        return copy;
    }

    /**
     * Fija el tamano y el desplazamiento de las teselas.
     *
     * @throws UnsupportedOperationException si no sabe hacer mosaico, o si se pide desplazamiento y
     *     no sabe desplazar
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     * @throws IllegalArgumentException si el ancho o el alto no son positivos, o si no estan entre los
     *     tamanos preferidos
     */
    public void setTiling(int tileWidth, int tileHeight, int tileGridXOffset,
                          int tileGridYOffset) {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (tileWidth <= 0 || tileHeight <= 0) {
            throw new IllegalArgumentException("tile dimensions are non-positive!");
        }
        boolean canOffset = canOffsetTiles();
        if (!canOffset && (tileGridXOffset != 0 || tileGridYOffset != 0)) {
            throw new UnsupportedOperationException("Can't offset tiles!");
        }
        if (this.preferredTileSizes != null) {
            // Los tamanos preferidos vienen de a pares: minimo y maximo de cada rango aceptable.
            boolean ok = false;
            int i = 0;
            while (i < this.preferredTileSizes.length && !ok) {
                Dimension min = this.preferredTileSizes[i];
                Dimension max = this.preferredTileSizes[i + 1];
                if (tileWidth >= min.width && tileWidth <= max.width
                    && tileHeight >= min.height && tileHeight <= max.height) {
                    ok = true;
                }
                i = i + 2;
            }
            if (!ok) {
                throw new IllegalArgumentException("Illegal tile size!");
            }
        }
        this.tilingSet = true;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileGridXOffset = tileGridXOffset;
        this.tileGridYOffset = tileGridYOffset;
    }

    /**
     * Olvida los parametros de mosaico.
     *
     * @throws UnsupportedOperationException si no sabe hacer mosaico
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     */
    public void unsetTiling() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        this.tilingSet = false;
        this.tileWidth = 0;
        this.tileHeight = 0;
        this.tileGridXOffset = 0;
        this.tileGridYOffset = 0;
    }

    /**
     * El ancho de tesela.
     *
     * @throws UnsupportedOperationException si no sabe hacer mosaico
     * @throws IllegalStateException si el modo no es explicito o no se fijaron los parametros
     */
    public int getTileWidth() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (!this.tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
        return this.tileWidth;
    }

    /** El alto. Mismas condiciones que {@link #getTileWidth}. */
    public int getTileHeight() {
        if (!canWriteTiles()) {
            throw new UnsupportedOperationException("Tiling not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (!this.tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
        return this.tileHeight;
    }

    /**
     * El desplazamiento de la rejilla en X.
     *
     * @throws UnsupportedOperationException si no sabe desplazar teselas
     * @throws IllegalStateException si el modo no es explicito o no se fijaron los parametros
     */
    public int getTileGridXOffset() {
        if (!canOffsetTiles()) {
            throw new UnsupportedOperationException("Tile offsets not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (!this.tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
        return this.tileGridXOffset;
    }

    /** Idem en Y. */
    public int getTileGridYOffset() {
        if (!canOffsetTiles()) {
            throw new UnsupportedOperationException("Tile offsets not supported!");
        }
        if (getTilingMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Tiling mode not MODE_EXPLICIT!");
        }
        if (!this.tilingSet) {
            throw new IllegalStateException("Tiling parameters not set!");
        }
        return this.tileGridYOffset;
    }

    /** Si este escritor sabe escribir progresivo. */
    public boolean canWriteProgressive() {
        return this.canWriteProgressive;
    }

    /**
     * Fija el modo de progresividad.
     *
     * <p>{@link #MODE_EXPLICIT} <b>no</b> se admite aca: no hay parametros que dar, un archivo es
     * progresivo o no lo es.
     *
     * @throws UnsupportedOperationException si no sabe
     * @throws IllegalArgumentException si el modo es {@link #MODE_EXPLICIT} o no es uno de los cuatro
     */
    public void setProgressiveMode(int mode) {
        if (!canWriteProgressive()) {
            throw new UnsupportedOperationException("Progressive output not supported");
        }
        if (mode < MODE_DISABLED || mode > MODE_COPY_FROM_METADATA) {
            throw new IllegalArgumentException("Illegal value for mode!");
        }
        if (mode == MODE_EXPLICIT) {
            throw new IllegalArgumentException(
                "MODE_EXPLICIT not supported for progressive output");
        }
        this.progressiveMode = mode;
    }

    /**
     * En que modo esta.
     *
     * @throws UnsupportedOperationException si no sabe
     */
    public int getProgressiveMode() {
        if (!canWriteProgressive()) {
            throw new UnsupportedOperationException("Progressive output not supported");
        }
        return this.progressiveMode;
    }

    /** Si este escritor sabe comprimir. */
    public boolean canWriteCompressed() {
        return this.canWriteCompressed;
    }

    /**
     * Fija el modo de compresion.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalArgumentException si el modo no es uno de los cuatro
     */
    public void setCompressionMode(int mode) {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (mode < MODE_DISABLED || mode > MODE_COPY_FROM_METADATA) {
            throw new IllegalArgumentException("Illegal value for mode!");
        }
        this.compressionMode = mode;
        if (mode == MODE_EXPLICIT) {
            unsetCompression();
        }
    }

    /**
     * En que modo esta.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     */
    public int getCompressionMode() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        return this.compressionMode;
    }

    /**
     * Los tipos de compresion que soporta, o null si no hay varios.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     */
    public String[] getCompressionTypes() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (this.compressionTypes == null) {
            return null;
        }
        String[] copy = new String[this.compressionTypes.length];
        System.arraycopy(this.compressionTypes, 0, copy, 0, this.compressionTypes.length);
        return copy;
    }

    /**
     * Elige el tipo de compresion.
     *
     * @param compressionType uno de {@link #getCompressionTypes}; null lo desetea
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     * @throws IllegalArgumentException si no es uno de los soportados
     */
    public void setCompressionType(String compressionType) {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        String[] legalTypes = getCompressionTypes();
        if (compressionType == null) {
            this.compressionType = null;
            return;
        }
        if (legalTypes == null) {
            throw new UnsupportedOperationException("No settable compression types");
        }
        boolean found = false;
        int i = 0;
        while (i < legalTypes.length) {
            if (compressionType.equals(legalTypes[i])) {
                found = true;
            }
            i = i + 1;
        }
        if (!found) {
            throw new IllegalArgumentException("Unknown compression type!");
        }
        this.compressionType = compressionType;
    }

    /**
     * El elegido, o null.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     */
    public String getCompressionType() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return this.compressionType;
    }

    /**
     * Olvida el tipo y la calidad.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es {@link #MODE_EXPLICIT}
     */
    public void unsetCompression() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        this.compressionType = null;
        this.compressionQuality = 1.0F;
    }

    /**
     * El nombre del tipo elegido, en el idioma que se pidio.
     *
     * <p>Esta implementacion devuelve el nombre tal cual: traducirlo pide un catalogo de textos que
     * solo el escritor concreto tiene.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es explicito o no hay tipo elegido
     */
    public String getLocalizedCompressionTypeName() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        String type = getCompressionType();
        if (type == null) {
            throw new IllegalStateException("No compression type set!");
        }
        return type;
    }

    /**
     * Si la compresion elegida conserva todo.
     *
     * <p>Devuelve true por omision: una implementacion que sepa comprimir con perdida <b>tiene</b> que
     * redefinirlo. Es el valor conservador -- decir "sin perdida" cuando hay perdida enganaria, y al
     * reves solo hace que alguien recomprima sin necesidad.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es explicito
     */
    public boolean isCompressionLossless() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return true;
    }

    /**
     * Fija la calidad, de 0 a 1. Ver la nota de la clase: 1 es la mejor.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es explicito
     * @throws IllegalArgumentException si esta fuera de rango
     */
    public void setCompressionQuality(float quality) {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        if (quality < 0 || quality > 1.0F) {
            throw new IllegalArgumentException("Quality out-of-bounds!");
        }
        this.compressionQuality = quality;
    }

    /**
     * La calidad.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es explicito
     */
    public float getCompressionQuality() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return this.compressionQuality;
    }

    /**
     * Cuantos bits por pixel daria esa calidad, o -1 si no se sabe.
     *
     * <p>Esta implementacion devuelve -1 siempre: estimarlo depende del codificador concreto. -1 es
     * el valor que la documentacion define para "no lo se".
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es explicito
     * @throws IllegalArgumentException si la calidad esta fuera de rango
     */
    public float getBitRate(float quality) {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        if (quality < 0.0F || quality > 1.0F) {
            throw new IllegalArgumentException("Quality out-of-bounds!");
        }
        return -1.0F;
    }

    /**
     * Como describir los tramos de calidad, o null si no hay descripciones.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es explicito
     */
    public String[] getCompressionQualityDescriptions() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return null;
    }

    /**
     * Los limites de esos tramos, o null.
     *
     * <p>Si hay {@code n} descripciones, hay {@code n + 1} limites: van de a pares con los tramos que
     * separan.
     *
     * @throws UnsupportedOperationException si no sabe comprimir
     * @throws IllegalStateException si el modo no es explicito
     */
    public float[] getCompressionQualityValues() {
        if (!canWriteCompressed()) {
            throw new UnsupportedOperationException("Compression not supported.");
        }
        if (getCompressionMode() != MODE_EXPLICIT) {
            throw new IllegalStateException("Compression mode not MODE_EXPLICIT!");
        }
        return null;
    }
}
