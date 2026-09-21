package java.awt;

import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * Drawing in continuous coordinates, with a transform, a stroke, a paint and a composite.
 *
 * <p>It is {@link Graphics} carried from pixels to geometry, and the jump is not one of convenience
 * but of model. There there was a colour and some integer coordinates; here there are four things
 * that combine on every operation:
 *
 * <ul>
 *   <li>the <strong>transform</strong>, which says where the user coordinates land;
 *   <li>the <strong>stroke</strong> ({@link Stroke}), which turns a line into the figure of its
 *       thickness, its ends and its dashing;
 *   <li>the <strong>paint</strong> ({@link Paint}), which decides what colour each point is and can
 *       therefore be a gradient or a texture and not only a colour;
 *   <li>the <strong>composite</strong> ({@link Composite}), which says how what is drawn mixes with
 *       what was there.
 * </ul>
 *
 * <p>From there comes the symmetry of the class: {@link #draw} is filling the figure the stroke
 * generates out of the outline, and {@link #fill} is filling the figure itself. One single
 * underlying operation with two different inputs.
 *
 * <p>The colour and the paint are the same state seen in two ways. `setColor` is `setPaint` with a
 * colour, and `getPaint` after a `setColor` returns that colour; but `getColor` after a gradient
 * returns the last plain colour, not the gradient. That explains why {@link #draw3DRect} saves the
 * paint and not the colour before touching anything.
 */
public abstract class Graphics2D extends Graphics {

    /** For the subclasses. */
    protected Graphics2D() {
    }

    /**
     * A rectangle in relief.
     *
     * <p>It is overridden with respect to {@link Graphics} because here the state that has to be
     * preserved is the **paint** and not the colour: if a gradient was set, restoring only the
     * colour would lose it.
     */
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        Paint p = this.getPaint();
        Color c = this.getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        this.setColor(raised ? brighter : darker);
        this.fillRect(x, y, 1, height + 1);
        this.fillRect(x + 1, y, width - 1, 1);
        this.setColor(raised ? darker : brighter);
        this.fillRect(x + 1, y + height, width, 1);
        this.fillRect(x + width, y, 1, height);
        this.setPaint(p);
    }

    /** A filled rectangle in relief. */
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        Paint p = this.getPaint();
        Color c = this.getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        if (!raised) {
            this.setColor(darker);
        } else if (p != c) {
            this.setColor(c);
        }
        this.fillRect(x + 1, y + 1, width - 2, height - 2);
        this.setColor(raised ? brighter : darker);
        this.fillRect(x, y, 1, height);
        this.fillRect(x + 1, y, width - 2, 1);
        this.setColor(raised ? darker : brighter);
        this.fillRect(x + 1, y + height - 1, width - 1, 1);
        this.fillRect(x + width - 1, y, 1, height - 1);
        this.setPaint(p);
    }

    /** The outline of a shape, with the current stroke. */
    public abstract void draw(Shape s);

    /** Draws a transformed image. */
    public abstract boolean drawImage(Image img, AffineTransform xform, ImageObserver obs);

    /** Applies an operation to an image and draws it at `(x, y)`. */
    public abstract void drawImage(BufferedImage img, BufferedImageOp op, int x, int y);

    /** Draws an already rasterised image, transformed. */
    public abstract void drawRenderedImage(RenderedImage img, AffineTransform xform);

    /**
     * Draws an image with no resolution of its own, transformed.
     *
     * <p>The image is rasterised **at the scale it is going to end up at**, so enlarging it does
     * not pixelate: it is drawn again, bigger.
     */
    public abstract void drawRenderableImage(RenderableImage img, AffineTransform xform);

    /** Draws a text with the start of the baseline at `(x, y)`. */
    public abstract void drawString(String str, int x, int y);

    /** The same, in continuous coordinates. */
    public abstract void drawString(String str, float x, float y);

    /** Draws a text with attributes. */
    public abstract void drawString(AttributedCharacterIterator iterator, int x, int y);

    /** The same, in continuous coordinates. */
    public abstract void drawString(AttributedCharacterIterator iterator, float x, float y);

    /**
     * Draws glyphs that are already placed.
     *
     * <p>It is the low road: here there are no characters left to interpret nor text to lay out,
     * only drawings with coordinates. It serves to draw the same text twice without laying it out
     * again.
     */
    public abstract void drawGlyphVector(GlyphVector g, float x, float y);

    /** Fills a shape with the current paint. */
    public abstract void fill(Shape s);

    /**
     * Whether a shape touches that rectangle of the device.
     *
     * <p>With `onStroke` the question is about the stroked outline instead of about the inside,
     * which is the difference between hitting a thin line and hitting what it encloses.
     */
    public abstract boolean hit(Rectangle rect, Shape s, boolean onStroke);

    /** The configuration of the device being drawn on. */
    public abstract GraphicsConfiguration getDeviceConfiguration();

    /** Changes how what is drawn mixes with what was there. */
    public abstract void setComposite(Composite comp);

    /** Changes what things are painted with. */
    public abstract void setPaint(Paint paint);

    /** Changes the thickness, the ends and the dashing of the lines. */
    public abstract void setStroke(Stroke s);

    /** Changes a quality hint. */
    public abstract void setRenderingHint(RenderingHints.Key hintKey, Object hintValue);

    /** The value of a hint, or `null` if it is not set. */
    public abstract Object getRenderingHint(RenderingHints.Key hintKey);

    /** Replaces every hint. */
    public abstract void setRenderingHints(Map<?, ?> hints);

    /** Adds hints without erasing the ones already there. */
    public abstract void addRenderingHints(Map<?, ?> hints);

    /** Every hint. */
    public abstract RenderingHints getRenderingHints();

    /** Shifts the origin. */
    public abstract void translate(int x, int y);

    /** Shifts the origin, in continuous coordinates. */
    public abstract void translate(double tx, double ty);

    /** Rotates around the origin, in radians and clockwise. */
    public abstract void rotate(double theta);

    /** Rotates around that point. */
    public abstract void rotate(double theta, double x, double y);

    /** Scales both axes. */
    public abstract void scale(double sx, double sy);

    /** Shears both axes. */
    public abstract void shear(double shx, double shy);

    /** Composes a transform with the one already there. */
    public abstract void transform(AffineTransform Tx);

    /**
     * Replaces the whole transform.
     *
     * <p>Dangerous on a borrowed context: it discards the one that was there, including the one the
     * system set to place the component. For changes of one's own there is {@link #transform}.
     */
    public abstract void setTransform(AffineTransform Tx);

    /** The current transform. */
    public abstract AffineTransform getTransform();

    /** What things are painted with. */
    public abstract Paint getPaint();

    /** How what is drawn mixes with what was there. */
    public abstract Composite getComposite();

    /**
     * Changes the background colour, which is the one {@link Graphics#clearRect} uses.
     *
     * <p>It is not the colour things are drawn with: it is what things are erased with.
     */
    public abstract void setBackground(Color color);

    /** The background colour. */
    public abstract Color getBackground();

    /** The current stroke. */
    public abstract Stroke getStroke();

    /**
     * Reduces the clip to the intersection with that shape.
     *
     * <p>Like {@link Graphics#clipRect}, it never enlarges it.
     */
    public abstract void clip(Shape s);

    /** The conditions text is going to be measured and drawn under. */
    public abstract FontRenderContext getFontRenderContext();
}
