package java.security;

// KajiLibrary's java.security.ProtectionDomain (finding #267).
//
// It exists because the API needs the TYPE: `jakarta.persistence.spi.ClassTransformer.transform`
// takes a ProtectionDomain parameter, and without the class the file does not compile. That is
// the honest scope of this class today.
//
// In the JDK a ProtectionDomain is the unit the access controller reasons about: a CodeSource
// (where the code came from), a ClassLoader, a set of Principals, and a PermissionCollection.
// KajiJDK has no security manager, no policy file and no permission model, so three of those four
// have no type to name -- CodeSource, Principal and PermissionCollection do not exist -- and the
// fourth, the ClassLoader, is the one thing we can answer truthfully.
//
// What it deliberately does NOT have: `getCodeSource()`, `getPermissions()`, `getPrincipals()`,
// `implies(Permission)`, `staticPermissionsOnly()`. Every one of them would have to invent a
// value, and an `implies` that answered anything at all would be granting or denying a permission
// on no evidence -- a decision, not a datum. A missing member is a legal subset; a member that
// lies is not.
public class ProtectionDomain {

    private final ClassLoader classLoader;

    /**
     * The domain of code loaded by {@code classLoader}. The JDK's constructors also take a
     * CodeSource, a PermissionCollection and a Principal array; none of those types exist here,
     * so this is the only form.
     *
     * @param classLoader the loader that defined the classes in this domain, or {@code null} for
     *                    the bootstrap loader
     */
    public ProtectionDomain(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * The class loader of this domain, or {@code null} if the classes were defined by the
     * bootstrap loader.
     */
    public final ClassLoader getClassLoader() {
        return this.classLoader;
    }

    @Override
    public String toString() {
        return "ProtectionDomain (" + this.classLoader + ")";
    }
}
