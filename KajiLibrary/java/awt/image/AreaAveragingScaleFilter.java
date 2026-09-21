package java.awt.image;

/**
 * Scales an image by **averaging** the source pixels that fall on each destination pixel.
 *
 * <p>It is the good scaling, and the difference from {@link ReplicateScaleFilter} shows above all
 * when shrinking: repeating and skipping can make a whole thin line disappear, averaging leaves it
 * as a faint grey. It costs one multiplication and one addition per overlap.
 *
 * <p>The split is done in integer arithmetic and with no accumulated rounding errors, working in
 * units where the whole image is `srcWidth * destWidth` wide: there a source pixel takes exactly
 * `destWidth` units and a destination one, `srcWidth`. Each overlap is the minimum of the two
 * remainders, and there are no divisions until the end.
 *
 * <p>The colours are averaged **premultiplied by the alpha**, and that is why it has to be undone
 * when closing each row. Without premultiplying, a transparent pixel would contribute its colour to
 * the average: shrinking a logo over a transparent background would show a fringe of the colour of
 * the invisible background.
 *
 * <p>It needs to receive the rows in order and whole. If the producer warns that it is not going to
 * send them that way, the filter gives up and behaves like its base class, which can work in any
 * order.
 */
public class AreaAveragingScaleFilter extends ReplicateScaleFilter {

    private static final ColorModel rgbmodel = ColorModel.getRGBdefault();
    private static final int neededHints = ImageConsumer.TOPDOWNLEFTRIGHT
            | ImageConsumer.COMPLETESCANLINES;

    private boolean passthrough;
    private float[] reds;
    private float[] greens;
    private float[] blues;
    private float[] alphas;

    /** The destination row that is being built. */
    private int savedy;

    /** How many units that row is still short of being complete. */
    private int savedyrem;

    /**
     * With the destination size.
     *
     * @throws IllegalArgumentException if either of the two measures is zero
     */
    public AreaAveragingScaleFilter(int width, int height) {
        super(width, height);
    }

    /**
     * Records whether the pixels are going to be averageable.
     *
     * <p>Without whole rows and in order it cannot be done: the average of a destination row needs
     * all the source ones that touch it, and with no guarantee of order the whole image would have
     * to be kept.
     */
    public void setHints(int hints) {
        this.passthrough = (hints & neededHints) != neededHints;
        super.setHints(hints);
    }

    /** Reserves the accumulators of one destination row. */
    private void createAccumulators() {
        this.reds = new float[this.destWidth];
        this.greens = new float[this.destWidth];
        this.blues = new float[this.destWidth];
        this.alphas = new float[this.destWidth];
    }

    /** Sets the accumulators to zero to start another row. */
    private void clearAccumulators() {
        for (int i = 0; i < this.destWidth; i++) {
            this.alphas[i] = 0.0f;
            this.reds[i] = 0.0f;
            this.greens[i] = 0.0f;
            this.blues[i] = 0.0f;
        }
    }

