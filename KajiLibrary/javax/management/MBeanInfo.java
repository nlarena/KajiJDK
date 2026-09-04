package javax.management;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Todo lo que hay que saber de un MBean sin tener su clase: atributos, constructores, operaciones y
 * notificaciones.
 *
 * <p>Es la pieza que sostiene el modelo entero. Un cliente remoto no carga la clase del MBean --
 * puede no tenerla-- y sin embargo puede listar sus atributos, invocar sus operaciones y suscribirse
 * a sus notificaciones, porque todo eso esta descrito aca en cadenas. Por eso los tipos son
 * `String` y no `Class`: un `Class` obligaria a cargar del otro lado lo que se quiso evitar.
 *
 * <p>Los cuatro arreglos nunca son `null` al salir: un `null` que entra se guarda como arreglo
 * vacio. Es la diferencia entre "no declara operaciones" y "no se sabe", y JMX se queda con la
 * primera.
 */
public class MBeanInfo implements Cloneable, Serializable, DescriptorRead {

    static final long serialVersionUID = -6451021435135161911L;

    /**
     * @serial texto para leer
     */
    private final String description;

    /**
     * @serial el nombre de la clase Java del MBean
     */
    private final String className;

    /**
     * @serial los atributos
     */
    private final MBeanAttributeInfo[] attributes;

    /**
     * @serial las operaciones
     */
    private final MBeanOperationInfo[] operations;

    /**
     * @serial los constructores
     */
    private final MBeanConstructorInfo[] constructors;

    /**
     * @serial las notificaciones
     */
    private final MBeanNotificationInfo[] notifications;

    private transient Descriptor descriptor;

    private transient int hashCode;

    public MBeanInfo(String className, String description, MBeanAttributeInfo[] attributes,
                     MBeanConstructorInfo[] constructors, MBeanOperationInfo[] operations,
                     MBeanNotificationInfo[] notifications) throws IllegalArgumentException {
        this(className, description, attributes, constructors, operations, notifications, null);
    }

    public MBeanInfo(String className, String description, MBeanAttributeInfo[] attributes,
                     MBeanConstructorInfo[] constructors, MBeanOperationInfo[] operations,
                     MBeanNotificationInfo[] notifications, Descriptor descriptor)
            throws IllegalArgumentException {
        this.className = className;
        this.description = description;
        this.attributes = attributes == null ? MBeanAttributeInfo.NO_ATTRIBUTES : attributes;
        this.constructors = constructors == null
                ? MBeanConstructorInfo.NO_CONSTRUCTORS : constructors;
        this.operations = operations == null ? MBeanOperationInfo.NO_OPERATIONS : operations;
        this.notifications = notifications == null
                ? MBeanNotificationInfo.NO_NOTIFICATIONS : notifications;
        this.descriptor = descriptor == null ? ImmutableDescriptor.EMPTY_DESCRIPTOR : descriptor;
    }

    /**
     * Copia superficial, con la identidad de la subclase intacta.
     *
     * <p>Va por `Object.clone()` y no por el constructor justamente por eso: un `new MBeanInfo(...)`
     * devolveria un `MBeanInfo` pelado aunque el original fuera de una subclase.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** El nombre de la clase Java del MBean. */
    public String getClassName() {
        return className;
    }

    /** Texto para leer. */
    public String getDescription() {
        return description;
    }

    /** Copia nueva en cada llamada. */
    public MBeanAttributeInfo[] getAttributes() {
        MBeanAttributeInfo[] r = new MBeanAttributeInfo[attributes.length];
        System.arraycopy(attributes, 0, r, 0, attributes.length);
        return r;
    }

    /** Copia nueva en cada llamada. */
    public MBeanOperationInfo[] getOperations() {
        MBeanOperationInfo[] r = new MBeanOperationInfo[operations.length];
        System.arraycopy(operations, 0, r, 0, operations.length);
        return r;
    }

    /** Copia nueva en cada llamada. */
    public MBeanConstructorInfo[] getConstructors() {
        MBeanConstructorInfo[] r = new MBeanConstructorInfo[constructors.length];
        System.arraycopy(constructors, 0, r, 0, constructors.length);
        return r;
    }

    /** Copia nueva en cada llamada. */
    public MBeanNotificationInfo[] getNotifications() {
        MBeanNotificationInfo[] r = new MBeanNotificationInfo[notifications.length];
        System.arraycopy(notifications, 0, r, 0, notifications.length);
        return r;
    }

    /** Nunca `null`. */
    public Descriptor getDescriptor() {
        return descriptor == null ? ImmutableDescriptor.EMPTY_DESCRIPTOR : descriptor;
    }

    public String toString() {
        return getClass().getName()
                + "[description=" + getDescription()
                + ", attributes=" + aTexto(attributes)
                + ", constructors=" + aTexto(constructors)
                + ", operations=" + aTexto(operations)
                + ", notifications=" + aTexto(notifications)
                + ", descriptor=" + getDescriptor()
                + "]";
    }

    /**
     * {@code [a, b, c]}, como el de `Arrays.toString`.
     *
     * <p>Vive aca y es de paquete porque las cinco clases de `MBean*Info` la comparten y esta
     * biblioteca no trae `Arrays.toString` para arreglos de objetos.
     */
    static String aTexto(Object[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(String.valueOf(a[i]));
        }
        return b.append("]").toString();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MBeanInfo)) {
            return false;
        }
        MBeanInfo p = (MBeanInfo) o;
        return MBeanFeatureInfo.igual(p.getClassName(), getClassName())
                && MBeanFeatureInfo.igual(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor())
                && Arrays.equals(p.attributes, attributes)
                && Arrays.equals(p.operations, operations)
                && Arrays.equals(p.constructors, constructors)
                && Arrays.equals(p.notifications, notifications);
    }

    /** Se calcula una vez: el objeto es inmutable. */
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = getClassName().hashCode()
                    ^ getDescriptor().hashCode()
                    ^ Arrays.hashCode(attributes)
                    ^ Arrays.hashCode(operations)
                    ^ Arrays.hashCode(constructors)
                    ^ Arrays.hashCode(notifications);
        }
        return hashCode;
    }
}
