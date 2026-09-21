package java.awt.image;

import java.awt.Graphics;
import java.awt.Image;

/**
 * The base for an image of several resolutions.
 *
 * <p>It solves the boring part: everything {@link Image} asks for —the width, the height, the
 * properties, the producer— is answered by forwarding it to **one** of the versions, the base one,
 * which the subclass chooses. All that is left to write is which one that base is and which the
 * variants are.
 *
 * <p>The base one is the one of logical resolution: the one that defines how big the image is in
 * user coordinates. The other versions have more pixels but the same logical size, which is exactly
 * what lets a dense screen use them without anything moving about.
 */
public abstract class AbstractMultiResolutionImage extends Image implements MultiResolutionImage {

    /** For the subclasses. */
    protected AbstractMultiResolutionImage() {
    }

    /** The version that defines the logical size. */
    protected abstract Image getBaseImage();

    /** The width of the base version. */
    public int getWidth(ImageObserver observer) {
        return this.getBaseImage().getWidth(observer);
    }

    /** The height of the base version. */
    public int getHeight(ImageObserver observer) {
        return this.getBaseImage().getHeight(observer);
    }

    /** The producer of the base version. */
    public ImageProducer getSource() {
        return this.getBaseImage().getSource();
    }

    /**
     * A context to draw over it.
     *
     * @throws UnsupportedOperationException always: drawing over an image of several resolutions
     *     would have to draw over all of them, and there is no way of knowing at which scale each
     *     stroke was meant
     */
    public Graphics getGraphics() {
        throw new UnsupportedOperationException("getGraphics() not supported"
                + " on Multi-Resolution Images");
    }

    /** A property of the base version. */
    public Object getProperty(String name, ImageObserver observer) {
        return this.getBaseImage().getProperty(name, observer);
    }
}
