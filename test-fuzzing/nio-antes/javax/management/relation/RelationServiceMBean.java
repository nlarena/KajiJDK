package javax.management.relation;

import java.util.List;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

/**
 * The {@link RelationService}'s management interface: everything that can be done with it from a
 * console.
 *
 * <h2>The three families of operations</h2>
 *
 * <ul>
 * <li><b>types</b> -- declaring the schema: {@code createRelationType}, {@code addRelationType},
 *     {@code removeRelationType}. It comes first, because a relation is validated against its
 *     type;</li>
 * <li><b>relations</b> -- creating them, removing them, reading and writing their roles;</li>
 * <li><b>queries</b> -- {@code findReferencingRelations}, {@code findAssociatedMBeans},
 *     {@code findRelationsOfType}. They are what makes the service useful: walking the links
 *     <em>backwards</em>, which is exactly what cannot be done when each MBean keeps the references
 *     on its own.</li>
 * </ul>
 *
 * <h2>The two {@code create} and {@code add}</h2>
 *
 * <p>{@code createRelation} and {@code createRelationType} build the object inside the service;
 * {@code addRelation} and {@code addRelationType} take one already built. The difference matters
 * because only the second form allows a relation that is a registered MBean.
 *
 * <h2>The purge flag, which is the most visible design decision</h2>
 *
 * <p>When a referenced MBean is unregistered, its relations are left inconsistent. With automatic
 * purging the service cleans them right away; without it {@link #purgeRelations} has to be called
 * by hand. The option exists because cleaning is expensive and because there are systems where an
 * MBean is unregistered and registered again as part of its normal operation.
 */
public interface RelationServiceMBean {

    /**
     * @throws RelationServiceNotRegisteredException if the service is not registered in any MBean
     *     server -- without a server it cannot verify anything
     */
    void isActive() throws RelationServiceNotRegisteredException;

    /** Whether the relations left inconsistent are cleaned up on their own. */
    boolean getPurgeFlag();

    /** Changes that policy; see the interface note. */
    void setPurgeFlag(boolean purgeFlag);

    /**
     * Declares a type with those roles.
     *
     * @throws InvalidRelationTypeException if there is already one with that name, or if the roles
     *     are inconsistent
     */
    void createRelationType(String relationTypeName, RoleInfo[] roleInfoArray)
            throws IllegalArgumentException, InvalidRelationTypeException;

    /** Adds an already built type. */
    void addRelationType(RelationType relationTypeObj)
            throws IllegalArgumentException, InvalidRelationTypeException;

    /** The names of the declared types. */
    List<String> getAllRelationTypeNames();

