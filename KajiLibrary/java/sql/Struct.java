package java.sql;

/**
 * KajiLibrary's java.sql.Struct -- un valor de un tipo estructurado de SQL.
 *
 * <p>Los atributos vienen como `Object[]` y no con nombres: el orden es el de la declaracion del
 * tipo, y quien lee tiene que saberlo. Es crudo, y es lo que hay.
 */
public interface Struct {

    /** El nombre SQL del tipo. */
    String getSQLTypeName() throws SQLException;

    /** Los atributos, en el orden en que el tipo los declara. */
    Object[] getAttributes() throws SQLException;

    /** Igual, traduciendo los tipos SQL con ese mapa. */
    Object[] getAttributes(java.util.Map<String, Class<?>> map) throws SQLException;
}
