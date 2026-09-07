package javax.swing.plaf;

import javax.swing.JOptionPane;

/**
 * El aspecto de un {@link JOptionPane}.
 *
 * <h2>Dos metodos nada mas</h2>
 *
 * <p>Casi todo lo que hace un panel de opciones -- armar los botones, medir el mensaje, elegir el
 * icono -- lo resuelve el aspecto desde {@code installUI}. Lo unico que hace falta pedirle despues
 * son estas dos cosas, y las dos existen por el mismo motivo: el panel no sabe que componentes
 * armo el aspecto.
 *
 * <p>{@link #selectInitialValue} le da el foco al boton que corresponde -- el panel no tiene los
 * botones, los tiene el aspecto. {@link #containsCustomComponents} dice si el mensaje trajo
 * componentes propios, que es lo que decide si al cerrar hay que sacarlos para que se los pueda
 * reusar.
 */
public abstract class OptionPaneUI extends ComponentUI {

    /** Para las subclases. */
    protected OptionPaneUI() {
    }

    /** Le da el foco al valor inicial; ver la nota de la clase. */
    public abstract void selectInitialValue(JOptionPane op);

    /** Si el mensaje trajo componentes propios; ver la nota de la clase. */
    public abstract boolean containsCustomComponents(JOptionPane op);
}
