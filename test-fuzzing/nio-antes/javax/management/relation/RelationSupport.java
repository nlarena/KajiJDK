package javax.management.relation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * The {@link Relation} implementation the JDK ships: the roles are kept in a map.
 *
 * <h2>The two ways of living</h2>
 *
 * <p>A relation can be <b>internal</b> --the service keeps it and nobody else sees it-- or a
 * <b>registered MBean</b>, visible from a console. This class serves for both, and hence it
 * implements {@link MBeanRegistration}: when it is registered, the server tells it.
 *
 * <p>{@link #isInRelationService} says which one it is in. It matters because almost no operation
 * works before the service takes it: without it there is nobody to ask about the relation's type
 * nor anything to verify that the referenced MBeans exist with.
 *
 * <h2>Why the methods delegate to the service</h2>
 *
 * <p>It shows in the {@code ...Int} methods below, which take the {@link RelationService} as an
 * argument. The reason is that this class <b>cannot validate on its own</b>: to know whether a role
 * is writable you have to look at the type's {@link RoleInfo}, and the type is held by the
 * service.
 *
 * <p>The consequence is the surprising one: a relation just built and not yet added to the service
 * rejects almost everything with {@link RelationServiceNotRegisteredException}.
 */
public class RelationSupport implements RelationSupportMBean, MBeanRegistration {

    private final String myRelId;
    private final ObjectName myRelServiceName;
    private final String myRelTypeName;
    private final Map<String, Role> myRoleName2ValueMap = new TreeMap<String, Role>();

    private MBeanServer myRelServiceMBeanServer;
    private boolean myInRelServFlg = false;

    /**
     * An internal relation.
     *
     * @throws IllegalArgumentException if something is missing
     * @throws InvalidRoleValueException if two roles have the same name
     */
    public RelationSupport(String relationId, ObjectName relationServiceName,
            String relationTypeName, RoleList list)
            throws InvalidRoleValueException, IllegalArgumentException {
        check(relationId, relationServiceName, relationTypeName);
        this.myRelId = relationId;
        this.myRelServiceName = relationServiceName;
        this.myRelTypeName = relationTypeName;
        load(list);
    }

    /**
     * A relation that will be an MBean, with the server where the service lives.
     *
     * @throws IllegalArgumentException if something is missing
     * @throws InvalidRoleValueException if two roles have the same name
     */
    public RelationSupport(String relationId, ObjectName relationServiceName,
            MBeanServer relationServiceMBeanServer, String relationTypeName, RoleList list)
            throws InvalidRoleValueException, IllegalArgumentException {
        this(relationId, relationServiceName, relationTypeName, list);
        if (relationServiceMBeanServer == null) {
            throw new IllegalArgumentException("the MBean server is missing");
        }
        this.myRelServiceMBeanServer = relationServiceMBeanServer;
    }

    private static void check(String id, ObjectName svc, String type) {
        if (id == null || svc == null || type == null) {
            throw new IllegalArgumentException(
                    "the identifier, the service and the type are required");
        }
    }

    private void load(RoleList list) throws InvalidRoleValueException {
        if (list == null) {
            return;
        }
        for (Role r : list.asList()) {
            if (this.myRoleName2ValueMap.containsKey(r.getRoleName())) {
                throw new InvalidRoleValueException(
                        "there are two roles named " + r.getRoleName());
            }
            this.myRoleName2ValueMap.put(r.getRoleName(), (Role) r.clone());
        }
    }

    private void requireService() throws RelationServiceNotRegisteredException {
        if (!this.myInRelServFlg) {
            throw new RelationServiceNotRegisteredException(
                    "the relation " + this.myRelId + " is not in the service yet");
        }
    }

    /** {@inheritDoc} */
    public List<ObjectName> getRole(String roleName)
            throws IllegalArgumentException, RoleNotFoundException,
            RelationServiceNotRegisteredException {
        if (roleName == null) {
            throw new IllegalArgumentException("the role name is missing");
        }
        requireService();
        Role r = this.myRoleName2ValueMap.get(roleName);
        if (r == null) {
            throw new RoleNotFoundException("there is no role named " + roleName);
        }
        return new ArrayList<ObjectName>(r.getRoleValue());
    }

    /** {@inheritDoc} */
    public RoleResult getRoles(String[] roleNameArray)
            throws IllegalArgumentException, RelationServiceNotRegisteredException {
        if (roleNameArray == null) {
            throw new IllegalArgumentException("the array of names is missing");
        }
        requireService();
        RoleList ok = new RoleList();
        RoleUnresolvedList mal = new RoleUnresolvedList();
        for (int i = 0; i < roleNameArray.length; i++) {
            Role r = this.myRoleName2ValueMap.get(roleNameArray[i]);
            if (r == null) {
                mal.add(new RoleUnresolved(roleNameArray[i], null,
                        RoleStatus.NO_ROLE_WITH_NAME));
            } else {
                ok.add((Role) r.clone());
            }
        }
        return new RoleResult(ok, mal);
    }

    /** {@inheritDoc} */
    public RoleResult getAllRoles() throws RelationServiceNotRegisteredException {
        requireService();
        RoleList ok = new RoleList();
        for (Role r : this.myRoleName2ValueMap.values()) {
            ok.add((Role) r.clone());
        }
        return new RoleResult(ok, new RoleUnresolvedList());
    }

    /** {@inheritDoc} */
    public RoleList retrieveAllRoles() {
        RoleList out = new RoleList();
        for (Role r : this.myRoleName2ValueMap.values()) {
            out.add((Role) r.clone());
        }
        return out;
    }

    /** {@inheritDoc} */
    public Integer getRoleCardinality(String roleName)
            throws IllegalArgumentException, RoleNotFoundException {
        if (roleName == null) {
            throw new IllegalArgumentException("the role name is missing");
        }
        Role r = this.myRoleName2ValueMap.get(roleName);
        if (r == null) {
            throw new RoleNotFoundException("there is no role named " + roleName);
        }
        return Integer.valueOf(r.getRoleValue().size());
    }

    /** {@inheritDoc} */
    public void setRole(Role role)
            throws IllegalArgumentException, RoleNotFoundException,
            RelationTypeNotFoundException, InvalidRoleValueException,
            RelationServiceNotRegisteredException, RelationNotFoundException {
        if (role == null) {
            throw new IllegalArgumentException("the role is missing");
        }
        requireService();
        if (!this.myRoleName2ValueMap.containsKey(role.getRoleName())) {
            throw new RoleNotFoundException("there is no role named " + role.getRoleName());
        }
        this.myRoleName2ValueMap.put(role.getRoleName(), (Role) role.clone());
    }

    /** {@inheritDoc} */
    public RoleResult setRoles(RoleList roleList)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            RelationTypeNotFoundException, RelationNotFoundException {
        if (roleList == null) {
            throw new IllegalArgumentException("the role list is missing");
        }
        requireService();
        RoleList ok = new RoleList();
        RoleUnresolvedList mal = new RoleUnresolvedList();
        for (Role r : roleList.asList()) {
            if (this.myRoleName2ValueMap.containsKey(r.getRoleName())) {
                this.myRoleName2ValueMap.put(r.getRoleName(), (Role) r.clone());
                ok.add((Role) r.clone());
            } else {
                mal.add(new RoleUnresolved(r.getRoleName(), r.getRoleValue(),
                        RoleStatus.NO_ROLE_WITH_NAME));
            }
        }
        return new RoleResult(ok, mal);
    }

    /** {@inheritDoc} */
    public void handleMBeanUnregistration(ObjectName objectName, String roleName)
            throws IllegalArgumentException, RoleNotFoundException, InvalidRoleValueException,
            RelationServiceNotRegisteredException, RelationTypeNotFoundException,
            RelationNotFoundException {
        if (objectName == null || roleName == null) {
            throw new IllegalArgumentException("the MBean or the role is missing");
        }
        requireService();
        Role r = this.myRoleName2ValueMap.get(roleName);
        if (r == null) {
            throw new RoleNotFoundException("there is no role named " + roleName);
        }
        List<ObjectName> remaining = new ArrayList<ObjectName>(r.getRoleValue());
        remaining.remove(objectName);
        // It may fall below the RoleInfo's minimum, and that is correct: the relation comes to be
        // in breach, which is exactly what the service detects when purging.
        this.myRoleName2ValueMap.put(roleName, new Role(roleName, remaining));
    }

    /** {@inheritDoc} */
    public Map<ObjectName, List<String>> getReferencedMBeans() {
        Map<ObjectName, List<String>> out = new HashMap<ObjectName, List<String>>();
        for (Role r : this.myRoleName2ValueMap.values()) {
            for (ObjectName on : r.getRoleValue()) {
                List<String> roles = out.get(on);
                if (roles == null) {
                    roles = new ArrayList<String>();
                    out.put(on, roles);
                }
                roles.add(r.getRoleName());
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    public String getRelationTypeName() {
        return this.myRelTypeName;
    }

    /** {@inheritDoc} */
    public ObjectName getRelationServiceName() {
        return this.myRelServiceName;
    }

    /** {@inheritDoc} */
    public String getRelationId() {
        return this.myRelId;
    }

    /** The server it gets registered in; the MBean server calls it. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.myRelServiceMBeanServer = server;
        return name;
    }

    /** Nothing to do after registering. */
    public void postRegister(Boolean registrationDone) {
    }

    /**
     * Before unregistering.
     *
     * <p>It does not remove the relation from the service: the service finds out through its own
     * notification filter, which is what keeps it consistent even when an MBean is unregistered by
     * someone who knows nothing about relations.
     */
    public void preDeregister() throws Exception {
    }

    /** Nothing to do afterwards. */
    public void postDeregister() {
    }

    /** {@inheritDoc} */
    public Boolean isInRelationService() {
        return Boolean.valueOf(this.myInRelServFlg);
    }

    /** {@inheritDoc} */
    public void setRelationServiceManagementFlag(Boolean flag) throws IllegalArgumentException {
        if (flag == null) {
            throw new IllegalArgumentException("the flag cannot be null");
        }
        this.myInRelServFlg = flag.booleanValue();
    }
}
