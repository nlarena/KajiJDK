package java.util.prefs;

import java.util.EventObject;

// A un nodo le nacio o se le murio un hijo: quien es el padre y quien el hijo.
//
// El mismo evento sirve para las dos cosas --lo que las distingue es a cual de los dos metodos de
// {@link NodeChangeListener} llego-- y por eso no hay ninguna bandera adentro que decir si fue alta
// o baja.
//
// Cuidado con el hijo de un `childRemoved`: llega **ya borrado**, asi que casi todo lo que se le
// pregunte va a tirar `IllegalStateException`. Lo unico seguro de leerle es `name()` y
// `absolutePath()`, que no consultan el deposito.
public class NodeChangeEvent extends EventObject {

    private static final long serialVersionUID = 8068949086596572957L;

    private final Preferences child;

    // El aviso de que `child` nacio de --o murio bajo-- `parent`.
    public NodeChangeEvent(Preferences parent, Preferences child) {
        super(parent);
        this.child = child;
    }

    // El nodo padre.
    public Preferences getParent() {
        return (Preferences) getSource();
    }

    // El hijo que se agrego o se quito.
    public Preferences getChild() {
        return child;
    }
}
