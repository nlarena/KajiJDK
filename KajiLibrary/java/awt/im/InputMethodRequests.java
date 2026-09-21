package java.awt.im;

import java.awt.Rectangle;
import java.awt.font.TextHitInfo;
import java.text.AttributedCharacterIterator;

/**
 * What a text component has to be able to answer to work with an input method.
 *
 * <p>When writing in Japanese or Chinese, the input method needs to **ask the component things**:
 * where to put its candidate window, what committed text there is around, what is selected. A
 * component that does not implement this can only receive finished text.
 *
 * <p>{@link #cancelLatestCommittedText} is the oddest one and the one that explains the rest: it
 * allows **undoing** the latest commit, because in some input methods the user can go back over a
 * word already accepted and choose among the candidates again.
 */
public interface InputMethodRequests {

    /**
     * Where on screen that position of the composed text is.
     *
     * <p>It is what lets the input method put its candidate window below the text and not just
     * anywhere.
     */
    Rectangle getTextLocation(TextHitInfo offset);

    /** Which position of the text falls at that point on the screen, or `null` if none. */
    TextHitInfo getLocationOffset(int x, int y);

    /** Where the composed text would start being inserted. */
    int getInsertPositionOffset();

    /**
     * A stretch of the already committed text.
     *
     * @param attributes which attributes are of interest, or `null` if none
     */
    AttributedCharacterIterator getCommittedText(int beginIndex, int endIndex,
            AttributedCharacterIterator.Attribute[] attributes);

    /** How much committed text there is. */
    int getCommittedTextLength();

    /**
     * Undoes the latest commit and returns what was removed.
     *
     * @return what was undone, or `null` if the component does not support it
     */
    AttributedCharacterIterator cancelLatestCommittedText(
            AttributedCharacterIterator.Attribute[] attributes);

    /** The selected text, or `null` if there is none. */
    AttributedCharacterIterator getSelectedText(
            AttributedCharacterIterator.Attribute[] attributes);
}
