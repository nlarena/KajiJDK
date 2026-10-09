package javax.swing.event;

import java.util.EventObject;

/**
 * A popup menu is about to appear, to go away, or was cancelled.
 *
 * <p>As with {@link MenuEvent}, which of the three is said by the method that receives it.
 */
public class PopupMenuEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public PopupMenuEvent(Object source) {
        super(source);
    }
}
