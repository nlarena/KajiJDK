package java.beans;

import java.util.EventListener;

// It hears that a constrained property is about to change, and it can stop it by throwing
// PropertyVetoException. Unlike PropertyChangeListener, this runs BEFORE the change and its
// exception cancels it.
public interface VetoableChangeListener extends EventListener {

    void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException;
}
