package javax.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.accessibility.AccessibleContext;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * A multi-line text area, with no styles.
 *
 * <h2>Rows and columns are not a size</h2>
 *
 * <p>{@link #setRows} and {@link #setColumns} do not fix the size: they fix the
 * <em>preferred</em> one. The area may end up larger or smaller according to the layout that
 * contains it. It is the commonest source of confusion with this class: asking for twenty
 * columns and seeing ten.
 *
 * <p>A column is the width of the letter <code>m</code> in the current typeface. It is not an
 * exact measurement save with a fixed-width typeface, and that is why a text area is almost
 * always used with one.
 *
 * <h2>The line wrapping belongs to the view, not to the document</h2>
 *
 * <p>With {@link #setLineWrap} switched on, a long line is seen cut into several. The document
 * does not change: there is no new line end, and {@link #getLineCount} goes on counting the
 * real ones. It is what is wanted -- keeping the text just as it was typed -- and what
 * surprises when counting lines.
 *
 * <h2>Nor does it scroll by itself</h2>
 *
 * <p>Like {@link JList}: it has to be put into a {@link JScrollPane}. An area with no scroller
 * grows with the text until it overflows whatever contains it.
 */
public class JTextArea extends JTextComponent {

    private static final String uiClassID = "TextAreaUI";

    private int rows;
    private int columns;
    private int columnWidth;
    private int rowHeight;
    private boolean wordWrap;
    private boolean wrap;
    private AccessibleContext accessibleContext;

    /** An empty area. */
    public JTextArea() {
        this(null, null, 0, 0);
    }

    /** An area with that text. */
    public JTextArea(String text) {
        this(null, text, 0, 0);
    }

    /** An area of that preferred size; see the class note. */
    public JTextArea(int rows, int columns) {
        this(null, null, rows, columns);
    }

    /** An area with that text and that preferred size. */
    public JTextArea(String text, int rows, int columns) {
        this(null, text, rows, columns);
    }

    /** An area over that document. */
    public JTextArea(Document doc) {
        this(doc, null, 0, 0);
    }

    /**
     * An area over that document, with that text and that size.
     *
     * @throws IllegalArgumentException if the rows or the columns are negative.
     */
    public JTextArea(Document doc, String text, int rows, int columns) {
        super();
        this.rows = rows;
        this.columns = columns;
        if (doc == null) {
            doc = createDefaultModel();
        }
        setDocument(doc);
        if (text != null) {
            setText(text);
            select(0, 0);
        }
        if (rows < 0) {
            throw new IllegalArgumentException("rows: " + rows);
        }
        if (columns < 0) {
            throw new IllegalArgumentException("columns: " + columns);
        }
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** A plain text document; it is all an area needs. */
    protected Document createDefaultModel() {
        return new PlainDocument();
    }

    /**
     * Every how many columns a tab stop falls.
     *
     * <p>It is kept as a property of the document, not of the area: who draws the text is the
     * view, and the view looks at the document.
     */
    public void setTabSize(int size) {
        Document doc = getDocument();
        if (doc != null) {
            int old = getTabSize();
            doc.putProperty(PlainDocument.tabSizeAttribute, Integer.valueOf(size));
            firePropertyChange("tabSize", old, size);
        }
    }

    public int getTabSize() {
        int size = 8;
        Document doc = getDocument();
        if (doc != null) {
            Integer i = (Integer) doc.getProperty(PlainDocument.tabSizeAttribute);
            if (i != null) {
                size = i.intValue();
            }
        }
        return size;
    }

    /** Whether long lines are seen cut; see the class note. */
    public void setLineWrap(boolean wrap) {
        boolean old = this.wrap;
        this.wrap = wrap;
        firePropertyChange("lineWrap", old, wrap);
    }

    public boolean getLineWrap() {
        return wrap;
    }

    /** Whether the cutting respects the words or splits wherever it falls. */
    public void setWrapStyleWord(boolean word) {
        boolean old = this.wordWrap;
        this.wordWrap = word;
        firePropertyChange("wrapStyleWord", old, word);
    }

    public boolean getWrapStyleWord() {
        return wordWrap;
    }

    /**
     * Which line that position falls in.
     *
     * @throws BadLocationException if the position does not exist.
     */
    public int getLineOfOffset(int offset) throws BadLocationException {
        Document doc = getDocument();
        if (offset < 0) {
            throw new BadLocationException("Can't translate offset to line", -1);
        }
        if (offset > doc.getLength()) {
            throw new BadLocationException("Can't translate offset to line",
                    doc.getLength() + 1);
        }
        Element map = getDocument().getDefaultRootElement();
        return map.getElementIndex(offset);
    }

    /** How many lines the text has; the real ones, not those that are seen. */
    public int getLineCount() {
        Element map = getDocument().getDefaultRootElement();
        return map.getElementCount();
    }

    /**
     * Where that line starts.
     *
     * @throws BadLocationException if the line does not exist.
     */
    public int getLineStartOffset(int line) throws BadLocationException {
        int lineCount = getLineCount();
        if (line < 0) {
            throw new BadLocationException("Negative line", -1);
        }
        if (line >= lineCount) {
            throw new BadLocationException("No such line", getDocument().getLength() + 1);
        }
        Element map = getDocument().getDefaultRootElement();
        Element lineElem = map.getElement(line);
        return lineElem.getStartOffset();
    }

    /**
     * Where that line ends, counting the line end.
     *
     * @throws BadLocationException if the line does not exist.
     */
    public int getLineEndOffset(int line) throws BadLocationException {
        int lineCount = getLineCount();
        if (line < 0) {
            throw new BadLocationException("Negative line", -1);
        }
        if (line >= lineCount) {
            throw new BadLocationException("No such line", getDocument().getLength() + 1);
        }
        Element map = getDocument().getDefaultRootElement();
        Element lineElem = map.getElement(line);
        int endOffset = lineElem.getEndOffset();
        // The last line has no line end to discount.
        return ((line == lineCount - 1) ? (endOffset - 1) : endOffset);
    }

    /** It puts text in at that position. */
    public void insert(String str, int pos) {
        Document doc = getDocument();
        if (doc != null) {
            try {
                doc.insertString(pos, str, null);
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    /** It adds text at the end. */
    public void append(String str) {
        Document doc = getDocument();
        if (doc != null) {
            try {
                doc.insertString(doc.getLength(), str, null);
            } catch (BadLocationException e) {
                // The document cannot be shorter than its own length.
            }
        }
    }

    /**
     * It replaces the text between those two positions.
     *
     * @throws IllegalArgumentException if the end is before the start.
     */
    public void replaceRange(String str, int start, int end) {
        if (end < start) {
            throw new IllegalArgumentException("end before start");
        }
        Document doc = getDocument();
        if (doc != null) {
            try {
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).replace(start, end - start, str,
                            null);
                } else {
                    doc.remove(start, end - start);
                    if (str != null && str.length() > 0) {
                        doc.insertString(start, str, null);
                    }
                }
            } catch (BadLocationException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
    }

    public int getRows() {
        return rows;
    }

    /**
     * How many rows are preferred.
     *
     * @throws IllegalArgumentException if it is negative.
     */
    public void setRows(int rows) {
        int oldVal = this.rows;
        if (rows < 0) {
            throw new IllegalArgumentException("rows less than zero.");
        }
        if (rows != oldVal) {
            this.rows = rows;
            invalidate();
        }
    }

    /** A row's height: the typeface's. */
    protected int getRowHeight() {
        if (rowHeight == 0) {
            FontMetrics metrics = getFontMetrics(getFont());
            rowHeight = metrics.getHeight();
        }
        return rowHeight;
    }

    public int getColumns() {
        return columns;
    }

    /**
     * How many columns are preferred.
     *
     * @throws IllegalArgumentException if it is negative.
     */
    public void setColumns(int columns) {
        int oldVal = this.columns;
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        if (columns != oldVal) {
            this.columns = columns;
            invalidate();
        }
    }

    /** A column's width: that of the letter {@code m}; see the class note. */
    protected int getColumnWidth() {
        if (columnWidth == 0) {
            FontMetrics metrics = getFontMetrics(getFont());
            columnWidth = metrics.charWidth('m');
        }
        return columnWidth;
    }

    /**
     * The preferred size.
     *
     * <p>It is the one the text asks for, but never less than the rows and columns asked for.
     * Hence an empty area with twenty columns already takes up room.
     */
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d = (d == null) ? new Dimension(400, 400) : d;
        Insets insets = getInsets();

        if (columns != 0) {
            d.width = Math.max(d.width, columns * getColumnWidth() + insets.left + insets.right);
        }
        if (rows != 0) {
            d.height = Math.max(d.height, rows * getRowHeight() + insets.top + insets.bottom);
        }
        return d;
    }

    /** Changing the typeface invalidates the measured height and width. */
    public void setFont(Font f) {
        super.setFont(f);
        rowHeight = 0;
        columnWidth = 0;
    }

    protected String paramString() {
        return super.paramString();
    }

    /**
     * Whether the area stretches to the scroller's width.
     *
     * <p>With line wrapping yes: wrapping at the scroller's width is precisely what was asked for.
     * Without wrapping no, because then the horizontal bar makes sense.
     */
    public boolean getScrollableTracksViewportWidth() {
        return (wrap) ? true : super.getScrollableTracksViewportWidth();
    }

    /** How much to ask the scroller for: the preferred rows and columns. */
    public Dimension getPreferredScrollableViewportSize() {
        Dimension size = super.getPreferredScrollableViewportSize();
        size = (size == null) ? new Dimension(400, 400) : size;
        Insets insets = getInsets();
        size.width = (columns == 0) ? size.width
                : columns * getColumnWidth() + insets.left + insets.right;
        size.height = (rows == 0) ? size.height
                : rows * getRowHeight() + insets.top + insets.bottom;
        return size;
    }

    /** How much the wheel advances by: one row or one column. */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return getRowHeight();
        }
        if (orientation == SwingConstants.HORIZONTAL) {
            return getColumnWidth();
        }
        throw new IllegalArgumentException("Invalid orientation: " + orientation);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
