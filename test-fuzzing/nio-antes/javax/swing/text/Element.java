package javax.swing.text;

/**
 * A node of a document's structure: a paragraph, a line, a run with the same formatting.
 *
 * <h2>Two views of the same text</h2>
 *
 * <p>A document is at the same time a flat sequence of characters and a tree of elements. The
 * elements do not keep text: they keep <strong>a stretch</strong> --{@link #getStartOffset} to
 * {@link #getEndOffset}-- and the attributes that hold there. That is why editing the text does
 * not rebuild the tree: the stretches shift.
 *
 * <p>One same document may have <em>several</em> trees at once over the same text -- one of
 * paragraphs and another of visual lines, which do not coincide when there is line wrapping.
 * Hence {@link Document#getRootElements} returns an array and not a single one.
 */
public interface Element {

    /** The document it belongs to. */
    Document getDocument();

    /** The element that contains it, or {@code null} if it is a root. */
    Element getParentElement();

    /** The element type's name. */
    String getName();

    /** The attributes that hold in this stretch. */
    AttributeSet getAttributes();

    /** Where the stretch starts. */
    int getStartOffset();

    /** Where the stretch ends. */
    int getEndOffset();

    /** Which of the children covers position {@code offset}. */
    int getElementIndex(int offset);

    /** How many children it has. */
    int getElementCount();

    /** Child number {@code index}. */
    Element getElement(int index);

    /**
     * Whether it has no children.
     *
     * <p>It is not the same as {@code getElementCount() == 0}: an element with children may be left
     * momentarily empty during an edit without ceasing to be a branch.
     */
    boolean isLeaf();
}
