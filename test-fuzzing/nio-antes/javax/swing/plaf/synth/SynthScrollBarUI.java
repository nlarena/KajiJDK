package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Synth's scroll bar.
 *
 * <p>It is the component with the most regions in the whole package: {@code ScrollBar} for the
 * whole bar, {@code ScrollBarTrack} for the track and {@code ScrollBarThumb} for the thumb.
 * Three different images for a single thing.
 *
 * <p>That the track and the thumb are regions and not colours is what allows the look no other
 * package can give: a thumb with rounded tips and a relief in the middle, drawn as an image and
 * not as four lines.
 */
public class SynthScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthScrollBarUI();
    }

    public SynthContext getContext(JComponent c) {
        return getContext(c, SynthLookAndFeel.stateOf(c));
    }

    /**
     * The context with that state.
     *
     * <p>The region comes from the component and not from a fixed constant, and that matters in the
     * chains of inheritance: {@code SynthCheckBoxUI} inherits this method from {@code
     * SynthButtonUI} and has to answer {@code CheckBox}, not {@code Button}. Measured.
     */
    private SynthContext getContext(JComponent c, int state) {
        Region r = SynthLookAndFeel.getRegion(c);
        return new SynthContext(c, (r != null) ? r : Region.SCROLL_BAR, style, state, true);
    }

    /** It asks the factory for the style; it blows up if there is none, and it is measured. */
    private void updateStyle(JComponent c) {
        style = SynthLookAndFeel.update(getContext(c, SynthConstants.ENABLED));
    }

    /**
     * It draws the background and then the content.
     *
     * <p>Synth separates the two things: the background is painted by the style -- which knows what
     * state the component is in -- and the content is painted by the basic look and feel. That is
     * why {@code update} is not {@code paint} with a fill in front, as in the basic one, but two
     * different steps.
     */
    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);
        if (context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintScrollBarBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        super.paint(g, context.getComponent());
    }

    /**
     * The track, in its own region.
     *
     * <p>The signature takes the context and does not ask for it: the caller already has it, and
     * building it again would mean asking the component for its state again in the middle of a
     * drawing.
     *
     * @param context the track's context
     * @param g where to draw
     * @param trackBounds where it goes
     */
    protected void paintTrack(SynthContext context, Graphics g, Rectangle trackBounds) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context).paintScrollBarTrackBackground(
                    context, g, trackBounds.x, trackBounds.y,
                    trackBounds.width, trackBounds.height);
        }
    }

    /**
     * The thumb, in its own.
     *
     * @param context the thumb's context
     * @param g where to draw
     * @param thumbBounds where it goes
     */
    protected void paintThumb(SynthContext context, Graphics g, Rectangle thumbBounds) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context).paintScrollBarThumbBackground(
                    context, g, thumbBounds.x, thumbBounds.y,
                    thumbBounds.width, thumbBounds.height,
                    scrollbar.getOrientation());
        }
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintScrollBarBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthScrollBarUI() {
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(scrollbar);
    }

    protected void uninstallDefaults() {
        style = null;
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        scrollbar.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        scrollbar.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    public Dimension getPreferredSize(JComponent c) {
        return super.getPreferredSize(c);
    }

    public Dimension getMinimumSize(JComponent c) {
        return super.getMinimumSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return super.getMaximumSize(c);
    }
}
