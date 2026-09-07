package javax.swing.text;

import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Una accion que trabaja sobre el componente de texto que la disparo.
 *
 * <h2>Por que no guarda el componente</h2>
 *
 * <p>Una accion de edicion —"borrar la palabra anterior"— vale para cualquier componente de texto,
 * y ponerla en un menu compartido significa que no se sabe de antemano sobre cual va a actuar. Por
 * eso {@link #getTextComponent} lo saca del evento: el que disparo la accion.
 *
 * <p>Cuando el evento no dice de donde vino —una accion invocada desde codigo—, se cae en
 * {@link #getFocusedComponent}, que en esta VM devuelve {@code null} porque no hay foco de
 * teclado.
 */
public abstract class TextAction extends AbstractAction {

    /** Una accion con ese nombre; el nombre es lo que la ata a una tecla. */
    public TextAction(String name) {
        super(name);
    }

    /** El componente sobre el que hay que actuar; ver la nota de la clase. */
    protected final JTextComponent getTextComponent(ActionEvent e) {
        if (e != null) {
            Object o = e.getSource();
            if (o instanceof JTextComponent) {
                return (JTextComponent) o;
            }
        }
        return getFocusedComponent();
    }

    /**
     * Junta dos listas de acciones, ganando la segunda.
     *
     * <p>Es como un juego de edicion agrega las suyas a las que hereda sin repetir: dos acciones
     * con el mismo nombre son la misma, y queda la de la lista de abajo.
     */
    public static final Action[] augmentList(Action[] list1, Action[] list2) {
        Hashtable<String, Action> h = new Hashtable<String, Action>();
        for (int i = 0; i < list1.length; i++) {
            Action a = list1[i];
            String value = (String) a.getValue(Action.NAME);
            h.put((value != null ? value : ""), a);
        }
        for (int i = 0; i < list2.length; i++) {
            Action a = list2[i];
            String value = (String) a.getValue(Action.NAME);
            h.put((value != null ? value : ""), a);
        }
        Action[] actions = new Action[h.size()];
        int index = 0;
        for (java.util.Enumeration<Action> e = h.elements(); e.hasMoreElements();) {
            actions[index] = e.nextElement();
            index = index + 1;
        }
        return actions;
    }

    /** {@code null}: esta VM no tiene foco de teclado; ver la nota de la clase. */
    protected final JTextComponent getFocusedComponent() {
        return JTextComponent.getFocusedComponent();
    }
}
