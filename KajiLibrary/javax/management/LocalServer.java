package javax.management;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.management.loading.ClassLoaderRepository;
import javax.management.loading.PrivateClassLoader;

/**
 * La implementacion de {@link MBeanServer} que devuelven {@link MBeanServerFactory} y
 * {@link MBeanServerBuilder}.
 *
 * <p>Es paquete-privada a proposito, igual que en el JDK: la especificacion dice que el agente se
 * obtiene por la fabrica y nunca con `new`, porque la fabrica es la que lleva el registro que hace
 * funcionar a `findMBeanServer`. Exponer la clase invitaria a saltearse ese registro.
 *
 * <h2>Como despacha</h2>
 *
 * <p>Todo lo que se registra se guarda como {@link DynamicMBean}. Los MBeans estandar se envuelven
 * en un {@link StandardMBean} al registrarse, y de ahi en adelante el agente no distingue: leer un
 * atributo es siempre `dynamic.getAttribute(name)`. Es lo que evita tener dos caminos de despacho
 * --uno reflexivo y otro dinamico-- que habria que mantener en paralelo.
 *
 * <h2>Que no hace</h2>
 *
 * <ul>
 *   <li><b>No hay repositorio de cargadores.</b> `javax.management.loading` no esta en esta
 *       biblioteca, y por eso {@link #getClassLoader} y {@link #getClassLoaderFor} contestan con
 *       cargadores reales pero las variantes de `createMBean`/`instantiate` que reciben un
 *       `loaderName` exigen que ese nombre este registrado y sea un `ClassLoader`. No se inventa un
 *       repositorio que no existe;
 *   <li><b>no hay control de acceso.</b> Los {@link MBeanPermission} se pueden construir y
 *       comparar, pero este agente no los consulta. Consultarlos a medias --unos si y otros no--
 *       seria peor que no consultarlos: daria una sensacion de estar protegido que no se cumple.
 * </ul>
 */
class LocalServer implements MBeanServer {

    /** Un MBean registrado: lo que se despacha, lo que se publica y el objeto de verdad. */
    private static class Entry {
        final DynamicMBean dynamic;
        final Object object;
        final ObjectInstance instance;

        Entry(DynamicMBean dynamic, Object object, ObjectInstance instance) {
            this.dynamic = dynamic;
            this.object = object;
            this.instance = instance;
        }
    }

    /**
     * Se conserva el orden de alta: `queryNames` sin patron devuelve los MBeans, y un orden estable
     * hace que dos corridas del mismo programa den lo mismo.
     */
    private final Map<ObjectName, Entry> registry = new LinkedHashMap<ObjectName, Entry>();

    /** El repositorio de cargadores; ver {@link #getClassLoaderRepository}. */
    private final LoaderRepository loaders = new LoaderRepository();

    /**
     * Los envoltorios que este agente le puso a los oyentes, para poder devolver <b>el mismo</b>.
     *
     * <p>Hace falta porque el emisor compara oyentes por identidad --lo hace el JDK y esta bien:
     * dos oyentes iguales pero distintos son dos registros distintos--. Si cada `remove` armara un
     * envoltorio nuevo, nunca coincidiria con el que puso el `add` y no se podria sacar a nadie.
     */
    private final List<SourceRewriter> wrappers = new ArrayList<SourceRewriter>();

    private final String defaultDomainName;
    private final MBeanServerDelegate delegateMBean;

    /**
     * El agente que ven los MBeans en `preRegister`.
     *
     * <p>Casi siempre es `this`, pero puede ser otro: es lo que permite envolver un agente en otro
     * --para auditar, para replicar-- sin que los MBeans registrados se enteren y se salteen el
     * envoltorio guardandose la referencia que recibieron.
     */
    private final MBeanServer visibleName;

    LocalServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate) {
        this.defaultDomainName = (defaultDomain == null) ? "DefaultDomain" : defaultDomain;
        this.visibleName = (outer == null) ? this : outer;
        this.delegateMBean = (delegate == null) ? new MBeanServerDelegate() : delegate;
        try {
            // El delegado se registra a si mismo: es el unico MBean que el agente tiene garantizado
            // desde el momento cero, y es donde se escuchan las altas de todos los demas.
            registerInternal(this.delegateMBean, MBeanServerDelegate.DELEGATE_NAME, false);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo registrar el delegado", e);
        }
    }

    // ---- alta y baja --------------------------------------------------------------------------

    public ObjectInstance registerMBean(Object object, ObjectName name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
                   NotCompliantMBeanException {
        if (object == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("El objeto no puede ser null"));
        }
        return registerInternal(object, name, true);
    }

    private synchronized ObjectInstance registerInternal(Object object, ObjectName name,
                                                         boolean notifyListeners)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
                   NotCompliantMBeanException {
        DynamicMBean dynamic = wrap(object);

        ObjectName chosen = name;
        // `preRegister` corre **antes** de mirar si el nombre esta libre porque es el que puede
        // elegir el nombre: un MBean que se autonombra recibe `null` y devuelve el suyo.
        if (object instanceof MBeanRegistration) {
            try {
                chosen = ((MBeanRegistration) object).preRegister(visibleName, name);
            } catch (Exception e) {
                throw new MBeanRegistrationException(e, "preRegister fallo");
            }
        }
        if (chosen == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("No se dio nombre y el MBean no eligio uno"));
        }
        if (chosen.isPattern()) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Un MBean no se registra bajo un patron: " + chosen));
        }

        boolean done = false;
        try {
            if (registry.containsKey(chosen)) {
                throw new InstanceAlreadyExistsException(chosen.toString());
            }
            String type = dynamic.getMBeanInfo().getClassName();
            registry.put(chosen, new Entry(dynamic, object,
                                              new ObjectInstance(chosen, type)));
            done = true;
        } finally {
            // Se avisa siempre, con `false` si no se pudo: el MBean tiene que poder deshacer lo que
            // haya preparado en `preRegister`.
            if (object instanceof MBeanRegistration) {
                ((MBeanRegistration) object).postRegister(Boolean.valueOf(done));
            }
        }

        if (notifyListeners) {
            delegateMBean.sendNotification(new MBeanServerNotification(
                MBeanServerNotification.REGISTRATION_NOTIFICATION, delegateMBean, 0L, chosen));
        }
        return registry.get(chosen).instance;
    }

    /**
     * Todo entra como {@link DynamicMBean}.
     *
     * <p>Un MBean estandar se envuelve en {@link StandardMBean} sin decirle la interfaz: la busca
     * por la convencion `<Clase>MBean`, que es exactamente lo que define a un MBean estandar.
     */
    private static DynamicMBean wrap(Object object) throws NotCompliantMBeanException {
        if (object instanceof DynamicMBean) {
            return (DynamicMBean) object;
        }
        try {
            return new StandardMBean(object, null);
        } catch (IllegalArgumentException e) {
            throw new NotCompliantMBeanException(
                object.getClass().getName()
                + " no es un DynamicMBean y no tiene interfaz <Clase>MBean");
        }
    }

    public synchronized void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException {
        Entry e = require(name);
        if (name.equals(MBeanServerDelegate.DELEGATE_NAME)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("El delegado no se puede desregistrar"));
        }
        if (e.object instanceof MBeanRegistration) {
            try {
                ((MBeanRegistration) e.object).preDeregister();
            } catch (Exception x) {
                throw new MBeanRegistrationException(x, "preDeregister fallo");
            }
        }
        registry.remove(name);
        if (e.object instanceof MBeanRegistration) {
            ((MBeanRegistration) e.object).postDeregister();
        }
        delegateMBean.sendNotification(new MBeanServerNotification(
            MBeanServerNotification.UNREGISTRATION_NOTIFICATION, delegateMBean, 0L, name));
    }

    private Entry require(ObjectName name) throws InstanceNotFoundException {
        if (name == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("El ObjectName no puede ser null"));
        }
        Entry e = registry.get(name);
        if (e == null) {
            throw new InstanceNotFoundException(name.toString());
        }
        return e;
    }

    // ---- consulta -----------------------------------------------------------------------------

    public synchronized ObjectInstance getObjectInstance(ObjectName name)
            throws InstanceNotFoundException {
        return require(name).instance;
    }

    public synchronized boolean isRegistered(ObjectName name) {
        return name != null && registry.containsKey(name);
    }

    public synchronized Integer getMBeanCount() {
        return Integer.valueOf(registry.size());
    }

    public synchronized Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        Set<ObjectInstance> r = new HashSet<ObjectInstance>();
        for (ObjectName n : matches(name, query)) {
            r.add(registry.get(n).instance);
        }
        return r;
    }

    public synchronized Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return new HashSet<ObjectName>(matches(name, query));
    }

    /**
     * El filtro en dos pasos: primero el patron del nombre, despues la expresion.
     *
     * <p>El orden importa por costo: el patron se resuelve sobre texto ya normalizado, la expresion
     * puede tener que leer atributos del MBean. Filtrar por nombre primero achica el conjunto sobre
     * el que se paga lo caro.
     */
    private List<ObjectName> matches(ObjectName pattern, QueryExp query) {
        List<ObjectName> r = new ArrayList<ObjectName>();
        if (query != null) {
            // La expresion puede necesitar consultar atributos, y para eso precisa el agente.
            query.setMBeanServer(visibleName);
        }
        for (ObjectName n : registry.keySet()) {
            if (pattern != null && !pattern.apply(n)) {
                continue;
            }
            if (query != null) {
                try {
                    if (!query.apply(n)) {
                        continue;
                    }
                } catch (Exception e) {
                    // Una expresion que falla sobre un MBean lo descarta y no tumba la consulta:
                    // preguntar por un atributo que este MBean no tiene es normal en una query
                    // hecha sobre un dominio heterogeneo.
                    continue;
                }
            }
            r.add(n);
        }
        return r;
    }

    public String getDefaultDomain() {
        return defaultDomainName;
    }

    public synchronized String[] getDomains() {
        Set<String> d = new TreeSet<String>();
        for (ObjectName n : registry.keySet()) {
            d.add(n.getDomain());
        }
        return d.toArray(new String[0]);
    }

    // ---- despacho -----------------------------------------------------------------------------

    public Object getAttribute(ObjectName name, String attribute)
            throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                   ReflectionException {
        return require(name).dynamic.getAttribute(attribute);
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException {
        return require(name).dynamic.getAttributes(attributes);
    }

    public void setAttribute(ObjectName name, Attribute attribute)
            throws InstanceNotFoundException, AttributeNotFoundException,
                   InvalidAttributeValueException, MBeanException, ReflectionException {
        require(name).dynamic.setAttribute(attribute);
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException {
        return require(name).dynamic.setAttributes(attributes);
    }

    public Object invoke(ObjectName name, String operationName, Object[] params,
                         String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException {
        return require(name).dynamic.invoke(operationName, params, signature);
    }

    public MBeanInfo getMBeanInfo(ObjectName name)
            throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return require(name).dynamic.getMBeanInfo();
    }

    /**
     * Contra la clase del objeto administrado, no contra la del envoltorio.
     *
     * <p>Es lo que dice la especificacion y es lo unico util: quien pregunta quiere saber si el
     * recurso es de ese tipo, no si el agente lo envolvio en un `StandardMBean`.
     */
    public boolean isInstanceOf(ObjectName name, String className)
            throws InstanceNotFoundException {
        Entry e = require(name);
        Object target = (e.dynamic instanceof StandardMBean)
                ? ((StandardMBean) e.dynamic).getImplementation() : e.object;
        Class<?> c = target.getClass();
        while (c != null) {
            if (c.getName().equals(className) || implementsInterface(c, className)) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private static boolean implementsInterface(Class<?> c, String name) {
        for (Class<?> i : c.getInterfaces()) {
            if (i.getName().equals(name) || implementsInterface(i, name)) {
                return true;
            }
        }
        return false;
    }

    // ---- notificaciones ------------------------------------------------------------------------

    /**
     * Registra un oyente contra el MBean, envolviendolo para reescribir la fuente.
     *
     * <p>La reescritura es la parte que la especificacion pide y que no se ve: el MBean pone en
     * `source` el objeto, y el que escucha a traves del agente tiene que ver el {@link ObjectName}.
     * Se hace sobre una copia --no sobre la notificacion que el MBean emitio-- porque esa
     * notificacion la comparten todos los oyentes, incluidos los registrados directo contra el
     * MBean, que si tienen que ver el objeto.
     */
    public void addNotificationListener(ObjectName name, NotificationListener listener,
                                        NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        Object o = requireBroadcaster(name);
        ((NotificationBroadcaster) o).addNotificationListener(
            wrapper(listener, name), filter, handback);
    }

    /** El envoltorio de ese par oyente/nombre; se crea la primera vez y despues se reusa. */
    private synchronized SourceRewriter wrapper(NotificationListener l, ObjectName n) {
        SourceRewriter wanted = new SourceRewriter(l, n);
        for (int i = 0; i < wrappers.size(); i++) {
            if (wrappers.get(i).equals(wanted)) {
                return wrappers.get(i);
            }
        }
        wrappers.add(wanted);
        return wanted;
    }

    /**
     * El envoltorio que ya existe, o nada.
     *
     * @param olvidar si ademas hay que sacarlo de la tabla: para el `remove` que saca **todos** los
     *        registros de ese oyente, despues del cual el envoltorio ya no le sirve a nadie
     */
    private synchronized SourceRewriter existingWrapper(NotificationListener l, ObjectName n,
                                                             boolean forget)
            throws ListenerNotFoundException {
        SourceRewriter wanted = new SourceRewriter(l, n);
        for (int i = 0; i < wrappers.size(); i++) {
            if (wrappers.get(i).equals(wanted)) {
                SourceRewriter r = wrappers.get(i);
                if (forget) {
                    wrappers.remove(i);
                }
                return r;
            }
        }
        throw new ListenerNotFoundException("Ese oyente no esta registrado contra " + n);
    }

    /** El oyente esta registrado como MBean: se busca y se usa. */
    public void addNotificationListener(ObjectName name, ObjectName listener,
                                        NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        Object l = require(listener).object;
        if (!(l instanceof NotificationListener)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(listener + " no es un NotificationListener"));
        }
        addNotificationListener(name, (NotificationListener) l, filter, handback);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException {
        Object l = require(listener).object;
        if (!(l instanceof NotificationListener)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(listener + " no es un NotificationListener"));
        }
        removeNotificationListener(name, (NotificationListener) l);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
                                           NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        Object l = require(listener).object;
        if (!(l instanceof NotificationListener)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(listener + " no es un NotificationListener"));
        }
        removeNotificationListener(name, (NotificationListener) l, filter, handback);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener)
            throws InstanceNotFoundException, ListenerNotFoundException {
        Object o = requireBroadcaster(name);
        ((NotificationBroadcaster) o).removeNotificationListener(
            existingWrapper(listener, name, true));
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener,
                                           NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        Object o = requireBroadcaster(name);
        if (!(o instanceof NotificationEmitter)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(name + " no es un NotificationEmitter"));
        }
        // Sin `forget`: el mismo oyente puede tener otros registros con otro filtro, y esos
        // siguen necesitando este envoltorio.
        ((NotificationEmitter) o).removeNotificationListener(
            existingWrapper(listener, name, false), filter, handback);
    }

    private Object requireBroadcaster(ObjectName name) throws InstanceNotFoundException {
        Entry e = require(name);
        Object o = (e.dynamic instanceof NotificationBroadcaster) ? e.dynamic : e.object;
        if (!(o instanceof NotificationBroadcaster)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(name + " no emite notificaciones"));
        }
        return o;
    }

    /**
     * El envoltorio que pone el {@link ObjectName} en la fuente.
     *
     * <p>Su `equals`/`hashCode` miran el oyente y el nombre, no la identidad del envoltorio: es lo
     * que hace que la tabla `wrappers` pueda encontrar el que ya existe a partir del par
     * oyente/nombre que trae el `remove`.
     */
    private static class SourceRewriter implements NotificationListener {
        private final NotificationListener target;
        private final ObjectName name;

        SourceRewriter(NotificationListener target, ObjectName name) {
            this.target = target;
            this.name = name;
        }

        public void handleNotification(Notification n, Object handback) {
            n.setSource(name);
            target.handleNotification(n, handback);
        }

        public boolean equals(Object o) {
            if (!(o instanceof SourceRewriter)) {
                return false;
            }
            SourceRewriter q = (SourceRewriter) o;
            return target == q.target && name.equals(q.name);
        }

        public int hashCode() {
            return System.identityHashCode(target) * 31 + name.hashCode();
        }
    }

    // ---- instanciacion -------------------------------------------------------------------------

    public Object instantiate(String className) throws ReflectionException, MBeanException {
        // El `catch` es inalcanzable con `loaderName` nulo, pero la firma sin `loaderName` no
        // declara la excepcion y hay que absorberla en algun lado.
        try {
            return instantiate(className, (ObjectName) null, null, null);
        } catch (InstanceNotFoundException e) {
            throw new ReflectionException(e);
        }
    }

    public Object instantiate(String className, ObjectName loaderName)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        return instantiate(className, loaderName, null, null);
    }

    public Object instantiate(String className, Object[] params, String[] signature)
            throws ReflectionException, MBeanException {
        try {
            return instantiate(className, (ObjectName) null, params, signature);
        } catch (InstanceNotFoundException e) {
            throw new ReflectionException(e);
        }
    }

    public Object instantiate(String className, ObjectName loaderName, Object[] params,
                              String[] signature)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        ClassLoader cl = loaderOf(loaderName);
        Class<?> c;
        try {
            c = (cl == null) ? Class.forName(className) : Class.forName(className, true, cl);
        } catch (ClassNotFoundException e) {
            throw new ReflectionException(e, "No se encontro la clase " + className);
        }
        String[] wanted = (signature == null) ? new String[0] : signature;
        Object[] args = (params == null) ? new Object[0] : params;
        try {
            Constructor<?> ct = constructor(c, wanted);
            return ct.newInstance(args);
        } catch (NoSuchMethodException e) {
            throw new ReflectionException(e, "No hay constructor con esa firma en " + className);
        } catch (InstantiationException e) {
            throw new ReflectionException(e, "No se pudo instanciar " + className);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e, "Constructor inaccesible en " + className);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException) {
                throw new RuntimeMBeanException((RuntimeException) cause, "El constructor tiro");
            }
            if (cause instanceof Error) {
                throw new RuntimeErrorException((Error) cause, "El constructor tiro");
            }
            throw new MBeanException((Exception) cause, "El constructor tiro");
        }
    }

    private static Constructor<?> constructor(Class<?> c, String[] signature)
            throws NoSuchMethodException, ReflectionException {
        Class<?>[] types = new Class<?>[signature.length];
        for (int i = 0; i < signature.length; i++) {
            types[i] = primitiveOrClass(signature[i], c.getClassLoader());
        }
        return c.getConstructor(types);
    }

    /**
     * La firma de JMX es un arreglo de nombres de tipo, y ahi `int` no es una clase que se cargue.
     * Hay que traducir los ocho a mano; no hay otra via.
     */
    private static Class<?> primitiveOrClass(String name, ClassLoader cl)
            throws ReflectionException {
        if (name.equals("int")) return Integer.TYPE;
        if (name.equals("long")) return Long.TYPE;
        if (name.equals("boolean")) return Boolean.TYPE;
        if (name.equals("byte")) return Byte.TYPE;
        if (name.equals("short")) return Short.TYPE;
        if (name.equals("char")) return Character.TYPE;
        if (name.equals("float")) return Float.TYPE;
        if (name.equals("double")) return Double.TYPE;
        if (name.equals("void")) return Void.TYPE;
        try {
            return (cl == null) ? Class.forName(name) : Class.forName(name, false, cl);
        } catch (ClassNotFoundException e) {
            throw new ReflectionException(e, "No se encontro el tipo " + name);
        }
    }

    private ClassLoader loaderOf(ObjectName loaderName) throws InstanceNotFoundException {
        if (loaderName == null) {
            return null;
        }
        Object o = require(loaderName).object;
        if (!(o instanceof ClassLoader)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(loaderName + " no es un ClassLoader"));
        }
        return (ClassLoader) o;
    }

    // ---- createMBean = instantiate + registerMBean ----------------------------------------------

    public ObjectInstance createMBean(String className, ObjectName name)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException {
        return registerMBean(instantiate(className), name);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return registerMBean(instantiate(className, loaderName), name);
    }

    public ObjectInstance createMBean(String className, ObjectName name, Object[] params,
                                      String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException {
        return registerMBean(instantiate(className, params, signature), name);
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName,
                                      Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException,
                   MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return registerMBean(instantiate(className, loaderName, params, signature), name);
    }

    // ---- cargadores ------------------------------------------------------------------------------

    /** El del objeto administrado; sirve para cargar clases en su mismo contexto. */
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        Entry e = require(mbeanName);
        Object target = (e.dynamic instanceof StandardMBean)
                ? ((StandardMBean) e.dynamic).getImplementation() : e.object;
        return target.getClass().getClassLoader();
    }

    /**
     * El MBean <b>es</b> un cargador.
     *
     * <p>El caso `null` esta en la especificacion y significa el cargador del propio agente.
     */
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        if (loaderName == null) {
            return getClass().getClassLoader();
        }
        Object o = require(loaderName).object;
        if (!(o instanceof ClassLoader)) {
            throw new InstanceNotFoundException(loaderName + " no es un ClassLoader");
        }
        return (ClassLoader) o;
    }

    /**
     * El repositorio de cargadores de este agente.
     *
     * <p>La lista se arma <b>en cada consulta</b> y no se guarda. Tiene que ser asi: un cargador se
     * registra como cualquier otro MBean, en cualquier momento, y un repositorio congelado al
     * arrancar no veria ninguno.
     *
     * <p>Va primero el cargador del propio agente y despues los MBeans que son cargadores, en orden
     * de registro. Los que implementan {@link PrivateClassLoader} quedan afuera, que es todo el
     * sentido de esa marca.
     */
    public ClassLoaderRepository getClassLoaderRepository() {
        return this.loaders;
    }

    /** La vista de los cargadores registrados. Ver {@link #getClassLoaderRepository}. */
    private final class LoaderRepository implements ClassLoaderRepository {

        /** El agente primero, despues los registrados; los privados no entran. */
        private List<ClassLoader> current() {
            List<ClassLoader> found = new ArrayList<ClassLoader>();
            found.add(LocalServer.class.getClassLoader());
            synchronized (LocalServer.this.registry) {
                for (Entry e : LocalServer.this.registry.values()) {
                    if (e.object instanceof ClassLoader
                            && !(e.object instanceof PrivateClassLoader)) {
                        found.add((ClassLoader) e.object);
                    }
                }
            }
            return found;
        }

        public Class<?> loadClass(String className) throws ClassNotFoundException {
            return search(current(), className);
        }

        public Class<?> loadClassWithout(ClassLoader exclude, String className)
                throws ClassNotFoundException {
            List<ClassLoader> pool = current();
            pool.remove(exclude);
            return search(pool, className);
        }

        public Class<?> loadClassBefore(ClassLoader stop, String className)
                throws ClassNotFoundException {
            List<ClassLoader> pool = current();
            int cut = pool.indexOf(stop);
            // Si no esta en la lista no hay donde cortar, y se buscan todos.
            if (cut >= 0) {
                pool = pool.subList(0, cut);
            }
            return search(pool, className);
        }

        /** El recorrido comun: el primero que la tenga gana. */
        private Class<?> search(List<ClassLoader> pool, String className)
                throws ClassNotFoundException {
            for (ClassLoader cl : pool) {
                try {
                    return Class.forName(className, false, cl);
                } catch (ClassNotFoundException e) {
                    // Este no la tiene; sigue el que viene.
                }
            }
            throw new ClassNotFoundException(className);
        }
    }
}
