package javax.sql;

import java.io.PrintWriter;

/**
 * KajiLibrary's javax.sql.CommonDataSource -- what every connection factory knows how to do.
 *
 * <p>It exists so as not to repeat the same methods three times: `DataSource`,
 * `ConnectionPoolDataSource` and `XADataSource` make different things and share the configuration
 * --where to log and how long to wait--. (The note said four methods; there are six now, with
 * `getParentLogger` and `createShardingKeyBuilder`.)
 */
public interface CommonDataSource {

    /** Where this source's messages go, or `null` if none was set. */
    PrintWriter getLogWriter() throws java.sql.SQLException;

    /** Sets where they go. */
    void setLogWriter(PrintWriter out) throws java.sql.SQLException;

    /**
     * How many seconds to wait when opening a connection; zero for the system limit.
     */
    void setLoginTimeout(int seconds) throws java.sql.SQLException;

    int getLoginTimeout() throws java.sql.SQLException;

    /**
     * The logger this source's loggers hang from.
     *
     * <p>It returns the **parent** and not its own on purpose: whoever configures tracing wants to
     * turn off or raise the level of a source's whole family in a single move, and cannot know the
     * names the driver chose for its own.
     *
     * @throws java.sql.SQLFeatureNotSupportedException if the source does not use
     *     `java.util.logging`
     */
    java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException;

    /**
     * A sharding key builder for this source.
     *
     * <p>The JDK's default throws {@code SQLFeatureNotSupportedException}; this one throws
     * {@code UnsupportedOperationException}, which is unchecked and is not a {@code SQLException}.
     */
    default java.sql.ShardingKeyBuilder createShardingKeyBuilder() throws java.sql.SQLException {
        throw new UnsupportedOperationException("createShardingKeyBuilder not implemented");
    }
}
