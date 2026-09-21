package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;

/**
 * Synth's button, and the root of three other classes.
 *
 * <p>{@link SynthToggleButtonUI}, {@link SynthRadioButtonUI} and {@link SynthCheckBoxUI} come
 * from here, in a chain, and each one changes a single thing: the prefix their values are read
 * from. It is the opposite of what happens in Metal, where the button and the toggle share not
 * a line because they inherit from different places; here the chain is well set from the start.
 *
 * <h2>The state is more than on or off</h2>
 *
 * <p>A button is the component where what {@link SynthContext} is for shows most: to the general
 * state -- on, off, focused -- it adds what its model says. Pressed, with the cursor over it,
 * selected, and {@code DEFAULT} if it is the dialog's default button. Each combination may have
 * its own background image, and that is what a look and feel lives on.
 *
 * <h2>Three methods for one icon</h2>
 *
 * <p>{@link #getIcon} is the one that is drawn: the button's if it has one, and otherwise the
 * style's. {@link #getDefaultIcon} is the style's alone. {@link #getSizingIcon} is the one used
 * to <strong>measure</strong>, and it is not always the same as the one drawn: if the icon
 * changes size between states, measuring with the current one would make the button change size
 * when the mouse passed over it.
 */
public class SynthButtonUI extends javax.swing.plaf.basic.BasicButtonUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthButtonUI();
    }

    public SynthContext getContext(JComponent c) {
        return getContext(c, buttonState(c));
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
        return new SynthContext(c, (r != null) ? r : Region.BUTTON, style, state, true);
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
                    .paintButtonBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
                    .paintButtonBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthButtonUI() {
    }

    protected void installDefaults(AbstractButton b) {
        updateStyle(b);
    }

    protected void uninstallDefaults(AbstractButton b) {
        style = null;
    }

    protected void installListeners(AbstractButton b) {
        super.installListeners(b);
        b.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(AbstractButton b) {
        b.removePropertyChangeListener(this);
        super.uninstallListeners(b);
    }

    /** The general state plus whatever the model says; see the class note. */
    private int buttonState(JComponent c) {
        if (!(c instanceof AbstractButton)) {
            return SynthLookAndFeel.stateOf(c);
        }
        ButtonModel m = ((AbstractButton) c).getModel();
        int state = SynthConstants.ENABLED;
        // Pressed is not ADDED to enabled: it replaces it. A pressed button is not also
                // enabled; it is pressed, which is another drawing. Measured: it gives 4 and not 5.
        if (m.isPressed()) {
            state = m.isArmed() ? SynthConstants.PRESSED : SynthConstants.MOUSE_OVER;
        }
        if (m.isRollover()) {
            state |= SynthConstants.MOUSE_OVER;
        }
        if (m.isSelected()) {
            state |= SynthConstants.SELECTED;
        }
        // And disabled overrides everything before it, pressed included. Also measured.
        if (!c.isEnabled()) {
            state = SynthConstants.DISABLED;
        }
        if (c.isFocusOwner()) {
            state |= SynthConstants.FOCUSED;
        }
        if (c instanceof javax.swing.JButton && ((javax.swing.JButton) c).isDefaultButton()) {
            state |= SynthConstants.DEFAULT;
        }
        return state;
    }

    /** The button's, or the style's if the button has none. */
    protected Icon getIcon(AbstractButton b) {
        Icon i = b.getIcon();
        return (i != null) ? i : getDefaultIcon(b);
    }

    /** The style's; null with no style. */
    protected Icon getDefaultIcon(AbstractButton b) {
        SynthContext context = getContext(b);
        if (context.getStyle() == null) {
            return null;
        }
        return context.getStyle().getIcon(context, getPropertyPrefix() + "icon");
    }

    /** The measuring one, which is not always the drawing one; see the class note. */
    protected Icon getSizingIcon(AbstractButton b) {
        Icon i = getDefaultIcon(b);
        return (i != null) ? i : getIcon(b);
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
