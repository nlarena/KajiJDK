package javax.accessibility;

/**
 * Lo implementa lo que tiene hijos que se pueden **elegir**: una lista, un árbol, una tabla, un
 * panel de pestañas.
 *
 * <p>Los hijos se nombran por su número dentro del padre, no por su número dentro de la selección.
 * Es la numeración que usa todo el paquete y evita que agregar un elemento renumere lo elegido.
 */
public interface AccessibleSelection {

    /** Cuántos hijos están elegidos. */
    int getAccessibleSelectionCount();

    /**
     * El `i`-ésimo hijo elegido.
     *
     * @return el hijo, o `null` si no hay tantos
     */
    Accessible getAccessibleSelection(int i);

    /** Si ese hijo está elegido. */
    boolean isAccessibleChildSelected(int i);

    /** Agrega ese hijo a la selección. */
    void addAccessibleSelection(int i);

    /** Saca ese hijo de la selección. */
    void removeAccessibleSelection(int i);

    /** Deja la selección vacía. */
    void clearAccessibleSelection();

    /** Elige todos los hijos, si el objeto lo admite. */
    void selectAllAccessibleSelection();
}
