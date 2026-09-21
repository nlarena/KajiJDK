package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a slider.
 *
 * <h2>Six rectangles, one inside the other</h2>
 *
 * <p>The whole class turns around six rectangles that are computed in a chain, each one from
 * the previous: {@link #focusRect} is the component minus its margins; {@link #contentRect} is
 * that minus the focus's air; {@link #trackRect} is the strip the thumb runs along;
 * {@link #tickRect} and {@link #labelRect} are the two strips at the bottom, which measure zero
 * if the slider shows neither ticks nor labels; and {@link #thumbRect} is the thumb.
 *
 * <p>The chain is recomputed entirely every time something that affects it changes -- the size,
 * the orientation, the margins, whether it shows ticks --, and not piecemeal: recomputing half
 * of it would be faster and would leave pairs of rectangles that do not match.
 *
 * <h2>The rectangles may have a negative width, and that is right</h2>
 *
 * <p>A slider that does not have a size yet gives a {@code trackRect} of width -10: the buffer
 * of five pixels on each side is subtracted from a content that measures zero. It is measured,
 * and it is not corrected: correcting it by hiding the negative would give thumb positions
 * different from the JDK's as soon as the slider had a size.
 *
 * <h2>From value to pixel and back</h2>
 *
 * <p>{@link #xPositionForValue} and {@link #yPositionForValue} are a rule of three between the
 * model's range and the strip's length, with both ends clipped. The clipping is what keeps the
 * thumb from going outside when the range is odd, and it is also what makes the three positions
 * give the same number in a slider with no size.
 *
 * <h2>The scrolling timer</h2>
 *
 * <p>Pressing on the strip -- not on the thumb -- makes it advance one block at a time, and
 * going on pressing repeats it: {@link #scrollTimer} is what repeats. The direction comes from
 * which side of the thumb was pressed.
 *
 * <h2>What is left said</h2>
 *
 * <p>The drawing of the thumb and of the strip belongs to the real look and feel; the basic one
 * draws flat rectangles. The labels are the program's components and are painted as they are.
 */
public class BasicSliderUI extends SliderUI {

    /** One block forwards. */
    public static final int POSITIVE_SCROLL = +1;

    /** One block backwards. */
    public static final int NEGATIVE_SCROLL = -1;

    /** As far as the minimum. */
    public static final int MIN_SCROLL = -2;

    /** As far as the maximum. */
    public static final int MAX_SCROLL = +2;

    protected Timer scrollTimer;
    protected JSlider slider;

    protected Insets focusInsets = null;
    protected Insets insetCache = null;
    protected boolean leftToRightCache = true;
    protected Rectangle focusRect = null;
    protected Rectangle contentRect = null;
    protected Rectangle labelRect = null;
    protected Rectangle tickRect = null;
    protected Rectangle trackRect = null;
    protected Rectangle thumbRect = null;

    /** The air left for the thumb at each end of the strip. */
    protected int trackBuffer = 0;

    protected ChangeListener changeListener;
    protected ComponentListener componentListener;
    protected FocusListener focusListener;
    protected ScrollListener scrollListener;
    protected PropertyChangeListener propertyChangeListener;
    protected TrackListener trackListener;

    private Color shadowColor;
    private Color highlightColor;
    private Color focusColor;
    private boolean dragging;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource FOCUS = new ColorUIResource(163, 184, 204);
    private static final ColorUIResource HIGHLIGHT = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource SHADOW = new ColorUIResource(184, 207, 229);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    /** For that slider; the rectangles are built empty and filled in by {@code installUI}. */
    public BasicSliderUI(JSlider b) {
        focusRect = new Rectangle();
        contentRect = new Rectangle();
        labelRect = new Rectangle();
        tickRect = new Rectangle();
        trackRect = new Rectangle();
        thumbRect = new Rectangle();
        insetCache = new Insets(0, 0, 0, 0);
        focusInsets = new Insets(0, 0, 0, 0);
    }

    /** A new one per slider: it keeps the six rectangles. */
    public static ComponentUI createUI(JComponent b) {
        return new BasicSliderUI((JSlider) b);
    }

    public void installUI(JComponent c) {
        slider = (JSlider) c;
        dragging = false;
        installDefaults(slider);
        installListeners(slider);
        installKeyboardActions(slider);
        insetCache = slider.getInsets();
        leftToRightCache = slider.getComponentOrientation().isLeftToRight();
        calculateGeometry();
    }

    public void uninstallUI(JComponent c) {
        if (c != slider) {
            throw new IllegalComponentStateException(this + " was asked to deinstall() "
                    + c + " when it only knows about " + slider + ".");
        }
        if (scrollTimer != null) {
            scrollTimer.stop();
        }
        scrollTimer = null;
        uninstallDefaults(slider);
        uninstallListeners(slider);
        uninstallKeyboardActions(slider);
        insetCache = null;
        leftToRightCache = true;
        focusRect = null;
        contentRect = null;
        labelRect = null;
        tickRect = null;
        trackRect = null;
        thumbRect = null;
        slider = null;
    }

    /** Colours and typeface; the values are those of {@code Slider.*} in Metal. */
    protected void installDefaults(JSlider slider) {
        Color background = slider.getBackground();
        if (background == null || background instanceof UIResource) {
            slider.setBackground(BACKGROUND);
        }
        Color foreground = slider.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            slider.setForeground(FOREGROUND);
        }
        Font font = slider.getFont();
        if (font == null || font instanceof UIResource) {
            slider.setFont(FONT);
        }
        LookAndFeel.installProperty(slider, "opaque", Boolean.TRUE);
        focusInsets = new Insets(0, 0, 0, 0);
        focusColor = FOCUS;
        highlightColor = HIGHLIGHT;
        shadowColor = SHADOW;
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults(JSlider slider) {
    }

    protected void installListeners(JSlider slider) {
        trackListener = createTrackListener(slider);
        changeListener = createChangeListener(slider);
        componentListener = createComponentListener(slider);
        focusListener = createFocusListener(slider);
        scrollListener = createScrollListener(slider);
        propertyChangeListener = createPropertyChangeListener(slider);

        slider.addMouseListener(trackListener);
        slider.addMouseMotionListener(trackListener);
        slider.addFocusListener(focusListener);
        slider.addComponentListener(componentListener);
        slider.addPropertyChangeListener(propertyChangeListener);
        slider.getModel().addChangeListener(changeListener);

        scrollTimer = new Timer(100, scrollListener);
        scrollTimer.setInitialDelay(300);
    }

    protected void uninstallListeners(JSlider slider) {
        slider.removeMouseListener(trackListener);
        slider.removeMouseMotionListener(trackListener);
        slider.removeFocusListener(focusListener);
        slider.removeComponentListener(componentListener);
        slider.removePropertyChangeListener(propertyChangeListener);
        slider.getModel().removeChangeListener(changeListener);
        trackListener = null;
        changeListener = null;
        componentListener = null;
        focusListener = null;
        scrollListener = null;
        propertyChangeListener = null;
    }

    /** With no shortcuts of its own: the arrows are tied by the look and feel's table. */
    protected void installKeyboardActions(JSlider slider) {
    }

    protected void uninstallKeyboardActions(JSlider slider) {
    }

    protected TrackListener createTrackListener(JSlider slider) {
        return new TrackListener(this);
    }

    protected ChangeListener createChangeListener(JSlider slider) {
        return new Handler(this);
    }

    protected ComponentListener createComponentListener(JSlider slider) {
        return new Handler(this);
    }

    protected FocusListener createFocusListener(JSlider slider) {
        return new Handler(this);
    }

    protected ScrollListener createScrollListener(JSlider slider) {
        return new ScrollListener(this);
    }

    protected PropertyChangeListener createPropertyChangeListener(JSlider slider) {
        return new Handler(this);
    }

    protected Color getShadowColor() {
        return shadowColor;
    }

    protected Color getHighlightColor() {
        return highlightColor;
    }

    protected Color getFocusColor() {
        return focusColor;
    }

    /** Whether the thumb is being dragged. */
    protected boolean isDragging() {
        return dragging;
    }

    /** Whether the slider is turned around -- the maximum on the minimum's side --. */
    protected boolean drawInverted() {
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            if (leftToRightCache) {
                return slider.getInverted();
            }
            return !slider.getInverted();
        }
        return slider.getInverted();
    }

    /** The smallest value of the label table, or {@code null} if there is no table. */
    protected Integer getLowestValue() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return null;
        }
        Enumeration<?> keys = dictionary.keys();
        Integer min = null;
        while (keys.hasMoreElements()) {
            Object k = keys.nextElement();
            if (k instanceof Integer) {
                Integer i = (Integer) k;
                if (min == null || i.intValue() < min.intValue()) {
                    min = i;
                }
            }
        }
        return min;
    }

    /** And the largest. */
    protected Integer getHighestValue() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return null;
        }
        Enumeration<?> keys = dictionary.keys();
        Integer max = null;
        while (keys.hasMoreElements()) {
            Object k = keys.nextElement();
            if (k instanceof Integer) {
                Integer i = (Integer) k;
                if (max == null || i.intValue() > max.intValue()) {
                    max = i;
                }
            }
        }
        return max;
    }

    /** The smallest value's label, or {@code null}. */
    protected Component getLowestValueLabel() {
        Integer min = getLowestValue();
        if (min == null) {
            return null;
        }
        Object o = slider.getLabelTable().get(min);
        return (o instanceof Component) ? (Component) o : null;
    }

    /** And the largest one's. */
    protected Component getHighestValueLabel() {
        Integer max = getHighestValue();
        if (max == null) {
            return null;
        }
        Object o = slider.getLabelTable().get(max);
        return (o instanceof Component) ? (Component) o : null;
    }

    protected int getWidthOfLowValueLabel() {
        Component label = getLowestValueLabel();
        return (label == null) ? 0 : label.getPreferredSize().width;
    }

    protected int getWidthOfHighValueLabel() {
        Component label = getHighestValueLabel();
        return (label == null) ? 0 : label.getPreferredSize().width;
    }

    protected int getHeightOfHighValueLabel() {
        Component label = getHighestValueLabel();
        return (label == null) ? 0 : label.getPreferredSize().height;
    }

    protected int getHeightOfLowValueLabel() {
        Component label = getLowestValueLabel();
        return (label == null) ? 0 : label.getPreferredSize().height;
    }

    /**
     * Whether every label rests its text at the same height.
     *
     * <p>It serves a single purpose, and it is the one that justifies the method: if they share a
     * baseline, the row of labels can be aligned by it and the numbers come out straight even
     * though one label is taller than another. If they do not share it -- one label is an icon, or
     * has two lines -- they have to be centred vertically, which is the only thing left.
     *
     * <p>With no label table the answer is no, even though it sounds the other way round: there
     * are no labels to align, so there is no shared baseline to hang them from. An empty table, on
     * the other hand, answers yes -- there is none to contradict it --. Both measured.
     *
     * <p>The JDK keeps the result and recomputes it when the table changes; here it is computed
     * each time. It is the same answer and it cannot go stale.
     */
    protected boolean labelsHaveSameBaselines() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return false;
        }
        int base = -1;
        Enumeration<?> elements = dictionary.elements();
        while (elements.hasMoreElements()) {
            Object o = elements.nextElement();
            if (!(o instanceof Component)) {
                return false;
            }
            Component label = (Component) o;
            Dimension pref = label.getPreferredSize();
            int its = label.getBaseline(pref.width, pref.height);
            if (its < 0) {
                return false;
            }
            if (base == -1) {
                base = its;
            } else if (base != its) {
                return false;
            }
        }
        return true;
    }

    /** The tallest label's height; zero if there is none. */
    protected int getHeightOfTallestLabel() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return 0;
        }
        int height = 0;
        Enumeration<?> elements = dictionary.elements();
        while (elements.hasMoreElements()) {
            Object o = elements.nextElement();
            if (o instanceof Component) {
                height = Math.max(height, ((Component) o).getPreferredSize().height);
            }
        }
        return height;
    }

    /** Eight pixels; it is what the big ticks measure. */
    protected int getTickLength() {
        return 8;
    }

    /** The thumb's size; the basic one makes it 11 x 20, and 20 x 11 lying down. */
    protected Dimension getThumbSize() {
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            return new Dimension(11, 20);
        }
        return new Dimension(20, 11);
    }

    public Dimension getPreferredHorizontalSize() {
        return new Dimension(200, 21);
    }

    public Dimension getPreferredVerticalSize() {
        return new Dimension(21, 200);
    }

    public Dimension getMinimumHorizontalSize() {
        return new Dimension(36, 21);
    }

    public Dimension getMinimumVerticalSize() {
        return new Dimension(21, 36);
    }

    /** The reference one, with the height -- or the width -- the three strips ask for. */
    public Dimension getPreferredSize(JComponent c) {
        recalculateIfInsetsChanged();
        Dimension d;
        if (slider.getOrientation() == SwingConstants.VERTICAL) {
            d = new Dimension(getPreferredVerticalSize());
            d.width = insetCache.left + insetCache.right;
            d.width += focusInsets.left + focusInsets.right;
            d.width += trackRect.width + tickRect.width + labelRect.width;
        } else {
            d = new Dimension(getPreferredHorizontalSize());
            d.height = insetCache.top + insetCache.bottom;
            d.height += focusInsets.top + focusInsets.bottom;
            d.height += trackRect.height + tickRect.height + labelRect.height;
        }
        return d;
    }

    /** The same, from the reference minimum. */
    public Dimension getMinimumSize(JComponent c) {
        recalculateIfInsetsChanged();
        Dimension d;
        if (slider.getOrientation() == SwingConstants.VERTICAL) {
            d = new Dimension(getMinimumVerticalSize());
            d.width = insetCache.left + insetCache.right;
            d.width += focusInsets.left + focusInsets.right;
            d.width += trackRect.width + tickRect.width + labelRect.width;
        } else {
            d = new Dimension(getMinimumHorizontalSize());
            d.height = insetCache.top + insetCache.bottom;
            d.height += focusInsets.top + focusInsets.bottom;
            d.height += trackRect.height + tickRect.height + labelRect.height;
        }
        return d;
    }

    /** It stretches lengthwise and not at all widthwise. */
    public Dimension getMaximumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        if (slider.getOrientation() == SwingConstants.VERTICAL) {
            d.height = Short.MAX_VALUE;
        } else {
            d.width = Short.MAX_VALUE;
        }
        return d;
    }

    /** It rebuilds the whole chain of rectangles; see the class note. */
    protected void calculateGeometry() {
        calculateFocusRect();
        calculateContentRect();
        calculateThumbSize();
        calculateTrackBuffer();
        calculateTrackRect();
        calculateTickRect();
        calculateLabelRect();
        calculateThumbLocation();
    }

    protected void calculateFocusRect() {
        focusRect.x = insetCache.left;
        focusRect.y = insetCache.top;
        focusRect.width = slider.getWidth() - (insetCache.left + insetCache.right);
        focusRect.height = slider.getHeight() - (insetCache.top + insetCache.bottom);
    }

    protected void calculateContentRect() {
        contentRect.x = focusRect.x + focusInsets.left;
        contentRect.y = focusRect.y + focusInsets.top;
        contentRect.width = focusRect.width - (focusInsets.left + focusInsets.right);
        contentRect.height = focusRect.height - (focusInsets.top + focusInsets.bottom);
    }

    protected void calculateThumbSize() {
        Dimension size = getThumbSize();
        thumbRect.setSize(size.width, size.height);
    }

    /** Half the thumb on each side, so that it does not go outside at the ends. */
    protected void calculateTrackBuffer() {
        if (slider.getPaintLabels() && slider.getLabelTable() != null) {
            Component highLabel = getHighestValueLabel();
            Component lowLabel = getLowestValueLabel();
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                trackBuffer = Math.max((highLabel == null) ? 0 : highLabel.getBounds().width / 2,
                        (lowLabel == null) ? 0 : lowLabel.getBounds().width / 2);
                trackBuffer = Math.max(trackBuffer, thumbRect.width / 2);
            } else {
                trackBuffer = Math.max((highLabel == null) ? 0 : highLabel.getBounds().height / 2,
                        (lowLabel == null) ? 0 : lowLabel.getBounds().height / 2);
                trackBuffer = Math.max(trackBuffer, thumbRect.height / 2);
            }
        } else {
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                trackBuffer = thumbRect.width / 2;
            } else {
                trackBuffer = thumbRect.height / 2;
            }
        }
    }

    protected void calculateTrackRect() {
        int centerSpacing;
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            centerSpacing = thumbRect.height;
            if (slider.getPaintTicks()) {
                centerSpacing += getTickLength();
            }
            if (slider.getPaintLabels()) {
                centerSpacing += getHeightOfTallestLabel();
            }
            trackRect.x = contentRect.x + trackBuffer;
            trackRect.y = contentRect.y + (contentRect.height - centerSpacing - 1) / 2;
            trackRect.width = contentRect.width - (trackBuffer * 2);
            trackRect.height = thumbRect.height;
        } else {
            centerSpacing = thumbRect.width;
            if (leftToRightCache) {
                if (slider.getPaintTicks()) {
                    centerSpacing += getTickLength();
                }
                if (slider.getPaintLabels()) {
                    centerSpacing += getWidthOfWidestLabel();
                }
            } else {
                if (slider.getPaintTicks()) {
                    centerSpacing -= getTickLength();
                }
                if (slider.getPaintLabels()) {
                    centerSpacing -= getWidthOfWidestLabel();
                }
            }
            trackRect.x = contentRect.x + (contentRect.width - centerSpacing - 1) / 2;
            trackRect.y = contentRect.y + trackBuffer;
            trackRect.width = thumbRect.width;
            trackRect.height = contentRect.height - (trackBuffer * 2);
        }
    }

    private int getWidthOfWidestLabel() {
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return 0;
        }
        int width = 0;
        Enumeration<?> elements = dictionary.elements();
        while (elements.hasMoreElements()) {
            Object o = elements.nextElement();
            if (o instanceof Component) {
                width = Math.max(width, ((Component) o).getPreferredSize().width);
            }
        }
        return width;
    }

    private void calculateTickRect() {
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            tickRect.x = trackRect.x;
            tickRect.y = trackRect.y + trackRect.height;
            tickRect.width = trackRect.width;
            tickRect.height = slider.getPaintTicks() ? getTickLength() : 0;
        } else {
            tickRect.width = slider.getPaintTicks() ? getTickLength() : 0;
            if (leftToRightCache) {
                tickRect.x = trackRect.x + trackRect.width;
            } else {
                tickRect.x = trackRect.x - tickRect.width;
            }
            tickRect.y = trackRect.y;
            tickRect.height = trackRect.height;
        }
    }

    protected void calculateLabelRect() {
        if (slider.getPaintLabels()) {
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                labelRect.x = tickRect.x - trackBuffer;
                labelRect.y = tickRect.y + tickRect.height;
                labelRect.width = tickRect.width + (trackBuffer * 2);
                labelRect.height = getHeightOfTallestLabel();
            } else {
                labelRect.width = getWidthOfWidestLabel();
                if (leftToRightCache) {
                    labelRect.x = tickRect.x + tickRect.width;
                } else {
                    labelRect.x = tickRect.x - labelRect.width;
                }
                labelRect.y = tickRect.y - trackBuffer;
                labelRect.height = tickRect.height + (trackBuffer * 2);
            }
        } else {
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                labelRect.x = tickRect.x;
                labelRect.y = tickRect.y + tickRect.height;
                labelRect.width = tickRect.width;
                labelRect.height = 0;
            } else {
                if (leftToRightCache) {
                    labelRect.x = tickRect.x + tickRect.width;
                } else {
                    labelRect.x = tickRect.x;
                }
                labelRect.y = tickRect.y;
                labelRect.width = 0;
                labelRect.height = tickRect.height;
            }
        }
    }

    protected void calculateThumbLocation() {
        if (slider.getSnapToTicks()) {
            // With ticks, the thumb jumps from one to the other instead of staying in the middle.
            int sliderValue = slider.getValue();
            int snappedValue = sliderValue;
            int tickSpacing = getTickSpacing();
            if (tickSpacing != 0) {
                int min = slider.getMinimum();
                if ((sliderValue - min) % tickSpacing != 0) {
                    float temp = (float) (sliderValue - min) / (float) tickSpacing;
                    snappedValue = min + (Math.round(temp) * tickSpacing);
                }
                if (snappedValue != sliderValue) {
                    slider.setValue(snappedValue);
                }
            }
        }
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            int valuePosition = xPositionForValue(slider.getValue());
            thumbRect.x = valuePosition - (thumbRect.width / 2);
            thumbRect.y = trackRect.y;
        } else {
            int valuePosition = yPositionForValue(slider.getValue());
            thumbRect.x = trackRect.x;
            thumbRect.y = valuePosition - (thumbRect.height / 2);
        }
    }

    private int getTickSpacing() {
        int majorTickSpacing = slider.getMajorTickSpacing();
        int minorTickSpacing = slider.getMinorTickSpacing();
        if (minorTickSpacing > 0) {
            return minorTickSpacing;
        }
        if (majorTickSpacing > 0) {
            return majorTickSpacing;
        }
        return 0;
    }

    /** It puts the thumb there and redraws what is needed. */
    public void setThumbLocation(int x, int y) {
        Rectangle unionRect = new Rectangle();
        unionRect.setBounds(thumbRect);
        thumbRect.setLocation(x, y);
        javax.swing.SwingUtilities.computeUnion(thumbRect.x, thumbRect.y,
                thumbRect.width, thumbRect.height, unionRect);
        slider.repaint(unionRect.x, unionRect.y, unionRect.width, unionRect.height);
    }

    /** The pixel that falls to that value; see the class note. */
    protected int xPositionForValue(int value) {
        int min = slider.getMinimum();
        int max = slider.getMaximum();
        int trackLength = trackRect.width;
        double valueRange = (double) max - (double) min;
        double pixelsPerValue = (double) trackLength / valueRange;
        int trackLeft = trackRect.x;
        int trackRight = trackRect.x + (trackRect.width - 1);
        int xPosition;
        if (!drawInverted()) {
            xPosition = trackLeft;
            xPosition += Math.round(pixelsPerValue * ((double) value - min));
        } else {
            xPosition = trackRight;
            xPosition -= Math.round(pixelsPerValue * ((double) value - min));
        }
        xPosition = Math.max(trackLeft, xPosition);
        xPosition = Math.min(trackRight, xPosition);
        return xPosition;
    }

    /** The same on the other axis. */
    protected int yPositionForValue(int value) {
        return yPositionForValue(value, trackRect.y, trackRect.height);
    }

    /** The same, with a given strip; useful for drawing in a borrowed rectangle. */
    protected int yPositionForValue(int value, int trackY, int trackHeight) {
        int min = slider.getMinimum();
        int max = slider.getMaximum();
        double valueRange = (double) max - (double) min;
        double pixelsPerValue = (double) trackHeight / valueRange;
        int trackBottom = trackY + (trackHeight - 1);
        int yPosition;
        if (!drawInverted()) {
            yPosition = trackY;
            yPosition += Math.round(pixelsPerValue * ((double) max - value));
        } else {
            yPosition = trackY;
            yPosition += Math.round(pixelsPerValue * ((double) value - min));
        }
        yPosition = Math.max(trackY, yPosition);
        yPosition = Math.min(trackBottom, yPosition);
        return yPosition;
    }

    /** It rebuilds the geometry if the margins changed. */
    protected void recalculateIfInsetsChanged() {
        Insets newInsets = slider.getInsets();
        if (!newInsets.equals(insetCache)) {
            insetCache = newInsets;
            calculateGeometry();
        }
    }

    /** The same if the language's orientation changed. */
    protected void recalculateIfOrientationChanged() {
        boolean ltr = slider.getComponentOrientation().isLeftToRight();
        if (ltr != leftToRightCache) {
            leftToRightCache = ltr;
            calculateGeometry();
        }
    }

    /** It advances one block in that direction. */
    public void scrollByBlock(int direction) {
        synchronized (slider) {
            int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / 10;
            if (blockIncrement == 0) {
                blockIncrement = 1;
            }
            if (slider.getSnapToTicks()) {
                int tickSpacing = getTickSpacing();
                if (blockIncrement < tickSpacing) {
                    blockIncrement = tickSpacing;
                }
            }
            int delta = blockIncrement * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
            slider.setValue(slider.getValue() + delta);
        }
    }

    /** And one unit. */
    public void scrollByUnit(int direction) {
        synchronized (slider) {
            int delta = ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);
            if (slider.getSnapToTicks()) {
                delta *= getTickSpacing();
            }
            slider.setValue(slider.getValue() + delta);
        }
    }

    /** Pressing on the strip: it advances one block towards where it was pressed. */
    protected void scrollDueToClickInTrack(int dir) {
        scrollByBlock(dir);
    }

    public void paint(Graphics g, JComponent c) {
        recalculateIfInsetsChanged();
        recalculateIfOrientationChanged();
        Rectangle clip = g.getClipBounds();
        if (slider.getPaintTrack() && clip.intersects(trackRect)) {
            paintTrack(g);
        }
        if (slider.getPaintTicks() && clip.intersects(tickRect)) {
            paintTicks(g);
        }
        if (slider.getPaintLabels() && clip.intersects(labelRect)) {
            paintLabels(g);
        }
        if (slider.hasFocus() && clip.intersects(focusRect)) {
            paintFocus(g);
        }
        if (clip.intersects(thumbRect)) {
            paintThumb(g);
        }
    }

    /** A dotted rectangle around it; see the class note. */
    public void paintFocus(Graphics g) {
        g.setColor(getFocusColor());
        g.drawRect(focusRect.x, focusRect.y, focusRect.width - 1, focusRect.height - 1);
    }

    /** The strip: a thin dent in the middle of the available height. */
    public void paintTrack(Graphics g) {
        Rectangle trackBounds = trackRect;
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            int cy = (trackBounds.height / 2) - 2;
            int cw = trackBounds.width;
            g.translate(trackBounds.x, trackBounds.y + cy);
            g.setColor(getShadowColor());
            g.drawLine(0, 0, cw - 1, 0);
            g.drawLine(0, 1, 0, 2);
            g.setColor(getHighlightColor());
            g.drawLine(0, 3, cw, 3);
            g.drawLine(cw, 0, cw, 3);
            g.translate(-trackBounds.x, -(trackBounds.y + cy));
        } else {
            int cx = (trackBounds.width / 2) - 2;
            int ch = trackBounds.height;
            g.translate(trackBounds.x + cx, trackBounds.y);
            g.setColor(getShadowColor());
            g.drawLine(0, 0, 0, ch - 1);
            g.drawLine(1, 0, 2, 0);
            g.setColor(getHighlightColor());
            g.drawLine(3, 0, 3, ch);
            g.drawLine(0, ch, 3, ch);
            g.translate(-(trackBounds.x + cx), -trackBounds.y);
        }
    }

    /** The big and small ticks. */
    public void paintTicks(Graphics g) {
        Rectangle tickBounds = tickRect;
        g.setColor(getShadowColor());
        if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
            g.translate(0, tickBounds.y);
            if (slider.getMinorTickSpacing() > 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    int xPos = xPositionForValue(value);
                    paintMinorTickForHorizSlider(g, tickBounds, xPos);
                    if (Integer.MAX_VALUE - slider.getMinorTickSpacing() < value) {
                        break;
                    }
                    value += slider.getMinorTickSpacing();
                }
            }
            if (slider.getMajorTickSpacing() > 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    int xPos = xPositionForValue(value);
                    paintMajorTickForHorizSlider(g, tickBounds, xPos);
                    if (Integer.MAX_VALUE - slider.getMajorTickSpacing() < value) {
                        break;
                    }
                    value += slider.getMajorTickSpacing();
                }
            }
            g.translate(0, -tickBounds.y);
        } else {
            g.translate(tickBounds.x, 0);
            if (slider.getMinorTickSpacing() > 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    int yPos = yPositionForValue(value);
                    paintMinorTickForVertSlider(g, tickBounds, yPos);
                    if (Integer.MAX_VALUE - slider.getMinorTickSpacing() < value) {
                        break;
                    }
                    value += slider.getMinorTickSpacing();
                }
            }
            if (slider.getMajorTickSpacing() > 0) {
                int value = slider.getMinimum();
                while (value <= slider.getMaximum()) {
                    int yPos = yPositionForValue(value);
                    paintMajorTickForVertSlider(g, tickBounds, yPos);
                    if (Integer.MAX_VALUE - slider.getMajorTickSpacing() < value) {
                        break;
                    }
                    value += slider.getMajorTickSpacing();
                }
            }
            g.translate(-tickBounds.x, 0);
        }
    }

    protected void paintMinorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
        g.drawLine(x, 0, x, tickBounds.height / 2 - 1);
    }

    protected void paintMajorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
        g.drawLine(x, 0, x, tickBounds.height - 2);
    }

    protected void paintMinorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
        g.drawLine(0, y, tickBounds.width / 2 - 1, y);
    }

    protected void paintMajorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
        g.drawLine(0, y, tickBounds.width - 2, y);
    }

    /** The labels, each one centred on its value. */
    public void paintLabels(Graphics g) {
        Rectangle labelBounds = labelRect;
        Dictionary<?, ?> dictionary = slider.getLabelTable();
        if (dictionary == null) {
            return;
        }
        Enumeration<?> keys = dictionary.keys();
        int minValue = slider.getMinimum();
        int maxValue = slider.getMaximum();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = dictionary.get(key);
            if (!(key instanceof Integer) || !(value instanceof Component)) {
                continue;
            }
            int labelValue = ((Integer) key).intValue();
            if (labelValue < minValue || labelValue > maxValue) {
                continue;
            }
            Component label = (Component) value;
            if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
                g.translate(0, labelBounds.y);
                paintHorizontalLabel(g, labelValue, label);
                g.translate(0, -labelBounds.y);
            } else {
                int offset = 0;
                if (!leftToRightCache) {
                    offset = labelBounds.width - label.getPreferredSize().width;
                }
                g.translate(labelBounds.x + offset, 0);
                paintVerticalLabel(g, labelValue, label);
                g.translate(-labelBounds.x - offset, 0);
            }
        }
    }

    protected void paintHorizontalLabel(Graphics g, int value, Component label) {
        int labelCenter = xPositionForValue(value);
        int labelLeft = labelCenter - (label.getPreferredSize().width / 2);
        g.translate(labelLeft, 0);
        label.paint(g);
        g.translate(-labelLeft, 0);
    }

    protected void paintVerticalLabel(Graphics g, int value, Component label) {
        int labelCenter = yPositionForValue(value);
        int labelTop = labelCenter - (label.getPreferredSize().height / 2);
        g.translate(0, labelTop);
        label.paint(g);
        g.translate(0, -labelTop);
    }

    /** The thumb: a flat rectangle; see the class note. */
    public void paintThumb(Graphics g) {
        Rectangle knobBounds = thumbRect;
        g.translate(knobBounds.x, knobBounds.y);
        g.setColor(slider.getForeground());
        g.fillRect(0, 0, knobBounds.width, knobBounds.height);
        g.setColor(getShadowColor());
        g.drawRect(0, 0, knobBounds.width - 1, knobBounds.height - 1);
        g.translate(-knobBounds.x, -knobBounds.y);
    }

    /**
     * Where the labels' text rests.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        if (slider.getPaintLabels() && labelRect.height > 0) {
            return labelRect.y;
        }
        return 0;
    }

    /** The one that follows the mouse over the strip and over the thumb. */
    public class TrackListener extends MouseInputAdapter {

        private final BasicSliderUI ui;
        protected transient int offset;
        protected transient int currentMouseX;
        protected transient int currentMouseY;

        /** The parameter is the look and feel; see finding #518. */
        public TrackListener(BasicSliderUI ui) {
            this.ui = ui;
        }

        public void mousePressed(MouseEvent e) {
            if (!ui.slider.isEnabled()) {
                return;
            }
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            if (ui.slider.isRequestFocusEnabled()) {
                ui.slider.requestFocus();
            }
            if (ui.thumbRect.contains(currentMouseX, currentMouseY)) {
                ui.dragging = true;
                offset = (ui.slider.getOrientation() == SwingConstants.HORIZONTAL)
                        ? (currentMouseX - ui.thumbRect.x)
                        : (currentMouseY - ui.thumbRect.y);
                return;
            }
            ui.dragging = false;
            int direction = currentDirection();
            if (direction != 0) {
                ui.scrollDueToClickInTrack(direction);
                if (ui.scrollTimer != null) {
                    ui.scrollListener.setDirection(direction);
                    ui.scrollTimer.start();
                }
            }
        }

        private int currentDirection() {
            if (ui.slider.getOrientation() == SwingConstants.HORIZONTAL) {
                if (currentMouseX < ui.thumbRect.x) {
                    return ui.drawInverted() ? POSITIVE_SCROLL : NEGATIVE_SCROLL;
                }
                if (currentMouseX > ui.thumbRect.x + ui.thumbRect.width) {
                    return ui.drawInverted() ? NEGATIVE_SCROLL : POSITIVE_SCROLL;
                }
                return 0;
            }
            if (currentMouseY < ui.thumbRect.y) {
                return ui.drawInverted() ? NEGATIVE_SCROLL : POSITIVE_SCROLL;
            }
            if (currentMouseY > ui.thumbRect.y + ui.thumbRect.height) {
                return ui.drawInverted() ? POSITIVE_SCROLL : NEGATIVE_SCROLL;
            }
            return 0;
        }

        public void mouseReleased(MouseEvent e) {
            if (!ui.slider.isEnabled()) {
                return;
            }
            if (ui.scrollTimer != null) {
                ui.scrollTimer.stop();
            }
            ui.dragging = false;
            ui.slider.setValueIsAdjusting(false);
            ui.slider.repaint();
        }

        public void mouseDragged(MouseEvent e) {
            if (!ui.slider.isEnabled() || !ui.dragging) {
                return;
            }
            ui.slider.setValueIsAdjusting(true);
            currentMouseX = e.getX();
            currentMouseY = e.getY();
            if (ui.slider.getOrientation() == SwingConstants.HORIZONTAL) {
                ui.slider.setValue(ui.valueForXPosition(currentMouseX - offset
                        + ui.thumbRect.width / 2));
            } else {
                ui.slider.setValue(ui.valueForYPosition(currentMouseY - offset
                        + ui.thumbRect.height / 2));
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        /** Whether that point falls on the thumb. */
        public boolean shouldScroll(int direction) {
            return true;
        }
    }

    /** The value that corresponds to that pixel; the way back from {@link #xPositionForValue}. */
    int valueForXPosition(int xPos) {
        int trackLength = trackRect.width;
        int trackLeft = trackRect.x;
        int trackRight = trackRect.x + (trackRect.width - 1);
        int minValue = slider.getMinimum();
        int maxValue = slider.getMaximum();
        if (trackLength <= 0) {
            return minValue;
        }
        int value;
        if (xPos <= trackLeft) {
            value = drawInverted() ? maxValue : minValue;
        } else if (xPos >= trackRight) {
            value = drawInverted() ? minValue : maxValue;
        } else {
            int distanceFromTrackLeft = xPos - trackLeft;
            double valueRange = (double) maxValue - (double) minValue;
            double valuePerPixel = valueRange / (double) trackLength;
            int valueFromTrackLeft = (int) Math.round(distanceFromTrackLeft * valuePerPixel);
            value = drawInverted() ? (maxValue - valueFromTrackLeft)
                    : (minValue + valueFromTrackLeft);
        }
        return value;
    }

    /** The same on the other axis. */
    int valueForYPosition(int yPos) {
        int trackLength = trackRect.height;
        int trackTop = trackRect.y;
        int trackBottom = trackRect.y + (trackRect.height - 1);
        int minValue = slider.getMinimum();
        int maxValue = slider.getMaximum();
        if (trackLength <= 0) {
            return minValue;
        }
        int value;
        if (yPos <= trackTop) {
            value = drawInverted() ? minValue : maxValue;
        } else if (yPos >= trackBottom) {
            value = drawInverted() ? maxValue : minValue;
        } else {
            int distanceFromTrackTop = yPos - trackTop;
            double valueRange = (double) maxValue - (double) minValue;
            double valuePerPixel = valueRange / (double) trackLength;
            int valueFromTrackTop = (int) Math.round(distanceFromTrackTop * valuePerPixel);
            value = drawInverted() ? (minValue + valueFromTrackTop)
                    : (maxValue - valueFromTrackTop);
        }
        return value;
    }

    /** The timer that repeats the advance while it is held down; see the class note. */
    public class ScrollListener implements ActionListener {

        private final BasicSliderUI ui;
        private int direction = POSITIVE_SCROLL;
        private boolean useBlockIncrement = true;

        /** The parameter is the look and feel; see finding #518. */
        public ScrollListener(BasicSliderUI ui) {
            this.ui = ui;
        }

        public void setDirection(int direction) {
            this.direction = direction;
        }

        public void setScrollByBlock(boolean block) {
            this.useBlockIncrement = block;
        }

        public void actionPerformed(ActionEvent e) {
            if (useBlockIncrement) {
                ui.scrollByBlock(direction);
            } else {
                ui.scrollByUnit(direction);
            }
        }
    }

    /**
     * The one that listens to the model, the focus, the size and the properties.
     *
     * <p>Static and with the look and feel as a field, for the same reason as everywhere in the
     * package.
     */
    private static class Handler implements ChangeListener, ComponentListener, FocusListener,
            PropertyChangeListener {

        private final BasicSliderUI ui;

        Handler(BasicSliderUI ui) {
            this.ui = ui;
        }

        public void stateChanged(ChangeEvent e) {
            if (!ui.isDragging()) {
                ui.calculateThumbLocation();
                ui.slider.repaint();
            }
        }

        public void componentResized(ComponentEvent e) {
            ui.calculateGeometry();
            ui.slider.repaint();
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }

        public void focusGained(FocusEvent e) {
            ui.slider.repaint();
        }

        public void focusLost(FocusEvent e) {
            ui.slider.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ("orientation".equals(name) || "inverted".equals(name)
                    || "labelTable".equals(name) || "majorTickSpacing".equals(name)
                    || "minorTickSpacing".equals(name) || "paintTicks".equals(name)
                    || "paintLabels".equals(name) || "paintTrack".equals(name)
                    || "font".equals(name) || "componentOrientation".equals(name)) {
                ui.calculateGeometry();
                ui.slider.repaint();
            } else if ("model".equals(name)) {
                Object old = e.getOldValue();
                Object newValue = e.getNewValue();
                if (old instanceof javax.swing.BoundedRangeModel) {
                    ((javax.swing.BoundedRangeModel) old)
                            .removeChangeListener(ui.changeListener);
                }
                if (newValue instanceof javax.swing.BoundedRangeModel) {
                    ((javax.swing.BoundedRangeModel) newValue).addChangeListener(ui.changeListener);
                }
                ui.calculateThumbLocation();
                ui.slider.repaint();
            }
        }
    }

    /** The one {@link #uninstallUI} throws if it is given another component. */
    private static class IllegalComponentStateException extends IllegalStateException {

        IllegalComponentStateException(String s) {
            super(s);
        }
    }
}
