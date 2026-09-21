package java.awt;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D$Double;

import jdk.internal.awt.BitmapFont;

/**
 * The metrics of the only font of this VM; see {@link BitmapFont}.
 *
 * <p>{@link FontMetrics} defines almost everything in terms of {@link #charsWidth} and
 * {@link #getWidths}, and the two are defined in a circle on purpose: the subclass has to break it
 * with the real measure, and this is the only one there is. The two methods here are the ones that
 * turn the circle into a table.
 *
 * <p>Every {@link Font} gives the same metrics, whatever its name or size, because every {@code
 * Font} is drawn with the same face. It is the substitution {@link BitmapFont} talks about, and
 * what matters is that it be <strong>the same</strong> at both ends: what this measures is what the
 * rasteriser paints.
 */
class KajiFontMetrics extends FontMetrics {

    private static final long serialVersionUID = 1L;

    KajiFontMetrics(Font font) {
        super(font);
    }

    public int getAscent() {
        return BitmapFont.ASCENT;
    }

    public int getDescent() {
        return BitmapFont.DESCENT;
    }

    public int getLeading() {
        return BitmapFont.LEADING;
    }

    public int getMaxAdvance() {
        return BitmapFont.MAX_ADVANCE;
    }

    /**
     * The sum of the advances. It is the primitive: {@code stringWidth} and {@code charWidth} come
     * out of here.
     */
    public int charsWidth(char[] data, int off, int len) {
        int total = 0;
        for (int i = 0; i < len; i++) {
            total = total + BitmapFont.advance(data[off + i]);
        }
        return total;
    }

    /** The advances of the first 256 characters; outside ASCII, the one of {@code ?}. */
    public int[] getWidths() {
        int[] widths = new int[256];
        for (int c = 0; c < 256; c++) {
            widths[c] = BitmapFont.advance((char) c);
        }
        return widths;
    }

    /**
     * The box of a string, from the baseline.
     *
     * <p>Overridden because the one of {@link FontMetrics} delegates to {@link
     * Font#getStringBounds}, which needs the font engine this VM does not ship. The box is the
     * width measured by {@link #stringWidth} and the height of the line, with the origin on the
     * baseline — hence the negative {@code y}: {@code -ascent}.
     */
    public Rectangle2D getStringBounds(String str, Graphics context) {
        return new Rectangle2D$Double(0, -getAscent(), stringWidth(str), getHeight());
    }

    public Rectangle2D getStringBounds(String str, int beginIndex, int limit, Graphics context) {
        return getStringBounds(str.substring(beginIndex, limit), context);
    }
}
