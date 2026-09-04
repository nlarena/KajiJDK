package java.net;

// Un datagrama: un bloque de bytes con un destino, o con un remitente.
//
// ===========================================================================================
// POR QUE ESTA ENTERA AUNQUE NO HAYA RED
// ===========================================================================================
//
// `DatagramPacket` **no manda ni recibe nada**. Es el sobre, no el correo: guarda un buffer, un
// tramo dentro de ese buffer, y una direccion con un puerto. Mandarlo es trabajo de
// `DatagramSocket`, que en KajiJDK no existe porque no hay nativos de red.
//
// Que el sobre exista sin correo no es una promesa vacia. Todos sus miembros son accesores sobre
// campos y validaciones de rango, y todos hacen exactamente lo que dicen. Un programa puede armar
// un datagrama, leerlo, reusarlo y verificarlo aca igual que en el JDK; lo unico que no puede es
// entregarlo, y de eso se entera al buscar `DatagramSocket` y no encontrarlo -- un error de
// compilacion, que es la forma honesta de decirlo.
//
// ===========================================================================================
// EL DETALLE QUE IMPORTA: `length` ES DE ENTRADA Y DE SALIDA
// ===========================================================================================
//
// Al **recibir**, `length` entra valiendo cuanto espacio hay y sale valiendo cuantos bytes
// llegaron. Por eso reusar un paquete para varias recepciones es un error clasico: despues de
// recibir 10 bytes en un buffer de 1024, `length` quedo en 10, y la recepcion siguiente trunca a
// 10 bytes sin avisar. La cura es `setLength(buf.length)` antes de cada recepcion.
//
// Se documenta aca porque es la parte del contrato que la firma no muestra, y este archivo es todo
// lo que queda de ese contrato en este arbol.
//
// Los dieciocho miembros estan; nada omitido.
public final class DatagramPacket {

    private byte[] buf;
    private int offset;
    private int length;

    // Cuanto se pidio en el ultimo `setLength`/`setData`. Al recibir, `length` se pisa con lo que
    // llego, y sin este campo no se podria saber cuanto entraba en realidad.
    private int bufLength;

    private InetAddress address;

    // Arranca en 0 y no en -1. Parece un detalle y no lo es: un paquete recien construido sin
    // destino reporta el puerto 0, que es lo que hace el JDK 25 --las versiones viejas ponian -1--
    // y lo que hace que `getSocketAddress()` de un paquete sin destino devuelva la direccion
    // comodin con puerto 0 en vez de tirar por un puerto invalido.
    private int port;

    /**
     * Un paquete que usa {@code length} bytes de {@code buf} a partir de {@code offset}.
     *
     * <p>Sin direccion: sirve para **recibir**, o para mandarlo despues de ponerle una.
     *
     * @throws IllegalArgumentException si el tramo no entra en el buffer
     * @throws NullPointerException     si {@code buf} es null
     */
    public DatagramPacket(byte[] buf, int offset, int length) {
        setData(buf, offset, length);
    }

    /**
     * Un paquete que usa los primeros {@code length} bytes de {@code buf}.
     *
     * @throws IllegalArgumentException si {@code length} no entra en el buffer
     */
    public DatagramPacket(byte[] buf, int length) {
        this(buf, 0, length);
    }

    /**
     * Un paquete listo para mandar a {@code address}:{@code port}.
     *
     * @throws IllegalArgumentException si el tramo no entra, o el puerto esta fuera de 0..65535
     */
    public DatagramPacket(byte[] buf, int offset, int length, InetAddress address, int port) {
        setData(buf, offset, length);
        setAddress(address);
        setPort(port);
    }

    /**
     * Un paquete listo para mandar a {@code address}, dada como direccion de socket.
     *
     * @throws IllegalArgumentException si {@code address} no es una {@link InetSocketAddress}
     *                                  resuelta, o el tramo no entra
     */
    public DatagramPacket(byte[] buf, int offset, int length, SocketAddress address) {
        setData(buf, offset, length);
        setSocketAddress(address);
    }

    /**
     * Un paquete listo para mandar, usando los primeros {@code length} bytes.
     *
     * @throws IllegalArgumentException si el tramo no entra, o el puerto esta fuera de rango
     */
    public DatagramPacket(byte[] buf, int length, InetAddress address, int port) {
        this(buf, 0, length, address, port);
    }

