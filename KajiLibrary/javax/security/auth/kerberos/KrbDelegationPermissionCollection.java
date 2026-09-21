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
 * The {@link DelegationPermission}s of a policy.
 *
 * <p>Without wildcards or actions there is nothing to merge: it is a set, and adding the same one
 * twice leaves one.
 */
@SuppressWarnings("removal")
final class KrbDelegationPermissionCollection extends PermissionCollection {

    private static final long serialVersionUID = -3383936936589966948L;

    /** The permissions, without repeats, in order of arrival. */
    private final Set<Permission> perms = new LinkedHashSet<Permission>();

    /** Whether any of them is the one asked for. */
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
     * Adds.
     *
     * @throws IllegalArgumentException if it is not a {@link DelegationPermission}
     * @throws SecurityException if the collection is read-only
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

    /** The permissions. */
    @Override
    public Enumeration<Permission> elements() {
        List<Permission> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<Permission>(this.perms);
        }
        return Collections.enumeration(snapshot);
    }
}
