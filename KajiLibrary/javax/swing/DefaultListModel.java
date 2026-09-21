package javax.swing;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A list model over a {@link Vector}.
 *
 * <h2>Two sets of names for the same thing</h2>
 *
 * <p>{@code addElement} and {@code add}, {@code elementAt} and {@code get},
 * {@code removeElementAt} and {@code remove}. It is not an oversight: the first are
 * {@code Vector}'s, from 1996, and the second {@code List}'s, from 1998. Both are there because
 * there is code written with each one, and removing either of the two breaks it.
 *
 * <p>The real difference is that {@code List}'s return what they removed and {@code Vector}'s do
 * not.
 *
 * <h2>Every change gives notice</h2>
 *
 * <p>Every method that touches the content fires the notice that applies. It is what makes the
 * list on the screen update itself, and it is also why adding a thousand elements one at a time
 * costs a thousand notices: that is what {@link #addAll} is for, which gives notice only once.
 *
 * @param <E> the elements' type.
 */
public class DefaultListModel<E> extends AbstractListModel<E> {

    private Vector<E> delegate = new Vector<E>();

    /** An empty model. */
    public DefaultListModel() {
    }

    public int getSize() {
        return delegate.size();
    }

    public E getElementAt(int index) {
        return delegate.elementAt(index);
    }

    public void copyInto(Object[] anArray) {
        delegate.copyInto(anArray);
    }

    public void trimToSize() {
        delegate.trimToSize();
    }

    public void ensureCapacity(int minCapacity) {
        delegate.ensureCapacity(minCapacity);
    }

    /**
     * It changes the number of elements.
     *
     * <p>Shrinking removes those at the end; growing fills with nulls. It gives notice of what it
     * added or removed, not of everything.
     */
    public void setSize(int newSize) {
        int oldSize = delegate.size();
        delegate.setSize(newSize);
        if (oldSize > newSize) {
            fireIntervalRemoved(this, newSize, oldSize - 1);
        } else if (oldSize < newSize) {
            fireIntervalAdded(this, oldSize, newSize - 1);
        }
    }

    public int capacity() {
        return delegate.capacity();
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Enumeration<E> elements() {
        return delegate.elements();
    }

    public boolean contains(Object elem) {
        return delegate.contains(elem);
    }

    public int indexOf(Object elem) {
        return delegate.indexOf(elem);
    }

    public int indexOf(Object elem, int index) {
        return delegate.indexOf(elem, index);
    }

    public int lastIndexOf(Object elem) {
        return delegate.lastIndexOf(elem);
    }

    public int lastIndexOf(Object elem, int index) {
        return delegate.lastIndexOf(elem, index);
    }

    public E elementAt(int index) {
        return delegate.elementAt(index);
    }

    public E firstElement() {
        return delegate.firstElement();
    }

    public E lastElement() {
        return delegate.lastElement();
    }

    public void setElementAt(E element, int index) {
        delegate.setElementAt(element, index);
        fireContentsChanged(this, index, index);
    }

    public void removeElementAt(int index) {
        delegate.removeElementAt(index);
        fireIntervalRemoved(this, index, index);
    }

    public void insertElementAt(E element, int index) {
        delegate.insertElementAt(element, index);
        fireIntervalAdded(this, index, index);
    }

    public void addElement(E element) {
        int index = delegate.size();
        delegate.addElement(element);
        fireIntervalAdded(this, index, index);
    }

    /** It removes the first appearance; it returns whether it was there. */
    public boolean removeElement(Object obj) {
        int index = indexOf(obj);
        boolean rv = delegate.removeElement(obj);
        if (index >= 0) {
            fireIntervalRemoved(this, index, index);
        }
        return rv;
    }

    /** It empties the model with a single notice. */
    public void removeAllElements() {
        int index1 = delegate.size() - 1;
        delegate.removeAllElements();
        if (index1 >= 0) {
            fireIntervalRemoved(this, 0, index1);
        }
    }

    public String toString() {
        return delegate.toString();
    }

    public Object[] toArray() {
        Object[] rv = new Object[delegate.size()];
        delegate.copyInto(rv);
        return rv;
    }

    public E get(int index) {
        return delegate.elementAt(index);
    }

    /** It replaces and returns what was there; see the class note. */
    public E set(int index, E element) {
        E rv = delegate.elementAt(index);
        delegate.setElementAt(element, index);
        fireContentsChanged(this, index, index);
        return rv;
    }

    public void add(int index, E element) {
        delegate.insertElementAt(element, index);
        fireIntervalAdded(this, index, index);
    }

    public E remove(int index) {
        E rv = delegate.elementAt(index);
        delegate.removeElementAt(index);
        fireIntervalRemoved(this, index, index);
        return rv;
    }

    public void clear() {
        int index1 = delegate.size() - 1;
        delegate.removeAllElements();
        if (index1 >= 0) {
            fireIntervalRemoved(this, 0, index1);
        }
    }

    /**
     * It removes the elements between those two indices, inclusive.
     *
     * @throws ArrayIndexOutOfBoundsException if the first is greater than the second.
     */
    public void removeRange(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex must be <= toIndex");
        }
        for (int i = toIndex; i >= fromIndex; i--) {
            delegate.removeElementAt(i);
        }
        fireIntervalRemoved(this, fromIndex, toIndex);
    }

    /** It adds them all at the end, with a single notice. */
    public void addAll(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return;
        }
        int startIndex = getSize();
        delegate.addAll(c);
        fireIntervalAdded(this, startIndex, getSize() - 1);
    }

    /** It inserts them from that index on, with a single notice. */
    public void addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > getSize()) {
            throw new ArrayIndexOutOfBoundsException("index out of range: " + index);
        }
        if (c.isEmpty()) {
            return;
        }
        delegate.addAll(index, c);
        fireIntervalAdded(this, index, index + c.size() - 1);
    }
}
