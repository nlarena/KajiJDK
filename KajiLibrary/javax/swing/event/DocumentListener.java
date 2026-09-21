package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear that a document changed.
 */
public interface DocumentListener extends EventListener {

    /** Text was inserted. */
    void insertUpdate(DocumentEvent e);

    /** Text was removed. */
    void removeUpdate(DocumentEvent e);

    /** An attribute changed, without the text changing. */
    void changedUpdate(DocumentEvent e);
}
