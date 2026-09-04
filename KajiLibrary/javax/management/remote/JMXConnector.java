package javax.management.remote;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.security.auth.Subject;

/**
 * KajiLibrary's javax.management.remote.JMXConnector -- el lado cliente de una conexion JMX.
 *
 * <p>Se consigue con {@link JMXConnectorFactory} y se usa asi: {@link #connect}, despues
 * {@link #getMBeanServerConnection} para operar, y {@link #close} al final. Es {@link Closeable}, asi
 * que sirve en un {@code try} con recursos.
 *
 * <h2>Crear y conectar son dos pasos</h2>
 *
 * <p>{@code JMXConnectorFactory.newJMXConnector} devuelve un conector <b>sin conectar</b>. Eso permite
 * registrar los escuchas de conexion antes de que pase nada, que es la unica forma de no perderse el
 * {@code OPENED}. {@code JMXConnectorFactory.connect} hace los dos pasos de una y es lo que se usa
 * cuando eso no importa.
 *
 * <h2>{@link #getMBeanServerConnection(Subject)} esta marcado</h2>
 *
 * <p>La version con {@code Subject} sirve para actuar en nombre de otro; su default lanza
 * {@link UnsupportedOperationException}. Depende del mecanismo de delegacion, que quedo obsoleto junto
 * con {@link SubjectDelegationPermission}.
 *
 * <h2>El identificador de conexion</h2>
 *
 * <p>{@link #getConnectionId} es unico y <b>cambia si la conexion se reabre</b>. Comparar el que se ve
 * ahora contra el que se vio antes es como se detecta que hubo una reconexion en el medio y que el
 * estado del servidor pudo cambiar.
 */
public interface JMXConnector extends Closeable {

    /** La clave del entorno donde van las credenciales. */
    String CREDENTIALS = "jmx.remote.credentials";

    /**
     * Conecta con el entorno que se dio al crearlo.
     *
     * @throws IOException si no se pudo
     * @throws SecurityException si no lo dejaron
     */
    void connect() throws IOException;

    /**
     * Conecta con este entorno, que se suma al de creacion.
     *
     * @throws IOException si no se pudo
     * @throws SecurityException si no lo dejaron
     */
    void connect(Map<String, ?> env) throws IOException;

    /**
     * Por donde se opera sobre el servidor remoto.
     *
     * @throws IOException si no esta conectado
     */
    MBeanServerConnection getMBeanServerConnection() throws IOException;

    /**
     * Idem, actuando en nombre de otro. Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException por omision
     */
    default MBeanServerConnection getMBeanServerConnection(Subject delegationSubject)
        throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Cierra. Se puede llamar mas de una vez.
     *
     * @throws IOException si algo fallo al cerrar
     */
    void close() throws IOException;

    /**
     * Registra un escucha de estado de la conexion.
     *
     * <p>Se puede antes de conectar, y hay que hacerlo asi para no perderse el {@code OPENED}.
     */
    void addConnectionNotificationListener(NotificationListener listener,
                                           NotificationFilter filter, Object handback);

    /**
     * Lo da de baja, en todas sus combinaciones de filtro y dato.
     *
     * @throws javax.management.ListenerNotFoundException si no estaba
     */
    void removeConnectionNotificationListener(NotificationListener listener)
        throws javax.management.ListenerNotFoundException;

    /**
     * Da de baja esa combinacion exacta.
     *
     * @throws javax.management.ListenerNotFoundException si no estaba
     */
    void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f,
                                              Object handback)
        throws javax.management.ListenerNotFoundException;

    /**
     * El identificador de esta conexion. Ver la nota de la clase.
     *
     * @throws IOException si no esta conectado
     */
    String getConnectionId() throws IOException;
}
