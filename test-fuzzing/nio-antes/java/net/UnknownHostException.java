package java.net;

import java.io.IOException;

// KajiLibrary's java.net.UnknownHostException -- a host could not be resolved. KajiJDK has no name
// resolver, so InetAddress.getByName and friends raise this for anything that is not a literal.
public class UnknownHostException extends IOException {

    public UnknownHostException(String host) {
        super(host);
    }

    public UnknownHostException() {
    }
}
