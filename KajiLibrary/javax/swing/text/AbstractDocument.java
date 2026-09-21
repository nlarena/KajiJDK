package javax.swing.text;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;
import javax.swing.event.DocumentEvent$EventType;
import javax.swing.event.DocumentListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.tree.TreeNode;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

/**
 * The base of every document: the text on one side, the structure on the other, and a lock
 * between the two.
 *
 * <h2>Three pieces</h2>
 *
 * <ul>
 * <li>The <strong>content</strong> ({@link Content}) keeps the characters and knows how to
 * create {@link Position}s, marks that move by themselves when something is inserted or removed
 * before them. It is the piece that keeps anything from having to walk the text to fix up
 * indices.
 * <li>The <strong>structure</strong> is a tree of {@link Element}s. Each element marks a stretch
 * of the content with {@code Position}s, so the structure survives the edits without being
 * recomputed. What shape the tree has is decided by the subclass: {@link PlainDocument} makes a
 * list of lines, {@link DefaultStyledDocument} makes sections, paragraphs and styled runs.
 * <li>The <strong>attribute context</strong> ({@link AttributeContext}) shares the repeated
 * attribute sets, which in a styled document are almost all of them.
 * </ul>
 *
 * <h2>The lock, and why it is single-writer</h2>
 *
 * <p>A document admits many readers at once and a single writer, and while there is a writer
 * nobody reads. The reason is not speed: it is that an edit changes the content and the
 * structure in two steps, and between those two steps the document is not consistent.
 * {@link #render} is the right way of reading without treading on anybody; the writing methods
 * take the lock themselves.
 *
 * <p>The writer may re-enter --an {@code insertUpdate} may call another write--, but a reader
 * that already has the read lock and asks to write is a program error, not a wait: it would be a
 * deadlock with itself, and that is why it throws {@link IllegalStateException}.
 *
 * <h2>The nested classes are static, and carry the document as a parameter</h2>
 *
 * <p>In the JDK {@link AbstractElement} and its kin are <em>inner</em> classes: each element has
 * an implicit reference to its document and the constructor is written
 * {@code new BranchElement(parent, attributes)}. Here they are static and the document goes
 * first: {@code new BranchElement(doc, parent, attributes)}. The resulting binary signature is
 * the same --an inner class compiles its outer one as its first parameter-- but in the source it
 * shows.
 *
 * <p>The reason is this project's compiler, not the design: an inner class cannot yet call a
 * sibling inner class's constructor nor one's {@code super(...)}, and there is no way of writing
 * that instance by hand (findings #507 and #508). When that is fixed, this goes back to the
 * JDK's form.
 *
 * <h2>What there is not</h2>
 *
 * <p>There is no bidirectional analysis. {@link #getBidiRootElement} returns a structure of a
 * single left-to-right run, which is exactly what the JDK builds for a text that does not mix
 * scripts, and {@link #isLeftToRight} always answers {@code true}. A text in Arabic or Hebrew is
 * kept correctly and is shown in the order it is in, without reordering.
 */
public abstract class AbstractDocument implements Document, Serializable {

    /** The message of the position exceptions. */
    protected static final String BAD_LOCATION = "document location failure";

    public static final String ParagraphElementName = "paragraph";

    public static final String ContentElementName = "content";

    /** The name of a styled document's root element. */
    public static final String SectionElementName = "section";

    /** The name of the bidirectional tree's elements. */
    public static final String BidiElementName = "bidi level";

    /** The key of the attribute that keeps an element's name. */
    public static final String ElementNameAttribute = "$ename";

    /** The property that marks a document with international text. */
    static final String I18NProperty = "i18n";

    /** The property that marks multi-byte content. */
    static final Object MultiByteProperty = "multiByte";

    /** The property with the asynchronous loading priority. */
    static final String AsyncLoadPriority = "load priority";

    protected EventListenerList listenerList = new EventListenerList();

    private transient Content data;
    private transient AttributeContext context;
    private transient BranchElement bidiRoot;
    private transient Dictionary<Object, Object> documentProperties;
    private transient Thread currentWriter = null;
    private transient int numWriters = 0;
    private transient int numReaders = 0;
    private transient boolean notifyingListeners = false;
    private transient DocumentFilter documentFilter;
    private transient DocumentFilterFilterBypass filterBypass;

    /** A document over that content, with the shared style context. */
    protected AbstractDocument(Content data) {
        this(data, StyleContext.getDefaultStyleContext());
    }

