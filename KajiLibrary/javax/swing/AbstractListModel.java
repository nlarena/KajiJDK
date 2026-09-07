package javax.swing;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * La parte de un modelo de lista que siempre es igual: avisar.
 *
 * <h2>Que resuelve</h2>
 *
 * <p>Todo modelo tiene que guardar quien escucha y avisarle de tres formas: cambio el contenido,
 * se agregaron renglones, se sacaron renglones. Eso no depende de donde salgan los datos, asi que
 * esta escrito una sola vez.
 *
 * <p>Lo que queda para la subclase son los dos metodos que si dependen: cuantos hay y cual es el
 * numero tal. Un modelo sobre un arreglo, sobre una consulta o sobre un archivo comparten todo lo
 * demas.
 *
 * <h2>Los avisos llevan un rango, no un elemento</h2>
 *
 * <p>Los tres metodos reciben dos indices. Es lo que permite agregar mil renglones con un solo
 * aviso: si llevaran un elemento, la lista se rearmaria mil veces.
 *
 * @param <E> el tipo de los elementos.
 */
public abstract class AbstractListModel<E> implements ListModel<E>, Serializable {

    /** Quienes escuchan. */
    protected EventListenerList listenerList = new EventListenerList();

    protected AbstractListModel() {
    }

    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

    public ListDataListener[] getListDataListeners() {
        return listenerList.getListeners(ListDataListener.class);
    }

    /**
     * Avisa que cambio lo que hay entre esos dos indices, inclusive.
     *
     * <p>El evento se arma recien cuando hay alguien a quien darselo. Armarlo antes seria mas
     * simple y reservaria un objeto por aviso aunque nadie escuche, y estos avisos salen de a
     * miles.
     */
    protected void fireContentsChanged(Object source, int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).contentsChanged(e);
            }
        }
    }

    /** Avisa que se agregaron los renglones entre esos dos indices. */
    protected void fireIntervalAdded(Object source, int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).intervalAdded(e);
            }
        }
    }

    /**
     * Avisa que se sacaron los renglones entre esos dos indices.
     *
     * <p>Los indices son los que tenian <em>antes</em> de sacarlos: despues ya no existen.
     */
    protected void fireIntervalRemoved(Object source, int index0, int index1) {
        Object[] listeners = listenerList.getListenerList();
        ListDataEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ListDataListener.class) {
                if (e == null) {
                    e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
                }
                ((ListDataListener) listeners[i + 1]).intervalRemoved(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
