package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a component's text changed.
 */
public interface TextListener extends EventListener {

    /** The text changed. */
    void textValueChanged(TextEvent e);
}
