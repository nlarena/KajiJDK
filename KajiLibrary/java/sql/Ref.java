package java.sql;

/**
 * KajiLibrary's java.sql.Ref -- a reference to a structured value in the database.
 *
 * <p>It is a persistent pointer: not a copy of the object but its address, which can be stored in
 * another column and followed later. It is the "object-relational" part of SQL, little used.
 */
public interface Ref {

    /** The name of the SQL type it points to. */
    String getBaseTypeName() throws SQLException;

    /** The value pointed to. */
    Object getObject() throws SQLException;

    /** The value pointed to, translating the SQL types with that map. */
    Object getObject(java.util.Map<String, Class<?>> map) throws SQLException;

    /** Changes the value pointed to. */
    void setObject(Object value) throws SQLException;
}
