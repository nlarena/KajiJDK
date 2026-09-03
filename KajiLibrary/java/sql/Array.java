package java.sql;

/**
 * KajiLibrary's java.sql.Array -- una columna que contiene un arreglo SQL.
 *
 * <p>Ofrece el contenido de **dos** formas, y no es redundancia: como arreglo Java, cuando entra en
 * memoria y se quiere manipular; o como {@link ResultSet} de dos columnas --indice y valor-- cuando
 * es grande y conviene recorrerlo de a poco. Es la misma tension que resuelve {@link Blob}, con las
 * dos salidas puestas en la misma interfaz.
 *
 * <p>Los indices se cuentan **desde uno**.
 */
public interface Array {

    /** El nombre SQL del tipo de los elementos. */
    String getBaseTypeName() throws SQLException;

    /** El codigo SQL del tipo de los elementos. */
    int getBaseType() throws SQLException;

    /** Todo el contenido, como arreglo Java. */
    Object getArray() throws SQLException;

    /** Igual, traduciendo los tipos SQL con ese mapa. */
    Object getArray(java.util.Map<String, Class<?>> map) throws SQLException;

    /** `count` elementos desde `index`. */
    Object getArray(long index, int count) throws SQLException;

    /** Igual, con mapa de tipos. */
    Object getArray(long index, int count, java.util.Map<String, Class<?>> map)
            throws SQLException;

    /** Todo el contenido, como filas de (indice, valor). */
    ResultSet getResultSet() throws SQLException;

    ResultSet getResultSet(java.util.Map<String, Class<?>> map) throws SQLException;

    ResultSet getResultSet(long index, int count) throws SQLException;

    ResultSet getResultSet(long index, int count, java.util.Map<String, Class<?>> map)
            throws SQLException;

    /** Suelta los recursos del puntero. */
    void free() throws SQLException;
}
