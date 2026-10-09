package javax.sql;

/**
 * KajiLibrary's javax.sql.RowSetInternal -- the **inside** face of a {@link RowSet}.
 *
 * <p>It exists so that the reader and the writer can see things the application should not touch:
 * the parameters it was executed with and --the interesting one-- the **original** values of the
 * rows.
 *
 * <p>The originals are what makes it possible to synchronize a disconnected set: when writing, the
 * writer compares what there was when it was read against what there is now in the database, and if
 * it differs somebody else wrote in between. Without that original there would be no way to detect
 * the conflict -- it could only be overwritten.
 */
public interface RowSetInternal {

    /** The parameters the query was executed with. */
    Object[] getParams() throws java.sql.SQLException;

    /** The connection, if the set is connected. */
    java.sql.Connection getConnection() throws java.sql.SQLException;

    /** All the rows **as they were** when read. */
    java.sql.ResultSet getOriginal() throws java.sql.SQLException;

    /** Only the current row, as it was. */
    java.sql.ResultSet getOriginalRow() throws java.sql.SQLException;

    /** Tells the set which columns it has. */
    void setMetaData(RowSetMetaData md) throws java.sql.SQLException;
}
