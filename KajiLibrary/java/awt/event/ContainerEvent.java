package java.awt.event;

import java.awt.Component;
import java.awt.Container;

/**
 * A child was added to or taken from a container.
 *
 * <p>It arrives **after** the change and is only a notice: it cannot be vetoed. The source is the
 * container and the child travels apart, because the interesting one is the child.
 */
public class ContainerEvent extends ComponentEvent {

    private static final long serialVersionUID = -4114942250539772041L;

    /** A child was added. */
    public static final int COMPONENT_ADDED = 300;

    /** A child was taken away. */
    public static final int COMPONENT_REMOVED = 301;

    /** The family's first identifier. */
    public static final int CONTAINER_FIRST = 300;

    /** The family's last identifier. */
    public static final int CONTAINER_LAST = 301;

    private final Component child;

    /**
     * With the container, the identifier and the child.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public ContainerEvent(Component source, int id, Component child) {
        super(source, id);
        this.child = child;
    }

    /** The container it happened to. */
    public Container getContainer() {
        if (this.source instanceof Container) {
            return (Container) this.source;
        }
        return null;
    }

    /** The child that was added or taken away. */
    public Component getChild() {
        return this.child;
    }

    public String paramString() {
        String type;
        if (this.id == COMPONENT_ADDED) {
            type = "COMPONENT_ADDED";
        } else if (this.id == COMPONENT_REMOVED) {
            type = "COMPONENT_REMOVED";
        } else {
            type = "unknown type";
        }
        return type + ",child=" + (this.child == null ? "null" : this.child.getName());
    }
}
