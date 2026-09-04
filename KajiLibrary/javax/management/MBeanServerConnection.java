package javax.management;

import java.io.IOException;
import java.util.Set;

/**
 * Todo lo que se le puede pedir a un agente JMX, este donde este.
 *
 * <p>Su razon de ser es un `throws`: cada metodo declara `IOException`. {@link MBeanServer}, que es
 * el agente <b>local</b>, redeclara los mismos metodos sin ella. Asi el codigo que solo va a hablar
 * con el agente de su propia maquina no paga el costo de atender una falla de red que no puede
 * ocurrir, y el codigo escrito contra esta interfaz anda con los dos.
 *
 * <p>De ahi que la relacion sea {@code MBeanServer extends MBeanServerConnection} y no al reves: lo
 * remoto es el caso general y lo local la restriccion.
 */
public interface MBeanServerConnection {

    /** Instancia y registra un MBean de la clase dada. */
    ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException,
                   NotCompliantMBeanException, IOException;

    /** Igual, pero cargando la clase con el cargador registrado bajo `loaderName`. */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                   InstanceNotFoundException, IOException;

    /**
     * Igual, eligiendo constructor.
     *
     * @param signature los nombres de las clases de los parametros, que es como se elige la firma
     */
    ObjectInstance createMBean(String className, ObjectName name, Object[] params,
                               String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException,
                   NotCompliantMBeanException, IOException;

    /** Con cargador y constructor elegidos. */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
                               Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                   InstanceNotFoundException, IOException;

    /** Da de baja un MBean. */
    void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException;

    /** Nombre y clase de un MBean registrado. */
    ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException, IOException;

    /**
     * Los MBeans que coinciden, con su clase.
     *
     * <p>Un `name` nulo equivale a {@link ObjectName#WILDCARD}; un `query` nulo no filtra nada mas.
     */
    Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException;

    /** Lo mismo, pero solo los nombres: mas barato si la clase no hace falta. */
    Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException;

    /** Si hay un MBean con ese nombre. */
    boolean isRegistered(ObjectName name) throws IOException;

    /** Cuantos MBeans hay. */
    Integer getMBeanCount() throws IOException;

    /** Lee un atributo. */
    Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException;

    /**
     * Lee varios de una.
     *
     * <p>La lista devuelta puede ser <b>mas corta</b> que la pedida: los atributos que fallaron
     * simplemente no estan. No hay forma de saber por cual fallo, y es asi por dise&ntilde;o -- la
     * operacion es de mejor esfuerzo.
     */
    AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException, IOException;

    /** Escribe un atributo. */
    void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException, IOException;

    /** Escribe varios; devuelve los que se pudieron escribir. */
    AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException, IOException;

    /**
     * Invoca una operacion.
     *
     * @param signature los nombres de las clases de los parametros, para desambiguar sobrecargas
     */
    Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException;

    /** El dominio que se usa cuando un nombre no trae ninguno. */
    String getDefaultDomain() throws IOException;

    /** Los dominios en los que hay algun MBean registrado. */
    String[] getDomains() throws IOException;

    /** Registra un oyente contra un MBean. */
    void addNotificationListener(ObjectName name, NotificationListener listener,
                                 NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException;

    /**
     * Registra como oyente a <b>otro MBean</b>.
     *
     * <p>Es la variante que sirve de verdad sobre una conexion remota: el oyente vive en el agente,
     * asi que las notificaciones no cruzan la red.
     */
    void addNotificationListener(ObjectName name, ObjectName listener,
                                 NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, IOException;

    /** Saca todos los registros de ese MBean oyente. */
    void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /** Saca el registro exacto de ese MBean oyente. */
    void removeNotificationListener(ObjectName name, ObjectName listener,
                                    NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /** Saca todos los registros de ese oyente. */
    void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /** Saca el registro exacto. */
    void removeNotificationListener(ObjectName name, NotificationListener listener,
                                    NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /** Los metadatos del MBean: la puerta de entrada a todo lo demas. */
    MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException;

    /** Si el MBean es de esa clase o de una subclase. */
    boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException, IOException;
}
