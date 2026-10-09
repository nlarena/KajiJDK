package javax.swing;

import java.awt.Component;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;

/**
 * A text area with styles, images and components inside.
 *
 * <h2>What separates it from {@link JEditorPane}</h2>
 *
 * <p>The one above shows any format -- plain text, HTML, RTF -- by choosing an
 * {@link EditorKit} according to the content type. This one fixes the format: always text with
 * styles, and in exchange it exposes the API for manipulating them without going through the
 * document. Hence {@link #setEditorKit} is final: changing it would leave every other method
 * meaningless.
 *
 * <h2>Three levels of attributes</h2>
 *
 * <p>The <em>logical style</em> is the background one, the one that is applied to the whole
 * paragraph for belonging to a category -- "title", "quotation" --. The <em>paragraph
 * attributes</em> override it for that paragraph. The <em>character attributes</em> override
 * both for a stretch of text. They are resolved in that order, and it is what allows the letter
 * of every title to be changed at once without touching the bold somebody set by hand.
 *
 * <h2>The input attributes</h2>
 *
 * <p>{@link #getInputAttributes} is what is going to be applied to the <em>next</em> thing that
 * is typed. It is what makes pressing the bold button with nothing selected put what is typed
 * next in bold, which is what one expects of an editor.
 */
public class JTextPane extends JEditorPane {

    /** An empty area with a document with styles. */
    public JTextPane() {
        super();
        EditorKit editorKit = createDefaultEditorKit();
        String contentType = editorKit.getContentType();
        if (contentType != null && getEditorKitClassNameForContentType(contentType)
                == getEditorKitClassNameForContentType("text/plain")) {
            setEditorKitForContentType(contentType, editorKit);
        }
        setEditorKit(editorKit);
    }

    /**
     * Over that document.
     *
     * @throws NullPointerException if the document is null
     */
    public JTextPane(StyledDocument doc) {
        this();
        setStyledDocument(doc);
    }

    public String getUIClassID() {
        return "TextPaneUI";
    }

    /**
     * It changes the document.
     *
     * @throws IllegalArgumentException if it is not a {@link StyledDocument}
     */
    public void setDocument(Document doc) {
        if (doc instanceof StyledDocument) {
            super.setDocument(doc);
        } else {
            throw new IllegalArgumentException("Model must be StyledDocument");
        }
    }

    /** The same, with the exact type. */
    public void setStyledDocument(StyledDocument doc) {
        super.setDocument(doc);
    }

    public StyledDocument getStyledDocument() {
        return (StyledDocument) getDocument();
    }

    /**
     * It replaces what is selected with that text.
     *
     * <p>The new text takes the input attributes; see the class note.
     */
    public void replaceSelection(String content) {
        replaceSelection(content, true);
    }

    private void replaceSelection(String content, boolean checkEditable) {
        if (checkEditable && !isEditable()) {
            javax.swing.UIManager.getLookAndFeel();
            return;
        }
        Document doc = getStyledDocument();
        if (doc != null) {
            try {
                java.awt.Rectangle r = null;
                int p0 = Math.min(getCaret().getDot(), getCaret().getMark());
                int p1 = Math.max(getCaret().getDot(), getCaret().getMark());
                if (p0 != p1) {
                    doc.remove(p0, p1 - p0);
                }
                if (content != null && content.length() > 0) {
                    doc.insertString(p0, content, getInputAttributes().copyAttributes());
                }
            } catch (javax.swing.text.BadLocationException e) {
                // The position comes from the caret, which the document has just validated: it
                // cannot happen.
                throw new IllegalStateException(e.getMessage());
            }
        }
    }

    /**
     * It puts a component inside the text, as though it were a letter.
     *
     * <p>It takes up a single position in the document and the view draws it as it is. It is how
     * a button is put inside a paragraph.
     */
    public void insertComponent(Component c) {
        MutableAttributeSet inputAttributes = getInputAttributes();
        inputAttributes.removeAttributes(inputAttributes);
        javax.swing.text.StyleConstants.setComponent(inputAttributes, c);
        replaceSelection(" ", false);
        inputAttributes.removeAttributes(inputAttributes);
    }

