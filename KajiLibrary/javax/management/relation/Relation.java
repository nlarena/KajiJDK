package javax.management.relation;

import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

/**
 * Una relacion concreta: que MBeans ocupan cada rol.
 *
 * <h2>Que resuelve el servicio de relaciones</h2>
 *
 * <p>JMX modela objetos administrados sueltos. Cuando entre ellos hay vinculos —este servidor aloja
 * estas aplicaciones, este disco pertenece a esta maquina— cada MBean podria guardar el
 * {@link ObjectName} del otro en un atributo, y ahi empiezan los problemas: nadie mantiene la
 * consistencia cuando uno se desregistra, no hay forma de recorrer el vinculo al reves, y la
 * cardinalidad no esta escrita en ningun lado.
 *
 * <p>Esto lo saca de los MBeans y lo pone en un servicio que si puede garantizarlo.
 *
 * <h2>Por que casi todo devuelve {@link RoleResult} en vez de tirar</h2>
 *
 * <p>Porque una operacion sobre varios roles puede fallar en algunos: ver {@link RoleUnresolved}. Los
 * metodos que trabajan sobre <strong>uno</strong> si tiran, porque ahi no hay mitad buena.
 *
 * <h2>Quien la implementa</h2>
 *
 * <p>{@link RelationSupport} para el caso normal. Implementarla directamente sirve para una relacion
 * cuyos roles se <em>calculen</em> en vez de guardarse — todas las maquinas de un rack, por ejemplo,
 * derivadas de otra cosa.
 */
public interface Relation {

    /**
     * Los MBeans que ocupan ese rol.
     *
     * @throws RoleNotFoundException si no existe o no se puede leer
     */
    List<ObjectName> getRole(String roleName)
            throws IllegalArgumentException, RoleNotFoundException,
            RelationServiceNotRegisteredException;

    /** Varios roles a la vez; los que fallen vienen como {@link RoleUnresolved}. */
    RoleResult getRoles(String[] roleNameArray)
            throws IllegalArgumentException, RelationServiceNotRegisteredException;

    /**
     * Cuantos MBeans tiene ese rol.
     *
     * @throws RoleNotFoundException si el rol no existe
     */
    Integer getRoleCardinality(String roleName)
            throws IllegalArgumentException, RoleNotFoundException;

    /** Todos los roles legibles, con los que no lo son aparte. */
    RoleResult getAllRoles() throws RelationServiceNotRegisteredException;

    /**
     * Todos los roles, <strong>sin</strong> comprobar si son legibles.
     *
     * <p>Es el acceso interno: lo usa el servicio de relaciones, que ya decidio que puede mirar. Por
     * eso devuelve una {@link RoleList} pelada y no un {@link RoleResult} — aca no hay nada que
     * pueda quedar sin resolver.
     */
    RoleList retrieveAllRoles();

    /**
     * Cambia el valor de un rol.
     *
     * @throws InvalidRoleValueException si el valor no cumple lo que el {@link RoleInfo} exige
     * @throws RoleNotFoundException si el rol no existe o no se puede escribir
     */
    void setRole(Role role)
            throws IllegalArgumentException, RoleNotFoundException,
            RelationTypeNotFoundException, InvalidRoleValueException,
            RelationServiceNotRegisteredException, RelationNotFoundException;

    /** Cambia varios; los que fallen vienen como {@link RoleUnresolved}. */
    RoleResult setRoles(RoleList roleList)
            throws IllegalArgumentException, RelationServiceNotRegisteredException,
            RelationTypeNotFoundException, RelationNotFoundException;

    /**
     * Le avisa a la relacion que un MBean referenciado se desregistro.
     *
     * <p>Lo llama el servicio, que es quien escucha al servidor de MBeans. La relacion saca esa
     * referencia de sus roles — y ahi puede quedar por debajo del minimo, que es como una relacion
     * pasa a estar en falta sin que nadie la haya tocado.
     */
    void handleMBeanUnregistration(ObjectName objectName, String roleName)
            throws IllegalArgumentException, RoleNotFoundException, InvalidRoleValueException,
            RelationServiceNotRegisteredException, RelationTypeNotFoundException,
            RelationNotFoundException;

    /**
     * Todos los MBeans referenciados, y en que roles aparece cada uno.
     *
     * <p>Es el indice al reves: dado un MBean, en que roles esta. El servicio lo usa para saber a
     * quien avisarle cuando uno se desregistra.
     */
    Map<ObjectName, List<String>> getReferencedMBeans();

    /** El nombre del tipo de esta relacion. */
    String getRelationTypeName();

    /** El nombre del servicio de relaciones que la administra. */
    ObjectName getRelationServiceName();

    /** El identificador de esta relacion, unico dentro del servicio. */
    String getRelationId();
}
