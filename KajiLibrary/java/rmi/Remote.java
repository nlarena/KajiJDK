package java.rmi;

/**
 * KajiLibrary's java.rmi.Remote -- this interface can be called from another virtual machine.
 *
 * <p>It declares nothing. It is a <b>marker</b>: the only thing it does is tell RMI that the
 * interfaces that extend it describe remote objects.
 *
 * <h2>Why marking is needed</h2>
 *
 * <p>Because a remote call does not behave like a local one, and the code has to be able to tell:
 *
 * <ul>
 *   <li>the arguments and the result are copied by serialisation, not passed by reference --unless
 *       they are remote objects themselves--;
 *   <li>any call can fail because of the network, and that is why <b>all</b> the methods of a
 *       remote interface have to declare {@link RemoteException};
 *   <li>this note used to say that {@code equals}, {@code hashCode} and {@code toString} on a
 *       remote reference talk about the local stub, not the object over there. It is the other way
 *       round: {@code java.rmi.server.RemoteObject} delegates them to its {@code RemoteRef} ({@code
 *       remoteEquals}, {@code remoteHashCode}, {@code remoteToString}), so two stubs for the same
 *       remote object are equal (checked in {@code RemoteObject.java}).
 * </ul>
 *
 * <p>This note used to say that an interface with a method that does not declare
 * {@code RemoteException} cannot be exported, an error found at export time and not at compile
 * time, and the most common stumble in RMI. Nothing in this library checks that: every
 * {@code UnicastRemoteObject.exportObject} overload throws {@code UnsupportedOperationException}
 * whatever the interface declares, because this VM has no RMI transport (checked by reading
 * {@code UnicastRemoteObject.java} and grepping {@code java/rmi} for such a check).
 */
public interface Remote {
}
