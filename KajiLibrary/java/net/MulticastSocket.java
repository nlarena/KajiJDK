package java.net;

import java.io.IOException;

// Un `DatagramSocket` con las opciones de multicast encima.
//
// ===========================================================================================
// LA MISMA LINEA QUE `DatagramSocket`, CORRIDA UN LUGAR
// ===========================================================================================
//
// Todo lo que esta clase agrega sobre su padre son **opciones del socket**: el TTL de los
// datagramas multicast, por que placa salen, y si se reciben los propios. Las tres son estado
// local, se fijan antes de mandar nada, y entran completas.
//
// **YA ENTRAN LOS CUATRO DE MEMBRESIA.** Esta cabecera decia que entrar a un grupo multicast --que
// es mandarle un IGMP al router y quedar anotado en una tabla que vive afuera de este proceso-- no
// se podia cumplir sin red, y era cierto mientras la VM no tuviera UDP. Ahora lo tiene, y los
// cuatro anotan de verdad: si el sistema rechaza la membresia, sale una `IOException` en vez de un
// silencio que dejaria al programa esperando datagramas que no van a llegar nunca.
//
// El trabajo esta en `DatagramSocket`, que es donde vive el socket. Los dos de aca que toman una
// `InetAddress` a secas son los mismos con la placa que este socket tenga configurada -- que es
// exactamente lo que el JDK documenta que hacen.
public class MulticastSocket extends DatagramSocket {

    private int timeToLive = 1;
    private InetAddress interfaz;
    private NetworkInterface placa;
    private boolean loopbackDeshabilitado = false;

    /**
     * Un socket multicast atado a un puerto cualquiera.
     *
     * @throws IOException siempre en KajiJDK; ver la cabecera. Para un socket sin atar y
     *     configurable, {@code new MulticastSocket(null)}.
     */
    public MulticastSocket() throws IOException {
        super(new InetSocketAddress(0));
    }

    /**
     * Un socket multicast atado a {@code port}.
     *
     * @throws IOException si no se pudo atar
     */
    public MulticastSocket(int port) throws IOException {
        super(new InetSocketAddress(port));
    }

    /**
     * Un socket multicast atado a {@code bindaddr}, o **sin atar** si es null.
     *
     * <p>El caso de null anda entero: es el socket sobre el que se pueden fijar y leer todas las
     * opciones de esta clase.
     *
     * @throws IOException si no se pudo atar
     */
    public MulticastSocket(SocketAddress bindaddr) throws IOException {
        super(bindaddr);
    }

    /**
     * Cuantos saltos viven los datagramas multicast que salgan de aca.
     *
     * @throws IllegalArgumentException si no entra en 0..255
     */
    public void setTimeToLive(int ttl) throws IOException {
        if (ttl < 0 || ttl > 255) {
            throw new IllegalArgumentException("ttl out of range");
        }
        this.chequearAbiertoIO();
        this.timeToLive = ttl;
    }

    /** El TTL de los datagramas multicast. Arranca en 1, que es el default de la plataforma. */
    public int getTimeToLive() throws IOException {
        this.chequearAbiertoIO();
        return this.timeToLive;
    }

    /**
     * El TTL, en un byte.
     *
     * @deprecated el TTL va de 0 a 255 y un `byte` de Java tiene signo, asi que 128 en adelante se
     *     escriben negativos. Usar {@link #setTimeToLive(int)}.
     */
    @Deprecated
    public void setTTL(byte ttl) throws IOException {
        this.setTimeToLive(ttl & 0xFF);
    }

    /**
     * El TTL, en un byte.
     *
     * @deprecated ver {@link #setTTL(byte)}.
     */
    @Deprecated
    public byte getTTL() throws IOException {
        return (byte) this.getTimeToLive();
    }

    /**
     * Por que direccion local salen los datagramas multicast.
     *
     * @throws SocketException si el socket esta cerrado
     */
    public void setInterface(InetAddress inf) throws SocketException {
        this.chequearAbiertoSock();
        if (inf == null) {
            throw new SocketException("Invalid value");
        }
        this.interfaz = inf;
    }

    /**
     * La direccion local por la que salen los datagramas multicast.
     *
     * <p>Si no se fijo ninguna, la direccion comodin -- que es lo que devuelve el JDK cuando el
     * sistema no eligio placa todavia.
     */
    public InetAddress getInterface() throws SocketException {
        this.chequearAbiertoSock();
        if (this.interfaz != null) {
            return this.interfaz;
        }
        try {
            return InetAddress.getByAddress(new byte[] {0, 0, 0, 0});
        } catch (UnknownHostException e) {
            throw new SocketException("no interface");
        }
    }

