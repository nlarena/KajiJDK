package javax.swing.plaf;

import javax.swing.ComponentInputMap;
import javax.swing.JComponent;

/**
 * Un {@link ComponentInputMap} marcado como puesto por el aspecto.
 *
 * <p>Igual que {@link InputMapUIResource}, con la diferencia de que este necesita saber de que
 * componente es; ver la nota de {@link ComponentInputMap}.
 */
public class ComponentInputMapUIResource extends ComponentInputMap implements UIResource {

    /** Una tabla para ese componente. */
    public ComponentInputMapUIResource(JComponent component) {
        super(component);
    }
}
