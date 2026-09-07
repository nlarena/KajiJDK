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
 * El dibujante de los items de un combo: una etiqueta con el texto del valor.
 *
 * <h2>Un solo objeto para todos los items</h2>
 *
 * <p>{@link #getListCellRendererComponent} devuelve {@code this}: se le cambian los colores y el
 * texto y se lo dibuja, item por item. Es el patron de todos los dibujantes de Swing, y es lo que
 * hace que una lista de diez mil items no cree diez mil etiquetas.
 *
 * <h2>El renglon vacio que igual mide</h2>
 *
 * <p>{@link #getPreferredSize} le pone un espacio al texto cuando esta vacio, mide, y lo saca. Sin
 * eso, un combo cuyo item elegido es la cadena vacia mediria cero de alto y se veria como una linea.
 * El truco es del JDK y se copia tal cual; el alto medido de un renglon vacio es 18.
 *
 * <h2>Un valor que es un icono</h2>
 *
 * <p>Si el valor es un {@link Icon} se pone como icono y <em>no</em> se toca el texto. Suena a
 * descuido y no lo es: un combo de iconos con nombre pone el nombre por otro lado, y borrarlo aca lo
 * perderia.
 */
public class BasicComboBoxRenderer extends JLabel implements ListCellRenderer, Serializable {

    /** El borde de un item que no tiene el foco: un pixel de aire por lado. */
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public BasicComboBoxRenderer() {
        super();
        setOpaque(true);
        setBorder(noFocusBorder);
    }

    /** Ver la nota de la clase. */
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

    /** Se prepara y se devuelve a si mismo; ver la nota de la clase. */
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
     * El mismo dibujante, marcado como puesto por el aspecto.
     *
     * <p>La marca es lo que deja que cambiar de aspecto lo reemplace; uno que puso el programa se
     * respeta. Ver {@link javax.swing.plaf.UIResource}.
     */
    public static class UIResource extends BasicComboBoxRenderer
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