    /**
     * Closes the destination row: divides by the area and undoes the premultiplication.
     *
     * <p>With alpha zero there is no colour to recover and the pixel comes out transparent and
     * black; with full alpha there is no need to divide twice and the average is enough.
     */
    private int[] closeRow() {
        float totalArea = ((float) this.srcWidth) * this.srcHeight;
        if (this.outpixbuf == null || !(this.outpixbuf instanceof int[])) {
            this.outpixbuf = new int[this.destWidth];
        }
        int[] outpix = (int[]) this.outpixbuf;
        for (int x = 0; x < this.destWidth; x++) {
            float mult = totalArea;
            int a = Math.round(this.alphas[x] / mult);
            if (a <= 0) {
                a = 0;
            } else if (a >= 255) {
                a = 255;
            } else {
                // Dividing by this other factor does the division by the area and the one that
                // undoes the premultiplication in a single step.
                mult = this.alphas[x] / 255;
            }
            int r = Math.round(this.reds[x] / mult);
            int g = Math.round(this.greens[x] / mult);
            int b = Math.round(this.blues[x] / mult);
            if (r < 0) {
                r = 0;
            } else if (r > 255) {
                r = 255;
            }
            if (g < 0) {
                g = 0;
            } else if (g > 255) {
                g = 255;
            }
            if (b < 0) {
                b = 0;
            } else if (b > 255) {
                b = 255;
            }
            outpix[x] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return outpix;
    }

    /**
     * Hands a batch of pixels out among the destination rows it touches.
     *
     * <p>`pixels` is a `byte[]` or an `int[]`; the colour model says how to read it.
     */
    private void accumulate(int x, int y, int w, int h, ColorModel model, Object pixels, int off,
            int scansize) {
        if (this.reds == null) {
            this.createAccumulators();
        }
        int sy = y;
        int syrem = this.destHeight;
        int dy;
        int dyrem;
        if (sy == 0) {
            dy = 0;
            dyrem = 0;
        } else {
            dy = this.savedy;
            dyrem = this.savedyrem;
        }
        int row = off;
        while (sy < y + h) {
            if (dyrem == 0) {
                this.clearAccumulators();
                dyrem = this.srcHeight;
            }
            int amty = syrem < dyrem ? syrem : dyrem;
            this.accumulateRow(model, pixels, row, w, amty);
            syrem = syrem - amty;
            dyrem = dyrem - amty;
            if (dyrem == 0 && dy < this.destHeight) {
                int[] outpix = this.closeRow();
                this.consumer.setPixels(0, dy, this.destWidth, 1, rgbmodel, outpix, 0,
                        this.destWidth);
                dy = dy + 1;
            }
            if (syrem == 0) {
                sy = sy + 1;
                syrem = this.destHeight;
                row = row + scansize;
            }
        }
        this.savedy = dy;
        this.savedyrem = dyrem;
    }

    /** Adds a source row to the accumulators, with the given vertical weight. */
    private void accumulateRow(ColorModel model, Object pixels, int off, int w, int amty) {
        int dx = 0;
        int dxrem = this.srcWidth;
        for (int sx = 0; sx < w; sx++) {
            int raw;
            if (pixels instanceof byte[]) {
                raw = ((byte[]) pixels)[off + sx] & 0xFF;
            } else {
                raw = ((int[]) pixels)[off + sx];
            }
            int rgb = model.getRGB(raw);
            float a = rgb >>> 24;
            float r = (rgb >> 16) & 0xFF;
            float g = (rgb >> 8) & 0xFF;
            float b = rgb & 0xFF;
            if (a != 255.0f) {
                float scaleBy = a / 255.0f;
                r = r * scaleBy;
                g = g * scaleBy;
                b = b * scaleBy;
            }
            int sxrem = this.destWidth;
            while (sxrem > 0 && dx < this.destWidth) {
                int amtx = sxrem < dxrem ? sxrem : dxrem;
                float mult = ((float) amtx) * amty;
                this.alphas[dx] = this.alphas[dx] + mult * a;
                this.reds[dx] = this.reds[dx] + mult * r;
                this.greens[dx] = this.greens[dx] + mult * g;
                this.blues[dx] = this.blues[dx] + mult * b;
                sxrem = sxrem - amtx;
                dxrem = dxrem - amtx;
                if (dxrem == 0) {
                    dx = dx + 1;
                    dxrem = this.srcWidth;
                }
            }
        }
    }

    /** Averages, or gives up and repeats if the producer does not guarantee the order. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        if (this.passthrough) {
            super.setPixels(x, y, w, h, model, pixels, off, scansize);
        } else {
            this.accumulate(x, y, w, h, model, pixels, off, scansize);
        }
    }

    /** The same for pixels of one `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        if (this.passthrough) {
            super.setPixels(x, y, w, h, model, pixels, off, scansize);
        } else {
            this.accumulate(x, y, w, h, model, pixels, off, scansize);
        }
    }
}
