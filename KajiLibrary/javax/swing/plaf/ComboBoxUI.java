package javax.swing.plaf;

import javax.swing.JComboBox;

/**
 * El aspecto de una {@link JComboBox}.
 *
 * <h2>Solo lo del desplegable</h2>
 *
 * <p>Los tres metodos son sobre la parte que la lista no puede manejar sola: la ventanita que se
 * abre. Abrirla y cerrarla es del aspecto porque depende de si se dibuja adentro de la ventana o en
 * una propia, y eso lo decide el aspecto segun el tamano y lo que haya alrededor.
 */
public abstract class ComboBoxUI extends ComponentUI {

    protected ComboBoxUI() {
    }

    /** Abre o cierra el desplegable. */
    public abstract void setPopupVisible(JComboBox<?> c, boolean v);

    public abstract boolean isPopupVisible(JComboBox<?> c);

    /** Si la lista puede recibir el foco con el tabulador. */
    public abstract boolean isFocusTraversable(JComboBox<?> c);
}
