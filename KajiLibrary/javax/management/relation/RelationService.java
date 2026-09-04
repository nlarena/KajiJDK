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
 * El servicio que administra tipos de relacion y relaciones, y las mantiene consistentes.
 *
 * <h2>Que problema resuelve, dicho una vez</h2>
 *
 * <p>JMX modela objetos administrados sueltos. Cuando entre ellos hay vinculos, la solucion casera
 * es que cada MBean guarde el {@link ObjectName} del otro en un atributo — y ahi aparecen tres
 * problemas que nadie resuelve: nadie limpia cuando el otro se desregistra, no se puede recorrer el
 * vinculo al reves, y la cardinalidad no esta escrita en ningun lado.
 *
 * <p>Este servicio se hace cargo de los tres. Es la razon de que exista todo el paquete.
 *
 * <h2>Las tres estructuras que lleva adentro</h2>
 *
 * <ul>
 * <li>los <strong>tipos</strong>, por nombre — el esquema contra el que se valida;</li>
 * <li>las <strong>relaciones</strong>, por identificador;</li>
 * <li>el <strong>indice inverso</strong>: de {@link ObjectName} a las relaciones y roles donde
 *     aparece. Es lo que hace que {@link #findReferencingRelations} sea una consulta y no un
 *     recorrido de todo.</li>
 * </ul>
 *
 * <p>El indice inverso es tambien lo que obliga a que las relaciones avisen cuando cambian un rol
 * ({@link #updateRoleMap}): sin ese aviso quedaria desactualizado y las consultas mentirian.
 *
 * <h2>Por que escucha al servidor de MBeans</h2>
 *
 * <p>Implementa {@link NotificationListener} y se suscribe a los desregistros. Sin eso, un MBean que
 * desaparece dejaria relaciones apuntando a nada — y el que lo desregistro no tiene por que saber
 * que estaba en una relacion.
 *
 * <p>Que hacer despues es la {@link #setPurgeFlag bandera de purga}: limpiar enseguida, o marcar y
 * dejar que alguien llame a {@link #purgeRelations}. Existe la opcion porque limpiar es caro y
 * porque hay sistemas donde desregistrar y volver a registrar es parte de la operacion normal.
 */
public class RelationService extends NotificationBroadcasterSupport
        implements RelationServiceMBean, MBeanRegistration, NotificationListener {

    private final Map<String, RelationType> myRelType2ObjMap =
            new TreeMap<String, RelationType>();
    private final Map<String, Object> myRelId2ObjMap = new TreeMap<String, Object>();
    private final Map<String, String> myRelId2RelTypeMap = new TreeMap<String, String>();
    private final Map<ObjectName, String> myRelMBeanObjName2RelIdMap =
            new HashMap<ObjectName, String>();

    /** El indice inverso: MBean -> { relacion -> roles }. Ver la nota de la clase. */
    private final Map<ObjectName, Map<String, List<String>>> myRefedMBeanObjName2RelIdsMap =
            new HashMap<ObjectName, Map<String, List<String>>>();

    private MBeanServer myMBeanServer;
    private ObjectName myObjName;
    private boolean myPurgeFlag = true;
    private long myNtfSeqNumber = 0;

    /**
     * @param purgeFlag si limpiar sola las relaciones que quedan inconsistentes
     */
    public RelationService(boolean purgeFlag) {
        super();
        this.myPurgeFlag = purgeFlag;
    }

    /** {@inheritDoc} */
    public void isActive() throws RelationServiceNotRegisteredException {
        if (this.myMBeanServer == null) {
            throw new RelationServiceNotRegisteredException(
                    "el servicio de relaciones no esta registrado en ningun servidor de MBeans");
        }
    }

    /** Guarda el servidor: es lo que habilita todo lo demas. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.myMBeanServer = server;
        this.myObjName = name;
        return name;
    }

    /** Sin nada que hacer despues de registrarse. */
    public void postRegister(Boolean registrationDone) {
    }

    /** Suelta el servidor: el servicio vuelve a estar inactivo. */
    public void preDeregister() throws Exception {
        this.myMBeanServer = null;
        this.myObjName = null;
    }

    /** Sin nada que hacer despues. */
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
            throw new IllegalArgumentException("falta el nombre del tipo");
        }
        if (this.myRelType2ObjMap.containsKey(relationTypeName)) {
            throw new InvalidRelationTypeException("ya hay un tipo llamado " + relationTypeName);
        }
        RelationTypeSupport t = new RelationTypeSupport(relationTypeName, roleInfoArray);
        t.setRelationServiceFlag(true);
        this.myRelType2ObjMap.put(relationTypeName, t);
    }

    /** {@inheritDoc} */
    public synchronized void addRelationType(RelationType relationTypeObj)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (relationTypeObj == null) {
            throw new IllegalArgumentException("falta el tipo");
        }
        String nombre = relationTypeObj.getRelationTypeName();
        if (nombre == null) {
            throw new InvalidRelationTypeException("el tipo no tiene nombre");
        }
        if (this.myRelType2ObjMap.containsKey(nombre)) {
            throw new InvalidRelationTypeException("ya hay un tipo llamado " + nombre);
        }
        List<RoleInfo> infos = relationTypeObj.getRoleInfos();
        if (infos == null || infos.isEmpty()) {
            throw new InvalidRelationTypeException("el tipo " + nombre + " no declara roles");
        }
        if (relationTypeObj instanceof RelationTypeSupport) {
            ((RelationTypeSupport) relationTypeObj).setRelationServiceFlag(true);
        }
        this.myRelType2ObjMap.put(nombre, relationTypeObj);
    }

    /** {@inheritDoc} */
    public synchronized List<String> getAllRelationTypeNames() {
        return new ArrayList<String>(this.myRelType2ObjMap.keySet());
    }

    /** {@inheritDoc} */
    public synchronized List<RoleInfo> getRoleInfos(String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException {
        return tipo(relationTypeName).getRoleInfos();
    }

    /** {@inheritDoc} */
    public synchronized RoleInfo getRoleInfo(String relationTypeName, String roleInfoName)
            throws IllegalArgumentException, RelationTypeNotFoundException,
            RoleInfoNotFoundException {
        return tipo(relationTypeName).getRoleInfo(roleInfoName);
    }

    private RelationType tipo(String nombre)
            throws IllegalArgumentException, RelationTypeNotFoundException {
        if (nombre == null) {
            throw new IllegalArgumentException("falta el nombre del tipo");
        }
        RelationType t = this.myRelType2ObjMap.get(nombre);
        if (t == null) {
            throw new RelationTypeNotFoundException("no hay un tipo llamado " + nombre);
        }
        return t;
    }

    /** {@inheritDoc} */
    public synchronized void removeRelationType(String relationTypeName)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationTypeNotFoundException {
        isActive();
        tipo(relationTypeName);
        // Las relaciones de ese tipo se van con el: dejarlas seria dejarlas sin esquema.
        List<String> aBorrar = new ArrayList<String>();
        for (Map.Entry<String, String> e : this.myRelId2RelTypeMap.entrySet()) {
            if (e.getValue().equals(relationTypeName)) {
                aBorrar.add(e.getKey());
            }
        }
        for (int i = 0; i < aBorrar.size(); i++) {
            try {
                removeRelation(aBorrar.get(i));
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
            throw new IllegalArgumentException("falta el identificador");
        }
        if (this.myRelId2ObjMap.containsKey(relationId)) {
            throw new InvalidRelationIdException("ya hay una relacion " + relationId);
        }
        tipo(relationTypeName);
        RelationSupport rel = new RelationSupport(relationId, this.myObjName,
                relationTypeName, roleList);
        rel.setRelationServiceManagementFlag(Boolean.TRUE);
        this.myRelId2ObjMap.put(relationId, rel);
        this.myRelId2RelTypeMap.put(relationId, relationTypeName);
        indexar(relationId, rel.retrieveAllRoles());
        try {
            sendRelationCreationNotification(relationId);
        } catch (RelationNotFoundException e) {
            // Imposible: la acabamos de poner en el mapa dos lineas arriba. Se atrapa porque el
            // metodo la declara para el caso general, no porque pueda pasar aca.
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
            throw new IllegalArgumentException("falta el nombre del MBean");
        }
        throw new InvalidRelationServiceException(
                "esta VM no tiene servidor de MBeans con el que consultar a "
                + relationObjectName.toString());
    }

    /** {@inheritDoc} */
    public synchronized ObjectName isRelationMBean(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        relacion(relationId);
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
            throw new IllegalArgumentException("falta el nombre del MBean");
        }
        return this.myRelMBeanObjName2RelIdMap.get(objectName);
    }

    /** {@inheritDoc} */
    public synchronized Boolean hasRelation(String relationId) throws IllegalArgumentException {
        if (relationId == null) {
            throw new IllegalArgumentException("falta el identificador");
        }
        return Boolean.valueOf(this.myRelId2ObjMap.containsKey(relationId));
    }

    /** {@inheritDoc} */
    public synchronized List<String> getAllRelationIds() {
        return new ArrayList<String>(this.myRelId2ObjMap.keySet());
    }

    private Object relacion(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        if (relationId == null) {
            throw new IllegalArgumentException("falta el identificador");
        }
        Object o = this.myRelId2ObjMap.get(relationId);
        if (o == null) {
            throw new RelationNotFoundException("no hay una relacion " + relationId);
        }
        return o;
    }

    /** {@inheritDoc} */
    public synchronized Integer checkRoleReading(String roleName, String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException {
        if (roleName == null) {
            throw new IllegalArgumentException("falta el nombre del rol");
        }
        RelationType t = tipo(relationTypeName);
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
            throw new IllegalArgumentException("faltan el rol o la bandera");
        }
        RelationType t = tipo(relationTypeName);
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
        // En la escritura inicial el minimo no se exige: una relacion se crea y se llena despues.
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
        relacion(relationId);
        ObjectName mb = isRelationMBean(relationId);
        String tipoNtf = mb == null
                ? RelationNotification.RELATION_BASIC_CREATION
                : RelationNotification.RELATION_MBEAN_CREATION;
        sendNotification(new RelationNotification(tipoNtf, this, nextSeq(),
                System.currentTimeMillis(), "se creo la relacion " + relationId,
                relationId, this.myRelId2RelTypeMap.get(relationId), mb, null));
    }

    /** {@inheritDoc} */
    public synchronized void sendRoleUpdateNotification(String relationId, Role newRole,
            List<ObjectName> oldRoleValue)
            throws IllegalArgumentException, RelationNotFoundException {
        if (newRole == null || oldRoleValue == null) {
            throw new IllegalArgumentException("faltan el rol nuevo o el valor viejo");
        }
        relacion(relationId);
        ObjectName mb = isRelationMBean(relationId);
        String tipoNtf = mb == null
                ? RelationNotification.RELATION_BASIC_UPDATE
                : RelationNotification.RELATION_MBEAN_UPDATE;
        sendNotification(new RelationNotification(tipoNtf, this, nextSeq(),
                System.currentTimeMillis(), "cambio el rol " + newRole.getRoleName(),
                relationId, this.myRelId2RelTypeMap.get(relationId), mb,
                newRole.getRoleName(), newRole.getRoleValue(), oldRoleValue));
    }

    /** {@inheritDoc} */
    public synchronized void sendRelationRemovalNotification(String relationId,
            List<ObjectName> unregMBeanList)
            throws IllegalArgumentException, RelationNotFoundException {
        relacion(relationId);
        ObjectName mb = isRelationMBean(relationId);
        String tipoNtf = mb == null
                ? RelationNotification.RELATION_BASIC_REMOVAL
                : RelationNotification.RELATION_MBEAN_REMOVAL;
        sendNotification(new RelationNotification(tipoNtf, this, nextSeq(),
                System.currentTimeMillis(), "se saco la relacion " + relationId,
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
            throw new IllegalArgumentException("faltan el rol nuevo o el valor viejo");
        }
        relacion(relationId);
        String rol = newRole.getRoleName();
        for (int i = 0; i < oldRoleValue.size(); i++) {
            desindexar(oldRoleValue.get(i), relationId, rol);
        }
        for (ObjectName on : newRole.getRoleValue()) {
            indexarUno(on, relationId, rol);
        }
    }

    private void indexar(String relationId, RoleList roles) {
        for (Role r : roles.asList()) {
            for (ObjectName on : r.getRoleValue()) {
                indexarUno(on, relationId, r.getRoleName());
            }
        }
    }

    private void indexarUno(ObjectName on, String relationId, String rol) {
        Map<String, List<String>> porRel = this.myRefedMBeanObjName2RelIdsMap.get(on);
        if (porRel == null) {
            porRel = new HashMap<String, List<String>>();
            this.myRefedMBeanObjName2RelIdsMap.put(on, porRel);
        }
        List<String> roles = porRel.get(relationId);
        if (roles == null) {
            roles = new ArrayList<String>();
            porRel.put(relationId, roles);
        }
        if (!roles.contains(rol)) {
            roles.add(rol);
        }
    }

    private void desindexar(ObjectName on, String relationId, String rol) {
        Map<String, List<String>> porRel = this.myRefedMBeanObjName2RelIdsMap.get(on);
        if (porRel == null) {
            return;
        }
        List<String> roles = porRel.get(relationId);
        if (roles != null) {
            roles.remove(rol);
            if (roles.isEmpty()) {
                porRel.remove(relationId);
            }
        }
        // Un MBean sin relaciones sale del indice: dejarlo con un mapa vacio lo haria crecer sin
        // limite en un sistema donde las relaciones van y vienen.
        if (porRel.isEmpty()) {
            this.myRefedMBeanObjName2RelIdsMap.remove(on);
        }
    }

    /** {@inheritDoc} */
    public synchronized void removeRelation(String relationId)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException {
        isActive();
        Object rel = relacion(relationId);
        List<ObjectName> aDesregistrar = new ArrayList<ObjectName>();
        ObjectName mb = isRelationMBean(relationId);
        if (mb != null) {
            aDesregistrar.add(mb);
        }
        sendRelationRemovalNotification(relationId, aDesregistrar);
        if (rel instanceof RelationSupport) {
            ((RelationSupport) rel).setRelationServiceManagementFlag(Boolean.FALSE);
        }
        this.myRelId2ObjMap.remove(relationId);
        this.myRelId2RelTypeMap.remove(relationId);
        if (mb != null) {
            this.myRelMBeanObjName2RelIdMap.remove(mb);
        }
        List<ObjectName> vacias = new ArrayList<ObjectName>();
        for (Map.Entry<ObjectName, Map<String, List<String>>> e
                : this.myRefedMBeanObjName2RelIdsMap.entrySet()) {
            e.getValue().remove(relationId);
            if (e.getValue().isEmpty()) {
                vacias.add(e.getKey());
            }
        }
        for (int i = 0; i < vacias.size(); i++) {
            this.myRefedMBeanObjName2RelIdsMap.remove(vacias.get(i));
        }
    }

    /** {@inheritDoc} */
    public synchronized void purgeRelations() throws RelationServiceNotRegisteredException {
        isActive();
    }

    /**
     * Atiende los desregistros del servidor de MBeans.
     *
     * <p>Es lo que mantiene consistentes las relaciones cuando un MBean desaparece sin que quien lo
     * desregistro sepa que estaba en una.
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

    /** Los seis tipos de {@link RelationNotification} que este servicio emite. */
    public MBeanNotificationInfo[] getNotificationInfo() {
        String[] tipos = new String[] {
            RelationNotification.RELATION_BASIC_CREATION,
            RelationNotification.RELATION_MBEAN_CREATION,
            RelationNotification.RELATION_BASIC_UPDATE,
            RelationNotification.RELATION_MBEAN_UPDATE,
            RelationNotification.RELATION_BASIC_REMOVAL,
            RelationNotification.RELATION_MBEAN_REMOVAL,
        };
        return new MBeanNotificationInfo[] {
            new MBeanNotificationInfo(tipos, RelationNotification.class.getName(),
                    "notificaciones del servicio de relaciones"),
        };
    }

    /** {@inheritDoc} */
    public synchronized Map<String, List<String>> findReferencingRelations(ObjectName mbeanName,
            String relationTypeName, String roleName) throws IllegalArgumentException {
        if (mbeanName == null) {
            throw new IllegalArgumentException("falta el nombre del MBean");
        }
        Map<String, List<String>> out = new HashMap<String, List<String>>();
        Map<String, List<String>> porRel = this.myRefedMBeanObjName2RelIdsMap.get(mbeanName);
        if (porRel == null) {
            return out;
        }
        for (Map.Entry<String, List<String>> e : porRel.entrySet()) {
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
        Map<String, List<String>> relaciones =
                findReferencingRelations(mbeanName, relationTypeName, roleName);
        Map<ObjectName, List<String>> out = new HashMap<ObjectName, List<String>>();
        for (String relId : relaciones.keySet()) {
            Map<ObjectName, List<String>> refs;
            try {
                refs = getReferencedMBeans(relId);
            } catch (RelationNotFoundException e) {
                continue;
            }
            for (ObjectName on : refs.keySet()) {
                // El propio MBean no es un asociado de si mismo.
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
        tipo(relationTypeName);
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
        return ((Relation) relacion(relationId)).getRole(roleName);
    }

    /** {@inheritDoc} */
    public synchronized RoleResult getRoles(String relationId, String[] roleNameArray)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException {
        isActive();
        return ((Relation) relacion(relationId)).getRoles(roleNameArray);
    }

    /** {@inheritDoc} */
    public synchronized RoleResult getAllRoles(String relationId)
            throws IllegalArgumentException, RelationNotFoundException,
            RelationServiceNotRegisteredException {
        isActive();
        return ((Relation) relacion(relationId)).getAllRoles();
    }

    /** {@inheritDoc} */
    public synchronized Integer getRoleCardinality(String relationId, String roleName)
            throws IllegalArgumentException, RelationNotFoundException, RoleNotFoundException {
        return ((Relation) relacion(relationId)).getRoleCardinality(roleName);
    }

    /** {@inheritDoc} */
    public synchronized void setRole(String relationId, Role role)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException, RoleNotFoundException, InvalidRoleValueException,
            RelationTypeNotFoundException {
        isActive();
        Relation rel = (Relation) relacion(relationId);
        List<ObjectName> viejo;
        try {
            viejo = rel.getRole(role.getRoleName());
        } catch (RoleNotFoundException e) {
            viejo = new ArrayList<ObjectName>();
        }
        rel.setRole(role);
        updateRoleMap(relationId, role, viejo);
        sendRoleUpdateNotification(relationId, role, viejo);
    }

    /** {@inheritDoc} */
    public synchronized RoleResult setRoles(String relationId, RoleList roleList)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException {
        isActive();
        Relation rel = (Relation) relacion(relationId);
        try {
            return rel.setRoles(roleList);
        } catch (RelationTypeNotFoundException e) {
            return new RoleResult(new RoleList(), new RoleUnresolvedList());
        }
    }

    /** {@inheritDoc} */
    public synchronized Map<ObjectName, List<String>> getReferencedMBeans(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        return ((Relation) relacion(relationId)).getReferencedMBeans();
    }

    /** {@inheritDoc} */
    public synchronized String getRelationTypeName(String relationId)
            throws IllegalArgumentException, RelationNotFoundException {
        relacion(relationId);
        return this.myRelId2RelTypeMap.get(relationId);
    }
}
