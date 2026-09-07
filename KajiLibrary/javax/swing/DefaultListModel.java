package javax.swing;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Un modelo de lista sobre un {@link Vector}.
 *
 * <h2>Dos juegos de nombres para lo mismo</h2>
 *
 * <p>{@code addElement} y {@code add}, {@code elementAt} y {@code get}, {@code removeElementAt} y
 * {@code remove}. No es descuido: los primeros son los de {@code Vector}, de 1996, y los segundos
 * los de {@code List}, de 1998. Los dos estan porque hay codigo escrito con cada uno, y sacar
 * cualquiera de los dos lo rompe.
 *
 * <p>La diferencia real es que los de {@code List} devuelven lo que sacaron y los de {@code Vector}
 * no.
 *
 * <h2>Cada cambio avisa</h2>
 *
 * <p>Todo metodo que toca el contenido dispara el aviso que corresponde. Es lo que hace que la
 * lista en pantalla se actualice sola, y es tambien por lo que agregar mil elementos de a uno
 * cuesta mil avisos: para eso estan {@link #addAll}, que avisa una sola vez.
 *
 * @param <E> el tipo de los elementos.
 */
public class DefaultListModel<E> extends AbstractListModel<E> {

    private Vector<E> delegate = new Vector<E>();

    /** Un modelo vacio. */
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
     * Cambia la cantidad de elementos.
     *
     * <p>Achicar saca los del final; agrandar rellena con nulos. Avisa de lo que agrego o saco,
     * no de todo.
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

    /** Saca la primera aparicion; devuelve si estaba. */
    public boolean removeElement(Object obj) {
        int index = indexOf(obj);
        boolean rv = delegate.removeElement(obj);
        if (index >= 0) {
            fireIntervalRemoved(this, index, index);
        }
        return rv;
    }

    /** Vacia el modelo con un solo aviso. */
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

    /** Reemplaza y devuelve lo que estaba; ver la nota de la clase. */
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
     * Saca los elementos entre esos dos indices, inclusive.
     *
     * @throws ArrayIndexOutOfBoundsException si el primero es mayor que el segundo.
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

    /** Agrega todos al final, con un solo aviso. */
    public void addAll(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return;
        }
        int startIndex = getSize();
        delegate.addAll(c);
        fireIntervalAdded(this, startIndex, getSize() - 1);
    }

    /** Los inserta a partir de ese indice, con un solo aviso. */
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
