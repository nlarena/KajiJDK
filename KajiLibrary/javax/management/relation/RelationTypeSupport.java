package javax.management.relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The {@link RelationType} implementation the JDK ships: the roles are declared and stored.
 *
 * <h2>The two ways of using it</h2>
 *
 * <p>Directly, passing the roles to the constructor; or by extending it and calling
 * {@link #addRoleInfo} from the subclass's constructor -- which is why that method is
 * {@code protected} and why the one-argument constructor exists.
 *
 * <p>The second form serves for a type whose roles depend on something computed.
 *
 * <h2>Why it is frozen on registration</h2>
 *
 * <p>Once the relation service has accepted the type, adding roles to it would make it inconsistent
 * with the relations already created against it: they would have one role fewer than their type
 * declares, without anyone having touched them. That is why {@link #addRoleInfo} fails after
 * registration.
 */
public class RelationTypeSupport implements RelationType {

    private static final long serialVersionUID = 4611072955724144607L;

    private final String typeName;
    private final Map<String, RoleInfo> roleName2InfoMap = new TreeMap<String, RoleInfo>();
    private boolean isInRelationService = false;

    /**
     * With its roles.
     *
     * @throws IllegalArgumentException if the name or the roles are missing
     * @throws InvalidRelationTypeException if two roles have the same name, or if one is {@code
     *     null}
     */
    public RelationTypeSupport(String relationTypeName, RoleInfo[] roleInfoArray)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (relationTypeName == null) {
            throw new IllegalArgumentException("the type name is missing");
        }
        checkRoleInfos(roleInfoArray);
        this.typeName = relationTypeName;
        for (int i = 0; i < roleInfoArray.length; i++) {
            this.roleName2InfoMap.put(roleInfoArray[i].getName(), new RoleInfo(roleInfoArray[i]));
        }
    }

    /**
     * For the subclasses, which add the roles with {@link #addRoleInfo}.
     *
     * @throws IllegalArgumentException if the name is missing
     */
    protected RelationTypeSupport(String relationTypeName) {
        if (relationTypeName == null) {
            throw new IllegalArgumentException("the type name is missing");
        }
        this.typeName = relationTypeName;
    }

    /** The type's name. */
    public String getRelationTypeName() {
        return this.typeName;
    }

    /** The roles it declares. */
    public List<RoleInfo> getRoleInfos() {
        return new ArrayList<RoleInfo>(this.roleName2InfoMap.values());
    }

    /**
     * That role's description.
     *
     * @throws RoleInfoNotFoundException if it does not declare it
     */
    public RoleInfo getRoleInfo(String roleInfoName)
            throws IllegalArgumentException, RoleInfoNotFoundException {
        if (roleInfoName == null) {
            throw new IllegalArgumentException("the role name is missing");
        }
        RoleInfo info = this.roleName2InfoMap.get(roleInfoName);
        if (info == null) {
            throw new RoleInfoNotFoundException(
                    "the type " + this.typeName + " does not declare the role " + roleInfoName);
        }
        return info;
    }

    /**
     * Adds a role; only before the type is registered.
     *
     * @throws IllegalStateException if the type is already in the relation service -- see the class
     *     note
     * @throws InvalidRelationTypeException if there is already a role with that name
     */
    protected void addRoleInfo(RoleInfo roleInfo)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (roleInfo == null) {
            throw new IllegalArgumentException("the role cannot be null");
        }
        if (this.isInRelationService) {
            throw new IllegalStateException(
                    "the type is already registered: roles cannot be added to it");
        }
        if (this.roleName2InfoMap.containsKey(roleInfo.getName())) {
            throw new InvalidRelationTypeException(
                    "there is already a role named " + roleInfo.getName());
        }
        this.roleName2InfoMap.put(roleInfo.getName(), new RoleInfo(roleInfo));
    }

    /** The relation service calls it when registering and when removing the type. */
    void setRelationServiceFlag(boolean flag) {
        this.isInRelationService = flag;
    }

    /**
     * Validates an array of roles before accepting it.
     *
     * @throws InvalidRelationTypeException if it is empty, if there is a {@code null} or if two
     *     have the same name -- two homonymous roles would make every access by name ambiguous
     */
    static void checkRoleInfos(RoleInfo[] roleInfoArray)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (roleInfoArray == null) {
            throw new IllegalArgumentException("the array of roles is missing");
        }
        if (roleInfoArray.length == 0) {
            throw new InvalidRelationTypeException("a relation type needs at least one role");
        }
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (int i = 0; i < roleInfoArray.length; i++) {
            RoleInfo r = roleInfoArray[i];
            if (r == null) {
                throw new InvalidRelationTypeException("there is a null role in the array");
            }
            if (!seen.add(r.getName())) {
                throw new InvalidRelationTypeException("there are two roles named " + r.getName());
            }
        }
    }
}
