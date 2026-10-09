package java.awt;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A container of **a single child** that shows part of it and lets one scroll through the rest.
 *
 * <p>The "single child" part is literal: adding a second one takes the first away. When several
 * things have to be scrolled, a {@link Panel} goes inside and the components go in it.
 *
 * <p>It has a layout of its own and {@link #setLayout} is `final`: changing it would break the only
 * thing the pane does.
 */
public class ScrollPane extends Container implements Accessible {

    private static final long serialVersionUID = 7956609840827222915L;

    private static int scrollPaneCounter = 0;

    /** Shows the bars only when the child does not fit. */
    public static final int SCROLLBARS_AS_NEEDED = 0;

    /** Shows them always. */
    public static final int SCROLLBARS_ALWAYS = 1;

    /** Never shows them; the scrolling is left to the program only. */
    public static final int SCROLLBARS_NEVER = 2;

    /** Which of the three policies was asked for. */
    private final int scrollbarDisplayPolicy;

    /** The vertical bar. */
    private final ScrollPaneAdjustable vAdjustable;

    /** The horizontal one. */
    private final ScrollPaneAdjustable hAdjustable;

    /** Whether the mouse wheel scrolls it. */
    private boolean wheelScrollingEnabled = true;

    /** A pane that shows the bars when they are needed. */
    public ScrollPane() throws HeadlessException {
        this(SCROLLBARS_AS_NEEDED);
    }

    /**
     * A pane with that bar policy.
     *
     * @throws IllegalArgumentException if the policy is not one of the three
     */
    public ScrollPane(int scrollbarDisplayPolicy) throws HeadlessException {
        if (scrollbarDisplayPolicy != SCROLLBARS_AS_NEEDED
                && scrollbarDisplayPolicy != SCROLLBARS_ALWAYS
                && scrollbarDisplayPolicy != SCROLLBARS_NEVER) {
            throw new IllegalArgumentException("illegal scrollbar display policy");
        }
        this.scrollbarDisplayPolicy = scrollbarDisplayPolicy;
        this.vAdjustable = new ScrollPaneAdjustable(this, null, Adjustable.VERTICAL);
        this.hAdjustable = new ScrollPaneAdjustable(this, null, Adjustable.HORIZONTAL);
        super.setLayout(null);
    }

    String constructComponentName() {
        synchronized (ScrollPane.class) {
            String n = "scrollpane" + scrollPaneCounter;
            scrollPaneCounter = scrollPaneCounter + 1;
            return n;
        }
    }

    /**
     * Adds the child, taking away whichever one was there.
     *
     * <p>It is `final` and it makes the previous one disappear on purpose: a scroll pane with two
     * children makes no sense, and letting them be added only to ignore one afterwards would be
     * worse.
     */
    protected final void addImpl(Component comp, Object constraints, int index) {
        if (this.getComponentCount() > 0) {
            this.remove(0);
        }
        super.addImpl(comp, constraints, 0);
    }

    /** Which bar policy was asked for. */
    public int getScrollbarDisplayPolicy() {
        return this.scrollbarDisplayPolicy;
    }

    /**
     * How much of the child is seen.
     *
     * <p>Since the bars take up no room without a screen, it is the size of the pane minus its
     * insets.
     */
    public Dimension getViewportSize() {
        Insets i = this.getInsets();
        return new Dimension(this.getWidth() - i.left - i.right,
                this.getHeight() - i.top - i.bottom);
    }

    /**
     * How much height the horizontal bar takes.
     *
     * @return 0: without a screen there is no drawn bar taking up room
     */
    public int getHScrollbarHeight() {
        return 0;
    }

    /**
     * How much width the vertical one takes.
     *
     * @return 0, for the same reason
     */
    public int getVScrollbarWidth() {
        return 0;
    }

    /** The vertical bar. */
    public Adjustable getVAdjustable() {
        return this.vAdjustable;
    }

    /** The horizontal one. */
    public Adjustable getHAdjustable() {
        return this.hAdjustable;
    }

    /**
     * Scrolls to that position of the child.
     *
     * <p>It is clamped to what can really be scrolled, which is the size of the child minus that of
     * the viewport. Scrolling beyond that would leave a gap below the content.
     *
     * @throws NullPointerException if the pane has no child
     */
    public void setScrollPosition(int x, int y) {
        synchronized (this.getTreeLock()) {
            if (this.getComponentCount() == 0) {
                throw new NullPointerException("Child does not exist");
            }
            Component child = this.getComponent(0);
            Dimension v = this.getViewportSize();
            int maxX = Math.max(0, child.getWidth() - v.width);
            int maxY = Math.max(0, child.getHeight() - v.height);
            int nx = Math.max(0, Math.min(x, maxX));
            int ny = Math.max(0, Math.min(y, maxY));
            Insets i = this.getInsets();
            child.setLocation(i.left - nx, i.top - ny);
            this.updateAdjustables();
        }
    }

    /**
     * Scrolls to that position.
     *
     * @throws NullPointerException if the point is `null` or the pane has no child
     */
    public void setScrollPosition(Point p) {
        this.setScrollPosition(p.x, p.y);
    }

    /**
     * Where the scrolling is at.
     *
     * @throws NullPointerException if the pane has no child
     */
    public Point getScrollPosition() {
        synchronized (this.getTreeLock()) {
            if (this.getComponentCount() == 0) {
                throw new NullPointerException("Child does not exist");
            }
            Component child = this.getComponent(0);
            Insets i = this.getInsets();
            return new Point(i.left - child.getX(), i.top - child.getY());
        }
    }

    /**
     * The layout cannot be changed.
     *
     * @throws AWTError always: the pane has its own and it is the only thing it does
     */
    public final void setLayout(LayoutManager mgr) {
        throw new AWTError("ScrollPane controls layout");
    }

    /**
     * Lays the child out.
     *
     * <p>It gives it its preferred size, or the viewport's if the preferred one is smaller: a child
     * smaller than the viewport fills it instead of leaving an unused border.
     */
    public void doLayout() {
        this.layout();
    }

    /** How much the child has to measure. */
    Dimension calculateChildSize() {
        Component child = this.getComponent(0);
        Dimension p = child.getPreferredSize();
        Dimension v = this.getViewportSize();
        return new Dimension(Math.max(p.width, v.width), Math.max(p.height, v.height));
    }

    /**
     * Lays the child out.
     *
     * @deprecated it is from the 1.1 naming. Use {@link #doLayout}.
     */
    @Deprecated
    public void layout() {
        synchronized (this.getTreeLock()) {
            if (this.getComponentCount() == 0) {
                return;
            }
            Component child = this.getComponent(0);
            Dimension d = this.calculateChildSize();
            child.setSize(d.width, d.height);
            this.updateAdjustables();
        }
    }

    /** Sets the range of both bars from the size of the child and of the viewport. */
    private void updateAdjustables() {
        if (this.getComponentCount() == 0) {
            return;
        }
        Component child = this.getComponent(0);
        Dimension v = this.getViewportSize();
        this.hAdjustable.setSpan(0, child.getWidth(), v.width);
        this.vAdjustable.setSpan(0, child.getHeight(), v.height);
    }

    /**
     * Prints the children.
     *
     * <p>It does nothing: printing needs a real {@link Graphics} and this implementation has no
     * rasteriser.
     */
    public void printComponents(Graphics g) {
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    public String paramString() {
        String policyName = "as-needed";
        if (this.scrollbarDisplayPolicy == SCROLLBARS_ALWAYS) {
            policyName = "always";
        } else if (this.scrollbarDisplayPolicy == SCROLLBARS_NEVER) {
            policyName = "never";
        }
        String pos = "";
        if (this.getComponentCount() > 0) {
            Point p = this.getScrollPosition();
            pos = ",ScrollPosition=(" + p.x + "," + p.y + ")";
        }
        return super.paramString() + pos + ",Insets=" + this.getInsets()
                + ",ScrollbarDisplayPolicy=" + policyName + ",wheelScrollingEnabled="
                + this.wheelScrollingEnabled;
    }

    /** Scrolls according to the wheel, if it is enabled. */
    void autoProcessMouseWheel(MouseWheelEvent e) {
        this.processMouseWheelEvent(e);
    }

    /**
     * Handles the mouse wheel.
     *
     * <p>It scrolls vertically by units, which is what the JDK does when the wheel asks for {@link
     * MouseWheelEvent#WHEEL_UNIT_SCROLL}. What it does not do is tell the block case apart —the JDK
     * hands the event to an internal scroller that does— nor scroll horizontally.
     */
    protected void processMouseWheelEvent(MouseWheelEvent e) {
        if (this.wheelScrollingEnabled && !e.isConsumed() && this.getComponentCount() > 0) {
            int amount = e.getUnitsToScroll() * this.vAdjustable.getUnitIncrement();
            this.vAdjustable.setValue(this.vAdjustable.getValue() + amount);
            e.consume();
        }
        super.processMouseWheelEvent(e);
    }

    /**
     * Whether that family of events is of interest; the wheel is, whenever it is enabled.
     *
     * <p>For any other family it answers `false` and does not ask its parent, which is what the JDK
     * does: here {@link Component} has no such method, so there is nothing to ask, and nobody calls
     * this one either.
     */
    protected boolean eventTypeEnabled(int type) {
        if (type == MouseEvent.MOUSE_WHEEL) {
            return this.isWheelScrollingEnabled();
        }
        return false;
    }

    /** Turns wheel scrolling on or off. */
    public void setWheelScrollingEnabled(boolean handleWheel) {
        this.wheelScrollingEnabled = handleWheel;
    }

    /** Whether the wheel scrolls it. */
    public boolean isWheelScrollingEnabled() {
        return this.wheelScrollingEnabled;
    }

    /** The accessibility information of this pane. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTScrollPane();
        }
        return this.accessibleContext;
    }

    /** A scroll pane, for accessibility, is a scroll pane. */
    protected class AccessibleAWTScrollPane extends AccessibleAWTContainer {

        /** For the subclasses. */
        protected AccessibleAWTScrollPane() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SCROLL_PANE;
        }
    }
}
