package java.security;

// Un objeto que puede vigilar el acceso a otro.
//
// Es la interfaz mas chica del paquete y la que explica su forma: `Permission` la implementa, asi
// que **un permiso es su propio guardia**. Un `GuardedObject` no necesita saber que clase de
// control se le pide — le pasa el objeto al guardia y este decide.
public interface Guard {

    // Determina si se permite el acceso a `object`. Lanza SecurityException si no.
    void checkGuard(Object object) throws SecurityException;
}
