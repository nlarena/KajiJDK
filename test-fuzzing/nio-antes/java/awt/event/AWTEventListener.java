package java.awt.event;

import java.awt.AWTEvent;
import java.util.EventListener;

/**
 * Whoever wants to see **every** event of certain families, before they reach their destination.
 *
 * <p>It is the back door of event dispatch: it registers with the {@code Toolkit} and not with a
 * component, and receives a copy of everything that happens. It serves for debugging and for
 * accessibility; using it for application logic is a sure way of coupling everything to everything.
 */
public interface AWTEventListener extends EventListener {

    /** An event of a watched family went past. */
    void eventDispatched(AWTEvent e);
}
