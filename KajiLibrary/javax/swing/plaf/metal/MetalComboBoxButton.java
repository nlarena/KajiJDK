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
 * El boton que ocupa todo un desplegable de Metal.
 *
 * <p>La sorpresa de esta clase es que <strong>no</strong> es la flechita: es el desplegable entero.
 * Un {@code JComboBox} no editable de Metal es un solo boton del ancho completo que dibuja adentro
 * el valor elegido y, a la derecha, la flecha. Por eso hereda de {@link JButton} y por eso tiene
 * una {@link CellRendererPane} y una {@link JList}: necesita el dibujante de la lista para pintar
 * el valor actual con el mismo aspecto que va a tener cuando se despliegue.
 *
 * <p>{@link #isIconOnly} distingue los dos modos. En {@code false} el boton es todo el desplegable;
 * en {@code true} es solo la flechita, que es lo que hace falta cuando el desplegable <em>es</em>
 * editable y el campo de texto ocupa el resto.
 *
 * <p>No toma el foco: lo toma el desplegable, que es el componente que el programa conoce.
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

    /** No; ver la nota de la clase. */
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

    /** Lo que mide la flecha mas su aire; el ancho del valor lo pone el desplegable. */
    public Dimension getMinimumSize() {
        Insets i = getInsets();
        int ancho = (comboIcon == null) ? 0 : comboIcon.getIconWidth();
        int alto = (comboIcon == null) ? 0 : comboIcon.getIconHeight();
        return new Dimension(ancho + i.left + i.right, alto + i.top + i.bottom);
    }

    /** El valor elegido, dibujado por el dibujante de la lista, y despues la flecha. */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Insets i = getInsets();
        int ancho = getWidth() - i.left - i.right;
        int alto = getHeight() - i.top - i.bottom;
        if (ancho <= 0 || alto <= 0) {
            return;
        }
        int anchoIcono = (comboIcon == null) ? 0 : comboIcon.getIconWidth();
        if (comboIcon != null) {
            int ix = i.left + ancho - anchoIcono;
            int iy = i.top + (alto - comboIcon.getIconHeight()) / 2;
            comboIcon.paintIcon(this, g, ix, iy);
        }
        if (iconOnly || comboBox == null || rendererPane == null || listBox == null) {
            return;
        }
        dibujarValor(g, new Rectangle(i.left, i.top, ancho - anchoIcono, alto));
    }

    /** El valor actual, con el dibujante de la lista; ver la nota de la clase. */
    private void dibujarValor(Graphics g, Rectangle caja) {
        ListCellRenderer dibujante = comboBox.getRenderer();
        if (dibujante == null) {
            return;
        }
        Object valor = comboBox.getSelectedItem();
        java.awt.Component c = dibujante.getListCellRendererComponent(
                listBox, valor, -1, false, false);
        if (c == null) {
            return;
        }
        c.setFont(comboBox.getFont());
        c.setForeground(comboBox.isEnabled()
                ? comboBox.getForeground()
                : MetalLookAndFeel.getInactiveControlTextColor());
        c.setBackground(comboBox.getBackground());
        rendererPane.paintComponent(g, c, this, caja.x, caja.y, caja.width, caja.height, true);
    }
}
