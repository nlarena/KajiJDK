package javax.swing.text.html.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import javax.swing.text.ChangedCharSetException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;

/**
 * The HTML parser driven by a DTD.
 *
 * <h2>Why the DTD is needed</h2>
 *
 * <p>The HTML that is really written has unclosed tags. A parser that only looked at what was
 * written could not build the tree of <code>&lt;p&gt;one&lt;p&gt;two</code>, because nobody
 * closed the first paragraph. The DTD says that a paragraph can close itself, and with that the
 * parser closes whatever is missing before opening what follows.
 *
 * <p>Three rules are enough for almost everything, and all three come from the DTD:
 *
 * <ul>
 * <li>If the current element does not accept what is coming and its closing may be omitted, it
 *     is closed.
 * <li>If what is coming cannot go loose, whatever it is missing is opened
 *     ({@link Element#omitStart}).
 * <li>An empty element never carries a closing.
 * </ul>
 *
 * <h2>The notices go by methods, not by events</h2>
 *
 * <p>{@link #handleStartTag} and company are protected methods that are overridden. It is the
 * 1997 form and {@link DocumentParser} uses it to forward to an
 * {@link javax.swing.text.html.HTMLEditorKit.ParserCallback}.
 *
 * <h2>Errors</h2>
 *
 * <p>Nothing is thrown: it is reported through {@link #handleError} and it goes on. A broken
 * page has to be shown all the same, even if only half of it. It is the difference between an
 * HTML parser and an XML one.
 */
public class Parser implements DTDConstants {

    /** The DTD that governs the parsing. */
    protected DTD dtd = null;

    /** Whether it is strict about what the DTD does not allow. */
    protected boolean strict = false;

    private Reader in;
    private char[] text = new char[128];
    private int textpos = 0;
    private int ch;
    private int ln = 1;
    private int pos = 0;
    private SimpleAttributeSet attributes = new SimpleAttributeSet();
    private Vector<Element> stack = new Vector<Element>();
    private boolean[] seen;
    private boolean seenHtml = false;
    private boolean seenHead = false;
    private boolean seenBody = false;
    private boolean ignoresSpace = true;

    /** A parser that uses that DTD. */
    public Parser(DTD dtd) {
        this.dtd = dtd;
        seen = new boolean[dtd.elements.size() + 64];
    }

    /** The line being read, counting from one. */
    protected int getCurrentLine() {
        return ln;
    }

    /** How many characters were read. */
    protected int getCurrentPos() {
        return pos;
    }

    /** A tag for that element, real or invented. */
    protected TagElement makeTag(Element elem, boolean fictional) {
        return new TagElement(elem, fictional);
    }

    /** A real tag for that element. */
    protected TagElement makeTag(Element elem) {
        return makeTag(elem, false);
    }

    /** The attributes gathered from the current tag. */
    protected SimpleAttributeSet getAttributes() {
        return attributes;
    }

    /** It empties the gathered attributes. */
    protected void flushAttributes() {
        attributes.removeAttributes(attributes);
    }

    protected void handleText(char[] text) {
    }

    /** The {@code <title>}'s text; it also arrives through {@link #handleText}. */
    protected void handleTitle(char[] text) {
        handleText(text);
    }

    protected void handleComment(char[] text) {
    }

    /** The file ended in the middle of a comment. */
    protected void handleEOFInComment() {
        error("eof.comment");
    }

    /**
     * A tag with no closing.
     *
     * @throws ChangedCharSetException if it was a {@code <meta>} that changes the encoding.
     */
    protected void handleEmptyTag(TagElement tag) throws ChangedCharSetException {
    }

    protected void handleStartTag(TagElement tag) {
    }

    protected void handleEndTag(TagElement tag) {
    }

    /** A problem; see the class note on why nothing is thrown. */
    protected void handleError(int ln, String msg) {
    }

    protected void error(String err, String arg1, String arg2, String arg3) {
        handleError(ln, err + " " + arg1 + " " + arg2 + " " + arg3);
    }

    protected void error(String err, String arg1, String arg2) {
        error(err, arg1, arg2, "?");
    }

    protected void error(String err, String arg1) {
        error(err, arg1, "?", "?");
    }

    protected void error(String err) {
        error(err, "?", "?", "?");
    }

    /**
     * Opens an element, closing and inserting whatever the DTD asks for.
     *
     * <p>It is where the second of the class note's three rules lives.
     */
    protected void startTag(TagElement tag) throws ChangedCharSetException {
        Element elem = tag.getElement();
        if (elem.isEmpty()) {
            handleEmptyTag(tag);
            return;
        }
        markFirstTime(elem);
        stack.addElement(elem);
        handleStartTag(tag);
    }

