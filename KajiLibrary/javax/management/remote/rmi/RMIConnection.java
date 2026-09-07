package javax.management.remote.rmi;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.MarshalledObject;
import java.rmi.Remote;
import java.util.Set;

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
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.NotificationResult;
import javax.security.auth.Subject;

/**
 * El objeto remoto por el que viaja cada llamada a un {@code MBeanServer} de otra maquina.
 *
 * <h2>Es el MBeanServer, mas dos cosas</h2>
 *
 * <p>Los veintipico de metodos son los de {@link javax.management.MBeanServer} con dos diferencias
 * sistematicas, y entender esas dos diferencias es entender la interfaz entera.
 *
 * <p><strong>Un {@link Subject} al final.</strong> Quien abrio la conexion se autentico una vez;
 * este parametro dice en nombre de quien se hace <strong>esta</strong> llamada. Es lo que permite
 * que un servidor intermedio hable con el MBeanServer en representacion de varios usuarios sin
 * abrir una conexion por cada uno. Con {@code null} la llamada va a nombre del que se autentico.
 *
 * <p><strong>{@link MarshalledObject} en lugar de los objetos.</strong> Los parametros que podrian
 * ser de clases que el servidor no conoce —un filtro de notificaciones propio, el argumento de una
 * operacion— viajan serializados y se deserializan del otro lado, con el cargador de clases de la
 * conexion. Si viajaran como objetos, RMI los deserializaria al recibirlos, antes de que nadie
 * pudiera decidir con que cargador; y ese momento es justamente donde hay que decidirlo.
 *
 * <p>Esa envoltura es tambien la que hace posible poner un filtro de deserializacion por conexion:
 * los bytes se pueden mirar antes de convertirlos en objetos.
 *
 * <h2>Las notificaciones van al reves</h2>
 *
 * <p>{@code fetchNotifications} es el unico metodo que no tiene equivalente en {@code MBeanServer}.
 * El cliente <strong>pregunta</strong> por las notificaciones acumuladas en vez de recibirlas: para
 * que el servidor pudiera llamarlo, el cliente tendria que ser el tambien un objeto remoto
 * alcanzable desde el servidor, y eso no sobrevive a un cortafuegos ni a un NAT.
 *
 * @since 1.5
 */
public interface RMIConnection extends Closeable, Remote {

