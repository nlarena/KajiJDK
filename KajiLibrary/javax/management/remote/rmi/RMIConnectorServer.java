package javax.management.remote.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerForwarder;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;

/**
 * El servidor JMX que se publica por RMI.
 *
 * <h2>Que hace</h2>
 *
 * <p>Es el objeto que se registra como MBean y que se arranca y se para. Adentro tiene un
 * {@link RMIServerImpl} --el que de verdad atiende-- y una direccion; su trabajo es el ciclo de vida
 * y la publicacion, no las llamadas.
 *
 * <h2>Arrancar es publicar</h2>
 *
 * <p>{@link #start} exporta el {@link RMIServerImpl} y, si la direccion lo pide, lo anota en un
 * directorio JNDI para que el cliente lo encuentre por nombre. Recien ahi
 * {@link #getAddress} devuelve una direccion completa, con el stub adentro o con el nombre en el
 * directorio.
 *
 * <p>Un servidor parado no se puede volver a arrancar. Es a proposito: el estado que se libero al
 * parar --el puerto, el registro-- no se reconstruye, y dejar que se "reanude" a medias seria peor
 * que obligar a crear otro.
 *
 * <h2>{@link #getAttributes} no devuelve el entorno</h2>
 *
 * <p>Devuelve el entorno <strong>menos lo que no se puede mostrar</strong>: el autenticador, las
 * fabricas de sockets, los archivos de claves, las credenciales de JNDI. La lista esta en
 * {@code OCULTOS} y se puede cambiar con {@code jmx.remote.x.hidden.attributes}.
 *
 * <p>Tampoco devuelve lo que no es serializable. Esa regla no es de seguridad sino practica: estos
 * atributos se leen a traves de la propia conexion JMX, asi que tienen que poder viajar.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Todo funciona menos {@link #start}, que tiene que exportar por RMI. El ciclo de vida, los
 * atributos, la direccion, el reenviador de MBeanServer y las notificaciones de conexion son reales
 * y se comportan como en el JDK: un servidor recien creado no esta activo, {@link #getAddress}
 * devuelve la direccion con la que se lo construyo, y {@link #stop} sobre uno que nunca arranco no
 * hace nada.
 *
 * @since 1.5
 */
public class RMIConnectorServer extends JMXConnectorServer {

    /**
     * Si al anotarse en el directorio JNDI se puede pisar una anotacion anterior.
     *
     * <p>El valor es el texto {@code "true"} o {@code "false"}. Pisar es comodo al desarrollar y
     * peligroso en produccion: dos servidores con la misma direccion y el segundo gana en silencio.
     */
    public static final String JNDI_REBIND_ATTRIBUTE = "jmx.remote.jndi.rebind";

    /** La fabrica de sockets del cliente; viaja dentro del stub. */
    public static final String RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE =
            "jmx.remote.rmi.client.socket.factory";

    /** La fabrica de sockets del servidor; se queda de este lado. */
    public static final String RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE =
            "jmx.remote.rmi.server.socket.factory";

    /**
     * El filtro de deserializacion que se le aplica a la credencial que manda el cliente.
     *
     * <p>Es lo primero que llega de afuera y todavia no se autentico nadie: sin un filtro, un
     * cliente cualquiera hace que el servidor construya el grafo de objetos que se le ocurra
     * <strong>antes</strong> de que se compruebe si tiene permiso.
     *
     * @since 10
     */
    public static final String CREDENTIALS_FILTER_PATTERN =
            "jmx.remote.rmi.server.credentials.filter.pattern";

    /**
     * El filtro de deserializacion de todo lo demas que llega por la conexion.
     *
     * @since 10
     */
    public static final String SERIAL_FILTER_PATTERN = "jmx.remote.rmi.server.serial.filter.pattern";

    /** La propiedad con la que se cambia la lista de atributos que no se muestran. */
    static final String ATRIBUTOS_OCULTOS = "jmx.remote.x.hidden.attributes";

    /**
     * Los atributos que {@link #getAttributes} no muestra.
     *
     * <p>Una entrada que termina en punto es un prefijo y tapa todo lo que empiece asi; las demas
     * son nombres exactos. Es la lista del JDK y no una eleccion: cambiarla haria que un servidor
     * de esta biblioteca expusiera por la red algo que el mismo servidor en el JDK no expone.
     */
    static final String[] OCULTOS = {
        "java.naming.security.",
        "jmx.remote.authenticator",
        "jmx.remote.context",
        "jmx.remote.default.class.loader",
        "jmx.remote.message.connection.server",
        "jmx.remote.object.wrapping",
        "jmx.remote.rmi.client.socket.factory",
        "jmx.remote.rmi.server.socket.factory",
        "jmx.remote.sasl.callback.handler",
        "jmx.remote.tls.socket.factory",
        "jmx.remote.x.access.file",
        "jmx.remote.x.password.file",
    };

