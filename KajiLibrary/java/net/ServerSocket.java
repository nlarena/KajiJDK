package java.net;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// El socket que escucha: se ata a un puerto y espera conexiones.
//
// ===========================================================================================
// DONDE ESTA LA LINEA
// ===========================================================================================
//
// `new ServerSocket()` --el constructor sin argumentos-- crea un socket **sin atar**. Eso es del
// JDK, no una version recortada, y se cumple aca entero: sobre ese objeto andan todas las
// opciones, el estado y `close`.
//
// **NO ENTRAN los dos que esperan a alguien:**
//
//  - `accept()`. Su contrato es devolver un `Socket` **conectado a un cliente**. No hay cliente, no
//    hay pila de TCP, y no hay forma de cumplirlo. Un `accept` que devolviera un socket inventado
//    seria la peor mentira de esta API entera --el servidor creeria que atendio a alguien-- y uno
//    que fallara siempre dejaria compilar un servidor completo que no sirve.
//  - `implAccept(Socket)`. Es la mitad de abajo de `accept`, para las subclases. Sin `accept` no
//    tiene sentido, y promete lo mismo.
//
// **SI ENTRAN los constructores que atan y `bind`**, tirando `IOException`. Atar es local
// --reservar un puerto en esta maquina-- y su fracaso es literalmente el caso que el contrato
// describe. La excepcion es chequeada: el compilador obliga a mirarla, asi que nadie se entera
// tarde.
//
// Todo lo demas --`setSoTimeout`, `setReuseAddress`, `setReceiveBufferSize`,
// `setPerformancePreferences`, `setOption`/`getOption`/`supportedOptions`, el estado, `toString` y
// la factoria-- es configuracion, y esta completo.
public class ServerSocket implements Closeable {

    private static volatile SocketImplFactory factory;

    private boolean bound;
    private boolean closed;

    /** El socket a la escucha de la VM, o -1 si todavia no se ato. */
    private int handle = -1;

    // Lo que necesita un `ServerSocketChannel` para entregar el socket que lo envuelve: el canal ya
    // tiene el descriptor abierto, y este objeto pasa a compartirlo. De paquete a proposito --nadie
    // de afuera tiene por que saber que un socket es un numero-- y por eso el puente
    // `jdk.internal.net.Adopcion`.
    void adoptar(int h) {
        this.handle = h;
        this.bound = true;
    }

    private int soTimeout = 0;
    private boolean reuseAddress = false;
    private int receiveBufferSize = 65536;

    /** Un socket servidor **sin atar**, listo para configurar. */
    public ServerSocket() throws IOException {
    }

    /**
     * Un socket servidor sobre la implementacion dada, sin atar.
     *
     * <p>Es el constructor de una subclase que trae su propia pila.
     */
    protected ServerSocket(SocketImpl impl) {
    }

    /**
     * Un socket servidor atado a {@code port}.
     *
     * <p>Un puerto cero deja que el sistema elija uno; el que toco se lee con
     * {@link #getLocalPort}.
     *
     * @throws IOException si no se pudo atar (el puerto ocupado, sin permiso)
     */
    public ServerSocket(int port) throws IOException {
        this(port, 50, null);
    }

    /**
     * Un socket servidor atado a {@code port}, con una cola de {@code backlog} conexiones.
     *
     * @throws IOException si no se pudo atar
     */
    public ServerSocket(int port, int backlog) throws IOException {
        this(port, backlog, null);
    }

