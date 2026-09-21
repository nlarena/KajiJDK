package javax.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ListUI;
import javax.swing.text.Position;

/**
 * A list of lines to choose from.
 *
 * <h2>Three objects, not one</h2>
 *
 * <p>The data go in a {@link ListModel}, what is chosen in a {@link ListSelectionModel}, and
 * how each line is drawn in a {@link ListCellRenderer}. The list keeps none of that: it brings
 * them together.
 *
 * <p>Separating them is what allows a million lines to be shown. The model may compute each
 * one on being asked, and the renderer is a single component used as a stamp; see
 * {@link ListCellRenderer}'s note.
 *
 * <h2>Why the list does not scroll by itself</h2>
 *
 * <p>A {@code JList} has no bars. It implements {@link Scrollable}, which is the interface it
 * explains to a {@link JScrollPane} how much to measure and how much to advance by with.
 * Putting a list in without putting it into a scroll pane is the commonest mistake with this
 * class: it is seen whole and with no bars, however long it is.
 *
 * <h2>Fixed or measured size</h2>
 *
 * <p>Measuring each line in order to know how much the list measures costs walking through them
 * all. {@link #setFixedCellHeight} and {@link #setPrototypeCellValue} are two ways of avoiding
 * that: the first says it, the second gives a sample value it is deduced from. With a long list
 * the difference shows.
 *
 * @param <E> the elements' type.
 */
public class JList<E> extends JComponent implements Scrollable, Accessible {

    private static final String uiClassID = "ListUI";

    /** The lines go one below the other, in a single column. */
    public static final int VERTICAL = 0;

    /** They go in columns, filling one column before going on to the next. */
    public static final int VERTICAL_WRAP = 1;

    /** They go in rows, filling one row before going on to the next. */
    public static final int HORIZONTAL_WRAP = 2;

    private int fixedCellWidth = -1;
    private int fixedCellHeight = -1;
    private int horizontalScrollIncrement = -1;
    private E prototypeCellValue;
    private int visibleRowCount = 8;
    private Color selectionForeground;
    private Color selectionBackground;
    private boolean dragEnabled;
    private ListSelectionModel selectionModel;
    private ListModel<E> dataModel;
    private ListCellRenderer<? super E> cellRenderer;
    private ListSelectionListener selectionListener;
    private int layoutOrientation = VERTICAL;
    private DropMode dropMode = DropMode.USE_SELECTION;
    private transient DropLocation dropLocation;
    private AccessibleContext accessibleContext;

    /** A list over that model. */
    public JList(ListModel<E> dataModel) {
        if (dataModel == null) {
            throw new IllegalArgumentException("dataModel must be non null");
        }
        layoutOrientation = VERTICAL;
        this.dataModel = dataModel;
        selectionModel = createSelectionModel();
        setAutoscrolls(true);
        setOpaque(true);
        updateUI();
    }

    /** A list with those elements, over a read-only model. */
    public JList(final E[] listData) {
        this(new ArrayModel<E>(listData));
    }

    /** A list with that vector's elements. */
    public JList(final Vector<? extends E> listData) {
        this(new VectorModel<E>(listData));
    }

    /** An empty list. */
    public JList() {
        this(new EmptyModel<E>());
    }

    /**
     * A read-only model over an array.
     *
     * <p>It does not copy the array. It is what the JDK does and it is what allows a list to be
     * built over large data without duplicating them; the price is that changing the array from
     * outside leaves the list showing the old thing, because the model has no way of learning
     * about it.
     */
    static class ArrayModel<E> extends AbstractListModel<E> {

        private final E[] data;

        ArrayModel(E[] data) {
            this.data = data;
        }

        public int getSize() {
            return data.length;
        }

        public E getElementAt(int i) {
            return data[i];
        }
    }

    /** The same, over a vector. */
    static class VectorModel<E> extends AbstractListModel<E> {

        private final Vector<? extends E> data;

        VectorModel(Vector<? extends E> data) {
            this.data = data;
        }

        public int getSize() {
            return data.size();
        }

        public E getElementAt(int i) {
            return data.elementAt(i);
        }
    }

    /** An empty list's model. */
    static class EmptyModel<E> extends AbstractListModel<E> {

        public int getSize() {
            return 0;
        }

        public E getElementAt(int i) {
            return null;
        }
    }

