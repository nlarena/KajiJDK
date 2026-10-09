package javax.swing.plaf;

import javax.swing.ComponentInputMap;
import javax.swing.JComponent;

/**
 * A {@link ComponentInputMap} marked as set by the look and feel.
 *
 * <p>The same as {@link InputMapUIResource}, with the difference that this one needs to know
 * which component it belongs to; see {@link ComponentInputMap}'s note.
 */
public class ComponentInputMapUIResource extends ComponentInputMap implements UIResource {

    /** A table for that component. */
    public ComponentInputMapUIResource(JComponent component) {
        super(component);
    }
}
