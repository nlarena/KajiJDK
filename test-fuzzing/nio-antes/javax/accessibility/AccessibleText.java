package javax.accessibility;

import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.text.AttributeSet;

/**
 * Implemented by what shows text that can be walked.
 *
 * <p>The distinctive thing is that the text is asked for **by unit**: a character, a word or a
 * sentence, and the three constants say which. An assistive technology that reads aloud wants
 * neither the whole text nor letter by letter; it wants the sentence where the caret is.
 *
 * <p>The three reading methods --{@link #getAtIndex}, {@link #getAfterIndex}, {@link
 * #getBeforeIndex}-- exist for the same reason: they allow moving through the text in the unit that
 * suits whoever reads, without bringing everything.
 */
public interface AccessibleText {

    /** The unit "a character". */
    int CHARACTER = 1;

    /** The unit "a word". */
    int WORD = 2;

    /** The unit "a sentence". */
    int SENTENCE = 3;

    /**
     * Which character falls at that point.
     *
     * @return the index, or -1 if the point falls outside the text
     */
    int getIndexAtPoint(Point p);

    /**
     * Where that character is on the screen.
     *
     * @return the rectangle, or `null` if the index does not exist
     */
    Rectangle getCharacterBounds(int i);

    /** How many characters there are. */
    int getCharCount();

    /** Where the caret is. */
    int getCaretPosition();

    /**
     * The unit that starts at that index.
     *
     * @param part {@link #CHARACTER}, {@link #WORD} or {@link #SENTENCE}
     */
    String getAtIndex(int part, int index);

    /** The unit following the one at that index. */
    String getAfterIndex(int part, int index);

    /** The unit preceding the one at that index. */
    String getBeforeIndex(int part, int index);

    /** With which attributes that character is drawn. */
    AttributeSet getCharacterAttribute(int i);

    /** Where the selection starts. */
    int getSelectionStart();

    /** Where the selection ends. */
    int getSelectionEnd();

    /** The selected text, or `null` if there is none. */
    String getSelectedText();
}
