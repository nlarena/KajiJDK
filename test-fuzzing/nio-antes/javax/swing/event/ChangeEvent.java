package javax.swing.event;

import java.util.EventObject;

/**
 * Something changed, and it does not say what.
 *
 * <p>It is Swing's most used event and the poorest on purpose: it only carries its source. The
 * idea is that whoever listens already has the object that changed and can ask it whatever it
 * needs -- the event has no reason to guess which of its properties interests them.
 *
 * <p>That poverty has a practical consequence: since it carries no data, one same instance serves
 * for all of an object's notices. Almost all of Swing creates one and reuses it for ever.
 */
public class ChangeEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** @param source the object that changed */
    public ChangeEvent(Object source) {
        super(source);
    }
}
