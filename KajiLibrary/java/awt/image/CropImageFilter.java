package java.awt.image;

import java.awt.Rectangle;
import java.util.Hashtable;

/**
 * A filter that lets only a rectangle of the image through.
 *
 * <p>It moves the origin: the corner of the crop becomes the (0,0) of what comes out. The batches
 * that fall entirely outside are discarded and the ones that fall halfway are cropped, without
 * copying anything — the consumer is handed a different offset into the same array.
 */
public class CropImageFilter extends ImageFilter {

    private final int cropX;
    private final int cropY;
    private final int cropW;
    private final int cropH;

    /** With the rectangle to crop. */
    public CropImageFilter(int x, int y, int w, int h) {
        this.cropX = x;
        this.cropY = y;
        this.cropW = w;
        this.cropH = h;
    }

    /** Forwards the properties, adding the cropped rectangle. */
    public void setProperties(Hashtable<?, ?> props) {
        Hashtable<Object, Object> p = copyProperties(props);
        p.put("croprect", new Rectangle(this.cropX, this.cropY, this.cropW, this.cropH));
        super.setProperties(p);
    }

    /** Announces the size of the crop, not that of the original image. */
    public void setDimensions(int w, int h) {
        this.consumer.setDimensions(this.cropW, this.cropH);
    }

    /** A sum that does not wrap around: it saturates instead of overflowing. */
    private static int saturatedSum(int x, int w) {
        int x2 = x + w;
        if (x > 0 && w > 0 && x2 < 0) {
            return Integer.MAX_VALUE;
        }
        return x2;
    }

    /**
     * Lets the part of the batch that falls inside the crop through, with the coordinates moved.
     */
    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off,
            int scansize) {
        int x1 = x;
        if (x1 < this.cropX) {
            x1 = this.cropX;
        }
        int x2 = saturatedSum(x, w);
        if (x2 > this.cropX + this.cropW) {
            x2 = this.cropX + this.cropW;
        }
        int y1 = y;
        if (y1 < this.cropY) {
            y1 = this.cropY;
        }
        int y2 = saturatedSum(y, h);
        if (y2 > this.cropY + this.cropH) {
            y2 = this.cropY + this.cropH;
        }
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        this.consumer.setPixels(x1 - this.cropX, y1 - this.cropY, x2 - x1, y2 - y1, model, pixels,
                off + (y1 - y) * scansize + (x1 - x), scansize);
    }

    /** The same for pixels of one `int`. */
    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off,
            int scansize) {
        int x1 = x;
        if (x1 < this.cropX) {
            x1 = this.cropX;
        }
        int x2 = saturatedSum(x, w);
        if (x2 > this.cropX + this.cropW) {
            x2 = this.cropX + this.cropW;
        }
        int y1 = y;
        if (y1 < this.cropY) {
            y1 = this.cropY;
        }
        int y2 = saturatedSum(y, h);
        if (y2 > this.cropY + this.cropH) {
            y2 = this.cropY + this.cropH;
        }
        if (x1 >= x2 || y1 >= y2) {
            return;
        }
        this.consumer.setPixels(x1 - this.cropX, y1 - this.cropY, x2 - x1, y2 - y1, model, pixels,
                off + (y1 - y) * scansize + (x1 - x), scansize);
    }
}
