package java.awt.datatransfer;

import java.util.EventListener;

/**
 * Whoever wants to hear that what is on the clipboard has changed.
 *
 * <p>The typical use is enabling or disabling the paste button according to whether there is
 * anything pasteable.
 */
public interface FlavorListener extends EventListener {

    /** Reports that the clipboard's contents are now something else. */
    void flavorsChanged(FlavorEvent e);
}
