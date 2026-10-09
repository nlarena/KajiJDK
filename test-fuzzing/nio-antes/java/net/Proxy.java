package java.net;

// A proxy: where to go out through instead of straight to the destination.
//
// The class is an immutable (type, address) pair, and the constructor's validation is the only
// interesting thing about it: `DIRECT` admits no address --going straight has no intermediary-- and
// the other two types require one. Both cases are rejected with the same message because they are the
// same error: the type and the address do not match.
//
// That `NO_PROXY` exists as a constant instead of accepting null is on purpose: a `select()` that can
// return "no proxy" as just another element of the list is simpler than one that returns empty lists
// or nulls.
//
// Describing a proxy is not using it: this is a value, not a connection. Nothing omitted.
public class Proxy {

    /** The three kinds of proxy the platform can name. */
    public enum Type {

        /** No proxy: a direct connection. */
        DIRECT,

        /** A high-level proxy, typically HTTP or FTP. */
        HTTP,

        /** A SOCKS proxy (v4 or v5). */
        SOCKS;
    }

    /** The proxy that is not a proxy: it represents "direct connection". */
    public static final Proxy NO_PROXY = new Proxy();

    private final Type type;
    private final SocketAddress sa;

    private Proxy() {
        this.type = Type.DIRECT;
        this.sa = null;
    }

    /**
     * A proxy of that type at that address.
     *
     * @throws IllegalArgumentException if the type is {@code DIRECT}, or if the address is not an
     *     {@link InetSocketAddress}
     */
    public Proxy(Type type, SocketAddress sa) {
        if (type == Type.DIRECT || !(sa instanceof InetSocketAddress)) {
            throw new IllegalArgumentException(
                    "type " + type + " is not compatible with address " + sa);
        }
        this.type = type;
        this.sa = sa;
    }

    public Type type() {
        return this.type;
    }

    /** The proxy's address, or null if it is {@code DIRECT}. */
    public SocketAddress address() {
        return this.sa;
    }

    public String toString() {
        if (this.type() == Type.DIRECT) {
            return "DIRECT";
        }
        return this.type() + " @ " + this.address();
    }

    public final boolean equals(Object obj) {
        if (!(obj instanceof Proxy)) {
            return false;
        }
        Proxy p = (Proxy) obj;
        if (p.type() != this.type()) {
            return false;
        }
        if (this.address() == null) {
            return p.address() == null;
        }
        return this.address().equals(p.address());
    }

    public final int hashCode() {
        if (this.address() == null) {
            return this.type().hashCode();
        }
        return this.type().hashCode() + this.address().hashCode();
    }
}
