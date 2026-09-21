package javax.management;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Translates ordinary Java calls into operations against an MBean.
 *
 * <p>It is the exact reverse of {@link StandardMBean}: there an interface plus an object become a
 * {@link DynamicMBean}; here an interface plus an {@link ObjectName} become an object that seems to
 * implement it. Both use the same naming rule --{@code getX}/{@code isX}/{@code setX} are
 * attributes, the rest are operations--, each in one direction.
 *
 * <p>What is done with what does not belong to the MBean:
 *
 * <ul>
 *   <li>the three methods of {@code Object} --{@code equals}, {@code hashCode}, {@code toString}--
 *       are answered <b>here</b> and do not travel. Sending {@code hashCode()} to the agent would
 *       return the remote object's, which has nothing to do with the proxy and would break any
 *       local {@code HashMap};
 *   <li>those of {@link NotificationEmitter} are forwarded to the connection's, which take the
 *       {@code ObjectName} as first argument. It is what makes a proxy requested with
 *       {@code notificationBroadcaster} really useful for listening.
 * </ul>
 *
 * <h2>MXBean mode</h2>
 *
 * <p>With {@code isMXBean} set to {@code true} the handler <b>converts</b>: the arguments go to the
 * server as open types and the results come back as the interface's Java types. That is the whole
 * difference between an MBean proxy and an MXBean one, and {@link MXMapping} does it.
 *
 * <p>The conversion is resolved <b>when the proxy is built</b>, not on every call: if some type of
 * the interface cannot be mapped, the proxy is not created. It is the moment when whoever writes
 * the code can do something about it, and it avoids the worst case -- a proxy that works for half
 * the methods. Which types go in and which do not is in the note of {@link MXMapping}.
 */
public class MBeanServerInvocationHandler implements InvocationHandler {

    private final MBeanServerConnection connection;
    private final ObjectName name;
    private final boolean mxbean;

    /** Equivalent to {@code isMXBean = false}. */
    public MBeanServerInvocationHandler(MBeanServerConnection connection, ObjectName objectName) {
        this(connection, objectName, false);
    }

    /**
     * @param isMXBean whether to convert to open types; see the class note
     */
    public MBeanServerInvocationHandler(MBeanServerConnection connection, ObjectName objectName,
                                        boolean isMXBean) {
        this.mxbean = isMXBean;
        if (connection == null) {
            throw new IllegalArgumentException("The connection cannot be null");
        }
        if (objectName == null) {
            throw new IllegalArgumentException("The ObjectName cannot be null");
        }
        this.connection = connection;
        this.name = objectName;
    }

    public MBeanServerConnection getMBeanServerConnection() {
        return connection;
    }

    public ObjectName getObjectName() {
        return name;
    }

    /** Whether this proxy converts to open types. */
    public boolean isMXBean() {
        return this.mxbean;
    }

    /**
     * Builds the proxy.
     *
     * @param notificationBroadcaster whether, besides {@code interfaceClass}, the proxy also has to
     *        implement {@link NotificationEmitter}. It is a {@code boolean} and not deduced from
     *        the interface because listening to notifications is independent of what the MBean
     *        exposes as attributes.
     */
    public static <T> T newProxyInstance(MBeanServerConnection connection, ObjectName objectName,
                                         Class<T> interfaceClass,
                                         boolean notificationBroadcaster) {
        return newProxyInstance(connection, objectName, interfaceClass, notificationBroadcaster,
                                false);
    }

