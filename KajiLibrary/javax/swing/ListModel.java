package javax.swing;

import javax.swing.event.ListDataListener;

/**
 * What a list needs to know about its data.
 *
 * <h2>Two methods and two notices</h2>
 *
 * <p>How many there are and which is number such-and-such. With that it is enough to draw a list
 * of a million lines without having the million in memory: the list asks only about those that
 * are seen.
 *
 * <p>The other two methods are for giving notice. Without them the model could change and the
 * list would go on showing the old thing, because it has no way of noticing on its own.
 *
 * @param <E> the elements' type.
 */
public interface ListModel<E> {

    /** How many elements there are. */
    int getSize();

    /** Element number such-and-such, counting from zero. */
    E getElementAt(int index);

    /** It adds whoever wants to learn about the changes. */
    void addListDataListener(ListDataListener l);

    void removeListDataListener(ListDataListener l);
}
