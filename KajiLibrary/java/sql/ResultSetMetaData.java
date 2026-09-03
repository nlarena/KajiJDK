package java.sql;

/**
 * KajiLibrary's java.sql.ResultSetMetaData -- que columnas trajo una consulta.
 *
 * <p>Existe porque una consulta se puede escribir sin saber su forma: un `select *`, o una que llega
 * como texto. Sin esto no habria manera de recorrer un resultado generico -- ni de saber cuantas
 * columnas hay ni como leer cada una.
 *
 * <p>Las columnas se numeran **desde uno**, no desde cero, en toda la API JDBC. Es la convencion de
 * SQL y no un descuido.
 */
public interface ResultSetMetaData extends Wrapper {

    /** La columna no admite nulos. */
    int columnNoNulls = 0;

    /** La columna admite nulos. */
    int columnNullable = 1;

    /** No se sabe si los admite. */
    int columnNullableUnknown = 2;

    /** Cuantas columnas hay. */
    int getColumnCount() throws SQLException;

    /** Si la columna se numera sola. */
    boolean isAutoIncrement(int column) throws SQLException;

    /** Si distingue mayusculas de minusculas. */
    boolean isCaseSensitive(int column) throws SQLException;

    /** Si se puede usar en un `where`. */
    boolean isSearchable(int column) throws SQLException;

    /** Si es un valor monetario. */
    boolean isCurrency(int column) throws SQLException;

    /** Si admite nulos: uno de los tres `columnNullable*`. */
    int isNullable(int column) throws SQLException;

    /** Si es un numero con signo. */
    boolean isSigned(int column) throws SQLException;

    /** El ancho normal en caracteres, para mostrarla. */
    int getColumnDisplaySize(int column) throws SQLException;

    /** El titulo sugerido -- el `as` de la consulta, si lo hubo. */
    String getColumnLabel(int column) throws SQLException;

    /** El nombre real de la columna. */
    String getColumnName(int column) throws SQLException;

    String getSchemaName(int column) throws SQLException;

    /** Los digitos totales de un numero, o los caracteres de un texto. */
    int getPrecision(int column) throws SQLException;

    /** Los digitos a la derecha del punto. */
    int getScale(int column) throws SQLException;

    String getTableName(int column) throws SQLException;

    String getCatalogName(int column) throws SQLException;

    /** El tipo SQL, como codigo. */
    int getColumnType(int column) throws SQLException;

    /** El tipo SQL, como lo llama el proveedor. */
    String getColumnTypeName(int column) throws SQLException;

    boolean isReadOnly(int column) throws SQLException;

    /** Si **podria** escribirse. */
    boolean isWritable(int column) throws SQLException;

    /** Si escribirla **va** a funcionar; mas fuerte que {@link #isWritable}. */
    boolean isDefinitelyWritable(int column) throws SQLException;

    /** La clase Java que devuelve `ResultSet.getObject` para esta columna. */
    String getColumnClassName(int column) throws SQLException;
}
