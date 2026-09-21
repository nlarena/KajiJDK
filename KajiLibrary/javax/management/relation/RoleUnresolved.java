package javax.management.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

/**
 * A role that could not be read or written, with the reason.
 *
 * <h2>Why this type exists</h2>
 *
 * <p>Because an operation over several roles is not all or nothing. Reading five roles may give
 * three values and two problems, and both are useful information: stopping at the first problem
 * would lose the three that were there.
 *
 * <p>This object is the "problem" half of that answer; the other is the {@link Role}s that did
 * resolve. The two travel together in a {@link RoleResult}.
 *
 * <p>The value <b>is kept</b> even though it failed: in a rejected write, it is what allows seeing
 * what was being put and not only that it could not be.
 */
public class RoleUnresolved implements Serializable {

    private static final long serialVersionUID = -48350262537070138L;

    private String roleName;
    private List<ObjectName> roleValue;
    private int problemType;

    /**
     * @param pbType one of the {@link RoleStatus} codes
     * @throws IllegalArgumentException if the name is missing or the code is not a
     *     {@link RoleStatus} one
     */
    public RoleUnresolved(String name, List<ObjectName> value, int pbType)
            throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("the role name is missing");
        }
        setProblemType(pbType);
        this.roleName = name;
        this.roleValue = value == null ? null : new ArrayList<ObjectName>(value);
    }

    /** The role's name. */
    public String getRoleName() {
        return this.roleName;
    }

    /** What was being put, or what was there; {@code null} if it does not apply. */
    public List<ObjectName> getRoleValue() {
        return this.roleValue;
    }

    /** The {@link RoleStatus} code that explains the problem. */
    public int getProblemType() {
        return this.problemType;
    }

    /**
     * @throws IllegalArgumentException if it is {@code null}
     */
    public void setRoleName(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("the name cannot be null");
        }
        this.roleName = name;
    }

    /** Sets the value; {@code null} removes it. */
    public void setRoleValue(List<ObjectName> value) {
        this.roleValue = value == null ? null : new ArrayList<ObjectName>(value);
    }

    /**
     * @throws IllegalArgumentException if it is not a {@link RoleStatus} code -- accepting any
     *     integer would let through a problem nobody can interpret afterwards
     */
    public void setProblemType(int pbType) throws IllegalArgumentException {
        if (!RoleStatus.isRoleStatus(pbType)) {
            throw new IllegalArgumentException(
                    "not a RoleStatus code: " + String.valueOf(pbType));
        }
        this.problemType = pbType;
    }

    /** A copy, with its own list. */
    public Object clone() {
        try {
            return new RoleUnresolved(this.roleName, this.roleValue, this.problemType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("role name: ").append(this.roleName);
        if (this.roleValue != null) {
            sb.append("; value: ").append(Role.roleValueToString(this.roleValue));
        }
        sb.append("; problem type: ").append(String.valueOf(this.problemType));
        return sb.toString();
    }
}
