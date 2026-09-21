package java.rmi.server;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.rmi.Remote;

/**
 * The handle of a remote object: where it lives and how to call it.
 *
 * <p>It is what a stub carries inside. This note used to say that its being
 * {@link Externalizable} is what lets a stub travel: that serialising it writes first the name of
 * the reference's class ({@link #getRefClass}) and then its data, so the other side knows which
 * implementation to rebuild. Nothing in the library does that: {@link #getRefClass} has no caller,
 * no class implements this interface (only the interface {@code ServerRef} extends it), and
 * {@link RemoteObject} keeps its {@code ref} {@code transient} with no {@code writeObject}, so a
 * serialised stub carries no reference at all (checked with grep over {@code java/}).
 *
 * <p>{@link #remoteHashCode} and {@link #remoteEquals} exist because the identity of a remote
 * object is <strong>that of its reference</strong>, not that of the stub: two distinct stubs that
 * point at the same object have to be equal, and that does not come from {@code Object}.
 */
public interface RemoteRef extends Externalizable {

    /** @deprecated the {@code newCall}/{@code invoke(RemoteCall)} part became obsolete. */
    @Deprecated(since = "1.2")
    static final String packagePrefix = "sun.rmi.server";

    static final long serialVersionUID = 3632638527362204081L;

    /** It invokes the method on the remote object and returns the result. */
    Object invoke(Remote obj, Method method, Object[] params, long opnum) throws Exception;

    /**
     * @deprecated from the days of generated stubs; use {@link #invoke(Remote, Method, Object[],
     *     long)}
     */
    @Deprecated(since = "1.2")
    RemoteCall newCall(RemoteObject obj, Operation[] op, int opnum, long hash) throws RemoteException;

    /** @deprecated ditto. */
    @Deprecated(since = "1.2")
    void invoke(RemoteCall call) throws Exception;

    /** @deprecated ditto. */
    @Deprecated(since = "1.2")
    void done(RemoteCall call) throws RemoteException;

    /** The name of this reference's class, so it can be rebuilt on the other side. */
    String getRefClass(ObjectOutput out);

    /** The hash of the remote object, not that of the stub. */
    int remoteHashCode();

    /** Whether they point at the same remote object. */
    boolean remoteEquals(RemoteRef obj);

    /** A description of the reference. */
    String remoteToString();
}
