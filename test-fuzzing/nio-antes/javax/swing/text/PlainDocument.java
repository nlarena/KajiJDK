package javax.swing.text;

import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$EventType;
import javax.swing.undo.UndoableEdit;

/**
 * A document with no styles: only text, and a structure of a single layer of lines.
 *
 * <h2>The structure is the list of lines</h2>
 *
 * <p>The root has one child per line, and each child goes from the beginning of the line to after
 * its line ending. There are no paragraphs or styled runs: it is what makes this document cheap
 * and what an ordinary text area uses.
 *
 * <p>The whole class is keeping that list when something is inserted or removed. Inserting a text
 * with two line endings splits one line into three; removing a stretch that crosses line endings
 * joins those left half-done into a single one.
 *
 * <p>{@link #tabSizeAttribute} and {@link #lineLimitAttribute} are document properties, not text
 * attributes: whoever draws reads them to know how often a tab goes and where to break the
 * lines.
 */
public class PlainDocument extends AbstractDocument {

    /** The number of spaces of a tab. */
    public static final String tabSizeAttribute = "tabSize";

    /** The maximum width of a line, in characters. */
    public static final String lineLimitAttribute = "lineLimit";

    private AbstractElement defaultRoot;
    private Vector<Element> added = new Vector<Element>();
    private Vector<Element> removed = new Vector<Element>();

    /** An empty document over a gap content. */
    public PlainDocument() {
        this(new GapContent());
    }

    /** A document over that content. */
    public PlainDocument(Content c) {
        super(c);
        putProperty(tabSizeAttribute, Integer.valueOf(8));
        defaultRoot = createDefaultRoot();
    }

    /**
     * It inserts text.
     *
     * <p>The attributes are ignored --this document does not keep them-- unless they carry the
     * international text mark, which is propagated as a document property.
     */
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        super.insertString(offs, str, a);
    }

    public Element getDefaultRootElement() {
        return defaultRoot;
    }

    /** The root with a single line, which is what there is in an empty document. */
    protected AbstractElement createDefaultRoot() {
        BranchElement map = (BranchElement) createBranchElement(null, null);
        Element line = createLeafElement(map, null, 0, 1);
        Element[] lines = new Element[1];
        lines[0] = line;
        map.replace(0, 0, lines);
        return map;
    }

    /** In this document a paragraph is a line. */
    public Element getParagraphElement(int pos) {
        Element lineMap = getDefaultRootElement();
        int lineIndex = lineMap.getElementIndex(pos);
        return lineMap.getElement(lineIndex);
    }

    /**
     * It splits lines if the inserted text carries line endings.
     *
     * <p>The line the insertion fell in is replaced by as many as there are line endings, plus the
     * rest. That the whole line is replaced and not cut by hand is what makes the event carry the
     * exact list of what left and what arrived.
     */
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        removed.removeAllElements();
        added.removeAllElements();
        BranchElement lineMap = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        if (offset > 0) {
            offset = offset - 1;
            length = length + 1;
        }
        int index = lineMap.getElementIndex(offset);
        Element rmCandidate = lineMap.getElement(index);
        int rmOffs0 = rmCandidate.getStartOffset();
        int rmOffs1 = rmCandidate.getEndOffset();
        int lastOffset = rmOffs0;
        try {
            Segment s = new Segment();
            getText(offset, length, s);
            boolean hasBreaks = false;
            for (int i = 0; i < length; i++) {
                char c = s.array[s.offset + i];
                if (c == '\n') {
                    int breakOffset = offset + i + 1;
                    added.addElement(createLeafElement(lineMap, null, lastOffset, breakOffset));
                    lastOffset = breakOffset;
                    hasBreaks = true;
                }
            }
            if (hasBreaks) {
                removed.addElement(rmCandidate);
                if ((rmOffs1 > lastOffset) || (rmOffs1 == getLength() + 1)) {
                    added.addElement(createLeafElement(lineMap, null, lastOffset, rmOffs1));
                }
            }
        } catch (BadLocationException e) {
            throw new Error("Internal error: " + e.toString());
        }

        if (added.size() > 0 || removed.size() > 0) {
            Element[] aelems = new Element[added.size()];
            added.copyInto(aelems);
            Element[] relems = new Element[removed.size()];
            removed.copyInto(relems);
            ElementEdit ee = new ElementEdit(lineMap, index, relems, aelems);
            chng.addEdit(ee);
            lineMap.replace(index, relems.length, aelems);
        }
        super.insertUpdate(chng, attr);
    }

    /**
     * It joins the lines the removal left half-done.
     *
     * <p>It only does something if the removed stretch crossed at least one line ending: if not,
     * the line is still the same one, only shorter, and its positions already fixed themselves up.
     */
    protected void removeUpdate(DefaultDocumentEvent chng) {
        removed.removeAllElements();
        BranchElement map = (BranchElement) getDefaultRootElement();
        int offset = chng.getOffset();
        int length = chng.getLength();
        int line0 = map.getElementIndex(offset);
        int line1 = map.getElementIndex(offset + length);
        if (line0 != line1) {
            for (int i = line0; i <= line1; i++) {
                removed.addElement(map.getElement(i));
            }
            int p0 = map.getElement(line0).getStartOffset();
            int p1 = map.getElement(line1).getEndOffset();
            Element[] aelems = new Element[1];
            aelems[0] = createLeafElement(map, null, p0, p1);
            Element[] relems = new Element[removed.size()];
            removed.copyInto(relems);
            ElementEdit ee = new ElementEdit(map, line0, relems, aelems);
            chng.addEdit(ee);
            map.replace(line0, relems.length, aelems);
        }
        super.removeUpdate(chng);
    }
}
