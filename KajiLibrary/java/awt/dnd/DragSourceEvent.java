package java.awt.dnd;

import java.awt.Point;
import java.util.EventObject;

/**
 * La base de los eventos que le llegan al **origen** de un arrastre.
 *
 * <p>La posición es de **pantalla**, no del componente, y no puede ser de otra manera: el arrastre
 * puede estar pasando por encima de otra ventana o de otro programa, así que no hay ningún
 * componente propio respecto del cual medir.
 *
 * <p>Puede no haber posición: el constructor de un solo argumento arma un evento sin punto, y ahí
 * {@link #getLocation} devuelve `null`. Es lo que corresponde cuando el evento no viene de un
 * movimiento sino del final del arrastre.
 */
public class DragSourceEvent extends EventObject {

    private static final long serialVersionUID = -763287114604032641L;

    private final boolean locationSpecified;
    private final int x;
    private final int y;

    /**
     * Sin posición.
     *
     * @throws IllegalArgumentException si el contexto es `null`
     */
    public DragSourceEvent(DragSourceContext dsc) {
        super(dsc);
        this.locationSpecified = false;
        this.x = 0;
        this.y = 0;
    }

    /**
     * Con la posición en pantalla.
     *
     * @throws IllegalArgumentException si el contexto es `null`
     */
    public DragSourceEvent(DragSourceContext dsc, int x, int y) {
        super(dsc);
        this.locationSpecified = true;
        this.x = x;
        this.y = y;
    }

    /** El contexto del arrastre en curso. */
    public DragSourceContext getDragSourceContext() {
        return (DragSourceContext) this.getSource();
    }

    /**
     * Dónde está el puntero, en coordenadas de pantalla.
     *
     * @return el punto, o `null` si este evento no trae posición
     */
    public Point getLocation() {
        if (this.locationSpecified) {
            return new Point(this.x, this.y);
        }
        return null;
    }

    /** La X en pantalla, o 0 si no hay posición. */
    public int getX() {
        return this.x;
    }

    /** La Y en pantalla, o 0 si no hay posición. */
    public int getY() {
        return this.y;
    }
}
