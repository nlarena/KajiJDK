package java.awt.image;

/**
 * The adapter between the two ways of filtering images that AWT has.
 *
 * <p>{@link ImageFilter} works in batches, over an image as it arrives;
 * {@link BufferedImageOp} works over the whole image at once. This class joins the two: it behaves
 * like a pipe filter, keeps the complete image while it arrives, and only at the end applies the
 * operation and delivers the result.
 *
 * <p>That "only at the end" is the part to keep in mind. An operation such as a convolution
 * **needs** the neighbours of each pixel, so it cannot start before having everything, and that is
 * why this filter keeps the whole image in memory: it is what the operation it wraps asks for, not
 * an oversight.
 */
public class BufferedImageFilter extends ImageFilter implements Cloneable {

    private final BufferedImageOp bufferedImageOp;
    private ColorModel model;
    private int width;
    private int height;
    private byte[] bytePixels;
    private int[] intPixels;

    /**
     * With the operation it is going to apply.
     *
     * @throws NullPointerException if the operation is `null`
     */
    public BufferedImageFilter(BufferedImageOp op) {
        if (op == null) {
            throw new NullPointerException("Operation cannot be null");
        }
        this.bufferedImageOp = op;
    }

    /** The operation it applies. */
    public BufferedImageOp getBufferedImageOp() {
        return this.bufferedImageOp;
    }

    /**
     * Records the size and reserves the image.
     *
     * @throws IllegalArgumentException if the size is empty
     */
    public void setDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            this.width = 0;
            this.height = 0;
            this.consumer.imageComplete(ImageConsumer.STATICIMAGEDONE);
            return;
        }
        this.width = width;
        this.height = height;
    }

    /** Records the colour model the pixels are going to come with. */
    public void setColorModel(ColorModel model) {
        this.model = model;
    }

    /** Stores a batch of pixels of one byte. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        if (this.width == 0 || this.height == 0) {
            return;
        }
        if (this.bytePixels == null && this.intPixels == null) {
            this.bytePixels = new byte[this.width * this.height];
            this.model = model;
        }
        if (this.bytePixels != null && this.model == model) {
            for (int cy = 0; cy < h; cy++) {
                System.arraycopy(pixels, off + cy * scansize, this.bytePixels,
                        (y + cy) * this.width + x, w);
            }
            return;
        }
        // A batch arrived with another model: there is no single one that describes both, so
        // everything is taken to ARGB, which is the only common one.
        this.aRGB();
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                this.intPixels[(y + cy) * this.width + x + cx] =
                        model.getRGB(pixels[off + cy * scansize + cx] & 0xFF);
            }
        }
    }

    /** Stores a batch of pixels of one `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        if (this.width == 0 || this.height == 0) {
            return;
        }
        if (this.intPixels == null) {
            this.aRGB();
        }
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                this.intPixels[(y + cy) * this.width + x + cx] =
                        model.getRGB(pixels[off + cy * scansize + cx]);
            }
        }
    }

    /** Takes whatever has been gathered to ARGB. */
    private void aRGB() {
        int[] fresh = new int[this.width * this.height];
        if (this.bytePixels != null && this.model != null) {
            for (int i = 0; i < fresh.length; i++) {
                fresh[i] = this.model.getRGB(this.bytePixels[i] & 0xFF);
            }
        }
        this.bytePixels = null;
        this.intPixels = fresh;
        this.model = ColorModel.getRGBdefault();
    }

    /**
     * Builds the image, applies the operation to it and delivers the result.
     *
     * <p>With an error or abort status nothing is applied: the image is incomplete and filtering a
     * half-finished image would give a result that is nobody's.
     */
    public void imageComplete(int status) {
        if (status == ImageConsumer.IMAGEERROR || status == ImageConsumer.IMAGEABORTED) {
            this.consumer.imageComplete(status);
            return;
        }
        if (this.width == 0 || this.height == 0) {
            this.consumer.imageComplete(ImageConsumer.STATICIMAGEDONE);
            return;
        }
        BufferedImage in;
        if (this.intPixels != null) {
            in = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
            in.setRGB(0, 0, this.width, this.height, this.intPixels, 0, this.width);
        } else {
            this.aRGB();
            in = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
            in.setRGB(0, 0, this.width, this.height, this.intPixels, 0, this.width);
        }
        BufferedImage out = this.bufferedImageOp.filter(in, null);
        int w = out.getWidth();
        int h = out.getHeight();
        int[] row = new int[w];
        ColorModel rgb = ColorModel.getRGBdefault();
        this.consumer.setDimensions(w, h);
        this.consumer.setColorModel(rgb);
        this.consumer.setHints(ImageConsumer.TOPDOWNLEFTRIGHT
                | ImageConsumer.COMPLETESCANLINES | ImageConsumer.SINGLEPASS
                | ImageConsumer.SINGLEFRAME);
        for (int y = 0; y < h; y++) {
            out.getRGB(0, y, w, 1, row, 0, w);
            this.consumer.setPixels(0, y, w, 1, rgb, row, 0, w);
        }
        this.consumer.imageComplete(status);
    }
}
