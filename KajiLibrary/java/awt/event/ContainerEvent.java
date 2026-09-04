package java.awt.event;

import java.awt.Component;
import java.awt.Container;

/**
 * A un contenedor le agregaron o le sacaron un hijo.
 *
 * <p>Llega **después** del cambio y es sólo un aviso: no se puede vetar. La fuente es el contenedor
 * y el hijo viene aparte, porque el interesante es el hijo.
 */
public class ContainerEvent extends ComponentEvent {

    private static final long serialVersionUID = -4114942250539772041L;

    /** Se agregó un hijo. */
    public static final int COMPONENT_ADDED = 300;

    /** Se sacó un hijo. */
    public static final int COMPONENT_REMOVED = 301;

    /** El primer identificador de la familia. */
    public static final int CONTAINER_FIRST = 300;

    /** El último identificador de la familia. */
    public static final int CONTAINER_LAST = 301;

    private final Component child;

    /**
     * Con el contenedor, el identificador y el hijo.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public ContainerEvent(Component source, int id, Component child) {
        super(source, id);
        this.child = child;
    }

    /** El contenedor al que le pasó. */
    public Container getContainer() {
        if (this.source instanceof Container) {
            return (Container) this.source;
        }
        return null;
    }

    /** El hijo que se agregó o se sacó. */
    public Component getChild() {
        return this.child;
    }

    public String paramString() {
        String tipo;
        if (this.id == COMPONENT_ADDED) {
            tipo = "COMPONENT_ADDED";
        } else if (this.id == COMPONENT_REMOVED) {
            tipo = "COMPONENT_REMOVED";
        } else {
            tipo = "unknown type";
        }
        return tipo + ",child=" + (this.child == null ? "null" : this.child.getName());
    }
}
