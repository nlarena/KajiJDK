package java.security;

// The unit the decision is made about: where the code came from, who loads it, with which identity
// it runs, and which permissions it has.
//
// It is the subject of the whole access control question. A permission is not granted "to a class":
// it is granted to a domain, and every class that shares origin, loader and principals shares a
// domain and therefore permissions.
//
// ===============================================================================================
// STATIC VS. DYNAMIC PERMISSIONS
// ===============================================================================================
//
// The difference between the two constructors is the most important one of the class and is hidden
// in a boolean with no visible name:
//
//   - The **two**-argument one creates a domain of **static** permissions: the ones it was passed
//     and nothing else, for ever. `staticPermissionsOnly()` gives `true`.
//   - The **four**-argument one creates a **dynamic** one: the ones it was passed **plus** the ones
//     the `Policy` in force grants it at the moment of asking. `staticPermissionsOnly()` gives
//     `false`.
//
// Refreshing the policy changes what a dynamic domain can do and does not touch a static one. It is
// the only way for a change of policy to have an effect over code that is already loaded.
//
// The collection of permissions is marked **read-only** when building the domain: if not, whoever
// handed over the permissions could add more to themselves after the domain was accepted.
//
// (In KajiJDK there is no `Policy` with contents, so the dynamic part always adds zero. The
// distinction is implemented all the same because it is observable contract:
// `staticPermissionsOnly()` answers differently depending on which constructor was used.)
public class ProtectionDomain {

    // null means "unknown origin", and implies nothing.
    private final CodeSource codesource;

    private final PermissionCollection permissions;

    private final ClassLoader classloader;

    // Never null: a domain with no principals has an empty array, not null. It simplifies things
    // for everybody who walks it.
    private final Principal[] principals;

    // A shortcut: if the permissions include `AllPermission` already, there is no need to consult
    // anything else.
    private final boolean hasAllPerm;

    private final boolean staticPermissions;

    // A domain of static permissions.
    public ProtectionDomain(CodeSource codesource, PermissionCollection permissions) {
        this.codesource = codesource;
        this.permissions = permissions;
        this.hasAllPerm = sealAndDetectAll(permissions);
        this.classloader = null;
        this.principals = new Principal[0];
        this.staticPermissions = true;
    }

    // A domain of dynamic permissions: to the given ones are added the ones the policy grants.
    public ProtectionDomain(CodeSource codesource, PermissionCollection permissions,
                            ClassLoader classloader, Principal[] principals) {
        this.codesource = codesource;
        this.permissions = permissions;
        this.hasAllPerm = sealAndDetectAll(permissions);
        this.classloader = classloader;
        this.principals = principals == null ? new Principal[0] : copyOf(principals);
        this.staticPermissions = false;
    }

    private static Principal[] copyOf(Principal[] a) {
        Principal[] c = new Principal[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    // It closes the collection and tells whether it brings the universal permission.
    private static boolean sealAndDetectAll(PermissionCollection pc) {
        if (pc == null) {
            return false;
        }
        pc.setReadOnly();
        if (pc instanceof Permissions) {
            return ((Permissions) pc).allPermission != null;
        }
        return false;
    }

    public final CodeSource getCodeSource() {
        return this.codesource;
    }

    // The loader of this domain, or null if the classes were defined by the bootstrap loader.
    public final ClassLoader getClassLoader() {
        return this.classloader;
    }

    public final Principal[] getPrincipals() {
        return copyOf(this.principals);
    }

    // The static collection of permissions, or null. It does not include what the policy may grant:
    // that is what `implies` is for.
    public final PermissionCollection getPermissions() {
        return this.permissions;
    }

    // Whether this domain ignores the policy.
    public final boolean staticPermissionsOnly() {
        return this.staticPermissions;
    }

    // Whether this domain has the permission asked for.
    public boolean implies(Permission perm) {
        if (this.hasAllPerm) {
            return true;
        }
        if (!this.staticPermissions && Policy.getPolicy().implies(this, perm)) {
            return true;
        }
        if (this.permissions != null) {
            return this.permissions.implies(perm);
        }
        return false;
    }

    @Override
    public String toString() {
        String pals = "<no principals>";
        if (this.principals.length > 0) {
            StringBuilder b = new StringBuilder("(principals ");
            int i = 0;
            while (i < this.principals.length) {
                b.append(this.principals[i].getClass().getName());
                b.append(" \"");
                b.append(this.principals[i].getName());
                b.append("\"");
                if (i < this.principals.length - 1) {
                    b.append(",\n");
                } else {
                    b.append(")\n");
                }
                i = i + 1;
            }
            pals = b.toString();
        }
        return "ProtectionDomain "
            + " " + this.codesource + "\n"
            + " " + this.classloader + "\n"
            + " " + pals + "\n"
            + " " + this.permissions + "\n";
    }
}
