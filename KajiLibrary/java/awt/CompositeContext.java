package java.awt;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * What really blends the pixels during a drawing operation.
 *
 * <p>It is to {@link Composite} what {@link PaintContext} is to {@link Paint}: the `Composite`
 * describes the rule and the context applies it, already knowing which pixel formats it will work
 * with.
 */
public interface CompositeContext {

    /** Releases the context's resources. */
    void dispose();

    /**
     * Blends the source with the destination and writes the result.
     *
     * <p>`dstOut` can be the same object as `dstIn`, and in fact usually is: compositing onto the
     * surface is the normal case.
     */
    void compose(Raster src, Raster dstIn, WritableRaster dstOut);
}
