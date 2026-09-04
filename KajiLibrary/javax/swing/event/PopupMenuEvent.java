package javax.swing.event;

import java.util.EventObject;

/**
 * Un menu emergente esta por aparecer, por irse, o se cancelo.
 *
 * <p>Como {@link MenuEvent}, cual de las tres lo dice el metodo que lo recibe.
 */
public class PopupMenuEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public PopupMenuEvent(Object source) {
        super(source);
    }
}
