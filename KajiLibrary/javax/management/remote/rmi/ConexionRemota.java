package javax.management.remote.rmi;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import javax.management.MBeanServerConnection;
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

/**
 * El {@link MBeanServerConnection} que ve el cliente, sobre un {@link RMIConnection}.
 *
 * <h2>Que traduce</h2>
 *
 * <p>{@link MBeanServerConnection} es la interfaz comoda: recibe objetos. {@link RMIConnection} es
 * la de la red: recibe {@link MarshalledObject}. Esta clase es el paso de una a la otra, y lo unico
 * que hace es envolver lo que va y agregarle el sujeto {@code null} a cada llamada.
 *
 * <h2>Las notificaciones al reves</h2>
 *
 * <p>Del lado del servidor las notificaciones se acumulan en una cola. Aca hay un hilo que las viene
 * a buscar y las reparte entre los oyentes locales. Ese hilo es lo que hace que el cliente pueda
 * escribir {@code addNotificationListener} con un oyente propio y que funcione, cuando por la red
 * solo viajan numeros.
 *
 * <p>El hilo arranca con el primer oyente y no antes: un cliente que solo lee atributos no tiene por
 * que pagar un hilo ni una consulta periodica.
 *
 * <p>El filtro viaja al servidor y se aplica alla. Es lo que evita traer por la red notificaciones
 * que el cliente iba a descartar, que es justamente lo que un filtro tiene que evitar.
 */
final class ConexionRemota implements MBeanServerConnection {

    /** Cuantas notificaciones se piden por vez. */
    private static final int POR_VEZ = 100;

    /** Cuanto espera el servidor por notificaciones antes de contestar que no hay, en ms. */
    private static final long ESPERA = 500;

    private final RMIConnection conn;
    private final Map<Integer, Local> locales = new LinkedHashMap<Integer, Local>();

    private Thread bomba;
    private volatile boolean cerrada;
    private long secuencia = -1;

    /** Un oyente del cliente, con el filtro y el objeto que se le devuelve con cada notificacion. */
    private static final class Local {
        final ObjectName nombre;
        final NotificationListener oyente;
        final NotificationFilter filtro;
        final Object handback;

        Local(ObjectName nombre, NotificationListener oyente, NotificationFilter filtro,
                Object handback) {
            this.nombre = nombre;
            this.oyente = oyente;
            this.filtro = filtro;
            this.handback = handback;
        }
    }

    ConexionRemota(RMIConnection conn) {
        this.conn = conn;
    }

    /** Deja de traer notificaciones. Lo llama el conector al cerrarse. */
    void cerrar() {
        cerrada = true;
        final Thread t;
        synchronized (this) {
            locales.clear();
            t = bomba;
            bomba = null;
        }
        if (t != null) {
            t.interrupt();
        }
    }

