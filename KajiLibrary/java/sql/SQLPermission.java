package java.sql;

/**
 * KajiLibrary's java.sql.SQLPermission -- el permiso para las operaciones sensibles de JDBC.
 *
 * <p>Los nombres que reconoce son `setLog`, `callAbort`, `setSyncFactory`, `setNetworkTimeout` y
 * `deregisterDriver`: todas operaciones que un codigo poco confiable podria usar para espiar o
 * cortar las conexiones de otro.
 *
 * <p>Hereda de `BasicPermission`, o sea que admite el comodin `*` y el sufijo `.*`. No tiene acciones
 * -- el permiso es o no es.
 */
public final class SQLPermission extends java.security.BasicPermission {

    public SQLPermission(String name) {
        super(name);
    }

    /** El de arriba; `actions` se ignora, que es lo que hace `BasicPermission`. */
    public SQLPermission(String name, String actions) {
        super(name, actions);
    }
}
