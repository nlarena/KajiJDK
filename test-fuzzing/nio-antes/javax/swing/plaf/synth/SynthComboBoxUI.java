package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.ComboPopup;

/**
 * Synth's combo box.
 *
 * <p>{@link #getDefaultSize} exists for a concrete problem: an empty combo box has nowhere to
 * get its height from. The basic one solves it by measuring a fake item; Synth takes it from the
 * style, which is where it belongs.
 */
public class SynthComboBoxUI extends javax.swing.plaf.basic.BasicComboBoxUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthComboBoxUI();
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
        return new SynthContext(c, (r != null) ? r : Region.COMBO_BOX, style, state, true);
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
                    .paintComboBoxBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // The value and the arrow draw themselves.
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintComboBoxBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthComboBoxUI() {
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        c.addPropertyChangeListener(this);
    }

    public void uninstallUI(JComponent c) {
        c.removePropertyChangeListener(this);
        super.uninstallUI(c);
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(comboBox);
    }

    protected void uninstallDefaults() {
        style = null;
        super.uninstallDefaults();
    }

    protected JButton createArrowButton() {
        return super.createArrowButton();
    }

    protected ComboPopup createPopup() {
        return super.createPopup();
    }

    protected ListCellRenderer createRenderer() {
        return super.createRenderer();
    }

    /** An empty combo box's height; see the class note. */
    protected Dimension getDefaultSize() {
        return new Dimension(0, 0);
    }

    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        super.paintCurrentValue(g, bounds, hasFocus);
    }
}
