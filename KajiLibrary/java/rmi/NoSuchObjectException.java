package java.rmi;

/**
 * KajiLibrary's java.rmi.NoSuchObjectException -- that object is no longer on the server.
 *
 * <p>The remote object no longer exists in the server's virtual machine: it was exported and then
 * unexported, or the server restarted.
 *
 * <p>It is <b>final</b> in the practical sense: retrying with the same reference will never work.
 * One has to look up the registry again to get a fresh reference.
 */
public class NoSuchObjectException extends RemoteException {

    private static final long serialVersionUID = 6619395951570472985L;

    /** @param s the message */
    public NoSuchObjectException(String s) {
        super(s);
    }
}
