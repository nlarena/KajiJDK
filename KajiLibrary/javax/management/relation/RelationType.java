package javax.management.relation;

import java.io.Serializable;
import java.util.List;

/**
 * El esquema de una relacion: que roles tiene y como se llama.
 *
 * <p>Es a una relacion lo que una clase a un objeto. Se declara una vez en el servicio de
 * relaciones, y despues se crean cuantas relaciones de ese tipo hagan falta — todas validadas contra
 * el mismo esquema.
 *
 * <p>Ver {@link RelationTypeSupport} para la implementacion que trae el JDK; implementar esta
 * interfaz directamente solo hace falta para un tipo que se calcule en vez de declararse.
 */
public interface RelationType extends Serializable {

    /** El nombre del tipo, unico dentro del servicio. */
    String getRelationTypeName();

    /** Los roles que declara. */
    List<RoleInfo> getRoleInfos();

    /**
     * La descripcion de ese rol.
     *
     * @throws RoleInfoNotFoundException si el tipo no declara ninguno con ese nombre
     */
    RoleInfo getRoleInfo(String roleInfoName)
            throws IllegalArgumentException, RoleInfoNotFoundException;
}
