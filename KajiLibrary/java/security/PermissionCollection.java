package java.security;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.stream.Stream;

// A homogeneous set of permissions, with an `implies` of its own.
//
// It is not a `Collection` of `java.util`, and that is not an oversight: what it contributes is the
// `implies` over **the whole set**, which just any collection does not know how to do. An
// implementation can answer it much faster than by asking each permission one at a time — by
// indexing by name, for example — and that is the whole reason the type exists.
//
// The read-only state is one-way: once closed, it does not reopen. It is what allows a collection
// of permissions to be handed over without fear of the receiver adding more to themselves.
public abstract class PermissionCollection implements Serializable {

    private volatile boolean readOnly;

    public PermissionCollection() {
    }

    // It adds a permission.
    public abstract void add(Permission permission);

    // Whether the permissions of this collection, taken together, imply the given one.
    public abstract boolean implies(Permission permission);

    // The permissions of this collection.
    public abstract Enumeration<Permission> elements();

    // The permissions, as a stream.
    public Stream<Permission> elementsAsStream() {
        Enumeration<Permission> e = this.elements();
        java.util.ArrayList<Permission> list = new java.util.ArrayList<Permission>();
        while (e.hasMoreElements()) {
            list.add(e.nextElement());
        }
        Object[] a = new Object[list.size()];
        int i = 0;
        while (i < list.size()) {
            a[i] = list.get(i);
            i = i + 1;
        }
        return (Stream<Permission>) Stream.of(a);
    }

    // It closes the collection. There is no way back.
    public void setReadOnly() {
        this.readOnly = true;
    }

    // Whether the collection is closed.
    public boolean isReadOnly() {
        return this.readOnly;
    }

    // The name of the class followed by the permissions, one per line.
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(super.toString());
        b.append(" (\n");
        Enumeration<Permission> e = this.elements();
        while (e.hasMoreElements()) {
            b.append(" ");
            b.append(e.nextElement().toString());
            b.append("\n");
        }
        b.append(")\n");
        return b.toString();
    }
}
