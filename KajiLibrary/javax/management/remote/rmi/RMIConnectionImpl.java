package javax.management.remote.rmi;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;
import javax.security.auth.Subject;

/**
 * La conexion de un cliente, del lado del servidor.
 *
 * <h2>Casi todo es reenvio</h2>
 *
 * <p>Cada metodo saca el {@link MBeanServer} del {@link RMIServerImpl} que la creo y le pasa la
 * llamada. Lo unico que agrega es lo que el API remoto necesita y el local no: deserializar los
 * argumentos que vinieron envueltos, y llevar la cola de notificaciones.
 *
 * <h2>Los {@link MarshalledObject}</h2>
 *
 * <p>Los argumentos que podrian ser de clases que el servidor no conoce viajan serializados. Aca se
 * los abre con {@link #desenvolver}, y ese es el momento --el unico-- en que se puede elegir con que
 * cargador de clases se los construye. Si viajaran como objetos, RMI ya los habria deserializado
 * antes de llegar hasta aca, con el cargador que le tocara.
 *
 * <h2>La cola de notificaciones</h2>
 *
 * <p>El cliente no recibe notificaciones: las viene a buscar con {@link #fetchNotifications}. Cada
 * oyente que registra queda anotado con un numero, y lo que llega se guarda en una cola con un
 * numero de secuencia creciente. El cliente pide "desde el numero N" y se lleva lo que haya.
 *
 * <p>La cola tiene un tope. Cuando se llena se tiran las mas viejas, y el numero de la mas vieja que
 * queda sube: asi el cliente que se durmio se entera de que se perdio cosas, en vez de recibir un
 * hueco silencioso.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Esta clase <strong>funciona entera</strong>, y es la unica del paquete de la que se puede decir
 * eso. No necesita el transporte de RMI: el transporte es lo que traeria las llamadas hasta aca, y
 * si se la construye a mano --que es lo que hace la prueba {@code java/RMI1.java}-- reenvia al
 * {@link MBeanServer} igual que en el JDK.
 *
 * @since 1.5
 */
public class RMIConnectionImpl implements RMIConnection, Unreferenced {

    /** Cuantas notificaciones se guardan antes de empezar a tirar las viejas. */
    private static final int TOPE = 1000;

    private final RMIServerImpl server;
    private final String connectionId;
    private final ClassLoader defaultLoader;
    private final Subject subject;

    private final List<TargetedNotification> cola = new ArrayList<TargetedNotification>();
    private final List<Registro> oyentes = new ArrayList<Registro>();

    /** El numero de secuencia de la primera notificacion que sigue en la cola. */
    private long primera;

    /** El numero que le tocara a la proxima notificacion que llegue. */
    private long proxima;

    private int proximoOyente;
    private boolean terminada;

    /** Un oyente registrado por el cliente, con el numero que lo identifica. */
    private static final class Registro {
        final Integer id;
        final ObjectName nombre;
        final NotificationListener oyente;

        Registro(Integer id, ObjectName nombre, NotificationListener oyente) {
            this.id = id;
            this.nombre = nombre;
            this.oyente = oyente;
        }
    }

    /**
     * Una conexion para ese cliente.
     *
     * @param rmiServer el servidor que la crea
     * @param connectionId el identificador de la conexion
     * @param defaultClassLoader el cargador con el que se deserializa, o {@code null}
     * @param subject quien se autentico, o {@code null}
     * @param env las propiedades de configuracion, o {@code null}
     * @throws NullPointerException si {@code rmiServer} o {@code connectionId} son {@code null}
     */
    public RMIConnectionImpl(RMIServerImpl rmiServer, String connectionId,
            ClassLoader defaultClassLoader, Subject subject, Map<String, ?> env) {
        if (rmiServer == null || connectionId == null) {
            throw new NullPointerException("Illegal null argument");
        }
        this.server = rmiServer;
        this.connectionId = connectionId;
        this.defaultLoader = defaultClassLoader;
        this.subject = subject;
        final long ahora = System.currentTimeMillis();
        this.primera = ahora;
        this.proxima = ahora;
    }