    /**
     * Un socket servidor atado a {@code bindAddr}:{@code port}.
     *
     * @throws IllegalArgumentException si el puerto esta fuera de rango
     * @throws IOException si no se pudo atar
     */
    public ServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        if (port < 0 || port > 0xFFFF) {
            throw new IllegalArgumentException("Port value out of range: " + port);
        }
        this.bind(new InetSocketAddress(bindAddr, port), backlog);
    }

    private void chequearAbierto() throws SocketException {
        if (this.closed) {
            throw new SocketException("Socket is closed");
        }
    }

    /**
     * Ata el socket a {@code endpoint}, con la cola de conexiones por defecto.
     *
     * @throws IOException siempre en KajiJDK
     */
    public void bind(SocketAddress endpoint) throws IOException {
        this.bind(endpoint, 50);
    }

    /**
     * Ata el socket a {@code endpoint}, con una cola de {@code backlog} conexiones.
     *
     * @throws IOException si no se pudo atar
     */
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        this.chequearAbierto();
        if (this.bound) {
            throw new SocketException("Already bound");
        }
        if (endpoint != null && !(endpoint instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        // Sin direccion, el comodin: escuchar en todas las interfaces, que es lo que
        // `new ServerSocket(puerto)` promete.
        String host = "0.0.0.0";
        int puerto = 0;
        if (endpoint != null) {
            InetSocketAddress dir = (InetSocketAddress) endpoint;
            if (dir.getAddress() != null && !dir.getAddress().isAnyLocalAddress()) {
                host = dir.getAddress().getHostAddress();
            }
            puerto = dir.getPort();
        }
        int h = jdk.internal.net.Net.listen(host, puerto, backlog <= 0 ? 50 : backlog);
        if (h < 0) {
            // El nativo no distingue "puerto ocupado" de "sin permiso"; el mensaje nombra lo unico
            // que se sabe con certeza.
            throw new BindException("Cannot assign requested address: " + host + ":" + puerto);
        }
        this.handle = h;
        this.bound = true;
    }

    /** La direccion local a la que esta atado, o null si no lo esta. */
    public InetAddress getInetAddress() {
        if (!this.isBound()) {
            return null;
        }
        try {
            return InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /** El puerto en el que escucha, o -1 si no esta atado. */
    public int getLocalPort() {
        if (this.handle >= 0) {
            int p = jdk.internal.net.Net.localPort(this.handle);
            if (p >= 0) {
                return p;
            }
        }
        return this.isBound() ? 0 : -1;
    }

    /** La direccion local como {@link SocketAddress}, o null si no esta atado. */
    public SocketAddress getLocalSocketAddress() {
        if (!this.isBound()) {
            return null;
        }
        return new InetSocketAddress(this.getInetAddress(), this.getLocalPort());
    }

    public boolean isBound() {
        return this.bound;
    }

    public boolean isClosed() {
        return this.closed;
    }

    /**
     * El canal NIO asociado, o null.
     *
     * <p>Null salvo que el socket haya salido de un `ServerSocketChannel`, igual que en el JDK.
     */
    public java.nio.channels.ServerSocketChannel getChannel() {
        return null;
    }

    // ---- opciones ----

    /**
     * Milisegundos que espera una conexion entrante; 0 es "para siempre".
     *
     * @throws IllegalArgumentException si el timeout es negativo
     */
    public void setSoTimeout(int timeout) throws SocketException {
        this.chequearAbierto();
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can't be negative");
        }
        this.soTimeout = timeout;
    }

    /** El timeout de espera. Declara `IOException` y no `SocketException`: es asi en el JDK. */
    public int getSoTimeout() throws IOException {
        this.chequearAbierto();
        return this.soTimeout;
    }

    /**
     * Si se puede reusar un puerto que quedo en TIME_WAIT.
     *
     * <p>Es la opcion que hace que un servidor pueda reiniciarse sin esperar dos minutos, y por eso
     * hay que fijarla **antes** de atar: despues no tiene efecto.
     */
    public void setReuseAddress(boolean on) throws SocketException {
        this.chequearAbierto();
        this.reuseAddress = on;
    }

    public boolean getReuseAddress() throws SocketException {
        this.chequearAbierto();
        return this.reuseAddress;
    }

    /**
     * Tamano sugerido del buffer de entrada que **heredan** los sockets aceptados.
     *
     * <p>Va aca y no en `Socket` porque para pedir una ventana de mas de 64 KiB hay que fijarla
     * antes del handshake, y el socket aceptado no existe todavia en ese momento.
     *
     * @throws IllegalArgumentException si el tamano no es positivo
     */
    public void setReceiveBufferSize(int size) throws SocketException {
        this.chequearAbierto();
        if (size <= 0) {
            throw new IllegalArgumentException("negative receive size");
        }
        this.receiveBufferSize = size;
    }

    public int getReceiveBufferSize() throws SocketException {
        this.chequearAbierto();
        return this.receiveBufferSize;
    }

    /**
     * Que importa mas de estas conexiones: tiempo de establecimiento, latencia o ancho de banda.
     *
     * <p>Es una sugerencia que el JDK permite ignorar por completo, y esta implementacion la
     * ignora -- una de las respuestas que el contrato admite, no una promesa incumplida.
     */
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    /**
     * Fija una opcion por su constante tipada.
     *
     * @throws UnsupportedOperationException si esta clase no soporta esa opcion
     */
    public <T> ServerSocket setOption(SocketOption<T> name, T value) throws IOException {
        this.chequearAbierto();
        if (name == null) {
            throw new NullPointerException();
        }
        if (name == StandardSocketOptions.SO_RCVBUF) {
            this.setReceiveBufferSize(((Integer) value).intValue());
        } else if (name == StandardSocketOptions.SO_REUSEADDR) {
            this.setReuseAddress(((Boolean) value).booleanValue());
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
        if (name == StandardSocketOptions.SO_RCVBUF) {
            return (T) Integer.valueOf(this.getReceiveBufferSize());
        }
        if (name == StandardSocketOptions.SO_REUSEADDR) {
            return (T) Boolean.valueOf(this.getReuseAddress());
        }
        throw new UnsupportedOperationException("'" + name + "' not supported");
    }

    /**
     * Las opciones que este socket entiende.
     *
     * <p>Son menos que las de {@link Socket}, y no es un recorte: un socket que escucha no tiene
     * buffer de salida ni algoritmo de Nagle que configurar.
     */
    public Set<SocketOption<?>> supportedOptions() {
        Set<SocketOption<?>> s = new HashSet<SocketOption<?>>();
        s.add(StandardSocketOptions.SO_RCVBUF);
        s.add(StandardSocketOptions.SO_REUSEADDR);
        s.add(StandardSocketOptions.IP_TOS);
        return Collections.unmodifiableSet(s);
    }

    /**
     * Cierra el socket servidor. Cerrar dos veces no hace nada.
     *
     * <p>En el JDK declara {@code throws IOException}. Aca no puede: la
     * {@code java.io.Closeable} de esta biblioteca declara {@code close()} sin excepcion, y un
     * override no puede ensanchar la clausula {@code throws} (JLS 8.4.8.3). Misma decision que
     * {@code java.nio.channels.Channel} en este arbol.
     */
    public void close() throws java.io.IOException {
        if (this.handle >= 0) {
            jdk.internal.net.Net.close(this.handle);
            this.handle = -1;
        }
        this.closed = true;
    }

    /** {@code ServerSocket[unbound]} mientras no este atado, que es el formato del JDK. */
    @Override
    public String toString() {
        if (!this.isBound()) {
            return "ServerSocket[unbound]";
        }
        return "ServerSocket[addr=" + this.getInetAddress() + ",localport=" + this.getLocalPort()
                + "]";
    }

    /**
     * Instala la factoria de implementaciones para toda la VM. Una sola vez.
     *
     * @throws Error si ya se habia instalado una
     * @deprecated el JDK deprecio el mecanismo de {@link SocketImpl}
     */
    @Deprecated
    public static synchronized void setSocketFactory(SocketImplFactory fac) throws IOException {
        if (factory != null) {
            throw new Error("factory already defined");
        }
        factory = fac;
    }

    /**
     * Espera una conexion y devuelve el socket que la atiende.
     *
     * <p>Espera hasta que alguien conecte, o hasta que venza el plazo puesto con
     * {@link #setSoTimeout}. Ese plazo se respeta de verdad: el nativo no espera --contesta
     * "todavia no" en el acto-- y quien cuenta el tiempo es este metodo, que es el que sabe cuando
     * empezo a esperar.
     *
     * @throws SocketTimeoutException si vencio el plazo sin que nadie conectara
     * @throws IOException si el socket esta cerrado o sin atar, o si fallo el accept
     */
    public Socket accept() throws IOException {
        this.chequearAbierto();
        if (!this.bound) {
            throw new SocketException("Socket is not bound yet");
        }
        Socket s = new Socket();
        this.implAccept(s);
        return s;
    }

    /**
     * Acepta una conexion **sobre el socket que se le da**.
     *
     * <p>Existe para que una subclase pueda entregar su propia clase de socket: redefine
     * {@link #accept} para construir la suya y llama a este con ella. Por eso es `final` -- lo que
     * la subclase cambia es que socket se pasa, no como se acepta.
     *
     * @throws IOException si fallo el accept
     */
    protected final void implAccept(Socket s) throws IOException {
        if (s == null) {
            throw new NullPointerException("s");
        }
        // El -3 es "todavia no hay nadie". Se reintenta durmiendo un poco entre intentos: dormir
        // suelta el interprete de la VM, y eso es justamente lo que le deja lugar al hilo que va a
        // conectar. Un milisegundo es corto para quien espera y largo para no quemar el procesador.
        long comienzo = System.currentTimeMillis();
        int h = jdk.internal.net.Net.accept(this.handle);
        while (h == -3) {
            if (this.closed) {
                throw new SocketException("Socket is closed");
            }
            if (this.soTimeout > 0
                    && System.currentTimeMillis() - comienzo >= this.soTimeout) {
                throw new SocketTimeoutException("Accept timed out");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.InterruptedIOException("accept interrupted");
            }
            h = jdk.internal.net.Net.accept(this.handle);
        }
        if (h < 0) {
            throw new IOException("accept failed");
        }
        s.adoptar(h);
    }
}
