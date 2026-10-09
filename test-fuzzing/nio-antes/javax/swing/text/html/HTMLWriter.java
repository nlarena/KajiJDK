package javax.swing.text.html;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.text.AbstractWriter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyleConstants;

/**
 * It writes an {@link HTMLDocument} back out as HTML.
 *
 * <h2>The problem: the tree is not the HTML</h2>
 *
 * <p>A document keeps paragraphs and stretches of characters with attributes. HTML has nested
 * tags. They are not the same: a stretch with bold and italic is <em>one</em> element of the
 * document and <em>two</em> nested tags in the HTML.
 *
 * <p>That is why the writer keeps track of the tags it has open
 * ({@link #writeEmbeddedTags} and {@link #closeOutUnwantedEmbeddedTags}): on going from one
 * stretch to the next it opens those that appear and closes those that stopped being there, in
 * the right order.
 *
 * <h2>Elements that are not written</h2>
 *
 * <p>The parser invents elements the author did not write -- the implicit paragraph, the
 * <code>&lt;html&gt;</code> that was missing -- and marks them. {@link #synthesizedElement}
 * recognizes them and skips them, so that the HTML that comes out looks like the one that went
 * in.
 */
public class HTMLWriter extends AbstractWriter {

    private Vector<HTML.Tag> tags = new Vector<HTML.Tag>(10);
    private Vector<Object> tagValues = new Vector<Object>(10);
    private Vector<HTML.Tag> tagsToRemove = new Vector<HTML.Tag>(10);
    private boolean wroteHead = false;
    private boolean replaceEntities = false;
    private boolean inContent = false;
    private boolean inPre = false;
    private boolean indentNext = false;
    private char[] tempChars;

    /** It writes the whole document. */
    public HTMLWriter(Writer w, HTMLDocument doc) {
        this(w, doc, 0, doc.getLength());
    }

    /** It writes that stretch of the document. */
    public HTMLWriter(Writer w, HTMLDocument doc, int pos, int len) {
        super(w, doc, pos, len);
    }

    /**
     * It writes the document.
     *
     * <p>It walks the element tree with an {@link ElementIterator}, opening and closing tags
     * according to the level.
     */
    public void write() throws IOException, BadLocationException {
        ElementIterator it = getElementIterator();
        Element current = it.current();
        Element next;

        setCurrentLineLength(0);
        while (current != null) {
            if (!inRange(current)) {
                current = it.next();
                continue;
            }
            if (current instanceof javax.swing.text.AbstractDocument.BranchElement) {
                if (!synthesizedElement(current)) {
                    startTag(current);
                }
            } else {
                if (matchNameAttribute(current.getAttributes(), HTML.Tag.CONTENT)) {
                    text(current);
                } else if (matchNameAttribute(current.getAttributes(), HTML.Tag.COMMENT)) {
                    comment(current);
                } else {
                    emptyTag(current);
                }
            }
            next = it.next();
            if (next == null) {
                closeUpTo(0, current);
                break;
            }
            int currentLevel = level(current);
            int nextLevel = level(next);
            if (nextLevel <= currentLevel) {
                closeUpTo(nextLevel, current);
            }
            current = next;
        }
        closeOutUnwantedEmbeddedTags(null);
    }

    private static int level(Element e) {
        int n = 0;
        for (Element p = e.getParentElement(); p != null; p = p.getParentElement()) {
            n++;
        }
        return n;
    }

    /** It closes the tags open from that element down to that level. */
    private void closeUpTo(int targetLevel, Element from) throws IOException {
        Element e = from;
        if (!(e instanceof javax.swing.text.AbstractDocument.BranchElement)) {
            e = e.getParentElement();
        }
        while (e != null && level(e) >= targetLevel) {
            if (!synthesizedElement(e)) {
                endTag(e);
            }
            e = e.getParentElement();
        }
    }

