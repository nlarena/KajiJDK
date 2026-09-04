package javax.management;

import javax.management.loading.ClassLoaderRepository;

import java.util.Set;

/**
 * El agente local: el registro donde viven los MBeans de esta maquina virtual.
 *
 * <p>Redeclara <b>todos</b> los metodos de {@link MBeanServerConnection} sin `IOException`. La
 * repeticion parece gratuita y no lo es: es la unica forma que da Java de restringir el `throws` de
 * una interfaz heredada, y es lo que hace que el codigo local no tenga que atajar una falla de red
 * imposible.
 *
 * <p>Agrega ademas lo que solo tiene sentido en el mismo proceso: {@link #registerMBean}, que
 * registra un objeto <b>ya construido</b> --imposible de mandar por la red--, los cuatro
 * {@link #instantiate} y el acceso a los cargadores de clases.
 *
 * <h2>Que falta y por que</h2>
 *
 * <p>Los tres {@code deserialize} estan, y tiran {@link UnsupportedOperationException} -- que es
 * <b>literalmente el cuerpo que tienen en el JDK</b>. Desde que se volvieron metodos por omision no
 * son un contrato que la interfaz prometa cumplir: son un lugar donde una implementacion concreta
 * puede poner algo si quiere, y la interfaz avisa que ella no lo hace. Estan obsoletos desde 1.5.
 *
 * <p>Antes faltaban porque `java.io.ObjectInputStream` no estaba en esta biblioteca; ya esta.
 */
public interface MBeanServer extends MBeanServerConnection {

    /** Instancia y registra un MBean de la clase dada. */
    ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException;

    /** Igual, cargando la clase con el cargador registrado bajo `loaderName`. */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                   InstanceNotFoundException;

    /** Igual, eligiendo constructor por la firma. */
    ObjectInstance createMBean(String className, ObjectName name, Object[] params,
                               String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException;

    /** Con cargador y constructor elegidos. */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
                               Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException,
                   MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
                   InstanceNotFoundException;

    /**
     * Registra un objeto que ya existe.
     *
     * <p>Es lo que un agente remoto no puede ofrecer, y por eso no esta en
     * {@link MBeanServerConnection}. El `name` puede ser `null` si el MBean implementa
     * {@link MBeanRegistration} y se nombra solo.
     */
    ObjectInstance registerMBean(Object object, ObjectName name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
                   NotCompliantMBeanException;

    /** Da de baja un MBean. */
    void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException;

    /** Nombre y clase de un MBean registrado. */
    ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException;

    /** Los MBeans que coinciden, con su clase. */
    Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query);

    /** Lo mismo, solo los nombres. */
    Set<ObjectName> queryNames(ObjectName name, QueryExp query);

    /** Si hay un MBean con ese nombre. */
    boolean isRegistered(ObjectName name);

    /** Cuantos MBeans hay. */
    Integer getMBeanCount();

    /** Lee un atributo. */
    Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException;

    /** Lee varios; los que fallan no aparecen en la respuesta. */
    AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException;

    /** Escribe un atributo. */
    void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException;

    /** Escribe varios; devuelve los que se pudieron escribir. */
    AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException;

    /** Invoca una operacion. */
    Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException;

    /** El dominio que se usa cuando un nombre no trae ninguno. */
    String getDefaultDomain();

    /** Los dominios en los que hay algun MBean registrado. */
    String[] getDomains();

    /** Registra un oyente contra un MBean. */
    void addNotificationListener(ObjectName name, NotificationListener listener,
                                 NotificationFilter filter, Object handback)
            throws InstanceNotFoundException;

    /** Registra como oyente a otro MBean. */
    void addNotificationListener(ObjectName name, ObjectName listener,
                                 NotificationFilter filter, Object handback)
            throws InstanceNotFoundException;

    /** Saca todos los registros de ese MBean oyente. */
    void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException;

    /** Saca el registro exacto de ese MBean oyente. */
    void removeNotificationListener(ObjectName name, ObjectName listener,
                                    NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException;

    /** Saca todos los registros de ese oyente. */
    void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException;

    /** Saca el registro exacto. */
    void removeNotificationListener(ObjectName name, NotificationListener listener,
                                    NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException;

    /** Los metadatos del MBean. */
    MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException;

    /** Si el MBean es de esa clase o de una subclase. */
    boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException;

    /**
     * Construye un objeto <b>sin</b> registrarlo.
     *
     * <p>Sirve para fabricar los argumentos de otra llamada usando los cargadores del agente.
     */
    Object instantiate(String className) throws ReflectionException, MBeanException;

    /** Igual, con el cargador registrado bajo `loaderName`. */
    Object instantiate(String className, ObjectName loaderName)
            throws ReflectionException, MBeanException, InstanceNotFoundException;

    /** Igual, eligiendo constructor. */
    Object instantiate(String className, Object[] params, String[] signature)
            throws ReflectionException, MBeanException;

    /** Con cargador y constructor elegidos. */
    Object instantiate(String className, ObjectName loaderName, Object[] params,
                       String[] signature)
            throws ReflectionException, MBeanException, InstanceNotFoundException;

    /** El cargador de clases con el que se cargo ese MBean. */
    ClassLoader getClassLoaderFor(ObjectName name) throws InstanceNotFoundException;

    /** El cargador de clases que esta registrado <b>como</b> MBean bajo ese nombre. */
    ClassLoader getClassLoader(ObjectName name) throws InstanceNotFoundException;

    /**
     * Los cargadores que este agente conoce, para buscar una clase por nombre.
     *
     * <p>Es lo que permite cargar una clase que llego nombrada desde afuera y que no esta en el
     * classpath del agente.
     */
    ClassLoaderRepository getClassLoaderRepository();

    /**
     * Deserializa un arreglo de bytes con el cargador del MBean nombrado.
     *
     * <p>Esta interfaz no lo hace: tira {@link UnsupportedOperationException}, que es el mismo
     * cuerpo que tiene en el JDK. Una implementacion concreta puede redefinirlo.
     *
     * @deprecated como en el JDK desde 1.5: usar {@link #getClassLoaderFor} y deserializar afuera
     */
    @Deprecated
    default java.io.ObjectInputStream deserialize(ObjectName name, byte[] data)
            throws InstanceNotFoundException, OperationsException {
        throw new UnsupportedOperationException("deserialize");
    }

    /**
     * Deserializa un arreglo de bytes con el cargador de la clase nombrada.
     *
     * <p>Ver {@link #deserialize(ObjectName, byte[])}: tira {@link UnsupportedOperationException}.
     *
     * @deprecated como en el JDK desde 1.5: usar {@link #getClassLoaderRepository}
     */
    @Deprecated
    default java.io.ObjectInputStream deserialize(String className, byte[] data)
            throws OperationsException, ReflectionException {
        throw new UnsupportedOperationException("deserialize");
    }

    /**
     * Deserializa un arreglo de bytes con el cargador nombrado.
     *
     * <p>Ver {@link #deserialize(ObjectName, byte[])}: tira {@link UnsupportedOperationException}.
     *
     * @deprecated como en el JDK desde 1.5: usar {@link #getClassLoader} y deserializar afuera
     */
    @Deprecated
    default java.io.ObjectInputStream deserialize(String className, ObjectName loaderName,
            byte[] data) throws InstanceNotFoundException, OperationsException,
            ReflectionException {
        throw new UnsupportedOperationException("deserialize");
    }
}
