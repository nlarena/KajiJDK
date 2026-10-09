package java.awt;

import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

/**
 * A surface that is drawn on, with its state.
 *
 * <p>It is not only a destination: it is a destination **plus** the colour, the font, the clip and
 * the shift of the origin. That is why {@link #create()} exists and gets used so much — it returns
 * another view of the same destination with a copy of the state, so that a component can change the
 * colour and the clip as it likes without dirtying anything for whoever called it.
 *
 * <p>The clip is **cumulative**: {@link #clipRect} intersects it with whatever was there and never
 * enlarges it. It is what keeps a child from painting outside its parent however hard it tries, and
 * that is why one has to keep the one that is handed over instead of setting one's own.
 *
 * <p>Every drawing goes through the baseline of the "mode": in paint mode the colour replaces the
 * one that was there, and in XOR mode it is combined with it, so that drawing the same thing twice
 * leaves the surface as it was. That last part is what made it possible to draw a cursor or a
 * rubber band selection without saving the background.
 *
 * <p>The coordinates are integers and the edges are painted: a 3×3 {@link #drawRect} marks a square
 * of 4×4 pixels, because it draws the line that **surrounds** the rectangle. A 3×3 {@link
 * #fillRect} paints 9. The difference is old and surprises every time.
 */
public abstract class Graphics {

    /** For the subclasses. */
    protected Graphics() {
    }

    /** Another view of the same destination, with a copy of this state. */
    public abstract Graphics create();

    /**
     * Another view with the origin shifted and the clip reduced to that rectangle.
     *
     * <p>The rectangle is given in the coordinates of **this** context, and in the one that comes
     * out its top-left corner becomes the origin.
     */
    public Graphics create(int x, int y, int width, int height) {
        Graphics g = this.create();
        if (g == null) {
            return null;
        }
        g.translate(x, y);
        g.clipRect(0, 0, width, height);
        return g;
    }

    /** Shifts the origin. */
    public abstract void translate(int x, int y);

    /** The colour things are drawn with. */
    public abstract Color getColor();

    /** Changes the colour things are drawn with. */
    public abstract void setColor(Color c);

    /** Sets the mode in which the colour replaces what was there. */
    public abstract void setPaintMode();

    /**
     * Sets the mode in which the colour is combined with what was there.
     *
     * <p>A pixel of the current colour goes to the alternation colour and the other way round; the
     * rest change in a way that undoes itself when the drawing is repeated. Hence its being good
     * for whatever has to be erased afterwards.
     */
    public abstract void setXORMode(Color c1);

    /** The font text is drawn with. */
    public abstract Font getFont();

    /** Changes the font text is drawn with. */
    public abstract void setFont(Font font);

    /** The measures of the current font. */
    public FontMetrics getFontMetrics() {
        return this.getFontMetrics(this.getFont());
    }

    /** The measures of that font on this destination. */
    public abstract FontMetrics getFontMetrics(Font f);

    /** The rectangle that encloses the clip, or `null` if there is no clip. */
    public abstract Rectangle getClipBounds();

    /** Reduces the clip to the intersection with that rectangle. */
    public abstract void clipRect(int x, int y, int width, int height);

    /**
     * Sets the clip to that rectangle.
     *
     * <p>Unlike {@link #clipRect}, this **can enlarge** the clip, so using it on a borrowed context
     * lets a component paint outside its own area.
     */
    public abstract void setClip(int x, int y, int width, int height);

    /** The clip, or `null` if there is none. */
    public abstract Shape getClip();

    /** Sets the clip to that shape. */
    public abstract void setClip(Shape clip);

    /**
     * Copies a rectangle of the destination to another place of the same destination.
     *
     * <p>Whatever is copied from outside the clip, or from a part that was covered, is left
     * undefined: there is nowhere to take those pixels from.
     */
    public abstract void copyArea(int x, int y, int width, int height, int dx, int dy);

    /** A line between two points, with both ends included. */
    public abstract void drawLine(int x1, int y1, int x2, int y2);

    /** Fills a rectangle with the current colour. */
    public abstract void fillRect(int x, int y, int width, int height);

