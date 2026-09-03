package java.sql;

/**
 * KajiLibrary's java.sql.Ref -- una referencia a un valor estructurado de la base.
 *
 * <p>Es un puntero persistente: no la copia del objeto sino su direccion, que se puede guardar en
 * otra columna y seguir despues. Es la parte "objeto-relacional" de SQL, poco usada.
 */
public interface Ref {

    /** El nombre del tipo SQL al que apunta. */
    String getBaseTypeName() throws SQLException;

    /** El valor apuntado. */
    Object getObject() throws SQLException;

    /** El valor apuntado, traduciendo los tipos SQL con ese mapa. */
    Object getObject(java.util.Map<String, Class<?>> map) throws SQLException;

    /** Cambia el valor apuntado. */
    void setObject(Object value) throws SQLException;
}
