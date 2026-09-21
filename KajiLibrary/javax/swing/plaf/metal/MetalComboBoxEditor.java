package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxEditor;

/**
 * The text field of an editable combo box, in Metal.
 *
 * <p>All that changes from the basic one is the border, and the border has a detail that shows
 * at once when it is missing: the margins are {@code (2,2,2,0)}. <strong>Zero on the
 * right.</strong>
 *
 * <p>The reason is that to the right of the field is the arrow, and the two pieces share a
 * single vertical line. If the field left its pixel of air, a step would be left between the
 * text and the arrow and the combo box would stop reading as a single control.
 *
 * <p>{@link #editorBorderInsets} is {@code protected static}, so a derived look and feel can
 * change it -- and it changes it for every combo box at once, because it is a single shared
 * object.
 */
public class MetalComboBoxEditor extends BasicComboBoxEditor {

    /** Zero on the right; see the class note. */
    protected static Insets editorBorderInsets = new Insets(2, 2, 2, 0);

    public MetalComboBoxEditor() {
        super();
        editor.setBorder(new EditorBorder());
    }

    /** The field's frame, without the side facing the arrow. */
    private static class EditorBorder extends AbstractBorder {

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawLine(x, y, x + w - 1, y);
            g.drawLine(x, y, x, y + h - 2);
            g.drawLine(x, y + h - 2, x + w - 1, y + h - 2);
            g.setColor(MetalLookAndFeel.getControlHighlight());
            g.drawLine(x + 1, y + 1, x + w - 1, y + 1);
            g.drawLine(x + 1, y + 1, x + 1, y + h - 3);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(editorBorderInsets.top, editorBorderInsets.left,
                    editorBorderInsets.bottom, editorBorderInsets.right);
            return insets;
        }
    }

    /**
     * The same editor, marked as set by the look and feel.
     *
     * <p>It exists only for that: an editor that is a {@code UIResource} is replaced by the next
     * look and feel, and one the program set by hand stays. It is the only difference between the
     * two classes.
     */
    public static class UIResource extends MetalComboBoxEditor
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
