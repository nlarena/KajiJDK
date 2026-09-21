package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import javax.swing.border.Border;
import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

/**
 * A pane that shows a piece of something large, with bars for moving about.
 *
 * <h2>It scrolls nothing</h2>
 *
 * <p>The pane does not move the content: the {@link JViewport} moves it, and the bars only
 * write into its position. The pane is the one that gathers the pieces -- viewport, two bars,
 * two headers, four corners --, gives them a layout and keeps the bars' models in step with
 * what the viewport shows. That keeping in step is done by the look and feel, not by this
 * class.
 *
 * <p>Hence almost every method is a {@code get}/{@code set} pair of pieces: adding a component
 * to the pane is adding it to its viewport ({@link #setViewportView}), and the rest settles
 * itself.
 *
 * <h2>The content may have an opinion</h2>
 *
 * <p>If the content implements {@link Scrollable}, the bars ask it how much to advance and the
 * layout asks it whether it wants to follow the viewport's size. The bar that asks those
 * questions is {@link ScrollBar}, the one the pane creates; a bar set by hand with
 * {@link #setVerticalScrollBar} does not ask them.
 *
 * <p>Wheel scrolling is not there: {@link #setWheelScrollingEnabled} keeps the property, but
 * with no wheel events to dispatch there is nothing to answer.
 */
public class JScrollPane extends JComponent implements ScrollPaneConstants, Accessible {

    private static final String uiClassID = "ScrollPaneUI";

    private Border viewportBorder;

    protected int verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;

    protected int horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;

    protected JViewport viewport;

    protected JScrollBar verticalScrollBar;

    protected JScrollBar horizontalScrollBar;

    protected JViewport rowHeader;

    protected JViewport columnHeader;

    protected Component lowerLeft;

    protected Component lowerRight;

    protected Component upperLeft;

    protected Component upperRight;

    private boolean wheelScrollState = true;

