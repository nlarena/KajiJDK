package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse del ciclo de vida de una ventana.

 <p>{@code windowClosing} es el aviso de que el usuario pidió cerrar, y llega **antes**: es donde se
 pregunta si quiere guardar. {@code windowClosed} llega después de que la ventana ya no existe.
 */
public interface WindowListener extends EventListener {

    /** La ventana se abrió por primera vez. */
    void windowOpened(WindowEvent e);

    /** El usuario pidió cerrarla. */
    void windowClosing(WindowEvent e);

    /** La ventana se cerró. */
    void windowClosed(WindowEvent e);

    /** Se minimizó. */
    void windowIconified(WindowEvent e);

    /** Se restauró. */
    void windowDeiconified(WindowEvent e);

    /** Pasó a ser la ventana activa. */
    void windowActivated(WindowEvent e);

    /** Dejó de ser la ventana activa. */
    void windowDeactivated(WindowEvent e);
}
