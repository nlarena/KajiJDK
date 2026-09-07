package javax.swing;

/**
 * Una lista que ademas tiene un elemento elegido.
 *
 * <h2>Por que el elegido va en el modelo</h2>
 *
 * <p>Podria estar en el componente. Que este en el modelo permite que dos listas desplegables
 * compartan modelo y muestren siempre lo mismo, y que el elegido sobreviva a cambiar el aspecto.
 *
 * <p>{@link #getSelectedItem} devuelve {@code Object} y no {@code E} a proposito: en una lista
 * editable el usuario puede escribir algo que no esta en la lista, y eso no tiene por que ser del
 * tipo de los elementos.
 *
 * @param <E> el tipo de los elementos.
 */
public interface ComboBoxModel<E> extends ListModel<E> {

    /** Elige ese elemento; deberia avisar a quien escucha. */
    void setSelectedItem(Object anItem);

    /** El elegido, que puede no estar en la lista; ver la nota de la clase. */
    Object getSelectedItem();
}