    /** Envuelve un argumento para que viaje. */
    private static MarshalledObject<Object> envolver(Object o) throws IOException {
        if (o == null) {
            return null;
        }
        if (!(o instanceof Serializable)) {
            throw new IOException("Not serializable: " + o.getClass().getName());
        }
        return new MarshalledObject<Object>(o);
    }

    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        return conn.createMBean(className, name, null);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        return conn.createMBean(className, name, loaderName, null);
    }

    public ObjectInstance createMBean(String className, ObjectName name, Object[] params,
            String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException {
        return conn.createMBean(className, name, envolver(params), signature, null);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException {
        return conn.createMBean(className, name, loaderName, envolver(params), signature, null);
    }

    public void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException {
        conn.unregisterMBean(name, null);
    }

    public ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException, IOException {
        return conn.getObjectInstance(name, null);
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
        return conn.queryMBeans(name, envolver(query), null);
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
        return conn.queryNames(name, envolver(query), null);
    }

    public boolean isRegistered(ObjectName name) throws IOException {
        return conn.isRegistered(name, null);
    }

    public Integer getMBeanCount() throws IOException {
        return conn.getMBeanCount(null);
    }

    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException {
        return conn.getAttribute(name, attribute, null);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException, IOException {
        return conn.getAttributes(name, attributes, null);
    }

    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException,
                   IOException {
        conn.setAttribute(name, envolver(attribute), null);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException, IOException {
        return conn.setAttributes(name, envolver(attributes), null);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params,
            String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return conn.invoke(name, operationName, envolver(params), signature, null);
    }

    public String getDefaultDomain() throws IOException {
        return conn.getDefaultDomain(null);
    }

    public String[] getDomains() throws IOException {
        return conn.getDomains(null);
    }

    public MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException {
        return conn.getMBeanInfo(name, null);
    }

    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException, IOException {
        return conn.isInstanceOf(name, className, null);
    }

    public void addNotificationListener(ObjectName name, NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener");
        }
        final Integer[] ids = conn.addNotificationListeners(new ObjectName[] {name},
                new MarshalledObject[] {envolver(filter)}, null);
        synchronized (this) {
            locales.put(ids[0], new Local(name, listener, filter, handback));
            arrancarBomba();
        }
    }

    public void addNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException {
        conn.addNotificationListener(name, listener, envolver(filter), envolver(handback), null);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        quitar(name, listener, false, null, null);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        quitar(name, listener, true, filter, handback);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        conn.removeNotificationListener(name, listener, null);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        conn.removeNotificationListener(name, listener, envolver(filter), envolver(handback), null);
    }

    /**
     * Saca los registros locales que coinciden.
     *
     * <p>Sin los tres datos saca todos los del mismo oyente, con ellos saca solo el que coincide en
     * los tres. Es la misma regla que {@code NotificationBroadcasterSupport}: un mismo oyente puede
     * estar registrado varias veces con filtros distintos, y quitar el que no era seria peor que no
     * quitar ninguno.
     */
    private void quitar(ObjectName name, NotificationListener listener, boolean conFiltro,
            NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        final List<Integer> ids = new ArrayList<Integer>();
        synchronized (this) {
            for (final Map.Entry<Integer, Local> e : locales.entrySet()) {
                final Local l = e.getValue();
                if (!l.nombre.equals(name) || l.oyente != listener) {
                    continue;
                }
                if (conFiltro && (l.filtro != filter || l.handback != handback)) {
                    continue;
                }
                ids.add(e.getKey());
            }
            for (final Integer id : ids) {
                locales.remove(id);
            }
        }
        if (ids.isEmpty()) {
            throw new ListenerNotFoundException("Listener not found");
        }
        conn.removeNotificationListeners(name, ids.toArray(new Integer[ids.size()]), null);
    }

    /** Arranca el hilo que trae las notificaciones, si no estaba andando. Con el candado tomado. */
    private void arrancarBomba() {
        if (bomba != null || cerrada) {
            return;
        }
        bomba = new Thread(new Runnable() {
            public void run() {
                traer();
            }
        }, "JMX client notification fetcher");
        bomba.setDaemon(true);
        bomba.start();
    }

    /** El ciclo del hilo: pedir, repartir, volver a pedir. */
    private void traer() {
        while (!cerrada) {
            final NotificationResult r;
            try {
                r = conn.fetchNotifications(secuencia, POR_VEZ, ESPERA);
            } catch (IOException e) {
                // La conexion se corto: no hay a quien pedirle mas. Salir es lo correcto; insistir
                // dejaria un hilo girando contra un servidor que ya no esta.
                return;
            } catch (RuntimeException e) {
                return;
            }
            secuencia = r.getNextSequenceNumber();
            for (final TargetedNotification tn : r.getTargetedNotifications()) {
                final Local l;
                synchronized (this) {
                    l = locales.get(tn.getListenerID());
                }
                if (l != null) {
                    repartir(l, tn.getNotification());
                }
            }
        }
    }

    private static void repartir(Local l, Notification n) {
        try {
            l.oyente.handleNotification(n, l.handback);
        } catch (RuntimeException e) {
            // Un oyente que falla no puede cortar el reparto: los que siguen no tienen la culpa, y
            // este hilo es el unico que hay.
        }
    }
}
