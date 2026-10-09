package java.awt.font;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * A line of text already laid out: the glyphs chosen, ordered and placed.
 *
 * <p>It is the class that settles everything a line of text has that is hard, and that goes
 * unnoticed until the text is not English. Which glyph corresponds to each character and which ones
 * melt into a ligature; in what order they go if the line mixes directions; where the caret falls
 * when the boundary between two characters falls in two different places on the screen; what is
 * highlighted when a stretch is selected that is not contiguous on screen.
 *
 * <p>That last family of methods —{@code getCaretShapes}, {@code getVisualHighlightShape},
 * {@code getLogicalRangesForVisualSelection}— exists entirely because of bidirectional text. In a
 * single-direction line they would be trivial; in one mixing Arabic with Latin, selecting three
 * consecutive characters may paint **two** separate rectangles, and dragging the caret one position
 * may move it the other way.
 *
 * <p><strong>It cannot be constructed.</strong> Laying out a line demands measuring every glyph, and
 * measuring a glyph demands reading the font's file. This library carries no text engine —the same
 * boundary that splits {@link Font} in two halves— so the three constructors throw
 * `UnsupportedOperationException` for that reason. The class is declared in full so that whatever
 * names it compiles, and it answers nothing it cannot know: a member that is missing is a legal
 * subset; one that lies is not.
 */
public final class TextLayout implements Cloneable {

    /**
     * Which of the two possible carets is the strong one when a position falls between two
     * directions.
     *
     * <p>In a bidirectional line, one and the same text position has **two** places on screen where
     * the caret could go. The policy decides which one is drawn solid and which is drawn as the weak
     * caret, or not drawn at all.
     */
    public static class CaretPolicy {

        /** One that picks the caret of the stretch with the higher bidirectional embedding level. */
        public CaretPolicy() {
        }

        /**
         * Which of the two carets is the strong one.
         *
         * @throws UnsupportedOperationException always: the answer depends on the line's
         *     bidirectional levels, which only exist if the line could be laid out
         */
        public TextHitInfo getStrongCaret(TextHitInfo hit1, TextHitInfo hit2,
                TextLayout layout) {
            throw noEngine("getStrongCaret");
        }
    }

    /** The policy used unless something else is said. */
    public static final CaretPolicy DEFAULT_CARET_POLICY = new CaretPolicy();

    /** The single message of everything that needs to measure glyphs. */
    private static UnsupportedOperationException noEngine(String method) {
        return new UnsupportedOperationException(method + " requires laying out the line, and "
                + "laying it out requires measuring the font's glyphs; this library carries no "
                + "text engine");
    }

    /**
     * A line with a single font.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public TextLayout(String string, Font font, FontRenderContext frc) {
        throw noEngine("TextLayout");
    }

    /**
     * A line with the given attributes.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public TextLayout(String string,
            Map<? extends AttributedCharacterIterator.Attribute, ?> attributes,
            FontRenderContext frc) {
        throw noEngine("TextLayout");
    }

    /**
     * A line out of a text with attributes per stretch.
     *
     * @throws UnsupportedOperationException always: the glyphs have to be measured
     */
    public TextLayout(AttributedCharacterIterator text, FontRenderContext frc) {
        throw noEngine("TextLayout");
    }

    /**
     * A copy.
     *
     * @throws UnsupportedOperationException always
     */
    protected Object clone() {
        throw noEngine("clone");
    }

    /**
     * The same line stretched to that width.
     *
     * @throws UnsupportedOperationException always
     */
    public TextLayout getJustifiedLayout(float justificationWidth) {
        throw noEngine("getJustifiedLayout");
    }

    /**
     * Shares the slack out among the line's glyphs.
     *
     * @throws UnsupportedOperationException always
     */
    protected void handleJustify(float justificationWidth) {
        throw noEngine("handleJustify");
    }

    /**
     * Which baseline the line rests on.
     *
     * @throws UnsupportedOperationException always
     */
    public byte getBaseline() {
        throw noEngine("getBaseline");
    }

    /**
     * The distance from each baseline to the line's.
     *
     * @throws UnsupportedOperationException always
     */
    public float[] getBaselineOffsets() {
        throw noEngine("getBaselineOffsets");
    }

    /**
     * How far the whole line advances.
     *
     * @throws UnsupportedOperationException always
     */
    public float getAdvance() {
        throw noEngine("getAdvance");
    }

    /**
     * How far it advances not counting the trailing spaces.
     *
     * @throws UnsupportedOperationException always
     */
    public float getVisibleAdvance() {
        throw noEngine("getVisibleAdvance");
    }

    /**
     * How far the line rises.
     *
     * @throws UnsupportedOperationException always
     */
    public float getAscent() {
        throw noEngine("getAscent");
    }

    /**
     * How far the line drops.
     *
     * @throws UnsupportedOperationException always
     */
    public float getDescent() {
        throw noEngine("getDescent");
    }

    /**
     * The air up to the next line.
     *
     * @throws UnsupportedOperationException always
     */
    public float getLeading() {
        throw noEngine("getLeading");
    }

    /**
     * Where the line's ink falls.
     *
     * @throws UnsupportedOperationException always
     */
    public Rectangle2D getBounds() {
        throw noEngine("getBounds");
    }

    /**
     * The pixels the line drawn at `(x, y)` touches.
     *
     * @throws UnsupportedOperationException always
     */
    public Rectangle getPixelBounds(FontRenderContext frc, float x, float y) {
        throw noEngine("getPixelBounds");
    }

