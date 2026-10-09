package javax.swing.text.html;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * A document that keeps HTML.
 *
 * <h2>The tree is not the HTML</h2>
 *
 * <p>A text document is paragraphs with stretches of characters inside. HTML is tags nested to
 * any depth. This document brings the two forms closer: it uses {@link BlockElement} for the
 * tags that make a block and {@link RunElement} for the stretches, and keeps in each element the
 * tag that generated it.
 *
 * <p>What it does <em>not</em> do is keep the character tags as elements. A bold is not an
 * element: it is an attribute of the stretch, with the {@code HTML.Tag.B} tag as the key. That is
 * why the tree is much flatter than the HTML, and that is why {@link HTMLWriter} has to rebuild
 * the nesting when writing.
 *
 * <h2>How it is filled</h2>
 *
 * <p>It is not filled by writing text: it is filled with an {@link HTMLReader}, which is what
 * {@link #getReader} returns and what the parser feeds. The reader gathers everything in a list
 * of specifications and only at the end builds the tree in one go, which is far cheaper than
 * inserting as it goes.
 *
 * <h2>Editing the HTML directly</h2>
 *
 * <p>The six methods {@code setInnerHTML}, {@code setOuterHTML}, {@code insertAfterStart},
 * {@code insertBeforeEnd}, {@code insertBeforeStart} and {@code insertAfterEnd} allow changing
 * the document by talking in HTML instead of in elements. They are named after what they do
 * relative to an element, and that is the whole difference between them.
 */
public class HTMLDocument extends DefaultStyledDocument {

    /** The key under which the comments left outside the body are kept. */
    public static final String AdditionalComments = "AdditionalComments";

    private URL base;
    private boolean preservesUnknownTags = true;
    private int tokenThreshold = Integer.MAX_VALUE;
    private HTMLEditorKit.Parser parser;
    private Hashtable<String, Element> map;

    /** An empty document with a style sheet of its own. */
    public HTMLDocument() {
        this(new javax.swing.text.GapContent(BUFFER_SIZE_DEFAULT), new StyleSheet());
    }

    /** An empty document with that style sheet. */
    public HTMLDocument(StyleSheet styles) {
        this(new javax.swing.text.GapContent(BUFFER_SIZE_DEFAULT), styles);
    }

    /** A document over that content and that style sheet. */
    public HTMLDocument(AbstractDocument.Content c, StyleSheet styles) {
        super(c, styles);
    }

    /** A reader that puts whatever comes in starting at that position. */
    public HTMLEditorKit.ParserCallback getReader(int pos) {
        Object desc = getProperty(Document.StreamDescriptionProperty);
        if (desc instanceof URL) {
            setBase((URL) desc);
        }
        return new HTMLReader(this, pos);
    }

    /**
     * A reader for inserting inside a tag.
     *
     * <p>The two numbers say how many elements to close before and how many to open afterwards; see
     * {@link HTMLEditorKit#insertHTML}.
     */
    public HTMLEditorKit.ParserCallback getReader(int pos, int popDepth, int pushDepth,
            HTML.Tag insertTag) {
        return new HTMLReader(this, pos, popDepth, pushDepth, insertTag);
    }

    /** The address the document's relative ones are resolved against. */
    public URL getBase() {
        return base;
    }

    public void setBase(URL u) {
        base = u;
        getStyleSheet().setBase(u);
    }

    protected void insert(int offset, DefaultStyledDocument.ElementSpec[] data)
            throws BadLocationException {
        super.insert(offset, data);
    }

    protected void insertUpdate(AbstractDocument.DefaultDocumentEvent chng, AttributeSet attr) {
        if (attr == null) {
            attr = contentAttributeSet;
        }
        super.insertUpdate(chng, attr);
    }

    private static final AttributeSet contentAttributeSet = createContentAttributes();

    private static AttributeSet createContentAttributes() {
        SimpleAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        return a;
    }

    protected void create(DefaultStyledDocument.ElementSpec[] data) {
        super.create(data);
    }

    public void setParagraphAttributes(int offset, int length, AttributeSet s, boolean replace) {
        super.setParagraphAttributes(offset, length, s, replace);
    }

    /** The style sheet: it is the document's attribute context. */
    public StyleSheet getStyleSheet() {
        return (StyleSheet) getAttributeContext();
    }

    /**
     * It walks every element with that tag.
     *
     * <p>It is the way of finding, for instance, all of a page's links without going down the tree
     * by hand.
     */
    public Iterator getIterator(HTML.Tag t) {
        if (t.isBlock()) {
            return new BlockIterator(this, t);
        }
        return new LeafIterator(this, t);
    }

    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
        return new RunElement(this, parent, a, p0, p1);
    }

    protected Element createBranchElement(Element parent, AttributeSet a) {
        return new BlockElement(this, parent, a);
    }

    protected AbstractDocument.AbstractElement createDefaultRoot() {
        writeLock();
        MutableAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.HTML);
        BlockElement root = new BlockElement(this, null, a.copyAttributes());
        a.removeAttributes(a);

        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.BODY);
        BlockElement body = new BlockElement(this, root, a.copyAttributes());
        a.removeAttributes(a);

        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.P);
        BlockElement paragraph = new BlockElement(this, body, a.copyAttributes());
        a.removeAttributes(a);

        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        RunElement content = new RunElement(this, paragraph, a, 0, 1);
        Element[] children = new Element[1];
        children[0] = content;
        paragraph.replace(0, 0, children);
        children[0] = paragraph;
        body.replace(0, 0, children);
        children[0] = body;
        root.replace(0, 0, children);
        writeUnlock();
        return root;
    }

    /**
     * How many elements are gathered before touching the document.
     *
     * <p>With a small number the document is seen assembling itself bit by bit; with a large one it
     * appears at once and takes less time in total. It is the trade-off between seeming fast and
     * being fast.
     */
    public void setTokenThreshold(int n) {
        putProperty(TokenThreshold, Integer.valueOf(n));
        tokenThreshold = n;
    }

    public int getTokenThreshold() {
        return tokenThreshold;
    }

    static final String TokenThreshold = "token threshold";

    /**
     * Whether the tags that are not known are kept all the same.
     *
     * <p>Keeping them allows writing the document back just as it came in, even if they cannot be
     * drawn. It is what is wanted almost always: throwing away what is not understood loses the
     * user's data.
     */
    public void setPreservesUnknownTags(boolean preservesTags) {
        preservesUnknownTags = preservesTags;
    }

    public boolean getPreservesUnknownTags() {
        return preservesUnknownTags;
    }

    /** It loads the link's document in whatever frame the event indicates. */
    public void processHTMLFrameHyperlinkEvent(HTMLFrameHyperlinkEvent e) {
    }

    /** The parser the methods that insert HTML use. */
    public void setParser(HTMLEditorKit.Parser parser) {
        this.parser = parser;
        putProperty("__PARSER__", null);
    }

    public HTMLEditorKit.Parser getParser() {
        Object p = getProperty("__PARSER__");
        if (p instanceof HTMLEditorKit.Parser) {
            return (HTMLEditorKit.Parser) p;
        }
        return parser;
    }

    /**
     * It replaces an element's content by that HTML.
     *
     * @throws IllegalStateException if there is no parser.
     */
    public void setInnerHTML(Element elem, String htmlText) throws BadLocationException,
            IOException {
        checkParser();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        if (elem.isLeaf()) {
            throw new IllegalArgumentException(
                    "Can not set inner HTML of a leaf");
        }
        int start = elem.getStartOffset();
        int end = elem.getEndOffset();
        insert(start, htmlText, 0, 0, null);
        removeRange(start + insertedLength, end - start);
    }

    /** It replaces the whole element, including its tags. */
    public void setOuterHTML(Element elem, String htmlText) throws BadLocationException,
            IOException {
        checkParser();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        int start = elem.getStartOffset();
        int end = elem.getEndOffset();
        insert(start, htmlText, 0, 0, null);
        removeRange(start + insertedLength, end - start);
    }

    /** It inserts just after the opening tag. */
    public void insertAfterStart(Element elem, String htmlText) throws BadLocationException,
            IOException {
        checkParser();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        if (elem.isLeaf()) {
            throw new IllegalArgumentException(
                    "Can not insert HTML after start of a leaf");
        }
        insert(elem.getStartOffset(), htmlText, 0, 0, null);
    }

    /** It inserts just before the closing tag. */
    public void insertBeforeEnd(Element elem, String htmlText) throws BadLocationException,
            IOException {
        checkParser();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        if (elem.isLeaf()) {
            throw new IllegalArgumentException(
                    "Can not set inner HTML before end of leaf");
        }
        int end = elem.getEndOffset();
        if (end > getLength()) {
            end = getLength();
        }
        insert(end, htmlText, 0, 0, null);
    }

    /** It inserts before the opening tag, that is outside the element. */
    public void insertBeforeStart(Element elem, String htmlText) throws BadLocationException,
            IOException {
        checkParser();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        insert(elem.getStartOffset(), htmlText, 0, 0, null);
    }

    /** It inserts after the closing tag. */
    public void insertAfterEnd(Element elem, String htmlText) throws BadLocationException,
            IOException {
        checkParser();
        if (elem == null || htmlText == null) {
            throw new IllegalArgumentException("null parameter");
        }
        int end = elem.getEndOffset();
        if (end > getLength()) {
            end = getLength();
        }
        insert(end, htmlText, 0, 0, null);
    }

    private int insertedLength;

    private void checkParser() {
        if (getParser() == null) {
            throw new IllegalStateException("No HTMLEditorKit.Parser");
        }
    }

    private void insert(int offset, String html, int popDepth, int pushDepth, HTML.Tag insertTag)
            throws BadLocationException, IOException {
        int before = getLength();
        HTMLEditorKit.ParserCallback r = getReader(offset, popDepth, pushDepth, insertTag);
        getParser().parse(new java.io.StringReader(html), r, true);
        r.flush();
        insertedLength = getLength() - before;
    }

    private void removeRange(int offset, int length) throws BadLocationException {
        if (length > 0 && offset < getLength()) {
            remove(offset, Math.min(length, getLength() - offset));
        }
    }

    /**
     * The element whose <code>id</code> attribute is that one.
     *
     * <p>It is what makes a link to <code>#section</code> able to find its target.
     */
    public Element getElement(String id) {
        if (id == null) {
            return null;
        }
        return getElement(getDefaultRootElement(), HTML.Attribute.ID, id, true);
    }

    /** The first element below that one whose attribute has that value. */
    public Element getElement(Element e, Object attribute, Object value) {
        return getElement(e, attribute, value, true);
    }

    private Element getElement(Element e, Object attribute, Object value, boolean searchLeafs) {
        AttributeSet attr = e.getAttributes();
        if (attr != null && attr.isDefined(attribute)) {
            if (value.equals(attr.getAttribute(attribute))) {
                return e;
            }
        }
        if (!e.isLeaf()) {
            int maxCounter = e.getElementCount();
            for (int counter = 0; counter < maxCounter; counter++) {
                Element child = e.getElement(counter);
                if (searchLeafs || !child.isLeaf()) {
                    Element retValue = getElement(child, attribute, value, searchLeafs);
                    if (retValue != null) {
                        return retValue;
                    }
                }
            }
        }
        return null;
    }

    protected void fireChangedUpdate(DocumentEvent e) {
        super.fireChangedUpdate(e);
    }

    protected void fireUndoableEditUpdate(UndoableEditEvent e) {
        super.fireUndoableEditUpdate(e);
    }

    /**
     * It walks the elements that have a tag.
     *
     * <p>It is abstract and not an interface because it has to be able to grow without breaking
     * whoever uses it; see {@link HTMLDocument#getIterator}.
     */
    public abstract static class Iterator {

        protected Iterator() {
        }

        /** The current element's attributes. */
        public abstract AttributeSet getAttributes();

        public abstract int getStartOffset();

        public abstract int getEndOffset();

        /** It advances to the next one. */
        public abstract void next();

        /** Whether there is still an element. */
        public abstract boolean isValid();

        public abstract HTML.Tag getTag();
    }

    /**
     * It walks the character tags, which are not elements but attributes.
     *
     * <p>A bold or a link lives as an attribute of a stretch; see the note of the class that
     * contains it. That is why this walk looks at each leaf's attributes and not at the elements.
     */
    static class LeafIterator extends Iterator {

        private final HTML.Tag tag;
        private final javax.swing.text.ElementIterator pos;
        private AttributeSet current;
        private int start;
        private int end;

        LeafIterator(HTMLDocument doc, HTML.Tag t) {
            tag = t;
            pos = new javax.swing.text.ElementIterator(doc);
            next();
        }

        public AttributeSet getAttributes() {
            return current;
        }

        public int getStartOffset() {
            return start;
        }

        public int getEndOffset() {
            return end;
        }

        public HTML.Tag getTag() {
            return tag;
        }

        public boolean isValid() {
            return current != null;
        }

        /**
         * It looks for the next leaf with the tag and joins the adjacent ones.
         *
         * <p>Joining them matters: a link split into two stretches because one has a bold inside is
         * still a single link, and whoever walks the links expects to see it once.
         */
        public void next() {
            current = null;
            Element e;
            while ((e = pos.next()) != null) {
                if (!e.isLeaf()) {
                    continue;
                }
                AttributeSet a = e.getAttributes();
                Object v = a.getAttribute(tag);
                if (v instanceof AttributeSet) {
                    current = (AttributeSet) v;
                    start = e.getStartOffset();
                    end = e.getEndOffset();
                    // It is stretched while the attribute is the same object.
                    Element sig;
                    while ((sig = pos.next()) != null) {
                        if (sig.isLeaf() && sig.getAttributes().getAttribute(tag) == v) {
                            end = sig.getEndOffset();
                        } else {
                            pos.previous();
                            break;
                        }
                    }
                    return;
                }
            }
        }
    }

    /** It walks the elements that make a block and have that tag. */
    static class BlockIterator extends Iterator {

        private final HTML.Tag tag;
        private final javax.swing.text.ElementIterator pos;
        private Element current;

        BlockIterator(HTMLDocument doc, HTML.Tag t) {
            tag = t;
            pos = new javax.swing.text.ElementIterator(doc);
            next();
        }

        public AttributeSet getAttributes() {
            return (current == null) ? null : current.getAttributes();
        }

        public int getStartOffset() {
            return (current == null) ? -1 : current.getStartOffset();
        }

        public int getEndOffset() {
            return (current == null) ? -1 : current.getEndOffset();
        }

        public HTML.Tag getTag() {
            return tag;
        }

        public boolean isValid() {
            return current != null;
        }

        public void next() {
            current = null;
            Element e;
            while ((e = pos.next()) != null) {
                if (e.getAttributes().getAttribute(StyleConstants.NameAttribute) == tag) {
                    current = e;
                    return;
                }
            }
        }
    }

    /**
     * An element with children that knows which tag it came from.
     *
     * <p>In the JDK it is an inner class; here it is static and takes the document, which is the
     * same signature in the compiled file. See {@link javax.swing.text.TableView.TableRow}'s note.
     */
    public static class BlockElement extends AbstractDocument.BranchElement {

        /** A block of that document, hung from that parent. */
        public BlockElement(HTMLDocument document, Element parent, AttributeSet a) {
            super(document, parent, a);
        }

        /** The name is the tag's, not the element type's. */
        public String getName() {
            Object o = getAttribute(StyleConstants.NameAttribute);
            if (o != null) {
                return o.toString();
            }
            return super.getName();
        }

        /**
         * Where it inherits the attributes it does not have from.
         *
         * <p>From the style sheet, not from the element above. It is the difference between HTML
         * and an ordinary document: who decides how a paragraph looks is the CSS rule that
         * corresponds to it, and that rule depends on the whole path from the root.
         */
        public AttributeSet getResolveParent() {
            return null;
        }
    }

    /** A stretch of text that knows which tag it came from. */
    public static class RunElement extends AbstractDocument.LeafElement {

        /** A stretch of that document, hung from that parent. */
        public RunElement(HTMLDocument document, Element parent, AttributeSet a, int offs0,
                int offs1) {
            super(document, parent, a, offs0, offs1);
        }

        public String getName() {
            Object o = getAttribute(StyleConstants.NameAttribute);
            if (o != null) {
                return o.toString();
            }
            return super.getName();
        }

        /** The same as in {@link BlockElement}. */
        public AttributeSet getResolveParent() {
            return null;
        }
    }

    /**
     * It turns what the parser finds into document elements.
     *
     * <h2>Gather and then build</h2>
     *
     * <p>It does not insert as it reads: it keeps specifications in {@link #parseBuffer} and builds
     * the tree when it has gathered enough or when it finishes. Inserting one tag at a time would
     * force rebuilding the tree at every step.
     *
     * <h2>One action per tag</h2>
     *
     * <p>Each tag has a {@link TagAction} registered. It is a table and not a chain of comparisons:
     * adding a tag is registering an action, and whoever inherits can change the treatment of a
     * single one without touching the rest.
     */
    public static class HTMLReader extends HTMLEditorKit.ParserCallback {

        /** What has been gathered so far. */
        protected Vector<DefaultStyledDocument.ElementSpec> parseBuffer =
                new Vector<DefaultStyledDocument.ElementSpec>();

        /** The character attributes that hold now. */
        protected MutableAttributeSet charAttr = new SimpleAttributeSet();

        final HTMLDocument document;
        private final int offset;
        private int popDepth;
        private int pushDepth;
        private HTML.Tag insertTag;
        private Hashtable<HTML.Tag, TagAction> tagMap = new Hashtable<HTML.Tag, TagAction>();
        private Stack<AttributeSet> charAttrStack = new Stack<AttributeSet>();
        private MutableAttributeSet blockAttributes = new SimpleAttributeSet();
        private boolean insertedSomething = false;

        /** A reader that puts things in starting at that position. */
        public HTMLReader(HTMLDocument document, int offset) {
            this(document, offset, 0, 0, null);
        }

        /** A reader for inserting inside a tag. */
        public HTMLReader(HTMLDocument document, int offset, int popDepth, int pushDepth,
                HTML.Tag insertTag) {
            this.document = document;
            this.offset = offset;
            this.popDepth = popDepth;
            this.pushDepth = pushDepth;
            this.insertTag = insertTag;
            registerActions();
        }

        private void registerActions() {
            TagAction block = new BlockAction(this);
            TagAction paragraph = new ParagraphAction(this);
            TagAction character = new CharacterAction(this);
            TagAction especial = new SpecialAction(this);
            TagAction hidden = new HiddenAction(this);
            TagAction pre = new PreAction(this);
            TagAction form = new FormAction(this);

            registerTag(HTML.Tag.HTML, block);
            registerTag(HTML.Tag.BODY, block);
            registerTag(HTML.Tag.DIV, block);
            registerTag(HTML.Tag.CENTER, block);
            registerTag(HTML.Tag.BLOCKQUOTE, block);
            registerTag(HTML.Tag.UL, block);
            registerTag(HTML.Tag.OL, block);
            registerTag(HTML.Tag.DIR, block);
            registerTag(HTML.Tag.MENU, block);
            registerTag(HTML.Tag.LI, block);
            registerTag(HTML.Tag.DL, block);
            registerTag(HTML.Tag.DD, block);
            registerTag(HTML.Tag.TABLE, block);
            registerTag(HTML.Tag.TR, block);
            registerTag(HTML.Tag.TD, block);
            registerTag(HTML.Tag.TH, block);
            registerTag(HTML.Tag.CAPTION, block);
            registerTag(HTML.Tag.NOFRAMES, block);

            registerTag(HTML.Tag.P, paragraph);
            registerTag(HTML.Tag.IMPLIED, paragraph);
            registerTag(HTML.Tag.DT, paragraph);
            registerTag(HTML.Tag.H1, paragraph);
            registerTag(HTML.Tag.H2, paragraph);
            registerTag(HTML.Tag.H3, paragraph);
            registerTag(HTML.Tag.H4, paragraph);
            registerTag(HTML.Tag.H5, paragraph);
            registerTag(HTML.Tag.H6, paragraph);

            registerTag(HTML.Tag.PRE, pre);

            registerTag(HTML.Tag.B, character);
            registerTag(HTML.Tag.I, character);
            registerTag(HTML.Tag.U, character);
            registerTag(HTML.Tag.S, character);
            registerTag(HTML.Tag.STRIKE, character);
            registerTag(HTML.Tag.TT, character);
            registerTag(HTML.Tag.CODE, character);
            registerTag(HTML.Tag.KBD, character);
            registerTag(HTML.Tag.SAMP, character);
            registerTag(HTML.Tag.VAR, character);
            registerTag(HTML.Tag.CITE, character);
            registerTag(HTML.Tag.DFN, character);
            registerTag(HTML.Tag.EM, character);
            registerTag(HTML.Tag.STRONG, character);
            registerTag(HTML.Tag.BIG, character);
            registerTag(HTML.Tag.SMALL, character);
            registerTag(HTML.Tag.SUB, character);
            registerTag(HTML.Tag.SUP, character);
            registerTag(HTML.Tag.FONT, character);
            registerTag(HTML.Tag.SPAN, character);
            registerTag(HTML.Tag.A, character);
            registerTag(HTML.Tag.ADDRESS, character);

            registerTag(HTML.Tag.IMG, especial);
            registerTag(HTML.Tag.BR, especial);
            registerTag(HTML.Tag.HR, especial);
            registerTag(HTML.Tag.OBJECT, especial);
            registerTag(HTML.Tag.APPLET, especial);
            registerTag(HTML.Tag.PARAM, especial);

            registerTag(HTML.Tag.INPUT, form);
            registerTag(HTML.Tag.SELECT, form);
            registerTag(HTML.Tag.OPTION, form);
            registerTag(HTML.Tag.TEXTAREA, form);
            registerTag(HTML.Tag.FORM, form);

            registerTag(HTML.Tag.HEAD, hidden);
            registerTag(HTML.Tag.TITLE, hidden);
            registerTag(HTML.Tag.META, hidden);
            registerTag(HTML.Tag.LINK, hidden);
            registerTag(HTML.Tag.STYLE, hidden);
            registerTag(HTML.Tag.SCRIPT, hidden);
            registerTag(HTML.Tag.AREA, hidden);
            registerTag(HTML.Tag.MAP, hidden);
            registerTag(HTML.Tag.BASE, hidden);
            registerTag(HTML.Tag.BASEFONT, hidden);
            registerTag(HTML.Tag.ISINDEX, new IsindexAction(this));
            registerTag(HTML.Tag.FRAMESET, hidden);
            registerTag(HTML.Tag.FRAME, hidden);
        }

        /** It flushes whatever was gathered to the document. */
        public void flush() throws BadLocationException {
            if (parseBuffer.size() == 0) {
                return;
            }
            DefaultStyledDocument.ElementSpec[] spec =
                    new DefaultStyledDocument.ElementSpec[parseBuffer.size()];
            parseBuffer.copyInto(spec);
            parseBuffer.removeAllElements();
            if (document.getLength() == 0 && offset == 0 && !insertedSomething) {
                document.create(spec);
            } else {
                document.insert(offset, spec);
            }
            insertedSomething = true;
        }

        public void handleText(char[] data, int pos) {
            if (data.length == 0) {
                return;
            }
            addContent(data, 0, data.length);
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            TagAction action = tagMap.get(t);
            if (action != null) {
                action.start(t, a);
            } else if (document.getPreservesUnknownTags()) {
                // A tag that is not known is kept as a block; see
                                // HTMLDocument.setPreservesUnknownTags.
                blockOpen(t, a);
            }
        }

        public void handleComment(char[] data, int pos) {
            SimpleAttributeSet a = new SimpleAttributeSet();
            a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.COMMENT);
            a.addAttribute(HTML.Attribute.COMMENT, new String(data));
            addSpecialElement(HTML.Tag.COMMENT, a);
        }

        public void handleEndTag(HTML.Tag t, int pos) {
            TagAction action = tagMap.get(t);
            if (action != null) {
                action.end(t);
            } else if (document.getPreservesUnknownTags()) {
                blockClose(t);
            }
        }

        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            TagAction action = tagMap.get(t);
            if (action != null) {
                action.start(t, a);
                action.end(t);
            } else if (document.getPreservesUnknownTags()) {
                addSpecialElement(t, a);
            }
        }

        public void handleEndOfLineString(String eol) {
            if (eol != null) {
                document.putProperty(javax.swing.text.DefaultEditorKit.EndOfLineStringProperty,
                        eol);
            }
        }

        /** It associates an action with a tag; see the class note. */
        protected void registerTag(HTML.Tag t, TagAction a) {
            tagMap.put(t, a);
        }

        /**
         * It keeps the character attributes that hold now.
         *
         * <p>They go on a stack because the character tags nest: on closing one, exactly what there
         * was before opening it has to come back, not nothing.
         */
        protected void pushCharacterStyle() {
            charAttrStack.push(charAttr.copyAttributes());
        }

        protected void popCharacterStyle() {
            if (!charAttrStack.isEmpty()) {
                charAttr = new SimpleAttributeSet(charAttrStack.pop());
            }
        }

        /** A {@code <textarea>}'s content: it goes to the control's model, not to the document. */
        protected void textAreaContent(char[] data) {
        }

        /** Text inside a {@code <pre>}: the line breaks count. */
        protected void preContent(char[] data) {
            int start = 0;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == '\n') {
                    addContent(data, start, i - start + 1);
                    blockClose(HTML.Tag.IMPLIED);
                    blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
                    start = i + 1;
                }
            }
            if (start < data.length) {
                addContent(data, start, data.length - start);
            }
        }

        /** It opens a block. */
        protected void blockOpen(HTML.Tag t, MutableAttributeSet attr) {
            attr.addAttribute(StyleConstants.NameAttribute, t);
            parseBuffer.addElement(new DefaultStyledDocument.ElementSpec(
                    attr.copyAttributes(), DefaultStyledDocument.ElementSpec.StartTagType));
        }

        protected void blockClose(HTML.Tag t) {
            parseBuffer.addElement(new DefaultStyledDocument.ElementSpec(
                    null, DefaultStyledDocument.ElementSpec.EndTagType));
        }

        protected void addContent(char[] data, int offs, int length) {
            addContent(data, offs, length, true);
        }

        /**
         * It adds text with the character attributes that hold now.
         *
         * <p>If there is no block open an implicit paragraph is opened: loose text has to live
         * inside a paragraph, because the document does not admit text hanging from the root.
         */
        protected void addContent(char[] data, int offs, int length, boolean generateImpliedPIfNecessary) {
            MutableAttributeSet a = new SimpleAttributeSet(charAttr);
            a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
            DefaultStyledDocument.ElementSpec spec = new DefaultStyledDocument.ElementSpec(
                    a.copyAttributes(), DefaultStyledDocument.ElementSpec.ContentType, data, offs,
                    length);
            parseBuffer.addElement(spec);
            if (parseBuffer.size() > document.getTokenThreshold()) {
                try {
                    flush();
                } catch (BadLocationException ble) {
                    // The document changed underneath: it goes on gathering.
                }
            }
        }

        /**
         * It adds an element that takes up room but has no text.
         *
         * <p>An image, a line break, a comment. They are kept as an invisible character with
         * attributes: the document counts in characters and something that takes up a place has to
         * take up one.
         */
        protected void addSpecialElement(HTML.Tag t, MutableAttributeSet a) {
            if (t != HTML.Tag.FRAME && !insertedSomething) {
                // Now it can.
                insertedSomething = false;
            }
            a.addAttribute(StyleConstants.NameAttribute, t);
            char[] one = {' '};
            parseBuffer.addElement(new DefaultStyledDocument.ElementSpec(a.copyAttributes(),
                    DefaultStyledDocument.ElementSpec.ContentType, one, 0, 1));
        }

        /**
         * What to do with a tag.
         *
         * <p>The base one does nothing. It serves for the tags that are to be ignored without the
         * reader having to ask whether there is an action registered.
         */
        public static class TagAction {

            final HTMLReader reader;

            /** An action for that reader. */
            public TagAction(HTMLReader reader) {
                this.reader = reader;
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
            }

            public void end(HTML.Tag t) {
            }
        }

        /** A tag that makes a block. */
        public static class BlockAction extends TagAction {

            public BlockAction(HTMLReader reader) {
                super(reader);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                reader.blockOpen(t, a);
            }

            public void end(HTML.Tag t) {
                reader.blockClose(t);
            }
        }

        /**
         * A character tag: it makes no element, it changes the text's attributes.
         *
         * <p>See {@link HTMLDocument}'s note: it is what makes the tree much flatter than the HTML.
         */
        public static class CharacterAction extends TagAction {

            public CharacterAction(HTMLReader reader) {
                super(reader);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                reader.pushCharacterStyle();
                a.addAttribute(t, a.copyAttributes());
                reader.charAttr.addAttributes(a);
            }

            public void end(HTML.Tag t) {
                reader.popCharacterStyle();
            }
        }

        /** A paragraph: a block that also closes the implicit one it may have opened. */
        public static class ParagraphAction extends BlockAction {

            public ParagraphAction(HTMLReader reader) {
                super(reader);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                super.start(t, a);
            }

            public void end(HTML.Tag t) {
                super.end(t);
            }
        }

        /** A {@code <pre>}: like a block, but the text inside respects the line breaks. */
        public static class PreAction extends BlockAction {

            public PreAction(HTMLReader reader) {
                super(reader);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                a.addAttribute(CSS.Attribute.WHITE_SPACE, "pre");
                super.start(t, a);
                reader.blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
            }

            public void end(HTML.Tag t) {
                reader.blockClose(HTML.Tag.IMPLIED);
                super.end(t);
            }
        }

        /** A tag that takes up a place with no text: an image, a break, a rule. */
        public static class SpecialAction extends TagAction {

            public SpecialAction(HTMLReader reader) {
                super(reader);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                reader.addSpecialElement(t, a);
            }
        }

        /** A form control. */
        public static class FormAction extends SpecialAction {

            public FormAction(HTMLReader reader) {
                super(reader);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                if (t == HTML.Tag.FORM) {
                    reader.blockOpen(t, a);
                    return;
                }
                super.start(t, a);
            }

            public void end(HTML.Tag t) {
                if (t == HTML.Tag.FORM) {
                    reader.blockClose(t);
                }
            }
        }

        /**
         * A tag that is not seen but is kept.
         *
         * <p>The head, a {@code <script>}, a {@code <meta>}. They are kept so as to be able to
         * write the document back the same; see {@link HTMLDocument#setPreservesUnknownTags}.
         */
        public static class HiddenAction extends TagAction {

            public HiddenAction(HTMLReader reader) {
                super(reader);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                reader.addSpecialElement(t, a);
            }

            public void end(HTML.Tag t) {
            }
        }

        /** An {@code <isindex>}: it is shown as a search field. */
        public static class IsindexAction extends TagAction {

            public IsindexAction(HTMLReader reader) {
                super(reader);
            }

            public void start(HTML.Tag t, MutableAttributeSet a) {
                reader.blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
                reader.addSpecialElement(t, a);
                reader.blockClose(HTML.Tag.IMPLIED);
            }
        }
    }
}
