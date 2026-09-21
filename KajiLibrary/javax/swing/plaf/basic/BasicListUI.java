package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.LookAndFeel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ListUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a list.
 *
 * <h2>The table of heights, and why there is not a single height</h2>
 *
 * <p>A list may have all its rows the same or each one of a different height, and the
 * difference changes everything else. With a fixed height, knowing which row falls at a
 * coordinate is a division; with different heights one has to walk along adding up. That is why
 * there are two fields and only one is set at a time: {@link #cellHeight} holds when they are
 * all the same and {@link #cellHeights} when they are not, and the other is left at -1 or at
 * {@code null}.
 *
 * <p>Measuring each row costs: the renderer has to be built with the row's value and asked how
 * much it measures. For a list of ten thousand elements that is ten thousand measurements, and
 * that is why they are not made until they are needed: {@link #updateLayoutStateNeeded} gathers
 * the reasons the table went stale -- the model, the typeface, the renderer changed -- and
 * {@link #maybeUpdateLayoutState} rebuilds it just once, right before somebody asks something
 * that depends on it.
 *
 * <p>The reasons are bit flags because they pile up: between two paints the model and the
 * typeface may change, and both have to be noted.
 *
 * <h2>The pane that is not a pane</h2>
 *
 * <p>{@link #rendererPane} exists because a cell renderer is a real component that is in no
 * window. In order to paint it, it has to be given a parent -- otherwise the painting chain
 * does not work -- and that parent must not take part in the layout nor in the handing out of
 * events. That is exactly {@link CellRendererPane}.
 *
 * <h2>Coordinates there and back</h2>
 *
 * <p>{@link #convertYToRow} and {@link #convertRowToY} are the two halves of the same
 * arithmetic, and both have odd edges that are measured: a coordinate below the end returns the
 * last row, and one above the beginning <em>also</em> does -- the walk finds nothing and keeps
 * the last one --. It is the JDK's and it is copied.
 *
 * <h2>What is left said</h2>
 *
 * <p>The two orientations that wrap -- {@code VERTICAL_WRAP} and {@code HORIZONTAL_WRAP} -- are
 * laid out in columns of fixed width {@link #cellWidth}; the JDK also hands out the last
 * column's leftovers. The difference shows in a wrapped list whose total is not a multiple of
 * the number of columns.
 */
public class BasicListUI extends ListUI {

    /** The data model changed. */
    protected static final int modelChanged = 1 << 0;

    /** The selection model changed. */
    protected static final int selectionModelChanged = 1 << 1;

    /** The typeface changed. */
    protected static final int fontChanged = 1 << 2;

    /** The fixed cell width changed. */
    protected static final int fixedCellWidthChanged = 1 << 3;

    /** The fixed cell height changed. */
    protected static final int fixedCellHeightChanged = 1 << 4;

    /** The prototype value the measuring is done with changed. */
    protected static final int prototypeCellValueChanged = 1 << 5;

    /** The cell renderer changed. */
    protected static final int cellRendererChanged = 1 << 6;

    protected JList list = null;
    protected CellRendererPane rendererPane;

    protected FocusListener focusListener;
    protected MouseInputListener mouseInputListener;
    protected ListSelectionListener listSelectionListener;
    protected ListDataListener listDataListener;
    protected PropertyChangeListener propertyChangeListener;

    /** The height of every row when they are all the same, or -1; see the class note. */
    protected int cellHeight = -1;

    /** The width of the widest row, or -1. */
    protected int cellWidth = -1;

    /** Each row's height when they are not the same, or {@code null}; see the class note. */
    protected int[] cellHeights = null;

    /** The reasons the table of heights went stale; see the class note. */
    protected int updateLayoutStateNeeded = modelChanged;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource SELECTION = new ColorUIResource(184, 207, 229);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    /** The renderer the baseline is measured with; a single one is enough for the whole VM. */
    private static Component baseRenderer;

    public BasicListUI() {
    }

    /** A new one per list: it keeps the list, its listeners and the table of heights. */
    public static ComponentUI createUI(JComponent list) {
        return new BasicListUI();
    }

    public void installUI(JComponent c) {
        list = (JList) c;
        rendererPane = new CellRendererPane();
        list.add(rendererPane);
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallListeners();
        uninstallDefaults();
        uninstallKeyboardActions();
        cellWidth = cellHeight = -1;
        cellHeights = null;
        list.remove(rendererPane);
        rendererPane = null;
        list = null;
    }

    /** Colours, typeface, renderer and opacity; the values are those of {@code List.*} in Metal. */
    protected void installDefaults() {
        list.setLayout(null);
        Color background = list.getBackground();
        if (background == null || background instanceof UIResource) {
            list.setBackground(BACKGROUND);
        }
        Color foreground = list.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            list.setForeground(FOREGROUND);
        }
        Font font = list.getFont();
        if (font == null || font instanceof UIResource) {
            list.setFont(FONT);
        }
        LookAndFeel.installProperty(list, "opaque", Boolean.TRUE);
        if (list.getCellRenderer() == null) {
            list.setCellRenderer(new javax.swing.DefaultListCellRenderer.UIResource());
        }
        Color sbg = list.getSelectionBackground();
        if (sbg == null || sbg instanceof UIResource) {
            list.setSelectionBackground(SELECTION);
        }
        Color sfg = list.getSelectionForeground();
        if (sfg == null || sfg instanceof UIResource) {
            list.setSelectionForeground(FOREGROUND);
        }
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        focusListener = createFocusListener();
        mouseInputListener = createMouseInputListener();
        listSelectionListener = createListSelectionListener();
        listDataListener = createListDataListener();
        propertyChangeListener = createPropertyChangeListener();

        list.addFocusListener(focusListener);
        list.addMouseListener(mouseInputListener);
        list.addMouseMotionListener(mouseInputListener);
        list.addPropertyChangeListener(propertyChangeListener);
        if (list.getModel() != null) {
            list.getModel().addListDataListener(listDataListener);
        }
        if (list.getSelectionModel() != null) {
            list.getSelectionModel().addListSelectionListener(listSelectionListener);
        }
    }

    protected void uninstallListeners() {
        list.removeFocusListener(focusListener);
        list.removeMouseListener(mouseInputListener);
        list.removeMouseMotionListener(mouseInputListener);
        list.removePropertyChangeListener(propertyChangeListener);
        if (list.getModel() != null) {
            list.getModel().removeListDataListener(listDataListener);
        }
        if (list.getSelectionModel() != null) {
            list.getSelectionModel().removeListSelectionListener(listSelectionListener);
        }
        focusListener = null;
        mouseInputListener = null;
        listSelectionListener = null;
        listDataListener = null;
        propertyChangeListener = null;
    }

    /**
     * With no shortcuts of its own: arrow navigation is handled by the look and feel's action
     * table.
     */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected FocusListener createFocusListener() {
        return new Handler();
    }

    protected MouseInputListener createMouseInputListener() {
        return new Handler();
    }

    protected ListSelectionListener createListSelectionListener() {
        return new Handler();
    }

    protected ListDataListener createListDataListener() {
        return new Handler();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler();
    }

    /** It rebuilds the table if it went stale; see the class note. */
    protected void maybeUpdateLayoutState() {
        if (updateLayoutStateNeeded != 0) {
            updateLayoutState();
            updateLayoutStateNeeded = 0;
        }
    }

    /**
     * It measures each row and builds the table; see the class note.
     *
     * <p>If the list has a fixed width or height, that half is not measured: the number is copied.
     * And if it has no renderer -- which may happen between two calls to {@code setCellRenderer} --
     * everything is left at zero instead of blowing up.
     */
    protected void updateLayoutState() {
        int fixedCellHeight = list.getFixedCellHeight();
        int fixedCellWidth = list.getFixedCellWidth();

        cellWidth = (fixedCellWidth != -1) ? fixedCellWidth : -1;

        if (fixedCellHeight != -1) {
            cellHeight = fixedCellHeight;
            cellHeights = null;
        } else {
            cellHeight = -1;
            cellHeights = new int[list.getModel().getSize()];
        }

        if ((fixedCellWidth == -1) || (fixedCellHeight == -1)) {
            ListModel dataModel = list.getModel();
            int dataModelSize = dataModel.getSize();
            ListCellRenderer renderer = list.getCellRenderer();

            if (renderer != null) {
                for (int index = 0; index < dataModelSize; index++) {
                    Object value = dataModel.getElementAt(index);
                    Component c = renderer.getListCellRendererComponent(list, value, index,
                            false, false);
                    rendererPane.add(c);
                    Dimension cellSize = c.getPreferredSize();
                    if (fixedCellWidth == -1) {
                        cellWidth = Math.max(cellSize.width, cellWidth);
                    }
                    if (fixedCellHeight == -1) {
                        cellHeights[index] = cellSize.height;
                    }
                }
            } else {
                if (cellWidth == -1) {
                    cellWidth = 0;
                }
                if (cellHeights == null) {
                    cellHeights = new int[dataModelSize];
                }
                for (int index = 0; index < dataModelSize; index++) {
                    cellHeights[index] = 0;
                }
            }
        }
        list.invalidate();
    }

    /** That row's height: the fixed one, or the table's. */
    protected int getRowHeight(int row) {
        if (row < 0 || row >= list.getModel().getSize()) {
            return -1;
        }
        return (cellHeights == null) ? cellHeight
                : ((row < cellHeights.length) ? cellHeights[row] : -1);
    }

    /** Which row falls at that coordinate; see the class note about the edges. */
    protected int convertYToRow(int y0) {
        int rowCount = list.getModel().getSize();
        if (rowCount <= 0) {
            return -1;
        }
        Insets insets = list.getInsets();
        if (cellHeights == null) {
            int row = (cellHeight == 0) ? 0 : ((y0 - insets.top) / cellHeight);
            return (row < 0) ? -1 : ((row >= rowCount) ? -1 : row);
        }
        if (rowCount > cellHeights.length) {
            return -1;
        }
        int y = insets.top;
        int row = 0;
        for (int i = 0; i < rowCount; i++) {
            if ((y0 >= y) && (y0 < y + cellHeights[i])) {
                return row;
            }
            y += cellHeights[i];
            row += 1;
        }
        return row - 1;
    }

    /** Where that row starts; -1 if it does not exist. */
    protected int convertRowToY(int row) {
        if (row >= list.getModel().getSize() || row < 0) {
            return -1;
        }
        Rectangle bounds = getCellBounds(list, row, row);
        return (bounds == null) ? -1 : bounds.y;
    }

    /** A row's rectangle, or {@code null} if it does not exist. */
    private Rectangle rowBand(int index) {
        maybeUpdateLayoutState();
        if (index < 0 || index >= list.getModel().getSize()) {
            return null;
        }
        Insets insets = list.getInsets();
        int y = insets.top;
        if (cellHeights == null) {
            y += index * cellHeight;
        } else {
            for (int i = 0; i < index && i < cellHeights.length; i++) {
                y += cellHeights[i];
            }
        }
        int w = list.getWidth() - (insets.left + insets.right);
        int h = (cellHeights == null) ? cellHeight
                : ((index < cellHeights.length) ? cellHeights[index] : 0);
        return new Rectangle(insets.left, y, w, h);
    }

    /**
     * The rectangle that spans from one index to the other.
     *
     * <p>The order does not matter: they sort themselves. {@code null} if the first of the two no
     * longer exists.
     */
    public Rectangle getCellBounds(JList list, int index1, int index2) {
        maybeUpdateLayoutState();
        int minIndex = Math.min(index1, index2);
        int maxIndex = Math.max(index1, index2);
        if (minIndex >= list.getModel().getSize()) {
            return null;
        }
        Rectangle minBounds = rowBand(minIndex);
        if (minBounds == null) {
            return null;
        }
        if (minIndex == maxIndex) {
            return minBounds;
        }
        Rectangle maxBounds = rowBand(maxIndex);
        if (maxBounds != null) {
            minBounds.add(maxBounds);
        }
        return minBounds;
    }

    /** That row's top left corner, or {@code null}. */
    public Point indexToLocation(JList list, int index) {
        maybeUpdateLayoutState();
        Rectangle rect = rowBand(index);
        return (rect != null) ? new Point(rect.x, rect.y) : null;
    }

    /** Which row falls at that point; -1 if there is none. */
    public int locationToIndex(JList list, Point location) {
        maybeUpdateLayoutState();
        return convertYToRow(location.y);
    }

    /**
     * The height of every row plus the margins; the width, that of the widest one.
     *
     * <p>An empty list measures zero by zero, not the margins.
     */
    public Dimension getPreferredSize(JComponent c) {
        maybeUpdateLayoutState();
        int lastRow = list.getModel().getSize() - 1;
        if (lastRow < 0) {
            return new Dimension(0, 0);
        }
        Insets insets = list.getInsets();
        int width = cellWidth + insets.left + insets.right;
        Rectangle bounds = rowBand(lastRow);
        int height = (bounds != null) ? bounds.y + bounds.height + insets.bottom : 0;
        return new Dimension(width, height);
    }

    /**
     * Where the first row's text rests.
     *
     * <p>It is measured with the renderer loaded with a letter, not with the content: the answer
     * has to be the same whether the list is full or empty, because who uses it is a layout that
     * has not put the data in yet.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        int rowHeight = list.getFixedCellHeight();
        Component renderer = baseRenderer;
        if (renderer == null) {
            ListCellRenderer lcr = list.getCellRenderer();
            if (lcr == null) {
                lcr = new javax.swing.DefaultListCellRenderer();
            }
            renderer = lcr.getListCellRendererComponent(list, "a", -1, false, false);
            baseRenderer = renderer;
        }
        renderer.setFont(list.getFont());
        if (rowHeight > 0) {
            return renderer.getBaseline(Integer.MAX_VALUE, rowHeight);
        }
        Dimension pref = renderer.getPreferredSize();
        return renderer.getBaseline(pref.width, pref.height);
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

    /** The rows that are seen, one by one. */
    public void paint(Graphics g, JComponent c) {
        maybeUpdateLayoutState();
        ListCellRenderer renderer = list.getCellRenderer();
        ListModel dataModel = list.getModel();
        ListSelectionModel selModel = list.getSelectionModel();
        if (renderer == null || dataModel.getSize() == 0) {
            return;
        }
        Rectangle paintBounds = g.getClipBounds();
        int first = convertYToRow(paintBounds.y);
        int last = convertYToRow(paintBounds.y + paintBounds.height);
        if (first < 0) {
            first = 0;
        }
        if (last < 0) {
            last = dataModel.getSize() - 1;
        }
        int lead = list.getLeadSelectionIndex();
        for (int row = first; row <= last && row < dataModel.getSize(); row++) {
            Rectangle rowBounds = rowBand(row);
            if (rowBounds == null) {
                break;
            }
            paintCell(g, row, rowBounds, renderer, dataModel, selModel, lead);
        }
        rendererPane.removeAll();
    }

    /** One row: the renderer is built with its value and painted in its rectangle. */
    protected void paintCell(Graphics g, int row, Rectangle rowBounds, ListCellRenderer cellRenderer,
            ListModel dataModel, ListSelectionModel selModel, int leadIndex) {
        Object value = dataModel.getElementAt(row);
        boolean cellHasFocus = list.hasFocus() && (row == leadIndex);
        boolean isSelected = selModel.isSelectedIndex(row);
        Component rendererComponent = cellRenderer.getListCellRendererComponent(
                list, value, row, isSelected, cellHasFocus);
        rendererPane.paintComponent(g, rendererComponent, list,
                rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height, true);
    }

    /** It moves the selection one row up. */
    protected void selectPreviousIndex() {
        int s = list.getSelectedIndex();
        if (s > 0) {
            s -= 1;
            list.setSelectedIndex(s);
            list.ensureIndexIsVisible(s);
        }
    }

    /** And one down. */
    protected void selectNextIndex() {
        int s = list.getSelectedIndex();
        if ((s + 1) < list.getModel().getSize()) {
            s += 1;
            list.setSelectedIndex(s);
            list.ensureIndexIsVisible(s);
        }
    }

    /**
     * The one that listens to everything.
     *
     * <p>Five interfaces in one object for the same reason as in {@link BasicMenuItemUI}: all
     * five react to the same state -- that the table of heights went stale -- and separating them
     * would force it to be shared.
     */
    private class Handler implements FocusListener, MouseInputListener, ListSelectionListener,
            ListDataListener, PropertyChangeListener {

        public void focusGained(FocusEvent e) {
            repaintSelection();
        }

        public void focusLost(FocusEvent e) {
            repaintSelection();
        }

        private void repaintSelection() {
            int lead = list.getLeadSelectionIndex();
            if (lead != -1) {
                Rectangle r = getCellBounds(list, lead, lead);
                if (r != null) {
                    list.repaint(r.x, r.y, r.width, r.height);
                }
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!list.isEnabled()) {
                return;
            }
            if (!list.hasFocus() && list.isRequestFocusEnabled()) {
                list.requestFocus();
            }
            int row = convertYToRow(e.getY());
            if (row < 0) {
                return;
            }
            if (e.isControlDown()) {
                if (list.isSelectedIndex(row)) {
                    list.removeSelectionInterval(row, row);
                } else {
                    list.addSelectionInterval(row, row);
                }
            } else if (e.isShiftDown()) {
                list.setSelectionInterval(list.getAnchorSelectionIndex(), row);
            } else {
                list.setSelectionInterval(row, row);
            }
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (!list.isEnabled()) {
                return;
            }
            int row = convertYToRow(e.getY());
            if (row >= 0) {
                list.setSelectionInterval(list.getAnchorSelectionIndex(), row);
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void valueChanged(ListSelectionEvent e) {
            maybeUpdateLayoutState();
            int first = e.getFirstIndex();
            int last = e.getLastIndex();
            if (first < 0 || last < 0) {
                list.repaint();
                return;
            }
            Rectangle r = getCellBounds(list, first, last);
            if (r != null) {
                list.repaint(r.x, r.y, r.width, r.height);
            } else {
                list.repaint();
            }
        }

        public void intervalAdded(ListDataEvent e) {
            updateLayoutStateNeeded |= modelChanged;
            list.revalidate();
            list.repaint();
        }

        public void intervalRemoved(ListDataEvent e) {
            updateLayoutStateNeeded |= modelChanged;
            list.revalidate();
            list.repaint();
        }

        public void contentsChanged(ListDataEvent e) {
            updateLayoutStateNeeded |= modelChanged;
            list.revalidate();
            list.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ("model".equals(name)) {
                ListModel old = (ListModel) e.getOldValue();
                if (old != null) {
                    old.removeListDataListener(listDataListener);
                }
                ListModel newValue = (ListModel) e.getNewValue();
                if (newValue != null) {
                    newValue.addListDataListener(listDataListener);
                }
                updateLayoutStateNeeded |= modelChanged;
            } else if ("selectionModel".equals(name)) {
                ListSelectionModel old = (ListSelectionModel) e.getOldValue();
                if (old != null) {
                    old.removeListSelectionListener(listSelectionListener);
                }
                ListSelectionModel newValue = (ListSelectionModel) e.getNewValue();
                if (newValue != null) {
                    newValue.addListSelectionListener(listSelectionListener);
                }
                updateLayoutStateNeeded |= selectionModelChanged;
            } else if ("font".equals(name)) {
                updateLayoutStateNeeded |= fontChanged;
            } else if ("fixedCellWidth".equals(name)) {
                updateLayoutStateNeeded |= fixedCellWidthChanged;
            } else if ("fixedCellHeight".equals(name)) {
                updateLayoutStateNeeded |= fixedCellHeightChanged;
            } else if ("prototypeCellValue".equals(name)) {
                updateLayoutStateNeeded |= prototypeCellValueChanged;
            } else if ("cellRenderer".equals(name)) {
                updateLayoutStateNeeded |= cellRendererChanged;
            } else {
                return;
            }
            list.revalidate();
            list.repaint();
        }
    }
}
