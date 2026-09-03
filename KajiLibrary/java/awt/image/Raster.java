package java.awt.image;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Píxeles con **domicilio**: un {@link SampleModel} más un {@link DataBuffer} más un rectángulo.
 *
 * <p>El modelo de muestras sabe cómo está guardado un píxel y el buffer tiene los números, pero
 * ninguno de los dos sabe *dónde* está la imagen. Eso lo agrega el ráster: sus coordenadas empiezan
 * en `(minX, minY)`, que no tiene por qué ser el origen.
 *
 * <p>De ahí sale el único concepto no obvio de la clase, `sampleModelTranslate`. Las coordenadas del
 * ráster y las del modelo de muestras son dos sistemas distintos, y la traducción es una resta:
 *
 * <pre>coordenada del modelo = coordenada del ráster - sampleModelTranslate</pre>
 *
 * <p>Sirve para que un **hijo** —un recorte— comparta los datos del padre en vez de copiarlos. El
 * hijo se declara en las coordenadas que quiera y su traducción absorbe la diferencia; los dos
 * rásters leen el mismo buffer. Un recorte, entonces, no cuesta memoria: es una ventana.
 *
 * <p>Esta clase es de **sólo lectura**. Los métodos que escriben están en {@link WritableRaster},
 * que hereda de ésta. La separación es real y no decorativa: `getParent()` de un ráster de sólo
 * lectura puede devolver un ráster que sí se puede escribir, y al revés no.
 *
 * <p>Los constructores son protegidos: un ráster se pide por los métodos estáticos `create*`, que
 * eligen el modelo de muestras adecuado a cada disposición.
 */
public class Raster {

    /** Cómo están dispuestos los píxeles. */
    protected SampleModel sampleModel;

    /** Dónde están los números. */
    protected DataBuffer dataBuffer;

    /** Coordenada X del ángulo superior izquierdo. */
    protected int minX;

    /** Coordenada Y del ángulo superior izquierdo. */
    protected int minY;

    /** Ancho, en píxeles. */
    protected int width;

    /** Alto, en píxeles. */
    protected int height;

    /** Lo que hay que restarle a una X del ráster para obtener la del modelo. */
    protected int sampleModelTranslateX;

    /** Lo que hay que restarle a una Y del ráster para obtener la del modelo. */
    protected int sampleModelTranslateY;

    /** Cuántas bandas tiene cada píxel. */
    protected int numBands;

    /** Cuántos elementos del buffer ocupa un píxel. */
    protected int numDataElements;

    /** El ráster del que éste es un recorte, o `null`. */
    protected Raster parent;

