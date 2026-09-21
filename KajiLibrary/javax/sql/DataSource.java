package javax.sql;

/**
 * KajiLibrary's javax.sql.DataSource -- where the connections come from.
 *
 * <p>It is **the** way of getting a connection in any application that is not an example: instead
 * of the code knowing the URL, the user and the password, it knows how to ask for a connection from
 * an object somebody else configured. That is what allows the same application to talk to a
 * different database without being recompiled, and the connections to come from a pool without
 * whoever uses them finding out.
 *
 * <p>The note that was here said the interface stayed **empty** because `java.sql` did not exist,
 * and that "the day it exists, the methods come with it". It exists -- a bounded core, see {@link
 * java.sql.Connection} -- and the methods came.
 *
 * <p>Declaring it without there being any driver is not an empty promise: an interface is a
 * contract, and the contract is exact. What should not be done is to give an implementation that
 * pretends to connect.
 */
public interface DataSource extends CommonDataSource, java.sql.Wrapper {

    /** A connection, with the credentials the source has configured. */
    java.sql.Connection getConnection() throws java.sql.SQLException;

    /** A connection with those credentials. */
    java.sql.Connection getConnection(String username, String password)
            throws java.sql.SQLException;

    /**
     * A connection builder, to ask for one with more data than user and password.
     *
     * <p>It refuses by default instead of returning a builder that turns out useless later: a
     * source that knows nothing of sharding cannot honour a `shardingKey`, and finding out at the
     * end is worse than at the beginning.
     *
     * <p>The JDK's default throws {@code SQLFeatureNotSupportedException}; this one throws
     * {@code UnsupportedOperationException}, which is unchecked and is not a {@code SQLException}.
     */
    default java.sql.ConnectionBuilder createConnectionBuilder() throws java.sql.SQLException {
        throw new UnsupportedOperationException("createConnectionBuilder not implemented");
    }
}
