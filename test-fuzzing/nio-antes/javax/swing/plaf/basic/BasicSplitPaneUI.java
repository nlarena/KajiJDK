package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a split pane.
 *
 * <h2>Three components and a layout with a memory</h2>
 *
 * <p>The pane has two children and a divider in the middle, and the sharing out is not
 * recomputed from scratch each time: the layout remembers how much it gave each one
 * -- {@link BasicHorizontalLayoutManager} and its table {@code sizes} -- and on changing the
 * pane's size it hands out the difference. Without that memory, enlarging the window would
 * return the divider to the middle and where the user left it would be lost.
 *
 * <h2>Continuous dragging or with a shadow</h2>
 *
 * <p>See {@link BasicSplitPaneDivider}'s note. Here is the other half: the "non-continuous
 * divider" ({@link #getNonContinuousLayoutDivider}) is the rectangle that is drawn while
 * dragging and that disappears on releasing. It is added to the pane's top layer so that it
 * covers the two children.
 *
 * <h2>The twelve fields that were left null</h2>
 *
 * <p>Seven {@link KeyStroke}s and five {@link ActionListener}s, all protected, all
 * {@code null}. They are from when the look and feel tied the keys by hand; now they are tied
 * by the look and feel's table. It is measured, and it is the same story as {@code shadow} and
 * {@code highlight} in {@link BasicSeparatorUI}.
 *
 * <h2>No insets</h2>
 *
 * <p>{@link #getInsets} returns {@code null}, not an {@code Insets} at zero: it lets the
 * component's border answer. Measured.
 */
public class BasicSplitPaneUI extends SplitPaneUI {

    /** The name the drag divider is added to the top layer with. */
    protected static final String NON_CONTINUOUS_DIVIDER = "nonContinuousDivider";

    /** How much the divider moves with a keyboard arrow. */
    protected static int KEYBOARD_DIVIDER_MOVE_OFFSET = 3;

    protected JSplitPane splitPane;
    protected BasicHorizontalLayoutManager layoutManager;
    protected BasicSplitPaneDivider divider;
    protected PropertyChangeListener propertyChangeListener;
    protected FocusListener focusListener;
    protected int dividerSize;
    protected Component nonContinuousLayoutDivider;
    protected boolean draggingHW;
    protected int beginDragDividerLocation;

    /** Unused; see the class note. */
    protected KeyStroke upKey;

    /** Unused; see the class note. */
    protected KeyStroke downKey;

    /** Unused; see the class note. */
    protected KeyStroke leftKey;

    /** Unused; see the class note. */
    protected KeyStroke rightKey;

    /** Unused; see the class note. */
    protected KeyStroke homeKey;

    /** Unused; see the class note. */
    protected KeyStroke endKey;

    /** Unused; see the class note. */
    protected KeyStroke dividerResizeToggleKey;

    /** Unused; see the class note. */
    protected ActionListener keyboardUpLeftListener;

    /** Unused; see the class note. */
    protected ActionListener keyboardDownRightListener;

    /** Unused; see the class note. */
    protected ActionListener keyboardHomeListener;

    /** Unused; see the class note. */
    protected ActionListener keyboardEndListener;

    /** Unused; see the class note. */
    protected ActionListener keyboardResizeToggleListener;

    private int lastDragLocation = -1;
    private boolean continuousLayout;
    private int orientation;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);

    public BasicSplitPaneUI() {
    }

    /** A new one per pane: it keeps the pane, its divider and the sharing out. */
    public static ComponentUI createUI(JComponent x) {
        return new BasicSplitPaneUI();
    }

    public void installUI(JComponent c) {
        splitPane = (JSplitPane) c;
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();
        splitPane = null;
    }

    /**
     * Colours, border, divider and layout; the values are those of {@code SplitPane.*} in Metal.
     */
    protected void installDefaults() {
        orientation = splitPane.getOrientation();
        continuousLayout = splitPane.isContinuousLayout();
        resetLayoutManager();

        if (divider == null) {
            divider = createDefaultDivider();
        }
        divider.setBasicSplitPaneUI(this);

        Color background = splitPane.getBackground();
        if (background == null || background instanceof UIResource) {
            splitPane.setBackground(BACKGROUND);
        }
        Border b = splitPane.getBorder();
        if (b == null || b instanceof UIResource) {
            splitPane.setBorder(BasicBorders.getSplitPaneBorder());
        }
        Border db = divider.getBorder();
        if (db == null || db instanceof UIResource) {
            divider.setBorder(BasicBorders.getSplitPaneDividerBorder());
        }
        LookAndFeel.installProperty(splitPane, "opaque", Boolean.TRUE);

        dividerSize = splitPane.getDividerSize();
        if (dividerSize == 0) {
            dividerSize = 10;
            splitPane.setDividerSize(dividerSize);
        }
        divider.setDividerSize(splitPane.getDividerSize());
        dividerSize = divider.getDividerSize();
        splitPane.add(divider, JSplitPane.DIVIDER);

        setNonContinuousLayoutDivider(createDefaultNonContinuousLayoutDivider(), true);
    }

    /** It removes the divider and the border this look and feel set. */
    protected void uninstallDefaults() {
        if (splitPane.getLayout() == layoutManager) {
            splitPane.setLayout(null);
        }
        if (nonContinuousLayoutDivider != null) {
            splitPane.remove(nonContinuousLayoutDivider);
        }
        LookAndFeel.uninstallBorder(splitPane);
        if (divider != null) {
            splitPane.remove(divider);
            divider.setBasicSplitPaneUI(null);
        }
        layoutManager = null;
        divider = null;
        nonContinuousLayoutDivider = null;
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        splitPane.addPropertyChangeListener(propertyChangeListener);
        focusListener = createFocusListener();
        splitPane.addFocusListener(focusListener);
    }

    protected void uninstallListeners() {
        splitPane.removePropertyChangeListener(propertyChangeListener);
        splitPane.removeFocusListener(focusListener);
        propertyChangeListener = null;
        focusListener = null;
    }

    /** With no shortcuts of its own; see the class note about the null fields. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler();
    }

    protected FocusListener createFocusListener() {
        return new Handler();
    }

    /** None; see the class note. */
    protected ActionListener createKeyboardUpLeftListener() {
        return null;
    }

    /** None; see the class note. */
    protected ActionListener createKeyboardDownRightListener() {
        return null;
    }

    /** None; see the class note. */
    protected ActionListener createKeyboardHomeListener() {
        return null;
    }

    /** None; see the class note. */
    protected ActionListener createKeyboardEndListener() {
        return null;
    }

    /** None; see the class note. */
    protected ActionListener createKeyboardResizeToggleListener() {
        return null;
    }

    public int getOrientation() {
        return orientation;
    }

    /** It changes the axis and rebuilds the layout. */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
        resetLayoutManager();
    }

    public boolean isContinuousLayout() {
        return continuousLayout;
    }

    public void setContinuousLayout(boolean b) {
        continuousLayout = b;
    }

    public int getLastDragLocation() {
        return lastDragLocation;
    }

    public void setLastDragLocation(int l) {
        lastDragLocation = l;
    }

    public BasicSplitPaneDivider getDivider() {
        return divider;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    /** The divider: one per pane. */
    public BasicSplitPaneDivider createDefaultDivider() {
        return new BasicSplitPaneDivider(this);
    }

    /** The rectangle that is drawn while dragging; see the class note. */
    protected Component createDefaultNonContinuousLayoutDivider() {
        return new DragShadow(this);
    }

    protected void setNonContinuousLayoutDivider(Component newDivider) {
        setNonContinuousLayoutDivider(newDivider, true);
    }

    protected void setNonContinuousLayoutDivider(Component newDivider, boolean rememberSizes) {
        if (nonContinuousLayoutDivider != null && splitPane != null) {
            splitPane.remove(nonContinuousLayoutDivider);
        }
        nonContinuousLayoutDivider = newDivider;
    }

    public Component getNonContinuousLayoutDivider() {
        return nonContinuousLayoutDivider;
    }

    /** One pixel: what the line of the divider's border takes up. */
    protected int getDividerBorderSize() {
        return 1;
    }

    /** It builds again the layout that corresponds to the axis. */
    protected void resetLayoutManager() {
        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            layoutManager = new BasicHorizontalLayoutManager(this);
        } else {
            layoutManager = new BasicVerticalLayoutManager(this);
        }
        splitPane.setLayout(layoutManager);
        layoutManager.updateComponents();
        splitPane.revalidate();
        splitPane.repaint();
    }

    /** It shares out again according to the children's preferred sizes. */
    public void resetToPreferredSizes(JSplitPane jc) {
        if (layoutManager != null) {
            layoutManager.resetToPreferredSizes();
            splitPane.revalidate();
        }
    }

    /** It begins a drag; it keeps where the divider was in case it has to go back. */
    protected void startDragging() {
        beginDragDividerLocation = getDividerLocation(splitPane);
        draggingHW = false;
        if (!isContinuousLayout() && nonContinuousLayoutDivider != null) {
            splitPane.add(nonContinuousLayoutDivider, JSplitPane.DIVIDER);
        }
    }

    /** It moves the divider, or the shadow if the drag is not continuous. */
    protected void dragDividerTo(int location) {
        setLastDragLocation(location);
        if (isContinuousLayout()) {
            splitPane.setDividerLocation(location);
        } else if (nonContinuousLayoutDivider != null) {
            if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                nonContinuousLayoutDivider.setLocation(location, 0);
            } else {
                nonContinuousLayoutDivider.setLocation(0, location);
            }
        }
    }

    /** It ends: it removes the shadow and leaves the divider where it ended up. */
    protected void finishDraggingTo(int location) {
        dragDividerTo(location);
        setLastDragLocation(-1);
        if (!isContinuousLayout()) {
            if (nonContinuousLayoutDivider != null) {
                splitPane.remove(nonContinuousLayoutDivider);
            }
            splitPane.setDividerLocation(location);
        }
    }

    /** Where the divider is, in pixels from the edge. */
    public int getDividerLocation(JSplitPane jc) {
        if (divider == null) {
            return 0;
        }
        return (orientation == JSplitPane.HORIZONTAL_SPLIT)
                ? divider.getLocation().x : divider.getLocation().y;
    }

    /** And how far it can be taken without shrinking the first child below its minimum. */
    public int getMinimumDividerLocation(JSplitPane jc) {
        int minLoc = 0;
        Component leftC = splitPane.getLeftComponent();
        if ((leftC != null) && (leftC.isVisible())) {
            Insets insets = splitPane.getInsets();
            Dimension minSize = leftC.getMinimumSize();
            minLoc = (orientation == JSplitPane.HORIZONTAL_SPLIT)
                    ? minSize.width : minSize.height;
            if (insets != null) {
                minLoc += (orientation == JSplitPane.HORIZONTAL_SPLIT)
                        ? insets.left : insets.top;
            }
        }
        return minLoc;
    }

    /**
     * And on the other side.
     *
     * <p>Never less than the minimum: in a pane that does not have a size yet the arithmetic gives
     * a negative, and a maximum below the minimum is of use to nobody.
     */
    public int getMaximumDividerLocation(JSplitPane jc) {
        Dimension splitPaneSize = splitPane.getSize();
        int maxLoc = 0;
        Component rightC = splitPane.getRightComponent();
        if (rightC != null) {
            Insets insets = splitPane.getInsets();
            Dimension minSize = new Dimension(0, 0);
            if (rightC.isVisible()) {
                minSize = rightC.getMinimumSize();
            }
            if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                maxLoc = splitPaneSize.width - minSize.width;
                if (insets != null) {
                    maxLoc -= insets.right;
                }
            } else {
                maxLoc = splitPaneSize.height - minSize.height;
                if (insets != null) {
                    maxLoc -= insets.bottom;
                }
            }
            maxLoc -= dividerSize;
        }
        return Math.max(getMinimumDividerLocation(splitPane), maxLoc);
    }

    /** It puts the divider there. */
    public void setDividerLocation(JSplitPane jc, int location) {
        if (layoutManager != null) {
            layoutManager.setDividerLocation(location);
            splitPane.revalidate();
            splitPane.repaint();
        }
    }

    /** {@code null}; see the class note. */
    public Insets getInsets(JComponent jc) {
        return null;
    }

    public Dimension getPreferredSize(JComponent jc) {
        if (splitPane == null || layoutManager == null) {
            return new Dimension(0, 0);
        }
        return layoutManager.preferredLayoutSize(splitPane);
    }

    public Dimension getMinimumSize(JComponent jc) {
        if (splitPane == null || layoutManager == null) {
            return new Dimension(0, 0);
        }
        return layoutManager.minimumLayoutSize(splitPane);
    }

    /** No cap: a split pane stretches as far as it is given. */
    public Dimension getMaximumSize(JComponent jc) {
        if (splitPane == null || layoutManager == null) {
            return new Dimension(0, 0);
        }
        return layoutManager.maximumLayoutSize(splitPane);
    }

    /** Nothing: the children and the divider paint themselves. */
    public void paint(Graphics g, JComponent jc) {
    }

    /** After the children: here the drag shadow would be drawn by hardware. */
    public void finishedPaintingChildren(JSplitPane jc, Graphics g) {
        if (jc == splitPane && getLastDragLocation() != -1
                && !isContinuousLayout() && !draggingHW) {
            Dimension size = splitPane.getSize();
            g.setColor(Color.darkGray);
            if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                g.fillRect(getLastDragLocation(), 0, dividerSize - 1, size.height - 1);
            } else {
                g.fillRect(0, getLastDragLocation(), size.width - 1, dividerSize - 1);
            }
        }
    }

    /**
     * The horizontal sharing out, with a memory; see the class note.
     *
     * <p>{@link #sizes} keeps three numbers: what the first child takes up, what the divider takes
     * up and what the second takes up. It is the only way for enlarging the pane not to return the
     * divider to the middle.
     */
    public static class BasicHorizontalLayoutManager implements LayoutManager2 {

        /** The owning look and feel; see finding #518 about why it goes as a parameter. */
        final BasicSplitPaneUI ui;

        /** The first child, the second and the divider, in that order. */
        protected Component[] components = new Component[3];

        /** What each one takes up; see the class note. */
        protected int[] sizes = new int[3];

        private final int axis;

        BasicHorizontalLayoutManager(BasicSplitPaneUI ui) {
            this(ui, JSplitPane.HORIZONTAL_SPLIT);
        }

        BasicHorizontalLayoutManager(BasicSplitPaneUI ui, int axis) {
            this.ui = ui;
            this.axis = axis;
        }

        /** How much that component measures on the axis that is shared out. */
        protected int getSizeOfComponent(Component comp) {
            Dimension d = comp.getSize();
            return (axis == JSplitPane.HORIZONTAL_SPLIT) ? d.width : d.height;
        }

        protected int getPreferredSizeOfComponent(Component comp) {
            Dimension d = comp.getPreferredSize();
            return (axis == JSplitPane.HORIZONTAL_SPLIT) ? d.width : d.height;
        }

        private int getMinimumSizeOfComponent(Component comp) {
            Dimension d = comp.getMinimumSize();
            return (axis == JSplitPane.HORIZONTAL_SPLIT) ? d.width : d.height;
        }

        /** How much room there is to share out, taking away the margins. */
        protected int getAvailableSize(Dimension containerSize, Insets insets) {
            if (insets == null) {
                return (axis == JSplitPane.HORIZONTAL_SPLIT)
                        ? containerSize.width : containerSize.height;
            }
            return (axis == JSplitPane.HORIZONTAL_SPLIT)
                    ? (containerSize.width - insets.left - insets.right)
                    : (containerSize.height - insets.top - insets.bottom);
        }

        /** Where the sharing out begins. */
        protected int getInitialLocation(Insets insets) {
            if (insets == null) {
                return 0;
            }
            return (axis == JSplitPane.HORIZONTAL_SPLIT) ? insets.left : insets.top;
        }

        protected int[] getSizes() {
            int[] copy = new int[3];
            System.arraycopy(sizes, 0, copy, 0, 3);
            return copy;
        }

        protected void setSizes(int[] newSizes) {
            System.arraycopy(newSizes, 0, sizes, 0, 3);
        }

        protected void resetSizeAt(int index) {
            sizes[index] = 0;
        }

        /** It reads again which component takes up each place. */
        protected void updateComponents() {
            Component comp = ui.splitPane.getLeftComponent();
            if (components[0] != comp) {
                components[0] = comp;
                if (comp == null) {
                    sizes[0] = 0;
                }
            }
            comp = ui.splitPane.getRightComponent();
            if (components[1] != comp) {
                components[1] = comp;
                if (comp == null) {
                    sizes[1] = 0;
                }
            }
            components[2] = ui.divider;
            if (ui.divider != null) {
                sizes[2] = ui.divider.getDividerSize();
            }
        }

        public void resetToPreferredSizes() {
            for (int i = 0; i < 2; i++) {
                sizes[i] = (components[i] != null)
                        ? getPreferredSizeOfComponent(components[i]) : 0;
            }
        }

        /** It puts the divider at that position and shares out what is left. */
        void setDividerLocation(int location) {
            updateComponents();
            Insets insets = ui.splitPane.getInsets();
            int start = getInitialLocation(insets);
            int total = getAvailableSize(ui.splitPane.getSize(), insets);
            sizes[0] = Math.max(0, location - start);
            sizes[2] = (ui.divider != null) ? ui.divider.getDividerSize() : 0;
            sizes[1] = Math.max(0, total - sizes[0] - sizes[2]);
        }

        public void addLayoutComponent(String place, Component component) {
            addLayoutComponent(component, place);
        }

        public void addLayoutComponent(Component comp, Object constraints) {
            updateComponents();
        }

        public void removeLayoutComponent(Component component) {
            for (int i = 0; i < 3; i++) {
                if (components[i] == component) {
                    components[i] = null;
                    sizes[i] = 0;
                }
            }
        }

        public void invalidateLayout(Container c) {
        }

        public float getLayoutAlignmentX(Container target) {
            return 0.0f;
        }

        public float getLayoutAlignmentY(Container target) {
            return 0.0f;
        }

        private Dimension measure(Container container, boolean min) {
            updateComponents();
            int length = 0;
            int thick = 0;
            for (int i = 0; i < 3; i++) {
                Component c = components[i];
                if (c == null || !c.isVisible()) {
                    continue;
                }
                Dimension d = min ? c.getMinimumSize() : c.getPreferredSize();
                if (axis == JSplitPane.HORIZONTAL_SPLIT) {
                    length += d.width;
                    thick = Math.max(thick, d.height);
                } else {
                    length += d.height;
                    thick = Math.max(thick, d.width);
                }
            }
            Insets insets = container.getInsets();
            int w;
            int h;
            if (axis == JSplitPane.HORIZONTAL_SPLIT) {
                w = length;
                h = thick;
            } else {
                w = thick;
                h = length;
            }
            if (insets != null) {
                w += insets.left + insets.right;
                h += insets.top + insets.bottom;
            }
            return new Dimension(w, h);
        }

        public Dimension preferredLayoutSize(Container container) {
            return measure(container, false);
        }

        public Dimension minimumLayoutSize(Container container) {
            return measure(container, true);
        }

        /** No cap. */
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        /** It gives each one what the table says, handing out the difference. */
        public void layoutContainer(Container container) {
            updateComponents();
            Insets insets = container.getInsets();
            int total = getAvailableSize(container.getSize(), insets);
            int start = getInitialLocation(insets);
            int divisor = (ui.divider != null) ? ui.divider.getDividerSize() : 0;
            sizes[2] = divisor;
            int busy = sizes[0] + sizes[1] + divisor;
            if (busy <= 0) {
                // The first time: half for each.
                sizes[0] = Math.max(0, (total - divisor) / 2);
                sizes[1] = Math.max(0, total - divisor - sizes[0]);
            } else if (busy != total) {
                // The whole difference goes to the second one, which is what makes the divider stay
                                // where the user left it.
                sizes[1] = Math.max(0, total - divisor - sizes[0]);
            }
            int pos = start;
            for (int i = 0; i < 3; i++) {
                int idx = (i == 0) ? 0 : ((i == 1) ? 2 : 1);
                Component c = components[idx];
                int tam = sizes[idx];
                if (c != null) {
                    setComponentToSize(c, tam, pos, insets, container.getSize());
                }
                pos += tam;
            }
        }

        /** It gives a component that size on the axis that is shared out, and all of the other. */
        protected void setComponentToSize(Component c, int size, int location, Insets insets,
                Dimension containerSize) {
            if (insets == null) {
                insets = new Insets(0, 0, 0, 0);
            }
            if (axis == JSplitPane.HORIZONTAL_SPLIT) {
                c.setBounds(location, insets.top, size,
                        containerSize.height - insets.top - insets.bottom);
            } else {
                c.setBounds(insets.left, location,
                        containerSize.width - insets.left - insets.right, size);
            }
        }
    }

    /** The same on the other axis; see {@link BasicHorizontalLayoutManager}. */
    public static class BasicVerticalLayoutManager extends BasicHorizontalLayoutManager {

        public BasicVerticalLayoutManager(BasicSplitPaneUI ui) {
            super(ui, JSplitPane.VERTICAL_SPLIT);
        }
    }

    /**
     * The shadow that is seen while dragging without laying out; see the class note.
     *
     * <p>It is an opaque component of a dark colour and nothing else: the only thing it has to do
     * is cover itself and move.
     */
    private static class DragShadow extends java.awt.Canvas {

        private final BasicSplitPaneUI ui;

        DragShadow(BasicSplitPaneUI ui) {
            this.ui = ui;
            setBackground(Color.darkGray);
        }

        public Dimension getPreferredSize() {
            if (ui.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                return new Dimension(ui.dividerSize, 1);
            }
            return new Dimension(1, ui.dividerSize);
        }
    }

    /** It reacts to the pane's changes: axis, divider, children, and dragging mode. */
    private class Handler implements PropertyChangeListener, FocusListener {

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() != splitPane) {
                return;
            }
            String name = e.getPropertyName();
            if (JSplitPane.ORIENTATION_PROPERTY.equals(name)) {
                orientation = splitPane.getOrientation();
                resetLayoutManager();
            } else if (JSplitPane.CONTINUOUS_LAYOUT_PROPERTY.equals(name)) {
                setContinuousLayout(splitPane.isContinuousLayout());
            } else if (JSplitPane.DIVIDER_SIZE_PROPERTY.equals(name)) {
                divider.setDividerSize(splitPane.getDividerSize());
                dividerSize = divider.getDividerSize();
                splitPane.revalidate();
                splitPane.repaint();
            } else if (JSplitPane.LEFT.equals(name) || JSplitPane.RIGHT.equals(name)
                    || JSplitPane.TOP.equals(name) || JSplitPane.BOTTOM.equals(name)) {
                if (layoutManager != null) {
                    layoutManager.updateComponents();
                }
                splitPane.revalidate();
                splitPane.repaint();
            } else if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(name)) {
                Object newValue = e.getNewValue();
                if (newValue instanceof Number && layoutManager != null) {
                    layoutManager.setDividerLocation(((Number) newValue).intValue());
                    splitPane.revalidate();
                    splitPane.repaint();
                }
            }
        }

        public void focusGained(FocusEvent e) {
            if (divider != null) {
                divider.repaint();
            }
        }

        public void focusLost(FocusEvent e) {
            if (divider != null) {
                divider.repaint();
            }
        }
    }
}
