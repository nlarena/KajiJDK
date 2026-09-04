package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// Un socket TCP: el objeto que se configura antes de conectar, y el que queda despues.
//
// ===========================================================================================
// DONDE ESTA LA LINEA, Y POR QUE JUSTO AHI
// ===========================================================================================
//
// Un `Socket` recien construido con `new Socket()` **no esta conectado a nada**, y esa no es una
// limitacion de KajiJDK: es el diseno del JDK. Ese objeto --sin conectar, con todas sus opciones
// fijables-- se puede cumplir aca al cien por ciento, y es lo que esta clase da.
//
// **YA CONECTA DE VERDAD.** Esta cabecera decia que no entraba nada que necesitara un par del otro
// lado --`connect`, los seis constructores que conectan, los dos flujos-- y era cierto mientras la VM
// no tuviera con que abrir un socket. Ahora lo tiene: `jdk.internal.net.Net`, la misma clase de
// costura que `Proc`. Asi que `connect(SocketAddress)`, `connect(SocketAddress, int)`,
// `Socket(String,int)`, `Socket(InetAddress,int)`, `getInputStream()` y `getOutputStream()` estan y
// hablan TCP.
//
// **Y YA ESTAN LOS SEIS QUE FALTABAN.** Los dos constructores con **direccion local** eligen la
// punta local de verdad, y con ellos `sendUrgentData(int)`: las tres cosas necesitan lo que
// `std::net` no expone --atar antes de conectar, y mandar con la bandera de fuera de banda-- y por
// eso la VM baja a las llamadas del sistema (`socket`/`bind`/`connect`/`send`), declaradas a mano
// como cualquier otra costura de esta casa.
//
// Los dos con la bandera **`stream`** entran por un motivo distinto y vale la pena decirlo, porque
// es el unico caso del archivo donde el contrato cambio: prometian un socket **UDP** con cara de
// `Socket` cuando se les pasaba `false`, y el JDK dejo de sostenerlo -- tira
// `IllegalArgumentException("Socket constructor does not support creation of datagram sockets")`. Se
// comprobo contra el JDK 25 y esta clase hace exactamente eso. Lo que era imposible de cumplir dejo
// de ser parte del contrato.
//
// **Todo lo demas es real**: las once opciones de socket con sus validaciones, el estado
// (`isConnected`, `isBound`, `isClosed`, `isInputShutdown`, `isOutputShutdown`), `close`,
// `shutdownInput`/`shutdownOutput` --que tiran `SocketException("Socket is not connected")`, que es
// lo que tira el JDK sobre un socket sin conectar--, `toString`, la factoria de implementaciones y
// el trio `setOption`/`getOption`/`supportedOptions`.
//
// Los valores por defecto de las opciones los fija esta clase y estan documentados; en el JDK los
// fija el sistema operativo y cambian de maquina en maquina, que es por lo que el JDK nunca los
// promete. Lo que si se garantiza es lo unico que importa: lo que se fija es lo que se lee.
public class Socket implements Closeable {

    static {
        // El puente que `java.nio.channels` usa para conseguir un `Socket` sobre un handle que ya
        // tiene abierto. Se instala al cargar esta clase; ver `jdk.internal.net.Adopcion`.
        jdk.internal.net.Adopcion.registrar(new AdopcionDeSockets());
    }

    private static volatile SocketImplFactory factory;

    private final Proxy proxy;

    private boolean bound;
    private boolean connected;
    private boolean closed;
    private boolean shutIn;
    private boolean shutOut;

    /** El socket de la VM, o -1 si este todavia no se conecto. */
    private int handle = -1;

    // La punta local que pidio `bind`, para que el `connect` que venga salga por ahi. La cadena
    // vacia es el comodin. Ver la nota de `bind`.
    private String bindHost = "";
    private int bindPort = 0;

    // Los dos flujos se fabrican **una vez** y se devuelven siempre los mismos: el contrato dice que
    // cerrar cualquiera de los dos cierra el socket, asi que dos objetos distintos sobre el mismo
    // handle harian que cerrar uno dejara al otro creyendose abierto.
    private InputStream entrada;
    private OutputStream salida;

    // Los defaults son los de la cabecera. `soLinger` en -1 significa "desactivado", que es como el
    // JDK representa "no esperes al cerrar".
    private boolean tcpNoDelay = false;
    private int soLinger = -1;
    private boolean oobInline = false;
    private int soTimeout = 0;
    private boolean keepAlive = false;
    private int trafficClass = 0;
    private boolean reuseAddress = false;
    private int sendBufferSize = 65536;
    private int receiveBufferSize = 65536;