    /**
     * Whether the line's base direction is left to right.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean isLeftToRight() {
        throw noEngine("isLeftToRight");
    }

    /**
     * Whether the line runs vertically.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean isVertical() {
        throw noEngine("isVertical");
    }

    /**
     * How many characters the line has.
     *
     * @throws UnsupportedOperationException always
     */
    public int getCharacterCount() {
        throw noEngine("getCharacterCount");
    }

    /**
     * Where and how to draw the caret at that position.
     *
     * @throws UnsupportedOperationException always
     */
    public float[] getCaretInfo(TextHitInfo hit, Rectangle2D bounds) {
        throw noEngine("getCaretInfo");
    }

    /**
     * The same, with the line's bounds.
     *
     * @throws UnsupportedOperationException always
     */
    public float[] getCaretInfo(TextHitInfo hit) {
        throw noEngine("getCaretInfo");
    }

    /**
     * The position right of it on screen.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo getNextRightHit(TextHitInfo hit) {
        throw noEngine("getNextRightHit");
    }

    /**
     * The same, with the given caret policy.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo getNextRightHit(int offset, CaretPolicy policy) {
        throw noEngine("getNextRightHit");
    }

    /**
     * The same, from an insertion position.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo getNextRightHit(int offset) {
        throw noEngine("getNextRightHit");
    }

    /**
     * The position left of it on screen.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo getNextLeftHit(TextHitInfo hit) {
        throw noEngine("getNextLeftHit");
    }

    /**
     * The same, with the given caret policy.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo getNextLeftHit(int offset, CaretPolicy policy) {
        throw noEngine("getNextLeftHit");
    }

    /**
     * The same, from an insertion position.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo getNextLeftHit(int offset) {
        throw noEngine("getNextLeftHit");
    }

    /**
     * The other way of naming the same boundary, in screen coordinates.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo getVisualOtherHit(TextHitInfo hit) {
        throw noEngine("getVisualOtherHit");
    }

    /**
     * The caret's shape at that position.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape getCaretShape(TextHitInfo hit, Rectangle2D bounds) {
        throw noEngine("getCaretShape");
    }

    /**
     * The same, with the line's bounds.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape getCaretShape(TextHitInfo hit) {
        throw noEngine("getCaretShape");
    }

    /**
     * That character's bidirectional embedding level.
     *
     * @throws UnsupportedOperationException always
     */
    public byte getCharacterLevel(int index) {
        throw noEngine("getCharacterLevel");
    }

    /**
     * The shapes of the two possible carets at that position.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape[] getCaretShapes(int offset, Rectangle2D bounds, CaretPolicy policy) {
        throw noEngine("getCaretShapes");
    }

    /**
     * The same, with the default policy.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape[] getCaretShapes(int offset, Rectangle2D bounds) {
        throw noEngine("getCaretShapes");
    }

    /**
     * The same, with the line's bounds.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape[] getCaretShapes(int offset) {
        throw noEngine("getCaretShapes");
    }

    /**
     * Which stretches of the text end up selected by a selection made on screen.
     *
     * <p>It returns several pairs because in a bidirectional line a selection contiguous on screen
     * may correspond to separate stretches of the text.
     *
     * @throws UnsupportedOperationException always
     */
    public int[] getLogicalRangesForVisualSelection(TextHitInfo firstEndpoint,
            TextHitInfo secondEndpoint) {
        throw noEngine("getLogicalRangesForVisualSelection");
    }

    /**
     * The shape to highlight for a selection made on screen.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape getVisualHighlightShape(TextHitInfo firstEndpoint, TextHitInfo secondEndpoint,
            Rectangle2D bounds) {
        throw noEngine("getVisualHighlightShape");
    }

    /**
     * The same, with the line's bounds.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape getVisualHighlightShape(TextHitInfo firstEndpoint, TextHitInfo secondEndpoint) {
        throw noEngine("getVisualHighlightShape");
    }

    /**
     * The shape to highlight for a stretch of the text.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape getLogicalHighlightShape(int firstEndpoint, int secondEndpoint,
            Rectangle2D bounds) {
        throw noEngine("getLogicalHighlightShape");
    }

    /**
     * The same, with the line's bounds.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape getLogicalHighlightShape(int firstEndpoint, int secondEndpoint) {
        throw noEngine("getLogicalHighlightShape");
    }

    /**
     * The ink of a stretch of the text.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape getBlackBoxBounds(int firstEndpoint, int secondEndpoint) {
        throw noEngine("getBlackBoxBounds");
    }

    /**
     * Which character falls at that point on the screen.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo hitTestChar(float x, float y, Rectangle2D bounds) {
        throw noEngine("hitTestChar");
    }

    /**
     * The same, with the line's bounds.
     *
     * @throws UnsupportedOperationException always
     */
    public TextHitInfo hitTestChar(float x, float y) {
        throw noEngine("hitTestChar");
    }

    /**
     * Equality with another line.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean equals(TextLayout rhs) {
        throw noEngine("equals");
    }

    public String toString() {
        return "java.awt.font.TextLayout[no text engine]";
    }

    /**
     * Draws the line with the start of the baseline at `(x, y)`.
     *
     * @throws UnsupportedOperationException always
     */
    public void draw(Graphics2D g2, float x, float y) {
        throw noEngine("draw");
    }

    /**
     * The whole line's outline.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape getOutline(AffineTransform tx) {
        throw noEngine("getOutline");
    }

    /**
     * The path the line rests on.
     *
     * @throws UnsupportedOperationException always
     */
    public LayoutPath getLayoutPath() {
        throw noEngine("getLayoutPath");
    }

    /**
     * Where that text position falls on screen.
     *
     * @throws UnsupportedOperationException always
     */
    public void hitToPoint(TextHitInfo hit, Point2D point) {
        throw noEngine("hitToPoint");
    }
}
