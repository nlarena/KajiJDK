package javax.swing.text;

import java.awt.Color;
import java.awt.Font;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$EventType;
import javax.swing.event.DocumentListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * A document with styles: a section, paragraphs and stretches of text with attributes.
 *
 * <h2>Three levels</h2>
 *
 * <p>The root is a <em>section</em>; its children are <em>paragraphs</em>, one per line ending;
 * a paragraph's children are the runs with the same attributes. Inserting text with attributes
 * different from those beside it splits a run in two; typing a line ending splits a paragraph.
 *
 * <h2>How an edit is applied</h2>
 *
 * <p>{@link #insertUpdate} translates the insertion into a list of {@link ElementSpec}s --"close
 * this paragraph", "open another", "put this text with these attributes"-- and
 * {@link ElementBuffer} applies it over the tree. That detour looks unnecessary and is not: the
 * same list may come from somewhere else --from an HTML reader, for instance-- and build a whole
 * tree at once.
 *
 * <h2>This ElementBuffer's limits</h2>
 *
 * <p>The JDK's knows how to <em>fracture</em>: split a paragraph in two through the middle of a
 * nested structure, with the {@code JoinFractureDirection} directions. That case is produced by
 * the HTML reader, which is not in this library. Here the fracture directions are treated as
 * {@code JoinNextDirection}, which is right for the three-level structure this document builds,
 * and it is said here because one day it may not be enough.
 */
public class DefaultStyledDocument extends AbstractDocument implements StyledDocument {

    /** The initial size of a styled document's content. */
    public static final int BUFFER_SIZE_DEFAULT = 4096;

    /** Who applies the lists of {@link ElementSpec}s over the tree. */
    protected ElementBuffer buffer;

    private transient Vector<Style> listeningStyles = new Vector<Style>();
    private transient ChangeListener styleChangeListener;
    private transient ChangeListener styleContextChangeListener;
    private transient ChangeListener styleListener;

    /** A document over that content and that style context. */
    public DefaultStyledDocument(Content c, StyleContext styles) {
        super(c, styles);
        listenerList = listenerList;
        buffer = new ElementBuffer(this, createDefaultRoot());
        Style defaultStyle = styles.getStyle(StyleContext.DEFAULT_STYLE);
        setLogicalStyle(0, defaultStyle);
    }

    public DefaultStyledDocument(StyleContext styles) {
        this(new GapContent(BUFFER_SIZE_DEFAULT), styles);
    }

    /** An empty document with the shared style context. */
    public DefaultStyledDocument() {
        this(new GapContent(BUFFER_SIZE_DEFAULT), new StyleContext());
    }

    public Element getDefaultRootElement() {
        return buffer.getRootElement();
    }

    /**
     * It builds the whole document from a list of specifications.
     *
     * <p>It erases whatever there was. It is how a reader builds a document at once, without going
     * through insertion after insertion.
     */
    protected void create(ElementSpec[] data) {
        try {
            writeLock();

            // Take out what is there.
            Element root = buffer.getRootElement();
            Element[] removed = new Element[root.getElementCount()];
            for (int i = 0; i < removed.length; i++) {
                removed[i] = root.getElement(i);
            }
            int len = getLength();
            if (len > 0) {
                getContent().remove(0, len);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                ElementSpec es = data[i];
                if (es.getLength() > 0 && es.getType() == ElementSpec.ContentType) {
                    sb.append(es.getArray(), es.getOffset(), es.getLength());
                }
            }
            if (sb.length() > 0) {
                getContent().insertString(0, sb.toString());
            }

            DefaultDocumentEvent evnt = new DefaultDocumentEvent(this, 0, getLength(),
                    DocumentEvent$EventType.INSERT);
            buffer.create(getLength(), data, evnt);
            evnt.end();
            fireInsertUpdate(evnt);
            fireChangedUpdate(evnt);
        } catch (BadLocationException ble) {
            throw new StateInvariantError("problem creating the document");
        } finally {
            writeUnlock();
        }
    }

    /** It inserts a list of specifications at that position. */
    protected void insert(int offset, ElementSpec[] data) throws BadLocationException {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            writeLock();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                ElementSpec es = data[i];
                if (es.getLength() > 0 && es.getType() == ElementSpec.ContentType) {
                    sb.append(es.getArray(), es.getOffset(), es.getLength());
                }
            }
            int length = sb.length();
            if (length > 0) {
                getContent().insertString(offset, sb.toString());
            }
            DefaultDocumentEvent evnt = new DefaultDocumentEvent(this, offset, length,
                    DocumentEvent$EventType.INSERT);
            buffer.insert(offset, length, data, evnt);
            evnt.end();
            fireInsertUpdate(evnt);
        } finally {
            writeUnlock();
        }
    }

    /** It takes that element out of the tree, and its text with it. */
    public void removeElement(Element elem) {
        try {
            removeElementImpl(elem);
        } catch (BadLocationException ble) {
            throw new IllegalArgumentException(ble.getMessage());
        }
    }

    private void removeElementImpl(Element elem) throws BadLocationException {
        if (elem.getDocument() != this) {
            throw new IllegalArgumentException("element doesn't belong to document");
        }
        BranchElement parent = (BranchElement) elem.getParentElement();
        if (parent == null) {
            throw new IllegalArgumentException("can't remove the root element");
        }
        int start = elem.getStartOffset();
        int end = Math.min(elem.getEndOffset(), getLength());
        remove(start, end - start);
    }

    public Style addStyle(String nm, Style parent) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.addStyle(nm, parent);
    }

    public void removeStyle(String nm) {
        StyleContext styles = (StyleContext) getAttributeContext();
        styles.removeStyle(nm);
    }

    public Style getStyle(String nm) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.getStyle(nm);
    }

    public Enumeration<?> getStyleNames() {
        return ((StyleContext) getAttributeContext()).getStyleNames();
    }

    /** It hangs that paragraph from that style; see {@link StyledDocument}. */
    public void setLogicalStyle(int pos, Style s) {
        Element paragraph = getParagraphElement(pos);
        if ((paragraph != null) && (paragraph instanceof AbstractElement)) {
            try {
                writeLock();
                StyleChangeUndoableEdit edit = new StyleChangeUndoableEdit(
                        (AbstractElement) paragraph, s);
                ((AbstractElement) paragraph).setResolveParent(s);
                int p0 = paragraph.getStartOffset();
                int p1 = paragraph.getEndOffset();
                DefaultDocumentEvent e = new DefaultDocumentEvent(this, p0, p1 - p0,
                        DocumentEvent$EventType.CHANGE);
                e.addEdit(edit);
                e.end();
                fireChangedUpdate(e);
                fireUndoableEditUpdate(new javax.swing.event.UndoableEditEvent(this, e));
            } finally {
                writeUnlock();
            }
        }
    }

    public Style getLogicalStyle(int p) {
        Style s = null;
        Element paragraph = getParagraphElement(p);
        if (paragraph != null) {
            AttributeSet a = paragraph.getAttributes();
            AttributeSet parent = a.getResolveParent();
            if (parent instanceof Style) {
                s = (Style) parent;
            }
        }
        return s;
    }

    /**
     * It applies character attributes to that stretch.
     *
     * <p>It splits the runs the range cuts through the middle: after this, every leaf has uniform
     * attributes, which is this document's invariant.
     */
    public void setCharacterAttributes(int offset, int length, AttributeSet s, boolean replace) {
        if (length == 0) {
            return;
        }
        try {
            writeLock();
            DefaultDocumentEvent changes = new DefaultDocumentEvent(this, offset, length,
                    DocumentEvent$EventType.CHANGE);

            buffer.change(offset, length, changes);

            AttributeSet sCopy = s.copyAttributes();
            int lastEnd;
            for (int pos = offset; pos < (offset + length); pos = lastEnd) {
                Element run = getCharacterElement(pos);
                lastEnd = run.getEndOffset();
                if (pos == lastEnd) {
                    break;
                }
                MutableAttributeSet attr = (MutableAttributeSet) run.getAttributes();
                changes.addEdit(new AttributeUndoableEdit(run, sCopy, replace));
                if (replace) {
                    attr.removeAttributes(attr);
                }
                attr.addAttributes(s);
            }
            changes.end();
            fireChangedUpdate(changes);
            fireUndoableEditUpdate(new javax.swing.event.UndoableEditEvent(this, changes));
        } finally {
            writeUnlock();
        }
    }

    /** It applies attributes to the paragraphs that stretch touches, whole. */
    public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace) {
        try {
            writeLock();
            DefaultDocumentEvent changes = new DefaultDocumentEvent(this, offset, length,
                    DocumentEvent$EventType.CHANGE);

            AttributeSet sCopy = s.copyAttributes();
            Element section = getDefaultRootElement();
            int index0 = section.getElementIndex(offset);
            int index1 = section.getElementIndex(offset + ((length > 0) ? length - 1 : 0));
            for (int i = index0; i <= index1; i++) {
                Element paragraph = section.getElement(i);
                MutableAttributeSet attr = (MutableAttributeSet) paragraph.getAttributes();
                changes.addEdit(new AttributeUndoableEdit(paragraph, sCopy, replace));
                if (replace) {
                    attr.removeAttributes(attr);
                }
                attr.addAttributes(s);
            }
            changes.end();
            fireChangedUpdate(changes);
            fireUndoableEditUpdate(new javax.swing.event.UndoableEditEvent(this, changes));
        } finally {
            writeUnlock();
        }
    }

    public Element getParagraphElement(int pos) {
        Element e;
        for (e = getDefaultRootElement(); !e.isLeaf();) {
            int index = e.getElementIndex(pos);
            e = e.getElement(index);
        }
        if (e != null) {
            return e.getParentElement();
        }
        return e;
    }

    public Element getCharacterElement(int pos) {
        Element e;
        for (e = getDefaultRootElement(); !e.isLeaf();) {
            int index = e.getElementIndex(pos);
            e = e.getElement(index);
        }
        return e;
    }

    /**
     * It fixes up the tree after an insertion.
     *
     * <p>It builds the list of specifications: the text goes as content, and each line ending is
     * translated into closing the paragraph and opening another.
     */
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        int offset = chng.getOffset();
        int length = chng.getLength();
        if (attr == null) {
            attr = SimpleAttributeSet.EMPTY;
        }

        Element paragraph = getParagraphElement(offset);
        AttributeSet pattr = paragraph.getAttributes();

        Segment s = new Segment();
        try {
            getText(offset, length, s);
        } catch (BadLocationException e) {
            throw new StateInvariantError("problema leyendo lo insertado");
        }

        Vector<ElementSpec> parseBuffer = new Vector<ElementSpec>();
        int lastOffset = 0;
        for (int i = 0; i < length; i++) {
            char c = s.array[s.offset + i];
            if (c == '\n') {
                int run = i - lastOffset + 1;
                parseBuffer.addElement(new ElementSpec(attr, ElementSpec.ContentType,
                        s.array, s.offset + lastOffset, run));
                parseBuffer.addElement(new ElementSpec(null, ElementSpec.EndTagType));
                parseBuffer.addElement(new ElementSpec(pattr, ElementSpec.StartTagType));
                lastOffset = i + 1;
            }
        }
        if (lastOffset < length) {
            parseBuffer.addElement(new ElementSpec(attr, ElementSpec.ContentType,
                    s.array, s.offset + lastOffset, length - lastOffset));
        }

        ElementSpec[] spec = new ElementSpec[parseBuffer.size()];
        parseBuffer.copyInto(spec);
        if (spec.length > 0) {
            spec[0].setDirection(ElementSpec.JoinPreviousDirection);
            if (spec.length > 1 && spec[spec.length - 1].getType() == ElementSpec.ContentType) {
                spec[spec.length - 1].setDirection(ElementSpec.JoinNextDirection);
            }
        }
        buffer.insert(offset, length, spec, chng);
        super.insertUpdate(chng, attr);
    }

    /**
     * It builds the specifications of an insertion after a line ending.
     *
     * <p>It returns the direction that corresponds to the first chunk. It is what decides whether
     * the new text sticks to the paragraph above or opens one of its own.
     */
    short createSpecsForInsertAfterNewline(Element paragraph, Element pParagraph,
            AttributeSet pattr, Vector<ElementSpec> parseBuffer, int offset, int endOffset) {
        if (paragraph.getStartOffset() == offset) {
            return ElementSpec.JoinNextDirection;
        }
        return ElementSpec.OriginateDirection;
    }

    /**
     * It joins the paragraphs the removal left half-done.
     *
     * <p>It runs <em>before</em> the text goes, which is when it can still be known how many
     * paragraphs the stretch touched: once removed, the two ends fall in the same place.
     */
    protected void removeUpdate(DefaultDocumentEvent chng) {
        super.removeUpdate(chng);
        buffer.remove(chng.getOffset(), chng.getLength(), chng);
    }

    /**
     * It removes the leaves the removal left empty.
     *
     * <p>It runs <em>afterwards</em>, which is when the leaves of what was removed have already
     * collapsed to zero length. Before there would be nothing to remove.
     */
    protected void postRemoveUpdate(DefaultDocumentEvent chng) {
        super.postRemoveUpdate(chng);
        buffer.clear(chng.getOffset(), chng);
    }

    /** The root section, with a paragraph with an empty run inside. */
    protected AbstractElement createDefaultRoot() {
        writeLock();
        BranchElement section = new SectionElement(this);
        BranchElement paragraph = new BranchElement(this, section, null);

        LeafElement brk = new LeafElement(this, paragraph, null, 0, 1);
        Element[] buff = new Element[1];
        buff[0] = brk;
        paragraph.replace(0, 0, buff);

        buff[0] = paragraph;
        section.replace(0, 0, buff);
        writeUnlock();
        return section;
    }

    public Color getForeground(AttributeSet attr) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.getForeground(attr);
    }

    public Color getBackground(AttributeSet attr) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.getBackground(attr);
    }

    public Font getFont(AttributeSet attr) {
        StyleContext styles = (StyleContext) getAttributeContext();
        return styles.getFont(attr);
    }

    /**
     * It reports that a style changed: everything hanging from it has to be repainted.
     *
     * <p>The event covers the whole document because the style may be used anywhere. It is
     * expensive and it is what the JDK does: finding exactly where it is used would cost more than
     * repainting.
     */
    protected void styleChanged(Style style) {
        DefaultDocumentEvent dde = new DefaultDocumentEvent(this, 0, getLength(),
                DocumentEvent$EventType.CHANGE);
        dde.end();
        fireChangedUpdate(dde);
    }

    /** On the first listener, this document starts listening to the styles. */
    public void addDocumentListener(DocumentListener listener) {
        synchronized (listeningStyles) {
            int oldDLCount = listenerList.getListenerCount(DocumentListener.class);
            super.addDocumentListener(listener);
            if (oldDLCount == 0) {
                if (styleContextChangeListener == null) {
                    styleContextChangeListener = createStyleContextChangeListener();
                }
                if (styleContextChangeListener != null) {
                    StyleContext styles = (StyleContext) getAttributeContext();
                    styles.addChangeListener(styleContextChangeListener);
                }
                updateStylesListeningTo();
            }
        }
    }

    /** With no listeners, it stops listening to the styles: nobody would hear about it. */
    public void removeDocumentListener(DocumentListener listener) {
        synchronized (listeningStyles) {
            super.removeDocumentListener(listener);
            if (listenerList.getListenerCount(DocumentListener.class) == 0) {
                for (int counter = listeningStyles.size() - 1; counter >= 0; counter--) {
                    listeningStyles.elementAt(counter).removeChangeListener(styleChangeListener);
                }
                listeningStyles.removeAllElements();
                if (styleContextChangeListener != null) {
                    StyleContext styles = (StyleContext) getAttributeContext();
                    styles.removeChangeListener(styleContextChangeListener);
                }
            }
        }
    }

    ChangeListener createStyleChangeListener() {
        return new StyleChangeHandler(this);
    }

    ChangeListener createStyleContextChangeListener() {
        return new StyleContextChangeHandler(this);
    }

    /** It looks again at which styles the paragraphs hang from, and listens to those. */
    void updateStylesListeningTo() {
        synchronized (listeningStyles) {
            StyleContext styles = (StyleContext) getAttributeContext();
            if (styleChangeListener == null) {
                styleChangeListener = createStyleChangeListener();
            }
            if (styleChangeListener != null && styles != null) {
                Element root = getDefaultRootElement();
                Vector<Style> seen = new Vector<Style>();
                for (int i = 0; i < root.getElementCount(); i++) {
                    Element p = root.getElement(i);
                    AttributeSet parent = p.getAttributes().getResolveParent();
                    if (parent instanceof Style) {
                        Style s = (Style) parent;
                        if (!seen.contains(s)) {
                            seen.addElement(s);
                        }
                    }
                }
                for (int i = listeningStyles.size() - 1; i >= 0; i--) {
                    Style s = listeningStyles.elementAt(i);
                    if (!seen.contains(s)) {
                        s.removeChangeListener(styleChangeListener);
                        listeningStyles.removeElementAt(i);
                    }
                }
                for (int i = 0; i < seen.size(); i++) {
                    Style s = seen.elementAt(i);
                    if (!listeningStyles.contains(s)) {
                        s.addChangeListener(styleChangeListener);
                        listeningStyles.addElement(s);
                    }
                }
            }
        }
    }

    /**
     * A structure instruction: open, close or put content.
     *
     * <p>It is the language the {@link ElementBuffer} is spoken to in. A document can be built
     * whole with a list of these, and that is why a reader of a foreign format needs to know
     * nothing about the tree: only to emit this sequence.
     */
    public static class ElementSpec {

        /** It opens an element. */
        public static final short StartTagType = 1;

        /** It closes the open element. */
        public static final short EndTagType = 2;

        /** It puts text. */
        public static final short ContentType = 3;

        /** It sticks to what was there before. */
        public static final short JoinPreviousDirection = 4;

        /** It sticks to what comes afterwards. */
        public static final short JoinNextDirection = 5;

        /** It starts something of its own. */
        public static final short OriginateDirection = 6;

        /** It sticks to what was left of a fracture; see {@link DefaultStyledDocument}'s note. */
        public static final short JoinFractureDirection = 7;

        private AttributeSet attr;
        private int len;
        private short type;
        private short direction;
        private int offs;
        private char[] data;

        /** An instruction with no content: open or close. */
        public ElementSpec(AttributeSet a, short type) {
            this(a, type, 0);
        }

        /** A content instruction of that length, with no text associated yet. */
        public ElementSpec(AttributeSet a, short type, int len) {
            attr = a;
            this.type = type;
            this.len = len;
            this.direction = OriginateDirection;
        }

        /** A content instruction with its text. */
        public ElementSpec(AttributeSet a, short type, char[] txt, int offs, int len) {
            attr = a;
            this.type = type;
            this.data = txt;
            this.offs = offs;
            this.len = len;
            this.direction = OriginateDirection;
        }

        public void setType(short type) {
            this.type = type;
        }

        public short getType() {
            return type;
        }

        public void setDirection(short direction) {
            this.direction = direction;
        }

        public short getDirection() {
            return direction;
        }

        public AttributeSet getAttributes() {
            return attr;
        }

        public char[] getArray() {
            return data;
        }

        public int getOffset() {
            return offs;
        }

        public int getLength() {
            return len;
        }

        public String toString() {
            String kind = "??";
            if (type == StartTagType) {
                kind = "StartTag";
            } else if (type == ContentType) {
                kind = "Content";
            } else if (type == EndTagType) {
                kind = "EndTag";
            }
            String dir = "??";
            if (direction == JoinPreviousDirection) {
                dir = "JoinPrevious";
            } else if (direction == JoinNextDirection) {
                dir = "JoinNext";
            } else if (direction == OriginateDirection) {
                dir = "Originate";
            } else if (direction == JoinFractureDirection) {
                dir = "Fracture";
            }
            return kind + ":" + dir + ":" + getLength();
        }
    }

    /**
     * It applies lists of {@link ElementSpec}s over the tree.
     *
     * <p>It keeps track of which elements left and which arrived, and notes them in the event: from
     * there come the {@code ElementChange}s an editor uses to repaint only what changed.
     *
     * <p>See {@link DefaultStyledDocument}'s note about what this buffer does not know how to do.
     */
    public static class ElementBuffer implements Serializable {

        /** The root it works over. */
        Element root;

        private final DefaultStyledDocument document;

        transient int pos;
        transient int offset;
        transient int length;
        transient int endOffset;
        transient boolean insertOp;

        /** A buffer over that root. */
        public ElementBuffer(DefaultStyledDocument document, Element root) {
            this.document = document;
            this.root = root;
        }

        public Element getRootElement() {
            return root;
        }

        /** It applies an insertion; see {@link #insertUpdate}. */
        public void insert(int offset, int length, ElementSpec[] data,
                DefaultDocumentEvent de) {
            if (length == 0) {
                return;
            }
            this.offset = offset;
            this.pos = offset;
            this.endOffset = offset + length;
            this.length = length;
            this.event = de;
            insertOp = true;
            insertUpdate(data);
            insertOp = false;
            this.event = null;
        }

        /** It builds the whole tree from the list. */
        public void create(int length, ElementSpec[] data, DefaultDocumentEvent de) {
            this.offset = 0;
            this.pos = 0;
            this.length = length;
            this.endOffset = length;
            this.event = de;

            BranchElement section = (BranchElement) root;
            Element[] oldElements = new Element[section.getElementCount()];
            for (int i = 0; i < oldElements.length; i++) {
                oldElements[i] = section.getElement(i);
            }

            Vector<Element> paragraphs = new Vector<Element>();
            BranchElement current = null;
            int p = 0;
            for (int i = 0; i < data.length; i++) {
                ElementSpec spec = data[i];
                if (spec.getType() == ElementSpec.StartTagType) {
                    current = new BranchElement(document, section, spec.getAttributes());
                    paragraphs.addElement(current);
                } else if (spec.getType() == ElementSpec.ContentType) {
                    if (current == null) {
                        current = new BranchElement(document, section, null);
                        paragraphs.addElement(current);
                    }
                    Element leaf = new LeafElement(document, current, spec.getAttributes(), p,
                            p + spec.getLength());
                    add(current, leaf);
                    p = p + spec.getLength();
                }
            }
            if (paragraphs.size() == 0) {
                current = new BranchElement(document, section, null);
                add(current, new LeafElement(document, current, null, 0, length));
                paragraphs.addElement(current);
            }
            Element[] newElements = new Element[paragraphs.size()];
            paragraphs.copyInto(newElements);
            section.replace(0, oldElements.length, newElements);
            de.addEdit(new ElementEdit(section, 0, oldElements, newElements));
            this.event = null;
        }

        /** It applies a removal; see {@link #removeUpdate}. */
        public void remove(int offset, int length, DefaultDocumentEvent de) {
            this.offset = offset;
            this.length = length;
            this.endOffset = offset + length;
            this.event = de;
            insertOp = false;
            removeUpdate();
            this.event = null;
        }

        /** It removes from that position's paragraph the leaves that were left empty. */
        void clear(int offset, DefaultDocumentEvent de) {
            this.event = de;
            BranchElement section = (BranchElement) root;
            int i = section.getElementIndex(offset);
            Element p = section.getElement(i);
            if (p instanceof BranchElement) {
                clearEmpty((BranchElement) p);
            }
            this.event = null;
        }

        /** It marks a stretch as changed, splitting the leaves the range cuts. */
        public void change(int offset, int length, DefaultDocumentEvent de) {
            this.offset = offset;
            this.length = length;
            this.endOffset = offset + length;
            this.event = de;
            changeUpdate();
            this.event = null;
        }

        private transient DefaultDocumentEvent event;

        /**
         * It applies the list over the tree, in three steps.
         *
         * <p>First it gives each inserted run its attributes, splitting the leaves that are left
         * half-done; then it cuts the paragraph at each line ending.
         *
         * <p>Two adjacent leaves with the same attributes are <strong>not</strong> joined, and it
         * is on purpose: the JDK does not join them either, and hence a freshly written document
         * has a separate run for the line ending at the end. Joining them would change the count of
         * children anybody walking the tree sees.
         */
        protected void insertUpdate(ElementSpec[] data) {
            BranchElement section = (BranchElement) root;
            int paragraphIndex = section.getElementIndex(offset);
            BranchElement paragraph = (BranchElement) section.getElement(paragraphIndex);

            int p = offset;
            Vector<Integer> splits = new Vector<Integer>();
            for (int i = 0; i < data.length; i++) {
                ElementSpec spec = data[i];
                short kind = spec.getType();
                if (kind == ElementSpec.ContentType) {
                    applyAttributes(paragraph, p, p + spec.getLength(), spec.getAttributes());
                    p = p + spec.getLength();
                } else if (kind == ElementSpec.EndTagType) {
                    splits.addElement(Integer.valueOf(p));
                }
            }

            BranchElement current = paragraph;
            int currentIndex = paragraphIndex;
            for (int i = 0; i < splits.size(); i++) {
                int split = splits.elementAt(i).intValue();
                current = splitParagraph(section, currentIndex, current, split);
                currentIndex = currentIndex + 1;
            }
        }

        /** It gives the run those attributes, splitting the leaves that are left half-done. */
        private void applyAttributes(BranchElement paragraph, int from, int to,
                AttributeSet attr) {
            if (to <= from) {
                return;
            }
            AttributeSet newElements = (attr == null) ? SimpleAttributeSet.EMPTY : attr;
            int p = from;
            while (p < to) {
                int index = paragraph.getElementIndex(p);
                Element leaf = paragraph.getElement(index);
                if (leaf == null) {
                    return;
                }
                int hd = leaf.getStartOffset();
                int hh = leaf.getEndOffset();
                if (hh <= p) {
                    return;
                }
                int split = Math.min(hh, to);
                // It already has those attributes: there is nothing to change and it is not split.
                // It is
                                // what keeps inserting unstyled text inside an unstyled run from
                                // leaving a cut.
                if (leaf.getAttributes().isEqual(newElements)) {
                    p = split;
                    continue;
                }
                Vector<Element> newLeaves = new Vector<Element>();
                if (hd < p) {
                    newLeaves.addElement(new LeafElement(document, paragraph, leaf.getAttributes(),
                            hd, p));
                }
                newLeaves.addElement(new LeafElement(document, paragraph, newElements, p, split));
                if (hh > split) {
                    newLeaves.addElement(new LeafElement(document, paragraph, leaf.getAttributes(),
                            split, hh));
                }
                Element[] addedLeaves = new Element[newLeaves.size()];
                newLeaves.copyInto(addedLeaves);
                Element[] oldLeaves = new Element[] {leaf};
                paragraph.replace(index, 1, addedLeaves);
                note(paragraph, index, oldLeaves, addedLeaves);
                p = split;
            }
        }

        /**
         * It cuts the paragraph at that position and returns the right-hand one.
         *
         * <p>The left-hand one keeps the same object --so the views that had it go on holding-- and
         * the right-hand one is new, with the same attributes.
         */
        private BranchElement splitParagraph(BranchElement section, int paragraphIndex,
                BranchElement paragraph, int split) {
            int end = paragraph.getEndOffset();
            if (split >= end) {
                return paragraph;
            }
            Vector<Element> left = new Vector<Element>();
            Vector<Element> right = new Vector<Element>();
            int n = paragraph.getElementCount();
            Element[] oldLeaves = new Element[n];
            for (int i = 0; i < n; i++) {
                Element h = paragraph.getElement(i);
                oldLeaves[i] = h;
                int hd = h.getStartOffset();
                int hh = h.getEndOffset();
                if (hh <= split) {
                    left.addElement(h);
                } else if (hd >= split) {
                    right.addElement(h);
                } else {
                    left.addElement(new LeafElement(document, paragraph, h.getAttributes(),
                            hd, split));
                    right.addElement(new LeafElement(document, paragraph, h.getAttributes(),
                            split, hh));
                }
            }

            BranchElement rest = new BranchElement(document, section, paragraph.getAttributes());
            Element[] leftElements = new Element[left.size()];
            left.copyInto(leftElements);
            if (leftElements.length == 0) {
                leftElements = new Element[] {new LeafElement(document, paragraph, null,
                        paragraph.getStartOffset(), split)};
            }
            paragraph.replace(0, n, leftElements);
            note(paragraph, 0, oldLeaves, leftElements);

            Element[] rightElements = new Element[right.size()];
            right.copyInto(rightElements);
            if (rightElements.length == 0) {
                rightElements = new Element[] {new LeafElement(document, rest, null, split, end)};
            }
            rest.replace(0, 0, rightElements);

            Element[] addedElements = new Element[] {rest};
            section.replace(paragraphIndex + 1, 0, addedElements);
            note(section, paragraphIndex + 1, new Element[0], addedElements);
            return rest;
        }

        /** It notes a change of children in the event in progress, if there is one. */
        private void note(Element parent, int index, Element[] oldElements, Element[] newElements) {
            if (event != null) {
                event.addEdit(new ElementEdit(parent, index, oldElements, newElements));
            }
        }

        /** It joins the paragraphs the removal left half-done. */
        protected void removeUpdate() {
            BranchElement section = (BranchElement) root;
            int i0 = section.getElementIndex(offset);
            int i1 = section.getElementIndex(endOffset);
            if (i0 == i1) {
                clearEmpty((BranchElement) section.getElement(i0));
                noteChange(section.getElement(i0));
                return;
            }

            Element[] oldElements = new Element[i1 - i0 + 1];
            for (int i = i0; i <= i1; i++) {
                oldElements[i - i0] = section.getElement(i);
            }

            BranchElement first = (BranchElement) section.getElement(i0);
            Vector<Element> leaves = new Vector<Element>();
            for (int i = i0; i <= i1; i++) {
                Element p = section.getElement(i);
                for (int j = 0; j < p.getElementCount(); j++) {
                    Element h = p.getElement(j);
                    if (h.getEndOffset() > h.getStartOffset()) {
                        leaves.addElement(new LeafElement(document, first, h.getAttributes(),
                                h.getStartOffset(), h.getEndOffset()));
                    }
                }
            }
            Element[] newLeaves = new Element[leaves.size()];
            leaves.copyInto(newLeaves);
            if (newLeaves.length == 0) {
                newLeaves = new Element[] {new LeafElement(document, first, null,
                        first.getStartOffset(), first.getStartOffset() + 1)};
            }
            first.replace(0, first.getElementCount(), newLeaves);

            Element[] newElements = new Element[] {first};
            section.replace(i0, oldElements.length, newElements);
            note(section, i0, oldElements, newElements);
            clearEmpty(first);
        }

        /** It splits the leaves the range cuts, so that the attributes are left uniform. */
        protected void changeUpdate() {
            BranchElement section = (BranchElement) root;
            int p = offset;
            while (p < endOffset) {
                Element paragraph = document.getParagraphElement(p);
                BranchElement br = (BranchElement) paragraph;
                int index = br.getElementIndex(p);
                Element leaf = br.getElement(index);
                int hd = leaf.getStartOffset();
                int hh = leaf.getEndOffset();
                int split = Math.min(hh, endOffset);
                if (hd < p || hh > split) {
                    Vector<Element> newLeaves = new Vector<Element>();
                    if (hd < p) {
                        newLeaves.addElement(new LeafElement(document, br, leaf.getAttributes(),
                                hd, p));
                    }
                    newLeaves.addElement(new LeafElement(document, br, leaf.getAttributes(), p,
                            split));
                    if (hh > split) {
                        newLeaves.addElement(new LeafElement(document, br, leaf.getAttributes(),
                                split, hh));
                    }
                    Element[] addedLeaves = new Element[newLeaves.size()];
                    newLeaves.copyInto(addedLeaves);
                    Element[] oldLeaves = new Element[] {leaf};
                    br.replace(index, 1, addedLeaves);
                    note(br, index, oldLeaves, addedLeaves);
                }
                p = split;
                if (split <= hd) {
                    break;
                }
            }
        }

        /**
         * It removes the leaves that were left with zero length.
         *
         * <p>A removal that eats a whole run leaves its leaf with beginning and end in the same
         * place. It does not bother the text, but it does bother everything that walks the tree: an
         * empty leaf represents nothing. If they all go, one is left covering the paragraph.
         */
        private void clearEmpty(BranchElement paragraph) {
            int n = paragraph.getElementCount();
            Vector<Element> alive = new Vector<Element>();
            for (int i = 0; i < n; i++) {
                Element h = paragraph.getElement(i);
                if (h.getEndOffset() > h.getStartOffset()) {
                    alive.addElement(h);
                }
            }
            if (alive.size() == n) {
                return;
            }
            Element[] oldLeaves = new Element[n];
            for (int i = 0; i < n; i++) {
                oldLeaves[i] = paragraph.getElement(i);
            }
            Element[] newLeaves;
            if (alive.size() == 0) {
                newLeaves = new Element[] {new LeafElement(document, paragraph, null,
                        paragraph.getStartOffset(), paragraph.getEndOffset())};
            } else {
                newLeaves = new Element[alive.size()];
                alive.copyInto(newLeaves);
            }
            paragraph.replace(0, n, newLeaves);
            note(paragraph, 0, oldLeaves, newLeaves);
        }

        /** It notes that that element changed without changing children. */
        private void noteChange(Element e) {
            if (e != null) {
                note(e, 0, new Element[0], new Element[0]);
            }
        }

        /** It adds a leaf at the end of a paragraph. */
        private void add(BranchElement paragraph, Element leaf) {
            Element[] one = new Element[] {leaf};
            paragraph.replace(paragraph.getElementCount(), 0, one);
        }
    }

    /** The root of a styled document; its name tells it apart from a paragraph. */
    protected class SectionElement extends BranchElement {

        public SectionElement(DefaultStyledDocument document) {
            super(document, null, null);
        }

        public String getName() {
            return SectionElementName;
        }
    }

    /** Undoing a change of attributes: the copy of how they were is kept. */
    public static class AttributeUndoableEdit extends AbstractUndoableEdit {

        protected AttributeSet newAttributes;
        protected AttributeSet copy;
        protected boolean isReplacing;
        protected Element element;

        public AttributeUndoableEdit(Element element, AttributeSet newAttributes,
                boolean isReplacing) {
            super();
            this.element = element;
            this.newAttributes = newAttributes;
            this.isReplacing = isReplacing;
            copy = element.getAttributes().copyAttributes();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            MutableAttributeSet as = (MutableAttributeSet) element.getAttributes();
            if (isReplacing) {
                as.removeAttributes(as);
            }
            as.addAttributes(newAttributes);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            MutableAttributeSet as = (MutableAttributeSet) element.getAttributes();
            as.removeAttributes(as);
            as.addAttributes(copy);
        }
    }

    /** Undoing a change of logical style. */
    static class StyleChangeUndoableEdit extends AbstractUndoableEdit {

        StyleChangeUndoableEdit(AbstractElement element, Style newStyle) {
            super();
            this.element = element;
            this.newStyle = newStyle;
            oldStyle = element.getResolveParent();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            element.setResolveParent(newStyle);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            element.setResolveParent(oldStyle);
        }

        protected AbstractElement element;
        protected Style newStyle;
        protected AttributeSet oldStyle;
    }

    /** A style changed: the document reports that a repaint is needed. */
    static class StyleChangeHandler implements ChangeListener, Serializable {

        private final DefaultStyledDocument document;

        StyleChangeHandler(DefaultStyledDocument document) {
            this.document = document;
        }

        public void stateChanged(ChangeEvent e) {
            Object source = e.getSource();
            if (source instanceof Style) {
                document.styleChanged((Style) source);
            } else {
                document.styleChanged(null);
            }
        }
    }

    /** The style set changed: which ones the paragraphs hang from has to be looked at again. */
    static class StyleContextChangeHandler implements ChangeListener, Serializable {

        private final DefaultStyledDocument document;

        StyleContextChangeHandler(DefaultStyledDocument document) {
            this.document = document;
        }

        public void stateChanged(ChangeEvent e) {
            document.updateStylesListeningTo();
        }
    }
}