    /**
     * El identificador de esta conexion.
     *
     * @return el identificador
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Cierra la conexion y le avisa al servidor.
     *
     * <p>Es reentrante a proposito: el servidor puede cerrarla mientras ella le esta avisando de que
     * se cerro. Sin la bandera, esos dos caminos se llamarian en circulo.
     *
     * @throws IOException si el servidor no pudo procesar el cierre
     */
    public void close() throws IOException {
        synchronized (this) {
            if (terminada) {
                return;
            }
            terminada = true;
            cola.clear();
            oyentes.clear();
        }
        server.clientClosed(this);
    }

    /**
     * Aviso de RMI de que ya no queda nadie del otro lado.
     *
     * <p>Es la red de seguridad para el cliente que se muere sin cerrar. Sin esto, sus oyentes
     * seguirian registrados en el MBeanServer para siempre.
     */
    public void unreferenced() {
        try {
            close();
        } catch (IOException e) {
            // No hay a quien contarselo: del otro lado ya no hay nadie, que es justamente por lo
            // que se llego hasta aca. Cerrar es lo que importaba y ya se intento.
        }
    }

    /** El MBeanServer contra el que trabaja esta conexion, comprobando que siga abierta. */
    private synchronized MBeanServer mbs() throws IOException {
        if (terminada) {
            throw new IOException("The connection has been closed.");
        }
        final MBeanServer m = server.getMBeanServer();
        if (m == null) {
            throw new IllegalStateException("Not attached to an MBean server");
        }
        return m;
    }

    /**
     * Abre un argumento que vino serializado.
     *
     * <p>Un envoltorio {@code null} y un envoltorio de {@code null} son cosas distintas y las dos
     * son legitimas: la primera quiere decir "no se mando el argumento" y la segunda "se mando
     * {@code null}". Las dos dan {@code null} aca, que es lo correcto, pero por caminos distintos.
     *
     * @param mo el envoltorio, o {@code null}
     * @return lo que traia adentro
     * @throws IOException si los bytes no se pudieron leer, o traian una clase que no esta
     */
    private Object desenvolver(MarshalledObject<?> mo) throws IOException {
        if (mo == null) {
            return null;
        }
        try {
            return mo.get();
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found: " + e.getMessage(), e);
        }
    }

    public ObjectInstance createMBean(String className, ObjectName name, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        return mbs().createMBean(className, name);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        return mbs().createMBean(className, name, loaderName);
    }

    public ObjectInstance createMBean(String className, ObjectName name,
            MarshalledObject params, String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        return mbs().createMBean(className, name, (Object[]) desenvolver(params), signature);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            MarshalledObject params, String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        return mbs().createMBean(className, name, loaderName,
                (Object[]) desenvolver(params), signature);
    }

