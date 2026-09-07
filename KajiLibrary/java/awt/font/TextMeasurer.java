package java.awt.font;

import java.text.AttributedCharacterIterator;

/**
 * Measures a paragraph stretch by stretch, keeping the work between queries.
 *
 * <p>It is the machinery underneath {@link LineBreakMeasurer}, and it exists for a reason of cost:
 * breaking a paragraph into lines demands measuring it many times --once for each break that is
 * tried-- and measuring from scratch every time would be quadratic.
 *
 * <p>Hence {@link #insertChar} and {@link #deleteChar}, which are the interesting part. When a
 * paragraph is edited, nearly everything measured still holds: only the stretch around the change
 * differs. These two methods tell it what was touched so that it can throw away what stopped holding
 * and keep the rest, which is what stops typing into a long text from getting slow.
 *
 * <p><strong>It cannot be constructed.</strong> Measuring a stretch is measuring its glyphs, and that
 * needs a text engine this library does not carry. It is the same boundary as {@link TextLayout}'s
 * and {@link java.awt.Font}'s.
 */
public final class TextMeasurer implements Cloneable {

    /** The single message of everything that needs to measure glyphs. */
    private static UnsupportedOperationException noEngine(String method) {
        return new UnsupportedOperationException(method + " requires measuring the font's "
                + "glyphs; this library carries no text engine");
    }

    /**
     * With the paragraph and the drawing conditions.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public TextMeasurer(AttributedCharacterIterator text, FontRenderContext frc) {
        throw noEngine("TextMeasurer");
    }

    /**
     * A copy with the work already done.
     *
     * @throws UnsupportedOperationException always
     */
    protected Object clone() {
        throw noEngine("clone");
    }

    /**
     * How far a line starting there and not going past that width reaches.
     *
     * @throws UnsupportedOperationException always
     */
    public int getLineBreakIndex(int start, float maxAdvance) {
        throw noEngine("getLineBreakIndex");
    }

    /**
     * How much that stretch measures.
     *
     * @throws UnsupportedOperationException always
     */
    public float getAdvanceBetween(int start, int limit) {
        throw noEngine("getAdvanceBetween");
    }

    /**
     * The line built out of that stretch.
     *
     * @throws UnsupportedOperationException always
     */
    public TextLayout getLayout(int start, int limit) {
        throw noEngine("getLayout");
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
