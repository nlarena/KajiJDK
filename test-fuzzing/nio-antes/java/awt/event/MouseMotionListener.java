package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that the mouse moved over the component.
 *
 * <p>It is kept apart from {@link MouseListener} because they are orders of magnitude more events:
 * moving the mouse for a second produces dozens, and pressing a button produces one.
 */
public interface MouseMotionListener extends EventListener {

    /** It moved with a button pressed. */
    void mouseDragged(MouseEvent e);

    /** It moved with no button pressed. */
    void mouseMoved(MouseEvent e);
}
