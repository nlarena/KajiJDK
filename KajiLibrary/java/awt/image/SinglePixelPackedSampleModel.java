package java.awt.image;

/**
 * Un píxel entero en **un solo** elemento del buffer, con sus bandas en campos de bits.

 * <p>Es el modelo de una imagen de pantalla: un `int` por píxel con el alfa, el rojo, el verde y el
 * azul en sus ocho bits cada uno. Las bandas se declaran por sus **máscaras**, y de la máscara sale
 * todo lo demás — cuántos bits usa la banda y cuánto hay que correrla.
 *
 * <p>Que la máscara sea el parámetro, y no el par (desplazamiento, ancho), es lo que hace que
 * formatos irregulares se declaren igual de fácil: un 5-6-5 de 16 bits son las máscaras
 * `0xF800, 0x07E0, 0x001F` y no hace falta decir nada más.
 *
 * <p><strong>Las máscaras no se pueden solapar</strong> y el constructor no lo comprueba, igual que
 * el JDK: dos bandas sobre los mismos bits producirían una imagen donde escribir una cambia la
 * otra, y detectarlo costaría comparar todos los pares en un constructor que se llama por imagen.
 */
public class SinglePixelPackedSampleModel extends SampleModel {

    private final int[] bitMasks;
    private final int[] bitOffsets;
    private final int[] bitSizes;
    private final int scanlineStride;

    /** Un píxel por elemento, sin relleno de fila. */
    public SinglePixelPackedSampleModel(int dataType, int w, int h, int[] bitMasks) {
        this(dataType, w, h, w, bitMasks);
    }

