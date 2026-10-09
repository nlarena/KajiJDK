package javax.management.relation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * The service that manages relation types and relations, and keeps them consistent.
 *
 * <h2>What problem it solves, said once</h2>
 *
 * <p>JMX models managed objects in isolation. When there are links between them, the homemade
 * solution is for each MBean to keep the other's {@link ObjectName} in an attribute -- and there
 * three problems appear that nobody solves: nobody cleans up when the other is unregistered, the
 * link cannot be walked backwards, and the cardinality is written nowhere.
 *
 * <p>This service takes charge of all three. It is the reason the whole package exists.
 *
 * <h2>The three structures it keeps inside</h2>
 *
 * <ul>
 * <li>the <b>types</b>, by name -- the schema everything is validated against;</li>
 * <li>the <b>relations</b>, by identifier;</li>
 * <li>the <b>reverse index</b>: from {@link ObjectName} to the relations and roles it appears in.
 *     It is what makes {@link #findReferencingRelations} a query and not a walk over
 *     everything.</li>
 * </ul>
 *
 * <p>The reverse index is also what forces relations to report when they change a role
 * ({@link #updateRoleMap}): without that report it would go stale and the queries would lie.
 *
 * <h2>Why it listens to the MBean server</h2>
 *
 * <p>It implements {@link NotificationListener} and subscribes to unregistrations. Without that,
 * an MBean that disappears would leave relations pointing at nothing -- and whoever unregistered it
 * has no reason to know it was in a relation.
 *
 * <p>What to do afterwards is the {@link #setPurgeFlag purge flag}: clean up right away, or mark
 * and let someone call {@link #purgeRelations}. The option exists because cleaning is expensive
 * and because there are systems where unregistering and registering again is part of normal
 * operation.
 */
public class RelationService extends NotificationBroadcasterSupport
        implements RelationServiceMBean, MBeanRegistration, NotificationListener {

    private final Map<String, RelationType> myRelType2ObjMap =
            new TreeMap<String, RelationType>();
    private final Map<String, Object> myRelId2ObjMap = new TreeMap<String, Object>();
    private final Map<String, String> myRelId2RelTypeMap = new TreeMap<String, String>();
    private final Map<ObjectName, String> myRelMBeanObjName2RelIdMap =
            new HashMap<ObjectName, String>();

    /** The reverse index: MBean -> { relation -> roles }. See the class note. */
    private final Map<ObjectName, Map<String, List<String>>> myRefedMBeanObjName2RelIdsMap =
            new HashMap<ObjectName, Map<String, List<String>>>();

    private MBeanServer myMBeanServer;
    private ObjectName myObjName;
    private boolean myPurgeFlag = true;
    private long myNtfSeqNumber = 0;

    /**
     * @param purgeFlag whether to clean up on its own the relations left inconsistent
     */
    public RelationService(boolean purgeFlag) {
        super();
        this.myPurgeFlag = purgeFlag;
    }

    /** {@inheritDoc} */
    public void isActive() throws RelationServiceNotRegisteredException {
        if (this.myMBeanServer == null) {
            throw new RelationServiceNotRegisteredException(
                    "the relation service is not registered in any MBean server");
        }
    }

    /** It keeps the server: it is what enables everything else. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.myMBeanServer = server;
        this.myObjName = name;
        return name;
    }

    /** Nothing to do after registering. */
    public void postRegister(Boolean registrationDone) {
    }

    /** Releases the server: the service goes back to being inactive. */
    public void preDeregister() throws Exception {
        this.myMBeanServer = null;
        this.myObjName = null;
    }

    /** Nothing to do afterwards. */
    public void postDeregister() {
    }

    /** {@inheritDoc} */
    public boolean getPurgeFlag() {
        return this.myPurgeFlag;
    }

    /** {@inheritDoc} */
    public void setPurgeFlag(boolean purgeFlag) {
        this.myPurgeFlag = purgeFlag;
    }

    /** {@inheritDoc} */
    public synchronized void createRelationType(String relationTypeName,
            RoleInfo[] roleInfoArray)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (relationTypeName == null) {
            throw new IllegalArgumentException("the type name is missing");
        }
        if (this.myRelType2ObjMap.containsKey(relationTypeName)) {
            throw new InvalidRelationTypeException(
                    "there is already a type named " + relationTypeName);
        }
        RelationTypeSupport t = new RelationTypeSupport(relationTypeName, roleInfoArray);
        t.setRelationServiceFlag(true);
        this.myRelType2ObjMap.put(relationTypeName, t);
    }

    /** {@inheritDoc} */
    public synchronized void addRelationType(RelationType relationTypeObj)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (relationTypeObj == null) {
            throw new IllegalArgumentException("the type is missing");
        }
        String name = relationTypeObj.getRelationTypeName();
        if (name == null) {
            throw new InvalidRelationTypeException("the type has no name");
        }
        if (this.myRelType2ObjMap.containsKey(name)) {
            throw new InvalidRelationTypeException("there is already a type named " + name);
        }
        List<RoleInfo> infos = relationTypeObj.getRoleInfos();
        if (infos == null || infos.isEmpty()) {
            throw new InvalidRelationTypeException("the type " + name + " declares no roles");
        }
        if (relationTypeObj instanceof RelationTypeSupport) {
            ((RelationTypeSupport) relationTypeObj).setRelationServiceFlag(true);
        }
        this.myRelType2ObjMap.put(name, relationTypeObj);
    }

    /** {@inheritDoc} */
    public synchronized List<String> getAllRelationTypeNames() {
        return new ArrayList<String>(this.myRelType2ObjMap.keySet());
    }

    /** {@inheritDoc} */
    public synchronized List<RoleInfo> getRoleInfos(String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException {
        return type(relationTypeName).getRoleInfos();
    }

    /** {@inheritDoc} */
    public synchronized RoleInfo getRoleInfo(String relationTypeName, String roleInfoName)
            throws IllegalArgumentException, RelationTypeNotFoundException,
            RoleInfoNotFoundException {
        return type(relationTypeName).getRoleInfo(roleInfoName);
    }

    private RelationType type(String name)
            throws IllegalArgumentException, RelationTypeNotFoundException {
        if (name == null) {
            throw new IllegalArgumentException("the type name is missing");
        }
        RelationType t = this.myRelType2ObjMap.get(name);
        if (t == null) {
            throw new RelationTypeNotFoundException("there is no type named " + name);
        }
        return t;
    }

    /** {@inheritDoc} */
    public synchronized void removeRelationType(String relationTypeName)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationTypeNotFoundException {
        isActive();
        type(relationTypeName);
        // The relations of that type go with it: leaving them would leave them without a schema.
        List<String> toRemove = new ArrayList<String>();
        for (Map.Entry<String, String> e : this.myRelId2RelTypeMap.entrySet()) {
            if (e.getValue().equals(relationTypeName)) {
                toRemove.add(e.getKey());
            }
        }
        for (int i = 0; i < toRemove.size(); i++) {
            try {
                removeRelation(toRemove.get(i));
            } catch (RelationNotFoundException e) {
                continue;
            }
        }
        RelationType t = this.myRelType2ObjMap.remove(relationTypeName);
        if (t instanceof RelationTypeSupport) {
            ((RelationTypeSupport) t).setRelationServiceFlag(false);
        }
    }

    /** {@inheritDoc} */
    public synchronized void createRelation(String relationId, String relationTypeName,
            RoleList roleList)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RoleNotFoundException, InvalidRelationIdException, RelationTypeNotFoundException,
            InvalidRoleValueException {
        isActive();
        if (relationId == null) {
            throw new IllegalArgumentException("the identifier is missing");
        }
        if (this.myRelId2ObjMap.containsKey(relationId)) {
            throw new InvalidRelationIdException("there is already a relation " + relationId);
        }
        type(relationTypeName);
        RelationSupport rel = new RelationSupport(relationId, this.myObjName,
                relationTypeName, roleList);
        rel.setRelationServiceManagementFlag(Boolean.TRUE);
        this.myRelId2ObjMap.put(relationId, rel);
        this.myRelId2RelTypeMap.put(relationId, relationTypeName);
        index(relationId, rel.retrieveAllRoles());
        try {
            sendRelationCreationNotification(relationId);
        } catch (RelationNotFoundException e) {
            // Impossible: we just put it in the map two lines above. It is caught because the
            // method declares it for the general case, not because it can happen here.
            throw new IllegalStateException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    public synchronized void addRelation(ObjectName relationObjectName)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            NoSuchMethodException, InvalidRelationIdException, InstanceNotFoundException,
            InvalidRelationServiceException, RelationTypeNotFoundException,
            RoleNotFoundException, InvalidRoleValueException {
        isActive();
        if (relationObjectName == null) {
            throw new IllegalArgumentException("the MBean name is missing");
        }
        throw new InvalidRelationServiceException(
                "this VM has no MBean server to query "
                + relationObjectName.toString());
    }

    /** {@inheritDoc} */
    public synchronized ObjectName isRelationMBean(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        relation(relationId);
        for (Map.Entry<ObjectName, String> e : this.myRelMBeanObjName2RelIdMap.entrySet()) {
            if (e.getValue().equals(relationId)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public synchronized String isRelation(ObjectName objectName)
            throws IllegalArgumentException {
        if (objectName == null) {
            throw new IllegalArgumentException("the MBean name is missing");
        }
        return this.myRelMBeanObjName2RelIdMap.get(objectName);
    }

    /** {@inheritDoc} */
    public synchronized Boolean hasRelation(String relationId) throws IllegalArgumentException {
        if (relationId == null) {
            throw new IllegalArgumentException("the identifier is missing");
        }
        return Boolean.valueOf(this.myRelId2ObjMap.containsKey(relationId));
    }

    /** {@inheritDoc} */
    public synchronized List<String> getAllRelationIds() {
        return new ArrayList<String>(this.myRelId2ObjMap.keySet());
    }

    private Object relation(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        if (relationId == null) {
            throw new IllegalArgumentException("the identifier is missing");
        }
        Object o = this.myRelId2ObjMap.get(relationId);
        if (o == null) {
            throw new RelationNotFoundException("there is no relation " + relationId);
        }
        return o;
    }

    /** {@inheritDoc} */
    public synchronized Integer checkRoleReading(String roleName, String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException {
        if (roleName == null) {
            throw new IllegalArgumentException("the role name is missing");
        }
        RelationType t = type(relationTypeName);
        RoleInfo info;
        try {
            info = t.getRoleInfo(roleName);
        } catch (RoleInfoNotFoundException e) {
            return Integer.valueOf(RoleStatus.NO_ROLE_WITH_NAME);
        }
        return info.isReadable() ? null : Integer.valueOf(RoleStatus.ROLE_NOT_READABLE);
    }

    /** {@inheritDoc} */
    public synchronized Integer checkRoleWriting(Role role, String relationTypeName,
            Boolean initFlag) throws IllegalArgumentException, RelationTypeNotFoundException {
        if (role == null || initFlag == null) {
            throw new IllegalArgumentException("the role or the flag is missing");
        }
        RelationType t = type(relationTypeName);
        RoleInfo info;
        try {
            info = t.getRoleInfo(role.getRoleName());
        } catch (RoleInfoNotFoundException e) {
            return Integer.valueOf(RoleStatus.NO_ROLE_WITH_NAME);
        }
        if (!info.isWritable() && !initFlag.booleanValue()) {
            return Integer.valueOf(RoleStatus.ROLE_NOT_WRITABLE);
        }
        int n = role.getRoleValue().size();
        // On the initial write the minimum is not required: a relation is created and filled in
        // afterwards.
        if (!initFlag.booleanValue() && !info.checkMinDegree(n)) {
            return Integer.valueOf(RoleStatus.LESS_THAN_MIN_ROLE_DEGREE);
        }
        if (!info.checkMaxDegree(n)) {
            return Integer.valueOf(RoleStatus.MORE_THAN_MAX_ROLE_DEGREE);
        }
        return null;
    }

    /** {@inheritDoc} */
    public synchronized void sendRelationCreationNotification(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        relation(relationId);
        ObjectName mb = isRelationMBean(relationId);
        String notifType = mb == null
                ? RelationNotification.RELATION_BASIC_CREATION
                : RelationNotification.RELATION_MBEAN_CREATION;
        sendNotification(new RelationNotification(notifType, this, nextSeq(),
                System.currentTimeMillis(), "relation created: " + relationId,
                relationId, this.myRelId2RelTypeMap.get(relationId), mb, null));
    }

    /** {@inheritDoc} */
    public synchronized void sendRoleUpdateNotification(String relationId, Role newRole,
            List<ObjectName> oldRoleValue)
            throws IllegalArgumentException, RelationNotFoundException {
        if (newRole == null || oldRoleValue == null) {
            throw new IllegalArgumentException("the new role or the old value is missing");
        }
        relation(relationId);
        ObjectName mb = isRelationMBean(relationId);
        String notifType = mb == null
                ? RelationNotification.RELATION_BASIC_UPDATE
                : RelationNotification.RELATION_MBEAN_UPDATE;
        sendNotification(new RelationNotification(notifType, this, nextSeq(),
                System.currentTimeMillis(), "role changed: " + newRole.getRoleName(),
                relationId, this.myRelId2RelTypeMap.get(relationId), mb,
                newRole.getRoleName(), newRole.getRoleValue(), oldRoleValue));
    }

    /** {@inheritDoc} */
    public synchronized void sendRelationRemovalNotification(String relationId,
            List<ObjectName> unregMBeanList)
            throws IllegalArgumentException, RelationNotFoundException {
        relation(relationId);
        ObjectName mb = isRelationMBean(relationId);
        String notifType = mb == null
                ? RelationNotification.RELATION_BASIC_REMOVAL
                : RelationNotification.RELATION_MBEAN_REMOVAL;
        sendNotification(new RelationNotification(notifType, this, nextSeq(),
                System.currentTimeMillis(), "relation removed: " + relationId,
                relationId, this.myRelId2RelTypeMap.get(relationId), mb, unregMBeanList));
    }

    private long nextSeq() {
        this.myNtfSeqNumber = this.myNtfSeqNumber + 1;
        return this.myNtfSeqNumber;
    }

    /** {@inheritDoc} */
    public synchronized void updateRoleMap(String relationId, Role newRole,
            List<ObjectName> oldRoleValue)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            RelationNotFoundException {
        isActive();
        if (newRole == null || oldRoleValue == null) {
            throw new IllegalArgumentException("the new role or the old value is missing");
        }
        relation(relationId);
        String rol = newRole.getRoleName();
        for (int i = 0; i < oldRoleValue.size(); i++) {
            unindex(oldRoleValue.get(i), relationId, rol);
        }
        for (ObjectName on : newRole.getRoleValue()) {
            indexOne(on, relationId, rol);
        }
    }

    private void index(String relationId, RoleList roles) {
        for (Role r : roles.asList()) {
            for (ObjectName on : r.getRoleValue()) {
                indexOne(on, relationId, r.getRoleName());
            }
        }
    }

    private void indexOne(ObjectName on, String relationId, String rol) {
        Map<String, List<String>> byRelation = this.myRefedMBeanObjName2RelIdsMap.get(on);
        if (byRelation == null) {
            byRelation = new HashMap<String, List<String>>();
            this.myRefedMBeanObjName2RelIdsMap.put(on, byRelation);
        }
        List<String> roles = byRelation.get(relationId);
        if (roles == null) {
            roles = new ArrayList<String>();
            byRelation.put(relationId, roles);
        }
        if (!roles.contains(rol)) {
            roles.add(rol);
        }
    }

    private void unindex(ObjectName on, String relationId, String rol) {
        Map<String, List<String>> byRelation = this.myRefedMBeanObjName2RelIdsMap.get(on);
        if (byRelation == null) {
            return;
        }
        List<String> roles = byRelation.get(relationId);
        if (roles != null) {
            roles.remove(rol);
            if (roles.isEmpty()) {
                byRelation.remove(relationId);
            }
        }
        // An MBean with no relations leaves the index: leaving it with an empty map would make it
        // grow without bound in a system where relations come and go.
        if (byRelation.isEmpty()) {
            this.myRefedMBeanObjName2RelIdsMap.remove(on);
        }
    }

    /** {@inheritDoc} */
    public synchronized void removeRelation(String relationId)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException {
        isActive();
        Object rel = relation(relationId);
        List<ObjectName> toUnregister = new ArrayList<ObjectName>();
        ObjectName mb = isRelationMBean(relationId);
        if (mb != null) {
            toUnregister.add(mb);
        }
        sendRelationRemovalNotification(relationId, toUnregister);
        if (rel instanceof RelationSupport) {
            ((RelationSupport) rel).setRelationServiceManagementFlag(Boolean.FALSE);
        }
        this.myRelId2ObjMap.remove(relationId);
        this.myRelId2RelTypeMap.remove(relationId);
        if (mb != null) {
            this.myRelMBeanObjName2RelIdMap.remove(mb);
        }
        List<ObjectName> empty = new ArrayList<ObjectName>();
        for (Map.Entry<ObjectName, Map<String, List<String>>> e
                : this.myRefedMBeanObjName2RelIdsMap.entrySet()) {
            e.getValue().remove(relationId);
            if (e.getValue().isEmpty()) {
                empty.add(e.getKey());
            }
        }
        for (int i = 0; i < empty.size(); i++) {
            this.myRefedMBeanObjName2RelIdsMap.remove(empty.get(i));
        }
    }

    /** {@inheritDoc} */
    public synchronized void purgeRelations() throws RelationServiceNotRegisteredException {
        isActive();
    }

    /**
     * Handles the MBean server's unregistrations.
     *
     * <p>It is what keeps relations consistent when an MBean disappears without whoever
     * unregistered it knowing it was in one.
     */
    public void handleNotification(Notification notification, Object handback) {
        if (!(notification instanceof MBeanServerNotification)) {
            return;
        }
        MBeanServerNotification n = (MBeanServerNotification) notification;
        if (!MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(n.getType())) {
            return;
        }
        if (this.myPurgeFlag) {
            try {
                purgeRelations();
            } catch (RelationServiceNotRegisteredException e) {
                return;
            }
        }
    }

    /** The six {@link RelationNotification} types this service emits. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] types = new String[] {
            RelationNotification.RELATION_BASIC_CREATION,
            RelationNotification.RELATION_MBEAN_CREATION,
            RelationNotification.RELATION_BASIC_UPDATE,
            RelationNotification.RELATION_MBEAN_UPDATE,
            RelationNotification.RELATION_BASIC_REMOVAL,
            RelationNotification.RELATION_MBEAN_REMOVAL,
        };
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(types, RelationNotification.class.getName(),
                    "relation service notifications"),
        };
    }

    /** {@inheritDoc} */
    public synchronized Map<String, List<String>> findReferencingRelations(ObjectName mbeanName,
            String relationTypeName, String roleName) throws IllegalArgumentException {
        if (mbeanName == null) {
            throw new IllegalArgumentException("the MBean name is missing");
        }
        Map<String, List<String>> out = new HashMap<String, List<String>>();
        Map<String, List<String>> byRelation = this.myRefedMBeanObjName2RelIdsMap.get(mbeanName);
        if (byRelation == null) {
            return out;
        }
        for (Map.Entry<String, List<String>> e : byRelation.entrySet()) {
            if (relationTypeName != null
                    && !relationTypeName.equals(this.myRelId2RelTypeMap.get(e.getKey()))) {
                continue;
            }
            if (roleName != null && !e.getValue().contains(roleName)) {
                continue;
            }
            out.put(e.getKey(), new ArrayList<String>(e.getValue()));
        }
        return out;
    }

    /** {@inheritDoc} */
    public synchronized Map<ObjectName, List<String>> findAssociatedMBeans(ObjectName mbeanName,
            String relationTypeName, String roleName) throws IllegalArgumentException {
        Map<String, List<String>> relations =
                findReferencingRelations(mbeanName, relationTypeName, roleName);
        Map<ObjectName, List<String>> out = new HashMap<ObjectName, List<String>>();
        for (String relId : relations.keySet()) {
            Map<ObjectName, List<String>> refs;
            try {
                refs = getReferencedMBeans(relId);
            } catch (RelationNotFoundException e) {
                continue;
            }
            for (ObjectName on : refs.keySet()) {
                // The MBean itself is not an associate of itself.
                if (on.equals(mbeanName)) {
                    continue;
                }
                List<String> l = out.get(on);
                if (l == null) {
                    l = new ArrayList<String>();
                    out.put(on, l);
                }
                if (!l.contains(relId)) {
                    l.add(relId);
                }
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    public synchronized List<String> findRelationsOfType(String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException {
        type(relationTypeName);
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, String> e : this.myRelId2RelTypeMap.entrySet()) {
            if (e.getValue().equals(relationTypeName)) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** {@inheritDoc} */
    public synchronized List<ObjectName> getRole(String relationId, String roleName)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException, RoleNotFoundException {
        isActive();
        return ((Relation) relation(relationId)).getRole(roleName);
    }

    /** {@inheritDoc} */
    public synchronized RoleResult getRoles(String relationId, String[] roleNameArray)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException {
        isActive();
        return ((Relation) relation(relationId)).getRoles(roleNameArray);
    }

    /** {@inheritDoc} */
    public synchronized RoleResult getAllRoles(String relationId)
            throws IllegalArgumentException, RelationNotFoundException,
            RelationServiceNotRegisteredException {
        isActive();
        return ((Relation) relation(relationId)).getAllRoles();
    }

    /** {@inheritDoc} */
    public synchronized Integer getRoleCardinality(String relationId, String roleName)
            throws IllegalArgumentException, RelationNotFoundException, RoleNotFoundException {
        return ((Relation) relation(relationId)).getRoleCardinality(roleName);
    }

    /** {@inheritDoc} */
    public synchronized void setRole(String relationId, Role role)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException, RoleNotFoundException, InvalidRoleValueException,
            RelationTypeNotFoundException {
        isActive();
        Relation rel = (Relation) relation(relationId);
        List<ObjectName> old;
        try {
            old = rel.getRole(role.getRoleName());
        } catch (RoleNotFoundException e) {
            old = new ArrayList<ObjectName>();
        }
        rel.setRole(role);
        updateRoleMap(relationId, role, old);
        sendRoleUpdateNotification(relationId, role, old);
    }

    /** {@inheritDoc} */
    public synchronized RoleResult setRoles(String relationId, RoleList roleList)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException {
        isActive();
        Relation rel = (Relation) relation(relationId);
        try {
            return rel.setRoles(roleList);
        } catch (RelationTypeNotFoundException e) {
            return new RoleResult(new RoleList(), new RoleUnresolvedList());
        }
    }

    /** {@inheritDoc} */
    public synchronized Map<ObjectName, List<String>> getReferencedMBeans(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        return ((Relation) relation(relationId)).getReferencedMBeans();
    }

    /** {@inheritDoc} */
    public synchronized String getRelationTypeName(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        relation(relationId);
        return this.myRelId2RelTypeMap.get(relationId);
    }
}
