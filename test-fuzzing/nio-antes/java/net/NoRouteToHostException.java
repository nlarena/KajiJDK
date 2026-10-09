package java.net;

// A NoRouteToHostException: there is no route to the host (typically a firewall dropping silently, or
// a network that is down).
//
// A subclass of `SocketException` for the same reason its parent exists here (see `SocketException`):
// it names a failure mode, it does not promise to be able to produce it. Whoever catches
// `SocketException` catches it too, which is what the hierarchy is about.
public class NoRouteToHostException extends SocketException {

    private static final long serialVersionUID = -1897550894873493790L;

    public NoRouteToHostException(String msg) {
        super(msg);
    }

    public NoRouteToHostException() {
    }
}
