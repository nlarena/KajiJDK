package javax.swing.text;

import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;

/**
 * The model of an editable text: content, structure and change notices.
 *
 * <h2>The three things it joins</h2>
 *
 * <p>A {@code Document} is at the same time the <strong>sequence of characters</strong>, the
 * <strong>element tree</strong> that structures it, and the <strong>broadcaster</strong> that
 * reports when something changes. Joining them is not laziness: the three have to move together
 * or the tree would be left describing a text that no longer is.
 *
 * <h2>{@link #render}, which is where the concurrency lives</h2>
 *
 * <p>An editor reads the document from the thread that paints and writes it from the one that
 * attends to the keyboard. {@code render} runs code with the guarantee that nobody modifies
 * meanwhile -- it is the only safe way of walking the text to draw it. Without it, a repaint
 * could read the document in the middle of an insertion.
 *
 * <p>Hence too the positions are asked for as {@link Position}s and not as integers: a number
 * kept between two edits points somewhere else.
 */
public interface Document {

    /** The key of the property that describes where the text came from. */
    public static final String StreamDescriptionProperty = "stream";

    /** The key of the title property. */
    public static final String TitleProperty = "title";

    /** How many characters it has. */
    int getLength();

    /** It adds a listener of content changes. */
    void addDocumentListener(DocumentListener listener);

    /** It removes a listener of content changes. */
    void removeDocumentListener(DocumentListener listener);

    /** It adds a listener of undoable edits. */
    void addUndoableEditListener(UndoableEditListener listener);

    /** It removes a listener of undoable edits. */
    void removeUndoableEditListener(UndoableEditListener listener);

    /** The value of a document property. */
    Object getProperty(Object key);

    /** It sets a document property. */
    void putProperty(Object key, Object value);

    /** It removes {@code length} characters from {@code offs}. */
    void remove(int offs, int len) throws BadLocationException;

    /** It inserts {@code str} at {@code offset}, with those attributes. */
    void insertString(int offset, String str, AttributeSet a) throws BadLocationException;

    /** A stretch's text, as a {@link String}. */
    String getText(int offset, int length) throws BadLocationException;

    /**
     * A stretch's text, without copying: see {@link Segment}.
     *
     * <p>The version to use on a hot path. The other one allocates.
     */
    void getText(int offset, int length, Segment txt) throws BadLocationException;

    /** A mark at the beginning, which stays there. */
    Position getStartPosition();

    /** A mark at the end, which follows the end. */
    Position getEndPosition();

    /** A mark at {@code offs}, which will move with the edits. */
    Position createPosition(int offs) throws BadLocationException;

    /** The roots of the structure trees; see {@link Element}. */
    Element[] getRootElements();

    /** The main tree's root. */
    Element getDefaultRootElement();

    /** It runs {@code r} with the guarantee that nobody modifies meanwhile. */
    void render(Runnable r);
}
