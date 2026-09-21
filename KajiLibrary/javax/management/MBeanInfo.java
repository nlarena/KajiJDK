package javax.management;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Everything there is to know about an MBean without having its class: attributes, constructors,
 * operations and notifications.
 *
 * <p>It is the piece that holds up the whole model. A remote client does not load the MBean's class
 * -- it may not have it-- and yet it can list its attributes, invoke its operations and subscribe
 * to its notifications, because all of that is described here in strings. That is why the types are
 * {@code String} and not {@code Class}: a {@code Class} would force loading on the other side what
 * was meant to be avoided.
 *
 * <p>The four arrays are never {@code null} on the way out: a {@code null} coming in is kept as an
 * empty array. It is the difference between "declares no operations" and "unknown", and JMX
 * keeps the first.
 */
public class MBeanInfo implements Cloneable, Serializable, DescriptorRead {

    static final long serialVersionUID = -6451021435135161911L;

    /**
     * @serial text to read
     */
    private final String description;

    /**
     * @serial the name of the MBean's Java class
     */
    private final String className;

    /**
     * @serial the attributes
     */
    private final MBeanAttributeInfo[] attributes;

    /**
     * @serial the operations
     */
    private final MBeanOperationInfo[] operations;

    /**
     * @serial the constructors
     */
    private final MBeanConstructorInfo[] constructors;

    /**
     * @serial the notifications
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
     * Shallow copy, with the subclass's identity intact.
     *
     * <p>It goes through {@code Object.clone()} and not through the constructor precisely for that:
     * a {@code new MBeanInfo(...)} would return a bare {@code MBeanInfo} even if the original were
     * of a subclass.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /** The name of the MBean's Java class. */
    public String getClassName() {
        return className;
    }

    /** Text to read. */
    public String getDescription() {
        return description;
    }

    /** A fresh copy on every call. */
    public MBeanAttributeInfo[] getAttributes() {
        MBeanAttributeInfo[] r = new MBeanAttributeInfo[attributes.length];
        System.arraycopy(attributes, 0, r, 0, attributes.length);
        return r;
    }

    /** A fresh copy on every call. */
    public MBeanOperationInfo[] getOperations() {
        MBeanOperationInfo[] r = new MBeanOperationInfo[operations.length];
        System.arraycopy(operations, 0, r, 0, operations.length);
        return r;
    }

    /** A fresh copy on every call. */
    public MBeanConstructorInfo[] getConstructors() {
        MBeanConstructorInfo[] r = new MBeanConstructorInfo[constructors.length];
        System.arraycopy(constructors, 0, r, 0, constructors.length);
        return r;
    }

    /** A fresh copy on every call. */
    public MBeanNotificationInfo[] getNotifications() {
        MBeanNotificationInfo[] r = new MBeanNotificationInfo[notifications.length];
        System.arraycopy(notifications, 0, r, 0, notifications.length);
        return r;
    }

    /** Never {@code null}. */
    public Descriptor getDescriptor() {
        return descriptor == null ? ImmutableDescriptor.EMPTY_DESCRIPTOR : descriptor;
    }

    public String toString() {
        return getClass().getName()
                + "[description=" + getDescription()
                + ", attributes=" + asText(attributes)
                + ", constructors=" + asText(constructors)
                + ", operations=" + asText(operations)
                + ", notifications=" + asText(notifications)
                + ", descriptor=" + getDescriptor()
                + "]";
    }

    /**
     * {@code [a, b, c]}, like {@code Arrays.toString}'s.
     *
     * <p>It lives here and is package-private because the five {@code MBean*Info} classes share it.
     * (An earlier note said this library had no {@code Arrays.toString} for object arrays; it has
     * one now, and this helper simply predates it.)
     */
    static String asText(Object[] a) {
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
        return MBeanFeatureInfo.same(p.getClassName(), getClassName())
                && MBeanFeatureInfo.same(p.getDescription(), getDescription())
                && p.getDescriptor().equals(getDescriptor())
                && Arrays.equals(p.attributes, attributes)
                && Arrays.equals(p.operations, operations)
                && Arrays.equals(p.constructors, constructors)
                && Arrays.equals(p.notifications, notifications);
    }

    /** Computed once: the object is immutable. */
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
