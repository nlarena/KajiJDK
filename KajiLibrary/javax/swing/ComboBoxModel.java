package javax.swing;

/**
 * A list that also has a chosen element.
 *
 * <h2>Why the chosen one goes in the model</h2>
 *
 * <p>It could be in the component. Having it in the model allows two combo boxes to share a
 * model and always show the same, and the chosen one to survive changing the look and feel.
 *
 * <p>{@link #getSelectedItem} returns {@code Object} and not {@code E} on purpose: in an
 * editable combo box the user may type something that is not in the list, and that does not have
 * to be of the elements' type.
 *
 * @param <E> the elements' type.
 */
public interface ComboBoxModel<E> extends ListModel<E> {

    /** It chooses that element; it should give notice to whoever listens. */
    void setSelectedItem(Object anItem);

    /** The chosen one, which may not be in the list; see the class note. */
    Object getSelectedItem();
}
