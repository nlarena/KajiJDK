package java.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

// Una placa de red de esta maquina: su nombre, su indice, y las direcciones que tiene puestas.
//
// ===========================================================================================
// LO QUE ESTA CLASE PUEDE Y NO PUEDE HACER EN KajiJDK, Y POR QUE ESTA IGUAL
// ===========================================================================================
//
// Enumerar las placas de una maquina es una llamada al sistema operativo --`getifaddrs` en POSIX,
// `GetAdaptersAddresses` en Windows-- y esta VM no expone ni una. No hay forma de averiguar que
// placas hay, como se llaman, ni que direcciones tienen.
//
// Entonces la pregunta es que hacer con los cuatro metodos que **buscan** placas
// (`getNetworkInterfaces`, `getByName`, `getByIndex`, `getByInetAddress`). Hay tres salidas y dos
// son mentiras:
//
//  - Devolver una lista **vacia** (o null en los que buscan una sola) seria afirmar algo falso:
//    "esta maquina no tiene placas de red". No lo sabemos, y casi seguro que no es cierto.
//  - Inventar una placa de loopback --"lo", indice 1, 127.0.0.1-- seria peor: son datos plausibles
//    y falsos, que es la unica clase de dato que nadie va a ir a verificar.
//  - **Tirar `SocketException`**, que es lo que se hace.
//
// La tercera no es un rodeo: `SocketException` es una excepcion **chequeada** que estos cuatro
// metodos ya declaran en el JDK, justamente para el caso de "no se pudo averiguar". El compilador
// **obliga** a quien llama a escribir el `catch`, asi que no hay forma de que esto sorprenda en
// produccion: se ve al compilar, que es cuando conviene enterarse. Es la misma forma que ya usa
// `URL.openStream()` para un esquema que no sabe abrir.
//
// Lo mismo vale para `isUp`, `isLoopback`, `isPointToPoint`, `supportsMulticast`,
// `getHardwareAddress` y `getMTU`: todos declaran `SocketException` y todos consultan al sistema
// operativo placa por placa.
//
// Lo que **si** es real y completo son los accesores de estado --`getName`, `getIndex`,
// `getDisplayName`, `getInetAddresses`, `getInterfaceAddresses`, `getParent`, `isVirtual`,
// `equals`, `hashCode`, `toString`--: son lectura de campos y comparacion, y operan exactamente
// como en el JDK sobre cualquier instancia que exista.
//
// El efecto practico es que el tipo **se puede nombrar**, que es lo que hace falta para que
// compilen las firmas que lo mencionan --`StandardSocketOptions.IP_MULTICAST_IF`,
// `MulticastSocket.setNetworkInterface`, `InetSocketAddress` de scope id-- sin que ninguna de ellas
// prometa datos que no hay.
public final class NetworkInterface {

    private final String name;
    private final String displayName;
    private final int index;
    private final List<InetAddress> addrs;
    private final List<InterfaceAddress> bindings;
    private final NetworkInterface parent;
    private final boolean virtual;

    // Package-private, como en el JDK: las placas las fabrica la plataforma, no el usuario. Que sea
    // asi es lo que garantiza que un `NetworkInterface` que exista describa una placa que existe.
    NetworkInterface() {
        this("", -1, new InetAddress[0]);
    }

    NetworkInterface(String name, int index, InetAddress[] addrs) {
        this.name = name;
        this.displayName = name;
        this.index = index;
        List<InetAddress> l = new ArrayList<InetAddress>();
        int i = 0;
        while (addrs != null && i < addrs.length) {
            l.add(addrs[i]);
            i = i + 1;
        }
        this.addrs = Collections.unmodifiableList(l);
        this.bindings = Collections.unmodifiableList(new ArrayList<InterfaceAddress>());
        this.parent = null;
        this.virtual = false;
    }

    /** El nombre con el que el sistema operativo la conoce ("eth0", "lo"). */
    public String getName() {
        return this.name;
    }

    /**
     * El nombre para mostrarle a una persona.
     *
     * <p>En Windows suele ser una frase larga; en Unix, el mismo que {@link #getName}.
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * El indice de la placa, o -1 si el sistema no lo da.
     *
     * <p>Es el numero que identifica la placa en las APIs de IPv6 --el "scope id" de una direccion
     * link-local es este numero-- y por eso vale mas que el nombre para esos usos.
     */
    public int getIndex() {
        return this.index;
    }

    /** Las direcciones IP puestas en esta placa. */
    public Enumeration<InetAddress> getInetAddresses() {
        return Collections.enumeration(this.addrs);
    }

    /** Lo mismo que {@link #getInetAddresses}, como flujo. */
    public Stream<InetAddress> inetAddresses() {
        return this.addrs.stream();
    }

    /**
     * Las direcciones **con su mascara y su broadcast**, que es mas de lo que dice
     * {@link #getInetAddresses}.
     */
    public List<InterfaceAddress> getInterfaceAddresses() {
        return this.bindings;
    }

