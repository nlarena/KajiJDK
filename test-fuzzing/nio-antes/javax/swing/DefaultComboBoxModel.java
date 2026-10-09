package javax.swing;

import java.io.Serializable;
import java.util.Collection;
import java.util.Vector;

/**
 * A combo box model over a {@link Vector}.
 *
 * <h2>What it does besides keeping</h2>
 *
 * <p>It carries the chosen element and keeps it consistent with the list. That is all it adds
 * over {@link DefaultListModel}, and it is more work than it seems: removing the chosen one has
 * to choose another, because a combo box cannot be left with nothing shown.
 *
 * <p>The rule on removing is to choose the one that ended up in its place; if it was the last,
 * the previous one; if none is left, nothing. It is what the JDK does and it is what surprises
 * least: the drop-down goes on showing something that is beside what was there.
 *
 * @param <E> the elements' type.
 */
public class DefaultComboBoxModel<E> extends AbstractListModel<E>
        implements MutableComboBoxModel<E>, Serializable {

    private Vector<E> objects;
    private Object selectedObject;

    /** An empty model. */
    public DefaultComboBoxModel() {
        objects = new Vector<E>();
    }

    /** A model with those elements; the first is left chosen. */
    public DefaultComboBoxModel(E[] items) {
        objects = new Vector<E>(items.length);
        for (int i = 0; i < items.length; i++) {
            objects.addElement(items[i]);
        }
        if (getSize() > 0) {
            selectedObject = getElementAt(0);
        }
    }

    /**
     * A model over that vector.
     *
     * <p>The vector is not copied: changing it from outside changes the model, and the model does
     * not learn about it. It is what the JDK does, and it is worth knowing before sharing a
     * vector.
     */
    public DefaultComboBoxModel(Vector<E> v) {
        objects = v;
        if (getSize() > 0) {
            selectedObject = getElementAt(0);
        }
    }

    /** It chooses that element, even though it is not in the list. */
    public void setSelectedItem(Object anObject) {
        if ((selectedObject != null && !selectedObject.equals(anObject))
                || selectedObject == null && anObject != null) {
            selectedObject = anObject;
            // The notice with (-1, -1) means "the chosen one changed", not "a line changed".
            fireContentsChanged(this, -1, -1);
        }
    }

    public Object getSelectedItem() {
        return selectedObject;
    }

    public int getSize() {
        return objects.size();
    }

    public E getElementAt(int index) {
        if (index >= 0 && index < objects.size()) {
            return objects.elementAt(index);
        }
        return null;
    }

    /** At what position that element is, or -1. */
    public int getIndexOf(Object anObject) {
        return objects.indexOf(anObject);
    }

    /** It adds at the end; if it was the first, it is left chosen. */
    public void addElement(E anObject) {
        objects.addElement(anObject);
        fireIntervalAdded(this, objects.size() - 1, objects.size() - 1);
        if (objects.size() == 1 && selectedObject == null && anObject != null) {
            setSelectedItem(anObject);
        }
    }

    public void insertElementAt(E anObject, int index) {
        objects.insertElementAt(anObject, index);
        fireIntervalAdded(this, index, index);
    }

    /** It removes the one at that position and chooses another; see the class note. */
    public void removeElementAt(int index) {
        if (getElementAt(index) == selectedObject) {
            if (index == 0) {
                setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
            } else {
                setSelectedItem(getElementAt(index - 1));
            }
        }
        objects.removeElementAt(index);
        fireIntervalRemoved(this, index, index);
    }

    public void removeElement(Object anObject) {
        int index = objects.indexOf(anObject);
        if (index != -1) {
            removeElementAt(index);
        }
    }

    /** It empties the model; nothing is left chosen. */
    public void removeAllElements() {
        if (objects.size() > 0) {
            int firstIndex = 0;
            int lastIndex = objects.size() - 1;
            objects.removeAllElements();
            selectedObject = null;
            fireIntervalRemoved(this, firstIndex, lastIndex);
        } else {
            selectedObject = null;
        }
    }

    /** It adds them all at the end, with a single notice. */
    public void addAll(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return;
        }
        int startIndex = getSize();
        objects.addAll(c);
        fireIntervalAdded(this, startIndex, getSize() - 1);
    }

    /**
     * It inserts them from that index on, with a single notice.
     *
     * @throws ArrayIndexOutOfBoundsException if the index does not exist.
     */
    public void addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > getSize()) {
            throw new ArrayIndexOutOfBoundsException("index out of range: " + index);
        }
        if (c.isEmpty()) {
            return;
        }
        objects.addAll(index, c);
        fireIntervalAdded(this, index, index + c.size() - 1);
    }
}