    // The general form: the one above is this with `mxbean` false.
    @SuppressWarnings("unchecked")
    static <T> T newProxyInstance(MBeanServerConnection connection, ObjectName objectName,
                                  Class<T> interfaceClass, boolean notificationBroadcaster,
                                  boolean mxbean) {
        if (mxbean) {
            // The mapping of the whole interface is resolved now: if something cannot be mapped,
            // the proxy never comes to exist. See the class note.
            MBeanServerInvocationHandler.requireMappable(interfaceClass);
        }
        InvocationHandler h = new MBeanServerInvocationHandler(connection, objectName, mxbean);
        Class<?>[] interfaces;
        if (notificationBroadcaster) {
            interfaces = new Class<?>[] { interfaceClass, NotificationEmitter.class };
        } else {
            interfaces = new Class<?>[] { interfaceClass };
        }
        Object p = Proxy.newProxyInstance(interfaceClass.getClassLoader(), interfaces, h);
        return (T) p;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> declares = method.getDeclaringClass();
        String nom = method.getName();
        Class<?>[] types = method.getParameterTypes();

        if (declares == Object.class) {
            return objectMethod(proxy, nom, args);
        }
        if (declares == NotificationBroadcaster.class || declares == NotificationEmitter.class) {
            return notificationMethod(nom, types, args);
        }

        try {
            if (types.length == 0 && nom.startsWith("get") && nom.length() > 3
                    && method.getReturnType() != Void.TYPE) {
                return toJava(connection.getAttribute(name, nom.substring(3)),
                             method.getReturnType());
            }
            if (types.length == 0 && nom.startsWith("is") && nom.length() > 2
                    && method.getReturnType() == Boolean.TYPE) {
                return toJava(connection.getAttribute(name, nom.substring(2)),
                             method.getReturnType());
            }
            if (types.length == 1 && nom.startsWith("set") && nom.length() > 3
                    && method.getReturnType() == Void.TYPE) {
                connection.setAttribute(name,
                        new Attribute(nom.substring(3), toOpen(args[0], types[0])));
                return null;
            }
            // The signature sent is that of the *open* types when the proxy is an MXBean: it is
            // what the server declares, and sending it the interface's Java types would make it not
            // find the operation.
            String[] signature = new String[types.length];
            Object[] passed = args == null ? null : new Object[args.length];
            for (int i = 0; i < types.length; i++) {
                signature[i] = this.mxbean
                        ? MBeanServerInvocationHandler.openName(types[i])
                        : types[i].getName();
                passed[i] = toOpen(args[i], types[i]);
            }
            return toJava(connection.invoke(name, nom, passed, signature), method.getReturnType());
        } catch (MBeanException e) {
            // It is unwrapped: whoever calls the proxy wrote a Java interface and expects *their*
            // exception, not the envelope JMX carried it in.
            throw e.getTargetException();
        } catch (RuntimeMBeanException e) {
            throw e.getTargetException();
        } catch (RuntimeErrorException e) {
            throw e.getTargetError();
        }
    }

    private Object objectMethod(Object proxy, String nom, Object[] args) {
        if (nom.equals("hashCode")) {
            return Integer.valueOf(name.hashCode());
        }
        if (nom.equals("toString")) {
            return getClass().getName() + "[" + name + "]";
        }
        // equals: two proxies are equal if they point to the same MBean over the same connection.
        Object other = args[0];
        if (other == null || !Proxy.isProxyClass(other.getClass())) {
            return Boolean.FALSE;
        }
        InvocationHandler h = Proxy.getInvocationHandler(other);
        if (!(h instanceof MBeanServerInvocationHandler)) {
            return Boolean.FALSE;
        }
        MBeanServerInvocationHandler q = (MBeanServerInvocationHandler) h;
        return Boolean.valueOf(name.equals(q.name) && connection == q.connection);
    }

    private Object notificationMethod(String nom, Class<?>[] types, Object[] args)
            throws Exception {
        if (nom.equals("getNotificationInfo")) {
            return connection.getMBeanInfo(name).getNotifications();
        }
        if (nom.equals("addNotificationListener")) {
            connection.addNotificationListener(name, (NotificationListener) args[0],
                                             (NotificationFilter) args[1], args[2]);
            return null;
        }
        if (nom.equals("removeNotificationListener")) {
            if (types.length == 1) {
                connection.removeNotificationListener(name, (NotificationListener) args[0]);
            } else {
                connection.removeNotificationListener(name, (NotificationListener) args[0],
                                                     (NotificationFilter) args[1], args[2]);
            }
            return null;
        }
        throw new UnsupportedOperationException(nom);
    }

    // ---- MXBean mode ------------------------------------------------------------------------

    /**
     * Checks that all the types the interface mentions can be mapped.
     *
     * @throws IllegalArgumentException if one cannot; the message names the method and the type
     */
    private static void requireMappable(Class<?> interfaceClass) {
        for (Method m : interfaceClass.getMethods()) {
            if (m.getDeclaringClass() == Object.class) {
                continue;
            }
            try {
                if (m.getReturnType() != Void.TYPE) {
                    MXMapping.de(m.getReturnType());
                }
                for (Class<?> p : m.getParameterTypes()) {
                    MXMapping.de(p);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(interfaceClass.getName() + "." + m.getName()
                        + ": " + e.getMessage());
            }
        }
    }

    /** The value that goes to the server. */
    private Object toOpen(Object v, Class<?> type) throws Exception {
        if (!this.mxbean || v == null) {
            return v;
        }
        return MXMapping.de(type).toOpen(v);
    }

    /** The value that comes back to the caller. */
    private Object toJava(Object v, Class<?> type) throws Exception {
        if (!this.mxbean || v == null) {
            return v;
        }
        return MXMapping.de(type).toJava(v);
    }

    // The class name with which a mapped type travels in a signature.
    private static String openName(Class<?> type) {
        MXMapping m = MXMapping.de(type);
        if (m.isIdentity()) {
            return type.getName();
        }
        javax.management.openmbean.OpenType<?> t = m.openType();
        return t == null ? type.getName() : t.getClassName();
    }
}
