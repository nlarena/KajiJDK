package javax.swing;

import java.io.Serializable;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * The part of a list model that is always the same: giving notice.
 *
 * <h2>What it resolves</h2>
 *
 * <p>Every model has to keep who listens and tell them in three ways: the content changed,
 * lines were added, lines were removed. That does not depend on where the data comes from, so
 * it is written once.
 *
 * <p>What is left for the subclass are the two methods that do depend: how many there are and
 * which is number such-and-such. A model over an array, over a query or over a file share
 * everything else.
 *
 * <h2>The notices carry a range, not an element</h2>
 *
 * <p>The three methods receive two indices. It is what allows a thousand lines to be added with
 * a single notice: if they carried an element, the list would rebuild itself a thousand times.
 *
 * @param <E> the elements' type.
 */
public abstract class AbstractListModel<E> implements ListModel<E>, Serializable {

    /** Those who listen. */
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
     * It gives notice that what is between those two indices changed, inclusive.
     *
     * <p>The event is built only when there is somebody to give it to. Building it beforehand
     * would be simpler and would reserve an object per notice even though nobody listens, and
     * these notices come out in their thousands.
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

    /** It gives notice that the lines between those two indices were added. */
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
     * It gives notice that the lines between those two indices were removed.
     *
     * <p>The indices are the ones they had <em>before</em> removing them: afterwards they no
     * longer exist.
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
