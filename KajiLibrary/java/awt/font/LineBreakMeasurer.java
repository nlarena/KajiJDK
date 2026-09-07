package java.awt.font;

import java.text.AttributedCharacterIterator;
import java.text.BreakIterator;

/**
 * Breaks a paragraph into lines that fit a given width.
 *
 * <p>It is used as an iterator with state: each {@link #nextLayout(float)} returns the next line and
 * advances the position. That the width is passed **on each call** and not in the constructor is no
 * oversight: it is what allows laying out around a figure, where each line has a different width
 * because the gap changes shape.
 *
 * <p>Where it may break is decided by a {@link BreakIterator}, and that is why one's own can be
 * given: in most languages the break goes at the spaces, but in Thai or Japanese there are no spaces
 * between words and the break needs to know about the language.
 *
 * <p><strong>It cannot be constructed.</strong> To know what fits in a width one has to measure, and
 * measuring needs a text engine this library does not carry. It is the same boundary as
 * {@link TextLayout}'s, {@link TextMeasurer}'s and {@link java.awt.Font}'s.
 */
public final class LineBreakMeasurer {

    /** The single message of everything that needs to measure glyphs. */
    private static UnsupportedOperationException noEngine(String method) {
        return new UnsupportedOperationException(method + " requires measuring the font's "
                + "glyphs; this library carries no text engine");
    }

    /**
     * With the paragraph and the drawing conditions, breaking at words.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public LineBreakMeasurer(AttributedCharacterIterator text, FontRenderContext frc) {
        throw noEngine("LineBreakMeasurer");
    }

    /**
     * With the given breaking criterion.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public LineBreakMeasurer(AttributedCharacterIterator text, BreakIterator breakIter,
            FontRenderContext frc) {
        throw noEngine("LineBreakMeasurer");
    }

    /**
     * How far the next line would reach, without consuming it.
     *
     * @throws UnsupportedOperationException always
     */
    public int nextOffset(float wrappingWidth) {
        throw noEngine("nextOffset");
    }

    /**
     * The same, with a limit and with the option of breaking anywhere.
     *
     * @throws UnsupportedOperationException always
     */
    public int nextOffset(float wrappingWidth, int offsetLimit, boolean requireNextWord) {
        throw noEngine("nextOffset");
    }

    /**
     * The next line, advancing the position.
     *
     * @throws UnsupportedOperationException always
     */
    public TextLayout nextLayout(float wrappingWidth) {
        throw noEngine("nextLayout");
    }

    /**
     * The same, with a limit and with the option of breaking anywhere.
     *
     * @throws UnsupportedOperationException always
     */
    public TextLayout nextLayout(float wrappingWidth, int offsetLimit, boolean requireNextWord) {
        throw noEngine("nextLayout");
    }

    /**
     * Where it has got to.
     *
     * @throws UnsupportedOperationException always
     */
    public int getPosition() {
        throw noEngine("getPosition");
    }

    /**
     * Moves the position.
     *
     * @throws UnsupportedOperationException always
     */
    public void setPosition(int newPosition) {
        throw noEngine("setPosition");
    }

    /**
     * Reports that a character was inserted at that position.
     *
     * @throws UnsupportedOperationException always
     */
    public void insertChar(AttributedCharacterIterator newParagraph, int insertPos) {
        throw noEngine("insertChar");
    }

    /**
     * Reports that a character was deleted at that position.
     *
     * @throws UnsupportedOperationException always
     */
    public void deleteChar(AttributedCharacterIterator newParagraph, int deletePos) {
        throw noEngine("deleteChar");
    }
}
