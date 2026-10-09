package java.security;

import java.util.Enumeration;

// A set of identities, which is in turn **an** identity.
//
// That a scope is also an identity is what allows them to be nested: a scope has a name, may have a
// key, and may live inside another. With that a hierarchy of name spaces was built —the scope of
// the system, that of a user, that of an application— where the full name of an identity places it
// unambiguously.
//
// Obsolete since 1.2, replaced by `KeyStore`. The invariant it imposes —inside a scope there cannot
// be two identities with the same name or two with the same key— is enforced by the subclasses in
// `addIdentity`; this class cannot, because it keeps nothing: every method that touches the
// collection is abstract.
@Deprecated
public abstract class IdentityScope extends Identity {

    // The scope of the system. In KajiLibrary nobody installs it, so it starts —and stays— at null
    // while nobody calls `setSystemScope`. Returning null is right: there is none.
    private static IdentityScope scope;

    protected IdentityScope() {
        this("restoring...");
    }

    public IdentityScope(String name) {
        super(name);
    }

    public IdentityScope(String name, IdentityScope scope) throws KeyManagementException {
        super(name, scope);
    }

    // The scope of the system, or null if there is none installed.
    public static IdentityScope getSystemScope() {
        return scope;
    }

    // It installs the scope of the system. `protected` and static: only a subclass can do it, which
    // was the way of keeping just anybody from replacing the identity store of the process.
    protected static void setSystemScope(IdentityScope scope) {
        IdentityScope.scope = scope;
    }

    // How many identities there are.
    public abstract int size();

    public abstract Identity getIdentity(String name);

    // By principal: it is resolved by name.
    //
    // Concrete and not abstract because it contributes nothing new — a `Principal` is a name— and
    // forcing it to be implemented would give room for a subclass to do it differently from
    // `getIdentity(String)` by accident.
    public Identity getIdentity(Principal principal) {
        return this.getIdentity(principal.getName());
    }

    public abstract Identity getIdentity(PublicKey key);

    public abstract void addIdentity(Identity identity) throws KeyManagementException;

    public abstract void removeIdentity(Identity identity) throws KeyManagementException;

    public abstract Enumeration<Identity> identities();

    @Override
    public String toString() {
        return super.toString() + "[" + this.size() + "]";
    }
}
