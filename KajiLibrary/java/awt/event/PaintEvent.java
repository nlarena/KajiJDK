package java.awt.event;

import java.awt.Component;
import java.awt.Rectangle;

/**
 * Hay que repintar una parte de un componente.
 *
 * <p>Es el único evento de AWT que **no se le entrega a un oyente**: no hay `PaintListener`. Va
 * directo a `Component.paint` o a `Component.update`, y por eso está en el paquete de eventos pero
 * no participa del modelo de oyentes.
 *
 * <p>El rectángulo a actualizar es lo que hace que repintar sea barato: en vez de redibujar el
 * componente entero cuando una ventana lo destapa, se redibuja el pedazo que se destapó.
 */
public class PaintEvent extends ComponentEvent {

    private static final long serialVersionUID = 1267492026433337593L;

    /** Hay que pintar, empezando por borrar el fondo. */
    public static final int PAINT = 800;

    /** El primer identificador de la familia. */
    public static final int PAINT_FIRST = 800;

    /** El último identificador de la familia. */
    public static final int PAINT_LAST = 801;

    /** Hay que actualizar, sin borrar el fondo. */
    public static final int UPDATE = 801;

    private Rectangle updateRect;

    /**
     * Con el componente, el identificador y el rectángulo.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public PaintEvent(Component source, int id, Rectangle updateRect) {
        super(source, id);
        this.updateRect = updateRect;
    }

    /** Qué parte hay que repintar. */
    public Rectangle getUpdateRect() {
        return this.updateRect;
    }

    /**
     * Cambia qué parte hay que repintar.
     *
     * <p>Sirve para juntar varios pedidos en uno: el sistema agranda el rectángulo en vez de
     * encolar dos eventos.
     */
    public void setUpdateRect(Rectangle updateRect) {
        this.updateRect = updateRect;
    }

    public String paramString() {
        String tipo;
        if (this.id == PAINT) {
            tipo = "PAINT";
        } else if (this.id == UPDATE) {
            tipo = "UPDATE";
        } else {
            tipo = "unknown type";
        }
        return tipo + ",updateRect=" + this.updateRect;
    }
}
