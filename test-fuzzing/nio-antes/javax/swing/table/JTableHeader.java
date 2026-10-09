package javax.swing.table;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TableHeaderUI;

/**
 * A table's title bar.
 *
 * <h2>It is a separate component, and for a concrete reason</h2>
 *
 * <p>Inside a pane with scroll bars, the header goes in the strip at the top and the table in
 * the part that scrolls. If they were a single component, the titles would go up on scrolling
 * down. That they are two is what keeps the titles fixed.
 *
 * <p>Hence it shares the <em>column model</em> with its table and not the data: all it needs to
 * know is which columns there are and how much they measure.
 *
 * <h2>Two different permissions</h2>
 *
 * <p>{@link #setReorderingAllowed} allows dragging a column elsewhere;
 * {@link #setResizingAllowed} allows changing its width by pulling the edge. They are
 * independent: it is common to allow changing the width and not the order.
 *
 * <h2>What is seen while dragging</h2>
 *
 * <p>{@link #getDraggedColumn} and {@link #getDraggedDistance} are the state of a drag in
 * progress, and the look and feel reads them to draw the half-moved column. With no look and
 * feel installed nobody writes them, and they stay null and zero.
 */
public class JTableHeader extends JComponent implements TableColumnModelListener, Accessible {

    private static final String uiClassID = "TableHeaderUI";

    /** The table it belongs to, or null. */
    protected JTable table;

    /** The column model, shared with the table. */
    protected TableColumnModel columnModel;

    /** Whether columns can be dragged elsewhere. */
    protected boolean reorderingAllowed;

    /** Whether their width can be changed. */
    protected boolean resizingAllowed;

    /** Whether the table rearranges itself while dragging, or only on release. */
    protected boolean updateTableInRealTime;

    /** The column whose width is being changed, or null. */
    protected transient TableColumn resizingColumn;

    /** The column being dragged, or null. */
    protected transient TableColumn draggedColumn;

    /** How far it has been dragged. */
    protected transient int draggedDistance;

    private TableCellRenderer defaultRenderer;

    /** A header with a column model of its own. */
    public JTableHeader() {
        this(null);
    }

    /** A header over that column model; null builds an empty one. */
    public JTableHeader(TableColumnModel cm) {
        super();
        if (cm == null) {
            cm = createDefaultColumnModel();
        }
        setColumnModel(cm);
        initializeLocalVars();
        updateUI();
    }

    /** The table it belongs to; the table sets it, not the caller. */
    public void setTable(JTable table) {
        JTable old = this.table;
        this.table = table;
        firePropertyChange("table", old, table);
    }

    public JTable getTable() {
        return table;
    }

    /** Whether columns can be dragged; see the class note. */
    public void setReorderingAllowed(boolean reorderingAllowed) {
        boolean old = this.reorderingAllowed;
        this.reorderingAllowed = reorderingAllowed;
        firePropertyChange("reorderingAllowed", old, reorderingAllowed);
    }

    public boolean getReorderingAllowed() {
        return reorderingAllowed;
    }

    /** Whether their width can be changed. */
    public void setResizingAllowed(boolean resizingAllowed) {
        boolean old = this.resizingAllowed;
        this.resizingAllowed = resizingAllowed;
        firePropertyChange("resizingAllowed", old, resizingAllowed);
    }

    public boolean getResizingAllowed() {
        return resizingAllowed;
    }

    public TableColumn getDraggedColumn() {
        return draggedColumn;
    }

    public int getDraggedDistance() {
        return draggedDistance;
    }

    public TableColumn getResizingColumn() {
        return resizingColumn;
    }

    /**
     * Whether the table rearranges itself while dragging.
     *
     * @deprecated As in the JDK: the value is kept and returned, and nobody looks at it any more.
     */
    @Deprecated
    public void setUpdateTableInRealTime(boolean flag) {
        updateTableInRealTime = flag;
    }

    /**
     * @deprecated Ver {@link #setUpdateTableInRealTime}.
     */
    @Deprecated
    public boolean getUpdateTableInRealTime() {
        return updateTableInRealTime;
    }

    /** The titles' renderer; null gives the decision back to the look and feel. */
    public void setDefaultRenderer(TableCellRenderer defaultRenderer) {
        this.defaultRenderer = defaultRenderer;
    }

    public TableCellRenderer getDefaultRenderer() {
        return defaultRenderer;
    }

    /** The column that falls on that point, or -1. */
    public int columnAtPoint(Point point) {
        int x = point.x;
        if (!getComponentOrientation().isLeftToRight()) {
            x = getWidthInRightToLeft() - x - 1;
        }
        return getColumnModel().getColumnIndexAtX(x);
    }

    private int getWidthInRightToLeft() {
        if ((table != null) && (table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF)) {
            return table.getWidth();
        }
        return super.getWidth();
    }

