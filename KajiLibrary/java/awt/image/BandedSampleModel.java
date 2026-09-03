package java.awt.image;

/**
 * Un {@link ComponentSampleModel} con **cada banda en su propio banco**: el formato por planos,
 * RRR...GGG...BBB.
 *
 * <p>Es el complementario de {@link PixelInterleavedSampleModel}: alli las bandas de un pixel estan
 * juntas y aca estan en arreglos distintos. La diferencia se nota al procesar una sola banda --leer
 * el canal rojo entero es recorrer un arreglo seguido en vez de saltar de a tres-- y al agregar o
 * quitar una banda, que aca es agregar o quitar un banco.
 *
 * <p>Se implementa fijando los parametros de la formula general: `pixelStride` es 1 --las bandas
 * estan juntas dentro de su banco-- y `bankIndices` es 0, 1, 2, ... El resto lo hace la clase base,
 * y por eso esta clase es tan corta.
 */
public final class BandedSampleModel extends ComponentSampleModel {

    /** Un banco por banda, sin relleno de fila ni desplazamientos. */
    public BandedSampleModel(int dataType, int w, int h, int numBands) {
        super(dataType, w, h, 1, w, bancos(numBands), new int[numBands]);
    }

    /** Un banco por banda, con el paso de fila, los bancos y los desplazamientos dados. */
    public BandedSampleModel(int dataType, int w, int h, int scanlineStride, int[] bankIndices,
            int[] bandOffsets) {
        super(dataType, w, h, 1, scanlineStride, bankIndices, bandOffsets);
    }

    private static int[] bancos(int numBands) {
        int[] out = new int[numBands];
        for (int i = 0; i < numBands; i++) {
            out[i] = i;
        }
        return out;
    }

    /** Otro por planos del tamano pedido. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int[] indices = this.getBankIndices();
        int[] offsets = this.getBandOffsets();
        // Los desplazamientos se llevan a cero: con otro tamano, los viejos apuntarian a lugares
        // que en el buffer nuevo no significan lo mismo.
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = 0;
        }
        return new BandedSampleModel(this.dataType, w, h, w, indices, offsets);
    }

    /**
     * Un por-planos con solo esas bandas, sobre los mismos datos.
     *
     * @throws RasterFormatException si alguna banda no existe
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        int[] indices = new int[bands.length];
        int[] offsets = new int[bands.length];
        int[] misIndices = this.getBankIndices();
        int[] misOffsets = this.getBandOffsets();
        for (int i = 0; i < bands.length; i++) {
            if (bands[i] < 0 || bands[i] >= this.numBands) {
                throw new RasterFormatException("Band " + bands[i] + " does not exist");
            }
            indices[i] = misIndices[bands[i]];
            offsets[i] = misOffsets[bands[i]];
        }
        return new BandedSampleModel(this.dataType, this.width, this.height, this.getScanlineStride(),
                indices, offsets);
    }

    /**
     * Un buffer con un banco por banda.
     *
     * <p>Cada banco mide lo que ocupa la imagen mas su desplazamiento, y **no** se suman entre si:
     * son arreglos independientes. Es la diferencia con el intercalado, donde todo entra en uno.
     *
     * @throws IllegalArgumentException si el tipo de datos no es uno de los seis
     */
    public DataBuffer createDataBuffer() {
        int[] offsets = this.getBandOffsets();
        int max = 0;
        for (int i = 0; i < offsets.length; i++) {
            if (offsets[i] > max) {
                max = offsets[i];
            }
        }
        int size = (this.height - 1) * this.getScanlineStride() + this.width + max;
        int banks = this.numBanks;
        if (this.dataType == DataBuffer.TYPE_BYTE) {
            return new DataBufferByte(size, banks);
        }
        if (this.dataType == DataBuffer.TYPE_USHORT) {
            return new DataBufferUShort(size, banks);
        }
        if (this.dataType == DataBuffer.TYPE_SHORT) {
            return new DataBufferShort(size, banks);
        }
        if (this.dataType == DataBuffer.TYPE_INT) {
            return new DataBufferInt(size, banks);
        }
        if (this.dataType == DataBuffer.TYPE_FLOAT) {
            return new DataBufferFloat(size, banks);
        }
        if (this.dataType == DataBuffer.TYPE_DOUBLE) {
            return new DataBufferDouble(size, banks);
        }
        throw new IllegalArgumentException("Unsupported dataType: " + this.dataType);
    }

    public int hashCode() {
        return super.hashCode() ^ 0x42414e44;
    }
}
