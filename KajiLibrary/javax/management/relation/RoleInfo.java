package javax.management.relation;

import java.io.Serializable;

/**
 * The description of a role within a relation type: what it is called, what can go in it and how
 * many.
 *
 * <h2>What a role is</h2>
 *
 * <p>A JMX relation connects MBeans, and each end of the connection is a <em>role</em>. In an
 * "owner/resource" relation, {@code owner} and {@code resource} are roles, and this class
 * describes one of them <b>before</b> any concrete relation exists -- it is the schema.
 *
 * <h2>The two degrees, which is what makes the schema useful</h2>
 *
 * <p>{@link #getMinDegree} and {@link #getMaxDegree} say how many MBeans the role may have. With
 * that the cardinality is expressed: {@code (1,1)} is exactly one, {@code (0,1)} optional,
 * {@code (1, INFINITY)} at least one.
 *
 * <p>It is what the relation service checks on every write, and what makes a malformed relation be
 * rejected instead of left inconsistent.
 *
 * <p>{@link #ROLE_CARDINALITY_INFINITY} is {@code -1} and not {@link Integer#MAX_VALUE}: if it were
 * a large number, comparing would be right but {@code maxDegree + 1} would overflow. With
 * {@code -1} the comparison is a separate and explicit case.
 *
 * <h2>Immutable</h2>
 *
 * <p>It has no setters, and it has to be that way: it is the schema everything is validated
 * against, and if it could be changed after being declared, the relations already created would
 * stop meeting it without anyone touching them.
 */
public class RoleInfo implements Serializable {

    private static final long serialVersionUID = 2504952983494636987L;

    /** No upper limit. It is {@code -1}; see the class note. */
    public static final int ROLE_CARDINALITY_INFINITY = -1;

    private final String name;
    private final boolean isReadable;
    private final boolean isWritable;
    private final String description;
    private final int minDegree;
    private final int maxDegree;
    private final String referencedMBeanClassName;

    /**
     * With everything.
     *
     * @param roleName the name; it cannot be {@code null}
     * @param mbeanClassName the class the referenced MBeans must be or extend
     * @param read whether the role can be read
     * @param write whether the role can be written
     * @param min how many MBeans at least
     * @param max how many at most, or {@link #ROLE_CARDINALITY_INFINITY}
     * @param descr a description to show
     * @throws IllegalArgumentException if the name or the class are missing
     * @throws InvalidRoleInfoException if the minimum is greater than the maximum -- such a role
     *     can never be fulfilled, and rejecting it here avoids discovering it only when the
     *     relation is created
     */
    public RoleInfo(String roleName, String mbeanClassName, boolean read, boolean write,
            int min, int max, String descr)
            throws IllegalArgumentException, InvalidRoleInfoException {
        if (roleName == null) {
            throw new IllegalArgumentException("the role name is missing");
        }
        if (mbeanClassName == null) {
            throw new IllegalArgumentException("the class of the referenced MBeans is missing");
        }
        int mn = min;
        int mx = max;
        if (mn == ROLE_CARDINALITY_INFINITY) {
            mn = Integer.MAX_VALUE;
        }
        if (mx == ROLE_CARDINALITY_INFINITY) {
            mx = Integer.MAX_VALUE;
        }
        if (mn > mx) {
            throw new InvalidRoleInfoException(
                    "the minimum degree is greater than the maximum in the role " + roleName);
        }
        this.name = roleName;
        this.referencedMBeanClassName = mbeanClassName;
        this.isReadable = read;
        this.isWritable = write;
        this.minDegree = mn;
        this.maxDegree = mx;
        this.description = descr;
    }

    /** Without a description, with cardinality {@code (1,1)}. */
    public RoleInfo(String roleName, String mbeanClassName, boolean read, boolean write)
            throws IllegalArgumentException {
        this(roleName, mbeanClassName, read, write, 1, 1, null, true);
    }

    /** Readable, writable, exactly one, without a description. */
    public RoleInfo(String roleName, String mbeanClassName) throws IllegalArgumentException {
        this(roleName, mbeanClassName, true, true, 1, 1, null, true);
    }

    /**
     * A copy.
     *
     * @throws IllegalArgumentException if {@code roleInfo} is {@code null}
     */
    public RoleInfo(RoleInfo roleInfo) throws IllegalArgumentException {
        if (roleInfo == null) {
            throw new IllegalArgumentException("there is nothing to copy");
        }
        this.name = roleInfo.getName();
        this.referencedMBeanClassName = roleInfo.getRefMBeanClassName();
        this.isReadable = roleInfo.isReadable();
        this.isWritable = roleInfo.isWritable();
        this.minDegree = roleInfo.getMinDegree();
        this.maxDegree = roleInfo.getMaxDegree();
        this.description = roleInfo.getDescription();
    }

    // The short constructors cannot throw `InvalidRoleInfoException` --it is not in their
    // signature-- and with (1,1) it is impossible to happen. This private one exists only so that
    // the compiler knows.
    private RoleInfo(String roleName, String mbeanClassName, boolean read, boolean write,
            int min, int max, String descr, boolean internal) throws IllegalArgumentException {
        if (roleName == null) {
            throw new IllegalArgumentException("the role name is missing");
        }
        if (mbeanClassName == null) {
            throw new IllegalArgumentException("the class of the referenced MBeans is missing");
        }
        this.name = roleName;
        this.referencedMBeanClassName = mbeanClassName;
        this.isReadable = read;
        this.isWritable = write;
        this.minDegree = min;
        this.maxDegree = max;
        this.description = descr;
    }

    /** The role's name. */
    public String getName() {
        return this.name;
    }

    /** Whether the role can be read. */
    public boolean isReadable() {
        return this.isReadable;
    }

    /** Whether the role can be written. */
    public boolean isWritable() {
        return this.isWritable;
    }

    /** The description, or {@code null}. */
    public String getDescription() {
        return this.description;
    }

    /** How many MBeans at least. */
    public int getMinDegree() {
        return this.minDegree;
    }

    /** How many at most. */
    public int getMaxDegree() {
        return this.maxDegree;
    }

    /** The class the referenced MBeans must be or extend. */
    public String getRefMBeanClassName() {
        return this.referencedMBeanClassName;
    }

    /** Whether {@code value} reaches the minimum. */
    public boolean checkMinDegree(int value) {
        return value >= this.minDegree;
    }

    /** Whether {@code value} does not exceed the maximum. */
    public boolean checkMaxDegree(int value) {
        return value <= this.maxDegree;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("role info name: ").append(this.name);
        sb.append("; isReadable: ").append(String.valueOf(this.isReadable));
        sb.append("; isWritable: ").append(String.valueOf(this.isWritable));
        sb.append("; description: ").append(String.valueOf(this.description));
        sb.append("; minimum degree: ").append(String.valueOf(this.minDegree));
        sb.append("; maximum degree: ").append(String.valueOf(this.maxDegree));
        sb.append("; ObjectName class: ").append(this.referencedMBeanClassName);
        return sb.toString();
    }
}
