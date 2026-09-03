package java.io;

import java.security.BasicPermission;

// KajiLibrary's java.io.SerializablePermission -- el permiso para tocar los mecanismos de la
// serializacion.
//
// Los dos nombres que existen dicen bien por que hace falta un permiso:
//
//   - `enableSubclassImplementation`, para subclasear `ObjectOutputStream`/`ObjectInputStream` y
//     sobreescribir como se escriben o se leen los objetos. Quien puede hacerlo puede cambiar lo
//     que un objeto *dice* ser al deserializarse.
//   - `enableSubstitution`, para `enableReplaceObject`/`enableResolveObject`, o sea para cambiar un
//     objeto por otro en pleno vuelo.
//
// Los dos rompen la garantia de que lo que entra a un stream es lo que sale, y por eso son
// permisos y no metodos comunes.
//
// **Sin acciones**: hereda de `BasicPermission`, asi que `implies` es comparacion de nombre con
// comodines (`*` al final) y `getActions()` devuelve la cadena vacia. El constructor de dos
// argumentos existe solo porque el contrato de `Permission` lo pide; ignora el segundo, como en el
// JDK.
public final class SerializablePermission extends BasicPermission {

    public SerializablePermission(String name) {
        super(name);
    }

    /** @param actions no se usa; esta por uniformidad con el resto de los permisos */
    public SerializablePermission(String name, String actions) {
        super(name, actions);
    }
}
