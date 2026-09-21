package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.util.Vector;

/**
 * KajiLibrary's java.awt.image.renderable.RenderableImage -- an image without resolution.
 *
 * <p>The contrast with {@code RenderedImage} is the whole point of the package, and it is in the
 * types: there {@code getWidth()} returns an {@code int} because they are pixels, and here it
 * returns a {@code float} because they are <b>user coordinates</b>. A renderable image describes
 * something --a blurred ellipse, a map of a country-- without committing to a size.
 *
 * <p>The resolution is chosen when asking for the rendering, and that is why there are three ways
 * to ask for it: {@link #createScaledRendering} for a size in pixels, {@link
 * #createDefaultRendering} for the one the image considers natural, and {@link #createRendering}
 * for full control with a {@link RenderContext}.
 *
 * <p>{@link #getSources} returns the images this one is computed from: a renderable image is
 * normally the node of an operation tree, not a lone piece of data.
 *
 * <p>{@link #HINTS_OBSERVED} is the name of a property, not a flag: if the rendering has it, its
 * value says which of the requested hints were really honoured. It exists because hints are
 * <b>hints</b>, and an implementation can ignore all of them without saying so.
 */
public interface RenderableImage {

    /** The name of the property that says which hints were honoured. */
    static final String HINTS_OBSERVED = "HINTS_OBSERVED";

    /**
     * The images this one is computed from: empty if it has none, or null if that is not known.
     * (This javadoc said only "empty if it is a source"; the JDK allows null, and {@link
     * RenderableImageOp} returns null when it has no renderable source.)
     */
    Vector<RenderableImage> getSources();

    /**
     * A property of the image.
     *
     * @return {@code java.awt.Image.UndefinedProperty} if it does not have it
     */
    Object getProperty(String name);

    /** The names of the properties, or null. */
    String[] getPropertyNames();

    /** Whether two equal renderings can give different results. */
    boolean isDynamic();

    /** The width in user coordinates. See the class note on why it is not an integer. */
    float getWidth();

    /** The height in user coordinates. */
    float getHeight();

    /** The left edge in user coordinates. */
    float getMinX();

    /** The top edge in user coordinates. */
    float getMinY();

    /**
     * Renders to a size in pixels.
     *
     * <p>One of the two can be 0 to say "whatever comes out keeping the proportion". Both at 0
     * means nothing, and the contract makes it an {@link IllegalArgumentException}.
     */
    RenderedImage createScaledRendering(int w, int h, RenderingHints hints);

    /** Renders at the size the image considers natural. */
    RenderedImage createDefaultRendering();

    /** Renders with full control. */
    RenderedImage createRendering(RenderContext renderContext);
}
