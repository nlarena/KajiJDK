package java.beans;

import java.util.EventListener;

// Se entera de que una propiedad restringida (`constrained`) esta por cambiar, y puede impedirlo
// tirando PropertyVetoException. A diferencia de PropertyChangeListener, esto corre ANTES del
// cambio y su excepcion lo cancela.
public interface VetoableChangeListener extends EventListener {

    void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException;
}
