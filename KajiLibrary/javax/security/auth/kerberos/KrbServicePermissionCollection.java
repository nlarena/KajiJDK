package javax.security.auth.kerberos;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Los {@link ServicePermission} de una politica, juntados por servicio.
 *
 * <p>Agregar {@code accept} y despues {@code initiate} sobre el mismo servicio deja <b>un</b> permiso
 * con las dos acciones, no dos permisos; es lo que hace que la coleccion conteste bien a un pedido
 * que necesite las dos a la vez. El que se llame {@code "*"} cubre a todos los demas.
 */
final class KrbServicePermissionCollection extends PermissionCollection {

    private static final long serialVersionUID = -4118834211490102011L;

    /** Un permiso por servicio, en orden de llegada. */
    private final Map<String, ServicePermission> perms =
        new LinkedHashMap<String, ServicePermission>();

    /** Si alguno de la coleccion alcanza. */
    @Override
    public boolean implies(Permission permission) {
        if (!(permission instanceof ServicePermission)) {
            return false;
        }
        ServicePermission wanted = (ServicePermission) permission;
        int needed = wanted.getMask();
        int have = 0;
        synchronized (this) {
            ServicePermission all = this.perms.get("*");
            if (all != null) {
                have = have | all.getMask();
                if ((have & needed) == needed) {
                    return true;
                }
            }
            ServicePermission exact = this.perms.get(wanted.getName());
            if (exact != null) {
                have = have | exact.getMask();
            }
        }
        return (have & needed) == needed;
    }

    /**
     * Agrega, juntando con el que hubiera para el mismo servicio.
     *
     * @throws IllegalArgumentException si no es un {@link ServicePermission}
     * @throws SecurityException si la coleccion es de solo lectura
     */
    @Override
    public void add(Permission permission) {
        if (!(permission instanceof ServicePermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        ServicePermission added = (ServicePermission) permission;
        String name = added.getName();
        synchronized (this) {
            ServicePermission existing = this.perms.get(name);
            if (existing == null) {
                this.perms.put(name, added);
            } else {
                int merged = existing.getMask() | added.getMask();
                if (merged != existing.getMask()) {
                    this.perms.put(name, new ServicePermission(name, merged));
                }
            }
        }
    }

    /** Los permisos, uno por servicio. */
    @Override
    public Enumeration<Permission> elements() {
        List<Permission> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<Permission>(this.perms.values());
        }
        return Collections.enumeration(snapshot);
    }
}
