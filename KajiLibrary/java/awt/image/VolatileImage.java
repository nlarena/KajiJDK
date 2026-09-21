package java.awt.image;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Transparency;

/**
 * An image that lives in the memory of the video device and that **may disappear**.
 *
 * <p>That is the whole idea. Keeping it there makes it very much faster to draw, but that memory is
 * nobody's: the system can take it away when the resolution changes, when the screen locks or when
 * another program needs it. The image is not corrupted, it is **emptied**, and it has to be drawn
 * again.
 *
 * <p>Hence the pair of methods that define it. {@link #validate} is called before using it and says
 * whether anything has to be redone; {@link #contentsLost} is called after drawing it and says
 * whether what was drawn got there. Both are needed because the memory can be lost **during** the
 * drawing, not only between two uses, and the right loop is:
 *
 * <pre>do {
 *     if (vi.validate(gc) == IMAGE_INCOMPATIBLE) vi = createIt(gc);
 *     drawIntoTheImage(vi);
 *     drawTheImage(vi);
 * } while (vi.contentsLost());</pre>
 *
 * <p>It is the class that makes double buffering without flicker possible, and also the reason why
 * that code looks strange the first time: the `do/while` is not paranoia, it is the only way of
 * writing the sequence without a window in which a frame is lost.
 */
public abstract class VolatileImage extends Image implements Transparency {

    /** The image is intact and can be used. */
    public static final int IMAGE_OK = 0;

    /** It had been lost and was restored empty: it has to be drawn again. */
    public static final int IMAGE_RESTORED = 1;

    /** It no longer serves for this device: another one has to be created. */
    public static final int IMAGE_INCOMPATIBLE = 2;

    /** `OPAQUE`, `BITMASK` or `TRANSLUCENT`. */
    protected int transparency = Transparency.TRANSLUCENT;

    /** For the subclasses. */
    protected VolatileImage() {
    }

    /**
     * A **non-volatile** copy of what is there now.
     *
     * <p>It is the way of getting the pixels out of here: a {@link BufferedImage} is in ordinary
     * memory and is not lost.
     */
    public abstract BufferedImage getSnapshot();

    /** Width, in pixels. */
    public abstract int getWidth();

    /** Height, in pixels. */
    public abstract int getHeight();

    /**
     * A producer with the pixels of this image.
     *
     * <p>It goes through {@link #getSnapshot}: the pixels that come out are those of the moment
     * they were asked for, because the image can change or be lost while they are being delivered.
     */
    public ImageProducer getSource() {
        return this.getSnapshot().getSource();
    }

    /** A context to draw over this image. */
    public Graphics getGraphics() {
        return this.createGraphics();
    }

    /** A context to draw over this image. */
    public abstract Graphics2D createGraphics();

    /**
     * Checks the state of the image and restores it if need be.
     *
     * @return {@link #IMAGE_OK}, {@link #IMAGE_RESTORED} —it has to be drawn again— or
     *     {@link #IMAGE_INCOMPATIBLE} —another one has to be created—
     */
    public abstract int validate(GraphicsConfiguration gc);

    /**
     * Whether the contents were lost since the last check.
     *
     * <p>It has to be called **after** drawing: a `false` from before says nothing about what
     * happened during the drawing.
     */
    public abstract boolean contentsLost();

    /** What can be accelerated about this image. */
    public abstract ImageCapabilities getCapabilities();

    /** `OPAQUE`, `BITMASK` or `TRANSLUCENT`. */
    public int getTransparency() {
        return this.transparency;
    }
}
