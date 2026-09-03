package java.util.prefs;

import java.util.EventObject;

// Una clave de un nodo cambio: quien es el nodo, cual es la clave y con que quedo.
//
// `getNewValue()` devuelve `null` cuando la clave se borro, y esa es la unica manera de distinguir
// un borrado de una escritura. No hay `getOldValue()`: el JDK no lo expone porque el aviso se arma
// despues de aplicar el cambio y el valor anterior ya no esta en ningun lado que se pueda consultar
// sin volver a tocar el deposito.
//
// **Sobre serializar.** Hereda de `EventObject`, que es `Serializable`, pero un evento de
// preferencias no se puede serializar: arrastraria el nodo, y un nodo es una posicion en un arbol
// vivo, no un dato. El JDK lo resuelve con un `writeObject` privado que tira
// `NotSerializableException`; aca la clase simplemente nunca se serializa con exito porque
// `Preferences` no es `Serializable`.
public class PreferenceChangeEvent extends EventObject {

    private static final long serialVersionUID = 793724513368024975L;

    private final String key;
    private final String newValue;

    // El aviso de que `key` quedo valiendo `newValue` en `node`. `newValue` en `null` significa que
    // la clave se borro.
    public PreferenceChangeEvent(Preferences node, String key, String newValue) {
        super(node);
        this.key = key;
        this.newValue = newValue;
    }

    // El nodo donde ocurrio el cambio.
    public Preferences getNode() {
        return (Preferences) getSource();
    }

    // La clave que cambio.
    public String getKey() {
        return key;
    }

    // El valor nuevo, o `null` si la clave se borro.
    public String getNewValue() {
        return newValue;
    }
}
