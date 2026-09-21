package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;
import javax.swing.text.View;
import javax.swing.text.WrappedPlainView;

/**
 * The basic look and feel of a multi-line text area.
 *
 * <h2>Two different views according to whether it wraps or not</h2>
 *
 * <p>If the area does not wrap long lines, the view is a {@link PlainView}: each line of the
 * document is a line on the screen and the area becomes as wide as it needs to be. If it wraps,
 * it is a {@link WrappedPlainView}, which splits each line to the available width and therefore
 * has to redo the arithmetic every time the area changes width. Changing {@code lineWrap} on
 * the fly forces the whole view tree to be rebuilt, and {@link #propertyChange} takes care of
 * that.
 *
 * <h2>The caret's pixel</h2>
 *
 * <p>The preferred size and the minimum are the views' <em>plus the caret's width</em>. Without
 * that pixel, the caret standing at the end of the longest line ends up half outside and is not
 * seen. The width comes from the client property {@code caretWidth} and is one if it is not
 * set; it is measured -- with {@code caretWidth} at five, the preferred one grows by four
 * pixels --.
 *
 * <h2>The baseline does not move</h2>
 *
 * <p>Unlike in a field, an area's first line is always at the very top: the baseline is the top
 * margin plus the typeface's ascent, and it does not depend on the height. That is why the
 * behaviour is {@code CONSTANT_ASCENT}, and that is why it answers the same with a height of
 * zero.
 */
public class BasicTextAreaUI extends BasicTextUI {

    public BasicTextAreaUI() {
        super();
    }

    /** A new one per area: a text look and feel keeps the component. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTextAreaUI();
    }

    protected String getPropertyPrefix() {
        return "TextArea";
    }

    /**
     * It adds nothing to what {@link BasicTextUI} installs.
     *
     * <p>It exists as the place where a subclass puts its own without repeating the whole chain;
     * the basic one has nothing of its own to put into an area.
     */
    protected void installDefaults() {
        super.installDefaults();
    }

    /** It rebuilds the views when how the lines are wrapped or how much a tab measures changes. */
    protected void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if ("lineWrap".equals(name) || "wrapStyleWord".equals(name)
                || "tabSize".equals(name)) {
            modelChanged();
        }
    }

    /** The view, according to whether it wraps or not; see the class note. */
    public View create(Element elem) {
        JTextComponent c = getComponent();
        if (c instanceof JTextArea) {
            JTextArea area = (JTextArea) c;
            if (area.getLineWrap()) {
                return new WrappedPlainView(elem, area.getWrapStyleWord());
            }
            return new PlainView(elem);
        }
        return null;
    }

    /** The views' plus the caret's width; see the class note. */
    public Dimension getPreferredSize(JComponent c) {
        return underCursor(c, super.getPreferredSize(c));
    }

    /** The same: in an area the minimum and the preferred come out of the same arithmetic. */
    public Dimension getMinimumSize(JComponent c) {
        return underCursor(c, super.getMinimumSize(c));
    }

    private static Dimension underCursor(JComponent c, Dimension d) {
        if (d == null) {
            return null;
        }
        Object width = c.getClientProperty("caretWidth");
        int px = 1;
        if (width instanceof Number) {
            px = ((Number) width).intValue();
        }
        d.width += px;
        return d;
    }

    /**
     * Top margin plus ascent; see the class note.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        Insets insets = c.getInsets();
        FontMetrics fm = c.getFontMetrics(c.getFont());
        return insets.top + fm.getAscent();
    }

    /**
     * {@code CONSTANT_ASCENT}; see the class note.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }
}
