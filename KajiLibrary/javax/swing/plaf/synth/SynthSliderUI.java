package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's slider.
 *
 * <p>Three regions, like the scroll bar: {@code Slider}, {@code SliderTrack} and
 * {@code SliderThumb}. And one turn more: a Synth slider's thumb may have a <strong>different
 * size per state</strong>, because its icon is a {@link SynthIcon}. A thumb that grows when the
 * mouse passes over it is something the basic one cannot express.
 */
public class SynthSliderUI extends javax.swing.plaf.basic.BasicSliderUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthSliderUI((javax.swing.JSlider) c);
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
        return new SynthContext(c, (r != null) ? r : Region.SLIDER, style, state, true);
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
                    .paintSliderBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
     * @param context the track's context
     * @param g where to draw
     * @param trackBounds where it goes
     */
    protected void paintTrack(SynthContext context, Graphics g, Rectangle trackBounds) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context).paintSliderTrackBackground(
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
            context.getStyle().getPainter(context).paintSliderThumbBackground(
                    context, g, thumbBounds.x, thumbBounds.y,
                    thumbBounds.width, thumbBounds.height,
                    slider.getOrientation());
        }
    }

    /**
     * It redoes the six rectangles.
     *
     * <p>It is called by itself every time it is needed and is not part of installing anything:
     * Synth's thumb may change size when it changes state, so the layout cannot be computed once.
     */
    protected void layout() {
        calculateGeometry();
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintSliderBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    /**
     * One for that slider.
     *
     * <p>It is the only constructor and it is {@code protected}: who installs is
     * {@link #createUI}. There is no no-argument version, and it is not an oversight -- a Synth
     * slider needs the component from the start in order to be able to ask it for its state --.
     * Measured.
     *
     * @param c the slider
     */
    protected SynthSliderUI(javax.swing.JSlider c) {
        super(c);
    }

    protected void installDefaults(javax.swing.JSlider s) {
        super.installDefaults(s);
        updateStyle(s);
    }

    protected void uninstallDefaults(javax.swing.JSlider s) {
        style = null;
        super.uninstallDefaults(s);
    }

    protected void installListeners(javax.swing.JSlider s) {
        super.installListeners(s);
        s.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(javax.swing.JSlider s) {
        s.removePropertyChangeListener(this);
        super.uninstallListeners(s);
    }
}
