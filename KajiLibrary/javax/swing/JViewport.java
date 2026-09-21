package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.Serializable;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ViewportUI;
import javax.swing.plaf.basic.BasicViewportUI;

/**
 * A viewport that shows a piece of something larger.
 *
 * <h2>A hole and something behind it</h2>
 *
 * <p>The viewport has a single child, the <em>view</em>, and the view is usually larger than
 * it. Scrolling does not move the viewport: it moves the view, the other way. That is why
 * {@link #getViewPosition} returns the view's position <em>with the sign turned round</em>: the
 * position is "which point of the view ends up at the top left of the hole", and for that
 * point to go down, the view has to go up.
 *
 * <p>The clipping is not done by this class: it is done by the painting chain, which gives each
 * child a graphics context clipped to its rectangle. A view at (-15, -20) of 80 by 90 inside a
 * viewport of 41 by 31 is drawn whole and the piece that falls inside is seen.
 *
 * <h2>With no border, and with no screen tricks</h2>
 *
 * <p>{@link #setBorder} throws an exception: a border would shift the content and the
 * arithmetic of what is seen would stop adding up. It is one of the few times Swing prefers to
 * forbid rather than to settle.
 *
 * <p>The three scrolling modes exist as constants and as a property, but the only one that can
 * be kept here is {@link #SIMPLE_SCROLL_MODE}: {@link #BLIT_SCROLL_MODE} copies pixels already
 * drawn <em>on the screen</em> and {@link #BACKINGSTORE_SCROLL_MODE} keeps a backing image of
 * the component on the screen. With no screen, {@link #paint} always redraws, which is exactly
 * what the JDK does when the blit cannot be used. The field {@link #backingStoreImage} is
 * always left at {@code null} for the same reason.
 */
public class JViewport extends JComponent implements Accessible {

    private static final String uiClassID = "ViewportUI";

    /** Whether somebody fixed the view's size by hand; see {@link #setViewSize}. */
    protected boolean isViewSizeSet = false;

    /** Where the view was the last time it was painted. */
    protected Point lastPaintPosition = null;

    /** @deprecated the backing store mode needs a screen; see the class note. */
    @Deprecated
    protected boolean backingStore = false;

    /** Always {@code null}; see the class note. */
    protected transient Image backingStoreImage = null;

    /** Whether the last change of position came from a scroll and has not been laid out yet. */
    protected boolean scrollUnderway = false;

    /** It copies what is already drawn and redraws only the new strip; it needs a screen. */
    public static final int BLIT_SCROLL_MODE = 1;

    /** It keeps a backing image of the component; it needs a screen. */
    public static final int BACKINGSTORE_SCROLL_MODE = 2;

    /** It redraws everything; it is what this VM always does. */
    public static final int SIMPLE_SCROLL_MODE = 3;

    private int scrollMode = BLIT_SCROLL_MODE;

    private ComponentListener viewListener = null;
    private transient ChangeEvent changeEvent = null;
    private boolean hasHadValidView = false;

    /** An empty viewport, opaque and with {@link ViewportLayout}'s layout. */
    public JViewport() {
        super();
        setLayout(createLayoutManager());
        setOpaque(true);
        updateUI();
    }

    public ViewportUI getUI() {
        return (ViewportUI) ui;
    }

    public void setUI(ViewportUI ui) {
        super.setUI(ui);
    }

