package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.JPanel;

/**
 * Synth's panel.
 *
 * <p>A panel draws nothing of its own except the background, so here the separation between
 * background and content is in plain sight: {@code paint(SynthContext, Graphics)} does nothing,
 * and everything that is seen was painted by {@code update}.
 */
public class SynthPanelUI extends javax.swing.plaf.basic.BasicPanelUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthPanelUI();
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
        return new SynthContext(c, (r != null) ? r : Region.PANEL, style, state, true);
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
                    .paintPanelBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // A panel has no content of its own; see the class note.
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintPanelBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthPanelUI() {
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    protected void installDefaults(JPanel p) {
        updateStyle(p);
    }

    protected void uninstallDefaults(JPanel p) {
        style = null;
    }

    protected void installListeners(JPanel p) {
        p.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(JPanel p) {
        p.removePropertyChangeListener(this);
    }
}
