package javax.management;

import java.io.Serializable;

/**
 * A registered MBean: its name plus the class that implements it.
 *
 * <p>It exists to save a round trip. {@code queryNames} returns names and a client that wants to
 * know which class each one is would have to ask one by one; {@code queryMBeans} returns these and
 * the answer already comes complete. Over a remote connection the difference is one trip against
 * hundreds.
 *
 * <p>The name can <b>not</b> be a pattern, and the constructor checks it: a pattern designates a
 * set and this identifies a single one.
 */
public class ObjectInstance implements Serializable {

    private static final long serialVersionUID = -4099952623687795850L;

    /**
     * @serial the MBean's name
     */
    private ObjectName name;

    /**
     * @serial the name of its Java class
     */
    private String className;

    /** Parses {@code objectName} and delegates. */
    public ObjectInstance(String objectName, String className)
            throws MalformedObjectNameException {
        this(new ObjectName(objectName), className);
    }

    /**
     * @throws RuntimeOperationsException wrapping {@code IllegalArgumentException} if the name is a
     *     pattern
     */
    public ObjectInstance(ObjectName objectName, String className) {
        if (objectName.isPattern()) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Invalid name->" + objectName.toString()));
        }
        this.name = objectName;
        this.className = className;
    }

    /** By name and class; the class may be {@code null} on both sides. */
    public boolean equals(Object object) {
        if (!(object instanceof ObjectInstance)) {
            return false;
        }
        ObjectInstance other = (ObjectInstance) object;
        if (!name.equals(other.getObjectName())) {
            return false;
        }
        if (className == null) {
            return other.getClassName() == null;
        }
        return className.equals(other.getClassName());
    }

    public int hashCode() {
        return name.hashCode() ^ (className == null ? 0 : className.hashCode());
    }

    /** The MBean's name. */
    public ObjectName getObjectName() {
        return name;
    }

    /** The name of the Java class that implements it. */
    public String getClassName() {
        return className;
    }

    /** {@code class[name]}. */
    public String toString() {
        return getClassName() + "[" + getObjectName() + "]";
    }
}