    /**
     * Un paquete listo para mandar, usando los primeros {@code length} bytes.
     *
     * @throws IllegalArgumentException si {@code address} no es una {@link InetSocketAddress}
     *                                  resuelta, o el tramo no entra
     */
    public DatagramPacket(byte[] buf, int length, SocketAddress address) {
        this(buf, 0, length, address);
    }

    /** A quien va, o de quien vino; null si no se le puso ninguna. */
    public synchronized InetAddress getAddress() {
        return this.address;
    }

    /** El puerto de destino o de origen; 0 si no se le puso ninguno. */
    public synchronized int getPort() {
        return this.port;
    }

    /**
     * El buffer, **sin copiar**.
     *
     * <p>Que no se copie es del contrato del JDK y es lo que hace barato reusar un paquete: quien
     * recibe escribe directo sobre este arreglo. Tambien significa que modificarlo cambia el
     * paquete, que es justamente para lo que esta.
     */
    public synchronized byte[] getData() {
        return this.buf;
    }

    /** Donde arrancan los datos dentro del buffer. */
    public synchronized int getOffset() {
        return this.offset;
    }

    /** Cuantos bytes valen. Ver la cabecera: al recibir, esto cambia. */
    public synchronized int getLength() {
        return this.length;
    }

    /**
     * Cambia el buffer y el tramo.
     *
     * @throws NullPointerException     si {@code buf} es null
     * @throws IllegalArgumentException si el tramo no entra en el buffer
     */
    public synchronized void setData(byte[] buf, int offset, int length) {
        if (buf == null) {
            throw new NullPointerException("null packet buffer");
        }
        // La suma se hace y se compara asi para que no la de vuelta un desbordamiento: con
        // `offset + length` en int, dos valores enormes dan negativo y pasarian el chequeo.
        if (offset < 0 || length < 0 || offset > buf.length - length) {
            throw new IllegalArgumentException("illegal length or offset");
        }
        this.buf = buf;
        this.offset = offset;
        this.length = length;
        this.bufLength = length;
    }

    /** A donde mandarlo; null lo deja sin destino. */
    public synchronized void setAddress(InetAddress iaddr) {
        this.address = iaddr;
    }

    /**
     * A que puerto mandarlo.
     *
     * @throws IllegalArgumentException si esta fuera de 0..65535
     */
    public synchronized void setPort(int iport) {
        if (iport < 0 || iport > 0xFFFF) {
            throw new IllegalArgumentException("Port out of range:" + iport);
        }
        this.port = iport;
    }

    /**
     * Destino y puerto de una sola vez.
     *
     * @throws IllegalArgumentException si no es una {@link InetSocketAddress}, o si es una sin
     *                                  resolver -- mandar a un nombre que nadie resolvio no es una
     *                                  operacion que se pueda completar
     */
    public synchronized void setSocketAddress(SocketAddress address) {
        if (address == null || !(address instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("unsupported address type");
        }
        InetSocketAddress addr = (InetSocketAddress) address;
        if (addr.isUnresolved()) {
            throw new IllegalArgumentException("unresolved address");
        }
        setAddress(addr.getAddress());
        setPort(addr.getPort());
    }

    /** Destino y puerto juntos. */
    public synchronized SocketAddress getSocketAddress() {
        return new InetSocketAddress(getAddress(), getPort());
    }

    /**
     * Cambia el buffer conservando el tramo, si sigue entrando.
     *
     * @throws NullPointerException     si {@code buf} es null
     * @throws IllegalArgumentException si el tramo que ya tenia no entra en el buffer nuevo
     */
    public synchronized void setData(byte[] buf) {
        if (buf == null) {
            throw new NullPointerException("null packet buffer");
        }
        this.buf = buf;
        this.offset = 0;
        this.length = buf.length;
        this.bufLength = buf.length;
    }

    /**
     * Cuantos bytes valen.
     *
     * <p>Antes de reusar un paquete para recibir hay que llamar a este metodo con el tamano del
     * buffer; ver la cabecera.
     *
     * @throws IllegalArgumentException si no entra en el buffer desde el offset actual
     */
    public synchronized void setLength(int length) {
        if (length < 0 || this.offset > this.buf.length - length) {
            throw new IllegalArgumentException("illegal length");
        }
        this.length = length;
        this.bufLength = length;
    }
}
