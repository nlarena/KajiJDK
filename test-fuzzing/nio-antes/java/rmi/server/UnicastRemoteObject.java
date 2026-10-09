package java.rmi.server;

import java.io.ObjectInputFilter;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The normal way of making an object reachable through RMI.
 *
 * <h2>What "unicast" means</h2>
 *
 * <p>That the reference points at <strong>one</strong> object, in one process, for as long as that
 * process lives. It is the only semantics left: the others RMI once had —activatable objects, which
 * started up on their own when a call came in— were removed.
 *
 * <p>Exporting does two things at once: it opens the port the object is going to be reached
 * through, and it <strong>anchors the object</strong> so that the local collector does not take it
 * away while there are clients. Hence {@link #unexportObject}: without calling it, an exported
 * object never gets collected.
 *
 * <h2>The deserialisation filter</h2>
 *
 * <p>The overloads with {@link ObjectInputFilter} came later and are the ones to prefer. Receiving
 * arguments over the network is deserialising whatever somebody else sends, and without a filter
 * that accepts any object graph — which is the vector of deserialisation attacks. The filter
 * narrows which classes are admitted <em>before</em> building them.
 *
 * <h2>In this VM</h2>
 *
 * <p>Exporting needs the RMI transport, which this VM does not have: the {@code exportObject}s
 * throw {@link UnsupportedOperationException} with the reason, instead of returning a stub that
 * would lead nowhere.
 */
public class UnicastRemoteObject extends RemoteServer {

    private static final long serialVersionUID = 4974527148936298033L;

    /** On an anonymous port. */
    protected UnicastRemoteObject() throws RemoteException {
        this(0);
    }

    /** On that port; {@code 0} lets the system choose it. */
    protected UnicastRemoteObject(int port) throws RemoteException {
        super();
    }

    /** On that port and with those socket factories. */
    protected UnicastRemoteObject(int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf) throws RemoteException {
        super();
    }

    /**
     * Cloning one of these exports the copy.
     *
     * @throws java.rmi.server.ServerCloneException if the copy could not be exported
     */
    public Object clone() throws CloneNotSupportedException {
        throw new ServerCloneException("this VM does not export remote objects");
    }

    /**
     * It exports the object on an anonymous port.
     *
     * @deprecated it returns a {@link RemoteStub}, which is from the days of {@code rmic}; use
     *     {@link #exportObject(Remote, int)}
     * @throws UnsupportedOperationException in this VM
     */
    @Deprecated(since = "1.2")
    public static RemoteStub exportObject(Remote obj) throws RemoteException {
        throw new UnsupportedOperationException("this VM does not have the RMI transport");
    }

    /**
     * It exports the object on that port.
     *
     * @throws UnsupportedOperationException in this VM
     */
    public static Remote exportObject(Remote obj, int port) throws RemoteException {
        throw new UnsupportedOperationException("this VM does not have the RMI transport");
    }

    /**
     * It exports with socket factories of its own; that is how an object demands TLS.
     *
     * @throws UnsupportedOperationException in this VM
     */
    public static Remote exportObject(Remote obj, int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf) throws RemoteException {
        throw new UnsupportedOperationException("this VM does not have the RMI transport");
    }

    /**
     * It exports with a deserialisation filter; see the class note.
     *
     * @throws UnsupportedOperationException in this VM
     */
    public static Remote exportObject(Remote obj, int port, ObjectInputFilter filter)
            throws RemoteException {
        throw new UnsupportedOperationException("this VM does not have the RMI transport");
    }

    /**
     * It exports with factories of its own and a filter.
     *
     * @throws UnsupportedOperationException in this VM
     */
    public static Remote exportObject(Remote obj, int port, RMIClientSocketFactory csf,
            RMIServerSocketFactory ssf, ObjectInputFilter filter) throws RemoteException {
        throw new UnsupportedOperationException("this VM does not have the RMI transport");
    }

    /**
     * It stops exporting the object, whereupon it can be collected again.
     *
     * @param force whether to unexport even when there are calls in progress or clients holding
     *     references
     * @throws NoSuchObjectException if the object was not exported — which is always, in this VM
     */
    public static boolean unexportObject(Remote obj, boolean force) throws NoSuchObjectException {
        throw new NoSuchObjectException("the object is not exported");
    }
}
