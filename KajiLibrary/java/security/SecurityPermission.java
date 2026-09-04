package java.security;

// Permiso sobre el propio subsistema de seguridad: agregar proveedores, tocar la politica, leer o
// escribir propiedades de `Security`.
//
// Es el permiso que hay que mirar dos veces en una politica, porque casi todos sus nombres son
// escaleras: `insertProvider` deja meter un proveedor propio y por lo tanto reimplementar
// cualquier algoritmo; `setPolicy` deja reescribir el resto de los permisos. Darlo equivale a dar
// `AllPermission` por un camino mas largo.
//
// Sin acciones —hereda el "" de `BasicPermission`— y `final`, como en el JDK: el conjunto de
// nombres es del sistema y una subclase que lo ampliara estaria inventando autoridad.
public final class SecurityPermission extends BasicPermission {

    public SecurityPermission(String name) {
        super(name);
    }

    // `actions` se ignora; existe para que el cargador de politicas pueda construirlo por
    // reflexion con la misma firma que cualquier otro permiso.
    public SecurityPermission(String name, String actions) {
        super(name, actions);
    }
}
