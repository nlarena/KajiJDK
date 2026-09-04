package java.security;

import java.io.Serializable;

// Un objeto detras de un guardia: para tenerlo hay que pasar por `getObject()`.
//
// Es el patron opuesto al de chequear el permiso antes de entregar la referencia. Ahi el chequeo
// pasa una vez, cuando se entrega; aca pasa **cada vez que alguien lo desenvuelve**, y eso importa
// porque la referencia puede viajar: se la puede guardar, serializar y mandar a otro lado, y el
// guardia sigue pegado a ella.
public class GuardedObject implements Serializable {

    private final Object object;
    private final Guard guard;

    public GuardedObject(Object object, Guard guard) {
        this.object = object;
        this.guard = guard;
    }

    // El objeto, si el guardia lo permite.
    public Object getObject() throws SecurityException {
        if (this.guard != null) {
            this.guard.checkGuard(this.object);
        }
        return this.object;
    }
}
