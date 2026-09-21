package javax.swing.text;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

/**
 * The base of those that write a document out as text.
 *
 * <h2>What it solves, and what it does not</h2>
 *
 * <p>It knows nothing about the output format: that is put in by {@link #write()}, which each
 * subclass writes. What it brings done is all the boring things that still have to be done
 * right: walking the elements of the requested range, indenting, breaking lines at the maximum
 * width without splitting words, and getting the text out of a leaf element.
 *
 * <h2>The line break</h2>
 *
 * <p>When breaking is on, every write looks backwards for a space and breaks there. If there is
 * no space at all, the line runs over: splitting a word in the middle would be worse than a long
 * line, because it would change the content.
 *
 * <p>The indentation is written only when there is something to put on the line. Writing it
 * before would leave lines with spaces and nothing else.
 */
public abstract class AbstractWriter {

    private ElementIterator it;
    private Writer out;
    private int indentLevel = 0;
    private int indentSpace = 2;
    private Document doc = null;
    private int maxLineLength = 100;
    private int currLength = 0;
    private int startOffset = 0;
    private int endOffset = 0;
    private int offsetIndent = 0;

    private String lineSeparator;
    private boolean canWrapLines;
    private boolean isLineEmpty;
    private char[] indentChars;
    private char[] tempChars;
    private char[] newlineChars;
    private Segment segment;

    /** The line ending used if nobody asks for another. */
    protected static final char NEWLINE = '\n';

    /** It writes the whole document. */
    protected AbstractWriter(Writer w, Document doc) {
        this(w, doc, 0, doc.getLength());
    }

    /** It writes the requested stretch of the document. */
    protected AbstractWriter(Writer w, Document doc, int pos, int len) {
        this.doc = doc;
        it = new ElementIterator(doc.getDefaultRootElement());
        out = w;
        startOffset = pos;
        endOffset = pos + len;
        Object docNewline = doc.getProperty(DefaultEditorKit.EndOfLineStringProperty);
        if (docNewline instanceof String) {
            setLineSeparator((String) docNewline);
        } else {
            String newline = null;
            try {
                newline = System.getProperty("line.separator");
            } catch (SecurityException se) {
                // With no permission to read it: the usual one is used.
            }
            if (newline == null) {
                newline = "\n";
            }
            setLineSeparator(newline);
        }
        canWrapLines = true;
    }

    /** It writes the tree hanging from that element. */
    protected AbstractWriter(Writer w, Element root) {
        this(w, root, 0, root.getEndOffset());
    }

