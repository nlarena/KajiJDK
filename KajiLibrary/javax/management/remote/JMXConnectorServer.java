package javax.management.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * KajiLibrary's javax.management.remote.JMXConnectorServer -- la base de los servidores de conectores.
 *
 * <p>Junta tres papeles, y la lista de interfaces lo dice: es un {@link JMXConnectorServerMBean} para
 * poder registrarse, un {@link MBeanRegistration} para enterarse de cuando lo registran, y un
 * {@link NotificationBroadcasterSupport} para avisar de las conexiones.
 *
 * <h2>El servidor de MBeans puede venir de dos lados</h2>
 *
 * <p>Por el constructor, o por el registro: si se construye sin uno y despues se registra como MBean,
 * {@link #preRegister} toma el servidor donde lo registraron. Es lo que permite escribir en una
 * configuracion "registra este conector" sin nombrar el servidor.
 *
 * <h2>{@link #setMBeanServerForwarder} apila al reves</h2>
 *
 * <p>Cada llamada pone el nuevo interceptor <b>delante</b> de lo que ya habia, asi que el ultimo
 * agregado es el primero en ver las llamadas. Ver {@link MBeanServerForwarder}.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta clase esta entera: lo que le falta a esta biblioteca es un <b>protocolo</b>, y eso vive en
 * las subclases que trae un proveedor. Ver {@link JMXConnectorServerFactory}.
 */
public abstract class JMXConnectorServer extends NotificationBroadcasterSupport
    implements JMXConnectorServerMBean, MBeanRegistration, JMXAddressable {

    /** La clave del entorno donde va el {@link JMXAuthenticator}. */
    public static final String AUTHENTICATOR = "jmx.remote.authenticator";

    /** A que servidor de MBeans expone. */
    private MBeanServer mbeanServer = null;

    /** Con que nombre se registro, o null. */
    private ObjectName myName;

    /** Las conexiones abiertas. */
    private final ArrayList<String> connectionIds = new ArrayList<String>();

    /** El proximo numero de secuencia de las notificaciones. */
    private long sequenceNumber = 0;

    /** Sin servidor de MBeans; se toma al registrarlo. */
    public JMXConnectorServer() {
        this(null);
    }

    /** @param mbeanServer a que servidor expone, o null */
    public JMXConnectorServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    /** A que servidor de MBeans expone. */
    public synchronized MBeanServer getMBeanServer() {
        return this.mbeanServer;
    }

    /**
     * Encadena un interceptor delante. Ver la nota de la clase sobre el orden.
     *
     * @throws IllegalArgumentException si es null
     */
    public synchronized void setMBeanServerForwarder(MBeanServerForwarder mbsf) {
        if (mbsf == null) {
            throw new IllegalArgumentException("Invalid null argument: mbsf");
        }
        if (this.mbeanServer != null) {
            mbsf.setMBeanServer(this.mbeanServer);
        }
        this.mbeanServer = mbsf;
    }

    /** Los identificadores de las conexiones abiertas. */
    public String[] getConnectionIds() {
        synchronized (this.connectionIds) {
            return this.connectionIds.toArray(new String[this.connectionIds.size()]);
        }
    }

    /**
     * Un conector cliente hacia este servidor.
     *
     * <p>Va por {@link JMXConnectorFactory} con la direccion propia, sin atajos: eso es lo que hace
     * que sirva para probar el camino remoto de verdad.
     *
     * @throws IllegalStateException si no esta activo
     * @throws IOException si no se pudo
     */
    public JMXConnector toJMXConnector(Map<String, ?> env) throws IOException {
        if (!isActive()) {
            throw new IllegalStateException("Connector server is not active");
        }
        JMXServiceURL address = getAddress();
        if (address == null) {
            throw new UnsupportedOperationException(
                "This connector server does not support connections from a JMXConnector");
        }
        return JMXConnectorFactory.newJMXConnector(address, env);
    }

    /** Las tres notificaciones de conexion que emite esta clase. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        final String[] types = {
            JMXConnectionNotification.OPENED,
            JMXConnectionNotification.CLOSED,
            JMXConnectionNotification.FAILED,
        };
        final String className = JMXConnectionNotification.class.getName();
        final String description = "A client connection has been opened or closed";
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, className, description),
        };
    }

    /** Para que la subclase avise que se abrio una conexion. */
    protected void connectionOpened(String connectionId, String message, Object userData) {
        synchronized (this.connectionIds) {
            this.connectionIds.add(connectionId);
        }
        sendNotification(JMXConnectionNotification.OPENED, connectionId, message, userData);
    }

    /** Idem, cerrada ordenadamente. */
    protected void connectionClosed(String connectionId, String message, Object userData) {
        synchronized (this.connectionIds) {
            this.connectionIds.remove(connectionId);
        }
        sendNotification(JMXConnectionNotification.CLOSED, connectionId, message, userData);
    }

    /** Idem, cortada sola. */
    protected void connectionFailed(String connectionId, String message, Object userData) {
        synchronized (this.connectionIds) {
            this.connectionIds.remove(connectionId);
        }
        sendNotification(JMXConnectionNotification.FAILED, connectionId, message, userData);
    }

    /**
     * Toma el servidor donde lo registran, si no tenia uno.
     *
     * <p>Ver la nota de la clase. Solo lo toma la primera vez: registrarlo dos veces no lo mueve.
     *
     * @throws NullPointerException si el servidor o el nombre son null
     */
    public synchronized ObjectName preRegister(MBeanServer mbs, ObjectName name) {
        if (mbs == null || name == null) {
            throw new NullPointerException("Null MBeanServer or ObjectName");
        }
        if (this.mbeanServer == null) {
            this.mbeanServer = mbs;
            this.myName = name;
        }
        return name;
    }

    /** No hace nada. */
    public void postRegister(Boolean registrationDone) {
    }

    /**
     * Lo para antes de sacarlo del registro.
     *
     * <p>Es lo que evita dejar un puerto escuchando despues de desregistrar el MBean.
     *
     * @throws IOException si fallo al parar
     */
    public synchronized void preDeregister() throws Exception {
        if (this.myName != null && isActive()) {
            stop();
            this.myName = null;
        }
    }

    /** Se olvida del nombre. */
    public void postDeregister() {
        this.myName = null;
    }

    /** El armado comun de las tres notificaciones de conexion. */
    private void sendNotification(String type, String connectionId, String message,
                                  Object userData) {
        long seq;
        synchronized (this) {
            seq = this.sequenceNumber;
            this.sequenceNumber = this.sequenceNumber + 1;
        }
        sendNotification(new JMXConnectionNotification(type, getNotificationSource(), connectionId,
                                                       seq, message, userData));
    }

    /**
     * Quien figura como fuente de las notificaciones.
     *
     * <p>El nombre con el que se registro si lo hay, y si no el objeto. Poner el nombre es lo correcto
     * cuando las notificaciones cruzan la red: el objeto no viaja, el nombre si.
     */
    private Object getNotificationSource() {
        if (this.myName != null) {
            return this.myName;
        }
        return this;
    }
}
