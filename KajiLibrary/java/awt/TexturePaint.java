package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

/**
 * Fills with an image repeated as a tile.
 *
 * <p>The anchor rectangle says **where and how big** one copy of the image goes; from there it is
 * repeated in both directions until it covers what is needed. The image is stretched to the
 * rectangle, so the same drawing serves for tiles of any size.
 *
 * <p>That the anchor is in user coordinates and not the shape's is what makes two different shapes
 * painted with the same texture **line up with each other**: the pattern belongs to the plane, not
 * to what is being filled.
 */
public class TexturePaint implements Paint {

    private final BufferedImage bufImg;
    private final double tx;
    private final double ty;
    private final double sx;
    private final double sy;

    /**
     * With the image and the rectangle where one copy goes.
     *
     * @throws NullPointerException if the image or the rectangle is missing
     */
    public TexturePaint(BufferedImage txtr, Rectangle2D anchor) {
        this.bufImg = txtr;
        this.tx = anchor.getX();
        this.ty = anchor.getY();
        this.sx = anchor.getWidth() / this.bufImg.getWidth();
        this.sy = anchor.getHeight() / this.bufImg.getHeight();
    }

    /** The image that is repeated. */
    public BufferedImage getImage() {
        return this.bufImg;
    }

    /** Where one copy of the image goes. */
    public Rectangle2D getAnchorRect() {
        return new Rectangle2D.Double(this.tx, this.ty, this.sx * this.bufImg.getWidth(),
                this.sy * this.bufImg.getHeight());
    }

    /**
     * The transparency of the image's colour model: `OPAQUE`, `BITMASK` or `TRANSLUCENT`.
     *
     * <p>The image's colour model is asked: an image without an alpha channel covers what is below,
     * and knowing that lets drawing skip compositing.
     */
    public int getTransparency() {
        return this.bufImg.getColorModel().getTransparency();
    }

    /**
     * Builds the machine that generates the pixels.
     *
     * <p>If the transformation cannot be inverted, the texture degrades to a transparent flat
     * colour: with no geometry there is no tile to place, and painting an invented colour would be
     * worse than not painting.
     */
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        try {
            return new TextureContext(xform);
        } catch (NoninvertibleTransformException e) {
            return new Color(0, 0, 0, 0).createContext(cm, deviceBounds, userBounds, xform, hints);
        }
    }

    /** The context that places each point inside the tile. */
    private final class TextureContext extends RasterPaintContext {

        TextureContext(AffineTransform xform) throws NoninvertibleTransformException {
            super(xform);
        }

        int colorAt(double ux, double uy) {
            TexturePaint p = TexturePaint.this;
            // The remainder of the division places the point inside a tile. If it comes out
            // negative the width is added, because Java's remainder keeps the sign, and without
            // that negative coordinates would fall outside the image. (This comment said the
            // remainder is taken again.)
            double ax = (ux - p.tx) / p.sx;
            double ay = (uy - p.ty) / p.sy;
            int w = p.bufImg.getWidth();
            int h = p.bufImg.getHeight();
            int ix = (int) Math.floor(ax) % w;
            int iy = (int) Math.floor(ay) % h;
            if (ix < 0) {
                ix = ix + w;
            }
            if (iy < 0) {
                iy = iy + h;
            }
            return p.bufImg.getRGB(ix, iy);
        }
    }
}
