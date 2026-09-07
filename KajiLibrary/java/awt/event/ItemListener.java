package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that an item was chosen or stopped being chosen.
 */
public interface ItemListener extends EventListener {

    /** What is chosen changed. */
    void itemStateChanged(ItemEvent e);
}
