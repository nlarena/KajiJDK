package java.awt;

import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.text.CharacterIterator;

/**
 * How much the text of a {@link Font} measures on a concrete device.
 *
 * <p>The font says what the letters are like; the metrics say how much room they take once drawn,
 * which depends on the device: on a screen the advances are rounded to whole pixels and on a
 * printer they are not. That is why the metrics are asked for through a {@link Graphics} and not
 * from the font directly.
 *
 * <p>Almost everything here is defined in terms of {@link #charsWidth} and {@link #getWidths},
 * which call each other. <strong>A subclass has to override at least one of the two</strong>, or
 * the first measurement goes into infinite recursion. It is the JDK's design and it is kept as it
 * is: changing which one is the primitive would break anyone who has already overridden the other.
 */
public abstract class FontMetrics implements Serializable {

    private static final long serialVersionUID = 1681126225205050147L;

    private static final FontRenderContext DEFAULT_FRC =
            new FontRenderContext(null, false, false);

    /** The font being measured. */
    protected Font font;

    /** With the given font. */
    protected FontMetrics(Font font) {
        this.font = font;
    }

    /** The font being measured. */
    public Font getFont() {
        return this.font;
    }

    /**
     * The drawing conditions these metrics assume.
     *
     * <p>The implementation here returns ones with no transform, no antialiasing and no fractional
     * metrics, which is what an ordinary screen corresponds to.
     */
    public FontRenderContext getFontRenderContext() {
        return DEFAULT_FRC;
    }

    /** The air between the bottom of one line and the top of the next. */
    public int getLeading() {
        return 0;
    }

    /** How far the text goes above the baseline. */
    public int getAscent() {
        return 0;
    }

    /** How far the text goes below the baseline. */
    public int getDescent() {
        return 0;
    }

    /** The sum of the three above: how far apart the lines go. */
    public int getHeight() {
        return this.getLeading() + this.getAscent() + this.getDescent();
    }

    /**
     * The most any character of the font goes up.
     *
     * <p>It can be more than {@link #getAscent}: that one is the typical ascent, this one the worst
     * case.
     */
    public int getMaxAscent() {
        return this.getAscent();
    }

    /** The most any character of the font goes down. */
    public int getMaxDescent() {
        return this.getDescent();
    }

    /**
     * The same as {@link #getMaxDescent}.
     *
     * @deprecated the name is misspelled. It is kept because it has been in the API since 1.0.
     */
    @Deprecated
    public int getMaxDecent() {
        return this.getMaxDescent();
    }

    /**
     * The advance of the widest character, or -1 if it is not known.
     *
     * <p>The -1 is an answer and not an error: there are fonts where finding it out would mean
     * measuring every glyph, and saying it is not known is more honest than returning an invented
     * bound.
     */
    public int getMaxAdvance() {
        return -1;
    }

    /**
     * The width of a character given by its code point.
     *
     * <p>A point that is not valid is measured as the missing glyph, which is what is going to be
     * drawn.
     */
    public int charWidth(int codePoint) {
        int cp = codePoint;
        if (!Character.isValidCodePoint(cp)) {
            cp = 0xFFFF;
        }
        if (cp < 256) {
            return this.getWidths()[cp];
        }
        char[] buffer = new char[2];
        int len = Character.toChars(cp, buffer, 0);
        return this.charsWidth(buffer, 0, len);
    }

    /**
     * The width of a character.
     *
     * <p>It is no good for the characters written with two `char`s; for those there is the code
     * point version.
     */
    public int charWidth(char ch) {
        if (ch < 256) {
            return this.getWidths()[ch];
        }
        char[] data = new char[1];
        data[0] = ch;
        return this.charsWidth(data, 0, 1);
    }

    /** The width of a string. */
    public int stringWidth(String str) {
        int len = str.length();
        char[] data = new char[len];
        str.getChars(0, len, data, 0);
        return this.charsWidth(data, 0, len);
    }

    /** The width of a stretch of an array of characters. */
    public int charsWidth(char[] data, int off, int len) {
        return this.stringWidth(new String(data, off, len));
    }

    /**
     * The width of a stretch of bytes, taking each one as a character.
     *
     * @deprecated it does not translate bytes into characters correctly in any encoding other than
     *     Latin-1. It is kept because it has been in the API since 1.0.
     */
    @Deprecated
    public int bytesWidth(byte[] data, int off, int len) {
        return this.charsWidth(new String(data, off, len).toCharArray(), 0, len);
    }

    /** The width of the first 256 characters. */
    public int[] getWidths() {
        int[] widths = new int[256];
        for (char ch = 0; ch < 256; ch++) {
            widths[ch] = this.charWidth(ch);
        }
        return widths;
    }

    /** Whether every character of the font shares the same line measures. */
    public boolean hasUniformLineMetrics() {
        return this.font.hasUniformLineMetrics();
    }

    /** The vertical measures of that string. */
    public LineMetrics getLineMetrics(String str, Graphics context) {
        return this.font.getLineMetrics(str, this.myFRC(context));
    }

    /** The vertical measures of a stretch of that string. */
    public LineMetrics getLineMetrics(String str, int beginIndex, int limit, Graphics context) {
        return this.font.getLineMetrics(str, beginIndex, limit, this.myFRC(context));
    }

    /** The vertical measures of a stretch of characters. */
    public LineMetrics getLineMetrics(char[] chars, int beginIndex, int limit, Graphics context) {
        return this.font.getLineMetrics(chars, beginIndex, limit, this.myFRC(context));
    }

    /** The vertical measures of a stretch of an iterator. */
    public LineMetrics getLineMetrics(CharacterIterator ci, int beginIndex, int limit,
            Graphics context) {
        return this.font.getLineMetrics(ci, beginIndex, limit, this.myFRC(context));
    }

    /** The rectangle that string takes up. */
    public Rectangle2D getStringBounds(String str, Graphics context) {
        return this.font.getStringBounds(str, this.myFRC(context));
    }

    /** The rectangle a stretch of that string takes up. */
    public Rectangle2D getStringBounds(String str, int beginIndex, int limit, Graphics context) {
        return this.font.getStringBounds(str, beginIndex, limit, this.myFRC(context));
    }

    /** The rectangle a stretch of characters takes up. */
    public Rectangle2D getStringBounds(char[] chars, int beginIndex, int limit, Graphics context) {
        return this.font.getStringBounds(chars, beginIndex, limit, this.myFRC(context));
    }

    /** The rectangle a stretch of an iterator takes up. */
    public Rectangle2D getStringBounds(CharacterIterator ci, int beginIndex, int limit,
            Graphics context) {
        return this.font.getStringBounds(ci, beginIndex, limit, this.myFRC(context));
    }

    /** The rectangle of the biggest character of the font. */
    public Rectangle2D getMaxCharBounds(Graphics context) {
        return this.font.getMaxCharBounds(this.myFRC(context));
    }

    /** The drawing conditions of the given context, or the default ones. */
    private FontRenderContext myFRC(Graphics context) {
        if (context instanceof Graphics2D) {
            return ((Graphics2D) context).getFontRenderContext();
        }
        return DEFAULT_FRC;
    }

    public String toString() {
        return this.getClass().getName() + "[font=" + this.getFont() + "ascent="
                + this.getAscent() + ", descent=" + this.getDescent() + ", height="
                + this.getHeight() + "]";
    }
}
