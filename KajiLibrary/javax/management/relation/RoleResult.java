package javax.management.relation;

import java.io.Serializable;

/**
 * El resultado de una operacion sobre varios roles: los que salieron bien y los que no.
 *
 * <h2>Por que dos listas y no una excepcion</h2>
 *
 * <p>Porque leer o escribir cinco roles puede dar tres exitos y dos problemas, y las dos mitades son
 * utiles. Una excepcion obligaria a tirar los tres que anduvieron; devolver solo los exitos
 * escondería los fallos.
 *
 * <p>Es la razon de que exista {@link RoleUnresolved}: sin un tipo que represente "este fallo, y por
 * esto", la mitad mala no se podria devolver.
 */
public class RoleResult implements Serializable {

    private static final long serialVersionUID = -6304063118040985512L;

    private RoleList roleList;
    private RoleUnresolvedList unresolvedRoleList;

    /** Las dos mitades; cualquiera puede ser {@code null} o vacia. */
    public RoleResult(RoleList list, RoleUnresolvedList unresolvedList) {
        setRoles(list);
        setRolesUnresolved(unresolvedList);
    }

    /** Los que se resolvieron. */
    public RoleList getRoles() {
        return this.roleList;
    }

    /** Los que no, con su motivo. */
    public RoleUnresolvedList getRolesUnresolved() {
        return this.unresolvedRoleList;
    }

    /** Fija los resueltos; {@code null} deja la lista vacia y no nula. */
    public void setRoles(RoleList list) {
        this.roleList = list == null ? new RoleList() : new RoleList(list.asList());
    }

    /** Fija los no resueltos. */
    public void setRolesUnresolved(RoleUnresolvedList unresolvedList) {
        this.unresolvedRoleList = unresolvedList == null
                ? new RoleUnresolvedList()
                : new RoleUnresolvedList(unresolvedList.asList());
    }
}
