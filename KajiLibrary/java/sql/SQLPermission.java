package java.sql;

/**
 * KajiLibrary's java.sql.SQLPermission -- the permission for JDBC's sensitive operations.
 *
 * <p>The names it recognizes are `setLog`, `callAbort`, `setSyncFactory`, `setNetworkTimeout` and
 * `deregisterDriver`: all operations that untrusted code could use to spy on or cut another's
 * connections.
 *
 * <p>It inherits from `BasicPermission`, so it accepts the `*` wildcard and the `.*` suffix. It has
 * no actions -- the permission is or is not.
 */
public final class SQLPermission extends java.security.BasicPermission {

    public SQLPermission(String name) {
        super(name);
    }

    /** The one above; `actions` is ignored, which is what `BasicPermission` does. */
    public SQLPermission(String name, String actions) {
        super(name, actions);
    }
}
