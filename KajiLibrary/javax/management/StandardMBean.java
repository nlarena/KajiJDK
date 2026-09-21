package javax.management;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Turns a standard MBean --an object plus its interface-- into a {@link DynamicMBean}.
 *
 * <p>It serves two different purposes worth not mixing. The first is <b>breaking the naming
 * convention</b>: an ordinary standard MBean requires {@code Foo} to implement {@code FooMBean},
 * and with this class any object is exposed under any interface it implements, whatever it is
 * called. The second is <b>touching up the metadata</b>: reflection knows the signature but not the
 * description or the impact, and that is what the {@code getDescription}/{@code getImpact} hooks
 * are for, redefined in a subclass.
 *
 * <h2>How it introspects</h2>
 *
 * <p>The <b>interface</b> is looked at, never the implementation's class. The methods are sorted
 * like this:
 *
 * <ul>
 *   <li>{@code T getX()} with no parameters and {@code T} other than {@code void} -- readable
 *       attribute {@code X};
 *   <li>{@code boolean isX()} with no parameters -- readable attribute {@code X}, in the
 *       {@code is} form;
 *   <li>{@code void setX(T)} with one parameter -- writable attribute {@code X};
 *   <li>everything else -- an operation.
 * </ul>
 *
 * <p>The rule is applied to the letter, and that is why {@code String getX(int)} is an operation
 * and not an indexed attribute: JMX has no indexed attributes, and treating it as an attribute
 * would mean inventing an index.
 *
 * <h2>The impact stays at {@code UNKNOWN}</h2>
 *
 * <p>And it cannot be otherwise from reflection: knowing whether an operation reads or writes
 * requires reading the body. {@code UNKNOWN} is the value the specification reserves precisely for
 * "not known"; putting {@code ACTION} or {@code INFO} by eye would be asserting something that
 * was not measured. Whoever knows declares it by redefining {@link #getImpact}.
 *
 * <h2>What this class does <b>not</b> do: MXBean</h2>
 *
 * <p>The constructors with {@code isMXBean} accept {@code false} --which is exactly the standard
 * MBean-- and <b>reject {@code true}</b>. The reason is no longer the one this note used to give:
 * {@code javax.management.openmbean} <b>is</b> here, complete, and the type conversion exists --
 * {@code MXMapping} writes it, and {@link JMX#newMXBeanProxy}, which is the <b>client</b> side,
 * lives off it.
 *
 * <p>What is missing is the <b>server</b> side, which is another thing and more work: a
 * {@code StandardMBean(x, I.class, true)} has to publish an {@code MBeanInfo} whose attributes and
 * operations are declared with the <b>open</b> types, and convert on every {@code getAttribute},
 * {@code setAttribute} and {@code invoke}. Until that is written, building one anyway would leave
 * an object that claims to be an MXBean and publishes raw Java types: a lie discovered only on the
 * client side. Rejecting it in the constructor leaves it where it can be seen.
 */
public class StandardMBean implements DynamicMBean, MBeanRegistration {

    private Object implementation;
    private final Class<?> iface;

    /** Built once and kept: the interface does not change, and reflection is not free. */
    private MBeanInfo cache;

    /** Attribute name to method, resolved at construction so as not to search on every call. */
    private final Map<String, Method> getters = new TreeMap<String, Method>();
    private final Map<String, Method> setters = new TreeMap<String, Method>();

    /** Operations indexed by name; there may be several overloads with the same name. */
    private final Map<String, List<Method>> operations = new LinkedHashMap<String, List<Method>>();

    /**
     * The interface's method, resolved against the implementation's class.
     *
     * <p>It is cached because {@code getMethod} is not cheap and dispatch goes through here on
     * every call. It is emptied in {@link #setImplementation}, which is the only thing that can
     * change the answer.
     */
    private final Map<Method, Method> resolved = new java.util.HashMap<Method, Method>();

    /**
     * Wraps {@code implementation} to expose it under {@code mbeanInterface}.
     *
     * @throws NotCompliantMBeanException if the object does not implement that interface, or if the
     *         interface is not coherent --for example a {@code getX}/{@code setX} that do not talk
     *         about the same type
     */
    public <T> StandardMBean(T implementation, Class<T> mbeanInterface)
            throws NotCompliantMBeanException {
        if (implementation == null) {
            throw new IllegalArgumentException("The implementation cannot be null");
        }
        this.iface = chooseInterface(implementation, mbeanInterface);
        checkImplements(implementation, this.iface);
        this.implementation = implementation;
        introspect();
    }

    /**
     * For subclassing: the implementation is {@code this}.
     *
     * <p>The order matters and is different from the other constructor: here it is not possible to
     * check that {@code this} implements the interface until the subclass finishes constructing...
     * but {@code this} is already of the final class, so the check is valid right now.
     */
    protected StandardMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        this.iface = chooseInterface(this, mbeanInterface);
        checkImplements(this, this.iface);
        this.implementation = this;
        introspect();
    }

    /**
     * @param isMXBean has to be {@code false}; see the MXBean note in the class
     * @throws UnsupportedOperationException if it is {@code true}
     * @throws IllegalArgumentException if the object or the interface are not valid. This
     *         constructor does not declare {@code NotCompliantMBeanException} --the JDK's does not
     *         either-- and that is why non-compliance comes out wrapped in an unchecked exception.
     */
    public <T> StandardMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean) {
        checkNotMXBean(isMXBean);
        if (implementation == null) {
            throw new IllegalArgumentException("The implementation cannot be null");
        }
        this.iface = chooseInterface(implementation, mbeanInterface);
        try {
            checkImplements(implementation, this.iface);
            this.implementation = implementation;
            introspect();
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @param isMXBean has to be {@code false}
     * @throws UnsupportedOperationException if it is {@code true}
     */
    protected StandardMBean(Class<?> mbeanInterface, boolean isMXBean) {
        checkNotMXBean(isMXBean);
        this.iface = chooseInterface(this, mbeanInterface);
        try {
            checkImplements(this, this.iface);
            this.implementation = this;
            introspect();
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void checkNotMXBean(boolean isMXBean) {
        if (isMXBean) {
            throw new UnsupportedOperationException(
                "StandardMBean does not support MXBeans in this library, so an MXBean cannot "
                + "be wrapped here. Only isMXBean=false is accepted.");
        }
    }

    /**
     * If no interface was given, the only one that fits by naming convention is looked for.
     *
     * <p>{@code null} is accepted because the JDK accepts it: it means "discover it". The
     * convention is that the class {@code p.Foo} is exposed through {@code p.FooMBean}.
     */
    private static Class<?> chooseInterface(Object impl, Class<?> declared) {
        if (declared != null) {
            return declared;
        }
        Class<?> c = impl.getClass();
        while (c != null) {
            String expected = c.getName() + "MBean";
            for (Class<?> i : c.getInterfaces()) {
                if (i.getName().equals(expected)) {
                    return i;
                }
            }
            c = c.getSuperclass();
        }
        throw new IllegalArgumentException(
            "No interface was given and none follows the <Class>MBean convention");
    }

    private static void checkImplements(Object impl, Class<?> iface)
            throws NotCompliantMBeanException {
        if (!iface.isInterface()) {
            throw new NotCompliantMBeanException(iface.getName() + " is not an interface");
        }
        if (!iface.isInstance(impl)) {
            throw new NotCompliantMBeanException(
                impl.getClass().getName() + " does not implement " + iface.getName());
        }
    }

    /** Sorts the interface's methods into attributes and operations. */
    private void introspect() throws NotCompliantMBeanException {
        for (Method m : iface.getMethods()) {
            String name = m.getName();
            Class<?>[] args = m.getParameterTypes();
            Class<?> ret = m.getReturnType();

            if (args.length == 0 && ret != Void.TYPE && name.startsWith("get")
                    && name.length() > 3) {
                setGetter(name.substring(3), m);
            } else if (args.length == 0 && ret == Boolean.TYPE && name.startsWith("is")
                    && name.length() > 2) {
                setGetter(name.substring(2), m);
            } else if (args.length == 1 && ret == Void.TYPE && name.startsWith("set")
                    && name.length() > 3) {
                setSetter(name.substring(3), m);
            } else {
                List<Method> l = operations.get(name);
                if (l == null) {
                    l = new ArrayList<Method>();
                    operations.put(name, l);
                }
                l.add(m);
            }
        }
        // It is checked only at the end because a `setX` may appear before its `getX`.
        for (Map.Entry<String, Method> e : getters.entrySet()) {
            Method set = setters.get(e.getKey());
            if (set != null && !set.getParameterTypes()[0].equals(e.getValue().getReturnType())) {
                throw new NotCompliantMBeanException(
                    "The attribute " + e.getKey()
                        + " has a getter and a setter of different types");
            }
        }
    }

    private void setGetter(String attribute, Method m) throws NotCompliantMBeanException {
        Method previous = getters.get(attribute);
        if (previous != null && !previous.getReturnType().equals(m.getReturnType())) {
            // Happens with `getX()` and `isX()` together, or inheriting from two different
            // interfaces.
            throw new NotCompliantMBeanException(
                "The attribute " + attribute + " has two getters of different types");
        }
        getters.put(attribute, m);
    }

    private void setSetter(String attribute, Method m) throws NotCompliantMBeanException {
        Method previous = setters.get(attribute);
        if (previous != null && !previous.getParameterTypes()[0].equals(m.getParameterTypes()[0])) {
            throw new NotCompliantMBeanException(
                "The attribute " + attribute + " has two setters of different types");
        }
        setters.put(attribute, m);
    }

    /**
     * Changes the object behind without redoing the introspection: the interface is the same, so
     * the metadata is too.
     *
     * @throws NotCompliantMBeanException if the new object does not implement the interface
     */
    public void setImplementation(Object implementation) throws NotCompliantMBeanException {
        if (implementation == null) {
            throw new IllegalArgumentException("The implementation cannot be null");
        }
        checkImplements(implementation, iface);
        this.implementation = implementation;
        synchronized (resolved) {
            resolved.clear();
        }
    }

    public Object getImplementation() {
        return implementation;
    }

    /** Final: changing the interface would invalidate the already published metadata. */
    public final Class<?> getMBeanInterface() {
        return iface;
    }

    public Class<?> getImplementationClass() {
        return implementation.getClass();
    }

    // ---- DynamicMBean -------------------------------------------------------------------------

    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        Method g = getters.get(attribute);
        if (g == null) {
            throw new AttributeNotFoundException("No readable attribute: " + attribute);
        }
        return call(g, new Object[0]);
    }

    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException,
                   ReflectionException {
        Method s = setters.get(attribute.getName());
        if (s == null) {
            throw new AttributeNotFoundException(
                "No writable attribute: " + attribute.getName());
        }
        try {
            call(s, new Object[] { attribute.getValue() });
        } catch (ReflectionException e) {
            // Reflection throws `IllegalArgumentException` when the value is not of the parameter's
            // type; in JMX that case has its own exception and has to be translated.
            if (e.getTargetException() instanceof IllegalArgumentException) {
                throw new InvalidAttributeValueException(
                    "Invalid value for " + attribute.getName() + ": " + attribute.getValue());
            }
            throw e;
        }
    }

    /**
     * Reads several, <b>best effort</b>: the ones that fail simply do not appear in the result.
     *
     * <p>It is what the specification dictates and it is not an oversight. This method exists to
     * save network round trips when reading a whole dashboard; if one broken attribute brought the
     * call down, a single MBean with a problem would blind the entire dashboard.
     */
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList();
        if (attributes == null) {
            return result;
        }
        for (String name : attributes) {
            try {
                result.add(new Attribute(name, getAttribute(name)));
            } catch (Exception e) {
                // Left out on purpose: see the javadoc.
            }
        }
        return result;
    }

    /** Writes several best effort; returns only the ones actually written. */
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList result = new AttributeList();
        if (attributes == null) {
            return result;
        }
        for (Attribute a : attributes.asList()) {
            try {
                setAttribute(a);
                result.add(new Attribute(a.getName(), a.getValue()));
            } catch (Exception e) {
                // Same.
            }
        }
        return result;
    }

    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        List<Method> candidates = operations.get(actionName);
        if (candidates == null) {
            throw new ReflectionException(
                new NoSuchMethodException(actionName), "No operation " + actionName);
        }
        String[] sig = (signature == null) ? new String[0] : signature;
        for (Method m : candidates) {
            if (signatureMatches(m, sig)) {
                return call(m, params == null ? new Object[0] : params);
            }
        }
        throw new ReflectionException(
            new NoSuchMethodException(actionName),
            "No overload of " + actionName + " matches the given signature");
    }

    private static boolean signatureMatches(Method m, String[] sig) {
        Class<?>[] args = m.getParameterTypes();
        if (args.length != sig.length) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (!args[i].getName().equals(sig[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calls and translates whatever comes out.
     *
     * <p>The translation is not decorative: JMX distinguishes <b>whose fault it is</b>. What the
     * MBean throws goes in {@link MBeanException} --it is a failure of the managed resource--; what
     * fails while trying to call it goes in {@link ReflectionException} --it is a failure of the
     * agent. A remote client that receives one or the other knows whether to retry or to report.
     */
    private Object call(Method m, Object[] args) throws MBeanException, ReflectionException {
        try {
            return resolver(m).invoke(implementation, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException) {
                throw new RuntimeMBeanException((RuntimeException) cause,
                        "The MBean threw " + cause);
            }
            if (cause instanceof Error) {
                throw new RuntimeErrorException((Error) cause, "The MBean threw " + cause);
            }
            throw new MBeanException((Exception) cause, "The MBean threw " + cause);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e, "Could not call " + m.getName());
        } catch (IllegalArgumentException e) {
            throw new ReflectionException(e, "Invalid arguments for " + m.getName());
        }
    }

    /**
     * Lowers the interface's method to the class that really implements it.
     *
     * <p>Semantically it is the same --it is the same override-- but it is needed anyway: if the
     * implementation is of a non-public class, that class's {@code Method} cannot be invoked
     * without {@code setAccessible}, while the public interface's can. The JDK needs this lowering
     * for that reason and so it is done here too.
     *
     * <p>There was a second reason, which no longer applies: KajiJDK's VM did not re-dispatch in
     * {@code Method.invoke} --it ran the body of the declared method-- so invoking the interface's
     * {@code Method} blew up when it was abstract. That is fixed (finding #466); this lowering
     * stayed because the first reason still holds.
     */
    private Method resolver(Method m) {
        synchronized (resolved) {
            Method already = resolved.get(m);
            if (already != null) {
                return already;
            }
        }
        Method chosen = m;
        try {
            Method concrete = implementation.getClass().getMethod(m.getName(),
                                                                  m.getParameterTypes());
            if (java.lang.reflect.Modifier.isPublic(concrete.getDeclaringClass().getModifiers())) {
                chosen = concrete;
            }
        } catch (NoSuchMethodException e) {
            // It should not happen --the implementation follows the interface-- but if it does, the
            // interface's one is still the best bet.
        }
        synchronized (resolved) {
            resolved.put(m, chosen);
        }
        return chosen;
    }

    /**
     * The metadata, with the cache in the middle.
     *
     * <p>It is built once and kept. A subclass that wants changing metadata redefines
     * {@link #getCachedMBeanInfo} to return {@code null}.
     */
    public MBeanInfo getMBeanInfo() {
        MBeanInfo mi = getCachedMBeanInfo();
        if (mi != null) {
            return mi;
        }
        mi = buildMBeanInfo();
        cacheMBeanInfo(mi);
        return mi;
    }

    private MBeanInfo buildMBeanInfo() {
        // First pass: what reflection knows, without descriptions or impact.
        List<MBeanAttributeInfo> atrs = new ArrayList<MBeanAttributeInfo>();
        List<String> names = new ArrayList<String>(getters.keySet());
        for (String n : setters.keySet()) {
            if (!names.contains(n)) {
                names.add(n);
            }
        }
        java.util.Collections.sort(names);
        for (String n : names) {
            try {
                atrs.add(new MBeanAttributeInfo(n, "Attribute exposed for management",
                                                getters.get(n), setters.get(n)));
            } catch (IntrospectionException e) {
                // Cannot happen: `introspect` already rejected the incoherent pairs.
                throw new IllegalStateException(e);
            }
        }

        List<MBeanOperationInfo> ops = new ArrayList<MBeanOperationInfo>();
        for (List<Method> l : operations.values()) {
            for (Method m : l) {
                ops.add(new MBeanOperationInfo("Operation exposed for management", m));
            }
        }

        MBeanConstructorInfo[] ctors = rawConstructors();

        MBeanInfo raw = new MBeanInfo(
            getImplementationClass().getName(),
            iface.getName(),
            atrs.toArray(new MBeanAttributeInfo[0]),
            ctors,
            ops.toArray(new MBeanOperationInfo[0]),
            notificationsOf(implementation));

        // Second pass: the hooks. The subclass sees the raw version and decides what changes. Doing
        // it in two passes and not calling the hooks while walking the reflection is what lets the
        // hook receive a complete `MBeanInfo`, with context.
        return applyHooks(raw);
    }

    private MBeanConstructorInfo[] rawConstructors() {
        Constructor<?>[] cs = getImplementationClass().getConstructors();
        MBeanConstructorInfo[] r = new MBeanConstructorInfo[cs.length];
        for (int i = 0; i < cs.length; i++) {
            r[i] = new MBeanConstructorInfo("Public constructor of the class", cs[i]);
        }
        return getConstructors(r, implementation);
    }

    private static MBeanNotificationInfo[] notificationsOf(Object impl) {
        if (impl instanceof NotificationBroadcaster) {
            return ((NotificationBroadcaster) impl).getNotificationInfo();
        }
        return new MBeanNotificationInfo[0];
    }

    private MBeanInfo applyHooks(MBeanInfo raw) {
        MBeanAttributeInfo[] atrs = raw.getAttributes();
        MBeanAttributeInfo[] attrs2 = new MBeanAttributeInfo[atrs.length];
        for (int i = 0; i < atrs.length; i++) {
            MBeanAttributeInfo a = atrs[i];
            attrs2[i] = new MBeanAttributeInfo(a.getName(), a.getType(), getDescription(a),
                                              a.isReadable(), a.isWritable(), a.isIs(),
                                              a.getDescriptor());
        }

        MBeanOperationInfo[] ops = raw.getOperations();
        MBeanOperationInfo[] ops2 = new MBeanOperationInfo[ops.length];
        for (int i = 0; i < ops.length; i++) {
            MBeanOperationInfo o = ops[i];
            ops2[i] = new MBeanOperationInfo(o.getName(), getDescription(o),
                                             params(o, o.getSignature()), o.getReturnType(),
                                             getImpact(o), o.getDescriptor());
        }

        MBeanConstructorInfo[] cs = raw.getConstructors();
        MBeanConstructorInfo[] cs2 = new MBeanConstructorInfo[cs.length];
        for (int i = 0; i < cs.length; i++) {
            MBeanConstructorInfo c = cs[i];
            cs2[i] = new MBeanConstructorInfo(c.getName(), getDescription(c),
                                              params(c, c.getSignature()), c.getDescriptor());
        }

        return new MBeanInfo(getClassName(raw), getDescription(raw), attrs2, cs2, ops2,
                             raw.getNotifications(), raw.getDescriptor());
    }

    private MBeanParameterInfo[] params(MBeanOperationInfo op, MBeanParameterInfo[] ps) {
        MBeanParameterInfo[] r = new MBeanParameterInfo[ps.length];
        for (int i = 0; i < ps.length; i++) {
            r[i] = new MBeanParameterInfo(getParameterName(op, ps[i], i), ps[i].getType(),
                                          getDescription(op, ps[i], i), ps[i].getDescriptor());
        }
        return r;
    }

    private MBeanParameterInfo[] params(MBeanConstructorInfo ct, MBeanParameterInfo[] ps) {
        MBeanParameterInfo[] r = new MBeanParameterInfo[ps.length];
        for (int i = 0; i < ps.length; i++) {
            r[i] = new MBeanParameterInfo(getParameterName(ct, ps[i], i), ps[i].getType(),
                                          getDescription(ct, ps[i], i), ps[i].getDescriptor());
        }
        return r;
    }

    // ---- hooks: by default they return what already came --------------------------------------

    protected String getClassName(MBeanInfo info) {
        return info == null ? null : info.getClassName();
    }

    protected String getDescription(MBeanInfo info) {
        return info == null ? null : info.getDescription();
    }

    protected String getDescription(MBeanFeatureInfo info) {
        return info == null ? null : info.getDescription();
    }

    protected String getDescription(MBeanAttributeInfo info) {
        return getDescription((MBeanFeatureInfo) info);
    }

    protected String getDescription(MBeanConstructorInfo info) {
        return getDescription((MBeanFeatureInfo) info);
    }

    protected String getDescription(MBeanConstructorInfo ctor, MBeanParameterInfo param,
                                    int sequence) {
        return param == null ? null : param.getDescription();
    }

    protected String getParameterName(MBeanConstructorInfo ctor, MBeanParameterInfo param,
                                      int sequence) {
        return param == null ? null : param.getName();
    }

    protected String getDescription(MBeanOperationInfo info) {
        return getDescription((MBeanFeatureInfo) info);
    }

    /** {@code UNKNOWN} unless the subclass knows more; see the note about impact in the class. */
    protected int getImpact(MBeanOperationInfo info) {
        return info == null ? MBeanOperationInfo.UNKNOWN : info.getImpact();
    }

    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param,
                                      int sequence) {
        return param == null ? null : param.getName();
    }

    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param,
                                    int sequence) {
        return param == null ? null : param.getDescription();
    }

    /**
     * Which constructors are published.
     *
     * <p>By default, none if the managed object is <b>another</b>. It is intentional: publishing
     * the constructors serves so that a client can instantiate the MBean through the agent, and
     * that only makes sense when this class <b>is</b> the MBean. When wrapping a third party, its
     * constructors would build the wrapped object and not the wrapper, which is not what the client
     * would be asking for.
     */
    protected MBeanConstructorInfo[] getConstructors(MBeanConstructorInfo[] ctors, Object impl) {
        if (ctors == null) {
            return null;
        }
        if (impl != null && impl != this) {
            return new MBeanConstructorInfo[0];
        }
        return ctors;
    }

    /** What is stored, or {@code null} if it has not been built yet. */
    protected MBeanInfo getCachedMBeanInfo() {
        return cache;
    }

    protected void cacheMBeanInfo(MBeanInfo info) {
        cache = info;
    }

    // ---- MBeanRegistration: delegated if the managed object knows about it ---------------------

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        if (implementation instanceof MBeanRegistration) {
            return ((MBeanRegistration) implementation).preRegister(server, name);
        }
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        if (implementation instanceof MBeanRegistration) {
            ((MBeanRegistration) implementation).postRegister(registrationDone);
        }
    }

    public void preDeregister() throws Exception {
        if (implementation instanceof MBeanRegistration) {
            ((MBeanRegistration) implementation).preDeregister();
        }
    }

    public void postDeregister() {
        if (implementation instanceof MBeanRegistration) {
            ((MBeanRegistration) implementation).postDeregister();
        }
    }
}
