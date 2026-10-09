package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that an action was carried out: a button pressed, an option chosen, an Enter
 * in a text field.
 */
public interface ActionListener extends EventListener {

    /** The action was carried out. */
    void actionPerformed(ActionEvent e);
}
