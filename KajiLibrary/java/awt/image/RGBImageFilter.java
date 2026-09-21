package java.awt.image;

/**
 * A filter that works **colour by colour**, without looking at the neighbours.
 *
 * <p>The subclass only writes {@link #filterRGB}: a function from one colour to another. Everything
 * else —unpacking the pixel, converting it to ARGB, packing it again— this class puts in.
 *
 * <p>And there is a shortcut worth understanding, because it is the reason for almost the whole
 * class. If the image comes with a palette and the function does not depend on the position, there
 * is no need to filter the pixels: it is enough to filter **the palette**, which is at most 256
 * colours, and leave the pixels as they are. An image of a million pixels is filtered with 256
 * calls.
 *
 * <p>That is what {@link #canFilterIndexColorModel} declares, and the subclass has to set it to
 * `true` only if its function really ignores the coordinates. If it uses them and turns the
 * shortcut on, every pixel of the same index receives the colour that fell to the coordinates the
 * palette was filtered with, and the result looks like nothing.
 */
public abstract class RGBImageFilter extends ImageFilter {

    /** The colour model the producer announced. */
    protected ColorModel origmodel;

    /** The model it is replaced with. */
    protected ColorModel newmodel;

    /**
     * Whether filtering the palette instead of the pixels is enough.
     *
     * <p>It can only be `true` if {@link #filterRGB} ignores the coordinates.
     */
    protected boolean canFilterIndexColorModel;

    /** For the subclasses. */
    protected RGBImageFilter() {
    }

    /**
     * The colour function, which is all the subclass has to write.
     *
     * @param x the coordinate, or -1 if the colour comes from a palette
     * @param y the same
     * @param rgb the input colour, in ARGB
     * @return the output colour, in ARGB
     */
    public abstract int filterRGB(int x, int y, int rgb);

    /**
     * Announces the colour model, applying the palette shortcut where it fits.
     *
     * <p>When the shortcut is on, what reaches the consumer is an **already filtered** palette and
     * the pixels untouched.
     */
    public void setColorModel(ColorModel model) {
        if (this.canFilterIndexColorModel && model instanceof IndexColorModel) {
            ColorModel newcm = this.filterIndexColorModel((IndexColorModel) model);
            this.substituteColorModel(model, newcm);
            this.consumer.setColorModel(newcm);
        } else {
            this.consumer.setColorModel(ColorModel.getRGBdefault());
        }
    }

    /** Records that one model is replaced by another. */
    public void substituteColorModel(ColorModel oldcm, ColorModel newcm) {
        this.origmodel = oldcm;
        this.newmodel = newcm;
    }

    /**
     * The same palette with all of its colours passed through {@link #filterRGB}.
     *
     * <p>The coordinates passed to it are -1: a palette colour is nowhere in particular, and
     * passing it any old point would be lying to the function.
     */
    public IndexColorModel filterIndexColorModel(IndexColorModel icm) {
        int mapsize = icm.getMapSize();
        byte[] r = new byte[mapsize];
        byte[] g = new byte[mapsize];
        byte[] b = new byte[mapsize];
        byte[] a = new byte[mapsize];
        icm.getReds(r);
        icm.getGreens(g);
        icm.getBlues(b);
        icm.getAlphas(a);
        int trans = icm.getTransparentPixel();
        boolean needalpha = false;
        for (int i = 0; i < mapsize; i++) {
            int rgb = this.filterRGB(-1, -1, icm.getRGB(i));
            a[i] = (byte) (rgb >> 24);
            if (a[i] != ((byte) 0xFF) && i != trans) {
                needalpha = true;
            }
            r[i] = (byte) (rgb >> 16);
            g[i] = (byte) (rgb >> 8);
            b[i] = (byte) rgb;
        }
        if (needalpha) {
            return new IndexColorModel(icm.getPixelSize(), mapsize, r, g, b, a);
        }
        return new IndexColorModel(icm.getPixelSize(), mapsize, r, g, b, trans);
    }

    /** Passes a batch of pixels through the function, one by one. */
    public void filterRGBPixels(int x, int y, int w, int h, int[] pixels, int off, int scansize) {
        int index = off;
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                pixels[index] = this.filterRGB(x + cx, y + cy, pixels[index]);
                index = index + 1;
            }
            index = index + scansize - w;
        }
        this.consumer.setPixels(x, y, w, h, ColorModel.getRGBdefault(), pixels, off, scansize);
    }

    /**
     * Forwards a batch of pixels of one byte.
     *
     * <p>If the model is the one that was replaced, the pixels go through **untouched**: the
     * palette was filtered already and filtering them again would apply the function twice.
     */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        if (model == this.origmodel) {
            this.consumer.setPixels(x, y, w, h, this.newmodel, pixels, off, scansize);
            return;
        }
        int[] filtered = new int[w];
        int index = off;
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                filtered[cx] = model.getRGB(pixels[index + cx] & 0xFF);
            }
            this.filterRGBPixels(x, y + cy, w, 1, filtered, 0, w);
            index = index + scansize;
        }
    }

    /** The same for pixels of one `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        if (model == this.origmodel) {
            this.consumer.setPixels(x, y, w, h, this.newmodel, pixels, off, scansize);
            return;
        }
        int[] filtered = new int[w];
        int index = off;
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                filtered[cx] = model.getRGB(pixels[index + cx]);
            }
            this.filterRGBPixels(x, y + cy, w, 1, filtered, 0, w);
            index = index + scansize;
        }
    }
}
