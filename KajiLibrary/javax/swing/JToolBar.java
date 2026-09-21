package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.LayoutManager2;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ToolBarUI;

/**
 * The row of buttons that goes below the menu.
 *
 * <h2>It is a panel with a rule</h2>
 *
 * <p>Any component goes inside: buttons, combo boxes, fields. The only thing it adds over an
 * ordinary panel is that it knows how to lay itself out in a row or in a column and, if it is
 * allowed, that it can be torn from its place and left floating in a little window
 * ({@link #setFloatable}).
 *
 * <h2>Adding an action creates the button</h2>
 *
 * <p>{@link #add(Action)} does not add the action: it builds a {@link JButton} configured from
 * it and adds that. It returns the button precisely so that it can be touched up. The button it
 * builds does not show the action's text if it has an icon -- a tool bar with text on each
 * button takes up twice the room -- and that is decided by {@link #createActionComponent},
 * which a subclass may change.
 *
 * <h2>The separator is not the menus'</h2>
 *
 * <p>{@link Separator} inherits from {@link JSeparator} but behaves the other way round in one
 * point: its size is fixed and it does not stretch. A separator that stretched would leave the
 * buttons bunched up on one side.
 */
public class JToolBar extends JComponent implements SwingConstants, Accessible {

    private static final String uiClassID = "ToolBarUI";

    private boolean paintBorder = true;
    private Insets margin = null;
    private boolean floatable = true;
    private int orientation = HORIZONTAL;

    /** Horizontal and with no name. */
    public JToolBar() {
        this(HORIZONTAL);
    }

    /**
     * With that orientation.
     *
     * @throws IllegalArgumentException if it is neither horizontal nor vertical.
     */
    public JToolBar(int orientation) {
        this(null, orientation);
    }

    /** With that name, horizontal; the name is the little window's title when floating. */
    public JToolBar(String name) {
        this(name, HORIZONTAL);
    }

    /**
     * With name and orientation.
     *
     * @throws IllegalArgumentException if the orientation is neither horizontal nor vertical.
     */
    public JToolBar(String name, int orientation) {
        setName(name);
        checkOrientation(orientation);
        this.orientation = orientation;
        DefaultToolBarLayout layout = new DefaultToolBarLayout(this, orientation);
        setLayout(layout);
        addPropertyChangeListener(layout);
        updateUI();
    }

    public ToolBarUI getUI() {
        return (ToolBarUI) ui;
    }

    public void setUI(ToolBarUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** At what position that component is, or -1 if it is not there. */
    public int getComponentIndex(Component c) {
        int ncomponents = this.getComponentCount();
        Component[] component = this.getComponents();
        for (int i = 0; i < ncomponents; i++) {
            Component comp = component[i];
            if (comp == c) {
                return i;
            }
        }
        return -1;
    }

    /** The component at that position, or null if it is out of range. */
    public Component getComponentAtIndex(int i) {
        int ncomponents = this.getComponentCount();
        if (i >= 0 && i < ncomponents) {
            Component[] component = this.getComponents();
            return component[i];
        }
        return null;
    }

    /** The bar's margins; null leaves those the look and feel sets. */
    public void setMargin(Insets m) {
        Insets old = margin;
        margin = m;
        firePropertyChange("margin", old, m);
        revalidate();
        repaint();
    }

    public Insets getMargin() {
        if (margin == null) {
            return new Insets(0, 0, 0, 0);
        }
        return margin;
    }

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        if (paintBorder != b) {
            boolean old = paintBorder;
            paintBorder = b;
            firePropertyChange("borderPainted", old, b);
            revalidate();
            repaint();
        }
    }

    /** It draws the border only if it is switched on. */
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    /** Whether it can be torn from its place; see the class note. */
    public boolean isFloatable() {
        return floatable;
    }

    public void setFloatable(boolean b) {
        if (floatable != b) {
            boolean old = floatable;
            floatable = b;
            firePropertyChange("floatable", old, b);
            revalidate();
            repaint();
        }
    }

    public int getOrientation() {
        return orientation;
    }

    /**
     * In a row or in a column.
     *
     * @throws IllegalArgumentException if it is neither horizontal nor vertical.
     */
    public void setOrientation(int o) {
        checkOrientation(o);
        if (orientation != o) {
            int old = orientation;
            orientation = o;
            firePropertyChange("orientation", old, o);
            revalidate();
            repaint();
        }
    }

    private void checkOrientation(int orientation) {
        if (orientation != VERTICAL && orientation != HORIZONTAL) {
            throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
        }
    }

    /** Whether the buttons are drawn flat until the pointer passes over them. */
    public void setRollover(boolean rollover) {
        putClientProperty("JToolBar.isRollover", rollover ? Boolean.TRUE : Boolean.FALSE);
    }

    public boolean isRollover() {
        Boolean rollover = (Boolean) getClientProperty("JToolBar.isRollover");
        if (rollover != null) {
            return rollover.booleanValue();
        }
        return false;
    }

    /** A separator of the size the look and feel decides. */
    public void addSeparator() {
        JToolBar.Separator s = new JToolBar.Separator();
        add(s);
    }

    /** A separator of that size. */
    public void addSeparator(Dimension size) {
        JToolBar.Separator s = new JToolBar.Separator(size);
        add(s);
    }

