package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Dimension;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * The renderer of a combo box's items: a label with the value's text.
 *
 * <h2>A single object for every item</h2>
 *
 * <p>{@link #getListCellRendererComponent} returns {@code this}: its colours and its text are
 * changed and it is drawn, item by item. It is the pattern of every renderer in Swing, and it
 * is what makes a list of ten thousand items not create ten thousand labels.
 *
 * <h2>The empty line that measures all the same</h2>
 *
 * <p>{@link #getPreferredSize} puts a space into the text when it is empty, measures, and takes
 * it out. Without that, a combo box whose chosen item is the empty string would measure zero in
 * height and would look like a line. The trick is the JDK's and it is copied as it is; the
 * measured height of an empty line is 18.
 *
 * <h2>A value that is an icon</h2>
 *
 * <p>If the value is an {@link Icon} it is set as the icon and the text is <em>not</em> touched.
 * It sounds like an oversight and it is not: a combo box of named icons puts the name somewhere
 * else, and erasing it here would lose it.
 */
public class BasicComboBoxRenderer extends JLabel implements ListCellRenderer, Serializable {

    /** The border of an item that does not have the focus: a pixel of air on each side. */
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public BasicComboBoxRenderer() {
        super();
        setOpaque(true);
        setBorder(noFocusBorder);
    }

    /** See the class note. */
    public Dimension getPreferredSize() {
        Dimension size;
        if ((this.getText() == null) || (this.getText().equals(""))) {
            setText(" ");
            size = super.getPreferredSize();
            setText("");
        } else {
            size = super.getPreferredSize();
        }
        return size;
    }

    /** It gets itself ready and returns itself; see the class note. */
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        setFont(list.getFont());

        if (value instanceof Icon) {
            setIcon((Icon) value);
        } else {
            setText((value == null) ? "" : value.toString());
        }
        return this;
    }

    /**
     * The same renderer, marked as set by the look and feel.
     *
     * <p>The mark is what lets changing the look and feel replace it; one the program set is
     * respected. See {@link javax.swing.plaf.UIResource}.
     */
    public static class UIResource extends BasicComboBoxRenderer
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
