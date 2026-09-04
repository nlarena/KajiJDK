package java.net;

// Un proxy: por donde salir en vez de ir derecho al destino.
//
// La clase es un par (tipo, direccion) inmutable, y la validacion del constructor es lo unico que
// tiene de interesante: `DIRECT` no admite direccion --ir derecho no tiene intermediario-- y los
// otros dos tipos la exigen. Los dos casos se rechazan con el mismo mensaje porque son el mismo
// error: el tipo y la direccion no se corresponden.
//
// Que exista `NO_PROXY` como constante en vez de aceptar null es a proposito: un `select()` que
// pueda devolver "sin proxy" como un elemento mas de la lista es mas simple que uno que devuelva
// listas vacias o nulls.
//
// Describir un proxy no es usarlo: esto es un valor, no una conexion. Nada omitido.
public class Proxy {

    /** Los tres tipos de proxy que la plataforma sabe nombrar. */
    public enum Type {

        /** Sin proxy: conexion directa. */
        DIRECT,

        /** Proxy de alto nivel, tipicamente HTTP o FTP. */
        HTTP,

        /** Proxy SOCKS (v4 o v5). */
        SOCKS;
    }

    /** El proxy que no es un proxy: representa "conexion directa". */
    public static final Proxy NO_PROXY = new Proxy();

    private final Type type;
    private final SocketAddress sa;

    private Proxy() {
        this.type = Type.DIRECT;
        this.sa = null;
    }

    /**
     * Un proxy de ese tipo en esa direccion.
     *
     * @throws IllegalArgumentException si el tipo es {@code DIRECT}, o si la direccion no es una
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

    /** La direccion del proxy, o null si es {@code DIRECT}. */
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