    /**
     * It builds a button from that action and adds it.
     *
     * @return the button, so that it can be touched up.
     */
    public JButton add(Action a) {
        JButton b = createActionComponent(a);
        b.setAction(a);
        add(b);
        return b;
    }

    /**
     * The button that represents that action.
     *
     * <p>With no text if the action brings an icon: see the class note.
     */
    protected JButton createActionComponent(Action a) {
        String text = a != null ? (String) a.getValue(Action.NAME) : null;
        Icon icon = a != null ? (Icon) a.getValue(Action.SMALL_ICON) : null;
        boolean enabled = a != null ? a.isEnabled() : true;
        String tooltip = a != null ? (String) a.getValue(Action.SHORT_DESCRIPTION) : null;
        JButton b = new JButton(text, icon) {
            protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
                PropertyChangeListener pcl = createActionChangeListener(this);
                if (pcl == null) {
                    pcl = super.createActionPropertyChangeListener(a);
                }
                return pcl;
            }
        };
        if (icon != null) {
            b.putClientProperty("hideActionText", Boolean.TRUE);
        }
        b.setHorizontalTextPosition(JButton.CENTER);
        b.setVerticalTextPosition(JButton.BOTTOM);
        b.setEnabled(enabled);
        b.setToolTipText(tooltip);
        return b;
    }

    /** Null: the button uses the listener {@code AbstractButton} builds on its own. */
    protected PropertyChangeListener createActionChangeListener(JButton b) {
        return null;
    }

    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof Separator) {
            if (getOrientation() == VERTICAL) {
                ((Separator) comp).setOrientation(JSeparator.HORIZONTAL);
            } else {
                ((Separator) comp).setOrientation(JSeparator.VERTICAL);
            }
        }
        super.addImpl(comp, constraints, index);
    }

    protected String paramString() {
        return super.paramString();
    }

    public void setLayout(LayoutManager mgr) {
        super.setLayout(mgr);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * A tool bar's separator.
     *
     * <p>Its size is the same for the minimum, the preferred and the maximum: it does not
     * stretch. See {@link JToolBar}'s note.
     */
    public static class Separator extends JSeparator {

        private Dimension separatorSize;

        /** Of the size the look and feel decides. */
        public Separator() {
            this(null);
        }

        /** Of that size. */
        public Separator(Dimension size) {
            super(JSeparator.HORIZONTAL);
            setSeparatorSize(size);
        }

        public String getUIClassID() {
            return "ToolBarSeparatorUI";
        }

        /** Null gives the decision back to the look and feel. */
        public void setSeparatorSize(Dimension size) {
            if (size != null) {
                separatorSize = size;
            } else {
                super.updateUI();
            }
            this.invalidate();
        }

        public Dimension getSeparatorSize() {
            return separatorSize;
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            if (separatorSize != null) {
                return separatorSize.getSize();
            }
            return super.getPreferredSize();
        }
    }

    /**
     * It lays out in a row or in a column, following the bar's orientation.
     *
     * <p>It does not inherit from {@link BoxLayout}: **it wraps it**. A {@code BoxLayout} ties
     * itself to the container it is passed in the constructor and refuses to lay another one out,
     * so turning the bar forces a new one to be built. By wrapping it the inner one can be
     * replaced without changing the layout the bar has set.
     */
    private static class DefaultToolBarLayout implements LayoutManager2, PropertyChangeListener,
            java.io.Serializable {

        private final JToolBar bar;
        private BoxLayout lm;

        DefaultToolBarLayout(JToolBar bar, int orientation) {
            this.bar = bar;
            this.lm = build(bar, orientation);
        }

        private static BoxLayout build(JToolBar bar, int orientation) {
            if (orientation == JToolBar.VERTICAL) {
                return new BoxLayout(bar, BoxLayout.PAGE_AXIS);
            }
            return new BoxLayout(bar, BoxLayout.LINE_AXIS);
        }

        public void addLayoutComponent(String name, Component comp) {
            lm.addLayoutComponent(name, comp);
        }

        public void addLayoutComponent(Component comp, Object constraints) {
            lm.addLayoutComponent(comp, constraints);
        }

        public void removeLayoutComponent(Component comp) {
            lm.removeLayoutComponent(comp);
        }

        public Dimension preferredLayoutSize(Container target) {
            return lm.preferredLayoutSize(target);
        }

        public Dimension minimumLayoutSize(Container target) {
            return lm.minimumLayoutSize(target);
        }

        public Dimension maximumLayoutSize(Container target) {
            return lm.maximumLayoutSize(target);
        }

        public void layoutContainer(Container target) {
            lm.layoutContainer(target);
        }

        public float getLayoutAlignmentX(Container target) {
            return lm.getLayoutAlignmentX(target);
        }

        public float getLayoutAlignmentY(Container target) {
            return lm.getLayoutAlignmentY(target);
        }

        public void invalidateLayout(Container target) {
            lm.invalidateLayout(target);
        }

        /** Turning the bar changes the axis; see the class note. */
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if (name.equals("orientation")) {
                int o = ((Integer) e.getNewValue()).intValue();
                lm = build(bar, o);
            }
        }
    }
}
