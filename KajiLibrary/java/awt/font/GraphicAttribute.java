package java.awt.font;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * A drawing that takes a character's place inside a line of text.
 *
 * <p>It is how an image, a bullet or a figure is put in the middle of a sentence and it goes on being
 * text: the object declares how far it rises, how far it drops and how far it advances, and the line
 * builder treats it as one more glyph.
 *
 * <p>The only thing that does not behave like a glyph is the **alignment**. A glyph always rests on
 * its baseline; a drawing may also align with the top or the bottom of the line
 * ({@link #TOP_ALIGNMENT}, {@link #BOTTOM_ALIGNMENT}), which is what is needed for a tall image not
 * to hang from the baseline as if it were a letter.
 */
public abstract class GraphicAttribute {

    /** It aligns with the top of the line. */
    public static final int TOP_ALIGNMENT = -1;

    /** It aligns with the bottom of the line. */
    public static final int BOTTOM_ALIGNMENT = -2;

    /** It rests on the roman baseline. */
    public static final int ROMAN_BASELINE = Font.ROMAN_BASELINE;

    /** It centres on the ideographic scripts' baseline. */
    public static final int CENTER_BASELINE = Font.CENTER_BASELINE;

    /** It hangs from the Indic scripts' baseline. */
    public static final int HANGING_BASELINE = Font.HANGING_BASELINE;

    private final int alignment;

    /**
     * With the given alignment.
     *
     * @throws IllegalArgumentException if it is none of the five
     */
    protected GraphicAttribute(int alignment) {
        if (alignment < BOTTOM_ALIGNMENT || alignment > HANGING_BASELINE) {
            throw new IllegalArgumentException("bad alignment");
        }
        this.alignment = alignment;
    }

    /** How far it rises above the baseline. */
    public abstract float getAscent();

    /** How far it drops below the baseline. */
    public abstract float getDescent();

    /** How far the line advances after drawing it. */
    public abstract float getAdvance();

    /**
     * Where the ink falls, relative to the origin point.
     *
     * <p>The implementation here assumes the drawing fills its measurement box exactly. A subclass
     * whose ink goes outside it --a figure with a thick stroke, say-- has to override it, or the line
     * will work out wrongly what has to be repainted.
     */
    public Rectangle2D getBounds() {
        float ascent = this.getAscent();
        return new Rectangle2D.Float(0, -ascent, this.getAdvance(), ascent + this.getDescent());
    }

    /**
     * The drawing's outline, transformed.
     *
     * <p>The implementation here returns {@link #getBounds}'s box, which is an honest approximation:
     * any old drawing has no reason to have an outline more precise than its box.
     */
    public Shape getOutline(AffineTransform tx) {
        Shape b = this.getBounds();
        if (tx != null) {
            b = tx.createTransformedShape(b);
        }
        return b;
    }

    /** Draws, with the origin at `(x, y)`. */
    public abstract void draw(Graphics2D graphics, float x, float y);

    /** What it aligns with inside the line. */
    public final int getAlignment() {
        return this.alignment;
    }

    /**
     * How it stretches or shrinks when justifying.
     *
     * <p>By default it behaves like space between letters: it can stretch up to a third of its
     * advance on each side and cannot shrink. It is the prudent answer for a drawing nothing is known
     * about.
     */
    public GlyphJustificationInfo getJustificationInfo() {
        float advance = this.getAdvance();
        return new GlyphJustificationInfo(advance, false,
                GlyphJustificationInfo.PRIORITY_INTERCHAR, advance / 3, advance / 3, false,
                GlyphJustificationInfo.PRIORITY_INTERCHAR, 0, 0);
    }
}