    /**
     * The roles that type declares.
     *
     * @throws RelationTypeNotFoundException if it does not exist
     */
    List<RoleInfo> getRoleInfos(String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException;

    /**
     * The description of a role of that type.
     *
     * @throws RoleInfoNotFoundException if the type does not declare it
     */
    RoleInfo getRoleInfo(String relationTypeName, String roleInfoName)
            throws IllegalArgumentException, RelationTypeNotFoundException,
            RoleInfoNotFoundException;

    /**
     * Removes the type, and with it <b>all the relations of that type</b>.
     *
     * <p>It is not a polite cleanup: leaving them would leave relations with no schema to validate
     * against.
     */
    void removeRelationType(String relationTypeName)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationTypeNotFoundException;

    /**
     * Creates an internally managed relation.
     *
     * @throws InvalidRelationIdException if there is already one with that identifier
     * @throws InvalidRoleValueException if some role does not meet what its description requires
     */
    void createRelation(String relationId, String relationTypeName, RoleList roleList)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RoleNotFoundException, InvalidRelationIdException, RelationTypeNotFoundException,
            InvalidRoleValueException;

    /**
     * Takes a relation that is already a registered MBean.
     *
     * @throws NoSuchMethodException if the MBean does not implement {@link Relation}
     * @throws InstanceNotFoundException if it is not registered
     */
    void addRelation(ObjectName relationObjectName)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            NoSuchMethodException, InvalidRelationIdException, InstanceNotFoundException,
            InvalidRelationServiceException, RelationTypeNotFoundException,
            RoleNotFoundException, InvalidRoleValueException;

    /** The name of that relation's MBean, or {@code null} if it is internal. */
    ObjectName isRelationMBean(String relationId)
            throws IllegalArgumentException, RelationNotFoundException;

    /** The identifier of the relation that is that MBean, or {@code null}. */
    String isRelation(ObjectName objectName) throws IllegalArgumentException;

    /** Whether there is a relation with that identifier. */
    Boolean hasRelation(String relationId) throws IllegalArgumentException;

    /** The identifiers of all the relations. */
    List<String> getAllRelationIds();

    /**
     * Whether that role can be read.
     *
     * @return {@code null} if it can, or a {@link RoleStatus} code if not
     */
    Integer checkRoleReading(String roleName, String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException;

    /**
     * Whether that role can be written with that value.
     *
     * @param initFlag whether the write is the initial one, where the minimum cardinality is not
     *     yet required -- a relation is created empty and filled in afterwards
     * @return {@code null} if it can, or a {@link RoleStatus} code
     */
    Integer checkRoleWriting(Role role, String relationTypeName, Boolean initFlag)
            throws IllegalArgumentException, RelationTypeNotFoundException;

    /** Emits the creation notification. */
    void sendRelationCreationNotification(String relationId)
            throws IllegalArgumentException, RelationNotFoundException;

    /** Emits the role update notification. */
    void sendRoleUpdateNotification(String relationId, Role newRole,
            List<ObjectName> oldRoleValue)
            throws IllegalArgumentException, RelationNotFoundException;

    /** Emits the removal notification. */
    void sendRelationRemovalNotification(String relationId, List<ObjectName> unregMBeanList)
            throws IllegalArgumentException, RelationNotFoundException;

    /**
     * Updates the reverse index from MBean to relations.
     *
     * <p>The relation calls it after changing a role. It is what keeps {@link
     * #findReferencingRelations} useful: without this, the backwards query would have to walk all
     * the relations.
     */
    void updateRoleMap(String relationId, Role newRole, List<ObjectName> oldRoleValue)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            RelationNotFoundException;

    /** Removes the relation. */
    void removeRelation(String relationId)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException;

    /** Cleans up the relations left inconsistent; see the purge flag. */
    void purgeRelations()
            throws RelationServiceNotRegisteredException;

    /**
     * Which relations reference that MBean, and in which roles.
     *
     * @param relationTypeName filters by type, or {@code null} for all
     * @param roleName filters by role, or {@code null} for all
     */
    Map<String, List<String>> findReferencingRelations(ObjectName mbeanName,
            String relationTypeName, String roleName) throws IllegalArgumentException;

    /**
     * Which MBeans are associated with that one, and through which relations.
     *
     * <p>It is the query that justifies the whole service: "what depends on this" cannot be
     * answered when each MBean keeps its own references.
     */
    Map<ObjectName, List<String>> findAssociatedMBeans(ObjectName mbeanName,
            String relationTypeName, String roleName) throws IllegalArgumentException;

    /** The identifiers of the relations of that type. */
    List<String> findRelationsOfType(String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException;

    /** The value of a role of that relation. */
    List<ObjectName> getRole(String relationId, String roleName)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException, RoleNotFoundException;

    /** Several roles at once. */
    RoleResult getRoles(String relationId, String[] roleNameArray)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException;

    /** All the readable roles. */
    RoleResult getAllRoles(String relationId)
            throws IllegalArgumentException, RelationNotFoundException,
            RelationServiceNotRegisteredException;

    /** How many MBeans that role has. */
    Integer getRoleCardinality(String relationId, String roleName)
            throws IllegalArgumentException, RelationNotFoundException, RoleNotFoundException;

    /** Changes a role. */
    void setRole(String relationId, Role role)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException, RoleNotFoundException, InvalidRoleValueException,
            RelationTypeNotFoundException;

    /** Changes several. */
    RoleResult setRoles(String relationId, RoleList roleList)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException;

    /** The MBeans that relation references, and in which roles. */
    Map<ObjectName, List<String>> getReferencedMBeans(String relationId)
            throws IllegalArgumentException, RelationNotFoundException;

    /** That relation's type. */
    String getRelationTypeName(String relationId)
            throws IllegalArgumentException, RelationNotFoundException;
}
