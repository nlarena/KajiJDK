package javax.swing.plaf.basic;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ScrollPaneUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalBorders$ScrollPaneBorder;

/**
 * The basic look and feel of a pane with bars: it keeps the three models in agreement.
 *
 * <h2>A triangle of listeners</h2>
 *
 * <p>Three things may change and all three have to be reflected in the others:
 *
 * <ul>
 * <li>The viewport changed -- it moved, or changed size, or was given other content --: the
 * two bars' value and extent are recomputed ({@link #syncScrollPaneWithViewport}).
 * <li>A bar's model changed -- somebody dragged it --: the viewport's position is moved.
 * <li>A property of the pane changed -- another viewport, another bar, another policy --:
 * whatever applies is hooked up again.
 * </ul>
 *
 * <p>The circuit does not feed back because each step writes a value that is already the right
 * one: at the second notice, nothing changes and the model does not give notice again.
 *
 * <h2>What is not there</h2>
 *
 * <p>The keyboard actions need the look and feel's table in order to know which key does what,
 * and that table is not there yet.
 */
public class BasicScrollPaneUI extends ScrollPaneUI implements ScrollPaneConstants {

    protected JScrollPane scrollpane;

    /** The wheel; it is registered on the pane, not on the view. */
    private MouseWheelListener mouseWheelListener;

    /** The shared listener; see {@link #createMouseWheelListener}. */
    private Handler handler;

    protected ChangeListener vsbChangeListener;

    protected ChangeListener hsbChangeListener;

    protected ChangeListener viewportChangeListener;

    protected PropertyChangeListener spPropertyChangeListener;

    private static final ColorUIResource DEFAULT_BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource DEFAULT_FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final Font DEFAULT_FONT = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicScrollPaneUI() {
    }

    /** One look and feel per pane: it keeps the pane and its four listeners. */
    public static ComponentUI createUI(JComponent x) {
        return new BasicScrollPaneUI();
    }

    /** It paints the viewport's border, if there is one; the rest is painted by the pieces. */
    public void paint(Graphics g, JComponent c) {
        Border vpBorder = scrollpane.getViewportBorder();
        if (vpBorder != null) {
            Rectangle r = scrollpane.getViewportBorderBounds();
            vpBorder.paintBorder(scrollpane, g, r.x, r.y, r.width, r.height);
        }
    }

