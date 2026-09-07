package java.beans;

import java.util.EventListener;

// It hears that a bound property has changed. The change has ALREADY happened: this notifies, it
// does not authorize. For being able to refuse it there is VetoableChangeListener.
public interface PropertyChangeListener extends EventListener {

    void propertyChange(PropertyChangeEvent evt);
}