    private final Map<String, ?> env;
    private final Map<String, Object> atributos;

    private final JMXServiceURL urlPedida;

    private RMIServerImpl rmiServerImpl;
    private JMXServiceURL direccion;
    private boolean arrancado;
    private boolean parado;

    /**
     * Un servidor en esa direccion.
     *
     * @param url la direccion, o {@code null} para una en el puerto que elija el sistema
     * @param environment las propiedades de configuracion, o {@code null}
     * @throws IOException si no se pudo crear
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String, ?> environment) throws IOException {
        this(url, environment, (RMIServerImpl) null, null);
    }

    /**
     * Un servidor en esa direccion, sobre ese {@link MBeanServer}.
     *
     * @param url la direccion, o {@code null}
     * @param environment las propiedades de configuracion, o {@code null}
     * @param mbeanServer el servidor de MBeans, o {@code null} para el que lo registre
     * @throws IOException si no se pudo crear
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String, ?> environment,
            MBeanServer mbeanServer) throws IOException {
        this(url, environment, (RMIServerImpl) null, mbeanServer);
    }

    /**
     * Un servidor con un {@link RMIServerImpl} ya construido.
     *
     * <p>Es la forma que permite poner otro transporte o una subclase propia. Con {@code null} lo
     * fabrica {@link #start} a partir de la direccion.
     *
     * @param url la direccion, o {@code null}
     * @param environment las propiedades de configuracion, o {@code null}
     * @param rmiServerImpl el servidor que atiende, o {@code null}
     * @param mbeanServer el servidor de MBeans, o {@code null}
     * @throws IOException si no se pudo crear
     * @throws java.net.MalformedURLException si la direccion no es de protocolo
     *     {@code rmi} ni {@code iiop}
     */
    public RMIConnectorServer(JMXServiceURL url, Map<String, ?> environment,
            RMIServerImpl rmiServerImpl, MBeanServer mbeanServer) throws IOException {
        super(mbeanServer);
        if (url != null) {
            final String p = url.getProtocol();
            if (!"rmi".equalsIgnoreCase(p) && !"iiop".equalsIgnoreCase(p)) {
                throw new MalformedURLException("Invalid protocol type: " + p);
            }
        }
        this.urlPedida = url;
        this.rmiServerImpl = rmiServerImpl;
        this.env = environment == null
                ? Collections.<String, Object>emptyMap() : new HashMap<String, Object>(environment);
        this.atributos = Collections.unmodifiableMap(filtrar(this.env));
    }

    /**
     * El entorno, sin lo que no se puede mostrar y sin lo que no puede viajar.
     *
     * @return los atributos, en un mapa que no se puede modificar
     */
    public Map<String, ?> getAttributes() {
        return atributos;
    }

    /**
     * La direccion en la que se lo puede alcanzar.
     *
     * <p>Es {@code null} hasta que se lo arranca, aunque se lo haya construido con una direccion.
     * No es un descuido: la direccion con la que se construye es un pedido --que puerto, que
     * nombre-- y la de verdad recien se sabe cuando se exporto, porque puede tener el puerto que le
     * asigno el sistema y el stub codificado adentro. Devolver la pedida seria dar por buena una
     * direccion a la que todavia no atiende nadie.
     *
     * @return la direccion, o {@code null} si todavia no arranco
     */
    public JMXServiceURL getAddress() {
        return direccion;
    }

    /**
     * Si esta arrancado y todavia no se paro.
     *
     * @return cierto si atiende
     */
    public synchronized boolean isActive() {
        return arrancado && !parado;
    }

    /**
     * Arranca el servidor: exporta el {@link RMIServerImpl} y lo publica.
     *
     * @throws IOException si no se pudo arrancar, o si ya se lo habia parado
     * @throws UnsupportedOperationException en esta VM, que no tiene el transporte de RMI
     */
    public synchronized void start() throws IOException {
        if (parado) {
            throw new IOException("The server has been stopped.");
        }
        if (arrancado) {
            return;
        }
        if (getMBeanServer() == null) {
            throw new IllegalStateException("This connector server is not attached to an "
                    + "MBean server");
        }
        if (rmiServerImpl == null) {
            rmiServerImpl = new RMIJRMPServerImpl(0, null, null, env);
        }
        rmiServerImpl.setMBeanServer(getMBeanServer());
        rmiServerImpl.export();
        direccion = urlPedida;
        arrancado = true;
    }