    /** A pane with that content and those two policies. */
    public JScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        setLayout(new ScrollPaneLayout$UIResource());
        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);
        setViewport(createViewport());
        setVerticalScrollBar(createVerticalScrollBar());
        setHorizontalScrollBar(createHorizontalScrollBar());
        if (view != null) {
            setViewportView(view);
        }
        setUIProperty("opaque", Boolean.TRUE);
        updateUI();

        if (!this.getComponentOrientation().isLeftToRight()) {
            viewport.setViewPosition(new Point(Integer.MAX_VALUE, 0));
        }
    }

    /** A pane with that content and the "as needed" policies. */
    public JScrollPane(Component view) {
        this(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public JScrollPane(int vsbPolicy, int hsbPolicy) {
        this(null, vsbPolicy, hsbPolicy);
    }

    /** An empty pane. */
    public JScrollPane() {
        this(null, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public ScrollPaneUI getUI() {
        return (ScrollPaneUI) ui;
    }

    public void setUI(ScrollPaneUI ui) {
        super.setUI(ui);
    }

    /** It installs the basic look and feel; see {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ScrollPaneUI) BasicScrollPaneUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** The layout has to be a {@link ScrollPaneLayout}, or {@code null}. */
    public void setLayout(LayoutManager layout) {
        if (layout instanceof ScrollPaneLayout) {
            super.setLayout(layout);
            ((ScrollPaneLayout) layout).syncWithScrollPane(this);
        } else if (layout == null) {
            super.setLayout(layout);
        } else {
            String s = "layout of JScrollPane must be a ScrollPaneLayout";
            throw new ClassCastException(s);
        }
    }

    /**
     * Yes: the layout comes here.
     *
     * <p>A pane with bars has a size of its own, and whatever happens inside has no reason to
     * force the whole window to be recomputed.
     */
    public boolean isValidateRoot() {
        return true;
    }

    public int getVerticalScrollBarPolicy() {
        return verticalScrollBarPolicy;
    }

    public void setVerticalScrollBarPolicy(int policy) {
        if (policy != VERTICAL_SCROLLBAR_AS_NEEDED && policy != VERTICAL_SCROLLBAR_NEVER
                && policy != VERTICAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException("invalid verticalScrollBarPolicy");
        }
        int old = verticalScrollBarPolicy;
        verticalScrollBarPolicy = policy;
        firePropertyChange("verticalScrollBarPolicy", old, policy);
        revalidate();
        repaint();
    }

    public int getHorizontalScrollBarPolicy() {
        return horizontalScrollBarPolicy;
    }

    public void setHorizontalScrollBarPolicy(int policy) {
        if (policy != HORIZONTAL_SCROLLBAR_AS_NEEDED && policy != HORIZONTAL_SCROLLBAR_NEVER
                && policy != HORIZONTAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException("invalid horizontalScrollBarPolicy");
        }
        int old = horizontalScrollBarPolicy;
        horizontalScrollBarPolicy = policy;
        firePropertyChange("horizontalScrollBarPolicy", old, policy);
        revalidate();
        repaint();
    }

    /** The border that is painted around the viewport, inside the pane's border. */
    public Border getViewportBorder() {
        return viewportBorder;
    }

    public void setViewportBorder(Border viewportBorder) {
        Border oldValue = this.viewportBorder;
        this.viewportBorder = viewportBorder;
        firePropertyChange("viewportBorder", oldValue, viewportBorder);
    }

    /** The rectangle that border goes in: what is left with neither headers nor bars. */
    public Rectangle getViewportBorderBounds() {
        Rectangle borderR = new Rectangle(getSize());

        java.awt.Insets insets = getInsets();
        borderR.x = insets.left;
        borderR.y = insets.top;
        borderR.width = borderR.width - (insets.left + insets.right);
        borderR.height = borderR.height - (insets.top + insets.bottom);

        boolean leftToRight = getComponentOrientation().isLeftToRight();

        JViewport colHead = getColumnHeader();
        if ((colHead != null) && (colHead.isVisible())) {
            int colHeadHeight = colHead.getHeight();
            borderR.y = borderR.y + colHeadHeight;
            borderR.height = borderR.height - colHeadHeight;
        }

        JViewport rowHead = getRowHeader();
        if ((rowHead != null) && (rowHead.isVisible())) {
            int rowHeadWidth = rowHead.getWidth();
            if (leftToRight) {
                borderR.x = borderR.x + rowHeadWidth;
            }
            borderR.width = borderR.width - rowHeadWidth;
        }

        JScrollBar vsb = getVerticalScrollBar();
        if ((vsb != null) && (vsb.isVisible())) {
            int vsbWidth = vsb.getWidth();
            if (!leftToRight) {
                borderR.x = borderR.x + vsbWidth;
            }
            borderR.width = borderR.width - vsbWidth;
        }

        JScrollBar hsb = getHorizontalScrollBar();
        if ((hsb != null) && (hsb.isVisible())) {
            int hsbHeight = hsb.getHeight();
            borderR.height = borderR.height - hsbHeight;
        }

        return borderR;
    }

    /** The bar that knows how to ask the content; see {@link ScrollBar}. */
    public JScrollBar createHorizontalScrollBar() {
        return new ScrollBar(JScrollBar.HORIZONTAL);
    }

    public JScrollBar getHorizontalScrollBar() {
        return horizontalScrollBar;
    }

    public void setHorizontalScrollBar(JScrollBar horizontalScrollBar) {
        JScrollBar old = getHorizontalScrollBar();
        this.horizontalScrollBar = horizontalScrollBar;
        if (horizontalScrollBar != null) {
            add(horizontalScrollBar, HORIZONTAL_SCROLLBAR);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("horizontalScrollBar", old, horizontalScrollBar);
        revalidate();
        repaint();
    }

    public JScrollBar createVerticalScrollBar() {
        return new ScrollBar(JScrollBar.VERTICAL);
    }

    public JScrollBar getVerticalScrollBar() {
        return verticalScrollBar;
    }

    public void setVerticalScrollBar(JScrollBar verticalScrollBar) {
        JScrollBar old = getVerticalScrollBar();
        this.verticalScrollBar = verticalScrollBar;
        if (verticalScrollBar != null) {
            add(verticalScrollBar, VERTICAL_SCROLLBAR);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("verticalScrollBar", old, verticalScrollBar);
        revalidate();
        repaint();
    }

    protected JViewport createViewport() {
        return new JViewport();
    }

    public JViewport getViewport() {
        return viewport;
    }

    public void setViewport(JViewport viewport) {
        JViewport old = getViewport();
        this.viewport = viewport;
        if (viewport != null) {
            add(viewport, VIEWPORT);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("viewport", old, viewport);

        if (accessibleContextExists()) {
            // The JDK hooks the accessibility listener up again here; with no context there is
            // nothing.
        }
        revalidate();
        repaint();
    }

    private boolean accessibleContextExists() {
        return false;
    }

    /** Setting content is setting it on the viewport; if there is no viewport, one is created. */
    public void setViewportView(Component view) {
        if (getViewport() == null) {
            setViewport(createViewport());
        }
        getViewport().setView(view);
    }

    public JViewport getRowHeader() {
        return rowHeader;
    }

    public void setRowHeader(JViewport rowHeader) {
        JViewport old = getRowHeader();
        this.rowHeader = rowHeader;
        if (rowHeader != null) {
            add(rowHeader, ROW_HEADER);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("rowHeader", old, rowHeader);
        revalidate();
        repaint();
    }

    /** It wraps that component in a viewport and sets it as the row header. */
    public void setRowHeaderView(Component view) {
        if (getRowHeader() == null) {
            setRowHeader(createViewport());
        }
        getRowHeader().setView(view);
    }

    public JViewport getColumnHeader() {
        return columnHeader;
    }

    public void setColumnHeader(JViewport columnHeader) {
        JViewport old = getColumnHeader();
        this.columnHeader = columnHeader;
        if (columnHeader != null) {
            add(columnHeader, COLUMN_HEADER);
        } else if (old != null) {
            remove(old);
        }
        firePropertyChange("columnHeader", old, columnHeader);
        revalidate();
        repaint();
    }

    public void setColumnHeaderView(Component view) {
        if (getColumnHeader() == null) {
            setColumnHeader(createViewport());
        }
        getColumnHeader().setView(view);
    }

    /**
     * That corner's piece.
     *
     * <p>The "leading" and "trailing" corners are resolved according to the orientation: in a
     * language that is read right to left, the leading top one is the top right one.
     */
    public Component getCorner(String key) {
        boolean isLeftToRight = getComponentOrientation().isLeftToRight();
        if (key.equals(LOWER_LEADING_CORNER)) {
            key = isLeftToRight ? LOWER_LEFT_CORNER : LOWER_RIGHT_CORNER;
        } else if (key.equals(LOWER_TRAILING_CORNER)) {
            key = isLeftToRight ? LOWER_RIGHT_CORNER : LOWER_LEFT_CORNER;
        } else if (key.equals(UPPER_LEADING_CORNER)) {
            key = isLeftToRight ? UPPER_LEFT_CORNER : UPPER_RIGHT_CORNER;
        } else if (key.equals(UPPER_TRAILING_CORNER)) {
            key = isLeftToRight ? UPPER_RIGHT_CORNER : UPPER_LEFT_CORNER;
        }

        if (key.equals(LOWER_LEFT_CORNER)) {
            return lowerLeft;
        } else if (key.equals(LOWER_RIGHT_CORNER)) {
            return lowerRight;
        } else if (key.equals(UPPER_LEFT_CORNER)) {
            return upperLeft;
        } else if (key.equals(UPPER_RIGHT_CORNER)) {
            return upperRight;
        }
        return null;
    }

    /** It puts a piece in a corner; a corner is only seen if the two bars around it are there. */
    public void setCorner(String key, Component corner) {
        Component old;
        boolean isLeftToRight = getComponentOrientation().isLeftToRight();
        if (key.equals(LOWER_LEADING_CORNER)) {
            key = isLeftToRight ? LOWER_LEFT_CORNER : LOWER_RIGHT_CORNER;
        } else if (key.equals(LOWER_TRAILING_CORNER)) {
            key = isLeftToRight ? LOWER_RIGHT_CORNER : LOWER_LEFT_CORNER;
        } else if (key.equals(UPPER_LEADING_CORNER)) {
            key = isLeftToRight ? UPPER_LEFT_CORNER : UPPER_RIGHT_CORNER;
        } else if (key.equals(UPPER_TRAILING_CORNER)) {
            key = isLeftToRight ? UPPER_RIGHT_CORNER : UPPER_LEFT_CORNER;
        }

        if (key.equals(LOWER_LEFT_CORNER)) {
            old = lowerLeft;
            lowerLeft = corner;
        } else if (key.equals(LOWER_RIGHT_CORNER)) {
            old = lowerRight;
            lowerRight = corner;
        } else if (key.equals(UPPER_LEFT_CORNER)) {
            old = upperLeft;
            upperLeft = corner;
        } else if (key.equals(UPPER_RIGHT_CORNER)) {
            old = upperRight;
            upperRight = corner;
        } else {
            throw new IllegalArgumentException("invalid corner key");
        }
        if (old != null) {
            remove(old);
        }
        if (corner != null) {
            add(corner, key);
        }
        firePropertyChange(key, old, corner);
        revalidate();
        repaint();
    }

    /** It passes the orientation on to the viewport and to the two bars. */
    public void setComponentOrientation(ComponentOrientation co) {
        super.setComponentOrientation(co);
        if (viewport != null) {
            viewport.setComponentOrientation(co);
        }
        if (verticalScrollBar != null) {
            verticalScrollBar.setComponentOrientation(co);
        }
        if (horizontalScrollBar != null) {
            horizontalScrollBar.setComponentOrientation(co);
        }
    }

    /** Whether the wheel scrolls; see the class note. */
    public boolean isWheelScrollingEnabled() {
        return wheelScrollState;
    }

    public void setWheelScrollingEnabled(boolean handleWheel) {
        boolean old = wheelScrollState;
        wheelScrollState = handleWheel;
        firePropertyChange("wheelScrollingEnabled", old, handleWheel);
    }

    protected String paramString() {
        String viewportBorderString = (viewportBorder != null ? viewportBorder.toString() : "");
        String viewportString = (viewport != null ? viewport.toString() : "");
        String verticalScrollBarPolicyString;
        if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_AS_NEEDED";
        } else if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_NEVER) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_NEVER";
        } else if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
            verticalScrollBarPolicyString = "VERTICAL_SCROLLBAR_ALWAYS";
        } else {
            verticalScrollBarPolicyString = "";
        }
        String horizontalScrollBarPolicyString;
        if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_AS_NEEDED";
        } else if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_NEVER";
        } else if (horizontalScrollBarPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
            horizontalScrollBarPolicyString = "HORIZONTAL_SCROLLBAR_ALWAYS";
        } else {
            horizontalScrollBarPolicyString = "";
        }

        return super.paramString() + ",columnHeader=" + (columnHeader != null ? "" : "")
                + ",horizontalScrollBar=" + (horizontalScrollBar != null ? "" : "")
                + ",horizontalScrollBarPolicy=" + horizontalScrollBarPolicyString
                + ",rowHeader=" + (rowHeader != null ? "" : "")
                + ",verticalScrollBar=" + (verticalScrollBar != null ? "" : "")
                + ",verticalScrollBarPolicy=" + verticalScrollBarPolicyString
                + ",viewport=" + viewportString + ",viewportBorder=" + viewportBorderString;
    }

    /** With no accessibility context: there is no assistive technology that reads it on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * The bar the pane creates: the one that asks the content how much to advance.
     *
     * <p>If the content is {@link Scrollable}, both steps come from it; if not, the small one is
     * one and the big one is a screenful. Fixing a step by hand cuts the question off: from there
     * on the fixed number rules, which is what whoever fixed it expects.
     */
    // Unqualified, `UIResource` resolves to `ScrollPaneLayout.UIResource`, of the same package,
    // and not to the imported interface (#493). It goes qualified.
    protected class ScrollBar extends JScrollBar implements javax.swing.plaf.UIResource {

        private boolean unitIncrementSet;
        private boolean blockIncrementSet;

        public ScrollBar(int orientation) {
            super(orientation);
        }

        public void setUnitIncrement(int unitIncrement) {
            unitIncrementSet = true;
            super.setUnitIncrement(unitIncrement);
        }

        public int getUnitIncrement(int direction) {
            JViewport vp = getViewport();
            if (!unitIncrementSet && (vp != null) && (vp.getView() instanceof Scrollable)) {
                Scrollable view = (Scrollable) (vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableUnitIncrement(vr, getOrientation(), direction);
            }
            return super.getUnitIncrement(direction);
        }

        public void setBlockIncrement(int blockIncrement) {
            blockIncrementSet = true;
            super.setBlockIncrement(blockIncrement);
        }

        public int getBlockIncrement(int direction) {
            JViewport vp = getViewport();
            if (blockIncrementSet || vp == null) {
                return super.getBlockIncrement(direction);
            } else if (vp.getView() instanceof Scrollable) {
                Scrollable view = (Scrollable) (vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableBlockIncrement(vr, getOrientation(), direction);
            } else if (getOrientation() == VERTICAL) {
                return vp.getExtentSize().height;
            } else {
                return vp.getExtentSize().width;
            }
        }
    }
}
