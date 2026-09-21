package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Graphics;

import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SplitPaneUI;

/**
 * Two components separated by a division that can be dragged.
 *
 * <h2>The children are set by position, not by order</h2>
 *
 * <p>{@code add(comp, JSplitPane.LEFT)} and {@code setLeftComponent(comp)} are the same.
 * Adding without saying where puts the first on the left and the second on the right, and the
 * third replaces the first: a split pane has exactly two places.
 *
 * <h2>Where the division ends up on changing size</h2>
 *
 * <p>{@link #setResizeWeight} decides it: zero gives all the new space to the right one, one to
 * the left one, and a half shares it out. It is the most forgotten property and the one that
 * explains why a split pane "does not respect" the size one gave it.
 *
 * <h2>Continuous or not</h2>
 *
 * <p>With {@link #setContinuousLayout} switched on, dragging the division lays the two sides
 * out at every pixel. Switched off, only a line moves and the laying out happens on releasing.
 * Switched off exists because laying an expensive content out sixty times a second feels worse
 * than a line.
 */
public class JSplitPane extends JComponent implements javax.accessibility.Accessible {

    private static final String uiClassID = "SplitPaneUI";

    /** The components go one above the other. */
    public static final int VERTICAL_SPLIT = 0;

    /** One beside the other. */
    public static final int HORIZONTAL_SPLIT = 1;

    /** The left-hand place. */
    public static final String LEFT = "left";

    /** The right-hand one. */
    public static final String RIGHT = "right";

    /** The top one; it is the same place as {@link #LEFT}. */
    public static final String TOP = "top";

    /** The bottom one; the same as {@link #RIGHT}. */
    public static final String BOTTOM = "bottom";

    /** The division itself, which the look and feel adds as a child. */
    public static final String DIVIDER = "divider";

    public static final String ORIENTATION_PROPERTY = "orientation";
    public static final String CONTINUOUS_LAYOUT_PROPERTY = "continuousLayout";
    public static final String DIVIDER_SIZE_PROPERTY = "dividerSize";
    public static final String ONE_TOUCH_EXPANDABLE_PROPERTY = "oneTouchExpandable";
    public static final String LAST_DIVIDER_LOCATION_PROPERTY = "lastDividerLocation";
    public static final String DIVIDER_LOCATION_PROPERTY = "dividerLocation";
    public static final String RESIZE_WEIGHT_PROPERTY = "resizeWeight";

    /** {@link #HORIZONTAL_SPLIT} or {@link #VERTICAL_SPLIT}. */
    protected int orientation;

    /** Whether dragging lays out at each step; see the class note. */
    protected boolean continuousLayout;

    /** The left-hand or top component. */
    protected Component leftComponent;

    /** The right-hand or bottom one. */
    protected Component rightComponent;

    /** The division's width. */
    protected int dividerSize;

    /** Whether the division has little arrows for folding one side with one click. */
    protected boolean oneTouchExpandable;

    /** Where the division was before the last movement. */
    protected int lastDividerLocation;

    private double resizeWeight;
    private boolean dividerSizeSet = false;
    private AccessibleContext accessibleContext;

    /** A pane divided in two across, empty. */
    public JSplitPane() {
        this(HORIZONTAL_SPLIT, false, new JButton("left"), new JButton("right"));
    }

    /** A pane divided with that orientation. */
    public JSplitPane(int newOrientation) {
        this(newOrientation, false);
    }

    /** With that orientation and that dragging mode. */
    public JSplitPane(int newOrientation, boolean newContinuousLayout) {
        this(newOrientation, newContinuousLayout, null, null);
    }

    /** With that orientation and those two components. */
    public JSplitPane(int newOrientation, Component newLeftComponent,
            Component newRightComponent) {
        this(newOrientation, false, newLeftComponent, newRightComponent);
    }

    /**
     * The complete constructor.
     *
     * @throws IllegalArgumentException if the orientation is not one of the two.
     */
    public JSplitPane(int newOrientation, boolean newContinuousLayout,
            Component newLeftComponent, Component newRightComponent) {
        super();
        dividerLocation = -1;
        setLayout(null);
        setUIProperty("opaque", Boolean.TRUE);
        orientation = newOrientation;
        if (orientation != HORIZONTAL_SPLIT && orientation != VERTICAL_SPLIT) {
            throw new IllegalArgumentException("cannot create JSplitPane, "
                    + "orientation must be one of "
                    + "JSplitPane.HORIZONTAL_SPLIT or JSplitPane.VERTICAL_SPLIT");
        }
        continuousLayout = newContinuousLayout;
        if (newLeftComponent != null) {
            setLeftComponent(newLeftComponent);
        }
        if (newRightComponent != null) {
            setRightComponent(newRightComponent);
        }
        updateUI();
    }