    /** It installs the basic look and feel; see {@code JButton#updateUI}. */
    public void updateUI() {
        setUI((ViewportUI) BasicViewportUI.createUI(this));
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Adding a component to a viewport is setting it as the view; see {@link #setView}. */
    protected void addImpl(Component child, Object constraints, int index) {
        setView(child);
    }

    /** It removes the view, and with it the listener that gave notice of its changes of size. */
    public void remove(Component child) {
        child.removeComponentListener(viewListener);
        super.remove(child);
    }

    /**
     * It scrolls just enough for that rectangle of the view to be in sight.
     *
     * <p>"Just enough" is literal: if it is already seen whole it moves nothing, and if it does
     * not fit it aligns it by the nearest edge instead of centring it. It is what keeps going to
     * an element from jumping suddenly when it was already almost visible.
     */
    public void scrollRectToVisible(Rectangle contentRect) {
        Component view = getView();
        if (view == null) {
            return;
        }
        if (!view.isValid()) {
            validateView();
        }
        int dx = positionAdjustment(getWidth(), contentRect.width, contentRect.x);
        int dy = positionAdjustment(getHeight(), contentRect.height, contentRect.y);

        if (dx != 0 || dy != 0) {
            Point viewPosition = getViewPosition();
            Dimension viewSize = view.getSize();
            int startX = viewPosition.x;
            int startY = viewPosition.y;
            Dimension extent = getExtentSize();

            viewPosition.x = viewPosition.x - dx;
            viewPosition.y = viewPosition.y - dy;

            // The clipping against the edges only runs if the view is laid out: a view that is not
                        // laid out usually measures zero by zero, and clipping against that zero
                        // would send everything to the origin. On this VM nothing gets as far as
                        // being laid out -- `validate` needs a window --, so this clipping never
                        // runs and the position may end up out of range. It is the same as the JDK
                        // does with no window.
            if (view.isValid()) {
                if (getParent() == null || getParent().getComponentOrientation().isLeftToRight()) {
                    if (viewPosition.x + extent.width > viewSize.width) {
                        viewPosition.x = Math.max(0, viewSize.width - extent.width);
                    } else if (viewPosition.x < 0) {
                        viewPosition.x = 0;
                    }
                } else {
                    if (extent.width > viewSize.width) {
                        viewPosition.x = viewSize.width - extent.width;
                    } else {
                        viewPosition.x = Math.max(0,
                                Math.min(viewSize.width - extent.width, viewPosition.x));
                    }
                }
                if (viewPosition.y + extent.height > viewSize.height) {
                    viewPosition.y = Math.max(0, viewSize.height - extent.height);
                } else if (viewPosition.y < 0) {
                    viewPosition.y = 0;
                }
            }
            if (viewPosition.x != startX || viewPosition.y != startY) {
                setViewPosition(viewPosition);
                scrollUnderway = false;
            }
        }
    }

    /**
     * It lays the subtree out before computing, so as not to measure over a stale size.
     *
     * <p>With no window it lays nothing out -- {@code validate} says so -- and that is why
     * {@link #scrollRectToVisible}'s clipping does not get to run; see the note there.
     */
    private void validateView() {
        Container root = getParent();
        while (root != null && !root.isValidateRoot()) {
            root = root.getParent();
        }
        if (root != null) {
            root.validate();
        }
    }

    /**
     * How much it has to shift for a child of that width, at that position, to fit into that
     * parent.
     *
     * <p>Six cases, and in all of them the nearest edge wins: if it already fits, zero; if it
     * sticks out on one side, just enough to stick it to that side; if it is larger than the
     * parent, the edge it was asked for is aligned.
     */
    private int positionAdjustment(int parentWidth, int childWidth, int childAt) {
        if (childAt >= 0 && childWidth + childAt <= parentWidth) {
            return 0;
        }
        if (childAt <= 0 && childWidth + childAt >= parentWidth) {
            return 0;
        }
        if (childAt > 0 && childWidth <= parentWidth) {
            return parentWidth - (childWidth + childAt);
        }
        if (childAt >= 0 && childWidth >= parentWidth) {
            return -childAt;
        }
        if (childAt <= 0 && childWidth <= parentWidth) {
            return -childAt;
        }
        if (childAt <= 0 && childWidth >= parentWidth) {
            return parentWidth - (childWidth + childAt);
        }
        return 0;
    }

    /** An {@link IllegalArgumentException}: a viewport carries no border; see the class note. */
    public final void setBorder(Border border) {
        if (border != null) {
            throw new IllegalArgumentException("JViewport.setBorder() not supported");
        }
    }

    /** Zero on the four sides, always. */
    public final Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    /**
     * Zero on the four sides; it writes into the one it is passed and returns it, reserving
     * nothing.
     */
    public final Insets getInsets(Insets insets) {
        insets.left = 0;
        insets.top = 0;
        insets.right = 0;
        insets.bottom = 0;
        return insets;
    }

    /** No: the view may be transparent and let the viewport's background be seen. */
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    /** Yes, when a scroll is under way: the repainting has to start here. */
    protected boolean isPaintingOrigin() {
        return scrollMode == BACKINGSTORE_SCROLL_MODE;
    }

    /** Where the view is, in the viewport's coordinates and without turning the sign round. */
    private Point getViewLocation() {
        Component view = getView();
        if (view != null) {
            return view.getLocation();
        }
        return new Point(0, 0);
    }

    /** It redraws; see the class note about why there is neither blit nor backing store. */
    public void paint(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        if ((width <= 0) || (height <= 0)) {
            return;
        }
        super.paint(g);
        lastPaintPosition = getViewLocation();
    }

    /** Changing size changes what is seen, and that is a change of state. */
    public void reshape(int x, int y, int w, int h) {
        boolean sizeChanged = (getWidth() != w) || (getHeight() != h);
        if (sizeChanged) {
            backingStoreImage = null;
        }
        super.reshape(x, y, w, h);
        if (sizeChanged) {
            fireStateChanged();
        }
    }

    /** The scrolling mode; all three are kept, only one is honoured. */
    public void setScrollMode(int mode) {
        scrollMode = mode;
        backingStore = mode == BACKINGSTORE_SCROLL_MODE;
    }

    public int getScrollMode() {
        return scrollMode;
    }

    /** @deprecated it is {@code getScrollMode() == BACKINGSTORE_SCROLL_MODE}. */
    @Deprecated
    public boolean isBackingStoreEnabled() {
        return scrollMode == BACKINGSTORE_SCROLL_MODE;
    }

    /** @deprecated it is {@link #setScrollMode}. */
    @Deprecated
    public void setBackingStoreEnabled(boolean enabled) {
        if (enabled) {
            setScrollMode(BACKINGSTORE_SCROLL_MODE);
        } else {
            setScrollMode(BLIT_SCROLL_MODE);
        }
    }

    /** The view, or {@code null}. */
    public Component getView() {
        return (getComponentCount() > 0) ? getComponent(0) : null;
    }

    /**
     * It sets the view, removing the previous one.
     *
     * <p>It does not use {@code removeAll}: the JDK does not either, because {@code removeAll}
     * does not go through {@link #remove} and the old view's listener would be left set.
     */
    public void setView(Component view) {
        int n = getComponentCount();
        for (int i = n - 1; i >= 0; i--) {
            remove(getComponent(i));
        }
        isViewSizeSet = false;
        if (view != null) {
            super.addImpl(view, null, -1);
            viewListener = createViewListener();
            view.addComponentListener(viewListener);
        }
        if (hasHadValidView) {
            fireStateChanged();
        } else if (view != null) {
            hasHadValidView = true;
        }
        revalidate();
        repaint();
    }

    /**
     * The view's size: the one it was given, or the one it prefers.
     *
     * <p>The distinction matters: while nobody fixes it, enlarging the viewport may enlarge the
     * view; once fixed, what was fixed rules.
     */
    public Dimension getViewSize() {
        Component view = getView();
        if (view == null) {
            return new Dimension(0, 0);
        } else if (isViewSizeSet) {
            return view.getSize();
        } else {
            return view.getPreferredSize();
        }
    }

    public void setViewSize(Dimension newSize) {
        Component view = getView();
        if (view != null) {
            Dimension oldSize = view.getSize();
            if (!newSize.equals(oldSize)) {
                scrollUnderway = false;
                view.setSize(newSize);
                isViewSizeSet = true;
                fireStateChanged();
            }
        }
    }

    /** Which point of the view ends up at the top left; see the class note. */
    public Point getViewPosition() {
        Component view = getView();
        if (view != null) {
            Point p = view.getLocation();
            p.x = -p.x;
            p.y = -p.y;
            return p;
        }
        return new Point(0, 0);
    }

    /** It takes that point of the view to the viewport's corner, moving the view the other way. */
    public void setViewPosition(Point p) {
        Component view = getView();
        if (view == null) {
            return;
        }
        int oldX;
        int oldY;
        int x = p.x;
        int y = p.y;

        Rectangle r = view.getBounds();
        oldX = r.x;
        oldY = r.y;

        int newX = -x;
        int newY = -y;

        if ((oldX != newX) || (oldY != newY)) {
            scrollUnderway = true;
            view.setLocation(newX, newY);
            fireStateChanged();
        }
    }

    /** What is seen of the view: the position plus the hole's size. */
    public Rectangle getViewRect() {
        return new Rectangle(getViewPosition(), getExtentSize());
    }

    /**
     * Which part can be copied and which part has to be redrawn on scrolling by {@code dx, dy}.
     *
     * <p>It only serves when the movement is in a single direction and smaller than the viewport:
     * if more than a screenful was jumped there is nothing left worth using, and it answers
     * {@code false}. The computation is pure arithmetic and is complete even though the blit that
     * would use it needs a screen.
     */
    protected boolean computeBlit(int dx, int dy, Point blitFrom, Point blitTo, Dimension blitSize,
            Rectangle blitPaint) {
        int dxAbs = Math.abs(dx);
        int dyAbs = Math.abs(dy);
        Dimension extentSize = getExtentSize();

        if ((dx == 0) && (dy != 0) && (dyAbs < extentSize.height)) {
            if (dy < 0) {
                blitFrom.y = -dy;
                blitTo.y = 0;
                blitPaint.y = extentSize.height + dy;
            } else {
                blitFrom.y = 0;
                blitTo.y = dy;
                blitPaint.y = 0;
            }
            blitPaint.x = 0;
            blitFrom.x = 0;
            blitTo.x = 0;
            blitSize.width = extentSize.width;
            blitSize.height = extentSize.height - dyAbs;
            blitPaint.width = extentSize.width;
            blitPaint.height = dyAbs;
            return true;
        } else if ((dy == 0) && (dx != 0) && (dxAbs < extentSize.width)) {
            if (dx < 0) {
                blitFrom.x = -dx;
                blitTo.x = 0;
                blitPaint.x = extentSize.width + dx;
            } else {
                blitFrom.x = 0;
                blitTo.x = dx;
                blitPaint.x = 0;
            }
            blitPaint.y = 0;
            blitFrom.y = 0;
            blitTo.y = 0;
            blitSize.width = extentSize.width - dxAbs;
            blitSize.height = extentSize.height;
            blitPaint.width = dxAbs;
            blitPaint.height = extentSize.height;
            return true;
        } else {
            blitFrom.x = 0;
            blitFrom.y = 0;
            blitTo.x = 0;
            blitTo.y = 0;
            blitSize.width = 0;
            blitSize.height = 0;
            blitPaint.x = 0;
            blitPaint.y = 0;
            blitPaint.width = 0;
            blitPaint.height = 0;
            return false;
        }
    }

    /** The hole's size: the viewport's own. */
    public Dimension getExtentSize() {
        return getSize();
    }

    /**
     * That size in the view's coordinates: the same.
     *
     * <p>The JDK uses it for viewports with a transformation -- a magnifier --, which do not exist
     * here. Returning a copy and not the same object is part of the contract: whoever receives it
     * may modify it.
     */
    public Dimension toViewCoordinates(Dimension size) {
        return new Dimension(size);
    }

    /** That point in the view's coordinates: the same. */
    public Point toViewCoordinates(Point p) {
        return new Point(p);
    }

    /** It changes the hole's size; it is {@code setSize} plus the notice. */
    public void setExtentSize(Dimension newExtent) {
        Dimension oldExtent = getExtentSize();
        if (!newExtent.equals(oldExtent)) {
            setSize(newExtent);
            fireStateChanged();
        }
    }

    /** The listener that gives notice when the view changes size. */
    protected ViewListener createViewListener() {
        return new ViewListener();
    }

    /** A viewport's layout: {@link ViewportLayout}'s shared one. */
    protected LayoutManager createLayoutManager() {
        return ViewportLayout.SHARED_INSTANCE;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * It gives notice that what is seen changed.
     *
     * <p>It is the only signal a {@link JScrollPane} needs: from here come the numbers it
     * synchronizes its two bars with.
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    /** It passes the request on to the parent, shifted to its coordinates. */
    public void repaint(long tm, int x, int y, int w, int h) {
        Component parent = getParent();
        if (parent != null) {
            parent.repaint(tm, x + getX(), y + getY(), w, h);
        } else {
            super.repaint(tm, x, y, w, h);
        }
    }

    protected String paramString() {
        String isViewSizeSetString = (isViewSizeSet ? "true" : "false");
        String lastPaintPositionString = (lastPaintPosition != null
                ? lastPaintPosition.toString() : "");
        String scrollUnderwayString = (scrollUnderway ? "true" : "false");

        return super.paramString() + ",isViewSizeSet=" + isViewSizeSetString
                + ",lastPaintPosition=" + lastPaintPositionString + ",scrollUnderway="
                + scrollUnderwayString;
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** With no accessibility context: there is no assistive technology that reads it on this VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * It listens to the view: if it changes size, what is seen changes.
     *
     * <p>It is a class and not a loose expression because {@link #createViewListener} returns it,
     * and a subclass may want its own.
     */
    protected class ViewListener extends ComponentAdapter implements Serializable {

        public ViewListener() {
        }

        public void componentResized(ComponentEvent e) {
            fireStateChanged();
            revalidate();
        }
    }
}
