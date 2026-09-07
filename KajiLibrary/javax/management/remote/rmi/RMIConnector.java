package javax.management.remote.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

/**
 * El lado del cliente: se conecta a un servidor JMX por RMI.
 *
 * <h2>Dos maneras de decirle a donde ir</h2>
 *
 * <p>Con una {@link JMXServiceURL} hay que <strong>encontrar</strong> el objeto remoto: o viene
 * codificado dentro de la propia direccion, o hay que ir a buscarlo a un directorio JNDI. Con un
 * {@link RMIServer} ya se lo tiene, y conectarse es pedirle una conexion.
 *
 * <h2>Conectarse no es abrir un socket</h2>
 *
 * <p>Es llamar a {@link RMIServer#newClient}, que autentica y devuelve la conexion propia de este
 * cliente. Recien despues {@link #getMBeanServerConnection} tiene algo que devolver: un
 * {@link MBeanServerConnection} que parece local y por debajo empaqueta cada llamada.
 *
 * <p>Todo lo que se haga antes de {@link #connect} falla con {@code IOException: Not connected}, y
 * todo lo que se haga despues de {@link #close} con {@code IOException: Connector closed}. Los dos
 * mensajes son distintos a proposito: "todavia no" y "ya no" son dos errores distintos del que
 * llama.
 *
 * <h2>Las notificaciones de la conexion</h2>
 *
 * <p>Este objeto es el mismo un emisor de notificaciones, y emite tres: abierta, cerrada y fallada.
 * Es como un cliente se entera de que se quedo sin servidor sin tener que descubrirlo en medio de
 * una llamada.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Con un {@link RMIServer} de este mismo proceso <strong>funciona entero</strong>: conecta,
 * devuelve un {@link MBeanServerConnection} que llega hasta el
 * {@link javax.management.MBeanServer}, reparte notificaciones y cierra. Es el camino que prueba
 * {@code java/RMI1.java}.
 *
 * <p>Con una {@link JMXServiceURL} no puede: encontrar el objeto remoto es el transporte, y esta VM
 * no lo tiene. En ese caso {@link #connect} tira {@link IOException} diciendolo, en vez de devolver
 * un conector que despues fallaria en la primera llamada.
 *
 * @since 1.5
 */
public class RMIConnector implements JMXConnector, Serializable {

    private static final long serialVersionUID = 817323035842634473L;

    /** Para que dos notificaciones de la misma conexion lleguen en orden. */
    private static final AtomicLong SECUENCIA = new AtomicLong();

    private final RMIServer rmiServer;
    private final JMXServiceURL jmxServiceURL;

    private transient NotificationBroadcasterSupport emisor = new NotificationBroadcasterSupport();
    private transient RMIConnection connection;
    private transient ConexionRemota remota;
    private transient String connectionId;
    private transient boolean cerrado;

    /**
     * Un conector a esa direccion.
     *
     * @param url la direccion del servidor
     * @param environment las propiedades de configuracion, o {@code null}
     * @throws IllegalArgumentException si {@code url} es {@code null}
     */
    public RMIConnector(JMXServiceURL url, Map<String, ?> environment) {
        this(null, url, environment);
    }

    /**
     * Un conector a un servidor que ya se tiene.
     *
     * @param rmiServer el servidor
     * @param environment las propiedades de configuracion, o {@code null}
     * @throws IllegalArgumentException si {@code rmiServer} es {@code null}
     */
    public RMIConnector(RMIServer rmiServer, Map<String, ?> environment) {
        this(rmiServer, null, environment);
    }

    private RMIConnector(RMIServer rmiServer, JMXServiceURL url, Map<String, ?> environment) {
        if (rmiServer == null && url == null) {
            throw new IllegalArgumentException("rmiServer and jmxServiceURL both null");
        }
        this.rmiServer = rmiServer;
        this.jmxServiceURL = url;
    }

    /**
     * La direccion del servidor, si se construyo con una.
     *
     * @return la direccion, o {@code null} si se construyo con un {@link RMIServer}
     */
    public JMXServiceURL getAddress() {
        return jmxServiceURL;
    }

    /**
     * Se conecta sin propiedades adicionales.
     *
     * @throws IOException si no se pudo conectar
     */
    public void connect() throws IOException {
        connect(null);
    }

