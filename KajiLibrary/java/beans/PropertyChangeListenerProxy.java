package java.beans;

import java.util.EventListenerProxy;

// Un oyente atado a UNA propiedad. Es como PropertyChangeSupport recuerda los oyentes que se
// registraron por nombre: en vez de llevar dos estructuras, envuelve al oyente junto con el
// nombre y lo guarda en la misma lista que los demas.
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

    // Delega tal cual: el filtrado por nombre ya lo hizo quien despacha.
    public void propertyChange(PropertyChangeEvent evt) {
        this.getListener().propertyChange(evt);
    }
}
