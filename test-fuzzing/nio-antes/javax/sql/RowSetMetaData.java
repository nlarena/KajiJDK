package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetMetaData -- the metadata of a {@link RowSet}, **writable**.
 *
 * <p>It adds the `set`s to the `get`s it inherits, and for a concrete reason: a disconnected
 * `RowSet` can be filled with data that did not come from a query --from a file, from another
 * source-- and then somebody has to **tell** it which columns it has. With a read-only
 * `ResultSetMetaData` that would be impossible.
 *
 * <p>{@link #setColumnCount} goes first: the others receive a column index, and without knowing how
 * many there are there is no valid index.
 */
public interface RowSetMetaData extends java.sql.ResultSetMetaData {

    /** How many columns there are. It is called before any other. */
    void setColumnCount(int columnCount) throws java.sql.SQLException;

    void setAutoIncrement(int columnIndex, boolean property) throws java.sql.SQLException;

    void setCaseSensitive(int columnIndex, boolean property) throws java.sql.SQLException;

    void setSearchable(int columnIndex, boolean property) throws java.sql.SQLException;

    void setCurrency(int columnIndex, boolean property) throws java.sql.SQLException;

    /** One of the `columnNullable*` of {@link java.sql.ResultSetMetaData}. */
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
