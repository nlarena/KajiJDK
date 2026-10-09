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
 * The {@link ServicePermission}s of a policy, merged by service.
 *
 * <p>Adding {@code accept} and then {@code initiate} on the same service leaves <b>one</b>
 * permission with both actions, not two permissions; it is what makes the collection answer right
 * to a request that needs both at once. The one named {@code "*"} covers all the others.
 */
final class KrbServicePermissionCollection extends PermissionCollection {

    private static final long serialVersionUID = -4118834211490102011L;

    /** One permission per service, in order of arrival. */
    private final Map<String, ServicePermission> perms =
        new LinkedHashMap<String, ServicePermission>();

    /** Whether any of the collection's is enough. */
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
     * Adds, merging with whatever there was for the same service.
     *
     * @throws IllegalArgumentException if it is not a {@link ServicePermission}
     * @throws SecurityException if the collection is read-only
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

    /** The permissions, one per service. */
    @Override
    public Enumeration<Permission> elements() {
        List<Permission> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<Permission>(this.perms.values());
        }
        return Collections.enumeration(snapshot);
    }
}
