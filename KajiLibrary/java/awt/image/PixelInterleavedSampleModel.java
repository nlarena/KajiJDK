package java.awt.image;

/**
 * Un {@link ComponentSampleModel} con **todas las bandas en el mismo banco**: el formato
 * intercalado, RGBRGBRGB.
 *
 * <p>No agrega ningun dato: es el mismo modelo con una restriccion. Que exista como clase propia
 * sirve para dos cosas concretas. Una, el constructor comprueba la restriccion en vez de dejar que
 * se arme un modelo que dice ser intercalado y no lo es. Dos --y es la que importa--,
 * `createCompatibleSampleModel` devuelve otro intercalado en vez de un componente generico, asi que
 * copiar una imagen conserva su formato.
 */
public class PixelInterleavedSampleModel extends ComponentSampleModel {

    /**
     * @throws RasterFormatException si los desplazamientos de banda no caben dentro de un pixel,
     *     que es lo que significa estar intercalado
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
        // Si las bandas se separan mas de lo que mide un pixel, no estan intercaladas: el modelo
        // seria por planos disfrazado, y `createCompatibleSampleModel` daria un resultado que no
        // describe los mismos datos.
        if (max - min > pixelStride) {
            throw new RasterFormatException("Offsets between bands must be less than the pixel "
                    + "stride");
        }
        if (pixelStride * w > scanlineStride) {
            throw new RasterFormatException("Pixel stride times width must be less than or "
                    + "equal to the scanline stride");
        }
    }

    /** Otro intercalado del tamano pedido. Ver la nota de la clase. */
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
     * Un intercalado con solo esas bandas, sobre los mismos datos.
     *
     * @throws RasterFormatException si alguna banda no existe
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