    protected AbstractDocument(Content data, AttributeContext context) {
        this.data = data;
        this.context = context;

        // The root and the bidi run are a BranchElement and a LeafElement with their name set by
                // attribute, and not two subclasses: an inner class cannot call a sibling inner
                // class's `super(...)` with our javac (#508), and the name by attribute comes to
                // the same. With the lock held: creating an element with attributes is writing in
                // the document, and the element itself checks it.
        writeLock();
        try {
            bidiRoot = new BranchElement(this, null, bidiName("bidi root"));
            Element[] p = new Element[1];
            p[0] = new LeafElement(this, bidiRoot, bidiLevel(0), 0, 0);
            bidiRoot.replace(0, 0, p);
        } finally {
            writeUnlock();
        }
    }

    /** The document's properties --title, character set--; they are created on first use. */
    public Dictionary<Object, Object> getDocumentProperties() {
        if (documentProperties == null) {
            documentProperties = new Hashtable<Object, Object>(2);
        }
        return documentProperties;
    }

    public void setDocumentProperties(Dictionary<Object, Object> x) {
        documentProperties = x;
    }

    /** It reports an insertion; it is called with the write lock held. */
    protected void fireInsertUpdate(DocumentEvent e) {
        notifyingListeners = true;
        try {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i = i - 2) {
                if (listeners[i] == DocumentListener.class) {
                    ((DocumentListener) listeners[i + 1]).insertUpdate(e);
                }
            }
        } finally {
            notifyingListeners = false;
        }
    }

    protected void fireChangedUpdate(DocumentEvent e) {
        notifyingListeners = true;
        try {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i = i - 2) {
                if (listeners[i] == DocumentListener.class) {
                    ((DocumentListener) listeners[i + 1]).changedUpdate(e);
                }
            }
        } finally {
            notifyingListeners = false;
        }
    }

    protected void fireRemoveUpdate(DocumentEvent e) {
        notifyingListeners = true;
        try {
            Object[] listeners = listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i = i - 2) {
                if (listeners[i] == DocumentListener.class) {
                    ((DocumentListener) listeners[i + 1]).removeUpdate(e);
                }
            }
        } finally {
            notifyingListeners = false;
        }
    }

    protected void fireUndoableEditUpdate(UndoableEditEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == UndoableEditListener.class) {
                ((UndoableEditListener) listeners[i + 1]).undoableEditHappened(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** The asynchronous loading priority; negative means load on the calling thread. */
    public int getAsynchronousLoadPriority() {
        Integer loadPriority = (Integer) getProperty(AbstractDocument.AsyncLoadPriority);
        if (loadPriority != null) {
            return loadPriority.intValue();
        }
        return -1;
    }

    public void setAsynchronousLoadPriority(int p) {
        Integer loadPriority = (p < 0) ? null : Integer.valueOf(p);
        putProperty(AbstractDocument.AsyncLoadPriority, loadPriority);
    }

    /** The filter that can veto or change each edit; see {@link DocumentFilter}. */
    public void setDocumentFilter(DocumentFilter filter) {
        documentFilter = filter;
    }

    public DocumentFilter getDocumentFilter() {
        return documentFilter;
    }

    /** It runs that with the read lock held; see the class note. */
    public void render(Runnable r) {
        readLock();
        try {
            r.run();
        } finally {
            readUnlock();
        }
    }

    public int getLength() {
        return data.length() - 1;
    }

    public void addDocumentListener(DocumentListener listener) {
        listenerList.add(DocumentListener.class, listener);
    }

    public void removeDocumentListener(DocumentListener listener) {
        listenerList.remove(DocumentListener.class, listener);
    }

    public DocumentListener[] getDocumentListeners() {
        return listenerList.getListeners(DocumentListener.class);
    }

    public void addUndoableEditListener(UndoableEditListener listener) {
        listenerList.add(UndoableEditListener.class, listener);
    }

    public void removeUndoableEditListener(UndoableEditListener listener) {
        listenerList.remove(UndoableEditListener.class, listener);
    }

    public UndoableEditListener[] getUndoableEditListeners() {
        return listenerList.getListeners(UndoableEditListener.class);
    }

    public final Object getProperty(Object key) {
        return getDocumentProperties().get(key);
    }

    public final void putProperty(Object key, Object value) {
        if (value != null) {
            getDocumentProperties().put(key, value);
        } else {
            getDocumentProperties().remove(key);
        }
    }

    /**
     * It removes a stretch; it goes through the filter if there is one.
     *
     * <p>Removing zero characters does nothing, not even report: it is what keeps a
     * {@code replace} with no removal from producing an extra event.
     */
    public void remove(int offs, int len) throws BadLocationException {
        DocumentFilter filter = getDocumentFilter();

        writeLock();
        try {
            if (filter != null) {
                filter.remove(getFilterBypass(), offs, len);
            } else {
                handleRemove(offs, len);
            }
        } finally {
            writeUnlock();
        }
    }

    /** The real removal, already past the filter. */
    void handleRemove(int offs, int len) throws BadLocationException {
        if (len > 0) {
            if (offs < 0 || (offs + len) > getLength()) {
                throw new BadLocationException("Invalid remove", getLength() + 1);
            }
            DefaultDocumentEvent chng = new DefaultDocumentEvent(this, offs, len,
                    DocumentEvent$EventType.REMOVE);

            boolean isComposedTextElement = false;

            removeUpdate(chng);
            UndoableEdit u = data.remove(offs, len);
            if (u != null) {
                chng.addEdit(u);
            }
            postRemoveUpdate(chng);
            updateBidi(chng);
            chng.end();
            fireRemoveUpdate(chng);
            if ((u != null) && !isComposedTextElement) {
                fireUndoableEditUpdate(new UndoableEditEvent(this, chng));
            }
        }
    }

    /**
     * It removes and inserts as a single operation.
     *
     * <p>That it is a single one matters for undo: without this, undoing a replacement would leave
     * the text removed and not put back. Without a filter, they are a removal and an insertion, and
     * that is why two events come out; with a filter, the filter decides.
     */
    public void replace(int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (length == 0 && (text == null || text.length() == 0)) {
            return;
        }
        DocumentFilter filter = getDocumentFilter();

        writeLock();
        try {
            if (filter != null) {
                filter.replace(getFilterBypass(), offset, length, text, attrs);
            } else {
                if (length > 0) {
                    remove(offset, length);
                }
                if (text != null && text.length() > 0) {
                    insertString(offset, text, attrs);
                }
            }
        } finally {
            writeUnlock();
        }
    }

    /** It inserts text; it goes through the filter if there is one. */
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if ((str == null) || (str.length() == 0)) {
            return;
        }
        DocumentFilter filter = getDocumentFilter();

        writeLock();
        try {
            if (filter != null) {
                filter.insertString(getFilterBypass(), offs, str, a);
            } else {
                handleInsertString(offs, str, a);
            }
        } finally {
            writeUnlock();
        }
    }

    /** The real insertion, already past the filter. */
    void handleInsertString(int offs, String str, AttributeSet a) throws BadLocationException {
        if ((str == null) || (str.length() == 0)) {
            return;
        }
        UndoableEdit u = data.insertString(offs, str);
        DefaultDocumentEvent e = new DefaultDocumentEvent(this, offs, str.length(),
                DocumentEvent$EventType.INSERT);
        if (u != null) {
            e.addEdit(u);
        }

        insertUpdate(e, a);
        updateBidi(e);

        e.end();
        fireInsertUpdate(e);
        if (u != null) {
            fireUndoableEditUpdate(new UndoableEditEvent(this, e));
        }
    }

    public String getText(int offset, int length) throws BadLocationException {
        if (length < 0) {
            throw new BadLocationException("Length must be positive", length);
        }
        String str = data.getString(offset, length);
        return str;
    }

    /**
     * The text without copying it: the segment points at the content's array when it can.
     *
     * <p>It is the form the drawing uses, which walks the text many times per second and cannot
     * afford one copy per frame.
     */
    public void getText(int offset, int length, Segment txt) throws BadLocationException {
        if (length < 0) {
            throw new BadLocationException("Length must be positive", length);
        }
        data.getChars(offset, length, txt);
    }

    /** A mark that moves with the text; see the class note. */
    public synchronized Position createPosition(int offs) throws BadLocationException {
        return data.createPosition(offs);
    }

    /** The document's beginning; it never moves. */
    public final Position getStartPosition() {
        Position p;
        try {
            p = createPosition(0);
        } catch (BadLocationException bl) {
            p = null;
        }
        return p;
    }

    /** The document's end; it shifts with every insertion. */
    public final Position getEndPosition() {
        Position p;
        try {
            p = createPosition(data.length());
        } catch (BadLocationException bl) {
            p = null;
        }
        return p;
    }

    /** The two roots: the structure's and the bidirectional one. */
    public Element[] getRootElements() {
        Element[] elems = new Element[2];
        elems[0] = getDefaultRootElement();
        elems[1] = getBidiRootElement();
        return elems;
    }

    public abstract Element getDefaultRootElement();

    /** See the class note about what there is not. */
    public Element getBidiRootElement() {
        return bidiRoot;
    }

    /** Always {@code true}; see the class note. */
    static boolean isLeftToRight(Document doc, int p0, int p1) {
        return true;
    }

    public abstract Element getParagraphElement(int pos);

    protected final AttributeContext getAttributeContext() {
        return context;
    }

    /**
     * The structure, after an insertion in the content.
     *
     * <p>The subclass redefines it to fix up its tree. The version here does nothing: a document
     * with no structure of its own has nothing to fix up.
     */
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
    }

    /** The structure, before taking the text out of the content. */
    protected void removeUpdate(DefaultDocumentEvent chng) {
    }

    /** The structure, after taking the text out; here what was removed can no longer be read. */
    protected void postRemoveUpdate(DefaultDocumentEvent chng) {
    }

    /** The attributes of a bidi element with that name. */
    private AttributeSet bidiName(String name) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(ElementNameAttribute, name);
        return a;
    }

    /** The attributes of a bidi run of that level. */
    private AttributeSet bidiLevel(int level) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(ElementNameAttribute, BidiElementName);
        a.addAttribute(StyleConstants.BidiLevel, Integer.valueOf(level));
        return a;
    }

    /**
     * It rebuilds the bidirectional run so that it covers the whole document.
     *
     * <p>A single run, left to right; see the class note. It reaches {@code getLength() + 1}, that
     * is it includes the implicit line ending at the end: the bidirectional run covers the content,
     * not the text the user sees. It is rebuilt instead of moved because that end is not a
     * {@link Position} that shifts by itself.
     */
    void updateBidi(DefaultDocumentEvent chng) {
        Element[] p = new Element[1];
        p[0] = new LeafElement(this, bidiRoot, bidiLevel(0), 0, getLength() + 1);
        int n = bidiRoot.getElementCount();
        bidiRoot.replace(0, n, p);
    }

    /** It prints the tree; it is a document's debugging tool. */
    public void dump(PrintStream out) {
        Element root = getDefaultRootElement();
        if (root instanceof AbstractElement) {
            ((AbstractElement) root).dump(out, 0);
        }
        ((AbstractElement) getBidiRootElement()).dump(out, 0);
    }

    protected final Content getContent() {
        return data;
    }

    /** A leaf element: a stretch of text with attributes. */
    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
        return new LeafElement(this, parent, a, p0, p1);
    }

    /** A branch element: an element with children, whose range is theirs. */
    protected Element createBranchElement(Element parent, AttributeSet a) {
        return new BranchElement(this, parent, a);
    }

    protected final synchronized Thread getCurrentWriter() {
        return currentWriter;
    }

    /**
     * It takes the write lock; it waits until no reader is left.
     *
     * <p>A writer that already has it re-enters without waiting. A reader that asks to write is a
     * program error; see the class note.
     */
    protected final synchronized void writeLock() {
        try {
            while ((numReaders > 0) || (currentWriter != null)) {
                if (Thread.currentThread() == currentWriter) {
                    if (notifyingListeners) {
                        // A listener cannot modify the document: the other listeners would receive
                        // an
                                                // event that no longer describes what is there.
                        throw new IllegalStateException(
                                "Attempt to mutate in notification");
                    }
                    numWriters = numWriters + 1;
                    return;
                }
                wait();
            }
            currentWriter = Thread.currentThread();
            numWriters = 1;
        } catch (InterruptedException e) {
            throw new Error("Interrupted attempt to acquire write lock");
        }
    }

    protected final synchronized void writeUnlock() {
        numWriters = numWriters - 1;
        if (numWriters <= 0) {
            numWriters = 0;
            currentWriter = null;
            notifyAll();
        }
    }

    public final synchronized void readLock() {
        try {
            while (currentWriter != null) {
                if (currentWriter == Thread.currentThread()) {
                    // The writer may read what it is writing itself.
                    return;
                }
                wait();
            }
            numReaders = numReaders + 1;
        } catch (InterruptedException e) {
            throw new Error("Interrupted attempt to acquire read lock");
        }
    }

    public final synchronized void readUnlock() {
        if (currentWriter == Thread.currentThread()) {
            return;
        }
        if (numReaders <= 0) {
            throw new StateInvariantError("Unbalanced readLock/readUnlock");
        }
        numReaders = numReaders - 1;
        notify();
    }

    private DocumentFilter.FilterBypass getFilterBypass() {
        if (filterBypass == null) {
            filterBypass = new DocumentFilterFilterBypass();
        }
        return filterBypass;
    }

    /** The shortcut the filter uses to write without going through itself again. */
    private class DocumentFilterFilterBypass extends DocumentFilter.FilterBypass {

        public Document getDocument() {
            return AbstractDocument.this;
        }

        public void remove(int offset, int length) throws BadLocationException {
            handleRemove(offset, length);
        }

        public void insertString(int offset, String string, AttributeSet attr)
                throws BadLocationException {
            handleInsertString(offset, string, attr);
        }

        public void replace(int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (length > 0) {
                handleRemove(offset, length);
            }
            if (text != null && text.length() > 0) {
                handleInsertString(offset, text, attrs);
            }
        }
    }

    /**
     * What keeps the characters.
     *
     * <p>Separate from the document because there is more than one reasonable way of keeping them:
     * a moving gap that makes successive edits in one same place cheap ({@link GapContent}), or a
     * plain string ({@link StringContent}). The {@link Position}s are this piece's responsibility
     * because only it knows when the characters shift.
     *
     * <p>The content always has at least one character, an implicit line ending, and that is why
     * {@link AbstractDocument#getLength} subtracts one.
     */
    public interface Content {

        Position createPosition(int offset) throws BadLocationException;

        int length();

        UndoableEdit insertString(int where, String str) throws BadLocationException;

        UndoableEdit remove(int where, int nitems) throws BadLocationException;

        String getString(int where, int len) throws BadLocationException;

        /** The text without copying it when it can; see {@link AbstractDocument#getText}. */
        void getChars(int where, int len, Segment txt) throws BadLocationException;
    }

    /**
     * Who shares the attribute sets.
     *
     * <p>The sets are immutable: changing an attribute returns another set. It sounds expensive and
     * it is the reverse: a document with a thousand italic paragraphs keeps a single "italic" set,
     * and comparing two runs is comparing two references.
     */
    public interface AttributeContext {

        AttributeSet addAttribute(AttributeSet old, Object name, Object value);

        AttributeSet addAttributes(AttributeSet old, AttributeSet attr);

        AttributeSet removeAttribute(AttributeSet old, Object name);

        AttributeSet removeAttributes(AttributeSet old, Enumeration<?> names);

        AttributeSet removeAttributes(AttributeSet old, AttributeSet attrs);

        AttributeSet getEmptySet();

        /**
         * It reports that that set is no longer used; the context decides whether to throw it away.
         */
        void reclaim(AttributeSet a);
    }

    /**
     * The change an element underwent: which children left and which arrived.
     *
     * <p>It is also an undoable edit, and that is why it keeps both lists: undoing is putting back
     * those that left.
     */
    public static class ElementEdit extends AbstractUndoableEdit implements
            DocumentEvent$ElementChange {

        private Element e;
        private int index;
        private Element[] removed;
        private Element[] added;

        public ElementEdit(Element e, int index, Element[] removed, Element[] added) {
            super();
            this.e = e;
            this.index = index;
            this.removed = removed;
            this.added = added;
        }

        public Element getElement() {
            return e;
        }

        /** Where the change started, counting children. */
        public int getIndex() {
            return index;
        }

        public Element[] getChildrenRemoved() {
            return removed;
        }

        public Element[] getChildrenAdded() {
            return added;
        }

        public void redo() throws CannotRedoException {
            super.redo();
            Element[] tmp = removed;
            removed = added;
            added = tmp;
            ((AbstractDocument.BranchElement) e).replace(index, removed.length, added);
        }

        public void undo() throws CannotUndoException {
            super.undo();
            ((AbstractDocument.BranchElement) e).replace(index, added.length, removed);
            Element[] tmp = removed;
            removed = added;
            added = tmp;
        }
    }

    /**
     * An element of the tree, which is also its own attribute set.
     *
     * <p>That union is not laziness: an element's attributes are consulted as many times as the
     * element is, and having them in the same object saves one hop per query. The real attributes
     * live in the shared context; this object keeps a reference to the immutable set and replaces
     * it every time it changes.
     */
    public abstract static class AbstractElement implements Element, MutableAttributeSet,
            Serializable, TreeNode {

        /** The document it belongs to; see the note about the nested classes. */
        protected final AbstractDocument document;

        private Element parent;
        private transient AttributeSet attributes;

        public AbstractElement(AbstractDocument document, Element parent, AttributeSet a) {
            this.document = document;
            this.parent = parent;
            attributes = document.getAttributeContext().getEmptySet();
            if (a != null) {
                addAttributes(a);
            }
        }

        /** It prints this element and its children, indented; see {@link AbstractDocument#dump}. */
        public void dump(PrintStream psOut, int indentAmount) {
            String indentation = "";
            for (int i = 0; i < indentAmount; i++) {
                indentation = indentation + "  ";
            }
            psOut.print(indentation + "<" + getName());
            Enumeration<?> names = getAttributeNames();
            while (names.hasMoreElements()) {
                Object name = names.nextElement();
                psOut.print(" " + name + "=" + getAttribute(name));
            }
            psOut.println(">");

            if (getElementCount() == 0) {
                int start = getStartOffset();
                int end = getEndOffset();
                psOut.print(indentation + "  [" + start + "," + end + "]");
                try {
                    String text = getDocument().getText(start, end - start);
                    psOut.print("[" + text + "]");
                } catch (BadLocationException e) {
                    // An element may be out of range while it is being edited; it is not an error.
                }
                psOut.println("");
            } else {
                for (int i = 0; i < getElementCount(); i++) {
                    ((AbstractElement) getElement(i)).dump(psOut, indentAmount + 1);
                }
            }
        }

        public int getAttributeCount() {
            return attributes.getAttributeCount();
        }

        public boolean isDefined(Object attrName) {
            return attributes.isDefined(attrName);
        }

        public boolean isEqual(AttributeSet attr) {
            return attributes.isEqual(attr);
        }

        public AttributeSet copyAttributes() {
            return attributes.copyAttributes();
        }

        public Object getAttribute(Object attrName) {
            Object value = attributes.getAttribute(attrName);
            if (value == null) {
                // The tree's parent acts as the resolving parent: a paragraph inherits from the
                // document.
                AttributeSet a = attributes.getResolveParent();
                if (a == null && parent != null) {
                    a = parent.getAttributes();
                }
                if (a != null) {
                    value = a.getAttribute(attrName);
                }
            }
            return value;
        }

        public Enumeration<?> getAttributeNames() {
            return attributes.getAttributeNames();
        }

        public boolean containsAttribute(Object name, Object value) {
            return attributes.containsAttribute(name, value);
        }

        public boolean containsAttributes(AttributeSet attrs) {
            return attributes.containsAttributes(attrs);
        }

        public AttributeSet getResolveParent() {
            AttributeSet a = attributes.getResolveParent();
            if ((a == null) && (parent != null)) {
                a = parent.getAttributes();
            }
            return a;
        }

        public void addAttribute(Object name, Object value) {
            checkForIllegalCast();
            AttributeContext context = document.getAttributeContext();
            attributes = context.addAttribute(attributes, name, value);
        }

        public void addAttributes(AttributeSet attr) {
            checkForIllegalCast();
            AttributeContext context = document.getAttributeContext();
            attributes = context.addAttributes(attributes, attr);
        }

        public void removeAttribute(Object name) {
            checkForIllegalCast();
            AttributeContext context = document.getAttributeContext();
            attributes = context.removeAttribute(attributes, name);
        }

        public void removeAttributes(Enumeration<?> names) {
            checkForIllegalCast();
            AttributeContext context = document.getAttributeContext();
            attributes = context.removeAttributes(attributes, names);
        }

        public void removeAttributes(AttributeSet attrs) {
            checkForIllegalCast();
            AttributeContext context = document.getAttributeContext();
            if (attrs == this) {
                attributes = context.getEmptySet();
            } else {
                attributes = context.removeAttributes(attributes, attrs);
            }
        }

        public void setResolveParent(AttributeSet parent) {
            checkForIllegalCast();
            AttributeContext context = document.getAttributeContext();
            if (parent != null) {
                attributes = context.addAttribute(attributes, StyleConstants.ResolveAttribute,
                        parent);
            } else {
                attributes = context.removeAttribute(attributes,
                        StyleConstants.ResolveAttribute);
            }
        }

        /** Changing attributes is writing in the document: the lock is needed. */
        private void checkForIllegalCast() {
            Thread t = document.getCurrentWriter();
            if ((t == null) || (t != Thread.currentThread())) {
                throw new StateInvariantError("Illegal cast to MutableAttributeSet");
            }
        }

        public Document getDocument() {
            return document;
        }

        public Element getParentElement() {
            return parent;
        }

        public AttributeSet getAttributes() {
            return this;
        }

        /** The element's name, from the attribute that keeps it. */
        public String getName() {
            if (attributes.isDefined(ElementNameAttribute)) {
                return (String) attributes.getAttribute(ElementNameAttribute);
            }
            return null;
        }

        public abstract int getStartOffset();

        public abstract int getEndOffset();

        public abstract Element getElement(int index);

        public abstract int getElementCount();

        public abstract int getElementIndex(int offset);

        public abstract boolean isLeaf();

        // -- TreeNode: the same structure, seen as a tree ---------------------------------------

        public TreeNode getChildAt(int childIndex) {
            return (AbstractElement) getElement(childIndex);
        }

        public int getChildCount() {
            return getElementCount();
        }

        public TreeNode getParent() {
            return (AbstractElement) getParentElement();
        }

        public int getIndex(TreeNode node) {
            for (int counter = getChildCount() - 1; counter >= 0; counter--) {
                if (getChildAt(counter) == node) {
                    return counter;
                }
            }
            return -1;
        }

        public abstract boolean getAllowsChildren();

        public abstract Enumeration<TreeNode> children();
    }

    /** An element with children; its range is theirs. */
    public static class BranchElement extends AbstractElement {

        private AbstractElement[] children;
        private int nchildren;
        private int lastIndex;

        public BranchElement(AbstractDocument document, Element parent, AttributeSet a) {
            super(document, parent, a);
            children = new AbstractElement[1];
            nchildren = 0;
            lastIndex = -1;
        }

        /** The child that contains that position, or {@code null} if it is outside. */
        public Element positionToElement(int pos) {
            int index = getElementIndex(pos);
            Element child = children[index];
            int p0 = child.getStartOffset();
            int p1 = child.getEndOffset();
            if ((pos >= p0) && (pos < p1)) {
                return child;
            }
            return null;
        }

        /** It swaps a stretch of children for another; it is the basic restructuring operation. */
        public void replace(int offset, int length, Element[] elems) {
            int delta = elems.length - length;
            int src = offset + length;
            int nmove = nchildren - src;
            int dest = src + delta;
            if ((nchildren + delta) >= children.length) {
                // The array grows by doublings: restructuring is frequent.
                int newLength = Math.max(2 * children.length, nchildren + delta);
                AbstractElement[] newChildren = new AbstractElement[newLength];
                System.arraycopy(children, 0, newChildren, 0, offset);
                System.arraycopy(elems, 0, newChildren, offset, elems.length);
                System.arraycopy(children, src, newChildren, dest, nmove);
                children = newChildren;
            } else {
                System.arraycopy(children, src, children, dest, nmove);
                System.arraycopy(elems, 0, children, offset, elems.length);
            }
            nchildren = nchildren + delta;
        }

        public String toString() {
            return "BranchElement(" + getName() + ") " + getStartOffset() + ","
                    + getEndOffset() + "\n";
        }

        public String getName() {
            String nm = super.getName();
            if (nm == null) {
                nm = ParagraphElementName;
            }
            return nm;
        }

        public int getStartOffset() {
            return children[0].getStartOffset();
        }

        public int getEndOffset() {
            Element child = (nchildren > 0) ? children[nchildren - 1] : children[0];
            return child.getEndOffset();
        }

        public Element getElement(int index) {
            if (index < nchildren) {
                return children[index];
            }
            return null;
        }

        public int getElementCount() {
            return nchildren;
        }

        /**
         * The index of the child that contains that position, by binary search.
         *
         * <p>It keeps the last result and tries it first: the queries come almost always in order,
         * walking the text, and that way most of them make no extra comparison.
         */
        public int getElementIndex(int offset) {
            int index;
            int lower = 0;
            int upper = nchildren - 1;
            int mid = 0;
            int p0 = getStartOffset();
            int p1;

            if (nchildren == 0) {
                return 0;
            }
            if (offset >= getEndOffset()) {
                return nchildren - 1;
            }

            if (lastIndex >= lower && lastIndex <= upper) {
                Element lastHit = children[lastIndex];
                p0 = lastHit.getStartOffset();
                p1 = lastHit.getEndOffset();
                if ((offset >= p0) && (offset < p1)) {
                    return lastIndex;
                }
                if (offset < p0) {
                    upper = lastIndex;
                } else {
                    lower = lastIndex;
                }
            }

            while (lower <= upper) {
                mid = lower + ((upper - lower) / 2);
                Element elem = children[mid];
                p0 = elem.getStartOffset();
                p1 = elem.getEndOffset();
                if ((offset >= p0) && (offset < p1)) {
                    lastIndex = mid;
                    return mid;
                } else if (offset < p0) {
                    upper = mid - 1;
                } else {
                    lower = mid + 1;
                }
            }

            // With no exact match: the nearest one, which is what whoever looks for a gap wants.
            if (offset < p0) {
                index = mid;
            } else {
                index = mid + 1;
            }
            lastIndex = index;
            return index;
        }

        public boolean isLeaf() {
            return false;
        }

        public boolean getAllowsChildren() {
            return true;
        }

        public Enumeration<TreeNode> children() {
            if (nchildren == 0) {
                return null;
            }
            Vector<TreeNode> tempVector = new Vector<TreeNode>(nchildren);
            for (int counter = 0; counter < nchildren; counter++) {
                tempVector.addElement(children[counter]);
            }
            return tempVector.elements();
        }
    }

    /** An element with no children: a stretch of text marked with two {@link Position}s. */
    public static class LeafElement extends AbstractElement {

        private transient Position p0;
        private transient Position p1;

        public LeafElement(AbstractDocument document, Element parent, AttributeSet a,
                int offs0, int offs1) {
            super(document, parent, a);
            try {
                p0 = document.createPosition(offs0);
                p1 = document.createPosition(offs1);
            } catch (BadLocationException e) {
                p0 = null;
                p1 = null;
                throw new StateInvariantError("Can't create Position references");
            }
        }

        public String toString() {
            return "LeafElement(" + getName() + ") " + p0 + "," + p1 + "\n";
        }

        public int getStartOffset() {
            return p0.getOffset();
        }

        public int getEndOffset() {
            return p1.getOffset();
        }

        public String getName() {
            String nm = super.getName();
            if (nm == null) {
                nm = ContentElementName;
            }
            return nm;
        }

        /** Zero: a leaf element has no children to search among. */
        public int getElementIndex(int pos) {
            return -1;
        }

        public Element getElement(int index) {
            return null;
        }

        public int getElementCount() {
            return 0;
        }

        public boolean isLeaf() {
            return true;
        }

        public boolean getAllowsChildren() {
            return false;
        }

        public Enumeration<TreeNode> children() {
            return null;
        }
    }

    /**
     * The change the document underwent, and at the same time the edit that undoes it.
     *
     * <p>That they are the same object is what makes undoing cheap: the event already has inside
     * every little piece of the edit --the text and the structure changes--, in order.
     */
    public static class DefaultDocumentEvent extends CompoundEdit implements DocumentEvent {

        /** The document it belongs to; see the note about the nested classes. */
        protected final AbstractDocument document;

        private int offset;
        private int length;
        private Hashtable<Element, DocumentEvent$ElementChange> changeLookup;
        private DocumentEvent$EventType type;

        public DefaultDocumentEvent(AbstractDocument document, int offs, int len,
                DocumentEvent$EventType type) {
            super();
            this.document = document;
            offset = offs;
            length = len;
            this.type = type;
        }

        public String toString() {
            return edits.toString();
        }

        /** A structure change is indexed by element, so that {@link #getChange} is cheap. */
        public boolean addEdit(UndoableEdit anEdit) {
            if ((changeLookup == null) && (anEdit instanceof DocumentEvent$ElementChange)) {
                changeLookup = new Hashtable<Element, DocumentEvent$ElementChange>();
            }
            if (changeLookup != null && (anEdit instanceof DocumentEvent$ElementChange)) {
                DocumentEvent$ElementChange ec = (DocumentEvent$ElementChange) anEdit;
                changeLookup.put(ec.getElement(), ec);
            }
            return super.addEdit(anEdit);
        }

        public void redo() throws CannotRedoException {
            document.writeLock();
            try {
                super.redo();
                if (type == DocumentEvent$EventType.INSERT) {
                    document.fireInsertUpdate(this);
                } else if (type == DocumentEvent$EventType.REMOVE) {
                    document.fireRemoveUpdate(this);
                } else {
                    document.fireChangedUpdate(this);
                }
            } finally {
                document.writeUnlock();
            }
        }

        /** Undoing an addition is a removal: the event reported is the opposite one. */
        public void undo() throws CannotUndoException {
            document.writeLock();
            try {
                super.undo();
                if (type == DocumentEvent$EventType.REMOVE) {
                    document.fireInsertUpdate(this);
                } else if (type == DocumentEvent$EventType.INSERT) {
                    document.fireRemoveUpdate(this);
                } else {
                    document.fireChangedUpdate(this);
                }
            } finally {
                document.writeUnlock();
            }
        }

        public boolean isSignificant() {
            return true;
        }

        public String getPresentationName() {
            DocumentEvent$EventType type = getType();
            if (type == DocumentEvent$EventType.INSERT) {
                return "addition";
            }
            if (type == DocumentEvent$EventType.REMOVE) {
                return "deletion";
            }
            return "style change";
        }

        public String getUndoPresentationName() {
            return "Undo " + getPresentationName();
        }

        public String getRedoPresentationName() {
            return "Redo " + getPresentationName();
        }

        public DocumentEvent$EventType getType() {
            return type;
        }

        public int getOffset() {
            return offset;
        }

        public int getLength() {
            return length;
        }

        public Document getDocument() {
            return document;
        }

        public DocumentEvent$ElementChange getChange(Element elem) {
            if (changeLookup != null) {
                return changeLookup.get(elem);
            }
            return null;
        }
    }
}
