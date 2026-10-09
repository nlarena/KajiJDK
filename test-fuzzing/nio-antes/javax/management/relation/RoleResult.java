package javax.management.relation;

import java.io.Serializable;

/**
 * The result of an operation over several roles: the ones that went well and the ones that did
 * not.
 *
 * <h2>Why two lists and not an exception</h2>
 *
 * <p>Because reading or writing five roles may give three successes and two problems, and both
 * halves are useful. An exception would force throwing away the three that worked; returning only
 * the successes would hide the failures.
 *
 * <p>It is the reason {@link RoleUnresolved} exists: without a type that represents "this one
 * failed, and for this reason", the bad half could not be returned.
 */
public class RoleResult implements Serializable {

    private static final long serialVersionUID = -6304063118040985512L;

    private RoleList roleList;
    private RoleUnresolvedList unresolvedRoleList;

    /** The two halves; either may be {@code null} or empty. */
    public RoleResult(RoleList list, RoleUnresolvedList unresolvedList) {
        setRoles(list);
        setRolesUnresolved(unresolvedList);
    }

    /** The ones that resolved. */
    public RoleList getRoles() {
        return this.roleList;
    }

    /** The ones that did not, with their reason. */
    public RoleUnresolvedList getRolesUnresolved() {
        return this.unresolvedRoleList;
    }

    /** Sets the resolved ones; {@code null} leaves the list empty and not null. */
    public void setRoles(RoleList list) {
        this.roleList = list == null ? new RoleList() : new RoleList(list.asList());
    }

    /** Sets the unresolved ones. */
    public void setRolesUnresolved(RoleUnresolvedList unresolvedList) {
        this.unresolvedRoleList = unresolvedList == null
                ? new RoleUnresolvedList()
                : new RoleUnresolvedList(unresolvedList.asList());
    }
}
