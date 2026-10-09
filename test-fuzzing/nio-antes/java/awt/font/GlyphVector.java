package java.awt.font;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A run of glyphs already placed, ready to be drawn.
 *
 * <p>It is the result of having settled everything a text has that is hard: which glyph corresponds
 * to each character, which ones join into a ligature, in what order they go if the line mixes
 * directions, and where each one falls. After that there are no characters left, there are drawings
 * with coordinates.
 *
 * <p>A glyph is not a character, and that is why {@link #getGlyphCharIndex} exists: a ligature is one
 * glyph for two characters, a loose accent may be a glyph for none, and in a bidirectional line the
 * glyphs' order is not the characters'. The correspondence is kept because it is needed to put the
 * caret where the user believes they clicked.
 *
 * <p>There are two rectangles per glyph and they are best not confused. The **logical** one is the
 * place it takes up for the purposes of selection and of the line; the **visual** one, where the ink
 * falls. The first includes the air at the sides and the second does not.
 */
public abstract class GlyphVector implements Cloneable {

    /** Some glyph has a transform of its own. */
    public static final int FLAG_HAS_TRANSFORMS = 1;

    /** Some position was corrected with respect to the one the advance would give. */
    public static final int FLAG_HAS_POSITION_ADJUSTMENTS = 2;

    /** The run goes right to left. */
    public static final int FLAG_RUN_RTL = 4;

    /** The correspondence between glyphs and characters is not one to one in order. */
    public static final int FLAG_COMPLEX_GLYPHS = 8;

    /** The bits the flags above use. */
    public static final int FLAG_MASK = FLAG_HAS_TRANSFORMS | FLAG_HAS_POSITION_ADJUSTMENTS
            | FLAG_RUN_RTL | FLAG_COMPLEX_GLYPHS;

    /** For the subclasses. */
    protected GlyphVector() {
    }

    /** The font the glyphs came out of. */
    public abstract Font getFont();

    /** The conditions it was built under. */
    public abstract FontRenderContext getFontRenderContext();

    /** Puts the glyphs back in their default positions. */
    public abstract void performDefaultLayout();

    /** How many glyphs there are. */
    public abstract int getNumGlyphs();

    /** That glyph's code inside its font. */
    public abstract int getGlyphCode(int glyphIndex);

    /** The codes of a run of glyphs. */
    public abstract int[] getGlyphCodes(int beginGlyphIndex, int numEntries, int[] codeReturn);

    /**
     * Which character gave rise to that glyph.
     *
     * <p>The implementation here assumes the trivial correspondence —glyph `i` comes from character
     * `i`—, which is the right one as long as there are no ligatures and no reordering. A subclass
     * that lays out complex text has to override it.
     */
    public int getGlyphCharIndex(int glyphIndex) {
        return glyphIndex;
    }

    /**
     * The same for a run.
     *
     * @throws IllegalArgumentException if `numEntries` is negative
     */
    public int[] getGlyphCharIndices(int beginGlyphIndex, int numEntries, int[] codeReturn) {
        if (numEntries < 0) {
            throw new IllegalArgumentException("numEntries must be >= 0");
        }
        int[] out = codeReturn;
        if (out == null) {
            out = new int[numEntries];
        }
        for (int i = 0; i < numEntries; i++) {
            out[i] = this.getGlyphCharIndex(beginGlyphIndex + i);
        }
        return out;
    }

    /** The rectangle the run takes up for the purposes of the line and of selection. */
    public abstract Rectangle2D getLogicalBounds();

    /** The rectangle where the ink falls. */
    public abstract Rectangle2D getVisualBounds();

    /**
     * The pixels the run drawn at `(x, y)` is going to touch.
     *
     * <p>It is the visual rectangle rounded outwards: a pixel half touched is a pixel touched.
     */
    public Rectangle getPixelBounds(FontRenderContext renderFRC, float x, float y) {
        return roundOutwards(this.getVisualBounds(), x, y);
    }

    /** The whole pixel rectangle covering a continuous rectangle shifted by `(x, y)`. */
    private static Rectangle roundOutwards(Rectangle2D rect, float x, float y) {
        int l = (int) Math.floor(rect.getX() + x);
        int t = (int) Math.floor(rect.getY() + y);
        int r = (int) Math.ceil(rect.getMaxX() + x);
        int b = (int) Math.ceil(rect.getMaxY() + y);
        return new Rectangle(l, t, r - l, b - t);
    }

    /** The whole run's outline. */
    public abstract Shape getOutline();

    /** The whole run's outline, shifted to `(x, y)`. */
    public abstract Shape getOutline(float x, float y);

    /** One glyph's outline. */
    public abstract Shape getGlyphOutline(int glyphIndex);

    /** One glyph's outline, shifted to `(x, y)`. */
    public Shape getGlyphOutline(int glyphIndex, float x, float y) {
        Shape s = this.getGlyphOutline(glyphIndex);
        AffineTransform at = AffineTransform.getTranslateInstance(x, y);
        return at.createTransformedShape(s);
    }

    /**
     * Where that glyph is.
     *
     * <p>An index equal to the number of glyphs is admitted: that is the position the next one would
     * go at, that is, the end of the run.
     */
    public abstract Point2D getGlyphPosition(int glyphIndex);

    /** Moves a glyph. */
    public abstract void setGlyphPosition(int glyphIndex, Point2D newPos);

    /** That glyph's own transform, or `null` if it is the identity. */
    public abstract AffineTransform getGlyphTransform(int glyphIndex);

    /** Gives a glyph a transform of its own. */
    public abstract void setGlyphTransform(int glyphIndex, AffineTransform newTX);

    /**
     * The flags describing the run.
     *
     * <p>The implementation here returns 0, which is true for a simple run. A subclass admitting
     * per-glyph transforms or complex text has to override it.
     */
    public int getLayoutFlags() {
        return 0;
    }

    /** The positions of a run of glyphs, as pairs. */
    public abstract float[] getGlyphPositions(int beginGlyphIndex, int numEntries,
            float[] positionReturn);

    /** The place a glyph takes up for the purposes of selection. */
    public abstract Shape getGlyphLogicalBounds(int glyphIndex);

    /** Where a glyph's ink falls. */
    public abstract Shape getGlyphVisualBounds(int glyphIndex);

    /** The pixels a glyph drawn at `(x, y)` is going to touch. */
    public Rectangle getGlyphPixelBounds(int index, FontRenderContext renderFRC, float x,
            float y) {
        return roundOutwards(this.getGlyphVisualBounds(index).getBounds2D(), x, y);
    }

    /** A glyph's measurements. */
    public abstract GlyphMetrics getGlyphMetrics(int glyphIndex);

    /** How a glyph stretches or shrinks when justifying. */
    public abstract GlyphJustificationInfo getGlyphJustificationInfo(int glyphIndex);

    /** Equality by font, conditions, codes and positions. */
    public abstract boolean equals(GlyphVector set);
}
