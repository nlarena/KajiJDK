package java.net;

// A BindException: the requested local address could not be assigned (the port is already taken, or
// the address does not belong to this machine).
//
// A subclass of `SocketException` for the same reason its parent exists here (see `SocketException`):
// it names a failure mode, it does not promise to be able to produce it. Whoever catches
// `SocketException` catches it too, which is what the hierarchy is about.
public class BindException extends SocketException {

    private static final long serialVersionUID = -5945005768251722951L;

    public BindException(String msg) {
        super(msg);
    }

    public BindException() {
    }
}
