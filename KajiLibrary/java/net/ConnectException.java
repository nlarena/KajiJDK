package java.net;

// A ConnectException: the far end refused the connection or did not answer in time.
//
// A subclass of `SocketException` for the same reason its parent exists here (see `SocketException`):
// it names a failure mode, it does not promise to be able to produce it. Whoever catches
// `SocketException` catches it too, which is what the hierarchy is about.
public class ConnectException extends SocketException {

    private static final long serialVersionUID = 3767514772251481192L;

    public ConnectException(String msg) {
        super(msg);
    }

    public ConnectException() {
    }
}
