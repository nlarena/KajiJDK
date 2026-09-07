package java.net;

/**
 * KajiLibrary's java.net.SocketAddress — a socket address, without saying of which protocol.
 *
 * <p>**It has no method at all, and that is what it is.** It exists so that the signatures speaking
 * of "an address" do not have to name a concrete protocol: `bind(SocketAddress)` serves an IP address
 * with a port just as well as a Unix-domain socket path. The address's shape is the subclass's
 * business.
 *
 * <p>It is abstract and memberless on purpose: a type that only contributes a common name. That
 * nothing can be done with one is correct — the only thing needed is to be able to pass it.
 */
public abstract class SocketAddress implements java.io.Serializable {

    private static final long serialVersionUID = 5215720748342549866L;

    public SocketAddress() {
    }
}
