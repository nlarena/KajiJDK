package java.awt.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que el ratón se movió sobre el componente.

 <p>Está separado de {@link MouseListener} porque son órdenes de magnitud más eventos: mover el
 ratón un segundo genera decenas, y apretar un botón genera uno.
 */
public interface MouseMotionListener extends EventListener {

    /** Se movió con un botón apretado. */
    void mouseDragged(MouseEvent e);

    /** Se movió sin botones apretados. */
    void mouseMoved(MouseEvent e);
}
