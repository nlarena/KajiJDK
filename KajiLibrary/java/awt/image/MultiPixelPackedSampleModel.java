package java.awt.image;

/**
 * Varios píxeles dentro de **un** elemento del buffer: el modelo de las imágenes de 1, 2 y 4 bits.
 *
 * <p>Es el inverso de {@link SinglePixelPackedSampleModel}, donde un píxel ocupa un elemento
 * entero. Acá un `byte` puede contener ocho píxeles de un bit — un mapa en blanco y negro — o dos
 * de cuatro. Tiene **una sola banda** por construcción: si hubiera más de una no serían píxeles
 * empaquetados sino campos de bits, que es el otro modelo.
 *
 * <p>La cuenta es la misma dos veces, una para el elemento y otra para el bit dentro de él:
 *
 * <pre>bit = x * pixelBitStride + dataBitOffset
 * elemento = y * scanlineStride + bit / bitsPorElemento
 * corrimiento = bit % bitsPorElemento</pre>
 *
 * <p>El primer píxel se cuenta **desde la izquierda**, o sea desde el bit más significativo: en un
 * byte con cuatro píxeles de dos bits, el píxel 0 está en los bits 7-6. Es lo que dice el formato y
 * es lo contrario de lo que uno escribiría por reflejo.
 *
 * <p>`dataBitOffset` corre el comienzo de la primera fila. Sirve para describir un recorte cuyo
 * borde izquierdo cae en medio de un elemento, sin tener que copiar la imagen.
 */
public class MultiPixelPackedSampleModel extends SampleModel {

    private final int pixelBitStride;
    private final int scanlineStride;
    private final int dataBitOffset;
    private final int dataElementSize;
    private final int bitMask;

    /**
     * Con el paso de fila mínimo y sin desplazamiento inicial.
     *
     * @throws IllegalArgumentException si el tipo no admite empaquetado
     */
    public MultiPixelPackedSampleModel(int dataType, int w, int h, int numberOfBits) {
        this(dataType, w, h, numberOfBits,
                (w * numberOfBits + DataBuffer.getDataTypeSize(dataType) - 1)
                        / DataBuffer.getDataTypeSize(dataType),
                0);
    }

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si el tipo no es `byte`, `ushort` ni `int`, si los bits por
     *     píxel no dividen al tamaño del elemento, o si algún parámetro es negativo
     */
    public MultiPixelPackedSampleModel(int dataType, int w, int h, int numberOfBits,
            int scanlineStride, int dataBitOffset) {
        super(dataType, w, h, 1);
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        this.dataElementSize = DataBuffer.getDataTypeSize(dataType);
        if (numberOfBits <= 0 || numberOfBits > this.dataElementSize) {
            throw new RasterFormatException("Number of bits must be > 0 and <= "
                    + this.dataElementSize);
        }
        // Que un pixel no cruce la frontera de un elemento es lo que permite leerlo con un solo
        // acceso. Sin esa condicion habria que juntar dos elementos, y el formato no lo contempla.
        if (this.dataElementSize % numberOfBits != 0) {
            throw new RasterFormatException("MultiPixelPackedSampleModel does not allow pixels to "
                    + "span data element boundaries");
        }
        if (scanlineStride < 0 || dataBitOffset < 0) {
            throw new IllegalArgumentException("Scanline stride and data bit offset must be >= 0");
        }
        this.pixelBitStride = numberOfBits;
        this.scanlineStride = scanlineStride;
        this.dataBitOffset = dataBitOffset;
        this.bitMask = (1 << numberOfBits) - 1;
    }

