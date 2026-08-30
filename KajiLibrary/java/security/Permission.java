package java.security;

import java.io.Serializable;

// KajiLibrary's java.security.Permission -- the abstract base of every permission: a named
// capability with a set of actions and an implies() relation. It is also a Guard (checkGuard asks
// the security manager to check it). KajiJDK never installs a security manager, so checkGuard is a
// no-op; the class is here as the parameter type of SecurityManager.checkPermission and as the base
// concrete permissions extend.
public abstract class Permission implements Guard, Serializable {

    private final String name;

    public Permission(String name) {
        this.name = name;
    }

    /**
     * Guards access by asking the security manager to check this permission. KajiJDK installs no
     * security manager, so nothing is ever denied.
     */
    public void checkGuard(Object object) throws SecurityException {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(this);
        }
    }

    /** Whether this permission implies {@code permission}. */
    public abstract boolean implies(Permission permission);

    public abstract boolean equals(Object obj);

    public abstract int hashCode();

    /** This permission's name. */
    public final String getName() {
        return this.name;
    }

    /** This permission's actions, as a canonical string. */
    public abstract String getActions();

    /** A collection suited to holding permissions of this kind, or null for the default. */
    public PermissionCollection newPermissionCollection() {
        return null;
    }

    public String toString() {
        String actions = this.getActions();
        if (actions == null || actions.length() == 0) {
            return "(\"" + this.getClass().getName() + "\" \"" + this.name + "\")";
        }
        return "(\"" + this.getClass().getName() + "\" \"" + this.name + "\" \"" + actions + "\")";
    }
}
