package java.awt.image;

import java.util.Hashtable;

/**
 * Scales an image by **repeating and skipping** pixels.
 *
 * <p>It is the cheapest scaling there is: for each pixel of the destination the nearest pixel of
 * the source is chosen, and that is that. Enlarging gives blocks and shrinking loses whole details
 * —a cable one pixel wide can disappear altogether—, but it does not do a single multiplication.
 *
 * <p>The correspondence between columns and rows is computed once, into two tables. That they are
 * **rounded to the centre** and not truncated is what keeps the image from moving half a pixel:
 * `srccols[dx]` is the source of the centre of the destination pixel, not that of its left edge.
 *
 * <p>With one of the two measures negative it is computed from the other keeping the proportion,
 * and that can only be done once the size of the source is known: that is why it happens in {@link
 * #setDimensions} and not in the constructor.
 */
public class ReplicateScaleFilter extends ImageFilter {

    /** Width of the source. */
    protected int srcWidth;

    /** Height of the source. */
    protected int srcHeight;

    /** Width of the destination. */
    protected int destWidth;

    /** Height of the destination. */
    protected int destHeight;

    /** For each row of the destination, which row of the source it comes from. */
    protected int[] srcrows;

    /** For each column of the destination, which column of the source it comes from. */
    protected int[] srccols;

    /** The array reused to build each output row. */
    protected Object outpixbuf;

    /**
     * With the destination size.
     *
     * @throws IllegalArgumentException if either of the two measures is zero
     */
    public ReplicateScaleFilter(int width, int height) {
        if (width == 0 || height == 0) {
            throw new IllegalArgumentException("Width (" + width + ") and height (" + height
                    + ") must be non-zero");
        }
        this.destWidth = width;
        this.destHeight = height;
    }

    /** Forwards the properties, leaving a record of the scaling. */
    public void setProperties(Hashtable<?, ?> props) {
        Hashtable<Object, Object> p = copyProperties(props);
        String key = "rescale";
        String val = this.destWidth + "x" + this.destHeight;
        Object o = p.get(key);
        if (o != null && o instanceof String) {
            val = ((String) o) + ", " + val;
        }
        p.put(key, val);
        super.setProperties(p);
    }

    /** Stores the size of the source and works out the destination measures that were missing. */
    public void setDimensions(int w, int h) {
        this.srcWidth = w;
        this.srcHeight = h;
        if (this.destWidth < 0) {
            if (this.destHeight < 0) {
                this.destWidth = this.srcWidth;
                this.destHeight = this.srcHeight;
            } else {
                this.destWidth = this.srcWidth * this.destHeight / this.srcHeight;
                if (this.destWidth == 0) {
                    this.destWidth = 1;
                }
            }
        } else if (this.destHeight < 0) {
            this.destHeight = this.srcHeight * this.destWidth / this.srcWidth;
            if (this.destHeight == 0) {
                this.destHeight = 1;
            }
        }
        this.consumer.setDimensions(this.destWidth, this.destHeight);
    }

    /**
     * Builds the two correspondence tables.
     *
     * <p>The sum `(2*d*src + src) / (2*dest)` is the source of the **centre** of the destination
     * pixel: the `+ src` on top is half a destination pixel taken to source units.
     */
    private void computeTables() {
        this.srcrows = new int[this.destHeight + 1];
        for (int y = 0; y <= this.destHeight; y++) {
            this.srcrows[y] = (2 * y * this.srcHeight + this.srcHeight) / (2 * this.destHeight);
        }
        this.srccols = new int[this.destWidth + 1];
        for (int x = 0; x <= this.destWidth; x++) {
            this.srccols[x] = (2 * x * this.srcWidth + this.srcWidth) / (2 * this.destWidth);
        }
    }

    /** Hands a batch of pixels of one byte out to the destination rows it falls on. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        if (this.srcrows == null || this.srccols == null) {
            this.computeTables();
        }
        int dx1 = (2 * x * this.destWidth + this.srcWidth - 1) / (2 * this.srcWidth);
        int dy1 = (2 * y * this.destHeight + this.srcHeight - 1) / (2 * this.srcHeight);
        byte[] outpix;
        if (this.outpixbuf != null && this.outpixbuf instanceof byte[]) {
            outpix = (byte[]) this.outpixbuf;
        } else {
            outpix = new byte[this.destWidth];
            this.outpixbuf = outpix;
        }
        int sy;
        int sx;
        for (int dy = dy1; dy < this.destHeight && (sy = this.srcrows[dy]) < y + h; dy++) {
            int srcoff = off + scansize * (sy - y);
            int dx;
            for (dx = dx1; dx < this.destWidth && (sx = this.srccols[dx]) < x + w; dx++) {
                outpix[dx] = pixels[srcoff + sx - x];
            }
            if (dx > dx1) {
                this.consumer.setPixels(dx1, dy, dx - dx1, 1, model, outpix, dx1, this.destWidth);
            }
        }
    }

    /** The same for pixels of one `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        if (this.srcrows == null || this.srccols == null) {
            this.computeTables();
        }
        int dx1 = (2 * x * this.destWidth + this.srcWidth - 1) / (2 * this.srcWidth);
        int dy1 = (2 * y * this.destHeight + this.srcHeight - 1) / (2 * this.srcHeight);
        int[] outpix;
        if (this.outpixbuf != null && this.outpixbuf instanceof int[]) {
            outpix = (int[]) this.outpixbuf;
        } else {
            outpix = new int[this.destWidth];
            this.outpixbuf = outpix;
        }
        int sy;
        int sx;
        for (int dy = dy1; dy < this.destHeight && (sy = this.srcrows[dy]) < y + h; dy++) {
            int srcoff = off + scansize * (sy - y);
            int dx;
            for (dx = dx1; dx < this.destWidth && (sx = this.srccols[dx]) < x + w; dx++) {
                outpix[dx] = pixels[srcoff + sx - x];
            }
            if (dx > dx1) {
                this.consumer.setPixels(dx1, dy, dx - dx1, 1, model, outpix, dx1, this.destWidth);
            }
        }
    }
}
