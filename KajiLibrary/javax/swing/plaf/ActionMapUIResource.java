package javax.swing.plaf;

import javax.swing.ActionMap;

/**
 * Una {@link ActionMap} marcada como puesta por el aspecto.
 *
 * <p>La marca es toda la clase. Sirve para que al cambiar de aspecto se reemplace esta tabla y se
 * respete la que haya puesto el programa; ver {@link UIResource}.
 */
public class ActionMapUIResource extends ActionMap implements UIResource {

    public ActionMapUIResource() {
    }
}
