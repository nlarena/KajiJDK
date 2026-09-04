package javax.management.relation;

import java.util.List;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

/**
 * La interfaz de gestion del {@link RelationService}: todo lo que se puede hacer con el desde una
 * consola.
 *
 * <h2>Las tres familias de operaciones</h2>
 *
 * <ul>
 * <li><strong>tipos</strong> — declarar el esquema: {@code createRelationType},
 *     {@code addRelationType}, {@code removeRelationType}. Es lo primero, porque una relacion se
 *     valida contra su tipo;</li>
 * <li><strong>relaciones</strong> — crearlas, borrarlas, leer y escribir sus roles;</li>
 * <li><strong>consultas</strong> — {@code findReferencingRelations},
 *     {@code findAssociatedMBeans}, {@code findRelationsOfType}. Son lo que hace util al servicio:
 *     recorrer los vinculos <em>al reves</em>, que es exactamente lo que no se puede hacer cuando
 *     cada MBean guarda las referencias por su cuenta.</li>
 * </ul>
 *
 * <h2>Los dos {@code create} y {@code add}</h2>
 *
 * <p>{@code createRelation} y {@code createRelationType} arman el objeto adentro del servicio;
 * {@code addRelation} y {@code addRelationType} toman uno ya construido. La diferencia importa
 * porque solo la segunda forma permite una relacion que sea un MBean registrado.
 *
 * <h2>La bandera de purga, que es la decision de diseno mas visible</h2>
 *
 * <p>Cuando un MBean referenciado se desregistra, sus relaciones quedan inconsistentes. Con la purga
 * automatica el servicio las limpia enseguida; sin ella hay que llamar a {@link #purgeRelations} a
 * mano. La opcion existe porque limpiar es caro y porque hay sistemas donde un MBean se desregistra
 * y se vuelve a registrar como parte de su operacion normal.
 */
public interface RelationServiceMBean {

    /**
     * @throws RelationServiceNotRegisteredException si el servicio no esta registrado en ningun
     *     servidor de MBeans — sin servidor no puede verificar nada
     */
    void isActive() throws RelationServiceNotRegisteredException;

    /** Si se limpian solas las relaciones que quedan inconsistentes. */
    boolean getPurgeFlag();

    /** Cambia esa politica; ver la nota de la interfaz. */
    void setPurgeFlag(boolean purgeFlag);

    /**
     * Declara un tipo con esos roles.
     *
     * @throws InvalidRelationTypeException si ya hay uno con ese nombre, o si los roles son
     *     inconsistentes
     */
    void createRelationType(String relationTypeName, RoleInfo[] roleInfoArray)
            throws IllegalArgumentException, InvalidRelationTypeException;

    /** Agrega un tipo ya construido. */
    void addRelationType(RelationType relationTypeObj)
            throws IllegalArgumentException, InvalidRelationTypeException;

    /** Los nombres de los tipos declarados. */
    List<String> getAllRelationTypeNames();

