package javax.swing.event;

import java.util.EventObject;

/**
 * A menu was selected, deselected or cancelled.
 *
 * <p>With no data beyond the source: which of the three things happened is said by the
 * {@link MenuListener} method it reaches, not by the event.
 */
public class MenuEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public MenuEvent(Object source) {
        super(source);
    }
}
