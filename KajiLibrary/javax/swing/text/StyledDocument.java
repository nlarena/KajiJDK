package javax.swing.text;

import java.awt.Color;
import java.awt.Font;

/**
 * A styled document: text that also has form.
 *
 * <p>It adds three things over {@link Document}: a set of named {@link Style}s, the ability to
 * apply attributes to a stretch --character or paragraph ones--, and that of asking how an
 * attribute set looks ({@link #getFont}, {@link #getForeground}).
 *
 * <p>The distinction between character and paragraph attributes is not a detail: the character
 * ones hold for any stretch, and the paragraph ones hold for the whole paragraph even if the
 * marked stretch is a word. Asking for "centred" over three letters centres the paragraph.
 *
 * <p>A <em>logical style</em> is the style a paragraph has assigned as the parent of all its
 * attributes: changing it changes the whole paragraph at once.
 */
public interface StyledDocument extends Document {

    Style addStyle(String nm, Style parent);

    void removeStyle(String nm);

    Style getStyle(String nm);

    /** It applies character attributes to that stretch; {@code replace} erases those there were. */
    void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace);

    /** It applies paragraph attributes to the paragraphs that stretch touches. */
    void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace);

    /** The style that paragraph hangs from; see the interface note. */
    void setLogicalStyle(int pos, Style s);

    Style getLogicalStyle(int p);

    Element getParagraphElement(int pos);

    /** The leaf element that contains that position: the run with the same attributes. */
    Element getCharacterElement(int pos);

    Color getForeground(AttributeSet attr);

    Color getBackground(AttributeSet attr);

    Font getFont(AttributeSet attr);
}
