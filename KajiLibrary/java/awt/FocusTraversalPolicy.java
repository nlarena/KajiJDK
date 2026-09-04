package java.awt;

/**
 * En qué orden el tabulador recorre los componentes de un contenedor.
 *
 * <p>El orden por omisión —el visual, de arriba abajo y de izquierda a derecha— casi siempre está
 * bien, pero no siempre: un formulario en dos columnas suele quererse recorrido por columna y no por
 * fila. Esta clase es donde se cambia.
 *
 * <p>Los cinco métodos abstractos no son redundantes. {@link #getDefaultComponent} es a quién le
 * toca el foco cuando el contenedor lo recibe **por primera vez**, y {@link #getFirstComponent} es
 * el primero del recorrido: en un diálogo, el primero suele ser un campo de texto y el
 * predeterminado el botón Aceptar.
 */
public abstract class FocusTraversalPolicy {

    /** Para las subclases. */
    protected FocusTraversalPolicy() {
    }

    /**
     * A quién le toca el foco después de ese componente.
     *
     * @throws IllegalArgumentException si falta alguno de los dos, o si el componente no está en
     *     ese contenedor
     */
    public abstract Component getComponentAfter(Container aContainer, Component aComponent);

    /**
     * A quién le tocaba antes.
     *
     * @throws IllegalArgumentException si falta alguno de los dos, o si el componente no está en
     *     ese contenedor
     */
    public abstract Component getComponentBefore(Container aContainer, Component aComponent);

    /**
     * El primero del recorrido.
     *
     * @throws IllegalArgumentException si el contenedor es `null`
     */
    public abstract Component getFirstComponent(Container aContainer);

    /**
     * El último del recorrido.
     *
     * @throws IllegalArgumentException si el contenedor es `null`
     */
    public abstract Component getLastComponent(Container aContainer);

    /**
     * A quién le toca cuando el contenedor recibe el foco.
     *
     * @throws IllegalArgumentException si el contenedor es `null`
     */
    public abstract Component getDefaultComponent(Container aContainer);

    /**
     * A quién le toca la primera vez que se muestra la ventana.
     *
     * <p>Por omisión, el mismo que {@link #getDefaultComponent}. Se separa para que una política
     * pueda distinguir la primera vez de las siguientes, que es lo que hace falta cuando el diálogo
     * tiene que arrancar con el foco en un campo pero volver al botón después.
     *
     * @throws IllegalArgumentException si la ventana es `null`
     */
    public Component getInitialComponent(Window window) {
        if (window == null) {
            throw new IllegalArgumentException("window cannot be equal to null.");
        }
        Component def = this.getDefaultComponent(window);
        if (def == null && window.isFocusableWindow()) {
            return window;
        }
        return def;
    }
}
