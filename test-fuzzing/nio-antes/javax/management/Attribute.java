package javax.management;

import java.io.Serializable;

/**
 * A name-value pair: what is read from or written to an MBean in one go.
 *
 * <p>It is immutable and has no declared type: the value is an {@code Object} because the same pair
 * has to be able to travel through {@link MBeanServer#setAttribute} without the server knowing
 * anything about the MBean's class. The type is declared separately by {@link MBeanAttributeInfo};
 * here only the data goes.
 */
public class Attribute implements Serializable {

    private static final long serialVersionUID = 2484220110589082382L;

    /**
     * @serial the attribute name
     */
    private String name;

    /**
     * @serial the value
     */
    private Object value;

    /**
     * @throws RuntimeOperationsException wrapping an {@code IllegalArgumentException} if {@code
     *     name} is {@code null}. It is unchecked on purpose: a null name is a program error, not a
     *     condition worth handling.
     */
    public Attribute(String name, Object value) {
        if (name == null) {
            throw new RuntimeOperationsException(
                    new IllegalArgumentException("Attribute name cannot be null"));
        }
        this.name = name;
        this.value = value;
    }

    /** The attribute name. */
    public String getName() {
        return name;
    }

    /** The value, which may be {@code null}. */
    public Object getValue() {
        return value;
    }

    /** By name and value; a {@code null} value only equals another {@code null}. */
    public boolean equals(Object object) {
        if (!(object instanceof Attribute)) {
            return false;
        }
        Attribute other = (Attribute) object;
        if (value == null) {
            return other.getValue() == null && name.equals(other.getName());
        }
        return name.equals(other.getName()) && value.equals(other.getValue());
    }

    public int hashCode() {
        return name.hashCode() ^ (value == null ? 0 : value.hashCode());
    }

    /** {@code name = value}. */
    public String toString() {
        return getName() + " = " + getValue();
    }
}