    /**
     * It writes a set's attributes.
     *
     * <p>It does not write the internal ones: the element's name and the marks the parser put in
     * are not HTML attributes, and writing them would give a document that cannot be read back.
     */
    protected void writeAttributes(AttributeSet attr) throws IOException {
        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            if (name instanceof HTML.Tag
                    || name instanceof StyleConstants
                    || name == HTMLEditorKit.ParserCallback.IMPLIED
                    || name == javax.swing.text.AbstractDocument.ElementNameAttribute) {
                continue;
            }
            Object value = attr.getAttribute(name);
            write(" " + name + "=\"" + value + "\"");
        }
    }

    /** It writes a tag with no closing, such as {@code <br>} or {@code <img>}. */
    protected void emptyTag(Element elem) throws BadLocationException, IOException {
        AttributeSet attr = elem.getAttributes();
        closeOutUnwantedEmbeddedTags(attr);
        writeEmbeddedTags(attr);
        Object name = attr.getAttribute(StyleConstants.NameAttribute);
        if (name instanceof HTML.Tag) {
            write('<');
            write(name.toString());
            writeAttributes(attr);
            write('>');
        }
    }

    /** Whether that tag makes a block. */
    protected boolean isBlockTag(AttributeSet attr) {
        Object o = attr.getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag) {
            return ((HTML.Tag) o).isBlock();
        }
        return false;
    }

    /** It writes the opening tag of an element with children. */
    protected void startTag(Element elem) throws IOException, BadLocationException {
        AttributeSet attr = elem.getAttributes();
        Object name = attr.getAttribute(StyleConstants.NameAttribute);
        if (!(name instanceof HTML.Tag)) {
            return;
        }
        HTML.Tag tag = (HTML.Tag) name;
        if (tag == HTML.Tag.PRE) {
            inPre = true;
        }
        if (isBlockTag(attr) && !isLineEmpty()) {
            writeLineSeparator();
        }
        indent();
        write('<');
        write(tag.toString());
        writeAttributes(attr);
        write('>');
        if (tag.isBlock()) {
            writeLineSeparator();
        }
        incrIndent();
    }

    /** A {@code <textarea>}'s content, as it is, without breaking lines. */
    protected void textAreaContent(AttributeSet attr) throws BadLocationException, IOException {
        Object model = attr.getAttribute(StyleConstants.ModelAttribute);
        if (model instanceof javax.swing.text.Document) {
            javax.swing.text.Document doc = (javax.swing.text.Document) model;
            String text = doc.getText(0, doc.getLength());
            setCanWrapLines(false);
            write(text);
            setCanWrapLines(true);
        }
    }

    /** A leaf element's text, with the entities escaped. */
    protected void text(Element elem) throws BadLocationException, IOException {
        int start = Math.max(getStartOffset(), elem.getStartOffset());
        int end = Math.min(getEndOffset(), elem.getEndOffset());
        if (start >= end) {
            return;
        }
        String s = getDocument().getText(start, end - start);
        inContent = true;
        if (inPre) {
            setCanWrapLines(false);
        }
        write(escape(s));
        if (inPre) {
            setCanWrapLines(true);
        }
    }

    /**
     * It escapes what in HTML cannot be written as it is.
     *
     * <p>There are four: the two angles, the ampersand and the double quote. The ampersand goes
     * first, otherwise it would escape the one the previous replacement has just written.
     */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * A {@code <select>}'s options.
     *
     * <p>They come from the control's model, not from the document: the user may have changed the
     * selection, and what has to be written is what is seen.
     */
    protected void selectContent(AttributeSet attr) throws IOException {
        Object model = attr.getAttribute(StyleConstants.ModelAttribute);
        incrIndent();
        if (model instanceof javax.swing.ComboBoxModel) {
            javax.swing.ComboBoxModel<?> m = (javax.swing.ComboBoxModel<?>) model;
            for (int i = 0; i < m.getSize(); i++) {
                Object o = m.getElementAt(i);
                if (o instanceof Option) {
                    writeOption((Option) o);
                }
            }
        } else if (model instanceof javax.swing.ListModel) {
            javax.swing.ListModel<?> m = (javax.swing.ListModel<?>) model;
            for (int i = 0; i < m.getSize(); i++) {
                Object o = m.getElementAt(i);
                if (o instanceof Option) {
                    writeOption((Option) o);
                }
            }
        }
        decrIndent();
    }

    /** A list option, with its selected mark if it has one. */
    protected void writeOption(Option option) throws IOException {
        indent();
        write('<');
        write("option");
        Object value = option.getAttributes().getAttribute(HTML.Attribute.VALUE);
        if (value != null) {
            write(" value=" + value);
        }
        if (option.isSelected()) {
            write(" selected");
        }
        write('>');
        if (option.getLabel() != null) {
            write(option.getLabel());
        }
        writeLineSeparator();
    }

    /** The closing tag. */
    protected void endTag(Element elem) throws IOException {
        AttributeSet attr = elem.getAttributes();
        Object name = attr.getAttribute(StyleConstants.NameAttribute);
        if (!(name instanceof HTML.Tag)) {
            return;
        }
        HTML.Tag tag = (HTML.Tag) name;
        if (tag == HTML.Tag.PRE) {
            inPre = false;
        }
        decrIndent();
        if (tag.isBlock() && !isLineEmpty()) {
            writeLineSeparator();
        }
        indent();
        write('<');
        write('/');
        write(tag.toString());
        write('>');
        if (tag.isBlock()) {
            writeLineSeparator();
        }
    }

    /** A comment, with its delimiters. */
    protected void comment(Element elem) throws BadLocationException, IOException {
        AttributeSet as = elem.getAttributes();
        Object o = as.getAttribute(HTML.Attribute.COMMENT);
        if (o instanceof String) {
            indent();
            write("<!--");
            write((String) o);
            write("-->");
            writeLineSeparator();
        }
    }

    /** Whether the parser invented the element; see the class note. */
    protected boolean synthesizedElement(Element elem) {
        Object o = elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
        if (o == HTML.Tag.IMPLIED) {
            return true;
        }
        return elem.getAttributes().getAttribute(
                HTMLEditorKit.ParserCallback.IMPLIED) != null;
    }

    /** Whether the element's name is that tag. */
    protected boolean matchNameAttribute(AttributeSet attr, HTML.Tag tag) {
        Object o = attr.getAttribute(StyleConstants.NameAttribute);
        return (o instanceof HTML.Tag && o == tag);
    }

    /**
     * It opens the character tags this stretch has and the previous one did not.
     *
     * <p>They are the ones that do not come from an element of the tree but from an attribute of
     * the stretch: bold, italic, a link. See the class note.
     */
    protected void writeEmbeddedTags(AttributeSet attr) throws IOException {
        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            if (name instanceof HTML.Tag) {
                HTML.Tag tag = (HTML.Tag) name;
                if (tag == HTML.Tag.FORM || tags.contains(tag)) {
                    continue;
                }
                write('<');
                write(tag.toString());
                Object o = attr.getAttribute(tag);
                if (o instanceof AttributeSet) {
                    writeAttributes((AttributeSet) o);
                }
                write('>');
                tags.addElement(tag);
                tagValues.addElement(o);
            }
        }
    }

    /**
     * It closes the character tags that no longer apply.
     *
     * <p>They are closed in the reverse order of opening, and if one in the middle stopped holding,
     * those inside are closed too and reopened. There is no other way: HTML does not allow closing
     * a tag skipping over those it has inside.
     */
    protected void closeOutUnwantedEmbeddedTags(AttributeSet attr) throws IOException {
        tagsToRemove.removeAllElements();
        for (int i = 0; i < tags.size(); i++) {
            HTML.Tag tag = tags.elementAt(i);
            Object value = tagValues.elementAt(i);
            if (attr == null || !sameValue(attr.getAttribute(tag), value)) {
                tagsToRemove.addElement(tag);
            }
        }
        if (tagsToRemove.size() == 0) {
            return;
        }
        int from = tags.size();
        for (int i = 0; i < tags.size(); i++) {
            if (tagsToRemove.contains(tags.elementAt(i))) {
                from = i;
                break;
            }
        }
        // It closes from the last one down to the first that stopped holding.
        Vector<HTML.Tag> reopen = new Vector<HTML.Tag>();
        Vector<Object> reopenValue = new Vector<Object>();
        for (int i = tags.size() - 1; i >= from; i--) {
            HTML.Tag tag = tags.elementAt(i);
            write('<');
            write('/');
            write(tag.toString());
            write('>');
            if (!tagsToRemove.contains(tag)) {
                reopen.insertElementAt(tag, 0);
                reopenValue.insertElementAt(tagValues.elementAt(i), 0);
            }
        }
        while (tags.size() > from) {
            tags.removeElementAt(tags.size() - 1);
            tagValues.removeElementAt(tagValues.size() - 1);
        }
        for (int i = 0; i < reopen.size(); i++) {
            HTML.Tag tag = reopen.elementAt(i);
            write('<');
            write(tag.toString());
            write('>');
            tags.addElement(tag);
            tagValues.addElement(reopenValue.elementAt(i));
        }
    }

    private static boolean sameValue(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }

    protected void writeLineSeparator() throws IOException {
        boolean pre = inPre;
        inPre = false;
        super.writeLineSeparator();
        inPre = pre;
    }

    protected void output(char[] chars, int start, int length) throws IOException {
        super.output(chars, start, length);
    }
}
