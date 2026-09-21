package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that the text cursor moved.
 */
public interface CaretListener extends EventListener {

    /** The cursor moved or the selection changed. */
    void caretUpdate(CaretEvent e);
}
