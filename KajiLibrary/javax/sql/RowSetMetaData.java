package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetMetaData -- los metadatos de un {@link RowSet}, **escribibles**.
 *
 * <p>Agrega los `set` a los `get` que hereda, y por una razon concreta: un `RowSet` desconectado
 * puede llenarse de datos que no vinieron de una consulta --de un archivo, de otra fuente-- y
 * entonces alguien tiene que **decirle** que columnas tiene. Con un `ResultSetMetaData` de solo
 * lectura eso seria imposible.
 *
 * <p>{@link #setColumnCount} va primero: los demas reciben un indice de columna, y sin saber cuantas
 * hay no hay indice valido.
 */
public interface RowSetMetaData extends java.sql.ResultSetMetaData {

    /** Cuantas columnas hay. Se llama antes que cualquier otro. */
    void setColumnCount(int columnCount) throws java.sql.SQLException;

    void setAutoIncrement(int columnIndex, boolean property) throws java.sql.SQLException;

    void setCaseSensitive(int columnIndex, boolean property) throws java.sql.SQLException;

    void setSearchable(int columnIndex, boolean property) throws java.sql.SQLException;

    void setCurrency(int columnIndex, boolean property) throws java.sql.SQLException;

    /** Uno de los `columnNullable*` de {@link java.sql.ResultSetMetaData}. */
    void setNullable(int columnIndex, int property) throws java.sql.SQLException;

    void setSigned(int columnIndex, boolean property) throws java.sql.SQLException;

    void setColumnDisplaySize(int columnIndex, int size) throws java.sql.SQLException;

    void setColumnLabel(int columnIndex, String label) throws java.sql.SQLException;

    void setColumnName(int columnIndex, String columnName) throws java.sql.SQLException;

    void setSchemaName(int columnIndex, String schemaName) throws java.sql.SQLException;

    void setPrecision(int columnIndex, int precision) throws java.sql.SQLException;

    void setScale(int columnIndex, int scale) throws java.sql.SQLException;

    void setTableName(int columnIndex, String tableName) throws java.sql.SQLException;

    void setCatalogName(int columnIndex, String catalogName) throws java.sql.SQLException;

    void setColumnType(int columnIndex, int SQLType) throws java.sql.SQLException;

    void setColumnTypeName(int columnIndex, String typeName) throws java.sql.SQLException;
}
