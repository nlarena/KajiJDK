package java.awt;

import java.awt.image.ColorModel;
import java.awt.image.Raster;

/**
 * What generates a {@link Paint}'s pixels during a drawing operation.
 *
 * <p>The separation between `Paint` and its context is what lets a gradient be described once and
 * drawn many times: the `Paint` is the **description** —two points and two colours— and the context
 * is the machine that, for a concrete transformation and colour model, produces the pixels.
 *
 * <p>It is asked for by rectangles and not by pixels because almost every gradient is computed much
 * more cheaply by rows than point by point.
 */
public interface PaintContext {

    /**
     * Releases the context's resources.
     *
     * <p>It is called every time, also when drawing failed, so it has to be callable on a context
     * that never generated a pixel.
     */
    void dispose();

    /** The format of the pixels it generates. */
    ColorModel getColorModel();

    /** The pixels of that rectangle, in device coordinates. */
    Raster getRaster(int x, int y, int w, int h);
}