    /** Otro igual del tamaño pedido, sin desplazamiento inicial. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        return new MultiPixelPackedSampleModel(this.dataType, w, h, this.pixelBitStride);
    }

    /**
     * Un buffer del tamaño necesario.
     *
     * @throws IllegalArgumentException si el tipo no es `byte`, `ushort` ni `int`
     */
    public DataBuffer createDataBuffer() {
        int size = (this.scanlineStride * (this.height - 1))
                + ((this.dataBitOffset + this.width * this.pixelBitStride
                        + this.dataElementSize - 1) / this.dataElementSize);
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            return new DataBufferByte(size);
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            return new DataBufferUShort(size);
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            return new DataBufferInt(size);
        }
        throw new IllegalArgumentException("Unsupported data type " + this.dataType);
    }

    /** Siempre 1. */
    public int getNumDataElements() {
        return 1;
    }

    /** Los bits de la única banda. */
    public int[] getSampleSize() {
        return new int[] { this.pixelBitStride };
    }

    /** Los bits de esa banda. */
    public int getSampleSize(int band) {
        return this.pixelBitStride;
    }

    /** El elemento del buffer donde está ese píxel. */
    public int getOffset(int x, int y) {
        return y * this.scanlineStride
                + (x * this.pixelBitStride + this.dataBitOffset) / this.dataElementSize;
    }

    /** El bit dentro del elemento donde empieza ese píxel, contado desde la izquierda. */
    public int getBitOffset(int x) {
        return (x * this.pixelBitStride + this.dataBitOffset) % this.dataElementSize;
    }

    /** El paso de fila, en elementos. */
    public int getScanlineStride() {
        return this.scanlineStride;
    }

    /** Cuántos bits ocupa un píxel. */
    public int getPixelBitStride() {
        return this.pixelBitStride;
    }

    /** Cuántos bits se saltean al principio. */
    public int getDataBitOffset() {
        return this.dataBitOffset;
    }

    /**
     * El tipo con el que se transfiere un píxel.
     *
     * <p>No es el del buffer: acá lo que viaja es **un píxel**, que entra en el tipo más chico que
     * lo contenga. Una imagen de un bit guardada en `int` transfiere en `byte`.
     */
    public int getTransferType() {
        if (this.pixelBitStride > 16) {
            return DataBuffer.TYPE_INT;
        }
        if (this.pixelBitStride > 8) {
            return DataBuffer.TYPE_USHORT;
        }
        return DataBuffer.TYPE_BYTE;
    }

    /**
     * Uno con esa banda.
     *
     * @throws RasterFormatException si se pide más de una banda, o una que no sea la 0
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        if (bands != null && (bands.length != 1 || bands[0] != 0)) {
            throw new RasterFormatException("MultiPixelPackedSampleModel has only one band");
        }
        return this.createCompatibleSampleModel(this.width, this.height);
    }

    public int getSample(int x, int y, int b, DataBuffer data) {
        // El corrimiento se cuenta desde la izquierda: el pixel 0 esta en los bits mas altos.
        int corrimiento = this.dataElementSize - this.getBitOffset(x) - this.pixelBitStride;
        return (data.getElem(this.getOffset(x, y)) >> corrimiento) & this.bitMask;
    }

    public void setSample(int x, int y, int b, int s, DataBuffer data) {
        int off = this.getOffset(x, y);
        int corrimiento = this.dataElementSize - this.getBitOffset(x) - this.pixelBitStride;
        int v = data.getElem(off);
        v = v & ~(this.bitMask << corrimiento);
        v = v | ((s & this.bitMask) << corrimiento);
        data.setElem(off, v);
    }

    /**
     * El píxel crudo, en el tipo de {@link #getTransferType}.
     *
     * <p>Ya viene **desempaquetado**: un píxel de dos bits llega como un byte con valor 0..3, no
     * como el byte del buffer con los otros tres píxeles adentro. Es la diferencia con
     * {@link SinglePixelPackedSampleModel}, donde el elemento crudo es el píxel entero empaquetado.
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        int v = this.getSample(x, y, 0, data);
        int tipo = this.getTransferType();
        if (tipo == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[1] : (byte[]) obj;
            out[0] = (byte) v;
            return out;
        }
        if (tipo == DataBuffer.TYPE_USHORT) {
            short[] out = obj == null ? new short[1] : (short[]) obj;
            out[0] = (short) v;
            return out;
        }
        int[] out = obj == null ? new int[1] : (int[]) obj;
        out[0] = v;
        return out;
    }

    /** Escribe el píxel crudo. */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        int tipo = this.getTransferType();
        int v;
        if (tipo == DataBuffer.TYPE_BYTE) {
            v = ((byte[]) obj)[0] & 0xFF;
        } else if (tipo == DataBuffer.TYPE_USHORT) {
            v = ((short[]) obj)[0] & 0xFFFF;
        } else {
            v = ((int[]) obj)[0];
        }
        this.setSample(x, y, 0, v, data);
    }

    /** La única banda del píxel. */
    public int[] getPixel(int x, int y, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[1] : iArray;
        out[0] = this.getSample(x, y, 0, data);
        return out;
    }

    /** Escribe la única banda del píxel. */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        this.setSample(x, y, 0, iArray[0], data);
    }

    /** Igualdad por tamaño, tipo y los tres parámetros de empaquetado. */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        MultiPixelPackedSampleModel that = (MultiPixelPackedSampleModel) o;
        return this.width == that.width && this.height == that.height
                && this.numBands == that.numBands && this.dataType == that.dataType
                && this.pixelBitStride == that.pixelBitStride
                && this.bitMask == that.bitMask
                && this.scanlineStride == that.scanlineStride
                && this.dataBitOffset == that.dataBitOffset;
    }

    public int hashCode() {
        int h = this.width;
        h = h * 31 + this.height;
        h = h * 31 + this.numBands;
        h = h * 31 + this.dataType;
        h = h * 31 + this.pixelBitStride;
        h = h * 31 + this.bitMask;
        h = h * 31 + this.scanlineStride;
        h = h * 31 + this.dataBitOffset;
        return h;
    }
}
