package java.awt;

import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.PixelGrabber;
import java.awt.image.ReplicateScaleFilter;

/**
 * An image, which may not be whole yet.
 *
 * <p>That last part is what explains the odd shape of the class. When it was designed, an image
 * came over the network while the page was already being shown, so asking it for its width had to
 * be able to answer "I do not know yet". Hence {@link #getWidth} taking an {@link ImageObserver}:
 * it returns -1 if it does not know, and tells the observer when it finds out.
 *
 * <p>A {@link BufferedImage} is the case where that asynchrony does not exist: the pixels are
 * already in memory, the observer is never used and the width is always known. It is still an
 * `Image` because everything that draws images is written against this class.
 */
public abstract class Image {

    /** What {@link #getProperty} returns when the property is not defined. */
    public static final Object UndefinedProperty = new Object();

    /** Let the scaling choose the algorithm. */
    public static final int SCALE_DEFAULT = 1;

    /** Let it favour speed. */
    public static final int SCALE_FAST = 2;

    /** Let it favour quality. */
    public static final int SCALE_SMOOTH = 4;

    /** Repeating or skipping pixels: the fastest and the ugliest. */
    public static final int SCALE_REPLICATE = 8;

    /** Averaging the area falling on each pixel: slower and much better when shrinking. */
    public static final int SCALE_AREA_AVERAGING = 16;

    private static final ImageCapabilities defaultImageCaps = new ImageCapabilities(false);

    /**
     * How much it is worth accelerating this image, from 0 to 1.
     *
     * <p>It is a hint about scarce memory: an image drawn on every frame deserves to stay in the
     * fast memory and one drawn once does not.
     */
    protected float accelerationPriority = 0.5f;

    /** For the subclasses. */
    protected Image() {
    }

    /**
     * The width, or -1 if it is not known yet.
     *
     * <p>The -1 is not an error: it is "ask again when I tell you".
     */
    public abstract int getWidth(ImageObserver observer);

    /** The height, or -1 if it is not known yet. */
    public abstract int getHeight(ImageObserver observer);

    /** Where the pixels come from. */
    public abstract ImageProducer getSource();

    /**
     * A context to draw **onto** this image.
     *
     * @throws UnsupportedOperationException if the image cannot be drawn onto
     */
    public abstract Graphics getGraphics();

    /**
     * A property of the image.
     *
     * @return the value, `null` if it is not known yet, or {@link #UndefinedProperty} if it does
     *     not exist
     */
    public abstract Object getProperty(String name, ImageObserver observer);

    /**
     * The same image at another size.
     *
     * <p>With either of the two measures negative it is worked out from the other one, keeping the
     * proportion.
     *
     * <p>Unlike the JDK, which returns a lazy image that is computed when it gets drawn, here the
     * scaling is done on the spot and what comes out is a {@link BufferedImage} already finished.
     * The reason is that the lazy version needs the windowing system to build the image, and this
     * library does not have it; the result is the same and the difference is when the work is done.
     *
     * @return the scaled image, or `null` if the pixels could not be grabbed
     * @throws IllegalArgumentException if both measures are zero
     */
    public Image getScaledInstance(int width, int height, int hints) {
        ImageFilter filter;
        if ((hints & (SCALE_SMOOTH | SCALE_AREA_AVERAGING)) != 0) {
            filter = new AreaAveragingScaleFilter(width, height);
        } else {
            filter = new ReplicateScaleFilter(width, height);
        }
        ImageProducer prod = new FilteredImageSource(this.getSource(), filter);
        // With the array at null and the measures at -1, the grabber reserves its own when the
        // filter announces the final size, which is the only thing that knows how big the result
        // is.
        PixelGrabber pg = new PixelGrabber(prod, 0, 0, -1, -1, null, 0, 0);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
            return null;
        }
        int w = pg.getWidth();
        int h = pg.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = (int[]) pg.getPixels();
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }

    /**
     * Releases the resources and forces the image to be worked out again if it is used again.
     *
     * <p>The implementation here does nothing, which is what is right for an image that is already
     * whole in memory.
     */
    public void flush() {
    }

    /**
     * What can be accelerated about this image on that configuration.
     *
     * @param gc the configuration, or `null` for the default device's
     */
    public ImageCapabilities getCapabilities(GraphicsConfiguration gc) {
        return defaultImageCaps;
    }

    /**
     * Changes how much it is worth accelerating this image.
     *
     * @throws IllegalArgumentException if the value is not between 0 and 1
     */
    public void setAccelerationPriority(float priority) {
        if (priority < 0 || priority > 1) {
            throw new IllegalArgumentException(
                    "Priority must be a value between 0 and 1, inclusive");
        }
        this.accelerationPriority = priority;
    }

    /** How much it is worth accelerating this image. */
    public float getAccelerationPriority() {
        return this.accelerationPriority;
    }
}
