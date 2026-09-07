package java.net;

// A PortUnreachableException: an ICMP port-unreachable arrived on a connected UDP socket: there was
// nobody listening on the other side.
//
// A subclass of `SocketException` for the same reason its parent exists here (see `SocketException`):
// it names a failure mode, it does not promise to be able to produce it. Whoever catches
// `SocketException` catches it too, which is what the hierarchy is about.
public class PortUnreachableException extends SocketException {

    private static final long serialVersionUID = 8462541992376507323L;

    public PortUnreachableException(String msg) {
        super(msg);
    }

    public PortUnreachableException() {
    }
}
