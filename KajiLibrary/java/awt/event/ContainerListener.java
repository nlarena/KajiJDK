package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a child was added to or taken from a container.
 */
public interface ContainerListener extends EventListener {

    /** A child was added. */
    void componentAdded(ContainerEvent e);

    /** A child was taken away. */
    void componentRemoved(ContainerEvent e);
}
