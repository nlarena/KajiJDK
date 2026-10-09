package java.awt.font;

/**
 * A line of text's vertical measurements.
 *
 * <p>Everything needed to stack lines without their treading on each other: how far the text rises
 * above the baseline, how far it drops, and how much air goes between one line and the next. The sum
 * of the three is {@link #getHeight}.
 *
 * <p>The baselines in the plural are no whim: a text mixing alphabets needs them. A Latin character
 * rests on the roman baseline, a Devanagari one hangs from a bar above, and an ideographic one is
 * centred; {@link #getBaselineOffsets} gives the distance between them so that the three line up on
 * the same line.
 */
public abstract class LineMetrics {

    /** For the subclasses. */
    protected LineMetrics() {
    }

    /** How many characters were measured. */
    public abstract int getNumChars();

    /** How far the text rises above the baseline. */
    public abstract float getAscent();

    /** How far the text drops below the baseline. */
    public abstract float getDescent();

    /** The air between the bottom of one line and the top of the next. */
    public abstract float getLeading();

    /** The sum of the three above. */
    public abstract float getHeight();

    /** Which of the baselines this text uses. */
    public abstract int getBaselineIndex();

    /** The distance from each baseline to the one this text uses. */
    public abstract float[] getBaselineOffsets();

    /** At what height the strikethrough line goes. */
    public abstract float getStrikethroughOffset();

    /** How thick the strikethrough line is. */
    public abstract float getStrikethroughThickness();

    /** At what height the underline goes. */
    public abstract float getUnderlineOffset();

    /** How thick the underline is. */
    public abstract float getUnderlineThickness();
}
