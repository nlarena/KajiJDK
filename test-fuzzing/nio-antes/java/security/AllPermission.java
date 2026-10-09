package java.security;

import java.util.Enumeration;
import java.util.NoSuchElementException;

// The permission that implies all the others.
//
// It is the equivalent of root, and its `implies` returns `true` without looking at the argument.
// It is worth keeping in mind when reading a policy: a single `AllPermission` makes everything
// around it irrelevant.
public final class AllPermission extends Permission {

    // The universal permission.
    public AllPermission() {
        super("<all permissions>");
    }

    // The same as the one with no arguments; both parameters are ignored. It exists so that the
    // policy loader can build it by reflection with the same signature as any other permission.
    public AllPermission(String name, String actions) {
        this();
    }

    // Always true. That is the point.
    public boolean implies(Permission p) {
        return true;
    }

    // Every AllPermission is equal to every other: they have no state that tells them apart.
    public boolean equals(Object obj) {
        return obj instanceof AllPermission;
    }

    public int hashCode() {
        return 1;
    }

    // "<all actions>", which is what the JDK returns.
    public String getActions() {
        return "<all actions>";
    }

    public PermissionCollection newPermissionCollection() {
        return new AllPermissionCollection();
    }
}

// The collection of AllPermission. It keeps only whether there is one: one or a thousand are the
// same.
final class AllPermissionCollection extends PermissionCollection {

    private boolean hasAny;

    public void add(Permission permission) {
        if (!(permission instanceof AllPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        this.hasAny = true;
    }

    public boolean implies(Permission permission) {
        return this.hasAny;
    }

    public Enumeration<Permission> elements() {
        return new AllPermEnum(this.hasAny);
    }
}

// An enumeration of zero or one AllPermission.
final class AllPermEnum implements Enumeration<Permission> {

    private boolean pending;

    AllPermEnum(boolean pending) {
        this.pending = pending;
    }

    public boolean hasMoreElements() {
        return this.pending;
    }

    public Permission nextElement() {
        if (!this.pending) {
            throw new NoSuchElementException();
        }
        this.pending = false;
        return new AllPermission();
    }
}
