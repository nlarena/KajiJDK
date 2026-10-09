package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.ScrollBarUI;

/**
 * The basic look and feel of a scroll bar: two buttons, a track and a thumb.
 *
 * <h2>The look and feel is also the layout</h2>
 *
 * <p>It implements {@link LayoutManager} and is installed as the bar's layout. It makes sense:
 * where each piece goes depends on the model -- the thumb is placed and sized by value and
 * extent --, and that is known by the look and feel, not by a generic layout. Hence
 * {@link #layoutVScrollbar} is the longest method in the class.
 *
 * <h2>How the thumb is placed</h2>
 *
 * <p>Two pieces of arithmetic. The <em>length</em> is the part of the track that corresponds to
 * what is seen: {@code track * extent / range}, never less than the minimum nor more than the
 * maximum. The <em>position</em> hands out the track's leftover according to how much of the
 * useful travel has been covered: {@code (track - thumb) * (value - minimum) /
 * (range - extent)}. The case of being at the end is treated separately, sticking the thumb
 * against the bottom button, so that a pixel of track is not left over by a rounding.
 *
 * <p>If the thumb does not fit in the track it is given a size of zero, which is how this
 * family of looks and feels says "there is nothing to scroll".
 *
 * <h2>What is not there</h2>
 *
 * <p>The keyboard actions are not there: tying them needs the look and feel's table, which this
 * library does not have yet, so there would be nowhere to get which key does what from.
 */
public class BasicScrollBarUI extends ScrollBarUI implements LayoutManager, SwingConstants {

    /** Not highlighted either. */
    protected static final int NO_HIGHLIGHT = 0;

    /** The part of the track before the thumb, highlighted. */
    protected static final int DECREASE_HIGHLIGHT = 1;

    /** The part of the track after the thumb, highlighted. */
    protected static final int INCREASE_HIGHLIGHT = 2;

    protected Dimension minimumThumbSize;
    protected Dimension maximumThumbSize;

    protected Color thumbHighlightColor;
    protected Color thumbLightShadowColor;
    protected Color thumbDarkShadowColor;
    protected Color thumbColor;
    protected Color trackColor;
    protected Color trackHighlightColor;

    protected JScrollBar scrollbar;
    protected JButton incrButton;
    protected JButton decrButton;

    protected boolean isDragging;
    protected TrackListener trackListener;
    protected ArrowButtonListener buttonListener;

    /** The one that repeats while the button stays pressed; see {@link ScrollListener}. */
    protected ScrollListener scrollListener;

    /** The timer that calls it: 300 ms until the first beat and 60 between beats. */
    protected Timer scrollTimer;
    protected ModelListener modelListener;

    protected Rectangle thumbRect;
    protected Rectangle trackRect;

    protected int trackHighlight;

    protected PropertyChangeListener propertyChangeListener;

    /** The bar's width across; 17 in Metal. */
    protected int scrollBarWidth;

    /** The gap between the thumb and the bottom or right button; zero in Metal. */
    protected int incrGap;

    /** The gap between the thumb and the top or left button; zero in Metal. */
    protected int decrGap;

    private boolean supportsAbsolutePositioning;
    private boolean thumbActive;

    public BasicScrollBarUI() {
    }

    /** One look and feel per bar: it keeps the bar, its buttons and the rectangles. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicScrollBarUI();
    }

    /**
     * The bar's six colours.
     *
     * <p>They are those of {@code ScrollBar.*} measured in Metal (JDK 25): thumb
     * (163, 184, 204), its highlight (184, 207, 229), its shadow (99, 130, 191) and its dark
     * shadow (122, 138, 153); track (238, 238, 238) and its highlight (122, 138, 153).
     */
    protected void configureScrollBarColors() {
        if (scrollbar.getBackground() == null
                || scrollbar.getBackground() instanceof javax.swing.plaf.UIResource) {
            scrollbar.setBackground(new javax.swing.plaf.ColorUIResource(238, 238, 238));
        }
        if (scrollbar.getForeground() == null
                || scrollbar.getForeground() instanceof javax.swing.plaf.UIResource) {
            scrollbar.setForeground(new javax.swing.plaf.ColorUIResource(238, 238, 238));
        }
        thumbHighlightColor = new Color(184, 207, 229);
        thumbLightShadowColor = new Color(99, 130, 191);
        thumbDarkShadowColor = new Color(122, 138, 153);
        thumbColor = new Color(163, 184, 204);
        trackColor = new Color(238, 238, 238);
        trackHighlightColor = new Color(122, 138, 153);
    }

