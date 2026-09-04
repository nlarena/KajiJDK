package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que un documento cambio.
 */
public interface DocumentListener extends EventListener {

    /** Se inserto texto. */
    void insertUpdate(DocumentEvent e);

    /** Se borro texto. */
    void removeUpdate(DocumentEvent e);

    /** Cambio un atributo, sin que cambie el texto. */
    void changedUpdate(DocumentEvent e);
}
