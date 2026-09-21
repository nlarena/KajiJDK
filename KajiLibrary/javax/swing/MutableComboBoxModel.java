package javax.swing;

/**
 * A combo box list that elements can be added to and removed from.
 *
 * <p>{@link ComboBoxModel} only allows looking. This one adds the four methods needed in order
 * to change it, and it is what {@link JComboBox} expects when it is asked to add something
 * directly.
 *
 * @param <E> the elements' type.
 */
public interface MutableComboBoxModel<E> extends ComboBoxModel<E> {

    /** It adds at the end. */
    void addElement(E item);

    void removeElement(Object obj);

    /** It inserts at that position. */
    void insertElementAt(E item, int index);

    void removeElementAt(int index);
}