    /**
     * Para el servidor y cierra las conexiones que haya.
     *
     * <p>Sobre uno que nunca arranco no hace nada mas que dejarlo marcado como parado, para que no
     * se pueda arrancar despues.
     *
     * @throws IOException si algo no se pudo cerrar
     */
    public void stop() throws IOException {
        final RMIServerImpl s;
        synchronized (this) {
            if (parado) {
                return;
            }
            parado = true;
            s = rmiServerImpl;
        }
        if (s != null) {
            s.close();
        }
    }

    /**
     * Un cliente ya conectado a este servidor.
     *
     * @param environment las propiedades de configuracion del cliente, o {@code null}
     * @return el conector
     * @throws IOException si no se pudo crear
     * @throws IllegalStateException si el servidor no esta activo
     */
    public JMXConnector toJMXConnector(Map<String, ?> environment) throws IOException {
        synchronized (this) {
            if (!isActive()) {
                throw new IllegalStateException("Connector is not active");
            }
        }
        return super.toJMXConnector(environment);
    }

    /**
     * Pone un reenviador delante del {@link MBeanServer}.
     *
     * <p>Es como se interponen los filtros de autorizacion: el reenviador ve cada llamada antes que
     * el servidor de MBeans. Si el servidor ya arranco, hay que decirselo tambien al
     * {@link RMIServerImpl}, porque las conexiones vivas piden el MBeanServer a traves de el.
     *
     * @param mbsf el reenviador
     */
    public synchronized void setMBeanServerForwarder(MBeanServerForwarder mbsf) {
        super.setMBeanServerForwarder(mbsf);
        if (rmiServerImpl != null) {
            rmiServerImpl.setMBeanServer(getMBeanServer());
        }
    }

    /**
     * Avisa que se abrio una conexion.
     *
     * @param connectionId el identificador de la conexion
     * @param message el mensaje
     * @param userData lo que se quiera adjuntar, o {@code null}
     */
    @Override
    protected void connectionOpened(String connectionId, String message, Object userData) {
        super.connectionOpened(connectionId, message, userData);
    }

    /**
     * Avisa que se cerro una conexion.
     *
     * @param connectionId el identificador de la conexion
     * @param message el mensaje
     * @param userData lo que se quiera adjuntar, o {@code null}
     */
    @Override
    protected void connectionClosed(String connectionId, String message, Object userData) {
        super.connectionClosed(connectionId, message, userData);
    }

    /**
     * Avisa que una conexion fallo.
     *
     * @param connectionId el identificador de la conexion
     * @param message el mensaje
     * @param userData lo que se quiera adjuntar, o {@code null}
     */
    @Override
    protected void connectionFailed(String connectionId, String message, Object userData) {
        super.connectionFailed(connectionId, message, userData);
    }

    /** El entorno sin lo oculto y sin lo que no es serializable. */
    private static Map<String, Object> filtrar(Map<String, ?> env) {
        final String[] ocultos = ocultosDe(env);
        final Map<String, Object> out = new HashMap<String, Object>();
        for (final Map.Entry<String, ?> e : env.entrySet()) {
            final String k = e.getKey();
            if (k == null || estaOculto(k, ocultos)) {
                continue;
            }
            if (e.getValue() != null && !(e.getValue() instanceof Serializable)) {
                continue;
            }
            out.put(k, e.getValue());
        }
        return out;
    }

    private static String[] ocultosDe(Map<String, ?> env) {
        final Object v = env.get(ATRIBUTOS_OCULTOS);
        if (!(v instanceof String)) {
            return OCULTOS;
        }
        // La lista propia se escribe separada por espacios y reemplaza a la de siempre, no la
        // amplia: quien la escribe esta diciendo exactamente que quiere tapar.
        final String s = ((String) v).trim();
        return s.isEmpty() ? new String[0] : s.split("\\s+");
    }

    private static boolean estaOculto(String clave, String[] ocultos) {
        for (final String o : ocultos) {
            if (o.endsWith(".") ? clave.startsWith(o) : clave.equals(o)) {
                return true;
            }
        }
        return false;
    }
}
