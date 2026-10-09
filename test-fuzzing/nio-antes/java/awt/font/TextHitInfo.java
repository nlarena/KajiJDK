package java.awt.font;

/**
 * A place in the text where the caret can go: **between** two characters.
 *
 * <p>An insertion point is not a character, it is a boundary, and there are two ways of naming the
 * same boundary: the leading edge of the character that follows or the trailing edge of the one that
 * comes before. This class keeps which of the two, and that is its whole reason for being.
 *
 * <p>It looks like a distinction without a difference until the line mixes directions. In a text
 * running left to right with an Arabic word inside it, the logical boundary between two characters
 * falls in **two different places on the screen** depending on which side one comes from, and
 * without saying so there is no way of knowing where to draw the caret.
 */
public final class TextHitInfo {

    private final int charIndex;
    private final boolean isLeadingEdge;

    /** With the character and which side. */
    private TextHitInfo(int charIndex, boolean isLeadingEdge) {
        this.charIndex = charIndex;
        this.isLeadingEdge = isLeadingEdge;
    }

    /** The character it refers to. */
    public int getCharIndex() {
        return this.charIndex;
    }

    /** Whether it is that character's leading edge. */
    public boolean isLeadingEdge() {
        return this.isLeadingEdge;
    }

    /**
     * The insertion position, counted in characters.
     *
     * <p>It is where a new character would go. Two different `TextHitInfo`s may give the same one:
     * character `n`'s trailing edge and `n+1`'s leading edge are the same boundary.
     */
    public int getInsertionIndex() {
        if (this.isLeadingEdge) {
            return this.charIndex;
        }
        return this.charIndex + 1;
    }

    public int hashCode() {
        return this.charIndex;
    }

    /** Equality by character and by side. */
    public boolean equals(Object obj) {
        return obj instanceof TextHitInfo && this.equals((TextHitInfo) obj);
    }

    /**
     * The same, with the type already known.
     *
     * <p>Two that point at the same boundary from different sides are **not** equal: the class keeps
     * which side it was reached from, and that is what tells them apart.
     */
    public boolean equals(TextHitInfo hitInfo) {
        return hitInfo != null && this.charIndex == hitInfo.charIndex
                && this.isLeadingEdge == hitInfo.isLeadingEdge;
    }

    public String toString() {
        return "TextHitInfo[" + this.charIndex + (this.isLeadingEdge ? "L" : "T") + "]";
    }

    /** That character's leading edge. */
    public static TextHitInfo leading(int charIndex) {
        return new TextHitInfo(charIndex, true);
    }

    /** That character's trailing edge. */
    public static TextHitInfo trailing(int charIndex) {
        return new TextHitInfo(charIndex, false);
    }

    /** The boundary before that position, named from the preceding character. */
    public static TextHitInfo beforeOffset(int offset) {
        return new TextHitInfo(offset - 1, false);
    }

    /** The boundary after that position, named from the following character. */
    public static TextHitInfo afterOffset(int offset) {
        return new TextHitInfo(offset, true);
    }

    /** The other way of naming the same boundary. */
    public TextHitInfo getOtherHit() {
        if (this.isLeadingEdge) {
            return trailing(this.charIndex - 1);
        }
        return leading(this.charIndex + 1);
    }

    /** The same side, so many characters further along. */
    public TextHitInfo getOffsetHit(int delta) {
        return new TextHitInfo(this.charIndex + delta, this.isLeadingEdge);
    }
}
