package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetInternal -- la cara **de adentro** de un {@link RowSet}.
 *
 * <p>Existe para que el lector y el escritor puedan ver cosas que la aplicacion no deberia tocar: los
 * parametros con los que se ejecuto y --la interesante-- los valores **originales** de las filas.
 *
 * <p>Los originales son lo que hace posible sincronizar un conjunto desconectado: al escribir, el
 * escritor compara lo que habia cuando se leyo contra lo que hay ahora en la base, y si difiere es
 * que otro escribio en el medio. Sin ese original no habria como detectar el conflicto -- solo se
 * podria pisar.
 */
public interface RowSetInternal {

    /** Los parametros con los que se ejecuto la consulta. */
    Object[] getParams() throws java.sql.SQLException;

    /** La conexion, si el conjunto esta conectado. */
    java.sql.Connection getConnection() throws java.sql.SQLException;

    /** Todas las filas **como estaban** al leerse. */
    java.sql.ResultSet getOriginal() throws java.sql.SQLException;

    /** Solo la fila actual, como estaba. */
    java.sql.ResultSet getOriginalRow() throws java.sql.SQLException;

    /** Le dice al conjunto que columnas tiene. */
    void setMetaData(RowSetMetaData md) throws java.sql.SQLException;
}
