package java.io;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

// The collection `FilePermission.newPermissionCollection()` returns. Package-private: the contract
// only promises a `PermissionCollection`, and the name is not API.
//
// It walks instead of indexing, and the why is in `newPermissionCollection`'s javadoc: a `/a/-`
// covers paths of any depth, so there is no handful of keys to look up.
//
// `implies` is the disjunction of its members' `implies` and **not** their union: having
// `("/tmp/x", "read")` and `("/tmp/x", "write")` does not give `("/tmp/x", "read,write")` here,
// just as in the JDK. Combining masks across different permissions would be granting something
// nobody wrote.
final class FilePermissionCollection extends PermissionCollection {

    private final List<Permission> permissions = new ArrayList<Permission>();

    FilePermissionCollection() {
    }

    /**
     * @throws IllegalArgumentException if it is not a `FilePermission` -- mixing classes would make
     *     the collection answer for permissions it does not understand
     * @throws SecurityException if it has already been marked read-only
     */
    public void add(Permission permission) {
        if (!(permission instanceof FilePermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException("attempt to add a Permission to a readonly PermissionCollection");
        }
        synchronized (this) {
            this.permissions.add(permission);
        }
    }

    public boolean implies(Permission permission) {
        if (!(permission instanceof FilePermission)) {
            return false;
        }
        synchronized (this) {
            int i = 0;
            while (i < this.permissions.size()) {
                if (this.permissions.get(i).implies(permission)) {
                    return true;
                }
                i = i + 1;
            }
        }
        return false;
    }

    public Enumeration<Permission> elements() {
        synchronized (this) {
            return Collections.enumeration(new ArrayList<Permission>(this.permissions));
        }
    }
}
