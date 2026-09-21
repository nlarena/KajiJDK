package java.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// PropertyChangeSupport's vetoable twin, and the difference is not cosmetic: here the notification
// can FAIL, and then it has to be undone.
//
// fireVetoableChange runs the listeners in order; if one throws PropertyVetoException, those that
// had already said yes are left believing the change is going ahead. That is why they are notified
// again, with the values swapped, so that they revert whatever they did -- and only then is the veto
// propagated. Without that second round a veto would leave the rest of the system out of step.
//
// Just as in PropertyChangeSupport, dispatch happens over a copy: a listener may unsubscribe from
// inside vetoableChange().
public class VetoableChangeSupport implements Serializable {

    private Object source;
    private List<VetoableChangeListener> global;
    private Map<String, List<VetoableChangeListener>> byName;

    public VetoableChangeSupport(Object sourceBean) {
        if (sourceBean == null) {
            throw new NullPointerException();
        }
        this.source = sourceBean;
        this.global = new ArrayList<VetoableChangeListener>();
        this.byName = new HashMap<String, List<VetoableChangeListener>>();
    }

    public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof VetoableChangeListenerProxy) {
            VetoableChangeListenerProxy proxy = (VetoableChangeListenerProxy) listener;
            this.addByName(proxy.getPropertyName(), proxy.getListener());
        } else {
            this.global.add(listener);
        }
    }

    public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (listener instanceof VetoableChangeListenerProxy) {
            VetoableChangeListenerProxy proxy = (VetoableChangeListenerProxy) listener;
            this.removeByName(proxy.getPropertyName(), proxy.getListener());
        } else {
            this.global.remove(listener);
        }
    }

    public synchronized void addVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        this.addByName(propertyName, listener);
    }

    public synchronized void removeVetoableChangeListener(String propertyName, VetoableChangeListener listener) {
        if (listener == null || propertyName == null) {
            return;
        }
        this.removeByName(propertyName, listener);
    }

    private void addByName(String propertyName, VetoableChangeListener listener) {
        List<VetoableChangeListener> l = this.byName.get(propertyName);
        if (l == null) {
            l = new ArrayList<VetoableChangeListener>();
            this.byName.put(propertyName, l);
        }
        l.add(listener);
    }

    private void removeByName(String propertyName, VetoableChangeListener listener) {
        List<VetoableChangeListener> l = this.byName.get(propertyName);
        if (l != null) {
            l.remove(listener);
            if (l.isEmpty()) {
                this.byName.remove(propertyName);
            }
        }
    }

    public synchronized VetoableChangeListener[] getVetoableChangeListeners() {
        List<VetoableChangeListener> out = new ArrayList<VetoableChangeListener>();
        for (int i = 0; i < this.global.size(); i++) {
            out.add(this.global.get(i));
        }
        Object[] names = this.byName.keySet().toArray();
        for (int i = 0; i < names.length; i++) {
            String name = (String) names[i];
            List<VetoableChangeListener> l = this.byName.get(name);
            for (int j = 0; j < l.size(); j++) {
                out.add(new VetoableChangeListenerProxy(name, l.get(j)));
            }
        }
        return this.asArray(out);
    }

    public synchronized VetoableChangeListener[] getVetoableChangeListeners(String propertyName) {
        List<VetoableChangeListener> out = new ArrayList<VetoableChangeListener>();
        if (propertyName != null) {
            List<VetoableChangeListener> l = this.byName.get(propertyName);
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    out.add(l.get(i));
                }
            }
        }
        return this.asArray(out);
    }

    private VetoableChangeListener[] asArray(List<VetoableChangeListener> l) {
        VetoableChangeListener[] a = new VetoableChangeListener[l.size()];
        for (int i = 0; i < l.size(); i++) {
            a[i] = l.get(i);
        }
        return a;
    }

    private synchronized VetoableChangeListener[] snapshot(String propertyName) {
        List<VetoableChangeListener> out = new ArrayList<VetoableChangeListener>();
        for (int i = 0; i < this.global.size(); i++) {
            out.add(this.global.get(i));
        }
        if (propertyName != null) {
            List<VetoableChangeListener> l = this.byName.get(propertyName);
            if (l != null) {
                for (int i = 0; i < l.size(); i++) {
                    out.add(l.get(i));
                }
            }
        }
        return this.asArray(out);
    }

    // It asks the listeners and, if one vetoes, reverts those that had already accepted before
    // letting the exception out.
    public void fireVetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        Object old = evt.getOldValue();
        Object fresh = evt.getNewValue();
        if (old == null || fresh == null || !old.equals(fresh)) {
            VetoableChangeListener[] copy = this.snapshot(evt.getPropertyName());
            int i = 0;
            PropertyVetoException veto = null;
            while (i < copy.length && veto == null) {
                try {
                    copy[i].vetoableChange(evt);
                    i = i + 1;
                } catch (PropertyVetoException e) {
                    veto = e;
                }
            }
            if (veto != null) {
                // `i` is left at the one that vetoed: [0, i) have to be undone.
                PropertyChangeEvent rollback = new PropertyChangeEvent(
                    evt.getSource(), evt.getPropertyName(), fresh, old);
                for (int j = 0; j < i; j++) {
                    try {
                        copy[j].vetoableChange(rollback);
                    } catch (PropertyVetoException ignored) {
                        // Vetoing the reversion has nowhere to go: the change was not made
                        // anyway.
                    }
                }
                throw veto;
            }
        }
    }

    public void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
            throws PropertyVetoException {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            this.fireVetoableChange(new PropertyChangeEvent(this.source, propertyName, oldValue, newValue));
        }
    }

    public void fireVetoableChange(String propertyName, int oldValue, int newValue)
            throws PropertyVetoException {
        if (oldValue != newValue) {
            this.fireVetoableChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void fireVetoableChange(String propertyName, boolean oldValue, boolean newValue)
            throws PropertyVetoException {
        if (oldValue != newValue) {
            this.fireVetoableChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    public synchronized boolean hasListeners(String propertyName) {
        boolean any = !this.global.isEmpty();
        if (!any && propertyName != null) {
            List<VetoableChangeListener> l = this.byName.get(propertyName);
            any = l != null && !l.isEmpty();
        }
        return any;
    }
}
