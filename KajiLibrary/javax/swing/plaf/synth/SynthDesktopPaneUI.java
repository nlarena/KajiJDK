package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's desktop, where the internal frames live.
 *
 * <p>It adds two methods no other class in the package has: {@link #installDesktopManager} and
 * {@link #uninstallDesktopManager}. The desktop manager is who decides what happens when an
 * internal frame is iconified, maximized or closed, and Synth separates it from the rest of the
 * installation because a look and feel may want to change that behaviour without touching any
 * of the drawing.
 */
public class SynthDesktopPaneUI extends javax.swing.plaf.basic.BasicDesktopPaneUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthDesktopPaneUI();
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
        return new SynthContext(c, (r != null) ? r : Region.DESKTOP_PANE, style, state, true);
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
                    .paintDesktopPaneBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // The desktop is all background; see the class note.
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintDesktopPaneBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthDesktopPaneUI() {
    }

    protected void installDefaults() {
        updateStyle(desktop);
    }

    protected void uninstallDefaults() {
        style = null;
    }

    protected void installListeners() {
        super.installListeners();
        desktop.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        desktop.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    /** Who handles iconifying, maximizing and closing; see the class note. */
    protected void installDesktopManager() {
        super.installDesktopManager();
    }

    protected void uninstallDesktopManager() {
        super.uninstallDesktopManager();
    }
}
