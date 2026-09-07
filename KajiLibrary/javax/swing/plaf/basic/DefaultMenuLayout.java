package javax.swing.plaf.basic;

import java.awt.Container;
import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPopupMenu;
import javax.swing.plaf.UIResource;

/**
 * El acomodador de una barra de menu y de un menu desplegable.
 *
 * <p>Es un {@link BoxLayout} con dos agregados chicos y necesarios.
 *
 * <h2>Un menu vacio no ocupa nada</h2>
 *
 * <p>Un {@link JPopupMenu} sin items mide cero por cero, y no lo que midan sus margenes. Sin eso, un
 * menu contextual que no tiene nada que mostrar apareceria igual como un rectangulito de unos pocos
 * pixeles. Esta medido: una barra de menu vacia mide 0 x 2 --sus margenes-- y un menu desplegable
 * vacio mide 0 x 0.
 *
 * <h2>La columna de los aceleradores</h2>
 *
 * <p>Antes de medir, el menu se olvida del ancho de acelerador mas grande que habia calculado. Ese
 * numero es lo que alinea los {@code Ctrl-O} de todos los items en una columna, y tiene que
 * recalcularse cada vez: si un item cambia de acelerador y el numero quedara viejo, la columna
 * queda torcida o el menu mas ancho de lo que hace falta.
 *
 * <p>Es {@link UIResource} para que instalar otro aspecto lo reemplace; un acomodador que puso el
 * programa se respeta.
 */
public class DefaultMenuLayout extends BoxLayout implements UIResource {

    /** Donde el menu guarda el ancho de acelerador mas grande; ver la nota de la clase. */
    static final String ANCHO_MAXIMO_DEL_ACELERADOR = "maxAccWidth";

    public DefaultMenuLayout(Container target, int axis) {
        super(target, axis);
    }

    /** Ver la nota de la clase. */
    public Dimension preferredLayoutSize(Container target) {
        if (target instanceof JPopupMenu) {
            JPopupMenu popupMenu = (JPopupMenu) target;
            popupMenu.putClientProperty(ANCHO_MAXIMO_DEL_ACELERADOR, null);
            if (popupMenu.getComponentCount() == 0) {
                return new Dimension(0, 0);
            }
        }
        // El BoxLayout guarda los tamanos de los hijos; hay que hacerselos olvidar para que
        // vuelva a preguntar.
        super.invalidateLayout(target);
        return super.preferredLayoutSize(target);
    }
}
