package javax.swing.plaf;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JList;

/**
 * El aspecto de una {@link JList}.
 *
 * <h2>Tres preguntas de geometria</h2>
 *
 * <p>Todo lo que la lista no puede contestar sola porque depende de como se dibuja: que renglon cae
 * en un punto, donde empieza un renglon, y cuanto ocupan varios. La lista se las reenvia al
 * aspecto y no las calcula, porque el alto de un renglon lo decide el dibujante y el acomodado en
 * columnas lo decide el aspecto.
 */
public abstract class ListUI extends ComponentUI {

    protected ListUI() {
    }

    /** Que renglon cae en ese punto, o -1. */
    public abstract int locationToIndex(JList<?> list, Point location);

    /** La esquina de arriba a la izquierda de ese renglon, o nulo. */
    public abstract Point indexToLocation(JList<?> list, int index);

    /** El rectangulo que ocupan los renglones entre esos dos indices. */
    public abstract Rectangle getCellBounds(JList<?> list, int index1, int index2);
}
