package java.net;

import java.nio.file.Path;

// A Unix-domain socket's address: a file-system path.
//
// It is `InetSocketAddress`'s counterpart for processes talking **inside the same machine**. There is
// no IP address and no port because there is no network in between: the name is a file, and the
// file's permissions are the connection's permissions. That last point is the practical reason they
// exist: granting access to a local service is a `chmod`, not a firewall rule.
//
// The empty path is legal and means an **unnamed** address, the one a socket has when it has not yet
// been bound to any path.
//
// The class is an immutable wrapper over a `Path`. Nothing omitted: describing the address is not
// opening the socket, and only the first is here.
public final class UnixDomainSocketAddress extends SocketAddress {

    private static final long serialVersionUID = 92902496589351698L;

    private final transient Path path;

    private UnixDomainSocketAddress(Path path) {
        this.path = path;
    }

    /** The address for that path, written as text. */
    public static UnixDomainSocketAddress of(String pathname) {
        return of(Path.of(pathname));
    }

    /** The address for that path. */
    public static UnixDomainSocketAddress of(Path path) {
        return new UnixDomainSocketAddress(path);
    }

    public Path getPath() {
        return this.path;
    }

    public int hashCode() {
        return this.path.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof UnixDomainSocketAddress)) {
            return false;
        }
        return this.path.equals(((UnixDomainSocketAddress) o).path);
    }

    public String toString() {
        return this.path.toString();
    }
}
