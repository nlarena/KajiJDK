package java.security;

import java.io.Serializable;

// The permission with a hierarchical name and a wildcard, which is the shape almost all of them
// use.
//
// A name is a string separated by dots —`java.home`, `os.name`— and `*` at the end of a segment
// means "and everything that hangs from it". The three forms:
//
//   "*"            implies everything
//   "java.*"       implies "java.home", "java.version", "java.a.b" — but NOT plain "java"
//   "java.home"    implies only itself
//
// The detail that `"java.*"` does **not** imply `"java"` is of the contract and not an arbitrary
// choice: the wildcard replaces a segment that exists, and `"java"` does not have that segment. A
// permission that says "everything below" should not give access to the node above.
//
// It has no actions: `getActions()` returns "". A subclass that needs them —such as
// `PropertyPermission`, with read/write— adds them itself.
public abstract class BasicPermission extends Permission implements Serializable {

    // The name without the trailing `*` if it had one; "" for the universal wildcard.
    private transient String path;

    // Whether the name ended in a `*` that counts as a wildcard.
    private transient boolean wildcard;

    // Whether the name is the bare `"exitVM"` of before 1.6. See `init`.
    private transient boolean exitVM;

    // A permission with the given name.
    public BasicPermission(String name) {
        super(name);
        this.init(name);
    }

    // A permission with the given name. `actions` is ignored: this class does not use them, and the
    // constructor exists so that the subclasses can chain and for deserialising.
    public BasicPermission(String name, String actions) {
        super(name);
        this.init(name);
    }

    // It splits the name into path and wildcard.
    //
    // The `*` only counts as a wildcard if it is the whole name or comes preceded by a dot:
    // `"a.b*"` is a literal name that ends in an asterisk, not a wildcard over `"a.b"`.
    private void init(String name) {
        if (name == null) {
            throw new NullPointerException("name can't be null");
        }
        int len = name.length();
        if (len == 0) {
            throw new IllegalArgumentException("name can't be empty");
        }
        char last = name.charAt(len - 1);
        if (last == '*' && (len == 1 || name.charAt(len - 2) == '.')) {
            this.wildcard = true;
            if (len == 1) {
                this.path = "";
            } else {
                this.path = name.substring(0, len - 1);
            }
        } else if (name.equals("exitVM")) {
            // The only exception to the rule, and it comes from an old compatibility: until 1.6 the
            // permission to end the VM was called plain `"exitVM"`, and afterwards it became
            // `"exitVM.<code>"` with `"exitVM.*"` for any of them. Both names have to go on meaning
            // the same, so the old one is parsed as if it were the wildcard: without this, an
            // `"exitVM.*"` would not imply an `"exitVM"` and a policy written before 1.6 would stop
            // being valid.
            //
            // It lives here and not in `RuntimePermission` —which is the only class where the name
            // appears— because the JDK put it here, and moving it would change which permission it
            // applies to.
            this.wildcard = true;
            this.path = "exitVM.";
            this.exitVM = true;
        } else {
            this.path = name;
        }
    }

    // Whether this permission implies the other.
    //
    // The class first: two permissions of different classes never imply each other, even if the
    // name coincides. A `PropertyPermission("x")` does not give a `RuntimePermission("x")`.
    public boolean implies(Permission p) {
        if (p == null || p.getClass() != this.getClass()) {
            return false;
        }
        BasicPermission that = (BasicPermission) p;
        if (this.wildcard) {
            if (that.wildcard) {
                // "a.*" implies "a.b.*"
                return that.path.startsWith(this.path);
            }
            // "a.*" implies "a.b" but not "a"
            return that.path.length() > this.path.length() && that.path.startsWith(this.path);
        }
        if (that.wildcard) {
            // a concrete name never implies a wildcard
            return false;
        }
        return this.path.equals(that.path);
    }

    // Equality by class and canonical name.
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        BasicPermission bp = (BasicPermission) obj;
        return this.getName().equals(bp.getName());
    }

    public int hashCode() {
        return this.getName().hashCode();
    }

    // "" — this class has no actions.
    public String getActions() {
        return "";
    }

    // A collection that knows how to resolve `implies` over hierarchical names without walking it
    // all.
    public PermissionCollection newPermissionCollection() {
        return new BasicPermissionCollection(this.getClass());
    }

    // The name as it was left after parsing the wildcard. Package-private, as in the JDK.
    //
    // It is the name the collection indexes by, and that is why the old `"exitVM"` has to be
    // canonicalised as `"exitVM.*"`: both names are the same permission and must land in the same
    // entry.
    final String getCanonicalName() {
        if (this.exitVM) {
            return "exitVM.*";
        }
        if (this.wildcard) {
            return this.path + "*";
        }
        return this.path;
    }
}
