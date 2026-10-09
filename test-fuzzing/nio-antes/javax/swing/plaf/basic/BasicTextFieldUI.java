package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.Element;
import javax.swing.text.FieldView;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

/**
 * The basic look and feel of a single-line text field.
 *
 * <p>Almost all the work is done by {@link BasicTextUI}; what is this class's own are two
 * things: the view it builds -- {@link FieldView}, the only one that knows how to centre a
 * single line vertically and to shift when the text does not fit -- and the baseline.
 *
 * <h2>The baseline moves with the height</h2>
 *
 * <p>A field centres its only line, so if the field grows the line goes down by half of what it
 * grew. That is why the behaviour is {@code CENTER_OFFSET} and not {@code CONSTANT_ASCENT}:
 * whoever lines a field up with a label beside it has to ask again every time the height
 * changes. Measured: with a height of 20 the baseline is at 15 and with 30 at 20.
 *
 * <p>With a height of zero there is nowhere to put anything and the answer is -1.
 */
public class BasicTextFieldUI extends BasicTextUI {

    public BasicTextFieldUI() {
        super();
    }

    /** A new one per field: a text look and feel keeps the component. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTextFieldUI();
    }

    protected String getPropertyPrefix() {
        return "TextField";
    }

    /**
     * The field's view.
     *
     * <p>Always a {@link FieldView}. The JDK has a variant for text that is read right to left
     * mixed with text that is read the other way round; here it is not there, and it is said: a
     * field with bidirectional text is drawn in a single direction.
     */
    public View create(Element elem) {
        return new FieldView(elem);
    }

    /**
     * Where the text rests; see the class note.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        View rootView = getRootView((JTextComponent) c);
        if (rootView.getViewCount() > 0) {
            Insets insets = c.getInsets();
            height = height - insets.top - insets.bottom;
            if (height > 0) {
                int baseline = insets.top;
                View fieldView = rootView.getView(0);
                int vspan = (int) fieldView.getPreferredSpan(View.Y_AXIS);
                if (height != vspan) {
                    // The line goes centred: half of what is left over stays on top.
                    int slop = height - vspan;
                    baseline += slop / 2;
                }
                FontMetrics fm = c.getFontMetrics(c.getFont());
                baseline += fm.getAscent();
                return baseline;
            }
        }
        return -1;
    }

    /**
     * {@code CENTER_OFFSET}; see the class note.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CENTER_OFFSET;
    }
}
