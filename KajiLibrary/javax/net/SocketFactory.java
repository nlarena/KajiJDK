package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * KajiLibrary's javax.net.SocketFactory -- crea sockets de cliente.
 *
 * <p>Una capa de indireccion sobre {@code new Socket(...)}, y toda su razon de ser es lo que permite:
 * que el mismo codigo cliente hable en claro o por TLS cambiando <b>solo</b> la fabrica.
 * {@code javax.net.ssl.SSLSocketFactory} es una subclase, y ahi esta el punto.
 *
 * <p>Tambien sirve para meter un proxy, un socket con instrumentacion, o uno falso para pruebas.
 *
 * <h2>{@link #createSocket()} sin argumentos</h2>
 *
 * <p>Devuelve un socket <b>sin conectar</b>. Es el unico camino para fijar opciones que tienen que
 * estar puestas <b>antes</b> de conectar --el tamano de los buferes, {@code SO_REUSEADDR}-- y por eso
 * no es abstracto: la clase base lo implementa lanzando {@link java.net.SocketException}, y una
 * fabrica que sepa hacerlo lo redefine.
 *
 * <p>Los cuatro con direccion local existen para elegir por que interfaz salir, que importa en una
 * maquina con varias.
 */
public abstract class SocketFactory {

    /** La de siempre; se crea una sola vez. */
    private static SocketFactory theFactory;

    /** Para las subclases. */
    protected SocketFactory() {
    }

    /**
     * La fabrica por omision: la que crea sockets normales, sin cifrar.
     *
     * <p>Siempre la misma instancia.
     */
    public static SocketFactory getDefault() {
        synchronized (SocketFactory.class) {
            if (theFactory == null) {
                theFactory = new DefaultSocketFactory();
            }
            return theFactory;
        }
    }

    /**
     * Un socket sin conectar. Ver la nota de la clase.
     *
     * @throws IOException si esta fabrica no sabe crear sockets sin conectar
     */
    public Socket createSocket() throws IOException {
        throw new java.net.SocketException("Unconnected sockets not implemented");
    }

    /**
     * Conecta a esa maquina y puerto.
     *
     * @throws IOException si no se pudo conectar
     * @throws UnknownHostException si el nombre no resuelve
     */
    public abstract Socket createSocket(String host, int port)
        throws IOException, UnknownHostException;

    /**
     * Idem, saliendo por esa direccion y puerto locales. Ver la nota de la clase.
     *
     * @throws IOException si no se pudo conectar
     * @throws UnknownHostException si el nombre no resuelve
     */
    public abstract Socket createSocket(String host, int port, InetAddress localHost,
                                        int localPort) throws IOException, UnknownHostException;

    /**
     * Conecta a esa direccion y puerto.
     *
     * @throws IOException si no se pudo conectar
     */
    public abstract Socket createSocket(InetAddress host, int port) throws IOException;

    /**
     * Idem, saliendo por esa direccion y puerto locales.
     *
     * @throws IOException si no se pudo conectar
     */
    public abstract Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                                        int localPort) throws IOException;
}