    /**
     * Closes the element at the top of the stack.
     *
     * @param omitted whether the closing was not written and the parser puts it in.
     */
    protected void endTag(boolean omitted) {
        if (stack.isEmpty()) {
            return;
        }
        Element elem = stack.elementAt(stack.size() - 1);
        stack.removeElementAt(stack.size() - 1);
        handleEndTag(makeTag(elem, omitted));
    }

    /** Notes that an element appeared for the first time. */
    protected void markFirstTime(Element elem) {
        int i = elem.getIndex();
        if (i >= 0 && i < seen.length) {
            seen[i] = true;
        }
        if (elem == dtd.html) {
            seenHtml = true;
        } else if (elem == dtd.head) {
            seenHead = true;
        } else if (elem == dtd.body) {
            seenBody = true;
        }
    }

    /**
     * Reads a DTD declaration, between {@code <!} and {@code >}.
     *
     * @return the text read.
     */
    public String parseDTDMarkup() throws IOException {
        StringBuffer sb = new StringBuffer();
        while (ch != -1 && ch != '>') {
            sb.append((char) ch);
            advance();
        }
        if (ch == '>') {
            advance();
        }
        return sb.toString();
    }

    /** Reads declarations inside a marked section. */
    protected boolean parseMarkupDeclarations(StringBuffer strBuff) throws IOException {
        String s = strBuff.toString();
        return s.length() > 0;
    }

    /**
     * Parses everything that comes from the reader.
     *
     * <p>On finishing it closes whatever was left open, so that whoever listens receives a closed
     * tree even if the document was truncated.
     */
    public synchronized void parse(Reader in) throws IOException {
        this.in = in;
        ln = 1;
        pos = 0;
        textpos = 0;
        stack.removeAllElements();
        advance();

        ignoresSpace = true;
        while (ch != -1) {
            if (ch == '<') {
                readTag();
            } else {
                gatherText((char) ch);
                advance();
            }
        }
        flushText(true);
        while (!stack.isEmpty()) {
            endTag(true);
        }
        this.in = null;
    }

    // ---- character reading ----

    private void advance() throws IOException {
        ch = in.read();
        pos++;
        if (ch == '\n') {
            ln++;
        }
    }

    private void gatherText(char c) {
        if (textpos == text.length) {
            char[] mas = new char[text.length * 2];
            System.arraycopy(text, 0, mas, 0, text.length);
            text = mas;
        }
        text[textpos] = c;
        textpos++;
    }

    /**
     * Releases the gathered text, resolving entities and fixing the spaces.
     *
     * <h2>The rule about spaces</h2>
     *
     * <p>In HTML, consecutive spaces are worth one, and those left against a block's edge are worth
     * nothing. It is not a detail: without this rule, an HTML written with indentation would show
     * the indentation.
     *
     * <p>What makes the rule need to look ahead is the edge: a space at the end of the text is
     * discarded if what follows is a block, and kept if it is a tag that goes on the line. That is
     * why this method takes {@code trimTrailing}, which whoever read the tag already knows.
     *
     * <p>Inside a {@code <pre>} nothing is touched, which is exactly what it exists for.
     */
    private void flushText(boolean trimTrailing) {
        if (textpos == 0) {
            return;
        }
        char[] data = new char[textpos];
        System.arraycopy(text, 0, data, 0, textpos);
        textpos = 0;
        char[] resolved = resolveEntities(data);
        if (!inPreformatted()) {
            resolved = collapseSpaces(resolved, trimTrailing);
        }
        if (resolved.length == 0) {
            return;
        }
        closeWhatIsLeftOver(dtd.pcdata);
        openWhatIsMissing(dtd.pcdata);
        ignoresSpace = false;
        if (inTitle()) {
            handleTitle(resolved);
        } else {
            handleText(resolved);
        }
    }

    /** Collapses consecutive spaces and removes those at the edges that do not count. */
    private char[] collapseSpaces(char[] data, boolean trimTrailing) {
        StringBuilder sb = new StringBuilder(data.length);
        boolean spacePending = ignoresSpace;
        for (int i = 0; i < data.length; i++) {
            char c = data[i];
            if (isBlank(c)) {
                if (!spacePending) {
                    sb.append(' ');
                    spacePending = true;
                }
            } else {
                sb.append(c);
                spacePending = false;
            }
        }
        if (trimTrailing && sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }
        char[] out = new char[sb.length()];
        sb.getChars(0, sb.length(), out, 0);
        return out;
    }

