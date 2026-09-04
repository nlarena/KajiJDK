package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * KajiLibrary's javax.net.ServerSocketFactory -- crea sockets de servidor.
 *
 * <p>El espejo de {@link SocketFactory} para el lado que escucha, con la misma razon de ser:
 * {@code javax.net.ssl.SSLServerSocketFactory} es una subclase, y cambiarla es todo lo que hace falta
 * para que un servidor pase a hablar TLS.
 *
 * <h2>Los tres argumentos</h2>
 *
 * <p>El puerto, la <b>cola de espera</b>, y la direccion local:
 *
 * <ul>
 *   <li>puerto 0 significa "el que el sistema quiera", y despues se pregunta cual toco;
 *   <li>la cola es cuantas conexiones pueden quedar esperando a que alguien las acepte. Chica, los
 *       clientes ven la conexion rechazada bajo un pico;
 *   <li>la direccion local decide <b>por que interfaz</b> se escucha. Sin ella se escucha en todas,
 *       que en una maquina con una pata en internet no siempre es lo que se quiere.
 * </ul>
 *
 * <p>{@link #createServerSocket()} devuelve uno sin atar, para poder fijar opciones antes; ver
 * {@link SocketFactory#createSocket()}.
 */
public abstract class ServerSocketFactory {

    /** La de siempre; se crea una sola vez. */
    private static ServerSocketFactory theFactory;

    /** Para las subclases. */
    protected ServerSocketFactory() {
    }

    /** La fabrica por omision: sockets de servidor normales, sin cifrar. Siempre la misma. */
    public static ServerSocketFactory getDefault() {
        synchronized (ServerSocketFactory.class) {
            if (theFactory == null) {
                theFactory = new DefaultServerSocketFactory();
            }
            return theFactory;
        }
    }

    /**
     * Un socket de servidor sin atar. Ver la nota de la clase.
     *
     * @throws IOException si esta fabrica no sabe crearlos sin atar
     */
    public ServerSocket createServerSocket() throws IOException {
        throw new java.net.SocketException("Unbound server sockets not implemented");
    }

    /**
     * Escucha en ese puerto; 0 deja que el sistema elija.
     *
     * @throws IOException si no se pudo abrir
     */
    public abstract ServerSocket createServerSocket(int port) throws IOException;

    /**
     * Idem, con esa cola de espera. Ver la nota de la clase.
     *
     * @throws IOException si no se pudo abrir
     */
    public abstract ServerSocket createServerSocket(int port, int backlog) throws IOException;

    /**
     * Idem, escuchando solo por esa interfaz. Ver la nota de la clase.
     *
     * @throws IOException si no se pudo abrir
     */
    public abstract ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress)
        throws IOException;
}