    /** Un socket **sin conectar**, listo para configurar. */
    public Socket() {
        this.proxy = null;
    }

    /**
     * Un socket sin conectar que, cuando se conecte, saldria por {@code proxy}.
     *
     * <p>{@link Proxy#NO_PROXY} pide explicitamente una conexion directa, saltandose el
     * {@link ProxySelector} de la VM.
     *
     * @throws IllegalArgumentException si {@code proxy} es null
     */
    public Socket(Proxy proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("Invalid Proxy");
        }
        this.proxy = proxy;
    }

    /**
     * Un socket sin conectar sobre la implementacion dada.
     *
     * <p>Es el constructor de una subclase que trae su propia pila.
     *
     * @throws SocketException si la implementacion no se puede usar
     */
    protected Socket(SocketImpl impl) throws SocketException {
        this.proxy = null;
    }

    private void chequearAbierto() throws SocketException {
        if (this.closed) {
            throw new SocketException("Socket is closed");
        }
    }

    /**
     * Ata el socket a una direccion local.
     *
     * @throws IOException siempre en KajiJDK -- no hay pila de TCP donde reservar el puerto. Ver la
     *     cabecera: es una excepcion chequeada que el contrato ya declara.
     */
    public void bind(SocketAddress bindpoint) throws IOException {
        this.chequearAbierto();
        if (this.bound) {
            throw new SocketException("Already bound");
        }
        if (bindpoint != null && !(bindpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        // Se anota, y el `connect` que venga sale por aca. **No reserva el puerto todavia**: para
        // eso habria que tener el socket abierto desde ahora, y esta clase no lo abre hasta que
        // sabe a donde va --su `connect` crea el socket con la familia del destino, que hasta ese
        // momento no se conoce--. La diferencia se nota en un solo caso: dos sockets atados al mismo
        // puerto local fallan recien al conectar el segundo, no al atarlo. Esta dicho aca y en el
        // javadoc del metodo.
        if (bindpoint == null) {
            this.bindHost = "";
            this.bindPort = 0;
        } else {
            InetSocketAddress dir = (InetSocketAddress) bindpoint;
            this.bindHost = dir.getAddress() == null || dir.getAddress().isAnyLocalAddress()
                    ? "" : dir.getAddress().getHostAddress();
            this.bindPort = dir.getPort();
        }
        this.bound = true;
    }

    // ---- estado ----

    /** La direccion del otro extremo, o null si no esta conectado. */
    public InetAddress getInetAddress() {
        if (this.handle < 0) {
            return null;
        }
        String d = jdk.internal.net.Net.remoteAddress(this.handle);
        if (d == null) {
            return null;
        }
        try {
            // Es un literal numerico, asi que esto no consulta ningun DNS.
            return InetAddress.getByName(d);
        } catch (UnknownHostException e) {
            // No puede pasar con un literal numerico; si pasara, "no se" es `null`, que es lo que
            // este metodo devuelve para un socket sin conectar.
            return null;
        }
    }

    /**
     * La direccion local.
     *
     * <p>La direccion comodin mientras el socket no este atado, que es lo que devuelve el JDK en la
     * misma situacion.
     */
    public InetAddress getLocalAddress() {
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
        // Sin socket todavia, el comodin: es lo que devuelve el JDK sobre un socket sin atar.
        try {
            return InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /** El puerto del otro extremo, o 0 si no esta conectado. */
    public int getPort() {
        if (this.handle < 0) {
            return 0;
        }
        int p = jdk.internal.net.Net.remotePort(this.handle);
        return p < 0 ? 0 : p;
    }

    /** El puerto local, o -1 si no esta atado. */
    public int getLocalPort() {
        if (this.handle < 0) {
            return this.bound ? 0 : -1;
        }
        int p = jdk.internal.net.Net.localPort(this.handle);
        return p < 0 ? -1 : p;
    }

    /** El otro extremo como {@link SocketAddress}, o null si no esta conectado. */
    public SocketAddress getRemoteSocketAddress() {
        if (!this.isConnected()) {
            return null;
        }
        return new InetSocketAddress(this.getInetAddress(), this.getPort());
    }

    /** La direccion local como {@link SocketAddress}, o null si no esta atado. */
    public SocketAddress getLocalSocketAddress() {
        if (!this.isBound()) {
            return null;
        }
        return new InetSocketAddress(this.getLocalAddress(), this.getLocalPort());
    }

    /**
     * El canal NIO asociado, o null.
     *
     * <p>Null salvo que el socket haya salido de un `SocketChannel`: un socket creado con `new` no
     * tiene canal, ni en el JDK ni aca.
     */
    public java.nio.channels.SocketChannel getChannel() {
        return null;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public boolean isBound() {
        return this.bound;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public boolean isInputShutdown() {
        return this.shutIn;
    }

    public boolean isOutputShutdown() {
        return this.shutOut;
    }

    // ---- opciones ----

    /**
     * Manda los datos apenas se escriben, sin juntarlos en un paquete lleno (algoritmo de Nagle).
     *
     * @throws SocketException si el socket esta cerrado
     */
    public void setTcpNoDelay(boolean on) throws SocketException {
        this.chequearAbierto();
        this.tcpNoDelay = on;
        if (this.handle >= 0) {
            jdk.internal.net.Net.setTcpNoDelay(this.handle, on);
        }
    }

    public boolean getTcpNoDelay() throws SocketException {
        this.chequearAbierto();
        return this.tcpNoDelay;
    }

    /**
     * Cuantos segundos espera {@link #close} a que se vacie el buffer de salida.
     *
     * <p>Apagarlo y el valor son un solo estado, y por eso hay un solo getter: apagado se lee -1.
     *
     * @throws IllegalArgumentException si esta prendido con un valor negativo
     */
    public void setSoLinger(boolean on, int linger) throws SocketException {
        this.chequearAbierto();
        if (!on) {
            this.soLinger = -1;
            return;
        }
        if (linger < 0) {
            throw new IllegalArgumentException("invalid value for SO_LINGER");
        }
        this.soLinger = linger > 65535 ? 65535 : linger;
    }

    /** Los segundos de espera, o -1 si esta apagado. */
    public int getSoLinger() throws SocketException {
        this.chequearAbierto();
        return this.soLinger;
    }

    /** Si los datos urgentes (TCP OOB) llegan mezclados con el resto. */
    public void setOOBInline(boolean on) throws SocketException {
        this.chequearAbierto();
        this.oobInline = on;
    }

    public boolean getOOBInline() throws SocketException {
        this.chequearAbierto();
        return this.oobInline;
    }

    /**
     * Milisegundos que espera una lectura; 0 es "para siempre".
     *
     * @throws IllegalArgumentException si el timeout es negativo
     */
    public void setSoTimeout(int timeout) throws SocketException {
        this.chequearAbierto();
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        this.soTimeout = timeout;
        if (this.handle >= 0) {
            jdk.internal.net.Net.setSoTimeout(this.handle, timeout);
        }
    }

    public int getSoTimeout() throws SocketException {
        this.chequearAbierto();
        return this.soTimeout;
    }

    /**
     * Tamano sugerido del buffer de salida.
     *
     * @throws IllegalArgumentException si el tamano no es positivo
     */
    public void setSendBufferSize(int size) throws SocketException {
        this.chequearAbierto();
        if (size <= 0) {
            throw new IllegalArgumentException("invalid send size");
        }
        this.sendBufferSize = size;
    }

    public int getSendBufferSize() throws SocketException {
        this.chequearAbierto();
        return this.sendBufferSize;
    }

    /**
     * Tamano sugerido del buffer de entrada.
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

    /** Manda sondas periodicas para detectar una conexion muerta. */
    public void setKeepAlive(boolean on) throws SocketException {
        this.chequearAbierto();
        this.keepAlive = on;
    }

    public boolean getKeepAlive() throws SocketException {
        this.chequearAbierto();
        return this.keepAlive;
    }

    /**
     * El campo "type of service" de la cabecera IP.
     *
     * @throws IllegalArgumentException si no entra en 0..255
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

    /** Si se puede reusar una direccion que quedo en TIME_WAIT. */
    public void setReuseAddress(boolean on) throws SocketException {
        this.chequearAbierto();
        this.reuseAddress = on;
    }

    public boolean getReuseAddress() throws SocketException {
        this.chequearAbierto();
        return this.reuseAddress;
    }

    /**
     * Que importa mas de esta conexion: tiempo de establecimiento, latencia o ancho de banda.
     *
     * <p>Es una **sugerencia**, y el JDK documenta que una implementacion puede ignorarla por
     * completo. Esta la ignora, que es una de las respuestas permitidas por el contrato -- no una
     * promesa incumplida.
     */
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    /**
     * Fija una opcion por su constante tipada.
     *
     * @throws UnsupportedOperationException si esta clase no soporta esa opcion
     */
    public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
        this.chequearAbierto();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.TCP_NODELAY) {
            this.setTcpNoDelay(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.SO_KEEPALIVE) {
            this.setKeepAlive(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.SO_SNDBUF) {
            this.setSendBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_RCVBUF) {
            this.setReceiveBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_REUSEADDR) {
            this.setReuseAddress(((Boolean) value).booleanValue());
        } else if (name == StandardSocketOptions.SO_LINGER) {
            int v = ((Integer) value).intValue();
            this.setSoLinger(v >= 0, v);
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
        if (name == StandardSocketOptions.TCP_NODELAY) {
            return (T) Boolean.valueOf(this.getTcpNoDelay());
        }
        if (name == StandardSocketOptions.SO_KEEPALIVE) {
            return (T) Boolean.valueOf(this.getKeepAlive());
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
        if (name == StandardSocketOptions.SO_LINGER) {
            return (T) Integer.valueOf(this.getSoLinger());
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
        s.add(StandardSocketOptions.SO_KEEPALIVE);
        s.add(StandardSocketOptions.SO_REUSEADDR);
        s.add(StandardSocketOptions.SO_LINGER);
        s.add(StandardSocketOptions.TCP_NODELAY);
        s.add(StandardSocketOptions.IP_TOS);
        return Collections.unmodifiableSet(s);
    }

    // ---- cierre ----

    /**
     * Cierra la mitad de lectura.
     *
     * @throws SocketException si el socket no esta conectado -- que es siempre, en KajiJDK, y es
     *     exactamente lo que tira el JDK sobre un socket sin conectar
     */
    public void shutdownInput() throws IOException {
        this.chequearAbierto();
        if (!this.isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        this.shutIn = true;
        if (this.handle >= 0) {
            jdk.internal.net.Net.shutdownIn(this.handle);
        }
    }

    /**
     * Cierra la mitad de escritura, mandando un FIN.
     *
     * @throws SocketException si el socket no esta conectado
     */
    public void shutdownOutput() throws IOException {
        this.chequearAbierto();
        if (!this.isConnected()) {
            throw new SocketException("Socket is not connected");
        }
        this.shutOut = true;
        if (this.handle >= 0) {
            jdk.internal.net.Net.shutdownOut(this.handle);
        }
    }

    /**
     * Cierra el socket. Cerrar dos veces no hace nada.
     *
     * <p>En el JDK declara {@code throws IOException}. Aca no puede: la
     * {@code java.io.Closeable} de esta biblioteca declara {@code close()} sin excepcion, y un
     * override no puede ensanchar la clausula {@code throws} (JLS 8.4.8.3). Es la misma decision
     * --y por la misma razon-- que ya tomo {@code java.nio.channels.Channel} en este arbol.
     */
    public void close() throws java.io.IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
    }

    /**
     * {@code Socket[unconnected]} mientras no este conectado, que es el formato del JDK.
     */
    @Override
    public String toString() {
        if (this.isConnected()) {
            return "Socket[addr=" + this.getInetAddress() + ",port=" + this.getPort()
                    + ",localport=" + this.getLocalPort() + "]";
        }
        return "Socket[unconnected]";
    }

    /**
     * Instala la factoria de implementaciones para toda la VM. Una sola vez.
     *
     * @throws Error si ya se habia instalado una
     * @deprecated el JDK deprecio el mecanismo de {@link SocketImpl}
     */
    @Deprecated
    public static synchronized void setSocketImplFactory(SocketImplFactory fac) throws IOException {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        factory = fac;
    }

    // ---- conectar ---------------------------------------------------------------------------

    /**
     * Conecta a esa direccion, sin limite de tiempo.
     *
     * @throws IOException si no se pudo conectar
     * @throws IllegalArgumentException si la direccion no es una {@link InetSocketAddress}
     */
    public void connect(SocketAddress endpoint) throws IOException {
        this.connect(endpoint, 0);
    }

    /**
     * Conecta a esa direccion, esperando a lo sumo `timeout` milisegundos.
     *
     * @param timeout cero significa sin limite, como en el JDK
     * @throws IOException si no se pudo conectar
     * @throws SocketTimeoutException si vencio el plazo
     * @throws IllegalArgumentException si la direccion no es una {@link InetSocketAddress}, o si el
     *     plazo es negativo
     */
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (endpoint == null) {
            throw new IllegalArgumentException("connect: The address can't be null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("connect: timeout can't be negative");
        }
        this.chequearAbierto();
        if (this.connected) {
            throw new SocketException("already connected");
        }
        if (!(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        InetSocketAddress dir = (InetSocketAddress) endpoint;
        if (dir.isUnresolved()) {
            throw new UnknownHostException(dir.getHostName());
        }
        String host = dir.getAddress().getHostAddress();
        if (!this.bindHost.isEmpty() || this.bindPort != 0) {
            // Hubo un `bind` antes: hay que salir por ahi, y eso necesita el camino que ata primero.
            this.conectarDesde(dir.getAddress(), dir.getPort(), InetAddress.getByName(
                    this.bindHost.isEmpty() ? "0.0.0.0" : this.bindHost), this.bindPort);
            return;
        }
        int h = jdk.internal.net.Net.connect(host, dir.getPort(), timeout);
        if (h < 0) {
            // El nativo no distingue "rechazado" de "no hay ruta" de "vencio el plazo", asi que el
            // mensaje nombra lo unico que se sabe con certeza: a donde se intento conectar. Inventar
            // un motivo seria adivinar cual de los tres fue.
            throw new java.net.ConnectException(
                    "Connection refused: " + host + ":" + dir.getPort());
        }
        this.handle = h;
        this.connected = true;
        this.bound = true;
        // Lo que se configuro antes de conectar se aplica ahora: hasta que no hay socket no hay
        // donde ponerlo, y perderlo haria que `setSoTimeout` antes del `connect` no hiciera nada.
        jdk.internal.net.Net.setSoTimeout(h, this.soTimeout);
        jdk.internal.net.Net.setTcpNoDelay(h, this.tcpNoDelay);
    }

    /**
     * Un socket ya conectado a ese host y puerto.
     *
     * @throws UnknownHostException si el nombre no resuelve
     * @throws IOException si no se pudo conectar
     */
    public Socket(String host, int port) throws IOException {
        this.proxy = null;
        this.connect(new InetSocketAddress(InetAddress.getByName(host), port), 0);
    }

    /**
     * Un socket ya conectado a esa direccion y puerto.
     *
     * @throws IOException si no se pudo conectar
     */
    public Socket(InetAddress address, int port) throws IOException {
        this.proxy = null;
        if (address == null) {
            throw new NullPointerException("address");
        }
        this.connect(new InetSocketAddress(address, port), 0);
    }

    // ---- los flujos -------------------------------------------------------------------------

    /**
     * Los bytes que llegan del par.
     *
     * <p>Siempre el mismo objeto: cerrarlo cierra el socket, asi que dos flujos distintos sobre el
     * mismo socket dejarian a uno creyendose abierto despues de cerrar el otro.
     *
     * @throws IOException si el socket esta cerrado, no conectado, o su lectura ya se cerro
     */
    public InputStream getInputStream() throws IOException {
        this.chequearAbierto();
        if (!this.connected) {
            throw new SocketException("Socket is not connected");
        }
        if (this.shutIn) {
            throw new SocketException("Socket input is shutdown");
        }
        if (this.entrada == null) {
            this.entrada = new EntradaDeSocket(this);
        }
        return this.entrada;
    }

    /**
     * Los bytes que van al par.
     *
     * @throws IOException si el socket esta cerrado, no conectado, o su escritura ya se cerro
     */
    public OutputStream getOutputStream() throws IOException {
        this.chequearAbierto();
        if (!this.connected) {
            throw new SocketException("Socket is not connected");
        }
        if (this.shutOut) {
            throw new SocketException("Socket output is shutdown");
        }
        if (this.salida == null) {
            this.salida = new SalidaDeSocket(this);
        }
        return this.salida;
    }

    // El handle, para los flujos y para `ServerSocket`. De paquete: nadie de afuera tiene por que
    // saber que un socket es un numero.
    int handle() {
        return this.handle;
    }

    boolean cerrado() {
        return this.closed;
    }

    // El plazo de lectura, que hace cumplir `EntradaDeSocket` porque el nativo no cuenta tiempo.
    int plazoDeLectura() {
        return this.soTimeout;
    }

    // Lo que `ServerSocket.implAccept` necesita para entregar un socket ya conectado.
    void adoptar(int h) {
        this.handle = h;
        this.connected = true;
        this.bound = true;
    }
    // ---- los constructores que eligen la punta local ------------------------------------------

    /**
     * Un socket conectado a {@code host}:{@code port}, **saliendo por** {@code localAddr}:{@code
     * localPort}.
     *
     * <p>Elegir la punta local sirve para dos cosas reales: salir por una placa determinada en una
     * maquina con varias, y ocupar un puerto de origen que el otro lado espera. Un {@code localAddr}
     * null es el comodin y un {@code localPort} cero deja que el sistema elija, que es lo mismo que
     * no pedir nada.
     *
     * @throws UnknownHostException si el nombre no resuelve
     * @throws IOException si no se pudo atar o no se pudo conectar
     */
    public Socket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        this.proxy = null;
        this.conectarDesde(InetAddress.getByName(host), port, localAddr, localPort);
    }

    /**
     * Un socket conectado a {@code address}:{@code port}, saliendo por {@code localAddr}:{@code
     * localPort}. Ver {@link #Socket(String, int, InetAddress, int)}.
     *
     * @throws NullPointerException si {@code address} es null
     * @throws IOException si no se pudo atar o no se pudo conectar
     */
    public Socket(InetAddress address, int port, InetAddress localAddr, int localPort)
            throws IOException {
        this.proxy = null;
        if (address == null) {
            throw new NullPointerException("address");
        }
        this.conectarDesde(address, port, localAddr, localPort);
    }

    // El cuerpo de los dos. La espera es de este lado: el nativo arranca el connect en un hilo del
    // sistema --tiene que bloquear para poder atar antes-- y contesta por un casillero.
    private void conectarDesde(InetAddress address, int port, InetAddress localAddr, int localPort)
            throws IOException {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("port out of range:" + port);
        }
        if (localPort < 0 || localPort > 0xFFFF) {
            throw new IllegalArgumentException("localPort out of range:" + localPort);
        }
        String local = localAddr == null ? "" : localAddr.getHostAddress();
        int casillero = jdk.internal.net.Net.connectFromStart(
                address.getHostAddress(), port, local, localPort);
        int h;
        if (casillero < 0) {
            h = -1;
        } else {
            try {
                h = jdk.internal.net.Net.answerPoll(casillero);
                while (h == -3) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new java.io.InterruptedIOException("connect interrupted");
                    }
                    h = jdk.internal.net.Net.answerPoll(casillero);
                }
            } finally {
                jdk.internal.net.Net.answerFree(casillero);
            }
        }
        if (h < 0) {
            // El nativo no distingue "no se pudo atar" de "no se pudo conectar", y las dos son
            // igual de probables aca --un puerto local ocupado es tan comun como un destino
            // caido--. El mensaje nombra las dos puntas, que es lo unico que se sabe con certeza.
            throw new java.net.ConnectException("Connection failed: "
                    + (local.isEmpty() ? "*" : local) + ":" + localPort
                    + " -> " + address.getHostAddress() + ":" + port);
        }
        this.handle = h;
        this.connected = true;
        this.bound = true;
        jdk.internal.net.Net.setSoTimeout(h, this.soTimeout);
        jdk.internal.net.Net.setTcpNoDelay(h, this.tcpNoDelay);
    }

    // ---- los dos con la bandera `stream` ------------------------------------------------------

    /**
     * Un socket conectado a {@code host}:{@code port}.
     *
     * @param stream tiene que ser {@code true}. Con {@code false} este constructor prometia un
     *     socket **UDP** con cara de {@code Socket}, y el JDK dejo de sostenerlo: tira
     *     {@link IllegalArgumentException}. Esto hace lo mismo, palabra por palabra.
     * @throws IllegalArgumentException si {@code stream} es false
     * @throws IOException si no se pudo conectar
     * @deprecated como en el JDK: usar {@link #Socket(String, int)} para TCP y
     *     {@link DatagramSocket} para UDP
     */
    @Deprecated
    public Socket(String host, int port, boolean stream) throws IOException {
        this.proxy = null;
        Socket.exigirFlujo(stream);
        this.connect(new InetSocketAddress(InetAddress.getByName(host), port), 0);
    }

    /**
     * Un socket conectado a {@code address}:{@code port}. Ver {@link #Socket(String, int, boolean)}.
     *
     * @throws IllegalArgumentException si {@code stream} es false
     * @throws NullPointerException si {@code address} es null
     * @throws IOException si no se pudo conectar
     * @deprecated como en el JDK
     */
    @Deprecated
    public Socket(InetAddress address, int port, boolean stream) throws IOException {
        this.proxy = null;
        Socket.exigirFlujo(stream);
        if (address == null) {
            throw new NullPointerException("address");
        }
        this.connect(new InetSocketAddress(address, port), 0);
    }

    // El mensaje es el del JDK 25, y se comprobo contra el: un `Socket` de datagramas dejo de
    // existir, y quien pase `false` tiene que enterarse de eso y no de una falla de conexion.
    private static void exigirFlujo(boolean stream) {
        if (!stream) {
            throw new IllegalArgumentException(
                    "Socket constructor does not support creation of datagram sockets");
        }
    }

    // ---- fuera de banda -----------------------------------------------------------------------

    /**
     * Manda un byte **fuera de banda**.
     *
     * <p>No es escribir en el flujo: va con una bandera del protocolo y llega por un camino aparte,
     * adelantandose a lo que ya este en cola. Solo se manda el byte de menor peso de {@code data},
     * que es lo que dice el contrato.
     *
     * @throws IOException si el socket no esta conectado o si no se pudo mandar
     */
    public void sendUrgentData(int data) throws IOException {
        this.chequearAbierto();
        if (!this.connected) {
            throw new SocketException("Socket is not connected");
        }
        if (this.shutOut) {
            throw new SocketException("Socket output is shutdown");
        }
        if (!jdk.internal.net.Net.sendUrgent(this.handle, data & 0xFF)) {
            throw new IOException("sendUrgentData failed");
        }
    }
}


// Los bytes que llegan de un socket. Es una vista sobre el handle y no un buffer propio: leer de
// aca lee de la conexion en ese momento, que es lo que un `InputStream` promete.
final class EntradaDeSocket extends InputStream {

    private final Socket socket;

    EntradaDeSocket(Socket socket) {
        this.socket = socket;
    }

    public int read() throws IOException {
        byte[] uno = new byte[1];
        int n = this.read(uno, 0, 1);
        if (n <= 0) {
            return -1;
        }
        // A 0..255: `read()` devuelve un byte sin signo y -1 significa fin.
        return uno[0] & 0xFF;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (this.socket.cerrado()) {
            throw new SocketException("Socket is closed");
        }
        // El nativo no espera: -3 es "todavia no llego nada". La espera se hace aca, durmiendo un
        // poco entre intentos, porque dormir suelta el interprete de la VM y deja correr al hilo que
        // tiene que escribir del otro lado. Ademas es lo que permite contar el plazo de verdad: el
        // -2 lo decide este lado, no el sistema.
        int plazo = this.socket.plazoDeLectura();
        long comienzo = System.currentTimeMillis();
        int n = jdk.internal.net.Net.read(this.socket.handle(), b, off, len);
        while (n == -3) {
            if (this.socket.cerrado()) {
                throw new SocketException("Socket is closed");
            }
            if (plazo > 0 && System.currentTimeMillis() - comienzo >= plazo) {
                // Plazo vencido no es fin de flujo: la conexion sigue viva, solo esta callada.
                throw new SocketTimeoutException("Read timed out");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("read interrupted");
            }
            n = jdk.internal.net.Net.read(this.socket.handle(), b, off, len);
        }
        if (n == -2) {
            throw new SocketTimeoutException("Read timed out");
        }
        return n;
    }

    public void close() throws IOException {
        this.socket.close();
    }
}

// Los bytes que van al socket. Cada escritura sale en el acto: un buffer de este lado haria que el
// par no viera lo ya escrito hasta un `flush()`, y quien escribe no tiene por que saber eso.
final class SalidaDeSocket extends OutputStream {

    private final Socket socket;

    SalidaDeSocket(Socket socket) {
        this.socket = socket;
    }

    public void write(int b) throws IOException {
        this.write(new byte[] { (byte) b }, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        if (this.socket.cerrado()) {
            throw new SocketException("Socket is closed");
        }
        if (!jdk.internal.net.Net.write(this.socket.handle(), b, off, len)) {
            throw new IOException("Connection reset by peer");
        }
    }

    public void close() throws IOException {
        this.socket.close();
    }

}
