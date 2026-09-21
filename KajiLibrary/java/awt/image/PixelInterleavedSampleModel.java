package java.awt.image;

/**
 * A {@link ComponentSampleModel} with **every band in the same bank**: the interleaved format,
 * RGBRGBRGB.
 *
 * <p>It adds no data at all: it is the same model with a restriction. That it exists as a class of
 * its own serves two concrete purposes. One, the constructor checks the restriction instead of
 * letting a model be built that says it is interleaved and is not. Two --and it is the one that
 * matters--, `createCompatibleSampleModel` returns another interleaved one instead of a generic
 * component one, so copying an image keeps its format.
 */
public class PixelInterleavedSampleModel extends ComponentSampleModel {

    /**
     * @throws RasterFormatException if the band offsets do not fit inside one pixel, which is what
     *     being interleaved means
     */
    public PixelInterleavedSampleModel(int dataType, int w, int h, int pixelStride,
            int scanlineStride, int[] bandOffsets) {
        super(dataType, w, h, pixelStride, scanlineStride, bandOffsets);
        int min = bandOffsets[0];
        int max = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++) {
            if (bandOffsets[i] < min) {
                min = bandOffsets[i];
            }
            if (bandOffsets[i] > max) {
                max = bandOffsets[i];
            }
        }
        // If the bands are further apart than a pixel measures, they are not interleaved: the model
        // would be by planes in disguise, and `createCompatibleSampleModel` would give a result
        // that does not describe the same data.
        if (max - min > pixelStride) {
            throw new RasterFormatException("Offsets between bands must be less than the pixel "
                    + "stride");
        }
        if (pixelStride * w > scanlineStride) {
            throw new RasterFormatException("Pixel stride times width must be less than or "
                    + "equal to the scanline stride");
        }
    }

    /** Another interleaved one of the size asked for. See the note of the class. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int min = this.bandOffsets[0];
        for (int i = 1; i < this.bandOffsets.length; i++) {
            if (this.bandOffsets[i] < min) {
                min = this.bandOffsets[i];
            }
        }
        int[] offsets = new int[this.bandOffsets.length];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = this.bandOffsets[i] - min;
        }
        return new PixelInterleavedSampleModel(this.dataType, w, h, this.pixelStride,
                this.pixelStride * w, offsets);
    }

    /**
     * An interleaved one with only those bands, over the same data.
     *
     * @throws RasterFormatException if some band does not exist
     */
    public SampleModel createSubsetSampleModel(int[] bands) {
        int[] offsets = new int[bands.length];
        for (int i = 0; i < bands.length; i++) {
            if (bands[i] < 0 || bands[i] >= this.numBands) {
                throw new RasterFormatException("Band " + bands[i] + " does not exist");
            }
            offsets[i] = this.bandOffsets[bands[i]];
        }
        return new PixelInterleavedSampleModel(this.dataType, this.width, this.height,
                this.pixelStride, this.scanlineStride, offsets);
    }

    public int hashCode() {
        return super.hashCode() ^ 0x5049584c;
    }
}