    public void unregisterMBean(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException {
        mbs().unregisterMBean(name);
    }

    public ObjectInstance getObjectInstance(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        return mbs().getObjectInstance(name);
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, MarshalledObject query,
            Subject delegationSubject) throws IOException {
        return mbs().queryMBeans(name, (QueryExp) desenvolver(query));
    }

    public Set<ObjectName> queryNames(ObjectName name, MarshalledObject query,
            Subject delegationSubject) throws IOException {
        return mbs().queryNames(name, (QueryExp) desenvolver(query));
    }

    public boolean isRegistered(ObjectName name, Subject delegationSubject) throws IOException {
        return mbs().isRegistered(name);
    }

    public Integer getMBeanCount(Subject delegationSubject) throws IOException {
        return mbs().getMBeanCount();
    }

    public Object getAttribute(ObjectName name, String attribute, Subject delegationSubject)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException {
        return mbs().getAttribute(name, attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes,
            Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException {
        return mbs().getAttributes(name, attributes);
    }

    public void setAttribute(ObjectName name, MarshalledObject attribute,
            Subject delegationSubject)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException,
                   IOException {
        mbs().setAttribute(name, (Attribute) desenvolver(attribute));
    }

    public AttributeList setAttributes(ObjectName name, MarshalledObject attributes,
            Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException {
        return mbs().setAttributes(name, (AttributeList) desenvolver(attributes));
    }

    public Object invoke(ObjectName name, String operationName, MarshalledObject params,
            String[] signature, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return mbs().invoke(name, operationName, (Object[]) desenvolver(params), signature);
    }

    public String getDefaultDomain(Subject delegationSubject) throws IOException {
        return mbs().getDefaultDomain();
    }

    public String[] getDomains(Subject delegationSubject) throws IOException {
        return mbs().getDomains();
    }

    public MBeanInfo getMBeanInfo(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException {
        return mbs().getMBeanInfo(name);
    }

    public boolean isInstanceOf(ObjectName name, String className, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        return mbs().isInstanceOf(name, className);
    }

    public void addNotificationListener(ObjectName name, ObjectName listener,
            MarshalledObject filter, MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, IOException {
        mbs().addNotificationListener(name, listener, (NotificationFilter) desenvolver(filter),
                desenvolver(handback));
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
            Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        mbs().removeNotificationListener(name, listener);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
            MarshalledObject filter, MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        mbs().removeNotificationListener(name, listener, (NotificationFilter) desenvolver(filter),
                desenvolver(handback));
    }

    /**
     * Registra varios oyentes y devuelve el numero de cada uno.
     *
     * <p>El oyente que se registra en el MBeanServer no es del cliente: es uno de aca, que lo unico
     * que hace es poner lo que llega en la cola con el numero que le corresponde. Ese numero es lo
     * que despues le permite al cliente saber a cual de sus oyentes va cada notificacion.
     *
     * <p>De a varios porque cada registro seria un viaje por la red.
     *
     * @param names los nombres de los MBeans
     * @param filters los filtros, serializados
     * @param delegationSubjects en nombre de quien se hace cada uno
     * @return un numero por cada oyente, en el mismo orden
     * @throws InstanceNotFoundException si alguno de los MBeans no esta
     * @throws IOException si la conexion esta cerrada o un filtro no se pudo abrir
     * @throws IllegalArgumentException si los arreglos no miden lo mismo
     */
    public Integer[] addNotificationListeners(ObjectName[] names, MarshalledObject[] filters,
            Subject[] delegationSubjects) throws InstanceNotFoundException, IOException {
        if (names == null || filters == null) {
            throw new IllegalArgumentException("Got null arguments.");
        }
        if (names.length != filters.length
                || delegationSubjects != null && delegationSubjects.length != names.length) {
            throw new IllegalArgumentException("The value lengths of 3 parameters are not same.");
        }
        final MBeanServer m = mbs();
        final Integer[] ids = new Integer[names.length];
        final List<Registro> puestos = new ArrayList<Registro>();
        try {
            for (int i = 0; i < names.length; i++) {
                if (names[i] == null) {
                    throw new IllegalArgumentException("Null Object name.");
                }
                final NotificationFilter f = (NotificationFilter) desenvolver(filters[i]);
                final Integer id;
                synchronized (this) {
                    id = Integer.valueOf(proximoOyente++);
                }
                final Registro r = new Registro(id, names[i], new Reenvio(id));
                m.addNotificationListener(names[i], r.oyente, f, null);
                puestos.add(r);
                ids[i] = id;
            }
        } catch (Exception e) {
            // Si uno falla se deshacen los que ya se pusieron: dejar la mitad registrada seria peor
            // que no registrar nada, porque el cliente cree que ninguno quedo y nadie los saca.
            for (final Registro r : puestos) {
                try {
                    m.removeNotificationListener(r.nombre, r.oyente);
                } catch (Exception otra) {
                    // Ya se esta deshaciendo por una falla; que uno no se pueda sacar no cambia
                    // cual es el problema que hay que contar.
                }
            }
            if (e instanceof InstanceNotFoundException) {
                throw (InstanceNotFoundException) e;
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new IOException(e.getMessage(), e);
        }
        synchronized (this) {
            oyentes.addAll(puestos);
        }
        return ids;
    }

    /**
     * Saca los oyentes con esos numeros.
     *
     * @param name el nombre del MBean
     * @param listenerIDs los numeros que devolvio {@link #addNotificationListeners}
     * @param delegationSubject en nombre de quien se hace
     * @throws InstanceNotFoundException si el MBean no esta
     * @throws ListenerNotFoundException si alguno de esos numeros no corresponde a un oyente
     * @throws IOException si la conexion esta cerrada
     * @throws IllegalArgumentException si algun numero es {@code null}
     */
    public void removeNotificationListeners(ObjectName name, Integer[] listenerIDs,
            Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        if (name == null || listenerIDs == null) {
            throw new IllegalArgumentException("Illegal null parameter");
        }
        final MBeanServer m = mbs();
        for (final Integer id : listenerIDs) {
            if (id == null) {
                throw new IllegalArgumentException("Null listener ID");
            }
            Registro r = null;
            synchronized (this) {
                for (final Registro x : oyentes) {
                    if (x.id.equals(id) && x.nombre.equals(name)) {
                        r = x;
                        break;
                    }
                }
                if (r != null) {
                    oyentes.remove(r);
                }
            }
            if (r == null) {
                throw new ListenerNotFoundException("Listener id: " + id);
            }
            m.removeNotificationListener(name, r.oyente);
        }
    }

    /**
     * Trae las notificaciones que se acumularon desde ese numero de secuencia.
     *
     * <p>Devuelve el numero de la mas vieja que todavia esta en la cola y el que le tocara a la
     * proxima. Si el primero es mayor que lo que el cliente pidio, el cliente sabe que se perdio
     * notificaciones porque tardo demasiado en volver.
     *
     * <p>El {@code timeout} es cuanto se espera si no hay ninguna. Es lo que convierte esto en algo
     * usable: sin la espera, un cliente que quiere estar al dia tendria que preguntar en un ciclo
     * cerrado.
     *
     * @param clientSequenceNumber desde que numero traer; negativo quiere decir "desde ahora"
     * @param maxNotifications cuantas traer como maximo
     * @param timeout cuanto esperar si no hay ninguna, en milisegundos
     * @return lo que habia
     * @throws IOException si la conexion esta cerrada
     * @throws IllegalArgumentException si {@code maxNotifications} o {@code timeout} son negativos
     */
    public NotificationResult fetchNotifications(long clientSequenceNumber, int maxNotifications,
            long timeout) throws IOException {
        if (maxNotifications < 0 || timeout < 0) {
            throw new IllegalArgumentException("Illegal negative argument");
        }
        final long limite = System.currentTimeMillis() + timeout;
        synchronized (this) {
            if (terminada) {
                throw new IOException("The connection has been closed.");
            }
            long desde = clientSequenceNumber < 0 ? proxima : clientSequenceNumber;
            while (desde >= proxima && !terminada) {
                final long queda = limite - System.currentTimeMillis();
                if (queda <= 0) {
                    break;
                }
                try {
                    wait(queda);
                } catch (InterruptedException e) {
                    // Que interrumpan la espera no es un error: quiere decir "devolve lo que tengas
                    // ahora". Se restaura la marca para que quien interrumpio se entere igual.
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (terminada) {
                throw new IOException("The connection has been closed.");
            }
            if (desde < primera) {
                desde = primera;
            }
            final List<TargetedNotification> salida = new ArrayList<TargetedNotification>();
            for (long i = desde; i < proxima && salida.size() < maxNotifications; i++) {
                salida.add(cola.get((int) (i - primera)));
            }
            final long siguiente = desde + salida.size();
            return new NotificationResult(primera, siguiente,
                    salida.toArray(new TargetedNotification[salida.size()]));
        }
    }

    /** Pone una notificacion en la cola y despierta a quien este esperando. */
    private synchronized void encolar(Integer id, Notification n) {
        if (terminada) {
            return;
        }
        cola.add(new TargetedNotification(n, id));
        proxima++;
        while (cola.size() > TOPE) {
            // Se tira la mas vieja y sube el numero de la primera. Que ese numero suba es
            // justamente el aviso: el cliente compara con lo que habia pedido y sabe que se le
            // escaparon algunas.
            cola.remove(0);
            primera++;
        }
        notifyAll();
    }

    /** El oyente que se registra en el MBeanServer en lugar del cliente. */
    private final class Reenvio implements NotificationListener {
        private final Integer id;

        Reenvio(Integer id) {
            this.id = id;
        }

        public void handleNotification(Notification notification, Object handback) {
            encolar(id, notification);
        }
    }

    /**
     * El identificador de la conexion, para el registro.
     *
     * @return una descripcion
     */
    @Override
    public String toString() {
        return super.toString() + ": connectionId=" + connectionId;
    }
}
