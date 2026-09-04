package javax.security.auth.kerberos;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Los {@link DelegationPermission} de una politica.
 *
 * <p>Sin comodines ni acciones no hay nada que juntar: es un conjunto, y agregar dos veces el mismo
 * deja uno.
 */
@SuppressWarnings("removal")
final class KrbDelegationPermissionCollection extends PermissionCollection {

    private static final long serialVersionUID = -3383936936589966948L;

    /** Los permisos, sin repetidos, en orden de llegada. */
    private final Set<Permission> perms = new LinkedHashSet<Permission>();

    /** Si alguno es el pedido. */
    @Override
    public boolean implies(Permission permission) {
        if (!(permission instanceof DelegationPermission)) {
            return false;
        }
        synchronized (this) {
            return this.perms.contains(permission);
        }
    }

    /**
     * Agrega.
     *
     * @throws IllegalArgumentException si no es un {@link DelegationPermission}
     * @throws SecurityException si la coleccion es de solo lectura
     */
    @Override
    public void add(Permission permission) {
        if (!(permission instanceof DelegationPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        synchronized (this) {
            this.perms.add(permission);
        }
    }

    /** Los permisos. */
    @Override
    public Enumeration<Permission> elements() {
        List<Permission> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<Permission>(this.perms);
        }
        return Collections.enumeration(snapshot);
    }
}
