package java.awt.event;

import java.awt.AWTEvent;
import java.awt.Component;

/**
 * Un componente cambió de tamaño, de lugar o de visibilidad.
 *
 * <p>Es también la raíz de casi todos los eventos que tienen un componente detrás: teclado, ratón,
 * foco, ventana y contenedor heredan de acá. Lo único que agrega respecto de {@link AWTEvent} es
 * poder devolver la fuente ya con el tipo {@link Component}, que es lo que todos necesitan.
 *
 * <p>Los cuatro eventos que da nombre a la clase llegan **después** del cambio: son un aviso, no un
 * permiso.
 */
public class ComponentEvent extends AWTEvent {

    private static final long serialVersionUID = 8101406823902991265L;

    /** El primer identificador de la familia. */
    public static final int COMPONENT_FIRST = 100;

    /** El componente se ocultó. */
    public static final int COMPONENT_HIDDEN = 103;

    /** El último identificador de la familia. */
    public static final int COMPONENT_LAST = 103;

    /** El componente cambió de lugar. */
    public static final int COMPONENT_MOVED = 100;

    /** El componente cambió de tamaño. */
    public static final int COMPONENT_RESIZED = 101;

    /** El componente se hizo visible. */
    public static final int COMPONENT_SHOWN = 102;

    /**
     * Con el componente y el identificador.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public ComponentEvent(Component source, int id) {
        super(source, id);
    }

    /**
     * El componente al que le pasó.
     *
     * @return la fuente si es un componente, o `null` si no
     */
    public Component getComponent() {
        if (this.source instanceof Component) {
            return (Component) this.source;
        }
        return null;
    }

    public String paramString() {
        String tipo;
        if (this.id == COMPONENT_SHOWN) {
            tipo = "COMPONENT_SHOWN";
        } else if (this.id == COMPONENT_HIDDEN) {
            tipo = "COMPONENT_HIDDEN";
        } else if (this.id == COMPONENT_MOVED) {
            tipo = "COMPONENT_MOVED";
        } else if (this.id == COMPONENT_RESIZED) {
            tipo = "COMPONENT_RESIZED";
        } else {
            tipo = "unknown type";
        }
        Component c = this.getComponent();
        String caja = c == null ? "" : " (" + c.getX() + "," + c.getY() + " " + c.getWidth()
                + "x" + c.getHeight() + ")";
        return tipo + caja;
    }
}
