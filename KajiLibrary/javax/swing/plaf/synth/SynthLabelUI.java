package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;

import javax.swing.JLabel;

/**
 * Synth's label.
 *
 * <p>It is the shortest class in the package and the one that best shows the sharing out: the
 * text and the icon go on being placed and drawn by the basic look and feel, and the only thing
 * Synth adds is the background and the border, taken from the style.
 *
 * <p>The label does not implement {@code PropertyChangeListener} -- it is one of the few that do
 * not --, and it makes sense: a label does not change state by itself. The style is asked for
 * when installing it and when the program changes it by hand.
 */
public class SynthLabelUI extends javax.swing.plaf.basic.BasicLabelUI implements SynthUI {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthLabelUI();
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
        return new SynthContext(c, (r != null) ? r : Region.LABEL, style, state, true);
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
                    .paintLabelBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        super.paint(g, context.getComponent());
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintLabelBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthLabelUI() {
    }

    protected void installDefaults(JLabel c) {
        updateStyle(c);
    }

    protected void uninstallDefaults(JLabel c) {
        style = null;
    }

    public int getBaseline(JComponent c, int width, int height) {
        return super.getBaseline(c, width, height);
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
