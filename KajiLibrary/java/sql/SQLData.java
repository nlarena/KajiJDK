package java.sql;

/**
 * KajiLibrary's java.sql.SQLData -- una clase Java que sabe leerse y escribirse como tipo SQL propio.
 *
 * <p>Es la alternativa a recibir un {@link Struct} con un `Object[]` y desarmarlo a mano: la clase se
 * registra en el mapa de tipos de la conexion y el driver la construye sola.
 *
 * <p>El orden manda: {@link #readSQL} tiene que leer los atributos en el **mismo orden** en que
 * {@link #writeSQL} los escribe, que es el de la declaracion del tipo SQL. No hay nombres, y por eso
 * un cambio en el tipo de la base rompe esto en silencio.
 */
public interface SQLData {

    /** El nombre del tipo SQL que esta clase representa. */
    String getSQLTypeName() throws SQLException;

    /** Se llena leyendo los atributos de `stream`, en orden. */
    void readSQL(SQLInput stream, String typeName) throws SQLException;

    /** Se escribe en `stream`, en el mismo orden. */
    void writeSQL(SQLOutput stream) throws SQLException;
}
