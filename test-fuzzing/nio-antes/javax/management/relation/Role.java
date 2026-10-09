package javax.management.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

/**
 * A role with its value: the name, and the MBeans that occupy that end of the relation today.
 *
 * <p>It is the instance of what {@link RoleInfo} describes. That one says "an owner, exactly one,
 * of class {@code Person}"; this one says "the owner is <em>this</em> MBean".
 *
 * <p>It is mutable --it has setters-- because it is used as an <b>argument</b>: you build one, pass
 * it to {@code setRole} and can reuse it by changing its value. The relation service keeps a copy,
 * so modifying it afterwards does not change the relation.
 */
public class Role implements Serializable {

    private static final long serialVersionUID = -279985518429862552L;

    private String name;
    private List<ObjectName> objectNameList = new ArrayList<ObjectName>();

    /**
     * @throws IllegalArgumentException if the name or the list are missing
     */
    public Role(String roleName, List<ObjectName> roleValue) throws IllegalArgumentException {
        if (roleName == null || roleValue == null) {
            throw new IllegalArgumentException("the name and the value are required");
        }
        this.name = roleName;
        this.objectNameList = new ArrayList<ObjectName>(roleValue);
    }

    /** The role's name. */
    public String getRoleName() {
        return this.name;
    }

    /** The MBeans that occupy it. */
    public List<ObjectName> getRoleValue() {
        return this.objectNameList;
    }

    /**
     * @throws IllegalArgumentException if it is {@code null}
     */
    public void setRoleName(String roleName) throws IllegalArgumentException {
        if (roleName == null) {
            throw new IllegalArgumentException("the name cannot be null");
        }
        this.name = roleName;
    }

    /**
     * @throws IllegalArgumentException if it is {@code null}
     */
    public void setRoleValue(List<ObjectName> roleValue) throws IllegalArgumentException {
        if (roleValue == null) {
            throw new IllegalArgumentException("the value cannot be null");
        }
        this.objectNameList = new ArrayList<ObjectName>(roleValue);
    }

    public String toString() {
        return "role name: " + this.name + "; role value: "
                + roleValueToString(this.objectNameList);
    }

    /**
     * A copy.
     *
     * <p>It copies the list too: a copy that shared the value with the original would make changing
     * one change the other, which is the opposite of what a {@code clone} promises.
     */
    public Object clone() {
        try {
            return new Role(this.name, this.objectNameList);
        } catch (IllegalArgumentException e) {
            // Impossible: both already passed validation when this object was built.
            return null;
        }
    }

    /** The MBean names, one per line. It is how they are shown in a log. */
    public static String roleValueToString(List<ObjectName> roleValue)
            throws IllegalArgumentException {
        if (roleValue == null) {
            throw new IllegalArgumentException("the value cannot be null");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roleValue.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(roleValue.get(i).toString());
        }
        return sb.toString();
    }
}