    /**
     * Se conecta.
     *
     * <p>Sobre un conector ya conectado no hace nada: es lo que permite llamarlo desde varios lados
     * sin coordinar quien conecta primero.
     *
     * @param environment las propiedades de configuracion, o {@code null}; la credencial va en
     *     {@code jmx.remote.credentials}
     * @throws IOException si no se pudo conectar, o si el conector ya se cerro
     */
    public synchronized void connect(Map<String, ?> environment) throws IOException {
        if (cerrado) {
            throw new IOException("Connector closed");
        }
        if (connection != null) {
            return;
        }
        if (rmiServer == null) {
            throw new IOException("esta VM no tiene el transporte de RMI: no hay forma de "
                    + "encontrar el objeto remoto de " + jmxServiceURL);
        }
        final Map<String, ?> env = environment == null
                ? Collections.<String, Object>emptyMap() : new HashMap<String, Object>(environment);
        connection = rmiServer.newClient(env.get(CREDENTIALS));
        connectionId = connection.getConnectionId();
        remota = new ConexionRemota(connection);
        emisor.sendNotification(new JMXConnectionNotification(
                JMXConnectionNotification.OPENED, this, connectionId,
                SECUENCIA.getAndIncrement(), "Connection opened", null));
    }

    /**
     * El identificador que el servidor le dio a esta conexion.
     *
     * @return el identificador
     * @throws IOException si todavia no se conecto o ya se cerro
     */
    public synchronized String getConnectionId() throws IOException {
        exigirConectado();
        return connectionId;
    }

    /**
     * La conexion al {@link javax.management.MBeanServer} del servidor.
     *
     * @return la conexion
     * @throws IOException si todavia no se conecto o ya se cerro
     */
    public synchronized MBeanServerConnection getMBeanServerConnection() throws IOException {
        exigirConectado();
        return remota;
    }

    private void exigirConectado() throws IOException {
        if (cerrado) {
            throw new IOException("Connector closed");
        }
        if (connection == null) {
            throw new IOException("Not connected");
        }
    }

    /**
     * Registra un oyente de las notificaciones de la conexion.
     *
     * @param listener el oyente
     * @param filter el filtro, o {@code null}
     * @param handback lo que se le devuelve con cada notificacion, o {@code null}
     * @throws NullPointerException si {@code listener} es {@code null}
     */
    public void addConnectionNotificationListener(NotificationListener listener,
            NotificationFilter filter, Object handback) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        emisor.addNotificationListener(listener, filter, handback);
    }

    /**
     * Saca un oyente de las notificaciones de la conexion.
     *
     * @param listener el oyente
     * @throws ListenerNotFoundException si no estaba registrado
     * @throws NullPointerException si {@code listener} es {@code null}
     */
    public void removeConnectionNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        emisor.removeNotificationListener(listener);
    }

    /**
     * Saca el oyente registrado con ese filtro y ese objeto.
     *
     * @param l el oyente
     * @param f el filtro con el que se lo registro
     * @param handback el objeto con el que se lo registro
     * @throws ListenerNotFoundException si no estaba registrado asi
     * @throws NullPointerException si {@code l} es {@code null}
     */
    public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f,
            Object handback) throws ListenerNotFoundException {
        if (l == null) {
            throw new NullPointerException("listener");
        }
        emisor.removeNotificationListener(l, f, handback);
    }

    /**
     * Cierra la conexion.
     *
     * <p>Sobre uno que nunca se conecto no hace nada mas que dejarlo cerrado. Es a proposito: cerrar
     * algo que no se abrio no es un error, y obligar a comprobarlo antes solo agrega ruido en el
     * bloque {@code finally} de todo el mundo.
     *
     * @throws IOException si el servidor no pudo cerrar la conexion
     */
    public synchronized void close() throws IOException {
        if (cerrado) {
            return;
        }
        cerrado = true;
        if (remota != null) {
            remota.cerrar();
        }
        final String id = connectionId;
        try {
            if (connection != null) {
                connection.close();
            }
        } finally {
            connection = null;
            remota = null;
            if (id != null) {
                emisor.sendNotification(new JMXConnectionNotification(
                        JMXConnectionNotification.CLOSED, this, id,
                        SECUENCIA.getAndIncrement(), "Connection closed", null));
            }
        }
    }

    /**
     * Una descripcion, con la direccion o con el servidor.
     *
     * @return la descripcion
     */
    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder(getClass().getName()).append(": ");
        if (rmiServer != null) {
            b.append("rmiServer=").append(rmiServer);
        } else {
            b.append("jmxServiceURL=").append(jmxServiceURL);
        }
        return b.toString();
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Los campos transitorios no se serializan y quedarian en null: un conector deserializado
        // tiene que poder registrar oyentes antes de conectarse, igual que uno recien construido.
        emisor = new NotificationBroadcasterSupport();
    }
}
