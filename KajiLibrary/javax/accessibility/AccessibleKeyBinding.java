package javax.accessibility;

/**
 * Los atajos de teclado que activan un objeto.
 *
 * <p>El atajo se devuelve como {@code Object} y no como un tipo concreto porque no todos son la
 * misma clase de cosa: en AWT es un {@code java.awt.MenuShortcut} y en Swing un {@code KeyStroke}.
 * Es una de esas firmas que se ven flojas y que en realidad están evitando acoplar el paquete a uno
 * de los dos.
 */
public interface AccessibleKeyBinding {

    /** Cuántos atajos hay. */
    int getAccessibleKeyBindingCount();

    /**
     * El `i`-ésimo atajo.
     *
     * @return el atajo, o `null` si no hay tantos
     */
    Object getAccessibleKeyBinding(int i);
}
