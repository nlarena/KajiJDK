package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * Metal's slider.
 *
 * <h2>The track is painted up to where the thumb is</h2>
 *
 * <p>{@link #filledSlider} starts at {@code true} and is the visible difference from the basic
 * one: the part of the track already covered is filled in. A program turns it off with the
 * client property {@value #SLIDER_FILL}, and again it is a client property because it is an
 * idea of Metal's and not of {@code JSlider}'s.
 *
 * <h2>A tick's length is not {@code tickLength}</h2>
 *
 * <p>{@link #getTickLength} returns eleven and the field {@link #tickLength} is worth six. The
 * sum is {@code tickLength + TICK_BUFFER + 1}: six of stroke, four of air and one of the track's
 * line. It is the kind of number only understood by measuring, and it is measured.
 *
 * <p>{@link #getTrackLength} may give a negative -- it gives {@code -14} on a slider with no
 * size -- for the same reason as in the basic one: the buffer on each side is subtracted from a
 * width that is still zero. It is measured and is not corrected.
 *
 * <h2>What is said</h2>
 *
 * <p>{@link #thumbColor} and {@link #darkShadowColor} are left at {@code null}, and it is
 * measured: they come from two keys Metal's table does not define. {@link #highlightColor} does
 * have a value.
 */
public class MetalSliderUI extends BasicSliderUI {

    /** The client property that turns the fill off. */
    protected final String SLIDER_FILL = "JSlider.isFilled";

    /** The air between the track and the ticks. */
    protected final int TICK_BUFFER = 4;

    protected static Color thumbColor;
    protected static Color highlightColor;
    protected static Color darkShadowColor;
    protected static int trackWidth = 7;
    protected static int tickLength = 6;
    protected static Icon horizThumbIcon;
    protected static Icon vertThumbIcon;

    protected boolean filledSlider = true;

    public MetalSliderUI() {
        super(null);
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalSliderUI();
    }

    public void installUI(JComponent c) {
        thumbColor = MetalLookAndFeel.tableColor("Slider.thumb");
        highlightColor = MetalLookAndFeel.tableColor("Slider.highlight");
        darkShadowColor = MetalLookAndFeel.tableColor("Slider.darkShadow");
        horizThumbIcon = MetalIconFactory.getHorizontalSliderThumbIcon();
        vertThumbIcon = MetalIconFactory.getVerticalSliderThumbIcon();
        super.installUI(c);
        Object o = c.getClientProperty(SLIDER_FILL);
        if (o instanceof Boolean) {
            filledSlider = ((Boolean) o).booleanValue();
        }
    }

    protected PropertyChangeListener createPropertyChangeListener(JSlider slider) {
        return new MetalPropertyListener();
    }

    /** The thumb icon's, which depends on the orientation. */
    protected Dimension getThumbSize() {
        Icon i = (slider != null && slider.getOrientation() == JSlider.VERTICAL)
                ? vertThumbIcon : horizThumbIcon;
        if (i == null) {
            return super.getThumbSize();
        }
        return new Dimension(i.getIconWidth(), i.getIconHeight());
    }

    protected int getTrackWidth() {
        return trackWidth;
    }

    /** It may give a negative; see the class note. */
    protected int getTrackLength() {
        if (slider != null && slider.getOrientation() == JSlider.HORIZONTAL) {
            return trackRect.width;
        }
        return (trackRect == null) ? 0 : trackRect.height;
    }

    /** How far the thumb sticks out of the track. */
    protected int getThumbOverhang() {
        return (getThumbSize().height - getTrackWidth()) / 2;
    }

    /** Eleven, not six; see the class note. */
    public int getTickLength() {
        return tickLength + TICK_BUFFER + 1;
    }

    protected void scrollDueToClickInTrack(int dir) {
        scrollByBlock(dir);
    }

    public void paintThumb(Graphics g) {
        Icon i = (slider.getOrientation() == JSlider.VERTICAL)
                ? vertThumbIcon : horizThumbIcon;
        if (i != null) {
            i.paintIcon(slider, g, thumbRect.x, thumbRect.y);
        }
    }

    public void paintTrack(Graphics g) {
        Rectangle t = trackRect;
        if (t == null || t.width <= 0 || t.height <= 0) {
            return;
        }
        g.setColor(MetalLookAndFeel.getControlShadow());
        g.fillRect(t.x, t.y, t.width, t.height);
        g.setColor(MetalLookAndFeel.getControlDarkShadow());
        g.drawRect(t.x, t.y, t.width - 1, t.height - 1);
        if (!filledSlider) {
            return;
        }
        // What has been covered, in the primary colour; see the class note.
        g.setColor(MetalLookAndFeel.getPrimaryControlShadow());
        if (slider.getOrientation() == JSlider.HORIZONTAL) {
            int to = thumbRect.x + thumbRect.width / 2 - t.x;
            if (to > 0) {
                g.fillRect(t.x + 1, t.y + 1, Math.min(to, t.width - 2), t.height - 2);
            }
        } else {
            int from = thumbRect.y + thumbRect.height / 2;
            int height = t.y + t.height - from;
            if (height > 0) {
                g.fillRect(t.x + 1, from, t.width - 2, Math.min(height, t.height - 2));
            }
        }
    }

    public void paintFocus(Graphics g) {
        g.setColor(MetalLookAndFeel.getFocusColor());
        g.drawRect(focusRect.x, focusRect.y, focusRect.width - 1, focusRect.height - 1);
    }

    protected void paintMinorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
        g.setColor(MetalLookAndFeel.getControlInfo());
        g.drawLine(x, TICK_BUFFER, x, TICK_BUFFER + tickLength / 2);
    }

    protected void paintMajorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
        g.setColor(MetalLookAndFeel.getControlInfo());
        g.drawLine(x, TICK_BUFFER, x, TICK_BUFFER + tickLength);
    }

    protected void paintMinorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
        g.setColor(MetalLookAndFeel.getControlInfo());
        g.drawLine(TICK_BUFFER, y, TICK_BUFFER + tickLength / 2, y);
    }

    protected void paintMajorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
        g.setColor(MetalLookAndFeel.getControlInfo());
        g.drawLine(TICK_BUFFER, y, TICK_BUFFER + tickLength, y);
    }

    /** The one that watches the fill's client property. */
    private class MetalPropertyListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            if (SLIDER_FILL.equals(e.getPropertyName())) {
                Object v = e.getNewValue();
                filledSlider = !(v instanceof Boolean) || ((Boolean) v).booleanValue();
                if (slider != null) {
                    slider.repaint();
                }
            }
        }
    }
}