    /**
     * The outline of a rectangle.
     *
     * <p>It covers `width + 1` by `height + 1` pixels: the line surrounds the rectangle.
     */
    public void drawRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            return;
        }
        if (height == 0 || width == 0) {
            this.drawLine(x, y, x + width, y + height);
        } else {
            this.drawLine(x, y, x + width - 1, y);
            this.drawLine(x + width, y, x + width, y + height - 1);
            this.drawLine(x + width, y + height, x + 1, y + height);
            this.drawLine(x, y + height, x, y + 1);
        }
    }

    /** Fills a rectangle with the background colour. */
    public abstract void clearRect(int x, int y, int width, int height);

    /** The outline of a rectangle with rounded corners. */
    public abstract void drawRoundRect(int x, int y, int width, int height, int arcWidth,
            int arcHeight);

    /** Fills a rectangle with rounded corners. */
    public abstract void fillRoundRect(int x, int y, int width, int height, int arcWidth,
            int arcHeight);

    /**
     * A rectangle in relief.
     *
     * <p>The relief is made with two shades of the current colour: the light one on the sides
     * facing the light and the dark one on the others. Swapping them is what makes it look sunken
     * instead of raised.
     */
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        Color c = this.getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        this.setColor(raised ? brighter : darker);
        this.drawLine(x, y, x, y + height);
        this.drawLine(x + 1, y, x + width - 1, y);
        this.setColor(raised ? darker : brighter);
        this.drawLine(x + 1, y + height, x + width, y + height);
        this.drawLine(x + width, y, x + width, y + height - 1);
        this.setColor(c);
    }

    /** A filled rectangle in relief. */
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        Color c = this.getColor();
        Color brighter = c.brighter();
        Color darker = c.darker();
        if (!raised) {
            this.setColor(darker);
        }
        this.fillRect(x + 1, y + 1, width - 2, height - 2);
        this.setColor(raised ? brighter : darker);
        this.drawLine(x, y, x, y + height - 1);
        this.drawLine(x + 1, y, x + width - 2, y);
        this.setColor(raised ? darker : brighter);
        this.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        this.drawLine(x + width - 1, y, x + width - 1, y + height - 2);
        this.setColor(c);
    }

    /** The outline of an oval inscribed in that rectangle. */
    public abstract void drawOval(int x, int y, int width, int height);

    /** Fills an oval inscribed in that rectangle. */
    public abstract void fillOval(int x, int y, int width, int height);

    /**
     * An arc of an oval.
     *
     * <p>The angles are in degrees, with zero at three o'clock and growing anticlockwise.
     */
    public abstract void drawArc(int x, int y, int width, int height, int startAngle,
            int arcAngle);

    /** Fills a sector of an oval. */
    public abstract void fillArc(int x, int y, int width, int height, int startAngle,
            int arcAngle);

    /** A sequence of segments, not closed. */
    public abstract void drawPolyline(int[] xPoints, int[] yPoints, int nPoints);

    /** The outline of a polygon, closed. */
    public abstract void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);

    /** The outline of a polygon. */
    public void drawPolygon(Polygon p) {
        this.drawPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    /** Fills a polygon. */
    public abstract void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);

    /** Fills a polygon. */
    public void fillPolygon(Polygon p) {
        this.fillPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    /**
     * Draws a text.
     *
     * <p>`(x, y)` is the start of the **baseline**, not the corner: the text goes up above that
     * point.
     */
    public abstract void drawString(String str, int x, int y);

    /** Draws a text with attributes. */
    public abstract void drawString(AttributedCharacterIterator iterator, int x, int y);

    /** Draws a stretch of an array of characters. */
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        this.drawString(new String(data, offset, length), x, y);
    }

    /**
     * Draws a stretch of bytes, taking each one as a character.
     *
     * @deprecated it does not translate bytes into characters correctly in any encoding other than
     *     Latin-1. It is kept because it has been in the API since 1.0.
     */
    @Deprecated
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        this.drawString(new String(data, offset, length), x, y);
    }

    /**
     * Draws an image with its top-left corner at `(x, y)`.
     *
     * <p>It returns `false` if the image is not whole yet; the observer will find out when the rest
     * arrives.
     */
    public abstract boolean drawImage(Image img, int x, int y, ImageObserver observer);

    /** Draws an image scaled to that size. */
    public abstract boolean drawImage(Image img, int x, int y, int width, int height,
            ImageObserver observer);

    /** Draws an image painting whatever is transparent in `bgcolor`. */
    public abstract boolean drawImage(Image img, int x, int y, Color bgcolor,
            ImageObserver observer);

    /** Draws an image scaled, painting whatever is transparent in `bgcolor`. */
    public abstract boolean drawImage(Image img, int x, int y, int width, int height,
            Color bgcolor, ImageObserver observer);

    /**
     * Draws a cut-out of an image inside a rectangle of the destination.
     *
     * <p>If the rectangles do not measure the same, the image is stretched; if a pair of
     * coordinates is given the other way round, it is mirrored. That mirroring is on purpose and is
     * the only way of flipping an image with this API.
     */
    public abstract boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1,
            int sy1, int sx2, int sy2, ImageObserver observer);

    /** Like the previous one, painting whatever is transparent in `bgcolor`. */
    public abstract boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1,
            int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer);

    /**
     * Releases the resources of this context.
     *
     * <p>It has to be called for every context asked for with {@link #create()}. Using it
     * afterwards is an error.
     */
    public abstract void dispose();

    /**
     * Releases the resources.
     *
     * @deprecated it depends on garbage collection, which gives no guarantee of when it is going to
     *     run or that it is going to run at all. {@link #dispose} has to be called by hand.
     */
    @Deprecated
    public void finalize() {
        this.dispose();
    }

    public String toString() {
        return this.getClass().getName() + "[font=" + this.getFont() + ",color="
                + this.getColor() + "]";
    }

    /**
     * The rectangle of the clip.
     *
     * @deprecated the name does not say that it returns the rectangle that **encloses** the clip,
     *     which may not be a rectangle. Use {@link #getClipBounds}.
     */
    @Deprecated
    public Rectangle getClipRect() {
        return this.getClipBounds();
    }

    /**
     * Whether that rectangle touches the clip.
     *
     * <p>It may give `true` too often but never `false` too often: it serves to skip a drawing that
     * is surely not seen, not to know whether something is seen.
     */
    public boolean hitClip(int x, int y, int width, int height) {
        Rectangle clipRect = this.getClipBounds();
        if (clipRect == null) {
            return true;
        }
        return clipRect.intersects(x, y, width, height);
    }

    /**
     * The rectangle of the clip, written into the one that is passed in.
     *
     * <p>It exists so as not to create an object per query in a drawing loop.
     */
    public Rectangle getClipBounds(Rectangle r) {
        Rectangle clipRect = this.getClipBounds();
        if (clipRect != null) {
            r.x = clipRect.x;
            r.y = clipRect.y;
            r.width = clipRect.width;
            r.height = clipRect.height;
        } else if (r == null) {
            throw new NullPointerException("null rectangle parameter");
        }
        return r;
    }
}
