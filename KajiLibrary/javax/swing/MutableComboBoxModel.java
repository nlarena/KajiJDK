package javax.swing;

/**
 * Una lista desplegable a la que se le pueden agregar y sacar elementos.
 *
 * <p>{@link ComboBoxModel} solo deja mirar. Esta agrega los cuatro metodos que hacen falta para
 * cambiarla, y es la que espera {@link JComboBox} cuando se le pide agregar algo directamente.
 *
 * @param <E> el tipo de los elementos.
 */
public interface MutableComboBoxModel<E> extends ComboBoxModel<E> {

    /** Agrega al final. */
    void addElement(E item);

    void removeElement(Object obj);

    /** Inserta en esa posicion. */
    void insertElementAt(E item, int index);

    void removeElementAt(int index);
}
