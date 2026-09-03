package java.util.prefs;

import java.util.EventListener;

// Quien quiera enterarse de que una clave de un nodo cambio de valor.
//
// Se escucha **un nodo**, no un subarbol: los cambios de los hijos no llegan aca. Es deliberado --
// propagar hacia arriba obligaria a notificar la raiz por cualquier cambio en cualquier lado.
public interface PreferenceChangeListener extends EventListener {

    // Una clave de `evt.getNode()` se agrego, cambio o se borro. En el borrado
    // {@link PreferenceChangeEvent#getNewValue} devuelve `null`.
    void preferenceChange(PreferenceChangeEvent evt);
}
