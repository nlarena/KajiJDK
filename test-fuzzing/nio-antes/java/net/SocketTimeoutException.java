package java.net;

import java.io.InterruptedIOException;

// A socket operation's deadline expired.
//
// It inherits from `InterruptedIOException`, not from `SocketException`, and there lies the whole
// point of the class: `InterruptedIOException` carries `bytesTransferred`, that is, **how much was
// moved before the cut**. A timeout does not invalidate the socket -- what happened, happened -- and
// whoever catches it usually wants to go on from where it stopped. Hanging off `SocketException` it
// would say "the socket died", which is a different thing.
public class SocketTimeoutException extends InterruptedIOException {

    private static final long serialVersionUID = -8846654841826352300L;

    public SocketTimeoutException(String msg) {
        super(msg);
    }

    public SocketTimeoutException() {
    }
}
