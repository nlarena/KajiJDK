package javax.management.relation;

import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

/**
 * A concrete relation: which MBeans occupy each role.
 *
 * <h2>What the relation service solves</h2>
 *
 * <p>JMX models managed objects in isolation. When there are links between them --this server hosts
 * these applications, this disk belongs to this machine-- each MBean could keep the other's
 * {@link ObjectName} in an attribute, and that is where the problems start: nobody keeps things
 * consistent when one is unregistered, there is no way to walk the link backwards, and the
 * cardinality is written nowhere.
 *
 * <p>This takes it out of the MBeans and puts it in a service that can guarantee it.
 *
 * <h2>Why almost everything returns {@link RoleResult} instead of throwing</h2>
 *
 * <p>Because an operation over several roles may fail on some: see {@link RoleUnresolved}. The
 * methods that work on <b>one</b> do throw, because there is no good half there.
 *
 * <h2>Who implements it</h2>
 *
 * <p>{@link RelationSupport} for the normal case. Implementing it directly serves for a relation
 * whose roles are <em>computed</em> instead of stored -- all the machines in a rack, for example,
 * derived from something else.
 */
public interface Relation {

    /**
     * The MBeans that occupy that role.
     *
     * @throws RoleNotFoundException if it does not exist or cannot be read
     */
    List<ObjectName> getRole(String roleName)
            throws IllegalArgumentException, RoleNotFoundException,
            RelationServiceNotRegisteredException;

    /** Several roles at once; the ones that fail come as {@link RoleUnresolved}. */
    RoleResult getRoles(String[] roleNameArray)
            throws IllegalArgumentException, RelationServiceNotRegisteredException;

    /**
     * How many MBeans that role has.
     *
     * @throws RoleNotFoundException if the role does not exist
     */
    Integer getRoleCardinality(String roleName)
            throws IllegalArgumentException, RoleNotFoundException;

    /** All the readable roles, with the unreadable ones apart. */
    RoleResult getAllRoles() throws RelationServiceNotRegisteredException;

    /**
     * All the roles, <b>without</b> checking whether they are readable.
     *
     * <p>It is the internal access: the relation service uses it, having already decided it may
     * look. That is why it returns a bare {@link RoleList} and not a {@link RoleResult} -- there is
     * nothing here that can be left unresolved.
     */
    RoleList retrieveAllRoles();

    /**
     * Changes a role's value.
     *
     * @throws InvalidRoleValueException if the value does not meet what the {@link RoleInfo}
     *     requires
     * @throws RoleNotFoundException if the role does not exist or cannot be written
     */
    void setRole(Role role)
            throws IllegalArgumentException, RoleNotFoundException,
            RelationTypeNotFoundException, InvalidRoleValueException,
            RelationServiceNotRegisteredException, RelationNotFoundException;

    /** Changes several; the ones that fail come as {@link RoleUnresolved}. */
    RoleResult setRoles(RoleList roleList)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            RelationTypeNotFoundException, RelationNotFoundException;

    /**
     * Tells the relation that a referenced MBean was unregistered.
     *
     * <p>The service, which is the one listening to the MBean server, calls it. The relation
     * removes that reference from its roles -- and there it may fall below the minimum, which is
     * how a relation comes to be in breach without anyone having touched it.
     */
    void handleMBeanUnregistration(ObjectName objectName, String roleName)
            throws IllegalArgumentException, RoleNotFoundException, InvalidRoleValueException,
            RelationServiceNotRegisteredException, RelationTypeNotFoundException,
            RelationNotFoundException;

    /**
     * All the referenced MBeans, and in which roles each appears.
     *
     * <p>It is the index the other way round: given an MBean, which roles it is in. The service
     * uses it to know whom to notify when one is unregistered.
     */
    Map<ObjectName, List<String>> getReferencedMBeans();

    /** The name of this relation's type. */
    String getRelationTypeName();

    /** The name of the relation service that manages it. */
    ObjectName getRelationServiceName();

    /** This relation's identifier, unique within the service. */
    String getRelationId();
}
