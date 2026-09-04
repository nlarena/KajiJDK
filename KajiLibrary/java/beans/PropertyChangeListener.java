package java.beans;

import java.util.EventListener;

// Se entera de que una propiedad ligada (`bound`) cambio. El cambio YA ocurrio: esto notifica,
// no autoriza. Para poder rechazarlo esta VetoableChangeListener.
public interface PropertyChangeListener extends EventListener {

    void propertyChange(PropertyChangeEvent evt);
}
