package javax.management.remote.rmi;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.security.auth.Subject;

/**
 * La parte del servidor que no depende del transporte.
 *
 * <h2>Que queda de este lado de la linea</h2>
 *
 * <p>Lleva la lista de clientes conectados, el {@link MBeanServer} contra el que se trabaja, el
 * cargador de clases por omision y el ciclo de vida. Nada de eso cambia segun como se llegue hasta
 * aca, y por eso esta escrito una sola vez.
 *
 * <p>Lo que deja abstracto es exactamente lo que si cambia: {@link #export} y {@link #toStub}
 * publican el objeto por el transporte concreto, y {@link #makeClient} fabrica la conexion. Es la
 * separacion entre "que hace un servidor JMX" y "como se lo alcanza".
 *
 * <h2>{@link #newClient} es la puerta</h2>
 *
 * <p>Autentica --si hay un {@link JMXAuthenticator} en el entorno--, arma el identificador de la
 * conexion, la crea con {@link #makeClient} y la anota. Cada cliente recibe la suya, que es lo que
 * permite cerrarle la puerta a uno sin tocar a los demas.
 *
 * <p>{@link #close} cierra todas: primero el transporte para que no entren mas, despues cada
 * conexion viva. El orden importa, porque al reves entraria un cliente nuevo mientras se cierran los
 * viejos.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Todo lo de esta clase funciona de verdad: la autenticacion, la lista de clientes, los
 * identificadores, el cierre en cascada. Lo que no puede funcionar es lo abstracto, y eso lo decide
 * la subclase: {@link RMIJRMPServerImpl} necesita exportar por RMI, y esta VM no tiene ese
 * transporte.
 *
 * @since 1.5
 */
public abstract class RMIServerImpl implements Closeable, RMIServer {

    private final Map<String, ?> env;
    private final List<RMIConnection> clientes = new ArrayList<RMIConnection>();

    private ClassLoader cl;
    private MBeanServer mbeanServer;
    private boolean cerrado;
    private int numeroDeConexion;

    /**
     * Un servidor con ese entorno.
     *
     * @param env las propiedades de configuracion, o {@code null}
     */
    public RMIServerImpl(Map<String, ?> env) {
        this.env = env == null ? Collections.<String, Object>emptyMap() : env;
    }

    /**
     * Publica este objeto por el transporte concreto.
     *
     * @throws IOException si no se pudo publicar
     */
    protected abstract void export() throws IOException;

    /**
     * El objeto remoto que hay que mandarle al cliente para que llegue hasta aca.
     *
     * @return el stub
     * @throws IOException si no se pudo obtener
     */
    public abstract Remote toStub() throws IOException;

    /**
     * Fija el cargador con el que se deserializa lo que mandan los clientes.
     *
     * <p>Es una decision de seguridad y no de comodidad: define que clases puede hacer aparecer un
     * cliente dentro de este proceso.
     *
     * @param cl el cargador
     */
    public synchronized void setDefaultClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    /**
     * El cargador por omision.
     *
     * @return el cargador, o {@code null}
     */
    public synchronized ClassLoader getDefaultClassLoader() {
        return cl;
    }

    /**
     * Fija el {@link MBeanServer} contra el que trabajan las conexiones.
     *
     * @param mbs el servidor de MBeans
     */
    public synchronized void setMBeanServer(MBeanServer mbs) {
        this.mbeanServer = mbs;
    }

    /**
     * El {@link MBeanServer} configurado.
     *
     * @return el servidor de MBeans, o {@code null}
     */
    public synchronized MBeanServer getMBeanServer() {
        return mbeanServer;
    }

    /**
     * La version del protocolo y del proveedor.
     *
     * <p>El formato es la version del protocolo, un espacio, y el nombre de la implementacion. La
     * segunda mitad sale de {@code java.runtime.version}: es a proposito que diga que runtime esta
     * corriendo, porque es lo que el cliente mira cuando algo no se entiende entre las dos puntas.
     *
     * @return la version
     */
    public String getVersion() {
        try {
            return "1.0 java_runtime_" + System.getProperty("java.runtime.version");
        } catch (SecurityException e) {
            return "1.0 ";
        }
    }

    /**
     * Autentica al cliente y le abre su conexion.
     *
     * @param credentials la credencial, o {@code null}
     * @return la conexion del cliente
     * @throws IOException si el servidor esta cerrado o no se pudo crear la conexion
     * @throws IllegalStateException si todavia no se le puso un {@link MBeanServer}
     * @throws SecurityException si la credencial no sirve
     */
    public RMIConnection newClient(Object credentials) throws IOException {
        // El orden es el del JDK y no da lo mismo: primero se comprueba que haya MBeanServer y
        // recien despues se autentica. Al reves, un servidor mal armado le pediria la credencial al
        // cliente para despues decirle que no estaba listo -- o sea, le haria mandar un secreto a
        // algo que no puede atenderlo.
        synchronized (this) {
            if (cerrado) {
                throw new IOException("The server has been closed");
            }
            if (mbeanServer == null) {
                throw new IllegalStateException("Not attached to an MBean server");
            }
        }
        final Subject subject = autenticar(credentials);
        final String id;
        synchronized (this) {
            id = idDeConexion(getProtocol(), subject);
        }
        // makeClient queda fuera del bloque sincronizado a proposito: es de la subclase, puede
        // tardar --exportar abre un puerto-- y tenerlo adentro dejaria al servidor entero trabado
        // mientras un solo cliente se conecta.
        final RMIConnection c = makeClient(id, subject);
        synchronized (this) {
            clientes.add(c);
        }
        return c;
    }

