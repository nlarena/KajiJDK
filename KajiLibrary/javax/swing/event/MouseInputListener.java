package javax.swing.event;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * AWT's two mouse listeners in one.
 *
 * <p>It adds no method, and even so it is not redundant: AWT separated them because following the
 * mouse's movement has a cost that not everybody wants to pay. A Swing component almost always
 * wants both things, and without this interface one would have to register twice and keep two
 * references to the same object.
 */
public interface MouseInputListener extends MouseListener, MouseMotionListener {
}
