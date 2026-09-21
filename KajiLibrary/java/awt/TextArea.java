package java.awt;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * A rectangle of several lines to type in.
 *
 * <p>Unlike {@link TextField}, Enter puts in a line break instead of firing an event: there is no
 * way to "confirm" a text area, and that is why it has no action listeners.
 *
 * <p>The scrollbars are chosen in the constructor and **cannot be changed afterwards**;
 * {@link #getScrollbarVisibility} only reports what was asked for. It is a real limitation of AWT.
 */
public class TextArea extends TextComponent {

    private static final long serialVersionUID = 3692302836626095722L;

    private static int textAreaCounter = 0;

    /** How many lines tall it asks to be. */
    int rows;

    /** How many letters wide it asks to be. */
    int columns;

    /** Both bars. */
    public static final int SCROLLBARS_BOTH = 0;

    /** The vertical one only. */
    public static final int SCROLLBARS_VERTICAL_ONLY = 1;

    /** The horizontal one only. */
    public static final int SCROLLBARS_HORIZONTAL_ONLY = 2;

    /** Neither of them. */
    public static final int SCROLLBARS_NONE = 3;

    /** Which ones were asked for. */
    private int scrollbarVisibility;

    /** An empty area with both bars. */
    public TextArea() throws HeadlessException {
        this("", 0, 0, SCROLLBARS_BOTH);
    }

    /** An area with that text and both bars. */
    public TextArea(String text) throws HeadlessException {
        this(text, 0, 0, SCROLLBARS_BOTH);
    }

    /** An empty area of that size, with both bars. */
    public TextArea(int rows, int columns) throws HeadlessException {
        this("", rows, columns, SCROLLBARS_BOTH);
    }

    /** An area with that text and that size, with both bars. */
    public TextArea(String text, int rows, int columns) throws HeadlessException {
        this(text, rows, columns, SCROLLBARS_BOTH);
    }

    /**
     * An area with everything given.
     *
     * <p>A scrollbar value that is not one of the four constants is taken as {@link
     * #SCROLLBARS_BOTH}, which is what the JDK does: it is a request about appearance, not
     * something that justifies an exception.
     */
    public TextArea(String text, int rows, int columns, int scrollbars) throws HeadlessException {
        super(text);
        this.rows = Math.max(0, rows);
        this.columns = Math.max(0, columns);
        if (scrollbars >= SCROLLBARS_BOTH && scrollbars <= SCROLLBARS_NONE) {
            this.scrollbarVisibility = scrollbars;
        } else {
            this.scrollbarVisibility = SCROLLBARS_BOTH;
        }
    }

    String constructComponentName() {
        synchronized (TextArea.class) {
            String n = "text" + textAreaCounter;
            textAreaCounter = textAreaCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * Puts that text at that position.
     *
     * @throws StringIndexOutOfBoundsException if the position falls outside the text
     */
    public void insert(String str, int pos) {
        this.insertText(str, pos);
    }

    /**
     * Puts text at that position.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #insert}.
     */
    @Deprecated
    public synchronized void insertText(String str, int pos) {
        String t = this.getText();
        this.setText(t.substring(0, pos) + str + t.substring(pos));
    }

    /** Sticks that text at the end. */
    public void append(String str) {
        this.appendText(str);
    }

    /**
     * Sticks text at the end.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #append}.
     */
    @Deprecated
    public synchronized void appendText(String str) {
        this.insertText(str, this.getText().length());
    }

    /**
     * Replaces that stretch with that text.
     *
     * <p>It does **not** clamp or reorder the positions, unlike {@link TextComponent#select}: a
     * stretch that is reversed or past the text throws. There the tolerance makes sense —the
     * selection is worked out by a search— and here it does not: replacing the wrong stretch
     * silently is worse than replacing nothing.
     *
     * @throws StringIndexOutOfBoundsException if the stretch is not valid
     */
    public void replaceRange(String str, int start, int end) {
        this.replaceText(str, start, end);
    }

    /**
     * Replaces a stretch.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #replaceRange}.
     */
    @Deprecated
    public synchronized void replaceText(String str, int start, int end) {
        String t = this.getText();
        this.setText(t.substring(0, start) + str + t.substring(end));
    }

    /** How many lines tall it asks to be. */
    public int getRows() {
        return this.rows;
    }

    /**
     * Changes the height it asks for.
     *
     * @throws IllegalArgumentException if it is negative
     */
    public void setRows(int rows) {
        if (rows < 0) {
            throw new IllegalArgumentException("rows less than zero.");
        }
        this.rows = rows;
    }

    /** How many letters wide it asks to be. */
    public int getColumns() {
        return this.columns;
    }

    /**
     * Changes the width it asks for.
     *
     * @throws IllegalArgumentException if it is negative
     */
    public void setColumns(int columns) {
        if (columns < 0) {
            throw new IllegalArgumentException("columns less than zero.");
        }
        this.columns = columns;
    }

    /** Which bars were asked for when it was built. */
    public int getScrollbarVisibility() {
        return this.scrollbarVisibility;
    }

    /**
     * What an area of that size would need.
     *
     * <p>It answers the current size and ignores the rows and the columns: working out what a given
     * number of letters measures needs the font measured on a screen.
     */
    public Dimension getPreferredSize(int rows, int columns) {
        return this.getSize();
    }

    /**
     * What an area of that size would need.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getPreferredSize(int, int)}.
     */
    @Deprecated
    public Dimension preferredSize(int rows, int columns) {
        return this.getPreferredSize(rows, columns);
    }

    public Dimension getPreferredSize() {
        if (this.rows > 0 && this.columns > 0) {
            return this.getPreferredSize(this.rows, this.columns);
        }
        return super.getPreferredSize();
    }

    /**
     * What it needs.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getPreferredSize()}.
     */
    @Deprecated
    public Dimension preferredSize() {
        return this.getPreferredSize();
    }

    /**
     * The minimum for an area of that size.
     *
     * <p>Like {@link #getPreferredSize(int, int)}, it answers the current size and ignores the rows
     * and the columns.
     */
    public Dimension getMinimumSize(int rows, int columns) {
        return this.getSize();
    }

    /**
     * The minimum for that size.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getMinimumSize(int, int)}.
     */
    @Deprecated
    public Dimension minimumSize(int rows, int columns) {
        return this.getMinimumSize(rows, columns);
    }

    public Dimension getMinimumSize() {
        if (this.rows > 0 && this.columns > 0) {
            return this.getMinimumSize(this.rows, this.columns);
        }
        return super.getMinimumSize();
    }

    /**
     * The minimum it needs.
     *
     * @deprecated it is from the 1.0 naming. Use {@link #getMinimumSize()}.
     */
    @Deprecated
    public Dimension minimumSize() {
        return this.getMinimumSize();
    }

    protected String paramString() {
        String bars = "both";
        if (this.scrollbarVisibility == SCROLLBARS_VERTICAL_ONLY) {
            bars = "vertical";
        } else if (this.scrollbarVisibility == SCROLLBARS_HORIZONTAL_ONLY) {
            bars = "horizontal";
        } else if (this.scrollbarVisibility == SCROLLBARS_NONE) {
            bars = "none";
        }
        return super.paramString() + ",rows=" + this.rows + ",columns=" + this.columns
                + ",scrollbarVisibility=" + bars;
    }

    /** The accessibility information of this area. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTTextArea();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a text area.
     *
     * <p>It reports `MULTI_LINE` instead of `SINGLE_LINE`, which is the difference that matters to
     * a screen reader: it tells it to offer navigation by line.
     */
    protected class AccessibleAWTTextArea extends AccessibleAWTTextComponent {

        /** For the subclasses. */
        protected AccessibleAWTTextArea() {
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            s.remove(AccessibleState.SINGLE_LINE);
            s.add(AccessibleState.MULTI_LINE);
            return s;
        }
    }
}
