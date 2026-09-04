package javax.accessibility;

/**
 * Lo implementa lo que se puede **hacer**: apretar, alternar, seguir un enlace.
 *
 * <p>Un objeto puede tener varias acciones y la 0 es la principal — la que pasa al hacer doble clic.
 * Describirlas con una cadena y ejecutarlas por número es lo que permite que una ayuda técnica
 * ofrezca "apretar el botón Aceptar" sin saber qué es un botón.
 */
public interface AccessibleAction {

    /** Alternar el estado de expandido de un nodo. */
    String TOGGLE_EXPAND = "toggleexpand";

    /** Aumentar el valor. */
    String INCREMENT = "increment";

    /** Disminuir el valor. */
    String DECREMENT = "decrement";

    /** Empezar a reproducir. */
    String CLICK = "click";

    /** Abrir el menú contextual. */
    String TOGGLE_POPUP = "toggle popup";

    /** Cuántas acciones hay. */
    int getAccessibleActionCount();

    /**
     * Qué hace esa acción, en palabras.
     *
     * @return la descripción, o `null` si el número no existe
     */
    String getAccessibleActionDescription(int i);

    /**
     * Ejecuta esa acción.
     *
     * @return `true` si se ejecutó
     */
    boolean doAccessibleAction(int i);
}
