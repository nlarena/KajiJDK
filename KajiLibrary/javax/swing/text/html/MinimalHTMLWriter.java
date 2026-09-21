package javax.swing.text.html;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import javax.swing.text.AbstractWriter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * It writes as HTML a styled document that is not an HTML one.
 *
 * <h2>What it is for</h2>
 *
 * <p>A {@link javax.swing.text.DefaultStyledDocument} has no tags: it has Swing attributes. This
 * writer translates them to HTML so as to be able to save or send what is in a
 * {@code JTextPane}.
 *
 * <p>It is "minimal" because it does not try to rebuild structure. It writes a {@code <p>} per
 * paragraph and whatever character tags are needed, and everything the Swing attributes cannot
 * say in HTML it puts as a style in the head.
 *
 * <h2>What cannot be written</h2>
 *
 * <p>An embedded component or icon has no equivalent: a comment is written saying they were
 * there. It is better than losing them silently, even though they cannot be read back.
 */
public class MinimalHTMLWriter extends AbstractWriter {

    private static final int BOLD = 0x01;
    private static final int ITALIC = 0x02;
    private static final int UNDERLINE = 0x04;

    private int fontMask = 0;
    private int startOffset = 0;
    private int endOffset = 0;
    private boolean inFontTag = false;
    private boolean fontAttrSet = false;
    private AttributeSet fontAttributes;

    /** It writes the whole document. */
    public MinimalHTMLWriter(Writer w, StyledDocument doc) {
        super(w, doc);
    }

    /** It writes that stretch of the document. */
    public MinimalHTMLWriter(Writer w, StyledDocument doc, int pos, int len) {
        super(w, doc, pos, len);
    }

    /** It writes the document: a head with the styles, and then the body. */
    public void write() throws IOException, BadLocationException {
        styleNameMapping = new java.util.Hashtable<String, String>();
        writeStartTag("<html>");
        writeHeader();
        writeBody();
        writeEndTag("</html>");
    }

    private java.util.Hashtable<String, String> styleNameMapping;

