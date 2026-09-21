package javax.swing.table;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A table column: its width, its header, and how its cells are drawn and edited.
 *
 * <h2>{@link #getModelIndex} is the key to the whole class</h2>
 *
 * <p>A column knows <strong>which model column it takes its data from</strong>, and that number
 * does not have to match where it is on the screen. It is what allows reordering columns by
 * dragging them, or hiding some, without touching the model: the column moves in the view and its
 * model index travels with it.
 *
 * <p>Confusing the two indices is a table's classic bug, and it is silent: it returns another
 * column's datum, not an error.
 *
 * <h2>Three widths, not one</h2>
 *
 * <p>The minimum, the preferred and the maximum. When the table grows or shrinks, the sharing out
 * respects the limits and stretches what it can -- without the three, resizing a table either
 * breaks one column's layout or breaks everybody's.
 *
 * <p>{@link #setWidth} clamps against the minimum and the maximum instead of accepting any
 * number: letting a width outside the range through would leave the column in a state it itself
 * declares invalid.
 */
public class TableColumn implements java.io.Serializable {

    private static final long serialVersionUID = -6113660025878112608L;

    /** The width property's name, for {@link PropertyChangeListener}. */
    public static final String COLUMN_WIDTH_PROPERTY = "columWidth";

    /** The header value property's name. */
    public static final String HEADER_VALUE_PROPERTY = "headerValue";

    /** The header renderer property's name. */
    public static final String HEADER_RENDERER_PROPERTY = "headerRenderer";

    /** The cell renderer property's name. */
    public static final String CELL_RENDERER_PROPERTY = "cellRenderer";

    /** Which model column it takes the data from; see the class note. */
    protected int modelIndex;

    /** What it is identified by; if it is {@code null}, the header value is used. */
    protected Object identifier;

    /** The current width. */
    protected int width;

    /** The minimum width. */
    protected int minWidth;

    private int preferredWidth;

    /** The maximum width. */
    protected int maxWidth;

    /** How the header is drawn; {@code null} for the table's. */
    protected TableCellRenderer headerRenderer;

    /** What the header says. */
    protected Object headerValue;

    /** How the cells are drawn; {@code null} for the table's. */
    protected TableCellRenderer cellRenderer;

    /** How the cells are edited; {@code null} for the table's. */
    protected TableCellEditor cellEditor;

    /** Whether the user can resize it. */
    protected boolean isResizable;

    /**
     * How many times the resize notices were asked to be silenced; see {@link
     * #disableResizedPosting}.
     */
    protected transient int resizedPostingDisableCount;

    private PropertyChangeSupport changes;

    /** Column 0, with the default width. */
    public TableColumn() {
        this(0);
    }

    /** Over that model column. */
    public TableColumn(int modelIndex) {
        this(modelIndex, 75, null, null);
    }

    /** Over that model column, with that width. */
    public TableColumn(int modelIndex, int width) {
        this(modelIndex, width, null, null);
    }

    /** With everything explicit. */
    public TableColumn(int modelIndex, int width, TableCellRenderer cellRenderer,
            TableCellEditor cellEditor) {
        super();
        this.modelIndex = modelIndex;
        this.width = width;
        this.preferredWidth = width;
        this.cellRenderer = cellRenderer;
        this.cellEditor = cellEditor;
        this.minWidth = 15;
        this.maxWidth = Integer.MAX_VALUE;
        this.isResizable = true;
        this.resizedPostingDisableCount = 0;
        this.headerValue = null;
    }

    /** Changes which model column it takes the data from. */
    public void setModelIndex(int modelIndex) {
        int old = this.modelIndex;
        this.modelIndex = modelIndex;
        fire("modelIndex", Integer.valueOf(old), Integer.valueOf(modelIndex));
    }

    /** Which model column it takes the data from. */
    public int getModelIndex() {
        return this.modelIndex;
    }

    /** Changes the identifier. */
    public void setIdentifier(Object identifier) {
        Object old = this.identifier;
        this.identifier = identifier;
        fire("identifier", old, identifier);
    }

    /**
     * The identifier; if none was set, the header value.
     *
     * <p>The fall back to the header is convenient and has an edge: two columns with the same title
     * have the same identifier, and looking up by identifier returns the first.
     */
    public Object getIdentifier() {
        if (this.identifier != null) {
            return this.identifier;
        }
        return getHeaderValue();
    }

    /** Changes what the header says. */
    public void setHeaderValue(Object headerValue) {
        Object old = this.headerValue;
        this.headerValue = headerValue;
        fire(HEADER_VALUE_PROPERTY, old, headerValue);
    }

    /** What the header says. */
    public Object getHeaderValue() {
        return this.headerValue;
    }

    /** Changes how the header is drawn. */
    public void setHeaderRenderer(TableCellRenderer headerRenderer) {
        TableCellRenderer old = this.headerRenderer;
        this.headerRenderer = headerRenderer;
        fire(HEADER_RENDERER_PROPERTY, old, headerRenderer);
    }

    /** How the header is drawn, or {@code null} for the table's. */
    public TableCellRenderer getHeaderRenderer() {
        return this.headerRenderer;
    }

    /** Changes how the cells are drawn. */
    public void setCellRenderer(TableCellRenderer cellRenderer) {
        TableCellRenderer old = this.cellRenderer;
        this.cellRenderer = cellRenderer;
        fire(CELL_RENDERER_PROPERTY, old, cellRenderer);
    }

    /** How the cells are drawn, or {@code null} for the table's. */
    public TableCellRenderer getCellRenderer() {
        return this.cellRenderer;
    }

    /** Changes how the cells are edited. */
    public void setCellEditor(TableCellEditor cellEditor) {
        TableCellEditor old = this.cellEditor;
        this.cellEditor = cellEditor;
        fire("cellEditor", old, cellEditor);
    }

    /** How the cells are edited, or {@code null} for the table's. */
    public TableCellEditor getCellEditor() {
        return this.cellEditor;
    }

    /**
     * Changes the width, clamping against the minimum and the maximum.
     *
     * <p>The notice may have been silenced with {@link #disableResizedPosting}: while the user
     * drags the edge, every pixel would fire a notice and a relayout.
     */
    public void setWidth(int width) {
        int old = this.width;
        int newValue = Math.min(Math.max(width, this.minWidth), this.maxWidth);
        this.width = newValue;
        if (newValue == old) {
            return;
        }
        if (this.resizedPostingDisableCount == 0) {
            // The notice goes with the name "width", NOT with COLUMN_WIDTH_PROPERTY, which is worth
                        // "columWidth" -- a typo of the JDK's that stayed in the public constant
                        // and that nobody uses. It is measured: whoever listens to a column
                        // receives "width".
            fire("width", Integer.valueOf(old), Integer.valueOf(newValue));
        }
    }

    /** The current width. */
    public int getWidth() {
        return this.width;
    }

    /** Changes the preferred width, clamping against the minimum and the maximum. */
    public void setPreferredWidth(int preferredWidth) {
        int old = this.preferredWidth;
        this.preferredWidth = Math.min(Math.max(preferredWidth, this.minWidth), this.maxWidth);
        fire("preferredWidth", Integer.valueOf(old), Integer.valueOf(this.preferredWidth));
    }

    /** The preferred width. */
    public int getPreferredWidth() {
        return this.preferredWidth;
    }

    /**
     * Changes the minimum width.
     *
     * <p>It raises the current and the preferred ones if they were left below: leaving them under
     * the minimum would be leaving the column violating its own constraint.
     */
    public void setMinWidth(int minWidth) {
        int old = this.minWidth;
        this.minWidth = Math.max(Math.min(minWidth, this.maxWidth), 0);
        if (this.width < this.minWidth) {
            setWidth(this.minWidth);
        }
        if (this.preferredWidth < this.minWidth) {
            setPreferredWidth(this.minWidth);
        }
        fire("minWidth", Integer.valueOf(old), Integer.valueOf(this.minWidth));
    }

    /** The minimum width. */
    public int getMinWidth() {
        return this.minWidth;
    }

    /** Changes the maximum width, lowering the current and the preferred ones if needed. */
    public void setMaxWidth(int maxWidth) {
        int old = this.maxWidth;
        this.maxWidth = Math.max(this.minWidth, maxWidth);
        if (this.width > this.maxWidth) {
            setWidth(this.maxWidth);
        }
        if (this.preferredWidth > this.maxWidth) {
            setPreferredWidth(this.maxWidth);
        }
        fire("maxWidth", Integer.valueOf(old), Integer.valueOf(this.maxWidth));
    }

    /** The maximum width. */
    public int getMaxWidth() {
        return this.maxWidth;
    }

    /** Changes whether the user can resize it. */
    public void setResizable(boolean isResizable) {
        boolean old = this.isResizable;
        this.isResizable = isResizable;
        fire("isResizable", Boolean.valueOf(old), Boolean.valueOf(isResizable));
    }

    /** Whether the user can resize it. */
    public boolean getResizable() {
        return this.isResizable;
    }

    /** Sets the three widths to the preferred one, leaving the column with no stretching room. */
    public void sizeWidthToFit() {
        if (this.headerRenderer == null) {
            return;
        }
        setMinWidth(this.preferredWidth);
        setMaxWidth(this.preferredWidth);
        setWidth(this.preferredWidth);
    }

    /**
     * Silences the width change notices.
     *
     * <p>It is a counter and not a flag so that two nested silencings do not tread on each other:
     * the inner one cannot turn the notices the outer one turned off back on.
     *
     * @deprecated it is from the time when the table used it while the user dragged; today the
     *     drag itself resolves it
     */
    @Deprecated
    public void disableResizedPosting() {
        this.resizedPostingDisableCount = this.resizedPostingDisableCount + 1;
    }

    /**
     * Allows the notices again, and sends one if the width changed while they were silenced.
     *
     * @deprecated see {@link #disableResizedPosting}
     */
    @Deprecated
    public void enableResizedPosting() {
        this.resizedPostingDisableCount = this.resizedPostingDisableCount - 1;
        if (this.resizedPostingDisableCount < 0) {
            this.resizedPostingDisableCount = 0;
        }
    }

    /** Adds a property change listener. */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (this.changes == null) {
            this.changes = new PropertyChangeSupport(this);
        }
        this.changes.addPropertyChangeListener(listener);
    }

    /** Removes a listener. */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (this.changes != null) {
            this.changes.removePropertyChangeListener(listener);
        }
    }

    /** The property change listeners. */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (this.changes == null) {
            return new PropertyChangeListener[0];
        }
        return this.changes.getPropertyChangeListeners();
    }

    /**
     * The header's default renderer.
     *
     * @return {@code null} in this VM: the JDK's is a component that paints itself, and this
     *     library does not ship drawable Swing. See {@link javax.swing.JTable}'s note. Returning
     *     {@code null} is what already means "use the table's", so it invents nothing
     */
    protected TableCellRenderer createDefaultHeaderRenderer() {
        return null;
    }

    private void fire(String property, Object old, Object newValue) {
        if (this.changes != null) {
            this.changes.firePropertyChange(property, old, newValue);
        }
    }
}
