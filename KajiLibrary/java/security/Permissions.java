package java.security;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

// A **heterogeneous** collection of permissions: the one at the very top.
//
// The `PermissionCollection`s of each class are homogeneous on purpose, because their `implies`
// knows about the shape of that class. This one joins them: it keeps one collection per class of
// permission and delegates to it. `implies` looks for the collection of the class of the permission
// asked for and asks it — it does not walk the others, because two permissions of different classes
// never imply each other.
//
// `AllPermission` is the exception and that is why it is kept apart: if there is one, `implies`
// returns true without consulting anything else.
public final class Permissions extends PermissionCollection implements Serializable {

    // Class of permission -> its homogeneous collection.
    private final HashMap<Class<?>, PermissionCollection> byClass =
        new HashMap<Class<?>, PermissionCollection>();

    // The collection of AllPermission, if any was added. Package-private as in the JDK.
    PermissionCollection allPermission;

    public Permissions() {
    }

    public void add(Permission permission) {
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly Permissions object");
        }
        PermissionCollection pc = this.collectionFor(permission);
        pc.add(permission);
        if (permission instanceof AllPermission) {
            this.allPermission = pc;
        }
    }

    public boolean implies(Permission permission) {
        // A stored AllPermission cuts the search short.
        if (this.allPermission != null && this.allPermission.implies(permission)) {
            return true;
        }
        PermissionCollection pc = this.byClass.get(permission.getClass());
        if (pc == null) {
            return false;
        }
        return pc.implies(permission);
    }

    public Enumeration<Permission> elements() {
        java.util.ArrayList<Permission> all = new java.util.ArrayList<Permission>();
        Iterator<Class<?>> classes = this.byClass.keySet().iterator();
        while (classes.hasNext()) {
            PermissionCollection pc = this.byClass.get(classes.next());
            Enumeration<Permission> e = pc.elements();
            while (e.hasMoreElements()) {
                all.add(e.nextElement());
            }
        }
        return new PermListEnum(all);
    }

    // The collection of the class of the permission, creating it if need be.
    //
    // It is asked of the class of permission itself (`newPermissionCollection`) because only it
    // knows whether it has a faster implementation. If it says `null` —I have nothing better— a
    // generic one that compares one at a time is used.
    private PermissionCollection collectionFor(Permission p) {
        Class<?> c = p.getClass();
        PermissionCollection pc = this.byClass.get(c);
        if (pc != null) {
            return pc;
        }
        pc = p.newPermissionCollection();
        if (pc == null) {
            pc = new GenericPermissions();
        }
        this.byClass.put(c, pc);
        return pc;
    }
}

// The collection of last resort: it keeps the permissions in a list and asks one at a time.
//
// It is right for any class of permission, and that is why it serves as a fallback; what it is not,
// is fast. A class that is used a lot should return its own in `newPermissionCollection`.
final class GenericPermissions extends PermissionCollection {

    private final java.util.ArrayList<Permission> perms = new java.util.ArrayList<Permission>();

    public void add(Permission permission) {
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        this.perms.add(permission);
    }

    public boolean implies(Permission permission) {
        int i = 0;
        while (i < this.perms.size()) {
            if (this.perms.get(i).implies(permission)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    public Enumeration<Permission> elements() {
        return new PermListEnum(this.perms);
    }
}

// An enumeration over a list of permissions.
final class PermListEnum implements Enumeration<Permission> {

    private final java.util.List<Permission> list;
    private int cursor;

    PermListEnum(java.util.List<Permission> list) {
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
