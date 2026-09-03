package java.util.logging;

import java.io.IOException;
import java.net.Socket;

/**
 * KajiLibrary's java.util.logging.SocketHandler -- manda la traza por una conexion TCP.
 *
 * <p>Es un {@link StreamHandler} sobre la salida de un socket, y casi todo su comportamiento viene
 * de ahi. Lo propio son tres cosas, y las tres tienen un motivo que conviene tener a mano.
 *
 * <h2>Vacia el buffer en CADA registro</h2>
 *
 * <p>{@link #publish} llama a `flush()` despues de cada uno, cosa que {@link StreamHandler} no hace.
 * No es prolijidad: un manejador de red existe para que alguien del otro lado vea lo que pasa
 * **mientras** pasa. Un registro que se queda en el buffer del emisor hasta que se llene no le sirve
 * a nadie, y si el programa se cae --que es cuando la traza importa-- no llega nunca.
 *
 * <h2>El destino se puede configurar, y si falta es un error</h2>
 *
 * <p>El constructor sin argumentos toma `java.util.logging.SocketHandler.host` y `.port` de la
 * configuracion. Si no estan, **tira**: un manejador de red sin destino no tiene ningun
 * comportamiento razonable por omision. Escribir a `localhost` seria adivinar, y quedarse callado
 * seria peor -- la traza se perderia sin que nadie se entere, que es justo lo que un manejador no
 * puede hacer.
 *
 * <h2>Cerrar cierra el socket</h2>
 *
 * <p>{@link #close} cierra el flujo por {@link StreamHandler} --lo que escribe la cola del
 * formateador-- y despues el socket. En ese orden: al reves, la cola no llegaria a salir.
 */
public class SocketHandler extends StreamHandler {

    private Socket socket;

    /**
     * Un manejador al host y puerto de la configuracion.
     *
     * @throws IllegalArgumentException si la configuracion no dice a donde conectarse
     * @throws IOException si no se pudo conectar
     */
    public SocketHandler() throws IOException {
        this.configurar("java.util.logging.SocketHandler");
        LogManager m = LogManager.getLogManager();
        String host = m.getStringProperty("java.util.logging.SocketHandler.host", null);
        String port = m.getStringProperty("java.util.logging.SocketHandler.port", null);
        int puerto = SocketHandler.puertoDe(port);
        if (host == null || host.length() == 0 || puerto <= 0) {
            throw new IllegalArgumentException(
                    "SocketHandler necesita java.util.logging.SocketHandler.host y .port");
        }
        this.conectar(host, puerto);
    }

    /**
     * Un manejador a ese host y puerto.
     *
     * @throws IllegalArgumentException si el puerto no es valido
     * @throws IOException si no se pudo conectar
     */
    public SocketHandler(String host, int port) throws IOException {
        this.configurar("java.util.logging.SocketHandler");
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("puerto fuera de rango: " + port);
        }
        this.conectar(host, port);
    }

    private static int puertoDe(String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            // Un puerto que no es un numero es lo mismo que no haberlo puesto: el constructor tira
            // con el mensaje que nombra las dos propiedades, que dice mas que un error de formato.
            return -1;
        }
    }

    private void conectar(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.setOutputStream(this.socket.getOutputStream());
    }

    /**
     * Escribe el registro y lo manda en el acto. Ver la nota de la clase sobre el `flush`.
     */
    public void publish(LogRecord record) {
        super.publish(record);
        this.flush();
    }

    /** Cierra el flujo y despues el socket. Ver la nota de la clase sobre el orden. */
    public synchronized void close() {
        super.close();
        if (this.socket != null) {
            try {
                this.socket.close();
            } catch (IOException e) {
                // Cerrar es lo ultimo que hace este manejador: si el socket ya estaba caido, la
                // traza igual salio o igual se perdio, y tirar aca no cambiaria ninguna de las dos.
                this.reportError(null, e, ErrorManager.CLOSE_FAILURE);
            }
            this.socket = null;
        }
    }
}
