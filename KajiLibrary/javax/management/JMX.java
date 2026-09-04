package javax.management;

/**
 * Las constantes de los campos de {@link Descriptor} y la fabrica de proxies.
 *
 * <p>No se instancia: es un lugar donde poner nombres, no un objeto. Las nueve constantes son las
 * claves con las que un `Descriptor` transporta lo que `MBeanInfo` no sabe expresar --el rango de
 * un atributo, su valor por omision, la lista de valores legales--. Existen como constantes y no
 * como literales sueltos por la razon de siempre: una clave mal escrita en un descriptor no falla,
 * simplemente no la lee nadie.
 *
 * <h2>Los dos `newMXBeanProxy`</h2>
 *
 * <p>Estaban afuera porque un proxy MXBean se define por convertir entre los tipos Java de la
 * interfaz y los tipos abiertos de `javax.management.openmbean`, y este arbol no tenia ese
 * subpaquete. Ya lo tiene --completo-- y la conversion la hace {@link MXMapeo}.
 *
 * <p>Lo que ese mapeo cubre y lo que no esta escrito en su propia nota, y conviene leerlo antes de
 * usar estos dos metodos: <b>{@code List<E>} y {@code Map<K,V>} quedan afuera</b> porque esta VM no
 * expone los argumentos de tipo, y sin saber quien es E no hay conversion posible. Una interfaz que
 * los mencione se rechaza <b>al crear el proxy</b>, con un mensaje que dice cual es el metodo y por
 * que. Nunca se devuelve un valor inventado.
 */
public class JMX {

    /** No se instancia. */
    private JMX() {
    }

    /** El valor que toma un atributo si no se le asigna otro. */
    public static final String DEFAULT_VALUE_FIELD = "defaultValue";

    /** Si el `MBeanInfo` no va a cambiar nunca, y entonces el cliente puede cachearlo. */
    public static final String IMMUTABLE_INFO_FIELD = "immutableInfo";

    /** La interfaz de administracion de un MBean estandar. */
    public static final String INTERFACE_CLASS_NAME_FIELD = "interfaceClassName";

    /** La enumeracion de valores aceptables. */
    public static final String LEGAL_VALUES_FIELD = "legalValues";

    /** Cota superior de un atributo o parametro numerico. */
    public static final String MAX_VALUE_FIELD = "maxValue";

    /** Cota inferior de un atributo o parametro numerico. */
    public static final String MIN_VALUE_FIELD = "minValue";

    /** Si el MBean es un MXBean. */
    public static final String MXBEAN_FIELD = "mxbean";

    /** El tipo abierto equivalente, para un MXBean. */
    public static final String OPEN_TYPE_FIELD = "openType";

    /** El tipo Java original, antes de mapearlo al abierto. */
    public static final String ORIGINAL_TYPE_FIELD = "originalType";

    /**
     * Un proxy local que habla con el MBean registrado bajo `objectName`.
     *
     * <p>No comprueba que el MBean exista ni que cumpla la interfaz: es a proposito y esta en la
     * especificacion. El proxy se puede armar antes de que el MBean se registre, y el error --si lo
     * hay-- aparece en la primera llamada, con el `ObjectName` adentro, que es mas util que un
     * fallo en el momento de armarlo.
     */
    public static <T> T newMBeanProxy(MBeanServerConnection connection, ObjectName objectName,
                                      Class<T> interfaceClass) {
        return newMBeanProxy(connection, objectName, interfaceClass, false);
    }

    /**
     * @param notificationEmitter si el proxy tiene que implementar ademas
     *        {@link NotificationEmitter}
     */
    public static <T> T newMBeanProxy(MBeanServerConnection connection, ObjectName objectName,
                                      Class<T> interfaceClass, boolean notificationEmitter) {
        return MBeanServerInvocationHandler.newProxyInstance(connection, objectName, interfaceClass,
                                                             notificationEmitter);
    }

    /**
     * Si la interfaz es un MXBean.
     *
     * <p>La anotacion {@link MXBean} manda, en los dos sentidos: `@MXBean(false)` sobre una interfaz
     * llamada `FooMXBean` la saca de la categoria. Solo si no esta la anotacion vale la convencion
     * del sufijo.
     *
     * @throws IllegalArgumentException si no es una interfaz
     */
    public static boolean isMXBeanInterface(Class<?> interfaceClass) {
        if (interfaceClass == null) {
            throw new IllegalArgumentException("La clase no puede ser null");
        }
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException(
                interfaceClass.getName() + " no es una interfaz");
        }
        MXBean anotacion = interfaceClass.getAnnotation(MXBean.class);
        if (anotacion != null) {
            return anotacion.value();
        }
        String simple = interfaceClass.getName();
        int punto = simple.lastIndexOf('.');
        if (punto >= 0) {
            simple = simple.substring(punto + 1);
        }
        int peso = simple.lastIndexOf('$');
        if (peso >= 0) {
            simple = simple.substring(peso + 1);
        }
        return simple.endsWith("MXBean") && simple.length() > "MXBean".length();
    }

    /**
     * Un proxy MXBean sobre ese MBean.
     *
     * <p>La diferencia con {@link #newMBeanProxy} es la conversion de tipos: lo que viaja son tipos
     * abiertos, y el proxy los traduce en las dos direcciones. Ver la nota de la clase.
     *
     * @throws IllegalArgumentException si algun tipo de la interfaz no se puede mapear
     */
    public static <T> T newMXBeanProxy(MBeanServerConnection connection, ObjectName objectName,
                                       Class<T> interfaceClass) {
        return newMXBeanProxy(connection, objectName, interfaceClass, false);
    }

    /**
     * @param notificationEmitter si el proxy tiene que implementar ademas
     *        {@link NotificationEmitter}
     * @throws IllegalArgumentException si algun tipo de la interfaz no se puede mapear
     */
    public static <T> T newMXBeanProxy(MBeanServerConnection connection, ObjectName objectName,
                                       Class<T> interfaceClass, boolean notificationEmitter) {
        return MBeanServerInvocationHandler.newProxyInstance(connection, objectName, interfaceClass,
                                                             notificationEmitter, true);
    }
}
