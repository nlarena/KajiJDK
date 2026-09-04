package javax.management.remote;

import javax.management.Notification;

/**
 * KajiLibrary's javax.management.remote.JMXConnectionNotification -- cambio de estado de una conexion
 * JMX.
 *
 * <p>La emiten tanto el {@link JMXConnector} del lado cliente como el {@link JMXConnectorServer} del
 * lado servidor, con los mismos cuatro tipos.
 *
 * <p>{@link #NOTIFS_LOST} es la que importa y la que se ignora. No es un error de conexion: la
 * conexion sigue viva y lo que se perdio son <b>notificaciones</b>, porque el buffer del servidor se
 * lleno antes de que el cliente las levantara. Su {@code userData} es un {@link Long} con cuantas se
 * perdieron. Ver {@link NotificationResult} sobre como se detecta.
 *
 * <p>{@link #FAILED} si es final: la conexion se corto sin que nadie la cerrara.
 */
public class JMXConnectionNotification extends Notification {

    private static final long serialVersionUID = -2331308725952627538L;

    /** Se abrio una conexion. */
    public static final String OPENED = "jmx.remote.connection.opened";

    /** Se cerro ordenadamente. */
    public static final String CLOSED = "jmx.remote.connection.closed";

    /** Se corto sola. */
    public static final String FAILED = "jmx.remote.connection.failed";

    /** Se perdieron notificaciones. Ver la nota de la clase. */
    public static final String NOTIFS_LOST = "jmx.remote.connection.notifs.lost";

    /** Cual conexion. */
    private final String connectionId;

    /**
     * @param type uno de los cuatro tipos
     * @param source quien la emite: el conector o el servidor
     * @param connectionId el identificador de la conexion
     * @param sequenceNumber el numero de secuencia de quien emite
     * @param message texto para mostrar, o null
     * @param userData el dato extra; para {@link #NOTIFS_LOST}, cuantas se perdieron
     * @throws NullPointerException si el tipo, la fuente o el identificador son null
     */
    public JMXConnectionNotification(String type, Object source, String connectionId,
                                     long sequenceNumber, String message, Object userData) {
        super(type, source, sequenceNumber, System.currentTimeMillis(), message);
        if (type == null || source == null || connectionId == null) {
            throw new NullPointerException("Illegal null argument");
        }
        this.connectionId = connectionId;
        setUserData(userData);
    }

    /** El identificador de la conexion. */
    public String getConnectionId() {
        return this.connectionId;
    }
}