    /**
     * Con el paso de fila dado.
     *
     * @throws IllegalArgumentException si el tipo no admite empaquetado --sólo `byte`, `ushort` e
     *     `int` lo hacen-- o si el paso de fila es negativo
     */
    public SinglePixelPackedSampleModel(int dataType, int w, int h, int scanlineStride,
            int[] bitMasks) {
        super(dataType, w, h, bitMasks.length);
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_USHORT
                && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Unsupported data type " + dataType);
        }
        if (scanlineStride < 0) {
            throw new IllegalArgumentException("Scanline stride must be >= 0");
        }
        this.scanlineStride = scanlineStride;
        this.bitMasks = new int[bitMasks.length];
        this.bitOffsets = new int[bitMasks.length];
        this.bitSizes = new int[bitMasks.length];
        for (int i = 0; i < bitMasks.length; i++) {
            int mascara = bitMasks[i];
            this.bitMasks[i] = mascara;
            // El desplazamiento es cuántos ceros hay a la derecha de la máscara, y el tamaño
            // cuántos unos tiene una vez corrida. Los dos salen de la misma pasada.
            int off = 0;
            int m = mascara;
            if (m != 0) {
                while ((m & 1) == 0) {
                    m = m >>> 1;
                    off = off + 1;
                }
            }
            int bits = 0;
            while ((m & 1) == 1) {
                m = m >>> 1;
                bits = bits + 1;
            }
            this.bitOffsets[i] = off;
            this.bitSizes[i] = bits;
        }
    }

    /** Siempre 1: el píxel entero entra en un elemento. */
    public int getNumDataElements() {
        return 1;
    }

    /** Otro igual del tamaño pedido. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        return new SinglePixelPackedSampleModel(this.dataType, w, h, w, this.getBitMasks());
    }

    /**
     * Un buffer del tamaño necesario.
     *
     * @throws IllegalArgumentException si el tipo no es `byte`, `ushort` ni `int`
     */
    public DataBuffer createDataBuffer() {
        int size = (this.height - 1) * this.scanlineStride + this.width;
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

    /** Cuántos bits usa cada banda, según su máscara. */
    public int[] getSampleSize() {
        int[] out = new int[this.bitSizes.length];
        System.arraycopy(this.bitSizes, 0, out, 0, this.bitSizes.length);
        return out;
    }

    /** Cuántos bits usa esa banda. */
    public int getSampleSize(int band) {
        return this.bitSizes[band];
    }

    /** El elemento del buffer donde está ese píxel. */
    public int getOffset(int x, int y) {
        return y * this.scanlineStride + x;
    }

    /** Cuántos bits hay que correr cada banda para llevarla a la derecha. */
    public int[] getBitOffsets() {
        int[] out = new int[this.bitOffsets.length];
        System.arraycopy(this.bitOffsets, 0, out, 0, this.bitOffsets.length);
        return out;
    }

    /** Las máscaras de cada banda. */
    public int[] getBitMasks() {
        int[] out = new int[this.bitMasks.length];
        System.arraycopy(this.bitMasks, 0, out, 0, this.bitMasks.length);
        return out;
    }

    /** El paso de fila. */
    public int getScanlineStride() {
        return this.scanlineStride;
    }

    /**
     * Uno con sólo esas bandas, sobre los mismos datos.
     *
     * @throws RasterFormatException si alguna banda no existe
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        int[] mascaras = new int[bands.length];
        for (int i = 0; i < bands.length; i++) {
            if (bands[i] < 0 || bands[i] >= this.numBands) {
                throw new RasterFormatException("Band " + bands[i] + " does not exist");
            }
            mascaras[i] = this.bitMasks[bands[i]];
        }
        return new SinglePixelPackedSampleModel(this.dataType, this.width, this.height,
                this.scanlineStride, mascaras);
    }

    /**
     * El elemento crudo del píxel: **un** valor con todas las bandas empaquetadas.
     *
     * @throws IllegalArgumentException si el tipo no es `byte`, `ushort` ni `int`
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        int v = data.getElem(this.getOffset(x, y));
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[1] : (byte[]) obj;
            out[0] = (byte) v;
            return out;
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            short[] out = obj == null ? new short[1] : (short[]) obj;
            out[0] = (short) v;
            return out;
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            int[] out = obj == null ? new int[1] : (int[]) obj;
            out[0] = v;
            return out;
        }
        throw new IllegalArgumentException("Unsupported data type " + this.dataType);
    }

    /** Escribe el elemento crudo del píxel. */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            data.setElem(this.getOffset(x, y), ((byte[]) obj)[0] & 0xFF);
            return;
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            data.setElem(this.getOffset(x, y), ((short[]) obj)[0] & 0xFFFF);
            return;
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            data.setElem(this.getOffset(x, y), ((int[]) obj)[0]);
            return;
        }
        throw new IllegalArgumentException("Unsupported data type " + this.dataType);
    }

    /**
     * Las bandas de un píxel, desempaquetadas.
     *
     * <p>Se redefine porque acá el píxel entero se lee de **una sola** vez y después se desarma;
     * la versión heredada leería el mismo elemento del buffer una vez por banda.
     */
    public int[] getPixel(int x, int y, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[this.numBands] : iArray;
        int v = data.getElem(this.getOffset(x, y));
        for (int i = 0; i < this.numBands; i++) {
            out[i] = (v & this.bitMasks[i]) >>> this.bitOffsets[i];
        }
        return out;
    }

    /** Como el anterior, para un rectángulo. */
    public int[] getPixels(int x, int y, int w, int h, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[w * h * this.numBands] : iArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                int v = data.getElem(this.getOffset(i, j));
                for (int b = 0; b < this.numBands; b++) {
                    out[k] = (v & this.bitMasks[b]) >>> this.bitOffsets[b];
                    k = k + 1;
                }
            }
        }
        return out;
    }

    public int getSample(int x, int y, int b, DataBuffer data) {
        return (data.getElem(this.getOffset(x, y)) & this.bitMasks[b]) >>> this.bitOffsets[b];
    }

    /** Los valores de una banda en un rectángulo. */
    public int[] getSamples(int x, int y, int w, int h, int b, int[] iArray, DataBuffer data) {
        int[] out = iArray == null ? new int[w * h] : iArray;
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                out[k] = this.getSample(i, j, b, data);
                k = k + 1;
            }
        }
        return out;
    }

    /**
     * Escribe todas las bandas de un píxel.
     *
     * <p>Se arma el valor entero y se escribe una vez. Cada banda se enmascara con la suya antes de
     * juntarla: un valor que se pase de su campo se recorta en vez de pisar la banda de al lado,
     * que es lo que pasaría sin el `&`.
     */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        int off = this.getOffset(x, y);
        int v = data.getElem(off);
        for (int i = 0; i < this.numBands; i++) {
            v = v & ~this.bitMasks[i];
            v = v | ((iArray[i] << this.bitOffsets[i]) & this.bitMasks[i]);
        }
        data.setElem(off, v);
    }

    /** Como el anterior, para un rectángulo. */
    public void setPixels(int x, int y, int w, int h, int[] iArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                int[] uno = new int[this.numBands];
                for (int b = 0; b < this.numBands; b++) {
                    uno[b] = iArray[k];
                    k = k + 1;
                }
                this.setPixel(i, j, uno, data);
            }
        }
    }

    public void setSample(int x, int y, int b, int s, DataBuffer data) {
        int off = this.getOffset(x, y);
        int v = data.getElem(off);
        v = v & ~this.bitMasks[b];
        v = v | ((s << this.bitOffsets[b]) & this.bitMasks[b]);
        data.setElem(off, v);
    }

    /** Escribe los valores de una banda en un rectángulo. */
    public void setSamples(int x, int y, int w, int h, int b, int[] iArray, DataBuffer data) {
        int k = 0;
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                this.setSample(i, j, b, iArray[k], data);
                k = k + 1;
            }
        }
    }

    /** Igualdad por tamaño, tipo, paso de fila y máscaras. */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        SinglePixelPackedSampleModel that = (SinglePixelPackedSampleModel) o;
        if (this.width != that.width || this.height != that.height
                || this.numBands != that.numBands || this.dataType != that.dataType
                || this.scanlineStride != that.scanlineStride) {
            return false;
        }
        for (int i = 0; i < this.bitMasks.length; i++) {
            if (this.bitMasks[i] != that.bitMasks[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = this.width;
        h = h * 31 + this.height;
        h = h * 31 + this.numBands;
        h = h * 31 + this.dataType;
        h = h * 31 + this.scanlineStride;
        for (int i = 0; i < this.bitMasks.length; i++) {
            h = h * 31 + this.bitMasks[i];
        }
        return h;
    }
}
