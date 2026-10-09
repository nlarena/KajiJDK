package java.sql;

/**
 * KajiLibrary's java.sql.Struct -- a value of an SQL structured type.
 *
 * <p>The attributes come as an `Object[]` and not with names: the order is that of the type's
 * declaration, and whoever reads has to know it. It is raw, and it is what there is.
 */
public interface Struct {

    /** The SQL name of the type. */
    String getSQLTypeName() throws SQLException;

    /** The attributes, in the order in which the type declares them. */
    Object[] getAttributes() throws SQLException;

    /** The same, translating the SQL types with that map. */
    Object[] getAttributes(java.util.Map<String, Class<?>> map) throws SQLException;
}
