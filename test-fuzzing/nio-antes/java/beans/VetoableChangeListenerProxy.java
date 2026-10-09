package java.beans;

import java.util.EventListenerProxy;

// PropertyChangeListenerProxy's vetoable equivalent: a listener tied to a single property.
public class VetoableChangeListenerProxy
        extends EventListenerProxy<VetoableChangeListener>
        implements VetoableChangeListener {

    private String propertyName;

    public VetoableChangeListenerProxy(String propertyName, VetoableChangeListener listener) {
        super(listener);
        this.propertyName = propertyName;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        this.getListener().vetoableChange(evt);
    }
}
