package java.security;

import java.util.Enumeration;
import java.util.NoSuchElementException;

// The policy: given a domain, which permissions it has.
//
// ===============================================================================================
// THE POLICY IN FORCE IS THE ONE THAT GRANTS NOTHING, AND IT CANNOT BE CHANGED
// ===============================================================================================
//
// This surprises and it is what JDK 25 does, checked against it:
//
//   - `getPolicy()` returns a **non-null** object whose answer to everything is "no". `implies`
//     gives `false` and `getPermissions` returns `UNSUPPORTED_EMPTY_COLLECTION`.
//   - `setPolicy(...)` throws `UnsupportedOperationException`. **A policy cannot be installed.**
//   - `getInstance(...)` throws `NoSuchAlgorithmException`: there is no provider of type "Policy".
//
// That `getPolicy()` does not return null is what lets `ProtectionDomain.implies` consult it
// without wondering about anything; that it grants nothing is what makes consulting it harmless.
// The mechanism was left with its shape intact and its contents emptied, and this class reproduces
// that instead of simulating a policy the JDK no longer lets be installed.
//
// `UNSUPPORTED_EMPTY_COLLECTION` deserves a note apart: it **is not** an ordinary empty collection.
// Its `add` throws `SecurityException` and its `implies` always gives `false`. The difference from
// a normal empty one is semantic — it means "it cannot be answered", not "there are no
// permissions"— and it serves so that a caller who tries to add something to it anyway finds out
// instead of believing they managed it.
public abstract class Policy {

    // The answer when there is no policy to consult. See the header: it denies and does not let
    // itself be modified.
    public static final PermissionCollection UNSUPPORTED_EMPTY_COLLECTION =
        new UnsupportedCollection();

    // The only policy there is. Static and final: `setPolicy` does not change it.
    private static final Policy IN_FORCE = new EmptyPolicy();

    public Policy() {
    }

    // The policy in force. Never null.
    public static Policy getPolicy() {
        return IN_FORCE;
    }

    // It always throws. Installing a global policy stopped being supported when the
    // `SecurityManager` was disabled, and pretending that it was installed would be worse: the
    // caller would believe that their rules are in force.
    public static void setPolicy(Policy p) {
        throw new UnsupportedOperationException(
            "Setting a system-wide Policy object is not supported");
    }

    public static Policy getInstance(String type, Policy.Parameters params)
            throws NoSuchAlgorithmException {
        return lookup(type, null);
    }

    public static Policy getInstance(String type, Policy.Parameters params, String provider)
            throws NoSuchProviderException, NoSuchAlgorithmException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return lookup(type, p);
    }

    public static Policy getInstance(String type, Policy.Parameters params, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        return lookup(type, provider);
    }

    // There is no registered provider of type "Policy", and there will not be: writing a policy
    // provider requires a parser of policy files that this library does not have. The search is
    // made all the same —against whatever providers there are— so that the day one exists, this
    // finds it without anything being touched.
    private static Policy lookup(String type, Provider only) throws NoSuchAlgorithmException {
        if (type == null) {
            throw new NullPointerException("null type name");
        }
        Provider[] provs = only == null ? Security.getProviders() : new Provider[] {only};
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("Policy", type);
            if (s != null) {
                Object o = s.newInstance(null);
                if (o instanceof Policy) {
                    return (Policy) o;
                }
                throw new NoSuchAlgorithmException(
                    "class configured for Policy is not a Policy: " + s.getClassName());
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(type + " Policy not available");
    }

    // The provider it came from, or null if it did not come out of a factory.
    public Provider getProvider() {
        return null;
    }

    public String getType() {
        return null;
    }

    public Policy.Parameters getParameters() {
        return null;
    }

    // The permissions this policy gives that origin.
    public PermissionCollection getPermissions(CodeSource codesource) {
        return UNSUPPORTED_EMPTY_COLLECTION;
    }

    // The permissions this policy gives that domain: the domain's own ones plus the ones that
    // correspond by origin.
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        PermissionCollection own = domain == null ? null : domain.getPermissions();
        PermissionCollection bySource =
            domain == null ? null : this.getPermissions(domain.getCodeSource());
        if (bySource == UNSUPPORTED_EMPTY_COLLECTION && own == null) {
            return UNSUPPORTED_EMPTY_COLLECTION;
        }
        Permissions together = new Permissions();
        addAll(together, own);
        if (bySource != UNSUPPORTED_EMPTY_COLLECTION) {
            addAll(together, bySource);
        }
        return together;
    }

    private static void addAll(Permissions target, PermissionCollection source) {
        if (source == null) {
            return;
        }
        Enumeration<Permission> e = source.elements();
        while (e.hasMoreElements()) {
            target.add(e.nextElement());
        }
    }

    // Whether this policy grants the permission to that domain.
    public boolean implies(ProtectionDomain domain, Permission permission) {
        if (domain == null) {
            return false;
        }
        PermissionCollection pc = this.getPermissions(domain);
        if (pc == null) {
            return false;
        }
        return pc.implies(permission);
    }

    // It rereads the policy from wherever it comes. The base one has nowhere to read it from: it
    // does nothing.
    public void refresh() {
    }

    // It marks the configuration parameters of a policy. Empty, like `AlgorithmParameterSpec`: only
    // the common type is needed.
    public interface Parameters {
    }
}

// The policy that grants nothing. See the header of `Policy`.
final class EmptyPolicy extends Policy {

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        return false;
    }
}

// "It cannot be answered", dressed up as a collection.
//
// It is not the same as an empty collection: `add` throws instead of accepting silently, so that
// whoever believes they are configuring permissions finds out that they are not.
final class UnsupportedCollection extends PermissionCollection {

    @Override
    public void add(Permission permission) {
        throw new SecurityException(
            "attempt to add a Permission to a readonly PermissionCollection");
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public Enumeration<Permission> elements() {
        return new EmptyEnumeration();
    }
}

final class EmptyEnumeration implements Enumeration<Permission> {

    public boolean hasMoreElements() {
        return false;
    }

    public Permission nextElement() {
        throw new NoSuchElementException();
    }
}
