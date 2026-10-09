package java.rmi;

/**
 * KajiLibrary's java.rmi.ServerRuntimeException -- the server threw an unchecked exception.
 *
 * <p>Deprecated since 1.2. Before that, a {@link RuntimeException} from the remote method was
 * wrapped here; now it propagates to the client <b>as is</b>, unwrapped.
 *
 * <p>The change was for the better and it is worth understanding why: wrapping it forced the client
 * to unwrap in order to catch what it cared about, and it lost the ability to write a {@code catch}
 * for the concrete type. Since an unchecked exception means the same thing on both sides --someone
 * programmed badly-- there is no reason to translate it, unlike an {@link Error}, which does have
 * {@link ServerError}.
 *
 * <p>It is kept so that old code compiles.
 */
@Deprecated
public class ServerRuntimeException extends RemoteException {

    private static final long serialVersionUID = 7054464920481467219L;

    /**
     * @param s the message
     * @param ex the original
     */
    @Deprecated
    public ServerRuntimeException(String s, Exception ex) {
        super(s, ex);
    }
}
