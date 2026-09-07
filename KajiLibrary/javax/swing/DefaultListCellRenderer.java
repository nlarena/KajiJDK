package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.io.Serializable;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * El dibujante de renglones de siempre: una etiqueta con el {@code toString} del elemento.
 *
 * <h2>Una etiqueta que no es un componente de verdad</h2>
 *
 * <p>Hereda de {@link JLabel} y sobrescribe todos los metodos de repintado y de aviso para que no
 * hagan nada. No es una optimizacion menor: este objeto se usa como sello, una vez por renglon
 * visible y muchas veces por segundo. Si cada cambio de texto disparara un aviso de propiedad y una
 * peticion de repintado, dibujar una lista de veinte renglones costaria cuarenta avisos inutiles.
 *
 * <p>Es tambien la razon de que no se lo pueda usar como un componente comun: puesto en una
 * ventana, no se repintaria nunca.
 *
 * <h2>El borde del foco</h2>
 *
 * <p>El renglon con el foco lleva un borde y los demas uno vacio del mismo tamano. Que el vacio
 * tenga el mismo tamano es lo que evita que el texto se mueva un pixel al pasar el foco de un
 * renglon a otro.
 */
public class DefaultListCellRenderer extends JLabel implements ListCellRenderer<Object>,
        Serializable {

    /**
     * El borde de los renglones sin foco.
     *
     * @deprecated Es compartido entre todos los dibujantes; cambiarlo cambia el de todos.
     */
    @Deprecated
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    private static final Border SIN_FOCO = new EmptyBorder(1, 1, 1, 1);

    /** Un dibujante alineado a la izquierda y opaco. */
    public DefaultListCellRenderer() {
        super();
        setOpaque(true);
        setBorder(getNoFocusBorder());
        setName("List.cellRenderer");
    }

    private Border getNoFocusBorder() {
        return (noFocusBorder != null) ? noFocusBorder : SIN_FOCO;
    }

    /** Prepara la etiqueta para ese renglon y la devuelve. */
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        setComponentOrientation(list.getComponentOrientation());

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (value instanceof Icon) {
            setIcon((Icon) value);
            setText("");
        } else {
            setIcon(null);
            setText((value == null) ? "" : value.toString());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBorder(getNoFocusBorder());
        return this;
    }

    public boolean isOpaque() {
        Color back = getBackground();
        Component p = getParent();
        if (p != null) {
            p = p.getParent();
        }
        // Con el mismo fondo que la lista y sin nada que ocultar, no vale la pena rellenar.
        boolean colorMatch = (back != null) && (p != null) && back.equals(p.getBackground())
                && p.isOpaque();
        return !colorMatch && super.isOpaque();
    }

    /** No hace nada; ver la nota de la clase. */
    public void validate() {
    }

    public void invalidate() {
    }

    public void repaint() {
    }

    public void revalidate() {
    }

    public void repaint(long tm, int x, int y, int width, int height) {
    }

    public void repaint(Rectangle r) {
    }

    /**
     * Solo deja pasar el aviso del texto.
     *
     * <p>Es el unico que alguien puede necesitar escuchar de un dibujante, y dejar pasar los demas
     * costaria un aviso por propiedad y por renglon.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        // El JDK deja pasar tambien "font" y "foreground" cuando el texto es HTML, para que la
        // vista de HTML se rearme. Aca no llega ese caso: `BasicHTML` todavia no existe.
        if (propertyName == "text") {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
    }

    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
    }

    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
    }

    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
    }

    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
    }

    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }

    /**
     * El mismo dibujante, marcado como puesto por el aspecto.
     *
     * <p>La marca es lo que permite que cambiar de aspecto reemplace este dibujante y respete uno
     * que haya puesto el programa; ver {@link javax.swing.plaf.UIResource}.
     */
    public static class UIResource extends DefaultListCellRenderer
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
