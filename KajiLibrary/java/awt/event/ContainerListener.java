package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que a un contenedor le agregaron o le sacaron un hijo.
 */
public interface ContainerListener extends EventListener {

    /** Se agregó un hijo. */
    void componentAdded(ContainerEvent e);

    /** Se sacó un hijo. */
    void componentRemoved(ContainerEvent e);
}
