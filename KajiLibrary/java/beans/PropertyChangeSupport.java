package java.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The helper a bean with bound properties delegates to for keeping the list of listeners and
// dispatching the events to them. It is used by composition, not by inheritance: the bean has one
// and forwards add/remove/fire to it.
//
// Two decisions that are not about convenience but about correctness:
//
// 1. **Notification happens over a copy.** A listener has every right to unsubscribe itself --or to
//    add another-- from inside propertyChange(). If the live list were iterated, that modification
//    would corrupt it mid-walk. Copying before dispatching is what stops the most natural case in
//    the world from breaking anything. Listeners added during a notification do not receive THAT
//    event, and those that leave do receive it: it is the price of the copy and it is the one the
//    JDK charges.
//
// 2. **The listeners registered by name are stored wrapped in a proxy.**
//    getPropertyChangeListeners() has to return both kinds of listener in a single array; wrapping
//    those tied to a property in a PropertyChangeListenerProxy is what allows telling them apart
//    when reading them back.
//
// The synchronization goes at method level and not in blocks: in this tree a block `synchronized`
// with an early `return` does not emit the monitorexit (finding #105), so the methods touching the
// structures are `synchronized` whole.
public class PropertyChangeSupport implements Serializable {

    // The bean that appears as the events' `source`. The JDK also rejects null here: an event with
    // no source is no use to any listener.
    private Object source;

    // Listeners registered with no name: they receive everything.
    private List<PropertyChangeListener> global;

    // Listeners tied to a property, by name.
    private Map<String, List<PropertyChangeListener>> byName;

    public PropertyChangeSupport(Object sourceBean) {
        if (sourceBean == null) {
            throw new NullPointerException();
        }
        this.source = sourceBean;
        this.global = new ArrayList<PropertyChangeListener>();
        this.byName = new HashMap<String, List<PropertyChangeListener>>();
    }

    // It registers a listener for every property.
    //
    // If a PropertyChangeListenerProxy comes in, it is unwrapped and registered under its name: that
    // way getPropertyChangeListeners()'s result can be handed back to addPropertyChangeListener()
    // and rebuild the same register, which is what anyone cloning a bean expects.
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
            this.addByName(proxy.getPropertyName(), proxy.getListener());
        } else {
            this.global.add(listener);
        }
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof PropertyChangeListenerProxy) {
            PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
            this.removeByName(proxy.getPropertyName(), proxy.getListener());
        } else {
            this.global.remove(listener);
        }
    }

    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        this.addByName(propertyName, listener);
    }

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        this.removeByName(propertyName, listener);
    }

    private void addByName(String propertyName, PropertyChangeListener listener) {
        List<PropertyChangeListener> l = this.byName.get(propertyName);
        if (l == null) {
            l = new ArrayList<PropertyChangeListener>();
            this.byName.put(propertyName, l);
        }
        l.add(listener);
    }

    private void removeByName(String propertyName, PropertyChangeListener listener) {
        List<PropertyChangeListener> l = this.byName.get(propertyName);
        if (l != null) {
            l.remove(listener);
            if (l.isEmpty()) {
                this.byName.remove(propertyName);
            }
        }
    }

    // Every listener: the global ones as they stand, and those tied to a property wrapped in a
    // proxy saying which one.
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        List<PropertyChangeListener> salida = new ArrayList<PropertyChangeListener>();
        for (int i = 0; i < this.global.size(); i++) {
            salida.add(this.global.get(i));
        }
        Object[] names = this.byName.keySet().toArray();
        for (int i = 0; i < names.length; i++) {
            String name = (String) names[i];
            List<PropertyChangeListener> l = this.byName.get(name);
            for (int j = 0; j < l.size(); j++) {
                salida.add(new PropertyChangeListenerProxy(name, l.get(j)));
            }
        }
        return this.asArray(salida);
    }

    // Only the listeners tied to `propertyName`, unwrapped: whoever asked already knows the
    // name.
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        List<PropertyChangeListener> salida = new ArrayList<PropertyChangeListener>();
        if (propertyName != null) {
            List<PropertyChangeListener> l = this.byName.get(propertyName);
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    salida.add(l.get(i));
                }
            }
        }
        return this.asArray(salida);
    }

    private PropertyChangeListener[] asArray(List<PropertyChangeListener> l) {
        PropertyChangeListener[] a = new PropertyChangeListener[l.size()];
        for (int i = 0; i < l.size(); i++) {
            a[i] = l.get(i);
        }
        return a;
    }

    // The copy that gets dispatched: the global ones plus those tied to the event's name. It is
    // built under the lock and walked outside it, which is exactly what makes unsubscribing from
    // inside a listener safe.
    private synchronized PropertyChangeListener[] instantanea(String propertyName) {
        List<PropertyChangeListener> salida = new ArrayList<PropertyChangeListener>();
        for (int i = 0; i < this.global.size(); i++) {
            salida.add(this.global.get(i));
        }
        if (propertyName != null) {
            List<PropertyChangeListener> l = this.byName.get(propertyName);
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    salida.add(l.get(i));
                }
            }
        }
        return this.asArray(salida);
    }

    public void firePropertyChange(PropertyChangeEvent evt) {
        Object viejo = evt.getOldValue();
        Object fresh = evt.getNewValue();
        // Two known, equal values are not a change. If either is null it is not known, and when in
        // doubt it notifies.
        if (viejo == null || fresh == null || !viejo.equals(fresh)) {
            PropertyChangeListener[] copy = this.instantanea(evt.getPropertyName());
            for (int i = 0; i < copy.length; i++) {
                copy[i].propertyChange(evt);
            }
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            this.firePropertyChange(new PropertyChangeEvent(this.source, propertyName, oldValue, newValue));
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (oldValue != newValue) {
            this.firePropertyChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            this.firePropertyChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            this.firePropertyChange(
                new IndexedPropertyChangeEvent(this.source, propertyName, oldValue, newValue, index));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (oldValue != newValue) {
            this.fireIndexedPropertyChange(propertyName, index, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            this.fireIndexedPropertyChange(propertyName, index, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    // Whether anyone is listening to this property. It serves to skip the work of computing the
    // old value when there is nobody to tell it to.
    public synchronized boolean hasListeners(String propertyName) {
        boolean any = !this.global.isEmpty();
        if (!any && propertyName != null) {
            List<PropertyChangeListener> l = this.byName.get(propertyName);
            any = l != null && !l.isEmpty();
        }
        return any;
    }
}
