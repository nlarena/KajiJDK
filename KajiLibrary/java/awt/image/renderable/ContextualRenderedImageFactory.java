package java.awt.image.renderable;

import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;

/**
 * KajiLibrary's java.awt.image.renderable.ContextualRenderedImageFactory -- an operation that knows
 * how much it needs from its sources.
 *
 * <p>It adds to {@link RenderedImageFactory} what is needed for a chain of operations to be
 * evaluated <b>in parts</b>. The key piece is {@link #mapRenderContext}: given what is wanted from
 * the output, it says what is needed from the input.
 *
 * <p>Without it, asking for a thousand-pixel crop from the end of a chain of ten filters would
 * force computing the ten whole images. With it, each operation translates the request backwards
 * and only the region actually used is computed. It is the difference between being able to work
 * with a gigapixel image and not.
 *
 * <p>The translation is almost never the identity. A blur of radius five needs five <b>extra</b>
 * pixels on each edge so that the edge of the crop does not come out wrong, and a rotation needs a
 * quadrilateral and not a rectangle. That is why the method returns a new {@link RenderContext} and
 * not a rectangle.
 *
 * <p>{@link #isDynamic} tells whether the operation can give different results with the same
 * arguments --because it reads from a live source, for example--. It is what tells the system
 * whether the result can be cached.
 */
public interface ContextualRenderedImageFactory extends RenderedImageFactory {

    /**
     * What is needed from a source to be able to produce what is asked.
     *
     * @param i which of the sources
     * @param renderContext what is wanted from the output
     * @return what has to be asked of that source; see the class note
     */
    RenderContext mapRenderContext(int i, RenderContext renderContext, ParameterBlock paramBlock,
                                   RenderableImage image);

    /** The concrete image for that context. */
    RenderedImage create(RenderContext renderContext, ParameterBlock paramBlock);

    /**
     * The rectangle the output occupies, in <b>real</b> coordinates.
     *
     * <p>Real and not integer because a renderable image has no resolution; see
     * {@link RenderedImageFactory}.
     */
    Rectangle2D getBounds2D(ParameterBlock paramBlock);

    /**
     * A property of the output, computed without rendering.
     *
     * @return {@code java.awt.Image.UndefinedProperty} if it does not have it
     */
    Object getProperty(ParameterBlock paramBlock, String name);

    /** The names of the properties it can answer, or null if it has none. */
    String[] getPropertyNames();

    /** Whether two equal renderings can give different results. See the class note. */
    boolean isDynamic();
}