    /** Las sub-interfaces (alias, VLANs) que cuelgan de esta. */
    public Enumeration<NetworkInterface> getSubInterfaces() {
        return Collections.enumeration(new ArrayList<NetworkInterface>());
    }

    /** Lo mismo que {@link #getSubInterfaces}, como flujo. */
    public Stream<NetworkInterface> subInterfaces() {
        return new ArrayList<NetworkInterface>().stream();
    }

    /** La placa de la que esta es sub-interfaz, o null si es una placa fisica. */
    public NetworkInterface getParent() {
        return this.parent;
    }

    /** Si es una sub-interfaz y no una placa de verdad. */
    public boolean isVirtual() {
        return this.virtual;
    }

    // ---- lo que necesita al sistema operativo ----

    private static SocketException sinPlataforma() {
        return new SocketException(
                "esta VM no expone la enumeracion de placas de red del sistema operativo");
    }

    /**
     * Todas las placas de la maquina.
     *
     * @throws SocketException siempre en KajiJDK; ver la cabecera del archivo. Devolver una lista
     *     vacia seria afirmar que la maquina no tiene placas, que es una afirmacion falsa; esto es
     *     "no pude averiguarlo", que es la verdad y la que el contrato ya contempla.
     */
    public static Enumeration<NetworkInterface> getNetworkInterfaces() throws SocketException {
        throw sinPlataforma();
    }

    /**
     * Lo mismo que {@link #getNetworkInterfaces}, como flujo.
     *
     * @throws SocketException siempre en KajiJDK
     */
    public static Stream<NetworkInterface> networkInterfaces() throws SocketException {
        throw sinPlataforma();
    }

    /**
     * La placa que se llama {@code name}.
     *
     * @throws SocketException siempre en KajiJDK. **No** devuelve null: null significa "esa placa
     *     no existe", y eso no lo sabemos.
     * @throws NullPointerException si {@code name} es null
     */
    public static NetworkInterface getByName(String name) throws SocketException {
        if (name == null) {
            throw new NullPointerException();
        }
        throw sinPlataforma();
    }

    /**
     * La placa con ese indice.
     *
     * @throws SocketException siempre en KajiJDK
     * @throws IllegalArgumentException si el indice es negativo
     */
    public static NetworkInterface getByIndex(int index) throws SocketException {
        if (index < 0) {
            throw new IllegalArgumentException("Interface index can't be negative");
        }
        throw sinPlataforma();
    }

    /**
     * La placa que tiene puesta esa direccion.
     *
     * @throws SocketException siempre en KajiJDK
     * @throws NullPointerException si {@code addr} es null
     */
    public static NetworkInterface getByInetAddress(InetAddress addr) throws SocketException {
        if (addr == null) {
            throw new NullPointerException();
        }
        throw sinPlataforma();
    }

    /**
     * Si la placa esta levantada.
     *
     * @throws SocketException siempre en KajiJDK: el estado de una placa se consulta al sistema
     *     operativo cada vez, no es un campo del objeto
     */
    public boolean isUp() throws SocketException {
        throw sinPlataforma();
    }

    /**
     * Si es la placa de loopback.
     *
     * @throws SocketException siempre en KajiJDK
     */
    public boolean isLoopback() throws SocketException {
        throw sinPlataforma();
    }

    /**
     * Si es un enlace punto a punto (un tunel, un PPP).
     *
     * @throws SocketException siempre en KajiJDK
     */
    public boolean isPointToPoint() throws SocketException {
        throw sinPlataforma();
    }

    /**
     * Si la placa hace multicast.
     *
     * @throws SocketException siempre en KajiJDK
     */
    public boolean supportsMulticast() throws SocketException {
        throw sinPlataforma();
    }

    /**
     * La direccion fisica (MAC) de la placa.
     *
     * @throws SocketException siempre en KajiJDK
     */
    public byte[] getHardwareAddress() throws SocketException {
        throw sinPlataforma();
    }

    /**
     * El tamano maximo de paquete de la placa.
     *
     * @throws SocketException siempre en KajiJDK
     */
    public int getMTU() throws SocketException {
        throw sinPlataforma();
    }

    // ---- identidad ----

    /**
     * Mismo nombre y mismas direcciones.
     *
     * <p>El indice **no** entra en la comparacion, y eso es del JDK: el indice lo asigna el sistema
     * y puede cambiar entre arranques, asi que dos objetos que describen la misma placa podrian
     * traer indices distintos.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NetworkInterface)) {
            return false;
        }
        NetworkInterface that = (NetworkInterface) obj;
        if (this.name == null) {
            if (that.name != null) {
                return false;
            }
        } else if (!this.name.equals(that.name)) {
            return false;
        }
        return this.addrs.equals(that.addrs);
    }

    @Override
    public int hashCode() {
        return this.name == null ? 0 : this.name.hashCode();
    }

    /** {@code name:displayName (dir1 dir2 ...)}, el mismo formato que el JDK. */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("name:").append(this.name == null ? "null" : this.name);
        if (this.displayName != null) {
            b.append(" (").append(this.displayName).append(')');
        }
        return b.toString();
    }
}