    /**
     * Autentica con el {@link JMXAuthenticator} del entorno, si hay alguno.
     *
     * <p>Sin autenticador la conexion se acepta sin sujeto: es la configuracion por omision, y es
     * la razon por la que un servidor JMX no se publica en una red que no sea de confianza.
     */
    private Subject autenticar(Object credentials) {
        final Object a = env.get(JMXConnectorServer.AUTHENTICATOR);
        if (a == null) {
            return null;
        }
        return ((JMXAuthenticator) a).authenticate(credentials);
    }

    /**
     * El identificador que le toca a la conexion que se esta abriendo.
     *
     * <p>Lleva el protocolo, la maquina del cliente, quien se autentico y un numero que no se
     * repite. Sirve para que el registro del servidor diga algo util: sin el protocolo y sin el
     * numero, dos conexiones del mismo usuario serian indistinguibles en el archivo de registro.
     *
     * <p>La maquina del cliente solo se sabe <strong>durante</strong> una llamada remota, asi que
     * cuando no hay ninguna en curso se omite. Aca nunca la hay, porque no hay transporte.
     */
    private String idDeConexion(String protocolo, Subject subject) {
        numeroDeConexion++;
        String maquina = "";
        try {
            maquina = RemoteServer.getClientHost();
        } catch (ServerNotActiveException e) {
            maquina = "";
        }
        final StringBuilder b = new StringBuilder();
        b.append(protocolo).append(':');
        if (maquina.length() > 0) {
            b.append("//").append(maquina);
        }
        b.append(' ');
        if (subject != null) {
            final Set<java.security.Principal> ps = subject.getPrincipals();
            String sep = "";
            for (final java.security.Principal p : ps) {
                final String nombre = p.getName().replace(' ', '_').replace(';', ':');
                b.append(sep).append(nombre);
                sep = ";";
            }
        }
        b.append(' ').append(numeroDeConexion);
        return b.toString();
    }

    /**
     * Fabrica la conexion de un cliente.
     *
     * @param connectionId el identificador que le toca
     * @param subject quien se autentico, o {@code null}
     * @return la conexion
     * @throws IOException si no se pudo crear
     */
    protected abstract RMIConnection makeClient(String connectionId, Subject subject)
            throws IOException;

    /**
     * Cierra la conexion de un cliente.
     *
     * @param client la conexion
     * @throws IOException si no se pudo cerrar
     */
    protected abstract void closeClient(RMIConnection client) throws IOException;

    /**
     * El nombre del protocolo de este transporte, como {@code "rmi"}.
     *
     * @return el protocolo
     */
    protected abstract String getProtocol();

    /**
     * Aviso de que una conexion se cerro sola.
     *
     * <p>Lo llama la propia conexion. Sacarla de la lista aca --y no solo en {@link #close}-- es lo
     * que evita que un servidor de larga vida acumule conexiones muertas.
     *
     * @param client la conexion que se cerro
     * @throws IOException si no se pudo procesar
     * @throws NullPointerException si {@code client} es {@code null}
     */
    protected void clientClosed(RMIConnection client) throws IOException {
        if (client == null) {
            throw new NullPointerException("Null client");
        }
        synchronized (this) {
            clientes.remove(client);
        }
        closeClient(client);
    }

    /**
     * Cierra el servidor y todas las conexiones vivas.
     *
     * <p>Primero deja de aceptar y despues cierra las que hay: al reves entraria un cliente nuevo
     * mientras se cierran los viejos.
     *
     * @throws IOException si algo no se pudo cerrar
     */
    public void close() throws IOException {
        final List<RMIConnection> copia;
        synchronized (this) {
            if (cerrado) {
                return;
            }
            cerrado = true;
            copia = new ArrayList<RMIConnection>(clientes);
            clientes.clear();
        }
        IOException primera = null;
        try {
            closeServer();
        } catch (IOException e) {
            primera = e;
        }
        for (final RMIConnection c : copia) {
            try {
                closeClient(c);
            } catch (IOException e) {
                // Una conexion que no cierra no puede impedir que cierren las demas: se guarda la
                // primera falla, se siguen cerrando todas, y recien al final se la lanza.
                if (primera == null) {
                    primera = e;
                }
            }
        }
        if (primera != null) {
            throw primera;
        }
    }

    /**
     * Cierra el transporte.
     *
     * @throws IOException si no se pudo cerrar
     */
    protected abstract void closeServer() throws IOException;

    /** El entorno con el que se construyo; nunca {@code null}. Para las subclases del paquete. */
    Map<String, ?> entorno() {
        return env;
    }
}
