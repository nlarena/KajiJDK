package java.awt;

import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * The painting context of a flat colour.
 *
 * <p>It is the simplest possible and that is why it is worth looking at: it inverts no
 * transformation, looks at no coordinates and computes nothing per pixel. It builds a raster of the
 * requested size filled with the colour, keeps it, and for a later request that fits returns a
 * child of it of that size, so requests of the same size do not allocate twice. (This note said the
 * kept raster is a single pixel stretched to each size.)
 *
 * <p>It is not public: it is how {@link Color#createContext} is written.
 */
class ColorPaintContext implements PaintContext {

    private final int color;
    private final ColorModel model = ColorModel.getRGBdefault();
    private WritableRaster cache;

    /** With the ARGB colour it will always return. */
    ColorPaintContext(int color) {
        this.color = color;
    }

    /** There is nothing to release but the kept raster. */
    public void dispose() {
        this.cache = null;
    }

    /** Always 8-bit-per-channel ARGB. */
    public ColorModel getColorModel() {
        return this.model;
    }

    /**
     * A raster of the requested size, all of the same colour.
     *
     * <p>The last one is kept and reused while it is big enough: whoever draws asks for rectangles
     * of the same size again and again, and allocating one per request would waste memory.
     */
    public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster r = this.cache;
        if (r == null || r.getWidth() < w || r.getHeight() < h) {
            r = this.model.createCompatibleWritableRaster(w, h);
            int[] row = new int[w];
            for (int i = 0; i < w; i++) {
                row[i] = this.color;
            }
            for (int j = 0; j < h; j++) {
                r.setDataElements(0, j, w, 1, row);
            }
            this.cache = r;
            return r;
        }
        return r.createWritableChild(0, 0, w, h, 0, 0, null);
    }
}
