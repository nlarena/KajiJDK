package java.awt.image;

/**
 * El modelo donde **cada banda es un número entero del buffer**, sin empaquetar.
 *
 * <p>Es el más general de los cinco y el que cubre casi todo lo que no es un píxel empaquetado. Su
 * idea completa cabe en una fórmula: el elemento de la banda `b` del píxel `(x,y)` está en
 *
 * <pre>y * scanlineStride + x * pixelStride + bandOffsets[b]</pre>
 *
 * <p>dentro del banco `bankIndices[b]`. Los cuatro parámetros de esa cuenta son lo que hace que la
 * misma clase describa formatos muy distintos:
 *
 * <ul>
 * <li><b>Intercalado</b> (RGBRGBRGB…): un banco, `pixelStride` 3, `bandOffsets` {0,1,2}.</li>
 * <li><b>Por planos</b> (RRR…GGG…BBB…): tres bancos, `pixelStride` 1, `bandOffsets` {0,0,0}.</li>
 * <li><b>Con relleno de fila</b>: `scanlineStride` mayor que `ancho * pixelStride`, que es lo que
 *     pasa cuando cada fila se alinea a una frontera.</li>
 * <li><b>Un recorte</b>: los mismos datos con `bandOffsets` corridos, sin copiar nada.</li>
 * <li><b>Con las bandas dadas vuelta</b> (BGR): `bandOffsets` {2,1,0}.</li>
 * </ul>
 *
 * <p>Que todo eso salga de una sola fórmula es la razón de que esta clase exista, y también la
 * razón de que sus cinco campos protegidos sean parte del contrato: una subclase los necesita para
 * hacer la misma cuenta más rápido.
 */
public class ComponentSampleModel extends SampleModel {

    /** Dónde empieza cada banda dentro de su banco. */
    protected int[] bandOffsets;

    /** En qué banco está cada banda. */
    protected int[] bankIndices;

    /** Cuántas bandas hay. */
    protected int numBands;

    /** Cuántos bancos usa. */
    protected int numBanks;

    /** Cuántos elementos hay entre el comienzo de una fila y el de la siguiente. */
    protected int scanlineStride;

    /** Cuántos elementos hay entre un píxel y el de al lado. */
    protected int pixelStride;

    /**
     * Todas las bandas en el banco 0.
     *
     * @throws IllegalArgumentException si los pasos son negativos o no hay desplazamientos
     */
    public ComponentSampleModel(int dataType, int w, int h, int pixelStride, int scanlineStride,
            int[] bandOffsets) {
        super(dataType, w, h, bandOffsets.length);
        if (pixelStride < 0) {
            throw new IllegalArgumentException("Pixel stride must be >= 0");
        }
        if (scanlineStride < 0) {
            throw new IllegalArgumentException("Scanline stride must be >= 0");
        }
        this.pixelStride = pixelStride;
        this.scanlineStride = scanlineStride;
        this.bandOffsets = copiar(bandOffsets);
        this.numBands = bandOffsets.length;
        this.numBanks = 1;
        this.bankIndices = new int[this.numBands];
        for (int i = 0; i < this.numBands; i++) {
            this.bankIndices[i] = 0;
        }
    }

    /**
     * Cada banda en el banco que se indique.
     *
     * @throws IllegalArgumentException si los pasos son negativos, o si hay distinta cantidad de
     *     bancos que de desplazamientos
     */
    public ComponentSampleModel(int dataType, int w, int h, int pixelStride, int scanlineStride,
            int[] bankIndices, int[] bandOffsets) {
        super(dataType, w, h, bandOffsets.length);
        if (pixelStride < 0) {
            throw new IllegalArgumentException("Pixel stride must be >= 0");
        }
        if (scanlineStride < 0) {
            throw new IllegalArgumentException("Scanline stride must be >= 0");
        }
        if (bankIndices.length != bandOffsets.length) {
            throw new IllegalArgumentException(
                    "Length of bandOffsets must equal length of bankIndices");
        }
        this.pixelStride = pixelStride;
        this.scanlineStride = scanlineStride;
        this.bandOffsets = copiar(bandOffsets);
        this.bankIndices = copiar(bankIndices);
        this.numBands = bandOffsets.length;
        int max = 0;
        for (int i = 0; i < bankIndices.length; i++) {
            if (bankIndices[i] < 0) {
                throw new IllegalArgumentException("Index of bank must be >= 0");
            }
            if (bankIndices[i] > max) {
                max = bankIndices[i];
            }
        }
        this.numBanks = max + 1;
    }