    private int dividerLocation;

    public void setComponentOrientation(ComponentOrientation orientation) {
        super.setComponentOrientation(orientation);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    public void setUI(SplitPaneUI ui) {
        if ((SplitPaneUI) this.ui != ui) {
            super.setUI(ui);
            revalidate();
        }
    }

    public SplitPaneUI getUI() {
        return (SplitPaneUI) ui;
    }

    public void updateUI() {
        revalidate();
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** The division's width, in pixels. */
    public void setDividerSize(int newSize) {
        int oldSize = dividerSize;
        dividerSizeSet = true;
        if (oldSize != newSize) {
            dividerSize = newSize;
            firePropertyChange(DIVIDER_SIZE_PROPERTY, oldSize, newSize);
        }
    }

    public int getDividerSize() {
        return dividerSize;
    }

    /** The left-hand or top component. */
    public void setLeftComponent(Component comp) {
        if (comp == null) {
            if (leftComponent != null) {
                remove(leftComponent);
                leftComponent = null;
            }
        } else {
            add(comp, JSplitPane.LEFT);
        }
    }

    public Component getLeftComponent() {
        return leftComponent;
    }

    /** The same place as {@link #setLeftComponent}. */
    public void setTopComponent(Component comp) {
        setLeftComponent(comp);
    }

    public Component getTopComponent() {
        return leftComponent;
    }

    public void setRightComponent(Component comp) {
        if (comp == null) {
            if (rightComponent != null) {
                remove(rightComponent);
                rightComponent = null;
            }
        } else {
            add(comp, JSplitPane.RIGHT);
        }
    }

    public Component getRightComponent() {
        return rightComponent;
    }

    public void setBottomComponent(Component comp) {
        setRightComponent(comp);
    }

    public Component getBottomComponent() {
        return rightComponent;
    }

    /** Whether the division carries little arrows for folding one side. */
    public void setOneTouchExpandable(boolean newValue) {
        boolean oldValue = oneTouchExpandable;
        oneTouchExpandable = newValue;
        firePropertyChange(ONE_TOUCH_EXPANDABLE_PROPERTY, oldValue, newValue);
        repaint();
    }

    public boolean isOneTouchExpandable() {
        return oneTouchExpandable;
    }

    /**
     * Where the division was before.
     *
     * <p>It is what the little arrows use in order to go back: folding and unfolding has to leave
     * the division where it was, not at a computed place.
     */
    public void setLastDividerLocation(int newLastLocation) {
        int oldLocation = lastDividerLocation;
        lastDividerLocation = newLastLocation;
        firePropertyChange(LAST_DIVIDER_LOCATION_PROPERTY, oldLocation, newLastLocation);
    }

    public int getLastDividerLocation() {
        return lastDividerLocation;
    }

    /**
     * Whether the components go side by side or one over the other.
     *
     * @throws IllegalArgumentException if it is not one of the two.
     */
    public void setOrientation(int orientation) {
        if ((orientation != VERTICAL_SPLIT) && (orientation != HORIZONTAL_SPLIT)) {
            throw new IllegalArgumentException("JSplitPane: orientation must be "
                    + "one of JSplitPane.VERTICAL_SPLIT or JSplitPane.HORIZONTAL_SPLIT");
        }
        int oldOrientation = this.orientation;
        this.orientation = orientation;
        firePropertyChange(ORIENTATION_PROPERTY, oldOrientation, orientation);
    }

    public int getOrientation() {
        return orientation;
    }

    /** Whether dragging lays out at each step; see the class note. */
    public void setContinuousLayout(boolean newContinuousLayout) {
        boolean oldCD = continuousLayout;
        continuousLayout = newContinuousLayout;
        firePropertyChange(CONTINUOUS_LAYOUT_PROPERTY, oldCD, newContinuousLayout);
    }

    public boolean isContinuousLayout() {
        return continuousLayout;
    }

    /**
     * How the new space is shared out on enlarging.
     *
     * @throws IllegalArgumentException if it is not between zero and one.
     */
    public void setResizeWeight(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("JSplitPane weight must be between 0 and 1");
        }
        double oldWeight = resizeWeight;
        resizeWeight = value;
        firePropertyChange(RESIZE_WEIGHT_PROPERTY, oldWeight, value);
    }

    public double getResizeWeight() {
        return resizeWeight;
    }

    /** It puts the division where both sides have their preferred size. */
    public void resetToPreferredSizes() {
        SplitPaneUI ui = getUI();
        if (ui != null) {
            ui.resetToPreferredSizes(this);
        }
    }

    /**
     * The divider's position as a proportion of the space.
     *
     * <p>Zero sticks it to the beginning, one to the end, {@code 0.5} leaves it in the middle. It
     * is translated into pixels <strong>now</strong>, with the size the pane has at this moment:
     * it is not a proportion that is kept on changing size -- that is what the resize weight is
     * for.
     *
     * @throws IllegalArgumentException if it is not between zero and one
     */
    public void setDividerLocation(double proportionalLocation) {
        if (proportionalLocation < 0.0 || proportionalLocation > 1.0) {
            throw new IllegalArgumentException(
                    "proportional location must be between 0.0 and 1.0.");
        }
        if (getOrientation() == VERTICAL_SPLIT) {
            setDividerLocation((int) ((double) (getHeight() - getDividerSize())
                    * proportionalLocation));
        } else {
            setDividerLocation((int) ((double) (getWidth() - getDividerSize())
                    * proportionalLocation));
        }
    }

    /**
     * It moves the division to that pixel.
     *
     * <p>A negative value means "lay it out by yourself", which is what a newly built pane
     * does.
     */
    public void setDividerLocation(int location) {
        int oldValue = dividerLocation;
        dividerLocation = location;
        SplitPaneUI ui = getUI();
        if (ui != null) {
            ui.setDividerLocation(this, location);
        }
        firePropertyChange(DIVIDER_LOCATION_PROPERTY, oldValue, location);
    }

    public int getDividerLocation() {
        return dividerLocation;
    }

    /** The furthest left the division may go without shrinking the first one too much. */
    public int getMinimumDividerLocation() {
        SplitPaneUI ui = getUI();
        return (ui != null) ? ui.getMinimumDividerLocation(this) : -1;
    }

    public int getMaximumDividerLocation() {
        SplitPaneUI ui = getUI();
        return (ui != null) ? ui.getMaximumDividerLocation(this) : -1;
    }

    /** It removes a component and forgets its place. */
    public void remove(Component component) {
        if (component == leftComponent) {
            leftComponent = null;
        } else if (component == rightComponent) {
            rightComponent = null;
        }
        super.remove(component);
        revalidate();
        repaint();
    }

    public void remove(int index) {
        Component comp = getComponent(index);
        if (comp == leftComponent) {
            leftComponent = null;
        } else if (comp == rightComponent) {
            rightComponent = null;
        }
        super.remove(index);
        revalidate();
        repaint();
    }

    public void removeAll() {
        leftComponent = null;
        rightComponent = null;
        super.removeAll();
        revalidate();
        repaint();
    }

    /**
     * Whether a change inside can be laid out without rebuilding the whole window.
     *
     * <p>Always true. The split pane shares a fixed space out between two sides: whatever happens
     * inside either of them does not change what the pane takes up, so the laying out may stop
     * here instead of going up as far as the window.
     */
    public boolean isValidateRoot() {
        return true;
    }

    /**
     * It adds a child in the place the constraint says.
     *
     * <p>With no constraint, the first goes on the left and the second on the right. It is what
     * makes {@code add(a); add(b);} build the pane one expects.
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        Component toRemove;
        if (constraints != null && !(constraints instanceof String)) {
            throw new IllegalArgumentException("cannot add to layout: "
                    + "constraint must be a string (or null)");
        }
        if (constraints == null) {
            if (getLeftComponent() == null) {
                constraints = JSplitPane.LEFT;
            } else if (getRightComponent() == null) {
                constraints = JSplitPane.RIGHT;
            }
        }
        if (constraints != null && (constraints.equals(JSplitPane.LEFT)
                || constraints.equals(JSplitPane.TOP))) {
            toRemove = getLeftComponent();
            if (toRemove != null) {
                remove(toRemove);
            }
            leftComponent = comp;
            index = -1;
        } else if (constraints != null && (constraints.equals(JSplitPane.RIGHT)
                || constraints.equals(JSplitPane.BOTTOM))) {
            toRemove = getRightComponent();
            if (toRemove != null) {
                remove(toRemove);
            }
            rightComponent = comp;
            index = -1;
        } else if (constraints != null && constraints.equals(JSplitPane.DIVIDER)) {
            index = -1;
        }
        super.addImpl(comp, constraints, index);
        revalidate();
        repaint();
    }

    /** It draws the children and then lets the look and feel finish the division. */
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        SplitPaneUI ui = getUI();
        if (ui != null) {
            Graphics tempG = g.create();
            ui.finishedPaintingChildren(this, tempG);
            tempG.dispose();
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
