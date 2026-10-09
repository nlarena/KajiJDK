package javax.management;

/**
 * The constants for the {@link Descriptor} fields and the proxy factory.
 *
 * <p>It is not instantiated: it is a place to put names, not an object. The nine constants are the
 * keys with which a {@code Descriptor} carries what {@code MBeanInfo} cannot express --an
 * attribute's range, its default value, the list of legal values. They exist as constants and not
 * as loose literals for the usual reason: a misspelled key in a descriptor does not fail, nobody
 * reads it.
 *
 * <h2>The two {@code newMXBeanProxy}</h2>
 *
 * <p>They were left out because an MXBean proxy is defined by converting between the interface's
 * Java types and the open types of {@code javax.management.openmbean}, and this tree did not have
 * that subpackage. It has it now --complete-- and {@link MXMapping} does the conversion.
 *
 * <p>What that mapping covers and what it does not is written in its own note, and it is worth
 * reading before using these two methods: <b>{@code List<E>} and {@code Map<K,V>} are left out</b>
 * because this VM does not expose type arguments ({@code getGenericReturnType} returns the raw
 * type), and without knowing what E is there is no possible conversion. An interface that mentions
 * them is rejected <b>when the proxy is created</b>, with a message saying which method and why. A
 * made-up value is never returned.
 */
public class JMX {

    /** Not instantiated. */
    private JMX() {
    }

    /** The value an attribute takes if it is not assigned another. */
    public static final String DEFAULT_VALUE_FIELD = "defaultValue";

    /** Whether the {@code MBeanInfo} will never change, so the client can cache it. */
    public static final String IMMUTABLE_INFO_FIELD = "immutableInfo";

    /** The management interface of a standard MBean. */
    public static final String INTERFACE_CLASS_NAME_FIELD = "interfaceClassName";

    /** The enumeration of acceptable values. */
    public static final String LEGAL_VALUES_FIELD = "legalValues";

    /** Upper bound of a numeric attribute or parameter. */
    public static final String MAX_VALUE_FIELD = "maxValue";

    /** Lower bound of a numeric attribute or parameter. */
    public static final String MIN_VALUE_FIELD = "minValue";

    /** Whether the MBean is an MXBean. */
    public static final String MXBEAN_FIELD = "mxbean";

    /** The equivalent open type, for an MXBean. */
    public static final String OPEN_TYPE_FIELD = "openType";

    /** The original Java type, before mapping it to the open one. */
    public static final String ORIGINAL_TYPE_FIELD = "originalType";

    /**
     * A local proxy that talks to the MBean registered under {@code objectName}.
     *
     * <p>It does not check that the MBean exists or follows the interface: that is on purpose and
     * in the specification. The proxy can be built before the MBean is registered, and the error
     * --if any-- shows up on the first call, with the {@code ObjectName} inside, which is more
     * useful than a failure when building it.
     */
    public static <T> T newMBeanProxy(MBeanServerConnection connection, ObjectName objectName,
                                      Class<T> interfaceClass) {
        return newMBeanProxy(connection, objectName, interfaceClass, false);
    }

    /**
     * @param notificationEmitter whether the proxy also has to implement
     *        {@link NotificationEmitter}
     */
    public static <T> T newMBeanProxy(MBeanServerConnection connection, ObjectName objectName,
                                      Class<T> interfaceClass, boolean notificationEmitter) {
        return MBeanServerInvocationHandler.newProxyInstance(connection, objectName, interfaceClass,
                                                             notificationEmitter);
    }

    /**
     * Whether the interface is an MXBean.
     *
     * <p>The {@link MXBean} annotation rules, both ways: {@code @MXBean(false)} on an interface
     * named {@code FooMXBean} takes it out of the category. Only if the annotation is absent does
     * the suffix convention apply.
     *
     * <p>Unlike the JDK, which returns {@code false} for a class that is not an interface (or not
     * public) and throws {@code NullPointerException} for {@code null}, this one throws
     * {@code IllegalArgumentException} in both cases and does not look at public-ness.
     *
     * @throws IllegalArgumentException if it is null or not an interface
     */
    public static boolean isMXBeanInterface(Class<?> interfaceClass) {
        if (interfaceClass == null) {
            throw new IllegalArgumentException("The class cannot be null");
        }
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException(
                interfaceClass.getName() + " is not an interface");
        }
        MXBean annotation = interfaceClass.getAnnotation(MXBean.class);
        if (annotation != null) {
            return annotation.value();
        }
        String simple = interfaceClass.getName();
        int dot = simple.lastIndexOf('.');
        if (dot >= 0) {
            simple = simple.substring(dot + 1);
        }
        int peso = simple.lastIndexOf('$');
        if (peso >= 0) {
            simple = simple.substring(peso + 1);
        }
        return simple.endsWith("MXBean") && simple.length() > "MXBean".length();
    }

    /**
     * An MXBean proxy over that MBean.
     *
     * <p>The difference from {@link #newMBeanProxy} is the type conversion: what travels are open
     * types, and the proxy translates them both ways. See the class note.
     *
     * @throws IllegalArgumentException if some type of the interface cannot be mapped
     */
    public static <T> T newMXBeanProxy(MBeanServerConnection connection, ObjectName objectName,
                                       Class<T> interfaceClass) {
        return newMXBeanProxy(connection, objectName, interfaceClass, false);
    }

    /**
     * @param notificationEmitter whether the proxy also has to implement
     *        {@link NotificationEmitter}
     * @throws IllegalArgumentException if some type of the interface cannot be mapped
     */
    public static <T> T newMXBeanProxy(MBeanServerConnection connection, ObjectName objectName,
                                       Class<T> interfaceClass, boolean notificationEmitter) {
        return MBeanServerInvocationHandler.newProxyInstance(connection, objectName, interfaceClass,
                                                             notificationEmitter, true);
    }
}
