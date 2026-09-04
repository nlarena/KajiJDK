package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Un socket de datagramas: el objeto que se configura para mandar y recibir UDP.
//
// ===========================================================================================
// QUE ENTRA Y QUE NO, Y DONDE ESTA EXACTAMENTE LA LINEA
// ===========================================================================================
//
// La linea es la misma de todo `java.net` en KajiJDK: **entra lo que se configura, no entra lo que
// transporta**. Un `DatagramSocket` es dos cosas pegadas --un puñado de opciones y un par de
// metodos que mueven bytes-- y solo la primera se puede cumplir aca.
//
// **YA ENTRA TODO.** Esta cabecera decia que `send`, `receive` y las membresias multicast no
// entraban porque no habia pila de UDP en esta VM. Ahora la hay --`jdk.internal.net.Net`, la misma
// costura que abrio TCP-- asi que los cuatro estan, los constructores que atan atan de verdad, y
// `MulticastSocket` entra a los grupos de verdad.
//
// **Como se espera un datagrama.** El nativo **no bloquea**: contesta -3 cuando todavia no llego
// nada. Tiene que ser asi, y no es una comodidad: los hilos de Java de esta VM comparten un
// interprete, asi que un nativo parado esperando no deja correr al hilo que iba a mandar el
// paquete. `receive` espera de este lado, reintentando con un `Thread.sleep` corto --dormir suelta
// el interprete-- y por eso puede respetar `setSoTimeout` de verdad.
//
// **Sobre `connect`:** en UDP no hay handshake. `connect` es una decision **local** --fija con
// quien se habla para que el resto de los datagramas se filtren-- y no manda un solo byte.
//
// **Sobre `connect`:** en UDP no hay handshake. `connect` es una decision **local** -- fija con
// quien se habla para que el resto de los datagramas se filtren-- y no manda un solo byte. Por eso
// se implementa de verdad y no se omite: registra estado, y todo lo que se puede observar despues
// (`isConnected`, `getInetAddress`, `getPort`, `getRemoteSocketAddress`) es cierto.
//
// **Sobre los valores por defecto de las opciones:** en el JDK los fija el sistema operativo y
// cambian de maquina en maquina. Aca los fija esta clase, y estan documentados uno por uno. Ningun
// programa correcto depende de ellos --por eso el JDK nunca los promete-- y lo que si se garantiza
// es lo unico que importa de un objeto de configuracion: lo que se fija es lo que se lee.
public class DatagramSocket implements Closeable {

    private static volatile DatagramSocketImplFactory factory;

    private final DatagramSocketImpl impl;

    private boolean bound;
    private boolean closed;
    private InetAddress remoteAddr;
    private int remotePort = -1;

    /** El socket de la VM, o -1 si este no se ato. */
    int handle = -1;

    // Lo que necesita un `DatagramChannel` para entregar el socket que lo envuelve. Ver
    // `ServerSocket.adoptar`.
    void adoptar(int h) {
        this.handle = h;
        this.bound = true;
    }

    // Valores por defecto: ver la nota de la cabecera. `soTimeout` en 0 significa "esperar para
    // siempre" y es el unico que el JDK si fija en Java. Los otros son los que usa la mayoria de
    // los sistemas.
    private int soTimeout = 0;
    private int sendBufferSize = 65507;
    private int receiveBufferSize = 65507;
    private boolean reuseAddress = false;
    private boolean broadcast = true;
    private int trafficClass = 0;

    /**
     * Un socket atado a un puerto cualquiera de todas las placas.
     *
     * @throws SocketException siempre en KajiJDK -- no hay pila de UDP que abrir ni puerto que
     *     atar. Para un socket **sin atar**, que si se puede tener y se puede configurar entero,
     *     usar {@code new DatagramSocket(null)}.
     */
    public DatagramSocket() throws SocketException {
        this.impl = null;
        this.atar("0.0.0.0", 0);
    }