    /** It writes the requested stretch of that element's tree. */
    protected AbstractWriter(Writer w, Element root, int pos, int len) {
        this.doc = root.getDocument();
        it = new ElementIterator(root);
        out = w;
        startOffset = pos;
        endOffset = pos + len;
        canWrapLines = true;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    protected ElementIterator getElementIterator() {
        return it;
    }

    protected Writer getWriter() {
        return out;
    }

    protected Document getDocument() {
        return doc;
    }

    /** Whether that element overlaps the stretch being written. */
    protected boolean inRange(Element next) {
        int startOffset = getStartOffset();
        int endOffset = getEndOffset();
        if ((next.getStartOffset() >= startOffset && next.getStartOffset() < endOffset)
                || (startOffset >= next.getStartOffset()
                        && startOffset < next.getEndOffset())) {
            return true;
        }
        return false;
    }

    /** What the subclass does: write the document in its format. */
    protected abstract void write() throws IOException, BadLocationException;

    /** That element's text, trimmed to the requested stretch. */
    protected String getText(Element elem) throws BadLocationException {
        return doc.getText(elem.getStartOffset(), elem.getEndOffset() - elem.getStartOffset());
    }

    /**
     * It writes a leaf element's text, trimmed to the stretch.
     *
     * <p>Trimming here and not in {@link #getText} is what allows writing half a leaf when the
     * stretch starts or ends in the middle of one.
     */
    protected void text(Element elem) throws BadLocationException, IOException {
        int start = Math.max(getStartOffset(), elem.getStartOffset());
        int end = Math.min(getEndOffset(), elem.getEndOffset());
        if (start < end) {
            if (segment == null) {
                segment = new Segment();
            }
            getDocument().getText(start, end - start, segment);
            if (segment.count > 0) {
                write(segment.array, segment.offset, segment.count);
            }
        }
    }

    /** The maximum line width; see the class note. */
    protected void setLineLength(int l) {
        maxLineLength = l;
    }

    protected int getLineLength() {
        return maxLineLength;
    }

    protected void setCurrentLineLength(int length) {
        currLength = length;
        isLineEmpty = (currLength == 0);
    }

    protected int getCurrentLineLength() {
        return currLength;
    }

    /** Whether nothing has been written on this line yet (the indentation does not count). */
    protected boolean isLineEmpty() {
        return isLineEmpty;
    }

    protected void setCanWrapLines(boolean newValue) {
        canWrapLines = newValue;
    }

    protected boolean getCanWrapLines() {
        return canWrapLines;
    }

    /** How many spaces one level of indentation is worth. */
    protected void setIndentSpace(int space) {
        indentSpace = space;
    }

    protected int getIndentSpace() {
        return indentSpace;
    }

    public void setLineSeparator(String value) {
        lineSeparator = value;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    protected void incrIndent() {
        // The level goes up unless the indentation already exceeds the line width.
        if (offsetIndent > 0 || ((indentLevel + 1) * getIndentSpace()) < getLineLength()) {
            indentLevel++;
        } else {
            offsetIndent++;
        }
    }

    protected void decrIndent() {
        if (offsetIndent > 0) {
            offsetIndent--;
        } else {
            indentLevel--;
        }
    }

    protected int getIndentLevel() {
        return indentLevel;
    }

    /** It writes the indentation of the coming line. */
    protected void indent() throws IOException {
        int max = getIndentLevel() * getIndentSpace();
        if (indentChars == null || max > indentChars.length) {
            indentChars = new char[max];
            for (int counter = 0; counter < max; counter++) {
                indentChars[counter] = ' ';
            }
        }
        int length = getCurrentLineLength();
        boolean wasEmpty = isLineEmpty();
        output(indentChars, 0, max);
        if (wasEmpty && length == 0) {
            isLineEmpty = true;
        }
    }

    protected void write(char ch) throws IOException {
        if (tempChars == null) {
            tempChars = new char[128];
        }
        tempChars[0] = ch;
        write(tempChars, 0, 1);
    }

    protected void write(String content) throws IOException {
        if (content == null) {
            return;
        }
        int size = content.length();
        if (tempChars == null || tempChars.length < size) {
            tempChars = new char[size];
        }
        content.getChars(0, size, tempChars, 0);
        write(tempChars, 0, size);
    }

    protected void writeLineSeparator() throws IOException {
        String newline = getLineSeparator();
        int length = newline.length();
        if (newlineChars == null || newlineChars.length < length) {
            newlineChars = new char[length];
        }
        newline.getChars(0, length, newlineChars, 0);
        output(newlineChars, 0, length);
        setCurrentLineLength(0);
    }

    /**
     * It writes that text, breaking lines if needed.
     *
     * <p>Every write goes through here: it is where the breaking lives and where the length is
     * counted. What really goes to the {@link Writer} goes out through {@link #output}.
     */
    protected void write(char[] chars, int startIndex, int length) throws IOException {
        if (!getCanWrapLines()) {
            // Without breaking: only the line ending is looked for so as not to lose the count.
            int lastIndex = startIndex;
            int endIndex = startIndex + length;
            int newlineIndex = indexOf(chars, NEWLINE, startIndex, endIndex);
            while (newlineIndex != -1) {
                if (newlineIndex > lastIndex) {
                    output(chars, lastIndex, newlineIndex - lastIndex);
                }
                writeLineSeparator();
                lastIndex = newlineIndex + 1;
                newlineIndex = indexOf(chars, NEWLINE, lastIndex, endIndex);
            }
            if (lastIndex < endIndex) {
                output(chars, lastIndex, endIndex - lastIndex);
            }
        } else {
            int lastIndex = startIndex;
            int endIndex = startIndex + length;
            int lineLength = getCurrentLineLength();
            int maxLength = getLineLength();

            while (lastIndex < endIndex) {
                int newlineIndex = indexOf(chars, NEWLINE, lastIndex, endIndex);
                boolean needsNewline = false;
                boolean forceNewLine = false;

                lineLength = getCurrentLineLength();
                if (newlineIndex != -1 && (lineLength + (newlineIndex - lastIndex))
                        < maxLength) {
                    if (newlineIndex > lastIndex) {
                        output(chars, lastIndex, newlineIndex - lastIndex);
                    }
                    lastIndex = newlineIndex + 1;
                    forceNewLine = true;
                } else if (newlineIndex == -1 && (lineLength + (endIndex - lastIndex))
                        < maxLength) {
                    if (endIndex > lastIndex) {
                        output(chars, lastIndex, endIndex - lastIndex);
                    }
                    lastIndex = endIndex;
                } else {
                    // It does not fit: a space is looked for backwards.
                    int breakPoint = -1;
                    int maxBreak = Math.min(endIndex - lastIndex,
                            maxLength - lineLength - 1);
                    int counter;
                    for (counter = 0; counter < maxBreak; counter++) {
                        if (Character.isWhitespace(chars[counter + lastIndex])) {
                            breakPoint = counter;
                        }
                    }
                    if (breakPoint != -1) {
                        breakPoint = breakPoint + lastIndex + 1;
                        output(chars, lastIndex, breakPoint - lastIndex);
                        lastIndex = breakPoint;
                    } else {
                        // Not even a space: the line runs over. See the class note.
                        boolean done = false;
                        int check = lastIndex + maxBreak;
                        maxBreak = endIndex;
                        for (counter = check; counter < maxBreak; counter++) {
                            if (Character.isWhitespace(chars[counter])) {
                                breakPoint = counter;
                                done = true;
                                break;
                            }
                        }
                        if (!done) {
                            output(chars, lastIndex, endIndex - lastIndex);
                            lastIndex = endIndex;
                        } else {
                            if (breakPoint < endIndex && chars[breakPoint] == NEWLINE) {
                                output(chars, lastIndex, breakPoint - lastIndex);
                                forceNewLine = true;
                                lastIndex = breakPoint + 1;
                            } else {
                                output(chars, lastIndex, breakPoint - lastIndex + 1);
                                lastIndex = breakPoint + 1;
                            }
                        }
                    }
                    needsNewline = true;
                }
                if (forceNewLine || needsNewline) {
                    writeLineSeparator();
                    if (lastIndex < endIndex) {
                        indent();
                    }
                }
            }
        }
    }

    /** It writes that set's attributes, one per line. */
    protected void writeAttributes(AttributeSet attr) throws IOException {
        Enumeration<?> names = attr.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            write(" " + name + "=" + attr.getAttribute(name));
        }
    }

    /**
     * The only real outlet to the {@link Writer}.
     *
     * <p>A subclass that wants to spy on or change what goes out only has to override this,
     * without repeating {@link #write(char[], int, int)}'s breaking logic.
     */
    protected void output(char[] content, int start, int length) throws IOException {
        getWriter().write(content, start, length);
        setCurrentLineLength(getCurrentLineLength() + length);
    }

    private static int indexOf(char[] chars, char sChar, int start, int end) {
        while (start < end) {
            if (chars[start] == sChar) {
                return start;
            }
            start++;
        }
        return -1;
    }
}