    /** The look and feel that draws the list. */
    public ListUI getUI() {
        return (ListUI) ui;
    }

    public void setUI(ListUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
        setCellRenderer(null);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** A sample value a line's size is deduced from; see the class note. */
    public E getPrototypeCellValue() {
        return prototypeCellValue;
    }

    public void setPrototypeCellValue(E prototypeCellValue) {
        E oldValue = this.prototypeCellValue;
        this.prototypeCellValue = prototypeCellValue;
        if (prototypeCellValue != null && !prototypeCellValue.equals(oldValue)) {
            firePropertyChange("prototypeCellValue", oldValue, prototypeCellValue);
        }
    }

    public int getFixedCellWidth() {
        return fixedCellWidth;
    }

    /** The width of every line; with -1 each one is measured. */
    public void setFixedCellWidth(int width) {
        int oldValue = fixedCellWidth;
        fixedCellWidth = width;
        firePropertyChange("fixedCellWidth", oldValue, fixedCellWidth);
    }

    public int getFixedCellHeight() {
        return fixedCellHeight;
    }

    public void setFixedCellHeight(int height) {
        int oldValue = fixedCellHeight;
        fixedCellHeight = height;
        firePropertyChange("fixedCellHeight", oldValue, fixedCellHeight);
    }

    /** Who draws each line. */
    public ListCellRenderer<? super E> getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(ListCellRenderer<? super E> cellRenderer) {
        ListCellRenderer<? super E> oldValue = this.cellRenderer;
        this.cellRenderer = cellRenderer;
        if (cellRenderer != null && !cellRenderer.equals(oldValue)) {
            firePropertyChange("cellRenderer", oldValue, cellRenderer);
        }
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    /** The colour of the chosen lines' letters. */
    public void setSelectionForeground(Color selectionForeground) {
        Color oldValue = this.selectionForeground;
        this.selectionForeground = selectionForeground;
        firePropertyChange("selectionForeground", oldValue, selectionForeground);
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        Color oldValue = this.selectionBackground;
        this.selectionBackground = selectionBackground;
        firePropertyChange("selectionBackground", oldValue, selectionBackground);
    }

    /** How many lines are seen without scrolling; it is what the list asks the scroller for. */
    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    public void setVisibleRowCount(int visibleRowCount) {
        int oldValue = this.visibleRowCount;
        this.visibleRowCount = Math.max(0, visibleRowCount);
        firePropertyChange("visibleRowCount", oldValue, visibleRowCount);
    }

    /** Whether the lines go in one column or are laid out in several. */
    public int getLayoutOrientation() {
        return layoutOrientation;
    }

    /**
     * How the lines are laid out.
     *
     * @throws IllegalArgumentException if it is not one of the three.
     */
    public void setLayoutOrientation(int layoutOrientation) {
        int oldValue = this.layoutOrientation;
        if (layoutOrientation != VERTICAL && layoutOrientation != VERTICAL_WRAP
                && layoutOrientation != HORIZONTAL_WRAP) {
            throw new IllegalArgumentException("layoutOrientation must be one of: "
                    + "VERTICAL, HORIZONTAL_WRAP or VERTICAL_WRAP");
        }
        this.layoutOrientation = layoutOrientation;
        firePropertyChange("layoutOrientation", oldValue, layoutOrientation);
    }

    /** The first line that is seen, or -1 if none is seen. */
    public int getFirstVisibleIndex() {
        Rectangle r = getVisibleRect();
        int first = (r.width > 0 && r.height > 0) ? locationToIndex(r.getLocation()) : -1;
        return first;
    }

    public int getLastVisibleIndex() {
        Rectangle r = getVisibleRect();
        if (r.width <= 0 || r.height <= 0) {
            return -1;
        }
        Point last = new Point(r.x + r.width - 1, r.y + r.height - 1);
        return locationToIndex(last);
    }

    /** It scrolls so that that line is seen. */
    public void ensureIndexIsVisible(int index) {
        Rectangle cellBounds = getCellBounds(index, index);
        if (cellBounds != null) {
            scrollRectToVisible(cellBounds);
        }
    }

    public void setDragEnabled(boolean b) {
        dragEnabled = b;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /**
     * How where what is being dragged will fall is shown.
     *
     * @throws IllegalArgumentException if the mode does not serve for a list.
     */
    public final void setDropMode(DropMode dropMode) {
        if (dropMode != null) {
            if (dropMode == DropMode.USE_SELECTION || dropMode == DropMode.ON
                    || dropMode == DropMode.INSERT || dropMode == DropMode.ON_OR_INSERT) {
                this.dropMode = dropMode;
                return;
            }
        }
        throw new IllegalArgumentException(dropMode + ": Unsupported drop mode for list");
    }

    public final DropMode getDropMode() {
        return dropMode;
    }

    /** Where what is being dragged would fall now, or null. */
    public final DropLocation getDropLocation() {
        return dropLocation;
    }

    /**
     * The next line whose text begins with that.
     *
     * <p>It is what makes typing in a list jump to the line. The comparison ignores case and uses
     * the text the renderer shows, not the object: it is what the user sees.
     */
    public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
        ListModel<E> model = getModel();
        int max = model.getSize();
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase(java.util.Locale.ROOT);

        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int index = startIndex;
        do {
            E item = model.getElementAt(index);
            if (item != null) {
                String string = item.toString();
                if (string != null
                        && string.toUpperCase(java.util.Locale.ROOT).startsWith(prefix)) {
                    return index;
                }
            }
            index = (index + increment + max) % max;
        } while (index != startIndex);
        return -1;
    }

    /** The tool tip text of the line that is under the mouse. */
    public String getToolTipText(MouseEvent event) {
        return super.getToolTipText(event);
    }

    /** Which line falls at that point; the look and feel answers it. */
    public int locationToIndex(Point location) {
        ListUI ui = getUI();
        return (ui != null) ? ui.locationToIndex(this, location) : -1;
    }

    public Point indexToLocation(int index) {
        ListUI ui = getUI();
        return (ui != null) ? ui.indexToLocation(this, index) : null;
    }

    /** The rectangle the lines between those two indices take up. */
    public Rectangle getCellBounds(int index0, int index1) {
        ListUI ui = getUI();
        return (ui != null) ? ui.getCellBounds(this, index0, index1) : null;
    }

    public ListModel<E> getModel() {
        return dataModel;
    }

    /**
     * It changes the data model.
     *
     * <p>It empties the selection: the chosen indices referred to the old data, and keeping them
     * would leave lines chosen that have nothing to do with it.
     */
    public void setModel(ListModel<E> model) {
        if (model == null) {
            throw new IllegalArgumentException("model must be non null");
        }
        ListModel<E> oldValue = dataModel;
        dataModel = model;
        firePropertyChange("model", oldValue, dataModel);
        clearSelection();
    }

    /** It replaces the data with that array. */
    public void setListData(final E[] listData) {
        setModel(new ArrayModel<E>(listData));
    }

    /** It replaces the data with that vector. */
    public void setListData(final Vector<? extends E> listData) {
        setModel(new VectorModel<E>(listData));
    }

    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /** It forwards the selection model's notice to whoever listens to the list. */
    protected void fireSelectionValueChanged(int firstIndex, int lastIndex,
            boolean isAdjusting) {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
                }
                ((ListSelectionListener) listeners[i + 1]).valueChanged(e);
            }
        }
    }

    /**
     * It adds whoever wants to learn about the changes of selection.
     *
     * <p>The list is listened to and not the selection model on purpose: changing the model should
     * not leave whoever signed up deaf. The list takes care of hooking itself up again.
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        if (selectionListener == null) {
            selectionListener = new SelectionListenerImpl(this);
            getSelectionModel().addListSelectionListener(selectionListener);
        }
        listenerList.add(ListSelectionListener.class, listener);
    }

    public void removeListSelectionListener(ListSelectionListener listener) {
        listenerList.remove(ListSelectionListener.class, listener);
    }

    public ListSelectionListener[] getListSelectionListeners() {
        return listenerList.getListeners(ListSelectionListener.class);
    }

    /** The bridge between the selection model and whoever listens to the list. */
    static class SelectionListenerImpl implements ListSelectionListener, java.io.Serializable {

        private final JList<?> list;

        SelectionListenerImpl(JList<?> list) {
            this.list = list;
        }

        public void valueChanged(ListSelectionEvent e) {
            list.fireSelectionValueChanged(e.getFirstIndex(), e.getLastIndex(),
                    e.getValueIsAdjusting());
        }
    }

    /** It changes the selection model, taking the bridge along to the new one. */
    public void setSelectionModel(ListSelectionModel selectionModel) {
        if (selectionModel == null) {
            throw new IllegalArgumentException("selectionModel must be non null");
        }
        if (selectionListener != null) {
            this.selectionModel.removeListSelectionListener(selectionListener);
            selectionModel.addListSelectionListener(selectionListener);
        }
        ListSelectionModel oldValue = this.selectionModel;
        this.selectionModel = selectionModel;
        firePropertyChange("selectionModel", oldValue, selectionModel);
    }

    public void setSelectionMode(int selectionMode) {
        getSelectionModel().setSelectionMode(selectionMode);
    }

    public int getSelectionMode() {
        return getSelectionModel().getSelectionMode();
    }

    public int getAnchorSelectionIndex() {
        return getSelectionModel().getAnchorSelectionIndex();
    }

    public int getLeadSelectionIndex() {
        return getSelectionModel().getLeadSelectionIndex();
    }

    public int getMinSelectionIndex() {
        return getSelectionModel().getMinSelectionIndex();
    }

    public int getMaxSelectionIndex() {
        return getSelectionModel().getMaxSelectionIndex();
    }

    public boolean isSelectedIndex(int index) {
        return getSelectionModel().isSelectedIndex(index);
    }

    public boolean isSelectionEmpty() {
        return getSelectionModel().isSelectionEmpty();
    }

    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    public void setSelectionInterval(int anchor, int lead) {
        getSelectionModel().setSelectionInterval(anchor, lead);
    }

    public void addSelectionInterval(int anchor, int lead) {
        getSelectionModel().addSelectionInterval(anchor, lead);
    }

    public void removeSelectionInterval(int index0, int index1) {
        getSelectionModel().removeSelectionInterval(index0, index1);
    }

    public void setValueIsAdjusting(boolean b) {
        getSelectionModel().setValueIsAdjusting(b);
    }

    public boolean getValueIsAdjusting() {
        return getSelectionModel().getValueIsAdjusting();
    }

    /** The chosen indices, sorted. */
    public int[] getSelectedIndices() {
        ListSelectionModel sm = getSelectionModel();
        int iMin = sm.getMinSelectionIndex();
        int iMax = sm.getMaxSelectionIndex();
        if ((iMin < 0) || (iMax < 0)) {
            return new int[0];
        }
        int[] rvTmp = new int[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (sm.isSelectedIndex(i)) {
                rvTmp[n] = i;
                n++;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /** It chooses only that line; with -1 none is left. */
    public void setSelectedIndex(int index) {
        if (index >= getModel().getSize()) {
            return;
        }
        getSelectionModel().setSelectionInterval(index, index);
    }

    /** It chooses those lines and no others. */
    public void setSelectedIndices(int[] indices) {
        ListSelectionModel sm = getSelectionModel();
        sm.clearSelection();
        int size = getModel().getSize();
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < size) {
                sm.addSelectionInterval(indices[i], indices[i]);
            }
        }
    }

    /**
     * The chosen elements.
     *
     * @deprecated Use {@link #getSelectedValuesList}, which returns a list with the type set.
     */
    @Deprecated
    public Object[] getSelectedValues() {
        ListSelectionModel sm = getSelectionModel();
        ListModel<E> dm = getModel();
        int iMin = sm.getMinSelectionIndex();
        int iMax = sm.getMaxSelectionIndex();
        if ((iMin < 0) || (iMax < 0)) {
            return new Object[0];
        }
        Object[] rvTmp = new Object[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (sm.isSelectedIndex(i)) {
                rvTmp[n] = dm.getElementAt(i);
                n++;
            }
        }
        Object[] rv = new Object[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /** The chosen elements, in order. */
    public List<E> getSelectedValuesList() {
        ListSelectionModel sm = getSelectionModel();
        ListModel<E> dm = getModel();
        int iMin = sm.getMinSelectionIndex();
        int iMax = sm.getMaxSelectionIndex();
        if ((iMin < 0) || (iMax < 0)) {
            return java.util.Collections.emptyList();
        }
        List<E> selectedItems = new ArrayList<E>();
        for (int i = iMin; i <= iMax; i++) {
            if (sm.isSelectedIndex(i)) {
                selectedItems.add(dm.getElementAt(i));
            }
        }
        return selectedItems;
    }

    /** The first of the chosen ones, or -1. */
    public int getSelectedIndex() {
        return getMinSelectionIndex();
    }

    public E getSelectedValue() {
        int i = getMinSelectionIndex();
        return (i == -1) ? null : getModel().getElementAt(i);
    }

    /** It looks that object up in the model and chooses it. */
    public void setSelectedValue(Object anObject, boolean shouldScroll) {
        if (anObject == null) {
            setSelectedIndex(-1);
        } else if (!anObject.equals(getSelectedValue())) {
            ListModel<E> dm = getModel();
            int c = dm.getSize();
            for (int i = 0; i < c; i++) {
                if (anObject.equals(dm.getElementAt(i))) {
                    setSelectedIndex(i);
                    if (shouldScroll) {
                        ensureIndexIsVisible(i);
                    }
                    repaint();
                    return;
                }
            }
            setSelectedIndex(-1);
        }
        repaint();
    }

    /**
     * How much to ask the scroller for.
     *
     * <p>The height comes from {@link #getVisibleRowCount} lines, not from all of them: it is
     * precisely what tells "how much I want to be seen" from "how much I measure".
     */
    public Dimension getPreferredScrollableViewportSize() {
        if (getLayoutOrientation() != VERTICAL) {
            return getPreferredSize();
        }
        Insets insets = getInsets();
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;
        int visibleRowCount = getVisibleRowCount();
        int fixedCellWidth = getFixedCellWidth();
        int fixedCellHeight = getFixedCellHeight();

        if ((fixedCellWidth > 0) && (fixedCellHeight > 0)) {
            int width = fixedCellWidth + dx;
            int height = (visibleRowCount * fixedCellHeight) + dy;
            return new Dimension(width, height);
        }
        if (getModel().getSize() > 0) {
            Rectangle r = getCellBounds(0, 0);
            if (r != null) {
                int width = getPreferredSize().width;
                int height = (visibleRowCount * r.height) + dy;
                return new Dimension(width, height);
            }
        }
        int fixedCellWidth2 = (fixedCellWidth > 0) ? fixedCellWidth : 256;
        int fixedCellHeight2 = (fixedCellHeight > 0) ? fixedCellHeight : 16;
        return new Dimension(fixedCellWidth2, fixedCellHeight2 * visibleRowCount);
    }

    /** How much a mouse wheel advances by: one line. */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            int row = locationToIndex(visibleRect.getLocation());
            if (row == -1) {
                return 0;
            }
            Rectangle r = getCellBounds(row, row);
            if (r == null) {
                return 0;
            }
            if (direction > 0) {
                return r.height - (visibleRect.y - r.y);
            }
            return (visibleRect.y - r.y > 0) ? visibleRect.y - r.y : r.height;
        }
        return (horizontalScrollIncrement > 0) ? horizontalScrollIncrement : 20;
    }

    /** How much it advances by on clicking on the bar: a screenful minus one line. */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            int inc = visibleRect.height;
            int unit = getScrollableUnitIncrement(visibleRect, orientation, direction);
            if (inc > unit) {
                inc = inc - unit;
            }
            return inc;
        }
        return visibleRect.width;
    }

    /** The list stretches to the scroller's width when the lines go in a column. */
    public boolean getScrollableTracksViewportWidth() {
        if (getLayoutOrientation() == VERTICAL_WRAP && getVisibleRowCount() <= 0) {
            return true;
        }
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        if (getLayoutOrientation() == HORIZONTAL_WRAP && getVisibleRowCount() <= 0) {
            return true;
        }
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getHeight() > getPreferredSize().height;
        }
        return false;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * Where what is being dragged would fall.
     *
     * <p>{@link #isInsert} tells the two forms apart: dropping <em>over</em> a line replaces it,
     * dropping <em>between</em> two inserts. The difference is seen on the screen as a line
     * between lines instead of a highlighted line.
     */
    public static final class DropLocation extends TransferHandler.DropLocation {

        private final int index;
        private final boolean isInsert;

        DropLocation(Point p, int index, boolean isInsert) {
            super(p);
            this.index = index;
            this.isInsert = isInsert;
        }

        /** The line it would fall over, or where it would be inserted. */
        public int getIndex() {
            return index;
        }

        public boolean isInsert() {
            return isInsert;
        }

        public String toString() {
            return getClass().getName() + "[dropPoint=" + getDropPoint() + ","
                    + "index=" + index + ","
                    + "insert=" + isInsert + "]";
        }
    }
}
