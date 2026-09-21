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
 * The {@link MBeanServer} implementation {@link MBeanServerFactory} and {@link MBeanServerBuilder}
 * return.
 *
 * <p>It is package-private on purpose, as in the JDK: the specification says the agent is obtained
 * through the factory and never with {@code new}, because the factory is the one that keeps the
 * registry that makes {@code findMBeanServer} work. Exposing the class would invite skipping that
 * registry.
 *
 * <h2>How it dispatches</h2>
 *
 * <p>Everything registered is kept as a {@link DynamicMBean}. Standard MBeans are wrapped in a
 * {@link StandardMBean} when registered, and from then on the agent does not distinguish: reading
 * an attribute is always {@code dynamic.getAttribute(name)}. It is what avoids having two dispatch
 * paths --a reflective one and a dynamic one-- that would have to be maintained in parallel.
 *
 * <h2>What it does not do</h2>
 *
 * <ul>
 *   <li><b>no access control.</b> {@link MBeanPermission}s can be built and compared, but this
 *       agent does not consult them. Consulting them halfway --some yes and some no-- would be
 *       worse than not consulting them: it would give a feeling of protection that does not hold.
 * </ul>
 *
 * <p>(An earlier note also listed a missing class loader repository, saying
 * {@code javax.management.loading} was not in this library. The package is here, and
 * {@link #getClassLoaderRepository} builds a real repository from the registered loaders.)
 */
class LocalServer implements MBeanServer {

    /** A registered MBean: what is dispatched to, what is published and the real object. */
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
     * Registration order is kept: {@code queryNames} without a pattern returns the MBeans, and a
     * stable order makes two runs of the same program give the same result.
     */
    private final Map<ObjectName, Entry> registry = new LinkedHashMap<ObjectName, Entry>();

    /** The class loader repository; see {@link #getClassLoaderRepository}. */
    private final LoaderRepository loaders = new LoaderRepository();

    /**
     * The wrappers this agent put on the listeners, so as to be able to return <b>the same one</b>.
     *
     * <p>It is needed because the emitter compares listeners by identity --the JDK does and it is
     * right: two equal but distinct listeners are two different registrations. If every {@code
     * remove} built a new wrapper, it would never match the one the {@code add} put in and nobody
     * could be removed.
     */
    private final List<SourceRewriter> wrappers = new ArrayList<SourceRewriter>();

    private final String defaultDomainName;
    private final MBeanServerDelegate delegateMBean;

    /**
     * The agent the MBeans see in {@code preRegister}.
     *
     * <p>It is almost always {@code this}, but it can be another: it is what allows wrapping one
     * agent in another --to audit, to replicate-- without the registered MBeans noticing and
     * bypassing the wrapper by keeping the reference they received.
     */
    private final MBeanServer visibleName;

    LocalServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate) {
        this.defaultDomainName = (defaultDomain == null) ? "DefaultDomain" : defaultDomain;
        this.visibleName = (outer == null) ? this : outer;
        this.delegateMBean = (delegate == null) ? new MBeanServerDelegate() : delegate;
        try {
            // The delegate registers itself: it is the only MBean the agent has guaranteed from
            // moment zero, and it is where registrations of all the others are listened to.
            registerInternal(this.delegateMBean, MBeanServerDelegate.DELEGATE_NAME, false);
        } catch (Exception e) {
            throw new IllegalStateException("Could not register the delegate", e);
        }
    }

    // ---- registration and unregistration -----------------------------------------------------

    public ObjectInstance registerMBean(Object object, ObjectName name)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
                   NotCompliantMBeanException {
        if (object == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("The object cannot be null"));
        }
        return registerInternal(object, name, true);
    }

    private synchronized ObjectInstance registerInternal(Object object, ObjectName name,
                                                         boolean notifyListeners)
            throws InstanceAlreadyExistsException, MBeanRegistrationException,
                   NotCompliantMBeanException {
        DynamicMBean dynamic = wrap(object);

        ObjectName chosen = name;
        // `preRegister` runs *before* checking whether the name is free because it is the one
        // that may choose the name: an MBean that names itself receives `null` and returns its own.
        if (object instanceof MBeanRegistration) {
            try {
                chosen = ((MBeanRegistration) object).preRegister(visibleName, name);
            } catch (Exception e) {
                throw new MBeanRegistrationException(e, "preRegister fallo");
            }
        }
        if (chosen == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("No name was given and the MBean did not choose one"));
        }
        if (chosen.isPattern()) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(
                    "An MBean is not registered under a pattern: " + chosen));
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
            // It is always told, with `false` if it could not be done: the MBean has to be able to
            // undo whatever it prepared in `preRegister`.
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
     * Everything goes in as a {@link DynamicMBean}.
     *
     * <p>A standard MBean is wrapped in {@link StandardMBean} without telling it the interface: it
     * looks for it by the {@code <Class>MBean} convention, which is exactly what defines a standard
     * MBean.
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
                + " is not a DynamicMBean and has no <Class>MBean interface");
        }
    }

    public synchronized void unregisterMBean(ObjectName name)
            throws InstanceNotFoundException, MBeanRegistrationException {
        Entry e = require(name);
        if (name.equals(MBeanServerDelegate.DELEGATE_NAME)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("The delegate cannot be unregistered"));
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
                new IllegalArgumentException("The ObjectName cannot be null"));
        }
        Entry e = registry.get(name);
        if (e == null) {
            throw new InstanceNotFoundException(name.toString());
        }
        return e;
    }

    // ---- queries ------------------------------------------------------------------------------

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
     * The two-step filter: first the name pattern, then the expression.
     *
     * <p>The order matters for cost: the pattern is resolved over already normalized text, the
     * expression may have to read MBean attributes. Filtering by name first shrinks the set over
     * which the expensive part is paid.
     */
    private List<ObjectName> matches(ObjectName pattern, QueryExp query) {
        List<ObjectName> r = new ArrayList<ObjectName>();
        if (query != null) {
            // The expression may need to query attributes, and for that it needs the agent.
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
                    // An expression that fails on an MBean discards it and does not bring the query
                    // down: asking for an attribute this MBean does not have is normal in a query
                    // made over a heterogeneous domain.
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

    // ---- dispatch -----------------------------------------------------------------------------

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
     * Against the class of the managed object, not against that of the wrapper.
     *
     * <p>It is what the specification says and the only useful thing: whoever asks wants to know
     * whether the resource is of that type, not whether the agent wrapped it in a
     * {@code StandardMBean}.
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

    // ---- notifications -------------------------------------------------------------------------

    /**
     * Registers a listener against the MBean, wrapping it to rewrite the source.
     *
     * <p>The rewriting is the part the specification asks for and that is not seen: the MBean puts
     * the object in {@code source}, and whoever listens through the agent has to see the {@link
     * ObjectName}. It is done on a copy --not on the notification the MBean emitted-- because that
     * notification is shared by all the listeners, including those registered directly against the
     * MBean, which do have to see the object.
     */
    public void addNotificationListener(ObjectName name, NotificationListener listener,
                                        NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        Object o = requireBroadcaster(name);
        ((NotificationBroadcaster) o).addNotificationListener(
            wrapper(listener, name), filter, handback);
    }

    /** The wrapper of that listener/name pair; created the first time and reused afterwards. */
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
     * The wrapper that already exists, or nothing.
     *
     * @param forget whether it also has to be removed from the table: for the {@code remove} that
     *        takes out <b>all</b> the registrations of that listener, after which the wrapper is of
     *        no use to anyone
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
        throw new ListenerNotFoundException("That listener is not registered against " + n);
    }

    /** The listener is registered as an MBean: it is looked up and used. */
    public void addNotificationListener(ObjectName name, ObjectName listener,
                                        NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        Object l = require(listener).object;
        if (!(l instanceof NotificationListener)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(listener + " is not a NotificationListener"));
        }
        addNotificationListener(name, (NotificationListener) l, filter, handback);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener)
            throws InstanceNotFoundException, ListenerNotFoundException {
        Object l = require(listener).object;
        if (!(l instanceof NotificationListener)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(listener + " is not a NotificationListener"));
        }
        removeNotificationListener(name, (NotificationListener) l);
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener,
                                           NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        Object l = require(listener).object;
        if (!(l instanceof NotificationListener)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(listener + " is not a NotificationListener"));
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
                new IllegalArgumentException(name + " is not a NotificationEmitter"));
        }
        // Without `forget`: the same listener may have other registrations with another filter, and
        // those still need this wrapper.
        ((NotificationEmitter) o).removeNotificationListener(
            existingWrapper(listener, name, false), filter, handback);
    }

    private Object requireBroadcaster(ObjectName name) throws InstanceNotFoundException {
        Entry e = require(name);
        Object o = (e.dynamic instanceof NotificationBroadcaster) ? e.dynamic : e.object;
        if (!(o instanceof NotificationBroadcaster)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(name + " does not emit notifications"));
        }
        return o;
    }

    /**
     * The wrapper that puts the {@link ObjectName} in the source.
     *
     * <p>Its {@code equals}/{@code hashCode} look at the listener and the name, not at the
     * wrapper's identity: it is what lets the {@code wrappers} table find the existing one from the
     * listener/name pair the {@code remove} brings.
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

    // ---- instantiation -------------------------------------------------------------------------

    public Object instantiate(String className) throws ReflectionException, MBeanException {
        // The `catch` is unreachable with a null `loaderName`, but the signature without
        // `loaderName` does not declare the exception and it has to be absorbed somewhere.
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
            throw new ReflectionException(e, "Class not found: " + className);
        }
        String[] wanted = (signature == null) ? new String[0] : signature;
        Object[] args = (params == null) ? new Object[0] : params;
        try {
            Constructor<?> ct = constructor(c, wanted);
            return ct.newInstance(args);
        } catch (NoSuchMethodException e) {
            throw new ReflectionException(e, "No constructor with that signature in " + className);
        } catch (InstantiationException e) {
            throw new ReflectionException(e, "Could not instantiate " + className);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e, "Constructor inaccesible en " + className);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException) {
                throw new RuntimeMBeanException((RuntimeException) cause, "The constructor threw");
            }
            if (cause instanceof Error) {
                throw new RuntimeErrorException((Error) cause, "The constructor threw");
            }
            throw new MBeanException((Exception) cause, "The constructor threw");
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
     * The JMX signature is an array of type names, and there {@code int} is not a class that can be
     * loaded. The eight have to be translated by hand; there is no other way.
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
            throw new ReflectionException(e, "Type not found: " + name);
        }
    }

    private ClassLoader loaderOf(ObjectName loaderName) throws InstanceNotFoundException {
        if (loaderName == null) {
            return null;
        }
        Object o = require(loaderName).object;
        if (!(o instanceof ClassLoader)) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException(loaderName + " is not a ClassLoader"));
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

    // ---- class loaders --------------------------------------------------------------------------

    /** The managed object's one; it serves to load classes in its same context. */
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        Entry e = require(mbeanName);
        Object target = (e.dynamic instanceof StandardMBean)
                ? ((StandardMBean) e.dynamic).getImplementation() : e.object;
        return target.getClass().getClassLoader();
    }

    /**
     * The MBean <b>is</b> a loader.
     *
     * <p>The {@code null} case is in the specification and means the agent's own loader.
     */
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        if (loaderName == null) {
            return getClass().getClassLoader();
        }
        Object o = require(loaderName).object;
        if (!(o instanceof ClassLoader)) {
            throw new InstanceNotFoundException(loaderName + " is not a ClassLoader");
        }
        return (ClassLoader) o;
    }

    /**
     * This agent's class loader repository.
     *
     * <p>The list is built <b>on every query</b> and not kept. It has to be: a loader registers
     * like any other MBean, at any time, and a repository frozen at startup would see none.
     *
     * <p>First goes the agent's own loader and then the MBeans that are loaders, in registration
     * order. Those implementing {@link PrivateClassLoader} are left out, which is the whole point
     * of that mark.
     */
    public ClassLoaderRepository getClassLoaderRepository() {
        return this.loaders;
    }

    /** The view of the registered loaders. See {@link #getClassLoaderRepository}. */
    private final class LoaderRepository implements ClassLoaderRepository {

        /** The agent first, then the registered ones; the private ones do not go in. */
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
            // If it is not in the list there is nowhere to cut, and all of them are searched.
            if (cut >= 0) {
                pool = pool.subList(0, cut);
            }
            return search(pool, className);
        }

        /** The common walk: the first that has it wins. */
        private Class<?> search(List<ClassLoader> pool, String className)
                throws ClassNotFoundException {
            for (ClassLoader cl : pool) {
                try {
                    return Class.forName(className, false, cl);
                } catch (ClassNotFoundException e) {
                    // This one does not have it; on to the next.
                }
            }
            throw new ClassNotFoundException(className);
        }
    }
}
