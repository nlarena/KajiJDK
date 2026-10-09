package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

/**
 * What a shape is filled with: a flat colour, a gradient or a texture.
 *
 * <p>It is the generalization of "the colour to draw with". A {@link Color} is a `Paint` that
 * answers the same at every point; a gradient answers differently depending on where the point is.
 * Drawing does not care: it asks the `Paint` for a {@link PaintContext} and asks that for pixels.
 *
 * <p>It extends {@link Transparency} because whoever draws needs to know, **before** starting,
 * whether what it will paint can let what is below show through: that decides whether it can write
 * directly or has to composite.
 */
public interface Paint extends Transparency {

    /**
     * Builds the machine that will generate the pixels.
     *
     * @param cm the format in which the destination would prefer to receive them, or `null` if it
     *     does not care; it is a suggestion and the context may return another
     * @param deviceBounds the device rectangle that will be painted
     * @param userBounds the same rectangle in user coordinates
     * @param xform from user coordinates to device coordinates
     * @param hints the quality hints
     */
    PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
            AffineTransform xform, RenderingHints hints);
}
