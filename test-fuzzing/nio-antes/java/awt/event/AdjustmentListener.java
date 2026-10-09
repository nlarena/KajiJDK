package java.awt.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a scrollbar was moved.
 */
public interface AdjustmentListener extends EventListener {

    /** The value changed. */
    void adjustmentValueChanged(AdjustmentEvent e);
}
