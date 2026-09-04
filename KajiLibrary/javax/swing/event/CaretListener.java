package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que el cursor de texto se movio.
 */
public interface CaretListener extends EventListener {

    /** El cursor se movio o cambio la seleccion. */
    void caretUpdate(CaretEvent e);
}
