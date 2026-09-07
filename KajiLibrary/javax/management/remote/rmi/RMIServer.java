package javax.management.remote.rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * El objeto remoto al que un cliente se conecta primero.
 *
 * <h2>Dos metodos, y los dos hacen falta</h2>
 *
 * <p>{@link #getVersion} se llama <strong>antes</strong> de autenticarse, y es lo que permite que
 * cliente y servidor de versiones distintas se entiendan o se rechacen con un mensaje claro en vez
 * de fallar en el medio de la primera operacion.
 *
 * <p>{@link #newClient} es el que autentica y devuelve la conexion. Que la credencial sea un
 * {@code Object} y no algo tipado es a proposito: puede ser un arreglo de dos {@code String}, un
 * objeto de un mecanismo propio, o {@code null} si el servidor no pide nada.
 *
 * <h2>Por que hay dos objetos y no uno</h2>
 *
 * <p>Este es unico y compartido; el {@link RMIConnection} que devuelve es <strong>uno por
 * cliente</strong>. Esa separacion es la que permite que cada cliente tenga su propia identidad, su
 * propio cargador de clases y su propia cola de notificaciones.
 *
 * @since 1.5
 */
public interface RMIServer extends Remote {

    /**
     * La version del protocolo y de la implementacion.
     *
     * <p>El formato es {@code "<version de la especificacion> <nombre del proveedor>"}.
     *
     * @return la version
     * @throws RemoteException si no se pudo hablar con el servidor
     */
    String getVersion() throws RemoteException;

    /**
     * Autentica al cliente y le abre una conexion propia.
     *
     * @param credentials la credencial, o {@code null} si el servidor no pide ninguna
     * @return la conexion de este cliente
     * @throws java.rmi.RemoteException si no se pudo hablar con el servidor
     * @throws java.lang.SecurityException si la credencial no sirve
     * @throws IOException si no se pudo abrir la conexion
     */
    RMIConnection newClient(Object credentials) throws IOException;
}
