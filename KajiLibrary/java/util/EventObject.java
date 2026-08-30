package java.util;

import java.io.Serializable;

// La raiz de todo evento: lleva el objeto que lo origino y nada mas.
//
// `source` es `transient` y aun asi el objeto es `Serializable`: es deliberado en el JDK y vale
// la pena entenderlo. Serializar un evento no debe arrastrar consigo al componente que lo
// disparo —una ventana, una conexion— que casi nunca es serializable y casi nunca tiene sentido
// mandar. Un EventObject deserializado tiene `source` en null.
public class EventObject implements Serializable {

    // El objeto sobre el que ocurrio el evento.
    protected transient Object source;

    // Un evento originado en `source`.
    public EventObject(Object source) {
        if (source == null) {
            throw new IllegalArgumentException("null source");
        }
        this.source = source;
    }

    // El objeto que origino el evento.
    public Object getSource() {
        return this.source;
    }

    public String toString() {
        return this.getClass().getName() + "[source=" + this.source + "]";
    }
}