    /** It puts an icon inside the text; see {@link #insertComponent}. */
    public void insertIcon(Icon g) {
        MutableAttributeSet inputAttributes = getInputAttributes();
        inputAttributes.removeAttributes(inputAttributes);
        javax.swing.text.StyleConstants.setIcon(inputAttributes, g);
        replaceSelection(" ", false);
        inputAttributes.removeAttributes(inputAttributes);
    }

    /**
     * It adds a named style.
     *
     * <p>The parent is where it inherits from what it does not define: it is what allows there to
     * be a "small title" that is the "title" with another size and nothing else.
     */
    public Style addStyle(String nm, Style parent) {
        StyledDocument doc = getStyledDocument();
        return doc.addStyle(nm, parent);
    }

    public void removeStyle(String nm) {
        StyledDocument doc = getStyledDocument();
        doc.removeStyle(nm);
    }

    public Style getStyle(String nm) {
        StyledDocument doc = getStyledDocument();
        return doc.getStyle(nm);
    }

    /** The background style of the paragraph the caret is in; see the class note. */
    public void setLogicalStyle(Style s) {
        StyledDocument doc = getStyledDocument();
        doc.setLogicalStyle(getCaretPosition(), s);
    }

    public Style getLogicalStyle() {
        StyledDocument doc = getStyledDocument();
        return doc.getLogicalStyle(getCaretPosition());
    }

    /** The attributes of the character the caret is at. */
    public AttributeSet getCharacterAttributes() {
        StyledDocument doc = getStyledDocument();
        javax.swing.text.Element run = doc.getCharacterElement(getCaretPosition());
        if (run != null) {
            return run.getAttributes();
        }
        return null;
    }

    /**
     * It applies those attributes to what is selected.
     *
     * <p>With no selection, they are left as input attributes -- see the class note --. With
     * {@code replace} at true whatever was there is discarded; at false it is merged.
     */
    public void setCharacterAttributes(AttributeSet attr, boolean replace) {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        if (p0 != p1) {
            StyledDocument doc = getStyledDocument();
            doc.setCharacterAttributes(p0, p1 - p0, attr, replace);
        } else {
            MutableAttributeSet inputAttributes = getInputAttributes();
            if (replace) {
                inputAttributes.removeAttributes(inputAttributes);
            }
            inputAttributes.addAttributes(attr);
        }
    }

    /** The attributes of the paragraph the caret is in. */
    public AttributeSet getParagraphAttributes() {
        StyledDocument doc = getStyledDocument();
        javax.swing.text.Element paragraph = doc.getParagraphElement(getCaretPosition());
        if (paragraph != null) {
            return paragraph.getAttributes();
        }
        return null;
    }

    /**
     * It applies those attributes to the paragraphs the selection touches.
     *
     * <p>A paragraph is applied whole even though the selection touches it at one letter: the
     * alignment or the indent cannot hold for half a line.
     */
    public void setParagraphAttributes(AttributeSet attr, boolean replace) {
        int p0 = getSelectionStart();
        int p1 = getSelectionEnd();
        StyledDocument doc = getStyledDocument();
        doc.setParagraphAttributes(p0, p1 - p0, attr, replace);
    }

    /** What is going to be applied to the next thing that is typed; see the class note. */
    public MutableAttributeSet getInputAttributes() {
        return getStyledEditorKit().getInputAttributes();
    }

    protected final StyledEditorKit getStyledEditorKit() {
        return (StyledEditorKit) getEditorKit();
    }

    protected EditorKit createDefaultEditorKit() {
        return new StyledEditorKit();
    }

    /**
     * It changes the editing engine.
     *
     * @throws IllegalArgumentException if it is not a {@link StyledEditorKit}; see the class note
     */
    public final void setEditorKit(EditorKit kit) {
        if (kit instanceof StyledEditorKit) {
            super.setEditorKit(kit);
        } else {
            throw new IllegalArgumentException("Must be StyledEditorKit");
        }
    }

    protected String paramString() {
        return super.paramString();
    }
}
