package java.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.stream.Stream;

// KajiLibrary's java.security.PermissionCollection -- a homogeneous collection of Permissions with
// a collective implies(). Abstract: concrete collections define add/implies/elements. The read-only
// latch and the stream view are provided here, as in the JDK.
public abstract class PermissionCollection implements Serializable {

    private volatile boolean readOnly;

    public PermissionCollection() {
    }

    /** Adds a permission to this collection. */
    public abstract void add(Permission permission);

    /** Whether the permissions in this collection imply {@code permission}. */
    public abstract boolean implies(Permission permission);

    /** An enumeration of the permissions in this collection. */
    public abstract Enumeration<Permission> elements();

    /** A sequential stream of the permissions in this collection. */
    public Stream<Permission> elementsAsStream() {
        ArrayList<Permission> list = new ArrayList<Permission>();
        Enumeration<Permission> e = this.elements();
        while (e.hasMoreElements()) {
            list.add(e.nextElement());
        }
        Permission[] arr = new Permission[list.size()];
        int i = 0;
        while (i < list.size()) {
            arr[i] = list.get(i);
            i = i + 1;
        }
        return Stream.of(arr);
    }

    /** Marks this collection read-only: no more permissions may be added. */
    public void setReadOnly() {
        this.readOnly = true;
    }

    /** Whether this collection is read-only. */
    public boolean isReadOnly() {
        return this.readOnly;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString()).append(" (\n");
        Enumeration<Permission> e = this.elements();
        while (e.hasMoreElements()) {
            sb.append(" ").append(e.nextElement()).append("\n");
        }
        sb.append(")\n");
        return sb.toString();
    }
}
