package javax.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Dibuja una celda de tabla como una etiqueta.
 *
 * <h2>Un solo componente para todas las celdas</h2>
 *
 * <p>Igual que en el arbol: la tabla no tiene un componente por celda, tiene <em>este</em>, y lo
 * configura y lo dibuja una vez por celda. De ahi que {@link #revalidate}, {@link #repaint},
 * {@link #invalidate} y casi todos los {@code firePropertyChange} esten <strong>vaciados a
 * proposito</strong>: un dibujante que pide repintarse mientras lo estan dibujando dejaria la tabla
 * en bucle.
 *
 * <h2>Los colores no se guardan, se turnan</h2>
 *
 * <p>Cada celda pide su color a la tabla -- de seleccion o normal -- y el dibujante lo aplica. Pero
 * si alguien le puso un color <em>a mano</em> al dibujante, ese gana: por eso los colores que vienen
 * del aspecto se marcan y se distinguen de los que puso el programa.
 *
 * <h2>La celda con el foco lleva borde</h2>
 *
 * <p>Y las demas llevan un borde vacio del mismo tamano, no ninguno. Si no, la celda enfocada
 * mediria distinto que las otras y el texto saltaria un pixel al moverse el foco.
 */
public class DefaultTableCellRenderer extends JLabel implements TableCellRenderer,
        java.io.Serializable {

    /** El borde sin foco: vacio, pero del mismo tamano que el otro. Ver la nota de la clase. */
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    private static final Border SIN_FOCO = new EmptyBorder(1, 1, 1, 1);

    private Color unselectedForeground;
    private Color unselectedBackground;

    /** Un dibujante alineado a la izquierda y opaco. */
    public DefaultTableCellRenderer() {
        super();
        setOpaque(true);
        setBorder(getNoFocusBorder());
        setName("Table.cellRenderer");
    }

    private Border getNoFocusBorder() {
        Border border = UIManager.getBorder("Table.cellNoFocusBorder");
        if (border != null) {
            return border;
        }
        return SIN_FOCO;
    }

    /**
     * El color del texto cuando la celda no esta elegida.
     *
     * <p>Nulo devuelve la decision a la tabla; ver la nota de la clase.
     */
    public void setForeground(Color c) {
        super.setForeground(c);
        unselectedForeground = c;
    }

    /** El fondo cuando la celda no esta elegida; nulo se lo devuelve a la tabla. */
    public void setBackground(Color c) {
        super.setBackground(c);
        unselectedBackground = c;
    }

    /** Vuelve a pedirle los colores al aspecto y olvida los puestos a mano. */
    public void updateUI() {
        super.updateUI();
        setForeground(null);
        setBackground(null);
    }

    /**
     * Se configura para dibujar esa celda y se devuelve a si mismo.
     *
     * <p>El texto sale de {@link #setValue}, que una subclase puede cambiar para formatear numeros
     * o fechas sin tocar nada mas.
     */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if (table == null) {
            return this;
        }
        if (isSelected) {
            super.setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
        } else {
            Color background = (unselectedBackground != null) ? unselectedBackground
                    : table.getBackground();
            super.setForeground((unselectedForeground != null) ? unselectedForeground
                    : table.getForeground());
            super.setBackground(background);
        }
        setFont(table.getFont());
        if (hasFocus) {
            Border border = UIManager.getBorder("Table.focusCellHighlightBorder");
            if (border == null) {
                border = SIN_FOCO;
            }
            setBorder(border);
            if (!isSelected && table.isCellEditable(row, column)) {
                Color col = UIManager.getColor("Table.focusCellForeground");
                if (col != null) {
                    super.setForeground(col);
                }
                col = UIManager.getColor("Table.focusCellBackground");
                if (col != null) {
                    super.setBackground(col);
                }
            }
        } else {
            setBorder(getNoFocusBorder());
        }
        setValue(value);
        return this;
    }

    /**
     * Pone el valor como texto.
     *
     * <p>Es el punto de extension de esta clase: una subclase que quiera mostrar un importe con dos
     * decimales cambia esto y nada mas.
     */
    protected void setValue(Object value) {
        setText((value == null) ? "" : value.toString());
    }

    /** No hace nada; ver la nota de la clase. */
    public void invalidate() {
    }

    /** No hace nada; ver la nota de la clase. */
    public void validate() {
    }

    /** No hace nada; ver la nota de la clase. */
    public void revalidate() {
    }

    /** No hace nada; ver la nota de la clase. */
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void repaint(Rectangle r) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void repaint() {
    }

    /**
     * Solo deja pasar el aviso de que cambio el texto.
     *
     * <p>El JDK deja pasar tambien la tipografia y el color cuando el texto es HTML; esa rama pide
     * {@code BasicHTML}, que esta biblioteca no trae.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyName == "text") {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }

    /**
     * Un dibujante que ademas es un recurso del aspecto.
     *
     * <p>Marcarlo asi es como el aspecto dice "este lo puse yo": al cambiar de aspecto se lo
     * reemplaza, y uno puesto por el programa se conserva.
     */
    public static class UIResource extends DefaultTableCellRenderer
            implements javax.swing.plaf.UIResource {

        /** Un dibujante de base marcado como del aspecto. */
        public UIResource() {
            super();
        }
    }
}
