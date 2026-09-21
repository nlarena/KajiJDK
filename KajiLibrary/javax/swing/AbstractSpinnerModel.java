package javax.swing;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The part of {@link SpinnerModel} that is the same in all of them: the listeners.
 *
 * <h2>A single event for everybody</h2>
 *
 * <p>{@link #fireStateChanged} builds a {@link ChangeEvent} and hands it out. The event does
 * not say what changed -- only that something changed --, so one is enough and it is built once
 * per round. Whoever listens asks for the value again.
 *
 * <p>The listeners are walked through back to front, which is the JDK's order: the last to sign
 * up is the first to learn about it.
 */
public abstract class AbstractSpinnerModel implements SpinnerModel, Serializable {

    /** The listeners, by type. */
    protected EventListenerList listenerList = new EventListenerList();

    private transient ChangeEvent changeEvent = null;

    /** For the subclasses. */
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
     * It gives notice that the value changed.
     *
     * <p>The event is built the first time it is needed and reused: it is immutable save for its
     * source, which is always this model.
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

    /** The listeners of that type signed up on this model. */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }
}
