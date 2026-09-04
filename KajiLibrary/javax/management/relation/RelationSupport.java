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
 * La implementacion de {@link Relation} que trae el JDK: los roles se guardan en un mapa.
 *
 * <h2>Las dos formas de vivir</h2>
 *
 * <p>Una relacion puede ser <strong>interna</strong> —el servicio la guarda y nadie mas la ve— o un
 * <strong>MBean registrado</strong>, visible desde una consola. Esta clase sirve para las dos, y de
 * ahi que implemente {@link MBeanRegistration}: cuando se la registra, el servidor le avisa.
 *
 * <p>{@link #isInRelationService} dice en cual esta. Importa porque casi ninguna operacion sirve
 * antes de que el servicio la tome: sin el no hay a quien preguntarle por el tipo de la relacion ni
 * con que verificar que los MBeans referenciados existan.
 *
 * <h2>Por que los metodos delegan en el servicio</h2>
 *
 * <p>Se ve en los {@code ...Int} de mas abajo, que reciben el {@link RelationService} como
 * argumento. La razon es que esta clase <strong>no puede validar sola</strong>: para saber si un rol
 * es escribible hay que mirar el {@link RoleInfo} del tipo, y el tipo lo tiene el servicio.
 *
 * <p>La consecuencia es la que sorprende: una relacion recien construida y todavia no agregada al
 * servicio rechaza casi todo con {@link RelationServiceNotRegisteredException}.
 */
public class RelationSupport implements RelationSupportMBean, MBeanRegistration {

    private final String myRelId;
    private final ObjectName myRelServiceName;
    private final String myRelTypeName;
    private final Map<String, Role> myRoleName2ValueMap = new TreeMap<String, Role>();

    private MBeanServer myRelServiceMBeanServer;
    private boolean myInRelServFlg = false;

    /**
     * Una relacion interna.
     *
     * @throws IllegalArgumentException si falta algo
     * @throws InvalidRoleValueException si dos roles se llaman igual
     */
    public RelationSupport(String relationId, ObjectName relationServiceName,
            String relationTypeName, RoleList list)
            throws InvalidRoleValueException, IllegalArgumentException {
        revisar(relationId, relationServiceName, relationTypeName);
        this.myRelId = relationId;
        this.myRelServiceName = relationServiceName;
        this.myRelTypeName = relationTypeName;
        cargar(list);
    }

    /**
     * Una relacion que va a ser un MBean, con el servidor donde vive el servicio.
     *
     * @throws IllegalArgumentException si falta algo
     * @throws InvalidRoleValueException si dos roles se llaman igual
     */
    public RelationSupport(String relationId, ObjectName relationServiceName,
            MBeanServer relationServiceMBeanServer, String relationTypeName, RoleList list)
            throws InvalidRoleValueException, IllegalArgumentException {
        this(relationId, relationServiceName, relationTypeName, list);
        if (relationServiceMBeanServer == null) {
            throw new IllegalArgumentException("falta el servidor de MBeans");
        }
        this.myRelServiceMBeanServer = relationServiceMBeanServer;
    }

    private static void revisar(String id, ObjectName svc, String tipo) {
        if (id == null || svc == null || tipo == null) {
            throw new IllegalArgumentException(
                    "hacen falta el identificador, el servicio y el tipo");
        }
    }

    private void cargar(RoleList list) throws InvalidRoleValueException {
        if (list == null) {
            return;
        }
        for (Role r : list.asList()) {
            if (this.myRoleName2ValueMap.containsKey(r.getRoleName())) {
                throw new InvalidRoleValueException(
                        "hay dos roles llamados " + r.getRoleName());
            }
            this.myRoleName2ValueMap.put(r.getRoleName(), (Role) r.clone());
        }
    }

    private void exigirServicio() throws RelationServiceNotRegisteredException {
        if (!this.myInRelServFlg) {
            throw new RelationServiceNotRegisteredException(
                    "la relacion " + this.myRelId + " todavia no esta en el servicio");
        }
    }

    /** {@inheritDoc} */
    public List<ObjectName> getRole(String roleName)
            throws IllegalArgumentException, RoleNotFoundException,
            RelationServiceNotRegisteredException {
        if (roleName == null) {
            throw new IllegalArgumentException("falta el nombre del rol");
        }
        exigirServicio();
        Role r = this.myRoleName2ValueMap.get(roleName);
        if (r == null) {
            throw new RoleNotFoundException("no hay un rol llamado " + roleName);
        }
        return new ArrayList<ObjectName>(r.getRoleValue());
    }

    /** {@inheritDoc} */
    public RoleResult getRoles(String[] roleNameArray)
            throws IllegalArgumentException, RelationServiceNotRegisteredException {
        if (roleNameArray == null) {
            throw new IllegalArgumentException("falta el arreglo de nombres");
        }
        exigirServicio();
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
        exigirServicio();
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
            throw new IllegalArgumentException("falta el nombre del rol");
        }
        Role r = this.myRoleName2ValueMap.get(roleName);
        if (r == null) {
            throw new RoleNotFoundException("no hay un rol llamado " + roleName);
        }
        return Integer.valueOf(r.getRoleValue().size());
    }

    /** {@inheritDoc} */
    public void setRole(Role role)
            throws IllegalArgumentException, RoleNotFoundException,
            RelationTypeNotFoundException, InvalidRoleValueException,
            RelationServiceNotRegisteredException, RelationNotFoundException {
        if (role == null) {
            throw new IllegalArgumentException("falta el rol");
        }
        exigirServicio();
        if (!this.myRoleName2ValueMap.containsKey(role.getRoleName())) {
            throw new RoleNotFoundException("no hay un rol llamado " + role.getRoleName());
        }
        this.myRoleName2ValueMap.put(role.getRoleName(), (Role) role.clone());
    }

    /** {@inheritDoc} */
    public RoleResult setRoles(RoleList roleList)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            RelationTypeNotFoundException, RelationNotFoundException {
        if (roleList == null) {
            throw new IllegalArgumentException("falta la lista de roles");
        }
        exigirServicio();
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
            throw new IllegalArgumentException("faltan el MBean o el rol");
        }
        exigirServicio();
        Role r = this.myRoleName2ValueMap.get(roleName);
        if (r == null) {
            throw new RoleNotFoundException("no hay un rol llamado " + roleName);
        }
        List<ObjectName> quedan = new ArrayList<ObjectName>(r.getRoleValue());
        quedan.remove(objectName);
        // Puede quedar por debajo del minimo del RoleInfo, y eso es correcto: la relacion pasa a
        // estar en falta, que es justamente lo que el servicio detecta al purgar.
        this.myRoleName2ValueMap.put(roleName, new Role(roleName, quedan));
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

    /** El servidor donde queda registrada; lo llama el servidor de MBeans. */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.myRelServiceMBeanServer = server;
        return name;
    }

    /** Sin nada que hacer despues de registrarse. */
    public void postRegister(Boolean registrationDone) {
    }

    /**
     * Antes de desregistrarse.
     *
     * <p>No saca la relacion del servicio: el servicio se entera por su propio filtro de
     * notificaciones, que es lo que lo mantiene consistente aunque a un MBean lo desregistre alguien
     * que no sabe nada de relaciones.
     */
    public void preDeregister() throws Exception {
    }

    /** Sin nada que hacer despues. */
    public void postDeregister() {
    }

    /** {@inheritDoc} */
    public Boolean isInRelationService() {
        return Boolean.valueOf(this.myInRelServFlg);
    }

    /** {@inheritDoc} */
    public void setRelationServiceManagementFlag(Boolean flag) throws IllegalArgumentException {
        if (flag == null) {
            throw new IllegalArgumentException("la bandera no puede ser null");
        }
        this.myInRelServFlg = flag.booleanValue();
    }
}
