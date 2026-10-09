package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;

import javax.swing.Icon;

/**
 * A menu of the bar, in Synth.
 *
 * <p>A {@code JMenu} is a menu item that also opens another menu, and that is why this class
 * looks so much like {@link SynthMenuItemUI} without inheriting from it: it inherits from
 * {@code BasicMenuUI}, which in turn comes from {@code BasicMenuItemUI}.
 */
public class SynthMenuUI extends javax.swing.plaf.basic.BasicMenuUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthMenuUI();
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
        return new SynthContext(c, (r != null) ? r : Region.MENU, style, state, true);
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
                    .paintMenuBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
                    .paintMenuBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthMenuUI() {
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(menuItem);
    }

    protected void uninstallDefaults() {
        style = null;
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        menuItem.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        menuItem.removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    protected Dimension getPreferredMenuItemSize(JComponent c, Icon checkIcon, Icon arrowIcon,
            int defaultTextIconGap) {
        return super.getPreferredMenuItemSize(c, checkIcon, arrowIcon, defaultTextIconGap);
    }
}
