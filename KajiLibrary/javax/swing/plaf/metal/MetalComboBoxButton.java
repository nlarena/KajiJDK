package javax.swing.plaf.metal;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.CellRendererPane;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * The button that takes up a whole Metal combo box.
 *
 * <p>This class's surprise is that it is <strong>not</strong> the little arrow: it is the whole
 * combo box. A non-editable Metal {@code JComboBox} is a single full-width button that draws the
 * chosen value inside and, on the right, the arrow. That is why it inherits from
 * {@link JButton} and why it has a {@link CellRendererPane} and a {@link JList}: it needs the
 * list's renderer to paint the current value with the same look it will have when it drops
 * down.
 *
 * <p>{@link #isIconOnly} tells the two modes apart. At {@code false} the button is the whole
 * combo box; at {@code true} it is only the little arrow, which is what is needed when the combo
 * box <em>is</em> editable and the text field takes up the rest.
 *
 * <p>It does not take the focus: the combo box takes it, which is the component the program
 * knows.
 */
public class MetalComboBoxButton extends JButton {

    protected JComboBox comboBox;
    protected JList listBox;
    protected CellRendererPane rendererPane;
    protected Icon comboIcon;
    protected boolean iconOnly;

    public MetalComboBoxButton(JComboBox cb, Icon i, CellRendererPane pane, JList list) {
        this(cb, i, false, pane, list);
    }

    public MetalComboBoxButton(JComboBox cb, Icon i, boolean onlyIcon,
            CellRendererPane pane, JList list) {
        super("");
        comboBox = cb;
        comboIcon = i;
        iconOnly = onlyIcon;
        rendererPane = pane;
        listBox = list;
        setModel(new javax.swing.DefaultButtonModel());
        setEnabled(comboBox == null || comboBox.isEnabled());
    }

    public final JComboBox getComboBox() {
        return comboBox;
    }

    public final void setComboBox(JComboBox cb) {
        comboBox = cb;
    }

    public final Icon getComboIcon() {
        return comboIcon;
    }

    public final void setComboIcon(Icon i) {
        comboIcon = i;
    }

    public final boolean isIconOnly() {
        return iconOnly;
    }

    public final void setIconOnly(boolean isIconOnly) {
        iconOnly = isIconOnly;
    }

    /** No; see the class note. */
    public boolean isFocusTraversable() {
        return false;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (comboBox != null) {
            setBackground(comboBox.getBackground());
            setForeground(comboBox.getForeground());
        }
    }

    /** What the arrow plus its air measures; the value's width is set by the combo box. */
    public Dimension getMinimumSize() {
        Insets i = getInsets();
        int width = (comboIcon == null) ? 0 : comboIcon.getIconWidth();
        int height = (comboIcon == null) ? 0 : comboIcon.getIconHeight();
        return new Dimension(width + i.left + i.right, height + i.top + i.bottom);
    }

    /** The chosen value, drawn by the list's renderer, and then the arrow. */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Insets i = getInsets();
        int width = getWidth() - i.left - i.right;
        int height = getHeight() - i.top - i.bottom;
        if (width <= 0 || height <= 0) {
            return;
        }
        int iconWidth = (comboIcon == null) ? 0 : comboIcon.getIconWidth();
        if (comboIcon != null) {
            int ix = i.left + width - iconWidth;
            int iy = i.top + (height - comboIcon.getIconHeight()) / 2;
            comboIcon.paintIcon(this, g, ix, iy);
        }
        if (iconOnly || comboBox == null || rendererPane == null || listBox == null) {
            return;
        }
        paintValue(g, new Rectangle(i.left, i.top, width - iconWidth, height));
    }

    /** The current value, with the list's renderer; see the class note. */
    private void paintValue(Graphics g, Rectangle box) {
        ListCellRenderer renderer = comboBox.getRenderer();
        if (renderer == null) {
            return;
        }
        Object value = comboBox.getSelectedItem();
        java.awt.Component c = renderer.getListCellRendererComponent(
                listBox, value, -1, false, false);
        if (c == null) {
            return;
        }
        c.setFont(comboBox.getFont());
        c.setForeground(comboBox.isEnabled()
                ? comboBox.getForeground()
                : MetalLookAndFeel.getInactiveControlTextColor());
        c.setBackground(comboBox.getBackground());
        rendererPane.paintComponent(g, c, this, box.x, box.y, box.width, box.height, true);
    }
}
