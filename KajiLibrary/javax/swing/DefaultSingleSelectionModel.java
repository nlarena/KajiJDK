package javax.swing;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * Un {@link SingleSelectionModel} guardado en un entero.
 *
 * <p>Todo el modelo es ese entero y la lista de quienes escuchan. El evento se arma una sola vez y
 * se reusa: no lleva datos -- solo dice "algo cambio" -- asi que reservar uno por aviso seria
 * gastar por nada.
 */
public class DefaultSingleSelectionModel implements SingleSelectionModel, Serializable {

    private static final int NADA = -1;

    /** El unico evento; ver la nota de la clase. */
    protected transient ChangeEvent changeEvent = null;

    /** Quienes escuchan. */
    protected EventListenerList listenerList = new EventListenerList();

    private int index = NADA;

    /** Un modelo sin nada elegido. */
    public DefaultSingleSelectionModel() {
    }

    public int getSelectedIndex() {
        return index;
    }

    /** Elige ese indice; solo avisa si de verdad cambio. */
    public void setSelectedIndex(int index) {
        if (this.index != index) {
            this.index = index;
            fireStateChanged();
        }
    }

    public void clearSelection() {
        setSelectedIndex(NADA);
    }

    public boolean isSelected() {
        return getSelectedIndex() != NADA;
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

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