    public void installUI(JComponent c) {
        scrollbar = (JScrollBar) c;
        thumbRect = new Rectangle(0, 0, 0, 0);
        trackRect = new Rectangle(0, 0, 0, 0);
        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        scrollbar = (JScrollBar) c;
        uninstallListeners();
        uninstallDefaults();
        uninstallComponents();
        uninstallKeyboardActions();
        thumbRect = null;
        scrollbar = null;
        incrButton = null;
        decrButton = null;
    }

    /** See {@link #configureScrollBarColors}'s note for where the numbers come from. */
    protected void installDefaults() {
        scrollBarWidth = 17;
        minimumThumbSize = new DimensionUIResource(8, 8);
        maximumThumbSize = new DimensionUIResource(4096, 4096);
        supportsAbsolutePositioning = true;
        incrGap = 0;
        decrGap = 0;

        trackHighlight = NO_HIGHLIGHT;
        if (scrollbar.getLayout() == null
                || (scrollbar.getLayout() instanceof javax.swing.plaf.UIResource)) {
            scrollbar.setLayout(this);
        }
        configureScrollBarColors();
        LookAndFeel.installProperty(scrollbar, "opaque", Boolean.TRUE);
    }

    protected void installComponents() {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            incrButton = createIncreaseButton(SOUTH);
            decrButton = createDecreaseButton(NORTH);
        } else {
            incrButton = createIncreaseButton(EAST);
            decrButton = createDecreaseButton(WEST);
        }
        scrollbar.add(incrButton);
        scrollbar.add(decrButton);
        scrollbar.setEnabled(scrollbar.isEnabled());
    }

    protected void uninstallComponents() {
        if (incrButton != null) {
            scrollbar.remove(incrButton);
        }
        if (decrButton != null) {
            scrollbar.remove(decrButton);
        }
    }

    protected void installListeners() {
        trackListener = createTrackListener();
        buttonListener = createArrowButtonListener();
        modelListener = createModelListener();
        propertyChangeListener = createPropertyChangeListener();
        scrollListener = createScrollListener();

        scrollTimer = new Timer(60, scrollListener);
        scrollTimer.setInitialDelay(300);

        scrollbar.addMouseListener(trackListener);
        scrollbar.addMouseMotionListener(trackListener);
        scrollbar.getModel().addChangeListener(modelListener);
        scrollbar.addPropertyChangeListener(propertyChangeListener);

        if (incrButton != null) {
            incrButton.addMouseListener(buttonListener);
        }
        if (decrButton != null) {
            decrButton.addMouseListener(buttonListener);
        }
    }

    /** Nothing: with no {@code InputMap} there is nowhere to register keys; see the class note. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected void uninstallListeners() {
        if (incrButton != null) {
            incrButton.removeMouseListener(buttonListener);
        }
        if (decrButton != null) {
            decrButton.removeMouseListener(buttonListener);
        }
        scrollbar.getModel().removeChangeListener(modelListener);
        scrollbar.removeMouseListener(trackListener);
        scrollbar.removeMouseMotionListener(trackListener);
        scrollbar.removePropertyChangeListener(propertyChangeListener);
        if (scrollTimer != null) {
            scrollTimer.stop();
            scrollTimer = null;
        }
    }

    /** It leaves the colours set, like the JDK; it only lets go of the layout. */
    protected void uninstallDefaults() {
        if (scrollbar.getLayout() == this) {
            scrollbar.setLayout(null);
        }
    }

    protected TrackListener createTrackListener() {
        return new TrackListener();
    }

    /** It gets the listener ready and starts the timer from zero. */
    private void startTimer(int direction, boolean byBlocks) {
        if (scrollTimer == null || scrollListener == null) {
            return;
        }
        scrollTimer.stop();
        scrollListener.setDirection(direction);
        scrollListener.setScrollByBlock(byBlocks);
        scrollTimer.start();
    }

    protected ScrollListener createScrollListener() {
        return new ScrollListener();
    }

    protected ArrowButtonListener createArrowButtonListener() {
        return new ArrowButtonListener();
    }

    protected ModelListener createModelListener() {
        return new ModelListener();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new PropertyChangeHandler();
    }

    /** Whether the cursor is over the thumb; some looks and feels paint it differently. */
    protected void setThumbRollover(boolean active) {
        if (thumbActive != active) {
            thumbActive = active;
            scrollbar.repaint(getThumbBounds());
        }
    }

    public boolean isThumbRollover() {
        return thumbActive;
    }

    /** Track and thumb, in that order: the thumb goes on top. */
    public void paint(Graphics g, JComponent c) {
        paintTrack(g, c, getTrackBounds());
        Rectangle thumbBounds = getThumbBounds();
        Rectangle clip = g.getClipBounds();
        if (clip == null || thumbBounds.intersects(clip)) {
            paintThumb(g, c, thumbBounds);
        }
    }

    /** Forty-eight long by the bar's width: two buttons and a bit of track. */
    public Dimension getPreferredSize(JComponent c) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(scrollBarWidth, 48);
        }
        return new Dimension(48, scrollBarWidth);
    }

    /** No cap: a bar stretches as far as its container gives it. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    protected JButton createDecreaseButton(int orientation) {
        return new BasicArrowButton(orientation, thumbColor, thumbLightShadowColor,
                thumbDarkShadowColor, thumbHighlightColor);
    }

    protected JButton createIncreaseButton(int orientation) {
        return new BasicArrowButton(orientation, thumbColor, thumbLightShadowColor,
                thumbDarkShadowColor, thumbHighlightColor);
    }

    /** It paints the track before the thumb as highlighted; it is the held click on that half. */
    protected void paintDecreaseHighlight(Graphics g) {
        Insets insets = scrollbar.getInsets();
        Rectangle thumbR = getThumbBounds();
        g.setColor(trackHighlightColor);

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            int x = insets.left;
            int y = decrButton.getY() + decrButton.getHeight();
            int w = scrollbar.getWidth() - (insets.left + insets.right);
            int h = thumbR.y - y;
            g.fillRect(x, y, w, h);
        } else {
            int x = decrButton.getX() + decrButton.getWidth();
            int y = insets.top;
            int w = thumbR.x - x;
            int h = scrollbar.getHeight() - (insets.top + insets.bottom);
            g.fillRect(x, y, w, h);
        }
    }

    /** The same on the other side of the thumb. */
    protected void paintIncreaseHighlight(Graphics g) {
        Insets insets = scrollbar.getInsets();
        Rectangle thumbR = getThumbBounds();
        g.setColor(trackHighlightColor);

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            int x = insets.left;
            int y = thumbR.y + thumbR.height;
            int w = scrollbar.getWidth() - (insets.left + insets.right);
            int h = incrButton.getY() - y;
            g.fillRect(x, y, w, h);
        } else {
            int x = thumbR.x + thumbR.width;
            int y = insets.top;
            int w = incrButton.getX() - x;
            int h = scrollbar.getHeight() - (insets.top + insets.bottom);
            g.fillRect(x, y, w, h);
        }
    }

    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(trackColor);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);

        if (trackHighlight == DECREASE_HIGHLIGHT) {
            paintDecreaseHighlight(g);
        } else if (trackHighlight == INCREASE_HIGHLIGHT) {
            paintIncreaseHighlight(g);
        }
    }

    /**
     * The thumb: a dark frame, the fill, and two relief lines.
     *
     * <p>The frame is drawn before the fill and the fill covers it at the top and on the left:
     * only the right edge and the bottom one stay dark. It is the same sequence as
     * {@link BasicArrowButton}, and that is why the bar's three pieces look of the same family.
     */
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        int w = thumbBounds.width;
        int h = thumbBounds.height;

        g.translate(thumbBounds.x, thumbBounds.y);

        g.setColor(thumbDarkShadowColor);
        g.drawRect(0, 0, w - 1, h - 1);
        g.setColor(thumbColor);
        g.fillRect(0, 0, w - 1, h - 1);

        g.setColor(thumbHighlightColor);
        g.drawLine(1, 1, 1, h - 2);
        g.drawLine(2, 1, w - 3, 1);

        // The bottom line starts at x=2 and not at x=1: the corner's pixel is left for the
                // highlight, and that is why the thumb's relief looks continuous at that corner.
                // The arrow button, which draws almost the same, does start it at x=1; both ways
                // are measured.
        g.setColor(thumbLightShadowColor);
        g.drawLine(2, h - 2, w - 2, h - 2);
        g.drawLine(w - 2, 1, w - 2, h - 2);

        g.translate(-thumbBounds.x, -thumbBounds.y);
    }

    protected Dimension getMinimumThumbSize() {
        return minimumThumbSize;
    }

    protected Dimension getMaximumThumbSize() {
        return maximumThumbSize;
    }

    public void addLayoutComponent(String name, Component child) {
    }

    public void removeLayoutComponent(Component child) {
    }

    public Dimension preferredLayoutSize(Container scrollbarContainer) {
        return getPreferredSize((JComponent) scrollbarContainer);
    }

    public Dimension minimumLayoutSize(Container scrollbarContainer) {
        return getMinimumSize((JComponent) scrollbarContainer);
    }

    /** See the class note about the thumb's two pieces of arithmetic. */
    protected void layoutVScrollbar(JScrollBar sb) {
        Dimension sbSize = sb.getSize();
        Insets sbInsets = sb.getInsets();

        int itemW = sbSize.width - (sbInsets.left + sbInsets.right);
        int itemX = sbInsets.left;

        int decrButtonH = decrButton.getPreferredSize().height;
        int decrButtonY = sbInsets.top;

        int incrButtonH = incrButton.getPreferredSize().height;
        int incrButtonY = sbSize.height - (sbInsets.bottom + incrButtonH);

        int sbInsetsH = sbInsets.top + sbInsets.bottom;
        int sbButtonsH = decrButtonH + incrButtonH;
        float trackH = sbSize.height - (sbInsetsH + sbButtonsH) - (decrGap + incrGap);

        int min = sb.getMinimum();
        int max = sb.getMaximum();
        int extent = sb.getVisibleAmount();
        int range = max - min;
        int value = sb.getValue();

        int maxThumbH = getMaximumThumbSize().height;
        int minThumbH = getMinimumThumbSize().height;
        int thumbH = (range <= 0) ? maxThumbH
                : (int) (trackH * ((float) extent / (float) range));
        thumbH = Math.max(thumbH, minThumbH);
        thumbH = Math.min(thumbH, maxThumbH);

        int thumbY = incrButtonY - incrGap - thumbH;
        if (value < (max - extent)) {
            float thumbRange = trackH - thumbH;
            thumbY = (int) (0.5f
                    + (thumbRange * ((float) (value - min) / (float) (range - extent))));
            thumbY = thumbY + decrButtonY + decrButtonH + decrGap;
        }

        // If the two buttons do not fit, they share out what there is in equal parts.
        int sbAvailButtonH = (sbSize.height - sbInsetsH);
        if (sbAvailButtonH < sbButtonsH) {
            incrButtonH = sbAvailButtonH / 2;
            decrButtonH = incrButtonH;
            incrButtonY = sbSize.height - (sbInsets.bottom + incrButtonH);
        }
        decrButton.setBounds(itemX, decrButtonY, itemW, decrButtonH);
        incrButton.setBounds(itemX, incrButtonY, itemW, incrButtonH);

        int itrackY = decrButtonY + decrButtonH + decrGap;
        int itrackH = incrButtonY - incrGap - itrackY;
        trackRect.setBounds(itemX, itrackY, itemW, itrackH);

        if (thumbH >= (int) trackH) {
            setThumbBounds(0, 0, 0, 0);
        } else {
            if ((thumbY + thumbH) > incrButtonY - incrGap) {
                thumbY = incrButtonY - incrGap - thumbH;
            }
            if (thumbY < (decrButtonY + decrButtonH + decrGap)) {
                thumbY = decrButtonY + decrButtonH + decrGap + 1;
            }
            setThumbBounds(itemX, thumbY, itemW, thumbH);
        }
    }

    /** The same as {@link #layoutVScrollbar}, with the axes swapped. */
    protected void layoutHScrollbar(JScrollBar sb) {
        Dimension sbSize = sb.getSize();
        Insets sbInsets = sb.getInsets();

        int itemH = sbSize.height - (sbInsets.top + sbInsets.bottom);
        int itemY = sbInsets.top;

        boolean ltr = sb.getComponentOrientation().isLeftToRight();

        int leftButtonW = (ltr ? decrButton : incrButton).getPreferredSize().width;
        int rightButtonW = (ltr ? incrButton : decrButton).getPreferredSize().width;
        int leftButtonX = sbInsets.left;
        int rightButtonX = sbSize.width - (sbInsets.right + rightButtonW);
        int leftGap = ltr ? decrGap : incrGap;
        int rightGap = ltr ? incrGap : decrGap;

        int sbInsetsW = sbInsets.left + sbInsets.right;
        int sbButtonsW = leftButtonW + rightButtonW;
        float trackW = sbSize.width - (sbInsetsW + sbButtonsW) - (leftGap + rightGap);

        int min = sb.getMinimum();
        int max = sb.getMaximum();
        int extent = sb.getVisibleAmount();
        int range = max - min;
        int value = sb.getValue();

        int maxThumbW = getMaximumThumbSize().width;
        int minThumbW = getMinimumThumbSize().width;
        int thumbW = (range <= 0) ? maxThumbW
                : (int) (trackW * ((float) extent / (float) range));
        thumbW = Math.max(thumbW, minThumbW);
        thumbW = Math.min(thumbW, maxThumbW);

        int thumbX = ltr ? rightButtonX - rightGap - thumbW : leftButtonX + leftButtonW + leftGap;
        if (value < (max - extent)) {
            float thumbRange = trackW - thumbW;
            if (ltr) {
                thumbX = (int) (0.5f
                        + (thumbRange * ((float) (value - min) / (float) (range - extent))));
                thumbX = thumbX + leftButtonX + leftButtonW + leftGap;
            } else {
                thumbX = (int) (0.5f
                        + (thumbRange * ((float) (max - extent - value) / (float) (range - extent))));
                thumbX = thumbX + leftButtonX + leftButtonW + leftGap;
            }
        }

        int sbAvailButtonW = (sbSize.width - sbInsetsW);
        if (sbAvailButtonW < sbButtonsW) {
            rightButtonW = sbAvailButtonW / 2;
            leftButtonW = rightButtonW;
            rightButtonX = sbSize.width - (sbInsets.right + rightButtonW);
        }

        (ltr ? decrButton : incrButton).setBounds(leftButtonX, itemY, leftButtonW, itemH);
        (ltr ? incrButton : decrButton).setBounds(rightButtonX, itemY, rightButtonW, itemH);

        int itrackX = leftButtonX + leftButtonW + leftGap;
        int itrackW = rightButtonX - rightGap - itrackX;
        trackRect.setBounds(itrackX, itemY, itrackW, itemH);

        if (thumbW >= (int) trackW) {
            setThumbBounds(0, 0, 0, 0);
        } else {
            if (thumbX + thumbW > rightButtonX - rightGap) {
                thumbX = rightButtonX - rightGap - thumbW;
            }
            if (thumbX < itrackX) {
                thumbX = itrackX + 1;
            }
            setThumbBounds(thumbX, itemY, thumbW, itemH);
        }
    }

    public void layoutContainer(Container scrollbarContainer) {
        // It may arrive while the look and feel is being changed: the container rules.
        JScrollBar scrollbar = (JScrollBar) scrollbarContainer;
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            layoutVScrollbar(scrollbar);
        } else {
            layoutHScrollbar(scrollbar);
        }
    }

    /** It changes the thumb's rectangle and repaints the old and the new one. */
    protected void setThumbBounds(int x, int y, int width, int height) {
        if (thumbRect.x == x && thumbRect.y == y && thumbRect.width == width
                && thumbRect.height == height) {
            return;
        }
        int minX = Math.min(x, thumbRect.x);
        int minY = Math.min(y, thumbRect.y);
        int maxX = Math.max(x + width, thumbRect.x + thumbRect.width);
        int maxY = Math.max(y + height, thumbRect.y + thumbRect.height);

        thumbRect.setBounds(x, y, width, height);
        scrollbar.repaint(minX, minY, maxX - minX, maxY - minY);

        setThumbRollover(false);
    }

    /** A copy: whoever receives it may modify it without moving the thumb. */
    protected Rectangle getThumbBounds() {
        return thumbRect.getBounds();
    }

    protected Rectangle getTrackBounds() {
        return trackRect.getBounds();
    }

    /** It advances one screenful in that direction; the value settles itself against the caps. */
    protected void scrollByBlock(int direction) {
        scrollbar.setValueIsAdjusting(true);
        int oldValue = scrollbar.getValue();
        int blockIncrement = scrollbar.getBlockIncrement(direction);
        if (blockIncrement == 0) {
            blockIncrement = scrollbar.getVisibleAmount();
        }
        int delta = blockIncrement * ((direction > 0) ? +1 : -1);
        int newValue = oldValue + delta;

        // An overflow leaves the value at that side's cap.
        if (delta > 0 && newValue < oldValue) {
            newValue = scrollbar.getMaximum();
        } else if (delta < 0 && newValue > oldValue) {
            newValue = scrollbar.getMinimum();
        }
        scrollbar.setValue(newValue);
        scrollbar.setValueIsAdjusting(false);
    }

    /** It advances one small step in that direction. */
    protected void scrollByUnit(int direction) {
        int delta;
        if (direction > 0) {
            delta = scrollbar.getUnitIncrement(direction);
        } else {
            delta = -scrollbar.getUnitIncrement(direction);
        }
        int oldValue = scrollbar.getValue();
        int newValue = oldValue + delta;

        if (delta > 0 && newValue < oldValue) {
            newValue = scrollbar.getMaximum();
        } else if (delta < 0 && newValue > oldValue) {
            newValue = scrollbar.getMinimum();
        }
        if (oldValue != newValue) {
            scrollbar.setValue(newValue);
        }
    }

    /** Whether a click with the system's modifier jumps straight to that position. */
    public boolean getSupportsAbsolutePositioning() {
        return supportsAbsolutePositioning;
    }

    /**
     * It listens to the thumb's dragging and to the clicks on the track.
     *
     * <p>Dragging translates pixels into values with the rule of three inverse to the one that
     * places the thumb; the {@code offset} is where inside the thumb it was grabbed, and it is
     * what keeps the thumb from jumping under the cursor on starting to drag.
     */
    protected class TrackListener extends MouseAdapter implements MouseMotionListener {

        protected transient int offset;
        protected transient int currentMouseX;
        protected transient int currentMouseY;

        public TrackListener() {
        }

        public void mouseReleased(MouseEvent e) {
            if (isDragging) {
                updateThumbState(e.getX(), e.getY());
            }
            if (scrollTimer != null) {
                scrollTimer.stop();
            }
            isDragging = false;
            offset = 0;
            trackHighlight = NO_HIGHLIGHT;
            scrollbar.setValueIsAdjusting(false);
            scrollbar.repaint();
        }

        public void mousePressed(MouseEvent e) {
            if (!scrollbar.isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            currentMouseX = e.getX();
            currentMouseY = e.getY();

            Rectangle thumbR = getThumbBounds();
            if (thumbR.contains(currentMouseX, currentMouseY)) {
                if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                    offset = currentMouseY - thumbR.y;
                } else {
                    offset = currentMouseX - thumbR.x;
                }
                isDragging = true;
                scrollbar.setValueIsAdjusting(true);
                return;
            }
            isDragging = false;

            // A click on the track advances one screenful towards the click's side.
            int direction;
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                direction = (currentMouseY < thumbR.y) ? -1 : +1;
            } else if (scrollbar.getComponentOrientation().isLeftToRight()) {
                direction = (currentMouseX < thumbR.x) ? -1 : +1;
            } else {
                direction = (currentMouseX < thumbR.x) ? +1 : -1;
            }
            trackHighlight = (direction > 0) ? INCREASE_HIGHLIGHT : DECREASE_HIGHLIGHT;
            scrollByBlock(direction);
            startTimer(direction, true);
        }

        public void mouseDragged(MouseEvent e) {
            if (!isDragging || !scrollbar.isEnabled()) {
                return;
            }
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                verticalDrag(e.getY());
            } else {
                horizontalDrag(e.getX());
            }
        }

        /** It translates a pixel position into a model value, vertically. */
        private void verticalDrag(int y) {
            Rectangle thumbR = getThumbBounds();
            Rectangle trackRect = getTrackBounds();
            int thumbMin = trackRect.y;
            int thumbMax = trackRect.y + trackRect.height - thumbR.height;
            int thumbTop = Math.max(thumbMin, Math.min(thumbMax, y - offset));

            setThumbBounds(thumbR.x, thumbTop, thumbR.width, thumbR.height);

            BoundedRangeModel model = scrollbar.getModel();
            int valueMax = model.getMaximum() - model.getExtent();
            int valueRange = valueMax - model.getMinimum();
            int thumbRange = thumbMax - thumbMin;
            int value;
            if (thumbRange <= 0) {
                value = model.getMinimum();
            } else if (thumbTop == thumbMax) {
                value = valueMax;
            } else {
                value = model.getMinimum()
                        + (int) (0.5f + ((float) (thumbTop - thumbMin) * valueRange) / thumbRange);
            }
            scrollbar.setValue(value);
        }

        /** The same horizontally; the language's direction turns the rule of three around. */
        private void horizontalDrag(int x) {
            Rectangle thumbR = getThumbBounds();
            Rectangle trackRect = getTrackBounds();
            int thumbMin = trackRect.x;
            int thumbMax = trackRect.x + trackRect.width - thumbR.width;
            int thumbLeft = Math.max(thumbMin, Math.min(thumbMax, x - offset));

            setThumbBounds(thumbLeft, thumbR.y, thumbR.width, thumbR.height);

            BoundedRangeModel model = scrollbar.getModel();
            int valueMax = model.getMaximum() - model.getExtent();
            int valueRange = valueMax - model.getMinimum();
            int thumbRange = thumbMax - thumbMin;
            boolean ltr = scrollbar.getComponentOrientation().isLeftToRight();
            int value;
            if (thumbRange <= 0) {
                value = model.getMinimum();
            } else if (ltr && thumbLeft == thumbMax) {
                value = valueMax;
            } else if (!ltr && thumbLeft == thumbMin) {
                value = valueMax;
            } else {
                int shifted = ltr ? (thumbLeft - thumbMin) : (thumbMax - thumbLeft);
                value = model.getMinimum()
                        + (int) (0.5f + ((float) shifted * valueRange) / thumbRange);
            }
            scrollbar.setValue(value);
        }

        public void mouseMoved(MouseEvent e) {
            if (!isDragging) {
                updateThumbState(e.getX(), e.getY());
            }
        }

        public void mouseExited(MouseEvent e) {
            if (!isDragging) {
                setThumbRollover(false);
            }
        }
    }

    private void updateThumbState(int x, int y) {
        Rectangle rect = getThumbBounds();
        setThumbRollover(rect.contains(x, y));
    }

    /**
     * It listens to the two arrows: each click moves one step and, if it is held down, it goes on.
     */
    protected class ArrowButtonListener extends MouseAdapter {

        public ArrowButtonListener() {
        }

        public void mousePressed(MouseEvent e) {
            if (!scrollbar.isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            int direction = (e.getSource() == incrButton) ? 1 : -1;
            scrollByUnit(direction);
            startTimer(direction, false);
            if (!scrollbar.hasFocus() && scrollbar.isRequestFocusEnabled()) {
                scrollbar.requestFocus();
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (scrollTimer != null) {
                scrollTimer.stop();
            }
        }
    }

    /**
     * The beat of the continuous scrolling.
     *
     * <p>It is the timer's listener: every time it rings it moves one step -- or one screenful, if
     * the track was pressed -- in the direction it kept. It stops by itself in two cases, and both
     * matter.
     *
     * <p>The first is reaching the cap: going on beating there does nothing and costs.
     *
     * <p>The second is the thumb catching up with the cursor. On pressing the track the thumb
     * comes towards the cursor one screenful at a time; if it did not stop, it would go past it
     * and the content would go on running under a still finger. That is why it looks at the track
     * listener's {@code currentMouseX}/{@code currentMouseY} and not at the event: what matters is
     * where the cursor is now.
     */
    protected class ScrollListener implements ActionListener {

        private int direction = +1;
        private boolean useBlockIncrement;

        /** Forwards and one step at a time. */
        public ScrollListener() {
        }

        public ScrollListener(int dir, boolean block) {
            direction = dir;
            useBlockIncrement = block;
        }

        public void setDirection(int direction) {
            this.direction = direction;
        }

        public void setScrollByBlock(boolean block) {
            this.useBlockIncrement = block;
        }

        public void actionPerformed(ActionEvent e) {
            if (useBlockIncrement) {
                scrollByBlock(direction);
                if (reachedCursor()) {
                    stopTimer(e);
                    return;
                }
            } else {
                scrollByUnit(direction);
            }
            if (direction > 0
                    && scrollbar.getValue() + scrollbar.getVisibleAmount()
                            >= scrollbar.getMaximum()) {
                stopTimer(e);
            } else if (direction < 0 && scrollbar.getValue() <= scrollbar.getMinimum()) {
                stopTimer(e);
            }
        }

        /** Whether the thumb has already reached where the cursor is. */
        private boolean reachedCursor() {
            Rectangle thumbR = getThumbBounds();
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                if (direction > 0) {
                    return thumbR.y + thumbR.height >= trackListener.currentMouseY;
                }
                return thumbR.y <= trackListener.currentMouseY;
            }
            if (direction > 0) {
                return thumbR.x + thumbR.width >= trackListener.currentMouseX;
            }
            return thumbR.x <= trackListener.currentMouseX;
        }

        /** The timer is the event's source; that way it works too if somebody else called it. */
        private void stopTimer(ActionEvent e) {
            if (e.getSource() instanceof Timer) {
                ((Timer) e.getSource()).stop();
            } else if (scrollTimer != null) {
                scrollTimer.stop();
            }
        }
    }

    /** The model changed: the thumb has to be placed again. */
    protected class ModelListener implements ChangeListener {

        public ModelListener() {
        }

        public void stateChanged(ChangeEvent e) {
            if (!useCachedValue()) {
                layoutContainer(scrollbar);
            }
        }
    }

    /** Always {@code false}: the JDK caches the value during a drag, here it is not needed. */
    private boolean useCachedValue() {
        return false;
    }

    /**
     * A property of the bar changed: if it was the model or the orientation, it has to be laid out
     * again.
     */
    public class PropertyChangeHandler implements PropertyChangeListener {

        public PropertyChangeHandler() {
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();

            if ("model".equals(propertyName)) {
                BoundedRangeModel oldModel = (BoundedRangeModel) e.getOldValue();
                BoundedRangeModel newModel = (BoundedRangeModel) e.getNewValue();
                if (oldModel != null) {
                    oldModel.removeChangeListener(modelListener);
                }
                if (newModel != null) {
                    newModel.addChangeListener(modelListener);
                }
                scrollbar.repaint();
                scrollbar.revalidate();
            } else if ("orientation".equals(propertyName)) {
                updateButtonDirections();
            } else if ("componentOrientation".equals(propertyName)) {
                updateButtonDirections();
            }
        }
    }

    /** It turns the arrows around when the bar's or the language's orientation changes. */
    private void updateButtonDirections() {
        int orient = scrollbar.getOrientation();
        if (scrollbar.getComponentOrientation().isLeftToRight()) {
            if (incrButton instanceof BasicArrowButton) {
                ((BasicArrowButton) incrButton).setDirection(
                        orient == JScrollBar.HORIZONTAL ? EAST : SOUTH);
                ((BasicArrowButton) decrButton).setDirection(
                        orient == JScrollBar.HORIZONTAL ? WEST : NORTH);
            }
        } else {
            if (incrButton instanceof BasicArrowButton) {
                ((BasicArrowButton) incrButton).setDirection(
                        orient == JScrollBar.HORIZONTAL ? WEST : SOUTH);
                ((BasicArrowButton) decrButton).setDirection(
                        orient == JScrollBar.HORIZONTAL ? EAST : NORTH);
            }
        }
    }
}
