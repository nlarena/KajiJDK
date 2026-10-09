package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * The basic look and feel of a table's header.
 *
 * <h2>The height is decided by the tallest title</h2>
 *
 * <p>The preferred width is the sum of the columns' preferred widths, and the height is that of
 * the tallest title of all. That means that adding a column with a two-line title enlarges the
 * whole header, which is what is right: the titles go aligned.
 *
 * <p>A header with no columns measures zero by zero, not the margins.
 *
 * <h2>The three sizes do not share the height by chance</h2>
 *
 * <p>Minimum, preferred and maximum differ only in the width -- the sum of the minimums, of the
 * preferred ones and of the maximums --; the height is the same in all three. A header neither
 * stretches nor shrinks vertically: the titles fit or they do not fit.
 *
 * <h2>The column under the mouse</h2>
 *
 * <p>{@link #getRolloverColumn} and {@link #rolloverColumnUpdated} are the hook for a look and
 * feel to draw the column with the mouse over it differently. The basic one draws nothing
 * differently, but it keeps count all the same, because the one that decides is the subclass's
 * {@code paint}.
 *
 * <h2>What is left said</h2>
 *
 * <p>Dragging columns -- moving a column somewhere else by pulling its title, and changing its
 * width by pulling the border -- needs the system cursor and a screen. The mouse listener is
 * there and notes the column, but it does not drag.
 */
public class BasicTableHeaderUI extends TableHeaderUI {

    protected JTableHeader header;
    protected CellRendererPane rendererPane;
    protected MouseInputListener mouseInputListener;

    private int rolloverColumn = -1;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicTableHeaderUI() {
    }

    /** A new one per header: it keeps the component and its renderer pane. */
    public static ComponentUI createUI(JComponent h) {
        return new BasicTableHeaderUI();
    }

    public void installUI(JComponent c) {
        header = (JTableHeader) c;
        rendererPane = new CellRendererPane();
        header.add(rendererPane);
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        header.remove(rendererPane);
        rendererPane = null;
        header = null;
    }

    /** Colours and typeface; the values are those of {@code TableHeader.*} in Metal. */
    protected void installDefaults() {
        Color background = header.getBackground();
        if (background == null || background instanceof UIResource) {
            header.setBackground(BACKGROUND);
        }
        Color foreground = header.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            header.setForeground(FOREGROUND);
        }
        Font font = header.getFont();
        if (font == null || font instanceof UIResource) {
            header.setFont(FONT);
        }
        LookAndFeel.installProperty(header, "opaque", Boolean.TRUE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        mouseInputListener = createMouseInputListener();
        header.addMouseListener(mouseInputListener);
        header.addMouseMotionListener(mouseInputListener);
    }

    protected void uninstallListeners() {
        header.removeMouseListener(mouseInputListener);
        header.removeMouseMotionListener(mouseInputListener);
        mouseInputListener = null;
    }

    /** With no shortcuts of its own. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected MouseInputListener createMouseInputListener() {
        return new Handler();
    }

    /** The column with the mouse over it, or -1. */
    protected int getRolloverColumn() {
        return rolloverColumn;
    }

    /**
     * Notice that the mouse went from one column to another.
     *
     * <p>The basic one redraws both; a subclass that paints them differently does not have to do
     * anything else.
     */
    protected void rolloverColumnUpdated(int oldColumn, int newColumn) {
        rolloverColumn = newColumn;
        repaintColumn(oldColumn);
        repaintColumn(newColumn);
    }

    private void repaintColumn(int column) {
        if (column < 0 || header == null) {
            return;
        }
        Rectangle r = header.getHeaderRect(column);
        header.repaint(r.x, 0, r.width, header.getHeight());
    }

    /** That column's renderer, already loaded with its title. */
    private Component headerRenderer(int column) {
        TableColumn aColumn = header.getColumnModel().getColumn(column);
        TableCellRenderer renderer = aColumn.getHeaderRenderer();
        if (renderer == null) {
            renderer = header.getDefaultRenderer();
        }
        boolean hasFocus = !header.isPaintingForPrint()
                && (column == getRolloverColumn())
                && (header.getDraggedColumn() == null);
        return renderer.getTableCellRendererComponent(header.getTable(),
                aColumn.getHeaderValue(), false, hasFocus, -1, column);
    }

    /** The tallest title's height; see the class note. */
    private int headerHeight() {
        int height = 0;
        TableColumnModel columnModel = header.getColumnModel();
        for (int column = 0; column < columnModel.getColumnCount(); column++) {
            Component comp = headerRenderer(column);
            height = Math.max(height, comp.getPreferredSize().height);
        }
        return height;
    }

    /**
     * The size from an already added-up width.
     *
     * <p>The gap between columns is <em>not</em> added. The JDK has a comment there saying that
     * the callers add it, and none of them adds it; the measured result is the bare sum of the
     * widths. It is copied like that: adding it would give a header two pixels wider than the
     * table.
     */
    private Dimension size(long width) {
        if (width > Integer.MAX_VALUE) {
            width = Integer.MAX_VALUE;
        }
        return new Dimension((int) width, headerHeight());
    }

    /** The sum of the columns' minimums. */
    public Dimension getMinimumSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = header.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getMinWidth();
        }
        return size(width);
    }

    /** The sum of the preferred ones. */
    public Dimension getPreferredSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = header.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getPreferredWidth();
        }
        return size(width);
    }

    /** The sum of the maximums, with a cap. */
    public Dimension getMaximumSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = header.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getMaxWidth();
        }
        return size(width);
    }

    /**
     * Where the titles' text rests.
     *
     * <p>-1 if two columns do not agree: a header with titles that rest at different heights does
     * not have a baseline, and answering the first one's would be lying.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        int baseline = -1;
        TableColumnModel columnModel = header.getColumnModel();
        for (int column = 0; column < columnModel.getColumnCount(); column++) {
            Component comp = headerRenderer(column);
            Dimension pref = comp.getPreferredSize();
            int columnBaseline = comp.getBaseline(pref.width, height);
            if (columnBaseline >= 0) {
                if (baseline == -1) {
                    baseline = columnBaseline;
                } else if (baseline != columnBaseline) {
                    baseline = -1;
                    break;
                }
            }
        }
        return baseline;
    }

    /** The titles of the columns that are seen. */
    public void paint(Graphics g, JComponent c) {
        if (header.getColumnModel().getColumnCount() <= 0) {
            return;
        }
        Rectangle clip = g.getClipBounds();
        TableColumnModel cm = header.getColumnModel();
        int columnMargin = cm.getColumnMargin();
        Rectangle cellRect = header.getHeaderRect(0);
        cellRect.y = 0;
        cellRect.height = header.getHeight();
        for (int column = 0; column < cm.getColumnCount(); column++) {
            TableColumn aColumn = cm.getColumn(column);
            cellRect.width = aColumn.getWidth();
            if (cellRect.intersects(clip)) {
                Component comp = headerRenderer(column);
                rendererPane.paintComponent(g, comp, header,
                        cellRect.x, cellRect.y,
                        cellRect.width - columnMargin, cellRect.height, true);
            }
            cellRect.x += cellRect.width;
        }
        rendererPane.removeAll();
    }

    /** It keeps track of which column the mouse is on; see the class note. */
    private class Handler implements MouseInputListener {

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            update(e);
        }

        public void mouseExited(MouseEvent e) {
            int old = rolloverColumn;
            if (old != -1) {
                rolloverColumnUpdated(old, -1);
            }
        }

        public void mouseMoved(MouseEvent e) {
            update(e);
        }

        public void mouseDragged(MouseEvent e) {
        }

        private void update(MouseEvent e) {
            int column = header.columnAtPoint(e.getPoint());
            if (column != rolloverColumn) {
                rolloverColumnUpdated(rolloverColumn, column);
            }
        }
    }
}
