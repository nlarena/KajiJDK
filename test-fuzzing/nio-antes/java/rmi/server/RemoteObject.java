package java.rmi.server;

import java.io.Serializable;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;

/**
 * The base of every remote object and of every stub.
 *
 * <h2>Why {@code equals} and {@code hashCode} live here</h2>
 *
 * <p>Because for a remote object the identity <strong>is not that of the Java object</strong>. Two
 * distinct stubs that point at the same object on the other side have to be equal, and with the
 * {@link Object} implementation they never would be. This class delegates to the
 * {@link RemoteRef}, which is the one that knows what it points at.
 *
 * <p>Without that, keeping stubs in a {@code HashSet} would keep duplicates of the same thing.
 *
 * <p>The {@link #ref} field is {@code transient}. This note used to say that was because it is not
 * serialised like an ordinary field but written by hand, with the name of its class in front, so
 * the other side knows which implementation to rebuild; nothing here does that: this class declares
 * no {@code writeObject}/{@code readObject}, no subclass does either, and
 * {@link RemoteRef#getRefClass} has no caller in the library (checked with grep over
 * {@code java/}). A serialised {@code RemoteObject} simply carries no reference at all.
 */
public abstract class RemoteObject implements Remote, Serializable {

    private static final long serialVersionUID = -3215090123894869218L;

    /** Which remote object this points at. */
    protected transient RemoteRef ref;

    /** With no reference yet. */
    protected RemoteObject() {
        this.ref = null;
    }

    /** With that reference. */
    protected RemoteObject(RemoteRef newref) {
        this.ref = newref;
    }

    /** The reference. */
    public RemoteRef getRef() {
        return this.ref;
    }

    /**
     * The stub of the remote object it is given.
     *
     * @throws NoSuchObjectException if the object is not exported
     */
    public static Remote toStub(Remote obj) throws NoSuchObjectException {
        if (obj instanceof RemoteStub) {
            return obj;
        }
        throw new NoSuchObjectException("the object is not exported");
    }

    /** The remote object's, not the stub's. */
    public int hashCode() {
        return this.ref == null ? super.hashCode() : this.ref.remoteHashCode();
    }

    /**
     * Whether the two point at the same remote object.
     *
     * <p>A {@code RemoteObject} with no reference falls back to the identity equality of
     * {@link Object}, which is the only sensible thing: with no reference there is nothing to
     * compare.
     */
    public boolean equals(Object obj) {
        if (obj instanceof RemoteObject) {
            if (this.ref == null) {
                return obj == this;
            }
            return this.ref.remoteEquals(((RemoteObject) obj).ref);
        }
        return obj != null && obj.equals(this);
    }

    public String toString() {
        String name = this.getClass().getName();
        return this.ref == null ? name : name + "[" + this.ref.remoteToString() + "]";
    }
}
