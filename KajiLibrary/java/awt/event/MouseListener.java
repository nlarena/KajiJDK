package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear about the mouse buttons and about when it enters and leaves the component.
 *
 * <p>{@code mouseClicked} arrives **on top of** the press and the release, and only if the mouse did
 * not move between the two. Whoever wants to react to a click regardless of dragging has to use
 * {@code mouseReleased}.
 */
public interface MouseListener extends EventListener {

    /** It was pressed and released without moving. */
    void mouseClicked(MouseEvent e);

    /** A button was pressed. */
    void mousePressed(MouseEvent e);

    /** A button was released. */
    void mouseReleased(MouseEvent e);

    /** The mouse entered the component. */
    void mouseEntered(MouseEvent e);

    /** The mouse left the component. */
    void mouseExited(MouseEvent e);
}