    /** Whether the open element keeps the spaces as they are. */
    private boolean inPreformatted() {
        for (int i = 0; i < stack.size(); i++) {
            HTML.Tag t = HTML.getTag(stack.elementAt(i).getName());
            if (t != null && t.isPreformatted()) {
                return true;
            }
        }
        return false;
    }

    private boolean inTitle() {
        for (int i = 0; i < stack.size(); i++) {
            if ("title".equals(stack.elementAt(i).getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces the entities by their text.
     *
     * <p>An entity that is not known is left just as it was. It is what has to be done: a loose
     * <code>&amp;</code> is common in real HTML, and turning it into nothing would lose the
     * author's text.
     */
    private char[] resolveEntities(char[] data) {
        StringBuilder sb = new StringBuilder(data.length);
        int i = 0;
        while (i < data.length) {
            char c = data[i];
            if (c != '&') {
                sb.append(c);
                i++;
                continue;
            }
            int end = -1;
            for (int j = i + 1; j < data.length && j < i + 12; j++) {
                if (data[j] == ';') {
                    end = j;
                    break;
                }
                if (!Character.isLetterOrDigit(data[j]) && data[j] != '#') {
                    break;
                }
            }
            if (end < 0) {
                sb.append(c);
                i++;
                continue;
            }
            String name = new String(data, i + 1, end - i - 1);
            String value = entity(name);
            if (value == null) {
                sb.append(c);
                i++;
            } else {
                sb.append(value);
                i = end + 1;
            }
        }
        char[] out = new char[sb.length()];
        sb.getChars(0, sb.length(), out, 0);
        return out;
    }

    /** An entity's text by name or by number, or null if it is not known. */
    private String entity(String name) {
        if (name.length() == 0) {
            return null;
        }
        if (name.charAt(0) == '#') {
            try {
                int cod;
                if (name.length() > 2 && (name.charAt(1) == 'x' || name.charAt(1) == 'X')) {
                    cod = Integer.parseInt(name.substring(2), 16);
                } else {
                    cod = Integer.parseInt(name.substring(1));
                }
                return String.valueOf((char) cod);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        Entity e = dtd.getEntity(name);
        return (e == null) ? null : e.getString();
    }

    // ---- tag reading ----

    /**
     * Reads what starts with {@code <}: a tag, a closing, a comment or a declaration.
     *
     * <p>The name is read <em>before</em> releasing the pending text, because the text needs to
     * know whether what comes is a block in order to decide about its last space; see
     * {@link #flushText}.
     */
    private void readTag() throws IOException {
        advance();
        if (ch == '!') {
            advance();
            if (ch == '-') {
                advance();
                if (ch == '-') {
                    advance();
                    flushText(false);
                    readComment();
                    return;
                }
            }
            flushText(false);
            readDeclaration();
            return;
        }
        if (ch == '/') {
            advance();
            String name = readName();
            flushText(breaksLine(name));
            readClose(name);
            return;
        }
        if (ch == -1) {
            return;
        }
        if (!Character.isLetter((char) ch)) {
            // A `<` that opens nothing: it is reported and discarded. Leaving it as text would be
                        // friendlier and is not what any browser does -- nor the JDK.
            error("expected.tagname");
            return;
        }
        String name = readName();
        flushText(breaksLine(name));
        readOpen(name);
    }

    /** Whether that tag breaks the line, and then eats the space beside it. */
    private static boolean breaksLine(String name) {
        HTML.Tag t = HTML.getTag(name);
        return (t != null) && (t.isBlock() || t.breaksFlow());
    }

    private void readComment() throws IOException {
        StringBuilder sb = new StringBuilder();
        int dashes = 0;
        while (ch != -1) {
            if (ch == '-') {
                dashes++;
            } else if (ch == '>' && dashes >= 2) {
                advance();
                String s = sb.toString();
                // The closing's two dashes were left inside.
                if (s.length() >= 2) {
                    s = s.substring(0, s.length() - 2);
                }
                char[] data = new char[s.length()];
                s.getChars(0, s.length(), data, 0);
                handleComment(data);
                return;
            } else {
                dashes = 0;
            }
            sb.append((char) ch);
            advance();
        }
        handleEOFInComment();
    }

    /** A declaration such as {@code <!DOCTYPE ...>}; it is read and discarded. */
    private void readDeclaration() throws IOException {
        StringBuffer sb = new StringBuffer();
        while (ch != -1 && ch != '>') {
            sb.append((char) ch);
            advance();
        }
        if (ch == '>') {
            advance();
        }
        parseMarkupDeclarations(sb);
    }

    private void readClose(String name) throws IOException {
        skipUpTo('>');
        if (name.length() == 0) {
            return;
        }
        if (breaksLine(name)) {
            ignoresSpace = true;
        }
        Element elem = dtd.getElement(name);
        closeUpTo(elem);
    }

    /**
     * Closes up to that element, closing along the way whatever was left open.
     *
     * <p>If the element is not open nothing is done: an extra closing is a common error and closing
     * anything just in case would destroy the tree.
     */
    private void closeUpTo(Element elem) {
        int i = stack.size() - 1;
        while (i >= 0 && stack.elementAt(i) != elem) {
            i--;
        }
        if (i < 0) {
            error("unmatched.endtag", elem.getName());
            return;
        }
        while (stack.size() > i + 1) {
            endTag(true);
        }
        endTag(false);
    }

    private void readOpen(String name) throws IOException {
        // The attributes go to a separate set: between reading them and firing the tag there
        // may be implicit tags, and if they were already installed the first one would take
        // them.
        SimpleAttributeSet read = new SimpleAttributeSet();
        readAttributes(read, dtd.getElement(name));
        boolean selfClosing = false;
        if (ch == '/') {
            selfClosing = true;
            advance();
        }
        if (ch == '>') {
            advance();
        }
        if (name.length() == 0) {
            return;
        }
        Element elem = dtd.getElement(name);
        if (breaksLine(name)) {
            ignoresSpace = true;
        }
        closeWhatIsLeftOver(elem);
        openWhatIsMissing(elem);

        flushAttributes();
        attributes.addAttributes(read);
        TagElement tag = makeTag(elem);
        try {
            if (elem.isEmpty() || selfClosing) {
                handleEmptyTag(tag);
            } else {
                startTag(tag);
            }
        } catch (ChangedCharSetException cse) {
            // The encoding changed halfway; whoever called us decides what to do.
            throw new RuntimeException(cse.getMessage());
        }
    }

    /**
     * Closes the open elements that do not accept what is coming.
     *
     * <p>It is the first of the class note's three rules. Only what the DTD allows to close itself
     * is closed: if the closing cannot be omitted, the document is wrong and it is left as it is.
     */
    private void closeWhatIsLeftOver(Element elem) {
        while (!stack.isEmpty()) {
            Element top = stack.elementAt(stack.size() - 1);
            if (acceptsInside(top, elem)) {
                return;
            }
            if (!top.omitEnd()) {
                return;
            }
            endTag(true);
        }
    }

    /**
     * Whether the parent can carry that child inside.
     *
     * <p>It asks whether the child appears <em>anywhere</em> in the model, not whether it can go
     * first. Asking about the first one would be right in SGML and would be useless in HTML:
     * <code>&lt;html&gt;</code> has the model <code>(head, body, plaintext?)</code>, so
     * <code>&lt;body&gt;</code> cannot go first, and yet it does. The order within the sequence is
     * resolved by {@link #openWhatIsMissing}, which is where it matters.
     */
    private boolean acceptsInside(Element parent, Element child) {
        if (parent.exclusions != null && child.getIndex() < parent.exclusions.size()
                && parent.exclusions.get(child.getIndex())) {
            return false;
        }
        if (parent.inclusions != null && child.getIndex() < parent.inclusions.size()
                && parent.inclusions.get(child.getIndex())) {
            return true;
        }
        ContentModel m = parent.getContent();
        if (m == null) {
            return parent.getType() == ANY;
        }
        return contains(m, child);
    }

    /** Whether that element appears in the model, at any depth. */
    private boolean contains(ContentModel m, Element child) {
        Vector<Element> v = new Vector<Element>();
        m.getElements(v);
        return v.contains(child);
    }

    /**
     * Opens what that element is missing in order to be able to appear.
     *
     * <p>It is the second rule. An intermediate element is looked for whose opening may be omitted
     * and which accepts the one coming: it is what puts in the {@code <body>} of a page that starts
     * with text.
     */
    private void openWhatIsMissing(Element elem) {
        for (int round = 0; round < 8; round++) {
            Element top = stack.isEmpty() ? null : stack.elementAt(stack.size() - 1);
            if (top == null) {
                if (elem == dtd.html) {
                    return;
                }
                openImplicit(dtd.html);
                continue;
            }
            if (acceptsInside(top, elem)) {
                skipPast(top, elem);
                return;
            }
            Element intermediate = intermediateFor(top, elem);
            if (intermediate == null) {
                return;
            }
            // Before the intermediate one, other members of the sequence may have been skipped.
            skipPast(top, intermediate);
            openImplicit(intermediate);
        }
    }

    /**
     * Opens and closes the members of a sequence that were skipped.
     *
     * <p>{@code html}'s model is <code>(head, body, plaintext?)</code>. A document that starts with
     * <code>&lt;body&gt;</code> skipped the head, and the head exists all the same: it can be
     * opened and closed by itself. Without this step, the tree would have no {@code head} and
     * whoever looks for the title would not find it.
     *
     * <p>Only those that can be opened <em>and</em> closed by themselves are skipped over. One that
     * needs a written tag is not invented: if it is missing, the document is wrong and it is not
     * the parser's job to fix it.
     */
    private void skipPast(Element parent, Element child) {
        ContentModel m = parent.getContent();
        if (m == null || m.type != ',') {
            return;
        }
        for (ContentModel c = (ContentModel) m.content; c != null; c = c.next) {
            if (contains(c, child)) {
                return;
            }
            Element e = c.first();
            if (e == null || !e.omitStart() || !e.omitEnd() || alreadySeen(e)) {
                continue;
            }
            openImplicit(e);
            endTag(true);
        }
    }

    private boolean alreadySeen(Element e) {
        int i = e.getIndex();
        return (i >= 0 && i < seen.length && seen[i]);
    }

    /** An element that can go inside the parent, accept the child, and open itself. */
    private Element intermediateFor(Element parent, Element child) {
        ContentModel m = parent.getContent();
        if (m == null) {
            return null;
        }
        Vector<Element> candidates = new Vector<Element>();
        m.getElements(candidates);
        for (int i = 0; i < candidates.size(); i++) {
            Element c = candidates.elementAt(i);
            if (c == child || !c.omitStart()) {
                continue;
            }
            if (acceptsInside(c, child)) {
                return c;
            }
        }
        return null;
    }

    private void openImplicit(Element elem) {
        markFirstTime(elem);
        stack.addElement(elem);
        handleStartTag(makeTag(elem, true));
    }

    // ---- names and attributes ----

    private String readName() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (ch != -1 && (Character.isLetterOrDigit((char) ch) || ch == '-' || ch == '.'
                || ch == '_' || ch == ':')) {
            sb.append(Character.toLowerCase((char) ch));
            advance();
        }
        return sb.toString();
    }

    private void readAttributes(SimpleAttributeSet out, Element elem)
            throws IOException {
        while (true) {
            skipBlanks();
            if (ch == -1 || ch == '>' || ch == '/') {
                return;
            }
            String name = readName();
            if (name.length() == 0) {
                // A character that does not start a name: it is discarded so as not to end up
                // in a loop.
                advance();
                continue;
            }
            skipBlanks();
            String value = null;
            if (ch == '=') {
                advance();
                skipBlanks();
                value = readValue();
            }
            storeAttribute(out, elem, name, value);
        }
    }

    /**
     * Stores an attribute under whatever key corresponds to it.
     *
     * <p>If it is one of the known ones, the key is the {@link HTML.Attribute} constant; if not,
     * the name as a string. That way the rest of the library can compare by identity those that
     * matter to it without losing those it does not know.
     *
     * <p>An attribute written with no value takes the special value
     * {@link HTML#NULL_ATTRIBUTE_VALUE}, not null: storing null would be indistinguishable from not
     * having it.
     */
    private void storeAttribute(SimpleAttributeSet out, Element elem, String name,
            String value) {
        if (value == null && elem != null) {
            // SGML's short form: `<ul compact>` is `<ul compact="compact">`. It is recognized
            // by looking in the DTD for an attribute that has that name among its allowed
            // values, which is exactly what getAttributeByValue exists for.
            AttributeList a = elem.getAttributeByValue(name);
            if (a != null) {
                name = a.getName();
                value = name;
            }
        }
        Object key = HTML.getAttributeKey(name);
        if (key == null) {
            key = name;
        }
        out.addAttribute(key, (value == null) ? HTML.NULL_ATTRIBUTE_VALUE : value);
    }

    private String readValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        if (ch == '"' || ch == '\'') {
            int quote = ch;
            advance();
            while (ch != -1 && ch != quote) {
                sb.append((char) ch);
                advance();
            }
            if (ch == quote) {
                advance();
            }
        } else {
            while (ch != -1 && !isBlank(ch) && ch != '>') {
                sb.append((char) ch);
                advance();
            }
        }
        char[] data = new char[sb.length()];
        sb.getChars(0, sb.length(), data, 0);
        return new String(resolveEntities(data));
    }

    private static boolean isBlank(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private void skipBlanks() throws IOException {
        while (ch != -1 && isBlank(ch)) {
            advance();
        }
    }

    private void skipUpTo(char c) throws IOException {
        while (ch != -1 && ch != c) {
            advance();
        }
        if (ch == c) {
            advance();
        }
    }
}
