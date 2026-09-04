package java.net;

// Una direccion IP mas un puerto -- o, cuando no se pudo resolver el nombre, un nombre mas un
// puerto.
//
// Ese "o" es toda la clase. Un `InetSocketAddress` puede estar en dos estados y la diferencia es
// visible en la API (`isUnresolved`, y `getAddress()` que devuelve null) porque **es util que lo
// sea**: se puede armar la direccion de un proxy o de un destino sin tener DNS a mano, pasarla por
// ahi, y que la resolucion pase mas tarde o en otra maquina. Por eso `createUnresolved` no es un
// caso degradado sino una factoria de primera.
//
// En KajiJDK eso deja de ser un detalle: sin resolver, `new InetSocketAddress(nombre, puerto)`
// termina en el estado no resuelto para todo lo que no sea un literal IP. No hay nada que
// disimular ahi -- es exactamente el estado que el JDK produce cuando el DNS no contesta, con los
// mismos observables.
//
// La clase entera es computacion pura: guarda, valida y formatea. No hay nada omitido.
public class InetSocketAddress extends SocketAddress {

    private static final long serialVersionUID = 5076001401234631237L;

    // Exactamente uno de estos dos manda: si `addr` no es null, la direccion esta resuelta y
    // `hostname` es null; si es null, esta sin resolver y `hostname` tiene el nombre.
    private final String hostname;
    private final InetAddress addr;
    private final int port;

    private InetSocketAddress(String hostname, InetAddress addr, int port) {
        this.hostname = hostname;
        this.addr = addr;
        this.port = port;
    }

    /**
     * Comodin (0.0.0.0) en ese puerto: "cualquier direccion local".
     *
     * @throws IllegalArgumentException si el puerto esta fuera de 0..65535
     */
    public InetSocketAddress(int port) {
        this(checkPort(port), (InetAddress) null);
    }

    /**
     * Esa direccion en ese puerto. {@code addr} null significa el comodin.
     *
     * @throws IllegalArgumentException si el puerto esta fuera de 0..65535
     */
    public InetSocketAddress(InetAddress addr, int port) {
        this(checkPort(port), addr);
    }

    private InetSocketAddress(int port, InetAddress addr) {
        this.hostname = null;
        this.addr = (addr == null) ? new Inet4Address() : addr;
        this.port = port;
    }

    /**
     * Intenta resolver {@code hostname}; si no se puede, queda sin resolver con ese nombre.
     *
     * <p>Que no tire cuando la resolucion falla es del contrato: el objeto sigue siendo utilizable y
     * el que lo recibe decide que hacer con un destino sin resolver.
     *
     * @throws IllegalArgumentException si el puerto esta fuera de rango o {@code hostname} es null
     */
    public InetSocketAddress(String hostname, int port) {
        checkPort(port);
        checkHost(hostname);
        InetAddress a = null;
        String h = null;
        try {
            a = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            h = hostname;
        }
        this.hostname = h;
        this.addr = a;
        this.port = port;
    }

    /** Una direccion sin resolver, sin siquiera intentar resolverla. */
    public static InetSocketAddress createUnresolved(String host, int port) {
        checkPort(port);
        checkHost(host);
        return new InetSocketAddress(host, null, port);
    }

    private static int checkPort(int port) {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("port out of range:" + port);
        }
        return port;
    }

    private static String checkHost(String hostname) {
        if (hostname == null) {
            throw new IllegalArgumentException("hostname can't be null");
        }
        return hostname;
    }

    public final int getPort() {
        return this.port;
    }

    /** La direccion, o null si esta sin resolver. */
    public final InetAddress getAddress() {
        return this.addr;
    }

    /** El nombre del host: el que se pidio si esta sin resolver, el de la direccion si no. */
    public final String getHostName() {
        if (this.hostname != null) {
            return this.hostname;
        }
        if (this.addr != null) {
            return this.addr.getHostName();
        }
        return null;
    }

    /**
     * Como {@link #getHostName()}, pero **sin** disparar una resolucion inversa.
     *
     * <p>La diferencia solo se nota en el JDK real, donde `getHostName()` puede salir a la red; aca
     * las dos hacen lo mismo. Aun asi el metodo corresponde: quien escribe codigo portable necesita
     * poder decir "el nombre que ya tenes, no vayas a buscar otro".
     */
    public final String getHostString() {
        if (this.hostname != null) {
            return this.hostname;
        }
        if (this.addr.hostName != null) {
            return this.addr.hostName;
        }
        return this.addr.getHostAddress();
    }

    public final boolean isUnresolved() {
        return this.addr == null;
    }

    public String toString() {
        if (this.isUnresolved()) {
            return this.hostname + "/<unresolved>:" + this.port;
        }
        String s = this.addr.toString();
        // Los corchetes de la parte numerica de una IPv6, sin los cuales "::1:80" seria ambiguo.
        if (this.addr instanceof Inet6Address) {
            int i = s.lastIndexOf('/');
            s = s.substring(0, i + 1) + "[" + s.substring(i + 1) + "]";
        }
        return s + ":" + this.port;
    }

    // Dos sin resolver son iguales si coinciden nombre (sin distinguir mayusculas) y puerto; dos
    // resueltas, si coinciden direccion y puerto. Una resuelta nunca es igual a una sin resolver,
    // aunque el nombre apunte a esa direccion: no se sabe, justamente porque no se resolvio.
    public final boolean equals(Object obj) {
        if (!(obj instanceof InetSocketAddress)) {
            return false;
        }
        InetSocketAddress that = (InetSocketAddress) obj;
        boolean sameIP;
        if (this.addr != null) {
            sameIP = this.addr.equals(that.addr);
        } else if (this.hostname != null) {
            sameIP = (that.addr == null) && this.hostname.equalsIgnoreCase(that.hostname);
        } else {
            sameIP = (that.addr == null) && (that.hostname == null);
        }
        return sameIP && (this.port == that.port);
    }

    public final int hashCode() {
        if (this.addr != null) {
            return this.addr.hashCode() + this.port;
        }
        if (this.hostname != null) {
            return this.hostname.hashCode() + this.port;
        }
        return this.port;
    }
}
