package java.rmi.registry;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * An RMI registry: the address book every client starts from.
 *
 * <p>It is RMI's cold start. Calling a remote object takes a reference, and getting the first one
 * takes a place known in advance: the registry, which lives on a fixed port
 * ({@link #REGISTRY_PORT}) and is looked up by name rather than by reference. From there on the
 * references travel as arguments and return values, and the registry is not needed again.
 *
 * <p>{@link java.rmi.Naming} is the same thing with URL-shaped names; this interface is the raw
 * one, with the registry already located.
 *
 * <h2>Who may modify it</h2>
 *
 * <p>{@link #bind}, {@link #rebind} and {@link #unbind} are only accepted from the **same
 * machine** the registry runs on; from outside they throw {@link AccessException}. {@link #lookup}
 * and {@link #list} have no such restriction.
 *
 * <p>It is not authentication: it is all there is. Any process on the machine can replace any
 * binding, and anyone who reaches the port can list everything bound.
 */
public interface Registry extends Remote {

    /** The usual port: 1099. */
    int REGISTRY_PORT = 1099;

    /**
     * The reference bound under that name.
     *
     * @throws NotBoundException if nothing is bound under that name
     * @throws AccessException if the registry refuses the call
     * @throws RemoteException if the communication fails
     */
    Remote lookup(String name) throws RemoteException, NotBoundException, AccessException;

    /**
     * It binds the reference under that name, if the name is free.
     *
     * @throws AlreadyBoundException if the name is already bound
     * @throws AccessException if the call does not come from the registry's machine
     * @throws RemoteException if the communication fails
     */
    void bind(String name, Remote obj) throws RemoteException, AlreadyBoundException,
            AccessException;

    /**
     * It removes the binding of that name.
     *
     * @throws NotBoundException if nothing was bound
     * @throws AccessException if the call does not come from the registry's machine
     * @throws RemoteException if the communication fails
     */
    void unbind(String name) throws RemoteException, NotBoundException, AccessException;

    /**
     * It binds the reference under that name, overwriting whatever was there.
     *
     * @throws AccessException if the call does not come from the registry's machine
     * @throws RemoteException if the communication fails
     */
    void rebind(String name, Remote obj) throws RemoteException, AccessException;

    /**
     * The bound names, as of the moment of the call.
     *
     * @throws AccessException if the registry refuses the call
     * @throws RemoteException if the communication fails
     */
    String[] list() throws RemoteException, AccessException;
}
