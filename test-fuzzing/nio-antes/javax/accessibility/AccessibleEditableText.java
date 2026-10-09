package javax.accessibility;

import javax.swing.text.AttributeSet;

/**
 * Accessible text that can also be **modified**.
 *
 * <p>It is what lets an assistive technology not only read a field but write into it: voice
 * dictation, automatic correction, form filling.
 *
 * <p>{@link #cut} and {@link #paste} are there besides {@link #delete} and {@link
 * #insertTextAtIndex} because they are not the same: cutting leaves the text on the clipboard and
 * deleting does not.
 */
public interface AccessibleEditableText extends AccessibleText {

    /** Replaces all the text. */
    void setTextContents(String s);

    /** Inserts text at that position. */
    void insertTextAtIndex(int index, String s);

    /** The text of that range. */
    String getTextRange(int startIndex, int endIndex);

    /** Deletes that range. */
    void delete(int startIndex, int endIndex);

    /** Cuts that range to the clipboard. */
    void cut(int startIndex, int endIndex);

    /** Pastes the clipboard at that position. */
    void paste(int startIndex);

    /** Replaces that range. */
    void replaceText(int startIndex, int endIndex, String s);

    /** Selects that range. */
    void selectText(int startIndex, int endIndex);

    /** Changes the attributes of that range. */
    void setAttributes(int startIndex, int endIndex, AttributeSet as);
}