    /**
     * That column's title rectangle.
     *
     * <p>An index out of range returns an empty rectangle at the position it would have, not an
     * error: the look and feel asks for it while drawing and has no way of knowing where it ends.
     */
    public Rectangle getHeaderRect(int column) {
        Rectangle r = new Rectangle();
        TableColumnModel cm = getColumnModel();
        r.height = getHeight();
        if (column < 0) {
            if (!getComponentOrientation().isLeftToRight()) {
                r.x = getWidthInRightToLeft();
            }
        } else if (column >= cm.getColumnCount()) {
            if (getComponentOrientation().isLeftToRight()) {
                r.x = getWidth();
            }
        } else {
            for (int i = 0; i < column; i++) {
                r.x = r.x + cm.getColumn(i).getWidth();
            }
            if (!getComponentOrientation().isLeftToRight()) {
                r.x = getWidthInRightToLeft() - r.x - cm.getColumn(column).getWidth();
            }
            r.width = cm.getColumn(column).getWidth();
        }
        return r;
    }

    /** The tooltip text of the title under the pointer, if its renderer gives one. */
    public String getToolTipText(MouseEvent event) {
        String tip = null;
        Point p = event.getPoint();
        int column = columnAtPoint(p);
        if (column != -1) {
            TableColumn aColumn = columnModel.getColumn(column);
            TableCellRenderer renderer = aColumn.getHeaderRenderer();
            if (renderer == null) {
                renderer = defaultRenderer;
            }
            if (renderer != null) {
                java.awt.Component component = renderer.getTableCellRendererComponent(
                        getTable(), aColumn.getHeaderValue(), false, false, -1, column);
                if (component instanceof JComponent) {
                    Rectangle cellRect = getHeaderRect(column);
                    p.translate(-cellRect.x, -cellRect.y);
                    MouseEvent newEvent = new MouseEvent(component, event.getID(),
                            event.getWhen(), event.getModifiersEx(), p.x, p.y,
                            event.getXOnScreen(), event.getYOnScreen(), event.getClickCount(),
                            event.isPopupTrigger(), MouseEvent.NOBUTTON);
                    tip = ((JComponent) component).getToolTipText(newEvent);
                }
            }
        }
        if (tip == null) {
            tip = getToolTipText();
        }
        return tip;
    }

    /**
     * How much it takes up: as wide as the sum of the columns.
     *
     * <p>The height is decided by the look and feel; with no look and feel it stays at whatever it
     * has set.
     */
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        long width = 0;
        java.util.Enumeration<TableColumn> enumeration = columnModel.getColumns();
        while (enumeration.hasMoreElements()) {
            TableColumn aColumn = enumeration.nextElement();
            width = width + aColumn.getPreferredWidth();
        }
        Dimension d = super.getPreferredSize();
        return new Dimension((int) Math.min(width, Integer.MAX_VALUE), d.height);
    }

    public TableHeaderUI getUI() {
        return (TableHeaderUI) ui;
    }

    public void setUI(TableHeaderUI ui) {
        if (this.ui != ui) {
            super.setUI(ui);
            repaint();
        }
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Changes the column model.
     *
     * @throws IllegalArgumentException if it is null
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
            firePropertyChange("columnModel", old, columnModel);
            resizeAndRepaint();
        }
    }

    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    public void columnAdded(TableColumnModelEvent e) {
        resizeAndRepaint();
    }

    public void columnRemoved(TableColumnModelEvent e) {
        resizeAndRepaint();
    }

    public void columnMoved(TableColumnModelEvent e) {
        repaint();
    }

    public void columnMarginChanged(ChangeEvent e) {
        resizeAndRepaint();
    }

    /** It does nothing: the header does not show the column selection. */
    public void columnSelectionChanged(ListSelectionEvent e) {
    }

    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel();
    }

    /**
     * The base title renderer.
     *
     * <p>In the JDK it is a subclass of {@link DefaultTableCellRenderer}, centred and with the look
     * and feel's border. Here it is the same cell renderer: with no look and feel installed there
     * is no header border to ask for, and the text looks the same.
     */
    protected TableCellRenderer createDefaultRenderer() {
        DefaultTableCellRenderer label = new DefaultTableCellRenderer.UIResource();
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        return label;
    }

    /** The starting values; the constructor calls it. */
    protected void initializeLocalVars() {
        setOpaque(true);
        table = null;
        reorderingAllowed = true;
        resizingAllowed = true;
        draggedColumn = null;
        draggedDistance = 0;
        resizingColumn = null;
        updateTableInRealTime = true;
        setDefaultRenderer(createDefaultRenderer());
    }

    /** It measures and draws again. */
    public void resizeAndRepaint() {
        revalidate();
        repaint();
    }

    /** Notes which column is being dragged; the look and feel writes it. */
    public void setDraggedColumn(TableColumn aColumn) {
        draggedColumn = aColumn;
    }

    /** Notes how far it has been dragged. */
    public void setDraggedDistance(int distance) {
        draggedDistance = distance;
    }

    /** Notes which column is being resized. */
    public void setResizingColumn(TableColumn aColumn) {
        resizingColumn = aColumn;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
