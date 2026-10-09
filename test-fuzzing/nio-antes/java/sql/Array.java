package java.sql;

/**
 * KajiLibrary's java.sql.Array -- a column that holds an SQL array.
 *
 * <p>It offers the content in **two** ways, and that is not redundancy: as a Java array, when it
 * fits in memory and is to be manipulated; or as a {@link ResultSet} of two columns --index and
 * value-- when it is large and better walked a little at a time. It is the same tension {@link
 * Blob} resolves, with both outlets put in the same interface.
 *
 * <p>Indices are counted **from one**.
 */
public interface Array {

    /** The SQL name of the elements' type. */
    String getBaseTypeName() throws SQLException;

    /** The SQL code of the elements' type. */
    int getBaseType() throws SQLException;

    /** The whole content, as a Java array. */
    Object getArray() throws SQLException;

    /** The same, translating the SQL types with that map. */
    Object getArray(java.util.Map<String, Class<?>> map) throws SQLException;

    /** `count` elements from `index`. */
    Object getArray(long index, int count) throws SQLException;

    /** The same, with a type map. */
    Object getArray(long index, int count, java.util.Map<String, Class<?>> map)
            throws SQLException;

    /** The whole content, as (index, value) rows. */
    ResultSet getResultSet() throws SQLException;

    ResultSet getResultSet(java.util.Map<String, Class<?>> map) throws SQLException;

    ResultSet getResultSet(long index, int count) throws SQLException;

    ResultSet getResultSet(long index, int count, java.util.Map<String, Class<?>> map)
            throws SQLException;

    /** Releases the pointer's resources. */
    void free() throws SQLException;
}
