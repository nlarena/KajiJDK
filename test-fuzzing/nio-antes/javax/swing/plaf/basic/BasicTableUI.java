package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.TableUI;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * The basic look and feel of a table.
 *
 * <h2>The height comes from the last row, not from a multiplication</h2>
 *
 * <p>It could be {@code rows * rowHeight}, and it is not: {@link #getPreferredSize} asks the
 * table where the last row ends. The difference matters as soon as the rows stop all measuring
 * the same -- a table with per-row heights, which is what {@code JTable.setRowHeight(row,
 * height)} does --, and there the multiplication would give anything.
 *
 * <p>The width is a sum: the columns' preferred widths. The minimum and the maximum are the
 * same arithmetic with the minimums and the maximums, and all three share the height: a table
 * does not stretch vertically on its own, it is stretched by whatever contains it.
 *
 * <p>A table with no rows and no columns measures zero by zero in all three.
 *
 * <h2>The baseline's renderer is a single one</h2>
 *
 * <p>And it has nothing to do with the content: it is loaded with a letter and asked. It has to
 * be like that -- whoever asks for the baseline is a layout, which calls before there is any
 * data -- and besides it would be terribly expensive to do it with the real first cell.
 *
 * <h2>No key listener</h2>
 *
 * <p>{@link #keyListener} is left {@code null}. A table's arrow navigation is not a key
 * listener: they are forty-four named actions in the action map, tied to keys by the look and
 * feel's table. Measured, and it is the same story as in
 * {@link BasicMenuItemUI#createMenuKeyListener}.
 *
 * <h2>What is left said</h2>
 *
 * <p>The keys are not tied: the actions exist in the map, but which key fires which comes from
 * the look and feel's table, and with no table there is none. It is the same gap as in
 * {@link BasicDesktopPaneUI}.
 */
public class BasicTableUI extends TableUI {

    protected JTable table;
    protected CellRendererPane rendererPane;
    protected KeyListener keyListener;
    protected FocusListener focusListener;
    protected MouseInputListener mouseInputListener;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource SELECTION = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource GRID = new ColorUIResource(122, 138, 153);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.PLAIN, 12);

    /** The renderer the baseline is measured with; see the class note. */
    private static Component baseRenderer;

    public BasicTableUI() {
    }

    /** A new one per table: it keeps the component and its renderer pane. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTableUI();
    }

    public void installUI(JComponent c) {
        table = (JTable) c;
        rendererPane = new CellRendererPane();
        table.add(rendererPane);
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        table.remove(rendererPane);
        rendererPane = null;
        table = null;
    }

    /** Colours, typeface and grid; the values are those of {@code Table.*} in Metal. */
    protected void installDefaults() {
        Color background = table.getBackground();
        if (background == null || background instanceof UIResource) {
            table.setBackground(BACKGROUND);
        }
        Color foreground = table.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            table.setForeground(FOREGROUND);
        }
        Font font = table.getFont();
        if (font == null || font instanceof UIResource) {
            table.setFont(FONT);
        }
        Color sbg = table.getSelectionBackground();
        if (sbg == null || sbg instanceof UIResource) {
            table.setSelectionBackground(SELECTION);
        }
        Color sfg = table.getSelectionForeground();
        if (sfg == null || sfg instanceof UIResource) {
            table.setSelectionForeground(FOREGROUND);
        }
        Color grid = table.getGridColor();
        if (grid == null || grid instanceof UIResource) {
            table.setGridColor(GRID);
        }
        LookAndFeel.installProperty(table, "opaque", Boolean.TRUE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        focusListener = createFocusListener();
        keyListener = createKeyListener();
        mouseInputListener = createMouseInputListener();
        table.addFocusListener(focusListener);
        if (keyListener != null) {
            table.addKeyListener(keyListener);
        }
        table.addMouseListener(mouseInputListener);
        table.addMouseMotionListener(mouseInputListener);
    }

    protected void uninstallListeners() {
        table.removeFocusListener(focusListener);
        if (keyListener != null) {
            table.removeKeyListener(keyListener);
        }
        table.removeMouseListener(mouseInputListener);
        table.removeMouseMotionListener(mouseInputListener);
        focusListener = null;
        keyListener = null;
        mouseInputListener = null;
    }

    /** The named actions; see the class note about the keys. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected FocusListener createFocusListener() {
        return new Handler();
    }

    /** None; see the class note. */
    protected KeyListener createKeyListener() {
        return null;
    }

    protected MouseInputListener createMouseInputListener() {
        return new Handler();
    }

    /** The requested width and the last row's height; see the class note. */
    private Dimension size(long width) {
        int height = 0;
        int rowCount = table.getRowCount();
        if (rowCount > 0 && table.getColumnCount() > 0) {
            Rectangle r = table.getCellRect(rowCount - 1, 0, true);
            height = r.y + r.height;
        }
        if (width > Integer.MAX_VALUE) {
            width = Integer.MAX_VALUE;
        }
        return new Dimension((int) width, height);
    }

    public Dimension getMinimumSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = table.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getMinWidth();
        }
        return size(width);
    }

    public Dimension getPreferredSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = table.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getPreferredWidth();
        }
        return size(width);
    }

    public Dimension getMaximumSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = table.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getMaxWidth();
        }
        return size(width);
    }

    /**
     * Where the first row's text rests; see the class note.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        Component renderer = baseRenderer;
        if (renderer == null) {
            javax.swing.table.DefaultTableCellRenderer tableRenderer =
                    new javax.swing.table.DefaultTableCellRenderer();
            renderer = tableRenderer.getTableCellRendererComponent(table, "a", false, false,
                    -1, -1);
            baseRenderer = renderer;
        }
        renderer.setFont(table.getFont());
        int rowMargin = table.getRowMargin();
        return renderer.getBaseline(Integer.MAX_VALUE, table.getRowHeight() - rowMargin)
                + rowMargin / 2;
    }

    /**
     * {@code CONSTANT_ASCENT}: the first row is always at the very top.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    /** The grid and the cells that are seen. */
    public void paint(Graphics g, JComponent c) {
        Rectangle clip = g.getClipBounds();
        Rectangle bounds = table.getBounds();
        bounds.x = 0;
        bounds.y = 0;
        if (table.getRowCount() <= 0 || table.getColumnCount() <= 0
                || !bounds.intersects(clip)) {
            return;
        }
        java.awt.Point upperLeft = clip.getLocation();
        java.awt.Point lowerRight = new java.awt.Point(clip.x + clip.width - 1,
                clip.y + clip.height - 1);
        int rMin = table.rowAtPoint(upperLeft);
        int rMax = table.rowAtPoint(lowerRight);
        if (rMin == -1) {
            rMin = 0;
        }
        if (rMax == -1) {
            rMax = table.getRowCount() - 1;
        }
        int cMin = table.columnAtPoint(upperLeft);
        int cMax = table.columnAtPoint(lowerRight);
        if (cMin == -1) {
            cMin = 0;
        }
        if (cMax == -1) {
            cMax = table.getColumnCount() - 1;
        }

        paintGrid(g, rMin, rMax, cMin, cMax);
        paintCells(g, rMin, rMax, cMin, cMax);
        rendererPane.removeAll();
    }

    private void paintGrid(Graphics g, int rMin, int rMax, int cMin, int cMax) {
        g.setColor(table.getGridColor());
        Rectangle minCell = table.getCellRect(rMin, cMin, true);
        Rectangle maxCell = table.getCellRect(rMax, cMax, true);
        Rectangle damaged = minCell.union(maxCell);

        if (table.getShowHorizontalLines()) {
            int tableWidth = damaged.x + damaged.width;
            int y = damaged.y;
            for (int row = rMin; row <= rMax; row++) {
                y += table.getRowHeight(row);
                g.drawLine(damaged.x, y - 1, tableWidth - 1, y - 1);
            }
        }
        if (table.getShowVerticalLines()) {
            int tableHeight = damaged.y + damaged.height;
            int x = damaged.x;
            for (int column = cMin; column <= cMax; column++) {
                x += table.getColumnModel().getColumn(column).getWidth();
                g.drawLine(x - 1, damaged.y, x - 1, tableHeight - 1);
            }
        }
    }

    private void paintCells(Graphics g, int rMin, int rMax, int cMin, int cMax) {
        for (int row = rMin; row <= rMax; row++) {
            for (int column = cMin; column <= cMax; column++) {
                Rectangle cellRect = table.getCellRect(row, column, false);
                paintCell(g, cellRect, row, column);
            }
        }
    }

    private void paintCell(Graphics g, Rectangle cellRect, int row, int column) {
        if (table.isEditing() && table.getEditingRow() == row
                && table.getEditingColumn() == column) {
            Component component = table.getEditorComponent();
            component.setBounds(cellRect);
            component.validate();
            return;
        }
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        Component component = table.prepareRenderer(renderer, row, column);
        rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y,
                cellRect.width, cellRect.height, true);
    }

    /**
     * The one that listens to the focus and to the mouse.
     *
     * <p>Selection with the mouse is resolved here and not in the table, because it depends on the
     * keys that are held down at the same time -- control adds, shift extends -- and that is a
     * platform convention, not the model's.
     */
    private class Handler implements FocusListener, MouseInputListener {

        public void focusGained(FocusEvent e) {
            repaintSelection();
        }

        public void focusLost(FocusEvent e) {
            repaintSelection();
        }

        private void repaintSelection() {
            int row = table.getSelectionModel().getLeadSelectionIndex();
            int col = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            if (row >= 0 && col >= 0) {
                table.repaint(table.getCellRect(row, col, false));
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!table.isEnabled()) {
                return;
            }
            if (!table.hasFocus() && table.isRequestFocusEnabled()) {
                table.requestFocus();
            }
            java.awt.Point p = e.getPoint();
            int row = table.rowAtPoint(p);
            int column = table.columnAtPoint(p);
            if (row == -1 || column == -1) {
                return;
            }
            if (table.editCellAt(row, column, e)) {
                Component editor = table.getEditorComponent();
                if (editor != null) {
                    editor.requestFocus();
                }
                return;
            }
            if (e.isControlDown()) {
                table.changeSelection(row, column, true, false);
            } else if (e.isShiftDown()) {
                table.changeSelection(row, column, false, true);
            } else {
                table.changeSelection(row, column, false, false);
            }
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (!table.isEnabled()) {
                return;
            }
            java.awt.Point p = e.getPoint();
            int row = table.rowAtPoint(p);
            int column = table.columnAtPoint(p);
            if (row == -1 || column == -1) {
                return;
            }
            table.changeSelection(row, column, false, true);
        }

        public void mouseMoved(MouseEvent e) {
        }
    }
}