    /**
     * Por que placa salen los datagramas multicast.
     *
     * <p>Es la version buena de {@link #setInterface}: una placa puede tener varias direcciones, y
     * en IPv6 la placa es la unica forma de nombrar el enlace.
     *
     * @throws SocketException si el socket esta cerrado
     */
    public void setNetworkInterface(NetworkInterface netIf) throws SocketException {
        this.chequearAbiertoSock();
        if (netIf == null) {
            throw new SocketException("Invalid value");
        }
        this.placa = netIf;
    }

    /**
     * La placa fijada con {@link #setNetworkInterface}.
     *
     * @throws SocketException si no se fijo ninguna. El JDK devuelve ahi una placa marcador que
     *     representa "la que elija el sistema"; aca no hay sistema que elija, y fabricar una placa
     *     de mentira para devolverla seria inventar un dato. La excepcion es chequeada y el
     *     contrato ya la declara.
     */
    public NetworkInterface getNetworkInterface() throws SocketException {
        this.chequearAbiertoSock();
        if (this.placa == null) {
            throw new SocketException("There is no multicast interface set");
        }
        return this.placa;
    }

    /**
     * Si se DESHABILITA la recepcion local de los propios datagramas multicast.
     *
     * <p>Ojo con el sentido, que esta invertido y es del JDK: {@code true} significa "no me los
     * mandes de vuelta". Esa inversion es justamente por lo que el metodo quedo deprecado.
     *
     * @deprecated usar {@code setOption(StandardSocketOptions.IP_MULTICAST_LOOP, ...)}, que se lee
     *     al derecho.
     */
    @Deprecated
    public void setLoopbackMode(boolean disable) throws SocketException {
        this.chequearAbiertoSock();
        this.loopbackDeshabilitado = disable;
    }

    /**
     * Si la recepcion local esta deshabilitada.
     *
     * @deprecated ver {@link #setLoopbackMode}.
     */
    @Deprecated
    public boolean getLoopbackMode() throws SocketException {
        this.chequearAbiertoSock();
        return this.loopbackDeshabilitado;
    }

    private void chequearAbiertoSock() throws SocketException {
        if (this.isClosed()) {
            throw new SocketException("Socket is closed");
        }
    }

    private void chequearAbiertoIO() throws IOException {
        if (this.isClosed()) {
            throw new SocketException("Socket is closed");
        }
    }

    /**
     * Entra al grupo multicast {@code mcastaddr} por la placa configurada en este socket.
     *
     * <p>Es {@link DatagramSocket#joinGroup(SocketAddress, NetworkInterface)} con la placa que se
     * haya fijado con {@link #setNetworkInterface} --o la que elija el sistema si no se fijo
     * ninguna--, que es lo que el JDK documenta.
     *
     * @throws IOException si no se pudo entrar al grupo
     * @throws IllegalArgumentException si la direccion no es multicast
     * @deprecated como en el JDK: usar {@link DatagramSocket#joinGroup(SocketAddress,
     *     NetworkInterface)}, que dice por que placa
     */
    @Deprecated
    public void joinGroup(InetAddress mcastaddr) throws IOException {
        this.joinGroup(new InetSocketAddress(mcastaddr, 0), this.placa);
    }

    /**
     * Sale del grupo multicast {@code mcastaddr}. Ver {@link #joinGroup(InetAddress)}.
     *
     * @throws IOException si no se pudo salir del grupo
     * @deprecated como en el JDK
     */
    @Deprecated
    public void leaveGroup(InetAddress mcastaddr) throws IOException {
        this.leaveGroup(new InetSocketAddress(mcastaddr, 0), this.placa);
    }

    /**
     * Manda ese datagrama con ese TTL, sin cambiar el TTL del socket.
     *
     * <p>El TTL se pone antes de mandar y se restaura despues, que es lo que hace el JDK: el
     * contrato dice que el TTL del socket queda como estaba.
     *
     * @throws IOException si el datagrama no se pudo mandar
     * @deprecated como en el JDK: usar {@link #setTimeToLive} y {@link DatagramSocket#send}
     */
    @Deprecated
    public void send(DatagramPacket p, byte ttl) throws IOException {
        this.chequearAbiertoIO();
        int antes = this.timeToLive;
        // El `& 0xFF` no es cosmetico: el parametro es un `byte` con signo y un TTL de 200 llega
        // como -56. El JDK lo trata como sin signo, y sin esto un TTL alto seria negativo.
        this.setTimeToLive(ttl & 0xFF);
        try {
            this.send(p);
        } finally {
            this.setTimeToLive(antes);
        }
    }
}
