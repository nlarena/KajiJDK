package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de los botones del ratón y de cuándo entra y sale del componente.

 <p>{@code mouseClicked} llega **además** de apretar y soltar, y sólo si el ratón no se movió entre
 los dos. Quien quiera reaccionar a un clic sin importar el arrastre tiene que usar
 {@code mouseReleased}.
 */
public interface MouseListener extends EventListener {

    /** Se apretó y se soltó sin mover. */
    void mouseClicked(MouseEvent e);

    /** Se apretó un botón. */
    void mousePressed(MouseEvent e);

    /** Se soltó un botón. */
    void mouseReleased(MouseEvent e);

    /** El ratón entró al componente. */
    void mouseEntered(MouseEvent e);

    /** El ratón salió del componente. */
    void mouseExited(MouseEvent e);
}