    /**
     * Un ráster con un buffer nuevo, del tamaño del modelo, ubicado en `origin`.
     *
     * @throws RasterFormatException si el tamaño resultante es vacío
     */
    protected Raster(SampleModel sampleModel, Point origin) {
        this(sampleModel, sampleModel.createDataBuffer(),
                new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()),
                origin, null);
    }

    /**
     * Un ráster sobre el buffer dado, del tamaño del modelo, ubicado en `origin`.
     *
     * @throws RasterFormatException si el tamaño resultante es vacío
     */
    protected Raster(SampleModel sampleModel, DataBuffer dataBuffer, Point origin) {
        this(sampleModel, dataBuffer,
                new Rectangle(origin.x, origin.y, sampleModel.getWidth(), sampleModel.getHeight()),
                origin, null);
    }

    /**
     * El constructor general: región, traducción y padre dados por separado.
     *
     * @throws NullPointerException si falta cualquiera de los cuatro primeros
     * @throws RasterFormatException si la región es vacía o si sus coordenadas se pasan de `int`
     */
    protected Raster(SampleModel sampleModel, DataBuffer dataBuffer, Rectangle aRegion,
            Point sampleModelTranslate, Raster parent) {
        if (sampleModel == null || dataBuffer == null || aRegion == null
                || sampleModelTranslate == null) {
            throw new NullPointerException("SampleModel, dataBuffer, aRegion and "
                    + "sampleModelTranslate cannot be null");
        }
        this.sampleModel = sampleModel;
        this.dataBuffer = dataBuffer;
        this.minX = aRegion.x;
        this.minY = aRegion.y;
        this.width = aRegion.width;
        this.height = aRegion.height;
        if (this.width <= 0 || this.height <= 0) {
            throw new RasterFormatException("negative or zero "
                    + (this.width <= 0 ? "width" : "height"));
        }
        // La suma se comprueba porque un rectangulo que se pasa de int daria un limite menor que su
        // propio origen, y todas las comprobaciones de borde de mas abajo pasarian al reves.
        if (this.minX + this.width < this.minX) {
            throw new RasterFormatException("overflow condition for X coordinates of Raster");
        }
        if (this.minY + this.height < this.minY) {
            throw new RasterFormatException("overflow condition for Y coordinates of Raster");
        }
        this.sampleModelTranslateX = sampleModelTranslate.x;
        this.sampleModelTranslateY = sampleModelTranslate.y;
        this.numBands = sampleModel.getNumBands();
        this.numDataElements = sampleModel.getNumDataElements();
        this.parent = parent;
    }

    // ---- fábricas ------------------------------------------------------------------------------

    /**
     * Un ráster intercalado con las bandas en orden y sin relleno.
     *
     * @throws IllegalArgumentException si el tipo no es `byte` ni `ushort`
     */
    public static WritableRaster createInterleavedRaster(int dataType, int w, int h, int bands,
            Point location) {
        int[] bandOffsets = new int[bands];
        for (int i = 0; i < bands; i++) {
            bandOffsets[i] = i;
        }
        return createInterleavedRaster(dataType, w, h, w * bands, bands, bandOffsets, location);
    }

    /**
     * Un ráster intercalado con la disposición dada, sobre un buffer nuevo.
     *
     * <p>Sólo `byte` y `ushort`: son los tipos en los que una imagen intercalada tiene sentido, y es
     * la misma restricción del JDK.
     *
     * @throws IllegalArgumentException si el tipo no es `byte` ni `ushort`, o si el tamaño es vacío
     */
    public static WritableRaster createInterleavedRaster(int dataType, int w, int h,
            int scanlineStride, int pixelStride, int[] bandOffsets, Point location) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") must be > 0");
        }
        int maxBandOff = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++) {
            if (bandOffsets[i] > maxBandOff) {
                maxBandOff = bandOffsets[i];
            }
        }
        // El ultimo elemento que se va a tocar es la banda mas lejana del ultimo pixel de la ultima
        // fila. El "+1" es porque lo anterior es un indice y esto un tamano.
        int size = maxBandOff + scanlineStride * (h - 1) + pixelStride * (w - 1) + 1;
        DataBuffer d;
        if (dataType == DataBuffer.TYPE_BYTE) {
            d = new DataBufferByte(size);
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            d = new DataBufferUShort(size);
        } else {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        return createInterleavedRaster(d, w, h, scanlineStride, pixelStride, bandOffsets, location);
    }

    /**
     * Un ráster intercalado sobre el buffer dado.
     *
     * @throws NullPointerException si el buffer es `null`
     * @throws RasterFormatException si el buffer tiene más de un banco
     * @throws IllegalArgumentException si el tipo no es `byte` ni `ushort`
     */
    public static WritableRaster createInterleavedRaster(DataBuffer dataBuffer, int w, int h,
            int scanlineStride, int pixelStride, int[] bandOffsets, Point location) {
        if (dataBuffer == null) {
            throw new NullPointerException("DataBuffer cannot be null");
        }
        Point donde = location;
        if (donde == null) {
            donde = new Point(0, 0);
        }
        int dataType = dataBuffer.getDataType();
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (dataBuffer.getNumBanks() != 1) {
            throw new RasterFormatException(
                    "DataBuffer for interleaved Rasters must only have 1 bank.");
        }
        PixelInterleavedSampleModel csm = new PixelInterleavedSampleModel(dataType, w, h,
                pixelStride, scanlineStride, bandOffsets);
        return new WritableRaster(csm, dataBuffer, donde);
    }

    /**
     * Un ráster por planos con una banda por banco y sin relleno.
     *
     * @throws ArrayIndexOutOfBoundsException si `bands` no es positivo
     */
    public static WritableRaster createBandedRaster(int dataType, int w, int h, int bands,
            Point location) {
        if (bands < 1) {
            throw new ArrayIndexOutOfBoundsException("Number of bands (" + bands
                    + ") must be greater than 0");
        }
        int[] bankIndices = new int[bands];
        int[] bandOffsets = new int[bands];
        for (int i = 0; i < bands; i++) {
            bankIndices[i] = i;
            bandOffsets[i] = 0;
        }
        return createBandedRaster(dataType, w, h, w, bankIndices, bandOffsets, location);
    }

    /**
     * Un ráster por planos con la disposición dada, sobre un buffer nuevo.
     *
     * @throws ArrayIndexOutOfBoundsException si falta alguno de los dos arreglos
     * @throws IllegalArgumentException si los arreglos no miden lo mismo, si el tamaño es vacío o si
     *     el tipo no es `byte`, `ushort` ni `int`
     */
    public static WritableRaster createBandedRaster(int dataType, int w, int h, int scanlineStride,
            int[] bankIndices, int[] bandOffsets, Point location) {
        if (bankIndices == null) {
            throw new ArrayIndexOutOfBoundsException("Bank indices array is null");
        }
        if (bandOffsets == null) {
            throw new ArrayIndexOutOfBoundsException("Band offsets array is null");
        }
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") must be > 0");
        }
        if (bankIndices.length != bandOffsets.length) {
            throw new IllegalArgumentException("bankIndices.length != bandOffsets.length");
        }
        if (bandOffsets.length < 1) {
            throw new IllegalArgumentException("Must have at least one band.");
        }
        int maxBank = bankIndices[0];
        int maxBandOff = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++) {
            if (bankIndices[i] > maxBank) {
                maxBank = bankIndices[i];
            }
            if (bandOffsets[i] > maxBandOff) {
                maxBandOff = bandOffsets[i];
            }
        }
        int banks = maxBank + 1;
        int size = maxBandOff + scanlineStride * (h - 1) + w;
        DataBuffer d;
        if (dataType == DataBuffer.TYPE_BYTE) {
            d = new DataBufferByte(size, banks);
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            d = new DataBufferUShort(size, banks);
        } else if (dataType == DataBuffer.TYPE_INT) {
            d = new DataBufferInt(size, banks);
        } else {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        return createBandedRaster(d, w, h, scanlineStride, bankIndices, bandOffsets, location);
    }

    /**
     * Un ráster por planos sobre el buffer dado.
     *
     * @throws NullPointerException si el buffer es `null`
     * @throws IllegalArgumentException si el tipo no es `byte`, `ushort` ni `int`
     */
    public static WritableRaster createBandedRaster(DataBuffer dataBuffer, int w, int h,
            int scanlineStride, int[] bankIndices, int[] bandOffsets, Point location) {
        if (dataBuffer == null) {
            throw new NullPointerException("DataBuffer cannot be null");
        }
        Point donde = location;
        if (donde == null) {
            donde = new Point(0, 0);
        }
        int dataType = dataBuffer.getDataType();
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        BandedSampleModel bsm = new BandedSampleModel(dataType, w, h, scanlineStride, bankIndices,
                bandOffsets);
        return new WritableRaster(bsm, dataBuffer, donde);
    }

    /**
     * Un ráster empaquetado con las bandas dadas por sus máscaras, sobre un buffer nuevo.
     *
     * @throws IllegalArgumentException si el tipo no es `byte`, `ushort` ni `int`
     */
    public static WritableRaster createPackedRaster(int dataType, int w, int h, int[] bandMasks,
            Point location) {
        DataBuffer d;
        if (dataType == DataBuffer.TYPE_BYTE) {
            d = new DataBufferByte(w * h);
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            d = new DataBufferUShort(w * h);
        } else if (dataType == DataBuffer.TYPE_INT) {
            d = new DataBufferInt(w * h);
        } else {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        return createPackedRaster(d, w, h, w, bandMasks, location);
    }

    /**
     * Un ráster empaquetado con `bands` bandas de `bitsPerBand` bits cada una.
     *
     * <p>Con más de una banda son campos de bits dentro de un píxel, y salen máscaras contiguas de
     * la más alta a la más baja. Con **una sola** banda son varios píxeles por elemento, que es el
     * otro empaquetado: ahí el modelo es {@link MultiPixelPackedSampleModel}.
     *
     * @throws IllegalArgumentException si algún parámetro no es positivo, si las bandas no entran en
     *     el tipo, o si el tipo no es `byte`, `ushort` ni `int`
     */
    public static WritableRaster createPackedRaster(int dataType, int w, int h, int bands,
            int bitsPerBand, Point location) {
        if (bands <= 0) {
            throw new IllegalArgumentException("Number of bands (" + bands
                    + ") must be greater than 0");
        }
        if (bitsPerBand <= 0) {
            throw new IllegalArgumentException("Bits per band (" + bitsPerBand
                    + ") must be greater than 0");
        }
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (bands != 1) {
            int[] masks = new int[bands];
            int mask = (1 << bitsPerBand) - 1;
            int shift = (bands - 1) * bitsPerBand;
            if (shift + bitsPerBand > DataBuffer.getDataTypeSize(dataType)) {
                throw new IllegalArgumentException("bitsPerBand(" + bitsPerBand
                        + ") * bands is greater than data type size.");
            }
            for (int i = 0; i < bands; i++) {
                masks[i] = mask << shift;
                shift = shift - bitsPerBand;
            }
            return createPackedRaster(dataType, w, h, masks, location);
        }
        // Una sola banda: varios pixeles por elemento. El tamano se redondea para arriba porque una
        // fila que no llene el ultimo elemento igual lo ocupa entero.
        int porElemento = DataBuffer.getDataTypeSize(dataType) / bitsPerBand;
        int elementosPorFila = (w + porElemento - 1) / porElemento;
        DataBuffer d;
        if (dataType == DataBuffer.TYPE_BYTE) {
            d = new DataBufferByte(elementosPorFila * h);
        } else if (dataType == DataBuffer.TYPE_USHORT) {
            d = new DataBufferUShort(elementosPorFila * h);
        } else {
            d = new DataBufferInt(elementosPorFila * h);
        }
        return createPackedRaster(d, w, h, bitsPerBand, location);
    }

    /**
     * Un ráster empaquetado por máscaras sobre el buffer dado.
     *
     * @throws NullPointerException si el buffer es `null`
     * @throws RasterFormatException si el buffer tiene más de un banco
     * @throws IllegalArgumentException si el tipo no es `byte`, `ushort` ni `int`
     */
    public static WritableRaster createPackedRaster(DataBuffer dataBuffer, int w, int h,
            int scanlineStride, int[] bandMasks, Point location) {
        if (dataBuffer == null) {
            throw new NullPointerException("DataBuffer cannot be null");
        }
        Point donde = location;
        if (donde == null) {
            donde = new Point(0, 0);
        }
        int dataType = dataBuffer.getDataType();
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (dataBuffer.getNumBanks() != 1) {
            throw new RasterFormatException(
                    "DataBuffer for packed Rasters must only have 1 bank.");
        }
        SinglePixelPackedSampleModel sppsm = new SinglePixelPackedSampleModel(dataType, w, h,
                scanlineStride, bandMasks);
        return new WritableRaster(sppsm, dataBuffer, donde);
    }

    /**
     * Un ráster de varios píxeles por elemento sobre el buffer dado.
     *
     * @throws NullPointerException si el buffer es `null`
     * @throws RasterFormatException si el buffer tiene más de un banco
     * @throws IllegalArgumentException si el tipo no es `byte`, `ushort` ni `int`
     */
    public static WritableRaster createPackedRaster(DataBuffer dataBuffer, int w, int h,
            int bitsPerPixel, Point location) {
        if (dataBuffer == null) {
            throw new NullPointerException("DataBuffer cannot be null");
        }
        Point donde = location;
        if (donde == null) {
            donde = new Point(0, 0);
        }
        int dataType = dataBuffer.getDataType();
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (dataBuffer.getNumBanks() != 1) {
            throw new RasterFormatException(
                    "DataBuffer for packed Rasters must only have 1 bank.");
        }
        MultiPixelPackedSampleModel mppsm = new MultiPixelPackedSampleModel(dataType, w, h,
                bitsPerPixel);
        return new WritableRaster(mppsm, dataBuffer, donde);
    }

    /**
     * Un ráster de sólo lectura sobre el modelo y el buffer dados.
     *
     * @throws NullPointerException si falta el modelo o el buffer
     */
    public static Raster createRaster(SampleModel sm, DataBuffer db, Point location) {
        if (sm == null || db == null) {
            throw new NullPointerException("SampleModel and DataBuffer cannot be null");
        }
        Point donde = location;
        if (donde == null) {
            donde = new Point(0, 0);
        }
        return new Raster(sm, db, donde);
    }

    /**
     * Un ráster escribible sobre el modelo dado, con un buffer nuevo.
     *
     * @throws NullPointerException si falta el modelo
     */
    public static WritableRaster createWritableRaster(SampleModel sm, Point location) {
        if (sm == null) {
            throw new NullPointerException("SampleModel cannot be null");
        }
        Point donde = location;
        if (donde == null) {
            donde = new Point(0, 0);
        }
        return new WritableRaster(sm, donde);
    }

    /**
     * Un ráster escribible sobre el modelo y el buffer dados.
     *
     * @throws NullPointerException si falta el modelo o el buffer
     */
    public static WritableRaster createWritableRaster(SampleModel sm, DataBuffer db,
            Point location) {
        if (sm == null || db == null) {
            throw new NullPointerException("SampleModel and DataBuffer cannot be null");
        }
        Point donde = location;
        if (donde == null) {
            donde = new Point(0, 0);
        }
        return new WritableRaster(sm, db, donde);
    }

    // ---- geometría -----------------------------------------------------------------------------

    /** El ráster del que éste es un recorte, o `null` si no lo es. */
    public Raster getParent() {
        return this.parent;
    }

    /** Lo que hay que restarle a una X del ráster para obtener la del modelo. */
    public final int getSampleModelTranslateX() {
        return this.sampleModelTranslateX;
    }

    /** Lo que hay que restarle a una Y del ráster para obtener la del modelo. */
    public final int getSampleModelTranslateY() {
        return this.sampleModelTranslateY;
    }

    /**
     * Otro ráster escribible con el mismo modelo, en el origen y con datos **propios**.
     *
     * <p>Comparte la disposición, no los píxeles.
     */
    public WritableRaster createCompatibleWritableRaster() {
        return new WritableRaster(this.sampleModel, new Point(0, 0));
    }

    /**
     * Como el anterior, del tamaño pedido.
     *
     * @throws RasterFormatException si el tamaño es vacío
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new RasterFormatException("negative " + (w <= 0 ? "width" : "height"));
        }
        SampleModel sm = this.sampleModel.createCompatibleSampleModel(w, h);
        return new WritableRaster(sm, new Point(0, 0));
    }

    /**
     * Como el anterior, del tamaño y en la posición pedidos.
     *
     * @throws RasterFormatException si el rectángulo es vacío
     */
    public WritableRaster createCompatibleWritableRaster(int x, int y, int w, int h) {
        WritableRaster ret = this.createCompatibleWritableRaster(w, h);
        return ret.createWritableChild(0, 0, w, h, x, y, null);
    }

    /**
     * Como el anterior, con el rectángulo dado.
     *
     * @throws NullPointerException si el rectángulo es `null`
     */
    public WritableRaster createCompatibleWritableRaster(Rectangle rect) {
        if (rect == null) {
            throw new NullPointerException("Rect cannot be null");
        }
        return this.createCompatibleWritableRaster(rect.x, rect.y, rect.width, rect.height);
    }

    /**
     * El mismo ráster mudado a otras coordenadas, **sobre los mismos datos**.
     *
     * @throws RasterFormatException si las coordenadas nuevas se pasan de `int`
     */
    public Raster createTranslatedChild(int childMinX, int childMinY) {
        return this.createChild(this.minX, this.minY, this.width, this.height, childMinX, childMinY,
                null);
    }

    /**
     * Un recorte sobre los **mismos datos**, opcionalmente con menos bandas.
     *
     * <p>`bandList` elige qué bandas ve el hijo y en qué orden; con `null` las ve todas.
     *
     * @throws RasterFormatException si el rectángulo pedido no cae dentro de éste, o si las
     *     coordenadas del hijo se pasan de `int`
     */
    public Raster createChild(int parentX, int parentY, int width, int height, int childMinX,
            int childMinY, int[] bandList) {
        if (parentX < this.minX) {
            throw new RasterFormatException("parentX lies outside raster");
        }
        if (parentY < this.minY) {
            throw new RasterFormatException("parentY lies outside raster");
        }
        if (parentX + width < parentX || parentX + width > this.minX + this.width) {
            throw new RasterFormatException("(parentX + width) is outside raster");
        }
        if (parentY + height < parentY || parentY + height > this.minY + this.height) {
            throw new RasterFormatException("(parentY + height) is outside raster");
        }
        SampleModel subSampleModel;
        if (bandList == null) {
            subSampleModel = this.sampleModel;
        } else {
            subSampleModel = this.sampleModel.createSubsetSampleModel(bandList);
        }
        // El hijo se declara donde le pidan; la traduccion absorbe la diferencia para que las dos
        // coordenadas sigan cayendo en el mismo elemento del buffer.
        int deltaX = childMinX - parentX;
        int deltaY = childMinY - parentY;
        return new Raster(subSampleModel, this.dataBuffer,
                new Rectangle(childMinX, childMinY, width, height),
                new Point(this.sampleModelTranslateX + deltaX,
                        this.sampleModelTranslateY + deltaY),
                this);
    }

    /** El rectángulo que ocupa. */
    public Rectangle getBounds() {
        return new Rectangle(this.minX, this.minY, this.width, this.height);
    }

    /** Coordenada X del ángulo superior izquierdo. */
    public final int getMinX() {
        return this.minX;
    }

    /** Coordenada Y del ángulo superior izquierdo. */
    public final int getMinY() {
        return this.minY;
    }

    /** Ancho, en píxeles. */
    public final int getWidth() {
        return this.width;
    }

    /** Alto, en píxeles. */
    public final int getHeight() {
        return this.height;
    }

    /** Cuántas bandas tiene cada píxel. */
    public final int getNumBands() {
        return this.numBands;
    }

    /** Cuántos elementos del buffer ocupa un píxel. */
    public final int getNumDataElements() {
        return this.numDataElements;
    }

    /** El tipo con el que se transfiere un píxel crudo. */
    public final int getTransferType() {
        return this.sampleModel.getTransferType();
    }

    /**
     * El buffer con los números.
     *
     * <p>Es el de verdad, no una copia: escribir en él cambia la imagen, y la de todos los rásters
     * que lo compartan.
     */
    public DataBuffer getDataBuffer() {
        return this.dataBuffer;
    }

    /** Cómo están dispuestos los píxeles. */
    public SampleModel getSampleModel() {
        return this.sampleModel;
    }

    // ---- lectura -------------------------------------------------------------------------------

    /**
     * Comprueba que un punto caiga adentro.
     *
     * @throws ArrayIndexOutOfBoundsException si no cae
     */
    private void checkPoint(int x, int y) {
        if (x < this.minX || y < this.minY || x >= this.minX + this.width
                || y >= this.minY + this.height) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    /**
     * Comprueba que un rectángulo caiga adentro.
     *
     * @throws ArrayIndexOutOfBoundsException si no cae
     */
    private void checkRect(int x, int y, int w, int h) {
        if (x < this.minX || y < this.minY || x + w > this.minX + this.width
                || y + h > this.minY + this.height || x + w < x || y + h < y) {
            throw new ArrayIndexOutOfBoundsException("Coordinate out of bounds!");
        }
    }

    /**
     * El píxel crudo, sin desempaquetar, en el tipo de {@link #getTransferType}.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public Object getDataElements(int x, int y, Object outData) {
        this.checkPoint(x, y);
        return this.sampleModel.getDataElements(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, outData, this.dataBuffer);
    }

    /**
     * Los píxeles crudos de un rectángulo.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public Object getDataElements(int x, int y, int w, int h, Object outData) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getDataElements(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, outData, this.dataBuffer);
    }

    /**
     * Las bandas de un píxel.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public int[] getPixel(int x, int y, int[] iArray) {
        this.checkPoint(x, y);
        return this.sampleModel.getPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, iArray, this.dataBuffer);
    }

    /**
     * Las bandas de un píxel, en `float`.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public float[] getPixel(int x, int y, float[] fArray) {
        this.checkPoint(x, y);
        return this.sampleModel.getPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, fArray, this.dataBuffer);
    }

    /**
     * Las bandas de un píxel, en `double`.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public double[] getPixel(int x, int y, double[] dArray) {
        this.checkPoint(x, y);
        return this.sampleModel.getPixel(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, dArray, this.dataBuffer);
    }

    /**
     * Los píxeles de un rectángulo, banda por banda.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public int[] getPixels(int x, int y, int w, int h, int[] iArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, iArray, this.dataBuffer);
    }

    /**
     * Como el anterior, en `float`.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public float[] getPixels(int x, int y, int w, int h, float[] fArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, fArray, this.dataBuffer);
    }

    /**
     * Como el anterior, en `double`.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public double[] getPixels(int x, int y, int w, int h, double[] dArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getPixels(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, dArray, this.dataBuffer);
    }

    /**
     * Una banda de un píxel.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public int getSample(int x, int y, int b) {
        this.checkPoint(x, y);
        return this.sampleModel.getSample(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, this.dataBuffer);
    }

    /**
     * Una banda de un píxel, en `float`.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public float getSampleFloat(int x, int y, int b) {
        this.checkPoint(x, y);
        return this.sampleModel.getSampleFloat(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, this.dataBuffer);
    }

    /**
     * Una banda de un píxel, en `double`.
     *
     * @throws ArrayIndexOutOfBoundsException si el punto cae afuera
     */
    public double getSampleDouble(int x, int y, int b) {
        this.checkPoint(x, y);
        return this.sampleModel.getSampleDouble(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, b, this.dataBuffer);
    }

    /**
     * Una banda en un rectángulo.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public int[] getSamples(int x, int y, int w, int h, int b, int[] iArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, iArray, this.dataBuffer);
    }

    /**
     * Como el anterior, en `float`.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public float[] getSamples(int x, int y, int w, int h, int b, float[] fArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, fArray, this.dataBuffer);
    }

    /**
     * Como el anterior, en `double`.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale
     */
    public double[] getSamples(int x, int y, int w, int h, int b, double[] dArray) {
        this.checkRect(x, y, w, h);
        return this.sampleModel.getSamples(x - this.sampleModelTranslateX,
                y - this.sampleModelTranslateY, w, h, b, dArray, this.dataBuffer);
    }
}
