package java.awt.font;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;

/**
 * An image put into a line of text as if it were a character.
 *
 * <p>The origin says **which point of the image** rests on the line, and that is why it is given in
 * the image's coordinates and not the text's. With the origin at (0,0) the whole image hangs below
 * the baseline; with the origin in the middle, it ends up centred; with the origin at the bottom, it
 * rests like a letter.
 *
 * <p>The size is asked for once only, on construction. An image still loading would answer -1, and
 * the line's measurements would be wrong for ever: this is better built with an image that is already
 * complete.
 */
public final class ImageGraphicAttribute extends GraphicAttribute {

    private final Image image;
    private final float imageWidth;
    private final float imageHeight;
    private final float originX;
    private final float originY;

    /**
     * With the origin at the image's top left corner.
     *
     * @throws IllegalArgumentException if the alignment is none of the five
     * @throws NullPointerException if the image is `null`
     */
    public ImageGraphicAttribute(Image image, int alignment) {
        this(image, alignment, 0, 0);
    }

    /**
     * With the origin at the given point of the image.
     *
     * @throws IllegalArgumentException if the alignment is none of the five
     * @throws NullPointerException if the image is `null`
     */
    public ImageGraphicAttribute(Image image, int alignment, float originX, float originY) {
        super(alignment);
        this.image = image;
        this.imageWidth = image.getWidth(null);
        this.imageHeight = image.getHeight(null);
        this.originX = originX;
        this.originY = originY;
    }

    /** How much of the image is left above the baseline. */
    public float getAscent() {
        return Math.max(0, this.originY);
    }

    /** How much of the image is left below the baseline. */
    public float getDescent() {
        return Math.max(0, this.imageHeight - this.originY);
    }

    /** How far the line advances: whatever is left to the right of the origin. */
    public float getAdvance() {
        return Math.max(0, this.imageWidth - this.originX);
    }

    /** The image's rectangle, relative to the origin. */
    public Rectangle2D getBounds() {
        return new Rectangle2D.Float(-this.originX, -this.originY, this.imageWidth,
                this.imageHeight);
    }

    /** Draws the image with its origin at `(x, y)`. */
    public void draw(Graphics2D graphics, float x, float y) {
        graphics.drawImage(this.image, (int) (x - this.originX), (int) (y - this.originY), null);
    }

    public int hashCode() {
        return this.image.hashCode();
    }

    /** Equality by image, alignment and origin. */
    public boolean equals(Object rhs) {
        if (rhs instanceof ImageGraphicAttribute) {
            return this.equals((ImageGraphicAttribute) rhs);
        }
        return false;
    }

    /**
     * The same, with the type already known.
     *
     * <p>The image is compared by **identity**: two different images with the same pixels are two
     * objects, and comparing them pixel by pixel in an `equals` called once per stretch of text would
     * not be reasonable.
     */
    public boolean equals(ImageGraphicAttribute rhs) {
        if (rhs == null) {
            return false;
        }
        if (this == rhs) {
            return true;
        }
        if (this.originX != rhs.originX || this.originY != rhs.originY) {
            return false;
        }
        if (this.getAlignment() != rhs.getAlignment()) {
            return false;
        }
        return this.image.equals(rhs.image);
    }
}
