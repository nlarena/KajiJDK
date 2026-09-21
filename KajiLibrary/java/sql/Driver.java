package java.sql;

/**
 * KajiLibrary's java.sql.Driver -- what someone who knows how to talk to a concrete database
 * implements.
 *
 * <p>{@link #acceptsURL} is the piece that makes the whole scheme work: {@link DriverManager} asks
 * each registered driver in turn and uses the first one that takes the URL. That is why an
 * application can change database by changing a string -- nobody names the driver.
 *
 * <p>And for the same reason {@link #connect} returns `null` instead of failing when the URL is not
 * its own: `null` means "not mine, keep asking", which is different from "it is mine and I could
 * not connect". Confusing the two would make a foreign driver abort the search.
 */
public interface Driver {

    /**
     * A connection to that URL, or `null` if the URL is not this driver's.
     *
     * @throws SQLException if the URL **is** its own and the connection failed
     */
    Connection connect(String url, java.util.Properties info) throws SQLException;

    /** Whether this driver understands that URL. */
    boolean acceptsURL(String url) throws SQLException;

    /** Which properties are needed to connect to that URL with what is already known. */
    DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) throws SQLException;

    int getMajorVersion();

    int getMinorVersion();

    /**
     * Whether the driver complies with the standard.
     *
     * <p>It may only return `true` if it passes the conformance tests, which require full support
     * of SQL-92 Entry Level.
     */
    boolean jdbcCompliant();

    /**
     * The logger this driver's loggers hang from.
     *
     * @throws SQLFeatureNotSupportedException if the driver does not use `java.util.logging`
     */
    java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException;
}
