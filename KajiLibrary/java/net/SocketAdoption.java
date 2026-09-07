package java.net;

/**
 * What `java.net` knows how to do and `java.nio.channels` cannot: wrap a socket the VM already has
 * open in the object of this package that corresponds to it.
 *
 * <p>It exists because {@code SocketChannel.socket()} and its two siblings have to return a
 * `java.net` object **over the same system socket** the channel already has. Wrapping a handle is a
 * package-private operation --it is in the public contract of none of the three classes, and adding
 * it would be inventing a member the JDK does not have-- so it is done here and offered through the
 * {@link jdk.internal.net.Adoption} bridge. That class's note explains why the bridge exists.
 *
 * <p>The three objects that come out of here **share the socket with the channel**: closing either
 * one closes the same descriptor, which is exactly what the JDK promises for the channel/socket
 * pair.
 */
final class SocketAdoption implements jdk.internal.net.Adoption.Factory {

    public Object tcp(int handle) {
        Socket s = new Socket();
        s.adopt(handle);
        return s;
    }

    public Object server(int handle) {
        try {
            ServerSocket s = new ServerSocket();
            s.adopt(handle);
            return s;
        } catch (java.io.IOException e) {
            // `new ServerSocket()` binds nothing and so cannot fail; the `throws` comes from the
            // contract. If it ever did fail, a `null` would be worse than saying so.
            throw new IllegalStateException("could not wrap the socket", e);
        }
    }

    public Object datagram(int handle) {
        try {
            DatagramSocket s = new DatagramSocket((SocketAddress) null);
            s.adopt(handle);
            return s;
        } catch (SocketException e) {
            // As above: `new DatagramSocket(null)` is the case that binds nothing.
            throw new IllegalStateException("could not wrap the socket", e);
        }
    }
}