    /**
     * Un socket atado a {@code port} en todas las placas.
     *
     * @throws SocketException siempre en KajiJDK; ver {@link #DatagramSocket()}
     */
    public DatagramSocket(int port) throws SocketException {
        this(port, null);
    }

    /**
     * Un socket atado a {@code laddr}:{@code port}.
     *
     * @throws IllegalArgumentException si el puerto esta fuera de rango
     * @throws SocketException siempre en KajiJDK; ver {@link #DatagramSocket()}
     */
    public DatagramSocket(int port, InetAddress laddr) throws SocketException {
        this.impl = null;
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("Port out of range:" + port);
        }
        this.atar(laddr == null ? "0.0.0.0" : laddr.getHostAddress(), port);
    }

    /**
     * Un socket atado a {@code bindaddr}, o **sin atar** si {@code bindaddr} es null.
     *
     * <p>El caso de null es el que anda entero en KajiJDK, y no es una excepcion inventada: el JDK
     * lo documenta asi mismo ("si la direccion es null, crea un socket sin atar").
     *
     * @throws SocketException si {@code bindaddr} no es null -- en KajiJDK no hay como atar
     */
    public DatagramSocket(SocketAddress bindaddr) throws SocketException {
        this.impl = crearImpl();
        if (bindaddr != null) {
            this.bind(bindaddr);
        }
    }

    /**
     * Un socket sin atar, sobre la implementacion dada.
     *
     * <p>Es el constructor que usa una subclase que trae su propia pila. No ata nada, asi que anda
     * completo.
     *
     * @throws NullPointerException si {@code impl} es null
     */
    protected DatagramSocket(DatagramSocketImpl impl) {
        if (impl == null) {
            throw new NullPointerException();
        }
        this.impl = impl;
    }

    private static DatagramSocketImpl crearImpl() {
        DatagramSocketImplFactory f = factory;
        return f == null ? null : f.createDatagramSocketImpl();
    }

    // Ata el socket de la VM y deja el handle. Es lo unico que hacen los constructores que atan y
    // `bind`, y esta junto para que los cuatro caminos no se separen nunca.
    private void atar(String host, int port) throws SocketException {
        int h = jdk.internal.net.Net.udpBind(host, port);
        if (h < 0) {
            // El nativo no distingue "puerto ocupado" de "sin permiso"; el mensaje nombra lo unico
            // que se sabe con certeza.
            throw new SocketException("Cannot bind: " + host + ":" + port);
        }
        this.handle = h;
        this.bound = true;
    }

    private void chequearAbierto() throws SocketException {
        if (this.closed) {
            throw new SocketException("Socket is closed");
        }
    }

    /**
     * Ata el socket a {@code addr}.
     *
     * @throws SocketException siempre en KajiJDK, salvo que el socket este cerrado o ya atado, que
     *     se chequean antes
     */
    public void bind(SocketAddress addr) throws SocketException {
        this.chequearAbierto();
        if (this.bound) {
            throw new SocketException("already bound");
        }
        if (addr != null && !(addr instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        // Sin direccion, el comodin: atar a un puerto cualquiera de todas las placas, que es lo que
        // el JDK documenta para `bind(null)`.
        String host = "0.0.0.0";
        int puerto = 0;
        if (addr != null) {
            InetSocketAddress dir = (InetSocketAddress) addr;
            if (dir.getAddress() != null && !dir.getAddress().isAnyLocalAddress()) {
                host = dir.getAddress().getHostAddress();
            }
            puerto = dir.getPort();
        }
        this.atar(host, puerto);
    }

    /**
     * Fija con quien habla este socket.
     *
     * <p>Es una decision local: no manda nada. Ver la nota de la cabecera.
     *
     * @throws IllegalArgumentException si la direccion es null o el puerto esta fuera de rango
     */
    public void connect(InetAddress address, int port) {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("connect: " + port);
        }
        if (address == null) {
            throw new IllegalArgumentException("connect: null address");
        }
        if (this.closed) {
            return;
        }
        this.remoteAddr = address;
        this.remotePort = port;
    }

    /**
     * Como {@link #connect(InetAddress, int)}, con la direccion y el puerto juntos.
     *
     * @throws IllegalArgumentException si {@code addr} es null o no es una {@link InetSocketAddress}
     * @throws SocketException si {@code addr} no tiene la direccion resuelta
     */
    public void connect(SocketAddress addr) throws SocketException {
        if (addr == null) {
            throw new IllegalArgumentException("Address can't be null");
        }
        if (!(addr instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress epoint = (InetSocketAddress) addr;
        if (epoint.isUnresolved()) {
            throw new SocketException("Unresolved address");
        }
        this.connect(epoint.getAddress(), epoint.getPort());
    }

    /** Deja de estar fijado a un destino. Si no lo estaba, no hace nada. */
    public void disconnect() {
        this.remoteAddr = null;
        this.remotePort = -1;
    }

    /** Si el socket esta atado a un puerto local. */
    public boolean isBound() {
        return this.bound;
    }

    /** Si el socket tiene fijado un destino. */
    public boolean isConnected() {
        return this.remoteAddr != null;
    }

    /** El destino fijado, o null. */
    public InetAddress getInetAddress() {
        return this.remoteAddr;
    }

    /** El puerto del destino fijado, o -1. */
    public int getPort() {
        return this.remotePort;
    }

    /** El destino fijado como {@link SocketAddress}, o null si no hay. */
    public SocketAddress getRemoteSocketAddress() {
        if (!this.isConnected()) {
            return null;
        }
        return new InetSocketAddress(this.remoteAddr, this.remotePort);
    }

    /** La direccion local a la que esta atado, o null si no esta atado. */
    public SocketAddress getLocalSocketAddress() {
        if (this.closed || !this.bound) {
            return null;
        }
        return new InetSocketAddress(this.getLocalAddress(), this.getLocalPort());
    }

    /**
     * La direccion local.
     *
     * <p>Null si esta cerrado, y la direccion comodin si no esta atado -- que es el caso siempre en
     * KajiJDK, y es lo que el JDK devuelve en la misma situacion.
     */
    public InetAddress getLocalAddress() {
        if (this.closed) {
            return null;
        }
        if (this.handle >= 0) {
            String d = jdk.internal.net.Net.localAddress(this.handle);
            if (d != null) {
                try {
                    // Es un literal numerico: esto no consulta ningun DNS.
                    return InetAddress.getByName(d);
                } catch (UnknownHostException e) {
                    // No puede pasar con un literal numerico; si pasara, cae al comodin de abajo.
                }
            }
        }
        try {
            return InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /** El puerto local: -1 si esta cerrado, 0 si no esta atado. */
    public int getLocalPort() {
        if (this.closed) {
            return -1;
        }
        if (this.handle >= 0) {
            int p = jdk.internal.net.Net.localPort(this.handle);
            if (p >= 0) {
                return p;
            }
        }
        return this.bound && this.impl != null ? this.impl.getLocalPort() : 0;
    }

    // ---- opciones ----

    /**
     * Milisegundos que espera una recepcion; 0 es "para siempre".
     *
     * @throws SocketException si el socket esta cerrado
     * @throws IllegalArgumentException si el timeout es negativo
     */
    public void setSoTimeout(int timeout) throws SocketException {
        this.chequearAbierto();
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout < 0");
        }
        this.soTimeout = timeout;
    }

    public int getSoTimeout() throws SocketException {
        this.chequearAbierto();
        return this.soTimeout;
    }

    /**
     * Tamano sugerido del buffer de salida.
     *
     * <p>"Sugerido" es del JDK, no una escapatoria de aca: el sistema operativo puede darte otro, y
     * por eso el getter nunca prometio devolver lo mismo que pusiste. Aca no hay sistema que lo
     * cambie, asi que devuelve exactamente lo que se fijo.
     *
     * @throws IllegalArgumentException si el tamano no es positivo
     */
    public void setSendBufferSize(int size) throws SocketException {
        this.chequearAbierto();
        if (size <= 0) {
            throw new IllegalArgumentException("negative send size");
        }
        this.sendBufferSize = size;
    }

    public int getSendBufferSize() throws SocketException {
        this.chequearAbierto();
        return this.sendBufferSize;
    }

    /**
     * Tamano sugerido del buffer de entrada. Ver {@link #setSendBufferSize}.
     *
     * @throws IllegalArgumentException si el tamano no es positivo
     */
    public void setReceiveBufferSize(int size) throws SocketException {
        this.chequearAbierto();
        if (size <= 0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        this.receiveBufferSize = size;
    }

    public int getReceiveBufferSize() throws SocketException {
        this.chequearAbierto();
        return this.receiveBufferSize;
    }

    /** Si se puede reusar una direccion que quedo ocupada. */
    public void setReuseAddress(boolean on) throws SocketException {
        this.chequearAbierto();
        this.reuseAddress = on;
    }

    public boolean getReuseAddress() throws SocketException {
        this.chequearAbierto();
        return this.reuseAddress;
    }

    /** Si se pueden mandar datagramas a la direccion de broadcast. */
    public void setBroadcast(boolean on) throws SocketException {
        this.chequearAbierto();
        this.broadcast = on;
    }

    public boolean getBroadcast() throws SocketException {
        this.chequearAbierto();
        return this.broadcast;
    }

    /**
     * El campo "type of service" de la cabecera IP.
     *
     * @throws IllegalArgumentException si no entra en un byte
     */
    public void setTrafficClass(int tc) throws SocketException {
        this.chequearAbierto();
        if (tc < 0 || tc > 255) {
            throw new IllegalArgumentException("tc is not in range 0 -- 255");
        }
        this.trafficClass = tc;
    }

    public int getTrafficClass() throws SocketException {
        this.chequearAbierto();
        return this.trafficClass;
    }

    /**
     * Fija una opcion por su constante tipada.
     *
     * @throws UnsupportedOperationException si esta clase no soporta esa opcion
     * @throws IllegalArgumentException si el valor no sirve para esa opcion
     */
    public <T> DatagramSocket setOption(SocketOption<T> name, T value) throws IOException {
        this.chequearAbierto();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.SO_SNDBUF) {
            this.setSendBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_RCVBUF) {
            this.setReceiveBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_REUSEADDR) {
            this.setReuseAddress(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.SO_BROADCAST) {
            this.setBroadcast(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.IP_TOS) {
            this.setTrafficClass(((Integer) value).intValue());
        } else {
            throw new UnsupportedOperationException("'" + name + "' not supported");
        }
        return this;
    }

    /**
     * El valor de una opcion.
     *
     * @throws UnsupportedOperationException si esta clase no soporta esa opcion
     */
    public <T> T getOption(SocketOption<T> name) throws IOException {
        this.chequearAbierto();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.SO_SNDBUF) {
            return (T) Integer.valueOf(this.getSendBufferSize());
        }
        if (name == StandardSocketOptions.SO_RCVBUF) {
            return (T) Integer.valueOf(this.getReceiveBufferSize());
        }
        if (name == StandardSocketOptions.SO_REUSEADDR) {
            return (T) Boolean.valueOf(this.getReuseAddress());
        }
        if (name == StandardSocketOptions.SO_BROADCAST) {
            return (T) Boolean.valueOf(this.getBroadcast());
        }
        if (name == StandardSocketOptions.IP_TOS) {
            return (T) Integer.valueOf(this.getTrafficClass());
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /** Las opciones que este socket entiende. */
    public Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.SO_SNDBUF);
        s.add(StandardSocketOptions.SO_RCVBUF);
        s.add(StandardSocketOptions.SO_REUSEADDR);
        s.add(StandardSocketOptions.SO_BROADCAST);
        s.add(StandardSocketOptions.IP_TOS);
        return Collections.unmodifiableSet(s);
    }

    // ---- ciclo de vida ----

    /** Cierra el socket. Cerrar dos veces no hace nada, que es lo que exige {@link Closeable}. */
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
    }

    /** Si ya se cerro. */
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * El canal NIO asociado, o null.
     *
     * <p>Null salvo que el socket haya salido de un `DatagramChannel`, que es lo que hace el JDK:
     * un socket creado con `new` no tiene canal.
     */
    public java.nio.channels.DatagramChannel getChannel() {
        return null;
    }

    /**
     * Instala la factoria de implementaciones para toda la VM. Una sola vez.
     *
     * @throws Error si ya se habia instalado una
     * @deprecated el JDK deprecio el mecanismo de {@link DatagramSocketImpl}
     */
    @Deprecated
    public static synchronized void setDatagramSocketImplFactory(DatagramSocketImplFactory fac)
            throws IOException {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        factory = fac;
    }

    // ---- mover datagramas -------------------------------------------------------------------

    /**
     * Manda ese datagrama.
     *
     * <p>Si el socket esta conectado, el paquete puede no traer destino: se usa el fijado. Si trae
     * uno **distinto** del fijado, se rechaza, que es lo que exige el contrato.
     *
     * @throws IOException si el datagrama no se pudo mandar entero
     * @throws IllegalArgumentException si el paquete no tiene destino y el socket no esta conectado
     * @throws SocketException si el socket esta cerrado
     */
    public void send(DatagramPacket p) throws IOException {
        if (p == null) {
            throw new NullPointerException("p");
        }
        this.chequearAbierto();
        InetAddress destino = p.getAddress();
        int puerto = p.getPort();
        if (this.isConnected()) {
            if (destino == null) {
                destino = this.remoteAddr;
                puerto = this.remotePort;
            } else if (!destino.equals(this.remoteAddr) || puerto != this.remotePort) {
                throw new IllegalArgumentException("connected address and packet address differ");
            }
        }
        if (destino == null) {
            throw new IllegalArgumentException("Address not set");
        }
        // Mandar sin atar ata: el sistema elige el puerto de salida. Es lo que hace el JDK, y sin
        // esto un `new DatagramSocket(null)` que solo manda no podria mandar nunca.
        if (this.handle < 0) {
            this.atar("0.0.0.0", 0);
        }
        boolean ok = jdk.internal.net.Net.udpSend(this.handle, destino.getHostAddress(), puerto,
                p.getData(), p.getOffset(), p.getLength());
        if (!ok) {
            throw new IOException("send failed");
        }
    }

    /**
     * Espera un datagrama y lo deja en {@code p}, junto con quien lo mando.
     *
     * <p>Respeta {@link #setSoTimeout}: el nativo no espera --contesta "todavia no" en el acto-- y
     * quien cuenta el tiempo es este metodo, que es el que sabe cuando empezo a esperar.
     *
     * <p>Es `synchronized` porque recibir y preguntar de quien vino son **una sola operacion**
     * partida en tres llamadas al nativo; sin el candado, dos hilos recibiendo sobre el mismo socket
     * podrian llevarse el remitente del otro.
     *
     * @throws SocketTimeoutException si vencio el plazo sin que llegara nada
     * @throws IOException si fallo la recepcion
     */
    public synchronized void receive(DatagramPacket p) throws IOException {
        if (p == null) {
            throw new NullPointerException("p");
        }
        this.chequearAbierto();
        if (this.handle < 0) {
            // Recibir sin atar ata, igual que mandar.
            this.atar("0.0.0.0", 0);
        }
        long comienzo = System.currentTimeMillis();
        byte[] buf = p.getData();
        int n = jdk.internal.net.Net.udpReceive(this.handle, buf, p.getOffset(), p.getLength());
        while (n == -3) {
            if (this.closed) {
                throw new SocketException("Socket is closed");
            }
            if (this.soTimeout > 0
                    && System.currentTimeMillis() - comienzo >= this.soTimeout) {
                throw new SocketTimeoutException("Receive timed out");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("receive interrupted");
            }
            n = jdk.internal.net.Net.udpReceive(this.handle, buf, p.getOffset(), p.getLength());
        }
        if (n < 0) {
            throw new IOException("receive failed");
        }
        p.setLength(n);
        String dir = jdk.internal.net.Net.udpSenderAddress(this.handle);
        if (dir != null) {
            p.setAddress(InetAddress.getByName(dir));
            p.setPort(jdk.internal.net.Net.udpSenderPort(this.handle));
        }
    }

    // ---- multicast --------------------------------------------------------------------------

    /**
     * Entra al grupo multicast {@code mcastaddr} por la placa {@code netIf}.
     *
     * @param netIf null deja que el sistema elija la placa, que es lo que documenta el JDK
     * @throws SocketException si la direccion no es multicast -- que es lo que tira el JDK 25,
     *     aunque su javadoc prometa `IllegalArgumentException`
     * @throws IOException si no se pudo entrar al grupo
     * @throws IllegalArgumentException si la direccion no es una {@link InetSocketAddress}
     */
    public void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        this.membresia(mcastaddr, netIf, true);
    }

    /**
     * Sale del grupo multicast {@code mcastaddr}. Ver {@link #joinGroup(SocketAddress,
     * NetworkInterface)}.
     *
     * @throws IOException si no se pudo salir del grupo
     */
    public void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {
        this.membresia(mcastaddr, netIf, false);
    }

    // Entrar y salir de un grupo resuelven exactamente lo mismo --la direccion, la placa, y si son
    // v4 o v6--, asi que estan juntos: separarlos duplicaria esa resolucion, que es donde estan
    // todos los casos raros.
    private void membresia(SocketAddress mcastaddr, NetworkInterface netIf, boolean entrar)
            throws IOException {
        this.chequearAbierto();
        if (!(mcastaddr instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetAddress grupo = ((InetSocketAddress) mcastaddr).getAddress();
        if (grupo == null || !grupo.isMulticastAddress()) {
            // El javadoc del JDK dice `IllegalArgumentException`, pero el JDK 25 tira
            // `SocketException("Not a multicast address")`. Se sigue lo que **hace**, no lo que
            // dice: es lo que un programa que corra contra los dos va a atrapar.
            throw new SocketException("Not a multicast address");
        }
        if (this.handle < 0) {
            this.atar("0.0.0.0", 0);
        }
        String placa = DatagramSocket.nombrarPlaca(grupo, netIf);
        boolean ok = entrar
                ? jdk.internal.net.Net.udpJoin(this.handle, grupo.getHostAddress(), placa)
                : jdk.internal.net.Net.udpLeave(this.handle, grupo.getHostAddress(), placa);
        if (!ok) {
            throw new IOException((entrar ? "join" : "leave") + " group failed: " + grupo);
        }
    }

    // Como nombrar la placa para el nativo: en IPv4 se la nombra por direccion y en IPv6 por
    // indice, y son dos cadenas distintas. La cadena vacia significa "la que elija el sistema".
    static String nombrarPlaca(InetAddress grupo, NetworkInterface netIf) {
        if (netIf == null) {
            return "";
        }
        if (grupo instanceof Inet6Address) {
            return Integer.toString(netIf.getIndex());
        }
        java.util.Enumeration<InetAddress> dirs = netIf.getInetAddresses();
        while (dirs.hasMoreElements()) {
            InetAddress d = dirs.nextElement();
            if (d instanceof Inet4Address) {
                return d.getHostAddress();
            }
        }
        // Una placa sin ninguna direccion IPv4 no puede recibir multicast v4; que el sistema elija
        // es mas util que fallar, y es lo que hace el JDK con una placa sin direcciones.
        return "";
    }
}
