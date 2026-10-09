package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

/**
 * KajiLibrary's java.awt.image.renderable.RenderedImageFactory -- makes a concrete image from an
 * operation.
 *
 * <p>One method, and with it the whole package is understood. A <b>renderable</b> image has no
 * pixels: it is a description of what to do --rotate this, blend it with that-- and it does not
 * even have a size in pixels, because its coordinates are real and not integers. This interface is
 * the point where that description becomes real pixels.
 *
 * <p>That is where the model's advantage comes from: the same chain of operations can be rendered
 * small for a preview and huge for printing, without recomputing anything, because until someone
 * calls {@code create} no resolution has been decided.
 *
 * <p>Returning null is valid and means this factory cannot handle those arguments.
 */
public interface RenderedImageFactory {

    /**
     * The concrete image.
     *
     * @param paramBlock the sources and parameters of the operation
     * @param hints quality-versus-speed hints; they may be ignored
     * @return null if this factory cannot handle that
     */
    RenderedImage create(ParameterBlock paramBlock, RenderingHints hints);
}
