package javax.management.modelmbean;

import java.lang.reflect.Method;
import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;

/**
 * KajiLibrary's javax.management.modelmbean.RequiredModelMBean -- la implementacion que toda
 * plataforma JMX tiene que traer.
 *
 * <p>Se le da un objeto cualquiera --{@link #setManagedResource}-- y una descripcion de que exponer
 * --{@link #setModelMBeanInfo}--, y queda administrable sin tocar su codigo. Es la unica forma de
 * poner bajo JMX una clase de terceros.
 *
 * <h2>Como resuelve una consulta</h2>
 *
 * <p>Cada atributo y cada operacion se resuelven por <b>reflexion</b> sobre el objeto administrado,
 * usando el descriptor del {@code Info} correspondiente:
 *
 * <ul>
 *   <li>para un atributo, el campo {@code getMethod} o {@code setMethod} nombra el metodo;
 *   <li>para una operacion, el campo {@code name} del descriptor, o el nombre de la operacion si no
 *       esta.
 * </ul>
 *
 * <p>De ahi que la configuracion pueda mentir y el error aparezca tarde: un {@code getMethod} que
 * nombra un metodo que no existe se descubre recien cuando alguien lee el atributo, y sale como
 * {@link ReflectionException}.
 *
 * <h2>Los dos canales de aviso</h2>
 *
 * <p>Los avisos comunes y los de cambio de atributo van por listas de oyentes <b>separadas</b>. Ver
 * {@link ModelMBeanNotificationBroadcaster}: el segundo canal filtra por atributo, y ese filtrado
 * pasa aca, no del lado del oyente.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #load} y {@link #store} lanzan {@link MBeanException}. La persistencia de un model MBean
 * --escribirse a un archivo segun {@code persistPolicy} y {@code persistLocation}-- necesita
 * serializacion de objetos, que esta biblioteca no tiene. Los dos metodos ya declaran esa excepcion
 * para el caso "esta implementacion no persiste", asi que no hay mentira: hay una operacion que dice
 * que no puede.
 *
 * <p>Todo lo demas --el despacho por reflexion, los descriptores, los dos canales de aviso, el
 * cacheo por {@code currencyTimeLimit}-- esta implementado.
 */
public class RequiredModelMBean implements ModelMBean, MBeanRegistration, NotificationEmitter {

    /** Que exponer. */
    private ModelMBeanInfo modelMBeanInfo;

    /** De que objeto. */
    private Object managedResource;

    /** Los oyentes del canal comun. */
    private final NotificationBroadcasterSupport general = new NotificationBroadcasterSupport();

    /** Los del canal de cambios de atributo, con el atributo que cada uno escucha. */
    private final java.util.List<AttributeWatcher> watchers =
        new java.util.ArrayList<AttributeWatcher>();

    /** El numero de secuencia de los avisos. */
    private long sequence = 1;

    /** El agente donde se registro, o null. Ver {@link #getClassLoaderRepository}. */
    private MBeanServer server;

    /** Sin nada configurado; hay que llamar a {@link #setModelMBeanInfo} antes de registrarlo. */
    public RequiredModelMBean() throws MBeanException, RuntimeOperationsException {
    }

    /**
     * Con la descripcion ya puesta.
     *
     * @throws RuntimeOperationsException si es null
     */
    public RequiredModelMBean(ModelMBeanInfo mbi)
        throws MBeanException, RuntimeOperationsException {
        setModelMBeanInfo(mbi);
    }

