package java.beans;

import java.util.EventListenerProxy;

// A listener tied to ONE property. It is how PropertyChangeSupport remembers the listeners that
// registered by name: instead of keeping two structures, it wraps the listener together with the
// name and stores it in the same list as the rest.
public class PropertyChangeListenerProxy
        extends EventListenerProxy<PropertyChangeListener>
        implements PropertyChangeListener {

    private String propertyName;

    public PropertyChangeListenerProxy(String propertyName, PropertyChangeListener listener) {
        super(listener);
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    // It delegates as it stands: the filtering by name was already done by whoever dispatches.
    public void propertyChange(PropertyChangeEvent evt) {
        this.getListener().propertyChange(evt);
    }
}
