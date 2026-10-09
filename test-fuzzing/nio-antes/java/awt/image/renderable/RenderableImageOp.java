package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.Vector;

/**
 * KajiLibrary's java.awt.image.renderable.RenderableImageOp -- a node of the operation tree.
 *
 * <p>It joins an operation --{@link ContextualRenderedImageFactory}-- with its arguments --a
 * {@link ParameterBlock}-- and presents itself as one more {@link RenderableImage}. With that, a
 * chain of operations is simply a tree of these, and each one knows nothing about the others except
 * that they are sources.
 *
 * <h2>Nothing is computed until someone asks for pixels</h2>
 *
 * <p>The constructor does not render. Not even {@link #getWidth} renders: it asks the operation for
 * the size, which it knows how to compute without producing a single pixel. Only {@link
 * #createRendering} triggers work, and it does so <b>backwards</b>: it asks the operation what it
 * needs from each source, asks the source for it, and builds the result with what comes back.
 *
 * <p>That is the part of the package to understand: the request goes down the tree being translated
 * at each level, and the pixels come up. Without the translation
 * --{@link ContextualRenderedImageFactory#mapRenderContext}-- each level would have to be computed
 * whole.
 *
 * <h2>The block is copied when rendering</h2>
 *
 * <p>When rendering, the sources that are renderable are replaced by what they returned, and that
 * is done on a <b>copy</b> of the block. It has to be so: the same node can be rendered twice at
 * different resolutions, and overwriting its sources the first time would leave the second looking
 * at the previous one's pixels.
 */
public class RenderableImageOp implements RenderableImage {

    /** The operation. */
    private ContextualRenderedImageFactory crif;

    /** Its arguments. */
    private ParameterBlock paramBlock;

    /** The rectangle in user coordinates; asked for once and remembered. */
    private Rectangle2D boundingBox;

    /**
     * @param crif the operation
     * @param paramBlock its arguments; a copy is stored, so changing it later does not change this
     *     node behind its back
     */
    public RenderableImageOp(ContextualRenderedImageFactory crif, ParameterBlock paramBlock) {
        this.crif = crif;
        this.paramBlock = (ParameterBlock) paramBlock.clone();
    }

    /** The sources that are renderable; those that are not do not count. Null if there are none. */
    public Vector<RenderableImage> getSources() {
        return getRenderableSources();
    }

    /** The shared walk; see {@link #getSources}. */
    private Vector<RenderableImage> getRenderableSources() {
        Vector<RenderableImage> sources = null;
        if (this.paramBlock.getNumSources() > 0) {
            sources = new Vector<RenderableImage>();
            int i = 0;
            while (i < this.paramBlock.getNumSources()) {
                Object o = this.paramBlock.getSource(i);
                if (o instanceof RenderableImage) {
                    sources.addElement((RenderableImage) o);
                }
                i = i + 1;
            }
            if (sources.size() == 0) {
                sources = null;
            }
        }
        return sources;
    }

    /** It asks the operation, which computes it without rendering. */
    public Object getProperty(String name) {
        return this.crif.getProperty(this.paramBlock, name);
    }

    /** Ver {@link #getProperty}. */
    public String[] getPropertyNames() {
        return this.crif.getPropertyNames();
    }

    /** Whatever the operation answers. */
    public boolean isDynamic() {
        return this.crif.isDynamic();
    }

    /** The width in user coordinates, without rendering anything. */
    public float getWidth() {
        if (this.boundingBox == null) {
            this.boundingBox = this.crif.getBounds2D(this.paramBlock);
        }
        return (float) this.boundingBox.getWidth();
    }

    /** The height in user coordinates. */
    public float getHeight() {
        if (this.boundingBox == null) {
            this.boundingBox = this.crif.getBounds2D(this.paramBlock);
        }
        return (float) this.boundingBox.getHeight();
    }

    /** The left edge in user coordinates. */
    public float getMinX() {
        if (this.boundingBox == null) {
            this.boundingBox = this.crif.getBounds2D(this.paramBlock);
        }
        return (float) this.boundingBox.getMinX();
    }

    /** The top edge in user coordinates. */
    public float getMinY() {
        if (this.boundingBox == null) {
            this.boundingBox = this.crif.getBounds2D(this.paramBlock);
        }
        return (float) this.boundingBox.getMinY();
    }

    /**
     * Changes the arguments.
     *
     * @return the ones there were before
     */
    public ParameterBlock setParameterBlock(ParameterBlock paramBlock) {
        ParameterBlock previous = this.paramBlock;
        this.paramBlock = (ParameterBlock) paramBlock.clone();
        // The rectangle depended on the old arguments: it has to be asked for again.
        this.boundingBox = null;
        return previous;
    }

    /**
     * The arguments themselves.
     *
     * <p>This javadoc said a copy. It returns the block this node renders from, as the JDK does, so
     * changing it changes this node; the remembered rectangle is not recomputed then.
     */
    public ParameterBlock getParameterBlock() {
        return this.paramBlock;
    }

    /**
     * Renders to a size in pixels.
     *
     * <p>A 0 in width or height means "whatever comes out keeping the proportion". Both at 0 means
     * nothing and is rejected: there is no resolution to deduce. The JDK's {@code
     * RenderableImageOp} does neither: it scales by {@code w / getWidth()} and {@code h /
     * getHeight()} as given, and throws for no combination.
     *
     * @throws IllegalArgumentException if both are 0, or if either is negative
     */
    public RenderedImage createScaledRendering(int w, int h, RenderingHints hints) {
        if (w < 0 || h < 0) {
            throw new IllegalArgumentException("width and height cannot be negative");
        }
        if (w == 0 && h == 0) {
            throw new IllegalArgumentException("width and height cannot both be zero");
        }
        float width = getWidth();
        float height = getHeight();
        int targetWidth = w;
        int targetHeight = h;
        if (targetWidth == 0) {
            targetWidth = Math.round(h * (width / height));
        }
        if (targetHeight == 0) {
            targetHeight = Math.round(w * (height / width));
        }
        double sx = targetWidth / (double) width;
        double sy = targetHeight / (double) height;
        AffineTransform usr2dev = AffineTransform.getScaleInstance(sx, sy);
        return createRendering(new RenderContext(usr2dev, hints));
    }

    /** Renders without scaling: one user unit, one pixel. */
    public RenderedImage createDefaultRendering() {
        return createRendering(new RenderContext(new AffineTransform()));
    }

    /**
     * Renders with full control.
     *
     * <p>This is where the request goes down the tree; see the class note.
     *
     * @return null if some source could not produce what was asked of it
     */
    public RenderedImage createRendering(RenderContext renderContext) {
        // A copy: the same node can be rendered twice. See the class note.
        ParameterBlock rendered = (ParameterBlock) this.paramBlock.clone();
        Vector<Object> sources = this.paramBlock.getSources();
        if (sources != null && sources.size() > 0) {
            Vector<Object> renderedSources = new Vector<Object>();
            int i = 0;
            while (i < sources.size()) {
                Object o = sources.elementAt(i);
                if (o instanceof RenderableImage) {
                    RenderContext forSource =
                        this.crif.mapRenderContext(i, renderContext, this.paramBlock, this);
                    RenderedImage produced = ((RenderableImage) o).createRendering(forSource);
                    if (produced == null) {
                        return null;
                    }
                    renderedSources.addElement(produced);
                } else {
                    renderedSources.addElement(o);
                }
                i = i + 1;
            }
            rendered.setSources(renderedSources);
        }
        return this.crif.create(renderContext, rendered);
    }
}