    /**
     * Que exponer.
     *
     * @throws RuntimeOperationsException si es null
     */
    public void setModelMBeanInfo(ModelMBeanInfo mbi)
        throws MBeanException, RuntimeOperationsException {
        if (mbi == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "ModelMBeanInfo is null"));
        }
        this.modelMBeanInfo = (ModelMBeanInfo) mbi.clone();
    }

    /**
     * De que objeto.
     *
     * @param mr_type el unico soportado es {@code "ObjectReference"}; ver
     *     {@link InvalidTargetObjectTypeException}
     * @throws RuntimeOperationsException si el objeto es null
     */
    public void setManagedResource(Object mr, String mr_type)
        throws MBeanException, RuntimeOperationsException, InstanceNotFoundException,
               InvalidTargetObjectTypeException {
        if (mr == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Managed resource is null"));
        }
        if (mr_type != null && !"objectreference".equalsIgnoreCase(mr_type)) {
            throw new InvalidTargetObjectTypeException(mr_type);
        }
        this.managedResource = mr;
    }

    /**
     * Recupera el estado persistido.
     *
     * @throws MBeanException siempre en KajiLibrary; ver la nota de la clase
     */
    public void load() throws MBeanException, RuntimeOperationsException, InstanceNotFoundException {
        throw new MBeanException(new UnsupportedOperationException(
            "KajiLibrary does not implement ModelMBean persistence"));
    }

    /**
     * Persiste el estado.
     *
     * @throws MBeanException siempre en KajiLibrary
     */
    public void store()
        throws MBeanException, RuntimeOperationsException, InstanceNotFoundException {
        throw new MBeanException(new UnsupportedOperationException(
            "KajiLibrary does not implement ModelMBean persistence"));
    }

    /** La descripcion configurada. */
    public MBeanInfo getMBeanInfo() {
        if (this.modelMBeanInfo == null) {
            return new MBeanInfo(getClass().getName(), "", null, null, null, null);
        }
        return (MBeanInfo) ((ModelMBeanInfoSupport) this.modelMBeanInfo).clone();
    }

    /**
     * Invoca una operacion sobre el objeto administrado.
     *
     * <p>Ver la nota de la clase sobre como se resuelve el metodo.
     *
     * @throws MBeanException si la operacion no esta declarada
     * @throws ReflectionException si el metodo que nombra el descriptor no existe o falla
     */
    public Object invoke(String opName, Object[] opArgs, String[] sig)
        throws MBeanException, ReflectionException {
        if (opName == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Operation name is null"));
        }
        require();
        ModelMBeanOperationInfo info;
        try {
            info = this.modelMBeanInfo.getOperation(opName);
        } catch (Exception e) {
            throw new MBeanException(e);
        }
        if (info == null) {
            throw new MBeanException(new ServiceNotFound("No such operation: " + opName));
        }
        String methodName = fieldOrDefault(info, "name", opName);
        try {
            Class<?>[] types = resolveSignature(sig);
            Method m = this.managedResource.getClass().getMethod(methodName, types);
            return m.invoke(this.managedResource, opArgs);
        } catch (Exception e) {
            throw new ReflectionException(e, "Cannot invoke " + methodName);
        }
    }

    /**
     * Lee un atributo del objeto administrado.
     *
     * @throws AttributeNotFoundException si no esta declarado, o si no tiene {@code getMethod}
     */
    public Object getAttribute(String attrName)
        throws AttributeNotFoundException, MBeanException, ReflectionException {
        if (attrName == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute name is null"));
        }
        require();
        ModelMBeanAttributeInfo info;
        try {
            info = this.modelMBeanInfo.getAttribute(attrName);
        } catch (Exception e) {
            throw new MBeanException(e);
        }
        if (info == null) {
            throw new AttributeNotFoundException("No such attribute: " + attrName);
        }
        String getter = field(info, "getMethod");
        if (getter == null) {
            throw new AttributeNotFoundException(
                "Attribute " + attrName + " has no getMethod descriptor field");
        }
        try {
            Method m = this.managedResource.getClass().getMethod(getter, new Class<?>[0]);
            return m.invoke(this.managedResource, new Object[0]);
        } catch (Exception e) {
            throw new ReflectionException(e, "Cannot read " + attrName);
        }
    }

    /**
     * Lee varios.
     *
     * <p>Los que fallan se <b>omiten</b> en vez de tirar. Es lo que pide el contrato de
     * {@code DynamicMBean}: una consola que pide veinte atributos no puede quedarse sin ninguno
     * porque uno solo falle.
     */
    public AttributeList getAttributes(String[] attrNames) {
        AttributeList out = new AttributeList();
        if (attrNames == null) {
            return out;
        }
        int i = 0;
        while (i < attrNames.length) {
            try {
                out.add(new Attribute(attrNames[i], getAttribute(attrNames[i])));
            } catch (Exception e) {
                // Omitido a proposito; ver la nota del metodo.
            }
            i = i + 1;
        }
        return out;
    }

    /**
     * Escribe un atributo, y avisa el cambio.
     *
     * <p>El aviso sale <b>despues</b> de escribir y solo si escribir anduvo, con el valor viejo y el
     * nuevo. Es lo que hace que un oyente pueda confiar en que el cambio ocurrio.
     *
     * @throws AttributeNotFoundException si no esta declarado, o si no tiene {@code setMethod}
     */
    public void setAttribute(Attribute attribute)
        throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException,
               ReflectionException {
        if (attribute == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute is null"));
        }
        require();
        String attrName = attribute.getName();
        ModelMBeanAttributeInfo info;
        try {
            info = this.modelMBeanInfo.getAttribute(attrName);
        } catch (Exception e) {
            throw new MBeanException(e);
        }
        if (info == null) {
            throw new AttributeNotFoundException("No such attribute: " + attrName);
        }
        String setter = field(info, "setMethod");
        if (setter == null) {
            throw new AttributeNotFoundException(
                "Attribute " + attrName + " has no setMethod descriptor field");
        }
        Object oldValue = null;
        try {
            oldValue = getAttribute(attrName);
        } catch (Exception e) {
            // Sin valor viejo el aviso sale igual, con null: no poder leerlo no impide escribir.
        }
        try {
            Method[] all = this.managedResource.getClass().getMethods();
            Method chosen = null;
            int i = 0;
            while (i < all.length) {
                if (all[i].getName().equals(setter) && all[i].getParameterCount() == 1) {
                    chosen = all[i];
                    break;
                }
                i = i + 1;
            }
            if (chosen == null) {
                throw new NoSuchMethodException(setter);
            }
            chosen.invoke(this.managedResource, new Object[] {attribute.getValue()});
        } catch (Exception e) {
            throw new ReflectionException(e, "Cannot write " + attrName);
        }
        sendAttributeChangeNotification(new Attribute(attrName, oldValue), attribute);
    }

    /** Escribe varios; los que fallan se omiten, igual que al leer. */
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList out = new AttributeList();
        if (attributes == null) {
            return out;
        }
        for (Object o : attributes) {
            if (!(o instanceof Attribute)) {
                continue;
            }
            try {
                setAttribute((Attribute) o);
                out.add(o);
            } catch (Exception e) {
                // Omitido a proposito.
            }
        }
        return out;
    }

    // ---- canal comun -----------------------------------------------------------------------

    /** Registra un oyente para los avisos comunes. */
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter,
                                        Object handback)
        throws IllegalArgumentException {
        this.general.addNotificationListener(listener, filter, handback);
    }

    /** Lo da de baja. */
    public void removeNotificationListener(NotificationListener listener)
        throws ListenerNotFoundException {
        this.general.removeNotificationListener(listener);
    }

    /** Idem, con el filtro y el testigo exactos. */
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter,
                                           Object handback) throws ListenerNotFoundException {
        this.general.removeNotificationListener(listener, filter, handback);
    }

    /** Manda ese aviso por el canal comun. */
    public void sendNotification(Notification ntfyObj)
        throws MBeanException, RuntimeOperationsException {
        if (ntfyObj == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Notification is null"));
        }
        this.general.sendNotification(ntfyObj);
    }

    /** Idem, armando el aviso a partir del texto. */
    public void sendNotification(String ntfyText)
        throws MBeanException, RuntimeOperationsException {
        if (ntfyText == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Notification text is null"));
        }
        sendNotification(new Notification("jmx.modelmbean.generic", this, nextSequence(), ntfyText));
    }

    /** Lo que este MBean declara que manda por el canal comun. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (this.modelMBeanInfo == null) {
            return new MBeanNotificationInfo[0];
        }
        return this.modelMBeanInfo.getNotifications();
    }

    // ---- canal de cambios de atributo -------------------------------------------------------

    /**
     * Registra un oyente para los cambios de un atributo.
     *
     * @param attributeName cual; null significa todos
     * @throws IllegalArgumentException si el oyente es null
     */
    public void addAttributeChangeNotificationListener(NotificationListener inlistener,
                                                       String inAttributeName, Object inhandback)
        throws MBeanException, RuntimeOperationsException, IllegalArgumentException {
        if (inlistener == null) {
            throw new IllegalArgumentException("Listener to be registered is null");
        }
        synchronized (this.watchers) {
            this.watchers.add(new AttributeWatcher(inlistener, inAttributeName, inhandback));
        }
    }

    /**
     * Lo da de baja.
     *
     * @throws ListenerNotFoundException si no estaba registrado para ese atributo
     */
    public void removeAttributeChangeNotificationListener(NotificationListener inlistener,
                                                          String inAttributeName)
        throws MBeanException, RuntimeOperationsException, ListenerNotFoundException {
        if (inlistener == null) {
            throw new ListenerNotFoundException("Listener to be removed is null");
        }
        synchronized (this.watchers) {
            int i = 0;
            boolean removed = false;
            while (i < this.watchers.size()) {
                AttributeWatcher w = this.watchers.get(i);
                boolean sameName = (inAttributeName == null)
                    ? w.attribute == null : inAttributeName.equals(w.attribute);
                if (w.listener == inlistener && sameName) {
                    this.watchers.remove(i);
                    removed = true;
                } else {
                    i = i + 1;
                }
            }
            if (!removed) {
                throw new ListenerNotFoundException(
                    "Listener not registered for attribute " + inAttributeName);
            }
        }
    }

    /** Manda ese aviso a los oyentes del atributo que nombra. */
    public void sendAttributeChangeNotification(AttributeChangeNotification ntfyObj)
        throws MBeanException, RuntimeOperationsException {
        if (ntfyObj == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "AttributeChangeNotification is null"));
        }
        java.util.List<AttributeWatcher> copy;
        synchronized (this.watchers) {
            copy = new java.util.ArrayList<AttributeWatcher>(this.watchers);
        }
        String name = ntfyObj.getAttributeName();
        int i = 0;
        while (i < copy.size()) {
            AttributeWatcher w = copy.get(i);
            // Un oyente sin atributo escucha todos; ver la nota de la clase.
            if (w.attribute == null || w.attribute.equals(name)) {
                w.listener.handleNotification(ntfyObj, w.handback);
            }
            i = i + 1;
        }
    }

    /**
     * Idem, armando el aviso a partir del valor viejo y el nuevo.
     *
     * @throws RuntimeOperationsException si los dos atributos no tienen el mismo nombre
     */
    public void sendAttributeChangeNotification(Attribute inOldVal, Attribute inNewVal)
        throws MBeanException, RuntimeOperationsException {
        if (inOldVal == null || inNewVal == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute object passed in is null"));
        }
        if (!inOldVal.getName().equals(inNewVal.getName())) {
            throw new RuntimeOperationsException(new IllegalArgumentException(
                "Attribute names are not the same"));
        }
        Object newValue = inNewVal.getValue();
        String type = (newValue == null) ? "java.lang.Object" : newValue.getClass().getName();
        sendAttributeChangeNotification(new AttributeChangeNotification(this, nextSequence(),
            System.currentTimeMillis(), inNewVal.getName() + " changed from "
                + inOldVal.getValue() + " to " + newValue,
            inNewVal.getName(), type, inOldVal.getValue(), newValue));
    }

    // ---- MBeanRegistration -------------------------------------------------------------------

    /** Se queda con el agente --lo necesita {@link #getClassLoaderRepository}-- y acepta el nombre. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.server = server;
        return name;
    }

    /**
     * Los cargadores del agente donde este MBean esta registrado.
     *
     * <p>Es {@code protected} porque esta para las subclases: un model MBean que resuelve nombres de
     * clase --al invocar una operacion, al deserializar un argumento-- tiene que buscarlos donde el
     * agente busca, y no en el classpath de esta clase. Buscarlos aca daria el error clasico de JMX:
     * la clase existe en el agente y "no se encuentra".
     *
     * @return null si todavia no se registro en ningun agente
     */
    protected javax.management.loading.ClassLoaderRepository getClassLoaderRepository() {
        return (this.server == null) ? null : this.server.getClassLoaderRepository();
    }

    /** Nada que hacer. */
    public void postRegister(Boolean registrationDone) {
    }

    /** Nada que hacer. */
    public void preDeregister() throws Exception {
    }

    /** Nada que hacer. */
    public void postDeregister() {
    }

    // ---- adentro -----------------------------------------------------------------------------

    /** El proximo numero de secuencia. */
    private synchronized long nextSequence() {
        long seq = this.sequence;
        this.sequence = this.sequence + 1;
        return seq;
    }

    /** Que este configurado. */
    private void require() throws MBeanException {
        if (this.modelMBeanInfo == null) {
            throw new MBeanException(new IllegalStateException("ModelMBeanInfo is not set"));
        }
        if (this.managedResource == null) {
            throw new MBeanException(new IllegalStateException("Managed resource is not set"));
        }
    }

    /** El valor de ese campo del descriptor, o null. */
    private static String field(javax.management.DescriptorAccess info, String name) {
        Object v = info.getDescriptor().getFieldValue(name);
        return (v == null) ? null : v.toString();
    }

    /** Idem, con un valor por omision. */
    private static String fieldOrDefault(javax.management.DescriptorAccess info, String name,
                                         String fallback) {
        String v = field(info, name);
        return (v == null) ? fallback : v;
    }

    /** Los tipos de una firma, resueltos por nombre. */
    private static Class<?>[] resolveSignature(String[] sig) throws ClassNotFoundException {
        if (sig == null) {
            return new Class<?>[0];
        }
        Class<?>[] out = new Class<?>[sig.length];
        int i = 0;
        while (i < sig.length) {
            out[i] = primitiveOrClass(sig[i]);
            i = i + 1;
        }
        return out;
    }

    /** Un nombre de tipo, incluidos los primitivos, que {@code Class.forName} no resuelve. */
    private static Class<?> primitiveOrClass(String name) throws ClassNotFoundException {
        if ("int".equals(name)) {
            return int.class;
        }
        if ("long".equals(name)) {
            return long.class;
        }
        if ("boolean".equals(name)) {
            return boolean.class;
        }
        if ("byte".equals(name)) {
            return byte.class;
        }
        if ("char".equals(name)) {
            return char.class;
        }
        if ("short".equals(name)) {
            return short.class;
        }
        if ("float".equals(name)) {
            return float.class;
        }
        if ("double".equals(name)) {
            return double.class;
        }
        if ("void".equals(name)) {
            return void.class;
        }
        return Class.forName(name);
    }

    /** Un oyente del canal de cambios, con el atributo que escucha. */
    private static final class AttributeWatcher {

        private final NotificationListener listener;

        /** Null significa todos. */
        private final String attribute;

        private final Object handback;

        AttributeWatcher(NotificationListener listener, String attribute, Object handback) {
            this.listener = listener;
            this.attribute = attribute;
            this.handback = handback;
        }
    }

    /** Lo que se envuelve cuando la operacion pedida no esta declarada. */
    private static final class ServiceNotFound extends Exception {

        private static final long serialVersionUID = 1L;

        ServiceNotFound(String message) {
            super(message);
        }
    }
}
