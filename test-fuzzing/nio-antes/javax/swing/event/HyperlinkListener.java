package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that something is happening with a link.
 */
public interface HyperlinkListener extends EventListener {

    /** The mouse entered, left, or the link was activated. */
    void hyperlinkUpdate(HyperlinkEvent e);
}
