package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's progress bar.
 *
 * <p>The background and the filled part are two different images, and that is the whole
 * difference from the basic one. It is enough for a Synth bar to be able to have texture or a
 * gradient in the filled part, which with a flat colour cannot be done.
 */
public class SynthProgressBarUI extends javax.swing.plaf.basic.BasicProgressBarUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthProgressBarUI();
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
        return new SynthContext(c, (r != null) ? r : Region.PROGRESS_BAR, style, state, true);
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
                    .paintProgressBarBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
     * The text inside the bar -- the "45%" --.
     *
     * <p>It is separate from the rest of the drawing because in Synth a progress bar's text may go
     * in two colours: one over the filled part and another over the empty one. The look and feel
     * solves it by drawing it twice with the clip changed, and that is why a method that draws only
     * the text is needed.
     *
     * @param context the context
     * @param g where to draw
     * @param text the text, or {@code null}
     */
    protected void paintText(SynthContext context, Graphics g, String text) {
        if (text == null || context == null || context.getStyle() == null) {
            return;
        }
        context.getStyle().getGraphicsUtils(context).paintText(
                context, g, text, 0, 0, -1);
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintProgressBarBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthProgressBarUI() {
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(progressBar);
    }

    protected void uninstallDefaults() {
        style = null;
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        progressBar.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        progressBar.removePropertyChangeListener(this);
        super.uninstallListeners();
    }
}
