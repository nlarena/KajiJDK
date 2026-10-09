package java.awt.image;

/**
 * A {@link ComponentSampleModel} with **each band in its own bank**: the format by planes,
 * RRR...GGG...BBB.
 *
 * <p>It is the complement of {@link PixelInterleavedSampleModel}: there the bands of a pixel are
 * together and here they are in different arrays. The difference shows when processing a single
 * band --reading the whole red channel is walking one array in a row instead of jumping by three--
 * and when adding or removing a band, which here is adding or removing a bank.
 *
 * <p>It is implemented by fixing the parameters of the general formula: `pixelStride` is 1 --the
 * bands are together inside their bank-- and `bankIndices` is 0, 1, 2, ... The rest is done by the
 * base class, and that is why this class is so short.
 */
public final class BandedSampleModel extends ComponentSampleModel {

    /** One bank per band, with no row padding and no offsets. */
    public BandedSampleModel(int dataType, int w, int h, int numBands) {
        super(dataType, w, h, 1, w, bankSeries(numBands), new int[numBands]);
    }

    /** One bank per band, with the row stride, the banks and the offsets given. */
    public BandedSampleModel(int dataType, int w, int h, int scanlineStride, int[] bankIndices,
            int[] bandOffsets) {
        super(dataType, w, h, 1, scanlineStride, bankIndices, bandOffsets);
    }

    private static int[] bankSeries(int numBands) {
        int[] out = new int[numBands];
        for (int i = 0; i < numBands; i++) {
            out[i] = i;
        }
        return out;
    }

    /** Another one by planes of the size asked for. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int[] indices = this.getBankIndices();
        int[] offsets = this.getBandOffsets();
        // The offsets are taken to zero: with another size, the old ones would point at places that
        // in the new buffer do not mean the same thing.
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = 0;
        }
        return new BandedSampleModel(this.dataType, w, h, w, indices, offsets);
    }

    /**
     * One by planes with only those bands, over the same data.
     *
     * @throws RasterFormatException if some band does not exist
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
     * A buffer with one bank per band.
     *
     * <p>Each bank measures what the image takes plus its offset, and they are **not** added up:
     * they are independent arrays. That is the difference from the interleaved one, where
     * everything fits in a single bank.
     *
     * @throws IllegalArgumentException if the data type is not one of the six
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
