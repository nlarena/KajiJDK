package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;

import javax.swing.Icon;

/**
 * Synth's menu item, and the root of three others.
 *
 * <p>{@link SynthCheckBoxMenuItemUI}, {@link SynthRadioButtonMenuItemUI} and, along another
 * branch, {@link SynthMenuUI} come from here. The first two change only the prefix.
 *
 * <p>It redefines {@link #getPreferredMenuItemSize} so that the measurement goes through
 * {@link SynthGraphicsUtils}: in Synth, an item's width -- text, icon, accelerator, arrow -- is
 * computed by the style, because it is the one that knows how much it separates each thing.
 */
public class SynthMenuItemUI extends javax.swing.plaf.basic.BasicMenuItemUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthMenuItemUI();
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
        return new SynthContext(c, (r != null) ? r : Region.MENU_ITEM, style, state, true);
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
                    .paintMenuItemBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
                    .paintMenuItemBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthMenuItemUI() {
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

    /** The measurement goes through the style; see the class note. */
    protected Dimension getPreferredMenuItemSize(JComponent c, Icon checkIcon, Icon arrowIcon,
            int defaultTextIconGap) {
        return super.getPreferredMenuItemSize(c, checkIcon, arrowIcon, defaultTextIconGap);
    }
}
