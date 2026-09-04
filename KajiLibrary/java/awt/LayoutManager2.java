package java.awt;

/**
 * Una distribución que además acepta **restricciones** por hijo.
 *
 * <p>{@link LayoutManager} sólo puede recibir un nombre al agregar un hijo, y eso no alcanza para
 * decir "esto va al norte" o "esto ocupa dos columnas y se estira". Esta interfaz cambia el nombre
 * por un objeto cualquiera, y con eso se puede pasar lo que la distribución necesite: una cadena,
 * un {@link GridBagConstraints}, lo que sea.
 *
 * <p>Agrega también la medida **máxima** y la alineación, que la primera no tenía: sin máximo, una
 * distribución que reparte espacio sobrante no sabe cuándo dejar de estirar.
 *
 * <p>{@link #invalidateLayout} existe porque estas distribuciones suelen guardar cuentas caras entre
 * llamadas; es el aviso de que hay que tirarlas.
 */
public interface LayoutManager2 extends LayoutManager {

    /**
     * Avisa que se agregó un hijo con esas restricciones.
     *
     * @throws IllegalArgumentException si las restricciones no son de la clase que esta
     *     distribución entiende
     */
    void addLayoutComponent(Component comp, Object constraints);

    /** Lo máximo que el contenedor puede aprovechar. */
    Dimension maximumLayoutSize(Container target);

    /** Cómo se alinea el contenedor horizontalmente dentro del suyo. */
    float getLayoutAlignmentX(Container target);

    /** Cómo se alinea verticalmente. */
    float getLayoutAlignmentY(Container target);

    /** Avisa que hay que tirar las cuentas guardadas. */
    void invalidateLayout(Container target);
}
