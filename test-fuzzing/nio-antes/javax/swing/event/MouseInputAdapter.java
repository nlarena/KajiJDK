package javax.swing.event;

import java.awt.event.MouseAdapter;

/**
 * A {@link MouseInputListener} with every method empty.
 *
 * <p>It serves for writing a single one: without this, attending to nothing but the click forces
 * writing seven empty methods. It is AWT's adapter pattern applied to the union of the two
 * listeners.
 *
 * <p>It inherits the bodies from {@link MouseAdapter} --which already has them all-- and only
 * adds the interface. Hence this class's body is empty: there is nothing to write, and that is
 * the point.
 */
public abstract class MouseInputAdapter extends MouseAdapter implements MouseInputListener {

    /** For the subclasses. */
    protected MouseInputAdapter() {
    }
}
