package java.sql;

/**
 * KajiLibrary's java.sql.SQLData -- a Java class that knows how to read and write itself as an SQL
 * type of its own.
 *
 * <p>It is the alternative to receiving a {@link Struct} with an `Object[]` and taking it apart by
 * hand: the class is registered in the connection's type map and the driver builds it by itself.
 *
 * <p>Order rules: {@link #readSQL} has to read the attributes in the **same order** in which
 * {@link #writeSQL} writes them, which is the order of the SQL type's declaration. There are no
 * names, and that is why a change in the database's type breaks this silently.
 */
public interface SQLData {

    /** The name of the SQL type this class represents. */
    String getSQLTypeName() throws SQLException;

    /** It fills itself by reading the attributes from `stream`, in order. */
    void readSQL(SQLInput stream, String typeName) throws SQLException;

    /** It writes itself to `stream`, in the same order. */
    void writeSQL(SQLOutput stream) throws SQLException;
}
