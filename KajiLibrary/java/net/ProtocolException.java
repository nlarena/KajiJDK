package java.net;

import java.io.IOException;

// The far end spoke the protocol wrongly: the connection is alive, what arrived makes no sense.
//
// It does not inherit from `SocketException` on purpose, and it is not an oversight of the JDK's:
// `SocketException` is "the socket failed" and this is "the socket worked and the message was
// broken". They are two different layers, and whoever catches one hardly ever wants the other.
public class ProtocolException extends IOException {

    private static final long serialVersionUID = 8207694371842273524L;

    public ProtocolException(String host) {
        super(host);
    }

    public ProtocolException() {
    }
}
