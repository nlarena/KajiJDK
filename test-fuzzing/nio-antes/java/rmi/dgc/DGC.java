package java.rmi.dgc;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ObjID;

/**
 * The distributed collector, as seen from the client.
 *
 * <p>Every VM that exports remote objects also exports a `DGC` under the fixed identifier
 * {@code ObjID.DGC_ID}. The protocol is two calls and one idea: **reference counting with
 * expiry**.
 *
 * <ul>
 *   <li>{@link #dirty} is what the client calls when it receives a reference to an object of this
 *       server. The server records that that VM holds it and returns a {@link Lease}: the lease
 *       expires, so a client that hangs or dies does not leave the reference alive forever. A
 *       client that wants to keep holding it has to renew before the expiry.
 *   <li>{@link #clean} is what the client calls when it drops the reference. It is the fast path:
 *       without it the server frees the object all the same when the term expires, but late.
 * </ul>
 *
 * <p>When no reference is left --neither local nor remote-- the object is left at the mercy of
 * the usual collector.
 *
 * <p>The reference count does not see cycles across VMs: two objects on two servers that point at
 * each other are never freed. It is a known limitation of the design, not an oversight.
 */
public interface DGC extends Remote {

    /**
     * It asks for, or renews, the lease to retain the given references.
     *
     * @param ids the objects the client wants to retain
     * @param sequenceNum the sequence number of the call, so that the server discards the ones
     *     that reach it out of order --with `dirty` and `clean` crossing each other, the order
     *     matters
     * @param lease the lease it asks for: only the duration is a request, the server decides
     * @return the lease granted, with the duration the server has decided on
     * @throws RemoteException if the call fails
     */
    Lease dirty(ObjID[] ids, long sequenceNum, Lease lease) throws RemoteException;

    /**
     * It reports that the client has dropped the given references.
     *
     * @param ids the objects the client no longer retains
     * @param sequenceNum the sequence number of the call
     * @param vmid the VM that drops them
     * @param strong whether the call has to win over an earlier `dirty` even if that one arrives
     *     with a lower sequence number
     * @throws RemoteException if the call fails
     */
    void clean(ObjID[] ids, long sequenceNum, VMID vmid, boolean strong) throws RemoteException;
}
