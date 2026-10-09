package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TableUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * A table: rows and columns of cells.
 *
 * <h2>Four models, not one</h2>
 *
 * <p>The <strong>data</strong> ({@link TableModel}), the <strong>columns</strong>
 * ({@link TableColumnModel}), the <strong>row selection</strong>
 * ({@link ListSelectionModel}) and -- inside the column model -- the column selection. They
 * are separate because they change for different reasons: moving a column somewhere else does
 * not touch the data, and choosing a row does not touch either of the two.
 *
 * <h2>View indices and model indices</h2>
 *
 * <p>It is what has to be kept in mind all the time. With a sort set or columns moved, the
 * view's row 0 is not the model's row 0, nor the view's column 2 the model's 2.
 * <strong>Almost every method of this class speaks in view indices</strong> --
 * {@link #getValueAt} and {@link #setValueAt} included, which translate them before touching
 * the model -- and the four {@code convertXxx} are the bridge.
 *
 * <p>Using a view index against the model does not fail: it returns another cell. It is the
 * classic mistake with sortable tables and it gives no signal.
 *
 * <h2>One renderer per column type</h2>
 *
 * <p>The table does not have one component per cell: it has one per <em>type</em>, and it
 * configures it and draws it once per cell. Which one falls to each column comes from
 * {@code getColumnClass}, and hence a model that declares its types looks right without writing
 * a line of drawing.
 *
 * <h2>Editing is a state, not a call</h2>
 *
 * <p>While editing, the table has a real component over the cell
 * ({@link #getEditorComponent}), and {@link #isEditing} is true. It ends when the editor gives
 * notice, and the table listens to that notice: that is why it implements
 * {@link CellEditorListener}.
 *
 * <h2>What a screen is needed to see</h2>
 *
 * <p>The drawing and the laying out of columns are written and work over any {@code Graphics};
 * what there is not is an installed look and feel that sets the colours, the grid and the
 * header. With that, {@link #getCellRenderer} returns the base renderer and the table is seen
 * grey. It is the usual gap and it does not belong to this class.
 */
public class JTable extends JComponent implements TableModelListener, Scrollable,
        TableColumnModelListener, ListSelectionListener, CellEditorListener, Accessible,
        RowSorterListener {

    private static final String uiClassID = "TableUI";

    /** The columns do not adjust by themselves; a horizontal bar appears. */
    public static final int AUTO_RESIZE_OFF = 0;

    /** On enlarging a column the next one shrinks. */
    public static final int AUTO_RESIZE_NEXT_COLUMN = 1;

    /** On enlarging a column it is shared out among all those that follow. */
    public static final int AUTO_RESIZE_SUBSEQUENT_COLUMNS = 2;

    /** On enlarging a column the last one shrinks. */
    public static final int AUTO_RESIZE_LAST_COLUMN = 3;

    /** On enlarging a column it is shared out among them all. */
    public static final int AUTO_RESIZE_ALL_COLUMNS = 4;

    /** The data. */
    protected TableModel dataModel;

    /** The view's columns. */
    protected TableColumnModel columnModel;

    /** The row selection. */
    protected ListSelectionModel selectionModel;

    /** The header bar, or null if it is not shown. */
    protected JTableHeader tableHeader;

    /** A row's height. */
    protected int rowHeight;

    /** The vertical space between rows. */
    protected int rowMargin;

    /** The grid's colour. */
    protected Color gridColor;

    /** Whether the horizontal lines are drawn. */
    protected boolean showHorizontalLines;

    /** Whether the vertical lines are drawn. */
    protected boolean showVerticalLines;

    /** What to do when a column's width changes. */
    protected int autoResizeMode;

    /** Whether the columns are built by themselves on changing the model. */
    protected boolean autoCreateColumnsFromModel;

    /** How much it asks to measure when it is inside a pane with bars. */
    protected Dimension preferredViewportSize;

    /** Whether rows may be chosen. */
    protected boolean rowSelectionAllowed;

    /** Whether what is chosen are cells and not whole rows or columns. */
    protected boolean cellSelectionEnabled;

    /** The component the editing is being done with, or null. */
    protected transient Component editorComp;

    /** The editor in use, or null. */
    protected transient TableCellEditor cellEditor;

    /** The column that is being edited, or -1. */
    protected transient int editingColumn;

    /** The row that is being edited, or -1. */
    protected transient int editingRow;

    /** The renderers by column type; see the class note. */
    protected transient Hashtable<Object, Object> defaultRenderersByColumnClass;

    /** The editors by column type. */
    protected transient Hashtable<Object, Object> defaultEditorsByColumnClass;

    /** The chosen text's colour. */
    protected Color selectionForeground;

    /** The background of what is chosen. */
    protected Color selectionBackground;

    private RowSorter<? extends TableModel> sortManager;
    private boolean autoCreateRowSorter;
    private boolean updateSelectionOnSort = true;
    private boolean fillsViewportHeight;
    private boolean surrendersFocusOnKeystroke;
    private boolean dragEnabled;
    private DropMode dropMode = DropMode.USE_SELECTION;
    private transient DropLocation dropLocation;
    private SizeSequence rowModel;

    /** An empty table, with a default model. */
    public JTable() {
        this(null, null, null);
    }

    /** Over those data. */
    public JTable(TableModel dm) {
        this(dm, null, null);
    }

    /** Over those data and those columns. */
    public JTable(TableModel dm, TableColumnModel cm) {
        this(dm, cm, null);
    }

    /**
     * With the three models.
     *
     * <p>Any of the three at null is replaced by the default one. If no columns are given, they
     * are built from the data model.
     */
    public JTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super();
        setLayout(null);
        if (cm != null) {
            columnModel = cm;
            autoCreateColumnsFromModel = false;
        } else {
            columnModel = createDefaultColumnModel();
            autoCreateColumnsFromModel = true;
        }
        columnModel.addColumnModelListener(this);
        if (sm != null) {
            selectionModel = sm;
        } else {
            selectionModel = createDefaultSelectionModel();
        }
        selectionModel.addListSelectionListener(this);
        setTableHeader(createDefaultTableHeader());
        initializeLocalVars();
        if (dm != null) {
            setModel(dm);
        } else {
            setModel(createDefaultDataModel());
        }
        updateUI();
    }

    /** Of that size, with empty and editable cells. */
    public JTable(int numRows, int numColumns) {
        this(new DefaultTableModel(numRows, numColumns));
    }

    /** With those data and those column names. */
    public JTable(Vector<? extends Vector> rowData, Vector<?> columnNames) {
        this(new DefaultTableModel(rowData, columnNames));
    }

    /** The same, with arrays. */
    public JTable(final Object[][] rowData, final Object[] columnNames) {
        this(new AbstractTableModel() {
            public String getColumnName(int column) {
                return columnNames[column].toString();
            }

            public int getRowCount() {
                return rowData.length;
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            public Object getValueAt(int row, int col) {
                return rowData[row][col];
            }

            public boolean isCellEditable(int row, int column) {
                return true;
            }

            public void setValueAt(Object value, int row, int col) {
                rowData[row][col] = value;
                fireTableCellUpdated(row, col);
            }
        });
    }

    /**
     * On entering a pane with bars, it puts the header on top of it.
     *
     * <p>It is what makes a table inside a {@code JScrollPane} show the column titles without
     * anybody adding them by hand.
     */
    public void addNotify() {
        super.addNotify();
        configureEnclosingScrollPane();
    }

    /** Ver {@link #addNotify}. */
    protected void configureEnclosingScrollPane() {
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            java.awt.Container gp = parent.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                scrollPane.setColumnHeaderView(getTableHeader());
            }
        }
    }

    /** On leaving, it removes the header. */
    public void removeNotify() {
        unconfigureEnclosingScrollPane();
        super.removeNotify();
    }

    /** Ver {@link #removeNotify}. */
    protected void unconfigureEnclosingScrollPane() {
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            java.awt.Container gp = parent.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                scrollPane.setColumnHeaderView(null);
            }
        }
    }

    /**
     * A pane with bars that contains that table.
     *
     * @deprecated Use {@code new JScrollPane(table)}, which does the same.
     */
    @Deprecated
    public static JScrollPane createScrollPaneForTable(JTable aTable) {
        return new JScrollPane(aTable);
    }

    /** The title bar; null removes it. */
    public void setTableHeader(JTableHeader tableHeader) {
        if (this.tableHeader != tableHeader) {
            JTableHeader old = this.tableHeader;
            if (old != null) {
                old.setTable(null);
            }
            this.tableHeader = tableHeader;
            if (tableHeader != null) {
                tableHeader.setTable(this);
            }
            firePropertyChange("tableHeader", old, tableHeader);
        }
    }

    public JTableHeader getTableHeader() {
        return tableHeader;
    }

    /**
     * The height of every row.
     *
     * @throws IllegalArgumentException if it is not positive
     */
    public void setRowHeight(int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("New row height less than 1");
        }
        int old = this.rowHeight;
        this.rowHeight = rowHeight;
        rowModel = null;
        resizeAndRepaint();
        firePropertyChange("rowHeight", old, rowHeight);
    }

    public int getRowHeight() {
        return rowHeight;
    }

    /**
     * The height of a single row.
     *
     * <p>A table with rows of different heights is more expensive to draw and to walk: until
     * somebody calls this, they all measure the same and the row of a pixel is a division.
     *
     * @throws IllegalArgumentException if the height is not positive or the row is out of range
     */
    public void setRowHeight(int row, int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("New row height less than 1");
        }
        if (rowModel == null) {
            rowModel = new SizeSequence(getRowCount(), getRowHeight());
        }
        rowModel.setSize(row, rowHeight);
        resizeAndRepaint();
    }

    /**
     * That row's height.
     *
     * <p><strong>It does not validate the index</strong>, and it is measured: with per-row heights
     * set, a row that does not exist returns zero instead of throwing. The look and feel asks for
     * it while drawing and has no way of knowing where it ends.
     */
    public int getRowHeight(int row) {
        return (rowModel == null) ? getRowHeight() : rowModel.getSize(row);
    }

    /** The vertical space between rows; it comes out of the row's height, it is not added. */
    public void setRowMargin(int rowMargin) {
        int old = this.rowMargin;
        this.rowMargin = rowMargin;
        resizeAndRepaint();
        firePropertyChange("rowMargin", old, rowMargin);
    }

    public int getRowMargin() {
        return rowMargin;
    }

    /** The space between cells: the width goes to the column model, the height here. */
    public void setIntercellSpacing(Dimension intercellSpacing) {
        setRowMargin(intercellSpacing.height);
        getColumnModel().setColumnMargin(intercellSpacing.width);
        resizeAndRepaint();
    }

    public Dimension getIntercellSpacing() {
        return new Dimension(getColumnModel().getColumnMargin(), rowMargin);
    }

    public void setGridColor(Color gridColor) {
        if (gridColor == null) {
            throw new IllegalArgumentException("New color is null");
        }
        Color old = this.gridColor;
        this.gridColor = gridColor;
        firePropertyChange("gridColor", old, gridColor);
        repaint();
    }

    public Color getGridColor() {
        return gridColor;
    }

    /** It switches both directions of the grid on or off at once. */
    public void setShowGrid(boolean showGrid) {
        setShowHorizontalLines(showGrid);
        setShowVerticalLines(showGrid);
        repaint();
    }

    public void setShowHorizontalLines(boolean showHorizontalLines) {
        boolean old = this.showHorizontalLines;
        this.showHorizontalLines = showHorizontalLines;
        firePropertyChange("showHorizontalLines", old, showHorizontalLines);
        repaint();
    }

    public void setShowVerticalLines(boolean showVerticalLines) {
        boolean old = this.showVerticalLines;
        this.showVerticalLines = showVerticalLines;
        firePropertyChange("showVerticalLines", old, showVerticalLines);
        repaint();
    }

    public boolean getShowHorizontalLines() {
        return showHorizontalLines;
    }

    public boolean getShowVerticalLines() {
        return showVerticalLines;
    }

    /**
     * What happens to the other columns when one changes width.
     *
     * <p>A mode that is none of the five is silently ignored, which is what the JDK does.
     */
    public void setAutoResizeMode(int mode) {
        if ((mode == AUTO_RESIZE_OFF) || (mode == AUTO_RESIZE_NEXT_COLUMN)
                || (mode == AUTO_RESIZE_SUBSEQUENT_COLUMNS)
                || (mode == AUTO_RESIZE_LAST_COLUMN) || (mode == AUTO_RESIZE_ALL_COLUMNS)) {
            int old = autoResizeMode;
            autoResizeMode = mode;
            resizeAndRepaint();
            if (tableHeader != null) {
                tableHeader.resizeAndRepaint();
            }
            firePropertyChange("autoResizeMode", old, autoResizeMode);
        }
    }

    public int getAutoResizeMode() {
        return autoResizeMode;
    }

    /** Whether changing the model rebuilds the columns. */
    public void setAutoCreateColumnsFromModel(boolean autoCreateColumnsFromModel) {
        if (this.autoCreateColumnsFromModel != autoCreateColumnsFromModel) {
            boolean old = this.autoCreateColumnsFromModel;
            this.autoCreateColumnsFromModel = autoCreateColumnsFromModel;
            if (autoCreateColumnsFromModel) {
                createDefaultColumnsFromModel();
            }
            firePropertyChange("autoCreateColumnsFromModel", old, autoCreateColumnsFromModel);
        }
    }

    public boolean getAutoCreateColumnsFromModel() {
        return autoCreateColumnsFromModel;
    }

    /**
     * It throws the columns away and builds one for each column of the model.
     *
     * <p>The widths adjusted by hand are lost; it is what has to be known before calling it.
     */
    public void createDefaultColumnsFromModel() {
        TableModel m = getModel();
        if (m != null) {
            TableColumnModel cm = getColumnModel();
            while (cm.getColumnCount() > 0) {
                cm.removeColumn(cm.getColumn(0));
            }
            for (int i = 0; i < m.getColumnCount(); i++) {
                TableColumn newColumn = new TableColumn(i);
                addColumn(newColumn);
            }
        }
    }

    /** The renderer for that column type. */
    public void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
        if (renderer != null) {
            defaultRenderersByColumnClass.put(columnClass, renderer);
        } else {
            defaultRenderersByColumnClass.remove(columnClass);
        }
    }

    /**
     * That type's renderer, or its superclass's if there is none of its own.
     *
     * <p>It goes up the hierarchy: a column of {@code Integer} with no renderer of its own uses
     * {@code Number}'s, and if there is none either, {@code Object}'s.
     */
    public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        if (columnClass == null) {
            return null;
        }
        Object renderer = defaultRenderersByColumnClass.get(columnClass);
        if (renderer != null) {
            return (TableCellRenderer) renderer;
        }
        Class<?> c = columnClass.getSuperclass();
        if (c == null && columnClass != Object.class) {
            c = Object.class;
        }
        return getDefaultRenderer(c);
    }

    /** The editor for that column type. */
    public void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) {
        if (editor != null) {
            defaultEditorsByColumnClass.put(columnClass, editor);
        } else {
            defaultEditorsByColumnClass.remove(columnClass);
        }
    }

    /** That type's editor, going up the hierarchy like {@link #getDefaultRenderer}. */
    public TableCellEditor getDefaultEditor(Class<?> columnClass) {
        if (columnClass == null) {
            return null;
        }
        Object editor = defaultEditorsByColumnClass.get(columnClass);
        if (editor != null) {
            return (TableCellEditor) editor;
        }
        Class<?> c = columnClass.getSuperclass();
        if (c == null && columnClass != Object.class) {
            c = Object.class;
        }
        return getDefaultEditor(c);
    }

    public void setDragEnabled(boolean b) {
        dragEnabled = b;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /**
     * How a drop over the table is interpreted.
     *
     * @throws IllegalArgumentException if the mode does not serve for a table
     */
    public final void setDropMode(DropMode dropMode) {
        if (dropMode != null) {
            if (dropMode == DropMode.USE_SELECTION || dropMode == DropMode.ON
                    || dropMode == DropMode.INSERT || dropMode == DropMode.INSERT_ROWS
                    || dropMode == DropMode.INSERT_COLS || dropMode == DropMode.ON_OR_INSERT
                    || dropMode == DropMode.ON_OR_INSERT_ROWS
                    || dropMode == DropMode.ON_OR_INSERT_COLS) {
                this.dropMode = dropMode;
                return;
            }
        }
        throw new IllegalArgumentException(dropMode + ": Unsupported drop mode for table");
    }

    public final DropMode getDropMode() {
        return dropMode;
    }

    /** Where what is being dragged would fall, or null if nothing is being dragged. */
    public final DropLocation getDropLocation() {
        return dropLocation;
    }

    /**
     * Whether changing the model builds a row sorter by itself.
     *
     * <p>Switched off by default: a sorter changes the rows' numbering, and switching it on
     * without meaning to breaks the code that uses model indices against the view.
     */
    public void setAutoCreateRowSorter(boolean autoCreateRowSorter) {
        boolean old = this.autoCreateRowSorter;
        this.autoCreateRowSorter = autoCreateRowSorter;
        if (autoCreateRowSorter) {
            setRowSorter(new javax.swing.table.TableRowSorter<TableModel>(getModel()));
        }
        firePropertyChange("autoCreateRowSorter", old, autoCreateRowSorter);
    }

    public boolean getAutoCreateRowSorter() {
        return autoCreateRowSorter;
    }

    /** Whether what is chosen is kept on reordering; switched on by default. */
    public void setUpdateSelectionOnSort(boolean update) {
        if (updateSelectionOnSort != update) {
            updateSelectionOnSort = update;
            firePropertyChange("updateSelectionOnSort", !update, update);
        }
    }

    public boolean getUpdateSelectionOnSort() {
        return updateSelectionOnSort;
    }

    /** The row sorter; null shows the rows in the model's order. */
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        RowSorter<? extends TableModel> oldRowSorter = getRowSorter();
        if (oldRowSorter != null) {
            oldRowSorter.removeRowSorterListener(this);
        }
        sortManager = sorter;
        if (sorter != null) {
            sorter.addRowSorterListener(this);
        }
        firePropertyChange("rowSorter", oldRowSorter, sorter);
        firePropertyChange("sorter", oldRowSorter, sorter);
        clearSelection();
        resizeAndRepaint();
    }

    public RowSorter<? extends TableModel> getRowSorter() {
        return sortManager;
    }

    /** One row, a range, or anything; see {@link ListSelectionModel}. */
    public void setSelectionMode(int selectionMode) {
        clearSelection();
        getSelectionModel().setSelectionMode(selectionMode);
        getColumnModel().getSelectionModel().setSelectionMode(selectionMode);
    }

    public void setRowSelectionAllowed(boolean rowSelectionAllowed) {
        boolean old = this.rowSelectionAllowed;
        this.rowSelectionAllowed = rowSelectionAllowed;
        if (old != rowSelectionAllowed) {
            repaint();
        }
        firePropertyChange("rowSelectionAllowed", old, rowSelectionAllowed);
    }

    public boolean getRowSelectionAllowed() {
        return rowSelectionAllowed;
    }

    public void setColumnSelectionAllowed(boolean columnSelectionAllowed) {
        boolean old = columnModel.getColumnSelectionAllowed();
        columnModel.setColumnSelectionAllowed(columnSelectionAllowed);
        if (old != columnSelectionAllowed) {
            repaint();
        }
        firePropertyChange("columnSelectionAllowed", old, columnSelectionAllowed);
    }

    public boolean getColumnSelectionAllowed() {
        return columnModel.getColumnSelectionAllowed();
    }

    /**
     * Whether what is chosen are cells.
     *
     * <p>Choosing cells is having both selections switched on at once: a cell is chosen when its
     * row and its column are. There is no third model.
     */
    public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
        setRowSelectionAllowed(cellSelectionEnabled);
        setColumnSelectionAllowed(cellSelectionEnabled);
        boolean old = this.cellSelectionEnabled;
        this.cellSelectionEnabled = cellSelectionEnabled;
        firePropertyChange("cellSelectionEnabled", old, cellSelectionEnabled);
    }

    public boolean getCellSelectionEnabled() {
        return getRowSelectionAllowed() && getColumnSelectionAllowed();
    }

    /** It chooses everything that can be chosen. */
    public void selectAll() {
        if (isEditing()) {
            removeEditor();
        }
        if (getRowCount() > 0 && getColumnCount() > 0) {
            int oldLead = getSelectionModel().getLeadSelectionIndex();
            int oldAnchor = getSelectionModel().getAnchorSelectionIndex();
            int oldLeadCol = getColumnModel().getSelectionModel().getLeadSelectionIndex();
            int oldAnchorCol = getColumnModel().getSelectionModel().getAnchorSelectionIndex();
            setRowSelectionInterval(0, getRowCount() - 1);
            setColumnSelectionInterval(0, getColumnCount() - 1);
            // The anchor and the lead are restored: choosing everything must not move the point the
                        // user was extending the selection from.
            restoreSelection(getSelectionModel(), oldAnchor, oldLead);
            restoreSelection(getColumnModel().getSelectionModel(), oldAnchorCol, oldLeadCol);
        }
    }

    private static void restoreSelection(ListSelectionModel m, int anchor, int lead) {
        if (anchor == -1 || lead == -1) {
            return;
        }
        m.addSelectionInterval(anchor, anchor);
        m.addSelectionInterval(lead, lead);
    }

    public void clearSelection() {
        selectionModel.clearSelection();
        columnModel.getSelectionModel().clearSelection();
    }

    /**
     * @throws IllegalArgumentException if some index is out of range
     */
    public void setRowSelectionInterval(int index0, int index1) {
        selectionModel.setSelectionInterval(boundRow(index0), boundRow(index1));
    }

    /**
     * @throws IllegalArgumentException if some index is out of range
     */
    public void setColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().setSelectionInterval(boundColumn(index0),
                boundColumn(index1));
    }

    /**
     * @throws IllegalArgumentException if some index is out of range
     */
    public void addRowSelectionInterval(int index0, int index1) {
        selectionModel.addSelectionInterval(boundRow(index0), boundRow(index1));
    }

    /**
     * @throws IllegalArgumentException if some index is out of range
     */
    public void addColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().addSelectionInterval(boundColumn(index0),
                boundColumn(index1));
    }

    /**
     * @throws IllegalArgumentException if some index is out of range
     */
    public void removeRowSelectionInterval(int index0, int index1) {
        selectionModel.removeSelectionInterval(boundRow(index0), boundRow(index1));
    }

    /**
     * @throws IllegalArgumentException if some index is out of range
     */
    public void removeColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().removeSelectionInterval(boundColumn(index0),
                boundColumn(index1));
    }

    private int boundRow(int row) {
        if (row < 0 || row >= getRowCount()) {
            throw new IllegalArgumentException("Row index out of range");
        }
        return row;
    }

    private int boundColumn(int col) {
        if (col < 0 || col >= getColumnCount()) {
            throw new IllegalArgumentException("Column index out of range");
        }
        return col;
    }

    /** The first chosen row, or -1. */
    public int getSelectedRow() {
        return selectionModel.getMinSelectionIndex();
    }

    /** The first chosen column, or -1. */
    public int getSelectedColumn() {
        return columnModel.getSelectionModel().getMinSelectionIndex();
    }

    public int[] getSelectedRows() {
        return chosenIndices(selectionModel);
    }

    public int[] getSelectedColumns() {
        return columnModel.getSelectedColumns();
    }

    private static int[] chosenIndices(ListSelectionModel m) {
        int iMin = m.getMinSelectionIndex();
        int iMax = m.getMaxSelectionIndex();
        if ((iMin == -1) || (iMax == -1)) {
            return new int[0];
        }
        int[] rvTmp = new int[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (m.isSelectedIndex(i)) {
                rvTmp[n] = i;
                n = n + 1;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    public int getSelectedRowCount() {
        return getSelectedRows().length;
    }

    public int getSelectedColumnCount() {
        return columnModel.getSelectedColumnCount();
    }

    public boolean isRowSelected(int row) {
        return selectionModel.isSelectedIndex(row);
    }

    public boolean isColumnSelected(int column) {
        return columnModel.getSelectionModel().isSelectedIndex(column);
    }

    /** A cell is chosen when its row and its column are; see {@link #setCellSelectionEnabled}. */
    public boolean isCellSelected(int row, int column) {
        if (!getRowSelectionAllowed() && !getColumnSelectionAllowed()) {
            return false;
        }
        return (!getRowSelectionAllowed() || isRowSelected(row))
                && (!getColumnSelectionAllowed() || isColumnSelected(column));
    }

    /**
     * What a click on a cell does.
     *
     * <p>The two booleans are the two keys: {@code extend} is Shift -- it extends from the anchor
     * -- and {@code toggle} is Control -- it adds or subtracts without touching the rest --. With
     * both at false, a clean click: only that cell is chosen.
     */
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        ListSelectionModel rsm = getSelectionModel();
        ListSelectionModel csm = getColumnModel().getSelectionModel();
        if (extend) {
            if (toggle) {
                rsm.setAnchorSelectionIndex(rowIndex);
                csm.setAnchorSelectionIndex(columnIndex);
            } else {
                rsm.setLeadSelectionIndex(rowIndex);
                csm.setLeadSelectionIndex(columnIndex);
            }
        } else {
            if (toggle) {
                if (isCellSelected(rowIndex, columnIndex)) {
                    rsm.removeSelectionInterval(rowIndex, rowIndex);
                    csm.removeSelectionInterval(columnIndex, columnIndex);
                } else {
                    rsm.addSelectionInterval(rowIndex, rowIndex);
                    csm.addSelectionInterval(columnIndex, columnIndex);
                }
            } else {
                rsm.setSelectionInterval(rowIndex, rowIndex);
                csm.setSelectionInterval(columnIndex, columnIndex);
            }
        }
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public void setSelectionForeground(Color selectionForeground) {
        Color old = this.selectionForeground;
        this.selectionForeground = selectionForeground;
        firePropertyChange("selectionForeground", old, selectionForeground);
        if (old == null || !old.equals(selectionForeground)) {
            repaint();
        }
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        Color old = this.selectionBackground;
        this.selectionBackground = selectionBackground;
        firePropertyChange("selectionBackground", old, selectionBackground);
        if (old == null || !old.equals(selectionBackground)) {
            repaint();
        }
    }

    /**
     * The column with that identifier.
     *
     * @throws IllegalArgumentException if there is none
     */
    public TableColumn getColumn(Object identifier) {
        TableColumnModel cm = getColumnModel();
        int columnIndex = cm.getColumnIndex(identifier);
        return cm.getColumn(columnIndex);
    }

    /** From view index to model index, for columns. */
    public int convertColumnIndexToModel(int viewColumnIndex) {
        if (viewColumnIndex < 0) {
            return viewColumnIndex;
        }
        return getColumnModel().getColumn(viewColumnIndex).getModelIndex();
    }

    /** From model index to view index; -1 if that column is not shown. */
    public int convertColumnIndexToView(int modelColumnIndex) {
        if (modelColumnIndex < 0) {
            return modelColumnIndex;
        }
        TableColumnModel cm = getColumnModel();
        for (int column = 0; column < getColumnCount(); column++) {
            if (cm.getColumn(column).getModelIndex() == modelColumnIndex) {
                return column;
            }
        }
        return -1;
    }

    /**
     * From model index to view index, for rows.
     *
     * @throws IndexOutOfBoundsException if it is out of range
     */
    public int convertRowIndexToView(int modelRowIndex) {
        RowSorter<? extends TableModel> sorter = getRowSorter();
        if (sorter != null) {
            return sorter.convertRowIndexToView(modelRowIndex);
        }
        return modelRowIndex;
    }

    /**
     * From view index to model index, for rows.
     *
     * @throws IndexOutOfBoundsException if it is out of range
     */
    public int convertRowIndexToModel(int viewRowIndex) {
        RowSorter<? extends TableModel> sorter = getRowSorter();
        if (sorter != null) {
            return sorter.convertRowIndexToModel(viewRowIndex);
        }
        return viewRowIndex;
    }

    /** How many rows are seen; with a filter set, fewer than the model's. */
    public int getRowCount() {
        RowSorter<? extends TableModel> sorter = getRowSorter();
        if (sorter != null) {
            return sorter.getViewRowCount();
        }
        return getModel().getRowCount();
    }

    public int getColumnCount() {
        return getColumnModel().getColumnCount();
    }

    /** That view column's name. */
    public String getColumnName(int column) {
        return getModel().getColumnName(convertColumnIndexToModel(column));
    }

    /** That view column's type. */
    public Class<?> getColumnClass(int column) {
        return getModel().getColumnClass(convertColumnIndexToModel(column));
    }

    /** That view cell's value; it translates both indices. */
    public Object getValueAt(int row, int column) {
        return getModel().getValueAt(convertRowIndexToModel(row),
                convertColumnIndexToModel(column));
    }

    /** It changes that view cell; it translates both indices. */
    public void setValueAt(Object aValue, int row, int column) {
        getModel().setValueAt(aValue, convertRowIndexToModel(row),
                convertColumnIndexToModel(column));
    }

    public boolean isCellEditable(int row, int column) {
        return getModel().isCellEditable(convertRowIndexToModel(row),
                convertColumnIndexToModel(column));
    }

    /** It adds a column at the end of the view. */
    public void addColumn(TableColumn aColumn) {
        if (aColumn.getHeaderValue() == null) {
            int modelColumn = aColumn.getModelIndex();
            String columnName = getModel().getColumnName(modelColumn);
            aColumn.setHeaderValue(columnName);
        }
        getColumnModel().addColumn(aColumn);
    }

    public void removeColumn(TableColumn aColumn) {
        getColumnModel().removeColumn(aColumn);
    }

    public void moveColumn(int column, int targetColumn) {
        getColumnModel().moveColumn(column, targetColumn);
    }

    /** The column that falls at that point, or -1. */
    public int columnAtPoint(Point point) {
        int x = point.x;
        if (!getComponentOrientation().isLeftToRight()) {
            x = getWidth() - x - 1;
        }
        return getColumnModel().getColumnIndexAtX(x);
    }

    /** The row that falls at that point, or -1. */
    public int rowAtPoint(Point point) {
        int y = point.y;
        int result = (rowModel == null) ? y / getRowHeight() : rowAt(y);
        if (result < 0) {
            return -1;
        } else if (result >= getRowCount()) {
            return -1;
        }
        return result;
    }

    /** The row that falls at that pixel when the rows measure differently. */
    private int rowAt(int y) {
        if (y < 0) {
            return -1;
        }
        int accumulated = 0;
        int n = getRowCount();
        for (int i = 0; i < n; i++) {
            accumulated = accumulated + getRowHeight(i);
            if (y < accumulated) {
                return i;
            }
        }
        return n;
    }

    /**
     * That view cell's rectangle.
     *
     * <p>With {@code includeSpacing} at false the margins are taken off it, which is what the
     * content takes up; with true, the whole cell including the grid line.
     */
    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        Rectangle r = new Rectangle();
        boolean valid = true;
        if (row < 0 || row >= getRowCount()) {
            valid = false;
        } else {
            r.height = getRowHeight(row);
            r.y = 0;
            for (int i = 0; i < row; i++) {
                r.y = r.y + getRowHeight(i);
            }
        }
        if (column < 0 || column >= getColumnCount()) {
            valid = false;
        } else {
            TableColumnModel cm = getColumnModel();
            if (getComponentOrientation().isLeftToRight()) {
                for (int i = 0; i < column; i++) {
                    r.x = r.x + cm.getColumn(i).getWidth();
                }
            } else {
                for (int i = cm.getColumnCount() - 1; i > column; i--) {
                    r.x = r.x + cm.getColumn(i).getWidth();
                }
            }
            r.width = cm.getColumn(column).getWidth();
        }
        if (valid && !includeSpacing) {
            int rm = getRowMargin();
            int cm2 = getColumnModel().getColumnMargin();
            r.setBounds(r.x + cm2 / 2, r.y + rm / 2, r.width - cm2, r.height - rm);
        }
        return r;
    }

    /** It lays the columns out according to the adjustment mode. */
    public void doLayout() {
        TableColumn resizingColumn = getResizingColumn();
        if (resizingColumn == null) {
            setWidthsFromPreferredWidths(false);
        } else {
            int columnIndex = viewIndexForColumn(resizingColumn);
            int delta = getWidth() - getColumnModel().getTotalColumnWidth();
            accommodateDelta(columnIndex, delta);
            delta = getWidth() - getColumnModel().getTotalColumnWidth();
            if (delta != 0) {
                resizingColumn.setWidth(resizingColumn.getWidth() + delta);
            }
            setWidthsFromPreferredWidths(true);
        }
        super.doLayout();
    }

    private TableColumn getResizingColumn() {
        return (tableHeader == null) ? null : tableHeader.getResizingColumn();
    }

    private int viewIndexForColumn(TableColumn aColumn) {
        TableColumnModel cm = getColumnModel();
        for (int column = 0; column < cm.getColumnCount(); column++) {
            if (cm.getColumn(column) == aColumn) {
                return column;
            }
        }
        return -1;
    }

    private void setWidthsFromPreferredWidths(boolean inverse) {
        int totalWidth = getWidth();
        int totalPreferred = 0;
        TableColumnModel cm = getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            totalPreferred = totalPreferred + cm.getColumn(i).getPreferredWidth();
        }
        if (getAutoResizeMode() == AUTO_RESIZE_OFF || totalPreferred == 0) {
            for (int i = 0; i < cm.getColumnCount(); i++) {
                TableColumn c = cm.getColumn(i);
                c.setWidth(c.getPreferredWidth());
            }
            return;
        }
        // It is shared out in proportion to the preferred one, which is what makes a column that
                    // asked for twice as much take twice the extra space.
        int shared = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn c = cm.getColumn(i);
            int w;
            if (i == cm.getColumnCount() - 1) {
                w = totalWidth - shared;
            } else {
                w = (int) ((long) c.getPreferredWidth() * totalWidth / totalPreferred);
            }
            c.setWidth(w);
            shared = shared + c.getWidth();
        }
    }

    private void accommodateDelta(int resizingColumnIndex, int delta) {
        // The fine sharing out is done by the look and feel while dragging; here laying out again
        // is enough.
        setWidthsFromPreferredWidths(false);
    }

    /**
     * It adjusts the widths.
     *
     * @deprecated Use {@link #doLayout}.
     */
    @Deprecated
    public void sizeColumnsToFit(boolean lastColumnOnly) {
        int oldAutoResizeMode = autoResizeMode;
        setAutoResizeMode(lastColumnOnly ? AUTO_RESIZE_LAST_COLUMN : AUTO_RESIZE_ALL_COLUMNS);
        sizeColumnsToFit(-1);
        setAutoResizeMode(oldAutoResizeMode);
    }

    /** It adjusts the widths taking that column as the one that is being resized. */
    public void sizeColumnsToFit(int resizingColumn) {
        if (resizingColumn == -1) {
            setWidthsFromPreferredWidths(false);
        } else {
            if (autoResizeMode == AUTO_RESIZE_OFF) {
                TableColumn aColumn = getColumnModel().getColumn(resizingColumn);
                aColumn.setPreferredWidth(aColumn.getWidth());
            } else {
                setWidthsFromPreferredWidths(true);
            }
        }
    }

    /** The tool tip text of the cell that is under the pointer, if the renderer gives one. */
    public String getToolTipText(MouseEvent event) {
        String tip = null;
        Point p = event.getPoint();
        int hitColumnIndex = columnAtPoint(p);
        int hitRowIndex = rowAtPoint(p);
        if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
            TableCellRenderer renderer = getCellRenderer(hitRowIndex, hitColumnIndex);
            Component component = prepareRenderer(renderer, hitRowIndex, hitColumnIndex);
            if (component instanceof JComponent) {
                Rectangle cellRect = getCellRect(hitRowIndex, hitColumnIndex, false);
                p.translate(-cellRect.x, -cellRect.y);
                MouseEvent newEvent = new MouseEvent(component, event.getID(), event.getWhen(),
                        event.getModifiersEx(), p.x, p.y, event.getXOnScreen(),
                        event.getYOnScreen(), event.getClickCount(), event.isPopupTrigger(),
                        MouseEvent.NOBUTTON);
                tip = ((JComponent) component).getToolTipText(newEvent);
            }
        }
        if (tip == null) {
            tip = getToolTipText();
        }
        return tip;
    }

    /** Whether on starting to edit the focus goes to the editor. */
    public void setSurrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) {
        this.surrendersFocusOnKeystroke = surrendersFocusOnKeystroke;
    }

    public boolean getSurrendersFocusOnKeystroke() {
        return surrendersFocusOnKeystroke;
    }

    /** It starts editing that cell, with no gesture behind it. */
    public boolean editCellAt(int row, int column) {
        return editCellAt(row, column, null);
    }

    /**
     * It starts editing that cell because of that gesture.
     *
     * @return whether the editing started
     */
    public boolean editCellAt(int row, int column, EventObject e) {
        if (cellEditor != null && !cellEditor.stopCellEditing()) {
            return false;
        }
        if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) {
            return false;
        }
        if (!isCellEditable(row, column)) {
            return false;
        }
        TableCellEditor editor = getCellEditor(row, column);
        if (editor != null && editor.isCellEditable(e)) {
            editorComp = prepareEditor(editor, row, column);
            if (editorComp == null) {
                removeEditor();
                return false;
            }
            editorComp.setBounds(getCellRect(row, column, false));
            add(editorComp);
            editorComp.validate();
            setCellEditor(editor);
            setEditingRow(row);
            setEditingColumn(column);
            editor.addCellEditorListener(this);
            return true;
        }
        return false;
    }

    public boolean isEditing() {
        return cellEditor != null;
    }

    public Component getEditorComponent() {
        return editorComp;
    }

    public int getEditingColumn() {
        return editingColumn;
    }

    public int getEditingRow() {
        return editingRow;
    }

    public TableUI getUI() {
        return (TableUI) ui;
    }

    public void setUI(TableUI ui) {
        if (this.ui != ui) {
            super.setUI(ui);
            repaint();
        }
    }

    /** It asks for the look and feel again, and tells the header and the editors. */
    public void updateUI() {
        // The header updates itself; the base renderers and editors are rebuilt because their
                // colours come from the look and feel.
        JTableHeader header = getTableHeader();
        if (header != null) {
            header.updateUI();
        }
        createDefaultRenderers();
        createDefaultEditors();
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * It changes the data.
     *
     * <p>If the columns are built by themselves, they are rebuilt: the new model may have
     * others.
     *
     * @throws IllegalArgumentException if the model is null
     */
    public void setModel(TableModel dataModel) {
        if (dataModel == null) {
            throw new IllegalArgumentException("Cannot set a null TableModel");
        }
        if (this.dataModel != dataModel) {
            TableModel old = this.dataModel;
            if (old != null) {
                old.removeTableModelListener(this);
            }
            this.dataModel = dataModel;
            dataModel.addTableModelListener(this);
            tableChanged(new TableModelEvent(dataModel, TableModelEvent.HEADER_ROW));
            firePropertyChange("model", old, dataModel);
            if (getAutoCreateRowSorter()) {
                setRowSorter(new javax.swing.table.TableRowSorter<TableModel>(dataModel));
            }
        }
    }

    public TableModel getModel() {
        return dataModel;
    }

    /**
     * @throws IllegalArgumentException if the model is null
     */
    public void setColumnModel(TableColumnModel columnModel) {
        if (columnModel == null) {
            throw new IllegalArgumentException("Cannot set a null ColumnModel");
        }
        TableColumnModel old = this.columnModel;
        if (columnModel != old) {
            if (old != null) {
                old.removeColumnModelListener(this);
            }
            this.columnModel = columnModel;
            columnModel.addColumnModelListener(this);
            if (tableHeader != null) {
                tableHeader.setColumnModel(columnModel);
            }
            firePropertyChange("columnModel", old, columnModel);
            resizeAndRepaint();
        }
    }

    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    /**
     * @throws IllegalArgumentException if the model is null
     */
    public void setSelectionModel(ListSelectionModel newModel) {
        if (newModel == null) {
            throw new IllegalArgumentException("Cannot set a null SelectionModel");
        }
        ListSelectionModel oldModel = selectionModel;
        if (newModel != oldModel) {
            if (oldModel != null) {
                oldModel.removeListSelectionListener(this);
            }
            selectionModel = newModel;
            newModel.addListSelectionListener(this);
            firePropertyChange("selectionModel", oldModel, newModel);
            repaint();
        }
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /** The order changed: the selection is kept if that was asked for. */
    public void sorterChanged(RowSorterEvent e) {
        if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
            if (getUpdateSelectionOnSort()) {
                // The selection is of view indices, and the view was reordered.
                clearSelection();
            }
        }
        resizeAndRepaint();
    }

    /**
     * The data changed.
     *
     * <p>A change of structure -- row {@link TableModelEvent#HEADER_ROW} -- rebuilds the columns
     * if that is asked for, and clears the selection: the indices no longer mean the same.
     */
    public void tableChanged(TableModelEvent e) {
        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            clearSelectionAndLeadAnchor();
            rowModel = null;
            if (getAutoCreateColumnsFromModel()) {
                createDefaultColumnsFromModel();
                return;
            }
            resizeAndRepaint();
            return;
        }
        if (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) {
            rowModel = null;
        }
        resizeAndRepaint();
    }

    private void clearSelectionAndLeadAnchor() {
        selectionModel.setValueIsAdjusting(true);
        columnModel.getSelectionModel().setValueIsAdjusting(true);
        clearSelection();
        selectionModel.setAnchorSelectionIndex(-1);
        selectionModel.setLeadSelectionIndex(-1);
        columnModel.getSelectionModel().setAnchorSelectionIndex(-1);
        columnModel.getSelectionModel().setLeadSelectionIndex(-1);
        selectionModel.setValueIsAdjusting(false);
        columnModel.getSelectionModel().setValueIsAdjusting(false);
    }

    public void columnAdded(TableColumnModelEvent e) {
        if (isEditing()) {
            removeEditor();
        }
        resizeAndRepaint();
    }

    public void columnRemoved(TableColumnModelEvent e) {
        if (isEditing()) {
            removeEditor();
        }
        resizeAndRepaint();
    }

    public void columnMoved(TableColumnModelEvent e) {
        if (isEditing() && !getCellEditor().stopCellEditing()) {
            getCellEditor().cancelCellEditing();
        }
        repaint();
    }

    public void columnMarginChanged(ChangeEvent e) {
        if (isEditing() && !getCellEditor().stopCellEditing()) {
            getCellEditor().cancelCellEditing();
        }
        TableColumn resizingColumn = getResizingColumn();
        if (resizingColumn != null && autoResizeMode == AUTO_RESIZE_OFF) {
            resizingColumn.setPreferredWidth(resizingColumn.getWidth());
        }
        resizeAndRepaint();
    }

    public void columnSelectionChanged(ListSelectionEvent e) {
        repaint();
    }

    public void valueChanged(ListSelectionEvent e) {
        repaint();
    }

    /** The editor finished: the value is kept and it is removed. */
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object value = editor.getCellEditorValue();
            setValueAt(value, editingRow, editingColumn);
            removeEditor();
        }
    }

    /** The editor was cancelled: nothing is kept. */
    public void editingCanceled(ChangeEvent e) {
        removeEditor();
    }

    public void setPreferredScrollableViewportSize(Dimension size) {
        preferredViewportSize = size;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return preferredViewportSize;
    }

    /** A scroll step is one row or one column, according to the direction. */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            int row = rowAtPoint(new Point(0, visibleRect.y));
            if (row == -1) {
                return 0;
            }
            return getRowHeight(row);
        }
        int col = columnAtPoint(new Point(visibleRect.x, 0));
        if (col == -1) {
            return 100;
        }
        return getColumnModel().getColumn(col).getWidth();
    }

    /** A scroll block is what is seen. */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRect.height;
        }
        return visibleRect.width;
    }

    /** True save with the adjustment switched off: then the horizontal bar appears. */
    public boolean getScrollableTracksViewportWidth() {
        return getAutoResizeMode() != AUTO_RESIZE_OFF;
    }

    /**
     * Whether the table stretches to fill the visible height.
     *
     * <p>Switched off by default, and that is why a table with few rows lets the pane's
     * background be seen below. {@link #setFillsViewportHeight} changes it.
     */
    public boolean getScrollableTracksViewportHeight() {
        java.awt.Container parent = getParent();
        return getFillsViewportHeight() && parent instanceof JViewport
                && parent.getHeight() > getPreferredSize().height;
    }

    public void setFillsViewportHeight(boolean fillsViewportHeight) {
        boolean old = this.fillsViewportHeight;
        this.fillsViewportHeight = fillsViewportHeight;
        resizeAndRepaint();
        firePropertyChange("fillsViewportHeight", old, fillsViewportHeight);
    }

    public boolean getFillsViewportHeight() {
        return fillsViewportHeight;
    }

    /**
     * It gives the key to the editor if there is one open, and otherwise resolves it like any
     * component.
     */
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition,
            boolean pressed) {
        boolean retValue = super.processKeyBinding(ks, e, condition, pressed);
        if (!retValue && condition == WHEN_ANCESTOR_OF_FOCUSED_COMPONENT && isFocusOwner()) {
            return false;
        }
        return retValue;
    }

    /**
     * It builds the base renderers, one per type.
     *
     * <p>The JDK's four: text for {@link Object}, numbers aligned to the right, formatted dates
     * and a tick for the booleans. They are private nested classes and carry the same names as in
     * the JDK on purpose: the name shows through {@code getClass()}, so changing it would be an
     * observable difference with no gain at all.
     */
    protected void createDefaultRenderers() {
        defaultRenderersByColumnClass = new Hashtable<Object, Object>(8);
        defaultRenderersByColumnClass.put(Object.class,
                new DefaultTableCellRenderer.UIResource());
        defaultRenderersByColumnClass.put(Number.class, new NumberRenderer());
        defaultRenderersByColumnClass.put(Float.class, new DoubleRenderer());
        defaultRenderersByColumnClass.put(Double.class, new DoubleRenderer());
        defaultRenderersByColumnClass.put(java.util.Date.class, new DateRenderer());
        defaultRenderersByColumnClass.put(Icon.class, new IconRenderer());
        // The JDK also registers `ImageIcon.class`, which this library does not have yet. It is
                // not named: a class literal of a non-existent type compiles and gives
                // `Object.class` -- see finding #520 --, and that line overwrote Object's renderer.
        defaultRenderersByColumnClass.put(Boolean.class, new BooleanRenderer());
    }

    /**
     * It builds the base editors.
     *
     * <p>{@link Object}'s is not a bare text editor: it is one that <strong>builds the value of
     * the column's type from what is typed</strong>, looking up its {@code String} constructor.
     * It is what makes typing 77 in a column of {@code Integer} return an {@code Integer} and not
     * the string "77", and typing "abc" there not be kept.
     */
    protected void createDefaultEditors() {
        defaultEditorsByColumnClass = new Hashtable<Object, Object>(3);
        defaultEditorsByColumnClass.put(Object.class, new GenericEditor());
        defaultEditorsByColumnClass.put(Number.class, new NumberEditor());
        defaultEditorsByColumnClass.put(Boolean.class, new BooleanEditor());
    }

    /** Numbers to the right, which is how they are read aligned. */
    private static class NumberRenderer extends DefaultTableCellRenderer.UIResource {

        NumberRenderer() {
            super();
            setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    /** Like the numbers one, but formatting the language's decimals. */
    private static class DoubleRenderer extends NumberRenderer {

        private final java.text.NumberFormat formatter =
                java.text.NumberFormat.getInstance();

        DoubleRenderer() {
            super();
        }

        public void setValue(Object value) {
            setText((value == null) ? "" : formatter.format(value));
        }
    }

    /** Dates with the language's format. */
    private static class DateRenderer extends DefaultTableCellRenderer.UIResource {

        private final java.text.DateFormat formatter =
                java.text.DateFormat.getDateInstance();

        DateRenderer() {
            super();
        }

        public void setValue(Object value) {
            setText((value == null) ? "" : formatter.format(value));
        }
    }

    /** A centred icon, with no text. */
    private static class IconRenderer extends DefaultTableCellRenderer.UIResource {

        IconRenderer() {
            super();
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        public void setValue(Object value) {
            setIcon((value instanceof Icon) ? (Icon) value : null);
        }
    }

    /** A centred tick; the only one that is not a label. */
    private static class BooleanRenderer extends JCheckBox implements TableCellRenderer,
            javax.swing.plaf.UIResource {

        BooleanRenderer() {
            super();
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            setSelected((value != null && ((Boolean) value).booleanValue()));
            return this;
        }
    }

    /**
     * The text editor that builds the value of the column's type.
     *
     * <p>It looks up the constructor that takes a {@code String} and calls it with what was
     * typed. If there is no such constructor, that column cannot be edited; if there is and what
     * was typed does not serve, the editing does not finish and the field is marked in red --
     * which is how the JDK says "fix it".
     */
    private static class GenericEditor extends DefaultCellEditor {

        private final Class<?>[] argTypes = new Class<?>[] {String.class};
        private java.lang.reflect.Constructor<?> constructor;
        private Object value;

        GenericEditor() {
            super(new JTextField());
            getComponent().setName("Table.editor");
        }

        public boolean stopCellEditing() {
            String s = (String) super.getCellEditorValue();
            try {
                if ("".equals(s)) {
                    if (constructor.getDeclaringClass() == String.class) {
                        value = s;
                    }
                    return super.stopCellEditing();
                }
                value = constructor.newInstance(new Object[] {s});
            } catch (Exception e) {
                ((JComponent) getComponent()).setBorder(
                        new javax.swing.border.LineBorder(Color.red));
                return false;
            }
            return super.stopCellEditing();
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.value = null;
            ((JComponent) getComponent()).setBorder(
                    new javax.swing.border.LineBorder(Color.black));
            try {
                Class<?> type = table.getColumnClass(column);
                // A column of Object accepts a String: a String is an Object.
                if (type == Object.class) {
                    type = String.class;
                }
                constructor = type.getConstructor(argTypes);
            } catch (Exception e) {
                return null;
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        public Object getCellEditorValue() {
            return value;
        }
    }

    /** The same, with the text on the right. */
    private static class NumberEditor extends GenericEditor {

        NumberEditor() {
            super();
            ((JTextField) getComponent()).setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    /** A tick. */
    private static class BooleanEditor extends DefaultCellEditor {

        BooleanEditor() {
            super(new JCheckBox());
            JCheckBox checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    /** The start-up values; the constructor calls them. */
    protected void initializeLocalVars() {
        setOpaque(true);
        createDefaultRenderers();
        createDefaultEditors();
        setTableHeader(createDefaultTableHeader());
        setShowGrid(true);
        setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setRowHeight(16);
        setRowMargin(1);
        setRowSelectionAllowed(true);
        setCellEditor(null);
        setEditingColumn(-1);
        setEditingRow(-1);
        setSurrendersFocusOnKeystroke(false);
        setPreferredScrollableViewportSize(new Dimension(450, 400));
        // The three colours that in the JDK are set by the look and feel and not by the
                // constructor. Here they are set marked as the look and feel's and with the values
                // measured in Metal (JDK 25), for two reasons: without them a table with no look
                // and feel installed would draw with nulls, and marked they are replaced by
                // `BasicTableUI.installDefaults` without overwriting those the program sets.
        setGridColor(new javax.swing.plaf.ColorUIResource(122, 138, 153));
        setSelectionForeground(new javax.swing.plaf.ColorUIResource(51, 51, 51));
        setSelectionBackground(new javax.swing.plaf.ColorUIResource(184, 207, 229));
    }

    protected TableModel createDefaultDataModel() {
        return new DefaultTableModel();
    }

    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel();
    }

    protected ListSelectionModel createDefaultSelectionModel() {
        return new DefaultListSelectionModel();
    }

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel);
    }

    /** It measures and draws again. */
    protected void resizeAndRepaint() {
        revalidate();
        repaint();
    }

    public TableCellEditor getCellEditor() {
        return cellEditor;
    }

    public void setCellEditor(TableCellEditor anEditor) {
        TableCellEditor old = cellEditor;
        cellEditor = anEditor;
        firePropertyChange("tableCellEditor", old, anEditor);
    }

    public void setEditingColumn(int aColumn) {
        editingColumn = aColumn;
    }

    public void setEditingRow(int aRow) {
        editingRow = aRow;
    }

    /**
     * That cell's renderer.
     *
     * <p>First the column's, if it has one of its own; otherwise, the column type's.
     */
    public TableCellRenderer getCellRenderer(int row, int column) {
        TableColumn tableColumn = getColumnModel().getColumn(column);
        TableCellRenderer renderer = tableColumn.getCellRenderer();
        if (renderer == null) {
            renderer = getDefaultRenderer(getColumnClass(column));
        }
        return renderer;
    }

    /** The renderer already configured for that cell. */
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Object value = getValueAt(row, column);
        boolean isSelected = false;
        boolean hasFocus = false;
        if (!isPaintingForPrint()) {
            isSelected = isCellSelected(row, column);
            boolean rowIsLead = (selectionModel.getLeadSelectionIndex() == row);
            boolean colIsLead = (columnModel.getSelectionModel().getLeadSelectionIndex()
                    == column);
            hasFocus = (rowIsLead && colIsLead) && isFocusOwner();
        }
        return renderer.getTableCellRendererComponent(this, value, isSelected, hasFocus, row,
                column);
    }

    /** That cell's editor; like {@link #getCellRenderer}, the column's first. */
    public TableCellEditor getCellEditor(int row, int column) {
        TableColumn tableColumn = getColumnModel().getColumn(column);
        TableCellEditor editor = tableColumn.getCellEditor();
        if (editor == null) {
            editor = getDefaultEditor(getColumnClass(column));
        }
        return editor;
    }

    /** The editor already loaded with that cell's value. */
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Object value = getValueAt(row, column);
        boolean isSelected = isCellSelected(row, column);
        Component comp = editor.getTableCellEditorComponent(this, value, isSelected, row,
                column);
        if (comp instanceof JComponent) {
            JComponent jComp = (JComponent) comp;
            if (jComp.getNextFocusableComponent() == null) {
                jComp.setNextFocusableComponent(this);
            }
        }
        return comp;
    }

    /** It removes the editor and goes back to the state of not editing. */
    public void removeEditor() {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            editor.removeCellEditorListener(this);
            if (editorComp != null) {
                remove(editorComp);
            }
            Rectangle cellRect = getCellRect(editingRow, editingColumn, false);
            setCellEditor(null);
            setEditingColumn(-1);
            setEditingRow(-1);
            editorComp = null;
            repaint(cellRect);
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    /**
     * It prints the table.
     *
     * <p><strong>With no printer there is no printing.</strong> This library runs with no screen
     * and no printing service, so these methods build the job and fail on asking for the printer,
     * which is what the JDK does in the same situation. {@link #getPrintable} does serve: it
     * returns something that knows how to draw the table on any {@code Graphics}, a PDF's
     * included.
     *
     * @throws PrinterException if it cannot print
     */
    public boolean print() throws PrinterException {
        return print(PrintMode.FIT_WIDTH);
    }

    /**
     * @throws PrinterException if it cannot print
     */
    public boolean print(PrintMode printMode) throws PrinterException {
        return print(printMode, null, null);
    }

    /**
     * @throws PrinterException if it cannot print
     */
    public boolean print(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat) throws PrinterException {
        return print(printMode, headerFormat, footerFormat, true, null, true);
    }

    /**
     * @throws PrinterException if it cannot print
     * @throws java.awt.HeadlessException if interaction is asked for and there is no screen
     */
    public boolean print(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat, boolean showPrintDialog,
            javax.print.attribute.PrintRequestAttributeSet attr, boolean interactive)
            throws PrinterException, java.awt.HeadlessException {
        return print(printMode, headerFormat, footerFormat, showPrintDialog, attr, interactive,
                null);
    }

    /**
     * @throws PrinterException if it cannot print
     * @throws java.awt.HeadlessException if interaction is asked for and there is no screen
     */
    public boolean print(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat, boolean showPrintDialog,
            javax.print.attribute.PrintRequestAttributeSet attr, boolean interactive,
            javax.print.PrintService service) throws PrinterException,
            java.awt.HeadlessException {
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        if (service != null) {
            job.setPrintService(service);
        }
        job.setPrintable(getPrintable(printMode, headerFormat, footerFormat));
        if (showPrintDialog && !job.printDialog()) {
            return false;
        }
        job.print();
        return true;
    }

    /**
     * Something that knows how to draw the table, page by page.
     *
     * @throws NullPointerException if the mode is null
     */
    public Printable getPrintable(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat) {
        if (printMode == null) {
            throw new NullPointerException("printMode");
        }
        return new TablePrintable(this, printMode, headerFormat, footerFormat);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /** How the table is shared out into pages. */
    public enum PrintMode {

        /** The columns that do not fit across go on separate pages. */
        NORMAL,

        /** Everything is shrunk so that the width fits on one page. */
        FIT_WIDTH;
    }

    /**
     * Where what is being dragged would fall.
     *
     * <p>A row and a column, and two flags that say whether it is <em>over</em> that cell or
     * <em>between</em> two. The difference matters: dropping over a row replaces it, dropping
     * between two inserts.
     */
    public static final class DropLocation extends TransferHandler.DropLocation {

        private final int row;
        private final int col;
        private final boolean isInsertRow;
        private final boolean isInsertCol;

        DropLocation(Point p, int row, int col, boolean isInsertRow, boolean isInsertCol) {
            super(p);
            this.row = row;
            this.col = col;
            this.isInsertRow = isInsertRow;
            this.isInsertCol = isInsertCol;
        }

        /** The row, in view indices; -1 if there is none. */
        public int getRow() {
            return row;
        }

        /** The column, in view indices; -1 if there is none. */
        public int getColumn() {
            return col;
        }

        /** Whether it is between two rows and not over one. */
        public boolean isInsertRow() {
            return isInsertRow;
        }

        /** Whether it is between two columns and not over one. */
        public boolean isInsertColumn() {
            return isInsertCol;
        }

        public String toString() {
            return getClass().getName() + "[dropPoint=" + getDropPoint() + ","
                    + "row=" + row + ","
                    + "column=" + col + ","
                    + "insertRow=" + isInsertRow + ","
                    + "insertColumn=" + isInsertCol + "]";
        }
    }

    /**
     * It draws the table page by page.
     *
     * <p>It shares the rows out according to what fits down and, in {@link PrintMode#NORMAL}
     * mode, the columns according to what fits across. It draws no cut lines nor headers repeated
     * beyond what the two formats say.
     */
    private static class TablePrintable implements Printable {

        private final JTable table;
        private final PrintMode printMode;
        private final MessageFormat headerFormat;
        private final MessageFormat footerFormat;

        TablePrintable(JTable table, PrintMode printMode, MessageFormat headerFormat,
                MessageFormat footerFormat) {
            this.table = table;
            this.printMode = printMode;
            this.headerFormat = headerFormat;
            this.footerFormat = footerFormat;
        }

        public int print(java.awt.Graphics graphics, java.awt.print.PageFormat pageFormat,
                int pageIndex) throws PrinterException {
            int height = (int) pageFormat.getImageableHeight();
            int perPage = Math.max(1, height / Math.max(1, table.getRowHeight()));
            int first = pageIndex * perPage;
            if (first >= table.getRowCount()) {
                return NO_SUCH_PAGE;
            }
            graphics.translate((int) pageFormat.getImageableX(),
                    (int) pageFormat.getImageableY());
            java.awt.Graphics g2 = graphics.create();
            g2.translate(0, -first * table.getRowHeight());
            table.paint(g2);
            g2.dispose();
            return PAGE_EXISTS;
        }
    }
}
