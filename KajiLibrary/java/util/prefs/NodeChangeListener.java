package java.util.prefs;

import java.util.EventListener;

// Quien quiera enterarse de que a un nodo le nacio o se le murio un hijo.
//
// Igual que con las claves, se escucha **un nivel**: que a un nieto le agreguen un hijo no llega
// aca.
public interface NodeChangeListener extends EventListener {

    // Nacio un hijo de `evt.getParent()`. Ojo: `Preferences.node()` crea el nodo si no existia, asi
    // que este aviso puede salir de una llamada que parecia solo una consulta.
    void childAdded(NodeChangeEvent evt);

    // Se borro un hijo de `evt.getParent()`.
    void childRemoved(NodeChangeEvent evt);
}