    /** The attributes that have no tag of their own, written as CSS. */
    protected void writeAttributes(AttributeSet attr) throws IOException {
        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            write("  " + name + ": " + attr.getAttribute(name) + ";");
            writeLineSeparator();
        }
    }

    /** A leaf element's text, with the entities escaped. */
    protected void text(Element elem) throws IOException, BadLocationException {
        String contentStr = getText(elem);
        if (contentStr.length() > 0 && contentStr.charAt(contentStr.length() - 1) == '\n') {
            contentStr = contentStr.substring(0, contentStr.length() - 1);
        }
        if (contentStr.length() > 0) {
            write(contentStr);
        }
    }

    /** It writes an opening tag on its own line and indents what follows. */
    protected void writeStartTag(String tag) throws IOException {
        indent();
        write(tag);
        writeLineSeparator();
        incrIndent();
    }

    protected void writeEndTag(String endTag) throws IOException {
        decrIndent();
        indent();
        write(endTag);
        writeLineSeparator();
    }

    /** The head, with the document's styles inside. */
    protected void writeHeader() throws IOException {
        writeStartTag("<head>");
        writeStartTag("<style>");
        writeStartTag("<!--");
        writeStyles();
        writeEndTag("-->");
        writeEndTag("</style>");
        writeEndTag("</head>");
    }

    /**
     * The document's named styles, as CSS rules.
     *
     * <p>The default style is not written: it has no name to give the rule, and its values are the
     * ones that already hold without saying anything.
     */
    protected void writeStyles() throws IOException {
        javax.swing.text.StyleContext ctx = null;
        javax.swing.text.Document doc = getDocument();
        if (doc instanceof javax.swing.text.DefaultStyledDocument) {
            Enumeration<?> names =
                    ((javax.swing.text.DefaultStyledDocument) doc).getStyleNames();
            while (names != null && names.hasMoreElements()) {
                String name = (String) names.nextElement();
                if (javax.swing.text.StyleContext.DEFAULT_STYLE.equals(name)) {
                    continue;
                }
                Style s = ((javax.swing.text.DefaultStyledDocument) doc).getStyle(name);
                if (s == null || s.getAttributeCount() == 0) {
                    continue;
                }
                indent();
                write("p." + addStyleName(name) + " {");
                writeLineSeparator();
                incrIndent();
                writeAttributes(s);
                decrIndent();
                indent();
                write("}");
                writeLineSeparator();
            }
        }
    }

    /** A style name that can be written in a rule; the spaces get in the way. */
    private String addStyleName(String name) {
        String clean = name.replace(' ', '-');
        styleNameMapping.put(name, clean);
        return clean;
    }

    /** The body: one paragraph per paragraph of the document. */
    protected void writeBody() throws IOException, BadLocationException {
        ElementIterator it = getElementIterator();
        writeStartTag("<body>");
        Element next;
        while ((next = it.next()) != null) {
            if (!inRange(next)) {
                continue;
            }
            if (next instanceof javax.swing.text.AbstractDocument.BranchElement) {
                writeStartParagraph(next);
            } else if (isText(next)) {
                writeContent(next, true);
            } else {
                writeLeaf(next);
            }
            if (isLastOfParagraph(it, next)) {
                writeEndParagraph();
            }
        }
        writeEndTag("</body>");
    }

    private boolean isLastOfParagraph(ElementIterator it, Element e) {
        Element parent = e.getParentElement();
        return (parent != null
                && parent.getElement(parent.getElementCount() - 1) == e);
    }

    /** It closes the paragraph and the character tags that were left open. */
    protected void writeEndParagraph() throws IOException {
        writeEndMask();
        if (inFontTag()) {
            endFontTag();
        }
        write("</p>");
        writeLineSeparator();
    }

    /** It opens the paragraph, with its class if the element has a named style. */
    protected void writeStartParagraph(Element elem) throws IOException {
        AttributeSet attr = elem.getAttributes();
        Object resolveAttr = attr.getAttribute(StyleConstants.ResolveAttribute);
        if (resolveAttr instanceof Style) {
            String name = ((Style) resolveAttr).getName();
            String clean = styleNameMapping.get(name);
            write("<p class=" + (clean == null ? name : clean) + ">");
        } else {
            write("<p>");
        }
        writeLineSeparator();
    }

    /** A leaf that is not text: an icon or a component. */
    protected void writeLeaf(Element elem) throws IOException {
        indent();
        if (elem.getName().equals(StyleConstants.IconElementName)) {
            writeImage(elem);
        } else if (elem.getName().equals(StyleConstants.ComponentElementName)) {
            writeComponent(elem);
        }
    }

    /** An icon; see the class note on what cannot be written. */
    protected void writeImage(Element elem) throws IOException {
        write("<!-- icono -->");
    }

    /** An embedded component. */
    protected void writeComponent(Element elem) throws IOException {
        write("<!-- componente -->");
    }

    /** Whether the element is ordinary text. */
    protected boolean isText(Element elem) {
        return (elem.getName().equals(javax.swing.text.AbstractDocument.ContentElementName));
    }

    /** It writes a stretch of text with its character tags around it. */
    protected void writeContent(Element elem, boolean needsIndenting) throws IOException,
            BadLocationException {
        AttributeSet attr = elem.getAttributes();
        writeNonHTMLAttributes(attr);
        if (needsIndenting) {
            indent();
        }
        writeHTMLTags(attr);
        text(elem);
    }

    /**
     * It opens and closes bold, italic and underline as they change.
     *
     * <p>It carries a three-bit mask with what is open. Comparing the new mask with the old one
     * says exactly what to open and what to close, without repeating tags or leaving any
     * unclosed.
     */
    protected void writeHTMLTags(AttributeSet attr) throws IOException {
        int oldMask = fontMask;
        setFontMask(attr);
        int endMask = 0;
        int startMask = 0;
        if ((oldMask & BOLD) != 0) {
            if ((fontMask & BOLD) == 0) {
                endMask = endMask | BOLD;
            }
        } else if ((fontMask & BOLD) != 0) {
            startMask = startMask | BOLD;
        }
        if ((oldMask & ITALIC) != 0) {
            if ((fontMask & ITALIC) == 0) {
                endMask = endMask | ITALIC;
            }
        } else if ((fontMask & ITALIC) != 0) {
            startMask = startMask | ITALIC;
        }
        if ((oldMask & UNDERLINE) != 0) {
            if ((fontMask & UNDERLINE) == 0) {
                endMask = endMask | UNDERLINE;
            }
        } else if ((fontMask & UNDERLINE) != 0) {
            startMask = startMask | UNDERLINE;
        }
        writeEndMask(endMask);
        writeStartMask(startMask);
    }

    private void setFontMask(AttributeSet attr) {
        fontMask = 0;
        if (StyleConstants.isBold(attr)) {
            fontMask = fontMask | BOLD;
        }
        if (StyleConstants.isItalic(attr)) {
            fontMask = fontMask | ITALIC;
        }
        if (StyleConstants.isUnderline(attr)) {
            fontMask = fontMask | UNDERLINE;
        }
    }

    private void writeStartMask(int mask) throws IOException {
        if ((mask & UNDERLINE) != 0) {
            write("<u>");
        }
        if ((mask & ITALIC) != 0) {
            write("<i>");
        }
        if ((mask & BOLD) != 0) {
            write("<b>");
        }
    }

    private void writeEndMask(int mask) throws IOException {
        // The reverse of how they were opened: HTML does not allow crossing them.
        if ((mask & BOLD) != 0) {
            write("</b>");
        }
        if ((mask & ITALIC) != 0) {
            write("</i>");
        }
        if ((mask & UNDERLINE) != 0) {
            write("</u>");
        }
    }

    private void writeEndMask() throws IOException {
        writeEndMask(fontMask);
        fontMask = 0;
    }

    /**
     * What has no HTML tag, written as a {@code <span>} with a style.
     *
     * <p>The size and the colour do have an old tag ({@code <font>}) and go through it; the rest
     * -- a spacing, an indent -- can only be said in CSS.
     */
    protected void writeNonHTMLAttributes(AttributeSet attr) throws IOException {
        String color = null;
        Color c = StyleConstants.getForeground(attr);
        if (c != null && !c.equals(Color.black)) {
            color = "#" + hex(c.getRed()) + hex(c.getGreen()) + hex(c.getBlue());
        }
        if (color != null) {
            if (inFontTag()) {
                endFontTag();
            }
            startFontTag("color=\"" + color + "\"");
        } else if (inFontTag()) {
            endFontTag();
        }
    }

    private static String hex(int v) {
        String s = Integer.toHexString(v);
        return (s.length() == 1) ? "0" + s : s;
    }

    /** Whether there is a {@code <font>} tag open. */
    protected boolean inFontTag() {
        return inFontTag;
    }

    protected void endFontTag() throws IOException {
        write("</font>");
        inFontTag = false;
    }

    protected void startFontTag(String style) throws IOException {
        if (inFontTag()) {
            endFontTag();
        }
        write("<font " + style + ">");
        inFontTag = true;
    }
}
