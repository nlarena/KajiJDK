package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;

import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/**
 * Synth's separator.
 *
 * <p>It is the only class in the package that does <strong>not</strong> inherit from a
 * {@code BasicXxxUI}: it comes straight from {@link javax.swing.plaf.SeparatorUI}. A Synth
 * separator is an image from the style end to end, and it has nothing left to reuse from the
 * basic one -- not even the two relief lines, which are exactly what Synth replaces.
 *
 * <p>That is why the three sizes have to be given by this class, and they come from the style:
 * the key {@code "Separator.thickness"}, with two by default.
 */
public class SynthSeparatorUI extends javax.swing.plaf.SeparatorUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthSeparatorUI();
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
        return new SynthContext(c, (r != null) ? r : Region.SEPARATOR, style, state, true);
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
                    .paintSeparatorBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // The separator is all background; see the class note.
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintSeparatorBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthSeparatorUI() {
    }

    public void installUI(JComponent c) {
        installDefaults((JSeparator) c);
        installListeners((JSeparator) c);
    }

    public void uninstallUI(JComponent c) {
        uninstallListeners((JSeparator) c);
        uninstallDefaults((JSeparator) c);
    }

    public void installDefaults(JSeparator c) {
        updateStyle(c);
    }

    public void uninstallDefaults(JSeparator c) {
        style = null;
    }

    public void installListeners(JSeparator c) {
        c.addPropertyChangeListener(this);
    }

    public void uninstallListeners(JSeparator c) {
        c.removePropertyChangeListener(this);
    }

    /** The thickness comes from the style; two if it does not say. */
    private int thickness(JComponent c) {
        SynthContext context = getContext(c);
        if (context.getStyle() == null) {
            return 2;
        }
        return context.getStyle().getInt(context, "Separator.thickness", 2);
    }

    public Dimension getPreferredSize(JComponent c) {
        int g = thickness(c);
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(g, 0);
        }
        return new Dimension(0, g);
    }

    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** With no cap lengthwise: a separator stretches as much as needed. */
    public Dimension getMaximumSize(JComponent c) {
        int g = thickness(c);
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(g, Integer.MAX_VALUE);
        }
        return new Dimension(Integer.MAX_VALUE, g);
    }
}
