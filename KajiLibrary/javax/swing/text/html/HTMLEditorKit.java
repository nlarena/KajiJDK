package javax.swing.text.html;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * The editor kit for HTML.
 *
 * <h2>What it joins</h2>
 *
 * <p>A parser that turns HTML text into an {@link HTMLDocument}, a view factory that knows how
 * to draw each tag, a style sheet with the default rules, and the actions that edit.
 *
 * <h2>The parser does not come built in</h2>
 *
 * <p>{@link #getParser} looks for it in {@code javax.swing.text.html.parser.ParserDelegator}, by
 * reflection and not by a direct reference. It is on purpose: the editor kit does not depend on
 * a concrete parser existing, and whoever wants another only has to give it an
 * {@link HTMLDocument#setParser}.
 *
 * <h2>The style sheet is shared</h2>
 *
 * <p>Every editor kit that does not ask for its own uses the same one. It is what allows a rule
 * set once to hold for every pane in the program; and it is also why {@link #setStyleSheet}
 * should be used with care, because it changes everybody's.
 */
public class HTMLEditorKit extends StyledEditorKit implements Accessible {

    /** The name of the resource with the default style sheet. */
    public static final String DEFAULT_CSS = "default.css";

    public static final String BOLD_ACTION = "html-bold-action";
    public static final String ITALIC_ACTION = "html-italic-action";
    public static final String PARA_INDENT_LEFT = "html-para-indent-left";
    public static final String PARA_INDENT_RIGHT = "html-para-indent-right";
    public static final String FONT_CHANGE_BIGGER = "html-font-bigger";
    public static final String FONT_CHANGE_SMALLER = "html-font-smaller";
    public static final String COLOR_ACTION = "html-color-action";
    public static final String LOGICAL_STYLE_ACTION = "html-logical-style-action";
    public static final String IMG_ALIGN_TOP = "html-image-align-top";
    public static final String IMG_ALIGN_MIDDLE = "html-image-align-middle";
    public static final String IMG_ALIGN_BOTTOM = "html-image-align-bottom";
    public static final String IMG_BORDER = "html-image-border";

    private static final Cursor MoveCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor DefaultCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);

    private static final ViewFactory defaultFactory = new HTMLFactory();
    private static StyleSheet defaultStyles = null;

    private Cursor defaultCursor = DefaultCursor;
    private Cursor linkCursor = MoveCursor;
    private boolean isAutoFormSubmission = true;
    private MutableAttributeSet input;
    private LinkController linkHandler = new LinkController();
    private AccessibleContext accessibleContext;

    /** An editor kit ready to use. */
    public HTMLEditorKit() {
    }

    /** Always {@code text/html}. */
    public String getContentType() {
        return "text/html";
    }

    public ViewFactory getViewFactory() {
        return defaultFactory;
    }

    /** An empty document, already tied to this kit's style sheet. */
    public Document createDefaultDocument() {
        StyleSheet styles = getStyleSheet();
        StyleSheet ss = new StyleSheet();
        ss.addStyleSheet(styles);
        HTMLDocument doc = new HTMLDocument(ss);
        doc.setParser(getParser());
        doc.setAsynchronousLoadPriority(4);
        doc.setTokenThreshold(100);
        return doc;
    }

    /**
     * It reads HTML and puts it into the document at that position.
     *
     * @throws IOException if there is no parser or the reading fails.
     */
    public void read(Reader in, Document doc, int pos) throws IOException, BadLocationException {
        if (doc instanceof HTMLDocument) {
            HTMLDocument hdoc = (HTMLDocument) doc;
            if (pos > doc.getLength()) {
                throw new BadLocationException("Invalid location", pos);
            }
            Parser p = getParser();
            if (p == null) {
                throw new IOException("Can't load parser");
            }
            ParserCallback receiver = hdoc.getReader(pos);
            Boolean ignoreCharset = (Boolean) doc.getProperty("IgnoreCharsetDirective");
            p.parse(in, receiver, (ignoreCharset == null) ? false : ignoreCharset.booleanValue());
            receiver.flush();
        } else {
            super.read(in, doc, pos);
        }
    }

    /**
     * It puts a fragment of HTML inside a tag of the document.
     *
     * <p>The two numbers {@code popDepth} and {@code pushDepth} are what make the fragment fall in
     * the right place of the tree: how many elements have to be closed before and how many opened
     * afterwards. Without them, inserting a <code>&lt;li&gt;</code> would leave it outside its
     * list.
     */
    public void insertHTML(HTMLDocument doc, int offset, String html, int popDepth, int pushDepth,
            HTML.Tag insertTag) throws BadLocationException, IOException {
        Parser p = getParser();
        if (p == null) {
            throw new IOException("Can't load parser");
        }
        if (offset > doc.getLength()) {
            throw new BadLocationException("Invalid location", offset);
        }
        ParserCallback receiver = doc.getReader(offset, popDepth, pushDepth, insertTag);
        Boolean ignoreCharset = (Boolean) doc.getProperty("IgnoreCharsetDirective");
        p.parse(new java.io.StringReader(html), receiver,
                (ignoreCharset == null) ? false : ignoreCharset.booleanValue());
        receiver.flush();
    }

    /** It writes the document as HTML. */
    public void write(Writer out, Document doc, int pos, int len) throws IOException,
            BadLocationException {
        if (doc instanceof HTMLDocument) {
            HTMLWriter w = new HTMLWriter(out, (HTMLDocument) doc, pos, len);
            w.write();
        } else if (doc instanceof javax.swing.text.StyledDocument) {
            MinimalHTMLWriter w = new MinimalHTMLWriter(out,
                    (javax.swing.text.StyledDocument) doc, pos, len);
            w.write();
        } else {
            super.write(out, doc, pos, len);
        }
    }

    /** It hooks itself to the pane and puts the link watcher on it. */
    public void install(JEditorPane c) {
        c.addMouseListener(linkHandler);
        c.addMouseMotionListener(linkHandler);
        super.install(c);
    }

    public void deinstall(JEditorPane c) {
        c.removeMouseListener(linkHandler);
        c.removeMouseMotionListener(linkHandler);
        super.deinstall(c);
    }

    /** This kit's style sheet; see the class note. */
    public void setStyleSheet(StyleSheet s) {
        defaultStyles = s;
    }

    public StyleSheet getStyleSheet() {
        if (defaultStyles == null) {
            defaultStyles = new StyleSheet();
            defaultStyles.addRule(BASE_RULES);
        }
        return defaultStyles;
    }

    public Action[] getActions() {
        return javax.swing.text.TextAction.augmentList(super.getActions(), defaultActions);
    }

    protected void createInputAttributes(Element element, MutableAttributeSet set) {
        set.removeAttributes(set);
        set.addAttributes(element.getAttributes());
        set.removeAttribute(StyleConstants.ComposedTextAttribute);
    }

    public MutableAttributeSet getInputAttributes() {
        if (input == null) {
            input = new SimpleAttributeSet();
        }
        return input;
    }

    /** The cursor seen over ordinary text. */
    public void setDefaultCursor(Cursor cursor) {
        defaultCursor = cursor;
    }

    public Cursor getDefaultCursor() {
        return defaultCursor;
    }

    /** The cursor seen over a link. */
    public void setLinkCursor(Cursor cursor) {
        linkCursor = cursor;
    }

    public Cursor getLinkCursor() {
        return linkCursor;
    }

    /**
     * Whether on submitting a form the kit itself loads the answer.
     *
     * <p>Turning it off makes the submission arrive as a {@link FormSubmitEvent} and nothing else:
     * whoever listens decides what to do. It is what has to be done if the document may come from
     * outside, because otherwise any page can make the program request an address.
     */
    public boolean isAutoFormSubmission() {
        return isAutoFormSubmission;
    }

    public void setAutoFormSubmission(boolean isAuto) {
        isAutoFormSubmission = isAuto;
    }

    public Object clone() {
        HTMLEditorKit o = (HTMLEditorKit) super.clone();
        if (o != null) {
            o.input = null;
            o.linkHandler = new LinkController();
        }
        return o;
    }

    /**
     * The HTML parser.
     *
     * <p>It is looked for by reflection; see the class note. If it is not there, null is returned
     * and whoever asked for it will fail when reading, which is better than failing when building
     * the editor kit.
     */
    protected Parser getParser() {
        if (defaultParser == null) {
            try {
                Class<?> c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
                defaultParser = (Parser) c.getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                defaultParser = null;
            }
        }
        return defaultParser;
    }

    private static Parser defaultParser = null;

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /** The minimum CSS that makes a document with no sheet look like HTML. */
    private static final String BASE_RULES =
            "body { margin-top: 8; margin-bottom: 8; margin-left: 8; margin-right: 8 }"
            + "p { margin-top: 5 }"
            + "h1 { font-size: 36pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h2 { font-size: 24pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h3 { font-size: 18pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h4 { font-size: 14pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h5 { font-size: 12pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "h6 { font-size: 10pt; font-weight: bold; margin-top: 8; margin-bottom: 8 }"
            + "b { font-weight: bold }"
            + "strong { font-weight: bold }"
            + "i { font-style: italic }"
            + "em { font-style: italic }"
            + "cite { font-style: italic }"
            + "u { text-decoration: underline }"
            + "tt { font-family: Monospaced }"
            + "code { font-family: Monospaced }"
            + "kbd { font-family: Monospaced }"
            + "samp { font-family: Monospaced }"
            + "pre { font-family: Monospaced; margin-top: 5 }"
            + "a { color: blue; text-decoration: underline }"
            + "ul { margin-left: 50; list-style-type: disc }"
            + "ol { margin-left: 50; list-style-type: decimal }"
            + "blockquote { margin-left: 40; margin-right: 40 }";

    private static final Action[] defaultActions = {
        new InsertHTMLTextAction("InsertTable", "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.BODY, HTML.Tag.TABLE),
        new InsertHTMLTextAction("InsertTableRow", "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.TABLE, HTML.Tag.TR, HTML.Tag.BODY, HTML.Tag.TABLE),
        new InsertHTMLTextAction("InsertTableDataCell",
                "<table border=1><tr><td></td></tr></table>",
                HTML.Tag.TR, HTML.Tag.TD, HTML.Tag.BODY, HTML.Tag.TABLE),
        new InsertHTMLTextAction("InsertUnorderedList", "<ul><li></li></ul>",
                HTML.Tag.BODY, HTML.Tag.UL),
        new InsertHTMLTextAction("InsertUnorderedListItem", "<ul><li></li></ul>",
                HTML.Tag.UL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.UL),
        new InsertHTMLTextAction("InsertOrderedList", "<ol><li></li></ol>",
                HTML.Tag.BODY, HTML.Tag.OL),
        new InsertHTMLTextAction("InsertOrderedListItem", "<ol><li></li></ol>",
                HTML.Tag.OL, HTML.Tag.LI, HTML.Tag.BODY, HTML.Tag.OL),
        new InsertHTMLTextAction("InsertPre", "<pre></pre>", HTML.Tag.BODY, HTML.Tag.PRE)
    };

    /**
     * What an HTML parser has to know how to do.
     *
     * <p>It is abstract and has a single method. That it is a class and not an interface is from
     * 1998 and is kept because {@link HTMLDocument#setParser} takes it.
     */
    public abstract static class Parser {

        protected Parser() {
        }

        /**
         * It parses the text and tells whoever listens.
         *
         * @param ignoreCharSet whether the encoding the document declares has to be ignored.
         */
        public abstract void parse(Reader r, ParserCallback cb, boolean ignoreCharSet)
                throws IOException;
    }

    /**
     * Whoever listens to what the parser finds.
     *
     * <p>Every method does nothing: the ones that matter are overridden. That it is a class with
     * empty bodies and not an interface is what allows listening only to the text without writing
     * six empty methods.
     *
     * <p>The number that arrives in each method is the position in the input text, not in the
     * document. It serves for pointing at where an error was.
     */
    public static class ParserCallback {

        /**
         * It marks the attributes of an element the parser invented.
         *
         * <p>It goes as a key in the attribute set, with the value {@link Boolean#TRUE}. Whoever
         * writes the document back looks at it so as not to write tags the author never put in.
         */
        public static final Object IMPLIED = "_implied_";

        public ParserCallback() {
        }

        /** It is called on finishing; it is where what was gathered should be flushed. */
        public void flush() throws BadLocationException {
        }

        public void handleText(char[] data, int pos) {
        }

        public void handleComment(char[] data, int pos) {
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        }

        public void handleEndTag(HTML.Tag t, int pos) {
        }

        /** A tag with no closing, such as {@code br} or {@code img}. */
        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        }

        public void handleError(String errorMsg, int pos) {
        }

        /** The line ending the document used, so as to be able to write it back the same. */
        public void handleEndOfLineString(String eol) {
        }
    }

    /**
     * The view factory: one view per tag.
     *
     * <p>The sharing out is not arbitrary. What makes a block goes to a {@link BlockView}, what
     * goes on the line to an {@link InlineView}, and the tags that have no text of their own -- an
     * image, a form field -- to views that draw something else.
     */
    public static class HTMLFactory implements ViewFactory {

        public HTMLFactory() {
        }

        public View create(Element elem) {
            AttributeSet attrs = elem.getAttributes();
            Object elementName = attrs.getAttribute(
                    javax.swing.text.AbstractDocument.ElementNameAttribute);
            Object o = (elementName != null) ? null
                    : attrs.getAttribute(StyleConstants.NameAttribute);
            if (o instanceof HTML.Tag) {
                HTML.Tag kind = (HTML.Tag) o;
                if (kind == HTML.Tag.CONTENT) {
                    return new InlineView(elem);
                } else if (kind == HTML.Tag.IMPLIED) {
                    return new ParagraphView(elem);
                } else if (kind == HTML.Tag.P || kind == HTML.Tag.H1 || kind == HTML.Tag.H2
                        || kind == HTML.Tag.H3 || kind == HTML.Tag.H4 || kind == HTML.Tag.H5
                        || kind == HTML.Tag.H6 || kind == HTML.Tag.DT) {
                    return new ParagraphView(elem);
                } else if (kind == HTML.Tag.MENU || kind == HTML.Tag.DIR || kind == HTML.Tag.UL
                        || kind == HTML.Tag.OL) {
                    return new ListView(elem);
                } else if (kind == HTML.Tag.BODY || kind == HTML.Tag.HTML
                        || kind == HTML.Tag.LI || kind == HTML.Tag.CENTER
                        || kind == HTML.Tag.DL || kind == HTML.Tag.DD || kind == HTML.Tag.DIV
                        || kind == HTML.Tag.BLOCKQUOTE || kind == HTML.Tag.PRE
                        || kind == HTML.Tag.FORM) {
                    return new BlockView(elem, View.Y_AXIS);
                } else if (kind == HTML.Tag.IMG) {
                    return new ImageView(elem);
                } else if (kind == HTML.Tag.INPUT || kind == HTML.Tag.SELECT
                        || kind == HTML.Tag.TEXTAREA) {
                    return new FormView(elem);
                } else if (kind == HTML.Tag.OBJECT) {
                    return new ObjectView(elem);
                } else if (kind == HTML.Tag.COMMENT || kind == HTML.Tag.HEAD
                        || kind == HTML.Tag.TITLE || kind == HTML.Tag.META
                        || kind == HTML.Tag.LINK || kind == HTML.Tag.STYLE
                        || kind == HTML.Tag.SCRIPT || kind == HTML.Tag.AREA
                        || kind == HTML.Tag.MAP || kind == HTML.Tag.PARAM
                        || kind == HTML.Tag.APPLET) {
                    // What is not seen takes up a place in the tree all the same: a view of zero
                    // size.
                    return new InvisibleView(elem);
                }
            }
            // An element that is not known: if it has children it is a block, if not it is text.
            String nm = elem.getName();
            if (nm != null && nm.equals(javax.swing.text.AbstractDocument.ContentElementName)) {
                return new javax.swing.text.LabelView(elem);
            }
            if (elem.isLeaf()) {
                return new javax.swing.text.LabelView(elem);
            }
            return new javax.swing.text.BoxView(elem, View.Y_AXIS);
        }
    }

    /** A view that takes up zero: the one for the tags that are not shown. */
    static class InvisibleView extends View {

        InvisibleView(Element elem) {
            super(elem);
        }

        public float getPreferredSpan(int axis) {
            return 0;
        }

        public void paint(java.awt.Graphics g, java.awt.Shape allocation) {
        }

        public java.awt.Shape modelToView(int pos, java.awt.Shape a,
                javax.swing.text.Position.Bias b) throws BadLocationException {
            return a;
        }

        public int viewToModel(float x, float y, java.awt.Shape a,
                javax.swing.text.Position.Bias[] bias) {
            bias[0] = javax.swing.text.Position.Bias.Forward;
            return getStartOffset();
        }
    }

    /**
     * It attends to clicks on links.
     *
     * <p>It changes the cursor when passing over and fires the event on clicking. That it is a
     * separate class and not code of the editor kit's allows a pane to have another behaviour
     * without replacing the whole kit.
     */
    public static class LinkController extends MouseAdapter implements MouseMotionListener,
            Serializable {

        public LinkController() {
        }

        public void mouseClicked(MouseEvent e) {
            JEditorPane editor = (JEditorPane) e.getSource();
            if (!editor.isEditable() && javax.swing.SwingUtilities.isLeftMouseButton(e)) {
                java.awt.Point pt = new java.awt.Point(e.getX(), e.getY());
                int pos = editor.viewToModel(pt);
                if (pos >= 0) {
                    activateLink(pos, editor);
                }
            }
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
        }

        /** It fires the event of the link at that position. */
        protected void activateLink(int pos, JEditorPane editor) {
        }
    }

    /**
     * The base of the actions that edit HTML.
     *
     * <p>It brings what all of them need: getting to the document and to the kit, and looking up
     * the tree for the element of a given tag. That last thing is what allows writing an action
     * such as "insert a row" without knowing where the table is.
     */
    public abstract static class HTMLTextAction extends StyledEditorKit.StyledTextAction {

        public HTMLTextAction(String name) {
            super(name);
        }

        protected HTMLDocument getHTMLDocument(JEditorPane e) {
            Document d = e.getDocument();
            if (d instanceof HTMLDocument) {
                return (HTMLDocument) d;
            }
            throw new IllegalArgumentException("document must be HTMLDocument");
        }

        protected HTMLEditorKit getHTMLEditorKit(JEditorPane e) {
            javax.swing.text.EditorKit k = e.getEditorKit();
            if (k instanceof HTMLEditorKit) {
                return (HTMLEditorKit) k;
            }
            throw new IllegalArgumentException("EditorKit must be HTMLEditorKit");
        }

        /** The path of elements from the root to that position. */
        protected Element[] getElementsAt(HTMLDocument doc, int offset) {
            return getElementsAt(doc.getDefaultRootElement(), offset, 0);
        }

        private Element[] getElementsAt(Element parent, int offset, int depth) {
            if (parent.isLeaf()) {
                Element[] retValue = new Element[depth + 1];
                retValue[depth] = parent;
                return retValue;
            }
            Element[] retValue = getElementsAt(parent.getElement(
                    parent.getElementIndex(offset)), offset, depth + 1);
            retValue[depth] = parent;
            return retValue;
        }

        /**
         * How many elements have to be closed to get to that tag.
         *
         * <p>It returns -1 if the tag is not on the path. It is the number
         * {@link HTMLEditorKit#insertHTML} calls {@code popDepth}.
         */
        protected int elementCountToTag(HTMLDocument doc, int offset, HTML.Tag tag) {
            int depth = -1;
            Element e = doc.getCharacterElement(offset);
            while (e != null && e.getAttributes().getAttribute(
                    StyleConstants.NameAttribute) != tag) {
                e = e.getParentElement();
                depth++;
            }
            if (e == null) {
                return -1;
            }
            return depth;
        }

        /** The nearest element upwards that has that tag. */
        protected Element findElementMatchingTag(HTMLDocument doc, int offset, HTML.Tag tag) {
            Element e = doc.getDefaultRootElement();
            Element lastMatch = null;
            while (e != null) {
                if (e.getAttributes().getAttribute(StyleConstants.NameAttribute) == tag) {
                    lastMatch = e;
                }
                e = e.getElement(e.getElementIndex(offset));
            }
            return lastMatch;
        }
    }

    /**
     * It inserts a fragment of HTML in the place that corresponds.
     *
     * <p>The two pairs of tags are the reason this action exists. The first says where it can be
     * inserted and what is inserted; the second is the fallback plan. Inserting a
     * <code>&lt;li&gt;</code> inside a <code>&lt;ul&gt;</code> adds a row; if there is no list, the
     * fallback plan creates the whole list.
     */
    public static class InsertHTMLTextAction extends HTMLTextAction {

        /** The HTML that is inserted. */
        protected String html;

        /** The tag that has to contain the insertion. */
        protected HTML.Tag parentTag;

        /** The tag that is added. */
        protected HTML.Tag addTag;

        /** The fallback plan's container. */
        protected HTML.Tag alternateParentTag;

        /** What is added in the fallback plan. */
        protected HTML.Tag alternateAddTag;

        /** An action with no fallback plan. */
        public InsertHTMLTextAction(String name, String html, HTML.Tag parentTag,
                HTML.Tag addTag) {
            this(name, html, parentTag, addTag, null, null);
        }

        /** An action with a fallback plan. */
        public InsertHTMLTextAction(String name, String html, HTML.Tag parentTag, HTML.Tag addTag,
                HTML.Tag alternateParentTag, HTML.Tag alternateAddTag) {
            super(name);
            this.html = html;
            this.parentTag = parentTag;
            this.addTag = addTag;
            this.alternateParentTag = alternateParentTag;
            this.alternateAddTag = alternateAddTag;
        }

        /** It does the insertion; see {@link HTMLEditorKit#insertHTML}. */
        protected void insertHTML(JEditorPane editor, HTMLDocument doc, int offset, String html,
                int popDepth, int pushDepth, HTML.Tag addTag) {
            try {
                getHTMLEditorKit(editor).insertHTML(doc, offset, html, popDepth, pushDepth,
                        addTag);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                editor.getToolkit().beep();
            }
        }

        /**
         * It inserts when the position falls right at an element's edge.
         *
         * <p>It is the awkward case: at the edge, the position belongs to two elements and one has
         * to be chosen. The inner one is chosen, which is what whoever is typing expects.
         */
        protected void insertAtBoundary(JEditorPane editor, HTMLDocument doc, int offset,
                Element insertElement, String html, HTML.Tag parentTag, HTML.Tag addTag) {
            insertAtBoundry(editor, doc, offset, insertElement, html, parentTag, addTag);
        }

        /**
         * The old name of {@link #insertAtBoundary}.
         *
         * @deprecated Use {@link #insertAtBoundary}, which is spelled right.
         */
        @Deprecated
        protected void insertAtBoundry(JEditorPane editor, HTMLDocument doc, int offset,
                Element insertElement, String html, HTML.Tag parentTag, HTML.Tag addTag) {
            Element[] elements = getElementsAt(doc, offset);
            int depth = 0;
            for (int i = elements.length - 1; i >= 0; i--) {
                if (elements[i].getAttributes().getAttribute(
                        StyleConstants.NameAttribute) == parentTag) {
                    break;
                }
                depth++;
            }
            insertHTML(editor, doc, offset, html, depth, 0, addTag);
        }

        public void actionPerformed(ActionEvent ae) {
            JEditorPane editor = getEditor(ae);
            if (editor != null) {
                HTMLDocument doc = getHTMLDocument(editor);
                int offset = editor.getSelectionStart();
                int depth = elementCountToTag(doc, offset, parentTag);
                if (depth != -1) {
                    insertHTML(editor, doc, offset, html, depth, 0, addTag);
                } else if (alternateParentTag != null) {
                    depth = elementCountToTag(doc, offset, alternateParentTag);
                    if (depth != -1) {
                        insertHTML(editor, doc, offset, html, depth, 0, alternateAddTag);
                        return;
                    }
                    editor.getToolkit().beep();
                } else {
                    editor.getToolkit().beep();
                }
            }
        }
    }
}
