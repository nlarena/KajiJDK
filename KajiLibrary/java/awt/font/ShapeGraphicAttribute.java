package java.awt.font;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * A figure put into a line of text as if it were a character.
 *
 * <p>The measurements come from the figure itself: whatever the figure has above the origin is the
 * ascent, whatever it has below it the descent, and how far it reaches to the right the advance.
 * Whatever is left to the **left** of the origin does not count as advance --the three are clamped to
 * zero-- so a figure starting before the origin tucks under what came before, just like an italic
 * letter.
 *
 * <p>It can be asked for filled or stroked. Stroked takes one pixel more in width and in height,
 * because the stroke is drawn **on** the edge and sticks out half a pixel on each side.
 */
public final class ShapeGraphicAttribute extends GraphicAttribute {

    /** That the figure be drawn stroked. */
    public static final boolean STROKE = true;

    /** That the figure be drawn filled. */
    public static final boolean FILL = false;

    private final Shape shape;
    private final boolean stroke;
    private final Rectangle2D shapeBounds;

    /**
     * With the figure, the alignment and whether it goes stroked or filled.
     *
     * @throws IllegalArgumentException if the alignment is none of the five
     * @throws NullPointerException if the figure is `null`
     */
    public ShapeGraphicAttribute(Shape shape, int alignment, boolean stroke) {
        super(alignment);
        this.shape = shape;
        this.stroke = stroke;
        this.shapeBounds = this.shape.getBounds2D();
    }

    /** How far the figure rises above the origin. */
    public float getAscent() {
        return (float) Math.max(0, -this.shapeBounds.getMinY());
    }

    /** How far the figure drops below the origin. */
    public float getDescent() {
        return (float) Math.max(0, this.shapeBounds.getMaxY());
    }

    /** How far the figure reaches to the right of the origin. */
    public float getAdvance() {
        return (float) Math.max(0, this.shapeBounds.getMaxX());
    }

    /**
     * Draws the figure with its origin at `(x, y)`.
     *
     * <p>The shift is done and undone on the context that is received, and undone in a `finally`: if
     * the drawing throws, the context is left as it was and does not drag the offset into the rest of
     * the line.
     */
    public void draw(Graphics2D graphics, float x, float y) {
        graphics.translate((int) x, (int) y);
        try {
            if (this.stroke == STROKE) {
                Stroke oldStroke = graphics.getStroke();
                graphics.setStroke(new BasicStroke());
                graphics.draw(this.shape);
                graphics.setStroke(oldStroke);
            } else {
                graphics.fill(this.shape);
            }
        } finally {
            graphics.translate(-(int) x, -(int) y);
        }
    }

    /**
     * Where the ink falls.
     *
     * <p>Stroked takes one pixel more in width and in height, because the stroke is drawn on the
     * edge.
     */
    public Rectangle2D getBounds() {
        Rectangle2D.Float bounds = new Rectangle2D.Float();
        bounds.setRect(this.shapeBounds);
        if (this.stroke == STROKE) {
            bounds.width = bounds.width + 1;
            bounds.height = bounds.height + 1;
        }
        return bounds;
    }

    /**
     * The figure's outline, transformed.
     *
     * <p>It is overridden because here there really is an outline: the figure itself, and not its
     * box.
     */
    public Shape getOutline(AffineTransform tx) {
        if (tx == null) {
            return this.shape;
        }
        return tx.createTransformedShape(this.shape);
    }

    public int hashCode() {
        return this.shape.hashCode();
    }

    /** Equality by figure, alignment and drawing mode. */
    public boolean equals(Object rhs) {
        if (rhs instanceof ShapeGraphicAttribute) {
            return this.equals((ShapeGraphicAttribute) rhs);
        }
        return false;
    }

    /** The same, with the type already known. */
    public boolean equals(ShapeGraphicAttribute rhs) {
        if (rhs == null) {
            return false;
        }
        if (this == rhs) {
            return true;
        }
        if (this.stroke != rhs.stroke) {
            return false;
        }
        if (this.getAlignment() != rhs.getAlignment()) {
            return false;
        }
        return this.shape.equals(rhs.shape);
    }
}
