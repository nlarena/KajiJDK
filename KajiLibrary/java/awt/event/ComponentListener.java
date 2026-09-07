package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a component changed size, place or visibility.
 *
 * <p>These events arrive **after** the change, so they serve to react and not to veto it.
 */
public interface ComponentListener extends EventListener {

    /** It changed size. */
    void componentResized(ComponentEvent e);

    /** It changed place. */
    void componentMoved(ComponentEvent e);

    /** It became visible. */
    void componentShown(ComponentEvent e);

    /** It was hidden. */
    void componentHidden(ComponentEvent e);
}