    /** No cap: a pane with bars stretches as far as it is given. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Colours, typeface and border.
     *
     * <p>They are those of {@code ScrollPane.*} measured in Metal (JDK 25): background
     * (238, 238, 238), foreground (51, 51, 51), Dialog 12 and the border from
     * {@code MetalBorders.ScrollPaneBorder}. The viewport's border is left {@code null}, as in
     * Metal.
     */
    protected void installDefaults(JScrollPane scrollpane) {
        Color background = scrollpane.getBackground();
        if (background == null || background instanceof UIResource) {
            scrollpane.setBackground(DEFAULT_BACKGROUND);
        }
        Color foreground = scrollpane.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            scrollpane.setForeground(DEFAULT_FOREGROUND);
        }
        Font font = scrollpane.getFont();
        if (font == null || font instanceof UIResource) {
            scrollpane.setFont(DEFAULT_FONT);
        }
        Border border = scrollpane.getBorder();
        if (border == null || border instanceof UIResource) {
            scrollpane.setBorder(new MetalBorders$ScrollPaneBorder());
        }
        Border viewportBorder = scrollpane.getViewportBorder();
        if (viewportBorder == null || viewportBorder instanceof UIResource) {
            scrollpane.setViewportBorder(null);
        }
        LookAndFeel.installProperty(scrollpane, "opaque", Boolean.TRUE);
    }

    protected void installListeners(JScrollPane c) {
        vsbChangeListener = createVSBChangeListener();
        hsbChangeListener = createHSBChangeListener();
        viewportChangeListener = createViewportChangeListener();
        spPropertyChangeListener = createPropertyChangeListener();
        mouseWheelListener = createMouseWheelListener();
        scrollpane.addMouseWheelListener(mouseWheelListener);

        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();

        if (viewport != null) {
            viewport.addChangeListener(viewportChangeListener);
        }
        if (vsb != null) {
            vsb.getModel().addChangeListener(vsbChangeListener);
        }
        if (hsb != null) {
            hsb.getModel().addChangeListener(hsbChangeListener);
        }
        scrollpane.addPropertyChangeListener(spPropertyChangeListener);
    }

    /** Nothing: with no {@code InputMap} there is nowhere to register keys. */
    protected void installKeyboardActions(JScrollPane c) {
    }

    public void installUI(JComponent x) {
        super.installUI(x);
        scrollpane = (JScrollPane) x;
        installDefaults(scrollpane);
        installListeners(scrollpane);
        installKeyboardActions(scrollpane);
    }

    /** What was installed stays in the component; the JDK does not erase it either. */
    protected void uninstallDefaults(JScrollPane c) {
        LookAndFeel.uninstallBorder(scrollpane);
        if (scrollpane.getViewportBorder() instanceof UIResource) {
            scrollpane.setViewportBorder(null);
        }
    }

    protected void uninstallListeners(JComponent c) {
        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();

        if (viewport != null) {
            viewport.removeChangeListener(viewportChangeListener);
        }
        if (vsb != null) {
            vsb.getModel().removeChangeListener(vsbChangeListener);
        }
        if (hsb != null) {
            hsb.getModel().removeChangeListener(hsbChangeListener);
        }
        scrollpane.removePropertyChangeListener(spPropertyChangeListener);

        vsbChangeListener = null;
        hsbChangeListener = null;
        viewportChangeListener = null;
        spPropertyChangeListener = null;
    }

    protected void uninstallKeyboardActions(JScrollPane c) {
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
        uninstallDefaults(scrollpane);
        uninstallListeners(c);
        uninstallKeyboardActions(scrollpane);
        scrollpane = null;
    }

    /**
     * It copies to the bars what the viewport shows.
     *
     * <p>The value is the position, the extent is what is seen, the maximum is the content's size
     * and the minimum is zero. The headers go along on their single axis: the row one follows the
     * vertical scrolling and the column one the horizontal, and that is why they do not leave the
     * screen.
     */
    protected void syncScrollPaneWithViewport() {
        JViewport viewport = scrollpane.getViewport();
        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        JScrollBar hsb = scrollpane.getHorizontalScrollBar();
        JViewport rowHead = scrollpane.getRowHeader();
        JViewport colHead = scrollpane.getColumnHeader();
        boolean ltr = scrollpane.getComponentOrientation().isLeftToRight();

        if (viewport != null) {
            Dimension extentSize = viewport.getExtentSize();
            Dimension viewSize = viewport.getViewSize();
            Point viewPosition = viewport.getViewPosition();

            if (vsb != null) {
                int extent = extentSize.height;
                int max = viewSize.height;
                int value = Math.max(0, Math.min(viewPosition.y, max - extent));
                vsb.setValues(value, extent, 0, max);
            }

            if (hsb != null) {
                int extent = extentSize.width;
                int max = viewSize.width;
                int value = Math.max(0, Math.min(viewPosition.x, max - extent));
                if (!ltr) {
                    value = Math.max(0, Math.min(max - extent, max - extent - viewPosition.x));
                }
                hsb.setValues(value, extent, 0, max);
            }

            if (rowHead != null) {
                Point p = rowHead.getViewPosition();
                p.y = viewport.getViewPosition().y;
                p.x = 0;
                rowHead.setViewPosition(p);
            }

            if (colHead != null) {
                Point p = colHead.getViewPosition();
                if (ltr) {
                    p.x = viewport.getViewPosition().x;
                } else {
                    p.x = Math.max(0, viewport.getViewPosition().x);
                }
                p.y = 0;
                colHead.setViewPosition(p);
            }
        }
    }

    /**
     * The pane's baseline: its column header's, and nothing else.
     *
     * <p>With no header it returns {@code -1}, even though the content has one. It makes sense:
     * the content scrolls, so its baseline is not at a fixed place in the pane, and lining up
     * against it would stop holding as soon as somebody moved the bar. The header, on the other
     * hand, does not move vertically.
     *
     * <p>Measured in JDK 25: with a border it gives 14 and with no border 13, for a header whose
     * own line is at 13; that is, the header's plus the pane's top inset.
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        JViewport columnHeader = scrollpane.getColumnHeader();
        if (columnHeader != null && columnHeader.isVisible()) {
            Component headerView = columnHeader.getView();
            if (headerView instanceof JComponent) {
                java.awt.Insets insets = scrollpane.getInsets();
                Dimension headerPref = columnHeader.getPreferredSize();
                int baseline = ((JComponent) headerView).getBaseline(headerPref.width,
                        headerPref.height);
                if (baseline >= 0) {
                    return insets.top + baseline;
                }
            }
        }
        return -1;
    }

    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component$BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    protected ChangeListener createViewportChangeListener() {
        return new ViewportChangeHandler();
    }

    /**
     * The wheel's listener.
     *
     * <p>It always returns the same one, and not a new {@link MouseWheelHandler}: it is what the
     * JDK does since it gathered all its internal listeners into a single class.
     * {@link MouseWheelHandler} is still there -- it is public and somebody may have extended it
     * -- but it is no longer what comes out of here; the only thing it does is delegate to this
     * one. Measured.
     */
    protected MouseWheelListener createMouseWheelListener() {
        return theListener();
    }

    /** The shared listener, created the first time it is needed. */
    private Handler theListener() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    protected ChangeListener createHSBChangeListener() {
        return new HSBChangeListener();
    }

    protected ChangeListener createVSBChangeListener() {
        return new VSBChangeListener();
    }

    /** A bar's policy changed: it has to be laid out again. */
    protected void updateScrollBarDisplayPolicy(PropertyChangeEvent e) {
        scrollpane.revalidate();
        scrollpane.repaint();
    }

    /** The viewport changed: the listener is hooked up again and everything is resynchronized. */
    protected void updateViewport(PropertyChangeEvent e) {
        JViewport oldViewport = (JViewport) (e.getOldValue());
        JViewport newViewport = (JViewport) (e.getNewValue());

        if (oldViewport != null) {
            oldViewport.removeChangeListener(viewportChangeListener);
        }
        if (newViewport != null) {
            Point p = newViewport.getViewPosition();
            if (scrollpane.getComponentOrientation().isLeftToRight()) {
                p.x = Math.max(p.x, 0);
            } else {
                p.x = Math.min(p.x, newViewport.getViewSize().width
                        - newViewport.getExtentSize().width);
            }
            p.y = Math.max(p.y, 0);
            newViewport.setViewPosition(p);
            newViewport.addChangeListener(viewportChangeListener);
        }
        syncScrollPaneWithViewport();
    }

    protected void updateRowHeader(PropertyChangeEvent e) {
        JViewport newRowHead = (JViewport) (e.getNewValue());
        if (newRowHead != null) {
            JViewport viewport = scrollpane.getViewport();
            Point p = newRowHead.getViewPosition();
            p.y = (viewport != null) ? viewport.getViewPosition().y : 0;
            p.x = 0;
            newRowHead.setViewPosition(p);
        }
    }

    protected void updateColumnHeader(PropertyChangeEvent e) {
        JViewport newColHead = (JViewport) (e.getNewValue());
        if (newColHead != null) {
            JViewport viewport = scrollpane.getViewport();
            Point p = newColHead.getViewPosition();
            if (viewport == null) {
                p.x = 0;
            } else {
                p.x = viewport.getViewPosition().x;
            }
            p.y = 0;
            newColHead.setViewPosition(p);
            scrollpane.add(newColHead, COLUMN_HEADER);
        }
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    /**
     * The mouse wheel.
     *
     * <p>It moves the vertical bar; if there is none or it is not seen, the horizontal one. That
     * the wheel scrolls horizontally when there is nothing to move vertically is what makes a wide
     * and low pane usable, where the only bar is the bottom one.
     *
     * <p>The event is consumed as soon as it is decided which bar to move, even before moving it:
     * consuming it is what keeps the outermost pane from scrolling with the same turn too.
     */
    protected class MouseWheelHandler implements MouseWheelListener {

        protected MouseWheelHandler() {
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            theListener().mouseWheelMoved(e);
        }
    }

    /** Where the wheel's logic really lives; see {@link MouseWheelHandler}. */
    private class Handler implements MouseWheelListener {

        public void mouseWheelMoved(MouseWheelEvent e) {
            if (!scrollpane.isWheelScrollingEnabled() || e.getWheelRotation() == 0) {
                return;
            }
            JScrollBar bar = scrollpane.getVerticalScrollBar();
            if (bar == null || !bar.isVisible()) {
                bar = scrollpane.getHorizontalScrollBar();
                if (bar == null || !bar.isVisible()) {
                    return;
                }
            }
            e.consume();
            if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                byBlocks(bar, e.getWheelRotation() < 0 ? -1 : 1);
            } else {
                bySteps(bar, e.getUnitsToScroll());
            }
        }

        /** One screenful in that direction. */
        private void byBlocks(JScrollBar bar, int direction) {
            int salto = bar.getBlockIncrement(direction);
            if (salto == 0) {
                salto = bar.getVisibleAmount();
            }
            bar.setValue(bar.getValue() + salto * direction);
        }

        /** As many steps as the event asks for, adding up each one's step. */
        private void bySteps(JScrollBar bar, int steps) {
            if (steps == 0) {
                return;
            }
            int direction = (steps < 0) ? -1 : 1;
            int total = 0;
            for (int i = Math.abs(steps); i > 0; i--) {
                total += bar.getUnitIncrement(direction) * direction;
            }
            bar.setValue(bar.getValue() + total);
        }
    }

    /** The viewport changed: what is now seen has to be copied to the bars. */
    protected class ViewportChangeHandler implements ChangeListener {

        public ViewportChangeHandler() {
        }

        public void stateChanged(ChangeEvent e) {
            syncScrollPaneWithViewport();
        }
    }

    /** The horizontal bar's model changed: the viewport is moved. */
    protected class HSBChangeListener implements ChangeListener {

        public HSBChangeListener() {
        }

        public void stateChanged(ChangeEvent e) {
            JViewport viewport = scrollpane.getViewport();
            if (viewport != null) {
                JScrollBar scrollbar = scrollpane.getHorizontalScrollBar();
                if (scrollbar == null) {
                    return;
                }
                int value = scrollbar.getValue();
                Point p = viewport.getViewPosition();
                if (scrollpane.getComponentOrientation().isLeftToRight()) {
                    p.x = value;
                } else {
                    int max = viewport.getViewSize().width;
                    int extent = viewport.getExtentSize().width;
                    p.x = max - extent - value;
                }
                viewport.setViewPosition(p);
            }
        }
    }

    /** The vertical bar's model changed: the viewport is moved. */
    protected class VSBChangeListener implements ChangeListener {

        public VSBChangeListener() {
        }

        public void stateChanged(ChangeEvent e) {
            JViewport viewport = scrollpane.getViewport();
            if (viewport != null) {
                JScrollBar scrollbar = scrollpane.getVerticalScrollBar();
                if (scrollbar == null) {
                    return;
                }
                Point p = viewport.getViewPosition();
                p.y = scrollbar.getValue();
                viewport.setViewPosition(p);
            }
        }
    }

    /** A piece or a policy of the pane changed; it hooks up again whatever is needed. */
    public class PropertyChangeHandler implements PropertyChangeListener {

        public PropertyChangeHandler() {
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();

            if (propertyName.equals("verticalScrollBarDisplayPolicy")
                    || propertyName.equals("verticalScrollBarPolicy")
                    || propertyName.equals("horizontalScrollBarDisplayPolicy")
                    || propertyName.equals("horizontalScrollBarPolicy")) {
                updateScrollBarDisplayPolicy(e);
            } else if (propertyName.equals("viewport")) {
                updateViewport(e);
            } else if (propertyName.equals("rowHeader")) {
                updateRowHeader(e);
            } else if (propertyName.equals("columnHeader")) {
                updateColumnHeader(e);
            } else if (propertyName.equals("verticalScrollBar")) {
                updateBar(e, vsbChangeListener);
            } else if (propertyName.equals("horizontalScrollBar")) {
                updateBar(e, hsbChangeListener);
            } else if (propertyName.equals("componentOrientation")) {
                syncScrollPaneWithViewport();
            }
        }
    }

    /** A bar changed: the listener moves from the old model to the new one. */
    private void updateBar(PropertyChangeEvent e, ChangeListener listener) {
        JScrollBar old = (JScrollBar) e.getOldValue();
        JScrollBar created = (JScrollBar) e.getNewValue();
        if (old != null) {
            old.getModel().removeChangeListener(listener);
        }
        if (created != null) {
            created.getModel().addChangeListener(listener);
        }
        syncScrollPaneWithViewport();
    }
}
