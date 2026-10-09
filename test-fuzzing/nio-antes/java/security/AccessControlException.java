package java.security;

// An access was denied, and optionally which permission would have been needed.
//
// The only thing it adds over `SecurityException` is `getPermission()`, and that datum is the
// difference between a useless message and an actionable one: whoever catches it can say exactly
// which line of policy is missing. In KajiLibrary nothing throws it by itself —there is no active
// access control— but it can be built and it goes on being the type the signatures name.
@Deprecated
public class AccessControlException extends SecurityException {

    // The permission that was missing, or null if whoever threw it did not say.
    private final Permission perm;

    public AccessControlException(String s) {
        super(s);
        this.perm = null;
    }

    public AccessControlException(String s, Permission p) {
        super(s);
        this.perm = p;
    }

    // The permission that would have been needed, or null.
    public Permission getPermission() {
        return this.perm;
    }
}