    /**
     * Los roles que declara ese tipo.
     *
     * @throws RelationTypeNotFoundException si no existe
     */
    List<RoleInfo> getRoleInfos(String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException;

    /**
     * La descripcion de un rol de ese tipo.
     *
     * @throws RoleInfoNotFoundException si el tipo no lo declara
     */
    RoleInfo getRoleInfo(String relationTypeName, String roleInfoName)
            throws IllegalArgumentException, RelationTypeNotFoundException,
            RoleInfoNotFoundException;

    /**
     * Saca el tipo, y con el <strong>todas las relaciones de ese tipo</strong>.
     *
     * <p>No es una limpieza cortes: dejarlas seria dejar relaciones sin esquema contra el cual
     * validarse.
     */
    void removeRelationType(String relationTypeName)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationTypeNotFoundException;

    /**
     * Crea una relacion administrada internamente.
     *
     * @throws InvalidRelationIdException si ya hay una con ese identificador
     * @throws InvalidRoleValueException si algun rol no cumple lo que su descripcion exige
     */
    void createRelation(String relationId, String relationTypeName, RoleList roleList)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RoleNotFoundException, InvalidRelationIdException, RelationTypeNotFoundException,
            InvalidRoleValueException;

    /**
     * Toma una relacion que ya es un MBean registrado.
     *
     * @throws NoSuchMethodException si el MBean no implementa {@link Relation}
     * @throws InstanceNotFoundException si no esta registrado
     */
    void addRelation(ObjectName relationObjectName)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            NoSuchMethodException, InvalidRelationIdException, InstanceNotFoundException,
            InvalidRelationServiceException, RelationTypeNotFoundException,
            RoleNotFoundException, InvalidRoleValueException;

    /** El nombre del MBean de esa relacion, o {@code null} si es interna. */
    ObjectName isRelationMBean(String relationId)
            throws IllegalArgumentException, RelationNotFoundException;

    /** El identificador de la relacion que es ese MBean, o {@code null}. */
    String isRelation(ObjectName objectName) throws IllegalArgumentException;

    /** Si hay una relacion con ese identificador. */
    Boolean hasRelation(String relationId) throws IllegalArgumentException;

    /** Los identificadores de todas las relaciones. */
    List<String> getAllRelationIds();

    /**
     * Si ese rol se puede leer.
     *
     * @return {@code null} si se puede, o un codigo de {@link RoleStatus} si no
     */
    Integer checkRoleReading(String roleName, String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException;

    /**
     * Si ese rol se puede escribir con ese valor.
     *
     * @param initFlag si la escritura es la inicial, donde la cardinalidad minima todavia no se
     *     exige — una relacion se crea vacia y se llena despues
     * @return {@code null} si se puede, o un codigo de {@link RoleStatus}
     */
    Integer checkRoleWriting(Role role, String relationTypeName, Boolean initFlag)
            throws IllegalArgumentException, RelationTypeNotFoundException;

    /** Emite la notificacion de creacion. */
    void sendRelationCreationNotification(String relationId)
            throws IllegalArgumentException, RelationNotFoundException;

    /** Emite la notificacion de actualizacion de un rol. */
    void sendRoleUpdateNotification(String relationId, Role newRole,
            List<ObjectName> oldRoleValue)
            throws IllegalArgumentException, RelationNotFoundException;

    /** Emite la notificacion de borrado. */
    void sendRelationRemovalNotification(String relationId, List<ObjectName> unregMBeanList)
            throws IllegalArgumentException, RelationNotFoundException;

    /**
     * Actualiza el indice inverso de MBean a relaciones.
     *
     * <p>Lo llama la relacion despues de cambiar un rol. Es lo que mantiene util a
     * {@link #findReferencingRelations}: sin esto, la consulta al reves tendria que recorrer todas
     * las relaciones.
     */
    void updateRoleMap(String relationId, Role newRole, List<ObjectName> oldRoleValue)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            RelationNotFoundException;

    /** Saca la relacion. */
    void removeRelation(String relationId)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException;

    /** Limpia las relaciones que quedaron inconsistentes; ver la bandera de purga. */
    void purgeRelations()
            throws RelationServiceNotRegisteredException;

    /**
     * Que relaciones referencian a ese MBean, y en que roles.
     *
     * @param relationTypeName filtra por tipo, o {@code null} para todos
     * @param roleName filtra por rol, o {@code null} para todos
     */
    Map<String, List<String>> findReferencingRelations(ObjectName mbeanName,
            String relationTypeName, String roleName) throws IllegalArgumentException;

    /**
     * Que MBeans estan asociados a ese, y por que relaciones.
     *
     * <p>Es la consulta que justifica todo el servicio: "que depende de esto" no se puede contestar
     * cuando cada MBean guarda sus propias referencias.
     */
    Map<ObjectName, List<String>> findAssociatedMBeans(ObjectName mbeanName,
            String relationTypeName, String roleName) throws IllegalArgumentException;

    /** Los identificadores de las relaciones de ese tipo. */
    List<String> findRelationsOfType(String relationTypeName)
            throws IllegalArgumentException, RelationTypeNotFoundException;

    /** El valor de un rol de esa relacion. */
    List<ObjectName> getRole(String relationId, String roleName)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException, RoleNotFoundException;

    /** Varios roles a la vez. */
    RoleResult getRoles(String relationId, String[] roleNameArray)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException;

    /** Todos los roles legibles. */
    RoleResult getAllRoles(String relationId)
            throws IllegalArgumentException, RelationNotFoundException,
            RelationServiceNotRegisteredException;

    /** Cuantos MBeans tiene ese rol. */
    Integer getRoleCardinality(String relationId, String roleName)
            throws IllegalArgumentException, RelationNotFoundException, RoleNotFoundException;

    /** Cambia un rol. */
    void setRole(String relationId, Role role)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException, RoleNotFoundException, InvalidRoleValueException,
            RelationTypeNotFoundException;

    /** Cambia varios. */
    RoleResult setRoles(String relationId, RoleList roleList)
            throws RelationServiceNotRegisteredException, IllegalArgumentException,
            RelationNotFoundException;

    /** Los MBeans que referencia esa relacion, y en que roles. */
    Map<ObjectName, List<String>> getReferencedMBeans(String relationId)
            throws IllegalArgumentException, RelationNotFoundException;

    /** El tipo de esa relacion. */
    String getRelationTypeName(String relationId)
            throws IllegalArgumentException, RelationNotFoundException;
}
