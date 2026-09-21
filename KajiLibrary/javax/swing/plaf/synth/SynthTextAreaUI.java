package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;

/**
 * Synth's text area.
 *
 * <p>It does not inherit from the text field -- they are different branches already in the basic
 * one -- but it repeats the same four methods. It is one of the few places where the package
 * duplicates code, and for the same reason as in Metal: the basic look and feel's hierarchy
 * rules.
 */
public class SynthTextAreaUI extends javax.swing.plaf.basic.BasicTextAreaUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthTextAreaUI();
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
        return new SynthContext(c, (r != null) ? r : Region.TEXT_AREA, style, state, true);
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
                    .paintTextAreaBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
                    .paintTextAreaBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthTextAreaUI() {
    }

    protected String getPropertyPrefix() {
        return "TextArea.";
    }

    /**
     * It installs the style's values.
     *
     * <p>The component is asked for with {@code getComponent()} and is not kept: it is the same one
     * the basic one already has, and having it twice would be a copy that can get out of step.
     */
    protected void installDefaults() {
        super.installDefaults();
        JTextComponent c = getComponent();
        if (c != null) {
            updateStyle(c);
            c.addPropertyChangeListener(this);
        }
    }

    protected void uninstallDefaults() {
        JTextComponent c = getComponent();
        if (c != null) {
            c.removePropertyChangeListener(this);
        }
        style = null;
        super.uninstallDefaults();
    }
}
