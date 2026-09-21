package java.security;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;

// The marker of a permission the policy mentions but whose class could not be loaded yet.
//
// ===============================================================================================
// WHY THIS EXISTS
// ===============================================================================================
//
// The policy is read on starting, when half the classes of permission are not there yet: the ones
// an application brings in its own jar cannot be loaded before the application. There would be two
// bad ways out —failing when reading the policy, or discarding the line— and one good one: keeping
// the **strings** and resolving them when the class appears. This class is that keeping.
//
// ===============================================================================================
// `implies` ALWAYS RETURNS `false`, AND THAT IS RIGHT
// ===============================================================================================
//
// An unresolved permission grants nothing. It is not a limitation of this implementation: an
// `UnresolvedPermission` **does not know** what the permission it represents means, because the one
// that knows is the `implies` of the class that has not been loaded yet. Answering anything other
// than `false` would be granting in advance a permission whose semantics is unknown.
//
// A KajiLibrary subset: the resolution proper is missing —the `resolve` method is package-private
// in the JDK and is called by the policy loader, which does not exist here— because building the
// real permission by reflection has nobody to serve while there is no installable `Policy`. What is
// there is everything needed for carrying the information until somebody can resolve it.
public final class UnresolvedPermission extends Permission implements Serializable {

    private final String type;
    private final String name;
    private final String actions;
    private final Certificate[] certs;

    // `type` is the name of the class of permission; `name` and `actions` are its arguments as they
    // appeared in the policy. `certs` are the certificates the class of permission has to be signed
    // with for the resolution to be accepted — without that condition, anybody who can put a class
    // with that name in the classpath defines what the permission means.
    public UnresolvedPermission(String type, String name, String actions, Certificate[] certs) {
        super(type);
        if (type == null) {
            throw new NullPointerException("type can't be null");
        }
        this.type = type;
        this.name = name;
        this.actions = actions;
        this.certs = certs == null ? null : copyOf(certs);
    }

    private static Certificate[] copyOf(Certificate[] a) {
        Certificate[] c = new Certificate[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    // Always `false`. See the header.
    @Override
    public boolean implies(Permission p) {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UnresolvedPermission)) {
            return false;
        }
        UnresolvedPermission that = (UnresolvedPermission) obj;
        if (!this.type.equals(that.type)) {
            return false;
        }
        if (!same(this.name, that.name) || !same(this.actions, that.actions)) {
            return false;
        }
        // The certificates are compared as a set in both directions: the order in which the policy
        // listed them does not change the condition they express.
        return containsAll(this.certs, that.certs) && containsAll(that.certs, this.certs);
    }

    private static boolean same(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private static boolean containsAll(Certificate[] set, Certificate[] wanted) {
        if (wanted == null || wanted.length == 0) {
            return true;
        }
        if (set == null) {
            return false;
        }
        int i = 0;
        while (i < wanted.length) {
            boolean hallado = false;
            int j = 0;
            while (j < set.length) {
                if (wanted[i].equals(set[j])) {
                    hallado = true;
                    j = set.length;
                } else {
                    j = j + 1;
                }
            }
            if (!hallado) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = this.type.hashCode();
        if (this.name != null) {
            hash = hash ^ this.name.hashCode();
        }
        if (this.actions != null) {
            hash = hash ^ this.actions.hashCode();
        }
        return hash;
    }

    // "" — the actions of the unresolved permission are in `getUnresolvedActions()`, not here.
    //
    // The distinction is not a formality: `getActions()` is what **this** permission returns, and
    // this permission has no actions because it permits nothing.
    @Override
    public String getActions() {
        return "";
    }

    public String getUnresolvedType() {
        return this.type;
    }

    public String getUnresolvedName() {
        return this.name;
    }

    public String getUnresolvedActions() {
        return this.actions;
    }

    public Certificate[] getUnresolvedCerts() {
        return this.certs == null ? null : copyOf(this.certs);
    }

    @Override
    public String toString() {
        return "(unresolved " + this.type + " " + this.name + " " + this.actions + ")";
    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return new UnresolvedCollection();
    }
}

// The collection of unresolved permissions.
//
// Its `implies` returns `false` without looking at anything, for the same reason as that of each
// element: none of them grants anything yet. It keeps them all the same, because the point of the
// collection is to have somewhere to go and look when the classes appear.
final class UnresolvedCollection extends PermissionCollection {

    private final ArrayList<Permission> perms = new ArrayList<Permission>();

    @Override
    public void add(Permission permission) {
        if (!(permission instanceof UnresolvedPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        this.perms.add(permission);
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public Enumeration<Permission> elements() {
        return new EnumSinResolver(this.perms);
    }
}

final class EnumSinResolver implements Enumeration<Permission> {

    private final ArrayList<Permission> list;
    private int cursor;

    EnumSinResolver(ArrayList<Permission> list) {
        this.list = list;
    }

    public boolean hasMoreElements() {
        return this.cursor < this.list.size();
    }

    public Permission nextElement() {
        if (this.cursor >= this.list.size()) {
            throw new NoSuchElementException();
        }
        Permission p = this.list.get(this.cursor);
        this.cursor = this.cursor + 1;
        return p;
    }
}
