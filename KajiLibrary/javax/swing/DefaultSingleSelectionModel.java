package javax.swing;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * A {@link SingleSelectionModel} kept in an integer.
 *
 * <p>The whole model is that integer and the list of those who listen. The event is built once
 * and reused: it carries no data -- it only says "something changed" -- so reserving one per
 * notice would be spending for nothing.
 */
public class DefaultSingleSelectionModel implements SingleSelectionModel, Serializable {

    private static final int NONE = -1;

    /** The single event; see the class note. */
    protected transient ChangeEvent changeEvent = null;

    /** Those who listen. */
    protected EventListenerList listenerList = new EventListenerList();

    private int index = NONE;

    /** A model with nothing chosen. */
    public DefaultSingleSelectionModel() {
    }

    public int getSelectedIndex() {
        return index;
    }

    /** It chooses that index; it only gives notice if it really changed. */
    public void setSelectedIndex(int index) {
        if (this.index != index) {
            this.index = index;
            fireStateChanged();
        }
    }

    public void clearSelection() {
        setSelectedIndex(NONE);
    }

    public boolean isSelected() {
        return getSelectedIndex() != NONE;
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
