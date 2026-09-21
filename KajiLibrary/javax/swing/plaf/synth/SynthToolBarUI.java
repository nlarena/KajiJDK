package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's tool bar.
 *
 * <p>Three regions: the bar, the content and the drag handle. As everywhere in the package,
 * what the basic one solves with colours and borders is here an image per state.
 */
public class SynthToolBarUI extends javax.swing.plaf.basic.BasicToolBarUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthToolBarUI();
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
        return new SynthContext(c, (r != null) ? r : Region.TOOL_BAR, style, state, true);
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
                    .paintToolBarBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        // The buttons draw themselves; see the class note.
    }

    /**
     * The bar's content, in its own region.
     *
     * <p>The bar has two regions and not one: {@code ToolBar} for the whole frame and
     * {@code ToolBarContent} for where the buttons go. They are separate because a bar may have a
     * drag handle, and the handle goes inside the frame but outside the content.
     *
     * @param context the context
     * @param g where to draw
     * @param bounds where it goes
     */
    protected void paintContent(SynthContext context, Graphics g, Rectangle bounds) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context).paintToolBarContentBackground(
                    context, g, bounds.x, bounds.y, bounds.width, bounds.height,
                    toolBar.getOrientation());
        }
    }

    /** The bar's layout; the basic one's is enough. */
    protected java.awt.LayoutManager createLayout() {
        return null;
    }

    /** The border is drawn by the style, not by a {@code Border}; see {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintToolBarBorder(context, g, x, y, w, h);
        }
    }

    /** Any change may want another style; see {@link SynthLookAndFeel#update}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthToolBarUI() {
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(toolBar);
    }

    protected void uninstallDefaults() {
        style = null;
        super.uninstallDefaults();
    }

    protected void installListeners() {
        super.installListeners();
        toolBar.addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        toolBar.removePropertyChangeListener(this);
        super.uninstallListeners();
    }
}
