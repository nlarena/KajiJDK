package java.security;

// The provider's face for a `Policy`.
//
// Only `engineImplies` is abstract, and that says it is the minimum needed in order to be a policy:
// answering whether a domain has a permission. The other two —enumerating the permissions of a
// source or of a domain— have a base implementation that returns
// `Policy.UNSUPPORTED_EMPTY_COLLECTION`, because there are policies that know how to decide without
// knowing how to enumerate: a rule such as "everything signed by X can read /var/data" answers
// `implies` instantly and has no finite list to return.
//
// KajiLibrary brings no implementation. See `Policy`: JDK 25 no longer lets a global policy be
// installed.
public abstract class PolicySpi {

    public PolicySpi() {
    }

    protected abstract boolean engineImplies(ProtectionDomain domain, Permission permission);

    // It rereads the policy. The base one has nowhere to read it from.
    protected void engineRefresh() {
    }

    protected PermissionCollection engineGetPermissions(CodeSource codesource) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }

    protected PermissionCollection engineGetPermissions(ProtectionDomain domain) {
        return Policy.UNSUPPORTED_EMPTY_COLLECTION;
    }
}