    /**
     * El identificador de esta conexion, el mismo que ve el servidor.
     * @return lo que conteste el MBeanServer del otro lado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    String getConnectionId() throws IOException;

    /**
     * Cierra la conexion y libera lo que el servidor tenia reservado para ella.
     * @throws IOException si se corto la comunicacion con el servidor
     */
    void close() throws IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param className el nombre de la clase del MBean
     * @param name el nombre del MBean
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws InstanceAlreadyExistsException si ya hay un MBean con ese nombre
     * @throws MBeanRegistrationException si el MBean se opuso a registrarse o a darse de baja
     * @throws MBeanException si el propio MBean lanzo una excepcion
     * @throws NotCompliantMBeanException si la clase no cumple con lo que un MBean tiene que ser
     * @throws IOException si se corto la comunicacion con el servidor
     */
    ObjectInstance createMBean(String className, ObjectName name, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param className el nombre de la clase del MBean
     * @param name el nombre del MBean
     * @param loaderName el cargador de clases a usar
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws InstanceAlreadyExistsException si ya hay un MBean con ese nombre
     * @throws MBeanRegistrationException si el MBean se opuso a registrarse o a darse de baja
     * @throws MBeanException si el propio MBean lanzo una excepcion
     * @throws NotCompliantMBeanException si la clase no cumple con lo que un MBean tiene que ser
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws IOException si se corto la comunicacion con el servidor
     */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param className el nombre de la clase del MBean
     * @param name el nombre del MBean
     * @param params los argumentos, serializados
     * @param signature la firma de los argumentos
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws InstanceAlreadyExistsException si ya hay un MBean con ese nombre
     * @throws MBeanRegistrationException si el MBean se opuso a registrarse o a darse de baja
     * @throws MBeanException si el propio MBean lanzo una excepcion
     * @throws NotCompliantMBeanException si la clase no cumple con lo que un MBean tiene que ser
     * @throws IOException si se corto la comunicacion con el servidor
     */
    ObjectInstance createMBean(String className, ObjectName name, MarshalledObject params,
            String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param className el nombre de la clase del MBean
     * @param name el nombre del MBean
     * @param loaderName el cargador de clases a usar
     * @param params los argumentos, serializados
     * @param signature la firma de los argumentos
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws InstanceAlreadyExistsException si ya hay un MBean con ese nombre
     * @throws MBeanRegistrationException si el MBean se opuso a registrarse o a darse de baja
     * @throws MBeanException si el propio MBean lanzo una excepcion
     * @throws NotCompliantMBeanException si la clase no cumple con lo que un MBean tiene que ser
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws IOException si se corto la comunicacion con el servidor
     */
    ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
            MarshalledObject params, String[] signature, Subject delegationSubject)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException,
                   IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws MBeanRegistrationException si el MBean se opuso a registrarse o a darse de baja
     * @throws IOException si se corto la comunicacion con el servidor
     */
    void unregisterMBean(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanRegistrationException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws IOException si se corto la comunicacion con el servidor
     */
    ObjectInstance getObjectInstance(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param query el filtro de consulta, serializado
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    Set<ObjectInstance> queryMBeans(ObjectName name, MarshalledObject query,
            Subject delegationSubject)
            throws IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param query el filtro de consulta, serializado
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    Set<ObjectName> queryNames(ObjectName name, MarshalledObject query, Subject delegationSubject)
            throws IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    boolean isRegistered(ObjectName name, Subject delegationSubject) throws IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    Integer getMBeanCount(Subject delegationSubject) throws IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param attribute el atributo
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws MBeanException si el propio MBean lanzo una excepcion
     * @throws AttributeNotFoundException si el MBean no tiene ese atributo
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws IOException si se corto la comunicacion con el servidor
     */
    Object getAttribute(ObjectName name, String attribute, Subject delegationSubject)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param attributes los atributos
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws IOException si se corto la comunicacion con el servidor
     */
    AttributeList getAttributes(ObjectName name, String[] attributes, Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param attribute el atributo
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws AttributeNotFoundException si el MBean no tiene ese atributo
     * @throws InvalidAttributeValueException si el valor no sirve para ese atributo
     * @throws MBeanException si el propio MBean lanzo una excepcion
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws IOException si se corto la comunicacion con el servidor
     */
    void setAttribute(ObjectName name, MarshalledObject attribute, Subject delegationSubject)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param attributes los atributos
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws IOException si se corto la comunicacion con el servidor
     */
    AttributeList setAttributes(ObjectName name, MarshalledObject attributes,
            Subject delegationSubject)
            throws InstanceNotFoundException, ReflectionException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param operationName el nombre de la operacion
     * @param params los argumentos, serializados
     * @param signature la firma de los argumentos
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws MBeanException si el propio MBean lanzo una excepcion
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws IOException si se corto la comunicacion con el servidor
     */
    Object invoke(ObjectName name, String operationName, MarshalledObject params,
            String[] signature, Subject delegationSubject)
            throws InstanceNotFoundException, MBeanException, ReflectionException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    String getDefaultDomain(Subject delegationSubject) throws IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    String[] getDomains(Subject delegationSubject) throws IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws IntrospectionException si no se pudo averiguar la forma del MBean
     * @throws ReflectionException si fallo la reflexion al construir o al llamar
     * @throws IOException si se corto la comunicacion con el servidor
     */
    MBeanInfo getMBeanInfo(ObjectName name, Subject delegationSubject)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException,
                   IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param className el nombre de la clase del MBean
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @return lo que conteste el MBeanServer del otro lado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws IOException si se corto la comunicacion con el servidor
     */
    boolean isInstanceOf(ObjectName name, String className, Subject delegationSubject)
            throws InstanceNotFoundException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param listener el oyente
     * @param filter el filtro, serializado
     * @param handback el objeto que se devuelve con cada notificacion, serializado
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws IOException si se corto la comunicacion con el servidor
     */
    void addNotificationListener(ObjectName name, ObjectName listener, MarshalledObject filter,
            MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param listener el oyente
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws ListenerNotFoundException si ese oyente no estaba registrado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    void removeNotificationListener(ObjectName name, ObjectName listener, Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param listener el oyente
     * @param filter el filtro, serializado
     * @param handback el objeto que se devuelve con cada notificacion, serializado
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws ListenerNotFoundException si ese oyente no estaba registrado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    void removeNotificationListener(ObjectName name, ObjectName listener, MarshalledObject filter,
            MarshalledObject handback, Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /**
     * Registra varios oyentes de una, y devuelve un identificador por cada uno.
     *
     * <p>De a varios porque cada registro es un viaje de ida y vuelta por la red. Los
     * identificadores son lo que despues permite sacarlos sin volver a mandar el filtro.
     *
     * @param names los nombres de los MBeans
     * @param filters los filtros, serializados
     * @param delegationSubjects en nombre de quien se hace cada llamada
     * @return lo que conteste el MBeanServer del otro lado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws IOException si se corto la comunicacion con el servidor
     */
    Integer[] addNotificationListeners(ObjectName[] names, MarshalledObject[] filters,
            Subject[] delegationSubjects)
            throws InstanceNotFoundException, IOException;

    /**
     * Reenvia la operacion del mismo nombre de {@code MBeanServer}.
     *
     * @param name el nombre del MBean
     * @param listenerIDs los identificadores que devolvio {@code addNotificationListeners}
     * @param delegationSubject en nombre de quien se hace, o {@code null} para el autenticado
     * @throws InstanceNotFoundException si no hay ningun MBean con ese nombre
     * @throws ListenerNotFoundException si ese oyente no estaba registrado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    void removeNotificationListeners(ObjectName name, Integer[] listenerIDs,
            Subject delegationSubject)
            throws InstanceNotFoundException, ListenerNotFoundException, IOException;

    /**
     * Trae las notificaciones que se acumularon del lado del servidor.
     *
     * <p>Es el corazon del modelo: las notificaciones no se empujan al cliente, el cliente
     * las viene a buscar. Con RMI no hay forma de que el servidor llame al cliente sin que
     * el cliente sea a su vez un objeto remoto, y eso no sobrevive a un cortafuegos.
     *
     * @param clientSequenceNumber desde que numero de secuencia traer
     * @param maxNotifications cuantas traer como maximo
     * @param timeout cuanto esperar si no hay ninguna
     * @return lo que conteste el MBeanServer del otro lado
     * @throws IOException si se corto la comunicacion con el servidor
     */
    NotificationResult fetchNotifications(long clientSequenceNumber, int maxNotifications,
            long timeout)
            throws IOException;
}
