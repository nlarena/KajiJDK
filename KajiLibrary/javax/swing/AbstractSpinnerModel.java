package javax.swing;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * La parte de {@link SpinnerModel} que es igual en todos: los oyentes.
 *
 * <h2>Un solo evento para todos</h2>
 *
 * <p>{@link #fireStateChanged} arma un {@link ChangeEvent} y lo reparte. El evento no dice que
 * cambio -- solo que algo cambio --, asi que uno solo alcanza y se arma una sola vez por tanda.
 * Quien escucha vuelve a preguntar el valor.
 *
 * <p>Los oyentes se recorren de atras para adelante, que es el orden del JDK: el ultimo en
 * anotarse es el primero en enterarse.
 */
public abstract class AbstractSpinnerModel implements SpinnerModel, Serializable {

    /** Los oyentes, por tipo. */
    protected EventListenerList listenerList = new EventListenerList();

    private transient ChangeEvent changeEvent = null;

    /** Para las subclases. */
    protected AbstractSpinnerModel() {
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Avisa que el valor cambio.
     *
     * <p>El evento se arma la primera vez que hace falta y se reusa: es inmutable salvo por su
     * origen, que siempre es este modelo.
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    /** Los oyentes de ese tipo anotados en este modelo. */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
