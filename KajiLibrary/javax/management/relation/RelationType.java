package javax.management.relation;

import java.io.Serializable;
import java.util.List;

/**
 * A relation's schema: which roles it has and what it is called.
 *
 * <p>It is to a relation what a class is to an object. It is declared once in the relation service,
 * and then as many relations of that type as needed are created -- all validated against the same
 * schema.
 *
 * <p>See {@link RelationTypeSupport} for the implementation the JDK ships; implementing this
 * interface directly is only needed for a type that is computed instead of declared.
 */
public interface RelationType extends Serializable {

    /** The type's name, unique within the service. */
    String getRelationTypeName();

    /** The roles it declares. */
    List<RoleInfo> getRoleInfos();

    /**
     * That role's description.
     *
     * @throws RoleInfoNotFoundException if the type declares none with that name
     */
    RoleInfo getRoleInfo(String roleInfoName)
            throws IllegalArgumentException, RoleInfoNotFoundException;
}
