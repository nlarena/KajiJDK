package javax.swing;

import java.io.Serializable;
import java.util.Collection;
import java.util.Vector;

/**
 * Un modelo de lista desplegable sobre un {@link Vector}.
 *
 * <h2>Que hace ademas de guardar</h2>
 *
 * <p>Lleva el elemento elegido y lo mantiene coherente con la lista. Eso es todo lo que agrega
 * sobre {@link DefaultListModel}, y es mas trabajo del que parece: sacar el elegido tiene que
 * elegir otro, porque una lista desplegable no puede quedar sin nada mostrado.
 *
 * <p>La regla al sacar es elegir el que quedo en su lugar; si era el ultimo, el anterior; si no
 * queda ninguno, nada. Es lo que hace el JDK y es lo que menos sorprende: el desplegable sigue
 * mostrando algo que esta al lado de lo que habia.
 *
 * @param <E> el tipo de los elementos.
 */
public class DefaultComboBoxModel<E> extends AbstractListModel<E>
        implements MutableComboBoxModel<E>, Serializable {

    private Vector<E> objects;
    private Object selectedObject;

    /** Un modelo vacio. */
    public DefaultComboBoxModel() {
        objects = new Vector<E>();
    }

    /** Un modelo con esos elementos; el primero queda elegido. */
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
     * Un modelo sobre ese vector.
     *
     * <p>El vector no se copia: cambiarlo por afuera cambia el modelo, y el modelo no se entera.
     * Es lo que hace el JDK, y conviene saberlo antes de compartir un vector.
     */
    public DefaultComboBoxModel(Vector<E> v) {
        objects = v;
        if (getSize() > 0) {
            selectedObject = getElementAt(0);
        }
    }

    /** Elige ese elemento, aunque no este en la lista. */
    public void setSelectedItem(Object anObject) {
        if ((selectedObject != null && !selectedObject.equals(anObject))
                || selectedObject == null && anObject != null) {
            selectedObject = anObject;
            // El aviso con (-1, -1) significa "cambio el elegido", no "cambio un renglon".
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

    /** En que posicion esta ese elemento, o -1. */
    public int getIndexOf(Object anObject) {
        return objects.indexOf(anObject);
    }

    /** Agrega al final; si era el primero, queda elegido. */
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

    /** Saca el de esa posicion y elige otro; ver la nota de la clase. */
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

    /** Vacia el modelo; no queda nada elegido. */
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

    /** Agrega todos al final, con un solo aviso. */
    public void addAll(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return;
        }
        int startIndex = getSize();
        objects.addAll(c);
        fireIntervalAdded(this, startIndex, getSize() - 1);
    }

    /**
     * Los inserta a partir de ese indice, con un solo aviso.
     *
     * @throws ArrayIndexOutOfBoundsException si el indice no existe.
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