    private static int[] copiar(int[] src) {
        int[] out = new int[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    /** Un modelo igual pero de otro tamaño. El paso de fila se recalcula al ancho nuevo. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int[] offsets = copiar(this.bandOffsets);
        return new ComponentSampleModel(this.dataType, w, h, this.pixelStride,
                this.pixelStride * w, this.bankIndices, offsets);
    }

    /**
     * Un modelo con sólo esas bandas, **sobre los mismos datos**.
     *
     * @throws RasterFormatException si alguna banda no existe
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        int[] indices = new int[bands.length];
        int[] offsets = new int[bands.length];
        for (int i = 0; i < bands.length; i++) {
            if (bands[i] < 0 || bands[i] >= this.numBands) {
                throw new RasterFormatException("Band " + bands[i] + " does not exist");
            }
            indices[i] = this.bankIndices[bands[i]];
            offsets[i] = this.bandOffsets[bands[i]];
        }
        return new ComponentSampleModel(this.dataType, this.width, this.height, this.pixelStride,
                this.scanlineStride, indices, offsets);
    }

    /**
     * Un buffer del tamaño que este modelo necesita.
     *
     * <p>El tamaño no es `ancho * alto * bandas`: es lo que ocupa la última fila **más** el
     * desplazamiento más grande, porque con relleno de fila o con desplazamientos corridos hay
     * elementos que el modelo nunca toca y que igual tienen que existir.
     *
     * @throws IllegalArgumentException si el tipo de datos no es uno de los seis
     */
    public DataBuffer createDataBuffer() {
        int max = 0;
        for (int i = 0; i < this.bandOffsets.length; i++) {
            if (this.bandOffsets[i] > max) {
                max = this.bandOffsets[i];
            }
        }
        int size = (this.height - 1) * this.scanlineStride
                + (this.width - 1) * this.pixelStride + max + 1;
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            return new DataBufferByte(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            return new DataBufferUShort(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_SHORT) {
            return new DataBufferShort(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            return new DataBufferInt(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_FLOAT) {
            return new DataBufferFloat(size, this.numBanks);
        }
        if (this.dataType == DataBuffer.TYPE_DOUBLE) {
            return new DataBufferDouble(size, this.numBanks);
        }
        throw new IllegalArgumentException("Unsupported dataType: " + this.dataType);
    }

    /** El desplazamiento de la banda 0 de ese píxel. Ver la fórmula de la clase. */
    public int getOffset(int x, int y) {
        return y * this.scanlineStride + x * this.pixelStride + this.bandOffsets[0];
    }

    /** El desplazamiento de esa banda de ese píxel. */
    public int getOffset(int x, int y, int b) {
        return y * this.scanlineStride + x * this.pixelStride + this.bandOffsets[b];
    }

    /** Los bits de cada banda: los del tipo del buffer, porque acá nada se empaqueta. */
    public final int[] getSampleSize() {
        int bits = DataBuffer.getDataTypeSize(this.dataType);
        int[] out = new int[this.numBands];
        for (int i = 0; i < this.numBands; i++) {
            out[i] = bits;
        }
        return out;
    }

    /** Los bits de esa banda. */
    public final int getSampleSize(int band) {
        return DataBuffer.getDataTypeSize(this.dataType);
    }

    /** En qué banco está cada banda. */
    public final int[] getBankIndices() {
        return copiar(this.bankIndices);
    }

    /** Dónde empieza cada banda. */
    public final int[] getBandOffsets() {
        return copiar(this.bandOffsets);
    }

    /** El paso de fila. */
    public final int getScanlineStride() {
        return this.scanlineStride;
    }

    /** El paso de píxel. */
    public final int getPixelStride() {
        return this.pixelStride;
    }

    /** Un elemento por banda: acá no hay empaquetado. */
    public final int getNumDataElements() {
        return this.numBands;
    }

    /**
     * La representación cruda de un píxel: un elemento por banda, en el tipo del buffer.
     *
     * @throws IllegalArgumentException si el tipo de datos no es uno de los seis
     */
    public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
        int tipo = this.getTransferType();
        if (tipo == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[this.numBands] : (byte[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = (byte) data.getElem(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        if (tipo == DataBuffer.TYPE_USHORT || tipo == DataBuffer.TYPE_SHORT) {
            short[] out = obj == null ? new short[this.numBands] : (short[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = (short) data.getElem(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        if (tipo == DataBuffer.TYPE_INT) {
            int[] out = obj == null ? new int[this.numBands] : (int[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = data.getElem(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        if (tipo == DataBuffer.TYPE_FLOAT) {
            float[] out = obj == null ? new float[this.numBands] : (float[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = data.getElemFloat(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        if (tipo == DataBuffer.TYPE_DOUBLE) {
            double[] out = obj == null ? new double[this.numBands] : (double[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                out[i] = data.getElemDouble(this.bankIndices[i], this.getOffset(x, y, i));
            }
            return out;
        }
        throw new IllegalArgumentException("Unsupported type: " + tipo);
    }

    /**
     * Escribe la representación cruda de un píxel.
     *
     * @throws IllegalArgumentException si el tipo de datos no es uno de los seis
     */
    public void setDataElements(int x, int y, Object obj, DataBuffer data) {
        int tipo = this.getTransferType();
        if (tipo == DataBuffer.TYPE_BYTE) {
            byte[] src = (byte[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElem(this.bankIndices[i], this.getOffset(x, y, i), src[i] & 0xFF);
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_USHORT || tipo == DataBuffer.TYPE_SHORT) {
            short[] src = (short[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElem(this.bankIndices[i], this.getOffset(x, y, i), src[i] & 0xFFFF);
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_INT) {
            int[] src = (int[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElem(this.bankIndices[i], this.getOffset(x, y, i), src[i]);
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_FLOAT) {
            float[] src = (float[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElemFloat(this.bankIndices[i], this.getOffset(x, y, i), src[i]);
            }
            return;
        }
        if (tipo == DataBuffer.TYPE_DOUBLE) {
            double[] src = (double[]) obj;
            for (int i = 0; i < this.numBands; i++) {
                data.setElemDouble(this.bankIndices[i], this.getOffset(x, y, i), src[i]);
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported type: " + tipo);
    }

    public int getSample(int x, int y, int b, DataBuffer data) {
        return data.getElem(this.bankIndices[b], this.getOffset(x, y, b));
    }

    public float getSampleFloat(int x, int y, int b, DataBuffer data) {
        return data.getElemFloat(this.bankIndices[b], this.getOffset(x, y, b));
    }

    public double getSampleDouble(int x, int y, int b, DataBuffer data) {
        return data.getElemDouble(this.bankIndices[b], this.getOffset(x, y, b));
    }

    /** Escribe todas las bandas de un píxel. */
    public void setPixel(int x, int y, int[] iArray, DataBuffer data) {
        for (int i = 0; i < this.numBands; i++) {
            data.setElem(this.bankIndices[i], this.getOffset(x, y, i), iArray[i]);
        }
    }

    public void setSample(int x, int y, int b, int s, DataBuffer data) {
        data.setElem(this.bankIndices[b], this.getOffset(x, y, b), s);
    }

    public void setSample(int x, int y, int b, float s, DataBuffer data) {
        data.setElemFloat(this.bankIndices[b], this.getOffset(x, y, b), s);
    }

    public void setSample(int x, int y, int b, double s, DataBuffer data) {
        data.setElemDouble(this.bankIndices[b], this.getOffset(x, y, b), s);
    }

    /**
     * Igualdad por **todo** lo que define el modelo, la clase incluida.
     *
     * <p>La comprobación de clase exacta no es pereza: un {@link BandedSampleModel} y un
     * `ComponentSampleModel` con los mismos números describen el mismo formato pero se comportan
     * distinto en `createCompatibleSampleModel`, así que tratarlos como iguales haría que una copia
     * saliera con otro formato.
     */
    public boolean equals(Object o) {
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        ComponentSampleModel that = (ComponentSampleModel) o;
        if (this.width != that.width || this.height != that.height
                || this.numBands != that.numBands || this.dataType != that.dataType
                || this.pixelStride != that.pixelStride
                || this.scanlineStride != that.scanlineStride
                || this.numBanks != that.numBanks) {
            return false;
        }
        for (int i = 0; i < this.numBands; i++) {
            if (this.bandOffsets[i] != that.bandOffsets[i]
                    || this.bankIndices[i] != that.bankIndices[i]) {
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
        h = h * 31 + this.pixelStride;
        h = h * 31 + this.scanlineStride;
        for (int i = 0; i < this.numBands; i++) {
            h = h * 31 + this.bandOffsets[i];
            h = h * 31 + this.bankIndices[i];
        }
        return h;
    }
}
