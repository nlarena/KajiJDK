package java.awt.font;

import java.awt.geom.Rectangle2D;

/**
 * **One** glyph's measurements.
 *
 * <p>There are two different measurements and they are best not confused. The **advance** is how far
 * to move over to draw the next glyph; the **rectangle** is where the ink falls. They do not agree: a
 * space has an advance and no ink, and a slanted letter may paint beyond its advance.
 *
 * <p>Out of that difference come the two side bearings. The left one ({@link #getLSB}) is the air
 * between the origin and where the ink starts, and the right one ({@link #getRSB}) the one left
 * between where it ends and the advance. Either of the two may be **negative**, and that is what lets
 * a letter tuck under the previous one.
 *
 * <p>The type says what the glyph is with respect to the characters: an ordinary one is a character,
 * a ligature is several in a single drawing, and a combining one --a loose accent-- is a glyph that
 * rests on another without advancing.
 */
public final class GlyphMetrics {

    /** One glyph per character. */
    public static final byte STANDARD = 0;

    /** A glyph that draws several characters together. */
    public static final byte LIGATURE = 1;

    /** A glyph that rests on another, such as an accent. */
    public static final byte COMBINING = 2;

    /** A glyph that is part of another and has no character of its own. */
    public static final byte COMPONENT = 3;

    /** A glyph with no ink, which only advances. */
    public static final byte WHITESPACE = 4;

    private final boolean horizontal;
    private final float advanceX;
    private final float advanceY;
    private final Rectangle2D.Float bounds;
    private final byte glyphType;

    /** With a horizontal advance. */
    public GlyphMetrics(float advance, Rectangle2D bounds, byte glyphType) {
        this.horizontal = true;
        this.advanceX = advance;
        this.advanceY = 0;
        this.bounds = new Rectangle2D.Float();
        this.bounds.setRect(bounds);
        this.glyphType = glyphType;
    }

    /** With an advance in whichever direction is stated. */
    public GlyphMetrics(boolean horizontal, float advanceX, float advanceY, Rectangle2D bounds,
            byte glyphType) {
        this.horizontal = horizontal;
        this.advanceX = advanceX;
        this.advanceY = advanceY;
        this.bounds = new Rectangle2D.Float();
        this.bounds.setRect(bounds);
        this.glyphType = glyphType;
    }

    /** The advance in the text's direction. */
    public float getAdvance() {
        if (this.horizontal) {
            return this.advanceX;
        }
        return this.advanceY;
    }

    /** The horizontal advance. */
    public float getAdvanceX() {
        return this.advanceX;
    }

    /** The vertical advance. */
    public float getAdvanceY() {
        return this.advanceY;
    }

    /** Where the ink falls, relative to the glyph's origin. */
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Float(this.bounds.x, this.bounds.y, this.bounds.width,
                this.bounds.height);
    }

    /** The air before the ink; it may be negative. */
    public float getLSB() {
        if (this.horizontal) {
            return this.bounds.x;
        }
        return this.bounds.y;
    }

    /** The air after the ink; it may be negative. */
    public float getRSB() {
        if (this.horizontal) {
            return this.advanceX - this.bounds.x - this.bounds.width;
        }
        return this.advanceY - this.bounds.y - this.bounds.height;
    }

    /** The raw type, with the class bits and the whitespace one together. */
    public int getType() {
        return this.glyphType;
    }

    /** Whether it is one glyph per character. */
    public boolean isStandard() {
        return (this.glyphType & 0x3) == STANDARD;
    }

    /** Whether it draws several characters together. */
    public boolean isLigature() {
        return (this.glyphType & 0x3) == LIGATURE;
    }

    /** Whether it rests on another glyph. */
    public boolean isCombining() {
        return (this.glyphType & 0x3) == COMBINING;
    }

    /** Whether it is part of another glyph. */
    public boolean isComponent() {
        return (this.glyphType & 0x3) == COMPONENT;
    }

    /**
     * Whether it has no ink.
     *
     * <p>It is independent of the other four: whitespace lives in its own bit, so a glyph can be
     * standard and white at the same time.
     */
    public boolean isWhitespace() {
        return (this.glyphType & 0x4) == WHITESPACE;
    }
}
