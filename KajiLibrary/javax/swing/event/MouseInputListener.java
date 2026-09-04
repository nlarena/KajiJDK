package javax.swing.event;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Los dos oyentes de mouse de AWT en uno.
 *
 * <p>No agrega ningun metodo, y aun asi no sobra: AWT los separo porque seguir el movimiento del
 * mouse tiene un costo que no todos quieren pagar. Un componente de Swing casi siempre quiere las
 * dos cosas, y sin esta interfaz habria que registrarse dos veces y guardar dos referencias al mismo
 * objeto.
 */
public interface MouseInputListener extends MouseListener, MouseMotionListener {
}
