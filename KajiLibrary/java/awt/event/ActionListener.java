package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que se ejecutó una acción: un botón apretado, una opción elegida, un Enter en un campo de texto.
 */
public interface ActionListener extends EventListener {

    /** Se ejecutó la acción. */
    void actionPerformed(ActionEvent e);
}
