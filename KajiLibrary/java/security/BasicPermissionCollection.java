package java.security;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

// The collection `BasicPermission.newPermissionCollection()` returns. Package-private: the contract
// only promises a `PermissionCollection`.
//
// It indexes by canonical name, and out of that comes its whole reason for being: to know whether
// the set implies `"a.b.c"` there is no need to walk it entirely, it is enough to try `"a.b.c"`,
// `"a.b.*"`, `"a.*"` and `"*"` — four table lookups instead of N comparisons. With a handful of
// permissions it makes no difference; with hundreds, it does.
//
// Every permission of a collection has to be of **the same class**: mixing a `PropertyPermission`
// with a `RuntimePermission` would make the index lie, because two permissions of different classes
// with the same name do not imply each other.
final class BasicPermissionCollection extends PermissionCollection {

    // Canonical name -> permission.
    private final HashMap<String, Permission> perms = new HashMap<String, Permission>();

    // The class this collection accepts.
    private final Class<?> permClass;

    // Whether any of the permissions is the universal wildcard `"*"`, which implies everything in
    // one go.
    private boolean all;

    BasicPermissionCollection(Class<?> permClass) {
        this.permClass = permClass;
    }

    public void add(Permission permission) {
        if (!(permission instanceof BasicPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (permission.getClass() != this.permClass) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        BasicPermission bp = (BasicPermission) permission;
        String canonical = bp.getCanonicalName();
        this.perms.put(canonical, permission);
        if (canonical.equals("*")) {
            this.all = true;
        }
    }

    public boolean implies(Permission permission) {
        if (!(permission instanceof BasicPermission)) {
            return false;
        }
        if (permission.getClass() != this.permClass) {
            return false;
        }
        if (this.all) {
            return true;
        }
        BasicPermission bp = (BasicPermission) permission;
        String name = bp.getCanonicalName();

        // An exact match.
        Permission exact = this.perms.get(name);
        if (exact != null) {
            return true;
        }

        // The wildcards of each prefix, from the most specific to the most general: for "a.b.c"
        // "a.b.*" and "a.*" are tried. The `lastIndexOf` cuts one segment per round.
        int cut = name.length() - 1;
        while (cut >= 0) {
            int dot = lastIndexOf(name, '.', cut);
            if (dot < 0) {
                break;
            }
            Permission wildcard = this.perms.get(name.substring(0, dot + 1) + "*");
            if (wildcard != null) {
                return true;
            }
            cut = dot - 1;
        }
        return false;
    }

    // The last `c` in `s` at the position `from` or before, or -1. Written by hand because
    // `String.lastIndexOf(int, int)` is not in this library.
    private static int lastIndexOf(String s, char c, int from) {
        int i = from;
        if (i >= s.length()) {
            i = s.length() - 1;
        }
        while (i >= 0) {
            if (s.charAt(i) == c) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    public Enumeration<Permission> elements() {
        return new PermissionEnum(this.perms.keySet().iterator(), this.perms);
    }
}

// The enumeration over the permissions of a BasicPermissionCollection.
final class PermissionEnum implements Enumeration<Permission> {

    private final Iterator<String> names;
    private final HashMap<String, Permission> perms;

    PermissionEnum(Iterator<String> names, HashMap<String, Permission> perms) {
        this.names = names;
        this.perms = perms;
    }

    public boolean hasMoreElements() {
        return this.names.hasNext();
    }

    public Permission nextElement() {
        if (!this.names.hasNext()) {
            throw new NoSuchElementException();
        }
        return this.perms.get(this.names.next());
    }
}
